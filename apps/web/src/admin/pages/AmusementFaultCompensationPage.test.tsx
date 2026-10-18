// 文件版本：V0.03
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AmusementFaultCompensationPage } from "./AmusementFaultCompensationPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const FAULTS = [{ id: "f-1", project_id: "prj-1", status: "lifted", reason: "供电故障", activated_at: "2026-10-17T10:00:00Z", lifted_at: "2026-10-17T12:00:00Z" }];
const POLICY = { project_id: "__global__", validity_days: 365, min_days: 1, max_days: 3650 };
const PENDING = [{ id: "b-1", fault_id: "f-1", project_id: "prj-1", status: "pending_confirmation", reason: "供电故障补偿", bearer_company_id: "ent-amu", validity_days: 365, window_from: "2026-10-17T10:00:00Z", window_to: "2026-10-17T12:00:00Z", initiated_by: "u-head", initiated_role: "amusement_head", initiated_at: "2026-10-17T13:00:00Z", item_count: 2, extend_count: 1, reissue_count: 1 }];
// TK18-R2-COMP（2026-10-18 操作员裁决补充第 3 条）：场次提前结束时已入场客人的补偿批次由系统生成（无发起岗位），
// 发放前须游乐公司负责人与工程或安全岗位共同确认，沿用同一确认入口。
const SESSION_BATCHES = [{ id: "sb-1", event_type: "session_early_end", ending_id: "end-1", session_id: "ses-9", project_id: "prj-1", status: "pending_confirmation", reason: "场次提前结束：设备临时停机", bearer_company_id: "be_amusement", validity_days: 365, window_from: "2026-10-17T20:00:00Z", window_to: "2026-10-17T22:00:00Z", item_count: 1, extend_count: 0, reissue_count: 1 }];
const ITEMS = [
  { id: "i-1", entitlement_id: "ent-a", parent_ticket_id: "tkt-1", slot_index: 1, customer_id: "cus-1", kind: "full_reissue", share_count: 1, original_session_id: "ses-1", status: "pending", compensation_entitlement_ids: [] },
  { id: "i-2", entitlement_id: "ent-b", parent_ticket_id: "tkt-1", slot_index: 2, customer_id: "cus-1", kind: "extend", share_count: 1, status: "pending", compensation_entitlement_ids: [] },
];

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function stubFetch(batches: unknown[], sessionBatches: unknown[] = []) {
  const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes("/compensation-policies/") && init?.method === "POST") return Promise.resolve(dataEnvelope({ project_id: "prj-1", validity_days: 400, min_days: 30, max_days: 730 }));
    if (url.includes("/compensation-policies/")) return Promise.resolve(dataEnvelope(POLICY));
    if (url.includes("/compensation-batches/") && url.endsWith("/confirm")) return Promise.resolve(dataEnvelope({ id: "b-1", status: "issued" }));
    if (url.includes("/compensation-batches/") && url.endsWith("/items")) return Promise.resolve(listEnvelope(ITEMS));
    if (url.includes("/compensation-batches") && init?.method === "POST") return Promise.resolve(dataEnvelope({ id: "b-2", status: "pending_confirmation" }));
    if (url.includes("/compensation-batches?event_type=session_early_end")) return Promise.resolve(listEnvelope(sessionBatches));
    if (url.includes("/compensation-batches")) return Promise.resolve(listEnvelope(batches));
    if (url.includes("/park-wide-faults")) return Promise.resolve(listEnvelope(FAULTS));
    return Promise.resolve(listEnvelope([]));
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<AmusementFaultCompensationPage {...props} />);
  return setActionResult;
}

function bodyOf(fetchMock: ReturnType<typeof vi.fn>, predicate: (url: string, init?: RequestInit) => boolean): Record<string, unknown> {
  const call = fetchMock.mock.calls.find((c) => predicate(String(c[0]), c[1] as RequestInit | undefined));
  return JSON.parse(String((call?.[1] as RequestInit | undefined)?.body ?? "{}")) as Record<string, unknown>;
}

