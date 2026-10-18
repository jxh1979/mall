// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { cnDate } from "../components/RowDetailDrawer";
import { useAdminResource } from "../hooks/useAdminResource";
import { useCan } from "../security/Can";
import type { AdminPageProps } from "../types";

// CG-08a 全园故障状态（ENUM「全园故障与补偿通票」）：由游乐公司负责人和工程或安全岗位共同确认后生效，一经生效立即停售
// 纯游乐门票与含游乐权益通票；解除同样两岗位共同确认，解除后恢复。客户端 unwrap 已 camel 化。
type FaultRow = {
  id: string;
  projectId?: string;
  status?: string;
  reason?: string | null;
  initiatedBy?: string | null;
  initiatedRole?: string | null;
  initiatedAt?: string | null;
  activationConfirmedBy?: string | null;
  activationConfirmedRole?: string | null;
  activatedAt?: string | null;
  liftInitiatedBy?: string | null;
  liftInitiatedRole?: string | null;
  liftInitiatedAt?: string | null;
  liftReason?: string | null;
  liftConfirmedBy?: string | null;
  liftConfirmedRole?: string | null;
  liftedAt?: string | null;
};
type ActionRow = { id: string; action?: string; actorId?: string | null; actorRole?: string | null; note?: string | null; createdAt?: string | null };

const ROLE_HEAD = "amusement_head";
const ROLE_ENGINEERING_SAFETY = "engineering_safety";
const ROLE_LABELS: Record<string, string> = { [ROLE_HEAD]: "游乐公司负责人", [ROLE_ENGINEERING_SAFETY]: "工程或安全岗位" };
const STATUS_LABELS: Record<string, string> = {
  pending_activation: "待第二岗位确认生效",
  active: "生效中（已停售纯游乐票与含游乐权益通票）",
  pending_lift: "待第二岗位确认解除（仍停售）",
  lifted: "已解除（已恢复销售）",
};
const ACTION_LABELS: Record<string, string> = {
  initiate: "发起全园故障状态", confirm_activation: "第二岗位确认生效", initiate_lift: "发起解除", confirm_lift: "第二岗位确认解除",
};
const PERM_HEAD = "amusement.park_fault.confirm_as_head";
const PERM_ENGINEERING_SAFETY = "amusement.park_fault.confirm_as_engineering_safety";

const roleLabel = (v?: string | null) => (v ? (ROLE_LABELS[v] ?? v) : "—");
const statusLabel = (v?: string | null) => (v ? (STATUS_LABELS[v] ?? v) : "—");
const person = (who?: string | null, role?: string | null, at?: string | null) => (who ? `${who}（${roleLabel(role)}）· ${cnDate(at)}` : "—");

/**
 * 全园故障状态页（ADM-AMU-006）。
 * 生效与解除各两步：一个岗位发起，另一个岗位确认；两次须不同人且分别持两个岗位的权限。后台只能以本人所持岗位操作。
 */
