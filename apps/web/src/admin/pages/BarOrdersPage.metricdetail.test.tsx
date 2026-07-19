import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps, BarOrderRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 本地列表两单（一未完结一已支付）：回退口径 = 订单数 2 / 未完结 1 / 金额 ¥208.00。
// mock 用真实序列化字段 totalAmountCent（BarOrderRecord），不用旧幽灵字段 totalAmount——回退断言才是真值自证。
const ORDERS: BarOrderRow[] = [
  { id: "b-1", orderNo: "BAR-0001", tableId: "T-1", status: "billed", totalAmountCent: 8800 },
  { id: "b-2", orderNo: "BAR-0002", tableId: "T-2", status: "paid", totalAmountCent: 12000 },
];

// 统一端点合计（数字=明细合计按构造成立）：计数类合计=行数，金额类合计=Σ totalAmountCent。
const COUNT_DETAIL = { metricKey: "bar.order_count", valueType: "additive", summaryValue: 5, rows: [], totalRows: 5, columns: [] };
const OPEN_DETAIL = { metricKey: "bar.open_order_count", valueType: "balance", summaryValue: 3, rows: [], totalRows: 3, columns: [] };
const AMOUNT_ROWS = [{ id: "b-1", totalAmountCent: 8800 }, { id: "b-2", totalAmountCent: 12000 }, { id: "b-3", totalAmountCent: 100 }];
const AMOUNT_DETAIL = { metricKey: "bar.order_amount", valueType: "additive", summaryValue: 20900, rows: AMOUNT_ROWS, totalRows: 3, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=bar.open_order_count")) return OPEN_DETAIL;
        if (url.includes("metric=bar.order_amount")) return AMOUNT_DETAIL;
        return COUNT_DETAIL;
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
  render(<BarOrdersPage {...props} />);
}

describe("BarOrdersPage 第二批接线：订单数/未完结/订单金额改走统一明细端点（数字=明细合计）", () => {
  it("三个 KPI 数字=统一端点合计（金额=明细行求和），并带完整明细入口", async () => {
    renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("bar-orders-kpi-orders").textContent).toBe("5"));
    expect(screen.getByTestId("bar-orders-kpi-open").textContent).toBe("3");
    // 一致性：金额卡数字 == summaryValue == mock 明细行求和（同一端点同源）。
    const rowSum = AMOUNT_ROWS.reduce((sum, row) => sum + row.totalAmountCent, 0);
    expect(rowSum).toBe(AMOUNT_DETAIL.summaryValue);
    expect(screen.getByTestId("bar-orders-kpi-amount").textContent).toContain("209.00");
    expect(screen.getByTestId("bar-orders-metric-detail-orders")).toBeTruthy();
    expect(screen.getByTestId("bar-orders-metric-detail-open")).toBeTruthy();
    expect(screen.getByTestId("bar-orders-metric-detail-amount")).toBeTruthy();
  });

  it("统一端点失败（含无权限）→ 回退本地列表计算并隐藏完整明细入口，体验不降级", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("bar-orders-kpi-orders").textContent).toBe(String(ORDERS.length)));
    expect(screen.getByTestId("bar-orders-kpi-open").textContent).toBe("1");
    expect(screen.getByTestId("bar-orders-kpi-amount").textContent).toContain("208.00");
    expect(screen.queryByTestId("bar-orders-metric-detail-orders")).toBeNull();
    expect(screen.queryByTestId("bar-orders-metric-detail-open")).toBeNull();
    expect(screen.queryByTestId("bar-orders-metric-detail-amount")).toBeNull();
  });
});
