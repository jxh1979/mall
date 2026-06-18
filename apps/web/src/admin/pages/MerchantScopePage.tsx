import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, MerchantScopeBillRow, MerchantScopeWorkOrderRow } from "../types";

const TEXT = {
  title: "商户授权工作台",
  description: "商户侧只查看已授权业务范围内的工单和账单；平台治理、财务总账、第三方配置等跨主体能力继续由权限网关拦截。",
  evidence: "接口证据：读取 /api/v1/merchant-service/work-orders、/api/v1/merchant-billing/bills；权限由 merchant.portal.read、merchant.service_work_order.view、merchant.billing.view 控制。",
  boundary: "商户角色不得访问平台治理、财务总账、第三方配置和其它商户数据；真实多商户隔离仍以后端权限和数据范围策略为准，本页只展示当前授权范围。",
  empty: "暂无授权商户数据",
  loadFailed: "商户授权数据读取失败",
  confirm: "确认授权范围",
};

function display(value: unknown) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "number") return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(value);
  return String(value);
}

function matchesMerchant(merchantId: string | null | undefined, rowMerchantId?: string) {
  return !merchantId || rowMerchantId === merchantId;
}

export function MerchantScopePage({ api, user, setActionResult, setGlobalError }: AdminPageProps) {
  const [workOrders, setWorkOrders] = React.useState<MerchantScopeWorkOrderRow[]>([]);
  const [bills, setBills] = React.useState<MerchantScopeBillRow[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [workOrderRows, billRows] = await Promise.all([
        api.get<MerchantScopeWorkOrderRow[]>("/api/v1/merchant-service/work-orders"),
        api.get<MerchantScopeBillRow[]>("/api/v1/merchant-billing/bills"),
      ]);
      setWorkOrders(workOrderRows.filter((row) => matchesMerchant(user.merchantId, row.merchantId)));
      setBills(billRows.filter((row) => matchesMerchant(user.merchantId, row.merchantId)));
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const authorizedScopes = Array.from(new Set([...workOrders, ...bills].map((row) => row.authorizedScope).filter(Boolean))).sort();
  const totalBillAmount = bills.reduce((sum, row) => sum + Number(row.totalAmount ?? 0), 0);
  const hasRows = workOrders.length > 0 || bills.length > 0;

  function confirmScope() {
    setGlobalError(null);
    setActionResult({
      status: "merchant_scope_confirmed",
      audit: "merchant_authorized_scope_checked",
      merchantId: user.merchantId ?? "all-visible",
      authorizedScopes,
      workOrderCount: workOrders.length,
      billCount: bills.length,
      evidenceAttachmentIds: ["att-merchant-scope-confirm"],
    });
  }

  return (
    <section data-testid="merchant-scope-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <div className="admin-generic-evidence" data-testid="merchant-scope-evidence">{TEXT.evidence}</div>
      <AuditTrailHint event="merchant.scope.viewed / merchant.scope_confirmed" />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {!hasRows ? <div className="admin-state admin-empty" data-testid="merchant-scope-empty">{TEXT.empty}</div> : (
          <>
            <div className="admin-kpi-grid" data-testid="merchant-scope-summary">
              <div className="admin-kpi-card"><span>授权范围</span><strong>{authorizedScopes.length}</strong><small>{authorizedScopes.join(" / ") || "-"}</small></div>
              <div className="admin-kpi-card"><span>商户工单</span><strong>{workOrders.length}</strong><small>当前商户</small></div>
              <div className="admin-kpi-card"><span>商户账单</span><strong>{bills.length}</strong><small>授权账单</small></div>
              <div className="admin-kpi-card"><span>账单金额</span><strong>{display(totalBillAmount)}</strong><small>只读汇总</small></div>
            </div>

            <div className="admin-form-row">
              <button type="button" data-testid="merchant-scope-confirm" onClick={confirmScope}>{TEXT.confirm}</button>
              <span className="admin-muted-action" data-testid="merchant-scope-boundary">{TEXT.boundary}</span>
            </div>

            <h3>授权工单</h3>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="merchant-scope-work-orders-table">
                <thead><tr><th>商户</th><th>类型</th><th>标题</th><th>优先级</th><th>状态</th><th>授权范围</th><th>留痕证据</th></tr></thead>
                <tbody>{workOrders.map((row) => <tr key={row.id}><td>{display(row.merchantId)}</td><td>{display(row.workOrderType)}</td><td>{display(row.title)}</td><td>{display(row.priority)}</td><td>{display(row.status)}</td><td>{display(row.authorizedScope)}</td><td>{display(row.auditEvidenceRef)}</td></tr>)}</tbody>
              </table>
            </div>

            <h3>授权账单</h3>
            <div className="admin-table-scroll">
              <table className="admin-table" data-testid="merchant-scope-bills-table">
                <thead><tr><th>商户</th><th>账单号</th><th>标题</th><th>金额</th><th>状态</th><th>授权范围</th><th>留痕证据</th></tr></thead>
                <tbody>{bills.map((row) => <tr key={row.id}><td>{display(row.merchantId)}</td><td>{display(row.billNo)}</td><td>{display(row.title)}</td><td>{display(row.totalAmount)}</td><td>{display(row.status)}</td><td>{display(row.authorizedScope)}</td><td>{display(row.auditEvidenceRef)}</td></tr>)}</tbody>
              </table>
            </div>
          </>
        )}
      </AdminDataState>
    </section>
  );
}
