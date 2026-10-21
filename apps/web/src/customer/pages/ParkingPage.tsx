// 文件版本：V0.01
import { Alert, Button, Card, Descriptions, Input, Space, Typography } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService, ParkingCredit, ParkingPayment, ParkingSessionQuote } from "../client/customerService";

const { Paragraph } = Typography;

function yuan(cent: number): string {
  return `¥${(cent / 100).toFixed(2)}`;
}

function localTime(iso: string | null | undefined): string {
  return iso ? new Date(iso).toLocaleString("zh-CN", { hour12: false }) : "-";
}

// HP-P01（BSM L1435「出口扫码支付，或在官方 App/小程序提前缴费后在离场宽限时限内离场……不做车牌绑定无感支付」；
// L1433「无牌车在入口扫码领取临时码、出口凭码缴费」）：出口二维码与 App 内入口都打开本页，按车牌或临时码查、付款、看离场宽限截止。
// 「连接客人端，客人……查看消费抵免」：显示本日消费抵免；抵免把应缴抵到 0 时由客人确认使用抵免离场（记 0 元已缴，停车系统按缴费记录放行）。
const CREDIT_LABELS: Record<ParkingCredit["status"], string> = { available: "可用", used: "已使用", revoked: "已撤销（消费退款后未达门槛）",
  not_reached: "未达门槛" };
export function ParkingPage({ service, initialProjectId, initialCode }: { service: CustomerService; initialProjectId?: string;
  initialCode?: string }): React.ReactElement {
  const [projectId] = useState(initialProjectId ?? "");
  const [code, setCode] = useState(initialCode ?? "");
  const [quote, setQuote] = useState<ParkingSessionQuote | null>(null);
  const [payment, setPayment] = useState<ParkingPayment | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [credit, setCredit] = useState<ParkingCredit | null>(null);

  useEffect(() => {
    if (projectId) service.getParkingCredit(projectId).then(setCredit).catch(() => setCredit(null));
  }, [service, projectId]);

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败，请稍后再试");
    } finally {
      setBusy(false);
    }
  }

  const lookup = () => run(async () => {
    setPayment(null);
    setQuote(await service.findParkingSession(projectId, code.trim()));
  });
  const pay = () => run(async () => {
    if (quote) setPayment(await service.createParkingPayment(quote.parkingSessionId));
  });
  const confirm = () => run(async () => {
    if (payment) setPayment(await service.confirmParkingPayment(payment.id));
  });

  return (
    <Card title="停车缴费" data-testid="parking-page">
      {!projectId ? <Alert type="warning" showIcon message="请扫描停车场出口二维码或从停车场入口进入本页" /> : null}
      <Space.Compact style={{ width: "100%", marginBottom: 12 }}>
        <Input aria-label="车牌号或临时码" placeholder="车牌号或临时码" value={code} onChange={(e) => setCode(e.target.value)} />
        <Button type="primary" onClick={lookup} disabled={busy || !projectId || !code.trim()}>查询停车费</Button>
      </Space.Compact>
      {credit ? (
        <Paragraph data-testid="parking-credit">
          今日消费抵免：{CREDIT_LABELS[credit.status]}（酒吧与商户有效消费已累计 {yuan(credit.eligibleAmountCent)}，满 {yuan(credit.thresholdCent)}
          可抵 {credit.creditMinutes} 分钟停车，每日一次）
        </Paragraph>
      ) : null}
      {error ? <Alert type="error" showIcon message={error} style={{ marginBottom: 12 }} /> : null}
      {quote ? (
        <Descriptions column={1} size="small" bordered>
          <Descriptions.Item label="车辆">{quote.plateNo ?? `临时码 ${quote.tempCode ?? "-"}`}</Descriptions.Item>
          <Descriptions.Item label="入场时间">{localTime(quote.enteredAt)}</Descriptions.Item>
          <Descriptions.Item label="停车费">{yuan(quote.grossFeeCent ?? quote.feeCent)}</Descriptions.Item>
          {/* 裁决第 11 条「一分没少收＝没用掉抵免」：只在实际抵掉小时数大于 0 时显示消费抵免一行（续缴累计少收为 0 时不显示 −¥0.00）；
              金额按实际少收＝停车费−max(抵后应收, 已缴)，与后端折回小时数同口径（先缴费、后达标再续缴时已缴可高于抵后应收）。 */}
          {(quote.creditOffsetHours ?? 0) > 0 ? (
            <Descriptions.Item label={`消费抵免（${quote.creditMinutes ?? 0} 分钟）`}>
              <span data-testid="parking-credit-offset">−{yuan((quote.grossFeeCent ?? quote.feeCent) - Math.max(quote.feeCent, quote.paidCent))}</span>
            </Descriptions.Item>
          ) : null}
          <Descriptions.Item label="已缴">{yuan(quote.paidCent)}</Descriptions.Item>
          <Descriptions.Item label="应缴"><span data-testid="parking-due">{yuan(quote.dueCent)}</span></Descriptions.Item>
        </Descriptions>
      ) : null}
      {quote && (quote.dueCent > 0 || quote.creditId) && !payment ? (
        <Button type="primary" style={{ marginTop: 12 }} onClick={pay} disabled={busy}>{quote.dueCent > 0 ? "去缴费" : "使用抵免离场"}</Button>
      ) : null}
      {payment && payment.status === "pending" ? (
        <Space direction="vertical" style={{ marginTop: 12 }}>
          {payment.paymentUrl ? <a href={payment.paymentUrl}>前往支付 {yuan(payment.amountCent)}</a> : null}
          <Button onClick={confirm} disabled={busy}>我已完成支付</Button>
        </Space>
      ) : null}
      {payment && payment.status === "paid" ? (
        <Paragraph data-testid="parking-grace" style={{ marginTop: 12 }}>
          已缴费 {yuan(payment.amountCent)}，请在 {localTime(payment.graceUntil)} 前离场；超过离场宽限时限须补缴。
        </Paragraph>
      ) : null}
    </Card>
  );
}
