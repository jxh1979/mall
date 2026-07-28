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
// 后端建单/发货/收货/归还/取消 + 发货即挂应付已具备，此页补仓管与财务的可视与可点：
// 「谁调给谁 → 调了什么多少 → 收没收到 → 这笔算多少钱 → 进没进应收应付台账」一行看全，
// 断链（货已发、账没挂）单独立卡告警——货对不上账最容易出在这一段。
// 读权限 inventory.transfer_order.view 由路由 requiredPermissions 门控；写动作按钮用 manage 码。
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
  lines?: TransferLine[];
};

type SupplyEntity = { id: string; name?: string; entityType?: string; status?: string };
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
    "接口证据：读取 /api/v1/inventory/transfer-orders、/api/v1/inventory/transfer-orders/{order_id}、/api/v1/inventory/supply-entities、/api/v1/inventory/items、/api/v1/platform/merchants、/api/v1/finance/ar-ap-items；写入 POST /api/v1/inventory/transfer-orders 及其 /ship、/receive、/return、/cancel。",
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
};
const OWNER_TYPE_LABELS: Record<string, string> = { supply_entity: "平台供货公司", merchant: "商户" };

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

/** 借用（borrow）默认不计占用费＝不挂账；供货调拨与买断按货款挂应付（后端 recordTransferSettlementOnShip 同口径）。 */
function isBillable(order: TransferOrder): boolean {
  return order.transferType === "supply" || (order.transferType === "lending" && order.lendingMode === "purchase");
}

type PostingKind =
  | "posted" // 已挂应付，凭证可点
  | "not_shipped" // 还没发货，按口径本就不该有账
  | "free_of_charge" // 借用不计费 / 货值为 0 的免费供货
  | "cancelled" // 单已取消（挂过的应付由后端冲销）
  | "missing" // ⚠ 货已发出，账没挂上：货账断链
  | "unknown"; // 财务台账没读到（无权限或接口降级），不下断言

type Posting = { kind: PostingKind; arap?: ArApLite };

const POSTING_LABELS: Record<PostingKind, string> = {
  posted: "已挂应付",
  not_shipped: "未发货·暂不挂账",
  free_of_charge: "不计货款",
  cancelled: "已取消·应付已冲销",
  missing: "⚠ 货已发·账未挂",
  unknown: "无法核对（无财务台账查看权限）",
};

