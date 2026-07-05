import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OrganizationMerchantPage } from "./OrganizationMerchantPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// mock 按后端真实 snake_case 响应结构造数据（platform 域 Jackson 全局 SNAKE_CASE，字段以实测响应为准），
// 并走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。字段再错位（读臆造字段名）本文件必红。
const ENTITIES = [
  { id: "org-1", entity_name: "平台经营主体", entity_role: "operation", unified_social_credit_code: "91330100MA27XW8H", legal_representative: "张三", bank_account_ref: "vault://demo", status: "active", created_at: "2026-07-01T00:00:00Z" },
];
const MERCHANTS = [
  { id: "m-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "fixed_rent", status: "active" },
  { id: "m-2", merchant_name: "停用商户乙", contact_name: "联系人乙", status: "suspended" },
];
const PROJECTS = [
  { id: "p-1", name: "项目一", code: "PJ-1", merchant_id: "m-1", venue_type: "bar", floor: "1F", capacity: 80, status: "active" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("business-entities")) return Promise.resolve(envelope(ENTITIES));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("projects")) return Promise.resolve(envelope(PROJECTS));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const fullProps: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<OrganizationMerchantPage {...fullProps} />);
}

// 聚焦视图断言：只剩商户表（active 行），其余表/表单/KPI 卡全隐藏，横幅在场。
function expectActiveMerchantsFocusView() {
  expect(screen.getByTestId("organization-kpi-focus-banner").textContent).toContain("正在查看：正常商户（1）");
  expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy();
  expect(screen.queryByTestId("organization-merchant-row-m-2")).toBeNull();
  expect(screen.queryByTestId("organization-entities-table")).toBeNull();
  expect(screen.queryByTestId("organization-projects-table")).toBeNull();
  expect(screen.queryByTestId("organization-merchant-summary")).toBeNull();
  expect(screen.queryByTestId("organization-merchant-name")).toBeNull();
}

describe("OrganizationMerchantPage KPI 聚焦视图（带参重进本页）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    // pushState 会跨用例残留 URL，逐例复位。
    window.history.replaceState({}, "", "/admin/governance/organizations");
  });

  it("点“正常商户”数字 → URL 带 ?kpi=merchants&status=active，页面进入聚焦视图", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-active-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-kpi-active-merchants"));
    expect(window.location.search).toContain("kpi=merchants");
    expect(window.location.search).toContain("status=active");
    expectActiveMerchantsFocusView();
  });

  it("聚焦视图点「返回全部」→ 参数清除，恢复完整页面（三表+表单+KPI 卡都在）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-active-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-kpi-active-merchants"));
    fireEvent.click(screen.getByTestId("organization-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("organization-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("organization-entities-table")).toBeTruthy();
    expect(screen.getByTestId("organization-projects-table")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-row-m-2")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-summary")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-name")).toBeTruthy();
  });

  it("带参刷新/直链进入 → 直接呈现聚焦视图（参数从 URL 重建）", async () => {
    window.history.replaceState({}, "", "/admin/governance/organizations?kpi=merchants&status=active");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-focus-banner")).toBeTruthy());
    expectActiveMerchantsFocusView();
  });

  it("浏览器后退（popstate）→ 退出聚焦视图回完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-active-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-kpi-active-merchants"));
    expect(screen.getByTestId("organization-kpi-focus-banner")).toBeTruthy();
    // jsdom 的 history.back 异步且不稳，直接模拟浏览器后退的效果：URL 回退+popstate 事件。
    window.history.replaceState({}, "", "/admin/governance/organizations");
    fireEvent.popState(window);
    await waitFor(() => expect(screen.queryByTestId("organization-kpi-focus-banner")).toBeNull());
    expect(screen.getByTestId("organization-merchant-summary")).toBeTruthy();
  });

  it("三表按后端真实字段渲染出真实值（字段错位时单元格是“-”，本用例必红）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-entities-table")).toBeTruthy());
    // 经营主体表：主体名/统一社会信用代码/角色中文。
    const entitiesText = screen.getByTestId("organization-entities-table").textContent ?? "";
    expect(entitiesText).toContain("平台经营主体");
    expect(entitiesText).toContain("91330100MA27XW8H");
    expect(entitiesText).toContain("运营主体");
    // 商户表：商户名/联系人/电话/区块/面积（租赁面积优先）/合作形式中文。
    const m1 = screen.getByTestId("organization-merchant-row-m-1").textContent ?? "";
    expect(m1).toContain("正常商户甲");
    expect(m1).toContain("联系人甲");
    expect(m1).toContain("13800000001");
    expect(m1).toContain("1F-A");
    expect(m1).toContain("120");
    expect(m1).toContain("固定租金");
    // 项目表：项目名/编码/商户下钻（merchant_id）/业态。
    const projectsText = screen.getByTestId("organization-projects-table").textContent ?? "";
    expect(projectsText).toContain("项目一");
    expect(projectsText).toContain("PJ-1");
    expect(projectsText).toContain("m-1");
    expect(projectsText).toContain("bar");
  });

  it("KPI 子标签渲染为中文，不含英文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-merchant-summary")).toBeTruthy());
    const text = screen.getByTestId("organization-merchant-summary").textContent ?? "";
    expect(text).not.toContain("merchant profiles");
    expect(text).not.toContain("projects");
    expect(text).not.toContain("active");
    expect(text).toContain("商户档案数");
    expect(text).toContain("项目数");
    expect(text).toContain("正常商户数");
  });
});
