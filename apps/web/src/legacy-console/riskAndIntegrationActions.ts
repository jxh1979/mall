import type { FormInstance } from "antd";
import { postJson } from "./apiClient";
import type { ProviderAdapter } from "./types";

type RunAction = <T>(label: string, action: () => Promise<T>, reset?: () => void) => Promise<T>;

type RiskAndIntegrationActionDependencies = {
  providerAdapterForm: FormInstance;
  riskForm: FormInstance;
  runAction: RunAction;
};

export type RiskAndIntegrationActions = ReturnType<typeof createRiskAndIntegrationActions>;

export function createRiskAndIntegrationActions({ providerAdapterForm, riskForm, runAction }: RiskAndIntegrationActionDependencies) {
  async function handleRiskAlert(values: Record<string, string>) {
    await runAction("风险预警创建", () => postJson("/api/v1/analytics/risk-alerts", {
      ruleId: values.ruleId || undefined,
      alertType: values.alertType || "manual",
      riskLevel: values.riskLevel || "normal",
      objectType: values.objectType || "p0",
      objectId: values.objectId || undefined,
      summary: values.summary,
    }), () => riskForm.resetFields());
  }

  async function handleResolveRisk(alertId: string) {
    await runAction("风险处理", () => postJson(`/api/v1/analytics/risk-alerts/${alertId}/resolve`, { reason: "P0 页面处理" }));
  }

  async function handleWebhookReserved() {
    await runAction("第三方预留 Webhook", () => postJson("/api/open/d/p0-frontend-test/webhooks/device/p0.frontend-test", {
      requestId: `web-${Date.now()}`,
      payload: { source: "P0 web console", reserved: true },
    }));
  }

  async function handleCreateProviderAdapter(values: Record<string, string>) {
    await runAction("供应商适配器创建", () => postJson<ProviderAdapter>("/api/v1/integration/provider-adapters", {
      providerCode: values.providerCode,
      adapterType: values.adapterType || "reservation_resource",
      mode: values.mode || "sdk_api",
      fieldMapping: {},
      webhookConfig: { preferred: values.webhookEnabled !== "false" },
      pollingConfig: { enabled: true, intervalMinutes: Number(values.pollingIntervalMinutes || 10) },
      productionReady: false,
      operatorNote: values.operatorNote || undefined,
      evidenceAttachmentIds: [],
    }), () => providerAdapterForm.resetFields());
  }

  return {
    handleCreateProviderAdapter,
    handleResolveRisk,
    handleRiskAlert,
    handleWebhookReserved,
  };
}
