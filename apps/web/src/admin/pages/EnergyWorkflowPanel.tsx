// 文件版本：V0.01
import React from "react";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";
import { Tabs } from "antd";

type Meter={id:string;name:string;projectId?:string|null};
type Reading={id:string;meterId:string;status:string};
type Fact={id:string;method:string;reason:string;verifiedValue:number;verifiedOpeningValue:number;verifiedBy:string;reviewId:string;status:string;attachmentIds:string[]};
type History={opening?:{initialValue:number;initialAt:string;attachmentIds:string[]}|null;retirement?:{terminalReadingId:string;retiredAt:string}|null;replacements?:Array<{id:string;oldMeterId:string;newMeterId:string;oldFinalValue:number;newInitialValue:number;replacedAt:string}>;readings?:Array<{original:{id:string;readingValue:number;prevReadingValue:number|null;consumption:number|null;status:string};confirmedValue:number;confirmedConsumption:number|null;evidence?:{attachmentIds:string[];note:string};verifications:Fact[];assignments:Array<{verifierId:string;reason:string}>;escalation?:{supervisorId?:string;deadlineAt?:string}}>};
type Charge={id:string;meterId:string;contractId:string;amountCent:number;consumption:number;dueDate:string;arApItemId?:string|null;settledAt?:string|null;attachmentIds:string[]};
type Props=Pick<AdminPageProps,"api"|"effectivePermissions"|"setActionResult"|"setGlobalError"> & {meters:Meter[];readings:Reading[];period:string;reload:()=>Promise<unknown>};
const permissions=["energy.reading.manage"];
const labels:Record<string,string>={pending_review:"待主管确认",approved:"已通过",rejected:"已驳回",reread:"第二人复抄",onsite_check:"到场核表"};

