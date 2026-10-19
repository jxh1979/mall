// 文件版本：V0.05
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

function makeApi(calls: Call[] = [], rows: unknown[] = SETTLEMENTS): AdminApi {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = (init?.method ?? "GET").toUpperCase();
    calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) as Record<string, unknown> : null });
    if (method === "POST" && url.includes("/dispute")) return Promise.resolve(single({ id: "st-1", status: "disputed" }));
    if (method === "POST" && url.includes("/reject-resolution")) return Promise.resolve(single({ id: "st-r1", status: "disputed_litigation" }));
    if (url.includes("/settlements")) return Promise.resolve(envelope(rows));
    return Promise.resolve(envelope([]));
  }));
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const VIEW_ONLY_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: ["merchant.portal.settlement.view"], denies: [], wildcard: false } };

function renderPage(overrides: Partial<AdminPageProps> = {}, calls: Call[] = [], rows: unknown[] = SETTLEMENTS) {
  const props: AdminPageProps = {
    api: makeApi(calls, rows),
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

  // HP-C04 按 2026-10-18 裁决第 5 条「商户异议→结算单进「有争议」挂起、最后审批不得通过；主管裁决（写结论留痕）后回待结算继续审批」改写：
  // 原「留言不改变结算状态、不暂停付款」文案作废。
  it("异议表单可见，文案写明提交后进「有争议」挂起、待主管裁决，不再写不改状态/不暂停付款；没有「有异议」KPI", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-dispute-hint").textContent).toContain("有争议"));
    expect(screen.getByTestId("merchant-zone-settlement-dispute-form")).toBeTruthy();
    const hint = screen.getByTestId("merchant-zone-settlement-dispute-hint").textContent ?? "";
    expect(hint).toContain("主管裁决");
    expect(hint).not.toContain("不改变结算状态");
    expect(hint).not.toContain("不暂停付款");
    expect(screen.getByTestId("merchant-zone-settlements-page").textContent).not.toContain("不改变结算状态");
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

// HP-C04（2026-10-18 裁决第 6 条「主管裁决后，商户在商户专区该结算单上可点「不接受裁决」（必填理由），单子进「有争议，诉讼/仲裁」」）。
describe("MerchantZoneSettlementsPage 处理结论与「不接受裁决」（HP-C04）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
  });

  const RULED = [
    { id: "st-r1", contract_id: "c-1", trigger_type: "move_out", status: "pending_settlement", dispute_reason: "违约金多算",
      dispute_resolution: "按清单结算，违约金不调整", resolved_by: "usr_mgr", resolved_at: "2026-10-10T00:00:00Z", settlement_items: [] },
    { id: "st-r2", contract_id: "c-2", trigger_type: "move_out", status: "disputed_litigation", dispute_reason: "不接受裁决：已申请仲裁",
      dispute_resolution: "按清单结算", resolved_by: "usr_mgr", resolved_at: "2026-10-10T00:00:00Z", settlement_items: [] },
    { id: "st-r3", contract_id: "c-3", trigger_type: "move_out", status: "pending_settlement", dispute_reason: null, settlement_items: [] },
    // 主管已录入诉讼/仲裁结果回待结算；主管裁决后商户又提新异议（新异议尚未裁决）。
    { id: "st-r4", contract_id: "c-4", trigger_type: "move_out", status: "pending_settlement", dispute_reason: "违约金多算\n不接受裁决：已申请仲裁",
      dispute_resolution: "按清单结算\n诉讼/仲裁结果：仲裁维持原清单", resolved_by: "usr_mgr", resolved_at: "2026-10-12T00:00:00Z", settlement_items: [] },
    { id: "st-r5", contract_id: "c-5", trigger_type: "move_out", status: "disputed", dispute_reason: "违约金多算\n又多算一期",
      dispute_resolution: "按清单结算", resolved_by: "usr_mgr", resolved_at: "2026-10-10T00:00:00Z", settlement_items: [] },
  ];

  it("结算表与明细显示主管处理结论", async () => {
    renderPage({}, [], RULED);
    await waitFor(() => expect(screen.getByTestId("merchant-zone-settlement-row-st-r1").textContent).toContain("按清单结算，违约金不调整"));
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-row-st-r1"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("按清单结算，违约金不调整");
  });

  it("「不接受裁决」只对已有主管裁决且未结清、未在诉讼/仲裁的结算单可点；填理由 → POST reject-resolution", async () => {
    const calls: Call[] = [];
    const props = renderPage({}, calls, RULED);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-settlement-reject-submit")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-r3"));
    expect((screen.getByTestId("merchant-zone-settlement-reject-submit") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-r2"));
    expect((screen.getByTestId("merchant-zone-settlement-reject-submit") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-r1"));
    fireEvent.change(screen.getByTestId("merchant-zone-settlement-dispute-reason"), { target: { value: "  已申请仲裁  " } });
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-reject-submit"));
    await waitFor(() => expect(calls.some((call) => call.method === "POST")).toBe(true));
    const post = calls.find((call) => call.method === "POST")!;
    expect(post.url).toContain("/api/v1/merchant-portal/settlements/st-r1/reject-resolution");
    expect(post.body?.reason).toBe("已申请仲裁");
    expect(String(post.body?.idempotency_key ?? "")).not.toBe("");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalled());
  });

  // 裁决第 6 条「诉讼/仲裁有结果后，由有裁决权的主管在后台录入结果，单子回待结算继续审批」：录入结果后不再显示（第三轮裁决第 7 条）；新异议待裁决时也不可凭旧裁决点。
  it("「不接受裁决」在已录入诉讼/仲裁结果、或新异议尚未裁决的结算单上不可点", async () => {
    const calls: Call[] = [];
    renderPage({}, calls, RULED);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-settlement-pick-st-r4")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-r4"));
    expect(screen.queryByTestId("merchant-zone-settlement-reject-submit")).toBeNull();
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-r5"));
    expect((screen.getByTestId("merchant-zone-settlement-reject-submit") as HTMLButtonElement).disabled).toBe(true);
    expect(calls.some((call) => call.method === "POST")).toBe(false);
  });

  it("「不接受裁决」理由为空 → 不发请求并给中文提示", async () => {
    const calls: Call[] = [];
    renderPage({}, calls, RULED);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-settlement-reject-submit")).toBeTruthy());
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-r1"));
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-reject-submit"));
    expect(screen.getByTestId("merchant-zone-settlement-form-error").textContent).toContain("必须填写理由");
    expect(calls.some((call) => call.method === "POST")).toBe(false);
  });
});


