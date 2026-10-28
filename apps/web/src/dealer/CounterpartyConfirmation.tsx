// 文件版本：V0.01
import React from "react";

/**
 * 第十八批第 31 条 经销商停用后的存量事项（BSM:1247「合同销售主体指定经办人，通过已核验的企业联系人取得签收、退货等确认并留证，
 * 经办人不得代替对方确认」）。登记区只由指定经办人使用；确认人固定取档案已核验联系人，须上传对方确认的凭证。
 * 停用后签收、退货申请、合同未约定的运费确认、交付差异方案、超收用途提交前须选一条对应事项、未使用的确认。
 * 裁决补充 15：停用后尾款的收款凭证即财务核验通过的到账记录，经办人交付对方后登记一条结果交付（关联到账记录号），不另建凭证对象。
 */
export type ConfirmationApi = {
  get<T>(path: string): Promise<T>;
  post<T>(path: string, body?: Record<string, unknown>): Promise<T>;
  postForm?<T>(path: string, form: FormData): Promise<T>;
};
export type CounterpartyConfirmation = { id: string; matterType: string; relatedObjectId?: string | null; contactName: string; content: string; confirmedAt: string; handlerUserId?: string; usedById?: string | null; usedAt?: string | null };
export const MATTERS: Record<string, string> = {
  delivery_receipt: "签收", return_request: "退货申请", freight_confirmation: "运费确认", delivery_resolution: "交付差异",
  overpayment_use: "超收用途", result_delivery: "结果交付（退款、红冲、发票、收款凭证）",
};
const path = (orderId: string) => `/api/v1/dealers/orders/${encodeURIComponent(orderId)}/counterparty-confirmations`;
const stamp = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
const time = (value?: string | null) => value ? new Date(value).toLocaleString("zh-CN") : "—";

/** 停用经销商的对方确认类表单：只列本订单本事项、尚未使用的确认。 */
export function CounterpartyConfirmationPick({ api, orderId, matterType, value, onChange, version = 0 }: { api: ConfirmationApi; orderId: string; matterType: string; value: string; onChange: (id: string) => void; version?: number }) {
  const [rows, setRows] = React.useState<CounterpartyConfirmation[]>([]);
  const [error, setError] = React.useState("");
  React.useEffect(() => {
    api.get<CounterpartyConfirmation[]>(path(orderId)).then(items => { setRows((items ?? []).filter(row => row.matterType === matterType && !row.usedAt)); setError(""); })
      .catch(e => setError(e instanceof Error ? e.message : "对方确认留证读取失败"));
  }, [api, orderId, matterType, version]);
  return <label data-testid={`counterparty-pick-${matterType}`}>经销商已停用，须选择指定经办人登记的对方确认留证（{MATTERS[matterType]}）
    <select aria-label={`对方确认留证（${MATTERS[matterType]}）`} value={value} onChange={e => onChange(e.target.value)}>
      <option value="">请选择</option>
      {rows.map(row => <option key={row.id} value={row.id}>{time(row.confirmedAt)} · {row.contactName}：{row.content}</option>)}
    </select>{error ? <span role="alert">{error}</span> : null}</label>;
}

