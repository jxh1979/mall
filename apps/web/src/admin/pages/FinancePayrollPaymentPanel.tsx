// 文件版本：V0.01
import React from "react";
import type { AdminPageProps, FinanceLedgerRow } from "../types";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useCan } from "../security/Can";
import { withBase } from "../../basePath";

type Attempt = {
  id: string; statementId: string; amountCent: number; status: string;
  bankRequestRef?: string; bankFlowRef?: string; requestedAt?: string; resultAt?: string;
};
type PaymentContext = { attempts: Attempt[]; relatedStatements: { id: string; arApItemId?: string; totalCent: number; paidAmountCent: number; status: string }[]; pendingCorrections: { id: string; originalMonth: string; amountCent?: number; status: string }[] };
const VIEW = "finance.payroll.view";
const PAY = "finance.payroll.payment.register";
const labels: Record<string, string> = { prepared: "待登记银行发起", processing: "银行处理中", unknown: "结果待核实", succeeded: "实际成功", failed: "明确失败", cancelled: "已撤销准备" };
const money = (value: number) => (value / 100).toFixed(2);
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult"> & { row: FinanceLedgerRow; onChanged: () => void };

export function FinancePayrollPaymentPanel({ api, effectivePermissions, setActionResult, row, onChanged }: Props) {
  const canRead = useCan(effectivePermissions, [VIEW]);
  const canPay = useCan(effectivePermissions, [PAY]);
  const [attempts, setAttempts] = React.useState<Attempt[]>([]);
  const [related, setRelated] = React.useState<PaymentContext["relatedStatements"]>([]);
  const [pending, setPending] = React.useState<PaymentContext["pendingCorrections"]>([]);
  const [selectedId, select] = React.useState("");
  const [amount, setAmount] = React.useState("");
  const [reference, setReference] = React.useState("");
  const [at, setAt] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [result, setResult] = React.useState("unknown");
  const [files, setFiles] = React.useState<string[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const keys = React.useRef(new Map<string, string>());
  const selected = attempts.find(item => item.id === selectedId);
  const root = "/api/v1/finance/payroll";
  const reload = React.useCallback(async () => {
    if (!row.sourceId || !canRead) return;
    const context = await api.get<PaymentContext>(`${root}/${encodeURIComponent(row.sourceId)}/payment-attempts`);
    setAttempts(context.attempts); setRelated(context.relatedStatements); setPending(context.pendingCorrections);
  }, [api, row.sourceId, canRead]);
  React.useEffect(() => { void reload().catch(e => setError(e instanceof Error ? e.message : "工资付款记录读取失败")); }, [reload]);

  async function run(action: () => Promise<void>) {
    setBusy(true); setError("");
    try { await action(); } catch (e) { setError(e instanceof Error ? e.message : "工资付款登记失败"); }
    finally { setBusy(false); }
  }
  async function write(path: string, body: Record<string, unknown>) {
    const signature = JSON.stringify([path, body]);
    if (!keys.current.has(signature)) keys.current.set(signature, crypto.randomUUID());
    const record = await api.post<Attempt>(path, { ...body, idempotencyKey: keys.current.get(signature) });
    await reload(); onChanged();
    // 已取得成功结果并刷新后结束本次意图；网络或刷新失败仍保留原键重试。
    keys.current.delete(signature);
    setActionResult({ action: "payroll_bank_fact", status: record.status, paymentAttemptId: record.id });
    select(record.id); setFiles([]); setReference(""); setReason(""); setAt("");
  }
  async function prepare() {
    await run(async () => {
      if (!row.sourceId || !/^\d+(\.\d{1,2})?$/.test(amount)) throw new Error("请填写最多两位小数的工资金额");
      const cents = Math.round(Number(amount) * 100);
      if (!Number.isSafeInteger(cents) || cents <= 0 || cents > (row.unpaidAmountCent ?? 0)) throw new Error("金额须大于零且不超过此单未付工资");
      await write(`${root}/${encodeURIComponent(row.sourceId)}/payment-attempts`, { amountCent: cents });
      setAmount("");
    });
  }
  async function choose(id: string) {
    select(id); setFiles([]); setReference(""); setAt(""); setReason(""); setResult("unknown");
    await run(async () => {
      const timeline = await api.get<{ itemType: string; referenceId: string; result: string }[]>(`/api/v1/evidence/timelines/staff_payroll_payment/${encodeURIComponent(id)}`);
      setFiles(timeline.filter(item => item.itemType === "attachment" && item.result === "uploaded").map(item => item.referenceId));
    });
  }
  async function upload(input: FileList | null) {
    if (!selected || !input?.length) return;
    await run(async () => {
      if (!api.postForm) throw new Error("当前客户端不支持凭证上传");
      for (const file of Array.from(input)) {
        const data = new FormData(); data.append("file", file); data.append("business_type", "staff_payroll_payment");
        data.append("business_id", selected.id); data.append("attachment_role", "bank_proof");
        const attachment = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", data);
        setFiles(old => [...new Set([...old, attachment.id])]);
      }
    });
  }
  async function record(kind: "submit" | "result") {
    await run(async () => {
      if (!selected) throw new Error("请先选择工资付款记录");
      const outcome = selected.status === "prepared" ? "cancelled" : result;
      const date = new Date(at);
      if (!at || Number.isNaN(date.getTime()) || date.getTime() > Date.now()) throw new Error("请填写实际银行事实时间，不能晚于当前时间");
      if ((kind === "submit" || outcome !== "cancelled") && !files.length) throw new Error("请上传本笔银行凭证");
      if (kind === "submit" && (!reference.trim() || !row.counterpartyId)) throw new Error("银行请求编号或工资收款员工缺失");
      if (kind === "result" && (!reason.trim() || (outcome === "succeeded" && !reference.trim()))) throw new Error("请填写核实说明；成功还须填写银行成功流水号");
      await write(`${root}/payment-attempts/${encodeURIComponent(selected.id)}/${kind}`, kind === "submit"
        ? { bankRequestRef: reference.trim(), requestedAt: date.toISOString(), recipientUserId: row.counterpartyId, evidenceAttachmentIds: files }
        : { status: outcome, bankFlowRef: outcome === "succeeded" ? reference.trim() : null, resultAt: date.toISOString(), reason: reason.trim(), evidenceAttachmentIds: files });
    });
  }
  const active = attempts.some(item => ["prepared", "processing", "unknown"].includes(item.status));
  return <section aria-label="工资银行付款" data-testid="payroll-bank-panel">
    <h3>工资银行付款 · {row.sourceId}</h3>
    <p>本单未付 {money(row.unpaidAmountCent ?? 0)} 元。银行付款由财务实际办理；此处登记凭证和核实结果，只有实际成功才计为已付。同月其他工资单和补差单须分别核实。</p>
    {!canRead ? <p>需要工资财务查看权限才能读取付款记录。</p> : <>
      {related.length ? <div role="status"><h4>同日应发的其他工资单</h4>{related.map(item => <p key={item.id}>工资单 {item.id}，应付台账 {item.arApItemId ?? "尚未形成"}，未付 {money(item.totalCent - item.paidAmountCent)} 元。请在台账选取该单分别办理。</p>)}</div> : null}
      {pending.length ? <div role="status"><h4>仍待核实的撤罚补差</h4>{pending.map(item => <p key={item.id}>{item.originalMonth} 补差 {item.id}：{item.amountCent == null ? "金额待核实" : `${money(item.amountCent)} 元（税额及定格仍待办理）`}。由人事、财务完成原件核实，本单付款不会结清此项。</p>)}</div> : null}
      <div className="admin-form-row"><label>本次工资金额（元）<input aria-label="本次工资金额" inputMode="decimal" value={amount} onChange={e => setAmount(e.target.value)} disabled={busy || active || !canPay} /></label>
        <HighRiskConfirm label="准备工资付款" impact="锁定本次计划支付金额，不代表已经付款。" irreversible={false}
          permissionsState={effectivePermissions} permissions={[PAY]} disabled={busy || active || !row.sourceId || !(row.unpaidAmountCent! > 0)} onConfirm={() => void prepare()} />
        <button type="button" disabled={busy} onClick={() => void run(reload)}>刷新银行记录</button></div>
      {active ? <p>存在准备中或结果未明的付款。先核实原笔，不可重复出款。</p> : null}
      <table className="admin-table"><thead><tr><th>记录</th><th>金额（元）</th><th>状态</th><th>银行凭据</th><th>操作</th></tr></thead>
        <tbody>{attempts.map(item => <tr key={item.id}><td>{item.id}</td><td>{money(item.amountCent)}</td><td>{labels[item.status] ?? item.status}</td><td>{item.bankFlowRef ?? item.bankRequestRef ?? "—"}</td><td><button type="button" disabled={busy} onClick={() => void choose(item.id)}>查看及核实</button></td></tr>)}</tbody></table>
      {selected ? <div data-testid="payroll-bank-detail"><p>所选记录：{selected.id} · {labels[selected.status]}</p>
        {files.map((id, i) => <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">银行凭证 {i + 1} </a>)}
        {["prepared", "processing", "unknown"].includes(selected.status) && canPay ? <>
          <label>银行凭证影像<input type="file" aria-label="银行凭证影像" accept="image/*,video/*" multiple disabled={busy} onChange={e => void upload(e.target.files)} /></label>
          <label>{selected.status === "prepared" ? "银行请求编号" : "银行成功流水号"}<input aria-label="银行编号" value={reference} onChange={e => setReference(e.target.value)} /></label>
          <label>实际银行事实时间<input aria-label="银行事实时间" type="datetime-local" value={at} onChange={e => setAt(e.target.value)} /></label>
          {selected.status === "prepared" ? <HighRiskConfirm label="登记银行已发起" impact="确认银行确已发起此笔工资；记录转为处理中，尚不计入已付。"
            permissionsState={effectivePermissions} permissions={[PAY]} disabled={busy} onConfirm={() => void record("submit")} /> : null}
          <label>核实结果<select aria-label="银行核实结果" value={selected.status === "prepared" ? "cancelled" : result} onChange={e => setResult(e.target.value)}>
            {selected.status === "prepared" ? <option value="cancelled">撤销未发起的准备</option> : <><option value="unknown">结果未明</option><option value="failed">银行明确失败</option><option value="succeeded">银行实际成功</option></>}</select></label>
          <label>核实说明<input aria-label="银行核实说明" value={reason} onChange={e => setReason(e.target.value)} /></label>
          <HighRiskConfirm label={selected.status === "prepared" ? "撤销付款准备" : "登记银行结果"} impact="按真实凭证登记；成功会增加已付工资，结果未明继续保留待核实。"
            permissionsState={effectivePermissions} permissions={[PAY]} disabled={busy} onConfirm={() => void record("result")} />
        </> : null}
      </div> : null}
    </>}
    {error ? <p role="alert">{error}</p> : null}
  </section>;
}
