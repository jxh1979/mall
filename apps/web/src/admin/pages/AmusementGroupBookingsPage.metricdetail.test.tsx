import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AmusementGroupBookingsPage } from "./AmusementGroupBookingsPage";
import type { AdminApiClient, AdminPageProps, AmusementGroupBookingRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// 本地预约两条：回退口径 = 预约数 2 / 容量占用 50（Σreserved_capacity）。
const BOOKINGS: AmusementGroupBookingRow[] = [
  { id: "g-1", reservedCapacity: 20, bookingStatus: "approved" },
  { id: "g-2", reservedCapacity: 30, bookingStatus: "pending_approval" },
];

const COUNT_DETAIL = { metricKey: "amusement.group_booking_count", valueType: "additive", summaryValue: 12, rows: [], totalRows: 12, columns: [] };
const RESERVED_DETAIL = { metricKey: "amusement.group_reserved_capacity", valueType: "additive", summaryValue: 555, rows: [], totalRows: 12, columns: [] };

function makeApi(metricDetailFails = false): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/analytics/metric-detail")) {
        if (metricDetailFails) throw new Error("forbidden");
        if (url.includes("metric=amusement.group_reserved_capacity")) return RESERVED_DETAIL;
        return COUNT_DETAIL;
      }
      if (url.includes("/amusement/group-bookings")) return BOOKINGS;
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
  render(<AmusementGroupBookingsPage {...props} />);
}

describe("AmusementGroupBookingsPage 第三批接线：预约数/容量占用改走统一明细端点（数字=明细合计）", () => {
  it("KPI 数字=统一端点合计，并带完整明细入口", async () => {
    renderPage(makeApi());

    await waitFor(() => expect(screen.getByTestId("amusement-group-bookings-kpi-total").textContent).toBe("12"));
    expect(screen.getByTestId("amusement-group-bookings-kpi-reserved").textContent).toBe("555");
    expect(screen.getByTestId("amusement-group-bookings-metric-detail-total")).toBeTruthy();
    expect(screen.getByTestId("amusement-group-bookings-metric-detail-reserved")).toBeTruthy();
  });

  it("统一端点失败（含无权限）→ 回退本地列表计算并隐藏完整明细入口，体验不降级", async () => {
    renderPage(makeApi(true));

    await waitFor(() => expect(screen.getByTestId("amusement-group-bookings-kpi-total").textContent).toBe(String(BOOKINGS.length)));
    expect(screen.getByTestId("amusement-group-bookings-kpi-reserved").textContent).toBe("50");
    expect(screen.queryByTestId("amusement-group-bookings-metric-detail-total")).toBeNull();
    expect(screen.queryByTestId("amusement-group-bookings-metric-detail-reserved")).toBeNull();
  });
});
