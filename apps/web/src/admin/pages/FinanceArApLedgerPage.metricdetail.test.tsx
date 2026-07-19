import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { FinanceArApLedgerPage } from "./FinanceArApLedgerPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(metricFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricFails) throw new Error("forbidden");
        const metric = new URL(url, "http://x").searchParams.get("metric") ?? "";
        const values: Record<string, number> = { "finance.ar_balance": 70000, "finance.ap_balance": 30000 };
        return { metricKey: metric, valueType: "balance", summaryValue: values[metric] ?? null, rows: [], totalRows: 0, columns: [] };
      }
      return [];
    }),
    getPaged: vi.fn(async () => ({ items: [], total: 0, page: 1, size: 10 })),
    post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: PERMS, navigateTo,
  };
  render(<FinanceArApLedgerPage {...props} />);
  return navigateTo;
}

describe("FinanceArApLedgerPage 第四批接线：应收/应付欠款余额走统一明细端点", () => {
  it("端点就绪 → 应收/应付余额=端点合计（元），提供「完整明细」出口", async () => {
    const navigateTo = renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("finance-ledger-kpi-ar").textContent).toContain("700.00"));
    expect(screen.getByTestId("finance-ledger-kpi-ap").textContent).toContain("300.00");
    fireEvent.click(screen.getByTestId("finance-ledger-metric-detail-ap"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", "finance.ap_balance||");
  });

  it("端点失败（弱权限财务账号）→ 显示「—」并隐藏出口", async () => {
    renderPage(makeApi(true));
    await waitFor(() => expect(screen.getByTestId("finance-ledger-kpi-ar").textContent).toBe("—"));
    expect(screen.getByTestId("finance-ledger-kpi-ap").textContent).toBe("—");
    expect(screen.queryByTestId("finance-ledger-metric-detail-ar")).toBeNull();
    expect(screen.queryByTestId("finance-ledger-metric-detail-ap")).toBeNull();
  });
});
