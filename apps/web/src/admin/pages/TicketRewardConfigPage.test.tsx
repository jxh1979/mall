// 文件版本：V0.07
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { TicketRewardConfigPage } from "./TicketRewardConfigPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

afterEach(()=>vi.unstubAllGlobals());
it("未批准配置不能发布，缺少数值上下限不能提交",async()=>{
  const posts:string[]=[];
  vi.stubGlobal("fetch",vi.fn(async(input:RequestInfo|URL,init?:RequestInit)=>{
    const url=String(input);if(init?.method==="POST")posts.push(url);let data:unknown=[];
    if(url.includes("/platform/projects"))data=[{id:"p1",name:"长沙项目"}];
    if(url.includes("/ticket-types"))data=[{id:"t1",ticket_name:"日票"}];
    if(url.includes("/ticket-reward-configs"))data=[{id:"r1",kind:"points",version:"V0.01",status:"pending_review",effective_at:"2027-01-01T00:00:00Z",values:{fixed_value:100}}];
    return new Response(JSON.stringify({success:true,code:"OK",data}),{headers:{"Content-Type":"application/json"}});
  }));
  const props:AdminPageProps={api:new AdminApi("test"),user:{id:"u",username:"admin",accountType:"platform_admin"},setActionResult:vi.fn(),setGlobalError:vi.fn(),effectivePermissions:{status:"ready",value:{grants:["ticket.reward.config.view","ticket.reward.config.manage"],denies:[],wildcard:false}}};
  render(<TicketRewardConfigPage {...props}/>);
  await screen.findByText("长沙项目");fireEvent.change(screen.getByLabelText("综合体"),{target:{value:"p1"}});
  await screen.findByText("待审批");fireEvent.change(screen.getByLabelText("公告说明"),{target:{value:"提前公告"}});
  expect(screen.getByText("发布已审批版本")).toBeDisabled();
  fireEvent.change(screen.getByLabelText("每人份／次份固定积分"),{target:{value:"100"}});
  fireEvent.click(screen.getByText("提交审批"));
  await screen.findByText("请填写积分下限");expect(posts).toHaveLength(0);
  fireEvent.change(screen.getByLabelText("配置类型"),{target:{value:"recovery_waiver"}});
  expect(screen.queryByLabelText("综合体")).not.toBeInTheDocument();
  expect(screen.getByLabelText("会员全部待追回积分豁免线")).toHaveValue(100);
  fireEvent.click(screen.getByText("提交审批"));
  await screen.findByText("请填写豁免线下限");expect(posts).toHaveLength(0);
});

