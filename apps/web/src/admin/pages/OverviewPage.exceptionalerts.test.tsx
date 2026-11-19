// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BC 段第 369 条 369-R3（BSM:1835「异常预警入口：经营看板显示异常预警的入口与未处理条数计数卡片，点击进入异常预警页面；
// 看板上的计数只是入口提示，预警的查看、转办与处置仍在异常预警页面完成。」）：持 risk_alert.view 才请求与显示，失败显示「—」。
// 假响应按接口真实形状（蛇形键文本体）。

const perms = (grants: string[]): EffectivePermissionsState => ({ status: "ready", value: { grants, denies: [], wildcard: false } });
const OPEN_COUNT = "/api/v1/evidence/risk-alerts/open-count?source_domain=platform,ticketing,parking";
const OVERVIEW = { as_of: "2026-07-18T02:00:00Z", scope: null, business_date: "2026-07-17", metrics: {} };

function text(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

let fetchMock: ReturnType<typeof vi.fn>;

function renderPage(grants: string[], openCount: () => Response) {
  fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.endsWith(OPEN_COUNT)) return Promise.resolve(openCount());
    if (url.includes("/analytics/metric-detail")) {
      return Promise.resolve(text(200, { success: true, code: "OK", message: "success", data: { summary_value: 0, rows: [], total_rows: 0, columns: [] } }));
    }
    return Promise.resolve(text(200, { success: true, code: "OK", message: "success", data: OVERVIEW }));
  });
  vi.stubGlobal("fetch", fetchMock);
  const navigateTo = vi.fn();
  const props: AdminPageProps = {
    api: new AdminApi(""),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: perms(grants),
    navigateTo,
  };
  render(<OverviewPage {...props} />);
  return navigateTo;
}

const urls = () => fetchMock.mock.calls.map((call: unknown[]) => String(call[0]));
const value = () => screen.queryByTestId("overview-kpi-exception-alerts-value")?.textContent;

describe("OverviewPage 369-R3：异常预警入口与未处理条数卡", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("持 risk_alert.view：按异常预警页三类来源取未处理条数，点数字进异常预警页", async () => {
    const navigateTo = renderPage(["analytics.dashboard.view", "risk_alert.view"],
      () => text(200, { success: true, code: "OK", message: "success", data: { open_count: 3 } }));
    await waitFor(() => expect(urls().some((url) => url.endsWith(OPEN_COUNT))).toBe(true));
    await waitFor(() => expect(value()).toBe("3"));
    expect(screen.getByTestId("overview-kpi-exception-alerts").textContent).toContain("异常预警");
    fireEvent.click(screen.getByTestId("overview-kpi-exception-alerts-value"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-RISK-002");
  });

  it("不持 risk_alert.view：不请求、不显示这张卡", async () => {
    renderPage(["analytics.dashboard.view"], () => text(200, { success: true, code: "OK", message: "success", data: { open_count: 3 } }));
    await waitFor(() => expect(screen.queryByTestId("overview-kpi-ticket")).not.toBeNull());
    expect(urls().some((url) => url.includes("/api/v1/evidence/risk-alerts"))).toBe(false);
    expect(screen.queryByTestId("overview-kpi-exception-alerts")).toBeNull();
  });

  it("接口拒绝：卡片显示「—」", async () => {
    renderPage(["analytics.dashboard.view", "risk_alert.view"],
      () => text(403, { success: false, code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "当前账号无此操作权限" }));
    await waitFor(() => expect(urls().some((url) => url.endsWith(OPEN_COUNT))).toBe(true));
    await waitFor(() => expect(screen.getByTestId("overview-kpi-exception-alerts")).toBeTruthy());
    expect(value()).toBe("—");
  });
});
