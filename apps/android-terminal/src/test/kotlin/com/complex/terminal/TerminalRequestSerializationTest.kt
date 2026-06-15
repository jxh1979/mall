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
