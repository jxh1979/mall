import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, AmusementProjectRow, AmusementSessionRow } from "../types";
import { TraceLink } from "../components/TraceLink";

const SESSION_MANAGE = ["amusement.session.manage"];

const TEXT = {
  title: "项目与场次",
  description: "管理游乐项目与场次容量，后续串联预约、核销和安全规则。",
  evidence: "接口证据：读取 /api/v1/amusement/projects、/api/v1/amusement/sessions；新增场次调用 /api/v1/amusement/sessions。",
  boundary: "项目与场次是预约、核销和安全规则的基础资源；本页只维护项目/场次资源，不替代安全巡检和事故闭环。",
  loadFailed: "游乐项目与场次读取失败",
  nameRequired: "场次名称不能为空。",
  focusClear: "清除筛选，返回全部",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return JSON.stringify(value);
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<projects|sessions>；项目卡→项目表，
// 场次/总容量/已锁定卡→场次表（都是场次表的聚合口径，聚焦同一张场次表）。pathname 不动保子路径。
type KpiTarget = "projects" | "sessions";

function readKpiFocusFromUrl(): KpiTarget | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  return table === "projects" || table === "sessions" ? table : null;
}

export function AmusementSessionsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [projects, setProjects] = React.useState<AmusementProjectRow[]>([]);
  const [sessions, setSessions] = React.useState<AmusementSessionRow[]>([]);
  const [sessionName, setSessionName] = React.useState("验收场次");
  const [capacity, setCapacity] = React.useState("12");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(table);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

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

  const pagedSessions = usePagedRows(sessions, 10);

  const projectsSection = (
    <>
      <div className="admin-section-title">项目</div>
      <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-projects-table"><thead><tr><th>项目</th><th>编码</th><th>容量</th><th>状态</th></tr></thead><tbody>{projects.map((row) => <tr key={row.id}><td>{display(row.projectName)}</td><td>{display(row.projectCode)}</td><td>{display(row.capacity)}</td><td>{formatBusinessStatus(row.status)}</td></tr>)}</tbody></table></div>
    </>
  );

  const sessionsSection = (
    <>
      <div className="admin-section-title">场次</div>
      <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-sessions-table"><thead><tr><th>场次</th><th>项目</th><th>开始</th><th>结束</th><th>容量</th><th>已用</th><th>锁定</th><th>状态</th></tr></thead><tbody>{pagedSessions.pageRows.map((row) => <tr key={row.id}><td>{display(row.sessionName)}</td><td><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{display(row.projectId)}</TraceLink></td><td>{display(row.startsAt)}</td><td>{display(row.endsAt)}</td><td>{display(row.capacity)}</td><td>{display(row.usedCapacity)}</td><td>{display(row.lockedCapacity)}</td><td>{formatBusinessStatus(row.status)}</td></tr>)}</tbody></table><TablePager testid="amusement-sessions-pager" page={pagedSessions.page} pageCount={pagedSessions.pageCount} total={pagedSessions.total} onPrev={pagedSessions.prev} onNext={pagedSessions.next} /></div>
    </>
  );

  const focusCount = kpiFocus === "projects" ? projects.length : sessions.length;

  return (
    <section data-testid="amusement-sessions-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="amusement-sessions-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="amusement_project.viewed / amusement_session.created" />
      {formError ? <div className="admin-inline-error" data-testid="amusement-sessions-form-error">{formError}</div> : null}
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {projects.length === 0 && sessions.length === 0 ? <div className="admin-state admin-empty" data-testid="amusement-sessions-empty">暂无游乐项目与场次</div> : kpiFocus ? (
          <>
            <div className="admin-state" data-testid="amusement-sessions-kpi-focus-banner">
              正在查看：{kpiFocus === "projects" ? "项目" : "场次"}（{focusCount}）
              <button type="button" data-testid="amusement-sessions-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {kpiFocus === "projects" ? projectsSection : sessionsSection}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="amusement-sessions-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>项目</span><button type="button" className="admin-kpi-value" data-testid="amusement-sessions-kpi-projects" onClick={() => enterKpiFocus("projects")}>{projects.length}</button><small>项目数</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>场次</span><button type="button" className="admin-kpi-value" data-testid="amusement-sessions-kpi-sessions" onClick={() => enterKpiFocus("sessions")}>{sessions.length}</button><small>场次数</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>总容量</span><button type="button" className="admin-kpi-value" data-testid="amusement-sessions-kpi-capacity" onClick={() => enterKpiFocus("sessions")}>{totalCapacity}</button><small>容量合计</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>已锁定</span><button type="button" className="admin-kpi-value" data-testid="amusement-sessions-kpi-locked" onClick={() => enterKpiFocus("sessions")}>{sessions.reduce((sum, row) => sum + Number(row.lockedCapacity ?? 0), 0)}</button><small>已锁定容量</small></div>
            </div>
            <div className="admin-form-grid"><label>场次名称<input data-testid="amusement-session-name" value={sessionName} onChange={(event) => setSessionName(event.target.value)} /></label><label>容量<input data-testid="amusement-session-capacity" value={capacity} onChange={(event) => setCapacity(event.target.value)} /></label></div>
            <div className="admin-form-row"><PermissionButton permissionsState={effectivePermissions} permissions={SESSION_MANAGE} data-testid="amusement-session-create" disabled={submitting} onClick={() => void createSession()}>新增验收场次</PermissionButton><span className="admin-muted-action" data-testid="amusement-sessions-boundary">{TEXT.boundary}</span></div>
            {projectsSection}
            {sessionsSection}
          </>
        )}
      </AdminDataState>
    </section>
  );
}
