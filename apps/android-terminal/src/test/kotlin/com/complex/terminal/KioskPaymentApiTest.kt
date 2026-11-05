package com.complex.terminal

// 文件版本：V0.02

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 第十八批 N 段第 120 条 120-R7（BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」）：
 * 一体机收款步骤按票务单、酒吧账单、订台三种单登记现金 / POS——请求带授权员工令牌、按蛇形键发，登记完还回常驻令牌；
 * 本步自己请来的授权员工登记完（含出错）以其令牌登出。
 */
class KioskPaymentApiTest {
  private data class Req(val path: String, val authorization: String?, val body: String)

  private val staff = LoginResponse(accessToken = "staff_tok", sessionId = "ses_staff", user = ApiUser(id = "usr_cashier"))

  private fun withServer(confirmPayStatus: String = "paid", block: (TerminalApiClient, List<Req>) -> Unit) {
    val requests = mutableListOf<Req>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val path = exchange.requestURI.path
      requests += Req(path, exchange.requestHeaders.getFirst("Authorization"), exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val data = when {
        path.endsWith("/pay-intents") -> """{"id":"pay_intent_1","status":"created","channel":"cash"}"""
        path.endsWith("/confirm-payment") -> """{"id":"ord_1","pay_status":"$confirmPayStatus"}"""
        path.endsWith("/settle") -> """{"id":"bill_1","settlement_status":"settled"}"""
        path.endsWith("/pay-deposit") -> """{"id":"resv_1","deposit_status":"paid"}"""
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
  fun ticketOrderCashIsRegisteredByTheSignedInStaffThenSignedOut() = withServer { api, requests ->
    api.registerKioskPayment(KioskPayTarget.TicketOrder("ord_1"), 5000, "cash", staff, posNo = "POS-IGNORED", signOut = true)

    assertEquals(listOf("/api/v1/ticketing/orders/ord_1/pay-intents", "/api/v1/ticketing/orders/ord_1/confirm-payment", "/api/v1/auth/logout"),
      requests.map { it.path })
    assertTrue(requests.all { it.authorization == "Bearer staff_tok" }, requests.toString())
    assertBody(requests[0].body, "\"idempotency_key\":", "\"business_order_type\":\"ticket_order\"", "\"business_order_id\":\"ord_1\"",
      "\"amount_cent\":5000", "\"currency\":\"CNY\"", "\"channel\":\"cash\"")
    assertBody(requests[1].body, "\"idempotency_key\":", "\"payment_intent_id\":\"pay_intent_1\"")
    assertTrue(!requests[1].body.contains("gateway_ref"), "现金不带 POS 单号：${requests[1].body}")
    assertBody(requests[2].body, "\"session_id\":\"ses_staff\"")
    assertEquals("resident", api.accessToken)
  }

  @Test
  fun barBillPosIsSettledWithTheStaffAsCashierAndKeepsAPassedInStaffSignedIn() = withServer { api, requests ->
    api.registerKioskPayment(KioskPayTarget.BarBill("bill_1"), 12800, "pos", staff, posNo = "POS-0001", signOut = false)

    assertEquals(listOf("/api/v1/bar/bills/bill_1/settle"), requests.map { it.path })
    assertEquals("Bearer staff_tok", requests.single().authorization)
    assertBody(requests.single().body, "\"idempotency_key\":", "\"settlement_amount_cent\":12800", "\"payment_channel\":\"pos\"",
      "\"payment_id\":", "\"pos_transaction_no\":\"POS-0001\"")
    // 第十八批 Q 段第 153 条 153-R2（BSM:940「……一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕」）：收款人由服务端按本员工会话定，结账体不发 cashier_staff_id。
    assertTrue(!requests.single().body.contains("cashier_staff_id"), "不发 cashier_staff_id：${requests.single().body}")
    assertEquals("resident", api.accessToken)
  }

  @Test
  fun reservationDepositCashOnlySendsTheChannelThenSignsOut() = withServer { api, requests ->
    api.registerKioskPayment(KioskPayTarget.Reservation("resv_1"), 20000, "cash", staff, posNo = null, signOut = true)

    assertEquals(listOf("/api/v1/bar/reservations/resv_1/pay-deposit", "/api/v1/auth/logout"), requests.map { it.path })
    assertTrue(requests.all { it.authorization == "Bearer staff_tok" }, requests.toString())
    assertBody(requests[0].body, "\"idempotency_key\":", "\"channel\":\"cash\"")
    assertEquals("resident", api.accessToken)
  }

  @Test
  fun ticketPaymentNotAcceptedByTheServerIsAFailureAndStillSignsOut() = withServer(confirmPayStatus = "unpaid") { api, requests ->
    assertFailsWith<IllegalStateException> {
      api.registerKioskPayment(KioskPayTarget.TicketOrder("ord_1"), 5000, "pos", staff, posNo = "POS-0002", signOut = true)
    }

    assertEquals("/api/v1/auth/logout", requests.last().path)
    assertBody(requests[1].body, "\"gateway_ref\":\"POS-0002\"")
    assertEquals("resident", api.accessToken)
  }
}
