import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffShiftAttendancePage } from "./StaffShiftAttendancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-10：新增排班改分步引导弹窗（入口→步进→提交带正确 body + 步内校验拦截不提交）。
// 只覆盖「新增排班」主创建流；考勤打卡记录/日期切换等不在本期改造范围。
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
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST" && url.includes("/staff/shifts")) {
      lastPostUrl = url;
      lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
      return Promise.resolve(raw({ id: "sh-new" }));
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

describe("StaffShiftAttendancePage 引导式新增排班(phase-10)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostBody = null;
    lastPostUrl = "";
    window.history.replaceState({}, "", "/admin/staff/shifts");
  });

  it("页面默认只留入口按钮，不展示排班字段", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-create-open")).toBeTruthy());
    expect(screen.queryByTestId("shift-user-input")).toBeNull();
    expect(screen.queryByTestId("shift-create-guided")).toBeNull();
  });

  it("入口→步进→提交：POST 新建排班带员工/起止时间/点位，成功后关闭弹窗", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("shift-create-open"));

    fireEvent.change(screen.getByTestId("shift-user-input"), { target: { value: "staff-9" } });
    fireEvent.change(screen.getByTestId("shift-start-input"), { target: { value: "2026-07-05T09:00" } });
    fireEvent.change(screen.getByTestId("shift-end-input"), { target: { value: "2026-07-05T18:00" } });
    fireEvent.click(screen.getByTestId("shift-create-guided-next")); // 步1→步2
    fireEvent.change(screen.getByTestId("shift-points-input"), { target: { value: "sp-a, sp-b" } });
    fireEvent.click(screen.getByTestId("shift-create-guided-next")); // 步2→确认
    await waitFor(() => expect(screen.getByTestId("shift-create-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("shift-save"));

    await waitFor(() => expect(lastPostBody).toBeTruthy());
    expect(lastPostUrl).toContain("/staff/shifts");
    // AdminApi 会把请求体 key 转 snake_case（user_id/point_space_ids/start_at/end_at）。
    expect((lastPostBody as { user_id: string }).user_id).toBe("staff-9");
    expect((lastPostBody as { point_space_ids: string[] }).point_space_ids).toEqual(["sp-a", "sp-b"]);
    expect(typeof (lastPostBody as { start_at: string }).start_at).toBe("string");
    expect(typeof (lastPostBody as { end_at: string }).end_at).toBe("string");
    await waitFor(() => expect(screen.queryByTestId("shift-create-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "staff_shift_saved" }));
  });

  it("步内校验拦截：员工留空则不进下一步、不提交", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("shift-create-open"));
    // 只填起止时间、员工留空
    fireEvent.change(screen.getByTestId("shift-start-input"), { target: { value: "2026-07-05T09:00" } });
    fireEvent.change(screen.getByTestId("shift-end-input"), { target: { value: "2026-07-05T18:00" } });
    fireEvent.click(screen.getByTestId("shift-create-guided-next"));
    expect(screen.getByTestId("shift-create-guided-error").textContent).toContain("员工");
    expect(screen.queryByTestId("shift-create-guided-summary")).toBeNull();
    expect(lastPostBody).toBeNull();
  });
});
