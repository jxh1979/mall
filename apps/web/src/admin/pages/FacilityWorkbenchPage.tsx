import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { shortId } from "../format/ids";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import { RowActions, type RowAction } from "../components/RowActions";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import type { AdminPageProps } from "../types";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { Tabs } from "antd";

type FacilityEquipmentRow = {
  id: string; name: string; equipmentType: string; status: string;
  spaceId?: string | null; brand?: string | null; responsibleUserId?: string | null;
  maintenanceCycleDays?: number | null; nextMaintenanceDue?: string | null;
  alarmSource?: string | null; alarmNote?: string | null;
};
type FacilityWorkOrderRow = {
  id: string; code?: string; workOrderType: string; priority: string; status: string; title: string;
  equipmentId?: string | null; assigneeId?: string | null; satisfaction?: number | null; overtime?: boolean;
};
type FacilityWorkOrderPolicyRow = {
  id: string; workOrderType: string; priority: string; responseMinutes: number; handleMinutes: number;
  requiresAcceptance: boolean; acceptanceBy: string; active: boolean;
};
type InspectionTemplateRow = {
  id: string; name: string; inspectionType: string; cycleDays?: number | null; abnormalMode: string; active: boolean;
};
type InspectionTaskRow = {
  id: string; templateId?: string | null; inspectionType: string; status: string; title: string;
  scheduleMode: string; assigneeId?: string | null; scheduledDate?: string | null; dueDate?: string | null; workOrderId?: string | null;
};
type InspectionRecordRow = { id: string; taskId: string; result: string; inspectorId?: string | null; abnormalNote?: string | null; recordedAt?: string | null };

const EQUIPMENT = ["facility.equipment.manage"];
const WORK_ORDER = ["facility.work_order.manage"];
const INSPECTION = ["facility.inspection.manage"];
const EQUIPMENT_TYPES = [
  ["elevator", "电梯扶梯"], ["hvac", "暖通空调"], ["power", "配电供电"], ["water_fire", "给排水消防"],
];
const EQUIPMENT_STATUSES = [
  ["normal", "正常"], ["alarm", "告警"], ["fault", "故障"], ["maintenance", "维保中"], ["disabled", "停用"], ["retired", "报废"],
];
const WORK_ORDER_TYPES = [
  ["repair", "报修"], ["cleaning", "保洁"], ["complaint", "投诉"], ["rectification", "整改"], ["inspection", "巡检"], ["other", "其他"],
];
const PRIORITIES = [["urgent", "紧急"], ["high", "高"], ["normal", "普通"]];
const INSPECTION_TYPES = [["security", "安防"], ["fire", "消防"], ["facility", "设施"], ["environment", "环境"]];
// 本页枚举中文兜底：既服务下拉展示，也服务表格裸字段渲染（value 不变）。
const LABELS: Record<string, string> = {
  ...Object.fromEntries(EQUIPMENT_TYPES), ...Object.fromEntries(EQUIPMENT_STATUSES),
  ...Object.fromEntries(WORK_ORDER_TYPES), ...Object.fromEntries(PRIORITIES),
  ...Object.fromEntries(INSPECTION_TYPES),
  manual: "手工", iot: "物联网", // 告警来源
  handler: "处理人", supervisor: "主管", reporter: "报告人", // 验收方
  light: "轻度告警", to_work_order: "转工单", // 巡检异常模式
  template: "按模板", route: "按路线", // 排程模式（manual=手工，见上）
  abnormal: "异常", // 巡检结果（normal=正常，已在设备状态里）
  // 工单/巡检状态枚举中文（statusBadge 与行明细共用；normal/alarm/fault/maintenance/disabled/retired 已在设备状态里）：
  pending_assign: "待派单", assigned: "已派单", processing: "处理中",
  pending_acceptance: "待验收", pending_review: "待回访", closed: "已关闭",
  suspended: "已挂起", overdue: "已逾期",
  pending: "待巡检", rectifying: "整改中", rechecked: "已复检",
  confirmed: "已确认", completed: "已完成",
};
const zh = (v?: string | null) => (v == null || v === "" ? "-" : (LABELS[v] ?? v));
const DEFAULT_POLICIES: FacilityWorkOrderPolicyRow[] = [
  { id: "fwop_repair_urgent", workOrderType: "repair", priority: "urgent", responseMinutes: 30, handleMinutes: 240, requiresAcceptance: true, acceptanceBy: "supervisor", active: true },
  { id: "fwop_repair_high", workOrderType: "repair", priority: "high", responseMinutes: 60, handleMinutes: 720, requiresAcceptance: true, acceptanceBy: "supervisor", active: true },
  { id: "fwop_cleaning_normal", workOrderType: "cleaning", priority: "normal", responseMinutes: 120, handleMinutes: 480, requiresAcceptance: false, acceptanceBy: "handler", active: true },
  { id: "fwop_complaint_normal", workOrderType: "complaint", priority: "normal", responseMinutes: 120, handleMinutes: 1440, requiresAcceptance: false, acceptanceBy: "reporter", active: true },
];

function idempotencyQuery() {
  return encodeURIComponent(`admin-web-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`);
}

function positiveInt(value: string) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.round(parsed) : undefined;
}

function splitLines(value: string) {
  return value.split(/[\n,，;；]+/).map((item) => item.trim()).filter(Boolean);
}

function statusBadge(status?: string | null) {
  const value = status || "-";
  const kind =
    ["normal", "closed", "confirmed", "completed"].includes(value) ? "is-ok"
      : ["alarm", "fault", "overdue", "rectifying", "pending_acceptance", "pending_review"].includes(value) ? "is-danger"
        : ["maintenance", "assigned", "processing", "rechecked"].includes(value) ? "is-info"
          : ["disabled", "retired", "suspended"].includes(value) ? "is-warn"
            : "";
  return <span className={`admin-badge ${kind}`}>{zh(value)}</span>;
}

