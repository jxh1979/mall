// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 CG 段第 410 条 410-R2（PGM:846「发现异常支持「轻记录/重转整改工单」分级与「全部转整改工单」两种模式，整改后复检、主管确认闭环」）：
// 分级模式（模板 abnormal_mode=light，不挂模板的任务同此）下提交异常须由巡检人选轻重；全部转整改工单模式不出分级；异常模式显示名按 HTML 原词。
const TEMPLATES = [
  { id: "tpl-graded", name: "消防通道巡检模板", inspectionType: "fire", cycleDays: 7, abnormalMode: "light", active: true },
  { id: "tpl-all", name: "消防泵房巡检模板", inspectionType: "fire", cycleDays: 7, abnormalMode: "to_work_order", active: true },
];
const TASKS = [
  { id: "task-graded", templateId: "tpl-graded", inspectionType: "fire", status: "in_progress", title: "消防通道巡检", scheduleMode: "template" },
  { id: "task-all", templateId: "tpl-all", inspectionType: "fire", status: "in_progress", title: "消防泵房巡检", scheduleMode: "template" },
];
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url === "/api/v1/facility/inspections/templates") return TEMPLATES;
      if (url === "/api/v1/facility/inspections/tasks") return TASKS;
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

function fillAbnormal(taskId: string) {
  fireEvent.change(screen.getByTestId("facility-inspection-record-task-id"), { target: { value: taskId } });
  fireEvent.change(screen.getByTestId("facility-inspection-record-result"), { target: { value: "abnormal" } });
  fireEvent.change(screen.getByTestId("facility-inspection-record-abnormal-note"), { target: { value: "消防通道被杂物堵住" } });
}

describe("FacilityWorkbenchPage 巡检异常分级（第 410 条）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("分级模式任务提交异常须选轻重：不选拦下不发请求，选重后请求体带 abnormalLevel=heavy", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    const setGlobalError = vi.fn();
    renderPage(makeApi(post), setGlobalError);
    await waitFor(() => expect(screen.queryByTestId("facility-inspection-record-task-id")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("facility-inspection-select-task-task-graded")).not.toBeNull());

    fillAbnormal("task-graded");
    await waitFor(() => expect(screen.queryByTestId("facility-inspection-record-abnormal-level")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-inspection-record-submit"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("请选择异常分级：轻记录或重转整改工单"));
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("facility-inspection-record-abnormal-level"), { target: { value: "heavy" } });
    fireEvent.click(screen.getByTestId("facility-inspection-record-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/inspections/tasks/task-graded/records",
      expect.objectContaining({ result: "abnormal", abnormalNote: "消防通道被杂物堵住", abnormalLevel: "heavy" })));
  });

  it("全部转整改工单模式任务不出分级、请求体不带 abnormalLevel；模板异常模式两项按 HTML 原词", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("facility-inspection-record-task-id")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("facility-inspection-select-task-task-all")).not.toBeNull());
    const modes = Array.from((screen.getByTestId("facility-inspection-template-abnormal-mode") as HTMLSelectElement).options).map((option) => option.textContent);
    expect(modes).toEqual(["轻记录/重转整改工单分级", "全部转整改工单"]);

    fillAbnormal("task-all");
    expect(screen.queryByTestId("facility-inspection-record-abnormal-level")).toBeNull();
    fireEvent.click(screen.getByTestId("facility-inspection-record-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/inspections/tasks/task-all/records", expect.objectContaining({ result: "abnormal" })));
    expect(((post.mock.calls[0] as unknown[])[1] as Record<string, unknown>).abnormalLevel).toBeUndefined();
  });
});
