package com.complex.terminal

// 文件版本：V0.02

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 M 段第 111 条 111-R1（BSM:1466「我的工单：只看派给自己的工单和处理进度。」）：员工手机「我的工单」一并拉派给本人的商户服务工单（只看），
 * 打 GET /api/v1/merchant-service/work-orders/assigned-to-me，蛇形键解析；状态按 ENUM:264 七态、类型按商户工单四类显示中文。
 */
class MyMerchantWorkOrderApiTest {
  @Test
  fun listsMerchantWorkOrdersAssignedToMe() {
    var got: Pair<String, String>? = null
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      got = exchange.requestMethod to exchange.requestURI.path
      val row = """{"id":"mswo-1","title":"门锁坏了","work_order_type":"repair","priority":"high","status":"pending_merchant","due_at":"2099-01-01T00:00:00Z","owner_user_id":"usr_me"}"""
      val bytes = """{"code":"OK","data":{"items":[$row],"page":1,"page_size":100,"total":1}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val mine = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "staff" }.listMyMerchantWorkOrders()
      assertEquals("GET" to "/api/v1/merchant-service/work-orders/assigned-to-me", got)
      assertEquals(listOf(MyMerchantWorkOrderRecord("mswo-1", "门锁坏了", "repair", "high", "pending_merchant", "2099-01-01T00:00:00Z")), mine)
      assertEquals("待商户确认", merchantWorkOrderStatusZh("pending_merchant"))
      assertEquals("已处理待关闭", merchantWorkOrderStatusZh("resolved"))
      assertEquals("报修", merchantWorkOrderTypeZh("repair"))
      assertEquals("其他服务请求", merchantWorkOrderTypeZh("service"))
      // 第十八批 W 段第 192 条 192-R2（PGM:287）：商户工单优先级显示中文。
      assertEquals("高", workPriorityZh(mine.single().priority))
    } finally { server.stop(0) }
  }
}
