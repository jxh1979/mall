// 文件版本：V0.04
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import { AdminApi } from "../client/adminClient";
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
    expect(screen.getByTestId("appeal-status-apl_1").textContent).toBe("已撤销");
    expect(screen.getByTestId("appeal-status-apl_2").textContent).toBe("维持");
    expect(screen.getByTestId("appeal-status-apl_3").textContent).toBe("裁定中");

    fireEvent.change(screen.getByTestId("appeal-user-input"), { target: { value: "usr_e" } });
    fireEvent.click(screen.getByTestId("appeal-load"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/staff/payroll/adjustments/appeals?user_id=usr_e"));
  });
});

// 第十八批 W 段 209-R1（PGM:291「用户看到的每一处文字都是中文，含状态、枚举、日期、按钮、提示、报错、导出与短信。」）：
// 奖惩与申诉请求被后端拒绝时，页面经全局报错显示中文提示，不记成功、不清空已填内容。修复前按钮 void 调用无 catch，什么都不显示。
describe("StaffPayrollLeavePage 奖惩与申诉被拒时显示报错（209-R1）", () => {
  afterEach(() => vi.unstubAllGlobals());

  const PAYROLL = [{ id: "pay_1", user_id: "u-9", payer_company_id: "ent-platform" }];
  function textResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
  }
  function stubRejectedPost(pathPart: string, status: number, error: Record<string, unknown>) {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST" && url.includes(pathPart)) return Promise.resolve(textResponse({ success: false, ...error }, status));
      const items = url.includes("/staff/payroll?") ? PAYROLL : [];
      return Promise.resolve(textResponse({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }));
    }));
  }
  function renderReal() {
    const setActionResult = vi.fn();
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<StaffPayrollLeavePage {...props} />);
    return { setActionResult, setGlobalError };
  }

  it("奖惩接口回 409 时显示后端中文原文，不记已发起复核，已填员工与事由保留", async () => {
    const message = "员工档案未归属公司，无法确定经营主体配置";
    stubRejectedPost("/api/v1/staff/payroll/adjustments", 409, { code: "STATE_CONFLICT", detail_code: "STAFF_COMPANY_REQUIRED", message });
    const { setActionResult, setGlobalError } = renderReal();
    await waitFor(() => expect(screen.getByTestId("payroll-payer-pay_1").textContent).toBe("ent-platform"));

    fireEvent.change(screen.getByTestId("adj-user-input"), { target: { value: "usr_e" } });
    fireEvent.change(screen.getByTestId("adj-amount-input"), { target: { value: "30" } });
    fireEvent.change(screen.getByTestId("adj-reason-input"), { target: { value: "迟到" } });
    fireEvent.click(screen.getByTestId("adj-save"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("操作冲突：" + message));
    expect(setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "payroll_adjustment_requested" }));
    expect((screen.getByTestId("adj-user-input") as HTMLInputElement).value).toBe("usr_e");
    expect((screen.getByTestId("adj-reason-input") as HTMLInputElement).value).toBe("迟到");
  });

  it("申诉接口回 403（非本人申诉）时显示中文报错，不记申诉已提交", async () => {
    stubRejectedPost("/api/v1/staff/payroll/adjustments/adj_1/appeals", 403, { code: "FORBIDDEN", detail_code: "APPEAL_SELF_ONLY", message: "只能由被扣罚员工本人提出申诉" });
    const { setActionResult, setGlobalError } = renderReal();
    await waitFor(() => expect(screen.getByTestId("payroll-payer-pay_1").textContent).toBe("ent-platform"));

    fireEvent.change(screen.getByTestId("appeal-adj-input"), { target: { value: "adj_1" } });
    fireEvent.change(screen.getByTestId("appeal-reason-input"), { target: { value: "当日已报备" } });
    fireEvent.click(screen.getByTestId("appeal-submit"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("当前账号无权进行此操作"));
    expect(setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "payroll_adjustment_appeal_submitted" }));
    expect((screen.getByTestId("appeal-reason-input") as HTMLInputElement).value).toBe("当日已报备");
  });
});
