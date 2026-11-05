// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 T 段第 157 条 157-R3：BSM:1109「经理确认最终无法收款后转为坏账损耗处理中并解除该订单对交班、日结的阻断」；BSM:1110「出品时没有主责服务员的
// 记为门店损耗」；BSM:1111「财务确认未收售价、库存成本和赔偿记录均完成后，订单转为已核销结案」——已挂起待付款的单由经理填原因转坏账，坏账单显示固化的
// 未收售价、库存成本与赔偿登记，财务确认核销结案；经真实后台客户端提交，请求体为蛇形键。
const HUNG = { id: "ord-hung", table_id: "T-VIP", table_type: "vip", total_amount_cent: 60000, status: "billed", created_at: "2026-11-05T10:00:00Z",
  bills: [{ id: "bill-hung", status: "unpaid" }], suspension_status: "hanging_unpaid", suspension_amount_cent: 60000,
  suspension: { id: "bos_hung", status: "hanging_unpaid" } };
const BAD = { id: "ord-bad", table_id: "T-VIP2", table_type: "vip", total_amount_cent: 60000, status: "billed", created_at: "2026-11-05T10:01:00Z",
  bills: [{ id: "bill-bad", status: "unpaid" }], suspension_status: "bad_debt_processing", suspension_amount_cent: 60000,
  items: [{ id: "it-1", product_name: "干红", quantity: 1, amount_cent: 40000, delivery_status: "delivered" },
    { id: "it-2", product_name: "精酿啤酒", quantity: 1, amount_cent: 20000, delivery_status: "delivered" }],
  suspension: { id: "bos_bad", status: "bad_debt_processing", bad_debt_reason: "客人失联", bad_debt_confirmed_at: "2026-11-05T12:00:00Z",
    unpaid_price_cent: 60000, inventory_cost_cent: 16000,
    compensation_lines: [{ order_item_id: "it-1", responsible_user_id: "usr_waiter_a", cost_cent: 12000 },
      { order_item_id: "it-2", responsible_user_id: null, cost_cent: 4000 }] } };
const CLOSED = { ...BAD, id: "ord-closed", table_id: "T-VIP3", suspension_status: "written_off_closed",
  suspension: { ...BAD.suspension, id: "bos_closed", status: "written_off_closed", written_off_at: "2026-11-05T13:00:00Z" } };
// 157-R4：已提交高价值复核、尚未批准的挂起单。
const REVIEWING = { ...HUNG, id: "ord-review", table_id: "T-VIP4",
  suspension: { id: "bos_review", status: "hanging_unpaid", bad_debt_reason: "客人失联", bad_debt_review_id: "hvr_review_1" } };
// 157-R5 ⑥（BSM:1109「经理未及时确认时，任务升级给店长或夜班值班经理」）：关台未付清待办已升级的挂起单（订单列表带 unpaid_close_escalated_at）。
const ESCALATED = { ...HUNG, id: "ord-escalated", table_id: "T-VIP5", unpaid_close_escalated_at: "2026-11-06T10:00:00Z",
  suspension: { id: "bos_escalated", status: "hanging_unpaid" } };
const ORDERS = [HUNG, BAD, CLOSED, REVIEWING, ESCALATED];

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(grants: string[]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/api/v1/bar/orders") && (init?.method ?? "GET") === "GET") {
      return Promise.resolve(ok({ items: ORDERS, page: 1, page_size: 50, total: ORDERS.length }));
    }
    if (url.includes("/api/v1/evidence/attachments/upload")) {
      const file = (init?.body as FormData).get("file") as File;
      return Promise.resolve(ok({ id: file.type.startsWith("video") ? "att-video" : "att-photo", file_name: file.name }));
    }
    if (url.includes("/ord-escalated/suspension/bad-debt")) {
      return Promise.resolve(new Response(JSON.stringify({ success: false, code: "FORBIDDEN", detail_code: "PERMISSION_DENIED",
        message: "没有操作权限：bar.bill.hang_cancel" }), { status: 403, headers: { "content-type": "application/json" } }));
    }
    if (url.includes("/suspension/")) return Promise.resolve(ok({ id: "bos_x", status: "bad_debt_processing" }));
    return Promise.resolve(ok({ items: [], page: 1, page_size: 50, total: 0 }));
  }));
  const permissions: EffectivePermissionsState = { status: "ready", value: { grants, denies: [], wildcard: false } };
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "manager", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: permissions,
  };
  render(<BarOrdersPage {...props} />);
}

function calls(path: string) {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input]) => String(input).includes(path));
}

async function select(orderId: string) {
  await userEvent.click(screen.getByTestId(`bar-order-select-${orderId}`));
  await waitFor(() => expect((screen.getByTestId(`bar-order-select-${orderId}`) as HTMLInputElement).checked).toBe(true));
}

afterEach(() => vi.unstubAllGlobals());

