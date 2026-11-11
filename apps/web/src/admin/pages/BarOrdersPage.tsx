// 文件版本：V0.14
import React from "react";
import { BarRefundBreakdownPanel } from "./BarRefundBreakdownPanel";
import { BarShiftPanel } from "./BarShiftPanel";
import { BarSuspendedOrdersBoard } from "./BarSuspendedOrdersBoard";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, BarBillRow, BarOrderItemRow, BarOrderItemVoidRow, BarOrderRow } from "../types";
import { commissionRateLabel, isSalesOrder, latestBill, salesRoleLabel } from "./barSalesRole";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { PermissionButton } from "../security/Can";
import { ApiError } from "../../shared/apiError";
import { TraceLink } from "../components/TraceLink";
import { usePagedRows, TablePager } from "../components/TablePager";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";

const TEXT = {
  ordered: "\u5df2\u70b9\u5355",
  billed: "\u5df2\u51fa\u8d26",
  delivered: "\u5df2\u51fa\u54c1",
  paid: "\u5df2\u652f\u4ed8",
  open: "\u8fdb\u884c\u4e2d",
  unset: "\u672a\u6807\u8bb0",
  loadFailed: "\u9152\u5427\u8ba2\u5355\u6570\u636e\u8bfb\u53d6\u5931\u8d25",
  noOrder: "\u5f53\u524d\u6ca1\u6709\u53ef\u64cd\u4f5c\u8ba2\u5355\u3002",
  actionFailed: "\u9152\u5427\u8ba2\u5355\u64cd\u4f5c\u5931\u8d25",
  billRequired: "\u9700\u5148\u751f\u6210\u8d26\u5355\u540e\u624d\u80fd\u7ed3\u7b97\u3002",
  title: "\u8ba2\u5355/\u51fa\u54c1/\u8d26\u5355",
  description: "\u628a\u9152\u5427\u70b9\u5355\u3001\u51fa\u54c1\u3001\u8d26\u5355\u3001\u7ed3\u7b97\u548c\u9500\u552e\u63d0\u6210\u53e3\u5f84\u653e\u5728\u540c\u4e00\u64cd\u4f5c\u9762\uff1b\u62c6\u5355\u3001\u5408\u5355\u3001\u6302\u8d26\u7ee7\u7eed\u6309\u589e\u5f3a POS \u80fd\u529b\u6269\u5c55\u3002",
  evidence: "\u63a5\u53e3\u8bc1\u636e\uff1a\u8bfb\u53d6 /api/v1/bar/orders\uff1b\u751f\u6210\u8d26\u5355 POST /api/v1/bar/orders/{order_id}/bill\uff1b\u51fa\u54c1 POST /api/v1/bar/orders/{order_id}/deliver\uff1b\u7ed3\u7b97 POST /api/v1/bar/bills/{bill_id}/settle\uff1b反结 POST /api/v1/bar/bills/{bill_id}/reopen\u3002",
  orderCount: "\u8ba2\u5355\u6570",
  currentRows: "\u5f53\u524d\u5217\u8868\u8bb0\u5f55",
  openOrders: "\u672a\u5b8c\u7ed3",
  openOrdersHelp: "\u672a\u652f\u4ed8\u6216\u672a\u53d6\u6d88",
  bill: "\u8d26\u5355",
  billHelp: "\u5df2\u6709\u5173\u8054\u8d26\u5355",
  commission: "\u9500\u552e\u63d0\u6210",
  commissionHelp: "\u5c5e\u9500\u552e\u5c97\u5f52\u5c5e",
  amount: "\u8ba2\u5355\u91d1\u989d",
  amountHelp: "\u4e0d\u542b\u901a\u7968\u6743\u76ca\u62b5\u6263",
  selectedOrder: "\u9009\u4e2d\u8ba2\u5355",
  noTable: "\u672a\u9009\u62e9\u5361\u5ea7",
  createBill: "\u751f\u6210\u8d26\u5355",
  deliver: "登记送达（已选明细）",
  settle: "\u7ed3\u7b97\u8d26\u5355",
  posBoundary: "\u62c6\u5355 / \u5408\u5355 / \u6302\u8d26 / \u4f4e\u6d88\u8865\u9f50\uff1aP1 \u589e\u5f3a POS\uff0c\u5f53\u524d\u9875\u4fdd\u7559\u8d26\u5355\u95ed\u73af\u5165\u53e3",
  select: "\u9009\u62e9",
  orderNo: "\u8ba2\u5355\u53f7",
  table: "\u5361\u5ea7",
  orderStatus: "\u8ba2\u5355\u72b6\u6001",
  billStatus: "\u8d26\u5355",
  commissionBasis: "\u63d0\u6210\u53e3\u5f84",
  time: "\u65f6\u95f4",
  createdAt: "\u4e0b\u5355\u65f6\u95f4",
  paidAt: "\u652f\u4ed8\u65f6\u95f4",
  noBill: "\u672a\u751f\u6210",
  billNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u8d26\u5355",
  deliverNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u51fa\u54c1",
  settleNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u7ed3\u7b97",
  focusClear: "\u6e05\u9664\u7b5b\u9009\uff0c\u8fd4\u56de\u5168\u90e8",
};

// KPI \u805a\u7126\u89c6\u56fe\uff08\u7167 OrganizationMerchantPage \u6837\u677f\uff09\uff1a\u70b9 KPI \u6570\u5b57=\u5e26 ?kpi= \u53c2\u6570\u91cd\u8fdb\u672c\u9875\uff0c\u53ea\u6e32\u67d3\u8ba2\u5355\u8868\u5e76\u6309\u5b50\u96c6\u8fc7\u6ee4\u3002
type KpiTarget = "orders" | "open";
function readKpiFocusFromUrl(): KpiTarget | null {
  if (typeof window === "undefined") return null;
  const target = new URLSearchParams(window.location.search).get("kpi");
  return target === "orders" || target === "open" ? target : null;
}
function isOpenOrder(order: BarOrderRow) {
  return !["paid", "cancelled"].includes(order.status ?? "");
}

const ORDER_STATUS_LABEL: Record<string, string> = {
  ordered: TEXT.ordered,
  billed: TEXT.billed,
  delivered: TEXT.delivered,
  paid: TEXT.paid,
  open: TEXT.open,
};

function displayStatus(value: string | undefined) {
  if (!value) return TEXT.unset;
  return ORDER_STATUS_LABEL[value] ?? value;
}

function formatAmount(amountCent: number | undefined) {
  const amount = Number(amountCent ?? 0) / 100;
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(amount);
}

type BillRewardRow = { id: string; points: number; growth: number; status: string };
const REWARD_STATUS: Record<string, string> = { pending: "待确认", confirmed: "已确认", cancelled: "已取消", reversed: "已冲回" };

function firstBill(order: BarOrderRow | undefined): BarBillRow | undefined {
  return latestBill(order);
}

function firstBillId(order: BarOrderRow | undefined) {
  return firstBill(order)?.id ?? "";
}

// V345（BAR-VOID-A1）退酒审批闸：先审后行 = 审批通过前不动库存/账单；即时生效 = 发起即生效、审批为事后抽检。
const VOID_APPROVAL_MODE_LABEL: Record<string, string> = {
  approval_first: "先审后行",
  immediate: "即时生效",
};
const VOID_STATUS_LABEL: Record<string, string> = {
  pending: "待审批",
  approved: "已通过",
  rejected: "已驳回",
  applied: "已生效",
};
const VOID_TYPE_LABEL: Record<string, string> = {
  cancel: "出品前撤单",
  return: "出品后退品",
};
// 第十六批第 16 条（ENUM「异常状态的统一出口」）：主管二选一处置异常账单。
const ANOMALY_RESOLUTION_LABEL = { restored: "更正后恢复", voided_reversed: "作废并冲正" } as const;
const ANOMALY_RESOLUTION_IMPACT = {
  restored: "账单回到登记争议之前的状态，原因进入审计留痕。",
  voided_reversed: "账单作废，已登记的收款流水冲正、不再计入营收与对账；已收款项按原渠道登记原路待退（储值部分当场退回原卡，其余挂退款由财务结清），本单已发的积分、成长值和销售提成一并冲回；作废后不能再收款。原因进入审计留痕。",
} as const;

