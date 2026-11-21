// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BR 段第 274 条 274-R1（ENUM:297「引进参股的游乐项目的参股方为引进该项目的域主管公司」；BSM:753「游乐公司再按已批准的内部结算价或域内规则，与实际
// 提供服务的游乐商户结算」）：方案弹窗收入域多「游乐域内结算」勾选项，勾选后提交体 revenue_domains 含 amusement；明细抽屉 revenue_amusement 行显示「游乐域内结算
// 收入」。274-R2：尾差明细行名改中性措辞「分配尾差（经营主体承担）」（与商户专区共用，对酒吧同样成立）。
const MERCHANTS = [{ id: "merchant-amu", merchant_name: "游乐甲", status: "active" }];
const PLAN = {
  id: "divp_amu", merchant_id: "merchant-amu", principal_user_id: "usr_principal", platform_share_bps: 8000,
  platform_share_min_bps: 5000, platform_share_max_bps: 9000,
  period_type: "monthly", period_min_months: 1, period_max_months: 12, revenue_domains: ["bar"], payout_pay_slot: 1,
  status: "active", reason: "按工商登记持股比例", effective_from: "2026-01-01", version_state: "in_force", other_shareholders: [],
};
const STATEMENT = {
  id: "divs_amu", merchant_id: "merchant-amu", plan_id: "divp_amu", kind: "official", period_label: "2026-08",
  period_start: "2026-08-01", period_end: "2026-08-31", principal_user_id: "usr_principal", platform_share_bps: 8000, tax_rate_bps: 2000,
  revenue_domains: ["amusement"], revenue_cent: 300000, cost_cent: 0, expense_cent: 0, net_profit_cent: 300000, distributable_cent: 300000,
  platform_share_cent: 240000, principal_gross_cent: 60001, other_shareholders_gross_cent: 0, rounding_residual_cent: -1,
  tax_cent: 12000, principal_net_cent: 48001, payout_cent: 48001, legal_distributable_cent: 300000, status: "draft",
  created_at: "2026-09-01T04:00:00Z",
};
const LINES = [
  { id: "divs_amu_1", statement_id: "divs_amu", seq: 1, line_type: "revenue_amusement", amount_cent: 300000, detail: "游乐公司按内部结算价与本商户结算" },
  { id: "divs_amu_2", statement_id: "divs_amu", seq: 2, line_type: "rounding_residual", amount_cent: -1, detail: "由本经营主体承担" },
];

const writes: { method: string; url: string; body: Record<string, unknown> }[] = [];

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    if (method === "POST" || method === "PUT") {
      writes.push({ method, url, body: init?.body ? JSON.parse(String(init.body)) : {} });
      return Promise.resolve(ok({ ...PLAN, id: "divp_new", status: "pending_review", version_state: "pending" }));
    }
    if (url.includes("/dividends/plans")) return Promise.resolve(ok([PLAN]));
    if (/\/dividends\/statements\/[^/?]+$/.test(url)) return Promise.resolve(ok({ statement: STATEMENT, lines: LINES }));
    if (url.includes("/dividends/statements")) return Promise.resolve(ok({ items: [STATEMENT], page: 1, page_size: 20, total: 1 }));
    if (url.includes("/dividends/reserve-releases")) return Promise.resolve(ok({ items: [], page: 1, page_size: 20, total: 0 }));
    if (url.includes("/platform/merchants")) return Promise.resolve(ok(MERCHANTS));
    return Promise.resolve(ok([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinanceDividendsPage {...props} />);
}

describe("FinanceDividendsPage 游乐域内结算收入域（274-R1、274-R2）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    writes.length = 0;
  });

  it("方案弹窗出现「游乐域内结算」勾选项，勾选后提交体 revenue_domains 含 amusement", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-plan-row-divp_amu").textContent).toContain("游乐甲"));
    fireEvent.click(screen.getByTestId("dividend-plan-edit-divp_amu"));
    await waitFor(() => expect(screen.queryByTestId("dividend-plan-domain-amusement")?.parentElement?.textContent).toContain("游乐域内结算"));

    fireEvent.click(screen.getByTestId("dividend-plan-domain-amusement"));
    fireEvent.change(screen.getByTestId("dividend-plan-effective-from"), { target: { value: "2026-12-01" } });
    fireEvent.change(screen.getByTestId("dividend-plan-reason"), { target: { value: "引进参股游乐项目计入游乐域内结算" } });
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dividend-plan-guided-summary").textContent).toContain("游乐域内结算"));
    fireEvent.click(screen.getByTestId("dividend-plan-save"));

    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]?.body.revenue_domains).toEqual(expect.arrayContaining(["amusement"]));
  });

  it("明细抽屉 revenue_amusement 行显示「游乐域内结算收入」，尾差行显示「分配尾差（经营主体承担）」", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-statement-row-divs_amu").textContent).toContain("游乐甲"));
    fireEvent.click(screen.getByTestId("dividend-statement-row-divs_amu"));
    await waitFor(() => expect(screen.queryByText("游乐域内结算收入")).not.toBeNull());
    expect(screen.queryByText("分配尾差（经营主体承担）")).not.toBeNull();
    expect(screen.queryByText("分配尾差（酒吧公司承担）")).toBeNull();
    expect(screen.queryAllByText(/多方按比例各自四舍五入的差额，由本经营主体承担/).length).toBeGreaterThan(0);
  });
});
