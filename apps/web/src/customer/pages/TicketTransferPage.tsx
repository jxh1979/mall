import { Alert, Button, Card, Empty, Input, List, Radio, Segmented, Space, Spin, Tag, Typography, message } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { CustomerTicket, TicketTransfer, TicketTransferPreview } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;

type Slot = { parentTicketId: string; slotIndex: number; ticketName: string | null; transferState?: string | null };

// AUD-077 门票转赠：把票送给别人（手机号 / 微信链接·二维码），领取前可撤回，转赠中冻结不可用。
export function TicketTransferPage({ service }: { service: CustomerService }): React.ReactElement {
  const [held, setHeld] = useState<CustomerTicket[]>([]);
  const [outgoing, setOutgoing] = useState<TicketTransfer[]>([]);
  const [incoming, setIncoming] = useState<TicketTransfer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 发起转赠表单
  const [channel, setChannel] = useState<"phone" | "link">("phone");
  const [receiverPhone, setReceiverPhone] = useState("");
  const [busy, setBusy] = useState(false);
  // 凭领取码领取
  const [claimCode, setClaimCode] = useState("");
  const [preview, setPreview] = useState<TicketTransferPreview | null>(null);

  const load = useCallback(() => {
    let active = true;
    setLoading(true);
    setError(null);
    Promise.all([service.listMyTickets(), service.listOutgoingTicketTransfers(), service.listIncomingTicketTransfers()])
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
      if (r.channel === "link" && r.claimCode) {
        message.success("已生成领取码，把它发给对方即可领取");
      } else {
        message.success("已发起转赠，已给对方发送领取通知");
      }
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

  const claimIncoming = async (id: string) => {
    setBusy(true);
    try {
      await service.claimTicketTransfer(id);
      message.success("领取成功，票已到你的“我的票”");
      load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : "领取失败");
    } finally {
      setBusy(false);
    }
  };

  const doPreview = async () => {
    if (!claimCode.trim()) return;
    setBusy(true);
    try {
      setPreview(await service.previewTicketTransfer(claimCode.trim()));
    } catch (err) {
      setPreview(null);
      message.error(err instanceof Error ? err.message : "领取码无效或已失效");
    } finally {
      setBusy(false);
    }
  };

  const claimByCode = async () => {
    if (!preview) return;
    setBusy(true);
    try {
      await service.claimTicketTransfer(preview.transferId, claimCode.trim());
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

  if (loading) return <Spin tip="加载中…" />;

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <h1 style={{ fontSize: 20, margin: 0 }}>门票转赠</h1>
      {error ? <Alert type="error" message={error} showIcon action={<Button onClick={load}>重试</Button>} /> : null}

      <Card title="送出我的票">
        <Paragraph type="secondary">选择送法后，点某张票下方“转赠给他”。整张送出，对方确认领取前你可以撤回。</Paragraph>
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
              const transferring = slot.transferState === "transferring";
              return (
                <List.Item
                  actions={[
                    transferring ? (
                      <Tag color="orange" key="s">转赠中</Tag>
                    ) : (
                      <Button key="b" type="link" disabled={busy} onClick={() => initiate(slot)}>
                        转赠给他
                      </Button>
                    ),
                  ]}
                >
                  <List.Item.Meta
                    title={slot.ticketName ?? "门票"}
                    description={`第 ${slot.slotIndex} 位 · ${slot.parentTicketId}`}
                  />
                </List.Item>
              );
            }}
          />
        )}
      </Card>

      <Card title="待我领取">
        {incoming.length === 0 ? (
          <Empty description="没有待领取的转赠" />
        ) : (
          <List
            dataSource={incoming}
            renderItem={(t) => (
              <List.Item
                actions={[
                  <Button key="c" type="primary" disabled={busy} onClick={() => claimIncoming(t.id)}>
                    确认领取
                  </Button>,
                ]}
              >
                <List.Item.Meta title={t.ticketName ?? "门票"} description="有人把这张票转赠给了你的手机号" />
              </List.Item>
            )}
          />
        )}
      </Card>

      <Card title="凭领取码 / 二维码链接领取">
        <Space.Compact style={{ width: "100%", maxWidth: 480 }}>
          <Input placeholder="粘贴领取码" value={claimCode} onChange={(e) => setClaimCode(e.target.value)} />
          <Button onClick={doPreview} disabled={busy || !claimCode.trim()}>查看</Button>
        </Space.Compact>
        {preview ? (
          <Card size="small" style={{ marginTop: 12 }}>
            <Paragraph>
              <Text strong>{preview.ticketName ?? "门票"}</Text>（{preview.numPeople} 人 · {preview.totalTimes} 次）
            </Paragraph>
            <Button type="primary" disabled={busy} onClick={claimByCode}>确认领取</Button>
          </Card>
        ) : null}
      </Card>

      <Card title="我发起的转赠">
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
                  description={
                    t.channel === "link" && t.claimCode
                      ? `领取码：${t.claimCode}`
                      : t.receiverPhoneMasked
                        ? `送给 ${t.receiverPhoneMasked}`
                        : "微信链接 / 二维码"
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Card>
    </Space>
  );
}
