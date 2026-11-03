package com.complex.terminal

// 文件版本：V0.12

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
  val ok: Boolean = false,
)

@Serializable
data class LoginRequest(
  val loginName: String,
  val password: String,
)

@Serializable
data class ApiUser(
  val id: String = "",
  val username: String = "",
  val accountType: String = "",
  val organizationId: String? = null,
  val merchantId: String? = null,
)

@Serializable
data class LoginResponse(
  val accessToken: String = "",
  val refreshToken: String = "",
  val sessionId: String = "",
  val tokenType: String = "Bearer",
  val user: ApiUser = ApiUser(),
)

/** 第十八批 N 段第 120 条 120-R1：一体机上授权员工用完即登出（会话号取该员工的登录响应）。 */
@Serializable
data class LogoutRequest(
  val idempotencyKey: String,
  val sessionId: String,
)

@Serializable
data class TerminalSession(
  val accessToken: String,
  val staffId: String,
  val username: String,
)

@Serializable
data class BindDeviceRequest(
  @SerialName("device_id")
  val deviceId: String,
  @SerialName("terminal_type")
  val terminalType: String = "staff_work_phone",
  @SerialName("binding_scope")
  val bindingScope: String = "space",
  @SerialName("bound_space_id")
  val boundSpaceId: String? = null,
  @SerialName("mdm_device_ref")
  val mdmDeviceRef: String? = null,
  @SerialName("kiosk_mode")
  val kioskMode: String? = null,
  /** U07-001：工作手机领用即绑定持有员工（员工档案编号），验签按 device.staffId 收口。 */
  @SerialName("staff_id")
  val staffId: String? = null,
  /** 第 96 条 96-R1（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态。」）：设备型号，领用时带本机型号；不传后端沿用台账原值。 */
  @SerialName("device_model")
  val deviceModel: String? = null,
  @SerialName("idempotency_key")
  val idempotencyKey: String = "",
)

@Serializable
data class DeviceBinding(
  val id: String = "",
  val deviceType: String = "",
  val deviceCode: String = "",
  val imei: String? = null,
  val serialNo: String? = null,
  val merchantId: String? = null,
  val venueId: String? = null,
  val areaId: String? = null,
  val locationPointId: String? = null,
  val status: String = "",
  /** 第十八批 N 段第 120 条 120-R1：一体机绑定时服务端签发的设备令牌（读本机配置用）；其他终端为空。 */
  val deviceToken: String? = null,
  val kioskMode: String? = null,
)

/**
 * 第十八批 N 段第 120 条 120-R1（BSM:980「……其中只有启用售票能力的一体机，才允许授权服务人员在到店服务场景下辅助客人购票。」）：
 * 一体机本机配置，服务端按设备台账给——售票型一体机且后台为本机开了售票开关才 [ticketingEnabled]；[merchantId]＝后台设备台账页设的所属商户。
 */
@Serializable
data class KioskConfig(
  val deviceId: String = "",
  val kioskMode: String? = null,
  val ticketingEnabled: Boolean = false,
  val boundScope: String? = null,
  val merchantId: String? = null,
)

/**
 * 第十八批 N 段第 120 条 120-R5（BSM:991「查看游乐设施状态：开放、排队、容量、暂停、关闭、故障、维护或安全提醒。」）：游乐设施状态看板一行——
 * 开放状态、容量、在玩人数、排队人数、维护中设备数、未结故障数、当前生效的安全提醒标题（服务端不给客人编号与排队号）。
 */
@Serializable
data class AmusementProjectStatus(
  val projectId: String = "",
  val projectName: String? = null,
  val openStatus: String? = null,
  val capacity: Int = 0,
  val occupiedCount: Int = 0,
  val waitingCount: Int = 0,
  val maintenanceDeviceCount: Int = 0,
  val faultCount: Int = 0,
  val safetyNotices: List<String> = emptyList(),
)

@Serializable
data class OfflineEntitlement(
  val id: String = "",
  val entitlementCode: String = "",
  val entitlementType: String = "",
  val merchantId: String? = null,
  val venueId: String? = null,
  val projectId: String? = null,
  val totalTimes: Int = 0,
  val usedTimes: Int = 0,
  val valueAmount: Int = 0,
  val status: String = "",
  val validFrom: String? = null,
  val validTo: String? = null,
  val updatedAt: String? = null,
)

@Serializable
data class OfflinePackageResponse(
  val generatedAt: String = "",
  val expiresAt: String = "",
  val entitlements: List<OfflineEntitlement> = emptyList(),
)

@Serializable
data class PreviewResponse(
  val allowed: Boolean = false,
  val reason: String = "",
  val entitlement: OfflineEntitlement? = null,
  // TK-06：一人多次票且人脸供应商可用时，capture=本次首次留脸（须客人同意并拍照），match=按留存人脸比对（须拍照）；其余为空。
  val faceBinding: String? = null,
)

