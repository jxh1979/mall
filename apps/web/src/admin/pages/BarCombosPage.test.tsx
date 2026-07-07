import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarCombosPage } from "./BarCombosPage";
import type { AdminApiClient, AdminPageProps, BarComboRow, BarProductRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const PRODUCTS: BarProductRow[] = [
  { id: "prod-1", barId: "bar-main", productName: "威士忌", priceCent: 8800, status: "active" },
  { id: "prod-2", barId: "bar-main", productName: "苏打水", priceCent: 1000, status: "active" },
];

const COMBOS: BarComboRow[] = [
  {
    id: "combo-1", barId: "bar-main", comboName: "威士忌套餐", priceCent: 9900, internalSettlementPriceCent: 6000,
    status: "active", items: [{ id: "ci-1", productId: "prod-1", productName: "威士忌", quantity: 1 }],
  },
];

function makeApi(post = vi.fn().mockResolvedValue({ id: "combo-2" })): AdminApiClient {
  const get = vi.fn().mockImplementation((path: string) =>
    Promise.resolve(path.includes("/combos") ? COMBOS : PRODUCTS));
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient): void {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BarCombosPage {...props} />);
}

describe("BarCombosPage 套餐管理", () => {
  it("列出套餐 + 含商品中文展示", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("bar-combo-row-combo-1")).toBeTruthy());
    const table = screen.getByTestId("bar-combos-table").textContent ?? "";
    expect(table).toContain("威士忌套餐");
    expect(table).toContain("99.00");
    expect(table).toContain("威士忌 ×1");
  });

  it("从 SKU 选组合 + 填价 → 提交携带 items", async () => {
    const post = vi.fn().mockResolvedValue({ id: "combo-2" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-combo-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-combo-create-open"));
    // 步骤1 套餐信息
    fireEvent.change(screen.getByTestId("bar-combo-bar-id"), { target: { value: "bar-main" } });
    fireEvent.change(screen.getByTestId("bar-combo-name"), { target: { value: "组合A" } });
    fireEvent.click(screen.getByTestId("bar-combo-guided-next"));
    // 步骤2 定价与商品
    fireEvent.change(screen.getByTestId("bar-combo-price"), { target: { value: "88" } });
    fireEvent.change(screen.getByTestId("bar-combo-item-product-0"), { target: { value: "prod-1" } });
    fireEvent.change(screen.getByTestId("bar-combo-item-qty-0"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("bar-combo-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("bar-combo-submit"));
    await waitFor(() => expect(post).toHaveBeenCalled());
    const call = post.mock.calls[0] ?? [];
    const path = call[0];
    const body = call[1];
    expect(path).toBe("/api/v1/bar/catalog/combos");
    expect(body).toMatchObject({
      barId: "bar-main",
      comboName: "组合A",
      priceCent: 8800,
      items: [{ productId: "prod-1", quantity: 2 }],
    });
  });

  it("无组合项 → 拦截提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("bar-combo-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-combo-create-open"));
    fireEvent.change(screen.getByTestId("bar-combo-bar-id"), { target: { value: "bar-main" } });
    fireEvent.change(screen.getByTestId("bar-combo-name"), { target: { value: "空套餐" } });
    fireEvent.click(screen.getByTestId("bar-combo-guided-next"));
    // 步骤2 只填价、不选商品 → 点下一步被步内校验拦截
    fireEvent.change(screen.getByTestId("bar-combo-price"), { target: { value: "10" } });
    fireEvent.click(screen.getByTestId("bar-combo-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-combo-guided-error").textContent).toContain("至少包含一个商品"));
    expect(post).not.toHaveBeenCalled();
  });
});
