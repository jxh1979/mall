import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneWorkOrdersPage } from "./MerchantZoneWorkOrdersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 3 行：2 未关闭 + 1 已关闭 → 总数3、进行中(未终结)2。
const ROWS = [
  { id: "wo-1", work_order_type: "repair", title: "工单甲", priority: "high", status: "processing", due_at: "2026-07-10T09:00:00Z", resolution: "更换密封圈", created_at: "2026-07-05T08:30:00Z" },
  { id: "wo-2", work_order_type: "service", title: "工单乙", priority: "low", status: "closed" },
  { id: "wo-3", work_order_type: "complaint", title: "工单丙", priority: "medium", status: "created" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(envelope(ROWS))));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "merchant", accountType: "merchant", merchantId: "m-1" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<MerchantZoneWorkOrdersPage {...fullProps} />);
}

describe("MerchantZoneWorkOrdersPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/merchant-zone/work-orders");
  });

  it("点“进行中”数字 → ?kpi=open，横幅(2)+表只剩未关闭子集，KPI 卡隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-orders-kpi-open"));
    expect(window.location.search).toContain("kpi=open");
    expect(screen.getByTestId("merchant-zone-work-orders-kpi-focus-banner").textContent).toContain("正在查看：进行中工单（2）");
    expect(screen.queryByTestId("merchant-zone-work-orders-summary")).toBeNull();
    const text = screen.getByTestId("merchant-zone-work-orders-table").textContent ?? "";
    expect(text).not.toContain("工单乙");
    expect(text).toContain("工单甲");
  });

  it("点“工单总数”数字 → 全量子集", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-orders-kpi-total"));
    expect(screen.getByTestId("merchant-zone-work-orders-kpi-focus-banner").textContent).toContain("（3）");
    expect(screen.getByTestId("merchant-zone-work-orders-table").textContent).toContain("工单乙");
  });

  it("「返回全部」→ 恢复 KPI 卡+全量表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-kpi-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-orders-kpi-open"));
    fireEvent.click(screen.getByTestId("merchant-zone-work-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-zone-work-orders-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-work-orders-table").textContent).toContain("工单乙");
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/merchant-zone/work-orders?kpi=open");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("merchant-zone-work-orders-summary")).toBeNull();
  });
});

describe("MerchantZoneWorkOrdersPage 行明细抽屉（整行可点·日期中文·零英文枚举）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/merchant-zone/work-orders");
  });

  it("点工单行 → 抽屉含标题+中文类型/优先级/状态+中文时间，无英文枚举与 ISO 原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-order-row-wo-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-row-wo-1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("工单甲");
    expect(text).toContain("报修");
    expect(text).toContain("处理中");
    expect(text).toContain("2026年7月10日");
    expect(text).toContain("2026年7月5日");
    expect(text).not.toContain("repair");
    expect(text).not.toContain("processing");
    expect(text).not.toContain("2026-07-10T");
  });

  it("列内截止/提交时间为中文，不直出 ISO 原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-orders-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-work-orders-table").textContent ?? "";
    expect(table).toContain("2026年7月10日");
    expect(table).not.toContain("2026-07-10T");
  });

  it("点工单行 → Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-work-order-row-wo-3")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-work-order-row-wo-3"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
