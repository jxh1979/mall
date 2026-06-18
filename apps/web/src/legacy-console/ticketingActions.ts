import type { FormInstance } from "antd";
import { postJson } from "./apiClient";
import { toCents } from "./formatters";
import { adminRoutes, redirectToAdmin } from "./redirectToAdmin";
import type { NumericFormValues } from "./stockMovementRequest";
import type {
  EntitlementTemplate,
  IssuedEntitlement,
  TicketDynamicPricingRule,
  TicketDynamicPricingSimulation,
  TicketOrder,
  TicketProduct,
} from "./types";

type RunAction = <T>(label: string, action: () => Promise<T>, reset?: () => void) => Promise<T>;

type TicketingActionDependencies = {
  dynamicPricingForm: FormInstance;
  dynamicPricingOrderForm: FormInstance;
  dynamicPricingSimulationForm: FormInstance;
  runAction: RunAction;
  settlementForm: FormInstance;
  ticketFlowForm: FormInstance;
  verifyForm: FormInstance;
};

export type TicketingActions = ReturnType<typeof createTicketingActions>;

function dynamicCondition(values: NumericFormValues) {
  const key = String(values.conditionKey || "").trim();
  const value = String(values.conditionValue || "").trim();
  return key && value ? { [key]: value } : {};
}

export function createTicketingActions({
  dynamicPricingForm,
  dynamicPricingOrderForm: _dynamicPricingOrderForm,
  dynamicPricingSimulationForm: _dynamicPricingSimulationForm,
  runAction,
  settlementForm,
  ticketFlowForm,
  verifyForm,
}: TicketingActionDependencies) {
  async function handleTicketFlow(values: NumericFormValues) {
    await runAction("票务闭环", async () => {
      const product = await postJson<TicketProduct>("/api/v1/ticketing/ticket-types", {
        productName: values.productName,
        productType: "package",
        priceAmount: toCents(Number(values.priceYuan)),
        status: "active",
      });
      const template = await postJson<EntitlementTemplate>("/api/v1/ticketing/entitlement-templates", {
        name: values.entitlementName,
        entitlementType: values.entitlementType || "entry",
        defaultTotalTimes: Number(values.totalTimes || 1),
        defaultValueAmount: toCents(Number(values.priceYuan)),
      });
      await postJson(`/api/v1/ticketing/ticket-types/${product.id}/entitlements`, {
        entitlementTemplateId: template.id,
        quantity: 1,
        totalTimes: Number(values.totalTimes || 1),
        valueAmount: toCents(Number(values.priceYuan)),
      });
      const issued = await postJson<{ ticketCode: string; issuedEntitlements: IssuedEntitlement[] }>(`/api/v1/ticketing/ticket-types/${product.id}/issue`, {
        purchaserName: values.purchaserName,
        purchaserPhone: values.purchaserPhone,
      });
      verifyForm.setFieldValue("entitlementCode", issued.issuedEntitlements[0]?.entitlementCode);
      return { product, template, issued };
    }, () => ticketFlowForm.resetFields());
  }

  async function handlePreview() {
    const entitlementCode = verifyForm.getFieldValue("entitlementCode");
    await runAction("核销预检", () => postJson("/api/v1/ticketing/verifications/preview", { entitlementCode }));
  }

  // 终端铁律：Web 不可直接核销（改变核销结果）。改为跳转管理后台执行。
  async function handleVerify() {
    redirectToAdmin("门票/权益核销", adminRoutes.ticketingVerification);
  }

  async function handleDynamicPricingRule(values: NumericFormValues) {
    await runAction("P2 动态定价规则", () => postJson<TicketDynamicPricingRule>("/api/v1/ticketing/dynamic-pricing-rules", {
      ticketTypeId: values.ticketTypeId,
      ruleName: values.ruleName,
      priority: Number(values.priority || 0),
      effectiveFrom: values.effectiveFrom,
      effectiveTo: values.effectiveTo,
      salesChannel: values.salesChannel || undefined,
      adjustmentType: values.adjustmentType,
      adjustmentRate: values.adjustmentType === "percent_delta" ? Number(values.adjustmentRate || 0) : undefined,
      adjustmentAmountCent: values.adjustmentType === "fixed_delta" ? toCents(Number(values.adjustmentAmountYuan || 0)) : undefined,
      minPriceCent: values.minPriceYuan === undefined || values.minPriceYuan === "" ? undefined : toCents(Number(values.minPriceYuan)),
      maxPriceCent: values.maxPriceYuan === undefined || values.maxPriceYuan === "" ? undefined : toCents(Number(values.maxPriceYuan)),
      conditionRule: dynamicCondition(values),
      operatorNote: values.operatorNote || undefined,
    }), () => dynamicPricingForm.resetFields());
  }

  async function handlePublishDynamicPricingRule(ruleId: string) {
    await runAction("P2 动态定价发布", () => postJson<TicketDynamicPricingRule>(`/api/v1/ticketing/dynamic-pricing-rules/${ruleId}/publish`, {
      action: "publish",
      reason: "Web 管理端发布动态定价规则",
    }));
  }

  async function handleDynamicPricingSimulation(values: NumericFormValues) {
    await runAction("P2 动态定价模拟", () => postJson<TicketDynamicPricingSimulation>("/api/v1/ticketing/dynamic-pricing/simulations", {
      ticketTypeId: values.ticketTypeId,
      visitDate: values.visitDate,
      salesChannel: values.salesChannel || "web",
      context: dynamicCondition(values),
    }));
  }

  async function handleDynamicPricingOrder(values: NumericFormValues) {
    await runAction("dynamic pricing order", () => postJson<TicketOrder>("/api/v1/ticketing/orders", {
      ticketTypeId: values.ticketTypeId,
      quantity: Number(values.quantity || 1),
      visitDate: values.visitDate,
      salesChannel: values.salesChannel || "web",
      buyerPhone: values.buyerPhone || undefined,
      customerId: values.customerId || undefined,
      dynamicPricingContext: dynamicCondition(values),
    }));
  }

  async function handleSettlementGenerate(values: Record<string, string>) {
    await runAction("结算单生成", () => postJson("/api/v1/finance/ar-ap-items", {
      merchantId: values.merchantId || undefined,
    }), () => settlementForm.resetFields());
  }

  return {
    handleDynamicPricingOrder,
    handleDynamicPricingRule,
    handleDynamicPricingSimulation,
    handlePreview,
    handlePublishDynamicPricingRule,
    handleSettlementGenerate,
    handleTicketFlow,
    handleVerify,
  };
}