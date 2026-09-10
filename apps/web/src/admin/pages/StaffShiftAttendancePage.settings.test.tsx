// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { StaffShiftAttendancePage } from "./StaffShiftAttendancePage";
import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

afterEach(() => vi.unstubAllGlobals());

function fixture() {
  const posts: Array<{ url: string; body: Record<string, unknown> }> = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    let data: unknown = { items: [], page: 1, page_size: 50, total: 0 };
    if (url.includes("/settings/shift-visible-days")) data = { value: 7, min_value: null, max_value: null };
    if (url.includes("/settings/payroll-penalty-cap")) data = { value: 2000, min_value: 0, max_value: 10000 };
    if (init?.method === "POST") {
      posts.push({ url, body: JSON.parse(String(init.body)) });
      data = { review_id: "review-cap", review_status: "pending_review" };
    }
    return new Response(JSON.stringify({ success: true, code: "OK", data }), { headers: { "Content-Type": "application/json" } });
  }));
  const props: AdminPageProps = { api: new AdminApi("test"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } } };
  return { props, posts };
}

it("可见天数首次设置必须填写上下限，提交后显示保存结果", async () => {
  const { props, posts } = fixture();
  render(<StaffShiftAttendancePage {...props} />);
  fireEvent.click(screen.getByText("设置本经营主体的排班可见天数"));
  const save = await screen.findByText("保存可见天数");
  fireEvent.click(save);
  expect(posts).toHaveLength(0);
  fireEvent.change(screen.getByLabelText("可见天数"), { target: { value: "3" } });
  fireEvent.change(screen.getByLabelText("天数下限"), { target: { value: "1" } });
  fireEvent.change(screen.getByLabelText("天数上限"), { target: { value: "14" } });
  fireEvent.change(screen.getByLabelText("配置事由"), { target: { value: "调整展示" } });
  fireEvent.click(save);
  await screen.findByText("本经营主体的排班可见天数已保存。");
  expect(posts[0]?.body).toMatchObject({ value: 3, min_value: 1, max_value: 14, reason: "调整展示" });
});

it("扣除上限以百分比填写并提交三条审批线，页面不把提交当成生效", async () => {
  const { props, posts } = fixture();
  render(<StaffPayrollLeavePage {...props} />);
  fireEvent.click(screen.getByText("普通扣除上限"));
  const submit = await screen.findByText("提交上限调整审批");
  fireEvent.click(submit);
  expect(posts).toHaveLength(0);
  fireEvent.change(screen.getByLabelText("上限比例（%）"), { target: { value: "10" } });
  for (const [label, value] of [["人力最终审批人", "hr"], ["财务最终审批人", "finance"], ["法务最终审批人", "legal"], ["上限调整事由", "调整"]] as const) {
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
  }
  fireEvent.click(submit);
  await waitFor(() => expect(posts).toHaveLength(1));
  expect(posts[0]?.body).toMatchObject({ value: 1000, min_value: 0, max_value: 10000,
    parallel_lines: [{ line_code: "hr_review", final_approver_id: "hr" }, { line_code: "finance_review", final_approver_id: "finance" }, { line_code: "legal_review", final_approver_id: "legal" }] });
  await screen.findByText("申请已提交，审批完成前现行上限不变。审批单：review-cap");
});