/** V351 退酒原因码字典项（后台可增删改；上屏一律用中文名，英文码只作统计标识）。 */
type VoidReasonCodeRow = { code: string; labelZh: string; sortOrder?: number; status?: string };

/** 队列里把原因码翻成中文；字典里查不到（已下线/历史码）就原样显示，不吞信息。 */
function reasonLabel(codes: VoidReasonCodeRow[], code: string | undefined) {
  if (!code) return "—";
  return codes.find((c) => c.code === code)?.labelZh ?? code;
}

function voidLabel(map: Record<string, string>, value: string | undefined) {
  if (!value) return "—";
  return map[value] ?? value;
}

// 第十批 TK-26-R2：明细送达标注（BSM L1025「分别标注已上菜与未上菜」）。
function isUndelivered(item: BarOrderItemRow) {
  return item.deliveryStatus !== "delivered" && item.deliveryStatus !== "voided";
}
// 第十八批 T 段第 157 条 157-R2（第十八批裁决 37「只有包厢能挂起」；BSM:1106 收银员挂起须填原因）：包厢订单有未结清账单且未挂起时才可挂起。
function canSuspend(order: BarOrderRow | undefined) {
  return order?.tableType === "vip" && !order.suspensionStatus
    && (order.bills ?? []).some((bill) => bill.status === "unpaid" || bill.status === "partial_paid");
}
function suspensionLabel(order: BarOrderRow) {
  if (!order.suspensionStatus) return "—";
  return `${KEY_VALUE_LABELS.suspension_status?.[order.suspensionStatus] ?? order.suspensionStatus} · ${formatAmount(order.suspensionAmountCent)}`;
}

// 157-R5 ⑥：转坏账被拒（403）时显示后端中文原因（夜班值班经理此刻是否在班由后端按排班判），不套通用「无权」文案。
function backendForbiddenReason(error: unknown) {
  if (error instanceof ApiError && error.detail.status === 403) {
    try {
      const message = (JSON.parse(error.detail.raw ?? "") as { message?: unknown }).message;
      if (typeof message === "string" && message) return new Error(message);
    } catch {
      // 非 JSON 照通用文案。
    }
  }
  return error;
}

function deliveryLabel(item: BarOrderItemRow) {
  if (item.deliveryStatus === "delivered") return "已上菜";
  if (item.deliveryStatus === "voided") return "已撤单";
  return "未上菜";
}

