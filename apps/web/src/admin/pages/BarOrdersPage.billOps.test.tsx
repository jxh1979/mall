// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import { formatActionResult } from "../components/ActionResultView";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AU 段第 282 条 282-R2（BSM:1077 小费「结账可录（按额或按比例）」；BSM:1079「按金额拆出子账单供客人分别付款，也可将同一酒吧内另一张未支付、
// 未结算账单合入主账单」）与第 283 条 283-R2（BSM:1064「不足最低消费的要补足」；BSM:1079「各结果台显示差额……结账补足」）：后台订单页按所选账单办理
// 结账、小费、拆单、合单与补足低消差额；经真实后台客户端提交，请求体为蛇形键。
const bill = (id: string, extra: Record<string, unknown> = {}) => ({ id, bill_no: `BN-${id}`, status: "unpaid", payable_amount_cent: 50000,
  paid_amount_cent: 0, tip_amount_cent: 0, min_spend_gap_cent: 0, ...extra });
const order = (id: string, barId: string, bills: unknown[]) => ({ id, bar_id: barId, table_id: `T-${id}`, status: "billed",
  total_amount_cent: 80000, created_at: "2026-11-18T10:00:00Z", bills });

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(grants: string[], options: { gap?: number; merged?: boolean; secondaryMerged?: boolean; b1?: Record<string, unknown>; topUpBillId?: string } = {}) {
  const orders = [
    order("ord-1", "bar-a", [bill("b1", { min_spend_gap_cent: options.gap ?? 0, ...options.b1 }), bill("b2", { payable_amount_cent: 30000 })]),
    order("ord-2", "bar-a", [bill("b3", options.secondaryMerged ? { status: "merged", payable_amount_cent: 0 } : { payable_amount_cent: 10000 })]),
    order("ord-3", "bar-a", [bill("b4", { status: "paid", paid_amount_cent: 50000 })]),
    order("ord-4", "bar-b", [bill("b5")]),
  ];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.endsWith("/api/v1/bar/orders") && (init?.method ?? "GET") === "GET") {
      return Promise.resolve(ok({ items: orders, page: 1, page_size: 50, total: orders.length }));
    }
    if (url.endsWith("/api/v1/bar/bills/b1/operations")) {
      const rows = options.merged ? [{ id: "op-1", operation_type: "merge", primary_bill_id: "b1", secondary_bill_id: "b3", amount_cent: 10000 }] : [];
      return Promise.resolve(ok({ items: rows, page: 1, page_size: 50, total: rows.length }));
    }
    // 后端各动作的返回形状：合单回账单操作记录、小费回账单、补足低消回账单调整记录；合单与补足低消照 BarBillRecords 带服务端快照 snapshot_json。
    if ((init?.method ?? "GET") === "POST" && url.endsWith("/merge")) {
      return Promise.resolve(ok({ id: "op-9", operation_type: "merge", primary_bill_id: "b1", secondary_bill_id: "b3", amount_cent: 10000, status: "applied",
        snapshot_json: '{"primary_bill_id":"b1","related_bill_id":"b3","primary_payable_before":50000,"primary_payable_after":60000,"related_payable":10000}' }));
    }
    if ((init?.method ?? "GET") === "POST" && url.endsWith("/tip")) {
      return Promise.resolve(ok({ id: "b1", bill_no: "BN-b1", currency: "CNY", payment_status: "unpaid", settlement_status: "unsettled",
        sales_role_type: "non_sales", tip_amount_cent: 5000 }));
    }
    if ((init?.method ?? "GET") === "POST" && url.endsWith("/adjustments")) {
      return Promise.resolve(ok({ id: "adj-1", bill_id: options.topUpBillId ?? "b1", adjustment_type: "min_spend_top_up", amount_cent: 30000, status: "applied",
        snapshot_json: '{"bill_id":"b1","payable_before":50000,"payable_after":80000,"payment_status_before":"unpaid","payment_status_after":"unpaid",'
          + '"settlement_status_before":"unsettled","settlement_status_after":"unsettled","ar_ap_item_id":""}' }));
    }
    if ((init?.method ?? "GET") === "POST" && url.includes("/api/v1/bar/bills/")) return Promise.resolve(ok({ id: "result-1" }));
    return Promise.resolve(ok({ items: [], page: 1, page_size: 50, total: 0 }));
  }));
  vi.spyOn(window, "confirm").mockReturnValue(true);
  const permissions: EffectivePermissionsState = { status: "ready", value: { grants, denies: [], wildcard: false } };
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "cashier", accountType: "staff" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: permissions,
  };
  render(<BarOrdersPage {...props} />);
  return props;
}

