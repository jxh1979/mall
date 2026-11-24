// 文件版本：V0.05
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { SettlementInvoicePage } from "./SettlementInvoicePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 开票清单（ADM-STL-002）组件测试：按开票主体分组的汇总/明细、税率税额显示、
// 已超开告警（退款后先红冲再重开）、内部单据在规则表里标「不开票」。
const SUMMARY = {
  subjects: [
    { subjectEntityId: "be_plat", subjectName: "娱乐平台公司", itemCount: 1, amountCent: 10000, vatAmountCent: 566, culturalFeeCent: 300 },
    { subjectEntityId: null, subjectName: null, itemCount: 1, amountCent: 17777, vatAmountCent: 1006, culturalFeeCent: 0 },
  ],
  rules: [
    { incomeType: "ticket", revenueStream: "all_pass_ticket", invoiceItemName: "综合体验服务费", taxCategory: "综合体验/娱乐平台服务", evidenceRequired: "购票页截图、用户协议、对账单", invoiceable: true },
    { incomeType: "marketing", revenueStream: null, invoiceItemName: "营销成本", taxCategory: "营销成本", evidenceRequired: "", invoiceable: false },
    { incomeType: "points_issuance", revenueStream: null, invoiceItemName: "积分发行费", taxCategory: "积分发行费", evidenceRequired: "", invoiceable: true },
  ],
  overIssued: [
    { billId: "sb_1", incomeType: "ticket", settlementItemId: null, subjectName: "娱乐平台公司", dueAmountCent: 4000, openAmountCent: 10000, overIssuedCent: 6000 },
  ],
};

const ITEMS = {
  items: [
    {
      id: "sii_1", billId: "sb_1", settlementItemId: null, projectId: "p1",
      periodStart: "2026-05-01", periodEnd: "2026-05-31", incomeType: "ticket",
      subjectEntityId: "be_plat", subjectName: "娱乐平台公司", invoiceItemName: "综合体验服务费",
      taxCategory: "综合体验/娱乐平台服务", evidenceRequired: "购票页截图、用户协议、对账单",
      amountCent: 10000, vatRateBps: 600, vatAmountCent: 566,
      culturalFeeApplicable: true, culturalFeeRateBps: 300, culturalFeeCent: 300,
      status: "issued", invoiceNo: "INV-0001", remark: null,
    },
    {
      id: "sii_2", billId: "sb_1", settlementItemId: "si_b", projectId: "p1",
      periodStart: "2026-05-01", periodEnd: "2026-05-31", incomeType: "spv_service",
      subjectEntityId: null, subjectName: null, invoiceItemName: "运营服务费",
      taxCategory: "运营服务", evidenceRequired: "服务协议、对账单、服务成果确认单",
      amountCent: 17777, vatRateBps: 600, vatAmountCent: 1006,
      culturalFeeApplicable: false, culturalFeeRateBps: 0, culturalFeeCent: 0,
      status: "pending", invoiceNo: null, remark: null,
    },
    {
      // FS-022-R1：积分发行费月账单行——开票主体＝收款方（全国积分结算中心），购方＝合同销售方。
      id: "sii_pts_1", billId: null, pointsBillId: "tpmb_1", settlementItemId: null, projectId: "p1",
      periodStart: "2026-05-01", periodEnd: "2026-05-31", incomeType: "points_issuance",
      subjectEntityId: "be_center", subjectName: "全国积分结算中心", invoiceItemName: "积分发行费",
      taxCategory: "积分发行费", evidenceRequired: "",
      amountCent: 6000, vatRateBps: 0, vatAmountCent: 0,
      culturalFeeApplicable: false, culturalFeeRateBps: 0, culturalFeeCent: 0,
      status: "pending", invoiceNo: null, remark: null, buyerEntityId: "be_seller", buyerName: "北辰酒吧经营公司",
    },
    {
      // 第十八批 AZ 段 290-R1：组合通票零售业态份额行——开票主体＝城市项目公司，购方＝娱乐平台公司。
      id: "sii_retail_1", billId: "sb_1", settlementItemId: null, projectId: "p1",
      periodStart: "2026-05-01", periodEnd: "2026-05-31", incomeType: "retail",
      subjectEntityId: "be_city", subjectName: "长沙城市项目公司", invoiceItemName: "零售商品",
      taxCategory: "零售", evidenceRequired: "对账单",
      amountCent: 8000, vatRateBps: 0, vatAmountCent: 0,
      culturalFeeApplicable: false, culturalFeeRateBps: 0, culturalFeeCent: 0,
      status: "pending", invoiceNo: null, remark: null, buyerEntityId: "be_plat", buyerName: "娱乐平台公司",
    },
  ],
  page: 1, pageSize: 200, total: 4,
};

function makeApi(summary: unknown = SUMMARY, items: unknown = ITEMS): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((path: string) =>
      Promise.resolve(path.includes("/summary") ? summary : items)) as AdminApiClient["get"],
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue(1),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const PERMISSIONS: EffectivePermissionsState = {
  status: "ready",
  value: { grants: ["settlement.invoice.view", "settlement.invoice.manage"], denies: [], wildcard: false },
};

