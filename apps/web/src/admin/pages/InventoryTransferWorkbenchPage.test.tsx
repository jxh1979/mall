import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { InventoryTransferWorkbenchPage } from "./InventoryTransferWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 三张单覆盖「货账链」的三种结局：已挂账 / 该挂没挂（断链告警）/ 借用不计费。
const ORDERS = [
  { id: "trf_posted", transferType: "supply", fromOwnerType: "supply_entity", fromOwnerId: "supply_1", toOwnerType: "merchant", toOwnerId: "merchant-1", status: "shipped", createdAt: "2026-07-08T13:19:03Z", updatedAt: "2026-07-08T13:20:00Z" },
  { id: "trf_missing", transferType: "supply", fromOwnerType: "supply_entity", fromOwnerId: "supply_1", toOwnerType: "merchant", toOwnerId: "merchant-1", status: "shipped", createdAt: "2026-07-08T13:46:09Z", updatedAt: "2026-07-08T13:46:09Z" },
  { id: "trf_borrow", transferType: "lending", lendingMode: "borrow", fromOwnerType: "merchant", fromOwnerId: "merchant-A", toOwnerType: "merchant", toOwnerId: "merchant-B", status: "received", createdAt: "2026-07-08T13:09:32Z", updatedAt: "2026-07-08T13:10:00Z" },
];

const LINES: Record<string, unknown> = {
  trf_posted: [{ id: "l1", itemId: "item-1", quantity: 10, unitPriceCent: 5000, traceCodes: ["TRACE-A", "TRACE-B"] }],
  trf_missing: [{ id: "l2", itemId: "item-1", quantity: 2, unitPriceCent: 5000, traceCodes: [] }],
  trf_borrow: [{ id: "l3", itemId: "item-1", quantity: 1, unitPriceCent: 0, traceCodes: [] }],
};

const ARAP = [
  { id: "arap_trf_trf_posted", itemNo: "ARAP-TRF-trf_posted", sourceType: "inventory_transfer", sourceId: "trf_posted", amountCent: 50000, paidAmountCent: 0, unpaidAmountCent: 50000, status: "pending_payment", createdAt: "2026-07-08T13:20:00Z" },
];

function getResolver(overrides: { arapFails?: boolean } = {}) {
  return vi.fn().mockImplementation((path: string) => {
    if (path.startsWith("/api/v1/inventory/transfer-orders/")) {
      const id = path.split("/").at(-1)!;
      return Promise.resolve({ ...ORDERS.find((order) => order.id === id), lines: LINES[id] });
    }
    if (path === "/api/v1/inventory/transfer-orders") return Promise.resolve(ORDERS);
    if (path.includes("/inventory/supply-entities")) return Promise.resolve([{ id: "supply_1", name: "平台酒水供货公司" }]);
    if (path.includes("/platform/merchants")) return Promise.resolve([{ id: "merchant-1", displayName: "一号商户" }, { id: "merchant-A", displayName: "A 商户" }, { id: "merchant-B", displayName: "B 商户" }]);
    if (path.includes("/inventory/items")) return Promise.resolve([{ id: "item-1", itemName: "威士忌" }]);
    if (path.includes("/finance/ar-ap-items")) {
      return overrides.arapFails ? Promise.reject(new Error("无权限")) : Promise.resolve(ARAP);
    }
    return Promise.resolve([]);
  });
}

function makeApi(get = getResolver(), post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient = makeApi(), extra: Partial<AdminPageProps> = {}) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
  render(<InventoryTransferWorkbenchPage {...props} />);
}

afterEach(() => {
  vi.restoreAllMocks();
  window.history.replaceState({}, "", "/admin/inventory/transfers");
});

describe("跨主体调拨与借调工作台：货与账一眼可查", () => {
  it("列表按主体名展示谁调给谁，并算出本单货值", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-row-trf_posted")).toBeInTheDocument());
    const row = screen.getByTestId("inventory-transfer-row-trf_posted");
    expect(row).toHaveTextContent("平台酒水供货公司");
    expect(row).toHaveTextContent("一号商户");
    expect(row).toHaveTextContent("¥500.00");
  });

  it("已挂应付的单显示可点凭证号；该挂没挂的单亮出断链告警", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-posting-trf_posted")).toBeInTheDocument());
    expect(screen.getByTestId("inventory-transfer-posting-trf_posted")).toHaveTextContent("ARAP-TRF-trf_posted");
    expect(screen.getByTestId("inventory-transfer-posting-trf_missing")).toHaveTextContent("货已发·账未挂");
    // 借用（约定归还）默认不计占用费，没有应付才是对的，不该报断链。
    expect(screen.getByTestId("inventory-transfer-posting-trf_borrow")).toHaveTextContent("不计货款");
    expect(screen.getByTestId("inventory-transfer-kpi-missing")).toHaveTextContent("1");
    expect(screen.getByTestId("inventory-transfer-kpi-posted")).toHaveTextContent("¥500.00");
  });

  it("点应付凭证下钻到应收应付台账", async () => {
    const navigateTo = vi.fn();
    renderPage(makeApi(), { navigateTo });
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-posting-trf_posted")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("trace-link-ADM-FIN-001-arap_trf_trf_posted"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-FIN-001", "arap_trf_trf_posted");
  });

  it("读不到财务台账时不冤枉任何一单为断链", async () => {
    renderPage(makeApi(getResolver({ arapFails: true })));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-posting-trf_posted")).toBeInTheDocument());
    expect(screen.getByTestId("inventory-transfer-kpi-missing")).toHaveTextContent("0");
    expect(screen.getByTestId("inventory-transfer-posting-trf_missing")).toHaveTextContent("无法核对");
  });

  it("整行点开明细，随货溯源码与挂账凭证同屏可查", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-row-trf_posted")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("inventory-transfer-row-trf_posted"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    expect(drawer).toHaveTextContent("TRACE-A、TRACE-B");
    expect(drawer).toHaveTextContent("威士忌");
    expect(drawer).toHaveTextContent("ARAP-TRF-trf_posted");
  });

  it("收货必须逐码扫齐：漏扫被前端拦下，不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_posted")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_posted"));
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-receive-codes"), { target: { value: "TRACE-A" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-receive")).toHaveTextContent("与本单应扫的 2 个不一致"));
    expect(post).not.toHaveBeenCalled();
  });

  it("扫齐后按后端契约提交扫描码", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_posted")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_posted"));
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-receive-codes"), { target: { value: "TRACE-A TRACE-B" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_posted/receive",
      { scannedTraceCodes: ["TRACE-A", "TRACE-B"] },
    ));
  });

  it("KPI「货已发·账未挂」可点，聚焦后只留那一单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-kpi-missing")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("inventory-transfer-kpi-missing"));
    expect(screen.getByTestId("inventory-transfer-kpi-focus-banner")).toHaveTextContent("货已发·账未挂（1）");
    expect(screen.getByTestId("inventory-transfer-row-trf_missing")).toBeInTheDocument();
    expect(screen.queryByTestId("inventory-transfer-row-trf_posted")).not.toBeInTheDocument();
  });

  it("单据状态决定动作可用：在途单可收货不可发货，借用已收货单可归还", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_posted")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_posted"));
    expect(screen.getByTestId("inventory-transfer-ship")).toBeDisabled();
    expect(screen.getByTestId("inventory-transfer-receive-open")).toBeEnabled();
    expect(screen.getByTestId("inventory-transfer-return")).toBeDisabled();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_borrow"));
    expect(screen.getByTestId("inventory-transfer-return")).toBeEnabled();
    expect(screen.getByTestId("inventory-transfer-cancel")).toBeDisabled();
  });
});
