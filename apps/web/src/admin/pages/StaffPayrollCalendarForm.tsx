// 文件版本：V0.01
import React from "react";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { cnDate } from "../components/RowDetailDrawer";
import type { AdminApiClient } from "../types";
import type { PayrollCalendar } from "./staffPayrollTypes";

export const payrollDayKinds: Record<string, string> = { workday: "普通工作日", adjusted_workday: "补班工作日", rest_day: "休息日", statutory_holiday: "法定节假日" };
export function StaffPayrollCalendarForm({ api, open, projectId, onClose, onSaved }: { api: AdminApiClient; open: boolean; projectId: string; onClose: () => void; onSaved: (row: PayrollCalendar) => void }) {
  const [form, setForm] = React.useState({ startDate: "", endDate: "", sourceBasis: "", reason: "" });
  const [days, setDays] = React.useState<{ date: string; kind: string }[]>([]);
  const [bulkKind, setBulkKind] = React.useState("");
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const requestKey = React.useRef("");
  React.useEffect(() => { if (open) { setError(""); requestKey.current = crypto.randomUUID(); } }, [open]);
  function buildDays() {
    if (!form.startDate || !form.endDate || form.startDate > form.endDate) { setError("请填写有效日历起止日期"); return; }
    const rows: { date: string; kind: string }[] = [];
    const cursor = new Date(form.startDate + "T00:00:00Z");
    const end = new Date(form.endDate + "T00:00:00Z");
    if (!Number.isFinite(cursor.getTime()) || !Number.isFinite(end.getTime())) { setError("日历日期无效"); return; }
    while (cursor <= end) { const date = cursor.toISOString().slice(0, 10); rows.push({ date, kind: days.find(day => day.date === date)?.kind ?? "" }); cursor.setUTCDate(cursor.getUTCDate() + 1); }
    setDays(rows); setError("");
  }
  const validate = () => !projectId.trim() || Object.values(form).some(value => !value.trim()) || !days.length || days.some(day => !day.kind) || days[0]?.date !== form.startDate || days.at(-1)?.date !== form.endDate ? "请填写项目、日历依据和事由，并生成、核实范围内每一天的性质" : null;
  async function save() {
    const message = validate(); if (message) { setError(message); return; }
    setBusy(true); setError("");
    try { const row = await api.post<PayrollCalendar>("/api/v1/staff/payroll/calendars", { ...form, idempotencyKey: requestKey.current, projectId: projectId.trim(), days }); onSaved(row); onClose(); }
    catch (e) { setError(e instanceof Error ? e.message : "日历草案保存失败"); }
    finally { setBusy(false); }
  }
  return <GuidedFlowModal open={open} title="新建计薪日历" testId="payroll-calendar-guided" submitTestId="payroll-calendar-save" submitLabel="保存日历草案" submitting={busy} error={error || undefined} onClose={onClose} onSubmit={() => void save()}
    steps={[{ title: "逐日认定", validate, content: <><p>项目：{projectId}。休假安排不能直接代替法定节假日认定，请依据正式文件逐日核实。</p>
      {(Object.entries({ startDate: "日历开始日期", endDate: "日历结束日期", sourceBasis: "日历来源依据", reason: "日历申请事由" }) as [keyof typeof form, string][]).map(([key, label]) => <label key={key}>{label}<input aria-label={label} type={key.endsWith("Date") ? "date" : "text"} value={form[key]} onChange={e => setForm(old => ({ ...old, [key]: e.target.value }))} /></label>)}
      <button type="button" onClick={buildDays}>生成逐日日历</button>
      {days.length ? <><label>批量设置日期性质<select aria-label="批量日期性质" value={bulkKind} onChange={e => setBulkKind(e.target.value)}><option value="">请选择</option>{Object.entries(payrollDayKinds).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><button type="button" disabled={!bulkKind} onClick={() => setDays(old => old.map(day => ({ ...day, kind: bulkKind })))}>将以上性质应用全部日期</button>
      {days.map((day, i) => <label key={day.date}>{cnDate(day.date)}<select aria-label={`日期性质${day.date}`} value={day.kind} onChange={e => setDays(old => old.map((item, n) => n === i ? { ...item, kind: e.target.value } : item))}><option value="">待核实</option>{Object.entries(payrollDayKinds).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>)}</> : null}
    </> }]} summary={[{ label: "项目", value: projectId }, { label: "范围", value: `${cnDate(form.startDate)} 至 ${cnDate(form.endDate)}，共${days.length}天` }, { label: "来源", value: form.sourceBasis }, { label: "生效条件", value: "草案上传依据，审批通过后才用于工时和工资期限计算" }]} />;
}
