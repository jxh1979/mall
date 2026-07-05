import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

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

describe("AuditEventsPage KPI 聚焦视图", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  it("点“本页成功”数字 → ?kpi=events&status=success，横幅+表只剩成功子集，KPI 卡/控件隐藏", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-kpi-success")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-kpi-success"));
    expect(window.location.search).toContain("kpi=events");
    expect(window.location.search).toContain("status=success");
    expect(screen.getByTestId("audit-events-kpi-focus-banner").textContent).toContain("正在查看：本页成功事件（1）");
    expect(screen.queryByTestId("audit-events-summary")).toBeNull();
    expect(screen.queryByTestId("audit-events-controls")).toBeNull();
    const text = screen.getByTestId("audit-events-table").textContent ?? "";
    expect(text).toContain("登录成功");
    expect(text).not.toContain("越权阻断");
  });

  it("点“本页失败/阻断”数字 → 失败子集(1)", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-kpi-failed")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-kpi-failed"));
    expect(screen.getByTestId("audit-events-kpi-focus-banner").textContent).toContain("（1）");
    const text = screen.getByTestId("audit-events-table").textContent ?? "";
    expect(text).toContain("越权阻断");
    expect(text).not.toContain("登录成功");
  });

  it("「返回全部」→ 恢复 KPI 卡+控件+全量表", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-kpi-success")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-kpi-success"));
    fireEvent.click(screen.getByTestId("audit-events-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("audit-events-summary")).toBeTruthy();
    expect(screen.getByTestId("audit-events-controls")).toBeTruthy();
    expect(screen.getByTestId("audit-events-table").textContent).toContain("越权阻断");
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/audit/events?kpi=events&status=failed");
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("audit-events-summary")).toBeNull();
    expect(screen.getByTestId("audit-events-table").textContent).not.toContain("登录成功");
  });

  it("分类/来源渲染为中文映射（eventCategory security→安防）", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-table")).toBeTruthy());
    expect(screen.getByTestId("audit-events-table").textContent).toContain("安防");
  });
});

describe("AuditEventsPage 行明细抽屉", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/audit/events");
  });

  it("点事件行 → 抽屉含该行分类（中文）与摘要，无英文枚举，Esc 关闭", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-row-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-row-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("安防");
    expect(drawer.textContent).toContain("越权阻断");
    expect(drawer.textContent).not.toContain("security");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("点行内单选按钮不打开抽屉（stopPropagation）", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("audit-events-select-0")).toBeTruthy());
    fireEvent.click(screen.getByTestId("audit-events-select-0"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
