import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BusinessAnalyticsPage } from "./BusinessAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-9：新建复盘锁改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
const SNAPSHOTS = [
  { snapshotId: "s1", metricKey: "revenue.bar", domain: "revenue", businessDate: "2026-07-01", actualValue: 100, baselineValue: 90, varianceRate: 0.1, trendDirection: "up", segmentType: "platform" },
];

function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/analytics/review-locks")) return Promise.resolve([]);
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
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BusinessAnalyticsPage {...props} />);
  return { setActionResult };
}

describe("BusinessAnalyticsPage 引导式新建复盘锁(phase-9)", () => {
  it("页面默认只留入口按钮，不展示复盘说明输入", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("business-analytics-lock-open")).toBeTruthy());
    expect(screen.queryByTestId("business-analytics-lock-note")).toBeNull();
    expect(screen.queryByTestId("business-analytics-lock-guided")).toBeNull();
  });

  it("入口→步进→提交：POST /review-locks 带业务域与说明，成功后关闭弹窗", async () => {
    const post = vi.fn().mockResolvedValue({ id: "brl-new" });
    const { setActionResult } = renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("business-analytics-lock-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-lock-open"));

    fireEvent.change(screen.getByTestId("business-analytics-lock-note"), { target: { value: "月度复盘锁定" } });
    fireEvent.click(screen.getByTestId("business-analytics-lock-guided-next"));
    await waitFor(() => expect(screen.getByTestId("business-analytics-lock-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-lock-snapshot"));

    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/analytics/review-locks",
      expect.objectContaining({ operatorNote: "月度复盘锁定", domain: "all", metricCount: 1, idempotencyKey: expect.stringContaining("review-lock-") }),
    ));
    await waitFor(() => expect(screen.queryByTestId("business-analytics-lock-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "business_snapshot_locked" }));
  });

  it("步内校验拦截：复盘说明清空则不进下一步、不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("business-analytics-lock-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("business-analytics-lock-open"));
    fireEvent.change(screen.getByTestId("business-analytics-lock-note"), { target: { value: "   " } });
    fireEvent.click(screen.getByTestId("business-analytics-lock-guided-next"));
    expect(screen.getByTestId("business-analytics-lock-guided-error").textContent).toContain("复盘说明");
    expect(screen.queryByTestId("business-analytics-lock-guided-summary")).toBeNull();
    expect(post).not.toHaveBeenCalled();
  });
});
