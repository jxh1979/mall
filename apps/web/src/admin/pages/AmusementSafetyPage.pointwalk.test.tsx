// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSafetyPage } from "./AmusementSafetyPage";
import { AdminApi } from "../client/adminClient";
import type { EffectivePermissionsState } from "../types";

// 第十八批 BS 段第 373 条 373-R3、373-R5（BSM:1300「巡检必须按点位顺序完成，缺一个点位、上一个点位就不能通过；未走完全部点位的巡检不成立、
// 不得标记为已完成」；第十八批裁决 76「巡检员用工作手机按顺序扫码后记结果」）：新建巡检选了配有点位的游乐项目，弹窗只读显示走点进度
// （网页上不能记点位），不再手填检查项；没走完点「创建巡检」提示且不发请求，走完后创建不带检查项（服务端取走完的点位）。
// 没配点位的项目表单照旧。经真实后台客户端提交，假响应为文本体。
const ROWS = [
  { id: "i-1", inspection_no: "INS-1", project_id: "amuse-1", project_name: "旋转木马", overall_result: "fail", inspection_status: "submitted",
    rectification_status: "pending", reopen_status: "pending", inspected_at: "2026-11-16T02:00:00Z", evidence_attachment_ids: [] },
];
const PROJECTS = [{ id: "amuse-1", project_name: "旋转木马" }, { id: "amuse-2", project_name: "碰碰车" }];
const POINTS = [{ seq: 1, point_name: "入口闸机", point_code: "AAAABBBBCCCC" }, { seq: 2, point_name: "安全压杠", point_code: "DDDDEEEEFFFF" }];
const FIRST = { seq: 1, point_name: "入口闸机", result: "pass", checked_by: "usr-1", checked_at: "2026-11-20T00:10:00Z" };
const SECOND = { seq: 2, point_name: "安全压杠", result: "pass", checked_by: "usr-1", checked_at: "2026-11-20T00:12:00Z" };
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function paged(items: unknown[]): Response {
  return ok({ items, page: 1, page_size: 50, total: items.length });
}

let walk: unknown[] = [];

function renderPage() {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET") === "POST") return Promise.resolve(ok({ id: "insp-new" }));
    if (url.includes("/amusement/projects/amuse-1/inspection-points")) return Promise.resolve(ok({ project_id: "amuse-1", points: POINTS, walk }));
    if (url.includes("/amusement/projects/amuse-2/inspection-points")) return Promise.resolve(ok({ project_id: "amuse-2", points: [], walk: [] }));
    if (url.includes("/amusement/safety-inspections")) return Promise.resolve(paged(ROWS));
    if (url.includes("/amusement/projects")) return Promise.resolve(paged(PROJECTS));
    return Promise.resolve(paged([]));
  }));
  render(<AmusementSafetyPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
    setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
}

function calls(match: (url: string, method: string) => boolean) {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) => match(String(input), (init as RequestInit | undefined)?.method ?? "GET"));
}

const createPosts = () => calls((url, method) => method === "POST" && url.endsWith("/api/v1/amusement/safety-inspections"));

async function openCreateFor(projectId: string) {
  await waitFor(() => expect((screen.queryByTestId("amusement-safety-select-i-1") as HTMLInputElement | null)?.checked).toBe(true));
  fireEvent.click(screen.getByTestId("amusement-safety-create-open"));
  await waitFor(() => expect(screen.queryByTestId("amusement-safety-create-project")).not.toBeNull());
  fireEvent.change(screen.getByTestId("amusement-safety-create-result"), { target: { value: "fail" } });
  fireEvent.change(screen.getByTestId("amusement-safety-create-project"), { target: { value: projectId } });
  await waitFor(() => expect(calls((url) => url.includes(`/amusement/projects/${projectId}/inspection-points`)).length).toBeGreaterThan(0));
}

describe("AmusementSafetyPage 巡检点位走点", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    walk = [];
  });

  it("配了点位：只读显示走点进度、不手填检查项；没走完不发请求，走完后创建不带检查项", async () => {
    walk = [FIRST];
    renderPage();
    await openCreateFor("amuse-1");
    await waitFor(() => expect(screen.getByTestId("amusement-inspection-walk-row-2").textContent).toContain("未记录"));
    expect(screen.getByTestId("amusement-inspection-walk-row-1").textContent).toContain("入口闸机");
    expect(screen.getByTestId("amusement-inspection-walk-row-1").textContent).toContain("通过");
    expect(screen.queryByTestId("amusement-safety-create-title")).toBeNull();
    expect(screen.queryByText("记录本点位")).toBeNull();

    fireEvent.click(screen.getByTestId("amusement-safety-guided-next"));
    fireEvent.click(screen.getByTestId("amusement-safety-create"));
    await waitFor(() => expect(screen.getByTestId("amusement-safety-guided-error").textContent).toContain("还有点位没走完"));
    expect(createPosts()).toHaveLength(0);

    walk = [FIRST, SECOND];
    fireEvent.click(screen.getByTestId("amusement-safety-create"));
    await waitFor(() => expect(createPosts()).toHaveLength(1));
    const body = JSON.parse(String((createPosts()[0]![1] as RequestInit).body)) as Record<string, unknown>;
    expect(body.project_id).toBe("amuse-1");
    expect(body).not.toHaveProperty("items");
  });

  it("没配点位：不出走点进度，照旧填检查项、请求带检查项", async () => {
    renderPage();
    await openCreateFor("amuse-2");
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-create-title")).not.toBeNull());
    expect(screen.queryByTestId("amusement-inspection-walk")).toBeNull();
    fireEvent.change(screen.getByTestId("amusement-safety-create-title"), { target: { value: "开放前护栏检查" } });
    fireEvent.click(screen.getByTestId("amusement-safety-guided-next"));
    fireEvent.click(screen.getByTestId("amusement-safety-create"));
    await waitFor(() => expect(createPosts()).toHaveLength(1));
    const body = JSON.parse(String((createPosts()[0]![1] as RequestInit).body)) as { items?: Array<Record<string, unknown>> };
    expect(body.items?.[0]?.item_name).toBe("开放前护栏检查");
  });
});
