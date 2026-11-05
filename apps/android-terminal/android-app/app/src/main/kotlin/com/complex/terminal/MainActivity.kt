package com.complex.terminal

// 文件版本：V0.22

import android.app.Activity
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.concurrent.thread

/**
 * 综合体 Android 终端：现场操作 UI。
 *
 * 本 Activity 仅负责界面与线程编排；所有业务逻辑直接调用已实现的端侧业务层：
 *   - [TerminalApiClient]：HTTP 契约（login / bindDevice / preview / verify（门票与游乐核销共用）/
 *     createBarOrder / pay-intent / settle / submitMaintenance /
 *     confirmResume / 设备凭证 / 冲正 …）。
 *   - [TerminalWorkflow]：组合上述调用 + 本地 [InMemoryLocalStore]（会话/绑定/事件）。
 *
 * 网络调用一律在后台线程（kotlin.concurrent.thread）执行，回到 UI 线程刷新；
 * 失败/离线均给出中文提示。
 *
 * 实现说明：
 *   - 扫码：ZXing 内置扫码（IntentIntegrator）调起真机相机，结果回填输入框；无相机时可手动输入 credential 兜底。
 *   - Keystore：设备公钥/指纹取自 AndroidKeyStore（EC P-256，私钥不可导出），见 DeviceKeystore。
 */
// TK-06：入场人脸拍照的回调请求码（与 ZXing 扫码 0xc0de 区分）。
private const val FACE_SHOT_REQUEST = 0x7E06
// 第 84 条 84-R2：交接补录选证据的回调请求码。
private const val TRACE_EVIDENCE_REQUEST = 0x7E84
// 第 123 条 123-R4：定位权限申请的请求码。
private const val LOCATION_PERMISSION_REQUEST = 0x7E7B

class MainActivity : Activity() {

  private val store = InMemoryLocalStore()
  // 平板 POS 绑定持久化：首次配置本机所属酒吧后写入，之后不再追问（跨启动保留）。
  private val posPrefs: SharedPreferences by lazy { getSharedPreferences("bar_pos", Context.MODE_PRIVATE) }
  private var posBarId: String? = null
  private val workPhonePrefs: SharedPreferences by lazy { getSharedPreferences("work_phone", Context.MODE_PRIVATE) }
  // 第十八批 M 段第 112 条 112-R2：一体机模式绑定持久化（设备号、设备台账行、绑定点位），跨启动保留。
  private val kioskPrefs: SharedPreferences by lazy { getSharedPreferences("kiosk", Context.MODE_PRIVATE) }
  // 购物车（本 Activity 会话态；下单成功清空；平板 POS 与一体机吧台买酒共用）。
  private val posCart = LinkedHashMap<String, PosCartLine>()
  // 4.2 allowed hosts 生效：COMPLEX_API_ALLOWED_HOSTS 构建白名单在客户端入口强制校验。
  private val api: TerminalApiClient by lazy {
    TerminalApiClient(
      BuildConfig.COMPLEX_API_BASE_URL.trim().trimEnd('/'),
      allowedHosts = BuildConfig.COMPLEX_API_ALLOWED_HOSTS.split(',')
        .map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
    ).apply {
      deviceId = workPhonePrefs.getString("device_id", null)
      deviceSigner = DeviceKeystore::sign
      // 第 123 条 123-R4：现场动作闭集提交那一刻取一次本机定位随请求带上（工作手机、平板、一体机共用本客户端）。
      locationProvider = DeviceLocator(this@MainActivity)::currentFix
    }
  }
  private val workflow: TerminalWorkflow by lazy { TerminalWorkflow(api, store) }

  // 4.2 扫码相机：ZXing 内置扫码界面（IntentIntegrator 旧式回调，兼容纯 Activity），
  // 本地解码无网络依赖，结果回填目标输入框。
  private var scanTarget: EditText? = null
  private var scanResult: ((String) -> Unit)? = null
  /** 第十八批 M 段第 116 条：服务端按设备类型下发的功能码，键＝取清单时的终端类型；没取到（或终端类型变了）按 [homeShows] 的口径。 */
  private var appFunctions: Pair<String, Set<String>>? = null
  private fun shows(code: String) = homeShows(api.terminalType, appFunctions?.takeIf { it.first == api.terminalType }?.second, code)
  private var nfcResult: ((String) -> Unit)? = null

  /** 84-R2：提示语可换（交接页对准酒水或物品上的平台二维码）；给了 onResult 就把扫到的码交给它，不回填输入框。 */
  private fun launchScan(target: EditText?, prompt: String = "对准客人验票二维码", onResult: ((String) -> Unit)? = null) {
    scanTarget = target
    scanResult = onResult
    IntentIntegrator(this)
      .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
      .setPrompt(prompt)
      .setBeepEnabled(true)
      .setOrientationLocked(false)
      .initiateScan()
  }

  // TK-06 一人多次票留脸 / 核对：用系统相机拍一张入场客人（缩略图即可交供应商比对），只在内存里，核销提交后即丢。
  private var faceShotResult: ((Bitmap?) -> Unit)? = null

