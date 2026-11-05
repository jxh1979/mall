package com.complex.terminal

// 文件版本：V0.14

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID
import kotlinx.serialization.Serializable
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
  /** 第十八批 N 段第 120 条 120-R1 复核：授权员工那一段请求的令牌，只对发起它的线程有效（[asStaff] 设、还），常驻令牌 [accessToken] 不动。 */
  val staffToken = ThreadLocal<String?>()
  var deviceId: String? = null
  var deviceSigner: ((ByteArray) -> String)? = null
  /** U01-001/U02-001：终端身份由绑定/登记的设备类型决定并只声明一次——应用在绑定成功后写入（staff_work_phone / tablet / kiosk）；
   *  员工私人手机通道固定声明 [STAFF_PRIVATE_PHONE]。未绑定时不发类型头（服务端缺头即拒，fail-close），不按路径改报、不自报 app。 */
  var terminalType: String? = null
  /** 第十八批 N 段第 123 条 123-R4（BSM:1630）：现场动作那一刻取本机定位（纬度, 经度，WGS-84 十进制度），取不到返回 null；
   *  只在现场动作闭集的 POST 上调用（[sensitiveLocationHeaders]），应用启动时接上设备定位。 */
  var locationProvider: (() -> Pair<Double, Double>?)? = null

  fun registerDeviceSigningKey(id: String, publicKey: String, requestKey: String, terminalType: String): JsonObject =
    post("/api/v1/terminals/devices/${urlEncode(id)}/signing-key", mapOf("public_key" to publicKey, "idempotency_key" to requestKey), terminalType)

  fun health(): HealthResponse = get("/api/v1/system/health")

  /** 第十八批 M 段第 116 条：本端可用的员工 App 功能（服务端按可信终端下发）。 */
  fun staffAppFunctions(): StaffAppFunctionsResponse = get("/api/v1/terminals/staff-app-functions")

  fun login(username: String, password: String): LoginResponse =
    post("/api/v1/auth/login", LoginRequest(username, password))

  /** 第十八批 N 段第 120 条 120-R1：授权员工用完即登出（以该员工令牌发，见 [asStaff]）。 */
  fun logout(sessionId: String): JsonObject =
    post("/api/v1/auth/logout", LogoutRequest(UUID.randomUUID().toString(), sessionId))

  /** 第十八批 N 段第 120 条 120-R1：一体机凭绑定时签发的设备令牌读本机配置（是否启用售票、所属商户）。 */
  fun kioskConfig(deviceCode: String, deviceToken: String): KioskConfig {
    val path = "/api/v1/terminals/kiosks/${urlEncode(deviceCode)}/config"
    val builder = HttpRequest.newBuilder()
      .uri(URI.create("$baseUrl$path"))
      .timeout(Duration.ofSeconds(30))
      .header("X-Device-Token", deviceToken)
      .GET()
    applyHeaders(builder, path, "GET", "")
    return decode(path, client.send(builder.build(), HttpResponse.BodyHandlers.ofString()))
  }

  fun bindDevice(request: BindDeviceRequest): DeviceBinding =
    post("/api/v1/terminals/devices/bind", request, request.terminalType)

  fun preview(request: VerificationRequest): PreviewResponse =
    post("/api/v1/ticketing/verifications/preview", request)

  fun verify(request: VerificationRequest): VerificationResponse =
    post("/api/v1/ticketing/verifications", request)

  /** 第十八批 Q 段第 150 条 150-R2：工作手机按入场码中的订台号核邀请函成员入场（路径在现场动作闭集里，自动带定位头）。 */
  fun verifyInvitationEntry(reservationId: String, request: VerificationRequest): BarInvitationEntryRecord =
    post("/api/v1/bar/reservations/${urlEncode(reservationId)}/invitation/entry-verify", request)

  fun reportEvents(request: EventBatchRequest): EventBatchResponse =
    post("/api/v1/evidence/events", request)

  fun returnDevice(request: DeviceReturnRequest): DeviceBinding =
    post("/api/v1/terminals/devices/return", request)

  fun events(limit: Int = 20): List<JsonObject> {
    // /api/v1/evidence/events 的 data 是分页信封 {items,page,page_size,total},非裸数组——取 items。
    val page: JsonObject = get("/api/v1/evidence/events?limit=$limit")
    return (page["items"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()
  }

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

  /** 第十八批 N 段第 120 条 120-R6：检修屏选项目、设备、检修单位，主管确认屏列待确认的检修记录。 */
  fun listAmusementProjects(): List<AmusementProject> = get<Page<AmusementProject>>("/api/v1/amusement/projects").items

  fun listAmusementDevices(projectId: String): List<AmusementDevice> =
    get<Page<AmusementDevice>>("/api/v1/amusement/projects/${urlEncode(projectId)}/devices").items

  fun listInventorySuppliers(): List<InventorySupplier> = get<Page<InventorySupplier>>("/api/v1/inventory/suppliers").items

  fun listAmusementMaintenanceRecords(): List<AmusementMaintenanceRecord> =
    get<Page<AmusementMaintenanceRecord>>("/api/v1/amusement/maintenance-records").items

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
    get<PayrollPage>("/api/v1/staff/payroll/mine?month=${urlEncode(month)}").items

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
  fun listMyLeaves(): List<LeaveRow> = get("/api/v1/staff/leaves/mine")

  /** 2026-10-18 裁决：员工本人对自己已批准的假申请销假（理由必填）→ 待销假审批，主管批准后才生效。 */
  fun requestLeaveRevoke(leaveId: String, reason: String, idempotencyKey: String): LeaveRow =
    post("/api/v1/staff/leaves/${urlEncode(leaveId)}/revoke-request", LeaveRevokeRequest(idempotencyKey, reason))

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

  /** 我的打卡记录(最近 30 天)。 */
  fun listMyAttendance(): List<AttendanceRow> = get("/api/v1/staff/attendance/mine")

  fun listStaffNotices(): List<EmployeeNoticeRecord> = get("/api/v1/staff/notices")

  /** 公告一键已读(留痕)。 */
  fun acknowledgeStaffNotice(id: String, request: NoticeAcknowledgeRequest): EmployeeNoticeRecord =
    post("/api/v1/staff/notices/$id/acknowledge", request)

  /** 我的工单(view_assigned):流转到自己的，加待我提单确认的(第十二批裁决第 13 条)。 */
  fun listMyWorkOrders(): List<MyWorkOrderRecord> = get<MyWorkOrderPage>("/api/v1/facility/work-orders/assigned-to-me").items

  /** 第十八批 M 段第 111 条 111-R1：我的工单一并列出派给本人的商户服务工单（只看）。 */
  fun listMyMerchantWorkOrders(): List<MyMerchantWorkOrderRecord> =
    get<MyMerchantWorkOrderPage>("/api/v1/merchant-service/work-orders/assigned-to-me").items

  /** 我的工单「确认」：待我提单确认的单做提单确认（既有 /review，服务层对承接人放行）。 */
  fun confirmWorkOrder(id: String, request: WorkOrderConfirmationRequest): MyWorkOrderRecord =
    post("/api/v1/facility/work-orders/${urlEncode(id)}/review", request)

  /** 我的工单「退回返工」：提单确认不通过，填原因退回原维修人（既有 /rework）。 */
  fun reworkWorkOrder(id: String, request: WorkOrderConfirmationRequest): MyWorkOrderRecord =
    post("/api/v1/facility/work-orders/${urlEncode(id)}/rework", request)

  /** HP-F01 我的接警：本人此刻当班可接的待接手告警与本人已接手未结的告警（只读，查看不等于接手）。 */
  fun listMyFacilityAlarms(): List<FacilityAlarmRecord> = get<FacilityAlarmPage>("/api/v1/facility/alarms/mine").items

  /** 第十八批 N 段第 120 条 120-R5（BSM:991「查看游乐设施状态：开放、排队、容量、暂停、关闭、故障、维护或安全提醒。」）：一体机游乐设施状态看板（常驻会话即可看）。 */
  fun amusementStatusBoard(): List<AmusementProjectStatus> =
    get<AmusementProjectStatusPage>("/api/v1/amusement/projects/status-board").items

  /** HP-F01 明确接手（「已收到并接手」）：唯一改变接警状态的动作。 */
  fun takeOverFacilityAlarm(id: String, request: FacilityAlarmTakeOverRequest): FacilityAlarmRecord =
    post("/api/v1/facility/alarms/${urlEncode(id)}/take-over", request)

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

  /** 第十八批 N 段第 120 条 120-R7：一体机现金 / POS 登记——票务订单建付款意向、确认收款，订台定金登记（见 [registerKioskPayment]）。 */
  fun createTicketPayIntent(orderId: String, request: PaymentIntentRequest): PaymentIntentRecord =
    post("/api/v1/ticketing/orders/${urlEncode(orderId)}/pay-intents", request)

  fun confirmTicketPayment(orderId: String, request: ConfirmTicketPaymentRequest): JsonObject =
    post("/api/v1/ticketing/orders/${urlEncode(orderId)}/confirm-payment", request)

  fun payReservationDeposit(reservationId: String, request: BarReservationDepositPayRequest): BarReservationRecord =
    post("/api/v1/bar/reservations/${urlEncode(reservationId)}/pay-deposit", request)

  /**
   * 第十八批 N 段第 120 条 120-R2：一体机辅助售票——票种目录（带所属酒吧时只列该酒吧单店票）、给客人手机号发绑定码、下单、出票、取消、
   * 按客人手机号查订单（请求由调用方放在授权服务人员令牌下发，见 [asStaff]）。
   */
  fun listKioskTicketProducts(barId: String?): List<KioskTicketProduct> =
    get<KioskTicketProductPage>("/api/v1/customer/ticket-products" + (barId?.takeIf { it.isNotBlank() }?.let { "?bar_id=${urlEncode(it)}" } ?: "")).items

  fun requestKioskBindCode(phone: String): JsonObject = post("/api/v1/ticketing/kiosk-bind-codes", mapOf("phone" to phone))

  fun createTicketOrder(request: KioskTicketOrderRequest): TicketOrderRow = post("/api/v1/ticketing/orders", request)

  fun issueTicketOrder(orderId: String, request: TicketOrderActionRequest): JsonObject =
    post("/api/v1/ticketing/orders/${urlEncode(orderId)}/issue", request)

  fun cancelTicketOrder(orderId: String, request: TicketOrderActionRequest): TicketOrderRow =
    post("/api/v1/ticketing/orders/${urlEncode(orderId)}/cancel", request)

  fun listTicketOrdersByPhone(phone: String): List<TicketOrderRow> =
    get<TicketOrderRowPage>("/api/v1/ticketing/orders?buyer_phone=${urlEncode(phone)}").items

  fun bindStaffDeviceCredential(request: StaffDeviceBindRequest): StaffDeviceCredentialRecord =
    post("/api/v1/staff/device-credentials", request, terminalType ?: STAFF_PRIVATE_PHONE)

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

  // ── 第十八批 I 段第 84 条 84-R2：酒水与物品交接扫码（PGM 第 633 行「记录出库、后厨接收、服务员取走和送达」；BSM 第 1201 行）──

  /** 按扫到的溯源码查交接单（新建在前）。 */
  fun listTraceHandovers(traceCode: String): List<TraceHandoverRecord> =
    get("/api/v1/inventory/trace-handovers?trace_code=${urlEncode(traceCode)}")

  /** 第十八批 K 段第 94 条 94-R2（BSM 第 1197 行「出库须同时核验订单和物品码」）：库管出库前按溯源码列含该物品待出品明细的酒吧订单。 */
  fun listOutboundOrders(code: String): List<TraceOutboundOrder> =
    get("/api/v1/inventory/trace-codes/${urlEncode(code)}/outbound-orders")

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

  // ── 第十八批 M 段第 112 条 112-R1（BSM:947「工作手机扫描权益二维码，核销赠饮、礼物或其他领取资格。」）：礼物（券）查券与核销；
  //    112-R2 一体机权益领取屏同用 ──

  /** 按客人出示的券码查券（只读：不动券、库存与码）。 */
  fun previewVoucherRedeem(code: String): VoucherRedeemPreview =
    post("/api/v1/vouchers/redemptions/preview", VoucherRedeemPreviewRequest(code))

  /** 券核销（店内实物券带逐件扫的物品码）。 */
  fun redeemVoucher(request: VoucherRedeemRequest): VoucherRedemption =
    post("/api/v1/vouchers/redemptions", request)

  /** 第 84 条 84-R1（BSM 第 1201 行「扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）」）：工作手机签名上传交接补录证据。
   *  business_type 固定 manual_backfill；签名摘要取部件规范串（[multipartBodyDigest]）。
   *  ponytail: 整个文件读进内存，上限就是服务端 50MB，以后要更大再改流式。 */
  fun uploadEvidence(fileName: String, bytes: ByteArray): EvidenceAttachmentRecord {
    val path = "/api/v1/evidence/attachments/upload"
    val fields = listOf("business_type" to "manual_backfill")
    val files = listOf(Triple("file", fileName, bytes))
    val boundary = "complex" + UUID.randomUUID().toString().replace("-", "")
    val builder = HttpRequest.newBuilder()
      .uri(URI.create("$baseUrl$path"))
      .timeout(Duration.ofSeconds(30))
      .header("content-type", "multipart/form-data; boundary=$boundary")
      .POST(HttpRequest.BodyPublishers.ofByteArray(multipartFormBody(boundary, fields, files)))
    applyHeaders(builder, path, "POST", "", bodyDigest = multipartBodyDigest(fields, files))
    val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    return decode(path, response)
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
  fun applyHeaders(builder: HttpRequest.Builder, path: String, method: String, body: String, terminalTypeOverride: String? = null,
      bodyDigest: String? = null) {
    (terminalTypeOverride ?: terminalType)?.let { builder.header("X-Terminal-Type", it) }
    sensitiveLocationHeaders(method, path, locationProvider).forEach { (name, value) -> builder.header(name, value) }
    val id = deviceId
    val signer = deviceSigner
    if (id != null && signer != null && path != "/api/v1/auth/login") {
      (if (bodyDigest != null) deviceRequestHeadersWithDigest(id, method, path, bodyDigest, signer)
        else deviceRequestHeaders(id, method, path, body, signer)).forEach { (name, value) -> builder.header(name, value) }
    }
    (staffToken.get() ?: accessToken)?.takeIf { it.isNotBlank() }?.let { builder.header("authorization", "Bearer $it") }
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

@Serializable
private data class PayrollPage(val items: List<PayrollStatementRow>)

@Serializable
private data class FacilityAlarmPage(val items: List<FacilityAlarmRecord> = emptyList())

@Serializable
private data class MyWorkOrderPage(val items: List<MyWorkOrderRecord> = emptyList())

@Serializable
private data class MyMerchantWorkOrderPage(val items: List<MyMerchantWorkOrderRecord> = emptyList())

@Serializable
private data class AmusementProjectStatusPage(val items: List<AmusementProjectStatus> = emptyList())

@Serializable
private data class Page<T>(val items: List<T> = emptyList())

@Serializable
private data class KioskTicketProductPage(val items: List<KioskTicketProduct> = emptyList())

@Serializable
private data class TicketOrderRowPage(val items: List<TicketOrderRow> = emptyList())
