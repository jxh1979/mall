import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

type VendorWorkOrderRow = {
  id: string;
  code?: string;
  workOrderType?: string;
  priority?: string;
  status?: string;
  title?: string;
  spaceId?: string | null;
  equipmentId?: string | null;
  assigneeId?: string | null;
  handleDueAt?: string | null;
  resolution?: string | null;
  projectId?: string | null;
};

type VendorEquipmentRow = {
  id: string;
  name?: string;
  equipmentType?: string;
  status?: string;
  spaceId?: string | null;
  projectId?: string | null;
  brand?: string | null;
  responsibleUserId?: string | null;
  nextMaintenanceDue?: string | null;
};

const PROCESS = ["facility.work_order.process_assigned"];

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

export function VendorWorkOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<VendorWorkOrderRow[]>([]);
  const [equipmentRows, setEquipmentRows] = React.useState<VendorEquipmentRow[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [note, setNote] = React.useState("外包维保现场处理");
  const [resolution, setResolution] = React.useState("已完成维保并上传现场记录");

  const selected = rows.find((row) => row.id === selectedId) ?? rows[0] ?? null;
  const visibleEquipmentRows = equipmentRows.filter((row) => (
    (selected?.equipmentId && row.id === selected.equipmentId)
    || (selected?.projectId && row.projectId === selected.projectId)
  ));

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [workOrders, equipment] = await Promise.all([
        api.get<VendorWorkOrderRow[]>("/api/v1/facility/work-orders"),
        api.get<VendorEquipmentRow[]>("/api/v1/facility/equipment"),
      ]);
      setRows(workOrders ?? []);
      setEquipmentRows(equipment ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "外包维保工单读取失败", onStart: () => setGlobalError(null) },
  );

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: "外包维保工单操作失败",
  });

  async function runWorkOrderAction(action: "start" | "complete") {
    if (!selected) {
      setFormError("请选择一条工单");
      return;
    }
    const request = {
      idempotencyKey: nextKey(`vendor-work-order-${action}`),
      note,
      resolution: action === "complete" ? resolution : undefined,
    };
    await runAction(`vendor_work_order_${action}`, request, () => api.post(`/api/v1/facility/work-orders/${encodeURIComponent(selected.id)}/${action}`, request));
  }

  const pagedRows = usePagedRows(rows, 10);

  return (
    <section data-testid="vendor-work-orders-page">
      <AdminPageHeader title="外包维保工单" description="维保单位只能查看和处理派给自己的楼宇工单；验收已收回物业侧，外包侧仅保留开始与完成。" />
      <TechDetails><div className="admin-generic-evidence" data-testid="vendor-work-orders-evidence">接口证据：GET /api/v1/facility/work-orders；GET /api/v1/facility/equipment；POST /api/v1/facility/work-orders/{"{work_order_id}"}/start|complete。</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-kpi-grid" data-testid="vendor-work-orders-summary">
          <div className="admin-kpi-card"><span>已指派工单</span><strong>{rows.length}</strong><small>assignee</small></div>
          <div className="admin-kpi-card"><span>待处理</span><strong>{rows.filter((row) => !["completed", "closed", "accepted"].includes(row.status ?? "")).length}</strong><small>status</small></div>
          <div className="admin-kpi-card"><span>当前工单</span><strong>{selected?.code ?? selected?.id ?? "-"}</strong><small>{selected?.status ?? "-"}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="vendor-work-order-action-form">
          <label>处理备注<input data-testid="vendor-work-order-note" value={note} onChange={(event) => setNote(event.target.value)} /></label>
          <label>完成说明<input data-testid="vendor-work-order-resolution" value={resolution} onChange={(event) => setResolution(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="vendor-work-order-actions">
          <PermissionButton permissionsState={effectivePermissions} permissions={PROCESS} data-testid="vendor-work-order-start" disabled={busyAction !== ""} onClick={() => void runWorkOrderAction("start")}>开始</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={PROCESS} data-testid="vendor-work-order-complete" disabled={busyAction !== ""} onClick={() => void runWorkOrderAction("complete")}>完成</PermissionButton>
        </div>
        {formError ? <div data-testid="vendor-work-order-error" className="admin-inline-error">{formError}</div> : null}

        <h3>关联设备信息（只读）</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-work-order-equipment-table">
            <thead><tr><th>设备</th><th>类型</th><th>状态</th><th>项目/空间</th><th>品牌/责任人</th><th>下次维保</th></tr></thead>
            <tbody>
              {(visibleEquipmentRows.length > 0 ? visibleEquipmentRows : equipmentRows).map((row) => (
                <tr key={row.id}>
                  <td>{row.name ?? row.id}</td>
                  <td>{row.equipmentType ?? "-"}</td>
                  <td>{row.status ?? "-"}</td>
                  <td>{row.projectId ?? "-"} / {row.spaceId ?? "-"}</td>
                  <td>{row.brand ?? "-"} / {row.responsibleUserId ?? "-"}</td>
                  <td>{row.nextMaintenanceDue ?? "-"}</td>
                </tr>
              ))}
              {equipmentRows.length === 0 ? <tr><td colSpan={6}>暂无关联设备</td></tr> : null}
            </tbody>
          </table>
        </div>

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="vendor-work-orders-table">
            <thead><tr><th>选择</th><th>工单号</th><th>标题</th><th>类型</th><th>优先级</th><th>状态</th><th>空间/设备</th><th>处理人</th><th>处理时限</th></tr></thead>
            <tbody>
              {pagedRows.pageRows.map((row) => (
                <tr key={row.id}>
                  <td><input type="radio" data-testid={`vendor-work-order-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.code ?? row.id}</td>
                  <td>{row.title ?? "-"}</td>
                  <td>{row.workOrderType ?? "-"}</td>
                  <td>{row.priority ?? "-"}</td>
                  <td>{row.status ?? "-"}</td>
                  <td>{row.spaceId ?? "-"} / {row.equipmentId ?? "-"}</td>
                  <td>{row.assigneeId ?? "-"}</td>
                  <td>{row.handleDueAt ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="vendor-work-orders-pager" page={pagedRows.page} pageCount={pagedRows.pageCount} total={pagedRows.total} onPrev={pagedRows.prev} onNext={pagedRows.next} />
        </div>
      </AdminDataState>
    </section>
  );
}
