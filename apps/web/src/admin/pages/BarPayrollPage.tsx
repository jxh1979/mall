// 文件版本：V0.01
import React from "react";
import { PermissionButton } from "../security/Can";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { withBase } from "../../basePath";
import type { AdminPageProps } from "../types";

type Participant = { userId: string; shareUnits: number; partialCoverMethod: string; payerCompanyId?: string };
type RuleValues = { rateBps?: number; minimumRateBps?: number; maximumRateBps?: number; withheldBps?: number; minimumWithheldBps?: number; maximumWithheldBps?: number; participants?: Participant[] };
type Rule = { id: string; ruleKind: string; projectId: string; merchantId?: string; effectiveFrom: string; version: string; values: RuleValues; platformCompanyId: string; merchantConfirmedBy?: string; platformConfirmedBy?: string; reviewId?: string; status: string; reason: string; evidenceAttachmentIds: string[] };
type Allocation = { id: string; userId: string; componentType: string; amountCent: number; payerCompanyId: string; sourceBillIds: string[]; ruleVersionId: string };
type Run = { id?: string; month: string; collectedTipCent: number; collectedServiceFeeCent: number; payableTipCent: number; payableServiceFeeCent: number; withheldServiceFeeCent: number; roundingResidualCent: number; settlementEntityId?: string; status: string; allocations: Allocation[]; unresolvedSources: string[]; createdAt?: string };
const labels: Record<string, string> = { draft: "草案", pending_review: "待审批", approved: "已批准", rejected: "已驳回", confirmed: "已确认原件", pending: "未结事项" };
const methods: Record<string, string> = { full_shift_share: "按参与本班的约定份额", actual_shift_fraction: "按实际顶班时长占原完整班次折算" };
const yuan = (value: number) => (value / 100).toFixed(2);
const ROOT = "/api/v1/bar";

