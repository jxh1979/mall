package com.complex.terminal

// 文件版本：V0.01

import java.util.UUID

/**
 * 第十八批 N 段第 120 条 120-R4（BSM:988「支持吧台买酒下单：选择酒水、套餐、数量，生成待支付订单。」；BSM:1024「接收一体机产生的吧台买酒订单。」）：
 * 一体机吧台买酒只在本机酒吧的吧台位（table_type=bar_counter）下单。
 */
fun kioskBarCounters(rows: List<BarTableAvailabilityRow>, barId: String): List<BarTableAvailabilityRow> =
  rows.filter { it.barId == barId && it.tableType == "bar_counter" }

/**
 * 120-R4：一体机吧台买酒按购物车在吧台位下单（`POST /api/v1/bar/orders`），再由订单生成待支付账单（`POST /api/v1/bar/orders/{order_id}/bill`），返回账单；
 * 用常驻会话，每次提交新幂等键。服务端按可信终端记一体机来源与设备号；收款走一体机收款步骤（[registerKioskPayment] 账单）。
 */
fun TerminalApiClient.placeKioskBarOrder(barId: String, tableId: String, items: List<Map<String, String>>): BarBillRecord {
  val order = createBarOrder(BarOrderRequest(idempotencyKey = UUID.randomUUID().toString(), barId = barId, tableId = tableId, items = items))
  return createBarOrderBill(order.id, BarOrderBillCreateRequest(idempotencyKey = UUID.randomUUID().toString()))
}
