// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MerchantZoneSettlementsPage } from "./MerchantZoneSettlementsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 夹具按后端 ContractSettlementRecord 真实形状（含 settlement_items 与 V307 的 voided）。
const SETTLEMENTS = [
  {
    id: "st-1", contract_id: "c-1", trigger_type: "move_out", status: "pending_settlement", dispute_reason: null,
    settlement_items: [
      { item: "unpaid_receivable_summary", amount_cent: 30000, status: "listed", note: "未清应收合计（已在应收台账，不重复挂账）" },
      { item: "deposit_disposal", amount_cent: 100000, direction: "payable", status: "payable_posted", note: "押金退还" },
      { item: "mystery_item", amount_cent: 1, note: null },
    ],
  },
  { id: "st-2", contract_id: "c-2", trigger_type: "expiry", status: "pending_settlement", dispute_reason: "金额不符", settlement_items: [] },
  { id: "st-3", contract_id: "c-3", trigger_type: "expiry", status: "settled", dispute_reason: null, settlement_items: [] },
  { id: "st-4", contract_id: "c-4", trigger_type: "move_out", status: "voided", dispute_reason: null, settlement_items: [] },
  { id: "st-5", contract_id: "c-5", trigger_type: "weird_trigger", status: "weird_status", dispute_reason: null, settlement_items: [] },
];

function envelope(items: unknown[]): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data: { items, page: 1, page_size: 50, total: items.length } }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

function single(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

type Call = { url: string; method: string; body: Record<string, unknown> | null };

function makeApi(calls: Call[] = []): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) as Record<string, unknown> : null });
    if (method === "POST" && url.includes("/dispute")) return Promise.resolve(single({ id: "st-1", status: "pending_settlement" }));
    if (url.includes("/settlements")) return Promise.resolve(envelope(SETTLEMENTS));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["merchant.portal.settlement.view"], denies: [], wildcard: false } };

function renderPage(overrides: Partial<AdminPageProps> = {}, calls: Call[] = []) {
  const props: AdminPageProps = {
    api: makeApi(calls),
    user: { id: "u1", username: "merchant", accountType: "merchant_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
    navigateTo: vi.fn(),
    ...overrides,
  };
  render(<MerchantZoneSettlementsPage {...props} />);
  return props;
}

describe("MerchantZoneSettlementsPage 留言提异议（M023-006）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
  });

  it("留言表单可见，文案写明不改状态、不暂停付款、正式异议走退租审批；没有「有异议」KPI", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-summary")).toBeTruthy());
    expect(screen.getByTestId("merchant-zone-settlement-dispute-form")).toBeTruthy();
    const hint = screen.getByTestId("merchant-zone-settlement-dispute-hint").textContent ?? "";
    expect(hint).toContain("不改变结算状态");
    expect(hint).toContain("不暂停付款");
    expect(hint).toContain("退租审批流程");
    expect(screen.queryByTestId("merchant-zone-settlements-kpi-disputed")).toBeNull();
    expect(screen.getByTestId("merchant-zone-settlements-page").textContent).not.toContain("待平台裁决");
  });

  it("选中待结算单填写理由 → POST 到该结算单的 dispute 端点，带 reason 与 idempotency_key", async () => {
    const calls: Call[] = [];
    const props = renderPage({}, calls);
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-pick-st-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-2"));
    fireEvent.change(screen.getByTestId("merchant-zone-settlement-dispute-reason"), { target: { value: "  违约金多算一期  " } });
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-dispute-submit"));
    await waitFor(() => expect(calls.some((call) => call.method === "POST")).toBe(true));
    const post = calls.find((call) => call.method === "POST")!;
    expect(post.url).toContain("/api/v1/merchant-portal/settlements/st-2/dispute");
    expect(post.body?.reason).toBe("违约金多算一期");
    expect(String(post.body?.idempotency_key ?? "")).not.toBe("");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalled());
  });

  it("理由为空或超过 500 字 → 不发请求并给中文提示", async () => {
    const calls: Call[] = [];
    renderPage({}, calls);
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-dispute-submit")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-dispute-submit"));
    expect(screen.getByTestId("merchant-zone-settlement-form-error").textContent).toContain("必须填写理由");
    fireEvent.change(screen.getByTestId("merchant-zone-settlement-dispute-reason"), { target: { value: "多".repeat(501) } });
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-dispute-submit"));
    expect(screen.getByTestId("merchant-zone-settlement-form-error").textContent).toContain("500");
    expect(calls.some((call) => call.method === "POST")).toBe(false);
  });

  it("已结清 / 已作废的结算单不可留言（按钮禁用）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-pick-st-3")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-3"));
    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-4"));
    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-1"));
    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(false);
  });

  it("没有 merchant.portal.settlement.dispute 权限 → 提交按钮禁用且不发请求", async () => {
    const calls: Call[] = [];
    renderPage({ effectivePermissions: VIEW_ONLY_PERMS }, calls);
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-dispute-submit")).toBeTruthy());
    fireEvent.change(screen.getByTestId("merchant-zone-settlement-dispute-reason"), { target: { value: "少算" } });
    const submit = screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement;
    expect(submit.disabled).toBe(true);
    fireEvent.click(submit);
    expect(calls.some((call) => call.method === "POST")).toBe(false);
  });
});

