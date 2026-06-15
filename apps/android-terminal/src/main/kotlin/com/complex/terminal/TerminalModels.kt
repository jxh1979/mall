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