@Serializable
data class VerificationRequest(
  val idempotencyKey: String,
  val credentialType: String = "qr",
  val credentialPayload: String,
  val deviceId: String,
  val pointId: String,
  val businessContext: Map<String, String> = emptyMap(),
  // TK-06：一人多次票入场拍下的人脸（base64）与首次留脸时客人的单独同意；其余入场不带（空值不上送）。
  val faceCapture: String? = null,
  val faceConsentAccepted: Boolean? = null,
)

@Serializable
data class VerificationResponse(
  val id: String = "",
  val verificationNo: String = "",
  val status: String = "",
  val resultReason: String? = null,
)

enum class LocalTaskStatus {
  PENDING,
  SYNCED,
  CONFLICT,
}

@Serializable
data class LocalTask(
  val clientTaskId: String,
  val taskType: String,
  val payload: Map<String, String>,
  val occurredAt: String,
  val status: LocalTaskStatus = LocalTaskStatus.PENDING,
  // 归属固化:任务创建时记录原操作员(staffId)与设备(deviceCode)。换人后新员工
  // 同步时按 originStaffId 校验,不得把上一员工的待同步任务冒充自己身份上送。
  val originStaffId: String? = null,
  val originDeviceCode: String? = null,
)

@Serializable
data class SyncTaskRequest(
  val clientTaskId: String,
  val taskType: String,
  val occurredAt: String,
  val payload: Map<String, String>,
)

@Serializable
data class SyncTasksRequest(
  val idempotencyKey: String,
  val operatorNote: String? = null,
  val deviceCode: String,
  val staffId: String? = null,
  val tasks: List<SyncTaskRequest>,
)

@Serializable
data class SyncTaskResult(
  val clientTaskId: String = "",
  val status: String = "",
  val reason: String? = null,
  val verificationId: String? = null,
  val verificationNo: String? = null,
  val riskAlertId: String? = null,
)

@Serializable
data class SyncTasksResponse(
  val syncedAt: String = "",
  val results: List<SyncTaskResult> = emptyList(),
)

@Serializable
data class TerminalEvent(
  @SerialName("event_type")
  val eventType: String,
  @SerialName("event_category")
  val eventCategory: String = "android_terminal",
  val result: String = "success",
  val summary: String,
  val payload: Map<String, String> = emptyMap(),
  @SerialName("occurred_at")
  val occurredAt: String,
)

@Serializable
data class EventBatchRequest(
  @SerialName("idempotency_key")
  val idempotencyKey: String = "",
  @SerialName("device_code")
  val deviceCode: String,
  @SerialName("staff_id")
  val staffId: String? = null,
  val events: List<TerminalEvent>,
)

@Serializable
data class EventBatchResponse(
  val accepted: Int = 0,
  val receivedAt: String = "",
)

@Serializable
data class DeviceReturnRequest(
  @SerialName("idempotency_key")
  val idempotencyKey: String = "",
  @SerialName("device_code")
  val deviceCode: String,
  @SerialName("staff_id")
  val staffId: String? = null,
  @SerialName("has_pending_tasks")
  val hasPendingTasks: Boolean = false,
  val reason: String? = null,
  val operatorNote: String? = null,
  val evidenceAttachmentIds: List<String> = emptyList(),
)

enum class ReturnStatus {
  BLOCKED_PENDING_TASKS,
  RETURNED,
}

@Serializable
data class ReturnDecision(
  val status: ReturnStatus,
  val pendingTaskCount: Int,
  val remoteStatus: String? = null,
)

// ---------------------------------------------------------------------------
// 游乐核销 POST /api/v1/amusement/verifications
// 复用 ticketing 的 VerificationRequest 契约（idempotency_key/credential_type/
// credential_payload/device_id/point_id/business_context）。
// 响应为 AmusementVerificationRecord。
// ---------------------------------------------------------------------------
@Serializable
data class AmusementVerificationRecord(
  val id: String = "",
  val projectId: String? = null,
  val sessionId: String? = null,
  val deviceId: String? = null,
  val staffId: String? = null,
  val verificationResult: String = "",
  val failureReason: String? = null,
  val evidenceAttachmentIds: List<String> = emptyList(),
  val createdAt: String? = null,
)

// ---------------------------------------------------------------------------
// 游乐检修 POST /api/v1/amusement/maintenance-records
// ---------------------------------------------------------------------------
@Serializable
data class AmusementMaintenanceRecordRequest(
  val idempotencyKey: String,
  val projectId: String? = null,
  val deviceId: String? = null,
  val locationPointId: String? = null,
  val maintenanceVendorId: String? = null,
  val engineerId: String? = null,
  val engineerName: String? = null,
  val maintenanceAt: String? = null,
  val result: String? = null,
  val exceptionNote: String? = null,
  val sourceTerminal: String? = null,
)

