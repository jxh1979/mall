import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VendorInspectionsPage } from "./VendorInspectionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const TASKS = [
  { id: "it-1", title: "夜间安防巡检", inspection_type: "security", status: "scheduled", schedule_mode: "template", assignee_id: "u-9", template_id: "tpl-1" },
  { id: "it-2", title: "已完成巡检", inspection_type: "fire", status: "completed", schedule_mode: "route", assignee_id: "u-9", template_id: "tpl-1" },
];
const TEMPLATES = [{ id: "tpl-1", name: "安防模板", inspection_type: "security", cycle_days: 1, abnormal_mode: "to_work_order", active: true, checklist: ["门禁", "监控"] }];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/templates")) return Promise.resolve(envelope(TEMPLATES));
    if (url.includes("/tasks")) return Promise.resolve(envelope(TASKS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "vendor", accountType: "outsourced_vendor" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VendorInspectionsPage {...props} />);
}

describe("VendorInspectionsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/inspections");
  });

  it("点“待记录” → URL 带 ?kpi=inspection-tasks&subset=pending，聚焦只剩未完成任务", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-inspections-kpi-pending")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-inspections-kpi-pending"));
    expect(window.location.search).toContain("kpi=inspection-tasks");
    expect(window.location.search).toContain("subset=pending");
    expect(screen.getByTestId("vendor-inspections-kpi-focus-banner").textContent).toContain("正在查看：待记录任务（1）");
    expect(screen.getByTestId("vendor-inspection-select-it-1")).toBeTruthy();
    expect(screen.queryByTestId("vendor-inspection-select-it-2")).toBeNull();
    expect(screen.queryByTestId("vendor-inspections-summary")).toBeNull();
    expect(screen.queryByTestId("vendor-inspection-templates-table")).toBeNull();
  });

  it("聚焦后点「返回全部」→ 恢复 KPI 卡+模板表+全量任务", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-inspections-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-inspections-kpi-total"));
    fireEvent.click(screen.getByTestId("vendor-inspections-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("vendor-inspections-summary")).toBeTruthy();
    expect(screen.getByTestId("vendor-inspection-select-it-2")).toBeTruthy();
  });

  it("KPI 子标签与列为中文，不含 assignee/status/英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-inspections-summary")).toBeTruthy());
    const summary = screen.getByTestId("vendor-inspections-summary").textContent ?? "";
    expect(summary).not.toContain("assignee");
    expect(summary).not.toContain("status");
    const table = screen.getByTestId("vendor-inspections-table").textContent ?? "";
    expect(table).toContain("安防");
    expect(table).toContain("已排期");
    expect(table).not.toContain("security");
    expect(table).not.toContain("scheduled");
  });
});

describe("VendorInspectionsPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/inspections");
  });

  it("点巡检任务行 → 抽屉含该任务字段（类型中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-inspection-row-it-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-inspection-row-it-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("夜间安防巡检");
    expect(drawer.textContent).toContain("安防");
    expect(drawer.textContent).not.toContain("security");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点模板行 → 抽屉含模板字段，异常模式中文无英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-inspection-template-row-tpl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-inspection-template-row-tpl-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("安防模板");
    expect(drawer.textContent).toContain("转工单");
    expect(drawer.textContent).not.toContain("to_work_order");
  });
});


describe("VendorInspectionsPage 副资源降级（AUD-032）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/inspections");
  });

  function forbidden(): Response {
    return new Response(
      JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }),
      { status: 403, headers: { "content-type": "application/json" } },
    );
  }

  it("模板接口 403 → 巡检任务主表照常渲染，模板区块显示无权限降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/templates")) return Promise.resolve(forbidden());
      if (url.includes("/tasks")) return Promise.resolve(envelope(TASKS));
      return Promise.resolve(envelope([]));
    }));
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "vendor", accountType: "outsourced_vendor" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
    };
    render(<VendorInspectionsPage {...props} />);
    await waitFor(() => expect(screen.getByTestId("vendor-inspection-row-it-1")).toBeTruthy());
    expect(screen.getByTestId("vendor-inspections-table")).toBeTruthy();
    expect(screen.getByTestId("vendor-inspection-templates-degraded").textContent).toContain("无权");
    expect(screen.queryByTestId("vendor-inspection-templates-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(props.setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
