import React from "react";
import { ConfigProvider, theme } from "antd";
import "antd/dist/reset.css";
import { AdminApi } from "./client/adminClient";
import { adminRoutes, findAdminRoute } from "./adminRoutes";
import { PermissionGate, hasPermissions } from "./security/PermissionGate";
import { OverviewPage } from "./pages/OverviewPage";
import { AccessUsersPage } from "./pages/AccessUsersPage";
import { PermissionTemplatesPage } from "./pages/PermissionTemplatesPage";
import { TicketingPoliciesPage } from "./pages/TicketingPoliciesPage";
import { DynamicPricingPage } from "./pages/DynamicPricingPage";
import { TicketOrdersPage } from "./pages/TicketOrdersPage";
import { BarTableSessionsPage } from "./pages/BarTableSessionsPage";
import { FinanceArApLedgerPage } from "./pages/FinanceArApLedgerPage";
import { ReconciliationPage } from "./pages/ReconciliationPage";
import { MerchantWorkOrdersPage } from "./pages/MerchantWorkOrdersPage";
import { MerchantScopePage } from "./pages/MerchantScopePage";
import { AmusementSafetyPage } from "./pages/AmusementSafetyPage";
import { AmusementSessionsPage } from "./pages/AmusementSessionsPage";
import { AmusementGroupBookingsPage } from "./pages/AmusementGroupBookingsPage";
import { SupervisorReviewPage } from "./pages/SupervisorReviewPage";
import { ContractApprovalPage } from "./pages/ContractApprovalPage";
import { GovernanceTopologyPage } from "./pages/GovernanceTopologyPage";
import { OrganizationMerchantPage } from "./pages/OrganizationMerchantPage";
import { SubjectBindingsPage } from "./pages/SubjectBindingsPage";
import { AdmissionAuditsPage } from "./pages/AdmissionAuditsPage";
import { RiskAlertsPage } from "./pages/RiskAlertsPage";
import { IntegrationEventsPage } from "./pages/IntegrationEventsPage";
import { AuditEventsPage } from "./pages/AuditEventsPage";
import { BusinessAnalyticsPage } from "./pages/BusinessAnalyticsPage";
import { InventoryLedgerPage } from "./pages/InventoryLedgerPage";
import { BarOrdersPage } from "./pages/BarOrdersPage";
import { StoredWinePage } from "./pages/StoredWinePage";
import { BarCommissionsPage } from "./pages/BarCommissionsPage";
import { IntegrationAdaptersPage } from "./pages/IntegrationAdaptersPage";
import { OpsReadinessPage } from "./pages/OpsReadinessPage";
import { GenericAdminPage } from "./pages/GenericAdminPage";
import type { AdminPageProps, AdminRoute, AdminUser, EffectivePermissions, EffectivePermissionsState } from "./types";

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

  function navigate(nextRoute: AdminRoute) {
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
          {adminRoutes.map((item) => {
            const allowed = hasPermissions(effectivePermissions, item.requiredPermissions);
            return (
              <button
                type="button"
                key={item.pageId}
                data-testid={`admin-nav-${item.pageId}`}
                className={item.pageId === route.pageId ? "active" : ""}
                onClick={() => navigate(item)}
                aria-disabled={!allowed}
              >
                <span>{item.module}</span>
                <strong>{item.label}</strong>
              </button>
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
          {renderPage(route, { api, user, setActionResult, setGlobalError })}
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
