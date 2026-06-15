package com.complex.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InMemoryLocalStoreTest {
  @Test
  fun localQueueTracksPendingAndSyncedTasks() {
    val store = InMemoryLocalStore()
    store.replaceOfflinePackage(
      listOf(
        OfflineEntitlement(
          id = "ent-1",
          entitlementCode = "E-001",
          entitlementType = "entry",
          totalTimes = 2,
          usedTimes = 0,
          status = "active",
        ),
      ),
    )

    assertEquals(2, store.remainingTimes("E-001"))
    store.markLocalUse("E-001")
    assertEquals(1, store.remainingTimes("E-001"))

    val task = LocalTask(
      clientTaskId = "task-1",
      taskType = "offline_verification",
      payload = mapOf("entitlementCode" to "E-001"),
      occurredAt = nowIso(),
    )
    store.enqueue(task)
    assertEquals(1, store.pendingTaskCount())

    store.markSynced(listOf(SyncTaskResult(clientTaskId = "task-1", status = "accepted")))
    assertEquals(0, store.pendingTaskCount())
  }

  @Test
  fun clearSensitiveDataKeepsOnlyPendingTasks() {
    val store = InMemoryLocalStore()
    store.saveSession(TerminalSession("token", "staff-1", "admin160"))
    store.saveBinding(DeviceBinding(id = "device-1", deviceCode = "D-1", status = "bound"))
    store.enqueue(LocalTask("pending-1", "manual_supplement", emptyMap(), nowIso()))
    store.enqueue(LocalTask("synced-1", "manual_supplement", emptyMap(), nowIso(), LocalTaskStatus.SYNCED))

    assertNotNull(store.session)
    store.clearSensitiveData()

    assertEquals(null, store.session)
    assertEquals(null, store.binding)
    assertEquals(1, store.pendingTaskCount())
  }
}
