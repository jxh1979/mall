package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 N 段第 120 条 120-R6（BSM:992「工程师可在授权后填写游乐设施定期检修记录，检修单位作为供应商记录。」；BSM:1269「工程师可在一体机上
 * 填写定期检修记录，记录检修单位、检修人员、结果和异常原因。」；BSM:1270）：检修屏从列表选项目、设备与检修单位，主管确认屏列检修记录，按蛇形键读出。
 */
class KioskMaintenanceApiTest {
  @Test
  fun maintenanceScreensListProjectsDevicesSuppliersAndRecords() {
    val requests = mutableListOf<String>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val path = exchange.requestURI.toString()
      requests += path
      val item = when (path) {
        "/api/v1/amusement/projects" -> """{"id":"amp_1","project_name":"旋转木马","open_status":"open","capacity":30}"""
        "/api/v1/amusement/projects/amp_1/devices" -> """{"id":"amd_1","project_id":"amp_1","device_code":"ride-1","name":"一号车","status":"active"}"""
        "/api/v1/inventory/suppliers" -> """{"id":"sup_1","supplier_name":"游乐检修公司","supplier_type":"general","status":"active"}"""
        else -> """{"id":"mr_1","project_id":"amp_1","device_id":"ride-1","engineer_name":"张工","result":"pass","status":"pending_confirm"}"""
      }
      val bytes = """{"code":"OK","data":{"items":[$item],"page":1,"page_size":50,"total":1}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "engineer"; terminalType = "kiosk" }

      assertEquals(listOf(AmusementProject("amp_1", "旋转木马", "open")), api.listAmusementProjects())
      assertEquals(listOf(AmusementDevice("amd_1", "ride-1", "一号车", "active")), api.listAmusementDevices("amp_1"))
      assertEquals(listOf(InventorySupplier("sup_1", "游乐检修公司", "active")), api.listInventorySuppliers())
      assertEquals(listOf("mr_1" to "pending_confirm"), api.listAmusementMaintenanceRecords().map { it.id to it.status })
      assertEquals(
        listOf("/api/v1/amusement/projects", "/api/v1/amusement/projects/amp_1/devices", "/api/v1/inventory/suppliers",
          "/api/v1/amusement/maintenance-records"),
        requests,
      )
    } finally { server.stop(0) }
  }
}
