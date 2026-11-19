// 文件版本：V0.03
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
    { incomeType: "giveaway_transfer", revenueStream: null, invoiceItemName: "内部领用单/成本转付单", taxCategory: "内部成本转付", evidenceRequired: "核销记录、内部结算价快照", invoiceable: false },
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

function makeApi(summary: unknown = SUMMARY): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((path: string) =>
      Promise.resolve(path.includes("/summary") ? summary : ITEMS)) as AdminApiClient["get"],
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

function renderPage(summary: unknown = SUMMARY) {
  const props = {
    api: makeApi(summary),
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

  it("规则表把内部成本转付标为不开票", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("settlement-invoice-rule-table")).toBeTruthy());
    const rules = screen.getByTestId("settlement-invoice-rule-table");
    expect(rules.textContent).toContain("内部领用单/成本转付单");
    expect(rules.textContent).toContain("不开票（内部单据）");
  });

  // 18AZ 收尾复核（控制席补充 c；SettlementInvoiceService.generateForBill 第 ③ 步：开票主体＝该业态份额归属主体，购方＝娱乐平台公司）：
  // 收入流为空的零售规则不再显示「按该笔分账的商户收款主体」。
  it("零售规则的开票主体来源显示组合通票业态份额的归属主体", async () => {
    renderPage({ ...SUMMARY, rules: [{ incomeType: "retail", revenueStream: null, invoiceItemName: "零售商品", taxCategory: "零售",
      evidenceRequired: "对账单", invoiceable: true }] });
    await waitFor(() => expect(screen.getByTestId("settlement-invoice-rule-table").textContent).toContain("组合通票零售份额的归属主体"));
    expect(screen.getByTestId("settlement-invoice-rule-table").textContent).not.toContain("按该笔分账的商户收款主体");
  });
});
