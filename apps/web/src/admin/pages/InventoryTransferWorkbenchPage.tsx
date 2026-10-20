// 文件版本：V0.04
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { FormModal } from "../components/FormModal";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { TablePager, usePagedRows } from "../components/TablePager";
import { TechDetails } from "../components/TechDetails";
import { TraceLink } from "../components/TraceLink";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// V277 库存跨主体流转（平台供货公司送货给商户＝调拨；商户 A 借/调给商户 B＝借调）后台工作台。
// 后端建单/发货/收货/归还/取消已具备（FS-011 起门店要货走九态、按行签收合格后才挂应付、已出库差异经登记与财务复核处理），
// 此页补仓管与财务的可视与可点：「谁调给谁 → 调了什么多少 → 收没收到 → 这笔算多少钱 → 进没进应收应付台账」一行看全，
// 断链（已合格签收、账没挂）单独立卡告警——货对不上账最容易出在这一段。
// 读权限 inventory.transfer_order.view 由路由 requiredPermissions 门控；写动作按钮用 manage 码。
// FS-019（BSM L1201）：借用只转保管责任不转所有权——建单须约定归还时间，借出方交货扫码、借入方收货扫码、借出方收货扫码按行按码归还，
// 无法归还经双方确认、主管与财务复核后形成借入方对借出方应付，借出方登记实物处置完成才结束该数量的归还事项。
const MANAGE_PERMISSION = "inventory.transfer_order.manage";

type TransferLine = {
  id: string;
  itemId?: string;
  quantity?: number;
  traceCodes?: string[];
  unitPriceCent?: number;
};

type TransferOrder = {
  id: string;
  transferType?: string;
  lendingMode?: string;
  fromOwnerType?: string;
  fromOwnerId?: string;
  toOwnerType?: string;
  toOwnerId?: string;
  fromLocationId?: string;
  toLocationId?: string;
  status?: string;
  operatorNote?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  returnDueAt?: string | null;
  lines?: TransferLine[];
};

type SupplyEntity = { id: string; name?: string; entityType?: string; status?: string };
type Warehouse = { id: string; merchantId?: string; name?: string; status?: string };
// FS-011 签收后逐行结果与差异处理（GET .../dispositions）。
type FulfillmentLine = {
  lineId: string;
  itemId?: string;
  quantity?: number;
  unitPriceCent?: number;
  acceptedQuantity?: number;
  rejectedQuantity?: number;
  shortQuantity?: number;
  resolvedQuantity?: number;
  unresolvedQuantity?: number;
  // FS-019 借用单：已归还、无法归还登记中、实物处置已完成、尚未归还。
  traceCodes?: string[];
  returnedQuantity?: number;
  unreturnableClaimedQuantity?: number;
  unreturnableClosedQuantity?: number;
  outstandingQuantity?: number;
};
type Disposition = {
  id: string;
  lineId?: string | null;
  dispositionType?: string;
  quantity?: number | null;
  responsibleParty?: string | null;
  amountCent?: number | null;
  reason?: string;
  status?: string;
  requestedBy?: string;
  reviewedBy?: string | null;
  reviewReason?: string | null;
  reshipOrderId?: string | null;
  traceCodes?: string[];
  confirmedBy?: string | null;
  disposalNote?: string | null;
  payableItemId?: string | null;
};
type TransferReturn = { id: string; lineId?: string; returnedQuantity?: number; traceCodes?: string[]; receivedBy?: string; receivedAt?: string };
type Fulfillment = { payableItemId?: string | null; lines?: FulfillmentLine[]; dispositions?: Disposition[]; returns?: TransferReturn[] };
type MerchantLite = { id: string; displayName?: string; merchantName?: string; companyName?: string };
type ItemLite = { id: string; itemName?: string; itemCode?: string; unit?: string };
type ArApLite = {
  id: string;
  itemNo?: string;
  sourceType?: string;
  sourceId?: string;
  amountCent?: number;
  paidAmountCent?: number;
  unpaidAmountCent?: number;
  status?: string;
  createdAt?: string;
};

const TEXT = {
  title: "跨主体调拨与借调",
  description:
    "平台供货公司送货给商户（调拨）、商户之间借/调（借调）的流转单在这里建单、发货、扫码收货、归还与取消；每一单的货值与应付挂账凭证同屏可查，可直接下钻应收应付台账。",
  evidence:
    "接口证据：读取 /api/v1/inventory/transfer-orders、/api/v1/inventory/transfer-orders/{order_id}、/api/v1/inventory/transfer-orders/{order_id}/dispositions、/api/v1/inventory/supply-entities、/api/v1/inventory/items、/api/v1/inventory/warehouses、/api/v1/platform/merchants、/api/v1/finance/ar-ap-items；写入 POST /api/v1/inventory/transfer-orders 及其 /submit、/lines、/supplier-review、/pick、/ship、/deliver、/receive、/dispositions、/dispositions/{disposition_id}/review、/return、/unreturnable、/dispositions/{disposition_id}/confirm、/dispositions/{disposition_id}/disposal-completed、/cancel，上传 /api/v1/evidence/attachments/upload。",
  loadFailed: "跨主体流转单读取失败",
};

// 流转口径中文（本域专有语义：received=商户已收到货，returned=借用货已还回出让方），
// 不并入全站通用状态码表，避免和票务/订单同名码互相污染。
const TRANSFER_TYPE_LABELS: Record<string, string> = { supply: "平台供货调拨", lending: "商户间借调" };
const LENDING_MODE_LABELS: Record<string, string> = { borrow: "借用（约定归还）", purchase: "买断（不归还）" };
const TRANSFER_STATUS_LABELS: Record<string, string> = {
  created: "待发货",
  shipped: "在途待收货",
  received: "已收货",
  settled: "已结清",
  returned: "已归还",
  cancelled: "已取消",
  // FS-011 门店要货（平台供货调拨）九态（BSM L1213、ENUM L169）。
  draft: "草稿",
  pending_supplier_review: "待供货主体审核",
  pending_picking: "待拣配",
  pending_outbound: "待出库",
  in_delivery: "配送中",
  pending_receipt: "待签收",
  discrepancy: "差异处理中",
  completed: "已完成",
};
/** 门店要货签收前的各态：货款在合格签收后才形成，此前没有应付是对的。 */
const REQUISITION_BEFORE_RECEIPT = new Set(["draft", "pending_supplier_review", "pending_picking", "pending_outbound", "in_delivery", "pending_receipt"]);
/** 门店要货签收后的两态：货款按签收形成的那笔应付（payable_item_id）对账。 */
const REQUISITION_AFTER_RECEIPT = new Set(["discrepancy", "completed"]);
/** 已提交、未出库：取消须经审批（BSM L1212「未出库数量可经审批取消」）。 */
const REQUISITION_CANCEL_NEEDS_APPROVAL = new Set(["pending_supplier_review", "pending_picking", "pending_outbound"]);
const OWNER_TYPE_LABELS: Record<string, string> = { supply_entity: "平台供货公司", merchant: "商户" };
// 差异处理（BSM L1212「财务复核金额后办理补发、退回、货款调整或责任损失」）。
const DISPOSITION_LABELS: Record<string, string> = { return_accepted: "退回验收", loss: "损失", reship: "补发", payment_adjustment: "货款调减", unreturnable: "无法归还（损坏/丢失）" };
const RESPONSIBLE_LABELS: Record<string, string> = { supplier: "供货主体", carrier: "配送保管方", receiver: "收货方（门店）" };
const DISPOSITION_STATUS_LABELS: Record<string, string> = {
  pending_counterparty: "待对方确认", pending_supervisor: "主管复核中", pending_review: "待复核（财务）", approved: "财务复核通过", rejected: "已驳回", disposal_completed: "实物处置已完成",
};
/** FS-019 借用单：收货后（含部分归还）才有归还、无法归还可办；返回后只看。 */
const BORROW_AFTER_RECEIPT = new Set(["received", "returned"]);
type ReturnInput = { quantity: string; codes: string };
const EMPTY_UNRETURNABLE = { lineId: "", quantity: "1", codes: "", reason: "", amountYuan: "", disposalNote: "" };
const FINANCE_REVIEW_PERMISSION = "finance.ar_ap.manage";
type ReceiptInput = { arrived: string; accepted: string; rejected: string; codes: string };
const EMPTY_RECEIPT_INPUT: ReceiptInput = { arrived: "", accepted: "", rejected: "", codes: "" };
const EMPTY_DISPOSITION = { type: "return_accepted", lineId: "", quantity: "1", responsible: "supplier", amountYuan: "", codes: "", reason: "", reviewReason: "" };

function label(table: Record<string, string>, code?: string | null): string {
  if (!code) return "—";
  return table[code] ?? code;
}

function yuan(cent?: number | null): string {
  if (typeof cent !== "number" || !Number.isFinite(cent)) return "—";
  return `¥${(cent / 100).toFixed(2)}`;
}

/** 单据货值＝Σ(数量 × 结算单价)。明细未取到（尚未加载/接口降级）时返回 null，不用 0 冒充“免费”。 */
function orderAmountCent(order: TransferOrder): number | null {
  if (!order.lines) return null;
  return order.lines.reduce((sum, line) => sum + (line.quantity ?? 0) * (line.unitPriceCent ?? 0), 0);
}

function orderQuantity(order: TransferOrder): number | null {
  if (!order.lines) return null;
  return order.lines.reduce((sum, line) => sum + (line.quantity ?? 0), 0);
}

