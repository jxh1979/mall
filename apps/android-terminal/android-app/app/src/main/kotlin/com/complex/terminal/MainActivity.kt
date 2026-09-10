package com.complex.terminal

// 文件版本：V0.05

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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.concurrent.thread
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 综合体 Android 终端：现场操作 UI。
 *
 * 本 Activity 仅负责界面与线程编排；所有业务逻辑直接调用已实现的端侧业务层：
 *   - [TerminalApiClient]：HTTP 契约（login / bindDevice / preview / verify /
 *     amusementVerify / createBarOrder / pay-intent / settle / submitMaintenance /
 *     confirmResume / 设备凭证 / 冲正 / syncTasks / offline …）。
 *   - [TerminalWorkflow]：组合上述调用 + 本地 [InMemoryLocalStore]（会话/绑定/离线包/
 *     待同步队列/事件）。
 *
 * 网络调用一律在后台线程（kotlin.concurrent.thread）执行，回到 UI 线程刷新；
 * 失败/离线均给出中文提示。
 *
 * 实现说明：
 *   - 扫码：ZXing 内置扫码（IntentIntegrator）调起真机相机，结果回填输入框；无相机时可手动输入 credential 兜底。
 *   - Keystore：设备公钥/指纹取自 AndroidKeyStore（EC P-256，私钥不可导出），见 DeviceKeystore。
 */
class MainActivity : Activity() {

  private val store = InMemoryLocalStore()
  // 平板 POS 绑定持久化：首次配置本机所属酒吧后写入，之后不再追问（跨启动保留）。
  private val posPrefs: SharedPreferences by lazy { getSharedPreferences("bar_pos", Context.MODE_PRIVATE) }
  private var posBarId: String? = null
  private val workPhonePrefs: SharedPreferences by lazy { getSharedPreferences("work_phone", Context.MODE_PRIVATE) }
  // 当前桌台 + 购物车（本 Activity 会话态；下单成功清空）。
  private val posCart = LinkedHashMap<String, PosCartLine>()
  private var posTable: BarTableAvailabilityRow? = null
  // 4.2 allowed hosts 生效：COMPLEX_API_ALLOWED_HOSTS 构建白名单在客户端入口强制校验。
  private val api: TerminalApiClient by lazy {
    TerminalApiClient(
      BuildConfig.COMPLEX_API_BASE_URL.trim().trimEnd('/'),
      allowedHosts = BuildConfig.COMPLEX_API_ALLOWED_HOSTS.split(',')
        .map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
    ).apply {
      deviceId = workPhonePrefs.getString("device_id", null)
      deviceSigner = DeviceKeystore::sign
    }
  }
  private val workflow: TerminalWorkflow by lazy { TerminalWorkflow(api, store) }

  // 4.2 扫码相机：ZXing 内置扫码界面（IntentIntegrator 旧式回调，兼容纯 Activity），
  // 本地解码无网络依赖，结果回填目标输入框。
  private var scanTarget: EditText? = null
  private var scanResult: ((String) -> Unit)? = null
  private var nfcResult: ((String) -> Unit)? = null

