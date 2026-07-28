import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarCommissionsPage } from "./BarCommissionsPage";
import type { AdminApiClient, AdminPageProps, BarOrderRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ORDERS: BarOrderRow[] = [
  { id: "ord-1", tableId: "T-1", salesStaffId: "staff_sales_1", totalAmount: 20000, status: "paid", createdAt: "2026-07-05T13:20:00Z", bills: [{ id: "bill-1", status: "settled", paidAt: "2026-07-05T14:00:00Z", salesRoleType: "sales", commissionRateBp: 2000, commissionAmountCent: 4000 }] },
  { id: "ord-2", tableId: "T-2", totalAmount: 10000, status: "ordered", createdAt: "2026-07-05T10:00:00Z" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(ORDERS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(): void {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BarCommissionsPage {...props} />);
}

describe("BarCommissionsPage 行明细抽屉 + 零英文", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/bar/commissions");
  });

  it("点提成行 → 抽屉含金额/比例/状态（中文）与中文时间，Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-commission-row-ord-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-commission-row-ord-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("¥200.00");
    // 计提比例读台账真实落库值（本例 2000bp=20%），不是页面写死的 15%。
    expect(drawer.textContent).toContain("20%");
    expect(drawer.textContent).toContain("已结清");
    expect(drawer.textContent).toContain("2026年7月5日");
    // 零英文：状态码不得漏出
    expect(drawer.textContent).not.toContain("settled");
    expect(drawer.textContent).not.toContain("2026-07-05T");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点行内单选按钮不打开抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-commission-select-ord-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-commission-select-ord-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("表格时间列渲染中文（无 ISO 时间戳漏出）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-commissions-table")).toBeTruthy());
    const table = screen.getByTestId("bar-commissions-table").textContent ?? "";
    expect(table).toContain("2026年7月5日");
    expect(table).not.toContain("2026-07-05T");
  });
});
