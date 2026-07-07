import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessGovernancePage } from "./AccessGovernancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-9：新增岗位角色模板改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 只覆盖岗位角色模板主创建流；授权申请/审批工作流不在本期改造范围。
const USERS = [{ id: "u-1", username: "张三", account_type: "staff", status: "active" }];
const ROLES = [{ id: "role-1", role_name: "运营经理", role_code: "ops", status: "active" }];
const TEMPLATES = [
  { id: "prt-1", position_code: "ops_manager", staff_level_id: "lvl_m1", role_id: "role-1", template_id: "tpl-1", organization_id: "org-1", status: "active" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function raw(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
let lastPostUrl = "";
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("position-role-templates")) {
        lastPostUrl = url;
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(raw({ id: "prt-new" }));
      }
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
      if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
      if (url.includes("position-role-templates")) return Promise.resolve(envelope(TEMPLATES));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AccessGovernancePage {...props} />);
  return { setActionResult };
}

describe("AccessGovernancePage 引导式新增岗位角色模板(phase-9)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
  });

  it("页面默认只留入口按钮，不展示模板字段", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
    expect(screen.queryByTestId("access-position-role-template-position")).toBeNull();
    expect(screen.queryByTestId("access-position-role-template-guided")).toBeNull();
  });

  it("入口→步进→提交：POST 新建模板带岗位/职级/角色，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
    // 等角色名单加载完，弹窗打开时 roleId 默认落到首个角色。
    await waitFor(() => expect(screen.getByTestId("access-position-role-templates-table").textContent).toContain("prt-1"));
    fireEvent.click(screen.getByTestId("access-position-role-template-create-open"));

    fireEvent.change(screen.getByTestId("access-position-role-template-position"), { target: { value: "front_desk" } });
    fireEvent.change(screen.getByTestId("access-position-role-template-level"), { target: { value: "lvl_p1" } });
    fireEvent.click(screen.getByTestId("access-position-role-template-guided-next")); // 步1→步2
    fireEvent.click(screen.getByTestId("access-position-role-template-guided-next")); // 步2→确认
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-position-role-template-save"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).not.toContain("prt-"); // 新建走 POST 集合路径，非 PUT /{id}
    expect((lastPostBody as { position_code: string }).position_code).toBe("front_desk");
    expect((lastPostBody as { staff_level_id: string }).staff_level_id).toBe("lvl_p1");
    expect((lastPostBody as { role_id: string }).role_id).toBe("role-1");
    await waitFor(() => expect(screen.queryByTestId("access-position-role-template-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "access_position_role_template_saved" }));
  });

  it("步内校验拦截：岗位清空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-position-role-template-create-open"));
    fireEvent.change(screen.getByTestId("access-position-role-template-position"), { target: { value: "  " } });
    fireEvent.click(screen.getByTestId("access-position-role-template-guided-next"));
    expect(screen.getByTestId("access-position-role-template-guided-error").textContent).toContain("岗位");
    expect(screen.queryByTestId("access-position-role-template-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
