package com.complex.terminal

// 文件版本：V0.02

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十二批操作员裁决第 13 条「员工手机「我的工单」」：我的工单一并列出「待我提单确认」的单（本人是提单确认承接人、未确认、
 * 待验收或已验收），带「确认」「退回返工」，分别打既有 /review 与 /rework（蛇形键、带幂等键；退回返工带不通过原因）。
 */
class MyWorkOrderConfirmApiTest {
  @Test
  fun myWorkOrdersMarkPendingMyConfirmationAndPostConfirmOrRework() {
    val requests = mutableListOf<Triple<String, String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += Triple(exchange.requestMethod, exchange.requestURI.toString(), exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val assigned = """{"id":"fwo-1","code":"FWO-1","status":"pending_acceptance","assignee_id":"usr_me","confirmer_id":"usr_x"}"""
      val toConfirm = """{"id":"fwo-2","code":"FWO-2","status":"accepted","assignee_id":"usr_eng","confirmer_id":"usr_me"}"""
      // 设备责任人本人即维修人（BSM L1385「本人负责此次维修时，由物业主管指定其他人员」）：作为派给我的单列出，不标待我提单确认。
      val selfRepaired = """{"id":"fwo-3","code":"FWO-3","status":"accepted","assignee_id":"usr_me","confirmer_id":"usr_me"}"""
      val payload = if (exchange.requestMethod == "GET") """{"items":[$assigned,$toConfirm,$selfRepaired],"page":1,"page_size":200,"total":3}"""
        else toConfirm.replace("accepted", if (exchange.requestURI.path.endsWith("/rework")) "processing" else "closed")
      val bytes = """{"code":"OK","data":$payload}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "staff" }
      val mine = api.listMyWorkOrders()
      assertEquals(listOf("fwo-1", "fwo-2", "fwo-3"), mine.map { it.id })
      // 第十八批 W 段第 192 条 192-R2（PGM:287）：物业工单状态显示中文。
      assertEquals("待验收", facilityWorkOrderStatusZh(mine.first().status))
      assertEquals(listOf("fwo-2"), mine.filter { it.awaitsConfirmationBy("usr_me") }.map { it.id })
      assertEquals(listOf("fwo-1"), mine.filter { it.awaitsConfirmationBy("usr_x") }.map { it.id })

      assertEquals("closed", api.confirmWorkOrder("fwo-2", WorkOrderConfirmationRequest(idempotencyKey = "k-c1")).status)
      assertEquals("POST" to "/api/v1/facility/work-orders/fwo-2/review", requests.last().first to requests.last().second)
      assertEquals("""{"idempotency_key":"k-c1"}""", requests.last().third)

      assertEquals("processing", api.reworkWorkOrder("fwo-2", WorkOrderConfirmationRequest(idempotencyKey = "k-r1", note = "仍漏水")).status)
      assertEquals("POST" to "/api/v1/facility/work-orders/fwo-2/rework", requests.last().first to requests.last().second)
      assertEquals("""{"idempotency_key":"k-r1","note":"仍漏水"}""", requests.last().third)
    } finally { server.stop(0) }
  }
}
