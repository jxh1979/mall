package com.complex.terminal

// 文件版本：V0.01

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 第 84 条 84-R1（BSM 第 1201 行「扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）」）：工作手机签名 multipart 上传补录证据。 */
class EvidenceUploadApiTest {
  @Test
  fun uploadEvidenceSendsSignedMultipartWithManualBackfillPart() {
    var request = Triple("", "", "")
    var signature: String? = null
    var body = ByteArray(0)
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      request = Triple(exchange.requestMethod, exchange.requestURI.toString(), exchange.requestHeaders.getFirst("Content-Type"))
      signature = exchange.requestHeaders.getFirst("X-Device-Signature")
      body = exchange.requestBody.readBytes()
      val bytes = """{"code":"OK","data":{"id":"att_1","content_type":"image/jpeg","file_size":3}}""".toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      val signed = mutableListOf<String>()
      val api = TerminalApiClient("http://127.0.0.1:${server.address.port}").apply {
        accessToken = "staff"
        deviceId = "phone-1"
        deviceSigner = { bytes -> signed += bytes.toString(Charsets.UTF_8); "sig-1" }
      }

      val record = api.uploadEvidence("a.jpg", "abc".toByteArray(Charsets.UTF_8))

      assertEquals(EvidenceAttachmentRecord("att_1", "image/jpeg", 3), record)
      assertEquals("POST" to "/api/v1/evidence/attachments/upload", request.first to request.second)
      assertTrue(request.third.startsWith("multipart/form-data; boundary="), request.third)
      val text = body.toString(Charsets.UTF_8)
      assertTrue(text.contains("Content-Disposition: form-data; name=\"business_type\"\r\n\r\nmanual_backfill\r\n"), text)
      assertTrue(text.contains("Content-Disposition: form-data; name=\"file\"; filename=\"a.jpg\"\r\n" +
        "Content-Type: application/octet-stream\r\n\r\nabc\r\n"), text)
      assertEquals("sig-1", signature)
      assertEquals("bd6fee8fd9f720c2809af688010ac65c38b4be2562ca42ddfa4656a5d11d99d3", signed.single().lines().last())
    } finally { server.stop(0) }
  }
}