// 第十一批 TK-04-R1（BSM L822「按商户业态可配，须同时配置上下限」；操作员 2026-10-21 裁决补充第 4 条「按商户业态分别配置」）：
// 商户业态积分获取率与成长值规则全国生效、不选综合体和票种，提交体带 business_format；全国参数另填商户消费等待期及上下限。
it("商户业态规则提交体含 business_format，全国参数含商户消费等待期",async()=>{
  const bodies:Record<string,unknown>[]=[];
  vi.stubGlobal("fetch",vi.fn(async(input:RequestInfo|URL,init?:RequestInit)=>{
    const url=String(input);if(init?.method==="POST")bodies.push(JSON.parse(String(init.body)));let data:unknown=[];
    if(url.includes("/platform/projects"))data=[{id:"p1",name:"长沙项目"}];
    if(init?.method==="POST")data={id:"r9",kind:"merchant_points",version:"V0.01",status:"pending_review",effective_at:"2099-01-01T00:00:00Z",values:{}};
    return new Response(JSON.stringify({success:true,code:"OK",data}),{headers:{"Content-Type":"application/json"}});
  }));
  const props:AdminPageProps={api:new AdminApi("test"),user:{id:"u",username:"admin",accountType:"platform_admin"},setActionResult:vi.fn(),setGlobalError:vi.fn(),effectivePermissions:{status:"ready",value:{grants:["ticket.reward.config.view","ticket.reward.config.manage"],denies:[],wildcard:false}}};
  render(<TicketRewardConfigPage {...props}/>);
  await waitFor(()=>expect(screen.getByText("长沙项目")).toBeInTheDocument());
  fireEvent.change(screen.getByLabelText("配置类型"),{target:{value:"national"}});
  await waitFor(()=>expect(screen.queryByLabelText("商户消费履约完成后等待小时数")).not.toBeNull());
  expect(screen.getByLabelText("商户消费履约完成后等待小时数")).toHaveValue(24);
  fireEvent.change(screen.getByLabelText("配置类型"),{target:{value:"merchant_points"}});
  expect(screen.queryByLabelText("综合体")).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("商户业态"),{target:{value:"bar"}});
  fireEvent.change(screen.getByLabelText("每产生 1 积分所需消费（分）"),{target:{value:"1000"}});
  fireEvent.change(screen.getByLabelText("积分获取率下限（分）"),{target:{value:"100"}});
  fireEvent.change(screen.getByLabelText("积分获取率上限（分）"),{target:{value:"10000"}});
  fireEvent.change(screen.getByLabelText("生效时间"),{target:{value:"2099-01-01T08:00"}});
  fireEvent.change(screen.getByLabelText("变更原因"),{target:{value:"酒吧业态积分获取率"}});
  fireEvent.change(screen.getByLabelText("最终审批人账号"),{target:{value:"u_domain"}});
  fireEvent.click(screen.getByText("提交审批"));
  await waitFor(()=>expect(bodies).toHaveLength(1));
  expect(bodies[0]).toMatchObject({kind:"merchant_points",business_format:"bar",project_id:null,ticket_type_id:null,final_approver_id:"u_domain",values:{fixed_value:1000,minimum_value:100,maximum_value:10000}});
});

// OBS-13-01（BSM L820「滚动统计周期／保护期／提前通知天数（业务参数，全平台统一可配……须同时配置上下限）」「变更须经审批、只对后续周期生效」）：
// 三项等级参数全国生效、不选综合体和票种，值与上下限同配后提交审批，无需公告。
it("会员等级保护期全平台统一，值与上下限同配提交审批",async()=>{
  const bodies:Record<string,unknown>[]=[];
  vi.stubGlobal("fetch",vi.fn(async(input:RequestInfo|URL,init?:RequestInit)=>{
    const url=String(input);if(init?.method==="POST")bodies.push(JSON.parse(String(init.body)));let data:unknown=[];
    if(url.includes("/platform/projects"))data=[{id:"p1",name:"长沙项目"}];
    if(url.includes("/ticket-reward-configs")&&init?.method!=="POST")data=[{id:"r7",kind:"level_protection_months",version:"V0.01",status:"approved",effective_at:"2000-01-01T00:00:00Z",values:{fixed_value:12}}];
    if(init?.method==="POST")data={id:"r8",kind:"level_protection_months",version:"V0.02",status:"pending_review",effective_at:"2099-01-01T00:00:00Z",values:{}};
    return new Response(JSON.stringify({success:true,code:"OK",data}),{headers:{"Content-Type":"application/json"}});
  }));
  const props:AdminPageProps={api:new AdminApi("test"),user:{id:"u",username:"admin",accountType:"platform_admin"},setActionResult:vi.fn(),setGlobalError:vi.fn(),effectivePermissions:{status:"ready",value:{grants:["ticket.reward.config.view","ticket.reward.config.manage"],denies:[],wildcard:false}}};
  render(<TicketRewardConfigPage {...props}/>);
  await waitFor(()=>expect(screen.queryByRole("option",{name:"会员等级保护期（月）"})).not.toBeNull());
  fireEvent.change(screen.getByLabelText("配置类型"),{target:{value:"level_protection_months"}});
  expect(screen.queryByLabelText("综合体")).not.toBeInTheDocument();
  await waitFor(()=>expect(screen.queryByText("无需公告")).not.toBeNull());
  fireEvent.change(screen.getByLabelText("保护期（月）"),{target:{value:"6"}});
  fireEvent.change(screen.getByLabelText("保护期下限（月）"),{target:{value:"1"}});
  fireEvent.change(screen.getByLabelText("保护期上限（月）"),{target:{value:"24"}});
  fireEvent.change(screen.getByLabelText("生效时间"),{target:{value:"2099-01-01T08:00"}});
  fireEvent.change(screen.getByLabelText("变更原因"),{target:{value:"保护期调整"}});
  fireEvent.change(screen.getByLabelText("最终审批人账号"),{target:{value:"u_platform"}});
  fireEvent.click(screen.getByText("提交审批"));
  await waitFor(()=>expect(bodies).toHaveLength(1));
  expect(bodies[0]).toMatchObject({kind:"level_protection_months",project_id:null,ticket_type_id:null,final_approver_id:"u_platform",values:{fixed_value:6,minimum_value:1,maximum_value:24}});
  expect(bodies[0]).not.toHaveProperty("business_format");
});

