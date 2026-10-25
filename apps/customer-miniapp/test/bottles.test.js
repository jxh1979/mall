// 文件版本：V0.01
// 我的存酒与取酒码（BSM「我的存酒：查看本人名下全部存酒、到期日与动态取酒码……客人端「我的存酒」为跨店汇总展示，每条记录标明所属酒吧，取酒须到该酒吧办理。
// 门店退出后仍在保管有效期内的存酒……由所属娱乐平台公司接收，并在指定地点领取」；ENUM「转存须客人本人短信验证码确认」）：取酒码是按全场统一间隔刷新的
// 动态码，只画进二维码、不以文字出现。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const bottle = (over) => ({
  id: "b1", bar_project_id: "bp1", bar_name: "蓝调酒吧", bottle_name: "芝华士 12 年", remaining_quantity: 0.5, unit: "瓶",
  expire_at: "2026-11-30T08:00:00Z", status: "stored", bottle_label_value: null, pickup_location: null, handed_over_to_platform: false, ...over,
});
const ROUTES = {
  "GET /api/v1/customer/my-saved-bottles": ok([
    bottle({}),
    bottle({ id: "b2", bar_name: "夜色酒吧", bottle_name: "轩尼诗", remaining_quantity: 1, handed_over_to_platform: true, pickup_location: "综合体一楼服务台" }),
  ]),
};

beforeEach(() => freshModules());

test("bottlesListOwningBarAndShowDynamicPickupCode", async () => {
  const { requests } = installWx({
    ...ROUTES,
    "GET /api/v1/customer/saved-bottles/b1/pickup-code": ok({ saved_bottle_id: "b1", code: "PICK-1", expires_at: "2026-10-24T08:00:15Z", refresh_interval_seconds: 15 }),
  }, TOKEN);
  const page = mount("pages/bottles/index");
  await flush();
  const first = text(q(page, "#bottle-b1"));
  expect(first).toContain("蓝调酒吧");
  expect(first).toContain("芝华士 12 年 · 剩余 0.5 瓶");
  expect(first).toContain("到期 2026年11月30日");
  expect(first).toContain("取酒须到蓝调酒吧办理");
  expect(text(q(page, "#bottle-b2"))).toContain("门店已退出，请到综合体一楼服务台领取");
  fire(page, ["#code-b1"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "GET /api/v1/customer/saved-bottles/b1/pickup-code")).toBe(true);
  expect(q(page, "#bottle-code", "#qr-image")).not.toBeNull();
  expect(text(page)).not.toContain("PICK-1");
});

test("renewNeedsOwnSmsCode", async () => {
  const { requests } = installWx({
    ...ROUTES,
    "POST /api/v1/customer/saved-bottles/b1/renewal-code": ok({ saved_bottle_id: "b1", requested_at: "2026-10-24T08:00:00Z" }),
    "POST /api/v1/customer/saved-bottles/b1/renew": ok({ id: "rn1", saved_bottle_id: "b1", new_expire_at: "2026-12-30T08:00:00Z", status: "renewed" }),
  }, TOKEN);
  const page = mount("pages/bottles/index");
  await flush();
  fire(page, ["#renew-b1"], "tap");
  await flush();
  fire(page, ["#renew-send"], "tap");
  await flush();
  expect(requests.some((r) => r.key === "POST /api/v1/customer/saved-bottles/b1/renewal-code")).toBe(true);
  fire(page, ["#renew-sms"], "input", { value: "123456" });
  fire(page, ["#renew-confirm"], "tap");
  await flush();
  expect(requests.find((r) => r.key === "POST /api/v1/customer/saved-bottles/b1/renew")?.body).toMatchObject({ sms_code: "123456" });
  expect(text(q(page, "#renew-result"))).toContain("已转存，新到期日 2026年12月30日");
  expect(requests.filter((r) => r.key === "GET /api/v1/customer/my-saved-bottles")).toHaveLength(2);
});
