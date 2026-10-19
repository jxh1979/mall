// 文件版本：V0.02
import React from "react";
import type { AdminPageProps, FinanceLedgerRow } from "../types";
import { PermissionButton } from "../security/Can";
import { cnDate } from "../components/RowDetailDrawer";

// FS-021-R5（BSM L1683；ENUM L253 内部应付执行状态）：普通内部结算应付的付款时点与执行状态，财务登记已发起（在途）、
// 明确失败（付款失败待处理，注明原因与预计重付时间，不迟于下一个付账日）与资金不足顺延；到账仍走上方「登记核销」，成功才结清。
// 补偿新增履约结算（FS-012）同为普通内部结算钱路（与 InternalPayDaysPanel.ROUTE_LABELS、后端 InternalPayDayService.ROUTES 同词表）。
export const INTERNAL_PAYABLE_ROUTES = new Set(["ticket_distribution", "platform_incentive_subsidy", "single_bar_service_fee", "compensation_fulfillment"]);
const PAY = ["finance.payment.register"];

type Attempt = { id: string; status: string };
type Execution = {
  executionStatus: string; inTransit: boolean; objectionDeadline?: string; designatedPayDay?: string; dueDate?: string;
  failureReason?: string | null; expectedRepayDate?: string | null; attempts: Attempt[];
};
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult"> & { row: FinanceLedgerRow; onChanged: () => void };

export function executionLabel(status: string, inTransit: boolean): string {
  if (status === "settled") return "已结清";
  if (status === "reversed") return "已冲正";
  if (status === "failed_pending") return "付款失败待处理";
  return inTransit ? "待付（银行在途）" : "待付";
}

export function FinanceInternalPayablePanel({ api, effectivePermissions, setActionResult, row, onChanged }: Props) {
  const [state, setState] = React.useState<Execution | null>(null);
  const [bankRef, setBankRef] = React.useState("");
  const [failure, setFailure] = React.useState("");
  const [repayDate, setRepayDate] = React.useState("");
  const [note, setNote] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const base = `/api/v1/finance/internal-payables/${encodeURIComponent(row.id)}`;

  const reload = React.useCallback(async () => {
    setState(await api.get<Execution>(base));
  }, [api, base]);
  React.useEffect(() => { void reload().catch((e) => setError(e instanceof Error ? e.message : "付款执行状态读取失败")); }, [reload]);

  async function run(action: string, path: string, body: Record<string, unknown>) {
    setBusy(true);
    setError("");
    try {
      const result = await api.post(path, body);
      setActionResult({ action, status: "recorded", ledgerItemId: row.id, result });
      await reload();
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : "登记失败");
    } finally {
      setBusy(false);
    }
  }

  const inTransitAttempt = state?.attempts.find((a) => a.status === "initiated");
  return (
    <div className="admin-panel" data-testid="internal-payable-panel">
      <h4>付款执行</h4>
      <div data-testid="internal-payable-status">执行状态：{state ? executionLabel(state.executionStatus, state.inTransit) : "—"}</div>
      <div>异议截止：{cnDate(state?.objectionDeadline)}　指定付账日：{cnDate(state?.designatedPayDay)}　当前付款日：{cnDate(state?.dueDate)}</div>
      {state?.executionStatus === "failed_pending" ? (
        <div>失败原因：{state.failureReason ?? "—"}　预计重付：{cnDate(state.expectedRepayDate)}</div>
      ) : null}
      <div className="admin-form-row">
        <label>银行流水号<input data-testid="internal-payable-bank-ref" value={bankRef} onChange={(e) => setBankRef(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={PAY} data-testid="internal-payable-initiate"
          disabled={busy || !state || state.inTransit || state.executionStatus === "settled" || state.executionStatus === "reversed"}
          onClick={() => void run("internal_payment_initiated", `${base}/payment-attempts`, { bankRef: bankRef.trim() || undefined })}>
          登记已发起付款
        </PermissionButton>
      </div>
      {inTransitAttempt ? (
        <div className="admin-form-row">
          <label>失败原因<input data-testid="internal-payable-failure-reason" value={failure} onChange={(e) => setFailure(e.target.value)} /></label>
          <label>预计重付日<input data-testid="internal-payable-repay-date" type="date" value={repayDate} onChange={(e) => setRepayDate(e.target.value)} /></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={PAY} data-testid="internal-payable-fail"
            disabled={busy || !failure.trim() || !repayDate}
            onClick={() => void run("internal_payment_failed", `${base}/payment-attempts/${encodeURIComponent(inTransitAttempt.id)}/failure`,
              { failureReason: failure.trim(), expectedRepayDate: repayDate })}>
            登记明确失败
          </PermissionButton>
        </div>
      ) : null}
      <div className="admin-form-row">
        <label>说明<input data-testid="internal-payable-funds-note" value={note} onChange={(e) => setNote(e.target.value)} placeholder="账上资金不足" /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={PAY} data-testid="internal-payable-funds-insufficient"
          disabled={busy || !note.trim() || !state || state.inTransit || state.executionStatus === "settled" || state.executionStatus === "reversed"}
          onClick={() => void run("internal_payment_postponed", `${base}/funds-insufficient`, { note: note.trim() })}>
          资金不足顺延至下一个付账日
        </PermissionButton>
      </div>
      {error ? <div className="admin-inline-error" data-testid="internal-payable-error">{error}</div> : null}
    </div>
  );
}
