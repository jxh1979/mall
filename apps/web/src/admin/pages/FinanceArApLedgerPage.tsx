import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useSortedRows } from "../components/useSortedRows";
import type { AdminPageProps, FinanceLedgerRow } from "../types";

// 后端写权限码（与 FinanceController.requirePermission 对齐）
const PAYMENT_PERMISSION = "finance.payment.register";

export function FinanceArApLedgerPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<FinanceLedgerRow[]>([]);
  const { setSelectedId, selected } = useSelectedRow(rows);
  const [amount, setAmount] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [exporting, setExporting] = React.useState(false);
  const [query, setQuery] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<FinanceLedgerRow[]>("/api/v1/finance/ar-ap-items"));
    },
    { onError: setGlobalError, fallbackMessage: "应收应付台账读取失败", onStart: () => setGlobalError(null) },
  );

  const filteredRows = rows.filter((row) => rowMatchesKeyword(row, query));
  const sort = useSortedRows(filteredRows, {
    voucher: (row) => row.billNo ?? row.sourceDoc,
    direction: (row) => row.direction,
    status: (row) => row.status,
  });
  const paged = usePagedRows(sort.sorted, 10, `${query}:${sort.key}:${sort.direction}`);

  function validatePayment() {
    if (!selected?.id) {
      setFormError("请先在台账中选择要核销/调账的凭证。");
      return false;
    }
    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setFormError("核销金额必须是大于 0 的数字。");
      return false;
    }
    if (!reason.trim()) {
      setFormError("核销或调整必须填写原因。");
      return false;
    }
    setFormError("");
    return true;
  }

  async function submitPayment() {
    if (!selected?.id) {
      setFormError("请先在台账中选择要核销/调账的凭证。");
      return;
    }
    const numericAmount = Number(amount);
    setSubmitting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.post(`/api/v1/finance/ar-ap-items/${selected.id}/payments`, {
        amount: Math.round(numericAmount * 100),
        reason,
      });
      setActionResult({ action: "ledger_payment", status: "partial", ledgerItemId: selected.id, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "核销/调账登记失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "ledger_payment", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }


  async function exportLedger() {
    setExporting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.get<Record<string, unknown>>("/api/v1/finance/ar-ap-items/export");
      setActionResult({ action: "ledger_export", status: "exported", result });
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "应收应付导出失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "ledger_export", status: "failed", message });
    } finally {
      setExporting(false);
    }
  }

  return (
    <section data-testid="finance-ledger-page">
      <AdminPageHeader title="应收应付台账" description="P0 完整台账支持应收、应付、部分核销、余额和调整原因留痕。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="finance-payment-amount" type="number" min={0} step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="核销金额" />
          <input data-testid="finance-payment-reason" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="调整/核销原因" />
          <HighRiskConfirm
            label="登记核销"
            impact="将对选中凭证写入部分核销/调账并落库留痕，影响应收应付余额和后续对账。"
            disabled={submitting}
            precheck={validatePayment}
            onConfirm={() => void submitPayment()}
            permissionsState={effectivePermissions}
            permissions={[PAYMENT_PERMISSION]}
            data-testid="finance-payment-submit"
          />
          <button type="button" data-testid="finance-export" disabled={exporting} onClick={() => void exportLedger()}>{exporting ? "导出中" : "导出台账"}</button>
        </div>
        {formError ? <div data-testid="finance-ledger-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-list-toolbar" data-testid="finance-ledger-filter">
          <input data-testid="finance-ledger-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索凭证 / 方向 / 状态" />
          <span className="admin-muted-action" data-testid="finance-ledger-filter-count">匹配 {filteredRows.length} / {rows.length} 条</span>
        </div>
        {filteredRows.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="finance-ledger-filter-empty">无匹配凭证</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="finance-ledger-table">
            <thead><tr><th>选择</th><th className="admin-sort-th" data-testid="finance-ledger-sort-voucher" onClick={() => sort.toggle("voucher")}>凭证{sort.indicator("voucher")}</th><th className="admin-sort-th" data-testid="finance-ledger-sort-direction" onClick={() => sort.toggle("direction")}>方向{sort.indicator("direction")}</th><th className="admin-sort-th" data-testid="finance-ledger-sort-status" onClick={() => sort.toggle("status")}>状态{sort.indicator("status")}</th></tr></thead>
            <tbody>{paged.pageRows.map((row) => (
              <tr key={row.id}>
                <td><input type="radio" data-testid={`finance-ledger-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                <td>{row.billNo ?? row.sourceDoc}</td>
                <td>{row.direction ?? "receivable"}</td>
                <td>{row.status}</td>
              </tr>
            ))}</tbody>
          </table>
          <TablePager testid="finance-ledger-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
    </section>
  );
}
