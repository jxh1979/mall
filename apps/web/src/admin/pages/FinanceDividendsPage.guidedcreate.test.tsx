// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 持股分红：方案引导式配置（三步：商户主理人→比例税率周期→基数构成）+ 期间作业 + 计算单状态机动作。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }];
const PLANS = [
  {
    id: "divp_1", merchant_id: "merchant-1", principal_user_id: "usr_principal", principal_name: "主理人甲",
    platform_share_bps: 8000, tax_rate_bps: 2000, period_type: "quarterly",
    revenue_domains: ["bar", "ticket"], include_inventory_cost: true,
    expense_types: ["rent_allocation", "energy", "property_fee", "labor", "amortization", "other"],
    status: "active", created_at: "2026-07-18T00:00:00Z", updated_at: "2026-07-18T00:00:00Z",
  },
];
const STATEMENTS = [
  {
    id: "divs_draft", merchant_id: "merchant-1", plan_id: "divp_1", kind: "official", period_label: "2026-Q2",
    period_start: "2026-04-01", period_end: "2026-06-30", principal_user_id: "usr_principal",
    platform_share_bps: 8000, tax_rate_bps: 2000, revenue_domains: ["bar", "ticket"],
    include_inventory_cost: true, expense_types: ["labor"],
    revenue_cent: 1020000, cost_cent: 150000, expense_cent: 220000, net_profit_cent: 650000,
    distributable_cent: 650000, platform_share_cent: 520000, principal_gross_cent: 130000,
    tax_cent: 26000, principal_net_cent: 104000, no_cost_basis_count: 0, legal_distributable_cent: 650000,
    status: "draft", created_at: "2026-07-01T04:00:00Z",
  },
  {
    id: "divs_pending", merchant_id: "merchant-1", plan_id: "divp_1", kind: "official", period_label: "2026-Q1",
    revenue_cent: 500000, cost_cent: 0, expense_cent: 0, net_profit_cent: 500000,
    distributable_cent: 500000, platform_share_cent: 400000, principal_gross_cent: 100000,
    tax_cent: 20000, principal_net_cent: 80000, no_cost_basis_count: 0,
    status: "pending_review", created_at: "2026-04-01T04:00:00Z",
  },
];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastWriteUrl: string | null = null;
let lastWriteBody: unknown = null;
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (method === "POST" || method === "PUT") {
        lastWriteUrl = url;
        lastWriteBody = init?.body ? JSON.parse(String(init.body)) : null;
        if (url.includes("/compute")) {
          return Promise.resolve(envelope({ period_label: "2026-Q2", kind: "official", computed: [STATEMENTS[0]], skipped: [] }));
        }
        return Promise.resolve(envelope({ id: "divp_new" }));
      }
      if (/\/dividends\/statements\/[^/?]+$/.test(url)) {
        return Promise.resolve(envelope({
          statement: { ...STATEMENTS[0], arrears_offset_cent: 28000, reserve_withheld_cent: 10000, payout_cent: 66000 },
          lines: [
            { id: "l1", statement_id: "divs_draft", seq: 1, line_type: "revenue_bar", amount_cent: 800000, detail: "酒吧结账实收" },
            { id: "l2", statement_id: "divs_draft", seq: 2, line_type: "net_profit", amount_cent: 650000, detail: "净利润" },
            { id: "l3", statement_id: "divs_draft", seq: 3, line_type: "loss_carryforward", amount_cent: -50000, detail: "亏损结转弥补 50000" },
            { id: "l4", statement_id: "divs_draft", seq: 4, line_type: "principal_net", amount_cent: 104000, detail: "税后应得" },
            { id: "l5", statement_id: "divs_draft", seq: 5, line_type: "arrears_offset", amount_cent: -28000, detail: "应收 AR-001 抵扣 28000" },
            { id: "l6", statement_id: "divs_draft", seq: 6, line_type: "reserve_withheld", amount_cent: -10000, detail: "留存 10%" },
            { id: "l7", statement_id: "divs_draft", seq: 7, line_type: "payout", amount_cent: 66000, detail: "实发额" },
          ],
        }));
      }
      if (url.includes("/dividends/statements")) {
        return Promise.resolve(envelope({ items: STATEMENTS, page: 1, page_size: 20, total: STATEMENTS.length }));
      }
      if (url.includes("/dividends/plans")) return Promise.resolve(envelope(PLANS));
      if (url.includes("/platform/merchants")) return Promise.resolve(envelope(MERCHANTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinanceDividendsPage {...props} />);
  return { setActionResult };
}

