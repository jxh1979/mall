// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSafetyPage } from "./AmusementSafetyPage";
import { AdminApi } from "../client/adminClient";
import type { EffectivePermissionsState } from "../types";

// 第十八批 BS 段第 373 条 373-R3、373-R5（BSM:1300「巡检必须按点位顺序完成……」「营业中抽检有最低频次要求，每隔多久至少抽检一次由后台配置
// （安全阈值，按项目可配，须同时配置上限与下限，出厂默认每 2 小时一次），达不到最低频次的视为当班未抽检」；第十八批裁决 76「每个点位生成一张码
// 贴在现场」）：安全巡检页的规则块——点位清单按文本框行序保存、逐个显示点位码二维码；抽检间隔三项同填保存，最高权限闸拒绝说清找谁办；
// 当班未抽检记录列表。经真实后台客户端，假响应为文本体。
const ROWS = [
  { id: "i-1", inspection_no: "INS-1", project_id: "amuse-1", project_name: "旋转木马", overall_result: "fail", inspection_status: "submitted",
    rectification_status: "pending", reopen_status: "pending", inspected_at: "2026-11-16T02:00:00Z", evidence_attachment_ids: [] },
];
const PROJECTS = [{ id: "amuse-1", project_name: "旋转木马" }];
const POINTS = [{ seq: 1, point_name: "入口闸机", point_code: "AAAABBBBCCCC" }, { seq: 2, point_name: "安全压杠", point_code: "DDDDEEEEFFFF" }];
const MISS = { id: "spm_1", project_id: "amuse-1", project_name: "过山车甲", complex_id: "prj-1", anchor_at: "2026-11-20T04:00:00Z",
  first_admission_at: "2026-11-20T06:01:00Z", interval_minutes: 120, detected_at: "2026-11-20T06:05:00Z" };
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
let rows: unknown[] = ROWS;

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function paged(items: unknown[]): Response {
  return ok({ items, page: 1, page_size: 50, total: items.length });
}

function renderPage(setGlobalError = vi.fn()) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    if (url.includes("/inspection-points") && method === "PUT") {
      const names = (JSON.parse(String(init?.body)) as { points: string[] }).points;
      return Promise.resolve(ok({ project_id: "amuse-1", walk: [],
        points: names.map((name, index) => ({ seq: index + 1, point_name: name, point_code: `CODE${index + 1}XXXXXXX` })) }));
    }
    if (url.includes("/inspection-points")) return Promise.resolve(ok({ project_id: "amuse-1", points: POINTS, walk: [] }));
    if (url.includes("/spot-check-policies") && method === "PUT") {
      return Promise.resolve(new Response(JSON.stringify({ code: "FORBIDDEN", detail_code: "MASTER_APPROVAL_REQUIRED",
        message: "营业中抽检间隔是安全阈值，调整须由最高权限本人办理" }), { status: 403, headers: { "content-type": "application/json" } }));
    }
    if (url.includes("/spot-check-policies")) return Promise.resolve(ok({ project_id: "__global__", interval_minutes: 120, min_minutes: null, max_minutes: null }));
    if (url.includes("/spot-check-misses")) return Promise.resolve(ok([MISS]));
    if (url.includes("/amusement/safety-inspections")) return Promise.resolve(paged(rows));
    if (url.includes("/amusement/projects")) return Promise.resolve(paged(PROJECTS));
    return Promise.resolve(paged([]));
  }));
  render(<AmusementSafetyPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
    setActionResult={vi.fn()} setGlobalError={setGlobalError} effectivePermissions={ADMIN_PERMS} />);
  return setGlobalError;
}

function puts(part: string) {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) => (init as RequestInit | undefined)?.method === "PUT" && String(input).includes(part));
}

function jsonBody(call: unknown[]): Record<string, unknown> {
  return JSON.parse(String((call[1] as RequestInit).body)) as Record<string, unknown>;
}

