package com.complex.terminal

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Android 端 [com.complex.terminal] 包内的 API 客户端。
 *
 * 与 JVM 版（apps/android-terminal/src/main 的 TerminalApiClient，使用 java.net.http.HttpClient）
 * 公共方法签名一致，供 [TerminalWorkflow] 直接调用；区别仅在传输层——Android 平台的
 * android.jar 不包含 java.net.http 包，故此处改用 [HttpURLConnection]。
 * 业务逻辑（TerminalWorkflow / TerminalModels / InMemoryLocalStore）保持复用，不重复实现。
 */
class ApiException(
  val statusCode: Int,
  val path: String,
  val responseBody: String,
) : RuntimeException("$path returned $statusCode: $responseBody")

class TerminalApiClient(
  private val baseUrl: String,
  private val connectTimeoutMs: Int = 10_000,
  private val readTimeoutMs: Int = 30_000,
  allowedHosts: Set<String>? = null,
) {
  init {
    val uri = URI.create(baseUrl)
    val loopbackHttp = uri.scheme == "http" && (uri.host == "127.0.0.1" || uri.host == "localhost")
    require(uri.scheme == "https" || loopbackHttp) {
      "terminal API baseUrl must use https outside loopback"
    }
    // 4.2 allowed hosts 生效：构建变量白名单在客户端入口强制，防构建误配把终端指到非授权目标。
    if (allowedHosts != null) {
      require(uri.host in allowedHosts) {
        "terminal API host '${uri.host}' is not in allowed hosts $allowedHosts"
      }
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  val json: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    namingStrategy = JsonNamingStrategy.SnakeCase
  }

  var accessToken: String? = null

  fun health(): HealthResponse = get("/api/v1/system/health")

  fun login(username: String, password: String): LoginResponse =
    post("/api/v1/auth/login", LoginRequest(username, password))

  fun bindDevice(request: BindDeviceRequest): DeviceBinding =
    post("/api/v1/terminals/devices/bind", request)

  fun offlineEntitlements(merchantId: String?, venueId: String?, limit: Int = 200): OfflinePackageResponse {
    val query = listOfNotNull(
      merchantId?.let { "merchantId=${urlEncode(it)}" },
      venueId?.let { "venueId=${urlEncode(it)}" },
      "limit=$limit",
    ).joinToString("&")
    return get("/api/v1/terminals/offline-entitlements?$query")
  }

  fun preview(request: VerificationRequest): PreviewResponse =
    post("/api/v1/ticketing/verifications/preview", request)

  fun verify(request: VerificationRequest): VerificationResponse =
    post("/api/v1/ticketing/verifications", request)

  fun syncTasks(request: SyncTasksRequest): SyncTasksResponse =
    post("/api/v1/terminals/sync-tasks", request)

  fun reportEvents(request: EventBatchRequest): EventBatchResponse =
    post("/api/v1/evidence/events", request)

  fun returnDevice(request: DeviceReturnRequest): DeviceBinding =
    post("/api/v1/terminals/devices/return", request)

  fun events(limit: Int = 20): List<JsonObject> = get("/api/v1/evidence/events?limit=$limit")

  fun amusementVerify(request: VerificationRequest): AmusementVerificationRecord =
    post("/api/v1/amusement/verifications", request)

  fun submitAmusementMaintenance(request: AmusementMaintenanceRecordRequest): AmusementMaintenanceRecord =
    post("/api/v1/amusement/maintenance-records", request)

  fun confirmAmusementMaintenanceResume(
    recordId: String,
    request: AmusementMaintenanceResumeRequest,
  ): AmusementMaintenanceRecord =
    post("/api/v1/amusement/maintenance-records/${urlEncode(recordId)}/confirm-resume", request)

  fun createBarOrder(request: BarOrderRequest): BarOrderRecord =
    post("/api/v1/bar/orders", request)

  fun createBarPaymentIntent(billId: String, request: PaymentIntentRequest): PaymentIntentRecord =
    post("/api/v1/bar/bills/${urlEncode(billId)}/pay-intents", request)

  fun settleBarBill(billId: String, request: BarBillSettleRequest): BarBillRecord =
    post("/api/v1/bar/bills/${urlEncode(billId)}/settle", request)

  fun bindStaffDeviceCredential(request: StaffDeviceBindRequest): StaffDeviceCredentialRecord =
    post("/api/v1/staff/device-credentials", request)

  fun approveStaffDeviceRebind(
    credentialId: String,
    request: StaffDeviceRebindDecisionRequest,
  ): StaffDeviceCredentialRecord =
    post("/api/v1/staff/device-credentials/${urlEncode(credentialId)}/approve-rebind", request)

  fun reportStaffDeviceIntegrity(
    credentialId: String,
    request: StaffDeviceIntegrityReportRequest,
  ): StaffDeviceCredentialRecord =
    post("/api/v1/staff/device-credentials/${urlEncode(credentialId)}/report-integrity", request)

  fun reverseVerification(verificationId: String, request: VerificationReverseRequest): VerificationResponse =
    post("/api/v1/ticketing/verifications/${urlEncode(verificationId)}/reverse", request)

  // --------------------------------------------------------------------------
  // 传输层（HttpURLConnection）
  // --------------------------------------------------------------------------

  private inline fun <reified T> get(path: String): T {
    val connection = open(path, "GET")
    return send(path, connection, body = null)
  }

  private inline fun <reified RequestBody, reified ResponseBody> post(path: String, body: RequestBody): ResponseBody {
    val connection = open(path, "POST")
    connection.setRequestProperty("content-type", "application/json")
    connection.doOutput = true
    return send(path, connection, json.encodeToString(body))
  }

  @PublishedApi
  internal fun open(path: String, method: String): HttpURLConnection {
    val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection // nosemgrep: baseUrl is validated to https/loopback in init.
    connection.requestMethod = method
    connection.connectTimeout = connectTimeoutMs
    connection.readTimeout = readTimeoutMs
    accessToken?.takeIf { it.isNotBlank() }?.let { connection.setRequestProperty("authorization", "Bearer $it") }
    return connection
  }

  @PublishedApi
  internal inline fun <reified T> send(path: String, connection: HttpURLConnection, body: String?): T {
    try {
      if (body != null) {
        connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
      }
      val status = connection.responseCode
      val responseBody = readBody(connection, status)
      if (status !in 200..299) {
        throw ApiException(status, path, responseBody)
      }
      val element = json.parseToJsonElement(responseBody)
      if (element is JsonObject && element.containsKey("code") && element.containsKey("data")) {
        return json.decodeFromJsonElement(element["data"]!!)
      }
      return json.decodeFromJsonElement(element)
    } finally {
      connection.disconnect()
    }
  }

  @PublishedApi
  internal fun readBody(connection: HttpURLConnection, status: Int): String {
    val stream = if (status in 200..299) connection.inputStream else (connection.errorStream ?: connection.inputStream)
    return stream.bufferedReader().use(BufferedReader::readText)
  }

  private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
