import { Alert, Button, Card, Empty, InputNumber, List, message, Select, Space, Spin, Tag, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { StoredValueAccount } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;

// 卡名：通用卡=综合体通用卡；商户卡=该发卡商户的卡。
function cardLabel(acc: StoredValueAccount): string {
  return acc.cardScope === "merchant" ? `商户卡（${acc.merchantId ?? "-"}）` : "综合体通用卡";
}

// 资金模式标签（仅商户卡）：平台代收 / 商户预收（旧卡未定格按商户预收）。
function fundLabel(acc: StoredValueAccount): string | null {
  if (acc.cardScope !== "merchant") return null;
  return acc.fundOwnership === "platform_collect" ? "平台代收" : "商户预收";
}

function yuan(cent: number): string {
  return `¥${(cent / 100).toFixed(2)}`;
}

// ⑦ 储值钱包（AUD-061）：多卡（通用卡 + 各商户卡）分卡余额展示 + 选卡充值/消费。3.17：error/loading/empty 分离可重试。
export function WalletPage({ service }: { service: CustomerService }): React.ReactElement {
  const [accounts, setAccounts] = useState<StoredValueAccount[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState<string | undefined>(undefined);
  const [amountYuan, setAmountYuan] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    let active = true;
    setLoading(true);
    setError(null);
    service
      .getStoredValueAccounts()
      .then((accs) => {
        if (!active) return;
        const cards = accs ?? [];
        setAccounts(cards);
        setSelectedId((prev) => prev ?? cards[0]?.id);
      })
      .catch((err) => {
        if (active) setError(err instanceof Error ? err.message : "钱包加载失败");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [service]);

  useEffect(() => load(), [load]);

  const selected = useMemo(() => accounts.find((a) => a.id === selectedId), [accounts, selectedId]);

  const scopeOf = (acc?: StoredValueAccount) => ({
    projectId: acc?.projectId ?? undefined,
    merchantId: acc?.merchantId ?? undefined,
  });

  const doTopup = (): void => {
    if (!selected || !amountYuan || amountYuan <= 0) {
      void message.warning("请选择储值卡并输入充值金额");
      return;
    }
    setBusy(true);
    service
      .topupStoredValue({ ...scopeOf(selected), amountCent: Math.round(amountYuan * 100) })
      .then(() => void message.success("已发起充值，请到柜台完成支付收款后到账"))
      .catch((e) => void message.error(e instanceof Error ? e.message : "充值发起失败"))
      .finally(() => setBusy(false));
  };

  const doConsume = (): void => {
    if (!selected || !amountYuan || amountYuan <= 0) {
      void message.warning("请选择储值卡并输入消费金额");
      return;
    }
    setBusy(true);
    service
      .consumeStoredValue({ ...scopeOf(selected), amountCent: Math.round(amountYuan * 100), reason: "钱包消费" })
      .then(() => {
        void message.success("消费成功");
        setAmountYuan(null);
        load();
      })
      .catch((e) => void message.error(e instanceof Error ? e.message : "消费失败"))
      .finally(() => setBusy(false));
  };

  return (
    <div data-testid="wallet-page">
      {error ? (
        <Alert
          type="error"
          message="钱包加载失败"
          description={error}
          style={{ marginBottom: 12 }}
          action={
            <Button size="small" onClick={() => load()} data-testid="wallet-retry">
              重试
            </Button>
          }
        />
      ) : null}

      <Card title="我的储值卡" data-testid="wallet-cards">
        {loading ? (
          <Spin />
        ) : error ? null : accounts.length ? (
          <List
            dataSource={accounts}
            locale={{ emptyText: "暂无储值卡" }}
            renderItem={(acc) => (
              <List.Item data-testid="wallet-card-item">
                <List.Item.Meta
                  title={
                    <span>
                      {cardLabel(acc)}{" "}
                      {fundLabel(acc) ? <Tag color="blue">{fundLabel(acc)}</Tag> : null}
                      <Tag color={acc.status === "active" ? "green" : "default"}>
                        {formatBusinessStatus(acc.status)}
                      </Tag>
                    </span>
                  }
                  description={<Text strong>余额 {yuan(acc.balanceCent)}</Text>}
                />
              </List.Item>
            )}
          />
        ) : (
          <Empty description="暂无储值卡" />
        )}
      </Card>

      <Card title="充值 / 消费" style={{ marginTop: 16 }}>
        {loading ? (
          <Spin />
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size="middle">
            <Select
              data-testid="wallet-card-select"
              placeholder="选择储值卡"
              value={selectedId}
              onChange={(v) => setSelectedId(v)}
              style={{ width: "100%" }}
              options={accounts.map((a) => ({ value: a.id, label: `${cardLabel(a)} · 余额 ${yuan(a.balanceCent)}` }))}
            />
            <InputNumber
              data-testid="wallet-amount"
              addonBefore="¥"
              min={0.01}
              step={1}
              precision={2}
              placeholder="金额（元）"
              value={amountYuan}
              onChange={(v) => setAmountYuan(typeof v === "number" ? v : null)}
              style={{ width: "100%" }}
            />
            <Space>
              <Button type="primary" loading={busy} data-testid="wallet-topup" onClick={doTopup}>
                充值
              </Button>
              <Button loading={busy} data-testid="wallet-consume" onClick={doConsume}>
                消费
              </Button>
            </Space>
            <Paragraph type="secondary" style={{ marginBottom: 0 }}>
              充值需到柜台完成支付收款后到账；储值消费即时扣本卡余额。商户预收卡退卡请到店办理。
            </Paragraph>
          </Space>
        )}
      </Card>
    </div>
  );
}
