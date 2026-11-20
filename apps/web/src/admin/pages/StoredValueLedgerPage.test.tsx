// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { StoredValueLedgerPage } from "./StoredValueLedgerPage";
import { AdminApi } from "../client/adminClient";
import type { AdminUser } from "../types";

// 第十八批 BD 段第 364 条 364-R2（BSM:833「运营端提供储值台账，可按客人、商户、期间查询逐笔明细与余额构成。」）：后台「储值台账」页。
// 假响应走真实客户端（文本体、蛇形键）。
const QUERY = "/api/v1/loyalty/stored-value/ledger/query";
const base = { created_at: "2026-11-19T02:00:00Z", account_id: "svacc_g", card_type: "general", project_id: "prj_a", issuer_merchant_id: null,
  issuer_merchant_name: null, customer_id: "cus_1", masked_phone: "139****1234", payment_channel: null, consume_merchant_id: null,
  consume_merchant_name: null, original_txn_id: null, reason: null, bonus_cent: 0, bonus_after_cent: 1000 };
const rows = [
  { ...base, txn_id: "t5", txn_type: "consume", amount_cent: -500, principal_cent: -500, balance_after_cent: 8500, principal_after_cent: 7500 },
  { ...base, txn_id: "t3", txn_type: "consume_reversal", amount_cent: 1000, principal_cent: 1000, balance_after_cent: 9000,
    principal_after_cent: 8000, original_txn_id: "t2", consume_merchant_id: "mch_bar", consume_merchant_name: "星光酒吧" },
  { ...base, txn_id: "t2", txn_type: "consume", amount_cent: -3000, principal_cent: -3000, balance_after_cent: 8000, principal_after_cent: 7000,
    consume_merchant_id: "mch_bar", consume_merchant_name: "星光酒吧" },
  { ...base, txn_id: "t1", txn_type: "topup", amount_cent: 11000, principal_cent: 10000, bonus_cent: 1000, balance_after_cent: 11000,
    principal_after_cent: 10000, payment_channel: "wechat" },
  { ...base, txn_id: "t4", account_id: "svacc_m", card_type: "merchant", issuer_merchant_id: "mch_bar", issuer_merchant_name: "星光酒吧",
    txn_type: "topup", amount_cent: 2000, principal_cent: 2000, balance_after_cent: 2000, principal_after_cent: 2000, bonus_after_cent: 0 },
];
const user: AdminUser = { id: "u1", username: "u1", accountType: "staff", organizationId: "prj_a", merchantId: null };

type Call = { url: string; method: string; body: Record<string, unknown> | null };

function setup(items: unknown[], grants: string[] = ["customer.stored_value.view"]) {
  const calls: Call[] = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    calls.push({ url, method: init?.method ?? "GET", body: init?.body ? JSON.parse(String(init.body)) as Record<string, unknown> : null });
    return new Response(JSON.stringify({ success: true, code: "OK", data: { items, page: 1, page_size: 20, total: items.length } }));
  }));
  const reportViewedObjects = vi.fn();
  render(<StoredValueLedgerPage api={new AdminApi("test")} user={user} setActionResult={vi.fn()} setGlobalError={vi.fn()}
    reportViewedObjects={reportViewedObjects} effectivePermissions={{ status: "ready", value: { grants, denies: [], wildcard: false } }} />);
  return { calls, reportViewedObjects };
}

const queries = (calls: Call[]) => calls.filter((c) => c.url.endsWith(QUERY));

describe("储值台账", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("按手机号与期间查询，表格显示类型、卡与本金赠送金构成中文", async () => {
    const { calls } = setup(rows);
    await waitFor(() => expect(screen.queryByLabelText("客人手机号")).not.toBeNull());
    expect(queries(calls)).toHaveLength(0);
    fireEvent.change(screen.getByLabelText("客人手机号"), { target: { value: "13800000000" } });
    fireEvent.click(screen.getByTestId("stored-value-ledger-query"));
    await waitFor(() => expect(queries(calls)[0]).toMatchObject({ method: "POST",
      body: { phone: "13800000000", date_from: expect.any(String), date_to: expect.any(String), page: 1, size: 20 } }));
    await waitFor(() => expect(screen.queryByTestId("stored-value-ledger-table")).not.toBeNull());
    const table = screen.getByTestId("stored-value-ledger-table");
    expect(screen.getAllByText("充值", { selector: "td" })).toHaveLength(2);
    expect(screen.getAllByText("消费", { selector: "td" })).toHaveLength(2);
    expect(screen.getByText("退款回冲")).toBeTruthy();
    expect(screen.getAllByText("综合体通用卡").length).toBeGreaterThan(0);
    expect(screen.getByText("商户卡·星光酒吧")).toBeTruthy();
    expect(screen.getByText("70.00")).toBeTruthy();
    expect(screen.getAllByText("星光酒吧")).toHaveLength(2);
    expect(screen.getByText("综合体")).toBeTruthy();
    expect(screen.getByText("微信")).toBeTruthy();
    expect(table.textContent).not.toMatch(/consume_reversal|general|wechat/);
  });

  it("按商户编号查询带 merchantId，本页满 20 笔时下一页带 page 2", async () => {
    const { calls } = setup(Array.from({ length: 20 }, (_, i) => ({ ...rows[0], txn_id: `t${i}` })));
    await waitFor(() => expect(screen.queryByLabelText("商户编号")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("商户编号"), { target: { value: "mch_bar" } });
    fireEvent.click(screen.getByTestId("stored-value-ledger-query"));
    await waitFor(() => expect(queries(calls)[0]?.body).toMatchObject({ merchant_id: "mch_bar", page: 1 }));
    await waitFor(() => expect((screen.getByTestId("stored-value-ledger-next") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("stored-value-ledger-next"));
    await waitFor(() => expect(queries(calls)[1]?.body).toMatchObject({ merchant_id: "mch_bar", page: 2 }));
  });

  it("敏感页登记本次看到的卡", async () => {
    const { reportViewedObjects } = setup(rows);
    await waitFor(() => expect(screen.queryByTestId("stored-value-ledger-query")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-value-ledger-query"));
    await waitFor(() => expect(reportViewedObjects).toHaveBeenLastCalledWith("customer_stored_value", [
      { id: "svacc_g", customerId: "cus_1", merchantId: null },
      { id: "svacc_m", customerId: "cus_1", merchantId: "mch_bar" },
    ]));
  });

  it("无权限按钮禁用", async () => {
    setup([], []);
    await waitFor(() => expect((screen.queryByTestId("stored-value-ledger-query") as HTMLButtonElement | null)?.disabled).toBe(true));
  });
});
