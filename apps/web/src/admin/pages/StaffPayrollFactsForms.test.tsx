// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { StaffPayrollCalendarForm } from "./StaffPayrollCalendarForm";
import { StaffPayrollPeriodForm } from "./StaffPayrollPeriodForm";
import type { AdminApiClient } from "../types";
import type { PayrollCalendar, PayrollScheme } from "./staffPayrollTypes";

const fill = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });
function apiFixture() { return { get: vi.fn(), post: vi.fn().mockResolvedValue({ id: "draft-1" }), put: vi.fn(), patch: vi.fn(), delete: vi.fn(), getPaged: vi.fn() } as unknown as AdminApiClient; }
it("日历逐日明确性质，不猜法定假日或漏末日", async () => {
  const api = apiFixture();
  render(<StaffPayrollCalendarForm api={api} projectId="project" open onClose={vi.fn()} onSaved={vi.fn()} />);
  fill("日历开始日期", "2099-10-01"); fill("日历结束日期", "2099-10-02"); fill("日历来源依据", "正式安排及法定日期核实"); fill("日历申请事由", "批准计薪日期");
  fireEvent.click(screen.getByRole("button", { name: "生成逐日日历" }));
  expect((screen.getByLabelText("日期性质2099-10-01") as HTMLSelectElement).value).toBe("");
  fireEvent.click(screen.getByTestId("payroll-calendar-guided-next"));
  expect(screen.getByTestId("payroll-calendar-guided-error")).toHaveTextContent("核实范围内每一天");
  expect(api.post).not.toHaveBeenCalled();
  fill("日期性质2099-10-01", "statutory_holiday"); fill("日期性质2099-10-02", "adjusted_workday");
  fireEvent.click(screen.getByTestId("payroll-calendar-guided-next")); fireEvent.click(screen.getByTestId("payroll-calendar-save"));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/payroll/calendars", expect.objectContaining({ projectId: "project", days: [{ date: "2099-10-01", kind: "statutory_holiday" }, { date: "2099-10-02", kind: "adjusted_workday" }] })));
});
it("数量事实记录已扣事假及真实请假来源，固定月额不接受手填已扣", async () => {
  const api = apiFixture();
  const scheme = { id: "scheme", version: "V0.01", status: "approved", effectiveFrom: "2099-10-01", performanceRules: null, components: [
    { code: "base", label: "基础", type: "base", method: "fixed_monthly" },
    { code: "allowance", label: "数量补贴", type: "allowance", method: "per_unit" },
  ] } as PayrollScheme;
  const calendar = { id: "calendar", status: "approved", version: "V0.01", startDate: "2099-10-01", endDate: "2099-11-05" } as PayrollCalendar;
  render(<StaffPayrollPeriodForm api={api} open userId="employee" month="2099-10" schemes={[scheme]} calendars={[calendar]} onClose={vi.fn()} onSaved={vi.fn()} />);
  fill("已批准薪资方案", "scheme"); fill("已批准计薪日历", "calendar");
  fill("工时周期开始日期", "2099-10-01"); fill("工时周期结束日期", "2099-10-31"); fill("周期标准分钟", "9600"); fill("约定工资支付日", "2099-11-05"); fill("期间事实申请事由", "已核实");
  fill("事实1数量", "9"); fill("事实1分母", "10"); fill("事实1已扣事假", "100"); fill("事实1依据", "已按实到数量折算");
  fireEvent.click(screen.getByTestId("payroll-period-guided-next"));
  expect(screen.getByTestId("payroll-period-guided-error")).toHaveTextContent("关联实际请假单号"); expect(api.post).not.toHaveBeenCalled();
  fill("事实1请假单号", "leave-1,leave-2");
  expect(screen.queryByLabelText("事实2数量")).toBeNull();
  fireEvent.click(screen.getByTestId("payroll-period-guided-next")); fireEvent.click(screen.getByTestId("payroll-period-save"));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/payroll/period-facts", expect.objectContaining({ userId: "employee", month: "2099-10", componentFacts: [{ componentCode: "allowance", quantity: 9, denominator: 10, embeddedLeaveCent: 10000, appliedLeaveIds: ["leave-1", "leave-2"], evidenceBasis: "已按实到数量折算" }], cycleStandardMinutes: 9600, statutoryDueDate: null })));
});
