// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AX 段第 273 条 273-R3（PGM:436「任何公司向酒吧提供的借款，出借时须写明金额、利率、还款期限与还款顺序」「分红方案提交时须附借款清偿情况」）：
// 持股分红页的酒吧借款面板——列出登记的借款与未还本息、状态；填齐后按蛇形键登记；无 finance.dividend.manage 不能登记。计算单详情列出该酒吧的借款清偿情况。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", merchant_type: "bar", status: "active" }];
const ENTITIES = [{ id: "ent_platform", entity_name: "娱乐平台公司", entity_role: "platform", status: "active" }];
const OVERDUE = {
  id: "dbl_1", merchant_id: "merchant-1", lender_entity_id: "ent_platform", lender_name: "娱乐平台公司", principal_cent: 1000000,
  annual_rate_bps: 600, interest_cent: 30000, lent_on: "2026-01-01", due_date: "2026-06-30", repay_order: 1,
  ar_ap_item_id: "arap_dbl_1", item_no: "ARAP-LOAN-1", outstanding_cent: 1030000, status: "overdue",
};
const STATEMENT = {
  id: "divs_1", merchant_id: "merchant-1", plan_id: "divp_1", kind: "official", period_label: "2026-08", period_start: "2026-08-01",
  period_end: "2026-08-31", net_profit_cent: 5000000, distributable_cent: 3970000, loan_repayment_cent: 1030000, status: "draft",
};

const writes: { url: string; body: Record<string, unknown> }[] = [];
let loanRows: unknown[] = [OVERDUE];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    if (method === "POST") {
      writes.push({ url, body: init?.body ? JSON.parse(String(init.body)) : {} });
      return Promise.resolve(envelope({ ...OVERDUE, id: "dbl_new", status: "not_due" }));
    }
    if (url.includes("/dividends/loans")) return Promise.resolve(envelope(loanRows));
    if (url.includes("/platform/business-entities")) return Promise.resolve(envelope(ENTITIES));
    if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
    if (/\/dividends\/statements\/[^/?]+$/.test(url)) {
      return Promise.resolve(envelope({ statement: STATEMENT, lines: [
        { id: "l1", statement_id: "divs_1", seq: 1, line_type: "loan_repayment", amount_cent: -1030000, detail: "出借方 娱乐平台公司，到期 2026-06-30" },
      ] }));
    }
    if (url.includes("/dividends/statements") || url.includes("/dividends/reserve-releases")) {
      return Promise.resolve(envelope({ items: url.includes("/dividends/statements") ? [STATEMENT] : [], page: 1, page_size: 20, total: 1 }));
    }
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["finance.dividend.view"], denies: [], wildcard: false } };

function renderPage(effectivePermissions: EffectivePermissionsState) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
    navigateTo: vi.fn(),
  };
  render(<FinanceDividendsPage {...props} />);
}

function change(testId: string, value: string) {
  fireEvent.change(screen.getByTestId(testId), { target: { value } });
}

describe("FinanceDividendsPage 酒吧借款（273-R3）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    writes.length = 0;
    loanRows = [OVERDUE];
  });

  it("列出借款的未还本息与状态；填齐后按蛇形键登记酒吧、出借方、本金、年利率、到期应付利息、出借日、还款期限与还款顺序", async () => {
    renderPage(ADMIN_PERMS);
    await waitFor(() => expect(screen.queryByTestId("dividend-loan-row-dbl_1")?.textContent).toContain("逾期未还"));
    expect(screen.getByTestId("dividend-loan-row-dbl_1").textContent).toContain("10300.00");
    await waitFor(() => expect(screen.getByTestId("dividend-loan-lender-entity").textContent).toContain("娱乐平台公司"));
    await waitFor(() => expect((screen.getByTestId("dividend-loan-merchant") as HTMLSelectElement).value).toBe("merchant-1"));

    change("dividend-loan-lender-entity", "ent_platform");
    change("dividend-loan-lender-name", "娱乐平台公司");
    change("dividend-loan-principal", "10000");
    change("dividend-loan-rate", "6");
    change("dividend-loan-interest", "300");
    change("dividend-loan-lent-on", "2026-01-01");
    change("dividend-loan-due-date", "2026-12-31");
    change("dividend-loan-repay-order", "1");
    fireEvent.click(screen.getByTestId("dividend-loan-register"));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]?.url).toContain("/api/v1/finance/dividends/loans");
    expect(writes[0]?.body).toMatchObject({
      merchant_id: "merchant-1", lender_entity_id: "ent_platform", lender_name: "娱乐平台公司", principal_cent: 1000000, annual_rate_bps: 600,
      interest_cent: 30000, lent_on: "2026-01-01", due_date: "2026-12-31", repay_order: 1,
    });
  });

  it("无 finance.dividend.manage 时登记借款按钮不可用", async () => {
    renderPage(VIEW_PERMS);
    await waitFor(() => expect(screen.queryByTestId("dividend-loan-row-dbl_1")).not.toBeNull());
    expect((screen.getByTestId("dividend-loan-register") as HTMLButtonElement).disabled).toBe(true);
  });

  it("计算单详情列出借款清偿额、借款清偿明细行与该酒吧借款清偿情况；没有登记借款时写明", async () => {
    loanRows = [];
    renderPage(ADMIN_PERMS);
    await waitFor(() => expect(screen.queryByTestId("dividend-statement-row-divs_1")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("dividend-loan-empty")).not.toBeNull());
    fireEvent.click(screen.getByTestId("dividend-statement-row-divs_1"));
    await waitFor(() => expect(screen.queryByText("借款清偿情况")).not.toBeNull());
    expect(screen.queryByText("该酒吧无登记借款")).not.toBeNull();
    expect(screen.queryByText("借款清偿（分红前置）")).not.toBeNull();
  });
});
