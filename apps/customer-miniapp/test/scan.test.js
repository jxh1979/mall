// 文件版本：V0.01
// 扫码点单与本桌账单（BSM「扫码点单只适用于散台……开台后客人扫描桌台二维码即可自助点单，下单时即时付款、付款成功订单才成立，不挂账……付款成功后
// 所点酒水计入该桌位当前账单，账单上逐项列出物品名称与价格，并分别标注已上菜与未上菜」；ENUM「非会员可以浏览、点单和付款」；
// ENUM「沽清商品在客人扫码点单菜单上置灰展示为已售完，不隐藏」）：wx.scanCode 只认本综合体的桌台码链接；未登录也能点单付款；付款只拉起服务端签发的参数并查单。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, header, installWx, mount, ok, q, text } from "./helpers.js";

const WX_PARAMS = { timeStamp: "1729756801", nonceStr: "n0nce2", package: "prepay_id=wx2410s", signType: "RSA", paySign: "c2lnbmVkMg==" };
const MENU = ok([
  { product_id: "p1", product_name: "精酿啤酒", product_type: "drink", price_cent: 3800, currency: "CNY", sold_out: false },
  { product_id: "p2", product_name: "莫吉托", product_type: "drink", price_cent: 6800, currency: "CNY", sold_out: true },
]);
const BILL = ok({
  table_id: "T9",
  items: [
    { product_name: "精酿啤酒", unit_price_cent: 3800, quantity: 2, amount_cent: 7600, served: false },
    { product_name: "果盘", unit_price_cent: 5800, quantity: 1, amount_cent: 5800, served: true },
  ],
  total_cent: 13400,
});

beforeEach(() => freshModules());

test("scanTableQrOpensMenuWithoutLoginAndGreysSoldOut", async () => {
  const { wx, requests } = installWx({ "GET /api/v1/customer/bar-tables/T9/menu": MENU, "GET /api/v1/customer/bar-tables/T9/current-bill": ok({ table_id: "T9", items: [], total_cent: 0 }) });
  const page = mount("pages/scan/index");
  await flush();
  expect(q(page, "#login-required")).toBeNull();
  wx.scanCode.mockImplementationOnce((o) => o.success({ result: "https://192.168.88.160/c/table?table_id=T9" }));
  fire(page, ["#scan-start"], "tap");
  await flush();
  const menu = requests.find((r) => r.key === "GET /api/v1/customer/bar-tables/T9/menu");
  expect(menu).toBeTruthy();
  expect(header(menu, "x-customer-token")).toBeUndefined();
  expect(header(menu, "x-module-code")).toBe("bar");
  expect(text(q(page, "#item-p1"))).toContain("精酿啤酒");
  expect(text(q(page, "#item-p2"))).toContain("已售完");
  fire(page, ["#add-p2"], "tap");
  await flush();
  expect(q(page, "#scan-pay")).toBeNull();
});

test("scanForeignQrRejected", async () => {
  const { wx, requests } = installWx({});
  const page = mount("pages/scan/index");
  await flush();
  wx.scanCode.mockImplementationOnce((o) => o.success({ result: "https://evil.example.com/c/table?table_id=T9" }));
  fire(page, ["#scan-start"], "tap");
  await flush();
  expect(text(q(page, "#scan-error"))).toContain("无法识别的二维码");
  wx.scanCode.mockImplementationOnce((o) => o.success({ result: "https://192.168.88.160/c/ticket-claim?claim_code=C1" }));
  fire(page, ["#scan-start"], "tap");
  await flush();
  expect(text(q(page, "#scan-error"))).toContain("无法识别的二维码");
  expect(requests.some((r) => r.key.includes("/bar-tables/"))).toBe(false);
});

test("scanOrderPaysImmediatelyThenCountsIntoTableBill", async () => {
  let billCalls = 0;
  const { wx, requests } = installWx({
    "GET /api/v1/customer/bar-tables/T9/menu": MENU,
    "GET /api/v1/customer/bar-tables/T9/current-bill": () => (billCalls++ === 0 ? ok({ table_id: "T9", items: [], total_cent: 0 }) : BILL),
    "POST /api/v1/customer/bar-tables/T9/scan-orders": ok({
      scan_order_id: "so1", table_id: "T9", amount_cent: 7600, status: "pending_payment", bar_order_id: null, payment_intent_id: "pi-s1",
      channel: "wechat", gateway_status: "PENDING", payment_url: null, launch: { kind: "miniapp_wechat", params: WX_PARAMS },
    }),
    "POST /api/v1/customer/bar-tables/T9/scan-orders/so1/confirm": ok({
      scan_order_id: "so1", table_id: "T9", amount_cent: 7600, status: "paid", bar_order_id: "bo1", payment_intent_id: "pi-s1",
    }),
  });
  const page = mount("pages/scan/index", { query: { table_id: "T9" } });
  await flush();
  fire(page, ["#add-p1"], "tap");
  fire(page, ["#add-p1"], "tap");
  await flush();
  expect(text(q(page, "#scan-pay"))).toContain("下单并支付 ¥76.00");
  fire(page, ["#scan-pay"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/bar-tables/T9/scan-orders")?.body)
    .toMatchObject({ items: [{ product_id: "p1", quantity: 2 }] });
  const { success, fail, complete, ...passed } = wx.requestPayment.mock.calls[0][0];
  expect(passed).toEqual(WX_PARAMS);
  expect(requests.some((r) => r.key === "POST /api/v1/customer/bar-tables/T9/scan-orders/so1/confirm")).toBe(true);
  expect(text(q(page, "#scan-paid"))).toContain("付款成功，已计入本桌账单");
  const bill = text(q(page, "#bill"));
  expect(bill).toContain("精酿啤酒 ¥38.00 × 2");
  expect(bill).toContain("未上菜");
  expect(bill).toContain("已上菜");
  expect(bill).toContain("合计 ¥134.00");
});
