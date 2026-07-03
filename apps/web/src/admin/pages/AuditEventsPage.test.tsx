import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AuditEventsPage } from "./AuditEventsPage";
import type { AdminApiClient, AdminPageProps, AuditEventRow, EffectivePermissionsState } from "../types";

const ROWS: AuditEventRow[] = [
  { id: "e1", eventType: "login", eventCategory: "auth", result: "success", traceId: "t-1", summary: "登录成功" },
  { id: "e2", eventType: "block", eventCategory: "security", result: "blocked", traceId: "t-2", summary: "越权阻断" },
];

function pagedResolver() {
  // total=42 > pageSize，可翻页；当前页 2 行。
  return vi.fn().mockResolvedValue({ items: ROWS, total: 42, page: 1, size: 10 });
}

function makeApi(getPaged = pagedResolver()): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([]),
    getPaged,
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const fullProps: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AuditEventsPage {...fullProps} />);
}

describe("AuditEventsPage 服务端分页", () => {
  it("首次渲染调用 getPaged 且事件总数用服务端 total", async () => {
    const getPaged = pagedResolver();
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalledWith(expect.stringContaining("page=1")));
    // 事件总数 KPI 用服务端 total（42），而非当前页行数（2）。
    expect(screen.getByTestId("audit-events-summary").textContent).toContain("42");
  });

  it("翻页调用 getPaged 带 page=2（可翻更早历史）", async () => {
    const getPaged = pagedResolver();
    renderPage(makeApi(getPaged));
    await waitFor(() => expect(getPaged).toHaveBeenCalled());
    fireEvent.click(screen.getByTestId("audit-events-pager-next"));
    await waitFor(() => expect(getPaged).toHaveBeenCalledWith(expect.stringContaining("page=2")));
  });
});
