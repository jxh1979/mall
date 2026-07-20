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
import { useCallback, useEffect, useMemo, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { CustomerVoucher, VoucherTransfer } from "../types";

const { Text, Paragraph } = Typography;

// 状态分组（可用/已核销/已过期）：统一台账券走状态机 issued/redeemed/expired/revoked；
// 通票第三方券为权益侧原状态（active/used/... 兼容映射，不硬套）。未知状态归入「已过期/失效」组不丢卡。
type Group = "available" | "redeemed" | "expired";

function groupOf(status: string): Group {
  const s = (status || "").toLowerCase();
  if (["issued", "active", "open"].includes(s)) return "available";
  if (["redeemed", "used", "consumed", "claimed"].includes(s)) return "redeemed";
  return "expired";
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
  onsite_code: "出示券码到核销点核销",
  external_reflow: "由合作方按其系统核销（以合作方为准）",
};

const GROUP_TITLE: Record<Group, string> = {
  available: "可用",
  redeemed: "已核销",
  expired: "已过期 / 失效",
};

const GROUP_TAG_COLOR: Record<Group, string> = {
  available: "green",
  redeemed: "blue",
  expired: "default",
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

// 统一台账券（source=unified）且可用时，才可自助「使用」（出示核销码）/「转赠」。
function isSelfServiceable(v: CustomerVoucher, group: Group): boolean {
  return group === "available" && v.source === "unified";
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
              <Button key="transfer" size="small" data-testid="voucher-transfer" onClick={() => onTransfer(v)}>
                转赠
              </Button>,
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
          </Space>
        }
      />
    </List.Item>
  );
}

// 我的券包（客人端）：积分兑换券 / 营销赠券 / 通票第三方券聚合，按状态分组展示。
// 统一台账券可自助「使用」（出示核销码给店员/核销点扫码，核销仍在 POS/工作手机侧）与「转赠」他人（会员间转移）。
// 内部结算价客人不可见（后端聚合视图本就不含成本字段）。
export function VoucherWalletPage({ service }: { service: CustomerService }): React.ReactElement {
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
  // 凭领取码领取。
  const [claimCode, setClaimCode] = useState("");

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
    const buckets: Record<Group, CustomerVoucher[]> = { available: [], redeemed: [], expired: [] };
    for (const v of vouchers) buckets[groupOf(v.status)].push(v);
    return buckets;
  }, [vouchers]);

  const order: Group[] = ["available", "redeemed", "expired"];

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

  const claimByCode = () => {
    const code = claimCode.trim();
    if (!code) {
      message.warning("请输入领取码");
      return;
    }
    service
      .previewVoucherTransfer(code)
      .then((preview) =>
        service.claimVoucherTransfer(preview.id, code).then(() => {
          message.success("领取成功，券已进入你的券包");
          setClaimCode("");
          load();
        }),
      )
      .catch((err) => message.error(err instanceof Error ? err.message : "领取码无效或已被领取"));
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
        这里汇总你名下的所有券（积分兑换券 / 活动赠券 / 通票合作券）。可用的券可「使用」（出示券码给店员/核销点扫码核销）或「转赠」给他人。
      </Paragraph>

      {/* 凭领取码领取（对方用二维码/链接方式转赠给你时）。 */}
      <Card size="small" title="凭领取码领取" style={{ marginBottom: 12 }} data-testid="voucher-claim-by-code">
        <Space.Compact style={{ width: "100%" }}>
          <Input
            placeholder="粘贴对方给你的领取码"
            value={claimCode}
            onChange={(e) => setClaimCode(e.target.value)}
            data-testid="voucher-claim-code-input"
          />
          <Button type="primary" onClick={claimByCode} data-testid="voucher-claim-code-submit">
            领取
          </Button>
        </Space.Compact>
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

      {/* 使用：出示核销码给店员/核销点扫码。 */}
      <Modal
        open={useVoucher != null}
        title="出示券码核销"
        onCancel={() => setUseVoucher(null)}
        footer={<Button onClick={() => setUseVoucher(null)}>关闭</Button>}
        data-testid="voucher-use-modal"
      >
        {useVoucher ? (
          <Space direction="vertical" style={{ width: "100%" }} size={8}>
            <Text strong>{useVoucher.name}</Text>
            <Text type="secondary">请把下面的券码出示给店员或核销点扫码核销：</Text>
            <div
              data-testid="voucher-use-code"
              style={{
                fontSize: 22,
                fontFamily: "monospace",
                letterSpacing: 2,
                textAlign: "center",
                padding: "16px 0",
                background: "rgba(0,0,0,0.03)",
                borderRadius: 8,
              }}
            >
              {useVoucher.code ?? "（该券无券码，请到核销点办理）"}
            </div>
            {useVoucher.code ? (
              <Text copyable={{ text: useVoucher.code }} type="secondary">
                复制券码
              </Text>
            ) : null}
          </Space>
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
                <Text>把下面的领取码发给对方，对方在券包「凭领取码领取」即可：</Text>
                <Text copyable={{ text: transferResult.claimCode }} strong data-testid="voucher-transfer-claim-code">
                  {transferResult.claimCode}
                </Text>
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
                  <Radio.Button value="link">生成领取码</Radio.Button>
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
              <Text type="secondary">将生成一次性领取码，你可以把它发给任意一位会员，对方凭码领取。</Text>
            )}
            <Divider style={{ margin: "4px 0" }} />
            <Text type="secondary">转赠后该券将不再属于你；对方领取前你可以随时撤回。</Text>
          </Space>
        )}
      </Modal>
    </div>
  );
}
