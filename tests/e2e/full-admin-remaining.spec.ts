import { expect, test } from "@playwright/test";
import { installMockApi } from "./support/mock-api";
import { fillCnDate } from "./support/cn-date";

type RouteSpec = {
  pageId: string;
  label: string;
  route: string;
};

const remainingRoutes: RouteSpec[] = [
  { pageId: "ADM-GOV-001", label: "集团/综合体拓扑", route: "/admin/governance/topology" },
  { pageId: "ADM-GOV-002", label: "组织与商户档案", route: "/admin/governance/organizations" },
  { pageId: "ADM-GOV-003", label: "四类主体绑定", route: "/admin/governance/subject-bindings" },
  { pageId: "ADM-CONT-001", label: "合同生命周期工作台", route: "/admin/contracts" },
  { pageId: "ADM-FAC-001", label: "楼宇运营工作台", route: "/admin/facility/workbench" },
  { pageId: "ADM-ENE-001", label: "能源工作台", route: "/admin/energy/workbench" },
  { pageId: "ADM-FLR-001", label: "商场平面图导入", route: "/admin/space/floor-plans" },
  { pageId: "ADM-COMP-001", label: "合规到期提醒台账", route: "/admin/compliance/reminders" },
  { pageId: "ADM-COMP-002", label: "企微会话合规监察", route: "/admin/compliance/wecom" },
  { pageId: "ADM-ACC-003", label: "访问治理工作台", route: "/admin/access/governance" },
  { pageId: "ADM-LEASE-001", label: "招商工作台", route: "/admin/leasing/workbench" },
  { pageId: "ADM-LEASE-002", label: "租赁状态看板", route: "/admin/leasing/rental-status" },
  { pageId: "ADM-FIN-005", label: "通票结算审阅", route: "/admin/finance/settlement-review" },
  { pageId: "ADM-AMU-005", label: "游乐核销记录", route: "/admin/amusement/verification-records" },
  { pageId: "ADM-FIN-004", label: "物业费管理", route: "/admin/finance/property-fee" },
  { pageId: "ADM-FIN-006", label: "商户费用台账", route: "/admin/finance/merchant-expenses" },
  { pageId: "ADM-FIN-007", label: "持股分红", route: "/admin/finance/dividends" },
  { pageId: "ADM-GOV-005", label: "商户准入审批流水", route: "/admin/governance/admission-audits" },
  { pageId: "ADM-GOV-006", label: "主管复核与违规阻断", route: "/admin/governance/supervisor-review" },
  { pageId: "ADM-GOV-007", label: "空间类型字典", route: "/admin/governance/space-types" },
  { pageId: "ADM-GOV-008", label: "场地营业时间", route: "/admin/governance/venue-business-hours" },
  { pageId: "ADM-CUS-001", label: "客人账号管理", route: "/admin/customers/accounts" },
  { pageId: "ADM-CUS-002", label: "券台账管理", route: "/admin/customers/vouchers" },
  { pageId: "ADM-CUS-004", label: "会员等级与忠诚度", route: "/admin/customers/loyalty-config" },
  { pageId: "ADM-CUS-005", label: "储值规则配置", route: "/admin/customers/stored-value-rules" },
  { pageId: "ADM-STL-001", label: "结算配置中心", route: "/admin/settlement/config" },
  { pageId: "ADM-STL-002", label: "开票清单", route: "/admin/settlement/invoice-items" },
  { pageId: "ADM-TER-001", label: "终端开关矩阵", route: "/admin/terminals/switch-matrix" },
  { pageId: "ADM-TER-002", label: "终端设备", route: "/admin/terminals/devices" },
  { pageId: "ADM-TKT-002", label: "动态定价", route: "/admin/ticketing/dynamic-pricing" },
  { pageId: "ADM-TKT-003", label: "出票与核销追踪", route: "/admin/ticketing/orders" },
  { pageId: "ADM-BAR-002", label: "订单/出品/账单", route: "/admin/bar/orders" },
  { pageId: "ADM-BAR-003", label: "存酒", route: "/admin/bar/stored-wine" },
  { pageId: "ADM-BAR-004", label: "销售提成", route: "/admin/bar/commissions" },
  { pageId: "ADM-BAR-005", label: "商品管理", route: "/admin/bar/products" },
  { pageId: "ADM-BAR-006", label: "套餐管理", route: "/admin/bar/combos" },
  { pageId: "ADM-AMU-001", label: "项目与场次", route: "/admin/amusement/sessions" },
  { pageId: "ADM-AMU-002", label: "团体/包场", route: "/admin/amusement/group-bookings" },
  { pageId: "ADM-AMU-003", label: "安全巡检/事故", route: "/admin/amusement/safety-incidents" },
  { pageId: "ADM-AMU-004", label: "巡检到期任务", route: "/admin/amusement/inspection-tasks" },
  { pageId: "ADM-INV-001", label: "库存台账", route: "/admin/inventory/ledger" },
  { pageId: "ADM-INV-002", label: "跨主体调拨与借调", route: "/admin/inventory/transfers" },
  { pageId: "ADM-FIN-002", label: "对账差异", route: "/admin/finance/reconciliation" },
  { pageId: "ADM-FIN-003", label: "退款分级授权", route: "/admin/finance/refund-authority" },
  { pageId: "ADM-MER-001", label: "商户服务工单", route: "/admin/merchant-service/tickets" },
  { pageId: "ADM-MER-002", label: "商户授权工作台", route: "/admin/merchant-portal/scope" },
  { pageId: "ADM-MER-003", label: "我的账单", route: "/admin/merchant-zone/bills" },
  { pageId: "ADM-MER-004", label: "我的工单", route: "/admin/merchant-zone/work-orders" },
  { pageId: "ADM-MER-005", label: "我的对账", route: "/admin/merchant-zone/settlements" },
  { pageId: "ADM-MER-006", label: "我的合同", route: "/admin/merchant-zone/contracts" },
  { pageId: "ADM-MER-007", label: "费用通知", route: "/admin/merchant-zone/fee-recipients" },
  { pageId: "ADM-MER-008", label: "我的分红", route: "/admin/merchant-zone/dividends" },
  { pageId: "ADM-VND-001", label: "外包维保工单", route: "/admin/vendor/work-orders" },
  { pageId: "ADM-VND-002", label: "外包巡检任务", route: "/admin/vendor/inspections" },
  { pageId: "ADM-VND-003", label: "外包进场报备", route: "/admin/vendor/site-entries" },
  { pageId: "ADM-VND-004", label: "外包下属账号", route: "/admin/vendor/subordinates" },
  { pageId: "ADM-INT-002", label: "回调/轮询/补偿", route: "/admin/integrations/events" },
  { pageId: "ADM-INT-003", label: "第三方开关", route: "/admin/integrations/switches" },
  { pageId: "ADM-INT-004", label: "短信通知开关", route: "/admin/integrations/sms-switches" },
  { pageId: "ADM-RISK-001", label: "风控预警", route: "/admin/risk/alerts" },
  { pageId: "ADM-EVD-001", label: "审计事件", route: "/admin/audit/events" },
  { pageId: "ADM-ANA-001", label: "经营分析", route: "/admin/analytics/business" },
  { pageId: "ADM-ANA-002", label: "集团总览", route: "/admin/analytics/group-overview" },
  { pageId: "ADM-ANA-003", label: "经营参数", route: "/admin/analytics/operating-params" },
  { pageId: "ADM-ANA-004", label: "欠费与回款", route: "/admin/analytics/receivables" },
  { pageId: "ADM-ANA-005", label: "出租与到期", route: "/admin/analytics/occupancy" },
  { pageId: "ADM-ANA-006", label: "工单时效", route: "/admin/analytics/work-order-sla" },
  { pageId: "ADM-ANA-007", label: "能耗分析", route: "/admin/analytics/energy" },
  { pageId: "ADM-ANA-008", label: "月度对比", route: "/admin/analytics/monthly" },
  { pageId: "ADM-ANA-010", label: "AI/对标阈值", route: "/admin/analytics/ai-thresholds" },
  { pageId: "ADM-ANA-011", label: "提醒与留存天数配置", route: "/admin/analytics/notification-windows" },
  { pageId: "ADM-ANA-009", label: "T3-A 只读经营分析", route: "/admin/analytics/t3a-readonly" },
  { pageId: "ADM-ANA-012", label: "指标明细", route: "/admin/metric-detail" },
  { pageId: "ADM-ANA-013", label: "放弃权益额外收入", route: "/admin/analytics/abandoned-entitlement-revenue" },
  { pageId: "ADM-STF-001", label: "排班与考勤", route: "/admin/staff/shifts" },
  { pageId: "ADM-STF-002", label: "薪酬与请假", route: "/admin/staff/payroll" },
  { pageId: "ADM-STF-003", label: "个税税表", route: "/admin/staff/tax-brackets" },
  { pageId: "ADM-STF-004", label: "员工档案", route: "/admin/staff/profiles" },
  { pageId: "ADM-SVC-001", label: "服务点单", route: "/admin/services/orders" },
  { pageId: "ADM-OPS-002", label: "数据清理工具", route: "/admin/ops/data-cleanup" },
];

