import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AmusementSafetyPage } from "./AmusementSafetyPage";
import type { AdminApiClient, AdminPageProps, AmusementSafetyInspectionRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 本地巡检三条：回退口径 = 巡检数 3 / 待整改 1（rectificationStatus=pending_rectification）。
const ROWS: AmusementSafetyInspectionRow[] = [
  { id: "i-1", rectificationStatus: "pending_rectification" },
  { id: "i-2", rectificationStatus: "resolved" },
  { id: "i-3", rectificationStatus: "resolved" },
];

const COUNT_DETAIL = { metricKey: "amusement.inspection_count", valueType: "additive", summaryValue: 13, rows: [], totalRows: 13, columns: [] };
const PENDING_DETAIL = { metricKey: "amusement.inspection_pending_rectification", valueType: "balance", summaryValue: 4, rows: [], totalRows: 4, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=amusement.inspection_pending_rectification")) return PENDING_DETAIL;
        return COUNT_DETAIL;
      }
      if (url.includes("/amusement/safety-inspections")) return ROWS;
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
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
  render(<AmusementSafetyPage {...props} />);
}

describe("AmusementSafetyPage 第三批接线：巡检数/待整改改走统一明细端点（数字=明细合计）", () => {
  it("KPI 数字=统一端点合计，并带完整明细入口", async () => {
    renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("amusement-safety-kpi-total").textContent).toBe("13"));
    expect(screen.getByTestId("amusement-safety-kpi-pending-rectification").textContent).toBe("4");
    expect(screen.getByTestId("amusement-safety-metric-detail-total")).toBeTruthy();
    expect(screen.getByTestId("amusement-safety-metric-detail-pending-rectification")).toBeTruthy();
  });

  it("统一端点失败（含无权限）→ 回退本地列表计算并隐藏完整明细入口，体验不降级", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("amusement-safety-kpi-total").textContent).toBe(String(ROWS.length)));
    expect(screen.getByTestId("amusement-safety-kpi-pending-rectification").textContent).toBe("1");
    expect(screen.queryByTestId("amusement-safety-metric-detail-total")).toBeNull();
    expect(screen.queryByTestId("amusement-safety-metric-detail-pending-rectification")).toBeNull();
  });
});
