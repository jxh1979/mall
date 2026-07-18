package com.complex.terminal

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class P0FixtureTest {
  @Test
  fun loginRequestUsesOpenApiLoginNameField() {
    var loginBody: String? = null
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    server.createContext("/") { exchange ->
      loginBody = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
      sendJson(
        exchange,
        """
        {
          "access_token": "token_1",
          "refresh_token": "refresh_1",
          "session_id": "sess_1",
          "token_type": "Bearer",
          "user": {"id": "staff_1", "username": "p0_admin"}
        }
        """.trimIndent(),
      )
    }

    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}")

      api.login("p0_admin", "secret")

      val request = Json.parseToJsonElement(assertNotNull(loginBody)).jsonObject
      assertTrue("login_name" in request)
      assertFalse("username" in request)
      assertEquals("p0_admin", request["login_name"]?.jsonPrimitive?.content)
    } finally {
      server.stop(0)
    }
  }

  @Test
  fun createAccessUserRequestIncludesIdempotencyMetadata() {
    val stamp = 123456789L
    val mutationBodies = mutableListOf<Pair<String, String>>()
    var accessUserBody: String? = null
    var spaceRequestCount = 0
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    server.createContext("/") { exchange ->
      val body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
      if (exchange.requestMethod == "POST") {
        mutationBodies.add(exchange.requestURI.path to body)
      }
      val response = when {
        exchange.requestMethod != "POST" -> """{"error":"unexpected method"}"""
        exchange.requestURI.path == "/api/v1/platform/business-entities" -> """{"id":"org_1"}"""
        exchange.requestURI.path == "/api/v1/platform/merchants" -> """{"id":"merchant_1"}"""
        exchange.requestURI.path == "/api/v1/platform/projects" -> """{"id":"venue_1"}"""
        exchange.requestURI.path == "/api/v1/platform/spaces" -> {
          spaceRequestCount += 1
          if (spaceRequestCount == 1) """{"id":"area_1"}""" else """{"id":"point_1"}"""
        }
        exchange.requestURI.path == "/api/v1/access/users" -> {
          accessUserBody = body
          """{"id":"staff_1","username":"p0_android_staff_$stamp"}"""
        }
        exchange.requestURI.path == "/api/v1/ticketing/ticket-types" -> """{"id":"ticket_type_1"}"""
        exchange.requestURI.path == "/api/v1/ticketing/entitlement-templates" -> """{"id":"template_1"}"""
        exchange.requestURI.path == "/api/v1/ticketing/ticket-types/ticket_type_1/entitlements" -> """{}"""
        exchange.requestURI.path == "/api/v1/ticketing/ticket-types/ticket_type_1/publish" -> """{}"""
        exchange.requestURI.path == "/api/v1/ticketing/orders" -> """{"id":"order_1"}"""
        exchange.requestURI.path == "/api/v1/ticketing/orders/order_1/issue" ->
          """{"issued_entitlements":[{"entitlement_code":"ENT-1"}]}"""
        else -> """{"error":"unexpected path","path":"${exchange.requestURI.path}"}"""
      }
      sendJson(exchange, response)
    }

    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}")
      P0Fixture(api, stamp).create()

      assertEquals(12, mutationBodies.size)
      for ((path, body) in mutationBodies) {
        val request = Json.parseToJsonElement(body).jsonObject
        assertTrue("idempotency_key" in request, "$path fixture request must include idempotency_key")
        assertTrue("operator_note" in request, "$path fixture request must include operator_note")
      }

      val request = Json.parseToJsonElement(assertNotNull(accessUserBody)).jsonObject
      assertEquals("android_p0_access_user_$stamp", request["idempotency_key"]?.jsonPrimitive?.content)
      assertEquals("Android P0 fixture setup", request["operator_note"]?.jsonPrimitive?.content)

      val requestsByPath = mutationBodies.groupBy(
        keySelector = { it.first },
        valueTransform = { Json.parseToJsonElement(it.second).jsonObject },
      )
      val businessEntityRequest = assertNotNull(requestsByPath["/api/v1/platform/business-entities"]?.single())
      assertTrue("entity_name" in businessEntityRequest)
      assertTrue("entity_role" in businessEntityRequest)

      val merchantRequest = assertNotNull(requestsByPath["/api/v1/platform/merchants"]?.single())
      assertTrue("merchant_name" in merchantRequest)
      assertTrue("merchant_type" in merchantRequest)
      assertTrue("business_entity_id" in merchantRequest)

      val projectRequest = assertNotNull(requestsByPath["/api/v1/platform/projects"]?.single())
      assertTrue("name" in projectRequest)
      assertTrue("venue_type" in projectRequest)

      val spaceRequests = assertNotNull(requestsByPath["/api/v1/platform/spaces"])
      assertEquals(2, spaceRequests.size)
      for (spaceRequest in spaceRequests) {
        assertTrue("space_name" in spaceRequest)
        assertTrue("space_type" in spaceRequest)
      }

      val ticketTypeRequest = assertNotNull(requestsByPath["/api/v1/ticketing/ticket-types"]?.single())
      assertTrue("ticket_name" in ticketTypeRequest)
      assertTrue("base_calendar_price_mode" in ticketTypeRequest)
      assertTrue("weekday_price_cent" in ticketTypeRequest)
      assertFalse("productName" in ticketTypeRequest)
      assertFalse("priceAmount" in ticketTypeRequest)

      val entitlementTemplateRequest =
        assertNotNull(requestsByPath["/api/v1/ticketing/entitlement-templates"]?.single())
      assertTrue("entitlement_type" in entitlementTemplateRequest)
      assertTrue("default_total_times" in entitlementTemplateRequest)
      assertTrue("default_value_cent" in entitlementTemplateRequest)
      assertFalse("entitlementType" in entitlementTemplateRequest)
      assertFalse("defaultValueAmount" in entitlementTemplateRequest)

      val ticketTypeEntitlementRequest =
        assertNotNull(requestsByPath["/api/v1/ticketing/ticket-types/ticket_type_1/entitlements"]?.single())
      assertTrue("entitlement_template_id" in ticketTypeEntitlementRequest)
      assertTrue("total_times" in ticketTypeEntitlementRequest)
      assertTrue("value_amount_cent" in ticketTypeEntitlementRequest)
      assertFalse("entitlementTemplateId" in ticketTypeEntitlementRequest)
      assertFalse("valueAmount" in ticketTypeEntitlementRequest)

      // AUD-019：direct 出票已退役——夹具走真实订单链（发布→下单→按单出票）。
      val orderRequest =
        assertNotNull(requestsByPath["/api/v1/ticketing/orders"]?.single())
      assertTrue("ticket_type_id" in orderRequest)
      assertTrue("sales_channel" in orderRequest)
      val issueRequest =
        assertNotNull(requestsByPath["/api/v1/ticketing/orders/order_1/issue"]?.single())
      assertTrue("reason" in issueRequest)
      assertFalse("purchaserName" in issueRequest)
    } finally {
      server.stop(0)
    }
  }

  private fun sendJson(exchange: HttpExchange, body: String) {
    val bytes = body.toByteArray(StandardCharsets.UTF_8)
    exchange.responseHeaders.add("content-type", "application/json")
    exchange.sendResponseHeaders(200, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
  }
}
