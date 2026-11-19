package com.complex.terminal

// 文件版本：V0.01

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 第十八批 BB 段第 266 条 266-R2（PGM:586「检测到手机被破解、篡改或处于异常环境时，私人手机 App 失效并复核。」）：
 * 本机特征 → 上报的完整性取值（与后端 trusted/root_detected/debug_detected/abnormal 闭集一致）。
 */
class DeviceIntegrityVerdictTest {
  private fun verdict(
    su: Boolean = false,
    debugger: Boolean = false,
    testKeys: Boolean = false,
    emulator: Boolean = false,
    hook: Boolean = false,
  ) = DeviceIntegrityVerdict.of(su, debugger, testKeys, emulator, hook)

  @Test
  fun localSignalsMapToTheBackendIntegrityStatuses() {
    assertEquals("root_detected", verdict(su = true))
    assertEquals("debug_detected", verdict(debugger = true))
    assertEquals("abnormal", verdict(testKeys = true))
    assertEquals("abnormal", verdict(emulator = true))
    assertEquals("abnormal", verdict(hook = true))
    assertEquals("trusted", verdict())
    assertEquals("root_detected", verdict(su = true, emulator = true))
  }
}
