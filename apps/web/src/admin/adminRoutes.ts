import type { AdminAccountForm, AdminNavGroup, AdminRoute, PermissionDefinitionRow } from "./types";

export const adminRoutes: AdminRoute[] = [
  { pageId: "ADM-OV-001", label: "总控驾驶舱", module: "总控", group: "overview", route: "/admin/overview", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ACC-001", label: "员工账号", module: "账号权限", group: "access", route: "/admin/access/users", requiredPermissions: ["access.user.view"] },
  { pageId: "ADM-ACC-002", label: "权限模板", module: "账号权限", group: "access", route: "/admin/access/permission-templates", requiredPermissions: ["access.permission_template.view"] },
  { pageId: "ADM-ACC-003", label: "访问治理工作台", module: "账号权限", group: "access", route: "/admin/access/governance", requiredPermissions: ["access.role.assign"] },
  { pageId: "ADM-ACC-004", label: "组织层级与审批线", module: "账号权限", group: "access", route: "/admin/access/org-hierarchy", requiredPermissions: ["access.role.assign"] },
  { pageId: "ADM-TKT-001", label: "票务策略", module: "票务", group: "ticketing", route: "/admin/ticketing/policies", requiredPermissions: ["ticket.product.view"] },
  { pageId: "ADM-BAR-001", label: "卡座与订台", module: "酒吧", group: "bar", route: "/admin/bar/table-sessions", requiredPermissions: ["bar.table_session.view", "bar.table.view", "bar.reservation.view", "bar.order.view"] },
  { pageId: "ADM-FIN-001", label: "应收应付台账", module: "财务", group: "finance", route: "/admin/finance/ar-ap-ledger", requiredPermissions: ["finance.ar_ap.view"] },
  { pageId: "ADM-FIN-005", label: "通票结算审阅", module: "财务", group: "finance", route: "/admin/finance/settlement-review", requiredPermissions: ["finance.settlement.view"] },
  { pageId: "ADM-INT-001", label: "供应商适配器", module: "第三方", group: "integration", route: "/admin/integrations/adapters", requiredPermissions: ["integration.provider_adapter.view"] },
  { pageId: "ADM-OPS-001", label: "生产 blocker 与证据", module: "运维", group: "ops", route: "/admin/ops/readiness", requiredPermissions: ["ops.production_readiness.view"] },
  { pageId: "ADM-OPS-002", label: "数据清理工具", module: "运维", group: "ops", route: "/admin/ops/data-cleanup", requiredPermissions: ["data.cleanup.request"] },
  { pageId: "ADM-GOV-001", label: "集团/综合体拓扑", module: "治理", group: "governance", route: "/admin/governance/topology", requiredPermissions: ["governance.project.view", "governance.space.view", "merchant.profile.view"] },
  { pageId: "ADM-GOV-002", label: "组织与商户档案", module: "治理", group: "governance", route: "/admin/governance/organizations", requiredPermissions: ["merchant.profile.view", "governance.project.view"] },
  { pageId: "ADM-GOV-003", label: "四类主体绑定", module: "治理", group: "governance", route: "/admin/governance/subject-bindings", requiredPermissions: ["entity.binding.view"] },
  { pageId: "ADM-CONT-001", label: "合同生命周期工作台", module: "合同", group: "governance", route: "/admin/contracts", requiredPermissions: ["contract.draft.manage"] },
  { pageId: "ADM-LEASE-001", label: "招商工作台", module: "招商租赁", group: "governance", route: "/admin/leasing/workbench", requiredPermissions: ["leasing.lead.manage", "leasing.intent.manage", "leasing.brand.manage"] },
  { pageId: "ADM-FAC-001", label: "楼宇运营工作台", module: "物业/楼宇", group: "ops", route: "/admin/facility/workbench", requiredPermissions: ["facility.equipment.manage"] },
  { pageId: "ADM-ENE-001", label: "能源工作台", module: "物业/楼宇", group: "ops", route: "/admin/energy/workbench", requiredPermissions: ["energy.reading.manage"] },
  { pageId: "ADM-GOV-005", label: "商户准入审批流水", module: "治理", group: "governance", route: "/admin/governance/admission-audits", requiredPermissions: ["merchant.profile.view", "entity.binding.view", "contract.view"] },
  { pageId: "ADM-GOV-006", label: "主管复核与违规阻断", module: "治理", group: "governance", route: "/admin/governance/supervisor-review", requiredPermissions: ["governance.supervisor_review.view"] },
  { pageId: "ADM-TKT-002", label: "动态定价", module: "票务", group: "ticketing", route: "/admin/ticketing/dynamic-pricing", requiredPermissions: ["ticket.dynamic_pricing.view"] },
  { pageId: "ADM-TKT-003", label: "出票与核销追踪", module: "票务", group: "ticketing", route: "/admin/ticketing/orders", requiredPermissions: ["ticket.order.read", "ticket.entitlement.view", "ticket.verify.view"] },
  { pageId: "ADM-BAR-002", label: "订单/出品/账单", module: "酒吧", group: "bar", route: "/admin/bar/orders", requiredPermissions: ["bar.order.view"] },
  { pageId: "ADM-BAR-003", label: "存酒", module: "酒吧", group: "bar", route: "/admin/bar/stored-wine", requiredPermissions: ["bar.saved_bottle.manage"] },
  { pageId: "ADM-BAR-004", label: "销售提成", module: "酒吧", group: "bar", route: "/admin/bar/commissions", requiredPermissions: ["bar.table_commission.calculate"] },
  { pageId: "ADM-BAR-005", label: "商品管理", module: "酒吧", group: "bar", route: "/admin/bar/products", requiredPermissions: ["bar.product.view"] },
  { pageId: "ADM-BAR-006", label: "套餐管理", module: "酒吧", group: "bar", route: "/admin/bar/combos", requiredPermissions: ["bar.combo.view"] },
  { pageId: "ADM-AMU-001", label: "项目与场次", module: "游乐", group: "amusement", route: "/admin/amusement/sessions", requiredPermissions: ["amusement.session.view"] },
  { pageId: "ADM-AMU-002", label: "团体/包场", module: "游乐", group: "amusement", route: "/admin/amusement/group-bookings", requiredPermissions: ["amusement.group_booking.view"] },
  { pageId: "ADM-AMU-003", label: "安全巡检/事故", module: "游乐", group: "amusement", route: "/admin/amusement/safety-incidents", requiredPermissions: ["amusement.safety_inspection.view"] },
  { pageId: "ADM-AMU-004", label: "巡检到期任务", module: "游乐", group: "amusement", route: "/admin/amusement/inspection-tasks", requiredPermissions: ["amusement.safety_inspection.view"] },
  { pageId: "ADM-AMU-005", label: "游乐核销记录", module: "游乐", group: "amusement", route: "/admin/amusement/verification-records", requiredPermissions: ["amusement.verification.view"] },
  { pageId: "ADM-INV-001", label: "库存台账", module: "库存", group: "inventory", route: "/admin/inventory/ledger", requiredPermissions: ["inventory.stock.view"] },
  { pageId: "ADM-INV-002", label: "跨主体调拨与借调", module: "库存", group: "inventory", route: "/admin/inventory/transfers", requiredPermissions: ["inventory.transfer_order.view"] },
  { pageId: "ADM-FIN-002", label: "对账差异", module: "财务", group: "finance", route: "/admin/finance/reconciliation", requiredPermissions: ["finance.reconciliation.view"] },
  { pageId: "ADM-FIN-003", label: "退款分级授权", module: "财务", group: "finance", route: "/admin/finance/refund-authority", requiredPermissions: ["refund.authority.view"] },
  { pageId: "ADM-FIN-004", label: "物业费管理", module: "财务", group: "finance", route: "/admin/finance/property-fee", requiredPermissions: ["finance.property_fee.manage"] },
  { pageId: "ADM-FIN-006", label: "商户费用台账", module: "财务", group: "finance", route: "/admin/finance/merchant-expenses", requiredPermissions: ["finance.merchant_expense.view"] },
  { pageId: "ADM-FIN-007", label: "持股分红", module: "财务", group: "finance", route: "/admin/finance/dividends", requiredPermissions: ["finance.dividend.view"] },
  { pageId: "ADM-STL-001", label: "结算配置中心", module: "结算", group: "settlement", route: "/admin/settlement/config", requiredPermissions: ["settlement.config.view"] },
  { pageId: "ADM-STL-002", label: "开票清单", module: "结算", group: "settlement", route: "/admin/settlement/invoice-items", requiredPermissions: ["settlement.invoice.view"] },
  { pageId: "ADM-STL-003", label: "内部结算价目录", module: "结算", group: "settlement", route: "/admin/settlement/internal-price-catalog", requiredPermissions: ["settlement.config.view"] },
  { pageId: "ADM-MER-001", label: "商户服务工单", module: "商户服务", group: "governance", route: "/admin/merchant-service/tickets", requiredPermissions: ["merchant.service_work_order.view"] },
  { pageId: "ADM-MER-002", label: "商户授权工作台", module: "商户门户", group: "governance", route: "/admin/merchant-portal/scope", requiredPermissions: ["merchant.service_work_order.view", "merchant.billing.view"] },
  { pageId: "ADM-VND-001", label: "外包维保工单", module: "外包工作台", group: "vendor", route: "/admin/vendor/work-orders", requiredPermissions: ["facility.work_order.view_assigned"], accountForms: ["vendor"] },
  { pageId: "ADM-GOV-007", label: "空间类型字典", module: "治理", group: "governance", route: "/admin/governance/space-types", requiredPermissions: ["governance.space.view"] },
  { pageId: "ADM-GOV-008", label: "场地营业时间", module: "治理", group: "governance", route: "/admin/governance/venue-business-hours", requiredPermissions: ["governance.space.view"] },
  { pageId: "ADM-GOV-009", label: "审批链与节点裁决", module: "治理", group: "governance", route: "/admin/governance/approval-chain", requiredPermissions: ["governance.supervisor_review.view"] },
  { pageId: "ADM-VND-004", label: "外包下属账号", module: "外包工作台", group: "vendor", route: "/admin/vendor/subordinates", requiredPermissions: ["partner.subordinate.view"], accountForms: ["vendor"] },
  { pageId: "ADM-TER-001", label: "终端开关矩阵", module: "终端", group: "integration", route: "/admin/terminals/switch-matrix", requiredPermissions: ["terminal.switch.view"] },
  { pageId: "ADM-TER-002", label: "终端设备", module: "终端", group: "integration", route: "/admin/terminals/devices", requiredPermissions: ["terminal.switch.view"] },
  { pageId: "ADM-CUS-001", label: "客人账号管理", module: "客人", group: "governance", route: "/admin/customers/accounts", requiredPermissions: ["customer.account.manage"] },
  { pageId: "ADM-CUS-002", label: "券台账管理", module: "客人", group: "governance", route: "/admin/customers/vouchers", requiredPermissions: ["customer.voucher.view"] },
  // ADM-CUS-003「积分规则配置」已于 2026-07-28 停用下线（V352）：该页所配四项口径全被别的活表接管，
  // 改任何字段都不影响任何计算，运营改完以为生效实际无效。积分口径统一在结算配置中心配置。
  { pageId: "ADM-CUS-004", label: "会员等级与忠诚度", module: "客人", group: "governance", route: "/admin/customers/loyalty-config", requiredPermissions: ["customer.loyalty.view"] },
  { pageId: "ADM-CUS-005", label: "储值规则配置", module: "客人", group: "governance", route: "/admin/customers/stored-value-rules", requiredPermissions: ["customer.stored_value.refund"] },
  { pageId: "ADM-INT-004", label: "短信通知开关", module: "第三方接入", group: "integration", route: "/admin/integrations/sms-switches", requiredPermissions: ["terminal.switch.view"] },
  { pageId: "ADM-LEASE-002", label: "租赁状态看板", module: "招商租赁", group: "governance", route: "/admin/leasing/rental-status", requiredPermissions: ["contract.draft.manage"] },
  { pageId: "ADM-VND-002", label: "外包巡检任务", module: "外包工作台", group: "vendor", route: "/admin/vendor/inspections", requiredPermissions: ["facility.inspection.view_assigned"], accountForms: ["vendor"] },
  { pageId: "ADM-VND-003", label: "外包进场报备", module: "外包工作台", group: "vendor", route: "/admin/vendor/site-entries", requiredPermissions: ["facility.site_entry.view_assigned"], accountForms: ["vendor"] },
  { pageId: "ADM-INT-002", label: "回调/轮询/补偿", module: "第三方", group: "integration", route: "/admin/integrations/events", requiredPermissions: ["integration.retry.view", "integration.compensation.view", "integration.order_sync.view", "integration.reconciliation.view"] },
  { pageId: "ADM-INT-003", label: "第三方开关", module: "第三方", group: "integration", route: "/admin/integrations/switches", requiredPermissions: ["terminal.switch.view"] },
  { pageId: "ADM-INT-005", label: "渠道映射维护", module: "第三方", group: "integration", route: "/admin/integrations/channel-maps", requiredPermissions: ["integration.channel_store_map.view"] },
  { pageId: "ADM-RISK-001", label: "风控预警", module: "风控", group: "risk", route: "/admin/risk/alerts", requiredPermissions: ["analytics.risk_alert.view"] },
  { pageId: "ADM-EVD-001", label: "审计事件", module: "审计", group: "audit", route: "/admin/audit/events", requiredPermissions: ["evidence.event.view"] },
  { pageId: "ADM-ANA-001", label: "经营分析", module: "分析", group: "overview", route: "/admin/analytics/business", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ANA-002", label: "集团总览", module: "分析", group: "overview", route: "/admin/analytics/group-overview", requiredPermissions: ["analytics.group.view"] },
  { pageId: "ADM-ANA-004", label: "欠费与回款", module: "分析", group: "overview", route: "/admin/analytics/receivables", requiredPermissions: ["finance.ar_ap.view"] },
  { pageId: "ADM-ANA-005", label: "出租与到期", module: "分析", group: "overview", route: "/admin/analytics/occupancy", requiredPermissions: ["contract.view"] },
  { pageId: "ADM-ANA-006", label: "工单时效", module: "分析", group: "overview", route: "/admin/analytics/work-order-sla", requiredPermissions: ["facility.work_order.manage"] },
  { pageId: "ADM-ANA-007", label: "能耗分析", module: "分析", group: "overview", route: "/admin/analytics/energy", requiredPermissions: ["energy.reading.manage"] },
  { pageId: "ADM-ANA-008", label: "月度对比", module: "分析", group: "overview", route: "/admin/analytics/monthly", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ANA-009", label: "T3-A 只读经营分析", module: "分析", group: "overview", route: "/admin/analytics/t3a-readonly", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ANA-012", label: "指标明细", module: "分析", group: "overview", route: "/admin/metric-detail", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ANA-013", label: "放弃权益额外收入", module: "分析", group: "overview", route: "/admin/analytics/abandoned-entitlement-revenue", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-STF-001", label: "排班与考勤", module: "人事", group: "ops", route: "/admin/staff/shifts", requiredPermissions: ["staff.shift.manage"] },
  { pageId: "ADM-STF-002", label: "薪酬与请假", module: "人事", group: "ops", route: "/admin/staff/payroll", requiredPermissions: ["staff.payroll.manage"] },
  { pageId: "ADM-STF-003", label: "个税税表", module: "人事", group: "ops", route: "/admin/staff/tax-brackets", requiredPermissions: ["staff.payroll.manage"] },
  { pageId: "ADM-STF-004", label: "员工档案", module: "人事", group: "ops", route: "/admin/staff/profiles", requiredPermissions: ["staff.profile.view"] },
  { pageId: "ADM-SVC-001", label: "服务点单", module: "服务", group: "ops", route: "/admin/services/orders", requiredPermissions: ["service.catalog.manage"] },
  { pageId: "ADM-ANA-003", label: "经营参数", module: "分析", group: "overview", route: "/admin/analytics/operating-params", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ANA-010", label: "AI阈值", module: "分析", group: "overview", route: "/admin/analytics/ai-thresholds", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-ANA-011", label: "提醒与留存天数配置", module: "分析", group: "overview", route: "/admin/analytics/notification-windows", requiredPermissions: ["analytics.dashboard.view"] },
  { pageId: "ADM-MER-003", label: "我的账单", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/bills", requiredPermissions: ["merchant.billing.view"], accountForms: ["merchant"] },
  { pageId: "ADM-MER-004", label: "我的工单", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/work-orders", requiredPermissions: ["merchant.service_work_order.view"], accountForms: ["merchant"] },
  { pageId: "ADM-MER-005", label: "我的对账", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/settlements", requiredPermissions: ["merchant.portal.settlement.view"], accountForms: ["merchant"] },
  { pageId: "ADM-MER-006", label: "我的合同", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/contracts", requiredPermissions: ["contract.view"], accountForms: ["merchant"] },
  { pageId: "ADM-MER-007", label: "费用通知", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/fee-recipients", requiredPermissions: ["merchant.portal.fee_recipient.manage"], accountForms: ["merchant"] },
  { pageId: "ADM-MER-008", label: "我的分红", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/dividends", requiredPermissions: ["finance.dividend.view"], accountForms: ["merchant"] },
  { pageId: "ADM-FLR-001", label: "商场平面图导入", module: "地产/空间", group: "ops", route: "/admin/space/floor-plans", requiredPermissions: ["floor_plan.view"] },
  { pageId: "ADM-COMP-001", label: "合规到期提醒台账", module: "合规", group: "compliance", route: "/admin/compliance/reminders", requiredPermissions: ["compliance.reminder.view"] },
  { pageId: "ADM-COMP-002", label: "企微会话合规监察", module: "合规", group: "compliance", route: "/admin/compliance/wecom", requiredPermissions: ["wecom.risk_hit.view"] },
];

// 业务域分组顺序与展示文案（frontend-admin §4.3：8 业务域 + 平台域聚合）。
// 渲染顺序即此数组顺序；未在 adminRoutes 中出现的分组会被 AdminApp 自动跳过。
export const adminNavGroupOrder: AdminNavGroup[] = [
  "overview",
  "merchant",
  "ticketing",
  "bar",
  "inventory",
  "amusement",
  "finance",
  "settlement",
  "vendor",
  "governance",
  "integration",
  "access",
  "risk",
  "compliance",
  "audit",
  "ops",
];

export const adminNavGroupLabels: Record<AdminNavGroup, string> = {
  overview: "看板/分析",
  merchant: "商户专区",
  ticketing: "票务",
  bar: "酒吧",
  inventory: "库存",
  amusement: "游乐",
  finance: "财务",
  settlement: "结算配置中心",
  vendor: "外包工作台",
  governance: "治理/合同",
  integration: "第三方对接",
  access: "账号权限",
  risk: "风控",
  compliance: "合规",
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

export function permissionCode(row: PermissionDefinitionRow) {
  return row.permissionCode ?? row.code ?? "";
}

/**
 * 权限点按左侧菜单模块分组（权限模板页、访问治理工作台新建角色共用）：从 adminRoutes 派生「权限域→菜单模块」，
 * 并按菜单模块出现顺序排列，使管理员按左侧目录的思路勾选权限。未匹配任何菜单的域归入「其他」置末。
 */
export function groupPermissionsByMenuModule(permissions: PermissionDefinitionRow[]): [string, PermissionDefinitionRow[]][] {
  const domainToModule = new Map<string, string>();
  const moduleOrder: string[] = [];
  for (const route of adminRoutes) {
    if (!moduleOrder.includes(route.module)) moduleOrder.push(route.module);
    for (const code of route.requiredPermissions ?? []) {
      const dom = code.split(".")[0];
      if (dom && !domainToModule.has(dom)) domainToModule.set(dom, route.module);
    }
  }
  const groups = new Map<string, PermissionDefinitionRow[]>();
  for (const row of permissions) {
    const domain = row.domain ?? permissionCode(row).split(".")[0] ?? "other";
    const moduleName = domainToModule.get(domain) ?? "其他";
    groups.set(moduleName, [...(groups.get(moduleName) ?? []), row]);
  }
  return [...moduleOrder, "其他"]
    .filter((m) => groups.has(m))
    .map((m) => [m, groups.get(m)!] as [string, PermissionDefinitionRow[]]);
}

/**
 * 账号形态判定（登录响应 user 字段）：
 * 外包机构账号 accountType=outsourced_vendor（后端 AuthorizationService 同口径）；
 * 平台/员工账号 accountType=platform_admin：即便种子数据挂了 merchantId 也归平台形态，
 *   否则会误当商户放行商户专区自助页（后端仍以登录主体判权 → 400 merchant_id is required）；
 * 其余带 merchantId 的账号看 merchantId（真实商户归属账号，与 merchant_portal 终端上报同口径）。
 */
export function accountFormOf(user: { accountType?: string | null; merchantId?: string | null } | null | undefined): AdminAccountForm {
  if (user?.accountType === "outsourced_vendor") return "vendor";
  if (user?.accountType === "platform_admin") return "platform";
  if (user?.merchantId) return "merchant";
  return "platform";
}

/** 该路由是否对某账号形态展示；未标 accountForms 的路由对全部形态展示。 */
export function routeAllowsAccountForm(route: AdminRoute, form: AdminAccountForm): boolean {
  return !route.accountForms || route.accountForms.includes(form);
}

/** 形态不匹配时的业务化提示（不再放请求打 403/400）。 */
export function accountFormHint(route: AdminRoute): string {
  if (route.accountForms?.includes("vendor")) return "此页面供外包机构账号使用，当前账号无需使用。";
  if (route.accountForms?.includes("merchant")) return "此页面供商户账号使用，当前账号无需使用。";
  return "此页面不适用于当前账号形态。";
}
