// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十二批操作员裁决第 9 条「补转派」：已派单或处理中的工单由物业主管改派给他人；处理时限不重算；
// 留痕原处理人、新处理人和原因（原因必填）。工单行「转派」用表单里的处理人与转派原因提交 reassign。
const PROCESSING = { id: "wo-1", code: "FWO-1", workOrderType: "repair", priority: "high", status: "processing", title: "2号冷机漏水", assigneeId: "usr_ops" };
const PENDING = { id: "wo-2", code: "FWO-2", workOrderType: "cleaning", priority: "normal", status: "pending_assign", title: "大堂保洁", assigneeId: null };
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url === "/api/v1/facility/work-orders" ? [PROCESSING, PENDING] : [])),
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

async function openActions(rowId: string) {
  await waitFor(() => expect(screen.queryByTestId(`facility-work-order-actions-${rowId}`)).not.toBeNull());
  fireEvent.click(screen.getByTestId(`facility-work-order-actions-${rowId}`));
}

describe("FacilityWorkbenchPage 工单转派（第十二批裁决第 9 条）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("处理中的工单显示「转派」，须填新处理人与转派原因，提交 reassign", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    const setGlobalError = vi.fn();
    renderPage(makeApi(post), setGlobalError);

    await waitFor(() => expect(screen.queryByTestId("facility-work-order-reassign-reason")).not.toBeNull());
    fireEvent.change(screen.getByTestId("facility-work-order-assignee"), { target: { value: "usr_new" } });
    await openActions("wo-1");
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-reassign-wo-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-reassign-wo-1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("原因")));
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("facility-work-order-reassign-reason"), { target: { value: "原维修人请假" } });
    await openActions("wo-1");
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-reassign-wo-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-reassign-wo-1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/wo-1/reassign",
      { assigneeId: "usr_new", reason: "原维修人请假" }));
  });

  it("待派单的工单不显示「转派」（只能派单）", async () => {
    renderPage(makeApi(vi.fn()));

    await openActions("wo-2");
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-assign-wo-2")).not.toBeNull());
    expect(screen.queryByTestId("facility-work-order-reassign-wo-2")).toBeNull();
  });
});

// 第十八批 AP 段第 229 条 229-R5（PGM:968「……人工只补原因和证据。……」）：完成工单的处理结果不预填「现场处理完成」，为空拦下，不再兜底成「已处理」。
describe("FacilityWorkbenchPage 完成工单处理结果（第 229 条）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("处理结果初始为空；为空点完成不发请求，填写后按所填提交", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    const setGlobalError = vi.fn();
    renderPage(makeApi(post), setGlobalError);
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-resolution")).not.toBeNull());
    expect((screen.getByTestId("facility-work-order-resolution") as HTMLInputElement).value).toBe("");
    await openActions("wo-1");
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-complete-wo-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-complete-wo-1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("处理结果不能为空。"));
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("facility-work-order-resolution"), { target: { value: "更换冷凝水管" } });
    await openActions("wo-1");
    await waitFor(() => expect(screen.queryByTestId("facility-work-order-complete-wo-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-work-order-complete-wo-1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/work-orders/wo-1/complete",
      expect.objectContaining({ resolution: "更换冷凝水管" })));
  });
});

// 第十八批 AP 段第 229 条 229-R8（BSM:940「……一律按执行该动作的设备绑定与该次登录会话定位到操作人并留痕……」）：巡检记录的巡检人取提交账号，
// 楼宇运营工作台代录不再手填巡检人。
describe("FacilityWorkbenchPage 巡检记录巡检人取登录人（第 229 条）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("无巡检人输入框；选任务不填巡检人即可提交，请求不带巡检人", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.queryByTestId("facility-inspection-record-task-id")).not.toBeNull());
    expect(screen.queryByTestId("facility-inspection-record-inspector")).toBeNull();
    fireEvent.change(screen.getByTestId("facility-inspection-record-task-id"), { target: { value: "task-9" } });
    fireEvent.click(screen.getByTestId("facility-inspection-record-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/inspections/tasks/task-9/records", expect.anything()));
    const call = post.mock.calls.find((args) => (args as unknown[])[0] === "/api/v1/facility/inspections/tasks/task-9/records") as unknown[];
    expect(call[1]).not.toHaveProperty("inspectorId");
  });
});
