import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";
import type { AdminPageProps, BarBillRow, BarOrderRow } from "../types";
import { commissionRateLabel, isSalesOrder, latestBill, salesRoleLabel } from "./barSalesRole";

const CALC_PERM = ["bar.table_commission.calculate"];

const TEXT = {
  title: "销售提成",
  description: "按卡座销售口径核算业务提成：计提档按员工岗位判（员工档案里标了「销售岗」的按销售人员费率，其余按非销售人员费率），费率在「结算配置中心 → 提成与积分」维护。本页比例列显示的是提成台账里的真实计提比例，不含通票金额，不在本页直接发放薪酬。",
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
  boundary: "提成只按卡座消费额计算：计提档按员工岗位判（销售岗按销售人员费率、其余按非销售人员费率，费率后台可配），不含通票、赠票、票务权益和卡座免通票入场人数。结算发放仍需财务复核。",
  select: "选择",
  orderNo: "订单号",
  table: "卡座",
  bill: "账单",
  amount: "消费额",
  sellerRole: "销售角色",
  rate: "计提比例",
  status: "状态",
  time: "时间",
  sales: "销售人员",
  nonSales: "非销售人员",
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
  return latestBill(order);
}

function firstBillId(order: BarOrderRow | undefined) {
  return firstBill(order)?.id ?? "";
}

function formatAmount(amountCent: number | undefined) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(Number(amountCent ?? 0) / 100);
}

// 计提比例/销售角色一律取真实落库值（barSalesRole 统一口径），页面不再自算。
function commissionRate(order: BarOrderRow | undefined) {
  return order ? commissionRateLabel(order) : "-";
}

function sellerLabel(order: BarOrderRow | undefined) {
  return order ? salesRoleLabel(order) : "-";
}

