// 文件版本：V0.01
import React from "react";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton, useCan } from "../security/Can";
import type { AdminPageProps } from "../types";

type Bill = { id: string; billingMonth: string; sellerEntityId: string; receiverEntityId: string; projectId: string; amountCent: number; visibleAt: string; objectionDeadline?: string; dueDate?: string; paymentStatus: string; paidAmountCent: number; unpaidAmountCent: number; openObjections: number };
type Fee = { rewardId: string; parentTicketId: string; slotIndex: number; useIndex: number; points: number; unitPriceCent: number; amountCent: number; confirmedAt: string };
type Objection = { id: string; reason: string; status: string; resolution?: string };
type Rule = { id: string; version: string; effectiveMonth: string; statementDay: number; objectionWorkingDays: number; payDays: number[]; status: string };
type CalendarCase = { id: string; days: string; sourceNote: string; status: string; reviewId: string };
type Outage = { id: string; projectId: string; unavailableFrom: string; unavailableTo: string; reason: string; status: string };
type ScheduleAdjustment = { id: string; previousDueDate: string; dueDate: string; previousObjectionDeadline: string; objectionDeadline: string; unpaidAmountCent: number; outageId: string };
const BASE="/api/v1/loyalty/ticket-points-billing";
const MANAGE=["ticket.points.billing.manage"];
const VIEW=["ticket.points.billing.view"];
const state: Record<string,string>={pending_review:"待审批",approved:"已批准",rejected:"已驳回",open:"未解决",resolved:"已复核解决",awaiting_publication:"待公布",awaiting_objection_period:"等待异议期及到期日确定",pending_collection:"待收款",settled:"已结清",partial_paid:"部分收款"};
const yuan=(cent:number)=>(cent/100).toFixed(2);

