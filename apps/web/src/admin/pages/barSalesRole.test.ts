import { describe, expect, it } from "vitest";

import { commissionRateLabel, isSalesOrder, salesRoleLabel } from "./barSalesRole";
import type { BarOrderRow } from "../types";

describe("isSalesOrder（提成档口径，G/V349 按员工岗位判、读账单落库的 salesRoleType）", () => {
  it("账单角色档 sales → 销售档", () => {
    expect(isSalesOrder({ bills: [{ id: "bill-1", salesRoleType: "sales" }] })).toBe(true);
  });

  it("账单角色档 non_sales → 非销售档", () => {
    expect(isSalesOrder({ bills: [{ id: "bill-1", salesRoleType: "non_sales" }] })).toBe(false);
  });

  it("挂了销售归属人但该员工不是销售岗（角色档 non_sales）→ 仍是非销售档", () => {
    // 本批修的病根：改前前端按「有没有 salesStaffId」判，这单会被误算成销售 15%。
    const order: BarOrderRow = { id: "ord-1", salesStaffId: "staff_1", bills: [{ id: "bill-1", salesRoleType: "non_sales" }] };
    expect(isSalesOrder(order)).toBe(false);
  });

  it("没有账单 / undefined 订单 → 非销售档（不猜）", () => {
    expect(isSalesOrder({})).toBe(false);
    expect(isSalesOrder(undefined)).toBe(false);
  });
});

describe("commissionRateLabel（计提比例只显示台账真实落库值）", () => {
  it("落库 1500bp → 15%", () => {
    expect(commissionRateLabel({ bills: [{ id: "b", commissionRateBp: 1500 }] })).toBe("15%");
  });

  it("运营把费率改成 2000bp → 页面跟着显示 20%（不再写死 15%）", () => {
    expect(commissionRateLabel({ bills: [{ id: "b", commissionRateBp: 2000 }] })).toBe("20%");
  });

  it("非整数百分比 1250bp → 12.5%", () => {
    expect(commissionRateLabel({ bills: [{ id: "b", commissionRateBp: 1250 }] })).toBe("12.5%");
  });

  it("尚未计提 → 未计提（不编数字）", () => {
    expect(commissionRateLabel({ bills: [{ id: "b" }] })).toBe("未计提");
    expect(commissionRateLabel({})).toBe("未计提");
  });
});

describe("salesRoleLabel（销售角色展示名）", () => {
  it("sales → 销售人员；non_sales → 非销售人员；缺角色档 → 占位符", () => {
    expect(salesRoleLabel({ bills: [{ id: "b", salesRoleType: "sales" }] })).toBe("销售人员");
    expect(salesRoleLabel({ bills: [{ id: "b", salesRoleType: "non_sales" }] })).toBe("非销售人员");
    expect(salesRoleLabel({ bills: [{ id: "b" }] })).toBe("—");
  });
});
