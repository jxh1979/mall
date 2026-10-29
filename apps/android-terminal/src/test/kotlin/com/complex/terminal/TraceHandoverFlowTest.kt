package com.complex.terminal

// 文件版本：V0.01

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 第十八批 I 段第 84 条 84-R2（BSM 第 1201 行「每一步都必须扫码」「扫码缺失的可事后人工补录……并经主管复核后方生效」）：
 * 工作手机交接页只给下一个人工段一个确认按钮；已通过的补录算该段完成，待复核的不算；机器人配送段不在手机上扫。
 */
class TraceHandoverFlowTest {
  private val fresh = linkedMapOf(
    "out_verify" to "pending", "dispatch" to "pending", "kitchen_receive" to "pending",
    "staff_pickup" to "pending", "deliver_customer" to "pending",
  )

  private fun chain(steps: Map<String, String>, vararg backfills: TraceHandoverBackfillRow) = TraceHandoverChain(
    TraceHandoverRecord("trace_1", overallStatus = "in_progress"),
    steps.map { (segment, status) -> TraceHandoverStep("trace_1_$segment", segment, stepStatus = status) },
    backfills.toList(),
  )

  @Test
  fun nextSegmentSkipsTheRobotSegmentAndCountsOnlyApprovedBackfills() {
    val afterOut = fresh + ("out_verify" to "done")
    assertEquals("kitchen_receive", nextStaffSegment(chain(afterOut)))
    assertEquals("staff_pickup", nextStaffSegment(chain(afterOut, TraceHandoverBackfillRow("b1", "kitchen_receive", "approved"))))
    assertEquals("kitchen_receive", nextStaffSegment(chain(afterOut, TraceHandoverBackfillRow("b1", "kitchen_receive", "pending"))))
    assertNull(nextStaffSegment(chain(fresh.mapValues { (segment, _) -> if (segment == "dispatch") "pending" else "done" })))
  }

  @Test
  fun backfillOnlyForPendingStaffSegmentsWithoutPendingOrApprovedBackfill() {
    val c = chain(fresh + ("out_verify" to "done"),
      TraceHandoverBackfillRow("b1", "kitchen_receive", "pending"), TraceHandoverBackfillRow("b2", "staff_pickup", "rejected"))
    assertFalse(canBackfill(c, "out_verify"))
    assertFalse(canBackfill(c, "kitchen_receive"))
    assertTrue(canBackfill(c, "staff_pickup"))
    assertTrue(canBackfill(c, "deliver_customer"))
    assertFalse(canBackfill(c, "dispatch"))
  }

  @Test
  fun activeChainIsTheNewestHandoverNotYetCompleted() {
    val list = listOf(
      TraceHandoverRecord("t3", overallStatus = "completed"),
      TraceHandoverRecord("t2", overallStatus = "exception"),
      TraceHandoverRecord("t1", overallStatus = "in_progress"),
    )
    assertEquals("t2", activeChain(list)?.id)
    assertNull(activeChain(listOf(TraceHandoverRecord("t1", overallStatus = "completed"))))
  }
}
