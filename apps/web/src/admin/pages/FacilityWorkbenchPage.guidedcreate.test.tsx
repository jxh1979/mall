// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const PROJECTS = [{ id: "prj_a", name: "一号综合体" }, { id: "prj_b", name: "二号综合体" }];

// 引导式流程 phase-2：新增设备改为分步弹窗（设备信息→责任与维保→确认）。仅改设备创建主流程，其余表单不动。
// 覆盖入口按钮 → 步进 → 设备信息步校验 → 确认提交，原 facility-equipment-* testid 仍在弹窗内。
function makeApi(postSpy: ReturnType<typeof vi.fn>, projects: unknown[] = PROJECTS): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path === "/api/v1/platform/projects" ? projects : []) as unknown),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: postSpy,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(postSpy: ReturnType<typeof vi.fn>, organizationId?: string, projects?: unknown[]) {
  const props: AdminPageProps = {
    api: makeApi(postSpy, projects),
    user: { id: "u1", username: "admin", accountType: "platform_admin", organizationId },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

describe("FacilityWorkbenchPage 引导式新增设备", () => {
  afterEach(() => vi.clearAllMocks());

  it("入口按钮打开弹窗 → 设备名为空报错 → 补齐步进 → 确认提交设备", async () => {
    const postSpy = vi.fn(async (_path: string) => ({}) as unknown);
    renderPage(postSpy);

    await waitFor(() => expect(screen.getByTestId("facility-equipment-create-open")).toBeTruthy());
    // 未打开时弹窗字段不渲染；页面其余表单（状态变更）仍在。
    expect(screen.queryByTestId("facility-equipment-name")).toBeNull();
    expect(screen.getByTestId("facility-equipment-status-form")).toBeTruthy();

    fireEvent.click(screen.getByTestId("facility-equipment-create-open"));
    // 步1 设备信息：设备名为空 → 下一步校验报错。
    fireEvent.click(screen.getByTestId("facility-equipment-flow-next"));
    expect(screen.getByTestId("facility-equipment-flow-error").textContent).toContain("设备名称");

    // 补齐设备名 → 步进到责任步 → 确认页 → 提交。
    fireEvent.change(screen.getByTestId("facility-equipment-name"), { target: { value: "1号客梯" } });
    fireEvent.click(screen.getByTestId("facility-equipment-flow-next"));
    fireEvent.click(screen.getByTestId("facility-equipment-create"));

    await waitFor(() => {
      const posted = postSpy.mock.calls.map((c) => String(c[0]));
      expect(posted.some((p) => p.endsWith("/facility/equipment"))).toBe(true);
    });
    // 提交成功后弹窗关闭。
    await waitFor(() => expect(screen.queryByTestId("facility-equipment-name")).toBeNull());
  });

  // 第 8 条（PGM:413「所有基础对象都要带“综合体项目归属”」）：新建设备带所属综合体项目（默认本人项目，可改选），没有可选项目不能提交。
  it("新建设备提交所选综合体项目；没有项目时报错不提交", async () => {
    const postSpy = vi.fn(async (_path: string, _body?: unknown) => ({}) as unknown);
    renderPage(postSpy, "prj_b");
    await waitFor(() => expect(screen.queryByTestId("facility-equipment-create-open")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-equipment-create-open"));
    await waitFor(() => expect((screen.queryByTestId("facility-equipment-project") as HTMLSelectElement | null)?.value).toBe("prj_b"));
    fireEvent.change(screen.getByTestId("facility-equipment-name"), { target: { value: "1号客梯" } });
    fireEvent.change(screen.getByTestId("facility-equipment-project"), { target: { value: "prj_a" } });
    fireEvent.click(screen.getByTestId("facility-equipment-flow-next"));
    fireEvent.click(screen.getByTestId("facility-equipment-create"));
    await waitFor(() => expect(postSpy).toHaveBeenCalledWith("/api/v1/facility/equipment", expect.objectContaining({ projectId: "prj_a" })));
  });

  it("没有可选综合体项目时下一步报错", async () => {
    const postSpy = vi.fn(async (_path: string) => ({}) as unknown);
    renderPage(postSpy, undefined, []);
    await waitFor(() => expect(screen.queryByTestId("facility-equipment-create-open")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-equipment-create-open"));
    fireEvent.change(screen.getByTestId("facility-equipment-name"), { target: { value: "1号客梯" } });
    fireEvent.click(screen.getByTestId("facility-equipment-flow-next"));
    expect(screen.queryByTestId("facility-equipment-flow-error")?.textContent ?? "").toContain("综合体项目");
    expect(postSpy).not.toHaveBeenCalled();
  });

  // 复核修（PGM:413、BSM:1417）：只授设备管理、没有 governance.project.view 的员工不请求项目清单（请求了也是 403），
  // 下拉只放本人项目并默认选中，照样建得出带项目的设备。
  it("没有项目查看权限时不请求项目清单，带本人项目建出设备", async () => {
    const postSpy = vi.fn(async (_path: string, _body?: unknown) => ({}) as unknown);
    const api = makeApi(postSpy);
    vi.mocked(api.get).mockImplementation(async (path: string) => {
      if (path === "/api/v1/platform/projects") throw Object.assign(new Error("无权查看项目"), { status: 403 });
      return [] as unknown;
    });
    const props = {
      api,
      user: { id: "u2", username: "guard", accountType: "staff", organizationId: "prj_own" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { grants: ["facility.equipment.manage"], denies: [], wildcard: false } },
      navigateTo: vi.fn(),
    } as unknown as AdminPageProps;
    render(<FacilityWorkbenchPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("facility-equipment-create-open")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-equipment-create-open"));
    await waitFor(() => expect((screen.getByTestId("facility-equipment-project") as HTMLSelectElement).value).toBe("prj_own"));
    fireEvent.change(screen.getByTestId("facility-equipment-name"), { target: { value: "1号客梯" } });
    fireEvent.click(screen.getByTestId("facility-equipment-flow-next"));
    fireEvent.click(screen.getByTestId("facility-equipment-create"));
    await waitFor(() => expect(postSpy).toHaveBeenCalledWith("/api/v1/facility/equipment", expect.objectContaining({ projectId: "prj_own" })));
    expect(vi.mocked(api.get).mock.calls.some((call) => call[0] === "/api/v1/platform/projects")).toBe(false);
  });

  // 第十八批 G 段 82b-R2（治理地图第 659 行「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」）：能看到两个以上项目时，
  // 建工单、巡检模板、巡检任务都可选所属项目，选了才在请求体带 projectId；默认不选（后端按设备、空间或模板所在项目，否则本人项目）。
  it("两个项目时建工单、巡检模板、巡检任务出现所属项目下拉，选了才带 projectId", async () => {
    const postSpy = vi.fn(async (_path: string, _body?: unknown) => ({}) as unknown);
    renderPage(postSpy, "prj_a");
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-project")).not.toBeNull());
    expect(screen.getByTestId("facility-inspection-template-project")).toBeTruthy();
    expect(screen.getByTestId("facility-inspection-task-project")).toBeTruthy();

    fireEvent.change(screen.getByTestId("facility-work-order-title"), { target: { value: "灯坏" } });
    fireEvent.change(screen.getByTestId("facility-work-order-project"), { target: { value: "prj_b" } });
    fireEvent.click(screen.getByTestId("facility-work-order-create"));
    await waitFor(() => expect(postSpy).toHaveBeenCalledWith("/api/v1/facility/work-orders", expect.objectContaining({ projectId: "prj_b" })));

    fireEvent.change(screen.getByTestId("facility-inspection-template-name"), { target: { value: "日巡" } });
    fireEvent.change(screen.getByTestId("facility-inspection-template-project"), { target: { value: "prj_b" } });
    fireEvent.click(screen.getByTestId("facility-inspection-template-create"));
    await waitFor(() => expect(postSpy).toHaveBeenCalledWith("/api/v1/facility/inspections/templates", expect.objectContaining({ projectId: "prj_b" })));

    fireEvent.change(screen.getByTestId("facility-inspection-task-title"), { target: { value: "夜巡" } });
    fireEvent.change(screen.getByTestId("facility-inspection-task-project"), { target: { value: "prj_b" } });
    fireEvent.click(screen.getByTestId("facility-inspection-task-create"));
    await waitFor(() => expect(postSpy).toHaveBeenCalledWith("/api/v1/facility/inspections/tasks", expect.objectContaining({ projectId: "prj_b" })));
  });

  // 复核：角色只勾了 B、只看得到 B 一个项目（不是本人项目 A）时三处也出所属项目下拉，选 B 后请求体带 projectId（修复前不出下拉、落 A 被 403）。
  it("只看得到一个且不是本人项目时也出所属项目下拉，选了带 projectId", async () => {
    const postSpy = vi.fn(async (_path: string, _body?: unknown) => ({}) as unknown);
    renderPage(postSpy, "prj_a", [{ id: "prj_b", name: "二号综合体" }]);
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-project")).not.toBeNull());
    expect(screen.getByTestId("facility-inspection-template-project")).toBeTruthy();
    expect(screen.getByTestId("facility-inspection-task-project")).toBeTruthy();
    fireEvent.change(screen.getByTestId("facility-inspection-template-name"), { target: { value: "日巡" } });
    fireEvent.change(screen.getByTestId("facility-inspection-template-project"), { target: { value: "prj_b" } });
    fireEvent.click(screen.getByTestId("facility-inspection-template-create"));
    await waitFor(() => expect(postSpy).toHaveBeenCalledWith("/api/v1/facility/inspections/templates", expect.objectContaining({ projectId: "prj_b" })));
  });

  it("只看得到本人项目时不显示所属项目下拉，请求体不带 projectId", async () => {
    const postSpy = vi.fn(async (_path: string, _body?: unknown) => ({}) as unknown);
    renderPage(postSpy, "prj_a", [{ id: "prj_a", name: "一号综合体" }]);
    await waitFor(() => expect(screen.getByTestId("facility-work-order-create-form")).toBeTruthy());
    expect(screen.queryByTestId("facility-work-order-project")).toBeNull();
    expect(screen.queryByTestId("facility-inspection-template-project")).toBeNull();
    expect(screen.queryByTestId("facility-inspection-task-project")).toBeNull();

    fireEvent.change(screen.getByTestId("facility-work-order-title"), { target: { value: "灯坏" } });
    fireEvent.click(screen.getByTestId("facility-work-order-create"));
    await waitFor(() => expect(postSpy).toHaveBeenCalledWith("/api/v1/facility/work-orders", expect.anything()));
    const body = postSpy.mock.calls.find((call) => call[0] === "/api/v1/facility/work-orders")?.[1] as Record<string, unknown>;
    expect(body).not.toHaveProperty("projectId");
  });
});
