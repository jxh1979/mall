import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, AmusementSafetyInspectionRow } from "../types";
import { PermissionButton } from "../security/Can";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";

const TEXT = {
  title: "安全巡检/事故",
  description: "覆盖游乐项目开放前安全巡检、提交、整改、复检和重开审批；事故工单深度闭环需由后续正式事故接口页承载。",
  evidence: "接口证据：读取/创建 /api/v1/amusement/safety-inspections；提交 POST /api/v1/amusement/safety-inspections/{id}/submit；整改 POST /api/v1/amusement/safety-inspections/{id}/rectify；复检 POST /api/v1/amusement/safety-inspections/{id}/recheck；审批重开 POST /api/v1/amusement/safety-inspections/{id}/approve-reopen。",
  loadFailed: "安全巡检数据读取失败",
  actionFailed: "安全巡检操作失败",
  noInspection: "当前没有可操作巡检。",
  createTitleRequired: "创建巡检必须填写检查项说明。",
  rectifyNoteRequired: "整改必须填写说明和证据。",
  total: "巡检总数",
  pendingRectification: "待整改",
  recheckPending: "待复检/审批",
  selected: "选中巡检",
  boundary: "事故工单深度闭环当前不在 ADM-AMU-003 正式接口内，本页不伪造事故关闭；高风险整改必须保留附件证据和重开审批留痕。",
  createTitle: "检查项说明",
  rectifyNote: "整改说明",
  create: "创建巡检",
  submit: "提交巡检",
  rectify: "提交整改",
  recheck: "提交复检",
  approveReopen: "审批重开",
  select: "选择",
  inspectionNo: "巡检号",
  project: "项目",
  overall: "整体结果",
  inspectionStatus: "巡检状态",
  rectificationStatus: "整改状态",
  reopenStatus: "重开状态",
  evidenceLabel: "附件证据",
  time: "时间",
  focusClear: "清除筛选，返回全部",
};

// 整体结果口径（本页专有：巡检整体判定，非全局状态码，页内映射）。
const OVERALL_RESULT_LABELS: Record<string, string> = {
  pass: "通过",
  fail: "不通过",
  pending_rectification: "待整改",
};
function overallLabel(row: AmusementSafetyInspectionRow): string {
  const code = row.overallResult ?? row.riskLevel;
  if (!code) return "-";
  return OVERALL_RESULT_LABELS[code] ?? formatBusinessStatus(code);
}

// 整改状态 pending_rectification=待整改 为本页域内码（全局表未含），页内映射后再交全局表兜底。
function rectificationLabel(code: string | null | undefined): string {
  if (!code) return "-";
  return code === "pending_rectification" ? "待整改" : formatBusinessStatus(code);
}

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function evidenceList(row: AmusementSafetyInspectionRow) {
  return (row.evidenceAttachmentIds ?? row.attachmentIds ?? []).join(", ") || "-";
}

function displayNo(row: AmusementSafetyInspectionRow) {
  return row.inspectionNo ?? row.id;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, evidenceAttachmentIds: request.evidenceAttachmentIds ?? request.attachmentIds, result };
}

// 待整改子集判定（与 KPI 计数同口径，聚焦时复用同一 predicate）。
function isPendingRectification(row: AmusementSafetyInspectionRow) {
  return ["pending", "rectification_pending", "pending_rectification"].includes(row.rectificationStatus ?? row.overallResult ?? "");
}
// 待复检/审批子集判定。
function isRecheckPending(row: AmusementSafetyInspectionRow) {
  return ["rectification_submitted", "rechecked"].includes(row.rectificationStatus ?? "") || row.reopenStatus === "pending";
}

// KPI 聚焦（照样板，单表页）：?kpi=inspections[&subset=pending_rectification|recheck_pending]。
// 巡检总数卡→全部，待整改/待复检卡→对应子集客户端过滤。选中巡检卡无计数、不可点。
type KpiSubset = "pending_rectification" | "recheck_pending";
type KpiFocus = { subset: KpiSubset | null };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "inspections") return null;
  const subset = params.get("subset");
  return { subset: subset === "pending_rectification" || subset === "recheck_pending" ? subset : null };
}

