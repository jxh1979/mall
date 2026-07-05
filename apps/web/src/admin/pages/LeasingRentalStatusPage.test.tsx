import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { LeasingRentalStatusPage } from "./LeasingRentalStatusPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ROWS = [
  { spaceId: "sp-1", spaceName: "A101", rentalStatus: "let", color: null, daysToExpiry: 45 },
  { spaceId: "sp-2", spaceName: "A102", rentalStatus: "expiring", color: "#e6a23c", daysToExpiry: 12 },
  { spaceId: "sp-3", spaceName: "B201", rentalStatus: "available", color: null, daysToExpiry: null },
];

function makeApi(get = vi.fn().mockResolvedValue(ROWS)): AdminApiClient {
  return { get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post: vi.fn().mockResolvedValue({}), put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS, navigateTo: vi.fn() };
  render(<LeasingRentalStatusPage {...props} />);
}

describe("LeasingRentalStatusPage 行明细抽屉 + 零英文", () => {
  it("状态列渲染中文，不含原始英文码", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("leasing-rental-status-table")).toBeTruthy());
    const table = screen.getByTestId("leasing-rental-status-table").textContent ?? "";
    expect(table).toContain("已租"); // let
    expect(table).toContain("即将到期"); // expiring
    expect(table).toContain("可租"); // available
    expect(table).not.toContain("let");
    expect(table).not.toContain("expiring");
    expect(table).not.toContain("available");
  });

  it("点整行弹出行明细抽屉，含该行状态与距到期天数", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("rental-status-sp-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("rental-status-sp-2"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("即将到期");
    expect(text).toContain("12 天");
    expect(text).toContain("sp-2");
    expect(text).not.toContain("expiring");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("rental-status-sp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("rental-status-sp-1"));
    expect(await screen.findByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
