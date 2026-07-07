import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { InventoryLedgerPage } from "./InventoryLedgerPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式：登记库存移动收起为入口按钮 → 两步引导（位置 / 数量与追踪）→ 确认页提交。
const ITEMS = [{ id: "i-1", itemCode: "SKU-1", itemName: "威士忌", category: "spirits", unit: "瓶", status: "active" }];

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((path: string) => {
      if (path.includes("/inventory/items")) return Promise.resolve(ITEMS);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<InventoryLedgerPage {...props} />);
}

describe("InventoryLedgerPage 引导式登记库存移动", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/inventory/ledger");
  });

  it("默认只留入口按钮，不展示弹窗", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("inventory-movement-open")).toBeTruthy());
    expect(screen.queryByTestId("inventory-movement-guided")).toBeNull();
  });

  it("入口→两步→确认提交：POST /stock-movements 带选中物料，成功后关弹窗", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("inventory-movement-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-movement-open"));

    await waitFor(() => expect(screen.getByTestId("inventory-movement-from")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-movement-guided-next"));

    await waitFor(() => expect(screen.getByTestId("inventory-movement-quantity")).toBeTruthy());
    fireEvent.change(screen.getByTestId("inventory-movement-quantity"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("inventory-movement-guided-next"));

    await waitFor(() => expect(screen.getByTestId("inventory-movement-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-movement-submit"));

    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    const call = post.mock.calls[0]!;
    expect(String(call[0])).toContain("/inventory/stock-movements");
    expect(call[1]).toEqual(expect.objectContaining({
      items: [expect.objectContaining({ item_id: "i-1", quantity: 2 })],
    }));
    await waitFor(() => expect(screen.queryByTestId("inventory-movement-guided")).toBeNull());
  });

  it("第二步校验拦截：数量为 0 不到确认页、不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("inventory-movement-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-movement-open"));
    fireEvent.click(screen.getByTestId("inventory-movement-guided-next"));
    await waitFor(() => expect(screen.getByTestId("inventory-movement-quantity")).toBeTruthy());
    fireEvent.change(screen.getByTestId("inventory-movement-quantity"), { target: { value: "0" } });
    fireEvent.click(screen.getByTestId("inventory-movement-guided-next"));
    expect(screen.getByTestId("inventory-movement-guided-error").textContent).toBeTruthy();
    expect(screen.queryByTestId("inventory-movement-guided-summary")).toBeNull();
    expect(post).not.toHaveBeenCalled();
  });
});
