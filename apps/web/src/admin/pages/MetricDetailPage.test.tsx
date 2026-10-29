import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { MetricDetailPage } from "./MetricDetailPage";
import { SENSITIVE_PAGE_IDS } from "../adminRoutes";
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

// 第 23 条复核（PGM:687「凡页面上出现上述两类必须强制脱敏内容的，这个页面就是敏感页」「打开敏感页这件事本身也要留痕，记下……这一次看到的是
// 哪些对象（哪个客人、哪个商户、哪些单据）」）：指标明细列出新增会员的脱敏手机号、团体预约的客户姓名，是敏感页，载入后报这一次看到的对象。
describe("MetricDetailPage 敏感页", () => {
  function renderSensitive(payload: MetricDetailPayload, focus: string) {
    const reportViewedObjects = vi.fn();
    render(<MetricDetailPage api={makeApi(() => payload)} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} navigateTo={vi.fn()} focusObjectId={focus}
      reportViewedObjects={reportViewedObjects} />);
    return reportViewedObjects;
  }

  it("新增会员明细（脱敏手机号）按客人报这一次看到的对象；页面列入敏感页清单", async () => {
    const report = renderSensitive({
      ...TICKET_PAYLOAD, metricKey: "member.new", name: "新增会员数", unit: "人",
      columns: [{ key: "maskedPhone", label: "脱敏手机号" }, { key: "id", label: "客人编号" }],
      rows: [{ id: "cus_1", maskedPhone: "138****0001" }, { id: "cus_2", maskedPhone: "139****0002" }], totalRows: 2, traceLinkPageId: "ADM-CUS-001",
    }, "member.new||");

    await waitFor(() => expect(report).toHaveBeenCalledWith("customer", [{ id: "cus_1" }, { id: "cus_2" }]));
    expect(SENSITIVE_PAGE_IDS.has("ADM-ANA-012")).toBe(true);
  });

  it("团体预约明细（客户姓名）按团体预约报；不含两类内容的指标不报", async () => {
    const report = renderSensitive({
      ...TICKET_PAYLOAD, metricKey: "amusement.group_booking_count", name: "团体预约数", unit: "笔",
      columns: [{ key: "customerName", label: "客户" }], rows: [{ id: "agb_1", customerName: "张三" }], totalRows: 1, traceLinkPageId: "ADM-AMU-002",
    }, "amusement.group_booking_count||");
    await waitFor(() => expect(report).toHaveBeenCalledWith("amusement_group_booking", [{ id: "agb_1" }]));

    const ticket = renderSensitive(TICKET_PAYLOAD, "revenue.ticket|2026-07-17|2026-07-17");
    await waitFor(() => expect(screen.getAllByTestId("metric-detail-row-to-1").length).toBeGreaterThan(0));
    expect(ticket).not.toHaveBeenCalled();
  });
});
