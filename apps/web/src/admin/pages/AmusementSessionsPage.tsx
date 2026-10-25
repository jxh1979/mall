// 文件版本：V0.04
import React from "react";
import { formatActionResult } from "../components/ActionResultView";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import { hasPermissions } from "../security/PermissionGate";
import type { AdminPageProps, AmusementProjectRow, AmusementProjectEntitlementRow, AmusementSessionRow, AmusementVerificationRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { RowActions, type RowAction } from "../components/RowActions";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";

const SESSION_MANAGE = ["amusement.session.manage"];
// V341（R-12 软删）：权益绑定停用/恢复走 manage；删除审批走 approve（只授平台最高管理员）。
const ENTITLEMENT_MANAGE = ["amusement.project_entitlement.manage"];
const ENTITLEMENT_APPROVE = ["amusement.project_entitlement.approve"];

const TEXT = {
  title: "项目与场次",
  description: "管理项目安全容量、场次时间及实际清场结束。",
  evidence: "接口证据：读取 /api/v1/amusement/projects、/api/v1/amusement/sessions、/api/v1/amusement/projects/{id}/entitlements；新建项目调用 /api/v1/amusement/projects；新增场次调用 /api/v1/amusement/sessions；权益绑定停用/恢复/申请删除/审批删除调用 /api/v1/amusement/projects/{id}/entitlements/{权益类型}/archive|unarchive|delete-request|delete-approve。",
  boundary: "项目与场次是预约、核销和安全规则的基础资源；本页只维护项目/场次资源，不替代安全巡检和事故闭环。",
  loadFailed: "游乐项目与场次读取失败",
  nameRequired: "场次名称不能为空。",
  projectNameRequired: "项目名称不能为空。",
  focusClear: "清除筛选，返回全部",
  entitlementTitle: "项目权益绑定",
  entitlementHint: "绑定决定该项目现场核销接受哪些权益类型。停用即存档：记录留库、前台不再显示、随时可恢复；彻底删除须填理由并由平台最高管理员批准。",
  entitlementArchivePrompt: "停用（存档）理由：",
  entitlementDeletePrompt: "删除理由（提交后需平台最高管理员批准）：",
  entitlementReasonRequired: "存档理由不能为空",
  entitlementDeleteReasonRequired: "删除理由不能为空",
  entitlementEmpty: "该项目暂无权益绑定",
  entitlementShowArchived: "查看已归档",
};

/** V341：绑定状态中文口径（生效中 / 已存档 / 待审批删除 / 已删除）。 */
function entitlementStatusLabel(row: AmusementProjectEntitlementRow): string {
  if (row.deleteStatus === "approved") return "已删除（可恢复）";
  if (row.deleteStatus === "pending") return "待审批删除";
  if (row.archivedAt) return "已存档";
  return "生效中";
}

/** TK18-R2：场次状态（ENUM L142 / BSM L1293）closed 读作「已结束」；共享码表的「已关闭」留给项目开放状态（BSM L1292）。 */
function sessionStatusLabel(status: string | null | undefined): string {
  return status === "closed" ? "已结束" : formatBusinessStatus(status);
}

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "number" || typeof value === "string") return String(value);
  if (Array.isArray(value)) return value.length ? value.join(" / ") : "-";
  return formatActionResult(value);
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<projects|sessions>；项目卡→项目表，
// 场次/总容量/已锁定卡→场次表（都是场次表的聚合口径，聚焦同一张场次表）。pathname 不动保子路径。
type KpiTarget = "projects" | "sessions";

function readKpiFocusFromUrl(): KpiTarget | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  return table === "projects" || table === "sessions" ? table : null;
}


