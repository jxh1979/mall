// 文件版本：V0.01
import { act, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import AdminApp from "./AdminApp";

// 第十八批 N 段第 123 条 123-R3（BSM:1876「使用 Web 端必须打开地理识别，没有打开地理识别的无法正常使用。」）：
// 网页端（web）登录后整页准入，浏览器授权变化时重新判定；商户门户是商户侧页面，不加准入。
vi.mock("./pages/OverviewPage", () => ({ OverviewPage: () => <section data-testid="overview-stub">总控</section> }));

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); });

function stubServer() {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input).split("?")[0]!;
    let data: unknown = [];
    if (url.endsWith("/api/v1/access/effective-permissions")) data = { grants: [], denies: [], wildcard: true };
    if (url.endsWith("/api/v1/security/page-views")) data = { code: "K7MX3PQR" };
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
  }));
}

/** 浏览器里「位置」授权状态；改 state 后调 onchange 模拟用户在网站设置里改了授权。 */
function permissionState(state: PermissionState) {
  const status = { state, onchange: null as (() => void) | null };
  vi.spyOn(navigator.permissions, "query").mockImplementation(async () => status as unknown as PermissionStatus);
  return status;
}

function mountSignedIn(user: Record<string, unknown>) {
  window.localStorage.setItem("admin.session.v1", JSON.stringify({ user, sessionId: "s-1" }));
  window.history.replaceState({}, "", "/admin/overview");
  if (typeof HTMLElement.prototype.scrollTo !== "function") HTMLElement.prototype.scrollTo = () => undefined;
  render(<AdminApp />);
}

describe("AdminApp 网页端地理识别准入（第 123 条 123-R3）", () => {
  it("网页端没打开地理识别：整页拦截提示，不渲染菜单与页面；在浏览器里允许位置后恢复", async () => {
    stubServer();
    const status = permissionState("denied");
    mountSignedIn({ id: "u-1", username: "运营甲", accountType: "platform_admin" });

    await waitFor(() => expect(screen.queryByTestId("geo-required")?.textContent ?? "").toContain("使用网页端须打开地理识别"));
    expect(screen.queryByTestId("admin-nav")).toBeNull();
    expect(screen.queryByTestId("overview-stub")).toBeNull();

    status.state = "granted";
    act(() => status.onchange?.());
    await waitFor(() => expect(screen.queryByTestId("overview-stub")).not.toBeNull());
    expect(screen.queryByTestId("admin-nav")).not.toBeNull();
    expect(screen.queryByTestId("geo-required")).toBeNull();
  });

  it("商户门户（merchant_portal）不加整页准入：没打开地理识别也照常进入", async () => {
    stubServer();
    permissionState("denied");
    mountSignedIn({ id: "mu-1", username: "商户甲账号", accountType: "merchant", merchantId: "m-1" });

    await waitFor(() => expect(screen.queryByTestId("admin-nav")).not.toBeNull());
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(screen.queryByTestId("geo-required")).toBeNull();
    expect(screen.queryByTestId("admin-nav")).not.toBeNull();
  });
});