/** 指定经办人登记对方确认并留证；同时列出本订单已有记录及是否已用。 */
export function CounterpartyConfirmationPanel({ api, orderId, contactName, canManage, onChanged }: { api: ConfirmationApi; orderId: string; contactName?: string | null; canManage: boolean; onChanged: () => void }) {
  const [rows, setRows] = React.useState<CounterpartyConfirmation[]>([]);
  const [matter, setMatter] = React.useState("delivery_receipt");
  const [content, setContent] = React.useState("");
  const [related, setRelated] = React.useState("");
  const [at, setAt] = React.useState(stamp);
  const [proofs, setProofs] = React.useState<{ id: string; name: string }[]>([]);
  const [key, setKey] = React.useState(() => `dcc-${crypto.randomUUID()}`);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const load = React.useCallback(() => api.get<CounterpartyConfirmation[]>(path(orderId)).then(items => setRows(items ?? []))
    .catch(e => setError(e instanceof Error ? e.message : "对方确认留证读取失败")), [api, orderId]);
  React.useEffect(() => { void load(); }, [load]);
  async function upload(files: FileList | null) {
    if (!files?.length || busy) return;
    if (!api.postForm) { setError("当前客户端无法上传凭证，请刷新后重试"); return; }
    setBusy(true); setError("");
    try {
      for (const file of Array.from(files)) {
        const form = new FormData(); form.append("file", file); form.append("business_type", "dealer_delivery"); form.append("business_id", orderId); form.append("attachment_role", "other");
        const saved = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", form);
        setProofs(previous => [...previous, { id: saved.id, name: file.name }]);
      }
    } catch (e) { setError(e instanceof Error ? e.message : "凭证上传失败"); } finally { setBusy(false); }
  }
  async function save() {
    if (busy) return;
    if (!content.trim() || !at || !proofs.length || (matter === "result_delivery" && !related.trim())) { setError("须填写对方确认内容、确认时间并上传对方确认的凭证；结果交付还须填写退款单、发票号或到账记录号。"); return; }
    setBusy(true); setError("");
    try {
      await api.post(path(orderId), { idempotencyKey: key, matterType: matter, relatedObjectId: matter === "result_delivery" ? related.trim() : undefined, content: content.trim(), confirmedAt: new Date(at).toISOString(), evidenceAttachmentIds: proofs.map(p => p.id) });
      setContent(""); setRelated(""); setProofs([]); setKey(`dcc-${crypto.randomUUID()}`);
      await load(); onChanged();
    } catch (e) { setError(e instanceof Error ? e.message : "登记失败"); } finally { setBusy(false); }
  }
  return <section data-testid="counterparty-confirmation-panel" className="admin-panel"><h4>经办人确认留证</h4>
    <p>经销商已停用：签收、退货、运费、交付差异、超收用途须由合同指定经办人通过已核验的企业联系人（{contactName || "档案未登记联系人"}）取得确认。须上传对方确认的凭证，经办人不得代替对方确认。</p>
    {error ? <p role="alert">{error}</p> : null}
    {canManage ? <fieldset disabled={busy}>
      <label>确认事项<select aria-label="确认事项" value={matter} onChange={e => setMatter(e.target.value)}>{Object.entries(MATTERS).map(([code, title]) => <option key={code} value={code}>{title}</option>)}</select></label>
      {matter === "result_delivery" ? <label>退款单、发票号或到账记录号<input aria-label="退款单、发票号或到账记录号" value={related} onChange={e => setRelated(e.target.value)} /></label> : null}
      <label>对方确认内容<input aria-label="对方确认内容" value={content} onChange={e => setContent(e.target.value)} /></label>
      <label>对方确认时间<input aria-label="对方确认时间" type="datetime-local" value={at} onChange={e => setAt(e.target.value)} /></label>
      <label>对方确认凭证<input aria-label="对方确认凭证" type="file" accept="image/*,video/*" multiple onChange={e => { void upload(e.target.files); e.target.value = ""; }} /></label>
      <ul>{proofs.map(p => <li key={p.id}>{p.name}（已上传）</li>)}</ul>
      <button type="button" data-testid="counterparty-save" onClick={() => void save()}>登记对方确认</button>
    </fieldset> : null}
    <ul data-testid="counterparty-list">{rows.map(row => <li key={row.id}>{MATTERS[row.matterType] ?? "其他事项"}{row.relatedObjectId ? `（${row.relatedObjectId}）` : ""}：{row.contactName}于{time(row.confirmedAt)}确认「{row.content}」；经办人 {row.handlerUserId ?? "—"}；{row.usedAt ? `已用于办理（${time(row.usedAt)}）` : "未使用"}</li>)}</ul>
  </section>;
}
