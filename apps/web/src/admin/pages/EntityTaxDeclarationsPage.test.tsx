// 文件版本：V0.05
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { EntityTaxDeclarationsPage } from "./EntityTaxDeclarationsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AC 段第 210 条 210-R2（第十八批裁决 42：财务按公司、税种、所属期手工登记「实际申报金额」，经财务、税务、法务审核并由顶层平台公司老板
// 最终确认后生效）：税费申报登记页列表中文显示，登记弹窗按蛇形键提交。
const ROWS = [
  { id: "etd_1", business_entity_id: "be_1", entity_name: "甲酒吧公司", tax_type: "vat", period_start_month: "2026-01", period_end_month: "2026-03",
    declared_amount_cent: 1234567, reason: "一季度增值税", evidence_attachment_ids: [], status: "pending_review", review_id: "review_1",
    created_by: "usr_fin", created_at: "2026-04-10T02:00:00Z", approved_at: null,
    system_calculated_amount_cent: 1300000, declaration_difference_cent: -65433, difference_mechanism: "adjustment", calculation_basis: [] },
  // 第十八批 AR 段第 233 条：已生效行带最近一条已生效实缴（233-R1）与一条已生效差异调整单（233-R3）。
  { id: "etd_2", business_entity_id: "be_1", entity_name: "甲酒吧公司", tax_type: "vat", period_start_month: "2026-01", period_end_month: "2026-01",
    declared_amount_cent: 1000000, reason: "一月增值税", evidence_attachment_ids: [], status: "approved", review_id: "review_2",
    created_by: "usr_fin", created_at: "2026-02-10T02:00:00Z", approved_at: "2026-02-12T02:00:00Z",
    paid_amount_cent: 930000, payment_difference_cent: -100000, payment_breakdown: [{ category: "unpaid", amount_cent: 100000 }], payment_pending: false,
    // 第十八批 AR 段第 233 条 233-R5：系统计算金额、申报差异、差异处理与逐月逐收入类型的计算依据。
    system_calculated_amount_cent: 954000, declaration_difference_cent: 46000, difference_mechanism: "adjustment",
    calculation_basis: [
      { month: "2026-01", income_type: "catering", income_cent: 10494000, rate_bps: 600, rate_version_id: "stc_9", tax_amount_cent: 594000 },
      { month: "2026-01", income_type: "rent", income_cent: 4360000, rate_bps: 900, rate_version_id: "stc_8", tax_amount_cent: 360000 },
    ],
    adjustments: [{ id: "eta_1", declaration_id: "etd_2", kind: "period_difference", amount_cent: 30000, origin_period_start_month: "2026-01",
      origin_period_end_month: "2026-01", accounting_month: "2026-11", reason: "更正申报表", status: "approved", review_id: "review_3" }] },
  // 233-R5、233-R7：企业所得税按季与财税软件核对拉平（第十八批裁决 59），系统计算＝利润×比例（裁决 60），已生效后以申报额为准拉平。
  { id: "etd_3", business_entity_id: "be_1", entity_name: "甲酒吧公司", tax_type: "corporate_income", period_start_month: "2026-01",
    period_end_month: "2026-03", declared_amount_cent: 50000, reason: "一季度企业所得税", evidence_attachment_ids: [], status: "approved",
    review_id: "review_4", created_by: "usr_fin", created_at: "2026-04-12T02:00:00Z", approved_at: "2026-04-15T02:00:00Z", payment_pending: false,
    adjustments: [], difference_mechanism: "alignment", system_calculated_amount_cent: 50000, declaration_difference_cent: 0, aligned_amount_cent: 50000,
    calculation_basis: [{ month: "2026-03", income_type: "all", income_cent: 300000, rate_bps: 2500, rate_version_id: "stc_4", tax_amount_cent: 50000,
      profit: { revenue_cent: 300000, revenue_items: [{ income_type: "ticket", amount_cent: 300000 }], cost_cent: 100000,
        cost_items: [{ cost_type: "payroll", amount_cent: 100000 }], profit_cent: 200000, basis: "platform" } }] },
  // 第十八批 AR 段第 233 条收尾复核：申报低于系统计算，批准时生成的冲回调整单（金额存差额绝对值）。
  { id: "etd_4", business_entity_id: "be_1", entity_name: "甲酒吧公司", tax_type: "vat", period_start_month: "2026-02", period_end_month: "2026-02",
    declared_amount_cent: 590000, reason: "二月增值税", evidence_attachment_ids: [], status: "approved", review_id: "review_5",
    created_by: "usr_fin", created_at: "2026-03-10T02:00:00Z", approved_at: "2026-03-12T02:00:00Z", payment_pending: false,
    system_calculated_amount_cent: 600000, declaration_difference_cent: -10000, difference_mechanism: "adjustment", calculation_basis: [],
    adjustments: [{ id: "eta_2", declaration_id: "etd_4", kind: "reversal", amount_cent: 10000, origin_period_start_month: "2026-02",
      origin_period_end_month: "2026-02", accounting_month: "2026-03", reason: "申报低于系统计算", status: "approved", review_id: "review_5" }] },
];
// 第十八批 AR 段第 233 条 233-R2：税率版本（平台默认的出厂种子生效日为空；含本条新增的停车费、零售、供货货款与企业所得税「全部收入」）。
const RATES = [
  { id: "stc_1", business_entity_id: null, entity_name: null, income_type: "parking", tax_type: "vat", applicable: true, rate_bps: 600,
    effective_from: null, reason: null, status: "approved", review_id: null },
  { id: "stc_2", business_entity_id: "be_1", entity_name: "甲酒吧公司", income_type: "retail", tax_type: "surcharge", applicable: true, rate_bps: 900,
    effective_from: "2026-03-01", reason: "零售附加税", status: "pending_review", review_id: "review_9" },
  { id: "stc_3", business_entity_id: "be_1", entity_name: "甲酒吧公司", income_type: "goods", tax_type: "cultural_fee", applicable: false, rate_bps: 0,
    effective_from: "2026-04-01", reason: "供货货款不计", status: "rejected", review_id: "review_10" },
  { id: "stc_4", business_entity_id: "be_1", entity_name: "甲酒吧公司", income_type: "all", tax_type: "corporate_income", applicable: true, rate_bps: 2500,
    effective_from: "2026-01-01", reason: "企业所得税比例", status: "approved", review_id: "review_11" },
];

