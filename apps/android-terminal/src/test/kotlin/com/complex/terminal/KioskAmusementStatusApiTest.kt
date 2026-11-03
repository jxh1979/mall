package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 N 段第 120 条 120-R5（BSM:991「查看游乐设施状态：开放、排队、容量、暂停、关闭、故障、维护或安全提醒。」；BSM:1268）：
 * 一体机游乐设施状态屏读 GET /api/v1/amusement/projects/status-board，按蛇形键读出每个项目的状态、在玩/容量、排队人数、维护设备数、故障数与安全提醒。
 */
class KioskAmusementStatusApiTest {
  @Test
  fun statusBoardGetsTheBoardAndReadsEachProjectRow() {
    val requests = mutableListOf<Pair<String, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += exchange.requestMethod to exchange.requestURI.toString()
      val row = """{"project_id":"amp_1","project_name":"旋转木马","open_status":"open","capacity":30,"occupied_count":12,""" +
        """"waiting_count":3,"maintenance_device_count":1,"fault_count":1,"safety_notices":["身高 1.2 米以下须家长陪同"]}"""
      val bytes = """{"code":"OK","data":{"items":[$row],"page":1,"page_size":50,"total":1}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "resident"; terminalType = "kiosk" }

      assertEquals(
        listOf(AmusementProjectStatus("amp_1", "旋转木马", "open", 30, 12, 3, 1, 1, listOf("身高 1.2 米以下须家长陪同"))),
        api.amusementStatusBoard(),
      )
      assertEquals(listOf("GET" to "/api/v1/amusement/projects/status-board"), requests)
    } finally { server.stop(0) }
  }
}
