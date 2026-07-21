package com.complex.terminal

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.Serializable
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

  // ── BYOD 一期(2026-07-02):员工自有手机四功能(与 JVM 版签名一致) ──

  fun submitStaffRegistration(request: StaffRegistrationSubmitRequest): StaffProfileRecord =
    post("/api/v1/staff/registrations", request)

  // ── G批:服务点单(公司代收口径:收款码原文由服务端返回,App 重绘 QR) ──

  fun listServiceCatalog(): List<ServiceCatalogRow> = get<Page<ServiceCatalogRow>>("/api/v1/services/catalog").items

  fun createServiceOrder(request: ServiceOrderCreateReq): ServiceOrderRow =
    post("/api/v1/services/orders", request)

  fun listMyServiceOrders(): List<ServiceOrderRow> = get<Page<ServiceOrderRow>>("/api/v1/services/orders/assigned-to-me").items

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
    get<Page<PayrollStatementRow>>("/api/v1/staff/payroll/mine?month=$month").items

  /** 提交请假(事假/病假/调休/年假;主管审)。 */
  fun submitLeave(request: LeaveSubmitRequest): LeaveRow =
    post("/api/v1/staff/leaves", request)

  /** 我的请假列表。 */
  fun listMyLeaves(): List<LeaveRow> = get<Page<LeaveRow>>("/api/v1/staff/leaves/mine").items

  /** 考勤月视图(六态逐日)。 */
  fun attendanceMonthView(month: String): List<AttendanceDayRow> =
    get("/api/v1/staff/attendance/month-view?month=$month")

  // ── BYOD 二期 Wave1:工资条提成明细 + 本人资料自助(只返本人;身份按 token 派生) ──

  /** 本人某月提成明细(酒水/卡座 + 服务点单),与工资条 commissionCent 对账。 */
  fun listMyPayrollCommissions(month: String): List<StaffCommissionLineRow> =
    get<Page<StaffCommissionLineRow>>("/api/v1/staff/payroll/mine/commissions?month=$month").items

  /** 本人档案(姓名/状态)。 */
  fun myStaffProfile(): StaffProfileRecord = get("/api/v1/staff/profiles/mine")

  /** 改本人有限字段(姓名)。 */
  fun updateMyStaffProfile(request: StaffProfileSelfUpdateRequest): StaffProfileRecord =
    post("/api/v1/staff/profiles/mine", request)

  // ── BYOD 二期 E 批:排班/考勤 ──

  /** 主管排班(双端;同员工同开始时刻幂等覆盖)。 */
  fun upsertStaffShift(request: StaffShiftUpsertRequest): StaffShiftRow =
    post("/api/v1/staff/shifts", request)

  /** 我的班次与点位(今天起 7 天,时间到分钟)。 */
  fun listMyShifts(): List<StaffShiftRow> = get<Page<StaffShiftRow>>("/api/v1/staff/shifts/mine").items

  /** 考勤打卡(NFC/扫码;时间以服务器为准;严格无宽限判定随响应返回)。 */
  fun punchAttendance(request: AttendancePunchRequest): AttendanceRow =
    post("/api/v1/staff/attendance/punch", request)

  /** 我的打卡记录(最近 30 天)。 */
  fun listMyAttendance(): List<AttendanceRow> = get<Page<AttendanceRow>>("/api/v1/staff/attendance/mine").items

  fun listStaffNotices(): List<EmployeeNoticeRecord> = get<Page<EmployeeNoticeRecord>>("/api/v1/staff/notices").items

  fun acknowledgeStaffNotice(id: String, request: NoticeAcknowledgeRequest): EmployeeNoticeRecord =
    post("/api/v1/staff/notices/$id/acknowledge", request)

  fun listMyWorkOrders(): List<MyWorkOrderRecord> = get<Page<MyWorkOrderRecord>>("/api/v1/facility/work-orders/assigned-to-me").items

  fun listBarTableAvailability(): List<BarTableAvailabilityRow> = get<Page<BarTableAvailabilityRow>>("/api/v1/bar/tables/availability").items

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

  fun createBarOrderBill(orderId: String, request: BarOrderBillCreateRequest): BarBillRecord =
    post("/api/v1/bar/orders/${urlEncode(orderId)}/bill", request)

  /** 鉴权回读目录图片（带 Bearer，返回原始字节供 UI 解码为 Bitmap）。 */
  fun catalogImageBytes(imageUrl: String): ByteArray {
    val path = if (imageUrl.startsWith("/")) imageUrl else "/$imageUrl"
    val connection = open(path, "GET")
    return try {
      val status = connection.responseCode
      if (status !in 200..299) throw ApiException(status, path, "")
      connection.inputStream.readBytes()
    } finally {
      connection.disconnect()
    }
  }

  fun createBarReservation(request: BarReservationCreateRequest): BarReservationRecord =
    post("/api/v1/bar/reservations", request)

  fun events(limit: Int = 20): List<JsonObject> = get<Page<JsonObject>>("/api/v1/evidence/events?limit=$limit").items

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
    // 3.19 二期：声明终端类型，接受 terminal.enabled 总开关逐请求后端强制（矩阵页关端=接口硬拒）。
    connection.setRequestProperty("X-Terminal-Type", "staff_work_phone")
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

@Serializable
private data class Page<T>(val items: List<T> = emptyList())
