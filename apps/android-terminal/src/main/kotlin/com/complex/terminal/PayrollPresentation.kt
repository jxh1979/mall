package com.complex.terminal

// 文件版本：V0.03

import java.math.BigDecimal

private fun payrollMoney(cent: Long?) = cent?.let { "¥" + BigDecimal.valueOf(it, 2).toPlainString() } ?: "—"
private fun payrollDate(value: String?) = value?.replace(Regex("^(\\d{4})-(\\d{2})-(\\d{2})$"), "$1年$2月$3日") ?: "—"
private fun payrollMonth(value: String?) = value?.replace(Regex("^(\\d{4})-(\\d{2})$"), "$1年$2月") ?: "—"
fun payrollStatusLabel(value: String) = when (value) {
  "closed" -> "已定格待付"
  "partial_paid" -> "部分已付"
  "paid" -> "已付清"
  "no_payment_due" -> "无需支付"
  "pending" -> "待处理"
  "attached" -> "已并入工资"
  "no_difference" -> "无需补差"
  "needs_review" -> "待复核"
  "pending_review" -> "审批中"
  "approved" -> "已批准"
  "rejected" -> "已驳回"
  else -> "待核实"
}
/** 员工端请假记录状态文案（AGENTS.md 铁律 7：展示文字全中文；状态闭集见 V492 chk_leave_status）。 */
fun leaveStatusLabel(value: String?) = when (value) {
  "pending" -> "待审"
  "approved" -> "已通过"
  "rejected" -> "已驳回"
  "revoke_requested" -> "待销假审批"
  "revoked" -> "已销假"
  else -> value ?: "-"
}
fun payrollDetailText(detail: PayrollStatementDetail) = buildString {
  val row = detail.statement
  append("薪资单号：").append(row.id ?: "—").append('\n')
  append("工资月份：").append(payrollMonth(row.month)).append("；")
  append(when (detail.statementKind) { "monthly" -> "普通月结"; "termination" -> "离职结清"; "supplemental" -> "补发"; else -> "待核实" }).append('\n')
  append("状态：").append(payrollStatusLabel(detail.status)).append('\n')
  append("应发净额：").append(payrollMoney(row.totalCent)).append('\n')
  append("实际已付：").append(payrollMoney(detail.paidAmountCent)).append('\n')
  append("工资支付期限：").append(payrollDate(detail.paymentDueDate)).append('\n')
  append("现金处罚扣款：").append(payrollMoney(row.penaltyCent)).append('\n')
  append("代扣个税：").append(payrollMoney(row.taxCent)).append('\n')
  append("现金处罚和个税已计入应发净额，上述扣减仅列示，不再次扣除。绩效减少已反映在对应计薪项目中。\n")
  append("计薪方案：").append(detail.schemeVersionId ?: "—").append("；事实单：").append(detail.factsId ?: "—").append('\n')
  append("逐项计薪（公式内已扣事假仅列示，不再次扣除）：\n")
  detail.lines.forEach { line ->
    append(line.description).append("：").append(payrollMoney(line.amountCent)).append('\n')
    append("  原应计 ").append(payrollMoney(line.grossCent))
    append("；公式内已扣事假 ").append(payrollMoney(line.embeddedLeaveCent))
    append("；另扣事假 ").append(payrollMoney(line.additionalLeaveCent)).append('\n')
    append("  来源：").append(line.sourceId ?: "—").append('\n')
  }
  detail.worktime?.let { work ->
    append("加班：普通日 ").append(payrollMoney(work.ordinaryOvertimeCent))
    append("；休息日 ").append(payrollMoney(work.restDayOvertimeCent))
    append("；法定节假日 ").append(payrollMoney(work.holidayOvertimeCent)).append('\n')
    work.segments.forEach {
      val kind = when (it.kind) { "statutory_holiday" -> "法定节假日"; "rest_day" -> "休息日"; "workday" -> "普通工作日"; "adjusted_workday" -> "补班工作日"; else -> "待核实" }
      append(payrollDate(it.date)).append(" ").append(kind).append("；实际 ").append(it.seconds).append(" 秒；工资月份 ").append(payrollMonth(it.payrollMonth)).append('\n')
    }
  }
  detail.corrections.forEach {
    append("补差 ").append(it.id).append("：").append(payrollMoney(it.amountCent)).append("（").append(payrollStatusLabel(it.status)).append("）\n")
    append("  原工资单 ").append(it.originalStatementId).append("；").append(payrollMonth(it.originalMonth)).append("；并入 ").append(it.targetStatementId ?: "待并入").append('\n')
  }
  if (detail.payments.isEmpty()) append("暂无实际成功付款\n")
  detail.payments.forEach { append("成功付款 ").append(it.financePaymentId).append("：").append(payrollMoney(it.amountCent)).append("；参考号 ").append(it.payRef ?: "—").append("；").append(it.paidAt ?: "—").append('\n') }
  (detail.pendingItems + (detail.worktime?.pendingItems ?: emptyList())).distinct().forEach { append("待处理：").append(it.message).append("（").append(it.sourceId ?: "—").append("）\n") }
}
