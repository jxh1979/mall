import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const PROJECTS = [
  { id: "amuse-1", project_code: "AM-1", project_name: "旋转木马", capacity: 30, status: "active" },
];
const SESSIONS = [
  { id: "s-1", project_id: "amuse-1", session_name: "上午场", starts_at: "2026-06-20T10:00:00Z", ends_at: "2026-06-20T11:00:00Z", capacity: 12, used_capacity: 3, locked_capacity: 2, status: "open" },
  { id: "s-2", project_id: "amuse-1", session_name: "下午场", starts_at: "2026-06-20T14:00:00Z", ends_at: "2026-06-20T15:00:00Z", capacity: 12, used_capacity: 0, locked_capacity: 4, status: "scheduled" },
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
      if (url.includes("/amusement/projects")) return Promise.resolve(envelope(PROJECTS));
      if (url.includes("/amusement/sessions")) return Promise.resolve(envelope(SESSIONS));
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
  render(<AmusementSessionsPage {...fullProps} />);
}

describe("AmusementSessionsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/amusement/sessions");
  });

  it("点“场次”数字 → URL 带 ?kpi=sessions，进入聚焦视图，只剩场次表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-sessions-kpi-sessions")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-sessions-kpi-sessions"));
    expect(window.location.search).toContain("kpi=sessions");
    expect(screen.getByTestId("amusement-sessions-kpi-focus-banner").textContent).toContain("正在查看：场次（2）");
    expect(screen.getByTestId("amusement-sessions-table")).toBeTruthy();
    expect(screen.queryByTestId("amusement-projects-table")).toBeNull();
    expect(screen.queryByTestId("amusement-sessions-summary")).toBeNull();
    expect(screen.queryByTestId("amusement-session-name")).toBeNull();
  });

  it("点“项目”数字 → 聚焦视图只剩项目表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-sessions-kpi-projects")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-sessions-kpi-projects"));
    expect(window.location.search).toContain("kpi=projects");
    expect(screen.getByTestId("amusement-sessions-kpi-focus-banner").textContent).toContain("正在查看：项目（1）");
    expect(screen.getByTestId("amusement-projects-table")).toBeTruthy();
    expect(screen.queryByTestId("amusement-sessions-table")).toBeNull();
  });

  it("聚焦视图点「返回全部」→ 参数清除，恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-sessions-kpi-sessions")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-sessions-kpi-sessions"));
    fireEvent.click(screen.getByTestId("amusement-sessions-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("amusement-sessions-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("amusement-projects-table")).toBeTruthy();
    expect(screen.getByTestId("amusement-sessions-table")).toBeTruthy();
    expect(screen.getByTestId("amusement-sessions-summary")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/amusement/sessions?kpi=sessions");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-sessions-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("amusement-sessions-table")).toBeTruthy();
    expect(screen.queryByTestId("amusement-projects-table")).toBeNull();
  });

  it("KPI 子标签为中文，不含英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-sessions-summary")).toBeTruthy());
    const text = screen.getByTestId("amusement-sessions-summary").textContent ?? "";
    expect(text).not.toContain("projects");
    expect(text).not.toContain("sessions");
    expect(text).not.toContain("capacity");
    expect(text).not.toContain("locked");
    expect(text).toContain("项目数");
    expect(text).toContain("已锁定容量");
  });
});
