import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", quantity: 2, status: "stored", createdAt: "2026-07-01" },
  { id: "w-2", savedNo: "SW-0002", customerName: "客户乙", itemName: "红酒", quantity: 1, status: "consumed", createdAt: "2026-07-02" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async () => ROWS),
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
  };
  render(<StoredWinePage {...props} />);
}

describe("StoredWinePage KPI 聚焦视图", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  it("点“可领取”KPI → ?kpi=stored，表内只剩存酒中记录，KPI 卡与领取表单隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("stored-wine-kpi-stored")).toBeTruthy());
    fireEvent.click(screen.getByTestId("stored-wine-kpi-stored"));
    expect(window.location.search).toContain("kpi=stored");
    expect(screen.getByTestId("stored-wine-kpi-focus-banner").textContent).toContain("可领取（1）");
    const table = screen.getByTestId("stored-wine-table").textContent ?? "";
    expect(table).toContain("SW-0001");
    expect(table).not.toContain("SW-0002");
    expect(screen.queryByTestId("stored-wine-summary")).toBeNull();
    expect(screen.queryByTestId("stored-wine-claim-form")).toBeNull();
  });

  it("聚焦点「返回全部」→ 恢复完整页面（KPI 卡 + 两条记录）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("stored-wine-kpi-bottles")).toBeTruthy());
    fireEvent.click(screen.getByTestId("stored-wine-kpi-bottles"));
    fireEvent.click(screen.getByTestId("stored-wine-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("stored-wine-summary")).toBeTruthy();
    const table = screen.getByTestId("stored-wine-table").textContent ?? "";
    expect(table).toContain("SW-0001");
    expect(table).toContain("SW-0002");
  });

  it("带参直链 ?kpi=stored → 直接进聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/bar/stored-wine?kpi=stored");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("stored-wine-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("stored-wine-kpi-focus-banner").textContent).toContain("可领取（1）");
  });

  it("表格状态渲染为中文（存酒中/已取用完），KPI 子标签无英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("stored-wine-table")).toBeTruthy());
    const table = screen.getByTestId("stored-wine-table").textContent ?? "";
    expect(table).toContain("存酒中");
    expect(table).toContain("已取用完");
    const summary = screen.getByTestId("stored-wine-summary").textContent ?? "";
    expect(summary).not.toContain("stored");
  });
});
