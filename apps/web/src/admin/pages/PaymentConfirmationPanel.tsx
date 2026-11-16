// 文件版本：V0.01
import React from "react";
import { withBase } from "../../basePath";
import { ApiError } from "../../shared/apiError";
import { cnDate } from "../components/RowDetailDrawer";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";
import { LINE_LABELS } from "./ApprovalChainPage";

// 第十八批 AM 段第 241 条 241-R2（BSM:1803「付款确认按全平台统一的审批模型执行——发起时指定这条主线的最终审批人……财务、法务及相关部门作为平行的
// 相关部门线，各指定该部门的最终审批人……主线与各条平行线都通过才完成付款确认；具体流程按支付类型分别设定。」；第十八批裁决 55、66「每类必带财务、
// 法务两线，其他部门线由发起人按单另加」、68）：通用付款确认面板，台账应付（可勾选合发）、工资、财务退款、门票退款、存酒赔付五处共用。
// 必带线取 GET 返回的 required_line_codes（含「组织层级与审批线」页为该类另设的线，裁决 67），不在前端写死；读不到（403）整块不显示。
type Item = { confirmationId: string; amountCent: number; status: string; reviewId?: string | null; requestedBy?: string | null; requestedAt?: string | null };
type View = { paymentTypeLabel?: string | null; requiredLineCodes?: string[] | null; confirmations?: Item[] | null };
type Confirmation = { confirmationId?: string; paymentTypeLabel?: string; amountCent?: number; status?: string; reviewId?: string };
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult"> & {
  objectType: string;
  objectId: string;
  createPath: string;
  permission: string;
  extraBody?: Record<string, unknown>;
  summary?: string;
};

// 同 V629 chk_payment_confirmations_status 闭集。
const STATUS: Record<string, string> = { pending_review: "待审批", approved: "已通过（可付款）", rejected: "已驳回（可重新发起）" };
const yuan = (cent: number) => (cent / 100).toFixed(2);

export function PaymentConfirmationPanel({ api, effectivePermissions, setActionResult, objectType, objectId, createPath, permission, extraBody, summary }: Props) {
  const [view, setView] = React.useState<View | null>(null);
  const [hidden, setHidden] = React.useState(false);
  const [reason, setReason] = React.useState("");
  const [finalApprover, setFinalApprover] = React.useState("");
  const [approvers, setApprovers] = React.useState<Record<string, string>>({});
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const keys = React.useRef(new Map<string, string>());

  const load = React.useCallback(async () => {
    try {
      setView(await api.get<View>(`/api/v1/finance/payment-confirmations?object_type=${encodeURIComponent(objectType)}&object_id=${encodeURIComponent(objectId)}`));
    } catch (loadError) {
      if (loadError instanceof ApiError && loadError.detail.status === 403) setHidden(true);
      else setError(loadError instanceof Error ? loadError.message : "付款确认读取失败");
    }
  }, [api, objectType, objectId]);
  React.useEffect(() => { void load(); }, [load]);

  // 必带线未读到前不出表单，免得漏填线就提交。
  if (hidden || (!view && !error)) return null;
  const items = view?.confirmations ?? [];
  const required = view?.requiredLineCodes ?? [];
  const optional = Object.keys(LINE_LABELS).filter((code) => !required.includes(code));
  const lineName = (code: string) => LINE_LABELS[code] ?? `审批线 ${code}`;
  const approver = (code: string) => (approvers[code] ?? "").trim();
  const open = items.some((item) => item.status === "pending_review" || item.status === "approved");

  async function submit() {
    if (!reason.trim() || !finalApprover.trim() || required.some((code) => !approver(code))) {
      setError("请填写付款事由、主线最终审批人和各必带审批线的最终审批人。");
      return;
    }
    const body = {
      ...extraBody,
      reason: reason.trim(),
      finalApproverId: finalApprover.trim(),
      parallelLines: [...required, ...optional.filter((code) => approver(code))].map((lineCode) => ({ lineCode, finalApproverId: approver(lineCode) })),
    };
    // 同一意图重试沿用原提交编号；成功后作废。
    const signature = JSON.stringify([createPath, body]);
    if (!keys.current.has(signature)) keys.current.set(signature, crypto.randomUUID());
    const idempotencyKey = keys.current.get(signature)!;
    setBusy(true);
    setError("");
    try {
      const result = await api.post<Confirmation>(createPath, { ...body, idempotencyKey });
      keys.current.delete(signature);
      setActionResult({ action: "payment_confirmation_requested", idempotencyKey, result: { confirmationId: result?.confirmationId, paymentTypeLabel: result?.paymentTypeLabel,
        amountCent: result?.amountCent, status: result?.status, reviewId: result?.reviewId } });
      setReason("");
      setFinalApprover("");
      setApprovers({});
      await load();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "付款确认发起失败");
    } finally {
      setBusy(false);
    }
  }

  const input = (code: string, label: string) => (
    <label key={code}>{label}<input data-testid={`payment-confirmation-line-${code}`} placeholder="员工账号编号" value={approvers[code] ?? ""}
      onChange={(event) => setApprovers({ ...approvers, [code]: event.target.value })} /></label>
  );

  return (
    <section data-testid="payment-confirmation-panel" aria-label="付款确认">
      <h4>付款确认{view?.paymentTypeLabel ? `（${view.paymentTypeLabel}）` : ""}</h4>
      <p className="admin-muted-action">付款前须经付款确认：主线与财务、法务等各条平行线全部审批通过后才能付款。一张确认管到该笔付清，分笔付款、失败重付不必重审；被驳回的可重新发起。</p>
      {summary ? <p>{summary}</p> : null}
      {items.length === 0 ? <p>尚未发起付款确认。</p> : (
        <table className="admin-table" data-testid="payment-confirmation-table">
          <thead><tr><th>付款确认单号</th><th>金额（元）</th><th>状态</th><th>发起人</th><th>发起时间</th><th>审批</th></tr></thead>
          <tbody>{items.map((item) => (
            <tr key={item.confirmationId} data-testid={`payment-confirmation-row-${item.confirmationId}`}>
              <td>{item.confirmationId}</td><td>{yuan(item.amountCent)}</td><td>{STATUS[item.status] ?? "状态待核实"}</td><td>{item.requestedBy ?? "—"}</td>
              <td>{cnDate(item.requestedAt ?? undefined)}</td>
              <td>{item.reviewId ? <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(item.reviewId)}`)}>查看审批链</a> : "—"}</td>
            </tr>
          ))}</tbody>
        </table>
      )}
      {open ? null : (
        <div className="admin-form-row" data-testid="payment-confirmation-form">
          <label>付款事由<input data-testid="payment-confirmation-reason" value={reason} onChange={(event) => setReason(event.target.value)} /></label>
          <label>主线最终审批人<input data-testid="payment-confirmation-final-approver" placeholder="员工账号编号" value={finalApprover} onChange={(event) => setFinalApprover(event.target.value)} /></label>
          {required.map((code) => input(code, `${lineName(code)}最终审批人`))}
          {optional.map((code) => input(code, `另加${lineName(code)}最终审批人（可选）`))}
          <PermissionButton permissionsState={effectivePermissions} permissions={[permission]} disabled={busy} data-testid="payment-confirmation-submit" onClick={() => void submit()}>发起付款确认</PermissionButton>
        </div>
      )}
      {error ? <p role="alert" className="admin-inline-error" data-testid="payment-confirmation-error">{error}</p> : null}
    </section>
  );
}
