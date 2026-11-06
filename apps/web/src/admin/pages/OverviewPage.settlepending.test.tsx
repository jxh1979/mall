// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 Y 段第 183 条 183-R2（BSM:1824「财务：日结状态、对账差异、结算待确认。」；第十八批裁决 40「数演出结算挂起待确认的、
// 积分月结账单有未处理异议的，分两个数显示，不相加」）：总控驾驶舱「结算待确认」卡——只请求所持权限那一类，没权限的那一类不显示，失败显示「—」。

const perms = (grants: string[], wildcard = false): EffectivePermissionsState => ({ status: "ready", value: { grants, denies: [], wildcard } });
const BOTH = perms([], true);
const MEDIA_ONLY = perms(["analytics.dashboard.view", "media.performance.view"]);
const POINTS_ONLY = perms(["analytics.dashboard.view", "ticket.points.billing.view"]);
const NEITHER = perms(["analytics.dashboard.view"]);

const OVERVIEW = { asOf: "2026-07-18T02:00:00Z", scope: null, businessDate: "2026-07-17", metrics: {} };
const MEDIA_URL = "/api/v1/settlement/media-shows/pending-settlement-count";
const BILLS_URL = "/api/v1/loyalty/ticket-points-billing/bills";
const TWO_BILLS = [{ id: "tpb-1", openObjections: 1 }, { id: "tpb-2", openObjections: 0 }];

function makeApi(media: () => Promise<unknown>, bills: () => Promise<unknown>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url === MEDIA_URL) return media();
      if (url === BILLS_URL) return bills();
      if (url.includes("/analytics/metric-detail")) return { summaryValue: 0, rows: [], totalRows: 0, columns: [] };
      return OVERVIEW;
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 200 }),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
}

const ok = () => makeApi(() => Promise.resolve({ pendingCount: 1 }), () => Promise.resolve(TWO_BILLS));

function renderPage(api: AdminApiClient, effectivePermissions = BOTH, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions,
    navigateTo,
  };
  render(<OverviewPage {...props} />);
  return navigateTo;
}

function getUrls(api: AdminApiClient): string[] {
  return (api.get as unknown as ReturnType<typeof vi.fn>).mock.calls.map((call) => String(call[0]));
}

const value = () => screen.queryByTestId("overview-kpi-settlepending-value")?.textContent;

describe("OverviewPage 183-R2：结算待确认卡", () => {
  it("两种权限都有：演出结算挂起条数与积分账单有未处理异议的张数分开显示、不相加", async () => {
    const api = ok();
    renderPage(api);

    await waitFor(() => expect(value()).toBe("演出结算 1 · 积分账单异议 1"));
    expect(getUrls(api)).toContain(MEDIA_URL);
    expect(getUrls(api)).toContain(BILLS_URL);
    expect(screen.getByTestId("overview-kpi-settlepending").textContent).toContain("结算待确认");
  });

  it("只有 media.performance.view：只显示并只请求演出结算", async () => {
    const api = ok();
    renderPage(api, MEDIA_ONLY);

    await waitFor(() => expect(value()).toBe("演出结算 1"));
    expect(getUrls(api)).not.toContain(BILLS_URL);
  });

  it("只有 ticket.points.billing.view：只显示并只请求积分账单异议", async () => {
    const api = ok();
    renderPage(api, POINTS_ONLY);

    await waitFor(() => expect(value()).toBe("积分账单异议 1"));
    expect(getUrls(api)).not.toContain(MEDIA_URL);
  });

  it("两种都没有：不显示这张卡、两类都不请求", async () => {
    const api = ok();
    renderPage(api, NEITHER);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-ticket")).not.toBeNull());
    await waitFor(() => expect(getUrls(api).some((url) => url.includes("metric=revenue.ticket"))).toBe(true));
    expect(screen.queryByTestId("overview-kpi-settlepending")).toBeNull();
    expect(getUrls(api).filter((url) => url === MEDIA_URL || url === BILLS_URL)).toEqual([]);
  });

  it("读取失败 → 显示「—」", async () => {
    const api = makeApi(() => Promise.reject(new Error("forbidden")), () => Promise.reject(new Error("forbidden")));
    renderPage(api);

    await waitFor(() => expect(getUrls(api)).toContain(MEDIA_URL));
    await waitFor(() => expect(getUrls(api)).toContain(BILLS_URL));
    await waitFor(() => expect(value()).toBe("演出结算 — · 积分账单异议 —"));
  });

  it("点演出结算 → 演出合同与履约结算页；点积分账单异议 → 门票积分月结页", async () => {
    const api = ok();
    const navigateTo = renderPage(api);

    await waitFor(() => expect(value()).toBe("演出结算 1 · 积分账单异议 1"));
    fireEvent.click(screen.getByTestId("overview-kpi-settlepending-media"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-STL-004");
    fireEvent.click(screen.getByTestId("overview-kpi-settlepending-points"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-FIN-008");
  });
});