/** 第十八批 N 段第 120 条 120-R6：检修屏选项目、设备、检修单位（GET /api/v1/amusement/projects、/projects/{id}/devices、/api/v1/inventory/suppliers）。 */
@Serializable
data class AmusementProject(val id: String = "", val projectName: String? = null, val openStatus: String? = null)

/** 120-R6：游乐设备（amusement_devices）；检修记录的设备填 [deviceCode]（桥接旧 device_id 字符串）。 */
@Serializable
data class AmusementDevice(val id: String = "", val deviceCode: String = "", val name: String? = null, val status: String? = null)

/** 120-R6：供应商（检修单位作为供应商记录，BSM:992）。 */
@Serializable
data class InventorySupplier(val id: String = "", val supplierName: String? = null, val status: String? = null)

// 游乐检修主管确认恢复 POST .../maintenance-records/{id}/confirm-resume
// 注意：此处用 remark（非 operator_note）。
@Serializable
data class AmusementMaintenanceResumeRequest(
  val idempotencyKey: String,
  val remark: String? = null,
)

@Serializable
data class AmusementMaintenanceRecord(
  val id: String = "",
  val projectId: String? = null,
  val deviceId: String? = null,
  val locationPointId: String? = null,
  val maintenanceVendorId: String? = null,
  val engineerId: String? = null,
  val engineerName: String? = null,
  val maintenanceAt: String? = null,
  val result: String? = null,
  val exceptionNote: String? = null,
  val sourceTerminal: String? = null,
  val status: String = "",
  val supervisorConfirmedBy: String? = null,
  val confirmedAt: String? = null,
  val resumeBusinessAt: String? = null,
  val createdAt: String? = null,
)

// ---------------------------------------------------------------------------
// 酒吧 一体机/平板 点酒 POST /api/v1/bar/orders
// ---------------------------------------------------------------------------
@Serializable
data class BarOrderRequest(
  val idempotencyKey: String,
  val operatorNote: String? = null,
  val evidenceAttachmentIds: List<String> = emptyList(),
  val barId: String? = null,
  val tableId: String? = null,
  val reservationId: String? = null,
  val items: List<Map<String, String>> = emptyList(),
  val serviceUserIds: List<String> = emptyList(),
  val paymentMode: String? = null,
)

@Serializable
data class BarOrderRecord(
  val id: String = "",
  val barId: String? = null,
  val tableId: String? = null,
  val reservationId: String? = null,
  val customerId: String? = null,
  val orderSource: String? = null,
  val status: String = "",
  val totalAmountCent: Long = 0,
  val currency: String? = null,
  val createdTerminal: String? = null,
  val deviceId: String? = null,
  val salesStaffId: String? = null,
  val orderType: String? = null,
  val createdAt: String? = null,
  val updatedAt: String? = null,
)

// 酒吧账单支付意图 POST /api/v1/bar/bills/{bill_id}/pay-intents
@Serializable
data class PaymentIntentRequest(
  val idempotencyKey: String,
  val operatorNote: String? = null,
  val evidenceAttachmentIds: List<String> = emptyList(),
  val businessOrderType: String? = null,
  val businessOrderId: String? = null,
  val amountCent: Long? = null,
  val currency: String? = null,
  val channel: String? = null,
)

@Serializable
data class PaymentIntentRecord(
  val id: String = "",
  val businessOrderType: String? = null,
  val businessOrderId: String? = null,
  val amountCent: Long = 0,
  val currency: String? = null,
  val channel: String? = null,
  val status: String = "",
  val createdAt: String? = null,
  val updatedAt: String? = null,
)

// 酒吧账单结算 POST /api/v1/bar/bills/{bill_id}/settle
@Serializable
data class BarBillSettleRequest(
  val idempotencyKey: String,
  val operatorNote: String? = null,
  val evidenceAttachmentIds: List<String> = emptyList(),
  val paymentId: String? = null,
  val settlementAmountCent: Long? = null,
  val paymentChannel: String? = null,
  val cashierStaffId: String? = null,
  val posTransactionNo: String? = null,
)

@Serializable
data class BarBillRecord(
  val id: String = "",
  val billNo: String? = null,
  val orderId: String? = null,
  val barId: String? = null,
  val tableId: String? = null,
  val reservationId: String? = null,
  val payableAmountCent: Long = 0,
  val paidAmountCent: Long = 0,
  val currency: String? = null,
  val paymentStatus: String? = null,
  val settlementStatus: String? = null,
  val createdAt: String? = null,
  val updatedAt: String? = null,
)

