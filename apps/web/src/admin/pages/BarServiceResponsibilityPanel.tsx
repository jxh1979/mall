// 文件版本：V0.02
import React from "react";
import { PermissionButton } from "../security/Can";
import { cnDate } from "../components/RowDetailDrawer";
import type { AdminPageProps, BarTableSessionRow } from "../types";

type Responsibility = { id: string; tableId: string; userId: string; payerCompanyId: string; startedAt: string; endedAt?: string; reason: string };
type Props = Pick<AdminPageProps, "api" | "effectivePermissions" | "setActionResult"> & { session?: BarTableSessionRow; onChanged: () => void };
export function BarServiceResponsibilityPanel({ api, effectivePermissions, setActionResult, session, onChanged }: Props) {
  const [rows, setRows] = React.useState<Responsibility[]>([]);
  const [next, setNext] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [finished, setFinished] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const attempt = React.useRef<{ body: string; key: string } | null>(null);
  React.useEffect(() => { setRows([]); setNext(""); setReason(""); setFinished(false); setError(""); attempt.current = null; }, [session?.id]);
  async function load() {
    if (!session) return;
    setBusy(true); setError("");
    try { setRows(await api.get<Responsibility[]>("/api/v1/bar/table-sessions/" + encodeURIComponent(session.id) + "/service-responsibilities")); }
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
  return <details data-testid="bar-service-responsibilities"><summary>本场服务交接与小费责任</summary>
    <p>当前场次：{session?.id ?? "请先选择"}。小费按实际负责该台的时长分配，交接记录将保留原人员和原时间。</p>
    {error ? <p role="alert">{error}</p> : null}
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.payroll.view"]} disabled={busy || !session} onClick={() => void load()}>查看责任历史</PermissionButton>
    <table className="admin-table"><thead><tr><th>原台位</th><th>责任人／发薪主体</th><th>开始／结束</th><th>依据</th></tr></thead><tbody>{rows.map(row => <tr key={row.id}><td>{row.tableId}</td><td>{row.userId}／{row.payerCompanyId}</td><td>{cnDate(row.startedAt)}／{row.endedAt ? cnDate(row.endedAt) : "当前接手中"}</td><td>{row.reason}</td></tr>)}</tbody></table>
    <label>接手员工账号<input aria-label="接手员工账号" value={next} onChange={e => setNext(e.target.value)} /></label>
    <label>实际交接或结束原因<input aria-label="实际交接或结束原因" value={reason} onChange={e => setReason(e.target.value)} /></label>
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.table_session.transfer"]} disabled={busy || session?.status !== "open"} onClick={() => void write("service-handover")}>确认此刻交接</PermissionButton>
    <label><input type="checkbox" checked={finished} onChange={e => setFinished(e.target.checked)} />本场及并入场次的实际服务均已结束</label>
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.table.open_close"]} disabled={busy || session?.status !== "open" || !finished} onClick={() => void write("service-close")}>关台（登记实际服务结束）</PermissionButton>
    <p>关台须台组全部账单已结清或已并入主账单；挂账、未结账单一律不得关台，关台后台位进入待清台，未达最低消费的已付定金没收。</p>
    <p>缺少早先责任区间时仍须补录复核，交接操作不会把未知历史自动补给当前人员。</p>
  </details>;
}
