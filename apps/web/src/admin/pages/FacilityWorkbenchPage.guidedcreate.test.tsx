import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FacilityWorkbenchPage } from "./FacilityWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 引导式流程 phase-2：新增设备改为分步弹窗（设备信息→责任与维保→确认）。仅改设备创建主流程，其余表单不动。
// 覆盖入口按钮 → 步进 → 设备信息步校验 → 确认提交，原 facility-equipment-* testid 仍在弹窗内。
function makeApi(postSpy: ReturnType<typeof vi.fn>): AdminApiClient {
  return {
    get: vi.fn(async () => [] as unknown),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: postSpy,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(postSpy: ReturnType<typeof vi.fn>) {
  const props: AdminPageProps = {
    api: makeApi(postSpy),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
  } as unknown as AdminPageProps;
  render(<FacilityWorkbenchPage {...props} />);
}

describe("FacilityWorkbenchPage 引导式新增设备", () => {
  afterEach(() => vi.clearAllMocks());

  it("入口按钮打开弹窗 → 设备名为空报错 → 补齐步进 → 确认提交设备", async () => {
    const postSpy = vi.fn(async (_path: string) => ({}) as unknown);
    renderPage(postSpy);

    await waitFor(() => expect(screen.getByTestId("facility-equipment-create-open")).toBeTruthy());
    // 未打开时弹窗字段不渲染；页面其余表单（状态变更）仍在。
    expect(screen.queryByTestId("facility-equipment-name")).toBeNull();
    expect(screen.getByTestId("facility-equipment-status-form")).toBeTruthy();

    fireEvent.click(screen.getByTestId("facility-equipment-create-open"));
    // 步1 设备信息：设备名为空 → 下一步校验报错。
    fireEvent.click(screen.getByTestId("facility-equipment-flow-next"));
    expect(screen.getByTestId("facility-equipment-flow-error").textContent).toContain("设备名称");

    // 补齐设备名 → 步进到责任步 → 确认页 → 提交。
    fireEvent.change(screen.getByTestId("facility-equipment-name"), { target: { value: "1号客梯" } });
    fireEvent.click(screen.getByTestId("facility-equipment-flow-next"));
    fireEvent.click(screen.getByTestId("facility-equipment-flow-next"));
    fireEvent.click(screen.getByTestId("facility-equipment-create"));

    await waitFor(() => {
      const posted = postSpy.mock.calls.map((c) => String(c[0]));
      expect(posted.some((p) => p.endsWith("/facility/equipment"))).toBe(true);
    });
    // 提交成功后弹窗关闭。
    await waitFor(() => expect(screen.queryByTestId("facility-equipment-name")).toBeNull());
  });
});
