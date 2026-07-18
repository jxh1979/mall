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
  // V296：带成本列的流水（移动加权定稿）。
  { id: "sm-2", itemId: "i-1", quantity: 3, status: "processed", createdAt: "2026-07-02T10:00:00Z", unitCostCent: 500, totalCostCent: 1500 },
];
// V296 本月出库成本汇总。
const COST_SUMMARY = { outboundTotalCostCent: 12345, outboundMovementCount: 3, noCostBasisCount: 1 };

function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/inventory/items")) return Promise.resolve(ITEMS);
    if (path.includes("/inventory/stock-balances")) return Promise.resolve(BALANCES);
    if (path.includes("/inventory/stock-movements")) return Promise.resolve(MOVEMENTS);
    if (path.includes("/inventory/cost-summary")) return Promise.resolve(COST_SUMMARY);
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

describe("InventoryLedgerPage 行明细抽屉（整行可点·时间中文）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/inventory/ledger");
  });

  it("点物料行 -> 抽屉含名称+编码+中文状态，无英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-item-row-i-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-item-row-i-1"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("威士忌");
    expect(drawer).toContain("SKU-1");
    expect(drawer).toContain("有效");
    expect(drawer).not.toContain("active");
  });

  it("点流水行 -> 抽屉时间为中文（非 ISO），状态中文；Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-movement-row-sm-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-movement-row-sm-1"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("2026年7月1日");
    expect(drawer).toContain("已处理");
    expect(drawer).not.toContain("2026-07-01T");
    expect(drawer).not.toContain("processed");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("物料行点单选框仅选中、不弹抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-item-row-i-1")).toBeTruthy());
    fireEvent.click(screen.getByLabelText("选择物料 SKU-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

describe("InventoryLedgerPage 成本台账（V296 移动加权）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/inventory/ledger");
  });

  it("流水表成本列：有成本显示金额，无成本显示“无成本基准”（不编造数字）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-ledger-movements-table")).toBeTruthy());
    const movements = screen.getByTestId("inventory-ledger-movements-table").textContent ?? "";
    expect(movements).toContain("¥15.00");
    expect(movements).toContain("无成本基准");
  });

  it("本月出库成本汇总小卡：合计金额 + 出库笔数 + 无成本基准笔数", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-cost-summary-card")).toBeTruthy());
    expect(screen.getByTestId("inventory-cost-summary-total").textContent).toBe("¥123.45");
    const card = screen.getByTestId("inventory-cost-summary-card").textContent ?? "";
    expect(card).toContain("出库 3 笔");
    expect(card).toContain("无成本基准 1 笔");
  });

  it("汇总接口失败降级为“暂无数据”，台账主链不受影响", async () => {
    const failing = vi.fn().mockImplementation((path: string) => {
      if (path.includes("/inventory/items")) return Promise.resolve(ITEMS);
      if (path.includes("/inventory/stock-balances")) return Promise.resolve(BALANCES);
      if (path.includes("/inventory/stock-movements")) return Promise.resolve(MOVEMENTS);
      if (path.includes("/inventory/cost-summary")) return Promise.reject(new Error("boom"));
      return Promise.resolve([]);
    });
    renderPage(makeApi(failing));
    await waitFor(() => expect(screen.getByTestId("inventory-cost-summary-card")).toBeTruthy());
    expect(screen.getByTestId("inventory-cost-summary-total").textContent).toBe("—");
    expect(screen.getByTestId("inventory-ledger-movements-table")).toBeTruthy();
  });

  it("流水行抽屉包含单价与成本字段", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-movement-row-sm-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inventory-movement-row-sm-2"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("¥5.00");
    expect(drawer).toContain("¥15.00");
  });
});
