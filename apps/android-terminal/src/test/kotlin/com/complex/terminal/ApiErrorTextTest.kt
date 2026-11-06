package com.complex.terminal

// 文件版本：V0.01

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 第十八批 W 段第 193 条 193-R1（PGM:287「可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。
 * 这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。」；决策台账 DEC-R3B-2026-08-04-12）：
 * 失败提示末尾括号只留状态码，接口路径不上屏（只记设备日志）。
 */
class ApiErrorTextTest {
  private fun shown(status: Int, body: String) = apiErrorText(ApiException(status, "/api/v1/bar/bills/bill_1/settle", body))
  private fun assertNoLetters(text: String) = assertFalse(text.any { it in 'A'..'Z' || it in 'a'..'z' }, text)

  @Test
  fun conflictShowsTheServerReasonAndOnlyTheStatusCode() {
    val text = shown(409, """{"code":"CONFLICT","message":"账单已结清，不能重复结账"}""")
    assertEquals("账单已结清，不能重复结账（409）", text)
    assertNoLetters(text)
  }

  @Test
  fun unauthorizedNotFoundAndServerErrorCarryNoInterfacePath() {
    listOf(
      shown(401, """{"code":"UNAUTHORIZED","message":"unauthorized"}""") to "请重新登录（401）",
      shown(404, """{"code":"NOT_FOUND","message":"not found"}""") to "数据不存在（404）",
      shown(500, "Internal Server Error") to "服务暂时不可用，请稍后重试（500）",
    ).forEach { (text, expected) ->
      assertEquals(expected, text)
      assertNoLetters(text)
    }
  }
}
