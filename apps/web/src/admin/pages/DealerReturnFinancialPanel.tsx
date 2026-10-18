// 文件版本：V0.01
import React from "react";
import { PermissionButton, useCan } from "../security/Can";
import type { AdminPageProps } from "../types";
import { FinanceRefundPanel } from "./FinanceRefundPanel";

const money = (value = 0) => `¥${(value / 100).toFixed(2)}`;
const key = () => `return-fin-${Date.now()}-${Math.random().toString(16).slice(2)}`;
type Props = AdminPageProps & { returnId: string; status?: string; refundApproved?: boolean; bearer?: string; refundable?: boolean | null; reasonType?: string; contract?: { returnFreightBearer?: string | null; firstLegFreightRefundable?: boolean | null } | null; onChanged: () => void };
type Refund = { id: string; component: string; amountCent: number; status: string; financeRefundId?: string | null };
type Facts = { goodsAmountCent: number; goodsOffsetCent: number; goodsRefundCent: number; goodsRefundedCent: number; freightAdjustmentId?: string | null; freightStatus?: string | null; firstLegAdjustmentCent: number; firstLegOffsetCent: number; firstLegRefundCent: number; firstLegRefundedCent: number; refunds: Refund[] };
const state: Record<string, string> = { pending_approval: "审批中", approved: "已批准", applied: "已登记调减", processing: "退款处理中", failed: "退款失败", refunded: "退款成功", requested: "财务待办理", rejected: "已驳回" };

export function ReturnEvidenceUpload({ api, effectivePermissions, type, id, permission, onChanged, testId }: Pick<AdminPageProps, "api" | "effectivePermissions"> & { type: string; id: string; permission: string; onChanged: (ids: string[]) => void; testId: string }) {
  const [files, setFiles] = React.useState<{ id: string; name: string }[]>([]);
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const can = useCan(effectivePermissions, [permission, "evidence.attachment.create"]);
  async function upload(selected: FileList | null) {
    if (!selected?.length || busy || !can) return;
    setBusy(true); setError("");
    try {
      if (!api.postForm) throw new Error("当前客户端不能上传凭证");
      const next = [...files];
      for (const file of Array.from(selected)) {
        const form = new FormData(); form.append("file", file); form.append("business_type", type); form.append("business_id", id); form.append("attachment_role", "other");
        const saved = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", form);
        next.push({ id: saved.id, name: file.name });
        setFiles([...next]); onChanged(next.map(row => row.id));
      }
    } catch (e) { setError(e instanceof Error ? e.message : "上传失败"); }
    finally { setBusy(false); }
  }
  return <div><label>上传实际业务凭证<input type="file" accept="image/*,video/*" multiple disabled={!can || busy} data-testid={testId} onChange={e => { void upload(e.target.files); e.target.value = ""; }} /></label><ul>{files.map(file => <li key={file.id}>{file.name}</li>)}</ul>{error ? <p role="alert">{error}</p> : null}</div>;
}

