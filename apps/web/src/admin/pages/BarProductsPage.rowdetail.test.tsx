import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarProductsPage } from "./BarProductsPage";
import type { AdminApiClient, AdminPageProps, BarProductRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: BarProductRow[] = [
  { id: "prod-1", barId: "bar-main", productName: "威士忌", productType: "spirit", priceCent: 8800, internalSettlementPriceCent: 5000, status: "active", soldOut: false },
  { id: "prod-2", barId: "bar-main", productName: "生啤", productType: "beer", priceCent: 3000, internalSettlementPriceCent: null, status: "active", soldOut: true },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(ROWS),
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
  render(<BarProductsPage {...props} />);
}

describe("BarProductsPage 行明细抽屉 + 零英文", () => {
  it("点商品行 → 抽屉含类别/售价/内部结算价（中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-product-row-prod-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-product-row-prod-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("烈酒");
    expect(drawer.textContent).toContain("¥88.00");
    expect(drawer.textContent).toContain("¥50.00");
    expect(drawer.textContent).toContain("有效");
    // 零英文断言：类别码/状态码不得原样漏出
    expect(drawer.textContent).not.toContain("spirit");
    expect(drawer.textContent).not.toContain("active");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("内部结算价未录入 → 抽屉显示“未录入”", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-product-row-prod-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-product-row-prod-2"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("未录入");
    expect(drawer.textContent).toContain("沽清");
  });

  it("表格类别/状态列渲染为中文（无 spirit/active/beer 漏出）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-products-table")).toBeTruthy());
    const table = screen.getByTestId("bar-products-table").textContent ?? "";
    expect(table).toContain("烈酒");
    expect(table).toContain("啤酒");
    expect(table).not.toContain("spirit");
    expect(table).not.toContain("beer");
    expect(table).not.toContain("active");
  });
});
