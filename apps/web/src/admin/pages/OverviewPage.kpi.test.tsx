import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const OVERVIEW = {
  metrics: {
    "revenue.ticket": { value: 12345 },
    "revenue.bar": { value: 6789 },
    "traffic.amusement": { value: 42 },
    "verify.done": { value: 17 },
    "member.new": { value: 5 },
    "risk.pending_review": { value: 3 },
  },
};

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async () => OVERVIEW),
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
  render(<OverviewPage {...props} />);
  return navigateTo;
}

describe("OverviewPage 跨域汇总 KPI 跳转（消除数据孤岛）", () => {
  it.each([
    ["overview-kpi-ticket-value", "ADM-TKT-003"],
    ["overview-kpi-bar-value", "ADM-BAR-002"],
    ["overview-kpi-amusement-value", "ADM-AMU-001"],
    ["overview-kpi-verify-value", "ADM-TKT-003"],
    ["overview-kpi-member-value", "ADM-CUS-001"],
    ["overview-kpi-risk-value", "ADM-GOV-006"],
  ])("点 %s → navigateTo(%s)", async (testid, target) => {
    const nav = renderPage();
    await waitFor(() => expect(screen.getByTestId(testid)).toBeTruthy());
    fireEvent.click(screen.getByTestId(testid));
    expect(nav).toHaveBeenCalledWith(target);
  });
});
