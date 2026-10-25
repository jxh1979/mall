// 文件版本：V0.04
import { Alert, Button, Card, Empty, Input, List, Space, Spin, Tag, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import type { CustomerService } from "@complex/customer-core";
import type { MerchantCardExitNotice, StoredValueAccount, StoredValueTopupRules, StoredValueTransaction } from "@complex/customer-core";
import { formatBusinessStatus } from "@complex/customer-core";

import { OnlinePayment } from "../components/OnlinePayment";

const { Paragraph, Text } = Typography;

// 卡名：通用卡=综合体通用卡；商户卡=该发卡商户的卡（按 merchantId 判，与后端 cardScope 同一口径）。
function cardLabel(acc: StoredValueAccount): string {
  return acc.merchantId ? `商户卡（${acc.merchantId}）` : "综合体通用卡";
}

// 资金模式标签（仅商户卡）：平台代收 / 商户预收（旧卡未定格按商户预收）。
function fundLabel(acc: StoredValueAccount): string | null {
  if (!acc.merchantId) return null;
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

// ROOT-03-R30（BSM「每笔充值（时间、金额、本金与赠送金拆分、支付通路）、每笔扣款（商户、扣减的本金与赠送金各多少）」）。
function txnDetail(txn: StoredValueTransaction): string {
  const parts = [splitLabel(txn.principalCent, txn.bonusCent)];
  if (txn.txnType === "topup" && txn.paymentChannel) parts.push(`支付通路：${formatBusinessStatus(txn.paymentChannel)}`);
  if (txn.txnType === "consume") parts.push(`扣款商户：${txn.merchantName ?? "综合体"}`);
  if (txn.reason) parts.push(txn.reason);
  return parts.join(" · ");
}

const scopeOf = (acc: StoredValueAccount) => ({ projectId: acc.projectId ?? undefined, merchantId: acc.merchantId ?? undefined });

// ROOT-03-R31（ENUM「可用到哪天、能否退、退多少比例、怎么退、找谁退」）。
function noticeLines(notice: MerchantCardExitNotice): string[] {
  return [
    `可用至 ${notice.useUntil}`,
    notice.refundable ? `可退，退回比例 ${notice.refundRatioBps / 100}%` : "不可退",
    `怎么退：${notice.refundMethod ?? "见商户店面公告"}`,
    `找谁退：${notice.refundContact ?? "见商户店面公告"}`,
    `公示至 ${notice.publishUntil}`,
  ];
}

// ⑦ 我的余额（BSM「查看本人储值卡余额，含本金与赠送金分列……逐笔记录……通用卡与商户级卡分列展示」）：多卡分卡余额、逐笔记录（充值支付通路、扣款商户）；
// 充值前先读该卡的扣款顺序、赠送金性质与退卡口径并展示（ENUM「须在客人充值之前告知」，读不到不许充值）；在线充值按渠道拉起，回到前台由服务端确认
// 收款后入账（未回前台的由服务端结算作业查单入账）。商户卡退出公告按 ENUM 在客人手机应用公示（第十三批裁决第 2 条：商户自行发公告）。
export function WalletPage({ service }: { service: CustomerService }): React.ReactElement {
  const [accounts, setAccounts] = useState<StoredValueAccount[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState<string | undefined>(undefined);
  const [amountYuan, setAmountYuan] = useState("");
  const [channel, setChannel] = useState("wechat");
  const [rules, setRules] = useState<StoredValueTopupRules | null>(null);
  const [rulesError, setRulesError] = useState(false);
  const [notices, setNotices] = useState<MerchantCardExitNotice[]>([]);
  const [transactions, setTransactions] = useState<StoredValueTransaction[]>([]);
  const [historyPage, setHistoryPage] = useState(1);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [topupDone, setTopupDone] = useState(false);

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

  useEffect(() => {
    service.listMerchantCardExitNotices().then((rows) => setNotices(rows ?? [])).catch(() => setNotices([]));
  }, [service]);

  const selected = useMemo(() => accounts.find((a) => a.id === selectedId), [accounts, selectedId]);

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

  // 选卡即读该卡的充值规则（商户卡按发卡商户、通用卡按综合体）。
  useEffect(() => {
    let active = true;
    setRules(null);
    setRulesError(false);
    if (!selected) return;
    service.getStoredValueTopupRules(scopeOf(selected))
      .then((value) => { if (active) setRules(value); })
      .catch(() => { if (active) setRulesError(true); });
    return () => { active = false; };
  }, [service, selected]);

  const amountCent = Math.round(Number(amountYuan) * 100);
  const canTopup = Boolean(selected && rules && Number.isFinite(amountCent) && amountCent > 0);

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

      {notices.length ? (
        <Card title="商户储值卡退出公告" style={{ marginBottom: 16 }} data-testid="wallet-exit-notices">
          <List dataSource={notices} renderItem={(notice) => (
            <List.Item key={notice.id}>
              <Space direction="vertical" size={0}>
                <Text strong>{notice.merchantName ?? notice.merchantId}{notice.holdsCard ? <Tag color="orange" style={{ marginLeft: 8 }}>你持有该商户的卡</Tag> : null}</Text>
                {noticeLines(notice).map((line) => <Text key={line} type="secondary">{line}</Text>)}
              </Space>
            </List.Item>
          )} />
        </Card>
      ) : null}

      <Card title="我的储值卡" data-testid="wallet-cards">
        {loading ? (
          <Spin />
        ) : error ? null : accounts.length ? (
          <List
            dataSource={accounts}
            locale={{ emptyText: "暂无储值卡" }}
            renderItem={(acc) => (
              <List.Item
                data-testid="wallet-card-item"
                actions={[
                  <Button key="pick" size="small" type={acc.id === selectedId ? "primary" : "default"} onClick={() => { setSelectedId(acc.id); setHistoryPage(1); }}
                    data-testid={`wallet-pick-${acc.id}`}>
                    {acc.id === selectedId ? "已选" : "选这张"}
                  </Button>,
                ]}
              >
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

      <Card title={selected ? `充值 · ${cardLabel(selected)}` : "充值"} style={{ marginTop: 16 }}>
        {loading ? (
          <Spin />
        ) : (
          <Space direction="vertical" style={{ width: "100%" }} size="middle">
            {topupDone ? <Alert type="success" showIcon message="充值已到账" /> : null}
            <div data-testid="wallet-topup-rules">
              {rules ? (
                <Space direction="vertical" size={0}>
                  <Text strong>充值前请阅读本卡规则</Text>
                  <Text>扣款顺序：{rules.consumeOrderText}</Text>
                  <Text>赠送金：{rules.bonusRuleText}</Text>
                  <Text>退卡：{rules.refundRuleText}</Text>
                </Space>
              ) : rulesError ? (
                <Alert type="warning" showIcon message="充值规则暂时读不到，读到规则后才能充值" />
              ) : selected ? <Spin size="small" /> : <Text type="secondary">请先选一张储值卡</Text>}
            </div>
            <label>充值支付方式<select aria-label="充值支付方式" value={channel} onChange={(e) => setChannel(e.target.value)}>
              <option value="wechat">微信支付</option><option value="alipay">支付宝</option><option value="unionpay">银联</option>
            </select></label>
            <label htmlFor="wallet-amount">充值金额（元）</label>
            <Input id="wallet-amount" type="number" min={0.01} step={1} value={amountYuan} onChange={(e) => setAmountYuan(e.target.value)} />
            {selected ? (
              <OnlinePayment
                testId="wallet-topup"
                label="充值"
                disabled={!canTopup}
                start={async () => {
                  const intent = await service.topupStoredValue({ ...scopeOf(selected), amountCent, channel });
                  return { paymentIntentId: String(intent.id ?? ""), businessOrderId: selected.id, amountCent,
                    channel: (intent.channel as string | undefined) ?? null, gatewayStatus: (intent.gatewayStatus as string | undefined) ?? null,
                    paymentUrl: (intent.paymentUrl as string | undefined) ?? null,
                    launch: (intent.launch as { kind: string; params: Record<string, string> | null } | undefined) ?? null };
                }}
                confirm={async (paymentIntentId) => {
                  await service.settleStoredValueTopup({ paymentIntentId, ...scopeOf(selected) });
                  return true;
                }}
                onPaid={() => {
                  setTopupDone(true);
                  load();
                }}
              />
            ) : (
              <Button type="primary" block disabled data-testid="wallet-topup">充值</Button>
            )}
            <Paragraph type="secondary" style={{ marginBottom: 0 }}>
              在对应订单中选择储值余额支付；现场消费报手机号并验证短信后按账单扣款。商户预收卡退卡请到店办理。
            </Paragraph>
          </Space>
        )}
      </Card>
      <Card title="交易记录" style={{ marginTop: 16 }}>
        {historyError ? <Alert type="error" message={historyError} /> : historyLoading ? <Spin /> :
          <List dataSource={transactions} locale={{ emptyText: "暂无交易记录" }} renderItem={(txn) => (
            <List.Item key={txn.id}>
              <Space direction="vertical">
                <Text>{TXN_TYPE_LABELS[txn.txnType] ?? "余额变动"} · {yuan(txn.amountCent)}</Text>
                <Text type="secondary" data-testid="wallet-txn-split">{txnDetail(txn)}</Text>
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
