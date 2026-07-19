import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps, BarOrderRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ORDERS: BarOrderRow[] = [
  { id: "ord-1", tableId: "T-1", salesStaffId: "staff_sales_1", totalAmount: 20000, status: "paid", createdAt: "2026-07-05T13:20:00Z", bills: [{ id: "bill-1", status: "settled", paidAt: "2026-07-05T14:05:00Z" }] },
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

function renderPage(extra: Partial<AdminPageProps> = {}): void {
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

describe("BarOrdersPage 行明细抽屉 + 零英文", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/bar/orders");
  });

  it("点订单行 → 抽屉含金额/状态（中文）+ 下单/支付时间（中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-order-row-ord-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-order-row-ord-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("¥200.00");
    expect(drawer.textContent).toContain("已支付");
    // 下单时间 + 支付时间都在抽屉里，且为中文
    expect(drawer.textContent).toContain("2026年7月5日 13:20");
    expect(drawer.textContent).toContain("2026年7月5日 14:05");
    expect(drawer.textContent).not.toContain("2026-07-05T");
    expect(drawer.textContent).not.toContain("paid");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点行内单选按钮不打开抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-order-select-ord-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-order-select-ord-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("支付时间列(V231)仍存在且为中文（不破坏既有列）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-order-paid-at-ord-1")).toBeTruthy());
    expect(screen.getByTestId("bar-order-paid-at-ord-1").textContent).toContain("2026年7月5日");
  });
});