export function AmusementSafetyPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<AmusementSafetyInspectionRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [createTitle, setCreateTitle] = React.useState("开放前护栏检查");
  const [rectifyNote, setRectifyNote] = React.useState("已更换防护垫并上传照片");
  // 行明细抽屉：整行可点 → 该巡检完整字段（结果/状态中文·时间中文·引用可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: KpiSubset | null = null) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "inspections");
    if (subset) params.set("subset", subset);
    else params.delete("subset");
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ subset });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    params.delete("subset");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<AmusementSafetyInspectionRow[]>("/api/v1/amusement/safety-inspections"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const pendingRectification = rows.filter(isPendingRectification).length;
  const recheckPending = rows.filter(isRecheckPending).length;

  const visibleRows = kpiFocus
    ? kpiFocus.subset === "pending_rectification"
      ? rows.filter(isPendingRectification)
      : kpiFocus.subset === "recheck_pending"
        ? rows.filter(isRecheckPending)
        : rows
    : rows;
  const pagedRows = usePagedRows(visibleRows, 10, `${kpiFocus?.subset ?? ""}`);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function createInspection() {
    if (!createTitle.trim()) {
      setFormError(TEXT.createTitleRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("amusement-safety-create"),
      operatorNote: "完整后台创建开放前安全巡检",
      evidenceAttachmentIds: ["att-amusement-safety-create"],
      projectId: "amuse-1",
      templateId: "safety-template-1",
      overallResult: "pending_rectification",
      inspectedAt: "2026-06-16T10:00:00.000Z",
      items: [{ itemCode: "guardrail", itemName: createTitle, result: "fail", severity: "high" }],
    };
    await runAction("amusement_safety_inspection_created", request, () => api.post("/api/v1/amusement/safety-inspections", request));
  }

  async function submitInspection() {
    if (!selected) {
      setFormError(TEXT.noInspection);
      return;
    }
    const request = {
      idempotencyKey: nextKey("amusement-safety-submit"),
      operatorNote: "完整后台提交巡检",
      attachmentIds: ["att-amusement-safety-submit"],
      actionResult: "submitted",
      comment: "发现高风险项，进入整改",
    };
    await runAction("amusement_safety_inspection_submitted", request, () => api.post(`/api/v1/amusement/safety-inspections/${selected.id}/submit`, request));
  }

  async function rectifyInspection() {
    if (!selected) {
      setFormError(TEXT.noInspection);
      return;
    }
    if (!rectifyNote.trim()) {
      setFormError(TEXT.rectifyNoteRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("amusement-safety-rectify"),
      operatorNote: rectifyNote,
      attachmentIds: ["att-amusement-safety-rectify"],
      actionResult: "rectified",
      comment: rectifyNote,
    };
    await runAction("amusement_safety_rectified", request, () => api.post(`/api/v1/amusement/safety-inspections/${selected.id}/rectify`, request));
  }

  async function recheckInspection() {
    if (!selected) {
      setFormError(TEXT.noInspection);
      return;
    }
    const request = {
      idempotencyKey: nextKey("amusement-safety-recheck"),
      operatorNote: "完整后台复检通过",
      attachmentIds: ["att-amusement-safety-recheck"],
      actionResult: "pass",
      comment: "复检通过，提交重开审批",
    };
    await runAction("amusement_safety_rechecked", request, () => api.post(`/api/v1/amusement/safety-inspections/${selected.id}/recheck`, request));
  }

  async function approveReopen() {
    if (!selected) {
      setFormError(TEXT.noInspection);
      return;
    }
    const request = {
      idempotencyKey: nextKey("amusement-safety-approve-reopen"),
      action: "approve",
      reason: "整改复检通过，同意恢复开放",
      evidenceAttachmentIds: ["att-amusement-safety-approve"],
    };
    await runAction("amusement_safety_reopen_approved", request, () => api.post(`/api/v1/amusement/safety-inspections/${selected.id}/approve-reopen`, request));
  }

  function kpiFocusTitle(): string {
    if (kpiFocus?.subset === "pending_rectification") return TEXT.pendingRectification;
    if (kpiFocus?.subset === "recheck_pending") return TEXT.recheckPending;
    return TEXT.total;
  }

  function openInspectionDetail(row: AmusementSafetyInspectionRow) {
    setDetail({
      title: `安全巡检明细 · ${displayNo(row)}`,
      fields: [
        { label: TEXT.inspectionNo, value: displayNo(row) },
        {
          label: TEXT.project,
          value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectName ?? row.projectId ?? "-"}</TraceLink>,
        },
        { label: TEXT.overall, value: overallLabel(row) },
        { label: TEXT.inspectionStatus, value: formatBusinessStatus(row.inspectionStatus ?? row.status) },
        { label: TEXT.rectificationStatus, value: rectificationLabel(row.rectificationStatus) },
        { label: TEXT.reopenStatus, value: row.reopenStatus ? formatBusinessStatus(row.reopenStatus) : "-" },
        { label: TEXT.evidenceLabel, value: evidenceList(row) },
        { label: TEXT.time, value: cnDate(row.inspectedAt ?? row.createdAt) },
      ],
    });
  }

  const inspectionsTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="amusement-safety-table">
        <thead><tr><th>{TEXT.select}</th><th>{TEXT.inspectionNo}</th><th>{TEXT.project}</th><th>{TEXT.overall}</th><th>{TEXT.inspectionStatus}</th><th>{TEXT.rectificationStatus}</th><th>{TEXT.reopenStatus}</th><th>{TEXT.evidenceLabel}</th><th>{TEXT.time}</th></tr></thead>
        <tbody>
          {pagedRows.pageRows.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`amusement-safety-row-${row.id}`}
              onClick={() => openInspectionDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openInspectionDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" data-testid={`amusement-safety-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
              <td>{displayNo(row)}</td>
              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-001" objectId={row.projectId}>{row.projectName ?? row.projectId ?? "-"}</TraceLink></td>
              <td>{overallLabel(row)}</td>
              <td>{formatBusinessStatus(row.inspectionStatus ?? row.status)}</td>
              <td>{rectificationLabel(row.rectificationStatus)}</td>
              <td>{row.reopenStatus ? formatBusinessStatus(row.reopenStatus) : "-"}</td>
              <td>{evidenceList(row)}</td>
              <td>{cnDate(row.inspectedAt ?? row.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <TablePager testid="amusement-safety-pager" page={pagedRows.page} pageCount={pagedRows.pageCount} total={pagedRows.total} onPrev={pagedRows.prev} onNext={pagedRows.next} />
    </div>
  );

  return (
    <section data-testid="amusement-safety-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="amusement-safety-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="amusement.safety_inspection.viewed / created / submitted / rectification_submitted / rechecked / reopened" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="amusement-safety-kpi-focus-banner">
              正在查看：{kpiFocusTitle()}（{visibleRows.length}）
              <button type="button" data-testid="amusement-safety-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
            </div>
            {inspectionsTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="amusement-safety-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.total}</span><button type="button" className="admin-kpi-value" data-testid="amusement-safety-kpi-total" onClick={() => enterKpiFocus()}>{rows.length}</button><small>巡检数</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.pendingRectification}</span><button type="button" className="admin-kpi-value" data-testid="amusement-safety-kpi-pending-rectification" onClick={() => enterKpiFocus("pending_rectification")}>{pendingRectification}</button><small>待整改数</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.recheckPending}</span><button type="button" className="admin-kpi-value" data-testid="amusement-safety-kpi-recheck-pending" onClick={() => enterKpiFocus("recheck_pending")}>{recheckPending}</button><small>待复检审批数</small></div>
              <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong className="admin-kpi-value--text" title={selected ? displayNo(selected) : "-"}>{selected ? displayNo(selected) : "-"}</strong><small>{selected?.rectificationStatus ? rectificationLabel(selected.rectificationStatus) : formatBusinessStatus(selected?.status)}</small></div>
            </div>

            <div className="admin-form-grid" data-testid="amusement-safety-create-form">
              <label>{TEXT.createTitle}<input data-testid="amusement-safety-create-title" value={createTitle} onChange={(event) => setCreateTitle(event.target.value)} /></label>
              <label>{TEXT.rectifyNote}<input data-testid="amusement-safety-rectify-note" value={rectifyNote} onChange={(event) => setRectifyNote(event.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="amusement-safety-actions">
              <PermissionButton permissionsState={effectivePermissions} permissions={["amusement.safety_inspection.create"]} data-testid="amusement-safety-create" disabled={busyAction !== ""} onClick={() => void createInspection()}>{TEXT.create}</PermissionButton>
              <PermissionButton permissionsState={effectivePermissions} permissions={["amusement.safety_inspection.submit"]} data-testid="amusement-safety-submit" disabled={busyAction !== ""} onClick={() => void submitInspection()}>{TEXT.submit}</PermissionButton>
              <PermissionButton permissionsState={effectivePermissions} permissions={["amusement.safety_inspection.rectify"]} data-testid="amusement-safety-rectify" disabled={busyAction !== ""} onClick={() => void rectifyInspection()}>{TEXT.rectify}</PermissionButton>
              <PermissionButton permissionsState={effectivePermissions} permissions={["amusement.safety_inspection.recheck"]} data-testid="amusement-safety-recheck" disabled={busyAction !== ""} onClick={() => void recheckInspection()}>{TEXT.recheck}</PermissionButton>
              <PermissionButton permissionsState={effectivePermissions} permissions={["amusement.safety_inspection.approve_reopen"]} data-testid="amusement-safety-approve-reopen" disabled={busyAction !== ""} onClick={() => void approveReopen()}>{TEXT.approveReopen}</PermissionButton>
            </div>
            <div className="admin-muted-action" data-testid="amusement-safety-boundary">{TEXT.boundary}</div>
            {formError ? <div data-testid="amusement-safety-error" className="admin-inline-error">{formError}</div> : null}

            {inspectionsTable}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
