import type { BarOrderRow } from "../types";

// 销售归属口径（镜像后端 BarBillingService：订单有 salesStaffId=销售归属，提成 1500bp/15%；
// 无 salesStaffId=非销售，1000bp/10%）。前端销售/非销售拆分统计一律据此推导。
// 历史缺陷：曾读 order.sellerRole / order.commissionRate，二者是后端 /api/v1/bar/orders
// （直接序列化 BarOrderRecord）从不返回的幽灵字段，导致「销售 15%」拆分恒为 0 单、数字误导。
export function isSalesOrder(order: Pick<BarOrderRow, "salesStaffId"> | undefined): boolean {
  return Boolean(order?.salesStaffId && order.salesStaffId.trim() !== "");
}
