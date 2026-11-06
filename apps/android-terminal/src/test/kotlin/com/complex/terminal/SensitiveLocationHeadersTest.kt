package com.complex.terminal

// 文件版本：V0.02

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpRequest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 N 段第 123 条 123-R4（BSM:1630「工作手机、平板、一体机绑定点位后，系统自动知道“在哪”……绑定点位的工作手机、平板、一体机
 * 离开授权范围后同样只保留查看，不得执行上述操作。」）：现场动作闭集的 POST 带上当下位置 X-Geo-Latitude、X-Geo-Longitude；
 * 查券、核销预检与 GET 不取位置也不带；取不到位置时不带位置头、请求照常发出（由服务端按范围外拒绝，第十八批裁决 30）。
 */
class SensitiveLocationHeadersTest {
  private var fixes = 0
  private val api = TerminalApiClient("http://127.0.0.1:1").apply {
    deviceId = "dev-1"
    terminalType = "kiosk"
    deviceSigner = { "sig" }
    locationProvider = { fixes++; 31.2304 to 121.4737 }
  }

  private fun geo(path: String, method: String = "POST"): List<String?> {
    val builder = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:1$path"))
    if (method == "POST") builder.POST(HttpRequest.BodyPublishers.ofString("{}")) else builder.GET()
    api.applyHeaders(builder, path, method, "{}")
    val headers = builder.build().headers()
    return listOf("X-Geo-Latitude", "X-Geo-Longitude").map { headers.firstValue(it).orElse(null) }
  }

  @Test
  fun sensitivePostsCarryCurrentLocation() {
    for (path in listOf(
      "/api/v1/ticketing/verifications",
      "/api/v1/vouchers/redemptions",
      "/api/v1/amusement/verifications",
      "/api/v1/services/orders/so_1/mark-paid",
      "/api/v1/bar/bills/bill_1/settle",
      "/api/v1/ticketing/orders/to_1/confirm-payment",
      "/api/v1/bar/reservations/br_1/pay-deposit",
    )) {
      assertEquals(listOf("31.2304", "121.4737"), geo(path), path)
    }
  }

  @Test
  fun previewsAndReadsNeitherAskForNorCarryLocation() {
    assertEquals(listOf(null, null), geo("/api/v1/ticketing/verifications/preview"))
    assertEquals(listOf(null, null), geo("/api/v1/vouchers/redemptions/preview"))
    assertEquals(listOf(null, null), geo("/api/v1/ticketing/verifications", method = "GET"))
    assertEquals(listOf(null, null), geo("/api/v1/evidence/events?limit=20", method = "GET"))
    assertEquals(0, fixes)
  }

  @Test
  fun noFixStillSendsTheRequestWithoutLocation() {
    val seen = mutableListOf<List<String?>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      seen += listOf("X-Geo-Latitude", "X-Geo-Longitude").map { exchange.requestHeaders.getFirst(it) }
      val bytes = """{"code":"OK","data":{}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val client = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply {
        accessToken = "staff"
        locationProvider = { null }
      }
      client.post<JsonObject, JsonObject>("/api/v1/ticketing/verifications", JsonObject(emptyMap()))
      assertEquals(listOf(listOf<String?>(null, null)), seen)
    } finally { server.stop(0) }
  }

  /** 123-R4 复核（plan ④「被拒时照常走 friendly 显示后端中文提示」；第十八批裁决 30「拒绝并留痕，只能查看；拿到位置后再办」）：
   *  定位闸的两种 403 给现场人员看服务端的中文原因，不套「无权操作」；其余 403 不变。 */
  @Test
  fun locationGateDenialShowsTheServerReasonInsteadOfNoPermission() {
    fun denied(path: String, detail: String, message: String) =
      ApiException(403, path, """{"code":"FORBIDDEN","detail_code":"$detail","message":"$message"}""")
    assertEquals("未取得本设备定位，不能执行核销、收款、退款、放行，只能查看（403）",
      apiErrorText(denied("/api/v1/vouchers/redemptions", "SENSITIVE_OPERATION_LOCATION_REQUIRED", "未取得本设备定位，不能执行核销、收款、退款、放行，只能查看")))
    assertEquals("操作设备不在本项目授权范围内，只能查看（403）",
      apiErrorText(denied("/api/v1/bar/bills/bill_1/settle", "SENSITIVE_OPERATION_OUT_OF_RANGE", "操作设备不在本项目授权范围内，只能查看")))
    assertEquals("当前账号或设备无权操作（403）",
      apiErrorText(denied("/api/v1/vouchers/redemptions", "PERMISSION_DENIED", "无权限")))
  }
}
