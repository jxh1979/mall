// 文件版本：V0.02
import React from "react";
import { withBase } from "../basePath";
import { CounterpartyConfirmationPick } from "./CounterpartyConfirmation";

export type DeliveryApi = {
  get<T>(path: string): Promise<T>;
  post<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  readFile?(path: string): Promise<Blob>;
  postForm?<T>(path: string, form: FormData): Promise<T>;
};
type Resolution = { id: string; quantity: number; resolutionType: string; physicalRoute: string; adjustmentCent: number; responsibilityParty: string; agreementBasis: string; status: string; reviewId?: string; evidenceAttachmentIds?: string[]; physicalEvidenceAttachmentIds?: string[]; physicalCompletedAt?: string | null; completedAt?: string | null };
type Difference = { id: string; orderLineId: string; discrepancyType: string; quantity: number; reservedQuantity: number; completedQuantity: number; resolutions: Resolution[] };
type Batch = { id: string; resolutionId?: string | null; shippedAt: string; evidenceAttachmentIds?: string[]; lines: { orderLineId: string; quantity: number; recordedQuantity: number; acceptedQuantity: number }[] };
type Receipt = { id: string; batchId: string; confirmedAt: string; buyerContact: string; evidenceAttachmentIds: string[]; lines: { orderLineId: string; acceptedQuantity: number; rejectedQuantity: number; damagedQuantity: number; shortQuantity: number }[] };
export type DeliveryProgress = { deliveryCompletedAt?: string | null; netReceivableCent: number; receivedCent: number; pendingRefundCent: number; moneyStatus: string; batches: Batch[]; receipts: Receipt[]; discrepancies: Difference[] };
type Props = { api: DeliveryApi; orderId: string; status?: string; portal?: boolean; canManage: boolean; canShip?: boolean; onChanged: () => Promise<void>; deactivated?: boolean; confirmationVersion?: number };
const names: Record<string, string> = { rejected: "拒收", damaged: "收到破损", short: "短少", replacement: "补交", price_adjustment: "确认调款", return_resalable: "实际退回且验收可售", return_quarantine: "实际退回并隔离", loss_confirmed: "实际损失处置", accepted_as_is: "双方确认由收货方留用", pending_approval: "待审批", approved: "已批准，待实际处理", completed: "该数量处理完成", settled: "交付差异款项已结清", unpaid: "尚有未收货款", pending_refund: "交付差异原路退款未结清" };
const money = (cent: number) => `¥${(cent / 100).toFixed(2)}`;
const stamp = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 23);
const newKey = () => `delivery-${crypto.randomUUID()}`;

