import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VendorWorkOrdersPage } from "./VendorWorkOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const WORK_ORDERS = [
  { id: "wo-1", code: "WO-1", work_order_type: "repair", priority: "high", status: "pending", title: "空调报修", assignee_id: "u-9", project_id: "p-1" },
  { id: "wo-2", code: "WO-2", work_order_type: "cleaning", priority: "low", status: "completed", title: "保洁完成", assignee_id: "u-9", project_id: "p-1" },
];
const EQUIPMENT = [{ id: "eq-1", name: "中央空调", equipment_type: "facility", status: "normal", project_id: "p-1" }];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/equipment")) return Promise.resolve(envelope(EQUIPMENT));
    if (url.includes("/work-orders")) return Promise.resolve(envelope(WORK_ORDERS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "vendor", accountType: "outsourced_vendor" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VendorWorkOrdersPage {...props} />);
}

describe("VendorWorkOrdersPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/work-orders");
  });

  it("点“待处理” → URL 带 ?kpi=work-orders&subset=pending，聚焦视图只剩工单表且只含未完工行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-work-orders-kpi-pending")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-work-orders-kpi-pending"));
    expect(window.location.search).toContain("kpi=work-orders");
    expect(window.location.search).toContain("subset=pending");
    expect(screen.getByTestId("vendor-work-orders-kpi-focus-banner").textContent).toContain("正在查看：待处理工单（1）");
    expect(screen.getByTestId("vendor-work-order-select-wo-1")).toBeTruthy();
    expect(screen.queryByTestId("vendor-work-order-select-wo-2")).toBeNull();
    expect(screen.queryByTestId("vendor-work-orders-summary")).toBeNull();
    expect(screen.queryByTestId("vendor-work-order-equipment-table")).toBeNull();
  });

  it("聚焦后点「返回全部」→ 参数清除，恢复 KPI 卡+设备表+全量工单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-work-orders-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-work-orders-kpi-total"));
    fireEvent.click(screen.getByTestId("vendor-work-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("vendor-work-orders-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("vendor-work-orders-summary")).toBeTruthy();
    expect(screen.getByTestId("vendor-work-order-select-wo-2")).toBeTruthy();
  });

  it("KPI 子标签与状态列为中文，不含英文枚举/字段名", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-work-orders-summary")).toBeTruthy());
    const summary = screen.getByTestId("vendor-work-orders-summary").textContent ?? "";
    expect(summary).not.toContain("assignee");
    expect(summary).not.toContain("status");
    const table = screen.getByTestId("vendor-work-orders-table").textContent ?? "";
    expect(table).toContain("报修");
    expect(table).toContain("已完成");
    expect(table).not.toContain("repair");
    expect(table).not.toContain("completed");
  });
});
