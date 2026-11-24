// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffShiftAttendancePage } from "./StaffShiftAttendancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BL 段第 390 条 390-R2（BSM:1474「主管排班：排到几点几分，可指定当天工作点位（比如某个服务区、某片保洁区域）」；
// BSM:1077「主责服务提供人按当日该场次排班指派到该卡座」）：单条与批量排班的点位从可选点位多选，列表与详情显示空间名。走真实 AdminApi（文本体响应）。
const OPTIONS = [{ id: "sp_1", space_name: "卡座-01" }, { id: "sp_2", space_name: "一楼保洁区" }];

function listEnvelope(items: unknown[]): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } });
}
function raw(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

let posts: Array<{ url: string; body: Record<string, unknown> }> = [];
let urls: string[] = [];
let shiftRows: unknown[] = [];

function renderPage() {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    urls.push(url);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(raw({ id: "sh-new", review_id: "review-1", created_count: 1, updated_count: 0, shifts: [] }));
    }
    // 第十八批 CJ 段第 416 条 416-R3：排班可排员工。
    if (url.includes("/staff/shift-staff-options")) return Promise.resolve(listEnvelope([{ user_id: "staff-9", name: "员工九" }, { user_id: "staff-1", name: "员工一" }]));
    if (url.includes("/staff/shift-point-options")) return Promise.resolve(listEnvelope(OPTIONS));
    if (url.includes("/staff/shifts")) return Promise.resolve(listEnvelope(shiftRows));
    return Promise.resolve(listEnvelope([]));
  }));
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "主管", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } } as EffectivePermissionsState,
  };
  render(<StaffShiftAttendancePage {...props} />);
}

async function ready() {
  await waitFor(() => expect(screen.getByTestId("shift-create-open")).toBeTruthy());
  await waitFor(() => expect(urls.some((url) => url.includes("/api/v1/staff/shift-point-options"))).toBe(true));
}

function choose(testId: string, values: string[]) {
  const select = screen.getByTestId(testId) as HTMLSelectElement;
  for (const option of Array.from(select.options)) option.selected = values.includes(option.value);
  fireEvent.change(select);
}

const optionTexts = (testId: string) => Array.from((screen.getByTestId(testId) as HTMLSelectElement).options).map((o) => o.textContent);

describe("StaffShiftAttendancePage 排班点位从可选点位多选（第 390 条）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    posts = [];
    urls = [];
    shiftRows = [];
    window.history.replaceState({}, "", "/admin/staff/shifts");
  });

  it("新增排班：从可选点位选卡座-01，提交带该空间编号，确认摘要显示空间名", async () => {
    renderPage();
    await ready();
    fireEvent.click(screen.getByTestId("shift-create-open"));
    await waitFor(() => expect(optionTexts("shift-points-select")).toEqual(["卡座-01", "一楼保洁区"]));
    await waitFor(() => expect(optionTexts("shift-user-select")).toEqual(["请选择员工", "员工九", "员工一"]));
    fireEvent.change(screen.getByTestId("shift-user-select"), { target: { value: "staff-9" } });
    fireEvent.change(screen.getByTestId("shift-start-input"), { target: { value: "2026-07-05T09:00" } });
    fireEvent.change(screen.getByTestId("shift-end-input"), { target: { value: "2026-07-05T18:00" } });
    choose("shift-points-select", ["sp_1"]);
    fireEvent.click(screen.getByTestId("shift-create-guided-next"));
    await waitFor(() => expect(screen.getByTestId("shift-create-guided-summary").textContent).toContain("卡座-01"));
    fireEvent.click(screen.getByTestId("shift-save"));
    await waitFor(() => expect(posts.some((post) => post.url.endsWith("/api/v1/staff/shifts"))).toBe(true));
    expect(posts.find((post) => post.url.endsWith("/api/v1/staff/shifts"))?.body.point_space_ids).toEqual(["sp_1"]);
  });

  it("批量排班：选两个点位，提交带两个空间编号", async () => {
    renderPage();
    await ready();
    fireEvent.click(screen.getByTestId("shift-batch-open"));
    await waitFor(() => expect(optionTexts("shift-batch-points-select")).toEqual(["卡座-01", "一楼保洁区"]));
    await waitFor(() => expect(optionTexts("shift-batch-users-select")).toEqual(["员工九", "员工一"]));
    choose("shift-batch-users-select", ["staff-1"]);
    fireEvent.change(screen.getByTestId("shift-batch-start-time"), { target: { value: "09:00" } });
    fireEvent.change(screen.getByTestId("shift-batch-end-time"), { target: { value: "18:00" } });
    choose("shift-batch-points-select", ["sp_1", "sp_2"]);
    fireEvent.click(screen.getByTestId("shift-batch-guided-next"));
    await waitFor(() => expect(screen.getByTestId("shift-batch-guided-summary").textContent).toContain("卡座-01、一楼保洁区"));
    fireEvent.click(screen.getByTestId("shift-batch-save"));
    await waitFor(() => expect(posts.some((post) => post.url.includes("/staff/shifts/batch"))).toBe(true));
    expect(posts.find((post) => post.url.includes("/staff/shifts/batch"))?.body.point_space_ids).toEqual(["sp_1", "sp_2"]);
  });

  it("排班列表的点位显示空间名，清单里没有的照显示编号", async () => {
    shiftRows = [{ id: "sh-1", user_id: "staff-1", start_at: "2026-07-05T01:00:00Z", end_at: "2026-07-05T10:00:00Z",
      point_space_ids: ["sp_1", "sp_x"], created_by: "other" }];
    renderPage();
    await waitFor(() => expect(screen.getByTestId("shift-row-sh-1").textContent).toContain("卡座-01、sp_x"));
  });

  it("申请修改排班：预选原点位", async () => {
    const future = new Date(Date.now() + 7 * 24 * 3600 * 1000);
    const start = new Date(future.getTime()); start.setHours(9, 0, 0, 0);
    const end = new Date(future.getTime()); end.setHours(18, 0, 0, 0);
    shiftRows = [{ id: "sh-2", user_id: "staff-1", start_at: start.toISOString(), end_at: end.toISOString(), point_space_ids: ["sp_1"], created_by: "u1" }];
    renderPage();
    await ready();
    fireEvent.click(await screen.findByText("申请修改"));
    await waitFor(() => expect(screen.getByTestId("shift-create-guided")).toBeTruthy());
    const selected = Array.from((screen.getByTestId("shift-points-select") as HTMLSelectElement).selectedOptions).map((o) => o.value);
    expect(selected).toEqual(["sp_1"]);
  });
});
