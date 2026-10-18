// 文件版本：V0.01
import React from "react";
import { StaffPayrollCalculationView } from "./StaffPayrollCalculationView";
import { withBase } from "../../basePath";
import { cnDate } from "../components/RowDetailDrawer";
import { StaffPayrollScoreRulesForm } from "./StaffPayrollScoreRulesForm";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";
import { payrollKind, payrollStatus } from "./StaffPayrollDetail";
import { StaffPayrollSchemeForm } from "./StaffPayrollSchemeForm";
import { StaffPayrollCalendarForm, payrollDayKinds } from "./StaffPayrollCalendarForm";
import { StaffPayrollPeriodForm } from "./StaffPayrollPeriodForm";
import type { PayrollCalendar, PayrollCalculation, PayrollTaxAssessment, PayrollPeriodFacts, PayrollProposal, PayrollScheme, PayrollScoreRuleVersion, PayrollScoreAdjustment, PayrollStatementDetail } from "./staffPayrollTypes";

const ROOT = "/api/v1/staff/payroll";
type Resource = "schemes" | "calendars" | "period-facts" | "performance-rules";
const evidenceTypes: Record<Resource, string> = { schemes: "staff_payroll_scheme", calendars: "staff_payroll_calendar", "period-facts": "staff_payroll_period", "performance-rules": "staff_performance_rule" };

