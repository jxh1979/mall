// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceDividendsPage } from "./FinanceDividendsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AQ 段第 243 条 243-R1（BSM:1787「酒吧关闭须完成关停结算，依法核清本次分红、已算未发分红与发放保护线待释放款项」「已确认应付及待释款项的
// 法律性质和处理由财务、法务核实……相关款项须按核实结果处理并留痕后才能结案。」；PGM:528「需要平行线审批的（如法务、财务），另行指定该条线的最终审批人」）：
// 持股分红页末尾的酒吧关停结算——列出三类款项，关店所在期正式单未出时提示且不能提交；逐条法律性质、处理方式与财务、法务两线最终审批人填齐才提交；
// 核实通过后逐条登记处理再结案。
const MERCHANTS = [{ id: "merchant-1", merchant_name: "酒吧甲", status: "active" }];
const PLANS = [{
  id: "divp_1", merchant_id: "merchant-1", principal_user_id: "usr_principal", platform_share_bps: 8000,
  period_type: "monthly", period_min_months: 1, period_max_months: 12, revenue_domains: ["bar"], payout_pay_slot: 1, status: "active",
  effective_from: "2026-01-01", version_state: "in_force", reason: "按工商登记持股比例",
}];
const ITEMS = [
  { item_type: "current_period", period_label: "2026-08", statement_id: null, amount_cent: 0 },
  { item_type: "unpaid_dividend", period_label: "2026-06", statement_id: "divs_06", statement_status: "draft", amount_cent: 1600000 },
  { item_type: "reserve_pending", period_label: "2026-07", statement_id: "divs_07", ar_ap_item_id: "arap_r07",
    source_type: "dividend_reserve_withheld", amount_cent: 400000 },
];
const OPEN = { id: "dco_1", merchant_id: "merchant-1", closed_business_date: "2026-08-20", reason: "租约到期退场关店", status: "open", closeout_items: ITEMS };
const READY = { ...OPEN, closeout_items: [{ ...ITEMS[0], statement_id: "divs_08", statement_status: "draft", amount_cent: 1600000 }, ITEMS[1], ITEMS[2]] };
const VERIFIED = {
  ...READY, status: "verified",
  closeout_items: READY.closeout_items.map((item, index) => ({ ...item, id: `dcoi_${index}`, legal_nature: "已确认的股东分红应付", handling: "按原分配依据付清" })),
};

let closeouts: unknown[] = [OPEN];
const writes: { url: string; body: Record<string, unknown> }[] = [];

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
      return Promise.resolve(envelope(closeouts[0]));
    }
    if (url.includes("/dividends/closeouts")) return Promise.resolve(envelope(closeouts));
    if (url.includes("/dividends/plans")) return Promise.resolve(envelope(PLANS));
    if (url.includes("/dividends/statements") || url.includes("/dividends/reserve-releases")) {
      return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
    }
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
    navigateTo: vi.fn(),
  };
  render(<FinanceDividendsPage {...props} />);
}