export function AmusementParkWideFaultPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const canHead = useCan(effectivePermissions, [PERM_HEAD]);
  const canEngineering = useCan(effectivePermissions, [PERM_ENGINEERING_SAFETY]);
  const canAct = canHead || canEngineering;
  const [projectId, setProjectId] = React.useState("");
  const projectIdRef = React.useRef("");
  const [rows, setRows] = React.useState<FaultRow[]>([]);
  const [actions, setActions] = React.useState<ActionRow[]>([]);
  const [role, setRole] = React.useState(canHead ? ROLE_HEAD : ROLE_ENGINEERING_SAFETY);
  const [reason, setReason] = React.useState("");
  const [note, setNote] = React.useState("");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const current = rows.find((row) => row.status !== "lifted") ?? null;

  const { loading, error, reload } = useAdminResource(
    async () => {
      const query = projectIdRef.current.trim() ? `?project_id=${encodeURIComponent(projectIdRef.current.trim())}` : "";
      const items = await api.get<FaultRow[]>(`/api/v1/amusement/park-wide-faults${query}`);
      const list = Array.isArray(items) ? items : [];
      setRows(list);
      const open = list.find((row) => row.status !== "lifted");
      if (open) {
        const trace = await api.get<ActionRow[]>(`/api/v1/amusement/park-wide-faults/${encodeURIComponent(open.id)}/actions`);
        setActions(Array.isArray(trace) ? trace : []);
      } else {
        setActions([]);
      }
    },
    { onError: setGlobalError, fallbackMessage: "全园故障状态读取失败" },
  );

  async function run(label: string, call: () => Promise<unknown>, result: Record<string, string>) {
    setFormError("");
    setSubmitting(true);
    try {
      await call();
      setActionResult({ 结果: label, ...result });
      setReason("");
      setNote("");
      await reload();
    } catch (actionError) {
      setFormError(actionError instanceof Error ? actionError.message : `${label}失败`);
    } finally {
      setSubmitting(false);
    }
  }

  function initiate() {
    const pid = projectId.trim();
    if (!pid) { setFormError("发起前须填写综合体项目编号。"); return; }
    if (!reason.trim()) { setFormError("发起全园故障状态须填写故障情形（全部游乐项目不可运行，或供电、消防、入口系统等公共关键设施故障）。"); return; }
    void run("全园故障状态已发起，等待另一岗位确认后生效",
      () => api.post("/api/v1/amusement/park-wide-faults", { projectId: pid, role, reason: reason.trim() }),
      { 综合体项目: pid, 发起岗位: roleLabel(role) });
  }

  function confirmActivation(row: FaultRow) {
    void run("全园故障状态已生效，已停售纯游乐票与含游乐权益通票",
      () => api.post(`/api/v1/amusement/park-wide-faults/${encodeURIComponent(row.id)}/confirm-activation`, { role, note: note.trim() || undefined }),
      { 记录: row.id, 确认岗位: roleLabel(role) });
  }

  function initiateLift(row: FaultRow) {
    if (!reason.trim()) { setFormError("发起解除须填写解除依据（公共关键设施或游乐项目恢复情况）。"); return; }
    void run("已发起解除，等待另一岗位确认后恢复销售",
      () => api.post(`/api/v1/amusement/park-wide-faults/${encodeURIComponent(row.id)}/initiate-lift`, { role, reason: reason.trim() }),
      { 记录: row.id, 发起岗位: roleLabel(role) });
  }

  function confirmLift(row: FaultRow) {
    void run("全园故障状态已解除，已恢复销售",
      () => api.post(`/api/v1/amusement/park-wide-faults/${encodeURIComponent(row.id)}/confirm-lift`, { role, note: note.trim() || undefined }),
      { 记录: row.id, 确认岗位: roleLabel(role) });
  }

  return (
    <section data-testid="amusement-park-wide-fault-page">
      <AdminPageHeader
        title="全园故障状态"
        description="全园故障（全部游乐项目均不可运行，或供电、消防、入口系统等公共关键设施故障导致游乐园必须整体关闭）由游乐公司负责人和工程或安全岗位共同确认后生效，一经生效立即停止所有纯游乐门票和所有包含游乐权益的通票的新销售；解除同样由两岗位共同确认，解除后恢复销售。局部设备或单个项目故障不在本页处理。"
      />
      <AuditTrailHint event="amusement.park_fault.initiated / amusement.park_fault.activated / amusement.park_fault.lift_initiated / amusement.park_fault.lifted" />
      <div className="admin-form-row" data-testid="park-fault-query-row">
        <label>综合体项目编号
          <input aria-label="综合体项目编号" data-testid="park-fault-project" value={projectId}
            onChange={(event) => { setProjectId(event.target.value); projectIdRef.current = event.target.value; }} />
        </label>
        <button type="button" data-testid="park-fault-reload" onClick={() => void reload()}>按项目查询</button>
      </div>
      <div className="admin-form-row" data-testid="park-fault-editor">
        <label>本次操作岗位
          <select aria-label="本次操作岗位" data-testid="park-fault-role" value={role} onChange={(event) => setRole(event.target.value)}>
            <option value={ROLE_HEAD} disabled={!canHead}>游乐公司负责人</option>
            <option value={ROLE_ENGINEERING_SAFETY} disabled={!canEngineering}>工程或安全岗位</option>
          </select>
        </label>
        <label>故障情形 / 解除依据
          <input aria-label="故障情形或解除依据" data-testid="park-fault-reason" placeholder="例：供电故障，游乐园整体关闭" value={reason} onChange={(event) => setReason(event.target.value)} />
        </label>
        <label>确认说明
          <input aria-label="确认说明" data-testid="park-fault-note" value={note} onChange={(event) => setNote(event.target.value)} />
        </label>
        {!current ? (
          <button type="button" data-testid="park-fault-initiate" disabled={submitting || !canAct} onClick={initiate}>发起全园故障状态</button>
        ) : null}
        {current?.status === "pending_activation" ? (
          <button type="button" data-testid="park-fault-confirm-activation" disabled={submitting || !canAct} onClick={() => confirmActivation(current)}>第二岗位确认生效（即刻停售）</button>
        ) : null}
        {current?.status === "active" ? (
          <button type="button" data-testid="park-fault-initiate-lift" disabled={submitting || !canAct} onClick={() => initiateLift(current)}>发起解除</button>
        ) : null}
        {current?.status === "pending_lift" ? (
          <button type="button" data-testid="park-fault-confirm-lift" disabled={submitting || !canAct} onClick={() => confirmLift(current)}>第二岗位确认解除（恢复销售）</button>
        ) : null}
        <span className="admin-muted-action">两次确认须由不同人、分别以两个岗位作出；发起人不得自行确认，同一岗位两人不构成共同确认。</span>
      </div>
      {formError ? <div data-testid="park-fault-error" className="admin-inline-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>当前状态</h3>
        {current ? (
          <div className="admin-kpi-card" data-testid="park-fault-current">
            <strong data-testid="park-fault-current-status">{statusLabel(current.status)}</strong>
            <div>综合体项目：{current.projectId ?? "—"}</div>
            <div>故障情形：{current.reason ?? "—"}</div>
            <div>发起：{person(current.initiatedBy, current.initiatedRole, current.initiatedAt)}</div>
            <div>确认生效：{person(current.activationConfirmedBy, current.activationConfirmedRole, current.activatedAt)}</div>
            <div>发起解除：{person(current.liftInitiatedBy, current.liftInitiatedRole, current.liftInitiatedAt)}{current.liftReason ? `，依据：${current.liftReason}` : ""}</div>
          </div>
        ) : <p data-testid="park-fault-none">当前没有未解除的全园故障状态，游乐票与含游乐权益通票正常销售。</p>}
        {current ? (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="park-fault-actions-table">
              <thead><tr><th>动作</th><th>操作人</th><th>岗位</th><th>说明</th><th>时间</th></tr></thead>
              <tbody>
                {actions.map((row) => (
                  <tr key={row.id} data-testid={`park-fault-action-${row.id}`}>
                    <td>{row.action ? (ACTION_LABELS[row.action] ?? row.action) : "—"}</td>
                    <td className="admin-id-cell" title={row.actorId ?? ""}>{row.actorId ?? "—"}</td>
                    <td>{roleLabel(row.actorRole)}</td>
                    <td>{row.note ?? "—"}</td>
                    <td>{cnDate(row.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <h3>历史记录</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="park-fault-history-table">
            <thead><tr><th>综合体项目</th><th>状态</th><th>故障情形</th><th>发起</th><th>确认生效</th><th>发起解除</th><th>确认解除</th></tr></thead>
            <tbody>
              {rows.length === 0 ? <tr><td colSpan={7}>暂无全园故障记录</td></tr> : rows.map((row) => (
                <tr key={row.id} data-testid={`park-fault-row-${row.id}`}>
                  <td>{row.projectId ?? "—"}</td>
                  <td data-testid={`park-fault-status-${row.id}`}>{statusLabel(row.status)}</td>
                  <td>{row.reason ?? "—"}</td>
                  <td>{person(row.initiatedBy, row.initiatedRole, row.initiatedAt)}</td>
                  <td>{person(row.activationConfirmedBy, row.activationConfirmedRole, row.activatedAt)}</td>
                  <td>{person(row.liftInitiatedBy, row.liftInitiatedRole, row.liftInitiatedAt)}</td>
                  <td>{person(row.liftConfirmedBy, row.liftConfirmedRole, row.liftedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
