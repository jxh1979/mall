// 文件版本：V0.07
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StaffProfilesPage } from "./StaffProfilesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const PROFILES = [
  { id: "sp-1", name: "张三", merchantId: null, userId: "usr_1", registrationStatus: "approved", status: "active", salesPosition: false },
  { id: "sp-2", name: "李四", merchantId: "mch-1", userId: "usr_2", registrationStatus: "approved", status: "active", salesPosition: true },
];

const DISPATCHES = [
  { id: "dsp-1", userId: "usr_1", companyId: "ent-platform", merchantId: "mch-1", startDate: "2026-09-01", endDate: null, status: "active", note: "派驻到酒吧" },
  { id: "dsp-2", userId: "usr_2", companyId: "ent-platform", merchantId: "mch-1", startDate: "2026-07-01", endDate: "2026-08-31", status: "ended", note: null },
];

function makeApi() {
  return {
    get: vi.fn(async (url: string) => (url.startsWith("/api/v1/staff/dispatches") ? DISPATCHES : PROFILES)),
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
  it("员工状态变更只填写事由，HR自动解析，只发起复核不直接写入", async () => {
    const api = makeApi();
    (api.post as ReturnType<typeof vi.fn>).mockResolvedValue({ reviewId: "review_hr3", reviewStatus: "pending_review" });
    const setActionResult = vi.fn();
    renderPage(api, setActionResult);
    const select = await screen.findByLabelText("员工状态-sp-1");
    expect(Array.from(select.querySelectorAll("option")).map((option) => option.textContent)).toEqual(["待入职", "在职", "试岗", "停岗", "离职"]);
    fireEvent.change(select, { target: { value: "employed" } });
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "已入职" } });
    expect(screen.queryByTestId("staff-profiles-hr-approver")).toBeNull();
    fireEvent.change(select, { target: { value: "departed" } });
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/profiles/sp-1/employment-status",
      { employmentStatus: "departed", reason: "已入职" }));
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

  // 198-R5：取消存档入口不收请求体，事由框照留（存档、申请删除读取），恢复时不再带事由。
  it("恢复档案不带事由（198-R5）", async () => {
    const api = makeApi();
    vi.mocked(api.get).mockResolvedValue([{ ...PROFILES[0], archivedAt: "2026-09-08T00:00:00Z", deleteStatus: "none" }]);
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("staff-profile-unarchive-sp-1")).toBeTruthy());
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "误存档" } });
    fireEvent.click(screen.getByTestId("staff-profile-unarchive-sp-1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/profiles/sp-1/unarchive", {}));
  });

  // 198-R5：审批删除只读 decision；删除理由在申请删除时已读取落行。
  it("最高权限批准删除只发 decision，不带事由（198-R5）", async () => {
    const api = makeApi();
    vi.mocked(api.get).mockResolvedValue([{ ...PROFILES[0], archivedAt: "2026-09-08T00:00:00Z", deleteStatus: "pending" }]);
    render(<StaffProfilesPage api={api} user={{ id: "usr_master_authority", username: "master-authority", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    await waitFor(() => expect(screen.getByTestId("staff-profile-delete-approve-sp-1")).toBeTruthy());
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "清理离职档案" } });
    fireEvent.click(screen.getByTestId("staff-profile-delete-approve-sp-1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/profiles/sp-1/delete-approve", { decision: "approved" }));
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
    expect((api.get as unknown as { mock: { calls: unknown[][] } }).mock.calls.map((call) => call[0]).filter((url) => String(url).startsWith("/api/v1/staff/profiles"))).toEqual(["/api/v1/staff/profiles"]);
    fireEvent.click(screen.getByTestId("staff-profiles-include-archived"));
    await waitFor(() => expect((api.get as unknown as { mock: { calls: unknown[][] } }).mock.calls.map((call) => call[0])).toContain("/api/v1/staff/profiles?include_archived=true"));
  });
});

