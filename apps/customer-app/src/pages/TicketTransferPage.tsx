// 文件版本：V0.01
import { Alert, Button, Card, Empty, Input, List, Segmented, Space, Spin, Tag, Typography, message } from "antd";
import { QRCodeSVG } from "qrcode.react";
import { useCallback, useEffect, useMemo, useState } from "react";

import type { CustomerService } from "@complex/customer-core";
import type { CustomerTicket, TicketTransfer, TicketTransferPreview } from "@complex/customer-core";
import { buildCustomerLink, formatBusinessStatus, parseCustomerLink } from "@complex/customer-core";

import { CUSTOMER_LINK_HOST } from "../api";
import { platform } from "../platform";

const { Paragraph, Text } = Typography;

type Slot = { parentTicketId: string; slotIndex: number; ticketName: string | null; transferState?: string | null };
type SessionLimit = { sessionRestricted?: boolean; sessionLabel?: string | null; sessionEnded?: boolean };

const SESSION_ENDED = "该权益所在场次已结束、权益已终结";

/** 多人票同票已有人入场：本人份只能在该场次内入场（BSM「转出时，系统须同时向转出人与受赠人明示这一场次限制」）。 */
function sessionLimitText(limit: SessionLimit): string | null {
  if (limit.sessionEnded) return SESSION_ENDED;
  if (!limit.sessionRestricted) return null;
  return `同票已有人入场：这一人份只能在 ${limit.sessionLabel ?? "该场次"} 场次内入场，该场次结束后权益即告终结`;
}

