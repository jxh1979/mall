// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { MerchantAppStore, MerchantAppTokens } from "./client/adminClient";
import type { AdminPageProps } from "./types";

// 第 25 条 25-R3：AdminApp 在商户手机应用里（BSM:1886 商户 App「在授权业务范围内移动处理本商户经营、对账、结算和看板」；
// 第十八批裁决 19 设备编号存 App 的系统安全存储、卸载重装或清除应用数据才算新设备）。网页模式不变。
vi.mock("./pages/OverviewPage", () => ({ OverviewPage: () => <section data-testid="overview-stub">总控</section> }));
vi.mock("./pages/MerchantZoneBillsPage", () => ({ MerchantZoneBillsPage: () => <section data-testid="bills-stub">我的账单</section> }));

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); });

const DEVICE = "WBABCDEFGHJKMN";
const MERCHANT_USER = { id: "mu-1", username: "商户甲账号", accountType: "merchant", merchantId: "m-1" };
const LOGIN = {
  access_token: "at-1", refresh_token: "rt-1", session_id: "s-1", device_id: DEVICE,
  user: { id: "mu-1", username: "商户甲账号", account_type: "merchant", merchant_id: "m-1" },
};

type Call = { url: string; init: RequestInit | undefined };

function fakeStore(tokens: MerchantAppTokens | null, deviceId: string | null) {
  let current = tokens;
  let device = deviceId;
  const store: MerchantAppStore = {
    tokens: () => current,
    deviceId: () => device,
    save: vi.fn(async (next: MerchantAppTokens, nextDevice: string | null) => { current = next; device = nextDevice; }),
    clearTokens: vi.fn(async () => { current = null; }),
  };
  return store;
}

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify({ success: status < 400, code: status < 400 ? "OK" : "UNAUTHORIZED", data }), { status, headers: { "content-type": "application/json" } });
}

function stubServer(override: (url: string) => Response | null = () => null): Call[] {
  const calls: Call[] = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    calls.push({ url, init });
    const replaced = override(url);
    if (replaced) return replaced;
    let data: unknown = [];
    if (url.endsWith("/api/v1/auth/login")) data = LOGIN;
    if (url.endsWith("/api/v1/access/effective-permissions")) data = { grants: [], denies: [], wildcard: true };
    if (url.endsWith("/api/v1/security/page-views")) data = { code: "K7MX3PQR" };
    return json(data);
  }));
  return calls;
}

// 模式是 adminClient 的模块级状态：每条用例重置模块后按需开启，再导入 AdminApp，不串到别的用例。
async function mountApp(store: MerchantAppStore | null, path: string, signedIn: boolean) {
  vi.resetModules();
  const client = await import("./client/adminClient");
  if (store) client.configureMerchantAppMode(store, "0.1.0");
  const { default: AdminApp } = await import("./AdminApp");
  if (signedIn) window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: MERCHANT_USER, sessionId: "s-1" }));
  window.history.replaceState({}, "", path);
  if (typeof HTMLElement.prototype.scrollTo !== "function") HTMLElement.prototype.scrollTo = () => undefined;
  render(<AdminApp />);
}

function callTo(calls: Call[], suffix: string): Call | undefined {
  return calls.find((call) => call.url.split("?")[0]!.endsWith(suffix));
}

function bodyOf(call: Call | undefined): Record<string, unknown> {
  return JSON.parse(String(call?.init?.body ?? "{}")) as Record<string, unknown>;
}

function headerOf(call: Call | undefined, name: string): string | null {
  return (call?.init?.headers as Headers | undefined)?.get(name) ?? null;
}

async function signIn(username: string) {
  await waitFor(() => expect(screen.queryByTestId("login-view")).not.toBeNull());
  fireEvent.change(screen.getByTestId("login-username"), { target: { value: username } });
  fireEvent.change(screen.getByTestId("login-password"), { target: { value: "pw-1" } });
  fireEvent.click(screen.getByTestId("login-submit"));
}

/** 点一个链接，返回外壳是否拦下（拦下即不跳离当前页）；测试里随后自行阻止 jsdom 的跳转。 */
function clickLink(href: string): boolean | null {
  const link = document.createElement("a");
  link.href = href;
  link.textContent = "证据1";
  document.body.appendChild(link);
  let prevented: boolean | null = null;
  link.addEventListener("click", (event) => { prevented = event.defaultPrevented; event.preventDefault(); });
  fireEvent.click(link);
  link.remove();
  return prevented;
}

