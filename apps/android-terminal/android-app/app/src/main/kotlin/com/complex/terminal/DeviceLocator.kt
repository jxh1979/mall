package com.complex.terminal

// 文件版本：V0.01

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 第十八批 N 段第 123 条 123-R4（BSM:1630「工作手机、平板、一体机绑定点位后，系统自动知道“在哪”……绑定点位的工作手机、平板、一体机
 * 离开授权范围后同样只保留查看，不得执行上述操作。」）：现场动作提交那一刻取一次本机定位，用系统 LocationManager（不依赖 GMS）。
 * 先卫星（GPS）后网络定位，每种最多等 10 秒；没给定位权限、定位关着或取不到返回 null——请求照常发出，所属项目已配授权范围时
 * 由服务端按范围外拒绝（第十八批裁决 30）。在请求所在的后台线程阻塞调用；不轮询、不存位置（平台治理 PRD §15.3）。
 */
class DeviceLocator(private val context: Context) {

  fun currentFix(): Pair<Double, Double>? {
    val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
      .any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    if (!granted) return null
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
      if (!manager.isProviderEnabled(provider)) continue
      fix(manager, provider)?.let { return it.latitude to it.longitude }
    }
    return null
  }

  @Suppress("MissingPermission")
  private fun fix(manager: LocationManager, provider: String): Location? {
    val latch = CountDownLatch(1)
    var result: Location? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val cancel = CancellationSignal()
      manager.getCurrentLocation(provider, cancel, context.mainExecutor) { result = it; latch.countDown() }
      if (!latch.await(10, TimeUnit.SECONDS)) cancel.cancel()
    } else {
      // API 26～29 平台的 LocationListener 其余三个方法没有默认实现，须逐个写上。
      val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) { result = location; latch.countDown() }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("API 29 起弃用，API 26～29 平台仍会回调")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
      }
      @Suppress("DEPRECATION")
      manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
      if (!latch.await(10, TimeUnit.SECONDS)) manager.removeUpdates(listener)
    }
    return result
  }
}
