import {
  ConfigProvider,
  Form,
  message,
  theme,
} from "antd";
import React from "react";
import { hasApiAccessToken, postJson, setApiAccessToken } from "./apiClient";
import { loadLegacyConsoleSnapshot } from "./snapshotClient";
import { AuthShell } from "./AuthShell";
import { LegacyAppShell } from "./LegacyAppShell";
import { LegacyConsoleContent, type LegacyConsoleForms, type LegacyConsoleHandlers } from "./LegacyConsoleContent";
import { createBarActions } from "./barActions";
import { createAmusementActions } from "./amusementActions";
import { createFoundationActions } from "./foundationActions";
import { createFloorPlanActions } from "./floorPlanActions";
import { createRiskAndIntegrationActions } from "./riskAndIntegrationActions";
import { createTicketingActions } from "./ticketingActions";
import {
  defaultOpenMenuKeys,
  getInitialSectionFromLocation,
  sectionUrl,
  type SectionKey,
} from "./shell";

import type {
  ApiUser,
  LoginResponse,
  Snapshot,
} from "./types";

const appTheme = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: "#0f766e",
    borderRadius: 6,
    fontFamily: "Noto Sans CJK SC, Microsoft YaHei, PingFang SC, Hiragino Sans GB, Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif",
  },
};

