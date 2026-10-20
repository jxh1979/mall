// 文件版本：V0.01
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

  // TK-23-R1（ENUM「固定金额必须在 0 至服务售价（含边界）之间；任一越界值在保存时直接拒绝」）：固定 101 元、参考价 100 元 → 步内校验拦截，不发请求。
  it("固定管理费 101 元 > 参考价 100 元 → 显示校验错误、不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("svc-catalog-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("svc-catalog-create-open"));
    fireEvent.change(screen.getByTestId("svc-name-input"), { target: { value: "点舞" } });
    fireEvent.change(screen.getByTestId("svc-price-input"), { target: { value: "100" } });
    fireEvent.change(screen.getByTestId("svc-fee-mode"), { target: { value: "fixed" } });
    fireEvent.change(screen.getByTestId("svc-fee-value"), { target: { value: "101" } });
    fireEvent.click(screen.getByTestId("svc-catalog-guided-next"));
    await waitFor(() => expect(screen.getByTestId("svc-catalog-guided-error").textContent).toContain("固定管理费"));
    expect(screen.queryByTestId("svc-save")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/services/catalog")).toBe(false);
  });

  // TK-23-R3（ENUM「变更版本化与指定生效日……比照销售提成」）：已上架项的管理费只出现「提交管理费草案」，没有直接保存入口；缺生效日不提交。
  it("已上架项编辑管理费只出现「提交草案」，不出现直接保存；缺生效日不发请求", async () => {
    const post = vi.fn().mockResolvedValue({});
    const catalog = [{ id: "c-1", name: "点舞", priceCent: 20000, durationMinutes: 10, active: true, mgmtFeeMode: "percent", mgmtFeeValue: 1000, feeVersion: 1 }];
    const api = {
      ...makeApi(post),
      get: vi.fn(async (url: string) => (url.includes("/catalog") ? catalog : [])),
    } as unknown as AdminApiClient;
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<ServiceOrdersPage {...props} />);
    // 上架项有「提交管理费草案」+「存档」两个动作 → RowActions 下拉，先点触发器再取菜单项。
    await waitFor(() => expect(screen.getByTestId("svc-catalog-actions-c-1")).toBeTruthy());
    expect(screen.getByTestId("svc-catalog-fee-c-1").textContent).toContain("比例 10%");
    fireEvent.click(screen.getByTestId("svc-catalog-actions-c-1"));
    await waitFor(() => expect(screen.getByTestId("svc-fee-draft-c-1")).toBeTruthy());
    const menu = screen.getByTestId("svc-fee-draft-c-1").closest("ul")?.textContent ?? "";
    expect(menu).toContain("提交管理费草案");
    expect(menu).not.toContain("保存");
    fireEvent.click(screen.getByTestId("svc-fee-draft-c-1"));
    fireEvent.change(screen.getByTestId("svc-fee-draft-mode"), { target: { value: "percent" } });
    fireEvent.change(screen.getByTestId("svc-fee-draft-value"), { target: { value: "20" } });
    fireEvent.click(screen.getByTestId("svc-fee-draft-submit"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("请选择生效日"));
    expect(post.mock.calls.some((c) => String(c[0]).includes("/fee-draft"))).toBe(false);
  });
});
