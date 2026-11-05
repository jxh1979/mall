package com.complex.terminal

// 文件版本：V0.07

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 现场工作流编排测试：使用 stub HttpClient 拦截请求，断言路径/请求体字段并返回固定响应。
 * 覆盖：游乐核销（成功/失败/断网拒绝）、游乐检修提交+主管确认、酒吧点酒+支付意图+结算、
 * 员工设备换机绑定+批准+完整性上报、票务核销冲正。
 */
class TerminalFieldWorkflowTest {
  private fun newApi(handler: StubHttpClient.Handler): TerminalApiClient {
    val client = StubHttpClient(handler)
    val api = TerminalApiClient("http://127.0.0.1:1", client)
    api.accessToken = "test-token"
    return api
  }

  private fun storeWithSessionAndBinding(): InMemoryLocalStore {
    val store = InMemoryLocalStore()
    store.saveSession(TerminalSession("test-token", "staff-1", "operator"))
    store.saveBinding(
      DeviceBinding(
        id = "device-1",
        deviceCode = "P0-ANDROID-1",
        locationPointId = "point-1",
        status = "bound",
      ),
    )
    return store
  }

  // 第十八批 Q 段第 146 条 146-R1（BSM:552「工作手机扫客人二维码核销各酒吧入场、赠饮、游乐等场内权益」；BSM:766「票里的游乐权益到项目入口核销。」）：
  // 游乐核销走票务核销入口，项目由入口点位或设备定位、场次取当前开放场次，端上不传项目与场次。
  @Test
  fun amusementVerifyOnlineSendsTicketingContractAndRecordsEvent() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val api = newApi { req ->
      captured += req
      assertEquals("/api/v1/ticketing/verifications", req.path)
      okJson("""{"id":"ver-1","verification_no":"V-1","status":"success"}""")
    }
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)

    val response = workflow.amusementVerifyOnline("E-AMUSE-1")

    val body = captured.single().body
    assertTrue("\"credential_payload\":\"E-AMUSE-1\"" in body, body)
    assertTrue("\"point_id\":\"point-1\"" in body, body)
    assertTrue("\"idempotency_key\"" in body, body)
    assertTrue("\"scenario\":\"amusement_entry\"" in body, body)
    assertFalse("project_id" in body, body)
    assertFalse("session_id" in body, body)
    assertEquals("success", response.status)
    assertTrue(store.pendingEvents().any { it.eventType == "terminal.amusement.verification.online" && it.payload["result"] == "success" })
  }

  @Test
  fun amusementVerifyOnlineSurfacesBackendFailureResponse() {
    val api = newApi { req ->
      assertEquals("/api/v1/ticketing/verifications", req.path)
      StubResponse(409, """{"code":"CONFLICT","detail_code":"ENTITLEMENT_EXPIRED","message":"权益已过期"}""")
    }
    val workflow = TerminalWorkflow(api, storeWithSessionAndBinding())

    val error = assertFailsWith<ApiException> { workflow.amusementVerifyOnline("E-EXPIRED") }

    assertEquals(409, error.statusCode)
  }

  @Test
  fun amusementMaintenanceSubmitThenConfirmResume() {
    val paths = mutableListOf<String>()
    val api = newApi { req ->
      paths += req.path
      when {
        req.path == "/api/v1/amusement/maintenance-records" -> {
          // 120-R6：来源终端取本机终端类型（服务端闭集内），检修单位必填。
          assertTrue("\"source_terminal\":\"staff_work_phone\"" in req.body)
          assertTrue("\"maintenance_vendor_id\":\"sup-1\"" in req.body)
          assertTrue("\"engineer_name\":\"张工\"" in req.body)
          assertTrue("\"device_id\":\"ride-1\"" in req.body)
          okJson("""{"id":"mr-1","status":"pending_resume","device_id":"ride-1"}""")
        }
        req.path == "/api/v1/amusement/maintenance-records/mr-1/confirm-resume" -> {
          // 120-R6：确认人由服务端取当前登录主管，请求不带。
          assertFalse("\"supervisor_confirmed_by\"" in req.body)
          assertTrue("\"remark\":\"已检修\"" in req.body)
          okJson("""{"id":"mr-1","status":"resumed"}""")
        }
        else -> error("unexpected path ${req.path}")
      }
    }
    api.terminalType = "staff_work_phone"
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)

    val submitted = workflow.submitAmusementMaintenance(
      projectId = "proj-1",
      deviceId = "ride-1",
      maintenanceVendorId = "sup-1",
      engineerName = "张工",
      result = "pass",
      exceptionNote = null,
    )
    assertEquals("mr-1", submitted.id)

    val resumed = workflow.confirmAmusementMaintenanceResume(
      recordId = submitted.id,
      remark = "已检修",
    )
    assertEquals("resumed", resumed.status)
    assertEquals(
      listOf(
        "/api/v1/amusement/maintenance-records",
        "/api/v1/amusement/maintenance-records/mr-1/confirm-resume",
      ),
      paths,
    )
  }

  @Test
  fun barOrderPaymentIntentAndSettlement() {
    val paths = mutableListOf<String>()
    val api = newApi { req ->
      paths += req.path
      when (req.path) {
        "/api/v1/bar/orders" -> {
          assertTrue("\"bar_id\":\"bar-1\"" in req.body)
          assertTrue("\"items\"" in req.body)
          // 第十八批 I 段 86-R1（BSM 第 1070 行「加单、送达等其他经手人只留责任痕迹、不产生销售提成」）：订单销售由服务端取开台会话的销售，终端不发下单人。
          assertTrue("salesperson_user_id" !in req.body)
          okJson("""{"id":"order-1","bar_id":"bar-1","status":"open","total_amount_cent":9900}""")
        }
        "/api/v1/bar/bills/bill-1/pay-intents" -> {
          // 第十八批 P 段第 137 条 137-R1：扫码结账先按账单剩余应收建支付意图。
          assertTrue("\"business_order_type\":\"bar_bill\"" in req.body)
          assertTrue("\"business_order_id\":\"bill-1\"" in req.body)
          assertTrue("\"amount_cent\":9900" in req.body)
          assertTrue("\"channel\":\"wechat\"" in req.body)
          okJson("""{"id":"intent-1","amount_cent":9900,"status":"created"}""")
        }
        "/api/v1/bar/bills/bill-1/settle" -> {
          // 137-R1：结账带支付意图编号 payment_id（服务端必填，BarBillingService.settleBill）。
          assertTrue("\"payment_id\":\"intent-1\"" in req.body)
          assertTrue("\"payment_channel\":\"wechat\"" in req.body)
          assertTrue("\"settlement_amount_cent\":9900" in req.body)
          okJson("""{"id":"bill-1","payment_status":"paid","settlement_status":"settled"}""")
        }
        else -> error("unexpected path ${req.path}")
      }
    }
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)

    val order = workflow.createBarOrder(
      barId = "bar-1",
      items = listOf(mapOf("sku" to "beer", "qty" to "2")),
      tableId = "table-1",
    )
    assertEquals("order-1", order.id)

    val bill = workflow.settleBarBillByScan(BarBillRecord(id = "bill-1", payableAmountCent = 9900), "wechat")
    assertEquals("settled", bill.settlementStatus)
    assertEquals(
      listOf(
        "/api/v1/bar/orders",
        "/api/v1/bar/bills/bill-1/pay-intents",
        "/api/v1/bar/bills/bill-1/settle",
      ),
      paths,
    )
  }

  @Test
  fun barScanSettlementUsesRemainingDueIncludingTip() {
    // 137-R1：意图与结账金额＝应付 + 小费 − 已付（与服务端支付意向金额校验同式，BarBillingService.createBillPayIntent）。
    val bodies = mutableMapOf<String, String>()
    val api = newApi { req ->
      bodies[req.path] = req.body
      when (req.path) {
        "/api/v1/bar/bills/bill-2/pay-intents" -> okJson("""{"id":"intent-2","amount_cent":10500,"status":"created"}""")
        "/api/v1/bar/bills/bill-2/settle" -> okJson("""{"id":"bill-2","payment_status":"paid","settlement_status":"settled"}""")
        else -> error("unexpected path ${req.path}")
      }
    }
    val workflow = TerminalWorkflow(api, storeWithSessionAndBinding())

    workflow.settleBarBillByScan(
      BarBillRecord(id = "bill-2", payableAmountCent = 12000, paidAmountCent = 2000, tipAmountCent = 500), "alipay")

    assertTrue("\"amount_cent\":10500" in bodies["/api/v1/bar/bills/bill-2/pay-intents"].orEmpty())
    val settle = bodies["/api/v1/bar/bills/bill-2/settle"].orEmpty()
    assertTrue("\"settlement_amount_cent\":10500" in settle)
    assertTrue("\"payment_id\":\"intent-2\"" in settle)
    // 第十八批 Q 段第 153 条 153-R2（BSM:940「……一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕」）：收款人由服务端按本员工会话定，结账体不发 cashier_staff_id（本工作流带登录会话 staff-1）。
    assertFalse("cashier_staff_id" in settle)
  }

  @Test
  fun barScanSettlementOfZeroDueSettlesWithoutPaymentIntent() {
    // 137-R1 复核修正（BSM:1064「……以该笔留款抵付的部分不重复收款、不重复确认收入。」）：留款抵完、剩余应收为 0 的账单不建支付意图
    // （服务端 amount_cent 须大于 0），结账带本次幂等键作 payment_id、settlement_amount_cent=0，由服务端关单（先例 120-R4 KioskPayment）。
    val bodies = mutableMapOf<String, String>()
    val api = newApi { req ->
      bodies[req.path] = req.body
      when (req.path) {
        "/api/v1/bar/bills/bill-0/pay-intents" ->
          StubResponse(400, """{"detail_code":"VALIDATION_FAILED","message":"amount_cent 必须大于 0"}""")
        "/api/v1/bar/bills/bill-0/settle" -> okJson("""{"id":"bill-0","payment_status":"paid","settlement_status":"settled"}""")
        else -> error("unexpected path ${req.path}")
      }
    }
    val workflow = TerminalWorkflow(api, storeWithSessionAndBinding())

    runCatching { workflow.settleBarBillByScan(BarBillRecord(id = "bill-0", payableAmountCent = 0), "wechat") }

    assertFalse("/api/v1/bar/bills/bill-0/pay-intents" in bodies)
    val settle = bodies["/api/v1/bar/bills/bill-0/settle"].orEmpty()
    assertTrue("\"settlement_amount_cent\":0" in settle)
    val key = Regex("\"idempotency_key\":\"([^\"]+)\"").find(settle)?.groupValues?.get(1)
    assertNotNull(key)
    assertTrue("\"payment_id\":\"$key\"" in settle)
  }

  @Test
  fun staffDeviceRebindBindApproveAndIntegrityReport() {
    val paths = mutableListOf<String>()
    val api = newApi { req ->
      paths += req.path
      when (req.path) {
        "/api/v1/staff/device-credentials" -> {
          assertTrue("\"device_fingerprint_hash\":\"hash-new\"" in req.body)
          okJson("""{"id":"cred-1","bind_status":"pending_rebind_approval"}""")
        }
        "/api/v1/staff/device-credentials/cred-1/approve-rebind" -> {
          assertTrue("\"idempotency_key\"" in req.body)
          okJson("""{"id":"cred-1","bind_status":"active"}""")
        }
        "/api/v1/staff/device-credentials/cred-1/report-integrity" -> {
          assertTrue("\"integrity_status\":\"compromised\"" in req.body)
          okJson("""{"id":"cred-1","bind_status":"active","integrity_status":"compromised"}""")
        }
        else -> error("unexpected path ${req.path}")
      }
    }
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)

    val bound = workflow.bindStaffDeviceCredential(
      staffProfileId = "profile-1",
      devicePublicKey = "pub",
      deviceFingerprintHash = "hash-new",
    )
    assertEquals("cred-1", bound.id)

    val approved = workflow.approveStaffDeviceRebind(bound.id)
    assertEquals("active", approved.bindStatus)

    val reported = workflow.reportStaffDeviceIntegrity(bound.id, integrityStatus = "compromised")
    assertEquals("compromised", reported.integrityStatus)
    assertEquals(
      listOf(
        "/api/v1/staff/device-credentials",
        "/api/v1/staff/device-credentials/cred-1/approve-rebind",
        "/api/v1/staff/device-credentials/cred-1/report-integrity",
      ),
      paths,
    )
  }

  @Test
  fun reverseVerificationSendsReasonAndRecordsEvent() {
    val api = newApi { req ->
      assertEquals("/api/v1/ticketing/verifications/ver-1/reverse", req.path)
      assertTrue("\"reason\":\"误核销\"" in req.body)
      okJson("""{"id":"ver-1","verification_no":"V-1","status":"reversed","result_reason":"误核销"}""")
    }
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)

    val response = workflow.reverseVerification("ver-1", reason = "误核销")

    assertEquals("reversed", response.status)
    assertNotNull(response.verificationNo)
    assertTrue(store.pendingEvents().any { it.eventType == "terminal.verification.reversed" })
  }

  @Test
  fun offlineGuardBlocksFieldWorkflows() {
    var calls = 0
    val api = newApi { calls++; error("network must not be called when offline") }
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)
    workflow.networkOnline = false

    var threw = false
    try {
      workflow.amusementVerifyOnline("E-1")
    } catch (e: IllegalStateException) {
      threw = true
    }
    assertTrue(threw, "online amusement verify should require network")

    // 第十八批 O 段第 129 条 129-R2（BSM:800「核销必须在线、不做离线核销」）：门票/权益核销断网时也只报错，不发请求、不入队。
    assertFailsWith<IllegalStateException> { workflow.verifyOnline("E-1") }
    assertEquals(0, calls, "断网时不得发出任何请求")
  }

  // 第十八批 O 段第 129 条 129-R2（BSM:800「核销必须在线、不做离线核销」）：离线权益包、离线核销与人工补录的本地队列、
  // 待同步任务回传入口都已删除，端上只剩在线核销。
  @Test
  fun offlineQueueAndSyncEntriesAreRemoved() {
    val removed = setOf(
      "refreshOfflinePackage", "verifyOffline", "amusementVerifyOffline", "addManualSupplement", "syncPendingTasks",
      "offlineEntitlements", "syncTasks", "replaceOfflinePackage", "enqueue", "pendingTaskCount",
    )
    val present = listOf(TerminalWorkflow::class.java, TerminalApiClient::class.java, InMemoryLocalStore::class.java)
      .flatMap { type -> type.methods.map { it.name.substringBefore('$') } }
      .filter { it in removed }
      .distinct()
    assertEquals(emptyList(), present)
  }

  // TK-06（BSM:780 一人多次票首次核销留存人脸、后续以该人脸核对）：预检读 face_binding；核销带上本次拍照与客人同意。
  @Test
  fun verifyOnlineSendsFaceCaptureForMultiUseFirstEntry() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val api = newApi { req ->
      captured += req
      when (req.path) {
        "/api/v1/ticketing/verifications/preview" ->
          okJson("""{"allowed":true,"reason":"ok","face_binding":"capture"}""")
        "/api/v1/ticketing/verifications" ->
          okJson("""{"id":"ver-9","verification_no":"V-9","status":"success"}""")
        else -> error("unexpected path ${req.path}")
      }
    }
    val workflow = TerminalWorkflow(api, storeWithSessionAndBinding())

    assertEquals("capture", workflow.previewOnline("E-MULTI-1").faceBinding)
    workflow.verifyOnline("E-MULTI-1", faceCapture = "RkFDRQ==", faceConsentAccepted = true)

    val body = captured.last().body
    assertTrue("\"face_capture\":\"RkFDRQ==\"" in body, body)
    assertTrue("\"face_consent_accepted\":true" in body, body)
  }

  @Test
  fun verifyOnlineWithoutFaceSendsNoFaceKeys() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val api = newApi { req ->
      captured += req
      okJson("""{"id":"ver-10","verification_no":"V-10","status":"success"}""")
    }
    TerminalWorkflow(api, storeWithSessionAndBinding()).verifyOnline("E-SINGLE-1")

    assertTrue("face_capture" !in captured.single().body)
    assertTrue("face_consent_accepted" !in captured.single().body)
  }

  // 第十八批 Q 段第 149 条 149-R1（ENUM:43「检票环节内部为一次动作：扫码即核销，不设先预检、后确认的两次操作，也不设「预检通过」中间态。」；DEC-R6-08）：
  // 核销屏扫码后在同一次动作里只读本票人脸处置（TK-06），读失败按不需要人脸处理，结果以核销为准。
  @Test
  fun faceBindingForReadsFaceDispositionAndTreatsFailureAsNone() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    var reply: HttpResponse<String> = okJson("""{"allowed":true,"reason":"ok","face_binding":"capture"}""")
    val workflow = TerminalWorkflow(newApi { req -> captured += req; reply }, storeWithSessionAndBinding())

    assertEquals("capture", workflow.faceBindingFor("E-MULTI-1"))
    reply = okJson("""{"allowed":true,"reason":"ok"}""")
    assertNull(workflow.faceBindingFor("E-SINGLE-1"))
    reply = StubResponse(409, """{"code":"CONFLICT","detail_code":"ENTITLEMENT_EXPIRED","message":"权益已过期"}""")
    assertNull(workflow.faceBindingFor("E-EXPIRED"))

    assertEquals(List(3) { "/api/v1/ticketing/verifications/preview" }, captured.map { it.path })
    val body = captured.first().body
    listOf("\"credential_payload\":\"E-MULTI-1\"", "\"device_id\":\"device-1\"", "\"point_id\":\"point-1\"", "\"idempotency_key\"")
      .forEach { assertTrue(it in body, body) }
  }

  // 149-R1：核销屏一次动作的顺序——先只读人脸处置，为空即提交核销，核销不带人脸。
  @Test
  fun ticketScreenVerifiesRightAfterReadingNoFaceDisposition() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val api = newApi { req ->
      captured += req
      when (req.path) {
        "/api/v1/ticketing/verifications/preview" -> okJson("""{"allowed":true,"reason":"ok"}""")
        "/api/v1/ticketing/verifications" -> okJson("""{"id":"ver-11","verification_no":"V-11","status":"success"}""")
        else -> error("unexpected path ${req.path}")
      }
    }
    val workflow = TerminalWorkflow(api, storeWithSessionAndBinding())

    val response = if (workflow.faceBindingFor("E-SINGLE-2") == null) workflow.verifyOnline("E-SINGLE-2") else null

    assertEquals(listOf("/api/v1/ticketing/verifications/preview", "/api/v1/ticketing/verifications"), captured.map { it.path })
    assertTrue("face_capture" !in captured.last().body, captured.last().body)
    assertEquals("success", response?.status)
  }

  // 第十八批 S 段第 164 条 164-R1（PGM:287「展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错……必须全部为中文，不得出现任何英文」）：
  // 核销屏、领取屏把核销应答的原因码显示成中文。
  @Test
  fun verifyOnlineResultReasonShowsChinese() {
    val api = newApi { req ->
      assertEquals("/api/v1/ticketing/verifications", req.path)
      okJson("""{"id":"ver-1","verification_no":"V-1","status":"success","result_reason":"session_reentry"}""")
    }

    val response = TerminalWorkflow(api, storeWithSessionAndBinding()).verifyOnline("E-1")

    assertEquals("本场次再次入场，不扣次", verificationReasonZh(response.resultReason))
  }

  // 164-R1：核销接口会回的五个原因码都有中文、不含英文；首次核销成功（空）与其它码不显示原码。
  @Test
  fun verificationReasonZhCoversAllReasonsWithoutEnglish() {
    listOf("window_expired", "signature_invalid", "session_reentry", "day_replay", "amusement_verified").forEach {
      val zh = verificationReasonZh(it)
      assertTrue(zh.isNotEmpty() && !Regex("[A-Za-z]").containsMatchIn(zh), "$it -> $zh")
    }
    assertEquals("", verificationReasonZh(null))
    assertEquals("", verificationReasonZh("unknown_reason"))
  }

  private fun okJson(body: String): HttpResponse<String> = StubResponse(200, body)
}

