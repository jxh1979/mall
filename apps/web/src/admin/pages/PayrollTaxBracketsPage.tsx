// 文件版本：V0.02
import React from "react";
import { StaffPayrollCalculationView } from "./StaffPayrollCalculationView";
import { withBase } from "../../basePath";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { useAdminResource } from "../hooks/useAdminResource";
import { PermissionButton } from "../security/Can";
import type { PayrollCalculation } from "./staffPayrollTypes";
import { payrollCents } from "./StaffPayrollSchemeForm";
import type { AdminPageProps } from "../types";

type Bracket = { seq: number; incomeFloorCent: number; incomeCeilingCent: number | null; incomeBaseBps: number; flatDeductionCent: number; rateBps: number; quickDeductionCent: number };
type Proposal = { id: string; category: string; reason: string; status: string; reviewId: string | null; evidenceAttachmentIds: string[] };
type Version = Proposal & { version: string; effectiveFrom: string; applicabilityBasis: string; calculationBasis: string; roundingRule: string; brackets: Bracket[] };
type Assessment = Proposal & { payrollCalculationId?: string | null; professionalCalculationReference?: string | null; userId: string; month: string; taxDate: string; identityBasis: string; transactionBasis: string; professionalBasis: string; payerCompanyId: string; incomeCent: number; taxCent: number; taxVersionId: string; sourceLines: { id: string; sourceId: string; amountCent: number }[] };
type BracketForm = { floor: string; ceiling: string; base: string; flat: string; rate: string; quick: string };
const blankBracket = (): BracketForm => ({ floor: "", ceiling: "", base: "", flat: "", rate: "", quick: "" });
const MANAGE = ["staff.tax_bracket.manage"];
const ROOT = "/api/v1/staff/payroll";
const statusLabels: Record<string, string> = { draft: "草案", pending_review: "待财税法及最高确认", approved: "已批准", rejected: "已驳回（原件保留）" };
const lineLabels = { finance_review: "财务最终审批人", tax_review: "税务最终审批人", legal_review: "法务最终审批人" };
const yuan = (value: number) => (value / 100).toFixed(2);

