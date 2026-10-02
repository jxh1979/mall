import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessGovernancePage } from "./AccessGovernancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-11：新建授权申请(grant-requests)改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 只覆盖「授权申请」主创建流；岗位角色模板弹窗(phase-9)/审批/定岗/装配角色不在本期改造范围。
const USERS = [{ id: "u-1", username: "张三", account_type: "staff", status: "active" }];
const ROLES = [{ id: "role-1", role_name: "运营经理", role_code: "ops", status: "active" }];

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
      if ((init?.method ?? "GET").toUpperCase() === "POST" && url.endsWith("/grant-requests")) {
        lastPostUrl = url;
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        return Promise.resolve(raw({ id: "gr-new", status: "pending" }));
      }
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
      if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
      if (url.includes("position-role-templates")) return Promise.resolve(envelope([]));
      if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
      if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS) {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions,
  };
  render(<AccessGovernancePage {...props} />);
  return { setActionResult };
}

describe("AccessGovernancePage 引导式新建授权申请(phase-11)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
  });

  it("页面默认只留入口按钮，不展示授权申请字段", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-request-create-open")).toBeTruthy());
    expect(screen.queryByLabelText("临时权限码")).toBeNull();
    expect(screen.queryByTestId("access-grant-request-guided")).toBeNull();
    // 岗位角色模板弹窗入口仍在（phase-9 未被本期触碰）。
    expect(screen.getByTestId("access-position-role-template-create-open")).toBeTruthy();
  });

  it("入口→填权限码/过期时间→步进→提交：POST 带 requestType/targetUserId/permissionCodes，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-request-create-open")).toBeTruthy());
    // 等用户名单加载，弹窗打开时 targetUserId 默认落到首个用户。
    await waitFor(() => expect(screen.getByTestId("access-grant-review-form")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));

    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步1→步2（类型默认临时权限，目标已默认首个用户）
    fireEvent.change(screen.getByLabelText("临时权限码"), { target: { value: "access.role.assign,finance.view" } });
    fireEvent.change(screen.getByLabelText("过期时间"), { target: { value: "2026-09-01T10:00" } });
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步2→确认
    await waitFor(() => expect(screen.getByTestId("access-grant-request-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/grant-requests");
    expect((lastPostBody as { request_type: string }).request_type).toBe("temporary_permission");
    expect((lastPostBody as { target_user_id: string }).target_user_id).toBe("u-1");
    expect((lastPostBody as { permission_codes: string[] }).permission_codes).toEqual(["access.role.assign", "finance.view"]);
    await waitFor(() => expect(screen.queryByTestId("access-grant-request-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^授权申请已提交/)); // PGM-1.18：结果提示全中文
  });

  it("步内校验拦截：临时权限缺权限码/过期时间则不进确认、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-request-create-open")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("access-grant-review-form")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next")); // 步1→步2
    // 临时权限码/过期时间留空 → 步2 校验拦截。
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next"));
    expect(screen.getByTestId("access-grant-request-guided-error").textContent).toContain("权限码");
    expect(screen.queryByTestId("access-grant-request-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });

  // M002-F01：员工单独授予只填权限码——留空被步内校验拦截；填写后提交带 permissionCodes，不带过期时间与单次有效。
  it("员工单独授予：权限码留空被拦截，填写后提交只带权限码（M002-F01）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-request-create-open")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("access-grant-review-form")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
    fireEvent.change(screen.getByLabelText("申请类型"), { target: { value: "user_permission_grant" } });
    fireEvent.change(screen.getByLabelText("权限码"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next"));
    expect(screen.getByTestId("access-grant-request-guided-error").textContent).toContain("员工单独授予、解除显式拒绝与岗位高危权限释放需填写权限码");
    expect(screen.queryByTestId("access-grant-request-guided-summary")).toBeNull();

    fireEvent.change(screen.getByLabelText("权限码"), { target: { value: "finance.refund.create" } });
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next"));
    await waitFor(() => expect(screen.getByTestId("access-grant-request-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    const body = lastPostBody as Record<string, unknown>;
    expect(body.request_type).toBe("user_permission_grant");
    expect(body.target_user_id).toBe("u-1");
    expect(body.permission_codes).toEqual(["finance.refund.create"]);
    expect(body).not.toHaveProperty("expires_at");
    expect(body).not.toHaveProperty("single_use");
  });

  // M002-F20：释放单被驳回后，对同一挂起再次发起「岗位高危权限释放」申请——下拉可选，只填权限码，提交带 position_high_risk_release。
  it("岗位高危权限释放：可选该类型，权限码留空被拦截，填写后提交只带权限码（M002-F20）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-request-create-open")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("access-grant-review-form")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
    fireEvent.change(screen.getByLabelText("申请类型"), { target: { value: "position_high_risk_release" } });
    expect((screen.getByLabelText("申请类型") as HTMLSelectElement).value).toBe("position_high_risk_release");
    fireEvent.change(screen.getByLabelText("权限码"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next"));
    expect(screen.getByTestId("access-grant-request-guided-error").textContent).toContain("岗位高危权限释放需填写权限码");

    fireEvent.change(screen.getByLabelText("权限码"), { target: { value: "finance.refund.create" } });
    fireEvent.click(screen.getByTestId("access-grant-request-guided-next"));
    await waitFor(() => expect(screen.getByTestId("access-grant-request-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    const body = lastPostBody as Record<string, unknown>;
    expect(body.request_type).toBe("position_high_risk_release");
    expect(body.target_user_id).toBe("u-1");
    expect(body.permission_codes).toEqual(["finance.refund.create"]);
    expect(body).not.toHaveProperty("expires_at");
    expect(body).not.toHaveProperty("single_use");
    expect(body).not.toHaveProperty("position_code");
  });

  // M002-F37（治理地图第 651 行）：员工单独授予、解除员工显式拒绝、岗位高危权限释放三类申请只能由具备分配/取消分配角色权限的人发起——
  // 不持 access.role.assign（或该码被显式拒绝）时，申请类型下拉只剩临时权限、用户角色、用户职位；持该码时三类照常可选。
  const APPLY_ONLY = ["access.grant_request.apply", "access.grant_request.view", "access.user.view", "access.role.view"];
  const requestTypeOptions = async (permissions: EffectivePermissionsState) => {
    renderPage(permissions);
    await waitFor(() => expect(screen.getByTestId("access-grant-request-create-open")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("access-grant-review-form")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-create-open"));
    return Array.from((screen.getByLabelText("申请类型") as HTMLSelectElement).options).map((option) => option.value);
  };

  it("不持分配角色权限：申请类型不提供三类（M002-F37）", async () => {
    expect(await requestTypeOptions({ status: "ready", value: { grants: APPLY_ONLY, denies: [], wildcard: false } }))
      .toEqual(["temporary_permission", "user_role", "user_position"]);
  });

  it("分配角色权限被显式拒绝：申请类型同样不提供三类（M002-F37）", async () => {
    expect(await requestTypeOptions({ status: "ready", value: { grants: [...APPLY_ONLY, "access.role.assign"], denies: ["access.role.assign"], wildcard: false } }))
      .toEqual(["temporary_permission", "user_role", "user_position"]);
  });

  it("持分配角色权限：三类照常可选（M002-F37 对照）", async () => {
    expect(await requestTypeOptions({ status: "ready", value: { grants: [...APPLY_ONLY, "access.role.assign"], denies: [], wildcard: false } }))
      .toEqual(["temporary_permission", "user_role", "user_position", "user_permission_grant", "user_permission_deny_removal", "position_high_risk_release"]);
  });
});
