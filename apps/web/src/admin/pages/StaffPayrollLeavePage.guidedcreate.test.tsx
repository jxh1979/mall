// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StaffPayrollLeavePage } from "./StaffPayrollLeavePage";
import type { AdminApiClient, AdminPageProps } from "../types";

function mount(fail = false) {
  const post = vi.fn(async (_url: string, body: Record<string, unknown>) => {
    if (fail) throw new Error("无权为该员工创建方案");
    return { ...body, id: "scheme-1", status: "draft", userId: body.userId, projectId: "project-1", version: 1, evidenceAttachmentIds: [] };
  });
  const api = { get: vi.fn().mockResolvedValue([]), getPaged: vi.fn(), post, put: vi.fn(), patch: vi.fn(), delete: vi.fn() } as unknown as AdminApiClient;
  const props: AdminPageProps = { api, user: { id: "hr", username: "hr", accountType: "staff" }, effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } }, setActionResult: vi.fn(), setGlobalError: vi.fn() };
  render(<StaffPayrollLeavePage {...props} />);
  fireEvent.click(screen.getByTestId("payroll-base-create-open"));
  return post;
}
function fill() {
  const values: Record<string, string> = {
    "方案员工账号": "staff-9", "适用岗位编号": "waiter", "方案生效日期": "2099-10-01", "工资支付约定": "次月5日", "方案申请事由": "劳动合同计薪依据",
    "项目1编号": "base", "项目1名称": "基础工资", "项目1金额": "5000", "项目1依据": "已签合同",
    "项目1项目类型": "base", "项目1计算方式": "fixed_monthly", "项目1计量单位": "month", "项目1事假口径": "proportional",
    "工时制度": "standard", "每日标准分钟": "480", "每周标准分钟": "2400", "小时计薪基数（元）": "28.74",
    "普通日加班比例（万分比）": "15000", "休息日加班比例（万分比）": "20000", "法定节假日加班比例（万分比）": "30000",
    "工时制度批准依据": "岗位适用核验", "工时计薪依据": "已核定计薪基数与算法",
  };
  Object.entries(values).forEach(([label, value]) => fireEvent.change(screen.getByLabelText(label), { target: { value } }));
}
describe("薪资方案替代未经审批的工资基数直写", () => {
  it("真实填写逐项方案保存为草案，不调用旧基数端点或直接生效", async () => {
    const post = mount(); fill();
    fireEvent.click(screen.getByTestId("payroll-base-guided-next"));
    fireEvent.click(screen.getByTestId("base-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/staff/payroll/schemes", expect.objectContaining({
      userId: "staff-9", effectiveFrom: "2099-10-01", effectiveTo: null, performanceRules: null,
      components: [{ code: "base", label: "基础工资", amountCent: 500000, type: "base", method: "fixed_monthly", unit: "month", leaveMode: "proportional", calculationBasis: "已签合同" }],
      worktimeTerms: expect.objectContaining({ system: "standard", hourlyBaseCent: 2874, ordinaryOvertimeBps: 15000 }),
    })));
    expect(post.mock.calls.some(([url]) => url === "/api/v1/staff/payroll/base")).toBe(false);
    await waitFor(() => expect(screen.queryByTestId("payroll-base-guided")).toBeNull());
    expect(screen.getByText("草案")).toBeTruthy();
  });
  it("必填依据缺失时保留表单，不默认金额和算法", async () => {
    const post = mount();
    fireEvent.click(screen.getByTestId("payroll-base-guided-next"));
    expect(await screen.findByTestId("payroll-base-guided-error")).toHaveTextContent("请填写员工账号");
    expect(post).not.toHaveBeenCalled();
  });
  it("服务端拒绝保留完整输入和错误，不显示已保存", async () => {
    const post = mount(true); fill();
    fireEvent.click(screen.getByTestId("payroll-base-guided-next")); fireEvent.click(screen.getByTestId("base-save"));
    expect(await screen.findByText("无权为该员工创建方案")).toBeTruthy();
    expect(post).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("payroll-base-guided")).toBeTruthy();
  });
});
