// 文件版本：V0.01
import React from "react";
import { cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import type { PayrollStatementDetail } from "./staffPayrollTypes";

const money = (value?: number | null) => value == null ? "—" : `¥${(value / 100).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
export const payrollStatus = (value: string) => ({
  draft: "草案", pending_review: "审批中", approved: "已批准", rejected: "已驳回", pending: "待处理",
  closed: "已定格待付", partial_paid: "部分已付", paid: "已付清", no_payment_due: "无需支付",
  attached: "已并入工资", no_difference: "无需补差", needs_review: "待复核",
})[value] ?? "待核实";
export const payrollKind = (value: string) => ({ monthly: "普通月结", termination: "离职结清", supplemental: "补发" })[value] ?? "待核实";
export const payrollDayKind = (value: string) => ({ workday: "普通工作日", rest_day: "休息日", statutory_holiday: "法定节假日", adjusted_workday: "补班工作日" })[value] ?? "待核实";

export function payrollDetailFields(detail: PayrollStatementDetail): RowDetailField[] {
  const row = detail.statement;
  return [
    { label: "薪资单号", value: row.id }, { label: "员工", value: row.userId ?? "—" },
    { label: "月份与类型", value: `${row.month?.replace(/^(\d{4})-(\d{2})$/, "$1年$2月") ?? "—"} · ${payrollKind(detail.statementKind)}` },
    { label: "发放主体", value: row.payerCompanyId ?? "—" },
    { label: "发放状态", value: payrollStatus(detail.status) },
    { label: "应发净额", value: money(row.totalCent) }, { label: "实际已付", value: money(detail.paidAmountCent) },
    { label: "工资支付期限", value: cnDate(detail.paymentDueDate) }, { label: "工资应付台账", value: detail.arApItemId ?? "尚未生成" },
    { label: "锁定薪资方案", value: detail.schemeVersionId ?? "—" }, { label: "锁定计薪事实", value: detail.factsId ?? "—" },
    { label: "现金处罚扣款", value: money(row.penaltyCent) },
    { label: "代扣个税", value: money(row.taxCent) }, { label: "锁定税务认定", value: row.taxAssessmentId ?? "—" },
    { label: "锁定税表版本", value: row.taxVersionId ?? "—" },
    { label: "对账说明", value: "现金处罚和个税已计入应发净额，上述扣减仅列示，不再次扣除。绩效减少已反映在对应计薪项目中。" },
    { label: "逐项计薪（金额为元）", value: <><p>公式内已扣事假只列示，不再次扣除。普通现金处罚仅按基础工资上限计算，绩效减少不占该额度。</p>
      <table className="admin-table"><thead><tr><th>项目</th><th>原应计</th><th>公式内已扣事假</th><th>另扣事假</th><th>本项金额</th><th>来源单据</th></tr></thead><tbody>
        {detail.lines.map(line => <tr key={line.id}><td>{line.description}</td><td>{money(line.grossCent)}</td><td>{money(line.embeddedLeaveCent)}</td><td>{money(line.additionalLeaveCent)}</td><td>{money(line.amountCent)}</td><td>{line.sourceId || "—"}</td></tr>)}
      </tbody></table></> },
    { label: "实际工时及加班", value: detail.worktime ? <>
      <p>普通日加班 {money(detail.worktime.ordinaryOvertimeCent)}；休息日加班 {money(detail.worktime.restDayOvertimeCent)}；法定节假日加班 {money(detail.worktime.holidayOvertimeCent)}</p>
      <table className="admin-table"><thead><tr><th>班次</th><th>实际日期</th><th>日期性质</th><th>实际小时</th><th>工资月份</th></tr></thead><tbody>
        {detail.worktime.segments.map((segment, index) => <tr key={index}><td>{segment.shiftId}</td><td>{cnDate(segment.date)}</td><td>{payrollDayKind(segment.kind)}</td><td>{(segment.seconds / 3600).toLocaleString("zh-CN", { maximumFractionDigits: 4 })}</td><td>{segment.payrollMonth.replace("-", "年")}月</td></tr>)}
      </tbody></table>{detail.worktime.pendingItems.map((item, i) => <p key={i}>{item.message}（{item.sourceId}）</p>)}
    </> : "本单无已核定工时明细" },
    { label: "工资补差", value: detail.corrections.length ? <table className="admin-table"><thead><tr><th>补差单</th><th>原工资单</th><th>原月份</th><th>实际补差</th><th>状态</th><th>并入工资单</th><th>审批单</th></tr></thead><tbody>
      {detail.corrections.map(item => <tr key={item.id}><td>{item.id}</td><td>{item.originalStatementId}</td><td>{item.originalMonth.replace("-", "年")}月</td><td>{money(item.amountCent)}</td><td>{payrollStatus(item.status)}</td><td>{item.targetStatementId ?? "待并入"}</td><td>{item.sourceReviewId}</td></tr>)}
    </tbody></table> : "无补差记录" },
    { label: "实际付款记录", value: detail.payments.length ? <table className="admin-table"><thead><tr><th>财务付款单</th><th>实际金额</th><th>银行参考号</th><th>成功时间</th></tr></thead><tbody>
      {detail.payments.map(payment => <tr key={payment.id}><td>{payment.financePaymentId}</td><td>{money(payment.amountCent)}</td><td>{payment.payRef}</td><td>{cnDate(payment.paidAt)}</td></tr>)}
    </tbody></table> : "暂无实际成功付款" },
    { label: "待处理事项", value: detail.pendingItems.length ? detail.pendingItems.map((item, i) => <p key={i}>{item.message}（{item.sourceId}）</p>) : "无" },
  ];
}