describe("AdminApp 商户手机应用模式", () => {
  it("首次登录报 merchant_app、无设备编号不带 device_id；令牌与系统发的设备编号存进安全存储，后续请求带访问令牌与版本号", async () => {
    const calls = stubServer();
    const store = fakeStore(null, null);
    await mountApp(store, "/admin/merchant-zone/bills", false);

    await signIn("商户甲账号");

    await waitFor(() => expect(callTo(calls, "/api/v1/auth/login")).toBeTruthy());
    const login = callTo(calls, "/api/v1/auth/login");
    expect(bodyOf(login)).toMatchObject({ login_name: "商户甲账号", password: "pw-1", terminal_type: "merchant_app" });
    expect(bodyOf(login)).not.toHaveProperty("device_id");
    expect(headerOf(login, "x-terminal-type")).toBe("merchant_app");
    await waitFor(() => expect(store.save).toHaveBeenCalledWith({ accessToken: "at-1", refreshToken: "rt-1" }, DEVICE));
    await waitFor(() => expect(callTo(calls, "/api/v1/access/effective-permissions")).toBeTruthy());
    const permissions = callTo(calls, "/api/v1/access/effective-permissions");
    expect(headerOf(permissions, "authorization")).toBe("Bearer at-1");
    expect(headerOf(permissions, "x-terminal-type")).toBe("merchant_app");
    expect(headerOf(permissions, "x-client-version")).toBe("0.1.0");
    expect(permissions?.init?.credentials).not.toBe("include");
  });

  it("会话失效（续期也被拒）只清令牌、回登录页；设备编号保留，再次登录带同一 device_id", async () => {
    let expired = true;
    const calls = stubServer((url) => (expired && !url.endsWith("/api/v1/auth/login") ? json(null, 401) : null));
    const store = fakeStore({ accessToken: "at-0", refreshToken: "rt-0" }, DEVICE);
    await mountApp(store, "/admin/merchant-zone/bills", true);

    await waitFor(() => expect(screen.queryByTestId("login-view")).not.toBeNull());
    expect(screen.getByTestId("global-error").textContent).toContain("登录已过期");
    const refresh = callTo(calls, "/api/v1/auth/refresh");
    expect(bodyOf(refresh)).toMatchObject({ refresh_token: "rt-0", device_id: DEVICE });
    expect(headerOf(refresh, "authorization")).toBe("Bearer at-0");
    expect(store.clearTokens).toHaveBeenCalled();
    expect(store.tokens()).toBeNull();
    expect(store.deviceId()).toBe(DEVICE);

    expired = false;
    await signIn("商户甲账号");
    await waitFor(() => expect(callTo(calls, "/api/v1/auth/login")).toBeTruthy());
    expect(bodyOf(callTo(calls, "/api/v1/auth/login"))).toMatchObject({ terminal_type: "merchant_app", device_id: DEVICE });
  });

  it("退出：登出请求带着令牌发出，之后只清令牌、设备编号保留", async () => {
    const calls = stubServer();
    const store = fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, DEVICE);
    await mountApp(store, "/admin/merchant-zone/bills", true);

    await waitFor(() => expect(screen.queryByTestId("admin-logout")).not.toBeNull());
    fireEvent.click(screen.getByTestId("admin-logout"));

    await waitFor(() => expect(callTo(calls, "/api/v1/auth/logout")).toBeTruthy());
    const logout = callTo(calls, "/api/v1/auth/logout");
    expect(bodyOf(logout)).toMatchObject({ session_id: "s-1" });
    expect(headerOf(logout, "authorization")).toBe("Bearer at-1");
    await waitFor(() => expect(store.clearTokens).toHaveBeenCalled());
    expect(store.deviceId()).toBe(DEVICE);
    expect(screen.queryByTestId("login-view")).not.toBeNull();
  });

  it("菜单按 BSM 商户 App 行：没有库存、经销、人事、账号权限与员工车辆，有我的账单、我的对账、我的证照与有权限的酒吧页", async () => {
    stubServer();
    await mountApp(fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, DEVICE), "/admin/merchant-zone/bills", true);

    await waitFor(() => expect(screen.queryByTestId("admin-nav-ADM-MER-003")).not.toBeNull());
    for (const pageId of ["ADM-MER-005", "ADM-MER-010", "ADM-BAR-001", "ADM-FIN-001"]) {
      expect(screen.queryByTestId(`admin-nav-${pageId}`)).not.toBeNull();
    }
    expect(screen.queryByTestId("admin-nav-group-inventory")).toBeNull();
    expect(screen.queryByTestId("admin-nav-group-access")).toBeNull();
    for (const pageId of ["ADM-INV-001", "ADM-DLR-001", "ADM-STF-001", "ADM-STF-002", "ADM-STF-003", "ADM-STF-004", "ADM-ACC-001", "ADM-MER-009"]) {
      expect(screen.queryByTestId(`admin-nav-${pageId}`)).toBeNull();
    }
  });

  it("直接打开员工车辆：提示到商户网页门户办理，不请求该页接口、不登记打开", async () => {
    const calls = stubServer();
    await mountApp(fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, DEVICE), "/admin/merchant-zone/staff-vehicles", true);

    await waitFor(() => expect(screen.queryByTestId("admin-account-form-mismatch")?.textContent).toBe("此功能请在商户网页门户办理"));
    await waitFor(() => expect(callTo(calls, "/api/v1/access/effective-permissions")).toBeTruthy());
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(calls.some((call) => call.url.includes("/api/v1/parking/"))).toBe(false);
    expect(callTo(calls, "/api/v1/security/page-views")).toBeUndefined();
  });

  it("点附件原件链接（/api/v1/…）不跳离当前页、提示到商户网页门户查看；/admin/ 整页链接不拦", async () => {
    stubServer();
    await mountApp(fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, DEVICE), "/admin/merchant-zone/bills", true);
    await waitFor(() => expect(screen.queryByTestId("bills-stub")).not.toBeNull());

    expect(clickLink("/api/v1/evidence/attachments/x/file")).toBe(true);
    await waitFor(() => expect(screen.queryByTestId("global-error")?.textContent).toBe("附件原件请在商户网页门户查看"));
    expect(window.location.pathname).toBe("/admin/merchant-zone/bills");
    expect(clickLink("/admin/governance/approval-chain?review_id=r-1")).toBe(false);
  });
});

