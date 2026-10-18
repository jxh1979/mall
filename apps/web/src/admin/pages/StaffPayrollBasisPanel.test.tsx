// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { StaffPayrollBasisPanel } from "./StaffPayrollBasisPanel";
import type { AdminApiClient } from "../types";

const scheme = { id: "scheme-1", userId: "employee", projectId: "project", version: "V0.01", status: "draft", reviewId: null, evidenceAttachmentIds: [], effectiveFrom: "2099-10-01", components: [], worktimeTerms: {}, paymentBasis: "合同工资日" };
const facts = { id: "facts-1", userId: "employee", month: "2099-10", status: "approved", reviewId: "facts-review", evidenceAttachmentIds: [], schemeVersionId: "scheme-1", componentFacts: [], cycleStart: "2099-10-01", cycleEnd: "2099-10-31", agreedDueDate: "2099-11-05" };
const calculation = { id: "calc-1", userId: "employee", month: "2099-10", factsId: "facts-1", statementKind: "monthly", originEventId: null, beforeTaxCent: 480000, schemeVersionId: "scheme-1", sourceFingerprint: "fp", createdAt: "2099-11-01T00:00:00Z" };
const rule = { id: "rule-1", status: "draft", reviewId: null, evidenceAttachmentIds: [], ownerCompanyId: "owner", deductions: { LATE_A_1: 10 }, effectiveFrom: "2099-10-01" };
function setup() {
  const get = vi.fn(async (path: string) => {
    if (path.includes("timelines")) return [];
    if (path.includes("performance-rules?")) return [rule];
    if (path.includes("schemes?")) return [scheme];
    if (path.includes("period-facts?")) return [facts];
    if (path.includes("calculations?")) return [calculation];
    if (path.includes("tax-assessments?")) return [
      { id: "tax-old-service", status: "approved", payrollCalculationId: null, taxCent: 0 },
      { id: "tax-other", status: "approved", payrollCalculationId: "other-calculation", taxCent: 0 },
      { id: "tax-pending", status: "pending_review", payrollCalculationId: "calc-1", taxCent: 0 },
      { id: "tax-1", status: "approved", payrollCalculationId: "calc-1", taxCent: 10000 },
    ];
    return [];
  });
  const result = { statement: { id: "pay-1" }, pendingItems: [] };
  const post = vi.fn(async (path: string, _body: Record<string, unknown>) => path.endsWith("/calculations") ? calculation : path.endsWith("/settlements") ? result : { ...(path.includes("/performance-rules/") ? rule : scheme), status: "pending_review", reviewId: "review-1" });
  const postForm = vi.fn().mockResolvedValue({ id: "evidence-1" });
  const api = { get, post, postForm, getPaged: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
  const onSettled = vi.fn();
  render(<StaffPayrollBasisPanel api={api} user={{ id: "employee", username: "employee", accountType: "staff" }} month="2099-10" onSettled={onSettled} effectivePermissions={{ status: "ready", value: { wildcard: true, grants: [], denies: [] } }} setActionResult={vi.fn()} setGlobalError={vi.fn()} />);
  return { get, post, postForm, onSettled, result };
}
const fill = (label: string, value: string) => fireEvent.change(screen.getByLabelText(label), { target: { value } });
async function load() { fill("查阅员工账号", "employee"); fill("计薪项目编号", "project"); fireEvent.click(screen.getByRole("button", { name: "读取员工计薪依据" })); await screen.findByText("合同工资日", { exact: false }); }
async function upload() {
  await waitFor(() => expect(screen.getByLabelText("计薪依据原件")).not.toBeDisabled());
  fireEvent.change(screen.getByLabelText("计薪依据原件"), { target: { files: [new File(["actual image"], "basis.png", { type: "image/png" })] } });
  await screen.findByText(/已关联 1 份原件/);
}
it("方案提交上传绑定真实草案，只传财务线，人力由系统确定", async () => {
  const { post, postForm } = setup(); await load();
  fireEvent.click(screen.getByRole("button", { name: "上传依据并提交审批" })); await upload();
  const data = postForm.mock.calls[0]![1] as FormData;
  expect(data.get("business_id")).toBe("scheme-1"); expect(data.get("business_type")).toBe("staff_payroll_scheme"); expect(data.get("attachment_role")).toBe("professional_basis");
  expect(data.get("file")).toBeInstanceOf(File);
  fill("财务最终审批人", "finance");
  expect(screen.queryByLabelText("人力最终审批人")).toBeNull();
  fireEvent.click(screen.getByTestId("payroll-basis-submit-guided-next")); fireEvent.click(screen.getByTestId("payroll-basis-submit"));
  await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/staff/payroll/schemes/scheme-1/submit", expect.objectContaining({ evidenceAttachmentIds: ["evidence-1"], parallelLines: [{ lineCode: "finance_review", finalApproverId: "finance" }] })));
});
it("分值表审批沿真实经营主体GM，不允许临时填写审批人", async () => {
  const { post, postForm } = setup();
  fill("计薪项目编号", "project"); fill("分值表经营主体", "owner"); fill("分值表商户", "bar");
  fireEvent.click(screen.getByRole("button", { name: "读取经营主体分值表" }));
  fireEvent.click(await screen.findByRole("button", { name: "上传依据并提交审批" })); await upload();
  expect((postForm.mock.calls[0]![1] as FormData).get("business_type")).toBe("staff_performance_rule");
  expect(screen.queryByLabelText("财务最终审批人")).toBeNull();
  fireEvent.click(screen.getByTestId("payroll-basis-submit-guided-next")); fireEvent.click(screen.getByTestId("payroll-basis-submit"));
  await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/staff/payroll/performance-rules/rule-1/submit", expect.objectContaining({ parallelLines: [], evidenceAttachmentIds: ["evidence-1"] })));
});
it("完整工资只选本次批准税认定，定格请求不自报工资或已付金额", async () => {
  const { post, onSettled, result } = setup(); await load();
  fireEvent.click(screen.getByRole("button", { name: "按本单核算工资" }));
  fireEvent.click(screen.getByRole("button", { name: "生成本次税前核算原件" }));
  await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/staff/payroll/calculations", expect.objectContaining({ userId: "employee", month: "2099-10", factsId: "facts-1", statementKind: "monthly", originEventId: null })));
  fireEvent.click(screen.getByRole("button", { name: "读取核算原件及税务认定" }));
  const select = screen.getByLabelText("已批准匹配税务认定");
  await waitFor(() => expect(within(select).getByRole("option", { name: /tax-1/ })).toBeTruthy());
  expect(within(select).queryByRole("option", { name: /tax-old|tax-other|tax-pending/ })).toBeNull();
  fill("已批准匹配税务认定", "tax-1");
  fireEvent.click(screen.getByTestId("payroll-settle-guided-next")); fireEvent.click(screen.getByTestId("payroll-settle-submit"));
  await waitFor(() => expect(onSettled).toHaveBeenCalledWith(result));
  const body = post.mock.calls.find(([path]) => path.endsWith("/settlements"))?.[1];
  expect(body).toMatchObject({ calculationId: "calc-1", taxAssessmentId: "tax-1", userId: "employee", month: "2099-10" });
  expect(body).not.toHaveProperty("amountCent"); expect(body).not.toHaveProperty("paid");
});
