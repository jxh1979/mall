import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StaffShiftAttendancePage } from "./StaffShiftAttendancePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TODAY = new Date().toISOString().slice(0, 10);
const VALUES: Record<string, number> = {
  "staff.shift_count": 7,
  "staff.attendance_count": 12,
  "staff.attendance_late_count": 2,
};

function makeApi(metricFails = false): AdminApiClient {
  const get = vi.fn(async (url: string) => {
    if (url.includes("/analytics/metric-detail")) {
      if (metricFails) throw new Error("forbidden");
      const metric = new URL(url, "http://x").searchParams.get("metric") ?? "";
      return { metricKey: metric, valueType: "additive", summaryValue: VALUES[metric] ?? null, rows: [], totalRows: 0, columns: [] };
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
  render(<StaffShiftAttendancePage {...props} />);
  return { navigateTo, api };
}

describe("StaffShiftAttendancePage 第五批接线：排班/考勤/迟到走统一明细端点（平台口径）", () => {
  it("就绪 → 三卡=端点合计，提供「完整明细」出口（带所选日区间）", async () => {
    const { navigateTo } = renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("staff-shift-kpi-count").textContent).toBe("7"));
    expect(screen.getByTestId("staff-attendance-kpi-count").textContent).toBe("12");
    expect(screen.getByTestId("staff-late-kpi-count").textContent).toBe("2");
    fireEvent.click(screen.getByTestId("staff-shift-metric-detail-count"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", `staff.shift_count|${TODAY}|${TODAY}`);
  });

  it("端点失败 → 三卡显示「—」并隐藏出口（fail-safe 回退）", async () => {
    renderPage(makeApi(true));
    await waitFor(() => expect(screen.getByTestId("staff-shift-kpi-count").textContent).toBe("—"));
    expect(screen.getByTestId("staff-attendance-kpi-count").textContent).toBe("—");
    expect(screen.getByTestId("staff-late-kpi-count").textContent).toBe("—");
    expect(screen.queryByTestId("staff-shift-metric-detail-count")).toBeNull();
  });

  it("筛选转换：所选日期落入统一明细端点区间（from=to=当日）", async () => {
    const { api } = renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("staff-shift-kpi-count").textContent).toBe("7"));
    const calls = (api.get as ReturnType<typeof vi.fn>).mock.calls.map((c) => String(c[0]));
    expect(calls.some((u) => u.includes("metric=staff.shift_count") && u.includes(`from=${TODAY}`) && u.includes(`to=${TODAY}`))).toBe(true);
  });
});
