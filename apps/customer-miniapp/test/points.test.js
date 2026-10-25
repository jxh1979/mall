// 文件版本：V0.01
// 积分、签到与兑换（BSM「查看会员积分摘要和会员等级；积分明细、礼物列表、兑换发起和兑换记录」）：积分明细用中文来源名（PGM「界面全中文」）；
// 签到；兑换按目录扣积分；有待追回积分时暂停兑换（门票奖励口径，与 App 同）。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const routes = (over = {}) => ({
  "GET /api/v1/customer/points": ok({ customer_id: "c1", balance: 500, level: "silver", recent_summary: null }),
  "GET /api/v1/customer/points/ledger": ok([
    { id: "l1", points_delta: 10, balance_after: 500, source_type: "check_in", change_type: "earn", status: "confirmed", created_at: "2026-10-24T01:00:00Z" },
    { id: "l2", points_delta: -100, balance_after: 490, source_type: "points_redemption", change_type: "deduct", status: "confirmed", created_at: "2026-10-23T01:00:00Z" },
  ]),
  "GET /api/v1/customer/check-ins": ok([]),
  "GET /api/v1/customer/points/redemption-items": ok([
    { id: "g1", item_name: "纪念徽章", item_type: "physical", points_cost: 100, stock_quantity: 5, status: "active" },
    { id: "g2", item_name: "大礼包", item_type: "physical", points_cost: 1000, stock_quantity: 3, status: "active" },
  ]),
  "GET /api/v1/customer/points/redemptions": ok([]),
  "GET /api/v1/customer/ticket-rewards": ok({ rewards: [], pending_recovery: 0 }),
  "GET /api/v1/customer/ticket-reward-rules": ok([]),
  "GET /api/v1/customer/merchant-rewards": ok([]),
  ...over,
});

beforeEach(() => freshModules());

test("pointsLedgerCheckInAndRedeem", async () => {
  const { wx, requests } = installWx(routes({
    "POST /api/v1/customer/check-ins": ok({ id: "ci1", customer_id: "c1", check_in_date: "2026-10-24", streak: 3, awarded_points: 5, source_terminal: "miniapp", created_at: "2026-10-24T01:00:00Z" }),
    "POST /api/v1/customer/points/redemptions": ok({ id: "rd1", item_name: "纪念徽章", quantity: 1, total_points: 100, status: "completed", created_at: "2026-10-24T01:00:00Z" }),
  }), TOKEN);
  const page = mount("pages/points/index");
  await flush();
  expect(text(q(page, "#points-summary"))).toContain("积分余额 500");
  const ledger = text(q(page, "#points-ledger"));
  expect(ledger).toContain("签到 +10 · 余额 500");
  expect(ledger).toContain("积分兑换 -100 · 余额 490");
  expect(text(q(page, "#redeem-g2"))).toContain("积分不足");
  fire(page, ["#check-in"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "POST /api/v1/customer/check-ins")).toBe(true);
  expect(wx.showToast.mock.calls.map((c) => c[0].title)).toContain("签到成功，连续 3 天，获 5 积分");
  fire(page, ["#redeem-g1"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/points/redemptions")?.body).toMatchObject({ catalog_item_id: "g1", quantity: 1 });
  expect(requests.filter((r) => r.key === "GET /api/v1/customer/points")).toHaveLength(3);
});

test("redeemPausedWhilePointsPendingRecovery", async () => {
  const { requests } = installWx(routes({ "GET /api/v1/customer/ticket-rewards": ok({ rewards: [], pending_recovery: 30 }) }), TOKEN);
  const page = mount("pages/points/index");
  await flush();
  expect(text(q(page, "#points-recovery"))).toContain("待追回 30 积分");
  fire(page, ["#redeem-g1"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "POST /api/v1/customer/points/redemptions")).toBe(false);
});