export function DealerReturnFinancialPanel(props: Props) {
  const { api, returnId, effectivePermissions, onChanged } = props;
  const [facts, setFacts] = React.useState<Facts | null>(null);
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const lockedBearer = props.reasonType === "quality_issue" ? "sales_entity" : props.contract?.returnFreightBearer;
  const [bearer, setBearer] = React.useState((lockedBearer && lockedBearer !== "pending_confirmation" ? lockedBearer : props.bearer) === "sales_entity" ? "sales_entity" : "dealer");
  const [refundable, setRefundable] = React.useState(props.contract?.firstLegFreightRefundable ?? props.refundable ?? false);
  const [amount, setAmount] = React.useState("");
  const [note, setNote] = React.useState("");
  const [evidence, setEvidence] = React.useState<string[]>([]);
  const [financeId, setFinanceId] = React.useState<string | null>(null);
  const canFinance = useCan(effectivePermissions, ["finance.ar_ap.view"]);
  async function load() { try { setFacts(await api.get<Facts>(`/api/v1/dealers/returns/${encodeURIComponent(returnId)}/financial`)); } catch (e) { setError(e instanceof Error ? e.message : "款项读取失败"); } }
  React.useEffect(() => { void load(); }, [returnId, api]);
  async function post(path: string, body: object) {
    if (busy) return; setBusy(true); setError("");
    try { await api.post(path, { idempotencyKey: key(), ...body }); await load(); onChanged(); }
    catch (e) { setError(e instanceof Error ? e.message : "办理失败"); }
    finally { setBusy(false); }
  }
  function confirm() {
    const cent = Math.round(Number(amount) * 100);
    if (amount.trim() === "" || !Number.isSafeInteger(cent) || cent < 0 || (!refundable && cent !== 0) || !evidence.length || !note.trim()) { setError("请填写明确金额、说明并上传双方确认或原合同凭证；不退首程费时填0。"); return; }
    void post(`/api/v1/dealers/returns/${encodeURIComponent(returnId)}/freight-confirmation`, { returnFreightBearer: bearer, firstLegFreightRefundable: refundable, firstLegAdjustmentCent: cent, note, evidenceAttachmentIds: evidence });
  }
  return <section data-testid="return-financial"><h4>退货款项与首程运费</h4>{error ? <p role="alert">{error}</p> : null}
    {facts ? <><table className="admin-table"><thead><tr><th>项目</th><th>核定减款</th><th>冲减未收款</th><th>应退现金</th><th>实际已退</th></tr></thead><tbody>
      <tr><td>商品货款</td><td>{money(facts.goodsAmountCent)}</td><td>{money(facts.goodsOffsetCent)}</td><td>{money(facts.goodsRefundCent)}</td><td>{money(facts.goodsRefundedCent)}</td></tr>
      <tr><td>首程运费（{state[facts.freightStatus ?? ""] ?? "待核定"}）</td><td>{money(facts.firstLegAdjustmentCent)}</td><td>{money(facts.firstLegOffsetCent)}</td><td>{money(facts.firstLegRefundCent)}</td><td data-testid="first-leg-refunded">{money(facts.firstLegRefundedCent)}</td></tr>
    </tbody></table>
    {!facts.freightAdjustmentId && !["pending_approval", "pending_receipt", "inspecting", "rejected"].includes(props.status ?? "") ? <div data-testid="return-freight-form">
      <p>质量退回运费由销售主体承担。首程费按原锁定合同或双方确认办理，原包邮不因部分退货补收。</p>
      <label>退回运费承担方<select data-testid="freight-bearer" value={bearer} disabled={!!lockedBearer && lockedBearer !== "pending_confirmation"} onChange={e => setBearer(e.target.value)}><option value="dealer">经销商</option><option value="sales_entity">销售主体</option></select></label>
      <label><input data-testid="freight-first-leg" type="checkbox" checked={refundable} disabled={props.contract?.firstLegFreightRefundable != null} onChange={e => setRefundable(e.target.checked)} />首程运费退还</label>
      <label>本次首程减款金额（元）<input data-testid="freight-amount" inputMode="decimal" value={amount} onChange={e => setAmount(e.target.value)} /></label>
      <label>金额和合同依据说明<input data-testid="freight-note" value={note} onChange={e => setNote(e.target.value)} /></label>
      <ReturnEvidenceUpload key={returnId} api={api} effectivePermissions={effectivePermissions} type="dealer_return" id={returnId} permission="dealer.return.manage" testId="freight-evidence" onChanged={setEvidence} />
      <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.return.manage"]} disabled={busy} data-testid="freight-confirm" onClick={confirm}>确认并提交运费减款审批</PermissionButton>
    </div> : null}
    <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.return.refund.execute"]} disabled={busy || props.status === "pending_reversal" || (!props.refundApproved && !facts.firstLegRefundCent)} data-testid="return-refund-prepare" onClick={() => void post(`/api/v1/dealers/returns/${encodeURIComponent(returnId)}/refunds/execute`, {})}>交财务办理原路退款</PermissionButton>
    <p>提交仅创建财务待办，实际退款成功后才更新已退金额。</p>
    {(facts.refunds ?? []).map(refund => <p key={refund.id}>{refund.component === "first_leg" ? "首程运费" : "商品货款"} {money(refund.amountCent)}：{state[refund.status] ?? refund.status}{refund.financeRefundId ? <PermissionButton permissionsState={effectivePermissions} permissions={["finance.ar_ap.view"]} onClick={() => setFinanceId(refund.financeRefundId!)}>查看财务办理</PermissionButton> : "，尚未创建财务待办"}</p>)}
    {financeId && canFinance ? <FinanceRefundPanel {...props} ledgerItem={null} initialRefundId={financeId} onChanged={() => { void load(); onChanged(); }} /> : null}
    </> : <p>正在读取款项…</p>}
  </section>;
}

export function DealerInvoiceActions(props: AdminPageProps & { invoice: { id: string; status?: string; requiresReversal?: boolean; reversalStatus?: string | null }; onChanged: () => void }) {
  const { invoice, api, effectivePermissions, onChanged } = props;
  const [ids, setIds] = React.useState<string[]>([]);
  const [reference, setReference] = React.useState("");
  const [result, setResult] = React.useState("processing");
  const [failure, setFailure] = React.useState("");
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const issue = ["pending", "issuing", "failed"].includes(invoice.status ?? "");
  const reversal = invoice.status === "issued" && invoice.requiresReversal;
  if (!issue && !reversal) return null;
  async function save() {
    if (!reference.trim() || !ids.length || (reversal && result === "failed" && !failure.trim())) { setError("请填写实际发票号或红冲业务编号，并上传实际结果凭证。"); return; }
    setBusy(true);setError("");
    try { await api.post(`/api/v1/dealers/invoices/${encodeURIComponent(invoice.id)}/${issue ? "issue" : "reversal-result"}`, { idempotencyKey: key(), evidenceAttachmentIds: ids, ...(issue ? { invoiceNo: reference } : { result, referenceNo: reference, failureReason: failure || undefined }) }); onChanged(); }
    catch (e) { setError(e instanceof Error ? e.message : "结果登记失败"); }
    finally { setBusy(false); }
  }
  return <div><label>{issue ? "实际发票号" : "实际红冲业务编号"}<input data-testid={`invoice-reference-${invoice.id}`} value={reference} onChange={e => setReference(e.target.value)} /></label>
    {reversal ? <><p>原票仍按原金额保留；实际红冲成功后才生成重开申请。</p><select aria-label="实际红冲结果" value={result} onChange={e => setResult(e.target.value)}><option value="processing">处理中</option><option value="failed">明确失败</option><option value="success">实际成功</option></select>{result === "failed" ? <input aria-label="红冲失败原因" value={failure} onChange={e => setFailure(e.target.value)} /> : null}</> : null}
    <ReturnEvidenceUpload api={api} effectivePermissions={effectivePermissions} type="dealer_invoice" id={invoice.id} permission="dealer.invoice.manage" testId={`invoice-evidence-${invoice.id}`} onChanged={setIds} />
    <PermissionButton permissionsState={effectivePermissions} permissions={["dealer.invoice.manage"]} disabled={busy} data-testid={`invoice-issue-${invoice.id}`} onClick={() => void save()}>{issue ? "登记实际开具" : "登记实际红冲结果"}</PermissionButton>{error ? <p role="alert">{error}</p> : null}
  </div>;
}