export function BarCommissionsPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [orders, setOrders] = React.useState<BarOrderRow[]>([]);
  const { selectedId: selectedOrderId, setSelectedId: setSelectedOrderId, selected: selectedOrder } = useSelectedRow(orders);
  const [formError, setFormError] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [kpiFocus, setKpiFocus] = React.useState<KpiTarget | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该提成单完整字段（角色/比例/状态中文·金额¥·时间中文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  function openCommissionDetail(order: BarOrderRow) {
    const rowBill = firstBill(order);
    setDetail({
      title: `提成单明细 · ${order.id}`,
      fields: [
        { label: "订单号", value: order.id },
        { label: "卡座", value: order.tableId ?? "—" },
        { label: "账单", value: rowBill?.id ?? "—" },
        { label: "消费额", value: formatAmount(order.totalAmountCent ?? order.totalAmount) },
        { label: "销售角色", value: sellerLabel(order) },
        { label: "计提比例", value: commissionRate(order) },
        { label: "状态", value: formatBusinessStatus(rowBill?.status ?? order.status) },
        { label: "时间", value: cnDate(order.createdAt) },
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


  const billId = firstBillId(selectedOrder);
  const salesOrders = orders.filter((order) => isSalesOrder(order)).length;
  const nonSalesOrders = orders.length - salesOrders;
  // 列表接口返回 totalAmountCent（旧 totalAmount 字段兼容保留）：此前读幽灵字段恒为 0。
  const totalAmount = orders.reduce((sum, order) => sum + Number(order.totalAmountCent ?? order.totalAmount ?? 0), 0);

  // 第二批接线（指标登记簿）：订单数卡（笔数+金额）改走统一明细端点（与订单/出品/账单页同一登记项同源，
  // 全历史口径与本页全量列表一致）；失败回退本地计算并隐藏「完整明细」入口。销售/非销售为本页口径拆分，保持本地。
  const orderCountKpi = useMetricKpiValue(api, "bar.order_count", {});
  const orderAmountKpi = useMetricKpiValue(api, "bar.order_amount", {});
  const orderCountDisplay = orderCountKpi.status === "ready" && orderCountKpi.value !== null ? orderCountKpi.value : orders.length;
  const orderAmountDisplay = orderAmountKpi.status === "ready" && orderAmountKpi.value !== null ? orderAmountKpi.value : totalAmount;
  // 聚焦时对提成表做客户端筛选：sales=销售岗归属、non_sales=其余；仅过滤已加载内存数组，不发后端查询。
  const visibleOrders = kpiFocus === "sales"
    ? orders.filter((order) => isSalesOrder(order))
    : kpiFocus === "non_sales"
      ? orders.filter((order) => !isSalesOrder(order))
      : orders;

  async function calculateCommission() {
    if (!selectedOrder || !billId) {
      setFormError(TEXT.noBill);
      return;
    }
    // 请求体只带后端 CommissionCalculateRequest 真有的字段：费率由后端按角色档从结算配置中心取，
    // 前端不下发费率（原先下发的 salesRate/nonSalesRate 后端根本不接，纯属误导读者以为费率由页面定）。
    const requestBody = { includeOnlyTableSales: true };
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
          <div className="admin-kpi-card"><span>{TEXT.orderCount}</span><strong data-testid="bar-commissions-kpi-orders">{orderCountDisplay}</strong><small data-testid="bar-commissions-kpi-amount">{formatAmount(orderAmountDisplay)}{orderCountKpi.status === "ready" ? <> · <MetricDetailLink navigateTo={navigateTo} metric="bar.order_count" testid="bar-commissions-metric-detail-orders" /></> : null}</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.salesRate}</span><button type="button" className="admin-kpi-value" data-testid="bar-commissions-kpi-sales" onClick={() => enterKpiFocus("sales")}>{salesOrders}</button><small>{salesOrders} 单 · 按销售人员费率</small></div>
          <div className="admin-kpi-card admin-kpi-card--clickable"><span>{TEXT.nonSalesRate}</span><button type="button" className="admin-kpi-value" data-testid="bar-commissions-kpi-non-sales" onClick={() => enterKpiFocus("non_sales")}>{nonSalesOrders}</button><small>{nonSalesOrders} 单 · 按非销售人员费率</small></div>
          <div className="admin-kpi-card"><span>{TEXT.excludeTickets}</span><strong>是</strong><small>{TEXT.excludeTicketsHelp}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedBill}</span><strong className="admin-kpi-value--text" title={billId || "-"}>{billId || "-"}</strong><small>{selectedOrder?.id ?? "-"}</small></div>
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
              <tr><th>{TEXT.select}</th><th>{TEXT.orderNo}</th><th>{TEXT.table}</th><th>{TEXT.bill}</th><th>{TEXT.amount}</th><th>{TEXT.sellerRole}</th><th>{TEXT.rate}</th><th>{TEXT.status}</th><th>{TEXT.time}</th><th>详情</th></tr>
            </thead>
            <tbody>
              {visibleOrders.map((order) => {
                const rowBill = firstBill(order);
                return (
                  <tr key={order.id} className="admin-row-clickable" tabIndex={0} data-testid={`bar-commission-row-${order.id}`}
                    onClick={() => openCommissionDetail(order)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openCommissionDetail(order); } }}>
                    <td onClick={(event) => event.stopPropagation()}><input type="radio" aria-label={`选择此提成单 ${order.id}`} data-testid={`bar-commission-select-${order.id}`} checked={(selectedOrder?.id ?? "") === order.id} onChange={() => setSelectedOrderId(order.id)} /></td>
                    <td>{order.id}</td>
                    <td>{order.tableId ?? "-"}</td>
                    <td>{rowBill?.id ?? "-"}</td>
                    <td>{formatAmount(order.totalAmountCent ?? order.totalAmount)}</td>
                    <td>{sellerLabel(order)}</td>
                    <td>{commissionRate(order)}</td>
                    <td>{formatBusinessStatus(rowBill?.status ?? order.status)}</td>
                    <td>{cnDate(order.createdAt)}</td>
                  <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看提成单详情" onClick={(event) => { event.stopPropagation(); openCommissionDetail(order); }}>查看详情</button></td></tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
