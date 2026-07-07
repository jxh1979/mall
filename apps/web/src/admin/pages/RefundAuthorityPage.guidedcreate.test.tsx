import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { RefundAuthorityPage } from "./RefundAuthorityPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
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
  render(<RefundAuthorityPage {...props} />);
}

// 引导式流程 phase-6：新增退款规则从内联表单改为「＋新增退款规则」按钮 → 分步引导弹窗。
describe("RefundAuthorityPage 引导式新增退款规则", () => {
  it("入口→步进→提交携带退款域/规则类型/enabled", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("refund-rule-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("refund-rule-create-open"));
    fireEvent.change(screen.getByLabelText("规则退款域"), { target: { value: "bar" } });
    fireEvent.change(screen.getByLabelText("规则类型"), { target: { value: "expired_unused" } });
    fireEvent.click(screen.getByTestId("refund-rule-guided-next"));
    fireEvent.click(screen.getByTestId("refund-rule-add"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/finance/refund-authority/rules")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/finance/refund-authority/rules");
    expect(call?.[1]).toMatchObject({ refundDomain: "bar", ruleType: "expired_unused", enabled: true, status: "active" });
    await waitFor(() => expect(screen.queryByTestId("refund-rule-guided")).toBeNull());
  });

  it("缺规则类型 → 步内校验拦截，不提交", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("refund-rule-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("refund-rule-create-open"));
    // 不填规则类型（默认已清空）
    fireEvent.click(screen.getByTestId("refund-rule-guided-next"));
    await waitFor(() => expect(screen.getByTestId("refund-rule-guided-error").textContent).toContain("规则类型"));
    expect(screen.queryByTestId("refund-rule-add")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/finance/refund-authority/rules")).toBe(false);
  });
});
