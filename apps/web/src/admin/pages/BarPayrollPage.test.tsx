// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { BarPayrollPage } from "./BarPayrollPage";
import type { AdminPageProps, AdminApiClient } from "../types";
function props(api: Partial<AdminApiClient>, permissions = ["bar.payroll.view", "bar.payroll.manage"]): AdminPageProps {
  return { api: api as AdminApiClient, user: { id: "user", username: "user", accountType: "staff", merchantId: "merchant" },
    effectivePermissions: { status: "ready", value: { grants: permissions, denies: [], wildcard: false } },
    setActionResult: vi.fn(), setGlobalError: vi.fn() };
}
function scope() {
  fireEvent.change(screen.getByLabelText("综合体项目编号"), { target: { value: "project" } });
  fireEvent.change(screen.getByLabelText("商户编号"), { target: { value: "merchant" } });
  fireEvent.change(screen.getByLabelText("营业月份"), { target: { value: "2026-09" } });
}
describe("酒吧服务收入页面", () => {
  it("无敏感单独授权不加载任何金额数据", () => {
    const get = vi.fn(); render(<BarPayrollPage {...props({ get }, [])} />);
    expect(screen.getByText("服务收入分配涉及员工金额，请先取得对应的单独授权。")).toBeTruthy();
    expect(get).not.toHaveBeenCalled();
  });
  it("缺来源显示待核实，不能把零元显示为结清", async () => {
    const get = vi.fn().mockResolvedValueOnce([]).mockResolvedValueOnce([{ month: "2026-09", status: "pending", allocations: [], unresolvedSources: ["原单A:缺少实际服务区间"] }]);
    render(<BarPayrollPage {...props({ get })} />); scope();
    fireEvent.click(screen.getByRole("button", { name: "查询规则与月结" }));
    await waitFor(() => expect(screen.getByText("原单A:缺少实际服务区间")).toBeTruthy());
    expect(screen.getAllByText("待核实").length).toBeGreaterThan(0);
    expect(screen.getByText("未形成原件")).toBeTruthy();
  });
  it("月结响应丢失时重试沿用原请求编号和范围", async () => {
    const post = vi.fn().mockRejectedValueOnce(new Error("响应未收到")).mockResolvedValueOnce([]);
    render(<BarPayrollPage {...props({ post })} />); scope();
    fireEvent.click(screen.getByRole("button", { name: "核对并生成月结原件" }));
    await waitFor(() => expect(screen.getByRole("alert").textContent).toContain("响应未收到"));
    fireEvent.click(screen.getByRole("button", { name: "核对并生成月结原件" }));
    await waitFor(() => expect(post).toHaveBeenCalledTimes(2));
    expect(post.mock.calls[1]).toEqual(post.mock.calls[0]);
  });
});
