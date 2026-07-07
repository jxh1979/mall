import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { LeasingWorkbenchPage } from "./LeasingWorkbenchPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// AdminDataState empty 需三表任一 ≥1 行才渲染非聚焦创建入口（否则空态遮住按钮）。
const BRAND = { id: "br-1", brandName: "示例品牌", brandType: "chain", businessType: "retail" };

function makeApi(post = vi.fn().mockResolvedValue({ id: "br-new" })): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/brands") ? [BRAND] : [])),
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
  render(<LeasingWorkbenchPage {...props} />);
}

// 引导式流程 phase-5：新增品牌从内联表单改为「＋录入品牌」按钮 → 分步引导弹窗（线索/意向/拉黑不动）。
describe("LeasingWorkbenchPage 引导式录入品牌", () => {
  it("入口→步进→提交携带品牌字段", async () => {
    const post = vi.fn().mockResolvedValue({ id: "br-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("leasing-brand-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-brand-create-open"));
    // 步骤1 品牌信息（brandName 有默认值“示例品牌”，改成自定义名）
    fireEvent.change(screen.getByTestId("leasing-brand-name"), { target: { value: "星海连锁" } });
    fireEvent.click(screen.getByTestId("leasing-brand-flow-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("leasing-brand-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/leasing/brands")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/leasing/brands");
    expect(call?.[1]).toMatchObject({ brandName: "星海连锁", brandType: "chain", businessType: "retail" });
    // 成功后弹窗关闭
    await waitFor(() => expect(screen.queryByTestId("leasing-brand-flow")).toBeNull());
  });

  it("品牌信息缺品牌名称 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("leasing-brand-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("leasing-brand-create-open"));
    // 清空品牌名称
    fireEvent.change(screen.getByTestId("leasing-brand-name"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("leasing-brand-flow-next"));
    await waitFor(() => expect(screen.getByTestId("leasing-brand-flow-error").textContent).toContain("品牌名称"));
    expect(screen.queryByTestId("leasing-brand-create")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/leasing/brands")).toBe(false);
  });
});
