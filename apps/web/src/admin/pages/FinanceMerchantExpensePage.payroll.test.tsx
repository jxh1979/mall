// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceMerchantExpensePage } from "./FinanceMerchantExpensePage";
import { ActionResultView } from "../components/ActionResultView";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AC 段第 210 条 210-R1（BSM:1801「连接工资单月结，拿到这期的人工成本」）：人工成本自动挂接行的来源与挂接结果条数。
const ENTRIES = [
  { id: "mex_apy_pay_1", merchant_id: "merchant-1", period: "2026-10", expense_type: "labor", amount_cent: 100000, source: "auto_payroll",
    source_ref: "pay_1", note: "工资单月结人工成本（工资单 pay_1）", operator_user_id: "usr_admin", created_at: "2026-11-02T00:00:00Z" },
];

function text(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200 });
}

function renderPage(setActionResult: AdminPageProps["setActionResult"]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("/merchant-expenses/sync-auto")) {
      return Promise.resolve(text({ period: "2026-10", energy_inserted: 0, property_inserted: 0, contract_property_inserted: 0,
        rent_inserted: 0, payroll_inserted: 3, drift: [], locked_merchants: [] }));
    }
    if (url.includes("/merchant-expenses/period-locks") || url.includes("/merchant-expenses/summary")) return Promise.resolve(text([]));
    if (url.includes("/merchant-expenses")) return Promise.resolve(text({ items: ENTRIES, page: 1, page_size: 20, total: ENTRIES.length }));
    if (url.includes("/platform/merchants")) return Promise.resolve(text([{ id: "merchant-1", merchant_name: "商户甲", status: "active" }]));
    return Promise.resolve(text([]));
  }));
  const permissions: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
  render(<FinanceMerchantExpensePage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
    setActionResult={setActionResult} setGlobalError={vi.fn()} effectivePermissions={permissions} />);
}

describe("FinanceMerchantExpensePage 工资单月结人工成本挂接", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("来源列显示「自动·工资单月结」，自动挂接结果显示工资单月结人工成本挂接条数", async () => {
    const setActionResult = vi.fn();
    renderPage(setActionResult);
    await waitFor(() => expect(screen.getByTestId("merchant-expense-row-mex_apy_pay_1").textContent).toContain("自动·工资单月结"));
    await waitFor(() => expect(screen.getByTestId("merchant-expense-row-mex_apy_pay_1").textContent).toContain("商户甲"));

    fireEvent.click(screen.getByTestId("merchant-expense-sync-run"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalled());
    render(<ActionResultView value={setActionResult.mock.calls.at(-1)?.[0]} />);
    expect(screen.getByTestId("admin-action-result").textContent).toContain("工资单月结人工成本已挂接：3");
  });
});
