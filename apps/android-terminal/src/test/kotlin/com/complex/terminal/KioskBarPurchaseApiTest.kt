package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
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

  @Test
  fun kioskBarOrderIsPlacedOnTheCounterThenBilledWithSnakeCaseKeys() = withServer { api, requests ->
    val bill = api.placeKioskBarOrder("bar_1", "counter_1",
      listOf(mapOf("product_id" to "prod_1", "quantity" to "2"), mapOf("combo_id" to "combo_1", "quantity" to "1")))

    assertEquals(listOf("/api/v1/bar/orders", "/api/v1/bar/orders/order_1/bill"), requests.map { it.path })
    assertTrue(requests.all { it.authorization == "Bearer resident" }, requests.toString())
    assertBody(requests[0].body, "\"idempotency_key\":", "\"bar_id\":\"bar_1\"", "\"table_id\":\"counter_1\"",
      "\"items\":[{\"product_id\":\"prod_1\",\"quantity\":\"2\"},{\"combo_id\":\"combo_1\",\"quantity\":\"1\"}]")
    assertBody(requests[1].body, "\"idempotency_key\":")
    assertEquals("bill_1", bill.id)
    assertEquals(13000L, bill.payableAmountCent - bill.paidAmountCent)
  }

  @Test
  fun barBillCashRegisteredOnTheKioskCarriesThePaymentReference() = withServer { api, requests ->
    val staff = LoginResponse(accessToken = "staff_tok", sessionId = "ses_staff", user = ApiUser(id = "usr_cashier"))
    api.registerKioskPayment(KioskPayTarget.BarBill("bill_1"), 13000, "cash", staff, posNo = null, signOut = true)

    assertEquals(listOf("/api/v1/bar/bills/bill_1/settle", "/api/v1/auth/logout"), requests.map { it.path })
    assertBody(requests[0].body, "\"payment_id\":", "\"settlement_amount_cent\":13000", "\"payment_channel\":\"cash\"",
      "\"cashier_staff_id\":\"usr_cashier\"")
    assertEquals("resident", api.accessToken)
  }
}
