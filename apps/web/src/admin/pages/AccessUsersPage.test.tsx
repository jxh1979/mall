import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessUsersPage } from "./AccessUsersPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。access 域 snake_case 响应。
const USERS = [
  { id: "u-1", username: "张三", account_type: "supervisor", status: "active", phone: "13800000001", organization_id: "org-1", merchant_id: "m-1" },
  { id: "u-2", username: "李四", account_type: "sales", status: "disabled", organization_id: "org-1" },
];
const TEMPLATES = [{ id: "tpl-1", template_name: "基础员工", permission_codes: ["a.b"] }];
const ENTITIES = [{ id: "org-1", name: "总部主体", code: "ORG-1" }];
const MERCHANTS = [{ id: "m-1", display_name: "商户甲" }];

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
      if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
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
  render(<AccessUsersPage {...fullProps} />);
}

describe("AccessUsersPage 行明细抽屉 + 零英文", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("点员工行 → 抽屉出现且含该行字段值（账号/类型中文/组织显名）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-user-row-u-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-user-row-u-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("张三");
    expect(drawer.textContent).toContain("主管");
    expect(drawer.textContent).toContain("总部主体");
    expect(drawer.textContent).toContain("商户甲");
    expect(drawer.textContent).toContain("13800000001");
  });

  it("Esc 关闭抽屉 + 点遮罩关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-user-row-u-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-user-row-u-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
    fireEvent.click(screen.getByTestId("access-user-row-u-2"));
    fireEvent.click(screen.getByTestId("row-detail-overlay"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("表格状态/账号类型列为中文，不含原英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-users-table")).toBeTruthy());
    const text = screen.getByTestId("access-users-table").textContent ?? "";
    expect(text).toContain("主管");
    expect(text).toContain("销售人员");
    expect(text).toContain("有效");
    expect(text).toContain("已禁用");
    expect(text).not.toContain("supervisor");
    expect(text).not.toContain("active");
    expect(text).not.toContain("disabled");
  });
});
