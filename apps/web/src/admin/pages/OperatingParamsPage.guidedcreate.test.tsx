import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { OperatingParamsPage } from "./OperatingParamsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("anomaly-config")) return { surgePct: 0.5, highPct: 0.8, enabled: true };
      return [];
    }),
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
  render(<OperatingParamsPage {...props} />);
}

// 引导式流程 phase-6：运营参数录入从内联表单改为「＋录入运营参数」按钮 → 分步引导弹窗。
describe("OperatingParamsPage 引导式录入运营参数", () => {
  it("入口→步进→提交携带综合体 id 与面积", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("operating-params-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("operating-params-create-open"));
    fireEvent.change(screen.getByLabelText("综合体 id"), { target: { value: "project_1" } });
    fireEvent.change(screen.getByLabelText("可经营面积（㎡）"), { target: { value: "8888" } });
    fireEvent.click(screen.getByTestId("operating-params-guided-next"));
    // 确认步 → 提交
    fireEvent.click(screen.getByTestId("operating-params-save"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/analytics/operating-params")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/analytics/operating-params");
    expect(call?.[1]).toMatchObject({ projectId: "project_1", operatingAreaSqm: 8888, status: "active" });
    await waitFor(() => expect(screen.queryByTestId("operating-params-guided")).toBeNull());
  });

  it("缺综合体 id → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("operating-params-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("operating-params-create-open"));
    // 只填面积，不填综合体 id
    fireEvent.change(screen.getByLabelText("可经营面积（㎡）"), { target: { value: "8888" } });
    fireEvent.click(screen.getByTestId("operating-params-guided-next"));
    await waitFor(() => expect(screen.getByTestId("operating-params-guided-error").textContent).toContain("综合体 id"));
    expect(screen.queryByTestId("operating-params-save")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/analytics/operating-params")).toBe(false);
  });
});
