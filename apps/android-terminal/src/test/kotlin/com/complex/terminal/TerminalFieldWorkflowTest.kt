package com.complex.terminal

// 文件版本：V0.12

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
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

  // 第十八批 BS 段第 373 条 373-R5（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过」；第十八批裁决 76「巡检员用工作手机按顺序
  // 扫码后记结果，扫错码或跳点都记不上」）：记点位走 POST /api/v1/amusement/projects/{id}/inspection-points/{seq}/check，蛇形键带扫到的点位码、结果
  // 与请求编号，返回本轮走点；服务端拒绝（跳点、扫错码）的中文文案原样给到屏上。
  @Test
  fun amusementInspectionPointCheckSendsScannedCodeAndSurfacesRefusal() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val api = newApi { req ->
      captured += req
      if (req.body.contains("\"point_code\":\"WRONGCODE000\"")) {
        StubResponse(409, """{"code":"STATE_CONFLICT","detail_code":"INSPECTION_POINT_CODE_MISMATCH","message":"扫到的不是第 2 个点位（安全压杠）的码"}""")
      } else {
        okJson("""{"project_id":"amu-1","points":[{"seq":1,"point_name":"入口闸机","point_code":"AAAABBBBCCCC"},{"seq":2,"point_name":"安全压杠","point_code":"DDDDEEEEFFFF"}],"walk":[{"seq":1,"point_name":"入口闸机","result":"pass","checked_by":"staff-1","checked_at":"2026-11-20T00:10:00Z"}]}""")
      }
    }
    val workflow = TerminalWorkflow(api, storeWithSessionAndBinding())

    val view = workflow.checkAmusementInspectionPoint("amu-1", 1, "AAAABBBBCCCC", "pass", "护栏完好")

    val request = captured.single()
    assertEquals("/api/v1/amusement/projects/amu-1/inspection-points/1/check", request.path)
    assertTrue("\"point_code\":\"AAAABBBBCCCC\"" in request.body, request.body)
    assertTrue("\"result\":\"pass\"" in request.body, request.body)
    assertTrue("\"note\":\"护栏完好\"" in request.body, request.body)
    assertTrue("\"idempotency_key\"" in request.body, request.body)
    assertEquals(listOf(1), view.walk.map { it.seq })
    assertEquals("安全压杠", view.points[1].pointName)

    val error = assertFailsWith<ApiException> { workflow.checkAmusementInspectionPoint("amu-1", 2, "WRONGCODE000", "pass") }
    assertEquals(409, error.statusCode)
    assertTrue("扫到的不是第 2 个点位（安全压杠）的码" in apiErrorText(error), apiErrorText(error))
  }

  @Test
  fun barOrderPaymentIntentAndSettlement() {
    val paths = mutableListOf<String>()
    val bodies = mutableMapOf<String, String>()
    val api = newApi { req ->
      paths += req.path
      bodies[req.path] = req.body
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
    // 第十八批 AF 段第 198 条 198-R5：服务端不读下单的 operator_note、evidence_attachment_ids、service_user_ids 与结账的 operator_note、evidence_attachment_ids，终端不再发。
    val orderBody = bodies["/api/v1/bar/orders"].orEmpty()
    listOf("operator_note", "evidence_attachment_ids", "service_user_ids").forEach { assertFalse(it in orderBody, it) }
    val settleBody = bodies["/api/v1/bar/bills/bill-1/settle"].orEmpty()
    listOf("operator_note", "evidence_attachment_ids").forEach { assertFalse(it in settleBody, it) }
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
    val bodies = mutableMapOf<String, String>()
    val api = newApi { req ->
      paths += req.path
      bodies[req.path] = req.body
      when (req.path) {
        "/api/v1/staff/device-credentials" -> {
          assertTrue("\"device_fingerprint_hash\":\"hash-new\"" in req.body)
          okJson("""{"id":"cred-1","bind_status":"pending_rebind_approval"}""")
        }
        "/api/v1/staff/device-credentials/cred-1/approve-rebind" -> {
          assertTrue("\"idempotency_key\"" in req.body)
          okJson("""{"id":"cred-1","bind_status":"active"}""")
        }
        // 第十八批 BB 段 266-R2：App 拿不到凭证编号，按登录人上报本机完整性。
        "/api/v1/staff/device-credentials/report-integrity" -> {
          assertTrue("\"idempotency_key\"" in req.body)
          assertTrue("\"integrity_status\":\"abnormal\"" in req.body)
          okJson("""{"id":"cred-1","bind_status":"revoked","integrity_status":"abnormal"}""")
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

    val reported = workflow.reportStaffDeviceIntegrity(integrityStatus = "abnormal")
    assertEquals("abnormal", reported.integrityStatus)
    assertEquals(1, store.pendingEvents().count { it.eventType == "terminal.staff_device.integrity_reported" })
    assertEquals(
      listOf(
        "/api/v1/staff/device-credentials",
        "/api/v1/staff/device-credentials/cred-1/approve-rebind",
        "/api/v1/staff/device-credentials/report-integrity",
      ),
      paths,
    )
    // 第十八批 AF 段第 198 条 198-R5：绑定设备凭证、批准换机、完整性上报，服务端都不读 operator_note，终端不再发。
    assertEquals(3, bodies.size)
    bodies.forEach { (path, body) -> assertFalse("operator_note" in body, path) }
  }

  /**
   * 第十八批 BB 段收尾复核（266-R2 补全；PGM:586「检测到手机被破解、篡改或处于异常环境时，私人手机 App 失效并复核。」）：未声明身份的手机在
   * 「员工设备凭证」首次绑定（无历史凭证，直接生效）后当场采用私人手机身份，本机检测不可信即以私人手机身份签名上报，服务端吊销本人凭证。
   * 采用身份、本机采集与退回登录页在 MainActivity.showStaffDevice（Activity 层，android-app 没有 JVM 单测），这里核判定与上报这一层。
   */
  @Test
  fun firstBindThenUntrustedPhoneReportsIntegrityAsPrivatePhone() {
    val seen = mutableListOf<StubHttpClient.Captured>()
    val api = newApi { req ->
      seen += req
      when (req.path) {
        "/api/v1/staff/device-credentials" -> okJson("""{"id":"cred-1","bind_status":"active"}""")
        "/api/v1/staff/device-credentials/report-integrity" ->
          okJson("""{"id":"cred-1","bind_status":"revoked","integrity_status":"root_detected"}""")
        else -> error("unexpected path ${req.path}")
      }
    }
    api.deviceSigner = { "sig" }
    val workflow = TerminalWorkflow(api, storeWithSessionAndBinding())

    val bound = workflow.bindStaffDeviceCredential(staffProfileId = "profile-1", devicePublicKey = "pub", deviceFingerprintHash = "fp-1")
    assertEquals("active", bound.bindStatus)
    // MainActivity.showStaffDevice 绑定成功分支：本机未声明身份时以凭证指纹作设备编号、采用私人手机身份。
    api.deviceId = "fp-1"
    api.terminalType = STAFF_PRIVATE_PHONE
    val integrity = DeviceIntegrityVerdict.of(suPresent = true, debuggerAttached = false, testKeys = false, emulator = false, hookFramework = false)
    val reported = workflow.reportStaffDeviceIntegrity(integrity)

    assertEquals("revoked", reported.bindStatus)
    val report = seen.single { it.path == "/api/v1/staff/device-credentials/report-integrity" }
    assertTrue("\"integrity_status\":\"root_detected\"" in report.body)
    assertEquals(listOf(STAFF_PRIVATE_PHONE), report.headers["X-Terminal-Type"])
    assertEquals(listOf("fp-1"), report.headers["X-Device-Id"])
    assertEquals(listOf("sig"), report.headers["X-Device-Signature"])
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

  // 第十八批 W 段第 192 条 192-R1（PGM:287「……日期时间等，必须全部为中文，不得出现任何英文（如……mm/dd/yyyy 英文日期格式……）」）：
  // 后端 ISO 瞬时按本机时区显示为「年-月-日 时:分」，空值显示「-」。
  @Test
  fun localTimeZhShowsIsoInstantAsLocalDateAndClock() {
    val shanghai = java.time.ZoneId.of("Asia/Shanghai")
    assertEquals("2026-11-05 20:30", localTimeZh("2026-11-05T12:30:00Z", shanghai))
    assertEquals("-", localTimeZh(null, shanghai))
  }

  // 192-R1：工资单成功付款时间按本机时区显示，不出现 ISO 的 T、Z。
  @Test
  fun payrollDetailShowsPaidAtAsLocalTime() {
    val saved = java.util.TimeZone.getDefault()
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"))
    try {
      val detail = PayrollStatementDetail(
        statement = PayrollStatementRow(id = "pay-192", month = "2026-10", totalCent = 480000),
        status = "paid", statementKind = "monthly", schemeVersionId = "scheme-1", factsId = "facts-1",
        paymentDueDate = "2026-11-05", arApItemId = "ap-1", paidAmountCent = 480000,
        lines = emptyList(), worktime = null, corrections = emptyList(),
        payments = listOf(PayrollPayment("p-1", "finance-1", 480000, "bank_transfer", "bank-1", "2026-11-05T12:30:00Z")),
        pendingItems = emptyList(),
      )
      val text = payrollDetailText(detail)
      assertTrue(text.contains("2026-11-05 20:30"), text)
      assertFalse(text.contains("2026-11-05T") || text.contains(":00Z"), text)
    } finally {
      java.util.TimeZone.setDefault(saved)
    }
  }

  // 192-R2（PGM:287「……状态、枚举取值……必须全部为中文……」；ENUM:41「已退款、已作废」）：员工端各枚举闭集全部取值都有中文、不含英文。
  @Test
  fun terminalEnumLabelsCoverClosedSetsWithoutEnglish() {
    val latin = Regex("[A-Za-z]")
    fun allZh(values: List<String>, zh: (String?) -> String) = values.forEach { v ->
      val text = zh(v)
      assertFalse(latin.containsMatchIn(text), "$v -> $text")
    }
    allZh(listOf("pending_assign", "assigned", "processing", "pending_review", "pending_acceptance", "accepted", "closed", "suspended", "overtime"),
      ::facilityWorkOrderStatusZh)
    allZh(listOf("urgent", "high", "normal", "low", "medium", "critical"), ::workPriorityZh)
    allZh(listOf("active", "refunded", "forfeited"), ::entitlementStatusZh)
    allZh(listOf("personal", "sick", "comp", "annual"), ::leaveTypeZh)
    allZh(listOf("created", "paid", "pending_accept", "accepted", "completed", "cancelled"), ::serviceOrderStatusZh)
    allZh(listOf("wechat", "alipay"), ::payChannelZh)
    assertEquals("已作废", entitlementStatusZh("forfeited"))
    assertEquals("已退款", entitlementStatusZh("refunded"))
    assertEquals("游乐", entitlementTypeZh("amusement"))
    assertEquals("酒吧", entitlementTypeZh("bar"))
    assertEquals("支付宝", payChannelZh("alipay"))
  }

  // 192-R3（PGM:287）：日期＋时刻两栏按本机时区转 ISO 瞬时，格式不对为空。
  @Test
  fun localInstantOfJoinsDateAndClockInLocalZone() {
    val shanghai = java.time.ZoneId.of("Asia/Shanghai")
    assertEquals("2026-11-05T12:30:00Z", localInstantOf("2026-11-05", "20:30", shanghai))
    assertEquals("2026-11-05T12:30:00Z", localInstantOf(" 2026-11-05 ", " 20:30 ", shanghai))
    assertNull(localInstantOf("2026/11/05", "20:30", shanghai))
    assertNull(localInstantOf("2026-11-05", "", shanghai))
  }

  // ── 第十八批 BO 段第 261 条 261-B06-R1（PGM:303「同一个操作被重复提交，系统只认一次，不会因为重试就重复扣款、重复入账或重复核销……
  //    同一次操作重复提交时，返回第一次的结果并提示已完成。」；PGM:528「⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）」）：
  //    在线核销（门票与游乐入口共用一槽）与点酒下单按「一次操作」沿用请求编号，重复提交读出「已完成」提示 ──

  private fun envelope(data: String, replayed: Boolean = false): HttpResponse<String> = okJson(
    if (replayed) """{"success":true,"code":"OK","message":"该操作已完成（重复提交，返回首次结果）","data":$data,"replayed":true}"""
    else """{"success":true,"code":"OK","message":"success","data":$data}""",
  )

  private fun verified(no: String, replayed: Boolean = false) =
    envelope("""{"id":"ver-$no","verification_no":"$no","status":"success"}""", replayed)

  /** 按次序回应每个请求：空＝没收到响应（读超时）。 */
  private fun replying(captured: MutableList<StubHttpClient.Captured>, vararg replies: HttpResponse<String>?): StubHttpClient.Handler {
    val queue = ArrayDeque(replies.toList())
    return StubHttpClient.Handler { req ->
      synchronized(queue) {
        captured += req
        queue.removeFirst()
      } ?: throw HttpTimeoutException("读超时")
    }
  }

  private fun key(body: String): String =
    Regex("\"idempotency_key\":\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: error("缺 idempotency_key：$body")

  @Test
  fun verifyOnlineWithoutAResultResendsTheSameRequestAndRenewsTheKeyAfterAResult() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val workflow = TerminalWorkflow(newApi(replying(captured, null, verified("V-1"), verified("V-2"))), storeWithSessionAndBinding())

    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-RETRY-1") }
    workflow.verifyOnline("E-RETRY-1")
    workflow.verifyOnline("E-RETRY-1")

    assertEquals(captured[0].body, captured[1].body, "没拿到结果再核同一个码，沿用原编号、请求体逐字节相同")
    assertTrue(key(captured[2].body) != key(captured[1].body), "拿到结果后再扫，换新编号")
  }

  @Test
  fun verifyOnlineReplayCarriesTheCompletedNotice() {
    val workflow = TerminalWorkflow(newApi(replying(mutableListOf(), verified("V-1"), verified("V-1", replayed = true))), storeWithSessionAndBinding())

    workflow.verifyOnline("E-1")
    assertNull(workflow.operationKeys.completedNotice, "首次执行不提示已完成")
    workflow.verifyOnline("E-1")
    assertEquals("该操作已完成（重复提交，返回首次结果）", workflow.operationKeys.completedNotice)
  }

  @Test
  fun scanningAnotherCodeFirstResendsTheEarlierVerificationAndReturnsItWhenItSucceeded() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val workflow = TerminalWorkflow(newApi(replying(captured, null, verified("V-A"), verified("V-B"))), storeWithSessionAndBinding())
    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-A", faceCapture = "RkFDRQ==", faceConsentAccepted = true) }

    val response = workflow.verifyOnline("E-B")

    assertEquals(2, captured.size, "本次扫到的码不提交：${captured.map { it.body }}")
    assertEquals(captured[0].body, captured[1].body, "先用原编号原样重发上一笔（含人脸）")
    assertEquals("V-A", response.verificationNo)
    assertEquals(PREVIOUS_VERIFICATION_NOTICE, workflow.operationKeys.completedNotice)

    val again = workflow.verifyOnline("E-B")

    assertEquals(3, captured.size, captured.map { it.body }.toString())
    assertTrue("\"credential_payload\":\"E-B\"" in captured[2].body, "上一笔已确认即清槽，乙再扫一次提交的是乙的码：${captured[2].body}")
    assertTrue(key(captured[2].body) != key(captured[0].body), "乙的码用新编号")
    assertEquals("V-B", again.verificationNo)
    assertNull(workflow.operationKeys.completedNotice)
  }

  @Test
  fun anEarlierVerificationThatFailedOrWasRejectedIsDroppedAndTheNewCodeGoesOutWithANewKey() {
    val windowExpired = envelope("""{"id":"ver-a","verification_no":"V-A","status":"failed","result_reason":"window_expired"}""")
    val rejected = StubResponse(409, """{"code":"CONFLICT","detail_code":"ENTITLEMENT_EXPIRED","message":"权益已过期"}""")
    listOf(windowExpired, rejected).forEach { earlierReply ->
      val captured = mutableListOf<StubHttpClient.Captured>()
      val workflow = TerminalWorkflow(newApi(replying(captured, null, earlierReply, verified("V-B"))), storeWithSessionAndBinding())
      assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-A") }

      val response = workflow.verifyOnline("E-B")

      assertEquals(3, captured.size, captured.map { it.body }.toString())
      assertEquals(captured[0].body, captured[1].body, "先原样重发上一笔")
      assertTrue("\"credential_payload\":\"E-B\"" in captured[2].body, captured[2].body)
      assertTrue(key(captured[2].body) != key(captured[0].body), "上一笔已有结论，本次用新编号")
      assertEquals("V-B", response.verificationNo)
      assertNull(workflow.operationKeys.completedNotice)
    }
  }

  @Test
  fun anEarlierVerificationStillWithoutAResultKeepsTheNewCodeFromBeingSent() {
    val gatewayTimeout = StubResponse(504, """{"code":"PROVIDER_UNAVAILABLE","message":"网关超时"}""")
    val inProgress = StubResponse(409, """{"code":"CONFLICT","detail_code":"IDEMPOTENCY_IN_PROGRESS","message":"请求正在处理中，请稍后刷新查看结果"}""")
    listOf(null, gatewayTimeout, inProgress).forEach { earlierReply ->
      val captured = mutableListOf<StubHttpClient.Captured>()
      val workflow = TerminalWorkflow(newApi(replying(captured, null, earlierReply)), storeWithSessionAndBinding())
      assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-A") }

      assertFails { workflow.verifyOnline("E-B") }

      assertEquals(2, captured.size, captured.map { it.body }.toString())
      assertEquals(captured[0].body, captured[1].body, "仍先原样重发上一笔")
      assertTrue(captured.none { "\"credential_payload\":\"E-B\"" in it.body }, "上一笔仍没有结果，本次的码不提交")
    }
  }

  @Test
  fun pressingAgainWhileTheFirstVerificationIsInFlightSendsTheSameKeyWithoutWaiting() {
    val captured = java.util.Collections.synchronizedList(mutableListOf<StubHttpClient.Captured>())
    val firstArrived = CountDownLatch(1)
    val release = CountDownLatch(1)
    val calls = AtomicInteger()
    val workflow = TerminalWorkflow(newApi { req ->
      captured += req
      if (calls.incrementAndGet() == 1) {
        firstArrived.countDown()
        release.await(5, TimeUnit.SECONDS)
      }
      verified("V-1")
    }, storeWithSessionAndBinding())
    val first = thread { workflow.verifyOnline("E-1") }
    assertTrue(firstArrived.await(5, TimeUnit.SECONDS))

    workflow.verifyOnline("E-1")
    val secondDidNotWait = release.count == 1L
    release.countDown()
    first.join(5_000)

    assertTrue(secondDidNotWait, "在途再点不等第一笔")
    assertEquals(2, captured.size)
    assertEquals(key(captured[0].body), key(captured[1].body), "在途再点带同一个编号")
  }

  @Test
  fun aKeyWithoutAResultIsGivenUpSixtySecondsAfterTheLastAttempt() {
    var clock = 0L
    val captured = mutableListOf<StubHttpClient.Captured>()
    val workflow = TerminalWorkflow(newApi(replying(captured, null, null, null, null)), storeWithSessionAndBinding(), TerminalOperationKeys { clock })

    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-1") }
    clock = 59_000
    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-1") }
    clock = 118_000
    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-1") }
    clock = 179_001
    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-1") }

    val keys = captured.map { key(it.body) }
    assertEquals(keys[0], keys[1], "失败后 59 秒同一个码再核，沿用原编号")
    assertEquals(keys[1], keys[2], "离最近一次失败 59 秒、离第一次发出 118 秒再核，仍沿用原编号（从最近一次算）：$keys")
    assertTrue(keys[3] != keys[2], "最近一次失败后 61 秒再核，当作放弃、换新编号：$keys")
  }

  @Test
  fun anotherStaffLoggingInDropsTheKeysWhileTheSameStaffLoggingInAgainKeepsThem() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val staffIds = ArrayDeque(listOf("staff-1", "staff-1", "staff-2"))
    val api = newApi { req ->
      captured += req
      if (req.path == "/api/v1/auth/login") {
        val id = staffIds.removeFirst()
        envelope("""{"access_token":"tok-$id","session_id":"ses-$id","user":{"id":"$id","username":"u-$id"}}""")
      } else {
        throw HttpTimeoutException("读超时")
      }
    }
    val workflow = TerminalWorkflow(api, storeWithSessionAndBinding())

    workflow.login("operator", "pw")
    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-1") }
    workflow.login("operator", "pw")
    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-1") }
    workflow.login("other", "pw")
    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-1") }

    val verifications = captured.filter { it.path == "/api/v1/ticketing/verifications" }.map { it.body }
    assertEquals(3, verifications.size)
    assertEquals(key(verifications[0]), key(verifications[1]), "同一位员工重新登录，沿用原编号")
    assertTrue("\"staff_id\":\"staff-2\"" in verifications[2], "换一位员工后不重发上一位的核销：${verifications[2]}")
    assertTrue(key(verifications[2]) != key(verifications[1]), "换一位员工登录，换新编号")
  }

  @Test
  fun amusementVerificationSharesTheOnlineVerificationSlot() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val workflow = TerminalWorkflow(newApi(replying(captured, null, verified("V-T"))), storeWithSessionAndBinding())
    assertFailsWith<java.io.IOException> { workflow.verifyOnline("E-T") }

    val response = workflow.amusementVerifyOnline("E-R")

    assertEquals(2, captured.size, captured.map { it.body }.toString())
    assertEquals(captured[0].body, captured[1].body, "游乐核销扫码前先原样重发没拿到结果的门票核销")
    assertEquals("V-T", response.verificationNo)
  }

  @Test
  fun barOrderKeyFollowsTheContentAndWhetherTheResultIsKnown() {
    val captured = mutableListOf<StubHttpClient.Captured>()
    val workflow = TerminalWorkflow(newApi(replying(captured,
      null,
      envelope("""{"id":"order-1","bar_id":"bar-1","status":"created"}"""),
      StubResponse(401, """{"code":"UNAUTHORIZED","message":"会话已失效"}"""),
      StubResponse(429, """{"code":"RATE_LIMITED","message":"请求过于频繁"}"""),
      StubResponse(400, """{"code":"VALIDATION_FAILED","detail_code":"VALIDATION_FAILED","message":"商品已下架"}"""),
      null,
      null,
    )), storeWithSessionAndBinding())
    val wine = listOf(mapOf("product_id" to "wine-1", "quantity" to "2"))
    fun press(items: List<Map<String, String>> = wine) = runCatching { workflow.createBarOrder("bar-1", items, tableId = "table-1") }

    repeat(6) { press() }
    press(listOf(mapOf("product_id" to "beer-1", "quantity" to "1")))

    val keys = captured.map { key(it.body) }
    assertEquals(7, keys.size)
    assertEquals(keys[0], keys[1], "没收到响应后同样内容再下单，沿用原编号")
    assertTrue(keys[2] != keys[1], "拿到结果后再下单，换新编号")
    assertEquals(keys[2], keys[3], "会话失效（401）后再下单，沿用原编号")
    assertEquals(keys[3], keys[4], "限速（429）后再下单，沿用原编号")
    assertTrue(keys[5] != keys[4], "被明确拒绝（400）后再下单，换新编号")
    assertTrue(keys[6] != keys[5], "购物车不同，换新编号")
  }

  private fun okJson(body: String): HttpResponse<String> = StubResponse(200, body)
}

/** 简易 HttpClient stub：把 send() 委托给 handler，记录 path/body。 */
private class StubHttpClient(
  private val handler: Handler,
) : HttpClient() {
  data class Captured(val path: String, val body: String, val method: String, val headers: Map<String, List<String>> = emptyMap())

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
    val captured = Captured(path = path, body = body, method = request.method(), headers = request.headers().map())
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
