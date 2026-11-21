package com.complex.terminal

// 文件版本：V0.01

/**
 * 第十八批 BS 段第 373 条 373-R5（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过；未走完全部点位的巡检不成立、不得标记为已完成」；
 * 第十八批裁决 76「每个点位生成一张码贴在现场，巡检员用工作手机按顺序扫码后记结果，扫错码或跳点都记不上」）：工作手机游乐巡检走点。
 * 选游乐项目后显示下一个待记点位：扫贴在点位上的码、选通过或不通过、填备注后提交；顺序与点位码由服务端判，拒绝文案原样显示。
 * 从第 1 个点位重记即重新开始。走完后在后台安全巡检页新建巡检（检查项取本轮走完的点位）。
 */
internal fun MainActivity.showAmusementInspectionWalk() {
  var projectId: String? = null
  var next: AmusementInspectionPoint? = null
  var first: AmusementInspectionPoint? = null
  var scanned: String? = null
  val status = statusText("正在读取游乐项目…")
  val projects = rowsContainer()
  val progress = statusText("请先选择游乐项目")
  val result = enumSpinner(listOf("通过", "不通过"))
  val note = editText("备注（可空）")

  fun show(view: AmusementInspectionPoints) {
    val done = view.walk.associateBy { it.seq }
    next = view.points.firstOrNull { it.seq !in done }
    first = view.points.firstOrNull()
    scanned = null
    progress.text = when {
      view.points.isEmpty() -> "该项目没有配置巡检点位，请在后台安全巡检页配置点位清单"
      else -> view.points.joinToString("\n") { p ->
        val row = done[p.seq]
        "${p.seq}. ${p.pointName}　" + when {
          row != null -> (if (row.result == "pass") "通过" else "不通过") + (row.checkedAt?.let { "　${localTimeZh(it)}" } ?: "")
          p.seq == next?.seq -> "← 下一个"
          else -> "未记录"
        }
      } + if (next == null) "\n全部点位已走完，请在后台安全巡检页新建巡检" else ""
    }
    status.text = next?.let { "请到第 ${it.seq} 个点位（${it.pointName}）扫码" }
      ?: if (view.points.isEmpty()) "该项目没有配置巡检点位" else "全部点位已走完，请在后台安全巡检页新建巡检"
  }

  fun load(pid: String) {
    status.text = "正在读取巡检点位…"
    runBackground(
      action = { api.getAmusementInspectionPoints(pid) },
      onSuccess = { show(it) },
      onError = { status.text = "读取失败：${friendly(it)}" },
    )
  }

  runBackground(
    action = { api.listAmusementProjects() },
    onSuccess = { rows ->
      status.text = "请选择游乐项目"
      fillSelectableRows(projects, rows.map { p -> (p.projectName ?: p.id) to { projectId = p.id; load(p.id) } }, "暂无游乐项目")
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
    statusText("检查结果"), result, note, submit, restart, backButton())))
}
