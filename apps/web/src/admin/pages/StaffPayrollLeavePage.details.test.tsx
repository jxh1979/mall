// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import type { AdminApiClient, AdminPageProps } from "../types";
import type { PayrollStatementDetail } from "./staffPayrollTypes";

const statement = { id: "pay-1", userId: "staff-1", month: "2099-10", baseCent: 500000, commissionCent: 0, bonusCent: 0, penaltyCent: 100000, leaveDeductionCent: 0, taxCent: 0, totalCent: 480000, payerCompanyId: "company-1" };
const detail = {
  statement, status: "partial_paid", statementKind: "monthly", schemeVersionId: "scheme-1", factsId: "facts-1",
  paymentDueDate: "2099-11-04", paymentDueBasis: {}, arApItemId: "ap-1", paidAmountCent: 200000,
  lines: [{ id: "line-1", componentCode: "performance", componentType: "performance", description: "绩效工资", grossCent: 100000, embeddedLeaveCent: 20000, additionalLeaveCent: 0, amountCent: 80000, sourceType: "period_facts", sourceId: "facts-1", calculationSnapshot: {} }],
  worktime: { segments: [{ shiftId: "shift-1", date: "2099-10-01", kind: "statutory_holiday", seconds: 7200, payrollMonth: "2099-10" }], ordinarySeconds: 0, restDaySeconds: 0, holidaySeconds: 7200, ordinaryOvertimeCent: 0, restDayOvertimeCent: 0, holidayOvertimeCent: 6000, pendingItems: [] },
  corrections: [{ id: "correction-1", originalStatementId: "old-pay", originalMonth: "2099-09", sourceReviewId: "review-1", sourceAdjustmentId: "adjustment-1", amountCent: 30000, status: "attached", targetStatementId: "pay-1", calculationSnapshot: {} }],
  payments: [{ id: "payment-1", financePaymentId: "bank-success-1", amountCent: 200000, payMethod: "bank_transfer", payRef: "bank-ref", paidAt: "2099-11-04T08:00:00Z" }],
  pendingItems: [{ code: "EVIDENCE_PENDING", sourceId: "source-1", message: "补差依据待核实" }],
};

function mount(fail = false, selectedDetail: PayrollStatementDetail = detail) {
  const api = {
    get: vi.fn(async (url: string) => {
      if (url === "/api/v1/staff/payroll/statements/pay-1") {
        if (fail) throw new Error("无权查看该工资单");
        return selectedDetail;
      }
      if (/\/staff\/payroll\?/.test(url)) return [selectedDetail.statement];
      return [];
    }), post: vi.fn(),
    getPaged: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
  const setGlobalError = vi.fn();
  render(<StaffPayrollLeavePage api={api} user={{ id: "hr", username: "hr", accountType: "staff" }} setActionResult={vi.fn()} setGlobalError={setGlobalError} effectivePermissions={{ status: "ready", value: { wildcard: true, grants: [], denies: [] } }} />);
  return { api, setGlobalError };
}

describe("工资详情来自已定格原件", () => {
  it("读取所选单详情，分开显示事假去重、补差与真实已付，不提供凭空已发按钮", async () => {
    const { api } = mount();
    fireEvent.click(await screen.findByTestId("payroll-row-pay-1"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/staff/payroll/statements/pay-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    expect(within(drawer).getByText("公式内已扣事假")).toBeTruthy();
    expect(within(drawer).getByText("另扣事假")).toBeTruthy();
    expect(within(drawer).getByText("old-pay")).toBeTruthy();
    expect(within(drawer).getByText("bank-success-1")).toBeTruthy();
    expect(within(drawer).getByText(/补差依据待核实/)).toBeTruthy();
    expect(within(drawer).getByText("实际已付")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "标记已发" })).toBeNull();
  });
  it("H01A 将现金处罚与扣分后绩效分别列示，净额使用原单结果且不再扣一次", async () => {
    mount(false, { ...detail, status: "closed", paidAmountCent: 0, worktime: null, corrections: [], payments: [], pendingItems: [],
      lines: [
        { id: "base", componentCode: "base", componentType: "base", description: "基础工资", grossCent: 500000, embeddedLeaveCent: 0, additionalLeaveCent: 0, amountCent: 500000, sourceType: "scheme", sourceId: "scheme-1", calculationSnapshot: {} },
        { id: "performance", componentCode: "performance", componentType: "performance", description: "绩效工资", grossCent: 80000, embeddedLeaveCent: 0, additionalLeaveCent: 0, amountCent: 80000, sourceType: "period_facts", sourceId: "facts-1", calculationSnapshot: {} },
      ],
    });
    fireEvent.click(await screen.findByTestId("payroll-row-pay-1"));
    const drawer = await screen.findByTestId("row-detail-drawer");
    expect(within(drawer).getByText("现金处罚扣款").nextElementSibling?.textContent).toBe("¥1,000.00");
    expect(within(drawer).getByText("应发净额").nextElementSibling?.textContent).toBe("¥4,800.00");
    expect(within(drawer).getByText("代扣个税").nextElementSibling?.textContent).toBe("¥0.00");
    const performance = within(drawer).getByRole("row", { name: /^绩效工资/ });
    expect(within(performance).getAllByRole("cell")[4]?.textContent).toBe("¥800.00");
    expect(within(drawer).getByText(/现金处罚和个税已计入应发净额/)).toBeTruthy();
    expect(drawer.textContent).not.toContain("¥3,800.00");
  });

  it("详情无权时显示原错误，不用列表金额冒充完整详情", async () => {
    const { setGlobalError } = mount(true);
    fireEvent.click(await screen.findByTestId("payroll-row-pay-1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("无权查看该工资单"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
  it("引导按批准原件逐员工定格，不再调用被关闭的旧一键月结", async () => {
    const { api } = mount();
    expect(screen.queryByTestId("payroll-close")).toBeNull();
    expect(screen.getByRole("link", { name: "按批准依据核算工资" }).getAttribute("href")).toBe("#payroll-approved-basis");
    expect(screen.getByText(/完成专业税额审批后定格/)).toBeTruthy();
    expect(api.post).not.toHaveBeenCalled();
  });
});
