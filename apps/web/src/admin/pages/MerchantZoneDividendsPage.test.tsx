import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneDividendsPage } from "./MerchantZoneDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 商户专区 · 我的分红（主理人只读视图）：列表 + 方案摘要 + 算式明细抽屉；无任何写动作入口。
const PLANS = [
  {
    id: "divp_1", merchant_id: "merchant-1", principal_user_id: "usr_principal", principal_name: "主理人甲",
    platform_share_bps: 8000, tax_rate_bps: 2000, period_type: "quarterly",
    revenue_domains: ["bar", "ticket"], include_inventory_cost: true, expense_types: ["labor"],
    status: "active", payout_floor_enabled: true, reserve_release_mode: "next_period",
  },
];
const RESERVES = [
  {
    statement_id: "divs_q2", merchant_id: "merchant-1", principal_user_id: "usr_principal",
    period_label: "2026-Q2", statement_status: "approved", reserve_withheld_cent: 16000,
    released: false, trigger_type: null, released_by: null, released_at: null,
  },
  {
    statement_id: "divs_q1", merchant_id: "merchant-1", principal_user_id: "usr_principal",
    period_label: "2026-Q1", statement_status: "paid", reserve_withheld_cent: 12000,
    released: true, trigger_type: "scheduled", released_by: "system_dividend_reserve_release",
    released_at: "2026-07-19T04:00:00Z",
  },
];
const STATEMENTS = [
  {
    id: "divs_q2", merchant_id: "merchant-1", kind: "official", period_label: "2026-Q2",
    period_start: "2026-04-01", period_end: "2026-06-30",
    platform_share_bps: 8000, tax_rate_bps: 2000,
    revenue_cent: 1020000, cost_cent: 150000, expense_cent: 220000, net_profit_cent: 650000,
    distributable_cent: 650000, platform_share_cent: 520000, principal_gross_cent: 130000,
    tax_cent: 26000, principal_net_cent: 104000, status: "approved", created_at: "2026-07-01T04:00:00Z",
  },
  {
    id: "divs_prev", merchant_id: "merchant-1", kind: "preview", period_label: "2026-07",
    revenue_cent: 300000, cost_cent: 0, expense_cent: 0, net_profit_cent: 300000,
    distributable_cent: 300000, platform_share_cent: 240000, principal_gross_cent: 60000,
    tax_cent: 12000, principal_net_cent: 48000, status: "preview", created_at: "2026-07-18T04:00:00Z",
  },
];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (/\/dividends\/statements\/[^/?]+$/.test(url)) {
        return Promise.resolve(envelope({
          statement: STATEMENTS[0],
          lines: [
            { id: "l1", statement_id: "divs_q2", seq: 1, line_type: "revenue_bar", amount_cent: 800000, detail: "酒吧结账实收" },
            { id: "l2", statement_id: "divs_q2", seq: 2, line_type: "tax_withheld", amount_cent: -26000, detail: "代扣股息红利个税" },
            { id: "l3", statement_id: "divs_q2", seq: 3, line_type: "principal_net", amount_cent: 104000, detail: "净发放额" },
          ],
        }));
      }
      if (url.includes("/dividends/reserve-releases")) {
        return Promise.resolve(envelope({ items: RESERVES, page: 1, page_size: 10, total: RESERVES.length }));
      }
      if (url.includes("/dividends/statements")) {
        return Promise.resolve(envelope({ items: STATEMENTS, page: 1, page_size: 10, total: STATEMENTS.length }));
      }
      if (url.includes("/dividends/plans")) return Promise.resolve(envelope(PLANS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const MERCHANT_PERMS: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["finance.dividend.view"], denies: [], wildcard: false },
};

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u-merchant", username: "merchant-admin", accountType: "merchant" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: MERCHANT_PERMS,
  };
  render(<MerchantZoneDividendsPage {...props} />);
}

describe("MerchantZoneDividendsPage 我的分红", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("列表展示每期金额与状态 + 当前方案摘要；只读无写动作", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-dividend-row-divs_q2")).toBeTruthy());
    expect(screen.getByTestId("merchant-dividend-row-divs_prev")).toBeTruthy();
    const row = screen.getByTestId("merchant-dividend-row-divs_q2");
    expect(row.textContent).toContain("6,500.00");
    expect(row.textContent).toContain("1,040.00");
    expect(row.textContent).toContain("已审批");
    // 月度预览同样可见（只看不发）。
    expect(screen.getByTestId("merchant-dividend-row-divs_prev").textContent).toContain("月度预览");
    // 方案摘要（比例/税率/周期口径）。
    expect(screen.getByTestId("merchant-dividend-plan-summary").textContent).toContain("80%");
    expect(screen.getByTestId("merchant-dividend-plan-summary").textContent).toContain("20%");
    // 只读页：无提交/审批/驳回/发放入口。
    expect(screen.queryByTestId("dividend-submit-divs_q2")).toBeNull();
    expect(screen.queryByTestId("dividend-approve-divs_q2")).toBeNull();
  });

  it("我的留存：展示待释放/已释放状态 + 方案释放时点提示（只读）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-dividend-reserve-row-divs_q2")).toBeTruthy());
    // 待释放行（暂扣）。
    expect(screen.getByTestId("merchant-dividend-reserve-row-divs_q2").textContent).toContain("待释放（暂扣）");
    expect(screen.getByTestId("merchant-dividend-reserve-row-divs_q2").textContent).toContain("160.00");
    // 已释放行（可发放）。
    expect(screen.getByTestId("merchant-dividend-reserve-row-divs_q1").textContent).toContain("已释放（可发放）");
    // 方案摘要含释放时点提示。
    expect(screen.getByTestId("merchant-dividend-reserve-hint").textContent).toContain("下期返还");
    // 只读：无释放按钮。
    expect(screen.queryByTestId("dividend-reserve-release-divs_q2")).toBeNull();
  });

  it("点行打开算式明细抽屉：逐项金额 + 取数口径说明", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-dividend-row-divs_q2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-dividend-row-divs_q2"));
    await waitFor(() => expect(screen.getByText(/分红明细 · 2026-Q2/)).toBeTruthy());
    expect(screen.getByText(/酒吧结账实收/)).toBeTruthy();
    expect(screen.getByText(/-260\.00（代扣股息红利个税）/)).toBeTruthy();
    expect(screen.getByText(/1,040\.00（净发放额）/)).toBeTruthy();
  });
});
