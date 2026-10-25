// 文件版本：V0.01
import { Alert, Button, Space } from "antd";
import { useEffect, useRef, useState } from "react";

import { PAYMENT_UNAVAILABLE_TEXT, launchDecision, type CustomerPaymentLaunchView } from "@complex/customer-core";

import { CUSTOMER_PAY_HOSTS } from "../api";
import { platform } from "../platform";

type OnlinePaymentProps = {
  /** 发起在线付款（金额由服务端定），返回渠道按本端签发的拉起参数。 */
  start: () => Promise<CustomerPaymentLaunchView>;
  /** 查单：渠道确认收款返回 true；尚未确认返回 false（保留待确认）。 */
  confirm: (paymentIntentId: string) => Promise<boolean>;
  onPaid: () => void;
  label: string;
  /** data-testid 前缀：<前缀>-pay、-unavailable、-confirm。 */
  testId: string;
  disabled?: boolean;
};

// 订台定金、扫码点单、储值充值共用的在线付款（BSM「通过官方 App/小程序……确认订单、查看支付结果」）：按 launchDecision 拉起——渠道未启用或未配置、
// 本端拉不起的方式显示「支付暂不可用，未扣款」；H5 付款页交系统打开（不在应用内加载远程网页），回到前台即查单；渠道确认收款才回调 onPaid。
export function OnlinePayment({ start, confirm, onPaid, label, testId, disabled = false }: OnlinePaymentProps): React.ReactElement {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [unavailable, setUnavailable] = useState(false);
  const [awaiting, setAwaiting] = useState<string | null>(null);
  const confirmRef = useRef(confirm);
  confirmRef.current = confirm;
  const paidRef = useRef(onPaid);
  paidRef.current = onPaid;

  const check = async (paymentIntentId: string) => {
    setBusy(true);
    setError(null);
    try {
      if (await confirmRef.current(paymentIntentId)) {
        setAwaiting(null);
        paidRef.current();
      } else {
        setAwaiting(paymentIntentId);
        setError("尚未确认收款，请完成支付后再查");
      }
    } catch (err) {
      setAwaiting(paymentIntentId);
      setError(err instanceof Error ? err.message : "支付结果确认失败");
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => (awaiting ? platform.onResume(() => void check(awaiting)) : undefined), [awaiting]);

  const pay = async () => {
    setBusy(true);
    setError(null);
    setUnavailable(false);
    setAwaiting(null);
    try {
      const view = await start();
      const decision = launchDecision(view, "app", CUSTOMER_PAY_HOSTS);
      if (decision.action === "paid") {
        setBusy(false);
        await check(view.paymentIntentId);
        return;
      }
      if (decision.action === "open_url" && (await platform.openPayment(decision.url))) setAwaiting(view.paymentIntentId);
      else setUnavailable(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "发起支付失败");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      <Button type="primary" block loading={busy} disabled={disabled} onClick={() => void pay()} data-testid={`${testId}-pay`}>
        {label}
      </Button>
      {unavailable ? <Alert type="warning" showIcon data-testid={`${testId}-unavailable`} message={PAYMENT_UNAVAILABLE_TEXT} /> : null}
      {awaiting ? (
        <Alert
          type="info"
          showIcon
          message="请在打开的付款页完成支付，回到本应用后自动确认结果"
          action={
            <Button size="small" disabled={busy} onClick={() => void check(awaiting)} data-testid={`${testId}-confirm`}>
              我已完成支付
            </Button>
          }
        />
      ) : null}
      {error ? <Alert type="error" showIcon message={error} /> : null}
    </Space>
  );
}
