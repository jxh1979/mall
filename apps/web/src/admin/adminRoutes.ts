import type { AdminRoute } from "./types";

export const adminRoutes: AdminRoute[] = [
  { pageId: "ADM-OV-001", label: "总控驾驶舱", module: "总控", route: "/admin/overview", requiredPermissions: ["overview.read"] },
  { pageId: "ADM-ACC-001", label: "员工账号", module: "账号权限", route: "/admin/access/users", requiredPermissions: ["user.read"] },
  { pageId: "ADM-ACC-002", label: "权限模板", module: "账号权限", route: "/admin/access/permission-templates", requiredPermissions: ["permission_template.read"] },
  { pageId: "ADM-TKT-001", label: "票务策略", module: "票务", route: "/admin/ticketing/policies", requiredPermissions: ["ticket.policy.read"] },
  { pageId: "ADM-BAR-001", label: "卡座与订台", module: "酒吧", route: "/admin/bar/table-sessions", requiredPermissions: ["bar.table.read"] },
  { pageId: "ADM-FIN-001", label: "应收应付台账", module: "财务", route: "/admin/finance/ar-ap-ledger", requiredPermissions: ["finance.ledger.read"] },
  { pageId: "ADM-INT-001", label: "供应商适配器", module: "第三方", route: "/admin/integrations/adapters", requiredPermissions: ["integration.adapter.read"] },
  { pageId: "ADM-OPS-001", label: "生产 blocker 与证据", module: "运维", route: "/admin/ops/readiness", requiredPermissions: ["ops.readiness.read"] },
  { pageId: "ADM-GOV-001", label: "集团/综合体拓扑", module: "治理", route: "/admin/governance/topology", requiredPermissions: ["governance.topology.read"] },
  { pageId: "ADM-GOV-002", label: "组织与商户档案", module: "治理", route: "/admin/governance/organizations", requiredPermissions: ["organization.read"] },
  { pageId: "ADM-GOV-003", label: "四类主体绑定", module: "治理", route: "/admin/governance/subject-bindings", requiredPermissions: ["subject_binding.read"] },
  { pageId: "ADM-GOV-004", label: "合同审批与附件归档", module: "合同", route: "/admin/contracts", requiredPermissions: ["contract.read"] },
  { pageId: "ADM-GOV-005", label: "商户准入审批流水", module: "治理", route: "/admin/governance/admission-audits", requiredPermissions: ["admission.audit.read"] },
  { pageId: "ADM-GOV-006", label: "主管复核与违规阻断", module: "治理", route: "/admin/governance/supervisor-review", requiredPermissions: ["governance.supervisor_review.view"] },
  { pageId: "ADM-TKT-002", label: "动态定价", module: "票务", route: "/admin/ticketing/dynamic-pricing", requiredPermissions: ["ticket.dynamic.read"] },
  { pageId: "ADM-TKT-003", label: "出票与核销追踪", module: "票务", route: "/admin/ticketing/orders", requiredPermissions: ["ticket.order.read"] },
  { pageId: "ADM-BAR-002", label: "订单/出品/账单", module: "酒吧", route: "/admin/bar/orders", requiredPermissions: ["bar.order.read"] },
  { pageId: "ADM-BAR-003", label: "存酒", module: "酒吧", route: "/admin/bar/stored-wine", requiredPermissions: ["bar.storage.read"] },
  { pageId: "ADM-BAR-004", label: "销售提成", module: "酒吧", route: "/admin/bar/commissions", requiredPermissions: ["bar.commission.read"] },
  { pageId: "ADM-AMU-001", label: "项目与场次", module: "游乐", route: "/admin/amusement/sessions", requiredPermissions: ["amusement.project.read"] },
  { pageId: "ADM-AMU-002", label: "团体/包场", module: "游乐", route: "/admin/amusement/group-bookings", requiredPermissions: ["amusement.group.read"] },
  { pageId: "ADM-AMU-003", label: "安全巡检/事故", module: "游乐", route: "/admin/amusement/safety-incidents", requiredPermissions: ["amusement.safety.read"] },
  { pageId: "ADM-INV-001", label: "库存台账", module: "库存", route: "/admin/inventory/ledger", requiredPermissions: ["inventory.ledger.read"] },
  { pageId: "ADM-FIN-002", label: "对账差异", module: "财务", route: "/admin/finance/reconciliation", requiredPermissions: ["finance.reconciliation.read"] },
  { pageId: "ADM-MER-001", label: "商户服务工单", module: "商户服务", route: "/admin/merchant-service/tickets", requiredPermissions: ["merchant.service_work_order.view"] },
  { pageId: "ADM-MER-002", label: "商户授权工作台", module: "商户门户", route: "/admin/merchant-portal/scope", requiredPermissions: ["merchant.portal.read"] },
  { pageId: "ADM-INT-002", label: "回调/轮询/补偿", module: "第三方", route: "/admin/integrations/events", requiredPermissions: ["integration.event.read"] },
  { pageId: "ADM-RISK-001", label: "风控预警", module: "风控", route: "/admin/risk/alerts", requiredPermissions: ["risk.alert.read"] },
  { pageId: "ADM-EVD-001", label: "审计事件", module: "审计", route: "/admin/audit/events", requiredPermissions: ["audit.event.read"] },
  { pageId: "ADM-ANA-001", label: "经营分析", module: "分析", route: "/admin/analytics/business", requiredPermissions: ["analytics.business.read"] },
];

export function findAdminRoute(pathname: string) {
  return adminRoutes.find((route) => route.route === pathname) ?? adminRoutes[0]!;
}
