package com.complex.terminal

// 文件版本：V0.02

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
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

  // ── 第十八批 M 段第 108 条（PGM:719「开关关闭时，端或模块就不能用；开关打开后，也还要继续看员工权限、商户范围和设备状态。」；
  //    PGM:906「员工注册审批、设备绑定、换机解绑重绑。」）：未绑定的手机发绑定、登记签名公钥、私人手机凭证绑定三条请求时，
  //    按所绑类型声明终端头（其他请求未绑定时照旧不发，见 unboundClientDeclaresNoTerminalTypeAndNoDeviceHeaders）。 ──

  /** 本机 HttpServer 收下客户端真实发出的请求：路径、终端头、设备头、请求体。客户端未绑定（没有终端类型与设备号），只有登录令牌。 */
  private fun captured(response: String, call: (TerminalApiClient) -> Unit): Map<String, String?> {
    val got = mutableMapOf<String, String?>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      got["path"] = exchange.requestURI.path
      got["type"] = exchange.requestHeaders.getFirst("X-Terminal-Type")
      got["device"] = exchange.requestHeaders.getFirst("X-Device-Id")
      got["body"] = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
      val bytes = response.toByteArray(Charsets.UTF_8)
      exchange.sendResponseHeaders(200, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
      call(TerminalApiClient("http://127.0.0.1:${server.address.port}").apply { accessToken = "admin" })
    } finally { server.stop(0) }
    return got
  }

  private fun bodyOf(got: Map<String, String?>) = api.json.parseToJsonElement(got["body"]!!).jsonObject

  @Test
  fun unboundBindDeviceDeclaresTheBoundTerminalType() {
    val phone = captured("""{"code":"OK","data":{"id":"td1","status":"bound"}}""") {
      it.bindDevice(BindDeviceRequest(deviceId = "dev-1", staffId = "sp_1", idempotencyKey = "b1"))
    }
    assertEquals("/api/v1/terminals/devices/bind", phone["path"])
    assertEquals("staff_work_phone", phone["type"])
    assertNull(phone["device"])
    assertEquals("sp_1", bodyOf(phone)["staff_id"]?.jsonPrimitive?.content)
    assertEquals("staff_work_phone", bodyOf(phone)["terminal_type"]?.jsonPrimitive?.content)

    val tablet = captured("""{"code":"OK","data":{"id":"td2","status":"bound"}}""") {
      it.bindDevice(BindDeviceRequest(deviceId = "pos-1", terminalType = "tablet", idempotencyKey = "b2"))
    }
    assertEquals("tablet", tablet["type"])
    assertNull(tablet["device"])
  }

  @Test
  fun unboundSigningKeyRegistrationDeclaresTheBoundTerminalType() {
    val got = captured("""{"code":"OK","data":{"status":"registered"}}""") {
      it.registerDeviceSigningKey("dev-1", "PUB", "k1", "staff_work_phone")
    }
    assertEquals("/api/v1/terminals/devices/dev-1/signing-key", got["path"])
    assertEquals("staff_work_phone", got["type"])
    assertNull(got["device"])
    assertEquals(setOf("public_key", "idempotency_key"), bodyOf(got).keys)
    assertEquals("PUB", bodyOf(got)["public_key"]?.jsonPrimitive?.content)
    assertEquals("k1", bodyOf(got)["idempotency_key"]?.jsonPrimitive?.content)
  }

  @Test
  fun unboundStaffDeviceCredentialBindDeclaresPrivatePhone() {
    val got = captured("""{"code":"OK","data":{"id":"c1","bind_status":"pending_review"}}""") {
      it.bindStaffDeviceCredential(StaffDeviceBindRequest(idempotencyKey = "c1", staffProfileId = "sp_1",
        devicePublicKey = "PUB", deviceFingerprintHash = "fp1"))
    }
    assertEquals("/api/v1/staff/device-credentials", got["path"])
    assertEquals(STAFF_PRIVATE_PHONE, got["type"])
    assertNull(got["device"])
    val body = bodyOf(got)
    assertEquals("c1", body["idempotency_key"]?.jsonPrimitive?.content)
    assertEquals("sp_1", body["staff_profile_id"]?.jsonPrimitive?.content)
    assertEquals("PUB", body["device_public_key"]?.jsonPrimitive?.content)
    assertEquals("fp1", body["device_fingerprint_hash"]?.jsonPrimitive?.content)
  }

  /** 第十八批 M 段第 116 条 116-R3：已声明身份的本机（工作手机）绑定员工设备凭证，照原类型发。 */
  @Test
  fun declaredClientStaffDeviceCredentialBindKeepsItsTerminalType() {
    val got = captured("""{"code":"OK","data":{"id":"c2","bind_status":"pending_review"}}""") {
      it.terminalType = "staff_work_phone"
      it.bindStaffDeviceCredential(StaffDeviceBindRequest(idempotencyKey = "c2", staffProfileId = "sp_1",
        devicePublicKey = "PUB", deviceFingerprintHash = "fp2"))
    }
    assertEquals("/api/v1/staff/device-credentials", got["path"])
    assertEquals("staff_work_phone", got["type"])
  }

  /** 第十八批 M 段第 112 条 112-R1：工作手机身份下查券与核销带工作手机终端头与本机设备号。 */
  @Test
  fun workPhoneVoucherCallsDeclareWorkPhone() {
    for (call in listOf<(TerminalApiClient) -> Unit>({ it.previewVoucherRedeem("vrc_1") },
        { it.redeemVoucher(VoucherRedeemRequest("k1", "vrc_1")) })) {
      val got = captured("""{"code":"OK","data":{}}""") {
        it.terminalType = "staff_work_phone"; it.deviceId = "dev-1"; it.deviceSigner = { "sig" }; call(it)
      }
      assertEquals("staff_work_phone", got["type"])
      assertEquals("dev-1", got["device"])
    }
  }

  /** 第十八批 M 段第 112 条 112-R2：一体机身份下领取请求带 kiosk 终端头与本机设备号。 */
  @Test
  fun kioskClaimCallsDeclareKiosk() {
    val got = captured("""{"code":"OK","data":{}}""") {
      it.terminalType = "kiosk"; it.deviceId = "kiosk-1"; it.deviceSigner = { "sig" }
      it.verify(kioskEntitlementRequest("ENT-1", "dev_kiosk_1", "space_1", "k1", "usr_1"))
    }
    assertEquals("/api/v1/ticketing/verifications", got["path"])
    assertEquals("kiosk", got["type"])
    assertEquals("kiosk-1", got["device"])
  }
}
