package com.complex.terminal

// 文件版本：V0.01

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PendingAttendanceReceipt(
  val eventId: String, val deviceId: String?, val contextId: String?, val anchorId: String?,
  val bootId: String, val elapsedMillis: Long, val observedAt: String, val method: String,
  val tagRef: String? = null, val spaceId: String? = null,
  val holderUserId: String? = null, val bindingAt: String? = null,
) {
  fun request(registeredDeviceId: String): AttendanceReceiptRequest = AttendanceReceiptRequest(
    eventId, deviceId ?: registeredDeviceId, contextId, anchorId, bootId, elapsedMillis, observedAt, method, tagRef, spaceId,
  )
}

/** write 必须同步持久化；先落事件再通知上传，收到服务端同一 eventId 回执才删除。 */
class AttendanceReceiptQueue(private val read: () -> String?, private val write: (String) -> Unit) {
  private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
  @Synchronized
  fun pending(): List<PendingAttendanceReceipt> = read()?.let { json.decodeFromString<List<PendingAttendanceReceipt>>(it) } ?: emptyList()

  @Synchronized
  fun enqueue(deviceId: String?, anchor: AttendanceAnchorResponse?, bootId: String, elapsedMillis: Long,
      observedAt: String, method: String, tagRef: String? = null,
      context: WorkPhoneAttendanceContext? = anchor?.context): PendingAttendanceReceipt {
    val proof = matchingReceiptAnchor(anchor, context)
    val event = PendingAttendanceReceipt(UUID.randomUUID().toString(), deviceId, context?.id,
      proof?.anchor?.id, bootId, elapsedMillis, observedAt, method, tagRef, tagRef?.removePrefix("space:"),
      context?.userId, context?.bindingAt)
    write(json.encodeToString(pending() + event))
    return event
  }

  @Synchronized
  fun acknowledge(eventId: String) { write(json.encodeToString(pending().filterNot { it.eventId == eventId })) }

  /** 缓存只提供锚线索，不能证明捕获当刻仍属该领用；归属留给服务端核验。 */
  @Synchronized
  fun captureUnconfirmed(deviceId: String?, anchorHintId: String?, bootId: String, elapsedMillis: Long,
      observedAt: String, method: String, tagRef: String?): PendingAttendanceReceipt {
    val event = PendingAttendanceReceipt(UUID.randomUUID().toString(),deviceId,null,anchorHintId,
      bootId,elapsedMillis,observedAt,method,tagRef,tagRef?.removePrefix("space:"))
    write(json.encodeToString(pending() + event))
    return event
  }

  fun flush(upload: (PendingAttendanceReceipt) -> AttendanceReceiptEvent): ReceiptUploadResult {
    val accepted = mutableListOf<AttendanceReceiptEvent>()
    val failed = linkedMapOf<String, String>()
    pending().forEach { event ->
      try {
        check(event.contextId != null || event.anchorId != null) { "原事件没有领用编号或可核验锚，须由主管据真实领用历史关联并核时；不能套用当前持有人" }
        val response = upload(event)
        check(response.id == event.eventId) { "服务端未确认原事件编号" }
        acknowledge(event.eventId)
        accepted.add(response)
      } catch (error: Exception) { failed[event.eventId] = error.message ?: "同步未完成" }
    }
    return ReceiptUploadResult(accepted, failed)
  }
}

data class ReceiptUploadResult(val accepted: List<AttendanceReceiptEvent>, val failed: Map<String, String>) {
  fun message(): String = if (failed.isNotEmpty()) "部分同步：${accepted.size}条已上传，${failed.size}条原事件保留待处理。请在待传事件中由原持有人重试或获授权主管代补报待核。\n" + failed.entries.joinToString("\n") { "${it.key}：${it.value}" }
    else if (accepted.any { it.status == "pending_review" }) "事件已上传；部分实际时间需核实，暂未计入考勤" else "领取事件同步完成"
}

fun matchingReceiptAnchor(anchor: AttendanceAnchorResponse?, current: WorkPhoneAttendanceContext?): AttendanceAnchorResponse? =
  anchor?.takeIf { current != null && current.closedAt == null && it.context.closedAt == null
    && it.context.id == current.id && it.context.deviceId == current.deviceId && it.context.userId == current.userId
    && it.context.bindingAt == current.bindingAt }
