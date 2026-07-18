import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { EnergyAnalyticsPage } from "./EnergyAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const DASH = {
  businessDate: "2026-07-17",
  metrics: {
    "energy.total_electricity": { value: 999 }, // 快照旧值：接线后以统一端点为准
    "energy.electricity_per_sqm": { value: 0.5 },
    "energy.abnormal_count": { value: 2 },
  },
};

const ELECTRICITY_ROWS = [{ id: "r1", consumption: 700.5 }, { id: "r2", consumption: 300 }];
const ELECTRICITY_DETAIL = { metricKey: "energy.total_electricity", valueType: "additive", summaryValue: 1000.5, rows: ELECTRICITY_ROWS, totalRows: 2, columns: [] };
const PER_SQM_DETAIL = { metricKey: "energy.electricity_per_sqm", valueType: "ratio", summaryValue: 2.5013, numeratorValue: 1000.5, denominatorValue: 400, rows: [], totalRows: 0, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        return url.includes("metric=energy.electricity_per_sqm") ? PER_SQM_DETAIL : ELECTRICITY_DETAIL;
      }
      return url.includes("/dashboards/energy") ? DASH : [];
    }),
    getPaged: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
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
  render(<EnergyAnalyticsPage {...props} />);
}

describe("EnergyAnalyticsPage 首批接线：用电/每平米电耗改走统一明细端点", () => {
  it("用电数字=统一端点合计=明细行求和；每平米电耗=分子/分母；带完整明细入口", async () => {
    const api = makeApi();
    renderPage(api);

    // 一致性：卡片数字 == summaryValue == mock 明细行 consumption 求和。
    const rowSum = ELECTRICITY_ROWS.reduce((sum, row) => sum + row.consumption, 0);
    await waitFor(() => expect(screen.getByTestId("energy-total-electricity").textContent)
      .toContain(new Intl.NumberFormat("zh-CN", { maximumFractionDigits: 2 }).format(rowSum)));
    expect(rowSum).toBe(ELECTRICITY_DETAIL.summaryValue);
    expect(screen.getByTestId("energy-metric-detail-electricity")).toBeTruthy();

    await waitFor(() => expect(screen.getByTestId("energy-per-sqm").textContent).toContain("2.5"));
    expect(screen.getByTestId("energy-metric-detail-per-sqm")).toBeTruthy();

    const detailCall = (api.get as ReturnType<typeof vi.fn>).mock.calls
      .map((call) => call[0] as string)
      .find((url) => url.includes("metric=energy.total_electricity"));
    expect(detailCall).toContain("from=2026-07-17");
    expect(detailCall).toContain("to=2026-07-17");
  });

  it("统一端点失败 → 回退快照值并隐藏完整明细入口", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("energy-total-electricity").textContent).toContain("999"));
    expect(screen.queryByTestId("energy-metric-detail-electricity")).toBeNull();
    expect(screen.queryByTestId("energy-metric-detail-per-sqm")).toBeNull();
  });
});