// 第十八批 AR 段第 233 条 233-R6：娱乐平台公司一季度收入、成本分项与利润（平台主体口径，第十八批裁决 60）。
const PROFIT = { revenue_cent: 5000000, revenue_items: [{ income_type: "ticket", amount_cent: 5000000 }], cost_cent: 4250000,
  cost_items: [{ cost_type: "pool_distribution", amount_cent: 3000000 }, { cost_type: "channel_fee", amount_cent: 250000 },
    { cost_type: "payroll", amount_cent: 1000000 }], profit_cent: 750000, basis: "platform" };

function text(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200 });
}

let lastPost: { url: string; body: Record<string, unknown> } | null = null;
let ratesFetched = false;
let profitUrl: string | null = null;

function renderPage(setActionResult: AdminPageProps["setActionResult"] = vi.fn()) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      lastPost = { url, body: JSON.parse(String(init?.body)) };
      if (url.includes("/entity-taxes/rates")) return Promise.resolve(text({ ...RATES[1], id: "stc_new" }));
      return Promise.resolve(text({ ...ROWS[0], id: "etd_new", tax_type: "cultural_fee" }));
    }
    if (url.includes("/finance/entity-taxes/profits")) {
      profitUrl = url;
      return Promise.resolve(text(PROFIT));
    }
    if (url.includes("/finance/entity-taxes/rates")) {
      ratesFetched = true;
      return Promise.resolve(text(RATES));
    }
    if (url.includes("/platform/business-entities")) {
      return Promise.resolve(text({ items: [{ id: "be_1", entity_name: "甲酒吧公司", entity_role: "management_company", status: "active" }], page: 1, page_size: 500, total: 1 }));
    }
    if (url.includes("/finance/entity-taxes")) return Promise.resolve(text({ items: ROWS, page: 1, page_size: 20, total: ROWS.length }));
    return Promise.resolve(text([]));
  }));
  const permissions: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
  render(<EntityTaxDeclarationsPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
    setActionResult={setActionResult} setGlobalError={vi.fn()} effectivePermissions={permissions} />);
}