// ---------------------------------------------------------------------------
// 员工设备凭证 POST /api/v1/staff/device-credentials
// ---------------------------------------------------------------------------
@Serializable
data class StaffDeviceBindRequest(
  val idempotencyKey: String,
  val operatorNote: String? = null,
  val staffProfileId: String? = null,
  val devicePublicKey: String? = null,
  val deviceFingerprintHash: String? = null,
)

// 主管批准换机 POST .../device-credentials/{id}/approve-rebind
@Serializable
data class StaffDeviceRebindDecisionRequest(
  val idempotencyKey: String,
  val operatorNote: String? = null,
)

// 设备完整性上报 POST .../device-credentials/{id}/report-integrity
@Serializable
data class StaffDeviceIntegrityReportRequest(
  val idempotencyKey: String,
  val operatorNote: String? = null,
  val integrityStatus: String? = null,
)

/** 第十八批 M 段第 116 条：服务端按可信终端下发的员工 App 功能码（GET /api/v1/terminals/staff-app-functions）。 */
@Serializable
data class StaffAppFunctionsResponse(
  val terminalType: String? = null,
  val functions: List<String> = emptyList(),
)

/** 第十八批 M 段第 116 条（PGM:747「……能做什么由后台按设备类型统一下发……」；BSM:936「员工安装私人手机 App 时生成并绑定一个该手机专属的设备标识……」）：
 *  首页是否显示该功能。本机还没声明身份时只给建立身份的入口（员工设备凭证、平板点单 POS、一体机模式——第十八批 M 段 112-R2）；已声明身份按服务端下发的清单，
 *  清单还没取到时全部显示（服务端仍按清单拒，PGM:744「前端隐藏只是辅助，后端接口必须独立校验。」）。 */
fun homeShows(terminalType: String?, functions: Set<String>?, code: String): Boolean = when {
  terminalType == null -> code == "staff_device" || code == "tablet_pos" || code == "kiosk_claim"
  functions.isNullOrEmpty() -> true
  else -> code in functions
}

@Serializable
data class StaffDeviceCredentialRecord(
  val id: String = "",
  val staffProfileId: String? = null,
  val devicePublicKey: String? = null,
  val deviceFingerprintHash: String? = null,
  val bindStatus: String = "",
  val supersedesCredentialId: String? = null,
  val integrityStatus: String? = null,
  val revokedReason: String? = null,
  val boundAt: String? = null,
  val revokedAt: String? = null,
  val createdAt: String? = null,
  val updatedAt: String? = null,
)

// ---------------------------------------------------------------------------
// 票务核销冲正 POST /api/v1/ticketing/verifications/{id}/reverse
// ---------------------------------------------------------------------------
@Serializable
data class VerificationReverseRequest(
  val idempotencyKey: String,
  val reason: String? = null,
)

// ── BYOD 一期(2026-07-02 拍板):员工自有手机——注册/公告/我的工单/酒吧空位代订 ──

@Serializable
data class StaffRegistrationSubmitRequest(
  val idempotencyKey: String,
  val projectId: String,
  val name: String,
  val operatorNote: String? = null,
  val merchantId: String? = null,
  val phone: String? = null,
  val idDoc: String? = null,
)

@Serializable
data class StaffRegistrationCompany(val id: String, val name: String)

/** U01-007/U02-002（PGM L579/BSM L935）：私人手机安装时生成的设备公钥（Base64 X.509 DER）与指纹（SHA-256 十六进制）随注册提交并绑定本人。 */
@Serializable
data class StaffSelfRegistrationRequest(
  val idempotencyKey: String,
  val companyId: String,
  val name: String,
  val devicePublicKey: String,
  val deviceFingerprintHash: String,
  val merchantId: String? = null,
  val phone: String? = null,
  val idDoc: String? = null,
)

@Serializable
data class StaffRegistrationReceipt(val id: String, val registrationStatus: String)

@Serializable
data class StaffProfileRecord(
  val id: String? = null,
  val name: String? = null,
  val status: String? = null,
  val registrationStatus: String? = null,
  val employmentStatus: String? = null,
  val phone: String? = null,
  val companyId: String? = null,
)

@Serializable
data class EmployeeNoticeRecord(
  val id: String? = null,
  val staffProfileId: String? = null,
  val noticeType: String? = null,
  val title: String? = null,
  val body: String? = null,
  val consentRequired: Boolean? = null,
  val consentStatus: String? = null,
  val createdAt: String? = null,
)

@Serializable
data class NoticeAcknowledgeRequest(
  val idempotencyKey: String,
)

/** HP-F01 我的接警（GET /api/v1/facility/alarms/mine）：status awaiting_takeover＝待明确接手，taken_over＝已明确接手。 */
@Serializable
data class FacilityAlarmRecord(
  val id: String? = null,
  val equipmentId: String? = null,
  val alarmType: String? = null,
  val priority: String? = null,
  val status: String? = null,
  val responseDueAt: String? = null,
  val holderUserId: String? = null,
  val uncovered: Boolean? = null,
)

