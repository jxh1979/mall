import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ReceivablesAnalyticsPage } from "./ReceivablesAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
// ETL 快照回退口径：欠费总额 12345 分、坏账预警区 6000 分。
const DASH = { businessDate: "2026-07-18", metrics: { "ar.balance": { value: 12345 }, "ar.aging_90p": { value: 6000 } } };

function makeApi(metricValues: Record<string, number> | null): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (!metricValues) throw new Error("forbidden");
        const metric = new URL(url, "http://x").searchParams.get("metric") ?? "";
        return { metricKey: metric, valueType: "balance", summaryValue: metricValues[metric] ?? null, rows: [], totalRows: 0, columns: [] };
      }
      if (url.includes("/dashboards/finance")) return DASH;
      return [];
    }),
    getPaged: vi.fn(),
    post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: PERMS, navigateTo,
  };
  render(<ReceivablesAnalyticsPage {...props} />);
  return navigateTo;
}

describe("ReceivablesAnalyticsPage 第四批接线：欠费总额/坏账预警区走统一明细端点", () => {
  it("统一端点就绪 → 数字=端点合计（元），并提供「完整明细」出口", async () => {
    const navigateTo = renderPage(makeApi({ "finance.ar_balance": 99900, "finance.ar_balance_aging90p": 45600 }));
    await waitFor(() => expect(screen.getByTestId("receivables-balance").textContent).toContain("999.00"));
    expect(screen.getByTestId("receivables-aging90p").textContent).toContain("456.00");
    fireEvent.click(screen.getByTestId("receivables-metric-detail-balance"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", "finance.ar_balance||");
  });

  it("端点失败（弱权限财务账号）→ 回退 ETL 快照值并隐藏出口", async () => {
    renderPage(makeApi(null));
    await waitFor(() => expect(screen.getByTestId("receivables-balance").textContent).toContain("123.45"));
    expect(screen.getByTestId("receivables-aging90p").textContent).toContain("60.00");
    expect(screen.queryByTestId("receivables-metric-detail-balance")).toBeNull();
    expect(screen.queryByTestId("receivables-metric-detail-aging90p")).toBeNull();
  });
});
