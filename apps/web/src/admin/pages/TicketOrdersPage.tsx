import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { loadSecondary, useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";

// 后端写权限码（TicketingController：/ticketing/orders/{order_id}/issue）
const TICKET_ISSUE_PERMISSION = "ticket.ticket.issue";
import { usePagedRows, TablePager } from "../components/TablePager";
import type { AdminPageProps, TicketIssuedEntitlementRow, TicketOrderRow, TicketVerificationRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { formatBusinessStatus } from "../i18n/statusLabels";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { MetricDetailLink, useMetricKpiValue } from "../components/MetricDetailEntry";

const TEXT = {
  title: "\u51fa\u7968\u4e0e\u6838\u9500\u8ffd\u8e2a",
  description: "\u805a\u5408\u7968\u52a1\u8ba2\u5355\u3001\u652f\u4ed8\u72b6\u6001\u3001\u51fa\u7968\u72b6\u6001\u3001\u6743\u76ca\u548c\u6838\u9500\u9884\u68c0\uff0c\u7528\u4e8e\u5b9a\u4f4d\u672a\u51fa\u7968\u548c\u5f02\u5e38\u6838\u9500\u3002",
  evidence: "\u63a5\u53e3\u8bc1\u636e\uff1a\u8bfb\u53d6 /api/v1/ticketing/orders\uff1b\u8bfb\u53d6 /api/v1/ticketing/issued-entitlements\uff1b\u8bfb\u53d6 /api/v1/ticketing/verifications\uff1b\u51fa\u7968 POST /api/v1/ticketing/orders/{order_id}/issue\uff1b\u6838\u9500\u9884\u68c0 POST /api/v1/ticketing/verifications/preview\u3002",
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
};

// KPI \u805a\u7126\u89c6\u56fe\uff08\u7167 OrganizationMerchantPage \u6837\u677f\uff09\uff1a\u70b9 KPI \u6570\u5b57=\u5e26 ?kpi= \u53c2\u6570\u91cd\u8fdb\u672c\u9875\uff0c\u53ea\u6e32\u67d3\u8ba2\u5355\u8868\u5e76\u6309\u5b50\u96c6\u8fc7\u6ee4\u3002
// \u26a0\ufe0f\u4e0e\u5df2\u6709\u7684 ?focus=<\u8ba2\u5355\u5b9a\u4f4d>\uff08P1-B\uff09\u8bfb\u4e0d\u540c\u53c2\u6570\uff0c\u4e92\u4e0d\u5e72\u6270\uff1akpi \u805a\u7126\u8bfb ?kpi\uff0c\u8ba2\u5355\u5b9a\u4f4d\u8bfb focusObjectId\u3002
type KpiTarget = "orders" | "paid" | "not_issued";
type KpiFocus = { target: KpiTarget };

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

function selectedLabel(order: TicketOrderRow | undefined) {
  return order?.orderNo ?? order?.id ?? "-";
}

export function TicketOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId }: AdminPageProps) {
  const [orders, setOrders] = React.useState<TicketOrderRow[]>([]);
  const [entitlements, setEntitlements] = React.useState<TicketIssuedEntitlementRow[]>([]);
  const [verifications, setVerifications] = React.useState<TicketVerificationRow[]>([]);
  const [entitlementsNotice, setEntitlementsNotice] = React.useState<string | null>(null);
  const [verificationsNotice, setVerificationsNotice] = React.useState<string | null>(null);
  const { selectedId: selectedOrderId, setSelectedId: setSelectedOrderId, selected: selectedOrder } = useSelectedRow(orders);
  const [entitlementCode, setEntitlementCode] = React.useState("ENT-0001");
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");
  const [query, setQuery] = React.useState("");
  const [kpiFocus, setKpiFocus] = React.useState<KpiFocus | null>(readKpiFocusFromUrl);
  // 行明细抽屉：整行可点 → 该订单完整字段（金额¥·枚举中文·下单/支付时间中文），与既有单选/下钻并存。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

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

  const { loading, error, reload: reloadResource } = useAdminResource(
    async () => {
      const [orderRows, entitlementRows, verificationRows] = await Promise.all([
        api.get<TicketOrderRow[]>("/api/v1/ticketing/orders"),
        // 权益/核销为副资源：403 或失败时降级（KPI 显「—」不静默归零），出票主表照常。
        loadSecondary(() => api.get<TicketIssuedEntitlementRow[]>("/api/v1/ticketing/issued-entitlements"), [] as TicketIssuedEntitlementRow[]),
        loadSecondary(() => api.get<TicketVerificationRow[]>("/api/v1/ticketing/verifications"), [] as TicketVerificationRow[]),
      ]);
      setOrders(orderRows);
      setEntitlements(entitlementRows.data);
      setEntitlementsNotice(entitlementRows.notice);
      setVerifications(verificationRows.data);
      setVerificationsNotice(verificationRows.notice);
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
    await runAction("ticket_verification_previewed", () => api.post("/api/v1/ticketing/verifications/preview", {
      entitlementCode,
      operatorNote: TEXT.previewNote,
    }), false);
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
                  className={`admin-row-clickable${focusMatches(order) ? " admin-row-focus" : ""}`} tabIndex={0}
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
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
