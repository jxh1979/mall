import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SettlementReviewPage } from "./SettlementReviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const ROWS = [
  { id: "s1", transactionNo: "ST-1", projectId: "p1", merchantId: "m1", verifiedCount: 2, numPeople: 2, verifiedAmountCent: 20000, settlementStatus: "draft", createdAt: "2026-07-18T10:00:00Z" },
];

function makeApi(metricFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricFails) throw new Error("forbidden");
        const metric = new URL(url, "http://x").searchParams.get("metric") ?? "";
        const values: Record<string, number> = {
          "settlement.transaction_count": 9, "settlement.verified_amount": 88800, "settlement.verified_count": 21,
        };
        return { metricKey: metric, valueType: "additive", summaryValue: values[metric] ?? null, rows: [], totalRows: 0, columns: [] };
      }
      return [];
    }),
    getPaged: vi.fn(async () => ({ items: ROWS, total: 3, page: 1, size: 20 })),
    post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: PERMS, navigateTo,
  };
  render(<SettlementReviewPage {...props} />);
  return navigateTo;
}

describe("SettlementReviewPage 第四批接线：结算笔数/核销金额/核销张数走统一明细端点", () => {
  it("无筛选就绪 → 三卡=端点合计（笔数原样/金额元/张数原样），提供「完整明细」出口", async () => {
    const navigateTo = renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("settlement-review-kpi-count").textContent).toBe("9"));
    expect(screen.getByTestId("settlement-review-kpi-amount").textContent).toContain("888.00");
    expect(screen.getByTestId("settlement-review-kpi-tickets").textContent).toBe("21");
    fireEvent.click(screen.getByTestId("settlement-review-metric-detail-count"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", "settlement.transaction_count||");
  });

  it("端点失败 → 结算笔数回退分页 total、金额/张数显示「—」并隐藏出口", async () => {
    renderPage(makeApi(true));
    await waitFor(() => expect(screen.getByTestId("settlement-review-kpi-count").textContent).toBe("3"));
    expect(screen.getByTestId("settlement-review-kpi-amount").textContent).toBe("—");
    expect(screen.getByTestId("settlement-review-kpi-tickets").textContent).toBe("—");
    expect(screen.queryByTestId("settlement-review-metric-detail-count")).toBeNull();
  });

  it("有筛选（enabled 关，筛选转换）→ 笔数回退当前筛选 total、金额/张数「—」并隐藏出口", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("settlement-review-kpi-count").textContent).toBe("9"));
    fireEvent.change(screen.getByTestId("settlement-review-merchant"), { target: { value: "m1" } });
    fireEvent.click(screen.getByTestId("settlement-review-search-submit"));
    await waitFor(() => expect(screen.getByTestId("settlement-review-kpi-amount").textContent).toBe("—"));
    expect(screen.getByTestId("settlement-review-kpi-count").textContent).toBe("3");
    expect(screen.getByTestId("settlement-review-kpi-tickets").textContent).toBe("—");
    expect(screen.queryByTestId("settlement-review-metric-detail-count")).toBeNull();
  });
});
