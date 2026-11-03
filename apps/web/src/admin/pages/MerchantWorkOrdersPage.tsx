import React from "react";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useAdminAction } from "../hooks/useAdminAction";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";
import { usePagedRows, TablePager } from "../components/TablePager";
import { useSortedRows } from "../components/useSortedRows";
import { PermissionButton, useCan } from "../security/Can";
import type { AdminPageProps, MerchantWorkOrderRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { ComplaintFollowupDialog } from "./ComplaintFollowupDialog";
import { WorkOrderMaterialsPanel } from "./WorkOrderMaterialsPanel";
import { MERCHANT_WORK_ORDER_STATUS_LABELS, MERCHANT_WORK_ORDER_TYPE_LABELS } from "./MerchantZoneWorkOrdersPage";
import { shortId } from "../format/ids";

const WO_ASSIGN = ["merchant.service_work_order.assign"];
const WO_PROCESS = ["merchant.service_work_order.process"];
const WO_CLOSE = ["merchant.service_work_order.close"];
const POLICIES = "/api/v1/merchant-service/work-order-policies";
const TYPE_OPTIONS = Object.entries(MERCHANT_WORK_ORDER_TYPE_LABELS);
// 第 93 条 93-R1：类型用商户工单自己的四类中文名（service＝其他服务请求，不用共享字典的「服务」）。
const typeName = (code?: string | null) => MERCHANT_WORK_ORDER_TYPE_LABELS[code ?? ""] ?? formatBusinessStatus(code);
const PRIORITY_OPTIONS: Array<[string, string]> = [["low", "低"], ["medium", "中"], ["high", "高"], ["critical", "紧急"]];
// 第 102 条 102-R2：状态用本链 ENUM:264 中文名（待商户确认、已处理待关闭）。
const statusName = (code?: string | null) => MERCHANT_WORK_ORDER_STATUS_LABELS[code ?? ""] ?? formatBusinessStatus(code);
// 第 102 条 102-R3（裁决 26）：报修、其他服务请求的关单方式三选一；保洁、投诉照 BSM:1385 写死。
const CLOSE_METHODS = "/api/v1/merchant-service/work-order-close-methods";
const CLOSE_METHOD_LABELS: Record<string, string> = { handler: "处理人完成即结束", staff_acceptance: "员工验收后关闭", merchant_confirm: "须商户确认" };
const CLOSE_METHOD_TYPE_OPTIONS = TYPE_OPTIONS.filter(([value]) => value === "repair" || value === "service");
type MerchantWorkOrderCloseMethodRow = { workOrderType: string; closeMethod: string };
// 第 102 条 102-R1：派单负责人候选（在岗、持处理权限、能管到该商户经营项目的平台员工）。
type OwnerCandidate = { userId: string; username?: string; staffName?: string };

// 第 77 条 77-R3：处理时限按「工单类型×优先级」后台配置（BSM:1385），商户单只有处理时限、没有响应时限。
type MerchantWorkOrderPolicyRow = { workOrderType: string; priority: string; handleMinutes: number };

const TEXT = {
  title: "商户服务工单",
  description: "商户在商户专区自助提报修、保洁、投诉和其他服务请求；本页派单、处理、关闭并配置处理时限与关单方式",
  evidence: "接口证据：读取 /api/v1/merchant-service/work-orders；负责人候选 GET /api/v1/merchant-service/work-orders/{id}/owner-candidates；派单 POST /api/v1/merchant-service/work-orders/{id}/assign；处理 POST /api/v1/merchant-service/work-orders/{id}/actions；关闭 POST /api/v1/merchant-service/work-orders/{id}/close；取消 POST /api/v1/merchant-service/work-orders/{id}/staff-cancel；客诉跟进 GET /api/v1/complaint-followups/merchant_service/{id}、POST …/loop-confirm；用料 GET/POST /api/v1/merchant-service/work-orders/{id}/materials、POST /api/v1/merchant-service/work-orders/{id}/cost-bearer、POST /api/v1/merchant-service/work-orders/{id}/bearer-dispute；关单方式 GET/PUT /api/v1/merchant-service/work-order-close-methods。",
  loadFailed: "商户服务工单读取失败",
  actionFailed: "商户服务工单操作失败",
  noWorkOrder: "当前没有可操作工单。",
  processNoteRequired: "处理动作必须填写处理说明。",
  resolutionRequired: "关闭工单必须填写处理结论。",
  ownerRequired: "派单必须选择负责人。",
  ownerPlaceholder: "请选择负责人",
  closeReason: "关闭缘由",
  closeReasonRequired: "投诉工单由客户关怀部关闭须填写关闭缘由。",
  cancel: "取消工单",
  cancelReason: "取消原因",
  cancelReasonRequired: "取消工单必须填写取消原因（重复提或误提）。",
  complaintFollowup: "客诉跟进",
  materials: "用料",
  closeMethodTitle: "关单方式",
  closeMethodHint: "保洁由处理人完成即结束；投诉须商户确认，商户未确认的由客户关怀部填关闭缘由后关闭，这两类不可改。报修、其他服务请求在此各选一种。",
  closeMethodSave: "保存关单方式",
  workOrderCount: "工单总数",
  pending: "待处理",
  highPriority: "高优先级",
  selected: "选中工单",
  boundary: "商户服务工单由商户在商户专区发起；平台发起的整改与巡检走物业工单",
  assign: "派单",
  process: "追加处理",
  close: "关闭工单",
  workOrderTitle: "工单标题",
  type: "类型",
  priority: "优先级",
  merchant: "商户",
  ownerDepartment: "负责部门",
  ownerUser: "负责人",
  processNote: "处理说明",
  targetStatus: "目标状态",
  resolution: "处理结论",
  satisfaction: "满意度",
  select: "选择",
  status: "状态",
  dueAt: "截止时间",
  evidenceLabel: "附件证据",
  focusClear: "清除筛选，返回全部",
  policyTitle: "处理时限",
  policyHint: "按工单类型×优先级配置，建单时截止时间＝建单时刻＋处理时限；未配置的组合不设截止时间、不超时升级。超时后通知持派单权限、能管到该商户经营项目的主管。",
  handleMinutes: "处理时限（分钟）",
  policyMinutesRequired: "处理时限必须是大于 0 的整数分钟。",
  policySave: "保存处理时限",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：?kpi=<all|pending|high>；点 KPI 数字=带参重进本页只渲染工单表（对应子集）。
// 本页单表：工单总数=全量、待处理=未终结子集、高优先级=high/critical 子集；选中工单=交互态无子集，KPI 数字保持纯文本不可点。
type KpiTarget = "all" | "pending" | "high";
type KpiFocus = { table: KpiTarget };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const table = new URLSearchParams(window.location.search).get("kpi");
  if (table !== "all" && table !== "pending" && table !== "high") return null;
  return { table };
}

function kpiFocusTitle(focus: KpiFocus): string {
  if (focus.table === "pending") return "待处理工单";
  if (focus.table === "high") return "高优先级工单";
  return "全部工单";
}

// KPI 子集过滤：聚焦时先按被点 KPI 把 rows 收窄，再进搜索/排序/分页管线。
function kpiSubset(rows: MerchantWorkOrderRow[], focus: KpiFocus | null): MerchantWorkOrderRow[] {
  if (!focus || focus.table === "all") return rows;
  if (focus.table === "pending") return rows.filter(nonTerminal);
  return rows.filter((row) => ["high", "critical"].includes(row.priority ?? ""));
}

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function nonTerminal(row: MerchantWorkOrderRow) {
  return !["closed", "cancelled"].includes(row.status ?? "");
}

function evidenceList(row: MerchantWorkOrderRow) {
  return (row.evidenceAttachmentIds ?? []).join(", ") || "-";
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, evidenceAttachmentIds: request.evidenceAttachmentIds, result };
}

