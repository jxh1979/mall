import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SubjectBindingsPage } from "./SubjectBindingsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 2 绑定：b-1 生效、b-2 终止 → 唯一绑定对象2、生效绑定1。
const BINDINGS = [
  { id: "b-1", business_object_id: "m-1", operator_subject: "运营甲", contract_subject: "合同甲", settlement_subject: "结算甲", status: "active" },
  { id: "b-2", business_object_id: "m-2", operator_subject: "运营乙", contract_subject: "合同乙", settlement_subject: "结算乙", status: "terminated" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation(() => Promise.resolve(envelope(BINDINGS))));
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
  render(<SubjectBindingsPage {...fullProps} />);
}

describe("SubjectBindingsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/subject-bindings");
  });

  it("点“生效绑定”数字 → ?kpi=bindings&status=active，横幅(1)+表只剩生效子集，KPI 卡/变更表单隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-kpi-active")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-kpi-active"));
    expect(window.location.search).toContain("kpi=bindings");
    expect(window.location.search).toContain("status=active");
    expect(screen.getByTestId("subject-bindings-kpi-focus-banner").textContent).toContain("正在查看：生效绑定（1）");
    expect(screen.queryByTestId("subject-bindings-summary")).toBeNull();
    expect(screen.queryByTestId("subject-bindings-submit-change")).toBeNull();
    const text = screen.getByTestId("subject-bindings-table").textContent ?? "";
    expect(text).toContain("m-1");
    expect(text).not.toContain("m-2");
  });

  it("点“唯一绑定对象”数字 → 全量表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-kpi-unique")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-kpi-unique"));
    expect(window.location.search).toContain("kpi=bindings");
    expect(window.location.search).not.toContain("status=");
    const text = screen.getByTestId("subject-bindings-table").textContent ?? "";
    expect(text).toContain("m-1");
    expect(text).toContain("m-2");
  });

  it("「返回全部」→ 恢复 KPI 卡+变更表单+全量表", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-kpi-active")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-kpi-active"));
    fireEvent.click(screen.getByTestId("subject-bindings-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("subject-bindings-summary")).toBeTruthy();
    expect(screen.getByTestId("subject-bindings-submit-change")).toBeTruthy();
    expect(screen.getByTestId("subject-bindings-table").textContent).toContain("m-2");
  });

  it("带参直链进入 → 直接呈现聚焦视图", async () => {
    window.history.replaceState({}, "", "/admin/governance/subject-bindings?kpi=bindings&status=active");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("subject-bindings-summary")).toBeNull();
    expect(screen.getByTestId("subject-bindings-table").textContent).not.toContain("m-2");
  });

  it("KPI 子标签与状态列为中文，不含英文 active", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-summary")).toBeTruthy());
    expect(screen.getByTestId("subject-bindings-summary").textContent).not.toContain("active");
    expect(screen.getByTestId("subject-bindings-table").textContent).toContain("有效");
  });
});

describe("SubjectBindingsPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/governance/subject-bindings");
  });

  it("点绑定行 → 抽屉含该行字段（状态中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-row-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-row-b-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("运营甲");
    expect(drawer.textContent).toContain("有效");
    expect(drawer.textContent).not.toContain("active");
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });

  it("点行内单选按钮不触发抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-select-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-select-b-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
