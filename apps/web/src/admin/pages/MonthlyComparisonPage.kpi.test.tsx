import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MonthlyComparisonPage } from "./MonthlyComparisonPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const RESPONSE = {
  month: "2026-07",
  rows: [
    { metricKey: "revenue.ticket", label: "售票收入", domain: "ticketing", unit: "cent", aggregation: "monthly_sum", current: 100, prevMonth: 90, momPct: 0.1 },
    { metricKey: "energy.total_electricity", label: "用电量", domain: "energy", unit: "kwh", aggregation: "monthly_sum", current: 1200, prevMonth: 1100, momPct: 0.09 },
    { metricKey: "occupancy.rate", label: "出租率", domain: "leasing", unit: "rate", aggregation: "period_end", current: 0.82, prevMonth: 0.8, momPct: 0.02 },
  ],
};

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/monthly-comparison") ? RESPONSE : [])),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo,
  };
  render(<MonthlyComparisonPage {...props} />);
  return navigateTo;
}

describe("MonthlyComparisonPage 指标按业务域跳源页", () => {
  it.each([
    ["monthly-comparison-metric-revenue.ticket", "ADM-TKT-003"],
    ["monthly-comparison-metric-energy.total_electricity", "ADM-ENE-001"],
    ["monthly-comparison-metric-occupancy.rate", "ADM-LEASE-002"],
  ])("点 %s → navigateTo(%s)", async (testid, target) => {
    const nav = renderPage();
    await waitFor(() => expect(screen.getByTestId(testid)).toBeTruthy());
    fireEvent.click(screen.getByTestId(testid));
    expect(nav).toHaveBeenCalledWith(target);
  });
});

describe("MonthlyComparisonPage 行明细抽屉（整行可点·口径中文·金额率格式化）", () => {
  it("点指标行 → 抽屉含中文口径+格式化本月/环比，无英文口径码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("monthly-comparison-row-revenue.ticket")).toBeTruthy());
    fireEvent.click(screen.getByTestId("monthly-comparison-row-revenue.ticket"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("售票收入");
    expect(text).toContain("月累计");
    expect(text).toContain("¥1"); // cent=100 → ¥1
    expect(text).toContain("+10.00%"); // momPct=0.1
    expect(text).not.toContain("monthly_sum");
  });

  it("点指标域跳转按钮 → navigateTo、不弹抽屉（stopPropagation）", async () => {
    const nav = renderPage();
    await waitFor(() => expect(screen.getByTestId("monthly-comparison-metric-revenue.ticket")).toBeTruthy());
    fireEvent.click(screen.getByTestId("monthly-comparison-metric-revenue.ticket"));
    expect(nav).toHaveBeenCalledWith("ADM-TKT-003");
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点指标行 → Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("monthly-comparison-row-occupancy.rate")).toBeTruthy());
    fireEvent.click(screen.getByTestId("monthly-comparison-row-occupancy.rate"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