export function TicketPointsBillingPage({api,effectivePermissions,setGlobalError,setActionResult}:AdminPageProps){
  const canManage=useCan(effectivePermissions,MANAGE);
  const [bills,setBills]=React.useState<Bill[]>([]),[rules,setRules]=React.useState<Rule[]>([]);
  const [calendars,setCalendars]=React.useState<CalendarCase[]>([]),[outages,setOutages]=React.useState<Outage[]>([]);
  const [selected,setSelected]=React.useState<Bill|null>(null),[items,setItems]=React.useState<Fee[]>([]),[objections,setObjections]=React.useState<Objection[]>([]);
  const [adjustments,setAdjustments]=React.useState<ScheduleAdjustment[]>([]);
  const [error,setError]=React.useState(""),[busy,setBusy]=React.useState(false),[reason,setReason]=React.useState(""),[approver,setApprover]=React.useState("");
  const [ruleForm,setRuleForm]=React.useState({effectiveMonth:"",statementDay:"1",objectionWorkingDays:"10",minimumDays:"",maximumDays:""});
  const [calendar,setCalendar]=React.useState(""),[calendarSource,setCalendarSource]=React.useState("");
  const [outage,setOutage]=React.useState({projectId:"",unavailableFrom:"",unavailableTo:"",reason:""});
  const load=React.useCallback(async()=>{
    setBills(await api.get<Bill[]>(`${BASE}/bills`));
    if(canManage){const [r,c,o]=await Promise.all([api.get<Rule[]>(`${BASE}/rules`),api.get<CalendarCase[]>(`${BASE}/calendars`),api.get<Outage[]>(`${BASE}/portal-outages`)]);setRules(r);setCalendars(c);setOutages(o);}
  },[api,canManage]);
  React.useEffect(()=>{void load().catch(e=>setError(e.message));},[load]);
  async function select(bill:Bill){setSelected(bill);setItems([]);setObjections([]);setAdjustments([]);try{const [i,o,a]=await Promise.all([api.get<Fee[]>(`${BASE}/bills/${bill.id}/items`),api.get<Objection[]>(`${BASE}/bills/${bill.id}/objections`),api.get<ScheduleAdjustment[]>(`${BASE}/bills/${bill.id}/schedule-adjustments`)]);setItems(i);setObjections(o);setAdjustments(a);}catch(e){setError(e instanceof Error?e.message:"账单明细读取失败");}}
  async function action(run:()=>Promise<unknown>){if(busy)return;setBusy(true);setError("");try{setActionResult(await run());await load();if(selected)await select(selected);}catch(e){const m=e instanceof Error?e.message:"办理失败";setError(m);setGlobalError(m);}finally{setBusy(false);}}
  const note=()=>({idempotencyKey:crypto.randomUUID(),reason,finalApproverId:approver.trim()});
  async function submitCalendar(){
    const days=calendar.trim().split(/\r?\n/).map(line=>{const [date,work,bank]=line.trim().split(/[,，\s]+/);if(!date||!work||!bank||!/^\d{4}-\d{2}-\d{2}$/.test(date)||!["是","否"].includes(work)||!["是","否"].includes(bank))throw new Error("日历每行填写：日期,是否工作日,是否银行工作日；后两项填是或否");return {date,workingDay:work==="是",bankWorkingDay:bank==="是"};});
    return api.post(`${BASE}/calendars`,{...note(),days,sourceNote:calendarSource});
  }
  return <section data-testid="ticket-points-billing-page">
    <AdminPageHeader title="门票积分发行费月结" description="自然月归集、次月出账。异议期从账单可查看日的次日起算；收款结果沿财务往来台账显示。" />
    {error&&<p role="alert">{error}</p>}
    <button onClick={()=>void load().catch(e=>setError(e.message))}>刷新账单</button>
    <table className="admin-table"><thead><tr><th>计费月／销售方</th><th>收款主体</th><th>应收／已收／未收（元）</th><th>可查看日期／异议截止／付款日</th><th>状态</th><th>明细</th></tr></thead><tbody>{bills.map(b=><tr key={b.id}><td>{b.billingMonth} · {b.sellerEntityId}</td><td>{b.receiverEntityId}</td><td>{yuan(b.amountCent)}／{yuan(b.paidAmountCent)}／{yuan(b.unpaidAmountCent)}</td><td>{b.visibleAt?new Date(b.visibleAt).toLocaleDateString("zh-CN"):"待承担方查看权限开通"}／{b.objectionDeadline??"核算中"}／{b.dueDate??"尚未形成应收"}</td><td>{state[b.paymentStatus]??b.paymentStatus}{b.openObjections>0?`；${b.openObjections}项异议待处理`:""}</td><td><button onClick={()=>void select(b)}>查看明细和异议</button></td></tr>)}</tbody></table>
    {selected&&<section><h3>账单履约明细</h3><table className="admin-table"><thead><tr><th>原票／人份／次份</th><th>转可用积分</th><th>发行单价（分）</th><th>发行费（元）</th><th>确认时间</th></tr></thead><tbody>{items.map(i=><tr key={i.rewardId}><td>{i.parentTicketId} · {i.slotIndex} · {i.useIndex}</td><td>{i.points}</td><td>{i.unitPriceCent}</td><td>{yuan(i.amountCent)}</td><td>{new Date(i.confirmedAt).toLocaleString("zh-CN")}</td></tr>)}</tbody></table>
      <label>异议内容／复核说明<input aria-label="异议内容／复核说明" value={reason} onChange={e=>setReason(e.target.value)}/></label><PermissionButton permissionsState={effectivePermissions} permissions={VIEW} disabled={busy||!reason.trim()} onClick={()=>void action(()=>api.post(`${BASE}/bills/${selected.id}/objections`,note()))}>提交账单异议</PermissionButton>
      {objections.map(o=><p key={o.id}>{o.reason} · {state[o.status]??o.status} · {o.resolution??""}{canManage&&o.status==="open"&&<PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busy||!reason.trim()||!approver.trim()} onClick={()=>void action(()=>api.post(`${BASE}/objections/${o.id}/confirm-unchanged`,{...note(),resolution:reason}))}>提交“原金额无误”复核</PermissionButton>}</p>)}
      {adjustments.length>0&&<section><h4>付款期限调整记录</h4><p>原账单与已完成的付款保留，延期仅针对当时未付部分。</p>{adjustments.map(a=><p key={a.id}>已核实整日门户故障：异议截止 {a.previousObjectionDeadline} → {a.objectionDeadline}；可付日 {a.previousDueDate} → {a.dueDate}；当时未付 {yuan(a.unpaidAmountCent)} 元。</p>)}</section>}
    </section>}
    {canManage&&<section><h3>全国账期与日历</h3><p>付账日沿用全平台已配置的三个日期。新版本仅适用于未来账期；逐日工作日和银行工作日必须有真实依据，经审批才用于到期计算。</p>
      <label>全国最终审批人账号<input aria-label="全国最终审批人账号" value={approver} onChange={e=>setApprover(e.target.value)}/></label>
      <label>变更原因<input aria-label="账期变更原因" value={reason} onChange={e=>setReason(e.target.value)}/></label>
      <div className="admin-form-row">{Object.entries({effectiveMonth:"生效计费月",statementDay:"次月出账日",objectionWorkingDays:"异议工作日数",minimumDays:"异议期下限",maximumDays:"异议期上限"}).map(([key,label])=><label key={key}>{label}<input aria-label={label} type={key==="effectiveMonth"?"month":"number"} value={ruleForm[key as keyof typeof ruleForm]} onChange={e=>setRuleForm(f=>({...f,[key]:e.target.value}))}/></label>)}
      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busy||Object.values(ruleForm).some(v=>!v.trim())} onClick={()=>void action(()=>api.post(`${BASE}/rules`,{...note(),effectiveMonth:`${ruleForm.effectiveMonth}-01`,statementDay:Number(ruleForm.statementDay),objectionWorkingDays:Number(ruleForm.objectionWorkingDays),minimumDays:Number(ruleForm.minimumDays),maximumDays:Number(ruleForm.maximumDays)}))}>提交账期审批</PermissionButton></div>
      <ul>{rules.map(r=><li key={r.id}>{r.version} · {r.effectiveMonth}生效 · 次月{r.statementDay}日出账 · {r.objectionWorkingDays}工作日异议 · 付账日{r.payDays.join("、")} · {state[r.status]??r.status}</li>)}</ul>
      <label>工作日与银行日历<textarea aria-label="工作日与银行日历" placeholder="2026-11-01,否,否" value={calendar} onChange={e=>setCalendar(e.target.value)}/></label>
      <label>日历依据<input aria-label="日历依据" value={calendarSource} onChange={e=>setCalendarSource(e.target.value)}/></label><PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busy||!calendar.trim()||!calendarSource.trim()} onClick={()=>void action(submitCalendar)}>提交日历审批</PermissionButton>
      {calendars.map(c=><details key={c.id}><summary>{c.sourceNote} · {state[c.status]??c.status}</summary><ul>{(JSON.parse(c.days) as Array<{date:string;workingDay:boolean;bankWorkingDay:boolean}>).map(d=><li key={d.date}>{d.date}：工作日{d.workingDay?"是":"否"}，银行工作日{d.bankWorkingDay?"是":"否"}</li>)}</ul></details>)}
      <h3>商户专区不可用事实</h3><p>只排除项目当地全天不可用的自然日；部分时段故障不延长异议期。</p>
      {Object.entries({projectId:"故障综合体编号",unavailableFrom:"不可用开始",unavailableTo:"恢复时间",reason:"故障核实依据"}).map(([key,label])=><label key={key}>{label}<input aria-label={label} type={key==="unavailableFrom"||key==="unavailableTo"?"datetime-local":"text"} value={outage[key as keyof typeof outage]} onChange={e=>setOutage(f=>({...f,[key]:e.target.value}))}/></label>)}
      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busy||Object.values(outage).some(v=>!v.trim())} onClick={()=>void action(()=>api.post(`${BASE}/portal-outages`,{...note(),...outage,unavailableFrom:new Date(outage.unavailableFrom).toISOString(),unavailableTo:new Date(outage.unavailableTo).toISOString()}))}>提交故障事实审批</PermissionButton>
      <ul>{outages.map(o=><li key={o.id}>{o.projectId} · {new Date(o.unavailableFrom).toLocaleString("zh-CN")} 至 {new Date(o.unavailableTo).toLocaleString("zh-CN")} · {o.reason} · {state[o.status]??o.status}</li>)}</ul>
    </section>}
  </section>;
}
