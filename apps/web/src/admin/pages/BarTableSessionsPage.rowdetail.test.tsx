import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import type { AdminApiClient, AdminPageProps, BarReservationRow, BarTableRow, BarTableSessionRow, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const TABLES: BarTableRow[] = [
  { id: "tbl-1", tableCode: "A-01", barId: "bar-main", capacity: 8, status: "available" },
];
const RESERVATIONS: BarReservationRow[] = [
  { id: "rsv-1", barId: "bar-main", tableId: "tbl-1", customerId: "cust-1", guestCount: 4, depositStatus: "paid", status: "confirmed", reservedAt: "2026-07-05T20:00:00Z", createdAt: "2026-07-04T09:00:00Z" },
];
const SESSIONS: BarTableSessionRow[] = [
  { id: "sess-1", barId: "bar-main", tableId: "tbl-1", guestCount: 4, status: "open", sessionVersion: 1, businessDate: "2026-07-05", openedAt: "2026-07-05T20:30:00Z", closedAt: null },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("/bar/tables")) return TABLES;
      if (url.includes("/bar/reservations")) return RESERVATIONS;
      if (url.includes("/bar/table-sessions")) return SESSIONS;
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(): void {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BarTableSessionsPage {...props} />);
}

describe("BarTableSessionsPage 行明细抽屉 + 零英文", () => {
  it("点开台会话行 → 抽屉含状态（中文）+ 开台时间（中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-session-row-sess-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-session-row-sess-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("进行中");
    expect(drawer.textContent).toContain("开台时间");
    expect(drawer.textContent).toContain("2026年7月5日");
    expect(drawer.textContent).not.toContain("open");
    expect(drawer.textContent).not.toContain("2026-07-05T");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点订台预定行 → 抽屉含定金状态（中文）+ 预定时间（中文）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-reservation-row-rsv-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-reservation-row-rsv-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("已确认");
    expect(drawer.textContent).toContain("预定时间");
    expect(drawer.textContent).toContain("2026年7月5日");
    expect(drawer.textContent).not.toContain("confirmed");
  });

  it("点卡座行 → 抽屉含卡座状态（中文）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-table-row-tbl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-table-row-tbl-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("可用");
    expect(drawer.textContent).toContain("A-01");
    expect(drawer.textContent).not.toContain("available");
  });

  it("点会话行内单选按钮不打开抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("bar-session-select-sess-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-session-select-sess-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
