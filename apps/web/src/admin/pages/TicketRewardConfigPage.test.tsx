// 文件版本：V0.03
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
