import React from "react";
import { ConfigProvider, theme } from "antd";
import zhCN from "antd/es/locale/zh_CN";
import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import "antd/dist/reset.css";
import { AdminApi } from "./client/adminClient";
import { accountFormHint, accountFormOf, adminRoutes, findAdminRoute, groupedAdminRoutes, routeAllowsAccountForm } from "./adminRoutes";
import { stripBase, withBase } from "../basePath";
import { PermissionGate, hasPermissions } from "./security/PermissionGate";
import { hiddenGroupKeys, type DisabledSwitchRow } from "./security/terminalGate";
import type { AdminPageProps, AdminRoute, AdminUser, EffectivePermissions, EffectivePermissionsState } from "./types";

// antd 组件（日期选择器等）中文化；dayjs 同步切中文（周一开头/中文月份）。
dayjs.locale("zh-cn");

// P2 bundle 优化：admin 页面路由级 lazy 分包。
// 此前 32 个页面组件 eager 导入，全部塞进 AdminApp chunk（~224kB）。
// 改为 React.lazy 后每个页面成为独立按需 chunk，仅在路由命中时加载。
// 命名导出页面通过 .then 映射为 default，复用 main.tsx 三壳的 lazy 写法。
const OverviewPage = React.lazy(() => import("./pages/OverviewPage").then((m) => ({ default: m.OverviewPage })));
const AccessUsersPage = React.lazy(() => import("./pages/AccessUsersPage").then((m) => ({ default: m.AccessUsersPage })));
const PermissionTemplatesPage = React.lazy(() => import("./pages/PermissionTemplatesPage").then((m) => ({ default: m.PermissionTemplatesPage })));
const AccessGovernancePage = React.lazy(() => import("./pages/AccessGovernancePage").then((m) => ({ default: m.AccessGovernancePage })));
const TicketingPoliciesPage = React.lazy(() => import("./pages/TicketingPoliciesPage").then((m) => ({ default: m.TicketingPoliciesPage })));
const DynamicPricingPage = React.lazy(() => import("./pages/DynamicPricingPage").then((m) => ({ default: m.DynamicPricingPage })));
const TicketOrdersPage = React.lazy(() => import("./pages/TicketOrdersPage").then((m) => ({ default: m.TicketOrdersPage })));
const BarTableSessionsPage = React.lazy(() => import("./pages/BarTableSessionsPage").then((m) => ({ default: m.BarTableSessionsPage })));
const FinanceArApLedgerPage = React.lazy(() => import("./pages/FinanceArApLedgerPage").then((m) => ({ default: m.FinanceArApLedgerPage })));
const FinancePropertyFeePage = React.lazy(() => import("./pages/FinancePropertyFeePage").then((m) => ({ default: m.FinancePropertyFeePage })));
const FinanceMerchantExpensePage = React.lazy(() => import("./pages/FinanceMerchantExpensePage").then((m) => ({ default: m.FinanceMerchantExpensePage })));
const FinanceDividendsPage = React.lazy(() => import("./pages/FinanceDividendsPage").then((m) => ({ default: m.FinanceDividendsPage })));
const MerchantZoneDividendsPage = React.lazy(() => import("./pages/MerchantZoneDividendsPage").then((m) => ({ default: m.MerchantZoneDividendsPage })));
const VoucherLedgerPage = React.lazy(() => import("./pages/VoucherLedgerPage").then((m) => ({ default: m.VoucherLedgerPage })));
const SettlementReviewPage = React.lazy(() => import("./pages/SettlementReviewPage").then((m) => ({ default: m.SettlementReviewPage })));
const ReconciliationPage = React.lazy(() => import("./pages/ReconciliationPage").then((m) => ({ default: m.ReconciliationPage })));
const MerchantWorkOrdersPage = React.lazy(() => import("./pages/MerchantWorkOrdersPage").then((m) => ({ default: m.MerchantWorkOrdersPage })));
const MerchantScopePage = React.lazy(() => import("./pages/MerchantScopePage").then((m) => ({ default: m.MerchantScopePage })));
const MerchantZoneBillsPage = React.lazy(() => import("./pages/MerchantZoneBillsPage").then((m) => ({ default: m.MerchantZoneBillsPage })));
const MerchantZoneWorkOrdersPage = React.lazy(() => import("./pages/MerchantZoneWorkOrdersPage").then((m) => ({ default: m.MerchantZoneWorkOrdersPage })));
const MerchantZoneSettlementsPage = React.lazy(() => import("./pages/MerchantZoneSettlementsPage").then((m) => ({ default: m.MerchantZoneSettlementsPage })));
const MerchantZoneContractsPage = React.lazy(() => import("./pages/MerchantZoneContractsPage").then((m) => ({ default: m.MerchantZoneContractsPage })));
const MerchantZoneFeeRecipientsPage = React.lazy(() => import("./pages/MerchantZoneFeeRecipientsPage").then((m) => ({ default: m.MerchantZoneFeeRecipientsPage })));
const ReceivablesAnalyticsPage = React.lazy(() => import("./pages/ReceivablesAnalyticsPage").then((m) => ({ default: m.ReceivablesAnalyticsPage })));
const OccupancyAnalyticsPage = React.lazy(() => import("./pages/OccupancyAnalyticsPage").then((m) => ({ default: m.OccupancyAnalyticsPage })));
const WorkOrderSlaPage = React.lazy(() => import("./pages/WorkOrderSlaPage").then((m) => ({ default: m.WorkOrderSlaPage })));
const EnergyAnalyticsPage = React.lazy(() => import("./pages/EnergyAnalyticsPage").then((m) => ({ default: m.EnergyAnalyticsPage })));
const MonthlyComparisonPage = React.lazy(() => import("./pages/MonthlyComparisonPage").then((m) => ({ default: m.MonthlyComparisonPage })));
const StaffShiftAttendancePage = React.lazy(() => import("./pages/StaffShiftAttendancePage").then((m) => ({ default: m.StaffShiftAttendancePage })));
const StaffPayrollLeavePage = React.lazy(() => import("./pages/StaffPayrollLeavePage").then((m) => ({ default: m.StaffPayrollLeavePage })));
const ServiceOrdersPage = React.lazy(() => import("./pages/ServiceOrdersPage").then((m) => ({ default: m.ServiceOrdersPage })));
const VendorWorkOrdersPage = React.lazy(() => import("./pages/VendorWorkOrdersPage").then((m) => ({ default: m.VendorWorkOrdersPage })));
const VendorInspectionsPage = React.lazy(() => import("./pages/VendorInspectionsPage").then((m) => ({ default: m.VendorInspectionsPage })));
const VendorSiteEntriesPage = React.lazy(() => import("./pages/VendorSiteEntriesPage").then((m) => ({ default: m.VendorSiteEntriesPage })));
const AmusementSafetyPage = React.lazy(() => import("./pages/AmusementSafetyPage").then((m) => ({ default: m.AmusementSafetyPage })));
const AmusementInspectionTasksPage = React.lazy(() => import("./pages/AmusementInspectionTasksPage").then((m) => ({ default: m.AmusementInspectionTasksPage })));
const AmusementSessionsPage = React.lazy(() => import("./pages/AmusementSessionsPage").then((m) => ({ default: m.AmusementSessionsPage })));
const AmusementVerificationRecordsPage = React.lazy(() => import("./pages/AmusementVerificationRecordsPage").then((m) => ({ default: m.AmusementVerificationRecordsPage })));
const AmusementGroupBookingsPage = React.lazy(() => import("./pages/AmusementGroupBookingsPage").then((m) => ({ default: m.AmusementGroupBookingsPage })));
const SupervisorReviewPage = React.lazy(() => import("./pages/SupervisorReviewPage").then((m) => ({ default: m.SupervisorReviewPage })));
const ContractWorkbenchPage = React.lazy(() => import("./pages/ContractWorkbenchPage").then((m) => ({ default: m.ContractWorkbenchPage })));
const LeasingWorkbenchPage = React.lazy(() => import("./pages/LeasingWorkbenchPage").then((m) => ({ default: m.LeasingWorkbenchPage })));
const FacilityWorkbenchPage = React.lazy(() => import("./pages/FacilityWorkbenchPage").then((m) => ({ default: m.FacilityWorkbenchPage })));
const EnergyWorkbenchPage = React.lazy(() => import("./pages/EnergyWorkbenchPage").then((m) => ({ default: m.EnergyWorkbenchPage })));
const GovernanceTopologyPage = React.lazy(() => import("./pages/GovernanceTopologyPage").then((m) => ({ default: m.GovernanceTopologyPage })));
const OrganizationMerchantPage = React.lazy(() => import("./pages/OrganizationMerchantPage").then((m) => ({ default: m.OrganizationMerchantPage })));
const SubjectBindingsPage = React.lazy(() => import("./pages/SubjectBindingsPage").then((m) => ({ default: m.SubjectBindingsPage })));
const AdmissionAuditsPage = React.lazy(() => import("./pages/AdmissionAuditsPage").then((m) => ({ default: m.AdmissionAuditsPage })));
const RiskAlertsPage = React.lazy(() => import("./pages/RiskAlertsPage").then((m) => ({ default: m.RiskAlertsPage })));
const IntegrationEventsPage = React.lazy(() => import("./pages/IntegrationEventsPage").then((m) => ({ default: m.IntegrationEventsPage })));
const AuditEventsPage = React.lazy(() => import("./pages/AuditEventsPage").then((m) => ({ default: m.AuditEventsPage })));
const BusinessAnalyticsPage = React.lazy(() => import("./pages/BusinessAnalyticsPage").then((m) => ({ default: m.BusinessAnalyticsPage })));
const GroupOverviewPage = React.lazy(() => import("./pages/GroupOverviewPage").then((m) => ({ default: m.GroupOverviewPage })));
const InventoryLedgerPage = React.lazy(() => import("./pages/InventoryLedgerPage").then((m) => ({ default: m.InventoryLedgerPage })));
const BarOrdersPage = React.lazy(() => import("./pages/BarOrdersPage").then((m) => ({ default: m.BarOrdersPage })));
const StoredWinePage = React.lazy(() => import("./pages/StoredWinePage").then((m) => ({ default: m.StoredWinePage })));
const BarCommissionsPage = React.lazy(() => import("./pages/BarCommissionsPage").then((m) => ({ default: m.BarCommissionsPage })));
const BarProductsPage = React.lazy(() => import("./pages/BarProductsPage").then((m) => ({ default: m.BarProductsPage })));
const BarCombosPage = React.lazy(() => import("./pages/BarCombosPage").then((m) => ({ default: m.BarCombosPage })));
const IntegrationAdaptersPage = React.lazy(() => import("./pages/IntegrationAdaptersPage").then((m) => ({ default: m.IntegrationAdaptersPage })));
const IntegrationSwitchesPage = React.lazy(() => import("./pages/IntegrationSwitchesPage").then((m) => ({ default: m.IntegrationSwitchesPage })));
const TerminalSwitchMatrixPage = React.lazy(() => import("./pages/TerminalSwitchMatrixPage").then((m) => ({ default: m.TerminalSwitchMatrixPage })));
const CustomerAccountsPage = React.lazy(() => import("./pages/CustomerAccountsPage").then((m) => ({ default: m.CustomerAccountsPage })));
const PointsRulesPage = React.lazy(() => import("./pages/PointsRulesPage").then((m) => ({ default: m.PointsRulesPage })));
const MemberLoyaltyConfigPage = React.lazy(() => import("./pages/MemberLoyaltyConfigPage").then((m) => ({ default: m.MemberLoyaltyConfigPage })));
const StoredValueRulesPage = React.lazy(() => import("./pages/StoredValueRulesPage").then((m) => ({ default: m.StoredValueRulesPage })));
const RefundAuthorityPage = React.lazy(() => import("./pages/RefundAuthorityPage").then((m) => ({ default: m.RefundAuthorityPage })));
const OperatingParamsPage = React.lazy(() => import("./pages/OperatingParamsPage").then((m) => ({ default: m.OperatingParamsPage })));
const PayrollTaxBracketsPage = React.lazy(() => import("./pages/PayrollTaxBracketsPage").then((m) => ({ default: m.PayrollTaxBracketsPage })));
const AiThresholdsPage = React.lazy(() => import("./pages/AiThresholdsPage").then((m) => ({ default: m.AiThresholdsPage })));
const NotificationWindowsPage = React.lazy(() => import("./pages/NotificationWindowsPage").then((m) => ({ default: m.NotificationWindowsPage })));
const SpaceTypesPage = React.lazy(() => import("./pages/SpaceTypesPage").then((m) => ({ default: m.SpaceTypesPage })));
const VendorSubordinatesPage = React.lazy(() => import("./pages/VendorSubordinatesPage").then((m) => ({ default: m.VendorSubordinatesPage })));
const SmsNotificationSwitchesPage = React.lazy(() => import("./pages/SmsNotificationSwitchesPage").then((m) => ({ default: m.SmsNotificationSwitchesPage })));
const LeasingRentalStatusPage = React.lazy(() => import("./pages/LeasingRentalStatusPage").then((m) => ({ default: m.LeasingRentalStatusPage })));
const OpsReadinessPage = React.lazy(() => import("./pages/OpsReadinessPage").then((m) => ({ default: m.OpsReadinessPage })));
const DataCleanupPage = React.lazy(() => import("./pages/DataCleanupPage").then((m) => ({ default: m.DataCleanupPage })));
const FloorPlanImportPage = React.lazy(() => import("./pages/FloorPlanImportPage").then((m) => ({ default: m.FloorPlanImportPage })));
const T3AReadonlyAnalyticsPage = React.lazy(() => import("./pages/T3AReadonlyAnalyticsPage").then((m) => ({ default: m.T3AReadonlyAnalyticsPage })));
const ComplianceRemindersPage = React.lazy(() => import("./pages/ComplianceRemindersPage").then((m) => ({ default: m.ComplianceRemindersPage })));
const WecomComplianceReviewPage = React.lazy(() => import("./pages/WecomComplianceReviewPage").then((m) => ({ default: m.WecomComplianceReviewPage })));
const MetricDetailPage = React.lazy(() => import("./pages/MetricDetailPage").then((m) => ({ default: m.MetricDetailPage })));

