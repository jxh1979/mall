// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { TicketPointsBillingPage } from "./TicketPointsBillingPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

afterEach(()=>vi.unstubAllGlobals());
it("商户只查看自身账单及原人次费用，不显示全国账期配置",async()=>{
  const urls:string[]=[];
  vi.stubGlobal("fetch",vi.fn(async(input:RequestInfo|URL)=>{
    const url=String(input);urls.push(url);let data:unknown=[];
    if(url.endsWith("/bills"))data=[{id:"bill1",billing_month:"2026-01-01",seller_entity_id:"销售酒吧",receiver_entity_id:"积分中心",project_id:"p1",amount_cent:100,paid_amount_cent:0,unpaid_amount_cent:100,visible_at:"2026-02-01T08:00:00Z",objection_deadline:"2026-02-13",due_date:"2026-02-16",payment_status:"awaiting_objection_period",open_objections:1}];
    if(url.endsWith("/items"))data=[{reward_id:"r1",parent_ticket_id:"原票1",slot_index:2,use_index:3,points:100,unit_price_cent:1,amount_cent:100}];
    if(url.endsWith("/objections"))data=[{id:"o1",reason:"核对本次费用",status:"open"}];
    if(url.endsWith("/schedule-adjustments"))data=[{id:"a1",previous_due_date:"2026-02-16",due_date:"2026-02-25",previous_objection_deadline:"2026-02-13",objection_deadline:"2026-02-16",unpaid_amount_cent:60,outage_id:"outage1"}];
    return new Response(JSON.stringify({success:true,code:"OK",data}),{headers:{"Content-Type":"application/json"}});
  }));
  const props:AdminPageProps={api:new AdminApi("test"),user:{id:"u",username:"merchant",accountType:"merchant"},setActionResult:vi.fn(),setGlobalError:vi.fn(),effectivePermissions:{status:"ready",value:{grants:["ticket.points.billing.view"],denies:[],wildcard:false}}};
  render(<TicketPointsBillingPage {...props}/>);
  await screen.findByText(/销售酒吧/);
  expect(screen.getByText(/2026-02-13/)).toBeInTheDocument();
  expect(screen.queryByText("全国账期与日历")).not.toBeInTheDocument();
  fireEvent.click(screen.getByText("查看明细和异议"));
  await screen.findByText(/核对本次费用/);
  expect(screen.getByText(/原票1/)).toBeInTheDocument();
  expect(screen.getByText(/当时未付 0.60 元/)).toBeInTheDocument();
  expect(screen.getByText(/可付日 2026-02-16 → 2026-02-25/)).toBeInTheDocument();
  expect(urls.some(url=>url.endsWith("/rules"))).toBe(false);
});

// 第十一批 TK-04-b（ENUM L84「各自全额挂账」；BSM L823「只有转为可用才产生商户积分发行费」）：商户积分发行费单独出账，明细取来源消费单据。
it("商户积分发行费账单标明费用类别，明细显示来源消费单据",async()=>{
  vi.stubGlobal("fetch",vi.fn(async(input:RequestInfo|URL)=>{
    const url=String(input);let data:unknown=[];
    if(url.endsWith("/bills"))data=[{id:"bill2",billing_month:"2026-10-01",seller_entity_id:"一号酒吧合同主体",receiver_entity_id:"积分中心",project_id:"p1",amount_cent:60,paid_amount_cent:0,unpaid_amount_cent:60,visible_at:"2026-11-01T08:00:00Z",payment_status:"awaiting_objection_period",open_objections:0,fee_kind:"merchant"}];
    if(url.endsWith("/items"))data=[{reward_id:"mcr1",parent_ticket_id:null,slot_index:0,use_index:0,points:30,unit_price_cent:2,amount_cent:60,confirmed_at:"2026-10-21T14:00:00Z",source_type:"bar_bill",source_id:"bill-001"}];
    return new Response(JSON.stringify({success:true,code:"OK",data}),{headers:{"Content-Type":"application/json"}});
  }));
  const props:AdminPageProps={api:new AdminApi("test"),user:{id:"u",username:"merchant",accountType:"merchant"},setActionResult:vi.fn(),setGlobalError:vi.fn(),effectivePermissions:{status:"ready",value:{grants:["ticket.points.billing.view"],denies:[],wildcard:false}}};
  render(<TicketPointsBillingPage {...props}/>);
  await waitFor(()=>expect(screen.getByText(/一号酒吧合同主体/)).toHaveTextContent("商户积分发行费"));
  fireEvent.click(screen.getByText("查看明细和异议"));
  await waitFor(()=>expect(screen.getByText(/消费单据 bill-001/)).toBeInTheDocument());
});
