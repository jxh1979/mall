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
};

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

export function AmusementSafetyPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<AmusementSafetyInspectionRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [createTitle, setCreateTitle] = React.useState("开放前护栏检查");
  const [rectifyNote, setRectifyNote] = React.useState("已更换防护垫并上传照片");

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows(await api.get<AmusementSafetyInspectionRow[]>("/api/v1/amusement/safety-inspections"));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const pagedRows = usePagedRows(rows, 10);
  const pendingRectification = rows.filter((row) => ["pending", "rectification_pending", "pending_rectification"].includes(row.rectificationStatus ?? row.overallResult ?? "")).length;
  const recheckPending = rows.filter((row) => ["rectification_submitted", "rechecked"].includes(row.rectificationStatus ?? "") || row.reopenStatus === "pending").length;

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

  return (
    <section data-testid="amusement-safety-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="amusement-safety-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="amusement.safety_inspection.viewed / created / submitted / rectification_submitted / rechecked / reopened" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="amusement-safety-summary">
          <div className="admin-kpi-card"><span>{TEXT.total}</span><strong>{rows.length}</strong><small>{TEXT.project}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.pendingRectification}</span><strong>{pendingRectification}</strong><small>{TEXT.rectificationStatus}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.recheckPending}</span><strong>{recheckPending}</strong><small>{TEXT.reopenStatus}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong>{selected ? displayNo(selected) : "-"}</strong><small>{selected?.rectificationStatus ?? selected?.status ?? "-"}</small></div>
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

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="amusement-safety-table">
            <thead><tr><th>{TEXT.select}</th><th>{TEXT.inspectionNo}</th><th>{TEXT.project}</th><th>{TEXT.overall}</th><th>{TEXT.inspectionStatus}</th><th>{TEXT.rectificationStatus}</th><th>{TEXT.reopenStatus}</th><th>{TEXT.evidenceLabel}</th><th>{TEXT.time}</th></tr></thead>
            <tbody>
              {pagedRows.pageRows.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`amusement-safety-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{displayNo(row)}</td>
                  <td>{row.projectName ?? row.projectId ?? "-"}</td>
                  <td>{row.overallResult ?? row.riskLevel ?? "-"}</td>
                  <td>{formatBusinessStatus(row.inspectionStatus ?? row.status)}</td>
                  <td>{row.rectificationStatus ?? "-"}</td>
                  <td>{row.reopenStatus ?? "-"}</td>
                  <td>{evidenceList(row)}</td>
                  <td>{row.inspectedAt ?? row.createdAt ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="amusement-safety-pager" page={pagedRows.page} pageCount={pagedRows.pageCount} total={pagedRows.total} onPrev={pagedRows.prev} onNext={pagedRows.next} />
        </div>
      </AdminDataState>
    </section>
  );
}
