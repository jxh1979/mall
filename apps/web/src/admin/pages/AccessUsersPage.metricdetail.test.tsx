import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AccessUsersPage } from "./AccessUsersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(metricFails = false, headcount = 8): AdminApiClient {
  const get = vi.fn(async (url: string) => {
    if (url.includes("/analytics/metric-detail")) {
      if (metricFails) throw new Error("forbidden");
      return { metricKey: "staff.active_headcount", valueType: "balance", summaryValue: headcount, rows: [], totalRows: 0, columns: [] };
    }
    return [];
  });
  return { get, getPaged: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, navigateTo = vi.fn()) {
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: PERMS, navigateTo,
  };
  render(<AccessUsersPage {...props} />);
  return { navigateTo };
}

describe("AccessUsersPage 第五批接线：在职员工数走统一明细端点（平台口径·余额）", () => {
  it("就绪 → 在职员工数=端点合计，提供「完整明细」出口", async () => {
    const { navigateTo } = renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("access-users-kpi-headcount").textContent).toBe("8"));
    fireEvent.click(screen.getByTestId("access-users-metric-detail-headcount"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", "staff.active_headcount||");
  });

  it("端点失败 → 显示「—」并隐藏出口（fail-safe 回退）", async () => {
    renderPage(makeApi(true));
    await waitFor(() => expect(screen.getByTestId("access-users-kpi-headcount").textContent).toBe("—"));
    expect(screen.queryByTestId("access-users-metric-detail-headcount")).toBeNull();
  });
});