function postingOf(order: TransferOrder, arapBySource: Map<string, ArApLite> | null): Posting {
  const arap = arapBySource?.get(order.id);
  if (arap) return { kind: "posted", arap };
  if (order.status === "cancelled") return { kind: "cancelled" };
  if (order.status === "created") return { kind: "not_shipped" };
  if (arapBySource === null) return { kind: "unknown" };
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
  missing: "货已发·账未挂",
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
  const [arapBySource, setArapBySource] = React.useState<Map<string, ArApLite> | null>(null);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [createOpen, setCreateOpen] = React.useState(false);
  const [receiveOpen, setReceiveOpen] = React.useState(false);
  const [scannedCodes, setScannedCodes] = React.useState("");
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
      const [orderRows, entityRows, merchantRows, itemRows, arapRows] = await Promise.all([
        api.get<TransferOrder[]>("/api/v1/inventory/transfer-orders"),
        api.get<SupplyEntity[]>("/api/v1/inventory/supply-entities").catch(() => [] as SupplyEntity[]),
        api.get<MerchantLite[]>("/api/v1/platform/merchants").catch(() => [] as MerchantLite[]),
        api.get<ItemLite[]>("/api/v1/inventory/items").catch(() => [] as ItemLite[]),
        // 关键词跨 source_type 服务端过滤（后端 listArApItemsPaged 的 ILIKE 白名单含 source_type），
        // 只取本域挂的应付；无 finance.ar_ap.view 的仓管账号会 403 → 降级为“无法核对”，不阻断发货收货主链。
        api
          .get<ArApLite[]>("/api/v1/finance/ar-ap-items?keyword=inventory_transfer&size=200")
          .catch(() => null),
      ]);
      // ponytail: 逐单取明细（列表接口不带 lines），开发库 14 单可接受；
      // 单量上百需后端 list 直接带 lines 或加一个货值汇总端点，别在前端 N+1。
      const withLines = await Promise.all(
        orderRows.map((order) =>
          api.get<TransferOrder>(`/api/v1/inventory/transfer-orders/${order.id}`).catch(() => order),
        ),
      );
      setOrders(withLines);
      setSupplyEntities(entityRows);
      setMerchants(merchantRows);
      setItems(itemRows);
      setArapBySource(
        arapRows === null
          ? null
          : new Map(
              arapRows
                .filter((row) => row.sourceType === "inventory_transfer" && row.sourceId)
                .map((row) => [row.sourceId as string, row]),
            ),
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
    () => new Map(orders.map((order) => [order.id, postingOf(order, arapBySource)])),
    [orders, arapBySource],
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
    if (!fromOwnerId) return "请选择出让方。";
    if (!toOwnerId) return "请选择接收方（商户）。";
    if (transferType === "lending" && fromOwnerId === toOwnerId) return "借调的出让方与接收方不能是同一个商户。";
    if (!itemId) return "请选择物料。";
    const qty = Number(quantity);
    if (!Number.isFinite(qty) || qty <= 0) return "数量必须大于 0。";
    const price = Number(unitPriceYuan);
    if (!Number.isFinite(price) || price < 0) return "结算单价不能为负数。";
    return null;
  }

  function submitCreate() {
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
        lines: [
          {
            itemId,
            quantity: Number(quantity),
            unitPriceCent: Math.round(Number(unitPriceYuan) * 100),
            traceCodes: splitCodes(traceCodes),
          },
        ],
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
      // 单据已取消时后端会把这笔应付冲销（金额留痕、凭证状态改 cancelled），凭证状态必须同屏，
      // 否则「已挂应付 + 一个凭证号」会被读成钱还欠着。
      fields.push({ label: "凭证状态", value: formatBusinessStatus(posting.arap.status) });
      fields.push({ label: "挂账金额", value: yuan(posting.arap.amountCent) });
      fields.push({ label: "已付 / 未付", value: `${yuan(posting.arap.paidAmountCent)} / ${yuan(posting.arap.unpaidAmountCent)}` });
      fields.push({ label: "发货挂账时间", value: cnDate(posting.arap.createdAt) });
    }
    setDetail({ title: `流转单明细 · ${order.id}`, fields });
  }

  const selectedPosting = selected ? postings.get(selected.id) : undefined;
  const canShip = selected?.status === "created";
  const canReceive = selected?.status === "shipped";
  const canReturn = selected?.status === "received" && selected.transferType === "lending" && selected.lendingMode === "borrow";
  const canCancel = selected?.status === "created" || selected?.status === "shipped";

  return (
    <section data-testid="inventory-transfer-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails>
        <div className="admin-generic-evidence" data-testid="inventory-transfer-evidence">{TEXT.evidence}</div>
      </TechDetails>
      <AuditTrailHint event="inventory.transfer_order_created / inventory.transfer_order_shipped / inventory.transfer_order_received / inventory.transfer_order_returned / inventory.transfer_order_cancelled" />
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
              <small>{arapBySource === null ? "无财务台账查看权限，未核对" : `${counts.postedCount} 笔已进应收应付台账`}</small>
            </div>
            <div className="admin-kpi-card admin-kpi-card--clickable">
              <span>货已发·账未挂</span>
              <button type="button" className="admin-kpi-value" data-testid="inventory-transfer-kpi-missing" onClick={() => enterKpiFocus("missing")}>{counts.missing}</button>
              <small>该收钱却没挂应付，需财务核实</small>
            </div>
          </div>
        )}

        <div className="admin-form-row" data-testid="inventory-transfer-actions">
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            data-testid="inventory-transfer-create-open"
            onClick={() => { setFormError(""); setCreateOpen(true); }}
          >
            ＋ 新建流转单
          </PermissionButton>
          <HighRiskConfirm
            label="发货"
            impact="发货即按货款在应收应付台账挂出接收方应付出让方的款项（借用型不计费不挂账），并把单据推进到在途待收货。"
            disabled={submitting || !canShip}
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            data-testid="inventory-transfer-ship"
            onConfirm={() => selected && void runAction("inventory_transfer_order_shipped", `/api/v1/inventory/transfer-orders/${selected.id}/ship`, undefined, "发货")}
          />
          <PermissionButton
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            disabled={!canReceive}
            data-testid="inventory-transfer-receive-open"
            onClick={() => { setFormError(""); setScannedCodes(""); setReceiveOpen(true); }}
          >
            扫码收货
          </PermissionButton>
          <HighRiskConfirm
            label="归还"
            impact="把借用的货退回出让方，溯源码归属翻回原主体并留痕；仅借用（约定归还）型已收货单据可归还。"
            disabled={submitting || !canReturn}
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            data-testid="inventory-transfer-return"
            onConfirm={() => selected && void runAction("inventory_transfer_order_returned", `/api/v1/inventory/transfer-orders/${selected.id}/return`, undefined, "归还")}
          />
          <HighRiskConfirm
            label="取消单据"
            impact="取消未收货的流转单；若已发货并挂了应付，会同步冲销该笔应付（已有收款的必须先退款才能取消）。"
            disabled={submitting || !canCancel}
            permissionsState={effectivePermissions}
            permissions={[MANAGE_PERMISSION]}
            data-testid="inventory-transfer-cancel"
            onConfirm={() => selected && void runAction("inventory_transfer_order_cancelled", `/api/v1/inventory/transfer-orders/${selected.id}/cancel`, undefined, "取消")}
          />
          <span className="admin-muted-action" data-testid="inventory-transfer-hint">
            {selected
              ? `已选中 ${selected.id}（${label(TRANSFER_STATUS_LABELS, selected.status)}·${POSTING_LABELS[selectedPosting?.kind ?? "unknown"]}）`
              : "先在下表选中一张流转单，再执行发货 / 收货 / 归还 / 取消。"}
          </span>
        </div>
        {formError ? <div data-testid="inventory-transfer-error" className="admin-inline-error">{formError}</div> : null}

        <FormModal
          open={createOpen}
          title="新建跨主体流转单"
          description="平台供货公司送货给商户选「平台供货调拨」；商户之间的货选「商户间借调」，并区分借用（约定归还）还是买断。一张单先录一条明细，发货后按货款挂应付。"
          testId="inventory-transfer-create"
          submitTestId="inventory-transfer-create-submit"
          submitLabel="建单"
          submitting={submitting}
          error={formError || undefined}
          validate={createValidation}
          onClose={() => setCreateOpen(false)}
          onSubmit={submitCreate}
        >
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
