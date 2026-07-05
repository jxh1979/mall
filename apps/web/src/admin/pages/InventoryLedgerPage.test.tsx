import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { InventoryLedgerPage } from "./InventoryLedgerPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ITEMS = [
  { id: "i-1", itemCode: "SKU-1", itemName: "威士忌", category: "spirits", unit: "瓶", status: "active" },
];
const BALANCES = [
  { id: "sb-1", itemId: "i-1", locationType: "warehouse", quantity: 12, status: "normal" },
];
const MOVEMENTS = [
  { id: "sm-1", itemId: "i-1", quantity: 3, status: "processed", createdAt: "2026-07-01T10:00:00Z" },
];

function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/inventory/items")) return Promise.resolve(ITEMS);
    if (path.includes("/inventory/stock-balances")) return Promise.resolve(BALANCES);
    if (path.includes("/inventory/stock-movements")) return Promise.resolve(MOVEMENTS);
    return Promise.resolve([]);
  });
}

function makeApi(get = getResolver()): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient = makeApi()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<InventoryLedgerPage {...props} />);
}

describe("InventoryLedgerPage KPI 聚焦视图（带参重进本页）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/inventory/ledger");
  });

  it("点“余额”数字 → URL 带 ?kpi=balances，只剩余额台账，物料/流水表与表单隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-kpi-balances")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-kpi-balances"));
    expect(window.location.search).toContain("kpi=balances");
    expect(screen.getByTestId("inventory-kpi-focus-banner").textContent).toContain("正在查看：余额（1）");
    expect(screen.getByTestId("inventory-ledger-balances-table")).toBeTruthy();
    expect(screen.queryByTestId("inventory-ledger-items-table")).toBeNull();
    expect(screen.queryByTestId("inventory-ledger-movements-table")).toBeNull();
    expect(screen.queryByTestId("inventory-movement-form")).toBeNull();
    expect(screen.queryByTestId("inventory-ledger-summary")).toBeNull();
  });

  it("点“物料”数字 → 聚焦物料承载表并保留登记表单（登记依赖选中物料）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-kpi-items")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-kpi-items"));
    expect(window.location.search).toContain("kpi=items");
    expect(screen.getByTestId("inventory-ledger-items-table")).toBeTruthy();
    expect(screen.getByTestId("inventory-movement-form")).toBeTruthy();
    expect(screen.queryByTestId("inventory-ledger-balances-table")).toBeNull();
  });

  it("聚焦视图点「返回全部」→ 参数清除，三表+KPI 卡恢复", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-kpi-movements")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-kpi-movements"));
    fireEvent.click(screen.getByTestId("inventory-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("inventory-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("inventory-ledger-summary")).toBeTruthy();
    expect(screen.getByTestId("inventory-ledger-items-table")).toBeTruthy();
    expect(screen.getByTestId("inventory-ledger-balances-table")).toBeTruthy();
    expect(screen.getByTestId("inventory-ledger-movements-table")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图（参数从 URL 重建）", async () => {
    window.history.replaceState({}, "", "/admin/inventory/ledger?kpi=movements");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("inventory-ledger-movements-table")).toBeTruthy();
    expect(screen.queryByTestId("inventory-ledger-items-table")).toBeNull();
  });

  it("浏览器后退（popstate）→ 退出聚焦视图回完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-kpi-balances")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-kpi-balances"));
    expect(screen.getByTestId("inventory-kpi-focus-banner")).toBeTruthy();
    window.history.replaceState({}, "", "/admin/inventory/ledger");
    fireEvent.popState(window);
    await waitFor(() => expect(screen.queryByTestId("inventory-kpi-focus-banner")).toBeNull());
    expect(screen.getByTestId("inventory-ledger-summary")).toBeTruthy();
  });

  it("流水表状态列渲染为中文枚举，不直出英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-ledger-movements-table")).toBeTruthy());
    const movements = screen.getByTestId("inventory-ledger-movements-table").textContent ?? "";
    expect(movements).toContain("已处理");
    expect(movements).not.toContain("processed");
  });
});