function renderPage(summary: unknown = SUMMARY, items: unknown = ITEMS) {
  const props = {
    api: makeApi(summary, items),
    user: { id: "u1", username: "admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: PERMISSIONS,
  } as unknown as AdminPageProps;
  render(<SettlementInvoicePage {...props} />);
}

describe("开票清单 ADM-STL-002", () => {
  it("按开票主体分组展示金额与税额，归属未配置的不静默丢账", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("settlement-invoice-subject-table")).toBeTruthy());

    const subjectTable = screen.getByTestId("settlement-invoice-subject-table");
    expect(subjectTable.textContent).toContain("娱乐平台公司");
    expect(subjectTable.textContent).toContain("100.00");   // 开票金额（元）
    expect(subjectTable.textContent).toContain("5.66");     // 6% 含税换算税额
    expect(subjectTable.textContent).toContain("3.00");     // 文化事业建设费
    expect(subjectTable.textContent).toContain("归属未配置");
  });

  it("明细带税率与凭证要件，文建费不计的显示「不计」", async () => {
    renderPage();
    await waitFor(() => expect(screen.getAllByTestId("settlement-invoice-item-table").length).toBeGreaterThan(0));

    const detail = screen.getAllByTestId("settlement-invoice-item-table")
      .map((t) => t.textContent ?? "").join("");
    expect(detail).toContain("综合体验服务费");
    expect(detail).toContain("6.00%");
    expect(detail).toContain("购票页截图、用户协议、对账单");
    expect(detail).toContain("已开票");
    expect(detail).toContain("运营服务费");
    expect(detail).toContain("不计");
  });

  it("积分发行费行显示收入类型与购方（合同销售方）", async () => {
    renderPage();
    const detail = () => screen.queryAllByTestId("settlement-invoice-item-table").map((t) => t.textContent ?? "").join("");
    await waitFor(() => expect(detail()).toContain("北辰酒吧经营公司"));
    expect(detail()).toContain("积分发行费");
    expect(detail()).not.toContain("points_issuance");
    const rules = screen.getByTestId("settlement-invoice-rule-table");
    expect(rules.textContent).toContain("积分月账单收款方");
  });

  it("组合通票零售业态份额行的收入类型显示「零售」", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryAllByText("零售").length).toBeGreaterThan(0));
    const detail = screen.getAllByTestId("settlement-invoice-item-table").map((t) => t.textContent ?? "").join("");
    expect(detail).toContain("零售商品");
    expect(detail).not.toContain("retail");
  });

  it("已超开时提示先红冲再重开", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("settlement-invoice-over-issued")).toBeTruthy());
    const alert = screen.getByTestId("settlement-invoice-over-issued");
    expect(alert.textContent).toContain("已超开，需先红冲再重开");
    expect(alert.textContent).toContain("应开 40.00 元");
    expect(alert.textContent).toContain("已开 100.00 元");
    expect(alert.textContent).toContain("超出 60.00 元");
  });

  // 18AZ 收尾复核（控制席补充 b）：同月两条传媒演出结算行超开（结算单号为空、收入类型同为 show），每行 key 唯一，两行都显示、不报重复 key。
  it("同月两条演出结算行超开时都显示、不报重复 key", async () => {
    const errors = vi.spyOn(console, "error").mockImplementation(() => undefined);
    renderPage({ ...SUMMARY, overIssued: [
      { billId: null, incomeType: "show", settlementItemId: null, subjectName: "演出传媒公司", dueAmountCent: 0, openAmountCent: 3000, overIssuedCent: 3000 },
      { billId: null, incomeType: "show", settlementItemId: null, subjectName: "演出传媒公司", dueAmountCent: 0, openAmountCent: 50000, overIssuedCent: 50000 },
    ] });
    await waitFor(() => expect(screen.getByTestId("settlement-invoice-over-issued").querySelectorAll("li").length).toBe(2));
    expect(errors.mock.calls.flat().join(" ")).not.toContain("same key");
    errors.mockRestore();
  });

  it("规则表把营销成本标为不开票", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("settlement-invoice-rule-table")).toBeTruthy());
    const rules = screen.getByTestId("settlement-invoice-rule-table");
    expect(rules.textContent).toContain("营销成本");
    expect(rules.textContent).toContain("不开票（内部单据）");
  });

  // 第十八批 CD 段复核：V760 停用通票赠送权益兑付「不开票」后，固定说明只写营销成本一类。
  it("固定说明不再写通票赠送权益兑付不开票", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("settlement-invoice-rule-table")).toBeTruthy());
    expect(document.body.textContent).not.toContain("通票赠送权益兑付是娱乐平台公司与提供商户之间的内部成本转付");
    expect(document.body.textContent).toContain("会员积分／门票折扣券是营销成本，不进对外开票清单");
  });

  // 18AZ 收尾复核（控制席补充 c；SettlementInvoiceService.generateForBill 第 ③ 步：开票主体＝该业态份额归属主体，购方＝娱乐平台公司）：
  // 收入流为空的零售规则不再显示「按该笔分账的商户收款主体」。
  it("零售规则的开票主体来源显示组合通票业态份额的归属主体", async () => {
    renderPage({ ...SUMMARY, rules: [{ incomeType: "retail", revenueStream: null, invoiceItemName: "零售商品", taxCategory: "零售",
      evidenceRequired: "对账单", invoiceable: true }] });
    await waitFor(() => expect(screen.getByTestId("settlement-invoice-rule-table").textContent).toContain("组合通票零售份额的归属主体"));
    expect(screen.getByTestId("settlement-invoice-rule-table").textContent).not.toContain("按该笔分账的商户收款主体");
  });

  // 第十八批 BU 段 374-R7（PGM:287「界面全中文」；BSM:522「可追溯到来源单据」；BSM:1803「平台内部各主体之间的往来需要开具发票」）：台账来源月度组行
  // 显示来源中文名与新增收入类型中文名（同主体同购方同月的提成拨付与报酬拨付靠「来源」分辨），规则表写清人员报酬拨付的开票主体与购方。
  it("台账来源的开票行显示来源与中文收入类型", async () => {
    const line = { billId: null, settlementItemId: null, projectId: "p1", periodStart: "2026-06-01", periodEnd: "2026-06-30",
      evidenceRequired: "对账单", vatRateBps: 0, vatAmountCent: 0, culturalFeeApplicable: false, culturalFeeRateBps: 0, culturalFeeCent: 0,
      status: "pending", invoiceNo: null, remark: null };
    const items = { items: [
      { ...line, id: "sii_lg_1", incomeType: "staff_remuneration", subjectEntityId: "be_staff", subjectName: "派驻人员所属公司",
        invoiceItemName: "派驻提成开票项目", taxCategory: "人员报酬拨付", amountCent: 42000, buyerEntityId: "be_bar", buyerName: "北辰酒吧经营公司",
        ledgerSource: "dispatch_commission_transfer" },
      { ...line, id: "sii_lg_2", incomeType: "goods", subjectEntityId: "be_supply", subjectName: "中央供货公司",
        invoiceItemName: "要货开票项目", taxCategory: "供货货款", amountCent: 16000, buyerEntityId: "be_store", buyerName: "要货门店经营公司",
        ledgerSource: "inventory_transfer" },
    ], page: 1, pageSize: 200, total: 2 };
    renderPage({ ...SUMMARY, rules: [{ incomeType: "staff_remuneration", revenueStream: null, invoiceItemName: "人员报酬拨付",
      taxCategory: "人员报酬拨付", evidenceRequired: "对账单", invoiceable: true }] }, items);
    const detail = () => screen.queryAllByTestId("settlement-invoice-item-table").map((t) => t.textContent ?? "").join("");
    await waitFor(() => expect(detail()).toContain("派驻提成月结拨付"));
    expect(detail()).toContain("人员报酬拨付");
    expect(detail()).toContain("门店要货货款");
    expect(detail()).toContain("供货货款");
    expect(detail()).not.toMatch(/staff_remuneration|dispatch_commission_transfer|inventory_transfer|goods/);
    expect(screen.getByTestId("settlement-invoice-rule-table").textContent).toContain("人员所属主体（购方为拨付酒吧）");
  });

  // 第十八批 CD 段第 402 条 402-R3（PGM:287「界面全中文」；BSM:1201「商户之间也可借调货物」）：商户之间借调买断货款与借用物赔偿行的来源显示中文名。
  it("商户借调货款与赔偿行显示中文来源", async () => {
    const items = { items: [
      { id: "sii_lg_lnd", billId: null, settlementItemId: null, projectId: "p1", periodStart: "2026-06-01", periodEnd: "2026-06-30",
        incomeType: "goods", subjectEntityId: "be_lender", subjectName: "借出酒吧经营公司", invoiceItemName: "借调开票项目",
        taxCategory: "借调税目", evidenceRequired: "对账单", amountCent: 3500, vatRateBps: 0, vatAmountCent: 0,
        culturalFeeApplicable: false, culturalFeeRateBps: 0, culturalFeeCent: 0, status: "pending", invoiceNo: null, remark: null,
        buyerEntityId: "be_borrower", buyerName: "借入酒吧经营公司", ledgerSource: "inventory_lending" },
    ], page: 1, pageSize: 200, total: 1 };
    renderPage(SUMMARY, items);
    const detail = () => screen.queryAllByTestId("settlement-invoice-item-table").map((t) => t.textContent ?? "").join("");
    await waitFor(() => expect(detail()).toContain("供货货款"));
    expect(detail()).toContain("商户借调货款与赔偿");
    expect(detail()).not.toMatch(/inventory_lending/);
  });
});
