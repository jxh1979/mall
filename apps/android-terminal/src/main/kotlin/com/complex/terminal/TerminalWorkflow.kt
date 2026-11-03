package com.complex.terminal

// 文件版本：V0.04

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
    val returned = api.returnDevice(
      DeviceReturnRequest(
        idempotencyKey = returnIdempotencyKey(binding.deviceCode, reason),
        deviceCode = binding.deviceCode,
        staffId = store.session?.staffId,
        hasPendingTasks = false,
        reason = reason,
      ),
    )
    store.clearSensitiveData()
    return ReturnDecision(ReturnStatus.RETURNED, returned.status)
  }

  // -------------------------------------------------------------------------
  // 游乐入口核销：工作手机在线核销游乐权益；失败或断网只提示，不入队（BSM:800「核销必须在线、不做离线核销」）。
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

  private fun returnIdempotencyKey(deviceCode: String, reason: String?): String =
    stableIdempotencyKey(
      "return-unbind-compatible",
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