/** 简易 HttpClient stub：把 send() 委托给 handler，记录 path/body。 */
private class StubHttpClient(
  private val handler: Handler,
) : HttpClient() {
  data class Captured(val path: String, val body: String, val method: String)

  fun interface Handler {
    fun handle(captured: Captured): HttpResponse<String>
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any?> send(
    request: HttpRequest,
    responseBodyHandler: HttpResponse.BodyHandler<T>,
  ): HttpResponse<T> {
    val body = extractBody(request)
    val path = request.uri().path + (request.uri().query?.let { "?$it" } ?: "")
    val captured = Captured(path = path, body = body, method = request.method())
    return handler.handle(captured) as HttpResponse<T>
  }

  private fun extractBody(request: HttpRequest): String {
    val publisher = request.bodyPublisher().orElse(null) ?: return ""
    val subscriber = BodyCollector()
    publisher.subscribe(subscriber)
    return subscriber.result()
  }

  // 以下方法在测试中不会被调用。
  override fun cookieHandler() = java.util.Optional.empty<java.net.CookieHandler>()
  override fun connectTimeout() = java.util.Optional.empty<java.time.Duration>()
  override fun followRedirects() = Redirect.NEVER
  override fun proxy() = java.util.Optional.empty<java.net.ProxySelector>()
  override fun sslContext(): javax.net.ssl.SSLContext = javax.net.ssl.SSLContext.getDefault()
  override fun sslParameters() = javax.net.ssl.SSLParameters()
  override fun authenticator() = java.util.Optional.empty<java.net.Authenticator>()
  override fun version() = Version.HTTP_1_1
  override fun executor() = java.util.Optional.empty<java.util.concurrent.Executor>()

  override fun <T : Any?> sendAsync(
    request: HttpRequest,
    responseBodyHandler: HttpResponse.BodyHandler<T>,
  ): java.util.concurrent.CompletableFuture<HttpResponse<T>> =
    java.util.concurrent.CompletableFuture.completedFuture(send(request, responseBodyHandler))

  override fun <T : Any?> sendAsync(
    request: HttpRequest,
    responseBodyHandler: HttpResponse.BodyHandler<T>,
    pushPromiseHandler: HttpResponse.PushPromiseHandler<T>?,
  ): java.util.concurrent.CompletableFuture<HttpResponse<T>> = sendAsync(request, responseBodyHandler)
}

/** 收集 BodyPublisher 内容为字符串。 */
private class BodyCollector : java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> {
  private val builder = StringBuilder()
  private val done = java.util.concurrent.CountDownLatch(1)

  override fun onSubscribe(subscription: java.util.concurrent.Flow.Subscription) {
    subscription.request(Long.MAX_VALUE)
  }

  override fun onNext(item: java.nio.ByteBuffer) {
    val bytes = ByteArray(item.remaining())
    item.get(bytes)
    builder.append(String(bytes, Charsets.UTF_8))
  }

  override fun onError(throwable: Throwable) {
    done.countDown()
  }

  override fun onComplete() {
    done.countDown()
  }

  fun result(): String {
    done.await(2, java.util.concurrent.TimeUnit.SECONDS)
    return builder.toString()
  }
}

/** 简易 HttpResponse stub。 */
private class StubResponse(
  private val status: Int,
  private val body: String,
) : HttpResponse<String> {
  override fun statusCode(): Int = status
  override fun request(): HttpRequest = throw UnsupportedOperationException()
  override fun previousResponse(): java.util.Optional<HttpResponse<String>> = java.util.Optional.empty()
  override fun headers(): java.net.http.HttpHeaders =
    java.net.http.HttpHeaders.of(emptyMap()) { _, _ -> true }
  override fun body(): String = body
  override fun sslSession(): java.util.Optional<javax.net.ssl.SSLSession> = java.util.Optional.empty()
  override fun uri(): java.net.URI = java.net.URI.create("http://127.0.0.1:1")
  override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
}
