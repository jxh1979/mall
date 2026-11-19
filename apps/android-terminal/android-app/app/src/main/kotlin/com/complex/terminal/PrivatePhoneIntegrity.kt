package com.complex.terminal

// 文件版本：V0.01

import android.os.Build
import android.os.Debug
import java.io.File

/**
 * 第十八批 BB 段第 266 条 266-R2（PGM:586「检测到手机被破解、篡改或处于异常环境时，私人手机 App 失效并复核。」；
 * BSM:938「……检测到手机被破解、篡改或处于异常环境时自动失效并进入复核。」）：私人手机登录时在本机按特征检测
 * （su 与 Magisk 痕迹、test-keys、调试器、模拟器、Xposed 与 Frida），判定见共享库 [DeviceIntegrityVerdict]。
 * 不接第三方设备证明服务，不查开发者选项与 USB 调试，不按构建类型豁免。
 * ponytail: 本机特征检测能被高手隐藏（隐藏 su、改系统属性、改挂钩名），能力上限在此；要更强得换硬件密钥证明或设备证明服务。
 */
object PrivatePhoneIntegrity {
  const val INVALIDATED_NOTICE = "本机被检测到已被破解、篡改或处于异常环境，私人手机 App 已失效；请联系主管复核后重新绑定。"

  private val SU_PATHS = listOf(
    "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su", "/data/local/xbin/su", "/data/local/bin/su",
    "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/adb/magisk",
  )

  fun status(): String {
    val fingerprint = Build.FINGERPRINT.orEmpty()
    return DeviceIntegrityVerdict.of(
      suPresent = SU_PATHS.any { runCatching { File(it).exists() }.getOrDefault(false) },
      debuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger(),
      testKeys = Build.TAGS.orEmpty().contains("test-keys"),
      emulator = fingerprint.startsWith("generic") || fingerprint.contains("emulator")
        || Build.HARDWARE.orEmpty() in setOf("goldfish", "ranchu") || Build.PRODUCT.orEmpty().contains("sdk"),
      hookFramework = runCatching { Class.forName("de.robv.android.xposed.XposedBridge"); true }.getOrDefault(false)
        || runCatching { File("/proc/self/maps").readText().let { "frida" in it || "XposedBridge" in it } }.getOrDefault(false),
    )
  }
}
