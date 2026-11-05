// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 T 段第 157 条 157-R6：BSM:1107「已出品挂起订单取消须经理审批。能回库的商品继续执行既有退菜、退酒和高价值退品证据审批链……经理审批不能替代
// 既有高价值审批链。」；BSM:1108「取消审批生效与付款成功只能有一项先完成，先完成者生效」——已挂起待付款的单由收银员勾选可回库的已出品明细发起取消，
// 取消审批中的单由经理批准或驳回；经真实后台客户端提交，请求体为蛇形键。
const ITEMS = [{ id: "it-1", product_name: "干红", quantity: 1, amount_cent: 30000, delivery_status: "delivered" },
  { id: "it-2", product_name: "精酿啤酒", quantity: 1, amount_cent: 5000, delivery_status: "delivered" },
  { id: "it-3", product_name: "果盘", quantity: 1, amount_cent: 3000, delivery_status: "pending" }];
const HUNG = { id: "ord-hung", table_id: "T-VIP", table_type: "vip", total_amount_cent: 38000, status: "billed", created_at: "2026-11-05T10:00:00Z",
  bills: [{ id: "bill-hung", status: "unpaid" }], suspension_status: "hanging_unpaid", suspension_amount_cent: 38000, items: ITEMS,
  suspension: { id: "bos_hung", status: "hanging_unpaid" } };
const PENDING = { ...HUNG, id: "ord-pending", table_id: "T-VIP2", suspension_status: "cancel_pending_approval",
  suspension: { id: "bos_pending", status: "cancel_pending_approval", cancel_item_ids: ["it-1"], cancel_reason: "客人整瓶未开，退回",
    cancel_requested_by: "usr_cashier" } };
const WAITING = { ...HUNG, id: "ord-waiting", table_id: "T-VIP3", suspension_status: "cancel_pending_approval",
  suspension: { id: "bos_waiting", status: "cancel_pending_approval", cancel_item_ids: ["it-1"], cancel_reason: "客人整瓶未开，退回",
    cancel_requested_by: "usr_cashier", cancel_decided_by: "usr_manager", cancel_decided_at: "2026-11-05T12:00:00Z" } };
const ORDERS = [HUNG, PENDING, WAITING];

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(grants: string[]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/api/v1/bar/orders") && (init?.method ?? "GET") === "GET") {
      return Promise.resolve(ok({ items: ORDERS, page: 1, page_size: 50, total: ORDERS.length }));
    }
    if (url.endsWith("/api/v1/bar/void-reason-codes")) {
      return Promise.resolve(ok([{ code: "customer_change", label_zh: "客人改单", sort_order: 30, status: "active" }]));
    }
    if (url.includes("/api/v1/evidence/attachments/upload")) {
      const file = (init?.body as FormData).get("file") as File;
      return Promise.resolve(ok({ id: file.type.startsWith("video") ? "att-video" : "att-photo", file_name: file.name }));
    }
    if (url.includes("/suspension/")) return Promise.resolve(ok({ id: "bos_x", status: "cancel_pending_approval" }));
    return Promise.resolve(ok({ items: [], page: 1, page_size: 50, total: 0 }));
  }));
  const permissions: EffectivePermissionsState = { status: "ready", value: { grants, denies: [], wildcard: false } };
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "cashier", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: permissions,
  };
  render(<BarOrdersPage {...props} />);
}

function calls(path: string) {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input]) => String(input).includes(path));
}

function body(path: string, index = 0) {
  return JSON.parse(String((calls(path)[index]![1] as RequestInit).body)) as Record<string, unknown>;
}

async function select(orderId: string) {
  await waitFor(() => expect(screen.queryByTestId(`bar-order-suspension-${orderId}`)).not.toBeNull());
  await waitFor(() => expect(calls("/api/v1/bar/void-reason-codes").length).toBeGreaterThan(0));
  await userEvent.click(screen.getByTestId(`bar-order-select-${orderId}`));
  await waitFor(() => expect((screen.getByTestId(`bar-order-select-${orderId}`) as HTMLInputElement).checked).toBe(true));
}

afterEach(() => vi.unstubAllGlobals());