describe("AdminApp 网页模式不变", () => {
  it("商户账号登录体只有账号密码、按 merchant_portal 上报、走 Cookie；员工车辆与库存照常在菜单，附件链接不拦", async () => {
    const calls = stubServer();
    await mountApp(null, "/admin/merchant-zone/bills", false);

    await signIn("商户甲账号");

    await waitFor(() => expect(screen.queryByTestId("admin-nav-ADM-MER-009")).not.toBeNull());
    const login = callTo(calls, "/api/v1/auth/login");
    expect(bodyOf(login)).not.toHaveProperty("terminal_type");
    expect(bodyOf(login)).not.toHaveProperty("device_id");
    expect(headerOf(login, "x-terminal-type")).toBe("web");
    const permissions = callTo(calls, "/api/v1/access/effective-permissions");
    expect(headerOf(permissions, "x-terminal-type")).toBe("merchant_portal");
    expect(headerOf(permissions, "authorization")).toBeNull();
    expect(permissions?.init?.credentials).toBe("include");
    expect(screen.queryByTestId("admin-nav-ADM-INV-001")).not.toBeNull();
    expect(screen.queryByTestId("admin-nav-ADM-STF-001")).not.toBeNull();

    expect(clickLink("/api/v1/evidence/attachments/x/file")).toBe(false);
    expect(screen.queryByTestId("global-error")).toBeNull();
  });
});

describe("我的工单报修渠道", () => {
  async function submitWorkOrder(appMode: boolean): Promise<Record<string, unknown> | null> {
    vi.resetModules();
    const client = await import("./client/adminClient");
    if (appMode) client.configureMerchantAppMode(fakeStore({ accessToken: "at-1", refreshToken: "rt-1" }, DEVICE), "0.1.0");
    const { MerchantZoneWorkOrdersPage } = await import("./pages/MerchantZoneWorkOrdersPage");
    let posted: Record<string, unknown> | null = null;
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      if ((init?.method ?? "GET") === "POST" && String(input).includes("/merchant-service/work-orders")) posted = JSON.parse(String(init?.body)) as Record<string, unknown>;
      return json({ items: [], page: 1, page_size: 50, total: 0 });
    }));
    const props: AdminPageProps = {
      api: new client.AdminApi(),
      user: MERCHANT_USER,
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } },
    };
    render(<MerchantZoneWorkOrdersPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-work-order-create-open")).not.toBeNull());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-create-open"));
    fireEvent.change(screen.getByTestId("merchant-zone-work-order-title"), { target: { value: "空调漏水" } });
    fireEvent.change(screen.getByTestId("merchant-zone-work-order-description"), { target: { value: "机房空调滴水" } });
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-work-order-guided-summary")).not.toBeNull());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-submit"));
    await waitFor(() => expect(posted).not.toBeNull());
    return posted;
  }

  it("App 里提交的工单报修渠道为 merchant_app", async () => {
    expect(await submitWorkOrder(true)).toMatchObject({ reported_channel: "merchant_app" });
  });

  it("网页里仍为 merchant_portal", async () => {
    expect(await submitWorkOrder(false)).toMatchObject({ reported_channel: "merchant_portal" });
  });
});
