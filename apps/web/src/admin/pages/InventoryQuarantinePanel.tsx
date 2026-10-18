// 文件版本：V0.01
import React from "react";
import { withBase } from "../../basePath";
import type { AdminApiClient } from "../types";

type Lot = { id: string; itemId: string; quantity: number; remainingQuantity: number; receivedAt: string; sourceId: string };
type Disposition = { id: string; quantity: number; dispositionType: string; status: string; reason: string; reviewId?: string; completedAt?: string };
type Warehouse = { id: string; name: string };
type Props = { api: AdminApiClient; canRead: boolean; canManage: boolean; onChanged?: () => Promise<void> };
const stateNames: Record<string, string> = { pending_approval: "待主管与财务审批", approved: "已批准，待实际处置", rejected: "已驳回", completed: "实物处置已完成" };
const key = () => `quarantine-${crypto.randomUUID()}`;
const now = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 23);

export function InventoryQuarantinePanel({ api, canRead, canManage, onChanged }: Props) {
  const [warehouses, setWarehouses] = React.useState<Warehouse[]>([]);
  const [expanded, setExpanded] = React.useState(false);
  const [warehouse, setWarehouse] = React.useState("");
  const [lots, setLots] = React.useState<Lot[]>([]);
  const [lot, setLot] = React.useState<Lot | null>(null);
  const [plans, setPlans] = React.useState<Disposition[]>([]);
  const [completing, setCompleting] = React.useState<Disposition | null>(null);
  const [editing, setEditing] = React.useState(false);
  const [quantity, setQuantity] = React.useState("");
  const [kind, setKind] = React.useState("release_resalable");
  const [reason, setReason] = React.useState("");
  const [approver, setApprover] = React.useState("");
  const [finance, setFinance] = React.useState("");
  const [at, setAt] = React.useState(now);
  const [proofs, setProofs] = React.useState<{ id: string; name: string; type: string }[]>([]);
  const [requestKey, setRequestKey] = React.useState(key);
  const [error, setError] = React.useState("");
  const [notice, setNotice] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  async function expand() {
    setExpanded(v => !v);
    if (expanded) return;
    setError("");
    try {
      const choices = await api.get<Warehouse[]>("/api/v1/inventory/quarantine/warehouses");
      setWarehouses(choices);
      await load(choices.some(row => row.id === warehouse) ? warehouse : choices[0]?.id ?? "");
    } catch (e) { setWarehouses([]); setLots([]); setError(e instanceof Error ? e.message : "隔离仓库读取失败"); }
  }
  async function load(id: string) {
    setWarehouse(id); setLot(null); setEditing(false); setPlans([]); setError("");
    if (!id) { setLots([]); return; }
    try { setLots(await api.get<Lot[]>(`/api/v1/inventory/quarantine?warehouse_id=${encodeURIComponent(id)}`)); }
    catch (e) { setLots([]); setError(e instanceof Error ? e.message : "隔离库存读取失败"); }
  }
  async function select(row: Lot) {
    setLot(row); setEditing(false); setCompleting(null); setError("");
    try { setPlans(await api.get<Disposition[]>(`/api/v1/inventory/quarantine/${encodeURIComponent(row.id)}/dispositions`)); }
    catch (e) { setPlans([]); setError(e instanceof Error ? e.message : "处置记录读取失败"); }
  }
  function open(plan: Disposition | null) {
    setCompleting(plan); setEditing(true); setQuantity(""); setReason(""); setProofs([]); setAt(now()); setError(""); setRequestKey(key());
  }
  async function upload(files: FileList | null) {
    if (!lot || !files?.length || busy) return;
    if (!api.postForm) { setError("当前客户端无法上传凭证，请刷新后重试"); return; }
    setBusy(true); setError("");
    try {
      for (const file of Array.from(files)) {
        const form = new FormData(); form.append("file", file); form.append("business_type", "inventory_quarantine"); form.append("business_id", lot.id); form.append("attachment_role", "other");
        const proof = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", form);
        setProofs(current => [...current, { id: proof.id, name: file.name, type: file.type }]);
      }
    } catch (e) { setError(e instanceof Error ? e.message : "凭证上传失败"); } finally { setBusy(false); }
  }
  async function save() {
    if (!lot || busy) return;
    setError(""); setNotice("");
    try {
      if (!proofs.length) throw new Error("请先上传本次真实凭证");
      if ((completing?.dispositionType ?? kind) === "scrap" && (!proofs.some(p => p.type.startsWith("image/")) || !proofs.some(p => p.type.startsWith("video/")))) throw new Error("报损须同时上传照片和视频");
      let path: string, body: Record<string, unknown>;
      if (completing) {
        if (!at || !Number.isFinite(new Date(at).getTime())) throw new Error("请填写实际处置完成时间");
        path = `/api/v1/inventory/quarantine/dispositions/${encodeURIComponent(completing.id)}/complete`;
        body = { completedAt: new Date(at).toISOString() };
      } else {
        const qty = Number(quantity), occupied = plans.filter(p => ["pending_approval", "approved"].includes(p.status)).reduce((sum, p) => sum + p.quantity, 0);
        if (!Number.isSafeInteger(qty) || qty <= 0 || qty > lot.remainingQuantity - occupied) throw new Error("处置数量不能超过尚未占用的隔离余额");
        if (![reason, approver, finance].every(v => v.trim())) throw new Error("请填写处置依据及主管、财务最终审批人");
        path = `/api/v1/inventory/quarantine/${encodeURIComponent(lot.id)}/dispositions`;
        body = { quantity: qty, dispositionType: kind, reason: reason.trim(), finalApproverId: approver.trim(), parallelLines: [{ lineCode: "finance_review", finalApproverId: finance.trim() }] };
      }
      setBusy(true);
      await api.post(path, { ...body, idempotencyKey: requestKey, evidenceAttachmentIds: proofs.map(p => p.id) });
      await load(warehouse); await onChanged?.();
      setNotice(completing ? "实物处置已登记，库存按实际结果更新。" : "方案已提交审批，当前隔离余额尚未改变。");
    } catch (e) { setError(e instanceof Error ? e.message : "隔离处置失败"); } finally { setBusy(false); }
  }
  if (!canRead) return null;
  return <section className="admin-panel" data-testid="inventory-quarantine-panel">
    <button type="button" onClick={() => void expand()}>隔离库存与后续处置</button>
    {expanded && <><p>隔离实物不计入可用库存。审批通过后仍需登记实际复检释放或报损，客人退款进度另行追踪。</p>
      {error && <p role="alert">{error}</p>}{notice && <p role="status">{notice}</p>}
      <label>隔离仓库<select aria-label="隔离仓库" value={warehouse} onChange={e => void load(e.target.value)}><option value="">请选择仓库</option>{warehouses.map(row => <option key={row.id} value={row.id}>{row.name}</option>)}</select></label>
      <button type="button" disabled={busy || !warehouse} onClick={() => void load(warehouse)}>刷新隔离库存</button>
      {!warehouses.length && <p>当前授权范围内没有隔离实物记录。</p>}
      <table className="admin-table"><thead><tr><th>商品</th><th>接收隔离数量</th><th>剩余隔离数量</th><th>接收时间</th><th>操作</th></tr></thead><tbody>{lots.map(row => <tr key={row.id}><td>{row.itemId}</td><td>{row.quantity}</td><td>{row.remainingQuantity}</td><td>{new Date(row.receivedAt).toLocaleString("zh-CN")}</td><td><button type="button" onClick={() => void select(row)}>查看处置</button></td></tr>)}</tbody></table>
      {lot && <><h4>{lot.itemId} 的隔离处置</h4>{plans.map(plan => <div key={plan.id}><p>{plan.dispositionType === "scrap" ? "报损" : "复检可售释放"} {plan.quantity}；{stateNames[plan.status] ?? plan.status}；依据：{plan.reason}</p>
        {plan.reviewId && <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(plan.reviewId)}`)}>查看并办理原审批</a>}
        {canManage && plan.status === "approved" && <button type="button" onClick={() => open(plan)}>登记实际处置完成</button>}</div>)}
        {canManage && lot.remainingQuantity > 0 && <button type="button" onClick={() => open(null)}>新建隔离处置方案</button>}
        {editing && <fieldset disabled={busy}><legend>{completing ? "实际处置凭证" : "隔离处置审批方案"}</legend>
          {!completing && <><label>处置数量<input aria-label="隔离处置数量" type="number" min="1" value={quantity} onChange={e => setQuantity(e.target.value)} /></label>
            <label>处置类型<select value={kind} onChange={e => setKind(e.target.value)}><option value="release_resalable">复检确认可售后释放</option><option value="scrap">实物报损处置</option></select></label>
            <label>处置依据<textarea value={reason} onChange={e => setReason(e.target.value)} /></label><label>主管最终审批人<input value={approver} onChange={e => setApprover(e.target.value)} /></label><label>财务最终审批人<input value={finance} onChange={e => setFinance(e.target.value)} /></label></>}
          {completing && <label>实际处置时间<input type="datetime-local" step="0.001" value={at} onChange={e => setAt(e.target.value)} /></label>}
          <label>隔离处置凭证<input aria-label="隔离处置凭证" type="file" multiple accept="image/*,video/*" onChange={e => void upload(e.target.files)} /></label><ul>{proofs.map(p => <li key={p.id}>{p.name}（已上传）</li>)}</ul>
          <button type="button" onClick={() => void save()}>{completing ? "确认实际处置" : "提交处置审批"}</button><button type="button" onClick={() => setEditing(false)}>取消填写</button>
        </fieldset>}</>}
    </>}
  </section>;
}
