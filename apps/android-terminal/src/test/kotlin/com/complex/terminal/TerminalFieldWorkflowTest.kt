package com.complex.terminal

// 文件版本：V0.03

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 现场工作流编排测试：使用 stub HttpClient 拦截请求，断言路径/请求体字段并返回固定响应。
 * 覆盖：游乐核销（成功/失败/离线）、游乐检修提交+主管确认、酒吧点酒+支付意图+结算、
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

  @Test
  fun amusementVerifyOnlineSendsTicketingContractAndRecordsEvent() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val api = newApi { req ->
      captured += req
      assertEquals("/api/v1/amusement/verifications", req.path)
      assertTrue("\"credential_payload\":\"E-AMUSE-1\"" in req.body)
      assertTrue("\"point_id\":\"point-1\"" in req.body)
      assertTrue("\"idempotency_key\"" in req.body)
      assertTrue("\"session_id\":\"sess-9\"" in req.body)
      okJson("""{"id":"amv-1","verification_result":"success","session_id":"sess-9"}""")
    }
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)

    val record = workflow.amusementVerifyOnline("E-AMUSE-1", sessionId = "sess-9")

    assertEquals("amv-1", record.id)
    assertEquals("success", record.verificationResult)
    assertEquals(1, captured.size)
    assertTrue(store.pendingEvents().any { it.eventType == "terminal.amusement.verification.online" })
  }

  @Test
  fun amusementVerifyOfflineQueuesTaskWithoutNetwork() {
    val api = newApi { error("should not call network when offline") }
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)
    workflow.networkOnline = false

    val task = workflow.amusementVerifyOffline("E-AMUSE-2", sessionId = "sess-1")

    assertEquals("offline_verification", task.taskType)
    assertEquals("amusement_entry", task.payload["scenario"])
    assertEquals(1, store.pendingTaskCount())
  }

  @Test
  fun amusementVerifyOnlineSurfacesBackendFailureResponse() {
    val api = newApi { okJson("""{"id":"amv-2","verification_result":"failed","failure_reason":"expired"}""") }
    val store = storeWithSessionAndBinding()
    val workflow = TerminalWorkflow(api, store)

    val record = workflow.amusementVerifyOnline("E-EXPIRED")

    assertEquals("failed", record.verificationResult)
    assertEquals("expired", record.failureReason)
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
          assertTrue("\"amount_cent\":9900" in req.body)
          assertTrue("\"channel\":\"wechat\"" in req.body)
          okJson("""{"id":"intent-1","amount_cent":9900,"status":"created"}""")
        }
        "/api/v1/bar/bills/bill-1/settle" -> {
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

    val intent = workflow.createBarPaymentIntent(billId = "bill-1", amountCent = 9900, channel = "wechat")
    assertEquals("created", intent.status)

    val bill = workflow.settleBarBill(billId = "bill-1", settlementAmountCent = 9900, paymentChannel = "wechat")
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
    val api = newApi { error("network must not be called when offline") }
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
  }

  @Test
  fun syncPendingTasksOnlyUploadsCurrentOperatorTasks() {
    val syncBodies = mutableListOf<String>()
    val api = newApi { req ->
      assertEquals("/api/v1/terminals/sync-tasks", req.path)
      syncBodies += req.body
      okJson("""{"synced_at":"t","results":[]}""")
    }
    val store = storeWithSessionAndBinding() // 会话 staff-1
    val workflow = TerminalWorkflow(api, store)

    // staff-1 离线核销产生待同步任务(归属固化到 staff-1)。
    workflow.networkOnline = false
    val task = workflow.amusementVerifyOffline("E-AMUSE-9")
    workflow.networkOnline = true

    // 换人:同一设备换 staff-2 登录(binding 不变,仅会话改)。
    store.saveSession(TerminalSession("token2", "staff-2", "operator2"))
    workflow.syncPendingTasks()
    assertTrue(syncBodies.isEmpty(), "新员工不得上送上一员工的待同步任务")
    assertEquals(1, store.pendingTaskCount(), "外来任务应保持 PENDING")

    // 原员工 staff-1 重登后可正常同步自己的任务,且以 staff-1 身份上送。
    store.saveSession(TerminalSession("token1", "staff-1", "operator"))
    workflow.syncPendingTasks()
    assertEquals(1, syncBodies.size)
    assertTrue(task.clientTaskId in syncBodies.first())
    assertTrue("\"staff_id\":\"staff-1\"" in syncBodies.first())
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