describe("FinanceDividendsPage 持股分红", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastWriteUrl = null;
    lastWriteBody = null;
  });

  it("方案表 + 计算单表展示；按状态渲染动作按钮", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-plan-row-divp_1")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("dividend-statement-row-divs_draft")).toBeTruthy());
    // draft 行：提交审阅；pending_review 行：审批通过 + 驳回。
    expect(screen.getByTestId("dividend-submit-divs_draft")).toBeTruthy();
    expect(screen.getByTestId("dividend-approve-divs_pending")).toBeTruthy();
    expect(screen.getByTestId("dividend-reject-open-divs_pending")).toBeTruthy();
    expect(screen.queryByTestId("dividend-plan-guided")).toBeNull();
  });

  it("引导式新建方案：校验拦截→确认页→提交带正确 body（百分比转万分比）", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-plan-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-plan-create-open"));

    // singlePage 模式：全部字段一屏。主理人留空点下一步 → 校验拦截，不进确认页。
    fireEvent.change(screen.getByTestId("dividend-plan-principal"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    expect(screen.getByTestId("dividend-plan-guided-error").textContent).toContain("主理人");
    expect(screen.queryByTestId("dividend-plan-guided-summary")).toBeNull();

    // 补齐：主理人 + 比例 70/30 + 去掉售票收入域 + 只留人工费用。
    fireEvent.change(screen.getByTestId("dividend-plan-principal"), { target: { value: "usr_new_principal" } });
    fireEvent.change(screen.getByTestId("dividend-plan-principal-name"), { target: { value: "主理人乙" } });
    fireEvent.change(screen.getByTestId("dividend-plan-share"), { target: { value: "70" } });
    fireEvent.click(screen.getByTestId("dividend-plan-domain-ticket"));
    for (const type of ["rent_allocation", "energy", "property_fee", "amortization", "other"]) {
      fireEvent.click(screen.getByTestId(`dividend-plan-expense-${type}`));
    }
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));

    await waitFor(() => expect(screen.getByTestId("dividend-plan-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-plan-save"));

    await waitFor(() => expect(lastWriteBody).toBeTruthy());
    const body = lastWriteBody as Record<string, unknown>;
    expect(lastWriteUrl).toContain("/api/v1/finance/dividends/plans");
    expect(body.merchant_id).toBe("merchant-1");
    expect(body.principal_user_id).toBe("usr_new_principal");
    expect(body.platform_share_bps).toBe(7000);
    expect(body.tax_rate_bps).toBe(2000);
    expect(body.period_type).toBe("quarterly");
    expect(body.revenue_domains).toEqual(["bar"]);
    expect(body.expense_types).toEqual(["labor"]);
    await waitFor(() => expect(screen.queryByTestId("dividend-plan-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "dividend_plan_created" }));
  });

  it("期间作业：填期间标签生成计算单；驳回走理由必填弹窗", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-compute-run")).toBeTruthy());
    fireEvent.change(screen.getByTestId("dividend-compute-label"), { target: { value: "2026-Q2" } });
    fireEvent.click(screen.getByTestId("dividend-compute-run"));
    await waitFor(() => expect(lastWriteUrl).toContain("/statements/compute"));
    expect((lastWriteBody as Record<string, unknown>).period_label).toBe("2026-Q2");
    expect((lastWriteBody as Record<string, unknown>).kind).toBe("official");
    await waitFor(() => expect(screen.getByTestId("dividend-compute-notice").textContent).toContain("1 张"));

    // 驳回：理由留空拦截，填理由后提交打到 /{id}/reject。
    fireEvent.click(screen.getByTestId("dividend-reject-open-divs_pending"));
    fireEvent.click(screen.getByTestId("dividend-reject-guided-next"));
    expect(screen.getByTestId("dividend-reject-guided-error").textContent).toContain("理由");
    fireEvent.change(screen.getByTestId("dividend-reject-note"), { target: { value: "费用未挂接完整" } });
    fireEvent.click(screen.getByTestId("dividend-reject-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dividend-reject-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-reject-save"));
    await waitFor(() => expect(lastWriteUrl).toContain("/api/v1/finance/dividends/statements/divs_pending/reject"));
    expect((lastWriteBody as Record<string, unknown>).note).toBe("费用未挂接完整");
  });

  // ── V303 扣除项三开关 ──────────────────────────────────────────

  it("扣除项默认全关：新建方案 body 三开关为 false、保护线参数为 0", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-plan-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-plan-create-open"));
    fireEvent.change(screen.getByTestId("dividend-plan-principal"), { target: { value: "usr_p_default" } });
    // 三开关默认渲染为未勾选，保护线参数输入未展开。
    expect((screen.getByTestId("dividend-plan-loss-carryforward") as HTMLInputElement).checked).toBe(false);
    expect((screen.getByTestId("dividend-plan-arrears-offset") as HTMLInputElement).checked).toBe(false);
    expect((screen.getByTestId("dividend-plan-payout-floor") as HTMLInputElement).checked).toBe(false);
    expect(screen.queryByTestId("dividend-plan-floor-percent")).toBeNull();
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dividend-plan-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-plan-save"));
    await waitFor(() => expect(lastWriteBody).toBeTruthy());
    const body = lastWriteBody as Record<string, unknown>;
    expect(body.loss_carryforward_enabled).toBe(false);
    expect(body.arrears_offset_enabled).toBe(false);
    expect(body.payout_floor_enabled).toBe(false);
    expect(body.payout_floor_bps).toBe(0);
    expect(body.payout_floor_fixed_cent).toBe(0);
  });

  it("开启三扣除 + 保护线留存 10%：body 携带开关与 payout_floor_bps", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-plan-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-plan-create-open"));
    fireEvent.change(screen.getByTestId("dividend-plan-principal"), { target: { value: "usr_p_ded" } });
    fireEvent.click(screen.getByTestId("dividend-plan-loss-carryforward"));
    fireEvent.click(screen.getByTestId("dividend-plan-arrears-offset"));
    fireEvent.click(screen.getByTestId("dividend-plan-payout-floor"));
    // 保护线开启后留存比例输入展开。
    await waitFor(() => expect(screen.getByTestId("dividend-plan-floor-percent")).toBeTruthy());
    fireEvent.change(screen.getByTestId("dividend-plan-floor-percent"), { target: { value: "10" } });
    fireEvent.click(screen.getByTestId("dividend-plan-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dividend-plan-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-plan-save"));
    await waitFor(() => expect(lastWriteBody).toBeTruthy());
    const body = lastWriteBody as Record<string, unknown>;
    expect(body.loss_carryforward_enabled).toBe(true);
    expect(body.arrears_offset_enabled).toBe(true);
    expect(body.payout_floor_enabled).toBe(true);
    expect(body.payout_floor_bps).toBe(1000);
  });

  it("计算单明细抽屉：逐行展示三扣除项 + 实发额", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dividend-statement-row-divs_draft")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dividend-statement-row-divs_draft"));
    // 抽屉逐行渲染扣除项与实发额（DIVIDEND_LINE_LABELS 中文标签）。
    expect(await screen.findByText("亏损结转（往期未弥补亏损）")).toBeTruthy();
    expect(await screen.findByText("欠款抵扣（未清应收）")).toBeTruthy();
    expect(await screen.findByText("发放保护线留存")).toBeTruthy();
    expect(await screen.findByText("实发额（挂应付）")).toBeTruthy();
  });
});
