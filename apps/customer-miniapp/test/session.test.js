// 文件版本：V0.01
// 小程序请求与会话（lib/api.js）：每个请求带本端类型、版本、所属模块与会话令牌（只经请求头）；任何请求 401 即清令牌回登录页，
// 403 不登出；登出先带令牌撤销服务端会话再清本端令牌；未登录的页面不发本人请求、给登录入口。
import path from "node:path";
import { beforeEach, expect, test } from "vitest";

import { fire, fail, flush, freshModules, header, installWx, mount, ok, q, require, ROOT } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const ORDERS_ROUTES = {
  "GET /api/v1/customer/orders": ok([]),
  "GET /api/v1/customer/my-tickets": ok([]),
  "GET /api/v1/customer/ticket-products": ok([]),
  "GET /api/v1/customer/ticket-channels": ok([]),
};

beforeEach(() => freshModules());

test("requestsCarryMiniappTerminalVersionModuleAndTokenOnlyInHeaders", async () => {
  const { requests } = installWx(ORDERS_ROUTES, TOKEN);
  mount("pages/orders/index");
  await flush();
  const req = requests.find((r) => r.key === "GET /api/v1/customer/orders");
  expect(req).toBeTruthy();
  expect(req.url).toBe("https://192.168.88.160/api/v1/customer/orders");
  expect(header(req, "x-terminal-type")).toBe("miniapp");
  expect(header(req, "x-client-version")).toBe("0.1.0");
  expect(header(req, "x-module-code")).toBe("ticketing");
  expect(header(req, "x-customer-token")).toBe("tok-1");
  expect(requests.every((r) => !r.url.includes("tok-1"))).toBe(true);
});

test("unauthorizedClearsTokenAndReturnsToLogin", async () => {
  const { wx, store } = installWx(
    { ...ORDERS_ROUTES, "GET /api/v1/customer/orders": fail(401, "CUSTOMER_UNAUTHENTICATED", "客人登录已过期，请重新登录") },
    TOKEN,
  );
  mount("pages/orders/index");
  await flush();
  expect(wx.removeStorageSync).toHaveBeenCalledWith("customer_token");
  expect(store.customer_token).toBeUndefined();
  expect(wx.reLaunch).toHaveBeenCalledWith({ url: "/pages/login/index?expired=1" });
});

test("forbiddenKeepsSession", async () => {
  const { wx, store } = installWx({ ...ORDERS_ROUTES, "GET /api/v1/customer/orders": fail(403, "CUSTOMER_ORDER_FORBIDDEN", "无权查看") }, TOKEN);
  const page = mount("pages/orders/index");
  await flush();
  expect(q(page, "#orders-error")).not.toBeNull();
  expect(wx.removeStorageSync).not.toHaveBeenCalled();
  expect(wx.reLaunch).not.toHaveBeenCalled();
  expect(store.customer_token).toBe("tok-1");
});

test("noSessionShowsLoginEntryWithoutOwnRequests", async () => {
  const { wx, requests } = installWx(ORDERS_ROUTES);
  const page = mount("pages/orders/index");
  await flush();
  expect(q(page, "#login-required")).not.toBeNull();
  expect(requests.filter((r) => r.key === "GET /api/v1/customer/orders")).toHaveLength(0);
  fire(page, ["#to-login"], "tap");
  await flush();
  expect(wx.navigateTo).toHaveBeenCalledWith({ url: "/pages/login/index" });
});

test("logoutRevokesServerSessionThenClearsToken", async () => {
  const { wx, requests, store } = installWx({ "POST /api/v1/customer/auth/logout": ok(null) }, TOKEN);
  const page = mount("pages/me/index");
  fire(page, ["#logout"], "tap");
  await flush();
  const logout = requests.find((r) => r.key === "POST /api/v1/customer/auth/logout");
  expect(logout).toBeTruthy();
  expect(header(logout, "x-customer-token")).toBe("tok-1");
  expect(store.customer_token).toBeUndefined();
  expect(wx.reLaunch).toHaveBeenCalledWith({ url: "/pages/home/index" });
});

// 恢复会话只在服务端明确判会话失效（401）时清令牌；断网、超时与服务端异常不算失效，令牌留着，下次请求再用。
test("restoreSessionKeepsTokenOnNetworkFailureButClearsOn401", async () => {
  const offline = installWx({ "GET /api/v1/customer/session": "network-fail" }, TOKEN);
  await expect(require(path.join(ROOT, "lib/api.js")).restoreSession()).resolves.toBeNull();
  expect(offline.store.customer_token).toBe("tok-1");
  expect(offline.wx.removeStorageSync).not.toHaveBeenCalled();

  freshModules();
  const stale = installWx({ "GET /api/v1/customer/session": fail(401, "CUSTOMER_UNAUTHENTICATED", "客人登录已过期，请重新登录") }, TOKEN);
  await expect(require(path.join(ROOT, "lib/api.js")).restoreSession()).resolves.toBeNull();
  expect(stale.store.customer_token).toBeUndefined();
});