describe("BarOrdersPage 挂起订单转坏账与核销结案", () => {
  it("已挂起待付款的单显示转坏账：原因为空不提交，填写原因后提交 reason、idempotency_key", async () => {
    renderPage(["bar.order.view", "bar.bill.hang_cancel"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-hung")?.textContent ?? "").toContain("已挂起待付款"));
    await select("ord-hung");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt")).not.toBeNull());
    expect(screen.queryByTestId("bar-order-write-off")).toBeNull();

    await userEvent.click(screen.getByTestId("bar-order-bad-debt"));
    await waitFor(() => expect(screen.getByTestId("bar-orders-error").textContent).toContain("原因"));
    expect(calls("/suspension/bad-debt")).toHaveLength(0);

    await userEvent.type(screen.getByTestId("bar-order-bad-debt-reason"), "客人失联，确认无法收回");
    await userEvent.click(screen.getByTestId("bar-order-bad-debt"));
    await waitFor(() => expect(calls("/suspension/bad-debt")).toHaveLength(1));
    const [input, init] = calls("/suspension/bad-debt")[0]!;
    expect(String(input)).toContain("/api/v1/bar/orders/ord-hung/suspension/bad-debt");
    const body = JSON.parse(String((init as RequestInit).body)) as Record<string, unknown>;
    expect(Object.keys(body).sort()).toEqual(["idempotency_key", "reason"]);
    expect(body.reason).toBe("客人失联，确认无法收回");
  });

  it("坏账单显示未收售价、库存成本与赔偿登记（责任人为空记门店损耗），财务确认核销结案提交 idempotency_key", async () => {
    renderPage(["bar.order.view", "finance.ar_ap.manage"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-bad")?.textContent ?? "").toContain("坏账损耗处理中"));
    await select("ord-bad");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt-detail")).not.toBeNull());
    const detail = screen.getByTestId("bar-order-bad-debt-detail").textContent ?? "";
    expect(detail).toContain("600.00");
    expect(detail).toContain("160.00");
    expect(detail).toContain("干红");
    expect(detail).toContain("usr_waiter_a");
    expect(detail).toContain("120.00");
    expect(detail).toContain("门店损耗");
    expect(screen.queryByTestId("bar-order-bad-debt")).toBeNull();

    await userEvent.click(screen.getByTestId("bar-order-write-off"));
    await waitFor(() => expect(calls("/suspension/write-off")).toHaveLength(1));
    const [input, init] = calls("/suspension/write-off")[0]!;
    expect(String(input)).toContain("/api/v1/bar/orders/ord-bad/suspension/write-off");
    expect(Object.keys(JSON.parse(String((init as RequestInit).body)) as Record<string, unknown>)).toEqual(["idempotency_key"]);
  });

  // 157-R4：BSM:1112「达到既有高价值标准的商品仍执行原证据和审批要求。」——本单含高价值已出品商品时须带照片与视频，提交后转主管复核。
  it("转坏账可上传现场照片与视频：上传按 business_type=bar_order_bad_debt，提交体带 photo_attachment_ids、video_attachment_ids", async () => {
    renderPage(["bar.order.view", "bar.bill.hang_cancel", "evidence.attachment.create"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-hung")?.textContent ?? "").toContain("已挂起待付款"));
    await select("ord-hung");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt-evidence-upload")).not.toBeNull());

    await userEvent.upload(screen.getByTestId("bar-order-bad-debt-evidence-upload"),
      [new File(["p"], "现场照片.jpg", { type: "image/jpeg" }), new File(["v"], "现场视频.mp4", { type: "video/mp4" })]);
    await waitFor(() => expect(screen.getByTestId("bar-order-bad-debt-evidence-list").textContent).toContain("现场视频.mp4"));
    const uploads = calls("/api/v1/evidence/attachments/upload");
    expect(uploads).toHaveLength(2);
    expect(((uploads[0]![1] as RequestInit).body as FormData).get("business_type")).toBe("bar_order_bad_debt");

    await userEvent.type(screen.getByTestId("bar-order-bad-debt-reason"), "客人失联，确认无法收回");
    await userEvent.click(screen.getByTestId("bar-order-bad-debt"));
    await waitFor(() => expect(calls("/suspension/bad-debt")).toHaveLength(1));
    const body = JSON.parse(String((calls("/suspension/bad-debt")[0]![1] as RequestInit).body)) as Record<string, unknown>;
    expect(body.photo_attachment_ids).toEqual(["att-photo"]);
    expect(body.video_attachment_ids).toEqual(["att-video"]);
    expect(body.reason).toBe("客人失联，确认无法收回");
  });

  it("已提交高价值复核的单显示复核单号，不再显示转坏账按钮", async () => {
    renderPage(["bar.order.view", "bar.bill.hang_cancel"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-review")?.textContent ?? "").toContain("已挂起待付款"));
    await select("ord-review");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt-review-pending")?.textContent ?? "").toContain("hvr_review_1"));
    expect(screen.queryByTestId("bar-order-bad-debt")).toBeNull();
  });

  // 157-R5 ⑥：被升级的店长只持 bar.bill.hang_escalation 也能办；已升级的单对能进本页的其他用户也可点，由后端按夜班值班经理排班鉴权，403 显示后端中文原因。
  // 18T 复核修复③（BSM:1109「经理未及时确认时，任务升级给店长或夜班值班经理」）：与后端一致——已升级的单不要求权限码，未升级的只认 bar.bill.hang_cancel。
  it("只持 bar.bill.hang_escalation 的店长：关台未付清待办已升级的单转坏账按钮可用", async () => {
    renderPage(["bar.order.view", "bar.bill.hang_escalation"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-escalated")?.textContent ?? "").toContain("已挂起待付款"));
    await select("ord-escalated");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt")).not.toBeNull());
    expect((screen.getByTestId("bar-order-bad-debt") as HTMLButtonElement).disabled).toBe(false);
  });

  it("只持 bar.bill.hang_escalation 的店长：待办未升级的单转坏账按钮不可用", async () => {
    renderPage(["bar.order.view", "bar.bill.hang_escalation"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-hung")?.textContent ?? "").toContain("已挂起待付款"));
    await select("ord-hung");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt")).not.toBeNull());
    expect((screen.getByTestId("bar-order-bad-debt") as HTMLButtonElement).disabled).toBe(true);
  });

  // 18T 复核修复⑤（BSM:1111「财务确认未收售价、库存成本和赔偿记录均完成后，订单转为已核销结案。」）：项目侧财务账号（无商户号；后端按 bar.order.view
  // 可触及的项目列订单）打开订单页即列出本项目坏账订单，选中后财务确认核销结案，办成后重新读取列表显示已核销结案。
  it("项目侧财务账号（无商户号）列出本项目坏账订单，核销结案后重新读取显示已核销结案", async () => {
    let closed = false;
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/api/v1/bar/orders") && (init?.method ?? "GET") === "GET") {
        return Promise.resolve(ok({ items: [closed ? { ...CLOSED, id: "ord-bad" } : BAD], page: 1, page_size: 50, total: 1 }));
      }
      if (url.includes("/ord-bad/suspension/write-off")) {
        closed = true;
        return Promise.resolve(ok({ id: "bos_bad", status: "written_off_closed" }));
      }
      return Promise.resolve(ok({ items: [], page: 1, page_size: 50, total: 0 }));
    }));
    const permissions: EffectivePermissionsState = { status: "ready", value: { grants: ["bar.order.view", "finance.ar_ap.manage"], denies: [], wildcard: false } };
    render(<BarOrdersPage api={new AdminApi("test-token")} user={{ id: "usr-fin", username: "project-finance", accountType: "staff" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={permissions} />);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-bad")?.textContent ?? "").toContain("坏账损耗处理中"));
    await select("ord-bad");
    await waitFor(() => expect(screen.queryByTestId("bar-order-write-off")).not.toBeNull());

    await userEvent.click(screen.getByTestId("bar-order-write-off"));
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-bad")?.textContent ?? "").toContain("已核销结案"));
    expect(calls("/ord-bad/suspension/write-off")).toHaveLength(1);
    expect(screen.queryByTestId("bar-order-write-off")).toBeNull();
  });

  it("关台未付清待办已升级的单，无经理码的用户也可提交，后端 403 时显示后端中文原因", async () => {
    renderPage(["bar.order.view"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-escalated")?.textContent ?? "").toContain("已挂起待付款"));
    await select("ord-escalated");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt")).not.toBeNull());
    expect((screen.getByTestId("bar-order-bad-debt") as HTMLButtonElement).disabled).toBe(false);
    await userEvent.type(screen.getByTestId("bar-order-bad-debt-reason"), "客人失联，确认无法收回");
    await userEvent.click(screen.getByTestId("bar-order-bad-debt"));
    await waitFor(() => expect(screen.getByTestId("bar-orders-error").textContent).toContain("没有操作权限：bar.bill.hang_cancel"));
    expect(calls("/ord-escalated/suspension/bad-debt")).toHaveLength(1);
  });

  it("已核销结案的单只显示登记、不再有核销按钮；没有经理权限的转坏账按钮不可用", async () => {
    renderPage(["bar.order.view"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-closed")?.textContent ?? "").toContain("已核销结案"));
    await select("ord-closed");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt-detail")).not.toBeNull());
    expect(screen.queryByTestId("bar-order-write-off")).toBeNull();
    await select("ord-hung");
    await waitFor(() => expect(screen.queryByTestId("bar-order-bad-debt")).not.toBeNull());
    expect((screen.getByTestId("bar-order-bad-debt") as HTMLButtonElement).disabled).toBe(true);
  });
});
