import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

type WorkOrderRow = {
  id: string; title: string; category: string; priority: string; status: string;
  assignee?: string; equipmentId?: string; spaceId?: string; slaDueAt?: string;
};

const MANAGE = ["facility.work_order.manage"];
const CATEGORIES = ["repair", "maintenance", "inspection_followup"];
const PRIORITIES = ["low", "medium", "high", "critical"];
const STATUS_TARGETS = ["in_progress", "resolved", "closed"];

function isOverdue(row: WorkOrderRow): boolean {
  return !!row.slaDueAt && !["resolved", "closed"].includes(row.status) && new Date(row.slaDueAt).getTime() < Date.now();
}

/**
 * 楼宇维保工单（ADM-FAC-002，③ 楼宇运营 S2）：报修建单→派单→流转，按 priority 自动算 SLA 到期；超期红点（读时派生）。
 * 功能标志 facility.work_order.enabled 默认关（关时写被拒）。
 */
export function FacilityWorkOrderPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<WorkOrderRow[]>([]);
  const [form, setForm] = React.useState({ title: "", category: "repair", priority: "medium", equipmentId: "", spaceId: "", reportedBy: "" });
  const [assignDraft, setAssignDraft] = React.useState<Record<string, string>>({});

  const { loading, error, reload } = useAdminResource(
    async () => {
      setRows((await api.get<WorkOrderRow[]>("/api/v1/facility/work-orders")) ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "维保工单读取失败", onStart: () => setGlobalError(null) },
  );

  async function createWorkOrder() {
    if (!form.title.trim()) {
      setGlobalError("请填写工单标题");
      return;
    }
    await api.post("/api/v1/facility/work-orders", {
      title: form.title.trim(),
      category: form.category,
      priority: form.priority,
      equipmentId: form.equipmentId.trim() || null,
      spaceId: form.spaceId.trim() || null,
      reportedBy: form.reportedBy.trim() || null,
    });
    setActionResult({ action: "facility_work_order_created", title: form.title.trim() });
    setForm({ ...form, title: "", equipmentId: "", spaceId: "" });
    reload();
  }

  async function assign(row: WorkOrderRow) {
    const assignee = (assignDraft[row.id] ?? "").trim();
    if (!assignee) {
      setGlobalError("请填写受理人");
      return;
    }
    await api.post(`/api/v1/facility/work-orders/${row.id}/assign`, { assignee });
    setActionResult({ action: "facility_work_order_assigned", workOrderId: row.id, assignee });
    reload();
  }

  async function setStatus(row: WorkOrderRow, status: string) {
    await api.post(`/api/v1/facility/work-orders/${row.id}/status`, { status });
    setActionResult({ action: "facility_work_order_status", workOrderId: row.id, status });
    reload();
  }

  return (
    <section data-testid="facility-work-order-page">
      <AdminPageHeader
        title="楼宇维保工单"
        description="楼宇设施报修/维保工单：建单按优先级自动定 SLA（紧急4h/高24h/中72h/低168h），派单→处理→解决→关闭；超期标红。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-work-order-table">
            <thead><tr><th>id</th><th>标题</th><th>类别</th><th>优先级</th><th>状态</th><th>受理人</th><th>SLA到期</th><th>派单</th><th>流转</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} data-testid={`facility-work-order-${row.id}`}>
                  <td>{row.id}</td><td>{row.title}</td><td>{row.category}</td><td>{row.priority}</td>
                  <td>{isOverdue(row) ? <span data-testid={`facility-work-order-overdue-${row.id}`} style={{ color: "#c00" }}>{row.status}·超期</span> : row.status}</td>
                  <td>{row.assignee ?? "-"}</td>
                  <td>{row.slaDueAt ? new Date(row.slaDueAt).toLocaleString() : "-"}</td>
                  <td>
                    <input aria-label={`受理人-${row.id}`} placeholder="受理人 id"
                      value={assignDraft[row.id] ?? ""} onChange={(e) => setAssignDraft({ ...assignDraft, [row.id]: e.target.value })} />
                    <button type="button" data-testid={`facility-work-order-assign-${row.id}`} onClick={() => void assign(row)}>派单</button>
                  </td>
                  <td>
                    <select aria-label={`状态-${row.id}`} data-testid={`facility-work-order-status-${row.id}`} value=""
                      onChange={(e) => { if (e.target.value) void setStatus(row, e.target.value); }}>
                      <option value="">流转到…</option>
                      {STATUS_TARGETS.map((s) => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="facility-work-order-form">
          <input aria-label="工单标题" placeholder="工单标题" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <select aria-label="类别" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
            {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
          <select aria-label="优先级" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
            {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
          <input aria-label="设备 id" placeholder="设备 id（可空）" value={form.equipmentId} onChange={(e) => setForm({ ...form, equipmentId: e.target.value })} />
          <input aria-label="位置空间 id" placeholder="位置空间 id" value={form.spaceId} onChange={(e) => setForm({ ...form, spaceId: e.target.value })} />
          <input aria-label="报修人" placeholder="报修人 id" value={form.reportedBy} onChange={(e) => setForm({ ...form, reportedBy: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE}
            data-testid="facility-work-order-create" onClick={() => void createWorkOrder()}>建单</PermissionButton>
        </div>
      </AdminDataState>
    </section>
  );
}
