// 文件版本：V0.05
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { InventoryTransferWorkbenchPage } from "./InventoryTransferWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 三张单覆盖「货账链」的三种结局：已挂账（借调买断收货后挂账）/ 已合格签收却没挂（断链告警）/ 借用不计费；另一张在途借调买断单走扫码收货。
const ORDERS = [
  { id: "trf_posted", transferType: "lending", lendingMode: "purchase", fromOwnerType: "merchant", fromOwnerId: "merchant-A", toOwnerType: "merchant", toOwnerId: "merchant-1", status: "received", createdAt: "2026-07-08T13:19:03Z", updatedAt: "2026-07-08T13:20:00Z" },
  { id: "trf_lend_ship", transferType: "lending", lendingMode: "purchase", fromOwnerType: "merchant", fromOwnerId: "merchant-A", toOwnerType: "merchant", toOwnerId: "merchant-1", status: "shipped", createdAt: "2026-07-08T14:00:00Z", updatedAt: "2026-07-08T14:00:00Z" },
  { id: "trf_missing", transferType: "supply", fromOwnerType: "supply_entity", fromOwnerId: "supply_1", toOwnerType: "merchant", toOwnerId: "merchant-1", status: "completed", createdAt: "2026-07-08T13:46:09Z", updatedAt: "2026-07-08T13:46:09Z" },
  { id: "trf_borrow", transferType: "lending", lendingMode: "borrow", fromOwnerType: "merchant", fromOwnerId: "merchant-A", toOwnerType: "merchant", toOwnerId: "merchant-B", status: "received", createdAt: "2026-07-08T13:09:32Z", updatedAt: "2026-07-08T13:10:00Z" },
  // FS-011 门店要货九态：草稿、待供货主体审核、待拣配、配送中各一张。
  ...["draft", "pending_supplier_review", "pending_picking", "in_delivery", "pending_receipt", "discrepancy"].map((status) => ({
    id: `trf_${status}`, transferType: "supply", fromOwnerType: "supply_entity", fromOwnerId: "supply_1", toOwnerType: "merchant", toOwnerId: "merchant-1", toLocationId: "loc-1", status, createdAt: "2026-10-19T08:00:00Z", updatedAt: "2026-10-19T08:00:00Z",
  })),
  // FS-019：带码的借用单各一张（列表第 2 页）——待发货（借出方交货扫码）、已收货（借出方收货扫码归还 / 无法归还处置）。
  { id: "trf_borrow_created", transferType: "lending", lendingMode: "borrow", fromOwnerType: "merchant", fromOwnerId: "merchant-A", toOwnerType: "merchant", toOwnerId: "merchant-B", status: "created", returnDueAt: "2026-12-31T10:00:00Z", createdAt: "2026-10-20T08:00:00Z", updatedAt: "2026-10-20T08:00:00Z" },
  { id: "trf_borrow_codes", transferType: "lending", lendingMode: "borrow", fromOwnerType: "merchant", fromOwnerId: "merchant-A", toOwnerType: "merchant", toOwnerId: "merchant-B", status: "received", returnDueAt: "2026-12-31T10:00:00Z", createdAt: "2026-10-20T08:00:00Z", updatedAt: "2026-10-20T08:30:00Z" },
];