export function BarOrdersPage({ api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo, reportViewedObjects }: AdminPageProps) {
  const [orders, setOrders] = React.useState<BarOrderRow[]>([]);
  // 第 23 条 23-R5：敏感页这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）。
  React.useEffect(() => reportViewedObjects?.("bar_order", orders), [orders, reportViewedObjects]);
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);
  // 聚焦时对订单表做客户端筛选：仅过滤已加载的内存数组，不发起后端查询。
  const visibleOrders = kpiFocus === "open" ? orders.filter(isOpenOrder) : orders;
  const pagedOrders = usePagedRows(visibleOrders, 10, `${visibleOrders.length}|${kpiFocus ?? ""}`);
  const { selectedId: selectedOrderId, setSelectedId: setSelectedOrderId, selected: selectedOrder } = useSelectedRow(orders);
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");
  const [paymentChannel, setPaymentChannel] = React.useState("aggregate_pay");
  const [paymentAmount, setPaymentAmount] = React.useState("");
  const [paymentReference, setPaymentReference] = React.useState("");
  const paymentRetry = React.useRef<{ body: string; key: string } | null>(null);
  const [posTransactionNo, setPosTransactionNo] = React.useState("");
  const [paymentPhone, setPaymentPhone] = React.useState("");
  const [paymentSmsCode, setPaymentSmsCode] = React.useState("");
  // 第十批 TK-28（ENUM L81「商户折扣券——到对应商户抵扣」；操作员裁决「一次结账一张」）：结账带一张折扣券码；结账后回显账单折扣合计。
  const [voucherCode, setVoucherCode] = React.useState("");
  // 第十一批 TK-04-R2（BSM L822「由收银在收款时现场录入会员号累计」；PGM L878 会员号即注册手机号）：选填，后端走客人查询专门通道留痕。
  const [memberPhone, setMemberPhone] = React.useState("");
  const [voucherDiscountCent, setVoucherDiscountCent] = React.useState<number | null>(null);
  // 行明细抽屉：整行可点 → 该订单完整字段（状态/提成口径中文·金额¥·下单/支付时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // V345（BAR-VOID-A1）退酒申请 + 待审队列。
  const [voidQueue, setVoidQueue] = React.useState<BarOrderItemVoidRow[]>([]);
  const [voidOrderItemId, setVoidOrderItemId] = React.useState("");
  const [voidReason, setVoidReason] = React.useState("");
  const [voidReasonCode, setVoidReasonCode] = React.useState("");
  const [voidReturnToStock, setVoidReturnToStock] = React.useState(false);
  const [voidReviewReason, setVoidReviewReason] = React.useState("");
  // V351 退酒原因码字典：店员从中文下拉里选，不再手打英文短码。
  const [reasonCodes, setReasonCodes] = React.useState<VoidReasonCodeRow[]>([]);
  const [newReasonCode, setNewReasonCode] = React.useState("");
  const [newReasonLabel, setNewReasonLabel] = React.useState("");
  const [archiveReason, setArchiveReason] = React.useState("");
  // 双证据闭环：证据附件由真上传产生（不再手打 ID）。这里存已上传附件的 id + 文件名。
  const [voidEvidence, setVoidEvidence] = React.useState<{ id: string; fileName: string }[]>([]);
  const [uploading, setUploading] = React.useState(false);
  // 第十批 TK-26-R2（BSM L1025「分别标注已上菜与未上菜」）：按明细勾选本次送达，默认勾选全部未上菜明细；
  // 只记操作人取消勾选的明细（按订单归零），换订单即恢复默认全选。
  const [deliverSkip, setDeliverSkip] = React.useState<{ orderId: string; ids: string[] }>({ orderId: "", ids: [] });
  // 第十六批第 16 条：客人账单争议登记与主管处置共用一个原因输入。
  const [anomalyReason, setAnomalyReason] = React.useState("");
  // 第十八批 AA 段第 202 条 202-R2：反结原因（被拦下送审后原样再点要用同一原因，失败不清空）与送审说明。
  const [reopenReason, setReopenReason] = React.useState("");
  const [reopenNote, setReopenNote] = React.useState("");
  const [suspendReason, setSuspendReason] = React.useState("");
  const [badDebtReason, setBadDebtReason] = React.useState("");
  // 第十八批 T 段第 157 条 157-R4：转坏账含高价值已出品商品时的现场照片、视频（上传后拿回附件 ID）。
  const [badDebtEvidence, setBadDebtEvidence] = React.useState<{ id: string; fileName: string; video: boolean }[]>([]);
  // 第十八批 T 段第 157 条 157-R6：挂起订单发起取消（勾选可回库的已出品明细、原因、原因码与证据）与经理审批意见。
  const [cancelItemIds, setCancelItemIds] = React.useState<string[]>([]);
  const [cancelReason, setCancelReason] = React.useState("");
  const [cancelReasonCode, setCancelReasonCode] = React.useState("");
  const [cancelEvidence, setCancelEvidence] = React.useState<{ id: string; fileName: string; video: boolean }[]>([]);
  const [cancelDecisionReason, setCancelDecisionReason] = React.useState("");
  const selectedBill = firstBill(selectedOrder);
  const orderItems: BarOrderItemRow[] = selectedOrder?.items ?? [];
  const cancelableItems = orderItems.filter((item) => item.deliveryStatus === "delivered");
  // 157-R5 ⑥：关台未付清待办已升级的单不要求权限码（被升级的店长、夜班值班经理等能进本页的用户都可点，由后端按 bar.bill.hang_escalation 或夜班值班经理排班鉴权）；
  // 未升级的只认 bar.bill.hang_cancel（18T 复核修复③：与后端 requireBadDebtAuthority 一致，BSM:1109「经理未及时确认时，任务升级给店长或夜班值班经理」）。
  const badDebtPermissions = selectedOrder?.unpaidCloseEscalatedAt ? [] : ["bar.bill.hang_cancel"];
  const skippedItemIds = deliverSkip.orderId === (selectedOrder?.id ?? "") ? deliverSkip.ids : [];
  const isDeliverChecked = (item: BarOrderItemRow) => isUndelivered(item) && !skippedItemIds.includes(item.id);

  function openOrderDetail(order: BarOrderRow) {
    const bill = firstBill(order);
    setDetail({
      title: `订单明细 · ${order.id}`,
      fields: [
        { label: TEXT.orderNo, value: order.id },
        { label: TEXT.table, value: order.tableId ?? "—" },
        { label: TEXT.orderStatus, value: displayStatus(order.status) },
        { label: TEXT.billStatus, value: bill?.id ? `${bill.id} / ${formatBusinessStatus(bill.status)}` : TEXT.noBill },
        { label: TEXT.amount, value: formatAmount(order.totalAmountCent ?? order.totalAmount) },
        { label: TEXT.commissionBasis, value: `${salesRoleLabel(order)} · ${commissionRateLabel(order)}` },
        { label: TEXT.createdAt, value: cnDate(order.createdAt) },
        { label: TEXT.paidAt, value: cnDate(bill?.paidAt ?? order.paidAt) },
      ],
    });
    // 第十一批 TK-04-R5（ENUM L77「从消费单据可以反查该单产生的积分与成长值记录」）：账单详情显示本单商户消费奖励结果。
    if (!bill?.id) return;
    const title = `订单明细 · ${order.id}`;
    const append = (value: string) => setDetail((current) => (current && current.title === title
      ? { ...current, fields: [...current.fields, { label: "本单消费奖励", value }] } : current));
    api.get<BillRewardRow[]>(`/api/v1/bar/bills/${bill.id}/rewards`)
      .then((rows) => append(rows.length === 0 ? "本单未产生消费积分或成长值"
        : rows.map((r) => `${r.points} 积分 · ${r.growth} 成长值 · ${REWARD_STATUS[r.status] ?? r.status}`).join("；")))
      .catch(() => append("消费奖励暂时加载失败，可稍后重开明细"));
  }

  React.useEffect(() => {
    const onPopState = () => setKpiFocus(readKpiFocusFromUrl());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  function enterKpiFocus(target: KpiTarget) {
    const params = new URLSearchParams(window.location.search);
    params.set("kpi", target);
    window.history.pushState(window.history.state, "", `${window.location.pathname}?${params.toString()}`);
    setKpiFocus(target);
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
      const rows = await api.get<BarOrderRow[]>("/api/v1/bar/orders");
      setOrders(rows);
      // 待审退酒队列与订单列表同批加载；无权限/接口不可用不阻断本页主流程（队列显示为空）。
      try {
        setVoidQueue(await api.get<BarOrderItemVoidRow[]>("/api/v1/bar/order-item-voids?status=pending"));
      } catch {
        setVoidQueue([]);
      }
      // V351 退酒原因码字典（中文下拉数据源）；不可用时下拉为空，不阻断本页主流程。
      try {
        setReasonCodes(await api.get<VoidReasonCodeRow[]>("/api/v1/bar/void-reason-codes"));
      } catch {
        setReasonCodes([]);
      }
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const openOrders = orders.filter((order) => !["paid", "cancelled"].includes(order.status ?? "")).length;
  const billCount = orders.filter((order) => firstBill(order)).length;
  // 列表接口返回 totalAmountCent（旧 totalAmount 字段兼容保留）：此前读幽灵字段恒为 0。
  const totalAmount = orders.reduce((sum, order) => sum + Number(order.totalAmountCent ?? order.totalAmount ?? 0), 0);
  const salesCommissionOrders = orders.filter((order) => isSalesOrder(order)).length;

  // 第二批接线（指标登记簿）：订单数/未完结/订单金额三个 KPI 数字改走统一明细端点（与明细页同源，全历史口径
  // 与本页全量列表一致）；端点失败（含无权限）→ 回退本地列表计算并隐藏「完整明细」入口，体验不降级。
  const orderCountKpi = useMetricKpiValue(api, "bar.order_count", {});
  const openOrderKpi = useMetricKpiValue(api, "bar.open_order_count", {});
  const orderAmountKpi = useMetricKpiValue(api, "bar.order_amount", {});
  const orderCountDisplay = orderCountKpi.status === "ready" && orderCountKpi.value !== null ? orderCountKpi.value : orders.length;
  const openOrdersDisplay = openOrderKpi.status === "ready" && openOrderKpi.value !== null ? openOrderKpi.value : openOrders;
  const orderAmountDisplay = orderAmountKpi.status === "ready" && orderAmountKpi.value !== null ? orderAmountKpi.value : totalAmount;

  async function runOrderAction(action: string, request: () => Promise<unknown>) {
    if (!selectedOrder) {
      setFormError(TEXT.noOrder);
      return;
    }
    setBusyAction(action);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await request();
      setActionResult({ action, orderId: selectedOrder.id, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : TEXT.actionFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action, orderId: selectedOrder.id, status: "failed", message });
    } finally {
      setBusyAction("");
    }
  }

  async function createBill() {
    await runOrderAction("bar_bill_created", () => api.post(`/api/v1/bar/orders/${selectedOrder!.id}/bill`, { operatorNote: TEXT.billNote }));
  }

  async function markDelivered() {
    const selectedItems = orderItems.filter(isDeliverChecked);
    if (selectedItems.length === 0) {
      setFormError("请先勾选本次实际送达的未上菜明细。");
      return;
    }
    await runOrderAction("bar_order_delivered", () => api.post(`/api/v1/bar/orders/${selectedOrder!.id}/deliver`, {
      operatorNote: TEXT.deliverNote,
      deliveryPhotoAttachmentIds: ["att-admin-delivery-1"],
      deliveredItems: selectedItems.map((item) => ({ orderItemId: item.id, quantity: item.quantity ?? 1 })),
    }));
  }

  function validateSettle() {
    if (!selectedOrder) {
      setFormError(TEXT.noOrder);
      return false;
    }
    if (!firstBillId(selectedOrder)) {
      setFormError(TEXT.billRequired);
      return false;
    }
    if (paymentChannel !== "stored_value" && !paymentReference.trim()) {
      setFormError("请填写本次真实付款凭据编号。"); return false;
    }
    if (paymentAmount.trim() && (!/^\d+(\.\d{1,2})?$/.test(paymentAmount.trim()) || Number(paymentAmount) <= 0 || !Number.isSafeInteger(Math.round(Number(paymentAmount) * 100)))) {
      setFormError("本次金额须为正数，最多两位小数；留空按账单剩余应收结清。"); return false;
    }
    if (paymentChannel === "stored_value" && (!paymentPhone.trim() || !paymentSmsCode.trim())) {
      setFormError("请填写付款手机号和短信验证码。");
      return false;
    }
    if (paymentChannel === "pos" && !posTransactionNo.trim()) {
      setFormError("POS 结算需填 POS 流水号，供对账留痕。");
      return false;
    }
    return true;
  }

  async function requestStoredValueCode() {
    const billId = firstBillId(selectedOrder);
    if (!billId || !paymentPhone.trim()) {
      setFormError("请先选择账单并填写付款手机号。");
      return;
    }
    setBusyAction("stored_value_sms");
    setFormError("");
    try {
      await api.post(`/api/v1/bar/bills/${billId}/stored-value/sms`, { phone: paymentPhone.trim() });
      setActionResult({ message: "验证码已发送，请由付款客人确认本账单扣款。" });
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "验证码发送失败");
    } finally {
      setBusyAction("");
    }
  }

  async function settleBill() {
    const billId = firstBillId(selectedOrder);
    if (!billId) {
      setFormError(TEXT.billRequired);
      return;
    }
    const body = {
      paymentId: paymentChannel === "stored_value" ? "stored-value:" + billId : paymentReference.trim(),
      ...(paymentChannel === "stored_value" ? { phone: paymentPhone.trim(), smsCode: paymentSmsCode.trim() } : {}),
      ...(paymentAmount.trim() ? { settlementAmountCent: Math.round(Number(paymentAmount) * 100) } : {}),
      paymentChannel,
      posTransactionNo: paymentChannel === "pos" ? posTransactionNo.trim() || undefined : undefined,
      ...(voucherCode.trim() ? { voucherCode: voucherCode.trim() } : {}),
      ...(memberPhone.trim() ? { memberPhone: memberPhone.trim() } : {}),
    };
    const serialized = JSON.stringify({ billId, ...body });
    if (paymentRetry.current?.body !== serialized) paymentRetry.current = { body: serialized, key: crypto.randomUUID() };
    await runOrderAction("bar_bill_settled", async () => {
      const result = await api.post<{ discountAmountCent?: number }>(`/api/v1/bar/bills/${billId}/settle`, { ...body, idempotencyKey: paymentRetry.current!.key });
      paymentRetry.current = null; setPaymentAmount(""); setPaymentReference(""); setMemberPhone("");
      if (voucherCode.trim()) { setVoucherDiscountCent(result?.discountAmountCent ?? null); setVoucherCode(""); }
      return result;
    });
  }

  // ==== 第十六批第 16 条（ENUM「账单状态」异常；操作员裁决补充第 2 条）：客人争议登记置异常，主管「更正后恢复」或「作废并冲正」 ====
  function validateAnomalyReason() {
    if (!selectedBill?.id) {
      setFormError("请先选择已生成账单的订单。");
      return false;
    }
    if (!anomalyReason.trim()) {
      setFormError("请先填写争议或处置原因，进入审计留痕。");
      return false;
    }
    return true;
  }

  async function raiseBillAnomaly() {
    if (!validateAnomalyReason()) return;
    await runOrderAction("bar_bill_anomaly_raised", () => api.post(`/api/v1/bar/bills/${selectedBill!.id}/anomaly`,
      { reason: anomalyReason.trim() }));
  }

  async function suspendOrder() {
    if (!suspendReason.trim()) {
      setFormError("挂起须填写原因，进入审计留痕。");
      return;
    }
    await runOrderAction("bar_order_suspended", () => api.post(`/api/v1/bar/orders/${selectedOrder!.id}/suspend`,
      { reason: suspendReason.trim() }));
  }

  // 第十八批 T 段第 157 条 157-R3（BSM:1109「经理确认最终无法收款后转为坏账损耗处理中」；BSM:1111「财务确认……均完成后，订单转为已核销结案」）。
  async function confirmBadDebt() {
    if (!badDebtReason.trim()) {
      setFormError("确认无法收款须填写原因，进入审计留痕。");
      return;
    }
    // 157-R4（BSM:1112「达到既有高价值标准的商品仍执行原证据和审批要求」）：上传了才带照片、视频两栏；是否高价值由后端判，命中即转主管复核。
    const photos = badDebtEvidence.filter((e) => !e.video).map((e) => e.id);
    const videos = badDebtEvidence.filter((e) => e.video).map((e) => e.id);
    await runOrderAction("bar_order_suspension_bad_debt", () => api.post(`/api/v1/bar/orders/${selectedOrder!.id}/suspension/bad-debt`,
      { reason: badDebtReason.trim(), photoAttachmentIds: photos.length ? photos : undefined, videoAttachmentIds: videos.length ? videos : undefined })
      .catch((error: unknown) => { throw backendForbiddenReason(error); }));
  }

  async function writeOffSuspension() {
    await runOrderAction("bar_order_suspension_written_off", () => api.post(`/api/v1/bar/orders/${selectedOrder!.id}/suspension/write-off`, {}));
  }

  // 第十八批 T 段第 157 条 157-R6（BSM:1107「已出品挂起订单取消须经理审批。能回库的商品继续执行既有退菜、退酒和高价值退品证据审批链」；BSM:1109「不可回库的
  // 已出品商品不得走普通取消」）：只能勾选本单已出品的明细，所列即认定能回库的；是否高价值由后端判，命中须原因码与证据。
  async function requestSuspensionCancel() {
    const picked = cancelItemIds.filter((id) => cancelableItems.some((item) => item.id === id));
    if (picked.length === 0) {
      setFormError("请勾选要取消的已出品、能回库的商品；不可回库的走确认无法收款（转坏账）。");
      return;
    }
    if (!cancelReason.trim()) {
      setFormError("发起取消须填写原因，进入审计留痕。");
      return;
    }
    await runOrderAction("bar_order_suspension_cancel_requested", () => api.post(`/api/v1/bar/orders/${selectedOrder!.id}/suspension/cancel`,
      { orderItemIds: picked, reason: cancelReason.trim(), reasonCode: cancelReasonCode || undefined, evidenceAttachmentIds: cancelEvidence.map((e) => e.id) }));
  }

  // 157-R6（PGM:699「经理审批取消」；BSM:1108「取消审批生效与付款成功只能有一项先完成，先完成者生效」）。
  async function decideSuspensionCancel(decision: "approve" | "reject") {
    await runOrderAction("bar_order_suspension_cancel_decided", () => api.post(`/api/v1/bar/orders/${selectedOrder!.id}/suspension/cancel-decision`,
      { decision, reason: cancelDecisionReason.trim() || undefined }));
  }

  async function resolveBillAnomaly(resolution: keyof typeof ANOMALY_RESOLUTION_LABEL) {
    await runOrderAction("bar_bill_anomaly_resolved", () => api.post(`/api/v1/bar/bills/${selectedBill!.id}/anomaly/resolve`,
      { resolution, reason: anomalyReason.trim() }));
  }

  // ==== 第十八批 AA 段第 202 条 202-R2（BSM:1069「反结（重开已结账单）……均需主管审批留痕」；第十八批裁决 39）：
  // 首次提交被准入闸拦下（ADMISSION_BLOCKED）并生成主管复核单，主管批准后原样再点一次才重开 ====
  function validateReopen() {
    if (!selectedBill?.id) {
      setFormError("请先选择已生成账单的订单。");
      return false;
    }
    if (!reopenReason.trim()) {
      setFormError("反结须填写原因，存进账单的反结原因与反结操作记录，不写入审计事件。");
      return false;
    }
    return true;
  }

  async function reopenBill() {
    setReopenNote("");
    await runOrderAction("bar_bill_reopened", () => api.post(`/api/v1/bar/bills/${selectedBill!.id}/reopen`, { reason: reopenReason.trim() })
      .catch((error: unknown) => {
        if (error instanceof ApiError && error.detailCode === "ADMISSION_BLOCKED") {
          setReopenNote("已提交主管审批：主管在「主管复核」页批准后，请原样再点一次「反结」。");
        }
        throw error;
      }));
  }

  // ==== V345（BAR-VOID-A1）退酒申请 / 审批 ====
  function validateVoidRequest() {
    if (!selectedOrder) {
      setFormError(TEXT.noOrder);
      return false;
    }
    if (!voidOrderItemId.trim()) {
      setFormError("请填写要退的订单明细 ID。");
      return false;
    }
    if (!voidReason.trim()) {
      setFormError("退菜退酒必须填写原因，存进退菜退酒单，不写入审计事件。");
      return false;
    }
    return true;
  }

  async function submitVoidRequest() {
    await runOrderAction("bar_order_item_void_requested", () => api.post(
      `/api/v1/bar/orders/${selectedOrder!.id}/void-item`,
      {
        orderItemId: voidOrderItemId.trim(),
        reason: voidReason.trim(),
        reasonCode: voidReasonCode || undefined,
        evidenceAttachmentIds: voidEvidence.map((e) => e.id),
        returnToStock: voidReturnToStock,
        billId: firstBillId(selectedOrder) || undefined,
      },
    ));
  }

  /** 现场拍的照片/视频直接上传，拿回真实附件 ID —— 不再让店员手打附件 ID。 */
  async function uploadEvidenceFiles(files: FileList | null, businessType = "bar_order_item_void",
    onUploaded: (rows: { id: string; fileName: string; video: boolean }[]) => void = (rows) => setVoidEvidence((prev) => [...prev, ...rows])) {
    if (!files || files.length === 0) return;
    setUploading(true);
    setFormError("");
    try {
      const uploaded: { id: string; fileName: string; video: boolean }[] = [];
      for (const file of Array.from(files)) {
        const form = new FormData();
        form.append("file", file);
        form.append("business_type", businessType);
        form.append("attachment_role", file.type.startsWith("video") ? "video" : "photo");
        if (!api.postForm) throw new Error("当前客户端不支持文件上传。");
        const saved = await api.postForm<{ id: string; fileName?: string }>(
          "/api/v1/evidence/attachments/upload", form);
        uploaded.push({ id: saved.id, fileName: saved.fileName ?? file.name, video: file.type.startsWith("video") });
      }
      onUploaded(uploaded);
    } catch (uploadError) {
      const message = uploadError instanceof Error ? uploadError.message : "证据上传失败。";
      setFormError(message);
    } finally {
      setUploading(false);
    }
  }

  /** 原因码字典维护：新增/改中文名（增改同一入口），或下线一条（必须填理由）。 */
  async function saveReasonCode() {
    if (!newReasonCode.trim() || !newReasonLabel.trim()) {
      setFormError("原因码与中文名都要填。");
      return;
    }
    setBusyAction("bar_void_reason_code_saved");
    setFormError("");
    try {
      await api.post("/api/v1/bar/void-reason-codes", {
        idempotencyKey: `reason-save-${newReasonCode.trim()}-${Date.now()}`,
        code: newReasonCode.trim(),
        labelZh: newReasonLabel.trim(),
        sortOrder: (reasonCodes.length + 1) * 10,
      });
      setReasonCodes(await api.get<VoidReasonCodeRow[]>("/api/v1/bar/void-reason-codes"));
      setNewReasonCode("");
      setNewReasonLabel("");
    } catch (saveError) {
      setFormError(saveError instanceof Error ? saveError.message : TEXT.actionFailed);
    } finally {
      setBusyAction("");
    }
  }

  async function archiveReasonCodeEntry(code: string) {
    if (!archiveReason.trim()) {
      setFormError("下线原因码必须填写理由（软删留痕）。");
      return;
    }
    setBusyAction("bar_void_reason_code_archived");
    setFormError("");
    try {
      await api.post(`/api/v1/bar/void-reason-codes/${code}/archive`, {
        idempotencyKey: `reason-archive-${code}-${Date.now()}`,
        archiveReason: archiveReason.trim(),
      });
      setReasonCodes(await api.get<VoidReasonCodeRow[]>("/api/v1/bar/void-reason-codes"));
      if (voidReasonCode === code) setVoidReasonCode("");
      setArchiveReason("");
    } catch (archiveError) {
      setFormError(archiveError instanceof Error ? archiveError.message : TEXT.actionFailed);
    } finally {
      setBusyAction("");
    }
  }

  // 审批动作不依赖「选中订单」：主管从待审队列直接决策（队列行自带订单/明细上下文）。
  async function decideVoid(voidId: string, decision: "approved" | "rejected") {
    const action = `bar_order_item_void_${decision}`;
    setBusyAction(action);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.post(`/api/v1/bar/order-item-voids/${voidId}/decision`,
        { decision, reason: voidReviewReason.trim() || undefined });
      setActionResult({ action, voidId, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : TEXT.actionFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action, voidId, status: "failed", message });
    } finally {
      setBusyAction("");
    }
  }

  return (
    <section data-testid="bar-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="bar-orders-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="bar.order.read / bar.order.bill / bar.order.deliver / bar.bill.settle" />
      {/* 第十八批 P 段第 133 条 133-R1：收银交班对账区块放在订单列表的读取／空态之外——当日第一张单之前就要能开班。 */}
      <BarShiftPanel api={api} user={user} effectivePermissions={effectivePermissions} setActionResult={setActionResult} setGlobalError={setGlobalError} />
      {/* 第十八批 T 段第 157 条 157-R7：挂起订单风险看板（BSM:1113），同样放在订单列表的读取／空态之外。 */}
      <BarSuspendedOrdersBoard api={api} effectivePermissions={effectivePermissions} setActionResult={setActionResult} setGlobalError={setGlobalError} />
      <AdminDataState loading={loading} error={error} empty={orders.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <div className="admin-state" data-testid="bar-orders-kpi-focus-banner">
            正在查看：{kpiFocus === "open" ? TEXT.openOrders : TEXT.orderCount}（{visibleOrders.length}）
            <button type="button" data-testid="bar-orders-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
          </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="bar-orders-summary">
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.orderCount}</span><button type="button" className="admin-kpi-value" data-testid="bar-orders-kpi-orders" onClick={() => enterKpiFocus("orders")}>{orderCountDisplay}</button><small>{TEXT.currentRows}{orderCountKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="bar.order_count" testid="bar-orders-metric-detail-orders" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.openOrders}</span><button type="button" className="admin-kpi-value" data-testid="bar-orders-kpi-open" onClick={() => enterKpiFocus("open")}>{openOrdersDisplay}</button><small>{TEXT.openOrdersHelp}{openOrderKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="bar.open_order_count" testid="bar-orders-metric-detail-open" /></> : null}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.bill}</span><strong>{billCount}</strong><small>{TEXT.billHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.commission}</span><strong>{salesCommissionOrders}</strong><small>{TEXT.commissionHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.amount}</span><strong data-testid="bar-orders-kpi-amount">{formatAmount(orderAmountDisplay)}</strong><small>{TEXT.amountHelp}{orderAmountKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="bar.order_amount" testid="bar-orders-metric-detail-amount" /></> : null}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedOrder}</span><strong className="admin-kpi-value--text" title={selectedOrder?.id ?? "-"}>{selectedOrder?.id ?? "-"}</strong><small>{selectedOrder?.tableId ?? TEXT.noTable}</small></div>
        </div>

        <div className="admin-form-grid" data-testid="bar-settle-payment-form">
          <label>本次收款金额（元，可留空结清）<input aria-label="本次收款金额" inputMode="decimal" value={paymentAmount} onChange={e => setPaymentAmount(e.target.value)} /></label>
          {paymentChannel !== "stored_value" ? <label>本次付款凭据编号<input aria-label="本次付款凭据编号" value={paymentReference} onChange={e => setPaymentReference(e.target.value)} /></label> : null}
          <p>金额留空由系统按账单真实剩余应收结清，包含服务费及代收小费；可分别登记各渠道实际付款，累计足额后才完成整单。</p>
          <label>结算方式<select data-testid="bar-settle-channel" value={paymentChannel} onChange={(e) => setPaymentChannel(e.target.value)}>
            <option value="aggregate_pay">聚合支付</option>
            <option value="stored_value">储值余额（手机号验证）</option>
            <option value="cash">现金</option>
            <option value="pos">POS 刷卡</option>
            <option value="bank_transfer">银行转账</option>
            <option value="other">其他</option>
          </select></label>
          {paymentChannel === "stored_value" ? <>
            <label>付款手机号<input type="tel" autoComplete="off" value={paymentPhone} onChange={(e) => setPaymentPhone(e.target.value)} /></label>
            <PermissionButton permissionsState={effectivePermissions} permissions={["finance.payment.register"]} disabled={busyAction !== ""} onClick={() => void requestStoredValueCode()}>发送扣款验证码</PermissionButton>
            <label>短信验证码<input inputMode="numeric" autoComplete="one-time-code" value={paymentSmsCode} onChange={(e) => setPaymentSmsCode(e.target.value)} /></label>
            <p>从付款客人在本商户的储值卡扣除本账单剩余应付金额，短信验证通过后才会扣款。</p>
          </> : null}
          {paymentChannel === "pos" ? (
            <label>POS 流水号<input data-testid="bar-settle-pos-no" value={posTransactionNo} onChange={(e) => setPosTransactionNo(e.target.value)} placeholder="POS 交易流水号" /></label>
          ) : null}
          <label>折扣券码（选填，一次结账一张）<input data-testid="bar-settle-voucher-code" value={voucherCode} onChange={(e) => setVoucherCode(e.target.value)} placeholder="本商户折扣券核销码" /></label>
          <p>折扣券按面值抵扣本次应付，面值超出部分不找零；多张券请分次结账各带一张。</p>
          <label>会员手机号（选填）<input aria-label="会员手机号（选填）" type="tel" autoComplete="off" value={memberPhone} onChange={(e) => setMemberPhone(e.target.value)} placeholder="客人注册手机号，累计积分与成长值" /></label>
          {voucherDiscountCent !== null ? <p data-testid="bar-settle-voucher-result">折扣券已抵扣，账单折扣合计 {formatAmount(voucherDiscountCent)}</p> : null}
        </div>
        <div className="admin-form-row" data-testid="bar-order-actions">
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.order.create"]} data-testid="bar-order-create-bill" disabled={busyAction !== ""} onClick={() => void createBill()}>{TEXT.createBill}</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.order.create"]} data-testid="bar-order-deliver" disabled={busyAction !== ""} onClick={() => void markDelivered()}>{TEXT.deliver}</PermissionButton>
          <HighRiskConfirm
            label={TEXT.settle}
            impact="将对选中账单发起资金结算并写入支付/结算留痕，结算后不可撤销。"
            disabled={busyAction !== ""}
            precheck={validateSettle}
            onConfirm={() => void settleBill()}
            permissionsState={effectivePermissions}
            permissions={["finance.payment.register"]}
            data-testid="bar-order-settle"
          />
          <span className="admin-muted-action" data-testid="bar-order-pos-boundary">{TEXT.posBoundary}</span>
        </div>
        <div className="admin-table-scroll" data-testid="bar-order-items">
          <table className="admin-table">
            <thead><tr><th>本次送达</th><th>物品</th><th>数量</th><th>金额</th><th>上菜状态</th></tr></thead>
            <tbody>{orderItems.length === 0 ? (
              <tr><td colSpan={5} className="admin-muted-action">选中订单没有明细</td></tr>
            ) : orderItems.map((item) => (
              <tr key={item.id} data-testid={`bar-order-item-row-${item.id}`}>
                <td>{isUndelivered(item) ? <input type="checkbox" aria-label={`本次送达 ${item.productName ?? item.id}`} data-testid={`bar-order-item-check-${item.id}`} checked={isDeliverChecked(item)} onChange={(e) => setDeliverSkip({ orderId: selectedOrder?.id ?? "", ids: e.target.checked ? skippedItemIds.filter((id) => id !== item.id) : [...skippedItemIds, item.id] })} /> : null}</td>
                <td>{item.productName ?? item.id}</td>
                <td>{item.quantity ?? "—"}</td>
                <td>{formatAmount(item.amountCent)}</td>
                <td>{deliveryLabel(item)}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <BarRefundBreakdownPanel api={api} effectivePermissions={effectivePermissions} setActionResult={setActionResult} setGlobalError={setGlobalError} billId={firstBillId(selectedOrder)} onChanged={reload} />
        <div className="admin-section-title">账单争议与异常处置</div>
        <p className="admin-muted-action" data-testid="bar-bill-anomaly-hint">客人对账单提出争议时，填写争议原因后登记，账单即为「异常」，异常期间不能收款、不能反结；之后由主管选「更正后恢复」（回到登记前的状态）或「作废并冲正」（账单作废、已登记的收款冲正），两种处置都须填写原因并留痕。</p>
        <div className="admin-form-row" data-testid="bar-bill-anomaly-form">
          <span data-testid="bar-bill-anomaly-status">选中账单：{selectedBill?.id ? `${selectedBill.id} / ${formatBusinessStatus(selectedBill.status)}` : TEXT.noBill}</span>
          <label>争议或处置原因<input aria-label="争议或处置原因" data-testid="bar-bill-anomaly-reason" value={anomalyReason} onChange={(e) => setAnomalyReason(e.target.value)} placeholder="例如：客人称未点第二瓶香槟" /></label>
          {selectedBill?.status === "abnormal" ? (["restored", "voided_reversed"] as const).map((resolution) => (
            <HighRiskConfirm
              key={resolution}
              label={ANOMALY_RESOLUTION_LABEL[resolution]}
              impact={ANOMALY_RESOLUTION_IMPACT[resolution]}
              disabled={busyAction !== ""}
              precheck={validateAnomalyReason}
              onConfirm={() => void resolveBillAnomaly(resolution)}
              permissionsState={effectivePermissions}
              permissions={["bar.bill.anomaly.resolve"]}
              data-testid={`bar-bill-anomaly-${resolution}`}
            />
          )) : (
            <PermissionButton permissionsState={effectivePermissions} permissions={["bar.bill.adjust"]} data-testid="bar-bill-anomaly-raise" disabled={busyAction !== "" || selectedBill?.status === "void"} onClick={() => void raiseBillAnomaly()}>登记争议（置为异常）</PermissionButton>
          )}
        </div>
        <div className="admin-section-title">反结（重开已结账单）</div>
        <p className="admin-muted-action" data-testid="bar-bill-reopen-hint">反结须主管审批：首次提交生成主管复核单，主管批准后原样再点一次，账单才重开为未付，之后重新登记收款。储值部分在反结生效时退回原卡，其余收款作废后由收银按更正后的账单重新收款。</p>
        <div className="admin-form-row" data-testid="bar-bill-reopen-form">
          <span data-testid="bar-bill-reopen-status">选中账单：{selectedBill?.id ? `${selectedBill.id} / ${formatBusinessStatus(selectedBill.status)}` : TEXT.noBill}</span>
          <label>反结原因<input aria-label="反结原因" data-testid="bar-bill-reopen-reason" value={reopenReason} onChange={(e) => setReopenReason(e.target.value)} placeholder="例如：漏记一瓶香槟，改正后重结" /></label>
          <HighRiskConfirm
            label="反结"
            impact="账单重开为未付：储值部分当场退回原卡，其余收款记录作废，由收银按更正后的账单重新收款。首次提交生成主管复核单，批准后原样再点一次才生效。"
            disabled={busyAction !== "" || selectedBill?.status !== "paid"}
            precheck={validateReopen}
            onConfirm={() => void reopenBill()}
            permissionsState={effectivePermissions}
            permissions={["bar.bill.reopen"]}
            data-testid="bar-bill-reopen"
          />
          {reopenNote ? <span data-testid="bar-bill-reopen-note">{reopenNote}</span> : null}
        </div>
        {canSuspend(selectedOrder) ? (
          <div className="admin-form-row" data-testid="bar-order-suspend-form">
            <span>包厢订单挂起：须填写原因；挂起后必须在本营业日结清，未结清的阻止交班和日结。</span>
            <label>挂起原因<input aria-label="挂起原因" data-testid="bar-order-suspend-reason" value={suspendReason} onChange={(e) => setSuspendReason(e.target.value)} placeholder="例如：客人稍后付款" /></label>
            <PermissionButton permissionsState={effectivePermissions} permissions={["bar.bill.hang"]} data-testid="bar-order-suspend" disabled={busyAction !== ""} onClick={() => void suspendOrder()}>挂起包厢未付款订单</PermissionButton>
          </div>
        ) : null}
        {selectedOrder?.suspensionStatus === "hanging_unpaid" && selectedOrder.suspension?.badDebtReviewId ? (
          <p className="admin-muted-action" data-testid="bar-order-bad-debt-review-pending">转坏账已提交高价值复核（复核单 {selectedOrder.suspension.badDebtReviewId}）：主管在「高价值复核」页批准后才转坏账损耗处理中，驳回后可重新发起；复核前仍阻止交班、日结与关台。</p>
        ) : null}
        {selectedOrder?.suspensionStatus === "hanging_unpaid" && !selectedOrder.suspension?.badDebtReviewId ? (
          <div className="admin-form-row" data-testid="bar-order-bad-debt-form">
            <span>经理确认最终无法收款：转为坏账损耗处理中，解除该订单对交班、日结与关台的阻断，并固化未收售价、库存成本与赔偿登记（赔偿只登记，不扣工资、绩效或提成）。</span>
            <label>无法收款原因<input aria-label="无法收款原因" data-testid="bar-order-bad-debt-reason" value={badDebtReason} onChange={(e) => setBadDebtReason(e.target.value)} placeholder="例如：客人失联，确认无法收回" /></label>
            <label>高价值商品现场照片与视频<input type="file" multiple accept="image/*,video/*" data-testid="bar-order-bad-debt-evidence-upload" disabled={uploading} onChange={(e) => void uploadEvidenceFiles(e.target.files, "bar_order_bad_debt", (rows) => setBadDebtEvidence((prev) => [...prev, ...rows]))} /></label>
            <span data-testid="bar-order-bad-debt-evidence-list">{badDebtEvidence.length === 0 ? "本单含高价值已出品商品时须同时上传照片和视频，提交后转主管复核，批准才转坏账。" : `已上传证据：${badDebtEvidence.map((e) => e.fileName).join("、")}`}</span>
            <PermissionButton permissionsState={effectivePermissions} permissions={badDebtPermissions} data-testid="bar-order-bad-debt" disabled={busyAction !== ""} onClick={() => void confirmBadDebt()}>确认无法收款（转坏账）</PermissionButton>
          </div>
        ) : null}
        {selectedOrder?.suspensionStatus === "hanging_unpaid" && !selectedOrder.suspension?.badDebtReviewId ? (
          <div className="admin-form-row" data-testid="bar-order-suspension-cancel-form">
            <span>发起取消：勾选能回库的已出品商品（不可回库的不能取消，走确认无法收款），须经理审批；含高价值商品须选退酒原因并上传照片和视频，经理批准后仍走退菜退酒审批。</span>
            {cancelableItems.map((item) => (
              <label key={item.id}><input type="checkbox" data-testid={`bar-order-suspension-cancel-item-${item.id}`} checked={cancelItemIds.includes(item.id)} onChange={(e) => {
                const checked = e.target.checked;
                setCancelItemIds((prev) => checked ? [...prev, item.id] : prev.filter((id) => id !== item.id));
              }} />{item.productName ?? item.id}</label>
            ))}
            <label>取消原因<input aria-label="取消原因" data-testid="bar-order-suspension-cancel-reason" value={cancelReason} onChange={(e) => setCancelReason(e.target.value)} placeholder="例如：客人整瓶未开，退回" /></label>
            <label>退酒原因
              <select data-testid="bar-order-suspension-cancel-reason-code" value={cancelReasonCode} onChange={(e) => setCancelReasonCode(e.target.value)}>
                <option value="">请选择（含高价值商品必选）</option>
                {reasonCodes.map((row) => <option key={row.code} value={row.code}>{row.labelZh}</option>)}
              </select>
            </label>
            <label>证据照片/视频<input type="file" multiple accept="image/*,video/*" data-testid="bar-order-suspension-cancel-evidence-upload" disabled={uploading} onChange={(e) => void uploadEvidenceFiles(e.target.files, "bar_order_item_void", (rows) => setCancelEvidence((prev) => [...prev, ...rows]))} /></label>
            <span data-testid="bar-order-suspension-cancel-evidence-list">{cancelEvidence.length === 0 ? "尚未上传证据（含高价值商品必须上传照片和视频）。" : `已上传证据：${cancelEvidence.map((e) => e.fileName).join("、")}`}</span>
            <PermissionButton permissionsState={effectivePermissions} permissions={["bar.bill.hang"]} data-testid="bar-order-suspension-cancel" disabled={busyAction !== ""} onClick={() => void requestSuspensionCancel()}>发起取消（经理审批）</PermissionButton>
          </div>
        ) : null}
        {selectedOrder?.suspensionStatus === "cancel_pending_approval" && selectedOrder.suspension?.cancelDecidedAt ? (
          <p className="admin-muted-action" data-testid="bar-order-suspension-cancel-waiting">经理已批准取消，所列高价值商品正等主管在退菜退酒待审队列里审批：批准才回库并转已取消，驳回回到已挂起待付款；期间付款结清的以付款为准。</p>
        ) : null}
        {selectedOrder?.suspensionStatus === "cancel_pending_approval" && !selectedOrder.suspension?.cancelDecidedAt ? (
          <div className="admin-form-row" data-testid="bar-order-suspension-cancel-decision-form">
            <span>取消审批：{(selectedOrder.suspension?.cancelItemIds ?? []).map((id) => orderItems.find((item) => item.id === id)?.productName ?? id).join("、")} · 原因 {selectedOrder.suspension?.cancelReason ?? "—"}。批准后能回库的商品回库并冲减账单（高价值的仍等退菜退酒审批），整单取消即作废账单与未完成付款；付款先结清的以付款为准。</span>
            <label>审批意见<input aria-label="审批意见" data-testid="bar-order-suspension-cancel-decision-reason" value={cancelDecisionReason} onChange={(e) => setCancelDecisionReason(e.target.value)} placeholder="选填" /></label>
            <PermissionButton permissionsState={effectivePermissions} permissions={["bar.bill.hang_cancel"]} data-testid="bar-order-suspension-cancel-approve" disabled={busyAction !== ""} onClick={() => void decideSuspensionCancel("approve")}>批准取消</PermissionButton>
            <PermissionButton permissionsState={effectivePermissions} permissions={["bar.bill.hang_cancel"]} data-testid="bar-order-suspension-cancel-reject" disabled={busyAction !== ""} onClick={() => void decideSuspensionCancel("reject")}>驳回取消</PermissionButton>
          </div>
        ) : null}
        {selectedOrder?.suspension?.badDebtConfirmedAt ? (
          <div data-testid="bar-order-bad-debt-detail">
            <p>坏账登记：未收售价 {formatAmount(selectedOrder.suspension.unpaidPriceCent)} · 库存成本 {formatAmount(selectedOrder.suspension.inventoryCostCent)} · 原因 {selectedOrder.suspension.badDebtReason ?? "—"}</p>
            <div className="admin-table-scroll">
              <table className="admin-table">
                <thead><tr><th>出品明细</th><th>责任人</th><th>赔偿登记成本</th></tr></thead>
                <tbody>{(selectedOrder.suspension.compensationLines ?? []).map((line) => (
                  <tr key={line.orderItemId}>
                    <td>{orderItems.find((item) => item.id === line.orderItemId)?.productName ?? line.orderItemId}</td>
                    <td>{line.responsibleUserId ?? "门店损耗"}</td>
                    <td>{formatAmount(line.costCent)}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            {selectedOrder.suspension.status === "bad_debt_processing" ? (
              <PermissionButton permissionsState={effectivePermissions} permissions={["finance.ar_ap.manage"]} data-testid="bar-order-write-off" disabled={busyAction !== ""} onClick={() => void writeOffSuspension()}>财务确认核销结案</PermissionButton>
            ) : null}
          </div>
        ) : null}
        {formError ? <div data-testid="bar-orders-error" className="admin-inline-error">{formError}</div> : null}
        </>
        )}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-orders-table">
            <thead>
              <tr>
                <th>{TEXT.select}</th><th>{TEXT.orderNo}</th><th>{TEXT.table}</th><th>{TEXT.orderStatus}</th><th>{TEXT.billStatus}</th><th>{TEXT.amount}</th><th>{TEXT.commissionBasis}</th><th>{TEXT.createdAt}</th><th>{TEXT.paidAt}</th><th>挂起状态</th>
              <th>详情</th></tr>
            </thead>
            <tbody>
              {pagedOrders.pageRows.map((order) => {
                const bill = firstBill(order);
                return (
                  <tr key={order.id} className="admin-row-clickable" tabIndex={0} data-testid={`bar-order-row-${order.id}`}
                    onClick={() => openOrderDetail(order)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openOrderDetail(order); } }}>
                    <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此订单 ${order.id}`} data-testid={`bar-order-select-${order.id}`} checked={(selectedOrder?.id ?? "") === order.id} onChange={() => setSelectedOrderId(order.id)} /></td>
                    <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-TKT-003" objectId={order.id}>{order.id}</TraceLink></td>
                    <td onClick={(event) => event.stopPropagation()}><TraceLink navigateTo={navigateTo} pageId="ADM-BAR-001" objectId={order.tableId}>{order.tableId ?? "-"}</TraceLink></td>
                    <td>{displayStatus(order.status)}</td>
                    <td>{bill?.id ? `${bill.id} / ${formatBusinessStatus(bill.status)}` : TEXT.noBill}</td>
                    <td>{formatAmount(order.totalAmountCent ?? order.totalAmount)}</td>
                    <td>{salesRoleLabel(order)} · {commissionRateLabel(order)}</td>
                    <td>{cnDate(order.createdAt)}</td>
                    <td data-testid={`bar-order-paid-at-${order.id}`}>{cnDate(bill?.paidAt ?? order.paidAt)}</td>
                    <td data-testid={`bar-order-suspension-${order.id}`}>{suspensionLabel(order)}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看订单详情" onClick={(event) => { event.stopPropagation(); openOrderDetail(order); }}>查看详情</button></td></tr>
                );
              })}
            </tbody>
          </table>
            <TablePager testid="bar-orders-pager" page={pagedOrders.page} pageCount={pagedOrders.pageCount} total={pagedOrders.total} onPrev={pagedOrders.prev} onNext={pagedOrders.next} />
        </div>

        <div className="admin-section-title">退菜退酒申请</div>
        <p className="admin-muted-action" data-testid="bar-void-rule-hint">
          已出品的高价值退酒为「先审后行」：申请后账单与库存不动，须主管审批通过才生效，且必须同时选择退酒原因并上传证据（双证据）。低价值或出品前撤单即时生效。是否高价值全平台一套判定：看这瓶酒的进货价有没有高于后台配置的那一个金额阈值，或者它被手工标为高价值；阈值在「结算配置中心」维护，没配置就一律按高价值拦下走审批。
        </p>
        <div className="admin-form-grid" data-testid="bar-void-request-form">
          <label>订单明细 ID<input data-testid="bar-void-order-item-id" value={voidOrderItemId} onChange={(e) => setVoidOrderItemId(e.target.value)} placeholder="要退的明细 ID" /></label>
          <label>退品说明<input data-testid="bar-void-reason" value={voidReason} onChange={(e) => setVoidReason(e.target.value)} placeholder="例如：客人反馈变质" /></label>
          <label>退酒原因
            <select data-testid="bar-void-reason-code" value={voidReasonCode} onChange={(e) => setVoidReasonCode(e.target.value)}>
              <option value="">请选择（高价值退酒必选）</option>
              {reasonCodes.map((row) => (
                <option key={row.code} value={row.code}>{row.labelZh}</option>
              ))}
            </select>
          </label>
          <label>证据照片/视频
            <input type="file" multiple accept="image/*,video/*" data-testid="bar-void-evidence-upload"
                   disabled={uploading} onChange={(e) => void uploadEvidenceFiles(e.target.files)} />
          </label>
          <label>未动可回库<input type="checkbox" data-testid="bar-void-return-to-stock" checked={voidReturnToStock} onChange={(e) => setVoidReturnToStock(e.target.checked)} /></label>
        </div>
        <p className="admin-muted-action" data-testid="bar-void-evidence-list">
          {uploading ? "证据上传中…" : voidEvidence.length === 0
            ? "尚未上传证据（高价值退酒必须上传）。"
            : `已上传证据：${voidEvidence.map((e) => e.fileName).join("、")}`}
        </p>
        <div className="admin-form-row" data-testid="bar-void-actions">
          <HighRiskConfirm
            label="发起退菜退酒"
            impact="高价值已出品退酒将进入待审队列，审批通过前不动账单与库存；低价值或出品前撤单立即生效并写入资金/库存留痕。"
            disabled={busyAction !== ""}
            precheck={validateVoidRequest}
            onConfirm={() => void submitVoidRequest()}
            permissionsState={effectivePermissions}
            permissions={["bar.order_item.void"]}
            data-testid="bar-void-submit"
          />
          <label>审批意见<input data-testid="bar-void-review-reason" value={voidReviewReason} onChange={(e) => setVoidReviewReason(e.target.value)} placeholder="通过/驳回的说明" /></label>
        </div>

        <div className="admin-section-title">退酒待审队列</div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-void-queue-table">
            <thead>
              <tr>
                <th>申请号</th><th>订单</th><th>类型</th><th>金额</th><th>审批闸</th><th>阈值</th><th>原因码</th><th>证据附件</th><th>是否已生效</th><th>状态</th><th>审批</th>
              </tr>
            </thead>
            <tbody>
              {voidQueue.length === 0 ? (
                <tr><td colSpan={11} data-testid="bar-void-queue-empty">当前没有待审批的退菜退酒申请。</td></tr>
              ) : voidQueue.map((row) => (
                <tr key={row.id} data-testid={`bar-void-row-${row.id}`}>
                  <td>{row.id}</td>
                  <td>{row.orderId ?? "—"}</td>
                  <td>{voidLabel(VOID_TYPE_LABEL, row.voidType)}</td>
                  <td>{formatAmount(row.amountCent)}</td>
                  <td data-testid={`bar-void-mode-${row.id}`}>{voidLabel(VOID_APPROVAL_MODE_LABEL, row.approvalMode)}</td>
                  <td>{row.highValueThresholdCent == null ? "—" : formatAmount(row.highValueThresholdCent)}</td>
                  <td data-testid={`bar-void-reason-${row.id}`}>{reasonLabel(reasonCodes, row.reasonCode)}</td>
                  <td>{(row.evidenceAttachmentIds ?? []).join("、") || "—"}</td>
                  <td data-testid={`bar-void-applied-${row.id}`}>{row.effectsAppliedAt ? `已生效 · ${cnDate(row.effectsAppliedAt)}` : "未生效"}</td>
                  <td>{voidLabel(VOID_STATUS_LABEL, row.status)}</td>
                  <td>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.order_item.void_decide"]} data-testid={`bar-void-approve-${row.id}`} disabled={busyAction !== ""} onClick={() => void decideVoid(row.id, "approved")}>通过</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.order_item.void_decide"]} data-testid={`bar-void-reject-${row.id}`} disabled={busyAction !== ""} onClick={() => void decideVoid(row.id, "rejected")}>驳回</PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="admin-section-title">退酒原因维护</div>
        <p className="admin-muted-action" data-testid="bar-void-reason-dict-hint">
          店员退酒时只能从这份清单里选，选不到的原因就填不上——所以这份清单要跟门店实际情况对得上。改了立刻生效；下线一条要写清楚为什么，历史单据上的原因照样看得到。
        </p>
        <div className="admin-form-grid" data-testid="bar-void-reason-dict-form">
          <label>原因标识<input data-testid="bar-void-reason-new-code" value={newReasonCode} onChange={(e) => setNewReasonCode(e.target.value)} placeholder="仅小写字母/数字/下划线，用于统计口径" /></label>
          <label>中文名称<input data-testid="bar-void-reason-new-label" value={newReasonLabel} onChange={(e) => setNewReasonLabel(e.target.value)} placeholder="店员在下拉里看到的字，如 酒品质量问题" /></label>
          <label>下线理由<input data-testid="bar-void-reason-archive-reason" value={archiveReason} onChange={(e) => setArchiveReason(e.target.value)} placeholder="下线某条原因时必填" /></label>
        </div>
        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={["bar.order_item.void_decide"]} data-testid="bar-void-reason-save" disabled={busyAction !== ""} onClick={() => void saveReasonCode()}>保存原因</PermissionButton>
        </div>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-void-reason-dict-table">
            <thead><tr><th>中文名称</th><th>统计标识</th><th>操作</th></tr></thead>
            <tbody>
              {reasonCodes.length === 0 ? (
                <tr><td colSpan={3} data-testid="bar-void-reason-dict-empty">暂无可选的退酒原因。</td></tr>
              ) : reasonCodes.map((row) => (
                <tr key={row.code} data-testid={`bar-void-reason-row-${row.code}`}>
                  <td>{row.labelZh}</td>
                  <td>{row.code}</td>
                  <td>
                    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.order_item.void_decide"]} data-testid={`bar-void-reason-archive-${row.code}`} disabled={busyAction !== ""} onClick={() => void archiveReasonCodeEntry(row.code)}>下线</PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
