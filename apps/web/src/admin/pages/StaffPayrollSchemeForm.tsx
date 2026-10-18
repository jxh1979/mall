// 文件版本：V0.01
import React from "react";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import type { AdminApiClient } from "../types";
import type { PayrollScheme, PayrollScoreRuleVersion } from "./staffPayrollTypes";

const componentTypes = { base: "基础工资", allowance: "补贴", full_attendance: "全勤", performance: "绩效", bonus: "奖金" };
const methods = { fixed_monthly: "固定月额", per_unit: "按核实数量计发", attendance_ratio: "按核实出勤比例计发", performance: "绩效公式" };
const units = { month: "月", day: "天", hour: "小时", shift: "班", unit: "项" };
type ComponentForm = { code: string; label: string; type: string; method: string; amount: string; unit: string; leaveMode: string; calculationBasis: string };
const blankComponent = (): ComponentForm => ({ code: "", label: "", type: "", method: "", amount: "", unit: "", leaveMode: "", calculationBasis: "" });
export function payrollNumber(value: string, label: string, integer = false) {
  if (!value.trim() || !Number.isFinite(Number(value)) || Number(value) < 0 || (integer && !Number.isSafeInteger(Number(value)))) throw new Error(`请明确填写${label}，须为非负${integer ? "整数" : "数值"}`);
  return Number(value);
}
export function payrollCents(value: string, label: string) {
  if (!/^\d+(\.\d{1,2})?$/.test(value.trim())) throw new Error(`${label}须为非负金额，最多两位小数`);
  const amount = Math.round(Number(value) * 100);
  if (!Number.isSafeInteger(amount)) throw new Error(`${label}超出允许范围`);
  return amount;
}
function options(labels: Record<string, string>) { return <><option value="">请选择</option>{Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</>; }

export function StaffPayrollSchemeForm({ api, open, scoreRuleVersions, onClose, onSaved }: { api: AdminApiClient; open: boolean; scoreRuleVersions: PayrollScoreRuleVersion[]; onClose: () => void; onSaved: (row: PayrollScheme) => void }) {
  const [form, setForm] = React.useState({ userId: "", effectiveFrom: "", effectiveTo: "", positionCode: "", paymentBasis: "", reason: "" });
  const [components, setComponents] = React.useState<ComponentForm[]>([blankComponent()]);
  const [work, setWork] = React.useState({ system: "", standardDailyMinutes: "", standardWeeklyMinutes: "", hourlyBase: "", ordinaryOvertimeBps: "", restDayOvertimeBps: "", holidayOvertimeBps: "", approvalBasis: "", calculationBasis: "" });
  const [performance, setPerformance] = React.useState({ basePoints: "", minPoints: "", maxPoints: "", gradeA: "", gradeB: "", gradeC: "", aCondition: "", cMissedPeriods: "", assessmentPeriodMonths: "", deductionRuleReviewId: "" });
  const approvedRules = scoreRuleVersions.filter(row => row.status === "approved" && row.reviewId);
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const requestKey = React.useRef("");
  React.useEffect(() => { if (open) { setError(""); requestKey.current = crypto.randomUUID(); } }, [open]);
  const hasPerformance = components.some(row => row.type === "performance");
  function body() {
    if (![form.userId, form.effectiveFrom, form.positionCode, form.paymentBasis, form.reason].every(v => v.trim())) throw new Error("请填写员工账号、生效日期、岗位、工资支付约定和申请事由");
    if (form.effectiveTo && form.effectiveTo < form.effectiveFrom) throw new Error("方案结束日期不得早于生效日期");
    if (components.some(row => Object.values(row).some(value => !value.trim()))) throw new Error("每个薪资项目的类型、算法、单位、金额、事假口径和计算依据均须填写");
    if (new Set(components.map(row => row.code.trim())).size !== components.length) throw new Error("薪资项目编号不能重复");
    if (![work.system, work.approvalBasis, work.calculationBasis].every(v => v.trim())) throw new Error("请填写工时制度及批准、计薪依据");
    if (hasPerformance && !performance.aCondition.trim()) throw new Error("请填写绩效条件");
    const scoreRule = approvedRules.find(row => row.reviewId === performance.deductionRuleReviewId);
    if (performance.deductionRuleReviewId && !scoreRule) throw new Error("请选择已批准的扣分分值原件");
    return {
      idempotencyKey: requestKey.current, ...form, userId: form.userId.trim(), effectiveTo: form.effectiveTo || null,
      components: components.map(({ amount, ...row }) => ({ ...row, code: row.code.trim(), amountCent: payrollCents(amount, row.label) })),
      worktimeTerms: { system: work.system, standardDailyMinutes: payrollNumber(work.standardDailyMinutes, "每日标准分钟", true), standardWeeklyMinutes: payrollNumber(work.standardWeeklyMinutes, "每周标准分钟", true), hourlyBaseCent: payrollCents(work.hourlyBase, "小时计薪基数"), ordinaryOvertimeBps: payrollNumber(work.ordinaryOvertimeBps, "普通日加班万分比", true), restDayOvertimeBps: payrollNumber(work.restDayOvertimeBps, "休息日加班万分比", true), holidayOvertimeBps: payrollNumber(work.holidayOvertimeBps, "法定节假日加班万分比", true), approvalBasis: work.approvalBasis, calculationBasis: work.calculationBasis },
      performanceRules: hasPerformance ? {
        basePoints: payrollNumber(performance.basePoints, "绩效基础分"), minPoints: payrollNumber(performance.minPoints, "绩效分下限"), maxPoints: payrollNumber(performance.maxPoints, "绩效分上限"),
        gradeCoefficients: { A: payrollNumber(performance.gradeA, "A档倍率"), B: payrollNumber(performance.gradeB, "B档倍率"), C: payrollNumber(performance.gradeC, "C档倍率") }, aCondition: performance.aCondition, cMissedPeriods: payrollNumber(performance.cMissedPeriods, "C档连续未达标周期", true), assessmentPeriodMonths: payrollNumber(performance.assessmentPeriodMonths, "绩效周期月数", true),
        deductionRules: scoreRule?.deductions ?? {}, deductionRuleReviewId: scoreRule?.reviewId ?? null,
      } : null,
    };
  }
  function validate() { try { body(); return null; } catch (e) { return (e as Error).message; } }
  async function save() {
    setBusy(true); setError("");
    try { const row = await api.post<PayrollScheme>("/api/v1/staff/payroll/schemes", body()); onSaved(row); onClose(); }
    catch (e) { setError(e instanceof Error ? e.message : "薪资方案草案保存失败"); }
    finally { setBusy(false); }
  }
  const field = (key: keyof typeof form, label: string, type = "text") => <label>{label}<input aria-label={label} type={type} value={form[key]} onChange={e => setForm(old => ({ ...old, [key]: e.target.value }))} /></label>;
  const workField = (key: keyof typeof work, label: string, type = "text") => <label>{label}<input aria-label={label} type={type} value={work[key]} onChange={e => setWork(old => ({ ...old, [key]: e.target.value }))} /></label>;
  return <GuidedFlowModal open={open} title="新建薪资方案" testId="payroll-base-guided" submitTestId="base-save" submitLabel="保存草案" submitting={busy} error={error || undefined} onClose={onClose} onSubmit={() => void save()}
    steps={[
      { title: "员工及适用约定", content: <>{field("userId", "方案员工账号")}{field("positionCode", "适用岗位编号")}{field("effectiveFrom", "方案生效日期", "date")}{field("effectiveTo", "方案结束日期（可空）", "date")}{field("paymentBasis", "工资支付约定")}{field("reason", "方案申请事由")}<p>保存草案后上传真实依据，再提交审批；批准前不参与计薪。</p></> },
      { title: "逐项薪资", content: <>{components.map((row, i) => <fieldset key={i}><legend>薪资项目 {i + 1}</legend>
        {(["code", "label", "amount", "calculationBasis"] as const).map((key, j) => <label key={key}>{["项目编号", "项目名称", "金额或单价（元）", "计算依据"][j]}<input aria-label={`项目${i + 1}${["编号", "名称", "金额", "依据"][j]}`} value={row[key]} onChange={e => setComponents(old => old.map((item, n) => n === i ? { ...item, [key]: e.target.value } : item))} /></label>)}
        {([["type", "项目类型", componentTypes], ["method", "计算方式", methods], ["unit", "计量单位", units], ["leaveMode", "事假口径", { proportional: "按事假比例处理", exempt: "不扣事假" }]] as const).map(([key, label, labels]) => <label key={key}>{label}<select aria-label={`项目${i + 1}${label}`} value={row[key]} onChange={e => setComponents(old => old.map((item, n) => n === i ? { ...item, [key]: e.target.value } : item))}>{options(labels)}</select></label>)}
        {components.length > 1 ? <button type="button" onClick={() => setComponents(old => old.filter((_, n) => n !== i))}>移除项目 {i + 1}</button> : null}</fieldset>)}
        <button type="button" onClick={() => setComponents(old => [...old, blankComponent()])}>增加薪资项目</button></> },
      { title: "工时和绩效依据", validate, content: <>
        <label>工时制度<select aria-label="工时制度" value={work.system} onChange={e => setWork(old => ({ ...old, system: e.target.value }))}>{options({ standard: "标准工时", comprehensive: "综合计算工时", flexible: "不定时工时" })}</select></label>
        {workField("standardDailyMinutes", "每日标准分钟", "number")}{workField("standardWeeklyMinutes", "每周标准分钟", "number")}{workField("hourlyBase", "小时计薪基数（元）")}
        {workField("ordinaryOvertimeBps", "普通日加班比例（万分比）", "number")}{workField("restDayOvertimeBps", "休息日加班比例（万分比）", "number")}{workField("holidayOvertimeBps", "法定节假日加班比例（万分比）", "number")}
        {workField("approvalBasis", "工时制度批准依据")}{workField("calculationBasis", "工时计薪依据")}
        {hasPerformance ? <fieldset><legend>绩效规则</legend><p>倍率按倍数填写，例如 1 表示一倍；须填写已批准规则，不根据职级推定。</p>
          {(Object.entries({ basePoints: "绩效基础分", minPoints: "绩效分下限", maxPoints: "绩效分上限", gradeA: "A档倍率", gradeB: "B档倍率", gradeC: "C档倍率", aCondition: "A档条件", cMissedPeriods: "C档连续未达标周期", assessmentPeriodMonths: "绩效周期月数" }) as [keyof typeof performance, string][]).map(([key, label]) => <label key={key}>{label}<input aria-label={label} value={performance[key]} onChange={e => setPerformance(old => ({ ...old, [key]: e.target.value }))} /></label>)}
          <label>已批准经营主体扣分表<select aria-label="已批准经营主体扣分表" value={performance.deductionRuleReviewId} onChange={e => setPerformance(old => ({ ...old, deductionRuleReviewId: e.target.value }))}><option value="">不启用违规扣分</option>{approvedRules.map(row => <option key={row.id} value={row.reviewId!}>{row.ownerCompanyId} · {row.version} · {row.id}</option>)}</select></label>
          <p>从已读取的批准原件选择；规则编号及分值保持原件，不手填审批号。</p></fieldset> : null}
      </> },
    ]} summary={[{ label: "员工", value: form.userId }, { label: "适用期间", value: `${form.effectiveFrom} 至 ${form.effectiveTo || "未指定终止日"}` }, { label: "薪资项目", value: components.map(row => `${row.label}：${row.amount}元`).join("；") }, { label: "生效条件", value: "上传依据并完成审批后生效" }]} />;
}