export function StaffPayrollBasisPanel({ api, effectivePermissions, setActionResult, setGlobalError, user, month, onSettled }: AdminPageProps & { month: string; onSettled: (detail: PayrollStatementDetail) => void }) {
  const [employee, setEmployee] = React.useState("");
  const [project, setProject] = React.useState("");
  const [ruleOwner, setRuleOwner] = React.useState("");
  const [ruleMerchant, setRuleMerchant] = React.useState("");
  const [scoreRuleVersions, setScoreRuleVersions] = React.useState<PayrollScoreRuleVersion[]>([]);
  const [schemes, setSchemes] = React.useState<PayrollScheme[]>([]);
  const [calendars, setCalendars] = React.useState<PayrollCalendar[]>([]);
  const [facts, setFacts] = React.useState<PayrollPeriodFacts[]>([]);
  const [scores, setScores] = React.useState<PayrollScoreAdjustment[]>([]);
  const [open, setOpen] = React.useState<Resource | null>(null);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const [target, setTarget] = React.useState<{ type: Resource; row: PayrollProposal } | null>(null);
  const [files, setFiles] = React.useState<string[]>([]);
  const [reviewers, setReviewers] = React.useState({ finance_review: "" });
  const [settle, setSettle] = React.useState<{ factsId: string; statementKind: string; originEventId: string; calculationId: string; taxAssessmentId: string } | null>(null);
  const [calculations, setCalculations] = React.useState<PayrollCalculation[]>([]);
  const [taxAssessments, setTaxAssessments] = React.useState<PayrollTaxAssessment[]>([]);
  const [scoreForm, setScoreForm] = React.useState({ ruleCode: "", reason: "" });
  const [appeal, setAppeal] = React.useState<{ id: string; reason: string } | null>(null);
  const requestKey = React.useRef("");
  const loadGeneration = React.useRef(0);
  React.useEffect(() => { loadGeneration.current++; setFacts([]); setScores([]); setSettle(null); }, [month]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true); setError(""); setGlobalError(null);
    try { await action(); return true; }
    catch (e) { const message = e instanceof Error ? e.message : "薪资依据操作失败"; setError(message); setGlobalError(message); return false; }
    finally { setBusy(false); }
  }
  async function load() {
    await run(async () => {
      if (!employee.trim() || !project.trim()) throw new Error("请填写员工账号及所属项目编号");
      const generation = ++loadGeneration.current;
      const [nextSchemes, nextCalendars, nextFacts, nextScores] = await Promise.all([
        api.get<PayrollScheme[]>(`${ROOT}/schemes?user_id=${encodeURIComponent(employee.trim())}`),
        api.get<PayrollCalendar[]>(`${ROOT}/calendars?project_id=${encodeURIComponent(project.trim())}`),
        api.get<PayrollPeriodFacts[]>(`${ROOT}/period-facts?user_id=${encodeURIComponent(employee.trim())}&month=${month}`),
        api.get<PayrollScoreAdjustment[]>(`${ROOT}/performance-adjustments?user_id=${encodeURIComponent(employee.trim())}&month=${month}`),
      ]);
      if (generation === loadGeneration.current) { setSchemes(nextSchemes); setCalendars(nextCalendars); setFacts(nextFacts); setScores(nextScores); }
    });
  }
  async function prepareSubmit(type: Resource, row: PayrollProposal) {
    requestKey.current = crypto.randomUUID(); setTarget({ type, row }); setFiles(row.evidenceAttachmentIds); setError("");
    await run(async () => {
      const timeline = await api.get<{ itemType: string; referenceId: string; result: string }[]>(`/api/v1/evidence/timelines/${evidenceTypes[type]}/${encodeURIComponent(row.id)}`);
      setFiles([...new Set([...row.evidenceAttachmentIds, ...timeline.filter(item => item.itemType === "attachment" && item.result === "uploaded").map(item => item.referenceId)])]);
    });
  }
  async function upload(uploaded: FileList | null) {
    if (!target || !uploaded?.length) return;
    await run(async () => {
      if (!api.postForm) throw new Error("当前客户端不支持凭证上传");
      for (const file of Array.from(uploaded)) {
        const data = new FormData(); data.append("file", file); data.append("business_type", evidenceTypes[target.type]); data.append("business_id", target.row.id); data.append("attachment_role", "professional_basis");
        const row = await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", data);
        setFiles(old => [...new Set([...old, row.id])]);
      }
    });
  }
  async function submit() {
    if (!target) return;
    if (await run(async () => {
      if (!files.length || (target.type !== "performance-rules" && !reviewers.finance_review.trim())) throw new Error("请上传依据，并填写各审批线最终审批人");
      const row = await api.post<PayrollProposal>(`${ROOT}/${target.type}/${encodeURIComponent(target.row.id)}/submit`, { idempotencyKey: requestKey.current, evidenceAttachmentIds: files, parallelLines: target.type === "performance-rules" ? [] : Object.entries(reviewers).map(([lineCode, finalApproverId]) => ({ lineCode, finalApproverId: finalApproverId.trim() })) });
      if (target.type === "performance-rules") setScoreRuleVersions(old => old.map(item => item.id === row.id ? { ...item, ...row } : item));
      if (target.type === "schemes") setSchemes(old => old.map(item => item.id === row.id ? { ...item, ...row } : item));
      if (target.type === "calendars") setCalendars(old => old.map(item => item.id === row.id ? { ...item, ...row } : item));
      if (target.type === "period-facts") setFacts(old => old.map(item => item.id === row.id ? { ...item, ...row } : item));
      setActionResult({ action: "payroll_basis_submitted", id: row.id, reviewId: row.reviewId });
    })) setTarget(null);
  }
  async function prepareCalculation() {
    if (!settle) return;
    await run(async () => {
      if (settle.statementKind !== "monthly" && !settle.originEventId.trim()) throw new Error("请关联真实来源事件后生成核算原件");
      const row = await api.post<PayrollCalculation>(`${ROOT}/calculations`, { idempotencyKey: crypto.randomUUID(), userId: employee.trim(), month, factsId: settle.factsId, statementKind: settle.statementKind, originEventId: settle.originEventId.trim() || null });
      setCalculations(old => [row, ...old.filter(item => item.id !== row.id)]);
      setSettle({ ...settle, calculationId: row.id, taxAssessmentId: "" });
    });
  }
  async function loadCalculations() {
    await run(async () => {
      const query = `?user_id=${encodeURIComponent(employee.trim())}&month=${encodeURIComponent(month)}`;
      const [rows, assessments] = await Promise.all([api.get<PayrollCalculation[]>(`${ROOT}/calculations${query}`), api.get<PayrollTaxAssessment[]>(`${ROOT}/tax-assessments${query}`)]);
      setCalculations(rows); setTaxAssessments(assessments);
    });
  }
  async function settleStatement() {
    if (!settle) return;
    if (await run(async () => {
      if (settle.statementKind !== "monthly" && !settle.originEventId.trim()) throw new Error("离职结清或补发须关联真实来源事件编号");
      if (!settle.calculationId || !settle.taxAssessmentId) throw new Error("须选择本次税前核算原件及匹配的已批准税务认定");
      const result = await api.post<PayrollStatementDetail>(`${ROOT}/settlements`, { ...settle, idempotencyKey: requestKey.current, userId: employee.trim(), month, originEventId: settle.originEventId.trim() || null });
      onSettled(result); setActionResult({ action: "payroll_statement_settled", id: result.statement.id, note: "计薪定格不代表工资已支付" });
    })) setSettle(null);
  }
  const approvedSchemeIds = new Set(facts.filter(row => row.status === "approved").map(row => row.schemeVersionId));
  const scoreRules = [...new Set(schemes.filter(row => approvedSchemeIds.has(row.id)).flatMap(row => Object.keys(row.performanceRules?.deductionRules ?? {})))];
  async function requestScore() {
    await run(async () => {
      if (!employee.trim() || !scoreForm.ruleCode || !scoreForm.reason.trim()) throw new Error("请先选择已批准扣分规则并填写事由");
      const row = await api.post<PayrollScoreAdjustment>(`${ROOT}/performance-adjustments`, { idempotencyKey: crypto.randomUUID(), userId: employee.trim(), month, ...scoreForm });
      setScores(old => [row, ...old]); setScoreForm({ ruleCode: "", reason: "" }); setActionResult({ action: "payroll_score_requested", id: row.id, reviewId: row.reviewId });
    });
  }
  const action = (type: Resource, row: PayrollProposal) => <>{row.status === "draft" ? <button type="button" disabled={busy} onClick={() => void prepareSubmit(type, row)}>上传依据并提交审批</button> : null}{row.reviewId ? <a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>查看审批链</a> : null}{row.evidenceAttachmentIds?.map((id, i) => <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">依据 {i + 1} </a>)}</>;
  return <section data-testid="payroll-basis-panel">
    <h3>薪资方案与计薪依据</h3><p>基础工资、各项薪资和工时制度统一使用已批准方案；旧定岗备注不作为计薪公式。工资定格与实际支付分别记录。</p>
    <label>查阅员工账号<input aria-label="查阅员工账号" disabled={busy} value={employee} onChange={e => { loadGeneration.current++; setEmployee(e.target.value); setSchemes([]); setFacts([]); setScores([]); setSettle(null); }} /></label>
    <label>计薪项目编号<input aria-label="计薪项目编号" disabled={busy} value={project} onChange={e => { loadGeneration.current++; setProject(e.target.value); setCalendars([]); setFacts([]); setScoreRuleVersions([]); setSettle(null); }} /></label>
    <button type="button" disabled={busy} onClick={() => void load()}>读取员工计薪依据</button>
    <PermissionButton permissionsState={effectivePermissions} permissions={["staff.payroll.manage"]} data-testid="payroll-base-create-open" onClick={() => setOpen("schemes")}>＋ 新建薪资方案</PermissionButton>
    <PermissionButton permissionsState={effectivePermissions} permissions={["staff.payroll.manage"]} disabled={!project.trim()} onClick={() => setOpen("calendars")}>＋ 新建计薪日历</PermissionButton>
    <PermissionButton permissionsState={effectivePermissions} permissions={["staff.payroll.manage"]} disabled={!employee.trim() || !schemes.some(row => row.status === "approved") || !calendars.some(row => row.status === "approved")} onClick={() => setOpen("period-facts")}>＋ 登记期间计薪事实</PermissionButton>
    {error && !target && !settle ? <p role="alert">{error}</p> : null}
    <table className="admin-table"><caption>薪资方案版本</caption><thead><tr><th>版本及适用时间</th><th>状态</th><th>项目及依据</th><th>操作</th></tr></thead><tbody>{schemes.map(row => <tr key={row.id}><td>{row.version} · {row.id}<br />{cnDate(row.effectiveFrom)} 至 {row.effectiveTo ? cnDate(row.effectiveTo) : "未指定终止日"}</td><td>{payrollStatus(row.status)}</td><td><details><summary>查看薪资项目和约定</summary>{row.components.map(item => <p key={item.code}>{item.label}：{(item.amountCent / 100).toFixed(2)} 元；{item.calculationBasis}</p>)}<p>支付约定：{row.paymentBasis}</p><p>工时批准依据：{row.worktimeTerms.approvalBasis}</p><p>工时计薪依据：{row.worktimeTerms.calculationBasis}</p></details></td><td>{action("schemes", row)}</td></tr>)}</tbody></table>
    <table className="admin-table"><caption>计薪日历版本</caption><thead><tr><th>版本及范围</th><th>状态</th><th>逐日性质</th><th>操作</th></tr></thead><tbody>{calendars.map(row => <tr key={row.id}><td>{row.version} · {row.id}<br />{cnDate(row.startDate)} 至 {cnDate(row.endDate)}</td><td>{payrollStatus(row.status)}</td><td><details><summary>查看逐日日历</summary><p>{row.sourceBasis}</p>{row.days.map(day => <p key={day.date}>{cnDate(day.date)}：{payrollDayKinds[day.kind] ?? "待核实"}</p>)}</details></td><td>{action("calendars", row)}</td></tr>)}</tbody></table>
    <table className="admin-table"><caption>本期计薪事实</caption><thead><tr><th>事实单及方案</th><th>状态</th><th>依据</th><th>操作</th></tr></thead><tbody>{facts.map(row => <tr key={row.id}><td>{row.id}<br />{row.schemeVersionId}</td><td>{payrollStatus(row.status)}</td><td><details><summary>查看工时周期和项目事实</summary><p>周期：{cnDate(row.cycleStart)} 至 {cnDate(row.cycleEnd)}；标准 {row.cycleStandardMinutes} 分钟；约定工资日 {cnDate(row.agreedDueDate)}</p>{row.componentFacts.map(item => <p key={item.componentCode}>{item.componentCode}：数量 {item.quantity}，分母 {item.denominator}；公式内已扣事假 {(item.embeddedLeaveCent / 100).toFixed(2)} 元；{item.evidenceBasis}</p>)}<p>{row.reason}</p></details></td><td>{action("period-facts", row)}{row.status === "approved" ? <button type="button" disabled={busy} onClick={() => { requestKey.current = crypto.randomUUID(); setCalculations([]); setTaxAssessments([]); setSettle({ factsId: row.id, statementKind: "monthly", originEventId: "", calculationId: "", taxAssessmentId: "" }); }}>按本单核算工资</button> : null}</td></tr>)}</tbody></table>
    <h4>经营主体扣分分值表</h4>
    <label>分值表经营主体<input aria-label="分值表经营主体" disabled={busy} value={ruleOwner} onChange={e => { setRuleOwner(e.target.value); setScoreRuleVersions([]); }} /></label>
    <label>分值表商户（项目自有人员留空）<input aria-label="分值表商户" disabled={busy} value={ruleMerchant} onChange={e => { setRuleMerchant(e.target.value); setScoreRuleVersions([]); }} /></label>
    <button type="button" disabled={busy || !project.trim() || !ruleOwner.trim()} onClick={() => void run(async () => setScoreRuleVersions(await api.get<PayrollScoreRuleVersion[]>(`${ROOT}/performance-rules?project_id=${encodeURIComponent(project.trim())}&owner_company_id=${encodeURIComponent(ruleOwner.trim())}${ruleMerchant.trim() ? `&merchant_id=${encodeURIComponent(ruleMerchant.trim())}` : ""}`)))}>读取经营主体分值表</button>
    <PermissionButton permissionsState={effectivePermissions} permissions={["staff.payroll.manage"]} disabled={busy || !project.trim() || !ruleOwner.trim()} onClick={() => setOpen("performance-rules")}>＋ 新建扣分分值表</PermissionButton>
    <table className="admin-table"><thead><tr><th>分值表及经营主体</th><th>状态</th><th>规则分值</th><th>原件及审批</th></tr></thead><tbody>{scoreRuleVersions.map(row => <tr key={row.id}><td>{row.id} · {row.ownerCompanyId}<br />{cnDate(row.effectiveFrom)} 起适用</td><td>{payrollStatus(row.status)}</td><td>{Object.entries(row.deductions).map(([code, points]) => <p key={code}>{code}：{points} 分</p>)}</td><td>{action("performance-rules", row)}</td></tr>)}</tbody></table>
    <h4>绩效扣分与申诉</h4><p>只选择已批准的分值表规则，分值由服务端确定；不把绩效扣分记成现金处罚。</p>
    <label>已批准扣分规则<select aria-label="已批准扣分规则" value={scoreForm.ruleCode} onChange={e => setScoreForm(old => ({ ...old, ruleCode: e.target.value }))}><option value="">请选择</option>{scoreRules.map(code => <option key={code}>{code}</option>)}</select></label>
    <label>扣分事由<input aria-label="扣分事由" value={scoreForm.reason} onChange={e => setScoreForm(old => ({ ...old, reason: e.target.value }))} /></label><button type="button" disabled={busy || !scoreRules.length} onClick={() => void requestScore()}>发起绩效扣分复核</button>
    <table className="admin-table"><thead><tr><th>记录</th><th>规则</th><th>分值</th><th>事由</th><th>状态</th><th>审批及申诉</th></tr></thead><tbody>{scores.map(row => <tr key={row.id}><td>{row.id}</td><td>{row.ruleCode}</td><td>{row.points}</td><td>{row.reason}</td><td>{payrollStatus(row.status)}</td><td><a href={withBase(`/admin/governance/approval-chain?review_id=${encodeURIComponent(row.reviewId)}`)}>查看审批链</a>{row.userId === user.id && row.status === "approved" ? <button type="button" onClick={() => setAppeal({ id: row.id, reason: "" })}>本人申诉</button> : null}</td></tr>)}</tbody></table>
    {appeal ? <fieldset><legend>绩效扣分申诉</legend><label>申诉事由<input aria-label="绩效申诉事由" value={appeal.reason} onChange={e => setAppeal({ ...appeal, reason: e.target.value })} /></label><button type="button" disabled={busy || !appeal.reason.trim()} onClick={() => void run(async () => { await api.post(`${ROOT}/performance-adjustments/${encodeURIComponent(appeal.id)}/appeals`, { reason: appeal.reason.trim() }); setActionResult({ action: "payroll_score_appeal_submitted", id: appeal.id }); setAppeal(null); })}>提交本人申诉</button><button type="button" onClick={() => setAppeal(null)}>取消</button></fieldset> : null}
    <StaffPayrollSchemeForm api={api} scoreRuleVersions={scoreRuleVersions} open={open === "schemes"} onClose={() => setOpen(null)} onSaved={row => { if (row.userId !== employee) { setFacts([]); setScores([]); setSettle(null); } if (row.projectId !== project) { setCalendars([]); setScoreRuleVersions([]); } setEmployee(row.userId); setProject(row.projectId); setSchemes(old => [row, ...old.filter(item => item.userId === row.userId)]); }} />
    <StaffPayrollScoreRulesForm api={api} open={open === "performance-rules"} projectId={project} ownerCompanyId={ruleOwner} merchantId={ruleMerchant} onClose={() => setOpen(null)} onSaved={row => setScoreRuleVersions(old => [row, ...old])} />
    <StaffPayrollCalendarForm api={api} open={open === "calendars"} projectId={project} onClose={() => setOpen(null)} onSaved={row => setCalendars(old => [row, ...old])} />
    <StaffPayrollPeriodForm api={api} open={open === "period-facts"} userId={employee} month={month} schemes={schemes} calendars={calendars} onClose={() => setOpen(null)} onSaved={row => setFacts(old => [row, ...old])} />
    <GuidedFlowModal open={target !== null} title="提交计薪依据审批" testId="payroll-basis-submit-guided" submitTestId="payroll-basis-submit" submitLabel="提交审批" submitting={busy} error={error || undefined} onClose={() => setTarget(null)} onSubmit={() => void submit()}
      steps={[{ title: "原件和审批线", validate: () => !files.length || (target?.type !== "performance-rules" && !reviewers.finance_review.trim()) ? "请上传依据并填写各线最终审批人" : null, content: <><label>计薪依据原件<input aria-label="计薪依据原件" type="file" accept="image/*,video/*" multiple disabled={busy} onChange={e => void upload(e.target.files)} /></label><p>已关联 {files.length} 份原件。上传草案对应的真实依据影像。{target?.type === "performance-rules" ? "分值表由实际经营主体总经理或合法授权人审批。" : target?.type === "calendars" ? "日历由经办主管上一级与财务审核。" : "薪资方案及期间事实沿直属共三级与人力审核，另经财务核实；直属及人力由系统确定。"}</p>{files.map((id, i) => <a key={id} href={withBase(`/api/v1/evidence/attachments/${encodeURIComponent(id)}/file`)} target="_blank" rel="noreferrer">查看凭证 {i + 1} </a>)}{target?.type !== "performance-rules" && ([["finance_review", "财务最终审批人"]] as const).map(([key, label]) => <label key={key}>{label}<input aria-label={label} value={reviewers[key]} onChange={e => setReviewers(old => ({ ...old, [key]: e.target.value }))} /></label>)}</> }]}
      summary={[{ label: "草案", value: target?.row.id ?? "" }, { label: "依据原件", value: `${files.length} 份` }, { label: "生效条件", value: "完成对应审批链后生效" }]} />
    <GuidedFlowModal open={settle !== null} title="核算工资" testId="payroll-settle-guided" submitTestId="payroll-settle-submit" submitLabel="按已批准依据定格" submitting={busy} error={error || undefined} onClose={() => setSettle(null)} onSubmit={() => void settleStatement()}
      steps={[{ title: "工资类型及来源", content: settle ? <><label>工资单类型<select aria-label="工资单类型" value={settle.statementKind} onChange={e => setSettle({ ...settle, statementKind: e.target.value, calculationId: "", taxAssessmentId: "" })}>{["monthly", "termination", "supplemental"].map(kind => <option key={kind} value={kind}>{payrollKind(kind)}</option>)}</select></label>{settle.statementKind !== "monthly" ? <label>真实来源事件编号<input aria-label="真实来源事件编号" value={settle.originEventId} onChange={e => setSettle({ ...settle, originEventId: e.target.value, calculationId: "", taxAssessmentId: "" })} /></label> : null}<p>先生成税前核算原件，再到<a href={withBase("/admin/staff/tax-brackets")}>税表与专业认定</a>办理本快照的税务审批；批准后读取认定并定格。原已发单据不覆盖；定格不代表银行到账。</p>
          <button type="button" disabled={busy} onClick={() => void prepareCalculation()}>生成本次税前核算原件</button>
          <button type="button" disabled={busy} onClick={() => void loadCalculations()}>读取核算原件及税务认定</button>
          <label>税前核算原件<select aria-label="税前核算原件" value={settle.calculationId} onChange={e => setSettle({ ...settle, calculationId: e.target.value, taxAssessmentId: "" })}><option value="">请选择本次原件</option>{calculations.filter(row => row.factsId === settle.factsId && row.statementKind === settle.statementKind && (row.originEventId ?? "") === settle.originEventId.trim()).map(row => <option key={row.id} value={row.id}>{row.id} · 税前 {(row.beforeTaxCent / 100).toFixed(2)} 元</option>)}</select></label>
          {calculations.filter(row => row.id === settle.calculationId).map(row => <StaffPayrollCalculationView key={row.id} row={row} />)}
          <label>已批准匹配税务认定<select aria-label="已批准匹配税务认定" value={settle.taxAssessmentId} onChange={e => setSettle({ ...settle, taxAssessmentId: e.target.value })}><option value="">请选择</option>{taxAssessments.filter(row => row.status === "approved" && row.payrollCalculationId === settle.calculationId).map(row => <option key={row.id} value={row.id}>{row.id} · 个税 {(row.taxCent / 100).toFixed(2)} 元</option>)}</select></label></> : null }]}
      summary={[{ label: "员工及月份", value: `${employee} · ${month}` }, { label: "期间事实", value: settle?.factsId ?? "" }, { label: "工资类型", value: payrollKind(settle?.statementKind ?? "") }]} />
  </section>;
}
