package com.complex.terminal

// 文件版本：V0.01

import kotlin.test.*

class AttendanceReceiptQueueTest {
  private var disk: String? = null
  private fun queue() = AttendanceReceiptQueue({ disk }, { disk = it })

  @Test fun noLoginAndRepeatedEventsSurviveProcessRestartWithOriginalIdsAndTimes() {
    val first=queue().enqueue(null,null,"boot-a",100,"2026-10-18T01:00:00Z","charging_disconnect")
    val second=queue().enqueue(null,null,"boot-a",200,"2026-10-18T01:00:01Z","charging_disconnect")
    assertEquals(listOf(first,second),queue().pending())
    assertEquals(first.eventId,queue().pending().first().request("newly-registered").eventId)
    assertEquals(100L,queue().pending().first().elapsedMillis)
  }

  @Test fun onlyServerAcknowledgedEventIsRemovedAndFailureLeavesSamePayloadForRetry() {
    val event=queue().enqueue("phone",null,"boot-a",100,"2026-10-18T01:00:00Z","qr","space:counter")
    val before=queue().pending().first().request("phone")
    // 上传失败不调用 acknowledge；重启后的重试逐字段相同。
    assertEquals(before,queue().pending().first().request("phone"))
    queue().acknowledge("unrelated")
    assertEquals(1,queue().pending().size)
    queue().acknowledge(event.eventId)
    assertTrue(queue().pending().isEmpty())
  }

  @Test fun rebootDoesNotRewriteOldEventBootOrAttachNewAnchorToIt() {
    val context=WorkPhoneAttendanceContext("context","phone","employee","shift")
    val anchor=AttendanceAnchorResponse(context,AttendanceTimeAnchor("anchor","context","boot-a",1,"2026-10-18T00:00:00Z"))
    val old=queue().enqueue("phone",anchor,"boot-a",100,"2026-10-18T01:00:00Z","nfc","point")
    val rebooted=queue().enqueue("phone",anchor,"boot-b",3,"2026-10-18T02:00:00Z","charging_disconnect")
    assertEquals(old,queue().pending().first())
    assertEquals("boot-b",rebooted.bootId)
    assertEquals("anchor",rebooted.anchorId) // 服务端将启动不符保留待复核，不伪造新锚。
  }

  @Test fun storageFailureDoesNotPretendToHaveQueuedEvent() {
    val queue=AttendanceReceiptQueue({null},{error("disk failed")})
    assertFailsWith<IllegalStateException> { queue.enqueue("phone",null,"boot",1,"2026-10-18T00:00:00Z","charging_disconnect") }
  }
}