export function PayrollTaxBracketsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [versions, setVersions] = React.useState<Version[]>([]);
  const [assessments, setAssessments] = React.useState<Assessment[]>([]);
  const [calculations, setCalculations] = React.useState<PayrollCalculation[]>([]);
  const [payrollTax, setPayrollTax] = React.useState({ calculationId: "", amount: "", reference: "" });
  const [busy, setBusy] = React.useState(false);
  const [formError, setFormError] = React.useState("");
  const [createOpen, setCreateOpen] = React.useState(false);
  const [assessmentOpen, setAssessmentOpen] = React.useState(false);
  const [form, setForm] = React.useState({ category: "", effectiveFrom: "", applicabilityBasis: "", calculationBasis: "", roundingRule: "", reason: "" });
  const [brackets, setBrackets] = React.useState<BracketForm[]>([blankBracket()]);
  const [employee, setEmployee] = React.useState("");
  const [month, setMonth] = React.useState("");
  const [assessment, setAssessment] = React.useState({ category: "", taxDate: "", identityBasis: "", transactionBasis: "", professionalBasis: "", reason: "" });
  const [target, setTarget] = React.useState<{ type: "version" | "assessment"; row: Proposal } | null>(null);
  const [files, setFiles] = React.useState<string[]>([]);
  const [reviewers, setReviewers] = React.useState({ finance_review: "", tax_review: "", legal_review: "" });
  const { loading, error, reload } = useAdminResource(async () => {
    setVersions(await api.get<Version[]>(`${ROOT}/tax-versions`));
  }, { onError: setGlobalError, fallbackMessage: "税表版本读取失败" });

  async function run(action: () => Promise<unknown>) {
    setBusy(true); setFormError(""); setGlobalError(null);
    try { const result = await action(); setActionResult(result); return true; }
    catch (e) { const message = e instanceof Error ? e.message : "税务操作失败"; setFormError(message); setGlobalError(message); return false; }
    finally { setBusy(false); }
  }
  function versionError() {
    if (Object.values(form).some(value => !value.trim())) return "请填写税目、生效日期、适用依据、计算及舍入依据和事由";
    let next = 0;
    for (const [i, b] of brackets.entries()) {
      if ([b.floor, b.base, b.flat, b.rate, b.quick].some(value => !value.trim())) return "每档金额和比例都须明确填写，0也须填写";
      const values = [b.floor, b.base, b.flat, b.rate, b.quick].map(Number);
      if (values.some(value => !Number.isSafeInteger(value) || value < 0) || Number(b.base) < 1 || Number(b.base) > 10000 || Number(b.rate) > 10000) return "金额须为非负整数分，基数比例须为1至10000，税率须为0至10000";
      if (Number(b.floor) !== next) return "完整税表须从0分连续覆盖，不能缺口或重叠";
      if (i === brackets.length - 1) { if (b.ceiling.trim()) return "最后一档上界须留空，表示无上限"; }
      else { const ceiling = Number(b.ceiling); if (!b.ceiling.trim() || !Number.isSafeInteger(ceiling) || ceiling < next) return "中间档位必须填写合法上界"; next = ceiling + 1; }
    }
    return null;
  }
  async function createVersion() {
    if (await run(async () => {
      const message = versionError(); if (message) throw new Error(message);
      const row = await api.post<Version>(`${ROOT}/tax-versions`, { ...form, idempotencyKey: crypto.randomUUID(), brackets: brackets.map((b, i) => ({ seq: i + 1, incomeFloorCent: Number(b.floor), incomeCeilingCent: b.ceiling.trim() ? Number(b.ceiling) : null, incomeBaseBps: Number(b.base), flatDeductionCent: Number(b.flat), rateBps: Number(b.rate), quickDeductionCent: Number(b.quick) })) });
      return { action: "payroll_tax_version_drafted", id: row.id, status: row.status };
    })) { setCreateOpen(false); reload(); }
  }
  async function loadAssessments() {
    await run(async () => {
      if (!employee.trim() || !month) throw new Error("请先填写员工账号和工资月份");
      const query = `?user_id=${encodeURIComponent(employee.trim())}&month=${encodeURIComponent(month)}`;
      const [rows, drafts] = await Promise.all([api.get<Assessment[]>(`${ROOT}/tax-assessments${query}`), api.get<PayrollCalculation[]>(`${ROOT}/calculations${query}`)]);
      setAssessments(rows); setCalculations(drafts);
      return { action: "payroll_tax_assessments_loaded" };
    });
  }
  function assessmentError() {
    if (!employee.trim() || !month || Object.values(assessment).some(value => !value.trim())) return "请填写员工、月份、税目、适用日期及完整专业认定依据";
    if (payrollTax.calculationId) {
      const calculation = calculations.find(row => row.id === payrollTax.calculationId);
      if (!calculation || !payrollTax.reference.trim()) return "请选择真实核算原件并填写专业核税计算说明";
      try { if (payrollCents(payrollTax.amount, "专业确认个税") > calculation.beforeTaxCent) return "专业核定个税不能超过本次税前金额"; }
      catch (e) { return (e as Error).message; }
    }
    return null;
  }
  async function createAssessment() {
    if (await run(async () => {
      const message = assessmentError(); if (message) throw new Error(message);
      const row = await api.post<Assessment>(`${ROOT}/tax-assessments`, { ...assessment, userId: employee.trim(), month, idempotencyKey: crypto.randomUUID(), ...(payrollTax.calculationId ? { payrollCalculationId: payrollTax.calculationId, professionalTaxCent: payrollCents(payrollTax.amount, "专业确认个税"), professionalCalculationReference: payrollTax.reference.trim() } : {}) });
      setAssessments(old => [row, ...old]); return { action: "payroll_tax_assessment_drafted", id: row.id, status: row.status };
    })) setAssessmentOpen(false);
  }
  async function prepareSubmit(type: "version" | "assessment", row: Proposal) {
    setTarget({ type, row }); setFiles(row.evidenceAttachmentIds ?? []); setFormError("");
    await run(async () => {
      const timeline = await api.get<{ itemType: string; referenceId: string; result: string }[]>(`/api/v1/evidence/timelines/payroll_tax_${type}/${encodeURIComponent(row.id)}`);
      setFiles([...new Set([...(row.evidenceAttachmentIds ?? []), ...timeline.filter(item => item.itemType === "attachment" && item.result === "uploaded").map(item => item.referenceId)])]);
      return { action: "payroll_tax_evidence_loaded", id: row.id };
    });
  }
  async function upload(input: FileList | null) {
    if (!input?.length || !target) return;
    await run(async () => {
      if (!api.postForm) throw new Error("当前客户端不支持凭证上传");
      const ids: string[] = [];
      for (const file of Array.from(input)) {
        const data = new FormData(); data.append("file", file); data.append("business_type", `payroll_tax_${target.type}`); data.append("business_id", target.row.id); data.append("attachment_role", "professional_basis");
        ids.push((await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", data)).id);
      }
      setFiles(old => [...old, ...ids]); return { action: "payroll_tax_evidence_uploaded", count: ids.length };
    });
  }
  async function submit() {
    if (!target) return;
    if (await run(async () => {
      if (!files.length || Object.values(reviewers).some(value => !value.trim())) throw new Error("请上传专业依据，并填写财务、税务、法务各线的最终审批人");
      const row = await api.post<Proposal>(`${ROOT}/tax-${target.type === "version" ? "versions" : "assessments"}/${encodeURIComponent(target.row.id)}/submit`, { idempotencyKey: crypto.randomUUID(), evidenceAttachmentIds: files, parallelLines: Object.entries(reviewers).map(([lineCode, finalApproverId]) => ({ lineCode, finalApproverId: finalApproverId.trim() })) });
      if (target.type === "assessment") setAssessments(old => old.map(item => item.id === row.id ? { ...item, ...row } : item));
      return { action: "payroll_tax_submitted", id: row.id, reviewId: row.reviewId, status: row.status };
    })) { setTarget(null); reload(); }
  }
  const field = (key: keyof typeof form, label: string, type = "text") => <label>{label}<input aria-label={label} type={type} value={form[key]} onChange={e => setForm(old => ({ ...old, [key]: e.target.value }))} /></label>;
  const assessmentField = (key: keyof typeof assessment, label: string, type = "text") => <label>{label}<input aria-label={label} type={type} value={assessment[key]} onChange={e => setAssessment(old => ({ ...old, [key]: e.target.value }))} /></label>;
  const evidenceLinks = (row: Proposal) => (row.evidenceAttachmentIds ?? []).map((id, i) => <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">专业凭证 {i + 1} </a>);
  const operations = (type: "version" | "assessment", row: Proposal) => <>{row.status === "draft" ? <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busy} onClick={() => void prepareSubmit(type, row)}>准备审批</PermissionButton> : null}{row.reviewId ? <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>查看审批链</a> : null}</>;

  return <section data-testid="payroll-tax-brackets-page">
    <AdminPageHeader title="税表版本与税务认定" description="完整税表和手工认定均须财务、税务、法务审核，再由最高权限确认。工资锁定批准认定与税表；本页不替代专业财税软件或正式申报。" />
    {formError && !createOpen && !assessmentOpen && !target ? <p role="alert">{formError}</p> : null}
    <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="payroll-tax-brackets-create-open" onClick={() => { setFormError(""); setCreateOpen(true); }}>＋ 新建完整税表版本</PermissionButton>
      <table className="admin-table" data-testid="payroll-tax-brackets-table"><thead><tr><th>税目及版本</th><th>适用日期</th><th>状态</th><th>内容与操作</th></tr></thead><tbody>
        {versions.map(row => <tr key={row.id}><td>{row.category} · {row.version}</td><td>{row.effectiveFrom} 起适用</td><td>{statusLabels[row.status] ?? row.status}</td><td><details><summary>完整税表与依据</summary><p>适用对象：{row.applicabilityBasis}</p><p>计算及舍入依据：{row.calculationBasis}</p><p>事由：{row.reason}</p><table><thead><tr><th>档</th><th>收入范围（元）</th><th>基数比例</th><th>定额减除</th><th>税率</th><th>速算扣除</th></tr></thead><tbody>{row.brackets.map(b => <tr key={b.seq}><td>{b.seq}</td><td>{yuan(b.incomeFloorCent)} 至 {b.incomeCeilingCent == null ? "无上限" : yuan(b.incomeCeilingCent)}</td><td>{b.incomeBaseBps / 100}%</td><td>{yuan(b.flatDeductionCent)}</td><td>{b.rateBps / 100}%</td><td>{yuan(b.quickDeductionCent)}</td></tr>)}</tbody></table>{evidenceLinks(row)}</details>{operations("version", row)}</td></tr>)}
      </tbody></table>
      <h3>工资及服务收入的专业税务认定</h3><p>完整工资必须选择税前核算原件，再登记有专业依据的本次个税，不能默认免税。旧服务收入认定保留原核算流程；不能用来替代全薪认定。税目及适用日期由专业结论确定，已定格工资的认定不可替换。</p>
      <label>员工账号<input aria-label="员工账号" disabled={busy} value={employee} onChange={e => { setEmployee(e.target.value); setAssessments([]); setCalculations([]); setPayrollTax({ calculationId: "", amount: "", reference: "" }); }} /></label>
      <label>工资月份<input aria-label="工资月份" type="month" disabled={busy} value={month} onChange={e => { setMonth(e.target.value); setAssessments([]); setCalculations([]); setPayrollTax({ calculationId: "", amount: "", reference: "" }); }} /></label>
      <button type="button" disabled={busy} onClick={() => void loadAssessments()}>查询认定历史</button>
      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={!employee.trim() || !month} onClick={() => { setFormError(""); setAssessmentOpen(true); }}>＋ 新建税务认定</PermissionButton>
      <table className="admin-table"><thead><tr><th>员工及月份</th><th>来源收入／税额（元）</th><th>税目及适用日期</th><th>状态</th><th>依据与操作</th></tr></thead><tbody>{assessments.map(row => <tr key={row.id}><td>{row.userId} · {row.month}</td><td>{yuan(row.incomeCent)} ／ {yuan(row.taxCent)}</td><td>{row.category} · {row.taxDate}</td><td>{statusLabels[row.status] ?? row.status}</td><td><details><summary>认定及原始收入</summary>{row.payrollCalculationId ? <p>完整工资核算原件：{row.payrollCalculationId}；专业核税计算说明：{row.professionalCalculationReference}</p> : <p>既有服务收入认定</p>}<p>身份依据：{row.identityBasis}</p><p>交易依据：{row.transactionBasis}</p><p>专业认定：{row.professionalBasis}</p><p>发放主体：{row.payerCompanyId}；税表：{row.taxVersionId}</p>{row.sourceLines.map(line => <p key={line.id}>来源 {line.sourceId}：{yuan(line.amountCent)} 元</p>)}{evidenceLinks(row)}</details>{operations("assessment", row)}</td></tr>)}</tbody></table>
    </AdminDataState>

    <GuidedFlowModal open={createOpen} title="新建完整税表版本" testId="payroll-tax-brackets-guided" submitTestId="payroll-tax-brackets-save" submitLabel="保存完整草案" submitting={busy} error={formError || undefined} onClose={() => setCreateOpen(false)} onSubmit={() => void createVersion()}
      steps={[{ title: "适用依据", content: <>{field("category", "税目编码")}{field("effectiveFrom", "生效日期", "date")}{field("applicabilityBasis", "适用身份及交易依据")}{field("calculationBasis", "计算及舍入专业依据")}{field("reason", "版本事由")}<p>当前公式：税额＝不低于0的［（收入×基数比例－定额减除）×税率－速算扣除］。</p><label>舍入口径<select aria-label="舍入口径" value={form.roundingRule} onChange={e => setForm(old => ({ ...old, roundingRule: e.target.value }))}><option value="">请选择已确认口径</option><option value="truncate_cent_each_step">按分逐步截断（须有专业依据）</option></select></label></> },
        { title: "完整税表", validate: versionError, content: <>{brackets.map((b, index) => <fieldset key={index}><legend>第 {index + 1} 档</legend>{([['floor', '收入下界（分）'], ['ceiling', '收入上界（分，末档留空）'], ['base', '基数万分比'], ['flat', '定额减除（分）'], ['rate', '税率万分比'], ['quick', '速算扣除（分）']] as const).map(([key, label]) => <label key={key}>{label}<input aria-label={`第${index + 1}档${label}`} type="number" value={b[key]} onChange={e => setBrackets(old => old.map((row, i) => i === index ? { ...row, [key]: e.target.value } : row))} /></label>)}{brackets.length > 1 ? <button type="button" onClick={() => setBrackets(old => old.filter((_, i) => i !== index))}>移除第{index + 1}档</button> : null}</fieldset>)}<button type="button" onClick={() => setBrackets(old => [...old, blankBracket()])}>增加档位</button></> }]}
      summary={[{ label: "税目", value: form.category }, { label: "生效日期", value: form.effectiveFrom }, { label: "档位", value: `${brackets.length} 档，从0分连续到无上限` }, { label: "后续", value: "保存原件后上传依据，再提交财税法及最高确认；保存不生效" }]} />
    <GuidedFlowModal open={assessmentOpen} title="新建税务认定" testId="payroll-tax-assessment-guided" submitTestId="payroll-tax-assessment-save" submitLabel="保存认定草案" submitting={busy} error={formError || undefined} onClose={() => setAssessmentOpen(false)} onSubmit={() => void createAssessment()}
      steps={[{ title: "专业认定", validate: assessmentError, content: <><label>认定收入来源<select aria-label="认定收入来源" value={payrollTax.calculationId} onChange={e => setPayrollTax({ calculationId: e.target.value, amount: "", reference: "" })}><option value="">既有服务收入台账</option>{calculations.map(row => <option key={row.id} value={row.id}>完整工资 {row.id} · 税前 {yuan(row.beforeTaxCent)} 元</option>)}</select></label>
          {payrollTax.calculationId ? <>{calculations.filter(row => row.id === payrollTax.calculationId).map(row => <StaffPayrollCalculationView key={row.id} row={row} />)}<label>专业确认个税（元）<input aria-label="专业确认个税（元）" value={payrollTax.amount} onChange={e => setPayrollTax(old => ({ ...old, amount: e.target.value }))} /></label><label>专业核税计算说明<input aria-label="专业核税计算说明" value={payrollTax.reference} onChange={e => setPayrollTax(old => ({ ...old, reference: e.target.value }))} /></label><p>含适用税期、累计基数、已缴税额、扣除项和本次税额的专业核算依据，提交前须上传真实原件。</p></> : null}
          {assessmentField("category", "认定税目编码")}{assessmentField("taxDate", "税务适用日期", "date")}{assessmentField("identityBasis", "人员身份依据")}{assessmentField("transactionBasis", "交易实质依据")}{assessmentField("professionalBasis", "专业财税结论及日期依据")}{assessmentField("reason", "认定事由")}</> }]}
      summary={[{ label: "员工及月份", value: `${employee} · ${month}` }, { label: "税目及日期", value: `${assessment.category} · ${assessment.taxDate}` }, { label: "收入", value: payrollTax.calculationId ? `锁定核算原件 ${payrollTax.calculationId}；本次专业确认个税 ${payrollTax.amount} 元` : "由真实服务收入台账计算并锁定，不能手填税后额" }]} />
    <GuidedFlowModal open={target !== null} title="提交税务审批" testId="payroll-tax-submit-guided" submitTestId="payroll-tax-submit" submitLabel="提交财税法及最高确认" submitting={busy} error={formError || undefined} onClose={() => setTarget(null)} onSubmit={() => void submit()}
      steps={[{ title: "凭证与真实部门审批线", validate: () => !files.length || Object.values(reviewers).some(value => !value.trim()) ? "请上传专业依据，并填写三条线最终审批人" : null, content: <><p>各线须已有真实部门起点，最终人须在相应上级链上。最高确认由现审批模型确定。</p><label>专业依据原件<input aria-label="专业依据原件" type="file" accept="image/*,video/*" multiple disabled={busy} onChange={e => void upload(e.target.files)} /></label><p>当前证据入口支持照片或视频，请上传专业依据的清晰影像。</p><p>已关联 {files.length} 份专业凭证</p>{files.map((id, i) => <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">查看凭证 {i + 1} </a>)}{(Object.entries(lineLabels) as [keyof typeof reviewers, string][]).map(([key, label]) => <label key={key}>{label}<input aria-label={label} value={reviewers[key]} placeholder="员工账号编号" onChange={e => setReviewers(old => ({ ...old, [key]: e.target.value }))} /></label>)}</> }]}
      summary={[{ label: "原件", value: target?.row.id ?? "" }, { label: "凭证", value: `${files.length} 份` }, { label: "生效条件", value: "财务、税务、法务全部通过后，由最高权限最后确认" }]} />
  </section>;
}
