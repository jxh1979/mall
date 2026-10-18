// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PayrollTaxBracketsPage } from "./PayrollTaxBracketsPage";
import type { AdminApiClient, AdminPageProps } from "../types";

const version = { id: "v1", category: "professional_category", version: "V0.01", effectiveFrom: "2099-01-01", status: "approved", reviewId: "review1", evidenceAttachmentIds: ["file1"], applicabilityBasis: "专业身份认定", calculationBasis: "已审批计算依据", reason: "测试依据", roundingRule: "truncate_cent_each_step", brackets: [{ seq: 1, incomeFloorCent: 0, incomeCeilingCent: null, incomeBaseBps: 10000, flatDeductionCent: 0, rateBps: 1000, quickDeductionCent: 0 }] };
function setup(rows = [version], writable = true) {
  const get = vi.fn().mockImplementation(async (path: string) => path.includes("tax-versions") ? rows : []);
  const post = vi.fn().mockResolvedValue({ ...version, id: "a1", status: "draft", userId: "employee", month: "2099-07", taxDate: "2099-07-31", identityBasis: "身份", transactionBasis: "交易", professionalBasis: "专业", payerCompanyId: "company", incomeCent: 100000, taxCent: 10000, taxVersionId: "v1", sourceLines: [] });
  const api: AdminApiClient = { get, post, getPaged: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() };
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "staff" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: writable } } };
  render(<PayrollTaxBracketsPage {...props} />); return { get, post };
}
function fill(label: string, value: string) { fireEvent.change(screen.getByLabelText(label), { target: { value } }); }

describe("完整税表和真实认定后台", () => {
  it("读取完整版本、专业依据和真实审批链，不将保存当作生效", async () => {
    const { get } = setup();
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/staff/payroll/tax-versions"));
    expect(await screen.findByText("professional_category · V0.01")).toBeTruthy();
    expect(screen.getByText("2099-01-01 起适用")).toBeTruthy();
    expect(screen.getByRole("link", { name: "查看审批链" }).getAttribute("href")).toContain("review_id=review1");
    expect(screen.getByTestId("payroll-tax-brackets-table").textContent).toContain("专业身份认定");
    expect(screen.queryByTestId("payroll-tax-brackets-guided")).toBeNull();
  });
  it("缺维护授权时不能新建税表或认定", async () => {
    setup([], false); await screen.findByText("既有服务收入的专业税务认定");
    const button = screen.queryByTestId("payroll-tax-brackets-create-open");
    expect(button === null || (button as HTMLButtonElement).disabled).toBe(true);
  });
  it("查询认定按员工和月份，并以专业事实创建，不接收手填税额", async () => {
    const { get, post } = setup(); await screen.findByText("既有服务收入的专业税务认定");
    fill("员工账号", "employee"); fill("工资月份", "2099-07");
    fireEvent.click(screen.getByRole("button", { name: "查询认定历史" }));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/staff/payroll/tax-assessments?user_id=employee&month=2099-07"));
    fireEvent.click(screen.getByRole("button", { name: "＋ 新建税务认定" }));
    fill("认定税目编码", "professional_category"); fill("税务适用日期", "2099-07-31"); fill("人员身份依据", "身份原件"); fill("交易实质依据", "合同交易"); fill("专业财税结论及日期依据", "财税认定文件"); fill("认定事由", "本月认定");
    fireEvent.click(screen.getByTestId("payroll-tax-assessment-guided-next")); fireEvent.click(screen.getByTestId("payroll-tax-assessment-save"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post.mock.calls[0]![0]).toBe("/api/v1/staff/payroll/tax-assessments");
    expect(post.mock.calls[0]![1]).toMatchObject({ userId: "employee", month: "2099-07", category: "professional_category", taxDate: "2099-07-31", professionalBasis: "财税认定文件" });
    expect(post.mock.calls[0]![1]).not.toHaveProperty("taxCent"); expect(post.mock.calls[0]![1]).not.toHaveProperty("incomeCent");
    await waitFor(() => expect(screen.queryByTestId("payroll-tax-assessment-guided")).toBeNull());
  });
});
