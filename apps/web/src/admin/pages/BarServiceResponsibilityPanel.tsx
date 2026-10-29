// 文件版本：V0.03
import React from "react";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, BarTableSessionRow } from "../types";
import { BackfillFormModal, BackfillTail, ManualTag, failure, loadSessionBackfills, time, type BackfillProof, type SessionBackfill } from "./BarSessionBackfill";

type Responsibility = { id: string; tableId: string; userId: string; payerCompanyId: string; startedAt: string; endedAt?: string; reason: string };
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult"> & { session?: BarTableSessionRow; onChanged: () => void };
// 18F 复核：开台与交接时刻由服务端取（带秒和秒以下），后端要求补录区间不早于开台、不与已有区间重叠，卡座小费又要求按毫秒覆盖开台到关台
// （BSM:1077「以真实交接记录为依据」）。所以表单默认带出第一个空档两端的服务端原串、不改即原样提交，不经 datetime-local 往返。
const at = (value: string) => Date.parse(value);
const inputTime = (iso: string) => { if (!iso) return ""; const d = new Date(iso); return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16); };
function firstGap(session: BarTableSessionRow, history: Responsibility[]): { start: string; end: string } {
  let cursor = session.openedAt ?? "";
  for (const row of [...history].sort((a, b) => at(a.startedAt) - at(b.startedAt))) {
    if (cursor && at(row.startedAt) > at(cursor)) return { start: cursor, end: row.startedAt };
    if (!row.endedAt) return { start: "", end: "" };
    if (!cursor || at(row.endedAt) > at(cursor)) cursor = row.endedAt;
  }
  return session.closedAt && at(session.closedAt) <= at(cursor) ? { start: "", end: "" } : { start: cursor, end: session.closedAt ?? "" };
}
export function BarServiceResponsibilityPanel({ api, effectivePermissions, setActionResult, session, onChanged }: Props) {
  const [rows, setRows] = React.useState<Responsibility[]>([]);
  const [next, setNext] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [finished, setFinished] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const attempt = React.useRef<{ body: string; key: string } | null>(null);
  // 第十八批 F 段第 14 条 14-R7（BSM:1642、BSM:1613）：服务责任区间人工补录，待复核与已驳回的并列在历史表下方（已通过的已作为「人工补录」区间进历史表）。
  const [backfills, setBackfills] = React.useState<SessionBackfill[]>([]);
  const [form, setForm] = React.useState<{ user: string; start: string; end: string } | null>(null);
  const [formError, setFormError] = React.useState("");
  React.useEffect(() => { setRows([]); setBackfills([]); setForm(null); setNext(""); setReason(""); setFinished(false); setError(""); attempt.current = null; }, [session?.id]);
  async function load() {
    if (!session) return;
    setBusy(true); setError("");
    try {
      const [history, pending] = await Promise.all([
        api.get<Responsibility[]>("/api/v1/bar/table-sessions/" + encodeURIComponent(session.id) + "/service-responsibilities"),
        loadSessionBackfills(api, "bar_session_service", session.id),
      ]);
      setRows(history); setBackfills(pending.filter(row => row.status !== "approved"));
    }
    catch (e) { setError(e instanceof Error ? e.message : "责任区间读取失败"); }
    finally { setBusy(false); }
  }
  async function write(kind: "service-handover" | "service-close") {
    if (!session || session.sessionVersion == null || !reason.trim()) { setError("请先选择场次，并填写实际交接或服务结束原因"); return; }
    if (kind === "service-handover" && !next.trim()) { setError("请明确接手员工账号"); return; }
    if (kind === "service-close" && !finished) { setError("须确认本场及并入场次的实际服务已经结束"); return; }
    const body = { sessionVersion: session.sessionVersion, reason: reason.trim(), ...(kind === "service-handover" ? { nextUserId: next.trim() } : {}) };
    const fingerprint = session.id + kind + JSON.stringify(body);
    if (attempt.current?.body !== fingerprint) attempt.current = { body: fingerprint, key: crypto.randomUUID() };
    setBusy(true); setError("");
    try {
      const result = await api.post<Responsibility[]>("/api/v1/bar/table-sessions/" + encodeURIComponent(session.id) + "/" + kind, { ...body, idempotencyKey: attempt.current.key });
      setRows(result); setActionResult(result); attempt.current = null; setReason(""); setFinished(false); onChanged();
    } catch (e) { setError(e instanceof Error ? e.message : "服务责任记录失败"); }
    finally { setBusy(false); }
  }
  async function submitBackfill(proof: BackfillProof) {
    if (!session || !form) return;
    const requestBody = { user_id: form.user.trim(), started_at: form.start, ended_at: form.end, ...proof };
    setBusy(true); setFormError("");
    try {
      const result = await api.post("/api/v1/bar/table-sessions/" + encodeURIComponent(session.id) + "/service-backfill", requestBody);
      setActionResult({ action: "bar_service_backfill_submitted", requestBody, result }); setForm(null);
    } catch (e) { setFormError(failure(e, "服务责任补录提交失败")); }
    finally { setBusy(false); }
  }
  async function openBackfill() {
    if (!session) return;
    setFormError("");
    const history = await api.get<Responsibility[]>("/api/v1/bar/table-sessions/" + encodeURIComponent(session.id) + "/service-responsibilities").catch(() => rows);
    setForm({ user: "", ...firstGap(session, history) });
  }
  const pick = (value: string) => (value ? new Date(value).toISOString() : "");
  return <details data-testid="bar-service-responsibilities"><summary>本场服务交接与小费责任</summary>
    <p>当前场次：{session?.id ?? "请先选择"}。小费按实际负责该台的时长分配，交接记录将保留原人员和原时间。</p>
    {error ? <p role="alert">{error}</p> : null}
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.payroll.view"]} disabled={busy || !session} onClick={() => void load()}>查看责任历史</PermissionButton>
    <table className="admin-table" data-testid="bar-service-history"><thead><tr><th>原台位</th><th>责任人／发薪主体</th><th>开始／结束</th><th>依据</th></tr></thead><tbody>{rows.map(row => <tr key={row.id}><td>{row.tableId}</td><td>{row.userId}／{row.payerCompanyId}</td><td>{time(row.startedAt)}／{row.endedAt ? time(row.endedAt) : "当前接手中"}</td><td>{row.reason}</td></tr>)}</tbody></table>
    <table className="admin-table" data-testid="bar-service-backfills"><thead><tr><th>标记</th><th>拟责任人</th><th>开始／结束</th><th>证据</th><th>录音起始</th><th>复核单号</th><th>状态</th></tr></thead><tbody>{backfills.map(row => <tr key={row.id} data-testid={`bar-service-backfill-${row.id}`}><td><ManualTag /></td><td>{String(row.proposed?.userId ?? "—")}</td><td>{time(row.proposed?.startedAt as string)}／{time(row.proposed?.endedAt as string)}</td><BackfillTail row={row} /></tr>)}</tbody></table>
    <label>接手员工账号<input aria-label="接手员工账号" value={next} onChange={e => setNext(e.target.value)} /></label>
    <label>实际交接或结束原因<input aria-label="实际交接或结束原因" value={reason} onChange={e => setReason(e.target.value)} /></label>
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.table_session.transfer"]} disabled={busy || session?.status !== "open"} onClick={() => void write("service-handover")}>确认此刻交接</PermissionButton>
    <label><input type="checkbox" checked={finished} onChange={e => setFinished(e.target.checked)} />本场及并入场次的实际服务均已结束</label>
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.table.open_close"]} disabled={busy || session?.status !== "open" || !finished} onClick={() => void write("service-close")}>关台（登记实际服务结束）</PermissionButton>
    <p>关台须台组全部账单已结清或已并入主账单；挂账、未结账单一律不得关台，关台后台位进入待清台，未达最低消费的已付定金没收。</p>
    <p>缺少早先责任区间时，用「补录服务责任区间」附凭证提交主管复核，通过后才计入；交接操作不会把未知历史自动补给当前人员。</p>
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.table_session.transfer", "evidence.attachment.create"]} disabled={busy || !session} data-testid="bar-service-backfill-open" onClick={() => void openBackfill()}>补录服务责任区间</PermissionButton>
    <BackfillFormModal open={form !== null} title="补录服务责任区间" testId="bar-service-backfill-form" api={api} submitting={busy} error={formError}
      validate={() => (!form?.user.trim() ? "请填写实际负责的员工账号" : !form.start || !form.end ? "请填写负责开始与结束时间" : null)} onSubmit={proof => void submitBackfill(proof)} onClose={() => setForm(null)}>
      <label>实际负责的员工账号<input data-testid="bar-service-backfill-user" value={form?.user ?? ""} onChange={e => setForm(f => f && { ...f, user: e.target.value })} /></label>
      <label>负责开始<input data-testid="bar-service-backfill-start" type="datetime-local" value={inputTime(form?.start ?? "")} onChange={e => setForm(f => f && { ...f, start: pick(e.target.value) })} /></label>
      <label>负责结束<input data-testid="bar-service-backfill-end" type="datetime-local" value={inputTime(form?.end ?? "")} onChange={e => setForm(f => f && { ...f, end: pick(e.target.value) })} /></label>
    </BackfillFormModal>
  </details>;
}
