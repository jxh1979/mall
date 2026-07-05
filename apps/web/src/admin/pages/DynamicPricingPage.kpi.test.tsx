import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { DynamicPricingPage } from "./DynamicPricingPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, TicketDynamicPricingRuleRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const RULES: TicketDynamicPricingRuleRow[] = [
  { id: "r-1", ruleName: "草稿规则", salesChannel: "web", priority: 10, adjustmentType: "percent_delta", adjustmentRate: 10, status: "draft", effectiveFrom: "2026-06-01", effectiveTo: "2026-06-30", ticketTypeId: "t-1" },
  { id: "r-2", ruleName: "已发布规则", salesChannel: "miniapp", priority: 5, adjustmentType: "absolute_delta", adjustmentRate: 200, status: "published", effectiveFrom: "2026-06-01", effectiveTo: "2026-06-30", ticketTypeId: "t-1" },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async () => RULES),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<DynamicPricingPage {...props} />);
}

describe("DynamicPricingPage KPI 聚焦视图", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing");
  });

  it("点“草稿”KPI → ?kpi=draft，表内只剩草稿规则，KPI 卡与表单隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-kpi-draft")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dynamic-pricing-kpi-draft"));
    expect(window.location.search).toContain("kpi=draft");
    expect(screen.getByTestId("dynamic-pricing-kpi-focus-banner").textContent).toContain("草稿（1）");
    const table = screen.getByTestId("dynamic-pricing-table").textContent ?? "";
    expect(table).toContain("草稿规则");
    expect(table).not.toContain("已发布规则");
    expect(screen.queryByTestId("dynamic-pricing-summary")).toBeNull();
    expect(screen.queryByTestId("dynamic-pricing-form")).toBeNull();
  });

  it("聚焦点「返回全部」→ 恢复完整页面（KPI 卡+两条规则）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-kpi-published")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dynamic-pricing-kpi-published"));
    fireEvent.click(screen.getByTestId("dynamic-pricing-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("dynamic-pricing-summary")).toBeTruthy();
    const table = screen.getByTestId("dynamic-pricing-table").textContent ?? "";
    expect(table).toContain("草稿规则");
    expect(table).toContain("已发布规则");
  });

  it("带参直链 ?kpi=published → 直接进聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing?kpi=published");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("dynamic-pricing-kpi-focus-banner").textContent).toContain("已上架（1）");
  });

  it("KPI 子标签与状态列为中文，不含 draft/published 英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-summary")).toBeTruthy());
    const summary = screen.getByTestId("dynamic-pricing-summary").textContent ?? "";
    expect(summary).not.toContain("draft");
    expect(summary).not.toContain("published");
    const table = screen.getByTestId("dynamic-pricing-table").textContent ?? "";
    expect(table).not.toContain("percent_delta");
    expect(table).not.toContain("absolute_delta");
    expect(table).toContain("百分比调整");
    expect(table).toContain("草稿");
    expect(table).toContain("已上架");
  });
});