export function BarPayrollPage({ api, user, effectivePermissions, setActionResult, setGlobalError }: AdminPageProps) {
  const [scope, setScope] = React.useState({ projectId: "", merchantId: "", month: "" });
  const [rules, setRules] = React.useState<Rule[]>([]);
  const [runs, setRuns] = React.useState<Run[]>([]);
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");
  const [form, setForm] = React.useState({ kind: "charge", effectiveFrom: "", company: "", rate: "10", minimum: "", maximum: "", withheld: "", withheldMin: "", withheldMax: "", reason: "" });
  const [participants, setParticipants] = React.useState([{ userId: "", shares: "", method: "" }]);
  const [target, setTarget] = React.useState<Rule | null>(null);
  const [files, setFiles] = React.useState<string[]>([]);
  const [approver, setApprover] = React.useState("");
  const keys = React.useRef(new Map<string, { body: string; key: string }>());
  const allowed = (permission: string) => effectivePermissions?.status === "ready" && effectivePermissions.value.grants.includes(permission) && !effectivePermissions.value.denies.includes(permission);

  async function action(work: () => Promise<unknown>) {
    setBusy(true); setError(""); setGlobalError(null);
    try { const result = await work(); setActionResult(result); return true; }
    catch (e) { const message = e instanceof Error ? e.message : "酒吧服务收入操作失败"; setError(message); setGlobalError(message); return false; }
    finally { setBusy(false); }
  }
  async function post<T>(path: string, body: Record<string, unknown>): Promise<T> {
    const serialized = JSON.stringify(body); const previous = keys.current.get(path);
    const key = previous?.body === serialized ? previous.key : crypto.randomUUID();
    keys.current.set(path, { body: serialized, key });
    const result = await api.post<T>(path, { ...body, idempotencyKey: key });
    keys.current.delete(path); return result;
  }
  const query = () => "?project_id=" + encodeURIComponent(scope.projectId.trim()) + "&merchant_id=" + encodeURIComponent(scope.merchantId.trim());
  async function load() {
    await action(async () => {
      if (!scope.projectId.trim()) throw new Error("请填写综合体项目编号");
      const rows = await api.get<Rule[]>(ROOT + "/payroll-rules" + query());
      setRules(rows);
      if (scope.merchantId.trim() && scope.month) setRuns(await api.get<Run[]>(ROOT + "/payroll-runs" + query() + "&month=" + scope.month));
      else setRuns([]);
      return { rules: rows.length };
    });
  }
  function bps(value: string, label: string) {
    if (!/^\d+(\.\d{1,2})?$/.test(value.trim())) throw new Error(label + "须明确填写，最多两位小数");
    const amount = Math.round(Number(value) * 100);
    if (amount < 0 || amount > 10000) throw new Error(label + "须在0%至100%之间");
    return amount;
  }
  async function draft() {
    await action(async () => {
      if (!scope.projectId.trim() || !form.effectiveFrom || !form.company.trim() || !form.reason.trim()) throw new Error("请填写综合体、生效日期、经营公司和申请事由");
      let values: RuleValues;
      if (form.kind === "charge") {
        values = { rateBps: bps(form.rate, "收费比例"), minimumRateBps: bps(form.minimum, "收费下限"), maximumRateBps: bps(form.maximum, "收费上限") };
      } else {
        if (!scope.merchantId.trim()) throw new Error("分配规则须指定商户");
        const people = participants.map(person => {
          if (!person.userId.trim() || !Number.isSafeInteger(Number(person.shares)) || Number(person.shares) <= 0 || !person.method) throw new Error("每名参与人须填写账号、正整数份数和部分顶班折算方式");
          return { userId: person.userId.trim(), shareUnits: Number(person.shares), partialCoverMethod: person.method };
        });
        values = { withheldBps: bps(form.withheld, "扣留比例"), minimumWithheldBps: bps(form.withheldMin, "扣留下限"), maximumWithheldBps: bps(form.withheldMax, "扣留上限"), participants: people };
      }
      const row = await post<Rule>(ROOT + "/payroll-rules", { ruleKind: form.kind, projectId: scope.projectId.trim(), merchantId: form.kind === "allocation" ? scope.merchantId.trim() : null, effectiveFrom: form.effectiveFrom, values, platformCompanyId: form.company.trim(), reason: form.reason.trim() });
      setRules(old => [row, ...old]); return row;
    });
  }
  async function confirm(row: Rule, party: string) {
    await action(async () => {
      const updated = await post<Rule>(ROOT + "/payroll-rules/" + encodeURIComponent(row.id) + "/confirm", { party });
      setRules(old => old.map(item => item.id === row.id ? updated : item)); return updated;
    });
  }
  async function upload(input: FileList | null) {
    if (!target || !input?.length) return;
    await action(async () => {
      if (!api.postForm) throw new Error("当前入口不能上传原件");
      const ids: string[] = [];
      for (const file of Array.from(input)) {
        const data = new FormData(); data.append("file", file); data.append("business_type", "bar_payroll_rule"); data.append("business_id", target.id); data.append("attachment_role", "rule_agreement");
        ids.push((await api.postForm<{ id: string }>("/api/v1/evidence/attachments/upload", data)).id);
      }
      setFiles(old => [...old, ...ids]); return { uploaded: ids.length };
    });
  }
  async function submit() {
    if (!target) return;
    if (await action(async () => {
      if (!files.length) throw new Error("请先上传双方约定或项目收费规则原件");
      const row = await post<Rule>(ROOT + "/payroll-rules/" + encodeURIComponent(target.id) + "/submit", { evidenceAttachmentIds: files, finalApproverId: approver.trim() || null, parallelLines: [] });
      setRules(old => old.map(item => item.id === row.id ? row : item)); return row;
    })) setTarget(null);
  }
  async function closeMonth() {
    await action(async () => {
      if (!scope.projectId.trim() || !scope.merchantId.trim() || !scope.month) throw new Error("请填写完整月结范围");
      const rows = await post<Run[]>(ROOT + "/payroll-runs/close", { projectId: scope.projectId.trim(), merchantId: scope.merchantId.trim(), month: scope.month });
      setRuns(rows); return rows;
    });
  }
  const field = (key: keyof typeof form, label: string, type = "text") => <label>{label}<input aria-label={label} type={type} value={form[key]} onChange={e => setForm(old => ({ ...old, [key]: e.target.value }))} /></label>;
  if (!allowed("bar.payroll.view")) return <p>服务收入分配涉及员工金额，请先取得对应的单独授权。</p>;
  return <section data-testid="bar-payroll-page">
    <AdminPageHeader title="酒吧小费与服务费" description="收费规则、双方分配约定和月结原件分别留存。现金小费直接交给服务人员，不进入公司代收工资。" />
    {error ? <p role="alert">{error}</p> : null}
    <label>综合体项目编号<input aria-label="综合体项目编号" value={scope.projectId} onChange={e => { setScope(old => ({ ...old, projectId: e.target.value })); setRules([]); setRuns([]); }} /></label>
    <label>商户编号<input aria-label="商户编号" value={scope.merchantId} onChange={e => { setScope(old => ({ ...old, merchantId: e.target.value })); setRules([]); setRuns([]); }} /></label>
    <label>营业月份<input aria-label="营业月份" type="month" value={scope.month} onChange={e => { setScope(old => ({ ...old, month: e.target.value })); setRuns([]); }} /></label>
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.payroll.view"]} disabled={busy} onClick={() => void load()}>查询规则与月结</PermissionButton>
    {allowed("bar.payroll.manage") ? <details><summary>新建规则草案</summary>
      <label>规则类型<select aria-label="规则类型" value={form.kind} onChange={e => setForm(old => ({ ...old, kind: e.target.value }))}><option value="charge">综合体卡座收费比例</option><option value="allocation">商户服务费分配</option></select></label>
      {field("effectiveFrom", "规则生效日期", "date")}{field("company", "所属经营公司编号")}{field("reason", "规则申请事由")}
      {form.kind === "charge" ? <>{field("rate", "服务费比例（%）")}{field("minimum", "收费比例下限（%）")}{field("maximum", "收费比例上限（%）")}<p>按折前及扣券前消费计算，仅适用于卡座；上下限也经审批后生效。</p></> : <>
        {field("withheld", "商户扣留比例（%）")}{field("withheldMin", "扣留比例下限（%）")}{field("withheldMax", "扣留比例上限（%）")}
        {participants.map((person, i) => <fieldset key={i}><legend>参与人 {i + 1}</legend>
          <label>员工账号<input aria-label={`参与人${i + 1}账号`} value={person.userId} onChange={e => setParticipants(old => old.map((v, n) => n === i ? { ...v, userId: e.target.value } : v))} /></label>
          <label>约定份数<input aria-label={`参与人${i + 1}份数`} value={person.shares} onChange={e => setParticipants(old => old.map((v, n) => n === i ? { ...v, shares: e.target.value } : v))} /></label>
          <label>部分顶班折算<select aria-label={`参与人${i + 1}顶班折算`} value={person.method} onChange={e => setParticipants(old => old.map((v, n) => n === i ? { ...v, method: e.target.value } : v))}><option value="">请选择双方明确的方式</option>{Object.entries(methods).map(([v, label]) => <option key={v} value={v}>{label}</option>)}</select></label>
          {participants.length > 1 ? <button type="button" onClick={() => setParticipants(old => old.filter((_, n) => n !== i))}>移除此参与人</button> : null}
        </fieldset>)}
        <button type="button" onClick={() => setParticipants(old => [...old, { userId: "", shares: "", method: "" }])}>增加参与人</button><p>人员和计份、顶班折算须由商户与所属娱乐平台分别确认；不自动套用小费的实际服务时长比例。</p>
      </>}
      <PermissionButton permissionsState={effectivePermissions} permissions={["bar.payroll.manage"]} disabled={busy} onClick={() => void draft()}>保存完整草案</PermissionButton>
    </details> : null}
    <table className="admin-table"><thead><tr><th>规则及版本</th><th>生效日</th><th>双方确认</th><th>内容与审批</th></tr></thead><tbody>{rules.map(row => <tr key={row.id}>
      <td>{row.ruleKind === "charge" ? "卡座收费" : "服务费分配"} · {row.version}<p>{labels[row.status] ?? row.status}</p></td><td>{row.effectiveFrom}</td>
      <td>商户：{row.merchantConfirmedBy ?? (row.ruleKind === "charge" ? "项目收费规则" : "待本人确认")}<br />平台主管：{row.platformConfirmedBy ?? "待本人确认"}</td>
      <td><details><summary>查看规则原件</summary><p>{row.reason}</p><p>经营公司：{row.platformCompanyId}</p>
        {row.ruleKind === "charge" ? <p>收费 {(row.values.rateBps ?? 0) / 100}%；允许 {Number(row.values.minimumRateBps) / 100}% 至 {Number(row.values.maximumRateBps) / 100}%</p> : <><p>扣留 {Number(row.values.withheldBps) / 100}%</p>{row.values.participants?.map(person => <p key={person.userId}>{person.userId}：{person.shareUnits} 份；{methods[person.partialCoverMethod]}；发薪主体 {person.payerCompanyId}</p>)}</>}
      </details>
      {row.status === "draft" ? <><PermissionButton permissionsState={effectivePermissions} permissions={["bar.payroll.manage"]} disabled={busy} onClick={() => void confirm(row, user.merchantId ? "merchant" : "platform")}>以本人所属方确认</PermissionButton>
        <PermissionButton permissionsState={effectivePermissions} permissions={["bar.payroll.manage"]} disabled={busy} onClick={() => { setTarget(row); setFiles(row.evidenceAttachmentIds ?? []); setApprover(""); }}>上传依据并提交</PermissionButton></> : null}
      {row.reviewId ? <a href={withBase("/admin/governance/approval-chain?review_id=" + encodeURIComponent(row.reviewId))}>查看真实审批链</a> : null}</td>
    </tr>)}</tbody></table>
    {target ? <fieldset><legend>提交规则 {target.version}</legend><label>约定原件<input type="file" aria-label="规则约定原件" accept="image/*,video/*" multiple disabled={busy} onChange={e => void upload(e.target.files)} /></label><p>已上传 {files.length} 份原件。保存草案或双方确认均不等于审批通过。</p>
      <label>最终审批人（按已配置流程）<input aria-label="服务费最终审批人" value={approver} onChange={e => setApprover(e.target.value)} /></label>
      <PermissionButton permissionsState={effectivePermissions} permissions={["bar.payroll.manage"]} disabled={busy} onClick={() => void submit()}>提交已配置审批流程</PermissionButton><button type="button" onClick={() => setTarget(null)}>关闭</button></fieldset> : null}
    <h3>按实际收款确认月结原件</h3><p>未结事项需继续处理，已核实的普通工资仍按期支付；本页确认分配不代表工资已到账。</p>
    <PermissionButton permissionsState={effectivePermissions} permissions={["bar.payroll.manage"]} disabled={busy} onClick={() => void closeMonth()}>核对并生成月结原件</PermissionButton>
    <table className="admin-table"><thead><tr><th>原件及状态</th><th>实收小费／服务费（元）</th><th>应发小费／服务费（元）</th><th>商户扣留／舍入尾差（元）</th><th>人员与未结事项</th></tr></thead><tbody>{runs.map((row, i) => <tr key={row.id ?? "pending-" + i}>
      <td>{row.id ?? "未形成原件"}<p>{labels[row.status] ?? row.status}</p></td>
      <td>{row.status === "pending" ? "待核实" : yuan(row.collectedTipCent) + "／" + yuan(row.collectedServiceFeeCent)}</td>
      <td>{row.status === "pending" ? "待核实" : yuan(row.payableTipCent) + "／" + yuan(row.payableServiceFeeCent)}</td>
      <td>{row.status === "pending" ? "待核实" : yuan(row.withheldServiceFeeCent) + "／" + yuan(row.roundingResidualCent)}<p>尾差主体：{row.settlementEntityId ?? "待核实"}</p></td>
      <td>{row.unresolvedSources.map(text => <p key={text}>{text}</p>)}<details><summary>分配及原账单</summary>{row.allocations.map(a => <p key={a.id}>{a.userId} · {a.componentType === "tip" ? "小费" : "服务费"} {yuan(a.amountCent)} 元；发薪主体 {a.payerCompanyId}；原账单 {a.sourceBillIds.join("、")}；规则 {a.ruleVersionId}</p>)}</details></td>
    </tr>)}</tbody></table>
  </section>;
}
