import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([]),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient): void {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StaffPayrollLeavePage {...props} />);
}

// 引导式流程 phase-5：薪资基数录入从内联表单改为「＋薪资基数录入」按钮 → 分步引导弹窗。
describe("StaffPayrollLeavePage 引导式薪资基数录入", () => {
  it("入口→步进→提交携带薪资基数字段", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("payroll-base-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("payroll-base-create-open"));
    // 步骤1 固定工资（无默认值，需填员工 + 月薪）
    fireEvent.change(screen.getByTestId("base-user-input"), { target: { value: "staff-9" } });
    fireEvent.change(screen.getByTestId("base-salary-input"), { target: { value: "8000" } });
    // 单步弹窗：直接进确认步
    fireEvent.click(screen.getByTestId("payroll-base-guided-next"));
    fireEvent.click(screen.getByTestId("base-save"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/staff/payroll/base")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/staff/payroll/base");
    expect(call?.[1]).toMatchObject({ userId: "staff-9", monthlySalaryCent: 800000 });
    // 成功后弹窗关闭
    await waitFor(() => expect(screen.queryByTestId("payroll-base-guided")).toBeNull());
  });

  it("固定工资缺员工 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("payroll-base-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("payroll-base-create-open"));
    // 只填月薪，不填员工 id
    fireEvent.change(screen.getByTestId("base-salary-input"), { target: { value: "8000" } });
    fireEvent.click(screen.getByTestId("payroll-base-guided-next"));
    await waitFor(() => expect(screen.getByTestId("payroll-base-guided-error").textContent).toContain("员工id"));
    expect(screen.queryByTestId("base-save")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/staff/payroll/base")).toBe(false);
  });
});
