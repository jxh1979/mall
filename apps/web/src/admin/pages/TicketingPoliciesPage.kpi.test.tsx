import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const TICKETS = [{ id: "t-1", ticketName: "通票甲", weekdayPriceCent: 9900, weekendPriceCent: 12900, holidayPriceCent: 15900, status: "active", stockQuantity: null }];
const TEMPLATES = [{ id: "tpl-1", name: "入园1次", entitlementType: "entry", status: "active" }];
const RULES = [{ id: "pr-1", ticketTypeId: "t-1", variantType: "student", priceMode: "derive_from_calendar_price", discountRate: 0.8, weekdayPriceCent: null, status: "active" }];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => {
      if (url.includes("ticket-types")) return TICKETS;
      if (url.includes("entitlement-templates")) return TEMPLATES;
      if (url.includes("price-rules")) return RULES;
      return [];
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(extra: Partial<AdminPageProps> = {}) {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    ...extra,
  };
  render(<TicketingPoliciesPage {...props} />);
}

describe("TicketingPoliciesPage 英文全清 + ticket_type 域内引用可点", () => {
  it("权益模板表的权益类型渲染为中文（entry→入园），不含英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("entitlement-templates-table")).toBeTruthy());
    const table = screen.getByTestId("entitlement-templates-table").textContent ?? "";
    expect(table).toContain("入园");
    expect(table).not.toContain("entry");
  });

  it("派生价规则行的票种引用是可点 TraceLink，跳 ADM-TKT-001 并带票种 id，不弹抽屉（stopPropagation）", async () => {
    const navigateTo = vi.fn();
    renderPage({ navigateTo });
    const link = await screen.findByTestId("trace-link-ADM-TKT-001-t-1");
    fireEvent.click(link);
    expect(navigateTo).toHaveBeenCalledWith("ADM-TKT-001", "t-1");
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

describe("TicketingPoliciesPage 行明细抽屉（三业务表整行可点·零英文）", () => {
  it("点票种行 -> 抽屉含三档价+限量+中文状态", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-type-t-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-type-t-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("通票甲");
    expect(text).toContain("¥99.00");
    expect(text).toContain("不限");
    expect(text).toContain("有效");
    expect(text).not.toContain("active");
  });

  it("点权益模板行 -> 抽屉权益类型为中文（入园），无英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("entitlement-template-tpl-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("entitlement-template-tpl-1"));
    const text = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(text).toContain("入园1次");
    expect(text).toContain("入园");
    expect(text).not.toContain("entry");
  });

  it("点派生价规则行 -> 抽屉含中文人群+折扣+票种下钻 TraceLink", async () => {
    renderPage({ navigateTo: vi.fn() });
    await waitFor(() => expect(screen.getByTestId("price-rule-pr-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("price-rule-pr-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("学生票");
    expect(text).toContain("80%");
    expect(text).toContain("按基础价折扣");
    expect(drawer.querySelector('[data-testid="trace-link-ADM-TKT-001-t-1"]')).toBeTruthy();
  });

  it("点票种行 -> Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-type-t-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-type-t-1"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
