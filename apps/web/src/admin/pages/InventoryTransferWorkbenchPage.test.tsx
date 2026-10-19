// 文件版本：V0.02
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
];

const LINES: Record<string, unknown> = {
  trf_posted: [{ id: "l1", itemId: "item-1", quantity: 10, unitPriceCent: 5000, traceCodes: ["TRACE-A", "TRACE-B"] }],
  trf_lend_ship: [{ id: "l1s", itemId: "item-1", quantity: 2, unitPriceCent: 5000, traceCodes: ["TRACE-A", "TRACE-B"] }],
  trf_missing: [{ id: "l2", itemId: "item-1", quantity: 2, unitPriceCent: 5000, traceCodes: [] }],
  trf_borrow: [{ id: "l3", itemId: "item-1", quantity: 1, unitPriceCent: 0, traceCodes: [] }],
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
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_lend_ship"));
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-receive-codes"), { target: { value: "TRACE-A" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-submit"));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-receive")).toHaveTextContent("与本单应扫的 2 个不一致"));
    expect(post).not.toHaveBeenCalled();
  });

  it("扫齐后按后端契约提交扫描码", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(getResolver(), post));
    await waitFor(() => expect(screen.getByTestId("inventory-transfer-select-trf_lend_ship")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("inventory-transfer-select-trf_lend_ship"));
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-open"));
    fireEvent.change(await screen.findByTestId("inventory-transfer-receive-codes"), { target: { value: "TRACE-A TRACE-B" } });
    fireEvent.click(screen.getByTestId("inventory-transfer-receive-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/inventory/transfer-orders/trf_lend_ship/receive",
      { scannedTraceCodes: ["TRACE-A", "TRACE-B"] },
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
