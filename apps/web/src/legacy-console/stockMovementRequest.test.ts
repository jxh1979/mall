import { describe, expect, it } from "vitest";

import { buildStockMovementRequest } from "./stockMovementRequest";

describe("buildStockMovementRequest", () => {
  it("builds an inbound stock movement request with default warehouse location", () => {
    expect(buildStockMovementRequest({ itemId: "item-1", quantity: 2 })).toEqual({
      path: "/api/v1/inventory/stock-movements/inbound",
      payload: {
        merchantId: undefined,
        venueId: undefined,
        itemId: "item-1",
        productId: undefined,
        reason: undefined,
        quantity: 2,
        locationType: "warehouse_stock",
      },
    });
  });

  it("builds transfer requests without changing path or location defaults", () => {
    expect(buildStockMovementRequest({ movementType: "transfer", itemId: "item-1", quantity: 3 })).toMatchObject({
      path: "/api/v1/inventory/stock-movements/transfer",
      payload: {
        itemId: "item-1",
        quantity: 3,
        fromLocationType: "warehouse_stock",
        toLocationType: "bar_stock",
      },
    });
  });

  it("builds count adjustment requests using counted quantity fallback", () => {
    expect(buildStockMovementRequest({ movementType: "count", itemId: "item-1", quantity: 4 })).toMatchObject({
      path: "/api/v1/inventory/stock-adjustments",
      payload: {
        itemId: "item-1",
        countedQuantity: 4,
        locationType: "bar_stock",
      },
    });
  });

  it("builds outbound requests with default bar location", () => {
    expect(buildStockMovementRequest({ movementType: "outbound", itemId: "item-1", quantity: 1 })).toMatchObject({
      path: "/api/v1/inventory/stock-movements/outbound",
      payload: {
        itemId: "item-1",
        quantity: 1,
        locationType: "bar_stock",
      },
    });
  });
});