const LINES: Record<string, unknown> = {
  trf_posted: [{ id: "l1", itemId: "item-1", quantity: 10, unitPriceCent: 5000, traceCodes: ["TRACE-A", "TRACE-B"] }],
  trf_lend_ship: [{ id: "l1s", itemId: "item-1", quantity: 2, unitPriceCent: 5000, traceCodes: ["TRACE-A", "TRACE-B"] }],
  trf_missing: [{ id: "l2", itemId: "item-1", quantity: 2, unitPriceCent: 5000, traceCodes: [] }],
  trf_borrow: [{ id: "l3", itemId: "item-1", quantity: 1, unitPriceCent: 0, traceCodes: [] }],
  trf_borrow_created: [{ id: "trf_borrow_created_l0", itemId: "item-1", quantity: 2, unitPriceCent: 80000, traceCodes: ["TRACE-A", "TRACE-B"] }],
  trf_borrow_codes: [{ id: "trf_borrow_codes_l0", itemId: "item-1", quantity: 2, unitPriceCent: 80000, traceCodes: ["TRACE-C", "TRACE-D"] }],
  trf_draft: [{ id: "trf_draft_l0", itemId: "item-1", quantity: 5, unitPriceCent: 1000, traceCodes: [] }],
  trf_pending_supplier_review: [{ id: "l5", itemId: "item-1", quantity: 5, unitPriceCent: 1000, traceCodes: [] }],
  trf_pending_picking: [{ id: "l6", itemId: "item-1", quantity: 5, unitPriceCent: 1000, traceCodes: [] }],
  trf_in_delivery: [{ id: "l7", itemId: "item-1", quantity: 5, unitPriceCent: 1000, traceCodes: [] }],
  trf_pending_receipt: [{ id: "trf_pending_receipt_l0", itemId: "item-1", quantity: 20, unitPriceCent: 500, traceCodes: [] }],
  trf_discrepancy: [{ id: "trf_discrepancy_l0", itemId: "item-1", quantity: 20, unitPriceCent: 500, traceCodes: [] }],
};

// FS-011 签收后的逐行结果与差异处理（GET .../dispositions）：应付按签收形成的那一笔（payable_item_id）对账。
const FULFILLMENT: Record<string, unknown> = {
  trf_discrepancy: {
    orderId: "trf_discrepancy", status: "discrepancy", payableItemId: "arap_trf_trr_d",
    lines: [{ lineId: "trf_discrepancy_l0", itemId: "item-1", quantity: 20, unitPriceCent: 500, acceptedQuantity: 16, rejectedQuantity: 2, shortQuantity: 2, resolvedQuantity: 0, unresolvedQuantity: 4 }],
    dispositions: [{ id: "trd_1", lineId: "trf_discrepancy_l0", dispositionType: "return_accepted", quantity: 2, reason: "拒收两瓶退回供货主体", status: "pending_review", requestedBy: "u-store" }],
  },
  trf_missing: {
    orderId: "trf_missing", status: "completed", payableItemId: null,
    lines: [{ lineId: "l2", itemId: "item-1", quantity: 2, unitPriceCent: 5000, acceptedQuantity: 2, rejectedQuantity: 0, shortQuantity: 0, resolvedQuantity: 0, unresolvedQuantity: 0 }],
    dispositions: [],
  },
  // FS-019：借用单两码都未归还；三条无法归还登记分别待对方确认、待财务复核、财务通过待实物处置完成。
  trf_borrow_codes: {
    orderId: "trf_borrow_codes", status: "received", returnDueAt: "2026-12-31T10:00:00Z",
    lines: [{ lineId: "trf_borrow_codes_l0", itemId: "item-1", quantity: 2, unitPriceCent: 80000, traceCodes: ["TRACE-C", "TRACE-D"], returnedQuantity: 0, unreturnableClaimedQuantity: 0, unreturnableClosedQuantity: 0, outstandingQuantity: 2 }],
    returns: [],
    dispositions: [
      { id: "trd_u1", lineId: "trf_borrow_codes_l0", dispositionType: "unreturnable", quantity: 1, traceCodes: ["TRACE-D"], amountCent: 80000, reason: "演出中摔坏", status: "pending_counterparty", requestedBy: "u-borrower" },
      { id: "trd_u2", lineId: "trf_borrow_codes_l0", dispositionType: "unreturnable", quantity: 1, traceCodes: ["TRACE-C"], amountCent: 80000, reason: "丢失", status: "pending_review", requestedBy: "u-lender" },
      { id: "trd_u3", lineId: "trf_borrow_codes_l0", dispositionType: "unreturnable", quantity: 1, traceCodes: ["TRACE-C"], amountCent: 80000, reason: "丢失", status: "approved", requestedBy: "u-lender", payableItemId: "arap_trf_trd_u3" },
    ],
  },
};