describe("MerchantZoneSettlementsPage KPI 聚焦视图", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
  });

  it("「未结清」不含已结清与已作废；点开聚焦排除这两类，点「返回全部」恢复完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-kpi-open")).toBeTruthy());
    expect(screen.getByTestId("merchant-zone-settlements-kpi-open").textContent).toBe("3");
    fireEvent.click(screen.getByTestId("merchant-zone-settlements-kpi-open"));
    expect(screen.getByTestId("merchant-zone-settlements-kpi-focus-banner").textContent).toContain("正在查看：未结清（3）");
    expect(screen.queryByTestId("merchant-zone-settlement-row-st-3")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-settlement-row-st-4")).toBeNull();
    fireEvent.click(screen.getByTestId("merchant-zone-settlements-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.getByTestId("merchant-zone-settlements-summary")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-settlement-row-st-3")).toBeTruthy();
    expect(screen.getByTestId("merchant-zone-settlement-row-st-4")).toBeTruthy();
  });

  it("触发/状态列为中文：已作废有中文，未知码给中文占位，不含英文枚举码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlements-table")).toBeTruthy());
    const table = screen.getByTestId("merchant-zone-settlements-table").textContent ?? "";
    expect(table).toContain("退租");
    expect(table).toContain("待结算");
    expect(table).toContain("已结清");
    expect(table).toContain("已作废");
    expect(table).toContain("未知状态");
    expect(table).toContain("未知触发");
    expect(table).not.toContain("voided");
    expect(table).not.toContain("weird_status");
    expect(table).not.toContain("weird_trigger");
    expect(table).not.toContain("move_out");
    expect(table).not.toContain("pending_settlement");
  });
});

describe("MerchantZoneSettlementsPage 行明细抽屉（整行可点·结算明细数字）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
  });

  it("点结算单行 -> 抽屉含结算明细条目中文名与分转元金额，合同下钻指向商户可达的「我的合同」", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-row-st-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-row-st-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const text = drawer.textContent ?? "";
    expect(text).toContain("st-1");
    expect(text).toContain("退租");
    expect(text).toContain("待结算");
    const items = screen.getByTestId("merchant-zone-settlement-items").textContent ?? "";
    expect(items).toContain("未清应收合计：300.00 元");
    expect(items).toContain("押金处置：1,000.00 元");
    expect(items).toContain("其他条目：0.01 元");
    expect(items).not.toContain("deposit_disposal");
    expect(items).not.toContain("mystery_item");
    expect(text).not.toContain("move_out");
    expect(text).not.toContain("pending_settlement");
    expect(drawer.querySelector('[data-testid="trace-link-ADM-MER-006-c-1"]')).toBeTruthy();
    expect(drawer.querySelector('[data-testid="trace-link-ADM-CONT-001-c-1"]')).toBeNull();
  });

  it("点结算单行 -> Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-row-st-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-row-st-2"));
    expect(screen.getByTestId("row-detail-drawer")).toBeTruthy();
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("合同下钻跳「我的合同」并带合同号、不弹抽屉", async () => {
    const props = renderPage();
    await waitFor(() => expect(screen.getAllByTestId("trace-link-ADM-MER-006-c-1").length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByTestId("trace-link-ADM-MER-006-c-1")[0]!);
    expect(props.navigateTo).toHaveBeenCalledWith("ADM-MER-006", "c-1");
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});
