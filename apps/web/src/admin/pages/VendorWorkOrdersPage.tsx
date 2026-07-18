import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useAdminAction } from "../hooks/useAdminAction";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
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
const DONE_STATUSES = ["completed", "closed", "accepted"];

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染工单表。
// URL 约定：?kpi=work-orders[&subset=pending]；刷新/直链带参直接进聚焦视图。
type KpiFocus = { subset: "all" | "pending" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "work-orders") return null;
  return { subset: params.get("subset") === "pending" ? "pending" : "all" };
}

export function VendorWorkOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<VendorWorkOrderRow[]>([]);
  const [equipmentRows, setEquipmentRows] = React.useState<VendorEquipmentRow[]>([]);
  const [equipmentNotice, setEquipmentNotice] = React.useState<string | null>(null);
  const [selectedId, setSelectedId] = React.useState("");
  const [note, setNote] = React.useState("外包维保现场处理");
  const [resolution, setResolution] = React.useState("已完成维保并上传现场记录");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该工单/设备完整字段（枚举中文·时间中文·处理人可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: "all" | "pending") {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "work-orders");
    if (subset === "pending") params.set("subset", "pending");
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

  const selected = rows.find((row) => row.id === selectedId) ?? rows[0] ?? null;
  const visibleEquipmentRows = equipmentRows.filter((row) => (
    (selected?.equipmentId && row.id === selected.equipmentId)
    || (selected?.projectId && row.projectId === selected.projectId)
  ));

  const { loading, error, reload } = useAdminResource(
    async () => {
      // AUD-032：设备台账是副资源，外包账号可能无权（403）——降级为区块提示，不拖垮工单主内容。
      const [workOrders, equipment] = await Promise.all([
        api.get<VendorWorkOrderRow[]>("/api/v1/facility/work-orders"),
        loadSecondary(() => api.get<VendorEquipmentRow[]>("/api/v1/facility/equipment"), [] as VendorEquipmentRow[]),
      ]);
      setRows(workOrders ?? []);
      setEquipmentRows(equipment.data);
      setEquipmentNotice(equipment.notice);
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

  const pendingRows = rows.filter((row) => !DONE_STATUSES.includes(row.status ?? ""));
  const focusRows = kpiFocus?.subset === "pending" ? pendingRows : rows;
  const pagedRows = usePagedRows(kpiFocus ? focusRows : rows, 10, kpiFocus?.subset ?? "");

  function openWorkOrderDetail(row: VendorWorkOrderRow) {
    setDetail({
      title: `工单明细 · ${row.code ?? row.id}`,
      fields: [
        { label: "工单号", value: row.code ?? row.id },
        { label: "标题", value: row.title ?? "—" },
        { label: "类型", value: formatBusinessStatus(row.workOrderType) },
        { label: "优先级", value: formatBusinessStatus(row.priority) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "空间", value: row.spaceId ?? "—" },
        { label: "设备", value: row.equipmentId ?? "—" },
        { label: "处理人", value: <TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.assigneeId}>{row.assigneeId ?? "—"}</TraceLink> },
        { label: "处理时限", value: cnDate(row.handleDueAt) },
        { label: "处理结果", value: row.resolution ?? "—" },
        { label: "所属项目", value: row.projectId ?? "—" },
      ],
    });
  }

  function openEquipmentDetail(row: VendorEquipmentRow) {
    setDetail({
      title: `设备明细 · ${row.name ?? row.id}`,
      fields: [
        { label: "设备", value: row.name ?? row.id },
        { label: "类型", value: formatBusinessStatus(row.equipmentType) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "所属项目", value: row.projectId ?? "—" },
        { label: "空间", value: row.spaceId ?? "—" },
        { label: "品牌", value: row.brand ?? "—" },
        { label: "责任人", value: <TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.responsibleUserId}>{row.responsibleUserId ?? "—"}</TraceLink> },
        { label: "下次维保", value: cnDate(row.nextMaintenanceDue) },
        { label: "设备编号", value: row.id },
      ],
    });
  }

  const workOrdersTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="vendor-work-orders-table">
        <thead><tr><th>选择</th><th>工单号</th><th>标题</th><th>类型</th><th>优先级</th><th>状态</th><th>空间/设备</th><th>处理人</th><th>处理时限</th><th>详情</th></tr></thead>
        <tbody>
          {pagedRows.pageRows.map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`vendor-work-order-row-${row.id}`}
              onClick={() => openWorkOrderDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此工单 ${row.id}`} data-testid={`vendor-work-order-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
              <td>{row.code ?? row.id}</td>
              <td>{row.title ?? "-"}</td>
              <td>{formatBusinessStatus(row.workOrderType)}</td>
              <td>{formatBusinessStatus(row.priority)}</td>
              <td>{formatBusinessStatus(row.status)}</td>
              <td>{row.spaceId ?? "-"} / {row.equipmentId ?? "-"}</td>
              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.assigneeId}>{row.assigneeId ?? "-"}</TraceLink></td>
              <td>{cnDate(row.handleDueAt)}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看工单详情" onClick={(event) => { event.stopPropagation(); openWorkOrderDetail(row); }}>查看详情</button></td>
            </tr>
          ))}
        </tbody>
      </table>
      <TablePager testid="vendor-work-orders-pager" page={pagedRows.page} pageCount={pagedRows.pageCount} total={pagedRows.total} onPrev={pagedRows.prev} onNext={pagedRows.next} />
    </div>
  );

  return (
    <section data-testid="vendor-work-orders-page">
      <AdminPageHeader title="外包维保工单" description="维保单位只能查看和处理派给自己的楼宇工单；验收已收回物业侧，外包侧仅保留开始与完成。" />
      <TechDetails><div className="admin-generic-evidence" data-testid="vendor-work-orders-evidence">接口证据：GET /api/v1/facility/work-orders；GET /api/v1/facility/equipment；POST /api/v1/facility/work-orders/{"{work_order_id}"}/start|complete。</div></TechDetails>
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="vendor-work-orders-kpi-focus-banner">
              正在查看：{kpiFocus.subset === "pending" ? "待处理工单" : "已指派工单"}（{focusRows.length}）
              <button type="button" data-testid="vendor-work-orders-kpi-focus-clear" onClick={exitKpiFocus}>清除筛选，返回全部</button>
            </div>
            {workOrdersTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="vendor-work-orders-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>已指派工单</span><button type="button" className="admin-kpi-value" data-testid="vendor-work-orders-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>派给本单位</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>待处理</span><button type="button" className="admin-kpi-value" data-testid="vendor-work-orders-kpi-pending" onClick={() => enterKpiFocus("pending")}>{pendingRows.length}</button><small>未完工</small></div>
              <div className="admin-kpi-card"><span>当前工单</span><strong className="admin-kpi-value--text" title={selected?.code ?? selected?.id ?? "-"}>{selected?.code ?? selected?.id ?? "-"}</strong><small>{formatBusinessStatus(selected?.status)}</small></div>
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
            {equipmentNotice ? (
              <div className="admin-state" data-testid="vendor-work-order-equipment-degraded">{equipmentNotice}</div>
            ) : (
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="vendor-work-order-equipment-table">
                <thead><tr><th>设备</th><th>类型</th><th>状态</th><th>项目/空间</th><th>品牌/责任人</th><th>下次维保</th><th>详情</th></tr></thead>
                <tbody>
                  {(visibleEquipmentRows.length > 0 ? visibleEquipmentRows : equipmentRows).map((row) => (
                    <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`vendor-work-order-equipment-row-${row.id}`}
                      onClick={() => openEquipmentDetail(row)}
                      onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openEquipmentDetail(row); } }}>
                      <td>{row.name ?? row.id}</td>
                      <td>{formatBusinessStatus(row.equipmentType)}</td>
                      <td>{formatBusinessStatus(row.status)}</td>
                      <td>{row.projectId ?? "-"} / {row.spaceId ?? "-"}</td>
                      <td>{row.brand ?? "-"} / {row.responsibleUserId ?? "-"}</td>
                      <td>{cnDate(row.nextMaintenanceDue)}</td>
                      <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看设备详情" onClick={(event) => { event.stopPropagation(); openEquipmentDetail(row); }}>查看详情</button></td>
                    </tr>
                  ))}
                  {equipmentRows.length === 0 ? <tr><td colSpan={7}>暂无关联设备</td></tr> : null}
                </tbody>
              </table>
            </div>
            )}

            {workOrdersTable}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
