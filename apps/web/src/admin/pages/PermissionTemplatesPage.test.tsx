import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PermissionTemplatesPage } from "./PermissionTemplatesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const TEMPLATES = [
  { id: "tpl-1", template_name: "基础员工", template_type: "base", status: "active", permission_codes: ["access.user.manage", "finance.view"] },
  { id: "tpl-2", template_name: "临时提权", template_type: "temporary", status: "inactive", valid_until: "2026-08-01T00:00:00Z", single_use: true, permission_codes: ["access.role.assign"] },
];
const PERMISSIONS = [
  { permission_code: "access.user.manage", permission_name: "管理员工", domain: "access" },
  { permission_code: "finance.view", permission_name: "查看财务", domain: "finance" },
];
const USERS = [{ id: "u-1", username: "张三", account_type: "staff", status: "active" }];

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
      if (url.includes("permission-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/permissions")) return Promise.resolve(envelope(PERMISSIONS));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
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
  render(<PermissionTemplatesPage {...fullProps} />);
}

describe("PermissionTemplatesPage 行明细抽屉 + 零英文", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("点模板行 → 抽屉出现且含该模板字段（类型中文/有效性/权限点）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-row-tpl-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("permission-template-row-tpl-2"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("临时提权");
    expect(drawer.textContent).toContain("临时模板");
    expect(drawer.textContent).toContain("单次有效");
    expect(drawer.textContent).toContain("access.role.assign");
    expect(drawer.textContent).not.toContain("temporary");
  });

  it("点模板行第一格单选不误触抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-select-tpl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("permission-template-select-tpl-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-row-tpl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("permission-template-row-tpl-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("模板表类型/状态列为中文，不含原英文枚举 base/temporary/inactive", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-templates-table")).toBeTruthy());
    const text = screen.getByTestId("permission-templates-table").textContent ?? "";
    expect(text).toContain("基础模板");
    expect(text).toContain("临时模板");
    expect(text).toContain("有效");
    expect(text).toContain("未激活");
    expect(text).not.toContain("temporary");
    expect(text).not.toContain("inactive");
  });
});
