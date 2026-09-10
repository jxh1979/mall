package com.complex.terminal

// 文件版本：V0.01

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DeviceRequestHeadersTest {
  @Test fun signedBytesMatchServerCanonicalAndDetectBodyChange() {
    val keys = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    val body = "{\"method\":\"qr\",\"tag_ref\":\"点位一\"}"
    val headers = deviceRequestHeaders("phone-1", "post", "/api/v1/staff/attendance/punch?view=x", body,
      signer = { bytes -> Base64.getEncoder().encodeToString(Signature.getInstance("SHA256withECDSA").run {
        initSign(keys.private); update(bytes); sign()
      }) }, timestamp = 123456L, nonce = "event-1")
    fun verifies(content: String): Boolean {
      val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
      val canonical = "phone-1\n123456\nevent-1\nPOST\n/api/v1/staff/attendance/punch\n$digest"
      return Signature.getInstance("SHA256withECDSA").run {
        initVerify(keys.public); update(canonical.toByteArray(Charsets.UTF_8))
        verify(Base64.getDecoder().decode(headers.getValue("X-Device-Signature")))
      }
    }
    assertEquals("phone-1", headers["X-Device-Id"])
    assertEquals("123456", headers["X-Device-Timestamp"])
    assertTrue(verifies(body))
    assertFalse(verifies(body.replace("qr", "nfc")))
  }
}
