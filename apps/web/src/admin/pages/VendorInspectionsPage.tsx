import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

type VendorInspectionTaskRow = {
  id: string;
  templateId?: string | null;
  inspectionType?: string;
  status?: string;
  title?: string;
  scheduleMode?: string;
  assigneeId?: string | null;
  scheduledDate?: string | null;
  dueDate?: string | null;
  spaceId?: string | null;
  workOrderId?: string | null;
};

type VendorInspectionTemplateRow = {
  id: string;
  name?: string;
  inspectionType?: string;
  cycleDays?: number | null;
  abnormalMode?: string;
  active?: boolean;
  checklist?: Array<string | { label?: string; item?: string }>;
  scopeNote?: string | null;
};

const RECORD = ["facility.inspection.record_assigned"];
const REPORT_EXCEPTION = ["facility.inspection.report_exception_assigned"];

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

function checklistText(checklist?: VendorInspectionTemplateRow["checklist"]) {
  if (!Array.isArray(checklist) || checklist.length === 0) return "-";
  return checklist.map((item) => (typeof item === "string" ? item : item.label ?? item.item ?? "-")).join("、");
}

export function VendorInspectionsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<VendorInspectionTaskRow[]>([]);
  const [templates, setTemplates] = React.useState<VendorInspectionTemplateRow[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [result, setResult] = React.useState("normal");
  const [note, setNote] = React.useState("现场巡检记录正常");
  const [abnormalNote, setAbnormalNote] = React.useState("");
  const [exceptionNote, setExceptionNote] = React.useState("巡检发现异常，待物业跟进");
  const [inspectorId, setInspectorId] = React.useState("vendor-user");

  const selected = rows.find((row) => row.id === selectedId) ?? rows[0] ?? null;
  const visibleTemplates = templates.filter((row) => row.id === selected?.templateId);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [tasks, templateRows] = await Promise.all([
        api.get<VendorInspectionTaskRow[]>("/api/v1/facility/inspections/tasks"),
        api.get<VendorInspectionTemplateRow[]>("/api/v1/facility/inspections/templates"),
      ]);
      setRows(tasks ?? []);
      setTemplates(templateRows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "外包巡检任务读取失败", onStart: () => setGlobalError(null) },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: "外包巡检任务操作失败",
  });

  async function startTask() {
    if (!selected) {
      setFormError("请选择一条巡检任务");
      return;
    }
    const request = { idempotencyKey: nextKey("vendor-inspection-start"), note };
    await runAction("vendor_inspection_started", request, () => api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(selected.id)}/start`, request));
  }

  async function submitRecord() {
    if (!selected) {
      setFormError("请选择一条巡检任务");
      return;
    }
    if (!inspectorId.trim()) {
      setFormError("巡检人不能为空");
      return;
    }
    const request = {
      idempotencyKey: nextKey("vendor-inspection-record"),
      result,
      note,
      abnormalNote: result === "abnormal" ? abnormalNote : undefined,
      inspectorId: inspectorId.trim(),
      photos: ["att-vendor-inspection"],
      checklistResult: [],
    };
    await runAction("vendor_inspection_recorded", request, () => api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(selected.id)}/records`, request));
  }

  async function reportException() {
    if (!selected) {
      setFormError("请选择一条巡检任务");
      return;
    }
    if (!inspectorId.trim() || !exceptionNote.trim()) {
      setFormError("巡检人和异常说明不能为空");
      return;
    }
    const request = {
      idempotencyKey: nextKey("vendor-inspection-exception"),
      photos: ["att-vendor-inspection-exception"],
      qrCode: "QR-EXCEPTION",
      gpsLat: null,
      gpsLng: null,
      checklistResult: [{ item: selected.title ?? selected.id, result: "abnormal" }],
      note,
      abnormalNote: exceptionNote.trim(),
      inspectorId: inspectorId.trim(),
    };
    await runAction("vendor_inspection_exception_reported", request, () => api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(selected.id)}/report-exception`, request));
  }

  return (
    <section data-testid="vendor-inspections-page">
      <AdminPageHeader title="外包巡检任务" description="安保/保洁外包人员只能查看派给自己的巡检任务、模板要求，并提交现场开始、记录与异常上报。" />
      <TechDetails><div className="admin-generic-evidence" data-testid="vendor-inspections-evidence">接口证据：GET /api/v1/facility/inspections/tasks；GET /api/v1/facility/inspections/templates；POST /api/v1/facility/inspections/tasks/{"{task_id}"}/start；POST /records；POST /report-exception。</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="vendor-inspections-summary">
          <div className="admin-kpi-card"><span>已指派任务</span><strong>{rows.length}</strong><small>assignee</small></div>
          <div className="admin-kpi-card"><span>待记录</span><strong>{rows.filter((row) => !["completed", "closed"].includes(row.status ?? "")).length}</strong><small>status</small></div>
          <div className="admin-kpi-card"><span>当前任务</span><strong>{selected?.title ?? selected?.id ?? "-"}</strong><small>{selected?.dueDate ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="vendor-inspection-record-form">
          <label>巡检人<input data-testid="vendor-inspection-inspector" value={inspectorId} onChange={(event) => setInspectorId(event.target.value)} /></label>
          <label>结果<select data-testid="vendor-inspection-result" value={result} onChange={(event) => setResult(event.target.value)}><option value="normal">正常</option><option value="abnormal">异常</option></select></label>
          <label>记录说明<input data-testid="vendor-inspection-note" value={note} onChange={(event) => setNote(event.target.value)} /></label>
          <label>异常说明<input data-testid="vendor-inspection-abnormal-note" value={abnormalNote} onChange={(event) => setAbnormalNote(event.target.value)} /></label>
          <label>异常上报说明<input data-testid="vendor-inspection-exception-note" value={exceptionNote} onChange={(event) => setExceptionNote(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="vendor-inspection-actions">
          <PermissionButton permissionsState={effectivePermissions} permissions={RECORD} data-testid="vendor-inspection-start" disabled={busyAction !== ""} onClick={() => void startTask()}>开始</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={RECORD} data-testid="vendor-inspection-record" disabled={busyAction !== ""} onClick={() => void submitRecord()}>记录</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={REPORT_EXCEPTION} data-testid="vendor-inspection-report-exception" disabled={busyAction !== ""} onClick={() => void reportException()}>上报异常</PermissionButton>
        </div>
        {formError ? <div data-testid="vendor-inspection-error" className="admin-inline-error">{formError}</div> : null}

        <h3>巡检模板/要求（只读）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-inspection-templates-table">
            <thead><tr><th>模板</th><th>类型</th><th>周期</th><th>异常模式</th><th>检查要求</th><th>状态</th></tr></thead>
            <tbody>
              {(visibleTemplates.length > 0 ? visibleTemplates : templates).map((row) => (
                <tr key={row.id}>
                  <td>{row.name ?? row.id}</td>
                  <td>{formatBusinessStatus(row.inspectionType)}</td>
                  <td>{row.cycleDays ?? "-"}</td>
                  <td>{formatBusinessStatus(row.abnormalMode)}</td>
                  <td>{checklistText(row.checklist)} {row.scopeNote ?? ""}</td>
                  <td>{row.active === false ? "停用" : "启用"}</td>
                </tr>
              ))}
              {templates.length === 0 ? <tr><td colSpan={6}>暂无巡检模板</td></tr> : null}
            </tbody>
          </table>
        </div>

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-inspections-table">
            <thead><tr><th>选择</th><th>任务</th><th>类型</th><th>状态</th><th>排程</th><th>空间</th><th>处理人</th><th>计划/到期</th><th>关联工单</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`vendor-inspection-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.title ?? row.id}</td>
                  <td>{formatBusinessStatus(row.inspectionType)}</td>
                  <td>{formatBusinessStatus(row.status)}</td>
                  <td>{formatBusinessStatus(row.scheduleMode)}</td>
                  <td>{row.spaceId ?? "-"}</td>
                  <td>{row.assigneeId ?? "-"}</td>
                  <td>{row.scheduledDate ?? "-"} / {row.dueDate ?? "-"}</td>
                  <td>{row.workOrderId ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
