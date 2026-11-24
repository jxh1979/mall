package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 CJ 段第 417 条 417-R1（BSM:1474「主管排班：排到几点几分，可指定当天工作点位（比如某个服务区、某片保洁区域）；主管在手机上也能排。」）：
 * 主管排班屏读 GET /api/v1/staff/shift-staff-options 与 GET /api/v1/staff/shift-point-options（分页包络），按蛇形键读出员工编号与姓名、空间编号与空间名。
 */
class ShiftManageOptionsApiTest {
  @Test
  fun readsStaffAndPointOptionsFromTheTwoLists() {
    val requests = mutableListOf<Pair<String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += exchange.requestMethod to exchange.requestURI.toString()
      val item = if (exchange.requestURI.path.endsWith("/shift-staff-options")) """{"user_id":"usr_a","name":"张三"}"""
        else """{"id":"sp_1","space_name":"卡座-01"}"""
      val bytes = """{"code":"OK","data":{"items":[$item],"page":1,"page_size":1,"total":1}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "manager"; terminalType = "staff_private_phone" }

      assertEquals(listOf(StaffShiftStaffOption("usr_a", "张三")), api.staffShiftStaffOptions())
      assertEquals(listOf(StaffShiftPointOption("sp_1", "卡座-01")), api.staffShiftPointOptions())
      assertEquals(listOf("GET" to "/api/v1/staff/shift-staff-options", "GET" to "/api/v1/staff/shift-point-options"), requests)
    } finally { server.stop(0) }
  }
}
