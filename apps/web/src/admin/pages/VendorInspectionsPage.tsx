import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminAction } from "../hooks/useAdminAction";
import { loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import { PermissionButton } from "../security/Can";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
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
const DONE_STATUSES = ["completed", "closed"];

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

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带参重进本页，只渲染巡检任务表。
// URL 约定：?kpi=inspection-tasks[&subset=pending]。
type KpiFocus = { subset: "all" | "pending" };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const params = new URLSearchParams(window.location.search);
  if (params.get("kpi") !== "inspection-tasks") return null;
  return { subset: params.get("subset") === "pending" ? "pending" : "all" };
}

export function VendorInspectionsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<VendorInspectionTaskRow[]>([]);
  const [templates, setTemplates] = React.useState<VendorInspectionTemplateRow[]>([]);
  const [templatesNotice, setTemplatesNotice] = React.useState<string | null>(null);
  const [selectedId, setSelectedId] = React.useState("");
  const [result, setResult] = React.useState("normal");
  // 第十八批 AP 段第 229 条 229-R2（PGM:966「已拍板：所有有过程的业务动作都必须留痕……」）：说明不预填，照片编号与二维码不再写死（先例第 32 条删而不补）；
  // 229-R8（BSM:940「……一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕……」）：巡检人由服务端取登录账号，页面不再手填。
  const [note, setNote] = React.useState("");
  const [abnormalNote, setAbnormalNote] = React.useState("");
  const [exceptionNote, setExceptionNote] = React.useState("");
  const [abnormalLevel, setAbnormalLevel] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该巡检任务/模板完整字段（枚举中文·时间中文·处理人可溯源）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(subset: "all" | "pending") {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", "inspection-tasks");
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
  const visibleTemplates = templates.filter((row) => row.id === selected?.templateId);
  // 第十八批 CG 段第 410 条（PGM:846「发现异常支持「轻记录/重转整改工单」分级与「全部转整改工单」两种模式」）：
  // 所选任务的模板为全部转整改工单才不分级；不挂模板或找不到模板按分级，异常须由巡检人选轻重。
  const graded = selected !== null && templates.find((row) => row.id === selected.templateId)?.abnormalMode !== "to_work_order";

  // 无权限副资源不请求（根因修）：巡检模板端点需 facility.inspection.template.view，页面基础权限 facility.inspection.view_assigned 不含之。
  const canInspectionTemplate = hasPermissions(effectivePermissions, ["facility.inspection.template.view"]);
  const { loading, error, reload } = useAdminResource(
    async () => {
      // AUD-032：巡检模板是副资源，外包账号可能无权（403）——降级为区块提示，不拖垮任务主内容。
      const [tasks, templateRows] = await Promise.all([
        api.get<VendorInspectionTaskRow[]>("/api/v1/facility/inspections/tasks"),
        loadSecondaryIfPermitted(canInspectionTemplate, () => api.get<VendorInspectionTemplateRow[]>("/api/v1/facility/inspections/templates"), [] as VendorInspectionTemplateRow[]),
      ]);
      setRows(tasks ?? []);
      setTemplates(templateRows.data);
      setTemplatesNotice(templateRows.notice);
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
    const request = { idempotencyKey: nextKey("vendor-inspection-start"), note: note.trim() || undefined };
    await runAction("vendor_inspection_started", request, () => api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(selected.id)}/start`, request));
  }

  async function submitRecord() {
    if (!selected) {
      setFormError("请选择一条巡检任务");
      return;
    }
    const levelNeeded = graded && result === "abnormal";
    if (levelNeeded && !abnormalLevel) {
      setFormError("请选择异常分级：轻记录或重转整改工单");
      return;
    }
    const request = {
      idempotencyKey: nextKey("vendor-inspection-record"),
      result,
      note: note.trim() || undefined,
      abnormalNote: result === "abnormal" ? abnormalNote : undefined,
      abnormalLevel: levelNeeded ? abnormalLevel : undefined,
      checklistResult: [],
    };
    await runAction("vendor_inspection_recorded", request, () => api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(selected.id)}/records`, request));
  }

  async function reportException() {
    if (!selected) {
      setFormError("请选择一条巡检任务");
      return;
    }
    if (!exceptionNote.trim()) {
      setFormError("异常说明不能为空");
      return;
    }
    if (graded && !abnormalLevel) {
      setFormError("请选择异常分级：轻记录或重转整改工单");
      return;
    }
    const request = {
      idempotencyKey: nextKey("vendor-inspection-exception"),
      gpsLat: null,
      gpsLng: null,
      checklistResult: [{ item: selected.title ?? selected.id, result: "abnormal" }],
      note: note.trim() || undefined,
      abnormalNote: exceptionNote.trim(),
      abnormalLevel: graded ? abnormalLevel : undefined,
    };
    await runAction("vendor_inspection_exception_reported", request, () => api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(selected.id)}/report-exception`, request));
  }

  const pendingRows = rows.filter((row) => !DONE_STATUSES.includes(row.status ?? ""));
  const focusRows = kpiFocus?.subset === "pending" ? pendingRows : rows;

  function openTaskDetail(row: VendorInspectionTaskRow) {
    setDetail({
      title: `巡检任务明细 · ${row.title ?? row.id}`,
      fields: [
        { label: "任务", value: row.title ?? row.id },
        { label: "类型", value: formatBusinessStatus(row.inspectionType) },
        { label: "状态", value: formatBusinessStatus(row.status) },
        { label: "排程", value: formatBusinessStatus(row.scheduleMode) },
        { label: "空间", value: row.spaceId ?? "—" },
        { label: "处理人", value: <TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.assigneeId}>{row.assigneeId ?? "—"}</TraceLink> },
        { label: "计划日期", value: cnDate(row.scheduledDate) },
        { label: "到期日期", value: cnDate(row.dueDate) },
        { label: "关联工单", value: row.workOrderId ?? "—" },
        { label: "使用模板", value: row.templateId ?? "—" },
        { label: "任务编号", value: row.id },
      ],
    });
  }

  function openTemplateDetail(row: VendorInspectionTemplateRow) {
    setDetail({
      title: `巡检模板明细 · ${row.name ?? row.id}`,
      fields: [
        { label: "模板", value: row.name ?? row.id },
        { label: "类型", value: formatBusinessStatus(row.inspectionType) },
        { label: "周期", value: row.cycleDays ? `${row.cycleDays} 天` : "—" },
        { label: "异常模式", value: formatBusinessStatus(row.abnormalMode) },
        { label: "检查要求", value: checklistText(row.checklist) },
        { label: "范围说明", value: row.scopeNote ?? "—" },
        { label: "状态", value: row.active === false ? "停用" : "启用" },
        { label: "模板编号", value: row.id },
      ],
    });
  }

  const tasksTable = (
    <div className="admin-table-scroll">
      <table className="admin-table" data-testid="vendor-inspections-table">
        <thead><tr><th>选择</th><th>任务</th><th>类型</th><th>状态</th><th>排程</th><th>空间</th><th>处理人</th><th>计划/到期</th><th>关联工单</th><th>详情</th></tr></thead>
        <tbody>
          {(kpiFocus ? focusRows : rows).map((row) => (
            <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`vendor-inspection-row-${row.id}`}
              onClick={() => openTaskDetail(row)}
              onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTaskDetail(row); } }}>
              <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此巡检 ${row.id}`} data-testid={`vendor-inspection-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
              <td>{row.title ?? row.id}</td>
              <td>{formatBusinessStatus(row.inspectionType)}</td>
              <td>{formatBusinessStatus(row.status)}</td>
              <td>{formatBusinessStatus(row.scheduleMode)}</td>
              <td>{row.spaceId ?? "-"}</td>
              <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.assigneeId}>{row.assigneeId ?? "-"}</TraceLink></td>
              <td>{cnDate(row.scheduledDate)} / {cnDate(row.dueDate)}</td>
              <td>{row.workOrderId ?? "-"}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看巡检任务详情" onClick={(event) => { event.stopPropagation(); openTaskDetail(row); }}>查看详情</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  return (
    <section data-testid="vendor-inspections-page">
      <AdminPageHeader title="外包巡检任务" description="安保/保洁外包人员只能查看派给自己的巡检任务、模板要求，并提交现场开始、记录与异常上报。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <>
            <div className="admin-state" data-testid="vendor-inspections-kpi-focus-banner">
              正在查看：{kpiFocus.subset === "pending" ? "待记录任务" : "已指派任务"}（{focusRows.length}）
              <button type="button" data-testid="vendor-inspections-kpi-focus-clear" onClick={exitKpiFocus}>清除筛选，返回全部</button>
            </div>
            {tasksTable}
          </>
        ) : (
          <>
            <div className="admin-kpi-grid" data-testid="vendor-inspections-summary">
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>已指派任务</span><button type="button" className="admin-kpi-value" data-testid="vendor-inspections-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>派给本人</small></div>
              <div className="admin-kpi-card admin-kpi-card--clickable"><span>待记录</span><button type="button" className="admin-kpi-value" data-testid="vendor-inspections-kpi-pending" onClick={() => enterKpiFocus("pending")}>{pendingRows.length}</button><small>未完成</small></div>
              <div className="admin-kpi-card"><span>当前任务</span><strong className="admin-kpi-value--text" title={selected?.title ?? selected?.id ?? "-"}>{selected?.title ?? selected?.id ?? "-"}</strong><small>{selected?.dueDate ?? "-"}</small></div>
            </div>

            <div className="admin-form-grid" data-testid="vendor-inspection-record-form">
              <label>结果<select data-testid="vendor-inspection-result" value={result} onChange={(event) => setResult(event.target.value)}><option value="normal">正常</option><option value="abnormal">异常</option></select></label>
              <label>记录说明<input data-testid="vendor-inspection-note" value={note} onChange={(event) => setNote(event.target.value)} /></label>
              <label>异常说明<input data-testid="vendor-inspection-abnormal-note" value={abnormalNote} onChange={(event) => setAbnormalNote(event.target.value)} /></label>
              {graded ? <label>异常分级<select data-testid="vendor-inspection-abnormal-level" value={abnormalLevel} onChange={(event) => setAbnormalLevel(event.target.value)}><option value="">请选择分级</option><option value="light">轻记录</option><option value="heavy">重转整改工单</option></select></label> : null}
              <label>异常上报说明<input data-testid="vendor-inspection-exception-note" value={exceptionNote} onChange={(event) => setExceptionNote(event.target.value)} /></label>
            </div>
            <div className="admin-form-row" data-testid="vendor-inspection-actions">
              <PermissionButton permissionsState={effectivePermissions} permissions={RECORD} data-testid="vendor-inspection-start" disabled={busyAction !== ""} onClick={() => void startTask()}>开始</PermissionButton>
              <PermissionButton permissionsState={effectivePermissions} permissions={RECORD} data-testid="vendor-inspection-record" disabled={busyAction !== ""} onClick={() => void submitRecord()}>记录</PermissionButton>
              <PermissionButton permissionsState={effectivePermissions} permissions={REPORT_EXCEPTION} data-testid="vendor-inspection-report-exception" disabled={busyAction !== ""} onClick={() => void reportException()}>上报异常</PermissionButton>
            </div>
            {formError ? <div data-testid="vendor-inspection-error" className="admin-inline-error">{formError}</div> : null}

            <h3>巡检模板/要求（只读）</h3>
            {templatesNotice ? (
              <div className="admin-state" data-testid="vendor-inspection-templates-degraded">{templatesNotice}</div>
            ) : (
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="vendor-inspection-templates-table">
                <thead><tr><th>模板</th><th>类型</th><th>周期</th><th>异常模式</th><th>检查要求</th><th>状态</th><th>详情</th></tr></thead>
                <tbody>
                  {(visibleTemplates.length > 0 ? visibleTemplates : templates).map((row) => (
                    <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`vendor-inspection-template-row-${row.id}`}
                      onClick={() => openTemplateDetail(row)}
                      onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openTemplateDetail(row); } }}>
                      <td>{row.name ?? row.id}</td>
                      <td>{formatBusinessStatus(row.inspectionType)}</td>
                      <td>{row.cycleDays ?? "-"}</td>
                      <td>{formatBusinessStatus(row.abnormalMode)}</td>
                      <td>{checklistText(row.checklist)} {row.scopeNote ?? ""}</td>
                      <td>{row.active === false ? "停用" : "启用"}</td>
                      <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看巡检模板详情" onClick={(event) => { event.stopPropagation(); openTemplateDetail(row); }}>查看详情</button></td>
                    </tr>
                  ))}
                  {templates.length === 0 ? <tr><td colSpan={7}>暂无巡检模板</td></tr> : null}
                </tbody>
              </table>
            </div>
            )}

            {tasksTable}
          </>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
