import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 本地列表三笔（两在存一已领完）：回退口径 = 总数 3 / 在存 2 / 剩余数量 5+0+2=7（remainingQuantity 字段）。
const ROWS: StoredWineRow[] = [
  { id: "sb-1", savedNo: "SB-001", customerName: "客A", itemName: "威士忌", remainingQuantity: 5, status: "stored", createdAt: "2026-07-01T10:00:00Z" },
  { id: "sb-2", savedNo: "SB-002", customerName: "客B", itemName: "清酒", remainingQuantity: 0, status: "claimed", createdAt: "2026-07-02T10:00:00Z" },
  { id: "sb-3", savedNo: "SB-003", customerName: "客C", itemName: "红酒", remainingQuantity: 2, status: "stored", createdAt: "2026-07-03T10:00:00Z" },
];

// 统一端点合计（数字=明细合计按构造成立）：数量类合计=Σ remainingQuantity。
const COUNT_DETAIL = { metricKey: "bar.saved_bottle_count", valueType: "additive", summaryValue: 9, rows: [], totalRows: 9, columns: [] };
const STORED_DETAIL = { metricKey: "bar.saved_bottle_stored", valueType: "balance", summaryValue: 6, rows: [], totalRows: 6, columns: [] };
const QUANTITY_ROWS = [{ id: "sb-1", remainingQuantity: 5 }, { id: "sb-2", remainingQuantity: 0 }, { id: "sb-3", remainingQuantity: 2 }, { id: "sb-4", remainingQuantity: 4 }];
const QUANTITY_DETAIL = { metricKey: "bar.saved_bottle_quantity", valueType: "balance", summaryValue: 11, rows: QUANTITY_ROWS, totalRows: 4, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=bar.saved_bottle_stored")) return STORED_DETAIL;
        if (url.includes("metric=bar.saved_bottle_quantity")) return QUANTITY_DETAIL;
        return COUNT_DETAIL;
      }
      return ROWS;
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
  render(<StoredWinePage {...props} />);
}

describe("StoredWinePage 第二批接线：存酒总数/在存/剩余数量改走统一明细端点（数字=明细合计）", () => {
  it("三个 KPI 数字=统一端点合计（剩余数量=明细行求和），并带完整明细入口", async () => {
    renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("stored-wine-kpi-bottles").textContent).toBe("9"));
    expect(screen.getByTestId("stored-wine-kpi-stored").textContent).toBe("6");
    const rowSum = QUANTITY_ROWS.reduce((sum, row) => sum + row.remainingQuantity, 0);
    expect(rowSum).toBe(QUANTITY_DETAIL.summaryValue);
    expect(screen.getByTestId("stored-wine-kpi-quantity").textContent).toBe(String(rowSum));
    expect(screen.getByTestId("stored-wine-metric-detail-bottles")).toBeTruthy();
    expect(screen.getByTestId("stored-wine-metric-detail-stored")).toBeTruthy();
    expect(screen.getByTestId("stored-wine-metric-detail-quantity")).toBeTruthy();
  });

  it("统一端点失败（含无权限）→ 回退本地列表计算（剩余数量按 remainingQuantity 求和）并隐藏完整明细入口", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("stored-wine-kpi-bottles").textContent).toBe(String(ROWS.length)));
    expect(screen.getByTestId("stored-wine-kpi-stored").textContent).toBe("2");
    expect(screen.getByTestId("stored-wine-kpi-quantity").textContent).toBe("7");
    expect(screen.queryByTestId("stored-wine-metric-detail-bottles")).toBeNull();
    expect(screen.queryByTestId("stored-wine-metric-detail-stored")).toBeNull();
    expect(screen.queryByTestId("stored-wine-metric-detail-quantity")).toBeNull();
  });
});
