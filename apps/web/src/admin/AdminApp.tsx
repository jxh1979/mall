import React from "react";
import { ConfigProvider, theme } from "antd";
import "antd/dist/reset.css";
import { AdminApi } from "./client/adminClient";
import { findAdminRoute, groupedAdminRoutes } from "./adminRoutes";
import { PermissionGate, hasPermissions } from "./security/PermissionGate";
import type { AdminPageProps, AdminRoute, AdminUser, EffectivePermissions, EffectivePermissionsState } from "./types";

// P2 bundle 优化：admin 页面路由级 lazy 分包。
// 此前 32 个页面组件 eager 导入，全部塞进 AdminApp chunk（~224kB）。
// 改为 React.lazy 后每个页面成为独立按需 chunk，仅在路由命中时加载。
// 命名导出页面通过 .then 映射为 default，复用 main.tsx 三壳的 lazy 写法。
const OverviewPage = React.lazy(() => import("./pages/OverviewPage").then((m) => ({ default: m.OverviewPage })));
const AccessUsersPage = React.lazy(() => import("./pages/AccessUsersPage").then((m) => ({ default: m.AccessUsersPage })));
const PermissionTemplatesPage = React.lazy(() => import("./pages/PermissionTemplatesPage").then((m) => ({ default: m.PermissionTemplatesPage })));
const TicketingPoliciesPage = React.lazy(() => import("./pages/TicketingPoliciesPage").then((m) => ({ default: m.TicketingPoliciesPage })));
const DynamicPricingPage = React.lazy(() => import("./pages/DynamicPricingPage").then((m) => ({ default: m.DynamicPricingPage })));
const TicketOrdersPage = React.lazy(() => import("./pages/TicketOrdersPage").then((m) => ({ default: m.TicketOrdersPage })));
const BarTableSessionsPage = React.lazy(() => import("./pages/BarTableSessionsPage").then((m) => ({ default: m.BarTableSessionsPage })));
const FinanceArApLedgerPage = React.lazy(() => import("./pages/FinanceArApLedgerPage").then((m) => ({ default: m.FinanceArApLedgerPage })));
const ReconciliationPage = React.lazy(() => import("./pages/ReconciliationPage").then((m) => ({ default: m.ReconciliationPage })));
const MerchantWorkOrdersPage = React.lazy(() => import("./pages/MerchantWorkOrdersPage").then((m) => ({ default: m.MerchantWorkOrdersPage })));
const MerchantScopePage = React.lazy(() => import("./pages/MerchantScopePage").then((m) => ({ default: m.MerchantScopePage })));
const AmusementSafetyPage = React.lazy(() => import("./pages/AmusementSafetyPage").then((m) => ({ default: m.AmusementSafetyPage })));
const AmusementSessionsPage = React.lazy(() => import("./pages/AmusementSessionsPage").then((m) => ({ default: m.AmusementSessionsPage })));
const AmusementGroupBookingsPage = React.lazy(() => import("./pages/AmusementGroupBookingsPage").then((m) => ({ default: m.AmusementGroupBookingsPage })));
const SupervisorReviewPage = React.lazy(() => import("./pages/SupervisorReviewPage").then((m) => ({ default: m.SupervisorReviewPage })));
const ContractApprovalPage = React.lazy(() => import("./pages/ContractApprovalPage").then((m) => ({ default: m.ContractApprovalPage })));
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
const IntegrationAdaptersPage = React.lazy(() => import("./pages/IntegrationAdaptersPage").then((m) => ({ default: m.IntegrationAdaptersPage })));
const OpsReadinessPage = React.lazy(() => import("./pages/OpsReadinessPage").then((m) => ({ default: m.OpsReadinessPage })));
const GenericAdminPage = React.lazy(() => import("./pages/GenericAdminPage").then((m) => ({ default: m.GenericAdminPage })));

type LoginResponse = {
  accessToken: string;
  refreshToken?: string;
  sessionId?: string;
  tokenType?: string;
  user: AdminUser;
};

function activeRouteFromLocation() {
  return findAdminRoute(window.location.pathname);
}

function renderPage(route: AdminRoute, props: AdminPageProps) {
  switch (route.pageId) {
    case "ADM-OV-001":
      return <OverviewPage {...props} />;
    case "ADM-ACC-001":
      return <AccessUsersPage {...props} />;
    case "ADM-ACC-002":
      return <PermissionTemplatesPage {...props} />;
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
    case "ADM-INV-001":
      return <InventoryLedgerPage {...props} />;
    case "ADM-FIN-002":
      return <ReconciliationPage {...props} />;
    case "ADM-MER-001":
      return <MerchantWorkOrdersPage {...props} />;
    case "ADM-MER-002":
      return <MerchantScopePage {...props} />;
    case "ADM-AMU-001":
      return <AmusementSessionsPage {...props} />;
    case "ADM-AMU-002":
      return <AmusementGroupBookingsPage {...props} />;
    case "ADM-AMU-003":
      return <AmusementSafetyPage {...props} />;
    case "ADM-GOV-006":
      return <SupervisorReviewPage {...props} />;
    case "ADM-GOV-003":
      return <SubjectBindingsPage {...props} />;
    case "ADM-GOV-004":
      return <ContractApprovalPage {...props} />;
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
    case "ADM-FIN-001":
      return <FinanceArApLedgerPage {...props} />;
    case "ADM-INT-001":
      return <IntegrationAdaptersPage {...props} />;
    case "ADM-OPS-001":
      return <OpsReadinessPage {...props} />;
    case "ADM-GOV-001":
      return <GovernanceTopologyPage {...props} />;
    case "ADM-GOV-002":
      return <OrganizationMerchantPage {...props} />;
    default:
      return <GenericAdminPage route={route} {...props} />;
  }
}

