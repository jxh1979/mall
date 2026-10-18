// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementParkWideFaultPage } from "./AmusementParkWideFaultPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const PENDING = [{ id: "f-1", project_id: "prj-1", status: "pending_activation", reason: "供电故障，游乐园整体关闭", initiated_by: "u-head", initiated_role: "amusement_head", initiated_at: "2026-10-17T10:00:00Z" }];
const ACTIVE = [{ id: "f-2", project_id: "prj-1", status: "active", reason: "消防系统故障", initiated_by: "u-head", initiated_role: "amusement_head", initiated_at: "2026-10-17T10:00:00Z", activation_confirmed_by: "u-eng", activation_confirmed_role: "engineering_safety", activated_at: "2026-10-17T10:05:00Z" }];
const ACTIONS = [
  { id: "a-1", action: "initiate", actor_id: "u-head", actor_role: "amusement_head", note: "供电故障", created_at: "2026-10-17T10:00:00Z" },
];

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function stubFetch(faults: unknown[]) {
  const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("/actions")) return Promise.resolve(listEnvelope(ACTIONS));
    if (/\/park-wide-faults\/[^/]+\/(confirm-activation|initiate-lift|confirm-lift)/.test(url)) return Promise.resolve(dataEnvelope({ id: "f-x", status: "active" }));
    if (url.includes("/park-wide-faults")) return Promise.resolve(listEnvelope(faults));
    return Promise.resolve(listEnvelope([]));
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<AmusementParkWideFaultPage {...props} />);
  return setActionResult;
}

function bodyOf(fetchMock: ReturnType<typeof vi.fn>, fragment: string): Record<string, unknown> {
  const call = fetchMock.mock.calls.find((c) => String(c[0]).includes(fragment));
  return JSON.parse(String((call?.[1] as RequestInit | undefined)?.body ?? "{}")) as Record<string, unknown>;
}

describe("AmusementParkWideFaultPage 全园故障状态（CG-08a，ENUM「全园故障与补偿通票」）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("待第二岗位确认：显示状态与留痕，选岗位后确认生效调后端", async () => {
    const fetchMock = stubFetch(PENDING);
    const setActionResult = renderPage();
    await waitFor(() => expect(screen.getByTestId("park-fault-current-status").textContent).toBe("待第二岗位确认生效"));
    expect(screen.getByTestId("park-fault-action-a-1").textContent).toContain("发起全园故障状态");
    expect(screen.getByTestId("park-fault-action-a-1").textContent).toContain("游乐公司负责人");
    expect(screen.queryByTestId("park-fault-initiate")).toBeNull();
    fireEvent.change(screen.getByTestId("park-fault-role"), { target: { value: "engineering_safety" } });
    fireEvent.change(screen.getByTestId("park-fault-note"), { target: { value: "工程岗确认" } });
    fireEvent.click(screen.getByTestId("park-fault-confirm-activation"));
    await waitFor(() => expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/api/v1/amusement/park-wide-faults/f-1/confirm-activation"))).toBe(true));
    const body = bodyOf(fetchMock, "/f-1/confirm-activation");
    expect(body.role).toBe("engineering_safety");
    expect(JSON.stringify(body)).toContain("工程岗确认");
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 确认岗位: "工程或安全岗位" })));
  });

  it("生效中：发起解除须填依据，填后带岗位与依据调后端", async () => {
    const fetchMock = stubFetch(ACTIVE);
    renderPage();
    await waitFor(() => expect(screen.getByTestId("park-fault-current-status").textContent).toContain("生效中"));
    fireEvent.click(screen.getByTestId("park-fault-initiate-lift"));
    expect(screen.getByTestId("park-fault-error").textContent).toContain("解除依据");
    expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/initiate-lift"))).toBe(false);
    fireEvent.change(screen.getByTestId("park-fault-reason"), { target: { value: "供电恢复，全部项目复检通过" } });
    fireEvent.click(screen.getByTestId("park-fault-initiate-lift"));
    await waitFor(() => expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/api/v1/amusement/park-wide-faults/f-2/initiate-lift"))).toBe(true));
    const body = bodyOf(fetchMock, "/f-2/initiate-lift");
    expect(body.role).toBe("amusement_head");
    expect(JSON.stringify(body)).toContain("供电恢复");
  });

  it("无未解除记录：发起须填项目与故障情形，提交后调后端", async () => {
    const fetchMock = stubFetch([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId("park-fault-none")).toBeTruthy());
    fireEvent.click(screen.getByTestId("park-fault-initiate"));
    expect(screen.getByTestId("park-fault-error").textContent).toContain("综合体项目");
    fireEvent.change(screen.getByTestId("park-fault-project"), { target: { value: "prj-1" } });
    fireEvent.change(screen.getByTestId("park-fault-reason"), { target: { value: "入口系统故障，游乐园整体关闭" } });
    fireEvent.click(screen.getByTestId("park-fault-initiate"));
    await waitFor(() => expect(fetchMock.mock.calls.some((c) => String(c[0]).endsWith("/api/v1/amusement/park-wide-faults") && (c[1] as RequestInit | undefined)?.method === "POST")).toBe(true));
    const call = fetchMock.mock.calls.find((c) => String(c[0]).endsWith("/api/v1/amusement/park-wide-faults") && (c[1] as RequestInit | undefined)?.method === "POST");
    const raw = String((call?.[1] as RequestInit | undefined)?.body ?? "");
    expect(raw).toContain("prj-1");
    expect(raw).toContain("入口系统故障");
    expect(raw).toContain("amusement_head");
  });
});
