package com.complex.terminal

import java.util.UUID

class TerminalWorkflow(
  private val api: TerminalApiClient,
  private val store: InMemoryLocalStore,
) {
  var networkOnline: Boolean = true

  fun login(username: String, password: String): TerminalSession {
    requireOnline()
    val response = api.login(username, password)
    api.accessToken = response.accessToken
    val session = TerminalSession(
      accessToken = response.accessToken,
      staffId = response.user.id,
      username = response.user.username,
    )
    store.saveSession(session)
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.login",
        summary = "终端员工登录",
        payload = mapOf("username" to username),
        occurredAt = nowIso(),
      ),
    )
    return session
  }

  fun bindDevice(request: BindDeviceRequest): DeviceBinding {
    requireOnline()
    val effectiveRequest = request.withStableIdempotencyKey()
    val binding = api.bindDevice(effectiveRequest)
    store.saveBinding(binding)
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.device.bound.local",
        summary = "终端本地保存设备绑定",
        payload = mapOf("deviceCode" to effectiveRequest.deviceId),
        occurredAt = nowIso(),
      ),
    )
    return binding
  }

  fun refreshOfflinePackage(merchantId: String?, venueId: String?): OfflinePackageResponse {
    requireOnline()
    val response = api.offlineEntitlements(merchantId, venueId)
    store.replaceOfflinePackage(response.entitlements)
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.offline_package.loaded",
        summary = "终端加载离线权益包",
        payload = mapOf("count" to response.entitlements.size.toString()),
        occurredAt = nowIso(),
      ),
    )
    return response
  }

  fun previewOnline(entitlementCode: String): PreviewResponse {
    requireOnline()
    val binding = requireNotNull(store.binding) { "device binding is missing" }
    return api.preview(
      VerificationRequest(
        idempotencyKey = newClientTaskId("online-preview"),
        credentialPayload = entitlementCode,
        deviceId = binding.id,
        pointId = binding.locationPointId ?: binding.deviceCode,
        businessContext = mapOf("scenario" to "ticket_checkin"),
      ),
    )
  }

  fun verifyOnline(entitlementCode: String): VerificationResponse {
    requireOnline()
    val session = requireNotNull(store.session) { "terminal session is missing" }
    val binding = requireNotNull(store.binding) { "device binding is missing" }
    val response = api.verify(
      VerificationRequest(
        idempotencyKey = newClientTaskId("online-verification"),
        credentialPayload = entitlementCode,
        deviceId = binding.id,
        pointId = binding.locationPointId ?: binding.deviceCode,
        businessContext = mapOf(
          "scenario" to "ticket_checkin",
          "staff_id" to session.staffId,
        ),
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.verification.online",
        summary = "终端在线核销完成",
        payload = mapOf("entitlementCode" to entitlementCode, "verificationNo" to response.verificationNo),
        occurredAt = nowIso(),
      ),
    )
    return response
  }

  fun verifyOffline(entitlementCode: String): LocalTask {
    val entitlement = requireNotNull(store.findEntitlement(entitlementCode)) {
      "offline entitlement package does not contain $entitlementCode"
    }
    require(entitlement.status == "active") { "offline entitlement is not active" }
    require(store.remainingTimes(entitlementCode) > 0) { "offline entitlement has no remaining times" }

    store.markLocalUse(entitlementCode)
    val task = LocalTask(
      clientTaskId = newClientTaskId("offline-verification"),
      taskType = "offline_verification",
      payload = mapOf(
        "entitlementCode" to entitlementCode,
        "entitlementId" to entitlement.id,
        "pointId" to (store.binding?.locationPointId ?: ""),
        "mode" to "offline",
      ),
      occurredAt = nowIso(),
    )
    store.enqueue(task)
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.verification.offline_queued",
        summary = "终端离线核销进入待同步",
        payload = mapOf("entitlementCode" to entitlementCode, "clientTaskId" to task.clientTaskId),
        occurredAt = nowIso(),
      ),
    )
    return task
  }

  fun addManualSupplement(reason: String, note: String): LocalTask {
    val task = LocalTask(
      clientTaskId = newClientTaskId("manual-supplement"),
      taskType = "manual_supplement",
      payload = mapOf("reason" to reason, "note" to note),
      occurredAt = nowIso(),
    )
    store.enqueue(task)
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.manual_supplement.queued",
        summary = "终端人工补录进入待同步",
        payload = mapOf("clientTaskId" to task.clientTaskId, "reason" to reason),
        occurredAt = nowIso(),
      ),
    )
    return task
  }

  fun syncPendingTasks(): SyncTasksResponse {
    requireOnline()
    val binding = requireNotNull(store.binding) { "device binding is missing" }
    val pending = store.pendingTasks()
    if (pending.isEmpty()) return SyncTasksResponse(results = emptyList(), syncedAt = nowIso())

    val response = api.syncTasks(
      SyncTasksRequest(
        idempotencyKey = newClientTaskId("sync-tasks"),
        operatorNote = "Android terminal pending task sync",
        deviceCode = binding.deviceCode,
        staffId = store.session?.staffId,
        tasks = pending.map {
          SyncTaskRequest(
            clientTaskId = it.clientTaskId,
            taskType = it.taskType,
            occurredAt = it.occurredAt,
            payload = it.payload,
          )
        },
      ),
    )
    store.markSynced(response.results)
    return response
  }

  fun reportDeviceEvents(): EventBatchResponse {
    requireOnline()
    val binding = requireNotNull(store.binding) { "device binding is missing" }
    val events = store.pendingEvents()
    if (events.isEmpty()) return EventBatchResponse(accepted = 0, receivedAt = nowIso())

    val response = api.reportEvents(
      EventBatchRequest(
        idempotencyKey = stableIdempotencyKey(
          "events",
          binding.deviceCode,
          events.joinToString("|") { "${it.eventType}:${it.occurredAt}" },
        ),
        deviceCode = binding.deviceCode,
        staffId = store.session?.staffId,
        events = events,
      ),
    )
    store.clearEvents()
    return response
  }

  fun returnDevice(reason: String? = null): ReturnDecision {
    requireOnline()
    val binding = requireNotNull(store.binding) { "device binding is missing" }
    val pendingTaskCount = store.pendingTaskCount()
    if (pendingTaskCount > 0) {
      return try {
        api.returnDevice(
          DeviceReturnRequest(
            idempotencyKey = returnIdempotencyKey(binding.deviceCode, reason, pending = true),
            deviceCode = binding.deviceCode,
            staffId = store.session?.staffId,
            hasPendingTasks = true,
            reason = reason,
          ),
        )
        ReturnDecision(ReturnStatus.BLOCKED_PENDING_TASKS, pendingTaskCount, "unexpected_remote_return")
      } catch (error: ApiException) {
        if (error.statusCode != 409) throw error
        ReturnDecision(ReturnStatus.BLOCKED_PENDING_TASKS, pendingTaskCount, "RETURN_BLOCKED_PENDING_TASKS")
      }
    }

    val returned = api.returnDevice(
      DeviceReturnRequest(
        idempotencyKey = returnIdempotencyKey(binding.deviceCode, reason, pending = false),
        deviceCode = binding.deviceCode,
        staffId = store.session?.staffId,
        hasPendingTasks = false,
        reason = reason,
      ),
    )
    store.clearSensitiveData()
    return ReturnDecision(ReturnStatus.RETURNED, pendingTaskCount, returned.status)
  }

  private fun requireOnline() {
    check(networkOnline) { "terminal network is offline" }
  }

  private fun BindDeviceRequest.withStableIdempotencyKey(): BindDeviceRequest =
    if (idempotencyKey.isNotBlank()) {
      this
    } else {
      copy(idempotencyKey = stableIdempotencyKey("bind", deviceId, boundSpaceId ?: bindingScope))
    }

  private fun returnIdempotencyKey(deviceCode: String, reason: String?, pending: Boolean): String =
    stableIdempotencyKey(
      if (pending) "return-blocked" else "return-unbind-compatible",
      deviceCode,
      reason ?: "no_reason",
    )

  private fun stableIdempotencyKey(prefix: String, vararg parts: String): String {
    val raw = (listOf(prefix) + parts.toList()).joinToString("|")
    val compact = parts.joinToString("_") { sanitizeKeyPart(it) }.ifBlank { "none" }
    val hash = raw.hashCode().toString().replace("-", "n")
    return "android_${sanitizeKeyPart(prefix)}_${compact.take(80)}_$hash".take(128)
  }

  private fun sanitizeKeyPart(value: String): String =
    value.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifBlank { "none" }

  private fun newClientTaskId(prefix: String): String = "$prefix-${UUID.randomUUID()}"
}
