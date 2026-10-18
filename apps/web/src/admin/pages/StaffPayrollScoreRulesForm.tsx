// 文件版本：V0.01
import React from "react";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import type { AdminApiClient } from "../types";
import type { PayrollScoreRuleVersion } from "./staffPayrollTypes";
import { payrollNumber } from "./StaffPayrollSchemeForm";

export function StaffPayrollScoreRulesForm({ api, open, projectId, merchantId, ownerCompanyId, onClose, onSaved }: { api: AdminApiClient; open: boolean; projectId: string; merchantId: string; ownerCompanyId: string; onClose: () => void; onSaved: (row: PayrollScoreRuleVersion) => void }) {
  const [date, setDate] = React.useState("");
  const [reason, setReason] = React.useState("");
  const [rules, setRules] = React.useState([{ code: "", points: "" }]);
  const [error, setError] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const key = React.useRef("");
  React.useEffect(() => { if (open) { key.current = crypto.randomUUID(); setError(""); } }, [open]);
  function body() {
    if (!projectId.trim() || !ownerCompanyId.trim() || !date || !reason.trim() || rules.some(row => !row.code.trim())) throw new Error("请填写项目、经营主体、生效日期、事由及每条规则编号");
    if (new Set(rules.map(row => row.code.trim())).size !== rules.length) throw new Error("扣分规则编号不能重复");
    if (rules.some(row => payrollNumber(row.points, "规则分值") <= 0)) throw new Error("扣分分值必须大于零");
    return { idempotencyKey: key.current, projectId: projectId.trim(), merchantId: merchantId.trim() || null, ownerCompanyId: ownerCompanyId.trim(), effectiveFrom: date, reason: reason.trim(), deductions: Object.fromEntries(rules.map(row => [row.code.trim(), payrollNumber(row.points, "规则分值")])) };
  }
  function validate() { try { body(); return null; } catch (e) { return (e as Error).message; } }
  async function save() {
    setBusy(true); setError("");
    try { onSaved(await api.post<PayrollScoreRuleVersion>("/api/v1/staff/payroll/performance-rules", body())); onClose(); }
    catch (e) { setError(e instanceof Error ? e.message : "扣分分值表草案保存失败"); } finally { setBusy(false); }
  }
  return <GuidedFlowModal open={open} title="新建扣分分值表" testId="payroll-score-rules-guided" submitTestId="payroll-score-rules-save" submitLabel="保存分值草案" error={error || undefined} submitting={busy} onClose={onClose} onSubmit={() => void save()}
    steps={[{ title: "经营主体及分值依据", validate, content: <><p>项目：{projectId}；经营主体：{ownerCompanyId}；商户：{merchantId || "项目自有人员"}。保存后上传原件，交实际经营主体总经理或合法授权人批准。</p>
      <label>分值表生效日期<input aria-label="分值表生效日期" type="date" value={date} onChange={e => setDate(e.target.value)} /></label>
      <label>分值表事由<input aria-label="分值表事由" value={reason} onChange={e => setReason(e.target.value)} /></label>
      {rules.map((row, i) => <fieldset key={i}><legend>规则 {i + 1}</legend><label>规则编号<input aria-label={`分值规则${i + 1}编号`} value={row.code} onChange={e => setRules(old => old.map((item, index) => index === i ? { ...item, code: e.target.value } : item))} /></label><label>扣分值<input aria-label={`分值规则${i + 1}分值`} value={row.points} onChange={e => setRules(old => old.map((item, index) => index === i ? { ...item, points: e.target.value } : item))} /></label>{rules.length > 1 ? <button type="button" onClick={() => setRules(old => old.filter((_, index) => index !== i))}>移除规则 {i + 1}</button> : null}</fieldset>)}
      <button type="button" onClick={() => setRules(old => [...old, { code: "", points: "" }])}>增加分值规则</button></> }]}
    summary={[{ label: "经营主体", value: ownerCompanyId }, { label: "规则数量", value: rules.length }, { label: "生效条件", value: "保存不生效；实际经营主体总经理或合法授权人批准后可用于薪资方案" }]} />;
}
