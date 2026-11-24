// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StaffShiftAttendancePage } from "./StaffShiftAttendancePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 CJ 段第 416 条 416-R3（BSM:1474「主管排班：排到几点几分……主管在手机上也能排。」「由领班直接安排其他工作人员顶班并在系统中记录顶班人员」）：
// 单条、按周批量与顶班的员工从可排员工清单选，排班表、确认摘要与顶班处显示姓名（清单里没有的显示编号）。走真实 AdminApi（文本体响应）。
const STAFF = [{ user_id: "usr_a", name: "张三" }, { user_id: "usr_b", name: "李四" }];

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
    if (url.includes("/staff/shift-staff-options")) return Promise.resolve(listEnvelope(STAFF));
    if (url.includes("/staff/shift-point-options")) return Promise.resolve(listEnvelope([]));
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
  await waitFor(() => expect(urls.some((url) => url.includes("/api/v1/staff/shift-staff-options"))).toBe(true));
  await waitFor(() => expect(screen.getByTestId("shift-create-open")).toBeTruthy());
}

function choose(testId: string, values: string[]) {
  const select = screen.getByTestId(testId) as HTMLSelectElement;
  for (const option of Array.from(select.options)) option.selected = values.includes(option.value);
  fireEvent.change(select);
}

const optionTexts = (select: HTMLElement) => Array.from((select as HTMLSelectElement).options).map((o) => o.textContent);

describe("StaffShiftAttendancePage 排班员工从可排员工清单选（第 416 条）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    posts = [];
    urls = [];
    shiftRows = [];
    window.history.replaceState({}, "", "/admin/staff/shifts");
  });

  it("排班表员工列显示姓名，清单里没有的照显示编号", async () => {
    shiftRows = [
      { id: "sh-a", user_id: "usr_a", start_at: "2026-07-05T01:00:00Z", end_at: "2026-07-05T10:00:00Z", created_by: "other" },
      { id: "sh-x", user_id: "usr_x", start_at: "2026-07-05T01:00:00Z", end_at: "2026-07-05T10:00:00Z", created_by: "other" },
    ];
    renderPage();
    await ready();
    await waitFor(() => expect(screen.getByTestId("shift-row-sh-a").textContent).toContain("张三"));
    expect(screen.getByTestId("shift-row-sh-a").textContent).not.toContain("usr_a");
    expect(screen.getByTestId("shift-row-sh-x").textContent).toContain("usr_x");
  });

  it("新增排班：员工从可排员工清单选，确认摘要显示姓名，提交带其编号", async () => {
    renderPage();
    await ready();
    fireEvent.click(screen.getByTestId("shift-create-open"));
    await waitFor(() => expect(optionTexts(screen.getByTestId("shift-user-select"))).toEqual(["请选择员工", "张三", "李四"]));
    fireEvent.change(screen.getByTestId("shift-user-select"), { target: { value: "usr_a" } });
    fireEvent.change(screen.getByTestId("shift-start-input"), { target: { value: "2026-07-05T09:00" } });
    fireEvent.change(screen.getByTestId("shift-end-input"), { target: { value: "2026-07-05T18:00" } });
    fireEvent.click(screen.getByTestId("shift-create-guided-next"));
    await waitFor(() => expect(screen.getByTestId("shift-create-guided-summary").textContent).toContain("张三"));
    expect(screen.getByTestId("shift-create-guided-summary").textContent).not.toContain("usr_a");
    fireEvent.click(screen.getByTestId("shift-save"));
    await waitFor(() => expect(posts.some((post) => post.url.endsWith("/api/v1/staff/shifts"))).toBe(true));
    expect(posts.find((post) => post.url.endsWith("/api/v1/staff/shifts"))?.body.user_id).toBe("usr_a");
  });

  it("批量排班：选张三、李四两名，确认摘要显示姓名与人数，提交带两人编号", async () => {
    renderPage();
    await ready();
    fireEvent.click(screen.getByTestId("shift-batch-open"));
    await waitFor(() => expect(optionTexts(screen.getByTestId("shift-batch-users-select"))).toEqual(["张三", "李四"]));
    choose("shift-batch-users-select", ["usr_a", "usr_b"]);
    fireEvent.change(screen.getByTestId("shift-batch-start-time"), { target: { value: "09:00" } });
    fireEvent.change(screen.getByTestId("shift-batch-end-time"), { target: { value: "18:00" } });
    fireEvent.click(screen.getByTestId("shift-batch-guided-next"));
    await waitFor(() => expect(screen.getByTestId("shift-batch-guided-summary").textContent).toContain("张三、李四(共 2 名)"));
    fireEvent.click(screen.getByTestId("shift-batch-save"));
    await waitFor(() => expect(posts.some((post) => post.url.includes("/staff/shifts/batch"))).toBe(true));
    expect(posts.find((post) => post.url.includes("/staff/shifts/batch"))?.body.user_ids).toEqual(["usr_a", "usr_b"]);
  });

  it("安排顶班：原员工显示姓名，顶班员工下拉不含原员工，选李四提交带其编号", async () => {
    shiftRows = [{ id: "sh-a", user_id: "usr_a", start_at: new Date("2099-08-03T20:00").toISOString(),
      end_at: new Date("2099-08-04T04:00").toISOString(), created_by: "other" }];
    renderPage();
    await ready();
    await waitFor(() => expect(screen.getByTestId("shift-row-sh-a").textContent).toContain("张三"));
    fireEvent.click(screen.getByText("安排顶班"));
    await waitFor(() => expect(screen.getByText("安排临时顶班 · 原员工 张三")).toBeTruthy());
    expect(optionTexts(screen.getByLabelText("顶班员工"))).toEqual(["请选择顶班员工", "李四"]);
    fireEvent.change(screen.getByLabelText("顶班员工"), { target: { value: "usr_b" } });
    for (const [label, value] of [["顶班开始", "2099-08-03T22:00"], ["顶班结束", "2099-08-04T04:00"], ["顶班事由", "临时缺岗"],
      ["实际交接事实", "领班确认交接"]] as const) fireEvent.change(screen.getByLabelText(label), { target: { value } });
    fireEvent.click(screen.getByText("确认安排顶班"));
    await waitFor(() => expect(posts.some((post) => post.url.endsWith("/staff/shifts/sh-a/cover"))).toBe(true));
    expect(posts.find((post) => post.url.endsWith("/staff/shifts/sh-a/cover"))?.body.cover_user_id).toBe("usr_b");
  });
});