const followupSourceLabels: Record<string, string> = { entry: "入场权益", reservation: "个人预约", group_booking: "团体预约" };
type SessionEnding = { id: string; mode: string; actualEndAt: string; plannedEndAt: string; clearedPeople: number; confirmedBy?: string; reason: string; businessDate: string; configVersionId: string };
type SessionFollowup = { id: string; sourceType: string; sourceId: string; status: string; orderId?: string; entitlementId?: string; resolutionType?: string; resolutionId?: string };
/** TK-18（2026-10-18 操作员裁决）：已入场待办比照 ENUM L148 全园故障补偿规则——发放前须游乐公司负责人与工程或安全岗位共同确认，发放后原票下恢复本次 1 次；预约待办仍按既有取消规则。 */
function followupStatusLabel(row: SessionFollowup): string {
  if (row.status === "completed") return row.resolutionType === "compensation_issued" ? "已按全园故障补偿规则发放补偿（原票下恢复本次 1 次）" : "已完成";
  return row.sourceType === "entry" ? "已入场，补偿待发放（须游乐公司负责人与工程或安全岗位共同确认）" : "待按原规则处理";
}
function SessionEndingHistory({ api, sessionId }: { api: AdminPageProps["api"]; sessionId: string }) {
  const [records, setRecords] = React.useState<SessionEnding[]>([]);
  const [tasks, setTasks] = React.useState<SessionFollowup[]>([]);
  const [notice, setNotice] = React.useState("正在读取清场记录…");
  React.useEffect(() => {
    let current = true;
    Promise.all([
      api.get<SessionEnding[]>(`/api/v1/amusement/sessions/${sessionId}/endings`),
      api.get<SessionFollowup[]>(`/api/v1/amusement/sessions/${sessionId}/followups`),
    ]).then(([ends, pending]) => {
      if (current) { setRecords(ends); setTasks(pending); setNotice(ends.length ? "" : "暂无清场记录"); }
    }).catch(error => { if (current) setNotice(error instanceof Error ? error.message : "清场记录读取失败"); });
    return () => { current = false; };
  }, [api, sessionId]);
  return <div data-testid="amusement-session-ending-history">
    {notice && <p>{notice}</p>}
    {records.map(row => <div key={row.id}>
      <p>{row.mode === "early" ? "负责人确认提前结束" : "计划场次结束"} · 实际结束：{cnDate(row.actualEndAt)} · 原计划：{cnDate(row.plannedEndAt)}</p>
      <p>释放本场在场人数：{row.clearedPeople}；确认人：{row.confirmedBy ?? "系统"}；原因：{row.reason}</p>
      <p>营业日：{row.businessDate}；配置版本：{row.configVersionId}</p>
    </div>)}
    {tasks.length > 0 && <table><thead><tr><th>待办来源</th><th>原记录</th><th>订单／权益</th><th>处理状态</th><th>实际结果</th></tr></thead><tbody>
      {tasks.map(row => <tr key={row.id}><td>{followupSourceLabels[row.sourceType] ?? row.sourceType}</td>
        <td>{row.sourceId}</td><td>{row.orderId ?? "—"}／{row.entitlementId ?? "—"}</td><td>{followupStatusLabel(row)}</td>
        <td>{row.resolutionId ?? "尚无实际结果"}</td></tr>)}
    </tbody></table>}
  </div>;
}

