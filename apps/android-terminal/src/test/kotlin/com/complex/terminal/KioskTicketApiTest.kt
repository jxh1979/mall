package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 第十八批 N 段第 120 条 120-R2（BSM:986「启用售票能力的一体机可由授权服务人员登录后辅助客人购票和查询订单（门票为动态二维码，不提供取票功能；
 * 客人用手机号短信验证绑定，在官方 App/小程序查看门票）……」；DEC-R7-05「售票型触摸屏一体机能力补「订单查询」」）：一体机辅助售票按所属酒吧取票种、
 * 给客人手机号发绑定码、按一体机渠道带验证码与售票原因下单、出票、取消，订单查询带客人手机号参数；请求按蛇形键发。
 */
class KioskTicketApiTest {
  private data class Req(val method: String, val path: String, val query: String?, val body: String)

  private fun withServer(block: (TerminalApiClient, List<Req>) -> Unit) {
    val requests = mutableListOf<Req>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val path = exchange.requestURI.path
      val method = exchange.requestMethod
      requests += Req(method, path, exchange.requestURI.rawQuery, exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val data = when {
        path == "/api/v1/customer/ticket-products" ->
          """{"items":[{"ticket_type_id":"tt_bar_a","ticket_name":"酒吧门票","weekday_price_cent":8000,"weekend_price_cent":9000,"holiday_price_cent":9900,"bar_id":"bar_a","refundable":true,"validity_days":1,"usage_scope":"A 酒吧"}]}"""
        path == "/api/v1/ticketing/kiosk-bind-codes" -> """{"phone_masked":"138****8000"}"""
        path == "/api/v1/ticketing/orders" && method == "POST" ->
          """{"id":"ord_1","order_no":"TO1","quantity":2,"amount_cent":16000,"buyer_phone":"138****8000","pay_status":"unpaid","issue_status":"not_issued","status":"created"}"""
        path == "/api/v1/ticketing/orders" ->
          """{"items":[{"id":"ord_1","order_no":"TO1","quantity":2,"amount_cent":16000,"buyer_phone":"138****8000","pay_status":"paid","issue_status":"issued","status":"created"}]}"""
        path.endsWith("/issue") -> """{"issued_entitlements":[{"entitlement_code":"E1"}]}"""
        else -> """{"id":"ord_1","status":"cancelled"}"""
      }
      val bytes = """{"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      block(TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "staff_tok"; terminalType = "kiosk" }, requests)
    } finally { server.stop(0) }
  }

  private fun assertBody(body: String, vararg parts: String) = parts.forEach { assertTrue(body.contains(it), "缺 $it：$body") }

  @Test
  fun kioskSaleOrdersByKioskChannelWithSmsCodeAndReasonAndLooksUpOrdersByPhone() = withServer { api, requests ->
    val product = api.listKioskTicketProducts("bar_a").single()
    api.requestKioskBindCode("13800138000")
    val order = api.createTicketOrder(KioskTicketOrderRequest("key_order", product.ticketTypeId, 2, "13800138000", "123456",
      "客人在酒吧门口补票", product.barId))
    api.issueTicketOrder(order.id, TicketOrderActionRequest("key_issue", "一体机辅助售票出票"))
    api.cancelTicketOrder(order.id, TicketOrderActionRequest("key_cancel"))
    val found = api.listTicketOrdersByPhone("13800138000")

    assertEquals(listOf("GET /api/v1/customer/ticket-products", "POST /api/v1/ticketing/kiosk-bind-codes", "POST /api/v1/ticketing/orders",
      "POST /api/v1/ticketing/orders/ord_1/issue", "POST /api/v1/ticketing/orders/ord_1/cancel", "GET /api/v1/ticketing/orders"),
      requests.map { "${it.method} ${it.path}" })
    assertEquals("bar_id=bar_a", requests[0].query)
    assertBody(requests[1].body, "\"phone\":\"13800138000\"")
    assertBody(requests[2].body, "\"idempotency_key\":\"key_order\"", "\"ticket_type_id\":\"tt_bar_a\"", "\"quantity\":2",
      "\"sales_channel\":\"kiosk\"", "\"buyer_phone\":\"13800138000\"", "\"sms_code\":\"123456\"", "\"operator_note\":\"客人在酒吧门口补票\"",
      "\"bar_id\":\"bar_a\"")
    assertBody(requests[3].body, "\"idempotency_key\":\"key_issue\"", "\"reason\":\"一体机辅助售票出票\"")
    assertBody(requests[4].body, "\"idempotency_key\":\"key_cancel\"")
    assertEquals("buyer_phone=13800138000", requests[5].query)
    assertEquals(9900L, product.holidayPriceCent)
    assertEquals("A 酒吧", product.usageScope)
    assertEquals(16000L, order.amountCent)
    assertEquals("paid", found.single().payStatus)
    assertEquals("issued", found.single().issueStatus)
  }

  @Test
  fun kioskWithoutOwnerMerchantListsTicketProductsWithoutBarFilter() = withServer { api, requests ->
    api.listKioskTicketProducts(null)

    assertEquals("/api/v1/customer/ticket-products", requests.single().path)
    assertEquals(null, requests.single().query)
  }
}