export function EnergyWorkflowPanel({api,effectivePermissions,setActionResult,setGlobalError,meters,readings,period,reload}:Props){
  const [meterId,setMeterId]=React.useState("");
  const [files,setFiles]=React.useState<Array<{id:string;name:string}>>([]);
  const [busy,setBusy]=React.useState(false);const [error,setError]=React.useState("");
  const [history,setHistory]=React.useState<History|null>(null);const [charges,setCharges]=React.useState<Charge[]>([]);
  const [initial,setInitial]=React.useState("");const [at,setAt]=React.useState("");const [note,setNote]=React.useState("");
  const [newName,setNewName]=React.useState("");const [oldFinal,setOldFinal]=React.useState("");
  const [readingId,setReadingId]=React.useState("");const [confirmed,setConfirmed]=React.useState("");const [opening,setOpening]=React.useState("");
  const [method,setMethod]=React.useState("reread");const [approver,setApprover]=React.useState("");const [verifier,setVerifier]=React.useState("");
  const [contract,setContract]=React.useState("");const [amount,setAmount]=React.useState("");const [due,setDue]=React.useState("");const [reference,setReference]=React.useState("");
  const [days,setDays]=React.useState("7");const [min,setMin]=React.useState("");const [max,setMax]=React.useState("");const [supervisor,setSupervisor]=React.useState("");
  const [escalations,setEscalations]=React.useState<Array<{readingId:string;meterId:string;period:string;deadlineAt:string}>>([]);
  const [settings,setSettings]=React.useState<Array<{id:string;days:number;minDays:number;maxDays:number;supervisorId:string;status:string}>>([]);
  const meter=meters.find(m=>m.id===meterId);const ids=files.map(f=>f.id);
  const key=()=>crypto.randomUUID();
  async function run(name:string,body:()=>Promise<unknown>){if(busy)return;setBusy(true);setError("");try{const result=await body();setActionResult({action:name,result});await reload();}catch(e){const message=e instanceof Error?e.message:"能源操作未完成";setError(message);setGlobalError(message);}finally{setBusy(false);}}
  async function upload(selected:FileList|null){if(!meterId||!selected?.length||busy)return;await run("energy_evidence_uploaded",async()=>{if(!api.postForm)throw new Error("当前客户端不能上传凭据");for(const file of Array.from(selected)){const body=new FormData();body.append("file",file);body.append("business_type","energy_meter");body.append("business_id",meterId);body.append("attachment_role","other");const saved=await api.postForm<{id:string}>("/api/v1/evidence/attachments/upload",body);setFiles(old=>[...old,{id:saved.id,name:file.name}]);}});}
  function requireMeter(){if(!meterId)throw new Error("请先选择表计");}
  function requireProof(){requireMeter();if(!ids.length)throw new Error("须先上传表底照片或实际费用凭据");}
  function number(value:string){if(value.trim()===""||!Number.isFinite(Number(value))||Number(value)<0)throw new Error("须填写核实后的非负读数");return Number(value);}
  function timestamp(){if(!at||Number.isNaN(new Date(at).getTime()))throw new Error("须填写实际交接时点");return new Date(at).toISOString();}
  const proofLinks=(items:string[])=>items.map((id,n)=><a key={id} href={`/api/v1/energy/meters/${encodeURIComponent(meterId)}/attachments/${encodeURIComponent(id)}/file`} target="_blank" rel="noreferrer">凭据{n+1} </a>);
  return <section data-testid="energy-workflow-panel">
    <p>商户只代收独立表实际费用。公区及水电损含在物业费中；未核异常仍阻止该商户整期能源出账。</p>
    <PermissionButton permissionsState={effectivePermissions} permissions={["energy.reading.approve"]} disabled={busy} onClick={()=>void run("energy_escalations_loaded",async()=>setEscalations(await api.get("/api/v1/energy/escalations")))}>查看指派给我的超期异常</PermissionButton>
    <ul>{escalations.map(e=><li key={e.readingId}>{e.period}，表计{e.meterId}，核实期限{new Date(e.deadlineAt).toLocaleString("zh-CN")} <button type="button" onClick={()=>{setMeterId(e.meterId);setReadingId(e.readingId);setFiles([]);setHistory(null);}}>选择此异常办理复抄</button></li>)}</ul>
    <label>处理表计<select aria-label="能源处理表计" value={meterId} onChange={e=>{setMeterId(e.target.value);setFiles([]);setHistory(null);setReadingId("");}}><option value="">请选择</option>{meters.map(m=><option key={m.id} value={m.id}>{m.name}（{m.id}）</option>)}</select></label>
    <label>真实凭据<input aria-label="能源真实凭据" type="file" multiple disabled={!meterId||busy} onChange={e=>void upload(e.target.files)}/></label>
    <ul>{files.map(f=><li key={f.id}>{f.name} {proofLinks([f.id])}</li>)}</ul>
    <label>核实或交接说明<input aria-label="能源处理说明" value={note} onChange={e=>setNote(e.target.value)}/></label>
    {error&&<p role="alert">{error}</p>}
    <Tabs items={[
      {key:"opening",label:"启用、换表与停用",children:<div>
        <label>实际交接时点<input aria-label="能源交接时点" type="datetime-local" value={at} onChange={e=>setAt(e.target.value)}/></label>
        <label>新表启用初值<input aria-label="能源启用初值" value={initial} onChange={e=>setInitial(e.target.value)}/></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["energy.meter.manage"]} disabled={busy} onClick={()=>void run("energy_opening_recorded",async()=>{requireProof();return api.post(`/api/v1/energy/meters/${encodeURIComponent(meterId)}/opening`,{idempotencyKey:key(),initialValue:number(initial),initialAt:timestamp(),note,attachmentIds:ids});})}>登记本表初值</PermissionButton>
        <label>新表名称<input aria-label="能源新表名称" value={newName} onChange={e=>setNewName(e.target.value)}/></label>
        <label>旧表终值<input aria-label="能源旧表终值" value={oldFinal} onChange={e=>setOldFinal(e.target.value)}/></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["energy.meter.manage","energy.reading.manage"]} disabled={busy} onClick={()=>void run("energy_meter_replaced",async()=>{requireProof();return api.post(`/api/v1/energy/meters/${encodeURIComponent(meterId)}/replacement`,{idempotencyKey:key(),newMeterName:newName,period,oldFinalValue:number(oldFinal),newInitialValue:number(initial),replacedAt:timestamp(),note,attachmentIds:ids});})}>登记真实换表</PermissionButton>
        <p>换表后旧表停用；新表继承原计费对象，并从已登记初值计量。旧段终值仍须审核。</p>
        <PermissionButton permissionsState={effectivePermissions} permissions={["energy.meter.manage","energy.reading.manage"]} disabled={busy} onClick={()=>void run("energy_meter_retired",async()=>{requireProof();return api.post(`/api/v1/energy/meters/${encodeURIComponent(meterId)}/retirement`,{idempotencyKey:key(),period,finalValue:number(oldFinal),retiredAt:timestamp(),note,attachmentIds:ids});})}>按实际终值永久停用本表</PermissionButton>
        <p>永久停用需填写旧表终值、实际交接时点并上传照片；不要求创建替代表。终段仍须审核及核实实际费用，停用不免除本期出账核对。</p>
      </div>},
      {key:"verify",label:"异常核表",children:<div>
        <label>待核异常<select aria-label="能源待核异常" value={readingId} onChange={e=>setReadingId(e.target.value)}><option value="">请选择</option>{readings.filter(r=>r.meterId===meterId&&r.status==="abnormal").map(r=><option key={r.id} value={r.id}>{r.id}</option>)}</select></label>
        <label>核实方式<select aria-label="能源核实方式" value={method} onChange={e=>setMethod(e.target.value)}><option value="reread">第二人复抄</option><option value="onsite_check">到场核表</option></select></label>
        <label>核实终值<input aria-label="能源核实终值" value={confirmed} onChange={e=>setConfirmed(e.target.value)}/></label>
        <label>缺失起点的核实初值<input aria-label="能源核实起点" value={opening} onChange={e=>setOpening(e.target.value)}/></label>
        <label>最终确认主管编号<input aria-label="能源确认主管" value={approver} onChange={e=>setApprover(e.target.value)}/></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={permissions} disabled={busy} onClick={()=>void run("energy_verification_submitted",async()=>{requireProof();if(!readingId)throw new Error("请选择异常读数");return api.post(`/api/v1/energy/readings/${encodeURIComponent(readingId)}/verification`,{idempotencyKey:key(),verifiedValue:number(confirmed),verifiedOpeningValue:opening.trim()?number(opening):undefined,method,reason:note,attachmentIds:ids,finalApproverId:approver});})}>以本人身份提交核表并申请主管确认</PermissionButton>
        <p>系统记录当前登录的真实核表人；主管在现有主管复核页确认。原始读数不会覆盖。</p>
        <label>指派复抄人员编号<input aria-label="能源指派复抄人" value={verifier} onChange={e=>setVerifier(e.target.value)}/></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["energy.reading.approve"]} disabled={busy} onClick={()=>void run("energy_verifier_assigned",()=>api.post(`/api/v1/energy/readings/${encodeURIComponent(readingId)}/assignment`,{idempotencyKey:key(),verifierId:verifier,reason:note}))}>主管指派复抄</PermissionButton>
      </div>},
      {key:"charge",label:"独表实际费用",children:<div>
        <label>有效合同编号<input aria-label="能源实际费用合同" value={contract} onChange={e=>setContract(e.target.value)}/></label>
        <label>供能单位账单号<input aria-label="能源供能账单号" value={reference} onChange={e=>setReference(e.target.value)}/></label>
        <label>实际费用（元）<input aria-label="能源实际费用金额" value={amount} onChange={e=>setAmount(e.target.value)}/></label>
        <label>供能单位最后付款日<input aria-label="能源最后付款日" type="date" value={due} onChange={e=>setDue(e.target.value)}/></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={permissions} disabled={busy} onClick={()=>void run("energy_actual_charge_recorded",async()=>{requireProof();if(!/^\d+(\.\d{1,2})?$/.test(amount)||!Number.isSafeInteger(Math.round(Number(amount)*100)))throw new Error("金额须为非负金额，最多两位小数");return api.post("/api/v1/energy/actual-charges",{idempotencyKey:key(),contractId:contract,meterId,period,amountCent:Math.round(Number(amount)*100),dueDate:due,supplierBillReference:reference,attachmentIds:ids});})}>确认凭据与独表用量一致，登记实际费用</PermissionButton>
        <button type="button" disabled={busy} onClick={()=>void run("energy_actual_charge_loaded",async()=>{const rows=await api.get<Charge[]>(`/api/v1/energy/actual-charges?period=${encodeURIComponent(period)}`);setCharges(rows??[]);})}>查看本期实际费用</button>
        <table><thead><tr><th>表计</th><th>实际用量</th><th>实际费用</th><th>最后付款日</th><th>往来状态</th></tr></thead><tbody>{charges.map(c=><tr key={c.id}><td>{c.meterId}</td><td>{c.consumption}</td><td>¥{(c.amountCent/100).toFixed(2)}</td><td>{c.dueDate}</td><td>{c.settledAt?c.arApItemId?"已生成代收往来":"零费用已核结":"待整期出账"}</td></tr>)}</tbody></table>
      </div>},
      {key:"deadline",label:"核实期限",children:<div>
        <p>默认核实天数为7天；上下限、项目主管须填全并审批后生效。缺配置或超期都不能跳过异常核实。</p>
        <label>核实天数<input aria-label="能源核实天数" value={days} onChange={e=>setDays(e.target.value)}/></label>
        <label>天数下限<input aria-label="能源核实下限" value={min} onChange={e=>setMin(e.target.value)}/></label>
        <label>天数上限<input aria-label="能源核实上限" value={max} onChange={e=>setMax(e.target.value)}/></label>
        <label>本项目能耗主管编号<input aria-label="能源项目主管" value={supervisor} onChange={e=>setSupervisor(e.target.value)}/></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["energy.meter.manage"]} disabled={busy} onClick={()=>void run("energy_review_settings_submitted",async()=>{requireMeter();if(!meter?.projectId)throw new Error("所选表计缺项目归属");if(![days,min,max].every(x=>/^\d+$/.test(x)))throw new Error("期限与上下限均须填写正整数");return api.post(`/api/v1/energy/projects/${encodeURIComponent(meter.projectId)}/review-settings`,{idempotencyKey:key(),days:Number(days),minDays:Number(min),maxDays:Number(max),supervisorId:supervisor,reason:note});})}>提交期限配置审批</PermissionButton>
        <button type="button" disabled={busy||!meter?.projectId} onClick={()=>void run("energy_review_settings_loaded",async()=>{setSettings(await api.get(`/api/v1/energy/projects/${encodeURIComponent(meter!.projectId!)}/review-settings`));})}>查看配置记录</button>
        <ul>{settings.map(s=><li key={s.id}>{s.days}天（{s.minDays}～{s.maxDays}天），主管{s.supervisorId}，{labels[s.status]??"待处理"}</li>)}</ul>
      </div>},
      {key:"history",label:"计量原件与处置记录",children:<div>
        <button type="button" disabled={busy||!meterId} onClick={()=>void run("energy_history_loaded",async()=>setHistory(await api.get(`/api/v1/energy/meters/${encodeURIComponent(meterId)}/history`)))}>读取本表完整记录</button>
        {history&&<div data-testid="energy-meter-history">
          <p>启用初值：{history.opening?.initialValue??"待核实"} {proofLinks(history.opening?.attachmentIds??[])}</p>
          {history.retirement&&<p>永久停用：{new Date(history.retirement.retiredAt).toLocaleString("zh-CN")}，终段读数{history.retirement.terminalReadingId}，原计量和费用继续保留。</p>}
          {(history.replacements??[]).map(x=><p key={x.id}>旧表{x.oldMeterId}终值{x.oldFinalValue}；新表{x.newMeterId}初值{x.newInitialValue}；{new Date(x.replacedAt).toLocaleString("zh-CN")}</p>)}
          {(history.readings??[]).map(r=><article key={r.original.id}><p>原件{r.original.id}：原读数{r.original.readingValue}，原起点{r.original.prevReadingValue??"待核"}；核实后用量{r.confirmedConsumption??"待核"} {proofLinks(r.evidence?.attachmentIds??[])}</p>
            {r.verifications.map(v=><p key={v.id}>{labels[v.method]}：起点{v.verifiedOpeningValue}、终值{v.verifiedValue}，核表人{v.verifiedBy}，{v.reason}，{labels[v.status]}，审批单{v.reviewId} {proofLinks(v.attachmentIds)}</p>)}
            {r.escalation?.supervisorId&&<p>已超期，已升级给主管{r.escalation.supervisorId}，等待指派和核实。</p>}
            {r.assignments.map((a,n)=><p key={n}>指派复抄人{a.verifierId}：{a.reason}</p>)}
          </article>)}
        </div>}
      </div>},
    ]}/>
  </section>;
}
