// 文件版本：V0.06
import React from "react";
import { InventoryQuarantinePanel } from "./InventoryQuarantinePanel";
import { InventoryWarehousePanel } from "./InventoryWarehousePanel";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { formatLocation, KNOWN_LOCATIONS } from "../i18n/locationLabels";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { FormModal } from "../components/FormModal";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { PermissionButton, useCan } from "../security/Can";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, InventoryItemRow, StockBalanceRow, StockMovementRow } from "../types";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

// 后端写权限码（InventoryController：POST /stock-movements）
const STOCK_MOVE_PERMISSION = "inventory.stock.move";
// 第十八批第 24 条 24-R5（ENUM:164「或由库房主管以上岗位手工标记为高价值，标记与取消标记都必须填写理由并留痕，一线库管无此权限」）：
// 标记与取消走 POST /api/v1/inventory/items/{id}/high-value-mark，持库房主管复核码。
const HIGH_VALUE_MARK_PERMISSION = "inventory.high_value.review";
// 第十八批 L 段 103-R2（BSM:1197「酒水/礼品/高价值物料贴平台唯一二维码」；BSM:1166「……可以入库贴码，也可以出库打印码」）：
// 物料行登记物品码走 POST /api/v1/inventory/trace-codes（入库贴码）。
const TRACE_CODE_REGISTER_PERMISSION = "inventory.trace_code.register";

const TEXT = {
  title: "库存台账",
  description: "集中查看物料、库存余额和库存流水，支持按 OpenAPI 契约登记库存移动；负库存禁止，高价值酒水保持在线核销。",
  evidence: "接口证据：读取 /api/v1/inventory/items、/api/v1/inventory/stock-balances、/api/v1/inventory/stock-movements、/api/v1/inventory/cost-summary；吧台消耗统计 GET /api/v1/inventory/bar-consumption-stats；登记 POST /api/v1/inventory/stock-movements；新建库存商品规格 POST /api/v1/inventory/items；修改安全库存 POST /api/v1/inventory/items/{id}/safety-stock；库房 GET/POST /api/v1/inventory/warehouses（列表与新建）；登记物品码 POST /api/v1/inventory/trace-codes。",
  loadFailed: "库存台账读取失败",
  actionFailed: "库存移动登记失败",
  quantityInvalid: "移动数量必须大于 0，禁止负库存调整。",
  noItem: "当前没有可操作库存物料。",
  items: "物料",
  balances: "余额",
  movements: "流水",
  totalQuantity: "总库存",
  noNegative: "负库存禁止",
  highValueOnline: "高价值酒水在线核销",
  fromLocation: "出库位置",
  toLocation: "入库位置",
  quantity: "数量",
  traceCode: "追踪码",
  note: "备注",
  submit: "登记库存移动",
  boundary: "库存移动必须使用 from_location_id、to_location_id、items 和 idempotency_key；高价值酒水不允许离线扣减，异常调整必须留原因和附件证据。",
  itemCode: "编码",
  itemName: "名称",
  category: "分类",
  unit: "单位",
  status: "状态",
  location: "位置",
  itemId: "物料",
  time: "时间",
  focusClear: "清除筛选，返回全部",
  // V296 成本台账（移动加权平均）。
  cost: "成本",
  unitCost: "单价",
  noCostBasis: "无成本基准",
  monthlyCost: "本月出库成本",
  costPending: "暂无数据",
  // 第十八批 I 段第 76 条：按杯按标准出酒量（按容量）扣库存的规格容量、已开瓶余量与流水出酒量、损耗量。
  specVolume: "规格容量（毫升）",
  openedVolume: "已开瓶余量（毫升）",
  pourVolume: "出酒量（毫升）",
  lossVolume: "损耗量（毫升）",
};

// 第十八批 K 段 97-R1（ENUM:161「物品大类：酒类、消耗品、礼品、其他物料」；ENUM:79「定价、库存、结算、对账一律按商品规格逐项进行」）：
// 新建库存商品规格，大类提交值即中文大类名；按杯卖的酒登记规格容量（毫升），按杯按容量扣库存（BSM:1168）。
const ITEM_CATEGORIES = ["酒类", "消耗品", "礼品", "其他物料"];
type ItemForm = { itemName: string; category: string; unit: string; minQuantity: string; specVolumeMl: string };
const EMPTY_ITEM_FORM: ItemForm = { itemName: "", category: "酒类", unit: "", minQuantity: "", specVolumeMl: "" };

function validateItemForm(form: ItemForm): string | null {
  if (!form.itemName.trim() || !form.unit.trim()) return "商品规格名称、单位必填。";
  if (form.minQuantity.trim() !== "" && !/^\d+$/.test(form.minQuantity.trim())) return "安全库存须为不小于 0 的整数。";
  if (form.specVolumeMl.trim() !== "" && !/^[1-9]\d*$/.test(form.specVolumeMl.trim())) return "规格容量须为正整数（毫升）；不按容量扣库存的留空。";
  return null;
}

