// 文件版本：V0.04
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { PermissionButton, useCan } from "../security/Can";
import { useAdminResource } from "../hooks/useAdminResource";
import { cnDate } from "../components/RowDetailDrawer";
import type { AdminPageProps, FinanceLedgerRow, FinanceRefundRow, FinanceRefundDetail, ArApPaymentRow } from "../types";

const CREATE = ["finance.refund.create"];
const CONFIRM = ["finance.refund.create", "finance.payment.register"];
const STATUS: Record<string, string> = { requested: "待办理，尚未结清", processing: "处理中，结果待核实", failed: "明确失败，仍未结清", success: "退款成功，已结清" };
const OPERATIONS: Record<string, string> = { submit: "首次发起", retry: "原单重试", query: "渠道查询", confirm: "凭证核实", execution: "经办人执行登记" };
const CHANNELS: Record<string, string> = { cash: "现金", pos: "刷卡", wechat: "微信", alipay: "支付宝", bank_transfer: "银行转账", aggregate_pay: "聚合支付", original: "原支付路径" };
const yuan = (cent: number) => (cent / 100).toFixed(2);
const stateLabel = (status: string) => STATUS[status] ?? "状态待核实";

export function FinanceRefundPanel({ api, user, effectivePermissions, setActionResult, setGlobalError, ledgerItem, onChanged }: AdminPageProps & { ledgerItem: FinanceLedgerRow | null; onChanged: () => void }) {
  const [rows, setRows] = React.useState<FinanceRefundRow[]>([]);
  const [payments, setPayments] = React.useState<ArApPaymentRow[]>([]);
  const [paymentId, setPaymentId] = React.useState("");
  const [amount, setAmount] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [deviceId, setDeviceId] = React.useState("");
  const [detail, setDetail] = React.useState<FinanceRefundDetail | null>(null);
  const [note, setNote] = React.useState("");
  const [resultStatus, setResultStatus] = React.useState("success");
  const [paymentReference, setPaymentReference] = React.useState("");
  const [evidence, setEvidence] = React.useState<{ id: string; fileName: string }[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [uploading, setUploading] = React.useState(false);
  const [errorText, setErrorText] = React.useState("");
  const [notice, setNotice] = React.useState("");
  const [paymentsLoading, setPaymentsLoading] = React.useState(false);
  const canUpload = useCan(effectivePermissions, ["evidence.attachment.create"]);
  const canReadEvidence = useCan(effectivePermissions, ["evidence.timeline.view"]);
  const refunds = useAdminResource(async () => setRows(await api.get<FinanceRefundRow[]>("/api/v1/finance/refunds")), { onError: setGlobalError, fallbackMessage: "退款台账读取失败" });

  React.useEffect(() => {
    let active = true;
    setPaymentId(""); setPayments([]); setPaymentsLoading(false);
    if (ledgerItem?.direction === "receivable") {
      setPaymentsLoading(true);
      void api.get<ArApPaymentRow[]>(`/api/v1/finance/ar-ap-items/${ledgerItem.id}/payments`).then((data) => {
        if (active) setPayments(data);
      }).catch((error: unknown) => {
        if (active) setErrorText(error instanceof Error ? error.message : "原收款流水读取失败");
      }).finally(() => { if (active) setPaymentsLoading(false); });
    }
    return () => { active = false; };
  }, [api, ledgerItem?.id, ledgerItem?.direction]);

  function fail(message: string) { setErrorText(message); return false; }
  function resetEvidence() { setNote(""); setEvidence([]); setPaymentReference(""); setResultStatus("success"); }
  async function showDetail(id: string) {
    setBusy(true); setErrorText(""); setDetail(null); resetEvidence();
    try { setDetail(await api.get<FinanceRefundDetail>(`/api/v1/finance/refunds/${id}`)); }
    catch (error) { fail(error instanceof Error ? error.message : "退款详情读取失败"); }
    finally { setBusy(false); }
  }

  async function submit(action: "create" | "query" | "retry" | "execution" | "confirm") {
    if (busy || uploading) return;
    setBusy(true); setErrorText(""); setNotice(""); setGlobalError(null);
    const selected = detail?.refund;
    try {
      const body: Record<string, unknown> = { operatorNote: note.trim(), evidenceAttachmentIds: evidence.map((item) => item.id), deviceId: deviceId.trim() || undefined };
      let updated: FinanceRefundRow;
      if (action === "create") {
        updated = await api.post<FinanceRefundRow>("/api/v1/finance/refunds", { operatorNote: reason.trim(), deviceId: deviceId.trim() || undefined, targetId: paymentId, refundAmountCent: Math.round(Number(amount) * 100), reason: reason.trim(), refundDirection: "original" });
        setAmount(""); setReason(""); setPaymentId("");
      } else {
        if (!selected) return;
        if (action === "execution") body.paymentReference = paymentReference.trim();
        if (action === "confirm") Object.assign(body, { status: resultStatus, amountCent: selected.refundAmountCent, paymentReference: execution?.paymentReference, paymentOperatorId: execution?.paymentOperatorId });
        if (action === "query") updated = await api.post<FinanceRefundRow>(`/api/v1/finance/refunds/${selected.id}/query`, body);
        else if (action === "retry") updated = await api.post<FinanceRefundRow>(`/api/v1/finance/refunds/${selected.id}/retry`, body);
        else if (action === "execution") updated = await api.post<FinanceRefundRow>(`/api/v1/finance/refunds/${selected.id}/execution`, body);
        else updated = await api.post<FinanceRefundRow>(`/api/v1/finance/refunds/${selected.id}/confirm`, body);
      }
      setNotice(stateLabel(updated.status));
      setActionResult({ action: `finance_refund_${action}`, refundId: updated.id, status: updated.status });
      // 先用服务端写入结果替换旧状态，详情回读失败也不能把已成功退款恢复为可重试。
      setDetail({ refund: updated, attempts: [] }); resetEvidence();
      await refunds.reload();
      try { setDetail(await api.get<FinanceRefundDetail>(`/api/v1/finance/refunds/${updated.id}`)); }
      catch { fail("操作结果已记录，办理记录读取失败，请刷新后查看。"); }
      onChanged();
    } catch (error) {
      const message = error instanceof Error ? error.message : "退款操作失败，请先查询原单结果";
      fail(message); setGlobalError(message);
    } finally { setBusy(false); }
  }

  async function upload(files: FileList | null) {
    if (!files?.length || !canUpload || busy) return;
    setUploading(true); setErrorText("");
    try {
      for (const file of Array.from(files)) {
        const form = new FormData(); form.append("file", file); form.append("business_type", "finance_refund");
        if (detail) form.append("business_id", detail.refund.id);
        form.append("attachment_role", "payment_receipt");
        if (!api.postForm) throw new Error("当前客户端不支持凭证上传");
        const saved = await api.postForm<{ id: string; fileName?: string }>("/api/v1/evidence/attachments/upload", form);
        setEvidence((previous) => [...previous, { id: saved.id, fileName: saved.fileName ?? file.name }]);
      }
    } catch (error) { fail(error instanceof Error ? error.message : "退款凭证上传失败"); }
    finally { setUploading(false); }
  }

  const selected = detail?.refund;
  const latestExecution = detail?.attempts.slice().reverse().find((attempt) => ["submit", "retry", "execution"].includes(attempt.operation));
  const execution = latestExecution?.operation === "execution" ? latestExecution : undefined;
  const projectUnresolved = !!selected && selected.sourceType !== "ar_ap_payment" && !selected.projectId?.trim();
  const mayExecute = !projectUnresolved && (selected?.status === "requested" || selected?.status === "failed");
  const mayRetry = mayExecute && selected?.sourceType === "ar_ap_payment" && !!selected.originalPaymentRef?.trim() && !["cash", "pos"].includes(selected.refundChannel);
  const mayConfirm = !projectUnresolved && selected?.status === "processing" && !!execution && execution.paymentOperatorId !== user.id && !(selected.refundChannel === "cash" && selected.requestedBy === user.id);
  const blocked = busy || uploading;
  const closed = selected?.status === "success";
  const payment = payments.find((item) => item.id === paymentId);
  function validateCreate() {
    if (!payment || ledgerItem?.direction !== "receivable") return fail("请先选择应收台账及其原收款流水。");
    const amountCent = Math.round(Number(amount) * 100);
    if (!/^\d+(\.\d{1,2})?$/.test(amount) || !Number.isSafeInteger(amountCent) || amountCent <= 0 || amountCent > payment.amountCent) return fail("退款金额须大于零、最多两位小数，且不超过原收款金额。");
    if (!reason.trim()) return fail("请填写退款原因。");
    if (["cash", "pos"].includes(payment.paymentMethod) && !deviceId.trim()) return fail("现金或刷卡原路退款须填写当前受管设备编号。");
    return true;
  }
  function validateExecution() {
    if (!mayExecute) return false;
    if (!note.trim() || !paymentReference.trim() || evidence.length === 0) return fail("请填写办理说明、原路执行的支付流水号，并上传本人实际执行退款的凭证。");
    if (selected && ["cash", "pos"].includes(selected.refundChannel) && !deviceId.trim()) return fail("现金或刷卡执行须填写当前受管设备编号。");
    return true;
  }
  function validateConfirm() {
    if (!selected || !mayConfirm) return false;
    if (!note.trim() || !execution?.paymentReference || !execution?.paymentOperatorId || evidence.length === 0) return fail("请填写办理说明并上传真实核实凭证；原路执行记录须先由实际支付经办人本人登记。");
    if (execution.paymentOperatorId === user.id || (selected.refundChannel === "cash" && selected.requestedBy === user.id)) return fail("实际支付经办人与核实人必须分离；现金退款申请人也不能自行核实。");
    if (["cash", "pos"].includes(selected.refundChannel) && !deviceId.trim()) return fail("现金或刷卡核实须填写当前受管设备编号。");
    return true;
  }

  return <section aria-label="退款办理" data-testid="finance-refund-panel">
    <h2>退款办理</h2>
    <p>先选择上方应收台账，再选择原收款流水。结果未明确时查询原单；明确失败才在原单重试，实际退款成功才结清。</p>
    {errorText ? <p role="alert" className="admin-inline-error">{errorText}</p> : null}
    {notice ? <p role="status">{notice}</p> : null}
    <fieldset disabled={blocked || paymentsLoading}>
      <legend>发起原路退款</legend>
      <div className="admin-form-row">
        <label>原收款流水<select value={paymentId} onChange={(event) => setPaymentId(event.target.value)}><option value="">{paymentsLoading ? "正在读取流水" : "请选择原收款流水"}</option>{payments.map((item) => <option key={item.id} value={item.id}>{item.id} · {CHANNELS[item.paymentMethod] ?? "其他支付渠道"} · ¥{yuan(item.amountCent)}</option>)}</select></label>
        <label>退款金额（元）<input type="number" min="0.01" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} /></label>
        <label>退款原因<input value={reason} onChange={(event) => setReason(event.target.value)} /></label>
        <HighRiskConfirm label="发起原路退款" impact="按所选原收款流水退回，未结退款继续占用可退额度。" permissionsState={effectivePermissions} permissions={CREATE} disabled={blocked || paymentsLoading} precheck={validateCreate} onConfirm={() => void submit("create")} />
      </div>
    </fieldset>
    <label>受管设备编号（现金或刷卡必填）<input value={deviceId} onChange={(event) => setDeviceId(event.target.value)} disabled={blocked} /></label>
    <AdminDataState loading={refunds.loading} error={refunds.error} empty={rows.length === 0} onRetry={refunds.reload}>
      <div className="admin-table-scroll"><table className="admin-table"><caption>退款台账</caption><thead><tr><th>退款单号</th><th>原收款、渠道回流或储值账户</th><th>金额（元）</th><th>状态</th><th>办理</th></tr></thead><tbody>{rows.map((row) => <tr key={row.id}><td>{row.id}</td><td>{row.targetId}</td><td>{yuan(row.refundAmountCent)}</td><td>{stateLabel(row.status)}</td><td><button type="button" disabled={blocked} aria-label={`查看退款 ${row.id}`} onClick={() => void showDetail(row.id)}>查看及办理</button></td></tr>)}</tbody></table></div>
    </AdminDataState>
    {detail && selected ? <section data-testid="finance-refund-detail" aria-label="退款详情">
      <h3>退款单 {selected.id}</h3><p>{stateLabel(selected.status)}</p>
      <p>金额：¥{yuan(selected.refundAmountCent)}；原收款流水：{selected.originalPaymentRef ?? "待核实"}；原因：{selected.reason}</p>
      {projectUnresolved ? <p>待核实项目归属</p> : ["ticket_payment_rejection", "channel_ticket_rejection"].includes(selected.sourceType ?? "") ? <p>停售拒收款：先核对原收款和渠道订单，按原渠道办理；真实退款凭证经另一人核实成功后才结清。</p> : selected.sourceType === "stored_value_account" && !selected.originalPaymentRef ? <p>该笔储值退款须按原收款路径办理并核实结果。</p> : null}
      <fieldset disabled={blocked || closed}>
        <legend>查询、重试与核实</legend>
        <div className="admin-form-row"><label>办理说明<input value={note} onChange={(event) => setNote(event.target.value)} /></label>
          <PermissionButton permissionsState={effectivePermissions} permissions={["finance.ar_ap.view"]} disabled={blocked || !selected || !["processing", "failed"].includes(selected.status) || !!execution} onClick={() => void submit("query")}>查询渠道结果</PermissionButton>
          <HighRiskConfirm label="在原单重试" impact="仅再次办理该退款单，不另建退款、不释放占用额度。" disabled={blocked || !mayRetry} permissionsState={effectivePermissions} permissions={CREATE} precheck={() => note.trim() ? true : fail("请填写重试说明。")} onConfirm={() => void submit("retry")} />
        </div>
        <p>本人实际按原路执行退款后，先登记凭证，再由另一人核实结果。渠道正在处理且结果未知时，须继续查询，不能另行付款。</p>
        <div className="admin-form-row">
          <label>核实结果<select value={resultStatus} onChange={(event) => setResultStatus(event.target.value)}><option value="success">实际退款成功</option><option value="failed">明确退款失败</option></select></label>
          <label>实际支付经办人账号编号<input value={execution?.paymentOperatorId ?? ""} readOnly /></label>
          <label>支付或渠道结果流水号<input value={mayExecute ? paymentReference : execution?.paymentReference ?? ""} readOnly={!mayExecute} onChange={(event) => setPaymentReference(event.target.value)} /></label>
          <label>上传退款凭证<input type="file" multiple disabled={!canUpload || blocked} onChange={(event) => { void upload(event.target.files); event.target.value = ""; }} /></label>
          <HighRiskConfirm label="登记本人已执行退款" impact="记录本人已按原收款路径执行退款及凭证，仍待另一人核实，不代表退款已结清。" permissionsState={effectivePermissions} permissions={CREATE} disabled={blocked || !mayExecute} precheck={validateExecution} onConfirm={() => void submit("execution")} />
          <HighRiskConfirm label="核实退款结果" impact={`将依据上传凭证记录${resultStatus === "success" ? "实际退款成功并结清" : "明确失败，保留未结退款"}，核实人与实际支付经办人必须分离。`} permissionsState={effectivePermissions} permissions={CONFIRM} disabled={blocked || !mayConfirm} precheck={validateConfirm} onConfirm={() => void submit("confirm")} />
        </div>
        {uploading ? <p role="status">正在上传凭证</p> : null}
        <ul>{evidence.map((item) => <li key={item.id}>{item.fileName}<button type="button" disabled={blocked} onClick={() => setEvidence((previous) => previous.filter((entry) => entry.id !== item.id))}>移除此凭证</button></li>)}</ul>
      </fieldset>
      <h4>办理记录</h4>
      {detail.attempts.length === 0 ? <p>暂无办理记录</p> : <ol>{detail.attempts.map((attempt) => <li key={attempt.id}>{OPERATIONS[attempt.operation] ?? "办理记录"} · {stateLabel(attempt.status)} · {cnDate(attempt.createdAt)}{attempt.paymentReference ? ` · 流水号：${attempt.paymentReference}` : ""}{attempt.paymentOperatorId ? ` · 支付经办人：${attempt.paymentOperatorId}` : ""}{attempt.verifiedBy ? ` · 核实人：${attempt.verifiedBy}` : ""}{canReadEvidence ? (attempt.evidenceAttachmentIds ?? []).map((id, index) => <a key={id} href={`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`} target="_blank" rel="noreferrer">查看凭证 {index + 1}</a>) : null}</li>)}</ol>}
    </section> : null}
  </section>;
}