describe("FinanceDividendsPage 酒吧关停结算（243-R1）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    closeouts = [OPEN];
    writes.length = 0;
  });

  it("列出三类款项，关店所在期正式单未出时提示且不能提交；登记载荷为酒吧、关店最后营业日与原因", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-closeout-row-dco_1")?.textContent).toContain("待提交核实"));
    const table = screen.getByTestId("dividend-closeout-items-dco_1").textContent ?? "";
    expect(table).toContain("本次分红");
    expect(table).toContain("已算未发分红");
    expect(table).toContain("保护线待释放");
    expect(table).toContain("16000.00");
    expect(screen.getByTestId("dividend-closeout-current-missing-dco_1").textContent).toContain("关店所在期正式单未出，出单后才能提交核实");
    expect((screen.getByTestId("dividend-closeout-submit-dco_1") as HTMLButtonElement).disabled).toBe(true);

    await waitFor(() => expect(screen.getByTestId("dividend-closeout-merchant").textContent).toContain("酒吧甲"));
    fireEvent.change(screen.getByTestId("dividend-closeout-date"), { target: { value: "2026-08-20" } });
    fireEvent.change(screen.getByTestId("dividend-closeout-reason"), { target: { value: "租约到期退场关店" } });
    fireEvent.click(screen.getByTestId("dividend-closeout-register"));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]?.url).toContain("/api/v1/finance/dividends/closeouts");
    expect(Object.keys(writes[0]?.body ?? {}).sort()).toEqual(["closed_business_date", "idempotency_key", "merchant_id", "reason"]);
    expect(writes[0]?.body).toMatchObject({ merchant_id: "merchant-1", closed_business_date: "2026-08-20", reason: "租约到期退场关店" });
  });

  it("未填法律性质点提交显示中文提示且不发请求；填齐后逐条带法律性质与处理方式、财务法务两线最终审批人提交", async () => {
    closeouts = [READY];
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-closeout-items-dco_1")?.textContent).toContain("计算单草稿"));
    fireEvent.change(screen.getByTestId("dividend-closeout-final-approver-dco_1"), { target: { value: "usr_main" } });
    fireEvent.change(screen.getByTestId("dividend-closeout-line-dco_1-finance_review"), { target: { value: "usr_fin" } });
    fireEvent.change(screen.getByTestId("dividend-closeout-line-dco_1-legal_review"), { target: { value: "usr_legal" } });
    fireEvent.click(screen.getByTestId("dividend-closeout-submit-dco_1"));
    await waitFor(() => expect(screen.getByTestId("dividend-closeout-error").textContent).toContain("法律性质"));
    expect(writes).toHaveLength(0);

    for (const index of [0, 1, 2]) {
      fireEvent.change(screen.getByTestId(`dividend-closeout-nature-dco_1-${index}`), { target: { value: "已确认的股东分红应付" } });
      fireEvent.change(screen.getByTestId(`dividend-closeout-handling-dco_1-${index}`), { target: { value: "按原分配依据付清" } });
    }
    fireEvent.click(screen.getByTestId("dividend-closeout-submit-dco_1"));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]?.url).toContain("/api/v1/finance/dividends/closeouts/dco_1/submit");
    expect(writes[0]?.body).toMatchObject({
      final_approver_id: "usr_main",
      parallel_lines: [{ line_code: "finance_review", final_approver_id: "usr_fin" }, { line_code: "legal_review", final_approver_id: "usr_legal" }],
    });
    const filled = { legal_nature: "已确认的股东分红应付", handling: "按原分配依据付清" };
    expect(writes[0]?.body.items).toEqual([
      { item_type: "current_period", statement_id: "divs_08", ar_ap_item_id: null, ...filled },
      { item_type: "unpaid_dividend", statement_id: "divs_06", ar_ap_item_id: null, ...filled },
      { item_type: "reserve_pending", statement_id: "divs_07", ar_ap_item_id: "arap_r07", ...filled },
    ]);
  });

  it("核实通过后逐条登记处理（处理说明必填）并结案", async () => {
    closeouts = [VERIFIED];
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("dividend-closeout-row-dco_1")?.textContent).toContain("已核实待处理"));
    expect(screen.getByTestId("dividend-closeout-items-dco_1").textContent).toContain("按原分配依据付清");
    fireEvent.click(screen.getByTestId("dividend-closeout-handle-dco_1-0"));
    await waitFor(() => expect(screen.getByTestId("dividend-closeout-error").textContent).toContain("请填写处理说明"));
    expect(writes).toHaveLength(0);

    fireEvent.change(screen.getByTestId("dividend-closeout-note-dco_1-0"), { target: { value: "已付清并留存凭证" } });
    fireEvent.click(screen.getByTestId("dividend-closeout-handle-dco_1-0"));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]?.url).toContain("/api/v1/finance/dividends/closeouts/dco_1/items/dcoi_0/handle");
    expect(writes[0]?.body).toMatchObject({ handled_note: "已付清并留存凭证" });

    await waitFor(() => expect((screen.getByTestId("dividend-closeout-close-dco_1") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByTestId("dividend-closeout-close-dco_1"));
    await waitFor(() => expect(writes).toHaveLength(2));
    expect(writes[1]?.url).toContain("/api/v1/finance/dividends/closeouts/dco_1/close");
  });
});