// 第十八批第 11 条（ENUM L86「赠送比例可配（业务参数，按项目可配，须同时配置上下限）」；BSM L521 涉钱配置版本化、经审批按指定日期生效）：通用卡只选综合体（可空）、
// 不选票种；值与上下限同配提交审批，无需公告。第十八批 CL 段 423-R3（第十八批裁决 85「平台不再代录、不再审批」）：平台账号不再出现商户级卡赠送比例一类。
it("平台账号只提交通用卡赠送比例，不再代录商户级卡",async()=>{
  const bodies:Record<string,unknown>[]=[];
  vi.stubGlobal("fetch",vi.fn(async(input:RequestInfo|URL,init?:RequestInit)=>{
    const url=String(input);if(init?.method==="POST")bodies.push(JSON.parse(String(init.body)));let data:unknown=[];
    if(url.includes("/platform/projects"))data=[{id:"p1",name:"长沙项目"}];
    if(init?.method==="POST")data={id:"r6",kind:"stored_value_bonus",version:"V0.02",status:"pending_review",effective_at:"2099-01-01T00:00:00Z",values:{}};
    return new Response(JSON.stringify({success:true,code:"OK",data}),{headers:{"Content-Type":"application/json"}});
  }));
  const props:AdminPageProps={api:new AdminApi("test"),user:{id:"u",username:"admin",accountType:"platform_admin"},setActionResult:vi.fn(),setGlobalError:vi.fn(),effectivePermissions:{status:"ready",value:{grants:["ticket.reward.config.view","ticket.reward.config.manage"],denies:[],wildcard:false}}};
  render(<TicketRewardConfigPage {...props}/>);
  await waitFor(()=>expect(screen.queryByRole("option",{name:"综合体通用卡充值赠送比例"})).not.toBeNull());
  expect(screen.queryByRole("option",{name:/商户级卡充值赠送比例/})).toBeNull();
  fireEvent.change(screen.getByLabelText("配置类型"),{target:{value:"stored_value_bonus"}});
  expect(screen.queryByLabelText("票种")).not.toBeInTheDocument();
  expect(screen.queryByLabelText("发卡商户编号")).not.toBeInTheDocument();
  await waitFor(()=>expect(screen.queryByRole("option",{name:"长沙项目"})).not.toBeNull());
  fireEvent.change(screen.getByLabelText("综合体"),{target:{value:"p1"}});
  fireEvent.change(screen.getByLabelText("赠送比例（万分比）"),{target:{value:"500"}});
  fireEvent.change(screen.getByLabelText("赠送比例下限（万分比）"),{target:{value:"0"}});
  fireEvent.change(screen.getByLabelText("赠送比例上限（万分比）"),{target:{value:"1000"}});
  fireEvent.change(screen.getByLabelText("生效时间"),{target:{value:"2099-01-01T08:00"}});
  fireEvent.change(screen.getByLabelText("变更原因"),{target:{value:"通用卡赠送比例"}});
  fireEvent.change(screen.getByLabelText("最终审批人账号"),{target:{value:"u_platform"}});
  fireEvent.click(screen.getByText("提交审批"));
  await waitFor(()=>expect(bodies).toHaveLength(1));
  expect(bodies[0]).toMatchObject({kind:"stored_value_bonus",project_id:"p1",ticket_type_id:null,values:{fixed_value:500,minimum_value:0,maximum_value:1000}});
  expect(bodies[0]).not.toHaveProperty("merchant_id");
});

