package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 第十八批 Q 段第 150 条 150-R2（BSM:552「进场之后，工作手机扫客人二维码核销各酒吧入场、赠饮、游乐等场内权益……」；
 * BSM:1036「……到店出示本人的动态入场码核验入场……」；ENUM:36「卡座免通票入场名额只授予所预定卡座对应的那一家酒吧的入场资格……」）：
 * 工作手机扫到邀请函成员入场码，按码中订台号送订台入场核验，酒吧取本机所属商户，请求体蛇形键。
 */
class InvitationEntryApiTest {
  private val code = "bie.reservation_abc.invm_1.1999999999.ab.sig"

  @Test
  fun entryCodeYieldsReservationIdOnlyForSixPartBieCodes() {
    assertEquals("reservation_abc", invitationEntryReservationId(code))
    assertNull(invitationEntryReservationId("E-TICKET-1"))
    assertNull(invitationEntryReservationId("bie.invm_1.1999999999.ab.sig"))
    assertNull(invitationEntryReservationId(""))
  }

  @Test
  fun workPhoneSendsEntryCodeToReservationEntryVerifyWithOwnBar() {
    withServer { api, requests ->
      val record = TerminalWorkflow(api, store(merchantId = "bar-1")).invitationEntryVerifyOnline(code)

      assertEquals("entered", record.status)
      assertEquals("reservation_abc", record.reservationId)
      val (method, uri, body) = requests.single()
      assertEquals("POST", method)
      assertEquals("/api/v1/bar/reservations/reservation_abc/invitation/entry-verify", uri)
      listOf(
        "\"idempotency_key\":\"invitation-entry-",
        "\"credential_payload\":\"$code\"",
        "\"device_id\":\"dev_1\"",
        "\"point_id\":\"pt-1\"",
        "\"business_context\":{\"bar_id\":\"bar-1\"}",
      ).forEach { assertTrue(it in body, body) }
    }
  }

  @Test
  fun workPhoneWithoutOwnBarSendsNothing() {
    withServer { api, requests ->
      val error = assertFailsWith<IllegalStateException> {
        TerminalWorkflow(api, store(merchantId = null)).invitationEntryVerifyOnline(code)
      }

      assertEquals("本机未设置所属酒吧，不能核邀请函入场", error.message)
      assertEquals(0, requests.size)
    }
  }

  private fun store(merchantId: String?) = InMemoryLocalStore().apply {
    saveSession(TerminalSession("staff", "s1", "operator"))
    saveBinding(DeviceBinding(id = "dev_1", deviceCode = "WP-1", merchantId = merchantId, locationPointId = "pt-1", status = "bound"))
  }

  private fun withServer(block: (TerminalApiClient, MutableList<Triple<String, String, String>>) -> Unit) {
    val requests = mutableListOf<Triple<String, String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += Triple(exchange.requestMethod, exchange.requestURI.toString(), exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val bytes = ("""{"code":"OK","data":{"id":"invm_1","reservation_id":"reservation_abc","bar_id":"bar-1",""" +
        """"status":"entered","entered_at":"2026-11-04T10:00:00Z"}}""").toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      block(TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "staff" }, requests)
    } finally { server.stop(0) }
  }
}
