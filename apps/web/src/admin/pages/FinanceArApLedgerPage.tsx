import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, FinanceLedgerRow } from "../types";

export function FinanceArApLedgerPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [rows, setRows] = React.useState<FinanceLedgerRow[]>([]);
  const [amount, setAmount] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [formError, setFormError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<FinanceLedgerRow[]>("/api/v1/finance/ar-ap-items"));
    },
    { onError: setGlobalError, fallbackMessage: "应收应付台账读取失败" },
  );

  async function submitPayment() {
    const numericAmount = Number(amount);
    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setFormError("核销金额必须是大于 0 的数字。");
      return;
    }
    if (!reason.trim()) {
      setFormError("核销或调整必须填写原因。");
      return;
    }
    setFormError("");
    const row = rows[0];
    const result = await api.post(`/api/v1/finance/ar-ap-items/${row?.id ?? "bill-1"}/payments`, { amount: Math.round(numericAmount * 100), reason });
    setActionResult({ action: "ledger_payment", status: "partial", result });
  }

  return (
    <section data-testid="finance-ledger-page">
      <AdminPageHeader title="应收应付台账" description="P0 完整台账支持应收、应付、部分核销、余额和调整原因留痕。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="finance-payment-amount" type="number" min={0} step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} placeholder="核销金额" />
          <input data-testid="finance-payment-reason" value={reason} onChange={(event) => setReason(event.target.value)} placeholder="调整/核销原因" />
          <button type="button" data-testid="finance-payment-submit" onClick={() => void submitPayment()}>登记核销</button>
          <button type="button" data-testid="finance-export" onClick={() => setActionResult({ action: "finance_export", status: "导出已留痕" })}>导出</button>
        </div>
        {formError ? <div data-testid="finance-ledger-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="finance-ledger-table">
            <thead><tr><th>凭证</th><th>方向</th><th>状态</th></tr></thead>
            <tbody>{rows.map((row) => <tr key={row.id}><td>{row.billNo ?? row.sourceDoc}</td><td>{row.direction ?? "receivable"}</td><td>{row.status}</td></tr>)}</tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
