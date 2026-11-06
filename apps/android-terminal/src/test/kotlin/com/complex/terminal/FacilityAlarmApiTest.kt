package com.complex.terminal

// 文件版本：V0.02

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** HP-F01（BSM「楼宇运营」告警条 L1417）：员工手机「我的接警」只读拉取，点「已收到并接手」才打明确接手接口（蛇形键、带幂等键）。 */
class FacilityAlarmApiTest {
  @Test
  fun myAlarmsAreReadOnlyAndTakeOverPostsIdempotencyKeyToTakeOverEndpoint() {
    val requests = mutableListOf<Triple<String, String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += Triple(exchange.requestMethod, exchange.requestURI.toString(), exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val alarm = """{"id":"falm-1","equipment_id":"eq-1","alarm_type":"elevator","priority":"urgent","status":"awaiting_takeover","response_due_at":"2026-10-21T10:00:00Z","uncovered":true}"""
      val payload = if (exchange.requestMethod == "GET") """{"items":[$alarm],"page":1,"page_size":500,"total":1}"""
        else alarm.replace("awaiting_takeover", "taken_over")
      val bytes = """{"code":"OK","data":$payload}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "staff" }
      val mine = api.listMyFacilityAlarms()
      assertEquals(listOf("falm-1"), mine.map { it.id })
      assertEquals("awaiting_takeover", mine.single().status)
      // 第十八批 W 段第 192 条 192-R1（PGM:287）：响应时限按本机时区显示，不出现 ISO 的英文字母。
      val due = localTimeZh(mine.single().responseDueAt)
      assertFalse(Regex("[A-Za-z]").containsMatchIn(due), due)
      // 192-R2：优先级显示中文。
      assertEquals("紧急", workPriorityZh(mine.single().priority))
      assertEquals(listOf("GET" to "/api/v1/facility/alarms/mine"), requests.map { it.first to it.second })

      val taken = api.takeOverFacilityAlarm("falm-1", FacilityAlarmTakeOverRequest(idempotencyKey = "k-take-1", note = "已到场"))
      assertEquals("taken_over", taken.status)
      assertEquals("POST" to "/api/v1/facility/alarms/falm-1/take-over", requests.last().first to requests.last().second)
      assertEquals("""{"idempotency_key":"k-take-1","note":"已到场"}""", requests.last().third)
    } finally { server.stop(0) }
  }
}
