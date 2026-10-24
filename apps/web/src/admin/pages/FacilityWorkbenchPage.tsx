// 文件版本：V0.02
import React from "react";
import { withBase } from "../../basePath";
import { AdminDataState } from "../components/AdminDataState";
import { CnDatePicker } from "../components/CnDatePicker";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { shortId } from "../format/ids";
import { loadSecondary, loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
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
  // HP-F02 双闸：主管／物业验收与提单方确认分别留存；confirmerId＝自动大型维修工单或员工手工建的关联设备报修单（裁决第 15 条）的提单确认承接人。
  source?: string | null; reporterType?: string | null; confirmerId?: string | null; verifierId?: string | null; verifiedAt?: string | null;
  reporterConfirmedAt?: string | null; reporterConfirmedBy?: string | null;
};
// ROOT-03 clients-R8 客人投诉附件（后端 CustomerComplaintService.MediaView）。
type CustomerMediaRow = { id: string; contentType: string; fileSize: number; createdAt: string };
const MEDIA_LABELS: Record<string, string> = { image: "照片", video: "视频", audio: "语音" };
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
// HP-F01 设备告警接警（BSM「楼宇运营」告警条 L1417）：接警单、逐次联系、值守安排、故障处置规则。
type FacilityAlarmRow = {
  id: string; equipmentId: string; workOrderId?: string; projectId?: string | null; alarmType: string; priority: string;
  status: string; responseDueAt?: string | null; escalationRound?: number; holderUserId?: string | null; takenOverAt?: string | null;
  uncovered?: boolean; appliedRuleId?: string | null; dispositionNote?: string | null; dispositionBy?: string | null;
};
type FacilityAlarmAttemptRow = { id: string; round: number; userId: string; dutyRole: string; channel: string; outcome: string; attemptedAt?: string | null };
type FacilityFaultRuleRow = {
  id: string; projectId: string; equipmentId?: string | null; equipmentType: string; faultScenario: string; handlingRequirement: string;
  status: string; specialistConfirmedAt?: string | null; vendorConfirmedAt?: string | null;
};
type FacilityAlarmDetailRow = { alarm: FacilityAlarmRow; attempts?: FacilityAlarmAttemptRow[]; takeovers?: { userId: string; kind: string; takenOverAt?: string | null }[]; rules?: FacilityFaultRuleRow[] };
type FacilityAlarmDutyRow = { id: string; projectId: string; shiftId: string; shiftUserId?: string | null; shiftStartAt?: string | null; shiftEndAt?: string | null; dutyRole: string; contactPhone: string; cancelledAt?: string | null };
type FacilityAlarmPolicyRow = { alarmType: string; priority: string; responseMinutes: number; active: boolean };
// HP-F03 楼宇设备报废单（BSM「楼宇机电设备」L1408）：审批、实物处置、款项三项事实分开。
type FacilityScrapRow = {
  id: string; equipmentId: string; equipmentName?: string | null; reason?: string | null; disposalMethod: string;
  approvalStatus: string; approvedBy?: string | null; approvedAt?: string | null; disposalStatus: string; disposalCompletedAt?: string | null;
  residualAmountCent?: number | null; residualCounterpartyId?: string | null; lossAmountCent?: number | null; moneyStatus: string; completed: boolean;
};
type InspectionRecordRow = { id: string; taskId: string; result: string; inspectorId?: string | null; abnormalNote?: string | null; recordedAt?: string | null };

const EQUIPMENT = ["facility.equipment.manage"];
const WORK_ORDER = ["facility.work_order.manage"];
const INSPECTION = ["facility.inspection.manage"];
const ALARM_MANAGE = ["facility.alarm.manage"];
const TAKE_OVER = ["facility.alarm.take_over"];
const RULE_CONFIRM = ["facility.fault_rule.confirm"];
const FINANCE_AR_AP = ["finance.ar_ap.manage"];
// 报废单三项事实中文（与上方同名键 pending 等语义不同，单列）。
const SCRAP_LABELS: Record<string, string> = {
  pending_approval: "待审批", approved: "已批准", rejected: "已驳回", pending: "待处置", completed: "已处置",
  pending_finance: "待财务登记", residual_pending_collection: "残值待收", settled: "已结清",
};
const scrapZh = (v?: string | null) => (v == null || v === "" ? "-" : (SCRAP_LABELS[v] ?? v));
const DUTY_ROLES = [["primary", "主接"], ["backup", "备用"], ["supervisor", "物业主管"], ["project_lead", "项目负责人"], ["specialist", "专业处置人员"]];
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
  pending_acceptance: "待验收", pending_review: "待回访", accepted: "已验收", closed: "已关闭",
  suspended: "已挂起", overdue: "已逾期",
  pending: "待巡检", rectifying: "整改中", rechecked: "已复检",
  confirmed: "已确认", completed: "已完成",
};
const zh = (v?: string | null) => (v == null || v === "" ? "-" : (LABELS[v] ?? v));
// 接警区枚举中文（与上方同名键如 supervisor/retired 语义不同，单列）。接听、已读都不等于明确接手。
const ALARM_LABELS: Record<string, string> = {
  ...Object.fromEntries(DUTY_ROLES), equipment_owner: "设备负责人",
  awaiting_takeover: "待明确接手", taken_over: "已明确接手",
  pending_confirmation: "待两方确认", active: "已生效", retired: "已停用",
  system_message: "系统消息", sms: "短信", voice: "语音",
  sent: "已发送", failed: "失败", placed: "已拨出", answered: "已接听（不等于接手）", throttled: "已限流", skipped_switch_off: "通道关闭未发",
  first: "首接", handover: "换班接手",
};
const alarmZh = (v?: string | null) => (v == null || v === "" ? "-" : (ALARM_LABELS[v] ?? zh(v)));
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

