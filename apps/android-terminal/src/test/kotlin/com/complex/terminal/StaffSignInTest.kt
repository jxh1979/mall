package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 第十八批 N 段第 120 条 120-R1（BSM:986「……可由授权服务人员登录后辅助客人购票……」；BSM:990「……必须由授权员工记录……」）：
 * 一体机上授权员工那一段请求带授权员工的令牌，做完（含出错）还回常驻员工令牌；授权员工用完以自己的令牌登出。
 */
class StaffSignInTest {
  @Test
  fun asStaffUsesTheSignedInStaffTokenAndRestoresTheResidentToken() {
    val requests = mutableListOf<Triple<String, String?, String>>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      requests += Triple(exchange.requestURI.toString(), exchange.requestHeaders.getFirst("Authorization"),
        exchange.requestBody.readBytes().toString(Charsets.UTF_8))
      val data = if (exchange.requestURI.path == "/api/v1/auth/logout") """{"session_id":"ses_staff","status":"logged_out"}""" else """{"ok":true}"""
      val bytes = """{"code":"OK","data":$data}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "resident"; terminalType = "kiosk" }

      api.asStaff("staff_tok") { api.logout("ses_staff") }
      val (path, authorization, body) = requests.single()
      assertEquals("/api/v1/auth/logout", path)
      assertEquals("Bearer staff_tok", authorization)
      assertTrue(body.contains("\"session_id\":\"ses_staff\"") && body.contains("\"idempotency_key\":"), body)
      assertEquals("resident", api.accessToken)

      assertFailsWith<IllegalStateException> { api.asStaff("staff_tok") { error("授权员工这一步出错") } }
      assertEquals("resident", api.accessToken)
      api.health()
      assertEquals("Bearer resident", requests.last().second)
    } finally { server.stop(0) }
  }

  /** 120-R1 复核：出票还没返回又起一段授权员工请求（两个后台线程重叠，先起的先完），常驻请求始终带常驻令牌——重叠进行中别的线程发的、两段都结束后发的都是。 */
  @Test
  fun overlappingStaffSectionsOnOtherThreadsNeverSwapTheResidentToken() {
    val auths = Collections.synchronizedList(mutableListOf<String?>())
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      auths += exchange.requestHeaders.getFirst("Authorization")
      val bytes = """{"code":"OK","data":{"ok":true}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "resident"; terminalType = "kiosk" }
      val aIn = CountDownLatch(1); val aGo = CountDownLatch(1); val bIn = CountDownLatch(1); val bGo = CountDownLatch(1)
      val a = thread { api.asStaff("staff_tok") { aIn.countDown(); aGo.await(); api.health() } }
      aIn.await()
      val b = thread { api.asStaff("staff_tok") { bIn.countDown(); bGo.await(); api.health() } }
      bIn.await()
      api.health()
      aGo.countDown(); a.join()
      bGo.countDown(); b.join()
      api.health()
      assertEquals(listOf("Bearer resident", "Bearer staff_tok", "Bearer staff_tok", "Bearer resident"), auths.toList())
    } finally { server.stop(0) }
  }
}
