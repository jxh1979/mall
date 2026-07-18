import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MetricDetailPage } from "./MetricDetailPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, MetricDetailPayload } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const TICKET_ROWS = [
  { id: "to-1", orderNo: "TK-1", quantity: 1, salesChannel: "online", amountCent: 10000, createdAt: "2026-07-17T10:00:00Z", paidAt: null },
  { id: "to-2", orderNo: "TK-2", quantity: 2, salesChannel: "onsite", amountCent: 2500, createdAt: "2026-07-17T11:00:00Z", paidAt: null },
];

const TICKET_PAYLOAD: MetricDetailPayload = {
  metricKey: "revenue.ticket",
  name: "票务收入",
  valueType: "additive",
  unit: "分",
  caliber: "已支付票务订单实付金额合计（分）。可加口径=区间求和。",
  source: "票务订单台账（实时查询）。",
  part: null,
  from: "2026-07-17",
  to: "2026-07-17",
  dataAsOf: "2026-07-18T02:00:00Z",
  summaryValue: 12500,
  summaryNote: null,
  columns: [
    { key: "orderNo", label: "订单号" },
    { key: "amountCent", label: "实付金额（分）" },
  ],
  rows: TICKET_ROWS,
  page: 1,
  size: 20,
  totalRows: 2,
  traceLinkPageId: "ADM-TKT-003",
  traceLinkColumnKey: "id",
};

const RATIO_PAYLOAD: MetricDetailPayload = {
  ...TICKET_PAYLOAD,
  metricKey: "energy.electricity_per_sqm",
  name: "每平米电耗",
  valueType: "ratio",
  unit: "kWh/㎡",
  part: "numerator",
  summaryValue: 3,
  numeratorValue: 1200,
  denominatorValue: 400,
  columns: [{ key: "consumption", label: "用量（kWh）" }],
  rows: [{ id: "r1", consumption: 1200 }],
  totalRows: 1,
  traceLinkPageId: "ADM-ENE-001",
};

function makeApi(handler: (url: string) => unknown): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => handler(url)),
    getPaged: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, focusObjectId?: string, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo,
    focusObjectId,
  };
  render(<MetricDetailPage {...props} />);
  return { navigateTo, api };
}

describe("MetricDetailPage 统一明细页（指标登记簿）", () => {
  it("按 focus=metric|from|to 载入：口径说明+合计+明细行，合计=明细行求和（同源一致性）", async () => {
    const api = makeApi(() => TICKET_PAYLOAD);
    renderPage(api, "revenue.ticket|2026-07-17|2026-07-17");

    await waitFor(() => expect(screen.getByTestId("metric-detail-summary")).toBeTruthy());
    // 数字=明细合计：页面顶部合计 == mock 明细行 amountCent 求和。
    const rowSum = TICKET_ROWS.reduce((sum, row) => sum + row.amountCent, 0);
    expect(screen.getByTestId("metric-detail-summary").textContent).toContain(new Intl.NumberFormat("zh-CN").format(rowSum));
    expect(screen.getByTestId("metric-detail-caliber").textContent).toContain("区间求和");
    expect(screen.getByTestId("metric-detail-caliber").textContent).toContain("票务订单台账");
    expect(screen.getByTestId("metric-detail-row-to-1")).toBeTruthy();
    // 行级单据下钻（TraceLink）。
    expect(screen.getByTestId("trace-link-ADM-TKT-003-to-1")).toBeTruthy();
    // 请求带同区间。
    const calledUrl = String((api.get as ReturnType<typeof vi.fn>).mock.calls[0]?.[0] ?? "");
    expect(calledUrl).toContain("metric=revenue.ticket");
    expect(calledUrl).toContain("from=2026-07-17");
    expect(calledUrl).toContain("to=2026-07-17");
  });

  it("比率类分「分子明细/分母明细」两页签，切换分母页签重新取数", async () => {
    const api = makeApi((url) => (url.includes("part=denominator")
      ? { ...RATIO_PAYLOAD, part: "denominator", columns: [{ key: "spaceName", label: "空间" }], rows: [{ id: "sp1", spaceName: "铺位A" }], traceLinkPageId: null, traceLinkColumnKey: null }
      : RATIO_PAYLOAD));
    renderPage(api, "energy.electricity_per_sqm||");

    await waitFor(() => expect(screen.getByTestId("metric-detail-part-tabs")).toBeTruthy());
    expect(screen.getByTestId("metric-detail-ratio-parts").textContent).toContain("1,200");
    fireEvent.click(screen.getByTestId("metric-detail-part-denominator"));
    await waitFor(() => expect(screen.getByText("铺位A")).toBeTruthy());
  });

  it("面包屑可回源页；未选指标时提示从入口进入", async () => {
    const api = makeApi(() => TICKET_PAYLOAD);
    const { navigateTo } = renderPage(api, "revenue.ticket|2026-07-17|2026-07-17");
    await waitFor(() => expect(screen.getByTestId("metric-detail-back")).toBeTruthy());
    fireEvent.click(screen.getByTestId("metric-detail-back"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-OV-001");
  });

  it("直达无 focus：显示指标选择提示，不发请求", () => {
    const api = makeApi(() => TICKET_PAYLOAD);
    renderPage(api, undefined);
    expect(screen.getByTestId("metric-detail-empty")).toBeTruthy();
    expect((api.get as ReturnType<typeof vi.fn>).mock.calls.length).toBe(0);
  });
});
