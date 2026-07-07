import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PermissionTemplatesPage } from "./PermissionTemplatesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-11：新建权限模板改分步引导弹窗（模板信息→选权限→确认）。
// 只覆盖「新建权限模板」主创建流；复制/应用/选中编辑动作不在本期改造范围。
const TEMPLATES = [
  { id: "tpl-1", template_name: "基础员工", template_type: "base", status: "active", permission_codes: ["access.user.manage"] },
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
      const method = (init?.method ?? "GET").toUpperCase();
      if (method === "POST" && url.endsWith("/permission-templates")) {
        lastPostUrl = url;
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(raw({ id: "tpl-new" }));
      }
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
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<PermissionTemplatesPage {...props} />);
  return { setActionResult };
}

describe("PermissionTemplatesPage 引导式新建权限模板(phase-11)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
  });

  it("页面默认只留入口按钮，不展示新建弹窗", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-create-open")).toBeTruthy());
    expect(screen.queryByTestId("permission-template-create-guided")).toBeNull();
  });

  it("入口→填名→选权限→确认→提交：POST 集合路径带 templateName/permissionCodes，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-create-open")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("permission-templates-table").textContent).toContain("基础员工"));
    fireEvent.click(screen.getByTestId("permission-template-create-open"));

    fireEvent.change(screen.getByTestId("permission-template-name"), { target: { value: "夜班主管" } });
    fireEvent.click(screen.getByTestId("permission-template-create-guided-next")); // 步1→步2 选权限
    fireEvent.click(screen.getByTestId("permission-template-new-code-finance.view"));
    fireEvent.click(screen.getByTestId("permission-template-create-guided-next")); // 步2→确认
    await waitFor(() => expect(screen.getByTestId("permission-template-create-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("permission-template-save"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/permission-templates");
    expect(lastPostUrl).not.toMatch(/permission-templates\/tpl-/); // 新建走 POST 集合路径，非 PUT /{id}
    expect((lastPostBody as { template_name: string }).template_name).toBe("夜班主管");
    expect((lastPostBody as { permission_codes: string[] }).permission_codes).toContain("finance.view");
    await waitFor(() => expect(screen.queryByTestId("permission-template-create-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "保存权限模板" }));
  });

  it("步内校验拦截：未选任何权限则不进确认、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("permission-template-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("permission-template-create-open"));
    fireEvent.change(screen.getByTestId("permission-template-name"), { target: { value: "夜班主管" } });
    fireEvent.click(screen.getByTestId("permission-template-create-guided-next")); // 步1→步2
    // 步2 不勾任何权限直接下一步 → 校验拦截。
    fireEvent.click(screen.getByTestId("permission-template-create-guided-next"));
    expect(screen.getByTestId("permission-template-create-guided-error").textContent).toContain("权限点");
    expect(screen.queryByTestId("permission-template-create-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
