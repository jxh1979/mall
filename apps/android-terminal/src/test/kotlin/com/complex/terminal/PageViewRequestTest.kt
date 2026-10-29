package com.complex.terminal

// 文件版本：V0.01

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 第 23 条 23-R6：员工手机在线屏打开留痕请求体与后台同一接口同一蛇形键；暗纹只挂员工工作手机、员工私人手机（PGM:687、PGM:745）。 */
class PageViewRequestTest {
  private val api = TerminalApiClient("http://127.0.0.1:1")

  @Test
  fun pageViewRequestUsesOpenApiSnakeCaseKeys() {
    val body = api.json.parseToJsonElement(
      api.json.encodeToString(
        PageViewRequest("酒吧空位 / 帮客订位", true, listOf(PageObjectRef("bar_reservation", "resv_1")), "pv_1"),
      ),
    ).jsonObject

    assertEquals(setOf("page_id", "sensitive", "object_refs", "idempotency_key"), body.keys)
    assertEquals(setOf("object_type", "object_id"), body.getValue("object_refs").jsonArray[0].jsonObject.keys)
    assertEquals("\"酒吧空位 / 帮客订位\"", body.getValue("page_id").toString())
  }

  @Test
  fun watermarkOnlyOnStaffPhones() {
    assertTrue(watermarkApplies("staff_work_phone"))
    assertTrue(watermarkApplies("staff_private_phone"))
    assertFalse(watermarkApplies("tablet"))
    assertFalse(watermarkApplies("kiosk"))
    assertFalse(watermarkApplies(null))
  }
}
