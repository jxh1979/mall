package com.complex.terminal

// 文件版本：V0.01

import kotlin.test.*

class AttendanceReceiptSwitchTest {
  @Test fun eventCapturedDuringUploadIsNotLostWhenEarlierEventIsAcknowledged() {
    var disk: String? = null
    val queue = AttendanceReceiptQueue({ disk }, { disk = it })
    val first = queue.enqueue("phone",anchor(context("a","2026-10-18T00:00:00Z")),"boot",10,"2026-10-18T00:00:00Z","charging_disconnect")
    var during: PendingAttendanceReceipt? = null
    queue.flush { event ->
      during = queue.enqueue("phone",null,"boot",20,"2026-10-18T00:00:01Z","charging_disconnect")
      AttendanceReceiptEvent(event.eventId,"accepted")
    }
    assertEquals(listOf(during),queue.pending())
    assertFalse(queue.pending().any { it.eventId == first.eventId })
  }

  private fun context(user: String, binding: String) = WorkPhoneAttendanceContext("ctx-$user-$binding", "phone", user,
    "shift-$user", bindingAt = binding)
  private fun anchor(ctx: WorkPhoneAttendanceContext) = AttendanceAnchorResponse(ctx,
    AttendanceTimeAnchor("anchor-${ctx.id}", ctx.id, "boot", 10, "2026-10-18T00:00:00Z"))

  @Test fun missingOriginalContextIsNeverAutomaticallyAttributedToCurrentHolder() {
    var disk: String? = null
    val queue = AttendanceReceiptQueue({ disk }, { disk = it })
    val original = queue.enqueue("phone",null,"boot",10,"2000-01-01T00:00:00Z","charging_disconnect")
    val next = queue.enqueue("phone",anchor(context("b","2026-10-18T08:00:00Z")),"boot",20,"2026-10-18T08:00:01Z","charging_disconnect")
    val attempted = mutableListOf<String>()
    val result = queue.flush { event -> attempted.add(event.eventId); AttendanceReceiptEvent(event.eventId,"accepted") }
    assertEquals(listOf(next.eventId),attempted)
    assertEquals(listOf(original),queue.pending())
    assertEquals(setOf(original.eventId),result.failed.keys)
    assertTrue(result.message().contains("不能套用当前持有人"))
  }

  @Test fun changingHolderOrSameHolderNewBindingCannotReuseOldAnchor() {
    val original = context("a", "2026-10-18T00:00:00Z")
    assertNull(matchingReceiptAnchor(anchor(original), context("b", "2026-10-18T08:00:00Z")))
    assertNull(matchingReceiptAnchor(anchor(original), context("a", "2026-10-19T00:00:00Z")))
    assertNull(matchingReceiptAnchor(anchor(original), original.copy(closedAt = "2026-10-18T08:00:00Z")))
    assertEquals(anchor(original), matchingReceiptAnchor(anchor(original), original))
  }

  @Test fun oldOwnerFailureDoesNotBlockNextHolderAndDoesNotRewriteOrDeleteOldEvent() {
    var disk: String? = null
    val queue = AttendanceReceiptQueue({ disk }, { disk = it })
    val old = queue.enqueue("phone", anchor(context("a", "2026-10-18T00:00:00Z")), "boot", 20,
      "2026-10-18T00:00:01Z", "charging_disconnect")
    val next = queue.enqueue("phone", anchor(context("b", "2026-10-18T08:00:00Z")), "boot", 30,
      "2026-10-18T08:00:01Z", "charging_disconnect")
    val attempted = mutableListOf<String>()
    val result = queue.flush { event ->
      attempted.add(event.eventId)
      if (event.eventId == old.eventId) error("原持有人不同")
      AttendanceReceiptEvent(event.eventId, "accepted")
    }
    assertEquals(listOf(old.eventId, next.eventId), attempted)
    assertEquals(listOf(old), queue.pending())
    assertEquals(old.request("phone"), queue.pending().single().request("phone"))
    assertEquals(listOf(next.eventId), result.accepted.map { it.id })
    assertEquals(setOf(old.eventId), result.failed.keys)
    assertTrue(result.message().contains("部分同步"))
    assertFalse(result.message().contains("同步完成"))
  }
}
