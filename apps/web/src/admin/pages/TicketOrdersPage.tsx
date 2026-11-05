// 文件版本：V0.04
import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { loadSecondary, loadSecondaryIfPermitted, useAdminResource } from "../hooks/useAdminResource";
import { hasPermissions } from "../security/PermissionGate";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";

// 后端写权限码（TicketingController：/ticketing/orders/{order_id}/issue）
const TICKET_ISSUE_PERMISSION = "ticket.ticket.issue";
import { usePagedRows, TablePager } from "../components/TablePager";
import type { AdminPageProps, TicketIssuedEntitlementRow, TicketOrderRow, TicketRefundRow, TicketVerificationRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import { TicketRefundCasesPanel } from "./TicketRefundCasesPanel";

const TEXT = {
  title: "\u51fa\u7968\u4e0e\u6838\u9500\u8ffd\u8e2a",
  description: "\u805a\u5408\u7968\u52a1\u8ba2\u5355\u3001\u652f\u4ed8\u72b6\u6001\u3001\u51fa\u7968\u72b6\u6001\u3001\u6743\u76ca\u548c\u6838\u9500\u9884\u68c0\uff0c\u7528\u4e8e\u5b9a\u4f4d\u672a\u51fa\u7968\u548c\u5f02\u5e38\u6838\u9500\u3002",
  evidence: "\u63a5\u53e3\u8bc1\u636e\uff1a\u8bfb\u53d6 /api/v1/ticketing/orders\uff1b\u8bfb\u53d6 /api/v1/ticketing/issued-entitlements\uff1b\u8bfb\u53d6 /api/v1/ticketing/verifications\uff1b\u51fa\u7968 POST /api/v1/ticketing/orders/{order_id}/issue\uff1b\u6838\u9500\u9884\u68c0 POST /api/v1/ticketing/verifications/preview；读取 /api/v1/ticketing/refunds；确认拨付 POST /api/v1/ticketing/refunds/{refund_id}/confirm-payout；提起核销异常复核 POST /api/v1/ticketing/verifications/{id}/anomaly；主管处置核销异常 POST /api/v1/ticketing/verification-anomalies/{id}/resolve；发起退款 POST /api/v1/ticketing/refunds；退款审批 POST /api/v1/ticketing/refunds/{refund_id}/approve、/reject；通票安全事件 GET/POST /api/v1/ticketing/safety-events、批准 POST /api/v1/ticketing/safety-events/{safety_event_id}/approve。",
  loadFailed: "\u7968\u52a1\u8ba2\u5355\u8ffd\u8e2a\u6570\u636e\u8bfb\u53d6\u5931\u8d25",
  actionFailed: "\u7968\u52a1\u8ba2\u5355\u64cd\u4f5c\u5931\u8d25",
  noOrder: "\u5f53\u524d\u6ca1\u6709\u53ef\u51fa\u7968\u8ba2\u5355\u3002",
  entitlementRequired: "\u8bf7\u586b\u5199\u6743\u76ca\u7801\u540e\u518d\u505a\u6838\u9500\u9884\u68c0\u3002",
  orders: "\u8ba2\u5355",
  paid: "\u5df2\u652f\u4ed8",
  pendingIssue: "\u5f85\u51fa\u7968",
  entitlements: "\u6743\u76ca",
  verifications: "\u6838\u9500",
  selectedOrder: "\u9009\u4e2d\u8ba2\u5355",
  issue: "\u89e6\u53d1\u51fa\u7968",
  preview: "\u6838\u9500\u9884\u68c0",
  entitlementCode: "\u6743\u76ca\u7801",
  boundary: "\u672c\u9875\u4e0d\u7ed5\u8fc7\u652f\u4ed8\u72b6\u6001\uff1b\u51fa\u7968\u548c\u6838\u9500\u9884\u68c0\u5747\u4fdd\u7559\u5e42\u7b49\u952e\u548c\u5ba1\u8ba1\u4e8b\u4ef6\u3002",
  select: "\u9009\u62e9",
  orderNo: "\u8ba2\u5355\u53f7",
  ticketType: "\u7968\u79cd",
  quantity: "\u6570\u91cf",
  visitDate: "\u5230\u8bbf\u65e5",
  channel: "\u6e20\u9053",
  amount: "\u91d1\u989d",
  payStatus: "\u652f\u4ed8",
  issueStatus: "\u51fa\u7968",
  status: "\u72b6\u6001",
  createdAt: "\u4e0b\u5355\u65f6\u95f4",
  paidAt: "\u652f\u4ed8\u65f6\u95f4",
  issueNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u51fa\u7968",
  previewNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u6838\u9500\u9884\u68c0",
  ordersHelp: "\u8ba2\u5355\u603b\u6570",
  paidHelp: "\u5df2\u652f\u4ed8\u8ba2\u5355",
  pendingIssueHelp: "\u672a\u51fa\u7968\u8ba2\u5355",
  entitlementsHelp: "\u5df2\u53d1\u6743\u76ca",
  verificationsHelp: "\u6838\u9500\u8bb0\u5f55",
  selectedOrderHelp: "\u5f53\u524d\u9009\u4e2d",
  focusClear: "\u6e05\u9664\u7b5b\u9009\uff0c\u8fd4\u56de\u5168\u90e8",
  // C2 \u8d2d\u7968\u4eba\u8138\u7167\u6bd4\u5bf9\uff08\u540e\u7aef V229 \u5b58\u6863\u3001V353 \u7559\u5b58\u5929\u6570\u540e\u53f0\u53ef\u914d\uff09\u3002
  // TK-06（BSM:780 一人多次票首次核销留存人脸、后续以该人脸核对；2026-10-21 裁决：供应商不可用时不留不核，不做目视核对）。
  faceBindingTitle: "一人多次票人脸核对",
  faceBindingCapture: "本票是一人多次票、尚未留存人脸：本次为首次入场，须客人单独同意留存人脸后在检票终端拍照，系统留存后用于核对后续各次是否同一使用人；客人不同意的不能入场，票保持未核销。",
  faceBindingMatch: "本票已留存首次入场人脸：在检票终端为本人拍照，由系统比对是否同一使用人，不一致的拒绝核销。",
  // 退款待确认拨付（业务口径：退款以“成功”为最终完成态，只有“成功”才计入已退金额；“已通过”只是已批准并已发起兑付）。
  refundTitle: "退款待确认拨付",
  refundHint: "“已通过”表示已批准并已发起原路退回，钱还没到客人账上；财务实际打款成功后在这里确认，退款才记“成功”并计入已退金额。",
  refundOnlyPending: "只看待确认拨付",
  refundResultLabel: "原路退回结果（打款流水号 / 回执号）",
  refundResultRequired: "请先填写原路退回结果（打款流水号或回执号）再确认拨付。",
  refundConfirm: "确认拨付",
  refundEmpty: "当前没有待确认拨付的退款。",
  refundPendingHelp: "已通过未拨付",
  refundNo: "退款单号",
  refundOrder: "原订单",
  refundAmount: "退款金额",
  refundChannel: "退款方式",
  refundRequestedBy: "申请人",
  refundApprovedBy: "审批人",
  refundCreatedAt: "申请时间",
  // TK-14 核销异常复核（ENUM「核销记录状态」「异常状态的统一出口」）。
  verificationTitle: "核销记录",
  verificationHint: "已成功扣次但现场未放行或实际权益交付有争议、需要主管核实时，对该成功记录提起异常复核：原成功记录与扣次保留；主管在「更正后恢复」「作废并冲正」中选一种处置并填写原因，不自动退款。普通拒绝、失败不改为异常。",
  verificationNo: "核销单号",
  verificationReason: "结果说明",
  verificationAt: "时间",
  anomalyColumn: "异常复核",
  anomalyReasonLabel: "原因（提起异常复核或主管处置时必填）",
  anomalyReasonRequired: "请先填写原因再提起异常复核或处置。",
  raiseAnomaly: "提起异常复核",
  anomalyPending: "待主管处置",
  resolveRestored: "更正后恢复",
  resolveVoided: "作废并冲正",
  verificationsEmpty: "当前没有核销记录。",
};

const ANOMALY_RESOLUTION_LABELS = {
  restored: TEXT.resolveRestored,
  voided_reversed: TEXT.resolveVoided,
};

// 第十八批 P 段 134-R3（PGM:287 所有终端零英文）：核销记录 result_reason 机器码 → 中文。
// 规则拒绝码（TicketVerificationService.RULE_REJECTION_CODES 小写）取后端该码抛出处的提示原文；冲正、异常原因是员工录入原话，原样显示。
// 第十八批 Q 段 145-R1：正常业务能落库的技术失败码同样取抛出处原文（device_not_managed 原文含 MDM，改写为中文）。
const VERIFICATION_RESULT_REASON_LABELS: Record<string, string> = {
  entitlement_not_active: "该权益未生效",
  entitlement_not_yet_valid: "该权益尚未到生效时间",
  entitlement_expired: "该权益已过期",
  entitlement_used_up: "该权益剩余次数已用完",
  entitlement_used: "该权益剩余次数已用完且当前营业日没有有效激活",
  ticket_instance_refund_in_progress: "该票已被冻结（退款中、已退款或转赠中），不能核销",
  multi_person_cross_session_forbidden: "多人票各人份额必须在同一场次内核销，不能跨场次/日期",
  entitlement_provider_mismatch: "该权益只能在发放它的商户核销",
  entitlement_not_amusement: "该权益不是游乐类权益",
  entitlement_not_for_project: "该权益未绑定到此游乐项目",
  amusement_project_unresolved: "游乐项目无法定位",
  amusement_business_day_required: "必须使用真实综合体当前批准的营业日窗口",
  park_wide_fault_verification_suspended: "全园故障期间暂停游乐入场核销",
  project_not_open: "该游乐项目未开放",
  safety_blocked: "安全闸未放行，游乐核销被拦截",
  session_not_open: "该场次未开放",
  session_capacity_exhausted: "项目在场人数已达硬安全容量",
  session_project_mismatch: "场次不属于该游乐项目",
  venue_not_in_session: "场馆当前不在营业场次内",
  entry_pass_past_session_void: "入场凭证已失效：已在上一场次入场",
  amusement_no_open_session: "当前时间没有开放中的游乐场次",
  multi_use_face_consent_required: "一人多次票首次入场须客人同意留存人脸，未同意不能入场（票仍未核销）",
  multi_use_face_capture_required: "一人多次票入场须现场拍摄人脸（照片缺失或格式不正确）",
  multi_use_holder_mismatch: "与该票留存的使用人人脸不一致，不能核销",
  bar_entry_not_verified: "须先核销这家酒吧的入场权益，再核销酒水（赠饮）权益",
  window_expired: "动态码已过刷新窗口",
  signature_invalid: "核销凭证签名无效",
  device_blocked: "设备已被封禁或挂失",
  device_not_managed: "设备未纳入设备管理",
  verify_device_merchant_unresolved: "核销设备未绑定商户，无法判定本次核销发生在哪家门店",
  credential_expired: "核销凭证已过期，请刷新后重试",
  session_reentry: "本场次再次入场，不扣次",
  day_replay: "当天已激活，再次入场不扣次",
  amusement_verified: "游乐入场核销",
};

// KPI \u805a\u7126\u89c6\u56fe\uff08\u7167 OrganizationMerchantPage \u6837\u677f\uff09\uff1a\u70b9 KPI \u6570\u5b57=\u5e26 ?kpi= \u53c2\u6570\u91cd\u8fdb\u672c\u9875\uff0c\u53ea\u6e32\u67d3\u8ba2\u5355\u8868\u5e76\u6309\u5b50\u96c6\u8fc7\u6ee4\u3002
// \u26a0\ufe0f\u4e0e\u5df2\u6709\u7684 ?focus=<\u8ba2\u5355\u5b9a\u4f4d>\uff08P1-B\uff09\u8bfb\u4e0d\u540c\u53c2\u6570\uff0c\u4e92\u4e0d\u5e72\u6270\uff1akpi \u805a\u7126\u8bfb ?kpi\uff0c\u8ba2\u5355\u5b9a\u4f4d\u8bfb focusObjectId\u3002
type KpiTarget = "orders" | "paid" | "not_issued";
type KpiFocus = { target: KpiTarget };

// TK-06：核销预检返回体里的人脸处置（capture=首次留脸、match=按留存人脸比对；单次票、多人票或供应商不可用时为空）。
type VerificationPreviewResult = { entitlement?: { id?: string }; faceBinding?: "capture" | "match" | null };

function readKpiFocusFromUrl(): KpiFocus | null {
  if (typeof window === "undefined") return null;
  const target = new URLSearchParams(window.location.search).get("kpi");
  if (target !== "orders" && target !== "paid" && target !== "not_issued") return null;
  return { target };
}

function kpiFocusTitle(target: KpiTarget): string {
  if (target === "paid") return TEXT.paid;
  if (target === "not_issued") return TEXT.pendingIssue;
  return TEXT.orders;
}

function formatAmount(amountCent: number | undefined) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(Number(amountCent ?? 0) / 100);
}

