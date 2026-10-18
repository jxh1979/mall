// 文件版本：V0.01
import React from "react";
import { cnDate } from "../components/RowDetailDrawer";
import { payrollDayKind } from "./StaffPayrollDetail";
import type { PayrollCalculation, PayrollLine, PayrollPendingItem, PayrollWorkResult } from "./staffPayrollTypes";

type Snapshot = {
  scheme?: { id: string; paymentBasis: string; worktimeTerms: { approvalBasis: string; calculationBasis: string } };
  facts?: { id: string; cycleStart: string; cycleEnd: string; cycleStandardMinutes: number; reason: string };
  calendar?: { id: string; sourceBasis: string };
  earnings?: { baseCent: number; penaltyRequestedCent: number; penaltyCapCent: number; penaltyCent: number };
  capBps?: number; points?: number; grade?: string; lines?: PayrollLine[];
  worktime?: { scheduledSeconds: number; leaveSeconds: number; attendanceIds: string[]; leaveIds: string[]; result: PayrollWorkResult };
  pending?: PayrollPendingItem[];
};
const money = (cent: number | undefined) => cent == null ? "—" : `${(cent / 100).toFixed(2)} 元`;

export function StaffPayrollCalculationView({ row }: { row: PayrollCalculation }) {
  const snapshot = row.calculationSnapshot as Snapshot;
  return <details><summary>查看税前核算原件 {row.id}</summary>
    <p>税前金额：{money(row.beforeTaxCent)}；发放主体：{row.payerCompanyId}；生成时间：{cnDate(row.createdAt)}。</p>
    <p>方案：{row.schemeVersionId}；计薪事实：{row.factsId}；核算来源校验号：{row.sourceFingerprint}。</p>
    {snapshot?.scheme ? <p>支付约定：{snapshot.scheme.paymentBasis}；工时制度依据：{snapshot.scheme.worktimeTerms.approvalBasis}；计薪依据：{snapshot.scheme.worktimeTerms.calculationBasis}</p> : null}
    {snapshot?.calendar ? <p>日历原件：{snapshot.calendar.id}；{snapshot.calendar.sourceBasis}</p> : null}
    {snapshot?.facts ? <p>工时周期：{cnDate(snapshot.facts.cycleStart)} 至 {cnDate(snapshot.facts.cycleEnd)}，标准 {snapshot.facts.cycleStandardMinutes} 分钟；事实说明：{snapshot.facts.reason}</p> : null}
    {snapshot?.earnings ? <p>普通现金处罚基数（基础工资）：{money(snapshot.earnings.baseCent)}；上限比例：{snapshot.capBps == null ? "—" : `${snapshot.capBps / 100}%`}；申请扣款：{money(snapshot.earnings.penaltyRequestedCent)}；本期上限：{money(snapshot.earnings.penaltyCapCent)}；实际扣款：{money(snapshot.earnings.penaltyCent)}。绩效少发不占该额度。</p> : null}
    {snapshot?.grade ? <p>绩效档位：{snapshot.grade}；绩效分：{snapshot.points}</p> : null}
    {snapshot?.lines ? <table className="admin-table"><thead><tr><th>项目</th><th>原应计</th><th>公式内已扣事假</th><th>另扣事假</th><th>本项金额</th><th>来源原件</th></tr></thead><tbody>{snapshot.lines.map((line, i) => <tr key={line.id || i}><td>{line.description}</td><td>{money(line.grossCent)}</td><td>{money(line.embeddedLeaveCent)}</td><td>{money(line.additionalLeaveCent)}</td><td>{money(line.amountCent)}</td><td>{line.sourceId ?? "—"}</td></tr>)}</tbody></table> : <p>逐项核算原件未返回，请重新读取后核实。</p>}
    {snapshot?.worktime ? <><p>排班 {snapshot.worktime.scheduledSeconds} 秒；事假 {snapshot.worktime.leaveSeconds} 秒。考勤原件：{snapshot.worktime.attendanceIds.join("、") || "无"}；请假原件：{snapshot.worktime.leaveIds.join("、") || "无"}。</p>
      <p>普通日加班 {money(snapshot.worktime.result.ordinaryOvertimeCent)}；休息日加班 {money(snapshot.worktime.result.restDayOvertimeCent)}；法定节假日加班 {money(snapshot.worktime.result.holidayOvertimeCent)}。</p>
      {snapshot.worktime.result.segments.map((part, i) => <p key={i}>{part.shiftId}：{cnDate(part.date)} · {payrollDayKind(part.kind)} · {part.seconds} 秒</p>)}</> : null}
    {snapshot?.pending?.map((item, i) => <p key={i}>待处理：{item.message}（{item.sourceId}）</p>)}
  </details>;
}
