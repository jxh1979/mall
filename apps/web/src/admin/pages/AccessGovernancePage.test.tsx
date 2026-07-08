import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
const SUMMARY = {
  position_recommendations: [{ position_code: "ops_manager", staff_level_id: "lvl_m1", permission_code: "access.role.assign", request_count: 3, recommendation: "建议固化进岗位模板" }],
  user_anomalies: [{ user_id: "u-1", request_count: 5, summary: "临时授权过于频繁" }],
};

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

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("grant-requests/summary")) return Promise.resolve(raw(SUMMARY));
      if (url.includes("grant-requests")) return Promise.resolve(envelope(GRANTS));
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
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<AccessGovernancePage {...fullProps} />);
}

describe("AccessGovernancePage 行明细抽屉 + 零英文", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("点岗位模板行 → 抽屉出现且含岗位/角色（角色中文名）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-position-role-template-prt-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).not.toContain("ops_manager"); // 岗位码已中文化
    expect(drawer.textContent).toContain("运营经理");
    expect(drawer.textContent).toContain("有效");
    expect(drawer.textContent).not.toContain("active");
  });

  it("点授权申请行 → 抽屉含状态中文与中文过期时间", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-request-gr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-request-gr-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("临时权限");
    expect(drawer.textContent).toContain("待处理");
    expect(drawer.textContent).toContain("2026年8月1日");
    expect(drawer.textContent).not.toContain("pending");
  });

  it("点智能总结行 → 抽屉含岗位建议内容", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-summary-position-ops_manager")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-summary-position-ops_manager"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("岗位");
    expect(drawer.textContent).toContain("建议固化进岗位模板");
  });

  it("点操作按钮不误触抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-actions-gr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-actions-gr-1"));
    await waitFor(() => expect(screen.getByTestId("access-grant-approve-gr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-grant-approve-gr-1"));
    // 审批会触发 reload；抽屉不应因点操作按钮弹出。
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-position-role-template-prt-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("access-position-role-template-prt-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("授权申请表状态/类型为中文，审批按钮为中文，不含原英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-requests-table")).toBeTruthy());
    const text = screen.getByTestId("access-grant-requests-table").textContent ?? "";
    expect(text).toContain("临时权限");
    expect(text).toContain("待处理");
    fireEvent.click(screen.getByTestId("access-grant-actions-gr-1"));
    await waitFor(() => expect(screen.getByTestId("access-grant-approve-gr-1")).toBeTruthy());
    expect(screen.getByTestId("access-grant-approve-gr-1").textContent).toContain("通过");
    expect(screen.getByTestId("access-grant-reject-gr-1").textContent).toContain("驳回");
    expect(text).not.toContain("pending");
    expect(text).not.toContain("approve");
    expect(text).not.toContain("reject");
  });

  it("智能总结表首列为中文（岗位/用户），不含 position/user 原文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("access-grant-summary-table")).toBeTruthy());
    const text = screen.getByTestId("access-grant-summary-table").textContent ?? "";
    expect(text).toContain("岗位");
    expect(text).toContain("用户");
    expect(text).not.toContain("position");
    expect(text).not.toMatch(/\buser\b/);
  });
});
