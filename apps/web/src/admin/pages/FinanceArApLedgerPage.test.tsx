// 文件版本：V0.06
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { FinanceArApLedgerPage } from "./FinanceArApLedgerPage";
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

function renderPage(api: AdminApiClient) {
  const fullProps: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
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
    renderPage(makeApi(getPaged));
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
});