// AUD-077 门票转赠（手机号 / 微信链接·二维码）。clients-R40（BSM「发起转赠后不冻结该权益：在接收人点击“确认领取”之前，原持有人仍然
// 可以自己使用这份权益，也可以再次转赠给其他人……以先处理成功的那个动作为准，其余动作自动失效并提示」）：发起后票照常可用，可改转他人；
// 多人票场次限制向转出人（发起结果）与受赠人（领取预览、待我领取）明示；场次已结束的「确认领取」不可点。领取链接由深链带领取码打开，
// 只预览、不领取（BSM「仅打开转赠页面不算领取」），点「确认领取」才领。分享的链接只带领取码，不带任何会话令牌。
export function TicketTransferPage({ service, initialClaimCode }: { service: CustomerService; initialClaimCode?: string }): React.ReactElement {
  const [held, setHeld] = useState<CustomerTicket[]>([]);
  const [outgoing, setOutgoing] = useState<TicketTransfer[]>([]);
  const [incoming, setIncoming] = useState<TicketTransfer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [outgoingNotice, setOutgoingNotice] = useState<string | null>(null);
  const [incomingNotice, setIncomingNotice] = useState<string | null>(null);

  // 发起转赠表单
  const [channel, setChannel] = useState<"phone" | "link">("phone");
  const [receiverPhone, setReceiverPhone] = useState("");
  const [busy, setBusy] = useState(false);
  const [created, setCreated] = useState<TicketTransfer | null>(null);
  // 凭领取码领取
  const [claimCode, setClaimCode] = useState(initialClaimCode ?? "");
  const [preview, setPreview] = useState<TicketTransferPreview | null>(null);
  const [scanNotice, setScanNotice] = useState<string | null>(null);

  const load = useCallback(() => {
    let active = true;
    setLoading(true);
    setError(null);
    setOutgoingNotice(null);
    setIncomingNotice(null);
    // 主资源=我的票(listMyTickets)决定页面级错误；转出/转入为副资源，各自 .catch 降级为空+局部提示，不拖垮我的票。
    Promise.all([
      service.listMyTickets(),
      service.listOutgoingTicketTransfers().catch(() => { if (active) setOutgoingNotice("我发起的转赠暂时加载失败，不影响其它内容"); return [] as TicketTransfer[]; }),
      service.listIncomingTicketTransfers().catch(() => { if (active) setIncomingNotice("待我领取暂时加载失败，不影响其它内容"); return [] as TicketTransfer[]; }),
    ])
      .then(([tickets, out, inc]) => {
        if (!active) return;
        setHeld(tickets ?? []);
        setOutgoing(out ?? []);
        setIncoming(inc ?? []);
      })
      .catch((err) => {
        if (active) setError(err instanceof Error ? err.message : "转赠信息加载失败");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [service]);

  useEffect(() => load(), [load]);

  const doPreview = useCallback(async (code: string) => {
    if (!code.trim()) return;
    setBusy(true);
    try {
      setPreview(await service.previewTicketTransfer(code.trim()));
    } catch (err) {
      setPreview(null);
      message.error(err instanceof Error ? err.message : "领取码无效或已失效");
    } finally {
      setBusy(false);
    }
  }, [service]);

  // 扫对方出示的领取二维码（BSM「微信链接或二维码允许多人打开」）：只认门票领取链接，扫到即预览、不领取。
  const scanClaim = async () => {
    const raw = await platform.scanCode();
    if (raw === null) {
      setScanNotice("本机暂不能扫码，请打开对方分享的领取链接或粘贴领取码");
      return;
    }
    const link = parseCustomerLink(raw, CUSTOMER_LINK_HOST);
    const code = link?.route === "ticket-claim" ? link.params.claim_code : undefined;
    if (!code) {
      setScanNotice("无法识别的二维码");
      return;
    }
    setScanNotice(null);
    setClaimCode(code);
    void doPreview(code);
  };

  // 领取深链（/c/ticket-claim?claim_code=…）进来：自动预览，不领取。
  useEffect(() => {
    if (initialClaimCode) void doPreview(initialClaimCode);
  }, [initialClaimCode, doPreview]);

  // 我持有的票按 父票+人员槽位 聚合成一个可转单位。
  const slots = useMemo<Slot[]>(() => {
    const map = new Map<string, Slot>();
    for (const t of held) {
      if (!t.parentTicketId) continue;
      const key = `${t.parentTicketId}#${t.slotIndex ?? 1}`;
      if (!map.has(key)) {
        map.set(key, {
          parentTicketId: t.parentTicketId,
          slotIndex: t.slotIndex ?? 1,
          ticketName: t.ticketName,
          transferState: t.transferState,
        });
      }
    }
    return [...map.values()];
  }, [held]);

  const initiate = async (slot: Slot) => {
    if (channel === "phone" && !receiverPhone.trim()) {
      message.warning("请填写受赠人手机号");
      return;
    }
    setBusy(true);
    try {
      const r = await service.initiateTicketTransfer({
        parentTicketId: slot.parentTicketId,
        slotIndex: slot.slotIndex,
        channel,
        receiverPhone: channel === "phone" ? receiverPhone.trim() : undefined,
      });
      setCreated(r);
      setReceiverPhone("");
      load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : "发起转赠失败");
    } finally {
      setBusy(false);
    }
  };

  const revoke = async (id: string) => {
    setBusy(true);
    try {
      await service.revokeTicketTransfer(id);
      message.success("已撤回转赠");
      load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : "撤回失败");
    } finally {
      setBusy(false);
    }
  };

  // 先成功的动作为准：原持有人已使用或已改转他人时，服务端拒绝领取并给出原因。
  const claim = async (id: string, code?: string) => {
    setBusy(true);
    try {
      await service.claimTicketTransfer(id, code);
      message.success("领取成功，票已到你的“我的票”");
      setClaimCode("");
      setPreview(null);
      load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : "领取失败");
    } finally {
      setBusy(false);
    }
  };

  const shareLink = created?.channel === "link" && created.claimCode
    ? buildCustomerLink(CUSTOMER_LINK_HOST, "ticket-claim", { claim_code: created.claimCode })
    : null;
  const share = async () => {
    if (!shareLink) return;
    const result = await platform.share({ title: `送你一张${created?.ticketName ?? "门票"}`, link: shareLink });
    if (result === "copied") message.success("领取链接已复制，发给对方即可");
    else if (result === "unavailable") message.warning("本机暂不能分享，请把领取码告诉对方");
  };

  if (loading) return <Spin tip="加载中…" />;

  const createdLimit = created ? sessionLimitText(created) : null;
  const previewLimit = preview ? sessionLimitText(preview) : null;

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      {error ? <Alert type="error" message={error} showIcon action={<Button onClick={load}>重试</Button>} /> : null}

      <Card title="送出我的票">
        <Paragraph type="secondary">
          选择送法后，点某张票下方“转赠给他”。按完整人份送出；对方点“确认领取”前，你仍可自己使用或改转他人，以先成功的为准，其余自动失效。
        </Paragraph>
        <Space style={{ marginBottom: 12 }} wrap>
          <Segmented
            value={channel}
            onChange={(v) => setChannel(v as "phone" | "link")}
            options={[
              { label: "填手机号", value: "phone" },
              { label: "微信链接 / 二维码", value: "link" },
            ]}
          />
          {channel === "phone" ? (
            <Input
              placeholder="受赠人手机号"
              value={receiverPhone}
              onChange={(e) => setReceiverPhone(e.target.value)}
              style={{ width: 200 }}
              inputMode="numeric"
            />
          ) : null}
        </Space>
        {slots.length === 0 ? (
          <Empty description="暂无可转赠的票" />
        ) : (
          <List
            dataSource={slots}
            renderItem={(slot) => {
              const pending = slot.transferState === "transferring";
              return (
                <List.Item
                  data-testid="ticket-transfer-slot"
                  actions={[
                    <Button key="b" type="link" disabled={busy} onClick={() => initiate(slot)} data-testid="ticket-transfer-initiate">
                      {pending ? "改转他人" : "转赠给他"}
                    </Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={<Space>{slot.ticketName ?? "门票"}{pending ? <Tag color="orange">转赠待领取</Tag> : null}</Space>}
                    description={`第 ${slot.slotIndex} 位`}
                  />
                </List.Item>
              );
            }}
          />
        )}
        {created ? (
          <Card size="small" style={{ marginTop: 12 }} data-testid="ticket-transfer-result">
            {createdLimit ? <Alert type="warning" showIcon data-testid="transfer-session-limit" message={createdLimit} style={{ marginBottom: 8 }} /> : null}
            {created.channel === "link" && created.claimCode ? (
              <Space direction="vertical">
                <Text>领取码：{created.claimCode}（对方在“凭领取码领取”里粘贴，或打开你分享的链接后点“确认领取”）</Text>
                {shareLink ? (
                  <>
                    <QRCodeSVG value={shareLink} size={160} title="领取二维码" role="img" />
                    <Button type="primary" onClick={() => void share()} data-testid="ticket-transfer-share">分享领取链接</Button>
                  </>
                ) : (
                  <Text type="secondary">客人链接域名未配置，暂不能分享链接与二维码，请把领取码告诉对方。</Text>
                )}
              </Space>
            ) : (
              <Text>已发起转赠，已给对方发送领取通知，等待对方确认领取。</Text>
            )}
          </Card>
        ) : null}
      </Card>

      <Card title="待我领取" data-testid="ticket-transfer-incoming">
        {incomingNotice ? <Alert type="warning" showIcon message={incomingNotice} data-testid="ticket-transfer-incoming-degraded" style={{ marginBottom: 12 }} /> : null}
        {incoming.length === 0 ? (
          <Empty description="没有待领取的转赠" />
        ) : (
          <List
            dataSource={incoming}
            renderItem={(t) => (
              <List.Item
                actions={[
                  <Button key="c" type="primary" disabled={busy || t.sessionEnded} onClick={() => claim(t.id)} data-testid="ticket-transfer-incoming-claim">
                    确认领取
                  </Button>,
                ]}
              >
                <List.Item.Meta title={t.ticketName ?? "门票"} description={sessionLimitText(t) ?? "有人把这张票转赠给了你的手机号"} />
              </List.Item>
            )}
          />
        )}
      </Card>

      <Card title="凭领取码 / 二维码链接领取">
        <Space.Compact style={{ width: "100%", maxWidth: 480 }}>
          <Input placeholder="粘贴领取码" value={claimCode} onChange={(e) => setClaimCode(e.target.value)} />
          <Button onClick={() => void doPreview(claimCode)} disabled={busy || !claimCode.trim()}>查看</Button>
        </Space.Compact>
        <Button style={{ marginTop: 8 }} disabled={busy} onClick={() => void scanClaim()} data-testid="ticket-transfer-scan-claim">扫领取二维码</Button>
        {scanNotice ? <Alert type="warning" showIcon message={scanNotice} style={{ marginTop: 8 }} /> : null}
        {preview ? (
          <Card size="small" style={{ marginTop: 12 }} data-testid="ticket-transfer-preview">
            <Paragraph>
              <Text strong>{preview.ticketName ?? "门票"}</Text>（{preview.numPeople} 人 · {preview.totalTimes} 次）
            </Paragraph>
            {previewLimit ? (
              <Alert type={preview.sessionEnded ? "error" : "warning"} showIcon message={previewLimit} style={{ marginBottom: 8 }} />
            ) : null}
            <Button type="primary" disabled={busy || preview.sessionEnded} onClick={() => void claim(preview.transferId, claimCode.trim())}
              data-testid="ticket-transfer-claim">
              确认领取
            </Button>
          </Card>
        ) : null}
      </Card>

      <Card title="我发起的转赠">
        {outgoingNotice ? <Alert type="warning" showIcon message={outgoingNotice} data-testid="ticket-transfer-outgoing-degraded" style={{ marginBottom: 12 }} /> : null}
        {outgoing.length === 0 ? (
          <Empty description="暂无转赠记录" />
        ) : (
          <List
            dataSource={outgoing}
            renderItem={(t) => (
              <List.Item
                actions={
                  t.status === "pending"
                    ? [
                        <Button key="r" danger type="link" disabled={busy} onClick={() => revoke(t.id)}>
                          撤回
                        </Button>,
                      ]
                    : []
                }
              >
                <List.Item.Meta
                  title={
                    <Space>
                      {t.ticketName ?? "门票"}
                      <Tag>{formatBusinessStatus(t.status)}</Tag>
                    </Space>
                  }
                  description={[
                    t.channel === "link" && t.claimCode
                      ? `领取码：${t.claimCode}`
                      : t.receiverPhoneMasked
                        ? `送给 ${t.receiverPhoneMasked}`
                        : "微信链接 / 二维码",
                    sessionLimitText(t),
                  ].filter(Boolean).join("；")}
                />
              </List.Item>
            )}
          />
        )}
      </Card>
    </Space>
  );
}
