// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// HP-F01（BSM「楼宇运营」告警条 L1417；2026-10-21 操作员裁决补充第 1 条）：告警/故障上报当场选优先级；当班接警人在「我的接警」
// 点「已收到并接手」才算明确接手；值守安排按班次登记角色与联系电话；未覆盖告警由专业处置人员记录判定。
const MINE = [{ id: "falm-1", equipmentId: "eq-1", alarmType: "elevator", priority: "urgent", status: "awaiting_takeover", responseDueAt: "2026-10-21T10:00:00Z", uncovered: true }];
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.endsWith("/facility/alarms/mine") ? MINE : [])),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

describe("FacilityWorkbenchPage 设备接警（HP-F01）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("我的接警点「已收到并接手」→ POST take-over；查看列表本身不提交任何接手", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post));

    await waitFor(() => expect(screen.queryByTestId("facility-alarm-take-over-falm-1")).not.toBeNull());
    expect(post).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("facility-alarm-take-over-falm-1"));

    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/alarms/falm-1/take-over", expect.any(Object)));
  });

  it("告警上报须当场选优先级：未选拒绝提交，选了随请求带 priority", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    const setGlobalError = vi.fn();
    renderPage(makeApi(post), setGlobalError);

    await waitFor(() => expect(screen.queryByTestId("facility-equipment-status-priority")).not.toBeNull());
    fireEvent.change(screen.getByTestId("facility-equipment-status-id"), { target: { value: "eq-1" } });
    fireEvent.change(screen.getByTestId("facility-equipment-status"), { target: { value: "alarm" } });
    fireEvent.click(screen.getByTestId("facility-equipment-status-change"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("优先级")));
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("facility-equipment-status-priority"), { target: { value: "urgent" } });
    fireEvent.click(screen.getByTestId("facility-equipment-status-change"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/equipment/eq-1/status",
      expect.objectContaining({ status: "alarm", alarmSource: "manual", priority: "urgent" })));
  });

  it("值守安排提交经客户端转为 shift_id / duty_role / contact_phone 蛇形键", async () => {
    const fetchMock = vi.fn(async (url: string, _init?: RequestInit) => new Response(JSON.stringify({
      code: "OK", message: "ok", data: url.includes("/alarm-duties") && _init?.method === "POST" ? { id: "fadu-1" } : [],
    }), { status: 200, headers: { "content-type": "application/json" } }));
    vi.stubGlobal("fetch", fetchMock);
    renderPage(new AdminApi() as unknown as AdminApiClient);

    await waitFor(() => expect(screen.queryByTestId("facility-alarm-duty-shift")).not.toBeNull());
    fireEvent.change(screen.getByTestId("facility-alarm-duty-shift"), { target: { value: "shift-1" } });
    fireEvent.change(screen.getByTestId("facility-alarm-duty-role"), { target: { value: "backup" } });
    fireEvent.change(screen.getByTestId("facility-alarm-duty-phone"), { target: { value: "13800000000" } });
    fireEvent.click(screen.getByTestId("facility-alarm-duty-assign"));

    await waitFor(() => expect(fetchMock.mock.calls.some(([url, init]) => String(url).endsWith("/api/v1/facility/alarm-duties")
      && (init as RequestInit | undefined)?.method === "POST")).toBe(true));
    const [, init] = fetchMock.mock.calls.find(([url, req]) => String(url).endsWith("/api/v1/facility/alarm-duties")
      && (req as RequestInit | undefined)?.method === "POST")!;
    expect(JSON.parse(String((init as RequestInit).body))).toMatchObject({ shift_id: "shift-1", duty_role: "backup", contact_phone: "13800000000" });
  });

  it("未覆盖告警由专业处置人员记录判定 → POST disposition 带 judgement", async () => {
    const post = vi.fn(async () => ({}) as unknown);
    renderPage(makeApi(post));

    await waitFor(() => expect(screen.queryByTestId("facility-alarm-dispose-falm-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("facility-alarm-dispose-falm-1"));
    fireEvent.change(screen.getByTestId("facility-alarm-disposition-judgement"), { target: { value: "停用该梯，等维保到场" } });
    fireEvent.click(screen.getByTestId("facility-alarm-disposition-submit"));

    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/facility/alarms/falm-1/disposition",
      expect.objectContaining({ judgement: "停用该梯，等维保到场" })));
  });
});