export default function LegacyConsoleApp() {
  const [snapshot, setSnapshot] = React.useState<Snapshot | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [authUser, setAuthUser] = React.useState<ApiUser | null>(null);
  const [activeSection, setActiveSection] = React.useState<SectionKey>(() => getInitialSectionFromLocation());
  const [openMenuKeys, setOpenMenuKeys] = React.useState<string[]>(defaultOpenMenuKeys);
  const [sidebarCollapsed, setSidebarCollapsed] = React.useState(false);
  const [lastResult, setLastResult] = React.useState<unknown>(null);
  const [pendingConfirmation, setPendingConfirmation] = React.useState<{ title: string; content: string; action: () => Promise<void> } | null>(null);
  const [selectedFloorPlanId, setSelectedFloorPlanId] = React.useState('');
  const [selectedFloorPlanImportId, setSelectedFloorPlanImportId] = React.useState('');
  const [loginForm] = Form.useForm();
  const [ticketFlowForm] = Form.useForm();
  const [verifyForm] = Form.useForm();
  const [settlementForm] = Form.useForm();
  const [foundationForm] = Form.useForm();
  const [userForm] = Form.useForm();
  const [floorPlanForm] = Form.useForm();
  const [floorPlanImportForm] = Form.useForm();
  const [floorPlanPublishForm] = Form.useForm();
  const [barFlowForm] = Form.useForm();
  const [inventoryForm] = Form.useForm();
  const [inventoryItemForm] = Form.useForm();
  const [stockMovementForm] = Form.useForm();
  const [savedBottleForm] = Form.useForm();
  const [savedBottlePickupForm] = Form.useForm();
  const [amusementFlowForm] = Form.useForm();
  const [amusementVerifyForm] = Form.useForm();
  const [amusementGroupBookingForm] = Form.useForm();
  const [dynamicPricingForm] = Form.useForm();
  const [dynamicPricingSimulationForm] = Form.useForm();
  const [dynamicPricingOrderForm] = Form.useForm();
  const [riskForm] = Form.useForm();
  const [providerAdapterForm] = Form.useForm();

  const navigateToSection = React.useCallback((section: SectionKey) => {
    setActiveSection(section);
    const nextUrl = sectionUrl(section);
    if (`${window.location.pathname}${window.location.search}` !== nextUrl) {
      window.history.pushState({ section }, "", nextUrl);
    }
  }, []);

  React.useEffect(() => {
    const handlePopState = () => setActiveSection(getInitialSectionFromLocation());
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  const loadData = React.useCallback(async () => {
    if (!hasApiAccessToken()) return;
    setLoading(true);
    setError(null);
    try {
      const result = await loadLegacyConsoleSnapshot({ selectedFloorPlanId, selectedFloorPlanImportId });
      if (result.selectedFloorPlanId !== selectedFloorPlanId) setSelectedFloorPlanId(result.selectedFloorPlanId);
      if (result.selectedFloorPlanImportId !== selectedFloorPlanImportId) setSelectedFloorPlanImportId(result.selectedFloorPlanImportId);
      setSnapshot(result.snapshot);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "\u8bfb取失败");
    } finally {
      setLoading(false);
    }
  }, [selectedFloorPlanId, selectedFloorPlanImportId]);

  React.useEffect(() => {
    if (authUser) void loadData();
  }, [authUser, loadData]);

  async function handleLogin(values: Record<string, string>) {
    setLoading(true);
    setError(null);
    try {
      const login = await postJson<LoginResponse>("/api/v1/auth/login", {
        login_name: values.username,
        password: values.password,
      });
      setApiAccessToken(login.accessToken);
      setAuthUser(login.user);
      message.success("登录成功");
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : "登录失败");
    } finally {
      setLoading(false);
    }
  }

  function handleLogout() {
    setApiAccessToken("");
    setAuthUser(null);
    setSnapshot(null);
    setLastResult(null);
    setError(null);
    message.success("\u5df2\u9000\u51fa");
  }

  async function runAction<T>(label: string, action: () => Promise<T>, reset?: () => void) {
    try {
      const result = await action();
      setLastResult(result);
      message.success(`${label}完成`);
      reset?.();
      await loadData();
      return result;
    } catch (actionError) {
      const detail = actionError instanceof Error ? actionError.message : `${label}失败`;
      setLastResult({ error: detail });
      message.error(detail);
      throw actionError;
    }
  }

  const foundationActions = createFoundationActions({
    foundationForm,
    runAction,
    snapshot,
    userForm,
  });

  const floorPlanActions = createFloorPlanActions({
    floorPlanForm,
    floorPlanImportForm,
    floorPlanPublishForm,
    runAction,
    selectedFloorPlanId,
    selectedFloorPlanImportId,
    setSelectedFloorPlanId,
    setSelectedFloorPlanImportId,
    snapshot,
  });

  const ticketingActions = createTicketingActions({
    dynamicPricingForm,
    dynamicPricingOrderForm,
    dynamicPricingSimulationForm,
    runAction,
    settlementForm,
    ticketFlowForm,
    verifyForm,
  });

  const barActions = createBarActions({
    barFlowForm,
    inventoryForm,
    inventoryItemForm,
    runAction,
    savedBottleForm,
    savedBottlePickupForm,
    snapshot,
    stockMovementForm,
  });

  const amusementActions = createAmusementActions({
    amusementFlowForm,
    amusementGroupBookingForm,
    amusementVerifyForm,
    runAction,
    snapshot,
  });

  const riskAndIntegrationActions = createRiskAndIntegrationActions({
    providerAdapterForm,
    riskForm,
    runAction,
  });

  function confirmHighImpactAction(title: string, content: string, action: () => Promise<void>) {
    setPendingConfirmation({ title, content, action });
  }

  if (!authUser) {
    return (
      <ConfigProvider theme={appTheme}>
        <AuthShell error={error} form={loginForm} loading={loading} onLogin={handleLogin} />
      </ConfigProvider>
    );
  }

  const forms: LegacyConsoleForms = {
    amusementFlowForm,
    amusementGroupBookingForm,
    amusementVerifyForm,
    barFlowForm,
    dynamicPricingForm,
    dynamicPricingOrderForm,
    dynamicPricingSimulationForm,
    floorPlanForm,
    floorPlanImportForm,
    floorPlanPublishForm,
    foundationForm,
    inventoryForm,
    inventoryItemForm,
    providerAdapterForm,
    riskForm,
    savedBottleForm,
    savedBottlePickupForm,
    settlementForm,
    stockMovementForm,
    ticketFlowForm,
    userForm,
    verifyForm,
  };

  const handlers: LegacyConsoleHandlers = {
    confirmHighImpactAction,
    handleAmusementFlow: amusementActions.handleAmusementFlow,
    handleAmusementGroupBooking: amusementActions.handleAmusementGroupBooking,
    handleAmusementVerify: amusementActions.handleAmusementVerify,
    handleApproveGroupBooking: amusementActions.handleApproveGroupBooking,
    handleBarFlow: barActions.handleBarFlow,
    handleCreateProviderAdapter: riskAndIntegrationActions.handleCreateProviderAdapter,
    handleCreateUser: foundationActions.handleCreateUser,
    handleDynamicPricingOrder: ticketingActions.handleDynamicPricingOrder,
    handleDynamicPricingRule: ticketingActions.handleDynamicPricingRule,
    handleDynamicPricingSimulation: ticketingActions.handleDynamicPricingSimulation,
    handleFloorPlanCreate: floorPlanActions.handleFloorPlanCreate,
    handleFloorPlanImport: floorPlanActions.handleFloorPlanImport,
    handleFoundationFlow: foundationActions.handleFoundationFlow,
    handleInventoryFlow: barActions.handleInventoryFlow,
    handleInventoryItemCreate: barActions.handleInventoryItemCreate,
    handleLoadFloorPlanImportDetails: floorPlanActions.handleLoadFloorPlanImportDetails,
    handlePreview: ticketingActions.handlePreview,
    handlePublishDynamicPricingRule: ticketingActions.handlePublishDynamicPricingRule,
    handlePublishFloorPlanImport: floorPlanActions.handlePublishFloorPlanImport,
    handleReleaseGroupBooking: amusementActions.handleReleaseGroupBooking,
    handleResolveRisk: riskAndIntegrationActions.handleResolveRisk,
    handleRiskAlert: riskAndIntegrationActions.handleRiskAlert,
    handleSavedBottleCreate: barActions.handleSavedBottleCreate,
    handleSavedBottlePickup: barActions.handleSavedBottlePickup,
    handleSettlementGenerate: ticketingActions.handleSettlementGenerate,
    handleStockMovement: barActions.handleStockMovement,
    handleTicketFlow: ticketingActions.handleTicketFlow,
    handleVerify: ticketingActions.handleVerify,
    handleWebhookReserved: riskAndIntegrationActions.handleWebhookReserved,
  };

  return (
    <ConfigProvider theme={appTheme}>
      <LegacyAppShell
        activeSection={activeSection}
        authUser={authUser}
        error={error}
        lastResult={lastResult}
        loading={loading}
        onLogout={handleLogout}
        onRefresh={() => void loadData()}
        onSectionChange={navigateToSection}
        openMenuKeys={openMenuKeys}
        pendingConfirmation={pendingConfirmation}
        setOpenMenuKeys={setOpenMenuKeys}
        setPendingConfirmation={setPendingConfirmation}
        setSidebarCollapsed={setSidebarCollapsed}
        sidebarCollapsed={sidebarCollapsed}
        snapshot={snapshot}
      >
        {snapshot ? (
          <LegacyConsoleContent
            activeSection={activeSection}
            forms={forms}
            handlers={handlers}
            onSectionChange={navigateToSection}
            selectedFloorPlanId={selectedFloorPlanId}
            selectedFloorPlanImportId={selectedFloorPlanImportId}
            setSelectedFloorPlanId={setSelectedFloorPlanId}
            setSelectedFloorPlanImportId={setSelectedFloorPlanImportId}
            snapshotValue={snapshot}
          />
        ) : null}
      </LegacyAppShell>
    </ConfigProvider>
  );
}