describe("EntityTaxDeclarationsPage 税费申报登记", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPost = null;
    ratesFetched = false;
    profitUrl = null;
  });

  it("列表的税种与状态显示中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("entity-tax-row-etd_1")?.textContent).toContain("增值税"));
    const row = screen.getByTestId("entity-tax-row-etd_1").textContent ?? "";
    expect(row).toContain("甲酒吧公司");
    expect(row).toContain("2026-01～2026-03");
    expect(row).toContain("12,345.67");
    expect(row).toContain("待财务、税务、法务审核与最高确认");
    expect(row).not.toContain("vat");
    expect(row).not.toContain("pending_review");
  });

  // 第十八批 AR 段第 233 条 233-R1（BSM:1760「缴税差异等于实际缴纳金额减实际申报金额」「实缴与申报不一致的部分先按未缴、预缴、多缴、抵扣或待退分别处理」）。
  it("已生效行显示实际缴纳金额与缴税差异分类，登记实际缴纳按更正后申报额算差额、五类中文、蛇形键提交", async () => {
    const setActionResult = vi.fn();
    renderPage(setActionResult);
    await waitFor(() => expect(screen.queryByTestId("entity-tax-payment-open-etd_2")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    expect(screen.queryByTestId("entity-tax-payment-open-etd_1")).toBeNull();
    const row = screen.getByTestId("entity-tax-row-etd_2").textContent ?? "";
    expect(row).toContain("9,300.00");
    expect(row).toContain("-1,000.00（未缴 1,000.00）");
    expect(row).not.toContain("unpaid");
    fireEvent.click(screen.getByTestId("entity-tax-payment-open-etd_2"));

    fireEvent.change(screen.getByTestId("entity-tax-act-amount"), { target: { value: "9300" } });
    await waitFor(() => expect(screen.getByTestId("entity-tax-act-difference").textContent).toContain("-1,000.00"));
    expect(screen.getByTestId("entity-tax-act-difference").textContent).toContain("10,300.00");
    for (const label of ["未缴", "预缴", "多缴", "抵扣", "待退"]) expect(screen.getByTestId("entity-tax-payment-guided").textContent).toContain(label);
    fireEvent.change(screen.getByTestId("entity-tax-act-category-prepaid"), { target: { value: "400" } });
    fireEvent.change(screen.getByTestId("entity-tax-act-category-unpaid"), { target: { value: "600" } });
    fireEvent.change(screen.getByTestId("entity-tax-act-reason"), { target: { value: "按完税凭证登记" } });
    for (const code of ["finance_review", "tax_review", "legal_review"]) {
      fireEvent.change(screen.getByTestId(`entity-tax-act-${code}`), { target: { value: `usr_${code}` } });
    }
    fireEvent.click(screen.getByTestId("entity-tax-payment-guided-next"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-payment-guided-summary").textContent).toContain("未缴 600.00"));
    fireEvent.click(screen.getByTestId("entity-tax-payment-save"));

    await waitFor(() => expect(lastPost?.url).toContain("/api/v1/finance/entity-taxes/etd_2/payments"));
    expect(lastPost?.body).toMatchObject({
      paid_amount_cent: 930000,
      difference_breakdown: [{ category: "unpaid", amount_cent: 60000 }, { category: "prepaid", amount_cent: 40000 }],
      reason: "按完税凭证登记",
      parallel_lines: [
        { line_code: "finance_review", final_approver_id: "usr_finance_review" },
        { line_code: "tax_review", final_approver_id: "usr_tax_review" },
        { line_code: "legal_review", final_approver_id: "usr_legal_review" },
      ],
    });
    expect(typeof lastPost?.body.idempotency_key).toBe("string");
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "entity_tax_payment_registered" }));
  });

  // 第十八批 AR 段第 233 条 233-R3（BSM:1760、ENUM:201「原期间已经关账、开票或申报的……只能在当前开放期间生成关联原期间的差异调整单」）。
  it("已生效行下挂差异调整单（种类中文、关联原所属期、计入月份），生成差异调整单负数正确转分", async () => {
    const setActionResult = vi.fn();
    renderPage(setActionResult);
    await waitFor(() => expect(screen.queryByTestId("entity-tax-adjust-open-etd_2")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    const row = screen.getByTestId("entity-tax-row-etd_2").textContent ?? "";
    expect(row).toContain("差异调整单 +300.00 元（关联原所属期 2026-01～2026-01，计入 2026-11｜已生效）");
    expect(row).not.toContain("period_difference");
    fireEvent.click(screen.getByTestId("entity-tax-adjust-open-etd_2"));

    fireEvent.change(screen.getByTestId("entity-tax-act-amount"), { target: { value: "-120.5" } });
    fireEvent.change(screen.getByTestId("entity-tax-act-reason"), { target: { value: "更正一月申报表" } });
    for (const code of ["finance_review", "tax_review", "legal_review"]) {
      fireEvent.change(screen.getByTestId(`entity-tax-act-${code}`), { target: { value: `usr_${code}` } });
    }
    fireEvent.click(screen.getByTestId("entity-tax-adjust-guided-next"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-adjust-guided-summary").textContent).toContain("2026-01～2026-01"));
    fireEvent.click(screen.getByTestId("entity-tax-adjust-save"));

    await waitFor(() => expect(lastPost?.url).toContain("/api/v1/finance/entity-taxes/etd_2/adjustments"));
    expect(lastPost?.body).toMatchObject({ amount_cent: -12050, reason: "更正一月申报表" });
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "entity_tax_adjustment_registered" }));
  });

  // 第十八批 AR 段第 233 条收尾复核（BSM:1760「申报金额高于系统计算金额的生成补提调整单、低于的生成冲回调整单」）：冲回调整单在「差异调整单」列不带加号，
  // 与同行「差异处理」列一致；只有关联原期间的差异调整单按正负带符号。
  it("冲回调整单在差异调整单列不带加号，与差异处理列写法一致", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("entity-tax-row-etd_4")?.textContent).toContain("冲回调整单 100.00 元（关联原所属期"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    const row = screen.getByTestId("entity-tax-row-etd_4").textContent ?? "";
    expect(row).toContain("冲回调整单 100.00 元（关联原所属期 2026-02～2026-02，计入 2026-03｜已生效）");
    expect(row).not.toContain("+");
    expect(screen.getByTestId("entity-tax-row-etd_2").textContent).toContain("差异调整单 +300.00 元");
  });

  // 第十八批 AR 段第 233 条 233-R2（ENUM:201「每个主体 × 每类收入 × 每种税（增值税/文化事业建设费/附加税）后台各自配“有没有这笔费用 + 收多少”」；
  // BSM:521「必须填写生效起始日期」；BSM:1760「所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效」）。
  it("税率配置页签点开才加载、列表全中文，提交税率版本按百分数转万分比、蛇形键提交", async () => {
    const setActionResult = vi.fn();
    renderPage(setActionResult);
    await waitFor(() => expect(screen.queryByTestId("entity-tax-tab-rates")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    expect(ratesFetched).toBe(false);
    fireEvent.click(screen.getByTestId("entity-tax-tab-rates"));

    await waitFor(() => expect(screen.queryByTestId("entity-tax-rate-row-stc_2")).not.toBeNull());
    const table = screen.getByTestId("entity-tax-rate-table").textContent ?? "";
    for (const label of ["平台默认", "停车费", "零售", "供货货款", "全部收入", "企业所得税", "附加税", "文化事业建设费", "9.00%", "25.00%", "2026-03-01", "自始有效",
      "已生效", "待财务、税务、法务审核与最高确认"]) {
      expect(table).toContain(label);
    }
    expect(table).not.toMatch(/parking|retail|goods|surcharge|corporate_income|pending_review|rejected/);
    fireEvent.click(screen.getByTestId("entity-tax-rate-open"));

    fireEvent.change(screen.getByTestId("entity-tax-rate-entity"), { target: { value: "be_1" } });
    fireEvent.change(screen.getByTestId("entity-tax-rate-tax"), { target: { value: "vat" } });
    fireEvent.change(screen.getByTestId("entity-tax-rate-income"), { target: { value: "catering" } });
    fireEvent.change(screen.getByTestId("entity-tax-rate-percent"), { target: { value: "9" } });
    fireEvent.change(screen.getByTestId("entity-tax-rate-from"), { target: { value: "2026-03-01" } });
    fireEvent.change(screen.getByTestId("entity-tax-rate-reason"), { target: { value: "餐饮增值税调整" } });
    for (const code of ["finance_review", "tax_review", "legal_review"]) {
      fireEvent.change(screen.getByTestId(`entity-tax-rate-${code}`), { target: { value: `usr_${code}` } });
    }
    fireEvent.click(screen.getByTestId("entity-tax-rate-guided-next"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-rate-guided-summary").textContent).toContain("9%"));
    fireEvent.click(screen.getByTestId("entity-tax-rate-save"));

    await waitFor(() => expect(lastPost?.url).toContain("/api/v1/finance/entity-taxes/rates"));
    expect(lastPost?.body).toMatchObject({
      business_entity_id: "be_1",
      income_type: "catering",
      tax_type: "vat",
      applicable: true,
      rate_bps: 900,
      effective_from: "2026-03-01",
      reason: "餐饮增值税调整",
      parallel_lines: [
        { line_code: "finance_review", final_approver_id: "usr_finance_review" },
        { line_code: "tax_review", final_approver_id: "usr_tax_review" },
        { line_code: "legal_review", final_approver_id: "usr_legal_review" },
      ],
    });
    expect(typeof lastPost?.body.idempotency_key).toBe("string");
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "entity_tax_rate_submitted" }));
  });

  // 第十八批 BU 段 374-R4（ENUM:201「每个主体 × 每类收入 × 每种税（增值税/文化事业建设费/附加税）后台各自配」；BSM:1760「税率、税目、发票对象和红冲流程
  // 由系统按有效规则配置」）：派驻提成与酒吧报酬拨付进开票清单后，税率版本的收入类型可选「人员报酬拨付」。
  it("税率版本的收入类型可选人员报酬拨付", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("entity-tax-tab-rates")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    fireEvent.click(screen.getByTestId("entity-tax-tab-rates"));
    await waitFor(() => expect(screen.queryByTestId("entity-tax-rate-row-stc_2")).not.toBeNull());
    fireEvent.click(screen.getByTestId("entity-tax-rate-open"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-rate-income").textContent).toContain("人员报酬拨付"));
  });

  // 第十八批 AR 段第 233 条 233-R5（BSM:1760「税费金额分三个阶段分别保存、互不覆盖」「申报金额高于系统计算金额的生成补提调整单、低于的生成冲回调整单。
  // 两套差异机制按税种互斥适用」；第十八批裁决 59「其他税费和企业所得税一样，按季与财税软件核对、以财税软件为准拉平」）。
  it("列表显示系统计算金额、申报差异与差异处理，展开计算依据显示月份、收入类型中文、收入、比例与税额", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByText("系统计算金额（元）")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("entity-tax-row-etd_3")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    expect(screen.queryByText("申报差异（元）")).not.toBeNull();
    expect(screen.queryByText("差异处理")).not.toBeNull();
    const approved = screen.getByTestId("entity-tax-row-etd_2").textContent ?? "";
    expect(approved).toContain("9,540.00");
    expect(approved).toContain("补提调整单 460.00 元");
    expect(screen.getByTestId("entity-tax-row-etd_1").textContent).toContain("冲回调整单 654.33 元");
    expect(screen.getByTestId("entity-tax-row-etd_3").textContent).toContain("按季核对拉平");
    expect(screen.queryByTestId("entity-tax-basis-etd_1")).toBeNull();
    const details = screen.getByTestId("entity-tax-basis-etd_2");
    fireEvent.click(details.querySelector("summary") as HTMLElement);
    const basis = details.textContent ?? "";
    for (const label of ["计算依据", "2026-01", "订台低消 / 二次消费 / 额外酒水", "商业租赁 / 市集摊位", "104,940.00", "6.00%", "5,940.00", "9.00%", "3,600.00"]) {
      expect(basis).toContain(label);
    }
    expect(approved).not.toMatch(/catering|rent|adjustment|alignment/);
  });

  // 第十八批 AR 段第 233 条 233-R7（BSM:1760「两套数据须核对，以财税软件的结果为准……税务按季度处理」；第十八批裁决 59「其他税费和企业所得税一样，
  // 按季与财税软件核对、以财税软件为准拉平」；裁决 60「企业所得税系统计算＝利润×配置比例」）。
  it("企业所得税行显示按季核对拉平后金额，计算依据显示以利润为基数", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByText("按季核对拉平，以财税软件结果为准：500.00 元")).toBeInTheDocument());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    const details = screen.getByTestId("entity-tax-basis-etd_3");
    fireEvent.click(details.querySelector("summary") as HTMLElement);
    const basis = details.textContent ?? "";
    for (const label of ["2026-03", "全部收入", "3,000.00", "25.00%", "以利润 2,000.00 元为基数：收入 3,000.00 元 − 成本 1,000.00 元", "500.00"]) {
      expect(basis).toContain(label);
    }
    expect(screen.getByTestId("entity-tax-row-etd_3").textContent).not.toMatch(/alignment|corporate_income/);
  });

  // 第十八批 AR 段第 233 条 233-R6（BSM:1760「娱乐平台公司、游乐公司、传媒公司、中央餐饮公司、城市项目公司五家主体各自的收入、成本与利润，由平台汇总计算」；
  // 第十八批裁决 60「按上面的口径自动汇总」）。
  it("主体收入成本利润页签点开才加载，选主体与起止月份后收入、成本分项与利润全中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByText("主体收入成本利润")).toBeInTheDocument());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    fireEvent.click(screen.getByTestId("entity-tax-tab-profits"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-profit-entity").textContent).toContain("甲酒吧公司"));
    fireEvent.change(screen.getByTestId("entity-tax-profit-entity"), { target: { value: "be_1" } });
    fireEvent.change(screen.getByTestId("entity-tax-profit-start"), { target: { value: "2026-01" } });
    expect(profitUrl).toBeNull();
    fireEvent.change(screen.getByTestId("entity-tax-profit-end"), { target: { value: "2026-03" } });

    await waitFor(() => expect(screen.getByTestId("entity-tax-profit-total").textContent).toContain("7,500.00"));
    expect(profitUrl).toContain("/api/v1/finance/entity-taxes/profits?");
    expect(profitUrl).toContain("business_entity_id=be_1");
    expect(profitUrl).toContain("start_month=2026-01");
    expect(profitUrl).toContain("end_month=2026-03");
    const table = screen.getByTestId("entity-tax-profit-table").textContent ?? "";
    for (const label of ["平台通票 / 门票综合服务费", "50,000.00", "收入合计", "通票分账池内分配", "30,000.00", "渠道费（通路费与销售渠道费）", "2,500.00",
      "工资单税前应发", "10,000.00", "成本合计", "42,500.00", "利润"]) {
      expect(table).toContain(label);
    }
    expect(table).not.toMatch(/ticket|pool_distribution|channel_fee|payroll/);
    expect(screen.getByTestId("entity-tax-profit-basis").textContent).toContain("平台主体");
  });

  // 第十八批 BR 段第 357 条 357-R4（第十八批裁决 60「成本＝该主体在平台内作为付款方的内部结算应付」）：游乐公司的游乐域内结算应付成本分项显中文。
  it("主体收入成本利润成本行显示「游乐域内结算」", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    const fetchMock = vi.mocked(globalThis.fetch);
    const base = fetchMock.getMockImplementation()!;
    fetchMock.mockImplementation((input: RequestInfo | URL, init?: RequestInit) => String(input).includes("/finance/entity-taxes/profits")
      ? Promise.resolve(text({ ...PROFIT, cost_cent: 300000, cost_items: [{ cost_type: "amusement_domain_settlement", amount_cent: 300000 }] }))
      : base(input, init));
    fireEvent.click(screen.getByTestId("entity-tax-tab-profits"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-profit-entity").textContent).toContain("甲酒吧公司"));
    fireEvent.change(screen.getByTestId("entity-tax-profit-entity"), { target: { value: "be_1" } });
    fireEvent.change(screen.getByTestId("entity-tax-profit-start"), { target: { value: "2026-01" } });
    fireEvent.change(screen.getByTestId("entity-tax-profit-end"), { target: { value: "2026-01" } });

    await waitFor(() => expect(screen.getByTestId("entity-tax-profit-table").textContent).toContain("游乐域内结算"));
    expect(screen.getByTestId("entity-tax-profit-table").textContent).not.toContain("amusement_domain_settlement");
  });

  // 第十八批 CC 段第 401 条 401-R2（ENUM:297「引进参股的游乐项目的参股方为引进该项目的域主管公司」）：引进参股的游乐项目经营主体按持股分红税前口径，
  // 口径说明写明，收入行游乐收入显中文。
  it("主体收入成本利润口径说明含引进参股的游乐项目经营主体，收入行显示「游乐收入」", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    const fetchMock = vi.mocked(globalThis.fetch);
    const base = fetchMock.getMockImplementation()!;
    fetchMock.mockImplementation((input: RequestInfo | URL, init?: RequestInit) => String(input).includes("/finance/entity-taxes/profits")
      ? Promise.resolve(text({ revenue_cent: 212000, revenue_items: [{ income_type: "game", amount_cent: 212000 }], cost_cent: 0, cost_items: [],
        profit_cent: 212000, basis: "dividend" }))
      : base(input, init));
    fireEvent.click(screen.getByTestId("entity-tax-tab-profits"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-profit-entity").textContent).toContain("甲酒吧公司"));
    fireEvent.change(screen.getByTestId("entity-tax-profit-entity"), { target: { value: "be_1" } });
    fireEvent.change(screen.getByTestId("entity-tax-profit-start"), { target: { value: "2026-05" } });
    fireEvent.change(screen.getByTestId("entity-tax-profit-end"), { target: { value: "2026-05" } });

    await waitFor(() => expect(screen.queryByTestId("entity-tax-profit-basis")?.textContent ?? "").toContain("引进参股的游乐项目"));
    expect(screen.getByTestId("entity-tax-profit-table").textContent).toContain("游乐收入");
    expect(screen.getByTestId("entity-tax-profit-table").textContent).not.toContain("game");
  });

  it("登记弹窗按蛇形键提交（金额元转分，三条线最终审批人）", async () => {
    const setActionResult = vi.fn();
    renderPage(setActionResult);
    await waitFor(() => expect(screen.queryByTestId("entity-tax-row-etd_1")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("entity-tax-filter-entity").textContent).toContain("甲酒吧公司"));
    fireEvent.click(screen.getByTestId("entity-tax-create-open"));

    fireEvent.change(screen.getByTestId("entity-tax-create-entity"), { target: { value: "be_1" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-type"), { target: { value: "cultural_fee" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-start"), { target: { value: "2026-04" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-end"), { target: { value: "2026-06" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-amount"), { target: { value: "1200.50" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-reason"), { target: { value: "二季度文化事业建设费申报表" } });
    fireEvent.click(screen.getByTestId("entity-tax-create-guided-next"));
    expect(screen.getByTestId("entity-tax-create-guided-error").textContent).toContain("最终审批人");

    fireEvent.change(screen.getByTestId("entity-tax-create-finance_review"), { target: { value: "usr_f" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-tax_review"), { target: { value: "usr_t" } });
    fireEvent.change(screen.getByTestId("entity-tax-create-legal_review"), { target: { value: "usr_l" } });
    fireEvent.click(screen.getByTestId("entity-tax-create-guided-next"));
    await waitFor(() => expect(screen.getByTestId("entity-tax-create-guided-summary").textContent).toContain("文化事业建设费"));
    fireEvent.click(screen.getByTestId("entity-tax-create-save"));

    await waitFor(() => expect(lastPost?.url).toContain("/api/v1/finance/entity-taxes"));
    expect(lastPost?.body).toMatchObject({
      business_entity_id: "be_1",
      tax_type: "cultural_fee",
      period_start_month: "2026-04",
      period_end_month: "2026-06",
      declared_amount_cent: 120050,
      reason: "二季度文化事业建设费申报表",
      parallel_lines: [
        { line_code: "finance_review", final_approver_id: "usr_f" },
        { line_code: "tax_review", final_approver_id: "usr_t" },
        { line_code: "legal_review", final_approver_id: "usr_l" },
      ],
    });
    expect(typeof lastPost?.body.idempotency_key).toBe("string");
    await waitFor(() => expect(screen.queryByTestId("entity-tax-create-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "entity_tax_declared" }));
  });
});
