package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 M 段第 112 条 112-R1（BSM:947「工作手机扫描权益二维码，核销赠饮、礼物或其他领取资格。」；BSM:1197「……出库须同时核验订单和物品码」）：
 * 工作手机礼物/券核销的两个请求——按客人出示的券码查券、带逐件扫的物品码核销——走后端券接口，请求体蛇形键。
 */
class VoucherRedeemApiTest {
  @Test
  fun voucherCallsHitTheVoucherEndpointsWithSnakeCaseBodies() {
    val requests = mutableListOf<Triple<String, String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val uri = exchange.requestURI.toString()
      requests += Triple(exchange.requestMethod, uri, exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val data = if (uri.endsWith("/preview")) {
        """{"voucher_instance_id":"vi_1","name":"礼品兑换券","voucher_kind":"in_store_goods","goods_quantity":2,""" +
          """"status":"issued","expires_at":"2026-12-01T00:00:00Z","block_reason":null}"""
      } else {
        """{"id":"vrr_vi_1","voucher_instance_id":"vi_1","voucher_kind":"in_store_goods"}"""
      }
      val bytes = """{"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "staff" }

      assertEquals(VoucherRedeemPreview("vi_1", "礼品兑换券", "in_store_goods", 2, "issued", "2026-12-01T00:00:00Z", null),
        api.previewVoucherRedeem("vrc_abc"))
      assertEquals(Triple("POST", "/api/v1/vouchers/redemptions/preview", """{"code":"vrc_abc"}"""), requests.last())

      assertEquals("vi_1", api.redeemVoucher(VoucherRedeemRequest("k1", "vrc_abc", listOf("TRC-1", "TRC-2"))).voucherInstanceId)
      assertEquals(Triple("POST", "/api/v1/vouchers/redemptions",
        """{"idempotency_key":"k1","code":"vrc_abc","trace_codes":["TRC-1","TRC-2"]}"""), requests.last())
      assertEquals(2, requests.size)
    } finally { server.stop(0) }
  }

  /** 物品码输入去空白去重（空格、逗号、换行分隔），个数再与每张券件数比。 */
  @Test
  fun scannedTraceCodesAreTrimmedAndDeduplicated() {
    assertEquals(listOf("TRC-1", "TRC-2"), scannedTraceCodes(" TRC-1, TRC-2 ，TRC-1\nTRC-2 "))
    assertEquals(emptyList(), scannedTraceCodes("  "))
  }
}
