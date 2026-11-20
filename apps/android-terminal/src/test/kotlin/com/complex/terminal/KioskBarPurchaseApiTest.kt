package com.complex.terminal

// 文件版本：V0.05

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 第十八批 N 段第 120 条 120-R4（BSM:988「支持吧台买酒下单：选择酒水、套餐、数量，生成待支付订单。」；BSM:1024「接收一体机产生的吧台买酒订单。」；
 * BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」）：一体机吧台买酒只取本机酒吧的吧台位；常驻会话按蛇形键下单、
 * 再由订单生成待支付账单；收款步骤登记账单现金时带结账必填的付款凭据编号。
 */
class KioskBarPurchaseApiTest {
  private data class Req(val path: String, val authorization: String?, val body: String)

  private fun withServer(block: (TerminalApiClient, List<Req>) -> Unit) {
    val requests = mutableListOf<Req>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val path = exchange.requestURI.path
      requests += Req(path, exchange.requestHeaders.getFirst("Authorization"), exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val data = when {
        path == "/api/v1/bar/orders" ->
          """{"id":"order_1","bar_id":"bar_1","table_id":"counter_1","order_source":"kiosk","status":"created","total_amount_cent":13000,"created_terminal":"kiosk"}"""
        path.endsWith("/bill") ->
          """{"id":"bill_1","order_id":"order_1","payable_amount_cent":13000,"paid_amount_cent":0,"payment_status":"unpaid"}"""
        path.endsWith("/settle") -> """{"id":"bill_1","payment_status":"paid","settlement_status":"settled"}"""
        else -> """{"session_id":"ses_staff","status":"logged_out"}"""
      }
      val bytes = """{"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      block(TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "resident"; terminalType = "kiosk" }, requests)
    } finally { server.stop(0) }
  }

  private fun assertBody(body: String, vararg parts: String) = parts.forEach { assertTrue(body.contains(it), "缺 $it：$body") }

  @Test
  fun onlyThisBarsBarCounterSeatsAreOffered() {
    val rows = listOf(
      BarTableAvailabilityRow(id = "counter_1", barId = "bar_1", tableType = "bar_counter"),
      BarTableAvailabilityRow(id = "card_1", barId = "bar_1", tableType = "card_seat"),
      BarTableAvailabilityRow(id = "counter_9", barId = "bar_9", tableType = "bar_counter"),
      BarTableAvailabilityRow(id = "counter_2", barId = "bar_1", tableType = "bar_counter"),
    )
    assertEquals(listOf("counter_1", "counter_2"), kioskBarCounters(rows, "bar_1").map { it.id })
    assertEquals(emptyList(), kioskBarCounters(rows, "bar_2").map { it.id })
  }

  // 第十八批 AA 段第 206 条 206-R3（ENUM:100「桌台/卡座/吧台位状态｜空闲、已预留、已开台、待清台、停用、异常」；「台位停用与恢复由店长及以上操作，填原因留痕」）：
  // 停用的吧台位不给一体机买酒（只有一个吧台位时会直接进点单，停用的每单 409）。
  @Test
  fun disabledBarCounterSeatIsNotOffered() {
    val rows = listOf(
      BarTableAvailabilityRow(id = "counter_1", barId = "bar_1", tableType = "bar_counter", status = "active"),
      BarTableAvailabilityRow(id = "counter_2", barId = "bar_1", tableType = "bar_counter", status = "disabled"),
    )
    assertEquals(listOf("counter_1"), kioskBarCounters(rows, "bar_1").map { it.id })
  }

  // 第十八批 AK 段第 226 条 226-R3（ENUM:100「桌台/卡座/吧台位状态｜空闲、已预留、已开台、待清台、停用、异常」；
  // 「台位未被占用——既无有效预留也未开台——时即可再次接受预订」）：登记为异常的吧台位不给一体机买酒。
  @Test
  fun abnormalBarCounterSeatIsNotOffered() {
    val rows = listOf(
      BarTableAvailabilityRow(id = "counter_1", barId = "bar_1", tableType = "bar_counter", status = "abnormal"),
      BarTableAvailabilityRow(id = "counter_2", barId = "bar_1", tableType = "bar_counter", status = "active"),
    )
    assertEquals(listOf("counter_2"), kioskBarCounters(rows, "bar_1").map { it.id })
  }

  @Test
  fun kioskBarOrderIsPlacedOnTheCounterThenBilledWithSnakeCaseKeys() = withServer { api, requests ->
    val bill = api.placeKioskBarOrder("bar_1", "counter_1",
      listOf(mapOf("product_id" to "prod_1", "quantity" to "2"), mapOf("combo_id" to "combo_1", "quantity" to "1")), TerminalOperationKeys())

    assertEquals(listOf("/api/v1/bar/orders", "/api/v1/bar/orders/order_1/bill"), requests.map { it.path })
    assertTrue(requests.all { it.authorization == "Bearer resident" }, requests.toString())
    assertBody(requests[0].body, "\"idempotency_key\":", "\"bar_id\":\"bar_1\"", "\"table_id\":\"counter_1\"",
      "\"items\":[{\"product_id\":\"prod_1\",\"quantity\":\"2\"},{\"combo_id\":\"combo_1\",\"quantity\":\"1\"}]")
    assertBody(requests[1].body, "\"idempotency_key\":")
    assertEquals("bill_1", bill.id)
    assertEquals(13000L, bill.payableAmountCent - bill.paidAmountCent)
  }

  // ── 第十八批 BO 段第 261 条 261-B06-R1（PGM:303「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款、重复入账或重复核销……
  //    同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」）：
  //    一体机买酒下单与开账单算同一次操作，下单的请求编号沿用到两步都成功 ──

  /** [replies] 按次序回应每个请求：空表示收下请求后断开连接（没拿到结果），否则回 200 与给定的响应体。 */
  private fun withReplies(vararg replies: String?, block: (TerminalApiClient, List<Req>) -> Unit) {
    val requests = mutableListOf<Req>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += Req(exchange.requestURI.path, exchange.requestHeaders.getFirst("Authorization"),
        exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val reply = replies[requests.size - 1]
      if (reply == null) {
        exchange.close()
      } else {
        val bytes = reply.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
      }
    }
    server.start()
    try {
      block(TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "resident"; terminalType = "kiosk" }, requests)
    } finally { server.stop(0) }
  }

  private fun key(body: String): String =
    Regex("\"idempotency_key\":\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: error("缺 idempotency_key：$body")

  private val order = """{"code":"OK","message":"success","data":{"id":"order_1","bar_id":"bar_1","table_id":"counter_1","status":"created"}}"""
  private val replayedOrder =
    """{"code":"OK","message":"该操作已完成（重复提交，返回首次结果）","data":{"id":"order_1","bar_id":"bar_1","table_id":"counter_1","status":"created"},"replayed":true}"""
  private val bill = """{"code":"OK","message":"success","data":{"id":"bill_1","order_id":"order_1","payable_amount_cent":13000,"paid_amount_cent":0}}"""
  private val cart = listOf(mapOf("product_id" to "prod_1", "quantity" to "2"))

  @Test
  fun kioskBarOrderWithoutAResultIsPlacedAgainWithTheSameKey() = withReplies(null, order, bill) { api, requests ->
    val keys = TerminalOperationKeys()
    assertFailsWith<java.io.IOException> { api.placeKioskBarOrder("bar_1", "counter_1", cart, keys) }

    api.placeKioskBarOrder("bar_1", "counter_1", cart, keys)

    assertEquals(listOf("/api/v1/bar/orders", "/api/v1/bar/orders", "/api/v1/bar/orders/order_1/bill"), requests.map { it.path })
    assertEquals(requests[0].body, requests[1].body, "没拿到结果再按，沿用原编号，酒吧、吧台位、购物车相同")
    assertBody(requests[1].body, "\"bar_id\":\"bar_1\"", "\"table_id\":\"counter_1\"", "\"items\":[{\"product_id\":\"prod_1\",\"quantity\":\"2\"}]")
  }

  @Test
  fun kioskBarOrderPlacedButNotBilledKeepsTheOrderKeyAndTheReplayIsReportedAsCompleted() =
    withReplies(order, null, replayedOrder, bill, order, bill) { api, requests ->
      val keys = TerminalOperationKeys()
      assertFailsWith<java.io.IOException> { api.placeKioskBarOrder("bar_1", "counter_1", cart, keys) }

      assertEquals("bill_1", api.placeKioskBarOrder("bar_1", "counter_1", cart, keys).id)
      assertEquals("该操作已完成（重复提交，返回首次结果）", keys.completedNotice, "下单一步是重复提交，提示已完成")
      api.placeKioskBarOrder("bar_1", "counter_1", cart, keys)

      assertEquals(listOf("/api/v1/bar/orders", "/api/v1/bar/orders/order_1/bill", "/api/v1/bar/orders", "/api/v1/bar/orders/order_1/bill",
        "/api/v1/bar/orders", "/api/v1/bar/orders/order_1/bill"), requests.map { it.path })
      assertEquals(key(requests[0].body), key(requests[2].body), "下单成了、开账单没收到响应，再按下单仍带原编号")
      assertTrue(key(requests[4].body) != key(requests[2].body), "整段成功后同样的购物车再下单，换新编号")
      assertNull(keys.completedNotice, "不是重复提交的成功结果不提示已完成")
    }

  @Test
  fun barBillCashRegisteredOnTheKioskCarriesThePaymentReference() = withServer { api, requests ->
    val staff = LoginResponse(accessToken = "staff_tok", sessionId = "ses_staff", user = ApiUser(id = "usr_cashier"))
    api.registerKioskPayment(KioskPayTarget.BarBill("bill_1"), 13000, "cash", staff, posNo = null, signOut = true, stepKey = KioskPaymentKey())

    assertEquals(listOf("/api/v1/bar/bills/bill_1/settle", "/api/v1/auth/logout"), requests.map { it.path })
    assertBody(requests[0].body, "\"payment_id\":", "\"settlement_amount_cent\":13000", "\"payment_channel\":\"cash\"")
    // 第十八批 Q 段第 153 条 153-R2（BSM:940「……一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕」）：收款人由服务端按本员工会话定，结账体不发 cashier_staff_id。
    assertEquals("Bearer staff_tok", requests[0].authorization)
    assertTrue(!requests[0].body.contains("cashier_staff_id"), "不发 cashier_staff_id：${requests[0].body}")
    assertEquals("resident", api.accessToken)
  }
}
