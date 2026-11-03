package com.complex.terminal

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class P0TerminalSmokeResult(
  val health: Boolean,
  val login: Boolean,
  val deviceBound: Boolean,
  val onlinePreviewAllowed: Boolean,
  val onlineVerificationStatus: String,
  val eventsReported: Int,
  val returned: Boolean,
  val backendEvents: Int,
)

fun main() {
  val baseUrl = System.getenv("P0_API_BASE_URL") ?: "http://127.0.0.1:8080"
  val username = requireNotNull(System.getenv("P0_ADMIN_USERNAME")) { "P0_ADMIN_USERNAME is required" }
  val password = requireNotNull(System.getenv("P0_ADMIN_PASSWORD")) { "P0_ADMIN_PASSWORD is required" }
  val stamp = System.currentTimeMillis()

  val api = TerminalApiClient(baseUrl)
  val health = api.health()
  check(health.ok) { "API health failed" }

  val adminLogin = api.login(username, password)
  check(adminLogin.accessToken.isNotBlank()) { "admin login failed" }
  api.accessToken = adminLogin.accessToken

  val fixture = P0Fixture(api, stamp).create()
  val store = InMemoryLocalStore()
  val workflow = TerminalWorkflow(api, store)

  val terminalSession = workflow.login(username, password)
  val binding = workflow.bindDevice(
    BindDeviceRequest(
      idempotencyKey = "android_p0_device_bind_$stamp",
      deviceId = "P0-ANDROID-$stamp",
      terminalType = "staff_work_phone",
      bindingScope = "space",
      boundSpaceId = fixture.pointId,
      mdmDeviceRef = "android-smoke-$stamp",
    ),
  )

  val preview = workflow.previewOnline(fixture.entitlementCode)
  val onlineVerification = workflow.verifyOnline(fixture.entitlementCode)

  val events = workflow.reportDeviceEvents()
  val returned = workflow.returnDevice("P0 smoke shift closed")
  val backendEvents = api.events(limit = 30)

  val result = P0TerminalSmokeResult(
    health = health.ok,
    login = terminalSession.accessToken.isNotBlank(),
    deviceBound = binding.status == "bound",
    onlinePreviewAllowed = preview.allowed,
    onlineVerificationStatus = onlineVerification.status,
    eventsReported = events.accepted,
    returned = returned.status == ReturnStatus.RETURNED,
    backendEvents = backendEvents.size,
  )

  check(result.login)
  check(result.deviceBound)
  check(result.onlinePreviewAllowed)
  check(result.onlineVerificationStatus == "success")
  check(result.eventsReported > 0)
  check(result.returned)

  println(api.json.encodeToString(result))
}
