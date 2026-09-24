import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { SpaceTypesPage } from "./SpaceTypesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-7：新增空间类型改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截）。
// M018-006/009：存档/申请删除/审批/恢复四条生命周期动作各有可失败断言。
const ROWS = [{ id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" }];
const LIFECYCLE_ROWS = [
  { id: "st-1", typeCode: "kiosk", typeName: "一体机位", sortOrder: 100, status: "active" },
  { id: "st-2", typeCode: "old", typeName: "旧种类", sortOrder: 90, status: "disabled" },
  { id: "st-3", typeCode: "arc", typeName: "已存档种类", sortOrder: 80, status: "archived" },
  { id: "st-4", typeCode: "pd", typeName: "待删种类", sortOrder: 70, status: "pending_delete" },
];

function makeApi(post = vi.fn().mockResolvedValue({}), rows: unknown[] = ROWS): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue(rows),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const ADMIN_USER = { id: "u1", username: "admin", accountType: "platform_admin" };
const MASTER_USER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };

function renderPage(api: AdminApiClient, user = ADMIN_USER) {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api,
    user,
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<SpaceTypesPage {...props} />);
  return { setActionResult };
}

describe("SpaceTypesPage 引导式新增空间类型(phase-7)", () => {
  it("默认只留入口按钮，不展示弹窗字段", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("space-types-create-open")).toBeTruthy());
    expect(screen.queryByTestId("space-types-guided")).toBeNull();
  });

  it("入口→步进→提交：带正确 body 且成功后关闭弹窗", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = makeApi(post);
    const { setActionResult } = renderPage(api);
    await waitFor(() => expect(screen.getByTestId("space-types-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("space-types-create-open"));

    fireEvent.change(screen.getByLabelText("种类编码"), { target: { value: "floor" } });
    fireEvent.change(screen.getByLabelText("显示名"), { target: { value: "楼层" } });
    fireEvent.change(screen.getByLabelText("排序"), { target: { value: "200" } });
    fireEvent.click(screen.getByTestId("space-types-guided-next"));
    await waitFor(() => expect(screen.getByTestId("space-types-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("space-types-save"));

    await waitFor(() => expect(post).toHaveBeenCalledTimes(1));
    expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types", expect.objectContaining({
      typeCode: "floor", typeName: "楼层", sortOrder: 200, status: "active",
    }));
    await waitFor(() => expect(screen.queryByTestId("space-types-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "space_type_saved" }));
  });

  it("新建没有停用选项，确认页状态固定为启用", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.getByTestId("space-types-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("space-types-create-open"));
    expect(screen.queryByLabelText("状态")).toBeNull();
    expect(screen.queryByText("停用")).toBeNull();
    fireEvent.change(screen.getByLabelText("种类编码"), { target: { value: "booth" } });
    fireEvent.change(screen.getByLabelText("显示名"), { target: { value: "摊位" } });
    fireEvent.click(screen.getByTestId("space-types-guided-next"));
    await waitFor(() => expect(screen.getByTestId("space-types-guided-summary")).toBeTruthy());
    expect(screen.getByTestId("space-types-guided-summary").textContent).toContain("启用");
    expect(screen.getByTestId("space-types-guided-summary").textContent).not.toContain("停用");
  });

  it("步内校验拦截：编码/显示名缺失则不进下一步、不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("space-types-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("space-types-create-open"));
    fireEvent.change(screen.getByLabelText("种类编码"), { target: { value: "floor" } });
    // 显示名留空
    fireEvent.click(screen.getByTestId("space-types-guided-next"));
    expect(screen.getByTestId("space-types-guided-error").textContent).toContain("请填写种类编码与显示名");
    expect(screen.queryByTestId("space-types-guided-summary")).toBeNull();
    expect(post).not.toHaveBeenCalled();
  });

  it("已删除种类不再提供申请删除但可恢复；已存档可取消存档", async () => {
    renderPage(makeApi(undefined, LIFECYCLE_ROWS));
    await waitFor(() => expect(screen.getByTestId("space-type-archive-kiosk")).toBeTruthy());
    expect(screen.getByTestId("space-types-table").textContent).toContain("已删除");
    expect(screen.queryByTestId("space-type-delete-request-old")).toBeNull();
    expect(screen.getByTestId("space-type-restore-old")).toBeTruthy(); // M018-009
    expect(screen.getByTestId("space-type-unarchive-arc")).toBeTruthy();
    expect(screen.getByTestId("space-type-delete-request-arc")).toBeTruthy();
  });
});

// M018-006：存档→申请删除→最高权限审批→恢复（业务地图 L566-577）各有可失败断言；理由经 window.prompt、高危经 window.confirm。
describe("SpaceTypesPage 生命周期动作", () => {
  it("存档：填理由并确认后 POST /archive 带 reason；理由留空不请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    const { setActionResult } = renderPage(makeApi(post, LIFECYCLE_ROWS));
    await waitFor(() => expect(screen.getByTestId("space-type-archive-kiosk")).toBeTruthy());
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const promptSpy = vi.spyOn(window, "prompt").mockReturnValueOnce("   ");
    fireEvent.click(screen.getByTestId("space-type-archive-kiosk"));
    expect(post).not.toHaveBeenCalled();

    promptSpy.mockReturnValueOnce("配错了");
    fireEvent.click(screen.getByTestId("space-type-archive-kiosk"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-1/archive", { reason: "配错了" }));
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "space_type_archived", typeCode: "kiosk" }));
  });

  it("申请删除：已存档种类填理由后 POST /delete-request 带 reason", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post, LIFECYCLE_ROWS));
    await waitFor(() => expect(screen.getByTestId("space-type-delete-request-arc")).toBeTruthy());
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.spyOn(window, "prompt").mockReturnValueOnce("不再使用");
    fireEvent.click(screen.getByTestId("space-type-delete-request-arc"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-3/delete-request", { reason: "不再使用" }));
  });

  it("审批：最高权限可通过/驳回（POST /delete-approve 带 decision），平台管理员只见「待最高权限审批」", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post, LIFECYCLE_ROWS));
    await waitFor(() => expect(screen.getByTestId("space-types-table")).toBeTruthy());
    expect(screen.queryByTestId("space-type-delete-approve-pd")).toBeNull();
    expect(screen.getByTestId("space-types-table").textContent).toContain("待最高权限审批");
    vi.unstubAllGlobals();
  });

  it("审批（最高权限）：通过 POST decision=approved", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post, LIFECYCLE_ROWS), MASTER_USER);
    await waitFor(() => expect(screen.getByTestId("space-type-delete-approve-pd")).toBeTruthy());
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("space-type-delete-approve-pd"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-4/delete-approve", { decision: "approved" }));
  });

  it("恢复：已删除种类确认后 POST /unarchive（M018-009）", async () => {
    const post = vi.fn().mockResolvedValue({});
    const { setActionResult } = renderPage(makeApi(post, LIFECYCLE_ROWS));
    await waitFor(() => expect(screen.getByTestId("space-type-restore-old")).toBeTruthy());
    vi.spyOn(window, "confirm").mockReturnValue(true);
    fireEvent.click(screen.getByTestId("space-type-restore-old"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/platform/space-types/st-2/unarchive"));
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "space_type_unarchived", typeCode: "old" }));
  });
});
