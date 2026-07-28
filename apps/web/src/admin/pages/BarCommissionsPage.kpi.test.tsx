import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarCommissionsPage } from "./BarCommissionsPage";
import type { AdminApiClient, AdminPageProps, BarOrderRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ORDERS: BarOrderRow[] = [
  // G/V349：销售/非销售档一律看账单上快照的 salesRoleType（员工岗位判定的结果），不看有没有 salesStaffId。
  { id: "b-1", tableId: "T-1", status: "billed", totalAmount: 8800, salesStaffId: "staff_sales_1", bills: [{ id: "bill-1", status: "settled", salesRoleType: "sales", commissionRateBp: 2000, commissionAmountCent: 1760 }] },
  { id: "b-2", tableId: "T-2", status: "billed", totalAmount: 6000, bills: [{ id: "bill-2", status: "settled", salesRoleType: "non_sales", commissionRateBp: 1000, commissionAmountCent: 600 }] },
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

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BarCommissionsPage {...props} />);
}

describe("BarCommissionsPage KPI 聚焦视图 + 英文全清", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/bar/commissions");
  });

  it("点“销售人员”KPI → ?kpi=sales，表内只剩销售岗归属订单，KPI 卡与操作隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-commissions-kpi-sales")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-commissions-kpi-sales"));
    expect(window.location.search).toContain("kpi=sales");
    expect(screen.getByTestId("bar-commissions-kpi-focus-banner").textContent).toContain("销售人员（1）");
    const table = screen.getByTestId("bar-commissions-table").textContent ?? "";
    expect(table).toContain("b-1");
    expect(table).not.toContain("b-2");
    expect(screen.queryByTestId("bar-commissions-summary")).toBeNull();
  });

  it("聚焦点「返回全部」→ 恢复完整页面（KPI 卡 + 两条订单）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-commissions-kpi-non-sales")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-commissions-kpi-non-sales"));
    fireEvent.click(screen.getByTestId("bar-commissions-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("bar-commissions-summary")).toBeTruthy();
    const table = screen.getByTestId("bar-commissions-table").textContent ?? "";
    expect(table).toContain("b-1");
    expect(table).toContain("b-2");
  });

  it("带参直链 ?kpi=non_sales → 直接进聚焦视图（只剩非销售订单）", async () => {
    window.history.replaceState({}, "", "/admin/bar/commissions?kpi=non_sales");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-commissions-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("bar-commissions-kpi-focus-banner").textContent).toContain("非销售人员（1）");
    const table = screen.getByTestId("bar-commissions-table").textContent ?? "";
    expect(table).toContain("b-2");
    expect(table).not.toContain("b-1");
  });

  it("“不含通票”KPI 子标签为业务中文，不含 includeTicketAmount=false 技术标签", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-commissions-summary")).toBeTruthy());
    const text = screen.getByTestId("bar-commissions-summary").textContent ?? "";
    expect(text).not.toContain("includeTicketAmount");
    expect(text).toContain("提成不计通票金额");
  });
});
