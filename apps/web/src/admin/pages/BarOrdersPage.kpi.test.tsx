import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps, BarOrderRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ORDERS: BarOrderRow[] = [
  { id: "b-1", orderNo: "BAR-0001", tableId: "T-1", status: "billed", totalAmount: 8800 },
  { id: "b-2", orderNo: "BAR-0002", tableId: "T-2", status: "paid", totalAmount: 12000 },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async () => ORDERS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(extra: Partial<AdminPageProps> = {}) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
  render(<BarOrdersPage {...props} />);
}

describe("BarOrdersPage KPI 聚焦视图", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/bar/orders");
  });

  it("点“未完结”KPI → ?kpi=open，表内只剩未完结订单（已支付隐藏），KPI 卡与操作隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-orders-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-orders-kpi-open"));
    expect(window.location.search).toContain("kpi=open");
    expect(screen.getByTestId("bar-orders-kpi-focus-banner").textContent).toContain("未完结（1）");
    const table = screen.getByTestId("bar-orders-table").textContent ?? "";
    expect(table).toContain("BAR-0001");
    expect(table).not.toContain("BAR-0002");
    expect(screen.queryByTestId("bar-orders-summary")).toBeNull();
    expect(screen.queryByTestId("bar-order-actions")).toBeNull();
  });

  it("聚焦点「返回全部」→ 恢复完整页面（KPI 卡 + 两条订单）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-orders-kpi-orders")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-orders-kpi-orders"));
    fireEvent.click(screen.getByTestId("bar-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("bar-orders-summary")).toBeTruthy();
    const table = screen.getByTestId("bar-orders-table").textContent ?? "";
    expect(table).toContain("BAR-0001");
    expect(table).toContain("BAR-0002");
  });

  it("带参直链 ?kpi=open → 直接进聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/bar/orders?kpi=open");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-orders-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("bar-orders-kpi-focus-banner").textContent).toContain("未完结（1）");
  });

  it("KPI 子标签均为中文，无英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-orders-summary")).toBeTruthy());
    const text = screen.getByTestId("bar-orders-summary").textContent ?? "";
    expect(text).toContain("当前列表记录");
    expect(text).toContain("未支付或未取消");
    expect(text).not.toContain("open");
    expect(text).not.toContain("orders");
  });

  it("进入 KPI 聚焦 → 窗口滚动复位到顶(断头修复)", async () => {
    const scrollSpy = vi.spyOn(window, "scrollTo").mockImplementation(() => {});
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-orders-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-orders-kpi-open"));
    expect(scrollSpy).toHaveBeenCalledWith({ top: 0 });
    scrollSpy.mockRestore();
  });

  it("退出 KPI 聚焦(返回全部) → 窗口滚动复位到顶", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-orders-kpi-orders")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-orders-kpi-orders"));
    const scrollSpy = vi.spyOn(window, "scrollTo").mockImplementation(() => {});
    fireEvent.click(screen.getByTestId("bar-orders-kpi-focus-clear"));
    expect(scrollSpy).toHaveBeenCalledWith({ top: 0 });
    scrollSpy.mockRestore();
  });

});