export default function AdminApp() {
  const [route, setRoute] = React.useState<AdminRoute>(activeRouteFromLocation);
  const [token, setToken] = React.useState("");
  const [user, setUser] = React.useState<AdminUser | null>(null);
  const [effectivePermissions, setEffectivePermissions] = React.useState<EffectivePermissionsState>({ status: "loading" });
  const [username, setUsername] = React.useState("admin");
  const [password, setPassword] = React.useState("");
  const [loading, setLoading] = React.useState(false);
  const [globalError, setGlobalError] = React.useState<string | null>(null);
  const [actionResult, setActionResult] = React.useState<unknown>(null);

  const api = React.useMemo(() => new AdminApi(token), [token]);

  React.useEffect(() => {
    const onPopState = () => setRoute(activeRouteFromLocation());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  React.useEffect(() => {
    if (!user || !token) {
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
  }, [api, token, user]);

  const [collapsedGroups, setCollapsedGroups] = React.useState<Record<string, boolean>>({});

  function toggleGroup(key: string) {
    setCollapsedGroups((current) => ({ ...current, [key]: !current[key] }));
  }

  function navigate(nextRoute: AdminRoute) {
    // onClick 级拦截：无权限项不导航（不止 aria-disabled）。
    if (!hasPermissions(effectivePermissions, nextRoute.requiredPermissions)) {
      return;
    }
    setRoute(nextRoute);
    setActionResult(null);
    setGlobalError(null);
    if (window.location.pathname !== nextRoute.route) {
      window.history.pushState({ pageId: nextRoute.pageId }, "", nextRoute.route);
    }
  }

  async function login(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setGlobalError(null);
    try {
      const loginApi = new AdminApi("");
      const response = await loginApi.post<LoginResponse>("/api/v1/auth/login", { login_name: username, password });
      setToken(response.accessToken);
      setUser(response.user);
    } catch (loginError) {
      setGlobalError(loginError instanceof Error ? loginError.message : "登录失败");
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    setToken("");
    setUser(null);
    setEffectivePermissions({ status: "loading" });
    setActionResult(null);
    setGlobalError(null);
  }

  const shell = !user ? (
    <main className="admin-auth-shell" data-testid="auth-shell">
      <form className="admin-auth-panel" data-testid="login-view" onSubmit={(event) => void login(event)}>
        <strong>综合体完整后台</strong>
        {globalError ? <div className="admin-inline-error" data-testid="global-error">{globalError}</div> : null}
        <label>账号<input data-testid="login-username" value={username} onChange={(event) => setUsername(event.target.value)} /></label>
        <label>密码<input data-testid="login-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>
        <button type="submit" data-testid="login-submit" disabled={loading}>{loading ? "登录中" : "登录"}</button>
      </form>
    </main>
  ) : (
    <div className="admin-shell" data-testid="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand">综合体完整后台</div>
        <nav data-testid="admin-nav">
          {groupedAdminRoutes().map((group) => {
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
      </aside>
      <section className="admin-main">
        <header className="admin-topbar">
          <div><strong>{route.label}</strong><span>{route.pageId} · {route.route}</span></div>
          <div className="admin-topbar-actions"><span>{user.username}</span><button type="button" data-testid="admin-logout" onClick={logout}>退出</button></div>
        </header>
        {globalError ? <div className="admin-inline-error" data-testid="global-error">{globalError}</div> : null}
        <PermissionGate permissionsState={effectivePermissions} permissions={route.requiredPermissions}>
          {/* lazy 页面用 Suspense 包裹；fallback 故意为 null（轻量占位，
              不带 .loading-state class），避免触发 e2e 的常驻 loading-state 断言。
              chunk 加载完成后页面 testid 会正常出现，Playwright 自动等待。 */}
          <React.Suspense fallback={null}>
            {renderPage(route, { api, user, setActionResult, setGlobalError, effectivePermissions })}
          </React.Suspense>
        </PermissionGate>
        {actionResult ? <pre className="admin-result" data-testid="admin-action-result">{typeof actionResult === "string" ? actionResult : JSON.stringify(actionResult, null, 2)}</pre> : null}
      </section>
    </div>
  );

  return (
    <ConfigProvider theme={{ algorithm: theme.defaultAlgorithm, token: { colorPrimary: "#0f766e", borderRadius: 6 } }}>
      {shell}
    </ConfigProvider>
  );
}
