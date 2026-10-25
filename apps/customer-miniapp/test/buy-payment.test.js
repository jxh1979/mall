// 文件版本：V0.01
// 购票与组合支付：购票前展示门票守则与我的价格（BSM「购票前须以门票守则的形式向客人告知该票的规则，包括能否退款、有效期、年龄限制与可使用范围」）；
// 储值先扣、余下在线补足（操作员原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」）；在线付款只把后端签发的小程序微信支付参数
// 原样交给 wx.requestPayment，未配置显示「支付暂不可用，未扣款」；拉起后一律查单，付清才显示支付成功。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, header, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const PRODUCTS = [
  {
    ticket_type_id: "t-pass", ticket_name: "单人通票", weekday_price_cent: 8800, weekend_price_cent: 9900, holiday_price_cent: 9900,
    currency: "CNY", status: "active", bar_id: null, bar_name: null, my_price_cent: 7800, my_price_basis: "会员价",
    refundable: true, validity_days: 30, usage_scope: "全场通用", entitlements: ["入场", "游乐一次"], age_limit_text: "须年满 18 周岁",
    project_id: "p1",
  },
  {
    ticket_type_id: "t-bar", ticket_name: "酒吧单店票", weekday_price_cent: 5000, weekend_price_cent: 5000, holiday_price_cent: 5000,
    currency: "CNY", status: "active", bar_id: "bar-1", bar_name: "蓝调酒吧", my_price_cent: 5000, my_price_basis: null,
    refundable: false, validity_days: null, usage_scope: null, entitlements: [], age_limit_text: null,
    project_id: "p2",
  },
];
const ORDER = {
  id: "o1", order_no: "T20261024001", customer_id: "c1", ticket_type_id: "t-bar", price_rule_id: null, quantity: 1, visit_date: null,
  sales_channel: "miniapp", buyer_phone: "13800000000", amount_cent: 5000, currency: "CNY", pay_status: "unpaid", issue_status: "not_issued",
  status: "created", created_at: "2026-10-24T08:00:00Z", updated_at: "2026-10-24T08:00:00Z", bar_id: "bar-1", project_id: "p1",
};
const PAID = { ...ORDER, pay_status: "paid", issue_status: "issued", status: "paid", paid_amount_cent: 5000, remaining_cent: 0 };
const WX_PARAMS = { timeStamp: "1729756800", nonceStr: "n0nce", package: "prepay_id=wx2410", signType: "RSA", paySign: "c2lnbmVk" };

beforeEach(() => freshModules());

async function orderedPage(routes, query = {}) {
  const env = installWx({
    "GET /api/v1/customer/ticket-products": ok(PRODUCTS),
    "POST /api/v1/customer/ticket-orders": ok(ORDER),
    ...routes,
  }, TOKEN);
  const page = mount("pages/buy/index", { query });
  await flush();
  fire(page, ["#product-picker"], "change", { value: "1" });
  fire(page, ["#submit"], "tap");
  await flush();
  return { ...env, page };
}

// PGM L736「可以按全局、综合体……开关」：购票请求带所选票种所属的综合体（票种列表每项带 project_id），后端按综合体的终端开关拒绝。
test("orderCarriesSelectedProductProject", async () => {
  const { requests } = await orderedPage({});
  expect(header(requests.find((r) => r.key === "POST /api/v1/customer/ticket-orders"), "x-project-id")).toBe("p2");
});

test("buyShowsTicketRulesAndMyPriceBeforeOrdering", async () => {
  installWx({ "GET /api/v1/customer/ticket-products": ok(PRODUCTS) }, TOKEN);
  const page = mount("pages/buy/index");
  await flush();
  fire(page, ["#product-picker"], "change", { value: "0" });
  await flush();
  const rules = text(q(page, "#ticket-rules"));
  expect(rules).toContain("按票种规则可申请退款，开始核销后不可退");
  expect(rules).toContain("30 天");
  expect(rules).toContain("须年满 18 周岁");
  expect(rules).toContain("全场通用");
  expect(rules).toContain("入场、游乐一次");
  expect(text(q(page, "#my-price"))).toContain("我的价格 ¥78.00（会员价），系统已按您的资格自动取最低价");
});

test("lockedBarEntryListsThatBarAndOrdersWithBarId", async () => {
  const { requests, page } = await orderedPage({}, { bar_id: "bar-1" });
  expect((requests.find((r) => r.key === "GET /api/v1/customer/ticket-products")?.path ?? "")).toContain("bar_id=bar-1");
  expect(text(q(page, "#locked-bar"))).toContain("蓝调酒吧");
  const order = requests.find((r) => r.key === "POST /api/v1/customer/ticket-orders");
  expect(order.body).toMatchObject({ ticket_type_id: "t-bar", quantity: 1, bar_id: "bar-1" });
  expect(text(q(page, "#payment", "#pay-summary"))).toContain("还需支付 ¥50.00（全部付清才算完成）");
});