const optionalInt = (value: string): number | null => (value.trim() === "" ? null : Number(value.trim()));

// 第十八批 K 段 97-R4（BSM:1168「记录吧台售卖后的酒水消耗，支持按杯、按瓶、按套餐或其他形式统计」）：吧台消耗统计一行。
type BarConsumptionRow = {
  barId: string; barName?: string | null; saleForm: string; comboId?: string | null; comboName?: string | null;
  productId: string; productName?: string | null; inventoryItemId?: string | null; itemName?: string | null;
  quantity: number; pourVolumeMl?: number | null; lossVolumeMl: number;
};
// 数量单位：按杯记杯数、按瓶记瓶数；套餐记套餐明细件数（套餐内件数×份数，可能是按杯商品），记「份」。
const SALE_FORM_UNIT: Record<string, string> = { by_glass: "杯", by_bottle: "瓶", combo: "份" };

// V296：成本金额展示（整数分 → 元）；null/缺省 = 无成本基准（存量库存未录进货价，不编造数字）。
function fmtCostCent(value: unknown): string {
  return typeof value === "number" && Number.isFinite(value) ? `¥${(value / 100).toFixed(2)}` : TEXT.noCostBasis;
}

// V296 期间出库成本汇总（本月，分红成本侧同一取数点）。
type CostSummaryView = {
  outboundTotalCostCent: number;
  outboundMovementCount: number;
  noCostBasisCount: number;
};

function asCostSummary(value: unknown): CostSummaryView | null {
  const raw = value as CostSummaryView | null;
  return raw && typeof raw === "object" && typeof raw.outboundTotalCostCent === "number" ? raw : null;
}

function currentMonthRange(): { start: string; end: string } {
  const now = new Date();
  const pad = (part: number) => String(part).padStart(2, "0");
  const month = `${now.getFullYear()}-${pad(now.getMonth() + 1)}`;
  return { start: `${month}-01`, end: `${month}-${pad(now.getDate())}` };
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：物料/余额/流水三卡各对一张承载台账，点数字=带
// ?kpi= 参数重进本页只渲染该表。负库存禁止/高价值酒水在线核销两卡为策略/状态说明，无承载表→不可点。
type KpiTarget = "items" | "balances" | "movements";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "items" && table !== "balances" && table !== "movements") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  return focus.table === "items" ? TEXT.items : focus.table === "balances" ? TEXT.balances : TEXT.movements;
}

