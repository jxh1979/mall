import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { RiskAlertsPage } from "./RiskAlertsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const ALERTS = [
  { id: "ra-1", alert_type: "staff_behavior", risk_level: "high", object_type: "bar_order", object_id: "o-1", summary: "异常折扣", status: "open" },
  { id: "ra-2", alert_type: "customer_risk", risk_level: "low", object_type: "member", object_id: "m-1", summary: "已处理低风险", status: "resolved" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("risk-alerts")) return Promise.resolve(envelope(ALERTS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<RiskAlertsPage {...props} />);
}

describe("RiskAlertsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/risk/alerts");
  });

  it("点“高风险” → URL 带 ?kpi=alerts&subset=high，聚焦只剩高风险行且隐藏表单/搜索/KPI卡", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alerts-kpi-high")).toBeTruthy());
    fireEvent.click(screen.getByTestId("risk-alerts-kpi-high"));
    expect(window.location.search).toContain("kpi=alerts");
    expect(window.location.search).toContain("subset=high");
    expect(screen.getByTestId("risk-alerts-kpi-focus-banner").textContent).toContain("正在查看：高风险（1）");
    expect(screen.getByTestId("risk-alert-select-ra-1")).toBeTruthy();
    expect(screen.queryByTestId("risk-alert-select-ra-2")).toBeNull();
    expect(screen.queryByTestId("risk-alerts-summary")).toBeNull();
    expect(screen.queryByTestId("risk-alert-form")).toBeNull();
    expect(screen.queryByTestId("risk-alerts-search")).toBeNull();
  });

  it("点“待处理” → 聚焦只剩未关闭行，点「返回全部」恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alerts-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("risk-alerts-kpi-open"));
    expect(screen.getByTestId("risk-alerts-kpi-focus-banner").textContent).toContain("正在查看：待处理（1）");
    expect(screen.queryByTestId("risk-alert-select-ra-2")).toBeNull();
    fireEvent.click(screen.getByTestId("risk-alerts-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("risk-alerts-summary")).toBeTruthy();
    expect(screen.getByTestId("risk-alert-select-ra-2")).toBeTruthy();
  });

  it("表格枚举为中文，不含英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("risk-alerts-table")).toBeTruthy());
    const table = screen.getByTestId("risk-alerts-table").textContent ?? "";
    expect(table).toContain("员工行为");
    expect(table).toContain("进行中");
    expect(table).not.toContain("staff_behavior");
    expect(table).not.toContain("open");
  });
});
