package com.complex.terminal

// 文件版本：V0.01

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder

/**
 * U01-002（BSM L1445 取离充电设施本身也算领取）：拔线接收器由前台服务常驻持有，应用被回收或开机后由 [BootReceiver] 重新拉起，
 * 不依赖「请保持应用运行」文案。投递经 [ChargingReceiptBridge] 交给活动页记录；活动不在时记为待补记，下次登录后补记。
 */
class ChargingReceiptService : Service() {
  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) ChargingReceiptBridge.deliver(this@ChargingReceiptService)
    }
  }

  override fun onCreate() {
    super.onCreate()
    val manager = getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= 26) {
      manager.createNotificationChannel(NotificationChannel(CHANNEL, "工作手机考勤监听", NotificationManager.IMPORTANCE_MIN))
    }
    val notification: Notification = Notification.Builder(this, CHANNEL)
      .setContentTitle("工作手机考勤监听运行中")
      .setContentText("取离充电设施会自动记录上班领取")
      .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
      .build()
    startForeground(NOTIFICATION_ID, notification)
    val filter = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
    if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(receiver, filter)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

  override fun onDestroy() {
    runCatching { unregisterReceiver(receiver) }
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    private const val CHANNEL = "charging_receipt"
    private const val NOTIFICATION_ID = 4101
    fun start(context: Context) {
      val intent = Intent(context, ChargingReceiptService::class.java)
      if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
    }
  }
}

/** 拔线事件投递桥：活动在前台即时记录；否则落待补记标记（时刻以补记时为准，服务端仍取最早一次）。 */
object ChargingReceiptBridge {
  private const val PREFS = "charging_receipts"
  private const val PENDING_KEY = "pending_charging_disconnect_at"

  @Volatile
  var handler: (() -> Unit)? = null

  fun deliver(context: Context) {
    val current = handler
    if (current != null) {
      current()
    } else {
      context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(PENDING_KEY, System.currentTimeMillis()).apply()
    }
  }

  /** 取走待补记标记；返回是否存在。 */
  fun consumePending(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val pending = prefs.getLong(PENDING_KEY, 0L) > 0L
    if (pending) prefs.edit().remove(PENDING_KEY).apply()
    return pending
  }
}

/** 开机后重新拉起常驻监听服务（U01-002）。 */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) ChargingReceiptService.start(context)
  }
}
