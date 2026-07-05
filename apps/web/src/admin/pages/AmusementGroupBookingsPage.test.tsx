import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementGroupBookingsPage } from "./AmusementGroupBookingsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const BOOKINGS = [
  { id: "b-1", project_id: "amuse-1", session_id: "s-1", booking_type: "reserved", customer_name: "团体甲", party_size: 20, reserved_capacity: 20, booking_status: "approved" },
  { id: "b-2", project_id: "amuse-1", session_id: "s-2", booking_type: "reserved", customer_name: "团体乙", party_size: 10, reserved_capacity: 10, booking_status: "pending" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/amusement/group-bookings")) return Promise.resolve(envelope(BOOKINGS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AmusementGroupBookingsPage {...fullProps} />);
}

describe("AmusementGroupBookingsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/amusement/group-bookings");
  });

  it("点“已审批”数字 → URL 带 ?kpi=bookings&status=approved，聚焦只剩已审批行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-group-bookings-kpi-approved")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-group-bookings-kpi-approved"));
    expect(window.location.search).toContain("kpi=bookings");
    expect(window.location.search).toContain("status=approved");
    expect(screen.getByTestId("amusement-group-bookings-kpi-focus-banner").textContent).toContain("正在查看：已审批预约（1）");
    expect(screen.getByTestId("amusement-group-booking-select-b-1")).toBeTruthy();
    expect(screen.queryByTestId("amusement-group-booking-select-b-2")).toBeNull();
    expect(screen.queryByTestId("amusement-group-bookings-summary")).toBeNull();
    expect(screen.queryByTestId("amusement-group-booking-release-reason")).toBeNull();
  });

  it("点“团体预约”数字 → 聚焦显示全部预约行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-group-bookings-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-group-bookings-kpi-total"));
    expect(screen.getByTestId("amusement-group-bookings-kpi-focus-banner").textContent).toContain("正在查看：团体预约（2）");
    expect(screen.getByTestId("amusement-group-booking-select-b-1")).toBeTruthy();
    expect(screen.getByTestId("amusement-group-booking-select-b-2")).toBeTruthy();
  });

  it("聚焦点「返回全部」→ 参数清除，恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-group-bookings-kpi-approved")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-group-bookings-kpi-approved"));
    fireEvent.click(screen.getByTestId("amusement-group-bookings-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("amusement-group-bookings-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("amusement-group-bookings-summary")).toBeTruthy();
    expect(screen.getByTestId("amusement-group-booking-select-b-2")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/amusement/group-bookings?kpi=bookings&status=approved");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-group-bookings-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("amusement-group-booking-select-b-1")).toBeTruthy();
    expect(screen.queryByTestId("amusement-group-booking-select-b-2")).toBeNull();
  });

  it("KPI 子标签+状态为中文，不含英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-group-bookings-summary")).toBeTruthy());
    const summary = screen.getByTestId("amusement-group-bookings-summary").textContent ?? "";
    expect(summary).not.toContain("bookings");
    expect(summary).not.toContain("reserved");
    expect(summary).not.toContain("approved");
    expect(summary).toContain("预约数");
    // 表格状态列中文（approved→已通过，reserved→已预订）
    const table = screen.getByTestId("amusement-group-bookings-table").textContent ?? "";
    expect(table).toContain("已通过");
  });
});
