package com.complex.terminal

// 文件版本：V0.03

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class PayrollApiTest {
  @Test
  fun detailAndScoreAppealUseEmployeeEndpointsAndKeepUnsettledAmountsUnknown() {
    val requests = mutableListOf<Pair<String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val path = exchange.requestURI.toString()
      requests += path to exchange.requestBody.readBytes().toString(Charsets.UTF_8)
      val detail = """{"statement":{"id":"pay-1","month":"2099-10","total_cent":480000,"tax_cent":0},"status":"partial_paid","statement_kind":"monthly","scheme_version_id":"scheme-1","facts_id":"facts-1","payment_due_date":"2099-11-05","paid_amount_cent":200000,"lines":[{"id":"line-1","component_code":"performance","component_type":"performance","description":"绩效","gross_cent":100000,"embedded_leave_cent":20000,"additional_leave_cent":0,"amount_cent":80000,"source_type":"facts","source_id":"facts-1"}],"corrections":[{"id":"c-1","original_statement_id":"old-1","original_month":"2099-09","amount_cent":null,"status":"needs_review"}],"payments":[{"id":"p-1","finance_payment_id":"finance-1","amount_cent":200000,"pay_method":"bank_transfer","pay_ref":"bank-1","paid_at":"2099-11-05T08:00:00Z"}],"pending_items":[{"code":"CYCLE","source_id":"cycle-1","message":"工时周期未结束"}]}"""
      val score = """{"id":"score-1","user_id":"me","month":"2099-10","points":20,"rule_code":"LATE_1","reason":"已核实违规","status":"approved"}"""
      val payload = if (path.contains("/statements/")) detail else if (path.endsWith("/appeals")) score.replace("approved", "pending_review") else "[$score]"
      val bytes = """{"code":"OK","data":$payload}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "mine" }
      val detail = api.myPayrollDetail("pay-1")
      assertEquals(200000, detail.paidAmountCent)
      assertEquals(null, detail.corrections.single().amountCent)
      val text = payrollDetailText(detail)
      assertContains(text, "应发净额：¥4800.00")
      assertContains(text, "实际已付：¥2000.00")
      assertContains(text, "公式内已扣事假 ¥200.00；另扣事假 ¥0.00")
      assertContains(text, "补差 c-1：—（待复核）")
      assertContains(text, "待处理：工时周期未结束")
      assertContains(text, "2099年11月05日")
      assertEquals("score-1", api.myPerformanceAdjustments("2099-10").single().id)
      assertEquals("pending_review", api.appealPerformance("score-1", "已报备", "appeal-1").status)
      assertEquals(listOf("/api/v1/staff/payroll/mine/statements/pay-1", "/api/v1/staff/payroll/mine/performance-adjustments?month=2099-10", "/api/v1/staff/payroll/performance-adjustments/score-1/appeals"), requests.map { it.first })
      assertEquals("""{"idempotency_key":"appeal-1","reason":"已报备"}""", requests.last().second)
      api.appealPerformance("score-1", "已报备", "appeal-1")
      assertEquals(requests[2], requests[3], "结果未明时重试原申请须保留幂等键与内容")
      assertFalse(requests.any { it.first.contains("user_id") })
    } finally { server.stop(0) }
  }

  @Test
  fun cashPenaltyIsListedSeparatelyFromReducedPerformanceWithoutDeductingAgain() {
    val detail = PayrollStatementDetail(
      statement = PayrollStatementRow(id = "h01a", month = "2099-10", baseCent = 500000,
        penaltyCent = 100000, taxCent = 0, totalCent = 480000),
      status = "closed", statementKind = "monthly", schemeVersionId = "scheme-1", factsId = "facts-1",
      paymentDueDate = "2099-11-05", arApItemId = "ap-1", paidAmountCent = 0,
      lines = listOf(
        PayrollLine("base", "base", "base", "基础工资", 500000, 0, 0, 500000, "scheme", "scheme-1"),
        PayrollLine("performance", "performance", "performance", "绩效工资", 80000, 0, 0, 80000, "period_facts", "facts-1"),
      ),
      worktime = null, corrections = emptyList(), payments = emptyList(), pendingItems = emptyList(),
    )
    val text = payrollDetailText(detail)
    assertContains(text, "基础工资：¥5000.00")
    assertContains(text, "绩效工资：¥800.00")
    assertContains(text, "现金处罚扣款：¥1000.00")
    assertContains(text, "代扣个税：¥0.00")
    assertContains(text, "应发净额：¥4800.00")
    assertContains(text, "现金处罚和个税已计入应发净额")
    assertFalse(text.contains("¥3800.00"))
  }

  @Test
  fun deniedStatementDoesNotReturnAnEmptyOrPaidFallback() {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val bytes = """{"code":"FORBIDDEN","message":"仅可查看本人"}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(403, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}")
      assertEquals(403, assertFailsWith<ApiException> { api.myPayrollDetail("other") }.statusCode)
    } finally { server.stop(0) }
  }

  @Test
  fun myPayrollReadsEveryStatementFromPageWithoutSupplyingEmployeeIdentity() {
    var requestedPath = ""
    var authorization = ""
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requestedPath = exchange.requestURI.toString()
      authorization = exchange.requestHeaders.getFirst("Authorization")
      val body = """{"success":true,"code":"OK","data":{"items":[{"id":"monthly","month":"2099-10","total_cent":480000},{"id":"correction","month":"2099-10","total_cent":30000}],"page":1,"page_size":20,"total":2}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, body.size.toLong())
      exchange.responseBody.use { it.write(body) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}")
      api.accessToken = "employee-token"
      val rows = api.listMyPayroll("2099-10")
      assertEquals(listOf("monthly", "correction"), rows.map { it.id })
      assertEquals(listOf(480000L, 30000L), rows.map { it.totalCent })
      assertEquals("/api/v1/staff/payroll/mine?month=2099-10", requestedPath)
      assertEquals("Bearer employee-token", authorization)
      assertFalse(requestedPath.contains("user_id"))
    } finally { server.stop(0) }
  }

  @Test
  fun requestLeaveRevokePostsReasonWithIdempotencyKeyToEmployeeEndpoint() {
    // 2026-10-18 裁决：员工本人对已批准的假申请销假——走 /leaves/{id}/revoke-request，理由必填、带幂等键，不接受员工身份参数。
    val requests = mutableListOf<Pair<String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += exchange.requestURI.toString() to exchange.requestBody.readBytes().toString(Charsets.UTF_8)
      val bytes = """{"code":"OK","data":{"id":"lv-1","leave_type":"personal","start_date":"2099-09-10","end_date":"2099-09-11","status":"revoke_requested","review_note":"准","revoke_requested_by":"me","revoke_request_reason":"已到岗"}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "mine" }
      val row = api.requestLeaveRevoke("lv-1", "已到岗", "revoke-1")
      assertEquals("revoke_requested", row.status)
      assertEquals("已到岗", row.revokeRequestReason)
      assertEquals("待销假审批", leaveStatusLabel(row.status))
      assertEquals(listOf("/api/v1/staff/leaves/lv-1/revoke-request"), requests.map { it.first })
      assertEquals("""{"idempotency_key":"revoke-1","reason":"已到岗"}""", requests.single().second)
      assertFalse(requests.any { it.first.contains("user_id") })
    } finally { server.stop(0) }
  }

  @Test
  fun leaveStatusLabelsAreChineseIncludingRevokeRequestAndRevoked() {
    // 2026-10-18 裁决：销假改为「本人申请＋主管批准」——员工申请后为 revoke_requested（待销假审批），主管批准后转 revoked（已销假）；
    // 员工端不得直出英文原值。
    assertEquals(listOf("待审", "已通过", "已驳回", "待销假审批", "已销假"), listOf("pending", "approved", "rejected", "revoke_requested", "revoked").map(::leaveStatusLabel))
  }
}
