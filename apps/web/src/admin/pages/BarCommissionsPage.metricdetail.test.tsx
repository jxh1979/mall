import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarCommissionsPage } from "./BarCommissionsPage";
import type { AdminApiClient, AdminPageProps, BarOrderRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 本地列表两单：回退口径 = 订单数 2 / 金额 ¥208.00。
const ORDERS: BarOrderRow[] = [
  { id: "b-1", orderNo: "BAR-0001", tableId: "T-1", status: "billed", totalAmount: 8800 },
  { id: "b-2", orderNo: "BAR-0002", tableId: "T-2", status: "paid", totalAmount: 12000 },
];

// 统一端点合计（与订单/出品/账单页共用同一登记项 bar.order_count / bar.order_amount）。
const COUNT_DETAIL = { metricKey: "bar.order_count", valueType: "additive", summaryValue: 7, rows: [], totalRows: 7, columns: [] };
const AMOUNT_ROWS = [{ id: "b-1", totalAmountCent: 8800 }, { id: "b-2", totalAmountCent: 12000 }, { id: "b-3", totalAmountCent: 4200 }];
const AMOUNT_DETAIL = { metricKey: "bar.order_amount", valueType: "additive", summaryValue: 25000, rows: AMOUNT_ROWS, totalRows: 3, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        return url.includes("metric=bar.order_amount") ? AMOUNT_DETAIL : COUNT_DETAIL;
      }
      return ORDERS;
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  };
  render(<BarCommissionsPage {...props} />);
}

describe("BarCommissionsPage 第二批接线：订单数卡（笔数+金额）改走统一明细端点（数字=明细合计）", () => {
  it("订单数与金额=统一端点合计（金额=明细行求和），并带完整明细入口", async () => {
    renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("bar-commissions-kpi-orders").textContent).toBe("7"));
    const rowSum = AMOUNT_ROWS.reduce((sum, row) => sum + row.totalAmountCent, 0);
    expect(rowSum).toBe(AMOUNT_DETAIL.summaryValue);
    expect(screen.getByTestId("bar-commissions-kpi-amount").textContent).toContain("250.00");
    expect(screen.getByTestId("bar-commissions-metric-detail-orders")).toBeTruthy();
  });

  it("统一端点失败（含无权限）→ 回退本地列表计算并隐藏完整明细入口，体验不降级", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("bar-commissions-kpi-orders").textContent).toBe(String(ORDERS.length)));
    expect(screen.getByTestId("bar-commissions-kpi-amount").textContent).toContain("208.00");
    expect(screen.queryByTestId("bar-commissions-metric-detail-orders")).toBeNull();
  });
});
