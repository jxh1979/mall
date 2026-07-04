import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { PayrollTaxBracketsPage } from "./PayrollTaxBracketsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const BRACKETS = [
  { id: "ptb_labor_1", category: "labor_remuneration", seq: 1, incomeFloorCent: 1, incomeCeilingCent: 400000, incomeBaseBps: 10000, flatDeductionCent: 80000, rateBps: 2000, quickDeductionCent: 0 },
];

function makeApi(get = vi.fn().mockResolvedValue(BRACKETS), post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}),
  };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient, post = vi.fn()) {
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: post, setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<PayrollTaxBracketsPage {...props} />);
}

describe("PayrollTaxBracketsPage 个税税表(V217)", () => {
  it("读回税表分档并展示税率", async () => {
    const get = vi.fn().mockResolvedValue(BRACKETS);
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/staff/payroll/tax-brackets"));
    await waitFor(() => expect(screen.getByTestId("payroll-tax-brackets-table").textContent).toContain("20.00%"));
  });
});
