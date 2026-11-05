// 文件版本：V0.02
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// ROOT-04-R5：总控驾驶舱补第三方回流收入、采购到货（按商品及单位分列，裁决 M01）、补录数量（提交即计数、状态另列，裁决 M02）三张卡；
// BSM 第 1839 行「卡片明细对账：总控上每一张汇总卡片都能点开看明细——卡片上的数字就是它下面明细的合计」。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const GROUPS = [
  { itemId: "i-1", itemName: "酒", unit: "瓶", acceptedQuantity: 10, arrivedQuantity: 12, rejectedQuantity: 2, receiptCount: 1 },
  { itemId: "i-2", itemName: "设备", unit: "台", acceptedQuantity: 5, arrivedQuantity: 5, rejectedQuantity: 0, receiptCount: 1 },
];

const OVERVIEW = {
  asOf: "2026-07-18T02:00:00Z",
  scope: null,
  businessDate: "2026-07-17",
  metrics: {
    "revenue.thirdparty": { value: 15000 },
    "supply.received": { value: null, unit: "quantity_by_item_unit", dimension: { groups: GROUPS } },
    "risk.manual_backfill": { value: 4, dimension: { statusCounts: { pending: 2, approved: 1, rejected: 1 } } },
  },
};

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=revenue.thirdparty")) return { metricKey: "revenue.thirdparty", summaryValue: 15000, rows: [], totalRows: 2, columns: [] };
        if (url.includes("metric=supply.received")) return { metricKey: "supply.received", summaryValue: null, rows: [], totalRows: 2, columns: [], groups: GROUPS };
        if (url.includes("metric=risk.manual_backfill")) return { metricKey: "risk.manual_backfill", summaryValue: 4, rows: [], totalRows: 4, columns: [] };
        return { summaryValue: 0, rows: [], totalRows: 0, columns: [] };
      }
      return OVERVIEW;
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 200 }),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo,
  };
  render(<OverviewPage {...props} />);
  return navigateTo;
}

describe("OverviewPage ROOT-04：第三方回流收入、采购到货、补录数量三张卡", () => {
  it("三卡展示：第三方回流单列金额；采购到货按商品及单位分列不出合计；补录数量与状态分列；均带完整明细入口", async () => {
    const navigateTo = renderPage(makeApi());

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-thirdparty-value")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("overview-metric-detail-thirdparty")).not.toBeNull());
    expect(screen.getByTestId("overview-kpi-thirdparty-value").textContent).toBe("15000");

    const received = screen.getByTestId("overview-kpi-received");
    expect(received.textContent).toContain("酒 10瓶");
    expect(received.textContent).toContain("拒收 2瓶");
    expect(received.textContent).toContain("设备 5台");
    expect(received.textContent).not.toContain("15");
    expect(screen.getByTestId("overview-metric-detail-received")).toBeTruthy();

    expect(screen.getByTestId("overview-kpi-backfill-value").textContent).toBe("4");
    const backfill = screen.getByTestId("overview-kpi-backfill");
    expect(backfill.textContent).toContain("待处理 2");
    expect(backfill.textContent).toContain("已通过 1");
    expect(backfill.textContent).toContain("已驳回 1");
    expect(screen.getByTestId("overview-metric-detail-backfill")).toBeTruthy();

    screen.getByTestId("overview-kpi-thirdparty-value").click();
    expect(navigateTo).toHaveBeenCalledWith("ADM-INT-002");
  });

  it("统一明细无权限：三卡回退总览值，不显示完整明细入口", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-thirdparty-value")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("overview-kpi-thirdparty-value").textContent).toBe("15000"));
    expect(screen.getByTestId("overview-kpi-received").textContent).toContain("酒 10瓶");
    expect(screen.getByTestId("overview-kpi-backfill-value").textContent).toBe("4");
    expect(screen.queryByTestId("overview-metric-detail-thirdparty")).toBeNull();
    expect(screen.queryByTestId("overview-metric-detail-received")).toBeNull();
    expect(screen.queryByTestId("overview-metric-detail-backfill")).toBeNull();
  });
});
