import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { T3AReadonlyAnalyticsPage } from "./T3AReadonlyAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 六个只读分析接口按路径返回；员工服务分/商户健康/指标定义带需业务化的英文枚举字段。
function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/staff-service-score")) {
      return Promise.resolve([{ scoreId: "ss-1", staffName: "张三", roleCode: "supervisor", serviceScore: 88, complaintCount: 0, riskLevel: "low" }]);
    }
    if (path.includes("/merchant-health")) {
      return Promise.resolve([{ merchantId: "m-1", score: 90, scoreLevel: "high", riskCount: 1, overdueArAmountCents: 0 }]);
    }
    if (path.includes("/metric-definitions")) {
      return Promise.resolve([{ metricKey: "revenue.bar", name: "酒吧收入", domain: "bar", unit: "元", status: "active" }]);
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
  };
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

describe("T3AReadonlyAnalyticsPage 英文枚举业务化", () => {
  afterEach(() => vi.restoreAllMocks());

  it("员工岗位/风险、商户健康等级、指标定义业务域与状态渲染为中文，不直出英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("t3a-staff-table")).toBeTruthy());
    const staff = screen.getByTestId("t3a-staff-table").textContent ?? "";
    expect(staff).toContain("主管"); // supervisor
    expect(staff).toContain("低"); // riskLevel low
    expect(staff).not.toContain("supervisor");

    const merchant = screen.getByTestId("t3a-merchant-table").textContent ?? "";
    expect(merchant).toContain("高"); // scoreLevel high
    expect(merchant).not.toContain("high");

    const definition = screen.getByTestId("t3a-definition-table").textContent ?? "";
    expect(definition).toContain("酒吧"); // domain bar
    expect(definition).toContain("有效"); // status active
    expect(definition).not.toContain("active");
  });
});