async function adminLogin(page: import("@playwright/test").Page, path = "/admin/overview") {
  await page.goto(path);
  await expect(page.getByTestId("login-view")).toBeVisible();
  await page.getByTestId("login-username").fill("admin");
  await page.getByTestId("login-password").fill("admin-pass");
  await page.getByTestId("login-submit").click();
  await expect(page.getByTestId("admin-shell")).toBeVisible();
  await expect(page.locator(".loading-state")).toHaveCount(0);
}

async function openAdminRoute(page: import("@playwright/test").Page, spec: RouteSpec) {
  // ≤860px 侧栏默认收起为抽屉：先点汉堡按钮展开导航（导航后自动关闭）。桌面视口按钮隐藏，跳过。
  const navToggle = page.getByTestId("admin-nav-toggle");
  if (await navToggle.isVisible()) {
    await navToggle.click();
  }
  await page.getByTestId(`admin-nav-${spec.pageId}`).click();
  await expect(page).toHaveURL(new RegExp(`${spec.route}$`)); // nosemgrep: javascript.lang.security.audit.detect-non-literal-regexp.detect-non-literal-regexp -- spec.route 为硬编码测试路由(非用户输入)
  await expect(page.getByTestId("admin-page-title")).toContainText(spec.label);
  if (spec.pageId === "ADM-GOV-001") {
    await expect(page.getByTestId("governance-topology-page")).toBeVisible();
  } else if (spec.pageId === "ADM-GOV-002") {
    await expect(page.getByTestId("organization-merchant-page")).toBeVisible();
  } else if (spec.pageId === "ADM-GOV-003") {
    await expect(page.getByTestId("subject-bindings-page")).toBeVisible();
  } else if (spec.pageId === "ADM-BAR-002") {
    await expect(page.getByTestId("bar-orders-page")).toBeVisible();
  } else if (spec.pageId === "ADM-TKT-002") {
    await expect(page.getByTestId("dynamic-pricing-page")).toBeVisible();
  } else if (spec.pageId === "ADM-TKT-003") {
    await expect(page.getByTestId("ticket-orders-page")).toBeVisible();
  } else if (spec.pageId === "ADM-BAR-003") {
    await expect(page.getByTestId("stored-wine-page")).toBeVisible();
  } else if (spec.pageId === "ADM-BAR-004") {
    await expect(page.getByTestId("bar-commissions-page")).toBeVisible();
  } else if (spec.pageId === "ADM-BAR-005") {
    await expect(page.getByTestId("bar-products-page")).toBeVisible();
  } else if (spec.pageId === "ADM-BAR-006") {
    await expect(page.getByTestId("bar-combos-page")).toBeVisible();
  } else if (spec.pageId === "ADM-INV-001") {
    await expect(page.getByTestId("inventory-ledger-page")).toBeVisible();
  } else if (spec.pageId === "ADM-INV-002") {
    await expect(page.getByTestId("inventory-transfer-page")).toBeVisible();
    await expect(page.getByTestId("inventory-transfer-table")).toBeVisible();
  } else if (spec.pageId === "ADM-FIN-006") {
    await expect(page.getByTestId("finance-merchant-expense-page")).toBeVisible();
    await expect(page.getByTestId("merchant-expense-table")).toBeVisible();
  } else if (spec.pageId === "ADM-FIN-007") {
    await expect(page.getByTestId("finance-dividends-page")).toBeVisible();
    await expect(page.getByTestId("dividend-statement-table")).toBeVisible();
  } else if (spec.pageId === "ADM-MER-008") {
    await expect(page.getByTestId("merchant-zone-dividends-page")).toBeVisible();
    await expect(page.getByTestId("merchant-dividend-table")).toBeVisible();
  } else if (spec.pageId === "ADM-CUS-002") {
    await expect(page.getByTestId("voucher-ledger-page")).toBeVisible();
    await expect(page.getByTestId("voucher-instance-table")).toBeVisible();
  } else if (spec.pageId === "ADM-CUS-004") {
    await expect(page.getByTestId("member-loyalty-config-page")).toBeVisible();
    await expect(page.getByTestId("member-levels-table")).toBeVisible();
  } else if (spec.pageId === "ADM-CUS-005") {
    await expect(page.getByTestId("stored-value-rules-page")).toBeVisible();
    await expect(page.getByTestId("stored-value-empty-hint")).toBeVisible();
  } else if (spec.pageId === "ADM-STL-001") {
    await expect(page.getByTestId("settlement-config-page")).toBeVisible();
    await expect(page.getByTestId("settlement-config-defaults-table")).toBeVisible();
    // 批1：切到「通票分账」栏目，8 配置块之一（留存比例）应可见、可「改」。
    await page.getByRole("tab", { name: "通票分账" }).click();
    await expect(page.getByTestId("settlement-config-block-retention")).toBeVisible();
    await expect(page.getByTestId("settlement-config-edit-retention")).toBeVisible();
  } else if (spec.pageId === "ADM-STL-002") {
    await expect(page.getByTestId("settlement-invoice-page")).toBeVisible();
    await expect(page.getByTestId("settlement-invoice-subject-table")).toBeVisible();
    await expect(page.getByTestId("settlement-invoice-rule-table")).toBeVisible();
  } else if (spec.pageId === "ADM-FIN-002") {
    await expect(page.getByTestId("reconciliation-page")).toBeVisible();
  } else if (spec.pageId === "ADM-FIN-003") {
    await expect(page.getByTestId("refund-authority-page")).toBeVisible();
  } else if (spec.pageId === "ADM-MER-001") {
    await expect(page.getByTestId("merchant-work-orders-page")).toBeVisible();
  } else if (spec.pageId === "ADM-MER-002") {
    await expect(page.getByTestId("merchant-scope-page")).toBeVisible();
  } else if (spec.pageId === "ADM-MER-003") {
    await expect(page.getByTestId("merchant-zone-bills-page")).toBeVisible();
  } else if (spec.pageId === "ADM-MER-004") {
    await expect(page.getByTestId("merchant-zone-work-orders-page")).toBeVisible();
  } else if (spec.pageId === "ADM-MER-005") {
    await expect(page.getByTestId("merchant-zone-settlements-page")).toBeVisible();
  } else if (spec.pageId === "ADM-MER-006") {
    await expect(page.getByTestId("merchant-zone-contracts-page")).toBeVisible();
  } else if (spec.pageId === "ADM-MER-007") {
    await expect(page.getByTestId("merchant-zone-fee-recipients-page")).toBeVisible();
  } else if (spec.pageId === "ADM-VND-001") {
    await expect(page.getByTestId("vendor-work-orders-page")).toBeVisible();
    await expect(page.getByTestId("vendor-work-orders-table")).toBeVisible();
  } else if (spec.pageId === "ADM-VND-002") {
    await expect(page.getByTestId("vendor-inspections-page")).toBeVisible();
    await expect(page.getByTestId("vendor-inspections-table")).toBeVisible();
  } else if (spec.pageId === "ADM-VND-003") {
    await expect(page.getByTestId("vendor-site-entries-page")).toBeVisible();
    await expect(page.getByTestId("vendor-site-entries-table")).toBeVisible();
  } else if (spec.pageId === "ADM-AMU-001") {
    await expect(page.getByTestId("amusement-sessions-page")).toBeVisible();
  } else if (spec.pageId === "ADM-AMU-002") {
    await expect(page.getByTestId("amusement-group-bookings-page")).toBeVisible();
  } else if (spec.pageId === "ADM-AMU-003") {
    await expect(page.getByTestId("amusement-safety-page")).toBeVisible();
  } else if (spec.pageId === "ADM-AMU-004") {
    await expect(page.getByTestId("amusement-inspection-tasks-page")).toBeVisible();
  } else if (spec.pageId === "ADM-GOV-006") {
    await expect(page.getByTestId("supervisor-review-page")).toBeVisible();
  } else if (spec.pageId === "ADM-CONT-001") {
    await expect(page.getByTestId("contract-workbench-page")).toBeVisible();
  } else if (spec.pageId === "ADM-FAC-001") {
    await expect(page.getByTestId("facility-workbench-page")).toBeVisible();
  } else if (spec.pageId === "ADM-ENE-001") {
    await expect(page.getByTestId("energy-workbench-page")).toBeVisible();
  } else if (spec.pageId === "ADM-ACC-003") {
    await expect(page.getByTestId("access-governance-page")).toBeVisible();
  } else if (spec.pageId === "ADM-GOV-005") {
    await expect(page.getByTestId("admission-audits-page")).toBeVisible();
  } else if (spec.pageId === "ADM-RISK-001") {
    await expect(page.getByTestId("risk-alerts-page")).toBeVisible();
  } else if (spec.pageId === "ADM-INT-002") {
    await expect(page.getByTestId("integration-events-page")).toBeVisible();
  } else if (spec.pageId === "ADM-INT-003") {
    await expect(page.getByTestId("integration-switches-page")).toBeVisible();
  } else if (spec.pageId === "ADM-EVD-001") {
    await expect(page.getByTestId("audit-events-page")).toBeVisible();
  } else if (spec.pageId === "ADM-ANA-001") {
    // 深断言(异常计数/两表内容)在 ADM-REM-01 专区;此处仅保证可见,REM-03 空态复用不误伤。
    await expect(page.getByTestId("business-analytics-page")).toBeVisible();
  } else if (spec.pageId === "ADM-ANA-002") {
    await expect(page.getByTestId("admin-group-overview")).toBeVisible();
  } else if (spec.pageId === "ADM-SVC-001") {
    await expect(page.getByTestId("service-orders-page")).toBeVisible();
    await expect(page.getByTestId("service-catalog-table")).toContainText("点舞");
    await expect(page.getByTestId("service-orders-table")).toContainText("待收款");
    await expect(page.getByTestId("svc-payconfig-editor")).toBeVisible();
    await expect(page.getByTestId("service-catalog-editor")).toBeVisible();
  } else if (spec.pageId === "ADM-STF-002") {
    await expect(page.getByTestId("staff-payroll-page")).toBeVisible();
    await expect(page.getByTestId("payroll-table")).toContainText("¥8,000");
    await expect(page.getByTestId("leave-table")).toContainText("年假");
    await expect(page.getByTestId("payroll-adj-editor")).toBeVisible();
  } else if (spec.pageId === "ADM-STF-003") {
    await expect(page.getByTestId("payroll-tax-brackets-page")).toBeVisible();
    await expect(page.getByTestId("payroll-tax-brackets-table")).toContainText("20.00%");
    await expect(page.getByTestId("payroll-tax-brackets-form")).toBeVisible();
  } else if (spec.pageId === "ADM-STF-004") {
    await expect(page.getByTestId("staff-profiles-page")).toBeVisible();
    await expect(page.getByTestId("staff-profiles-table")).toContainText("销售岗");
    await expect(page.getByTestId("staff-profiles-reason")).toBeVisible();
  } else if (spec.pageId === "ADM-OPS-002") {
    await expect(page.getByTestId("data-cleanup-page")).toBeVisible();
    await expect(page.getByTestId("data-cleanup-request")).toBeVisible();
    await expect(page.getByTestId("data-cleanup-scope")).toBeVisible();
    await expect(page.getByTestId("data-cleanup-preview-button")).toBeVisible();
    await expect(page.getByTestId("data-cleanup-pending")).toContainText("待审批的清理申请");
  } else if (spec.pageId === "ADM-ANA-010") {
    await expect(page.getByTestId("ai-thresholds-page")).toBeVisible();
    await expect(page.getByTestId("ai-threshold-confidence")).toContainText("60%");
    await expect(page.getByTestId("ai-thresholds-form")).toBeVisible();
  } else if (spec.pageId === "ADM-ANA-012") {
    await expect(page.getByTestId("metric-detail-page")).toBeVisible();
    await expect(page.getByTestId("metric-detail-metric-select")).toBeVisible();
  } else if (spec.pageId === "ADM-ANA-013") {
    await expect(page.getByTestId("abandoned-entitlement-revenue-page")).toBeVisible();
    await expect(page.getByTestId("abandoned-cross-table")).toBeVisible();
  } else if (spec.pageId === "ADM-ANA-011") {
    await expect(page.getByTestId("notification-windows-page")).toBeVisible();
    await expect(page.getByTestId("notification-windows-table")).toContainText("续约");
  } else if (spec.pageId === "ADM-STF-001") {
    await expect(page.getByTestId("staff-shift-page")).toBeVisible();
    await expect(page.getByTestId("shift-table")).toContainText("usr_staff_1");
    await expect(page.getByTestId("attendance-table")).toContainText("迟到");
    await expect(page.getByTestId("shift-editor")).toBeVisible();
  } else if (spec.pageId === "ADM-ANA-008") {
    await expect(page.getByTestId("monthly-comparison-page")).toBeVisible();
    const comparisonTable = page.getByTestId("monthly-comparison-table");
    await expect(comparisonTable).toContainText("净收入");
    await expect(comparisonTable).toContainText("¥30,000");   // 300万分=3万元 本月
    await expect(comparisonTable).toContainText("+50.00%");   // 环比
    await expect(comparisonTable).toContainText("积累中");     // 缺同比不伪造
    await expect(page.getByTestId("monthly-comparison-export")).toBeVisible();
    await page.getByTestId("holiday-comparison-select").selectOption("春节|2026");
    await expect(page.getByTestId("holiday-comparison-table")).toContainText("¥10,000");   // 今年日均 100万分
    await expect(page.getByTestId("holiday-comparison-table")).toContainText("+25.00%");   // 按日均同比
    await expect(page.getByTestId("holiday-comparison-range")).toContainText("2025-01-28");
  } else if (spec.pageId === "ADM-ANA-006") {
    await expect(page.getByTestId("wo-sla-page")).toBeVisible();
    await expect(page.getByTestId("wo-response-p50")).toContainText("2.5 小时");
    await expect(page.getByTestId("wo-overdue-rate")).toContainText("25.00%");
    await expect(page.getByTestId("wo-overdue-table")).toContainText("示例超时工单");
    await expect(page.getByTestId("mwo-handle-p50")).toContainText("5.0 小时");
    await expect(page.getByTestId("mwo-overdue-rate")).toContainText("50.00%");
    await expect(page.getByTestId("mwo-backlog")).toContainText("3");
    await expect(page.getByTestId("mwo-overdue-table")).toContainText("示例超时工单");
  } else if (spec.pageId === "ADM-ANA-007") {
    await expect(page.getByTestId("energy-analytics-page")).toBeVisible();
    await expect(page.getByTestId("energy-total-electricity")).toContainText("1,200.5 kWh");
    await expect(page.getByTestId("energy-per-sqm")).toContainText("2.4 kWh/㎡");
    await expect(page.getByTestId("energy-abnormal-table")).toContainText("示例电表");
  } else if (spec.pageId === "ADM-ANA-005") {
    await expect(page.getByTestId("occupancy-analytics-page")).toBeVisible();
    await expect(page.getByTestId("occupancy-rate")).toContainText("80.00%");
    await expect(page.getByTestId("occupancy-summary")).toContainText("1,000 ㎡");
    await expect(page.getByTestId("occupancy-vacant-table")).toContainText("示例空铺");
    await expect(page.getByTestId("occupancy-expiring-table")).toContainText("contract-exp-1");
  } else if (spec.pageId === "ADM-ANA-004") {
    await expect(page.getByTestId("receivables-analytics-page")).toBeVisible();
    await expect(page.getByTestId("receivables-summary")).toContainText("欠费总额");
    await expect(page.getByTestId("receivables-aging-bars")).toBeVisible();
    await expect(page.getByTestId("receivables-debtors-table")).toContainText("欠费示例商户");
  } else if (spec.pageId === "ADM-ANA-003") {
    await expect(page.getByTestId("operating-params-page")).toBeVisible();
    await expect(page.getByTestId("anomaly-config-editor")).toBeVisible();
    await expect(page.getByTestId("anomaly-surge-input")).toHaveValue("50");
    await expect(page.getByTestId("holiday-calendar-table")).toContainText("春节");
    await expect(page.getByTestId("holiday-fetch-editor")).toBeVisible();
    await expect(page.getByTestId("business-day-cutoffs-table")).toBeVisible();
    await expect(page.getByTestId("cutoff-hour-bar")).toContainText("6:00");
  }
}

