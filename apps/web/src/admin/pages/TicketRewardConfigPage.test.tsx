// 文件版本：V0.01
import { fireEvent, render, screen } from "@testing-library/react";
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
});
