import type { FormInstance } from "antd";
import React from "react";
import { AmusementPage } from "./pages/AmusementPage";
import { AnalyticsReadOnlyPage } from "./pages/AnalyticsReadOnlyPage";
import { BarPage } from "./pages/BarPage";
import { EventsPage } from "./pages/EventsPage";
import { FloorPlanPage } from "./pages/FloorPlanPage";
import { FoundationPage } from "./pages/FoundationPage";
import { InterfacesPage } from "./pages/InterfacesPage";
import { SecurityPage } from "./pages/SecurityPage";
import { TicketingPage } from "./pages/TicketingPage";
import type { NumericFormValues } from "./stockMovementRequest";
import {
  DashboardPage,
  PageHeader,
  getSectionStats,
  type SectionKey,
} from "./shell";
import type { Snapshot } from "./types";

type HighImpactConfirmation = (title: string, content: string, action: () => Promise<void>) => void;

type LegacyConsoleForms = {
  amusementFlowForm: FormInstance;
  amusementGroupBookingForm: FormInstance;
  amusementVerifyForm: FormInstance;
  barFlowForm: FormInstance;
  dynamicPricingForm: FormInstance;
  dynamicPricingOrderForm: FormInstance;
  dynamicPricingSimulationForm: FormInstance;
  floorPlanForm: FormInstance;
  floorPlanImportForm: FormInstance;
  floorPlanPublishForm: FormInstance;
  foundationForm: FormInstance;
  inventoryForm: FormInstance;
  inventoryItemForm: FormInstance;
  providerAdapterForm: FormInstance;
  riskForm: FormInstance;
  savedBottleForm: FormInstance;
  savedBottlePickupForm: FormInstance;
  settlementForm: FormInstance;
  stockMovementForm: FormInstance;
  ticketFlowForm: FormInstance;
  userForm: FormInstance;
  verifyForm: FormInstance;
};

type LegacyConsoleHandlers = {
  confirmHighImpactAction: HighImpactConfirmation;
  handleAmusementFlow: (values: NumericFormValues) => Promise<void>;
  handleAmusementGroupBooking: (values: NumericFormValues) => Promise<void>;
  handleAmusementVerify: (values: Record<string, string>) => Promise<void>;
  handleApproveGroupBooking: (bookingId: string) => Promise<void>;
  handleBarFlow: (values: NumericFormValues) => Promise<void>;
  handleCreateProviderAdapter: (values: Record<string, string>) => Promise<void>;
  handleCreateUser: (values: Record<string, string>) => Promise<void>;
  handleDynamicPricingOrder: (values: NumericFormValues) => Promise<void>;
  handleDynamicPricingRule: (values: NumericFormValues) => Promise<void>;
  handleDynamicPricingSimulation: (values: NumericFormValues) => Promise<void>;
  handleFloorPlanCreate: (values: NumericFormValues) => Promise<void>;
  handleFloorPlanImport: (values: Record<string, string>) => Promise<void>;
  handleFoundationFlow: (values: Record<string, string>) => Promise<void>;
  handleInventoryFlow: (values: NumericFormValues) => Promise<void>;
  handleInventoryItemCreate: (values: NumericFormValues) => Promise<void>;
  handleLoadFloorPlanImportDetails: (floorPlanId: string, importId: string) => Promise<void>;
  handlePreview: () => Promise<void>;
  handlePublishDynamicPricingRule: (ruleId: string) => Promise<void>;
  handlePublishFloorPlanImport: (values: Record<string, string>) => Promise<void>;
  handleReleaseGroupBooking: (bookingId: string) => Promise<void>;
  handleResolveRisk: (alertId: string) => Promise<void>;
  handleRiskAlert: (values: Record<string, string>) => Promise<void>;
  handleSavedBottleCreate: (values: NumericFormValues) => Promise<void>;
  handleSavedBottlePickup: (values: NumericFormValues) => Promise<void>;
  handleSettlementGenerate: (values: Record<string, string>) => Promise<void>;
  handleStockMovement: (values: NumericFormValues) => Promise<void>;
  handleTicketFlow: (values: NumericFormValues) => Promise<void>;
  handleVerify: () => Promise<void>;
  handleWebhookReserved: () => Promise<void>;
};

type LegacyConsoleContentProps = {
  activeSection: SectionKey;
  forms: LegacyConsoleForms;
  handlers: LegacyConsoleHandlers;
  onSectionChange: (section: SectionKey) => void;
  selectedFloorPlanId: string;
  selectedFloorPlanImportId: string;
  setSelectedFloorPlanId: (value: string) => void;
  setSelectedFloorPlanImportId: (value: string) => void;
  snapshotValue: Snapshot;
};

