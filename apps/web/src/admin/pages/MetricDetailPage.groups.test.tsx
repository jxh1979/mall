// 文件版本：V0.01
import { render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MetricDetailPage } from "./MetricDetailPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, MetricDetailPayload } from "../types";

// ROOT-04-R5：统一明细页对采购到货按商品及单位分组列合计（裁决 M01「采购到货按商品及单位分别显示」，不出跨单位标量）；
// 补录类型枚举中文化；直达选择含三项新登记指标。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const RECEIVED_PAYLOAD: MetricDetailPayload = {
  metricKey: "supply.received",
  name: "采购到货",
  valueType: "additive",
  unit: "按商品及单位",
  caliber: "按商品及单位分列合格接收、实到、拒收数量与到货次数，不同单位不相加、不出合计数。",
  source: "采购收货登记（purchase_receipts，实时查询）。",
  part: null,
  from: "2026-07-17",
  to: "2026-07-17",
  dataAsOf: "2026-07-18T02:00:00Z",
  summaryValue: null,
  columns: [
    { key: "itemName", label: "商品" },
    { key: "acceptedQuantity", label: "合格接收" },
  ],
  rows: [{ purchaseId: "po-1", itemName: "酒", acceptedQuantity: 10 }],
  page: 1,
  size: 20,
  totalRows: 1,
  traceLinkPageId: "ADM-INV-003",
  traceLinkColumnKey: "purchaseId",
  groupColumns: [
    { key: "itemName", label: "商品" },
    { key: "unit", label: "单位" },
    { key: "acceptedQuantity", label: "合格接收" },
    { key: "rejectedQuantity", label: "拒收" },
  ],
  groups: [
    { itemName: "酒", unit: "瓶", acceptedQuantity: 10, rejectedQuantity: 2 },
    { itemName: "设备", unit: "台", acceptedQuantity: 5, rejectedQuantity: 0 },
  ],
};

const BACKFILL_PAYLOAD: MetricDetailPayload = {
  ...RECEIVED_PAYLOAD,
  metricKey: "risk.manual_backfill",
  name: "补录数量",
  unit: "笔",
  summaryValue: 1,
  columns: [
    { key: "sourceType", label: "补录类型" },
    { key: "status", label: "状态" },
  ],
  rows: [{ id: "adj-1", sourceType: "locked_backfill", status: "pending" }],
  traceLinkPageId: null,
  traceLinkColumnKey: null,
  groupColumns: null,
  groups: null,
};

function renderPage(payload: MetricDetailPayload, focusObjectId: string) {
  const api = {
    get: vi.fn(async () => payload),
    getPaged: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
    focusObjectId,
  };
  render(<MetricDetailPage {...props} />);
}

describe("MetricDetailPage ROOT-04：分组表与新登记指标", () => {
  it("采购到货：按商品及单位分组列合计，不出标量合计", async () => {
    renderPage(RECEIVED_PAYLOAD, "supply.received|2026-07-17|2026-07-17");

    await waitFor(() => expect(screen.queryByTestId("metric-detail-groups")).not.toBeNull());
    const groups = screen.getByTestId("metric-detail-groups");
    expect(within(groups).getByText("单位")).toBeTruthy();
    expect(within(groups).getByText("瓶")).toBeTruthy();
    expect(within(groups).getByText("设备")).toBeTruthy();
    expect(within(groups).getAllByRole("row")).toHaveLength(3);
    expect(screen.queryByTestId("metric-detail-summary")).toBeNull();
    // 直达选择含三项新登记指标；返回来源页为总控驾驶舱。
    const options = Array.from(screen.getByTestId("metric-detail-metric-select").querySelectorAll("option")).map((o) => o.value);
    expect(options).toEqual(expect.arrayContaining(["revenue.thirdparty", "supply.received", "risk.manual_backfill"]));
    expect(screen.getByTestId("metric-detail-back").textContent).toContain("总控驾驶舱");
  });

  it("补录数量：补录类型枚举显示中文，不渲染分组表", async () => {
    renderPage(BACKFILL_PAYLOAD, "risk.manual_backfill|2026-07-17|2026-07-17");

    await waitFor(() => expect(screen.queryByTestId("metric-detail-table")).not.toBeNull());
    expect(screen.getByTestId("metric-detail-table").textContent).toContain("锁账后补录");
    expect(screen.queryByTestId("metric-detail-groups")).toBeNull();
    expect(screen.getByTestId("metric-detail-summary").textContent).toContain("1");
  });
});
