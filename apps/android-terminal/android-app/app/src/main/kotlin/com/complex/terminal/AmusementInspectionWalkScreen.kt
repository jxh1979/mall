package com.complex.terminal

// 文件版本：V0.02

/**
 * 第十八批 BS 段第 373 条 373-R5（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过；未走完全部点位的巡检不成立、不得标记为已完成」；
 * 第十八批裁决 76「每个点位生成一张码贴在现场，巡检员用工作手机按顺序扫码后记结果，扫错码或跳点都记不上」）：工作手机游乐巡检走点。
 * 选游乐项目后显示下一个待记点位：扫贴在点位上的码、选通过或不通过、填备注后提交；顺序与点位码由服务端判，拒绝文案原样显示。
 * 从第 1 个点位重记即重新开始。走完后在后台安全巡检页新建巡检（检查项取本轮走完的点位）。
 * 第十八批 CF 段第 404 条 404-R2（BSM:1300「确需补录的一律须附证据（照片／录音／录像等）并经主管复核后方生效」；DEC-R6-09「补录记录与原始扫码记录并列
 * 展示、互不覆盖」）：下一个待记点位缺扫码时可「补录本点位」；本轮该点位有待复核补录时不给补录按钮、提示待复核；补录落的行标「人工补录」，被驳回的标「补录已驳回」。
 */
internal fun MainActivity.showAmusementInspectionWalk(initialProjectId: String? = null, notice: String? = null) {
  var projectId: String? = null
  var next: AmusementInspectionPoint? = null
  var first: AmusementInspectionPoint? = null
  var scanned: String? = null
  var current: AmusementInspectionPoints? = null
  var pendingNotice = notice
  val status = statusText("正在读取游乐项目…")
  val projects = rowsContainer()
  val progress = statusText("请先选择游乐项目")
  val result = enumSpinner(listOf("通过", "不通过"))
  val note = editText("备注（可空）")
  lateinit var backfill: android.widget.Button

  fun pendingBackfill(seq: Int) = current?.backfills?.any { it.targetPart == seq.toString() && it.status == "pending" } == true

  fun show(view: AmusementInspectionPoints) {
    val done = view.walk.associateBy { it.seq }
    current = view
    next = view.points.firstOrNull { it.seq !in done }
    first = view.points.firstOrNull()
    scanned = null
    progress.text = when {
      view.points.isEmpty() -> "该项目没有配置巡检点位，请在后台安全巡检页配置点位清单"
      else -> view.points.joinToString("\n") { p ->
        val row = done[p.seq]
        val latest = view.backfills.lastOrNull { it.targetPart == p.seq.toString() }
        "${p.seq}. ${p.pointName}　" + when {
          row != null -> (if (row.result == "pass") "通过" else "不通过") + (if (row.backfillId != null) "（人工补录）" else "") +
            (row.checkedAt?.let { "　${localTimeZh(it)}" } ?: "")
          latest?.status == "pending" -> "补录待复核"
          latest?.status == "rejected" -> "补录已驳回"
          p.seq == next?.seq -> "← 下一个"
          else -> "未记录"
        }
      } + if (next == null) "\n全部点位已走完，请在后台安全巡检页新建巡检" else ""
    }
    status.text = next?.let {
      if (pendingBackfill(it.seq)) "第 ${it.seq} 个点位补录待复核，主管复核通过后才能记下一个点位" else "请到第 ${it.seq} 个点位（${it.pointName}）扫码"
    } ?: if (view.points.isEmpty()) "该项目没有配置巡检点位" else "全部点位已走完，请在后台安全巡检页新建巡检"
    backfill.visibility = if (next?.let { !pendingBackfill(it.seq) } == true) android.view.View.VISIBLE else android.view.View.GONE
  }

  fun load(pid: String) {
    status.text = "正在读取巡检点位…"
    runBackground(
      action = { api.getAmusementInspectionPoints(pid) },
      onSuccess = { show(it); pendingNotice?.let { text -> status.text = text; pendingNotice = null } },
      onError = { status.text = "读取失败：${friendly(it)}" },
    )
  }

  runBackground(
    action = { api.listAmusementProjects() },
    onSuccess = { rows ->
      status.text = "请选择游乐项目"
      fillSelectableRows(projects, rows.map { p -> (p.projectName ?: p.id) to { projectId = p.id; load(p.id) } }, "暂无游乐项目")
      initialProjectId?.let { projectId = it; load(it) }
    },
    onError = { status.text = "读取失败：${friendly(it)}" },
  )

  val scan = button("扫点位码") {
    val point = next ?: run { status.text = "没有待记录的点位"; return@button }
    launchScan(null, "对准第 ${point.seq} 个点位（${point.pointName}）上贴的巡检码") { code ->
      scanned = code.trim()
      status.text = "已扫码，请选择检查结果后提交第 ${point.seq} 个点位（${point.pointName}）"
    }
  }
  backfill = button("补录本点位（扫码缺失，须附证据、主管复核）") {
    val pid = projectId ?: run { status.text = "请先选择游乐项目"; return@button }
    val point = next ?: run { status.text = "没有待记录的点位"; return@button }
    showAmusementInspectionPointBackfill(pid, point)
  }.apply { visibility = android.view.View.GONE }
  val restart = button("从第 1 个点位重新走") {
    next = first ?: run { status.text = "请先选择配置了点位的游乐项目"; return@button }
    scanned = null
    status.text = "重新开始：请到第 1 个点位（${first?.pointName}）扫码"
  }
  val submit = button("提交本点位") {
    val pid = projectId ?: run { status.text = "请先选择游乐项目"; return@button }
    val point = next ?: run { status.text = "没有待记录的点位"; return@button }
    val code = scanned ?: run { status.text = "请先扫第 ${point.seq} 个点位的码"; return@button }
    status.text = "正在提交…"
    runBackground(
      action = {
        workflow.checkAmusementInspectionPoint(pid, point.seq, code, if (result.selectedItemPosition == 0) "pass" else "fail",
          note.text.toString().trim().ifEmpty { null })
      },
      onSuccess = { note.setText(""); show(it) },
      onError = { scanned = null; status.text = "记录失败：${friendly(it)}" },
    )
  }
  setContentView(screen(title = "游乐巡检走点", children = listOf(status, statusText("游乐项目"), projects, progress, scan,
    statusText("检查结果"), result, note, submit, backfill, restart, backButton())))
}

