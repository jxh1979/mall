import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { T3AReadonlyAnalyticsPage } from "./T3AReadonlyAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 六个只读分析接口按路径返回；含需业务化的英文枚举字段与 ISO 日期。
function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/staff-service-score")) {
      return Promise.resolve([{ scoreId: "ss-1", staffName: "张三", roleCode: "supervisor", serviceScore: 88, complaintCount: 0, riskLevel: "low" }]);
    }
    if (path.includes("/merchant-health")) {
      return Promise.resolve([{ merchantId: "m-1", score: 90, scoreLevel: "high", riskCount: 1, overdueArAmountCents: 123400 }]);
    }
    if (path.includes("/metric-definitions")) {
      return Promise.resolve([{ metricKey: "revenue.bar", name: "酒吧收入", domain: "bar", unit: "元", status: "active" }]);
    }
    if (path.includes("/analytics/robot")) {
      return Promise.resolve([{ metricId: "rb-1", robotId: "R-01", providerCode: "acme", taskCount: 10, successCount: 9, failureCount: 1, businessDate: "2026-07-05" }]);
    }
    if (path.includes("/customer-intents")) {
      return Promise.resolve([{ intentId: "in-1", customerRef: "C-9", intentType: "咨询入驻", riskLevel: "medium", summary: "有意向" }]);
    }
    return Promise.resolve([]);
  });
}

function makeApi(get = getResolver()): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<T3AReadonlyAnalyticsPage {...props} />);
}

describe("T3AReadonlyAnalyticsPage 行明细抽屉", () => {
  afterEach(() => vi.restoreAllMocks());

  it("点商户健康分行 → 抽屉含该行值(逾期应收¥·等级中文)，无英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("t3a-merchant-row-m-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("t3a-merchant-row-m-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("m-1");
    expect(text).toContain("¥1,234"); // 123400 分 → ¥1,234
    expect(text).toContain("高"); // scoreLevel high
    expect(text).not.toContain("high");
  });

  it("点机器人行 → 抽屉日期中文化(ISO→年月日)，不直出 ISO", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("t3a-robot-row-rb-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("t3a-robot-row-rb-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("2026年7月5日");
    expect(text).not.toContain("2026-07-05");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("t3a-staff-row-ss-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("t3a-staff-row-ss-1"));
    expect(await screen.findByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
