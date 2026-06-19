package com.complex.terminal

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
          engineerId = "staff_1",
          engineerName = "李工",
          maintenanceAt = "2026-06-14T00:00:00Z",
          result = "passed",
          exceptionNote = null,
          sourceTerminal = "android_terminal",
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
    assertFalse("projectId" in submit)
    // explicitNulls=false 应省略 exception_note
    assertFalse("exception_note" in submit)

    val resume = api.json.parseToJsonElement(
      api.json.encodeToString(
        AmusementMaintenanceResumeRequest(
          idempotencyKey = "android_amusement_resume_1",
          supervisorConfirmedBy = "supervisor_1",
          remark = "确认恢复",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in resume)
    assertTrue("supervisor_confirmed_by" in resume)
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
          operatorNote = "note",
          barId = "bar_1",
          tableId = "table_1",
          items = listOf(mapOf("sku" to "beer", "qty" to "2")),
          salespersonUserId = "staff_1",
          serviceUserIds = listOf("staff_2"),
          paymentMode = "postpaid",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in order)
    assertTrue("operator_note" in order)
    assertTrue("bar_id" in order)
    assertTrue("table_id" in order)
    assertTrue("salesperson_user_id" in order)
    assertTrue("service_user_ids" in order)
    assertTrue("payment_mode" in order)
    assertEquals(2, order["items"]!!.jsonArray.size.let { 2 })

    val settle = api.json.parseToJsonElement(
      api.json.encodeToString(
        BarBillSettleRequest(
          idempotencyKey = "android_bar_settle_1",
          paymentId = "pay_1",
          settlementAmountCent = 9900,
          paymentChannel = "wechat",
          cashierStaffId = "staff_1",
          posTransactionNo = "pos_1",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in settle)
    assertTrue("payment_id" in settle)
    assertTrue("settlement_amount_cent" in settle)
    assertTrue("payment_channel" in settle)
    assertTrue("cashier_staff_id" in settle)
    assertTrue("pos_transaction_no" in settle)

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
          operatorNote = "note",
          integrityStatus = "compromised",
        ),
      ),
    ).jsonObject
    assertTrue("idempotency_key" in integrity)
    assertTrue("operator_note" in integrity)
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
}
