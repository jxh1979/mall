import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { EnergyAnalyticsPage } from "./EnergyAnalyticsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const DASH = { businessDate: "2026-07-05", metrics: { "energy.abnormal_count": { value: 1 } } };
const ABNORMAL = [
  { readingId: "rd-1", meterId: "MT-9", meterName: "1号电表", meterType: "electricity", period: "2026-07", consumption: 8888, abnormalReason: "surge", readAt: "2026-07-05T13:20:00Z" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/dashboards/energy") ? DASH : ABNORMAL)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  };
  render(<EnergyAnalyticsPage {...props} />);
}

describe("EnergyAnalyticsPage 异常读数行明细抽屉", () => {
  it("点异常读数行 → 抽屉含该行值(类型/原因中文)，无英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("energy-abnormal-row-rd-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("energy-abnormal-row-rd-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("1号电表");
    expect(text).toContain("电"); // meterType electricity
    expect(text).toContain("突增"); // abnormalReason surge
    expect(text).not.toContain("electricity");
    expect(text).not.toContain("surge");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("energy-abnormal-row-rd-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("energy-abnormal-row-rd-1"));
    expect(await screen.findByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
