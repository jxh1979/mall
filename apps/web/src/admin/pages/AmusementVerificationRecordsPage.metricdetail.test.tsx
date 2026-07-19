import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AmusementVerificationRecordsPage } from "./AmusementVerificationRecordsPage";
import type { AdminApiClient, AdminPageProps, AmusementVerificationRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 本地核销三条：回退口径 = 入场 2（direction=entry 或空）/ 出场 1（direction=exit）。
const RECORDS: AmusementVerificationRow[] = [
  { id: "v-1", direction: "entry" },
  { id: "v-2" },
  { id: "v-3", direction: "exit" },
];

const ENTRY_DETAIL = { metricKey: "amusement.entry_count", valueType: "additive", summaryValue: 22, rows: [], totalRows: 22, columns: [] };
const EXIT_DETAIL = { metricKey: "amusement.exit_count", valueType: "additive", summaryValue: 8, rows: [], totalRows: 8, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=amusement.exit_count")) return EXIT_DETAIL;
        return ENTRY_DETAIL;
      }
      if (url.includes("/amusement/verifications")) return RECORDS;
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
  render(<AmusementVerificationRecordsPage {...props} />);
}

describe("AmusementVerificationRecordsPage 第三批接线：入场/出场改走统一明细端点（无筛选时数字=明细合计）", () => {
  it("无筛选时 KPI 数字=统一端点合计，并带完整明细入口", async () => {
    renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("amu-verif-kpi-entry").textContent).toBe("22"));
    expect(screen.getByTestId("amu-verif-kpi-exit").textContent).toBe("8");
    expect(screen.getByTestId("amu-verif-metric-detail-entry")).toBeTruthy();
    expect(screen.getByTestId("amu-verif-metric-detail-exit")).toBeTruthy();
  });

  it("统一端点失败（含无权限）→ 回退本地筛选后计数并隐藏完整明细入口，体验不降级", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("amu-verif-kpi-entry").textContent).toBe("2"));
    expect(screen.getByTestId("amu-verif-kpi-exit").textContent).toBe("1");
    expect(screen.queryByTestId("amu-verif-metric-detail-entry")).toBeNull();
    expect(screen.queryByTestId("amu-verif-metric-detail-exit")).toBeNull();
  });
});
