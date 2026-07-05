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
// V232 补时间第2批：游乐核销记录 fixture（核销时间 + 对应场次）。
const VERIFICATIONS = [
  { id: "v-1", project_id: "amuse-1", session_id: "s-1", device_id: "dev-1", verification_result: "success", verified_people: 2, created_at: "2026-06-20T10:15:00Z" },
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
      if (url.includes("/amusement/verifications")) return Promise.resolve(envelope(VERIFICATIONS));
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

  it("V232：核销记录展示核销时间 + 对应场次（场次以名称呈现）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-verifications-table")).toBeTruthy());
    const table = screen.getByTestId("amusement-verifications-table");
    expect(table.textContent).toContain("2026-06-20T10:15:00Z");
    expect(table.textContent).toContain("上午场");
  });
});

describe("AmusementSessionsPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/amusement/sessions");
  });

  it("点场次整行 → 抽屉含该行值（时间中文·状态中文）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-session-row-s-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-session-row-s-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("上午场");
    expect(drawer.textContent).toContain("2026年6月20日 10:00");
    expect(drawer.textContent).not.toContain("2026-06-20T10:00:00Z");
    expect(drawer.textContent).not.toContain("open");
  });

  it("点项目整行 → 抽屉含该行值（状态中文）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-project-row-amuse-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-project-row-amuse-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("旋转木马");
    expect(drawer.textContent).toContain("AM-1");
    expect(drawer.textContent).not.toContain("active");
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-session-row-s-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("amusement-session-row-s-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("场次表时间列零英文 ISO", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-sessions-table")).toBeTruthy());
    const table = screen.getByTestId("amusement-sessions-table").textContent ?? "";
    expect(table).not.toContain("2026-06-20T10:00:00Z");
    expect(table).toContain("2026年6月20日");
  });
});