describe("AmusementInspectionRulesSection 巡检点位与营业中抽检", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    rows = ROWS;
  });

  it("当班未抽检记录列出项目、上次巡检时间、首次超间隔入场与当时间隔", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("amusement-rules-miss-spm_1").textContent).toContain("过山车甲"));
    expect(screen.getByTestId("amusement-rules-miss-spm_1").textContent).toContain("120");
  });

  it("点位清单读回后逐个显示点位码；保存按文本框行序提交，保存后新点位也出码", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("amusement-rules-points-project")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("amusement-rules-points-project").textContent).toContain("旋转木马"));
    fireEvent.change(screen.getByTestId("amusement-rules-points-project"), { target: { value: "amuse-1" } });
    await waitFor(() => expect((screen.getByTestId("amusement-rules-points-text") as HTMLTextAreaElement).value).toBe("入口闸机\n安全压杠"));
    expect(screen.getByTestId("amusement-rules-point-qr-1").querySelector("svg")).not.toBeNull();
    expect(screen.getByTestId("amusement-rules-point-qr-2").textContent).toContain("DDDDEEEEFFFF");

    fireEvent.change(screen.getByTestId("amusement-rules-points-text"), { target: { value: "入口闸机\n安全压杠\n\n出口通道" } });
    fireEvent.click(screen.getByTestId("amusement-rules-points-save"));
    await waitFor(() => expect(puts("/inspection-points")).toHaveLength(1));
    expect(jsonBody(puts("/inspection-points")[0]!).points).toEqual(["入口闸机", "安全压杠", "出口通道"]);
    await waitFor(() => expect(screen.getByTestId("amusement-rules-point-qr-3").querySelector("svg")).not.toBeNull());
  });

  // 点位清单与抽检间隔要在第一次巡检之前配（BSM:1300 抽检频次「由后台配置」；第十八批裁决 76「每个点位生成一张码贴在现场」）。
  it("一条巡检都没有：规则块与新建巡检按钮照样显示，暂无数据只占巡检表格区", async () => {
    rows = [];
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("amusement-safety-create-open")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("amusement-rules-miss-spm_1").textContent).toContain("过山车甲"));
    expect(screen.getByTestId("amusement-rules-points-save")).not.toBeNull();
    expect(screen.getByTestId("amusement-rules-spot-save")).not.toBeNull();
    expect(screen.getByTestId("admin-empty").textContent).toContain("暂无数据");
    expect(screen.queryByTestId("amusement-safety-table")).toBeNull();
  });

  it("抽检间隔三项同填保存，最高权限闸拒绝时说清须由最高权限本人办理", async () => {
    const setGlobalError = renderPage();
    await waitFor(() => expect(screen.queryByTestId("amusement-rules-spot-project")).not.toBeNull());
    fireEvent.change(screen.getByTestId("amusement-rules-spot-project"), { target: { value: "prj-1" } });
    fireEvent.click(screen.getByTestId("amusement-rules-spot-query"));
    await waitFor(() => expect(screen.getByTestId("amusement-rules-spot-current").textContent).toContain("出厂值"));
    expect((screen.getByTestId("amusement-rules-spot-interval") as HTMLInputElement).value).toBe("120");

    fireEvent.change(screen.getByTestId("amusement-rules-spot-interval"), { target: { value: "90" } });
    fireEvent.change(screen.getByTestId("amusement-rules-spot-min"), { target: { value: "60" } });
    fireEvent.change(screen.getByTestId("amusement-rules-spot-max"), { target: { value: "180" } });
    fireEvent.click(screen.getByTestId("amusement-rules-spot-save"));
    await waitFor(() => expect(puts("/spot-check-policies/prj-1")).toHaveLength(1));
    expect(jsonBody(puts("/spot-check-policies/prj-1")[0]!)).toMatchObject({ interval_minutes: 90, min_minutes: 60, max_minutes: 180 });
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("营业中抽检间隔是安全阈值，调整须由最高权限本人办理"));
  });
});
