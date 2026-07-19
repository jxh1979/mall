import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const MONTH = new Date().toISOString().slice(0, 7);
const MONTH_DAY = `${MONTH}-01`;
const VALUES: Record<string, number> = {
  "staff.payroll_net_total": 120000,
  "staff.payroll_statement_count": 3,
  "staff.pending_leave_count": 5,
};

function makeApi(metricFails = false): AdminApiClient {
  const get = vi.fn(async (url: string) => {
    if (url.includes("/analytics/metric-detail")) {
      if (metricFails) throw new Error("forbidden");
      const metric = new URL(url, "http://x").searchParams.get("metric") ?? "";
      return { metricKey: metric, valueType: metric === "staff.pending_leave_count" ? "balance" : "additive", summaryValue: VALUES[metric] ?? null, rows: [], totalRows: 0, columns: [] };
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
  render(<StaffPayrollLeavePage {...props} />);
  return { navigateTo, api };
}

describe("StaffPayrollLeavePage 第五批接线：实发合计/薪资单数/待审请假走统一明细端点（平台口径）", () => {
  it("就绪 → 实发合计=端点合计（元）、薪资单数、待审请假；提供出口", async () => {
    const { navigateTo } = renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("staff-payroll-kpi-total").textContent).toContain("1,200"));
    expect(screen.getByTestId("staff-payroll-kpi-count").textContent).toBe("3");
    expect(screen.getByTestId("staff-leave-kpi-pending").textContent).toBe("5");
    fireEvent.click(screen.getByTestId("staff-payroll-metric-detail-total"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", `staff.payroll_net_total|${MONTH_DAY}|${MONTH_DAY}`);
    fireEvent.click(screen.getByTestId("staff-leave-metric-detail-pending"));
    expect(navigateTo).toHaveBeenCalledWith("ADM-ANA-012", "staff.pending_leave_count||");
  });

  it("端点失败 → 三卡显示「—」并隐藏出口（fail-safe 回退）", async () => {
    renderPage(makeApi(true));
    await waitFor(() => expect(screen.getByTestId("staff-payroll-kpi-total").textContent).toBe("—"));
    expect(screen.getByTestId("staff-payroll-kpi-count").textContent).toBe("—");
    expect(screen.getByTestId("staff-leave-kpi-pending").textContent).toBe("—");
    expect(screen.queryByTestId("staff-payroll-metric-detail-total")).toBeNull();
    expect(screen.queryByTestId("staff-leave-metric-detail-pending")).toBeNull();
  });

  it("筛选转换：所选月份落入统一明细端点区间（from=to=月首日）", async () => {
    const { api } = renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("staff-payroll-kpi-count").textContent).toBe("3"));
    const calls = (api.get as ReturnType<typeof vi.fn>).mock.calls.map((c) => String(c[0]));
    expect(calls.some((u) => u.includes("metric=staff.payroll_net_total") && u.includes(`from=${MONTH_DAY}`))).toBe(true);
  });
});
