// 文件版本：V0.05
import React from "react";
import { BarRefundBreakdownPanel } from "./BarRefundBreakdownPanel";
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
import { TraceLink } from "../components/TraceLink";
import { usePagedRows, TablePager } from "../components/TablePager";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";

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
  evidence: "\u63a5\u53e3\u8bc1\u636e\uff1a\u8bfb\u53d6 /api/v1/bar/orders\uff1b\u751f\u6210\u8d26\u5355 POST /api/v1/bar/orders/{order_id}/bill\uff1b\u51fa\u54c1 POST /api/v1/bar/orders/{order_id}/deliver\uff1b\u7ed3\u7b97 POST /api/v1/bar/bills/{bill_id}/settle\u3002",
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
  voidNote: "\u540e\u53f0\u53d1\u8d77\u9000\u83dc\u9000\u9152\u7533\u8bf7",
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
function deliveryLabel(item: BarOrderItemRow) {
  if (item.deliveryStatus === "delivered") return "已上菜";
  if (item.deliveryStatus === "voided") return "已撤单";
  return "未上菜";
}

export function BarOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [orders, setOrders] = React.useState<BarOrderRow[]>([]);
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
  const [cashierStaffId, setCashierStaffId] = React.useState("");
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
  const orderItems: BarOrderItemRow[] = selectedOrder?.items ?? [];
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
    if (paymentChannel === "cash" && !cashierStaffId.trim()) {
      setFormError("现金结算需填现金收款人，供对账留痕。");
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
      cashierStaffId: paymentChannel === "cash" ? cashierStaffId.trim() || undefined : undefined,
      posTransactionNo: paymentChannel === "pos" ? posTransactionNo.trim() || undefined : undefined,
      ...(voucherCode.trim() ? { voucherCode: voucherCode.trim() } : {}),
      ...(memberPhone.trim() ? { memberPhone: memberPhone.trim() } : {}),
      operatorNote: "按本次真实收款凭据登记，多渠道累计足额后完成账单",
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
      setFormError("退菜退酒必须填写原因，进入审计留痕。");
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
        operatorNote: TEXT.voidNote,
      },
    ));
  }

  /** 现场拍的照片/视频直接上传，拿回真实附件 ID —— 不再让店员手打附件 ID。 */
  async function uploadEvidenceFiles(files: FileList | null) {
    if (!files || files.length === 0) return;
    setUploading(true);
    setFormError("");
    try {
      const uploaded: { id: string; fileName: string }[] = [];
      for (const file of Array.from(files)) {
        const form = new FormData();
        form.append("file", file);
        form.append("business_type", "bar_order_item_void");
        form.append("attachment_role", file.type.startsWith("video") ? "video" : "photo");
        if (!api.postForm) throw new Error("当前客户端不支持文件上传。");
        const saved = await api.postForm<{ id: string; fileName?: string }>(
          "/api/v1/evidence/attachments/upload", form);
        uploaded.push({ id: saved.id, fileName: saved.fileName ?? file.name });
      }
      setVoidEvidence((prev) => [...prev, ...uploaded]);
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
          {paymentChannel === "cash" ? (
            <label>现金收款人<input data-testid="bar-settle-cashier" value={cashierStaffId} onChange={(e) => setCashierStaffId(e.target.value)} placeholder="收款员工 ID" /></label>
          ) : null}
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
        {formError ? <div data-testid="bar-orders-error" className="admin-inline-error">{formError}</div> : null}
        </>
        )}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-orders-table">
            <thead>
              <tr>
                <th>{TEXT.select}</th><th>{TEXT.orderNo}</th><th>{TEXT.table}</th><th>{TEXT.orderStatus}</th><th>{TEXT.billStatus}</th><th>{TEXT.amount}</th><th>{TEXT.commissionBasis}</th><th>{TEXT.createdAt}</th><th>{TEXT.paidAt}</th>
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
