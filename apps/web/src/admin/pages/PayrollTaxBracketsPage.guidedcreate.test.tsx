import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { PayrollTaxBracketsPage } from "./PayrollTaxBracketsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-7：新增税率档改分步引导弹窗（入口→两步步进→提交带正确 body + 各步校验拦截）。
const BRACKETS = [
  { id: "ptb_labor_1", category: "labor_remuneration", seq: 1, incomeFloorCent: 1, incomeCeilingCent: 400000, incomeBaseBps: 10000, flatDeductionCent: 80000, rateBps: 2000, quickDeductionCent: 0 },
];

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(BRACKETS),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient) {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<PayrollTaxBracketsPage {...props} />);
  return { setActionResult };
}

describe("PayrollTaxBracketsPage 引导式新增税率档(phase-7)", () => {
  it("默认只留入口按钮，不展示弹窗字段", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("payroll-tax-brackets-create-open")).toBeTruthy());
    expect(screen.queryByTestId("payroll-tax-brackets-guided")).toBeNull();
  });

  it("入口→两步→提交：带正确 body 且成功后关闭弹窗", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = makeApi(post);
    const { setActionResult } = renderPage(api);
    await waitFor(() => expect(screen.getByTestId("payroll-tax-brackets-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-create-open"));

    // 第一步：档位与收入区间
    fireEvent.change(screen.getByLabelText("档位 id"), { target: { value: "ptb_labor_2" } });
    fireEvent.change(screen.getByLabelText("序号"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("毛收入下界(分)"), { target: { value: "400001" } });
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-guided-next"));

    // 第二步：税率参数
    await waitFor(() => expect(screen.getByLabelText("应税基数万分比")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("应税基数万分比"), { target: { value: "8000" } });
    fireEvent.change(screen.getByLabelText("税率万分比"), { target: { value: "3000" } });
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-guided-next"));

    await waitFor(() => expect(screen.getByTestId("payroll-tax-brackets-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-save"));

    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post).toHaveBeenCalledWith("/api/v1/staff/payroll/tax-brackets", expect.objectContaining({
      id: "ptb_labor_2", category: "labor_remuneration", seq: 2, incomeFloorCent: 400001, incomeBaseBps: 8000, rateBps: 3000,
    }));
    await waitFor(() => expect(screen.queryByTestId("payroll-tax-brackets-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "payroll_tax_bracket_saved" }));
  });

  it("第一步校验拦截：缺 id 不进第二步", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("payroll-tax-brackets-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-create-open"));
    // id 留空、只填序号/下界
    fireEvent.change(screen.getByLabelText("序号"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("毛收入下界(分)"), { target: { value: "400001" } });
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-guided-next"));
    expect(screen.getByTestId("payroll-tax-brackets-guided-error").textContent).toContain("档位 id");
    expect(screen.queryByLabelText("应税基数万分比")).toBeNull();
    expect(post).not.toHaveBeenCalled();
  });

  it("第二步校验拦截：税率超界不到确认页、不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("payroll-tax-brackets-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-create-open"));
    fireEvent.change(screen.getByLabelText("档位 id"), { target: { value: "ptb_labor_2" } });
    fireEvent.change(screen.getByLabelText("序号"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("毛收入下界(分)"), { target: { value: "400001" } });
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-guided-next"));
    await waitFor(() => expect(screen.getByLabelText("应税基数万分比")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("应税基数万分比"), { target: { value: "8000" } });
    fireEvent.change(screen.getByLabelText("税率万分比"), { target: { value: "99999" } });
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-guided-next"));
    expect(screen.getByTestId("payroll-tax-brackets-guided-error").textContent).toContain("税率万分比");
    expect(screen.queryByTestId("payroll-tax-brackets-guided-summary")).toBeNull();
    expect(post).not.toHaveBeenCalled();
  });
});