// 侧边栏分组图标（移植自 UI 原型，纯展示，不影响导航行为/测试 testid）。
const ICON_PATHS: Record<string, string> = {
  grid: "M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z",
  shield: "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z",
  ticket: "M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2 2 2 0 0 0 0 4 2 2 0 0 1-2 2H5a2 2 0 0 1-2-2 2 2 0 0 0 0-4M14 6v12",
  cup: "M6 3h12l-1 7a5 5 0 0 1-10 0zM4 21h16M9 14v7",
  ferris: "M12 12V3m0 9 6.5 4M12 12l-6.5 4M12 2a10 10 0 0 0-7 17h14a10 10 0 0 0-7-17M9 21l3-4 3 4",
  link: "M9 15l6-6M10 6l1-1a4 4 0 0 1 6 6l-1 1M14 18l-1 1a4 4 0 0 1-6-6l1-1",
  alert: "M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6zM12 9v4M12 16h.01",
  coin: "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20M9 9a3 2 0 0 1 6 0M9 14a3 2 0 0 0 6 0M12 7v10",
  chart: "M4 20V10M10 20V4M16 20v-6M22 20H2",
  box: "M3 7l9-4 9 4-9 4zM3 7v10l9 4 9-4V7M12 11v10",
  user: "M4 21a8 8 0 0 1 16 0M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
  doc: "M6 2h8l4 4v16H6zM14 2v6h6",
  gear: "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8M12 2v2.5M12 19.5V22M4.2 7l2.2 1.2M17.6 15.8 19.8 17M4.2 17l2.2-1.2M17.6 8.2 19.8 7",
};