/** HP-F01 明确接手（「已收到并接手」）；查看列表、公告确认已读都不等于接手。 */
@Serializable
data class FacilityAlarmTakeOverRequest(
  val idempotencyKey: String,
  val note: String? = null,
)

@Serializable
data class MyWorkOrderRecord(
  val id: String? = null,
  val code: String? = null,
  val title: String? = null,
  val status: String? = null,
  val priority: String? = null,
  val handleDueAt: String? = null,
  // 第十二批操作员裁决第 13 条：我的工单一并列出「待我提单确认」的单。
  val assigneeId: String? = null,
  val confirmerId: String? = null,
  val reporterConfirmedAt: String? = null,
) {
  /**
   * 本人是提单确认承接人、不是本次维修人、尚未确认、待验收或已验收＝「待我提单确认」（可「确认」「退回返工」）。
   * 本人即本次维修人的，按 BSM L1385「本人负责此次维修时，由物业主管指定其他人员」不由本人确认，只作派给我的单列出。
   */
  fun awaitsConfirmationBy(userId: String?): Boolean =
    userId != null && confirmerId == userId && assigneeId != userId && reporterConfirmedAt == null &&
      status in setOf("pending_acceptance", "accepted")
}

/** 第十八批 M 段第 111 条 111-R1（BSM:1466「我的工单：只看派给自己的工单和处理进度。」）：派给本人的商户服务工单（只看）。 */
@Serializable
data class MyMerchantWorkOrderRecord(
  val id: String? = null,
  val title: String? = null,
  val workOrderType: String? = null,
  val priority: String? = null,
  val status: String? = null,
  val dueAt: String? = null,
)

/** ENUM:264 商户服务工单状态七态。 */
fun merchantWorkOrderStatusZh(status: String?): String = when (status) {
  "created" -> "已提交"
  "assigned" -> "已派单"
  "processing" -> "处理中"
  "pending_merchant" -> "待商户确认"
  "resolved" -> "已处理待关闭"
  "closed" -> "已关闭"
  "cancelled" -> "已取消"
  else -> status ?: "-"
}

/** 商户服务工单四类（第十八批 K 段第 93 条）。 */
fun merchantWorkOrderTypeZh(type: String?): String = when (type) {
  "repair" -> "报修"
  "cleaning" -> "保洁"
  "complaint" -> "投诉"
  "service" -> "其他服务请求"
  else -> type ?: "-"
}

// ── 第十八批 M 段第 112 条 112-R1（BSM:947「工作手机扫描权益二维码，核销赠饮、礼物或其他领取资格。」）：礼物（券）查券与核销 ──

@Serializable
data class VoucherRedeemPreviewRequest(val code: String)

/** 查券结果：goodsQuantity＝店内实物券每张件数（要逐件扫的物品码个数），其他券为空；blockReason 为空＝可核销。 */
@Serializable
data class VoucherRedeemPreview(
  val voucherInstanceId: String = "",
  val name: String? = null,
  val voucherKind: String? = null,
  val goodsQuantity: Int? = null,
  val status: String? = null,
  val expiresAt: String? = null,
  val blockReason: String? = null,
)

/** 券核销：code＝客人出示的券动态码；店内实物券带逐件扫的物品码（BSM:1197「出库须同时核验订单和物品码」）。 */
@Serializable
data class VoucherRedeemRequest(
  val idempotencyKey: String,
  val code: String,
  val traceCodes: List<String>? = null,
)

@Serializable
data class VoucherRedemption(
  val id: String = "",
  val voucherInstanceId: String = "",
  val voucherKind: String? = null,
)

/** 券状态（与后台券台账页同词）。 */
fun voucherStatusZh(status: String?): String = when (status) {
  "issued" -> "已发放"
  "redeemed" -> "已核销"
  "expired" -> "已过期"
  "revoked" -> "已作废"
  else -> status ?: "-"
}

/** 查券不可核销的原因（同核销拒绝码）。 */
fun voucherBlockReasonZh(reason: String?): String = when (reason) {
  "VOUCHER_ALREADY_REDEEMED" -> "已核销"
  "VOUCHER_EXPIRED" -> "已过期"
  "VOUCHER_REVOKED" -> "已作废"
  else -> reason ?: "-"
}

/** 逐件扫的物品码：按空格、逗号、换行拆开，去空白去重。 */
fun scannedTraceCodes(raw: String): List<String> =
  raw.split(Regex("[\\s,，]+")).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

/** 我的工单「确认」（POST /review）与「退回返工」（POST /rework，note＝不通过原因，必填）。 */
@Serializable
data class WorkOrderConfirmationRequest(
  val idempotencyKey: String,
  val note: String? = null,
)

