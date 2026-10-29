// 文件版本：V0.01
// 商户手机应用入口（第 25 条 25-R4；第十八批裁决 19「存进 App 的系统安全存储……卸载重装或清除应用数据算新设备」）：
// 令牌与设备编号只进系统安全存储、不写 localStorage，退出只清令牌、设备编号保留；没有安装标记（新装或清除了应用数据）先清空安全存储
// （iPhone 钥匙串卸载后不清）；启动即以商户手机应用模式挂 apps/web 的后台外壳。安全存储插件在 jsdom 里没有原生实现，这里用替身。
import { screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({ items: new Map<string, string>(), failRead: false }));

vi.mock("@aparajita/capacitor-secure-storage", () => ({
  SecureStorage: {
    getItem: vi.fn(async (key: string) => {
      if (h.failRead) throw new Error("读取失败");
      return h.items.get(key) ?? null;
    }),
    setItem: vi.fn(async (key: string, value: string) => void h.items.set(key, value)),
    removeItem: vi.fn(async (key: string) => void h.items.delete(key)),
    clear: vi.fn(async () => h.items.clear()),
  },
}));

const SESSION_KEY = "merchant_app_session";
const DEVICE_KEY = "merchant_app_device_no";
const INSTALLED_KEY = "merchant_app_installed";
const DEVICE = "WBABCDEFGHJKMN";
const TOKENS = { accessToken: "at-1", refreshToken: "rt-1" };

/** 走真实入口 main.tsx：模式是后台请求层的模块级状态，每条用例重置模块后重新启动，不串到别的用例。 */
async function boot() {
  vi.resetModules();
  document.body.innerHTML = '<div id="root"></div>';
  await import("./main");
  const client = await import("../../web/src/admin/client/adminClient");
  const { SecureStorage } = await import("@aparajita/capacitor-secure-storage");
  return { client, storage: vi.mocked(SecureStorage) };
}

function localStorageText(): string {
  return Array.from({ length: window.localStorage.length }, (_, i) => window.localStorage.getItem(window.localStorage.key(i)!)).join("\n");
}

beforeEach(() => {
  vi.clearAllMocks();
  h.items.clear();
  h.failRead = false;
  window.localStorage.clear();
  window.history.replaceState({}, "", "/");
  vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify({ success: true, code: "OK", data: null }), { status: 200, headers: { "content-type": "application/json" } })));
});

afterEach(() => vi.unstubAllGlobals());

describe("商户手机应用入口", () => {
  it("没有安装标记（新装或清除了应用数据）：先清空安全存储再写标记，按新设备、无会话挂出后台登录页", async () => {
    h.items.set(SESSION_KEY, JSON.stringify(TOKENS));
    h.items.set(DEVICE_KEY, DEVICE);
    const markInstalled = vi.spyOn(Storage.prototype, "setItem");
    const { client, storage } = await boot();
    await waitFor(() => expect(screen.getByTestId("login-view")).toBeInTheDocument());
    expect(storage.clear).toHaveBeenCalledTimes(1);
    expect(window.localStorage.getItem(INSTALLED_KEY)).not.toBeNull();
    const marked = markInstalled.mock.calls.findIndex(([key]) => key === INSTALLED_KEY);
    expect(storage.clear.mock.invocationCallOrder[0]).toBeLessThan(markInstalled.mock.invocationCallOrder[marked]!);
    expect(client.isMerchantAppMode()).toBe(true);
    expect(client.merchantAppStore()?.tokens()).toBeNull();
    expect(client.merchantAppStore()?.deviceId()).toBeNull();
  });

  it("已有安装标记：不清空，安全存储里的令牌与设备编号交给后台请求层，挂出后台登录页", async () => {
    window.localStorage.setItem(INSTALLED_KEY, "1");
    h.items.set(SESSION_KEY, JSON.stringify(TOKENS));
    h.items.set(DEVICE_KEY, DEVICE);
    const { client, storage } = await boot();
    await waitFor(() => expect(client.merchantAppStore()?.deviceId()).toBe(DEVICE));
    expect(client.isMerchantAppMode()).toBe(true);
    expect(client.merchantAppStore()?.tokens()).toEqual(TOKENS);
    expect(storage.clear).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.getByTestId("login-view")).toBeInTheDocument());
  });

  it("保存令牌只进安全存储、不写 localStorage；退出只清令牌，设备编号保留", async () => {
    window.localStorage.setItem(INSTALLED_KEY, "1");
    const { client, storage } = await boot();
    await waitFor(() => expect(client.isMerchantAppMode()).toBe(true));
    const store = client.merchantAppStore()!;
    await store.save({ accessToken: "at-2", refreshToken: "rt-2" }, DEVICE);
    expect(storage.setItem).toHaveBeenCalledWith(SESSION_KEY, JSON.stringify({ accessToken: "at-2", refreshToken: "rt-2" }));
    expect(storage.setItem).toHaveBeenCalledWith(DEVICE_KEY, DEVICE);
    expect(localStorageText()).not.toContain("at-2");
    expect(localStorageText()).not.toContain("rt-2");
    expect(store.tokens()).toEqual({ accessToken: "at-2", refreshToken: "rt-2" });

    await store.clearTokens();
    expect(storage.removeItem).toHaveBeenCalledTimes(1);
    expect(storage.removeItem).toHaveBeenCalledWith(SESSION_KEY);
    expect(h.items.get(DEVICE_KEY)).toBe(DEVICE);
    expect(store.tokens()).toBeNull();
    expect(store.deviceId()).toBe(DEVICE);
  });

  it("读安全存储出错：按无会话启动，仍挂出后台登录页", async () => {
    window.localStorage.setItem(INSTALLED_KEY, "1");
    h.failRead = true;
    const { client } = await boot();
    await waitFor(() => expect(client.isMerchantAppMode()).toBe(true));
    expect(client.merchantAppStore()?.tokens()).toBeNull();
    expect(client.merchantAppStore()?.deviceId()).toBeNull();
    await waitFor(() => expect(screen.getByTestId("login-view")).toBeInTheDocument());
  });
});
