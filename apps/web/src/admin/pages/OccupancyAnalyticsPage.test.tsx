import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OccupancyAnalyticsPage } from "./OccupancyAnalyticsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const DASHBOARD = {
  business_date: "2026-07-04",
  metrics: {
    "occupancy.rate": { value: 0.82 },
    "occupancy.rentable_area": { value: 10000 },
    "occupancy.leased_area": { value: 8200 },
    "occupancy.signed_pending_area": { value: 300 },
    "occupancy.parking_rate": { value: 0.65 },
    "contract.expiring_90d": { value: 2 },
  },
};
const VACANT = [
  { space_id: "sp-1", space_name: "A101", space_type: "shop", floor_no: "1F", area_sqm: 120, rental_status: "vacant" },
  { space_id: "sp-2", space_name: "B201", space_type: "booth", floor_no: "2F", area_sqm: 40, rental_status: "negotiating" },
];
const EXPIRING = [
  { contract_id: "c-1", merchant_name: "商户甲", expiry_date: "2026-08-01", days_left: 27, bound_area: 120 },
];

function dataEnvelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 200, total: items.length });
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/dashboards/leasing")) return Promise.resolve(dataEnvelope(DASHBOARD));
      if (url.includes("/occupancy/vacant-spaces")) return Promise.resolve(listEnvelope(VACANT));
      if (url.includes("/occupancy/expiring-contracts")) return Promise.resolve(listEnvelope(EXPIRING));
      return Promise.resolve(listEnvelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<OccupancyAnalyticsPage {...fullProps} />);
}

describe("OccupancyAnalyticsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/analytics/occupancy");
  });

  it("点“空置面积”→ URL 带 ?kpi=vacant，聚焦只剩空置铺位表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("occupancy-kpi-vacant")).toBeTruthy());
    fireEvent.click(screen.getByTestId("occupancy-kpi-vacant"));
    expect(window.location.search).toContain("kpi=vacant");
    expect(screen.getByTestId("occupancy-kpi-focus-banner").textContent).toContain("正在查看：空置铺位明细（2）");
    expect(screen.getByTestId("occupancy-vacant-table")).toBeTruthy();
    expect(screen.queryByTestId("occupancy-expiring-table")).toBeNull();
    expect(screen.queryByTestId("occupancy-summary")).toBeNull();
  });

  it("点“90 天内到期”→ 聚焦只剩到期预警表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("occupancy-kpi-expiring")).toBeTruthy());
    fireEvent.click(screen.getByTestId("occupancy-kpi-expiring"));
    expect(window.location.search).toContain("kpi=expiring");
    expect(screen.getByTestId("occupancy-kpi-focus-banner").textContent).toContain("正在查看：到期预警（1）");
    expect(screen.getByTestId("occupancy-expiring-table")).toBeTruthy();
    expect(screen.queryByTestId("occupancy-vacant-table")).toBeNull();
  });

  it("聚焦点「返回全部」→ 参数清除，恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("occupancy-kpi-vacant")).toBeTruthy());
    fireEvent.click(screen.getByTestId("occupancy-kpi-vacant"));
    fireEvent.click(screen.getByTestId("occupancy-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("occupancy-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("occupancy-summary")).toBeTruthy();
    expect(screen.getByTestId("occupancy-vacant-table")).toBeTruthy();
    expect(screen.getByTestId("occupancy-expiring-table")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/analytics/occupancy?kpi=expiring");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("occupancy-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("occupancy-expiring-table")).toBeTruthy();
    expect(screen.queryByTestId("occupancy-vacant-table")).toBeNull();
  });
});

describe("OccupancyAnalyticsPage 行明细抽屉（整行可点·日期中文·零英文枚举）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/analytics/occupancy");
  });

  it("点空置铺位行 → 抽屉含中文类型/状态+面积，无英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("occupancy-vacant-row-sp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("occupancy-vacant-row-sp-1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("A101");
    expect(text).toContain("商铺");
    expect(text).toContain("空置");
    expect(text).not.toContain("shop");
    expect(text).not.toContain("vacant");
  });

  it("点到期合同行 → 抽屉含中文到期日+合同 TraceLink，不直出 ISO", async () => {
    const navigateTo = vi.fn();
    const props: AdminPageProps = {
      api: makeApi(),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
      navigateTo,
    };
    render(<OccupancyAnalyticsPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("occupancy-expiring-row-c-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("occupancy-expiring-row-c-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("商户甲");
    expect(text).toContain("2026年8月1日");
    expect(text).not.toContain("2026-08-01");
    // 抽屉内合同引用为可点 TraceLink（navigateTo 存在时才渲染为链接）。
    expect(drawer.querySelector('[data-testid="trace-link-ADM-CONT-001-c-1"]')).toBeTruthy();
  });

  it("点到期合同 TraceLink → 下钻、不弹抽屉（stopPropagation）", async () => {
    const navigateTo = vi.fn();
    const props: AdminPageProps = {
      api: makeApi(),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
      navigateTo,
    };
    render(<OccupancyAnalyticsPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("trace-link-ADM-CONT-001-c-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("trace-link-ADM-CONT-001-c-1"));
    expect(navigateTo).toHaveBeenCalled();
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点空置铺位行 → Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("occupancy-vacant-row-sp-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("occupancy-vacant-row-sp-2"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
