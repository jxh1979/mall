import type { JsonBody } from "./types";

export type NumericFormValues = Record<string, string | number>;

export type StockMovementRequest = {
  path: string;
  payload: JsonBody;
};

function defaultStockLocation(movementType: string): string {
  if (movementType === "inbound") return "warehouse_stock";
  return "bar_stock";
}

function stockMovementPath(movementType: string): string {
  switch (movementType) {
    case "transfer":
      return "/api/v1/inventory/stock-movements/transfer";
    case "count":
      return "/api/v1/inventory/stock-adjustments";
    default:
      return `/api/v1/inventory/stock-movements/${movementType}`;
  }
}

export function buildStockMovementRequest(values: NumericFormValues): StockMovementRequest {
  const movementType = String(values.movementType || "inbound");
  const basePayload = {
    merchantId: values.merchantId || undefined,
    venueId: values.venueId || undefined,
    itemId: values.itemId,
    productId: values.productId || undefined,
    reason: values.reason || undefined,
  };

  switch (movementType) {
    case "transfer":
      return {
        path: stockMovementPath(movementType),
        payload: {
          ...basePayload,
          quantity: Number(values.quantity || 0),
          fromLocationType: values.fromLocationType || "warehouse_stock",
          toLocationType: values.toLocationType || "bar_stock",
        },
      };
    case "count":
      return {
        path: stockMovementPath(movementType),
        payload: {
          ...basePayload,
          countedQuantity: Number(values.countedQuantity || values.quantity || 0),
          locationType: values.locationType || "bar_stock",
        },
      };
    default:
      return {
        path: stockMovementPath(movementType),
        payload: {
          ...basePayload,
          quantity: Number(values.quantity || 0),
          locationType: values.locationType || defaultStockLocation(movementType),
        },
      };
  }
}
