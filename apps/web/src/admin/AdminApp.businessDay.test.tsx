// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import AdminApp from "./AdminApp";
import { OperatingParamsPage } from "./pages/OperatingParamsPage";
import type { AdminApiClient } from "./types";

vi.mock("./pages/AmusementBusinessDayPage", () => ({ AmusementBusinessDayPage: () => <section data-testid="business-day-page">营业日配置</section> }));

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); });

function mount(grants: string[]) {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
    const data = String(input).endsWith("/api/v1/access/effective-permissions") ? { grants, denies: [], wildcard: false } : [];
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
  }));
  window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "ops-1", username: "ops", accountType: "platform_admin" }, sessionId: "session-1" }));
  window.history.replaceState({}, "", "/admin/amusement/business-day-configs");
  render(<AdminApp />);
}

// ENUM:187「按每个项目、每家酒吧可配」：营业日配置页综合体与酒吧两套共用一页，持有其中一个查看权限即可进入。
describe("营业日配置页入口", () => {
  it.each(["amusement.business_day.view", "bar.business_day.view"])("只有 %s 也能进入营业日配置页", async permission => {
    mount([permission]);
    await waitFor(() => expect(screen.queryByTestId("business-day-page")).not.toBeNull());
    expect(screen.queryByTestId("admin-permission-denied")).toBeNull();
  });

  it("两个查看权限都没有仍被拒", async () => {
    mount(["inventory.stock.view"]);
    await waitFor(() => expect(screen.queryByTestId("admin-permission-denied")).not.toBeNull());
    expect(screen.queryByTestId("business-day-page")).toBeNull();
  });

  it.each(["amusement.business_day.view", "bar.business_day.view"])("经营参数页「打开营业日配置」只有 %s 也可点", async permission => {
    const navigateTo = vi.fn();
    const api = {
      get: vi.fn(async (url: string) => (url.includes("anomaly-config") ? { surgePct: 0.5, highPct: 0.8, enabled: true } : [])),
      post: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
    render(<OperatingParamsPage api={api} user={{ id: "u1", username: "ops", accountType: "platform_admin" }} setActionResult={vi.fn()}
      setGlobalError={vi.fn()} navigateTo={navigateTo}
      effectivePermissions={{ status: "ready", value: { grants: [permission], denies: [], wildcard: false } }} />);
    await waitFor(() => expect((screen.getByRole("button", { name: "打开营业日配置" }) as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByRole("button", { name: "打开营业日配置" }));
    expect(navigateTo).toHaveBeenCalledWith("ADM-AMU-008");
  });
});