export function MerchantWorkOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [rows, setRows] = React.useState<MerchantWorkOrderRow[]>([]);
  const { selectedId, setSelectedId, selected } = useSelectedRow(rows);
  const [ownerDepartment, setOwnerDepartment] = React.useState("property");
  const [ownerUserId, setOwnerUserId] = React.useState("");
  const [ownerCandidates, setOwnerCandidates] = React.useState<OwnerCandidate[]>([]);
  const [closeReason, setCloseReason] = React.useState("");
  // 第 111 条 111-R2（第十八批裁决 27「派单后，重复提或误提的由客户关怀部填原因取消」）；111-R3 投诉行的客诉跟进弹窗。
  const [cancelReason, setCancelReason] = React.useState("");
  const [complaintFollowup, setComplaintFollowup] = React.useState<MerchantWorkOrderRow | null>(null);
  // 第十八批 M 段第 117 条 117-R5：选中工单的「用料」面板（BSM:1380；承担方争议按第十八批裁决 29 停在原状态转上级判定）。
  const [materialsOpen, setMaterialsOpen] = React.useState(false);
  const [processNote, setProcessNote] = React.useState("已到场处理，等待商户确认");
  const [targetStatus, setTargetStatus] = React.useState("processing");
  const [resolution, setResolution] = React.useState("已维修完成并确认关闭");
  // 102-R2：不替提单方默认评分（BSM:1385「满意度在回访/验收时由提单方评分」），不填发 null。
  const [satisfactionScore, setSatisfactionScore] = React.useState("");
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该工单完整字段（枚举中文·截止时间中文·引用 TraceLink）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [policies, setPolicies] = React.useState<MerchantWorkOrderPolicyRow[]>([]);
  const [policyForm, setPolicyForm] = React.useState({ workOrderType: "repair", priority: "high", handleMinutes: "" });
  const canSetPolicy = useCan(effectivePermissions, WO_ASSIGN);
  const [closeMethods, setCloseMethods] = React.useState<MerchantWorkOrderCloseMethodRow[]>([]);
  const [closeMethodForm, setCloseMethodForm] = React.useState({ workOrderType: "repair", closeMethod: "staff_acceptance" });

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(table: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", table);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ table });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const workOrders = await api.get<MerchantWorkOrderRow[]>("/api/v1/merchant-service/work-orders");
      setRows(workOrders);
      // 时限读写都要派单权限（与后端同码）；没有的不读，免得整页读取失败。
      if (canSetPolicy) {
        setPolicies(await api.get<MerchantWorkOrderPolicyRow[]>(POLICIES));
        setCloseMethods(await api.get<MerchantWorkOrderCloseMethodRow[]>(CLOSE_METHODS));
      }
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  // 102-R1：选中已提交的单且有派单权限时取负责人候选；换单即清空所选。
  const ownerOrderId = selected?.id;
  const ownerOrderStatus = selected?.status;
  React.useEffect(() => {
    setOwnerUserId("");
    setOwnerCandidates([]);
    if (!ownerOrderId || ownerOrderStatus !== "created" || !canSetPolicy) return;
    let alive = true;
    api.get<OwnerCandidate[]>(`/api/v1/merchant-service/work-orders/${ownerOrderId}/owner-candidates`)
      .then((list) => { if (alive) setOwnerCandidates(list); })
      .catch(() => { if (alive) setOwnerCandidates([]); });
    return () => { alive = false; };
  }, [api, ownerOrderId, ownerOrderStatus, canSetPolicy]);

  const pendingCount = rows.filter(nonTerminal).length;
  const highPriorityCount = rows.filter((row) => ["high", "critical"].includes(row.priority ?? "")).length;

  // 聚焦时先按 KPI 子集收窄，再进搜索/排序/分页；非聚焦=全量。
  const baseRows = kpiSubset(rows, kpiFocus);
  const filteredRows = baseRows.filter((row) => rowMatchesKeyword(row, query));
  const sort = useSortedRows(filteredRows, {
    title: (row) => row.title ?? row.id,
    type: (row) => row.workOrderType,
    priority: (row) => row.priority,
    status: (row) => row.status,
    dueAt: (row) => row.dueAt,
  });
  const paged = usePagedRows(sort.sorted, 10, `${kpiFocus?.table ?? "none"}:${query}:${sort.key}:${sort.direction}`);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.actionFailed,
  });

  async function assignWorkOrder() {
    if (!selected) {
      setFormError(TEXT.noWorkOrder);
      return;
    }
    if (!ownerUserId) {
      setFormError(TEXT.ownerRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-work-order-assign"),
      operatorNote: "完整后台商户工单派单",
      evidenceAttachmentIds: ["att-merchant-work-order-assign"],
      ownerDepartment,
      ownerUserId,
      operatorUserId: "staff_dispatcher",
    };
    await runAction("merchant_work_order_assigned", request, () => api.post(`/api/v1/merchant-service/work-orders/${selected.id}/assign`, request));
  }

  async function processWorkOrder() {
    if (!selected) {
      setFormError(TEXT.noWorkOrder);
      return;
    }
    if (!processNote.trim()) {
      setFormError(TEXT.processNoteRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-work-order-process"),
      operatorNote: processNote,
      evidenceAttachmentIds: ["att-merchant-work-order-process"],
      targetStatus,
      operatorUserId: "staff_processor",
    };
    await runAction("merchant_work_order_processed", request, () => api.post(`/api/v1/merchant-service/work-orders/${selected.id}/actions`, request));
  }

  async function closeWorkOrder() {
    if (!selected) {
      setFormError(TEXT.noWorkOrder);
      return;
    }
    if (!resolution.trim()) {
      setFormError(TEXT.resolutionRequired);
      return;
    }
    if (selected.workOrderType === "complaint" && !closeReason.trim()) {
      setFormError(TEXT.closeReasonRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-work-order-close"),
      operatorNote: "完整后台关闭商户服务工单",
      evidenceAttachmentIds: ["att-merchant-work-order-close"],
      resolution,
      satisfactionScore: satisfactionScore.trim() ? Number(satisfactionScore) : null,
      operatorUserId: "staff_closer",
      ...(closeReason.trim() ? { closeReason: closeReason.trim() } : {}),
    };
    await runAction("merchant_work_order_closed", request, () => api.post(`/api/v1/merchant-service/work-orders/${selected.id}/close`, request));
  }

  async function cancelWorkOrder() {
    if (!selected) {
      setFormError(TEXT.noWorkOrder);
      return;
    }
    if (!cancelReason.trim()) {
      setFormError(TEXT.cancelReasonRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("merchant-work-order-cancel"), reason: cancelReason.trim() };
    await runAction("merchant_work_order_cancelled", request, () => api.post(`/api/v1/merchant-service/work-orders/${selected.id}/staff-cancel`, request));
  }

  async function savePolicy() {
    const handleMinutes = Number(policyForm.handleMinutes);
    if (!Number.isInteger(handleMinutes) || handleMinutes <= 0) {
      setFormError(TEXT.policyMinutesRequired);
      return;
    }
    const request = {
      idempotencyKey: nextKey("merchant-work-order-policy"),
      workOrderType: policyForm.workOrderType,
      priority: policyForm.priority,
      handleMinutes,
    };
    await runAction("merchant_work_order_policy_saved", request, () => api.put(POLICIES, request));
  }

  async function saveCloseMethod() {
    const request = {
      idempotencyKey: nextKey("merchant-work-order-close-method"),
      workOrderType: closeMethodForm.workOrderType,
      closeMethod: closeMethodForm.closeMethod,
    };
    await runAction("merchant_work_order_close_method_saved", request, () => api.put(CLOSE_METHODS, request));
  }

  function openWorkOrderDetail(row: MerchantWorkOrderRow) {
    setDetail({
      title: `工单明细 · ${row.title ?? row.id}`,
      fields: [
        { label: TEXT.workOrderTitle, value: row.title ?? row.id },
        { label: TEXT.merchant, value: <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{row.merchantId ?? "-"}</TraceLink> },
        { label: TEXT.type, value: typeName(row.workOrderType) },
        { label: TEXT.priority, value: formatBusinessStatus(row.priority) },
        { label: TEXT.status, value: statusName(row.status) },
        { label: TEXT.ownerDepartment, value: row.ownerDepartment || "-" },
        { label: TEXT.ownerUser, value: <TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.ownerUserId}>{row.ownerUserId || "-"}</TraceLink> },
        { label: TEXT.dueAt, value: cnDate(row.dueAt) },
        { label: TEXT.evidenceLabel, value: evidenceList(row) },
      ],
    });
  }

  return (
    <section data-testid="merchant-work-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="merchant-work-orders-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="merchant_service.listMerchantServiceWorkOrders.succeeded / assignMerchantServiceWorkOrder.succeeded / addMerchantServiceWorkOrderAction.succeeded / closeMerchantServiceWorkOrder.succeeded / work_order.cancelled / work_order_close_method_saved / complaint.loop_confirmed" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        {kpiFocus ? (
        <div className="admin-state" data-testid="merchant-work-orders-kpi-focus-banner">
          正在查看：{kpiFocusTitle(kpiFocus)}（{baseRows.length}）
          <button type="button" data-testid="merchant-work-orders-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
        </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="merchant-work-orders-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.workOrderCount}</span><button type="button" className="admin-kpi-value" data-testid="merchant-work-orders-kpi-total" onClick={() => enterKpiFocus("all")}>{rows.length}</button><small>{TEXT.merchant}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.pending}</span><button type="button" className="admin-kpi-value" data-testid="merchant-work-orders-kpi-pending" onClick={() => enterKpiFocus("pending")}>{pendingCount}</button><small>{TEXT.status}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.highPriority}</span><button type="button" className="admin-kpi-value" data-testid="merchant-work-orders-kpi-high" onClick={() => enterKpiFocus("high")}>{highPriorityCount}</button><small>{TEXT.priority}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selected}</span><strong className="admin-kpi-value--text" title={selected?.title ?? "-"}>{selected?.title ?? "-"}</strong><small>{selected?.status ?? "-"}</small></div>
        </div>

        <div className="admin-form-row">
          <span className="admin-muted-action" data-testid="merchant-work-orders-boundary">{TEXT.boundary}</span>
        </div>

        <div className="admin-form-grid" data-testid="merchant-work-order-action-form">
          <label>{TEXT.ownerDepartment}<input data-testid="merchant-work-order-owner-department" value={ownerDepartment} onChange={(event) => setOwnerDepartment(event.target.value)} /></label>
          <label>{TEXT.ownerUser}<select data-testid="merchant-work-order-owner-user" value={ownerUserId} onChange={(event) => setOwnerUserId(event.target.value)}>
            <option value="">{TEXT.ownerPlaceholder}</option>
            {ownerCandidates.map((candidate) => <option key={candidate.userId} value={candidate.userId}>{candidate.staffName || candidate.username || candidate.userId}（{shortId(candidate.userId)}）</option>)}
          </select></label>
          <label>{TEXT.processNote}<input data-testid="merchant-work-order-process-note" value={processNote} onChange={(event) => setProcessNote(event.target.value)} /></label>
          <label>{TEXT.targetStatus}<select data-testid="merchant-work-order-target-status" value={targetStatus} onChange={(event) => setTargetStatus(event.target.value)}><option value="assigned">已派单</option><option value="processing">处理中</option><option value="pending_merchant">待商户确认</option><option value="resolved">已处理待关闭</option></select></label>
          <label>{TEXT.resolution}<input data-testid="merchant-work-order-resolution" value={resolution} onChange={(event) => setResolution(event.target.value)} /></label>
          <label>{TEXT.closeReason}<input data-testid="merchant-work-order-close-reason" value={closeReason} placeholder="投诉必填" onChange={(event) => setCloseReason(event.target.value)} /></label>
          <label>{TEXT.satisfaction}<input data-testid="merchant-work-order-satisfaction" value={satisfactionScore} placeholder="提单方评分 1～5，可不填" onChange={(event) => setSatisfactionScore(event.target.value)} /></label>
          <label>{TEXT.cancelReason}<input data-testid="merchant-work-order-cancel-reason" value={cancelReason} placeholder="已派单的重复提或误提单" onChange={(event) => setCancelReason(event.target.value)} /></label>
        </div>
        <div className="admin-form-row" data-testid="merchant-work-order-actions">
          <PermissionButton permissionsState={effectivePermissions} permissions={WO_ASSIGN} data-testid="merchant-work-order-assign" disabled={busyAction !== ""} onClick={() => void assignWorkOrder()}>{TEXT.assign}</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={WO_PROCESS} data-testid="merchant-work-order-process" disabled={busyAction !== ""} onClick={() => void processWorkOrder()}>{TEXT.process}</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={WO_CLOSE} data-testid="merchant-work-order-close" disabled={busyAction !== ""} onClick={() => void closeWorkOrder()}>{TEXT.close}</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={WO_CLOSE} data-testid="merchant-work-order-cancel" disabled={busyAction !== ""} onClick={() => void cancelWorkOrder()}>{TEXT.cancel}</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={WO_PROCESS} data-testid="merchant-work-order-materials" disabled={!selected} onClick={() => setMaterialsOpen(true)}>{TEXT.materials}</PermissionButton>
        </div>
        {formError ? <div data-testid="merchant-work-order-error" className="admin-inline-error">{formError}</div> : null}

        {canSetPolicy ? (
        <div data-testid="merchant-work-order-policies">
          <h3>{TEXT.policyTitle}</h3>
          <span className="admin-muted-action">{TEXT.policyHint}</span>
          <div className="admin-inline-form" data-testid="merchant-work-order-policy-form">
            <select data-testid="merchant-work-order-policy-type" aria-label={TEXT.type} value={policyForm.workOrderType} onChange={(event) => setPolicyForm({ ...policyForm, workOrderType: event.target.value })}>{TYPE_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
            <select data-testid="merchant-work-order-policy-priority" aria-label={TEXT.priority} value={policyForm.priority} onChange={(event) => setPolicyForm({ ...policyForm, priority: event.target.value })}>{PRIORITY_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
            <input data-testid="merchant-work-order-policy-minutes" aria-label={TEXT.handleMinutes} placeholder={TEXT.handleMinutes} type="number" min="1" value={policyForm.handleMinutes} onChange={(event) => setPolicyForm({ ...policyForm, handleMinutes: event.target.value })} />
            <PermissionButton permissionsState={effectivePermissions} permissions={WO_ASSIGN} data-testid="merchant-work-order-policy-save" disabled={busyAction !== ""} onClick={() => void savePolicy()}>{TEXT.policySave}</PermissionButton>
          </div>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="merchant-work-order-policies-table">
              <thead><tr><th>{TEXT.type}</th><th>{TEXT.priority}</th><th>{TEXT.handleMinutes}</th></tr></thead>
              <tbody>{policies.map((row) => <tr key={`${row.workOrderType}-${row.priority}`} data-testid={`merchant-work-order-policy-${row.workOrderType}-${row.priority}`}><td>{typeName(row.workOrderType)}</td><td>{formatBusinessStatus(row.priority)}</td><td>{row.handleMinutes}</td></tr>)}</tbody>
            </table>
          </div>
          <h4>{TEXT.closeMethodTitle}</h4>
          <span className="admin-muted-action">{TEXT.closeMethodHint}</span>
          <div className="admin-inline-form" data-testid="merchant-work-order-close-method-form">
            <select data-testid="merchant-work-order-close-method-type" aria-label={TEXT.type} value={closeMethodForm.workOrderType} onChange={(event) => setCloseMethodForm({ ...closeMethodForm, workOrderType: event.target.value })}>{CLOSE_METHOD_TYPE_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
            <select data-testid="merchant-work-order-close-method" aria-label={TEXT.closeMethodTitle} value={closeMethodForm.closeMethod} onChange={(event) => setCloseMethodForm({ ...closeMethodForm, closeMethod: event.target.value })}>{Object.entries(CLOSE_METHOD_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
            <PermissionButton permissionsState={effectivePermissions} permissions={WO_ASSIGN} data-testid="merchant-work-order-close-method-save" disabled={busyAction !== ""} onClick={() => void saveCloseMethod()}>{TEXT.closeMethodSave}</PermissionButton>
          </div>
          <ul data-testid="merchant-work-order-close-methods">{closeMethods.map((row) => <li key={row.workOrderType} data-testid={`merchant-work-order-close-method-${row.workOrderType}`}>{typeName(row.workOrderType)}：{CLOSE_METHOD_LABELS[row.closeMethod] ?? "-"}</li>)}</ul>
        </div>
        ) : null}
        </>
        )}

        <div className="admin-list-toolbar" data-testid="merchant-work-orders-filter">
          <input data-testid="merchant-work-orders-search" value={query} onChange={(event) => setQuery(event.target.value)} aria-label="搜索标题 / 类型 / 优先级 / 状态 / 负责人" placeholder="搜索标题 / 类型 / 优先级 / 状态 / 负责人" />
          <span className="admin-muted-action" data-testid="merchant-work-orders-filter-count">匹配 {filteredRows.length} / {baseRows.length} 条</span>
        </div>
        {filteredRows.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="merchant-work-orders-filter-empty">无匹配工单</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-work-orders-table">
            <thead><tr><th>{TEXT.select}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-title" onClick={() => sort.toggle("title")} {...sort.thProps("title")}>{TEXT.workOrderTitle}{sort.indicator("title")}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-type" onClick={() => sort.toggle("type")} {...sort.thProps("type")}>{TEXT.type}{sort.indicator("type")}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-priority" onClick={() => sort.toggle("priority")} {...sort.thProps("priority")}>{TEXT.priority}{sort.indicator("priority")}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-status" onClick={() => sort.toggle("status")} {...sort.thProps("status")}>{TEXT.status}{sort.indicator("status")}</th><th>{TEXT.ownerDepartment}</th><th>{TEXT.ownerUser}</th><th className="admin-sort-th" data-testid="merchant-work-orders-sort-dueAt" onClick={() => sort.toggle("dueAt")} {...sort.thProps("dueAt")}>{TEXT.dueAt}{sort.indicator("dueAt")}</th><th>{TEXT.evidenceLabel}</th><th>详情</th></tr></thead>
            <tbody>
              {paged.pageRows.map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`merchant-work-order-row-${row.id}`}
                  onClick={() => openWorkOrderDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openWorkOrderDetail(row); } }}>
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此工单 ${row.id}`} data-testid={`merchant-work-order-select-${row.id}`} checked={(selected?.id ?? "") === row.id} onChange={() => setSelectedId(row.id)} /></td>
                  <td>{row.title ?? row.id}<br /><span onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}><small>商户 {row.merchantId}</small></TraceLink></span></td>
                  <td>{typeName(row.workOrderType)}</td>
                  <td>{formatBusinessStatus(row.priority)}</td>
                  <td>{statusName(row.status)}</td>
                  <td>{row.ownerDepartment || "-"}</td>
                  <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-ACC-001" objectId={row.ownerUserId}>{row.ownerUserId || "-"}</TraceLink></td>
                  <td>{cnDate(row.dueAt)}</td>
                  <td>{evidenceList(row)}</td>
                <td className="admin-row-detail-cell">{row.workOrderType === "complaint" ? (
                  <span onClick={(event) => event.stopPropagation()}><PermissionButton permissionsState={effectivePermissions} permissions={WO_CLOSE}
                    data-testid={`merchant-work-order-complaint-followup-${row.id}`} onClick={() => setComplaintFollowup(row)}>{TEXT.complaintFollowup}</PermissionButton></span>
                ) : null}<button type="button" className="admin-row-detail-btn" aria-label="查看工单详情" onClick={(event) => { event.stopPropagation(); openWorkOrderDetail(row); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="merchant-work-orders-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
      {complaintFollowup ? (
        <ComplaintFollowupDialog api={api} chain="merchant_service" workOrderId={complaintFollowup.id} closed={complaintFollowup.status === "closed"}
          onClose={() => setComplaintFollowup(null)} setActionResult={setActionResult} setGlobalError={setGlobalError} />
      ) : null}
      {materialsOpen && selected ? (
        <WorkOrderMaterialsPanel api={api} basePath={`/api/v1/merchant-service/work-orders/${encodeURIComponent(selected.id)}`}
          title={`用料 · ${selected.title ?? selected.id}`} canIssue={["assigned", "processing", "pending_merchant"].includes(selected.status ?? "")}
          canMarkBearer={nonTerminal(selected)} canDispute={nonTerminal(selected)} allowMerchantPick={false}
          disputeHint="已转上级判定；判定前不能转已处理待关闭、不能关闭，判定后照常处理"
          setActionResult={setActionResult} onChanged={() => void reload()} onClose={() => setMaterialsOpen(false)} />
      ) : null}
    </section>
  );
}