// HP-C06：BSM L1352「押金的退还……以退租结算算账完成为准执行支付，且不早于等待期届满……算账晚于等待期完成的顺延至算账完成之日支付，
// 顺延原因与预计时间在商户专区对商户可见」、2026-10-18 裁决第 7 条。
describe("MerchantZoneSettlementsPage 预计退押金日期（HP-C06）", () => {
  afterEach(() => vi.unstubAllGlobals());

  const REASON = "退租结算算账晚于等待期完成，顺延至算账完成之日支付";
  const ROWS = [
    { id: "st-w1", contract_id: "c-1", trigger_type: "move_out", status: "settled", dispute_reason: null, settlement_items: [
      { item: "deposit_disposal", amount_cent: 100000, status: "payable_posted", note: "押金退还", expected_refund_date: "2026-11-17" },
    ] },
    { id: "st-w2", contract_id: "c-2", trigger_type: "early_termination", status: "settled", dispute_reason: null, settlement_items: [
      { item: "deposit_disposal", amount_cent: 50000, status: "payable_posted", note: "押金退还", expected_refund_date: "2026-12-01",
        refund_deferral_reason: REASON },
    ] },
    { id: "st-w3", contract_id: "c-3", trigger_type: "move_out", status: "pending_settlement", dispute_reason: null, settlement_items: [] },
  ];

  it("结算单显示预计退押金日期（＝应付到期日）；算账晚于等待期时另显示顺延原因与预计时间；未落账的单显示 -", async () => {
    renderPage({}, [], ROWS);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-settlement-refund-st-w1")?.textContent ?? "").toContain("2026年11月17日"));
    expect(screen.getByTestId("merchant-zone-settlement-refund-st-w1").textContent).not.toContain("顺延");
    const deferred = screen.getByTestId("merchant-zone-settlement-refund-st-w2").textContent ?? "";
    expect(deferred).toContain("2026年12月1日");
    expect(deferred).toContain(REASON);
    expect(deferred).toContain("预计 2026年12月1日支付");
    expect(screen.getByTestId("merchant-zone-settlement-refund-st-w3").textContent).toBe("-");
  });
});

// 第三轮裁决第 4 条＋控制席约定「落账后的「押金抵扣中」「欠费追缴中」是已落账状态——不可再提异议」；第 7 条「诉讼/仲裁结果录入后，
// 该结算单商户专区不再提供「提异议」和「不接受裁决」，按结果继续审批落账」。
describe("MerchantZoneSettlementsPage 落账后与诉讼/仲裁结果录入后（第三轮裁决第 4、7 条）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/merchant-zone/settlements");
  });

  const ROWS = [
    { id: "st-p1", contract_id: "c-1", trigger_type: "move_out", status: "deposit_offsetting", dispute_reason: null, settlement_items: [] },
    { id: "st-p2", contract_id: "c-2", trigger_type: "expiry", status: "arrears_collecting", dispute_reason: null, settlement_items: [] },
    { id: "st-p3", contract_id: "c-3", trigger_type: "move_out", status: "pending_settlement", dispute_reason: "违约金多算\n不接受裁决：已申请仲裁",
      dispute_resolution: "按清单结算\n诉讼/仲裁结果：仲裁维持原清单", resolved_by: "usr_mgr", resolved_at: "2026-10-12T00:00:00Z", settlement_items: [] },
    { id: "st-p4", contract_id: "c-4", trigger_type: "move_out", status: "pending_settlement", dispute_reason: null, settlement_items: [] },
  ];

  it("押金抵扣中/欠费追缴中显示中文、不可提异议；已录入诉讼/仲裁结果的结算单不再显示「提交异议」「不接受裁决」", async () => {
    const calls: Call[] = [];
    renderPage({}, calls, ROWS);
    await waitFor(() => expect(screen.queryByTestId("merchant-zone-settlement-row-st-p1")?.textContent ?? "").toContain("押金抵扣中"));
    expect(screen.getByTestId("merchant-zone-settlement-row-st-p2").textContent).toContain("欠费追缴中");
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-p1"));
    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-p2"));
    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-p4"));
    expect((screen.getByTestId("merchant-zone-settlement-dispute-submit") as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(screen.getByTestId("merchant-zone-settlement-pick-st-p3"));
    expect(screen.queryByTestId("merchant-zone-settlement-dispute-submit")).toBeNull();
    expect(screen.queryByTestId("merchant-zone-settlement-reject-submit")).toBeNull();
    expect(calls.some((call) => call.method === "POST")).toBe(false);
  });
});
