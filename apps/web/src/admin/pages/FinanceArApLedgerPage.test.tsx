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
});
