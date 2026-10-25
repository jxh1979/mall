// 文件版本：V0.01
// 动态码组件（BSM「有效期（刷新间隔）由后台统一设置、以 5 秒为单位可调、最长 60 秒；截图、转发出去的旧码过了有效期就作废」；
// BSM「二维码原始值不存在手机本地，只在线核验」）：按服务端到期时刻显示，到点即隐藏并取新码；取码失败、切后台即隐藏；回前台先取新码；不写存储。
import { afterEach, beforeEach, expect, test, vi } from "vitest";

import { fail, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const credential = (seconds) => ok({
  id: "cr1", ticket_id: "tk1", order_id: "o1", credential_token: "CRED-TK1", credential_type: "qr",
  issued_at: "2026-10-24T08:00:00Z", expires_at: new Date(Date.parse("2026-10-24T08:00:00Z") + seconds * 1000).toISOString(), refreshed_count: 0,
});
const tick = (ms) => vi.advanceTimersByTimeAsync(ms);

beforeEach(() => {
  freshModules();
  vi.useFakeTimers();
});
afterEach(() => vi.useRealTimers());

function sequence(...responses) {
  let i = 0;
  return () => responses[Math.min(i++, responses.length - 1)];
}

test("fiveSecondWindowHidesCodeAtExpiryAndRefetches", async () => {
  const { requests } = installWx({ "GET /api/v1/customer/tickets/tk1/qr": sequence(credential(5), "pending") }, TOKEN);
  const comp = mount("components/dynamic-code/index", { properties: { kind: "ticket", targetId: "tk1" } });
  await tick(0);
  expect(q(comp, "#qr-image")).not.toBeNull();
  expect(text(q(comp, "#qr-countdown"))).toContain("5 秒后失效");
  await tick(4000);
  expect(q(comp, "#qr-image")).not.toBeNull();
  expect(text(q(comp, "#qr-countdown"))).toContain("1 秒后失效");
  await tick(1000);
  expect(q(comp, "#qr-image")).toBeNull();
  expect(requests).toHaveLength(2);
});

test("hideClearsCodeAndShowRefetchesFirst", async () => {
  const { requests } = installWx({ "GET /api/v1/customer/tickets/tk1/qr": credential(30) }, TOKEN);
  const comp = mount("components/dynamic-code/index", { properties: { kind: "ticket", targetId: "tk1" } });
  await tick(0);
  expect(q(comp, "#qr-image")).not.toBeNull();
  comp.triggerPageLifeTime("hide");
  expect(q(comp, "#qr-image")).toBeNull();
  expect(text(q(comp, "#qr-hidden"))).toContain("二维码已隐藏");
  comp.triggerPageLifeTime("show");
  expect(requests).toHaveLength(2);
  await tick(0);
  expect(q(comp, "#qr-image")).not.toBeNull();
});

test("refreshFailureHidesPreviousCode", async () => {
  installWx({ "GET /api/v1/customer/tickets/tk1/qr": sequence(credential(5), fail(500, null, "服务异常")) }, TOKEN);
  const comp = mount("components/dynamic-code/index", { properties: { kind: "ticket", targetId: "tk1" } });
  await tick(0);
  expect(q(comp, "#qr-image")).not.toBeNull();
  await tick(5000);
  expect(q(comp, "#qr-image")).toBeNull();
  expect(text(q(comp, "#qr-hidden"))).toContain("二维码已隐藏，请联网后重试");
});

test("neverWritesCodeToStorage", async () => {
  const { wx } = installWx({ "GET /api/v1/customer/tickets/tk1/qr": credential(5) }, TOKEN);
  const comp = mount("components/dynamic-code/index", { properties: { kind: "ticket", targetId: "tk1" } });
  await tick(0);
  await tick(5000);
  comp.triggerPageLifeTime("hide");
  comp.triggerPageLifeTime("show");
  await tick(0);
  expect(q(comp, "#qr-image")).not.toBeNull();
  expect(wx.setStorageSync).not.toHaveBeenCalled();
  expect(wx.setStorage).not.toHaveBeenCalled();
  expect(text(comp)).not.toContain("CRED-TK1");
});
