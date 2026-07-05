import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SupervisorReviewPage } from "./SupervisorReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const POLICIES = [
  { id: "sp-1", policy_code: "POL-1", action_type: "contract_violation", required_level: 3, blocks_by_default: true, required_evidence_types: ["contract"], status: "active" },
  { id: "sp-2", policy_code: "POL-2", action_type: "billing_discount", required_level: 2, blocks_by_default: false, required_evidence_types: [], status: "disabled" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes("supervisor-review-policies")) return Promise.resolve(envelope(POLICIES));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<SupervisorReviewPage {...props} />);
}

describe("SupervisorReviewPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/supervisor-review");
  });

  it("点“默认阻断” → URL 带 ?kpi=policies&subset=blocked，聚焦只剩默认阻断策略", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-kpi-blocked")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-kpi-blocked"));
    expect(window.location.search).toContain("kpi=policies");
    expect(window.location.search).toContain("subset=blocked");
    expect(screen.getByTestId("supervisor-review-kpi-focus-banner").textContent).toContain("正在查看：默认阻断（1）");
    const table = screen.getByTestId("supervisor-review-table").textContent ?? "";
    expect(table).toContain("POL-1");
    expect(table).not.toContain("POL-2");
    expect(screen.queryByTestId("supervisor-review-summary")).toBeNull();
    expect(screen.queryByTestId("supervisor-review-form")).toBeNull();
  });

  it("聚焦后点「返回全部」→ 恢复 KPI 卡+评估表单+全量策略", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-kpi-total")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-kpi-total"));
    fireEvent.click(screen.getByTestId("supervisor-review-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("supervisor-review-summary")).toBeTruthy();
    expect(screen.getByTestId("supervisor-review-form")).toBeTruthy();
    expect(screen.getByTestId("supervisor-review-table").textContent).toContain("POL-2");
  });

  it("KPI 子标签与动作类型列为中文，不含 blocksByDefault/英文枚举", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-summary")).toBeTruthy());
    const summary = screen.getByTestId("supervisor-review-summary").textContent ?? "";
    expect(summary).not.toContain("blocksByDefault");
    const table = screen.getByTestId("supervisor-review-table").textContent ?? "";
    expect(table).toContain("合同违规");
    expect(table).not.toContain("contract_violation");
  });
});

describe("SupervisorReviewPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/supervisor-review");
  });

  it("点策略行 → 抽屉含该行动作类型（中文）与默认阻断（是/否），无英文枚举，Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("supervisor-review-row-sp-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("supervisor-review-row-sp-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("合同违规");
    expect(drawer.textContent).toContain("是");
    expect(drawer.textContent).not.toContain("contract_violation");
    expect(drawer.textContent).not.toContain("blocksByDefault");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });
});
