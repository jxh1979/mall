package com.complex.terminal

import android.app.Activity
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.concurrent.thread

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
 * 占位说明：
 *   - 扫码：未接真机摄像头/扫码 SDK，现场以「文本输入 credential」代替（输入框）。
 *   - Keystore：设备公钥/指纹取自 AndroidKeyStore（EC P-256，私钥不可导出），见 DeviceKeystore。
 */
class MainActivity : Activity() {

  private val store = InMemoryLocalStore()
  // 4.2 allowed hosts 生效：COMPLEX_API_ALLOWED_HOSTS 构建白名单在客户端入口强制校验。
  private val api: TerminalApiClient by lazy {
    TerminalApiClient(
      BuildConfig.COMPLEX_API_BASE_URL.trim().trimEnd('/'),
      allowedHosts = BuildConfig.COMPLEX_API_ALLOWED_HOSTS.split(',')
        .map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
    )
  }
  private val workflow: TerminalWorkflow by lazy { TerminalWorkflow(api, store) }

  // 4.2 扫码相机：ZXing 内置扫码界面（IntentIntegrator 旧式回调，兼容纯 Activity），
  // 本地解码无网络依赖，结果回填目标输入框。
  private var scanTarget: EditText? = null

  private fun launchScan(target: EditText) {
    scanTarget = target
    IntentIntegrator(this)
      .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
      .setPrompt("对准客人验票二维码")
      .setBeepEnabled(true)
      .setOrientationLocked(false)
      .initiateScan()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
    if (result != null && result.contents != null) {
      scanTarget?.setText(result.contents)
      return
    }
    super.onActivityResult(requestCode, resultCode, data)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    showLogin()
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

    val loginButton = primaryButton("登录") {
      val u = username.text.toString().trim()
      val p = password.text.toString()
      if (u.isEmpty() || p.isEmpty()) {
        status.text = "请输入账号和密码"
        return@primaryButton
      }
      status.text = "正在登录…"
      runBackground(
        action = { workflow.login(u, p) },
        onSuccess = { session ->
          status.text = "登录成功：${session.username}"
          showDeviceBinding()
        },
        onError = { status.text = "登录失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(
        title = "综合体终端 · 登录",
        children = listOf(status, username, password, loginButton),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 屏 ② 设备绑定 -> TerminalWorkflow.bindDevice
  // ---------------------------------------------------------------------------
  private fun showDeviceBinding() {
    val deviceId = editText("设备编号 device_id").apply {
      setText("ANDROID-${System.currentTimeMillis()}")
    }
    val spaceId = editText("绑定空间 bound_space_id（点位）")
    val status = statusText("登录账号：${store.session?.username ?: "-"}\n请绑定本机到现场点位")

    val bindButton = primaryButton("绑定设备") {
      val d = deviceId.text.toString().trim()
      if (d.isEmpty()) {
        status.text = "请输入设备编号"
        return@primaryButton
      }
      status.text = "正在绑定…"
      runBackground(
        action = {
          workflow.bindDevice(
            BindDeviceRequest(
              deviceId = d,
              terminalType = "staff_work_phone",
              bindingScope = "space",
              boundSpaceId = spaceId.text.toString().trim().ifEmpty { null },
            ),
          )
        },
        onSuccess = { binding ->
          status.text = "绑定完成：${binding.deviceCode}（状态 ${binding.status}）"
          showHome()
        },
        onError = { status.text = "绑定失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(
        title = "设备绑定",
        children = listOf(status, deviceId, spaceId, bindButton, secondaryButton("跳过，进入首页") { showHome() }),
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
      primaryButton("门票/权益核销") { showTicketVerify() },
      primaryButton("游乐核销") { showAmusementVerify() },
      primaryButton("酒吧点酒下单 / 支付") { showBarOrder() },
      primaryButton("游乐检修提交 / 主管确认") { showMaintenance() },
      secondaryButton("同步待提交任务") { syncTasksAction(info) },
      secondaryButton("员工设备凭证（占位）") { showStaffDevice() },
      secondaryButton("票务核销冲正") { showReverse() },
      secondaryButton("重新登录") { logoutAndRestart() },
    )

    setContentView(screen(title = "现场操作首页", children = children))
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
  // 扫码占位：以文本输入 credential 代替。
  // ---------------------------------------------------------------------------
  private fun showTicketVerify() {
    val credential = editText("核销凭证（扫码或手动输入）")
    val status = statusText("先预检（preview）再核销（verify）")
    val scanButton = secondaryButton("扫码读取凭证") { launchScan(credential) }

    val previewButton = secondaryButton("预检 preview") {
      val code = credential.text.toString().trim()
      if (code.isEmpty()) { status.text = "请输入核销凭证"; return@secondaryButton }
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

    val verifyButton = primaryButton("核销 verify") {
      val code = credential.text.toString().trim()
      if (code.isEmpty()) { status.text = "请输入核销凭证"; return@primaryButton }
      requireBindingOr(status) {
        status.text = "正在核销…"
        runBackground(
          action = { workflow.verifyOnline(code) },
          onSuccess = { r -> status.text = "核销结果：${r.status}\n核销号：${r.verificationNo}\n${r.resultReason ?: ""}" },
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
    val credential = editText("游乐凭证（扫码占位：手动输入 credential）")
    val sessionId = editText("场次 session_id（可选）")
    val status = statusText("游乐入口在线核销")

    val verifyButton = primaryButton("游乐核销") {
      val code = credential.text.toString().trim()
      if (code.isEmpty()) { status.text = "请输入游乐凭证"; return@primaryButton }
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
    val barId = editText("酒吧 bar_id")
    val tableId = editText("桌台 table_id（可选）")
    val skuId = editText("酒水 SKU / 商品 id")
    val quantity = editText("数量").apply { inputType = InputType.TYPE_CLASS_NUMBER; setText("1") }
    val status = statusText("点酒下单 -> 生成账单后创建支付意图 -> 结算")

    val orderButton = primaryButton("提交点酒下单") {
      val bar = barId.text.toString().trim()
      val sku = skuId.text.toString().trim()
      if (bar.isEmpty() || sku.isEmpty()) { status.text = "请填写酒吧与酒水 SKU"; return@primaryButton }
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
    val billId = editText("账单 bill_id（下单后生成）")
    val amount = editText("金额（元）").apply {
      inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
      setText("%.2f".format(order.totalAmountCent / 100.0))
    }
    val channel = editText("支付渠道 channel（如 wechat / alipay）").apply { setText("wechat") }
    val status = statusText("订单 ${order.id}\n创建支付意图后引导顾客扫码支付，再结算账单。")

    val intentButton = secondaryButton("创建支付意图（引导扫码）") {
      val bill = billId.text.toString().trim()
      if (bill.isEmpty()) { status.text = "请输入账单 bill_id"; return@secondaryButton }
      val cents = parseAmountCent(amount.text.toString()) ?: run { status.text = "金额格式不正确"; return@secondaryButton }
      status.text = "正在创建支付意图…"
      runBackground(
        action = { workflow.createBarPaymentIntent(bill, cents, channel.text.toString().trim().ifEmpty { "wechat" }) },
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

    val settleButton = primaryButton("结算账单 settle") {
      val bill = billId.text.toString().trim()
      if (bill.isEmpty()) { status.text = "请输入账单 bill_id"; return@primaryButton }
      val cents = parseAmountCent(amount.text.toString()) ?: run { status.text = "金额格式不正确"; return@primaryButton }
      status.text = "正在结算…"
      runBackground(
        action = { workflow.settleBarBill(bill, cents, channel.text.toString().trim().ifEmpty { "wechat" }) },
        onSuccess = { b ->
          status.text = "结算完成：账单 ${b.id}\n支付状态：${b.paymentStatus ?: "-"}，结算状态：${b.settlementStatus ?: "-"}"
        },
        onError = { status.text = "结算失败：${friendly(it)}" },
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
    val projectId = editText("项目 project_id")
    val deviceId = editText("设备 device_id")
    val result = editText("检修结论 result（如 pass / fail）").apply { setText("pass") }
    val note = editText("异常说明 exception_note（可选）")
    val recordId = editText("检修记录 record_id（确认恢复时填）")
    val status = statusText("先提交检修记录，主管再确认恢复运营")

    val submitButton = primaryButton("提交检修记录") {
      val pid = projectId.text.toString().trim()
      val did = deviceId.text.toString().trim()
      if (pid.isEmpty() || did.isEmpty()) { status.text = "请填写项目与设备"; return@primaryButton }
      status.text = "正在提交…"
      runBackground(
        action = {
          workflow.submitAmusementMaintenance(
            projectId = pid,
            deviceId = did,
            result = result.text.toString().trim().ifEmpty { "pass" },
            exceptionNote = note.text.toString().trim().ifEmpty { null },
          )
        },
        onSuccess = { rec ->
          recordId.setText(rec.id)
          status.text = "检修记录已提交：${rec.id}（状态 ${rec.status}）\n请主管确认恢复运营。"
        },
        onError = { status.text = "提交失败：${friendly(it)}" },
      )
    }

    val confirmButton = secondaryButton("主管确认恢复运营") {
      val rid = recordId.text.toString().trim()
      if (rid.isEmpty()) { status.text = "请输入检修记录 record_id"; return@secondaryButton }
      status.text = "正在确认…"
      runBackground(
        action = { workflow.confirmAmusementMaintenanceResume(rid) },
        onSuccess = { rec -> status.text = "已确认恢复：${rec.id}（状态 ${rec.status}）" },
        onError = { status.text = "确认失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(
        title = "游乐检修 / 主管确认",
        children = listOf(status, projectId, deviceId, result, note, submitButton, recordId, confirmButton, backButton()),
      ),
    )
  }

  // ---------------------------------------------------------------------------
  // 员工设备凭证：devicePublicKey / deviceFingerprintHash 取自 AndroidKeyStore
  // （EC P-256，私钥硬件保护不可导出），见 DeviceKeystore；后端拒 PLACEHOLDER_* 占位。
  // ---------------------------------------------------------------------------
  private fun showStaffDevice() {
    val profileId = editText("员工档案 staff_profile_id")
    val status = statusText(
      "设备公钥/指纹哈希取自 Android Keystore（EC P-256，私钥不可导出），首次绑定自动生成。",
    )

    val bindButton = primaryButton("绑定设备凭证") {
      val pid = profileId.text.toString().trim()
      if (pid.isEmpty()) { status.text = "请输入 staff_profile_id"; return@primaryButton }
      status.text = "正在绑定凭证…"
      runBackground(
        action = {
          workflow.bindStaffDeviceCredential(
            staffProfileId = pid,
            devicePublicKey = DeviceKeystore.devicePublicKeyBase64(),
            deviceFingerprintHash = DeviceKeystore.deviceFingerprintHash(),
          )
        },
        onSuccess = { rec -> status.text = "凭证绑定：${rec.id}（状态 ${rec.bindStatus}）" },
        onError = { status.text = "绑定失败：${friendly(it)}" },
      )
    }

    setContentView(
      screen(title = "员工设备凭证", children = listOf(status, profileId, bindButton, backButton())),
    )
  }

  // 票务核销冲正
  private fun showReverse() {
    val verificationId = editText("核销记录 verification_id")
    val reason = editText("冲正原因 reason")
    val status = statusText("对已核销记录冲正（撤销）")

    val reverseButton = primaryButton("提交冲正") {
      val vid = verificationId.text.toString().trim()
      val r = reason.text.toString().trim()
      if (vid.isEmpty() || r.isEmpty()) { status.text = "请填写记录与原因"; return@primaryButton }
      status.text = "正在冲正…"
      runBackground(
        action = { workflow.reverseVerification(vid, r) },
        onSuccess = { res -> status.text = "冲正完成：${res.id}（状态 ${res.status}）" },
        onError = { status.text = "冲正失败：${friendly(it)}" },
      )
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

  private fun requireBindingOr(status: TextView, block: () -> Unit) {
    if (store.session == null) { status.text = "请先登录"; return }
    if (store.binding == null) { status.text = "请先绑定设备"; return }
    block()
  }

  /** 错误信息转中文友好提示，区分离线/网络/接口错误。 */
  private fun friendly(error: Throwable): String = when (error) {
    is ApiException -> "接口 ${error.statusCode}（${error.path}）"
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

  private fun parseAmountCent(input: String): Long? =
    input.trim().toDoubleOrNull()?.let { Math.round(it * 100) }

  // ---------- 视图工厂（纯 Android Views，无 AndroidX/Compose） ----------

  private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

  private fun screen(title: String, children: List<View>): View {
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
    return ScrollView(this).apply {
      setBackgroundColor(Color.WHITE)
      addView(container)
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

  private fun primaryButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
    this.text = text
    setOnClickListener { onClick() }
  }

  private fun secondaryButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
    this.text = text
    setOnClickListener { onClick() }
  }

  private fun backButton(): Button = secondaryButton("返回首页") { showHome() }
}
