// 文件版本：V0.15
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinanceArApLedgerPage } from "./FinanceArApLedgerPage";
import { formatActionResult } from "../components/ActionResultView";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, FinanceLedgerRow } from "../types";

const ROWS: FinanceLedgerRow[] = [
  { id: "ar-1", itemNo: "AR-0001", direction: "receivable", status: "open", amountCent: 10000, unpaidAmountCent: 10000, dueDate: "2026-08-01" },
];

// 服务端分页 mock：默认 total=23（>10，可翻页），可按 overrides 定制。
function pagedResolver() {
  return vi.fn().mockResolvedValue({ items: ROWS, total: 23, page: 1, size: 10 });
}

function makeApi(getPaged = pagedResolver()): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([]),
    getPaged,
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient, setActionResult: AdminPageProps["setActionResult"] = vi.fn()) {
  const fullProps: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<FinanceArApLedgerPage {...fullProps} />);
}

describe("FinanceArApLedgerPage 服务端分页", () => {
  it("首次渲染调用 getPaged 并用服务端 total 显示计数", async () => {
    const getPaged = pagedResolver();
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalled());
    // 计数用服务端 total（23），不是当前页行数（1）。
    expect(screen.getByTestId("finance-ledger-filter-count").textContent).toContain("23");
    expect(getPaged).toHaveBeenCalledWith(expect.stringContaining("page=1"));
  });

  it("输关键词点查询触发带 keyword 的重新请求（即时搜索降级）", async () => {
    const getPaged = pagedResolver();
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalled());
    fireEvent.change(screen.getByTestId("finance-ledger-search"), { target: { value: "AR-0001" } });
    // 打字本身不发请求（降级）：仍只有首次那一发。
    expect(getPaged).toHaveBeenCalledTimes(1);
    fireEvent.click(screen.getByTestId("finance-ledger-search-submit"));
    await waitFor(() => expect(getPaged).toHaveBeenCalledWith(expect.stringContaining("keyword=AR-0001")));
  });

  it("翻页调用 getPaged 带 page=2", async () => {
    const getPaged = pagedResolver();
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("finance-ledger-pager-next"));
    await waitFor(() => expect(getPaged).toHaveBeenCalledWith(expect.stringContaining("page=2")));
  });
});

describe("FinanceArApLedgerPage 来源类型（退租相关可辨识）", () => {
  it("台账详情展示来源类型中文标签（退租押金退还）", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ar-dr", itemNo: "AR-DR-1", direction: "payable", status: "pending_payment", amountCent: 200000, unpaidAmountCent: 200000, dueDate: "2026-08-01", sourceType: "deposit_refund" },
    ];
    const getPaged = vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 });
    const api = makeApi(getPaged);
    // 第十八批 AV 段 310-R1：押金退还应付选中即带出内部应付执行面板（同下方执行状态用例的假响应形状）。
    (api.get as ReturnType<typeof vi.fn>).mockImplementation((path: string) =>
      Promise.resolve(path.includes("/internal-payables/") ? { executionStatus: "pending", inTransit: false, attempts: [], postponements: [] } : []));
    renderPage(api);
    await waitFor(() => expect(getPaged).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ar-dr"));
    await waitFor(() => expect(screen.getByText("来源类型")).toBeTruthy());
    expect(screen.getByText("退租押金退还")).toBeTruthy();
  });

  // HP-C03-3：面积/位置变更与续租的押金补退差额（source_type=deposit_adjustment）在台账显中文来源。
  it("台账详情展示来源类型中文标签（变更押金补退差额）", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ar-adj", itemNo: "ARAP-ADJ-1", direction: "receivable", status: "pending_collection", amountCent: 150000, unpaidAmountCent: 150000, dueDate: "2026-09-30", sourceType: "deposit_adjustment" },
    ];
    const getPaged = vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 });
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ar-adj"));
    await waitFor(() => expect(screen.getByText("来源类型")).toBeTruthy());
    expect(screen.getByText("变更押金补退差额")).toBeTruthy();
  });

  // FS-013：单一酒吧门票综合服务费按计费月单独挂酒吧应收（source_type=single_bar_service_fee）在台账显中文来源。
  it("台账详情展示来源类型中文标签（单店门票综合服务费）", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ar-sbf", itemNo: "ARAP-SBF-1", direction: "receivable", status: "pending_collection", amountCent: 10000, unpaidAmountCent: 10000, dueDate: "2026-11-18", sourceType: "single_bar_service_fee" },
    ];
    const getPaged = vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 });
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ar-sbf"));
    await waitFor(() => expect(screen.getByText("单店门票综合服务费")).toBeTruthy());
  });

  // 第十八批 AL 段第 223 条 223-R1（BSM:1751「高价值差异复核通过才入账」）：对账差异复核通过后入账的应收应付（source_type=reconciliation_difference）显中文来源。
  it("台账详情展示来源类型中文标签（对账差异）", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ar-rd", itemNo: "ARAP-RD-1", direction: "receivable", status: "pending_collection", amountCent: 1000, unpaidAmountCent: 1000, dueDate: "2026-11-16", sourceType: "reconciliation_difference" },
    ];
    renderPage(makeApi(vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 })));
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-row-ar-rd")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ar-rd"));
    await waitFor(() => expect(screen.queryByText("对账差异")).not.toBeNull());
  });
});