// 第十八批 CL 段 423-R3（第十八批裁决 85：ENUM:86 改句「赠送比例由发卡商户在商户后台自行设置」；「照旧先存草案、填生效日、经本商户有审批权的人转正后生效」）：
// 商户账号提交本商户的商户级卡赠送比例，不填发卡商户编号（取本账号商户）、不选综合体；列表只列本商户的版本，无需公告。
// 18CL 收尾复核：项目、主体列表只对平台开放（后端 requirePlatformScope 对商户账号一律 403），商户账号不请求，页面不出现读取失败（修复前打开即报错）。
it("商户账号提交本商户的商户级卡赠送比例",async()=>{
  const bodies:Record<string,unknown>[]=[];const platformCalls:string[]=[];
  vi.stubGlobal("fetch",vi.fn(async(input:RequestInfo|URL,init?:RequestInit)=>{
    const url=String(input);if(init?.method==="POST")bodies.push(JSON.parse(String(init.body)));let data:unknown=[];
    if(url.includes("/platform/")){platformCalls.push(url);return new Response(JSON.stringify({success:false,code:"FORBIDDEN",detail_code:"PLATFORM_SCOPE_REQUIRED",message:"商户账号无权查看平台级经营分析"}),{status:403,headers:{"Content-Type":"application/json"}});}
    if(url.includes("/ticket-reward-configs")&&init?.method!=="POST")data=[{id:"r5",kind:"merchant_card_bonus",merchant_id:"m1",version:"V0.01",status:"approved",effective_at:"2099-01-01T00:00:00Z",values:{fixed_value:1000}}];
    if(init?.method==="POST")data={id:"r6",kind:"merchant_card_bonus",version:"V0.02",status:"pending_review",effective_at:"2099-01-01T00:00:00Z",values:{}};
    return new Response(JSON.stringify({success:true,code:"OK",data}),{headers:{"Content-Type":"application/json"}});
  }));
  const props:AdminPageProps={api:new AdminApi("test"),user:{id:"u2",username:"shop",accountType:"merchant_admin",merchantId:"m1"},setActionResult:vi.fn(),setGlobalError:vi.fn(),effectivePermissions:{status:"ready",value:{grants:["ticket.reward.config.view","ticket.reward.config.manage"],denies:[],wildcard:false}}};
  render(<TicketRewardConfigPage {...props}/>);
  await waitFor(()=>expect(screen.queryByRole("option",{name:"商户级卡充值赠送比例（本商户自行设置）"})).not.toBeNull());
  fireEvent.change(screen.getByLabelText("配置类型"),{target:{value:"merchant_card_bonus"}});
  expect(screen.queryByLabelText("综合体")).not.toBeInTheDocument();
  expect(screen.queryByLabelText("发卡商户编号")).not.toBeInTheDocument();
  await waitFor(()=>expect(screen.queryByText("无需公告")).not.toBeNull());
  expect(screen.queryByRole("alert")).toBeNull();expect(platformCalls).toHaveLength(0);
  fireEvent.change(screen.getByLabelText("赠送比例（万分比）"),{target:{value:"1000"}});
  fireEvent.change(screen.getByLabelText("赠送比例下限（万分比）"),{target:{value:"0"}});
  fireEvent.change(screen.getByLabelText("赠送比例上限（万分比）"),{target:{value:"2000"}});
  fireEvent.change(screen.getByLabelText("生效时间"),{target:{value:"2099-01-01T08:00"}});
  fireEvent.change(screen.getByLabelText("变更原因"),{target:{value:"本商户充值活动"}});
  fireEvent.change(screen.getByLabelText("最终审批人账号"),{target:{value:"u_shop_manager"}});
  fireEvent.click(screen.getByText("提交审批"));
  await waitFor(()=>expect(bodies).toHaveLength(1));
  expect(bodies[0]).toMatchObject({kind:"merchant_card_bonus",merchant_id:"m1",project_id:null,ticket_type_id:null,final_approver_id:"u_shop_manager",values:{fixed_value:1000,minimum_value:0,maximum_value:2000}});
});