  private fun launchFaceShot(onShot: (Bitmap?) -> Unit) {
    faceShotResult = onShot
    startActivityForResult(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE), FACE_SHOT_REQUEST)
  }

  private fun jpegBase64(bitmap: Bitmap): String {
    val out = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
    return android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == FACE_SHOT_REQUEST) {
      val callback = faceShotResult
      faceShotResult = null
      @Suppress("DEPRECATION")
      callback?.invoke(if (resultCode == RESULT_OK) data?.extras?.get("data") as? Bitmap else null)
      return
    }
    if (requestCode == TRACE_EVIDENCE_REQUEST) {
      val callback = evidencePickResult
      evidencePickResult = null
      val clip = data?.clipData
      val uris = when {
        resultCode != RESULT_OK || data == null -> emptyList()
        clip != null -> (0 until clip.itemCount).map { clip.getItemAt(it).uri }
        else -> listOfNotNull(data.data)
      }
      callback?.invoke(uris)
      return
    }
    val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
    if (result != null) {
      val callback = scanResult
      scanResult = null
      result.contents?.let { value -> if (callback != null) callback(value) else scanTarget?.setText(value) }
      return
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  // ── E批 NFC 打卡:前台分发读点位标签(NDEF 文本或标签 UID),填入当前打卡屏输入框 ──
  private var nfcAdapter: android.nfc.NfcAdapter? = null

  private val uploadingReceipts = java.util.concurrent.atomic.AtomicBoolean(false)

  private fun recordWorkPhoneReceipt(method: String, tag: String? = null, status: TextView? = null) {
    try {
      ChargingReceiptBridge.enqueue(this, method, tag)
      status?.text = "领取事件已保存在手机，正在同步"
      flushWorkPhoneReceipts(status)
    } catch (error: Exception) { status?.text = "保存失败：${friendly(error)}" }
  }

  private fun flushWorkPhoneReceipts(status: TextView? = null) {
    // 第十八批 M 段第 116 条：领取事件与考勤上下文只认工作手机，别的终端不去调。
    if (api.terminalType != "staff_work_phone") return
    val device = api.deviceId
    if (api.accessToken == null || device == null) {
      status?.text = "事件已保留，登录并登记后重试同步"
      return
    }
    if (!uploadingReceipts.compareAndSet(false, true)) return
    runBackground(
      action = {
        try {
          try { ChargingReceiptBridge.rememberContext(this,api.currentWorkPhoneContext()) }
          catch (error: Exception) { ChargingReceiptBridge.clearAnchor(this) }
          ChargingReceiptBridge.flush(this) { pending ->
            try { api.attendanceReceipt(pending.request(device)) }
            catch (error: Exception) { throw IllegalStateException(friendly(error),error) }
          }
        } finally { uploadingReceipts.set(false) }
      },
      onSuccess = { result ->
        ChargingReceiptBridge.saveSyncMessage(this,result.message())
        status?.text = result.message()
        if (status == null && result.failed.isNotEmpty()) android.widget.Toast.makeText(this,"有原领取事件未同步，请打开工作手机考勤处理",android.widget.Toast.LENGTH_LONG).show()
      },
      onError = { status?.text = "同步未完成，原事件保留可重试：${friendly(it)}" },
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ChargingReceiptBridge.clearAnchor(this)
    nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(this)
    // U01-001：终端身份由本机绑定类型决定并只声明一次，重启后从本地绑定记录恢复（工作手机 / 平板 POS）。
    workPhonePrefs.getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = "staff_work_phone" }
    posPrefs.getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = "tablet" }
    // 第十八批 M 段第 112 条 112-R2：一体机模式同样从本机绑定记录恢复 kiosk 身份。
    kioskPrefs.getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = "kiosk" }
    // U01-007：私人手机在本人注册时已把 Keystore 公钥/指纹绑定为员工设备凭证，重启后按登记的指纹恢复私人手机身份（只声明一次）。
    getSharedPreferences("private_phone", MODE_PRIVATE).getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = STAFF_PRIVATE_PHONE }
    if (api.terminalType in setOf("staff_work_phone", "tablet", "kiosk")) requestLocationPermission()
    // U01-002：拔线接收器由常驻前台服务持有并在开机后重注册；本页只消费投递（进程被回收期间的拔线在下次登录后补记）。
    ChargingReceiptBridge.handler = { runOnUiThread { flushWorkPhoneReceipts() } }
    ChargingReceiptService.start(this)
    showLogin()
  }

  // ── 返回键：非首页屏回首页（与各屏「返回首页」按钮一致，无路由栈）；
  //    首页/登录页两秒内双击才退出，避免误触闪退感。 ──
  private var onRootScreen = true
  private var lastBackPressAt = 0L

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if (onRootScreen) {
      val now = System.currentTimeMillis()
      if (now - lastBackPressAt < 2000) {
        @Suppress("DEPRECATION")
        super.onBackPressed()
      } else {
        lastBackPressAt = now
        android.widget.Toast.makeText(this, "再按一次退出", android.widget.Toast.LENGTH_SHORT).show()
      }
    } else {
      showHome()
    }
  }

  override fun onDestroy() {
    ChargingReceiptBridge.handler = null
    super.onDestroy()
  }

  override fun onResume() {
    super.onResume()
    if (api.accessToken != null && api.terminalType == "staff_work_phone") {
      flushWorkPhoneReceipts()
    }
    val adapter = nfcAdapter ?: return
    val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    val pending = android.app.PendingIntent.getActivity(
      this, 0, intent, android.app.PendingIntent.FLAG_MUTABLE)
    adapter.enableForegroundDispatch(this, pending, null, null)
  }

  override fun onPause() {
    super.onPause()
    nfcAdapter?.disableForegroundDispatch(this)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val messages = intent.getParcelableArrayExtra(android.nfc.NfcAdapter.EXTRA_NDEF_MESSAGES)
    // 正确解析 NDEF 文本记录:取第一条「知名类型·文本(RTD_TEXT)」记录,按 payload 结构
    // [状态字节][语言码][正文]跳过状态字节+语言码,只留正文;避免旧写法把语言码"en"污染进点位ID。
    val ndefText = messages?.filterIsInstance<android.nfc.NdefMessage>()
      ?.flatMap { it.records.toList() }
      ?.firstOrNull {
        it.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
          it.type.contentEquals(android.nfc.NdefRecord.RTD_TEXT)
      }
      ?.let { record -> runCatching { decodeNdefTextPayload(record.payload) }.getOrNull() }
    val tag = intent.getParcelableExtra<android.nfc.Tag>(android.nfc.NfcAdapter.EXTRA_TAG)
    val uid = tag?.id?.joinToString("") { b -> "%02x".format(b) }
    val value = ndefText ?: uid
    if (value != null) {
      nfcResult?.invoke(value)
    }
  }

  // ---------------------------------------------------------------------------
  // 屏 ① 登录 -> TerminalWorkflow.login
  // ---------------------------------------------------------------------------
  private fun showLogin() {
    val username = editText("员工账号 / 登录名")
    val password = editText("密码").apply {
      inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
    val status = statusText("请使用员工账号登录\nAPI: ${BuildConfig.COMPLEX_API_BASE_URL}")

    val loginButton = button("登录") {
      val u = username.text.toString().trim()
      val p = password.text.toString()
      if (u.isEmpty() || p.isEmpty()) {
        status.text = "请输入账号和密码"
        return@button
      }
      status.text = "正在登录…"
      runBackground(
        action = { ChargingReceiptBridge.clearAnchor(this); workflow.login(u, p) },
        onSuccess = { session ->
          status.text = "登录成功：${session.username}"
          flushWorkPhoneReceipts()
          if (api.deviceId != null) showHome() else showDeviceBinding()
        },
        onError = { status.text = "登录失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(
        title = "综合体终端 · 登录",
        // U01-001/U01-007：自助注册只属于员工私人手机通道——本机已绑定为工作手机/平板时不显示。
        children = listOfNotNull(status, username, password, loginButton,
          if (api.terminalType == null) button("公司员工自助注册（私人手机）") { showStaffSelfRegistration() } else null),
        root = true,
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ② 设备绑定 -> TerminalWorkflow.bindDevice
  // ---------------------------------------------------------------------------
  private fun showDeviceBinding() {
    val deviceId = editText("设备编号").apply {
      setText(workPhonePrefs.getString("device_id", ""))
    }
    val spaceId = editText("绑定空间（点位）")
    val staffId = editText("持有员工档案编号（领用即绑定，U07-001）")
    val status = statusText("登录账号：${store.session?.username ?: "-"}\n由设备管理员填写已登记的设备编号与领用员工的档案编号；普通员工无需重复配置")

    val bindButton = button("绑定设备") {
      val d = deviceId.text.toString().trim()
      if (d.isEmpty()) {
        status.text = "请输入设备编号"
        return@button
      }
      val holder = staffId.text.toString().trim()
      if (holder.isEmpty()) {
        status.text = "请输入持有员工档案编号"
        return@button
      }
      val space = spaceId.text.toString().trim().ifEmpty { null }
      status.text = "正在绑定…"
      runBackground(
        action = {
          val binding = workflow.bindDevice(
            BindDeviceRequest(
              deviceId = d,
              terminalType = "staff_work_phone",
              bindingScope = "space",
              boundSpaceId = space,
              staffId = holder,
              // 第 96 条 96-R1（PGM:623）：领用即带本机型号进设备台账
              deviceModel = android.os.Build.MODEL,
            ),
          )
          api.registerDeviceSigningKey(d, DeviceKeystore.devicePublicKeyBase64(), java.util.UUID.randomUUID().toString(), "staff_work_phone")
          binding
        },
        onSuccess = { binding ->
          workPhonePrefs.edit().putString("device_id", d).apply()
          ChargingReceiptBridge.clearAnchor(this)
          api.deviceId = d
          api.terminalType = "staff_work_phone"
          requestLocationPermission()
          flushWorkPhoneReceipts()
          status.text = "绑定完成：${binding.deviceCode}（状态 ${deviceStatusZh(binding.status)}）"
          showHome()
        },
        onError = { status.text = "绑定失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(
        title = "设备绑定",
        children = listOf(status, deviceId, spaceId, staffId, bindButton, button("跳过，进入首页") { showHome() }),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ③ 首页 / 功能入口
  // ---------------------------------------------------------------------------
  private fun showHome() {
    // 第十八批 N 段第 120 条 120-R1：一体机进一体机首页（按本机配置列一体机功能）。
    if (api.terminalType == "kiosk") { showKioskHome(); return }
    val info = statusText(homeSummary())
    // 第十八批 M 段第 116 条：每个按钮配服务端功能码（与 StaffAppFunctions.ALL 同名），按下发的清单显示。
    fun entry(code: String, label: String, open: () -> Unit): View? = if (shows(code)) button(label) { open() } else null
    val declared = api.terminalType

    val children = listOfNotNull(
      info,
      entry("ticket_verify", "门票/权益核销") { showTicketVerify() },
      entry("voucher_redeem", "礼物/券核销") { showVoucherRedeem() },
      entry("trace_handover", "酒水与物品交接扫码") { showTraceHandover() },
      entry("amusement_verify", "游乐核销") { showAmusementVerify() },
      entry("bar_order", "酒吧点酒下单 / 支付") { showBarOrder() },
      entry("tablet_pos", "平板点单 POS（图形）") { showPosEntry() },
      entry("kiosk_claim", "一体机模式（权益领取）") { showKioskEntry() },
      entry("amusement_maintenance", "游乐检修提交 / 主管确认") { showMaintenance() },
      entry("staff_device", "员工设备凭证") { showStaffDevice() },
      entry("ticket_reverse", "票务核销冲正") { showReverse() },
      declared?.let { statusText("—— 员工自有手机(BYOD 一期) ——") },
      entry("inbox", "待办与消息中心") { showInbox() },
      entry("notices", "公司公告") { showStaffNotices() },
      entry("my_work_orders", "我的工单") { showMyWorkOrders() },
      entry("my_alarms", "我的接警（已收到并接手）") { showMyFacilityAlarms() },
      entry("bar_reservation", "酒吧空位 / 帮客订位") { showBarAvailability() },
      entry("my_shifts", "我的排班") { showMyShifts() },
      entry("attendance_punch", "考勤打卡(NFC/扫码)") { showPunch() },
      entry("my_payroll", "我的薪资") { showMyPayroll() },
      entry("attendance_month", "考勤月视图") { showAttendanceMonth() },
      entry("leave", "请假") { showLeave() },
      declared?.let { statusText("—— 服务点单(公司代收,月结分账) ——") },
      entry("service_order_create", "代客点服务") { showServiceOrderCreate() },
      entry("my_service_income", "我的服务收入") { showMyServiceOrders() },
      entry("shift_manage", "主管排班(需排班权限)") { showShiftManage() },
      entry("merchant_staff_registration", "商户员工注册申请（商户代提交）") { showStaffRegistration() },
      entry("my_profile", "我的资料(个人信息)") { showMyProfile() },
      button("重新登录") { logoutAndRestart() },
    )

    val view = screen(title = "现场操作首页", children = children, root = true)
    setContentView(view)
    // 已声明身份：后台取一次本端的功能清单，取到后若仍在首页即重绘；取不到维持全部显示。
    if (declared != null && appFunctions?.first != declared) {
      runBackground(
        action = { api.staffAppFunctions().functions.toSet() },
        onSuccess = { appFunctions = declared to it; if (view.isAttachedToWindow) showHome() },
        onError = { },
      )
    }
  }

  private fun homeSummary(): String {
    val s = store.session
    val b = store.binding
    return buildString {
      append("员工：").append(s?.username ?: "未登录").append('\n')
      append("设备：").append(b?.deviceCode ?: "未绑定")
      if (b?.status != null) append("（").append(b.status).append("）")
    }
  }

  // ---------------------------------------------------------------------------
  // 屏 ④ 门票/权益核销 -> 扫码即核销（verify）
  // 第十八批 Q 段第 149 条 149-R1（ENUM:43「检票环节内部为一次动作：扫码即核销，不设先预检、后确认的两次操作，也不设「预检通过」中间态。」；DEC-R6-08）：
  // 扫码即提交，同一次动作里先只读本票人脸处置（faceBindingFor）：不需要人脸即直接核销；需要时现场勾同意、拍照，拍完即提交。
  // 无相机时手动输入后点「核销」兜底。
  // ---------------------------------------------------------------------------
  private fun showTicketVerify() {
    val credential = editText("核销凭证（扫码或手动输入）")
    val status = statusText("扫码即核销")
    // TK-06：capture（首次留脸，须客人单独同意 + 拍照）或 match（按留存人脸比对，须拍照）；为空则不涉及人脸。faceCode＝读到该处置的那个码。
    var faceCode: String? = null
    var faceBinding: String? = null
    var faceCapture: String? = null
    val faceConsent = android.widget.CheckBox(this).apply {
      text = "客人已阅读并同意：本票为一人多次票，首次入场留存人脸，仅用于核对后续各次是否同一使用人；加密保存，该票用完或到期后删除"
      visibility = View.GONE
    }

    fun showResult(r: VerificationResponse) {
      faceCapture = null
      // 164-R1（PGM:287）：原因显示中文，没有中文原因时不出这一行。
      status.text = listOf("核销结果：${verifyStatusZh(r.status)}", "核销号：${r.verificationNo}", verificationReasonZh(r.resultReason))
        .filter { it.isNotEmpty() }.joinToString("\n")
    }

    fun verifyWithFace(code: String, capture: String) {
      val consent = if (faceBinding == "capture") true else null
      requireBindingOr(status) {
        status.text = "正在核销…"
        runBackground(
          action = { workflow.verifyOnline(code, capture, consent) },
          onSuccess = { showResult(it) },
          onError = { status.text = "核销失败：${friendly(it)}" },
        )
      }
    }

    // TK-06 裁决2「不同意就不能首次入场：票仍是未核销状态」：capture 时未勾同意不拍、不提交。
    fun consentMissing(): Boolean {
      if (faceBinding != "capture" || faceConsent.isChecked) return false
      status.text = "客人未同意留存人脸，不能首次入场（票仍未核销）"
      return true
    }

    // 拍到照片即提交。
    val faceButton = button("为入场客人拍照") {
      val code = faceCode ?: return@button
      if (consentMissing()) return@button
      launchFaceShot { bitmap ->
        if (bitmap == null) {
          status.text = "未拍到照片，请重拍"
        } else {
          val shot = jpegBase64(bitmap)
          faceCapture = shot
          verifyWithFace(code, shot)
        }
      }
    }.apply { visibility = View.GONE }

    fun submit(raw: String) {
      val code = raw.trim()
      if (code.isEmpty()) { status.text = "请输入核销凭证"; return }
      faceCode = null
      faceBinding = null
      faceCapture = null
      faceConsent.isChecked = false
      faceConsent.visibility = View.GONE
      faceButton.visibility = View.GONE
      // 第十八批 Q 段第 150 条 150-R2（BSM:552「进场之后，工作手机扫客人二维码核销各酒吧入场……」）：邀请函成员入场码在读人脸处置之前分流，
      // 按码中订台号送订台入场核验，同样扫码即核销（ENUM:43）。
      if (invitationEntryReservationId(code) != null) {
        requireBindingOr(status) {
          status.text = "正在核验入场…"
          runBackground(
            action = { workflow.invitationEntryVerifyOnline(code) },
            onSuccess = { status.text = "邀请函入场核验通过：已入场" },
            onError = { status.text = "邀请函入场核验失败：${friendly(it)}" },
          )
        }
        return
      }
      requireBindingOr(status) {
        status.text = "正在核销…"
        runBackground(
          // 同一次动作：先只读人脸处置，不需要人脸就直接核销。
          action = { workflow.faceBindingFor(code).let { binding -> binding to (if (binding == null) workflow.verifyOnline(code) else null) } },
          onSuccess = { (binding, r) ->
            if (r != null) {
              showResult(r)
            } else {
              faceCode = code
              faceBinding = binding
              faceConsent.visibility = if (binding == "capture") View.VISIBLE else View.GONE
              faceButton.visibility = View.VISIBLE
              status.text = if (binding == "capture") "一人多次票首次入场：须客人同意留存人脸并拍照；不同意不能入场（票仍未核销）"
              else "一人多次票：须为入场客人拍照，由系统比对是否同一使用人"
            }
          },
          onError = { status.text = "核销失败：${friendly(it)}" },
        )
      }
    }

    // 给了 onResult 时扫码结果只交回调、不回填输入框（launchScan），这里回填后直接提交。
    val scanButton = button("扫码核销") { launchScan(credential) { code -> credential.setText(code); submit(code) } }
    // 手输兜底；本码已读到人脸处置且已拍照（如上次提交失败）时直接重提，否则按一次动作重走。
    val verifyButton = button("核销") {
      val code = credential.text.toString().trim()
      val capture = faceCapture
      if (code == faceCode && capture != null) {
        if (!consentMissing()) verifyWithFace(code, capture)
      } else {
        submit(code)
      }
    }

    setContentView(
      screen(
        title = "门票 / 权益核销",
        children = listOf(status, credential, scanButton, faceConsent, faceButton, verifyButton, backButton()),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 第十八批 M 段第 112 条 112-R1（BSM:947「工作手机扫描权益二维码，核销赠饮、礼物或其他领取资格。」；ENUM:81「到店由店员核销领取时才实扣」；
  // BSM:1197「出库须同时核验订单和物品码」）：工作手机「礼物/券核销」——扫客人券包出示的券码先查券，店内实物券逐件扫物品码后核销。
  // 112-R2（BSM:985、BSM:1948；BSM:980「……让客人或现场工作人员在固定点位完成查询、权益领取……」；BSM:1047「……权益领取自助操作。」）：
  // 一体机权益领取屏同一套，另收门票权益码（走票务核销预览与核销）；登录员工进入后常驻，客人或现场工作人员都可在此扫码查看与领取。
  // ---------------------------------------------------------------------------
  private fun showVoucherRedeem() = showClaim(kiosk = false)

  private fun showKioskClaim() = showClaim(kiosk = true)

  private fun showClaim(kiosk: Boolean) {
    val verb = if (kiosk) "领取" else "核销"
    val code = editText(if (kiosk) "券码或权益码（扫码或手动输入）" else "券码（扫码或手动输入）")
    val traceCodes = editText("物品码（逐件扫，空格或逗号分隔）").apply { visibility = View.GONE }
    val status = statusText(if (kiosk) "扫码查看能领什么、能领多少、是否已领取，再点领取" else "扫客人券包里的券码，先查券再核销")
    // 只认刚查过的那个码；店内实物券记下要逐件扫的物品码个数。
    var checked: String? = null
    var goodsQuantity: Int? = null
    val scanTrace = button("扫物品码") {
      launchScan(null, "对准礼品上的平台二维码") { c ->
        traceCodes.setText(listOf(traceCodes.text.toString().trim(), c.trim()).filter { it.isNotEmpty() }.joinToString(" "))
      }
    }.apply { visibility = View.GONE }
    fun showTrace(quantity: Int?) {
      goodsQuantity = quantity
      traceCodes.setText("")
      traceCodes.visibility = if (quantity != null) View.VISIBLE else View.GONE
      scanTrace.visibility = traceCodes.visibility
    }
    // 门票权益：设备＝本机设备台账行、点位＝本机绑定点位（一体机绑定时存下）。
    fun kioskTarget(): Pair<String, String>? {
      val device = kioskPrefs.getString("binding_id", null)
      val point = kioskPrefs.getString("point_id", null)
      if (device.isNullOrBlank() || point.isNullOrBlank()) { status.text = "本机未完成一体机绑定"; return null }
      return device to point
    }
    val scanButton = button("扫码读取") { launchScan(code, "对准客人出示的券码或权益码") }
    val previewButton = button(if (kiosk) "查看可领取" else "查券") {
      val c = code.text.toString().trim()
      if (c.isEmpty()) { status.text = "请扫码或输入"; return@button }
      if (store.session == null) { status.text = "请先登录"; return@button }
      checked = null
      showTrace(null)
      if (!kiosk || claimTarget(c) == ClaimTarget.VOUCHER) {
        status.text = "正在查询…"
        runBackground(
          action = { api.previewVoucherRedeem(c) },
          onSuccess = { p ->
            checked = c
            if (p.blockReason == null) showTrace(p.goodsQuantity)
            status.text = buildString {
              append(p.name ?: "券")
              p.goodsQuantity?.let { append(" × ").append(it).append(" 件") }
              append("\n状态：").append(voucherStatusZh(p.status))
              p.expiresAt?.let { append("\n有效期至：").append(it) }
              append('\n').append(p.blockReason?.let { "不可${verb}：${voucherBlockReasonZh(it)}" }
                ?: p.goodsQuantity?.let { "可${verb}：须逐件扫 $it 个物品码" } ?: "可${verb}")
            }
          },
          onError = { status.text = "查询失败：${friendly(it)}" },
        )
      } else {
        val (device, point) = kioskTarget() ?: return@button
        status.text = "正在查询…"
        runBackground(
          action = { api.preview(kioskEntitlementRequest(c, device, point, java.util.UUID.randomUUID().toString(), store.session?.staffId)) },
          onSuccess = { r ->
            checked = c
            val e = r.entitlement
            status.text = buildString {
              append(entitlementTypeZh(e?.entitlementType))
              if (e != null) {
                append("\n剩余次数：").append(e.totalTimes - e.usedTimes).append("（共 ").append(e.totalTimes).append(" 次）")
                e.validTo?.let { append("\n有效期至：").append(it) }
                append("\n状态：").append(statusZh(e.status))
              }
              append('\n').append(if (r.allowed) "可领取" else "不可领取")
            }
          },
          onError = { status.text = "查询失败：${friendly(it)}" },
        )
      }
    }
    val claimButton = button(verb) {
      val c = code.text.toString().trim()
      if (c.isEmpty() || c != checked) { status.text = if (kiosk) "请先查看可领取" else "请先查券"; return@button }
      if (!kiosk || claimTarget(c) == ClaimTarget.VOUCHER) {
        val quantity = goodsQuantity
        val codes = scannedTraceCodes(traceCodes.text.toString())
        if (quantity != null && codes.size != quantity) { status.text = "须逐件扫 $quantity 个物品码"; return@button }
        status.text = "正在提交…"
        runBackground(
          // 每次新幂等键提交。
          action = { api.redeemVoucher(VoucherRedeemRequest(java.util.UUID.randomUUID().toString(), c, codes.takeIf { quantity != null })) },
          onSuccess = { checked = null; showTrace(null); status.text = "${verb}完成" },
          onError = { status.text = "${verb}失败：${friendly(it)}" },
        )
      } else {
        val (device, point) = kioskTarget() ?: return@button
        status.text = "正在提交…"
        runBackground(
          action = { api.verify(kioskEntitlementRequest(c, device, point, java.util.UUID.randomUUID().toString(), store.session?.staffId)) },
          onSuccess = { r ->
            checked = null
            status.text = if (r.status == "success") "领取完成" else "不可领取：${verifyStatusZh(r.status)} ${verificationReasonZh(r.resultReason)}"
          },
          onError = { status.text = "领取失败：${friendly(it)}" },
        )
      }
    }
    setContentView(
      screen(
        title = if (kiosk) "一体机 · 权益领取" else "礼物 / 券核销",
        children = listOf(status, code, scanButton, previewButton, traceCodes, scanTrace, claimButton, backButton()),
      ),
    )
  }

  /** 112-R2：一体机模式入口——本机还没绑定为一体机时先绑定，之后进一体机首页（120-R1）。 */
  private fun showKioskEntry() {
    if (store.session == null) { showLogin(); return }
    if (kioskPrefs.getString("device_id", null).isNullOrBlank()) showKioskSetup() else showKioskHome()
  }

  /** 112-R2：把本机绑定为一体机（按空间绑定点位）并登记签名公钥；所属商户不在一体机上填（赠饮按设备所属商户判门店）。
   *  120-R1（BSM:980「……其中只有启用售票能力的一体机，才允许授权服务人员在到店服务场景下辅助客人购票。」）：绑定时选一体机类型
   *  （服务型 / 售票型），存下服务端签发的设备令牌（读本机配置用）与类型。 */
  private fun showKioskSetup() {
    val deviceId = editText("设备编号")
    val spaceId = editText("绑定点位")
    val serviceType = android.widget.RadioButton(this).apply { text = "服务型"; id = View.generateViewId() }
    val ticketingType = android.widget.RadioButton(this).apply { text = "售票型"; id = View.generateViewId() }
    val kioskType = android.widget.RadioGroup(this).apply {
      orientation = LinearLayout.HORIZONTAL; addView(serviceType); addView(ticketingType); check(serviceType.id)
    }
    val status = statusText("由设备管理员填写已登记的设备编号与点位，并选一体机类型。所属商户请管理员在后台设备台账页「所属商户」设置，未设置时赠饮领取会被拒绝；售票型一体机还须管理员在后台为本机开启售票。")
    val bindButton = button("绑定并进入一体机首页") {
      val d = deviceId.text.toString().trim()
      val space = spaceId.text.toString().trim()
      val mode = if (ticketingType.isChecked) "ticketing" else "service"
      if (d.isEmpty()) { status.text = "请输入设备编号"; return@button }
      if (space.isEmpty()) { status.text = "请输入绑定点位"; return@button }
      status.text = "正在绑定…"
      runBackground(
        action = {
          val binding = workflow.bindDevice(
            BindDeviceRequest(
              deviceId = d,
              terminalType = "kiosk",
              bindingScope = "space",
              boundSpaceId = space,
              kioskMode = mode,
              deviceModel = android.os.Build.MODEL,
            ),
          )
          api.registerDeviceSigningKey(d, DeviceKeystore.devicePublicKeyBase64(), java.util.UUID.randomUUID().toString(), "kiosk")
          binding
        },
        onSuccess = { binding ->
          kioskPrefs.edit().putString("device_id", d).putString("binding_id", binding.id).putString("point_id", space)
            .putString("device_token", binding.deviceToken).putString("kiosk_mode", mode).apply()
          api.deviceId = d
          api.terminalType = "kiosk"
          requestLocationPermission()
          kioskConfig = null
          showKioskHome()
        },
        onError = { status.text = "绑定失败：${friendly(it)}" },
      )
    }
    setContentView(screen(title = "一体机 · 绑定",
      children = listOf(status, deviceId, spaceId, fieldLabel("一体机类型"), kioskType, bindButton, backButton())))
  }

  // ---------------------------------------------------------------------------
  // 第十八批 N 段第 120 条 120-R1（BSM:1906「扫码查看和领取权益，也可做订台下单、吧台买酒下单、游乐设施状态查看、定期检修记录和主管确认；
  // 启用售票能力的一体机可由授权服务人员辅助到店购票和订单查询。」）：一体机首页——凭绑定时签发的设备令牌取本机配置（类型、是否启用售票、
  // 所属商户），取到后按配置列一体机功能；取不到只留权益领取与重新登录。常驻员工会话不变，要授权员工的动作走 [staffSignInDialog]。
  // ---------------------------------------------------------------------------
  private var kioskConfig: KioskConfig? = null
  /** 120-R1 复核：一体机上当场登录、整段办理的授权员工（辅助售票、检修）；回一体机首页（结束售票、返回首页、系统返回键）时以其令牌登出。 */
  private var kioskStaff: LoginResponse? = null

  /** 120-R1 复核：每次进一体机首页都按设备台账重读本机配置（后台改所属商户、开关售票后回首页即生效）；读到后只重绘（[reload] 为假）。 */
  private fun showKioskHome(reload: Boolean = true) {
    kioskStaff?.let { staffSignOut(it) }
    val config = kioskConfig
    val info = statusText(config?.let {
      "本机：${it.deviceId}（${kioskModeZh(it.kioskMode)}）\n售票：${if (it.ticketingEnabled) "已启用" else "未启用"}\n所属商户：${it.merchantId ?: "未设置"}"
    } ?: "正在读取本机配置…")
    val view = screen(
      title = "一体机首页",
      children = listOfNotNull(info, button("权益领取") { showKioskClaim() },
        config?.let { button("订台") { showKioskReservation() } },
        config?.let { button("吧台买酒") { showKioskBarPurchase() } },
        config?.let { button("游乐设施状态") { showKioskAmusementStatus() } },
        config?.let { button("游乐检修 / 主管确认") { showMaintenance() } },
        config?.takeIf { it.ticketingEnabled }?.let { button("辅助售票与订单查询") { showKioskTicketing() } },
        button("重新登录") { logoutAndRestart() }),
      root = true,
    )
    setContentView(view)
    if (!reload) return
    val device = kioskPrefs.getString("device_id", null)
    val token = kioskPrefs.getString("device_token", null)
    if (device.isNullOrBlank() || token.isNullOrBlank()) { info.text = "本机配置读取失败：本机未完成一体机绑定"; return }
    runBackground(
      action = { api.kioskConfig(device, token) },
      onSuccess = { kioskConfig = it; if (view.isAttachedToWindow) showKioskHome(reload = false) },
      onError = { info.text = "本机配置读取失败：${friendly(it)}" },
    )
  }

  /**
   * 第十八批 N 段第 120 条 120-R5（BSM:991「查看游乐设施状态：开放、排队、容量、暂停、关闭、故障、维护或安全提醒。」；BSM:1268「通过触摸屏一体机展示
   * 游乐设施开放、排队、容量、暂停、故障和维护状态。」）：一体机游乐设施状态屏，常驻会话即可看（客人可看）。每个项目一张卡：状态、在玩/容量、排队人数、
   * 维护设备数、故障数、安全提醒；服务端读取即留状态检查痕（设备、点位、时间）。
   */
  private fun showKioskAmusementStatus() {
    val status = statusText("正在读取游乐设施状态…")
    val list = rowsContainer()
    val refresh = {
      status.text = "正在读取游乐设施状态…"
      runBackground(
        action = { api.amusementStatusBoard() },
        onSuccess = { rows ->
          status.text = if (rows.isEmpty()) "暂无游乐设施" else "游乐设施 ${rows.size} 个"
          list.removeAllViews()
          rows.forEach { row ->
            list.addView(statusText(amusementStatusCard(row)).apply {
              layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(8) }
            })
          }
        },
        onError = { status.text = "读取失败：${friendly(it)}" },
      )
    }
    refresh()
    setContentView(screen(title = "游乐设施状态", children = listOf(status, list, button("刷新") { refresh() }, backButton())))
  }

  /** 120-R5：一张项目卡的文字。 */
  private fun amusementStatusCard(row: AmusementProjectStatus): String = buildString {
    append("${row.projectName ?: row.projectId}　${amusementOpenStatusZh(row.openStatus)}")
    append("\n在玩 ${row.occupiedCount} / 容量 ${row.capacity}　排队 ${row.waitingCount} 人")
    append("\n维护设备 ${row.maintenanceDeviceCount} 台　故障 ${row.faultCount} 起")
    if (row.safetyNotices.isNotEmpty()) append("\n安全提醒：${row.safetyNotices.joinToString("；")}")
  }

  /** 游乐项目开放状态->中文（amusement_projects.open_status）。 */
  private fun amusementOpenStatusZh(raw: String?): String = when (raw) {
    "open" -> "开放"; "paused" -> "暂停"; "closed" -> "关闭"; "maintenance" -> "维护"
    else -> raw ?: "-"
  }

  /**
   * 第十八批 N 段第 120 条 120-R3（BSM:987「支持订台下单：选择酒吧、日期、时段、卡座/桌台/吧台位、人数和订台备注；订台需要支付定金。」；
   * BSM:1058「……客人自助订台支付定金成功……该台位即进入已预留……冲突时先支付定金者得……」）：一体机订台屏，常驻会话即可下（客人可自助）。
   * 先选酒吧再选台位（已订的置灰），填到店时间、最晚到店时间、人数、客人手机号、订台备注下单；服务端按一体机来源记定金待付，
   * 收定金走 [kioskPayStep]（授权员工登记现金 / POS），登记后才保留台位。
   */
  private fun showKioskReservation() {
    val status = statusText("正在读取可订台位…")
    val bars = rowsContainer()
    val tables = rowsContainer()
    var rows: List<BarTableAvailabilityRow> = emptyList()
    var picked: BarTableAvailabilityRow? = null
    val time = editText("到店时间（如 2026-11-03T20:00:00+08:00）")
    val latest = editText("最晚到店时间（如 2026-11-03T22:00:00+08:00）")
    val guests = editText("人数").apply { inputType = InputType.TYPE_CLASS_NUMBER }
    val phone = editText("客人手机号").apply { inputType = InputType.TYPE_CLASS_PHONE }
    val remark = editText("订台备注（选填）")
    fun showTables(barId: String) {
      picked = null
      val list = rows.filter { it.barId == barId }
      fillSelectableRows(tables, list.map { r ->
        "${r.tableCode ?: r.id} ${r.tableName ?: ""} ${tableTypeZh(r.tableType)}　可坐 ${r.capacity ?: 0} 人　低消 ¥${yuan(r.minConsumptionCent)}" +
          (if (r.reserved == true) "　已订" else "") to { picked = r; status.text = "已选台位：${r.tableCode ?: r.id}" }
      }, "该酒吧暂无台位")
      list.forEachIndexed { i, r -> if (r.reserved == true) tables.getChildAt(i)?.apply { isEnabled = false; alpha = 0.4f } }
      status.text = "请选台位（已订的不可选）"
    }
    val submit = button("提交订台") {
      val table = picked ?: run { status.text = "请先选酒吧和台位"; return@button }
      val at = time.text.toString().trim()
      val la = latest.text.toString().trim()
      val g = guests.text.toString().trim().toIntOrNull()
      val ph = phone.text.toString().trim()
      if (at.isEmpty() || la.isEmpty() || ph.isEmpty() || g == null || g <= 0) {
        status.text = "请填全到店时间、最晚到店时间、人数和客人手机号"
        return@button
      }
      status.text = "正在提交订台…"
      runBackground(
        action = {
          api.createBarReservation(BarReservationCreateRequest(idempotencyKey = java.util.UUID.randomUUID().toString(),
            barId = table.barId ?: "", tableId = table.id ?: "", reservationTime = at, guestCount = g, latestArrivalAt = la,
            customerPhone = ph, remark = remark.text.toString().trim().ifEmpty { null }))
        },
        onSuccess = { showKioskReservationDeposit(it) },
        onError = { status.text = "订台失败：${friendly(it)}" },
      )
    }
    setContentView(screen(title = "订台", children = listOf(status, fieldLabel("选择酒吧"), bars, fieldLabel("选择台位"), tables,
      time, latest, guests, phone, remark, submit, backButton())))
    runBackground(
      action = { api.listBarTableAvailability() },
      onSuccess = { all ->
        rows = all
        val barIds = all.mapNotNull { it.barId }.distinct()
        status.text = if (barIds.isEmpty()) "暂无可订台位" else "请选酒吧"
        fillSelectableRows(bars, barIds.map { b -> "酒吧 $b" to { showTables(b) } }, "暂无可订台位")
      },
      onError = { status.text = "台位读取失败：${friendly(it)}" },
    )
  }

  /** 120-R3：订台已提交——显示订台号与应付定金，收定金走 [kioskPayStep]，登记后显示台位是否已保留。 */
  private fun showKioskReservationDeposit(reservation: BarReservationRecord) {
    val id = reservation.id ?: "-"
    val amount = reservation.depositAmountCent ?: 0L
    setContentView(screen(title = "订台已提交", children = listOf(
      statusText("订台号：$id\n应付定金：¥${yuan(amount)}\n付定金后才为您保留台位"),
      button("付定金") {
        kioskPayStep("订台 $id 定金", amount, KioskPayTarget.Reservation(id), null) { paid ->
          val result = if (paid?.holdStatus == "held") "台位已保留" else "该台位已被先付定金者预订，定金将原路退回"
          setContentView(screen(title = "订台结果", children = listOf(statusText("订台号：$id\n$result"), backButton())))
        }
      },
      backButton())))
  }

  /**
   * 第十八批 N 段第 120 条 120-R4（BSM:988「支持吧台买酒下单：选择酒水、套餐、数量，生成待支付订单。」；BSM:1024「接收一体机产生的吧台买酒订单。」；
   * BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」）：一体机吧台买酒，常驻会话即可下（客人可自助）。
   * 酒吧＝本机所属商户，台位＝该酒吧的吧台位（一个自动选、多个让选）；目录与购物车同平板 POS（[showPosCatalog]），下单后生成待支付账单，
   * 收款走 [kioskPayStep]（账单），登记后提示到吧台取酒（出品与扣吧台库存由吧台办理）。
   */
  private fun showKioskBarPurchase() {
    val bar = kioskConfig?.merchantId ?: run {
      setContentView(screen(title = "吧台买酒", children = listOf(
        statusText("本机未设置所属商户，请管理员在后台设备台账页设置本机所属商户"), backButton())))
      return
    }
    posCart.clear()
    val status = statusText("正在读取吧台位…")
    val tables = rowsContainer()
    setContentView(screen(title = "吧台买酒", children = listOf(status, tables, backButton())))
    runBackground(
      action = { kioskBarCounters(api.listBarTableAvailability(), bar) },
      onSuccess = { counters ->
        when (counters.size) {
          0 -> status.text = "本酒吧未配置吧台位"
          1 -> showPosCatalog(bar, counters.first(), kiosk = true)
          else -> {
            status.text = "请选吧台位"
            fillSelectableRows(tables, counters.map { t ->
              (t.tableName ?: t.tableCode ?: t.id ?: "吧台位") to { showPosCatalog(bar, t, kiosk = true) }
            }, "本酒吧未配置吧台位")
          }
        }
      },
      onError = { status.text = "吧台位读取失败：${friendly(it)}" },
    )
  }

  /** 一体机类型->中文（terminal_devices.kiosk_mode）。 */
  private fun kioskModeZh(raw: String?): String = when (raw) {
    "ticketing" -> "售票型"; "service" -> "服务型"
    else -> raw ?: "-"
  }

  /**
   * 120-R1（BSM:986「……可由授权服务人员登录后辅助客人购票……」；BSM:990「……必须由授权员工记录……」；BSM:992「工程师可在授权后填写……」）：
   * 一体机上的授权员工登录——常驻员工会话不动，授权员工输账号密码取自己的令牌交给调用方在 [asStaff] 里用，用完 [staffSignOut]。
   */
  private fun staffSignInDialog(title: String, onSignedIn: (LoginResponse) -> Unit) {
    val username = editText("授权员工账号 / 登录名")
    val password = editText("密码").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
    val form = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), 0); addView(username); addView(password)
    }
    android.app.AlertDialog.Builder(this)
      .setTitle(title)
      .setView(form)
      .setPositiveButton("登录") { _, _ ->
        val u = username.text.toString().trim()
        val p = password.text.toString()
        if (u.isEmpty() || p.isEmpty()) {
          android.widget.Toast.makeText(this, "请输入授权员工账号和密码", android.widget.Toast.LENGTH_SHORT).show()
          return@setPositiveButton
        }
        runBackground(
          action = { api.login(u, p) },
          onSuccess = onSignedIn,
          onError = { android.widget.Toast.makeText(this, "授权员工登录失败：${friendly(it)}", android.widget.Toast.LENGTH_LONG).show() },
        )
      }
      .setNegativeButton("取消", null)
      .show()
  }

  /** 120-R1：授权员工用完即以自己的令牌登出，常驻会话不动。 */
  private fun staffSignOut(signIn: LoginResponse) {
    if (kioskStaff === signIn) kioskStaff = null
    runBackground(action = { api.asStaff(signIn.accessToken) { api.logout(signIn.sessionId) } }, onSuccess = { }, onError = { })
  }

  /**
   * 第十八批 N 段第 120 条 120-R7（BSM:989「支持支付引导：优先生成二维码让客人手机扫码支付，也可由工作人员辅助扫码付款码。」；
   * BSM:990「允许现场现金和 POS 机支付，但必须由授权员工记录到订单、账单、日结和对账里。」）：一体机收款步骤——显示应付金额；
   * 线上扫码支付待支付渠道开通（第 41 条），只写明、不放按钮；现金 / POS 由授权员工登记（[registerKioskPayment]）。
   * [signedIn] 为空时先请授权员工当场登录、登记完即登出；售票流程传入已登录的服务人员，由售票流程结束时登出。
   * 第十八批 S 段第 169 条（BSM:1066「POS（记流水号+收款人）」）：酒吧账单（169-R1）与订台定金（169-R2）POS 收款必填 POS 流水号，没填不进员工登录与提交；票务订单仍可不填。
   */
  private fun kioskPayStep(title: String, amountCent: Long, target: KioskPayTarget, signedIn: LoginResponse?,
      onPaid: (BarReservationRecord?) -> Unit) {
    val amount = "¥${"%.2f".format(amountCent / 100.0)}"
    val status = statusText("应付金额：$amount\n线上扫码支付待支付渠道开通，请由工作人员收取现金或 POS")
    val cash = android.widget.RadioButton(this).apply { text = "现金"; id = View.generateViewId() }
    val pos = android.widget.RadioButton(this).apply { text = "POS"; id = View.generateViewId() }
    val channel = android.widget.RadioGroup(this).apply {
      orientation = LinearLayout.HORIZONTAL; addView(cash); addView(pos); check(cash.id)
    }
    val posRequired = target !is KioskPayTarget.TicketOrder
    val posNo = editText(if (posRequired) "POS 流水号（POS 收款必填）" else "POS 单号（POS 收款时可填）")
    val register = button("工作人员登记现金 / POS") {
      val ch = if (pos.isChecked) "pos" else "cash"
      val no = posNo.text.toString().trim().ifEmpty { null }
      if (posRequired && ch == "pos" && no == null) {
        status.text = "POS 收款须填写 POS 流水号"
        return@button
      }
      fun submit(staff: LoginResponse, signOut: Boolean) {
        status.text = "正在登记收款…"
        runBackground(
          action = { api.registerKioskPayment(target, amountCent, ch, staff, no, signOut) },
          onSuccess = { onPaid(it) },
          onError = { status.text = "登记失败：${friendly(it)}" },
        )
      }
      confirmThen(title, "确认已收取 $amount（${if (ch == "pos") "POS" else "现金"}）？登记即确认收款完成，不可撤销。") {
        if (signedIn != null) submit(signedIn, false) else staffSignInDialog("授权员工登记收款") { submit(it, true) }
      }
    }
    setContentView(screen(title = title, children = listOf(status, fieldLabel("收款方式"), channel, posNo, register, backButton())))
  }

  // ---------------------------------------------------------------------------
  // 第十八批 N 段第 120 条 120-R2（BSM:986「启用售票能力的一体机可由授权服务人员登录后辅助客人购票和查询订单（门票为动态二维码，不提供取票功能；
  // 客人用手机号短信验证绑定，在官方 App/小程序查看门票）；在某个酒吧门口或前台售票时，只能售卖该酒吧的单店门票，不得售卖综合体通票。」；
  // BSM:1947「……一体机必须绑定点位、限制可售票种，并记录服务人员、设备、点位、购票原因、支付和出票。」；DEC-R7-05「售票型触摸屏一体机能力补「订单查询」」）：
  // 辅助售票与订单查询——授权服务人员当场登录，整段请求带其令牌（[asStaff]），「结束售票」或回一体机首页即登出。票种按本机所属酒吧列（服务端再按本机与员工范围拦），
  // 选票种与数量、录客人手机号发验证码、填客人报的验证码与售票原因下单，收款走 [kioskPayStep]，收款后出票；不显示票码、不提供取票。
  // ---------------------------------------------------------------------------
  private fun showKioskTicketing() = staffSignInDialog("授权服务人员登录") { kioskStaff = it; showKioskTicketSale(it) }

  private fun endKioskTicketing(staff: LoginResponse) {
    staffSignOut(staff)
    showKioskHome()
  }

  private fun showKioskTicketSale(staff: LoginResponse) {
    val status = statusText("正在读取可售票种…")
    val products = rowsContainer()
    var picked: KioskTicketProduct? = null
    val quantity = editText("购票数量").apply { inputType = InputType.TYPE_CLASS_NUMBER; setText("1") }
    val phone = editText("客人手机号").apply { inputType = InputType.TYPE_CLASS_PHONE }
    val smsCode = editText("客人收到的验证码").apply { inputType = InputType.TYPE_CLASS_NUMBER }
    val reason = editText("售票原因（必填）")
    val sendCode = button("发送验证码") {
      val p = phone.text.toString().trim()
      if (p.isEmpty()) { status.text = "请输入客人手机号"; return@button }
      runBackground(
        action = { api.asStaff(staff.accessToken) { api.requestKioskBindCode(p) } },
        onSuccess = { status.text = "验证码已发送到客人手机，请客人报出验证码" },
        onError = { status.text = "验证码发送失败：${friendly(it)}" },
      )
    }
    val order = button("下单") {
      val product = picked ?: run { status.text = "请先选票种"; return@button }
      val count = quantity.text.toString().trim().toIntOrNull()?.takeIf { it > 0 } ?: run { status.text = "请输入购票数量"; return@button }
      val p = phone.text.toString().trim()
      val code = smsCode.text.toString().trim()
      val r = reason.text.toString().trim()
      if (p.isEmpty() || code.isEmpty()) { status.text = "请输入客人手机号和客人收到的验证码"; return@button }
      if (r.isEmpty()) { status.text = "请填写售票原因"; return@button }
      status.text = "正在下单…"
      runBackground(
        action = {
          api.asStaff(staff.accessToken) {
            api.createTicketOrder(KioskTicketOrderRequest(java.util.UUID.randomUUID().toString(), product.ticketTypeId, count, p, code, r, product.barId))
          }
        },
        onSuccess = { kioskTicketPay(staff, it) },
        onError = { status.text = "下单失败：${friendly(it)}" },
      )
    }
    setContentView(screen(title = "辅助售票", children = listOf(status, products, fieldLabel("购票数量"), quantity, phone, sendCode, smsCode, reason,
      order, button("订单查询") { showKioskOrderLookup(staff) }, button("结束售票") { endKioskTicketing(staff) })))
    runBackground(
      action = { api.asStaff(staff.accessToken) { api.listKioskTicketProducts(kioskConfig?.merchantId) } },
      onSuccess = { rows ->
        status.text = if (rows.isEmpty()) "暂无可售票种" else "请选票种（门票为动态二维码，客人在官方 App/小程序查看，不提供取票）"
        fillSelectableRows(products, rows.map { row -> kioskTicketProductText(row) to { picked = row; status.text = "已选：${row.ticketName ?: row.ticketTypeId}" } },
          "暂无可售票种")
      },
      onError = { status.text = "票种读取失败：${friendly(it)}" },
    )
  }

  /** 120-R2：票种一行——票名、平日/周末/节假日价、能否退、有效天数、使用范围。 */
  private fun kioskTicketProductText(row: KioskTicketProduct): String = buildString {
    append(row.ticketName ?: row.ticketTypeId)
    append("\n平日 ¥${yuan(row.weekdayPriceCent)}　周末 ¥${yuan(row.weekendPriceCent)}　节假日 ¥${yuan(row.holidayPriceCent)}")
    append("\n${if (row.refundable) "可退" else "不可退"}　有效 ${row.validityDays ?: "-"} 天　使用范围：${row.usageScope ?: "-"}")
  }

  /** 120-R2：下单后收款（[kioskPayStep]，服务人员已登录、不再二次登录），收款后出票。 */
  private fun kioskTicketPay(staff: LoginResponse, order: TicketOrderRow) =
    kioskPayStep("票务订单 ${order.orderNo ?: order.id}", order.amountCent, KioskPayTarget.TicketOrder(order.id), staff) {
      kioskTicketIssue(staff, order)
    }

  private fun kioskTicketIssue(staff: LoginResponse, order: TicketOrderRow) {
    val status = statusText("收款已登记，正在出票…")
    // 120-R1 复核：出票返回前「继续售票」「结束售票」不可点，出票请求还在路上时不登出。
    val next = listOf(button("继续售票") { showKioskTicketSale(staff) }, button("结束售票") { endKioskTicketing(staff) })
      .onEach { it.isEnabled = false }
    setContentView(screen(title = "出票", children = listOf(status) + next))
    runBackground(
      action = {
        api.asStaff(staff.accessToken) {
          api.issueTicketOrder(order.id, TicketOrderActionRequest(java.util.UUID.randomUUID().toString(), "一体机辅助售票出票"))
        }
      },
      onSuccess = { status.text = "已出票，请客人用该手机号登录官方 App/小程序查看门票"; next.forEach { it.isEnabled = true } },
      onError = { status.text = "出票失败：${friendly(it)}"; next.forEach { it.isEnabled = true } },
    )
  }

  /** 120-R2（DEC-R7-05）：订单查询——按客人手机号列订单（号码掩码），待支付的选中后可继续收款或取消。 */
  private fun showKioskOrderLookup(staff: LoginResponse) {
    val status = statusText("输入客人手机号查订单")
    val phone = editText("客人手机号").apply { inputType = InputType.TYPE_CLASS_PHONE }
    val orders = rowsContainer()
    var picked: TicketOrderRow? = null
    fun search() {
      val p = phone.text.toString().trim()
      if (p.isEmpty()) { status.text = "请输入客人手机号"; return }
      picked = null
      runBackground(
        action = { api.asStaff(staff.accessToken) { api.listTicketOrdersByPhone(p) } },
        onSuccess = { rows ->
          status.text = if (rows.isEmpty()) "该手机号没有订单" else "共 ${rows.size} 张订单，待支付的可选中后继续收款或取消"
          fillSelectableRows(orders, rows.map { row -> kioskOrderText(row) to { picked = row } }, "该手机号没有订单")
        },
        onError = { status.text = "查询失败：${friendly(it)}" },
      )
    }
    fun unpaid(): TicketOrderRow? = picked?.takeIf { it.payStatus == "unpaid" && it.status != "cancelled" }
      ?: run { status.text = "请先选中一张待支付的订单"; null }
    val cancel = button("取消订单") {
      val order = unpaid() ?: return@button
      confirmThen("取消订单", "确认取消订单 ${order.orderNo ?: order.id}？取消后不能再收款出票。") {
        runBackground(
          action = { api.asStaff(staff.accessToken) { api.cancelTicketOrder(order.id, TicketOrderActionRequest(java.util.UUID.randomUUID().toString())) } },
          onSuccess = { status.text = "订单已取消"; search() },
          onError = { status.text = "取消失败：${friendly(it)}" },
        )
      }
    }
    setContentView(screen(title = "订单查询", children = listOf(status, phone, button("查询") { search() }, orders,
      button("继续收款") { unpaid()?.let { kioskTicketPay(staff, it) } }, cancel,
      button("返回售票") { showKioskTicketSale(staff) }, button("结束售票") { endKioskTicketing(staff) })))
  }

  /** 120-R2：订单一行——订单号、张数、金额、掩码手机号、支付状态、出票状态（中文）。 */
  private fun kioskOrderText(row: TicketOrderRow): String {
    val pay = when (row.payStatus) { "unpaid" -> "待支付"; "paid" -> "已支付"; else -> row.payStatus ?: "-" }
    val issue = when (row.issueStatus) { "issued" -> "已出票"; "not_issued" -> "未出票"; else -> row.issueStatus ?: "-" }
    return "订单 ${row.orderNo ?: row.id}　${row.quantity} 张　¥${yuan(row.amountCent)}　${row.buyerPhone ?: ""}\n" +
      "支付：$pay　出票：$issue${if (row.status == "cancelled") "（已取消）" else ""}"
  }

  // ---------------------------------------------------------------------------
  // 屏 ⑤ 游乐核销 -> amusementVerifyOnline（走票务核销入口 verify）
  // 第十八批 Q 段第 146 条 146-R1（BSM:552「工作手机扫客人二维码核销各酒吧入场、赠饮、游乐等场内权益」；BSM:766「票里的游乐权益到项目入口核销。」）：
  // 项目由入口点位或设备定位、场次取当前开放场次，员工不选项目与场次；扫码即提交，手输后点「游乐核销」兜底。
  // ---------------------------------------------------------------------------
  private fun showAmusementVerify() {
    val credential = editText("游乐凭证（扫码或手动输入）")
    val status = statusText("游乐入口在线核销")

    fun submit(raw: String) {
      val code = raw.trim()
      if (code.isEmpty()) { status.text = "请输入游乐凭证"; return }
      requireBindingOr(status) {
        status.text = "正在核销…"
        runBackground(
          action = { workflow.amusementVerifyOnline(code) },
          // PGM:287：只显示中文状态与核销号，不显示 result_reason 机器码。
          onSuccess = { r -> status.text = "核销结果：${verifyStatusZh(r.status)}\n核销号：${r.verificationNo}" },
          onError = { status.text = "游乐核销失败：${friendly(it)}" },
        )
      }
    }

    // 给了 onResult 时扫码结果只交回调、不回填输入框（launchScan），这里回填后直接提交。
    val scanButton = button("扫码核销") { launchScan(credential) { code -> credential.setText(code); submit(code) } }
    val verifyButton = button("游乐核销") { submit(credential.text.toString()) }

    setContentView(
      screen(
        title = "游乐核销",
        children = listOf(status, credential, scanButton, verifyButton, backButton()),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ⑥ 酒吧点酒下单 + 支付引导
  // createBarOrder -> showPosCheckout：生成账单 -> 支付意图（引导扫码支付）-> 带意图编号结算（18P 137-R1）
  // ---------------------------------------------------------------------------
  private fun showBarOrder() {
    val barId = editText("酒吧")
    val tableId = editText("桌台（可选）")
    val skuId = editText("酒水编号 / 商品编号")
    val quantity = editText("数量").apply { inputType = InputType.TYPE_CLASS_NUMBER; setText("1") }
    val status = statusText("点酒下单 -> 生成账单后创建支付意图 -> 结算")

    val orderButton = button("提交点酒下单") {
      val bar = barId.text.toString().trim()
      val sku = skuId.text.toString().trim()
      if (bar.isEmpty() || sku.isEmpty()) { status.text = "请填写酒吧与酒水 SKU"; return@button }
      val qty = quantity.text.toString().trim().ifEmpty { "1" }
      status.text = "正在下单…"
      runBackground(
        action = {
          workflow.createBarOrder(
            barId = bar,
            items = listOf(mapOf("skuId" to sku, "quantity" to qty)),
            tableId = tableId.text.toString().trim().ifEmpty { null },
          )
        },
        onSuccess = { o -> showPosCheckout(o) },
        onError = { status.text = "下单失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(
        title = "酒吧点酒下单",
        children = listOf(status, barId, tableId, skuId, quantity, orderButton, backButton()),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ⑦ 游乐检修提交 + 主管确认恢复
  // submitAmusementMaintenance -> confirmAmusementMaintenanceResume
  // 第十八批 N 段第 120 条 120-R6（BSM:992「工程师可在授权后填写游乐设施定期检修记录，检修单位作为供应商记录。」；BSM:993「检修完成后必须由
  // 主管确认，设施才能恢复营业。」；BSM:1269「工程师可在一体机上填写定期检修记录，记录检修单位、检修人员、结果和异常原因。」；BSM:1270）：
  // 一体机上工程师、主管各自当场授权登录（[staffSignInDialog]，提交或离开即登出），工作手机直接用本人会话；项目、设备、检修单位从列表选，
  // 主管从待确认的检修记录里选一条二次确认，确认人由服务端取当前登录主管。
  // ---------------------------------------------------------------------------
  private fun showMaintenance() {
    val kiosk = api.terminalType == "kiosk"
    fun enter(title: String, next: (LoginResponse?) -> Unit) = if (kiosk) staffSignInDialog(title) { kioskStaff = it; next(it) } else next(null)
    setContentView(screen(title = "游乐检修 / 主管确认", children = listOf(
      statusText(if (kiosk) "工程师填写检修、主管确认恢复营业，各自当场授权登录" else "先提交检修记录，主管再确认恢复营业"),
      button("工程师填写检修") { enter("工程师授权登录") { showMaintenanceSubmit(it) } },
      button("主管确认恢复营业") { enter("主管授权登录") { showMaintenanceConfirm(it) } },
      backButton(),
    )))
  }

  /** 120-R6：一体机上这段请求带授权员工令牌（[asStaff]）；工作手机（[staff] 为空）用本人会话。 */
  private fun <T> asMaintainer(staff: LoginResponse?, block: () -> T): T =
    if (staff == null) block() else api.asStaff(staff.accessToken, block)

  /** 120-R6：做完或离开检修屏，一体机上的授权员工即登出（工作手机不动）。 */
  private fun leaveMaintenance(staff: LoginResponse?, message: String? = null) {
    staff?.let { staffSignOut(it) }
    message?.let { android.widget.Toast.makeText(this, it, android.widget.Toast.LENGTH_LONG).show() }
    showMaintenance()
  }

  private fun showMaintenanceSubmit(staff: LoginResponse?) {
    var projectId: String? = null
    var deviceCode: String? = null
    var vendorId: String? = null
    val status = statusText("正在读取游乐项目与检修单位…")
    val projects = rowsContainer()
    val devices = rowsContainer()
    val vendors = rowsContainer()
    val engineerName = editText("检修人员")
    val result = enumSpinner(listOf("通过", "不通过"))
    val note = editText("异常原因（不通过必填）")
    runBackground(
      action = { asMaintainer(staff) { api.listAmusementProjects() to api.listInventorySuppliers().filter { it.status == "active" } } },
      onSuccess = { (projectRows, vendorRows) ->
        status.text = "请选择项目、设备与检修单位"
        fillSelectableRows(projects, projectRows.map { p ->
          (p.projectName ?: p.id) to {
            projectId = p.id
            deviceCode = null
            runBackground(
              action = { asMaintainer(staff) { api.listAmusementDevices(p.id) } },
              onSuccess = { rows ->
                fillSelectableRows(devices, rows.map { d -> "${d.name ?: d.deviceCode}（${d.deviceCode}）" to { deviceCode = d.deviceCode } }, "该项目没有登记设备")
              },
              onError = { status.text = "设备读取失败：${friendly(it)}" },
            )
          }
        }, "暂无游乐项目")
        fillSelectableRows(vendors, vendorRows.map { v -> (v.supplierName ?: v.id) to { vendorId = v.id } }, "暂无启用的检修单位")
      },
      onError = { status.text = "读取失败：${friendly(it)}" },
    )
    val submit = button("提交检修记录") {
      val pid = projectId
      val did = deviceCode
      val vid = vendorId
      val name = engineerName.text.toString().trim()
      val pass = result.selectedItemPosition == 0
      val reason = note.text.toString().trim().ifEmpty { null }
      if (pid == null || did == null || vid == null) { status.text = "请选择项目、设备与检修单位"; return@button }
      if (name.isEmpty()) { status.text = "请填写检修人员"; return@button }
      if (!pass && reason == null) { status.text = "检修不通过须填写异常原因"; return@button }
      status.text = "正在提交…"
      runBackground(
        action = {
          asMaintainer(staff) {
            workflow.submitAmusementMaintenance(
              projectId = pid, deviceId = did, maintenanceVendorId = vid, engineerName = name,
              result = if (pass) "pass" else "fail", exceptionNote = reason,
              engineerId = staff?.user?.id, locationPointId = staff?.let { kioskPrefs.getString("point_id", null) },
            )
          }
        },
        onSuccess = { rec ->
          val done = "检修记录已提交（${maintStatusZh(rec.status)}）" + if (pass) "，请主管确认恢复营业" else ""
          leaveMaintenance(staff, done)
        },
        onError = { status.text = "提交失败：${friendly(it)}" },
      )
    }
    setContentView(screen(title = "工程师填写检修", children = listOf(status, fieldLabel("项目"), projects, fieldLabel("设备"), devices,
      fieldLabel("检修单位"), vendors, engineerName, fieldLabel("检修结果"), result, note, submit,
      button("返回") { leaveMaintenance(staff) })))
  }

  private fun showMaintenanceConfirm(staff: LoginResponse?) {
    val status = statusText("正在读取待主管确认的检修记录…")
    val records = rowsContainer()
    runBackground(
      action = { asMaintainer(staff) { api.listAmusementMaintenanceRecords().filter { it.status == "pending_confirm" } } },
      onSuccess = { rows ->
        status.text = "选一条检修记录确认恢复营业"
        fillSelectableRows(records, rows.map { r ->
          "项目 ${r.projectId ?: "-"}　设备 ${r.deviceId ?: "-"}\n检修人员 ${r.engineerName ?: "-"}　${r.maintenanceAt?.let { traceTime(it) } ?: ""}" to {
            confirmThen(
              title = "主管确认恢复营业",
              message = "确认后设施立即恢复营业，责任落在确认主管，不可撤销。\n请确认检修已完成且设施安全。",
            ) {
              status.text = "正在确认…"
              runBackground(
                action = { asMaintainer(staff) { workflow.confirmAmusementMaintenanceResume(r.id) } },
                onSuccess = { rec -> leaveMaintenance(staff, "已确认恢复营业（${maintStatusZh(rec.status)}）") },
                onError = { status.text = "确认失败：${friendly(it)}" },
              )
            }
          }
        }, "没有待主管确认的检修记录")
      },
      onError = { status.text = "读取失败：${friendly(it)}" },
    )
    setContentView(screen(title = "主管确认恢复营业", children = listOf(status, records, button("返回") { leaveMaintenance(staff) })))
  }

  // ---------------------------------------------------------------------------
  // 员工设备凭证：devicePublicKey / deviceFingerprintHash 取自 AndroidKeyStore
  // （EC P-256，私钥硬件保护不可导出），见 DeviceKeystore；后端拒 PLACEHOLDER_* 占位。
  // ---------------------------------------------------------------------------
  private fun showStaffDevice() {
    val profileId = editText("员工档案")
    val status = statusText(
      "设备公钥/指纹哈希取自 Android Keystore（EC P-256，私钥不可导出），首次绑定自动生成。",
    )

    val bindButton = button("绑定设备凭证") {
      val pid = profileId.text.toString().trim()
      if (pid.isEmpty()) { status.text = "请输入员工档案"; return@button }
      status.text = "正在绑定凭证…"
      val fingerprint = DeviceKeystore.deviceFingerprintHash()
      runBackground(
        action = {
          workflow.bindStaffDeviceCredential(
            staffProfileId = pid,
            devicePublicKey = DeviceKeystore.devicePublicKeyBase64(),
            deviceFingerprintHash = fingerprint,
          )
        },
        onSuccess = { rec ->
          // 第十八批 M 段第 108 条 108-R3（PGM:585「员工换手机必须先解绑旧手机，再绑定新手机。」）：同自助注册成功分支，
          // 本机以凭证指纹作设备编号、按私人手机声明身份并签名（换机审批通过后生效）。
          // 第 116 条 116-R3：只在本机还没声明身份时采用私人手机身份，工作手机、平板不改；回首页即按私人手机重取功能清单。
          if (api.terminalType == null) {
            getSharedPreferences("private_phone", MODE_PRIVATE).edit().putString("device_id", fingerprint).apply()
            api.deviceId = fingerprint
            api.terminalType = STAFF_PRIVATE_PHONE
          }
          status.text = "凭证绑定：${rec.id}（状态 ${statusZh(rec.bindStatus)}）"
        },
        onError = { status.text = "绑定失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(title = "员工设备凭证", children = listOf(status, profileId, bindButton, backButton())),
    )
  }

  // 票务核销冲正
  private fun showReverse() {
    val verificationId = editText("核销记录")
    val reason = editText("冲正原因")
    val status = statusText("对已核销记录冲正（撤销）")

    val reverseButton = button("提交冲正") {
      val vid = verificationId.text.toString().trim()
      val r = reason.text.toString().trim()
      if (vid.isEmpty() || r.isEmpty()) { status.text = "请填写记录与原因"; return@button }
      confirmThen(
        title = "核销冲正",
        message = "将冲正核销记录 $vid（原因：$r）。\n冲正即撤销原核销结果，不可恢复。",
      ) {
        status.text = "正在冲正…"
        runBackground(
          action = { workflow.reverseVerification(vid, r) },
          onSuccess = { res -> status.text = "冲正完成：${res.id}（状态 ${verifyStatusZh(res.status)}）" },
          onError = { status.text = "冲正失败：${friendly(it)}" },
        )
      }
    }

    setContentView(
      screen(title = "票务核销冲正", children = listOf(status, verificationId, reason, reverseButton, backButton())),
    )
  }

  private fun logoutAndRestart() {
    ChargingReceiptBridge.clearAnchor(this)
    store.clearSensitiveData()
    api.accessToken = null
    kioskConfig = null
    showLogin()
  }

  // ---------------------------------------------------------------------------
  // 第十八批 I 段第 84 条 84-R2 酒水与物品交接扫码（BSM:1891 工作手机「库房和酒水交接扫码」；PGM:633「记录出库、后厨接收、服务员取走和送达」；
  // BSM:1201「每一步都必须扫码」「扫码缺失的可事后人工补录，但一律须附证据（照片、录音、录像等）并经主管复核后方生效」）。
  // 扫溯源码查进行中的交接单：没有就由库管先选该物品所属的待出品酒吧订单（第十八批 K 段第 94 条 94-R2，BSM:1197「出库须同时核验订单和物品码」），
  // 再建单并确认出库；有就只给下一个人工段一个确认按钮。每次扫码只确认一段——确认成功回到扫码页，
  // 下一段须重新扫码才出现按钮。机器人配送段被动留痕、不在手机上扫。
  // ---------------------------------------------------------------------------

  /** 幂等键按「交接单＋段」：失败重试沿用同一个键，成功后才换，网络重试不会变成重复扫码。 */
  private val traceKeys = java.util.concurrent.ConcurrentHashMap<String, String>()
  private var evidencePickResult: ((List<android.net.Uri>) -> Unit)? = null

  private fun traceKey(scope: String): String = traceKeys.getOrPut(scope) { java.util.UUID.randomUUID().toString() }

  private fun showTraceHandover(message: String = "扫酒水或物品上的平台二维码，逐段确认交接", chain: TraceHandoverChain? = null) {
    val status = statusText(message)
    val chainView = statusText("").apply { visibility = View.GONE }
    val actions = rowsContainer()
    val scan = button("扫描溯源码") {
      launchScan(null, "对准酒水或物品上的平台二维码") { code -> lookupTrace(code.trim(), status, chainView, actions) }
    }
    setContentView(screen(title = "酒水与物品交接扫码", children = listOf(status, scan, chainView, actions, backButton())))
    if (chain != null) {
      renderTrace(chain.handover.traceCode ?: "", chain, status, chainView, actions)
      status.text = message + "\n" + status.text
    }
  }

  private fun lookupTrace(code: String, status: TextView, chainView: TextView, actions: LinearLayout) {
    actions.removeAllViews()
    chainView.visibility = View.GONE
    if (code.isEmpty()) { status.text = "未读取到二维码，请重扫"; return }
    status.text = "正在查询交接单…"
    runBackground(
      action = { activeChain(api.listTraceHandovers(code))?.let { api.getTraceHandover(it.id) } },
      onSuccess = { chain -> renderTrace(code, chain, status, chainView, actions) },
      onError = { status.text = "查询失败：${friendly(it)}" },
    )
  }

  private fun renderTrace(code: String, chain: TraceHandoverChain?, status: TextView, chainView: TextView, actions: LinearLayout) {
    actions.removeAllViews()
    if (chain == null) {
      chainView.visibility = View.GONE
      status.text = "正在查询待出品的酒吧订单…"
      runBackground(
        action = { api.listOutboundOrders(code) },
        onSuccess = { orders ->
          status.text = if (orders.isEmpty()) "没有待出品的酒吧订单含此物品，不能出库" else "溯源码 $code 没有进行中的交接单，请选出库给哪张订单"
          orders.forEach { order ->
            val label = "出库给：${order.tableName ?: "—"} ${order.createdAt?.let { traceTime(it) } ?: ""} ${order.productName ?: ""}×${order.quantity}"
            actions.addView(button(label) {
              val device = api.deviceId
              if (device == null) { status.text = "请先绑定设备"; return@button }
              val createScope = "create:$code:${order.orderId}"
              val createKey = traceKey(createScope)
              status.text = "正在建交接单并确认出库…"
              runBackground(
                action = {
                  val id = api.createTraceHandover(TraceHandoverCreateRequest(createKey, code, order.orderId, device)).handover.id
                  api.scanTraceSegment(id, TraceScanSegmentRequest(traceKey("$id:out_verify"), "out_verify"))
                },
                onSuccess = { done ->
                  traceKeys.remove(createScope)
                  traceKeys.remove("${done.handover.id}:out_verify")
                  showTraceHandover("已确认：库管出库。下一段请重新扫码")
                },
                onError = { status.text = "出库确认失败：${friendly(it)}" },
              )
            })
          }
        },
        onError = { status.text = "查询订单失败：${friendly(it)}" },
      )
      return
    }
    val handover = chain.handover
    chainView.visibility = View.VISIBLE
    chainView.text = buildString {
      append("溯源码：").append(handover.traceCode ?: code)
      append("\n整链状态：").append(handover.overallStatus?.let { TRACE_OVERALL_STATUS[it] } ?: "—")
      TRACE_STAFF_SEGMENTS.forEach { (segment, name) ->
        val step = chain.steps.firstOrNull { it.segment == segment }
        append("\n\n").append(name).append("：").append(step?.stepStatus?.let { TRACE_STEP_STATUS[it] } ?: "—")
        step?.operatorUserId?.let { append("　扫码人 ").append(it) }
        step?.scanAt?.let { append("　").append(traceTime(it)) }
        chain.backfills.filter { it.targetPart == segment }.forEach { row ->
          append("\n　人工补录·").append(row.status?.let { TRACE_BACKFILL_STATUS[it] } ?: "—")
          row.occurredAt?.let { append("　原时点 ").append(traceTime(it)) }
          row.responsibleStaffId?.let { append("　责任人 ").append(it) }
        }
      }
    }
    val next = nextStaffSegment(chain)
    status.text = if (next == null) "四个人工段都已完成" else "本次扫码可确认：${TRACE_STAFF_SEGMENTS[next]}"
    if (next != null) {
      val name = TRACE_STAFF_SEGMENTS.getValue(next)
      actions.addView(button("确认：$name") {
        val scope = "${handover.id}:$next"
        val key = traceKey(scope)
        status.text = "正在确认：$name…"
        runBackground(
          action = { api.scanTraceSegment(handover.id, TraceScanSegmentRequest(key, next)) },
          onSuccess = { traceKeys.remove(scope); showTraceHandover("已确认：$name。下一段请重新扫码") },
          onError = { status.text = "确认失败：${friendly(it)}" },
        )
      })
    }
    TRACE_STAFF_SEGMENTS.forEach { (segment, name) ->
      if (canBackfill(chain, segment)) actions.addView(button("人工补录：$name") { showTraceBackfill(chain, segment) })
    }
  }

  /** 缺段人工补录：原时点、原因、责任人账号（默认本人）、证据（照片、录音、录像，至少一份）；提交后进主管复核，通过才算该段完成。 */
  private fun showTraceBackfill(chain: TraceHandoverChain, segment: String) {
    val id = chain.handover.id
    val name = TRACE_STAFF_SEGMENTS.getValue(segment)
    val status = statusText("补录「$name」：须附照片、录音或录像证据，经主管复核通过后才算该段完成")
    val date = editText("原时点日期（如 2026-10-29）").apply { setText(java.time.LocalDate.now().toString()) }
    val time = editText("原时点时刻（如 20:30）")
    val reason = editText("补录原因")
    val responsible = editText("责任人账号").apply { setText(store.session?.staffId ?: "") }
    val proofs = mutableListOf<String>()
    val proofText = statusText("尚未上传证据")
    val pick = button("选择证据（照片、录音、录像）") {
      evidencePickResult = { uris ->
        if (uris.isEmpty()) {
          status.text = "未选择证据"
        } else {
          val start = proofs.size
          status.text = "正在上传证据…"
          runBackground(
            action = {
              uris.mapIndexed { index, uri ->
                val ext = evidenceExtension(uri)
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("读不到所选文件")
                api.uploadEvidence("evidence_${start + index + 1}" + (if (ext.isEmpty()) "" else ".$ext"), bytes).id
              }
            },
            onSuccess = { ids -> proofs += ids; proofText.text = "已上传证据 ${proofs.size} 份"; status.text = "证据已上传" },
            onError = { status.text = "证据上传失败：${friendly(it)}" },
          )
        }
      }
      startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "audio/*", "video/*"))
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      }, TRACE_EVIDENCE_REQUEST)
    }
    val submit = button("提交补录（进主管复核）") {
      val occurred = runCatching {
        java.time.LocalDateTime.parse("${date.text.toString().trim()}T${time.text.toString().trim()}")
          .atZone(java.time.ZoneId.systemDefault()).toInstant().toString()
      }.getOrNull()
      val why = reason.text.toString().trim()
      val who = responsible.text.toString().trim()
      if (occurred == null) { status.text = "请填写原时点日期和时刻（如 2026-10-29 和 20:30）"; return@button }
      if (why.isEmpty()) { status.text = "请填写补录原因"; return@button }
      if (who.isEmpty()) { status.text = "请填写责任人账号"; return@button }
      if (proofs.isEmpty()) { status.text = "须先上传至少一份证据（照片、录音或录像）"; return@button }
      val scope = "$id:$segment:backfill"
      val key = traceKey(scope)
      status.text = "正在提交补录…"
      runBackground(
        action = {
          api.submitTraceStepBackfill(id, segment, TraceStepBackfillRequest(key, occurred, why, who, proofs.toList()))
          api.getTraceHandover(id)
        },
        onSuccess = { refreshed -> traceKeys.remove(scope); showTraceHandover("已提交「$name」补录，待主管复核", refreshed) },
        onError = { status.text = "补录提交失败：${friendly(it)}" },
      )
    }
    setContentView(screen(title = "交接人工补录", children = listOf(status, date, time, reason, responsible, pick, proofText, submit,
      button("返回交接扫码") { showTraceHandover() })))
  }

  /** 证据扩展名：取所选文件显示名的扩展名，没有时按内容类型反查（服务端按扩展名判定照片、录音、录像）；只留 ASCII 小写字母数字，签名规范串里的文件名与服务端一致。 */
  private fun evidenceExtension(uri: android.net.Uri): String {
    val name = contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
      ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    val ext = name?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.isNotBlank() }
      ?: contentResolver.getType(uri)?.let { android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
      ?: ""
    return ext.takeIf { value -> value.all { it in 'a'..'z' || it in '0'..'9' } } ?: ""
  }

  private fun traceTime(iso: String): String = runCatching {
    java.time.Instant.parse(iso).atZone(java.time.ZoneId.systemDefault())
      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
  }.getOrDefault(iso)

  // ---------------------------------------------------------------------------
  // 通用辅助
  // ---------------------------------------------------------------------------

  /** 后台线程执行网络业务，回到 UI 线程回调。 */
  /** 第十八批 N 段第 123 条 123-R4（BSM:1630「工作手机、平板、一体机绑定点位后，系统自动知道“在哪”」）：本机绑定为工作手机、平板或一体机后
   *  申请定位权限，核销、收款、退款、放行那一刻取一次位置随请求带上（[DeviceLocator]）；取不到时，所属项目已配授权范围的由服务端拒绝、
   *  只能查看（第十八批裁决 30）。工作手机由企业管控统一打开定位权限，不在本端。 */
  private fun requestLocationPermission() {
    val fine = android.Manifest.permission.ACCESS_FINE_LOCATION
    val coarse = android.Manifest.permission.ACCESS_COARSE_LOCATION
    if (listOf(fine, coarse).none { checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
      requestPermissions(arrayOf(fine, coarse), LOCATION_PERMISSION_REQUEST)
    }
  }

  private fun <T> runBackground(action: () -> T, onSuccess: (T) -> Unit, onError: (Throwable) -> Unit) {
    thread {
      val outcome = runCatching(action)
      runOnUiThread {
        outcome
          .onSuccess { onSuccess(it) }
          .onFailure { onError(it) }
      }
    }
  }

  /** 高危操作二次确认（照后台 HighRiskConfirm 语义）：标题=动作，内容=影响+不可逆说明+关键参数回显。 */
  private fun confirmThen(title: String, message: String, onConfirm: () -> Unit) {
    android.app.AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton("确定") { _, _ -> onConfirm() }
      .setNegativeButton("取消", null)
      .show()
  }

  private fun requireBindingOr(status: TextView, block: () -> Unit) {
    if (store.session == null) { status.text = "请先登录"; return }
    if (store.binding == null) { status.text = "请先绑定设备"; return }
    block()
  }

  /** 错误信息转中文友好提示，区分离线/网络/接口错误。 */
  private fun friendly(error: Throwable): String {
    // 原始异常记入日志便于报障排查，不吞掉；用户面只给可读中文。
    android.util.Log.w("ComplexTerminal", "operation failed", error)
    return when (error) {
    is ApiException -> apiErrorText(error)
    is javax.net.ssl.SSLException -> "无法安全连接到服务器，请检查网络或证书配置"
    is java.net.SocketTimeoutException -> "连接超时，请稍后重试"
    is java.io.IOException -> "无法连接到服务器，请检查网络后重试"
    is IllegalStateException -> error.message?.let {
      when {
        it.contains("offline") -> "网络离线，请检查连接后重试"
        it.contains("session") -> "请先登录"
        it.contains("binding") -> "请先绑定设备"
        else -> it
      }
    } ?: "操作失败"
    else -> error.message ?: error.javaClass.simpleName
    }
  }

  // ---------- 视图工厂（纯 Android Views，无 AndroidX/Compose） ----------

  private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

  // ---------------------------------------------------------------------------
  // BYOD 一期(2026-07-02 拍板):员工自有手机——公告 / 我的工单 / 酒吧空位代订 / 注册申请。
  // 能力分级:公司设备全功能、自有手机只开轻功能,由后台按设备类型下发(能力开关)。
  // ---------------------------------------------------------------------------
  private fun showStaffNotices() {
    val status = statusText("正在拉取公司公告…")
    val list = rowsContainer()
    val ackId = editText("要确认已读的公告（点上方公告自动回填）")
    val refresh = {
      runBackground(
        action = { api.listStaffNotices() },
        onSuccess = { notices ->
          status.text = "公告 ${notices.size} 条（点一条自动回填）"
          fillSelectableRows(
            list,
            notices.map { n ->
              "[${statusZh(n.consentStatus)}] ${n.title ?: n.id}\n${n.body ?: ""}" to {
                ackId.setText(n.id ?: "")
                status.text = "已选公告：${n.title ?: n.id}"
              }
            },
            emptyText = "暂无公告",
          )
        },
        onError = { status.text = "拉取失败：${friendly(it)}" },
      )
    }
    val ackButton = button("确认已读") {
      val id = ackId.text.toString().trim()
      if (id.isEmpty()) { status.text = "请输入公告"; return@button }
      runBackground(
        action = { api.acknowledgeStaffNotice(id, NoticeAcknowledgeRequest("byod_ack_${System.currentTimeMillis()}")) },
        onSuccess = { status.text = "已确认已读：${it.id ?: id}"; refresh() },
        onError = { status.text = "确认失败：${friendly(it)}" },
      )
    }
    refresh()
    setContentView(screen(
      title = "公司公告",
      children = listOf(status, list, ackId, ackButton, backButton()),
    ))
  }

  /**
   * 我的工单：派给我的只读（处理在公司设备）；第十二批操作员裁决第 13 条：一并列出「待我提单确认」的单，
   * 点选后可「确认」（/review）或填不通过原因「退回返工」（/rework）。
   * 第十八批 M 段第 111 条 111-R1：物业工单之后追加派给我的商户服务工单（只读，点选不回填）；两份各自拉取，一份失败只在状态行注明。
   */
  private fun showMyWorkOrders() {
    val status = statusText("正在拉取流转到我的工单…")
    val list = rowsContainer()
    val orderId = editText("待我提单确认的工单（点上方「待我提单确认」一条自动回填）")
    val reason = editText("退回返工须填不通过原因")
    val me = store.session?.staffId
    val refresh = {
      runBackground(
        action = { runCatching { api.listMyWorkOrders() } to runCatching { api.listMyMerchantWorkOrders() } },
        onSuccess = { (facility, merchant) ->
          val orders = facility.getOrDefault(emptyList())
          val merchantOrders = merchant.getOrDefault(emptyList())
          status.text = listOfNotNull(
            "我的工单 ${orders.size + merchantOrders.size} 条（派给我的只读，处理在公司设备；待我提单确认的可确认或退回返工）",
            facility.exceptionOrNull()?.let { "物业工单拉取失败：${friendly(it)}" },
            merchant.exceptionOrNull()?.let { "商户工单拉取失败：${friendly(it)}" },
          ).joinToString("\n")
          fillSelectableRows(
            list,
            orders.map { o ->
              val pending = o.awaitsConfirmationBy(me)
              "${if (pending) "[待我提单确认] " else ""}${o.code ?: o.id} [${o.status ?: "-"}] ${o.title ?: ""}\n优先级 ${o.priority ?: "-"} / 处理时限 ${o.handleDueAt ?: "-"}" to {
                if (pending) { orderId.setText(o.id ?: ""); status.text = "已选待我提单确认：${o.code ?: o.id}" }
              }
            } + merchantOrders.map { m ->
              "[商户工单] ${m.title ?: m.id} [${merchantWorkOrderStatusZh(m.status)}]\n${merchantWorkOrderTypeZh(m.workOrderType)}/${m.priority ?: "-"} / 处理时限 ${m.dueAt ?: "-"}" to {}
            },
            emptyText = "暂无流转到你的工单",
          )
        },
        onError = { status.text = "拉取失败：${friendly(it)}" },
      )
    }
    val confirm = button("确认") {
      val id = orderId.text.toString().trim()
      if (id.isEmpty()) { status.text = "请先选择待我提单确认的工单"; return@button }
      runBackground(
        action = { api.confirmWorkOrder(id, WorkOrderConfirmationRequest("byod_wo_confirm_${System.currentTimeMillis()}")) },
        onSuccess = { status.text = "已确认：${it.code ?: id}"; orderId.setText(""); refresh() },
        onError = { status.text = "确认失败：${friendly(it)}" },
      )
    }
    val rework = button("退回返工") {
      val id = orderId.text.toString().trim()
      val note = reason.text.toString().trim()
      if (id.isEmpty()) { status.text = "请先选择待我提单确认的工单"; return@button }
      if (note.isEmpty()) { status.text = "退回返工须填不通过原因"; return@button }
      runBackground(
        action = { api.reworkWorkOrder(id, WorkOrderConfirmationRequest("byod_wo_rework_${System.currentTimeMillis()}", note)) },
        onSuccess = { status.text = "已退回返工：${it.code ?: id}"; orderId.setText(""); reason.setText(""); refresh() },
        onError = { status.text = "退回失败：${friendly(it)}" },
      )
    }
    refresh()
    setContentView(screen(
      title = "我的工单",
      children = listOf(status, list, orderId, reason, confirm, rework, backButton()),
    ))
  }

  /** HP-F01（BSM「楼宇运营」告警条）：当班接警人须点「已收到并接手」才算明确接手；查看本屏、公告确认已读、接听语音都不算。 */
  private fun showMyFacilityAlarms() {
    val status = statusText("正在拉取我的接警…")
    val list = rowsContainer()
    val alarmId = editText("要接手的告警（点上方待接手告警自动回填）")
    val refresh = {
      runBackground(
        action = { api.listMyFacilityAlarms() },
        onSuccess = { alarms ->
          status.text = "我的接警 ${alarms.size} 条（点一条自动回填）"
          fillSelectableRows(
            list,
            alarms.map { a ->
              val st = if (a.status == "awaiting_takeover") "待明确接手" else "已明确接手"
              "[$st] 设备 ${a.equipmentId ?: "-"} 优先级 ${a.priority ?: "-"}\n响应时限 ${a.responseDueAt ?: "-"}" +
                (if (a.uncovered == true) "（无已确认处置规则，待专业处置人员判定）" else "") to {
                alarmId.setText(a.id ?: "")
                status.text = "已选告警：${a.id}"
              }
            },
            emptyText = "暂无待接手或我持有的告警",
          )
        },
        onError = { status.text = "拉取失败：${friendly(it)}" },
      )
    }
    val takeOver = button("已收到并接手") {
      val id = alarmId.text.toString().trim()
      if (id.isEmpty()) { status.text = "请先选择告警"; return@button }
      runBackground(
        action = { api.takeOverFacilityAlarm(id, FacilityAlarmTakeOverRequest("byod_alarm_take_${System.currentTimeMillis()}")) },
        onSuccess = { status.text = "已明确接手：${it.id ?: id}，请按原派单流程派维修"; refresh() },
        onError = { status.text = "接手失败：${friendly(it)}" },
      )
    }
    refresh()
    setContentView(screen(
      title = "我的接警",
      children = listOf(status, list, alarmId, takeOver, backButton()),
    ))
  }

  private fun showBarAvailability() {
    val status = statusText("正在拉取酒吧桌位…")
    val list = rowsContainer()
    val barId = editText("酒吧(点上方桌位自动回填)")
    val tableId = editText("桌位(点上方空位自动回填)")
    val time = editText("到店时间(如 2026-07-02T20:00:00Z)")
    val latest = editText("最晚到店时间(如 2026-07-02T22:00:00Z)")
    val phone = editText("客人手机号(必填,核对会员是否受限)").apply { inputType = InputType.TYPE_CLASS_PHONE }
    val guests = editText("人数").apply { inputType = InputType.TYPE_CLASS_NUMBER }
    runBackground(
      action = { api.listBarTableAvailability() },
      onSuccess = { rows ->
        status.text = "桌位 ${rows.size} 个(员工代订免定金,收款在公司设备/前台;点一条自动回填)"
        fillSelectableRows(
          list,
          rows.map { r ->
            "${if (r.reserved == true) "[已订]" else "[空]"} ${r.tableCode ?: r.id} ${r.tableName ?: ""} " +
              "${tableTypeZh(r.tableType)} ${r.capacity ?: 0}人 低消 ¥${"%.2f".format((r.minConsumptionCent ?: 0) / 100.0)}" to {
              tableId.setText(r.id ?: "")
              r.barId?.let { barId.setText(it) }
              status.text = "已选桌位：${r.tableCode ?: r.id}"
            }
          },
          emptyText = "暂无桌位",
        )
      },
      onError = { status.text = "拉取失败：${friendly(it)}" },
    )
    val reserveButton = button("帮客人订位(免定金)") {
      val b = barId.text.toString().trim()
      val t = tableId.text.toString().trim()
      val at = time.text.toString().trim()
      val la = latest.text.toString().trim()
      val ph = phone.text.toString().trim()
      val g = guests.text.toString().trim().toIntOrNull()
      if (b.isEmpty() || t.isEmpty() || at.isEmpty() || la.isEmpty() || ph.isEmpty() || g == null || g <= 0) {
        status.text = "请填全 酒吧/桌位/时间/最晚到店时间/客人手机号/人数"
        return@button
      }
      runBackground(
        action = {
          api.createBarReservation(BarReservationCreateRequest(
            idempotencyKey = "byod_resv_${System.currentTimeMillis()}",
            barId = b, tableId = t, reservationTime = at, guestCount = g,
            salespersonUserId = store.session?.staffId,
            latestArrivalAt = la, customerPhone = ph,
          ))
        },
        onSuccess = { r ->
          status.text = "订位成功：${r.id ?: "-"}(定金 ${statusZh(r.depositStatus ?: "not_required")})"
          r.id?.let { reportPageView("酒吧空位 / 帮客订位", true, listOf(PageObjectRef("bar_reservation", it)), null) }
        },
        onError = { status.text = "订位失败：${friendly(it)}" },
      )
    }
    setContentView(screen(
      title = "酒吧空位 / 帮客订位",
      children = listOf(status, list, barId, tableId, time, latest, phone, guests, reserveButton,
        backButton()),
      sensitive = true,
    ))
  }

  // ── E批(2026-07-02 拍板):我的排班 / NFC打卡 / 主管排班 ──
  private fun showMyShifts() {
    val status = statusText("正在拉取我的班次…")
    val list = statusText("")
    runBackground(
      action = { api.listMyShifts() },
      onSuccess = { shifts ->
        status.text = "已发布班次 ${shifts.size} 个"
        list.text = if (shifts.isEmpty()) "暂无排班" else shifts.joinToString("\n\n") { s ->
          "${s.workDate}  ${s.startAt ?: "-"} ~ ${s.endAt ?: "-"}\n点位: " +
            (s.pointSpaceIds?.takeIf { it.isNotEmpty() }?.joinToString("、") ?: "未指定") +
            (s.nightDutyProjectId?.let { "\n夜班值班职责: 项目 $it" } ?: "") +
            (s.note?.let { "\n备注: $it" } ?: "")
        }
      },
      onError = { status.text = "拉取失败：${friendly(it)}" },
    )
    setContentView(screen(
      title = "我的排班(时间到分钟)",
      children = listOf(status, list, backButton()),
    ))
  }

  private fun showPunch() {
    val status = statusText("领用准备时先选择本班并校准时间。领取事件先保存在手机；未登录、断网或重启不丢失。缺少可靠时间依据的事件交由复核。")
    val shiftPicker = android.widget.Spinner(this)
    var shifts = emptyList<StaffShiftRow>()
    runBackground(action = { api.listMyShifts() }, onSuccess = {
      shifts = it
      shiftPicker.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
        it.map { row -> "${row.startAt} 至 ${row.endAt} ${row.note ?: ""}" })
    }, onError = { status.text = "班次读取失败：${friendly(it)}" })
    val calibrate = button("为所选班次校准时间") {
      val shiftId = shifts.getOrNull(shiftPicker.selectedItemPosition)?.id
      if (shiftId == null) { status.text = "请选择已发布班次"; return@button }
      val request = AttendanceAnchorRequest(java.util.UUID.randomUUID().toString(), shiftId,
        ChargingReceiptBridge.bootId(this), android.os.SystemClock.elapsedRealtime())
      runBackground(action = { api.attendanceAnchor(request) }, onSuccess = {
        ChargingReceiptBridge.rememberAnchor(this, it)
        status.text = "已关联本班并校准；后续领取事件按实际发生时刻核验"
      }, onError = { status.text = "校准未完成：${friendly(it)}" })
    }
    fun record(method: String, tag: String) {
      if (tag.isBlank()) { status.text = "未读取到点位标签，请重试"; return }
      recordWorkPhoneReceipt(method, tag, status)
    }
    nfcResult = { tag -> record("nfc", tag) }
    val scan = button("扫描领取点位二维码") {
      scanTarget = null
      scanResult = { tag -> record("qr", tag) }
      IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        .setPrompt("对准工作手机领取点位二维码").setBeepEnabled(true).initiateScan()
    }
    setContentView(screen(title = "工作手机考勤", children = listOf(status, shiftPicker, calibrate,
      statusText("NFC、扫码或离开充电设施均记录领取，以最早核实事件为准。下班由设备接收人办理实际归还；班中故障使用换机流程。"),
      scan, button("重试同步待传事件") { flushWorkPhoneReceipts(status) },
      button("查看服务器领取事件") {
        runBackground(action = { api.attendanceReceiptEvents() }, onSuccess = { rows ->
          status.text = rows.joinToString("\n") { row -> "${row.actualAt ?: row.observedAt ?: "-"} · ${if(row.status=="accepted") "已核实" else if(row.status=="rejected") "未采纳" else "待复核"}" }
        }, onError = { status.text = friendly(it) })
      },
      button("本机待传事件与主管代补报") { showPendingReceipts() },
      button("核时申请与审批记录") { showReceiptTimeReview() },
      button("接收本班归还／办理故障换机") { showWorkPhoneReturn() }, backButton())))
  }

  private fun showPendingReceipts() {
    val status = statusText(ChargingReceiptBridge.lastSyncMessage(this))
    val picker = android.widget.Spinner(this)
    var rows = ChargingReceiptBridge.pending(this)
    fun reload() {
      rows = ChargingReceiptBridge.pending(this)
      picker.adapter = android.widget.ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,
        rows.map { "${it.eventId} · 原员工${it.holderUserId ?: "待核"} · ${it.observedAt}" })
    }
    reload()
    val originalEmployee = editText("原领用员工编号（原事件无编号时须核对）")
    val historicalPicker = android.widget.Spinner(this)
    var historical = emptyList<WorkPhoneAttendanceContext>()
    historicalPicker.adapter = android.widget.ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("未查询原手机领用历史"))
    val lookup = button("查询本机该员工真实领用历史") {
      val employeeId = originalEmployee.text.toString().trim()
      if (employeeId.isBlank()) { status.text = "请据交接记录填写原员工编号，不能用当前登录人代替"; return@button }
      runBackground(action = { api.historicalWorkPhoneContexts(employeeId) },onSuccess = { found ->
        historical = found
        historicalPicker.adapter = android.widget.ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,
          listOf("请选择拟关联的原领用记录") + found.map { "${it.id} · ${it.userId} · ${it.bindingAt} 至 ${it.closedAt ?: "尚未归还"}" })
        status.text = if (found.isEmpty()) "本机没有该员工可关联的真实领用记录，原事件继续保留" else "仅为主管提出的关联，须补证并经真实上级核实"
      },onError = { historical = emptyList(); status.text = "领用历史查询失败：${friendly(it)}" })
    }
    val reason = editText("主管代补报原因")
    val evidence = editText("原手机领用及交接核实事实")
    val recover = button("主管代补报原事件（仍须核时审批）") {
      val event = rows.getOrNull(picker.selectedItemPosition)
      if (event == null) { status.text = "请选择本机保留的原事件"; return@button }
      val proposedContextId = if (event.contextId == null) historical.getOrNull(historicalPicker.selectedItemPosition - 1)?.id else null
      if (event.contextId == null && proposedContextId == null) { status.text = "原事件缺少领用编号；请查询真实原员工领用记录并明确选择，不能套用当前绑定"; return@button }
      if (reason.text.isBlank() || evidence.text.isBlank()) { status.text = "请填写代补报原因和原交接核实事实"; return@button }
      runBackground(action = { api.recoverAttendanceReceipt(AttendanceReceiptRecoveryRequest(
        event.request(api.deviceId ?: ""),reason.text.toString().trim(),evidence.text.toString().trim(),proposedContextId)) },onSuccess = { result ->
        if (result.id == event.eventId) { ChargingReceiptBridge.acknowledge(this,event.eventId); reload() }
        status.text = "原事件已由主管代补报，仍待真实上一级核时审批；未直接计入考勤。可在核时申请中按原员工查询。"
      },onError = { status.text = "代补报未完成，原事件保留：${friendly(it)}" })
    }
    setContentView(screen(title = "本机原领取事件",children = listOf(status,picker,
      button("原持有人重试同步") { flushWorkPhoneReceipts(status) },originalEmployee,lookup,historicalPicker,reason,evidence,recover,
      button("核时申请与审批记录") { showReceiptTimeReview() },backButton())))
  }

  private fun showReceiptTimeReview() {
    val status = statusText("查看本人或获授权员工的原领取事件。主管补证由其真实上一级审批，批准前保持待核，驳回不直接认定缺勤。")
    val employee = editText("员工编号（留空查看本人）")
    val picker = android.widget.Spinner(this)
    var events = emptyList<AttendanceReceiptEvent>()
    val shift = editText("原员工已发布班次编号")
    val actual = editText("核实的实际领取时间（如2026-10-18T20:00:00+08:00）")
    val reason = editText("补证原因")
    val evidence = editText("实际交接核实事实（原领用登记、交接人员及核对结论）")
    var requestKey = java.util.UUID.randomUUID().toString()
    val load = button("查询原领取事件") {
      runBackground(action = { api.attendanceReceiptEvents(employee.text.toString().trim().ifEmpty { null }) }, onSuccess = {
        events = it
        picker.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
          it.map { event -> "${event.id} · ${event.deviceId ?: "原设备"} · ${if (event.status == "accepted") "已核实" else "待核实"}" })
        status.text = "已读 ${it.size} 条。原手机观察时间未直接计作考勤。"
      }, onError = { status.text = "读取失败：${friendly(it)}" })
    }
    picker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
      override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
        shift.setText(events.getOrNull(position)?.shiftId ?: "")
        status.text = if (events.getOrNull(position)?.reason == "SUPERVISOR_CONTEXT_RECOVERY_REVIEW_REQUIRED")
          "原事件未记录领用编号；员工与领用关联由主管补证提出，仍须真实上一级同时核实关联和时间，尚未计入出勤。"
          else "请核对原领取、领用与实际时间证据，批准前保持待核。"
        actual.setText(""); reason.setText(""); evidence.setText("")
        requestKey = java.util.UUID.randomUUID().toString()
      }
      override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }
    val submit = button("主管提交上一级核时审批") {
      val event = events.getOrNull(picker.selectedItemPosition)
      if (event == null || event.status != "pending_review") { status.text = "请选择待核实的原领取事件"; return@button }
      if (shift.text.isBlank() || actual.text.isBlank() || reason.text.isBlank() || evidence.text.isBlank()) {
        status.text = "请填写原班次、实际时间、原因和交接核实事实"; return@button
      }
      val actualTime = try { java.time.OffsetDateTime.parse(actual.text.toString().trim()).toInstant().toString() }
        catch (error: Exception) { status.text = "实际时间须包含日期、时间及时区"; return@button }
      runBackground(action = { api.submitAttendanceReceiptReview(event.id, AttendanceReceiptReviewRequest(requestKey,
        shift.text.toString().trim(), actualTime, reason.text.toString().trim(), evidence.text.toString().trim())) },
        onSuccess = { result -> status.text = "已提交，等待经办主管的上一级审批。审批单：${result.reviewId}" },
        onError = { status.text = "提交未完成：${friendly(it)}" })
    }
    val history = button("查看所选事件核时审批历史") {
      val event = events.getOrNull(picker.selectedItemPosition)
      if (event == null) { status.text = "请先查询并选择原事件"; return@button }
      runBackground(action = { api.attendanceReceiptReviewHistory(event.id) }, onSuccess = { rows ->
        status.text = if (rows.isEmpty()) "暂无核时申请" else rows.joinToString("\n\n") { row ->
          "审批单 ${row.reviewId} · ${when(row.status) { "approved" -> "已批准并入考勤"; "rejected" -> "已驳回，原时间仍待核实"; else -> "待上一级审批" }}\n拟确认 ${row.actualAt}；班次 ${row.shiftId}\n核实事实：${row.handoverEvidence}"
        }
      }, onError = { status.text = "读取失败：${friendly(it)}" })
    }
    setContentView(screen(title = "领取实际时间核实", children = listOf(status, employee, load, picker,
      shift, actual, reason, evidence, submit, history, backButton())))
  }

  private fun showWorkPhoneReturn() {
    val status = statusText("设备接收人核对实物与当班收尾。故障换机仅夜班值班经理可办，关联原班，不产生下班。")
    val code = editText("原工作手机设备码").apply { setText(api.deviceId ?: "") }
    val reason = editText("交接说明／故障原因")
    val replacement = editText("故障替换手机设备编号")
    val completed = android.widget.CheckBox(this).apply { text = "已实际收到手机，并确认本人当班工作及收尾检查完成" }
    val returnKey = java.util.UUID.randomUUID().toString()
    val swapKey = java.util.UUID.randomUUID().toString()
    val submit = button("确认本班手机实际归还") {
      if (!completed.isChecked || code.text.isBlank() || reason.text.isBlank()) { status.text = "请核对实物、完成情况并填写交接说明"; return@button }
      runBackground(action = {
        api.returnDevice(DeviceReturnRequest(idempotencyKey=returnKey, deviceCode=code.text.toString().trim(),
          hasPendingTasks=false, reason=reason.text.toString(), operatorNote=reason.text.toString()))
      }, onSuccess = {
        status.text = "归还成功，下班时间已记录"
        if (it.deviceCode == api.deviceId) ChargingReceiptBridge.clearAnchor(this)
      }, onError = { status.text = "归还未完成：${friendly(it)}" })
    }
    val swap = button("夜班值班经理确认故障换机") {
      if (code.text.isBlank() || replacement.text.isBlank() || reason.text.isBlank()) { status.text = "请填原手机、替换手机和故障原因"; return@button }
      runBackground(action = {
        api.swapWorkPhone(WorkPhoneSwapRequest(swapKey,code.text.toString().trim(),replacement.text.toString().trim(),reason.text.toString()))
      }, onSuccess = { status.text = "换机完成，沿用原班次与原上班记录；新机领取前重新校准本班时间" },
        onError = { status.text = "换机未完成：${friendly(it)}" })
    }
    setContentView(screen(title = "工作手机实物交接", children = listOf(status,code,reason,completed,submit,replacement,swap,backButton())))
  }

  private fun showShiftManage() {
    val userId = editText("员工")
    val workDate = editText("日期(2026-07-03)")
    val startAt = editText("开始(2026-07-03T10:00:00Z)")
    val endAt = editText("结束(2026-07-03T18:30:00Z)")
    val points = editText("点位,逗号分隔(可空)")
    val note = editText("备注(可空)")
    val nightDuty = android.widget.CheckBox(this).apply { text = "承担所属项目本日夜班值班经理职责" }
    val status = statusText("排班提交上一级审批后发布；同项目同日夜班值班经理仅一名，职责仅在已发布班次时段内生效")
    val submitButton = button("提交排班审批") {
      val u = userId.text.toString().trim()
      val d = workDate.text.toString().trim()
      val st = startAt.text.toString().trim()
      val en = endAt.text.toString().trim()
      if (u.isEmpty() || d.isEmpty() || st.isEmpty() || en.isEmpty()) {
        status.text = "请填全 员工/日期/起止时间"
        return@button
      }
      runBackground(
        action = {
          api.upsertStaffShift(StaffShiftUpsertRequest(
            idempotencyKey = "shift_${System.currentTimeMillis()}",
            nightDutyManager = nightDuty.isChecked,
            userId = u, workDate = d, startAt = st, endAt = en,
            pointSpaceIds = points.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
              .ifEmpty { null },
            note = note.text.toString().trim().ifEmpty { null },
          ))
        },
        onSuccess = { s -> status.text = "已提交，等待上一级批准后发布。审批编号：${s.reviewId ?: "-"}" },
        onError = { status.text = "保存失败：${friendly(it)}" },
      )
    }
    setContentView(screen(
      title = "主管排班",
      children = listOf(status, userId, workDate, startAt, endAt, points, nightDuty, note, submitButton,
        backButton()),
    ))
  }

  // ── F批(2026-07-02 拍板):我的薪资 / 考勤月视图 / 请假 ──
  private fun showMyPayroll() {
    val month = editText("月份(2026-06)")
    val status = statusText("已定格工资可查看逐项计薪、工时和补差；应发净额与实际付款分别列示。")
    val detail = statusText("")
    val commissionDetail = statusText("")
    val queryButton = button("查询") {
      val m = month.text.toString().trim()
      if (!Regex("\\d{4}-\\d{2}").matches(m)) { status.text = "请输入月份如 2026-06"; return@button }
      detail.text = ""; commissionDetail.text = ""; status.text = "读取已定格工资原件…"
      runBackground(
        action = { api.listMyPayroll(m).map { row -> api.myPayrollDetail(requireNotNull(row.id) { "工资单号缺失" }) } },
        onSuccess = { rows ->
          if (rows.isEmpty()) {
            status.text = "$m 尚未月结(人事月结后可见)或无记录"
            detail.text = ""
            commissionDetail.text = ""
          } else {
            status.text = "$m 共 ${rows.size} 张薪资单（已定格）"
            detail.text = rows.joinToString("\n\n") { payrollDetailText(it) }
            commissionDetail.text = "提成明细加载中…"
            runBackground(
              action = { api.listMyPayrollCommissions(m) },
              onSuccess = { lines -> commissionDetail.text = commissionBreakdown(lines) },
              onError = { commissionDetail.text = "提成明细拉取失败：${friendly(it)}" },
            )
          }
        },
        onError = { status.text = "查询失败：${friendly(it)}" },
      )
    }
    setContentView(screen(
      title = "我的薪资",
      children = listOf(status, month, queryButton, detail, commissionDetail,
        button("我的绩效扣分与申诉") { showMyPerformanceAdjustments() }, backButton()),
      secure = true,
    ))
  }

  private fun showMyPerformanceAdjustments() {
    val month = editText("月份(2026-10)")
    val status = statusText("查看本人绩效扣分原件；对已批准且未冲回的扣分提出申诉。")
    val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    fun load() {
      val value = month.text.toString().trim()
      if (!Regex("\\d{4}-\\d{2}").matches(value)) { status.text = "请填写有效工资月份"; return }
      list.removeAllViews(); status.text = "读取本人绩效记录…"
      runBackground(
        action = { api.myPerformanceAdjustments(value) },
        onSuccess = { rows ->
          status.text = "共 ${rows.size} 条本人记录"
          rows.forEach { row ->
            list.addView(statusText("${row.id}\n规则：${row.ruleCode}；分值：${row.points}\n事由：${row.reason}\n状态：${payrollStatusLabel(row.status)}" + (row.reversalOf?.let { "\n申诉冲回原记录：$it" } ?: "")))
            if (row.status == "approved" && row.reversalOf == null && rows.none { it.reversalOf == row.id }) {
              val reason = editText("申诉事由")
              val appealKey = java.util.UUID.randomUUID().toString()
              var submitting = false
              val appealButton = button("申诉 ${row.id}") {
                val text = reason.text.toString().trim()
                if (text.isBlank()) { status.text = "请填写申诉事由"; return@button }
                if (submitting) return@button
                submitting = true
                reason.isEnabled = false
                runBackground(
                  action = { api.appealPerformance(row.id, text, appealKey) },
                  onSuccess = { status.text = "已发起申诉，按直属共三级和人力审批"; load() },
                  onError = { submitting = false; status.text = "申诉提交失败：${friendly(it)}；可重试原申请" },
                )
              }
              list.addView(reason); list.addView(appealButton)
            }
          }
        },
        onError = { status.text = "查询失败：${friendly(it)}" },
      )
    }
    setContentView(screen(title = "我的绩效扣分与申诉", children = listOf(status, month, button("读取本人记录") { load() }, list, backButton()), secure = true))
  }

  private fun showAttendanceMonth() {
    val month = editText("月份(2026-07)")
    val status = statusText("逐日:出勤/缺勤/迟到/早退/未打下班卡/请假;无排班=休。")
    val list = statusText("")
    val labels = mapOf(
      "present" to "出勤", "absent" to "缺勤", "late" to "迟到", "early_leave" to "早退",
      "missing_checkout" to "未打下班卡", "leave" to "请假", "rest" to "休", "pending_review" to "实际时间待核实",
    )
    val queryButton = button("查询") {
      val m = month.text.toString().trim()
      if (!Regex("\\d{4}-\\d{2}").matches(m)) { status.text = "请输入月份如 2026-07"; return@button }
      runBackground(
        action = { api.attendanceMonthView(m) },
        onSuccess = { days ->
          status.text = "$m 考勤月视图(${days.size} 天)"
          list.text = days.joinToString("\n") { d ->
            val states = d.dayStatus?.joinToString("+") { labels[it] ?: it } ?: "-"
            val shift = if (d.shiftStartAt != null) "  班次 ${d.shiftStartAt} ~ ${d.shiftEndAt}" else ""
            "${d.date}  $states$shift"
          }
        },
        onError = { status.text = "查询失败：${friendly(it)}" },
      )
    }
    setContentView(screen(
      title = "考勤月视图",
      children = listOf(status, month, queryButton, list, backButton()),
      secure = true,
    ))
  }

  private fun showLeave() {
    val typeLabel = linkedMapOf("personal" to "事假", "sick" to "病假", "comp" to "调休", "annual" to "年假")
    val typeValues = listOf("") + typeLabel.keys                 // [0]=占位首项(空 token),提交前拦截
    val type = enumSpinner(listOf("请选择请假类型") + typeLabel.values)
    val start = editText("开始日期(2026-07-05)")
    val end = editText("结束日期(2026-07-06)")
    val reason = editText("事由")
    val status = statusText("提交后由排班主管审批;通过的日子考勤记「请假」不算缺勤。已通过的假可申请销假,主管批准后才生效。")
    val list = rowsContainer()
    // 2026-10-18 裁决:销假=本人申请＋主管批准。点选已通过的记录自动回填,填写理由后申请销假。
    val revokeTarget = editText("要申请销假的请假(点上方已通过的记录自动回填)")
    val revokeReason = editText("销假理由(必填)")
    val refresh = {
      runBackground(
        action = { api.listMyLeaves() },
        onSuccess = { rows ->
          fillSelectableRows(
            list,
            rows.map { r ->
              val st = leaveStatusLabel(r.status)
              val text = "${r.startDate}~${r.endDate} ${r.leaveType?.let { typeLabel[it] ?: it } ?: "-"} [$st]" +
                (r.reviewNote?.let { " 批注:$it" } ?: "") + (r.revokeRequestReason?.let { " 销假理由:$it" } ?: "")
              text to {
                if (r.status == "approved") {
                  revokeTarget.setText(r.id ?: "")
                  status.text = "已选请假:${r.startDate}~${r.endDate},填写理由后可申请销假"
                } else {
                  status.text = "只有已通过的请假才能申请销假(当前:$st)"
                }
              }
            },
            emptyText = "暂无请假记录",
          )
        },
        onError = { status.text = "请假记录拉取失败：${friendly(it)}" },
      )
    }
    val submitButton = button("提交请假") {
      val t = typeValues.getOrElse(type.selectedItemPosition) { "" }
      val s1 = start.text.toString().trim()
      val e1 = end.text.toString().trim()
      val r1 = reason.text.toString().trim()
      if (t.isEmpty() || s1.isEmpty() || e1.isEmpty() || r1.isEmpty()) {
        status.text = "请选择请假类型并填全 起止日期/事由"
        return@button
      }
      runBackground(
        action = {
          api.submitLeave(LeaveSubmitRequest(
            idempotencyKey = "leave_${System.currentTimeMillis()}",
            leaveType = t, startDate = s1, endDate = e1, reason = r1))
        },
        onSuccess = { status.text = "已提交,等待主管审批"; refresh() },
        onError = { status.text = "提交失败：${friendly(it)}" },
      )
    }
    val revokeButton = button("申请销假") {
      val id = revokeTarget.text.toString().trim()
      val why = revokeReason.text.toString().trim()
      if (id.isEmpty() || why.isEmpty()) {
        status.text = "请先点选已通过的请假并填写销假理由"
        return@button
      }
      runBackground(
        action = { api.requestLeaveRevoke(id, why, "leave_revoke_${System.currentTimeMillis()}") },
        onSuccess = { status.text = "已申请销假,等待主管批准"; refresh() },
        onError = { status.text = "申请销假失败：${friendly(it)}" },
      )
    }
    refresh()
    setContentView(screen(
      title = "请假",
      children = listOf(status, fieldLabel("请假类型"), type, start, end, reason, submitButton, list,
        revokeTarget, revokeReason, revokeButton, backButton()),
    ))
  }

  // ── G批(2026-07-02 六口径):代客点服务(公司代收QR) / 我的服务任务 ──
  private fun showServiceOrderCreate() {
    val catalog = statusText("正在拉取服务目录…")
    val catalogList = rowsContainer()
    val itemId = editText("服务项(点上方目录自动回填)")
    val performer = editText("履约员工(客人指定谁填谁)")
    val tableId = editText("桌台(可空)")
    val note = editText("备注(可空)")
    val status = statusText("下单后出示公司统一收款码,客人扫码支付、钱进公司账户;完成履约后管理费归公司,履约人应得份额月结进薪资并代扣个税。")
    val qrImage = android.widget.ImageView(this)
    runBackground(
      action = { api.listServiceCatalog() },
      onSuccess = { rows ->
        catalog.text = "服务目录 ${rows.size} 项(点一项自动回填 id)"
        fillSelectableRows(
          catalogList,
          rows.map { r ->
            "${r.name}  参考价 ¥${"%.2f".format((r.priceCent ?: 0) / 100.0)}" +
              (r.durationMinutes?.let { "  约${it}分钟" } ?: "") to {
              itemId.setText(r.id ?: "")
              catalog.text = "已选服务项：${r.name}"
            }
          },
          emptyText = "目录为空(请后台维护)",
        )
      },
      onError = { catalog.text = "目录拉取失败：${friendly(it)}" },
    )
    val lastOrderId = editText("服务单(下单后自动填)")
    val memberPhone = editText("会员手机号(选填，累计积分与成长值)")
    val paidButton = button("已收款到公司账户(目视支付成功)") {
      val id = lastOrderId.text.toString().trim()
      if (id.isEmpty()) { status.text = "请先下单"; return@button }
      confirmThen(
        title = "确认收款到公司账户",
        message = "将把服务单 $id 标记为已收款（公司统一账户）。\n标记后服务进入履约与分账流程，不可撤回。\n请先目视确认客人支付成功。",
      ) {
        runBackground(
          action = { api.markServiceOrderPaid(id, ServiceMarkPaidReq("mp_${System.currentTimeMillis()}",
            memberPhone = memberPhone.text.toString().trim().ifEmpty { null })) },
          onSuccess = { status.text = "已确认收款(公司账户):${it.id};舞者可上台" },
          onError = { status.text = "确认收款失败：${friendly(it)}" },
        )
      }
    }
    val completeButton = button("完成履约(跳完点这里)") {
      val id = lastOrderId.text.toString().trim()
      if (id.isEmpty()) { status.text = "请先下单"; return@button }
      runBackground(
        action = { api.completeServiceOrder(id, ServiceActionReq("cp_${System.currentTimeMillis()}")) },
        onSuccess = { o ->
          status.text = "已完成:管理费 ¥${"%.2f".format((o.mgmtFeeCent ?: 0) / 100.0)} 归公司," +
            "舞者应得 ¥${"%.2f".format((o.dancerShareCent ?: 0) / 100.0)}(月结进工资,代扣个税)"
        },
        onError = { status.text = "完成失败：${friendly(it)}" },
      )
    }
    val orderButton = button("下单并出示公司收款码") {
      val item = itemId.text.toString().trim()
      val perf = performer.text.toString().trim()
      if (item.isEmpty() || perf.isEmpty()) { status.text = "请填 服务项与履约员工"; return@button }
      runBackground(
        action = {
          val order = api.createServiceOrder(ServiceOrderCreateReq(
            idempotencyKey = "svo_${System.currentTimeMillis()}",
            catalogItemId = item, performerUserId = perf,
            tableId = tableId.text.toString().trim().ifEmpty { null },
            customerNote = note.text.toString().trim().ifEmpty { null },
          ))
          val pay = api.serviceOrderPaymentCode(order.id ?: "")
          Pair(order, pay["qr_content"]?.toString()?.trim('"') ?: "")
        },
        onSuccess = { (order, qr) ->
          lastOrderId.setText(order.id ?: "")
          order.id?.let { reportPageView("代客点服务(公司代收)", true, listOf(PageObjectRef("service_order", it)), null) }
          status.text = "已下单 ${order.itemName}(${order.id});请客人扫码支付到公司账户 ¥${"%.2f".format((order.priceCent ?: 0) / 100.0)}"
          if (qr.isNotEmpty()) {
            runCatching {
              val bitmap = com.journeyapps.barcodescanner.BarcodeEncoder()
                .encodeBitmap(qr, com.google.zxing.BarcodeFormat.QR_CODE, 600, 600)
              qrImage.setImageBitmap(bitmap)
            }.onFailure { status.append("\n二维码渲染失败:${it.message}") }
          }
        },
        onError = { status.text = "下单失败：${friendly(it)}" },
      )
    }
    setContentView(screen(
      title = "代客点服务(公司代收)",
      children = listOf(catalog, catalogList, itemId, performer, tableId, note, orderButton, status, qrImage,
        lastOrderId, memberPhone, paidButton, completeButton,
        backButton()),
      secure = true,
      sensitive = true,
    ))
  }

  private fun showMyServiceOrders() {
    val status = statusText("正在拉取我的服务任务…")
    val list = rowsContainer()
    val orderId = editText("要操作的服务单(点上方任务自动回填)")
    val labels = mapOf("pending_accept" to "待接单", "accepted" to "已接单", "completed" to "已完成", "cancelled" to "已取消")
    val refresh = {
      runBackground(
        action = {
          val orders = api.listMyServiceOrders()
          val month = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
          val stats = api.myServiceStats(month)
          Pair(orders, stats)
        },
        onSuccess = { (orders, stats) ->
          val count = stats["completed_count"]?.toString()?.trim('"') ?: "0"
          val income = "%.2f".format((stats["reference_income_cent"]?.toString()?.trim('"')?.toLongOrNull() ?: 0) / 100.0)
          status.text = "本月完成 $count 单,应得 ¥$income(公司代收,月结进工资并代扣个税)"
          fillSelectableRows(
            list,
            orders.map { o ->
              "${o.itemName} ¥${"%.2f".format((o.priceCent ?: 0) / 100.0)} 桌台 ${o.tableId ?: "-"} " +
                "[${labels[o.status] ?: o.status}]" + (o.customerNote?.let { "\n备注: $it" } ?: "") to {
                orderId.setText(o.id ?: "")
              }
            },
            emptyText = "暂无服务任务",
          )
        },
        onError = { status.text = "拉取失败：${friendly(it)}" },
      )
    }
    // 第十八批 M 段第 116 条：接单、完成履约是现场操作（BSM:1132），清单不含 service_order_fulfil 的终端（私人手机）不给这两个按钮。
    val fulfil = shows("service_order_fulfil")
    val acceptButton = button("接单") {
      val id = orderId.text.toString().trim()
      if (id.isEmpty()) { status.text = "请填服务单"; return@button }
      runBackground(
        action = { api.acceptServiceOrder(id, ServiceActionReq("acc_${System.currentTimeMillis()}")) },
        onSuccess = { status.text = "已接单：${it.id}"; refresh() },
        onError = { status.text = "接单失败：${friendly(it)}" },
      )
    }
    val completeButton = button("完成履约") {
      val id = orderId.text.toString().trim()
      if (id.isEmpty()) { status.text = "请填服务单"; return@button }
      runBackground(
        action = { api.completeServiceOrder(id, ServiceActionReq("cmp_${System.currentTimeMillis()}")) },
        onSuccess = { status.text = "已完成：${it.id}(已分账:管理费归公司,应得月结进薪资)"; refresh() },
        onError = { status.text = "完成失败：${friendly(it)}" },
      )
    }
    refresh()
    setContentView(screen(
      title = "我的服务任务",
      children = if (fulfil) listOf(status, list, orderId, acceptButton, completeButton, backButton()) else listOf(status, list, backButton()),
      secure = true,
    ))
  }

  /** U01-007（PGM L579 / BSM L935）：公司员工在本人私人手机上注册；本机 Keystore 生成的设备公钥与指纹随申请提交，服务端在登记时绑定本人。 */
  private fun showStaffSelfRegistration() {
    val name = editText("姓名")
    val phone = editText("手机号")
    val idDoc = editText("证件号码")
    val merchant = editText("自营商户编号（公司总部员工留空）")
    val company = android.widget.Spinner(this)
    var companies: List<StaffRegistrationCompany> = emptyList()
    val status = statusText("选择所属公司并提交资料；本机将生成并随申请绑定该手机专属的设备标识（Keystore 公钥/指纹）。公司审批通过后，用本机登录。")
    val requestKey = java.util.UUID.randomUUID().toString()
    val submit = button("提交本人注册申请") {
      val selected = companies.getOrNull(company.selectedItemPosition)
      if (selected == null || name.text.toString().trim().isEmpty()) { status.text = "请选择公司并填写姓名"; return@button }
      val fingerprint = DeviceKeystore.deviceFingerprintHash()
      val request = StaffSelfRegistrationRequest(requestKey, selected.id, name.text.toString().trim(),
        DeviceKeystore.devicePublicKeyBase64(), fingerprint,
        merchant.text.toString().trim().ifEmpty { null }, phone.text.toString().trim().ifEmpty { null }, idDoc.text.toString().trim().ifEmpty { null })
      runBackground(
        action = { api.submitStaffSelfRegistration(request) },
        onSuccess = { receipt ->
          // 绑定成功：本机以登记的指纹作为设备编号、按私人手机终端码声明身份并签名（U01-001 只声明一次）。
          getSharedPreferences("private_phone", MODE_PRIVATE).edit().putString("device_id", fingerprint).apply()
          api.deviceId = fingerprint
          api.terminalType = STAFF_PRIVATE_PHONE
          status.text = "申请已提交，本机设备标识已随申请绑定；等待所属公司审批，通过后用本机登录。申请编号：${receipt.id}"
        },
        onError = { status.text = "提交失败：${friendly(it)}" },
      )
    }
    runBackground(
      action = { api.listStaffRegistrationCompanies() },
      onSuccess = { choices -> companies = choices; company.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices.map { it.name }) },
      onError = { status.text = "公司列表读取失败：${friendly(it)}" },
    )
    setContentView(screen(title = "公司员工自助注册（私人手机）", children = listOf(status, company, name, phone, idDoc, merchant, submit, button("返回登录") { showLogin() })))
  }

  private fun showStaffRegistration() {
    val name = editText("新员工姓名")
    val projectId = editText("所属项目编号")
    val merchantId = editText("本商户编号")
    val phone = editText("员工手机号")
    val idDoc = editText("员工证件号码")
    val status = statusText("商户代员工提交资料，由对应域公司审批。自营商户员工请在本人私人手机安装本应用后，从登录页「公司员工自助注册（私人手机）」提交。")
    val requestKey = java.util.UUID.randomUUID().toString()
    val submitButton = button("提交商户员工注册申请") {
      val n = name.text.toString().trim()
      val pj = projectId.text.toString().trim()
      val merchant = merchantId.text.toString().trim()
      if (n.isEmpty() || pj.isEmpty() || merchant.isEmpty()) { status.text = "请填姓名、项目与商户编号"; return@button }
      val request = StaffRegistrationSubmitRequest(requestKey, pj, n, "商户手机代提交", merchant,
        phone.text.toString().trim().ifEmpty { null }, idDoc.text.toString().trim().ifEmpty { null })
      runBackground(
        action = { api.submitStaffRegistration(request) },
        onSuccess = { p ->
          status.text = "已提交：${p.name ?: n}，等待对应域公司审批"
          p.id?.let { reportPageView("商户员工注册申请", true, listOf(PageObjectRef("staff_profile", it)), null) }
        },
        onError = { status.text = "提交失败：${friendly(it)}" },
      )
    }
    setContentView(screen(title = "商户员工注册申请", children = listOf(status, name, projectId, merchantId, phone, idDoc, submitButton, backButton()), sensitive = true))
  }

  // ===========================================================================
  // 平板图形化点单 POS：① 首次绑定本机酒吧 → ② 选桌 → ③ 分类目录/SKU 卡片/套餐 →
  //   ④ 购物车加减 → ⑤ 下单(挂桌台) → ⑥ 由订单生成账单 → 按桌结算。
  // 图片走鉴权 GET（Bearer），异步加载解码为 Bitmap。首次绑定后 bar_id 本地持久化不再追问。
  // ===========================================================================

  /** 购物车行：SKU 用 productId、套餐用 comboId（二选一），name/priceCent 展示，qty 数量。 */
  private data class PosCartLine(
    val productId: String?,
    val comboId: String?,
    val name: String,
    val unitPriceCent: Long,
    var qty: Int,
  )

  private fun showPosEntry() {
    if (store.session == null) { showLogin(); return }
    posBarId = posPrefs.getString("bar_id", null)?.ifBlank { null }
    if (posBarId == null) {
      showPosSetup()
    } else {
      showPosTables()
    }
  }

  /** ① 首次配置本机所属酒吧并绑定设备（terminal_type=bar_pos）。绑定成功后本地持久化 bar_id。 */
  private fun showPosSetup() {
    val barId = editText("本机所属酒吧").apply {
      posPrefs.getString("bar_id", null)?.let { setText(it) }
    }
    val deviceId = editText("设备编号").apply { setText("BARPOS-${System.currentTimeMillis()}") }
    val status = statusText("首次使用：把这台平板绑定到某个酒吧。绑定后进入图形点单，之后不再询问。")
    val bindButton = button("绑定并进入点单") {
      val bar = barId.text.toString().trim()
      val dev = deviceId.text.toString().trim()
      if (bar.isEmpty()) { status.text = "请填写酒吧"; return@button }
      if (dev.isEmpty()) { status.text = "请填写设备编号"; return@button }
      status.text = "正在绑定…"
      runBackground(
        action = {
          // U01-006：平板 POS 与工作手机同样写入设备编号并登记签名公钥，enforce 下写请求才有签名；类型码用服务端闭集 tablet。
          val binding = workflow.bindDevice(
            BindDeviceRequest(
              deviceId = dev,
              terminalType = "tablet",
              bindingScope = "space",
              boundSpaceId = bar,
              // 第 96 条 96-R1（PGM:623）：领用即带本机型号进设备台账
              deviceModel = android.os.Build.MODEL,
            ),
          )
          api.registerDeviceSigningKey(dev, DeviceKeystore.devicePublicKeyBase64(), java.util.UUID.randomUUID().toString(), "tablet")
          binding
        },
        onSuccess = { binding ->
          posPrefs.edit().putString("bar_id", bar).putString("device_id", dev).apply()
          api.deviceId = dev
          api.terminalType = "tablet"
          requestLocationPermission()
          posBarId = bar
          status.text = "绑定完成：${binding.deviceCode}（${deviceStatusZh(binding.status)}）"
          showPosTables()
        },
        onError = { status.text = "绑定失败：${friendly(it)}" },
      )
    }
    setContentView(
      screen(
        title = "平板 POS · 绑定酒吧",
        children = listOf(status, barId, deviceId, bindButton, backButton()),
      ),
    )
  }

  /** ② 选桌：图形化桌位（可点方块，显示桌号/容纳/占用状态）。 */
  private fun showPosTables() {
    val bar = posBarId
    if (bar == null) { showPosSetup(); return }
    val status = statusText("酒吧 $bar · 请选择桌台开始点单")
    val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    setContentView(
      screen(
        title = "平板 POS · 选桌",
        children = listOf(status, grid,
          button("刷新桌台") { loadPosTables(bar, grid, status) },
          button("重设本机酒吧") { showPosSetup() },
          backButton()),
        root = true,
      ),
    )
    loadPosTables(bar, grid, status)
  }

  private fun loadPosTables(bar: String, grid: LinearLayout, status: TextView) {
    status.text = "正在加载桌台…"
    runBackground(
      action = { api.listBarTableAvailability().filter { it.barId == null || it.barId == bar } },
      onSuccess = { tables ->
        grid.removeAllViews()
        if (tables.isEmpty()) { grid.addView(statusText("暂无桌台（可先在后台配置）")); status.text = "无桌台"; return@runBackground }
        status.text = "共 ${tables.size} 桌，点选进入点单"
        var row: LinearLayout? = null
        tables.forEachIndexed { i, t ->
          if (i % 2 == 0) {
            row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            grid.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
          }
          val occupied = t.reserved == true || (t.status != null && t.status != "available" && t.status != "free")
          val tile = TextView(this).apply {
            text = buildString {
              append(t.tableName ?: t.tableCode ?: t.id ?: "桌")
              t.capacity?.let { append("\n可坐 ").append(it) }
              append('\n').append(if (occupied) "占用/预留" else "空闲")
            }
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(20), dp(12), dp(20))
            setTextColor(Color.parseColor(if (occupied) "#8a5a00" else "#1f5f2f"))
            setBackgroundColor(Color.parseColor(if (occupied) "#fff3e0" else "#e6f6ea"))
            isClickable = true
            setOnClickListener {
              posCart.clear()
              showPosCatalog(bar, t)
            }
          }
          row!!.addView(tile, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = if (i % 2 == 1) dp(8) else 0 })
        }
      },
      onError = { status.text = "加载桌台失败：${friendly(it)}" },
    )
  }

  /** ③④ 分类目录 + SKU/套餐卡片 + 购物车。分类横向切换，卡片图片异步鉴权加载。
   *  120-R4：一体机吧台买酒（[kiosk]）共用本屏，酒吧与吧台位由调用方传入，下单走 [submitKioskBarOrder]。 */
  private fun showPosCatalog(bar: String, table: BarTableAvailabilityRow?, kiosk: Boolean = false) {
    val tableLabel = table?.tableName ?: table?.tableCode ?: table?.id ?: "-"
    val status = statusText(if (kiosk) "吧台位 $tableLabel · 选择酒水、套餐和数量加入购物车" else "桌台 $tableLabel · 选择商品加入本桌订单")
    val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val tabScroll = android.widget.HorizontalScrollView(this).apply { addView(tabs) }
    val cardsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val cartSummary = statusText("购物车：空")
    val submitButton = if (kiosk) button("下单") { submitKioskBarOrder(bar, table, status) }
      else button("下单（挂本桌）") { submitPosOrder(bar, table, status) }

    fun refreshCart() {
      if (posCart.isEmpty()) { cartSummary.text = "购物车：空"; return }
      val total = posCart.values.sumOf { it.unitPriceCent * it.qty }
      cartSummary.text = buildString {
        append("购物车合计 ¥").append("%.2f".format(total / 100.0)).append('\n')
        posCart.values.forEach { append("· ").append(it.name).append(" ×").append(it.qty).append('\n') }
      }
    }

    fun card(name: String, priceCent: Long, imageUrl: String?, soldOut: Boolean, cartKey: String, mk: () -> PosCartLine): View {
      val box = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(10), dp(10), dp(10), dp(10))
        setBackgroundColor(Color.parseColor("#f7f8fa"))
        gravity = Gravity.CENTER_VERTICAL
      }
      val img = ImageView(this).apply {
        setBackgroundColor(Color.parseColor("#e3e6ea"))
        scaleType = ImageView.ScaleType.CENTER_CROP
      }
      box.addView(img, LinearLayout.LayoutParams(dp(64), dp(64)))
      val text = TextView(this).apply {
        this.text = buildString {
          append(name).append('\n').append("¥").append("%.2f".format(priceCent / 100.0))
          if (soldOut) append("  [沽清]")
        }
        textSize = 15f
        setTextColor(Color.parseColor("#1f2329"))
        setPadding(dp(12), 0, dp(12), 0)
      }
      box.addView(text, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
      val add = button(if (soldOut) "沽清" else "加入") {
        val line = posCart.getOrPut(cartKey) { mk() }
        line.qty += 1
        refreshCart()
      }
      add.isEnabled = !soldOut
      box.addView(add)
      // 异步鉴权加载图片
      if (imageUrl != null && imageUrl.isNotBlank()) {
        runBackground(
          action = { runCatching { api.catalogImageBytes(imageUrl) }.getOrNull() },
          onSuccess = { bytes ->
            if (bytes != null) {
              val bmp: Bitmap? = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
              if (bmp != null) img.setImageBitmap(bmp)
            }
          },
          onError = { /* 图片失败静默降级为占位灰块 */ },
        )
      }
      return box
    }

    fun loadProducts(categoryId: String?) {
      cardsContainer.removeAllViews()
      cardsContainer.addView(statusText("正在加载商品…"))
      runBackground(
        action = { api.listBarCatalogProducts(bar, categoryId) },
        onSuccess = { products ->
          cardsContainer.removeAllViews()
          if (products.isEmpty()) { cardsContainer.addView(statusText("该分类暂无商品")) }
          products.forEach { p ->
            cardsContainer.addView(
              card(p.productName, p.priceCent, p.imageUrl, p.soldOut, "sku:${p.id}") {
                PosCartLine(productId = p.id, comboId = null, name = p.productName, unitPriceCent = p.priceCent, qty = 0)
              },
              LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) },
            )
          }
        },
        onError = { cardsContainer.removeAllViews(); cardsContainer.addView(statusText("加载商品失败：${friendly(it)}")) },
      )
    }

    fun loadCombos() {
      cardsContainer.removeAllViews()
      cardsContainer.addView(statusText("正在加载套餐…"))
      runBackground(
        action = { api.listBarCombos(bar) },
        onSuccess = { combos ->
          cardsContainer.removeAllViews()
          if (combos.isEmpty()) { cardsContainer.addView(statusText("暂无套餐")) }
          combos.forEach { c ->
            val itemsDesc = c.items.joinToString("、") { "${it.productName ?: it.productId}×${it.quantity}" }
            cardsContainer.addView(
              card("${c.comboName}（含 $itemsDesc）", c.priceCent, c.imageUrl, c.status != null && c.status != "active", "combo:${c.id}") {
                PosCartLine(productId = null, comboId = c.id, name = c.comboName, unitPriceCent = c.priceCent, qty = 0)
              },
              LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) },
            )
          }
        },
        onError = { cardsContainer.removeAllViews(); cardsContainer.addView(statusText("加载套餐失败：${friendly(it)}")) },
      )
    }

    // 分类标签：动态拉取 + 固定「套餐」页签。
    runBackground(
      action = { api.listBarCategories(bar) },
      onSuccess = { cats ->
        tabs.removeAllViews()
        fun tab(label: String, onPick: () -> Unit) {
          tabs.addView(button(label) { onPick() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(6) })
        }
        tab("全部") { loadProducts(null) }
        cats.sortedBy { it.sortOrder }.forEach { c -> tab(c.categoryName) { loadProducts(c.id) } }
        tab("套餐") { loadCombos() }
        loadProducts(null)
      },
      onError = { status.text = "加载分类失败：${friendly(it)}"; loadProducts(null) },
    )

    setContentView(
      screen(
        title = if (kiosk) "吧台买酒" else "平板 POS · 点单",
        children = listOf(status, tabScroll, cardsContainer, cartSummary, submitButton,
          button("清空购物车") { posCart.clear(); refreshCart() },
          if (kiosk) button("返回一体机首页") { showKioskHome() } else button("返回选桌") { showPosTables() }),
      ),
    )
  }

  /** ⑤ 下单：购物车 → items（SKU=product_id、套餐=combo_id）→ createBarOrder 挂当前桌。 */
  private fun submitPosOrder(bar: String, table: BarTableAvailabilityRow?, status: TextView) {
    if (posCart.isEmpty()) { status.text = "购物车为空，请先加入商品"; return }
    val items = posCartItems()
    status.text = "正在下单…"
    runBackground(
      action = { workflow.createBarOrder(barId = bar, items = items, tableId = table?.id) },
      onSuccess = { order ->
        posCart.clear()
        showPosCheckout(order)
      },
      onError = { status.text = "下单失败：${friendly(it)}" },
    )
  }

  /** 购物车 → 下单 items（SKU=product_id、套餐=combo_id）。 */
  private fun posCartItems(): List<Map<String, String>> = posCart.values.filter { it.qty > 0 }.map { line ->
    if (line.comboId != null) mapOf("combo_id" to line.comboId, "quantity" to line.qty.toString())
    else mapOf("product_id" to (line.productId ?: ""), "quantity" to line.qty.toString())
  }

  /**
   * 120-R4：一体机吧台买酒下单——购物车在本机酒吧吧台位下单并生成待支付账单（[placeKioskBarOrder]），显示待支付金额后收款走 [kioskPayStep]（账单），
   * 登记后提示到吧台取酒。
   */
  private fun submitKioskBarOrder(bar: String, table: BarTableAvailabilityRow?, status: TextView) {
    val tableId = table?.id ?: return
    if (posCart.isEmpty()) { status.text = "购物车为空，请先加入酒水或套餐"; return }
    val items = posCartItems()
    status.text = "正在下单…"
    runBackground(
      action = { api.placeKioskBarOrder(bar, tableId, items) },
      onSuccess = { bill ->
        posCart.clear()
        kioskPayStep("吧台买酒 账单 ${bill.billNo ?: bill.id}", bill.payableAmountCent - bill.paidAmountCent, KioskPayTarget.BarBill(bill.id), null) {
          setContentView(screen(title = "吧台买酒", children = listOf(statusText("已付款，请到吧台取酒"),
            button("返回一体机首页") { showKioskHome() })))
        }
      },
      onError = { status.text = "下单失败：${friendly(it)}" },
    )
  }

  /** ⑥ 按桌结账（平板 POS 与「酒吧点酒下单 / 支付」共用）：由订单生成账单 → 展示账单剩余应收（只读）→ 选渠道 → 建支付意图并带意图编号结算（18P 137-R1）。 */
  private fun showPosCheckout(order: BarOrderRecord) {
    val status = statusText("订单 ${order.id} 已提交\n正在生成账单…")
    val amount = statusText("应收金额：账单生成后显示")
    val (channel, selectedChannel) = channelPicker()
    var bill: BarBillRecord? = null
    val settleButton = button("整桌结算") {
      val b = bill
      if (b == null) { status.text = "账单尚未生成，请稍候或返回重试"; return@button }
      settleAction(
        status, b, selectedChannel(),
        title = "整桌结算",
        messageTail = "结算本桌账单 ${b.id}。\n结算即确认收款完成、账单关闭，不可撤销。",
      )
    }
    setContentView(
      screen(
        title = "平板 POS · 结账",
        children = listOf(status, amount, channel, settleButton,
          button("返回选桌") { showPosTables() }, backButton()),
      ),
    )
    // 生成账单
    runBackground(
      action = { workflow.createBarOrderBill(order.id) },
      onSuccess = { b ->
        bill = b
        status.text = "订单 ${order.id}\n账单 ${b.id} 已生成，应收 ¥${"%.2f".format(b.remainingDueCent / 100.0)}\n引导顾客扫码支付后点「整桌结算」。"
        amount.text = dueText(b.remainingDueCent)
      },
      onError = { status.text = "生成账单失败：${friendly(it)}（可返回选桌重试）" },
    )
  }

  private fun dueText(cents: Long): String =
    "应收金额：¥${"%.2f".format(cents / 100.0)}（按账单应收结算，不可修改）"

  /** 支付渠道单选（微信/支付宝，默认微信）：返回控件与取值函数（按桌结账屏用）。 */
  private fun channelPicker(): Pair<android.widget.RadioGroup, () -> String> {
    val wechatRadio = android.widget.RadioButton(this).apply { text = "微信 wechat"; id = View.generateViewId() }
    val alipayRadio = android.widget.RadioButton(this).apply { text = "支付宝 alipay"; id = View.generateViewId() }
    val group = android.widget.RadioGroup(this).apply {
      orientation = LinearLayout.HORIZONTAL; addView(wechatRadio); addView(alipayRadio); check(wechatRadio.id)
    }
    return group to { if (alipayRadio.isChecked) "alipay" else "wechat" }
  }

  /** 结算动作（按桌结账屏用）：二次确认（账单剩余应收+渠道回显+屏内差异尾句）→ settleBarBillByScan（建支付意图、带意图编号结算）→ 状态回显。 */
  private fun settleAction(status: TextView, bill: BarBillRecord, ch: String, title: String, messageTail: String) {
    confirmThen(
      title = title,
      message = "将按 ¥${"%.2f".format(bill.remainingDueCent / 100.0)}（渠道 $ch）$messageTail",
    ) {
      status.text = "正在结算…"
      runBackground(
        action = { workflow.settleBarBillByScan(bill, ch) },
        onSuccess = { b ->
          status.text = "结算完成：账单 ${b.id}\n支付状态：${statusZh(b.paymentStatus)}，结算状态：${statusZh(b.settlementStatus)}"
        },
        onError = { status.text = "结算失败：${friendly(it)}" },
      )
    }
  }

  private fun screen(title: String, children: List<View>, root: Boolean = false, secure: Boolean = false, sensitive: Boolean = false): View {
    onRootScreen = root
    if (title != "考勤打卡") nfcResult = null
    // 防截屏:敏感屏(薪资/考勤/服务分账)设 FLAG_SECURE 挡截图/录屏/最近任务缩略图;
    // 因是单 Activity 多屏切换,每次进屏按敏感与否设/清标记,离开敏感屏即恢复可截屏。
    if (secure) {
      window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    val titleView = TextView(this).apply {
      text = title
      textSize = 22f
      setTextColor(Color.parseColor("#1f2329"))
      setPadding(0, 0, 0, dp(16))
    }
    val container = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(dp(20), dp(24), dp(20), dp(24))
      addView(titleView)
      children.forEach { child ->
        addView(
          child,
          LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
          },
        )
      }
    }
    // 大屏(平板)响应式：限制内容最大宽度并居中；内容不足一屏时垂直居中，超出则正常滚动。
    // 手机上 available 小于上限，宽度自动回退为满屏(MATCH_PARENT)，不写死单一尺寸。
    val maxContentWidth = dp(600)
    val available = resources.displayMetrics.widthPixels - dp(40)
    val contentWidth = if (available > maxContentWidth) maxContentWidth else ViewGroup.LayoutParams.MATCH_PARENT
    val centerWrapper = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
      addView(container, LinearLayout.LayoutParams(contentWidth, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    val page = ScrollView(this).apply {
      setBackgroundColor(Color.WHITE)
      isFillViewport = true
      addView(centerWrapper, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    // 第 23 条 23-R6：外包一层，暗纹叠在整屏最上层、不随内容滚动。敏感屏（页面上出现 PGM:686 两类内容）输入框不能选中、不弹复制菜单
    // （PGM:687「敏感页上的文字不能被选中、不能被复制」；TextView 默认不可选，应用内也没有另存与打印入口）。
    val frame = FrameLayout(this).apply {
      addView(page, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
    if (sensitive) {
      children.filterIsInstance<EditText>().forEach { it.isLongClickable = false; it.customSelectionActionModeCallback = noCopy }
    }
    reportPageView(title, sensitive, emptyList(), frame)
    return frame
  }

  private val noCopy = object : android.view.ActionMode.Callback {
    override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
    override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?) = false
    override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?) = false
    override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
  }

  /**
   * 第 23 条 23-R6（PGM:687）：员工工作手机、员工私人手机登录后的在线屏，打开即上报哪个屏（page_id＝屏标题）、是否敏感屏、这一次看到的对象；
   * 拿到暗纹码后平铺在 [frame] 最上层（frame 为空＝敏感屏补报看到的对象，不换已显示的暗纹）。失败或离线不叠；登录前的屏没有账号，不发。
   */
  private fun reportPageView(pageId: String, sensitive: Boolean, objects: List<PageObjectRef>, frame: FrameLayout?) {
    if (api.accessToken == null || !watermarkApplies(api.terminalType)) return
    runBackground(
      action = { api.postPageView(PageViewRequest(pageId, sensitive, objects, java.util.UUID.randomUUID().toString())) },
      onSuccess = { mark ->
        frame?.addView(watermark(mark.code), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
      },
      onError = { android.util.Log.w("ComplexTerminal", "page view not recorded", it) },
    )
  }

  /**
   * 与后台 PageWatermark、后端 WatermarkQr 同一组参数：码画成纠错 H、无静区二维码（模块 6dp），每格 200dp 平铺、格内偏移 24dp，码文小字在下，
   * 不透明度 0.08；不可点击，触摸照常落到下面的屏。
   */
  private fun watermark(code: String): View {
    val module = dp(6)
    val offset = dp(24)
    val qr = com.google.zxing.qrcode.QRCodeWriter().encode(code, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, mapOf(
      com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
      com.google.zxing.EncodeHintType.MARGIN to 0))
    val tile = Bitmap.createBitmap(dp(200), dp(200), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(tile)
    val paint = android.graphics.Paint().apply { color = Color.BLACK; textSize = dp(12).toFloat() }
    for (y in 0 until qr.height) for (x in 0 until qr.width) if (qr.get(x, y)) {
      canvas.drawRect((offset + x * module).toFloat(), (offset + y * module).toFloat(),
        (offset + (x + 1) * module).toFloat(), (offset + (y + 1) * module).toFloat(), paint)
    }
    canvas.drawText(code, offset.toFloat(), (offset + qr.height * module + dp(14)).toFloat(), paint)
    return View(this).apply {
      background = android.graphics.drawable.BitmapDrawable(resources, tile).apply {
        setTileModeXY(android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
      }
      alpha = 0.08f
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
  }

  /** 可点选列表容器：配合 [fillSelectableRows] 使用。 */
  private fun rowsContainer(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
  }

  /** 可点选列表：每条记录一个独立 TextView，点中高亮并执行回填，免去肉眼抄 id。 */
  private fun fillSelectableRows(
    container: LinearLayout,
    rows: List<Pair<String, () -> Unit>>,
    emptyText: String,
  ) {
    container.removeAllViews()
    fun params() = LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(4) }
    if (rows.isEmpty()) {
      container.addView(statusText(emptyText), params())
      return
    }
    rows.forEach { (text, onPick) ->
      val row = statusText(text)
      row.isClickable = true
      row.setOnClickListener {
        for (i in 0 until container.childCount) {
          container.getChildAt(i).setBackgroundColor(Color.parseColor("#f2f4f7"))
        }
        row.setBackgroundColor(Color.parseColor("#d9e7ff"))
        onPick()
      }
      container.addView(row, params())
    }
  }

  private fun statusText(text: String): TextView = TextView(this).apply {
    this.text = text
    textSize = 15f
    setTextColor(Color.parseColor("#3a3f47"))
    setPadding(dp(12), dp(12), dp(12), dp(12))
    setBackgroundColor(Color.parseColor("#f2f4f7"))
  }

  private fun editText(hint: String): EditText = EditText(this).apply {
    this.hint = hint
    textSize = 16f
    inputType = InputType.TYPE_CLASS_TEXT
  }

  /** 后端英文状态码->中文展示（仅改展示，不影响提交/存储的原值）；未知值兜底显示原文，不崩。
   *  仅收录展示点用到、跨枚举语义稳定的状态词；含义随上下文而异的状态各自就地映射（见 maintStatusZh 等）。 */
  private fun statusZh(raw: String?): String = when (raw) {
    "pending" -> "处理中"
    "paid" -> "已支付"; "partial_paid" -> "部分支付"; "unpaid" -> "未支付"
    "settled" -> "已结算"; "unsettled" -> "未结算"; "not_required" -> "无需"
    // 第十八批 C 段第 15 条：ENUM 第 103 行订台定金状态「免收（员工代订）」（订位成功提示用）。
    "staff_waived" -> "免收（员工代订）"
    "pending_review" -> "待审核"; "active" -> "已生效"; "superseded" -> "已被替换"; "revoked" -> "已撤销"
    "acknowledged" -> "已知悉"; "consented" -> "已同意"; "declined" -> "已拒绝"
    else -> raw ?: "-"
  }

  /** 游乐检修记录状态->中文（此枚举 open/submitted 语义与其他域不同，故就地单独映射）。 */
  private fun maintStatusZh(raw: String?): String = when (raw) {
    "submitted" -> "已提交"; "pending_confirm" -> "待主管确认"; "confirmed" -> "已确认恢复"
    "open" -> "已恢复运营"; "maintenance" -> "维保中"; "closed" -> "已关闭"
    else -> raw ?: "-"
  }

  /** 中文枚举下拉：显示中文 label，提交时由调用方按 selectedItemPosition 取对应英文 token（后端强校验英文枚举，中文仅用于展示，绝不提交给后端）。 */
  private fun enumSpinner(labels: List<String>): android.widget.Spinner = android.widget.Spinner(this).apply {
    adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, labels).apply {
      setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }
  }

  private fun fieldLabel(text: String): TextView = TextView(this).apply {
    this.text = text
    textSize = 13f
    setTextColor(Color.parseColor("#6b7280"))
    setPadding(dp(12), dp(8), dp(12), 0)
  }

  /** 终端设备绑定状态->中文（terminal_devices.status，见迁移 chk_terminal_devices_status CHECK 注释）。 */
  private fun deviceStatusZh(raw: String?): String = when (raw) {
    "bound" -> "已绑定"; "returned" -> "已归还"; "unbound" -> "已解绑"; "disabled" -> "已停用"
    "repair" -> "送修中"; "lost" -> "挂失"; "found" -> "已找回"; "scrapped" -> "已报废"; "blocked" -> "已封禁"
    else -> raw ?: "-"
  }

  /** 票务/游乐核销结果状态->中文（VerificationResponse.status，后端 ticketing 域）。 */
  private fun verifyStatusZh(raw: String?): String = when (raw) {
    "success" -> "核销成功"; "failed" -> "核销失败"; "rejected" -> "已拒绝"
    "reversed" -> "已冲正"; "revoked" -> "已作废"
    else -> raw ?: "-"
  }

  /** 酒吧桌型->中文（bar table_type 字典：bar/standard/card_seat）。 */
  private fun tableTypeZh(raw: String?): String = when (raw) {
    "bar" -> "吧台"; "standard" -> "普通桌"; "card_seat" -> "卡座"
    else -> raw ?: ""
  }

  /** 员工账号状态->中文（StaffProfileRecord.status = active/inactive；与 registration_status 不同域，不共用 statusZh）。 */
  private fun staffStatusZh(raw: String?): String = when (raw) {
    "active" -> "正常"; "inactive" -> "停用"
    else -> raw ?: "-"
  }

  private fun button(text: String, onClick: () -> Unit): Button = Button(this).apply {
    this.text = text
    setOnClickListener { onClick() }
  }

  private fun backButton(): Button = button("返回首页") { showHome() }

  // ── BYOD 二期 Wave1(2026-07-19 拍板) ──

  /** 分金额转「元」两位小数(工资条/提成复用)。 */
  private fun yuan(cent: Long?): String = "%.2f".format((cent ?: 0) / 100.0)

  /** 工资条提成明细:按来源归类合计(酒水/卡座 vs 服务点单 …),与 commissionCent 对账。 */
  private fun commissionBreakdown(lines: List<StaffCommissionLineRow>): String {
    if (lines.isEmpty()) return "提成明细: 本月无提成台账"
    val label = mapOf(
      "bar_table" to "酒水/卡座提成", "service_order" to "服务点单份额",
      "amusement" to "游乐提成", "ticket" to "票务提成", "other" to "其他",
    )
    return buildString {
      append("提成明细(按来源):\n")
      lines.groupBy { it.sourceType ?: "other" }.forEach { (type, rows) ->
        val sum = rows.sumOf { it.amountCent ?: 0 }
        append("· ").append(label[type] ?: type).append(": ¥").append(yuan(sum))
          .append(" (").append(rows.size).append("笔)\n")
      }
    }.trimEnd()
  }

  /** ②员工自助:我的资料——只读(BSM:1458 本人基础资料「可查看」;18P 136-R3 删自改姓名)。手机、证件只显示脱敏。 */
  private fun showMyProfile() {
    val session = store.session
    val identity = statusText(
      "账号: ${session?.username ?: "-"}\n工号(用户id): ${session?.staffId ?: "-"}",
    )
    val status = statusText("正在拉取本人档案…")
    runBackground(
      action = { api.myStaffProfile() },
      onSuccess = { p ->
        val reg = when (p.registrationStatus) {
          "approved" -> "已入职"; "submitted" -> "审核中"
          "rejected" -> "已驳回"; "suspended" -> "已停用"
          else -> p.registrationStatus ?: "-"
        }
        status.text = "姓名: ${p.name ?: "-"}\n档案状态: $reg / ${staffStatusZh(p.status)}(手机、证件加密存放，界面只显示脱敏)"
      },
      onError = { status.text = "拉取失败：${friendly(it)}(本账号可能无员工档案)" },
    )
    setContentView(screen(
      title = "我的资料",
      children = listOf(identity, status, backButton()),
    ))
  }

  /** ③待办与消息中心:端侧汇总本人相关消息——请假审批结果 / 待办工单 / 近期排班 / 公告。
   * 四路来源逐路容错(任一失败不拖垮整屏);Wave1 只读,处理仍在各自入口。 */
  private fun showInbox() {
    val status = statusText("正在汇总待办与消息…")
    val list = statusText("")
    runBackground(
      action = {
        val items = mutableListOf<String>()
        runCatching { api.listMyLeaves() }.getOrDefault(emptyList())
          .filter { it.status == "approved" || it.status == "rejected" }
          .forEach { lv ->
            val st = if (lv.status == "approved") "已通过" else "已驳回"
            items.add("[请假审批] ${lv.startDate}~${lv.endDate} ${lv.leaveType} $st" +
              (lv.reviewNote?.let { "，批注:$it" } ?: ""))
          }
        runCatching { api.listMyWorkOrders() }.getOrDefault(emptyList()).forEach { o ->
          items.add("[待办工单] ${o.code ?: o.id} [${o.status ?: "-"}] ${o.title ?: ""} 时限${o.handleDueAt ?: "-"}")
        }
        // 第十八批 M 段第 116 条：清单不含我的接警的终端（私人手机）不去调，免得每开一次就留一条被拒记录。
        (if (shows("my_alarms")) runCatching { api.listMyFacilityAlarms() }.getOrDefault(emptyList()) else emptyList())
          .filter { it.status == "awaiting_takeover" }
          .forEach { a -> items.add("[待接警] 设备 ${a.equipmentId ?: "-"} 优先级 ${a.priority ?: "-"} 时限${a.responseDueAt ?: "-"}（去「我的接警」明确接手）") }
        runCatching { api.listMyShifts() }.getOrDefault(emptyList()).forEach { s ->
          items.add("[排班] ${s.workDate} ${s.startAt ?: "-"}~${s.endAt ?: "-"}")
        }
        runCatching { api.listStaffNotices() }.getOrDefault(emptyList()).forEach { n ->
          val needAck = n.consentRequired == true && n.consentStatus != "acknowledged"
          items.add("[公告] ${n.title ?: n.id}" + (if (needAck) "(待确认已读)" else ""))
        }
        items.toList()
      },
      onSuccess = { items ->
        status.text = "共 ${items.size} 条(请假结果/待办工单/待接警/排班/公告)"
        list.text = if (items.isEmpty()) "暂无待办与消息" else items.joinToString("\n\n")
      },
      onError = { status.text = "汇总失败：${friendly(it)}" },
    )
    setContentView(screen(
      title = "待办与消息中心",
      children = listOf(status, list, button("公司公告(去确认已读)") { showStaffNotices() },
        backButton()),
    ))
  }
}
