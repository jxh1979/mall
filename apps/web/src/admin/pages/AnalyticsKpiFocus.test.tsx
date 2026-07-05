import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BusinessAnalyticsPage } from "./BusinessAnalyticsPage";
import { T3AReadonlyAnalyticsPage } from "./T3AReadonlyAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function baseProps(api: AdminApiClient): AdminPageProps {
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
}

describe("经营分析 KPI 卡可点 → 聚焦本页对应明细表", () => {
  afterEach(() => window.history.replaceState({}, "", "/admin/analytics/business"));

  function makeApi(): AdminApiClient {
    return {
      get: vi.fn(async (url: string) => {
        if (url.includes("business-metric-snapshots")) return [
          { snapshotId: "s1", metricKey: "revenue.bar", domain: "bar", actualValue: 100, baselineValue: 90, varianceRate: 0.1, trendDirection: "up", businessDate: "2026-07-05", segmentType: "platform" },
          { snapshotId: "s2", metricKey: "revenue.ticket", domain: "ticket", actualValue: 80, baselineValue: 100, varianceRate: -0.2, trendDirection: "down", businessDate: "2026-07-05", segmentType: "platform" },
        ];
        if (url.includes("traffic-hourly")) return { buckets: [] };
        if (url.includes("metric-definitions")) return [{ metricKey: "revenue.bar", domain: "bar", unit: "元", status: "active" }];
        if (url.includes("metric-anomalies")) return [{ id: "a1", metricKey: "revenue.bar", riskLevel: "high", observedValue: 5, expectedValue: 100, status: "open" }];
        if (url.includes("review-locks")) return [];
        return [];
      }),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }

  it("KPI 卡默认可点（4 张卡里 3 张为 admin-kpi-value 按钮）", async () => {
    render(<BusinessAnalyticsPage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("business-analytics-kpi-snapshots")).toBeTruthy());
    expect(screen.getByTestId("business-analytics-kpi-upward")).toBeTruthy();
    expect(screen.getByTestId("business-analytics-kpi-anomalies")).toBeTruthy();
  });

  it("点“指标快照”KPI → ?kpi=snapshots，只留指标快照表、隐藏定义/异常表", async () => {
    render(<BusinessAnalyticsPage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("business-analytics-kpi-snapshots")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-kpi-snapshots"));
    expect(window.location.search).toContain("kpi=snapshots");
    expect(screen.getByTestId("business-analytics-kpi-focus-banner")).toBeTruthy();
    expect(screen.getByTestId("business-analytics-table")).toBeTruthy();
    expect(screen.queryByTestId("business-analytics-definitions-table")).toBeNull();
    expect(screen.queryByTestId("business-analytics-anomalies-table")).toBeNull();
  });

  it("点“异常指标”KPI → 只留异常表；返回全部恢复 KPI 卡", async () => {
    render(<BusinessAnalyticsPage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("business-analytics-kpi-anomalies")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-kpi-anomalies"));
    expect(screen.getByTestId("business-analytics-anomalies-table")).toBeTruthy();
    expect(screen.queryByTestId("business-analytics-table")).toBeNull();
    fireEvent.click(screen.getByTestId("business-analytics-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("business-analytics-summary")).toBeTruthy();
  });

  it("点“上升趋势”KPI → 指标快照表只剩上升行", async () => {
    render(<BusinessAnalyticsPage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("business-analytics-kpi-upward")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-kpi-upward"));
    const table = screen.getByTestId("business-analytics-table").textContent ?? "";
    expect(table).toContain("上升"); // up 行保留
    expect(table).toContain("酒吧"); // 酒吧收入(up)
    expect(table).not.toContain("下降"); // down 行剔除
  });
});

describe("T3-A 只读分析 KPI 卡可点 → 聚焦对应明细表", () => {
  afterEach(() => window.history.replaceState({}, "", "/admin/analytics/t3a"));

  function makeApi(): AdminApiClient {
    return {
      get: vi.fn(async (url: string) => {
        if (url.includes("merchant-health")) return [{ merchantId: "m1", score: 88, scoreLevel: "good", riskCount: 0, overdueArAmountCents: 0 }];
        if (url.includes("staff-service-score")) return [{ scoreId: "ss1", staffName: "小李", roleCode: "waiter", serviceScore: 92, complaintCount: 0, riskLevel: "low" }];
        if (url.includes("robot")) return [{ metricId: "r1", robotId: "R-1", providerCode: "prov", taskCount: 3, successCount: 3, failureCount: 0, businessDate: "2026-07-05" }];
        return [];
      }),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }

  it("点“商户健康分”KPI → 只留商户健康表、隐藏机器人/员工表", async () => {
    render(<T3AReadonlyAnalyticsPage {...baseProps(makeApi())} />);
    await waitFor(() => expect(screen.getByTestId("t3a-kpi-merchant")).toBeTruthy());
    fireEvent.click(screen.getByTestId("t3a-kpi-merchant"));
    expect(window.location.search).toContain("kpi=merchant");
    expect(screen.getByTestId("t3a-merchant-table")).toBeTruthy();
    expect(screen.queryByTestId("t3a-robot-table")).toBeNull();
    expect(screen.queryByTestId("t3a-staff-table")).toBeNull();
    fireEvent.click(screen.getByTestId("t3a-kpi-focus-clear"));
    expect(screen.getByTestId("t3a-summary")).toBeTruthy();
    expect(screen.getByTestId("t3a-robot-table")).toBeTruthy();
  });
});