const ALL = ["bar.order.view", "bar.bill.tip", "bar.bill.split_merge", "bar.bill.adjust", "finance.payment.register"];

function calls(path: string, method = "GET") {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) => String(input).endsWith(path) && ((init as RequestInit | undefined)?.method ?? "GET") === method);
}

function body(path: string) {
  const posted = calls(path, "POST");
  return JSON.parse(String((posted[posted.length - 1]![1] as RequestInit).body)) as Record<string, unknown>;
}

async function ready() {
  await waitFor(() => expect(screen.queryByTestId("bar-order-bill-select")).not.toBeNull());
  await waitFor(() => expect((screen.getByTestId("bar-order-select-ord-1") as HTMLInputElement).checked).toBe(true));
  await waitFor(() => expect((screen.getByTestId("bar-order-bill-select") as HTMLSelectElement).value).toBe("b1"));
  await waitFor(() => expect(calls("/api/v1/bar/bills/b1/operations").length).toBeGreaterThan(0));
}

afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe("BarOrdersPage 账单操作：小费、拆单、合单、补足低消", () => {
  it("拆单：填拆出金额 300 元与原因确认后提交 split_amount_cent=30000、reason", async () => {
    renderPage(ALL);
    await ready();
    await userEvent.type(screen.getByLabelText("拆出金额"), "300");
    await userEvent.type(screen.getByLabelText("拆单原因"), "客人分开付");
    await userEvent.click(screen.getByTestId("bar-bill-split"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b1/split", "POST").length).toBe(1));
    expect(body("/api/v1/bar/bills/b1/split")).toMatchObject({ split_amount_cent: 30000, reason: "客人分开付" });
    expect(body("/api/v1/bar/bills/b1/split").idempotency_key).toBeTruthy();
  });

  it("小费按比例 10% 提交 tip_ratio_bps=1000，按金额 50 元提交 tip_amount_cent=5000", async () => {
    renderPage(ALL);
    await ready();
    await userEvent.type(screen.getByLabelText("小费比例"), "10");
    await userEvent.click(screen.getByTestId("bar-bill-tip-submit"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b1/tip", "POST").length).toBe(1));
    expect(body("/api/v1/bar/bills/b1/tip")).toMatchObject({ tip_ratio_bps: 1000 });
    expect(body("/api/v1/bar/bills/b1/tip")).not.toHaveProperty("tip_amount_cent");

    await waitFor(() => expect((screen.getByLabelText("小费比例") as HTMLInputElement).value).toBe(""));
    await userEvent.type(screen.getByLabelText("小费金额"), "50");
    await userEvent.click(screen.getByTestId("bar-bill-tip-submit"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b1/tip", "POST").length).toBe(2));
    expect(body("/api/v1/bar/bills/b1/tip")).toMatchObject({ tip_amount_cent: 5000 });
    expect(body("/api/v1/bar/bills/b1/tip")).not.toHaveProperty("tip_ratio_bps");
  });

  it("合单下拉只列同一酒吧、未付款、非本张的账单，提交 merged_bill_id 为所选", async () => {
    renderPage(ALL);
    await ready();
    const options = Array.from((screen.getByTestId("bar-bill-merge-target") as HTMLSelectElement).options).map((option) => option.value).filter(Boolean);
    expect(options.sort()).toEqual(["b2", "b3"]);
    fireEvent.change(screen.getByTestId("bar-bill-merge-target"), { target: { value: "b3" } });
    await waitFor(() => expect((screen.getByTestId("bar-bill-merge-target") as HTMLSelectElement).value).toBe("b3"));
    await userEvent.type(screen.getByLabelText("合单原因"), "同行客人一起付");
    await userEvent.click(screen.getByTestId("bar-bill-merge"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b1/merge", "POST").length).toBe(1));
    expect(body("/api/v1/bar/bills/b1/merge")).toMatchObject({ merged_bill_id: "b3", reason: "同行客人一起付" });
  });

  it("账单下拉切到第二张后点结算，结账打到第二张账单", async () => {
    renderPage(ALL);
    await ready();
    fireEvent.change(screen.getByTestId("bar-order-bill-select"), { target: { value: "b2" } });
    await waitFor(() => expect((screen.getByTestId("bar-order-bill-select") as HTMLSelectElement).value).toBe("b2"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b2/operations").length).toBeGreaterThan(0));
    fireEvent.change(screen.getByLabelText("本次付款凭据编号"), { target: { value: "pay-2" } });
    await waitFor(() => expect((screen.getByLabelText("本次付款凭据编号") as HTMLInputElement).value).toBe("pay-2"));
    await userEvent.click(screen.getByTestId("bar-order-settle"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b2/settle", "POST").length).toBe(1));
    expect(calls("/api/v1/bar/bills/b1/settle", "POST").length).toBe(0);
  });

  it("所选账单有合单记录时出现「客人原账单」下拉，小费提交带 source_bill_id", async () => {
    renderPage(ALL, { merged: true });
    await ready();
    await waitFor(() => expect(screen.queryByTestId("bar-bill-tip-source")).not.toBeNull());
    fireEvent.change(screen.getByTestId("bar-bill-tip-source"), { target: { value: "b3" } });
    await waitFor(() => expect((screen.getByTestId("bar-bill-tip-source") as HTMLSelectElement).value).toBe("b3"));
    await userEvent.type(screen.getByLabelText("小费金额"), "50");
    await userEvent.click(screen.getByTestId("bar-bill-tip-submit"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b1/tip", "POST").length).toBe(1));
    expect(body("/api/v1/bar/bills/b1/tip")).toMatchObject({ tip_amount_cent: 5000, source_bill_id: "b3" });
  });

  it("没有 bar.bill.split_merge 时拆单、合单按钮不可点", async () => {
    renderPage(["bar.order.view", "bar.bill.tip", "finance.payment.register"]);
    await ready();
    expect((screen.getByTestId("bar-bill-split") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("bar-bill-merge") as HTMLButtonElement).disabled).toBe(true);
  });

  it("所选账单低消差额 300 元时显示差额，确认补足提交 adjustment_type=min_spend_top_up、amount_cent=30000", async () => {
    renderPage(ALL, { gap: 30000 });
    await ready();
    await waitFor(() => expect(screen.queryByTestId("bar-bill-min-spend-gap")?.textContent).toContain("低消差额 ¥300.00"));
    await userEvent.click(screen.getByTestId("bar-bill-min-spend-top-up"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b1/adjustments", "POST").length).toBe(1));
    expect(body("/api/v1/bar/bills/b1/adjustments")).toMatchObject({ adjustment_type: "min_spend_top_up", amount_cent: 30000 });
    expect(String(body("/api/v1/bar/bills/b1/adjustments").reason ?? "")).not.toBe("");
  });

  // 第十八批 AY 段第 342 条 342-R2（BSM:1079「拆台后低消不足不阻断，各结果台显示差额并按既有规则继续消费或结账补足」；BSM:1064「不足最低消费的要补足」）：
  // 已结清的账单所在台仍有低消差额时照样显示差额与补足入口；后端另开一张补足账单（返回的 bill_id 不是所选账单），页面提示到「办理账单」里选中结账。
  it("已结清账单差额 300 元时显示「结账或关台前须补足」，补足提交 amount_cent=30000，另开补足账单后提示选中结账", async () => {
    renderPage(ALL, { gap: 30000, b1: { status: "paid", paid_amount_cent: 50000 }, topUpBillId: "b-topup" });
    await waitFor(() => expect(screen.getByTestId("bar-bill-min-spend-gap")).toHaveTextContent("关台前须补足"));
    await ready();
    await userEvent.click(screen.getByTestId("bar-bill-min-spend-top-up"));
    expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining("另开一张补足账单"));
    await waitFor(() => expect(calls("/api/v1/bar/bills/b1/adjustments", "POST").length).toBe(1));
    expect(body("/api/v1/bar/bills/b1/adjustments")).toMatchObject({ adjustment_type: "min_spend_top_up", amount_cent: 30000 });
    await waitFor(() => expect(screen.getByTestId("bar-bill-checkout-note")).toHaveTextContent("已另开补足低消账单"));
  });

  // 第十八批 AU 段收尾复核（PGM:287「界面全中文（所有终端零英文）……状态、枚举取值……必须全部为中文」；BSM:1079「账单合并称为“合单”」）：
  // 合单后的副单状态 merged 在办理账单下拉、所选账单行与订单列表账单列都显示「已合并」。
  it("已并入别单的副单在办理账单下拉、所选账单行与订单列表显示「已合并」，不出现 merged", async () => {
    renderPage(ALL, { secondaryMerged: true });
    await ready();
    await userEvent.click(screen.getByTestId("bar-order-select-ord-2"));
    await waitFor(() => expect((screen.getByTestId("bar-order-bill-select") as HTMLSelectElement).value).toBe("b3"));
    const option = (screen.getByTestId("bar-order-bill-select") as HTMLSelectElement).selectedOptions[0]!.textContent ?? "";
    expect(option).toContain("BN-b3 · 已合并");
    expect(screen.getByTestId("bar-bill-checkout-panel").textContent).toContain("所选账单：BN-b3 · 已合并");
    expect(screen.getByTestId("bar-order-row-ord-2").textContent).toContain("b3 / 已合并");
    expect(document.body.textContent).not.toMatch(/merged/);
  });

  // 同上依据：合单、录入小费、补足低消成功后结果区（ActionResultView）里的动作码与状态码都显示中文；服务端快照 snapshot_json（英文键与状态码的 JSON 串）不上屏。
  it("合单、录入小费、补足低消成功后结果区显示「已合单」「未结清」「非销售人员」「补足低消差额」，不出现英文码", async () => {
    const props = renderPage(ALL, { gap: 30000 });
    await ready();
    const lastResult = () => formatActionResult(vi.mocked(props.setActionResult).mock.calls.at(-1)?.[0]);

    fireEvent.change(screen.getByTestId("bar-bill-merge-target"), { target: { value: "b3" } });
    await waitFor(() => expect((screen.getByTestId("bar-bill-merge-target") as HTMLSelectElement).value).toBe("b3"));
    await userEvent.type(screen.getByLabelText("合单原因"), "同行客人一起付");
    await userEvent.click(screen.getByTestId("bar-bill-merge"));
    await waitFor(() => expect(lastResult()).toContain("操作类型：已合单"));
    expect(lastResult()).not.toMatch(/merge|applied|primary_bill_id|related_bill_id|payable/);
    expect(lastResult()).not.toContain("快照内容");

    await userEvent.type(screen.getByLabelText("小费金额"), "50");
    await userEvent.click(screen.getByTestId("bar-bill-tip-submit"));
    await waitFor(() => expect(lastResult()).toContain("结算状态：未结清"));
    expect(lastResult()).toContain("销售岗位类型：非销售人员");
    expect(lastResult()).not.toMatch(/unsettled|unpaid|non_sales|CNY/);

    await userEvent.click(screen.getByTestId("bar-bill-min-spend-top-up"));
    await waitFor(() => expect(lastResult()).toContain("调整类型：补足低消差额"));
    expect(lastResult()).not.toMatch(/min_spend_top_up|applied|payable_before|unpaid|unsettled/);
    expect(lastResult()).not.toContain("快照内容");
  });

  // 第十八批 AY 段第 348 条 348-R1（BSM:1079「账单合并称为“合单”，不等同于台位“并台”。」）：合单确认的影响说明称副单「已合并」，与状态字典同一叫法，不出「已并单」。
  it("合单确认说明副单变为「已合并」，不出现「已并单」", async () => {
    renderPage(ALL);
    await ready();
    fireEvent.change(screen.getByTestId("bar-bill-merge-target"), { target: { value: "b3" } });
    await waitFor(() => expect((screen.getByTestId("bar-bill-merge-target") as HTMLSelectElement).value).toBe("b3"));
    await userEvent.type(screen.getByLabelText("合单原因"), "同行客人一起付");
    await userEvent.click(screen.getByTestId("bar-bill-merge"));
    await waitFor(() => expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining("合入后副单变为已合并")));
    expect(vi.mocked(window.confirm).mock.calls.flat().join("\n")).not.toContain("已并单");
  });

  it("差额为 0 时不出现补足按钮", async () => {
    renderPage(ALL);
    await ready();
    expect(screen.queryByTestId("bar-bill-min-spend-top-up")).toBeNull();
    expect(screen.queryByTestId("bar-bill-min-spend-gap")).toBeNull();
  });
});
