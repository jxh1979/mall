import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps, BarBillRow, BarOrderRow } from "../types";

const CALC_PERM = ["bar.table_commission.calculate"];

const TEXT = {
  title: "销售提成",
  description: "按卡座销售口径核算业务提成：销售人员 15%，非销售人员 10%，不含通票金额，不在本页直接发放薪酬。",
  evidence: "接口证据：读取 /api/v1/bar/orders；计算 POST /api/v1/bar/bills/{bill_id}/commissions/calculate。",
  loadFailed: "销售提成数据读取失败",
  actionFailed: "销售提成计算失败",
  noBill: "当前订单没有可计提账单。",
  orderCount: "订单数",
  salesRate: "销售人员",
  nonSalesRate: "非销售人员",
  excludeTickets: "不含通票",
  selectedBill: "选中账单",
  calculate: "计算提成",
  boundary: "提成只按卡座消费额计算：销售人员 15%，非销售人员 10%，不含通票、赠票、票务权益和卡座免通票入场人数。结算发放仍需财务复核。",
  select: "选择",
  orderNo: "订单号",
  table: "卡座",
  bill: "账单",
  amount: "消费额",
  sellerRole: "销售角色",
  rate: "计提比例",
  status: "状态",
  time: "时间",
  sales: "销售人员 15%",
  nonSales: "非销售人员 10%",
  excludeTicketsHelp: "提成不计通票金额",
  focusClear: "清除筛选，返回全部",
};

// KPI 聚焦视图（照 OrganizationMerchantPage 样板）：点 KPI 数字=带 ?kpi= 参数重进本页，只渲染提成表并按销售/非销售子集过滤。
type KpiTarget = "sales" | "non_sales";
function readKpiFocusFromUrl(): KpiTarget | null {
  if (typeof window === "undefined") return null;
  const target = new URLSearchParams(window.location.search).get("kpi");
  return target === "sales" || target === "non_sales" ? target : null;
}

function firstBill(order: BarOrderRow | undefined): BarBillRow | undefined {
  return order?.bills?.[0] ?? (order?.billId ? { id: order.billId, status: "created" } : undefined);
}

function firstBillId(order: BarOrderRow | undefined) {
  return firstBill(order)?.id ?? order?.billId ?? "";
}

function formatAmount(amountCent: number | undefined) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(Number(amountCent ?? 0) / 100);
}

function commissionRate(order: BarOrderRow | undefined) {
  if (!order) return "-";
  if (order.sellerRole === "sales" || order.commissionRate === "15%") return "15%";
  return order.commissionRate ?? "10%";
}

function sellerLabel(order: BarOrderRow | undefined) {
  if (!order) return "-";
  return order.sellerRole === "sales" || order.commissionRate === "15%" ? TEXT.sales : TEXT.nonSales;
}

