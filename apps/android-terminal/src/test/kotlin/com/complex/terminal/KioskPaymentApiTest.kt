package com.complex.terminal

// 文件版本：V0.04

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
    api.registerKioskPayment(KioskPayTarget.TicketOrder("ord_1"), 5000, "cash", staff, posNo = "POS-IGNORED", signOut = true, stepKey = KioskPaymentKey())

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
    api.registerKioskPayment(KioskPayTarget.BarBill("bill_1"), 12800, "pos", staff, posNo = "POS-0001", signOut = false, stepKey = KioskPaymentKey())

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
    api.registerKioskPayment(KioskPayTarget.Reservation("resv_1"), 20000, "cash", staff, posNo = null, signOut = true, stepKey = KioskPaymentKey())

    assertEquals(listOf("/api/v1/bar/reservations/resv_1/pay-deposit", "/api/v1/auth/logout"), requests.map { it.path })
    assertTrue(requests.all { it.authorization == "Bearer staff_tok" }, requests.toString())
    assertBody(requests[0].body, "\"idempotency_key\":", "\"channel\":\"cash\"")
    assertTrue(!requests[0].body.contains("pos_transaction_no"), "现金不带 POS 流水号：${requests[0].body}")
    assertEquals("resident", api.accessToken)
  }

  /** 第十八批 S 段第 169 条 169-R2（BSM:1066「POS（记流水号+收款人）」）：订台定金 POS 登记带上 POS 流水号。 */
  @Test
  fun reservationDepositPosSendsTheTransactionNo() = withServer { api, requests ->
    api.registerKioskPayment(KioskPayTarget.Reservation("resv_1"), 20000, "pos", staff, posNo = "POS-0003", signOut = true, stepKey = KioskPaymentKey())

    assertEquals("/api/v1/bar/reservations/resv_1/pay-deposit", requests[0].path)
    assertBody(requests[0].body, "\"channel\":\"pos\"", "\"pos_transaction_no\":\"POS-0003\"")
  }

  @Test
  fun ticketPaymentNotAcceptedByTheServerIsAFailureAndStillSignsOut() = withServer(confirmPayStatus = "unpaid") { api, requests ->
    assertFailsWith<IllegalStateException> {
      api.registerKioskPayment(KioskPayTarget.TicketOrder("ord_1"), 5000, "pos", staff, posNo = "POS-0002", signOut = true, stepKey = KioskPaymentKey())
    }

    assertEquals("/api/v1/auth/logout", requests.last().path)
    assertBody(requests[1].body, "\"gateway_ref\":\"POS-0002\"")
    assertEquals("resident", api.accessToken)
  }

  // ── 第十八批 AK 段第 261 条 261-B09-R1（PGM:303「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款、重复入账或重复核销……
  //    同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528 红线「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」）：
  //    酒吧账单现金、POS 登记没拿到结果再按，沿用收款步骤持有的原编号 ──

  /** [replies] 按次序回应每个请求：空表示收下请求后断开连接（没拿到结果），否则回给定的状态码与响应体。 */
  private fun withReplies(vararg replies: Pair<Int, String>?, block: (TerminalApiClient, List<Req>) -> Unit) {
    val requests = mutableListOf<Req>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += Req(exchange.requestURI.path, exchange.requestHeaders.getFirst("Authorization"),
        exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val reply = replies[requests.size - 1]
      if (reply == null) {
        exchange.close()
      } else {
        val bytes = reply.second.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(reply.first, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
      }
    }
    server.start()
    try {
      block(TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "resident"; terminalType = "kiosk" }, requests)
    } finally { server.stop(0) }
  }

  private fun field(body: String, name: String): String =
    Regex("\"$name\":\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: error("缺 $name：$body")

  private val settled = 200 to """{"success":true,"code":"OK","message":"success","data":{"id":"bill_1","settlement_status":"settled"}}"""
  private val replayedSettled = 200 to
    """{"success":true,"code":"OK","message":"该操作已完成（重复提交，返回首次结果）","data":{"id":"bill_1","settlement_status":"settled"},"replayed":true}"""

  @Test
  fun barBillWithoutAResultIsRegisteredAgainWithTheSameKeyAndTheReplayIsReportedAsCompleted() = withReplies(null, replayedSettled) { api, requests ->
    val step = KioskPaymentKey()
    assertFailsWith<java.io.IOException> {
      api.registerKioskPayment(KioskPayTarget.BarBill("bill_1"), 12800, "pos", staff, posNo = "POS-0001", signOut = false, stepKey = step)
    }
    assertEquals(null, step.completedNotice)

    api.registerKioskPayment(KioskPayTarget.BarBill("bill_1"), 12800, "pos", staff, posNo = "POS-0001", signOut = false, stepKey = step)

    assertEquals(listOf("/api/v1/bar/bills/bill_1/settle", "/api/v1/bar/bills/bill_1/settle"), requests.map { it.path })
    assertBody(requests[1].body, "\"settlement_amount_cent\":12800", "\"payment_channel\":\"pos\"", "\"pos_transaction_no\":\"POS-0001\"")
    assertEquals(field(requests[0].body, "idempotency_key"), field(requests[1].body, "idempotency_key"), "没拿到结果再按，沿用原请求编号")
    assertEquals(field(requests[0].body, "payment_id"), field(requests[1].body, "payment_id"), "付款凭据编号也沿用")
    assertEquals(field(requests[1].body, "idempotency_key"), field(requests[1].body, "payment_id"))
    assertEquals("该操作已完成（重复提交，返回首次结果）", step.completedNotice)
  }

  @Test
  fun barBillKeyIsKeptWhileTheResultIsUnknownAndRenewedAfterAnExplicitRejectionOrSuccess() = withReplies(
    503 to """{"success":false,"code":"INTERNAL_ERROR","message":"服务暂时不可用"}""",
    409 to """{"success":false,"code":"CONFLICT","detail_code":"IDEMPOTENCY_IN_PROGRESS","message":"请求正在处理中，请稍后刷新查看结果"}""",
    409 to """{"success":false,"code":"CONFLICT","detail_code":"BAR_BILL_ALREADY_SETTLED","message":"账单已结账，请勿重复操作"}""",
    settled,
    settled,
  ) { api, requests ->
    val step = KioskPaymentKey()
    val outcomes = (1..5).map {
      runCatching {
        api.registerKioskPayment(KioskPayTarget.BarBill("bill_1"), 12800, "cash", staff, posNo = null, signOut = false, stepKey = step)
      }
    }

    assertEquals(listOf(503, 409, 409, null, null), outcomes.map { (it.exceptionOrNull() as? ApiException)?.statusCode })
    assertTrue(outcomes[3].isSuccess && outcomes[4].isSuccess, outcomes.toString())
    val keys = requests.map { field(it.body, "idempotency_key") }
    assertEquals(keys[0], keys[1], "服务暂时不可用（5xx）后再按，沿用原编号")
    assertEquals(keys[1], keys[2], "请求正在处理中（409 IDEMPOTENCY_IN_PROGRESS）后再按，沿用原编号")
    assertTrue(keys[3] != keys[2], "被服务端明确拒绝（409 BAR_BILL_ALREADY_SETTLED）后再按，换新编号：$keys")
    assertTrue(keys[4] != keys[3], "拿到成功结果后再登记，换新编号：$keys")
    assertEquals(keys, requests.map { field(it.body, "payment_id") })
    assertEquals(null, step.completedNotice, "不是重复提交的成功结果不提示已完成")
  }

  @Test
  fun barBillKeyIsRenewedWhenTheBillAmountChannelOrPosNoChanges() = withReplies(null, null, null, null, null, null) { api, requests ->
    val step = KioskPaymentKey()
    fun press(billId: String, amountCent: Long, channel: String, posNo: String?) {
      assertFailsWith<java.io.IOException> {
        api.registerKioskPayment(KioskPayTarget.BarBill(billId), amountCent, channel, staff, posNo, signOut = false, stepKey = step)
      }
    }
    press("bill_1", 12800, "pos", "POS-0001")
    press("bill_1", 12800, "pos", "POS-0002")
    press("bill_1", 12800, "cash", null)
    press("bill_1", 12000, "cash", null)
    press("bill_2", 12000, "cash", null)
    press("bill_2", 12000, "cash", null)

    val keys = requests.map { field(it.body, "idempotency_key") }
    assertEquals(5, keys.distinct().size, "改了 POS 流水号、渠道、金额、账单后再按，各换新编号：$keys")
    assertEquals(keys[4], keys[5], "都没变时再按，沿用原编号")
  }
}
