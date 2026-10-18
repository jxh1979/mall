// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

/** CG-12（BSM:1487 / ENUM:283）：奖惩只发起 人力会签＋直属领导向上三级复核；员工对已生效扣罚可申诉，同链裁定。 */
describe("StaffPayrollLeavePage 奖惩复核链与扣罚申诉（CG-12）", () => {
  function makeApi(getImpl: (url: string) => Promise<unknown>) {
    return {
      get: vi.fn(async (url: string) => getImpl(url)),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({ reviewId: "review_adj_1", id: "apl_1" }),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }

  function renderPage(api: AdminApiClient) {
    const setActionResult = vi.fn();
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<StaffPayrollLeavePage {...props} />);
    return { setActionResult, setGlobalError };
  }

  it("奖惩填写事由后发起复核，HR自动解析且不随单传入", async () => {
    const api = makeApi(async () => []);
    const { setActionResult, setGlobalError } = renderPage(api);
    await screen.findByTestId("payroll-adj-editor");
    fireEvent.change(screen.getByTestId("adj-user-input"), { target: { value: "usr_e" } });
    fireEvent.change(screen.getByTestId("adj-type-select"), { target: { value: "penalty" } });
    fireEvent.change(screen.getByTestId("adj-amount-input"), { target: { value: "30" } });
    fireEvent.change(screen.getByTestId("adj-reason-input"), { target: { value: "迟到" } });
    expect(screen.queryByTestId("adj-hr-input")).toBeNull();
    fireEvent.click(screen.getByTestId("adj-save"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/payroll/adjustments",
      expect.objectContaining({ userId: "usr_e", adjType: "penalty", amountCent: 3000, reason: "迟到" })));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "payroll_adjustment_requested", reviewId: "review_adj_1" })));
  });

  it("扣罚申诉提交到申诉端点，申诉清单结论全中文", async () => {
    const api = makeApi(async (url: string) => {
      if (url.startsWith("/api/v1/staff/payroll/adjustments/appeals")) {
        return [
          { id: "apl_1", adjustmentId: "adj_1", userId: "usr_e", month: "2099-09", amountCent: 4000, reason: "当日已报备", reviewId: "review_apl_1", status: "revoked", reversalAdjustmentId: "adj_rev_1", createdAt: "2099-09-10T00:00:00Z", decidedAt: "2099-09-12T00:00:00Z" },
          { id: "apl_2", adjustmentId: "adj_2", userId: "usr_e", month: "2099-10", amountCent: 1000, reason: "不服", reviewId: "review_apl_2", status: "upheld", reversalAdjustmentId: null, createdAt: "2099-10-10T00:00:00Z", decidedAt: null },
          { id: "apl_3", adjustmentId: "adj_3", userId: "usr_e", month: "2099-10", amountCent: 500, reason: "待裁", reviewId: "review_apl_3", status: "pending", reversalAdjustmentId: null, createdAt: "2099-10-11T00:00:00Z", decidedAt: null },
        ];
      }
      return [];
    });
    const { setActionResult } = renderPage(api);
    await screen.findByTestId("payroll-appeal-editor");
    fireEvent.change(screen.getByTestId("appeal-adj-input"), { target: { value: "adj_1" } });
    fireEvent.change(screen.getByTestId("appeal-reason-input"), { target: { value: "当日已报备" } });
    expect(screen.queryByTestId("appeal-hr-input")).toBeNull();
    fireEvent.click(screen.getByTestId("appeal-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/payroll/adjustments/adj_1/appeals", { reason: "当日已报备" }));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "payroll_adjustment_appeal_submitted", appealId: "apl_1" })));
    await screen.findByTestId("appeal-table");
    expect(screen.getByTestId("appeal-status-apl_1").textContent).toBe("已撤销（已冲回）");
    expect(screen.getByTestId("appeal-status-apl_2").textContent).toBe("维持");
    expect(screen.getByTestId("appeal-status-apl_3").textContent).toBe("裁定中");

    fireEvent.change(screen.getByTestId("appeal-user-input"), { target: { value: "usr_e" } });
    fireEvent.click(screen.getByTestId("appeal-load"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/staff/payroll/adjustments/appeals?user_id=usr_e"));
  });
});
