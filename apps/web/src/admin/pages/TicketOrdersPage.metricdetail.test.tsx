import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, TicketOrderRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 本地列表 3 单：回退口径 = 订单数 3 / 已支付 2 / 待出票 2（issue_status!=='issued'）。
const ORDERS: TicketOrderRow[] = [
  { id: "t-1", orderNo: "TK-1", payStatus: "paid", issueStatus: "issued" },
  { id: "t-2", orderNo: "TK-2", payStatus: "paid", issueStatus: "pending" },
  { id: "t-3", orderNo: "TK-3", payStatus: "pending", issueStatus: "pending" },
];

// 统一端点合计（数字=明细合计按构造成立）：计数类合计=行数。
const COUNT_DETAIL = { metricKey: "ticket.order_count", valueType: "additive", summaryValue: 9, rows: [], totalRows: 9, columns: [] };
const PAID_DETAIL = { metricKey: "ticket.paid_order_count", valueType: "additive", summaryValue: 7, rows: [], totalRows: 7, columns: [] };
const PENDING_DETAIL = { metricKey: "ticket.pending_issue_count", valueType: "balance", summaryValue: 5, rows: [], totalRows: 5, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=ticket.paid_order_count")) return PAID_DETAIL;
        if (url.includes("metric=ticket.pending_issue_count")) return PENDING_DETAIL;
        return COUNT_DETAIL;
      }
      if (url.includes("/ticketing/orders")) return ORDERS;
      return [];
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
  render(<TicketOrdersPage {...props} />);
}

describe("TicketOrdersPage 第三批接线：订单数/已支付/待出票改走统一明细端点（数字=明细合计）", () => {
  it("三个 KPI 数字=统一端点合计，并带完整明细入口", async () => {
    renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("ticket-orders-kpi-orders").textContent).toBe("9"));
    expect(screen.getByTestId("ticket-orders-kpi-paid").textContent).toBe("7");
    expect(screen.getByTestId("ticket-orders-kpi-not-issued").textContent).toBe("5");
    expect(screen.getByTestId("ticket-orders-metric-detail-orders")).toBeTruthy();
    expect(screen.getByTestId("ticket-orders-metric-detail-paid")).toBeTruthy();
    expect(screen.getByTestId("ticket-orders-metric-detail-not-issued")).toBeTruthy();
  });

  it("统一端点失败（含无权限）→ 回退本地列表计算并隐藏完整明细入口，体验不降级", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("ticket-orders-kpi-orders").textContent).toBe(String(ORDERS.length)));
    expect(screen.getByTestId("ticket-orders-kpi-paid").textContent).toBe("2");
    expect(screen.getByTestId("ticket-orders-kpi-not-issued").textContent).toBe("2");
    expect(screen.queryByTestId("ticket-orders-metric-detail-orders")).toBeNull();
    expect(screen.queryByTestId("ticket-orders-metric-detail-paid")).toBeNull();
    expect(screen.queryByTestId("ticket-orders-metric-detail-not-issued")).toBeNull();
  });
});