test("paymentPassesLaunchParamsUnchangedThenQueriesBeforeSuccess", async () => {
  const { wx, requests, page } = await orderedPage({
    "POST /api/v1/customer/orders/o1/payments": ok({
      payment_intent_id: "pi-1", business_order_id: "o1", amount_cent: 5000, channel: "wechat", gateway_status: "PENDING",
      payment_url: null, launch: { kind: "miniapp_wechat", params: WX_PARAMS },
    }),
    "POST /api/v1/customer/orders/o1/payments/pi-1/confirm": ok(PAID),
  });
  fire(page, ["#payment", "#pay-online"], "tap");
  await flush();
  expect(wx.requestPayment).toHaveBeenCalledTimes(1);
  const { success, fail, complete, ...passed } = wx.requestPayment.mock.calls[0][0];
  expect(passed).toEqual(WX_PARAMS);
  expect(requests.some((r) => r.key === "POST /api/v1/customer/orders/o1/payments/pi-1/confirm")).toBe(true);
  expect(text(q(page, "#paid"))).toContain("支付成功");
});

test("paymentWithoutLaunchShowsUnavailableAndNoSuccess", async () => {
  const { wx, requests, page } = await orderedPage({
    "POST /api/v1/customer/orders/o1/payments": ok({
      payment_intent_id: "pi-2", business_order_id: "o1", amount_cent: 5000, channel: null, gateway_status: "NOT_CONFIGURED",
      payment_url: null, launch: null,
    }),
  });
  fire(page, ["#payment", "#pay-online"], "tap");
  await flush();
  expect(wx.requestPayment).not.toHaveBeenCalled();
  expect(text(q(page, "#payment", "#pay-unavailable"))).toContain("支付暂不可用，未扣款");
  expect(requests.some((r) => r.key.endsWith("/confirm"))).toBe(false);
  expect(q(page, "#paid")).toBeNull();
});

test("cancelledWechatPaymentStillQueriesAndShowsNoSuccess", async () => {
  const { wx, requests, page } = await orderedPage({
    "POST /api/v1/customer/orders/o1/payments": ok({
      payment_intent_id: "pi-3", business_order_id: "o1", amount_cent: 5000, channel: "wechat", gateway_status: "PENDING",
      payment_url: null, launch: { kind: "miniapp_wechat", params: WX_PARAMS },
    }),
    "POST /api/v1/customer/orders/o1/payments/pi-3/confirm": ok({ ...ORDER, remaining_cent: 5000, paid_amount_cent: 0 }),
  });
  wx.requestPayment.mockImplementation((o) => {
    o.fail?.({ errMsg: "requestPayment:fail cancel" });
    o.complete?.({});
  });
  fire(page, ["#payment", "#pay-online"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "POST /api/v1/customer/orders/o1/payments/pi-3/confirm")).toBe(true);
  expect(q(page, "#paid")).toBeNull();
  expect(text(q(page, "#payment", "#pay-error"))).toContain("尚未确认收款");
});

test("storedValueShortfallPaysCardBalanceOnlyThenOnlineTopUp", async () => {
  const { requests, page } = await orderedPage({
    "GET /api/v1/customer/stored-value/accounts": ok([
      {
        id: "sv1", customer_id: "c1", project_id: null, merchant_id: null, card_scope: "general", fund_ownership: null, balance_cent: 3000,
        principal_cent: 3000, bonus_cent: 0, status: "active", created_at: "2026-01-01T00:00:00Z", updated_at: "2026-01-01T00:00:00Z",
      },
    ]),
    "POST /api/v1/customer/stored-value/sms": ok({}),
    "POST /api/v1/customer/orders/o1/stored-value/pay": ok({ ...ORDER, paid_amount_cent: 3000, remaining_cent: 2000 }),
  });
  fire(page, ["#payment", "#sv-open"], "tap");
  await flush();
  expect(text(q(page, "#payment", "#sv-amount"))).toContain("本次从储值卡扣 ¥30.00，余下 ¥20.00 请在线补足");
  fire(page, ["#payment", "#sv-phone"], "input", { value: "13800000000" });
  fire(page, ["#payment", "#sv-send"], "tap");
  await flush();
  fire(page, ["#payment", "#sv-code"], "input", { value: "654321" });
  fire(page, ["#payment", "#sv-pay"], "tap");
  await flush();
  const paid = requests.find((r) => r.key === "POST /api/v1/customer/orders/o1/stored-value/pay");
  expect(paid.body).toMatchObject({ account_id: "sv1", phone: "13800000000", sms_code: "654321", amount_cent: 3000 });
  expect(text(q(page, "#payment", "#pay-summary"))).toContain("还需支付 ¥20.00");
  expect(text(q(page, "#payment", "#pay-online"))).toContain("在线支付补足");
  expect(q(page, "#paid")).toBeNull();
});
