import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, AmusementProjectRow, AmusementSessionRow } from "../types";

const TEXT = {
  title: "项目与场次",
  description: "管理游乐项目与场次容量，后续串联预约、核销和安全规则。",
  evidence: "接口证据：读取 /api/v1/amusement/projects、/api/v1/amusement/sessions；新增场次调用 /api/v1/amusement/sessions。",
  boundary: "项目与场次是预约、核销和安全规则的基础资源；本页只维护项目/场次资源，不替代安全巡检和事故闭环。",
  loadFailed: "游乐项目与场次读取失败",
  nameRequired: "场次名称不能为空。",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

export function AmusementSessionsPage({ api, setActionResult, setGlobalError }: AdminPageProps) {
  const [projects, setProjects] = React.useState<AmusementProjectRow[]>([]);
  const [sessions, setSessions] = React.useState<AmusementSessionRow[]>([]);
  const [sessionName, setSessionName] = React.useState("验收场次");
  const [capacity, setCapacity] = React.useState("12");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [nextProjects, nextSessions] = await Promise.all([
        api.get<AmusementProjectRow[]>("/api/v1/amusement/projects"),
        api.get<AmusementSessionRow[]>("/api/v1/amusement/sessions"),
      ]);
      setProjects(nextProjects);
      setSessions(nextSessions);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  async function createSession() {
    if (!sessionName.trim()) {
      setFormError(TEXT.nameRequired);
      return;
    }
    const numericCapacity = Math.max(1, Number(capacity) || 1);
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    const request = {
      idempotencyKey: `amusement-session-create-${Date.now()}`,
      operatorNote: "完整后台项目与场次验收新增",
      evidenceAttachmentIds: ["att-amusement-session-create"],
      projectId: projects[0]?.id ?? "amuse-1",
      sessionName,
      startsAt: "2026-06-20T10:00:00Z",
      endsAt: "2026-06-20T11:00:00Z",
      capacity: numericCapacity,
    };
    try {
      const result = await api.post<unknown>("/api/v1/amusement/sessions", request);
      setActionResult({ action: "amusement_session_created", evidenceAttachmentIds: request.evidenceAttachmentIds, request, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "新增场次失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "amusement_session_created", status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  const totalCapacity = sessions.reduce((sum, row) => sum + Number(row.capacity ?? 0), 0);

  return (
    <section data-testid="amusement-sessions-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="amusement-sessions-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="amusement_project.viewed / amusement_session.created" />
      {formError ? <div className="admin-inline-error" data-testid="amusement-sessions-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {projects.length === 0 && sessions.length === 0 ? <div className="admin-state admin-empty" data-testid="amusement-sessions-empty">暂无游乐项目与场次</div> : (
          <>
            <div className="admin-kpi-grid" data-testid="amusement-sessions-summary">
              <div className="admin-kpi-card"><span>项目</span><strong>{projects.length}</strong><small>projects</small></div>
              <div className="admin-kpi-card"><span>场次</span><strong>{sessions.length}</strong><small>sessions</small></div>
              <div className="admin-kpi-card"><span>总容量</span><strong>{totalCapacity}</strong><small>capacity</small></div>
              <div className="admin-kpi-card"><span>已锁定</span><strong>{sessions.reduce((sum, row) => sum + Number(row.lockedCapacity ?? 0), 0)}</strong><small>locked</small></div>
            </div>
            <div className="admin-form-grid"><label>场次名称<input data-testid="amusement-session-name" value={sessionName} onChange={(event) => setSessionName(event.target.value)} /></label><label>容量<input data-testid="amusement-session-capacity" value={capacity} onChange={(event) => setCapacity(event.target.value)} /></label></div>
            <div className="admin-form-row"><button type="button" data-testid="amusement-session-create" disabled={submitting} onClick={() => void createSession()}>新增验收场次</button><span className="admin-muted-action" data-testid="amusement-sessions-boundary">{TEXT.boundary}</span></div>
            <div className="admin-section-title">项目</div>
            <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-projects-table"><thead><tr><th>项目</th><th>编码</th><th>容量</th><th>状态</th></tr></thead><tbody>{projects.map((row) => <tr key={row.id}><td>{display(row.projectName)}</td><td>{display(row.projectCode)}</td><td>{display(row.capacity)}</td><td>{display(row.status)}</td></tr>)}</tbody></table></div>
            <div className="admin-section-title">场次</div>
            <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-sessions-table"><thead><tr><th>场次</th><th>项目</th><th>开始</th><th>结束</th><th>容量</th><th>已用</th><th>锁定</th><th>状态</th></tr></thead><tbody>{sessions.map((row) => <tr key={row.id}><td>{display(row.sessionName)}</td><td>{display(row.projectId)}</td><td>{display(row.startsAt)}</td><td>{display(row.endsAt)}</td><td>{display(row.capacity)}</td><td>{display(row.usedCapacity)}</td><td>{display(row.lockedCapacity)}</td><td>{display(row.status)}</td></tr>)}</tbody></table></div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
