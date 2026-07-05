import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { WorkOrderSlaPage } from "./WorkOrderSlaPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const DASH = {
  metrics: {
    "wo.response_hours_p50": { value: 1.2 },
    "wo.handle_hours_p50": { value: 3.4 },
    "wo.overdue_rate": { value: 0.1 },
    "wo.backlog": { value: 5 },
    "mwo.handle_hours_p50": { value: 2.1 },
    "mwo.overdue_rate": { value: 0.2 },
    "mwo.backlog": { value: 3 },
  },
};

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/dashboards/facility") ? DASH : [])),
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
  render(<WorkOrderSlaPage {...props} />);
  return navigateTo;
}

describe("WorkOrderSlaPage 工单时效 KPI 按来源跳转", () => {
  it.each([
    ["wo-response-p50", "ADM-FAC-001"],
    ["wo-handle-p50", "ADM-FAC-001"],
    ["wo-overdue-rate", "ADM-FAC-001"],
    ["wo-backlog", "ADM-FAC-001"],
    ["mwo-handle-p50", "ADM-MER-001"],
    ["mwo-overdue-rate", "ADM-MER-001"],
    ["mwo-backlog", "ADM-MER-001"],
  ])("点 %s → navigateTo(%s)", async (testid, target) => {
    const nav = renderPage();
    await waitFor(() => expect(screen.getByTestId(testid)).toBeTruthy());
    fireEvent.click(screen.getByTestId(testid));
    expect(nav).toHaveBeenCalledWith(target);
  });
});