const ARAP = [
  { id: "arap_trf_trf_posted", itemNo: "ARAP-TRF-trf_posted", sourceType: "inventory_transfer", sourceId: "trf_posted", amountCent: 50000, paidAmountCent: 0, unpaidAmountCent: 50000, status: "pending_payment", createdAt: "2026-07-08T13:20:00Z" },
  { id: "arap_trf_trr_d", itemNo: "ARAP-TRF-trr_d", sourceType: "inventory_transfer", sourceId: "trr_d", amountCent: 8000, paidAmountCent: 0, unpaidAmountCent: 8000, status: "pending_payment", createdAt: "2026-10-19T09:00:00Z" },
];

function getResolver(overrides: { arapFails?: boolean } = {}) {
  return vi.fn().mockImplementation((path: string) => {
    if (path.endsWith("/dispositions")) {
      const id = path.split("/").at(-2)!;
      return FULFILLMENT[id] ? Promise.resolve(FULFILLMENT[id]) : Promise.reject(new Error("无差异处理"));
    }
    if (path === "/api/v1/inventory/warehouses") return Promise.resolve([{ id: "wh-1", merchantId: "merchant-1", name: "一号门店仓", status: "active" }, { id: "wh-2", merchantId: "merchant-A", name: "A 商户仓", status: "active" }]);
    if (path.startsWith("/api/v1/inventory/transfer-orders/")) {
      const id = path.split("/").at(-1)!;
      return Promise.resolve({ ...ORDERS.find((order) => order.id === id), lines: LINES[id] });
    }
    if (path === "/api/v1/inventory/transfer-orders") return Promise.resolve(ORDERS);
    if (path.includes("/inventory/supply-entities")) return Promise.resolve([{ id: "supply_1", name: "平台酒水供货公司" }]);
    if (path.includes("/platform/merchants")) return Promise.resolve([{ id: "merchant-1", displayName: "一号商户" }, { id: "merchant-A", displayName: "A 商户" }, { id: "merchant-B", displayName: "B 商户" }]);
    // 第十八批第 18 条：物品按商户归属——item-1 属出让方 A 商户，item-9 属接收方一号商户（买断收货只列接收方本商户物品）。
    if (path.includes("/inventory/items")) return Promise.resolve([{ id: "item-1", itemName: "威士忌", merchantId: "merchant-A" }, { id: "item-9", itemName: "一号店威士忌", merchantId: "merchant-1" }]);
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

/** 列表到达后 useSelectedRow 默认选中首行、TablePager 复位第 1 页都在被动 effect 里补做；首行单选框被勾上＝这批 effect 已执行，再点选才不会被改回。 */
async function waitDefaultSelection() {
  await waitFor(() => expect((screen.getAllByTestId(/^inventory-transfer-select-/)[0] as HTMLInputElement).checked).toBe(true));
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
    expect(row).toHaveTextContent("A 商户");
    expect(row).toHaveTextContent("一号商户");
    expect(row).toHaveTextContent("¥500.00");
  });

  it("已挂应付的单显示可点凭证号；该挂没挂的单亮出断链告警", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-posting-trf_posted")).toBeInTheDocument());
    expect(screen.getByTestId("inventory-transfer-posting-trf_posted")).toHaveTextContent("ARAP-TRF-trf_posted");
    // FS-011：门店要货按签收形成的那笔应付对账；已合格签收却没挂应付才算断链，配送中的单本就不该有账。
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-posting-trf_discrepancy")).toHaveTextContent("ARAP-TRF-trr_d"));
    expect(screen.getByTestId("inventory-transfer-posting-trf_missing")).toHaveTextContent("已合格签收·账未挂");
    expect(screen.getByTestId("inventory-transfer-posting-trf_in_delivery")).toHaveTextContent("未收货·暂不挂账");
    // 借用（约定归还）默认不计占用费，没有应付才是对的，不该报断链。
    expect(screen.getByTestId("inventory-transfer-posting-trf_borrow")).toHaveTextContent("不计货款");
    expect(screen.getByTestId("inventory-transfer-kpi-missing")).toHaveTextContent("1");
    expect(screen.getByTestId("inventory-transfer-kpi-posted")).toHaveTextContent("¥580.00");
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
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_lend_ship")).toBeInTheDocument());
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_lend_ship"));
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-receive-location"), { target: { value: "loc-1" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-receive-item-l1s"), { target: { value: "item-9" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-receive-codes"), { target: { value: "TRACE-A" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-receive")).toHaveTextContent("与本单应扫的 2 个不一致"));
    expect(post).not.toHaveBeenCalled();
  });

  // 第十八批第 18 条（BSM:1201「「调」是买断，成交时所有权转移给接收方」）：买断收货入接收方本商户物品与仓库库位，未选物品被前端拦下不发请求。
  it("借调买断收货须选收货仓库、库位与本商户物品：未选物品被拦下，不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_lend_ship")).toBeInTheDocument());
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_lend_ship"));
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-open"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-receive-item-l1s")).not.toBeNull());
    expect(screen.getByTestId("inventory-transfer-receive-warehouse")).toHaveValue("wh-1");
    const options = Array.from(screen.getByTestId("inventory-transfer-receive-item-l1s").querySelectorAll("option")).map((option) => option.getAttribute("value"));
    expect(options).toEqual(["", "item-9"]);
    fireEvent.change(screen.getByTestId("inventory-transfer-receive-location"), { target: { value: "loc-1" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-receive-codes"), { target: { value: "TRACE-A TRACE-B" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-receive")).toHaveTextContent("请为每一行选择本商户物品"));
    expect(post).not.toHaveBeenCalled();
  });

  it("扫齐后按后端契约提交扫描码", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_lend_ship")).toBeInTheDocument());
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_lend_ship"));
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-receive-location"), { target: { value: "loc-1" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-receive-item-l1s"), { target: { value: "item-9" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-receive-codes"), { target: { value: "TRACE-A TRACE-B" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_lend_ship/receive",
      { scannedTraceCodes: ["TRACE-A", "TRACE-B"], warehouseId: "wh-1", locationId: "loc-1", lines: [{ lineId: "l1s", itemId: "item-9" }] },
    ));
  });

  it("KPI「已合格签收·账未挂」可点，聚焦后只留那一单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-kpi-missing")).toHaveTextContent("1"));
    fireEvent.click(screen.getByTestId("inventory-transfer-kpi-missing"));
    expect(screen.getByTestId("inventory-transfer-kpi-focus-banner")).toHaveTextContent("已合格签收·账未挂（1）");
    expect(screen.getByTestId("inventory-transfer-row-trf_missing")).toBeInTheDocument();
    expect(screen.queryByTestId("inventory-transfer-row-trf_posted")).not.toBeInTheDocument();
  });

  it("单据状态决定动作可用：在途单可收货不可发货，借用已收货单可归还", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_lend_ship")).toBeInTheDocument());
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_lend_ship"));
    expect(screen.getByTestId("inventory-transfer-ship")).toBeDisabled();
    expect(screen.getByTestId("inventory-transfer-receive-open")).toBeEnabled();
    expect(screen.getByTestId("inventory-transfer-return")).toBeDisabled();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_borrow"));
    expect(screen.getByTestId("inventory-transfer-return")).toBeEnabled();
    expect(screen.getByTestId("inventory-transfer-cancel")).toBeDisabled();
  });
});

