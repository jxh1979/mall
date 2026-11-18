// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AQ 段第 234 条 234-R2（BSM:1775「娱乐平台公司、主理人和其他股东按每家酒吧工商登记的真实持股比例分配」；BSM:1757「多方分配产生的非涉税尾差
// 单独留痕」；BSM:1776「股东为公司股东的，不代扣个人所得税」）：方案弹窗可增删其他股东、实时显示主理人比例，合计超过 100% 拦住提交；方案表列平台、主理人与
// 其他股东合计；明细抽屉按该单所用版本列各方比例，并显示其他股东应得、代扣、实发与分配尾差四类明细行。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }];
const PLAN = {
  id: "divp_now", merchant_id: "merchant-1", principal_user_id: "usr_principal", platform_share_bps: 8000,
  platform_share_min_bps: 5000, platform_share_max_bps: 9000,
  period_type: "semiannual", period_min_months: 3, period_max_months: 12, revenue_domains: ["bar"], payout_pay_slot: 1,
  status: "active", reason: "按工商登记持股比例", effective_from: "2026-01-01", version_state: "in_force",
  other_shareholders: [{ id: "dps_c", plan_id: "divp_now", shareholder_name: "丙公司", shareholder_type: "company", share_bps: 500, sort_order: 0 }],
};
const STATEMENT = {
  id: "divs_aug", merchant_id: "merchant-1", plan_id: "divp_now", kind: "official", period_label: "2026-08",
  period_start: "2026-08-01", period_end: "2026-08-31", principal_user_id: "usr_principal", platform_share_bps: 8000, tax_rate_bps: 2000,
  revenue_domains: ["bar"], revenue_cent: 1000006, cost_cent: 0, expense_cent: 0, net_profit_cent: 1000006, distributable_cent: 1000006,
  platform_share_cent: 800005, principal_gross_cent: 150002, other_shareholders_gross_cent: 50000, rounding_residual_cent: -1,
  tax_cent: 30000, principal_net_cent: 120002, payout_cent: 120002, legal_distributable_cent: 2000000, status: "draft",
  created_at: "2026-09-01T04:00:00Z",
};
const LINES = [
  { id: "divs_aug_1", statement_id: "divs_aug", seq: 1, line_type: "platform_share", amount_cent: 800005, detail: "平台分成" },
  { id: "divs_aug_2", statement_id: "divs_aug", seq: 2, line_type: "shareholder_gross", amount_cent: 50000, detail: "其他股东「丙公司」应得" },
  { id: "divs_aug_3", statement_id: "divs_aug", seq: 3, line_type: "shareholder_tax_withheld", amount_cent: 0, detail: "公司股东不代扣个税" },
  { id: "divs_aug_4", statement_id: "divs_aug", seq: 4, line_type: "shareholder_payout", amount_cent: 50000, detail: "审批通过后按此额挂应付" },
  { id: "divs_aug_5", statement_id: "divs_aug", seq: 5, line_type: "rounding_residual", amount_cent: -1, detail: "由本酒吧公司承担" },
];

const writes: { method: string; url: string; body: Record<string, unknown> }[] = [];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    if (method === "POST" || method === "PUT") {
      writes.push({ method, url, body: init?.body ? JSON.parse(String(init.body)) : {} });
      return Promise.resolve(envelope({ ...PLAN, id: "divp_new", status: "pending_review", version_state: "pending" }));
    }
    if (url.includes("/dividends/plans")) return Promise.resolve(envelope([PLAN]));
    if (/\/dividends\/statements\/[^/?]+$/.test(url)) return Promise.resolve(envelope({ statement: STATEMENT, lines: LINES }));
    if (url.includes("/dividends/statements")) return Promise.resolve(envelope({ items: [STATEMENT], page: 1, page_size: 20, total: 1 }));
    if (url.includes("/dividends/reserve-releases")) return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
    if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
    return Promise.resolve(envelope([]));
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

describe("FinanceDividendsPage 其他股东（234-R2）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    writes.length = 0;
  });

  it("方案弹窗增删其他股东、实时显示主理人比例，合计超过 100% 拦住提交；提交带 other_shareholders 蛇形键", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-plan-row-divp_now").textContent).toContain("酒吧甲"));
    expect(screen.getByTestId("dividend-plan-row-divp_now").textContent).toContain("平台 80% / 主理人 15% / 其他股东 5%（1 人）");

    fireEvent.click(screen.getByTestId("dividend-plan-edit-divp_now"));
    await waitFor(() => expect((screen.queryByTestId("dividend-plan-shareholder-name-0") as HTMLInputElement | null)?.value).toBe("丙公司"));
    fireEvent.change(screen.getByTestId("dividend-plan-share"), { target: { value: "60" } });
    fireEvent.click(screen.getByTestId("dividend-plan-shareholder-add"));
    fireEvent.change(screen.getByTestId("dividend-plan-shareholder-name-1"), { target: { value: "股东乙" } });
    fireEvent.change(screen.getByTestId("dividend-plan-shareholder-percent-1"), { target: { value: "50" } });
    expect(screen.getByTestId("dividend-plan-shareholder-over").textContent).toContain("平台与其他股东持股比例合计不能超过 100%");
    fireEvent.change(screen.getByTestId("dividend-plan-effective-from"), { target: { value: "2026-12-01" } });
    fireEvent.change(screen.getByTestId("dividend-plan-reason"), { target: { value: "工商登记新增股东乙" } });
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    expect(screen.getByTestId("dividend-plan-guided-error").textContent).toContain("平台与其他股东持股比例合计不能超过 100%");
    expect(writes).toHaveLength(0);

    fireEvent.click(screen.getByTestId("dividend-plan-shareholder-remove-0"));
    fireEvent.change(screen.getByTestId("dividend-plan-shareholder-percent-0"), { target: { value: "10" } });
    expect(screen.getByTestId("dividend-plan-principal-share").textContent).toContain("30%");
    expect(screen.queryByTestId("dividend-plan-shareholder-over")).toBeNull();
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dividend-plan-guided-summary").textContent)
      .toContain("平台 60% / 主理人 30% / 其他股东 10%（1 人）"));
    fireEvent.click(screen.getByTestId("dividend-plan-save"));

    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]?.body.other_shareholders).toEqual([{ shareholder_name: "股东乙", shareholder_type: "individual", share_bps: 1000 }]);
  });

  it("明细抽屉按该单所用版本列各方比例，并显示其他股东应得、代扣、实发与分配尾差四类明细行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-statement-row-divs_aug").textContent).toContain("酒吧甲"));
    await waitFor(() => expect(screen.getByTestId("dividend-plan-row-divp_now").textContent).toContain("酒吧甲"));
    fireEvent.click(screen.getByTestId("dividend-statement-row-divs_aug"));
    await waitFor(() => expect(screen.queryByText("其他股东应得")).not.toBeNull());
    for (const label of ["其他股东代扣个税", "其他股东实发（挂应付）", "分配尾差（酒吧公司承担）"]) {
      expect(screen.queryByText(label)).not.toBeNull();
    }
    expect(screen.queryByText(/平台 80% \/ 主理人 15% \/ 丙公司（公司股东）5%/)).not.toBeNull();
  });
});
