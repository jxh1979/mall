import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantWorkOrdersPage } from "./MerchantWorkOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 3 行：wo-1 处理中/高、wo-2 已关闭/低、wo-3 处理中/紧急 → 总数3、待处理(未终结)2、高优先级(high/critical)2。
const ROWS = [
  { id: "wo-1", merchant_id: "m-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing" },
  { id: "wo-2", merchant_id: "m-1", work_order_type: "service", title: "工单乙", priority: "low", status: "closed" },
  { id: "wo-3", merchant_id: "m-1", work_order_type: "complaint", title: "工单丙", priority: "critical", status: "processing" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(envelope(ROWS))));
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
  render(<MerchantWorkOrdersPage {...fullProps} />);
}

describe("MerchantWorkOrdersPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/work-orders");
  });

  it("点“高优先级”数字 → ?kpi=high，聚焦横幅+表只剩高优先级子集(2)，KPI 卡/创建表单隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-high")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-high"));
    expect(window.location.search).toContain("kpi=high");
    expect(screen.getByTestId("merchant-work-orders-kpi-focus-banner").textContent).toContain("正在查看：高优先级工单（2）");
    expect(screen.queryByTestId("merchant-work-orders-summary")).toBeNull();
    expect(screen.queryByTestId("merchant-work-order-create-form")).toBeNull();
    // 表只含高优先级/紧急两行，不含低优先级 wo-2。
    const text = screen.getByTestId("merchant-work-orders-table").textContent ?? "";
    expect(text).toContain("工单甲");
    expect(text).toContain("工单丙");
    expect(text).not.toContain("工单乙");
  });

  it("点“待处理”数字 → 未终结子集(2)", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-pending")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-pending"));
    expect(screen.getByTestId("merchant-work-orders-kpi-focus-banner").textContent).toContain("（2）");
    const text = screen.getByTestId("merchant-work-orders-table").textContent ?? "";
    expect(text).not.toContain("工单乙");
  });

  it("「返回全部」→ 恢复 KPI 卡+创建表单+全量表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-high")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-high"));
    fireEvent.click(screen.getByTestId("merchant-work-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-work-orders-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-work-order-create-form")).toBeTruthy();
    expect(screen.getByTestId("merchant-work-orders-table").textContent).toContain("工单乙");
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/merchant/work-orders?kpi=all");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-work-orders-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("merchant-work-orders-summary")).toBeNull();
    expect(screen.getByTestId("merchant-work-orders-table")).toBeTruthy();
  });
});