const GROUP_ICONS: Record<string, string> = {
  overview: "grid",
  merchant: "coin",
  ticketing: "ticket",
  bar: "cup",
  inventory: "box",
  amusement: "ferris",
  finance: "coin",
  vendor: "user",
  governance: "shield",
  integration: "link",
  access: "user",
  risk: "alert",
  audit: "doc",
  ops: "gear",
};

function NavIcon({ name }: { name: string }) {
  const d = ICON_PATHS[name] ?? ICON_PATHS.grid;
  return (
    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.6} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={d} />
    </svg>
  );
}

type LoginResponse = {
  accessToken: string;
  refreshToken?: string;
  sessionId?: string;
  tokenType?: string;
  user: AdminUser;
};

// 会话持久化（2026-07-19 用户拍板：令牌迁 HttpOnly Cookie）。令牌不再落 localStorage——JS 读不到，XSS 偷不走。
// localStorage 仅存展示用身份（user）+ 登出用 sessionId（均非凭证；真正的鉴权靠浏览器自动携带的 HttpOnly Cookie）。
// F5 不掉登录：Cookie 为会话 Cookie，刷新保留；令牌失效时请求层 401→Cookie 刷新兜底。
const SESSION_STORAGE_KEY = "admin.session.v1";

type StoredSession = {
  user: AdminUser;
  sessionId: string | null;
};

