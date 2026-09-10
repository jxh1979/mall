package com.complex.terminal

// 文件版本：V0.04

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

/** 员工私人手机通道的终端类型码（main TerminalCanaryService 闭集）。 */
const val STAFF_PRIVATE_PHONE = "staff_private_phone"

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
  var deviceId: String? = null
  var deviceSigner: ((ByteArray) -> String)? = null
  /** U01-001/U02-001：终端身份由绑定/登记的设备类型决定并只声明一次——应用在绑定成功后写入（staff_work_phone / tablet / kiosk）；
   *  员工私人手机通道固定声明 [STAFF_PRIVATE_PHONE]。未绑定时不发类型头（服务端缺头即拒，fail-close），不按路径改报、不自报 app。 */
  var terminalType: String? = null

  fun registerDeviceSigningKey(id: String, publicKey: String, requestKey: String): JsonObject =
    post("/api/v1/terminals/devices/${urlEncode(id)}/signing-key", mapOf("public_key" to publicKey, "idempotency_key" to requestKey))

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

  fun events(limit: Int = 20): List<JsonObject> {
    // /api/v1/evidence/events 的 data 是分页信封 {items,page,page_size,total},非裸数组——取 items。
    val page: JsonObject = get("/api/v1/evidence/events?limit=$limit")
    return (page["items"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
  }

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
  ): AmusementMaintenanceRecord {
    val recordSeg = urlEncode(recordId)
    return post("/api/v1/amusement/maintenance-records/${recordSeg}/confirm-resume", request)
  }

  // 酒吧一体机/平板：点酒下单
  fun createBarOrder(request: BarOrderRequest): BarOrderRecord =
    post("/api/v1/bar/orders", request)

  // 酒吧账单：创建支付意图
  fun createBarPaymentIntent(billId: String, request: PaymentIntentRequest): PaymentIntentRecord {
    val billSeg = urlEncode(billId)
    return post("/api/v1/bar/bills/${billSeg}/pay-intents", request)
  }

  // 酒吧账单：结算
  fun settleBarBill(billId: String, request: BarBillSettleRequest): BarBillRecord {
    val billSeg = urlEncode(billId)
    return post("/api/v1/bar/bills/${billSeg}/settle", request)
  }

  // 员工设备凭证：绑定
  // ── BYOD 一期(2026-07-02):员工自有手机四功能 ──

  /** 私人手机自助提交员工注册申请(审批通过后才有账号)：以 staff_private_phone 声明通道身份，请求体携带安装时生成的设备公钥/指纹（U01-007/U02-002）。 */
  fun listStaffRegistrationCompanies(): List<StaffRegistrationCompany> =
    get("/api/v1/staff/registration-companies", STAFF_PRIVATE_PHONE)

  fun submitStaffSelfRegistration(request: StaffSelfRegistrationRequest): StaffRegistrationReceipt =
    post("/api/v1/staff/self-registrations", request, STAFF_PRIVATE_PHONE)

  fun submitStaffRegistration(request: StaffRegistrationSubmitRequest): StaffProfileRecord =
    post("/api/v1/staff/registrations", request)

  /** 公司内部公告(员工可见)。 */
  // ── G批:服务点单(公司代收口径:收款码原文由服务端返回,App 重绘 QR) ──

  fun listServiceCatalog(): List<ServiceCatalogRow> = get("/api/v1/services/catalog")

  fun createServiceOrder(request: ServiceOrderCreateReq): ServiceOrderRow =
    post("/api/v1/services/orders", request)

  fun listMyServiceOrders(): List<ServiceOrderRow> = get("/api/v1/services/orders/assigned-to-me")

  /** H批:服务员现场确认收款到公司账户(公司统一收款码,财务可对账)。 */
  fun markServiceOrderPaid(id: String, request: ServiceMarkPaidReq): ServiceOrderRow =
    post("/api/v1/services/orders/$id/mark-paid", request)

  fun acceptServiceOrder(id: String, request: ServiceActionReq): ServiceOrderRow =
    post("/api/v1/services/orders/$id/accept", request)

  fun completeServiceOrder(id: String, request: ServiceActionReq): ServiceOrderRow =
    post("/api/v1/services/orders/$id/complete", request)

  fun serviceOrderPaymentCode(id: String): JsonObject = get("/api/v1/services/orders/$id/payment-code")

  fun setMyPaymentCode(request: PaymentCodeReq): JsonObject = post("/api/v1/services/payment-code", request)

  fun myPaymentCode(): JsonObject = get("/api/v1/services/payment-code/mine")

  fun myServiceStats(month: String): JsonObject = get("/api/v1/services/orders/my-stats?month=$month")

  // ── BYOD 三期 F 批:我的薪资/请假/考勤月视图 ──

  /** 我的薪资(仅已月结定格月份;未结月为空——拍板:月结后才可见)。 */
  fun listMyPayroll(month: String): List<PayrollStatementRow> =
    get("/api/v1/staff/payroll/mine?month=$month")

  /** 提交请假(事假/病假/调休/年假;主管审)。 */
  fun submitLeave(request: LeaveSubmitRequest): LeaveRow =
    post("/api/v1/staff/leaves", request)

  /** 我的请假列表。 */
  fun listMyLeaves(): List<LeaveRow> = get("/api/v1/staff/leaves/mine")

  /** 考勤月视图(六态逐日)。 */
  fun attendanceMonthView(month: String): List<AttendanceDayRow> =
    get("/api/v1/staff/attendance/month-view?month=$month")

  // ── BYOD 二期 E 批:排班/考勤 ──

  /** 主管排班(双端;同员工同开始时刻幂等覆盖)。 */
  fun upsertStaffShift(request: StaffShiftUpsertRequest): StaffShiftRow =
    post("/api/v1/staff/shifts", request)

  /** 我的班次与点位(今天起 7 天,时间到分钟)。 */
  fun listMyShifts(): List<StaffShiftRow> = get("/api/v1/staff/shifts/mine")

  /** 考勤打卡(NFC/扫码;时间以服务器为准;严格无宽限判定随响应返回)。 */
  fun punchAttendance(request: AttendancePunchRequest): AttendanceRow =
    post("/api/v1/staff/attendance/punch", request)

  /** 我的打卡记录(最近 30 天)。 */
  fun listMyAttendance(): List<AttendanceRow> = get("/api/v1/staff/attendance/mine")

  fun listStaffNotices(): List<EmployeeNoticeRecord> = get("/api/v1/staff/notices")

  /** 公告一键已读(留痕)。 */
  fun acknowledgeStaffNotice(id: String, request: NoticeAcknowledgeRequest): EmployeeNoticeRecord =
    post("/api/v1/staff/notices/$id/acknowledge", request)

  /** 我的工单:只看流转到自己的(view_assigned,只读)。 */
  fun listMyWorkOrders(): List<MyWorkOrderRecord> = get("/api/v1/facility/work-orders/assigned-to-me")

  /** 酒吧空位视图(桌位+活跃预订占用标记,帮客人订位用)。 */
  fun listBarTableAvailability(): List<BarTableAvailabilityRow> = get("/api/v1/bar/tables/availability")

  /** 员工帮客人订位(免定金——预订创建即 not_required,收款只在公司设备/前台)。 */
  // ── 图形化点单 POS 目录（V256/V257）──
  fun listBarCategories(barId: String): List<BarProductCategory> =
    get("/api/v1/bar/catalog/categories?bar_id=${urlEncode(barId)}")

  fun listBarCatalogProducts(barId: String, categoryId: String?): List<BarProduct> {
    val q = StringBuilder("bar_id=").append(urlEncode(barId))
    if (categoryId != null && categoryId.isNotBlank()) q.append("&category_id=").append(urlEncode(categoryId))
    return get("/api/v1/bar/catalog/products?$q")
  }

  fun listBarCombos(barId: String): List<BarCombo> =
    get("/api/v1/bar/catalog/combos?bar_id=${urlEncode(barId)}")

  fun createBarOrderBill(orderId: String, request: BarOrderBillCreateRequest): BarBillRecord {
    val orderSeg = urlEncode(orderId)
    return post("/api/v1/bar/orders/${orderSeg}/bill", request)
  }

  fun createBarReservation(request: BarReservationCreateRequest): BarReservationRecord =
    post("/api/v1/bar/reservations", request)

  fun bindStaffDeviceCredential(request: StaffDeviceBindRequest): StaffDeviceCredentialRecord =
    post("/api/v1/staff/device-credentials", request)

  // 员工设备凭证：主管批准换机
  fun approveStaffDeviceRebind(
    credentialId: String,
    request: StaffDeviceRebindDecisionRequest,
  ): StaffDeviceCredentialRecord {
    val credentialSeg = urlEncode(credentialId)
    return post("/api/v1/staff/device-credentials/${credentialSeg}/approve-rebind", request)
  }

  // 员工设备凭证：完整性上报
  fun reportStaffDeviceIntegrity(
    credentialId: String,
    request: StaffDeviceIntegrityReportRequest,
  ): StaffDeviceCredentialRecord {
    val credentialSeg = urlEncode(credentialId)
    return post("/api/v1/staff/device-credentials/${credentialSeg}/report-integrity", request)
  }

  // 票务核销冲正
  fun reverseVerification(verificationId: String, request: VerificationReverseRequest): VerificationResponse {
    val verificationSeg = urlEncode(verificationId)
    return post("/api/v1/ticketing/verifications/${verificationSeg}/reverse", request)
  }

  inline fun <reified T> get(path: String, terminalTypeOverride: String? = null): T {
    val builder = HttpRequest.newBuilder()
      .uri(URI.create("$baseUrl$path"))
      .timeout(Duration.ofSeconds(30))
      .GET()
    applyHeaders(builder, path, "GET", "", terminalTypeOverride)
    val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    return decode(path, response)
  }

  inline fun <reified RequestBody, reified ResponseBody> post(path: String, body: RequestBody, terminalTypeOverride: String? = null): ResponseBody {
    val encoded = json.encodeToString(body)
    val builder = HttpRequest.newBuilder()
      .uri(URI.create("$baseUrl$path"))
      .timeout(Duration.ofSeconds(30))
      .header("content-type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(encoded))
    applyHeaders(builder, path, "POST", encoded, terminalTypeOverride)
    val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    return decode(path, response)
  }

  inline fun <reified ResponseBody> postJson(path: String, body: JsonObject): ResponseBody {
    val encoded = json.encodeToString(normalizeJsonObjectKeys(body))
    val builder = HttpRequest.newBuilder()
      .uri(URI.create("$baseUrl$path"))
      .timeout(Duration.ofSeconds(30))
      .header("content-type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(encoded))
    applyHeaders(builder, path, "POST", encoded)
    val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    return decode(path, response)
  }

  /** 类型头只来自绑定/登记类型（或私人手机通道的固定声明），不按路径改报；设备签名只跳过登录（U02-001/U07-003）。 */
  fun applyHeaders(builder: HttpRequest.Builder, path: String, method: String, body: String, terminalTypeOverride: String? = null) {
    (terminalTypeOverride ?: terminalType)?.let { builder.header("X-Terminal-Type", it) }
    val id = deviceId
    val signer = deviceSigner
    if (id != null && signer != null && path != "/api/v1/auth/login") {
      deviceRequestHeaders(id, method, path, body, signer).forEach { (name, value) -> builder.header(name, value) }
    }
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
