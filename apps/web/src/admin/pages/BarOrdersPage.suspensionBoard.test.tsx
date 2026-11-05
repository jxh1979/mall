// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 T 段第 157 条 157-R7：BSM:1113「不设额度时，风险看板和审计导出必须持续展示挂起金额、账龄、责任人和未结清数量。」；
// BSM:1840「看板按权限看，但把数据导出带走须单独授权并留痕」——订单页挂起订单风险看板经真实后台客户端读蛇形响应，显示未结清汇总、
// 中文状态、账龄、责任人与门店损耗；导出按钮只在持审计导出码时可点，下载服务端带密码 PDF。
const BOARD = {
  summary: { unsettled_count: 2, unsettled_amount_cent: 90000, oldest_unsettled_age_minutes: 95 },
  orders: [
    { order_id: "ord-1", bill_no: "B-001", bar_id: "bar-a", bar_name: "看板酒吧甲", table_id: "t-1", table_name: "包厢 1", status: "hanging_unpaid",
      amount_cent: 60000, unpaid_cent: 60000, suspended_at: "2026-11-05T10:00:00Z", age_minutes: 95,
      responsible_users: [{ user_id: "usr-w", username: "waiter01" }, { user_id: "usr-x", username: null }, { user_id: null, username: null }],
      suspended_by: "cashier01" },
    { order_id: "ord-2", bill_no: "B-002", bar_id: "bar-a", bar_name: "看板酒吧甲", table_id: "t-2", table_name: "包厢 2", status: "bad_debt_processing",
      amount_cent: 30000, unpaid_cent: 30000, suspended_at: "2026-11-05T11:00:00Z", age_minutes: 40, responsible_users: [{ user_id: null, username: null }],
      suspended_by: "cashier01" },
    { order_id: "ord-3", bill_no: "B-003", bar_id: "bar-a", bar_name: "看板酒吧甲", table_id: "t-3", table_name: "包厢 3", status: "hanging_unpaid",
      amount_cent: 10000, unpaid_cent: 10000, suspended_at: "2026-11-05T11:30:00Z", age_minutes: 10, responsible_users: [],
      suspended_by: "cashier01" },
  ],
  page: 1, size: 100, total: 2,
};

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(grants: string[]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/api/v1/bar/suspended-orders/export")) {
      return Promise.resolve(new Response("%PDF-1.7", { status: 200, headers: { "content-type": "application/pdf",
        "content-disposition": "attachment; filename*=UTF-8''%E6%8C%82%E8%B5%B7.pdf" } }));
    }
    if (url.includes("/api/v1/bar/suspended-orders")) return Promise.resolve(ok(BOARD));
    return Promise.resolve(ok({ items: [], page: 1, page_size: 50, total: 0 }));
  }));
  const permissions: EffectivePermissionsState = { status: "ready", value: { grants, denies: [], wildcard: false } };
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "manager", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: permissions,
  };
  render(<BarOrdersPage {...props} />);
  return props;
}

function calls(fragment: string) {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input]) => String(input).includes(fragment));
}

beforeEach(() => {
  Object.assign(URL, { createObjectURL: vi.fn(() => "blob:board"), revokeObjectURL: vi.fn() });
});
afterEach(() => vi.unstubAllGlobals());

describe("BarOrdersPage 挂起订单风险看板", () => {
  it("显示未结清数量、金额与最长账龄，各行中文状态、账龄、责任人，责任人为空显示门店损耗；无导出授权时导出按钮不可点", async () => {
    renderPage(["bar.order.view"]);
    await waitFor(() => expect(screen.queryByTestId("bar-suspended-kpi-count")?.textContent).toBe("2"));
    expect(screen.getByTestId("bar-suspended-kpi-amount").textContent).toBe("¥900.00");
    expect(screen.getByTestId("bar-suspended-kpi-age").textContent).toBe("1 小时 35 分");
    const first = screen.getByTestId("bar-suspended-row-ord-1").textContent ?? "";
    expect(first).toContain("已挂起待付款");
    expect(first).toContain("1 小时 35 分");
    expect(first).toContain("waiter01");
    expect(first).toContain("包厢 1");
    const second = screen.getByTestId("bar-suspended-row-ord-2").textContent ?? "";
    expect(second).toContain("坏账损耗处理中");
    expect(second).toContain("门店损耗");
    expect(second).toContain("40 分");
    expect((screen.getByTestId("bar-suspended-export") as HTMLButtonElement).disabled).toBe(true);
  });

  // 18T 复核修复④（BSM:1110「服务员责任以商品出品时该桌绑定的主责服务员为准……出品时没有主责服务员的记为门店损耗」）：后端按本单未作废已出品明细逐项
  // 取出品时刻的主责服务员；多人全列（有账号显示账号，否则显示编号），取不到的列「门店损耗」，尚无已出品明细的留空。
  it("责任人按出品时主责服务员全列：账号优先、无账号显示编号、取不到的显示门店损耗，尚无已出品明细的留空", async () => {
    renderPage(["bar.order.view"]);
    await waitFor(() => expect(screen.queryByTestId("bar-suspended-responsible-ord-1")?.textContent).toBe("waiter01、usr-x、门店损耗"));
    expect(screen.queryByTestId("bar-suspended-responsible-ord-2")?.textContent).toBe("门店损耗");
    expect(screen.queryByTestId("bar-suspended-responsible-ord-3")?.textContent).toBe("");
  });

  it("持审计导出授权时可导出：下载服务端 PDF，结果区提示到「我的导出」查密码", async () => {
    const props = renderPage(["bar.order.view", "audit.operation_log.export"]);
    await waitFor(() => expect(screen.queryByTestId("bar-suspended-row-ord-1")).not.toBeNull());
    await waitFor(() => expect((screen.getByTestId("bar-suspended-export") as HTMLButtonElement).disabled).toBe(false));

    await userEvent.click(screen.getByTestId("bar-suspended-export"));
    await waitFor(() => expect(calls("/api/v1/bar/suspended-orders/export")).toHaveLength(1));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({
      action: "bar_suspended_orders_export", status: "exported", message: expect.stringContaining("我的导出") })));
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
  });

  it("没有查看订单权限时不显示看板、不读取", async () => {
    renderPage([]);
    await waitFor(() => expect(screen.queryByTestId("bar-orders-page")).not.toBeNull());
    expect(screen.queryByTestId("bar-suspended-orders-board")).toBeNull();
    expect(calls("/api/v1/bar/suspended-orders")).toHaveLength(0);
  });
});