// FS-011（BSM L1212/L1213、ENUM L169；操作员裁决 2026-10-19 第 2 条）：门店要货按九态逐步推进，供货主体不同意退回草稿，未出库取消须审批。
describe("门店要货九态：按状态提交、审核、拣配、出库、送达与取消", () => {
  it("草稿显示中文状态，门店可改单后再提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-row-trf_draft")).toHaveTextContent("草稿"));
    expect(screen.getByTestId("inventory-transfer-posting-trf_draft")).not.toHaveTextContent("账未挂");
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_draft"));
    fireEvent.click(screen.getByTestId("inventory-transfer-edit-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-create-quantity"), { target: { value: "3" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-create-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_draft/lines",
      { lines: [{ itemId: "item-1", quantity: 3, unitPriceCent: 1000, traceCodes: [] }] },
    ));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-submit")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders/trf_draft/submit", {}));
  });

  it("供货主体不同意须写原因，提交后退回草稿", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-row-trf_pending_supplier_review")).toHaveTextContent("待供货主体审核"));
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_pending_supplier_review"));
    fireEvent.click(screen.getByTestId("inventory-transfer-review-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-review-decision"), { target: { value: "rejected" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-review-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-review")).toHaveTextContent("不同意须写明原因"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("inventory-transfer-review-reason"), { target: { value: "库存不足，请改为 3 瓶" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-review-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_pending_supplier_review/supplier-review",
      { decision: "rejected", reason: "库存不足，请改为 3 瓶" },
    ));
  });

  it("已提交未出库取消须写原因并走审批；出库后不能取消，只能送达待签收", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-row-trf_pending_picking")).toHaveTextContent("待拣配"));
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_pending_picking"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-pick")).toBeEnabled());
    expect(screen.getByTestId("inventory-transfer-ship")).toBeDisabled();
    expect(screen.getByTestId("inventory-transfer-cancel")).toBeDisabled();
    fireEvent.click(screen.getByTestId("inventory-transfer-cancel-request-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-cancel-request-reason"), { target: { value: "门店活动取消" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-cancel-request-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_pending_picking/cancel",
      { operatorNote: "门店活动取消" },
    ));

    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_in_delivery"));
    expect(screen.getByTestId("inventory-transfer-row-trf_in_delivery")).toHaveTextContent("配送中");
    expect(screen.getByTestId("inventory-transfer-cancel-request-open")).toBeDisabled();
    expect(screen.getByTestId("inventory-transfer-cancel")).toBeDisabled();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-deliver")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-deliver"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders/trf_in_delivery/deliver", {}));
  });
});