export function AmusementSessionsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [projects, setProjects] = React.useState<AmusementProjectRow[]>([]);
  const [sessions, setSessions] = React.useState<AmusementSessionRow[]>([]);
  const [verifications, setVerifications] = React.useState<AmusementVerificationRow[]>([]);
  const [sessionName, setSessionName] = React.useState("验收场次");
  const [sessionProjectId, setSessionProjectId] = React.useState("");
  const [startsAt, setStartsAt] = React.useState("");
  const [endsAt, setEndsAt] = React.useState("");
  const [ending, setEnding] = React.useState<AmusementSessionRow | null>(null);
  const [clearanceConfirmed, setClearanceConfirmed] = React.useState(false);
  const [clearedAt, setClearedAt] = React.useState("");
  const [endReason, setEndReason] = React.useState("");
  const [endKey, setEndKey] = React.useState("");
  const [endEvidence, setEndEvidence] = React.useState<Array<{id: string; name: string}>>([]);
  const [projectName, setProjectName] = React.useState("新游乐项目");
  const [projectCapacity, setProjectCapacity] = React.useState("");
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

  // V341（R-12 软删）：项目↔权益绑定管理区（默认只显示生效中；「查看已归档」才带出已存档/已删除行）。
  const [entitlementProjectId, setEntitlementProjectId] = React.useState("");
  const [entitlements, setEntitlements] = React.useState<AmusementProjectEntitlementRow[]>([]);
  const [showArchivedEntitlements, setShowArchivedEntitlements] = React.useState(false);
  const [entitlementNotice, setEntitlementNotice] = React.useState<string | null>(null);
  const canManageEntitlements = hasPermissions(effectivePermissions, ENTITLEMENT_MANAGE);

  const loadEntitlements = React.useCallback(async (projectId: string, includeArchived: boolean) => {
    if (!projectId || !canManageEntitlements) {
      setEntitlements([]);
      return;
    }
    try {
      const rows = await api.get<AmusementProjectEntitlementRow[]>(
        `/api/v1/amusement/projects/${projectId}/entitlements${includeArchived ? "?include_archived=true" : ""}`,
      );
      setEntitlements(rows);
      setEntitlementNotice(null);
    } catch (loadError) {
      // 副资源容错：权益绑定另需 amusement.project_entitlement.manage，读不到只降级为区块提示，不拖垮主内容。
      setEntitlements([]);
      setEntitlementNotice(loadError instanceof Error ? loadError.message : "权益绑定读取失败");
    }
  }, [api, canManageEntitlements]);

  React.useEffect(() => {
    if (!entitlementProjectId && projects.length > 0) {
      setEntitlementProjectId(String(projects[0]?.id ?? ""));
    }
  }, [projects, entitlementProjectId]);

  React.useEffect(() => {
    void loadEntitlements(entitlementProjectId, showArchivedEntitlements);
  }, [entitlementProjectId, showArchivedEntitlements, loadEntitlements]);

  async function postEntitlementAction(entitlementType: string, suffix: string, body: Record<string, unknown>, action: string) {
    try {
      await api.post(
        `/api/v1/amusement/projects/${entitlementProjectId}/entitlements/${encodeURIComponent(entitlementType)}/${suffix}`,
        body,
      );
      setActionResult({ action, projectId: entitlementProjectId, entitlementType });
      await loadEntitlements(entitlementProjectId, showArchivedEntitlements);
    } catch (actionError) {
      setGlobalError(actionError instanceof Error ? actionError.message : "操作失败");
    }
  }

  /** 停用=存档：理由必填（前端先拦，后端再强校验）。 */
  async function archiveEntitlement(entitlementType: string) {
    const input = window.prompt(TEXT.entitlementArchivePrompt);
    if (input === null) return;
    if (!input.trim()) {
      setGlobalError(TEXT.entitlementReasonRequired);
      return;
    }
    await postEntitlementAction(entitlementType, "archive",
      { action: "archive", reason: input.trim() }, "amusement_project_entitlement_archived");
  }

  /** 恢复：清存档/删除标记，绑定重新生效。 */
  async function restoreEntitlement(entitlementType: string) {
    await postEntitlementAction(entitlementType, "unarchive",
      { action: "unarchive" }, "amusement_project_entitlement_restored");
  }

  /** 申请删除：须先存档，删除理由必填。 */
  async function requestDeleteEntitlement(entitlementType: string) {
    const input = window.prompt(TEXT.entitlementDeletePrompt);
    if (input === null) return;
    if (!input.trim()) {
      setGlobalError(TEXT.entitlementDeleteReasonRequired);
      return;
    }
    await postEntitlementAction(entitlementType, "delete-request",
      { action: "delete_request", reason: input.trim() }, "amusement_project_entitlement_delete_requested");
  }

  /** 审批删除：approved=落墓碑（仍留库可恢复）/ rejected=退回已存档。 */
  async function decideDeleteEntitlement(entitlementType: string, decision: "approved" | "rejected") {
    await postEntitlementAction(entitlementType, "delete-approve",
      { action: decision }, `amusement_project_entitlement_delete_${decision}`);
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
    const numericCapacity = Number(projectCapacity);
    if (!Number.isInteger(numericCapacity) || numericCapacity <= 0) {
      setFormError("须填写经确认的大于零的项目硬安全容量。"); return;
    }
    setFormError("");
    setGlobalError(null);
    setProjectSubmitting(true);
    const request = {
      idempotencyKey: `amusement-project-create-${Date.now()}`,
      operatorNote: "完整后台新建游乐项目",
      evidenceAttachmentIds: [],
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
    if (!sessionProjectId || !startsAt || !endsAt || new Date(endsAt) <= new Date(startsAt)) {
      setFormError("须选择游乐项目，并填写开始及晚于开始的结束时间。"); return;
    }
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    const request = {
      idempotencyKey: `amusement-session-create-${Date.now()}`,
      evidenceAttachmentIds: [],
      projectId: sessionProjectId,
      sessionName,
      startTime: new Date(startsAt).toISOString(),
      endTime: new Date(endsAt).toISOString(),
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

  async function uploadEndEvidence(files: FileList | null) {
    if (!ending || !files?.length || !api.postForm) return;
    setSubmitting(true);
    try {
      for (const file of Array.from(files)) {
        const form = new FormData();
        form.append("file", file); form.append("business_type", "amusement_session");
        form.append("business_id", ending.id); form.append("attachment_role", "other");
        const saved = await api.postForm<{id: string}>("/api/v1/evidence/attachments/upload", form);
        setEndEvidence(previous => [...previous, {id: saved.id, name: file.name}]);
      }
    } catch (e) { setFormError(e instanceof Error ? e.message : "清场凭据上传失败"); }
    finally { setSubmitting(false); }
  }

  async function endSession() {
    if (!ending || !clearanceConfirmed || !clearedAt || !endReason.trim() || submitting) return;
    setSubmitting(true); setFormError("");
    try {
      const result = await api.post(`/api/v1/amusement/sessions/${encodeURIComponent(ending.id)}/end`, {
        idempotencyKey: endKey, clearanceConfirmed, clearedAt: new Date(clearedAt).toISOString(),
        reason: endReason.trim(), evidenceAttachmentIds: endEvidence.map(f => f.id),
      });
      setActionResult({action: "amusement_session_ended", result});
      setEnding(null); await reload();
    } catch (e) { setFormError(e instanceof Error ? e.message : "场次结束未完成"); }
    finally { setSubmitting(false); }
  }

  const totalCapacity = projects.reduce((sum, row) => sum + Number(row.capacity ?? 0), 0);
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
        { label: "开始", value: cnDate(row.startTime ?? row.startsAt) },
        { label: "结束", value: cnDate(row.endTime ?? row.endsAt) },

        { label: "状态", value: sessionStatusLabel(row.status) },
        { label: "场次编号", value: row.id },
        { label: "清场及后续处理", value: <SessionEndingHistory api={api} sessionId={row.id} /> },
      ],
    });
  }

  const projectsSection = (
    <>
      <div className="admin-section-title">项目</div>
      <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-projects-table"><thead><tr><th>项目</th><th>编码</th><th>容量</th><th>状态</th><th>详情</th></tr></thead><tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`amusement-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.projectName)}</td><td>{display(row.projectCode)}</td><td>{display(row.capacity)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody></table></div>
    </>
  );

  // V341（R-12）：项目权益绑定区——默认只列生效中；「查看已归档」勾选后带出已存档/已删除行并给「恢复」入口。
  const entitlementsSection = (
    <>
      <div className="admin-section-title">{TEXT.entitlementTitle}</div>
      <div className="admin-generic-evidence" data-testid="amusement-entitlements-hint">{TEXT.entitlementHint}</div>
      <div className="admin-form-row">
        <label>所属项目
          <select
            data-testid="amusement-entitlement-project"
            aria-label="选择项目"
            value={entitlementProjectId}
            onChange={(event) => setEntitlementProjectId(event.target.value)}
          >
            {projects.map((row) => <option key={row.id} value={row.id}>{display(row.projectName)}（{row.id}）</option>)}
          </select>
        </label>
        <label>
          <input
            type="checkbox"
            data-testid="amusement-entitlement-show-archived"
            checked={showArchivedEntitlements}
            onChange={(event) => setShowArchivedEntitlements(event.target.checked)}
          />
          {TEXT.entitlementShowArchived}
        </label>
      </div>
      {entitlementNotice ? (
        <div className="admin-state" data-testid="amusement-entitlements-degraded">{entitlementNotice}</div>
      ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="amusement-entitlements-table">
            <thead><tr><th>权益类型</th><th>状态</th><th>存档理由</th><th>删除理由</th><th>操作</th></tr></thead>
            <tbody>
              {entitlements.length === 0 ? (
                <tr data-testid="amusement-entitlements-empty-row"><td colSpan={5} className="admin-empty-cell">{TEXT.entitlementEmpty}</td></tr>
              ) : entitlements.map((row) => (
                <tr key={row.id ?? row.entitlementType} data-testid={`amusement-entitlement-row-${row.entitlementType}`}>
                  <td>{display(row.entitlementType)}</td>
                  <td>{entitlementStatusLabel(row)}</td>
                  <td>{display(row.archiveReason)}</td>
                  <td>{display(row.deleteReason)}</td>
                  <td>{(() => {
                    const acts: RowAction[] = [];
                    if (row.deleteStatus === "pending") {
                      acts.push({ key: "approve", label: "批准删除", permissions: ENTITLEMENT_APPROVE, onClick: () => void decideDeleteEntitlement(row.entitlementType, "approved"), testId: `amusement-entitlement-delete-approve-${row.entitlementType}` });
                      acts.push({ key: "reject", label: "驳回", permissions: ENTITLEMENT_APPROVE, danger: true, onClick: () => void decideDeleteEntitlement(row.entitlementType, "rejected"), testId: `amusement-entitlement-delete-reject-${row.entitlementType}` });
                    } else if (row.deleteStatus === "approved") {
                      acts.push({ key: "restore", label: "恢复", permissions: ENTITLEMENT_APPROVE, onClick: () => void restoreEntitlement(row.entitlementType), testId: `amusement-entitlement-restore-${row.entitlementType}` });
                    } else if (row.archivedAt) {
                      acts.push({ key: "restore", label: "恢复", permissions: ENTITLEMENT_MANAGE, onClick: () => void restoreEntitlement(row.entitlementType), testId: `amusement-entitlement-restore-${row.entitlementType}` });
                      acts.push({ key: "delete-request", label: "申请删除", permissions: ENTITLEMENT_MANAGE, danger: true, onClick: () => void requestDeleteEntitlement(row.entitlementType), testId: `amusement-entitlement-delete-request-${row.entitlementType}` });
                    } else {
                      acts.push({ key: "archive", label: "停用（存档）", permissions: ENTITLEMENT_MANAGE, onClick: () => void archiveEntitlement(row.entitlementType), testId: `amusement-entitlement-archive-${row.entitlementType}` });
                    }
                    return <RowActions actions={acts} permissionsState={effectivePermissions} testId={`amusement-entitlement-actions-${row.entitlementType}`} />;
                  })()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
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
      <div className="admin-table-scroll"><table className="admin-table" data-testid="amusement-sessions-table"><thead><tr><th>场次</th><th>项目</th><th>开始</th><th>结束</th><th>状态</th><th>详情</th></tr></thead><tbody>{pagedSessions.pageRows.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`amusement-session-row-${row.id}`} onClick={() => openSessionDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openSessionDetail(row); } }}><td>{display(row.sessionName)}</td><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{display(row.projectId)}</TraceLink></td><td>{cnDate(row.startTime ?? row.startsAt)}</td><td>{cnDate(row.endTime ?? row.endsAt)}</td><td>{sessionStatusLabel(row.status)}</td><td className="admin-row-detail-cell" onClick={event => event.stopPropagation()}><button type="button" className="admin-row-detail-btn" aria-label="查看场次详情" onClick={() => openSessionDetail(row)}>查看详情</button>{row.status === "open" && <PermissionButton permissionsState={effectivePermissions} permissions={["amusement.session.end"]} data-testid={`amusement-session-end-${row.id}`} onClick={() => { setEnding(row); setClearanceConfirmed(false); setClearedAt(""); setEndReason(""); setEndEvidence([]); setEndKey(crypto.randomUUID()); setFormError(""); }}>确认清场并提前结束</PermissionButton>}</td></tr>)}</tbody></table><TablePager testid="amusement-sessions-pager" page={pagedSessions.page} pageCount={pagedSessions.pageCount} total={pagedSessions.total} onPrev={pagedSessions.prev} onNext={pagedSessions.next} /></div>
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
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>项目硬安全容量</span><button type="button" className="admin-kpi-value" data-testid="amusement-sessions-kpi-capacity" onClick={() => enterKpiFocus("projects")}>{capacityDisplay}</button><small>按唯一项目合计{capacityKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="amusement.session_capacity" testid="amusement-sessions-metric-detail-capacity" /></> : null}</small></div>
            </div>
            <div className="admin-form-row"><PermissionButton permissionsState={effectivePermissions} permissions={SESSION_MANAGE} data-testid="amusement-project-create-open" disabled={projectSubmitting} onClick={() => { setFormError(""); setProjectOpen(true); }}>＋ 新建项目</PermissionButton></div>
            <div className="admin-form-row"><PermissionButton permissionsState={effectivePermissions} permissions={SESSION_MANAGE} data-testid="amusement-session-create-open" disabled={submitting} onClick={() => { setFormError(""); setSessionOpen(true); }}>＋ 新增场次</PermissionButton><span className="admin-muted-action" data-testid="amusement-sessions-boundary">{TEXT.boundary}</span></div>
            {projectsSection}
            {canManageEntitlements ? entitlementsSection : null}
            {sessionsSection}
            {verificationsSection}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      {ending && <section role="dialog" aria-modal="true" aria-label="确认实际清场" className="admin-panel">
        <h3>提前结束：{ending.sessionName ?? ending.id}</h3>
        <p>只处理此场次。尚未履行的订单和权益须继续按原流程办理。</p>
        <label>实际清场时刻<input type="datetime-local" value={clearedAt} onChange={e => setClearedAt(e.target.value)} /></label>
        <label>提前结束原因<input value={endReason} onChange={e => setEndReason(e.target.value)} /></label>
        <label>清场凭据<input type="file" multiple disabled={submitting} onChange={e => void uploadEndEvidence(e.target.files)} /></label>
        <ul>{endEvidence.map(f => <li key={f.id}>{f.name}</li>)}</ul>
        <label><input type="checkbox" checked={clearanceConfirmed} onChange={e => setClearanceConfirmed(e.target.checked)} />确认现场全部客人已离场</label>
        {formError && <p role="alert">{formError}</p>}
        <button type="button" disabled={submitting} onClick={() => setEnding(null)}>取消</button>
        <PermissionButton permissionsState={effectivePermissions} permissions={["amusement.session.end"]} data-testid="amusement-session-end-submit" disabled={submitting || !clearanceConfirmed || !clearedAt || !endReason.trim()} onClick={() => void endSession()}>按实际清场事实结束场次</PermissionButton>
      </section>}
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
                <label>项目硬安全容量<input data-testid="amusement-project-capacity" value={projectCapacity} onChange={(event) => setProjectCapacity(event.target.value)} /></label>
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
            description: "选择项目并填写场次时间。容量统一由项目实时在玩人数控制。",
            validate: () => (!sessionName.trim() ? TEXT.nameRequired : null),
            content: (
              <>
                <label>场次名称<input data-testid="amusement-session-name" value={sessionName} onChange={(event) => setSessionName(event.target.value)} /></label>
                <label>游乐项目<select data-testid="amusement-session-project" value={sessionProjectId} onChange={e => setSessionProjectId(e.target.value)}><option value="">请选择</option>{projects.map(p => <option key={p.id} value={p.id}>{p.projectName ?? p.id}</option>)}</select></label>
                <label>开始时间<input data-testid="amusement-session-start" type="datetime-local" value={startsAt} onChange={e => setStartsAt(e.target.value)} /></label>
                <label>结束时间<input data-testid="amusement-session-end" type="datetime-local" value={endsAt} onChange={e => setEndsAt(e.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "场次名称", value: sessionName.trim() || "未填写" },
          { label: "项目", value: projects.find(p => p.id === sessionProjectId)?.projectName ?? "未选择" },
          { label: "开始时间", value: startsAt || "未填写" },
          { label: "结束时间", value: endsAt || "未填写" },
          { label: "归属项目", value: projects[0]?.projectName ?? projects[0]?.id ?? "amuse-1" },
        ]}
      />
    </section>
  );
}
