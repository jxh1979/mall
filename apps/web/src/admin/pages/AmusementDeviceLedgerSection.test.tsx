// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSafetyPage } from "./AmusementSafetyPage";
import { AdminApi } from "../client/adminClient";
import type { EffectivePermissionsState } from "../types";

// 第十八批 CF 段第 407 条 407-R3（BSM:1271「每台关键安全设备属于哪一种，须在设备台账中登记」「判断结论必须留痕，记下是谁判的、什么时间判的、依据了哪几项条件」）：
// 安全巡检页的设备台账——选游乐项目后列设备（分级、依赖关系、分级复核中提示）、新建设备按蛇形键提交分级与理由、停用的辅助设备填「能否继续运行」判定
// （依据条件逐行一项）。经真实后台客户端，假响应为文本体。
const PROJECTS = [{ id: "amuse-1", project_name: "旋转木马" }];
const DEVICES = [
  { id: "dev-1", project_id: "amuse-1", device_code: "RIDE-1", name: "安全压杠", status: "active", safety_check_interval_hours: 24,
    safety_class: null, dependency: null, last_pass_inspection_at: "2026-11-20T00:10:00Z" },
  { id: "dev-2", project_id: "amuse-1", device_code: "RIDE-2", name: "排队闸机", status: "active", safety_check_interval_hours: 12,
    safety_class: "auxiliary", dependency: null, last_pass_inspection_at: "2026-11-18T00:10:00Z" },
];
const CHANGES = [{ id: "chg-1", device_id: "dev-1", project_id: "amuse-1", to_class: "critical", to_dependency: "dependent",
  reason: "首次分级", status: "pending_review", requested_by: "usr-mgr", requested_at: "2026-11-20T01:00:00Z" }];
const STOPS = [{ device_id: "dev-2", device_code: "RIDE-2", name: "排队闸机", safety_class: "auxiliary", reasons: ["expired"],
  stopped_at: "2026-11-19T00:10:00Z", latest_judgement: null },
  { device_id: "dev-4", device_code: "RIDE-4", name: "主轴", safety_class: "critical", dependency: "dependent", reasons: ["inspection_failed"],
    stopped_at: "2026-11-19T02:00:00Z", latest_judgement: null }];
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function paged(items: unknown[]): Response {
  return ok({ items, page: 1, page_size: 50, total: items.length });
}

function renderPage() {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    if (method === "POST" && url.includes("/continue-judgements")) return Promise.resolve(ok({ id: "jdg-1", conclusion: "continue" }));
    if (method === "POST" && url.endsWith("/amusement/projects/amuse-1/devices")) return Promise.resolve(ok({ id: "dev-3" }));
    if (url.includes("/devices/classification-changes")) return Promise.resolve(ok(CHANGES));
    if (url.includes("/devices/stops")) return Promise.resolve(ok(STOPS));
    if (url.endsWith("/amusement/projects/amuse-1/devices")) return Promise.resolve(paged(DEVICES));
    if (url.endsWith("/api/v1/amusement/projects")) return Promise.resolve(paged(PROJECTS));
    return Promise.resolve(paged([]));
  }));
  render(<AmusementSafetyPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
    setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
}

function posts(part: string) {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) => (init as RequestInit | undefined)?.method === "POST"
    && String(input).includes(part));
}

function jsonBody(call: unknown[]): Record<string, unknown> {
  return JSON.parse(String((call[1] as RequestInit).body)) as Record<string, unknown>;
}

async function selectProject() {
  await waitFor(() => expect(screen.getByTestId("amusement-device-ledger-project").textContent).toContain("旋转木马"));
  fireEvent.change(screen.getByTestId("amusement-device-ledger-project"), { target: { value: "amuse-1" } });
  await waitFor(() => expect(screen.getByTestId("amusement-device-row-dev-2").textContent).toContain("辅助设备"));
}

describe("AmusementDeviceLedgerSection 游乐设备台账", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("设备表显示分级，分级复核中的设备标「分级待工程／安全复核」，分级历史列出申请", async () => {
    renderPage();
    await selectProject();
    expect(screen.getByTestId("amusement-device-row-dev-1").textContent).toContain("分级待工程／安全复核");
    expect(screen.getByTestId("amusement-device-change-chg-1").textContent).toContain("关键安全设备");
    expect(screen.getByTestId("amusement-device-change-chg-1").textContent).toContain("首次分级");
  });

  it("新建关键安全设备：请求体蛇形键带 safety_class、dependency、operator_note", async () => {
    renderPage();
    await selectProject();
    fireEvent.change(screen.getByTestId("amusement-device-create-code"), { target: { value: "RIDE-3" } });
    fireEvent.change(screen.getByTestId("amusement-device-create-name"), { target: { value: "主轴" } });
    fireEvent.change(screen.getByTestId("amusement-device-create-interval"), { target: { value: "24" } });
    fireEvent.click(screen.getByTestId("amusement-device-create-class-critical"));
    fireEvent.change(screen.getByTestId("amusement-device-create-dependency"), { target: { value: "dependent" } });
    fireEvent.change(screen.getByTestId("amusement-device-create-reason"), { target: { value: "主轴断了整台停" } });
    fireEvent.click(screen.getByTestId("amusement-device-create-submit"));
    await waitFor(() => expect(posts("/amusement/projects/amuse-1/devices")).toHaveLength(1));
    const body = jsonBody(posts("/amusement/projects/amuse-1/devices")[0]!);
    expect(body).toMatchObject({ device_code: "RIDE-3", safety_class: "critical", dependency: "dependent", operator_note: "主轴断了整台停",
      safety_check_interval_hours: 24 });
  });

  it("停用的辅助设备提交「能否继续运行」判定：conditions 为逐行数组、conclusion=continue", async () => {
    renderPage();
    await selectProject();
    await waitFor(() => expect(screen.getByTestId("amusement-device-stop-dev-2").textContent).toContain("巡检超期"));
    fireEvent.change(screen.getByTestId("amusement-device-judge-conditions-dev-2"), { target: { value: "其余关键设备正常\n\n已设隔离围挡" } });
    fireEvent.click(screen.getByTestId("amusement-device-judge-submit-dev-2"));
    await waitFor(() => expect(posts("/devices/dev-2/continue-judgements")).toHaveLength(1));
    const body = jsonBody(posts("/devices/dev-2/continue-judgements")[0]!);
    expect(body.conclusion).toBe("continue");
    expect(body.conditions).toEqual(["其余关键设备正常", "已设隔离围挡"]);
  });

  // 收尾复核修正（BSM:1271「每台关键安全设备属于哪一种，须在设备台账中登记，作为故障时判定停机范围的依据」；「没有留痕的一律按停止运行处理」只对辅助设备）：
  // 停用的关键安全设备行分级带依赖关系，最近判定一列为「—」（关键安全设备不做继续运行判定）；辅助设备行照旧「未判定（按停止运行处理）」。
  it("停用的关键安全设备行显示依赖关系、最近判定为「—」", async () => {
    renderPage();
    await selectProject();
    await waitFor(() => expect(screen.getByTestId("amusement-device-stop-dev-4").textContent).toContain("巡检不合格"));
    const critical = screen.getByTestId("amusement-device-stop-dev-4").querySelectorAll("td");
    expect(critical[1]?.textContent).toBe("关键安全设备（依赖（必经环节））");
    expect(critical[4]?.textContent).toBe("—");
    expect(screen.getByTestId("amusement-device-stop-dev-2").querySelectorAll("td")[4]?.textContent).toBe("未判定（按停止运行处理）");
  });
});