describe("经销应收待合同事件", () => {
  it("未发生起算事件仍展示未收货款和明确的待日期说明", async () => {
    const rows = [{ id: "ar-dealer", itemNo: "AR-DEALER", sourceType: "dealer_order", direction: "receivable", status: "open", amountCent: 10000, unpaidAmountCent: 7000 }];
    renderPage(makeApi(vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 })));
    await screen.findByTestId("finance-ledger-row-ar-dealer");
    expect(screen.getByText("待合同约定事件确定")).toBeTruthy();
    expect(screen.getByTestId("finance-ledger-row-ar-dealer").textContent).toContain("70.00");
  });
});

// FS-021-R1：负额结算明细过账为单独追收应收（source_type=ticket_distribution_recovery），台账显中文来源。
describe("FinanceArApLedgerPage 通票分账追收", () => {
  it("台账详情展示来源类型中文标签（通票分账追收）", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ar-tdr", itemNo: "ARAP-TDR-1", direction: "receivable", status: "pending_collection", amountCent: 3000, unpaidAmountCent: 3000, dueDate: "2026-11-05", sourceType: "ticket_distribution_recovery" },
    ];
    renderPage(makeApi(vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 })));
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-row-ar-tdr")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ar-tdr"));
    await waitFor(() => expect(screen.queryByText("通票分账追收")).not.toBeNull());
  });
});

// 操作员裁决 2026-10-19 第二轮第 5 条「已付部分另挂追收」：补偿应付已付部分在该次核销撤销后另挂对提供方的追收应收，台账显中文来源。
describe("FinanceArApLedgerPage 补偿应付已付追收", () => {
  it("台账详情展示来源类型中文标签（补偿应付已付追收）", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ar-cpr", itemNo: "ARAP-CPR-1", direction: "receivable", status: "pending_collection", amountCent: 500, unpaidAmountCent: 500, dueDate: "2026-12-05", sourceType: "compensation_fulfillment_recovery" },
    ];
    renderPage(makeApi(vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 })));
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-row-ar-cpr")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ar-cpr"));
    await waitFor(() => expect(screen.queryByText("补偿应付已付追收")).not.toBeNull());
  });
});

// 第十八批 R 段第 142 条 142-R1（BSM:1081「长款当期计入该商户营业外收入并单独留痕可查」）：长款复核平账写的内部核算台账显中文来源。
describe("FinanceArApLedgerPage 收银长款", () => {
  it("台账详情展示来源类型中文标签（收银长款（营业外收入））", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ia-ov", itemNo: "ARAP-OV-1", direction: "internal_accounting", status: "settled", amountCent: 100, unpaidAmountCent: 0, dueDate: "2026-11-04", sourceType: "bar_shift_overage" },
    ];
    renderPage(makeApi(vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 })));
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-row-ia-ov")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ia-ov"));
    await waitFor(() => expect(screen.queryByText("收银长款（营业外收入）")).not.toBeNull());
  });
});

