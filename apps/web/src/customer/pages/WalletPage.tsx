// 文件版本：V0.03
import { Alert, Button, Card, Empty, InputNumber, List, message, Select, Space, Spin, Tag, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { StoredValueAccount, StoredValueTransaction } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;
type PendingTopup = { paymentIntentId: string; projectId?: string; merchantId?: string };
const PENDING_TOPUPS = "customer-pending-stored-value-topups";

function readPendingTopups(): PendingTopup[] {
  try {
    const value = JSON.parse(sessionStorage.getItem(PENDING_TOPUPS) ?? "[]") as unknown;
    return Array.isArray(value) ? value.filter((item) => typeof item?.paymentIntentId === "string") : [];
  } catch { return []; }
}

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

// CG-06a（BSM「我的余额」：含本金与赠送金分列；逐笔记录本金与赠送金拆分）。
function splitLabel(principalCent: number | undefined, bonusCent: number | undefined): string {
  return `本金 ${yuan(principalCent ?? 0)} · 赠送金 ${yuan(bonusCent ?? 0)}`;
}

// TK-20-R4（BSM「我的余额」：每笔退款与回冲（含回冲顺序结果））：消费退款回原卡=退款回冲，拆分行里的本金/赠送金两段即回冲顺序结果。
const TXN_TYPE_LABELS: Record<string, string> = { topup: "充值", consume: "消费", refund: "退款", adjust: "调整", consume_reversal: "退款回冲" };

// ⑦ 储值钱包（AUD-061）：多卡（通用卡 + 各商户卡）分卡余额展示 + 选卡充值与交易记录。3.17：error/loading/empty 分离可重试。
export function WalletPage({ service }: { service: CustomerService }): React.ReactElement {
  const [accounts, setAccounts] = useState<StoredValueAccount[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState<string | undefined>(undefined);
  const [amountYuan, setAmountYuan] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [channel, setChannel] = useState("wechat");
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [pendingTopups, setPendingTopups] = useState<PendingTopup[]>(readPendingTopups);
  const [transactions, setTransactions] = useState<StoredValueTransaction[]>([]);
  const [historyPage, setHistoryPage] = useState(1);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setTransactions([]);
    setHistoryError(null);
    if (!selectedId) return;
    setHistoryLoading(true);
    service.getStoredValueTransactions(selectedId, historyPage)
      .then((rows) => { if (active) setTransactions(rows); })
      .catch((err) => { if (active) setHistoryError(err instanceof Error ? err.message : "交易记录加载失败"); })
      .finally(() => { if (active) setHistoryLoading(false); });
    return () => { active = false; };
  }, [service, selectedId, historyPage, accounts]);

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

  async function checkTopup(pending: PendingTopup) {
    setBusy(true);
    setPaymentError(null);
    try {
      await service.settleStoredValueTopup(pending);
      const remaining = readPendingTopups().filter((item) => item.paymentIntentId !== pending.paymentIntentId);
      sessionStorage.setItem(PENDING_TOPUPS, JSON.stringify(remaining));
      setPendingTopups(remaining);
      load();
      void message.success("充值已到账");
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : "暂未确认到账，请稍后查询");
    } finally {
      setBusy(false);
    }
  }

  useEffect(() => {
    // 从渠道回到钱包或刷新页面后，继续核验尚未确认的真实充值单。
    void (async () => { for (const pending of readPendingTopups()) await checkTopup(pending); })();
  }, [service]);

  async function doTopup() {
    if (!selected || !amountYuan || amountYuan <= 0) {
      void message.warning("请选择储值卡并输入充值金额");
      return;
    }
    setBusy(true);
    setPaymentError(null);
    try {
      const scope = scopeOf(selected);
      const intent = await service.topupStoredValue({ ...scope, amountCent: Math.round(amountYuan * 100), channel });
      if (intent.gatewayStatus === "RESERVED") throw new Error("支付渠道未启用，尚未扣款或充值到账");
      if (intent.gatewayStatus === "NOT_CONFIGURED") throw new Error("支付渠道未配置，尚未扣款或充值到账");
      if (!["PENDING", "SUCCEEDED"].includes(String(intent.gatewayStatus))
          || typeof intent.paymentUrl !== "string" || !intent.id) {
        throw new Error("支付渠道未返回有效支付页面，尚未确认收款");
      }
      const paymentUrl = new URL(intent.paymentUrl);
      if (paymentUrl.protocol !== "https:") throw new Error("支付渠道返回的支付地址无效");
      const pending = { paymentIntentId: intent.id, ...scope };
      const next = [...readPendingTopups().filter((item) => item.paymentIntentId !== intent.id), pending];
      sessionStorage.setItem(PENDING_TOPUPS, JSON.stringify(next));
      setPendingTopups(next);
      window.location.assign(paymentUrl.href);
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : "充值发起失败");
    } finally {
      setBusy(false);
    }
  }

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
                  description={
                    <Space direction="vertical" size={0}>
                      <Text strong>余额 {yuan(acc.balanceCent)}</Text>
                      <Text type="secondary" data-testid="wallet-card-split">{splitLabel(acc.principalCent, acc.bonusCent)}</Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        ) : (
          <Empty description="暂无储值卡" />
        )}
      </Card>

      <Card title="充值" style={{ marginTop: 16 }}>
        {paymentError ? <Alert type="error" message={paymentError} /> : null}
        {loading ? (
          <Spin />
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size="middle">
            <Select
              data-testid="wallet-card-select"
              placeholder="选择储值卡"
              value={selectedId}
              onChange={(v) => { setSelectedId(v); setHistoryPage(1); }}
              style={{ width: "100%" }}
              options={accounts.map((a) => ({ value: a.id, label: `${cardLabel(a)} · 余额 ${yuan(a.balanceCent)}` }))}
            />
            <label>充值支付方式<select aria-label="充值支付方式" value={channel} onChange={(e) => setChannel(e.target.value)}>
              <option value="wechat">微信支付</option><option value="alipay">支付宝</option><option value="unionpay">银联</option>
            </select></label>
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
            </Space>
            <Paragraph type="secondary" style={{ marginBottom: 0 }}>
              在对应订单中选择储值余额支付；现场消费报手机号并验证短信后按账单扣款。商户预收卡退卡请到店办理。
            </Paragraph>
          </Space>
        )}
      </Card>
      {pendingTopups.map((pending) => <Button key={pending.paymentIntentId} disabled={busy} style={{ marginTop: 12 }} onClick={() => void checkTopup(pending)}>查询充值到账 · {pending.paymentIntentId}</Button>)}
      <Card title="交易记录" style={{ marginTop: 16 }}>
        {historyError ? <Alert type="error" message={historyError} /> : historyLoading ? <Spin /> :
          <List dataSource={transactions} locale={{ emptyText: "暂无交易记录" }} renderItem={(txn) => (
            <List.Item key={txn.id}>
              <Space direction="vertical">
                <Text>{TXN_TYPE_LABELS[txn.txnType] ?? "余额变动"} · {yuan(txn.amountCent)}</Text>
                <Text type="secondary" data-testid="wallet-txn-split">{splitLabel(txn.principalCent, txn.bonusCent)}{txn.reason ? ` · ${txn.reason}` : ""}</Text>
                <Text type="secondary">余额 {yuan(txn.balanceAfterCent)} · {new Date(txn.createdAt).toLocaleString("zh-CN")}</Text>
              </Space>
            </List.Item>
          )} />}
        <Space>
          <Button disabled={historyLoading || historyPage === 1} onClick={() => setHistoryPage((p) => p - 1)}>上一页</Button>
          <Text>第 {historyPage} 页</Text>
          <Button disabled={historyLoading || transactions.length < 20} onClick={() => setHistoryPage((p) => p + 1)}>下一页</Button>
        </Space>
      </Card>
    </div>
  );
}