/** 借用（borrow）默认不计占用费＝不挂账；供货调拨与买断按货款挂应付（后端在接收方合格签收/收货后挂账）。 */
function isBillable(order: TransferOrder): boolean {
  return order.transferType === "supply" || (order.transferType === "lending" && order.lendingMode === "purchase");
}

function isBorrowOrder(order?: TransferOrder | null): boolean {
  return order?.transferType === "lending" && order.lendingMode === "borrow";
}

/** 借用单当前保管方：收货后在借入方，归还/处置完成后回到借出方；所有权始终是借出方（BSM L1201）。 */
function custodianOf(order: TransferOrder): string | undefined {
  return order.status === "received" ? order.toOwnerId : order.fromOwnerId;
}

type PostingKind =
  | "posted" // 已挂应付，凭证可点
  | "not_shipped" // 还没收货（借调在途、门店要货未签收），按口径本就不该有账
  | "free_of_charge" // 借用不计费 / 货值为 0 的免费供货 / 一件都没合格接收
  | "cancelled" // 单已取消（只有出库前能取消，本就没账）
  | "missing" // ⚠ 已合格签收（或借调已收货），账没挂上：货账断链
  | "unknown"; // 财务台账没读到（无权限或接口降级），不下断言

type Posting = { kind: PostingKind; arap?: ArApLite };

const POSTING_LABELS: Record<PostingKind, string> = {
  posted: "已挂应付",
  not_shipped: "未收货·暂不挂账",
  free_of_charge: "不计货款",
  cancelled: "已取消·不挂账",
  missing: "⚠ 已合格签收·账未挂",
  unknown: "无法核对（无财务台账查看权限）",
};

type ArapIndex = { bySource: Map<string, ArApLite>; byId: Map<string, ArApLite> };

function postingOf(order: TransferOrder, arapIndex: ArapIndex | null, fulfillment?: Fulfillment | null): Posting {
  const legacy = arapIndex?.bySource.get(order.id);
  if (legacy) return { kind: "posted", arap: legacy };
  const status = order.status ?? "";
  if (status === "cancelled") return { kind: "cancelled" };
  // FS-011：出库、在途都不该挂账，货款在接收方合格签收（借调收货）后才形成。
  if (status === "created" || status === "shipped" || REQUISITION_BEFORE_RECEIPT.has(status)) return { kind: "not_shipped" };
  if (arapIndex === null) return { kind: "unknown" };
  if (order.transferType === "supply" && REQUISITION_AFTER_RECEIPT.has(status)) {
    if (!fulfillment) return { kind: "unknown" };
    const posted = fulfillment.payableItemId ? arapIndex.byId.get(fulfillment.payableItemId) : undefined;
    if (posted) return { kind: "posted", arap: posted };
    const accepted = (fulfillment.lines ?? []).reduce((sum, line) => sum + (line.acceptedQuantity ?? 0) * (line.unitPriceCent ?? 0), 0);
    return accepted === 0 ? { kind: "free_of_charge" } : { kind: "missing" };
  }
  const amount = orderAmountCent(order);
  if (!isBillable(order) || amount === 0) return { kind: "free_of_charge" };
  if (amount === null) return { kind: "unknown" };
  return { kind: "missing" };
}

// KPI 卡聚焦（照 InventoryLedgerPage 样板）：点数字＝带 ?kpi= 参数重进本页，只列该口径下的单。
type KpiFocus = "pending" | "in_transit" | "received" | "posted" | "missing";
const KPI_FOCUS_KEYS: KpiFocus[] = ["pending", "in_transit", "received", "posted", "missing"];
const KPI_FOCUS_TITLES: Record<KpiFocus, string> = {
  pending: "待发货",
  in_transit: "在途待收货",
  received: "已收货",
  posted: "已挂应付",
  missing: "已合格签收·账未挂",
};

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const value = new URLSearchParams(window.location.search).get("kpi");
  return KPI_FOCUS_KEYS.includes(value as KpiFocus) ? (value as KpiFocus) : null;
}

function matchesFocus(order: TransferOrder, posting: Posting, focus: KpiFocus): boolean {
  if (focus === "pending") return order.status === "created";
  if (focus === "in_transit") return order.status === "shipped";
  if (focus === "received") return order.status === "received";
  if (focus === "posted") return posting.kind === "posted";
  return posting.kind === "missing";
}

function splitCodes(raw: string): string[] {
  return raw
    .split(/[\s,，;；]+/)
    .map((code) => code.trim())
    .filter(Boolean);
}

