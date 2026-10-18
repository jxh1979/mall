// 文件版本：V0.01
import React from "react";
import type { AdminPageProps } from "../types";

type Source = { sourceBillId: string; tableId: string; remainingGoodsCent?: number; remainingServiceFeeCent: number; remainingTipCent: number };
type PriorLine = { adjustmentId: string; sourceBillId: string; componentType: string; amountCent: number; paymentStatus: string };
type Context = { billId: string; remainingGoodsAndServiceFeeCent: number; remainingTipCent: number; sources: Source[]; priorLines: PriorLine[] };
type Amounts = { goods: string; service_fee: string; tip: string };
const types = { goods: "商品", service_fee: "服务费", tip: "小费" } as const;
const yuan = (cent: number) => (cent / 100).toFixed(2);
function cents(raw: string) {
  if (!raw.trim()) return 0;
  if (!/^\d+(\.\d{1,2})?$/.test(raw.trim())) throw new Error("金额须填写非负数，最多两位小数");
  const value = Math.round(Number(raw) * 100);
  if (!Number.isSafeInteger(value)) throw new Error("金额超出可办理范围");
  return value;
}
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult" | "setGlobalError"> & { billId?: string; onChanged: () => Promise<unknown> };

export function BarRefundBreakdownPanel({ api, effectivePermissions, setActionResult, setGlobalError, billId, onChanged }: Props) {
  const [context, setContext] = React.useState<Context | null>(null);
  const [amounts, setAmounts] = React.useState<Record<string, Amounts>>({});
  const [total, setTotal] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [files, setFiles] = React.useState<{ id: string; name: string }[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const [outcome, setOutcome] = React.useState("");
  const requestKey = React.useRef<{ body: string; key: string } | null>(null);
  const allowed = (permission: string) => effectivePermissions?.status === "ready" && effectivePermissions.value.grants.includes(permission) && !effectivePermissions.value.denies.includes(permission);
  React.useEffect(() => { setContext(null); setAmounts({}); setTotal(""); setReason(""); setFiles([]); setError(""); setOutcome(""); requestKey.current = null; }, [billId]);
  async function action(work: () => Promise<void>) {
    setBusy(true); setError(""); setGlobalError(null);
    try { await work(); } catch (e) { const message = e instanceof Error ? e.message : "退款操作失败"; setError(message); setGlobalError(message); }
    finally { setBusy(false); }
  }
  async function load() {
    await action(async () => {
      if (!billId) throw new Error("请先选择已有账单的订单");
      const data = await api.get<Context>("/api/v1/bar/bills/" + encodeURIComponent(billId) + "/refund-context");
      setContext(data); setAmounts(Object.fromEntries(data.sources.map(source => [source.sourceBillId, { goods: "", service_fee: "", tip: "" }]))); setOutcome("");
    });
  }
  async function upload(input: FileList | null) {
    if (!input?.length || !billId) return;
    await action(async () => {
      if (!api.postForm) throw new Error("当前入口不支持原件上传");
      const saved: { id: string; name: string }[] = [];
      for (const file of Array.from(input)) {
        const form = new FormData(); form.append("file", file); form.append("business_type", "bar_bill"); form.append("business_id", billId); form.append("attachment_role", file.type.startsWith("video") ? "video" : "photo");
        const attachment = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", form);
        saved.push({ id: attachment.id, name: file.name });
      }
      setFiles(old => [...old, ...saved]);
    });
  }
  function lines() {
    return Object.entries(amounts).flatMap(([sourceBillId, values]) => Object.entries(values).flatMap(([componentType, value]) => {
      const amountCent = cents(value); return amountCent > 0 ? [{ sourceBillId, componentType, amountCent }] : [];
    }));
  }
  async function submit() {
    await action(async () => {
      if (!context || context.billId !== billId) throw new Error("请先重新读取本账单真实来源");
      const refundLines = lines(); const amountCent = cents(total);
      if (amountCent <= 0 || refundLines.reduce((sum, line) => sum + line.amountCent, 0) !== amountCent) throw new Error("各原账单商品、服务费、小费明细合计必须等于本次实际退款额");
      if (!reason.trim()) throw new Error("请填写退款原因");
      for (const source of context.sources) {
        const values = amounts[source.sourceBillId] ?? { goods: "", service_fee: "", tip: "" };
        if (cents(values.service_fee) > source.remainingServiceFeeCent || cents(values.tip) > source.remainingTipCent || (source.remainingGoodsCent != null && cents(values.goods) > source.remainingGoodsCent)) throw new Error("原账单分项退款累计超出剩余可退金额，处理中退款同样占用额度");
      }
      const tip = refundLines.filter(line => line.componentType === "tip").reduce((sum, line) => sum + line.amountCent, 0);
      if (tip > context.remainingTipCent || amountCent - tip > context.remainingGoodsAndServiceFeeCent) throw new Error("退款明细超过本收款账单剩余可退金额");
      const body = { adjustmentType: "refund", amountCent, reason: reason.trim(), evidenceAttachmentIds: files.map(file => file.id), refundLines };
      const serialized = JSON.stringify({ billId, ...body });
      if (requestKey.current?.body !== serialized) requestKey.current = { body: serialized, key: crypto.randomUUID() };
      const result = await api.post("/api/v1/bar/bills/" + encodeURIComponent(billId!) + "/adjustments", { ...body, idempotencyKey: requestKey.current.key });
      requestKey.current = null; setActionResult(result); setContext(null); setOutcome("退款申请及真实分项已提交。请按原审批、付款流程办理，提交成功不表示客人已收到退款。"); await onChanged();
    });
  }
  let sum: string;
  try { sum = yuan(lines().reduce((amount, line) => amount + line.amountCent, 0)); } catch { sum = "金额格式待更正"; }
  if (!allowed("bar.bill.refund") || !allowed("bar.bill.adjust")) return null;
  return <details data-testid="bar-refund-breakdown"><summary>按原账单明细办理退款</summary>
    <p>按真实商品、服务费、小费退款分项填写，不能按总额自动同比例分摊。小费及服务费沿发薪前退款专条办理。</p>
    <button type="button" disabled={busy || !billId} onClick={() => void load()}>读取退款原账单与历史</button>
    {error ? <p role="alert">{error}</p> : null}{outcome ? <p role="status">{outcome}</p> : null}
    {context ? <>
      <p>收款账单 {context.billId}；剩余商品及服务费 {yuan(context.remainingGoodsAndServiceFeeCent)} 元；小费 {yuan(context.remainingTipCent)} 元。</p>
      {context.sources.map(source => <fieldset key={source.sourceBillId}><legend>原账单 {source.sourceBillId} · 原台 {source.tableId}</legend>
        <p>尚可退服务费 {yuan(source.remainingServiceFeeCent)} 元；小费 {yuan(source.remainingTipCent)} 元。{source.remainingGoodsCent == null ? "商品按实际退品原件核实。" : "商品尚可退 " + yuan(source.remainingGoodsCent) + " 元。"}</p>
        {Object.entries(types).map(([type, label]) => <label key={type}>{label}退款（元）<input inputMode="decimal" aria-label={source.sourceBillId + label + "退款"} value={amounts[source.sourceBillId]?.[type as keyof Amounts] ?? ""} onChange={e => setAmounts(old => ({ ...old, [source.sourceBillId]: { ...(old[source.sourceBillId] ?? { goods: "", service_fee: "", tip: "" }), [type]: e.target.value } }))} /></label>)}
      </fieldset>)}
      <p>本次分项合计：{sum} 元</p>
      <label>本次实际退款额（元）<input aria-label="本次实际退款额" inputMode="decimal" value={total} onChange={e => setTotal(e.target.value)} /></label>
      <label>退款原因<input aria-label="退款原因" value={reason} onChange={e => setReason(e.target.value)} /></label>
      <label>退款原件<input type="file" aria-label="退款原件" accept="image/*,video/*" multiple disabled={busy} onChange={e => void upload(e.target.files)} /></label>
      <ul>{files.map(file => <li key={file.id}>{file.name}</li>)}</ul>
      <button type="button" disabled={busy} onClick={() => void submit()}>按原流程提交退款</button>
      <h4>既有退款明细（处理中也占用可退额度）</h4>
      <table><thead><tr><th>退款记录／原账单</th><th>分项</th><th>金额（元）</th><th>实际付款</th></tr></thead><tbody>{context.priorLines.map((line, i) => <tr key={line.adjustmentId + i}><td>{line.adjustmentId}／{line.sourceBillId}</td><td>{types[line.componentType as keyof typeof types] ?? line.componentType}</td><td>{yuan(line.amountCent)}</td><td>{line.paymentStatus === "paid" ? "已付成" : "待实际付款"}</td></tr>)}</tbody></table>
    </> : null}
  </details>;
}
