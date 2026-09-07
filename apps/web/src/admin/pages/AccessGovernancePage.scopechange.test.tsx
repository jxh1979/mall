import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AccessGovernancePage } from "./AccessGovernancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const USERS = [{ id: "u-1", username: "张三", account_type: "staff", status: "active" }];
const ROLES = [{ id: "role-1", role_name: "运营经理", role_code: "ops", status: "active" }];

function envelope(items: unknown[]): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }), { status: 200, headers: { "content-type": "application/json" } });
}
function raw(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("/users/u-1/scope") && init?.method === "PUT") {
      return Promise.resolve(raw({ id: "agr_scope_1", request_type: "user_scope_change", target_user_id: "u-1", status: "pending", review_id: "review_scope_1", merchant_id: "merchant_b" }));
    }
    if (url.includes("grant-requests/summary")) return Promise.resolve(raw({ position_recommendations: [], user_anomalies: [] }));
    if (url.includes("grant-requests")) return Promise.resolve(envelope([]));
    if (url.includes("position-role-templates")) return Promise.resolve(envelope([]));
    if (url.includes("access/roles")) return Promise.resolve(envelope(ROLES));
    if (url.includes("access/users")) return Promise.resolve(envelope(USERS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AccessGovernancePage {...props} />);
  return props;
}

// M087-A-04（PGM-1.10.1「保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批」）
describe("AccessGovernancePage 人员范围变更入口", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("提交范围变更 → PUT /users/{id}/scope 带新商户、岗位、职级与最终审批人，结果为中文句子", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("access-user-scope-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("access-user-scope-merchant"), { target: { value: "merchant_b" } });
    fireEvent.change(screen.getByTestId("access-user-scope-position"), { target: { value: "ops_clerk" } });
    fireEvent.change(screen.getByTestId("access-user-scope-level"), { target: { value: "lvl_m1_lower" } });
    fireEvent.change(screen.getByTestId("access-user-scope-reason"), { target: { value: "调往 B 商户" } });
    fireEvent.change(screen.getByTestId("access-user-scope-final-approver"), { target: { value: "usr_boss" } });
    fireEvent.click(screen.getByTestId("access-user-scope-change"));
    const fetchMock = vi.mocked(globalThis.fetch);
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/users/u-1/scope"))).toBe(true));
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes("/users/u-1/scope"));
    const body = JSON.parse(String((call?.[1] as RequestInit | undefined)?.body));
    expect(body.merchant_id).toBe("merchant_b");
    expect(body.position_code).toBe("ops_clerk");
    expect(body.staff_level_id).toBe("lvl_m1_lower");
    expect(body.final_approver_id).toBe("usr_boss");
    expect(body.reason).toBe("调往 B 商户");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.stringMatching(/^已提交用户 u-1 的范围变更：旧派生权限已失效，新范围待审批（复核单 review_scope_1）/)));
  });

  it("未填写理由时页面即拒绝，不发请求", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getByTestId("access-user-scope-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("access-user-scope-merchant"), { target: { value: "merchant_b" } });
    fireEvent.click(screen.getByTestId("access-user-scope-change"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("变更理由")));
    const fetchMock = vi.mocked(globalThis.fetch);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes("/scope"))).toBe(false);
  });
});