export function InventoryTransferWorkbenchPage({
  api,
  setActionResult,
  setGlobalError,
  effectivePermissions,
  navigateTo,
  focusObjectId,
}: AdminPageProps) {
  const [orders, setOrders] = React.useState<TransferOrder[]>([]);
  const [supplyEntities, setSupplyEntities] = React.useState<SupplyEntity[]>([]);
  const [merchants, setMerchants] = React.useState<MerchantLite[]>([]);
  const [items, setItems] = React.useState<ItemLite[]>([]);
  // null＝财务台账没读到（无 finance.ar_ap.view 或接口降级）：此时不对“账挂没挂”下断言。
  const [arapIndex, setArapIndex] = React.useState<ArapIndex | null>(null);
  // 门店要货签收后（差异处理中、已完成）的逐行结果与差异处理；读不到为 null。
  const [fulfillments, setFulfillments] = React.useState<Map<string, Fulfillment | null>>(new Map());
  const [warehouses, setWarehouses] = React.useState<Warehouse[]>([]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [createOpen, setCreateOpen] = React.useState(false);
  const [receiveOpen, setReceiveOpen] = React.useState(false);
  const [scannedCodes, setScannedCodes] = React.useState("");
  // FS-019：借用单借出方交货扫码、借出方收货按行扫码归还、无法归还登记。
  const [shipOpen, setShipOpen] = React.useState(false);
  const [shipCodes, setShipCodes] = React.useState("");
  const [returnOpen, setReturnOpen] = React.useState(false);
  const [returnLines, setReturnLines] = React.useState<Record<string, ReturnInput>>({});
  const [unreturnableOpen, setUnreturnableOpen] = React.useState(false);
  const [unreturnable, setUnreturnable] = React.useState(EMPTY_UNRETURNABLE);
  const [returnDueAt, setReturnDueAt] = React.useState("");
  // FS-011：草稿改单复用建单表单；供货主体审核与未出库取消申请各一个弹窗。
  const [editingId, setEditingId] = React.useState<string | null>(null);
  const [reviewOpen, setReviewOpen] = React.useState(false);
  const [reviewDecision, setReviewDecision] = React.useState("approved");
  const [reviewReason, setReviewReason] = React.useState("");
  const [cancelRequestOpen, setCancelRequestOpen] = React.useState(false);
  const [cancelReason, setCancelReason] = React.useState("");
  // FS-011 门店要货按行签收；差异处理登记与复核。
  const [receiptOpen, setReceiptOpen] = React.useState(false);
  const [receiptWarehouse, setReceiptWarehouse] = React.useState("");
  const [receiptLocation, setReceiptLocation] = React.useState("");
  const [receiptLines, setReceiptLines] = React.useState<Record<string, ReceiptInput>>({});
  const [dispositionOpen, setDispositionOpen] = React.useState(false);
  const [disposition, setDisposition] = React.useState(EMPTY_DISPOSITION);
  const [proofs, setProofs] = React.useState<{ id: string; name: string }[]>([]);
  const [uploading, setUploading] = React.useState(false);
  // 建单表单
  const [transferType, setTransferType] = React.useState("supply");
  const [lendingMode, setLendingMode] = React.useState("borrow");
  const [fromOwnerId, setFromOwnerId] = React.useState("");
  const [toOwnerId, setToOwnerId] = React.useState("");
  const [itemId, setItemId] = React.useState("");
  const [quantity, setQuantity] = React.useState("1");
  const [unitPriceYuan, setUnitPriceYuan] = React.useState("0");
  const [traceCodes, setTraceCodes] = React.useState("");
  const [operatorNote, setOperatorNote] = React.useState("");

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [orderRows, entityRows, merchantRows, itemRows, arapRows, warehouseRows] = await Promise.all([
        api.get<TransferOrder[]>("/api/v1/inventory/transfer-orders"),
        api.get<SupplyEntity[]>("/api/v1/inventory/supply-entities").catch(() => [] as SupplyEntity[]),
        api.get<MerchantLite[]>("/api/v1/platform/merchants").catch(() => [] as MerchantLite[]),
        api.get<ItemLite[]>("/api/v1/inventory/items").catch(() => [] as ItemLite[]),
        // 关键词跨 source_type 服务端过滤（后端 listArApItemsPaged 的 ILIKE 白名单含 source_type），
        // 只取本域挂的应付；无 finance.ar_ap.view 的仓管账号会 403 → 降级为“无法核对”，不阻断发货收货主链。
        api
          .get<ArApLite[]>("/api/v1/finance/ar-ap-items?keyword=inventory_transfer&size=200")
          .catch(() => null),
        // 签收选仓用；无库房权限时为空，签收弹窗改为手填仓库编号。
        api.get<Warehouse[]>("/api/v1/inventory/warehouses").catch(() => [] as Warehouse[]),
      ]);
      // ponytail: 逐单取明细（列表接口不带 lines），开发库 14 单可接受；
      // 单量上百需后端 list 直接带 lines 或加一个货值汇总端点，别在前端 N+1。
      const withLines = await Promise.all(
        orderRows.map((order) =>
          api.get<TransferOrder>(`/api/v1/inventory/transfer-orders/${order.id}`).catch(() => order),
        ),
      );
      // FS-011：门店要货的应付挂在签收（及差异处理）上，按签收结果里的 payable_item_id 对账。
      const fulfillmentRows = await Promise.all(
        withLines
          .filter((order) => (order.transferType === "supply" && REQUISITION_AFTER_RECEIPT.has(order.status ?? ""))
            || (isBorrowOrder(order) && BORROW_AFTER_RECEIPT.has(order.status ?? "")))
          .map(async (order) => [
            order.id,
            await api.get<Fulfillment>(`/api/v1/inventory/transfer-orders/${order.id}/dispositions`).catch(() => null),
          ] as const),
      );
      setOrders(withLines);
      setSupplyEntities(entityRows);
      setMerchants(merchantRows);
      setItems(itemRows);
      setWarehouses(warehouseRows);
      setFulfillments(new Map(fulfillmentRows));
      setArapIndex(
        arapRows === null
          ? null
          : {
              bySource: new Map(
                arapRows
                  .filter((row) => row.sourceType === "inventory_transfer" && row.sourceId)
                  .map((row) => [row.sourceId as string, row]),
              ),
              byId: new Map(arapRows.map((row) => [row.id, row])),
            },
      );
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const merchantName = React.useCallback(
    (id?: string | null) => {
      if (!id) return "—";
      const found = merchants.find((row) => row.id === id);
      return found?.displayName ?? found?.merchantName ?? found?.companyName ?? id;
    },
    [merchants],
  );

  const ownerName = React.useCallback(
    (ownerType?: string | null, ownerId?: string | null) => {
      if (!ownerId) return "—";
      if (ownerType === "supply_entity") {
        return supplyEntities.find((row) => row.id === ownerId)?.name ?? ownerId;
      }
      return merchantName(ownerId);
    },
    [merchantName, supplyEntities],
  );

  const itemName = React.useCallback(
    (id?: string | null) => {
      if (!id) return "—";
      const found = items.find((row) => row.id === id);
      return found?.itemName ?? found?.itemCode ?? id;
    },
    [items],
  );

  const postings = React.useMemo(
    () => new Map(orders.map((order) => [order.id, postingOf(order, arapIndex, fulfillments.get(order.id))])),
    [orders, arapIndex, fulfillments],
  );

  const visibleOrders = React.useMemo(() => {
    const focused = kpiFocus
      ? orders.filter((order) => matchesFocus(order, postings.get(order.id) ?? { kind: "unknown" }, kpiFocus))
      : orders;
    // 从别处下钻进来（TraceLink ?focus=）时只留那一单，省得仓管在 14 行里再找一遍。
    return focusObjectId ? focused.filter((order) => order.id === focusObjectId) : focused;
  }, [orders, postings, kpiFocus, focusObjectId]);

  const paged = usePagedRows(visibleOrders, 10, visibleOrders.length);
  const { selectedId, setSelectedId, selected } = useSelectedRow(visibleOrders);

  const counts = React.useMemo(() => {
    let pending = 0;
    let inTransit = 0;
    let received = 0;
    let postedCount = 0;
    let postedCent = 0;
    let missing = 0;
    for (const order of orders) {
      if (order.status === "created") pending += 1;
      if (order.status === "shipped") inTransit += 1;
      if (order.status === "received") received += 1;
      const posting = postings.get(order.id);
      if (posting?.kind === "posted") {
        postedCount += 1;
        postedCent += posting.arap?.amountCent ?? 0;
      }
      if (posting?.kind === "missing") missing += 1;
    }
    return { pending, inTransit, received, postedCount, postedCent, missing };
  }, [orders, postings]);

  function enterKpiFocus(focus: KpiFocus) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", focus);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(focus);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  async function runAction(action: string, path: string, body: Record<string, unknown> | undefined, actionName: string) {
    setSubmitting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.post(path, body ?? {});
      setActionResult({ action, path, result });
      setReceiveOpen(false);
      setCreateOpen(false);
      setReviewOpen(false);
      setCancelRequestOpen(false);
      setReceiptOpen(false);
      setDispositionOpen(false);
      setShipOpen(false);
      setReturnOpen(false);
      setUnreturnableOpen(false);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : `${actionName}失败`;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  function createValidation(): string | null {
    if (!editingId && !fromOwnerId) return "请选择出让方。";
    if (!editingId && !toOwnerId) return "请选择接收方（商户）。";
    if (!editingId && transferType === "lending" && fromOwnerId === toOwnerId) return "借调的出让方与接收方不能是同一个商户。";
    // BSM L1201「不可消费物品（设备、器具等）的借用必须约定归还时间」。
    if (!editingId && transferType === "lending" && lendingMode === "borrow" && !returnDueAt) return "请填写约定归还时间。";
    if (!itemId) return "请选择物料。";
    const qty = Number(quantity);
    if (!Number.isFinite(qty) || qty <= 0) return "数量必须大于 0。";
    const price = Number(unitPriceYuan);
    if (!Number.isFinite(price) || price < 0) return "结算单价不能为负数。";
    return null;
  }

  function formLine() {
    return {
      itemId,
      quantity: Number(quantity),
      unitPriceCent: Math.round(Number(unitPriceYuan) * 100),
      traceCodes: splitCodes(traceCodes),
    };
  }

  function openEdit() {
    const line = selected?.lines?.[0];
    if (!selected || !line) return;
    setFormError("");
    setItemId(line.itemId ?? "");
    setQuantity(String(line.quantity ?? 1));
    setUnitPriceYuan(String((line.unitPriceCent ?? 0) / 100));
    setTraceCodes((line.traceCodes ?? []).join(" "));
    setEditingId(selected.id);
    setCreateOpen(true);
  }

  function submitCreate() {
    if (editingId) {
      void runAction("inventory_transfer_order_draft_edited", `/api/v1/inventory/transfer-orders/${editingId}/lines`, { lines: [formLine()] }, "改单");
      return;
    }
    void runAction(
      "inventory_transfer_order_created",
      "/api/v1/inventory/transfer-orders",
      {
        transferType,
        lendingMode: transferType === "lending" ? lendingMode : null,
        fromOwnerType: transferType === "supply" ? "supply_entity" : "merchant",
        fromOwnerId,
        toOwnerType: "merchant",
        toOwnerId,
        operatorNote: operatorNote || null,
        lines: [formLine()],
        returnDueAt: transferType === "lending" && lendingMode === "borrow" ? new Date(returnDueAt).toISOString() : null,
      },
      "建单",
    );
  }

  const selectedCodes = selected?.lines?.flatMap((line) => line.traceCodes ?? []) ?? [];

  function receiveValidation(): string | null {
    if (!selected) return "请先选中一张流转单。";
    if (selectedCodes.length === 0) return null;
    const scanned = splitCodes(scannedCodes);
    if (scanned.length === 0) return `本单随货 ${selectedCodes.length} 个溯源码，收货必须逐码扫齐后再提交。`;
    if (new Set(scanned).size !== selectedCodes.length) {
      return `已录入 ${new Set(scanned).size} 个溯源码，与本单应扫的 ${selectedCodes.length} 个不一致，请核对实物后重扫。`;
    }
    return null;
  }

  const storeWarehouses = warehouses.filter((row) => row.merchantId === selected?.toOwnerId && (row.status ?? "active") === "active");
  const selectedFulfillment = selected ? fulfillments.get(selected.id) : undefined;

  // ── FS-019 借用：借出方交货扫码（借出时间取此刻）。 ──
  function shipValidation(): string | null {
    if (!selected) return "请先选中一张流转单。";
    const scanned = splitCodes(shipCodes);
    if (scanned.length === 0) return `本单随货 ${selectedCodes.length} 个溯源码，借出方交货必须逐码扫齐后再发货。`;
    if (new Set(scanned).size !== selectedCodes.length) {
      return `已录入 ${new Set(scanned).size} 个溯源码，与本单应扫的 ${selectedCodes.length} 个不一致，请核对实物后重扫。`;
    }
    return null;
  }

  function outstandingOf(line: TransferLine): number {
    const fact = selectedFulfillment?.lines?.find((row) => row.lineId === line.id);
    return fact?.outstandingQuantity ?? line.quantity ?? 0;
  }

  // ── FS-019 借用归还：借出方收货按行扫码/数量登记，可分多次。 ──
  function openReturn() {
    setFormError("");
    setReturnLines(Object.fromEntries((selected?.lines ?? []).map((line) => [line.id, { quantity: String(outstandingOf(line)), codes: "" }])));
    setReturnOpen(true);
  }

  function setReturnField(lineId: string, key: keyof ReturnInput, value: string) {
    setReturnLines((current) => ({ ...current, [lineId]: { ...(current[lineId] ?? { quantity: "0", codes: "" }), [key]: value } }));
  }

  function returnValidation(): string | null {
    let any = false;
    for (const line of selected?.lines ?? []) {
      const input = returnLines[line.id] ?? { quantity: "0", codes: "" };
      const quantityValue = Number(input.quantity);
      if (input.quantity.trim() === "" || !Number.isInteger(quantityValue) || quantityValue < 0) return `${itemName(line.itemId)}：归还数量须为不小于 0 的整数。`;
      if (quantityValue === 0) continue;
      any = true;
      if (quantityValue > outstandingOf(line)) return `${itemName(line.itemId)}：归还数量不能超过尚未归还的 ${outstandingOf(line)}。`;
      if ((line.traceCodes ?? []).length > 0 && new Set(splitCodes(input.codes)).size !== quantityValue) {
        return `${itemName(line.itemId)}：归还 ${quantityValue} 件须逐件扫码录入溯源码。`;
      }
    }
    return any ? null : "归还须按实物扫码/数量登记，至少一行数量大于 0。";
  }

  function submitReturn() {
    void runAction("inventory_transfer_order_returned", orderPath("return"), {
      lines: (selected?.lines ?? [])
        .map((line) => ({ line, input: returnLines[line.id] ?? { quantity: "0", codes: "" } }))
        .filter(({ input }) => Number(input.quantity) > 0)
        .map(({ line, input }) => ({ lineId: line.id, returnedQuantity: Number(input.quantity), traceCodes: splitCodes(input.codes) })),
    }, "归还");
  }

  // ── FS-019 无法归还：双方确认、主管与财务复核、赔偿应付、实物处置完成。 ──
  function openUnreturnable() {
    const line = selected?.lines?.[0];
    setFormError("");
    setProofs([]);
    setUnreturnable({ ...EMPTY_UNRETURNABLE, lineId: line?.id ?? "", amountYuan: line ? String((line.unitPriceCent ?? 0) / 100) : "" });
    setUnreturnableOpen(true);
  }

  function setUnreturnableField(key: keyof typeof EMPTY_UNRETURNABLE) {
    return (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      const value = event.target.value;
      setUnreturnable((current) => {
        const next = { ...current, [key]: value };
        // 赔偿缺省＝数量×建单锁定内部结算价（DEC-R3-38），改数量或明细时跟着算，手改后以手改为准。
        if (key === "quantity" || key === "lineId") {
          const line = selected?.lines?.find((row) => row.id === next.lineId);
          next.amountYuan = String(((line?.unitPriceCent ?? 0) * (Number(next.quantity) || 0)) / 100);
        }
        return next;
      });
    };
  }

  function unreturnableValidation(): string | null {
    const line = selected?.lines?.find((row) => row.id === unreturnable.lineId);
    if (!line) return "请选择明细。";
    const quantityValue = Number(unreturnable.quantity);
    if (!Number.isInteger(quantityValue) || quantityValue <= 0) return "无法归还数量须为大于 0 的整数。";
    if (quantityValue > outstandingOf(line)) return `无法归还数量不能超过尚未归还的 ${outstandingOf(line)}。`;
    if ((line.traceCodes ?? []).length > 0 && new Set(splitCodes(unreturnable.codes)).size !== quantityValue) return `须逐件列出无法归还 ${quantityValue} 件的溯源码。`;
    const amount = Number(unreturnable.amountYuan);
    if (!Number.isFinite(amount) || Math.round(amount * 100) < (line.unitPriceCent ?? 0) * quantityValue || amount <= 0) return "赔偿不得低于内部结算价。";
    if (!unreturnable.reason.trim()) return "请写明无法归还的原因。";
    if (proofs.length === 0) return "须上传证据（照片、录像等）后再登记。";
    return null;
  }

  function submitUnreturnable() {
    void runAction("inventory_transfer_unreturnable_registered", orderPath("unreturnable"), {
      lineId: unreturnable.lineId,
      quantity: Number(unreturnable.quantity),
      traceCodes: splitCodes(unreturnable.codes),
      reason: unreturnable.reason.trim(),
      amountCent: Math.round(Number(unreturnable.amountYuan) * 100),
      disposalNote: unreturnable.disposalNote.trim() || null,
      evidenceAttachmentIds: proofs.map((proof) => proof.id),
    }, "登记无法归还");
  }

  /** 门店要货待签收走按行签收；借调仍扫码收货。 */
  function openReceive() {
    setFormError("");
    if (selected?.status !== "pending_receipt") {
      setScannedCodes("");
      setReceiveOpen(true);
      return;
    }
    setReceiptWarehouse(storeWarehouses[0]?.id ?? "");
    setReceiptLocation(selected.toLocationId ?? "");
    setReceiptLines(Object.fromEntries((selected.lines ?? []).map((line) => [
      line.id,
      { arrived: String(line.quantity ?? 0), accepted: String(line.quantity ?? 0), rejected: "0", codes: "" },
    ])));
    setReceiptOpen(true);
  }

  function setReceiptField(lineId: string, key: keyof ReceiptInput, value: string) {
    setReceiptLines((current) => ({ ...current, [lineId]: { ...(current[lineId] ?? EMPTY_RECEIPT_INPUT), [key]: value } }));
  }

  // BSM L1212「签收分别记录实收、合格接收、拒收和短少数量」：实收＝合格＋拒收，不超过发货；短少＝发货－实收由后端算出。
  function receiptValidation(): string | null {
    if (!receiptWarehouse.trim()) return "请选择收货仓库。";
    if (!receiptLocation.trim()) return "请填写收货库位。";
    for (const line of selected?.lines ?? []) {
      const input = receiptLines[line.id] ?? EMPTY_RECEIPT_INPUT;
      const arrived = Number(input.arrived);
      const accepted = Number(input.accepted);
      const rejected = Number(input.rejected);
      if (![input.arrived, input.accepted, input.rejected].every((raw) => raw.trim() !== "" && Number.isInteger(Number(raw)) && Number(raw) >= 0)) {
        return `${itemName(line.itemId)}：实收、合格接收、拒收须为不小于 0 的整数。`;
      }
      if (accepted + rejected !== arrived) return `${itemName(line.itemId)}：实收须等于合格接收加拒收。`;
      if (arrived > (line.quantity ?? 0)) return `${itemName(line.itemId)}：实收不能超过发货数量 ${line.quantity ?? 0}。`;
      if ((line.traceCodes ?? []).length > 0 && new Set(splitCodes(input.codes)).size !== accepted) {
        return `${itemName(line.itemId)}：合格接收 ${accepted} 件须逐件扫码录入溯源码。`;
      }
    }
    return null;
  }

  function submitReceipt() {
    void runAction("inventory_transfer_order_received", orderPath("receive"), {
      warehouseId: receiptWarehouse.trim(),
      locationId: receiptLocation.trim(),
      lines: (selected?.lines ?? []).map((line) => {
        const input = receiptLines[line.id] ?? EMPTY_RECEIPT_INPUT;
        return {
          lineId: line.id,
          arrivedQuantity: Number(input.arrived),
          acceptedQuantity: Number(input.accepted),
          rejectedQuantity: Number(input.rejected),
          acceptedTraceCodes: splitCodes(input.codes),
        };
      }),
    }, "签收");
  }

  function openDisposition() {
    setFormError("");
    setProofs([]);
    setDisposition({ ...EMPTY_DISPOSITION, lineId: selectedFulfillment?.lines?.[0]?.lineId ?? "" });
    setDispositionOpen(true);
  }

  function setDispositionField(key: keyof typeof EMPTY_DISPOSITION) {
    return (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      const value = event.target.value;
      setDisposition((current) => ({ ...current, [key]: value }));
    };
  }

  // BSM L1212「供货、配送保管和收货方按交接证据确认差异」：证据先上传并绑定本要货单。
  async function uploadProofs(files: FileList | null) {
    if (!files?.length || !selected) return;
    if (!api.postForm) {
      setFormError("当前环境不支持上传交接证据。");
      return;
    }
    setUploading(true);
    setFormError("");
    try {
      for (const file of Array.from(files)) {
        const data = new FormData();
        data.append("file", file);
        data.append("business_type", "inventory_transfer_order");
        data.append("business_id", selected.id);
        data.append("attachment_role", "other");
        const result = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", data);
        setProofs((current) => [...current, { id: result.id, name: file.name }]);
      }
    } catch (uploadError) {
      setFormError(uploadError instanceof Error ? uploadError.message : "交接证据上传失败，已上传的文件已保留");
    } finally {
      setUploading(false);
    }
  }

  function dispositionValidation(): string | null {
    if (disposition.type === "payment_adjustment") {
      const amount = Number(disposition.amountYuan);
      if (!Number.isFinite(amount) || amount <= 0) return "请填写大于 0 的调减金额。";
    } else {
      if (!disposition.lineId) return "请选择明细。";
      const quantityValue = Number(disposition.quantity);
      if (!Number.isInteger(quantityValue) || quantityValue <= 0) return "处理数量须为大于 0 的整数。";
    }
    if (!disposition.reason.trim()) return "请写明差异原因与处理依据。";
    if (proofs.length === 0) return "须上传交接证据（交接单、照片等）后再登记。";
    return null;
  }

  function submitDisposition() {
    const body: Record<string, unknown> = { dispositionType: disposition.type };
    if (disposition.type === "payment_adjustment") {
      body.amountCent = Math.round(Number(disposition.amountYuan) * 100);
    } else {
      body.lineId = disposition.lineId;
      body.quantity = Number(disposition.quantity);
    }
    if (disposition.type === "loss") body.responsibleParty = disposition.responsible;
    if (disposition.type === "reship") body.traceCodes = splitCodes(disposition.codes);
    body.reason = disposition.reason.trim();
    body.evidenceAttachmentIds = proofs.map((proof) => proof.id);
    void runAction("inventory_transfer_disposition_registered", orderPath("dispositions"), body, "登记差异处理");
  }

  function reviewDisposition(id: string, decision: "approved" | "rejected") {
    if (!disposition.reviewReason.trim()) {
      setFormError("请写明复核意见。");
      return;
    }
    void runAction("inventory_transfer_disposition_reviewed", orderPath(`dispositions/${id}/review`), { decision, reason: disposition.reviewReason.trim() }, "复核");
  }

  function openDetail(order: TransferOrder) {
    const posting = postings.get(order.id) ?? { kind: "unknown" as PostingKind };
    const amount = orderAmountCent(order);
    const fields: RowDetailField[] = [
      { label: "流转类型", value: label(TRANSFER_TYPE_LABELS, order.transferType) },
      { label: "借调方式", value: order.lendingMode ? label(LENDING_MODE_LABELS, order.lendingMode) : "—" },
      {
        label: "出让方",
        value: `${label(OWNER_TYPE_LABELS, order.fromOwnerType)}：${ownerName(order.fromOwnerType, order.fromOwnerId)}`,
      },
      {
        label: "接收方",
        value: `${label(OWNER_TYPE_LABELS, order.toOwnerType)}：${ownerName(order.toOwnerType, order.toOwnerId)}`,
      },
      { label: "出货库位", value: order.fromLocationId ?? "—" },
      { label: "收货库位", value: order.toLocationId ?? "—" },
      { label: "单据状态", value: label(TRANSFER_STATUS_LABELS, order.status) },
      ...(isBorrowOrder(order)
        ? [
            { label: "所有权方", value: `始终为借出方：${merchantName(order.fromOwnerId)}` },
            { label: "当前保管方", value: merchantName(custodianOf(order)) },
            { label: "应归还期限", value: cnDate(order.returnDueAt ?? undefined) },
          ]
        : []),
      { label: "货值合计", value: amount === null ? "—" : yuan(amount) },
      { label: "建单时间", value: cnDate(order.createdAt) },
      { label: "最近状态变更时间", value: cnDate(order.updatedAt) },
      { label: "建单人", value: order.createdBy ?? "—" },
      { label: "备注", value: order.operatorNote ?? "—" },
      { label: "流转单号", value: order.id },
    ];
    for (const line of order.lines ?? []) {
      fields.push({
        label: `明细 · ${itemName(line.itemId)}`,
        value: `数量 ${line.quantity ?? 0} · 结算单价 ${yuan(line.unitPriceCent)} · 小计 ${yuan((line.quantity ?? 0) * (line.unitPriceCent ?? 0))}`,
      });
      fields.push({
        label: "随货溯源码",
        value: (line.traceCodes ?? []).length > 0 ? (line.traceCodes ?? []).join("、") : "本明细无随货溯源码",
      });
    }
    fields.push({ label: "应付挂账", value: POSTING_LABELS[posting.kind] });
    if (posting.arap) {
      fields.push({
        label: "应付凭证",
        value: (
          <TraceLink navigateTo={navigateTo} pageId="ADM-FIN-001" objectId={posting.arap.id}>
            {posting.arap.itemNo ?? posting.arap.id}
          </TraceLink>
        ),
      });
      // 凭证状态与已付/未付必须同屏：付款是否结清另行显示（BSM L1212），不并入单据状态。
      fields.push({ label: "凭证状态", value: formatBusinessStatus(posting.arap.status) });
      fields.push({ label: "挂账金额", value: yuan(posting.arap.amountCent) });
      fields.push({ label: "已付 / 未付", value: `${yuan(posting.arap.paidAmountCent)} / ${yuan(posting.arap.unpaidAmountCent)}` });
      fields.push({ label: "挂账时间", value: cnDate(posting.arap.createdAt) });
    }
    setDetail({ title: `流转单明细 · ${order.id}`, fields });
  }

  const selectedPosting = selected ? postings.get(selected.id) : undefined;
  const isRequisition = selected?.transferType === "supply";
  const canSubmit = isRequisition && selected?.status === "draft";
  const canReview = isRequisition && selected?.status === "pending_supplier_review";
  const canPick = isRequisition && selected?.status === "pending_picking";
  const canShip = isRequisition ? selected?.status === "pending_outbound" : selected?.status === "created";
  const canDeliver = isRequisition && selected?.status === "in_delivery";
  const canReceive = selected?.status === "shipped" || (isRequisition && selected?.status === "pending_receipt");
  const canReturn = selected?.status === "received" && selected.transferType === "lending" && selected.lendingMode === "borrow";
  // BSM L1212「已出库数量须实际退回验收，或经复核完成损失处理，不能仅点取消就回库」：只有出库前能直接取消。
  const canCancel = ["created", "draft"].includes(selected?.status ?? "");
  const canDispose = isRequisition && selected?.status === "discrepancy" && Boolean(selectedFulfillment);
  const canRequestCancel = isRequisition && REQUISITION_CANCEL_NEEDS_APPROVAL.has(selected?.status ?? "");
  const orderPath = (action: string) => `/api/v1/inventory/transfer-orders/${selected?.id}/${action}`;

  return (
    <section data-testid="inventory-transfer-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails>
        <div className="admin-generic-evidence" data-testid="inventory-transfer-evidence">{TEXT.evidence}</div>
      </TechDetails>
      <AuditTrailHint event="inventory.transfer_order_created / inventory.transfer_order_submitted / inventory.transfer_order_supplier_reviewed / inventory.transfer_order_picked / inventory.transfer_order_shipped / inventory.transfer_order_delivered / inventory.transfer_order_received / inventory.transfer_disposition_registered / inventory.transfer_disposition_reviewed / inventory.transfer_order_returned / inventory.transfer_unreturnable_registered / inventory.transfer_unreturnable_confirmed / inventory.transfer_disposal_completed / inventory.transfer_order_cancelled" />
      <AdminDataState loading={loading} error={error} empty={orders.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <div className="admin-state" data-testid="inventory-transfer-kpi-focus-banner">
            正在查看：{KPI_FOCUS_TITLES[kpiFocus]}（{visibleOrders.length}）
            <button type="button" data-testid="inventory-transfer-kpi-focus-clear" onClick={exitKpiFocus}>清除筛选，返回全部</button>
          </div>
        ) : (
          <div className="admin-kpi-grid" data-testid="inventory-transfer-summary">
            <div className="admin-kpi-card admin-kpi-card--clickable">
              <span>待发货</span>
              <button type="button" className="admin-kpi-value" data-testid="inventory-transfer-kpi-pending" onClick={() => enterKpiFocus("pending")}>{counts.pending}</button>
              <small>已建单，仓库还没发出</small>
            </div>
            <div className="admin-kpi-card admin-kpi-card--clickable">
              <span>在途待收货</span>
              <button type="button" className="admin-kpi-value" data-testid="inventory-transfer-kpi-in-transit" onClick={() => enterKpiFocus("in_transit")}>{counts.inTransit}</button>
              <small>货已发出，接收方还没扫码确认</small>
            </div>
            <div className="admin-kpi-card admin-kpi-card--clickable">
              <span>已收货</span>
              <button type="button" className="admin-kpi-value" data-testid="inventory-transfer-kpi-received" onClick={() => enterKpiFocus("received")}>{counts.received}</button>
              <small>溯源码归属已过户到接收方</small>
            </div>
            <div className="admin-kpi-card admin-kpi-card--clickable">
              <span>已挂应付金额</span>
              <button type="button" className="admin-kpi-value" data-testid="inventory-transfer-kpi-posted" onClick={() => enterKpiFocus("posted")}>{yuan(counts.postedCent)}</button>
              <small>{arapIndex === null ? "无财务台账查看权限，未核对" : `${counts.postedCount} 笔已进应收应付台账`}</small>
            </div>
            <div className="admin-kpi-card admin-kpi-card--clickable">
              <span>已合格签收·账未挂</span>
              <button type="button" className="admin-kpi-value" data-testid="inventory-transfer-kpi-missing" onClick={() => enterKpiFocus("missing")}>{counts.missing}</button>
              <small>已合格签收（或借调已收货）却没挂应付，需财务核实</small>
            </div>
          </div>
        )}

        <div className="admin-form-row" data-testid="inventory-transfer-actions">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            data-testid="inventory-transfer-create-open"
            onClick={() => { setFormError(""); setEditingId(null); setCreateOpen(true); }}
          >
            ＋ 新建流转单
          </PermissionButton>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={!canSubmit}
            data-testid="inventory-transfer-edit-open"
            onClick={openEdit}
          >
            修改草稿
          </PermissionButton>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={submitting || !canSubmit}
            data-testid="inventory-transfer-submit"
            onClick={() => void runAction("inventory_transfer_order_submitted", orderPath("submit"), undefined, "提交")}
          >
            提交供货主体审核
          </PermissionButton>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={!canReview}
            data-testid="inventory-transfer-review-open"
            onClick={() => { setFormError(""); setReviewDecision("approved"); setReviewReason(""); setReviewOpen(true); }}
          >
            供货主体审核
          </PermissionButton>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={submitting || !canPick}
            data-testid="inventory-transfer-pick"
            onClick={() => void runAction("inventory_transfer_order_picked", orderPath("pick"), undefined, "拣配")}
          >
            拣配完成
          </PermissionButton>
          <HighRiskConfirm
            label="发货"
            impact="货物出库，单据进入配送中（门店要货须先经供货主体审核和拣配）；货款在接收方签收合格后才挂应付，借用型不计费不挂账。"
            disabled={submitting || !canShip}
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            data-testid="inventory-transfer-ship"
            precheck={() => {
              // FS-019：借用单有随货溯源码时，借出方交货须逐码扫齐（借出时间取此刻），改走扫码弹窗。
              if (isBorrowOrder(selected) && selectedCodes.length > 0) {
                setFormError("");
                setShipCodes("");
                setShipOpen(true);
                return false;
              }
              return true;
            }}
            onConfirm={() => selected && void runAction("inventory_transfer_order_shipped", `/api/v1/inventory/transfer-orders/${selected.id}/ship`, undefined, "发货")}
          />
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={submitting || !canDeliver}
            data-testid="inventory-transfer-deliver"
            onClick={() => void runAction("inventory_transfer_order_delivered", orderPath("deliver"), undefined, "送达")}
          >
            送达待签收
          </PermissionButton>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={!canReceive}
            data-testid="inventory-transfer-receive-open"
            onClick={openReceive}
          >
            收货 / 签收
          </PermissionButton>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={!canDispose}
            data-testid="inventory-transfer-disposition-open"
            onClick={openDisposition}
          >
            差异处理
          </PermissionButton>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={submitting || !canReturn}
            data-testid="inventory-transfer-return"
            onClick={openReturn}
          >
            归还（借出方收货扫码）
          </PermissionButton>
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={!canReturn}
            data-testid="inventory-transfer-unreturnable-open"
            onClick={openUnreturnable}
          >
            无法归还 / 处置
          </PermissionButton>
          <HighRiskConfirm
            label="取消单据"
            impact="取消草稿或未发货的借调单；已提交未出库的门店要货单须走「申请取消」审批；已出库的不能直接取消，须经差异处理实际退回验收或复核完成损失处理。"
            disabled={submitting || !canCancel}
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            data-testid="inventory-transfer-cancel"
            onConfirm={() => selected && void runAction("inventory_transfer_order_cancelled", `/api/v1/inventory/transfer-orders/${selected.id}/cancel`, undefined, "取消")}
          />
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={!canRequestCancel}
            data-testid="inventory-transfer-cancel-request-open"
            onClick={() => { setFormError(""); setCancelReason(""); setCancelRequestOpen(true); }}
          >
            申请取消（须审批）
          </PermissionButton>
          <span className="admin-muted-action" data-testid="inventory-transfer-hint">
            {selected
              ? `已选中 ${selected.id}（${label(TRANSFER_STATUS_LABELS, selected.status)}·${POSTING_LABELS[selectedPosting?.kind ?? "unknown"]}）`
              : "先在下表选中一张流转单，再执行发货 / 收货 / 归还 / 取消。"}
          </span>
        </div>
        {formError ? <div data-testid="inventory-transfer-error" className="admin-inline-error">{formError}</div> : null}

        <FormModal
          open={createOpen}
          title={editingId ? "修改要货草稿" : "新建跨主体流转单"}
          description={editingId
            ? "供货主体退回或尚未提交的要货草稿，改好后再提交供货主体审核。"
            : "平台供货公司送货给门店选「平台供货调拨」（门店要货，建单为草稿，提交后经供货主体审核、拣配、出库、配送、签收）；商户之间的货选「商户间借调」，并区分借用（约定归还）还是买断。一张单先录一条明细，签收后按货款挂应付。"}
          testId="inventory-transfer-create"
          submitTestId="inventory-transfer-create-submit"
          submitLabel={editingId ? "保存草稿" : "建单"}
          submitting={submitting}
          error={formError || undefined}
          validate={createValidation}
          onClose={() => setCreateOpen(false)}
          onSubmit={submitCreate}
        >
          {editingId ? null : (
          <>
          <label>流转类型
            <select data-testid="inventory-transfer-create-type" value={transferType} onChange={(event) => { setTransferType(event.target.value); setFromOwnerId(""); }}>
              <option value="supply">平台供货调拨（平台供货公司 → 商户）</option>
              <option value="lending">商户间借调（商户 → 商户）</option>
            </select>
          </label>
          {transferType === "lending" ? (
            <label>借调方式
              <select data-testid="inventory-transfer-create-mode" value={lendingMode} onChange={(event) => setLendingMode(event.target.value)}>
                <option value="borrow">借用（约定归还，默认不计占用费）</option>
                <option value="purchase">买断（不归还，按结算价付款）</option>
              </select>
            </label>
          ) : null}
          {transferType === "lending" && lendingMode === "borrow" ? (
            <label>约定归还时间（借用只限不可消费物品，必须约定归还时间）
              <input data-testid="inventory-transfer-create-return-due" type="datetime-local" value={returnDueAt} onChange={(event) => setReturnDueAt(event.target.value)} />
            </label>
          ) : null}
          <label>出让方
            <select data-testid="inventory-transfer-create-from" value={fromOwnerId} onChange={(event) => setFromOwnerId(event.target.value)}>
              <option value="">请选择</option>
              {(transferType === "supply"
                ? supplyEntities.map((row) => ({ id: row.id, name: row.name ?? row.id }))
                : merchants.map((row) => ({ id: row.id, name: merchantName(row.id) }))
              ).map((option) => <option key={option.id} value={option.id}>{option.name}</option>)}
            </select>
          </label>
          <label>接收方（商户）
            <select data-testid="inventory-transfer-create-to" value={toOwnerId} onChange={(event) => setToOwnerId(event.target.value)}>
              <option value="">请选择</option>
              {merchants.map((row) => <option key={row.id} value={row.id}>{merchantName(row.id)}</option>)}
            </select>
          </label>
          </>
          )}
          <label>物料
            <select data-testid="inventory-transfer-create-item" value={itemId} onChange={(event) => setItemId(event.target.value)}>
              <option value="">请选择</option>
              {items.map((row) => <option key={row.id} value={row.id}>{itemName(row.id)}</option>)}
            </select>
          </label>
          <label>数量<input data-testid="inventory-transfer-create-quantity" type="number" min="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} /></label>
          <label>结算单价（元）<input data-testid="inventory-transfer-create-price" type="number" min="0" step="0.01" value={unitPriceYuan} onChange={(event) => setUnitPriceYuan(event.target.value)} /></label>
          <label>随货溯源码（多个用空格或逗号分隔，可留空）<input data-testid="inventory-transfer-create-codes" value={traceCodes} onChange={(event) => setTraceCodes(event.target.value)} /></label>
          <label>备注<input data-testid="inventory-transfer-create-note" value={operatorNote} onChange={(event) => setOperatorNote(event.target.value)} /></label>
        </FormModal>

        <FormModal
          open={shipOpen}
          title="借出方交货扫码"
          description={`本单随货 ${selectedCodes.length} 个溯源码，借出方交货须逐个扫描/录入后发货；借出时间取此次扫码时刻，不设手工填写。`}
          testId="inventory-transfer-ship-scan"
          submitTestId="inventory-transfer-ship-scan-submit"
          submitLabel="确认发货"
          submitting={submitting}
          error={formError || undefined}
          validate={shipValidation}
          onClose={() => setShipOpen(false)}
          onSubmit={() => void runAction("inventory_transfer_order_shipped", orderPath("ship"), { scannedTraceCodes: splitCodes(shipCodes) }, "发货")}
        >
          <label>扫描到的溯源码（多个用空格或逗号分隔）
            <input data-testid="inventory-transfer-ship-scan-codes" value={shipCodes} onChange={(event) => setShipCodes(event.target.value)} />
          </label>
        </FormModal>

        <FormModal
          open={returnOpen}
          title="借用归还（借出方收货扫码）"
          description="借出方收到实物时按行登记归还数量，有随货溯源码的逐件扫码；可分多次归还，全部数量归还或实物处置完成后借用结束。实际归还时间取本次扫码时刻；所有权始终在借出方，只是保管责任回到借出方。"
          testId="inventory-transfer-return-form"
          submitTestId="inventory-transfer-return-submit"
          submitLabel="确认归还"
          submitting={submitting}
          error={formError || undefined}
          validate={returnValidation}
          onClose={() => setReturnOpen(false)}
          onSubmit={submitReturn}
        >
          {(selected?.lines ?? []).map((line) => {
            const input = returnLines[line.id] ?? { quantity: "0", codes: "" };
            return (
              <fieldset key={line.id} data-testid={`inventory-transfer-return-line-${line.id}`}>
                <legend>{itemName(line.itemId)} · 借出 {line.quantity ?? 0} · 尚未归还 {outstandingOf(line)}</legend>
                <label>本次归还数量<input data-testid={`inventory-transfer-return-quantity-${line.id}`} type="number" min="0" value={input.quantity} onChange={(event) => setReturnField(line.id, "quantity", event.target.value)} /></label>
                {(line.traceCodes ?? []).length > 0 ? (
                  <label>归还件溯源码（逐件扫码，空格或逗号分隔）
                    <input data-testid={`inventory-transfer-return-codes-${line.id}`} value={input.codes} onChange={(event) => setReturnField(line.id, "codes", event.target.value)} />
                  </label>
                ) : null}
              </fieldset>
            );
          })}
        </FormModal>

        <FormModal
          open={unreturnableOpen}
          title="无法归还（损坏/丢失）"
          description="借出方或借入方任一方登记原因、数量、实物处置方式并附证据；另一方确认后进主管复核，再由财务复核赔偿金额（缺省按数量×建单锁定内部结算价），通过后形成借入方对借出方的应付、单独跟进清偿；借出方登记实物处置实际完成后，才结束对应数量的归还事项。借入方单方报废不构成归还。"
          testId="inventory-transfer-unreturnable"
          submitTestId="inventory-transfer-unreturnable-submit"
          submitLabel="登记，待对方确认"
          submitting={submitting || uploading}
          error={formError || undefined}
          validate={unreturnableValidation}
          onClose={() => setUnreturnableOpen(false)}
          onSubmit={submitUnreturnable}
        >
          <ul data-testid="inventory-transfer-unreturnable-list">
            {(selectedFulfillment?.dispositions ?? []).map((row) => (
              <li key={row.id} data-testid={`inventory-transfer-unreturnable-row-${row.id}`}>
                {label(DISPOSITION_LABELS, row.dispositionType)}
                {row.quantity ? ` ${row.quantity} 件` : ""}
                {(row.traceCodes ?? []).length > 0 ? ` · ${(row.traceCodes ?? []).join("、")}` : ""}
                {row.amountCent ? ` · 赔偿 ${yuan(row.amountCent)}` : " · 不赔偿"}
                {` · ${row.reason ?? ""} · ${label(DISPOSITION_STATUS_LABELS, row.status)}`}
                {row.disposalNote ? ` · 处置：${row.disposalNote}` : ""}
                {row.payableItemId ? ` · 应付 ${row.payableItemId}（单独跟进）` : ""}
                {row.status === "pending_counterparty" ? (
                  <PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE_PERMISSION]} disabled={submitting} data-testid={`inventory-transfer-unreturnable-confirm-${row.id}`} onClick={() => void runAction("inventory_transfer_unreturnable_confirmed", orderPath(`dispositions/${row.id}/confirm`), { operatorNote: unreturnable.reason.trim() || null }, "对方确认")}>对方确认，提交主管复核</PermissionButton>
                ) : null}
                {row.status === "pending_review" ? (
                  <>
                    <PermissionButton permissionsState={effectivePermissions} permissions={[FINANCE_REVIEW_PERMISSION]} disabled={submitting} data-testid={`inventory-transfer-disposition-approve-${row.id}`} onClick={() => reviewDisposition(row.id, "approved")}>财务复核通过</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={[FINANCE_REVIEW_PERMISSION]} disabled={submitting} data-testid={`inventory-transfer-disposition-reject-${row.id}`} onClick={() => reviewDisposition(row.id, "rejected")}>驳回</PermissionButton>
                  </>
                ) : null}
                {row.status === "approved" ? (
                  <PermissionButton permissionsState={effectivePermissions} permissions={[MANAGE_PERMISSION]} disabled={submitting} data-testid={`inventory-transfer-disposal-complete-${row.id}`} onClick={() => void runAction("inventory_transfer_disposal_completed", orderPath(`dispositions/${row.id}/disposal-completed`), { operatorNote: unreturnable.disposalNote.trim() || null }, "登记实物处置完成")}>借出方登记实物处置完成</PermissionButton>
                ) : null}
              </li>
            ))}
          </ul>
          <label>复核意见（财务复核待复核项时填写）<input data-testid="inventory-transfer-unreturnable-review-reason" value={disposition.reviewReason} onChange={setDispositionField("reviewReason")} /></label>
          <label>明细
            <select data-testid="inventory-transfer-unreturnable-line" value={unreturnable.lineId} onChange={setUnreturnableField("lineId")}>
              {(selected?.lines ?? []).map((line) => <option key={line.id} value={line.id}>{itemName(line.itemId)} · 尚未归还 {outstandingOf(line)}</option>)}
            </select>
          </label>
          <label>无法归还数量<input data-testid="inventory-transfer-unreturnable-quantity" type="number" min="1" value={unreturnable.quantity} onChange={setUnreturnableField("quantity")} /></label>
          {(selected?.lines?.find((row) => row.id === unreturnable.lineId)?.traceCodes ?? []).length > 0 ? (
            <label>无法归还件的溯源码（逐件列出）<input data-testid="inventory-transfer-unreturnable-codes" value={unreturnable.codes} onChange={setUnreturnableField("codes")} /></label>
          ) : null}
          <label>原因（损坏/丢失经过）<input data-testid="inventory-transfer-unreturnable-reason" value={unreturnable.reason} onChange={setUnreturnableField("reason")} /></label>
          <label>赔偿金额（元，缺省＝数量×内部结算价）<input data-testid="inventory-transfer-unreturnable-amount" type="number" min="0" step="0.01" value={unreturnable.amountYuan} onChange={setUnreturnableField("amountYuan")} /></label>
          <label>实物处置方式<input data-testid="inventory-transfer-unreturnable-disposal" value={unreturnable.disposalNote} onChange={setUnreturnableField("disposalNote")} /></label>
          <label>证据（照片、录像等）
            <input data-testid="inventory-transfer-unreturnable-evidence" type="file" multiple disabled={uploading} onChange={(event) => { void uploadProofs(event.target.files); event.target.value = ""; }} />
          </label>
          <ul>{proofs.map((proof) => <li key={proof.id}>{proof.name}（已上传）</li>)}</ul>
        </FormModal>

        <FormModal
          open={receiveOpen}
          title="扫码收货"
          description={
            selectedCodes.length > 0
              ? `本单随货 ${selectedCodes.length} 个溯源码，请逐个扫描/录入后提交；数量或内容对不上会被后端拒收，防止中途调包。`
              : "本单没有随货溯源码，直接确认收货即可。"
          }
          testId="inventory-transfer-receive"
          submitTestId="inventory-transfer-receive-submit"
          submitLabel="确认收货"
          submitting={submitting}
          error={formError || undefined}
          validate={receiveValidation}
          onClose={() => setReceiveOpen(false)}
          onSubmit={() => selected && void runAction(
            "inventory_transfer_order_received",
            `/api/v1/inventory/transfer-orders/${selected.id}/receive`,
            { scannedTraceCodes: splitCodes(scannedCodes) },
            "收货",
          )}
        >
          <label>扫描到的溯源码（多个用空格或逗号分隔）
            <input data-testid="inventory-transfer-receive-codes" value={scannedCodes} onChange={(event) => setScannedCodes(event.target.value)} />
          </label>
        </FormModal>

        <FormModal
          open={receiptOpen}
          title="门店要货签收"
          description="逐行登记实收、合格接收、拒收；短少＝发货－实收。合格部分转入本门店货权与库存，并按锁定内部结算价形成货款；拒收与短少的件签收后经差异处理退回验收或复核损失。"
          testId="inventory-transfer-receipt"
          submitTestId="inventory-transfer-receipt-submit"
          submitLabel="确认签收"
          submitting={submitting}
          error={formError || undefined}
          validate={receiptValidation}
          onClose={() => setReceiptOpen(false)}
          onSubmit={submitReceipt}
        >
          <label>收货仓库
            {storeWarehouses.length > 0 ? (
              <select data-testid="inventory-transfer-receipt-warehouse" value={receiptWarehouse} onChange={(event) => setReceiptWarehouse(event.target.value)}>
                {storeWarehouses.map((row) => <option key={row.id} value={row.id}>{row.name ?? row.id}</option>)}
              </select>
            ) : (
              <input data-testid="inventory-transfer-receipt-warehouse" placeholder="本门店仓库编号" value={receiptWarehouse} onChange={(event) => setReceiptWarehouse(event.target.value)} />
            )}
          </label>
          <label>收货库位<input data-testid="inventory-transfer-receipt-location" value={receiptLocation} onChange={(event) => setReceiptLocation(event.target.value)} /></label>
          {(selected?.lines ?? []).map((line) => {
            const input = receiptLines[line.id] ?? EMPTY_RECEIPT_INPUT;
            return (
              <fieldset key={line.id} data-testid={`inventory-transfer-receipt-line-${line.id}`}>
                <legend>{itemName(line.itemId)} · 发货 {line.quantity ?? 0}</legend>
                <label>实收<input data-testid={`inventory-transfer-receipt-arrived-${line.id}`} type="number" min="0" value={input.arrived} onChange={(event) => setReceiptField(line.id, "arrived", event.target.value)} /></label>
                <label>合格接收<input data-testid={`inventory-transfer-receipt-accepted-${line.id}`} type="number" min="0" value={input.accepted} onChange={(event) => setReceiptField(line.id, "accepted", event.target.value)} /></label>
                <label>拒收<input data-testid={`inventory-transfer-receipt-rejected-${line.id}`} type="number" min="0" value={input.rejected} onChange={(event) => setReceiptField(line.id, "rejected", event.target.value)} /></label>
                <span>短少 <b data-testid={`inventory-transfer-receipt-short-${line.id}`}>{Math.max(0, (line.quantity ?? 0) - (Number(input.arrived) || 0))}</b></span>
                {(line.traceCodes ?? []).length > 0 ? (
                  <label>合格件溯源码（逐件扫码，空格或逗号分隔）
                    <input data-testid={`inventory-transfer-receipt-codes-${line.id}`} value={input.codes} onChange={(event) => setReceiptField(line.id, "codes", event.target.value)} />
                  </label>
                ) : null}
              </fieldset>
            );
          })}
        </FormModal>

        <FormModal
          open={dispositionOpen}
          title="差异处理"
          description="拒收与短少的每一件须经退回验收或复核损失有了结果，要货单才完成；补发另开关联要货单照常出库签收；货款调减按原账冲正重述、不改原账。每项登记须附交接证据，由财务复核（登记人不能自审）后生效。"
          testId="inventory-transfer-disposition"
          submitTestId="inventory-transfer-disposition-submit"
          submitLabel="登记，提交财务复核"
          submitting={submitting || uploading}
          error={formError || undefined}
          validate={dispositionValidation}
          onClose={() => setDispositionOpen(false)}
          onSubmit={submitDisposition}
        >
          <table className="admin-table" data-testid="inventory-transfer-disposition-lines">
            <thead><tr><th>物料</th><th>发货</th><th>合格</th><th>拒收</th><th>短少</th><th>已有结果</th><th>待处理</th></tr></thead>
            <tbody>
              {(selectedFulfillment?.lines ?? []).map((line) => (
                <tr key={line.lineId}>
                  <td>{itemName(line.itemId)}</td>
                  <td>{line.quantity ?? 0}</td>
                  <td>{line.acceptedQuantity ?? 0}</td>
                  <td>{line.rejectedQuantity ?? 0}</td>
                  <td>{line.shortQuantity ?? 0}</td>
                  <td>{line.resolvedQuantity ?? 0}</td>
                  <td data-testid={`inventory-transfer-disposition-unresolved-${line.lineId}`}>{line.unresolvedQuantity ?? 0}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <ul data-testid="inventory-transfer-disposition-list">
            {(selectedFulfillment?.dispositions ?? []).map((row) => (
              <li key={row.id} data-testid={`inventory-transfer-disposition-row-${row.id}`}>
                {label(DISPOSITION_LABELS, row.dispositionType)}
                {row.quantity ? ` ${row.quantity} 件` : ""}
                {row.responsibleParty ? ` · 责任方：${label(RESPONSIBLE_LABELS, row.responsibleParty)}` : ""}
                {row.amountCent ? ` · ${yuan(row.amountCent)}` : ""}
                {` · ${row.reason ?? ""} · ${label(DISPOSITION_STATUS_LABELS, row.status)}`}
                {row.reshipOrderId ? ` · 补发单 ${row.reshipOrderId}` : ""}
                {row.status === "pending_review" ? (
                  <>
                    <PermissionButton permissionsState={effectivePermissions} permissions={[FINANCE_REVIEW_PERMISSION]} disabled={submitting} data-testid={`inventory-transfer-disposition-approve-${row.id}`} onClick={() => reviewDisposition(row.id, "approved")}>复核通过</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={[FINANCE_REVIEW_PERMISSION]} disabled={submitting} data-testid={`inventory-transfer-disposition-reject-${row.id}`} onClick={() => reviewDisposition(row.id, "rejected")}>驳回</PermissionButton>
                  </>
                ) : null}
              </li>
            ))}
          </ul>
          <label>复核意见（财务复核待复核项时填写）<input data-testid="inventory-transfer-disposition-review-reason" value={disposition.reviewReason} onChange={setDispositionField("reviewReason")} /></label>
          <label>处理方式
            <select data-testid="inventory-transfer-disposition-type" value={disposition.type} onChange={setDispositionField("type")}>
              {Object.entries(DISPOSITION_LABELS).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
            </select>
          </label>
          {disposition.type === "payment_adjustment" ? (
            <label>调减金额（元）<input data-testid="inventory-transfer-disposition-amount" type="number" min="0" step="0.01" value={disposition.amountYuan} onChange={setDispositionField("amountYuan")} /></label>
          ) : (
            <>
              <label>明细
                <select data-testid="inventory-transfer-disposition-line" value={disposition.lineId} onChange={setDispositionField("lineId")}>
                  {(selectedFulfillment?.lines ?? []).map((line) => <option key={line.lineId} value={line.lineId}>{itemName(line.itemId)}</option>)}
                </select>
              </label>
              <label>数量<input data-testid="inventory-transfer-disposition-quantity" type="number" min="1" value={disposition.quantity} onChange={setDispositionField("quantity")} /></label>
            </>
          )}
          {disposition.type === "loss" ? (
            <label>责任方
              <select data-testid="inventory-transfer-disposition-responsible" value={disposition.responsible} onChange={setDispositionField("responsible")}>
                {Object.entries(RESPONSIBLE_LABELS).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
              </select>
            </label>
          ) : null}
          {disposition.type === "reship" ? (
            <label>补发货随货溯源码（可留空）<input data-testid="inventory-transfer-disposition-codes" value={disposition.codes} onChange={setDispositionField("codes")} /></label>
          ) : null}
          <label>差异原因与处理依据<input data-testid="inventory-transfer-disposition-reason" value={disposition.reason} onChange={setDispositionField("reason")} /></label>
          <label>交接证据（交接单、照片等）
            <input data-testid="inventory-transfer-disposition-evidence" type="file" multiple disabled={uploading} onChange={(event) => { void uploadProofs(event.target.files); event.target.value = ""; }} />
          </label>
          <ul>{proofs.map((proof) => <li key={proof.id}>{proof.name}（已上传）</li>)}</ul>
        </FormModal>

        <FormModal
          open={reviewOpen}
          title="供货主体审核"
          description="同意后进入待拣配；不同意退回草稿，门店按原因改单后可重新提交。"
          testId="inventory-transfer-review"
          submitTestId="inventory-transfer-review-submit"
          submitLabel="提交审核结论"
          submitting={submitting}
          error={formError || undefined}
          validate={() => (reviewDecision === "rejected" && !reviewReason.trim() ? "不同意须写明原因，门店据此改单。" : null)}
          onClose={() => setReviewOpen(false)}
          onSubmit={() => void runAction("inventory_transfer_order_supplier_reviewed", orderPath("supplier-review"), { decision: reviewDecision, reason: reviewReason.trim() }, "审核")}
        >
          <label>审核结论
            <select data-testid="inventory-transfer-review-decision" value={reviewDecision} onChange={(event) => setReviewDecision(event.target.value)}>
              <option value="approved">同意，进入待拣配</option>
              <option value="rejected">不同意，退回草稿</option>
            </select>
          </label>
          <label>原因<input data-testid="inventory-transfer-review-reason" value={reviewReason} onChange={(event) => setReviewReason(event.target.value)} /></label>
        </FormModal>

        <FormModal
          open={cancelRequestOpen}
          title="申请取消要货单"
          description="已提交、还没出库的要货单取消须经审批：提交后由上级审批，批准后才取消；审批期间单据不再往下流转。"
          testId="inventory-transfer-cancel-request"
          submitTestId="inventory-transfer-cancel-request-submit"
          submitLabel="提交取消申请"
          submitting={submitting}
          error={formError || undefined}
          validate={() => (cancelReason.trim() ? null : "请写明取消原因。")}
          onClose={() => setCancelRequestOpen(false)}
          onSubmit={() => void runAction("inventory_transfer_order_cancel_requested", orderPath("cancel"), { operatorNote: cancelReason.trim() }, "申请取消")}
        >
          <label>取消原因<input data-testid="inventory-transfer-cancel-request-reason" value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} /></label>
        </FormModal>

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="inventory-transfer-table">
            <thead>
              <tr>
                <th>选择</th>
                <th>出让方</th>
                <th>接收方</th>
                <th>流转类型</th>
                <th>品项 · 数量</th>
                <th>货值</th>
                <th>单据状态</th>
                <th>最近状态时间</th>
                <th>应付挂账</th>
                <th>详情</th>
              </tr>
            </thead>
            <tbody>
              {paged.pageRows.map((order) => {
                const posting = postings.get(order.id) ?? { kind: "unknown" as PostingKind };
                const amount = orderAmountCent(order);
                return (
                  <tr
                    key={order.id}
                    className="admin-row-clickable"
                    tabIndex={0}
                    data-testid={`inventory-transfer-row-${order.id}`}
                    onClick={() => openDetail(order)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openDetail(order); } }}
                  >
                    <td onClick={(event) => event.stopPropagation()}>
                      <input type="radio" aria-label={`选择流转单 ${order.id}`} data-testid={`inventory-transfer-select-${order.id}`} checked={selectedId === order.id} onChange={() => setSelectedId(order.id)} />
                    </td>
                    <td>{ownerName(order.fromOwnerType, order.fromOwnerId)}</td>
                    <td>{ownerName(order.toOwnerType, order.toOwnerId)}</td>
                    <td>{label(TRANSFER_TYPE_LABELS, order.transferType)}{order.lendingMode ? ` · ${label(LENDING_MODE_LABELS, order.lendingMode)}` : ""}</td>
                    <td>{order.lines ? `${order.lines.length} 项 · ${orderQuantity(order)}` : "—"}</td>
                    <td className="admin-num" style={{ textAlign: "right" }}>{amount === null ? "—" : yuan(amount)}</td>
                    <td>{label(TRANSFER_STATUS_LABELS, order.status)}</td>
                    <td>{cnDate(order.updatedAt)}</td>
                    <td data-testid={`inventory-transfer-posting-${order.id}`} onClick={(event) => { if (posting.arap) event.stopPropagation(); }}>
                      {posting.arap
                        ? <TraceLink navigateTo={navigateTo} pageId="ADM-FIN-001" objectId={posting.arap.id}>{posting.arap.itemNo ?? posting.arap.id}</TraceLink>
                        : POSTING_LABELS[posting.kind]}
                    </td>
                    <td className="admin-row-detail-cell">
                      <button type="button" className="admin-row-detail-btn" aria-label="查看流转单详情" onClick={(event) => { event.stopPropagation(); openDetail(order); }}>查看详情</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {visibleOrders.length === 0 ? (
            <div className="admin-state admin-empty" data-testid="inventory-transfer-filter-empty">
              当前筛选下没有流转单{focusObjectId ? `（正在按单号 ${focusObjectId} 定位）` : ""}。
            </div>
          ) : null}
          <TablePager testid="inventory-transfer-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