// 第十八批 U 段第 155 条 155-R2（PGM:287「界面全中文（所有终端零英文）」）：跨期退款按原账期档位费率冲回的营业额抽成冲正应付，台账显中文来源。
describe("FinanceArApLedgerPage 营业额抽成冲正", () => {
  it("台账详情展示来源类型中文标签（营业额抽成冲正）", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ap-trr", itemNo: "ARAP-TRR-1", direction: "payable", status: "open", amountCent: 400, unpaidAmountCent: 400, dueDate: "2026-09-10", sourceType: "turnover_rent_reversal" },
    ];
    renderPage(makeApi(vi.fn().mockResolvedValue({ items: rows, total: 1, page: 1, size: 10 })));
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-row-ap-trr")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-ledger-row-ap-trr"));
    await waitFor(() => expect(screen.queryByText("营业额抽成冲正")).not.toBeNull());
  });
});

// 第十八批 AI 段 214-R1、214-R2（ENUM:32「其余各域份额自收款之日起即为应付给对应域主管公司的款项」；BSM:755「不得轧差」；ENUM:49「其一，到期未核销票款」）：
// 组合通票各域份额的主体间往来在台账显中文来源，与后端 AR_AP_SOURCE_TYPE_LABELS 同名。
describe("FinanceArApLedgerPage 通票域间往来来源", () => {
  it("台账详情展示通票域间份额、通票餐饮零售代付、通票到期未核销归属", async () => {
    const rows: FinanceLedgerRow[] = [
      { id: "ap-pds", itemNo: "ARAP-PDS-1", direction: "payable", status: "pending_payment", amountCent: 91000, unpaidAmountCent: 91000, dueDate: "2026-12-05", sourceType: "pass_domain_share" },
      { id: "ar-pdd", itemNo: "ARAP-PDD-1", direction: "receivable", status: "pending_collection", amountCent: 200000, unpaidAmountCent: 200000, dueDate: "2026-12-05", sourceType: "pass_domain_direct_settlement" },
      { id: "ap-pua", itemNo: "ARAP-PUA-1", direction: "payable", status: "pending_payment", amountCent: 182000, unpaidAmountCent: 182000, dueDate: "2026-12-05", sourceType: "pass_unverified_attribution" },
    ];
    const api = makeApi(vi.fn().mockResolvedValue({ items: rows, total: 3, page: 1, size: 10 }));
    // 选中应付行会带出内部应付执行面板（同下方执行状态用例的假响应形状）。
    (api.get as ReturnType<typeof vi.fn>).mockImplementation((path: string) =>
      Promise.resolve(path.includes("/internal-payables/") ? { executionStatus: "pending", inTransit: false, attempts: [], postponements: [] } : []));
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-row-ap-pua")).not.toBeNull());
    for (const [id, label] of [["ap-pds", "通票域间份额"], ["ar-pdd", "通票餐饮零售代付"], ["ap-pua", "通票到期未核销归属"]] as const) {
      fireEvent.click(screen.getByTestId(`finance-ledger-row-${id}`));
      await waitFor(() => expect(screen.queryByText(label)).not.toBeNull());
    }
  });
});

