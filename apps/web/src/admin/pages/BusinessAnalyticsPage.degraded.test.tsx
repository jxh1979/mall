import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BusinessAnalyticsPage } from "./BusinessAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const SNAPSHOTS = [
  { snapshotId: "s1", metricKey: "revenue.bar", domain: "revenue", businessDate: "2026-07-01", actualValue: 100, baselineValue: 90, varianceRate: 0.1, trendDirection: "up", segmentType: "platform" },
];
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// failing=会抛错的路径子串；其余副资源正常返回。主资源=business-metric-snapshots 恒成功。
function makeApi(failing: string): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => {
      if (path.includes(failing)) throw new Error("boom");
      if (path.includes("business-metric-snapshots")) return SNAPSHOTS;
      if (path.includes("traffic-hourly")) return { buckets: [] };
      if (path.includes("metric-definitions")) return [{ metricKey: "revenue.bar", domain: "revenue", unit: "元", status: "active" }];
      if (path.includes("metric-anomalies")) return [{ id: "a1", metricKey: "revenue.bar", riskLevel: "high", observedValue: 5, expectedValue: 100, status: "open" }];
      if (path.includes("review-locks")) return [];
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BusinessAnalyticsPage {...props} />);
}

describe("BusinessAnalyticsPage 副资源降级（批2）", () => {
  afterEach(() => window.history.replaceState({}, "", "/admin/analytics/business"));

  it("异常指标接口失败 → 主快照表与快照KPI照常，异常KPI显“—”不静默归零，降级提示出现，整页不报错", async () => {
    const setGlobalError = vi.fn();
    renderPage(makeApi("metric-anomalies"), setGlobalError);
    await waitFor(() => expect(screen.getByTestId("business-analytics-table")).toBeTruthy());
    // 主 KPI（指标快照）取自主资源快照，仍为真实计数 1
    expect(screen.getByTestId("business-analytics-kpi-snapshots").textContent).toBe("1");
    // 异常 KPI 副资源失败：显示“—”而非 0
    expect(screen.getByTestId("business-analytics-kpi-anomalies").textContent).toBe("—");
    expect(screen.getByTestId("business-analytics-anomalies-degraded")).toBeTruthy();
    // 主资源正常，未触发整页错误
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
    expect(screen.queryByTestId("business-analytics-empty")).toBeNull();
  });

  it("指标定义接口失败 → 定义表降级提示，主快照表仍在", async () => {
    renderPage(makeApi("metric-definitions"));
    await waitFor(() => expect(screen.getByTestId("business-analytics-table")).toBeTruthy());
    expect(screen.getByTestId("business-analytics-definitions-degraded")).toBeTruthy();
  });
});
