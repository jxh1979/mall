import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const OVERVIEW = {
  asOf: "2026-07-18T02:00:00Z",
  scope: null,
  businessDate: "2026-07-17",
  metrics: {
    "revenue.ticket": { value: 999 }, // 快照旧值：接线后数字应以统一端点为准
    "revenue.bar": { value: 100 },
    "ar.balance": { value: 111 },
    "traffic.amusement": { value: 40 },
    "verify.done": { value: 41 },
    "member.new": { value: 42 },
    "risk.pending_review": { value: 43 },
  },
};

const TICKET_ROWS = [{ id: "to-1", amountCent: 10000 }, { id: "to-2", amountCent: 2500 }];
const TICKET_DETAIL = { metricKey: "revenue.ticket", valueType: "additive", summaryValue: 12500, rows: TICKET_ROWS, totalRows: 2, columns: [] };
const AR_DETAIL = { metricKey: "finance.ar_balance", valueType: "balance", summaryValue: 70000, rows: [{ id: "ar-1", unpaidAmountCent: 70000 }], totalRows: 1, columns: [] };
const BAR_ROWS = [{ id: "bbs-1", billId: "bill-1", settlementAmountCent: 8800 }, { id: "bbs-2", billId: "bill-2", settlementAmountCent: 1200 }];
const BAR_DETAIL = { metricKey: "revenue.bar", valueType: "additive", summaryValue: 10000, rows: BAR_ROWS, totalRows: 2, columns: [] };
// 第六批：总控剩余四卡（可加类合计=明细行数；余额类待复核=区间末值）。
const AMUSEMENT_ROWS = [{ id: "av-1" }, { id: "av-2" }, { id: "av-3" }];
const AMUSEMENT_DETAIL = { metricKey: "traffic.amusement", valueType: "additive", summaryValue: 3, rows: AMUSEMENT_ROWS, totalRows: 3, columns: [] };
const VERIFY_ROWS = [{ id: "tv-1" }, { id: "tv-2" }];
const VERIFY_DETAIL = { metricKey: "verify.done", valueType: "additive", summaryValue: 2, rows: VERIFY_ROWS, totalRows: 2, columns: [] };
const MEMBER_ROWS = [{ id: "cu-1" }, { id: "cu-2" }, { id: "cu-3" }, { id: "cu-4" }];
const MEMBER_DETAIL = { metricKey: "member.new", valueType: "additive", summaryValue: 4, rows: MEMBER_ROWS, totalRows: 4, columns: [] };
const RISK_DETAIL = { metricKey: "risk.pending_review", valueType: "balance", summaryValue: 7, rows: [{ id: "rv-1", source: "主管复核" }], totalRows: 1, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=finance.ar_balance")) return AR_DETAIL;
        if (url.includes("metric=revenue.bar")) return BAR_DETAIL;
        if (url.includes("metric=traffic.amusement")) return AMUSEMENT_DETAIL;
        if (url.includes("metric=verify.done")) return VERIFY_DETAIL;
        if (url.includes("metric=member.new")) return MEMBER_DETAIL;
        if (url.includes("metric=risk.pending_review")) return RISK_DETAIL;
        return TICKET_DETAIL;
      }
      return OVERVIEW;
    }),
    getPaged: vi.fn(),
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