// 第十八批 AV 段第 317 条 317-R1（PGM:287「界面全中文（所有终端零英文）」）：台账来源中文名按后端 AR_AP_SOURCE_TYPE_LABELS 同名补齐，
// 详情抽屉不再显示原码。
describe("FinanceArApLedgerPage 来源中文名与后端同名补齐", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("分红留存已释放、工单用料费（商户承担）、存酒补存/赔付损失、手工录入显中文，不显原码", async () => {
    const row = (id: string, direction: string, sourceType: string, status: string) => ({ id, item_no: `ARAP-${id}`, direction,
      source_type: sourceType, status, amount_cent: 1000, paid_amount_cent: 0, unpaid_amount_cent: 1000, due_date: "2026-12-05" });
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      let data: unknown = {};
      if (url.includes("/api/v1/finance/ar-ap-items?")) {
        data = { items: [row("ap-drr", "payable", "dividend_reserve_released", "pending_payment"),
          row("ar-wom", "receivable", "work_order_material", "pending_collection"),
          row("ia-sbr", "internal_accounting", "saved_bottle_remedy", "settled"),
          row("ar-man", "receivable", "manual", "pending_collection"),
          // 第十八批 AV 段收尾复核：正常业务写进台账、此前前后端都缺的五个来源（楼宇设备报废残值与损失、通用卡充值与票务订单的内部核算行、服务点单收款）。
          row("ar-fes", "receivable", "facility_equipment_scrap", "pending_collection"),
          row("ia-fesl", "internal_accounting", "facility_equipment_scrap_loss", "settled"),
          row("ia-svt", "internal_accounting", "stored_value_topup", "settled"),
          row("ia-tko", "internal_accounting", "ticket_order", "settled"),
          row("ar-svo", "receivable", "service_order", "paid"),
          // 第十八批 AX 段 273-R1（PGM:436）：公司向酒吧借款登记后挂的酒吧应付，变基后中文名由统一字段值表 source_type 提供。
          row("ap-bl", "payable", "bar_loan", "pending_payment")], total: 10, page: 1, page_size: 10 };
      } else if (url.includes("/api/v1/finance/internal-payables/")) {
        data = { ar_ap_item_id: "ap-drr", direction: "payable", source_type: "dividend_reserve_released", amount_cent: 1000, unpaid_amount_cent: 1000,
          execution_status: "pending", in_transit: false, attempts: [], postponements: [] };
      }
      return new Response(JSON.stringify({ success: true, code: "OK", data }));
    }));
    renderPage(new AdminApi("test"));
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-row-ar-man")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("finance-ledger-filter-count").textContent).toContain("10"));
    for (const [id, code, label] of [["ap-drr", "dividend_reserve_released", "分红留存已释放"],
      ["ar-wom", "work_order_material", "工单用料费（商户承担）"], ["ia-sbr", "saved_bottle_remedy", "存酒补存/赔付损失"],
      ["ar-man", "manual", "手工录入"], ["ar-fes", "facility_equipment_scrap", "楼宇设备报废残值"],
      ["ia-fesl", "facility_equipment_scrap_loss", "楼宇设备报废损失"], ["ia-svt", "stored_value_topup", "通用卡储值充值"],
      ["ia-tko", "ticket_order", "票务订单"], ["ar-svo", "service_order", "服务点单"], ["ap-bl", "bar_loan", "借款还款"]] as const) {
      fireEvent.click(screen.getByTestId(`finance-ledger-row-${id}`));
      await waitFor(() => expect(screen.queryByText(label)).not.toBeNull());
      expect(screen.queryByText(code)).toBeNull();
    }
  });
});

