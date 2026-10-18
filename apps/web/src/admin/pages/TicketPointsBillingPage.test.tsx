// 文件版本：V0.01
import { fireEvent, render, screen } from "@testing-library/react";
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