function loadStoredSession(): StoredSession | null {
  try {
    const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StoredSession;
    if (!parsed || !parsed.user) return null;
    return { user: parsed.user, sessionId: parsed.sessionId ?? null };
  } catch {
    return null;
  }
}

function saveStoredSession(session: StoredSession | null) {
  try {
    if (session) window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    else window.localStorage.removeItem(SESSION_STORAGE_KEY);
  } catch {
    // 隐私模式等存储不可用时静默降级为内存会话。
  }
}

function activeRouteFromLocation() {
  return findAdminRoute(stripBase(window.location.pathname));
}

function renderPage(route: AdminRoute, props: AdminPageProps) {
  switch (route.pageId) {
    case "ADM-OV-001":
      return <OverviewPage {...props} />;
    case "ADM-ACC-001":
      return <AccessUsersPage {...props} />;
    case "ADM-ACC-002":
      return <PermissionTemplatesPage {...props} />;
    case "ADM-ACC-003":
      return <AccessGovernancePage {...props} />;
    case "ADM-TKT-001":
      return <TicketingPoliciesPage {...props} />;
    case "ADM-TKT-002":
      return <DynamicPricingPage {...props} />;
    case "ADM-TKT-003":
      return <TicketOrdersPage {...props} />;
    case "ADM-BAR-001":
      return <BarTableSessionsPage {...props} />;
    case "ADM-BAR-002":
      return <BarOrdersPage {...props} />;
    case "ADM-BAR-003":
      return <StoredWinePage {...props} />;
    case "ADM-BAR-004":
      return <BarCommissionsPage {...props} />;
    case "ADM-BAR-005":
      return <BarProductsPage {...props} />;
    case "ADM-BAR-006":
      return <BarCombosPage {...props} />;
    case "ADM-INV-001":
      return <InventoryLedgerPage {...props} />;
    case "ADM-FIN-002":
      return <ReconciliationPage {...props} />;
    case "ADM-TER-001":
      return <TerminalSwitchMatrixPage {...props} />;
    case "ADM-CUS-001":
      return <CustomerAccountsPage {...props} />;
    case "ADM-FIN-003":
      return <RefundAuthorityPage {...props} />;
    case "ADM-MER-001":
      return <MerchantWorkOrdersPage {...props} />;
    case "ADM-MER-002":
      return <MerchantScopePage {...props} />;
    case "ADM-MER-003":
      return <MerchantZoneBillsPage {...props} />;
    case "ADM-MER-004":
      return <MerchantZoneWorkOrdersPage {...props} />;
    case "ADM-MER-005":
      return <MerchantZoneSettlementsPage {...props} />;
    case "ADM-MER-006":
      return <MerchantZoneContractsPage {...props} />;
    case "ADM-MER-007":
      return <MerchantZoneFeeRecipientsPage {...props} />;
    case "ADM-ANA-004":
      return <ReceivablesAnalyticsPage {...props} />;
    case "ADM-ANA-005":
      return <OccupancyAnalyticsPage {...props} />;
    case "ADM-ANA-006":
      return <WorkOrderSlaPage {...props} />;
    case "ADM-ANA-007":
      return <EnergyAnalyticsPage {...props} />;
    case "ADM-ANA-008":
      return <MonthlyComparisonPage {...props} />;
    case "ADM-STF-001":
      return <StaffShiftAttendancePage {...props} />;
    case "ADM-STF-002":
      return <StaffPayrollLeavePage {...props} />;
    case "ADM-SVC-001":
      return <ServiceOrdersPage {...props} />;
    case "ADM-VND-001":
      return <VendorWorkOrdersPage {...props} />;
    case "ADM-VND-002":
      return <VendorInspectionsPage {...props} />;
    case "ADM-VND-003":
      return <VendorSiteEntriesPage {...props} />;
    case "ADM-AMU-001":
      return <AmusementSessionsPage {...props} />;
    case "ADM-AMU-002":
      return <AmusementGroupBookingsPage {...props} />;
    case "ADM-AMU-003":
      return <AmusementSafetyPage {...props} />;
    case "ADM-AMU-004":
      return <AmusementInspectionTasksPage {...props} />;
    case "ADM-AMU-005":
      return <AmusementVerificationRecordsPage {...props} />;
    case "ADM-GOV-006":
      return <SupervisorReviewPage {...props} />;
    case "ADM-GOV-003":
      return <SubjectBindingsPage {...props} />;
    case "ADM-CONT-001":
      return <ContractWorkbenchPage {...props} />;
    case "ADM-LEASE-001":
      return <LeasingWorkbenchPage {...props} />;
    case "ADM-FAC-001":
      return <FacilityWorkbenchPage {...props} />;
    case "ADM-ENE-001":
      return <EnergyWorkbenchPage {...props} />;
    case "ADM-GOV-005":
      return <AdmissionAuditsPage {...props} />;
    case "ADM-RISK-001":
      return <RiskAlertsPage {...props} />;
    case "ADM-INT-002":
      return <IntegrationEventsPage {...props} />;
    case "ADM-EVD-001":
      return <AuditEventsPage {...props} />;
    case "ADM-ANA-001":
      return <BusinessAnalyticsPage {...props} />;
    case "ADM-ANA-002":
      return <GroupOverviewPage {...props} />;
    case "ADM-ANA-003":
      return <OperatingParamsPage {...props} />;
    case "ADM-STF-003":
      return <PayrollTaxBracketsPage {...props} />;
    case "ADM-ANA-010":
      return <AiThresholdsPage {...props} />;
    case "ADM-ANA-011":
      return <NotificationWindowsPage {...props} />;
    case "ADM-ANA-012":
      return <MetricDetailPage {...props} />;
    case "ADM-GOV-007":
      return <SpaceTypesPage {...props} />;
    case "ADM-VND-004":
      return <VendorSubordinatesPage {...props} />;
    case "ADM-INT-004":
      return <SmsNotificationSwitchesPage {...props} />;
    case "ADM-LEASE-002":
      return <LeasingRentalStatusPage {...props} />;
    case "ADM-FIN-001":
      return <FinanceArApLedgerPage {...props} />;
    case "ADM-FIN-004":
      return <FinancePropertyFeePage {...props} />;
    case "ADM-FIN-006":
      return <FinanceMerchantExpensePage {...props} />;
    case "ADM-FIN-007":
      return <FinanceDividendsPage {...props} />;
    case "ADM-MER-008":
      return <MerchantZoneDividendsPage {...props} />;
    case "ADM-CUS-002":
      return <VoucherLedgerPage {...props} />;
    case "ADM-CUS-003":
      return <PointsRulesPage {...props} />;
    case "ADM-CUS-004":
      return <MemberLoyaltyConfigPage {...props} />;
    case "ADM-CUS-005":
      return <StoredValueRulesPage {...props} />;
    case "ADM-FIN-005":
      return <SettlementReviewPage {...props} />;
    case "ADM-INT-001":
      return <IntegrationAdaptersPage {...props} />;
    case "ADM-INT-003":
      return <IntegrationSwitchesPage {...props} />;
    case "ADM-OPS-001":
      return <OpsReadinessPage {...props} />;
    case "ADM-OPS-002":
      return <DataCleanupPage {...props} />;
    case "ADM-GOV-001":
      return <GovernanceTopologyPage {...props} />;
    case "ADM-GOV-002":
      return <OrganizationMerchantPage {...props} />;
    case "ADM-FLR-001":
      return <FloorPlanImportPage {...props} />;
    case "ADM-ANA-009":
      return <T3AReadonlyAnalyticsPage {...props} />;
    case "ADM-COMP-001":
      return <ComplianceRemindersPage {...props} />;
    case "ADM-COMP-002":
      return <WecomComplianceReviewPage {...props} />;
    default:
      // findAdminRoute 兜底到 adminRoutes[0]，正常导航不会落到这里；保留未知页面占位仅为类型完备与极端防御。
      return <div className="admin-state admin-error">未知页面：{route.pageId}</div>;
  }
}

