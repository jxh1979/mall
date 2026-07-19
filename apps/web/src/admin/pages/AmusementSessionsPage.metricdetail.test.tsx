import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AmusementSessionsPage } from "./AmusementSessionsPage";
import type { AdminApiClient, AdminPageProps, AmusementSessionRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 本地场次两条：回退口径 = 场次数 2 / 总容量 150（Σcapacity）。
const SESSIONS: AmusementSessionRow[] = [
  { id: "s-1", capacity: 100, lockedCapacity: 10 },
  { id: "s-2", capacity: 50, lockedCapacity: 0 },
];

const COUNT_DETAIL = { metricKey: "amusement.session_count", valueType: "additive", summaryValue: 11, rows: [], totalRows: 11, columns: [] };
const CAP_DETAIL = { metricKey: "amusement.session_capacity", valueType: "additive", summaryValue: 999, rows: [], totalRows: 3, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=amusement.session_capacity")) return CAP_DETAIL;
        return COUNT_DETAIL;
      }
      if (url.includes("/amusement/sessions")) return SESSIONS;
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
  render(<AmusementSessionsPage {...props} />);
}

describe("AmusementSessionsPage 第三批接线：场次数/总容量改走统一明细端点（数字=明细合计）", () => {
  it("KPI 数字=统一端点合计，并带完整明细入口", async () => {
    renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("amusement-sessions-kpi-sessions").textContent).toBe("11"));
    expect(screen.getByTestId("amusement-sessions-kpi-capacity").textContent).toBe("999");
    expect(screen.getByTestId("amusement-sessions-metric-detail-sessions")).toBeTruthy();
    expect(screen.getByTestId("amusement-sessions-metric-detail-capacity")).toBeTruthy();
  });

  it("统一端点失败（含无权限）→ 回退本地列表计算并隐藏完整明细入口，体验不降级", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("amusement-sessions-kpi-sessions").textContent).toBe(String(SESSIONS.length)));
    expect(screen.getByTestId("amusement-sessions-kpi-capacity").textContent).toBe("150");
    expect(screen.queryByTestId("amusement-sessions-metric-detail-sessions")).toBeNull();
    expect(screen.queryByTestId("amusement-sessions-metric-detail-capacity")).toBeNull();
  });
});