// 账号形态裁决（fix/web-critical）：商户专区/外包工作台页面只对匹配形态渲染菜单；
// 平台账号（admin）菜单不出现这些项，直链进入显示业务化提示（不放请求打 403/400）。
const merchantOnlyPages = new Set(["ADM-MER-003", "ADM-MER-004", "ADM-MER-005", "ADM-MER-006", "ADM-MER-007", "ADM-MER-008"]);
const vendorOnlyPages = new Set(["ADM-VND-001", "ADM-VND-002", "ADM-VND-003", "ADM-VND-004"]);

test.describe("full admin remaining pages acceptance", () => {
  test("ADM-REM-01 exposes all non-first admin pages with route, evidence and readable content", async ({ page }) => {
    test.slow(); // visits all remaining pages with detailed assertions; triple the timeout under parallel load
    await installMockApi(page);
    await adminLogin(page, "/admin/governance/topology");

    for (const spec of remainingRoutes) {
      if (merchantOnlyPages.has(spec.pageId) || vendorOnlyPages.has(spec.pageId)) {
        // 平台账号：形态不匹配的页面菜单不渲染（内容验收在 ADM-REM-04/05 用匹配账号覆盖）。
        await expect(page.getByTestId(`admin-nav-${spec.pageId}`)).toHaveCount(0);
        continue;
      }
      await expect(page.getByTestId(`admin-nav-${spec.pageId}`)).toBeVisible();
      await openAdminRoute(page, spec);
      if (spec.pageId === "ADM-ANA-009") {
        await expect(page.getByTestId("t3a-readonly-analytics-page")).toBeVisible();
        await expect(page.getByTestId("t3a-readonly-note")).toContainText("只读分析，不发控制指令");
        await expect(page.getByTestId("t3a-summary")).toBeVisible();
        continue;
      }
      if (spec.pageId === "ADM-FLR-001") {
        await expect(page.getByTestId("floor-plan-import-page")).toBeVisible();
        await expect(page.getByTestId("floor-plan-evidence")).toContainText("/api/v1/floor-plans");
        await expect(page.getByTestId("floor-plans-table")).toContainText("示例平面图");
        continue;
      }
      if (spec.pageId === "ADM-COMP-001") {
        await expect(page.getByTestId("compliance-reminders-page")).toBeVisible();
        await expect(page.getByTestId("compliance-reminders-evidence")).toContainText("/api/v1/compliance");
        await expect(page.getByTestId("compliance-reminders-summary")).toBeVisible();
        await expect(page.getByTestId("compliance-reminders-table")).toContainText("续约");
        continue;
      }
      if (spec.pageId === "ADM-COMP-002") {
        await expect(page.getByTestId("wecom-compliance-review-page")).toBeVisible();
        await expect(page.getByTestId("wecom-compliance-evidence")).toContainText("/api/v1/compliance/wecom");
        await expect(page.getByTestId("wecom-review-note")).toContainText("先选中一条命中");
        await expect(page.getByTestId("wecom-risk-hits-table")).toContainText("PRIVATE_CONTACT");
        continue;
      }
      if (spec.pageId === "ADM-GOV-001") {
        await expect(page.getByTestId("governance-topology-evidence")).toContainText("/api/v1/platform/business-entities");
        await expect(page.getByTestId("governance-topology-evidence")).toContainText("/api/v1/platform/spaces");
        await expect(page.getByTestId("governance-topology-summary")).toContainText("经营主体");
        await expect(page.getByTestId("governance-topology-summary")).toContainText("商户");
        await expect(page.getByTestId("governance-topology-table")).toContainText("Platform Org");
        await expect(page.getByTestId("governance-topology-table")).toContainText("Main Merchant");
        await expect(page.getByTestId("governance-topology-table")).toContainText("Main Venue");
        await expect(page.getByTestId("governance-topology-spaces-table")).toContainText("Gate A");
        await expect(page.getByTestId("governance-topology-boundary")).toContainText("集团、综合体、商户、项目和空间点位");
        continue;
      }
      if (spec.pageId === "ADM-GOV-002") {
        await expect(page.getByTestId("organization-merchant-evidence")).toContainText("/api/v1/platform/business-entities");
        await expect(page.getByTestId("organization-merchant-evidence")).toContainText("/api/v1/platform/merchants");
        await expect(page.getByTestId("organization-merchant-summary")).toContainText("商户档案");
        await expect(page.getByTestId("organization-entities-table")).toContainText("Platform Org");
        await expect(page.getByTestId("organization-merchants-table")).toContainText("Main Merchant");
        await expect(page.getByTestId("organization-projects-table")).toContainText("Main Venue");
        await expect(page.getByTestId("organization-merchant-boundary")).toContainText("运营主体、合同主体和结算主体");
        continue;
      }
      if (spec.pageId === "ADM-GOV-003") {
        await expect(page.getByTestId("subject-bindings-evidence")).toContainText("/api/v1/platform/business-entity-bindings");
        await expect(page.getByTestId("subject-bindings-summary")).toContainText("唯一绑定");
        await expect(page.getByTestId("subject-bindings-table")).toContainText("merchant-1");
        await expect(page.getByTestId("subject-bindings-table")).toContainText("Main Contract Co");
        await expect(page.getByTestId("subject-bindings-table")).toContainText("audit://admission/binding-1");
        await expect(page.getByTestId("subject-bindings-boundary")).toContainText("四类主体");
        continue;
      }
      if (spec.pageId === "ADM-BAR-002") {
        await expect(page.getByTestId("bar-orders-evidence")).toContainText("/api/v1/bar/orders");
        const barOrdersTable = page.getByTestId("bar-orders-table");
        await expect(barOrdersTable).toBeVisible();
        await expect(barOrdersTable).toContainText("order-1");
        await expect(barOrdersTable).toContainText("15%");
        continue;
      }
      if (spec.pageId === "ADM-TKT-002") {
        await expect(page.getByTestId("dynamic-pricing-evidence")).toContainText("/api/v1/ticketing/dynamic-pricing-rules");
        const dynamicPricingTable = page.getByTestId("dynamic-pricing-table");
        await expect(dynamicPricingTable).toBeVisible();
        await expect(dynamicPricingTable).toContainText("Weekend Boost");
        await expect(dynamicPricingTable).toContainText("百分比调整");
        await expect(dynamicPricingTable).toContainText("草稿");
        continue;
      }
      if (spec.pageId === "ADM-TKT-003") {
        await expect(page.getByTestId("ticket-orders-evidence")).toContainText("/api/v1/ticketing/orders");
        const ticketOrdersTable = page.getByTestId("ticket-orders-table");
        await expect(ticketOrdersTable).toBeVisible();
        await expect(ticketOrdersTable).toContainText("TO-MOCK-1");
        await expect(ticketOrdersTable).toContainText("未出票"); // 3.8 展示层中文业务口径（提交仍原码）
        // KPI 摘要改版为计数卡片,权益码默认值落在核销预检输入框
        await expect(page.getByTestId("ticket-entitlement-code")).toHaveValue("ENT-0001");
        continue;
      }
      if (spec.pageId === "ADM-BAR-003") {
        await expect(page.getByTestId("stored-wine-evidence")).toContainText("/api/v1/inventory/saved-bottles");
        await expect(page.getByTestId("stored-wine-summary")).toContainText("在线校验");
        const storedWineTable = page.getByTestId("stored-wine-table");
        await expect(storedWineTable).toBeVisible();
        await expect(storedWineTable).toContainText("SAVED-1");
        await expect(storedWineTable).toContainText("存酒中");
        continue;
      }
      if (spec.pageId === "ADM-BAR-004") {
        await expect(page.getByTestId("bar-commissions-evidence")).toContainText("/api/v1/bar/bills/{bill_id}/commissions/calculate");
        await expect(page.getByTestId("bar-commissions-summary")).toContainText("15%");
        await expect(page.getByTestId("bar-commissions-summary")).toContainText("10%");
        await expect(page.getByTestId("bar-commissions-boundary")).toContainText("不含通票");
        const commissionsTable = page.getByTestId("bar-commissions-table");
        await expect(commissionsTable).toBeVisible();
        await expect(commissionsTable).toContainText("order-1");
        await expect(commissionsTable).toContainText("15%");
        continue;
      }
      if (spec.pageId === "ADM-AMU-001") {
        await expect(page.getByTestId("amusement-sessions-evidence")).toContainText("/api/v1/amusement/projects");
        await expect(page.getByTestId("amusement-sessions-evidence")).toContainText("/api/v1/amusement/sessions");
        await expect(page.getByTestId("amusement-sessions-summary")).toContainText("场次");
        await expect(page.getByTestId("amusement-projects-table")).toContainText("Adventure");
        await expect(page.getByTestId("amusement-sessions-table")).toContainText("Morning Session");
        await expect(page.getByTestId("amusement-sessions-boundary")).toContainText("预约、核销和安全规则");
        continue;
      }
      if (spec.pageId === "ADM-AMU-002") {
        await expect(page.getByTestId("amusement-group-bookings-evidence")).toContainText("/api/v1/amusement/group-bookings");
        await expect(page.getByTestId("amusement-group-bookings-summary")).toContainText("团体");
        await expect(page.getByTestId("amusement-group-bookings-table")).toContainText("Team A");
        await expect(page.getByTestId("amusement-group-bookings-table")).toContainText("已通过");
        await expect(page.getByTestId("amusement-group-bookings-boundary")).toContainText("容量占用");
        continue;
      }
      if (spec.pageId === "ADM-INV-001") {
        await expect(page.getByTestId("inventory-ledger-evidence")).toContainText("/api/v1/inventory/stock-movements");
        await expect(page.getByTestId("inventory-ledger-summary")).toContainText("负库存禁止");
        await expect(page.getByTestId("inventory-ledger-items-table")).toContainText("WINE");
        await expect(page.getByTestId("inventory-ledger-balances-table")).toContainText("10");
        continue;
      }
      if (spec.pageId === "ADM-CUS-001") {
        await expect(page.getByTestId("customer-account-search")).toBeVisible();
        await page.getByLabel("客人手机号").fill("13900001234");
        await page.getByTestId("customer-account-search-button").click();
        await expect(page.getByTestId("customer-account-status")).toContainText("正常");
        continue;
      }
      if (spec.pageId === "ADM-TER-001") {
        await expect(page.getByTestId("terminal-master-switch-table")).toBeVisible();
        await expect(page.getByTestId("terminal-master-web")).toContainText("Web 前端");
        await expect(page.getByTestId("terminal-capability-group-tablet")).toContainText("camera.scan");
        continue;
      }
      if (spec.pageId === "ADM-TER-002") {
        // K16 设备商户归属工作台：台账表可见，含「所属商户」列与改绑入口。
        await expect(page.getByTestId("terminal-devices-table")).toBeVisible();
        await expect(page.getByTestId("terminal-devices-table")).toContainText("所属商户");
        await expect(page.getByTestId("terminal-devices-table")).toContainText("改绑商户");
        continue;
      }
      if (spec.pageId === "ADM-FIN-002") {
        await expect(page.getByTestId("reconciliation-evidence")).toContainText("/api/v1/finance/reconciliation/differences");
        await expect(page.getByTestId("reconciliation-summary")).toContainText("正式关闭接口已就绪");
        await expect(page.getByTestId("reconciliation-differences-table")).toContainText("DIFF-1");
        await expect(page.getByTestId("reconciliation-boundary")).toContainText("原因和附件");
        continue;
      }
      if (spec.pageId === "ADM-MER-001") {
        await expect(page.getByTestId("merchant-work-orders-evidence")).toContainText("/api/v1/merchant-service/work-orders");
        await expect(page.getByTestId("merchant-work-orders-summary")).toContainText("待处理");
        await expect(page.getByTestId("merchant-work-orders-table")).toContainText("空调漏水");
        await expect(page.getByTestId("merchant-work-orders-boundary")).toContainText("维修 / 投诉 / 整改 / 服务");
        continue;
      }
      if (spec.pageId === "ADM-AMU-003") {
        await expect(page.getByTestId("amusement-safety-evidence")).toContainText("/api/v1/amusement/safety-inspections");
        await expect(page.getByTestId("amusement-safety-summary")).toContainText("待整改");
        await expect(page.getByTestId("amusement-safety-table")).toContainText("SAFE-1");
        await expect(page.getByTestId("amusement-safety-boundary")).toContainText("事故工单深度闭环");
        continue;
      }
      if (spec.pageId === "ADM-CONT-001") {
        // 合同生命周期工作台（V162 重建）：起草→并行会签→生效固化→变更/退租结算 + 审批链/到期着色配置。
        // 页面分标签：① 合同（默认）/ ② 审批与配置（审批链表）/ ③ 变更与结算（结算表）；断言各表须先切到其所在标签。
        await expect(page.getByTestId("contract-workbench-page")).toBeVisible();
        await expect(page.getByTestId("contracts-table")).toBeVisible();
        await expect(page.getByTestId("contract-draft-open")).toBeVisible();
        await page.getByRole("tab", { name: "② 审批与配置" }).click();
        await expect(page.getByTestId("contract-flow-table")).toBeVisible();
        await page.getByRole("tab", { name: "③ 变更与结算" }).click();
        await expect(page.getByTestId("contract-settlements-table")).toBeVisible();
        continue;
      }
      if (spec.pageId === "ADM-FAC-001") {
        // 楼宇运营工作台（V165-167）：设备台账 + 维保工单 + 巡检任务分标签，逐标签核对表格。
        await expect(page.getByTestId("facility-workbench-page")).toBeVisible();
        await expect(page.getByTestId("facility-equipment-table")).toBeVisible();
        await page.getByRole("tab", { name: "② 维保工单" }).click();
        await expect(page.getByTestId("facility-work-orders-table")).toBeVisible();
        await page.getByRole("tab", { name: "③ 楼宇巡检" }).click();
        await expect(page.getByTestId("facility-inspection-tasks-table")).toBeVisible();
        continue;
      }
      if (spec.pageId === "ADM-ENE-001") {
        // 能源工作台（V168）：表计台账 + 抄表抹差 + 能源费结算分标签，逐标签核对表格。
        await expect(page.getByTestId("energy-workbench-page")).toBeVisible();
        await expect(page.getByTestId("energy-meters-table")).toBeVisible();
        await page.getByRole("tab", { name: "② 抄表与抹差" }).click();
        await expect(page.getByTestId("energy-readings-table")).toBeVisible();
        await page.getByRole("tab", { name: "③ 能源费结算" }).click();
        await expect(page.getByTestId("energy-settlements-table")).toBeVisible();
        continue;
      }
      if (spec.pageId === "ADM-ACC-003") {
        // 访问治理工作台（V169-170）：角色装配（默认）+ 高危授权审批分标签，切标签核对授权申请表。
        await expect(page.getByTestId("access-governance-page")).toBeVisible();
        await expect(page.getByTestId("access-position-role-templates-table")).toBeVisible();
        await page.getByRole("tab", { name: "② 高危授权审批" }).click();
        await expect(page.getByTestId("access-grant-requests-table")).toBeVisible();
        continue;
      }
      if (spec.pageId === "ADM-GOV-005") {
        await expect(page.getByTestId("admission-audits-evidence")).toContainText("/api/v1/platform/merchants");
        await expect(page.getByTestId("admission-audits-evidence")).toContainText("/api/v1/platform/business-entity-bindings");
        await expect(page.getByTestId("admission-audits-evidence")).toContainText("/api/v1/contracts");
        await expect(page.getByTestId("admission-audits-summary")).toContainText("准入闭环");
        await expect(page.getByTestId("admission-audits-merchants-table")).toContainText("Main Merchant");
        await expect(page.getByTestId("admission-audits-bindings-table")).toContainText("Main Contract Co");
        await expect(page.getByTestId("admission-audits-bindings-table")).toContainText("audit://admission/binding-1");
        await expect(page.getByTestId("admission-audits-contracts-table")).toContainText("contract-subject-1");
        await expect(page.getByTestId("admission-audits-contracts-table")).toContainText("att-contract-1");
        await expect(page.getByTestId("admission-audits-boundary")).toContainText("商户档案、四类主体绑定、合同审批");
        continue;
      }
      if (spec.pageId === "ADM-GOV-006") {
        await expect(page.getByTestId("supervisor-review-evidence")).toContainText("/api/v1/platform/supervisor-review/evaluate");
        await expect(page.getByTestId("supervisor-review-summary")).toContainText("默认阻断");
        await expect(page.getByTestId("supervisor-review-table")).toContainText("contract_violation");
        await expect(page.getByTestId("supervisor-review-table")).toContainText("base_definition_violation");
        await expect(page.getByTestId("supervisor-review-boundary")).toContainText("违规动作必须先阻断");
        await expect(page.getByTestId("supervisor-review-current-review")).toContainText("未生成");
        await expect(page.getByTestId("supervisor-review-approve")).toBeDisabled();
        continue;
      }
      if (spec.pageId === "ADM-RISK-001") {
        await expect(page.getByTestId("risk-alerts-evidence")).toContainText("/api/v1/analytics/risk-alerts");
        await expect(page.getByTestId("risk-alerts-summary")).toContainText("高风险");
        await expect(page.getByTestId("risk-alerts-table")).toContainText("Mock risk");
        await expect(page.getByTestId("risk-alerts-boundary")).toContainText("忽略高风险预警需审批");
        continue;
      }
      if (spec.pageId === "ADM-INT-002") {
        await expect(page.getByTestId("integration-events-evidence")).toContainText("/api/v1/integration/retry-tasks");
        await expect(page.getByTestId("integration-events-evidence")).toContainText("/api/v1/integration/voucher-verifications");
        await expect(page.getByTestId("integration-events-summary")).toContainText("待处理");
        await expect(page.getByTestId("integration-events-table")).toContainText("RT-1");
        await expect(page.getByTestId("integration-events-table")).toContainText("CP-1");
        await expect(page.getByTestId("integration-events-table")).toContainText("SYNC-1");
        await expect(page.getByTestId("integration-events-table")).toContainText("VCH-1");
        await expect(page.getByTestId("integration-events-summary")).toContainText("四类读取接口");
        await expect(page.getByTestId("integration-events-boundary")).toContainText("Webhook + 定时轮询校验");
        await expect(page.getByTestId("integration-events-boundary")).toContainText("不得伪造第三方生产补偿执行");
        continue;
      }
      if (spec.pageId === "ADM-MER-002") {
        await expect(page.getByTestId("merchant-scope-evidence")).toContainText("/api/v1/merchant-service/work-orders");
        await expect(page.getByTestId("merchant-scope-evidence")).toContainText("/api/v1/merchant-billing/bills");
        await expect(page.getByTestId("merchant-scope-summary")).toContainText("授权范围");
        await expect(page.getByTestId("merchant-scope-work-orders-table")).toContainText("merchant-1:work-orders");
        await expect(page.getByTestId("merchant-scope-bills-table")).toContainText("merchant-1:billing");
        await expect(page.getByTestId("merchant-scope-bills-table")).toContainText("audit://merchant-scope/bill-1");
        await expect(page.getByTestId("merchant-scope-boundary")).toContainText("不得访问平台治理");
        continue;
      }
      if (spec.pageId === "ADM-EVD-001") {
        await expect(page.getByTestId("audit-events-evidence")).toContainText("/api/v1/evidence/events");
        await expect(page.getByTestId("audit-events-summary")).toContainText("本页成功");
        await expect(page.getByTestId("audit-events-table")).toContainText("event-trace-1");
        await page.getByTestId("audit-events-search").fill("ticket");
        await expect(page.getByTestId("audit-events-table")).toContainText("ticket.order.created");
        await page.getByTestId("audit-events-search").fill("no-match");
        await expect(page.getByTestId("audit-events-filter-empty")).toContainText("无匹配审计事件");
        await page.getByTestId("audit-events-search").fill("event-trace-1");
        await page.getByTestId("audit-events-show-detail").click();
        await expect(page.getByTestId("audit-events-detail")).toContainText("beforeAfter");
        await page.getByTestId("audit-events-export-reason").fill("验收导出审计事件");
        await page.getByTestId("audit-events-export").click();
        await expect(page.getByTestId("admin-action-result")).toContainText("audit_events_exported");
        await expect(page.getByTestId("admin-action-result")).toContainText("evidence.events_reported");
        continue;
      }
      if (spec.pageId === "ADM-ANA-001") {
        await expect(page.getByTestId("business-analytics-evidence")).toContainText("/api/v1/analytics/business-metric-snapshots");
        await expect(page.getByTestId("business-analytics-evidence")).toContainText("日环比突变检测");
        await expect(page.getByTestId("business-analytics-summary")).toContainText("上升");
        await expect(page.getByTestId("business-analytics-table")).toContainText("gmv_amount_cents");
        await expect(page.getByTestId("business-analytics-definitions-table")).toContainText("gmv_amount_cents");
        await expect(page.getByTestId("business-analytics-anomalies-table")).toContainText("revenue.net");
        await expect(page.getByTestId("business-analytics-anomaly-count")).toContainText("1");
        await page.getByTestId("business-analytics-domain-filter").selectOption("bar");
        await expect(page.getByTestId("business-analytics-table")).toContainText("bar_order_count");
        await page.getByTestId("business-analytics-domain-filter").selectOption("all");
        // 引导式 phase-9：新建复盘锁改分步弹窗（入口→填说明→确认→提交）。
        await page.getByTestId("business-analytics-lock-open").click();
        await page.getByTestId("business-analytics-lock-note").fill("总控周会复盘锁定");
        await page.getByTestId("business-analytics-lock-guided-next").click();
        await page.getByTestId("business-analytics-lock-snapshot").click();
        await expect(page.getByTestId("admin-action-result")).toContainText("business_snapshot_locked");
        // V216 复盘锁真落库后不再返回 management_review_ready 状态,以审计键为准
        await expect(page.getByTestId("admin-action-result")).toContainText("analytics.review.locked");
        continue;
      }
      if (spec.pageId === "ADM-ANA-002") {
        await expect(page.getByTestId("group-overview-table")).toBeVisible();
        await expect(page.getByTestId("group-overview-row-project_1")).toBeVisible();
        continue;
      }
      if (spec.pageId === "ADM-BAR-005") {
        const barProductsTable = page.getByTestId("bar-products-table");
        await expect(barProductsTable).toBeVisible();
        await expect(barProductsTable).toContainText("Mock Drink");
        continue;
      }
      if (spec.pageId === "ADM-INT-003") {
        const switchesTable = page.getByTestId("integration-switches-table");
        await expect(switchesTable).toBeVisible();
        await expect(switchesTable).toContainText("face.verification.enabled");
        continue;
      }
    }

    // 平台账号直链进入形态受限页：显示业务化提示，不渲染页面（会话已持久化，刷新后仍在登录态）。
    await page.goto("/admin/vendor/subordinates");
    await expect(page.getByTestId("admin-account-form-mismatch")).toContainText("外包机构账号");
    await page.goto("/admin/merchant-zone/bills");
    await expect(page.getByTestId("admin-account-form-mismatch")).toContainText("商户账号");
  });

  test("ADM-REM-02 governance, contract, ticketing, bar and amusement actions are auditable", async ({ page }) => {
    test.slow(); // exercises auditable actions across many modules; triple the timeout under parallel load
    await installMockApi(page);
    await adminLogin(page);

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-GOV-002")!);
    // 引导式流程一期：主按钮打开分步弹窗 → 基本信息 → 联系人 → 经营与租赁 → 确认页创建。
    await page.getByTestId("organization-merchant-create-open").click();
    await page.getByTestId("organization-merchant-name").fill("二期测试商户");
    await page.getByTestId("organization-merchant-flow-next").click();
    await page.getByTestId("organization-merchant-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant_profile_created");
    await expect(page.getByTestId("admin-action-result")).toContainText("Created Merchant");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-GOV-003")!);
    await page.getByTestId("subject-bindings-change-reason").fill("总控验收主体绑定变更");
    await page.getByTestId("subject-bindings-submit-change").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("binding_changed");
    await expect(page.getByTestId("admin-action-result")).toContainText("evidence-full-admin");

    // 合同生命周期工作台（V162 重建）：起草合同为可审计写动作（商户/主体/起止日期必填，POST /api/v1/contracts）。
    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-CONT-001")!);
    // 引导式流程（单屏范式 singlePage=true）：合同双方 + 租期租金 + 结算方式 + 经营范围 同屏，一次「下一步」到确认页 → 起草。
    await page.getByTestId("contract-draft-open").click();
    await page.getByLabel("商户 id").selectOption("merchant-1");
    // 合同主体下拉选项来自 /platform/business-entities（mock 仅 org-1）；结果断言只看商户。
    await page.getByLabel("合同主体 id").selectOption("org-1");
    await fillCnDate(page, page.getByLabel("生效日期"), "2026-07-01");
    await fillCnDate(page, page.getByLabel("到期日期"), "2027-06-30");
    await page.getByTestId("contract-draft-next").click(); // 填写信息 → 确认信息
    await page.getByTestId("contract-draft-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("contract_drafted");
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant-1");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-GOV-005")!);
    // 引导式 phase-9：登记证据事件改分步弹窗（入口→填原因→确认→提交）。
    await page.getByTestId("admission-audits-create-open").click();
    await page.getByTestId("admission-audits-export-reason").fill("总控导出准入审计片段");
    await page.getByTestId("admission-audits-guided-next").click();
    await page.getByTestId("admission-audits-export").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("admission_audit_exported");
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant_admission_trace_ready");
    await expect(page.getByTestId("admin-action-result")).toContainText("att-admission-audit-export");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-GOV-006")!);
    await expect(page.getByTestId("supervisor-review-approve")).toBeDisabled();
    await page.getByTestId("supervisor-review-violation-summary").fill("合同主体与基础定义不一致");
    await page.getByTestId("supervisor-review-evaluate").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("supervisor_review_evaluated");
    await expect(page.getByTestId("admin-action-result")).toContainText("pending_review");
    await expect(page.getByTestId("admin-action-result")).toContainText("evidenceAttachmentIds");
    await expect(page.getByTestId("supervisor-review-current-review")).toContainText("review-demo");
    await expect(page.getByTestId("supervisor-review-approve")).toBeEnabled();
    await page.getByTestId("supervisor-review-approval-comment").fill("证据齐备，同意放行");
    await page.getByTestId("supervisor-review-approve").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("supervisor_review_approved");
    await expect(page.getByTestId("admin-action-result")).toContainText("approved");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-TKT-002")!);
    await page.getByTestId("dynamic-pricing-simulate").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("finalPriceCent");
    // 引导式流程 phase-3:新建定价规则改弹窗(规则名默认周末上浮/调整率默认10,均有效)→翻页→提交。
    await page.getByTestId("dynamic-pricing-create-open").click();
    await page.getByTestId("dynamic-pricing-guided-next").click();
    await page.getByTestId("dynamic-pricing-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("draft");
    await page.getByTestId("dynamic-pricing-publish").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("published");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-TKT-003")!);
    await page.getByTestId("ticket-order-issue").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("issued");
    await page.getByTestId("ticket-verification-preview").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("preview_ok");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-BAR-002")!);
    await page.getByTestId("bar-order-create-bill").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("billed");
    await page.getByTestId("bar-order-deliver").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("delivered");
    await page.getByTestId("bar-order-settle").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("paid");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-BAR-003")!);
    await page.getByTestId("stored-wine-claim-code").fill("123456");
    await page.getByTestId("stored-wine-claim").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("claimed");
    await expect(page.getByTestId("admin-action-result")).toContainText("claim_items");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-BAR-004")!);
    await page.getByTestId("bar-commission-calculate").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("calculated");
    await expect(page.getByTestId("admin-action-result")).toContainText("includeTicketAmount");
    await expect(page.getByTestId("admin-action-result")).toContainText("false");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-INV-001")!);
    await page.getByTestId("inventory-movement-open").click();
    await page.getByTestId("inventory-movement-quantity").fill("1");
    await page.getByTestId("inventory-movement-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("inventory_stock_movement_created");
    await expect(page.getByTestId("admin-action-result")).toContainText("from_location_id");
    await expect(page.getByTestId("admin-action-result")).toContainText("items");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-FIN-002")!);
    await page.getByTestId("reconciliation-review-reason").fill("支付通道入账时间差异，待供应商对账单复核");
    await page.getByTestId("reconciliation-review-attachment").fill("att-recon-1");
    await page.getByTestId("reconciliation-review-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("reconciliation_difference_closed");
    await expect(page.getByTestId("admin-action-result")).toContainText("att-recon-1");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-AMU-001")!);
    // 引导式流程 phase-7：新增场次收起为主按钮 + 分步弹窗（入口→填字段→下一步→提交）。
    await page.getByTestId("amusement-session-create-open").click();
    await page.getByTestId("amusement-session-name").fill("验收场次");
    await page.getByTestId("amusement-session-capacity").fill("12");
    await page.getByTestId("amusement-session-guided-next").click();
    await page.getByTestId("amusement-session-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("amusement_session_created");
    await expect(page.getByTestId("admin-action-result")).toContainText("P0 Web Session");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-AMU-002")!);
    await page.getByTestId("amusement-group-booking-release-reason").fill("验收释放团体容量");
    await page.getByTestId("amusement-group-booking-release").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("group_booking_released");
    await expect(page.getByTestId("admin-action-result")).toContainText("released");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-AMU-003")!);
    await page.getByTestId("amusement-safety-create-open").click();
    await page.getByTestId("amusement-safety-create-title").fill("开放前护栏检查");
    // 单屏引导校验必填：巡检项目（下拉）；结果选「不合格」以驱动后续提交→整改流程。
    await page.getByTestId("amusement-safety-create-project").selectOption("amuse-1");
    await page.getByTestId("amusement-safety-create-result").selectOption("fail");
    await page.getByTestId("amusement-safety-guided-next").click();
    await page.getByTestId("amusement-safety-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("amusement_safety_inspection_created");
    await page.getByTestId("amusement-safety-submit").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("amusement_safety_inspection_submitted");
    await page.getByTestId("amusement-safety-rectify-note").fill("已更换防护垫并上传照片");
    await page.getByTestId("amusement-safety-rectify").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("amusement_safety_rectified");
    await page.getByTestId("amusement-safety-recheck").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("amusement_safety_rechecked");
    await page.getByTestId("amusement-safety-approve-reopen").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("amusement_safety_reopen_approved");
    await expect(page.getByTestId("admin-action-result")).toContainText("evidenceAttachmentIds");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-MER-001")!);
    await page.getByTestId("merchant-work-order-create-open").click();
    await page.getByTestId("merchant-work-order-title").fill("排水异味投诉");
    await page.getByTestId("merchant-work-order-guided-next").click();
    await page.getByTestId("merchant-work-order-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant_work_order_created");
    await expect(page.getByTestId("admin-action-result")).toContainText("idempotencyKey");
    await page.getByTestId("merchant-work-order-assign").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant_work_order_assigned");
    await page.getByTestId("merchant-work-order-process-note").fill("已到场排查，等待商户确认");
    await page.getByTestId("merchant-work-order-process").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant_work_order_processed");
    await page.getByTestId("merchant-work-order-resolution").fill("已维修完成，商户确认关闭");
    await page.getByTestId("merchant-work-order-close").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant_work_order_closed");
    await expect(page.getByTestId("admin-action-result")).toContainText("evidenceAttachmentIds");
  });

  test("ADM-REM-03 risk, audit, analytics and integration pages cover empty/error/mobile states", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await installMockApi(page, { errorPath: "/api/v1/evidence/events" });
    await adminLogin(page, "/admin/risk/alerts");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-RISK-001")!);
    await page.getByTestId("risk-alert-create-open").click();
    await page.getByTestId("risk-alert-summary-input").fill("员工异常折扣触发高风险预警");
    await page.getByTestId("risk-alert-create").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("risk_alert_created");
    await expect(page.getByTestId("admin-action-result")).toContainText("att-risk-alert-create");
    await page.getByTestId("risk-alert-resolution-reason").fill("已复核订单、员工和支付证据，确认关闭");
    await page.getByTestId("risk-alert-resolve").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("risk_alert_resolved");
    await expect(page.getByTestId("admin-action-result")).toContainText("resolved");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-INT-002")!);
    await page.getByTestId("integration-events-polling-note").fill("已完成 Webhook 与定时轮询一致性校验");
    await page.getByTestId("integration-events-log-polling").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("integration_polling_checked");
    // 该页已从通用页升级为专属 IntegrationEventsPage：登记轮询校验 POST /integration/polling-checks，
    // 回显真实结果（status=recorded + 反伪造边界），不再是旧通用配置的 webhook_plus_polling_verified 占位。
    await expect(page.getByTestId("admin-action-result")).toContainText("platform_task_status_only_not_supplier_execution");
    await expect(page.getByTestId("admin-action-result")).toContainText("att-integration-polling");
    await page.getByTestId("integration-events-retry-reason").fill("供应商超时后人工触发重试");
    await page.getByTestId("integration-events-run-retry").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("integration_retry_run_now");
    await expect(page.getByTestId("admin-action-result")).toContainText("ready");
    await page.getByTestId("integration-events-compensation-note").fill("已核对订单和券核销回流，处理补偿任务");
    await page.getByTestId("integration-events-resolve-compensation").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("integration_compensation_resolved");
    await expect(page.getByTestId("admin-action-result")).toContainText("resolved");
    await expect(page.getByTestId("admin-action-result")).toContainText("att-integration-compensation");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-EVD-001")!);
    // 错误翻译层：正文给业务话术，原始报文（含 SERVER_ERROR）收进折叠的技术详情供报障。
    await expect(page.getByTestId("admin-error")).toContainText("服务暂时不可用");
    await expect(page.getByTestId("admin-error-detail")).toContainText("SERVER_ERROR");

    await installMockApi(page, { empty: true });
    // 会话现持久化到 localStorage：先清会话再刷新，才能回到登录视图重新登录。
    await page.evaluate(() => window.localStorage.clear());
    await page.reload();
    await adminLogin(page, "/admin/analytics/business");
    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-ANA-001")!);
    await expect(page.getByTestId("business-analytics-empty")).toContainText("暂无经营指标");

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(80);
  });
  test("ADM-REM-04 merchant scoped role can use authorized merchant workspace but is denied platform pages", async ({ page }) => {
    await installMockApi(page, { user: { username: "merchant-user", accountType: "merchant_operator", merchantId: "merchant-1" } });
    await adminLogin(page, "/admin/merchant-portal/scope");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-MER-002")!);
    await expect(page.getByTestId("merchant-scope-bills-table")).toContainText("merchant-1:billing");
    await expect(page.getByTestId("merchant-scope-work-orders-table")).toContainText("merchant-1:work-orders");
    await page.getByTestId("merchant-scope-confirm").click();
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant_scope_confirmed");
    await expect(page.getByTestId("admin-action-result")).toContainText("merchant_authorized_scope_checked");

    // 商户专区一期:merchant 账号整个会话按 merchant_portal 终端上报,我的账单页可用(后端 currentMerchantScope 只回本商户)。
    const billsRequestPromise = page.waitForRequest((request) => request.url().includes("/api/v1/merchant-billing/bills"));
    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-MER-003")!);
    expect((await billsRequestPromise).headers()["x-terminal-type"]).toBe("merchant_portal");
    await expect(page.getByTestId("merchant-zone-bills-summary")).toContainText("账单总数");
    await expect(page.getByTestId("merchant-zone-bills-table")).toContainText("MBILL-1");
    await expect(page.getByTestId("merchant-zone-bills-table")).toContainText("120,000.00");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-MER-004")!);
    await expect(page.getByTestId("merchant-zone-work-orders-summary")).toContainText("工单总数");
    await expect(page.getByTestId("merchant-zone-work-orders-table")).toContainText("空调漏水");
    await expect(page.getByTestId("merchant-zone-work-order-form")).toBeVisible();

    // 账号形态裁决：商户专区页内容验收从 ADM-REM-01（平台账号已不可见）移到商户账号会话。
    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-MER-005")!);
    await expect(page.getByTestId("merchant-zone-settlements-summary")).toContainText("结算单");
    await expect(page.getByTestId("merchant-zone-settlements-table")).toContainText("cst-1");
    await expect(page.getByTestId("merchant-zone-settlement-dispute-form")).toBeVisible();

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-MER-006")!);
    await expect(page.getByTestId("merchant-zone-contracts-summary")).toContainText("合同总数");
    await expect(page.getByTestId("merchant-zone-contracts-table")).toContainText("ntract-1");

    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-MER-007")!);
    await expect(page.getByTestId("merchant-zone-fee-recipients-summary")).toContainText("接收人配置");
    await expect(page.getByTestId("merchant-zone-fee-recipients-table")).toContainText("15577000001");
    await expect(page.getByTestId("merchant-zone-fee-recipient-form")).toBeVisible();

    // 商户专区主理人分红只读视图（V299）：每期金额 + 状态可见，无审批入口。
    await openAdminRoute(page, remainingRoutes.find((item) => item.pageId === "ADM-MER-008")!);
    await expect(page.getByTestId("merchant-dividend-table")).toContainText("2026-Q2");
    await expect(page.getByTestId("merchant-dividend-table")).toContainText("1,040.00");
    await expect(page.getByTestId("merchant-dividend-table")).toContainText("已审批");

    // 商户形态看不到外包工作台菜单。
    await expect(page.getByTestId("admin-nav-ADM-VND-001")).toHaveCount(0);

    await page.getByTestId("admin-logout").click();
    await adminLogin(page, "/admin/finance/ar-ap-ledger");
    await expect(page.getByTestId("admin-permission-denied")).toContainText("权限不足");
  });

  test("ADM-REM-05 outsourced vendor can use assigned workbench pages only", async ({ page }) => {
    await installMockApi(page, { user: { id: "vendor-user", username: "vendor-user", accountType: "outsourced_vendor" } });
    await adminLogin(page, "/admin/vendor/work-orders");

    for (const spec of remainingRoutes.filter((item) => item.pageId.startsWith("ADM-VND-"))) {
      await expect(page.getByTestId(`admin-nav-${spec.pageId}`)).toBeVisible();
      await openAdminRoute(page, spec);

      // 账号形态裁决：外包工作台页内容验收从 ADM-REM-01（平台账号已不可见）移到外包账号会话。
      if (spec.pageId === "ADM-VND-001") {
        await expect(page.getByTestId("vendor-work-orders-table")).toContainText("VND-WO-1");
        await expect(page.getByTestId("vendor-work-orders-table")).toContainText("vendor-user");
        await expect(page.getByTestId("vendor-work-order-accept")).toHaveCount(0);
        await expect(page.getByTestId("vendor-work-orders-evidence")).toContainText("/api/v1/facility/equipment");
        await expect(page.getByTestId("vendor-work-order-equipment-table")).toContainText("空调主机");
      }
      if (spec.pageId === "ADM-VND-002") {
        await expect(page.getByTestId("vendor-inspections-table")).toContainText("夜间安保巡检");
        await expect(page.getByTestId("vendor-inspections-table")).toContainText("vendor-user");
        await expect(page.getByTestId("vendor-inspection-templates-table")).toContainText("夜间安保模板");
        await expect(page.getByTestId("vendor-inspection-report-exception")).toBeVisible();
        await page.getByTestId("vendor-inspection-report-exception").click();
        await expect(page.getByTestId("admin-action-result")).toContainText("vendor_inspection_exception_reported");
      }
      if (spec.pageId === "ADM-VND-003") {
        await expect(page.getByTestId("vendor-site-entries-table")).toContainText("店铺装修围挡施工");
        await expect(page.getByTestId("vendor-site-entries-table")).toContainText("vendor-user");
        await expect(page.getByTestId("vendor-site-entry-completion-upload")).toBeEnabled();
        await page.getByTestId("vendor-site-entry-completion-note").fill("完工材料已上传");
        await page.getByTestId("vendor-site-entry-completion-attachments").fill("att-completion-1");
        await page.getByTestId("vendor-site-entry-completion-upload").click();
        await expect(page.getByTestId("admin-action-result")).toContainText("vendor_site_entry_completion_uploaded");
      }
    }

    await expect(page.getByTestId("admin-nav-ADM-FAC-001")).toBeDisabled();
    // 外包形态看不到商户专区菜单。
    await expect(page.getByTestId("admin-nav-ADM-MER-003")).toHaveCount(0);
  });

});
