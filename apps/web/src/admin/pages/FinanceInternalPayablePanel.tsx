// 文件版本：V0.05
import React from "react";
import type { AdminPageProps, FinanceLedgerRow } from "../types";
import { PermissionButton } from "../security/Can";
import { cnDate } from "../components/RowDetailDrawer";

// FS-021-R5（BSM L1683；ENUM L253 内部应付执行状态）：普通内部结算应付的付款时点与执行状态，财务登记已发起（在途）、
// 明确失败（付款失败待处理，注明原因与预计重付时间，不迟于下一个付账日）与资金不足顺延；到账仍走上方「登记核销」，成功才结清。
// 补偿新增履约结算（FS-012）同为普通内部结算钱路（与 InternalPayDaysPanel.ROUTE_LABELS、后端 InternalPayDayService.ROUTES 同词表）。
// 第十八批 AI 段 214-R1、214-R2：组合通票域间份额、餐饮零售代付（负额时为应付）、到期未核销归属三个钱路的应付同样登记执行状态。
// 第十八批 AV 段 310-R1（ENUM:252「押金退还付款状态按「内部应付执行状态」条执行」）：退租押金退还应付同样登记执行状态；它不受三个付账日约束，
// 没有异议截止与指定付账日，也不出资金不足顺延。
export const INTERNAL_PAYABLE_ROUTES = new Set(["ticket_distribution", "platform_incentive_subsidy", "single_bar_service_fee", "compensation_fulfillment",
  "pass_domain_share", "pass_domain_direct_settlement", "pass_unverified_attribution", "deposit_refund",
  // 第十八批 AZ 段 350-R1（BSM:1704）：通用卡消费结算应付同为普通内部结算应付。
  "general_card_consume"]);
const PAY = ["finance.payment.register"];

type Attempt = { id: string; status: string };
// 第十八批 AZ 段 358-R1（BSM:1683「处理路径为原路重付，或更换收款账户后重付」）：收款方财务负责人已审批的换账户单。
type AccountChange = { id: string; newAccountRef: string; reason: string; approvedAt: string };
type Execution = {
  executionStatus: string; inTransit: boolean; objectionDeadline?: string; designatedPayDay?: string; dueDate?: string;
  failureReason?: string | null; expectedRepayDate?: string | null; attempts: Attempt[]; accountChanges?: AccountChange[];
};
const approvedDay = (at: string) =>
  new Date(at).toLocaleDateString("zh-CN", { timeZone: "Asia/Shanghai", year: "numeric", month: "long", day: "numeric" });
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
  const [account, setAccount] = React.useState<string | null>(null);
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
  // 358-R1：换账户只在付款失败待处理时审批，有审批即收款方要求改付新账户——默认选最近审批的一条；选空＝原收款账户（原路重付）。
  const changes = state?.accountChanges ?? [];
  const payAccount = account ?? changes[changes.length - 1]?.id ?? "";
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
        {changes.length > 0 ? (
          <label>付款账户<select data-testid="internal-payable-account" value={payAccount} onChange={(e) => setAccount(e.target.value)}>
            {changes.map((c) => <option key={c.id} value={c.id}>新账户 {c.newAccountRef}（{approvedDay(c.approvedAt)} 审批，{c.reason}）</option>)}
            <option value="">原收款账户（原路重付）</option>
          </select></label>
        ) : null}
        <PermissionButton permissionsState={effectivePermissions} permissions={PAY} data-testid="internal-payable-initiate"
          disabled={busy || !state || state.inTransit || state.executionStatus === "settled" || state.executionStatus === "reversed"}
          onClick={() => void run("internal_payment_initiated", `${base}/payment-attempts`,
            { bankRef: bankRef.trim() || undefined, accountChangeId: payAccount || undefined })}>
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
      {row.sourceType === "deposit_refund" ? null : (
        <div className="admin-form-row">
          <label>说明<input data-testid="internal-payable-funds-note" value={note} onChange={(e) => setNote(e.target.value)} placeholder="账上资金不足" /></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={PAY} data-testid="internal-payable-funds-insufficient"
            disabled={busy || !note.trim() || !state || state.inTransit || state.executionStatus === "settled" || state.executionStatus === "reversed"}
            onClick={() => void run("internal_payment_postponed", `${base}/funds-insufficient`, { note: note.trim() })}>
            资金不足顺延至下一个付账日
          </PermissionButton>
        </div>
      )}
      {error ? <div className="admin-inline-error" data-testid="internal-payable-error">{error}</div> : null}
    </div>
  );
}
