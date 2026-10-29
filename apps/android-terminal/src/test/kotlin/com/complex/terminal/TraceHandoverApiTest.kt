package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 I 段第 84 条 84-R2（PGM 第 633 行「工作手机还负责酒水和物品交接扫码，记录出库、后厨接收、服务员取走和送达」）：
 * 工作手机交接页的五个请求——按溯源码查单（urlEncode）、查链、建链、逐段扫码、缺段补录——走后端交接链接口，请求体蛇形键。
 */
class TraceHandoverApiTest {
  @Test
  fun handoverCallsHitTheTraceEndpointsWithSnakeCaseBodies() {
    val requests = mutableListOf<Triple<String, String, String>>()
    val chain = """{"handover":{"id":"trace_x","trace_code":"TRC-1","overall_status":"in_progress","current_segment":"kitchen_receive"},""" +
      """"steps":[{"id":"trace_x_out_verify","segment":"out_verify","seq":1,"actor_type":"staff","operator_user_id":"usr_1","step_status":"done"}],""" +
      """"exceptions":[],"backfills":[]}"""
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val uri = exchange.requestURI.toString()
      requests += Triple(exchange.requestMethod, uri, exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val data = when {
        "?trace_code=" in uri -> """[{"id":"trace_x","trace_code":"TRC 1","overall_status":"in_progress","created_at":"2026-10-29T01:00:00Z"}]"""
        uri.endsWith("/backfill") -> """{"id":"mbf_1","target_part":"kitchen_receive","status":"pending","review_id":"rv_1","proposed":{"x":1}}"""
        else -> chain
      }
      val bytes = """{"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "staff" }

      assertEquals(listOf("trace_x"), api.listTraceHandovers("TRC 1").map { it.id })
      assertEquals("GET" to "/api/v1/inventory/trace-handovers?trace_code=TRC+1", requests.last().first to requests.last().second)

      assertEquals("done", api.getTraceHandover("trace_x").steps.single().stepStatus)
      assertEquals("GET" to "/api/v1/inventory/trace-handovers/trace_x", requests.last().first to requests.last().second)

      assertEquals("trace_x", api.createTraceHandover(TraceHandoverCreateRequest("k1", "TRC-1", "dev-1")).handover.id)
      assertEquals(Triple("POST", "/api/v1/inventory/trace-handovers", """{"idempotency_key":"k1","trace_code":"TRC-1","device_id":"dev-1"}"""),
        requests.last())

      api.scanTraceSegment("trace_x", TraceScanSegmentRequest("k2", "kitchen_receive"))
      assertEquals(Triple("POST", "/api/v1/inventory/trace-handovers/trace_x/scan-segment", """{"idempotency_key":"k2","segment":"kitchen_receive"}"""),
        requests.last())

      val row = api.submitTraceStepBackfill("trace_x", "kitchen_receive",
        TraceStepBackfillRequest("k3", "2026-10-29T01:00:00Z", "扫码枪没电，漏扫", "usr_1", listOf("att_1")))
      assertEquals(TraceHandoverBackfillRow("mbf_1", "kitchen_receive", "pending", "rv_1"), row)
      assertEquals(Triple("POST", "/api/v1/inventory/trace-handovers/trace_x/segments/kitchen_receive/backfill",
        """{"idempotency_key":"k3","occurred_at":"2026-10-29T01:00:00Z","reason":"扫码枪没电，漏扫","responsible_staff_id":"usr_1","evidence_attachment_ids":["att_1"]}"""),
        requests.last())
      assertEquals(5, requests.size)
    } finally { server.stop(0) }
  }
}
