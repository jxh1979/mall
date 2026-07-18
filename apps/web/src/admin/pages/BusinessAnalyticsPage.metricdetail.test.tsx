import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BusinessAnalyticsPage } from "./BusinessAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 快照：酒吧/票务各 100（旧值 → 占比 50/50）；统一端点酒吧实收 300 → 占比 75/25。
const SNAPSHOTS = [
  { snapshotId: "s1", metricKey: "revenue.bar", domain: "revenue", businessDate: "2026-07-01", actualValue: 100, baselineValue: 90, varianceRate: 0.1, trendDirection: "up", segmentType: "platform" },
  { snapshotId: "s2", metricKey: "revenue.ticket", domain: "revenue", businessDate: "2026-07-01", actualValue: 100, baselineValue: 90, varianceRate: 0.1, trendDirection: "up", segmentType: "platform" },
];

const BAR_ROWS = [{ id: "bbs-1", billId: "bill-1", settlementAmountCent: 200 }, { id: "bbs-2", billId: "bill-2", settlementAmountCent: 100 }];
const BAR_DETAIL = { metricKey: "revenue.bar", valueType: "additive", summaryValue: 300, rows: BAR_ROWS, totalRows: 2, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        return BAR_DETAIL;
      }
      if (url.includes("/analytics/business-metric-snapshots")) return SNAPSHOTS;
      if (url.includes("/analytics/traffic-hourly")) return { buckets: [] };
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  };
  render(<BusinessAnalyticsPage {...props} />);
}

describe("BusinessAnalyticsPage 第二批接线：品类占比酒吧实收改走统一明细端点（数字=明细合计）", () => {
  it("酒吧实收=统一端点合计=明细行求和（占比随之更新），带完整明细入口且区间=快照经营日", async () => {
    const api = makeApi();
    renderPage(api);

    // 一致性：合计 == mock 明细行求和；酒吧 300 vs 票务 100 → 占比 75/25。
    const rowSum = BAR_ROWS.reduce((sum, row) => sum + row.settlementAmountCent, 0);
    expect(rowSum).toBe(BAR_DETAIL.summaryValue);
    await waitFor(() => expect(screen.getByTestId("business-analytics-category-share-note").textContent).toContain("酒吧 75.0%"));
    expect(screen.getByTestId("business-analytics-metric-detail-bar")).toBeTruthy();

    const detailCall = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .map((call) => call[0] as string)
      .find((url) => url.includes("metric=revenue.bar"));
    expect(detailCall).toContain("from=2026-07-01");
    expect(detailCall).toContain("to=2026-07-01");
  });

  it("统一端点失败（含无权限）→ 回退最新快照值（占比 50/50）并隐藏完整明细入口", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("business-analytics-category-share-note").textContent).toContain("酒吧 50.0%"));
    expect(screen.queryByTestId("business-analytics-metric-detail-bar")).toBeNull();
  });
});
