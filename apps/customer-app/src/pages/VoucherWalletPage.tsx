// 文件版本：V0.01
import {
  Alert,
  Button,
  Card,
  Divider,
  Empty,
  Input,
  List,
  Modal,
  Radio,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from "antd";
import dayjs from "dayjs";
import { QRCodeSVG } from "qrcode.react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { buildCustomerLink, parseCustomerLink } from "@complex/customer-core";
import type { CustomerService } from "@complex/customer-core";
import type { CustomerVoucher, VoucherTransfer } from "@complex/customer-core";

import { CUSTOMER_LINK_HOST } from "../api";
import { DynamicCode } from "../components/DynamicCode";
import { platform } from "../platform";

const { Text, Paragraph } = Typography;

// 状态分组（BSM「按「可用 / 已核销（已使用） / 已过期 / 已失效」分组」）：统一台账券走状态机 issued/redeemed/expired/revoked；
// 通票第三方券为权益侧原状态（active/used/... 兼容映射，不硬套）。作废与其余状态归「已失效」组不丢卡。
type Group = "available" | "redeemed" | "expired" | "invalid";

function groupOf(status: string): Group {
  const s = (status || "").toLowerCase();
  if (["issued", "active", "open"].includes(s)) return "available";
  if (["redeemed", "used", "consumed", "claimed"].includes(s)) return "redeemed";
  if (s === "expired") return "expired";
  return "invalid";
}

const STATUS_TEXT: Record<string, string> = {
  issued: "可用",
  active: "可用",
  open: "可用",
  redeemed: "已核销",
  used: "已核销",
  consumed: "已核销",
  claimed: "已核销",
  expired: "已过期",
  revoked: "已作废",
  voided: "已失效",
  inactive: "未生效",
};

// 适用范围说明（券类型口径）。
const KIND_SCOPE: Record<string, string> = {
  merchant_discount: "商户折扣券 · 结账时抵扣",
  in_store_goods: "店内实物券 · 到核销点领取",
  external_partner: "外部合作伙伴券 · 由合作方核销",
};

// 核销方式说明（可用券展示；出示码 / 到店核销点口径）。核销在 POS/工作手机侧。
const METHOD_TEXT: Record<string, string> = {
  in_store_staff: "到店由店员核销",
  onsite_code: "出示动态券码到核销点核销",
  external_reflow: "由合作方按其系统核销（以合作方为准）",
};

const GROUP_TITLE: Record<Group, string> = {
  available: "可用",
  redeemed: "已核销（已使用）",
  expired: "已过期",
  invalid: "已失效",
};

const GROUP_TAG_COLOR: Record<Group, string> = {
  available: "green",
  redeemed: "blue",
  expired: "default",
  invalid: "red",
};

function statusText(status: string): string {
  return STATUS_TEXT[(status || "").toLowerCase()] ?? status;
}

function yuan(cent: number): string {
  return `¥${(cent / 100).toFixed(2)}`;
}

// 面额/折扣：折扣券为抵扣额，实物/其它为面额；通票第三方券无面额则不展示该行。
function valueLine(v: CustomerVoucher): string | null {
  if (v.faceValueCent == null || v.faceValueCent <= 0) return null;
  return v.voucherKind === "merchant_discount" ? `抵扣 ${yuan(v.faceValueCent)}` : `面额 ${yuan(v.faceValueCent)}`;
}

function scopeText(v: CustomerVoucher): string {
  return KIND_SCOPE[v.voucherKind] ?? "券";
}

function methodText(v: CustomerVoucher): string | null {
  if (!v.redeemMethod) return null;
  return METHOD_TEXT[v.redeemMethod] ?? null;
}

function expiryText(v: CustomerVoucher): string | null {
  if (!v.expiresAt) return null;
  const d = dayjs(v.expiresAt);
  return d.isValid() ? `有效期至 ${d.format("YYYY-MM-DD")}` : null;
}

// 统一台账券（source=unified）且可用时，才可自助「使用」（出示动态核销码）；「转赠」另须券种允许（服务端 transferable）。
function isSelfServiceable(v: CustomerVoucher, group: Group): boolean {
  return group === "available" && v.source === "unified";
}

// 已失效详情：原因与是否退回积分（积分兑换券才有退回积分一说，其余券 pointsRefunded 为空）。
function invalidLines(v: CustomerVoucher): string[] {
  const lines = [`失效原因：${v.invalidReason ?? "未注明"}`];
  if (v.pointsRefunded != null) lines.push(v.pointsRefunded > 0 ? `已退回 ${v.pointsRefunded} 积分` : "未退回积分");
  return lines;
}

function VoucherCard({
  v,
  group,
  onUse,
  onTransfer,
}: {
  v: CustomerVoucher;
  group: Group;
  onUse: (v: CustomerVoucher) => void;
  onTransfer: (v: CustomerVoucher) => void;
}): React.ReactElement {
  const value = valueLine(v);
  const expiry = expiryText(v);
  const method = group === "available" ? methodText(v) : null;
  const remaining = v.remainingTimes != null && v.remainingTimes > 1 ? `剩余 ${v.remainingTimes} 次` : null;
  const selfService = isSelfServiceable(v, group);
  return (
    <List.Item
      data-testid="voucher-item"
      actions={
        selfService
          ? [
              <Button key="use" type="primary" size="small" data-testid="voucher-use" onClick={() => onUse(v)}>
                使用
              </Button>,
              ...(v.transferable
                ? [
                    <Button key="transfer" size="small" data-testid="voucher-transfer" onClick={() => onTransfer(v)}>
                      转赠
                    </Button>,
                  ]
                : []),
            ]
          : undefined
      }
    >
      <List.Item.Meta
        title={
          <span>
            {v.name} <Tag color={GROUP_TAG_COLOR[group]}>{statusText(v.status)}</Tag>
            {remaining ? <Tag color="gold">{remaining}</Tag> : null}
          </span>
        }
        description={
          <Space direction="vertical" size={2} style={{ width: "100%" }}>
            <Text type="secondary">{scopeText(v)}</Text>
            {value ? <Text strong>{value}</Text> : null}
            {expiry ? <Text type="secondary">{expiry}</Text> : null}
            {method ? <Text type="secondary">核销方式：{method}</Text> : null}
            {group === "invalid" ? invalidLines(v).map((line) => <Text key={line} type="secondary">{line}</Text>) : null}
          </Space>
        }
      />
    </List.Item>
  );
}

// 我的券包（客人端）：积分兑换券 / 营销赠券 / 通票第三方券聚合，按「可用 / 已核销（已使用） / 已过期 / 已失效」分组展示（BSM「我的券包」）。
// 统一台账券可自助「使用」——出示按全场统一间隔刷新的动态核销码（BSM「客人出示的……权益核销码……都不是一张固定的图」；不显示、不复制固定券码），
// 核销仍在 POS/工作手机侧；券种允许的才可「转赠」他人（第十三批裁决第 9 条：发起不冻结，对方确认领取前仍可自用或改转他人，先成功的为准）。
// 领取链接由深链带领取码打开，只预览，点「确认领取」才领。内部结算价客人不可见（后端聚合视图本就不含成本字段）。
export function VoucherWalletPage({ service, initialClaimCode }: { service: CustomerService; initialClaimCode?: string }): React.ReactElement {
  const [vouchers, setVouchers] = useState<CustomerVoucher[]>([]);
  const [incoming, setIncoming] = useState<VoucherTransfer[]>([]);
  const [outgoing, setOutgoing] = useState<VoucherTransfer[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // 使用（出示码）弹窗。
  const [useVoucher, setUseVoucher] = useState<CustomerVoucher | null>(null);
  // 转赠弹窗。
  const [transferVoucher, setTransferVoucher] = useState<CustomerVoucher | null>(null);
  const [channel, setChannel] = useState<"phone" | "link">("phone");
  const [receiverPhone, setReceiverPhone] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [transferResult, setTransferResult] = useState<VoucherTransfer | null>(null);
  // 凭领取码领取（先预览，确认才领）。
  const [claimCode, setClaimCode] = useState(initialClaimCode ?? "");
  const [scanNotice, setScanNotice] = useState<string | null>(null);
  const [claimPreview, setClaimPreview] = useState<VoucherTransfer | null>(null);
  const [claiming, setClaiming] = useState(false);

  const load = useCallback(() => {
    let active = true;
    setLoading(true);
    setError(null);
    Promise.all([
      service.listMyVouchers(),
      service.listIncomingVoucherTransfers().catch(() => [] as VoucherTransfer[]),
      service.listOutgoingVoucherTransfers().catch(() => [] as VoucherTransfer[]),
    ])
      .then(([list, inc, out]) => {
        if (!active) return;
        setVouchers(list ?? []);
        setIncoming(inc ?? []);
        setOutgoing(out ?? []);
      })
      .catch((err) => {
        if (active) setError(err instanceof Error ? err.message : "券包加载失败");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [service]);

  useEffect(() => load(), [load]);

  const groups = useMemo(() => {
    const buckets: Record<Group, CustomerVoucher[]> = { available: [], redeemed: [], expired: [], invalid: [] };
    for (const v of vouchers) buckets[groupOf(v.status)].push(v);
    return buckets;
  }, [vouchers]);

  const order: Group[] = ["available", "redeemed", "expired", "invalid"];

  const closeTransfer = () => {
    setTransferVoucher(null);
    setReceiverPhone("");
    setChannel("phone");
    setTransferResult(null);
    setSubmitting(false);
  };

  const submitTransfer = () => {
    if (!transferVoucher) return;
    if (channel === "phone" && !receiverPhone.trim()) {
      message.warning("请填写接收人手机号");
      return;
    }
    setSubmitting(true);
    service
      .initiateVoucherTransfer({
        voucherInstanceId: transferVoucher.id,
        channel,
        receiverPhone: channel === "phone" ? receiverPhone.trim() : undefined,
      })
      .then((res) => {
        setTransferResult(res);
        message.success("转赠已发起，等待对方领取");
        load();
      })
      .catch((err) => message.error(err instanceof Error ? err.message : "转赠发起失败"))
      .finally(() => setSubmitting(false));
  };

  const claimIncoming = (t: VoucherTransfer) => {
    service
      .claimVoucherTransfer(t.id)
      .then(() => {
        message.success("领取成功，券已进入你的券包");
        load();
      })
      .catch((err) => message.error(err instanceof Error ? err.message : "领取失败"));
  };

  const previewByCode = useCallback((raw: string) => {
    const code = raw.trim();
    if (!code) {
      message.warning("请输入领取码");
      return;
    }
    setClaimPreview(null);
    service
      .previewVoucherTransfer(code)
      .then(setClaimPreview)
      .catch((err) => message.error(err instanceof Error ? err.message : "领取码无效或已被领取"));
  }, [service]);

  // 扫对方出示的领取二维码（BSM「微信链接或二维码允许多人打开」）：只认券领取链接，扫到即预览、不领取。
  const scanClaim = async () => {
    const raw = await platform.scanCode();
    if (raw === null) {
      setScanNotice("本机暂不能扫码，请打开对方分享的领取链接或粘贴领取码");
      return;
    }
    const link = parseCustomerLink(raw, CUSTOMER_LINK_HOST);
    const code = link?.route === "voucher-claim" ? link.params.claim_code : undefined;
    if (!code) {
      setScanNotice("无法识别的二维码");
      return;
    }
    setScanNotice(null);
    setClaimCode(code);
    previewByCode(code);
  };

  // 领取深链（/c/voucher-claim?claim_code=…）进来：自动预览，不领取。
  useEffect(() => {
    if (initialClaimCode) previewByCode(initialClaimCode);
  }, [initialClaimCode, previewByCode]);

  const confirmClaim = () => {
    if (!claimPreview) return;
    setClaiming(true);
    service
      .claimVoucherTransfer(claimPreview.id, claimCode.trim())
      .then(() => {
        message.success("领取成功，券已进入你的券包");
        setClaimCode("");
        setClaimPreview(null);
        load();
      })
      .catch((err) => message.error(err instanceof Error ? err.message : "领取失败"))
      .finally(() => setClaiming(false));
  };

  const shareLink = transferResult?.channel === "link" && transferResult.claimCode
    ? buildCustomerLink(CUSTOMER_LINK_HOST, "voucher-claim", { claim_code: transferResult.claimCode })
    : null;
  const share = async () => {
    if (!shareLink) return;
    const result = await platform.share({ title: `送你一张${transferResult?.voucherName ?? "券"}`, link: shareLink });
    if (result === "copied") message.success("领取链接已复制，发给对方即可");
    else if (result === "unavailable") message.warning("本机暂不能分享，请把领取码告诉对方");
  };

  const revokeOutgoing = (t: VoucherTransfer) => {
    service
      .revokeVoucherTransfer(t.id)
      .then(() => {
        message.success("已撤回转赠");
        load();
      })
      .catch((err) => message.error(err instanceof Error ? err.message : "撤回失败"));
  };

  const pendingIncoming = incoming.filter((t) => t.status === "pending");
  const pendingOutgoing = outgoing.filter((t) => t.status === "pending");

  return (
    <div data-testid="voucher-wallet-page">
      {error ? (
        <Alert
          type="error"
          message="券包加载失败"
          description={error}
          style={{ marginBottom: 12 }}
          action={
            <Button size="small" onClick={() => load()} data-testid="voucher-retry">
              重试
            </Button>
          }
        />
      ) : null}

      <Paragraph type="secondary">
        这里汇总你名下的所有券（积分兑换券 / 活动赠券 / 通票合作券）。可用的券可「使用」（出示动态券码给店员/核销点扫码核销），券种允许的可「转赠」给他人。
      </Paragraph>

      {/* 凭领取码领取（对方用二维码/链接方式转赠给你时）：先看是什么券，确认才领。 */}
      <Card size="small" title="凭领取码领取" style={{ marginBottom: 12 }} data-testid="voucher-claim-by-code">
        <Space.Compact style={{ width: "100%" }}>
          <Input
            placeholder="粘贴对方给你的领取码"
            value={claimCode}
            onChange={(e) => setClaimCode(e.target.value)}
            data-testid="voucher-claim-code-input"
          />
          <Button onClick={() => previewByCode(claimCode)} data-testid="voucher-claim-code-submit">
            查看
          </Button>
        </Space.Compact>
        <Button style={{ marginTop: 8 }} onClick={() => void scanClaim()} data-testid="voucher-claim-scan">
          扫领取二维码
        </Button>
        {scanNotice ? <Alert type="warning" showIcon message={scanNotice} style={{ marginTop: 8 }} /> : null}
        {claimPreview ? (
          <Space direction="vertical" style={{ marginTop: 12 }} data-testid="voucher-claim-preview">
            <Text strong>{claimPreview.voucherName ?? "券"}</Text>
            <Button type="primary" loading={claiming} onClick={confirmClaim} data-testid="voucher-claim-confirm">
              确认领取
            </Button>
          </Space>
        ) : null}
      </Card>

      {/* 待领取（别人转赠给你、手机号方式）。 */}
      {pendingIncoming.length ? (
        <Card size="small" title={`待你领取（${pendingIncoming.length}）`} style={{ marginBottom: 12 }} data-testid="voucher-incoming">
          <List
            dataSource={pendingIncoming}
            renderItem={(t) => (
              <List.Item
                actions={[
                  <Button key="claim" type="primary" size="small" onClick={() => claimIncoming(t)}>
                    领取
                  </Button>,
                ]}
              >
                <List.Item.Meta title={t.voucherName ?? "券"} description={<Text type="secondary">有人把券转赠给你</Text>} />
              </List.Item>
            )}
          />
        </Card>
      ) : null}

      {/* 我发起的转赠（可撤回未领取的）。 */}
      {pendingOutgoing.length ? (
        <Card size="small" title={`我发起的转赠（${pendingOutgoing.length}）`} style={{ marginBottom: 12 }} data-testid="voucher-outgoing">
          <List
            dataSource={pendingOutgoing}
            renderItem={(t) => (
              <List.Item
                actions={[
                  <Button key="revoke" danger size="small" onClick={() => revokeOutgoing(t)}>
                    撤回
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title={t.voucherName ?? "券"}
                  description={
                    <Space direction="vertical" size={0}>
                      <Text type="secondary">
                        {t.channel === "phone" ? `转赠给 ${t.receiverPhoneMasked ?? "指定手机号"}` : "二维码/领取码方式，等待领取"}
                      </Text>
                      {t.channel === "link" && t.claimCode ? (
                        <Text copyable={{ text: t.claimCode }} type="secondary">
                          领取码：{t.claimCode}
                        </Text>
                      ) : null}
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        </Card>
      ) : null}

      {loading ? (
        <Spin data-testid="voucher-loading" />
      ) : error ? null : vouchers.length === 0 ? (
        <Empty description="暂无券" data-testid="voucher-empty" />
      ) : (
        order.map((g) => (
          <Card
            key={g}
            title={`${GROUP_TITLE[g]}（${groups[g].length}）`}
            style={{ marginBottom: 16 }}
            data-testid={`voucher-group-${g}`}
          >
            {groups[g].length ? (
              <List
                dataSource={groups[g]}
                renderItem={(v) => (
                  <VoucherCard v={v} group={g} onUse={setUseVoucher} onTransfer={setTransferVoucher} />
                )}
              />
            ) : (
              <Empty description="暂无" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
          </Card>
        ))
      )}

      {/* 使用：出示动态核销码给店员/核销点扫码（码值只画进二维码，不显示文字、不提供复制）。 */}
      <Modal
        open={useVoucher != null}
        title="出示券码核销"
        onCancel={() => setUseVoucher(null)}
        footer={<Button onClick={() => setUseVoucher(null)}>关闭</Button>}
        destroyOnHidden
        data-testid="voucher-use-modal"
      >
        {useVoucher ? (
          <DynamicCode
            title={useVoucher.name}
            fetchCode={() => service.getVoucherRedeemCode(useVoucher.id)}
            valueOf={(code) => code.code}
            testId="voucher-use-code"
          />
        ) : null}
      </Modal>

      {/* 转赠他人。 */}
      <Modal
        open={transferVoucher != null}
        title={`转赠：${transferVoucher?.name ?? ""}`}
        onCancel={closeTransfer}
        confirmLoading={submitting}
        footer={
          transferResult
            ? [
                <Button key="done" type="primary" onClick={closeTransfer}>
                  完成
                </Button>,
              ]
            : [
                <Button key="cancel" onClick={closeTransfer}>
                  取消
                </Button>,
                <Button
                  key="ok"
                  type="primary"
                  loading={submitting}
                  onClick={submitTransfer}
                  data-testid="voucher-transfer-submit"
                >
                  确认转赠
                </Button>,
              ]
        }
        data-testid="voucher-transfer-modal"
      >
        {transferResult ? (
          <Space direction="vertical" style={{ width: "100%" }} size={8}>
            <Alert type="success" showIcon message="转赠已发起，等待对方领取。你可在「我发起的转赠」里撤回。" />
            {transferResult.channel === "link" && transferResult.claimCode ? (
              <>
                <Text>领取码：{transferResult.claimCode}（对方在券包「凭领取码领取」里粘贴，或打开你分享的链接后点「确认领取」）</Text>
                {shareLink ? (
                  <>
                    <QRCodeSVG value={shareLink} size={160} title="领取二维码" role="img" />
                    <Button type="primary" onClick={() => void share()} data-testid="voucher-transfer-share">分享领取链接</Button>
                  </>
                ) : (
                  <Text type="secondary">客人链接域名未配置，暂不能分享链接与二维码，请把领取码告诉对方。</Text>
                )}
              </>
            ) : (
              <Text>已通知 {transferResult.receiverPhoneMasked ?? "对方"}，对方用该手机号登录后可在券包领取。</Text>
            )}
          </Space>
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size={12}>
            <div>
              <Text type="secondary">转赠方式</Text>
              <div style={{ marginTop: 6 }}>
                <Radio.Group value={channel} onChange={(e) => setChannel(e.target.value)}>
                  <Radio.Button value="phone">指定手机号</Radio.Button>
                  <Radio.Button value="link">生成领取链接</Radio.Button>
                </Radio.Group>
              </div>
            </div>
            {channel === "phone" ? (
              <Input
                placeholder="接收人手机号"
                value={receiverPhone}
                onChange={(e) => setReceiverPhone(e.target.value)}
                data-testid="voucher-transfer-phone"
              />
            ) : (
              <Text type="secondary">将生成一次性领取链接与二维码，你可以分享给任意一位会员，对方打开后确认领取。</Text>
            )}
            <Divider style={{ margin: "4px 0" }} />
            <Text type="secondary">对方确认领取前，你仍可自己使用这张券或改转他人，以先成功的为准，其余自动失效；也可以随时撤回。</Text>
          </Space>
        )}
      </Modal>
    </div>
  );
}
