package com.complex.terminal

// 文件版本：V0.02

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

/** 无论是否登录/前台都先持久保存；handler 只通知上传，不拥有原始事件。 */
object ChargingReceiptBridge {
  private const val PREFS = "charging_receipts"
  private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
  private val processBoot = java.util.UUID.randomUUID().toString()
  private var persistedQueue: AttendanceReceiptQueue? = null
  @Volatile var handler: (() -> Unit)? = null

  fun bootId(context: Context): String {
    val count = runCatching { android.provider.Settings.Global.getInt(context.contentResolver, "boot_count", -1) }.getOrDefault(-1)
    return if (count >= 0) "boot-$count" else processBoot
  }

  @Synchronized
  private fun queue(context: Context): AttendanceReceiptQueue {
    persistedQueue?.let { return it }
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return AttendanceReceiptQueue({ prefs.getString("events", null) }, { value ->
      check(prefs.edit().putString("events", value).commit()) { "考勤事件未能保存到手机" }
    }).also { persistedQueue = it }
  }

  @Synchronized
  fun rememberAnchor(context: Context, value: AttendanceAnchorResponse) {
    rememberContext(context,value.context)
    check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putString("anchor", json.encodeToString(AttendanceAnchorResponse.serializer(), value)).commit())
  }

  @Synchronized
  fun rememberContext(context: Context, current: WorkPhoneAttendanceContext) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val previous = prefs.getString("anchor", null)?.let { json.decodeFromString(AttendanceAnchorResponse.serializer(), it) }
    val editor = prefs.edit().putString("current_context", json.encodeToString(WorkPhoneAttendanceContext.serializer(),current))
    if (matchingReceiptAnchor(previous,current) == null) editor.remove("anchor")
    check(editor.commit())
  }

  @Synchronized
  fun clearAnchor(context: Context) { check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
    .remove("anchor").remove("current_context").commit()) }

  fun lastSyncMessage(context: Context): String = context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
    .getString("last_sync_message","未同步的原事件保留在本机，可查询重试或交主管代补报待核")!!

  fun saveSyncMessage(context: Context,message: String) {
    check(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("last_sync_message",message).commit())
  }

  fun flush(context: Context, upload: (PendingAttendanceReceipt) -> AttendanceReceiptEvent): ReceiptUploadResult = queue(context).flush(upload)

  @Synchronized
  fun enqueue(context: Context, method: String, tag: String? = null): PendingAttendanceReceipt {
    val device = context.getSharedPreferences("work_phone", Context.MODE_PRIVATE).getString("device_id", null)
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val current = prefs.getString("current_context",null)?.let { json.decodeFromString(WorkPhoneAttendanceContext.serializer(),it) }
    val anchor = prefs.getString("anchor",null)?.let { json.decodeFromString(AttendanceAnchorResponse.serializer(),it) }
    return AttendanceReceiptCapture.capture(queue(context),device,current,anchor,bootId(context),
      android.os.SystemClock.elapsedRealtime(),java.time.Instant.now().toString(),method,tag)
  }

  @Synchronized fun pending(context: Context): List<PendingAttendanceReceipt> = queue(context).pending()
  @Synchronized fun acknowledge(context: Context, eventId: String) { queue(context).acknowledge(eventId) }

  fun deliver(context: Context) {
    // 未配置为工作手机的私人手机/平板不生成员工领取事件。
    if (context.getSharedPreferences("work_phone", Context.MODE_PRIVATE).getString("device_id", null) == null) return
    enqueue(context, "charging_disconnect")
    handler?.invoke()
  }
}

/** 开机后重新拉起常驻监听服务（U01-002）。 */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) ChargingReceiptService.start(context)
  }
}
