import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useSortedRows } from "../components/useSortedRows";
import type { AdminPageProps, StoredWineRow } from "../types";

// 后端写权限码（InventoryController：/saved-bottles/{id}/claim）
const STORED_WINE_CLAIM_PERMISSION = "bar.saved_bottle.manage";

const TEXT = {
  title: "存酒",
  description: "展示客人存酒资产、在线领取核销、证据附件和幂等留痕；高价值酒水保持在线校验，不做离线核销。",
  evidence: "接口证据：读取 /api/v1/inventory/saved-bottles；领取 POST /api/v1/inventory/saved-bottles/{id}/claim；请求体使用 claim_items、claim_by_phone、verification_code、evidence_attachment_ids。",
  loadFailed: "存酒数据读取失败",
  claimFailed: "存酒领取核销失败",
  noBottle: "当前没有可领取的存酒记录。",
  codeRequired: "领取前必须录入短信/人工核验码。",
  quantityInvalid: "领取数量必须大于 0。",
  totalBottles: "存酒记录",
  storedBottles: "可领取",
  totalQuantity: "剩余数量",
  selectedBottle: "选中存酒",
  onlineCheck: "在线校验",
  onlineCheckHelp: "高价值酒水不支持离线领取",
  claim: "在线领取核销",
  phone: "领取手机号",
  code: "核验码",
  quantity: "领取数量",
  note: "备注",
  boundary: "领取会写入幂等键、核验码、附件证据和审计事件；重复提交必须返回同一处理结果，避免重复扣减客户资产。",
  select: "选择",
  savedNo: "存酒号",
  customer: "客户",
  item: "酒水",
  status: "状态",
  createdAt: "时间",
  auditNote: "完整后台验收领取",
};