/**
 * 第十八批 CF 段第 404 条 404-R2：点位缺扫码补录——点位结果、原时点（默认当前）、原因、责任人（默认本人）、证据（照片、录音、录像，至少一份；选取与上传同交接补录）；
 * 提交后进主管复核，通过后才算该点位已记，回到走点屏重读本项目点位。最终审批人不传（同交接补录）。
 */
internal fun MainActivity.showAmusementInspectionPointBackfill(projectId: String, point: AmusementInspectionPoint) {
  val status = statusText("补录第 ${point.seq} 个点位（${point.pointName}）：须附照片、录音或录像证据，经主管复核通过后才算该点位已记")
  val result = enumSpinner(listOf("通过", "不通过"))
  val date = editText("原时点日期（如 2026-11-24）").apply { setText(java.time.LocalDate.now().toString()) }
  val time = editText("原时点时刻（如 09:30）").apply { setText(java.time.LocalTime.now().withNano(0).toString()) }
  val reason = editText("补录原因")
  val responsible = editText("责任人账号").apply { setText(store.session?.staffId ?: "") }
  val proofs = mutableListOf<String>()
  val proofText = statusText("尚未上传证据")
  val key = java.util.UUID.randomUUID().toString()
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
    startActivityForResult(android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
      type = "*/*"
      addCategory(android.content.Intent.CATEGORY_OPENABLE)
      putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "audio/*", "video/*"))
      putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
    }, TRACE_EVIDENCE_REQUEST)
  }
  val submit = button("提交补录（进主管复核）") {
    val occurred = localInstantOf(date.text.toString(), time.text.toString())
    val why = reason.text.toString().trim()
    val who = responsible.text.toString().trim()
    if (occurred == null) { status.text = "请填写原时点日期和时刻（如 2026-11-24 和 09:30）"; return@button }
    if (why.isEmpty()) { status.text = "请填写补录原因"; return@button }
    if (who.isEmpty()) { status.text = "请填写责任人账号"; return@button }
    if (proofs.isEmpty()) { status.text = "须先上传至少一份证据（照片、录音或录像）"; return@button }
    status.text = "正在提交补录…"
    runBackground(
      action = {
        api.submitAmusementInspectionPointBackfill(projectId, point.seq, AmusementInspectionPointBackfillRequest(key, occurred, why, who,
          proofs.toList(), if (result.selectedItemPosition == 0) "pass" else "fail"))
      },
      onSuccess = { showAmusementInspectionWalk(projectId, "已提交第 ${point.seq} 个点位补录，主管复核通过后生效") },
      onError = { status.text = "补录提交失败：${friendly(it)}" },
    )
  }
  setContentView(screen(title = "巡检点位补录", children = listOf(status, statusText("检查结果"), result, date, time, reason, responsible, pick,
    proofText, submit, button("返回巡检走点") { showAmusementInspectionWalk(projectId) })))
}
