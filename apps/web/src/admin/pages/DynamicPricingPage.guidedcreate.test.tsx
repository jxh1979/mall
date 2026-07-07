import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { DynamicPricingPage } from "./DynamicPricingPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, TicketDynamicPricingRuleRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// form 在 AdminDataState empty 内，需 ≥1 条规则才渲染新建入口。
const RULES: TicketDynamicPricingRuleRow[] = [
  { id: "r-1", ruleName: "现有规则", salesChannel: "web", priority: 10, adjustmentType: "percent_delta", adjustmentRate: 10, status: "draft", effectiveFrom: "2026-06-01", effectiveTo: "2026-06-30", ticketTypeId: "t-1" },
];

function makeApi(post = vi.fn().mockResolvedValue({ id: "rule-new" })): AdminApiClient {
  return {
    get: vi.fn(async () => RULES),
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
  render(<DynamicPricingPage {...props} />);
}

// 引导式流程 phase-3：新建定价规则从内联表单改为「＋新建定价规则」按钮 → 分步引导弹窗。
describe("DynamicPricingPage 引导式新建定价规则", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing");
  });

  it("入口→步进→提交调用建规则接口", async () => {
    const post = vi.fn().mockResolvedValue({ id: "rule-new" });
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dynamic-pricing-create-open"));
    fireEvent.change(screen.getByTestId("dynamic-pricing-rule-name"), { target: { value: "低库存加价" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-adjustment-rate"), { target: { value: "15" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(true));
    const call = post.mock.calls.find((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules");
    expect(call?.[1]).toMatchObject({ ruleName: "低库存加价", adjustmentRate: 15 });
  });

  it("规则名为空 → 步内校验拦截，不建规则", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dynamic-pricing-create-open"));
    // 规则名默认非空,显式清空以触发校验
    fireEvent.change(screen.getByTestId("dynamic-pricing-rule-name"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-guided-error")).toBeTruthy());
    expect(screen.queryByTestId("dynamic-pricing-create")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(false);
  });
});
