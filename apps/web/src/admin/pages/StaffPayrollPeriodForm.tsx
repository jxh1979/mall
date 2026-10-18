// 文件版本：V0.01
import React from "react";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import type { AdminApiClient } from "../types";
import type { PayrollCalendar, PayrollPeriodFacts, PayrollScheme } from "./staffPayrollTypes";
import { payrollCents, payrollNumber } from "./StaffPayrollSchemeForm";

export function StaffPayrollPeriodForm({ api, open, userId, month, schemes, calendars, onClose, onSaved }: { api: AdminApiClient; open: boolean; userId: string; month: string; schemes: PayrollScheme[]; calendars: PayrollCalendar[]; onClose: () => void; onSaved: (row: PayrollPeriodFacts) => void }) {
  const [form, setForm] = React.useState({ schemeVersionId: "", calendarVersionId: "", cycleStart: "", cycleEnd: "", cycleStandardMinutes: "", agreedDueDate: "", statutoryDueDate: "", reason: "" });
  const [facts, setFacts] = React.useState<{ componentCode: string; quantity: string; denominator: string; embeddedLeave: string; appliedLeaveIds: string; evidenceBasis: string }[]>([]);
  const [performance, setPerformance] = React.useState({ aConditionMet: "", targetMet: "", evidenceBasis: "" });
  const [rest, setRest] = React.useState<{ date: string; minutes: string }[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const requestKey = React.useRef("");
  const scheme = schemes.find(row => row.id === form.schemeVersionId);
  React.useEffect(() => { if (open) { setError(""); requestKey.current = crypto.randomUUID(); } }, [open]);
  function selectScheme(id: string) {
    setForm(old => ({ ...old, schemeVersionId: id }));
    const selected = schemes.find(row => row.id === id);
    setFacts((selected?.components ?? []).filter(row => row.method === "per_unit" || row.method === "attendance_ratio").map(row => ({ componentCode: row.code, quantity: "", denominator: "", embeddedLeave: "", appliedLeaveIds: "", evidenceBasis: "" })));
  }
  function body() {
    if (!userId.trim() || !month || !scheme || !form.calendarVersionId || !form.cycleStart || !form.cycleEnd || !form.agreedDueDate || !form.reason.trim()) throw new Error("请填写员工、工资月份、已批准方案和日历、工时周期、工资支付日及事由");
    if (form.cycleStart > form.cycleEnd) throw new Error("工时周期结束日不得早于开始日");
    const componentFacts = facts.map(row => {
      if (!row.evidenceBasis.trim()) throw new Error("请填写各数量或出勤项目的核实依据");
      const embeddedLeaveCent = payrollCents(row.embeddedLeave, "公式内已扣事假");
      const appliedLeaveIds = row.appliedLeaveIds.split(/[，,\s]+/).filter(Boolean);
      if (embeddedLeaveCent > 0 && !appliedLeaveIds.length) throw new Error("公式内已扣事假须关联实际请假单号");
      return { componentCode: row.componentCode, quantity: payrollNumber(row.quantity, "已核实数量"), denominator: payrollNumber(row.denominator, "计算分母"), embeddedLeaveCent, appliedLeaveIds, evidenceBasis: row.evidenceBasis };
    });
    if (scheme.performanceRules && (!performance.aConditionMet || !performance.targetMet || !performance.evidenceBasis.trim())) throw new Error("请明确绩效条件、达标情况和事实依据");
    if (rest.some(row => !row.date) || new Set(rest.map(row => row.date)).size !== rest.length) throw new Error("已补休日期须明确且不能重复");
    return { ...form, idempotencyKey: requestKey.current, userId: userId.trim(), month, cycleStandardMinutes: payrollNumber(form.cycleStandardMinutes, "周期标准分钟", true), statutoryDueDate: form.statutoryDueDate || null, componentFacts,
      performanceFacts: scheme.performanceRules ? { aConditionMet: performance.aConditionMet === "yes", targetMet: performance.targetMet === "yes", evidenceBasis: performance.evidenceBasis } : null,
      restCompensatedMinutes: Object.fromEntries(rest.map(row => [row.date, payrollNumber(row.minutes, "已补休分钟", true)])) };
  }
  function validate() { try { body(); return null; } catch (e) { return (e as Error).message; } }
  async function save() {
    setBusy(true); setError("");
    try { const row = await api.post<PayrollPeriodFacts>("/api/v1/staff/payroll/period-facts", body()); onSaved(row); onClose(); }
    catch (e) { setError(e instanceof Error ? e.message : "期间计薪事实保存失败"); }
    finally { setBusy(false); }
  }
  return <GuidedFlowModal open={open} title="登记期间计薪事实" testId="payroll-period-guided" submitTestId="payroll-period-save" submitLabel="保存事实草案" submitting={busy} error={error || undefined} onClose={onClose} onSubmit={() => void save()}
    steps={[{ title: "期间及工资支付依据", validate, content: <>
      <p>员工：{userId}；月份：{month.replace("-", "年")}月。实际工时由已核实考勤读取，不在此手填出勤分钟。</p>
      <label>已批准薪资方案<select aria-label="已批准薪资方案" value={form.schemeVersionId} onChange={e => selectScheme(e.target.value)}><option value="">请选择</option>{schemes.filter(row => row.status === "approved").map(row => <option key={row.id} value={row.id}>{row.version} · {row.effectiveFrom} · {row.id}</option>)}</select></label>
      <label>已批准计薪日历<select aria-label="已批准计薪日历" value={form.calendarVersionId} onChange={e => setForm(old => ({ ...old, calendarVersionId: e.target.value }))}><option value="">请选择</option>{calendars.filter(row => row.status === "approved").map(row => <option key={row.id} value={row.id}>{row.version} · {row.startDate} 至 {row.endDate} · {row.id}</option>)}</select></label>
      {(Object.entries({ cycleStart: "工时周期开始日期", cycleEnd: "工时周期结束日期", cycleStandardMinutes: "周期标准分钟", agreedDueDate: "约定工资支付日", statutoryDueDate: "法定最迟支付日（适用时填写）", reason: "期间事实申请事由" }) as [keyof typeof form, string][]).map(([key, label]) => <label key={key}>{label}<input aria-label={label} type={key === "reason" || key === "cycleStandardMinutes" ? "text" : "date"} value={form[key]} onChange={e => setForm(old => ({ ...old, [key]: e.target.value }))} /></label>)}
      {facts.map((row, i) => <fieldset key={row.componentCode}><legend>{scheme?.components.find(item => item.code === row.componentCode)?.label ?? row.componentCode}</legend>
        {(["quantity", "denominator", "embeddedLeave", "appliedLeaveIds", "evidenceBasis"] as const).map((key, j) => <label key={key}>{["已核实数量", "计算分母", "公式内已扣事假（元，未扣填0）", "对应请假单号（多个用逗号分隔）", "数量及事假核实依据"][j]}<input aria-label={`事实${i + 1}${["数量", "分母", "已扣事假", "请假单号", "依据"][j]}`} value={row[key]} onChange={e => setFacts(old => old.map((item, n) => n === i ? { ...item, [key]: e.target.value } : item))} /></label>)}
      </fieldset>)}
      {scheme?.performanceRules ? <fieldset><legend>本期绩效事实</legend>{([["aConditionMet", "是否满足A档条件"], ["targetMet", "本期是否达标"]] as const).map(([key, label]) => <label key={key}>{label}<select aria-label={label} value={performance[key]} onChange={e => setPerformance(old => ({ ...old, [key]: e.target.value }))}><option value="">请选择已核实结果</option><option value="yes">是</option><option value="no">否</option></select></label>)}<label>绩效事实依据<input aria-label="绩效事实依据" value={performance.evidenceBasis} onChange={e => setPerformance(old => ({ ...old, evidenceBasis: e.target.value }))} /></label></fieldset> : null}
      {rest.map((row, i) => <fieldset key={i}><legend>已补休记录 {i + 1}</legend><label>对应休息日<input aria-label={`补休${i + 1}日期`} type="date" value={row.date} onChange={e => setRest(old => old.map((item, n) => n === i ? { ...item, date: e.target.value } : item))} /></label><label>已补休分钟<input aria-label={`补休${i + 1}分钟`} value={row.minutes} onChange={e => setRest(old => old.map((item, n) => n === i ? { ...item, minutes: e.target.value } : item))} /></label><button type="button" onClick={() => setRest(old => old.filter((_, n) => n !== i))}>移除补休记录 {i + 1}</button></fieldset>)}
      <button type="button" onClick={() => setRest(old => [...old, { date: "", minutes: "" }])}>增加已核实补休记录</button>
    </> }]} summary={[{ label: "员工及月份", value: `${userId} · ${month}` }, { label: "锁定方案", value: form.schemeVersionId }, { label: "锁定日历", value: form.calendarVersionId }, { label: "工资约定日", value: form.agreedDueDate }, { label: "生效条件", value: "事实原件上传凭证并审批通过后，才能用于正式计薪" }]} />;
}
