import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationEventsPage } from "./IntegrationEventsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, IntegrationEventRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const RETRY: IntegrationEventRow[] = [{ id: "retry-1", taskNo: "RT-1", providerCode: "prov-a", status: "pending" }];
const COMPENSATION: IntegrationEventRow[] = [{ id: "comp-1", taskNo: "CT-1", providerCode: "prov-b", status: "pending" }];
const ORDER_SYNC: IntegrationEventRow[] = [{ id: "sync-1", orderNo: "TKT-9001", syncType: "order", status: "matched" }];
const VOUCHER: IntegrationEventRow[] = [{ id: "vv-1", verificationNo: "VV-1", status: "processed" }];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("retry-tasks")) return RETRY;
      if (url.includes("compensation-tasks")) return COMPENSATION;
      if (url.includes("order-syncs")) return ORDER_SYNC;
      if (url.includes("voucher-verifications")) return VOUCHER;
      return [];
    }),
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
  render(<IntegrationEventsPage {...props} />);
}

describe("IntegrationEventsPage KPI 聚焦视图", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/integrations/events");
  });

  it("点“订单同步”来源 KPI → ?kpi=order_sync，表内只剩订单同步事件", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-events-kpi-order_sync")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-events-kpi-order_sync"));
    expect(window.location.search).toContain("kpi=order_sync");
    expect(screen.getByTestId("integration-events-kpi-focus-banner").textContent).toContain("订单同步（1）");
    const table = screen.getByTestId("integration-events-table").textContent ?? "";
    expect(table).toContain("TKT-9001");
    expect(table).not.toContain("RT-1");
    expect(screen.queryByTestId("integration-events-summary")).toBeNull();
    expect(screen.queryByTestId("integration-events-form")).toBeNull();
  });

  it("聚焦点「返回全部」→ 恢复完整页面（KPI 卡 + 四类事件）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-events-kpi-retry_task")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-events-kpi-retry_task"));
    fireEvent.click(screen.getByTestId("integration-events-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("integration-events-summary")).toBeTruthy();
  });

  it("带参直链 ?kpi=voucher_verification → 直接进聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/integrations/events?kpi=voucher_verification");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-events-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("integration-events-kpi-focus-banner").textContent).toContain("券核销（1）");
  });

  it("KPI 子标签为中文，不含 retry/compensation/sync/voucher 英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-events-summary")).toBeTruthy());
    const text = screen.getByTestId("integration-events-summary").textContent ?? "";
    expect(text).not.toContain("retry");
    expect(text).not.toContain("compensation");
    expect(text).not.toContain("voucher");
    expect(text).toContain("待处理事件");
  });
});
