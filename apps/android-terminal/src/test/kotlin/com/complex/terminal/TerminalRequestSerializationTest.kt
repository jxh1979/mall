package com.complex.terminal

// 文件版本：V0.08

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalRequestSerializationTest {
  private val api = TerminalApiClient("http://127.0.0.1:1")

  @Test
  fun byodRequestsUseOpenApiSnakeCaseFields() {
    // BYOD 一期:注册/代订请求必须与 OpenAPI snake_case 契约一致(全局 SnakeCase 策略回归锚)。
    val reg = api.json.parseToJsonElement(
      api.json.encodeToString(
        StaffRegistrationSubmitRequest(
          idempotencyKey = "byod_reg_1",
          projectId = "project_1",
          name = "测试员工",
        ),
      ),
    ).jsonObject
    assertEquals("project_1", reg["project_id"]?.toString()?.trim('"'))
    assertEquals("byod_reg_1", reg["idempotency_key"]?.toString()?.trim('"'))

    val resv = api.json.parseToJsonElement(
      api.json.encodeToString(
        BarReservationCreateRequest(
          idempotencyKey = "byod_resv_1",
          barId = "bar_1",
          tableId = "tbl_1",
          reservationTime = "2026-07-02T20:00:00Z",
          guestCount = 4,
          salespersonUserId = "usr_staff_1",
          latestArrivalAt = "2026-07-02T22:00:00Z",
          customerPhone = "13800000000",
        ),
      ),
    ).jsonObject
    assertEquals("tbl_1", resv["table_id"]?.toString()?.trim('"'))
    assertEquals("2026-07-02T20:00:00Z", resv["reservation_time"]?.toString()?.trim('"'))
    assertEquals("usr_staff_1", resv["salesperson_user_id"]?.toString()?.trim('"'))
    assertEquals("2026-07-02T22:00:00Z", resv["latest_arrival_at"]?.toString()?.trim('"'))
    assertEquals("13800000000", resv["customer_phone"]?.toString()?.trim('"'))
  }

  @Test
  fun serviceMarkPaidCarriesOptionalMemberPhone() {
    // 第十一批 TK-04-R2（BSM L822「由收银在收款时现场录入会员号累计」）：确认收款可带会员号（注册手机号）member_phone；不填不带该键。
    val req = api.json.decodeFromString<ServiceMarkPaidReq>("""{"idempotency_key":"mp_1","member_phone":"13900001111"}""")
    val body = api.json.parseToJsonElement(api.json.encodeToString(req)).jsonObject
    assertEquals("13900001111", body["member_phone"]?.toString()?.trim('"'))
    val none = api.json.parseToJsonElement(api.json.encodeToString(ServiceMarkPaidReq("mp_2"))).jsonObject
    assertFalse(none.containsKey("member_phone"))
  }

  @Test
  fun bindDeviceRequestUsesOpenApiSnakeCaseFields() {
    val body = api.json.parseToJsonElement(
      api.json.encodeToString(
        BindDeviceRequest(
          idempotencyKey = "android_bind_P0_ANDROID_1",
          deviceId = "P0-ANDROID-1",
          terminalType = "staff_work_phone",
          bindingScope = "space",
          boundSpaceId = "point_1",
          mdmDeviceRef = "mdm_1",
          kioskMode = null,
        ),
      ),
    ).jsonObject

    assertEquals("P0-ANDROID-1", body["device_id"]?.toString()?.trim('"'))
    assertEquals("staff_work_phone", body["terminal_type"]?.toString()?.trim('"'))
    assertEquals("space", body["binding_scope"]?.toString()?.trim('"'))
    assertEquals("point_1", body["bound_space_id"]?.toString()?.trim('"'))
    assertTrue("idempotency_key" in body)
    assertFalse("deviceCode" in body)
    assertFalse("device_code" in body)
    assertFalse("deviceType" in body)
  }

  @Test
  fun bindDeviceRequestCarriesDeviceModel() {
    // 第 96 条 96-R1（PGM:623「建设备台账，记录编号、型号、点位、责任人和状态。」）：领用工作手机、平板时带本机型号 device_model；
    // 不给型号时不带该键（explicitNulls=false），后端沿用台账原值。
    val req = api.json.decodeFromString<BindDeviceRequest>("""{"device_id":"P0-1","device_model":"Pixel 8"}""")
    val body = api.json.parseToJsonElement(api.json.encodeToString(req)).jsonObject
    assertEquals("Pixel 8", body["device_model"]?.toString()?.trim('"'))
    val none = api.json.parseToJsonElement(api.json.encodeToString(BindDeviceRequest(deviceId = "P0-1"))).jsonObject
    assertFalse(none.containsKey("device_model"))
  }

  @Test
  fun eventBatchRequestIncludesIdempotencyKeyAndOpenApiEventFields() {
    val body = api.json.parseToJsonElement(
      api.json.encodeToString(
        EventBatchRequest(
          idempotencyKey = "android_events_P0_ANDROID_1",
          deviceCode = "P0-ANDROID-1",
          staffId = "staff_1",
          events = listOf(
            TerminalEvent(
              eventType = "terminal.login",
              eventCategory = "android_terminal",
              result = "success",
              summary = "login",
              payload = mapOf("mode" to "online"),
              occurredAt = "2026-06-14T00:00:00Z",
            ),
          ),
        ),
      ),
    ).jsonObject
    val event = body["events"]!!.jsonArray.first().jsonObject

    assertTrue("idempotency_key" in body)
    assertTrue("device_code" in body)
    assertTrue("staff_id" in body)
    assertTrue("event_type" in event)
    assertTrue("event_category" in event)
    assertTrue("occurred_at" in event)
    assertFalse("eventType" in event)
  }

  @Test
  fun amusementMaintenanceRequestsUseSnakeCaseAndCorrectFieldNames() {
    val submit = api.json.parseToJsonElement(
      api.json.encodeToString(
        AmusementMaintenanceRecordRequest(
          idempotencyKey = "android_amusement_maintenance_1",
          projectId = "proj_1",
          deviceId = "device_1",
          locationPointId = "point_1",
          maintenanceVendorId = "sup_1",
          engineerId = "staff_1",
          engineerName = "李工",
          maintenanceAt = "2026-06-14T00:00:00Z",
          result = "pass",
          exceptionNote = null,
          sourceTerminal = "kiosk",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in submit)
    assertTrue("project_id" in submit)
    assertTrue("device_id" in submit)
    assertTrue("location_point_id" in submit)
    assertTrue("engineer_id" in submit)
    assertTrue("maintenance_at" in submit)
    assertTrue("source_terminal" in submit)
    assertTrue("maintenance_vendor_id" in submit)
    assertFalse("projectId" in submit)
    // explicitNulls=false 应省略 exception_note
    assertFalse("exception_note" in submit)

    val resume = api.json.parseToJsonElement(
      api.json.encodeToString(
        AmusementMaintenanceResumeRequest(
          idempotencyKey = "android_amusement_resume_1",
          remark = "确认恢复",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in resume)
    // 120-R6：确认人由服务端取当前登录主管，确认体不带确认人。
    assertFalse("supervisor_confirmed_by" in resume)
    // 后端确认恢复使用 remark，而非 operator_note
    assertTrue("remark" in resume)
    assertFalse("operator_note" in resume)
  }

  @Test
  fun barOrderAndSettlementRequestsUseSnakeCaseFields() {
    val order = api.json.parseToJsonElement(
      api.json.encodeToString(
        BarOrderRequest(
          idempotencyKey = "android_bar_order_1",
          barId = "bar_1",
          tableId = "table_1",
          items = listOf(mapOf("sku" to "beer", "qty" to "2")),
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in order)
    // 第十八批 AF 段第 198 条 198-R5：服务端不读下单的 operator_note、evidence_attachment_ids、service_user_ids，终端不再发。
    assertFalse("operator_note" in order)
    assertFalse("evidence_attachment_ids" in order)
    assertTrue("bar_id" in order)
    assertTrue("table_id" in order)
    // 第十八批 I 段 86-R1：订单销售由服务端取开台会话的销售，点单请求不含 salesperson_user_id。
    assertTrue("salesperson_user_id" !in order)
    assertFalse("service_user_ids" in order)
    assertFalse("payment_mode" in order)
    assertEquals(2, order["items"]!!.jsonArray.size.let { 2 })

    val settle = api.json.parseToJsonElement(
      api.json.encodeToString(
        BarBillSettleRequest(
          idempotencyKey = "android_bar_settle_1",
          paymentId = "pay_1",
          settlementAmountCent = 9900,
          paymentChannel = "wechat",
          posTransactionNo = "pos_1",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in settle)
    assertTrue("payment_id" in settle)
    assertTrue("settlement_amount_cent" in settle)
    assertTrue("payment_channel" in settle)
    // 第十八批 Q 段第 153 条 153-R2（BSM:940「……一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕」）：收款人由服务端按本员工会话定，结账体不发 cashier_staff_id。
    assertFalse("cashier_staff_id" in settle)
    assertTrue("pos_transaction_no" in settle)
    // 198-R5：服务端不读结账的 operator_note、evidence_attachment_ids，结账体不再发。
    assertFalse("operator_note" in settle)
    assertFalse("evidence_attachment_ids" in settle)

    val intent = api.json.parseToJsonElement(
      api.json.encodeToString(
        PaymentIntentRequest(
          idempotencyKey = "android_pay_intent_1",
          businessOrderType = "bar_bill",
          businessOrderId = "bill_1",
          amountCent = 9900,
          currency = "CNY",
          channel = "wechat",
        ),
      ),
    ).jsonObject
    assertTrue("business_order_type" in intent)
    assertTrue("business_order_id" in intent)
    assertTrue("amount_cent" in intent)
  }

  @Test
  fun staffDeviceCredentialRequestsUseSnakeCaseFields() {
    val bind = api.json.parseToJsonElement(
      api.json.encodeToString(
        StaffDeviceBindRequest(
          idempotencyKey = "android_staff_device_bind_1",
          staffProfileId = "profile_1",
          devicePublicKey = "pubkey",
          deviceFingerprintHash = "hash_1",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in bind)
    assertTrue("staff_profile_id" in bind)
    assertTrue("device_public_key" in bind)
    assertTrue("device_fingerprint_hash" in bind)
    assertFalse("staffProfileId" in bind)

    val integrity = api.json.parseToJsonElement(
      api.json.encodeToString(
        StaffDeviceIntegrityReportRequest(
          idempotencyKey = "android_staff_device_integrity_1",
          integrityStatus = "compromised",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in integrity)
    // 198-R5：服务端不读完整性上报的 operator_note，不再发。
    assertFalse("operator_note" in integrity)
    assertTrue("integrity_status" in integrity)
  }

  @Test
  fun verificationReverseRequestUsesReasonAndIdempotencyFields() {
    val body = api.json.parseToJsonElement(
      api.json.encodeToString(
        VerificationReverseRequest(
          idempotencyKey = "android_verification_reverse_1",
          reason = "误核销",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in body)
    // 冲正使用 reason 字段，而非 operator_note
    assertTrue("reason" in body)
    assertFalse("operator_note" in body)
  }

  @Test
  fun catalogImagePathUsesImageReadEndpoint() {
    // 18P 136-R2：平板点单 POS 与一体机吧台买酒按 image_url 回读商品图，走 GET /api/v1/bar/catalog/images/{name}。
    assertEquals("/api/v1/bar/catalog/images/abc.jpg", barCatalogImagePath("bar-images/abc.jpg"))
  }

  @Test
  fun returnDeviceRequestUsesOpenApiPendingTaskAndIdempotencyFields() {
    val body = api.json.parseToJsonElement(
      api.json.encodeToString(
        DeviceReturnRequest(
          idempotencyKey = "android_return_P0_ANDROID_1",
          deviceCode = "P0-ANDROID-1",
          staffId = "staff_1",
          hasPendingTasks = true,
          reason = "pending offline tasks",
        ),
      ),
    ).jsonObject

    assertTrue("idempotency_key" in body)
    assertTrue("device_code" in body)
    assertTrue("has_pending_tasks" in body)
    assertFalse("deviceCode" in body)
    assertFalse("hasPendingTasks" in body)
  }

  @Test
  fun appScreenRequestsNoLongerCarryKeysTheServerDoesNotRead() {
    // 第十八批 AF 段第 198 条 198-R5：员工 App 的商户代提交员工注册、工作手机归还与故障换机（android-app MainActivity），
    // 不再发服务端不读的 operator_note、evidence_attachment_ids；模型已无这些分量，带着这些键解码再编码也发不出去。
    val reg = api.json.parseToJsonElement(api.json.encodeToString(api.json.decodeFromString<StaffRegistrationSubmitRequest>(
      """{"idempotency_key":"reg_1","project_id":"project_1","name":"测试员工","operator_note":"商户手机代提交","merchant_id":"merchant_1"}"""))).jsonObject
    assertFalse("operator_note" in reg)
    assertEquals("merchant_1", reg["merchant_id"]?.toString()?.trim('"'))

    val ret = api.json.parseToJsonElement(api.json.encodeToString(api.json.decodeFromString<DeviceReturnRequest>(
      """{"idempotency_key":"ret_1","device_code":"P0-ANDROID-1","has_pending_tasks":false,"reason":"屏幕碎裂","operator_note":"屏幕碎裂","evidence_attachment_ids":[]}"""))).jsonObject
    assertFalse("operator_note" in ret)
    assertFalse("evidence_attachment_ids" in ret)
    // 交接说明经 reason 读取落库，照发。
    assertEquals("屏幕碎裂", ret["reason"]?.toString()?.trim('"'))

    val swap = api.json.parseToJsonElement(
      api.json.encodeToString(WorkPhoneSwapRequest("swap_1", "P0-ANDROID-1", "device_2", "屏幕碎裂")),
    ).jsonObject
    assertFalse("evidence_attachment_ids" in swap)
    assertEquals("device_2", swap["replacement_device_id"]?.toString()?.trim('"'))
  }
}