export function LegacyConsoleContent({
  activeSection,
  forms,
  handlers,
  onSectionChange,
  selectedFloorPlanId,
  selectedFloorPlanImportId,
  setSelectedFloorPlanId,
  setSelectedFloorPlanImportId,
  snapshotValue,
}: LegacyConsoleContentProps) {
  let sectionContent: React.ReactNode = <DashboardPage activeSection={activeSection} snapshotValue={snapshotValue} onSelect={onSectionChange} />;

  if (activeSection === "foundation") sectionContent = <FoundationPage
    snapshotValue={snapshotValue}
    foundationForm={forms.foundationForm}
    userForm={forms.userForm}
    handleFoundationFlow={handlers.handleFoundationFlow}
    handleCreateUser={handlers.handleCreateUser}
  />;

  if (activeSection === "floorplan") sectionContent = <FloorPlanPage
    snapshotValue={snapshotValue}
    selectedFloorPlanId={selectedFloorPlanId}
    selectedFloorPlanImportId={selectedFloorPlanImportId}
    setSelectedFloorPlanId={setSelectedFloorPlanId}
    setSelectedFloorPlanImportId={setSelectedFloorPlanImportId}
    floorPlanForm={forms.floorPlanForm}
    floorPlanImportForm={forms.floorPlanImportForm}
    floorPlanPublishForm={forms.floorPlanPublishForm}
    handleFloorPlanCreate={handlers.handleFloorPlanCreate}
    handleFloorPlanImport={handlers.handleFloorPlanImport}
    handleLoadFloorPlanImportDetails={handlers.handleLoadFloorPlanImportDetails}
    handlePublishFloorPlanImport={handlers.handlePublishFloorPlanImport}
    confirmHighImpactAction={handlers.confirmHighImpactAction}
  />;

  if (activeSection === "ticketing") sectionContent = <TicketingPage
    snapshotValue={snapshotValue}
    ticketFlowForm={forms.ticketFlowForm}
    verifyForm={forms.verifyForm}
    settlementForm={forms.settlementForm}
    dynamicPricingForm={forms.dynamicPricingForm}
    dynamicPricingSimulationForm={forms.dynamicPricingSimulationForm}
    dynamicPricingOrderForm={forms.dynamicPricingOrderForm}
    handleTicketFlow={handlers.handleTicketFlow}
    handlePreview={handlers.handlePreview}
    handleVerify={handlers.handleVerify}
    handleSettlementGenerate={handlers.handleSettlementGenerate}
    handleDynamicPricingRule={handlers.handleDynamicPricingRule}
    handlePublishDynamicPricingRule={handlers.handlePublishDynamicPricingRule}
    handleDynamicPricingSimulation={handlers.handleDynamicPricingSimulation}
    handleDynamicPricingOrder={handlers.handleDynamicPricingOrder}
    confirmHighImpactAction={handlers.confirmHighImpactAction}
  />;

  if (activeSection === "bar") sectionContent = <BarPage
    snapshotValue={snapshotValue}
    barFlowForm={forms.barFlowForm}
    inventoryForm={forms.inventoryForm}
    inventoryItemForm={forms.inventoryItemForm}
    stockMovementForm={forms.stockMovementForm}
    savedBottleForm={forms.savedBottleForm}
    savedBottlePickupForm={forms.savedBottlePickupForm}
    handleBarFlow={handlers.handleBarFlow}
    handleInventoryFlow={handlers.handleInventoryFlow}
    handleInventoryItemCreate={handlers.handleInventoryItemCreate}
    handleStockMovement={handlers.handleStockMovement}
    handleSavedBottleCreate={handlers.handleSavedBottleCreate}
    handleSavedBottlePickup={handlers.handleSavedBottlePickup}
  />;

  if (activeSection === "amusement") sectionContent = <AmusementPage
    snapshotValue={snapshotValue}
    amusementFlowForm={forms.amusementFlowForm}
    amusementVerifyForm={forms.amusementVerifyForm}
    amusementGroupBookingForm={forms.amusementGroupBookingForm}
    handleAmusementFlow={handlers.handleAmusementFlow}
    handleAmusementVerify={handlers.handleAmusementVerify}
    handleAmusementGroupBooking={handlers.handleAmusementGroupBooking}
    handleApproveGroupBooking={handlers.handleApproveGroupBooking}
    handleReleaseGroupBooking={handlers.handleReleaseGroupBooking}
    confirmHighImpactAction={handlers.confirmHighImpactAction}
  />;

  if (activeSection === "analytics") sectionContent = <AnalyticsReadOnlyPage snapshotValue={snapshotValue} />;

  if (activeSection === "interfaces") sectionContent = <InterfacesPage
    snapshotValue={snapshotValue}
    providerAdapterForm={forms.providerAdapterForm}
    handleWebhookReserved={handlers.handleWebhookReserved}
    handleCreateProviderAdapter={handlers.handleCreateProviderAdapter}
  />;

  if (activeSection === "security") sectionContent = <SecurityPage
    snapshotValue={snapshotValue}
    riskForm={forms.riskForm}
    handleRiskAlert={handlers.handleRiskAlert}
    handleResolveRisk={handlers.handleResolveRisk}
  />;

  if (activeSection === "events") sectionContent = <EventsPage snapshotValue={snapshotValue} />;

  return (
    <>
      <PageHeader
        activeSection={activeSection}
        loadedAt={snapshotValue.loadedAt}
        stats={getSectionStats(activeSection, snapshotValue)}
        healthOk={snapshotValue.health.ok}
        onSectionChange={onSectionChange}
      />
      {sectionContent}
    </>
  );
}

export type { LegacyConsoleForms, LegacyConsoleHandlers };