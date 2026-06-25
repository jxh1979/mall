import type { AdminNavGroup, AdminRoute } from "./types";

export const adminRoutes: AdminRoute[] = [
  { pageId: "ADM-OV-001", label: "总控驾驶舱", module: "总控", group: "overview", route: "/admin/overview", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ACC-001", label: "员工账号", module: "账号权限", group: "access", route: "/admin/access/users", requiredPermissions: ["access.user.view"] },
  { pageId: "ADM-ACC-002", label: "权限模板", module: "账号权限", group: "access", route: "/admin/access/permission-templates", requiredPermissions: ["access.permission_template.view"] },
  { pageId: "ADM-TKT-001", label: "票务策略", module: "票务", group: "ticketing", route: "/admin/ticketing/policies", requiredPermissions: ["ticket.product.view"] },
  { pageId: "ADM-BAR-001", label: "卡座与订台", module: "酒吧", group: "bar", route: "/admin/bar/table-sessions", requiredPermissions: ["bar.table_session.view", "bar.table.view", "bar.reservation.view", "bar.order.view"] },
  { pageId: "ADM-FIN-001", label: "应收应付台账", module: "财务", group: "finance", route: "/admin/finance/ar-ap-ledger", requiredPermissions: ["finance.ar_ap.view"] },
  { pageId: "ADM-INT-001", label: "供应商适配器", module: "第三方", group: "integration", route: "/admin/integrations/adapters", requiredPermissions: ["integration.provider_adapter.view"] },
  { pageId: "ADM-OPS-001", label: "生产 blocker 与证据", module: "运维", group: "ops", route: "/admin/ops/readiness", requiredPermissions: ["ops.production_readiness.view"] },
  { pageId: "ADM-GOV-001", label: "集团/综合体拓扑", module: "治理", group: "governance", route: "/admin/governance/topology", requiredPermissions: ["governance.project.view", "governance.space.view", "merchant.profile.view"] },
  { pageId: "ADM-GOV-002", label: "组织与商户档案", module: "治理", group: "governance", route: "/admin/governance/organizations", requiredPermissions: ["merchant.profile.view", "governance.project.view"] },
  { pageId: "ADM-GOV-003", label: "四类主体绑定", module: "治理", group: "governance", route: "/admin/governance/subject-bindings", requiredPermissions: ["entity.binding.view"] },
  { pageId: "ADM-GOV-004", label: "合同审批与附件归档", module: "合同", group: "governance", route: "/admin/contracts", requiredPermissions: ["contract.view"] },
  { pageId: "ADM-GOV-005", label: "商户准入审批流水", module: "治理", group: "governance", route: "/admin/governance/admission-audits", requiredPermissions: ["merchant.profile.view", "entity.binding.view", "contract.view"] },
  { pageId: "ADM-GOV-006", label: "主管复核与违规阻断", module: "治理", group: "governance", route: "/admin/governance/supervisor-review", requiredPermissions: ["governance.supervisor_review.view"] },
  { pageId: "ADM-TKT-002", label: "动态定价", module: "票务", group: "ticketing", route: "/admin/ticketing/dynamic-pricing", requiredPermissions: ["ticket.dynamic_pricing.view"] },
  { pageId: "ADM-TKT-003", label: "出票与核销追踪", module: "票务", group: "ticketing", route: "/admin/ticketing/orders", requiredPermissions: ["ticket.order.read", "ticket.entitlement.view", "ticket.verify.view"] },
  { pageId: "ADM-BAR-002", label: "订单/出品/账单", module: "酒吧", group: "bar", route: "/admin/bar/orders", requiredPermissions: ["bar.order.view"] },
  { pageId: "ADM-BAR-003", label: "存酒", module: "酒吧", group: "bar", route: "/admin/bar/stored-wine", requiredPermissions: ["bar.saved_bottle.manage"] },
  { pageId: "ADM-BAR-004", label: "销售提成", module: "酒吧", group: "bar", route: "/admin/bar/commissions", requiredPermissions: ["bar.table_commission.calculate"] },
  { pageId: "ADM-BAR-005", label: "商品管理", module: "酒吧", group: "bar", route: "/admin/bar/products", requiredPermissions: ["bar.product.view"] },
  { pageId: "ADM-AMU-001", label: "项目与场次", module: "游乐", group: "amusement", route: "/admin/amusement/sessions", requiredPermissions: ["amusement.session.view"] },
  { pageId: "ADM-AMU-002", label: "团体/包场", module: "游乐", group: "amusement", route: "/admin/amusement/group-bookings", requiredPermissions: ["amusement.group_booking.view"] },
  { pageId: "ADM-AMU-003", label: "安全巡检/事故", module: "游乐", group: "amusement", route: "/admin/amusement/safety-incidents", requiredPermissions: ["amusement.safety_inspection.view"] },
  { pageId: "ADM-AMU-004", label: "巡检到期任务", module: "游乐", group: "amusement", route: "/admin/amusement/inspection-tasks", requiredPermissions: ["amusement.safety_inspection.view"] },
  { pageId: "ADM-INV-001", label: "库存台账", module: "库存", group: "inventory", route: "/admin/inventory/ledger", requiredPermissions: ["inventory.stock.view"] },
  { pageId: "ADM-FIN-002", label: "对账差异", module: "财务", group: "finance", route: "/admin/finance/reconciliation", requiredPermissions: ["finance.reconciliation.view"] },
  { pageId: "ADM-FIN-003", label: "退款分级授权", module: "财务", group: "finance", route: "/admin/finance/refund-authority", requiredPermissions: ["refund.authority.view"] },
  { pageId: "ADM-MER-001", label: "商户服务工单", module: "商户服务", group: "governance", route: "/admin/merchant-service/tickets", requiredPermissions: ["merchant.service_work_order.view"] },
  { pageId: "ADM-MER-002", label: "商户授权工作台", module: "商户门户", group: "governance", route: "/admin/merchant-portal/scope", requiredPermissions: ["merchant.service_work_order.view", "merchant.billing.view"] },
  { pageId: "ADM-INT-002", label: "回调/轮询/补偿", module: "第三方", group: "integration", route: "/admin/integrations/events", requiredPermissions: ["integration.retry.view", "integration.compensation.view", "integration.order_sync.view", "integration.reconciliation.view"] },
  { pageId: "ADM-INT-003", label: "第三方开关", module: "第三方", group: "integration", route: "/admin/integrations/switches", requiredPermissions: ["terminal.switch.view"] },
  { pageId: "ADM-RISK-001", label: "风控预警", module: "风控", group: "risk", route: "/admin/risk/alerts", requiredPermissions: ["analytics.risk_alert.view"] },
  { pageId: "ADM-EVD-001", label: "审计事件", module: "审计", group: "audit", route: "/admin/audit/events", requiredPermissions: ["evidence.event.view"] },
  { pageId: "ADM-ANA-001", label: "经营分析", module: "分析", group: "overview", route: "/admin/analytics/business", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ANA-002", label: "集团总览", module: "分析", group: "overview", route: "/admin/analytics/group-overview", requiredPermissions: ["analytics.group.view"] },
  { pageId: "ADM-ANA-003", label: "经营参数", module: "分析", group: "overview", route: "/admin/analytics/operating-params", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-LEASE-001", label: "招商租赁工作台", module: "招商租赁", group: "governance", route: "/admin/leasing/workbench", requiredPermissions: ["leasing.lead.manage"] },
];

// 业务域分组顺序与展示文案（frontend-admin §4.3：8 业务域 + 平台域聚合）。
// 渲染顺序即此数组顺序；未在 adminRoutes 中出现的分组会被 AdminApp 自动跳过。
export const adminNavGroupOrder: AdminNavGroup[] = [
  "overview",
  "ticketing",
  "bar",
  "inventory",
  "amusement",
  "finance",
  "governance",
  "integration",
  "access",
  "risk",
  "audit",
  "ops",
];

export const adminNavGroupLabels: Record<AdminNavGroup, string> = {
  overview: "看板/分析",
  ticketing: "票务",
  bar: "酒吧",
  inventory: "库存",
  amusement: "游乐",
  finance: "财务",
  governance: "治理/合同",
  integration: "第三方对接",
  access: "账号权限",
  risk: "风控",
  audit: "审计",
  ops: "运维",
};

export type AdminNavGroupView = {
  key: AdminNavGroup;
  label: string;
  routes: AdminRoute[];
};

/**
 * 把扁平 adminRoutes 折叠成两级分组视图，供 AdminApp 渲染可折叠菜单。
 * 缺 group 字段的路由回落到 "ops"，保证不丢项。
 */
export function groupedAdminRoutes(): AdminNavGroupView[] {
  return adminNavGroupOrder
    .map((key) => ({
      key,
      label: adminNavGroupLabels[key],
      routes: adminRoutes.filter((route) => (route.group ?? "ops") === key),
    }))
    .filter((group) => group.routes.length > 0);
}

export function findAdminRoute(pathname: string) {
  return adminRoutes.find((route) => route.route === pathname) ?? adminRoutes[0]!;
}
