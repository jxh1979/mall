package com.complex.terminal

// 文件版本：V0.03

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 第十八批 M 段第 112 条 112-R2（BSM:985「扫码查看客人当前可领取的赠饮、礼物、积分兑换权益和领取状态。」；BSM:1948「……需要扫码后在一体机上
 * 清楚看到能领什么、能领多少、是否已经领取，并把领取结果回写到权益记录。」；BSM:1004「……领取……都要记录设备、点位和时间。」）：
 * 一体机权益领取屏扫到的码分两路——券动态码走查券与核销，其余按门票权益走核销预览与核销（设备＝本机设备台账行、点位＝本机绑定点位）。
 */
class KioskClaimTest {
  // 第十八批 AH 段 217-R1（ENUM:36「餐饮券、零售券」；PGM:287「展示给用户的一切文字……必须全部为中文」）：后台组合配置可建零售券，领取屏显示中文。
  @Test
  fun retailEntitlementTypeShowsChinese() {
    assertEquals("零售", entitlementTypeZh("retail"))
  }

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

  // ── 第十八批 BO 段第 261 条 261-B06-R1（PGM:303「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款、重复入账或重复核销……
  //    同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」）：
  //    一体机领取门票权益与券核销按内容沿用请求编号（领取前须先查看、码不变） ──

  private data class Req(val path: String, val body: String)

  /** [replies] 按次序回应每个请求：空表示收下请求后断开连接（没拿到结果），否则回给定的状态码与响应体。 */
  private fun withReplies(vararg replies: Pair<Int, String>?, block: (TerminalApiClient, List<Req>) -> Unit) {
    val requests = mutableListOf<Req>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += Req(exchange.requestURI.path, exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val reply = replies[requests.size - 1]
      if (reply == null) {
        exchange.close()
      } else {
        val bytes = reply.second.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(reply.first, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
      }
    }
    server.start()
    try {
      block(TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "resident"; terminalType = "kiosk" }, requests)
    } finally { server.stop(0) }
  }

  private fun key(body: String): String =
    Regex("\"idempotency_key\":\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: error("缺 idempotency_key：$body")

  private val claimed = 200 to """{"code":"OK","message":"success","data":{"id":"ver_1","verification_no":"V1","status":"success"}}"""
  private val replayedClaim =
    200 to """{"code":"OK","message":"该操作已完成（重复提交，返回首次结果）","data":{"id":"ver_1","verification_no":"V1","status":"success"},"replayed":true}"""

  @Test
  fun kioskEntitlementClaimWithoutAResultIsClaimedAgainWithTheSameKeyAndTheReplayIsReportedAsCompleted() =
    withReplies(null, replayedClaim, claimed) { api, requests ->
      val keys = TerminalOperationKeys()
      assertFailsWith<java.io.IOException> { api.claimKioskEntitlement("ENT-1", "dev_kiosk_1", "space_1", "usr_1", keys) }

      assertEquals("success", api.claimKioskEntitlement("ENT-1", "dev_kiosk_1", "space_1", "usr_1", keys).status)
      assertEquals("该操作已完成（重复提交，返回首次结果）", keys.completedNotice)
      api.claimKioskEntitlement("ENT-1", "dev_kiosk_1", "space_1", "usr_1", keys)

      assertEquals(List(3) { "/api/v1/ticketing/verifications" }, requests.map { it.path })
      val first = api.json.parseToJsonElement(requests[0].body).jsonObject
      val second = api.json.parseToJsonElement(requests[1].body).jsonObject
      listOf("idempotency_key", "credential_payload", "device_id", "point_id", "business_context").forEach {
        assertTrue(it in second, "缺 $it：$second")
        assertEquals(first[it], second[it], "没拿到结果再领，$it 相同")
      }
      assertTrue(key(requests[2].body) != key(requests[1].body), "领取成功后同一个码再领，换新编号")
      assertNull(keys.completedNotice)
    }

  @Test
  fun kioskEntitlementClaimRejectedIsClaimedAgainWithANewKey() = withReplies(
    409 to """{"code":"CONFLICT","detail_code":"ENTITLEMENT_EXPIRED","message":"权益已过期"}""",
    claimed,
  ) { api, requests ->
    val keys = TerminalOperationKeys()
    val error = assertFailsWith<ApiException> { api.claimKioskEntitlement("ENT-1", "dev_kiosk_1", "space_1", "usr_1", keys) }
    assertEquals(409, error.statusCode)

    api.claimKioskEntitlement("ENT-1", "dev_kiosk_1", "space_1", "usr_1", keys)

    assertTrue(key(requests[1].body) != key(requests[0].body), "被明确拒绝后再领，换新编号")
  }

  // 18BO 控制席判定（第 261 条补充②）：工作手机与一体机的券核销同写法——先查券、再点核销，码不变，按内容沿用请求编号。
  @Test
  fun voucherRedeemWithoutAResultIsRedeemedAgainWithTheSameKeyAndTheReplayIsReportedAsCompleted() = withReplies(
    null,
    200 to """{"code":"OK","message":"该操作已完成（重复提交，返回首次结果）","data":{"id":"vrr_vi_1","voucher_instance_id":"vi_1"},"replayed":true}""",
    200 to """{"code":"OK","message":"success","data":{"id":"vrr_vi_1","voucher_instance_id":"vi_1"}}""",
  ) { api, requests ->
    val keys = TerminalOperationKeys()
    assertFailsWith<java.io.IOException> { api.redeemVoucherOnce("vrc_1", listOf("TRC-1"), keys) }

    assertEquals("vrr_vi_1", api.redeemVoucherOnce("vrc_1", listOf("TRC-1"), keys).id)
    assertEquals("该操作已完成（重复提交，返回首次结果）", keys.completedNotice)
    api.redeemVoucherOnce("vrc_1", listOf("TRC-1"), keys)

    assertEquals(List(3) { "/api/v1/vouchers/redemptions" }, requests.map { it.path })
    assertEquals(requests[0].body, requests[1].body, "没拿到结果再核销，编号、券码、物品码都相同")
    assertTrue(key(requests[2].body) != key(requests[1].body), "核销成功后再核销，换新编号")
  }
}
