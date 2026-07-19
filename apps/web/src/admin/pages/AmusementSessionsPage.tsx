import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps, AmusementProjectRow, AmusementSessionRow, AmusementVerificationRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";

const SESSION_MANAGE = ["amusement.session.manage"];

const TEXT = {
  title: "项目与场次",
  description: "管理游乐项目与场次容量，后续串联预约、核销和安全规则。",
  evidence: "接口证据：读取 /api/v1/amusement/projects、/api/v1/amusement/sessions；新建项目调用 /api/v1/amusement/projects；新增场次调用 /api/v1/amusement/sessions。",
  boundary: "项目与场次是预约、核销和安全规则的基础资源；本页只维护项目/场次资源，不替代安全巡检和事故闭环。",
  loadFailed: "游乐项目与场次读取失败",
  nameRequired: "场次名称不能为空。",
  projectNameRequired: "项目名称不能为空。",
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
  const [verifications, setVerifications] = React.useState<AmusementVerificationRow[]>([]);
  const [sessionName, setSessionName] = React.useState("验收场次");
  const [capacity, setCapacity] = React.useState("12");
  const [projectName, setProjectName] = React.useState("新游乐项目");
  const [projectCapacity, setProjectCapacity] = React.useState("30");
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [projectSubmitting, setProjectSubmitting] = React.useState(false);
  // 引导式流程 phase-7：新增场次收起为主按钮 + 分步引导弹窗。
  const [sessionOpen, setSessionOpen] = React.useState(false);
  // 引导式流程 phase-8：新建项目也收起为主按钮 + 分步引导弹窗（与场次流各自独立 testid/弹窗，不互相影响）。
  const [projectOpen, setProjectOpen] = React.useState(false);
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 项目/场次完整字段（时间中文·状态中文·引用可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

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
      const canViewVerifications = hasPermissions(effectivePermissions, ["amusement.verification.view"]);
      const [nextProjects, nextSessions, nextVerifications] = await Promise.all([
        api.get<AmusementProjectRow[]>("/api/v1/amusement/projects"),
        api.get<AmusementSessionRow[]>("/api/v1/amusement/sessions"),
        canViewVerifications ? api.get<AmusementVerificationRow[]>("/api/v1/amusement/verifications") : Promise.resolve([]),
      ]);
      setProjects(nextProjects);
      setSessions(nextSessions);
      setVerifications(nextVerifications);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  async function createProject() {
    if (!projectName.trim()) {
      setFormError(TEXT.projectNameRequired);
      return;
    }
    const numericCapacity = Math.max(0, Number(projectCapacity) || 0);
    setFormError("");
    setGlobalError(null);
    setProjectSubmitting(true);
    const request = {
      idempotencyKey: `amusement-project-create-${Date.now()}`,
      operatorNote: "完整后台新建游乐项目",
      evidenceAttachmentIds: ["att-amusement-project-create"],
      projectName,
      capacity: numericCapacity,
      openStatus: "open",
    };
    try {
      const result = await api.post<unknown>("/api/v1/amusement/projects", request);
      setActionResult({ action: "amusement_project_created", evidenceAttachmentIds: request.evidenceAttachmentIds, request, result });
      setProjectOpen(false);
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "新建项目失败";
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "amusement_project_created", status: "failed", message });
    } finally {
      setProjectSubmitting(false);
    }
  }

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
      setSessionOpen(false);
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
  // KPI 改走统一明细端点（数字=明细合计同源）；失败/无权限回退本地列表值并隐藏入口，体验不降级。
  const sessionCountKpi = useMetricKpiValue(api, "amusement.session_count", {});
  const capacityKpi = useMetricKpiValue(api, "amusement.session_capacity", {});
  const sessionCountDisplay = sessionCountKpi.status === "ready" && sessionCountKpi.value !== null ? sessionCountKpi.value : sessions.length;
  const capacityDisplay = capacityKpi.status === "ready" && capacityKpi.value !== null ? capacityKpi.value : totalCapacity;

  const pagedSessions = usePagedRows(sessions, 10);

  function openProjectDetail(row: AmusementProjectRow) {
    setDetail({
      title: `项目明细 · ${row.projectName ?? row.id}`,
      fields: [
        { label: "项目", value: display(row.projectName) },
        { label: "编码", value: display(row.projectCode) },
        { label: "容量", value: display(row.capacity) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "项目编号", value: row.id },
      ],
    });
  }

  function openSessionDetail(row: AmusementSessionRow) {
    setDetail({
      title: `场次明细 · ${row.sessionName ?? row.id}`,
      fields: [
        { label: "场次", value: display(row.sessionName) },
        {
          label: "项目",
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{display(row.projectId)}</TraceLink>,
        },
        { label: "开始", value: cnDate(row.startsAt) },
        { label: "结束", value: cnDate(row.endsAt) },
        { label: "容量", value: display(row.capacity) },
        { label: "已用", value: display(row.usedCapacity) },
        { label: "锁定", value: display(row.lockedCapacity) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "场次编号", value: row.id },
      ],
    });
  }

  const projectsSection = (
    <>
      <div className="admin-section-title">项目</div>
      <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-projects-table"><thead><tr><th>项目</th><th>编码</th><th>容量</th><th>状态</th><th>详情</th></tr></thead><tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`amusement-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.projectName)}</td><td>{display(row.projectCode)}</td><td>{display(row.capacity)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody></table></div>
    </>
  );

  // V232 补时间第2批：游乐核销记录展示「核销时间 + 对应场次」（场次以名称呈现，取自本页场次表）。
  const sessionNameById = React.useMemo(() => {
    const map = new Map<string, string>();
    for (const row of sessions) map.set(row.id, String(row.sessionName ?? row.id));
    return map;
  }, [sessions]);
  const verificationsSection = (
    <>
      <div className="admin-section-title">核销记录</div>
      <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-verifications-table"><thead><tr><th>核销时间</th><th>对应场次</th><th>结果</th><th>核销人数</th><th>设备</th></tr></thead><tbody>{verifications.length === 0 ? <tr data-testid="amusement-verifications-empty-row"><td colSpan={5}>暂无核销记录</td></tr> : verifications.map((row) => <tr key={row.id}><td>{display(row.createdAt)}</td><td>{display(sessionNameById.get(String(row.sessionId ?? "")) ?? row.sessionId)}</td><td>{formatBusinessStatus(row.verificationResult)}</td><td>{display(row.verifiedPeople)}</td><td>{display(row.deviceId)}</td></tr>)}</tbody></table></div>
    </>
  );

  const sessionsSection = (
    <>
      <div className="admin-section-title">场次</div>
      <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-sessions-table"><thead><tr><th>场次</th><th>项目</th><th>开始</th><th>结束</th><th>容量</th><th>已用</th><th>锁定</th><th>状态</th><th>详情</th></tr></thead><tbody>{pagedSessions.pageRows.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`amusement-session-row-${row.id}`} onClick={() => openSessionDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSessionDetail(row); } }}><td>{display(row.sessionName)}</td><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{display(row.projectId)}</TraceLink></td><td>{cnDate(row.startsAt)}</td><td>{cnDate(row.endsAt)}</td><td>{display(row.capacity)}</td><td>{display(row.usedCapacity)}</td><td>{display(row.lockedCapacity)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看场次详情" onClick={(event) => { event.stopPropagation(); openSessionDetail(row); }}>查看详情</button></td></tr>)}</tbody></table><TablePager testid="amusement-sessions-pager" page={pagedSessions.page} pageCount={pagedSessions.pageCount} total={pagedSessions.total} onPrev={pagedSessions.prev} onNext={pagedSessions.next} /></div>
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
        {kpiFocus ? (
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
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>场次</span><button type="button" className="admin-kpi-value" data-testid="amusement-sessions-kpi-sessions" onClick={() => enterKpiFocus("sessions")}>{sessionCountDisplay}</button><small>场次数{sessionCountKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="amusement.session_count" testid="amusement-sessions-metric-detail-sessions" /></> : null}</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>总容量</span><button type="button" className="admin-kpi-value" data-testid="amusement-sessions-kpi-capacity" onClick={() => enterKpiFocus("sessions")}>{capacityDisplay}</button><small>容量合计{capacityKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="amusement.session_capacity" testid="amusement-sessions-metric-detail-capacity" /></> : null}</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>已锁定</span><button type="button" className="admin-kpi-value" data-testid="amusement-sessions-kpi-locked" onClick={() => enterKpiFocus("sessions")}>{sessions.reduce((sum, row) => sum + Number(row.lockedCapacity ?? 0), 0)}</button><small>已锁定容量</small></div>
            </div>
            <div className="admin-form-row"><PermissionButton permissionsState={effectivePermissions} permissions={SESSION_MANAGE} data-testid="amusement-project-create-open" disabled={projectSubmitting} onClick={() => { setFormError(""); setProjectOpen(true); }}>＋ 新建项目</PermissionButton></div>
            <div className="admin-form-row"><PermissionButton permissionsState={effectivePermissions} permissions={SESSION_MANAGE} data-testid="amusement-session-create-open" disabled={submitting} onClick={() => { setFormError(""); setSessionOpen(true); }}>＋ 新增场次</PermissionButton><span className="admin-muted-action" data-testid="amusement-sessions-boundary">{TEXT.boundary}</span></div>
            {projectsSection}
            {sessionsSection}
            {verificationsSection}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      <GuidedFlowModal
        open={projectOpen}
        title="新建项目"
        testId="amusement-project-guided"
        submitTestId="amusement-project-create"
        submitLabel="新建项目"
        submitting={projectSubmitting}
        error={formError || undefined}
        onClose={() => setProjectOpen(false)}
        onSubmit={() => void createProject()}
        steps={[
          {
            title: "项目信息",
            description: "填写项目名称与容量，项目名称必填、容量取不小于 0 的数字。",
            validate: () => (!projectName.trim() ? TEXT.projectNameRequired : null),
            content: (
              <>
                <label>项目名称<input data-testid="amusement-project-name" value={projectName} onChange={(event) => setProjectName(event.target.value)} /></label>
                <label>项目容量<input data-testid="amusement-project-capacity" value={projectCapacity} onChange={(event) => setProjectCapacity(event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "项目名称", value: projectName.trim() || "未填写" },
          { label: "项目容量", value: String(Math.max(0, Number(projectCapacity) || 0)) },
        ]}
      />
      <GuidedFlowModal
        open={sessionOpen}
        title="新增场次"
        testId="amusement-session-guided"
        submitTestId="amusement-session-create"
        submitLabel="新增场次"
        submitting={submitting}
        error={formError || undefined}
        onClose={() => setSessionOpen(false)}
        onSubmit={() => void createSession()}
        steps={[
          {
            title: "场次信息",
            description: "填写场次名称与容量，容量至少 1；场次归属首个项目。",
            validate: () => (!sessionName.trim() ? TEXT.nameRequired : null),
            content: (
              <>
                <label>场次名称<input data-testid="amusement-session-name" value={sessionName} onChange={(event) => setSessionName(event.target.value)} /></label>
                <label>容量<input data-testid="amusement-session-capacity" value={capacity} onChange={(event) => setCapacity(event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "场次名称", value: sessionName.trim() || "未填写" },
          { label: "容量", value: String(Math.max(1, Number(capacity) || 1)) },
          { label: "归属项目", value: projects[0]?.projectName ?? projects[0]?.id ?? "amuse-1" },
        ]}
      />
    </section>
  );
}