// FS-021-R5（BSM L1683；ENUM L253）：选中普通内部结算应付 → 显示执行状态与付款时点，可登记已发起付款（在途）。
describe("FinanceArApLedgerPage 内部应付执行状态", () => {
  const PAYABLE: FinanceLedgerRow = { id: "ap-td", itemNo: "ARAP-TD-1", direction: "payable", status: "pending_payment", amountCent: 5000, unpaidAmountCent: 5000, dueDate: "2026-11-25", sourceType: "ticket_distribution" };
  const EXECUTION = {
    arApItemId: "ap-td", direction: "payable", sourceType: "ticket_distribution", counterpartyId: "bar-1", projectId: "p1",
    amountCent: 5000, unpaidAmountCent: 5000, visibleAt: "2026-11-01T00:00:00Z", objectionDeadline: "2026-11-13",
    designatedPayDay: "2026-11-25", dueDate: "2026-11-25", executionStatus: "failed_pending", inTransit: false,
    failureReason: "收款账户被冻结", expectedRepayDate: "2026-12-05",
    attempts: [{ id: "ippa-1", attemptNo: 1, amountCent: 5000, status: "failed" }], postponements: [],
  };

  it("显示付款失败待处理、原因与预计重付日，发起付款请求带银行流水号", async () => {
    const api = makeApi(vi.fn().mockResolvedValue({ items: [PAYABLE], total: 1, page: 1, size: 10 }));
    (api.get as ReturnType<typeof vi.fn>).mockImplementation((path: string) =>
      Promise.resolve(path.includes("/internal-payables/") ? EXECUTION : []));
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-select-ap-td")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-ledger-select-ap-td"));
    await waitFor(() => expect(screen.queryByTestId("internal-payable-status")?.textContent ?? "").toContain("付款失败待处理"));
    const panel = screen.getByTestId("internal-payable-panel").textContent ?? "";
    expect(panel).toContain("收款账户被冻结");
    expect(panel).toContain("2026年12月5日");
    expect(panel).toContain("2026年11月13日");
    fireEvent.change(screen.getByTestId("internal-payable-bank-ref"), { target: { value: "BANK-2" } });
    fireEvent.click(screen.getByTestId("internal-payable-initiate"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/finance/internal-payables/ap-td/payment-attempts",
      expect.objectContaining({ bankRef: "BANK-2" })));
  });

  // 补偿新增履约结算（FS-012，钱路 compensation_fulfillment）同为普通内部结算应付：选中即出执行面板，可登记在途/失败/顺延。
  it("补偿应付（compensation_fulfillment）同样显示执行面板", async () => {
    const row: FinanceLedgerRow = { ...PAYABLE, id: "ap-cf", itemNo: "ARAP-CF-1", sourceType: "compensation_fulfillment" };
    const api = makeApi(vi.fn().mockResolvedValue({ items: [row], total: 1, page: 1, size: 10 }));
    (api.get as ReturnType<typeof vi.fn>).mockImplementation((path: string) =>
      Promise.resolve(path.includes("/internal-payables/") ? { ...EXECUTION, arApItemId: "ap-cf", sourceType: "compensation_fulfillment" } : []));
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-select-ap-cf")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-ledger-select-ap-cf"));
    await waitFor(() => expect(screen.queryByTestId("internal-payable-panel")).not.toBeNull());
  });

  // 第十八批 AI 段 214-R1：娱乐平台公司对域主管公司的组合通票域间份额应付同为普通内部结算应付，选中即出执行面板。
  it("通票域间份额应付（pass_domain_share）同样显示执行面板", async () => {
    const row: FinanceLedgerRow = { ...PAYABLE, id: "ap-pds", itemNo: "ARAP-PDS-1", sourceType: "pass_domain_share" };
    const api = makeApi(vi.fn().mockResolvedValue({ items: [row], total: 1, page: 1, size: 10 }));
    (api.get as ReturnType<typeof vi.fn>).mockImplementation((path: string) =>
      Promise.resolve(path.includes("/internal-payables/") ? { ...EXECUTION, arApItemId: "ap-pds", sourceType: "pass_domain_share" } : []));
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-select-ap-pds")).not.toBeNull());
    fireEvent.click(screen.getByTestId("finance-ledger-select-ap-pds"));
    await waitFor(() => expect(screen.queryByTestId("internal-payable-panel")).not.toBeNull());
  });
});

