// 文件版本：V0.01
import React from "react";
import type { AdminPageProps } from "../types";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import { formatBusinessStatus } from "../i18n/statusLabels";

type Config = { id: string; projectId: string; version: string; zoneId: string; cutoffHour: number; minCutoffHour: number; maxCutoffHour: number; effectiveAt: string; status: string; reviewId?: string; reason: string };
type Catalog = { projects: Array<{ id: string; name: string }>; versions: Config[] };
const endpoint = "/api/v1/amusement/business-day-configs";

export function AmusementBusinessDayPage({ api, effectivePermissions, setActionResult, setGlobalError, navigateTo }: AdminPageProps) {
  const [catalog, setCatalog] = React.useState<Catalog>({ projects: [], versions: [] });
  const [project, setProject] = React.useState("");
  const [form, setForm] = React.useState({ zoneId: "", cutoffHour: "10", minCutoffHour: "", maxCutoffHour: "", effectiveAt: "", reason: "", finalApproverId: "" });
  const [attachments, setAttachments] = React.useState<string[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const load = React.useCallback(async () => {
    setCatalog(await api.get<Catalog>(endpoint + (project ? `?project_id=${encodeURIComponent(project)}` : "")));
  }, [api, project]);
  React.useEffect(() => { setCatalog(old => ({ ...old, versions: [] })); void load().catch(e => setError(e.message)); }, [load]);
  async function action(call: () => Promise<unknown>) {
    if (busy) return;
    setBusy(true); setError("");
    try { setActionResult(await call()); await load(); }
    catch (e) { const message = e instanceof Error ? e.message : "操作失败"; setError(message); setGlobalError(message); }
    finally { setBusy(false); }
  }
  async function submit() {
    const hour = Number(form.cutoffHour), min = Number(form.minCutoffHour), max = Number(form.maxCutoffHour);
    if (!project || !form.zoneId.trim() || !form.reason.trim() || !form.effectiveAt) throw new Error("请选择综合体并填写所在地时区、未来生效时间与变更原因");
    if ([form.cutoffHour, form.minCutoffHour, form.maxCutoffHour].some(v => !v.trim()) || ![hour, min, max].every(Number.isInteger) || min < 0 || max > 23 || min > hour || hour > max) throw new Error("请完整填写日切小时及上下限，须满足 0 ≤ 下限 ≤ 日切小时 ≤ 上限 ≤ 23");
    const effective = new Date(form.effectiveAt);
    if (!Number.isFinite(effective.getTime()) || effective.getTime() <= Date.now()) throw new Error("生效时间必须在未来");
    return api.post(endpoint, { idempotencyKey: crypto.randomUUID(), projectId: project, zoneId: form.zoneId.trim(), cutoffHour: hour, minCutoffHour: min, maxCutoffHour: max, effectiveAt: effective.toISOString(), reason: form.reason.trim(), finalApproverId: form.finalApproverId.trim() || null, attachmentIds: attachments });
  }
  async function upload(files: FileList | null) {
    if (!files?.length) return;
    await action(async () => {
      if (!project || !api.postForm) throw new Error("请先选择综合体后上传配置依据");
      const ids: string[] = [];
      for (const file of Array.from(files)) {
        const payload = new FormData(); payload.append("file", file); payload.append("business_type", "amusement_business_day_config"); payload.append("business_id", project); payload.append("attachment_role", "configuration_basis");
        ids.push((await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", payload)).id);
      }
      setAttachments(old => [...old, ...ids]); return { 已上传依据份数: ids.length };
    });
  }
  const text = (key: keyof typeof form, title: string, type = "text") => <label>{title}<input aria-label={title} type={type} value={form[key]} onChange={e => setForm(old => ({ ...old, [key]: e.target.value }))} /></label>;
  return <section data-testid="amusement-business-day-page">
    <AdminPageHeader title="游乐营业日" description="同一综合体的全部游乐项目使用同一营业日。按所在地时区配置日切，提交后经最高权限审批，到未来生效时点才启用；缺配置不会使用全平台默认日切。10 时为填写建议，须一并确认上下限。" />
    {error && <p role="alert">{error}</p>}
    <label>综合体<select aria-label="综合体" value={project} onChange={e => { setProject(e.target.value); setAttachments([]); setError(""); }}><option value="">请选择</option>{catalog.projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select></label>
    <div className="admin-form-row">
      {text("zoneId", "所在地时区（如 Asia/Shanghai）")}
      {text("cutoffHour", "日切小时", "number")}
      {text("minCutoffHour", "日切下限", "number")}
      {text("maxCutoffHour", "日切上限", "number")}
      {text("effectiveAt", "生效时间（当前设备时区）", "datetime-local")}
      {text("reason", "变更原因")}
      {text("finalApproverId", "组织链最终审批人账号（可选）")}
      <label>配置依据（可选）<input aria-label="配置依据（可选）" type="file" multiple disabled={busy || !project} onChange={e => void upload(e.target.files)} /></label>
      <span>已上传 {attachments.length} 份依据</span>
      <PermissionButton permissionsState={effectivePermissions} permissions={["amusement.business_day.manage"]} disabled={busy} onClick={() => void action(submit)}>提交最高权限审批</PermissionButton>
    </div>
    <table className="admin-table"><thead><tr><th>版本</th><th>所在地时区／日切</th><th>上下限</th><th>生效时间</th><th>审批状态／原件</th></tr></thead><tbody>
      {catalog.versions.map(row => <tr key={row.id}><td>{row.version}<p>{row.reason}</p></td><td>{row.zoneId}／{row.cutoffHour}:00</td><td>{row.minCutoffHour}～{row.maxCutoffHour} 时</td><td>{new Date(row.effectiveAt).toLocaleString("zh-CN", { timeZone: row.zoneId })}（{row.zoneId}）</td><td>{row.status === "expired" ? "超过拟生效时点，须重新提交" : formatBusinessStatus(row.status)}{row.reviewId && <button type="button" onClick={() => navigateTo?.("ADM-GOV-009", row.reviewId)}>查看审批原件</button>}</td></tr>)}
      {project && catalog.versions.length === 0 && <tr><td colSpan={5}>该综合体尚无游乐营业日配置，请提交并完成审批。</td></tr>}
    </tbody></table>
  </section>;
}
