// 文件版本：V0.01
// 储值与商户卡退出公告（BSM「我的余额：查看本人储值卡余额，含本金与赠送金分列……每笔充值（时间、金额、本金与赠送金拆分、支付通路）、每笔扣款（商户……）；
// 通用卡与商户级卡分列展示」；ENUM「储值卡的消费扣款顺序、赠送金的性质与退卡口径，须在客人充值之前告知」；ENUM「可用到哪天、能否退、退多少比例、怎么退、
// 找谁退」；第十三批裁决第 2 条「商户自己发，不卡」）：读不到充值规则不许充值；充值只拉起服务端签发的小程序微信支付参数，回来由服务端确认收款后才算到账。
import { beforeEach, expect, test } from "vitest";

import { fail, fire, flush, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const WX_PARAMS = { timeStamp: "1729756802", nonceStr: "n0nce3", package: "prepay_id=wx2410t", signType: "RSA", paySign: "c2lnbmVkMw==" };
const RULES = {
  card_scope: "project", project_id: "p1", merchant_id: null, bonus_bps: 500, consume_order: "principal_first",
  consume_order_text: "先扣本金后扣赠送金", bonus_rule_text: "赠送金不可提现、不可退", refund_rule_text: "退卡只退本金", refund_allowed: true, refund_ratio_bps: null,
};
const routes = (over = {}) => ({
  "GET /api/v1/customer/stored-value/accounts": ok([
    { id: "sv1", customer_id: "c1", project_id: "p1", merchant_id: null, card_scope: "project", fund_ownership: null, balance_cent: 15000,
      principal_cent: 10000, bonus_cent: 5000, status: "active", created_at: "2026-01-01T00:00:00Z", updated_at: "2026-10-01T00:00:00Z" },
    { id: "sv2", customer_id: "c1", project_id: "p1", merchant_id: "m-bar", card_scope: "merchant", fund_ownership: "merchant_precollect", balance_cent: 2000,
      principal_cent: 2000, bonus_cent: 0, status: "active", created_at: "2026-01-01T00:00:00Z", updated_at: "2026-10-01T00:00:00Z" },
  ]),
  "GET /api/v1/customer/merchant-card-exit-notices": ok([
    { id: "n1", merchant_id: "m-bar", merchant_name: "蓝调酒吧", project_id: "p1", use_until: "2026-12-31", refundable: true, refund_ratio_bps: 8000,
      refund_method: "到店退回原支付方式", refund_contact: "前台服务台", publish_from: "2026-10-01", publish_until: "2026-11-30", holds_card: true },
  ]),
  "GET /api/v1/customer/stored-value/topup-rules": ok(RULES),
  "GET /api/v1/customer/stored-value/accounts/sv1/transactions": ok([
    { id: "t1", account_id: "sv1", customer_id: "c1", txn_type: "topup", amount_cent: 10000, balance_after_cent: 15000, principal_cent: 10000,
      bonus_cent: 500, reason: null, created_at: "2026-10-20T08:00:00Z", payment_channel: "wechat", merchant_id: null, merchant_name: null },
    { id: "t2", account_id: "sv1", customer_id: "c1", txn_type: "consume", amount_cent: -3000, balance_after_cent: 12000, principal_cent: -3000,
      bonus_cent: 0, reason: null, created_at: "2026-10-21T08:00:00Z", payment_channel: null, merchant_id: "m-bar", merchant_name: "蓝调酒吧" },
  ]),
  ...over,
});

beforeEach(() => freshModules());

test("walletCardsSplitExitNoticeAndTransactionDetails", async () => {
  const { requests } = installWx(routes(), TOKEN);
  const page = mount("pages/wallet/index");
  await flush();
  const general = text(q(page, "#card-sv1"));
  expect(general).toContain("综合体通用卡");
  expect(general).toContain("余额 ¥150.00");
  expect(general).toContain("本金 ¥100.00 · 赠送金 ¥50.00");
  expect(text(q(page, "#card-sv2"))).toContain("商户卡");
  expect(text(q(page, "#card-sv2"))).toContain("商户预收");
  const notices = text(q(page, "#exit-notices"));
  for (const line of ["蓝调酒吧", "你持有该商户的卡", "可用至 2026-12-31", "可退，退回比例 80%", "怎么退：到店退回原支付方式", "找谁退：前台服务台", "公示至 2026-11-30"]) {
    expect(notices).toContain(line);
  }
  expect(requests.find((r) => r.key === "GET /api/v1/customer/stored-value/topup-rules")?.path ?? "").toContain("project_id=p1");
  const rules = text(q(page, "#topup-rules"));
  expect(rules).toContain("扣款顺序：先扣本金后扣赠送金");
  expect(rules).toContain("赠送金：赠送金不可提现、不可退");
  expect(rules).toContain("退卡：退卡只退本金");
  const txns = text(q(page, "#transactions"));
  expect(txns).toContain("充值 · ¥100.00");
  expect(txns).toContain("支付通路：");
  expect(txns).toContain("扣款商户：蓝调酒吧");
});

test("topupBlockedWithoutRules", async () => {
  const { requests } = installWx(routes({ "GET /api/v1/customer/stored-value/topup-rules": fail(500, null, "服务异常") }), TOKEN);
  const page = mount("pages/wallet/index");
  await flush();
  expect(text(q(page, "#topup-rules"))).toContain("充值规则暂时读不到，读到规则后才能充值");
  fire(page, ["#topup-amount"], "input", { value: "100" });
  fire(page, ["#topup-pay"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "POST /api/v1/customer/stored-value/topup")).toBe(false);
});

test("topupPassesLaunchParamsUnchangedAndCreditsOnlyAfterServerSettles", async () => {
  const { wx, requests } = installWx(routes({
    "POST /api/v1/customer/stored-value/topup": ok({ id: "pi-t1", status: "pending", channel: "wechat", gateway_status: "PENDING", payment_url: null,
      launch: { kind: "miniapp_wechat", params: WX_PARAMS } }),
    "POST /api/v1/customer/stored-value/settle": ok({ id: "t9", account_id: "sv1", txn_type: "topup", amount_cent: 10000, balance_after_cent: 25000 }),
  }), TOKEN);
  const page = mount("pages/wallet/index");
  await flush();
  fire(page, ["#topup-amount"], "input", { value: "100" });
  fire(page, ["#topup-pay"], "tap");
  await flush();
  const topup = requests.find((r) => r.key === "POST /api/v1/customer/stored-value/topup");
  expect(topup.body).toMatchObject({ project_id: "p1", amount_cent: 10000, channel: "wechat" });
  expect(topup.body.merchant_id).toBeUndefined();
  const { success, fail: failed, complete, ...passed } = wx.requestPayment.mock.calls[0][0];
  expect(passed).toEqual(WX_PARAMS);
  expect(requests.find((r) => r.key === "POST /api/v1/customer/stored-value/settle")?.body).toMatchObject({ payment_intent_id: "pi-t1", project_id: "p1" });
  expect(text(q(page, "#topup-done"))).toContain("充值已到账");
});
