package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AttendanceReceiptReviewApiTest {
  @Test
  fun missingContextRecoveryQueriesRealHistoryAndKeepsProposalOutsideOriginal() {
    val requests = mutableListOf<Triple<String,String,String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
    server.createContext("/") { exchange ->
      requests.add(Triple(exchange.requestMethod,exchange.requestURI.toString(),exchange.requestBody.readBytes().toString(Charsets.UTF_8)))
      val data = if (exchange.requestMethod == "GET") """[{"id":"old-context","device_id":"phone","user_id":"old-user","binding_at":"2026-10-18T00:00:00Z","closed_at":"2026-10-18T08:00:00Z"}]"""
        else """{"id":"original-event","status":"pending_review","user_id":"old-user","context_id":"old-context","reason":"SUPERVISOR_CONTEXT_RECOVERY_REVIEW_REQUIRED"}"""
      val bytes = """{"success":true,"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200,bytes.size.toLong());exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}")
      val context = api.historicalWorkPhoneContexts("old-user").single()
      assertEquals("/api/v1/staff/attendance/work-phone-contexts?user_id=old-user",requests[0].second)
      val original = AttendanceReceiptRequest("original-event","phone",null,null,"boot",15,"2000-01-01T00:00:00Z","charging_disconnect")
      val result = api.recoverAttendanceReceipt(AttendanceReceiptRecoveryRequest(original,"首次离线","原领用人与交班登记已核对",context.id))
      assertEquals("pending_review",result.status)
      val body = api.json.parseToJsonElement(requests[1].third).jsonObject
      assertEquals("old-context",body.getValue("proposed_context_id").jsonPrimitive.content)
      val captured = api.json.decodeFromJsonElement(AttendanceReceiptRequest.serializer(),body.getValue("original"))
      assertNull(captured.contextId);assertEquals(original,captured)
      assertFalse(body.containsKey("actual_at"));assertFalse(body.containsKey("user_id"))
    } finally { server.stop(0) }
  }

  @Test
  fun bindingLookupAndSupervisorRecoveryKeepOriginalPayloadSeparateFromEvidence() {
    val requests = mutableListOf<Triple<String,String,String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
    server.createContext("/") { exchange ->
      requests.add(Triple(exchange.requestMethod,exchange.requestURI.toString(),exchange.requestBody.readBytes().toString(Charsets.UTF_8)))
      val data = if (exchange.requestMethod == "GET") """{"id":"context1","device_id":"phone","user_id":"original-user","binding_at":"2026-10-18T00:00:00Z"}"""
        else """{"id":"original-event","status":"pending_review","user_id":"original-user","context_id":"context1"}"""
      val bytes = """{"success":true,"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200,bytes.size.toLong());exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}")
      assertEquals("2026-10-18T00:00:00Z",api.currentWorkPhoneContext().bindingAt)
      assertEquals("/api/v1/staff/attendance/work-phone-context",requests[0].second)
      val original = AttendanceReceiptRequest("original-event","phone","context1",null,"boot",15,"2026-10-18T00:00:01Z","charging_disconnect")
      val result = api.recoverAttendanceReceipt(AttendanceReceiptRecoveryRequest(original,"交班后代补报","原领用登记已核对"))
      assertEquals("pending_review",result.status)
      assertEquals("/api/v1/staff/attendance/receipt-events/recover",requests[1].second)
      val body = api.json.parseToJsonElement(requests[1].third).jsonObject
      val payload = body.getValue("original").jsonObject
      assertEquals("original-event",payload.getValue("event_id").jsonPrimitive.content)
      assertEquals("context1",payload.getValue("context_id").jsonPrimitive.content)
      assertEquals("2026-10-18T00:00:01Z",payload.getValue("observed_at").jsonPrimitive.content)
      assertFalse(body.containsKey("actual_at"));assertFalse(body.containsKey("user_id"))
      assertEquals("原领用登记已核对",body.getValue("handover_evidence").jsonPrimitive.content)
    } finally { server.stop(0) }
  }

  @Test
  fun reviewUsesOriginalEventAndEvidenceAndReadsHistoryWithoutChoosingApprover() {
    val requests = mutableListOf<Triple<String, String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val review = """{"id":"rr1","event_id":"event1","review_id":"approval1","status":"pending","shift_id":"shift1","actual_at":"2026-10-18T12:00:00Z","reason":"补证","handover_evidence":"登记和双方交接已核对"}"""
    server.createContext("/") { exchange ->
      requests.add(Triple(exchange.requestMethod, exchange.requestURI.toString(), exchange.requestBody.readBytes().toString(Charsets.UTF_8)))
      val data = if (exchange.requestMethod == "POST") review else "[$review]"
      val bytes = """{"success":true,"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}")
      val result = api.submitAttendanceReceiptReview("event1", AttendanceReceiptReviewRequest(
        "fixed-key", "shift1", "2026-10-18T12:00:00Z", "补证", "登记和双方交接已核对"))
      assertEquals("pending", result.status)
      assertEquals("approval1", result.reviewId)
      assertEquals("/api/v1/staff/attendance/receipt-events/event1/review", requests[0].second)
      val body = api.json.parseToJsonElement(requests[0].third).jsonObject
      assertEquals("fixed-key", body["idempotency_key"]?.jsonPrimitive?.content)
      assertEquals("登记和双方交接已核对", body["handover_evidence"]?.jsonPrimitive?.content)
      assertFalse(body.containsKey("approver_id"))
      assertEquals("rr1", api.attendanceReceiptReviewHistory("event1").single().id)
      assertEquals("/api/v1/staff/attendance/receipt-events/event1/reviews", requests[1].second)
    } finally { server.stop(0) }
  }
}
