// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 S 段第 173 条 173-R1（BSM:1824「财务：日结状态、对账差异、结算待确认。」；BSM:1747「待生成、待确认、有差异、已确认、已锁定、已归档」）：
// 总控驾驶舱加「日结状态」卡——看板所看营业日、查看人可见范围内各日结按状态计数；一笔都没有即「待生成」（同 143-R1）。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const NO_DAILY_CLOSE_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["analytics.dashboard.view"], denies: [], wildcard: false } };

const OVERVIEW = { asOf: "2026-07-18T02:00:00Z", scope: null, businessDate: "2026-07-17", metrics: {} };
const FOUR_ROWS = [
  { id: "dc-1", status: "pending_confirm" },
  { id: "dc-2", status: "has_difference" },
  { id: "dc-3", status: "locked" },
  { id: "dc-4", status: "locked" },
];

function makeApi(dailyCloses: (url: string) => Promise<unknown>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) return { summaryValue: 0, rows: [], totalRows: 0, columns: [] };
      return OVERVIEW;
    }),
    getPaged: vi.fn(async (url: string) => {
      if (url.startsWith("/api/v1/finance/daily-closes")) return dailyCloses(url);
      return { items: [], total: 0, page: 1, size: 20 };
    }),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
}

const paged = (items: unknown[]) => Promise.resolve({ items, total: items.length, page: 1, size: 200 });

function renderPage(api: AdminApiClient, effectivePermissions = ADMIN_PERMS, navigateTo = vi.fn()) {
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

function dailyCloseUrls(api: AdminApiClient): string[] {
  return (api.getPaged as unknown as ReturnType<typeof vi.fn>).mock.calls
    .map((call) => String(call[0]))
    .filter((url) => url.startsWith("/api/v1/finance/daily-closes"));
}

describe("OverviewPage 173-R1：日结状态卡", () => {
  it("有日结权限：按总览营业日查日结、蛇形查询键，按状态依次计数", async () => {
    const api = makeApi(() => paged(FOUR_ROWS));
    renderPage(api);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-dayclose-value")?.textContent).toBe("待确认 1 · 有差异 1 · 已确认 0 · 已锁定 2 · 已归档 0"));
    expect(dailyCloseUrls(api)).toContain("/api/v1/finance/daily-closes?business_date=2026-07-17&page=1&size=200");
    expect(screen.getByTestId("overview-kpi-dayclose").textContent).toContain("日结状态");
  });

  it("该营业日一笔日结都没有 → 待生成", async () => {
    const api = makeApi(() => paged([]));
    renderPage(api);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-dayclose-value")?.textContent).toBe("待生成"));
  });

  it("筛选结束日 2026-07-20 后按该营业日重查", async () => {
    const api = makeApi((url) => paged(url.includes("business_date=2026-07-20") ? [{ id: "dc-9", status: "archived" }] : FOUR_ROWS));
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("overview-kpi-dayclose-value")?.textContent).toBe("待确认 1 · 有差异 1 · 已确认 0 · 已锁定 2 · 已归档 0"));

    fireEvent.click(screen.getByTestId("overview-date-to"));
    fireEvent.click(screen.getByRole("button", { name: "选择年月" }));
    fireEvent.click(screen.getByRole("button", { name: "下一年" }));
    fireEvent.click(screen.getByRole("button", { name: "7月" }));
    fireEvent.click(screen.getByRole("button", { name: "20" }));
    fireEvent.click(screen.getByTestId("overview-date-apply"));

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-dayclose-value")?.textContent).toBe("待确认 0 · 有差异 0 · 已确认 0 · 已锁定 0 · 已归档 1"));
    expect(dailyCloseUrls(api)).toContain("/api/v1/finance/daily-closes?business_date=2026-07-20&page=1&size=200");
  });

  it("没有 finance.daily_close.manage：不显示这张卡、不发日结请求", async () => {
    const api = makeApi(() => paged(FOUR_ROWS));
    renderPage(api, NO_DAILY_CLOSE_PERMS);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-ticket")).not.toBeNull());
    await waitFor(() => expect((api.get as unknown as ReturnType<typeof vi.fn>).mock.calls.length).toBeGreaterThan(1));
    expect(screen.queryByTestId("overview-kpi-dayclose")).toBeNull();
    expect(dailyCloseUrls(api)).toEqual([]);
  });

  it("点卡片 → 对账差异页（日结区块）", async () => {
    const api = makeApi(() => paged(FOUR_ROWS));
    const navigateTo = renderPage(api);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-dayclose-value")?.textContent).toBe("待确认 1 · 有差异 1 · 已确认 0 · 已锁定 2 · 已归档 0"));
    fireEvent.click(screen.getByTestId("overview-kpi-dayclose-value"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-FIN-002");
  });

  it("日结接口失败 → 显示「—」", async () => {
    const api = makeApi(() => Promise.reject(new Error("forbidden")));
    renderPage(api);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-dayclose-value")?.textContent).toBe("—"));
    await waitFor(() => expect(dailyCloseUrls(api)).toContain("/api/v1/finance/daily-closes?business_date=2026-07-17&page=1&size=200"));
  });
});
