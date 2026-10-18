// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { PayrollTaxBracketsPage } from "./PayrollTaxBracketsPage";
import type { AdminApiClient } from "../types";

it("完整工资专业个税成组绑定核算原件，缺税额不按零处理", async () => {
  const get = vi.fn(async (path: string) => path.includes("/calculations?") ? [{ id: "calc-1", beforeTaxCent: 480000, userId: "employee", month: "2099-10" }] : []);
  const post = vi.fn().mockResolvedValue({ id: "tax-1", userId: "employee", month: "2099-10", status: "draft", payrollCalculationId: "calc-1", professionalCalculationReference: "专业核定含累计扣除", sourceLines: [], evidenceAttachmentIds: [], incomeCent: 480000, taxCent: 0 });
  const api = { get, post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(), getPaged: vi.fn() } as unknown as AdminApiClient;
  render(<PayrollTaxBracketsPage api={api} user={{ id: "hr", username: "hr", accountType: "staff" }} setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={{ status: "ready", value: { wildcard: true, denies: [], grants: [] } }} />);
  const fill = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });
  await screen.findByText("工资及服务收入的专业税务认定");
  fill("员工账号", "employee"); fill("工资月份", "2099-10");
  fireEvent.click(screen.getByRole("button", { name: "查询认定历史" }));
  await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/staff/payroll/calculations?user_id=employee&month=2099-10"));
  fireEvent.click(screen.getByRole("button", { name: "＋ 新建税务认定" }));
  fill("认定收入来源", "calc-1"); fill("认定税目编码", "wage"); fill("税务适用日期", "2099-10-31"); fill("人员身份依据", "员工"); fill("交易实质依据", "工资"); fill("专业财税结论及日期依据", "当期核定"); fill("认定事由", "工资"); fill("专业核税计算说明", "专业核定含累计扣除");
  fireEvent.click(screen.getByTestId("payroll-tax-assessment-guided-next"));
  expect(screen.getByTestId("payroll-tax-assessment-guided-error")).toHaveTextContent("专业确认个税");
  expect(post).not.toHaveBeenCalled();
  fill("专业确认个税（元）", "4800.01"); fireEvent.click(screen.getByTestId("payroll-tax-assessment-guided-next"));
  expect(screen.getByTestId("payroll-tax-assessment-guided-error")).toHaveTextContent("不能超过本次税前金额");
  fill("专业确认个税（元）", "0"); fireEvent.click(screen.getByTestId("payroll-tax-assessment-guided-next")); fireEvent.click(screen.getByTestId("payroll-tax-assessment-save"));
  await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/staff/payroll/tax-assessments", expect.objectContaining({ payrollCalculationId: "calc-1", professionalTaxCent: 0, professionalCalculationReference: "专业核定含累计扣除" })));
  expect(post.mock.calls[0]![1]).not.toHaveProperty("incomeCent");
});
