package com.complex.terminal

// 文件版本：V0.01

import java.net.URI
import java.net.http.HttpRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** U02-003/U01-008：终端身份只来自绑定类型、设备签名头与验签器规范串一致、注册请求携带设备证明（可失败断言）。 */
class TerminalApiClientHeadersTest {
  private val api = TerminalApiClient("http://127.0.0.1:1")

  private fun headers(path: String, method: String = "POST", body: String = "{}", override: String? = null): Map<String, List<String>> {
    val builder = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:1$path"))
    if (method == "POST") builder.POST(HttpRequest.BodyPublishers.ofString(body)) else builder.GET()
    api.applyHeaders(builder, path, method, body, override)
    return builder.build().headers().map()
  }

  @Test
  fun unboundClientDeclaresNoTerminalTypeAndNoDeviceHeaders() {
    val h = headers("/api/v1/staff/shifts")
    assertNull(h["X-Terminal-Type"])
    assertNull(h["X-Device-Id"])
  }

  @Test
  fun terminalTypeComesFromBindingNotFromPath() {
    api.terminalType = "tablet"
    assertEquals(listOf("tablet"), headers("/api/v1/services/orders")["X-Terminal-Type"])
    // 不按路径改报：同一绑定下注册路径也只报绑定类型；私人手机通道由调用方显式声明。
    assertEquals(listOf("tablet"), headers("/api/v1/staff/self-registrations")["X-Terminal-Type"])
    assertEquals(listOf(STAFF_PRIVATE_PHONE), headers("/api/v1/staff/self-registrations", override = STAFF_PRIVATE_PHONE)["X-Terminal-Type"])
  }

  @Test
  fun deviceSignatureHeadersFollowVerifierCanonicalFormExceptLogin() {
    api.deviceId = "dev-1"
    api.terminalType = "staff_work_phone"
    var signed: ByteArray? = null
    api.deviceSigner = { bytes -> signed = bytes; "sig" }
    val body = "{\"a\":1}"
    val h = headers("/api/v1/staff/attendance/punch", body = body)
    assertEquals(listOf("dev-1"), h["X-Device-Id"])
    assertEquals(listOf("sig"), h["X-Device-Signature"])
    assertNotNull(h["X-Device-Timestamp"])
    assertNotNull(h["X-Device-Nonce"])
    val canonical = String(signed!!, Charsets.UTF_8).split("\n")
    assertEquals("dev-1", canonical[0])
    assertEquals("POST", canonical[3])
    assertEquals("/api/v1/staff/attendance/punch", canonical[4])
    assertNull(headers("/api/v1/auth/login")["X-Device-Id"])
  }

  @Test
  fun selfRegistrationRequestCarriesDeviceProofInSnakeCase() {
    val json = api.json.parseToJsonElement(
      api.json.encodeToString(
        StaffSelfRegistrationRequest(
          idempotencyKey = "k1", companyId = "company_1", name = "员工",
          devicePublicKey = "PUBKEY", deviceFingerprintHash = "abcd",
        ),
      ),
    ).jsonObject
    assertEquals("PUBKEY", json["device_public_key"]?.jsonPrimitive?.content)
    assertEquals("abcd", json["device_fingerprint_hash"]?.jsonPrimitive?.content)
    assertEquals("company_1", json["company_id"]?.jsonPrimitive?.content)
  }

  @Test
  fun bindRequestCarriesStaffIdForWorkPhone() {
    val json = api.json.parseToJsonElement(
      api.json.encodeToString(BindDeviceRequest(deviceId = "dev-1", staffId = "staff_1", idempotencyKey = "b1")),
    ).jsonObject
    assertEquals("staff_1", json["staff_id"]?.jsonPrimitive?.content)
    assertEquals("staff_work_phone", json["terminal_type"]?.jsonPrimitive?.content)
  }
}