// ---------------------------------------------------------------------------
// 图形化点单 POS 目录（V256/V257）：分类 / SKU / 套餐。GET 返回 {code,data:[...]}，
// 客户端 decode 已剥壳为 List<T>（非分页信封，无需 Page 包装）。
// ---------------------------------------------------------------------------
@Serializable
data class BarProductCategory(
  val id: String = "",
  val barId: String? = null,
  val merchantId: String? = null,
  val categoryName: String = "",
  val sortOrder: Int = 0,
  val status: String? = null,
)

@Serializable
data class BarProduct(
  val id: String = "",
  val barId: String? = null,
  val merchantId: String? = null,
  val productName: String = "",
  val productType: String? = null,
  val priceCent: Long = 0,
  val currency: String? = null,
  val soldOut: Boolean = false,
  val status: String? = null,
  val categoryId: String? = null,
  val imageUrl: String? = null,
)

@Serializable
data class BarComboItem(
  val id: String = "",
  val comboId: String? = null,
  val productId: String = "",
  val productName: String? = null,
  val quantity: Int = 0,
)

@Serializable
data class BarCombo(
  val id: String = "",
  val barId: String? = null,
  val merchantId: String? = null,
  val comboName: String = "",
  val priceCent: Long = 0,
  val internalSettlementPriceCent: Long? = null,
  val imageUrl: String? = null,
  val status: String? = null,
  val items: List<BarComboItem> = emptyList(),
)

// 由订单生成账单 POST /api/v1/bar/orders/{order_id}/bill（按桌整单结账前置步）。
@Serializable
data class BarOrderBillCreateRequest(
  val idempotencyKey: String,
  val operatorNote: String? = null,
  val evidenceAttachmentIds: List<String> = emptyList(),
  val paymentMode: String? = null,
  val minSpendFillRequired: Boolean? = null,
  val serviceFeeRateBp: Int? = null,
)

@Serializable
data class BarTableAvailabilityRow(
  val id: String? = null,
  val barId: String? = null,
  val tableCode: String? = null,
  val tableName: String? = null,
  val tableType: String? = null,
  val capacity: Int? = null,
  val minConsumptionCent: Long? = null,
  val status: String? = null,
  val reserved: Boolean? = null,
)

@Serializable
data class BarReservationCreateRequest(
  val idempotencyKey: String,
  val barId: String,
  val tableId: String,
  val reservationTime: String,
  val guestCount: Int,
  val salespersonUserId: String? = null,
  // ROOT-03-R19（BSM 第 1062 行「每一笔订台都必须填写最晚到店时间」）；操作员裁决第十三批第 7 条「员工代订必须录入客人手机号」。
  val latestArrivalAt: String,
  val customerPhone: String,
  // 第十八批 N 段第 120 条 120-R3（BSM:987「……人数和订台备注……」）：订台备注（选填）。
  val remark: String? = null,
)

@Serializable
data class BarReservationRecord(
  val id: String? = null,
  val status: String? = null,
  val depositStatus: String? = null,
  // 120-R3：一体机订台的应付定金；登记定金后是否已保留台位（held）。
  val depositAmountCent: Long? = null,
  val holdStatus: String? = null,
)

/** 第十八批 N 段第 120 条 120-R7：票务订单确认收款 POST /api/v1/ticketing/orders/{order_id}/confirm-payment（POS 时 gateway_ref 可填 POS 单号）。 */
@Serializable
data class ConfirmTicketPaymentRequest(
  val idempotencyKey: String,
  val paymentIntentId: String,
  val gatewayRef: String? = null,
)

/** 第十八批 N 段第 120 条 120-R7：订台定金登记 POST /api/v1/bar/reservations/{id}/pay-deposit。 */
@Serializable
data class BarReservationDepositPayRequest(
  val idempotencyKey: String,
  val channel: String,
)

/**
 * 第十八批 N 段第 120 条 120-R2（BSM:986「启用售票能力的一体机可由授权服务人员登录后辅助客人购票和查询订单（门票为动态二维码，不提供取票功能；
 * 客人用手机号短信验证绑定，在官方 App/小程序查看门票）……」）：一体机售票屏的票种（客人端票种目录一行：票名、平日/周末/节假日价、能否退、有效天数、使用范围）。
 */
@Serializable
data class KioskTicketProduct(
  val ticketTypeId: String = "",
  val ticketName: String? = null,
  val weekdayPriceCent: Long? = null,
  val weekendPriceCent: Long? = null,
  val holidayPriceCent: Long? = null,
  val barId: String? = null,
  val refundable: Boolean = false,
  val validityDays: Int? = null,
  val usageScope: String? = null,
)

