import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessGovernancePage } from "./AccessGovernancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const USERS = [{ id: "u-1", username: "张三", account_type: "staff", status: "active" }];
const ROLES = [{ id: "role-1", role_name: "运营经理", role_code: "ops", status: "active" }];
const TEMPLATES = [
  { id: "prt-1", position_code: "ops_manager", staff_level_id: "lvl_m1", role_id: "role-1", template_id: "tpl-1", organization_id: "org-1", status: "active" },
];
const GRANTS = [
  { id: "gr-1", request_type: "temporary_permission", target_user_id: "u-1", permission_codes: ["access.role.assign"], status: "pending", single_use: true, expires_at: "2026-08-01T10:00:00Z", reason: "临时救火", created_at: "2026-07-05T09:00:00Z" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function forbidden(): Response {
  return new Response(
    JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }),
    { status: 403, headers: { "content-type": "application/json" } },
  );
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("AccessGovernancePage 智能总结副资源降级（批2）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("总结接口 403 → 角色装配/审批主表照常，总结区块显示无权限降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("grant-requests/summary")) return Promise.resolve(forbidden());
      if (url.includes("grant-requests")) return Promise.resolve(envelope(GRANTS));
      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError,
      effectivePermissions: ADMIN_PERMS,
    };
    render(<AccessGovernancePage {...props} />);
    // 主资源：岗位模板表 + 授权申请表照常渲染（Tabs forceRender 使各面板都在 DOM）
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-1")).toBeTruthy());
    expect(screen.getByTestId("access-grant-request-gr-1")).toBeTruthy();
    // 总结副资源 403 → 无权限降级提示
    expect(screen.getByTestId("access-grant-summary-degraded").textContent).toContain("无权");
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
