// 文件版本：V0.01
// 注销（BSM「发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销」）、账户与条款、
// 「我的」与「酒吧」入口随终端开关隐藏（PGM「关闭后菜单不可见或不可点」）、客人链接入口（只认白名单路由）。
import path from "node:path";
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, installWx, mount, ok, q, require, ROOT, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const api = () => require(path.join(ROOT, "lib", "api.js"));
const preview = (over) => ok({
  stored_value_cards: [{ account_id: "sv1", card_scope: "project", merchant_id: null, principal_cent: 10000, bonus_cent: 500 }],
  saved_bottles: [{ id: "b1", bar_project_id: "bp1", bar_name: "蓝调酒吧", bottle_name: "芝华士", remaining_quantity: 1, unit: "瓶", expire_at: "2026-11-30T08:00:00Z" }],
  ...over,
});

beforeEach(() => freshModules());

test("deregisterRemindsSavedBottlesAndBalanceThenSubmitsChoices", async () => {
  const { wx, requests, store } = installWx({
    "GET /api/v1/customer/deregister/preview": preview({}),
    "POST /api/v1/customer/deregister": ok({ status: "deregistered" }),
    "POST /api/v1/customer/auth/logout": ok(null),
  }, TOKEN);
  const page = mount("pages/deregister/index");
  await flush();
  expect(text(q(page, "#deregister-bottles"))).toContain("蓝调酒吧 · 芝华士 1 瓶 · 到期 2026年11月30日");
  expect(text(q(page, "#deregister-cards"))).toContain("综合体通用卡：本金 ¥100.00 · 赠送金 ¥5.00");
  fire(page, ["#deregister-submit"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "POST /api/v1/customer/deregister")).toBe(false);
  expect(text(q(page, "#deregister-notice"))).toContain("放弃名下存酒");
  fire(page, ["#abandon-bottles"], "change", { value: ["abandon"] });
  fire(page, ["#waive"], "change", { value: ["waive"] });
  fire(page, ["#deregister-submit"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/deregister")?.body).toMatchObject({ waive_balance: true, abandon_saved_bottles: true });
  expect(requests.some((r) => r.key === "POST /api/v1/customer/auth/logout")).toBe(true);
  expect(store.customer_token).toBeUndefined();
  expect(wx.reLaunch).toHaveBeenCalledWith(expect.objectContaining({ url: "/pages/home/index" }));
});

test("accountShowsMaskedPhoneAndCurrentTermsVersions", async () => {
  installWx({
    "GET /api/v1/customer/session": ok({ customer_id: "c1", masked_phone: "138****0000", expires_at: null }),
    "GET /api/v1/customer/terms": ok({ member_terms: { version: "会员-2026-10", content: "会员条款正文" }, privacy_policy: { version: "隐私-2026-10", content: "隐私条款正文" } }),
  }, TOKEN);
  await api().restoreSession();
  const page = mount("pages/account/index");
  await flush();
  expect(text(q(page, "#account-phone"))).toContain("登录手机号：138****0000");
  expect(text(q(page, "#terms-member"))).toContain("会员条款（版本 会员-2026-10）");
  expect(text(q(page, "#terms-member"))).toContain("会员条款正文");
  expect(text(q(page, "#terms-privacy"))).toContain("隐私条款（版本 隐私-2026-10）");
});

test("meAndBarEntriesFollowModuleSwitches", async () => {
  const { wx } = installWx({
    "GET /api/v1/customer/terminal-switches/effective": ok([
      { terminal_type: "miniapp", capability_code: "terminal.enabled", scope_type: "module", scope_id: "vouchers", enabled: false },
      { terminal_type: "miniapp", capability_code: "terminal.enabled", scope_type: "module", scope_id: "bar", enabled: false },
    ]),
  }, TOKEN);
  await api().loadSwitches();
  const me = mount("pages/me/index");
  await flush();
  for (const key of ["points", "wallet", "bottles", "complaints", "student", "account", "deregister"]) expect(q(me, `#entry-${key}`), key).not.toBeNull();
  expect(q(me, "#entry-vouchers")).toBeNull();
  fire(me, ["#entry-points"], "tap");
  await flush();
  expect(wx.navigateTo).toHaveBeenCalledWith({ url: "/pages/points/index" });
  const bar = mount("pages/bar/index");
  await flush();
  expect(q(bar, "#bar-paused")).not.toBeNull();
  expect(q(bar, "#entry-scan")).toBeNull();
});

test("barTabListsReserveReservationsInvitationAndScan", async () => {
  const { wx } = installWx({});
  const bar = mount("pages/bar/index");
  await flush();
  for (const key of ["reserve", "reservations", "invitation", "scan"]) expect(q(bar, `#entry-${key}`), key).not.toBeNull();
  fire(bar, ["#entry-scan"], "tap");
  await flush();
  expect(wx.navigateTo).toHaveBeenCalledWith({ url: "/pages/scan/index" });
});

test("linkEntryRoutesTableVoucherClaimAndInvitation", () => {
  const cases = [
    ["https://192.168.88.160/c/table?table_id=T9", "/pages/scan/index?table_id=T9"],
    ["https://192.168.88.160/c/voucher-claim?claim_code=VCLAIM1", "/pages/vouchers/index?claim_code=VCLAIM1"],
    ["https://192.168.88.160/c/invitation?token=INVTOK1&sig=SIG1&table_id=T1&capacity=6", "/pages/invitation/index?token=INVTOK1&sig=SIG1&table_id=T1&capacity=6"],
  ];
  for (const [link, url] of cases) {
    freshModules();
    const { wx } = installWx();
    mount("pages/link/index", { query: { q: encodeURIComponent(link) } });
    expect(wx.redirectTo).toHaveBeenCalledWith({ url });
  }
});
