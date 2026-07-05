import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ServiceOrdersPage } from "./ServiceOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const CATALOG = [{ id: "c-1", name: "点舞", priceCent: 20000, durationMinutes: 10, active: true }];
const ORDERS = [
  { id: "so-1", itemName: "点舞", priceCent: 20000, tableId: "T-1", performerUserId: "staff-9", status: "paid", mgmtFeeCent: 6000, dancerShareCent: 14000 },
  { id: "so-2", itemName: "点舞", priceCent: 20000, tableId: "T-2", performerUserId: "staff-8", status: "completed", mgmtFeeCent: 6000, dancerShareCent: 14000 },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/catalog") ? CATALOG : ORDERS)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(extra: Partial<AdminPageProps> = {}) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
  render(<ServiceOrdersPage {...props} />);
}

describe("ServiceOrdersPage KPI 聚焦视图 + 员工引用可点", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/services/orders");
  });

  it("点“已完成”KPI → ?kpi=completed，服务单表只剩已完成单，KPI 卡隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("service-orders-kpi-completed")).toBeTruthy());
    fireEvent.click(screen.getByTestId("service-orders-kpi-completed"));
    expect(window.location.search).toContain("kpi=completed");
    expect(screen.getByTestId("service-orders-kpi-focus-banner").textContent).toContain("已完成（1）");
    const table = screen.getByTestId("service-orders-table").textContent ?? "";
    expect(table).toContain("so-2");
    expect(table).not.toContain("so-1");
    expect(screen.queryByTestId("service-orders-summary")).toBeNull();
  });

  it("聚焦点「返回全部」→ 恢复 KPI 卡与两条服务单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("service-orders-kpi-orders")).toBeTruthy());
    fireEvent.click(screen.getByTestId("service-orders-kpi-orders"));
    fireEvent.click(screen.getByTestId("service-orders-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("service-orders-summary")).toBeTruthy();
    const table = screen.getByTestId("service-orders-table").textContent ?? "";
    expect(table).toContain("so-1");
    expect(table).toContain("so-2");
  });

  it("带参直链 ?kpi=paid → 直接进聚焦视图（只剩已收款单）", async () => {
    window.history.replaceState({}, "", "/admin/services/orders?kpi=paid");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("service-orders-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("service-orders-kpi-focus-banner").textContent).toContain("已收款（1）");
  });

  it("履约人是可点 TraceLink，跳 ADM-ACC-001 并带员工 id", async () => {
    const navigateTo = vi.fn();
    renderPage({ navigateTo });
    const link = await screen.findByTestId("trace-link-ADM-ACC-001-staff-9");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-ACC-001", "staff-9");
  });
});
