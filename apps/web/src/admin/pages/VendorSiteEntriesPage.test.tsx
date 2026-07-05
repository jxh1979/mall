import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VendorSiteEntriesPage } from "./VendorSiteEntriesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const ENTRIES = [
  { id: "se-1", work_scope: "围挡施工", project_id: "venue-1", space_id: "area-1", status: "in_progress", vendor_user_id: "u1" },
  { id: "se-2", work_scope: "已完工撤场", project_id: "venue-1", space_id: "area-2", status: "completed", vendor_user_id: "u1" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("site-entry-reports")) return Promise.resolve(envelope(ENTRIES));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "施工队甲", accountType: "outsourced_vendor" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VendorSiteEntriesPage {...props} />);
}

describe("VendorSiteEntriesPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/site-entries");
  });

  it("点“可进场” → URL 带 ?kpi=site-entries&subset=enterable，聚焦只剩可进场行", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-site-entries-kpi-enterable")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-site-entries-kpi-enterable"));
    expect(window.location.search).toContain("kpi=site-entries");
    expect(window.location.search).toContain("subset=enterable");
    expect(screen.getByTestId("vendor-site-entries-kpi-focus-banner").textContent).toContain("正在查看：可进场报备（1）");
    expect(screen.getByTestId("vendor-site-entry-select-se-1")).toBeTruthy();
    expect(screen.queryByTestId("vendor-site-entry-select-se-2")).toBeNull();
    expect(screen.queryByTestId("vendor-site-entries-summary")).toBeNull();
    expect(screen.queryByTestId("vendor-site-entry-create-form")).toBeNull();
  });

  it("带参直链进入 → 直接呈现聚焦视图，点「返回全部」恢复全量+表单", async () => {
    window.history.replaceState({}, "", "/admin/vendor/site-entries?kpi=site-entries&subset=enterable");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-site-entries-kpi-focus-banner")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-site-entries-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("vendor-site-entries-summary")).toBeTruthy();
    expect(screen.getByTestId("vendor-site-entry-select-se-2")).toBeTruthy();
  });

  it("状态列与子标签为中文，不含英文枚举/status", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-site-entries-summary")).toBeTruthy());
    const summary = screen.getByTestId("vendor-site-entries-summary").textContent ?? "";
    expect(summary).not.toContain("status");
    const table = screen.getByTestId("vendor-site-entries-table").textContent ?? "";
    expect(table).toContain("进行中");
    expect(table).not.toContain("in_progress");
  });
});