export function DealerDeliveryPanel({ api, orderId, status, portal = false, canManage, canShip = false, onChanged, deactivated = false, confirmationVersion = 0 }: Props) {
  const [confirmationId, setConfirmationId] = React.useState("");
  const base = portal ? "/api/v1/dealer-portal/orders/" : "/api/v1/dealers/orders/";
  const [progress, setProgress] = React.useState<DeliveryProgress | null>(null);
  const [lines, setLines] = React.useState<{ id: string; itemId: string; spec: string }[]>([]);
  const [error, setError] = React.useState("");
  const [notice, setNotice] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [batchId, setBatchId] = React.useState("");
  const [quantities, setQuantities] = React.useState<Record<string, string[]>>({});
  const [at, setAt] = React.useState(stamp);
  const [contact, setContact] = React.useState("");
  const [proofs, setProofs] = React.useState<{ id: string; name: string }[]>([]);
  const [key, setKey] = React.useState(newKey);
  const [mode, setMode] = React.useState<{ kind: "proposal" | "physical" | "replacement"; difference?: Difference; resolution?: Resolution } | null>(null);
  const [quantity, setQuantity] = React.useState("");
  const [resolutionType, setResolutionType] = React.useState("replacement");
  const [route, setRoute] = React.useState("return_quarantine");
  const [amount, setAmount] = React.useState("");
  const [responsibility, setResponsibility] = React.useState("");
  const [basis, setBasis] = React.useState("");
  const [approver, setApprover] = React.useState("");
  const [finance, setFinance] = React.useState("");
  const load = React.useCallback(async () => {
    const [data, rows] = await Promise.all([api.get<DeliveryProgress>(`${base}${encodeURIComponent(orderId)}/delivery`), api.get<typeof lines>(`${base}${encodeURIComponent(orderId)}/lines`)]);
    setProgress(data); setLines(rows ?? []);
    setBatchId(current => data?.batches.some(b => b.id === current) ? current : data?.batches.find(b => b.lines.some(l => l.recordedQuantity < l.quantity))?.id ?? "");
  }, [api, base, orderId]);
  React.useEffect(() => { setProgress(null); setProofs([]); setMode(null); setQuantities({}); setKey(newKey()); void load().catch(e => setError(e instanceof Error ? e.message : "交付记录读取失败")); }, [load]);
  const item = (lineId: string) => { const row = lines.find(l => l.id === lineId); return row ? `${row.itemId}（${row.spec || "默认规格"}）` : lineId; };
  // 第十八批第 31 条：经销商已停用时，签收与差异方案须用掉一条对应事项的对方确认留证（签收人取其已核验联系人）。
  const confirmationMatter = deactivated && !portal ? (!mode ? "delivery_receipt" : mode.kind === "proposal" ? "delivery_resolution" : null) : null;
  function open(next: NonNullable<typeof mode>) { setMode(next); setConfirmationId(""); setProofs([]); setAt(stamp()); setKey(newKey()); setError(""); setQuantity(""); setAmount(""); setBasis(""); setResponsibility(""); }
  async function upload(selected: FileList | null) {
    if (!selected?.length || busy) return;
    if (!api.postForm) { setError("当前客户端无法上传凭证，请刷新后重试"); return; }
    setBusy(true); setError("");
    try {
      for (const file of Array.from(selected)) {
        const form = new FormData(); form.append("file", file);
        if (!portal) { form.append("business_type", "dealer_delivery"); form.append("business_id", orderId); form.append("attachment_role", "other"); }
        const uploaded = await api.postForm<{ id: string }>(portal ? `${base}${encodeURIComponent(orderId)}/delivery-evidence` : "/api/v1/evidence/attachments/upload", form);
        setProofs(previous => [...previous, { id: uploaded.id, name: file.name }]);
      }
    } catch (e) { setError(e instanceof Error ? e.message : "凭证上传失败"); } finally { setBusy(false); }
  }
  async function downloadProof(id: string) {
    if (!api.readFile) { setError("当前客户端无法读取凭证，请刷新后重试"); return; }
    try {
      const blob = await api.readFile(`${base}${encodeURIComponent(orderId)}/delivery-evidence/${encodeURIComponent(id)}`);
      const url = URL.createObjectURL(blob), link = document.createElement("a");
      link.href = url; link.download = `交付凭证.${({ "image/png": "png", "image/jpeg": "jpg", "image/webp": "webp", "image/heic": "heic", "video/mp4": "mp4", "video/quicktime": "mov", "video/webm": "webm" } as Record<string, string>)[blob.type] ?? "bin"}`; link.click(); URL.revokeObjectURL(url);
    } catch (e) { setError(e instanceof Error ? e.message : "凭证读取失败"); }
  }
  const proofLinks = (ids?: string[]) => (ids ?? []).map((id, i) => portal
    ? <button key={id} type="button" onClick={() => void downloadProof(id)}>下载凭证 {i + 1}</button>
    : <a key={id} href={`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`} target="_blank" rel="noreferrer">查看凭证 {i + 1}</a>);
  async function save() {
    if (busy) return;
    setError(""); setNotice("");
    try {
      if (!proofs.length) throw new Error("请先上传本次实际签收、双方确认或实物处理凭证");
      if (!at || !Number.isFinite(new Date(at).getTime())) throw new Error("请填写实际时间");
      if (confirmationMatter && !confirmationId) throw new Error("经销商已停用，请先选择指定经办人登记的对方确认留证");
      const evidenceAttachmentIds = proofs.map(p => p.id);
      let path: string; let body: Record<string, unknown>;
      if (mode?.kind === "proposal") {
        const qty = Number(quantity), cent = resolutionType === "replacement" ? 0 : Math.round(Number(amount) * 100);
        if (!Number.isSafeInteger(qty) || qty <= 0 || qty > (mode.difference!.quantity - mode.difference!.reservedQuantity)) throw new Error("请填写有效的差异处理数量");
        if (resolutionType === "price_adjustment" && (!/^\d+(\.\d{1,2})?$/.test(amount) || !Number.isSafeInteger(cent))) throw new Error("请填写双方确认的调减货款金额，未减款请明确填0");
        if (![responsibility, basis, approver, finance].every(v => v.trim())) throw new Error("须填写双方确认依据、责任方和主管及财务最终审批人");
        path = `/api/v1/dealers/delivery/discrepancies/${encodeURIComponent(mode.difference!.id)}/resolutions`;
        body = { quantity: qty, resolutionType, physicalRoute: route, adjustmentCent: cent, responsibilityParty: responsibility.trim(), agreementBasis: basis.trim(), finalApproverId: approver.trim(), parallelLines: [{ lineCode: "finance_review", finalApproverId: finance.trim() }] };
      } else if (mode) {
        path = `/api/v1/dealers/delivery/resolutions/${encodeURIComponent(mode.resolution!.id)}/${mode.kind === "physical" ? "physical-completion" : "replacement-shipment"}`;
        body = { completedAt: new Date(at).toISOString() };
      } else {
        const batch = progress?.batches.find(b => b.id === batchId);
        if (!batch || (!confirmationMatter && !contact.trim())) throw new Error("请选择实际发运批次并填写已核验的收货确认人");
        const receiptLines = batch.lines.map(line => {
          const values = (quantities[line.orderLineId] ?? ["", "", "", ""]).map(v => v === "" ? 0 : Number(v));
          if (values.some(v => !Number.isSafeInteger(v) || v < 0) || values.reduce((a, b) => a + b, 0) > line.quantity - line.recordedQuantity) throw new Error("分类数量须为非负整数，且不超过本批尚未登记数量");
          return { orderLineId: line.orderLineId, acceptedQuantity: values[0] ?? 0, rejectedQuantity: values[1] ?? 0, damagedQuantity: values[2] ?? 0, shortQuantity: values[3] ?? 0 };
        }).filter(l => l.acceptedQuantity + l.rejectedQuantity + l.damagedQuantity + l.shortQuantity > 0);
        if (!receiptLines.length) throw new Error("请填写本次实际数量，不能直接把整单标记完成");
        path = `${base}${encodeURIComponent(orderId)}/sign`;
        body = { deliveryBatchId: batchId, confirmedAt: new Date(at).toISOString(), buyerContact: confirmationMatter ? undefined : contact.trim(), lines: receiptLines };
      }
      setBusy(true);
      await api.post(path, { ...body, idempotencyKey: key, evidenceAttachmentIds, ...(confirmationMatter ? { counterpartyConfirmationId: confirmationId } : {}) });
      setProofs([]); setQuantities({}); setMode(null); setKey(newKey()); setConfirmationId("");
      await load(); await onChanged(); setNotice("本次事实已记录；数量、审批和款项进度请分别核对。");
    } catch (e) { setError(e instanceof Error ? e.message : "交付处理失败"); } finally { setBusy(false); }
  }
  const batch = progress?.batches.find(b => b.id === batchId);
  const canReceive = status === "shipping" && canManage;
  return <section data-testid="dealer-delivery-panel" className="admin-panel">
    <h4>实际交付与差异处理</h4>
    {error && <p role="alert">{error}</p>}{notice && <p role="status">{notice}</p>}
    <button type="button" disabled={busy} onClick={() => void load().catch(e => setError(String(e)))}>刷新交付记录</button>
    {progress && <>
      <p>{progress.deliveryCompletedAt ? `交付数量已全部处理（${new Date(progress.deliveryCompletedAt).toLocaleString("zh-CN")}）` : "交付数量尚未处理完成"}；交付差异款项进度：{names[progress.moneyStatus] ?? progress.moneyStatus}。</p>
      <p>调整后应收 {money(progress.netReceivableCent)}；累计已收 {money(progress.receivedCent)}；交付差异原路退款待结 {money(progress.pendingRefundCent)}。</p>
      {progress.batches.map((b, i) => <div key={b.id}><b>{b.resolutionId ? "补交" : "原发运"}批次 {i + 1}</b>{b.lines.map(l => <p key={l.orderLineId}>{item(l.orderLineId)}：发运 {l.quantity}，已登记 {l.recordedQuantity}，合格接收 {l.acceptedQuantity}，未登记 {l.quantity - l.recordedQuantity}</p>)}{proofLinks(b.evidenceAttachmentIds)}</div>)}
      {progress.receipts.map(r => <details key={r.id}><summary>{new Date(r.confirmedAt).toLocaleString("zh-CN")} · {r.buyerContact} 的签收记录</summary>{r.lines.map(l => <p key={l.orderLineId}>{item(l.orderLineId)}：合格 {l.acceptedQuantity}，拒收 {l.rejectedQuantity}，破损 {l.damagedQuantity}，短少 {l.shortQuantity}</p>)}{proofLinks(r.evidenceAttachmentIds)}</details>)}
      {progress.discrepancies.map(d => <div key={d.id} className="admin-panel"><p>{item(d.orderLineId)} · {names[d.discrepancyType]} {d.quantity}，实际处理完成 {d.completedQuantity}，尚未指定有效方案 {d.quantity - d.reservedQuantity}。</p>
        {!portal && canManage && d.quantity > d.reservedQuantity && <button type="button" onClick={() => open({ kind: "proposal", difference: d })}>登记双方确认方案</button>}
        {d.resolutions.map(r => <div key={r.id}><p>{names[r.resolutionType]} {r.quantity}，{names[r.physicalRoute]}，调减 {money(r.adjustmentCent)}；{names[r.status] ?? r.status}。责任方：{r.responsibilityParty}；依据：{r.agreementBasis}</p>{proofLinks(r.evidenceAttachmentIds)}{proofLinks(r.physicalEvidenceAttachmentIds)}
          {!portal && r.reviewId && <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(r.reviewId)}`)}>查看并办理原审批</a>}
          {!portal && canShip && r.status === "approved" && !r.physicalCompletedAt && <button type="button" onClick={() => open({ kind: "physical", resolution: r })}>登记实际交接或处置</button>}
          {!portal && canShip && r.status === "approved" && r.physicalCompletedAt && r.resolutionType === "replacement" && !progress.batches.some(b => b.resolutionId === r.id) && <button type="button" onClick={() => open({ kind: "replacement", resolution: r })}>登记实际补交出库</button>}
        </div>)}
      </div>)}
    </>}
    {(canReceive || mode) && <fieldset disabled={busy} data-testid="delivery-facts-form"><legend>{mode?.kind === "proposal" ? "双方确认的差异方案" : mode?.kind === "physical" ? "实际原物交接或处置" : mode?.kind === "replacement" ? "补交实际出库" : "本次实际签收"}</legend>
      {!mode && <><label>发运批次<select aria-label="发运批次" value={batchId} onChange={e => { setBatchId(e.target.value); setQuantities({}); setKey(newKey()); }}>{progress?.batches.map((b, i) => <option key={b.id} value={b.id}>{b.resolutionId ? "补交" : "原发运"}批次 {i + 1}</option>)}</select></label>
        {confirmationMatter ? null : <label>已核验收货确认人<input aria-label="已核验收货确认人" value={contact} onChange={e => setContact(e.target.value)} /></label>}
        <table className="admin-table"><thead><tr><th>商品</th><th>尚未登记</th>{["合格接收", "拒收", "收到破损", "短少"].map(v => <th key={v}>{v}</th>)}</tr></thead><tbody>{batch?.lines.map(line => <tr key={line.orderLineId}><td>{item(line.orderLineId)}</td><td>{line.quantity - line.recordedQuantity}</td>{["合格接收", "拒收", "收到破损", "短少"].map((label, index) => <td key={label}><input type="number" min="0" max={line.quantity - line.recordedQuantity} aria-label={`${item(line.orderLineId)} ${label}`} value={quantities[line.orderLineId]?.[index] ?? ""} onChange={e => setQuantities(current => { const row = [...(current[line.orderLineId] ?? ["", "", "", ""])]; row[index] = e.target.value; return { ...current, [line.orderLineId]: row }; })} /></td>)}</tr>)}</tbody></table></>}
      {mode?.kind === "proposal" && <><label>本方案数量<input aria-label="本方案数量" type="number" min="1" value={quantity} onChange={e => setQuantity(e.target.value)} /></label>
        <label>处理方式<select value={resolutionType} onChange={e => setResolutionType(e.target.value)}><option value="replacement">补交</option><option value="price_adjustment">确认调款</option></select></label>
        <label>原物实际处理路径<select value={route} onChange={e => setRoute(e.target.value)}>{["return_resalable", "return_quarantine", "loss_confirmed", ...(mode.difference?.discrepancyType === "short" ? [] : ["accepted_as_is"])].map(v => <option key={v} value={v}>{names[v]}</option>)}</select></label>
        {resolutionType === "price_adjustment" && <label>双方确认调减货款（元）<input aria-label="双方确认调减货款" value={amount} onChange={e => setAmount(e.target.value)} /></label>}
        <label>双方确认责任方<input value={responsibility} onChange={e => setResponsibility(e.target.value)} /></label><label>合同及双方确认依据<textarea value={basis} onChange={e => setBasis(e.target.value)} /></label>
        <label>主管最终审批人账号<input value={approver} onChange={e => setApprover(e.target.value)} /></label><label>财务最终审批人账号<input value={finance} onChange={e => setFinance(e.target.value)} /></label></>}
      {confirmationMatter && <CounterpartyConfirmationPick key={`${confirmationMatter}-${confirmationVersion}`} api={api} orderId={orderId} matterType={confirmationMatter} value={confirmationId} onChange={setConfirmationId} version={confirmationVersion} />}
      {mode?.kind !== "proposal" && <label>实际时间<input aria-label="实际时间" type="datetime-local" step="0.001" value={at} onChange={e => setAt(e.target.value)} /></label>}
      <label>本次真实凭证<input aria-label="实际交接凭证" type="file" accept="image/*,video/*" multiple onChange={e => void upload(e.target.files)} /></label>
      <ul>{proofs.map(p => <li key={p.id}>{p.name}（已上传）</li>)}</ul>
      <button type="button" onClick={() => void save()}>{mode ? "保存本次事实" : "保存本次收货事实"}</button>{mode && <button type="button" onClick={() => { setMode(null); setProofs([]); setKey(newKey()); }}>取消本次填写</button>}
    </fieldset>}
  </section>;
}
