// 文件版本：V0.02
import { waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { MerchantAppStore, MerchantAppTokens } from "./adminClient";

// 第 25 条 25-R3：商户手机应用令牌模式（第十八批裁决 19「存进 App 的系统安全存储」；网页版不变）。
// 模式是模块级状态：每条用例 vi.resetModules 后重新导入，不串到别的用例。
async function loadClient() {
  vi.resetModules();
  return import("./adminClient");
}

function fakeStore(tokens: MerchantAppTokens | null, deviceId: string | null) {
  let current = tokens;
  let device = deviceId;
  const store: MerchantAppStore = {
    tokens: () => current,
    deviceId: () => device,
    save: vi.fn(async (next: MerchantAppTokens, nextDevice: string | null) => {
      current = next;
      device = nextDevice;
    }),
    clearTokens: vi.fn(async () => {
      current = null;
    }),
  };
  return store;
}

function jsonResponse(data: unknown): Response {
  return new Response(JSON.stringify({ code: "OK", message: "ok", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function unauthorized(): Response {
  return new Response(JSON.stringify({ code: "UNAUTHORIZED", message: "expired" }), { status: 401, headers: { "content-type": "application/json" } });
}

function headersOf(fetchMock: ReturnType<typeof vi.fn>, index: number): Headers {
  return (fetchMock.mock.calls[index]![1] as RequestInit).headers as Headers;
}

function bodyOf(fetchMock: ReturnType<typeof vi.fn>, index: number): Record<string, unknown> {
  return JSON.parse(String((fetchMock.mock.calls[index]![1] as RequestInit).body)) as Record<string, unknown>;
}

describe("AdminApi 商户手机应用令牌模式", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("读写与上传都带访问令牌、merchant_app 终端与 App 版本号，不带 Cookie 与 CSRF 头；请求体照常转蛇形键", async () => {
    document.cookie = "XSRF-TOKEN=csrf-abc; path=/";
    const fetchMock = vi.fn().mockImplementation(async () => jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);
    const client = await loadClient();
    client.configureMerchantAppMode(fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, "WBABCDEFGHJKMN"), "0.1.0");
    const api = new client.AdminApi();

    await api.get("/api/v1/merchant-portal/settlements");
    await api.post("/api/v1/merchant-service/work-orders", { workOrderType: "repair" });
    await api.postForm("/api/v1/evidence/attachments/upload", new FormData());

    await waitForCalls(fetchMock, 3);
    expect(client.isMerchantAppMode()).toBe(true);
    for (const index of [0, 1, 2]) {
      const headers = headersOf(fetchMock, index);
      expect(headers.get("authorization")).toBe("Bearer at-1");
      expect(headers.get("x-terminal-type")).toBe("merchant_app");
      expect(headers.get("x-client-version")).toBe("0.1.0");
      expect(headers.get("x-xsrf-token")).toBeNull();
      expect((fetchMock.mock.calls[index]![1] as RequestInit).credentials).not.toBe("include");
    }
    expect(bodyOf(fetchMock, 1)).toMatchObject({ work_order_type: "repair" });
  });

  it("401 时带旧令牌按请求体的刷新令牌与设备编号续期，保存新令牌后用新令牌重放", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(unauthorized())
      .mockResolvedValueOnce(jsonResponse({ access_token: "at-2", refresh_token: "rt-2", device_id: "WBABCDEFGHJKMN", user: { id: "u-1" } }))
      .mockResolvedValueOnce(jsonResponse({ items: [] }));
    vi.stubGlobal("fetch", fetchMock);
    const client = await loadClient();
    const store = fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, "WBABCDEFGHJKMN");
    client.configureMerchantAppMode(store, "0.1.0");

    await new client.AdminApi().get("/api/v1/merchant-portal/settlements");

    await waitForCalls(fetchMock, 3);
    expect(String(fetchMock.mock.calls[1]![0])).toContain("/api/v1/auth/refresh");
    expect(headersOf(fetchMock, 1).get("authorization")).toBe("Bearer at-1");
    expect((fetchMock.mock.calls[1]![1] as RequestInit).credentials).not.toBe("include");
    expect(bodyOf(fetchMock, 1)).toMatchObject({ refresh_token: "rt-1", device_id: "WBABCDEFGHJKMN" });
    expect(store.save).toHaveBeenCalledWith({ accessToken: "at-2", refreshToken: "rt-2" }, "WBABCDEFGHJKMN");
    expect(headersOf(fetchMock, 2).get("authorization")).toBe("Bearer at-2");
  });

  it("续期失败只清令牌、设备编号保留，并通知会话失效", async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(unauthorized()).mockResolvedValueOnce(unauthorized());
    vi.stubGlobal("fetch", fetchMock);
    const client = await loadClient();
    const store = fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, "WBABCDEFGHJKMN");
    client.configureMerchantAppMode(store, "0.1.0");
    const api = new client.AdminApi();
    const expired = vi.fn();
    api.onSessionExpired = expired;

    await expect(api.get("/api/v1/merchant-portal/settlements")).rejects.toBeTruthy();

    await waitForCalls(fetchMock, 2);
    expect(String(fetchMock.mock.calls[1]![0])).toContain("/api/v1/auth/refresh");
    expect(store.clearTokens).toHaveBeenCalled();
    expect(store.tokens()).toBeNull();
    expect(store.deviceId()).toBe("WBABCDEFGHJKMN");
    expect(expired).toHaveBeenCalledTimes(1);
  });

  it("App 里四类导出不发请求、提示到商户网页门户办理；页内显示证件照片照常请求", async () => {
    const fetchMock = vi.fn().mockImplementation(async () => new Response("img", { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const client = await loadClient();
    client.configureMerchantAppMode(fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, "WBABCDEFGHJKMN"), "0.1.0");
    const api = new client.AdminApi();

    for (const path of [
      "/api/v1/finance/ar-ap-items/export",
      "/api/v1/settlement/invoice-items/export?status=issued",
      "/api/v1/analytics/reports/monthly-export?month=2026-10",
      "/api/v1/security/export-files/exp-1/editable-copy?review_id=rv-1",
    ]) {
      await expect(api.getFile(path)).rejects.toThrow("导出文件请在商户网页门户办理");
    }
    expect(fetchMock).not.toHaveBeenCalled();

    await api.getFile("/api/v1/student-certifications/cert-1/files/1");
    await waitForCalls(fetchMock, 1);
    expect(headersOf(fetchMock, 0).get("authorization")).toBe("Bearer at-1");
  });

  it("未配置 App 模式时网页版不变：Cookie 与 CSRF、不带令牌与版本号，导出照常请求", async () => {
    document.cookie = "XSRF-TOKEN=csrf-abc; path=/";
    const fetchMock = vi.fn().mockImplementation(async (input: RequestInfo | URL) =>
      String(input).endsWith("/export") ? new Response("pdf", { status: 200 }) : jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);
    const client = await loadClient();
    const api = new client.AdminApi();

    await api.getFile("/api/v1/finance/ar-ap-items/export");
    await api.post("/api/v1/merchant-service/work-orders", {});

    await waitForCalls(fetchMock, 2);
    expect(client.isMerchantAppMode()).toBe(false);
    expect(String(fetchMock.mock.calls[0]![0])).toContain("/api/v1/finance/ar-ap-items/export");
    for (const index of [0, 1]) {
      const headers = headersOf(fetchMock, index);
      expect(headers.get("authorization")).toBeNull();
      expect(headers.get("x-client-version")).toBeNull();
      expect(headers.get("x-terminal-type")).toBe("web");
      expect((fetchMock.mock.calls[index]![1] as RequestInit).credentials).toBe("include");
    }
    expect(headersOf(fetchMock, 1).get("x-xsrf-token")).toBe("csrf-abc");
  });
});

// 第十八批 R 段 152-R4：后台复制给客人的邀请链接主机＝后台主机（发行打包客人链接主机取 public_host）；
// 商户手机应用的页面不在后台主机上，按接口基址 VITE_API_BASE_URL 取主机。
describe("customerLinkHost 客人链接主机", () => {
  afterEach(() => vi.unstubAllEnvs());

  it("配了 VITE_API_BASE_URL 时取其主机，未配时取当前页面主机", async () => {
    const client = await loadClient();
    expect(client.customerLinkHost).toBeTypeOf("function");
    vi.stubEnv("VITE_API_BASE_URL", "https://192.168.88.160/");
    expect(client.customerLinkHost()).toBe("192.168.88.160");
    vi.stubEnv("VITE_API_BASE_URL", "");
    expect(client.customerLinkHost()).toBe(window.location.host);
  });
});

async function waitForCalls(fetchMock: ReturnType<typeof vi.fn>, count: number) {
  await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThanOrEqual(count));
}
