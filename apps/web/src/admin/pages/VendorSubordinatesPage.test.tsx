import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VendorSubordinatesPage } from "./VendorSubordinatesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const SUBORDINATES = [
  { id: "s-1", username: "现场员工甲", partner_org_id: "org-1", account_type: "outsourced_vendor", status: "active" },
  { id: "s-2", username: "现场员工乙", partner_org_id: "org-1", account_type: "outsourced_vendor", status: "disabled" },
];
const COMPLETION = [
  { subordinate_id: "s-1", username: "现场员工甲", status: "active", work_order_total: 5, work_order_completed: 3, inspection_total: 4, inspection_completed: 4 },
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
    if (url.includes("/completion")) return Promise.resolve(envelope(COMPLETION));
    if (url.includes("/subordinates")) return Promise.resolve(envelope(SUBORDINATES));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "机构管理员", accountType: "outsourced_vendor" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VendorSubordinatesPage {...props} />);
}

describe("VendorSubordinatesPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/vendor/subordinates");
  });

  it("状态列为中文，不含英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-subordinates-table")).toBeTruthy());
    const table = screen.getByTestId("vendor-subordinates-table").textContent ?? "";
    expect(table).toContain("有效");
    expect(table).not.toContain("active");
    expect(table).not.toContain("disabled");
  });

  it("点下属账号行 → 抽屉含该行字段（状态中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-subordinate-s-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-subordinate-s-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("现场员工甲");
    expect(drawer.textContent).toContain("有效");
    expect(drawer.textContent).not.toContain("active");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点完成情况行 → 抽屉含工单/巡检完成数", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-subordinate-completion-s-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-subordinate-completion-s-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("3/5");
    expect(drawer.textContent).toContain("4/4");
  });

  it("点行内“停用”按钮不触发抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("vendor-subordinate-disable-s-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("vendor-subordinate-disable-s-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
