package com.complex.terminal

// 文件版本：V0.01

/**
 * 第十八批 BB 段第 266 条 266-R2（PGM:586「检测到手机被破解、篡改或处于异常环境时，私人手机 App 失效并复核。」；
 * BSM:2007「已拍板：私人手机被破解、篡改或处于异常环境判为不可信并失效复核」）：私人手机本机特征 → 上报的完整性取值，
 * 与后端 StaffService.reportIntegrity 的 trusted/root_detected/debug_detected/abnormal 闭集一致。特征采集在 android-app 的 PrivatePhoneIntegrity。
 */
object DeviceIntegrityVerdict {
  fun of(suPresent: Boolean, debuggerAttached: Boolean, testKeys: Boolean, emulator: Boolean, hookFramework: Boolean): String =
    when {
      suPresent -> "root_detected"
      debuggerAttached -> "debug_detected"
      testKeys || emulator || hookFramework -> "abnormal"
      else -> "trusted"
    }
}
