import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementInspectionTasksPage } from "./AmusementInspectionTasksPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const TASKS = [
  { id: "t-1", project_id: "amuse-1", device_id: "dev-9", due_at: "2026-06-20T10:00:00Z", status: "overdue" },
  { id: "t-2", project_id: "amuse-1", device_id: null, due_at: "2026-06-21T14:30:00Z", status: "pending" },
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
      if (url.includes("/amusement/inspection-tasks")) return Promise.resolve(envelope(TASKS));
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
  render(<AmusementInspectionTasksPage {...fullProps} />);
}

describe("AmusementInspectionTasksPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("点整行 → 抽屉含该行值（时间中文·状态中文）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inspection-task-t-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inspection-task-t-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("t-1");
    expect(drawer.textContent).toContain("dev-9");
    // 到期时间中文化（2026年6月20日 10:00），不得出现 ISO 原文
    expect(drawer.textContent).toContain("2026年6月20日");
    expect(drawer.textContent).not.toContain("2026-06-20T10:00:00Z");
    // 状态中文（overdue→已逾期）
    expect(drawer.textContent).toContain("已逾期");
    expect(drawer.textContent).not.toContain("overdue");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inspection-task-t-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("inspection-task-t-2"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("表格状态/时间列零英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("inspection-tasks-table")).toBeTruthy());
    const table = screen.getByTestId("inspection-tasks-table").textContent ?? "";
    expect(table).not.toContain("overdue");
    expect(table).not.toContain("pending");
    expect(table).not.toContain("2026-06-20T10:00:00Z");
    expect(table).toContain("逾期");
    expect(table).toContain("待巡检");
    expect(table).toContain("2026年6月20日");
  });
});
