// 文件版本：V0.01
import { describe, expect, it } from "vitest";

import { launchDecision } from "./payments";
import type { CustomerPaymentLaunchView } from "./types";

const view = (patch: Partial<CustomerPaymentLaunchView>): CustomerPaymentLaunchView => ({
  paymentIntentId: "pi-1",
  businessOrderId: "o-1",
  amountCent: 9900,
  channel: "wechat",
  gatewayStatus: "PENDING",
  paymentUrl: null,
  launch: null,
  ...patch,
});

describe("launchDecision（支付拉起判定）", () => {
  it("reservedNotConfiguredOrNoLaunchUnavailable", () => {
    expect(launchDecision(view({ gatewayStatus: "RESERVED" }), "app", ["pay.example"])).toEqual({ action: "unavailable" });
    expect(launchDecision(view({ gatewayStatus: "NOT_CONFIGURED" }), "miniapp", [])).toEqual({ action: "unavailable" });
    expect(launchDecision(view({ launch: null }), "app", ["pay.example"])).toEqual({ action: "unavailable" });
    expect(launchDecision(view({ gatewayStatus: "RESERVED", launch: { kind: "miniapp_wechat", params: { package: "p" } } }), "miniapp", []))
      .toEqual({ action: "unavailable" });
  });

  it("succeededWithoutLaunchIsPaid：渠道已收款的旧付款单按已付处理", () => {
    expect(launchDecision(view({ gatewayStatus: "SUCCEEDED" }), "app", [])).toEqual({ action: "paid" });
  });

  it("h5UrlMustBeHttpsAllowedHost", () => {
    const h5 = (url: string) => view({ paymentUrl: url, launch: { kind: "h5_url", params: null } });
    expect(launchDecision(h5("https://pay.example/cashier?id=1"), "app", ["pay.example"]))
      .toEqual({ action: "open_url", url: "https://pay.example/cashier?id=1" });
    expect(launchDecision(h5("http://pay.example/cashier"), "app", ["pay.example"])).toEqual({ action: "unavailable" });
    expect(launchDecision(h5("https://evil.example/cashier"), "app", ["pay.example"])).toEqual({ action: "unavailable" });
    expect(launchDecision(h5("https://pay.example@evil.example/"), "app", ["pay.example"])).toEqual({ action: "unavailable" });
    expect(launchDecision(h5("https://pay.example/cashier"), "app", [])).toEqual({ action: "unavailable" });
  });

  it("miniappWechatOnlyOnMiniapp", () => {
    const params = { timeStamp: "1700000000", package: "prepay_id=wx1", nonceStr: "n", signType: "RSA", paySign: "s" };
    const wechat = view({ launch: { kind: "miniapp_wechat", params } });
    expect(launchDecision(wechat, "miniapp", [])).toEqual({ action: "wx_pay", params });
    expect(launchDecision(wechat, "app", [])).toEqual({ action: "unavailable" });
  });

  it("appSdkKindsUnavailable：App 端未接原生支付 SDK，不假装拉起", () => {
    expect(launchDecision(view({ launch: { kind: "app_wechat", params: { prepayid: "x" } } }), "app", ["pay.example"])).toEqual({ action: "unavailable" });
    expect(launchDecision(view({ launch: { kind: "app_alipay", params: { orderInfo: "x" } } }), "app", ["pay.example"])).toEqual({ action: "unavailable" });
    expect(launchDecision(view({ launch: { kind: "unknown", params: {} } }), "miniapp", [])).toEqual({ action: "unavailable" });
  });
});