// FS-011 R3/R4（BSM L1212「签收分别记录实收、合格接收、拒收和短少数量……已出库数量须实际退回验收，或经复核完成损失处理，不能仅点取消就回库。
// 供货、配送保管和收货方按交接证据确认差异，财务复核金额后办理补发、退回、货款调整或责任损失，不重复收款」）。
describe("门店要货签收与差异处理：按行签收、出库后不可直接取消、凭交接证据登记、财务复核", () => {
  it("待签收按行录实收、合格、拒收，短少＝发货－实收只读算出，按后端契约提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-row-trf_pending_receipt")).toHaveTextContent("待签收"));
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_pending_receipt"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-receive-open")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-open"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-receipt-arrived-trf_pending_receipt_l0")).toBeInTheDocument());
    fireEvent.change(screen.getByTestId("inventory-transfer-receipt-arrived-trf_pending_receipt_l0"), { target: { value: "18" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-receipt-accepted-trf_pending_receipt_l0"), { target: { value: "16" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-receipt-rejected-trf_pending_receipt_l0"), { target: { value: "1" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-receipt-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-receipt")).toHaveTextContent("实收须等于合格接收加拒收"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("inventory-transfer-receipt-rejected-trf_pending_receipt_l0"), { target: { value: "2" } });
    expect(screen.getByTestId("inventory-transfer-receipt-short-trf_pending_receipt_l0")).toHaveTextContent("2");
    fireEvent.click(screen.getByTestId("inventory-transfer-receipt-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders/trf_pending_receipt/receive", {
      warehouseId: "wh-1",
      locationId: "loc-1",
      lines: [{ lineId: "trf_pending_receipt_l0", arrivedQuantity: 18, acceptedQuantity: 16, rejectedQuantity: 2, acceptedTraceCodes: [] }],
    }));
  });

  it("已出库在途的单不能直接取消", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_lend_ship")).toBeInTheDocument());
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_lend_ship"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-cancel")).toBeDisabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_discrepancy"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-cancel")).toBeDisabled());
    expect(screen.getByTestId("inventory-transfer-cancel-request-open")).toBeDisabled();
  });

  it("差异处理中按行显示待处理数量；登记须附交接证据，按后端契约提交", async () => {
    const post = vi.fn().mockResolvedValue({ id: "trd_2", status: "pending_review" });
    const postForm = vi.fn().mockResolvedValue({ id: "att-1" });
    renderPage({ ...makeApi(getResolver(), post), postForm });
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-row-trf_discrepancy")).toHaveTextContent("差异处理中"));
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_discrepancy"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-disposition-open")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-disposition-open")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-disposition-open"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-disposition-unresolved-trf_discrepancy_l0")).toHaveTextContent("4"));
    fireEvent.change(screen.getByTestId("inventory-transfer-disposition-type"), { target: { value: "loss" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-disposition-quantity"), { target: { value: "2" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-disposition-responsible"), { target: { value: "supplier" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-disposition-reason"), { target: { value: "交接单短少两瓶" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-disposition-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-disposition")).toHaveTextContent("须上传交接证据"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("inventory-transfer-disposition-evidence"), { target: { files: [new File(["x"], "handover.png", { type: "image/png" })] } });
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-disposition")).toHaveTextContent("handover.png"));
    expect(postForm).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-disposition-submit")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-disposition-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders/trf_discrepancy/dispositions", {
      dispositionType: "loss",
      lineId: "trf_discrepancy_l0",
      quantity: 2,
      responsibleParty: "supplier",
      reason: "交接单短少两瓶",
      evidenceAttachmentIds: ["att-1"],
    }));
  });

  it("待复核的差异处理由财务复核通过或驳回", async () => {
    const post = vi.fn().mockResolvedValue({ id: "trd_1", status: "approved" });
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-row-trf_discrepancy")).toHaveTextContent("差异处理中"));
    await waitDefaultSelection();
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_discrepancy"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-disposition-open")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-disposition-open")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-disposition-open"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-disposition-row-trd_1")).toHaveTextContent("退回验收"));
    expect(screen.getByTestId("inventory-transfer-disposition-row-trd_1")).toHaveTextContent("待复核");
    fireEvent.change(screen.getByTestId("inventory-transfer-disposition-review-reason"), { target: { value: "核对退回交接单" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-disposition-approve-trd_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_discrepancy/dispositions/trd_1/review",
      { decision: "approved", reason: "核对退回交接单" },
    ));
  });
});

