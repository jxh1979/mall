import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ReceivablesAnalyticsPage } from "./ReceivablesAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const DASH = {
  metrics: {
    "ar.balance": { value: 123456 },
    "ar.aging_90p": { value: 20000 },
    "ar.collected_mtd": { value: 50000 },
    "ar.receivable_mtd": { value: 80000 },
    "ar.collection_rate_mtd": { value: 0.625 },
    "ar.aging_0_30": { value: 40000 },
    "ar.aging_31_60": { value: 30000 },
    "ar.aging_61_90": { value: 20000 },
  },
};

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/dashboards/finance") ? DASH : [])),
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
  render(<ReceivablesAnalyticsPage {...props} />);
  return navigateTo;
}

describe("ReceivablesAnalyticsPage 欠费汇总 KPI 跳应收应付台账", () => {
  it.each([
    "receivables-balance",
    "receivables-aging90p",
    "receivables-rate",
  ])("点 %s → navigateTo(ADM-FIN-001)", async (testid) => {
    const nav = renderPage();
    await waitFor(() => expect(screen.getByTestId(testid)).toBeTruthy());
    fireEvent.click(screen.getByTestId(testid));
    expect(nav).toHaveBeenCalledWith("ADM-FIN-001");
  });
});
