// 文件版本：V0.01
import { Alert, Button, Input, Modal, Select, Space, Typography, message } from "antd";
import { useEffect, useState } from "react";

import {
  PAYMENT_UNAVAILABLE_TEXT,
  launchDecision,
  type CustomerService,
  type StoredValueAccount,
  type TicketOrder,
} from "@complex/customer-core";

import { CUSTOMER_PAY_HOSTS } from "../api";
import { platform } from "../platform";

const { Text } = Typography;

const yuan = (cent: number) => `¥${(cent / 100).toFixed(2)}`;

/** 待付＝订单视图的 remaining_cent；下单接口只回订单原字段时按订单金额减已付。 */
export function remainingOf(order: TicketOrder): number {
  return order.remainingCent ?? Math.max(0, order.amountCent - (order.paidAmountCent ?? 0));
}

function cardAppliesToOrder(card: StoredValueAccount, order: TicketOrder): boolean {
  return card.status === "active" && card.balanceCent > 0
    && (card.projectId === null || card.projectId === order.projectId)
    && (card.merchantId === null || card.merchantId === order.barId);
}

// 本人待付订单的组合支付（操作员原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」；BSM「通过官方 App/小程序购票、预定、
// 确认订单、查看支付结果」）：储值卡先扣（卡内余额不足待付时只扣卡内余额，不让客人填扣款金额），余下在线补足；在线付款金额由服务端定。
// 在线付款按 launchDecision：渠道未启用或未配置、或本端拉不起的方式显示「支付暂不可用，未扣款」；H5 付款页交系统打开（不在应用内加载
// 远程网页），回到前台即查单，渠道确认收款才算付成；付清与否以服务端订单视图为准，只有付清才由上层显示支付成功。
export function OrderPayment({ service, order, onChange }: {
  service: CustomerService;
  order: TicketOrder;
  onChange: (order: TicketOrder) => void;
}): React.ReactElement {
  const remaining = remainingOf(order);
  const [online, setOnline] = useState<{ busy: boolean; error: string | null; unavailable: boolean; awaitingIntentId: string | null }>(
    { busy: false, error: null, unavailable: false, awaitingIntentId: null });
  const [modalOpen, setModalOpen] = useState(false);
  const [cards, setCards] = useState<StoredValueAccount[]>([]);
  const [cardId, setCardId] = useState<string>();
  const [phone, setPhone] = useState("");
  const [smsCode, setSmsCode] = useState("");
  const [svBusy, setSvBusy] = useState(false);
  const [svError, setSvError] = useState<string | null>(null);

  const card = cards.find((item) => item.id === cardId);
  // 卡内余额不足待付时只扣卡内余额，其余在线补足；够付则不传金额（付清待付）。
  const storedValueAmount = card ? Math.min(card.balanceCent, remaining) : 0;

  const confirmOnline = async (paymentIntentId: string) => {
    setOnline((state) => ({ ...state, busy: true, error: null }));
    try {
      const next = await service.confirmTicketPayment(order.id, paymentIntentId);
      setOnline({ busy: false, error: null, unavailable: false, awaitingIntentId: next.payStatus === "paid" ? null : paymentIntentId });
      onChange(next);
    } catch (err) {
      setOnline((state) => ({ ...state, busy: false, error: err instanceof Error ? err.message : "支付结果确认失败" }));
    }
  };

  // 付款页在系统里打开，客人付完回到本应用即查单。
  const awaiting = online.awaitingIntentId;
  useEffect(() => (awaiting ? platform.onResume(() => void confirmOnline(awaiting)) : undefined), [awaiting]);

  const payOnline = async () => {
    setOnline({ busy: true, error: null, unavailable: false, awaitingIntentId: null });
    try {
      const view = await service.startTicketPayment(order.id);
      const decision = launchDecision(view, "app", CUSTOMER_PAY_HOSTS);
      if (decision.action === "paid") {
        await confirmOnline(view.paymentIntentId);
        return;
      }
      if (decision.action === "open_url" && (await platform.openPayment(decision.url))) {
        setOnline({ busy: false, error: null, unavailable: false, awaitingIntentId: view.paymentIntentId });
        return;
      }
      setOnline({ busy: false, error: null, unavailable: true, awaitingIntentId: null });
    } catch (err) {
      setOnline({ busy: false, error: err instanceof Error ? err.message : "发起支付失败", unavailable: false, awaitingIntentId: null });
    }
  };

  const openStoredValue = async () => {
    setModalOpen(true);
    setCards([]);
    setCardId(undefined);
    setPhone("");
    setSmsCode("");
    setSvError(null);
    setSvBusy(true);
    try {
      const own = await service.getStoredValueAccounts();
      setCards(own);
      setCardId(own.find((item) => cardAppliesToOrder(item, order))?.id);
    } catch (err) {
      setSvError(err instanceof Error ? err.message : "储值卡加载失败");
    } finally {
      setSvBusy(false);
    }
  };

  const requestCode = async () => {
    if (!phone.trim()) {
      setSvError("请填写本人注册手机号");
      return;
    }
    setSvBusy(true);
    setSvError(null);
    try {
      await service.requestStoredValueCode(phone.trim());
      void message.success("验证码已发送");
    } catch (err) {
      setSvError(err instanceof Error ? err.message : "验证码发送失败");
    } finally {
      setSvBusy(false);
    }
  };

  const payStoredValue = async () => {
    if (!card || !phone.trim() || !smsCode.trim()) return;
    setSvBusy(true);
    setSvError(null);
    try {
      const next = await service.payTicketOrderWithStoredValue(order.id, {
        accountId: card.id,
        phone: phone.trim(),
        smsCode: smsCode.trim(),
        ...(storedValueAmount < remaining ? { amountCent: storedValueAmount } : {}),
      });
      setModalOpen(false);
      onChange(next);
    } catch (err) {
      setSvError(err instanceof Error ? err.message : "余额支付失败");
    } finally {
      setSvBusy(false);
    }
  };

  return (
    <div data-testid="order-payment">
      <Space direction="vertical" style={{ width: "100%" }}>
        <Text data-testid="order-payment-remaining">
          应付 {yuan(order.amountCent)} · 已付 {yuan(order.amountCent - remaining)} · 还需支付 {yuan(remaining)}（全部付清才算完成）
        </Text>
        <Space wrap>
          <Button onClick={() => void openStoredValue()} disabled={online.busy}>储值余额支付</Button>
          <Button type="primary" onClick={() => void payOnline()} loading={online.busy} data-testid="order-pay-online">
            {order.amountCent > remaining ? "在线支付补足" : "在线支付"}
          </Button>
        </Space>
        {online.unavailable ? (
          <Alert type="warning" showIcon data-testid="order-payment-unavailable" message={PAYMENT_UNAVAILABLE_TEXT} />
        ) : null}
        {awaiting ? (
          <Alert
            type="info"
            showIcon
            message="请在打开的付款页完成支付，回到本应用后自动确认结果"
            action={
              <Button size="small" onClick={() => void confirmOnline(awaiting)} disabled={online.busy} data-testid="order-payment-confirm">
                我已完成支付
              </Button>
            }
          />
        ) : null}
        {online.error ? <Alert type="error" showIcon message={online.error} /> : null}
      </Space>
      <Modal title="订单储值支付" open={modalOpen} onCancel={() => setModalOpen(false)}
        closable={!svBusy} maskClosable={false} keyboard={!svBusy}
        okText="确认余额支付" cancelText="取消" confirmLoading={svBusy}
        okButtonProps={{ disabled: svBusy || !card || !phone.trim() || !smsCode.trim() }}
        cancelButtonProps={{ disabled: svBusy }} onOk={() => void payStoredValue()}>
        {svError ? <Alert type="error" message={svError} /> : null}
        <Space direction="vertical" style={{ width: "100%" }}>
          <Text>本订单还需支付 {yuan(remaining)}</Text>
          <Select aria-label="付款储值卡" value={cardId} onChange={setCardId} style={{ width: "100%" }}
            options={cards.map((item) => ({ value: item.id,
              disabled: !cardAppliesToOrder(item, order),
              label: `${item.merchantId ? "商户卡（" + item.merchantId + "）" : "综合体通用卡"} · 余额 ${yuan(item.balanceCent)}${cardAppliesToOrder(item, order) ? "" : " · 不适用于该订单"}` }))} />
          {card ? (
            <Text data-testid="order-payment-stored-value-amount">
              本次从储值卡扣 {yuan(storedValueAmount)}{storedValueAmount < remaining ? `，余下 ${yuan(remaining - storedValueAmount)} 请在线补足` : ""}
            </Text>
          ) : null}
          <label htmlFor={`order-payment-phone-${order.id}`}>付款手机号</label>
          <Input id={`order-payment-phone-${order.id}`} type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <Button disabled={svBusy} onClick={() => void requestCode()}>发送扣款验证码</Button>
          <label htmlFor={`order-payment-code-${order.id}`}>短信验证码</label>
          <Input id={`order-payment-code-${order.id}`} autoComplete="one-time-code" inputMode="numeric" value={smsCode}
            onChange={(e) => setSmsCode(e.target.value)} />
        </Space>
      </Modal>
    </div>
  );
}