describe("StaffProfilesPage 派驻记录（CG-07，BSM「派驻人员的成本与报酬」）", () => {
  it("商户账号即使持有管理权限也不能登记或结束派驻", async () => {
    const api = makeApi();
    render(<StaffProfilesPage api={api} user={{ id: "merchant-admin", username: "店长", accountType: "merchant_admin", merchantId: "mch-1" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
    const end = await screen.findByTestId("staff-dispatch-end-dsp-1");
    expect((screen.getByTestId("staff-dispatch-create") as HTMLButtonElement).disabled).toBe(true);
    expect((end as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("staff-dispatch-create"));
    fireEvent.click(end);
    expect(api.post).not.toHaveBeenCalled();
  });

  it("列出派驻记录：所属主体、派驻酒吧、起止时间分别显示，进行中才有结束入口", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("staff-dispatch-row-dsp-1")).toBeTruthy());
    expect(screen.getByTestId("staff-dispatch-row-dsp-1").textContent).toContain("ent-platform");
    expect(screen.getByTestId("staff-dispatch-row-dsp-1").textContent).toContain("mch-1");
    expect(screen.getByTestId("staff-dispatch-row-dsp-1").textContent).toContain("2026-09-01");
    expect(screen.getByTestId("staff-dispatch-status-dsp-1").textContent).toBe("进行中");
    expect(screen.getByTestId("staff-dispatch-status-dsp-2").textContent).toBe("已结束");
    expect(screen.getByTestId("staff-dispatch-end-dsp-1")).toBeTruthy();
    expect(screen.queryByTestId("staff-dispatch-end-dsp-2")).toBeNull();
  });

  it("登记派驻：填员工账号、派驻酒吧、开始日期后调后端", async () => {
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("staff-dispatch-create")).toBeTruthy());
    fireEvent.click(screen.getByTestId("staff-dispatch-create"));
    expect(screen.getByTestId("staff-profiles-error").textContent).toContain("员工账号");
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("staff-dispatch-user"), { target: { value: "usr_3" } });
    fireEvent.change(screen.getByTestId("staff-dispatch-merchant"), { target: { value: "mch-2" } });
    fireEvent.change(screen.getByTestId("staff-dispatch-start"), { target: { value: "2026-10-01" } });
    fireEvent.change(screen.getByTestId("staff-dispatch-note"), { target: { value: "派驻到酒吧" } });
    fireEvent.click(screen.getByTestId("staff-dispatch-create"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/dispatches",
      { userId: "usr_3", merchantId: "mch-2", startDate: "2026-10-01", note: "派驻到酒吧" }));
  });

  it("结束派驻须填事由，带结束日期与事由调后端", async () => {
    const api = makeApi();
    renderPage(api);
    const end = await screen.findByTestId("staff-dispatch-end-dsp-1");
    fireEvent.click(end);
    expect(screen.getByTestId("staff-profiles-error").textContent).toContain("改动事由");
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "派驻期满" } });
    fireEvent.change(screen.getByTestId("staff-dispatch-end-date"), { target: { value: "2026-09-30" } });
    fireEvent.click(end);
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/dispatches/dsp-1/end", { endDate: "2026-09-30", reason: "派驻期满" }));
  });
});



describe("StaffProfilesPage 生效时间与原业务交接", () => {
  it("指定生效时间随原审批提交，不直接改任职状态", async () => {
    const api = makeApi(); renderPage(api);
    await screen.findByLabelText("员工状态-sp-1");
    fireEvent.change(screen.getByLabelText("员工状态生效时间"), { target: { value: "2026-12-01T09:00" } });
    fireEvent.change(screen.getByTestId("staff-profiles-reason"), { target: { value: "按约定离职" } });
    fireEvent.change(screen.getByLabelText("员工状态-sp-1"), { target: { value: "departed" } });
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/profiles/sp-1/employment-status", {
      employmentStatus: "departed", reason: "按约定离职", effectiveAt: new Date("2026-12-01T09:00").toISOString(),
    }));
  });

  it("显示真实未结事项和挂起原因，填写接手人后提交原业务编号并刷新", async () => {
    const api = makeApi();
    const pending = { businessType: "facility_work_order", businessId: "repair-1", businessName: "维修工单", status: "processing", projectId: "p1", assignable: true };
    let transferred = false;
    vi.mocked(api.get).mockImplementation(async (url: string) => {
      if (url.endsWith("/handover")) return { businessAccessStopped: true, revisions: [], pending: transferred ? [] : [pending,
        { ...pending, businessId: "unknown", assignable: false, holdReason: "原业务项目或商户归属待核实" }], history: [] } as never;
      return (url.includes("dispatches") ? DISPATCHES : PROFILES) as never;
    });
    vi.mocked(api.post).mockImplementation(async () => { transferred = true; return {} as never; });
    renderPage(api);
    fireEvent.click(await screen.findByTestId("staff-profile-handover-sp-1"));
    const assign = await screen.findByTestId("staff-handover-assign-repair-1");
    expect(screen.getByText("业务权限已停止，未结事项继续交接。")).toBeTruthy();
    expect((screen.getByTestId("staff-handover-assign-unknown") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(assign); expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("接手员工账号"), { target: { value: "usr_next" } });
    fireEvent.change(screen.getByLabelText("交接原因"), { target: { value: "主管指定承接" } });
    fireEvent.click(assign);
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/staff/profiles/sp-1/handover", {
      businessType: "facility_work_order", businessId: "repair-1", targetUserId: "usr_next", reason: "主管指定承接",
    }));
    await screen.findByText("暂无待交接事项");
  });
});
