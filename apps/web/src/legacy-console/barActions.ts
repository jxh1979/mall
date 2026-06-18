import type { FormInstance } from "antd";
import { postJson } from "./apiClient";
import { adminRoutes, redirectToAdmin } from "./redirectToAdmin";
import { buildStockMovementRequest, type NumericFormValues } from "./stockMovementRequest";
import type { Snapshot } from "./types";

type RunAction = <T>(label: string, action: () => Promise<T>, reset?: () => void) => Promise<T>;

type BarActionDependencies = {
  barFlowForm: FormInstance;
  inventoryForm: FormInstance;
  inventoryItemForm: FormInstance;
  runAction: RunAction;
  savedBottleForm: FormInstance;
  savedBottlePickupForm: FormInstance;
  snapshot: Snapshot | null;
  stockMovementForm: FormInstance;
};

export type BarActions = ReturnType<typeof createBarActions>;

export function createBarActions({
  inventoryForm,
  inventoryItemForm,
  runAction,
  stockMovementForm,
}: BarActionDependencies) {
  // 终端铁律：Web 不可直接执行酒吧结账支付（改变资金）。改为跳转管理后台执行。
  async function handleBarFlow(_values: NumericFormValues) {
    redirectToAdmin("酒吧开台/点单/结账支付", adminRoutes.barOrders);
  }

  async function handleInventoryFlow(values: NumericFormValues) {
    await runAction("登记库存流水", () => postJson("/api/v1/inventory/stock-movements", {
      productId: values.productId || undefined,
      flowType: values.flowType || "inbound",
      quantity: Number(values.quantity || 0),
      reason: values.reason || undefined,
    }), () => inventoryForm.resetFields());
  }

  async function handleInventoryItemCreate(values: NumericFormValues) {
    await runAction("P1 库存物料创建", () => postJson("/api/v1/inventory/items", {
      merchantId: values.merchantId || undefined,
      venueId: values.venueId || undefined,
      itemName: values.itemName,
      category: values.category || "drink",
      unit: values.unit || "bottle",
      minQuantity: Number(values.minQuantity || 0),
    }), () => inventoryItemForm.resetFields());
  }

  async function handleStockMovement(values: NumericFormValues) {
    const { path, payload } = buildStockMovementRequest(values);
    await runAction("P1 库存动作", () => postJson(path, payload), () => stockMovementForm.resetFields());
  }

  // 终端铁律：Web 不可直接登记存酒（改变库存）。改为跳转管理后台执行。
  async function handleSavedBottleCreate(_values: NumericFormValues) {
    redirectToAdmin("存酒登记", adminRoutes.barStoredWine);
  }

  // 终端铁律：Web 不可直接取酒（claim 改变库存）。改为跳转管理后台执行。
  async function handleSavedBottlePickup(_values: NumericFormValues) {
    redirectToAdmin("存酒取用", adminRoutes.barStoredWine);
  }

  return {
    handleBarFlow,
    handleInventoryFlow,
    handleInventoryItemCreate,
    handleSavedBottleCreate,
    handleSavedBottlePickup,
    handleStockMovement,
  };
}
