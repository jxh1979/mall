package com.complex.terminal

// 文件版本：V0.10

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
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
/** U01-001/U01-007：员工私人手机通道的终端码（与 JVM 版 TerminalApiClient 常量同值；该文件不在 Android 源集内）。 */
const val STAFF_PRIVATE_PHONE = "staff_private_phone"

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
  var deviceId: String? = null
  var deviceSigner: ((ByteArray) -> String)? = null
  /** U01-001：终端身份由本机绑定类型决定并只声明一次（工作手机/平板）；未绑定不发类型头（服务端 fail-close）。 */
  var terminalType: String? = null

  fun registerDeviceSigningKey(id: String, publicKey: String, requestKey: String): JsonObject =
    post("/api/v1/terminals/devices/${urlEncode(id)}/signing-key", mapOf("public_key" to publicKey, "idempotency_key" to requestKey))

  fun login(username: String, password: String): LoginResponse =
    post("/api/v1/auth/login", LoginRequest(username, password))

  /** 第 23 条 23-R6：员工手机在线屏打开即发暗纹码并留痕（与后台、商户专区同一接口；走设备签名，设备号由服务端按验签结果取登记设备号）。 */
  fun postPageView(request: PageViewRequest): PageViewMark =
    post("/api/v1/security/page-views", request)

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

  // ── U01-007（PGM L579/BSM L935）：公司员工本人在私人手机 App 注册；两条公开路径只声明私人手机终端码，
  //    设备公钥/指纹随请求体提交并在服务端绑定本人（无设备证明即 4xx）。未绑定前无设备编号，不做设备签名（公开路径）。 ──
  fun listStaffRegistrationCompanies(): List<StaffRegistrationCompany> =
    get("/api/v1/staff/registration-companies", STAFF_PRIVATE_PHONE)

  fun submitStaffSelfRegistration(request: StaffSelfRegistrationRequest): StaffRegistrationReceipt =
    post("/api/v1/staff/self-registrations", request, STAFF_PRIVATE_PHONE)

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

  fun myServiceStats(month: String): JsonObject = get("/api/v1/services/orders/my-stats?month=$month")

  // ── BYOD 三期 F 批:我的薪资/请假/考勤月视图 ──

  /** 我的薪资(仅已月结定格月份;未结月为空——拍板:月结后才可见)。 */
  fun listMyPayroll(month: String): List<PayrollStatementRow> =
    get<Page<PayrollStatementRow>>("/api/v1/staff/payroll/mine?month=$month").items

  fun myPerformanceAdjustments(month: String): List<PayrollScoreAdjustment> =
    get("/api/v1/staff/payroll/mine/performance-adjustments?month=${urlEncode(month)}")

  fun appealPerformance(adjustmentId: String, reason: String, idempotencyKey: String): PayrollScoreAdjustment =
    post("/api/v1/staff/payroll/performance-adjustments/${urlEncode(adjustmentId)}/appeals", PayrollAppealRequest(idempotencyKey, reason))

  /** 仅本人已定格工资原件，不接受员工身份参数。 */
  fun myPayrollDetail(statementId: String): PayrollStatementDetail =
    get("/api/v1/staff/payroll/mine/statements/${urlEncode(statementId)}")

  /** 提交请假(事假/病假/调休/年假;主管审)。 */
  fun submitLeave(request: LeaveSubmitRequest): LeaveRow =
    post("/api/v1/staff/leaves", request)

  /** 我的请假列表。 */
  fun listMyLeaves(): List<LeaveRow> = get<Page<LeaveRow>>("/api/v1/staff/leaves/mine").items

  /** 2026-10-18 裁决：员工本人对自己已批准的假申请销假（理由必填）→ 待销假审批，主管批准后才生效。 */
  fun requestLeaveRevoke(leaveId: String, reason: String, idempotencyKey: String): LeaveRow =
    post("/api/v1/staff/leaves/${urlEncode(leaveId)}/revoke-request", LeaveRevokeRequest(idempotencyKey, reason))

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
  fun attendanceAnchor(request: AttendanceAnchorRequest): AttendanceAnchorResponse =
    post("/api/v1/staff/attendance/time-anchors", request)

  fun attendanceReceipt(request: AttendanceReceiptRequest): AttendanceReceiptEvent =
    post("/api/v1/staff/attendance/receipt-events", request)

  fun currentWorkPhoneContext(): WorkPhoneAttendanceContext =
    get("/api/v1/staff/attendance/work-phone-context")

  fun historicalWorkPhoneContexts(userId: String): List<WorkPhoneAttendanceContext> =
    get("/api/v1/staff/attendance/work-phone-contexts?user_id=${urlEncode(userId)}")

  fun recoverAttendanceReceipt(request: AttendanceReceiptRecoveryRequest): AttendanceReceiptEvent =
    post("/api/v1/staff/attendance/receipt-events/recover",request)

  fun attendanceReceiptEvents(userId: String? = null): List<AttendanceReceiptEvent> =
    get("/api/v1/staff/attendance/receipt-events" + (userId?.takeIf { it.isNotBlank() }?.let { "?user_id=${urlEncode(it)}" } ?: ""))

  fun submitAttendanceReceiptReview(eventId: String, request: AttendanceReceiptReviewRequest): AttendanceReceiptReview =
    post("/api/v1/staff/attendance/receipt-events/${urlEncode(eventId)}/review", request)

  fun attendanceReceiptReviewHistory(eventId: String): List<AttendanceReceiptReview> =
    get("/api/v1/staff/attendance/receipt-events/${urlEncode(eventId)}/reviews")

  fun swapWorkPhone(request: WorkPhoneSwapRequest): DeviceBinding =
    post("/api/v1/terminals/devices/work-phone-swap", request)

  fun punchAttendance(request: AttendancePunchRequest): AttendanceRow =
    post("/api/v1/staff/attendance/punch", request)

  fun listStaffNotices(): List<EmployeeNoticeRecord> = get<Page<EmployeeNoticeRecord>>("/api/v1/staff/notices").items

  fun acknowledgeStaffNotice(id: String, request: NoticeAcknowledgeRequest): EmployeeNoticeRecord =
    post("/api/v1/staff/notices/$id/acknowledge", request)

  fun listMyWorkOrders(): List<MyWorkOrderRecord> = get<Page<MyWorkOrderRecord>>("/api/v1/facility/work-orders/assigned-to-me").items

  /** 第十二批裁决第 13 条：我的工单里「待我提单确认」的单——确认（/review）与退回返工（/rework，带不通过原因）。 */
  fun confirmWorkOrder(id: String, request: WorkOrderConfirmationRequest): MyWorkOrderRecord =
    post("/api/v1/facility/work-orders/${urlEncode(id)}/review", request)

  fun reworkWorkOrder(id: String, request: WorkOrderConfirmationRequest): MyWorkOrderRecord =
    post("/api/v1/facility/work-orders/${urlEncode(id)}/rework", request)

  fun listBarTableAvailability(): List<BarTableAvailabilityRow> = get<Page<BarTableAvailabilityRow>>("/api/v1/bar/tables/availability").items

  /** HP-F01 我的接警：本人此刻当班可接的待接手告警与本人已接手未结的告警（只读，查看不等于接手）。 */
  fun listMyFacilityAlarms(): List<FacilityAlarmRecord> = get<Page<FacilityAlarmRecord>>("/api/v1/facility/alarms/mine").items

  /** HP-F01 明确接手（「已收到并接手」）：唯一改变接警状态的动作。 */
  fun takeOverFacilityAlarm(id: String, request: FacilityAlarmTakeOverRequest): FacilityAlarmRecord =
    post("/api/v1/facility/alarms/${urlEncode(id)}/take-over", request)

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

  // ── 第十八批 I 段第 84 条 84-R2：酒水与物品交接扫码（PGM 第 633 行「记录出库、后厨接收、服务员取走和送达」；BSM 第 1201 行）──

  /** 按扫到的溯源码查交接单（新建在前）。 */
  fun listTraceHandovers(traceCode: String): List<TraceHandoverRecord> =
    get("/api/v1/inventory/trace-handovers?trace_code=${urlEncode(traceCode)}")

  /** 交接链：表头、五段原扫码记录与人工补录单并列。 */
  fun getTraceHandover(id: String): TraceHandoverChain = get("/api/v1/inventory/trace-handovers/${urlEncode(id)}")

  /** 库管出库前建交接单（device_id＝本机登记设备号）。 */
  fun createTraceHandover(request: TraceHandoverCreateRequest): TraceHandoverChain =
    post("/api/v1/inventory/trace-handovers", request)

  /** 逐段扫码确认一个人工段。 */
  fun scanTraceSegment(id: String, request: TraceScanSegmentRequest): TraceHandoverChain =
    post("/api/v1/inventory/trace-handovers/${urlEncode(id)}/scan-segment", request)

  /** 缺段人工补录（附证据，进主管复核）。 */
  fun submitTraceStepBackfill(id: String, segment: String, request: TraceStepBackfillRequest): TraceHandoverBackfillRow =
    post("/api/v1/inventory/trace-handovers/${urlEncode(id)}/segments/${urlEncode(segment)}/backfill", request)

  /** 第 84 条 84-R1（BSM 第 1201 行「扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）」）：工作手机签名上传交接补录证据。
   *  business_type 固定 manual_backfill；签名摘要取部件规范串（[multipartBodyDigest]）。
   *  ponytail: 整个文件读进内存，上限就是服务端 50MB，以后要更大再改流式。 */
  fun uploadEvidence(fileName: String, bytes: ByteArray): EvidenceAttachmentRecord {
    val path = "/api/v1/evidence/attachments/upload"
    val fields = listOf("business_type" to "manual_backfill")
    val files = listOf(Triple("file", fileName, bytes))
    val boundary = "complex" + UUID.randomUUID().toString().replace("-", "")
    val connection = open(path, "POST")
    connection.setRequestProperty("content-type", "multipart/form-data; boundary=$boundary")
    connection.doOutput = true
    return send(path, connection, body = null, multipart = multipartFormBody(boundary, fields, files),
      bodyDigest = multipartBodyDigest(fields, files))
  }

  // --------------------------------------------------------------------------
  // 传输层（HttpURLConnection）
  // --------------------------------------------------------------------------

  private inline fun <reified T> get(path: String, terminalTypeOverride: String? = null): T {
    val connection = open(path, "GET", terminalTypeOverride)
    return send(path, connection, body = null)
  }

  private inline fun <reified RequestBody, reified ResponseBody> post(path: String, body: RequestBody, terminalTypeOverride: String? = null): ResponseBody {
    val connection = open(path, "POST", terminalTypeOverride)
    connection.setRequestProperty("content-type", "application/json")
    connection.doOutput = true
    return send(path, connection, json.encodeToString(body))
  }

  @PublishedApi
  internal fun open(path: String, method: String, terminalTypeOverride: String? = null): HttpURLConnection {
    val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection // nosemgrep: baseUrl is validated to https/loopback in init.
    connection.requestMethod = method
    connection.connectTimeout = connectTimeoutMs
    connection.readTimeout = readTimeoutMs
    // 3.19 二期：声明终端类型，接受 terminal.enabled 总开关逐请求后端强制（矩阵页关端=接口硬拒）。
    (terminalTypeOverride ?: terminalType)?.let { connection.setRequestProperty("X-Terminal-Type", it) }
    accessToken?.takeIf { it.isNotBlank() }?.let { connection.setRequestProperty("authorization", "Bearer $it") }
    return connection
  }

  @PublishedApi
  internal inline fun <reified T> send(path: String, connection: HttpURLConnection, body: String?,
      multipart: ByteArray? = null, bodyDigest: String? = null): T {
    try {
      val id = deviceId
      val signer = deviceSigner
      if (id != null && signer != null && path != "/api/v1/auth/login") {
        (if (bodyDigest != null) deviceRequestHeadersWithDigest(id, connection.requestMethod, path, bodyDigest, signer)
          else deviceRequestHeaders(id, connection.requestMethod, path, body ?: "", signer))
          .forEach { (name, value) -> connection.setRequestProperty(name, value) }
      }
      val bytes = multipart ?: body?.toByteArray(StandardCharsets.UTF_8)
      if (bytes != null) {
        connection.outputStream.use { it.write(bytes) }
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