export function FacilityWorkbenchPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [equipment, setEquipment] = React.useState<FacilityEquipmentRow[]>([]);
  const [maintenanceDue, setMaintenanceDue] = React.useState<FacilityEquipmentRow[]>([]);
  const [workOrders, setWorkOrders] = React.useState<FacilityWorkOrderRow[]>([]);
  const [overdueWorkOrders, setOverdueWorkOrders] = React.useState<FacilityWorkOrderRow[]>([]);
  const [inspectionTemplates, setInspectionTemplates] = React.useState<InspectionTemplateRow[]>([]);
  const [inspectionTasks, setInspectionTasks] = React.useState<InspectionTaskRow[]>([]);
  const [inspectionRecords, setInspectionRecords] = React.useState<InspectionRecordRow[]>([]);
  const [policyRows, setPolicyRows] = React.useState<FacilityWorkOrderPolicyRow[]>(DEFAULT_POLICIES);
  const [equipmentForm, setEquipmentForm] = React.useState({ name: "", equipmentType: "elevator", brand: "", responsibleUserId: "", maintenanceCycleDays: "90", spaceId: "" });
  const [statusForm, setStatusForm] = React.useState({ equipmentId: "", status: "normal", alarmSource: "manual", alarmNote: "" });
  const [workOrderForm, setWorkOrderForm] = React.useState({ workOrderType: "repair", priority: "normal", title: "", equipmentId: "", spaceId: "", reporterId: "", reporterType: "staff", source: "platform" });
  const [workOrderAction, setWorkOrderAction] = React.useState({ assigneeId: "", resolution: "现场处理完成", satisfaction: "5", note: "" });
  const [policyForm, setPolicyForm] = React.useState({ workOrderType: "repair", priority: "normal", responseMinutes: "60", handleMinutes: "720", requiresAcceptance: true, acceptanceBy: "supervisor", active: true });
  const [templateForm, setTemplateForm] = React.useState({ name: "", inspectionType: "security", cycleDays: "7", abnormalMode: "light", checklist: "现场照片,关键设备状态", scopeNote: "" });
  const [taskForm, setTaskForm] = React.useState({ templateId: "", inspectionType: "security", title: "", scheduleMode: "manual", assigneeId: "", scheduledDate: "", dueDate: "", spaceId: "" });
  const [selectedTaskId, setSelectedTaskId] = React.useState("");
  const [recordForm, setRecordForm] = React.useState({ result: "normal", inspectorId: "", note: "", abnormalNote: "" });
  // 行明细抽屉：本页 8 张业务表整行可点 → 该行完整字段（枚举中文·日期 cnDate·人员 TraceLink）。一个 detail state 复用。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 引导式流程 phase-2：新增设备收起为主按钮，点击弹出分步引导（GuidedFlowModal）。仅改设备创建主流程。
  const [createOpen, setCreateOpen] = React.useState(false);
  const [equipmentError, setEquipmentError] = React.useState("");

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [equipmentRows, dueRows, orderRows, overdueRows, templateRows, taskRows] = await Promise.all([
        api.get<FacilityEquipmentRow[]>("/api/v1/facility/equipment"),
        api.get<FacilityEquipmentRow[]>("/api/v1/facility/equipment/maintenance-due"),
        api.get<FacilityWorkOrderRow[]>("/api/v1/facility/work-orders"),
        api.get<FacilityWorkOrderRow[]>("/api/v1/facility/work-orders/overdue"),
        api.get<InspectionTemplateRow[]>("/api/v1/facility/inspections/templates"),
        api.get<InspectionTaskRow[]>("/api/v1/facility/inspections/tasks"),
      ]);
      setEquipment(equipmentRows ?? []);
      setMaintenanceDue(dueRows ?? []);
      setWorkOrders(orderRows ?? []);
      setOverdueWorkOrders(overdueRows ?? []);
      setInspectionTemplates(templateRows ?? []);
      setInspectionTasks(taskRows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "楼宇运营数据读取失败", onStart: () => setGlobalError(null) },
  );

  async function createEquipment() {
    // 双保险：步内已校验设备名，提交前再挡一次。
    if (!equipmentForm.name.trim()) {
      setEquipmentError("设备名称不能为空");
      return;
    }
    try {
      await api.post("/api/v1/facility/equipment", {
        name: equipmentForm.name.trim(),
        equipmentType: equipmentForm.equipmentType,
        brand: equipmentForm.brand.trim() || undefined,
        responsibleUserId: equipmentForm.responsibleUserId.trim() || undefined,
        maintenanceCycleDays: positiveInt(equipmentForm.maintenanceCycleDays),
        spaceId: equipmentForm.spaceId.trim() || undefined,
      });
      setActionResult({ action: "facility_equipment_created", name: equipmentForm.name.trim() });
      setEquipmentForm({ ...equipmentForm, name: "", brand: "" });
      setCreateOpen(false);
      await reload();
    } catch (createError) {
      setEquipmentError(createError instanceof Error ? createError.message : "新建设备失败");
    }
  }

  async function changeEquipmentStatus() {
    if (!statusForm.equipmentId.trim()) {
      setGlobalError("先选择或填写设备 id");
      return;
    }
    await api.post(`/api/v1/facility/equipment/${encodeURIComponent(statusForm.equipmentId.trim())}/status`, {
      status: statusForm.status,
      alarmSource: statusForm.status === "alarm" ? statusForm.alarmSource : undefined,
      alarmNote: statusForm.alarmNote.trim() || undefined,
    });
    setActionResult({ action: "facility_equipment_status_changed", equipmentId: statusForm.equipmentId.trim(), status: statusForm.status });
    await reload();
  }

  async function maintenanceDone(id: string) {
    await api.post(`/api/v1/facility/equipment/${encodeURIComponent(id)}/maintenance-done?idempotency_key=${idempotencyQuery()}`, {});
    setActionResult({ action: "facility_equipment_maintenance_done", equipmentId: id });
    await reload();
  }

  async function createWorkOrder() {
    if (!workOrderForm.title.trim()) {
      setGlobalError("工单标题不能为空");
      return;
    }
    await api.post("/api/v1/facility/work-orders", {
      workOrderType: workOrderForm.workOrderType,
      priority: workOrderForm.priority,
      title: workOrderForm.title.trim(),
      source: workOrderForm.source,
      equipmentId: workOrderForm.equipmentId.trim() || undefined,
      spaceId: workOrderForm.spaceId.trim() || undefined,
      reporterId: workOrderForm.reporterId.trim() || undefined,
      reporterType: workOrderForm.reporterType,
    });
    setActionResult({ action: "facility_work_order_created", title: workOrderForm.title.trim() });
    setWorkOrderForm({ ...workOrderForm, title: "" });
    await reload();
  }

  async function assignWorkOrder(id: string) {
    if (!workOrderAction.assigneeId.trim()) {
      setGlobalError("派单需填写处理人 id");
      return;
    }
    await api.post(`/api/v1/facility/work-orders/${encodeURIComponent(id)}/assign`, { assigneeId: workOrderAction.assigneeId.trim() });
    setActionResult({ action: "facility_work_order_assigned", workOrderId: id });
    await reload();
  }

  async function workOrderActionPost(id: string, action: "start" | "complete" | "accept" | "review" | "suspend" | "resume") {
    await api.post(`/api/v1/facility/work-orders/${encodeURIComponent(id)}/${action}`, {
      resolution: action === "complete" ? workOrderAction.resolution.trim() || "已处理" : undefined,
      satisfaction: action === "accept" || action === "review" ? positiveInt(workOrderAction.satisfaction) : undefined,
      note: workOrderAction.note.trim() || undefined,
    });
    setActionResult({ action: `facility_work_order_${action}`, workOrderId: id });
    await reload();
  }

  async function markOverdue() {
    const count = await api.post<number>(`/api/v1/facility/work-orders/mark-overdue?idempotency_key=${idempotencyQuery()}`, {});
    setActionResult({ action: "facility_work_orders_marked_overdue", count });
    await reload();
  }

  async function savePolicy() {
    const saved = await api.post<FacilityWorkOrderPolicyRow>("/api/v1/facility/work-orders/policies", {
      workOrderType: policyForm.workOrderType,
      priority: policyForm.priority,
      responseMinutes: positiveInt(policyForm.responseMinutes),
      handleMinutes: positiveInt(policyForm.handleMinutes),
      requiresAcceptance: policyForm.requiresAcceptance,
      acceptanceBy: policyForm.acceptanceBy,
      active: policyForm.active,
    });
    setPolicyRows((current) => [...current.filter((row) => row.workOrderType !== saved.workOrderType || row.priority !== saved.priority), saved]);
    setActionResult({ action: "facility_work_order_policy_saved", policyId: saved.id });
  }

  async function createInspectionTemplate() {
    if (!templateForm.name.trim()) {
      setGlobalError("巡检模板名称不能为空");
      return;
    }
    await api.post("/api/v1/facility/inspections/templates", {
      name: templateForm.name.trim(),
      inspectionType: templateForm.inspectionType,
      cycleDays: positiveInt(templateForm.cycleDays),
      abnormalMode: templateForm.abnormalMode,
      checklist: splitLines(templateForm.checklist).map((label) => ({ label })),
      scopeNote: templateForm.scopeNote.trim() || undefined,
      active: true,
    });
    setActionResult({ action: "facility_inspection_template_created", name: templateForm.name.trim() });
    setTemplateForm({ ...templateForm, name: "" });
    await reload();
  }

  async function createInspectionTask() {
    if (!taskForm.title.trim() && !taskForm.templateId.trim()) {
      setGlobalError("巡检任务需填写标题或选择模板");
      return;
    }
    await api.post("/api/v1/facility/inspections/tasks", {
      templateId: taskForm.templateId.trim() || undefined,
      inspectionType: taskForm.inspectionType,
      title: taskForm.title.trim() || undefined,
      scheduleMode: taskForm.scheduleMode,
      assigneeId: taskForm.assigneeId.trim() || undefined,
      scheduledDate: taskForm.scheduledDate || undefined,
      dueDate: taskForm.dueDate || undefined,
      spaceId: taskForm.spaceId.trim() || undefined,
    });
    setActionResult({ action: "facility_inspection_task_created", title: taskForm.title.trim() || taskForm.templateId.trim() });
    setTaskForm({ ...taskForm, title: "" });
    await reload();
  }

  async function selectInspectionTask(id: string) {
    setSelectedTaskId(id);
    setInspectionRecords(await api.get<InspectionRecordRow[]>(`/api/v1/facility/inspections/tasks/${encodeURIComponent(id)}/records`));
  }

  async function startInspectionTask(id: string) {
    await api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(id)}/start`, { note: "后台开始巡检" });
    setActionResult({ action: "facility_inspection_task_started", taskId: id });
    await reload();
  }

  async function submitInspectionRecord() {
    if (!selectedTaskId.trim() || !recordForm.inspectorId.trim()) {
      setGlobalError("提交巡检记录需选择任务并填写巡检人");
      return;
    }
    await api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(selectedTaskId.trim())}/records`, {
      result: recordForm.result,
      inspectorId: recordForm.inspectorId.trim(),
      note: recordForm.note.trim() || undefined,
      abnormalNote: recordForm.result === "abnormal" ? recordForm.abnormalNote.trim() : undefined,
      checklistResult: [],
      photos: [],
    });
    setActionResult({ action: "facility_inspection_record_submitted", taskId: selectedTaskId.trim(), result: recordForm.result });
    await selectInspectionTask(selectedTaskId.trim());
    await reload();
  }

  async function inspectionAction(id: string, action: "recheck" | "supervisor-confirm") {
    await api.post(`/api/v1/facility/inspections/tasks/${encodeURIComponent(id)}/${action}`, { note: recordForm.note.trim() || undefined });
    setActionResult({ action: `facility_inspection_${action}`, taskId: id });
    await reload();
  }

  function userTrace(userId?: string | null) {
    return <TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={userId}>{userId ?? "-"}</TraceLink>;
  }

  function openEquipmentDetail(row: FacilityEquipmentRow) {
    setDetail({
      title: `设备明细 · ${row.name}`,
      fields: [
        { label: "设备编号", value: row.id },
        { label: "名称", value: row.name },
        { label: "分类", value: zh(row.equipmentType) },
        { label: "状态", value: zh(row.status) },
        { label: "品牌", value: row.brand ?? "-" },
        { label: "责任人", value: userTrace(row.responsibleUserId) },
        { label: "所在空间", value: row.spaceId ?? "-" },
        { label: "维保周期", value: row.maintenanceCycleDays != null ? `${row.maintenanceCycleDays} 天` : "-" },
        { label: "维保到期", value: cnDate(row.nextMaintenanceDue) },
        { label: "告警来源", value: zh(row.alarmSource) },
        { label: "告警说明", value: row.alarmNote ?? "-" },
      ],
    });
  }

  function openMaintenanceDueDetail(row: FacilityEquipmentRow) {
    setDetail({
      title: `维保到期明细 · ${row.name}`,
      fields: [
        { label: "设备编号", value: row.id },
        { label: "名称", value: row.name },
        { label: "分类", value: zh(row.equipmentType) },
        { label: "责任人", value: userTrace(row.responsibleUserId) },
        { label: "到期日", value: cnDate(row.nextMaintenanceDue) },
      ],
    });
  }

  function openWorkOrderDetail(row: FacilityWorkOrderRow) {
    setDetail({
      title: `工单明细 · ${row.code ?? row.id}`,
      fields: [
        { label: "工单编号", value: row.code ?? row.id },
        { label: "标题", value: row.title },
        { label: "类型", value: zh(row.workOrderType) },
        { label: "优先级", value: zh(row.priority) },
        { label: "状态", value: zh(row.status) },
        { label: "是否逾期", value: row.overtime ? "已逾期" : "未逾期" },
        { label: "关联设备", value: row.equipmentId ?? "-" },
        { label: "处理人", value: userTrace(row.assigneeId) },
        { label: "满意度", value: row.satisfaction != null ? `${row.satisfaction} 分` : "-" },
      ],
    });
  }

  function openOverdueWorkOrderDetail(row: FacilityWorkOrderRow) {
    setDetail({
      title: `逾期工单明细 · ${row.code ?? row.id}`,
      fields: [
        { label: "工单编号", value: row.code ?? row.id },
        { label: "标题", value: row.title },
        { label: "类型", value: zh(row.workOrderType) },
        { label: "优先级", value: zh(row.priority) },
        { label: "状态", value: `${zh(row.status)} · 已逾期` },
        { label: "处理人", value: userTrace(row.assigneeId) },
      ],
    });
  }

  function openPolicyDetail(row: FacilityWorkOrderPolicyRow) {
    setDetail({
      title: `SLA 策略明细 · ${zh(row.workOrderType)}/${zh(row.priority)}`,
      fields: [
        { label: "工单类型", value: zh(row.workOrderType) },
        { label: "优先级", value: zh(row.priority) },
        { label: "响应时限", value: `${row.responseMinutes} 分钟` },
        { label: "处理时限", value: `${row.handleMinutes} 分钟` },
        { label: "是否需验收", value: row.requiresAcceptance ? "需验收" : "无需" },
        { label: "验收方", value: row.requiresAcceptance ? zh(row.acceptanceBy) : "-" },
        { label: "状态", value: row.active ? "启用" : "停用" },
      ],
    });
  }

  function openInspectionTemplateDetail(row: InspectionTemplateRow) {
    setDetail({
      title: `巡检模板明细 · ${row.name}`,
      fields: [
        { label: "模板编号", value: row.id },
        { label: "模板名", value: row.name },
        { label: "巡检类型", value: zh(row.inspectionType) },
        { label: "巡检周期", value: row.cycleDays != null ? `${row.cycleDays} 天` : "-" },
        { label: "异常模式", value: zh(row.abnormalMode) },
        { label: "状态", value: row.active ? "启用" : "停用" },
      ],
    });
  }

  function openInspectionTaskDetail(row: InspectionTaskRow) {
    setDetail({
      title: `巡检任务明细 · ${row.title}`,
      fields: [
        { label: "任务编号", value: row.id },
        { label: "标题", value: row.title },
        { label: "巡检类型", value: zh(row.inspectionType) },
        { label: "排程模式", value: zh(row.scheduleMode) },
        { label: "状态", value: zh(row.status) },
        { label: "巡检人", value: userTrace(row.assigneeId) },
        { label: "计划日期", value: cnDate(row.scheduledDate) },
        { label: "到期日期", value: cnDate(row.dueDate) },
        { label: "关联模板", value: row.templateId ?? "-" },
        { label: "关联工单", value: row.workOrderId ?? "-" },
      ],
    });
  }

  function openInspectionRecordDetail(row: InspectionRecordRow) {
    setDetail({
      title: `巡检记录明细 · ${row.id}`,
      fields: [
        { label: "记录编号", value: row.id },
        { label: "关联任务", value: row.taskId },
        { label: "结果", value: zh(row.result) },
        { label: "巡检人", value: userTrace(row.inspectorId) },
        { label: "异常说明", value: row.abnormalNote ?? "-" },
        { label: "记录时间", value: cnDate(row.recordedAt) },
      ],
    });
  }

  return (
    <section data-testid="facility-workbench-page">
      <AdminPageHeader title="楼宇运营工作台" description="设备台账、维保工单、楼宇巡检按后台真实端点闭环；设备告警和巡检异常仍由后端触发工单联动。" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-preview" data-testid="facility-workbench-guide">
          <strong>这一页做什么：</strong>楼宇运营三大块——① 设备台账（含维保到期提醒）② 维保工单（派单 / 逾期 / SLA 策略）③ 楼宇巡检（模板 / 任务 / 记录）。切换下方标签查看；设备告警与巡检异常仍由后端自动联动生成工单。
        </div>
        <Tabs
          data-testid="facility-workbench-tabs"
          items={[
            { key: "equipment", label: "① 设备台账", forceRender: true, children: (<>
        <h3>设备台账</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-equipment-table">
            <thead><tr><th>id</th><th>名称</th><th>分类</th><th>状态</th><th>品牌/责任人</th><th>维保到期</th><th>告警</th><th>操作</th></tr></thead>
            <tbody>{equipment.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-equipment-${row.id}`}
                onClick={() => openEquipmentDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openEquipmentDetail(row); } }}>
                <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td>{row.name}</td><td>{zh(row.equipmentType)}</td><td>{statusBadge(row.status)}</td>
                <td>{row.brand ?? "-"} / {row.responsibleUserId ?? "-"}</td><td>{cnDate(row.nextMaintenanceDue)}</td>
                <td>{zh(row.alarmSource)} {row.alarmNote ?? ""}</td>
                <td onClick={(event) => event.stopPropagation()}>
                  <button type="button" data-testid={`facility-equipment-select-${row.id}`} onClick={() => setStatusForm({ ...statusForm, equipmentId: row.id })}>选中</button>
                  <PermissionButton permissionsState={effectivePermissions} permissions={EQUIPMENT} data-testid={`facility-equipment-maintenance-done-${row.id}`} onClick={() => void maintenanceDone(row.id)}>维保完成</PermissionButton>
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        {/* 引导式流程 phase-2：新增设备入口按钮（权限门在入口），点击弹出分步引导。其余表单（状态变更等）保持不变。 */}
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={EQUIPMENT} data-testid="facility-equipment-create-open" onClick={() => { setEquipmentError(""); setCreateOpen(true); }}>＋ 新增设备</PermissionButton>
        </div>
        <GuidedFlowModal
          open={createOpen}
          title="新增设备"
          testId="facility-equipment-flow"
          submitTestId="facility-equipment-create"
          submitLabel="新建设备"
          error={equipmentError}
          onClose={() => setCreateOpen(false)}
          onSubmit={() => void createEquipment()}
          steps={[
            {
              title: "设备信息",
              description: "填写设备名称、选择分类，可填品牌。",
              validate: () => (equipmentForm.name.trim() ? null : "设备名称不能为空。"),
              content: (
                <>
                  <label>设备名称<input data-testid="facility-equipment-name" aria-label="设备名称" placeholder="设备名称" value={equipmentForm.name} onChange={(event) => setEquipmentForm({ ...equipmentForm, name: event.target.value })} /></label>
                  <label>设备分类<select data-testid="facility-equipment-type" aria-label="设备分类" value={equipmentForm.equipmentType} onChange={(event) => setEquipmentForm({ ...equipmentForm, equipmentType: event.target.value })}>{EQUIPMENT_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
                  <label>品牌<input data-testid="facility-equipment-brand" aria-label="品牌" placeholder="品牌" value={equipmentForm.brand} onChange={(event) => setEquipmentForm({ ...equipmentForm, brand: event.target.value })} /></label>
                </>
              ),
            },
            {
              title: "责任与维保",
              description: "指定责任人、维保周期与所在空间（均可空）。",
              content: (
                <>
                  <label>责任人<input data-testid="facility-equipment-owner" aria-label="责任人" placeholder="责任人 id" value={equipmentForm.responsibleUserId} onChange={(event) => setEquipmentForm({ ...equipmentForm, responsibleUserId: event.target.value })} /></label>
                  <label>维保周期（天）<input data-testid="facility-equipment-cycle" aria-label="维保周期" type="number" min="1" placeholder="维保周期(天)" value={equipmentForm.maintenanceCycleDays} onChange={(event) => setEquipmentForm({ ...equipmentForm, maintenanceCycleDays: event.target.value })} /></label>
                  <label>所在空间<input data-testid="facility-equipment-space" aria-label="空间" placeholder="空间 id" value={equipmentForm.spaceId} onChange={(event) => setEquipmentForm({ ...equipmentForm, spaceId: event.target.value })} /></label>
                </>
              ),
            },
          ]}
          summary={[
            { label: "设备名称", value: equipmentForm.name.trim() || "未填写" },
            { label: "设备分类", value: zh(equipmentForm.equipmentType) },
            { label: "品牌", value: equipmentForm.brand.trim() || "未填写" },
            { label: "责任人", value: equipmentForm.responsibleUserId.trim() || "未指定" },
            { label: "维保周期（天）", value: equipmentForm.maintenanceCycleDays.trim() || "未填写" },
            { label: "所在空间", value: equipmentForm.spaceId.trim() || "未指定" },
          ]}
        />
        <div className="admin-inline-form" data-testid="facility-equipment-status-form">
          <input data-testid="facility-equipment-status-id" aria-label="状态设备 id" placeholder="设备 id" value={statusForm.equipmentId} onChange={(event) => setStatusForm({ ...statusForm, equipmentId: event.target.value })} />
          <select data-testid="facility-equipment-status" aria-label="设备状态" value={statusForm.status} onChange={(event) => setStatusForm({ ...statusForm, status: event.target.value })}>{EQUIPMENT_STATUSES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <select data-testid="facility-equipment-alarm-source" aria-label="告警源" value={statusForm.alarmSource} onChange={(event) => setStatusForm({ ...statusForm, alarmSource: event.target.value })}><option value="manual">手工</option><option value="iot">物联网</option></select>
          <input data-testid="facility-equipment-alarm-note" aria-label="告警说明" placeholder="告警说明" value={statusForm.alarmNote} onChange={(event) => setStatusForm({ ...statusForm, alarmNote: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={EQUIPMENT} data-testid="facility-equipment-status-change" onClick={() => void changeEquipmentStatus()}>状态变更/告警上报</PermissionButton>
        </div>
        <h3>设备维保到期清单</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-maintenance-due-table">
            <thead><tr><th>设备</th><th>分类</th><th>责任人</th><th>到期日</th></tr></thead>
            <tbody>{maintenanceDue.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-maintenance-due-${row.id}`} onClick={() => openMaintenanceDueDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMaintenanceDueDetail(row); } }}><td>{row.name}</td><td>{zh(row.equipmentType)}</td><td>{row.responsibleUserId ?? "-"}</td><td>{cnDate(row.nextMaintenanceDue)}</td></tr>)}</tbody>
          </table>
        </div>

          </>) },
          { key: "workorders", label: "② 维保工单", forceRender: true, children: (<>
        <h3>维保工单</h3>
        <div className="admin-inline-form" data-testid="facility-work-order-create-form">
          <select data-testid="facility-work-order-type" aria-label="工单类型" value={workOrderForm.workOrderType} onChange={(event) => setWorkOrderForm({ ...workOrderForm, workOrderType: event.target.value })}>{WORK_ORDER_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <select data-testid="facility-work-order-priority" aria-label="优先级" value={workOrderForm.priority} onChange={(event) => setWorkOrderForm({ ...workOrderForm, priority: event.target.value })}>{PRIORITIES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input data-testid="facility-work-order-title" aria-label="工单标题" placeholder="工单标题" value={workOrderForm.title} onChange={(event) => setWorkOrderForm({ ...workOrderForm, title: event.target.value })} />
          <input data-testid="facility-work-order-equipment" aria-label="关联设备" placeholder="设备 id" value={workOrderForm.equipmentId} onChange={(event) => setWorkOrderForm({ ...workOrderForm, equipmentId: event.target.value })} />
          <input data-testid="facility-work-order-space" aria-label="空间 id" placeholder="空间 id" value={workOrderForm.spaceId} onChange={(event) => setWorkOrderForm({ ...workOrderForm, spaceId: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={WORK_ORDER} data-testid="facility-work-order-create" onClick={() => void createWorkOrder()}>新建工单</PermissionButton>
        </div>
        <div className="admin-inline-form" data-testid="facility-work-order-action-form">
          <input data-testid="facility-work-order-assignee" aria-label="派单处理人" placeholder="处理人 id" value={workOrderAction.assigneeId} onChange={(event) => setWorkOrderAction({ ...workOrderAction, assigneeId: event.target.value })} />
          <input data-testid="facility-work-order-resolution" aria-label="处理结果" placeholder="处理结果" value={workOrderAction.resolution} onChange={(event) => setWorkOrderAction({ ...workOrderAction, resolution: event.target.value })} />
          <input data-testid="facility-work-order-satisfaction" aria-label="满意度" type="number" min="1" max="5" value={workOrderAction.satisfaction} onChange={(event) => setWorkOrderAction({ ...workOrderAction, satisfaction: event.target.value })} />
          <input data-testid="facility-work-order-note" aria-label="操作备注" placeholder="备注" value={workOrderAction.note} onChange={(event) => setWorkOrderAction({ ...workOrderAction, note: event.target.value })} />
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-work-orders-table">
            <thead><tr><th>编号</th><th>类型/优先级</th><th>标题</th><th>状态</th><th>处理人</th><th>满意度</th><th>操作</th></tr></thead>
            <tbody>{workOrders.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-work-order-${row.id}`}
                onClick={() => openWorkOrderDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}>
                <td>{row.code ?? row.id}</td><td>{zh(row.workOrderType)} / {zh(row.priority)}</td><td>{row.title}</td><td>{statusBadge(row.status)}{row.overtime ? "（已逾期）" : ""}</td><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.assigneeId}>{row.assigneeId ?? "-"}</TraceLink></td><td>{row.satisfaction ?? "-"}</td>
                <td onClick={(event) => event.stopPropagation()}>
                  {(() => {
                    const wo: RowAction[] = [];
                    if (row.status === "pending_assign") wo.push({ key: "assign", label: "派单", permissions: WORK_ORDER, onClick: () => void assignWorkOrder(row.id), testId: `facility-work-order-assign-${row.id}` });
                    if (row.status === "assigned") wo.push({ key: "start", label: "开始", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "start"), testId: `facility-work-order-start-${row.id}` });
                    if (row.status === "processing") wo.push({ key: "complete", label: "完成", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "complete"), testId: `facility-work-order-complete-${row.id}` });
                    if (row.status === "pending_acceptance") wo.push({ key: "accept", label: "验收", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "accept"), testId: `facility-work-order-accept-${row.id}` });
                    if (row.status === "pending_review") wo.push({ key: "review", label: "回访", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "review"), testId: `facility-work-order-review-${row.id}` });
                    if (row.status !== "closed" && row.status !== "suspended") wo.push({ key: "suspend", label: "挂起", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "suspend"), testId: `facility-work-order-suspend-${row.id}` });
                    if (row.status === "suspended") wo.push({ key: "resume", label: "恢复", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "resume"), testId: `facility-work-order-resume-${row.id}` });
                    return <RowActions actions={wo} permissionsState={effectivePermissions} testId={`facility-work-order-actions-${row.id}`} />;
                  })()}
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <h3>逾期工单</h3>
        <div className="admin-inline-form" data-testid="facility-work-order-overdue-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={WORK_ORDER} data-testid="facility-work-order-mark-overdue" onClick={() => void markOverdue()}>标记逾期</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-work-orders-overdue-table">
            <thead><tr><th>编号</th><th>类型/优先级</th><th>标题</th><th>状态</th><th>处理人</th></tr></thead>
            <tbody>{overdueWorkOrders.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-work-order-overdue-${row.id}`} onClick={() => openOverdueWorkOrderDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openOverdueWorkOrderDetail(row); } }}><td>{row.code ?? row.id}</td><td>{zh(row.workOrderType)} / {zh(row.priority)}</td><td>{row.title}</td><td>{statusBadge(row.status)} · 已逾期</td><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.assigneeId}>{row.assigneeId ?? "-"}</TraceLink></td></tr>)}</tbody>
          </table>
        </div>
        <h3>SLA 策略表</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-work-order-policies-table">
            <thead><tr><th>类型</th><th>优先级</th><th>响应/处理分钟</th><th>验收</th><th>状态</th></tr></thead>
            <tbody>{policyRows.map((row) => <tr key={`${row.workOrderType}-${row.priority}`} className="admin-row-clickable" tabIndex={0} data-testid={`facility-work-order-policy-${row.workOrderType}-${row.priority}`} onClick={() => openPolicyDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}><td>{zh(row.workOrderType)}</td><td>{zh(row.priority)}</td><td>{row.responseMinutes} / {row.handleMinutes}</td><td>{row.requiresAcceptance ? zh(row.acceptanceBy) : "无需"}</td><td>{row.active ? "启用" : "停用"}</td></tr>)}</tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="facility-work-order-policy-form">
          <select data-testid="facility-policy-type" aria-label="策略类型" value={policyForm.workOrderType} onChange={(event) => setPolicyForm({ ...policyForm, workOrderType: event.target.value })}>{WORK_ORDER_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <select data-testid="facility-policy-priority" aria-label="策略优先级" value={policyForm.priority} onChange={(event) => setPolicyForm({ ...policyForm, priority: event.target.value })}>{PRIORITIES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input data-testid="facility-policy-response" aria-label="响应分钟" type="number" min="1" value={policyForm.responseMinutes} onChange={(event) => setPolicyForm({ ...policyForm, responseMinutes: event.target.value })} />
          <input data-testid="facility-policy-handle" aria-label="处理分钟" type="number" min="1" value={policyForm.handleMinutes} onChange={(event) => setPolicyForm({ ...policyForm, handleMinutes: event.target.value })} />
          <label><input data-testid="facility-policy-requires-acceptance" type="checkbox" checked={policyForm.requiresAcceptance} onChange={(event) => setPolicyForm({ ...policyForm, requiresAcceptance: event.target.checked })} />需验收</label>
          <select data-testid="facility-policy-acceptance-by" aria-label="验收方" value={policyForm.acceptanceBy} onChange={(event) => setPolicyForm({ ...policyForm, acceptanceBy: event.target.value })}><option value="handler">处理人</option><option value="supervisor">主管</option><option value="reporter">报告人</option></select>
          <PermissionButton permissionsState={effectivePermissions} permissions={WORK_ORDER} data-testid="facility-policy-save" onClick={() => void savePolicy()}>保存 SLA</PermissionButton>
        </div>

          </>) },
          { key: "inspection", label: "③ 楼宇巡检", forceRender: true, children: (<>
        <h3>楼宇巡检</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-inspection-templates-table">
            <thead><tr><th>id</th><th>模板</th><th>类型</th><th>周期</th><th>异常模式</th><th>状态</th></tr></thead>
            <tbody>{inspectionTemplates.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-inspection-template-${row.id}`} onClick={() => openInspectionTemplateDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openInspectionTemplateDetail(row); } }}><td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td>{row.name}</td><td>{zh(row.inspectionType)}</td><td>{row.cycleDays ?? "-"}</td><td>{zh(row.abnormalMode)}</td><td>{row.active ? "启用" : "停用"}</td></tr>)}</tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="facility-inspection-template-form">
          <input data-testid="facility-inspection-template-name" aria-label="模板名" placeholder="模板名" value={templateForm.name} onChange={(event) => setTemplateForm({ ...templateForm, name: event.target.value })} />
          <select data-testid="facility-inspection-template-type" aria-label="巡检类型" value={templateForm.inspectionType} onChange={(event) => setTemplateForm({ ...templateForm, inspectionType: event.target.value })}>{INSPECTION_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input data-testid="facility-inspection-template-cycle" aria-label="周期天数" type="number" min="1" value={templateForm.cycleDays} onChange={(event) => setTemplateForm({ ...templateForm, cycleDays: event.target.value })} />
          <select data-testid="facility-inspection-template-abnormal-mode" aria-label="异常模式" value={templateForm.abnormalMode} onChange={(event) => setTemplateForm({ ...templateForm, abnormalMode: event.target.value })}><option value="light">轻度告警</option><option value="to_work_order">转工单</option></select>
          <input data-testid="facility-inspection-template-checklist" aria-label="检查项" placeholder="检查项，逗号分隔" value={templateForm.checklist} onChange={(event) => setTemplateForm({ ...templateForm, checklist: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={INSPECTION} data-testid="facility-inspection-template-create" onClick={() => void createInspectionTemplate()}>新建模板</PermissionButton>
        </div>
        <div className="admin-inline-form" data-testid="facility-inspection-task-form">
          <input data-testid="facility-inspection-task-template" aria-label="模板 id" placeholder="模板 id" value={taskForm.templateId} onChange={(event) => setTaskForm({ ...taskForm, templateId: event.target.value })} />
          <select data-testid="facility-inspection-task-type" aria-label="任务类型" value={taskForm.inspectionType} onChange={(event) => setTaskForm({ ...taskForm, inspectionType: event.target.value })}>{INSPECTION_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input data-testid="facility-inspection-task-title" aria-label="任务标题" placeholder="任务标题" value={taskForm.title} onChange={(event) => setTaskForm({ ...taskForm, title: event.target.value })} />
          <select data-testid="facility-inspection-task-schedule-mode" aria-label="排程模式" value={taskForm.scheduleMode} onChange={(event) => setTaskForm({ ...taskForm, scheduleMode: event.target.value })}><option value="manual">手工</option><option value="template">按模板</option><option value="route">按路线</option></select>
          <input data-testid="facility-inspection-task-assignee" aria-label="巡检人" placeholder="巡检人 id" value={taskForm.assigneeId} onChange={(event) => setTaskForm({ ...taskForm, assigneeId: event.target.value })} />
          <CnDatePicker data-testid="facility-inspection-task-scheduled-date" aria-label="计划日期" value={taskForm.scheduledDate} onChange={(v) => setTaskForm({ ...taskForm, scheduledDate: v })} />
          <CnDatePicker data-testid="facility-inspection-task-due-date" aria-label="到期日期" value={taskForm.dueDate} onChange={(v) => setTaskForm({ ...taskForm, dueDate: v })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={INSPECTION} data-testid="facility-inspection-task-create" onClick={() => void createInspectionTask()}>新建任务</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-inspection-tasks-table">
            <thead><tr><th>id</th><th>标题</th><th>类型/排程</th><th>状态</th><th>人员/日期</th><th>工单</th><th>操作</th></tr></thead>
            <tbody>{inspectionTasks.map((row) => (
              <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-inspection-task-${row.id}`}
                onClick={() => openInspectionTaskDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openInspectionTaskDetail(row); } }}>
                <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td>{row.title}</td><td>{zh(row.inspectionType)} / {zh(row.scheduleMode)}</td><td>{statusBadge(row.status)}</td><td><span onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.assigneeId}>{row.assigneeId ?? "-"}</TraceLink></span> / {cnDate(row.dueDate ?? row.scheduledDate)}</td><td>{row.workOrderId ?? "-"}</td>
                <td onClick={(event) => event.stopPropagation()}>
                  <button type="button" data-testid={`facility-inspection-select-task-${row.id}`} onClick={() => void selectInspectionTask(row.id)}>记录</button>
                  {row.status === "pending" ? <PermissionButton permissionsState={effectivePermissions} permissions={INSPECTION} data-testid={`facility-inspection-start-${row.id}`} onClick={() => void startInspectionTask(row.id)}>开始</PermissionButton> : null}
                  {row.status === "rectifying" ? <PermissionButton permissionsState={effectivePermissions} permissions={INSPECTION} data-testid={`facility-inspection-recheck-${row.id}`} onClick={() => void inspectionAction(row.id, "recheck")}>复检</PermissionButton> : null}
                  {row.status === "rechecked" ? <PermissionButton permissionsState={effectivePermissions} permissions={INSPECTION} data-testid={`facility-inspection-confirm-${row.id}`} onClick={() => void inspectionAction(row.id, "supervisor-confirm")}>主管确认</PermissionButton> : null}
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="facility-inspection-record-form">
          <input data-testid="facility-inspection-record-task-id" aria-label="巡检任务 id" placeholder="任务 id" value={selectedTaskId} onChange={(event) => setSelectedTaskId(event.target.value)} />
          <select data-testid="facility-inspection-record-result" aria-label="巡检结果" value={recordForm.result} onChange={(event) => setRecordForm({ ...recordForm, result: event.target.value })}><option value="normal">正常</option><option value="abnormal">异常</option></select>
          <input data-testid="facility-inspection-record-inspector" aria-label="巡检人 id" placeholder="巡检人 id" value={recordForm.inspectorId} onChange={(event) => setRecordForm({ ...recordForm, inspectorId: event.target.value })} />
          <input data-testid="facility-inspection-record-note" aria-label="记录备注" placeholder="备注" value={recordForm.note} onChange={(event) => setRecordForm({ ...recordForm, note: event.target.value })} />
          <input data-testid="facility-inspection-record-abnormal-note" aria-label="异常说明" placeholder="异常说明" value={recordForm.abnormalNote} onChange={(event) => setRecordForm({ ...recordForm, abnormalNote: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={INSPECTION} data-testid="facility-inspection-record-submit" onClick={() => void submitInspectionRecord()}>提交记录</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-inspection-records-table">
            <thead><tr><th>记录</th><th>任务</th><th>结果</th><th>巡检人</th><th>异常</th><th>时间</th></tr></thead>
            <tbody>{inspectionRecords.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-inspection-record-${row.id}`} onClick={() => openInspectionRecordDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openInspectionRecordDetail(row); } }}><td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td>{row.taskId}</td><td>{zh(row.result)}</td><td>{row.inspectorId ?? "-"}</td><td>{row.abnormalNote ?? "-"}</td><td>{cnDate(row.recordedAt)}</td></tr>)}</tbody>
          </table>
        </div>
          </>) },
        ]}
      />
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