function numberValue(value: unknown, fallback = 0) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function StoredWinePage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<StoredWineRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [claimPhone, setClaimPhone] = React.useState("13800013800");
  const [verificationCode, setVerificationCode] = React.useState("");
  const [claimQuantity, setClaimQuantity] = React.useState("1");
  const [operatorNote, setOperatorNote] = React.useState(TEXT.auditNote);
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [query, setQuery] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const data = await api.get<StoredWineRow[]>("/api/v1/inventory/saved-bottles");
      setRows(data);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const storedCount = rows.filter((row) => (row.status ?? "") === "stored").length;
  const totalQuantity = rows.reduce((sum, row) => sum + numberValue(row.quantity), 0);

  const filteredRows = rows.filter((row) => rowMatchesKeyword(row, query));
  const sort = useSortedRows(filteredRows, {
    savedNo: (row) => row.savedNo ?? row.id,
    customer: (row) => row.customerName ?? row.customerId,
    item: (row) => row.itemName ?? row.itemId,
    quantity: (row) => numberValue(row.quantity),
    status: (row) => row.status,
    createdAt: (row) => row.createdAt,
  });
  const paged = usePagedRows(sort.sorted, 10, `${query}:${sort.key}:${sort.direction}`);

  function validateClaim() {
    if (!selected) {
      setFormError(TEXT.noBottle);
      return false;
    }
    const quantity = Number(claimQuantity);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setFormError(TEXT.quantityInvalid);
      return false;
    }
    if (!verificationCode.trim()) {
      setFormError(TEXT.codeRequired);
      return false;
    }
    return true;
  }

  async function claimStoredWine() {
    if (!selected) {
      setFormError(TEXT.noBottle);
      return;
    }
    const quantity = Number(claimQuantity);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setFormError(TEXT.quantityInvalid);
      return;
    }
    if (!verificationCode.trim()) {
      setFormError(TEXT.codeRequired);
      return;
    }
    setSubmitting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const body = {
        idempotency_key: `admin-saved-bottle-claim-${selected.id}-${quantity}`,
        operator_note: operatorNote,
        evidence_attachment_ids: ["att-saved-bottle-claim-1"],
        claim_items: [{ saved_bottle_id: selected.id, item_name: selected.itemName ?? selected.savedNo ?? selected.id, claim_quantity: quantity }],
        claim_by_phone: claimPhone,
        verification_code: verificationCode,
      };
      const result = await api.post(`/api/v1/inventory/saved-bottles/${selected.id}/claim`, body);
      setActionResult({ action: "stored_wine_claimed", savedBottleId: selected.id, requestBody: body, result });
      await reload();
    } catch (claimError) {
      const message = claimError instanceof Error ? claimError.message : TEXT.claimFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "stored_wine_claimed", savedBottleId: selected.id, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="stored-wine-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="stored-wine-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="bar.saved_bottle.viewed / bar.saved_bottle_picked_up" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="stored-wine-summary">
          <div className="admin-kpi-card"><span>{TEXT.totalBottles}</span><strong>{rows.length}</strong><small>{TEXT.totalBottles}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.storedBottles}</span><strong>{storedCount}</strong><small>{TEXT.onlineCheck}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.totalQuantity}</span><strong>{totalQuantity}</strong><small>{TEXT.quantity}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedBottle}</span><strong>{selected?.savedNo ?? "-"}</strong><small>{selected?.customerName ?? "-"}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.onlineCheck}</span><strong>必需</strong><small>{TEXT.onlineCheckHelp}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="stored-wine-claim-form">
          <label>{TEXT.phone}<input data-testid="stored-wine-claim-phone" value={claimPhone} onChange={(event) => setClaimPhone(event.target.value)} /></label>
          <label>{TEXT.code}<input data-testid="stored-wine-claim-code" value={verificationCode} onChange={(event) => setVerificationCode(event.target.value)} /></label>
          <label>{TEXT.quantity}<input data-testid="stored-wine-claim-quantity" type="number" min="1" value={claimQuantity} onChange={(event) => setClaimQuantity(event.target.value)} /></label>
          <label>{TEXT.note}<input data-testid="stored-wine-claim-note" value={operatorNote} onChange={(event) => setOperatorNote(event.target.value)} /></label>
        </div>
        <div className="admin-form-row">
          <HighRiskConfirm
            label={TEXT.claim}
            impact="将在线核销选中存酒并扣减客户资产，写入幂等键、核验码与审计留痕，重复提交返回同一结果。"
            disabled={submitting}
            precheck={validateClaim}
            onConfirm={() => void claimStoredWine()}
            permissionsState={effectivePermissions}
            permissions={[STORED_WINE_CLAIM_PERMISSION]}
            data-testid="stored-wine-claim"
          />
          <span className="admin-muted-action" data-testid="stored-wine-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="stored-wine-error" className="admin-inline-error">{formError}</div> : null}

        <div className="admin-list-toolbar" data-testid="stored-wine-filter">
          <input data-testid="stored-wine-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索存酒号 / 客户 / 酒水 / 状态" />
          <span className="admin-muted-action" data-testid="stored-wine-filter-count">匹配 {filteredRows.length} / {rows.length} 条</span>
        </div>
        {filteredRows.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="stored-wine-filter-empty">无匹配存酒记录</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="stored-wine-table">
            <thead>
              <tr><th>{TEXT.select}</th><th className="admin-sort-th" data-testid="stored-wine-sort-savedNo" onClick={() => sort.toggle("savedNo")}>{TEXT.savedNo}{sort.indicator("savedNo")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-customer" onClick={() => sort.toggle("customer")}>{TEXT.customer}{sort.indicator("customer")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-item" onClick={() => sort.toggle("item")}>{TEXT.item}{sort.indicator("item")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-quantity" onClick={() => sort.toggle("quantity")}>{TEXT.quantity}{sort.indicator("quantity")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-status" onClick={() => sort.toggle("status")}>{TEXT.status}{sort.indicator("status")}</th><th className="admin-sort-th" data-testid="stored-wine-sort-createdAt" onClick={() => sort.toggle("createdAt")}>{TEXT.createdAt}{sort.indicator("createdAt")}</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`stored-wine-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.savedNo ?? row.id}</td>
                  <td>{row.customerName ?? row.customerId ?? "-"}</td>
                  <td>{row.itemName ?? row.itemId ?? "-"}</td>
                  <td>{row.quantity ?? "-"}</td>
                  <td>{row.status ?? "-"}</td>
                  <td>{row.createdAt ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="stored-wine-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
    </section>
  );
}
