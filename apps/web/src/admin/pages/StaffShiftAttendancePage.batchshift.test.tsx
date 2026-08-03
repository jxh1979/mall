import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffShiftAttendancePage } from "./StaffShiftAttendancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 按周批量排班（2026-08-02 用户拍板）：排班员一次排一周，跨零点班只填时段、界面明说「结束为次日」。
// 只覆盖新增的批量入口；单班表单仍由 guidedcreate 用例把守（两者并存，不能互相顶掉）。
function listEnvelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function raw(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostBody: unknown = null;
let lastPostUrl = "";
function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("/staff/shifts/batch")) {
      lastPostUrl = url;
      lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
      return Promise.resolve(raw({ created_count: 5, updated_count: 2, shifts: [] }));
    }
    if (url.includes("/staff/shifts")) return Promise.resolve(listEnvelope([]));
    if (url.includes("/staff/attendance")) return Promise.resolve(listEnvelope([]));
    return Promise.resolve(listEnvelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StaffShiftAttendancePage {...props} />);
  return { setActionResult };
}

/** 打开批量弹窗并填到「员工/日期/时段」都齐（时段由调用方给，跨零点用例要自己挑）。 */
async function openBatchAndFill(startTime: string, endTime: string) {
  await waitFor(() => expect(screen.getByTestId("shift-batch-open")).toBeTruthy());
  fireEvent.click(screen.getByTestId("shift-batch-open"));
  fireEvent.change(screen.getByTestId("shift-batch-users-input"), { target: { value: "staff-1, staff-2" } });
  fireEvent.change(screen.getByTestId("shift-batch-days-input"), { target: { value: "7" } });
  fireEvent.change(screen.getByTestId("shift-batch-start-time"), { target: { value: startTime } });
  fireEvent.change(screen.getByTestId("shift-batch-end-time"), { target: { value: endTime } });
}

describe("StaffShiftAttendancePage 按周批量排班", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
    window.history.replaceState({}, "", "/admin/staff/shifts");
  });

  it("单班入口与批量入口并存，默认都不展开表单", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-create-open")).toBeTruthy());
    expect(screen.getByTestId("shift-batch-open")).toBeTruthy();
    expect(screen.queryByTestId("shift-batch-guided")).toBeNull();
    expect(screen.queryByTestId("shift-batch-users-input")).toBeNull();
  });

  it("勾周一/周三/周五 + 每日时段 → POST 批量接口带员工列表/星期几/时刻，成功后显示新建与覆盖条数", async () => {
    const { setActionResult } = renderPage();
    await openBatchAndFill("09:00", "18:00");
    fireEvent.click(screen.getByTestId("shift-batch-weekday-1"));
    fireEvent.click(screen.getByTestId("shift-batch-weekday-3"));
    fireEvent.click(screen.getByTestId("shift-batch-weekday-5"));
    fireEvent.change(screen.getByTestId("shift-batch-points-input"), { target: { value: "sp-a, sp-b" } });
    fireEvent.click(screen.getByTestId("shift-batch-guided-next"));
    await waitFor(() => expect(screen.getByTestId("shift-batch-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("shift-batch-save"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/staff/shifts/batch");
    // AdminApi 把请求体 key 转 snake_case（user_ids/start_date/start_time/end_time/weekdays）。
    const body = lastPostBody as { user_ids: string[]; weekdays: number[]; start_time: string; end_time: string; days: number; point_space_ids: string[] };
    expect(body.user_ids).toEqual(["staff-1", "staff-2"]);
    expect(body.weekdays).toEqual([1, 3, 5]);
    expect(body.start_time).toBe("09:00");
    expect(body.end_time).toBe("18:00");
    expect(body.days).toBe(7);
    expect(body.point_space_ids).toEqual(["sp-a", "sp-b"]);

    await waitFor(() => expect(screen.queryByTestId("shift-batch-guided")).toBeNull());
    expect(screen.getByTestId("shift-batch-result").textContent).toContain("新建 5 个班次");
    expect(screen.getByTestId("shift-batch-result").textContent).toContain("覆盖修改 2 个班次");
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "staff_shift_batch_saved" }));
  });

  it("跨零点班(22:00-06:00)界面明说「结束时间为次日」，白班不提示", async () => {
    renderPage();
    await openBatchAndFill("22:00", "06:00");
    expect(screen.getByTestId("shift-batch-overnight-hint").textContent).toContain("次日 06:00");

    fireEvent.change(screen.getByTestId("shift-batch-end-time"), { target: { value: "23:30" } });
    expect(screen.queryByTestId("shift-batch-overnight-hint")).toBeNull();
  });

  it("步内校验拦截：员工留空、天数越界都不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-batch-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("shift-batch-open"));
    fireEvent.click(screen.getByTestId("shift-batch-guided-next"));
    expect(screen.getByTestId("shift-batch-guided-error").textContent).toContain("员工");
    expect(screen.queryByTestId("shift-batch-guided-summary")).toBeNull();

    fireEvent.change(screen.getByTestId("shift-batch-users-input"), { target: { value: "staff-1" } });
    fireEvent.change(screen.getByTestId("shift-batch-days-input"), { target: { value: "60" } });
    fireEvent.click(screen.getByTestId("shift-batch-guided-next"));
    expect(screen.getByTestId("shift-batch-guided-error").textContent).toContain("1 到 31");
    expect(screen.queryByTestId("shift-batch-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
