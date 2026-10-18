import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const PAYROLL = [{ user_id: "u-9", base_salary_cent: 800000, net_pay_cent: 760000 }];

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("StaffPayrollLeavePage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("待审请假接口 403 → 当月薪资明细主表照常渲染，请假区块降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/staff/leaves")) return Promise.resolve(forbidden());
      if (url.includes("/staff/payroll")) return Promise.resolve(listEnvelope(PAYROLL));
      return Promise.resolve(listEnvelope([]));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<StaffPayrollLeavePage {...props} />);

    await waitFor(() => expect(screen.getByTestId("leave-degraded")).toBeTruthy());
    expect(screen.getByTestId("payroll-table")).toBeTruthy();
    expect(screen.queryByTestId("leave-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });

  it("请假状态 revoked 显示「已销假」，不直出英文原值", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      // 页面同时拉 pending 与 revoke_requested 两段列表：只在 pending 段返回该行，免得同一单重复出现。
      if (url.includes("/staff/leaves")) return Promise.resolve(listEnvelope(url.includes("status=pending") ? [{ id: "lv_revoked", user_id: "u-9", leave_type: "personal", start_date: "2099-09-10", end_date: "2099-09-11", reason: "事假", status: "revoked", review_note: "准" }] : []));
      if (url.includes("/staff/payroll")) return Promise.resolve(listEnvelope(PAYROLL));
      return Promise.resolve(listEnvelope([]));
    }));
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<StaffPayrollLeavePage {...props} />);

    const row = await screen.findByTestId("leave-row-lv_revoked");
    expect(row.textContent).toContain("已销假");
    expect(row.textContent).not.toContain("revoked");
  });
});
