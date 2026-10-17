// 文件版本：V0.04
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StaffProfilesPage } from "./StaffProfilesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const PROFILES = [
  { id: "sp-1", name: "张三", merchantId: null, userId: "usr_1", registrationStatus: "approved", status: "active", salesPosition: false },
  { id: "sp-2", name: "李四", merchantId: "mch-1", userId: "usr_2", registrationStatus: "approved", status: "active", salesPosition: true },
];

function makeApi() {
  return {
    get: vi.fn(async () => PROFILES),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setActionResult: AdminPageProps["setActionResult"] = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StaffProfilesPage {...props} />);
}

describe("StaffProfilesPage 员工档案销售岗维护（ADM-STF-004）", () => {
  it("员工状态只提供五种业务状态，变更须填写事由与 人力会签人，只发起复核不直接写入（CG-12）", async () => {
    const api = makeApi();
    (api.post as ReturnType<typeof vi.fn>).mockResolvedValue({ reviewId: "review_hr3", reviewStatus: "pending_review" });
    const setActionResult = vi.fn();
    renderPage(api, setActionResult);
    const select = await screen.findByLabelText("员工状态-sp-1");
    expect(Array.from(select.querySelectorAll("option")).map((option) => option.textContent)).toEqual(["待入职", "在职", "试岗", "停岗", "离职"]);
    fireEvent.change(select, { target: { value: "employed" } });
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "已入职" } });
    fireEvent.change(select, { target: { value: "employed" } });
    expect(api.post).not.toHaveBeenCalled();
    expect(screen.getByTestId("staff-profiles-error").textContent).toContain("人力会签人");
    fireEvent.change(screen.getByTestId("staff-profiles-hr-approver"), { target: { value: "usr_hr" } });
    fireEvent.change(select, { target: { value: "departed" } });
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/profiles/sp-1/employment-status",
      { employmentStatus: "departed", reason: "已入职", hrApproverId: "usr_hr" }));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 结果: "已发起员工状态变更复核", 复核单: "review_hr3", 目标状态: "离职" })));
  });

  it("日常存档提交事由，恢复入口只显示在已存档档案上", async () => {
    const api = makeApi();
    renderPage(api);
    const archive = await screen.findByTestId("staff-profile-archive-sp-1");
    fireEvent.click(archive);
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "工作结束" } });
    fireEvent.click(archive);
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/profiles/sp-1/archive", { reason: "工作结束" }));
  });

  it("显示已存档后可以申请删除，不把删除申请当成审批完成", async () => {
    const api = makeApi();
    vi.mocked(api.get).mockResolvedValue([{ ...PROFILES[0], archivedAt: "2026-09-08T00:00:00Z", deleteStatus: "none" }]);
    renderPage(api);
    fireEvent.click(await screen.findByLabelText("显示已存档"));
    fireEvent.change(await screen.findByTestId("staff-profiles-reason"), { target: { value: "申请清理" } });
    fireEvent.click(screen.getByTestId("staff-profile-delete-request-sp-1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/profiles/sp-1/delete-request", { reason: "申请清理" }));
    expect(screen.queryByTestId("staff-profile-delete-approve-sp-1")).toBeNull();
  });

  it("列出员工并显示销售岗是/否", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("staff-profiles-table")).toBeTruthy());
    expect(screen.getByTestId("staff-profile-sales-position-sp-1").textContent).toBe("否");
    expect(screen.getByTestId("staff-profile-sales-position-sp-2").textContent).toBe("是");
  });

  it("不填事由不许改：销售岗决定提成按哪档费率发，必须留痕", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("staff-profile-toggle-sales-sp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("staff-profile-toggle-sales-sp-1"));
    expect(screen.getByTestId("staff-profiles-error").textContent).toContain("改动事由");
    expect(api.post).not.toHaveBeenCalled();
  });

  it("填事由后设为销售岗 → 带 salesPosition/reason 调后端", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("staff-profiles-reason")).toBeTruthy());
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "转岗销售" } });
    fireEvent.click(screen.getByTestId("staff-profile-toggle-sales-sp-1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/staff/profiles/sp-1/sales-position",
      { salesPosition: true, reason: "转岗销售" },
    ));
  });

  it("已是销售岗的员工按钮为「取消销售岗」，点击下发 false", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("staff-profile-toggle-sales-sp-2")).toBeTruthy());
    expect(screen.getByTestId("staff-profile-toggle-sales-sp-2").textContent).toContain("取消销售岗");
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "转岗后勤" } });
    fireEvent.click(screen.getByTestId("staff-profile-toggle-sales-sp-2"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/staff/profiles/sp-2/sales-position",
      { salesPosition: false, reason: "转岗后勤" },
    ));
  });
});

describe("StaffProfilesPage 已存档档案按需拉取（U03-008）", () => {
  it("默认不请求已存档档案，勾选后才带 include_archived=true 重新拉取", async () => {
    const api = makeApi();
    renderPage(api);
    await screen.findByLabelText("员工状态-sp-1");
    expect((api.get as unknown as { mock: { calls: unknown[][] } }).mock.calls.map((call) => call[0])).toEqual(["/api/v1/staff/profiles"]);
    fireEvent.click(screen.getByTestId("staff-profiles-include-archived"));
    await waitFor(() => expect((api.get as unknown as { mock: { calls: unknown[][] } }).mock.calls.map((call) => call[0])).toContain("/api/v1/staff/profiles?include_archived=true"));
  });
});
