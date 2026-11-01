// 文件版本：V0.17
import React from "react";
import { ConfigProvider, theme } from "antd";
import zhCN from "antd/es/locale/zh_CN";
import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import "antd/dist/reset.css";
import { AdminApi, isMerchantAppMode, merchantAppStore } from "./client/adminClient";
import { SENSITIVE_PAGE_IDS, accountFormHint, accountFormOf, adminRoutes, findAdminRoute, groupedAdminRoutes, merchantAppExcluded, routeAllowsAccountForm } from "./adminRoutes";
import { stripBase, withBase } from "../basePath";
import { forgetWriteOperations } from "../shared/operationKey";
import { ReplayedNotice } from "../shared/ReplayedNotice";
import { ActionResultView } from "./components/ActionResultView";
import { MerchantUnreadBillsReminder } from "./components/MerchantUnreadBillsReminder";
import { PageWatermark } from "./components/PageWatermark";
import { PermissionGate, hasPermissions } from "./security/PermissionGate";
import { hiddenGroupKeys, type DisabledSwitchRow } from "./security/terminalGate";
import type { AdminPageProps, AdminRoute, AdminUser, EffectivePermissions, EffectivePermissionsState, ViewedRow } from "./types";

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
const MerchantZoneLicensesPage = React.lazy(() => import("./pages/MerchantZoneLicensesPage").then((m) => ({ default: m.MerchantZoneLicensesPage })));
const VoucherLedgerPage = React.lazy(() => import("./pages/VoucherLedgerPage").then((m) => ({ default: m.VoucherLedgerPage })));
const SettlementReviewPage = React.lazy(() => import("./pages/SettlementReviewPage").then((m) => ({ default: m.SettlementReviewPage })));
const SettlementConfigPage = React.lazy(() => import("./pages/SettlementConfigPage").then((m) => ({ default: m.SettlementConfigPage })));
const InternalPriceCatalogPage = React.lazy(() => import("./pages/InternalPriceCatalogPage").then((m) => ({ default: m.InternalPriceCatalogPage })));
const MediaPerformanceShowPage = React.lazy(() => import("./pages/MediaPerformanceShowPage").then((m) => ({ default: m.MediaPerformanceShowPage })));
const SettlementInvoicePage = React.lazy(() => import("./pages/SettlementInvoicePage").then((m) => ({ default: m.SettlementInvoicePage })));
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
const AbandonedEntitlementRevenuePage = React.lazy(() => import("./pages/AbandonedEntitlementRevenuePage").then((m) => ({ default: m.AbandonedEntitlementRevenuePage })));
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
const AmusementBusinessDayPage = React.lazy(() => import("./pages/AmusementBusinessDayPage").then((m) => ({ default: m.AmusementBusinessDayPage })));
const AmusementFaultCompensationPage = React.lazy(() => import("./pages/AmusementFaultCompensationPage").then((m) => ({ default: m.AmusementFaultCompensationPage })));
const AmusementParkWideFaultPage = React.lazy(() => import("./pages/AmusementParkWideFaultPage").then((m) => ({ default: m.AmusementParkWideFaultPage })));
const AmusementVerificationRecordsPage = React.lazy(() => import("./pages/AmusementVerificationRecordsPage").then((m) => ({ default: m.AmusementVerificationRecordsPage })));
const AmusementGroupBookingsPage = React.lazy(() => import("./pages/AmusementGroupBookingsPage").then((m) => ({ default: m.AmusementGroupBookingsPage })));
const SupervisorReviewPage = React.lazy(() => import("./pages/SupervisorReviewPage").then((m) => ({ default: m.SupervisorReviewPage })));
const ApprovalChainPage = React.lazy(() => import("./pages/ApprovalChainPage").then((m) => ({ default: m.ApprovalChainPage })));
const OrgHierarchyPage = React.lazy(() => import("./pages/OrgHierarchyPage").then((m) => ({ default: m.OrgHierarchyPage })));
const ContractWorkbenchPage = React.lazy(() => import("./pages/ContractWorkbenchPage").then((m) => ({ default: m.ContractWorkbenchPage })));
const LeasingWorkbenchPage = React.lazy(() => import("./pages/LeasingWorkbenchPage").then((m) => ({ default: m.LeasingWorkbenchPage })));
const PointsRecoveryWaiverPage = React.lazy(() => import("./pages/PointsRecoveryWaiverPage").then((m) => ({ default: m.PointsRecoveryWaiverPage })));
const TicketRewardConfigPage = React.lazy(() => import("./pages/TicketRewardConfigPage").then((m) => ({ default: m.TicketRewardConfigPage })));
const TicketPointsBillingPage = React.lazy(() => import("./pages/TicketPointsBillingPage").then((m) => ({ default: m.TicketPointsBillingPage })));
const SiteEntryPropertyPage = React.lazy(() => import("./pages/SiteEntryPropertyPage").then((m) => ({ default: m.SiteEntryPropertyPage })));
const FacilityWorkbenchPage = React.lazy(() => import("./pages/FacilityWorkbenchPage").then((m) => ({ default: m.FacilityWorkbenchPage })));
const ParkingWorkbenchPage = React.lazy(() => import("./pages/ParkingWorkbenchPage").then((m) => ({ default: m.ParkingWorkbenchPage })));
const MerchantZoneStaffVehiclesPage = React.lazy(() => import("./pages/ParkingWorkbenchPage").then((m) => ({ default: m.MerchantZoneStaffVehiclesPage })));
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
const PurchaseWorkbenchPage = React.lazy(() => import("./pages/PurchaseWorkbenchPage").then((m) => ({ default: m.PurchaseWorkbenchPage })));
const InventoryLedgerPage = React.lazy(() => import("./pages/InventoryLedgerPage").then((m) => ({ default: m.InventoryLedgerPage })));
const InventoryTransferWorkbenchPage = React.lazy(() => import("./pages/InventoryTransferWorkbenchPage").then((m) => ({ default: m.InventoryTransferWorkbenchPage })));
const InventoryHighValueReviewPage = React.lazy(() => import("./pages/InventoryHighValueReviewPage").then((m) => ({ default: m.InventoryHighValueReviewPage })));
const TraceHandoverBackfillPage = React.lazy(() => import("./pages/TraceHandoverBackfillPage").then((m) => ({ default: m.TraceHandoverBackfillPage })));
const BarOrdersPage = React.lazy(() => import("./pages/BarOrdersPage").then((m) => ({ default: m.BarOrdersPage })));
const StoredWinePage = React.lazy(() => import("./pages/StoredWinePage").then((m) => ({ default: m.StoredWinePage })));
const BarCommissionsPage = React.lazy(() => import("./pages/BarCommissionsPage").then((m) => ({ default: m.BarCommissionsPage })));
const BarProductsPage = React.lazy(() => import("./pages/BarProductsPage").then((m) => ({ default: m.BarProductsPage })));
const BarCombosPage = React.lazy(() => import("./pages/BarCombosPage").then((m) => ({ default: m.BarCombosPage })));
const BarPayrollPage = React.lazy(() => import("./pages/BarPayrollPage").then((m) => ({ default: m.BarPayrollPage })));
const IntegrationAdaptersPage = React.lazy(() => import("./pages/IntegrationAdaptersPage").then((m) => ({ default: m.IntegrationAdaptersPage })));
const IntegrationSwitchesPage = React.lazy(() => import("./pages/IntegrationSwitchesPage").then((m) => ({ default: m.IntegrationSwitchesPage })));
const IntegrationChannelMapsPage = React.lazy(() => import("./pages/IntegrationChannelMapsPage").then((m) => ({ default: m.IntegrationChannelMapsPage })));
const TerminalSwitchMatrixPage = React.lazy(() => import("./pages/TerminalSwitchMatrixPage").then((m) => ({ default: m.TerminalSwitchMatrixPage })));
const TerminalDevicesPage = React.lazy(() => import("./pages/TerminalDevicesPage").then((m) => ({ default: m.TerminalDevicesPage })));
const MdmWhitelistPage = React.lazy(() => import("./pages/MdmWhitelistPage").then((m) => ({ default: m.MdmWhitelistPage })));
const StudentCertificationsPage = React.lazy(() => import("./pages/StudentCertificationsPage").then((m) => ({ default: m.StudentCertificationsPage })));
const CustomerAccountsPage = React.lazy(() => import("./pages/CustomerAccountsPage").then((m) => ({ default: m.CustomerAccountsPage })));
const MemberLoyaltyConfigPage = React.lazy(() => import("./pages/MemberLoyaltyConfigPage").then((m) => ({ default: m.MemberLoyaltyConfigPage })));
const StoredValueRulesPage = React.lazy(() => import("./pages/StoredValueRulesPage").then((m) => ({ default: m.StoredValueRulesPage })));
const RefundAuthorityPage = React.lazy(() => import("./pages/RefundAuthorityPage").then((m) => ({ default: m.RefundAuthorityPage })));
const OperatingParamsPage = React.lazy(() => import("./pages/OperatingParamsPage").then((m) => ({ default: m.OperatingParamsPage })));
const PayrollTaxBracketsPage = React.lazy(() => import("./pages/PayrollTaxBracketsPage").then((m) => ({ default: m.PayrollTaxBracketsPage })));
const StaffProfilesPage = React.lazy(() => import("./pages/StaffProfilesPage").then((m) => ({ default: m.StaffProfilesPage })));
const AiThresholdsPage = React.lazy(() => import("./pages/AiThresholdsPage").then((m) => ({ default: m.AiThresholdsPage })));
const NotificationWindowsPage = React.lazy(() => import("./pages/NotificationWindowsPage").then((m) => ({ default: m.NotificationWindowsPage })));
const SpaceTypesPage = React.lazy(() => import("./pages/SpaceTypesPage").then((m) => ({ default: m.SpaceTypesPage })));
const VenueBusinessHoursPage = React.lazy(() => import("./pages/VenueBusinessHoursPage").then((m) => ({ default: m.VenueBusinessHoursPage })));
const MerchantPenaltyPage = React.lazy(() => import("./pages/MerchantPenaltyPage").then((m) => ({ default: m.MerchantPenaltyPage })));
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
const DealerAdmissionCatalogPage = React.lazy(() => import("./pages/DealerAdmissionCatalogPage").then((m) => ({ default: m.DealerAdmissionCatalogPage })));
const DealerOrderFulfillmentPage = React.lazy(() => import("./pages/DealerOrderFulfillmentPage").then((m) => ({ default: m.DealerOrderFulfillmentPage })));
const DealerReturnInvoicePage = React.lazy(() => import("./pages/DealerReturnInvoicePage").then((m) => ({ default: m.DealerReturnInvoicePage })));
const WatermarkTracePage = React.lazy(() => import("./pages/WatermarkTracePage").then((m) => ({ default: m.WatermarkTracePage })));
const MySecurityExportsPage = React.lazy(() => import("./pages/MySecurityExportsPage").then((m) => ({ default: m.MySecurityExportsPage })));

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
  settlement: "coin",
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
  /** 本次会话设备编号（商户手机应用，第十八批裁决 19）。 */
  deviceId?: string;
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
    case "ADM-FIN-009":
      return <PointsRecoveryWaiverPage {...props} />;
    case "ADM-TKT-004":
      return <TicketRewardConfigPage {...props} />;
    case "ADM-TKT-005":
      return <StudentCertificationsPage {...props} />;
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
    case "ADM-BAR-007":
      return <BarPayrollPage {...props} />;
    case "ADM-INV-001":
      return <InventoryLedgerPage {...props} />;
    case "ADM-INV-002":
      return <InventoryTransferWorkbenchPage {...props} />;
    case "ADM-INV-003":
      return <PurchaseWorkbenchPage {...props} />;
    case "ADM-INV-004":
      return <InventoryHighValueReviewPage {...props} />;
    case "ADM-INV-005":
      return <TraceHandoverBackfillPage {...props} />;
    case "ADM-FIN-002":
      return <ReconciliationPage {...props} />;
    case "ADM-TER-001":
      return <TerminalSwitchMatrixPage {...props} />;
    case "ADM-TER-002":
      return <TerminalDevicesPage {...props} />;
    case "ADM-TER-003":
      return <MdmWhitelistPage {...props} />;
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
    case "ADM-AMU-006":
      return <AmusementParkWideFaultPage {...props} />;
    case "ADM-AMU-007":
      return <AmusementFaultCompensationPage {...props} />;
    case "ADM-AMU-008":
      return <AmusementBusinessDayPage {...props} />;
    case "ADM-GOV-006":
      return <SupervisorReviewPage {...props} />;
    case "ADM-GOV-009":
      return <ApprovalChainPage {...props} />;
    case "ADM-ACC-004":
      return <OrgHierarchyPage {...props} />;
    case "ADM-GOV-003":
      return <SubjectBindingsPage {...props} />;
    case "ADM-CONT-001":
      return <ContractWorkbenchPage {...props} />;
    case "ADM-LEASE-001":
      return <LeasingWorkbenchPage {...props} />;
    case "ADM-FAC-001":
      return <FacilityWorkbenchPage {...props} />;
    case "ADM-PARK-001":
      return <ParkingWorkbenchPage {...props} />;
    case "ADM-MER-009":
      return <MerchantZoneStaffVehiclesPage {...props} />;
    case "ADM-FAC-002":
      return <SiteEntryPropertyPage {...props} stage="review" />;
    case "ADM-FAC-003":
      return <SiteEntryPropertyPage {...props} stage="accept" />;
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
    case "ADM-STF-004":
      return <StaffProfilesPage {...props} />;
    case "ADM-ANA-010":
      return <AiThresholdsPage {...props} />;
    case "ADM-ANA-011":
      return <NotificationWindowsPage {...props} />;
    case "ADM-ANA-012":
      return <MetricDetailPage {...props} />;
    case "ADM-ANA-013":
      return <AbandonedEntitlementRevenuePage {...props} />;
    case "ADM-GOV-007":
      return <SpaceTypesPage {...props} />;
    case "ADM-GOV-008":
      return <VenueBusinessHoursPage {...props} />;
    case "ADM-GOV-010":
      return <MerchantPenaltyPage {...props} />;
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
    case "ADM-FIN-008":
      return <TicketPointsBillingPage {...props} />;
    case "ADM-STL-001":
      return <SettlementConfigPage {...props} />;
    case "ADM-STL-002":
      return <SettlementInvoicePage {...props} />;
    case "ADM-STL-003":
      return <InternalPriceCatalogPage {...props} />;
    case "ADM-STL-004":
      return <MediaPerformanceShowPage {...props} />;
    case "ADM-MER-008":
      return <MerchantZoneDividendsPage {...props} />;
    case "ADM-MER-010":
      return <MerchantZoneLicensesPage {...props} />;
    case "ADM-CUS-002":
      return <VoucherLedgerPage {...props} />;
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
    case "ADM-INT-005":
      return <IntegrationChannelMapsPage {...props} />;
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
    case "ADM-DLR-001":
      return <DealerAdmissionCatalogPage {...props} />;
    case "ADM-DLR-002":
      return <DealerOrderFulfillmentPage {...props} />;
    case "ADM-DLR-003":
      return <DealerReturnInvoicePage {...props} />;
    case "ADM-SEC-001":
      return <MySecurityExportsPage {...props} />;
    case "ADM-SEC-002":
      return <WatermarkTracePage {...props} />;
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
  const [storedSession] = React.useState(() => {
    const stored = loadStoredSession();
    // M725-F02：恢复会话也记下是谁，同一个人会话失效后重新登录照常沿用没拿到结果的请求编号。
    if (stored) forgetWriteOperations(stored.user.id);
    return stored;
  });
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
    // 商户专区(3.18 方案A):merchant 归属账号整个后台会话按 merchant_portal 终端上报(V197 总开关+V199 模块开关约束)；商户手机应用里报 merchant_app（第 25 条 25-R3）。
    instance.terminalType = isMerchantAppMode() ? "merchant_app" : user?.merchantId ? "merchant_portal" : "web";
    // 会话续期：401 → /auth/refresh（Cookie 自动携带刷新令牌）→ 重放原请求一次；刷新失败清会话回登录视图。
    instance.onSessionExpired = () => {
      // 商户手机应用：只清令牌，设备编号保留（第十八批裁决 19）。
      void merchantAppStore()?.clearTokens().catch(() => undefined);
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

  // 第 25 条 25-R3：App 用令牌、不带浏览器 Cookie，附件原件链接（/api/v1/…）在 App 里打不开还会跳离当前页，拦下并提示；/admin/ 整页链接照常。
  React.useEffect(() => {
    if (!isMerchantAppMode()) return;
    const onClick = (event: MouseEvent) => {
      const link = event.target instanceof Element ? event.target.closest("a") : null;
      if (!link || !stripBase(link.pathname).startsWith("/api/v1/")) return;
      event.preventDefault();
      setGlobalError("附件原件请在商户网页门户查看");
    };
    document.addEventListener("click", onClick, true);
    return () => document.removeEventListener("click", onClick, true);
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

  // M002-F49（M002-F34／F08 裁决「本人可以直接解除，不会卡死」）：页面改了本人权限后重读有效集，
  // 页面才不会按旧有效集去请求已被拒绝的副资源。
  const refreshEffectivePermissions = React.useCallback(async () => {
    const value = await api.get<EffectivePermissions>("/api/v1/access/effective-permissions");
    setEffectivePermissions({ status: "ready", value });
    return value;
  }, [api]);

  // 3.19 二期：终端开关菜单 gating——[]=无关闭项/加载中(不闪隐藏)，null=拉取失败→兜底关(留自救组)。
  const [disabledSwitchRows, setDisabledSwitchRows] = React.useState<DisabledSwitchRow[] | null>([]);
  // 第 92 条：开关读回之前不出商户首页提醒（模块被关时连请求也不发）。
  const [switchRowsLoaded, setSwitchRowsLoaded] = React.useState(false);

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
      })
      .finally(() => {
        if (!cancelled) setSwitchRowsLoaded(true);
      });
    return () => {
      cancelled = true;
    };
  }, [api, user]);

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
  // 第 25 条 25-R3（BSM:1886）：商户手机应用里另不开库存、经销、人事、账号权限与员工车辆（见 merchantAppExcluded）。
  const inMerchantApp = isMerchantAppMode();
  function routeAllowedHere(target: AdminRoute) {
    return routeAllowsAccountForm(target, accountForm) && !(inMerchantApp && merchantAppExcluded(target));
  }

  // M002-F34（依据 M002-F08 裁决「本人可以直接解除，不会卡死」）：最高权限本人给自己设了拒绝（含 access.role.assign）后，
  // 导航与直链仍能进访问治理工作台去解除；页内只放行「移除本人显式拒绝」，其余路由门禁不变。
  const isMasterSession = user?.id === "usr_master_authority" && user?.username === "master-authority";
  // 小费与服务费为敏感工资信息，入口与后端一致要求逐码查看授权。
  function routePermissionState(target: AdminRoute): EffectivePermissionsState {
    return target.pageId === "ADM-BAR-007" && effectivePermissions.status === "ready"
      ? { ...effectivePermissions, value: { ...effectivePermissions.value, wildcard: false } }
      : effectivePermissions;
  }
  function routePermissions(target: AdminRoute) {
    if (target.pageId === "ADM-STF-003") return hasPermissions(effectivePermissions, ["staff.payroll.manage"]) ? ["staff.payroll.manage"] : ["staff.tax_bracket.manage"];
    if (target.pageId === "ADM-INV-003" && hasPermissions(effectivePermissions, ["inventory.supply_contract.manage"])) return ["inventory.supply_contract.manage"];
    // 营业日配置页综合体与酒吧两套共用（ENUM:187 按每个项目、每家酒吧可配）：持有其中一个查看码即可进入，都没有照旧拒绝。
    // 第十八批第 24 条：高价值复核页门店主管与财务共用，持主管复核码或财务应收应付管理码任一即可进入。
    if (target.pageId === "ADM-AMU-008" || target.pageId === "ADM-INV-004") {
      const held = target.requiredPermissions.find((code) => hasPermissions(effectivePermissions, [code]));
      return held ? [held] : target.requiredPermissions;
    }
    if (target.pageId !== "ADM-ACC-003") return target.requiredPermissions;
    if (isMasterSession) return [];
    // M736-F08（治理地图第 653 行「能配置角色的人都可以设置这一项。」）：有 access.role.manage 的人没有分配角色权限也能进本页配角色；页内各动作照旧各自判权限。
    return hasPermissions(effectivePermissions, ["access.role.manage"]) ? ["access.role.manage"] : target.requiredPermissions;
  }

  // 第 23 条 23-R2（PGM:687「后台、商户专区和员工手机端的所有在线页面都带有按同一规则生成的暗纹」）：页面放行后登记一次打开，
  // 拿到的暗纹码平铺在最上层；拿不到时页面照常、下次切路由重取。敏感页报 sensitive（清单见 SENSITIVE_PAGE_IDS，23-R5）。
  // 23-R5（PGM:687「打开敏感页这件事本身也要留痕」）：敏感页登记成功才显示内容，登记失败报错、不显示（留痕先于查看）。
  const [watermarkCode, setWatermarkCode] = React.useState<string | null>(null);
  const [openedPageId, setOpenedPageId] = React.useState<string | null>(null);
  const sensitivePage = SENSITIVE_PAGE_IDS.has(route.pageId);
  const routeOpen = !!user && routeAllowedHere(route)
    && hasPermissions(routePermissionState(route), routePermissions(route));
  React.useEffect(() => {
    if (!user) {
      setWatermarkCode(null);
      return;
    }
    if (!routeOpen) return;
    let cancelled = false;
    setOpenedPageId(null);
    api
      .post<{ code?: string }>("/api/v1/security/page-views", { pageId: route.pageId, sensitive: sensitivePage, objectRefs: [] })
      .then((mark) => {
        if (cancelled) return;
        if (mark?.code) setWatermarkCode(mark.code);
        setOpenedPageId(route.pageId);
      })
      .catch((error) => {
        if (!cancelled && sensitivePage) setGlobalError(error instanceof Error ? error.message : "页面打开登记失败，请刷新后重试");
      });
    return () => {
      cancelled = true;
    };
  }, [api, user, route, routeOpen, sensitivePage]);

  // 23-R5 这一次看到的对象（PGM:687「哪个客人、哪个商户、哪些单据」）：敏感页每次载入完把显示的行交上来再登记一次，拿到的码不替换已显示的暗纹。
  const reportViewedObjects = React.useCallback((objectType: string, rows: ReadonlyArray<ViewedRow>) => {
    if (!sensitivePage) return;
    const refs = new Map<string, { objectType: string; objectId: string }>();
    for (const row of rows) {
      for (const [type, id] of [[objectType, row.id], ["customer", row.customerId], ["merchant", row.merchantId]] as const) {
        if (id) refs.set(`${type}:${id}`, { objectType: type, objectId: id });
      }
    }
    if (refs.size === 0) return;
    void api.post("/api/v1/security/page-views", { pageId: route.pageId, sensitive: true, objectRefs: [...refs.values()] }).catch(() => undefined);
  }, [api, route.pageId, sensitivePage]);

  // 23-R5（PGM:687「敏感页上的文字不能被选中、不能被复制，也不提供页面另存与打印」）：标记打在 body 上（盖住 antd 弹窗浮层），拦复制、剪切、
  // 右键、拖拽与另存、打印快捷键；不可选中与打印不出内容见 styles.css。浏览器菜单里的另存为拦不住（第十八批裁决 6 不装软件）。
  React.useEffect(() => {
    if (!user || !sensitivePage) return;
    document.body.dataset.sensitive = "true";
    const block = (event: Event) => event.preventDefault();
    const blockKeys = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && /^[sp]$/i.test(event.key)) event.preventDefault();
    };
    const types = ["copy", "cut", "contextmenu", "dragstart"];
    types.forEach((type) => document.addEventListener(type, block));
    document.addEventListener("keydown", blockKeys);
    return () => {
      delete document.body.dataset.sensitive;
      types.forEach((type) => document.removeEventListener(type, block));
      document.removeEventListener("keydown", blockKeys);
    };
  }, [user, sensitivePage]);

  function navigate(nextRoute: AdminRoute) {
    // onClick 级拦截：无权限项不导航（不止 aria-disabled）。
    if (!hasPermissions(routePermissionState(nextRoute), routePermissions(nextRoute))) {
      return;
    }
    if (!routeAllowedHere(nextRoute)) {
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
    if (!target || !hasPermissions(routePermissionState(target), routePermissions(target))) {
      return;
    }
    if (!routeAllowedHere(target)) {
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
      // 商户手机应用（25-R3）：报 merchant_app、带上次拿到的设备编号，响应里的令牌与设备编号存进 App 的系统安全存储（第十八批裁决 19）。
      const store = merchantAppStore();
      const response = await loginApi.post<LoginResponse>("/api/v1/auth/login", store
        ? { login_name: username, password, terminal_type: "merchant_app", device_id: store.deviceId() ?? undefined }
        : { login_name: username, password });
      if (store) await store.save({ accessToken: response.accessToken, refreshToken: response.refreshToken ?? "" }, response.deviceId ?? store.deviceId());
      // M725-F02：上一个人没拿到结果的请求编号不沿用到这个人身上；同一个人重新登录照常沿用。
      forgetWriteOperations(response.user.id);
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
    // 商户手机应用：登出请求已带着令牌发出，再清令牌；设备编号保留（第十八批裁决 19）。
    void merchantAppStore()?.clearTokens().catch(() => undefined);
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
      {watermarkCode ? <PageWatermark code={watermarkCode} theme={themeMode} /> : null}
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
            .map((group) => ({ ...group, routes: group.routes.filter((item) => routeAllowedHere(item)) }))
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
                      const allowed = hasPermissions(routePermissionState(item), routePermissions(item));
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
          {/* M725-F03：重复提交返回第一次的结果时提示已完成。 */}
          <ReplayedNotice />
          {/* 第 92 条（BSM:1597「商户专区首页保留未读账单提醒」）：商户形态账号落地页顶部，在页面门禁之外（没有驾驶舱权限也显示）；商户专区模块被终端开关关闭时不显示。 */}
          {accountForm === "merchant" && route.pageId === "ADM-OV-001" && switchRowsLoaded && !hiddenGroups.has("merchant")
            ? <MerchantUnreadBillsReminder api={api} navigateTo={navigateTo} /> : null}
          {!routeAllowedHere(route) ? (
            // 形态不匹配或商户手机应用不开的页（直链/历史进入）：给业务化提示，不渲染页面、不放请求打 403/400。
            <div className="admin-denied" data-testid="admin-account-form-mismatch">{routeAllowsAccountForm(route, accountForm) ? "此功能请在商户网页门户办理" : accountFormHint(route)}</div>
          ) : (
          <PermissionGate permissionsState={routePermissionState(route)} permissions={routePermissions(route)}>
            {/* lazy 页面用 Suspense 包裹；fallback 故意为 null（轻量占位，
                不带 .loading-state class），避免触发 e2e 的常驻 loading-state 断言。
                chunk 加载完成后页面 testid 会正常出现，Playwright 自动等待。 */}
            <React.Suspense fallback={<div className="admin-page-loading" role="status" aria-live="polite">页面加载中…</div>}>
              {sensitivePage && openedPageId !== route.pageId ? null
                : renderPage(route, { api, user, setActionResult, setGlobalError, effectivePermissions, refreshEffectivePermissions, navigateTo, focusObjectId, reportViewedObjects })}
            </React.Suspense>
          </PermissionGate>
          )}
          {/* OBS-14-02：结果对象按中文键名逐行展示（不再整段 JSON 上屏）。 */}
          {actionResult ? <ActionResultView value={actionResult} /> : null}
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