  private fun launchScan(target: EditText) {
    scanTarget = target
    scanResult = null
    IntentIntegrator(this)
      .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
      .setPrompt("对准客人验票二维码")
      .setBeepEnabled(true)
      .setOrientationLocked(false)
      .initiateScan()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

  private fun recordWorkPhoneReceipt(method: String, tag: String? = null, type: String = "check_in", status: TextView? = null) {
    if (api.accessToken == null || api.deviceId == null) {
      status?.text = "请先登录并在本机完成工作手机登记"
      return
    }
    runBackground(
      action = { api.punchAttendance(AttendancePunchRequest(java.util.UUID.randomUUID().toString(), type, method,
        tagRef = tag, spaceId = tag?.removePrefix("space:"))) },
      onSuccess = { row ->
        val text = "${if (type == "check_in") "上班领取" else "下班"}已记录：${row.punchedAt ?: "-"}"
        status?.text = text
        android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show()
      },
      onError = {
        val text = "考勤记录失败：${friendly(it)}"
        status?.text = text
        android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show()
      },
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(this)
    // U01-001：终端身份由本机绑定类型决定并只声明一次，重启后从本地绑定记录恢复（工作手机 / 平板 POS）。
    workPhonePrefs.getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = "staff_work_phone" }
    posPrefs.getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = "tablet" }
    // U01-007：私人手机在本人注册时已把 Keystore 公钥/指纹绑定为员工设备凭证，重启后按登记的指纹恢复私人手机身份（只声明一次）。
    getSharedPreferences("private_phone", MODE_PRIVATE).getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = STAFF_PRIVATE_PHONE }
    // U01-002：拔线接收器由常驻前台服务持有并在开机后重注册；本页只消费投递（进程被回收期间的拔线在下次登录后补记）。
    ChargingReceiptBridge.handler = { runOnUiThread { recordWorkPhoneReceipt("charging_disconnect") } }
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
        action = { workflow.login(u, p) },
        onSuccess = { session ->
          status.text = "登录成功：${session.username}"
          if (ChargingReceiptBridge.consumePending(this@MainActivity)) recordWorkPhoneReceipt("charging_disconnect")
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
            ),
          )
          api.registerDeviceSigningKey(d, DeviceKeystore.devicePublicKeyBase64(), java.util.UUID.randomUUID().toString())
          binding
        },
        onSuccess = { binding ->
          workPhonePrefs.edit().putString("device_id", d).apply()
          api.deviceId = d
          api.terminalType = "staff_work_phone"
          status.text = "绑定完成：${binding.deviceCode}（状态 ${deviceStatusZh(binding.status)}）"
          showHome()
        },
        onError = { status.text = "绑定失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(
        title = "设备绑定",
        children = listOf(status, deviceId, spaceId, bindButton, button("跳过，进入首页") { showHome() }),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ③ 首页 / 功能入口
  // ---------------------------------------------------------------------------
  private fun showHome() {
    val info = statusText(homeSummary())

    val children = mutableListOf<View>(
      info,
      button("门票/权益核销") { showTicketVerify() },
      button("游乐核销") { showAmusementVerify() },
      button("酒吧点酒下单 / 支付") { showBarOrder() },
      button("平板点单 POS（图形）") { showPosEntry() },
      button("游乐检修提交 / 主管确认") { showMaintenance() },
      button("同步待提交任务") { syncTasksAction(info) },
      button("员工设备凭证") { showStaffDevice() },
      button("票务核销冲正") { showReverse() },
      statusText("—— 员工自有手机(BYOD 一期) ——"),
      button("待办与消息中心") { showInbox() },
      button("公司公告") { showStaffNotices() },
      button("我的工单") { showMyWorkOrders() },
      button("酒吧空位 / 帮客订位") { showBarAvailability() },
      button("我的排班") { showMyShifts() },
      button("考勤打卡(NFC/扫码)") { showPunch() },
      button("我的薪资") { showMyPayroll() },
      button("考勤月视图") { showAttendanceMonth() },
      button("请假") { showLeave() },
      statusText("—— 服务点单(公司代收,月结分账) ——"),
      button("代客点服务") { showServiceOrderCreate() },
      button("我的服务收入") { showMyServiceOrders() },
      button("主管排班(需排班权限)") { showShiftManage() },
      button("商户员工注册申请（商户代提交）") { showStaffRegistration() },
      button("我的资料(个人信息)") { showMyProfile() },
      button("重新登录") { logoutAndRestart() },
    )

    setContentView(screen(title = "现场操作首页", children = children, root = true))
  }

  private fun homeSummary(): String {
    val s = store.session
    val b = store.binding
    return buildString {
      append("员工：").append(s?.username ?: "未登录").append('\n')
      append("设备：").append(b?.deviceCode ?: "未绑定")
      if (b?.status != null) append("（").append(b.status).append("）")
      append('\n')
      append("离线权益包：").append(store.offlineEntitlementCount()).append(" 条 / 待同步任务：")
      append(store.pendingTaskCount()).append(" 条")
    }
  }

  // ---------------------------------------------------------------------------
  // 屏 ④ 门票/权益核销 -> preview -> verify
  // 扫码读取 credential（ZXing 相机），无相机时手动输入兜底。
  // ---------------------------------------------------------------------------
  private fun showTicketVerify() {
    val credential = editText("核销凭证（扫码或手动输入）")
    val status = statusText("先预检再核销")
    val scanButton = button("扫码读取凭证") { launchScan(credential) }

    val previewButton = button("预检") {
      val code = credential.text.toString().trim()
      if (code.isEmpty()) { status.text = "请输入核销凭证"; return@button }
      requireBindingOr(status) {
        status.text = "正在预检…"
        runBackground(
          action = { workflow.previewOnline(code) },
          onSuccess = { r ->
            status.text = if (r.allowed) "预检通过：可核销（${r.reason.ifBlank { "allowed" }}）"
            else "预检不通过：${r.reason.ifBlank { "denied" }}"
          },
          onError = { status.text = "预检失败：${friendly(it)}" },
        )
      }
    }

    val verifyButton = button("核销") {
      val code = credential.text.toString().trim()
      if (code.isEmpty()) { status.text = "请输入核销凭证"; return@button }
      requireBindingOr(status) {
        status.text = "正在核销…"
        runBackground(
          action = { workflow.verifyOnline(code) },
          onSuccess = { r -> status.text = "核销结果：${verifyStatusZh(r.status)}\n核销号：${r.verificationNo}\n${r.resultReason ?: ""}" },
          onError = { status.text = "核销失败：${friendly(it)}" },
        )
      }
    }

    setContentView(
      screen(
        title = "门票 / 权益核销",
        children = listOf(status, credential, scanButton, previewButton, verifyButton, backButton()),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ⑤ 游乐核销 -> amusementVerifyOnline
  // ---------------------------------------------------------------------------
  private fun showAmusementVerify() {
    val credential = editText("游乐凭证（扫码或手动输入）")
    val sessionId = editText("场次（可选）")
    val status = statusText("游乐入口在线核销")

    val verifyButton = button("游乐核销") {
      val code = credential.text.toString().trim()
      if (code.isEmpty()) { status.text = "请输入游乐凭证"; return@button }
      requireBindingOr(status) {
        status.text = "正在核销…"
        runBackground(
          action = { workflow.amusementVerifyOnline(code, sessionId.text.toString().trim().ifEmpty { null }) },
          onSuccess = { r ->
            status.text = "核销结果：${r.verificationResult}\n记录：${r.id}\n${r.failureReason ?: ""}"
          },
          onError = { status.text = "游乐核销失败：${friendly(it)}" },
        )
      }
    }

    setContentView(
      screen(
        title = "游乐核销",
        children = listOf(status, credential, sessionId, verifyButton, backButton()),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ⑥ 酒吧点酒下单 + 支付引导
  // createBarOrder -> 账单支付意图（pay-intent，引导扫码支付）-> 结算 settle
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
        onSuccess = { o ->
          status.text = buildString {
            append("下单成功：订单 ").append(o.id).append('\n')
            append("状态：").append(o.status).append("，合计 ")
            append("%.2f".format(o.totalAmountCent / 100.0)).append(" 元\n")
            append("下一步：用账单号创建支付意图（引导扫码支付）后结算。")
          }
          showBarPayment(o)
        },
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

  // 酒吧支付引导：创建支付意图（生成支付二维码由后端返回 channel/status，UI 展示引导）+ 结算。
  private fun showBarPayment(order: BarOrderRecord) {
    val billId = editText("账单（下单后生成）")
    // 金额防误：结算金额=订单金额，只读展示，杜绝手滑改小。
    val cents = order.totalAmountCent
    val amount = statusField(cents)
    // 渠道防误：自由文本改为 wechat/alipay 单选，默认微信。
    val (channel, selectedChannel) = channelPicker()
    val status = statusText("订单 ${order.id}\n创建支付意图后引导顾客扫码支付，再结算账单。")

    val intentButton = button("创建支付意图（引导扫码）") {
      val bill = billId.text.toString().trim()
      if (bill.isEmpty()) { status.text = "请输入账单"; return@button }
      status.text = "正在创建支付意图…"
      runBackground(
        action = { workflow.createBarPaymentIntent(bill, cents, selectedChannel()) },
        onSuccess = { intent ->
          status.text = buildString {
            append("支付意图：").append(intent.id).append("（").append(intent.status).append("）\n")
            append("渠道：").append(intent.channel ?: "-").append("\n")
            append("请引导顾客按渠道二维码/收银台完成支付，支付成功后点击结算。")
          }
        },
        onError = { status.text = "创建支付意图失败：${friendly(it)}" },
      )
    }

    val settleButton = button("结算账单") {
      val bill = billId.text.toString().trim()
      if (bill.isEmpty()) { status.text = "请输入账单"; return@button }
      settleAction(
        status, bill, cents, selectedChannel(),
        title = "结算账单",
        messageTail = "结算账单 $bill。\n结算即确认收款完成、账单关闭，不可撤销。\n请确认客人已完成支付。",
      )
    }

    setContentView(
      screen(
        title = "酒吧账单支付 / 结算",
        children = listOf(status, billId, amount, channel, intentButton, settleButton, backButton()),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ⑦ 游乐检修提交 + 主管确认恢复
  // submitAmusementMaintenance -> confirmAmusementMaintenanceResume
  // ---------------------------------------------------------------------------
  private fun showMaintenance() {
    val projectId = editText("项目")
    val deviceId = editText("设备")
    val resultValues = listOf("pass", "fail")
    val result = enumSpinner(listOf("通过", "失败"))
    val note = editText("异常说明（可选）")
    val recordId = editText("检修记录（确认恢复时填）")
    val status = statusText("先提交检修记录，主管再确认恢复运营")

    val submitButton = button("提交检修记录") {
      val pid = projectId.text.toString().trim()
      val did = deviceId.text.toString().trim()
      if (pid.isEmpty() || did.isEmpty()) { status.text = "请填写项目与设备"; return@button }
      status.text = "正在提交…"
      runBackground(
        action = {
          workflow.submitAmusementMaintenance(
            projectId = pid,
            deviceId = did,
            result = resultValues.getOrElse(result.selectedItemPosition) { "pass" },
            exceptionNote = note.text.toString().trim().ifEmpty { null },
          )
        },
        onSuccess = { rec ->
          recordId.setText(rec.id)
          status.text = "检修记录已提交：${rec.id}（状态 ${maintStatusZh(rec.status)}）\n请主管确认恢复运营。"
        },
        onError = { status.text = "提交失败：${friendly(it)}" },
      )
    }

    val confirmButton = button("主管确认恢复运营") {
      val rid = recordId.text.toString().trim()
      if (rid.isEmpty()) { status.text = "请输入检修记录"; return@button }
      confirmThen(
        title = "主管确认恢复运营",
        message = "将对检修记录 $rid 确认恢复运营。\n确认后设备立即对客开放，责任落在确认主管，不可撤销。\n请确认检修已完成且设备安全。",
      ) {
        status.text = "正在确认…"
        runBackground(
          action = { workflow.confirmAmusementMaintenanceResume(rid) },
          onSuccess = { rec -> status.text = "已确认恢复：${rec.id}（状态 ${maintStatusZh(rec.status)}）" },
          onError = { status.text = "确认失败：${friendly(it)}" },
        )
      }
    }

    setContentView(
      screen(
        title = "游乐检修 / 主管确认",
        children = listOf(status, projectId, deviceId, fieldLabel("检修结论"), result, note, submitButton, recordId, confirmButton, backButton()),
      ),
    )
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
      runBackground(
        action = {
          workflow.bindStaffDeviceCredential(
            staffProfileId = pid,
            devicePublicKey = DeviceKeystore.devicePublicKeyBase64(),
            deviceFingerprintHash = DeviceKeystore.deviceFingerprintHash(),
          )
        },
        onSuccess = { rec -> status.text = "凭证绑定：${rec.id}（状态 ${statusZh(rec.bindStatus)}）" },
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

  // 同步待提交任务（离线核销/人工补录）
  private fun syncTasksAction(info: TextView) {
    info.text = "正在同步待提交任务…"
    runBackground(
      action = { workflow.syncPendingTasks() },
      onSuccess = { resp ->
        info.text = "同步完成：${resp.results.size} 条结果\n" + homeSummary()
      },
      onError = { info.text = "同步失败：${friendly(it)}\n" + homeSummary() },
    )
  }

  private fun logoutAndRestart() {
    store.clearSensitiveData()
    api.accessToken = null
    showLogin()
  }

  // ---------------------------------------------------------------------------
  // 通用辅助
  // ---------------------------------------------------------------------------

  /** 后台线程执行网络业务，回到 UI 线程回调。 */
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

  /** 接口错误转业务话术：解析响应体 {code, detail_code, message}；状态码+路径保留在末尾括号里供报障。 */
  private fun apiErrorText(error: ApiException): String {
    val body = runCatching {
      api.json.parseToJsonElement(error.responseBody) as? JsonObject
    }.getOrNull()
    fun field(key: String): String? = (body?.get(key) as? JsonPrimitive)?.content
    val code = field("detail_code") ?: field("code")
    val message = field("message")
    val text = when {
      error.statusCode == 401 || code == "UNAUTHORIZED" -> "请重新登录"
      error.statusCode == 403 || code == "FORBIDDEN" -> "当前账号或设备无权操作"
      code == "VALIDATION_FAILED" || error.statusCode == 400 || error.statusCode == 422 ->
        "提交内容有误" + (message?.let { "：$it" } ?: "")
      error.statusCode == 404 || code == "NOT_FOUND" -> "数据不存在"
      error.statusCode >= 500 -> "服务暂时不可用，请稍后重试"
      else -> message ?: "操作失败"
    }
    return "$text（${error.statusCode} ${error.path}）"
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

  private fun showMyWorkOrders() {
    val status = statusText("正在拉取流转到我的工单…")
    val list = statusText("")
    runBackground(
      action = { api.listMyWorkOrders() },
      onSuccess = { orders ->
        status.text = "我的工单 ${orders.size} 条(只读,处理在公司设备)"
        list.text = if (orders.isEmpty()) "暂无流转到你的工单" else orders.joinToString("\n\n") { o ->
          "${o.code ?: o.id} [${o.status ?: "-"}] ${o.title ?: ""}\n优先级 ${o.priority ?: "-"} / 处理时限 ${o.handleDueAt ?: "-"}"
        }
      },
      onError = { status.text = "拉取失败：${friendly(it)}" },
    )
    setContentView(screen(
      title = "我的工单",
      children = listOf(status, list, backButton()),
    ))
  }

  private fun showBarAvailability() {
    val status = statusText("正在拉取酒吧桌位…")
    val list = rowsContainer()
    val barId = editText("酒吧(点上方桌位自动回填)")
    val tableId = editText("桌位(点上方空位自动回填)")
    val time = editText("到店时间(如 2026-07-02T20:00:00Z)")
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
      val g = guests.text.toString().trim().toIntOrNull()
      if (b.isEmpty() || t.isEmpty() || at.isEmpty() || g == null || g <= 0) {
        status.text = "请填全 酒吧/桌位/时间/人数"
        return@button
      }
      runBackground(
        action = {
          api.createBarReservation(BarReservationCreateRequest(
            idempotencyKey = "byod_resv_${System.currentTimeMillis()}",
            barId = b, tableId = t, reservationTime = at, guestCount = g,
            salespersonUserId = store.session?.staffId,
          ))
        },
        onSuccess = { r -> status.text = "订位成功：${r.id ?: "-"}(定金 ${statusZh(r.depositStatus ?: "not_required")})" },
        onError = { status.text = "订位失败：${friendly(it)}" },
      )
    }
    setContentView(screen(
      title = "酒吧空位 / 帮客订位",
      children = listOf(status, list, barId, tableId, time, guests, reserveButton,
        backButton()),
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
    val status = statusText("领取工作手机即记录上班；碰 NFC、扫码或取离充电设施，以最早一次为准。拔线由常驻服务监听并记录。")
    val type = android.widget.Spinner(this).apply {
      adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("上班领取", "下班"))
    }
    fun record(method: String, tag: String) {
      if (tag.isBlank()) { status.text = "未读取到点位标签，请重试"; return }
      recordWorkPhoneReceipt(method, tag, if (method == "nfc" || type.selectedItemPosition == 0) "check_in" else "check_out", status)
    }
    nfcResult = { tag -> record("nfc", tag) }
    val scan = button("扫描点位二维码并记录") {
      scanTarget = null
      scanResult = { tag -> record("qr", tag) }
      IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        .setPrompt("对准工作手机领取点位二维码").setBeepEnabled(true).initiateScan()
    }
    setContentView(screen(title = "考勤打卡", children = listOf(status, type,
      statusText(if (nfcAdapter == null) "本机不支持 NFC，请扫码。" else "碰 NFC 标签只记录上班领取；下班请选择“下班”后扫码。"), scan, backButton())))
  }

  private fun showShiftManage() {
    val userId = editText("员工")
    val workDate = editText("日期(2026-07-03)")
    val startAt = editText("开始(2026-07-03T10:00:00Z)")
    val endAt = editText("结束(2026-07-03T18:30:00Z)")
    val points = editText("点位,逗号分隔(可空)")
    val note = editText("备注(可空)")
    val status = statusText("排班提交上一级审批后发布；修改仅限原发起人在班次开始前申请")
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
      children = listOf(status, userId, workDate, startAt, endAt, points, note, submitButton,
        backButton()),
    ))
  }

  // ── F批(2026-07-02 拍板):我的薪资 / 考勤月视图 / 请假 ──
  private fun showMyPayroll() {
    val month = editText("月份(2026-06)")
    val status = statusText("薪资单在人事「月结」后可见;固定工资+提成+激励-处罚-无薪假-个税=实发。")
    val detail = statusText("")
    val commissionDetail = statusText("")
    val queryButton = button("查询") {
      val m = month.text.toString().trim()
      if (!Regex("\\d{4}-\\d{2}").matches(m)) { status.text = "请输入月份如 2026-06"; return@button }
      runBackground(
        action = { api.listMyPayroll(m) },
        onSuccess = { rows ->
          if (rows.isEmpty()) {
            status.text = "$m 尚未月结(人事月结后可见)或无记录"
            detail.text = ""
            commissionDetail.text = ""
          } else {
            val r = rows.first()
            status.text = "$m 薪资单(已定格)"
            detail.text = buildString {
              append("固定工资: ¥").append(yuan(r.baseCent)).append('\n')
              append("提成合计: ¥").append(yuan(r.commissionCent)).append('\n')
              append("激励奖金: ¥").append(yuan(r.bonusCent)).append('\n')
              append("处罚扣款: -¥").append(yuan(r.penaltyCent)).append('\n')
              append("无薪假(事假)扣款: -¥").append(yuan(r.leaveDeductionCent)).append('\n')
              append("代扣个税: -¥").append(yuan(r.taxCent)).append('\n')
              append("实发合计: ¥").append(yuan(r.totalCent))
            }
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
        backButton()),
      secure = true,
    ))
  }

  private fun showAttendanceMonth() {
    val month = editText("月份(2026-07)")
    val status = statusText("逐日:出勤/缺勤/迟到/早退/未打下班卡/请假;无排班=休。")
    val list = statusText("")
    val labels = mapOf(
      "present" to "出勤", "absent" to "缺勤", "late" to "迟到", "early_leave" to "早退",
      "missing_checkout" to "未打下班卡", "leave" to "请假", "rest" to "休",
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
    val status = statusText("提交后由排班主管审批;通过的日子考勤记「请假」不算缺勤。")
    val list = statusText("")
    val refresh = {
      runBackground(
        action = { api.listMyLeaves() },
        onSuccess = { rows ->
          list.text = if (rows.isEmpty()) "暂无请假记录" else rows.joinToString("\n") { r ->
            val st = when (r.status) { "pending" -> "待审"; "approved" -> "已通过"; "rejected" -> "已驳回"; else -> r.status ?: "-" }
            "${r.startDate}~${r.endDate} ${r.leaveType?.let { typeLabel[it] ?: it } ?: "-"} [$st]" + (r.reviewNote?.let { " 批注:$it" } ?: "")
          }
        },
        onError = { list.text = "请假记录拉取失败：${friendly(it)}" },
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
    refresh()
    setContentView(screen(
      title = "请假",
      children = listOf(status, fieldLabel("请假类型"), type, start, end, reason, submitButton, list,
        backButton()),
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
    val paidButton = button("已收款到公司账户(目视支付成功)") {
      val id = lastOrderId.text.toString().trim()
      if (id.isEmpty()) { status.text = "请先下单"; return@button }
      confirmThen(
        title = "确认收款到公司账户",
        message = "将把服务单 $id 标记为已收款（公司统一账户）。\n标记后服务进入履约与分账流程，不可撤回。\n请先目视确认客人支付成功。",
      ) {
        runBackground(
          action = { api.markServiceOrderPaid(id, ServiceMarkPaidReq("mp_${System.currentTimeMillis()}")) },
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
        lastOrderId, paidButton, completeButton,
        backButton()),
      secure = true,
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
      children = listOf(status, list, orderId, acceptButton, completeButton,
        backButton()),
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
        onSuccess = { p -> status.text = "已提交：${p.name ?: n}，等待对应域公司审批" },
        onError = { status.text = "提交失败：${friendly(it)}" },
      )
    }
    setContentView(screen(title = "商户员工注册申请", children = listOf(status, name, projectId, merchantId, phone, idDoc, submitButton, backButton())))
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
            ),
          )
          api.registerDeviceSigningKey(dev, DeviceKeystore.devicePublicKeyBase64(), java.util.UUID.randomUUID().toString())
          binding
        },
        onSuccess = { binding ->
          posPrefs.edit().putString("bar_id", bar).putString("device_id", dev).apply()
          api.deviceId = dev
          api.terminalType = "tablet"
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
              posTable = t
              posCart.clear()
              showPosCatalog()
            }
          }
          row!!.addView(tile, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = if (i % 2 == 1) dp(8) else 0 })
        }
      },
      onError = { status.text = "加载桌台失败：${friendly(it)}" },
    )
  }

  /** ③④ 分类目录 + SKU/套餐卡片 + 购物车。分类横向切换，卡片图片异步鉴权加载。 */
  private fun showPosCatalog() {
    val bar = posBarId ?: return
    val table = posTable
    val tableLabel = table?.tableName ?: table?.tableCode ?: table?.id ?: "-"
    val status = statusText("桌台 $tableLabel · 选择商品加入本桌订单")
    val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val tabScroll = android.widget.HorizontalScrollView(this).apply { addView(tabs) }
    val cardsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val cartSummary = statusText("购物车：空")
    val submitButton = button("下单（挂本桌）") { submitPosOrder(status) }

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
        title = "平板 POS · 点单",
        children = listOf(status, tabScroll, cardsContainer, cartSummary, submitButton,
          button("清空购物车") { posCart.clear(); refreshCart() },
          button("返回选桌") { showPosTables() }),
      ),
    )
  }

  /** ⑤ 下单：购物车 → items（SKU=product_id、套餐=combo_id）→ createBarOrder 挂当前桌。 */
  private fun submitPosOrder(status: TextView) {
    val bar = posBarId ?: return
    val table = posTable
    if (posCart.isEmpty()) { status.text = "购物车为空，请先加入商品"; return }
    val items: List<Map<String, String>> = posCart.values.filter { it.qty > 0 }.map { line ->
      if (line.comboId != null) mapOf("combo_id" to line.comboId, "quantity" to line.qty.toString())
      else mapOf("product_id" to (line.productId ?: ""), "quantity" to line.qty.toString())
    }
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

  /** ⑥ 按桌结账：由订单生成账单 → 展示应收（只读）→ 选渠道 → 结算。 */
  private fun showPosCheckout(order: BarOrderRecord) {
    val cents = order.totalAmountCent
    val status = statusText("订单 ${order.id} 已生成，合计 ¥${"%.2f".format(cents / 100.0)}\n正在生成账单…")
    val (channel, selectedChannel) = channelPicker()
    var billId: String? = null
    val settleButton = button("整桌结算") {
      val bill = billId
      if (bill == null) { status.text = "账单尚未生成，请稍候或返回重试"; return@button }
      settleAction(
        status, bill, cents, selectedChannel(),
        title = "整桌结算",
        messageTail = "结算本桌账单 $bill。\n结算即确认收款完成、账单关闭，不可撤销。",
      )
    }
    setContentView(
      screen(
        title = "平板 POS · 结账",
        children = listOf(status, statusField(cents), channel, settleButton,
          button("返回选桌") { showPosTables() }, backButton()),
      ),
    )
    // 生成账单
    runBackground(
      action = { workflow.createBarOrderBill(order.id) },
      onSuccess = { b ->
        billId = b.id
        status.text = "订单 ${order.id}\n账单 ${b.id} 已生成，应收 ¥${"%.2f".format(cents / 100.0)}\n引导顾客扫码支付后点「整桌结算」。"
      },
      onError = { status.text = "生成账单失败：${friendly(it)}（可返回选桌重试）" },
    )
  }

  private fun statusField(cents: Long): TextView =
    statusText("应收金额：¥${"%.2f".format(cents / 100.0)}（按订单金额结算，不可修改）")

  /** 支付渠道单选（微信/支付宝，默认微信）：返回控件与取值函数（酒吧支付/POS 结账共用）。 */
  private fun channelPicker(): Pair<android.widget.RadioGroup, () -> String> {
    val wechatRadio = android.widget.RadioButton(this).apply { text = "微信 wechat"; id = View.generateViewId() }
    val alipayRadio = android.widget.RadioButton(this).apply { text = "支付宝 alipay"; id = View.generateViewId() }
    val group = android.widget.RadioGroup(this).apply {
      orientation = LinearLayout.HORIZONTAL; addView(wechatRadio); addView(alipayRadio); check(wechatRadio.id)
    }
    return group to { if (alipayRadio.isChecked) "alipay" else "wechat" }
  }

  /** 结算动作（酒吧支付/POS 结账共用）：二次确认（金额+渠道回显+屏内差异尾句）→ settleBarBill → 状态回显。 */
  private fun settleAction(status: TextView, bill: String, cents: Long, ch: String, title: String, messageTail: String) {
    confirmThen(
      title = title,
      message = "将按 ¥${"%.2f".format(cents / 100.0)}（渠道 $ch）$messageTail",
    ) {
      status.text = "正在结算…"
      runBackground(
        action = { workflow.settleBarBill(bill, cents, ch) },
        onSuccess = { b ->
          status.text = "结算完成：账单 ${b.id}\n支付状态：${statusZh(b.paymentStatus)}，结算状态：${statusZh(b.settlementStatus)}"
        },
        onError = { status.text = "结算失败：${friendly(it)}" },
      )
    }
  }

  private fun screen(title: String, children: List<View>, root: Boolean = false, secure: Boolean = false): View {
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
    return ScrollView(this).apply {
      setBackgroundColor(Color.WHITE)
      isFillViewport = true
      addView(centerWrapper, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
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

  /** ②员工自助:我的资料——只读身份 + 有限字段(姓名)自助编辑。手机/证件仅存不可逆哈希不回显。 */
  private fun showMyProfile() {
    val session = store.session
    val identity = statusText(
      "账号: ${session?.username ?: "-"}\n工号(用户id): ${session?.staffId ?: "-"}",
    )
    val status = statusText("正在拉取本人档案…")
    val nameInput = editText("姓名")
    val saveButton = button("保存姓名") {
      val nm = nameInput.text.toString().trim()
      if (nm.isEmpty()) { status.text = "姓名不能为空"; return@button }
      runBackground(
        action = {
          api.updateMyStaffProfile(StaffProfileSelfUpdateRequest(
            idempotencyKey = "profile_${System.currentTimeMillis()}", name = nm))
        },
        onSuccess = { p -> status.text = "已保存：${p.name ?: nm}" },
        onError = { status.text = "保存失败：${friendly(it)}" },
      )
    }
    runBackground(
      action = { api.myStaffProfile() },
      onSuccess = { p ->
        nameInput.setText(p.name ?: "")
        val reg = when (p.registrationStatus) {
          "approved" -> "已入职"; "submitted" -> "审核中"
          "rejected" -> "已驳回"; "suspended" -> "已停用"
          else -> p.registrationStatus ?: "-"
        }
        status.text = "档案状态: $reg / ${staffStatusZh(p.status)}(手机、证件加密存放，界面只显示脱敏)"
      },
      onError = { status.text = "拉取失败：${friendly(it)}(本账号可能无员工档案)" },
    )
    setContentView(screen(
      title = "我的资料",
      children = listOf(identity, status, nameInput, saveButton, backButton()),
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
        status.text = "共 ${items.size} 条(请假结果/待办工单/排班/公告)"
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
