// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OverviewPage } from "./OverviewPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 Y 段第 183 条 183-R1（BSM:1824「财务：日结状态、对账差异、结算待确认。」；BSM:1839「卡片上的数字就是它下面明细的合计」）：
// 总控驾驶舱加「对账差异」卡——复用指标 finance.recon_diff_count（所看营业日新发现的对账差异笔数，含已关闭），持 finance.reconciliation.view 才请求与显示。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const NO_RECON_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["analytics.dashboard.view"], denies: [], wildcard: false } };

const OVERVIEW = { asOf: "2026-07-18T02:00:00Z", scope: null, businessDate: "2026-07-17", metrics: {} };
const RECON_URL_0717 = "/api/v1/analytics/metric-detail?metric=finance.recon_diff_count&page=1&size=1&from=2026-07-17&to=2026-07-17";
const RECON_URL_0720 = "/api/v1/analytics/metric-detail?metric=finance.recon_diff_count&page=1&size=1&from=2026-07-20&to=2026-07-20";

function makeApi(recon: (url: string) => Promise<unknown>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("metric=finance.recon_diff_count")) return recon(url);
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

const detail = (summaryValue: number) => Promise.resolve({ metricKey: "finance.recon_diff_count", summaryValue, rows: [], totalRows: summaryValue, columns: [] });

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

function getUrls(api: AdminApiClient): string[] {
  return (api.get as unknown as ReturnType<typeof vi.fn>).mock.calls.map((call) => String(call[0]));
}

describe("OverviewPage 183-R1：对账差异卡", () => {
  it("有对账权限：按总览营业日取 finance.recon_diff_count，卡显示合计并出完整明细入口（同区间）", async () => {
    const api = makeApi(() => detail(3));
    const navigateTo = renderPage(api);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-recondiff-value")?.textContent).toBe("3"));
    expect(getUrls(api)).toContain(RECON_URL_0717);
    expect(screen.getByTestId("overview-kpi-recondiff").textContent).toContain("对账差异");
    fireEvent.click(screen.getByTestId("overview-metric-detail-recondiff"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", "finance.recon_diff_count|2026-07-17|2026-07-17");
  });

  it("指标端点拒绝（如项目级账号）→ 显示「—」、不出完整明细入口", async () => {
    const api = makeApi(() => Promise.reject(new Error("forbidden")));
    renderPage(api);

    await waitFor(() => expect(getUrls(api)).toContain(RECON_URL_0717));
    await waitFor(() => expect(screen.queryByTestId("overview-kpi-recondiff-value")?.textContent).toBe("—"));
    expect(screen.queryByTestId("overview-metric-detail-recondiff")).toBeNull();
  });

  it("没有 finance.reconciliation.view：不显示这张卡、不请求该指标", async () => {
    const api = makeApi(() => detail(3));
    renderPage(api, NO_RECON_PERMS);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-ticket")).not.toBeNull());
    await waitFor(() => expect(getUrls(api).some((url) => url.includes("metric=revenue.ticket"))).toBe(true));
    expect(screen.queryByTestId("overview-kpi-recondiff")).toBeNull();
    expect(getUrls(api).filter((url) => url.includes("finance.recon_diff_count"))).toEqual([]);
  });

  it("点数字 → 对账差异页", async () => {
    const api = makeApi(() => detail(3));
    const navigateTo = renderPage(api);

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-recondiff-value")?.textContent).toBe("3"));
    fireEvent.click(screen.getByTestId("overview-kpi-recondiff-value"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-FIN-002");
  });

  it("筛选结束日 2026-07-20 后按该营业日重查", async () => {
    const api = makeApi((url) => detail(url.includes("from=2026-07-20") ? 5 : 3));
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("overview-kpi-recondiff-value")?.textContent).toBe("3"));

    fireEvent.click(screen.getByTestId("overview-date-to"));
    fireEvent.click(screen.getByRole("button", { name: "选择年月" }));
    fireEvent.click(screen.getByRole("button", { name: "下一年" }));
    fireEvent.click(screen.getByRole("button", { name: "7月" }));
    fireEvent.click(screen.getByRole("button", { name: "20" }));
    fireEvent.click(screen.getByTestId("overview-date-apply"));

    await waitFor(() => expect(screen.queryByTestId("overview-kpi-recondiff-value")?.textContent).toBe("5"));
    expect(getUrls(api)).toContain(RECON_URL_0720);
  });
});