// 第十八批 AV 段 310-R1（ENUM:252「押金退还付款状态按「内部应付执行状态」条执行」「押金退还支付不受三个付账日与账单异议期约束」）：
// 退租押金退还应付同样出付款执行面板（登记已发起、明确失败），但不适用资金不足顺延至下一个付账日。
describe("FinanceArApLedgerPage 退租押金退还付款执行", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("选中退租押金退还应付显示执行面板与登记已发起按钮，没有资金不足顺延按钮", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      let data: unknown = {};
      if (url.includes("/api/v1/finance/ar-ap-items?")) {
        data = { items: [{ id: "ap-dr", item_no: "ARAP-DR-1", direction: "payable", source_type: "deposit_refund", status: "pending_payment",
          amount_cent: 200000, paid_amount_cent: 0, unpaid_amount_cent: 200000, due_date: "2026-11-18" }], total: 1, page: 1, page_size: 10 };
      } else if (url.includes("/api/v1/finance/internal-payables/ap-dr")) {
        data = { ar_ap_item_id: "ap-dr", direction: "payable", source_type: "deposit_refund", amount_cent: 200000, unpaid_amount_cent: 200000,
          visible_at: "2026-11-18T01:00:00Z", objection_deadline: null, designated_pay_day: null, due_date: "2026-11-18",
          execution_status: "pending", in_transit: false, failure_reason: null, expected_repay_date: null, attempts: [], postponements: [] };
      }
      return new Response(JSON.stringify({ success: true, code: "OK", data }));
    }));
    renderPage(new AdminApi("test"));
    await waitFor(() => expect(screen.queryByTestId("internal-payable-status")?.textContent ?? "").toContain("待付"));
    expect(screen.queryByTestId("internal-payable-initiate")).not.toBeNull();
    expect(screen.getByTestId("internal-payable-panel").textContent).toContain("2026年11月18日");
    expect(screen.queryByTestId("internal-payable-funds-insufficient")).toBeNull();
  });
});

// 第十八批 W 段第 200 条 200-R1（契约 ArApPaymentRequest 必填 amount_cent、payment_method）：「登记核销」按契约发金额（分）与收付方式。
describe("FinanceArApLedgerPage 登记核销请求体", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  function setupRealApi(setActionResult?: AdminPageProps["setActionResult"], paymentData?: unknown) {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      let data: unknown = {};
      if (init?.method === "POST") {
        posts.push({ url, body: JSON.parse(String(init.body)) as Record<string, unknown> });
        data = paymentData ?? { id: "pay-1", ar_ap_item_id: "ar-1", amount_cent: 10000, payment_method: "bank_transfer" };
      } else if (url.includes("/api/v1/finance/ar-ap-items?")) {
        data = { items: [{ id: "ar-1", item_no: "AR-0001", direction: "receivable", status: "open", amount_cent: 20000,
          paid_amount_cent: 0, unpaid_amount_cent: 20000, due_date: "2026-08-01" }], total: 1, page: 1, page_size: 10 };
      }
      return new Response(JSON.stringify({ success: true, code: "OK", data }));
    }));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPage(new AdminApi("test"), setActionResult);
    return posts;
  }

  async function pageSettled() {
    await waitFor(() => {
      expect((screen.queryByTestId("finance-ledger-select-ar-1") as HTMLInputElement | null)?.checked).toBe(true);
      expect(screen.queryByTestId("finance-payment-method")).not.toBeNull();
    });
  }

  // 第 198 条 198-R4②：收付款登记是系统自动留痕的「支付、核销」（BSM:1631），不是调账；页面没有原因框，请求体不带 operator_note。
  it("填 100.00、选银行转账并确认：请求体为 amount_cent、payment_method、idempotency_key，不含 operator_note、amount；页面没有原因框", async () => {
    const posts = setupRealApi();
    await pageSettled();
    expect(screen.queryByTestId("finance-payment-reason")).toBeNull();
    fireEvent.change(screen.getByTestId("finance-payment-amount"), { target: { value: "100.00" } });
    fireEvent.change(screen.getByTestId("finance-payment-method"), { target: { value: "bank_transfer" } });
    fireEvent.click(screen.getByTestId("finance-payment-submit"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/finance/ar-ap-items/ar-1/payments");
    expect(posts[0]!.body).toMatchObject({ amount_cent: 10000, payment_method: "bank_transfer" });
    expect(posts[0]!.body).not.toHaveProperty("operator_note");
    expect(typeof posts[0]!.body.idempotency_key).toBe("string");
    expect(posts[0]!.body).not.toHaveProperty("amount");
    expect(posts[0]!.body).not.toHaveProperty("reason");
  });

  it("没选收付方式时不发请求并提示「请选择收付方式。」", async () => {
    const posts = setupRealApi();
    await pageSettled();
    fireEvent.change(screen.getByTestId("finance-payment-amount"), { target: { value: "100.00" } });
    fireEvent.click(screen.getByTestId("finance-payment-submit"));
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-error")?.textContent).toBe("请选择收付方式。"));
    expect(posts).toHaveLength(0);
  });

  // 18W 复核修复（PGM:287「枚举取值……必须全部为中文」）：登记核销成功后，结果区的收付方式与来源类型显示中文名，不显示原码。
  it("登记核销成功后结果区收付方式、来源类型显示中文：不含 bank_transfer、POS刷卡 与来源原码", async () => {
    const setActionResult = vi.fn();
    setupRealApi(setActionResult, {
      payment: { id: "pay-1", ar_ap_item_id: "ar-1", amount_cent: 10000, payment_method: "bank_transfer" },
      item: { id: "ar-1", item_no: "AR-0001", direction: "receivable", source_type: "contract_rent", amount_cent: 20000 },
    });
    await pageSettled();
    fireEvent.change(screen.getByTestId("finance-payment-amount"), { target: { value: "100.00" } });
    fireEvent.change(screen.getByTestId("finance-payment-method"), { target: { value: "bank_transfer" } });
    fireEvent.click(screen.getByTestId("finance-payment-submit"));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "ledger_payment", status: "partial" })));
    const text = formatActionResult(setActionResult.mock.calls.at(-1)![0]);
    expect(text).toContain("银行转账");
    expect(text).toContain("合同租金");
    expect(text).not.toContain("bank_transfer");
    expect(text).not.toContain("POS刷卡");
    expect(text).not.toContain("contract_rent");
  });
});

