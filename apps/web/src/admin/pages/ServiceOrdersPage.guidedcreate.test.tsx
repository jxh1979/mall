import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ServiceOrdersPage } from "./ServiceOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post = vi.fn().mockResolvedValue({ id: "cat-new" })): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => (url.includes("/catalog") ? [] : [])),
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
  render(<ServiceOrdersPage {...props} />);
}

// 引导式流程 phase-5：新增服务目录从内联表单改为「＋新增服务目录」按钮 → 分步引导弹窗。
describe("ServiceOrdersPage 引导式新增服务目录", () => {
  it("入口→步进→提交携带服务目录字段", async () => {
    const post = vi.fn().mockResolvedValue({ id: "cat-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("svc-catalog-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("svc-catalog-create-open"));
    // 步骤1 服务信息（无默认值，需填服务名 + 参考价）
    fireEvent.change(screen.getByTestId("svc-name-input"), { target: { value: "点舞" } });
    fireEvent.change(screen.getByTestId("svc-price-input"), { target: { value: "200" } });
    fireEvent.change(screen.getByTestId("svc-minutes-input"), { target: { value: "10" } });
    fireEvent.click(screen.getByTestId("svc-catalog-guided-next"));
    // 步骤2 管理费（默认免收）
    fireEvent.click(screen.getByTestId("svc-catalog-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("svc-save"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/services/catalog")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/services/catalog");
    expect(call?.[1]).toMatchObject({ name: "点舞", priceCent: 20000, durationMinutes: 10, mgmtFeeMode: "none" });
    // 成功后弹窗关闭
    await waitFor(() => expect(screen.queryByTestId("svc-catalog-guided")).toBeNull());
  });

  it("服务信息缺服务名 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("svc-catalog-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("svc-catalog-create-open"));
    // 只填价格，不填服务名
    fireEvent.change(screen.getByTestId("svc-price-input"), { target: { value: "200" } });
    fireEvent.click(screen.getByTestId("svc-catalog-guided-next"));
    await waitFor(() => expect(screen.getByTestId("svc-catalog-guided-error").textContent).toContain("服务名"));
    expect(screen.queryByTestId("svc-save")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/services/catalog")).toBe(false);
  });
});
