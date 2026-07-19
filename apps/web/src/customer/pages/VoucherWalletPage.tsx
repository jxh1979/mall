import { Alert, Button, Card, Empty, List, Space, Spin, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { CustomerVoucher } from "../types";

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

// 核销方式说明（可用券展示；出示码 / 到店核销点口径）。核销在 POS/工作手机侧，本端只读不发起核销。
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

function VoucherCard({ v, group }: { v: CustomerVoucher; group: Group }): React.ReactElement {
  const value = valueLine(v);
  const expiry = expiryText(v);
  const method = group === "available" ? methodText(v) : null;
  const remaining = v.remainingTimes != null && v.remainingTimes > 1 ? `剩余 ${v.remainingTimes} 次` : null;
  return (
    <List.Item data-testid="voucher-item">
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

// 我的券包（客人端只读）：积分兑换券 / 营销赠券 / 通票第三方券聚合，按状态分组展示。
// 内部结算价客人不可见（后端聚合视图本就不含成本字段）；核销在 POS/工作手机侧，本页不发起核销。
export function VoucherWalletPage({ service }: { service: CustomerService }): React.ReactElement {
  const [vouchers, setVouchers] = useState<CustomerVoucher[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    let active = true;
    setLoading(true);
    setError(null);
    service
      .listMyVouchers()
      .then((list) => {
        if (active) setVouchers(list ?? []);
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
        这里汇总你名下的所有券（积分兑换券 / 活动赠券 / 通票合作券）。券的核销请到店由店员或核销点办理，本页仅供查看。
      </Paragraph>

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
                renderItem={(v) => <VoucherCard v={v} group={g} />}
              />
            ) : (
              <Empty description="暂无" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
          </Card>
        ))
      )}
    </div>
  );
}
