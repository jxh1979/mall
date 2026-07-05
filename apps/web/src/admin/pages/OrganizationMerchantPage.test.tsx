import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { OrganizationMerchantPage } from "./OrganizationMerchantPage";
import type { AdminApiClient, AdminPageProps, BusinessEntityRow, EffectivePermissionsState, GovernanceProjectRow, MerchantAdmissionRow } from "../types";

const ENTITIES: BusinessEntityRow[] = [{ id: "org-1", name: "平台经营主体", code: "ORG-1", orgType: "platform", status: "active" }];
const MERCHANTS: MerchantAdmissionRow[] = [
  { id: "m-1", displayName: "正常商户甲", companyName: "甲公司", status: "active" },
  { id: "m-2", displayName: "停用商户乙", companyName: "乙公司", status: "suspended" },
];
const PROJECTS: GovernanceProjectRow[] = [{ id: "p-1", name: "项目一", code: "PJ-1", status: "active" }];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes("business-entities")) return Promise.resolve(ENTITIES);
      if (url.includes("merchants")) return Promise.resolve(MERCHANTS);
      if (url.includes("projects")) return Promise.resolve(PROJECTS);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
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

describe("OrganizationMerchantPage KPI 可点击下钻", () => {
  let scrollSpy: ReturnType<typeof vi.fn>;
  beforeEach(() => {
    // jsdom 不实现 scrollIntoView，直接挂桩并断言被调用。
    scrollSpy = vi.fn();
    // @ts-expect-error jsdom 缺省无此方法，测试内补桩
    HTMLElement.prototype.scrollIntoView = scrollSpy;
  });

  it("点“商户档案”数字 → 定位商户表并高亮该区块", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-kpi-merchants"));
    expect(scrollSpy).toHaveBeenCalled();
    expect(screen.getByTestId("organization-merchants-section").className).toContain("admin-section-highlight");
    // 全量计数不筛选：两行商户都在。
    expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-row-m-2")).toBeTruthy();
  });

  it("点“正常商户”数字 → 商户表按 active 筛选（仅正常商户可见）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("organization-kpi-active-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("organization-kpi-active-merchants"));
    expect(screen.getByTestId("organization-merchants-filter-hint")).toBeTruthy();
    expect(screen.getByTestId("organization-merchant-row-m-1")).toBeTruthy();
    expect(screen.queryByTestId("organization-merchant-row-m-2")).toBeNull();
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