export function BarCommissionsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [orders, setOrders] = React.useState<BarOrderRow[]>([]);
  const { selectedId: selectedOrderId, setSelectedId: setSelectedOrderId, selected: selectedOrder } = useSelectedRow(orders);
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);

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
    window.scrollTo({ top: 0 });
  }

  function exitKpiFocus() {
    const params = new URLSearchParams(window.location.search);
    params.delete("kpi");
    const search = params.toString();
    window.history.pushState(window.history.state, "", `${window.location.pathname}${search ? `?${search}` : ""}`);
    setKpiFocus(null);
    window.scrollTo({ top: 0 });
  }

  const { loading, error, reload } = useAdminResource(
    async () => {
      const rows = await api.get<BarOrderRow[]>("/api/v1/bar/orders");
      setOrders(rows);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const billId = firstBillId(selectedOrder);
  const salesOrders = orders.filter((order) => commissionRate(order) === "15%").length;
  const nonSalesOrders = orders.length - salesOrders;
  const totalAmount = orders.reduce((sum, order) => sum + Number(order.totalAmount ?? 0), 0);
  // 聚焦时对提成表做客户端筛选：sales=15%口径、non_sales=其余；仅过滤已加载内存数组，不发后端查询。
  const visibleOrders = kpiFocus === "sales"
    ? orders.filter((order) => commissionRate(order) === "15%")
    : kpiFocus === "non_sales"
      ? orders.filter((order) => commissionRate(order) !== "15%")
      : orders;

  async function calculateCommission() {
    if (!selectedOrder || !billId) {
      setFormError(TEXT.noBill);
      return;
    }
    const requestBody = {
      includeTicketAmount: false,
      salesRate: 0.15,
      nonSalesRate: 0.1,
      operatorNote: "完整后台验收提成计算",
    };
    setSubmitting(true);
    setFormError("");
    setGlobalError(null);
    try {
      const result = await api.post(`/api/v1/bar/bills/${billId}/commissions/calculate`, requestBody);
      setActionResult({ action: "bar_commission_calculated", orderId: selectedOrder.id, billId, requestBody, result });
      await reload();
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : TEXT.actionFailed;
      setFormError(message);
      setGlobalError(message);
      setActionResult({ action: "bar_commission_calculated", orderId: selectedOrder.id, billId, status: "failed", message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="bar-commissions-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="bar-commissions-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="bar.commission.read / bar.commission.calculated" />
      <AdminDataState loading={loading} error={error} empty={orders.length === 0} onRetry={reload}>
        {kpiFocus ? (
          <div className="admin-state" data-testid="bar-commissions-kpi-focus-banner">
            正在查看：{kpiFocus === "sales" ? TEXT.salesRate : TEXT.nonSalesRate}（{visibleOrders.length}）
            <button type="button" data-testid="bar-commissions-kpi-focus-clear" onClick={exitKpiFocus}>{TEXT.focusClear}</button>
          </div>
        ) : (
        <>
        <div className="admin-kpi-grid" data-testid="bar-commissions-summary">
          <div className="admin-kpi-card"><span>{TEXT.orderCount}</span><strong>{orders.length}</strong><small>{formatAmount(totalAmount)}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.salesRate}</span><button type="button" className="admin-kpi-value" data-testid="bar-commissions-kpi-sales" onClick={() => enterKpiFocus("sales")}>{salesOrders}</button><small>15% · {salesOrders} 单</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.nonSalesRate}</span><button type="button" className="admin-kpi-value" data-testid="bar-commissions-kpi-non-sales" onClick={() => enterKpiFocus("non_sales")}>{nonSalesOrders}</button><small>10% · {nonSalesOrders} 单</small></div>
          <div className="admin-kpi-card"><span>{TEXT.excludeTickets}</span><strong>是</strong><small>{TEXT.excludeTicketsHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedBill}</span><strong>{billId || "-"}</strong><small>{selectedOrder?.orderNo ?? "-"}</small></div>
        </div>

        <div className="admin-form-row">
          <PermissionButton permissionsState={effectivePermissions} permissions={CALC_PERM} data-testid="bar-commission-calculate" disabled={submitting} onClick={() => void calculateCommission()}>{TEXT.calculate}</PermissionButton>
          <span className="admin-muted-action" data-testid="bar-commissions-boundary">{TEXT.boundary}</span>
        </div>
        {formError ? <div data-testid="bar-commissions-error" className="admin-inline-error">{formError}</div> : null}
        </>
        )}

        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="bar-commissions-table">
            <thead>
              <tr><th>{TEXT.select}</th><th>{TEXT.orderNo}</th><th>{TEXT.table}</th><th>{TEXT.bill}</th><th>{TEXT.amount}</th><th>{TEXT.sellerRole}</th><th>{TEXT.rate}</th><th>{TEXT.status}</th><th>{TEXT.time}</th></tr>
            </thead>
            <tbody>
              {visibleOrders.map((order) => {
                const rowBill = firstBill(order);
                return (
                  <tr key={order.id}>
                    <td><input type="radio" data-testid={`bar-commission-select-${order.id}`} checked={(selectedOrder?.id ?? "") === order.id} onChange={() => setSelectedOrderId(order.id)} /></td>
                    <td>{order.orderNo ?? order.id}</td>
                    <td>{order.tableId ?? "-"}</td>
                    <td>{rowBill?.id ?? order.billId ?? "-"}</td>
                    <td>{formatAmount(order.totalAmount)}</td>
                    <td>{sellerLabel(order)}</td>
                    <td>{commissionRate(order)}</td>
                    <td>{formatBusinessStatus(rowBill?.status ?? order.status)}</td>
                    <td>{order.createdAt ?? "-"}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
