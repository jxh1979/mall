package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 第十八批 N 段第 120 条 120-R3（BSM:987「支持订台下单：选择酒吧、日期、时段、卡座/桌台/吧台位、人数和订台备注；订台需要支付定金。」；
 * BSM:1058「……客人自助订台支付定金成功……该台位即进入已预留……冲突时先支付定金者得……」）：一体机订台屏按蛇形键下单（带订台备注、不带销售员），
 * 读回应付定金；收定金登记后读回是否已保留台位。
 */
class KioskReservationApiTest {
  private data class Req(val path: String, val authorization: String?, val body: String)

  private fun withServer(holdStatus: String, block: (TerminalApiClient, List<Req>) -> Unit) {
    val requests = mutableListOf<Req>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val path = exchange.requestURI.path
      requests += Req(path, exchange.requestHeaders.getFirst("Authorization"), exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val data = when {
        path == "/api/v1/bar/reservations" ->
          """{"id":"resv_1","status":"reserved","deposit_status":"pending","deposit_amount_cent":20000,"order_source":"kiosk"}"""
        path.endsWith("/pay-deposit") ->
          """{"id":"resv_1","status":"${if (holdStatus == "held") "reserved" else "cancelled"}","deposit_status":"paid","hold_status":"$holdStatus"}"""
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

  @Test
  fun kioskReservationSendsSnakeCaseWithRemarkAndReadsDepositThenHoldAfterDepositRegistered() = withServer("held") { api, requests ->
    val created = api.createBarReservation(BarReservationCreateRequest(
      idempotencyKey = "kiosk-resv-1", barId = "bar_1", tableId = "table_1", reservationTime = "2026-11-03T20:00:00+08:00",
      guestCount = 4, latestArrivalAt = "2026-11-03T22:00:00+08:00", customerPhone = "13800000000", remark = "靠舞台，生日布置"))

    assertEquals("/api/v1/bar/reservations", requests[0].path)
    assertEquals("Bearer resident", requests[0].authorization)
    listOf("\"idempotency_key\":\"kiosk-resv-1\"", "\"bar_id\":\"bar_1\"", "\"table_id\":\"table_1\"",
      "\"reservation_time\":\"2026-11-03T20:00:00+08:00\"", "\"latest_arrival_at\":\"2026-11-03T22:00:00+08:00\"", "\"guest_count\":4",
      "\"customer_phone\":\"13800000000\"", "\"remark\":\"靠舞台，生日布置\"").forEach {
      assertTrue(requests[0].body.contains(it), "缺 $it：${requests[0].body}")
    }
    assertTrue(!requests[0].body.contains("salesperson_user_id"), "一体机订台不带销售员：${requests[0].body}")
    assertEquals(20000L, created.depositAmountCent)

    val staff = LoginResponse(accessToken = "staff_tok", sessionId = "ses_staff", user = ApiUser(id = "usr_cashier"))
    val paid: Any? = api.registerKioskPayment(KioskPayTarget.Reservation("resv_1"), 20000, "cash", staff, posNo = null, signOut = true)
    assertEquals("held", (paid as? BarReservationRecord)?.holdStatus)
    assertEquals("/api/v1/bar/reservations/resv_1/pay-deposit", requests[1].path)
  }

  @Test
  fun depositRegisteredAfterAnotherPaidFirstReadsBackCancelled() = withServer("") { api, _ ->
    val staff = LoginResponse(accessToken = "staff_tok", sessionId = "ses_staff", user = ApiUser(id = "usr_cashier"))
    val paid: Any? = api.registerKioskPayment(KioskPayTarget.Reservation("resv_1"), 20000, "cash", staff, posNo = null, signOut = true)
    assertEquals("cancelled", (paid as? BarReservationRecord)?.status)
  }
}
