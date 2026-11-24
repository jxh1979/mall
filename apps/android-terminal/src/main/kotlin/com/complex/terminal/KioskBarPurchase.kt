package com.complex.terminal

// 文件版本：V0.04

import java.util.UUID

/**
 * 第十八批 N 段第 120 条 120-R4（BSM:988「支持吧台买酒下单：选择酒水、套餐、数量，生成待支付订单。」；BSM:1024「接收一体机产生的吧台买酒订单。」）：
 * 一体机吧台买酒只在本机酒吧的吧台位（table_type=bar_counter）下单。
 * 第十八批 AA 段第 206 条 206-R3（ENUM:100「台位停用与恢复由店长及以上操作，填原因留痕」）：停用（disabled）的吧台位不给。
 * 第十八批 AK 段第 226 条 226-R3（ENUM:100「桌台/卡座/吧台位状态｜空闲、已预留、已开台、待清台、停用、异常」）：登记为异常（abnormal）的吧台位也不给。
 */
fun kioskBarCounters(rows: List<BarTableAvailabilityRow>, barId: String): List<BarTableAvailabilityRow> =
  rows.filter { it.barId == barId && it.tableType == "bar_counter" && it.status != "disabled" && it.status != "abnormal" }

/**
 * 120-R4：一体机吧台买酒按购物车在吧台位下单（`POST /api/v1/bar/orders`），再由订单生成待支付账单（`POST /api/v1/bar/orders/{order_id}/bill`），返回账单；
 * 用常驻会话。服务端按可信终端记一体机来源与设备号；收款走一体机收款步骤（[registerKioskPayment] 账单）。
 * 第十八批 BO 段第 261 条 261-B06-R1（PGM:303「同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作重复提交不重复生效……」）：
 * 下单与开账单算同一次操作，下单的请求编号从登记表 [keys] 取（槽 kiosk-bar-order，内容＝酒吧、吧台位、购物车）——两步都成功才清，
 * 哪一步没拿到结果再按，下单都沿用原编号、服务端回放同一张订单；开账单一步仍每次新编号（同一订单再开由服务端返回已开出的那张，222-R1）。
 * 下单一步是重复提交时，服务端的「已完成」提示记到 [TerminalOperationKeys.completedNotice]。
 */
fun TerminalApiClient.placeKioskBarOrder(barId: String, tableId: String, items: List<Map<String, String>>, keys: TerminalOperationKeys): BarBillRecord =
  keys.submit("kiosk-bar-order", listOf(barId, tableId, items), "kiosk-bar-order") { key ->
    val order = createBarOrder(BarOrderRequest(idempotencyKey = key, barId = barId, tableId = tableId, items = items))
    val notice = lastReplayedNotice.get()
    createBarOrderBill(order.id, BarOrderBillCreateRequest(idempotencyKey = UUID.randomUUID().toString())) to notice
  }
