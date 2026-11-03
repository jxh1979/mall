package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 N 段第 120 条 120-R1（BSM:980「……其中只有启用售票能力的一体机，才允许授权服务人员在到店服务场景下辅助客人购票。」）：
 * 一体机凭绑定时签发的设备令牌读本机配置——GET /api/v1/terminals/kiosks/{device_id}/config 带 X-Device-Token，读出是否启用售票与所属商户；
 * 绑定响应里的设备令牌与一体机模式按蛇形键收下。
 */
class KioskConfigApiTest {
  @Test
  fun kioskConfigSendsTheDeviceTokenAndReadsTicketingAndMerchant() {
    val requests = mutableListOf<Triple<String, String, String?>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += Triple(exchange.requestMethod, exchange.requestURI.toString(), exchange.requestHeaders.getFirst("X-Device-Token"))
      val bytes = ("""{"code":"OK","data":{"device_id":"KIOSK-1","kiosk_mode":"ticketing","ticketing_enabled":true,""" +
        """"bound_scope":"point_1","merchant_id":"bar_1","config":{},"updated_at":"2026-11-03T00:00:00Z"}}""").toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "resident"; terminalType = "kiosk" }

      assertEquals(KioskConfig("KIOSK-1", "ticketing", true, "point_1", "bar_1"), api.kioskConfig("KIOSK-1", "kiosk_tok"))
      assertEquals(Triple("GET", "/api/v1/terminals/kiosks/KIOSK-1/config", "kiosk_tok"), requests.single())
    } finally { server.stop(0) }
  }

  @Test
  fun deviceBindingKeepsTheKioskDeviceTokenAndMode() {
    val api = TerminalApiClient("http://127.0.0.1:1")
    val binding = api.json.decodeFromString(DeviceBinding.serializer(),
      """{"id":"dev_kiosk_1","device_code":"KIOSK-1","kiosk_mode":"ticketing","status":"bound","device_token":"kiosk_abc"}""")

    assertEquals("kiosk_abc", binding.deviceToken)
    assertEquals("ticketing", binding.kioskMode)
  }
}