describe("BarOrdersPage 挂起订单经理审批取消", () => {
  it("已挂起待付款的单可勾选已出品明细发起取消：未勾选、原因为空不提交，提交 order_item_ids、reason、reason_code、evidence_attachment_ids", async () => {
    renderPage(["bar.order.view", "bar.bill.hang", "evidence.attachment.create"]);
    await select("ord-hung");
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-cancel-form")).not.toBeNull());
    expect(screen.queryByTestId("bar-order-suspension-cancel-item-it-1")).not.toBeNull();
    expect(screen.queryByTestId("bar-order-suspension-cancel-item-it-2")).not.toBeNull();
    expect(screen.queryByTestId("bar-order-suspension-cancel-item-it-3")).toBeNull();

    await userEvent.click(screen.getByTestId("bar-order-suspension-cancel"));
    await waitFor(() => expect(screen.getByTestId("bar-orders-error").textContent).toContain("勾选"));
    await userEvent.click(screen.getByTestId("bar-order-suspension-cancel-item-it-1"));
    await userEvent.click(screen.getByTestId("bar-order-suspension-cancel"));
    await waitFor(() => expect(screen.getByTestId("bar-orders-error").textContent).toContain("原因"));
    expect(calls("/suspension/cancel")).toHaveLength(0);

    await userEvent.type(screen.getByTestId("bar-order-suspension-cancel-reason"), "客人整瓶未开，退回");
    await userEvent.selectOptions(screen.getByTestId("bar-order-suspension-cancel-reason-code"), "customer_change");
    await userEvent.upload(screen.getByTestId("bar-order-suspension-cancel-evidence-upload"),
      [new File(["p"], "现场照片.jpg", { type: "image/jpeg" }), new File(["v"], "现场视频.mp4", { type: "video/mp4" })]);
    await waitFor(() => expect(screen.getByTestId("bar-order-suspension-cancel-evidence-list").textContent).toContain("现场视频.mp4"));
    await userEvent.click(screen.getByTestId("bar-order-suspension-cancel"));
    await waitFor(() => expect(calls("/suspension/cancel")).toHaveLength(1));
    expect(String(calls("/suspension/cancel")[0]![0])).toContain("/api/v1/bar/orders/ord-hung/suspension/cancel");
    const sent = body("/suspension/cancel");
    expect(Object.keys(sent).sort()).toEqual(["evidence_attachment_ids", "idempotency_key", "order_item_ids", "reason", "reason_code"]);
    expect(sent.order_item_ids).toEqual(["it-1"]);
    expect(sent.reason).toBe("客人整瓶未开，退回");
    expect(sent.reason_code).toBe("customer_change");
    expect(sent.evidence_attachment_ids).toEqual(["att-photo", "att-video"]);
  });

  it("取消审批中的单显示所列明细与原因，经理批准提交 decision=approve、驳回提交 decision=reject", async () => {
    renderPage(["bar.order.view", "bar.bill.hang_cancel"]);
    await select("ord-pending");
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-cancel-decision-form")).not.toBeNull());
    const form = screen.getByTestId("bar-order-suspension-cancel-decision-form").textContent ?? "";
    expect(form).toContain("干红");
    expect(form).toContain("客人整瓶未开，退回");
    expect(screen.queryByTestId("bar-order-suspension-cancel-form")).toBeNull();

    await userEvent.type(screen.getByTestId("bar-order-suspension-cancel-decision-reason"), "核对过现场");
    await userEvent.click(screen.getByTestId("bar-order-suspension-cancel-approve"));
    await waitFor(() => expect(calls("/suspension/cancel-decision")).toHaveLength(1));
    expect(String(calls("/suspension/cancel-decision")[0]![0])).toContain("/api/v1/bar/orders/ord-pending/suspension/cancel-decision");
    expect(body("/suspension/cancel-decision")).toEqual(expect.objectContaining({ decision: "approve", reason: "核对过现场" }));
    expect(Object.keys(body("/suspension/cancel-decision")).sort()).toEqual(["decision", "idempotency_key", "reason"]);

    await waitFor(() => expect((screen.getByTestId("bar-order-suspension-cancel-reject") as HTMLButtonElement).disabled).toBe(false));
    await userEvent.click(screen.getByTestId("bar-order-suspension-cancel-reject"));
    await waitFor(() => expect(calls("/suspension/cancel-decision")).toHaveLength(2));
    expect(body("/suspension/cancel-decision", 1).decision).toBe("reject");
  });

  it("经理已批准、等高价值退品审批的单只显示等待提示；没有经理权限的审批按钮不可用", async () => {
    renderPage(["bar.order.view"]);
    await select("ord-waiting");
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-cancel-waiting")).not.toBeNull());
    expect(screen.queryByTestId("bar-order-suspension-cancel-approve")).toBeNull();
    await select("ord-pending");
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-cancel-approve")).not.toBeNull());
    expect((screen.getByTestId("bar-order-suspension-cancel-approve") as HTMLButtonElement).disabled).toBe(true);
  });
});