/** 120-R2：一体机售票下单 POST /api/v1/ticketing/orders——一体机渠道，客人手机号与客人报的验证码，operator_note＝售票原因，单店票带所属酒吧。 */
@Serializable
data class KioskTicketOrderRequest(
  val idempotencyKey: String,
  val ticketTypeId: String,
  val quantity: Int,
  val buyerPhone: String,
  val smsCode: String,
  val operatorNote: String,
  val barId: String? = null,
  val salesChannel: String = "kiosk",
)

/** 120-R2：票务订单（员工端出参，购票手机号已掩码）。 */
@Serializable
data class TicketOrderRow(
  val id: String = "",
  val orderNo: String? = null,
  val quantity: Int = 0,
  val amountCent: Long = 0,
  val buyerPhone: String? = null,
  val payStatus: String? = null,
  val issueStatus: String? = null,
  val status: String? = null,
)

/** 120-R2：票务订单出票、取消 POST /api/v1/ticketing/orders/{order_id}/issue 与 /cancel（出票须填原因）。 */
@Serializable
data class TicketOrderActionRequest(
  val idempotencyKey: String,
  val reason: String? = null,
)

// ── BYOD 二期 E 批(2026-07-02 拍板):排班(主管双端)/考勤打卡(NFC/扫码,严格无宽限) ──

@Serializable
data class StaffShiftUpsertRequest(
  val idempotencyKey: String,
  val userId: String,
  val workDate: String,
  val startAt: String,
  val endAt: String,
  val pointSpaceIds: List<String>? = null,
  val note: String? = null,
  val nightDutyManager: Boolean = false,
)

@Serializable
data class StaffShiftRow(
  val id: String? = null,
  val userId: String? = null,
  val workDate: String? = null,
  val startAt: String? = null,
  val endAt: String? = null,
  val pointSpaceIds: List<String>? = null,
  val note: String? = null,
  val reviewId: String? = null,
  val publicationStatus: String? = null,
  val nightDutyProjectId: String? = null,
)

// 工作手机实际领取：事件编号固定，时钟证明与原设备/班次绑定，观察到的墙钟时间只供复核。
@Serializable
data class AttendanceAnchorRequest(val idempotencyKey: String, val shiftId: String, val bootId: String, val elapsedMillis: Long)

@Serializable
data class WorkPhoneAttendanceContext(val id: String, val deviceId: String, val userId: String, val shiftId: String? = null,
  val bindingAt: String? = null, val staffProfileId: String? = null, val previousContextId: String? = null,
  val closedAt: String? = null, val closeReason: String? = null, val createdAt: String? = null)

@Serializable
data class AttendanceTimeAnchor(val id: String, val contextId: String, val bootId: String, val elapsedMillis: Long, val serverAt: String)

@Serializable
data class AttendanceAnchorResponse(val context: WorkPhoneAttendanceContext, val anchor: AttendanceTimeAnchor)

@Serializable
data class AttendanceReceiptRequest(val eventId: String, val deviceId: String, val contextId: String? = null,
  val anchorId: String? = null, val bootId: String, val elapsedMillis: Long, val observedAt: String,
  val method: String, val tagRef: String? = null, val spaceId: String? = null)

@Serializable
data class AttendanceReceiptRecoveryRequest(val original: AttendanceReceiptRequest,val reason: String,val handoverEvidence: String,
  val proposedContextId: String? = null)

@Serializable
data class AttendanceReceiptEvent(val id: String, val status: String, val actualAt: String? = null,
  val observedAt: String? = null, val reason: String? = null, val reviewId: String? = null,
  val userId: String? = null, val shiftId: String? = null, val contextId: String? = null, val deviceId: String? = null)

@Serializable
data class AttendanceReceiptReviewRequest(val idempotencyKey: String, val shiftId: String, val actualAt: String,
  val reason: String, val handoverEvidence: String)

@Serializable
data class AttendanceReceiptReview(val id: String, val eventId: String, val reviewId: String, val status: String,
  val shiftId: String, val actualAt: String, val reason: String, val handoverEvidence: String,
  val initiatedBy: String? = null, val createdAt: String? = null, val decidedAt: String? = null)

@Serializable
data class WorkPhoneSwapRequest(val idempotencyKey: String, val oldDeviceCode: String, val replacementDeviceId: String,
  val reason: String, val evidenceAttachmentIds: List<String> = emptyList())

@Serializable
data class AttendancePunchRequest(
  val idempotencyKey: String,
  val punchType: String,
  val method: String,
  val tagRef: String? = null,
  val spaceId: String? = null,
  val deviceFingerprint: String? = null,
)

@Serializable
data class AttendanceRow(
  val id: String? = null,
  val punchType: String? = null,
  val punchedAt: String? = null,
  val method: String? = null,
  val judgement: String? = null,
  val spaceId: String? = null,
)

// ── BYOD 三期 F 批(2026-07-02 拍板):我的薪资(月结后可见)/请假(主管审)/考勤月视图 ──