// 第十八批 AM 段第 241 条 241-R2、241-R9（BSM:1803 付款确认；第十八批裁决 68「同收款方、同类型、同综合体可勾选合发；工资、退款、赔付按单发起」、
// 69「付款方是平台主体的应收，登记收款前也须付款确认」）：台账勾选未结清款项合并发起付款确认。
describe("FinanceArApLedgerPage 付款确认勾选合发", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  const item = (id: string, direction: string, counterparty: string, source: string, unpaid: number, status = "pending_payment") => ({
    id, item_no: id.toUpperCase(), direction, counterparty_id: counterparty, source_type: source, status,
    amount_cent: 20000, paid_amount_cent: 20000 - unpaid, unpaid_amount_cent: unpaid, due_date: "2026-11-25" });
  const ITEMS = [
    item("ap-a", "payable", "sup-1", "purchase_inbound", 10000),
    item("ap-b", "payable", "sup-1", "purchase_inbound", 20000),
    item("ap-c", "payable", "sup-2", "purchase_inbound", 20000),
    item("ap-d", "payable", "sup-1", "purchase_prepayment", 20000),
    item("ar-p", "receivable", "bar-1", "pass_domain_share", 5000, "pending_collection"),
    item("ar-x", "receivable", "sup-1", "contract_rent", 20000, "pending_collection"),
    item("ap-r", "payable", "sup-1", "purchase_inbound", 20000, "reversed"),
    item("ap-w", "payable", "usr-owner", "dividend_reserve_withheld", 20000),
    item("ap-s", "payable", "sup-1", "purchase_inbound", 0, "settled"),
    item("ap-p", "payable", "usr-1", "staff_payroll", 20000),
    item("ap-q", "payable", "bar-1", "pass_domain_share", 8000),
  ];

  function mountLedger(paymentConflict = false) {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST") {
        posts.push({ url, body: JSON.parse(String(init.body)) as Record<string, unknown> });
        if (paymentConflict && url.endsWith("/payments")) {
          return new Response(JSON.stringify({ success: false, code: "CONFLICT", detail_code: "PAYMENT_CONFIRMATION_REQUIRED",
            message: "该笔付款须先完成付款确认（主线与财务、法务等各条平行线全部审批通过）" }), { status: 409 });
        }
        return new Response(JSON.stringify({ success: true, code: "OK", data: { confirmation_id: "pc_1", status: "pending_review" } }));
      }
      let data: unknown = {};
      if (url.includes("/api/v1/finance/ar-ap-items?")) data = { items: ITEMS, total: ITEMS.length, page: 1, page_size: 10 };
      else if (url.includes("/api/v1/finance/payment-confirmations?")) {
        data = { payment_type: "purchase_inbound", payment_type_label: "采购入库", required_line_codes: ["finance_review", "legal_review"], confirmations: [] };
      }
      return new Response(JSON.stringify({ success: true, code: "OK", data }));
    }));
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPage(new AdminApi("test"));
    return posts;
  }

  // 首行单选回落选中、勾选列已渲染，页面被动 effect 落定后再点。
  async function settled() {
    await waitFor(() => {
      expect((screen.queryByTestId("finance-ledger-select-ap-a") as HTMLInputElement | null)?.checked).toBe(true);
      expect(screen.queryByTestId("finance-ledger-confirm-ap-a")).not.toBeNull();
    });
  }
  const box = (id: string) => screen.getByTestId(`finance-ledger-confirm-${id}`) as HTMLInputElement;

  it("未结清应付与平台主体付款的应收可勾选；其他应收、冲正、留存待释放、已结清、工资行不可勾选；勾了一行后收款方或来源不同的行不可再勾", async () => {
    mountLedger();
    await settled();
    for (const id of ["ap-a", "ap-b", "ap-c", "ap-d", "ar-p"]) expect(box(id).disabled).toBe(false);
    for (const id of ["ar-x", "ap-r", "ap-w", "ap-s", "ap-p"]) expect(box(id).disabled).toBe(true);
    fireEvent.click(box("ap-a"));
    await waitFor(() => expect(box("ap-c").disabled).toBe(true));
    expect(box("ap-d").disabled).toBe(true);
    expect(box("ar-p").disabled).toBe(true);
    expect(box("ap-b").disabled).toBe(false);
  });

  it("勾两笔同收款方、同来源的应付合并发起：面板显示笔数与未付合计，请求 ar_ap_item_ids 为所勾各行", async () => {
    const posts = mountLedger();
    await settled();
    fireEvent.click(box("ap-a"));
    await waitFor(() => expect(box("ap-b").disabled).toBe(false));
    fireEvent.click(box("ap-b"));
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-line-legal_review")).not.toBeNull());
    const panel = screen.getByTestId("payment-confirmation-panel").textContent ?? "";
    expect(panel).toContain("2 笔");
    expect(panel).toContain("300.00");
    fireEvent.change(screen.getByTestId("payment-confirmation-reason"), { target: { value: "十一月货款" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-final-approver"), { target: { value: "usr_boss" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-finance_review"), { target: { value: "usr_f" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-legal_review"), { target: { value: "usr_l" } });
    fireEvent.click(screen.getByTestId("payment-confirmation-submit"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/finance/ar-ap-payment-confirmations");
    expect(posts[0]!.body.ar_ap_item_ids).toEqual(["ap-a", "ap-b"]);
    expect(posts[0]!.body).toMatchObject({ reason: "十一月货款", final_approver_id: "usr_boss" });
  });

  // 第十八批裁决 68「同一收款方……的多笔未结清应付」；裁决 69 题面「记成应收的那一行，登记收款就是登记该平台主体的出款」：应收行的收款方是本方，
  // 不能与同往来方的应付合进一张。
  it("勾了应付后，同往来方、同来源的平台主体付款应收行不可再勾（合并须同方向）", async () => {
    mountLedger();
    await settled();
    expect(box("ar-p").disabled).toBe(false);
    fireEvent.click(box("ap-q"));
    await waitFor(() => expect(box("ap-q").checked).toBe(true));
    expect(box("ar-p").disabled).toBe(true);
  });

  it("登记核销被付款确认闸拦下（409）时显示后端中文原因", async () => {
    mountLedger(true);
    await settled();
    fireEvent.change(screen.getByTestId("finance-payment-amount"), { target: { value: "100.00" } });
    fireEvent.change(screen.getByTestId("finance-payment-method"), { target: { value: "bank_transfer" } });
    fireEvent.click(screen.getByTestId("finance-payment-submit"));
    await waitFor(() => expect(screen.queryByTestId("finance-ledger-error")?.textContent ?? "").toContain("须先完成付款确认"));
  });
});