function asNumber(value: unknown, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function InventoryLedgerPage({ api, setActionResult, setGlobalError, effectivePermissions, reportViewedObjects }: AdminPageProps) {
  const [items, setItems] = React.useState<InventoryItemRow[]>([]);
  const [balances, setBalances] = React.useState<StockBalanceRow[]>([]);
  const canReadQuarantine = useCan(effectivePermissions, ["inventory.stock.view"]);
  const canManageQuarantine = useCan(effectivePermissions, ["inventory.stock.adjust", "evidence.attachment.create"]);
  // 第十八批 L 段 105-R1（BSM:1162「管理库房和库位……」）：库房区块（列表与新建同一权限码）。
  const canManageWarehouse = useCan(effectivePermissions, ["inventory.warehouse.manage"]);
  const [movements, setMovements] = React.useState<StockMovementRow[]>([]);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("inventory_item", items), [items, reportViewedObjects]);
  React.useEffect(() => reportViewedObjects?.("stock_movement", movements), [movements, reportViewedObjects]);
  const pagedBalances = usePagedRows(balances, 10, balances.length);
  const pagedMovements = usePagedRows(movements, 10, movements.length);
  const { selectedId: selectedItemId, setSelectedId: setSelectedItemId, selected: selectedItem } = useSelectedRow(items);
  const [fromLocation, setFromLocation] = React.useState("bar-main");
  const [toLocation, setToLocation] = React.useState("warehouse-main");
  const [quantity, setQuantity] = React.useState("1");
  const [traceCode, setTraceCode] = React.useState("TRACE-ADMIN-001");
  const [operatorNote, setOperatorNote] = React.useState("完整后台验收库存移动");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [createOpen, setCreateOpen] = React.useState(false);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // V296：本月出库成本汇总（读取失败不阻断台账主链）。
  const [costSummary, setCostSummary] = React.useState<CostSummaryView | null>(null);
  // 行明细抽屉：整行可点 -> 该物料/余额/流水完整字段（状态中文·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 第十八批第 24 条 24-R5：手工标记／取消标记高价值（理由必填）。
  const [markTarget, setMarkTarget] = React.useState<InventoryItemRow | null>(null);
  const [markReason, setMarkReason] = React.useState("");
  const [markError, setMarkError] = React.useState("");
  const [marking, setMarking] = React.useState(false);
  // 第十八批第 33 条 33-R4：修改已有物料的安全库存（采购建议量算式用；权限与新建物料同码）。
  const [stockTarget, setStockTarget] = React.useState<InventoryItemRow | null>(null);
  const [stockValue, setStockValue] = React.useState("");
  const [stockError, setStockError] = React.useState("");
  const [stockSaving, setStockSaving] = React.useState(false);
  // 第十八批 L 段 103-R2：物料行登记物品码。
  const [traceTarget, setTraceTarget] = React.useState<InventoryItemRow | null>(null);
  const [traceValue, setTraceValue] = React.useState("");
  const [traceError, setTraceError] = React.useState("");
  const [traceSaving, setTraceSaving] = React.useState(false);
  // 第十八批 K 段 97-R1：新建库存商品规格（权限同库存移动 inventory.stock.move，即 POST /items 的后端码）。
  const [itemCreateOpen, setItemCreateOpen] = React.useState(false);
  const [itemForm, setItemForm] = React.useState<ItemForm>(EMPTY_ITEM_FORM);
  const [itemCreateError, setItemCreateError] = React.useState("");
  const [itemCreating, setItemCreating] = React.useState(false);
  // 第十八批 K 段 97-R4：吧台消耗统计（起止日期填齐点查询才请求）。
  const canViewConsumption = useCan(effectivePermissions, ["inventory.stock.view"]);
  const [consumptionStart, setConsumptionStart] = React.useState("");
  const [consumptionEnd, setConsumptionEnd] = React.useState("");
  const [consumptionRows, setConsumptionRows] = React.useState<BarConsumptionRow[] | null>(null);
  const [consumptionError, setConsumptionError] = React.useState("");
  const [consumptionLoading, setConsumptionLoading] = React.useState(false);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table });
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

  const { loading, error, reload } = useAdminResource(
    async () => {
      const month = currentMonthRange();
      const [itemRows, balanceRows, movementRows, summaryRaw] = await Promise.all([
        api.get<InventoryItemRow[]>("/api/v1/inventory/items"),
        api.get<StockBalanceRow[]>("/api/v1/inventory/stock-balances"),
        // AUD-013: 后端已改真分页(默认 50)。本页仍客户端分页 → 显式取一页上限(后端 size 封顶 200)。
        // ponytail: 200 上限, 超量需改 useServerPagedRows+getPaged 服务端分页(并加汇总端点还原 KPI 全量口径)。
        api.get<StockMovementRow[]>("/api/v1/inventory/stock-movements?size=200"),
        // V296：本月出库成本汇总小卡；失败降级为“暂无数据”，不拖垮台账主链。
        api.get<CostSummaryView>(`/api/v1/inventory/cost-summary?period_start=${month.start}&period_end=${month.end}`)
          .catch(() => null),
      ]);
      setItems(itemRows);
      setBalances(balanceRows);
      setMovements(movementRows);
      setCostSummary(asCostSummary(summaryRaw));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const totalQuantity = balances.reduce((sum, row) => sum + asNumber(row.quantity), 0);

  function validateMovement() {
    if (!selectedItem) {
      setFormError(TEXT.noItem);
      return false;
    }
    const movementQuantity = Number(quantity);
    if (!Number.isFinite(movementQuantity) || movementQuantity <= 0) {
      setFormError(TEXT.quantityInvalid);
      return false;
    }
    return true;
  }

  async function submitMovement() {
    if (!selectedItem) {
      setFormError(TEXT.noItem);
      return;
    }
    const movementQuantity = Number(quantity);
    if (!Number.isFinite(movementQuantity) || movementQuantity <= 0) {
      setFormError(TEXT.quantityInvalid);
      return;
    }
    const requestBody = {
      idempotency_key: `admin-stock-movement-${selectedItem.id}-${movementQuantity}`,
      operator_note: operatorNote,
      evidence_attachment_ids: ["att-stock-movement-1"],
      from_location_id: fromLocation,
      to_location_id: toLocation,
      items: [{ item_id: selectedItem.id, quantity: movementQuantity, unit: selectedItem.unit ?? "unit" }],
      trace_code: traceCode,
    };
    setSubmitting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.post("/api/v1/inventory/stock-movements", requestBody);
      setActionResult({ action: "inventory_stock_movement_created", requestBody, result });
      setCreateOpen(false);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : TEXT.actionFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "inventory_stock_movement_created", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  async function submitHighValueMark() {
    if (!markTarget) return;
    const requestBody = {
      idempotency_key: `admin-high-value-mark-${markTarget.id}-${Date.now()}`,
      manual_high_value: !markTarget.manualHighValue,
      reason: markReason.trim(),
    };
    setMarking(true);
    setMarkError("");
    try {
      const result = await api.post(`/api/v1/inventory/items/${encodeURIComponent(markTarget.id)}/high-value-mark`, requestBody);
      setActionResult({ action: "inventory_item_high_value_marked", requestBody, result });
      setMarkTarget(null);
      await reload();
    } catch (actionError) {
      setMarkError(actionError instanceof Error ? actionError.message : "高价值标记失败");
    } finally {
      setMarking(false);
    }
  }

  async function submitSafetyStock() {
    if (!stockTarget) return;
    const requestBody = {
      idempotency_key: `admin-safety-stock-${stockTarget.id}-${Date.now()}`,
      min_quantity: Number(stockValue.trim()),
    };
    setStockSaving(true);
    setStockError("");
    try {
      const result = await api.post(`/api/v1/inventory/items/${encodeURIComponent(stockTarget.id)}/safety-stock`, requestBody);
      setActionResult({ action: "inventory_item_safety_stock_updated", requestBody, result });
      setStockTarget(null);
      await reload();
    } catch (actionError) {
      setStockError(actionError instanceof Error ? actionError.message : "安全库存修改失败");
    } finally {
      setStockSaving(false);
    }
  }

  async function submitTraceCode() {
    if (!traceTarget) return;
    const requestBody = {
      idempotency_key: `admin-trace-code-${traceTarget.id}-${Date.now()}`,
      item_id: traceTarget.id,
      trace_code: traceValue.trim(),
    };
    setTraceSaving(true);
    setTraceError("");
    try {
      const result = await api.post("/api/v1/inventory/trace-codes", requestBody);
      setActionResult({ action: "inventory_trace_code_registered", requestBody, result });
      setTraceTarget(null);
    } catch (actionError) {
      setTraceError(actionError instanceof Error ? actionError.message : "物品码登记失败");
    } finally {
      setTraceSaving(false);
    }
  }

  async function submitInventoryItem() {
    const requestBody = {
      idempotency_key: `admin-inventory-item-${Date.now()}`,
      item_name: itemForm.itemName.trim(),
      category: itemForm.category,
      unit: itemForm.unit.trim(),
      min_quantity: optionalInt(itemForm.minQuantity),
      spec_volume_ml: optionalInt(itemForm.specVolumeMl),
    };
    setItemCreating(true);
    setItemCreateError("");
    try {
      const result = await api.post("/api/v1/inventory/items", requestBody);
      setActionResult({ action: "inventory_item_created", requestBody, result });
      setItemCreateOpen(false);
      setItemForm(EMPTY_ITEM_FORM);
      await reload();
    } catch (actionError) {
      setItemCreateError(actionError instanceof Error ? actionError.message : "库存商品规格新建失败");
    } finally {
      setItemCreating(false);
    }
  }

  async function queryConsumption() {
    setConsumptionLoading(true);
    setConsumptionError("");
    try {
      setConsumptionRows(await api.get<BarConsumptionRow[]>(`/api/v1/inventory/bar-consumption-stats?period_start=${consumptionStart}&period_end=${consumptionEnd}`));
    } catch (queryError) {
      setConsumptionError(queryError instanceof Error ? queryError.message : "吧台消耗统计读取失败");
    } finally {
      setConsumptionLoading(false);
    }
  }

  function setItemField(key: keyof ItemForm, value: string) {
    setItemForm((current) => ({ ...current, [key]: value }));
  }

  function openItemDetail(item: InventoryItemRow) {
    setDetail({
      title: `${TEXT.items} · ${item.itemName ?? item.itemCode ?? item.id}`,
      fields: [
        { label: TEXT.itemCode, value: item.itemCode ?? item.id },
        { label: TEXT.itemName, value: item.itemName ?? "-" },
        { label: TEXT.category, value: item.category ?? "-" },
        { label: TEXT.unit, value: item.unit ?? "-" },
        { label: TEXT.specVolume, value: item.specVolumeMl ?? "-" },
        { label: TEXT.status, value: formatBusinessStatus(item.status) },
        { label: TEXT.itemId, value: item.id },
      ],
    });
  }

  function openBalanceDetail(row: StockBalanceRow) {
    setDetail({
      title: `${TEXT.balances} · ${row.itemId ?? row.id}`,
      fields: [
        { label: TEXT.itemId, value: row.itemId ?? "-" },
        { label: TEXT.location, value: formatLocation(row.locationType ?? row.locationId) },
        { label: TEXT.quantity, value: row.quantity ?? "-" },
        { label: TEXT.openedVolume, value: row.openedVolumeMl ?? "-" },
        { label: TEXT.status, value: formatBusinessStatus(row.status ?? "normal") },
      ],
    });
  }

  function openMovementDetail(row: StockMovementRow) {
    setDetail({
      title: `${TEXT.movements} · ${row.itemId ?? row.id ?? "-"}`,
      fields: [
        { label: TEXT.itemId, value: row.itemId ?? "-" },
        { label: TEXT.quantity, value: row.quantity ?? "-" },
        { label: TEXT.pourVolume, value: row.volumeMl ?? "-" },
        { label: TEXT.lossVolume, value: row.lossVolumeMl ?? "-" },
        { label: TEXT.unitCost, value: fmtCostCent(row.unitCostCent) },
        { label: TEXT.cost, value: fmtCostCent(row.totalCostCent) },
        { label: TEXT.status, value: formatBusinessStatus(row.status) },
        { label: TEXT.time, value: cnDate(row.createdAt) },
      ],
    });
  }

  // 物料区：登记表单（对选中物料操作）+ 物料承载表。聚焦「物料」时表单一起呈现，登记不断。
  const itemsSection = (
    <>
      {/* 引导式：登记库存移动收起为入口按钮，先在下方物料表选中物料，再点开分步引导（位置→数量→确认）。 */}
      <div className="admin-form-row" data-testid="inventory-movement-form">
        <PermissionButton permissionsState={effectivePermissions} permissions={[STOCK_MOVE_PERMISSION]} data-testid="inventory-item-create-open" onClick={() => { setItemCreateError(""); setItemCreateOpen(true); }}>＋ 新建库存商品规格</PermissionButton>
        <PermissionButton permissionsState={effectivePermissions} permissions={[STOCK_MOVE_PERMISSION]} data-testid="inventory-movement-open" onClick={() => { setFormError(""); setCreateOpen(true); }}>＋ {TEXT.submit}</PermissionButton>
        <span className="admin-muted-action" data-testid="inventory-ledger-boundary">{TEXT.boundary}</span>
      </div>
      {formError ? <div data-testid="inventory-ledger-error" className="admin-inline-error">{formError}</div> : null}
      <FormModal
        open={createOpen}
        title={TEXT.submit}
        description={selectedItem ? `物料：${selectedItem.itemName ?? selectedItem.itemCode ?? selectedItem.id}（出入库位置/数量/追踪一屏填完提交）` : TEXT.noItem}
        testId="inventory-movement-guided"
        submitTestId="inventory-movement-submit"
        submitLabel={TEXT.submit}
        submitting={submitting}
        error={formError || undefined}
        validate={() => {
          const q = Number(quantity);
          if (!selectedItem) return TEXT.noItem;
          return !Number.isFinite(q) || q <= 0 ? TEXT.quantityInvalid : null;
        }}
        onClose={() => setCreateOpen(false)}
        onSubmit={() => void submitMovement()}
      >
        <label>{TEXT.fromLocation}<input list="inventory-location-options" data-testid="inventory-movement-from" value={fromLocation} onChange={(event) => setFromLocation(event.target.value)} /></label>
        <label>{TEXT.toLocation}<input list="inventory-location-options" data-testid="inventory-movement-to" value={toLocation} onChange={(event) => setToLocation(event.target.value)} /></label>
        <datalist id="inventory-location-options">{KNOWN_LOCATIONS.map((loc) => <option key={loc.code} value={loc.code}>{loc.name}</option>)}</datalist>
        <label>{TEXT.quantity}<input data-testid="inventory-movement-quantity" type="number" min="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} /></label>
        <label>{TEXT.traceCode}<input data-testid="inventory-movement-trace" value={traceCode} onChange={(event) => setTraceCode(event.target.value)} /></label>
        <label>{TEXT.note}<input data-testid="inventory-movement-note" value={operatorNote} onChange={(event) => setOperatorNote(event.target.value)} /></label>
      </FormModal>
      <GuidedFlowModal
        open={itemCreateOpen}
        title="新建库存商品规格"
        testId="inventory-item-create-guided"
        submitTestId="inventory-item-create-submit"
        submitLabel="新建库存商品规格"
        submitting={itemCreating}
        error={itemCreateError || undefined}
        onClose={() => setItemCreateOpen(false)}
        onSubmit={() => void submitInventoryItem()}
        steps={[
          {
            title: "商品规格信息",
            description: "商品规格＝商品名加型号规格，例如「某某威士忌 700 毫升瓶装」。按杯卖的酒须填规格容量（每瓶毫升数），按杯按标准容量扣库存；不按容量扣库存的留空。",
            validate: () => validateItemForm(itemForm),
            content: (
              <>
                <label><span>商品规格名称</span><input data-testid="inventory-item-create-name" value={itemForm.itemName} onChange={(event) => setItemField("itemName", event.target.value)} /></label>
                <label><span>物品大类</span><select data-testid="inventory-item-create-category" value={itemForm.category} onChange={(event) => setItemField("category", event.target.value)}>
                  {ITEM_CATEGORIES.map((category) => <option key={category} value={category}>{category}</option>)}
                </select></label>
                <label><span>{TEXT.unit}</span><input data-testid="inventory-item-create-unit" value={itemForm.unit} onChange={(event) => setItemField("unit", event.target.value)} placeholder="例如 瓶、罐、包" /></label>
                <label><span>安全库存（可空）</span><input data-testid="inventory-item-create-min-quantity" type="number" min="0" step="1" value={itemForm.minQuantity} onChange={(event) => setItemField("minQuantity", event.target.value)} /></label>
                <label><span>{TEXT.specVolume}（可空）</span><input data-testid="inventory-item-create-spec-volume" type="number" min="1" step="1" value={itemForm.specVolumeMl} onChange={(event) => setItemField("specVolumeMl", event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "商品规格名称", value: itemForm.itemName.trim() || "未填写" },
          { label: "物品大类", value: itemForm.category },
          { label: TEXT.unit, value: itemForm.unit.trim() || "未填写" },
          { label: "安全库存", value: itemForm.minQuantity.trim() || "0" },
          { label: TEXT.specVolume, value: itemForm.specVolumeMl.trim() || "不登记（不按容量扣库存）" },
        ]}
      />
      <FormModal
        open={markTarget !== null}
        title={markTarget?.manualHighValue ? "取消高价值标记" : "手工标记为高价值"}
        description={markTarget ? `物料：${markTarget.itemName ?? markTarget.itemCode ?? markTarget.id}。标记与取消标记都须填写理由，留痕可查。` : undefined}
        testId="inventory-high-value-mark"
        submitTestId="inventory-high-value-mark-submit"
        submitLabel={markTarget?.manualHighValue ? "取消标记" : "标记高价值"}
        submitting={marking}
        error={markError || undefined}
        validate={() => (markReason.trim() === "" ? "请填写理由。" : null)}
        onClose={() => setMarkTarget(null)}
        onSubmit={() => void submitHighValueMark()}
      >
        <label>理由<input data-testid="inventory-high-value-mark-reason" value={markReason} onChange={(event) => setMarkReason(event.target.value)} /></label>
      </FormModal>
      <FormModal
        open={stockTarget !== null}
        title="修改安全库存"
        description={stockTarget ? `物料：${stockTarget.itemName ?? stockTarget.itemCode ?? stockTarget.id}。采购建议量＝安全库存＋近 7 天销量－当前可用库存－在途量。` : undefined}
        testId="inventory-safety-stock"
        submitTestId="inventory-safety-stock-submit"
        submitLabel="保存安全库存"
        submitting={stockSaving}
        error={stockError || undefined}
        validate={() => (/^\d+$/.test(stockValue.trim()) ? null : "安全库存须为不小于 0 的整数。")}
        onClose={() => setStockTarget(null)}
        onSubmit={() => void submitSafetyStock()}
      >
        <label>安全库存<input data-testid="inventory-safety-stock-input" type="number" min="0" step="1" value={stockValue} onChange={(event) => setStockValue(event.target.value)} /></label>
      </FormModal>
      <FormModal
        open={traceTarget !== null}
        title="登记物品码"
        description={traceTarget ? `物料：${traceTarget.itemName ?? traceTarget.itemCode ?? traceTarget.id}。扫贴在实物上的平台唯一二维码，一件一码。` : undefined}
        testId="inventory-trace-code"
        submitTestId="inventory-trace-code-submit"
        submitLabel="登记物品码"
        submitting={traceSaving}
        error={traceError || undefined}
        validate={() => (traceValue.trim() === "" ? "请扫码或填写物品码。" : null)}
        onClose={() => setTraceTarget(null)}
        onSubmit={() => void submitTraceCode()}
      >
        <label>物品码<input data-testid="inventory-trace-code-input" value={traceValue} onChange={(event) => setTraceValue(event.target.value)} /></label>
      </FormModal>
      <h3>{TEXT.items}</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="inventory-ledger-items-table">
          <thead><tr><th>{TEXT.itemCode}</th><th>{TEXT.itemName}</th><th>{TEXT.category}</th><th>{TEXT.unit}</th><th>{TEXT.status}</th><th>安全库存</th><th>高价值标记</th><th>物品码</th><th>详情</th></tr></thead>
          <tbody>{items.map((item) => <tr key={item.id} className="admin-row-clickable" tabIndex={0} data-testid={`inventory-item-row-${item.id}`} onClick={() => openItemDetail(item)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openItemDetail(item); } }}><td onClick={(event) => event.stopPropagation()}>{item.itemCode ?? item.id} <input type="radio" aria-label={`选择物料 ${item.itemCode ?? item.id}`} checked={selectedItemId === item.id} onChange={() => setSelectedItemId(item.id)} /></td><td>{item.itemName ?? "-"}</td><td>{item.category ?? "-"}</td><td>{item.unit ?? "-"}</td><td>{formatBusinessStatus(item.status)}</td><td onClick={(event) => event.stopPropagation()}><span data-testid={`inventory-item-min-quantity-${item.id}`}>{item.minQuantity ?? 0}</span> <PermissionButton permissionsState={effectivePermissions} permissions={[STOCK_MOVE_PERMISSION]} data-testid={`inventory-item-safety-stock-${item.id}`} onClick={() => { setStockValue(String(item.minQuantity ?? 0)); setStockError(""); setStockTarget(item); }}>修改安全库存</PermissionButton></td><td onClick={(event) => event.stopPropagation()}>{item.manualHighValue ? "已手工标记" : "未标记"} <PermissionButton permissionsState={effectivePermissions} permissions={[HIGH_VALUE_MARK_PERMISSION]} data-testid={`inventory-item-high-value-mark-${item.id}`} onClick={() => { setMarkReason(""); setMarkError(""); setMarkTarget(item); }}>{item.manualHighValue ? "取消标记" : "标记高价值"}</PermissionButton></td><td onClick={(event) => event.stopPropagation()}><PermissionButton permissionsState={effectivePermissions} permissions={[TRACE_CODE_REGISTER_PERMISSION]} data-testid={`inventory-item-trace-code-${item.id}`} onClick={() => { setTraceValue(""); setTraceError(""); setTraceTarget(item); }}>登记物品码</PermissionButton></td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看物料详情" onClick={(event) => { event.stopPropagation(); openItemDetail(item); }}>查看详情</button></td></tr>)}</tbody>
        </table>
      </div>
    </>
  );

  const balancesSection = (
    <>
      <h3>{TEXT.balances}</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="inventory-ledger-balances-table">
          <thead><tr><th>{TEXT.itemId}</th><th>{TEXT.location}</th><th>{TEXT.quantity}</th><th>{TEXT.status}</th><th>详情</th></tr></thead>
          <tbody>{pagedBalances.pageRows.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`inventory-balance-row-${row.id}`} onClick={() => openBalanceDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openBalanceDetail(row); } }}><td>{row.itemId ?? "-"}</td><td title={row.locationType ?? row.locationId ?? ""}>{formatLocation(row.locationType ?? row.locationId)}</td><td>{row.quantity ?? "-"}</td><td>{formatBusinessStatus(row.status ?? "normal")}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看库存余额详情" onClick={(event) => { event.stopPropagation(); openBalanceDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
        <TablePager testid="inventory-balances-pager" page={pagedBalances.page} pageCount={pagedBalances.pageCount} total={pagedBalances.total} onPrev={pagedBalances.prev} onNext={pagedBalances.next} />
      </div>
    </>
  );

  const movementsSection = (
    <>
      <h3>{TEXT.movements}</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="inventory-ledger-movements-table">
          <thead><tr><th>{TEXT.itemId}</th><th>{TEXT.quantity}</th><th>{TEXT.cost}</th><th>{TEXT.status}</th><th>{TEXT.time}</th><th>详情</th></tr></thead>
          <tbody>{pagedMovements.pageRows.map((row, index) => <tr key={row.id ?? index} className="admin-row-clickable" tabIndex={0} data-testid={`inventory-movement-row-${row.id ?? index}`} onClick={() => openMovementDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMovementDetail(row); } }}><td>{row.itemId ?? "-"}</td><td>{row.quantity ?? "-"}</td><td>{fmtCostCent(row.totalCostCent)}</td><td>{formatBusinessStatus(row.status)}</td><td>{cnDate(row.createdAt)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看库存流水详情" onClick={(event) => { event.stopPropagation(); openMovementDetail(row); }}>查看详情</button></td></tr>)}</tbody>
        </table>
        <TablePager testid="inventory-movements-pager" page={pagedMovements.page} pageCount={pagedMovements.pageCount} total={pagedMovements.total} onPrev={pagedMovements.prev} onNext={pagedMovements.next} />
      </div>
    </>
  );

  const consumptionSection = (
    <>
      <h3>吧台消耗统计</h3>
      <div className="admin-form-row" data-testid="inventory-bar-consumption-form">
        <label>开始日期<input type="date" data-testid="inventory-bar-consumption-start" value={consumptionStart} onChange={(event) => setConsumptionStart(event.target.value)} /></label>
        <label>结束日期<input type="date" data-testid="inventory-bar-consumption-end" value={consumptionEnd} onChange={(event) => setConsumptionEnd(event.target.value)} /></label>
        <button type="button" data-testid="inventory-bar-consumption-query" disabled={!consumptionStart || !consumptionEnd || consumptionLoading} onClick={() => void queryConsumption()}>查询</button>
      </div>
      {consumptionError ? <div className="admin-inline-error" data-testid="inventory-bar-consumption-error">{consumptionError}</div> : null}
      {consumptionRows ? (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="inventory-bar-consumption-table">
            <thead><tr><th>酒吧</th><th>销售形式</th><th>套餐</th><th>酒吧商品</th><th>库存商品规格</th><th>{TEXT.quantity}</th><th>{TEXT.pourVolume}</th><th>{TEXT.lossVolume}</th></tr></thead>
            <tbody>{consumptionRows.map((row, index) => <tr key={index} data-testid={`inventory-bar-consumption-row-${index}`}><td>{row.barName ?? row.barId}</td><td>{KEY_VALUE_LABELS.sale_form?.[row.saleForm] ?? row.saleForm}</td><td>{row.comboName ?? "-"}</td><td>{row.productName ?? row.productId}</td><td>{row.itemName ?? "-"}</td><td>{row.quantity} {SALE_FORM_UNIT[row.saleForm] ?? ""}</td><td>{row.pourVolumeMl ?? "-"}</td><td>{row.lossVolumeMl}</td></tr>)}</tbody>
          </table>
        </div>
      ) : null}
    </>
  );

  const focusCount = kpiFocus ? (kpiFocus.table === "items" ? items.length : kpiFocus.table === "balances" ? balances.length : movements.length) : 0;

  return (
    <section data-testid="inventory-ledger-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="inventory-ledger-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="inventory.item.viewed / inventory.stock_balance.viewed / inventory.outbound_verified" />
      <InventoryQuarantinePanel api={api} canRead={canReadQuarantine} canManage={canManageQuarantine} onChanged={error ? undefined : reload} />
      {canManageWarehouse ? <InventoryWarehousePanel api={api} setActionResult={setActionResult} /> : null}
      <AdminDataState loading={loading} error={error} empty={items.length === 0 && balances.length === 0 && movements.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            {/* 聚焦视图：只渲染被点 KPI 对应那张承载台账，其余表/表单/KPI 卡全隐藏，感知上=新页面。 */}
            <div className="admin-state" data-testid="inventory-kpi-focus-banner">
              正在查看：{kpiFocusTitle(kpiFocus)}（{focusCount}）
              <button type="button" data-testid="inventory-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus.table === "items" ? itemsSection : kpiFocus.table === "balances" ? balancesSection : movementsSection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="inventory-ledger-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>{TEXT.items}</span>
                <button type="button" className="admin-kpi-value" data-testid="inventory-kpi-items" onClick={() => enterKpiFocus("items")}>{items.length}</button>
                <small>{TEXT.itemName}</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>{TEXT.balances}</span>
                <button type="button" className="admin-kpi-value" data-testid="inventory-kpi-balances" onClick={() => enterKpiFocus("balances")}>{balances.length}</button>
                <small>{TEXT.totalQuantity}: {totalQuantity}</small>
              </div>
              <div className="admin-kpi-card admin-kpi-card--clickable">
                <span>{TEXT.movements}</span>
                <button type="button" className="admin-kpi-value" data-testid="inventory-kpi-movements" onClick={() => enterKpiFocus("movements")}>{movements.length}</button>
                <small>{TEXT.time}</small>
              </div>
              {/* V296 本月出库成本汇总小卡（分红成本侧同一取数点）；重型成本报表归 KPI 明细页体系，此处只给合计。 */}
              <div className="admin-kpi-card" data-testid="inventory-cost-summary-card">
                <span>{TEXT.monthlyCost}</span>
                <strong data-testid="inventory-cost-summary-total">{costSummary ? fmtCostCent(costSummary.outboundTotalCostCent) : "—"}</strong>
                <small>{costSummary ? `出库 ${costSummary.outboundMovementCount} 笔 · ${TEXT.noCostBasis} ${costSummary.noCostBasisCount} 笔` : TEXT.costPending}</small>
              </div>
              {/* 负库存禁止/高价值酒水在线核销=策略与状态说明，无独立承载表→不可点。 */}
              <div className="admin-kpi-card"><span>{TEXT.noNegative}</span><strong>必需</strong><small>{TEXT.quantityInvalid}</small></div>
              <div className="admin-kpi-card"><span>{TEXT.highValueOnline}</span><strong>在线</strong><small>{selectedItem?.itemName ?? selectedItem?.itemCode ?? "-"}</small></div>
            </div>

            {itemsSection}
            {balancesSection}
            {movementsSection}
            {canViewConsumption ? consumptionSection : null}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
