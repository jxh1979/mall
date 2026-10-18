package com.complex.terminal

// 文件版本：V0.01

import kotlin.test.*

class AttendanceReceiptCaptureTest {
  private fun context(user: String, binding: String) = WorkPhoneAttendanceContext("context-$binding","phone",user,
    "shift-$user",bindingAt=binding)
  private fun anchor(context: WorkPhoneAttendanceContext) = AttendanceAnchorResponse(context,
    AttendanceTimeAnchor("anchor-${context.id}",context.id,"boot",100,"2026-10-18T00:00:00Z"))

  @Test fun externalHandoverThenUnplugBeforeLoginNeverFreezesPreviousHolder() {
    var disk: String? = null
    val queue = AttendanceReceiptQueue({disk},{disk=it})
    val old = context("employee-a","2026-10-18T00:00:00Z")
    // 外部终端完成归还改绑，不会更新这台后台手机仍存着的 A 缓存。
    val captured = AttendanceReceiptCapture.capture(queue,"phone",old,anchor(old),"boot",28800100,
      "2026-10-18T08:00:00Z","charging_disconnect")
    assertNull(captured.contextId,"先拔线捕获不得把旧缓存当成本次原领用")
    assertNull(captured.holderUserId)
    val fixed = captured.request("phone")
    // 此后才登录 B 取到新上下文，不能倒改已经保存的原发生证据。
    val next = context("employee-b","2026-10-18T08:00:00Z")
    AttendanceReceiptCapture.capture(queue,"phone",next,anchor(next),"boot",28800200,
      "2026-10-18T08:00:01Z","nfc","point")
    assertEquals(fixed,queue.pending().first().request("phone"))
    assertEquals("2026-10-18T08:00:00Z",queue.pending().first().observedAt)
    assertEquals(28800100L,queue.pending().first().elapsedMillis)
  }

  @Test fun sameEmployeeNextBindingBeforeRefreshDoesNotFreezePreviousRound() {
    var disk: String? = null
    val queue = AttendanceReceiptQueue({disk},{disk=it})
    val stale = context("employee-a","2026-10-17T00:00:00Z")
    val event = AttendanceReceiptCapture.capture(queue,"phone",stale,anchor(stale),"boot",86400100,
      "2026-10-18T00:00:00Z","charging_disconnect")
    assertNull(event.contextId)
    assertEquals(event,AttendanceReceiptQueue({disk},{disk=it}).pending().single())
  }

  @Test fun logoutThenUnplugKeepsUnassignedOriginalForSupervisorReview() {
    var disk: String? = null
    val queue = AttendanceReceiptQueue({disk},{disk=it})
    val event = AttendanceReceiptCapture.capture(queue,"phone",null,null,"boot",105,
      "2026-10-18T00:00:01Z","charging_disconnect")
    assertNull(event.contextId);assertNull(event.anchorId)
    var sent = false
    val result = queue.flush { sent=true; AttendanceReceiptEvent(it.eventId,"accepted") }
    assertFalse(sent);assertEquals(setOf(event.eventId),result.failed.keys)
    assertEquals(event,queue.pending().single())
  }

  @Test fun validAnchorHintCanBeVerifiedAutomaticallyWithoutClaimingCachedHolder() {
    var disk: String? = null
    val queue = AttendanceReceiptQueue({disk},{disk=it})
    val current = context("employee-a","2026-10-18T00:00:00Z")
    val proof = anchor(current)
    val event = AttendanceReceiptCapture.capture(queue,"phone",current,proof,"boot",100,
      "2026-10-18T00:00:00Z","nfc","point")
    assertNull(event.contextId);assertNull(event.holderUserId)
    assertEquals(proof.anchor.id,event.anchorId)
    val result = queue.flush { candidate ->
      assertEquals(event.request("phone"),candidate.request("phone"))
      AttendanceReceiptEvent(candidate.eventId,"accepted")
    }
    assertEquals(listOf(event.eventId),result.accepted.map { it.id });assertTrue(queue.pending().isEmpty())
  }

  @Test fun serverRefusalOfOldAnchorRetainsOriginalAndContinuesNewCapture() {
    var disk: String? = null
    val queue = AttendanceReceiptQueue({disk},{disk=it})
    val previous = context("employee-a","2026-10-18T00:00:00Z")
    val old = AttendanceReceiptCapture.capture(queue,"phone",previous,anchor(previous),"boot",200,
      "2026-10-18T08:00:00Z","charging_disconnect")
    val current = context("employee-b","2026-10-18T08:00:00Z")
    val next = AttendanceReceiptCapture.capture(queue,"phone",current,anchor(current),"boot",300,
      "2026-10-18T08:00:01Z","qr","point")
    val result = queue.flush { candidate ->
      if (candidate.anchorId == old.anchorId) error("原锚不能证明本次领用")
      AttendanceReceiptEvent(candidate.eventId,"accepted")
    }
    assertEquals(listOf(old),queue.pending());assertNull(queue.pending().single().contextId)
    assertEquals(listOf(next.eventId),result.accepted.map { it.id })
    val recovery = AttendanceReceiptRecoveryRequest(old.request("phone"),"交班待核","原领用登记已核对",current.id)
    assertEquals(old.request("phone"),recovery.original);assertEquals(current.id,recovery.proposedContextId)
  }
}
