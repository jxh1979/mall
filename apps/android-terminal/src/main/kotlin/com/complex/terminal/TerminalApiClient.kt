package com.complex.terminal

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.buildJsonObject

class ApiException(
  val statusCode: Int,
  val path: String,
  val responseBody: String,
) : RuntimeException("$path returned $statusCode: $responseBody")

class TerminalApiClient(
  @PublishedApi internal val baseUrl: String,
  @PublishedApi internal val client: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .build(),
) {
  init {
    val uri = URI.create(baseUrl)
    val loopbackHttp = uri.scheme == "http" && (uri.host == "127.0.0.1" || uri.host == "localhost")
    val explicitlyAllowed = System.getenv("ALLOW_INSECURE_TERMINAL_HTTP") == "true"
    require(uri.scheme == "https" || loopbackHttp || explicitlyAllowed) {
      "terminal API baseUrl must use https outside loopback"
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

  // 游乐核销（复用 ticketing VerificationRequest 契约）
  fun amusementVerify(request: VerificationRequest): AmusementVerificationRecord =
    post("/api/v1/amusement/verifications", request)

  // 游乐检修：提交检修记录
  fun submitAmusementMaintenance(request: AmusementMaintenanceRecordRequest): AmusementMaintenanceRecord =
    post("/api/v1/amusement/maintenance-records", request)

  // 游乐检修：主管确认恢复运营
  fun confirmAmusementMaintenanceResume(
    recordId: String,
    request: AmusementMaintenanceResumeRequest,
  ): AmusementMaintenanceRecord =
    post("/api/v1/amusement/maintenance-records/${urlEncode(recordId)}/confirm-resume", request)

  // 酒吧一体机/平板：点酒下单
  fun createBarOrder(request: BarOrderRequest): BarOrderRecord =
    post("/api/v1/bar/orders", request)

  // 酒吧账单：创建支付意图
  fun createBarPaymentIntent(billId: String, request: PaymentIntentRequest): PaymentIntentRecord =
    post("/api/v1/bar/bills/${urlEncode(billId)}/pay-intents", request)

  // 酒吧账单：结算
  fun settleBarBill(billId: String, request: BarBillSettleRequest): BarBillRecord =
    post("/api/v1/bar/bills/${urlEncode(billId)}/settle", request)

  // 员工设备凭证：绑定
  fun bindStaffDeviceCredential(request: StaffDeviceBindRequest): StaffDeviceCredentialRecord =
    post("/api/v1/staff/device-credentials", request)

  // 员工设备凭证：主管批准换机
  fun approveStaffDeviceRebind(
    credentialId: String,
    request: StaffDeviceRebindDecisionRequest,
  ): StaffDeviceCredentialRecord =
    post("/api/v1/staff/device-credentials/${urlEncode(credentialId)}/approve-rebind", request)

  // 员工设备凭证：完整性上报
  fun reportStaffDeviceIntegrity(
    credentialId: String,
    request: StaffDeviceIntegrityReportRequest,
  ): StaffDeviceCredentialRecord =
    post("/api/v1/staff/device-credentials/${urlEncode(credentialId)}/report-integrity", request)

  // 票务核销冲正
  fun reverseVerification(verificationId: String, request: VerificationReverseRequest): VerificationResponse =
    post("/api/v1/ticketing/verifications/${urlEncode(verificationId)}/reverse", request)

  inline fun <reified T> get(path: String): T {
    val builder = HttpRequest.newBuilder()
      .uri(URI.create("$baseUrl$path"))
      .timeout(Duration.ofSeconds(30))
      .GET()
    applyHeaders(builder)
    val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    return decode(path, response)
  }

  inline fun <reified RequestBody, reified ResponseBody> post(path: String, body: RequestBody): ResponseBody {
    val builder = HttpRequest.newBuilder()
      .uri(URI.create("$baseUrl$path"))
      .timeout(Duration.ofSeconds(30))
      .header("content-type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(body)))
    applyHeaders(builder)
    val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    return decode(path, response)
  }

  inline fun <reified ResponseBody> postJson(path: String, body: JsonObject): ResponseBody {
    val builder = HttpRequest.newBuilder()
      .uri(URI.create("$baseUrl$path"))
      .timeout(Duration.ofSeconds(30))
      .header("content-type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(normalizeJsonObjectKeys(body))))
    applyHeaders(builder)
    val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    return decode(path, response)
  }

  fun applyHeaders(builder: HttpRequest.Builder) {
    accessToken?.takeIf { it.isNotBlank() }?.let { builder.header("authorization", "Bearer $it") }
  }

  inline fun <reified T> decode(path: String, response: HttpResponse<String>): T {
    if (response.statusCode() !in 200..299) {
      throw ApiException(response.statusCode(), path, response.body())
    }
    val element = json.parseToJsonElement(response.body())
    if (element is JsonObject && element.containsKey("code") && element.containsKey("data")) {
      return json.decodeFromJsonElement(element["data"]!!)
    }
    return json.decodeFromJsonElement(element)
  }

  private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

  fun normalizeJsonObjectKeys(body: JsonObject): JsonObject =
    normalizeJsonElementKeys(body) as JsonObject

  private fun normalizeJsonElementKeys(element: JsonElement): JsonElement =
    when (element) {
      is JsonArray -> JsonArray(element.map(::normalizeJsonElementKeys))
      is JsonObject -> buildJsonObject {
        for ((key, value) in element) {
          put(toSnakeCase(key), if (key == "payload") value else normalizeJsonElementKeys(value))
        }
      }
      else -> element
    }

  private fun toSnakeCase(value: String): String =
    value.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}
