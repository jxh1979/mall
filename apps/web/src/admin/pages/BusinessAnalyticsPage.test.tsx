import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BusinessAnalyticsPage } from "./BusinessAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const SNAPSHOTS = [
  { snapshotId: "s1", metricKey: "revenue.bar", domain: "revenue", businessDate: "2026-07-01", actualValue: 100, baselineValue: 90, varianceRate: 0.1, trendDirection: "up", segmentType: "platform" },
];

const EXISTING_LOCKS = [
  { id: "brl-1", projectId: null, domain: "all", operatorNote: "上周复盘锁定", metricCount: 5, lockedBy: "admin", lockedAt: "2026-07-01T09:30:00Z", idempotencyKey: "k1" },
];

// V216：按路径返回不同数据；review-locks 返回已锁定列表，其它返回快照/空。
function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/analytics/review-locks")) return Promise.resolve(EXISTING_LOCKS);
    if (path.includes("/analytics/business-metric-snapshots")) return Promise.resolve(SNAPSHOTS);
    if (path.includes("/analytics/traffic-hourly")) return Promise.resolve({ buckets: [] });
    return Promise.resolve([]);
  });
}

function makeApi(get = getResolver(), post = vi.fn().mockResolvedValue({ id: "brl-new" })): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const fullProps: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BusinessAnalyticsPage {...fullProps} />);
}

describe("BusinessAnalyticsPage 复盘锁定真落库(V216)", () => {
  it("加载时读回已锁定复盘并展示(刷新后仍可见)", async () => {
    const get = getResolver();
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/analytics/review-locks"));
    await waitFor(() => expect(screen.getByTestId("business-analytics-review-locks-list")).toBeTruthy());
    expect(screen.getByTestId("business-analytics-review-locks-list").textContent).toContain("上周复盘锁定");
  });

  it("锁定按钮调用 POST /review-locks(带幂等键),成功后刷新(引导式:入口→确认→提交)", async () => {
    const post = vi.fn().mockResolvedValue({ id: "brl-new" });
    renderPage(makeApi(getResolver(), post));
    // 引导式 phase-9：先点主按钮开弹窗，翻到确认页再点提交（默认复盘说明已带值）。
    await waitFor(() => expect(screen.getByTestId("business-analytics-lock-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-lock-open"));
    fireEvent.click(screen.getByTestId("business-analytics-lock-guided-next"));
    await waitFor(() => expect(screen.getByTestId("business-analytics-lock-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-lock-snapshot"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/analytics/review-locks",
      expect.objectContaining({ operatorNote: expect.any(String), domain: "all", idempotencyKey: expect.stringContaining("review-lock-") }),
    ));
  });

  it("复盘说明为空时步内拦截不发请求,弹窗内提示错误", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("business-analytics-lock-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-lock-open"));
    fireEvent.change(screen.getByTestId("business-analytics-lock-note"), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("business-analytics-lock-guided-next"));
    await waitFor(() => expect(screen.getByTestId("business-analytics-lock-guided-error")).toBeTruthy());
    expect(screen.queryByTestId("business-analytics-lock-guided-summary")).toBeNull();
    expect(post).not.toHaveBeenCalled();
  });
});

describe("BusinessAnalyticsPage 分组列英文枚举业务化", () => {
  it("非平台口径分组的 segmentType 渲染为中文（project→项目），不直出英文码", async () => {
    const get = vi.fn().mockImplementation((path: string) => {
      if (path.includes("/analytics/review-locks")) return Promise.resolve([]);
      if (path.includes("/analytics/business-metric-snapshots")) {
        return Promise.resolve([
          { snapshotId: "s2", metricKey: "revenue.bar", domain: "bar", businessDate: "2026-07-01", actualValue: 100, baselineValue: 90, varianceRate: 0.1, trendDirection: "up", segmentType: "project", segmentId: "pj-1" },
        ]);
      }
      if (path.includes("/analytics/traffic-hourly")) return Promise.resolve({ buckets: [] });
      return Promise.resolve([]);
    });
    renderPage(makeApi(get));
    await waitFor(() => expect(screen.getByTestId("business-analytics-table")).toBeTruthy());
    const table = screen.getByTestId("business-analytics-table").textContent ?? "";
    expect(table).toContain("项目:pj-1");
    expect(table).not.toContain("project:pj-1");
  });
});

describe("BusinessAnalyticsPage 行明细抽屉（整行可点·域/趋势/风险/状态中文）", () => {
  function richResolver() {
    return vi.fn().mockImplementation((path: string) => {
      if (path.includes("/analytics/review-locks")) return Promise.resolve([]);
      if (path.includes("/analytics/business-metric-snapshots")) return Promise.resolve(SNAPSHOTS);
      if (path.includes("/analytics/traffic-hourly")) return Promise.resolve({ buckets: [] });
      if (path.includes("/analytics/metric-definitions")) {
        return Promise.resolve([{ metricKey: "revenue.bar", domain: "bar", unit: "元", calculationNote: "酒水实收合计", status: "active" }]);
      }
      if (path.includes("/analytics/metric-anomalies")) {
        return Promise.resolve([{ id: "an-1", metricKey: "revenue.bar", riskLevel: "high", observedValue: 100, expectedValue: 200, summary: "环比骤降", status: "open", detectedAt: "2026-07-02T10:00:00Z" }]);
      }
      return Promise.resolve([]);
    });
  }

  it("点指标快照行 → 抽屉含中文域/趋势/口径快照值，无英文枚举", async () => {
    renderPage(makeApi(richResolver()));
    await waitFor(() => expect(screen.getByTestId("business-analytics-row-s1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-row-s1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("上升"); // trendDirection up
    expect(text).toContain("平台"); // segmentType platform
    expect(text).not.toContain("up");
    expect(text).not.toContain("platform");
  });

  it("点指标定义行 → 抽屉含中文状态（启用），无 active 原码", async () => {
    renderPage(makeApi(richResolver()));
    await waitFor(() => expect(screen.getByTestId("business-analytics-definition-row-revenue.bar")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-definition-row-revenue.bar"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("酒水实收合计");
    expect(text).toContain("启用");
    expect(text).not.toContain("active");
  });

  it("点异常指标行 → 抽屉含中文风险（高）/状态（待处置）+中文时间，无英文枚举与 ISO", async () => {
    renderPage(makeApi(richResolver()));
    await waitFor(() => expect(screen.getByTestId("business-analytics-anomaly-row-an-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-anomaly-row-an-1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("高");
    expect(text).toContain("待处置");
    expect(text).toContain("环比骤降");
    expect(text).not.toContain("high");
    expect(text).not.toContain("open");
    expect(text).not.toContain("2026-07-02T");
  });

  it("点异常指标行 → Esc 关闭抽屉", async () => {
    renderPage(makeApi(richResolver()));
    await waitFor(() => expect(screen.getByTestId("business-analytics-anomaly-row-an-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-anomaly-row-an-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
