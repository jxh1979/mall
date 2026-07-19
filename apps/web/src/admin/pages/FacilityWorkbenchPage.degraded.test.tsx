import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const EQUIPMENT = [{ id: "eq-1", name: "1号客梯", equipmentType: "elevator", status: "normal", nextMaintenanceDue: "2026-08-01" }];
const WORK_ORDERS = [{ id: "wo-1", code: "WO-0001", workOrderType: "repair", priority: "high", status: "processing", title: "客梯异响", assigneeId: "u-fix" }];
const TEMPLATES = [{ id: "tpl-1", name: "消防巡检模板", inspectionType: "fire", cycleDays: 7, abnormalMode: "to_work_order", active: true }];
const TASKS = [{ id: "task-1", inspectionType: "fire", status: "pending", title: "月度消防巡检", scheduleMode: "template" }];
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// failing=会抛错的派生视图路径子串；四张基础表（equipment/work-orders/templates/tasks）恒成功。
function makeApi(failing: string): AdminApiClient {
  return {
    get: vi.fn((url: string) => {
      if (url.includes(failing)) return Promise.reject(new Error("boom"));
      if (url.includes("/maintenance-due")) return Promise.resolve([]);
      if (url.includes("/work-orders/overdue")) return Promise.resolve([]);
      if (url.includes("/work-orders")) return Promise.resolve(WORK_ORDERS);
      if (url.includes("/equipment")) return Promise.resolve(EQUIPMENT);
      if (url.includes("/inspections/templates")) return Promise.resolve(TEMPLATES);
      if (url.includes("/inspections/tasks")) return Promise.resolve(TASKS);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

describe("FacilityWorkbenchPage 派生视图副资源降级（批2）", () => {
  it("逾期工单派生视图失败 → 设备/工单主表照常，逾期区块降级提示，整页不报错", async () => {
    const setGlobalError = vi.fn();
    renderPage(makeApi("/work-orders/overdue"), setGlobalError);
    await waitFor(() => expect(screen.getByTestId("facility-equipment-eq-1")).toBeTruthy());
    expect(screen.getByTestId("facility-work-order-wo-1")).toBeTruthy();
    expect(screen.getByTestId("facility-work-orders-overdue-degraded")).toBeTruthy();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });

  it("维保到期派生视图失败 → 设备主表照常，维保到期区块降级提示", async () => {
    renderPage(makeApi("/maintenance-due"));
    await waitFor(() => expect(screen.getByTestId("facility-equipment-eq-1")).toBeTruthy());
    expect(screen.getByTestId("facility-maintenance-due-degraded")).toBeTruthy();
  });
});
