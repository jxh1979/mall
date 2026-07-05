import React from "react";
import { TechDetails } from "../components/TechDetails";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { HighRiskConfirm } from "../components/HighRiskConfirm";
import { useAdminResource } from "../hooks/useAdminResource";
import { useSelectedRow } from "../hooks/useSelectedRow";
import { rowMatchesKeyword } from "../components/rowSearch";

// 后端写权限码（TicketingController：/ticketing/orders/{order_id}/issue）
const TICKET_ISSUE_PERMISSION = "ticket.ticket.issue";
import { usePagedRows, TablePager } from "../components/TablePager";
import type { AdminPageProps, TicketIssuedEntitlementRow, TicketOrderRow, TicketVerificationRow } from "../types";
import { TraceLink } from "../components/TraceLink";
import { formatBusinessStatus } from "../i18n/statusLabels";

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
  issueNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u51fa\u7968",
  previewNote: "\u5b8c\u6574\u540e\u53f0\u9a8c\u6536\u6838\u9500\u9884\u68c0",
};

function formatAmount(amountCent: number | undefined) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(Number(amountCent ?? 0) / 100);
}

function selectedLabel(order: TicketOrderRow | undefined) {
  return order?.orderNo ?? order?.id ?? "-";
}

export function TicketOrdersPage({ api, setActionResult, setGlobalError, effectivePermissions, navigateTo }: AdminPageProps) {
  const [orders, setOrders] = React.useState<TicketOrderRow[]>([]);
  const [entitlements, setEntitlements] = React.useState<TicketIssuedEntitlementRow[]>([]);
  const [verifications, setVerifications] = React.useState<TicketVerificationRow[]>([]);
  const { selectedId: selectedOrderId, setSelectedId: setSelectedOrderId, selected: selectedOrder } = useSelectedRow(orders);
  const [entitlementCode, setEntitlementCode] = React.useState("ENT-0001");
  const [formError, setFormError] = React.useState("");
  const [busyAction, setBusyAction] = React.useState("");
  const [query, setQuery] = React.useState("");

  const { loading, error, reload: reloadResource } = useAdminResource(
    async () => {
      const [orderRows, entitlementRows, verificationRows] = await Promise.all([
        api.get<TicketOrderRow[]>("/api/v1/ticketing/orders"),
        api.get<TicketIssuedEntitlementRow[]>("/api/v1/ticketing/issued-entitlements"),
        api.get<TicketVerificationRow[]>("/api/v1/ticketing/verifications"),
      ]);
      setOrders(orderRows);
      setEntitlements(entitlementRows);
      setVerifications(verificationRows);
      setEntitlementCode((current) => current || entitlementRows[0]?.entitlementCode || "");
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );


  const paidCount = orders.filter((order) => order.payStatus === "paid").length;
  const pendingIssueCount = orders.filter((order) => order.issueStatus !== "issued").length;
  const filteredOrders = orders.filter((order) => rowMatchesKeyword(order, query));
  const paged = usePagedRows(filteredOrders, 10, query);

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

  return (
    <section data-testid="ticket-orders-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <TechDetails><div className="admin-generic-evidence" data-testid="ticket-orders-evidence">{TEXT.evidence}</div></TechDetails>
      <AuditTrailHint event="ticket_order.viewed / ticket.issued / ticket_verification.previewed" />
      <AdminDataState loading={loading} error={error} empty={orders.length === 0} onRetry={reloadResource}>
        <div className="admin-kpi-grid" data-testid="ticket-orders-summary">
          <div className="admin-kpi-card"><span>{TEXT.orders}</span><strong>{orders.length}</strong><small>{TEXT.boundary}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.paid}</span><strong>{paidCount}</strong><small>paid</small></div>
          <div className="admin-kpi-card"><span>{TEXT.pendingIssue}</span><strong>{pendingIssueCount}</strong><small>not_issued</small></div>
          <div className="admin-kpi-card"><span>{TEXT.entitlements}</span><strong>{entitlements.length}</strong><small>{entitlements[0]?.entitlementCode ?? "-"}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.verifications}</span><strong>{verifications.length}</strong><small>{verifications[0]?.status ?? "-"}</small></div>
          <div className="admin-kpi-card"><span>{TEXT.selectedOrder}</span><strong>{selectedLabel(selectedOrder)}</strong><small>{selectedOrder?.issueStatus ?? "-"}</small></div>
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

        <div className="admin-form-row" data-testid="ticket-orders-filter">
          <input data-testid="ticket-orders-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索订单号 / 票种 / 渠道 / 支付/出票状态" />
          <span className="admin-muted-action" data-testid="ticket-orders-filter-count">匹配 {filteredOrders.length} / {orders.length} 条</span>
        </div>
        {filteredOrders.length === 0 ? (
          <div className="admin-state admin-empty" data-testid="ticket-orders-filter-empty">无匹配订单</div>
        ) : (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="ticket-orders-table">
            <thead>
              <tr><th>{TEXT.select}</th><th>{TEXT.orderNo}</th><th>{TEXT.ticketType}</th><th>{TEXT.quantity}</th><th>{TEXT.visitDate}</th><th>{TEXT.channel}</th><th>{TEXT.amount}</th><th>{TEXT.payStatus}</th><th>{TEXT.issueStatus}</th><th>{TEXT.status}</th></tr>
            </thead>
            <tbody>
              {paged.pageRows.map((order) => (
                <tr key={order.id}>
                  <td><input type="radio" data-testid={`ticket-order-select-${order.id}`} checked={(selectedOrder?.id ?? "") === order.id} onChange={() => setSelectedOrderId(order.id)} /></td>
                  <td>{order.orderNo ?? order.id}</td>
                  <td><TraceLink navigateTo={navigateTo} pageId="ADM-TKT-001" objectId={order.ticketTypeId}>{order.ticketTypeId ?? "-"}</TraceLink></td>
                  <td>{order.quantity ?? "-"}</td>
                  <td>{order.visitDate ?? "-"}</td>
                  <td>{formatBusinessStatus(order.salesChannel)}</td>
                  <td>{formatAmount(order.amountCent)}</td>
                  <td>{formatBusinessStatus(order.payStatus)}</td>
                  <td>{formatBusinessStatus(order.issueStatus)}</td>
                  <td>{formatBusinessStatus(order.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="ticket-orders-pager" page={paged.page} pageCount={paged.pageCount} total={paged.total} onPrev={paged.prev} onNext={paged.next} />
        </div>
        )}
      </AdminDataState>
    </section>
  );
}
