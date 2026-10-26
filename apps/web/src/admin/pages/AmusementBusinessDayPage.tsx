// 文件版本：V0.02
import React from "react";
import type { AdminPageProps } from "../types";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import { formatBusinessStatus } from "../i18n/statusLabels";

type Config = { id: string; version: string; zoneId?: string; cutoffHour: number; minCutoffHour: number; maxCutoffHour: number; closeHour: number; minCloseHour: number; maxCloseHour: number; effectiveAt: string; status: string; reviewId?: string; reason: string };
type Option = { id: string; name: string };
type Catalog = { projects?: Option[]; bars?: Option[]; versions: Config[] };
type Kind = "complex" | "bar";

// 第十八批第 1 条（ENUM:187、BSM:1682）：综合体（游乐同用这一套）与每家酒吧分别配置营业日起点与闭店时点，出厂起点 10 时、闭店 7 时。
const KINDS: Record<Kind, { endpoint: string; param: string; label: string; manage: string; view: string; evidence: string }> = {
  complex: { endpoint: "/api/v1/amusement/business-day-configs", param: "project_id", label: "综合体", manage: "amusement.business_day.manage", view: "amusement.business_day.view", evidence: "amusement_business_day_config" },
  bar: { endpoint: "/api/v1/bar-business-day-configs", param: "bar_id", label: "酒吧", manage: "bar.business_day.manage", view: "bar.business_day.view", evidence: "bar_business_day_config" },
};
const EMPTY_FORM = { zoneId: "", cutoffHour: "10", minCutoffHour: "", maxCutoffHour: "", closeHour: "7", minCloseHour: "", maxCloseHour: "", effectiveAt: "", reason: "", finalApproverId: "" };

