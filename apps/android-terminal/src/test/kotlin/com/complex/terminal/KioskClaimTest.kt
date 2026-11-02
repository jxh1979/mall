package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 M 段第 112 条 112-R2（BSM:985「扫码查看客人当前可领取的赠饮、礼物、积分兑换权益和领取状态。」；BSM:1948「……需要扫码后在一体机上
 * 清楚看到能领什么、能领多少、是否已经领取，并把领取结果回写到权益记录。」；BSM:1004「……领取……都要记录设备、点位和时间。」）：
 * 一体机权益领取屏扫到的码分两路——券动态码走查券与核销，其余按门票权益走核销预览与核销（设备＝本机设备台账行、点位＝本机绑定点位）。
 */
class KioskClaimTest {
  @Test
  fun voucherDynamicCodesGoToVoucherAndOtherCodesToEntitlement() {
    assertEquals(ClaimTarget.VOUCHER, claimTarget("vrc_abc"))
    assertEquals(ClaimTarget.VOUCHER, claimTarget("  vrc_abc "))
    assertEquals(ClaimTarget.ENTITLEMENT, claimTarget("gift-code-1"))
    assertEquals(ClaimTarget.ENTITLEMENT, claimTarget("VC123"))
  }

  @Test
  fun kioskClaimsHitVoucherAndTicketVerificationEndpoints() {
    val requests = mutableListOf<Triple<String, String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val path = exchange.requestURI.path
      requests += Triple(exchange.requestMethod, path, exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val data = when (path) {
        "/api/v1/vouchers/redemptions/preview" -> """{"voucher_instance_id":"vi_1","voucher_kind":"in_store_goods","goods_quantity":1,"status":"issued"}"""
        "/api/v1/vouchers/redemptions" -> """{"id":"vrr_vi_1","voucher_instance_id":"vi_1","voucher_kind":"in_store_goods"}"""
        "/api/v1/ticketing/verifications/preview" ->
          """{"allowed":true,"reason":"allowed","entitlement":{"entitlement_type":"gift_drink","total_times":2,"used_times":1,"status":"active"}}"""
        else -> """{"id":"ver_1","verification_no":"V1","status":"success"}"""
      }
      val bytes = """{"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "staff"; terminalType = "kiosk" }

      assertEquals(1, api.previewVoucherRedeem("vrc_1").goodsQuantity)
      api.redeemVoucher(VoucherRedeemRequest("k1", "vrc_1", listOf("TRC-1")))
      assertEquals(listOf("/api/v1/vouchers/redemptions/preview", "/api/v1/vouchers/redemptions"), requests.map { it.second })
      val redeemBody = api.json.parseToJsonElement(requests[1].third).jsonObject
      assertEquals(setOf("idempotency_key", "code", "trace_codes"), redeemBody.keys)

      val preview = api.preview(kioskEntitlementRequest("ENT-1", "dev_kiosk_1", "space_1", "k2", "usr_1"))
      assertEquals(1, preview.entitlement!!.totalTimes - preview.entitlement!!.usedTimes)
      assertEquals("success", api.verify(kioskEntitlementRequest("ENT-1", "dev_kiosk_1", "space_1", "k3", "usr_1")).status)
      assertEquals(listOf("/api/v1/ticketing/verifications/preview", "/api/v1/ticketing/verifications"), requests.drop(2).map { it.second })
      for ((index, key) in listOf(2 to "k2", 3 to "k3")) {
        val body = api.json.parseToJsonElement(requests[index].third).jsonObject
        assertEquals(key, body["idempotency_key"]?.jsonPrimitive?.content)
        assertEquals("ENT-1", body["credential_payload"]?.jsonPrimitive?.content)
        assertEquals("dev_kiosk_1", body["device_id"]?.jsonPrimitive?.content)
        assertEquals("space_1", body["point_id"]?.jsonPrimitive?.content)
      }
    } finally { server.stop(0) }
  }
}
