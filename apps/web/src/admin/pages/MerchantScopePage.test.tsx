import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantScopePage } from "./MerchantScopePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const WORK_ORDERS = [
  { id: "wo-1", merchant_id: "m-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing", authorized_scope: "工单" },
  { id: "wo-2", merchant_id: "m-2", work_order_type: "repair", title: "工单乙", priority: "high", status: "processing", authorized_scope: "工单" },
];
const BILLS = [
  { id: "bl-1", merchant_id: "m-1", bill_no: "B-1", title: "账单甲", total_amount: 100, status: "unpaid", authorized_scope: "账单" },
  { id: "bl-2", merchant_id: "m-2", bill_no: "B-2", title: "账单乙", total_amount: 200, status: "unpaid", authorized_scope: "账单" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("work-orders")) return Promise.resolve(envelope(WORK_ORDERS));
      if (url.includes("bills")) return Promise.resolve(envelope(BILLS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(user: AdminPageProps["user"] = { id: "u1", username: "merchant", accountType: "merchant_admin", merchantId: "m-1" }) {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user,
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<MerchantScopePage {...fullProps} />);
}

describe("MerchantScopePage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/scope");
  });

  it("点“商户账单”数字 → ?kpi=bills，只剩账单表+横幅", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-bills")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-kpi-bills"));
    expect(window.location.search).toContain("kpi=bills");
    expect(screen.getByTestId("merchant-scope-kpi-focus-banner").textContent).toContain("正在查看：商户账单（1）");
    expect(screen.getByTestId("merchant-scope-bills-table")).toBeTruthy();
    expect(screen.queryByTestId("merchant-scope-work-orders-table")).toBeNull();
    expect(screen.queryByTestId("merchant-scope-summary")).toBeNull();
  });

  it("点“商户工单”数字 → 只剩工单表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-work-orders")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-kpi-work-orders"));
    expect(screen.getByTestId("merchant-scope-work-orders-table")).toBeTruthy();
    expect(screen.queryByTestId("merchant-scope-bills-table")).toBeNull();
  });

  it("「返回全部」→ 恢复两表+KPI 卡", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-bills")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-kpi-bills"));
    fireEvent.click(screen.getByTestId("merchant-scope-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-scope-work-orders-table")).toBeTruthy();
    expect(screen.getByTestId("merchant-scope-bills-table")).toBeTruthy();
    expect(screen.getByTestId("merchant-scope-summary")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/merchant/scope?kpi=workOrders");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("merchant-scope-work-orders-table")).toBeTruthy();
    expect(screen.queryByTestId("merchant-scope-bills-table")).toBeNull();
  });

  it("工单类型渲染为中文，不含英文枚举 repair", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-work-orders-table")).toBeTruthy());
    const text = screen.getByTestId("merchant-scope-work-orders-table").textContent ?? "";
    expect(text).toContain("报修");
    expect(text).not.toContain("repair");
  });

  it("本商户看不见其他商户的工单和账单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-work-order-row-wo-1")).toBeTruthy());
    expect(screen.queryByTestId("merchant-scope-work-order-row-wo-2")).toBeNull();
    expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy();
    expect(screen.queryByTestId("merchant-scope-bill-row-bl-2")).toBeNull();
  });

  it("平台管理员无商户范围时不看全量", async () => {
    renderPage({ id: "u-admin", username: "admin", accountType: "platform_admin" });
    await waitFor(() => expect(screen.getByTestId("merchant-scope-page")).toBeTruthy());
    expect(screen.queryByTestId("merchant-scope-work-order-row-wo-1")).toBeNull();
    expect(screen.queryByTestId("merchant-scope-bill-row-bl-1")).toBeNull();
  });
});

describe("MerchantScopePage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant/scope");
  });

  it("点工单行 → 抽屉含该行类型（中文）与状态，无英文枚举，Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-work-order-row-wo-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-work-order-row-wo-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("报修");
    expect(drawer.textContent).toContain("处理中");
    expect(drawer.textContent).not.toContain("repair");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点账单行 → 抽屉含该行账单号", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-scope-bill-row-bl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-scope-bill-row-bl-1"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("B-1");
  });
});
