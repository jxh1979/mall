import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, InventoryItemRow, StockBalanceRow, StockMovementRow } from "../types";

// 后端写权限码（InventoryController：POST /stock-movements）
const STOCK_MOVE_PERMISSION = "inventory.stock.move";

const TEXT = {
  title: "库存台账",
  description: "集中查看物料、库存余额和库存流水，支持按 OpenAPI 契约登记库存移动；负库存禁止，高价值酒水保持在线核销。",
  evidence: "接口证据：读取 /api/v1/inventory/items、/api/v1/inventory/stock-balances、/api/v1/inventory/stock-movements；登记 POST /api/v1/inventory/stock-movements。",
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
};

function asNumber(value: unknown, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function InventoryLedgerPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [items, setItems] = React.useState<InventoryItemRow[]>([]);
  const [balances, setBalances] = React.useState<StockBalanceRow[]>([]);
  const [movements, setMovements] = React.useState<StockMovementRow[]>([]);
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

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [itemRows, balanceRows, movementRows] = await Promise.all([
        api.get<InventoryItemRow[]>("/api/v1/inventory/items"),
        api.get<StockBalanceRow[]>("/api/v1/inventory/stock-balances"),
        api.get<StockMovementRow[]>("/api/v1/inventory/stock-movements"),
      ]);
      setItems(itemRows);
      setBalances(balanceRows);
      setMovements(movementRows);
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

  return (
    <section data-testid="inventory-ledger-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="inventory-ledger-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="inventory.item.viewed / inventory.stock_balance.viewed / inventory.outbound_verified" />
      <AdminDataState loading={loading} error={error} empty={items.length === 0 && balances.length === 0 && movements.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="inventory-ledger-summary">
          <div className="admin-kpi-card"><span>{TEXT.items}</span><strong>{items.length}</strong><small>{TEXT.itemName}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.balances}</span><strong>{balances.length}</strong><small>{TEXT.totalQuantity}: {totalQuantity}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.movements}</span><strong>{movements.length}</strong><small>{TEXT.time}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.noNegative}</span><strong>必需</strong><small>{TEXT.quantityInvalid}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.highValueOnline}</span><strong>在线</strong><small>{selectedItem?.itemName ?? selectedItem?.itemCode ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="inventory-movement-form">
          <label>{TEXT.fromLocation}<input data-testid="inventory-movement-from" value={fromLocation} onChange={(event) => setFromLocation(event.target.value)} /></label>
          <label>{TEXT.toLocation}<input data-testid="inventory-movement-to" value={toLocation} onChange={(event) => setToLocation(event.target.value)} /></label>
          <label>{TEXT.quantity}<input data-testid="inventory-movement-quantity" type="number" min="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} /></label>
          <label>{TEXT.traceCode}<input data-testid="inventory-movement-trace" value={traceCode} onChange={(event) => setTraceCode(event.target.value)} /></label>
          <label>{TEXT.note}<input data-testid="inventory-movement-note" value={operatorNote} onChange={(event) => setOperatorNote(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.submit}
            impact="将按选中物料登记库存移动并落库，影响库存余额；禁止负库存，高价值酒水保持在线核销。"
            disabled={submitting}
            precheck={validateMovement}
            onConfirm={() => void submitMovement()}
            permissionsState={effectivePermissions}
            permissions={[STOCK_MOVE_PERMISSION]}
            data-testid="inventory-movement-submit"
          />
          <span className="admin-muted-action" data-testid="inventory-ledger-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="inventory-ledger-error" className="admin-inline-error">{formError}</div> : null}

        <h3>{TEXT.items}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="inventory-ledger-items-table">
            <thead><tr><th>{TEXT.itemCode}</th><th>{TEXT.itemName}</th><th>{TEXT.category}</th><th>{TEXT.unit}</th><th>{TEXT.status}</th></tr></thead>
            <tbody>{items.map((item) => <tr key={item.id} onClick={() => setSelectedItemId(item.id)}><td>{item.itemCode ?? item.id}</td><td>{item.itemName ?? "-"}</td><td>{item.category ?? "-"}</td><td>{item.unit ?? "-"}</td><td>{formatBusinessStatus(item.status)}</td></tr>)}</tbody>
          </table>
        </div>
        <h3>{TEXT.balances}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="inventory-ledger-balances-table">
            <thead><tr><th>{TEXT.itemId}</th><th>{TEXT.location}</th><th>{TEXT.quantity}</th><th>{TEXT.status}</th></tr></thead>
            <tbody>{pagedBalances.pageRows.map((row) => <tr key={row.id}><td>{row.itemId ?? "-"}</td><td>{row.locationType ?? row.locationId ?? "-"}</td><td>{row.quantity ?? "-"}</td><td>{row.status ?? "normal"}</td></tr>)}</tbody>
          </table>
            <TablePager testid="inventory-balances-pager" page={pagedBalances.page} pageCount={pagedBalances.pageCount} total={pagedBalances.total} onPrev={pagedBalances.prev} onNext={pagedBalances.next} />
        </div>
        <h3>{TEXT.movements}</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="inventory-ledger-movements-table">
            <thead><tr><th>{TEXT.itemId}</th><th>{TEXT.quantity}</th><th>{TEXT.status}</th><th>{TEXT.time}</th></tr></thead>
            <tbody>{pagedMovements.pageRows.map((row, index) => <tr key={row.id ?? index}><td>{row.itemId ?? "-"}</td><td>{row.quantity ?? "-"}</td><td>{formatBusinessStatus(row.status)}</td><td>{row.createdAt ?? "-"}</td></tr>)}</tbody>
          </table>
            <TablePager testid="inventory-movements-pager" page={pagedMovements.page} pageCount={pagedMovements.pageCount} total={pagedMovements.total} onPrev={pagedMovements.prev} onNext={pagedMovements.next} />
        </div>
      </AdminDataState>
    </section>
  );
}
