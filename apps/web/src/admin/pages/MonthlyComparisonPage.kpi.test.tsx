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
