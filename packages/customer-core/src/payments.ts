// 文件版本：V0.01
// 支付拉起判定（三端共用）。依据 OpenAPI（客人本人订单／订台发起在线付款，ROOT-03-R18）：
// 「支付渠道未启用或未配置时 launch 与 payment_url 为空、gateway_status 为 RESERVED/NOT_CONFIGURED，客户端显示「支付暂不可用，未扣款」」；
// 「渠道已收款的……直接返回那笔已成功的付款（gateway_status=SUCCEEDED，无 launch），客户端按已付处理」。
// 不认识或本端拉不起的方式一律按暂不可用处理，不显示任何支付成功。
import type { CustomerTerminalType } from "./client";
import type { CustomerPaymentLaunchView } from "./types";

export const PAYMENT_UNAVAILABLE_TEXT = "支付暂不可用，未扣款";

export type PaymentLaunchDecision =
  | { action: "paid" }
  | { action: "unavailable" }
  | { action: "open_url"; url: string }
  | { action: "wx_pay"; params: Record<string, string> };

/**
 * @param allowedPayHosts H5 支付页允许的主机（小写，外部资料到位后配置）；空表时 H5 方式一律暂不可用。
 */
export function launchDecision(
  view: CustomerPaymentLaunchView,
  terminal: CustomerTerminalType,
  allowedPayHosts: readonly string[],
): PaymentLaunchDecision {
  const launch = view.launch;
  if (view.gatewayStatus === "SUCCEEDED" && !launch) return { action: "paid" };
  if (!launch || view.gatewayStatus === "RESERVED" || view.gatewayStatus === "NOT_CONFIGURED") return { action: "unavailable" };
  if (launch.kind === "h5_url") {
    const url = launch.params?.url ?? view.paymentUrl ?? "";
    const host = /^https:\/\/([^/?#@]+)(?:[/?#]|$)/.exec(url)?.[1]?.toLowerCase();
    return host && allowedPayHosts.includes(host) ? { action: "open_url", url } : { action: "unavailable" };
  }
  if (launch.kind === "miniapp_wechat" && terminal === "miniapp" && launch.params) return { action: "wx_pay", params: launch.params };
  // app_wechat／app_alipay：App 端未接入原生支付 SDK（开放平台资料未到）；其余方式不认识。
  return { action: "unavailable" };
}