export function AmusementBusinessDayPage({ api, effectivePermissions, setActionResult, setGlobalError, navigateTo }: AdminPageProps) {
  const [kind, setKind] = React.useState<Kind>(() => hasPermissions(effectivePermissions, [KINDS.complex.view]) ? "complex" : "bar");
  const [catalog, setCatalog] = React.useState<Catalog>({ versions: [] });
  const [target, setTarget] = React.useState("");
  const [form, setForm] = React.useState(EMPTY_FORM);
  const [attachments, setAttachments] = React.useState<string[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const meta = KINDS[kind];
  const options = (kind === "complex" ? catalog.projects : catalog.bars) ?? [];
  const load = React.useCallback(async () => {
    setCatalog(await api.get<Catalog>(meta.endpoint + (target ? `?${meta.param}=${encodeURIComponent(target)}` : "")));
  }, [api, meta, target]);
  React.useEffect(() => { setCatalog(old => ({ ...old, versions: [] })); void load().catch(e => setError(e.message)); }, [load]);
  function switchKind(next: Kind) {
    setKind(next); setTarget(""); setAttachments([]); setError(""); setCatalog({ versions: [] }); setForm(EMPTY_FORM);
  }
  async function action(call: () => Promise<unknown>) {
    if (busy) return;
    setBusy(true); setError("");
    try { setActionResult(await call()); await load(); }
    catch (e) { const message = e instanceof Error ? e.message : "操作失败"; setError(message); setGlobalError(message); }
    finally { setBusy(false); }
  }
  function bounded(hour: string, min: string, max: string) {
    const h = Number(hour), lo = Number(min), hi = Number(max);
    return [hour, min, max].every(v => v.trim()) && [h, lo, hi].every(Number.isInteger) && lo >= 0 && hi <= 23 && lo <= h && h <= hi;
  }
  async function submit() {
    if (!target || (kind === "complex" && !form.zoneId.trim()) || !form.reason.trim() || !form.effectiveAt)
      throw new Error(kind === "complex" ? "请选择综合体并填写所在地时区、未来生效时间与变更原因" : "请选择酒吧并填写未来生效时间与变更原因");
    if (!bounded(form.cutoffHour, form.minCutoffHour, form.maxCutoffHour)) throw new Error("请完整填写日切小时及上下限，须满足 0 ≤ 下限 ≤ 日切小时 ≤ 上限 ≤ 23");
    if (!bounded(form.closeHour, form.minCloseHour, form.maxCloseHour)) throw new Error("请完整填写闭店小时及上下限，须满足 0 ≤ 下限 ≤ 闭店小时 ≤ 上限 ≤ 23");
    const effective = new Date(form.effectiveAt);
    if (!Number.isFinite(effective.getTime()) || effective.getTime() <= Date.now()) throw new Error("生效时间必须在未来");
    const hours = { cutoffHour: Number(form.cutoffHour), minCutoffHour: Number(form.minCutoffHour), maxCutoffHour: Number(form.maxCutoffHour), closeHour: Number(form.closeHour), minCloseHour: Number(form.minCloseHour), maxCloseHour: Number(form.maxCloseHour) };
    const owner = kind === "complex" ? { projectId: target, zoneId: form.zoneId.trim() } : { barId: target };
    return api.post(meta.endpoint, { idempotencyKey: crypto.randomUUID(), ...owner, ...hours, effectiveAt: effective.toISOString(), reason: form.reason.trim(), finalApproverId: form.finalApproverId.trim() || null, attachmentIds: attachments });
  }
  async function upload(files: FileList | null) {
    if (!files?.length) return;
    await action(async () => {
      if (!target || !api.postForm) throw new Error(`请先选择${meta.label}后上传配置依据`);
      const ids: string[] = [];
      for (const file of Array.from(files)) {
        const payload = new FormData(); payload.append("file", file); payload.append("business_type", meta.evidence); payload.append("business_id", target); payload.append("attachment_role", "configuration_basis");
        ids.push((await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", payload)).id);
      }
      setAttachments(old => [...old, ...ids]); return { 已上传依据份数: ids.length };
    });
  }
  const text = (key: keyof typeof form, title: string, type = "text") => <label>{title}<input aria-label={title} type={type} value={form[key]} onChange={e => setForm(old => ({ ...old, [key]: e.target.value }))} /></label>;
  return <section data-testid="amusement-business-day-page">
    <AdminPageHeader title="营业日配置" description="营业日从起点起算至次日起点止，闭店后至次日起点前的记录仍归正在进行的营业日。按综合体（同一综合体的游乐项目共用这一套）与每家酒吧分别配置，未配置的按出厂起点 10 时、闭店 7 时；提交后经最高权限审批，到未来生效时点才启用，已发生的记录不回改。" />
    {error && <p role="alert">{error}</p>}
    <label>配置对象<select aria-label="配置对象" data-testid="business-day-kind" value={kind} onChange={e => switchKind(e.target.value as Kind)}><option value="complex">综合体</option><option value="bar">酒吧</option></select></label>
    <label>{meta.label}<select aria-label={meta.label} value={target} onChange={e => { setTarget(e.target.value); setAttachments([]); setError(""); }}><option value="">请选择</option>{options.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label>
    <div className="admin-form-row">
      {kind === "complex" && text("zoneId", "所在地时区（如 Asia/Shanghai）")}
      {text("cutoffHour", "日切小时", "number")}
      {text("minCutoffHour", "日切下限", "number")}
      {text("maxCutoffHour", "日切上限", "number")}
      {text("closeHour", "闭店小时", "number")}
      {text("minCloseHour", "闭店下限", "number")}
      {text("maxCloseHour", "闭店上限", "number")}
      {text("effectiveAt", "生效时间（当前设备时区）", "datetime-local")}
      {text("reason", "变更原因")}
      {text("finalApproverId", "组织链最终审批人账号（可选）")}
      <label>配置依据（可选）<input aria-label="配置依据（可选）" type="file" multiple disabled={busy || !target} onChange={e => void upload(e.target.files)} /></label>
      <span>已上传 {attachments.length} 份依据</span>
      <PermissionButton permissionsState={effectivePermissions} permissions={[meta.manage]} disabled={busy} onClick={() => void action(submit)}>提交最高权限审批</PermissionButton>
    </div>
    <table className="admin-table"><thead><tr><th>版本</th><th>{kind === "complex" ? "所在地时区／日切" : "日切"}</th><th>闭店</th><th>上下限</th><th>生效时间</th><th>审批状态／原件</th></tr></thead><tbody>
      {catalog.versions.map(row => <tr key={row.id}><td>{row.version}<p>{row.reason}</p></td><td>{row.zoneId ? `${row.zoneId}／` : ""}{row.cutoffHour}:00</td><td>次日 {row.closeHour}:00</td><td>日切 {row.minCutoffHour}～{row.maxCutoffHour} 时；闭店 {row.minCloseHour}～{row.maxCloseHour} 时</td><td>{new Date(row.effectiveAt).toLocaleString("zh-CN", { timeZone: row.zoneId ?? "Asia/Shanghai" })}{row.zoneId ? `（${row.zoneId}）` : ""}</td><td>{row.status === "expired" ? "超过拟生效时点，须重新提交" : formatBusinessStatus(row.status)}{row.reviewId && <button type="button" onClick={() => navigateTo?.("ADM-GOV-009", row.reviewId)}>查看审批原件</button>}</td></tr>)}
      {target && catalog.versions.length === 0 && <tr><td colSpan={6}>{kind === "complex" ? "该综合体尚无营业日配置，按出厂起点 10 时、闭店 7 时；游乐核销须先提交并完成审批。" : "该酒吧尚无营业日配置，按出厂起点 10 时、闭店 7 时。"}</td></tr>}
    </tbody></table>
  </section>;
}
