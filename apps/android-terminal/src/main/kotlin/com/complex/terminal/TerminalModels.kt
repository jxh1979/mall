package com.complex.terminal

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
)

@Serializable
data class VerificationRequest(
  val idempotencyKey: String,
  val credentialType: String = "qr",
  val credentialPayload: String,
  val deviceId: String,
  val pointId: String,
  val businessContext: Map<String, String> = emptyMap(),
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

// 游乐检修主管确认恢复 POST .../maintenance-records/{id}/confirm-resume
// 注意：此处用 remark（非 operator_note）。
@Serializable
data class AmusementMaintenanceResumeRequest(
  val idempotencyKey: String,
  val supervisorConfirmedBy: String? = null,
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
  val salespersonUserId: String? = null,
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
)

@Serializable
data class StaffProfileRecord(
  val id: String? = null,
  val name: String? = null,
  val status: String? = null,
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

@Serializable
data class MyWorkOrderRecord(
  val id: String? = null,
  val code: String? = null,
  val title: String? = null,
  val status: String? = null,
  val priority: String? = null,
  val handleDueAt: String? = null,
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
)

@Serializable
data class BarReservationRecord(
  val id: String? = null,
  val status: String? = null,
  val depositStatus: String? = null,
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
)

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
)

@Serializable
data class AttendanceDayRow(
  val date: String? = null,
  val dayStatus: List<String>? = null,
  val shiftStartAt: String? = null,
  val shiftEndAt: String? = null,
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

@Serializable
data class ServiceActionReq(
  val idempotencyKey: String,
)

@Serializable
data class ServiceMarkPaidReq(
  val idempotencyKey: String,
  val payMethod: String? = null,
)

@Serializable
data class PaymentCodeReq(
  val idempotencyKey: String,
  val qrContent: String,
)