export function FacilityWorkbenchPage({ api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [equipment, setEquipment] = React.useState<FacilityEquipmentRow[]>([]);
  const [maintenanceDue, setMaintenanceDue] = React.useState<FacilityEquipmentRow[]>([]);
  const [workOrders, setWorkOrders] = React.useState<FacilityWorkOrderRow[]>([]);
  const [overdueWorkOrders, setOverdueWorkOrders] = React.useState<FacilityWorkOrderRow[]>([]);
  // 批2 派生视图副资源容错：维保到期/逾期工单是 equipment/work-orders 的服务端派生视图，可单独降级。
  const [maintenanceDueNotice, setMaintenanceDueNotice] = React.useState<string | null>(null);
  const [overdueNotice, setOverdueNotice] = React.useState<string | null>(null);
  const [inspectionTemplates, setInspectionTemplates] = React.useState<InspectionTemplateRow[]>([]);
  const [inspectionTasks, setInspectionTasks] = React.useState<InspectionTaskRow[]>([]);
  const [inspectionRecords, setInspectionRecords] = React.useState<InspectionRecordRow[]>([]);
  const [policyRows, setPolicyRows] = React.useState<FacilityWorkOrderPolicyRow[]>(DEFAULT_POLICIES);
  const [equipmentForm, setEquipmentForm] = React.useState({ name: "", equipmentType: "elevator", brand: "", responsibleUserId: "", maintenanceCycleDays: "90", spaceId: "" });
  const [statusForm, setStatusForm] = React.useState({ equipmentId: "", status: "normal", alarmSource: "manual", alarmNote: "", priority: "" });
  const [alarmRows, setAlarmRows] = React.useState<FacilityAlarmDetailRow[]>([]);
  const [myAlarms, setMyAlarms] = React.useState<FacilityAlarmRow[]>([]);
  const [dutyRows, setDutyRows] = React.useState<FacilityAlarmDutyRow[]>([]);
  const [faultRules, setFaultRules] = React.useState<FacilityFaultRuleRow[]>([]);
  const [alarmNotice, setAlarmNotice] = React.useState<string | null>(null);
  const [alarmPolicies, setAlarmPolicies] = React.useState<FacilityAlarmPolicyRow[]>([]);
  const [dutyForm, setDutyForm] = React.useState({ shiftId: "", dutyRole: "primary", contactPhone: "", cancelReason: "" });
  const [alarmPolicyForm, setAlarmPolicyForm] = React.useState({ alarmType: "elevator", priority: "urgent", responseMinutes: "" });
  const [ruleForm, setRuleForm] = React.useState({ projectId: "", equipmentId: "", equipmentType: "elevator", faultScenario: "", handlingRequirement: "" });
  const [dispositionForm, setDispositionForm] = React.useState({ alarmId: "", appliedRuleId: "", judgement: "" });
  const [scrapRows, setScrapRows] = React.useState<FacilityScrapRow[]>([]);
  const [scrapNotice, setScrapNotice] = React.useState<string | null>(null);
  const [scrapForm, setScrapForm] = React.useState({ equipmentId: "", reason: "", disposalMethod: "", finalApproverId: "" });
  const [scrapDisposal, setScrapDisposal] = React.useState<{ scrapId: string; note: string; proofs: { id: string; name: string }[] }>({ scrapId: "", note: "", proofs: [] });
  const [scrapFinance, setScrapFinance] = React.useState({ scrapId: "", residual: "", counterparty: "", loss: "" });
  const [workOrderForm, setWorkOrderForm] = React.useState({ workOrderType: "repair", priority: "normal", title: "", equipmentId: "", spaceId: "", reporterId: "", reporterType: "staff", source: "platform" });
  const [workOrderAction, setWorkOrderAction] = React.useState({ assigneeId: "", resolution: "现场处理完成", satisfaction: "5", note: "", confirmerId: "", reassignReason: "", customerFeedback: "" });
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

  // 无权限副资源不请求（根因修）：超期工单端点需 facility.work_order.manage，页面基础权限 facility.equipment.manage 不含之。
  const canWorkOrderManage = hasPermissions(effectivePermissions, ["facility.work_order.manage"]);
  const canAlarmManage = hasPermissions(effectivePermissions, ALARM_MANAGE);
  const canTakeOver = hasPermissions(effectivePermissions, TAKE_OVER);
  const { loading, error, reload } = useAdminResource(
    async () => {
      const [alarmList, mineList, dutyList, ruleList] = await Promise.all([
        loadSecondaryIfPermitted(canAlarmManage, () => api.get<FacilityAlarmDetailRow[]>("/api/v1/facility/alarms"), [] as FacilityAlarmDetailRow[]),
        loadSecondaryIfPermitted(canTakeOver, () => api.get<FacilityAlarmRow[]>("/api/v1/facility/alarms/mine"), [] as FacilityAlarmRow[]),
        loadSecondaryIfPermitted(canAlarmManage, () => api.get<FacilityAlarmDutyRow[]>("/api/v1/facility/alarm-duties"), [] as FacilityAlarmDutyRow[]),
        loadSecondaryIfPermitted(canAlarmManage, () => api.get<FacilityFaultRuleRow[]>("/api/v1/facility/fault-rules"), [] as FacilityFaultRuleRow[]),
      ]);
      setAlarmRows(alarmList.data ?? []);
      setMyAlarms(mineList.data ?? []);
      setDutyRows(dutyList.data ?? []);
      setFaultRules(ruleList.data ?? []);
      setAlarmNotice(alarmList.notice ?? mineList.notice ?? dutyList.notice ?? ruleList.notice);
      const [equipmentRows, dueRows, orderRows, overdueRows, templateRows, taskRows, scrapList] = await Promise.all([
        api.get<FacilityEquipmentRow[]>("/api/v1/facility/equipment"),
        loadSecondary(() => api.get<FacilityEquipmentRow[]>("/api/v1/facility/equipment/maintenance-due"), [] as FacilityEquipmentRow[]),
        api.get<FacilityWorkOrderRow[]>("/api/v1/facility/work-orders"),
        loadSecondaryIfPermitted(canWorkOrderManage, () => api.get<FacilityWorkOrderRow[]>("/api/v1/facility/work-orders/overdue"), [] as FacilityWorkOrderRow[]),
        api.get<InspectionTemplateRow[]>("/api/v1/facility/inspections/templates"),
        api.get<InspectionTaskRow[]>("/api/v1/facility/inspections/tasks"),
        loadSecondary(() => api.get<FacilityScrapRow[]>("/api/v1/facility/equipment-scraps"), [] as FacilityScrapRow[]),
      ]);
      setScrapRows(scrapList.data ?? []);
      setScrapNotice(scrapList.notice);
      setEquipment(equipmentRows ?? []);
      setMaintenanceDue(dueRows.data);
      setMaintenanceDueNotice(dueRows.notice);
      setWorkOrders(orderRows ?? []);
      setOverdueWorkOrders(overdueRows.data);
      setOverdueNotice(overdueRows.notice);
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
    // HP-F01（2026-10-21 操作员裁决补充第 1 条）：告警/故障由上报人当场选优先级（决定接警响应时限）。
    const needsPriority = statusForm.status === "alarm" || statusForm.status === "fault";
    if (needsPriority && !statusForm.priority) {
      setGlobalError("告警或故障上报须当场选择优先级（紧急 / 高 / 普通）");
      return;
    }
    await api.post(`/api/v1/facility/equipment/${encodeURIComponent(statusForm.equipmentId.trim())}/status`, {
      status: statusForm.status,
      alarmSource: statusForm.status === "alarm" ? statusForm.alarmSource : undefined,
      alarmNote: statusForm.alarmNote.trim() || undefined,
      priority: needsPriority ? statusForm.priority : undefined,
    });
    setActionResult({ action: "facility_equipment_status_changed", equipmentId: statusForm.equipmentId.trim(), status: statusForm.status });
    await reload();
  }

  // ---- HP-F01 设备接警 ----
  async function takeOverAlarm(id: string) {
    await api.post(`/api/v1/facility/alarms/${encodeURIComponent(id)}/take-over`, {});
    setActionResult({ action: "facility_alarm_taken_over", alarmId: id });
    await reload();
  }

  async function recordDisposition() {
    const alarmId = dispositionForm.alarmId.trim();
    const appliedRuleId = dispositionForm.appliedRuleId.trim();
    const judgement = dispositionForm.judgement.trim();
    if (!alarmId || (!appliedRuleId && !judgement)) {
      setGlobalError("记录处置要求需选择告警，并填写所按的故障处置规则或专业处置判定");
      return;
    }
    await api.post(`/api/v1/facility/alarms/${encodeURIComponent(alarmId)}/disposition`, {
      appliedRuleId: appliedRuleId || undefined,
      judgement: judgement || undefined,
    });
    setActionResult({ action: "facility_alarm_disposition_recorded", alarmId });
    setDispositionForm({ alarmId: "", appliedRuleId: "", judgement: "" });
    await reload();
  }

  async function assignDuty() {
    if (!dutyForm.shiftId.trim() || !dutyForm.contactPhone.trim()) {
      setGlobalError("接警值守安排需填写班次与联系电话");
      return;
    }
    await api.post("/api/v1/facility/alarm-duties", {
      shiftId: dutyForm.shiftId.trim(),
      dutyRole: dutyForm.dutyRole,
      contactPhone: dutyForm.contactPhone.trim(),
    });
    setActionResult({ action: "facility_alarm_duty_assigned", shiftId: dutyForm.shiftId.trim(), dutyRole: dutyForm.dutyRole });
    setDutyForm({ ...dutyForm, shiftId: "", contactPhone: "" });
    await reload();
  }

  async function cancelDuty(id: string) {
    if (!dutyForm.cancelReason.trim()) {
      setGlobalError("撤销接警值守安排须填写原因");
      return;
    }
    await api.post(`/api/v1/facility/alarm-duties/${encodeURIComponent(id)}/cancel`, { reason: dutyForm.cancelReason.trim() });
    setActionResult({ action: "facility_alarm_duty_cancelled", dutyId: id });
    await reload();
  }

  async function saveAlarmPolicy() {
    const responseMinutes = positiveInt(alarmPolicyForm.responseMinutes);
    if (!responseMinutes) {
      setGlobalError("响应时限须填写正整数分钟");
      return;
    }
    const saved = await api.post<FacilityAlarmPolicyRow>("/api/v1/facility/alarms/policies", {
      alarmType: alarmPolicyForm.alarmType, priority: alarmPolicyForm.priority, responseMinutes, active: true,
    });
    setAlarmPolicies((current) => [...current.filter((row) => row.alarmType !== saved.alarmType || row.priority !== saved.priority), saved]);
    setActionResult({ action: "facility_alarm_policy_saved", alarmType: saved.alarmType, priority: saved.priority });
  }

  async function createFaultRule() {
    if ((!ruleForm.equipmentId.trim() && !ruleForm.projectId.trim()) || !ruleForm.faultScenario.trim() || !ruleForm.handlingRequirement.trim()) {
      setGlobalError("故障处置规则需填写项目（或设备）、故障场景与处置要求");
      return;
    }
    await api.post("/api/v1/facility/fault-rules", {
      projectId: ruleForm.projectId.trim() || undefined,
      equipmentId: ruleForm.equipmentId.trim() || undefined,
      equipmentType: ruleForm.equipmentType,
      faultScenario: ruleForm.faultScenario.trim(),
      handlingRequirement: ruleForm.handlingRequirement.trim(),
    });
    setActionResult({ action: "facility_fault_rule_created", faultScenario: ruleForm.faultScenario.trim() });
    setRuleForm({ ...ruleForm, faultScenario: "", handlingRequirement: "" });
    await reload();
  }

  async function faultRuleAction(id: string, action: "confirm" | "retire") {
    await api.post(`/api/v1/facility/fault-rules/${encodeURIComponent(id)}/${action}`, {});
    setActionResult({ action: `facility_fault_rule_${action}`, ruleId: id });
    await reload();
  }

  function openAlarmDetail(row: FacilityAlarmDetailRow) {
    const a = row.alarm;
    setDetail({
      title: `接警明细 · ${a.id}`,
      fields: [
        { label: "设备", value: a.equipmentId },
        { label: "告警类型 / 优先级", value: `${zh(a.alarmType)} / ${zh(a.priority)}` },
        { label: "状态", value: alarmZh(a.status) },
        { label: "响应时限", value: cnDate(a.responseDueAt) },
        { label: "升级轮次", value: String(a.escalationRound ?? 0) },
        { label: "接手人", value: userTrace(a.holderUserId) },
        { label: "故障处置规则", value: a.uncovered ? "未覆盖（转当班专业处置人员判定）" : (row.rules ?? []).map((r) => `${r.faultScenario}：${r.handlingRequirement}`).join("；") || "-" },
        { label: "处置要求", value: a.dispositionNote ?? (a.appliedRuleId ? `按规则 ${a.appliedRuleId}` : "-") },
        { label: "联系记录", value: (row.attempts ?? []).map((t) => `第${t.round}轮 ${alarmZh(t.dutyRole)} ${t.userId} ${alarmZh(t.channel)}：${alarmZh(t.outcome)}`).join("；") || "-" },
        { label: "接手记录", value: (row.takeovers ?? []).map((t) => `${alarmZh(t.kind)} ${t.userId} ${cnDate(t.takenOverAt)}`).join("；") || "-" },
      ],
    });
  }

  // ---- HP-F03 设备报废：申请走所有权主体审批；实物处置与款项分别登记 ----
  async function requestScrap() {
    if (!scrapForm.reason.trim() || !scrapForm.disposalMethod.trim()) {
      setGlobalError("报废申请须填写报废原因与处置方式");
      return;
    }
    await api.post(`/api/v1/facility/equipment/${encodeURIComponent(scrapForm.equipmentId)}/scrap-requests`, {
      reason: scrapForm.reason.trim(),
      disposalMethod: scrapForm.disposalMethod.trim(),
      finalApproverId: scrapForm.finalApproverId.trim() || undefined,
    });
    setActionResult({ action: "facility_equipment_scrap_requested", equipmentId: scrapForm.equipmentId });
    setScrapForm({ equipmentId: "", reason: "", disposalMethod: "", finalApproverId: "" });
    await reload();
  }

  async function uploadScrapEvidence(files: FileList | null) {
    if (!files?.length || !api.postForm) return;
    for (const file of Array.from(files)) {
      const form = new FormData();
      form.append("file", file); form.append("business_type", "facility_equipment_scrap");
      form.append("business_id", scrapDisposal.scrapId); form.append("attachment_role", "other");
      const proof = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", form);
      setScrapDisposal((current) => ({ ...current, proofs: [...current.proofs, { id: proof.id, name: file.name }] }));
    }
  }

  async function completeScrapDisposal() {
    if (!scrapDisposal.proofs.length) {
      setGlobalError("登记实际处置须先上传处置凭证");
      return;
    }
    await api.post(`/api/v1/facility/equipment-scraps/${encodeURIComponent(scrapDisposal.scrapId)}/disposal`, {
      evidenceAttachmentIds: scrapDisposal.proofs.map((p) => p.id),
      disposalNote: scrapDisposal.note.trim() || undefined,
    });
    setActionResult({ action: "facility_equipment_scrap_disposal_completed", scrapId: scrapDisposal.scrapId });
    setScrapDisposal({ scrapId: "", note: "", proofs: [] });
    await reload();
  }

  async function recordScrapFinance() {
    const residualAmountCent = Math.round(Number(scrapFinance.residual || "0") * 100);
    const lossAmountCent = Math.round(Number(scrapFinance.loss || "0") * 100);
    if (!Number.isFinite(residualAmountCent) || !Number.isFinite(lossAmountCent) || residualAmountCent < 0 || lossAmountCent < 0) {
      setGlobalError("残值与损失须填写不小于 0 的金额（元）");
      return;
    }
    if (residualAmountCent > 0 && !scrapFinance.counterparty.trim()) {
      setGlobalError("有残值须填写回收方");
      return;
    }
    await api.post(`/api/v1/facility/equipment-scraps/${encodeURIComponent(scrapFinance.scrapId)}/finance`, {
      residualAmountCent,
      residualCounterpartyId: scrapFinance.counterparty.trim() || undefined,
      lossAmountCent,
    });
    setActionResult({ action: "facility_equipment_scrap_finance_recorded", scrapId: scrapFinance.scrapId });
    setScrapFinance({ scrapId: "", residual: "", counterparty: "", loss: "" });
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

  // 第十二批操作员裁决第 9 条「补转派」：已派单或处理中改派他人，处理时限不重算；原因必填并留痕。
  async function reassignWorkOrder(id: string) {
    if (!workOrderAction.assigneeId.trim() || !workOrderAction.reassignReason.trim()) {
      setGlobalError("转派需填写新处理人 id 和转派原因");
      return;
    }
    await api.post(`/api/v1/facility/work-orders/${encodeURIComponent(id)}/reassign`, {
      assigneeId: workOrderAction.assigneeId.trim(),
      reason: workOrderAction.reassignReason.trim(),
    });
    setActionResult({ action: "facility_work_order_reassigned", workOrderId: id });
    await reload();
  }

  async function workOrderActionPost(id: string, action: "start" | "complete" | "accept" | "review" | "suspend" | "resume" | "rework", source?: string | null) {
    // 第十二批操作员裁决第 2 条：验收或提单确认不通过，须填不通过原因后退回原维修人返工。
    if (action === "rework" && !workOrderAction.note.trim()) {
      setGlobalError("退回返工须在备注里填写不通过原因");
      return;
    }
    // ROOT-03 clients-R8（BSM 第 1379 行「客户关怀部记录问题解决方案及客人反馈后可以关闭」）：关闭客人投诉须填客人反馈；满意度由客人本人评（R26）。
    const customerComplaint = source === "customer";
    const customerFeedback = workOrderAction.customerFeedback.trim();
    if (customerComplaint && (action === "accept" || action === "review") && !customerFeedback) {
      setGlobalError("关闭客人投诉须填写客人反馈");
      return;
    }
    await api.post(`/api/v1/facility/work-orders/${encodeURIComponent(id)}/${action}`, {
      resolution: action === "complete" ? workOrderAction.resolution.trim() || "已处理" : undefined,
      satisfaction: !customerComplaint && (action === "accept" || action === "review") ? positiveInt(workOrderAction.satisfaction) : undefined,
      note: workOrderAction.note.trim() || undefined,
      customerFeedback: customerComplaint && customerFeedback ? customerFeedback : undefined,
    });
    setActionResult({ action: `facility_work_order_${action}`, workOrderId: id });
    await reload();
  }

  // HP-F02：自动生成的大型维修工单，设备责任人不在岗或本人维修时由物业主管指定提单确认人（留痕）；员工手工建的关联设备报修单，
  // 提单人就是本次维修人时同此（裁决第 15 条）。
  async function designateConfirmer(id: string) {
    if (!workOrderAction.confirmerId.trim()) {
      setGlobalError("指定提单确认人需填写确认人 id");
      return;
    }
    await api.post(`/api/v1/facility/work-orders/${encodeURIComponent(id)}/confirmer`, {
      confirmerId: workOrderAction.confirmerId.trim(),
      note: workOrderAction.note.trim() || undefined,
    });
    setActionResult({ action: "facility_work_order_confirmer_designated", workOrderId: id });
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
        { label: "主管验收人 / 时间", value: <>{userTrace(row.verifierId)} {cnDate(row.verifiedAt)}</> },
        { label: "提单确认承接人", value: userTrace(row.confirmerId) },
        { label: "提单确认人 / 时间", value: <>{userTrace(row.reporterConfirmedBy)} {cnDate(row.reporterConfirmedAt)}</> },
        { label: "满意度", value: row.satisfaction != null ? `${row.satisfaction} 分` : "-" },
      ],
    });
    // ROOT-03 clients-R8：客人投诉附件（照片、视频、语音）按工单查看权限受控查看。
    if (row.source === "customer") {
      void api.get<CustomerMediaRow[]>(`/api/v1/facility/work-orders/${encodeURIComponent(row.id)}/customer-media`).then((media) => {
        const links = (media ?? []).map((m, index) => (
          <a key={m.id} href={withBase(`/api/v1/facility/work-orders/${encodeURIComponent(row.id)}/customer-media/${encodeURIComponent(m.id)}/file`)} target="_blank" rel="noreferrer">
            {MEDIA_LABELS[m.contentType.split("/")[0] ?? ""] ?? "附件"} {index + 1}{" "}
          </a>
        ));
        setDetail((current) => current && current.title.endsWith(row.code ?? row.id)
          ? { ...current, fields: [...current.fields, { label: "客人附件", value: links.length > 0 ? <span data-testid="facility-work-order-customer-media">{links}</span> : "无" }] }
          : current);
      }).catch(() => undefined);
    }
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
            <thead><tr><th>id</th><th>名称</th><th>分类</th><th>状态</th><th>品牌/责任人</th><th>维保到期</th><th>告警</th><th>操作</th><th>详情</th></tr></thead>
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
                  {row.status !== "retired" ? <PermissionButton permissionsState={effectivePermissions} permissions={EQUIPMENT} data-testid={`facility-equipment-scrap-open-${row.id}`} onClick={() => setScrapForm({ equipmentId: row.id, reason: "", disposalMethod: "", finalApproverId: "" })}>申请报废</PermissionButton> : null}
                </td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看设备详情" onClick={(event) => { event.stopPropagation(); openEquipmentDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
        </div>
        {scrapForm.equipmentId ? (
          <div className="admin-inline-form" data-testid="facility-scrap-request-form">
            <span>申请报废 {scrapForm.equipmentId}（经所有权主体审批后才置报废）</span>
            <input data-testid="facility-scrap-reason" aria-label="报废原因" placeholder="报废原因" value={scrapForm.reason} onChange={(event) => setScrapForm({ ...scrapForm, reason: event.target.value })} />
            <input data-testid="facility-scrap-method" aria-label="处置方式" placeholder="处置方式" value={scrapForm.disposalMethod} onChange={(event) => setScrapForm({ ...scrapForm, disposalMethod: event.target.value })} />
            <input data-testid="facility-scrap-approver" aria-label="最终审批人" placeholder="最终审批人 id（可空）" value={scrapForm.finalApproverId} onChange={(event) => setScrapForm({ ...scrapForm, finalApproverId: event.target.value })} />
            <PermissionButton permissionsState={effectivePermissions} permissions={EQUIPMENT} data-testid="facility-scrap-submit" onClick={() => void requestScrap()}>提交报废申请</PermissionButton>
            <button type="button" onClick={() => setScrapForm({ equipmentId: "", reason: "", disposalMethod: "", finalApproverId: "" })}>取消</button>
          </div>
        ) : null}
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
          <select data-testid="facility-equipment-status" aria-label="设备状态" value={statusForm.status} onChange={(event) => setStatusForm({ ...statusForm, status: event.target.value })}>{EQUIPMENT_STATUSES.filter(([value]) => value !== "retired").map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <select data-testid="facility-equipment-alarm-source" aria-label="告警源" value={statusForm.alarmSource} onChange={(event) => setStatusForm({ ...statusForm, alarmSource: event.target.value })}><option value="manual">手工</option><option value="iot">物联网</option></select>
          <select data-testid="facility-equipment-status-priority" aria-label="告警优先级" value={statusForm.priority} onChange={(event) => setStatusForm({ ...statusForm, priority: event.target.value })}><option value="">告警/故障须选优先级</option>{PRIORITIES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input data-testid="facility-equipment-alarm-note" aria-label="告警说明" placeholder="告警说明" value={statusForm.alarmNote} onChange={(event) => setStatusForm({ ...statusForm, alarmNote: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={EQUIPMENT} data-testid="facility-equipment-status-change" onClick={() => void changeEquipmentStatus()}>状态变更/告警上报</PermissionButton>
        </div>
        <h3>设备维保到期清单</h3>
        {maintenanceDueNotice ? <div className="admin-state" data-testid="facility-maintenance-due-degraded">{maintenanceDueNotice}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-maintenance-due-table">
            <thead><tr><th>设备</th><th>分类</th><th>责任人</th><th>到期日</th><th>详情</th></tr></thead>
            <tbody>{maintenanceDue.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-maintenance-due-${row.id}`} onClick={() => openMaintenanceDueDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openMaintenanceDueDetail(row); } }}><td>{row.name}</td><td>{zh(row.equipmentType)}</td><td>{row.responsibleUserId ?? "-"}</td><td>{cnDate(row.nextMaintenanceDue)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看维保到期详情" onClick={(event) => { event.stopPropagation(); openMaintenanceDueDetail(row); }}>查看详情</button></td></tr>)}</tbody>
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
          <input data-testid="facility-work-order-note" aria-label="操作备注" placeholder="备注（退回返工填不通过原因）" value={workOrderAction.note} onChange={(event) => setWorkOrderAction({ ...workOrderAction, note: event.target.value })} />
          <input data-testid="facility-work-order-customer-feedback" aria-label="客人反馈" placeholder="客人反馈（关闭客人投诉必填）" value={workOrderAction.customerFeedback} onChange={(event) => setWorkOrderAction({ ...workOrderAction, customerFeedback: event.target.value })} />
          <input data-testid="facility-work-order-confirmer" aria-label="提单确认人" placeholder="指定提单确认人 id" value={workOrderAction.confirmerId} onChange={(event) => setWorkOrderAction({ ...workOrderAction, confirmerId: event.target.value })} />
          <input data-testid="facility-work-order-reassign-reason" aria-label="转派原因" placeholder="转派原因（转派必填，新处理人填在处理人 id）" value={workOrderAction.reassignReason} onChange={(event) => setWorkOrderAction({ ...workOrderAction, reassignReason: event.target.value })} />
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-work-orders-table">
            <thead><tr><th>编号</th><th>类型/优先级</th><th>标题</th><th>状态</th><th>处理人</th><th>满意度</th><th>操作</th><th>详情</th></tr></thead>
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
                    if (row.status === "processing") wo.push({ key: "complete", label: "完成", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "complete", row.source), testId: `facility-work-order-complete-${row.id}` });
                    if (row.status === "assigned" || row.status === "processing") wo.push({ key: "reassign", label: "转派", permissions: WORK_ORDER, onClick: () => void reassignWorkOrder(row.id), testId: `facility-work-order-reassign-${row.id}` });
                    if (row.status === "pending_acceptance") wo.push({ key: "accept", label: "验收", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "accept", row.source), testId: `facility-work-order-accept-${row.id}` });
                    if (row.status === "pending_review") wo.push({ key: "review", label: "回访", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "review", row.source), testId: `facility-work-order-review-${row.id}` });
                    // HP-F02 双闸（裁决第 12 条：关联设备的报修才两道）：已验收或待验收且提单方未确认 → 提单确认；任一方不通过 → 退回返工。
                    const dualPending = row.workOrderType === "repair" && !!row.equipmentId && (row.status === "pending_acceptance" || row.status === "accepted");
                    // 告警单与裁决第 15 条员工手工建的关联设备报修单（提单员工就是承接人）：只给承接人本人且不是本次维修人时显示「提单确认」，
                    // 持 manage 的旁人不能代点；承接人本人负责维修时由物业主管「指定确认人」。
                    const staffReported = row.workOrderType === "repair" && !!row.equipmentId && row.reporterType === "staff";
                    const confirmerOnly = row.source === "equipment_alarm" || staffReported;
                    const confirmerSelf = row.confirmerId === user?.id && row.assigneeId !== user?.id;
                    if (dualPending && !row.reporterConfirmedAt && (!confirmerOnly || confirmerSelf)) wo.push({ key: "confirm", label: "提单确认", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "review"), testId: `facility-work-order-review-${row.id}` });
                    // 裁决第 16 条「只许承接人本人」：这类单主管已验收后只剩提单确认这一道，「退回返工」也只给承接人本人且其不是本次维修人。
                    if (dualPending && (row.status !== "accepted" || !confirmerOnly || confirmerSelf)) wo.push({ key: "rework", label: "退回返工", permissions: WORK_ORDER, danger: true, onClick: () => void workOrderActionPost(row.id, "rework"), testId: `facility-work-order-rework-${row.id}` });
                    if ((row.source === "equipment_alarm" || staffReported) && row.status !== "closed" && !row.reporterConfirmedAt) wo.push({ key: "confirmer", label: "指定确认人", permissions: WORK_ORDER, onClick: () => void designateConfirmer(row.id), testId: `facility-work-order-designate-confirmer-${row.id}` });
                    if (row.status !== "closed" && row.status !== "suspended") wo.push({ key: "suspend", label: "挂起", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "suspend"), testId: `facility-work-order-suspend-${row.id}` });
                    if (row.status === "suspended") wo.push({ key: "resume", label: "恢复", permissions: WORK_ORDER, onClick: () => void workOrderActionPost(row.id, "resume"), testId: `facility-work-order-resume-${row.id}` });
                    return <RowActions actions={wo} permissionsState={effectivePermissions} testId={`facility-work-order-actions-${row.id}`} />;
                  })()}
                </td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看工单详情" onClick={(event) => { event.stopPropagation(); openWorkOrderDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
        </div>
        <h3>逾期工单</h3>
        <div className="admin-inline-form" data-testid="facility-work-order-overdue-form">
          <PermissionButton permissionsState={effectivePermissions} permissions={WORK_ORDER} data-testid="facility-work-order-mark-overdue" onClick={() => void markOverdue()}>标记逾期</PermissionButton>
        </div>
        {overdueNotice ? <div className="admin-state" data-testid="facility-work-orders-overdue-degraded">{overdueNotice}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-work-orders-overdue-table">
            <thead><tr><th>编号</th><th>类型/优先级</th><th>标题</th><th>状态</th><th>处理人</th><th>详情</th></tr></thead>
            <tbody>{overdueWorkOrders.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-work-order-overdue-${row.id}`} onClick={() => openOverdueWorkOrderDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openOverdueWorkOrderDetail(row); } }}><td>{row.code ?? row.id}</td><td>{zh(row.workOrderType)} / {zh(row.priority)}</td><td>{row.title}</td><td>{statusBadge(row.status)} · 已逾期</td><td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.assigneeId}>{row.assigneeId ?? "-"}</TraceLink></td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看逾期工单详情" onClick={(event) => { event.stopPropagation(); openOverdueWorkOrderDetail(row); }}>查看详情</button></td></tr>)}</tbody>
          </table>
        </div>
        <h3>SLA 策略表</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-work-order-policies-table">
            <thead><tr><th>类型</th><th>优先级</th><th>响应/处理分钟</th><th>验收</th><th>状态</th><th>详情</th></tr></thead>
            <tbody>{policyRows.map((row) => <tr key={`${row.workOrderType}-${row.priority}`} className="admin-row-clickable" tabIndex={0} data-testid={`facility-work-order-policy-${row.workOrderType}-${row.priority}`} onClick={() => openPolicyDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openPolicyDetail(row); } }}><td>{zh(row.workOrderType)}</td><td>{zh(row.priority)}</td><td>{row.responseMinutes} / {row.handleMinutes}</td><td>{row.requiresAcceptance ? zh(row.acceptanceBy) : "无需"}</td><td>{row.active ? "启用" : "停用"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看SLA 策略详情" onClick={(event) => { event.stopPropagation(); openPolicyDetail(row); }}>查看详情</button></td></tr>)}</tbody>
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
            <thead><tr><th>id</th><th>模板</th><th>类型</th><th>周期</th><th>异常模式</th><th>状态</th><th>详情</th></tr></thead>
            <tbody>{inspectionTemplates.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-inspection-template-${row.id}`} onClick={() => openInspectionTemplateDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openInspectionTemplateDetail(row); } }}><td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td>{row.name}</td><td>{zh(row.inspectionType)}</td><td>{row.cycleDays ?? "-"}</td><td>{zh(row.abnormalMode)}</td><td>{row.active ? "启用" : "停用"}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看巡检模板详情" onClick={(event) => { event.stopPropagation(); openInspectionTemplateDetail(row); }}>查看详情</button></td></tr>)}</tbody>
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
            <thead><tr><th>id</th><th>标题</th><th>类型/排程</th><th>状态</th><th>人员/日期</th><th>工单</th><th>操作</th><th>详情</th></tr></thead>
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
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看巡检任务详情" onClick={(event) => { event.stopPropagation(); openInspectionTaskDetail(row); }}>查看详情</button></td></tr>
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
            <thead><tr><th>记录</th><th>任务</th><th>结果</th><th>巡检人</th><th>异常</th><th>时间</th><th>详情</th></tr></thead>
            <tbody>{inspectionRecords.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-inspection-record-${row.id}`} onClick={() => openInspectionRecordDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openInspectionRecordDetail(row); } }}><td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td>{row.taskId}</td><td>{zh(row.result)}</td><td>{row.inspectorId ?? "-"}</td><td>{row.abnormalNote ?? "-"}</td><td>{cnDate(row.recordedAt)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看巡检记录详情" onClick={(event) => { event.stopPropagation(); openInspectionRecordDetail(row); }}>查看详情</button></td></tr>)}</tbody>
          </table>
        </div>
          </>) },
          { key: "alarms", label: "④ 设备接警", forceRender: true, children: (<>
        <h3>我的接警</h3>
        <div className="admin-preview" data-testid="facility-alarm-guide">当班且承担接警任务的人员须点「已收到并接手」才算明确接手；消息送达、已读、语音拨通或接听都不算。超过响应时限无人接手的，系统向此刻当班的备用、物业主管、项目负责人升级，不呼叫休息人员；换班时未结告警须下一班再次明确接手。</div>
        {alarmNotice ? <div className="admin-state" data-testid="facility-alarms-degraded">{alarmNotice}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-my-alarms-table">
            <thead><tr><th>设备</th><th>告警类型/优先级</th><th>状态</th><th>响应时限</th><th>故障处置规则</th><th>操作</th></tr></thead>
            <tbody>{myAlarms.map((row) => (
              <tr key={row.id} data-testid={`facility-my-alarm-${row.id}`}>
                <td>{row.equipmentId}</td><td>{zh(row.alarmType)} / {zh(row.priority)}</td><td>{alarmZh(row.status)}</td><td>{cnDate(row.responseDueAt)}</td>
                <td>{row.uncovered ? "未覆盖，待专业处置人员判定" : "按已确认规则处理"}</td>
                <td>
                  {row.status === "awaiting_takeover" ? <PermissionButton permissionsState={effectivePermissions} permissions={TAKE_OVER} data-testid={`facility-alarm-take-over-${row.id}`} onClick={() => void takeOverAlarm(row.id)}>已收到并接手</PermissionButton> : null}
                  {!row.dispositionNote && !row.appliedRuleId ? <PermissionButton permissionsState={effectivePermissions} permissions={TAKE_OVER} data-testid={`facility-alarm-dispose-${row.id}`} onClick={() => setDispositionForm({ ...dispositionForm, alarmId: row.id })}>记录处置要求</PermissionButton> : null}
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="facility-alarm-disposition-form">
          <input data-testid="facility-alarm-disposition-alarm" aria-label="接警单" placeholder="接警单 id" value={dispositionForm.alarmId} onChange={(event) => setDispositionForm({ ...dispositionForm, alarmId: event.target.value })} />
          <input data-testid="facility-alarm-disposition-rule" aria-label="所按故障处置规则" placeholder="所按规则 id（按已确认规则处理时填）" value={dispositionForm.appliedRuleId} onChange={(event) => setDispositionForm({ ...dispositionForm, appliedRuleId: event.target.value })} />
          <input data-testid="facility-alarm-disposition-judgement" aria-label="专业处置判定" placeholder="专业处置判定（如停用或继续使用）" value={dispositionForm.judgement} onChange={(event) => setDispositionForm({ ...dispositionForm, judgement: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={TAKE_OVER} data-testid="facility-alarm-disposition-submit" onClick={() => void recordDisposition()}>记录处置要求（不远程停机）</PermissionButton>
        </div>
        <h3>接警记录</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-alarms-table">
            <thead><tr><th>设备</th><th>告警类型/优先级</th><th>状态</th><th>响应时限/升级轮次</th><th>接手人</th><th>联系次数</th><th>故障处置规则</th><th>详情</th></tr></thead>
            <tbody>{alarmRows.map((row) => (
              <tr key={row.alarm.id} className="admin-row-clickable" tabIndex={0} data-testid={`facility-alarm-${row.alarm.id}`} onClick={() => openAlarmDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openAlarmDetail(row); } }}>
                <td>{row.alarm.equipmentId}</td><td>{zh(row.alarm.alarmType)} / {zh(row.alarm.priority)}</td><td>{alarmZh(row.alarm.status)}</td>
                <td>{cnDate(row.alarm.responseDueAt)} / 第 {row.alarm.escalationRound ?? 0} 轮</td>
                <td onClick={(event) => event.stopPropagation()}>{userTrace(row.alarm.holderUserId)}</td>
                <td>{(row.attempts ?? []).length}</td>
                <td>{row.alarm.uncovered ? "未覆盖" : `${(row.rules ?? []).length} 条`}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看接警详情" onClick={(event) => { event.stopPropagation(); openAlarmDetail(row); }}>查看详情</button></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <h3>接警值守安排</h3>
        <div className="admin-inline-form" data-testid="facility-alarm-duty-form">
          <input data-testid="facility-alarm-duty-shift" aria-label="班次" placeholder="已发布班次 id" value={dutyForm.shiftId} onChange={(event) => setDutyForm({ ...dutyForm, shiftId: event.target.value })} />
          <select data-testid="facility-alarm-duty-role" aria-label="接警角色" value={dutyForm.dutyRole} onChange={(event) => setDutyForm({ ...dutyForm, dutyRole: event.target.value })}>{DUTY_ROLES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input data-testid="facility-alarm-duty-phone" aria-label="联系电话" placeholder="联系电话" value={dutyForm.contactPhone} onChange={(event) => setDutyForm({ ...dutyForm, contactPhone: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ALARM_MANAGE} data-testid="facility-alarm-duty-assign" onClick={() => void assignDuty()}>安排接警值守</PermissionButton>
          <input data-testid="facility-alarm-duty-cancel-reason" aria-label="撤销原因" placeholder="撤销原因" value={dutyForm.cancelReason} onChange={(event) => setDutyForm({ ...dutyForm, cancelReason: event.target.value })} />
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-alarm-duties-table">
            <thead><tr><th>班次</th><th>班次人员</th><th>时段</th><th>接警角色</th><th>联系电话</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>{dutyRows.map((row) => (
              <tr key={row.id} data-testid={`facility-alarm-duty-${row.id}`}>
                <td>{row.shiftId}</td><td>{userTrace(row.shiftUserId)}</td><td>{cnDate(row.shiftStartAt)} ~ {cnDate(row.shiftEndAt)}</td>
                <td>{alarmZh(row.dutyRole)}</td><td>{row.contactPhone}</td><td>{row.cancelledAt ? "已撤销" : "有效"}</td>
                <td>{row.cancelledAt ? null : <PermissionButton permissionsState={effectivePermissions} permissions={ALARM_MANAGE} data-testid={`facility-alarm-duty-cancel-${row.id}`} onClick={() => void cancelDuty(row.id)}>撤销</PermissionButton>}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <h3>告警响应时限（告警类型 × 优先级）</h3>
        <div className="admin-inline-form" data-testid="facility-alarm-policy-form">
          <select data-testid="facility-alarm-policy-type" aria-label="告警类型" value={alarmPolicyForm.alarmType} onChange={(event) => setAlarmPolicyForm({ ...alarmPolicyForm, alarmType: event.target.value })}>{EQUIPMENT_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <select data-testid="facility-alarm-policy-priority" aria-label="告警优先级" value={alarmPolicyForm.priority} onChange={(event) => setAlarmPolicyForm({ ...alarmPolicyForm, priority: event.target.value })}>{PRIORITIES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input data-testid="facility-alarm-policy-minutes" aria-label="响应时限分钟" type="number" min="1" placeholder="响应时限（分钟）" value={alarmPolicyForm.responseMinutes} onChange={(event) => setAlarmPolicyForm({ ...alarmPolicyForm, responseMinutes: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ALARM_MANAGE} data-testid="facility-alarm-policy-save" onClick={() => void saveAlarmPolicy()}>保存响应时限</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-alarm-policies-table">
            <thead><tr><th>告警类型</th><th>优先级</th><th>响应时限</th></tr></thead>
            <tbody>{alarmPolicies.map((row) => <tr key={`${row.alarmType}-${row.priority}`}><td>{zh(row.alarmType)}</td><td>{zh(row.priority)}</td><td>{row.responseMinutes} 分钟</td></tr>)}</tbody>
          </table>
        </div>
        <h3>故障处置规则（专业负责人＋维保单位两方确认后生效）</h3>
        <div className="admin-inline-form" data-testid="facility-fault-rule-form">
          <input data-testid="facility-fault-rule-project" aria-label="项目" placeholder="项目 id（按分类通用时填）" value={ruleForm.projectId} onChange={(event) => setRuleForm({ ...ruleForm, projectId: event.target.value })} />
          <input data-testid="facility-fault-rule-equipment" aria-label="设备" placeholder="设备 id（按单台设备时填）" value={ruleForm.equipmentId} onChange={(event) => setRuleForm({ ...ruleForm, equipmentId: event.target.value })} />
          <select data-testid="facility-fault-rule-type" aria-label="设备分类" value={ruleForm.equipmentType} onChange={(event) => setRuleForm({ ...ruleForm, equipmentType: event.target.value })}>{EQUIPMENT_TYPES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
          <input data-testid="facility-fault-rule-scenario" aria-label="故障场景" placeholder="故障场景" value={ruleForm.faultScenario} onChange={(event) => setRuleForm({ ...ruleForm, faultScenario: event.target.value })} />
          <input data-testid="facility-fault-rule-requirement" aria-label="处置要求" placeholder="处置要求（停用或继续使用等）" value={ruleForm.handlingRequirement} onChange={(event) => setRuleForm({ ...ruleForm, handlingRequirement: event.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={ALARM_MANAGE} data-testid="facility-fault-rule-create" onClick={() => void createFaultRule()}>登记故障处置规则</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-fault-rules-table">
            <thead><tr><th>规则</th><th>项目/设备</th><th>分类</th><th>故障场景</th><th>处置要求</th><th>状态</th><th>专业负责人/维保单位</th><th>操作</th></tr></thead>
            <tbody>{faultRules.map((row) => (
              <tr key={row.id} data-testid={`facility-fault-rule-${row.id}`}>
                <td className="admin-id-cell" title={row.id}>{shortId(row.id)}</td><td>{row.projectId} / {row.equipmentId ?? "本分类通用"}</td><td>{zh(row.equipmentType)}</td>
                <td>{row.faultScenario}</td><td>{row.handlingRequirement}</td><td>{alarmZh(row.status)}</td>
                <td>{row.specialistConfirmedAt ? "已确认" : "未确认"} / {row.vendorConfirmedAt ? "已确认" : "未确认"}</td>
                <td>
                  {row.status === "pending_confirmation" && !row.specialistConfirmedAt ? <PermissionButton permissionsState={effectivePermissions} permissions={RULE_CONFIRM} data-testid={`facility-fault-rule-confirm-${row.id}`} onClick={() => void faultRuleAction(row.id, "confirm")}>专业负责人确认</PermissionButton> : null}
                  {row.status !== "retired" ? <PermissionButton permissionsState={effectivePermissions} permissions={ALARM_MANAGE} data-testid={`facility-fault-rule-retire-${row.id}`} onClick={() => void faultRuleAction(row.id, "retire")}>停用</PermissionButton> : null}
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
          </>) },
          { key: "scraps", label: "⑤ 报废处置", forceRender: true, children: (<>
        <h3>设备报废处置（审批、实物处置、款项分别收尾）</h3>
        {scrapNotice ? <div className="admin-preview">{scrapNotice}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="facility-scrap-table">
            <thead><tr><th>设备</th><th>处置方式</th><th>审批</th><th>批准人</th><th>实物处置</th><th>款项</th><th>收尾</th><th>操作</th></tr></thead>
            <tbody>{scrapRows.map((row) => (
              <tr key={row.id} data-testid={`facility-scrap-${row.id}`}>
                <td>{row.equipmentName ?? row.equipmentId}</td><td>{row.disposalMethod}</td><td>{scrapZh(row.approvalStatus)}</td>
                <td>{row.approvedBy ?? "-"}</td><td>{scrapZh(row.disposalStatus)}</td><td>{scrapZh(row.moneyStatus)}</td>
                <td>{row.completed ? "已全部收尾" : "未收尾"}</td>
                <td>
                  {row.approvalStatus === "approved" && row.disposalStatus === "pending" ? <PermissionButton permissionsState={effectivePermissions} permissions={EQUIPMENT} data-testid={`facility-scrap-dispose-open-${row.id}`} onClick={() => setScrapDisposal({ scrapId: row.id, note: "", proofs: [] })}>登记实际处置</PermissionButton> : null}
                  {row.approvalStatus === "approved" && row.moneyStatus === "pending_finance" ? <PermissionButton permissionsState={effectivePermissions} permissions={FINANCE_AR_AP} data-testid={`facility-scrap-finance-open-${row.id}`} onClick={() => setScrapFinance({ scrapId: row.id, residual: "", counterparty: "", loss: "" })}>财务登记残值/损失</PermissionButton> : null}
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        {scrapDisposal.scrapId ? (
          <div className="admin-inline-form" data-testid="facility-scrap-disposal-form">
            <span>登记实际处置 {scrapDisposal.scrapId}</span>
            <input data-testid="facility-scrap-evidence" aria-label="处置凭证" type="file" multiple onChange={(event) => void uploadScrapEvidence(event.target.files)} />
            <span>{scrapDisposal.proofs.map((p) => p.name).join("、") || "未上传凭证"}</span>
            <input data-testid="facility-scrap-disposal-note" aria-label="处置说明" placeholder="处置说明" value={scrapDisposal.note} onChange={(event) => setScrapDisposal({ ...scrapDisposal, note: event.target.value })} />
            <PermissionButton permissionsState={effectivePermissions} permissions={EQUIPMENT} data-testid="facility-scrap-disposal-submit" onClick={() => void completeScrapDisposal()}>提交实际处置</PermissionButton>
          </div>
        ) : null}
        {scrapFinance.scrapId ? (
          <div className="admin-inline-form" data-testid="facility-scrap-finance-form">
            <span>财务登记 {scrapFinance.scrapId}</span>
            <input data-testid="facility-scrap-residual" aria-label="残值（元）" type="number" min="0" step="0.01" placeholder="残值（元）" value={scrapFinance.residual} onChange={(event) => setScrapFinance({ ...scrapFinance, residual: event.target.value })} />
            <input data-testid="facility-scrap-counterparty" aria-label="残值回收方" placeholder="残值回收方" value={scrapFinance.counterparty} onChange={(event) => setScrapFinance({ ...scrapFinance, counterparty: event.target.value })} />
            <input data-testid="facility-scrap-loss" aria-label="损失（元）" type="number" min="0" step="0.01" placeholder="损失（元）" value={scrapFinance.loss} onChange={(event) => setScrapFinance({ ...scrapFinance, loss: event.target.value })} />
            <PermissionButton permissionsState={effectivePermissions} permissions={FINANCE_AR_AP} data-testid="facility-scrap-finance-submit" onClick={() => void recordScrapFinance()}>提交财务登记</PermissionButton>
          </div>
        ) : null}
          </>) },
        ]}
      />
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
