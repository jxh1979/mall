import React from "react";
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

const RECORD = ["facility.inspection.record_assigned"];

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

export function VendorInspectionsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<VendorInspectionTaskRow[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [result, setResult] = React.useState("normal");
  const [note, setNote] = React.useState("现场巡检记录正常");
  const [abnormalNote, setAbnormalNote] = React.useState("");
  const [inspectorId, setInspectorId] = React.useState("vendor-user");

  const selected = rows.find((row) => row.id === selectedId) ?? rows[0] ?? null;

  const { loading, error, reload } = useAdminResource(
    async () => {
      const tasks = await api.get<VendorInspectionTaskRow[]>("/api/v1/facility/inspections/tasks");
      setRows(tasks ?? []);
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

  return (
    <section data-testid="vendor-inspections-page">
      <AdminPageHeader title="外包巡检任务" description="安保/保洁外包人员只能查看派给自己的巡检任务，并提交现场开始与记录。" />
      <div className="admin-generic-evidence" data-testid="vendor-inspections-evidence">接口证据：GET /api/v1/facility/inspections/tasks；POST /api/v1/facility/inspections/tasks/{"{task_id}"}/start；POST /records。</div>
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="vendor-inspections-summary">
          <div className="admin-kpi-card"><span>已指派任务</span><strong>{rows.length}</strong><small>assignee</small></div>
          <div className="admin-kpi-card"><span>待记录</span><strong>{rows.filter((row) => !["completed", "closed"].includes(row.status ?? "")).length}</strong><small>status</small></div>
          <div className="admin-kpi-card"><span>当前任务</span><strong>{selected?.title ?? selected?.id ?? "-"}</strong><small>{selected?.dueDate ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="vendor-inspection-record-form">
          <label>巡检人<input data-testid="vendor-inspection-inspector" value={inspectorId} onChange={(event) => setInspectorId(event.target.value)} /></label>
          <label>结果<select data-testid="vendor-inspection-result" value={result} onChange={(event) => setResult(event.target.value)}><option value="normal">normal</option><option value="abnormal">abnormal</option></select></label>
          <label>记录说明<input data-testid="vendor-inspection-note" value={note} onChange={(event) => setNote(event.target.value)} /></label>
          <label>异常说明<input data-testid="vendor-inspection-abnormal-note" value={abnormalNote} onChange={(event) => setAbnormalNote(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="vendor-inspection-actions">
          <PermissionButton permissionsState={effectivePermissions} permissions={RECORD} data-testid="vendor-inspection-start" disabled={busyAction !== ""} onClick={() => void startTask()}>开始</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={RECORD} data-testid="vendor-inspection-record" disabled={busyAction !== ""} onClick={() => void submitRecord()}>记录</PermissionButton>
        </div>
        {formError ? <div data-testid="vendor-inspection-error" className="admin-inline-error">{formError}</div> : null}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-inspections-table">
            <thead><tr><th>选择</th><th>任务</th><th>类型</th><th>状态</th><th>排程</th><th>空间</th><th>处理人</th><th>计划/到期</th><th>关联工单</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`vendor-inspection-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.title ?? row.id}</td>
                  <td>{row.inspectionType ?? "-"}</td>
                  <td>{row.status ?? "-"}</td>
                  <td>{row.scheduleMode ?? "-"}</td>
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
