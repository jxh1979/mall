package com.complex.terminal

// 文件版本：V0.03

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

  fun verifyOnline(
    entitlementCode: String,
    faceCapture: String? = null,
    faceConsentAccepted: Boolean? = null,
  ): VerificationResponse {
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
        faceCapture = faceCapture,
        faceConsentAccepted = faceConsentAccepted,
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
      originStaffId = store.session?.staffId,
      originDeviceCode = store.binding?.deviceCode,
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
      originStaffId = store.session?.staffId,
      originDeviceCode = store.binding?.deviceCode,
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
    val currentStaffId = requireNotNull(store.session?.staffId) { "terminal session is missing" }
    // 归属校验:只上送「原操作员=当前登录员工」的待同步任务。换人(clearSensitiveData 保留
    // PENDING)后,上一员工遗留的任务归属不符即不上送,绝不冒充新员工身份补登;这些任务
    // 保持 PENDING,待原员工重登后自行同步(或由主管交接处理)。
    val mine = store.pendingTasks().filter { it.originStaffId == currentStaffId }
    if (mine.isEmpty()) return SyncTasksResponse(results = emptyList(), syncedAt = nowIso())

    val response = api.syncTasks(
      SyncTasksRequest(
        idempotencyKey = newClientTaskId("sync-tasks"),
        operatorNote = "Android terminal pending task sync",
        deviceCode = binding.deviceCode,
        staffId = currentStaffId,
        tasks = mine.map {
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

  // -------------------------------------------------------------------------
  // 游乐入口核销：工作手机在线核销游乐权益；失败/离线时进入待同步队列。
  // -------------------------------------------------------------------------
  fun amusementVerifyOnline(entitlementCode: String, sessionId: String? = null): AmusementVerificationRecord {
    requireOnline()
    val session = requireNotNull(store.session) { "terminal session is missing" }
    val binding = requireNotNull(store.binding) { "device binding is missing" }
    val businessContext = buildMap {
      put("scenario", "amusement_entry")
      put("staff_id", session.staffId)
      if (sessionId != null) put("session_id", sessionId)
    }
    val record = api.amusementVerify(
      VerificationRequest(
        idempotencyKey = newClientTaskId("amusement-verification"),
        credentialPayload = entitlementCode,
        deviceId = binding.id,
        pointId = binding.locationPointId ?: binding.deviceCode,
        businessContext = businessContext,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.amusement.verification.online",
        summary = "游乐入口在线核销完成",
        payload = mapOf("entitlementCode" to entitlementCode, "result" to record.verificationResult),
        occurredAt = nowIso(),
      ),
    )
    return record
  }

  // 游乐离线核销：进入待同步队列（复用 offline_verification 同步通道）。
  fun amusementVerifyOffline(entitlementCode: String, sessionId: String? = null): LocalTask {
    val task = LocalTask(
      clientTaskId = newClientTaskId("amusement-offline-verification"),
      taskType = "offline_verification",
      payload = buildMap {
        put("entitlementCode", entitlementCode)
        put("pointId", store.binding?.locationPointId ?: "")
        put("scenario", "amusement_entry")
        put("mode", "offline")
        if (sessionId != null) put("sessionId", sessionId)
      },
      occurredAt = nowIso(),
      originStaffId = store.session?.staffId,
      originDeviceCode = store.binding?.deviceCode,
    )
    store.enqueue(task)
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.amusement.verification.offline_queued",
        summary = "游乐离线核销进入待同步",
        payload = mapOf("entitlementCode" to entitlementCode, "clientTaskId" to task.clientTaskId),
        occurredAt = nowIso(),
      ),
    )
    return task
  }

  // -------------------------------------------------------------------------
  // 游乐检修：提交检修记录 -> 主管确认恢复运营。
  // 第十八批 N 段第 120 条 120-R6（BSM:992「工程师可在授权后填写游乐设施定期检修记录，检修单位作为供应商记录。」；BSM:993）：
  // 来源终端取本机终端类型（kiosk、staff_work_phone 都在服务端闭集内），检修单位、检修人员必填；定期检修同一设备会反复提交，
  // 每次提交新幂等键（服务端检修记录号由幂等键派生，固定键会把上一次的记录覆盖）；确认人由服务端取当前登录主管。
  // -------------------------------------------------------------------------
  fun submitAmusementMaintenance(
    projectId: String,
    deviceId: String,
    maintenanceVendorId: String,
    engineerName: String,
    result: String,
    exceptionNote: String? = null,
    engineerId: String? = null,
    locationPointId: String? = null,
  ): AmusementMaintenanceRecord {
    requireOnline()
    val record = api.submitAmusementMaintenance(
      AmusementMaintenanceRecordRequest(
        idempotencyKey = UUID.randomUUID().toString(),
        projectId = projectId,
        deviceId = deviceId,
        locationPointId = locationPointId ?: store.binding?.locationPointId,
        maintenanceVendorId = maintenanceVendorId,
        engineerId = engineerId ?: store.session?.staffId,
        engineerName = engineerName,
        maintenanceAt = nowIso(),
        result = result,
        exceptionNote = exceptionNote,
        sourceTerminal = api.terminalType,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.amusement.maintenance.submitted",
        summary = "游乐检修记录已提交",
        payload = mapOf("deviceId" to deviceId, "result" to result, "recordId" to record.id),
        occurredAt = nowIso(),
      ),
    )
    return record
  }

  fun confirmAmusementMaintenanceResume(
    recordId: String,
    remark: String? = null,
  ): AmusementMaintenanceRecord {
    requireOnline()
    val record = api.confirmAmusementMaintenanceResume(
      recordId,
      AmusementMaintenanceResumeRequest(
        idempotencyKey = stableIdempotencyKey("amusement-maintenance-resume", recordId),
        remark = remark,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.amusement.maintenance.resumed",
        summary = "游乐检修主管确认恢复运营",
        payload = mapOf("recordId" to recordId, "status" to record.status),
        occurredAt = nowIso(),
      ),
    )
    return record
  }

  // -------------------------------------------------------------------------
  // 酒吧一体机/平板：点酒下单 -> （针对账单）支付意图 -> 结算。
  // -------------------------------------------------------------------------
  fun createBarOrder(
    barId: String,
    items: List<Map<String, String>>,
    tableId: String? = null,
    reservationId: String? = null,
    paymentMode: String? = null,
  ): BarOrderRecord {
    requireOnline()
    val order = api.createBarOrder(
      BarOrderRequest(
        idempotencyKey = newClientTaskId("bar-order"),
        operatorNote = "Android terminal bar order",
        barId = barId,
        tableId = tableId,
        reservationId = reservationId,
        items = items,
        paymentMode = paymentMode,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.bar.order.created",
        summary = "酒吧终端点酒下单",
        payload = mapOf("barId" to barId, "orderId" to order.id, "status" to order.status),
        occurredAt = nowIso(),
      ),
    )
    return order
  }

  /** 由订单生成账单（按桌整单结账前置步）。 */
  fun createBarOrderBill(orderId: String): BarBillRecord {
    requireOnline()
    val bill = api.createBarOrderBill(
      orderId,
      BarOrderBillCreateRequest(
        idempotencyKey = stableIdempotencyKey("bar-order-bill", orderId),
        operatorNote = "Android terminal bar order bill",
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.bar.bill.created",
        summary = "酒吧终端由订单生成账单",
        payload = mapOf("orderId" to orderId, "billId" to bill.id),
        occurredAt = nowIso(),
      ),
    )
    return bill
  }

  fun createBarPaymentIntent(
    billId: String,
    amountCent: Long,
    channel: String,
    currency: String = "CNY",
  ): PaymentIntentRecord {
    requireOnline()
    val intent = api.createBarPaymentIntent(
      billId,
      PaymentIntentRequest(
        idempotencyKey = stableIdempotencyKey("bar-pay-intent", billId, amountCent.toString(), channel),
        operatorNote = "Android terminal bar payment intent",
        businessOrderType = "bar_bill",
        businessOrderId = billId,
        amountCent = amountCent,
        currency = currency,
        channel = channel,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.bar.payment_intent.created",
        summary = "酒吧终端创建支付意图",
        payload = mapOf("billId" to billId, "intentId" to intent.id, "status" to intent.status),
        occurredAt = nowIso(),
      ),
    )
    return intent
  }

  fun settleBarBill(
    billId: String,
    settlementAmountCent: Long,
    paymentChannel: String,
    paymentId: String? = null,
    posTransactionNo: String? = null,
  ): BarBillRecord {
    requireOnline()
    val bill = api.settleBarBill(
      billId,
      BarBillSettleRequest(
        idempotencyKey = stableIdempotencyKey("bar-settle", billId, settlementAmountCent.toString()),
        operatorNote = "Android terminal bar settlement",
        paymentId = paymentId,
        settlementAmountCent = settlementAmountCent,
        paymentChannel = paymentChannel,
        cashierStaffId = store.session?.staffId,
        posTransactionNo = posTransactionNo,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.bar.bill.settled",
        summary = "酒吧终端账单结算",
        payload = mapOf("billId" to billId, "settlementStatus" to (bill.settlementStatus ?: "")),
        occurredAt = nowIso(),
      ),
    )
    return bill
  }

  // -------------------------------------------------------------------------
  // 员工设备凭证：换机绑定 -> 主管批准换机 -> 完整性上报。
  // -------------------------------------------------------------------------
  fun bindStaffDeviceCredential(
    staffProfileId: String,
    devicePublicKey: String,
    deviceFingerprintHash: String,
  ): StaffDeviceCredentialRecord {
    requireOnline()
    val record = api.bindStaffDeviceCredential(
      StaffDeviceBindRequest(
        idempotencyKey = stableIdempotencyKey("staff-device-bind", staffProfileId, deviceFingerprintHash),
        operatorNote = "Android terminal device credential bind",
        staffProfileId = staffProfileId,
        devicePublicKey = devicePublicKey,
        deviceFingerprintHash = deviceFingerprintHash,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.staff_device.bound",
        summary = "员工设备凭证绑定",
        payload = mapOf("credentialId" to record.id, "bindStatus" to record.bindStatus),
        occurredAt = nowIso(),
      ),
    )
    return record
  }

  fun approveStaffDeviceRebind(credentialId: String): StaffDeviceCredentialRecord {
    requireOnline()
    val record = api.approveStaffDeviceRebind(
      credentialId,
      StaffDeviceRebindDecisionRequest(
        idempotencyKey = stableIdempotencyKey("staff-device-approve-rebind", credentialId),
        operatorNote = "Android terminal approve device rebind",
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.staff_device.rebind_approved",
        summary = "员工设备换机已批准",
        payload = mapOf("credentialId" to credentialId, "bindStatus" to record.bindStatus),
        occurredAt = nowIso(),
      ),
    )
    return record
  }

  fun reportStaffDeviceIntegrity(
    credentialId: String,
    integrityStatus: String,
  ): StaffDeviceCredentialRecord {
    requireOnline()
    val record = api.reportStaffDeviceIntegrity(
      credentialId,
      StaffDeviceIntegrityReportRequest(
        idempotencyKey = stableIdempotencyKey("staff-device-integrity", credentialId, integrityStatus),
        operatorNote = "Android terminal device integrity report",
        integrityStatus = integrityStatus,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.staff_device.integrity_reported",
        summary = "员工设备完整性上报",
        payload = mapOf("credentialId" to credentialId, "integrityStatus" to (record.integrityStatus ?: integrityStatus)),
        occurredAt = nowIso(),
      ),
    )
    return record
  }

  // -------------------------------------------------------------------------
  // 票务核销冲正：对已核销记录冲正（撤销）。
  // -------------------------------------------------------------------------
  fun reverseVerification(verificationId: String, reason: String): VerificationResponse {
    requireOnline()
    val response = api.reverseVerification(
      verificationId,
      VerificationReverseRequest(
        idempotencyKey = stableIdempotencyKey("verification-reverse", verificationId),
        reason = reason,
      ),
    )
    store.addEvent(
      TerminalEvent(
        eventType = "terminal.verification.reversed",
        summary = "票务核销冲正完成",
        payload = mapOf("verificationId" to verificationId, "status" to response.status, "reason" to reason),
        occurredAt = nowIso(),
      ),
    )
    return response
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
