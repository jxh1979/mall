// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementSessionsPage } from "./AmusementSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { EffectivePermissionsState } from "../types";

// 第十八批 CF 段第 409 条 409-R2（BSM:1267「达到运营容量（软上限）时的处置方式可在后台配置为两种之一：「记录异常事件」（超过软上限即生成异常事件留痕，
// 出厂默认）或「仅提示」（只在现场提示、不生成异常事件）」）：「项目与场次」页选游乐项目后显示当前运营容量与处置方式，改成「仅提示」保存时请求体为蛇形键、
// 运营容量是数字；没有配置权限时保存按钮不可点。经真实后台客户端，假响应为文本体。
const PROJECTS = [{ id: "amuse-1", project_name: "旋转木马", capacity: 30, status: "open" }];
const POLICY = { project_id: "amuse-1", operating_capacity: 20, disposal_mode: "block_hard", enabled: true };
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY: EffectivePermissionsState = { status: "ready",
  value: { grants: ["amusement.session.view", "amusement.over_capacity_policy.view"], denies: [], wildcard: false } };

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(permissions: EffectivePermissionsState = ADMIN_PERMS) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? "GET";
    if (url.includes("/over-capacity-policy") && method === "PUT") return Promise.resolve(ok({ ...POLICY, ...JSON.parse(String(init?.body)) }));
    if (url.includes("/over-capacity-policy")) return Promise.resolve(ok(POLICY));
    if (url.endsWith("/api/v1/amusement/projects")) return Promise.resolve(ok({ items: PROJECTS, page: 1, page_size: 50, total: 1 }));
    return Promise.resolve(ok({ items: [], page: 1, page_size: 50, total: 0 }));
  }));
  render(<AmusementSessionsPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
    setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={permissions} />);
}

function puts() {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([input, init]) => (init as RequestInit | undefined)?.method === "PUT"
    && String(input).includes("/over-capacity-policy"));
}

async function selectProject() {
  await waitFor(() => expect(screen.getByTestId("amusement-overcap-project").textContent).toContain("旋转木马"));
  fireEvent.change(screen.getByTestId("amusement-overcap-project"), { target: { value: "amuse-1" } });
  await waitFor(() => expect(screen.getByTestId("amusement-overcap-current").textContent).toContain("记录异常事件"));
}

describe("AmusementOverCapacityPolicySection 软上限处置方式", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("选项目后显示当前运营容量与处置方式「记录异常事件」", async () => {
    renderPage();
    await selectProject();
    expect(screen.getByTestId("amusement-overcap-current").textContent).toContain("20");
    expect((screen.getByTestId("amusement-overcap-capacity") as HTMLInputElement).value).toBe("20");
  });

  it("改成「仅提示」保存：请求体蛇形键 disposal_mode=warn_only、operating_capacity 为数字", async () => {
    renderPage();
    await selectProject();
    fireEvent.click(screen.getByTestId("amusement-overcap-mode-warn_only"));
    fireEvent.change(screen.getByTestId("amusement-overcap-capacity"), { target: { value: "25" } });
    fireEvent.click(screen.getByTestId("amusement-overcap-save"));
    await waitFor(() => expect(puts()).toHaveLength(1));
    const body = JSON.parse(String((puts()[0]![1] as RequestInit).body)) as Record<string, unknown>;
    expect(body.disposal_mode).toBe("warn_only");
    expect(body.operating_capacity).toBe(25);
    expect(body).toHaveProperty("idempotency_key");
  });

  it("没有配置权限：保存按钮不可点", async () => {
    renderPage(VIEW_ONLY);
    await selectProject();
    expect((screen.getByTestId("amusement-overcap-save") as HTMLButtonElement).disabled).toBe(true);
  });
});
