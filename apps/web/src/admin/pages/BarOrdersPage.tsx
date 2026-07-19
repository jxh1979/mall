import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import type { AdminPageProps, BarBillRow, BarOrderRow } from "../types";
import { isSalesOrder } from "./barSalesRole";
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
  commissionHelp: "\u9500\u552e\u4eba\u5458\u6309 15%",
  amount: "\u8ba2\u5355\u91d1\u989d",
  amountHelp: "\u4e0d\u542b\u901a\u7968\u6743\u76ca\u62b5\u6263",
  selectedOrder: "\u9009\u4e2d\u8ba2\u5355",
  noTable: "\u672a\u9009\u62e9\u5361\u5ea7",
  createBill: "\u751f\u6210\u8d26\u5355",
  deliver: "\u6807\u8bb0\u51fa\u54c1",
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
  sales15: "\u9500\u552e\u4eba\u5458 15%",
  nonSales: "\u975e\u9500\u552e",
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

function firstBill(order: BarOrderRow | undefined): BarBillRow | undefined {
  return order?.bills?.[0];
}

function firstBillId(order: BarOrderRow | undefined) {
  return firstBill(order)?.id ?? "";
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
  const [cashierStaffId, setCashierStaffId] = React.useState("");
  const [posTransactionNo, setPosTransactionNo] = React.useState("");
  // 行明细抽屉：整行可点 → 该订单完整字段（状态/提成口径中文·金额¥·下单/支付时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

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
        { label: TEXT.commissionBasis, value: isSalesOrder(order) ? TEXT.sales15 : `${TEXT.nonSales} 10%` },
        { label: TEXT.createdAt, value: cnDate(order.createdAt) },
        { label: TEXT.paidAt, value: cnDate(bill?.paidAt ?? order.paidAt) },
      ],
    });
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
    await runOrderAction("bar_order_delivered", () => api.post(`/api/v1/bar/orders/${selectedOrder!.id}/deliver`, {
      operatorNote: TEXT.deliverNote,
      deliveryPhotoAttachmentIds: ["att-admin-delivery-1"],
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

  async function settleBill() {
    const billId = firstBillId(selectedOrder);
    if (!billId) {
      setFormError(TEXT.billRequired);
      return;
    }
    await runOrderAction("bar_bill_settled", () => api.post(`/api/v1/bar/bills/${billId}/settle`, {
      paymentId: "payment-admin-web",
      settlementAmountCent: selectedOrder?.totalAmountCent ?? selectedOrder?.totalAmount ?? 0,
      paymentChannel,
      cashierStaffId: paymentChannel === "cash" ? cashierStaffId.trim() || undefined : undefined,
      posTransactionNo: paymentChannel === "pos" ? posTransactionNo.trim() || undefined : undefined,
      operatorNote: TEXT.settleNote,
    }));
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
          <label>结算方式<select data-testid="bar-settle-channel" value={paymentChannel} onChange={(e) => setPaymentChannel(e.target.value)}>
            <option value="aggregate_pay">聚合支付</option>
            <option value="cash">现金</option>
            <option value="pos">POS 刷卡</option>
            <option value="bank_transfer">银行转账</option>
            <option value="other">其他</option>
          </select></label>
          {paymentChannel === "cash" ? (
            <label>现金收款人<input data-testid="bar-settle-cashier" value={cashierStaffId} onChange={(e) => setCashierStaffId(e.target.value)} placeholder="收款员工 ID" /></label>
          ) : null}
          {paymentChannel === "pos" ? (
            <label>POS 流水号<input data-testid="bar-settle-pos-no" value={posTransactionNo} onChange={(e) => setPosTransactionNo(e.target.value)} placeholder="POS 交易流水号" /></label>
          ) : null}
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
                    <td>{isSalesOrder(order) ? TEXT.sales15 : `${TEXT.nonSales} 10%`}</td>
                    <td>{cnDate(order.createdAt)}</td>
                    <td data-testid={`bar-order-paid-at-${order.id}`}>{cnDate(bill?.paidAt ?? order.paidAt)}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看订单详情" onClick={(event) => { event.stopPropagation(); openOrderDetail(order); }}>查看详情</button></td></tr>
                );
              })}
            </tbody>
          </table>
            <TablePager testid="bar-orders-pager" page={pagedOrders.page} pageCount={pagedOrders.pageCount} total={pagedOrders.total} onPrev={pagedOrders.prev} onNext={pagedOrders.next} />
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
