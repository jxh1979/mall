import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { GovernanceTopologyPage } from "./GovernanceTopologyPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// mock 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。platform 域 snake_case 响应。
const ENTITIES = [{ id: "org-1", entity_name: "平台经营主体", unified_social_credit_code: "ORG-1", entity_role: "operation", status: "active" }];
const MERCHANTS = [
  { id: "m-1", business_entity_id: "org-1", merchant_name: "商户甲", status: "active" },
  { id: "m-2", business_entity_id: "org-1", merchant_name: "商户乙", status: "suspended" },
];
const PROJECTS = [{ id: "p-1", name: "项目一", code: "PJ-1", merchant_id: "m-1", venue_type: "bar", status: "active" }];
const SPACES = [{ id: "s-1", space_name: "空间甲", project_id: "p-1", floor_no: "L1", area_square_meter: 120, space_type: "hall", status: "active" }];

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
      if (url.includes("spaces")) return Promise.resolve(envelope(SPACES));
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
  render(<GovernanceTopologyPage {...fullProps} />);
}

describe("GovernanceTopologyPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/topology");
  });

  it("点“空间点位”数字 → URL 带 ?kpi=spaces，只剩空间表+横幅", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-spaces")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-topology-kpi-spaces"));
    expect(window.location.search).toContain("kpi=spaces");
    expect(screen.getByTestId("governance-topology-kpi-focus-banner").textContent).toContain("正在查看：空间点位（1）");
    expect(screen.getByTestId("governance-topology-spaces-table")).toBeTruthy();
    expect(screen.queryByTestId("governance-topology-table")).toBeNull();
    expect(screen.queryByTestId("governance-topology-summary")).toBeNull();
  });

  it("点“商户”数字 → 只剩主体-商户-项目表+横幅", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-merchants")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-topology-kpi-merchants"));
    expect(window.location.search).toContain("kpi=merchants");
    expect(screen.getByTestId("governance-topology-table")).toBeTruthy();
    expect(screen.queryByTestId("governance-topology-spaces-table")).toBeNull();
  });

  it("「返回全部」→ 参数清除，恢复完整页面（两表+KPI 卡都在）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-spaces")).toBeTruthy());
    fireEvent.click(screen.getByTestId("governance-topology-kpi-spaces"));
    fireEvent.click(screen.getByTestId("governance-topology-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("governance-topology-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("governance-topology-table")).toBeTruthy();
    expect(screen.getByTestId("governance-topology-spaces-table")).toBeTruthy();
    expect(screen.getByTestId("governance-topology-summary")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/governance/topology?kpi=spaces");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("governance-topology-spaces-table")).toBeTruthy();
    expect(screen.queryByTestId("governance-topology-table")).toBeNull();
  });

  it("渲染输出状态列为中文，不含原英文枚举 active/suspended", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("governance-topology-table")).toBeTruthy());
    const text = screen.getByTestId("governance-topology-table").textContent ?? "";
    expect(text).toContain("有效");
    expect(text).toContain("已封禁");
    expect(text).not.toContain("suspended");
  });
});