describe("OverviewPage 首批接线：数字改走统一明细端点（数字=明细合计）", () => {
  it("票务收入数字=统一端点合计=明细行求和；带完整明细入口且区间=经营日", async () => {
    const api = makeApi();
    renderPage(api);

    // 一致性：卡片数字 == summaryValue == mock 明细行求和（同一端点同源）。
    const rowSum = TICKET_ROWS.reduce((sum, row) => sum + row.amountCent, 0);
    await waitFor(() => expect(screen.getByTestId("overview-kpi-ticket-value").textContent).toBe(String(rowSum)));
    expect(rowSum).toBe(TICKET_DETAIL.summaryValue);
    expect(screen.getByTestId("overview-metric-detail-ticket")).toBeTruthy();

    const detailCall = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .map((call) => call[0] as string)
      .find((url) => url.includes("metric=revenue.ticket"));
    expect(detailCall).toContain("from=2026-07-17");
    expect(detailCall).toContain("to=2026-07-17");
  });

  it("应收欠费余额卡（余额类）：数字=统一端点区间末值，可跳欠费与回款", async () => {
    const api = makeApi();
    const navigateTo = renderPage(api);

    await waitFor(() => expect(screen.getByTestId("overview-kpi-ar-value").textContent).toBe("70000"));
    expect(screen.getByTestId("overview-metric-detail-ar")).toBeTruthy();
    screen.getByTestId("overview-kpi-ar-value").click();
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-004");
  });

  it("酒吧收入卡（第二批接线）：数字=统一端点合计=明细行求和，带完整明细入口且区间=经营日", async () => {
    const api = makeApi();
    renderPage(api);

    const rowSum = BAR_ROWS.reduce((sum, row) => sum + row.settlementAmountCent, 0);
    await waitFor(() => expect(screen.getByTestId("overview-kpi-bar-value").textContent).toBe(String(rowSum)));
    expect(rowSum).toBe(BAR_DETAIL.summaryValue);
    expect(screen.getByTestId("overview-metric-detail-bar")).toBeTruthy();

    const detailCall = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .map((call) => call[0] as string)
      .find((url) => url.includes("metric=revenue.bar"));
    expect(detailCall).toContain("from=2026-07-17");
    expect(detailCall).toContain("to=2026-07-17");
  });

  it.each([
    ["overview-kpi-amusement-value", "overview-metric-detail-amusement", "traffic.amusement", AMUSEMENT_ROWS.length],
    ["overview-kpi-verify-value", "overview-metric-detail-verify", "verify.done", VERIFY_ROWS.length],
    ["overview-kpi-member-value", "overview-metric-detail-member", "member.new", MEMBER_ROWS.length],
  ])("第六批可加卡 %s：数字=统一端点合计=明细行数，带完整明细入口且区间=经营日", async (valueId, linkId, metric, rowCount) => {
    const api = makeApi();
    renderPage(api);

    await waitFor(() => expect(screen.getByTestId(valueId).textContent).toBe(String(rowCount)));
    expect(screen.getByTestId(linkId)).toBeTruthy();

    const detailCall = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .map((call) => call[0] as string)
      .find((url) => url.includes(`metric=${metric}`));
    expect(detailCall).toContain("from=2026-07-17");
    expect(detailCall).toContain("to=2026-07-17");
  });

  it("待复核卡（余额类）：数字=统一端点区间末值，带完整明细入口且仅带 to", async () => {
    const api = makeApi();
    renderPage(api);

    await waitFor(() => expect(screen.getByTestId("overview-kpi-risk-value").textContent).toBe("7"));
    expect(screen.getByTestId("overview-metric-detail-risk")).toBeTruthy();

    const detailCall = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .map((call) => call[0] as string)
      .find((url) => url.includes("metric=risk.pending_review"));
    expect(detailCall).toContain("to=2026-07-17");
    expect(detailCall).not.toContain("from=");
  });

  it("统一端点失败（含无权限）→ 回退快照值并隐藏完整明细入口，体验不降级", async () => {
    const api = makeApi(true);
    renderPage(api);

    await waitFor(() => expect(screen.getByTestId("overview-kpi-ticket-value").textContent).toBe("999"));
    expect(screen.queryByTestId("overview-metric-detail-ticket")).toBeNull();
    expect(screen.getByTestId("overview-kpi-ar-value").textContent).toBe("111");
    expect(screen.queryByTestId("overview-metric-detail-ar")).toBeNull();
    // 第二批：酒吧收入卡同样回退快照值并隐藏入口。
    expect(screen.getByTestId("overview-kpi-bar-value").textContent).toBe("100");
    expect(screen.queryByTestId("overview-metric-detail-bar")).toBeNull();
    // 第六批：总控剩余四卡同样回退快照值并隐藏入口。
    expect(screen.getByTestId("overview-kpi-amusement-value").textContent).toBe("40");
    expect(screen.queryByTestId("overview-metric-detail-amusement")).toBeNull();
    expect(screen.getByTestId("overview-kpi-verify-value").textContent).toBe("41");
    expect(screen.queryByTestId("overview-metric-detail-verify")).toBeNull();
    expect(screen.getByTestId("overview-kpi-member-value").textContent).toBe("42");
    expect(screen.queryByTestId("overview-metric-detail-member")).toBeNull();
    expect(screen.getByTestId("overview-kpi-risk-value").textContent).toBe("43");
    expect(screen.queryByTestId("overview-metric-detail-risk")).toBeNull();
  });
});
