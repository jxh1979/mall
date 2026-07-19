import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BusinessAnalyticsPage } from "./BusinessAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// BI 异常处置流转:异常列表 + 逐条处置(已阅/已处置/误报/转告警)+ 状态筛选。
const SNAPSHOTS = [
  { snapshotId: "s1", metricKey: "revenue.bar", domain: "revenue", businessDate: "2026-07-01", actualValue: 100, baselineValue: 90, varianceRate: 0.1, trendDirection: "up", segmentType: "platform" },
];

const ANOMALIES = [
  { id: "ano_open", metricKey: "revenue.bar", riskLevel: "high", observedValue: 300, expectedValue: 100, summary: "bar surge", status: "open", detectedAt: "2026-07-02T00:00:00Z" },
  { id: "ano_done", metricKey: "revenue.net", riskLevel: "suspicious", observedValue: 50, expectedValue: 100, summary: "net drop", status: "resolved", detectedAt: "2026-07-01T00:00:00Z", disposedBy: "admin", dispositionNote: "已核实" },
];

function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/analytics/metric-anomalies")) return Promise.resolve(ANOMALIES);
    if (path.includes("/analytics/business-metric-snapshots")) return Promise.resolve(SNAPSHOTS);
    if (path.includes("/analytics/traffic-hourly")) return Promise.resolve({ buckets: [] });
    return Promise.resolve([]);
  });
}

function makeApi(get = getResolver(), post = vi.fn().mockResolvedValue({ id: "ano_open", status: "acknowledged" })): AdminApiClient {
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

describe("BusinessAnalyticsPage 异常处置流转(V306)", () => {
  it("非终态异常显示处置入口,标记已阅走 POST /dispose(带幂等键)", async () => {
    const post = vi.fn().mockResolvedValue({ id: "ano_open", status: "acknowledged" });
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("business-analytics-anomaly-dispose-ano_open")).toBeTruthy());
    // 终态(resolved)异常不给处置按钮。
    expect(screen.queryByTestId("business-analytics-anomaly-dispose-ano_done")).toBeNull();

    fireEvent.click(screen.getByTestId("business-analytics-anomaly-dispose-ano_open"));
    fireEvent.click(screen.getByTestId("business-analytics-dispose-guided-next"));
    await waitFor(() => expect(screen.getByTestId("business-analytics-dispose-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-dispose-submit"));

    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/analytics/metric-anomalies/ano_open/dispose",
      expect.objectContaining({ action: "acknowledge", idempotencyKey: expect.stringContaining("anomaly-dispose-ano_open-") }),
    ));
  });

  it("状态筛选按处置状态过滤异常行", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("business-analytics-anomaly-row-ano_open")).toBeTruthy());
    expect(screen.getByTestId("business-analytics-anomaly-row-ano_done")).toBeTruthy();

    fireEvent.change(screen.getByTestId("business-analytics-anomaly-status-filter"), { target: { value: "resolved" } });
    expect(screen.queryByTestId("business-analytics-anomaly-row-ano_open")).toBeNull();
    expect(screen.getByTestId("business-analytics-anomaly-row-ano_done")).toBeTruthy();
  });

  it("标记已处置必须填写说明,补齐说明后 POST 带 action=resolve+note", async () => {
    const post = vi.fn().mockResolvedValue({ id: "ano_open", status: "resolved" });
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("business-analytics-anomaly-dispose-ano_open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-anomaly-dispose-ano_open"));
    fireEvent.change(screen.getByTestId("business-analytics-dispose-action"), { target: { value: "resolve" } });
    // 无说明推进:校验拦截,不发请求。
    fireEvent.click(screen.getByTestId("business-analytics-dispose-guided-next"));
    await waitFor(() => expect(screen.getByTestId("business-analytics-dispose-guided-error").textContent).toContain("处置说明不能为空"));
    expect(post).not.toHaveBeenCalled();
    // 补齐说明后可提交。
    fireEvent.change(screen.getByTestId("business-analytics-dispose-note"), { target: { value: "已核对为大促真实波动" } });
    fireEvent.click(screen.getByTestId("business-analytics-dispose-guided-next"));
    await waitFor(() => expect(screen.getByTestId("business-analytics-dispose-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-dispose-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/analytics/metric-anomalies/ano_open/dispose",
      expect.objectContaining({ action: "resolve", note: "已核对为大促真实波动" }),
    ));
  });
});
