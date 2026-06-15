package com.complex.terminal

import java.time.Instant

class InMemoryLocalStore {
  var session: TerminalSession? = null
    private set

  var binding: DeviceBinding? = null
    private set

  private val entitlements = linkedMapOf<String, OfflineEntitlement>()
  private val tasks = mutableListOf<LocalTask>()
  private val events = mutableListOf<TerminalEvent>()

  fun saveSession(value: TerminalSession) {
    session = value
  }

  fun saveBinding(value: DeviceBinding) {
    binding = value
  }

  fun replaceOfflinePackage(values: List<OfflineEntitlement>) {
    entitlements.clear()
    values.forEach { entitlements[it.entitlementCode] = it }
  }

  fun offlineEntitlementCount(): Int = entitlements.size

  fun findEntitlement(entitlementCode: String): OfflineEntitlement? = entitlements[entitlementCode]

  fun remainingTimes(entitlementCode: String): Int {
    val entitlement = entitlements[entitlementCode] ?: return 0
    return entitlement.totalTimes - entitlement.usedTimes
  }

  fun markLocalUse(entitlementCode: String) {
    val entitlement = entitlements[entitlementCode] ?: return
    val nextUsedTimes = entitlement.usedTimes + 1
    entitlements[entitlementCode] = entitlement.copy(
      usedTimes = nextUsedTimes,
      status = if (nextUsedTimes >= entitlement.totalTimes) "used" else entitlement.status,
    )
  }

  fun enqueue(task: LocalTask) {
    tasks += task
  }

  fun pendingTasks(): List<LocalTask> = tasks.filter { it.status == LocalTaskStatus.PENDING }

  fun pendingTaskCount(): Int = pendingTasks().size

  fun markSynced(results: List<SyncTaskResult>) {
    val resultById = results.associateBy { it.clientTaskId }
    tasks.replaceAll { task ->
      val result = resultById[task.clientTaskId] ?: return@replaceAll task
      when (result.status) {
        "accepted" -> task.copy(status = LocalTaskStatus.SYNCED)
        "conflict" -> task.copy(status = LocalTaskStatus.CONFLICT)
        else -> task
      }
    }
  }

  fun addEvent(event: TerminalEvent) {
    events += event
  }

  fun pendingEvents(): List<TerminalEvent> = events.toList()

  fun clearEvents() {
    events.clear()
  }

  fun clearSensitiveData() {
    session = null
    binding = null
    entitlements.clear()
    tasks.removeIf { it.status != LocalTaskStatus.PENDING }
    events.clear()
  }
}

fun nowIso(): String = Instant.now().toString()
