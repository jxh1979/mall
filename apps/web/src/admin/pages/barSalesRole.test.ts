import { describe, expect, it } from "vitest";

import { isSalesOrder } from "./barSalesRole";

describe("isSalesOrder（销售归属口径，镜像后端 salesStaffId 有无）", () => {
  it("有 salesStaffId → 销售（15% 口径）", () => {
    expect(isSalesOrder({ salesStaffId: "staff_sales_1" })).toBe(true);
  });

  it("无 salesStaffId → 非销售（10% 口径）", () => {
    expect(isSalesOrder({})).toBe(false);
  });

  it("空串 / 仅空白 salesStaffId → 非销售（不误判为销售）", () => {
    expect(isSalesOrder({ salesStaffId: "" })).toBe(false);
    expect(isSalesOrder({ salesStaffId: "   " })).toBe(false);
  });

  it("undefined 订单 → 非销售", () => {
    expect(isSalesOrder(undefined)).toBe(false);
  });
});