export default function AdminApp() {
  const [route, setRoute] = React.useState<AdminRoute>(activeRouteFromLocation);
  const [focusObjectId, setFocusObjectId] = React.useState<string | undefined>(
    () => new URLSearchParams(window.location.search).get("focus") ?? undefined,
  );
  // 启动时从 localStorage 恢复展示身份（F5 不掉登录）；鉴权靠 HttpOnly Cookie，令牌失效时请求层 401→Cookie 刷新兜底。
  const storedSession = React.useRef(loadStoredSession()).current;
  const [user, setUser] = React.useState<AdminUser | null>(storedSession?.user ?? null);
  const [sessionId, setSessionId] = React.useState<string | null>(storedSession?.sessionId ?? null);
  const [effectivePermissions, setEffectivePermissions] = React.useState<EffectivePermissionsState>({ status: "loading" });
  const [username, setUsername] = React.useState("admin");
  const [password, setPassword] = React.useState("");
  const [loading, setLoading] = React.useState(false);
  const [globalError, setGlobalError] = React.useState<string | null>(null);
  const [actionResult, setActionResult] = React.useState<unknown>(null);
  // 深色模式（2026-07-07）：localStorage 优先，无则跟随系统偏好；data-theme 打在 <html> 上（覆盖 antd portal）。
  const [themeMode, setThemeMode] = React.useState<"light" | "dark">(() => {
    const saved = typeof window !== "undefined" ? window.localStorage.getItem("adm-theme") : null;
    if (saved === "dark" || saved === "light") return saved;
    return window.matchMedia?.("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  });
  React.useEffect(() => {
    document.documentElement.setAttribute("data-theme", themeMode);
    window.localStorage.setItem("adm-theme", themeMode);
  }, [themeMode]);

  const api = React.useMemo(() => {
    const instance = new AdminApi();
    // 商户专区(3.18 方案A):merchant 归属账号整个后台会话按 merchant_portal 终端上报(V197 总开关+V199 模块开关约束)。
    instance.terminalType = user?.merchantId ? "merchant_portal" : "web";
    // 会话续期：401 → /auth/refresh（Cookie 自动携带刷新令牌）→ 重放原请求一次；刷新失败清会话回登录视图。
    instance.onSessionExpired = () => {
      saveStoredSession(null);
      setUser(null);
      setSessionId(null);
      setEffectivePermissions({ status: "loading" });
      setGlobalError("登录已过期，请重新登录");
    };
    return instance;
  }, [user]);

  React.useEffect(() => {
    const onPopState = () => {
      setRoute(activeRouteFromLocation());
      setFocusObjectId(new URLSearchParams(window.location.search).get("focus") ?? undefined);
    };
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  React.useEffect(() => {
    if (!user) {
      setEffectivePermissions({ status: "loading" });
      return;
    }
    let cancelled = false;
    setEffectivePermissions({ status: "loading" });
    api
      .get<EffectivePermissions>("/api/v1/access/effective-permissions")
      .then((value) => {
        if (cancelled) return;
        setEffectivePermissions({ status: "ready", value });
      })
      .catch(() => {
        if (cancelled) return;
        setEffectivePermissions({ status: "error" });
      });
    return () => {
      cancelled = true;
    };
  }, [api, user]);

  // 3.19 二期：终端开关菜单 gating——[]=无关闭项/加载中(不闪隐藏)，null=拉取失败→兜底关(留自救组)。
  const [disabledSwitchRows, setDisabledSwitchRows] = React.useState<DisabledSwitchRow[] | null>([]);

  React.useEffect(() => {
    if (!user) return;
    let cancelled = false;
    api
      .get<DisabledSwitchRow[]>("/api/v1/terminals/capability-switches/effective")
      .then((rows) => {
        if (!cancelled) setDisabledSwitchRows(rows ?? []);
      })
      .catch(() => {
        if (!cancelled) setDisabledSwitchRows(null);
      });
    return () => {
      cancelled = true;
    };
  }, [api, user]);

  // 3.19 二期：当前菜单模块随路由更新，请求头 X-Module-Code 让后端按模块闸。
  React.useEffect(() => {
    api.moduleCode = route?.group ?? null;
  }, [api, route]);

  const hiddenGroups = React.useMemo(
    () => hiddenGroupKeys(disabledSwitchRows, groupedAdminRoutes().map((group) => group.key)),
    [disabledSwitchRows],
  );

  const [collapsedGroups, setCollapsedGroups] = React.useState<Record<string, boolean>>({});

  // 移动端（≤860px）侧栏抽屉：默认收起，汉堡按钮开合；选中菜单项/点遮罩/Esc 关闭。
  // 桌面端侧栏常驻，此状态无效果（CSS 媒体查询控制）。
  const [navOpen, setNavOpen] = React.useState(false);

  React.useEffect(() => {
    if (!navOpen) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setNavOpen(false);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [navOpen]);

  function toggleGroup(key: string) {
    setCollapsedGroups((current) => ({ ...current, [key]: !current[key] }));
  }

  // 菜单按账号形态隐藏（用户裁决）：外包/商户专用页对不匹配形态不渲染、不导航。
  const accountForm = accountFormOf(user);

  function navigate(nextRoute: AdminRoute) {
    // onClick 级拦截：无权限项不导航（不止 aria-disabled）。
    if (!hasPermissions(effectivePermissions, nextRoute.requiredPermissions)) {
      return;
    }
    if (!routeAllowsAccountForm(nextRoute, accountForm)) {
      return;
    }
    // 3.19 二期：被终端开关关闭的模块不导航（菜单已隐藏，此处兜底直链/历史记录）。
    if (nextRoute.group && hiddenGroups.has(nextRoute.group)) {
      return;
    }
    setRoute(nextRoute);
    setFocusObjectId(undefined);
    setActionResult(null);
    setGlobalError(null);
    setNavOpen(false);
    // 固定高度布局下内容滚动发生在 .admin-content 内；切换页面后复位到顶部，
    // 避免从长列表页跳到短页面时停留在空白滚动位置。
    const content = document.querySelector<HTMLElement>(".admin-content");
    content?.scrollTo({ top: 0 });
    // SPA 换页后把焦点移到主内容区（tabIndex=-1），读屏/键盘用户不再滞留在旧菜单项。
    content?.focus();
    if (stripBase(window.location.pathname) !== nextRoute.route) {
      window.history.pushState({ pageId: nextRoute.pageId }, "", withBase(nextRoute.route));
    }
  }

  // 全链下钻：按 pageId 找目标页，校验权限后带对象 id 跳转并定位（URL ?focus=）。
  function navigateTo(pageId: string, objectId?: string) {
    const target = adminRoutes.find((item) => item.pageId === pageId);
    if (!target || !hasPermissions(effectivePermissions, target.requiredPermissions)) {
      return;
    }
    if (!routeAllowsAccountForm(target, accountForm)) {
      return;
    }
    setFocusObjectId(objectId);
    setRoute(target);
    setActionResult(null);
    setGlobalError(null);
    setNavOpen(false);
    const content = document.querySelector<HTMLElement>(".admin-content");
    content?.scrollTo({ top: 0 });
    content?.focus();
    const url = objectId ? `${target.route}?focus=${encodeURIComponent(objectId)}` : target.route;
    window.history.pushState({ pageId: target.pageId }, "", withBase(url));
  }

  async function login(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setGlobalError(null);
    try {
      const loginApi = new AdminApi();
      // 令牌由服务端以 HttpOnly Cookie 下发（响应体令牌前端不再读取/落盘）；仅取展示身份 + 登出用 sessionId。
      const response = await loginApi.post<LoginResponse>("/api/v1/auth/login", { login_name: username, password });
      setUser(response.user);
      setSessionId(response.sessionId ?? null);
      saveStoredSession({ user: response.user, sessionId: response.sessionId ?? null });
    } catch (loginError) {
      setGlobalError(loginError instanceof Error ? loginError.message : "登录失败");
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    // 令牌在 HttpOnly Cookie 里，JS 清不掉——必须调后端登出以 Set-Cookie 过期 Cookie 并吊销会话（尽力而为，失败也清本地态）。
    if (sessionId) void api.post("/api/v1/auth/logout", { session_id: sessionId }).catch(() => undefined);
    saveStoredSession(null);
    setUser(null);
    setSessionId(null);
    setEffectivePermissions({ status: "loading" });
    setActionResult(null);
    setGlobalError(null);
  }

  const shell = !user ? (
    <main className="admin-auth-shell" data-testid="auth-shell">
      <aside className="admin-auth-art">
        <div className="admin-auth-art-inner">
          <span className="admin-auth-mark">星</span>
          <h1 className="admin-auth-art-title">星海综合体</h1>
          <p className="admin-auth-art-sub">统一运营管理平台</p>
        </div>
        <div className="admin-auth-art-foot mono">治理 · 票务 · 酒吧 · 游乐 · 财务</div>
      </aside>
      <div className="admin-auth-form-wrap">
        <form className="admin-auth-panel" data-testid="login-view" onSubmit={(event) => void login(event)}>
          <strong role="heading" aria-level={1}>综合体完整后台</strong>
          <p className="admin-auth-desc">登录以进入对应权限与数据范围</p>
          {globalError ? <div className="admin-inline-error" role="alert" aria-live="assertive" data-testid="global-error">{globalError}</div> : null}
          <label><span>账号</span><input data-testid="login-username" value={username} onChange={(event) => setUsername(event.target.value)} /></label>
          <label><span>密码</span><input data-testid="login-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          <button type="submit" data-testid="login-submit" disabled={loading}>{loading ? "登录中" : "登录"}</button>
        </form>
      </div>
    </main>
  ) : (
    <div className="admin-shell" data-testid="admin-shell">
      <a className="skip-link" href="#main-content">跳到主内容</a>
      {navOpen ? (
        <div
          className="admin-nav-backdrop"
          data-testid="admin-nav-backdrop"
          aria-hidden="true"
          onClick={() => setNavOpen(false)}
        />
      ) : null}
      <aside className={navOpen ? "admin-sidebar is-open" : "admin-sidebar"}>
        <div className="admin-brand">
          <span className="admin-brand-mark">星</span>
          <div className="admin-brand-text">
            <div className="admin-brand-name">综合体完整后台</div>
            <div className="admin-brand-sub">统一运营管理平台</div>
          </div>
        </div>
        <nav data-testid="admin-nav">
          {groupedAdminRoutes()
            .map((group) => ({ ...group, routes: group.routes.filter((item) => routeAllowsAccountForm(item, accountForm)) }))
            .filter((group) => !hiddenGroups.has(group.key) && group.routes.length > 0)
            .map((group) => {
            const collapsed = collapsedGroups[group.key] ?? false;
            return (
              <div className="admin-nav-group" key={group.key} data-testid={`admin-nav-group-${group.key}`}>
                <button
                  type="button"
                  className="admin-nav-group-header"
                  data-testid={`admin-nav-group-toggle-${group.key}`}
                  aria-expanded={!collapsed}
                  onClick={() => toggleGroup(group.key)}
                >
                  <span className="admin-nav-ic"><NavIcon name={GROUP_ICONS[group.key] ?? "grid"} /></span>
                  <span>{group.label}</span>
                  <small>{collapsed ? "▸" : "▾"}</small>
                </button>
                {collapsed ? null : (
                  <div className="admin-nav-group-items">
                    {group.routes.map((item) => {
                      const allowed = hasPermissions(effectivePermissions, item.requiredPermissions);
                      return (
                        <button
                          type="button"
                          key={item.pageId}
                          data-testid={`admin-nav-${item.pageId}`}
                          className={item.pageId === route.pageId ? "active" : ""}
                          aria-current={item.pageId === route.pageId ? "page" : undefined}
                          onClick={() => navigate(item)}
                          disabled={!allowed}
                          aria-disabled={!allowed}
                          title={!allowed ? "当前账号无权访问该页面" : undefined}
                        >
                          <span>{item.module}</span>
                          <strong>{item.label}</strong>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </nav>
        <div className="admin-sidebar-foot">
          <span className="admin-avatar">{(user.username ?? "U").slice(0, 1).toUpperCase()}</span>
          <div>
            <div className="admin-sf-name">{user.username}</div>
            <div className="admin-sf-role">完整后台 · 在岗</div>
          </div>
        </div>
      </aside>
      <section className="admin-main">
        <header className="admin-topbar">
          <button
            type="button"
            className="admin-nav-toggle"
            data-testid="admin-nav-toggle"
            aria-label={navOpen ? "关闭导航" : "打开导航"}
            aria-expanded={navOpen}
            onClick={() => setNavOpen((open) => !open)}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" aria-hidden="true">
              <path d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <div><strong>{route.label}</strong><span>{route.module}</span></div>
          <div className="admin-topbar-actions">
            <span className="admin-topbar-store">营业中 · 综合体</span>
            <span className="admin-user-chip">{user.username}</span>
            <button
              type="button"
              className="admin-theme-toggle"
              data-testid="admin-theme-toggle"
              aria-label={themeMode === "dark" ? "切换到浅色模式" : "切换到深色模式"}
              title={themeMode === "dark" ? "切换到浅色模式" : "切换到深色模式"}
              onClick={() => setThemeMode((m) => (m === "dark" ? "light" : "dark"))}
            >
              {themeMode === "dark" ? "☀" : "🌙"}
            </button>
            <button type="button" data-testid="admin-logout" onClick={logout}>退出</button>
          </div>
        </header>
        <main className="admin-content" id="main-content" tabIndex={-1}>
          {globalError ? <div className="admin-inline-error" role="alert" aria-live="assertive" data-testid="global-error">{globalError}</div> : null}
          {!routeAllowsAccountForm(route, accountForm) ? (
            // 形态不匹配（直链/历史进入）：给业务化提示，不渲染页面、不放请求打 403/400。
            <div className="admin-denied" data-testid="admin-account-form-mismatch">{accountFormHint(route)}</div>
          ) : (
          <PermissionGate permissionsState={effectivePermissions} permissions={route.requiredPermissions}>
            {/* lazy 页面用 Suspense 包裹；fallback 故意为 null（轻量占位，
                不带 .loading-state class），避免触发 e2e 的常驻 loading-state 断言。
                chunk 加载完成后页面 testid 会正常出现，Playwright 自动等待。 */}
            <React.Suspense fallback={<div className="admin-page-loading" role="status" aria-live="polite">页面加载中…</div>}>
              {renderPage(route, { api, user, setActionResult, setGlobalError, effectivePermissions, navigateTo, focusObjectId })}
            </React.Suspense>
          </PermissionGate>
          )}
          {actionResult ? <pre className="admin-result" role="status" aria-live="polite" data-testid="admin-action-result">{typeof actionResult === "string" ? actionResult : JSON.stringify(actionResult, null, 2)}</pre> : null}
        </main>
      </section>
    </div>
  );

  return (
    <ConfigProvider locale={zhCN} theme={{ algorithm: themeMode === "dark" ? theme.darkAlgorithm : theme.defaultAlgorithm, token: { colorPrimary: themeMode === "dark" ? "#d99a2b" : "#1b1d21", borderRadius: 10, fontFamily: "'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif" } }}>
      {shell}
    </ConfigProvider>
  );
}