// FS-019（BSM L1201「「借」只转移保管责任、不转移所有权」「不可消费物品……的借用必须约定归还时间」「借出时间取借出方交货扫码的时刻」
// 「实际归还时间取借出方收货扫码的时刻」「不可消费借用物无法归还时……由借出所有权方与借入保管方确认……并经主管及财务复核」）。
describe("借用只转保管责任：约定归还时间、交货与收货扫码归还、无法归还双方确认与处置", () => {
  /** 借用单夹具在列表第 2 页：翻页后选中。 */
  async function selectOnPageTwo(id: string) {
    await waitDefaultSelection();
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-pager-next")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-pager-next"));
    await waitFor(() => expect(screen.queryByTestId(`inventory-transfer-select-${id}`)).not.toBeNull());
    fireEvent.click(screen.getByTestId(`inventory-transfer-select-${id}`));
  }

  async function openCreateAsBorrow() {
    fireEvent.click(screen.getByTestId("inventory-transfer-create-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-create-type"), { target: { value: "lending" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-create-mode"), { target: { value: "borrow" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-create-from"), { target: { value: "merchant-A" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-create-to"), { target: { value: "merchant-B" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-create-item"), { target: { value: "item-1" } });
  }

  // 第十八批第 18 条（ENUM:162「借出」）：借调交货扣出让方库位库存，建单须填出货库位。
  it("借调建单必须填出货库位：缺填被前端拦下不发请求，填了按后端契约提交 fromLocationId", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-create-open")).toBeEnabled());
    await openCreateAsBorrow();
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-create-from-location")).not.toBeNull());
    fireEvent.change(screen.getByTestId("inventory-transfer-create-return-due"), { target: { value: "2026-12-31T10:00" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-create-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-create")).toHaveTextContent("请填写出货库位。"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("inventory-transfer-create-from-location"), { target: { value: "loc-A" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-create-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders", expect.objectContaining({
      transferType: "lending",
      fromOwnerId: "merchant-A",
      fromLocationId: "loc-A",
    })));
  });

  it("借用建单必须填约定归还时间：缺填被前端拦下不发请求，填了按后端契约提交 returnDueAt", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-create-open")).toBeEnabled());
    await openCreateAsBorrow();
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-create-return-due")).not.toBeNull());
    fireEvent.click(screen.getByTestId("inventory-transfer-create-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-create")).toHaveTextContent("请填写约定归还时间"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("inventory-transfer-create-return-due"), { target: { value: "2026-12-31T10:00" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-create-from-location"), { target: { value: "loc-A" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-create-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders", expect.objectContaining({
      transferType: "lending",
      lendingMode: "borrow",
      fromOwnerId: "merchant-A",
      toOwnerId: "merchant-B",
      returnDueAt: new Date("2026-12-31T10:00").toISOString(),
    })));
  });

  it("借用单发货须逐码扫齐后按后端契约提交 scanned_trace_codes", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await selectOnPageTwo("trf_borrow_created");
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-ship")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-ship"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-ship-scan-codes")).not.toBeNull());
    fireEvent.change(screen.getByTestId("inventory-transfer-ship-scan-codes"), { target: { value: "TRACE-A" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-ship-scan-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-ship-scan")).toHaveTextContent("与本单应扫的 2 个不一致"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("inventory-transfer-ship-scan-codes"), { target: { value: "TRACE-A TRACE-B" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-ship-scan-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_borrow_created/ship",
      { scannedTraceCodes: ["TRACE-A", "TRACE-B"] },
    ));
  });

  it("归还须逐件扫码：漏扫被前端拦下，不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await selectOnPageTwo("trf_borrow_codes");
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-return")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-return"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-return-codes-trf_borrow_codes_l0")).not.toBeNull());
    fireEvent.change(screen.getByTestId("inventory-transfer-return-codes-trf_borrow_codes_l0"), { target: { value: "TRACE-C" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-return-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-return-form")).toHaveTextContent("归还 2 件须逐件扫码录入溯源码"));
    expect(post).not.toHaveBeenCalled();
  });

  it("扫齐后按后端契约提交 lines（可部分归还）", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await selectOnPageTwo("trf_borrow_codes");
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-return")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-return"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-return-quantity-trf_borrow_codes_l0")).not.toBeNull());
    fireEvent.change(screen.getByTestId("inventory-transfer-return-quantity-trf_borrow_codes_l0"), { target: { value: "1" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-return-codes-trf_borrow_codes_l0"), { target: { value: "TRACE-C" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-return-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_borrow_codes/return",
      { lines: [{ lineId: "trf_borrow_codes_l0", returnedQuantity: 1, traceCodes: ["TRACE-C"] }] },
    ));
  });

  it("借用单登记无法归还须附证据并按后端契约提交", async () => {
    const post = vi.fn().mockResolvedValue({ id: "trd_u9", status: "pending_counterparty" });
    const postForm = vi.fn().mockResolvedValue({ id: "att-9" });
    renderPage({ ...makeApi(getResolver(), post), postForm });
    await selectOnPageTwo("trf_borrow_codes");
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-unreturnable-open")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-unreturnable-open")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-unreturnable-open"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-unreturnable-quantity")).not.toBeNull());
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-quantity"), { target: { value: "1" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-codes"), { target: { value: "TRACE-D" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-reason"), { target: { value: "演出中摔坏" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-disposal"), { target: { value: "借出方收回残件报废" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-unreturnable-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-unreturnable")).toHaveTextContent("须上传证据"));
    expect(post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-evidence"), { target: { files: [new File(["x"], "damage.png", { type: "image/png" })] } });
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-unreturnable")).toHaveTextContent("damage.png"));
    expect(postForm).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-unreturnable-submit")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-unreturnable-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders/trf_borrow_codes/unreturnable", {
      lineId: "trf_borrow_codes_l0",
      quantity: 1,
      traceCodes: ["TRACE-D"],
      reason: "演出中摔坏",
      amountCent: 80000,
      disposalNote: "借出方收回残件报废",
      evidenceAttachmentIds: ["att-9"],
    }));
  });

  it("无法归还赔偿低于数量×内部结算价时拦截（操作员裁决「不得低于内部结算价」）", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await selectOnPageTwo("trf_borrow_codes");
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-unreturnable-open")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-unreturnable-open"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-unreturnable-amount")).not.toBeNull());
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-quantity"), { target: { value: "1" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-codes"), { target: { value: "TRACE-D" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-reason"), { target: { value: "丢失" } });
    fireEvent.change(screen.getByTestId("inventory-transfer-unreturnable-amount"), { target: { value: "799.99" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-unreturnable-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-unreturnable")).toHaveTextContent("赔偿不得低于内部结算价"));
    expect(post).not.toHaveBeenCalled();
  });

  it("处置按状态显示对方确认 / 财务复核 / 实物处置完成按钮，并按后端契约提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await selectOnPageTwo("trf_borrow_codes");
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-unreturnable-open")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-unreturnable-open")).toBeEnabled());
    fireEvent.click(screen.getByTestId("inventory-transfer-unreturnable-open"));
    await waitFor(() => expect(screen.queryByTestId("inventory-transfer-unreturnable-row-trd_u1")).not.toBeNull());
    expect(screen.getByTestId("inventory-transfer-unreturnable-row-trd_u1")).toHaveTextContent("待对方确认");
    expect(screen.getByTestId("inventory-transfer-unreturnable-confirm-trd_u1")).toBeInTheDocument();
    expect(screen.getByTestId("inventory-transfer-unreturnable-row-trd_u2")).toHaveTextContent("待复核（财务）");
    expect(screen.getByTestId("inventory-transfer-disposition-approve-trd_u2")).toBeInTheDocument();
    expect(screen.getByTestId("inventory-transfer-unreturnable-row-trd_u3")).toHaveTextContent("财务复核通过");
    expect(screen.getByTestId("inventory-transfer-unreturnable-row-trd_u3")).toHaveTextContent("单独跟进");
    expect(screen.getByTestId("inventory-transfer-disposal-complete-trd_u3")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("inventory-transfer-unreturnable-confirm-trd_u1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders/trf_borrow_codes/dispositions/trd_u1/confirm", { operatorNote: null }));
    fireEvent.click(screen.getByTestId("inventory-transfer-unreturnable-open"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-disposal-complete-trd_u3")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("inventory-transfer-disposal-complete-trd_u3"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/inventory/transfer-orders/trf_borrow_codes/dispositions/trd_u3/disposal-completed", { operatorNote: null }));
  });
});
