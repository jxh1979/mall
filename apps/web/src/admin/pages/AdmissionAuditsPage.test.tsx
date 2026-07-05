import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdmissionAuditsPage } from "./AdmissionAuditsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const MERCHANTS = [{ id: "m-1", display_name: "商户甲", company_name: "甲公司", cooperation_form: "fixed_rent", risk_level: "low", status: "active" }];
const BINDINGS = [{ id: "b-1", business_object_id: "m-1", operator_subject: "运营甲", contract_subject: "合同甲", settlement_subject: "结算甲", status: "active" }];
const CONTRACTS = [{ id: "c-1", merchant_id: "m-1", contract_subject_id: "cs-1", operation_subject_id: "os-1", settlement_subject_id: "ss-1", lifecycle_status: "effective", status: "effective" }];

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
      if (url.includes("business-entity-bindings")) return Promise.resolve(envelope(BINDINGS));
      if (url.includes("merchants")) return Promise.resolve(envelope(MERCHANTS));
      if (url.includes("contracts")) return Promise.resolve(envelope(CONTRACTS));
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
  render(<AdmissionAuditsPage {...fullProps} />);
}

describe("AdmissionAuditsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/admission-audits");
  });

  it("点“合同审批”数字 → ?kpi=contracts，只剩合同表+横幅", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-kpi-contracts")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-kpi-contracts"));
    expect(window.location.search).toContain("kpi=contracts");
    expect(screen.getByTestId("admission-audits-kpi-focus-banner").textContent).toContain("正在查看：合同审批（1）");
    expect(screen.getByTestId("admission-audits-contracts-table")).toBeTruthy();
    expect(screen.queryByTestId("admission-audits-merchants-table")).toBeNull();
    expect(screen.queryByTestId("admission-audits-bindings-table")).toBeNull();
    expect(screen.queryByTestId("admission-audits-summary")).toBeNull();
  });

  it("点“主体绑定”数字 → 只剩绑定表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-kpi-bindings")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-kpi-bindings"));
    expect(screen.getByTestId("admission-audits-bindings-table")).toBeTruthy();
    expect(screen.queryByTestId("admission-audits-contracts-table")).toBeNull();
  });

  it("「返回全部」→ 恢复三表+KPI 卡", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-kpi-contracts")).toBeTruthy());
    fireEvent.click(screen.getByTestId("admission-audits-kpi-contracts"));
    fireEvent.click(screen.getByTestId("admission-audits-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("admission-audits-merchants-table")).toBeTruthy();
    expect(screen.getByTestId("admission-audits-bindings-table")).toBeTruthy();
    expect(screen.getByTestId("admission-audits-contracts-table")).toBeTruthy();
    expect(screen.getByTestId("admission-audits-summary")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/governance/admission-audits?kpi=merchants");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("admission-audits-merchants-table")).toBeTruthy();
    expect(screen.queryByTestId("admission-audits-contracts-table")).toBeNull();
  });

  it("合作形式/合同状态渲染为中文，不含英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("admission-audits-merchants-table")).toBeTruthy());
    expect(screen.getByTestId("admission-audits-merchants-table").textContent).toContain("固定租金");
    expect(screen.getByTestId("admission-audits-merchants-table").textContent).not.toContain("fixed_rent");
    expect(screen.getByTestId("admission-audits-contracts-table").textContent).toContain("生效中");
    expect(screen.getByTestId("admission-audits-contracts-table").textContent).not.toContain("effective");
  });
});
