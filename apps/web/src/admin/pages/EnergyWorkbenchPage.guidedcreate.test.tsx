import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { EnergyWorkbenchPage } from "./EnergyWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post = vi.fn().mockResolvedValue({ id: "meter-new" })): AdminApiClient {
  return {
    get: vi.fn().mockResolvedValue([]),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient): void {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<EnergyWorkbenchPage {...props} />);
}

// 引导式流程 phase-4：主创建流=新增仪表，从内联表单改为「＋新增仪表」按钮 → 分步引导弹窗。
describe("EnergyWorkbenchPage 引导式新增仪表", () => {
  it("入口→步进→提交携带表计字段（POST /meters 无 id）", async () => {
    const post = vi.fn().mockResolvedValue({ id: "meter-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("energy-meter-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("energy-meter-create-open"));
    // 步骤1 基本信息（type/gather 有默认值，填 name）
    fireEvent.change(screen.getByTestId("energy-meter-guided-name"), { target: { value: "公区总电表" } });
    fireEvent.click(screen.getByTestId("energy-meter-guided-next"));
    // 步骤2 归属与读数（默认值即可）
    fireEvent.click(screen.getByTestId("energy-meter-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("energy-meter-save"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/energy/meters")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/energy/meters");
    expect(call?.[1]).toMatchObject({ name: "公区总电表", meterType: "electricity", gatherObject: "public_area", readMode: "remote", active: true });
    await waitFor(() => expect(screen.queryByTestId("energy-meter-guided")).toBeNull());
  });

  it("表计名称为空 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("energy-meter-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("energy-meter-create-open"));
    // name 默认空，直接下一步
    fireEvent.click(screen.getByTestId("energy-meter-guided-next"));
    await waitFor(() => expect(screen.getByTestId("energy-meter-guided-error").textContent).toContain("名称"));
    expect(screen.queryByTestId("energy-meter-save")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/energy/meters")).toBe(false);
  });
});
