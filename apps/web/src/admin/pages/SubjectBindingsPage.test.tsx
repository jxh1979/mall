// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SubjectBindingsPage } from "./SubjectBindingsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// M012-001/005：夹具按后台 BusinessEntityBindingRecord 真实字段（platform 域 snake_case）造数，
// 走真实 AdminApi（stub fetch）→ unwrap 转 camelCase → 组件。字段再错位（读臆造键）本文件必红。
// 2 绑定：b-1 生效（active）、b-2 停用（inactive；绑定三态 active/inactive/suspended）→ 唯一绑定对象2、生效绑定1。
const BINDINGS = [
  { id: "b-1", merchant_id: "m-1", operation_entity_id: "be_op_1", contract_entity_id: "be_ct_1", settlement_entity_id: "be_st_1", status: "active", effective_from: "2026-06-14", effective_to: null, change_reason: "initial" },
  { id: "b-2", merchant_id: "m-2", operation_entity_id: "be_op_2", contract_entity_id: "be_ct_2", settlement_entity_id: "be_st_2", status: "inactive", effective_from: "2026-06-01", effective_to: "2026-06-30", change_reason: "contract changed" },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function envelopeOne(item: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: item }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
      if (init?.method === "POST") return Promise.resolve(envelopeOne({ ...BINDINGS[0], status: "inactive" }));
      return Promise.resolve(envelope(BINDINGS));
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
  render(<SubjectBindingsPage {...fullProps} />);
  return fullProps;
}

function postCalls() {
  const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
  return fetchMock.mock.calls.filter(([, init]) => (init as RequestInit | undefined)?.method === "POST");
}

describe("SubjectBindingsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
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
    expect(screen.getByTestId("subject-bindings-kpi-unique").textContent).toBe("2");
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

  it("KPI 子标签、表头与状态列为中文，不含英文 active/ID", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-summary")).toBeTruthy());
    expect(screen.getByTestId("subject-bindings-summary").textContent).not.toContain("active");
    const table = screen.getByTestId("subject-bindings-table");
    expect(table.textContent).toContain("有效");
    // M012-004：表头字段名全中文（「合同主体编号」而非「合同主体 ID」）。
    expect(table.querySelector("thead")?.textContent ?? "").not.toMatch(/\bID\b/);
    expect(table.querySelector("thead")?.textContent ?? "").toContain("合同主体编号");
  });
});

describe("SubjectBindingsPage 真实字段渲染（M012-001）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/governance/subject-bindings");
  });

  it("表格按后台真实字段渲染商户/三主体编号与变更原因（读臆造键时整行是“-”，本用例必红）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-row-b-1")).toBeTruthy());
    const row = screen.getByTestId("subject-bindings-row-b-1").textContent ?? "";
    expect(row).toContain("m-1");
    expect(row).toContain("be_op_1");
    expect(row).toContain("be_ct_1");
    expect(row).toContain("be_st_1");
    expect(row).toContain("initial");
  });
});

describe("SubjectBindingsPage 行明细抽屉", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/governance/subject-bindings");
  });

  it("点绑定行 → 抽屉含该行三主体编号（状态中文），Esc 关闭", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-row-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-row-b-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("be_op_1");
    expect(drawer.textContent).toContain("be_ct_1");
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

describe("SubjectBindingsPage 绑定变更提交（M012-002/006/008）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/governance/subject-bindings");
  });

  it("未选变更后状态 → 表单报错、不发请求（后台 action 为必填三态）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-select-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-select-b-1"));
    fireEvent.change(screen.getByTestId("subject-bindings-change-reason"), { target: { value: "合同到期停用" } });
    fireEvent.click(screen.getByTestId("subject-bindings-submit-change"));
    expect(screen.getByTestId("subject-bindings-form-error").textContent).toContain("请选择变更后的绑定状态");
    expect(postCalls()).toHaveLength(0);
  });

  it("原因为空 → 表单报错、不发请求（原因不再预填验收句）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-select-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-select-b-1"));
    fireEvent.change(screen.getByTestId("subject-bindings-change-action"), { target: { value: "inactive" } });
    expect((screen.getByTestId("subject-bindings-change-reason") as HTMLInputElement).value).toBe("");
    fireEvent.click(screen.getByTestId("subject-bindings-submit-change"));
    expect(screen.getByTestId("subject-bindings-form-error").textContent).toContain("变更原因不能为空");
    expect(postCalls()).toHaveLength(0);
  });

  it("选行+选状态+填原因 → 确认后 POST …/change，请求体含 action 与 reason、不含假证据附件", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderPage();
    await waitFor(() => expect(screen.getByTestId("subject-bindings-select-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("subject-bindings-select-b-1"));
    fireEvent.change(screen.getByTestId("subject-bindings-change-action"), { target: { value: "inactive" } });
    fireEvent.change(screen.getByTestId("subject-bindings-change-reason"), { target: { value: "合同到期停用" } });
    fireEvent.click(screen.getByTestId("subject-bindings-submit-change"));
    await waitFor(() => expect(postCalls()).toHaveLength(1));
    const [url, init] = postCalls()[0]!;
    expect(String(url)).toContain("/api/v1/platform/business-entity-bindings/b-1/change");
    const body = String((init as RequestInit).body);
    expect(body).toContain("inactive");
    expect(body).toContain("合同到期停用");
    expect(body).not.toContain("evidence-full-admin");
    expect(body).not.toContain("完整后台验收变更");
  });
});
