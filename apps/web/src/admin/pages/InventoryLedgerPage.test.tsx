// 文件版本：V0.02
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
  it("普通项目员工旧台账读取被拒后仍能独立查看授权隔离仓库，只有查看权不能处置", async () => {
    const api = makeApi(vi.fn(async (path: string) => {
      if (path === "/api/v1/inventory/quarantine/warehouses") return [{ id: "project-warehouse", name: "项目中心仓" }];
      if (path.includes("/quarantine?")) return [{ id: "isolated-lot", itemId: "damaged-goods", quantity: 2, remainingQuantity: 2, receivedAt: "2026-10-18T10:00:00Z", sourceId: "difference" }];
      if (path.includes("/quarantine/isolated-lot/dispositions")) return [];
      throw new Error("旧台账需要商户范围");
    }));
    render(<InventoryLedgerPage api={api} user={{ id: "project-staff", username: "项目查看人员", accountType: "staff" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={{ status: "ready", value: { grants: ["inventory.stock.view"], denies: [], wildcard: false } }} />);
    await screen.findByText("旧台账需要商户范围");
    fireEvent.click(screen.getByRole("button", { name: "隔离库存与后续处置" }));
    fireEvent.click(await screen.findByRole("button", { name: "查看处置" }));
    expect(await screen.findByText("damaged-goods 的隔离处置")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "新建隔离处置方案" })).toBeNull();
    expect(api.post).not.toHaveBeenCalled();
    expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/quarantine/warehouses");
  });

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

// 第十八批第 24 条 24-R5（ENUM:164「或由库房主管以上岗位手工标记为高价值，标记与取消标记都必须填写理由并留痕，一线库管无此权限」）。
describe("InventoryLedgerPage 手工标记高价值（第十八批第 24 条）", () => {
  it("点「标记高价值」理由为空不发请求；填理由提交 POST {idempotency_key, manual_high_value: true, reason}", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("inventory-item-high-value-mark-i-1")?.textContent ?? "").toContain("标记高价值"));

    fireEvent.click(screen.getByTestId("inventory-item-high-value-mark-i-1"));
    fireEvent.click(screen.getByTestId("inventory-high-value-mark-submit"));
    expect(screen.getByTestId("inventory-high-value-mark-error").textContent).toContain("请填写理由");
    expect(api.post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("inventory-high-value-mark-reason"), { target: { value: "进口名酒按高价值管" } });
    fireEvent.click(screen.getByTestId("inventory-high-value-mark-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/items/i-1/high-value-mark",
      expect.objectContaining({ manual_high_value: true, reason: "进口名酒按高价值管", idempotency_key: expect.any(String) })));
  });

  it("已标记的物料给「取消标记」并提交 manual_high_value: false；不持库房主管复核码的账号按钮不可用", async () => {
    const marked = vi.fn().mockImplementation((path: string) => {
      if (path.includes("/inventory/items")) return Promise.resolve([{ ...ITEMS[0], manualHighValue: true }]);
      return getResolver()(path);
    });
    const api = makeApi(marked);
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("inventory-item-high-value-mark-i-1")?.textContent ?? "").toContain("取消标记"));
    fireEvent.click(screen.getByTestId("inventory-item-high-value-mark-i-1"));
    fireEvent.change(screen.getByTestId("inventory-high-value-mark-reason"), { target: { value: "已降为普通酒水" } });
    fireEvent.click(screen.getByTestId("inventory-high-value-mark-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/items/i-1/high-value-mark",
      expect.objectContaining({ manual_high_value: false, reason: "已降为普通酒水" })));
  });

  it("只持库存移动权限的一线库管看得到标记状态，但「标记高价值」按钮不可用", async () => {
    render(<InventoryLedgerPage api={makeApi()} user={{ id: "u2", username: "库管", accountType: "staff" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()}
      effectivePermissions={{ status: "ready", value: { grants: ["inventory.stock.move"], denies: [], wildcard: false } }} />);
    await waitFor(() => expect(screen.queryByTestId("inventory-item-high-value-mark-i-1")).not.toBeNull());
    expect((screen.getByTestId("inventory-item-high-value-mark-i-1") as HTMLButtonElement).disabled).toBe(true);
  });
});

// 第十八批第 33 条 33-R4：已有物料的安全库存在物料表修改（权限与新建物料同码 inventory.stock.move），采购建议量按它计算。
describe("InventoryLedgerPage 修改物料安全库存（第十八批第 33 条）", () => {
  const withSafetyStock = () => vi.fn().mockImplementation((path: string) => {
    if (path.includes("/inventory/items")) return Promise.resolve([{ ...ITEMS[0], minQuantity: 8 }]);
    return getResolver()(path);
  });

  it("物料表显示安全库存；改为负数不发请求，改为 20 提交 POST {idempotency_key, min_quantity: 20}", async () => {
    const api = makeApi(withSafetyStock());
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("inventory-item-safety-stock-i-1")).not.toBeNull());
    expect(screen.getByTestId("inventory-item-min-quantity-i-1").textContent).toBe("8");
    fireEvent.click(screen.getByTestId("inventory-item-safety-stock-i-1"));
    expect((screen.getByTestId("inventory-safety-stock-input") as HTMLInputElement).value).toBe("8");
    fireEvent.change(screen.getByTestId("inventory-safety-stock-input"), { target: { value: "-1" } });
    fireEvent.click(screen.getByTestId("inventory-safety-stock-submit"));
    expect(screen.getByTestId("inventory-safety-stock-error").textContent).toContain("安全库存须为不小于 0 的整数");
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("inventory-safety-stock-input"), { target: { value: "20" } });
    fireEvent.click(screen.getByTestId("inventory-safety-stock-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/items/i-1/safety-stock",
      expect.objectContaining({ min_quantity: 20, idempotency_key: expect.any(String) })));
  });

  it("不持库存移动权限（与新建物料同码）的账号看得到安全库存，但修改按钮不可用", async () => {
    render(<InventoryLedgerPage api={makeApi(withSafetyStock())} user={{ id: "u3", username: "查看", accountType: "staff" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()}
      effectivePermissions={{ status: "ready", value: { grants: ["inventory.stock.view"], denies: [], wildcard: false } }} />);
    await waitFor(() => expect(screen.queryByTestId("inventory-item-safety-stock-i-1")).not.toBeNull());
    expect((screen.getByTestId("inventory-item-safety-stock-i-1") as HTMLButtonElement).disabled).toBe(true);
  });
});
