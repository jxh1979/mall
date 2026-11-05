// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 T 段第 157 条 157-R2：第十八批裁决 37「散台、卡座下单必须当场付款、不能挂起」（只有包厢能挂起）；BSM:1106「收银员可以直接挂起，
// 不设单笔金额和当班累计金额上限；必须填写原因并记录金额、桌台、责任人、操作时间和完整日志。」——包厢有未结清账单的订单才显示「挂起」，
// 原因必填；经真实后台客户端提交，请求体为蛇形 reason、idempotency_key；列表显示挂起状态中文与挂起金额。
const ORDERS = [
  { id: "ord-vip", table_id: "T-VIP", table_type: "vip", total_amount_cent: 60000, status: "billed", created_at: "2026-11-05T10:00:00Z",
    bills: [{ id: "bill-vip", status: "unpaid" }] },
  { id: "ord-card", table_id: "T-CARD", table_type: "card_seat", total_amount_cent: 30000, status: "billed", created_at: "2026-11-05T10:01:00Z",
    bills: [{ id: "bill-card", status: "unpaid" }] },
  { id: "ord-hung", table_id: "T-VIP2", table_type: "vip", total_amount_cent: 50000, status: "billed", created_at: "2026-11-05T10:02:00Z",
    bills: [{ id: "bill-hung", status: "unpaid" }], suspension_status: "hanging_unpaid", suspension_amount_cent: 50000 },
];

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(grants: string[]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/api/v1/bar/orders") && (init?.method ?? "GET") === "GET") {
      return Promise.resolve(ok({ items: ORDERS, page: 1, page_size: 50, total: ORDERS.length }));
    }
    if (url.endsWith("/suspend")) return Promise.resolve(ok({ id: "bos_1", status: "hanging_unpaid" }));
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

function suspendCalls() {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input]) => String(input).endsWith("/suspend"));
}

afterEach(() => vi.unstubAllGlobals());

describe("BarOrdersPage 包厢订单挂起", () => {
  it("包厢有未结清账单才显示挂起，卡座不显示；已挂起的单列表显示挂起状态与金额", async () => {
    renderPage(["bar.order.view", "bar.bill.hang"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-hung")?.textContent ?? "").toContain("已挂起待付款"));
    expect(screen.getByTestId("bar-order-suspension-ord-hung").textContent).toContain("500.00");
    await userEvent.click(screen.getByTestId("bar-order-select-ord-card"));
    await waitFor(() => expect((screen.getByTestId("bar-order-select-ord-card") as HTMLInputElement).checked).toBe(true));
    expect(screen.queryByTestId("bar-order-suspend")).toBeNull();
    await userEvent.click(screen.getByTestId("bar-order-select-ord-hung"));
    await waitFor(() => expect((screen.getByTestId("bar-order-select-ord-hung") as HTMLInputElement).checked).toBe(true));
    expect(screen.queryByTestId("bar-order-suspend")).toBeNull();
    await userEvent.click(screen.getByTestId("bar-order-select-ord-vip"));
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspend")).not.toBeNull());
  });

  it("原因为空不提交；填写原因后经后台客户端提交，请求体为 reason、idempotency_key", async () => {
    renderPage(["bar.order.view", "bar.bill.hang"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-vip")?.textContent ?? "").toContain("—"));
    await userEvent.click(screen.getByTestId("bar-order-select-ord-vip"));
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspend")).not.toBeNull());

    await userEvent.click(screen.getByTestId("bar-order-suspend"));
    await waitFor(() => expect(screen.getByTestId("bar-orders-error").textContent).toContain("原因"));
    expect(suspendCalls()).toHaveLength(0);

    await userEvent.type(screen.getByTestId("bar-order-suspend-reason"), "客人稍后付款");
    await userEvent.click(screen.getByTestId("bar-order-suspend"));
    await waitFor(() => expect(suspendCalls()).toHaveLength(1));
    const [input, init] = suspendCalls()[0]!;
    expect(String(input)).toContain("/api/v1/bar/orders/ord-vip/suspend");
    const body = JSON.parse(String((init as RequestInit).body)) as Record<string, unknown>;
    expect(Object.keys(body).sort()).toEqual(["idempotency_key", "reason"]);
    expect(body.reason).toBe("客人稍后付款");
  });

  it("没有挂起权限的员工看到挂起按钮不可用", async () => {
    renderPage(["bar.order.view"]);
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspension-ord-vip")?.textContent ?? "").toContain("—"));
    await userEvent.click(screen.getByTestId("bar-order-select-ord-vip"));
    await waitFor(() => expect(screen.queryByTestId("bar-order-suspend")).not.toBeNull());
    expect((screen.getByTestId("bar-order-suspend") as HTMLButtonElement).disabled).toBe(true);
  });
});
