import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const OVERVIEW = {
  asOf: "2026-07-18T02:00:00Z",
  scope: null,
  businessDate: "2026-07-17",
  metrics: {
    "revenue.ticket": { value: 999 }, // 快照旧值：接线后数字应以统一端点为准
    "revenue.bar": { value: 100 },
    "ar.balance": { value: 111 },
  },
};

const TICKET_ROWS = [{ id: "to-1", amountCent: 10000 }, { id: "to-2", amountCent: 2500 }];
const TICKET_DETAIL = { metricKey: "revenue.ticket", valueType: "additive", summaryValue: 12500, rows: TICKET_ROWS, totalRows: 2, columns: [] };
const AR_DETAIL = { metricKey: "finance.ar_balance", valueType: "balance", summaryValue: 70000, rows: [{ id: "ar-1", unpaidAmountCent: 70000 }], totalRows: 1, columns: [] };
const BAR_ROWS = [{ id: "bbs-1", billId: "bill-1", settlementAmountCent: 8800 }, { id: "bbs-2", billId: "bill-2", settlementAmountCent: 1200 }];
const BAR_DETAIL = { metricKey: "revenue.bar", valueType: "additive", summaryValue: 10000, rows: BAR_ROWS, totalRows: 2, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=finance.ar_balance")) return AR_DETAIL;
        if (url.includes("metric=revenue.bar")) return BAR_DETAIL;
        return TICKET_DETAIL;
      }
      return OVERVIEW;
    }),
    getPaged: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo,
  };
  render(<OverviewPage {...props} />);
  return navigateTo;
}

describe("OverviewPage 首批接线：数字改走统一明细端点（数字=明细合计）", () => {
  it("票务收入数字=统一端点合计=明细行求和；带完整明细入口且区间=经营日", async () => {
    const api = makeApi();
    renderPage(api);

    // 一致性：卡片数字 == summaryValue == mock 明细行求和（同一端点同源）。
    const rowSum = TICKET_ROWS.reduce((sum, row) => sum + row.amountCent, 0);
    await waitFor(() => expect(screen.getByTestId("overview-kpi-ticket-value").textContent).toBe(String(rowSum)));
    expect(rowSum).toBe(TICKET_DETAIL.summaryValue);
    expect(screen.getByTestId("overview-metric-detail-ticket")).toBeTruthy();

    const detailCall = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .map((call) => call[0] as string)
      .find((url) => url.includes("metric=revenue.ticket"));
    expect(detailCall).toContain("from=2026-07-17");
    expect(detailCall).toContain("to=2026-07-17");
  });

  it("应收欠费余额卡（余额类）：数字=统一端点区间末值，可跳欠费与回款", async () => {
    const api = makeApi();
    const navigateTo = renderPage(api);

    await waitFor(() => expect(screen.getByTestId("overview-kpi-ar-value").textContent).toBe("70000"));
    expect(screen.getByTestId("overview-metric-detail-ar")).toBeTruthy();
    screen.getByTestId("overview-kpi-ar-value").click();
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-004");
  });

  it("酒吧收入卡（第二批接线）：数字=统一端点合计=明细行求和，带完整明细入口且区间=经营日", async () => {
    const api = makeApi();
    renderPage(api);

    const rowSum = BAR_ROWS.reduce((sum, row) => sum + row.settlementAmountCent, 0);
    await waitFor(() => expect(screen.getByTestId("overview-kpi-bar-value").textContent).toBe(String(rowSum)));
    expect(rowSum).toBe(BAR_DETAIL.summaryValue);
    expect(screen.getByTestId("overview-metric-detail-bar")).toBeTruthy();

    const detailCall = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .map((call) => call[0] as string)
      .find((url) => url.includes("metric=revenue.bar"));
    expect(detailCall).toContain("from=2026-07-17");
    expect(detailCall).toContain("to=2026-07-17");
  });

  it("统一端点失败（含无权限）→ 回退快照值并隐藏完整明细入口，体验不降级", async () => {
    const api = makeApi(true);
    renderPage(api);

    await waitFor(() => expect(screen.getByTestId("overview-kpi-ticket-value").textContent).toBe("999"));
    expect(screen.queryByTestId("overview-metric-detail-ticket")).toBeNull();
    expect(screen.getByTestId("overview-kpi-ar-value").textContent).toBe("111");
    expect(screen.queryByTestId("overview-metric-detail-ar")).toBeNull();
    // 第二批：酒吧收入卡同样回退快照值并隐藏入口。
    expect(screen.getByTestId("overview-kpi-bar-value").textContent).toBe("100");
    expect(screen.queryByTestId("overview-metric-detail-bar")).toBeNull();
  });
});
