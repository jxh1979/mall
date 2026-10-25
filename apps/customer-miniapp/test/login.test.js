// 文件版本：V0.01
// 登录与注册（BSM「创建会员身份必须完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款和隐私条款」；
// 第十三批裁决第 3 条「满 18 周岁」由服务端判定）：未注册手机号登录不建会话、转注册；生日与两份条款逐份同意缺一不提交。
import { beforeEach, expect, test } from "vitest";

import { fire, fail, flush, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

const TERMS = {
  member_terms: { version: "会员条款第3版", content: "会员条款正文" },
  privacy_policy: { version: "隐私条款第2版", content: "隐私条款正文" },
};

beforeEach(() => freshModules());

test("loginUnknownPhoneGoesToRegisterThenRegistersWithBirthDateAndBothConsents", async () => {
  const { wx, requests, store } = installWx({
    "POST /api/v1/customer/auth/sms-codes": ok({ phone_masked: "138****0000", requested_at: "2026-10-24T08:00:00Z" }),
    "POST /api/v1/customer/auth/phone-login": fail(409, "CUSTOMER_REGISTRATION_REQUIRED", "该手机号尚未注册，请完成注册"),
    "GET /api/v1/customer/terms": ok(TERMS),
    "POST /api/v1/customer/auth/register": ok({
      customer_id: "c1", masked_phone: "138****0000", customer_token: "tok-new", expires_at: "2026-11-24T00:00:00Z",
    }),
  });
  const page = mount("pages/login/index");
  fire(page, ["#phone"], "input", { value: "13800000000" });
  fire(page, ["#send-code"], "tap");
  await flush();
  expect(text(q(page, "#sent"))).toContain("138****0000");

  fire(page, ["#sms"], "input", { value: "123456" });
  fire(page, ["#submit"], "tap");
  await flush();
  // 未注册：不建会话，转注册并展示当前两份条款。
  expect(store.customer_token).toBeUndefined();
  expect(text(q(page, "#terms"))).toContain("会员条款正文");
  expect(text(q(page, "#terms"))).toContain("隐私条款正文");

  fire(page, ["#submit"], "tap");
  await flush();
  expect(text(q(page, "#error"))).toContain("请选择生日");
  fire(page, ["#birth"], "change", { value: "2000-01-02" });
  fire(page, ["#agree"], "change", { value: ["member"] });
  fire(page, ["#submit"], "tap");
  await flush();
  expect(text(q(page, "#error"))).toContain("请阅读并同意隐私条款");
  expect(requests.some((r) => r.key === "POST /api/v1/customer/auth/register")).toBe(false);

  fire(page, ["#agree"], "change", { value: ["member", "privacy"] });
  fire(page, ["#submit"], "tap");
  await flush();
  const register = requests.find((r) => r.key === "POST /api/v1/customer/auth/register");
  expect(register?.body ?? {}).toMatchObject({
    phone: "13800000000",
    sms_code: "123456",
    birth_date: "2000-01-02",
    member_terms_version: "会员条款第3版",
    privacy_terms_version: "隐私条款第2版",
  });
  expect(store.customer_token).toBe("tok-new");
  // 令牌不进任何跳转路径。
  const jumps = ["navigateBack", "switchTab", "reLaunch", "navigateTo", "redirectTo"].flatMap((name) => wx[name].mock.calls);
  expect(JSON.stringify(jumps)).not.toContain("tok-new");
  expect(wx.switchTab).toHaveBeenCalledWith({ url: "/pages/home/index" });
});

test("expiredSessionNoticeShownOnLogin", () => {
  installWx();
  const page = mount("pages/login/index", { query: { expired: "1" } });
  expect(text(q(page, "#notice"))).toContain("登录已过期，请重新登录");
});
