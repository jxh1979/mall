import type { BarBillRow, BarOrderRow } from "../types";

// 提成角色档与计提比例的展示口径（G/V349）。
//
// 后端在开单时按【销售归属人的员工岗位】（员工档案的「销售岗」标识）把角色档快照进账单
// bar_bills.sales_role_type；计提时按该档去结算配置中心取费率，落进提成台账 commission_rate_bp。
// 前端一律只读这两个真实落库值，自己不算、不猜、不写死百分比。
//
// 历史缺陷两轮，都是「前端自己猜」惹的祸：
//   ① 曾读 order.sellerRole / order.commissionRate —— 后端从不返回的幽灵字段，销售拆分恒为 0 单；
//   ② 改读 salesStaffId 有无 —— 那是「这单有没有归属人」，不是「这人是不是销售岗」；且页面把
//      15%/10% 写死在文案里：运营在结算配置中心把费率改成 20%，页面照旧显示 15%，对账对不上。

/** 该单最新账单（后端 BarOrderView.bills 已按 created_at 倒序，最新在前）。 */
export function latestBill(order: Pick<BarOrderRow, "bills"> | undefined): BarBillRow | undefined {
  return order?.bills?.[0];
}

/** 是否按销售人员费率计提 = 账单上快照的角色档为 sales（即归属人在员工档案里被标了销售岗）。 */
export function isSalesOrder(order: Pick<BarOrderRow, "bills"> | undefined): boolean {
  return latestBill(order)?.salesRoleType === "sales";
}

/** 销售角色展示名；账单未落角色档时显示占位符，不猜。 */
export function salesRoleLabel(order: Pick<BarOrderRow, "bills"> | undefined): string {
  const roleType = latestBill(order)?.salesRoleType;
  if (roleType === "sales") return "销售人员";
  if (roleType === "non_sales") return "非销售人员";
  return "—";
}

/**
 * 计提比例展示值 = 提成台账里的真实落库费率（万分比 → 百分比）。
 * 未计提的账单显示「未计提」——页面宁可说「还没算」，也不编一个数字让人以为算过了。
 */
export function commissionRateLabel(order: Pick<BarOrderRow, "bills"> | undefined): string {
  const rateBp = latestBill(order)?.commissionRateBp;
  if (rateBp === undefined || rateBp === null) return "未计提";
  return `${Number((rateBp / 100).toFixed(2))}%`;
}
