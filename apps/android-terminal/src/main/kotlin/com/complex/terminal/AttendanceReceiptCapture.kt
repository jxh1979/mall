package com.complex.terminal

// 文件版本：V0.01

/** 充电广播、NFC及扫码共用的实际捕获决策；先持久化原发生证据，再通知联网同步。 */
object AttendanceReceiptCapture {
  fun capture(queue: AttendanceReceiptQueue, deviceId: String?, cachedContext: WorkPhoneAttendanceContext?,
      cachedAnchor: AttendanceAnchorResponse?, bootId: String, elapsedMillis: Long, observedAt: String,
      method: String, tagRef: String? = null): PendingAttendanceReceipt {
    val current = cachedContext?.takeIf { it.deviceId == deviceId && it.closedAt == null }
    val anchor = matchingReceiptAnchor(cachedAnchor,current)
    return queue.captureUnconfirmed(deviceId,anchor?.anchor?.id,bootId,elapsedMillis,observedAt,method,tagRef)
  }
}
