// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PayrollTaxBracketsPage } from "./PayrollTaxBracketsPage";
import type { AdminApiClient, AdminPageProps } from "../types";
const draft = { id: "v1", category: "professional_category", version: "V0.01", effectiveFrom: "2099-01-01", status: "draft", reviewId: null, evidenceAttachmentIds: [], applicabilityBasis: "身份", calculationBasis: "计算依据", reason: "测试依据", roundingRule: "truncate_cent_each_step", brackets: [{ seq: 1, incomeFloorCent: 0, incomeCeilingCent: null, incomeBaseBps: 10000, flatDeductionCent: 0, rateBps: 1000, quickDeductionCent: 0 }] };
function setup(options: { failure?: boolean; files?: string[] } = {}) {
  const get = vi.fn().mockImplementation(async (path: string) => path.includes("tax-versions") ? [draft] : (options.files ?? []).map(referenceId => ({ itemType: "attachment", result: "uploaded", referenceId })));
  const post = options.failure ? vi.fn().mockRejectedValue(new Error("真实部门起点尚未配置")) : vi.fn().mockResolvedValue(draft);
  const postForm = vi.fn().mockResolvedValue({ id: "actual_file" });
  const api: AdminApiClient = { get, post, postForm, getPaged: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() };
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "staff" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } } };
  render(<PayrollTaxBracketsPage {...props} />); return { post, postForm };
}
function fill(label: string, value: string) { fireEvent.change(screen.getByLabelText(label), { target: { value } }); }
async function openVersion() { fireEvent.click(await screen.findByTestId("payroll-tax-brackets-create-open")); }
function fillVersion() {
  fill("税目编码", "professional_category"); fill("生效日期", "2099-01-01"); fill("适用身份及交易依据", "专业身份文件"); fill("计算及舍入专业依据", "专业计算文件"); fill("版本事由", "新完整版本"); fill("舍入口径", "truncate_cent_each_step");
  fill("第1档收入下界（分）", "0"); fill("第1档基数万分比", "10000"); fill("第1档定额减除（分）", "0"); fill("第1档税率万分比", "1000"); fill("第1档速算扣除（分）", "0");
}
function fillReviewers() { fill("财务最终审批人", "finance"); fill("税务最终审批人", "tax"); fill("法务最终审批人", "legal"); }

describe("完整税档草案与审批操作", () => {
  it("完整草案保存不直接写旧单档或发布", async () => {
    const { post } = setup(); await openVersion(); fillVersion();
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-guided-next"));
    expect(screen.getByTestId("payroll-tax-brackets-guided-summary").textContent).toContain("保存不生效");
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-save")); await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post).toHaveBeenCalledWith("/api/v1/staff/payroll/tax-versions", expect.objectContaining({ category: "professional_category", effectiveFrom: "2099-01-01", brackets: draft.brackets }));
    await waitFor(() => expect(screen.queryByTestId("payroll-tax-brackets-guided")).toBeNull());
  });
  it("缺显式零值和区间缺口不能进入确认", async () => {
    const { post } = setup(); await openVersion(); fillVersion(); fill("第1档定额减除（分）", "");
    fireEvent.click(screen.getByTestId("payroll-tax-brackets-guided-next")); expect(screen.getByTestId("payroll-tax-brackets-guided-error").textContent).toContain("0也须填写");
    fill("第1档定额减除（分）", "0"); fill("第1档收入下界（分）", "1"); fireEvent.click(screen.getByTestId("payroll-tax-brackets-guided-next"));
    expect(screen.getByTestId("payroll-tax-brackets-guided-error").textContent).toContain("从0分连续覆盖"); expect(post).not.toHaveBeenCalled();
  });
  it("实际上传绑定原草案，三线及文件随提交送出", async () => {
    const { post, postForm } = setup(); fireEvent.click(await screen.findByRole("button", { name: "准备审批" }));
    await screen.findByText("已关联 0 份专业凭证");
    expect(screen.getByLabelText("专业依据原件").getAttribute("accept")).toBe("image/*,video/*");
    fireEvent.change(screen.getByLabelText("专业依据原件"), { target: { files: [new File(["professional evidence"], "basis.png", { type: "image/png" })] } });
    await waitFor(() => expect(postForm).toHaveBeenCalledTimes(1)); await screen.findByText("已关联 1 份专业凭证");
    const data = postForm.mock.calls[0]![1] as FormData; expect(data.get("business_type")).toBe("payroll_tax_version"); expect(data.get("business_id")).toBe("v1"); expect(data.get("file")).toBeInstanceOf(File);
    fillReviewers(); fireEvent.click(screen.getByTestId("payroll-tax-submit-guided-next")); fireEvent.click(screen.getByTestId("payroll-tax-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/staff/payroll/tax-versions/v1/submit", expect.objectContaining({ evidenceAttachmentIds: ["actual_file"], parallelLines: [{ lineCode: "finance_review", finalApproverId: "finance" }, { lineCode: "tax_review", finalApproverId: "tax" }, { lineCode: "legal_review", finalApproverId: "legal" }] })));
  });
  it("刷新后恢复已上传凭证，审批失败保留原件和弹窗", async () => {
    const { post } = setup({ failure: true, files: ["persisted_file"] }); fireEvent.click(await screen.findByRole("button", { name: "准备审批" }));
    await screen.findByText("已关联 1 份专业凭证"); fillReviewers(); fireEvent.click(screen.getByTestId("payroll-tax-submit-guided-next")); fireEvent.click(screen.getByTestId("payroll-tax-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(1)); await screen.findByText("真实部门起点尚未配置");
    expect(screen.getByTestId("payroll-tax-submit-guided")).toBeTruthy(); expect(post.mock.calls[0]![1].evidenceAttachmentIds).toEqual(["persisted_file"]);
  });
});
