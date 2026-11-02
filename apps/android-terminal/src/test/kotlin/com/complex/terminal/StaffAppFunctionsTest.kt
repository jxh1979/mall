package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 第十八批 M 段第 116 条（PGM:747「……能做什么由后台按设备类型统一下发，员工自有手机只开放低风险功能……」；
 * BSM:936「员工安装私人手机 App 时生成并绑定一个该手机专属的设备标识，把手机和员工绑定……」）：首页按服务端下发的功能清单显示；
 * 本机还没声明身份时只给建立身份的入口。
 */
class StaffAppFunctionsTest {
  @Test
  fun privatePhoneFetchesItsFunctionList() {
    var got: Triple<String, String, String?>? = null
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      got = Triple(exchange.requestMethod, exchange.requestURI.path, exchange.requestHeaders.getFirst("X-Terminal-Type"))
      val bytes = """{"code":"OK","data":{"terminal_type":"staff_private_phone","functions":["leave","notices"]}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply {
        accessToken = "staff"; terminalType = STAFF_PRIVATE_PHONE; deviceId = "fp1"
      }
      val functions = api.staffAppFunctions()
      assertEquals(Triple("GET", "/api/v1/terminals/staff-app-functions", STAFF_PRIVATE_PHONE), got)
      assertEquals("staff_private_phone", functions.terminalType)
      assertEquals(listOf("leave", "notices"), functions.functions)
    } finally { server.stop(0) }
  }

  @Test
  fun homeShowsOnlyIdentityEntriesUntilDeclaredThenFollowsTheList() {
    assertFalse(homeShows(null, null, "ticket_verify"))
    assertTrue(homeShows(null, null, "staff_device"))
    assertTrue(homeShows(null, null, "tablet_pos"))
    // 第十八批 M 段第 112 条 112-R2：一体机模式也是建立身份的入口；112-R1 礼物/券核销不是。
    assertTrue(homeShows(null, null, "kiosk_claim"))
    assertFalse(homeShows(null, null, "voucher_redeem"))
    assertTrue(homeShows("staff_work_phone", null, "ticket_verify"))
    assertTrue(homeShows("staff_private_phone", setOf("leave"), "leave"))
    assertFalse(homeShows("staff_private_phone", setOf("leave"), "ticket_verify"))
  }
}