@Serializable
data class PayrollStatementRow(
  val id: String? = null,
  val month: String? = null,
  val baseCent: Long? = null,
  val commissionCent: Long? = null,
  val bonusCent: Long? = null,
  val penaltyCent: Long? = null,
  val leaveDeductionCent: Long? = null,
  val taxCent: Long? = null,
  val totalCent: Long? = null,
)

@Serializable
data class LeaveSubmitRequest(
  val idempotencyKey: String,
  val leaveType: String,
  val startDate: String,
  val endDate: String,
  val reason: String,
)

@Serializable
data class LeaveRow(
  val id: String? = null,
  val leaveType: String? = null,
  val startDate: String? = null,
  val endDate: String? = null,
  val reason: String? = null,
  val status: String? = null,
  val reviewNote: String? = null,
  /** 2026-10-18 裁决：员工本人申请销假的理由（待销假审批/已销假时有值）。 */
  val revokeRequestReason: String? = null,
)

/** 员工本人申请销假（只对自己已批准的假；理由必填；主管批准后才生效）。 */
@Serializable
data class LeaveRevokeRequest(
  val idempotencyKey: String,
  val reason: String,
)

@Serializable
data class AttendanceDayRow(
  val date: String? = null,
  val dayStatus: List<String>? = null,
  val shiftStartAt: String? = null,
  val shiftEndAt: String? = null,
)

// ── BYOD 二期 Wave1(2026-07-19 拍板):工资条提成明细 + 本人资料自助 ──

@Serializable
data class StaffCommissionLineRow(
  val sourceType: String? = null,
  val sourceId: String? = null,
  val amountCent: Long? = null,
  val note: String? = null,
  val createdAt: String? = null,
)

@Serializable
data class StaffProfileSelfUpdateRequest(
  val idempotencyKey: String,
  val name: String,
)

// ── G批(2026-07-02 六口径拍板):服务点单——目录/代客下单指定履约人/公司收款码/履约流转 ──

@Serializable
data class ServiceCatalogRow(
  val id: String? = null,
  val name: String? = null,
  val priceCent: Long? = null,
  val durationMinutes: Int? = null,
  val active: Boolean? = null,
)

@Serializable
data class ServiceOrderCreateReq(
  val idempotencyKey: String,
  val catalogItemId: String,
  val performerUserId: String,
  val barId: String? = null,
  val tableId: String? = null,
  val customerNote: String? = null,
)

@Serializable
data class ServiceOrderRow(
  val id: String? = null,
  val itemName: String? = null,
  val priceCent: Long? = null,
  val tableId: String? = null,
  val performerUserId: String? = null,
  val status: String? = null,
  val customerNote: String? = null,
  val cancelReason: String? = null,
  val paidAt: String? = null,
  val mgmtFeeCent: Long? = null,
  val dancerShareCent: Long? = null,
  val createdAt: String? = null,
)

/** 第 84 条 84-R1：证据上传回执（POST /api/v1/evidence/attachments/upload 的 data）。 */
@Serializable
data class EvidenceAttachmentRecord(
  val id: String,
  val contentType: String? = null,
  val fileSize: Long? = null,
)

@Serializable
data class ServiceActionReq(
  val idempotencyKey: String,
)

@Serializable
data class ServiceMarkPaidReq(
  val idempotencyKey: String,
  val payMethod: String? = null,
  // 第十一批 TK-04-R2（BSM L822、PGM L878）：收款时录入会员号（注册手机号），选填；后端走客人查询专门通道留痕。
  val memberPhone: String? = null,
)

@Serializable
data class PaymentCodeReq(
  val idempotencyKey: String,
  val qrContent: String,
)

/**
 * 解析 NDEF 文本记录(RTD_TEXT)的 payload,取正文。
 *
 * NFC Forum 文本记录 payload 结构:[状态字节][语言码(ISO 639)][正文(UTF-8/UTF-16)]。
 * 状态字节:最高位(0x80)=编码(0=UTF-8,1=UTF-16),低 6 位(0x3F)=语言码字节长度。
 * 正文从 1 + 语言码长度 处开始。据此跳过状态字节与语言码,只返回正文(去首尾空白);
 * 非法/空返回 null(避免旧写法 dropWhile 未剥语言码把"en"污染进点位ID)。
 */
fun decodeNdefTextPayload(payload: ByteArray): String? {
  if (payload.isEmpty()) return null
  val status = payload[0].toInt()
  val langLen = status and 0x3F
  if (payload.size < 1 + langLen) return null
  val charset = if (status and 0x80 != 0) Charsets.UTF_16 else Charsets.UTF_8
  return String(payload, 1 + langLen, payload.size - 1 - langLen, charset)
    .trim().ifEmpty { null }
}