/**
 * ROOT-03-R20（BSM 第 1749 行混合支付逐笔原路退回）：财务确认拨付按待打款明细打款——只打分到在线笔的金额（渠道×金额），
 * 储值部分批准时已当场回原卡，注明「已回卡」，不能按整笔金额再打一次。
 */
function payoutText(refund: TicketRefundRow) {
  const items = refund.payoutItems ?? [];
  const online = items.reduce((sum, item) => sum + Number(item.amountCent ?? 0), 0);
  const storedValue = Number(refund.refundAmountCent ?? 0) - online;
  const parts = items.map((item) => `${formatBusinessStatus(item.channel)} ${formatAmount(item.amountCent)}`).join("、");
  return `待打款：${parts}${storedValue > 0 ? `；储值 ${formatAmount(storedValue)} 已回卡` : ""}`;
}

function selectedLabel(order: TicketOrderRow | undefined) {
  return order?.orderNo ?? order?.id ?? "-";
}

export function TicketOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
  const [orders, setOrders] = React.useState<TicketOrderRow[]>([]);
  const [entitlements, setEntitlements] = React.useState<TicketIssuedEntitlementRow[]>([]);
  const [verifications, setVerifications] = React.useState<TicketVerificationRow[]>([]);
  const [entitlementsNotice, setEntitlementsNotice] = React.useState<string | null>(null);
  const [verificationsNotice, setVerificationsNotice] = React.useState<string | null>(null);
  // 退款台账：默认只显示 status='approved'（已批准、钱还没打出去）——业务人员此前只能翻审计事件才知道哪几笔欠拨付。
  const [refunds, setRefunds] = React.useState<TicketRefundRow[]>([]);
  const [refundsNotice, setRefundsNotice] = React.useState<string | null>(null);
  const [onlyPendingPayout, setOnlyPendingPayout] = React.useState(true);
  const [payoutResult, setPayoutResult] = React.useState("");
  const [anomalyReason, setAnomalyReason] = React.useState("");
  const { selectedId: selectedOrderId, setSelectedId: setSelectedOrderId, selected: selectedOrder } = useSelectedRow(orders);
  const [entitlementCode, setEntitlementCode] = React.useState("ENT-0001");
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该订单完整字段（金额¥·枚举中文·下单/支付时间中文），与既有单选/下钻并存。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [faceBinding, setFaceBinding] = React.useState<VerificationPreviewResult["faceBinding"]>(null);

  // 浏览器后退/前进也能进出聚焦视图。AdminApp 的 popstate 只重建 route/focus，本页 ?kpi= 参数自理，互不干扰。
  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  // 点 KPI 数字 → 带 ?kpi= 参数重进本页（pathname 原样保留，子路径前缀由门户链路负责，只改查询串）。
  function enterKpiFocus(target: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", target);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus({ target });
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  // 「返回全部」：清掉 kpi 参数（保留 focus 等其它参数），恢复完整页面。
  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    (document.querySelector(".admin-content") ?? window).scrollTo({ top: 0 });
  }

  // 无权限副资源不请求（根因修）：核销记录端点需 verification.view，页面基础权限 ticket.verify.view 不含之。
  const canVerificationView = hasPermissions(effectivePermissions, ["verification.view"]);
  const canRefundView = hasPermissions(effectivePermissions, ["ticket.refund.view"]);
  const canRaiseAnomaly = hasPermissions(effectivePermissions, ["verification.execute"]);
  const canResolveAnomaly = hasPermissions(effectivePermissions, ["ticket.verify.anomaly.resolve"]);
  const { loading, error, reload: reloadResource } = useAdminResource(
    async () => {
      const [orderRows, entitlementRows, verificationRows, refundRows] = await Promise.all([
        api.get<TicketOrderRow[]>("/api/v1/ticketing/orders"),
        // 权益/核销为副资源：403 或失败时降级（KPI 显「—」不静默归零），出票主表照常。
        loadSecondary(() => api.get<TicketIssuedEntitlementRow[]>("/api/v1/ticketing/issued-entitlements"), [] as TicketIssuedEntitlementRow[]),
        loadSecondaryIfPermitted(canVerificationView, () => api.get<TicketVerificationRow[]>("/api/v1/ticketing/verifications"), [] as TicketVerificationRow[]),
        loadSecondaryIfPermitted(canRefundView, () => api.get<TicketRefundRow[]>("/api/v1/ticketing/refunds"), [] as TicketRefundRow[]),
      ]);
      setOrders(orderRows);
      setEntitlements(entitlementRows.data);
      setEntitlementsNotice(entitlementRows.notice);
      setVerifications(verificationRows.data);
      setVerificationsNotice(verificationRows.notice);
      setRefunds(refundRows.data);
      setRefundsNotice(refundRows.notice);
      setEntitlementCode((current) => current || entitlementRows.data[0]?.entitlementCode || "");
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const paidCount = orders.filter((order) => order.payStatus === "paid").length;
  const pendingIssueCount = orders.filter((order) => order.issueStatus !== "issued").length;
  // KPI 改走统一明细端点（数字=明细合计同源）；失败/无权限回退本地列表计数并隐藏「完整明细」入口，体验不降级。
  const orderCountKpi = useMetricKpiValue(api, "ticket.order_count", {});
  const paidOrderKpi = useMetricKpiValue(api, "ticket.paid_order_count", {});
  const pendingIssueKpi = useMetricKpiValue(api, "ticket.pending_issue_count", {});
  const orderCountDisplay = orderCountKpi.status === "ready" && orderCountKpi.value !== null ? orderCountKpi.value : orders.length;
  const paidCountDisplay = paidOrderKpi.status === "ready" && paidOrderKpi.value !== null ? paidOrderKpi.value : paidCount;
  const pendingIssueDisplay = pendingIssueKpi.status === "ready" && pendingIssueKpi.value !== null ? pendingIssueKpi.value : pendingIssueCount;
  // KPI 聚焦子集：paid=已支付订单、not_issued=未出票订单、orders=全部。聚焦只客户端过滤已加载内存数组，不发后端查询。
  const kpiOrders = kpiFocus?.target === "paid"
    ? orders.filter((order) => order.payStatus === "paid")
    : kpiFocus?.target === "not_issued"
      ? orders.filter((order) => order.issueStatus !== "issued")
      : orders;
  // 作下钻目标：focusObjectId 可能是订单 id 或订单号，两者都匹配
  const focusMatches = (order: TicketOrderRow) => !!focusObjectId && (order.id === focusObjectId || order.orderNo === focusObjectId);
  const focusedOrder = focusObjectId ? kpiOrders.find(focusMatches) : undefined;
  const keywordOrders = kpiOrders.filter((order) => rowMatchesKeyword(order, query));
  // 命中下钻订单时置顶，确保落在第 1 页可见并高亮；无 focus 时顺序完全不变
  const filteredOrders = focusedOrder
    ? [focusedOrder, ...keywordOrders.filter((order) => order.id !== focusedOrder.id)]
    : keywordOrders;
  const paged = usePagedRows(filteredOrders, 10, query + "|" + (focusObjectId || "") + "|" + (kpiFocus?.target || ""));
  // 待确认拨付=已通过但尚未确认实际打款的退款单（业务 enum：只有“成功”才计入已退金额）。
  const pendingPayoutRefunds = refunds.filter((refund) => refund.status === "approved");
  const visibleRefunds = onlyPendingPayout ? pendingPayoutRefunds : refunds;
  // 有待处置异常的原成功核销：不再给提起入口（同一原记录同时只能有一条待处置异常）。
  const openAnomalyOf = new Set(verifications
    .filter((row) => row.status === "anomaly" && !row.anomalyResolution)
    .map((row) => row.anomalyOfVerificationId));
  const pagedVerifications = usePagedRows(verifications, 10);

  React.useEffect(() => {
    if (focusedOrder) setSelectedOrderId(focusedOrder.id);
  }, [focusedOrder, setSelectedOrderId]);

  async function runAction(action: string, request: () => Promise<unknown>, reload = true) {
    setBusyAction(action);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await request();
      setActionResult({ action, orderId: selectedOrder?.id, result });
      if (reload) await reloadResource();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : TEXT.actionFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action, orderId: selectedOrder?.id, status: "failed", message });
    } finally {
      setBusyAction("");
    }
  }

  function validateIssue() {
    if (!selectedOrder) {
      setFormError(TEXT.noOrder);
      return false;
    }
    return true;
  }

  async function issueOrder() {
    if (!selectedOrder) {
      setFormError(TEXT.noOrder);
      return;
    }
    await runAction("ticket_order_issued", () => api.post(`/api/v1/ticketing/orders/${selectedOrder.id}/issue`, { operatorNote: TEXT.issueNote }));
  }

  async function previewVerification() {
    if (!entitlementCode.trim()) {
      setFormError(TEXT.entitlementRequired);
      return;
    }
    // 换一张票就先清掉上一张票的人脸提示。
    setFaceBinding(null);
    await runAction("ticket_verification_previewed", async () => {
      const preview = await api.post<VerificationPreviewResult>("/api/v1/ticketing/verifications/preview", {
        entitlementCode,
        operatorNote: TEXT.previewNote,
      });
      // TK-06：只按后端给的人脸处置提示；单次票、多人票或供应商不可用时不出现任何人脸区块。
      setFaceBinding(preview?.faceBinding ?? null);
      return preview;
    }, false);
  }

  // 财务实际打完款后确认拨付：approved→success。原路退回结果必填（业务 HTML 退款授权与留痕行要求留痕该结果）。
  async function confirmRefundPayout(refund: TicketRefundRow) {
    await runAction("ticket_refund_payout_confirmed", () => api.post(
      `/api/v1/ticketing/refunds/${encodeURIComponent(refund.id)}/confirm-payout`,
      { reason: payoutResult.trim() },
    ));
  }

  function validatePayout() {
    if (!payoutResult.trim()) {
      setFormError(TEXT.refundResultRequired);
      return false;
    }
    return true;
  }

  function validateAnomalyReason() {
    if (!anomalyReason.trim()) {
      setFormError(TEXT.anomalyReasonRequired);
      return false;
    }
    return true;
  }

  // TK-14：成功核销提起异常复核（原成功记录与扣次保留）。
  async function raiseAnomaly(row: TicketVerificationRow) {
    if (!validateAnomalyReason()) return;
    await runAction("ticket_verification_anomaly_raised", () => api.post(
      `/api/v1/ticketing/verifications/${encodeURIComponent(row.id)}/anomaly`,
      { reason: anomalyReason.trim() },
    ));
  }

  // TK-14：主管二选一处置（更正后恢复 / 作废并冲正），原因必填。
  async function resolveAnomaly(row: TicketVerificationRow, resolution: "restored" | "voided_reversed") {
    await runAction("ticket_verification_anomaly_resolved", () => api.post(
      `/api/v1/ticketing/verification-anomalies/${encodeURIComponent(row.id)}/resolve`,
      { resolution, reason: anomalyReason.trim() },
    ));
  }

  function anomalyCell(row: TicketVerificationRow) {
    if (row.status === "anomaly") {
      if (row.anomalyResolution) {
        return `${(ANOMALY_RESOLUTION_LABELS as Record<string, string | undefined>)[row.anomalyResolution] ?? row.anomalyResolution} · ${row.anomalyResolvedBy ?? "-"} · ${cnDate(row.anomalyResolvedAt)} · ${row.anomalyResolutionReason ?? ""}`;
      }
      return (
        <>
          {TEXT.anomalyPending}
          {canResolveAnomaly ? (["restored", "voided_reversed"] as const).map((resolution) => (
            <HighRiskConfirm
              key={resolution}
              label={ANOMALY_RESOLUTION_LABELS[resolution]}
              impact={resolution === "restored"
                ? "原成功核销记录与扣次保持不变，本异常记为已处置并留痕原因；不退款。"
                : "按既有冲正规则作废原成功核销并恢复一次次数，本异常记为已处置并留痕原因；不自动退款。"}
              disabled={busyAction !== ""}
              precheck={validateAnomalyReason}
              onConfirm={() => void resolveAnomaly(row, resolution)}
              data-testid={`ticket-verification-resolve-${resolution}-${row.id}`}
            />
          )) : null}
        </>
      );
    }
    if (row.status === "success" && canRaiseAnomaly && !openAnomalyOf.has(row.id)) {
      return (
        <button type="button" data-testid={`ticket-verification-raise-anomaly-${row.id}`} disabled={busyAction !== ""}
          onClick={() => void raiseAnomaly(row)}>{TEXT.raiseAnomaly}</button>
      );
    }
    return "—";
  }

  function openOrderDetail(order: TicketOrderRow) {
    setDetail({
      title: `订单明细 · ${order.orderNo ?? order.id}`,
      fields: [
        { label: TEXT.orderNo, value: order.orderNo ?? order.id },
        { label: TEXT.ticketType, value: <TraceLink navigateTo={navigateTo} pageId="ADM-TKT-001" objectId={order.ticketTypeId}>{order.ticketTypeId ?? "—"}</TraceLink> },
        { label: TEXT.quantity, value: order.quantity ?? "—" },
        { label: TEXT.visitDate, value: cnDate(order.visitDate) },
        { label: TEXT.channel, value: formatBusinessStatus(order.salesChannel) },
        { label: TEXT.amount, value: formatAmount(order.amountCent) },
        { label: TEXT.payStatus, value: formatBusinessStatus(order.payStatus) },
        { label: TEXT.issueStatus, value: formatBusinessStatus(order.issueStatus) },
        { label: TEXT.status, value: formatBusinessStatus(order.status) },
        { label: TEXT.createdAt, value: cnDate(order.createdAt) },
        { label: TEXT.paidAt, value: cnDate(order.paidAt) },
        { label: "订单编号", value: order.id },
      ],
    });
  }

  return (
    <section data-testid="ticket-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="ticket-orders-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="ticket_order.viewed / ticket.issued / ticket_verification.previewed" />
      {focusObjectId ? (
        <div className="admin-state" data-testid="ticket-orders-focus">已从下钻定位到订单：{focusObjectId}{focusedOrder ? "（下表已置顶并高亮）" : "（未在当前订单列表中）"}</div>
      ) : null}
      <AdminDataState loading={loading} error={error} empty={orders.length === 0} onRetry={reloadResource}>
        {kpiFocus ? (
          <div className="admin-state" data-testid="ticket-orders-kpi-focus-banner">
            正在查看：{kpiFocusTitle(kpiFocus.target)}（{filteredOrders.length}）
            <button type="button" data-testid="ticket-orders-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
          </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="ticket-orders-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.orders}</span><button type="button" className="admin-kpi-value" data-testid="ticket-orders-kpi-orders" onClick={() => enterKpiFocus("orders")}>{orderCountDisplay}</button><small>{TEXT.ordersHelp}{orderCountKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="ticket.order_count" testid="ticket-orders-metric-detail-orders" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.paid}</span><button type="button" className="admin-kpi-value" data-testid="ticket-orders-kpi-paid" onClick={() => enterKpiFocus("paid")}>{paidCountDisplay}</button><small>{TEXT.paidHelp}{paidOrderKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="ticket.paid_order_count" testid="ticket-orders-metric-detail-paid" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.pendingIssue}</span><button type="button" className="admin-kpi-value" data-testid="ticket-orders-kpi-not-issued" onClick={() => enterKpiFocus("not_issued")}>{pendingIssueDisplay}</button><small>{TEXT.pendingIssueHelp}{pendingIssueKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="ticket.pending_issue_count" testid="ticket-orders-metric-detail-not-issued" /></> : null}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.entitlements}</span><strong data-testid="ticket-orders-kpi-entitlements">{entitlementsNotice ? "—" : entitlements.length}</strong><small data-testid={entitlementsNotice ? "ticket-orders-entitlements-degraded" : undefined}>{entitlementsNotice ?? TEXT.entitlementsHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.verifications}</span><strong data-testid="ticket-orders-kpi-verifications">{verificationsNotice ? "—" : verifications.length}</strong><small data-testid={verificationsNotice ? "ticket-orders-verifications-degraded" : undefined}>{verificationsNotice ?? TEXT.verificationsHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedOrder}</span><strong className="admin-kpi-value--text" title={selectedLabel(selectedOrder)}>{selectedLabel(selectedOrder)}</strong><small>{formatBusinessStatus(selectedOrder?.issueStatus)}</small></div>
        </div>

        <div className="admin-form-row" data-testid="ticket-order-actions">
          <input data-testid="ticket-entitlement-code" value={entitlementCode} onChange={(event) => setEntitlementCode(event.target.value)} placeholder={TEXT.entitlementCode} />
          <HighRiskConfirm
            label={TEXT.issue}
            impact="将对选中订单触发正式出票并写入审计留痕，出票后权益与核销随之生效。"
            disabled={busyAction !== ""}
            precheck={validateIssue}
            onConfirm={() => void issueOrder()}
            permissionsState={effectivePermissions}
            permissions={[TICKET_ISSUE_PERMISSION]}
            data-testid="ticket-order-issue"
          />
          <button type="button" data-testid="ticket-verification-preview" disabled={busyAction !== ""} onClick={() => void previewVerification()}>{TEXT.preview}</button>
          <span className="admin-muted-action" data-testid="ticket-order-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="ticket-orders-error" className="admin-inline-error">{formError}</div> : null}
        {faceBinding ? (
          <div className="admin-state" data-testid="ticket-face-binding">
            <strong>{TEXT.faceBindingTitle}</strong>
            <p>{faceBinding === "capture" ? TEXT.faceBindingCapture : TEXT.faceBindingMatch}</p>
          </div>
        ) : null}
        </>
        )}

        <div className="admin-form-row" data-testid="ticket-orders-filter">
          <input data-testid="ticket-orders-search" value={query} onChange={(event) => setQuery(event.target.value)} aria-label="搜索订单号 / 票种 / 渠道 / 支付/出票状态" placeholder="搜索订单号 / 票种 / 渠道 / 支付/出票状态" />
          <span className="admin-muted-action" data-testid="ticket-orders-filter-count">匹配 {filteredOrders.length} / {orders.length} 条</span>
        </div>
        {filteredOrders.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="ticket-orders-filter-empty">无匹配订单</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="ticket-orders-table">
            <thead>
              <tr><th>{TEXT.select}</th><th>{TEXT.orderNo}</th><th>{TEXT.ticketType}</th><th>{TEXT.quantity}</th><th>{TEXT.visitDate}</th><th>{TEXT.channel}</th><th>{TEXT.amount}</th><th>{TEXT.payStatus}</th><th>{TEXT.issueStatus}</th><th>{TEXT.status}</th><th>{TEXT.createdAt}</th><th>{TEXT.paidAt}</th><th>详情</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((order) => (
                <tr key={order.id} data-testid={`ticket-order-row-${order.id}`}
                  className={`admin-row-clickable${focusMatches(order) ? " admin-row-selected" : ""}`} tabIndex={0}
                  onClick={() => openOrderDetail(order)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openOrderDetail(order); } }}>
                  <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此订单 ${order.id}`} data-testid={`ticket-order-select-${order.id}`} checked={(selectedOrder?.id ?? "") === order.id} onChange={() => setSelectedOrderId(order.id)} /></td>
                  <td>{order.orderNo ?? order.id}</td>
                  <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-TKT-001" objectId={order.ticketTypeId}>{order.ticketTypeId ?? "-"}</TraceLink></td>
                  <td>{order.quantity ?? "-"}</td>
                  <td>{cnDate(order.visitDate)}</td>
                  <td>{formatBusinessStatus(order.salesChannel)}</td>
                  <td>{formatAmount(order.amountCent)}</td>
                  <td>{formatBusinessStatus(order.payStatus)}</td>
                  <td>{formatBusinessStatus(order.issueStatus)}</td>
                  <td>{formatBusinessStatus(order.status)}</td>
                  <td>{cnDate(order.createdAt)}</td>
                  <td data-testid={`ticket-order-paid-at-${order.id}`}>{cnDate(order.paidAt)}</td>
                <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看订单详情" onClick={(event) => { event.stopPropagation(); openOrderDetail(order); }}>查看详情</button></td></tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="ticket-orders-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
        {canVerificationView && !verificationsNotice ? (
          <div className="admin-section" data-testid="ticket-verification-panel">
            <h3>{TEXT.verificationTitle}</h3>
            <p className="admin-muted-action">{TEXT.verificationHint}</p>
            <div className="admin-form-row">
              <input
                data-testid="ticket-verification-anomaly-reason"
                value={anomalyReason}
                onChange={(event) => setAnomalyReason(event.target.value)}
                aria-label={TEXT.anomalyReasonLabel}
                placeholder={TEXT.anomalyReasonLabel}
              />
            </div>
            {verifications.length === 0 ? (
              <div className="admin-state admin-empty">{TEXT.verificationsEmpty}</div>
            ) : (
              <div className="admin-table-scroll">
                <table className="admin-table" data-testid="ticket-verifications-table">
                  <thead>
                    <tr><th>{TEXT.verificationNo}</th><th>{TEXT.status}</th><th>{TEXT.verificationReason}</th><th>{TEXT.verificationAt}</th><th>{TEXT.anomalyColumn}</th></tr>
                  </thead>
                  <tbody>
                    {pagedVerifications.pageRows.map((row) => (
                      <tr key={row.id} data-testid={`ticket-verification-row-${row.id}`}>
                        <td>{row.verificationNo ?? row.id}</td>
                        {/* ENUM:43 核销记录状态「拒绝（规则不允许）」；全局 rejected→已驳回 是审批口径，不改。 */}
                        <td>{row.status === "rejected" ? "拒绝" : formatBusinessStatus(row.status)}</td>
                        <td>{VERIFICATION_RESULT_REASON_LABELS[row.resultReason ?? ""] ?? row.resultReason ?? "—"}</td>
                        <td>{cnDate(row.createdAt)}</td>
                        <td>{anomalyCell(row)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <TablePager testid="ticket-verifications-pager" page={pagedVerifications.page} pageCount={pagedVerifications.pageCount}
                  total={pagedVerifications.total} onPrev={pagedVerifications.prev} onNext={pagedVerifications.next} />
              </div>
            )}
          </div>
        ) : null}
        <div className="admin-section" data-testid="ticket-refund-payout-panel">
          <h3>{TEXT.refundTitle}（{pendingPayoutRefunds.length}）</h3>
          <p className="admin-muted-action">{TEXT.refundHint}</p>
          {refundsNotice ? (
            <div className="admin-state" data-testid="ticket-refunds-degraded">{refundsNotice}</div>
          ) : (
            <>
              <div className="admin-form-row" data-testid="ticket-refund-payout-filter">
                <label>
                  <input
                    type="checkbox"
                    data-testid="ticket-refund-only-pending"
                    checked={onlyPendingPayout}
                    onChange={(event) => setOnlyPendingPayout(event.target.checked)}
                  />
                  {TEXT.refundOnlyPending}
                </label>
                <input
                  data-testid="ticket-refund-payout-result"
                  value={payoutResult}
                  onChange={(event) => setPayoutResult(event.target.value)}
                  aria-label={TEXT.refundResultLabel}
                  placeholder={TEXT.refundResultLabel}
                />
              </div>
              {visibleRefunds.length === 0 ? (
                <div className="admin-state admin-empty" data-testid="ticket-refunds-empty">{TEXT.refundEmpty}</div>
              ) : (
                <div className="admin-table-scroll">
                  <table className="admin-table" data-testid="ticket-refunds-table">
                    <thead>
                      <tr><th>{TEXT.refundNo}</th><th>{TEXT.refundOrder}</th><th>{TEXT.refundAmount}</th><th>{TEXT.refundChannel}</th><th>{TEXT.status}</th><th>{TEXT.refundRequestedBy}</th><th>{TEXT.refundApprovedBy}</th><th>{TEXT.refundCreatedAt}</th><th>操作</th></tr>
                    </thead>
                    <tbody>
                      {visibleRefunds.map((refund) => (
                        <tr key={refund.id} data-testid={`ticket-refund-row-${refund.id}`}>
                          <td>{refund.id}</td>
                          <td>{refund.targetId ?? "-"}</td>
                          <td>{formatAmount(refund.refundAmountCent)}</td>
                          <td>{refund.payoutItems?.length ? <span data-testid={`ticket-refund-payout-${refund.id}`}>{payoutText(refund)}</span>
                            : formatBusinessStatus(refund.refundChannel)}</td>
                          <td>{formatBusinessStatus(refund.status)}</td>
                          <td>{refund.requestedBy ?? "-"}</td>
                          <td>{refund.approvedBy ?? "-"}</td>
                          <td>{cnDate(refund.createdAt)}</td>
                          <td>
                            {refund.status === "approved" ? (
                              <HighRiskConfirm
                                label={TEXT.refundConfirm}
                                impact="确认后该笔退款记为「成功」并计入已退金额、冲减当日经营净收入，请务必先确认钱已实际打给客人。"
                                disabled={busyAction !== ""}
                                precheck={validatePayout}
                                onConfirm={() => void confirmRefundPayout(refund)}
                                permissionsState={effectivePermissions}
                                permissions={["ticket.refund.approve"]}
                                data-testid={`ticket-refund-confirm-payout-${refund.id}`}
                              />
                            ) : "—"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </div>
        <TicketRefundCasesPanel api={api} effectivePermissions={effectivePermissions} selectedOrderId={selectedOrder?.id}
          refunds={refunds} busy={busyAction !== ""} runAction={runAction} setFormError={setFormError} />
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