describe("AmusementFaultCompensationPage 全园故障补偿（CG-08b，ENUM「全园故障与补偿通票」）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("待确认批次：显示快照记录（延长 / 完整补发）与出厂有效期，另一岗位确认发放调后端", async () => {
    const fetchMock = stubFetch(PENDING);
    const setActionResult = renderPage();
    await waitFor(() => expect(screen.getByTestId("compensation-batch-status-b-1").textContent).toBe("待第二岗位确认"));
    expect(screen.getByTestId("compensation-policy-current").textContent).toContain("365 天");
    await waitFor(() => expect(screen.getByTestId("compensation-item-kind-i-1").textContent).toBe("另补一次完整票面权益"));
    expect(screen.getByTestId("compensation-item-kind-i-2").textContent).toBe("延长原核销权");
    expect(screen.queryByTestId("compensation-initiate")).toBeNull();
    fireEvent.change(screen.getByTestId("compensation-role"), { target: { value: "engineering_safety" } });
    fireEvent.change(screen.getByTestId("compensation-note"), { target: { value: "工程岗确认发放" } });
    fireEvent.click(screen.getByTestId("compensation-confirm"));
    await waitFor(() => expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/api/v1/amusement/compensation-batches/b-1/confirm"))).toBe(true));
    const body = bodyOf(fetchMock, (url) => url.includes("/compensation-batches/b-1/confirm"));
    expect(body.role).toBe("engineering_safety");
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 确认岗位: "工程或安全岗位" })));
  });

  it("无待确认批次：发起须填原因与承担主体，提交后按故障事件调后端", async () => {
    const fetchMock = stubFetch([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId("compensation-initiate")).toBeTruthy());
    fireEvent.click(screen.getByTestId("compensation-initiate"));
    expect(screen.getByTestId("compensation-error").textContent).toContain("承担主体");
    fireEvent.change(screen.getByTestId("compensation-reason"), { target: { value: "供电故障补偿" } });
    fireEvent.change(screen.getByTestId("compensation-bearer"), { target: { value: "ent-amu" } });
    fireEvent.click(screen.getByTestId("compensation-initiate"));
    await waitFor(() => expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/api/v1/amusement/park-wide-faults/f-1/compensation-batches") && (c[1] as RequestInit | undefined)?.method === "POST")).toBe(true));
    const raw = String((fetchMock.mock.calls.find((c) => String(c[0]).includes("/f-1/compensation-batches") && (c[1] as RequestInit | undefined)?.method === "POST")?.[1] as RequestInit | undefined)?.body ?? "");
    expect(raw).toContain("ent-amu");
    expect(raw).toContain("供电故障补偿");
    expect(raw).toContain("amusement_head");
  });

  it("补偿有效期只接受1至3650内整数，不向服务端提交越界上限", async () => {
    const fetchMock = stubFetch([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId("compensation-policy-save")).toBeTruthy());
    for (const value of ["0", "3651", "1.5"]) {
      fireEvent.change(screen.getByTestId("compensation-policy-days"), { target: { value } });
      fireEvent.change(screen.getByTestId("compensation-policy-min"), { target: { value: "1" } });
      fireEvent.change(screen.getByTestId("compensation-policy-max"), { target: { value: "3651" } });
      fireEvent.click(screen.getByTestId("compensation-policy-save"));
      expect(screen.getByTestId("compensation-error").textContent).toContain("3650");
    }
    expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/compensation-policies/") && (c[1] as RequestInit)?.method === "POST")).toBe(false);
    fireEvent.change(screen.getByTestId("compensation-policy-days"), { target: { value: "3650" } });
    fireEvent.change(screen.getByTestId("compensation-policy-max"), { target: { value: "3650" } });
    fireEvent.click(screen.getByTestId("compensation-policy-save"));
    await waitFor(() => expect(bodyOf(fetchMock, (url, init) => url.includes("/compensation-policies/") && init?.method === "POST").validity_days).toBe(3650));
  });

  it("补偿有效期参数：天数、下限、上限须同时填写，提交后按项目调后端", async () => {
    const fetchMock = stubFetch([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId("compensation-policy-save")).toBeTruthy());
    fireEvent.change(screen.getByTestId("compensation-policy-days"), { target: { value: "400" } });
    fireEvent.click(screen.getByTestId("compensation-policy-save"));
    expect(screen.getByTestId("compensation-error").textContent).toContain("下限");
    fireEvent.change(screen.getByTestId("compensation-policy-min"), { target: { value: "30" } });
    fireEvent.change(screen.getByTestId("compensation-policy-max"), { target: { value: "730" } });
    fireEvent.click(screen.getByTestId("compensation-policy-save"));
    await waitFor(() => expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/api/v1/amusement/compensation-policies/prj-1") && (c[1] as RequestInit | undefined)?.method === "POST")).toBe(true));
    const raw = String((fetchMock.mock.calls.find((c) => String(c[0]).includes("/compensation-policies/prj-1") && (c[1] as RequestInit | undefined)?.method === "POST")?.[1] as RequestInit | undefined)?.body ?? "");
    expect(raw).toContain("400");
    expect(raw).toContain("730");
  });

  it("场次提前结束补偿批次：按事件选择后显示待两岗位共同确认，确认走同一发放入口", async () => {
    const fetchMock = stubFetch([], SESSION_BATCHES);
    renderPage();
    const select = await screen.findByTestId("compensation-fault");
    await waitFor(() => expect(within(select).getAllByRole("option").some((o) => (o.textContent ?? "").includes("场次提前结束"))).toBe(true));
    fireEvent.change(select, { target: { value: "session:sb-1" } });
    await waitFor(() => expect(screen.getByTestId("compensation-batch-status-sb-1").textContent).toBe("待游乐公司负责人与工程或安全岗位共同确认"));
    expect(screen.queryByTestId("compensation-initiate")).toBeNull();
    fireEvent.change(screen.getByTestId("compensation-role"), { target: { value: "amusement_head" } });
    fireEvent.click(screen.getByTestId("compensation-confirm"));
    await waitFor(() => expect(fetchMock.mock.calls.some((c) => String(c[0]).includes("/api/v1/amusement/compensation-batches/sb-1/confirm"))).toBe(true));
    expect(bodyOf(fetchMock, (url) => url.includes("/compensation-batches/sb-1/confirm")).role).toBe("amusement_head");
  });
});
