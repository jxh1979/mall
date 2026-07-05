import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationEventsPage } from "./IntegrationEventsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, IntegrationEventRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const RETRY: IntegrationEventRow[] = [
  { id: "retry-1", taskNo: "RT-1", providerCode: "prov-a", eventType: "webhook", status: "pending", retryCount: 2, updatedAt: "2026-07-05T13:20:00Z" },
];
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

describe("IntegrationEventsPage 行明细抽屉", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/integrations/events");
  });

  it("点事件行 → 抽屉含该行值（状态中文·时间中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-event-row-retry-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("integration-event-row-retry-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("RT-1");
    expect(drawer.textContent).toContain("重试任务");
    expect(drawer.textContent).toContain("待处理");
    expect(drawer.textContent).toContain("2026年7月5日 13:20");
    // 抽屉零英文枚举：状态码不得原文出现
    expect(drawer.textContent).not.toContain("pending");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("表格更新时间中文化，不出现 ISO 时间戳原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-events-table")).toBeTruthy());
    const table = screen.getByTestId("integration-events-table").textContent ?? "";
    expect(table).toContain("2026年7月5日 13:20");
    expect(table).not.toContain("2026-07-05T13:20:00Z");
  });

  it("点行内任务号 TraceLink 不触发抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("integration-event-row-retry-1")).toBeTruthy());
    const cell = screen.getByTestId("integration-event-row-retry-1").querySelector("td:nth-child(2)") as HTMLElement;
    fireEvent.click(cell);
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
