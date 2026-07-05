import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { EnergyAnalyticsPage } from "./EnergyAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const DASH = {
  metrics: {
    "energy.total_electricity": { value: 1200 },
    "energy.total_water": { value: 30 },
    "energy.total_gas": { value: 15 },
    "energy.total_heat_cold": { value: 8 },
    "energy.electricity_per_sqm": { value: 0.12 },
    "energy.abnormal_count": { value: 2 },
  },
};

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/dashboards/energy") ? DASH : [])),
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
  render(<EnergyAnalyticsPage {...props} />);
  return navigateTo;
}

describe("EnergyAnalyticsPage 能耗汇总 KPI 跳能源工作台", () => {
  it.each([
    "energy-total-electricity",
    "energy-total-water",
    "energy-total-gas",
    "energy-total-heat-cold",
    "energy-per-sqm",
    "energy-abnormal-count",
  ])("点 %s → navigateTo(ADM-ENE-001)", async (testid) => {
    const nav = renderPage();
    await waitFor(() => expect(screen.getByTestId(testid)).toBeTruthy());
    fireEvent.click(screen.getByTestId(testid));
    expect(nav).toHaveBeenCalledWith("ADM-ENE-001");
  });
});
