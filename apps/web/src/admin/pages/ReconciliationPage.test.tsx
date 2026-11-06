import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ReconciliationPage } from "./ReconciliationPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 Y 段第 180 条 180-R2：桩为后端财务记录的真实字段（ReconciliationBatchRecord、ReconciliationDifferenceRecord）。
const BATCHES = [
  { id: "b-1", batchNo: "RB-0001", businessDate: "2026-07-01", scopeType: "bar", scopeId: "bar_a", status: "open" },
];
const DIFFERENCES = [
  { id: "d-1", batchId: "b-1", differenceType: "amount", businessRef: "pay-1", amountCent: 2500, expectedAmountCent: 10000, actualAmountCent: 7500, source: "manual", status: "pending_handling", evidenceAttachmentIds: ["att-1"] },
  { id: "d-2", batchId: "b-1", differenceType: "missing", businessRef: "pay-2", amountCent: 800, expectedAmountCent: 800, actualAmountCent: 0, source: "system", status: "closed", evidenceAttachmentIds: [] },
];

function getResolver() {
  return vi.fn().mockImplementation((path: string) => {
    if (path.includes("/reconciliation/batches")) return Promise.resolve(BATCHES);
    if (path.includes("/reconciliation/differences")) return Promise.resolve(DIFFERENCES);
    return Promise.resolve([]);
  });
}

function makeApi(get = getResolver()): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(api: AdminApiClient = makeApi()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<ReconciliationPage {...props} />);
}

describe("ReconciliationPage KPI 聚焦视图（带参重进本页）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
  });

  it("点“对账差异”数字 → URL 带 ?kpi=differences，只剩差异承载表+复核表单，批次表/KPI 卡隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-differences"));
    expect(window.location.search).toContain("kpi=differences");
    expect(screen.getByTestId("reconciliation-kpi-focus-banner").textContent).toContain("正在查看：对账差异（2）");
    expect(screen.getByTestId("reconciliation-differences-table")).toBeTruthy();
    expect(screen.getByTestId("reconciliation-review-form")).toBeTruthy();
    expect(screen.queryByTestId("reconciliation-batches-table")).toBeNull();
    expect(screen.queryByTestId("reconciliation-summary")).toBeNull();
  });

  it("点“对账批次”数字 → 只剩批次承载表，差异表/复核表单隐藏", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-batches")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-batches"));
    expect(window.location.search).toContain("kpi=batches");
    expect(screen.getByTestId("reconciliation-batches-table")).toBeTruthy();
    expect(screen.queryByTestId("reconciliation-differences-table")).toBeNull();
    expect(screen.queryByTestId("reconciliation-review-form")).toBeNull();
  });

  it("聚焦视图点「返回全部」→ 参数清除，恢复完整页面（两表+KPI 卡都在）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-differences"));
    fireEvent.click(screen.getByTestId("reconciliation-kpi-focus-clear"));
    expect(window.location.search).not.toContain("kpi=");
    expect(screen.queryByTestId("reconciliation-kpi-focus-banner")).toBeNull();
    expect(screen.getByTestId("reconciliation-summary")).toBeTruthy();
    expect(screen.getByTestId("reconciliation-batches-table")).toBeTruthy();
    expect(screen.getByTestId("reconciliation-differences-table")).toBeTruthy();
  });

  it("带参直链进入 → 直接呈现聚焦视图（参数从 URL 重建）", async () => {
    window.history.replaceState({}, "", "/admin/finance/reconciliation?kpi=batches");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-focus-banner")).toBeTruthy());
    expect(screen.getByTestId("reconciliation-batches-table")).toBeTruthy();
    expect(screen.queryByTestId("reconciliation-differences-table")).toBeNull();
  });

  it("浏览器后退（popstate）→ 退出聚焦视图回完整页面", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-differences")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-kpi-differences"));
    expect(screen.getByTestId("reconciliation-kpi-focus-banner")).toBeTruthy();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
    fireEvent.popState(window);
    await waitFor(() => expect(screen.queryByTestId("reconciliation-kpi-focus-banner")).toBeNull());
    expect(screen.getByTestId("reconciliation-summary")).toBeTruthy();
  });

  it("选中差异卡状态与差异表状态列渲染为中文枚举，不直出英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-summary")).toBeTruthy());
    // 选中首个差异（status=pending_handling）→ KPI 卡状态应显示中文“待处理”，不是 "pending_handling"。
    fireEvent.click(screen.getByTestId("reconciliation-select-d-1"));
    const summary = screen.getByTestId("reconciliation-summary").textContent ?? "";
    expect(summary).toContain("待处理");
    expect(summary).not.toContain("pending_handling");
    // 差异金额卡的口径说明不再直出接口路径英文。
    expect(summary).not.toContain("POST");
    const diffTable = screen.getByTestId("reconciliation-differences-table").textContent ?? "";
    expect(diffTable).toContain("待处理");
    expect(diffTable).toContain("已关闭");
    expect(diffTable).not.toContain("pending_handling");
    expect(diffTable).not.toContain("closed");
  });
});

describe("ReconciliationPage 行明细抽屉（整行可点·零英文）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
  });

  it("点批次行 -> 抽屉含批次号、营业日与范围（中文）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-batch-row-b-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-batch-row-b-1"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("RB-0001");
    expect(drawer).toContain("2026年7月1日");
    expect(drawer).toContain("酒吧 bar_a");
  });

  it("点差异行 -> 抽屉含差异号、差异类型、差额、来源与中文状态；Esc 关闭抽屉", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-difference-row-d-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-difference-row-d-1"));
    const drawer = screen.getByTestId("row-detail-drawer").textContent ?? "";
    expect(drawer).toContain("d-1");
    expect(drawer).toContain("金额差异");
    expect(drawer).toContain("¥25.00");
    expect(drawer).toContain("人工录入");
    expect(drawer).toContain("待处理");
    expect(drawer).not.toContain("pending_handling");
    fireEvent.keyDown(window, { key: "Escape" });
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")).toBeNull());
  });

  it("差异行点单选框仅选中、不弹抽屉（stopPropagation）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-select-d-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("reconciliation-select-d-1"));
    expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
  });
});

// 第十八批 P 段第 133 条 133-R3（ENUM:188「对账口径：按订单、按核销、按合同、按渠道、按日结」）：
// 没有第三方对账批次和差异时页面显示「暂无数据」，日结区块仍渲染；KPI 聚焦视图只放被点那张承载表，不渲染日结区块。
describe("ReconciliationPage 日结区块", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
  });

  it("对账批次与差异都为空时显示暂无数据，日结区块仍渲染", async () => {
    const api = makeApi(vi.fn().mockResolvedValue([]));
    api.getPaged = vi.fn().mockResolvedValue({ items: [{ id: "dc1", businessDate: "2026-11-02", scopeType: "bar", scopeId: "bar_a", status: "locked" }], total: 1, page: 1, size: 20 });
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("admin-empty").textContent).toContain("暂无数据"));
    await waitFor(() => expect(screen.queryByTestId("daily-close-row-dc1")).not.toBeNull());
  });

  it("KPI 聚焦视图不渲染日结区块", async () => {
    window.history.replaceState({}, "", "/admin/finance/reconciliation?kpi=batches");
    renderPage();
    await waitFor(() => expect(screen.getByTestId("reconciliation-kpi-focus-banner")).toBeTruthy());
    expect(screen.queryByTestId("finance-daily-close-panel")).toBeNull();
    expect(screen.queryByTestId("bar-shift-panel")).toBeNull();
  });

  // 第十八批 R 段第 142 条 142-R3（BSM:1848「财务看退款、差异、日结和结算依据。」）：收银交班复核区块同在对账批次与差异之外。
  it("对账批次与差异都为空时收银交班复核区块仍渲染", async () => {
    const api = makeApi(vi.fn().mockImplementation((path: string) => Promise.resolve(path === "/api/v1/bar/shifts"
      ? [{ id: "s1", barId: "bar_a", cashierUserId: "u2", status: "closed", cashDiffCent: -100, needsReview: true }] : [])));
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("admin-empty").textContent).toContain("暂无数据"));
    await waitFor(() => expect(screen.queryByTestId("bar-shift-resolve-s1")).not.toBeNull());
  });
});

// 第十八批 Y 段第 180 条 180-R2（ENUM:189「金额差异、数量差异、状态差异、缺失差异（单边缺失）」；BSM:1746「平台侧金额必须系统自动取数、不许手填；
// 第三方侧接入前可人工录入但须标记并强制复核」；ENUM:202「渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证」）：
// 页面按后端财务记录字段显示；「登记差异」表单未上传原件不能提交，上传后以蛇形键提交。走真实客户端，假响应用文本体。
describe("ReconciliationPage 180-R2 后端字段与登记差异", () => {
  const SNAKE_BATCHES = [{ id: "rb-1", batch_no: "RB-0001", scope_type: "bar", scope_id: "bar_a", business_date: "2026-07-01", status: "open", created_at: "2026-07-02T01:00:00Z" }];
  const SNAKE_DIFFERENCES = [
    { id: "rd-1", batch_id: "rb-1", difference_type: "amount", business_ref: "pay-1", amount_cent: 2500, expected_amount_cent: 10000, actual_amount_cent: 7500, source: "manual", status: "pending_review", created_at: "2026-07-02T02:00:00Z", evidence_attachment_ids: ["att-0"] },
    { id: "rd-2", batch_id: "rb-1", difference_type: "missing", business_ref: "pay-2", amount_cent: 800, expected_amount_cent: 800, actual_amount_cent: 0, source: "system", status: "closed", created_at: "2026-07-02T03:00:00Z", evidence_attachment_ids: [] },
  ];

  function textBody(data: unknown, status = 200): Response {
    return new Response(JSON.stringify(status < 400 ? { success: true, code: "OK", message: "success", data } : { success: false, code: "FORBIDDEN", message: "无权限" }),
      { status, headers: { "content-type": "application/json" } });
  }

  function stubFetch(posts: Array<{ url: string; body: unknown }>) {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST") {
        posts.push({ url, body: init.body });
        if (url.includes("/evidence/attachments/upload")) return Promise.resolve(textBody({ id: "att-9", file_name: "statement.jpg" }));
        return Promise.resolve(textBody({ id: "rd-3", batch_id: "rb-1", status: "pending_identification", evidence_attachment_ids: ["att-9"] }));
      }
      if (url.includes("/analytics/metric-detail")) return Promise.resolve(textBody(null, 403));
      if (url.includes("/reconciliation/batches")) return Promise.resolve(textBody({ items: SNAKE_BATCHES, page: 1, page_size: 50, total: 1 }));
      if (url.includes("/reconciliation/differences")) return Promise.resolve(textBody({ items: SNAKE_DIFFERENCES, page: 1, page_size: 50, total: 2 }));
      return Promise.resolve(textBody({ items: [], page: 1, page_size: 20, total: 0 }));
    }));
  }

  function renderReal(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS) {
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions,
    };
    render(<ReconciliationPage {...props} />);
    return props;
  }

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
  });

  it("表格按后端字段显示营业日、范围、差异类型中文、平台侧/第三方侧/差额（元）与来源中文；已关闭不计未处理", async () => {
    stubFetch([]);
    renderReal();
    await waitFor(() => expect(screen.queryByTestId("reconciliation-difference-row-rd-1")).not.toBeNull());

    const batchTable = screen.getByTestId("reconciliation-batches-table").textContent ?? "";
    expect(batchTable).toContain("RB-0001");
    expect(batchTable).toContain("2026年7月1日");
    expect(batchTable).toContain("酒吧 bar_a");
    const first = screen.getByTestId("reconciliation-difference-row-rd-1").textContent ?? "";
    for (const text of ["2026年7月1日", "金额差异", "pay-1", "¥100.00", "¥75.00", "¥25.00", "人工录入", "待复核"]) expect(first).toContain(text);
    const second = screen.getByTestId("reconciliation-difference-row-rd-2").textContent ?? "";
    for (const text of ["缺失差异", "系统取数", "已关闭"]) expect(second).toContain(text);
    expect(screen.getByTestId("reconciliation-summary").textContent).toContain("未处理: 1");
  });

  it("登记差异：未上传对账单原件不能提交；上传后以蛇形键提交 batch_id、difference_type、business_ref、source、actual_amount_cent、evidence_attachment_ids", async () => {
    const posts: Array<{ url: string; body: unknown }> = [];
    stubFetch(posts);
    renderReal();
    await waitFor(() => expect((screen.queryByTestId("reconciliation-register-batch") as HTMLSelectElement | null)?.options.length).toBe(2));

    fireEvent.change(screen.getByTestId("reconciliation-register-batch"), { target: { value: "rb-1" } });
    fireEvent.change(screen.getByTestId("reconciliation-register-type"), { target: { value: "missing" } });
    fireEvent.change(screen.getByTestId("reconciliation-register-ref"), { target: { value: "pay-9" } });
    fireEvent.change(screen.getByTestId("reconciliation-register-actual"), { target: { value: "12.34" } });
    const submit = screen.getByTestId("reconciliation-register-submit") as HTMLButtonElement;
    expect(submit.disabled).toBe(true);

    fireEvent.change(screen.getByTestId("reconciliation-register-statement"), { target: { files: [new File(["x"], "statement.jpg", { type: "image/jpeg" })] } });
    await waitFor(() => expect(submit.disabled).toBe(false));
    const upload = posts.find((post) => post.url.includes("/api/v1/evidence/attachments/upload"));
    expect((upload?.body as FormData).get("business_type")).toBe("finance_reconciliation_difference");

    fireEvent.click(submit);
    await waitFor(() => expect(posts.some((post) => post.url.endsWith("/api/v1/finance/reconciliation/differences"))).toBe(true));
    const body = JSON.parse(String(posts.find((post) => post.url.endsWith("/api/v1/finance/reconciliation/differences"))!.body));
    expect(body).toMatchObject({ batch_id: "rb-1", difference_type: "missing", business_ref: "pay-9", source: "manual",
      actual_amount_cent: 1234, evidence_attachment_ids: ["att-9"] });
    expect(body.idempotency_key).toBeTruthy();
  });

  // 第十八批 Y 段第 180 条 180-R3（ENUM:188「对账口径 按订单、按核销、按合同、按渠道、按日结」）：业务单号可填收款记录号、票务订单号或酒吧账单号。
  it("登记差异：业务单号标签写明收款记录号、票务订单号或酒吧账单号", async () => {
    stubFetch([]);
    renderReal();
    await waitFor(() => expect(screen.queryByTestId("reconciliation-register-ref")).not.toBeNull());
    expect(screen.getByTestId("reconciliation-register-ref").closest("label")?.textContent).toBe("业务单号（收款记录号、票务订单号或酒吧账单号）");
  });

  it("没有 finance.reconciliation.difference.create 权限不出登记表单", async () => {
    stubFetch([]);
    renderReal({ status: "ready", value: { grants: ["finance.reconciliation.view", "finance.reconciliation.resolve"], denies: [], wildcard: false } });
    await waitFor(() => expect(screen.queryByTestId("reconciliation-difference-row-rd-1")).not.toBeNull());
    expect(screen.queryByTestId("reconciliation-register-form")).toBeNull();
  });
});

// 第十八批 Y 段第 181 条 181-R1（ENUM:191「差异处理状态 待识别、待处理、待复核、已关闭。……复核环节属业务审批……各阶段由财务岗全程负责。」；
// BSM:1729「对不上的进差异处理，按原因定位、复核后处理」）：差异表四态中文并显示负责人与本阶段起始时间；待识别的行出「识别」，待处理的行出
// 「提交复核」，待复核的行只提示到主管复核页审批。走真实客户端，假响应用文本体。
describe("ReconciliationPage 181-R1 差异四态", () => {
  const BATCHES_181 = [{ id: "rb-1", batch_no: "RB-0001", scope_type: "bar", scope_id: "bar_a", business_date: "2026-07-01", status: "open", created_at: "2026-07-02T01:00:00Z" }];
  const row = (id: string, status: string) => ({ id, batch_id: "rb-1", difference_type: "amount", business_ref: `pay-${id}`, amount_cent: 2500,
    expected_amount_cent: 10000, actual_amount_cent: 7500, source: "manual", status, created_at: "2026-07-02T02:00:00Z", evidence_attachment_ids: ["att-0"],
    responsible_user_id: "usr-fin-1", stage_entered_at: "2026-07-02T02:00:00Z", identified_cause: null, review_id: status === "pending_review" ? "review-1" : null });
  const ROWS_181 = [row("rd-a", "pending_identification"), row("rd-b", "pending_handling"), row("rd-c", "pending_review"), row("rd-d", "closed")];

  function body(data: unknown, status = 200): Response {
    return new Response(JSON.stringify(status < 400 ? { success: true, code: "OK", message: "success", data } : { success: false, code: "FORBIDDEN", message: "无权限" }),
      { status, headers: { "content-type": "application/json" } });
  }

  function stub(posts: Array<{ url: string; body: unknown }>) {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST") {
        posts.push({ url, body: init.body });
        if (url.includes("/evidence/attachments/upload")) return Promise.resolve(body({ id: "att-9", file_name: "statement.jpg" }));
        return Promise.resolve(body({ id: url.split("/").at(-2), status: url.endsWith("/identify") ? "pending_handling" : "pending_review", review_id: "review-9" }));
      }
      if (url.includes("/analytics/metric-detail")) return Promise.resolve(body(null, 403));
      if (url.includes("/reconciliation/batches")) return Promise.resolve(body({ items: BATCHES_181, page: 1, page_size: 50, total: 1 }));
      if (url.includes("/reconciliation/differences")) return Promise.resolve(body({ items: ROWS_181, page: 1, page_size: 50, total: 4 }));
      return Promise.resolve(body({ items: [], page: 1, page_size: 20, total: 0 }));
    }));
  }

  function renderReal() {
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions: ADMIN_PERMS,
      navigateTo: vi.fn(),
    };
    render(<ReconciliationPage {...props} />);
    return props;
  }

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
  });

  it("差异表四态中文，显示负责人与本阶段起始时间，不直出英文码；没有「正式关闭接口」卡", async () => {
    stub([]);
    renderReal();
    await waitFor(() => expect(screen.queryByTestId("reconciliation-difference-row-rd-d")).not.toBeNull());

    const table = screen.getByTestId("reconciliation-differences-table").textContent ?? "";
    for (const text of ["待识别", "待处理", "待复核", "已关闭", "负责人", "本阶段起始时间", "usr-fin-1"]) expect(table).toContain(text);
    for (const code of ["pending_identification", "pending_handling", "pending_review"]) expect(table).not.toContain(code);
    expect(screen.getByTestId("reconciliation-summary").textContent).not.toContain("正式关闭接口");
  });

  it("待识别的行点「识别」，以蛇形键 POST identify 带 cause_note", async () => {
    const posts: Array<{ url: string; body: unknown }> = [];
    stub(posts);
    renderReal();
    await waitFor(() => expect(screen.queryByTestId("reconciliation-identify-form")).not.toBeNull());
    expect(screen.queryByTestId("reconciliation-review-submit")).toBeNull();

    fireEvent.change(screen.getByTestId("reconciliation-identify-cause"), { target: { value: "渠道少记一笔核销" } });
    fireEvent.click(screen.getByTestId("reconciliation-identify-submit"));
    await waitFor(() => expect(posts.some((post) => post.url.endsWith("/api/v1/finance/reconciliation/differences/rd-a/identify"))).toBe(true));
    const sent = JSON.parse(String(posts.find((post) => post.url.endsWith("/rd-a/identify"))!.body));
    expect(sent).toMatchObject({ cause_note: "渠道少记一笔核销" });
    expect(sent.idempotency_key).toBeTruthy();
  });

  it("待处理的行提交复核：以蛇形键 POST close 带 close_reason、evidence_attachment_ids、final_approver_id", async () => {
    const posts: Array<{ url: string; body: unknown }> = [];
    stub(posts);
    renderReal();
    // 同 rd-c 用例：先等首行（待识别）的「识别」按钮落定；默认选中首行在被动 effect 里补做，没落定就点 rd-b 会被补跑的 effect 改回 rd-a，
    // 所以没勾上就再点一次，直到 rd-b 勾上、出「提交复核」表单。
    await waitFor(() => expect(screen.queryByTestId("reconciliation-select-rd-b")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("reconciliation-identify-submit")).not.toBeNull());
    await waitFor(() => {
      const rdB = screen.getByTestId("reconciliation-select-rd-b") as HTMLInputElement;
      if (!rdB.checked) fireEvent.click(rdB);
      expect(rdB.checked).toBe(true);
      expect(screen.queryByTestId("reconciliation-review-final-approver")).not.toBeNull();
    });
    expect(screen.queryByTestId("reconciliation-identify-form")).toBeNull();

    fireEvent.change(screen.getByTestId("reconciliation-review-reason"), { target: { value: "以平台口径为准" } });
    fireEvent.change(screen.getByTestId("reconciliation-review-final-approver"), { target: { value: "usr-boss" } });
    fireEvent.change(screen.getByTestId("reconciliation-review-attachment"), { target: { files: [new File(["x"], "statement.jpg", { type: "image/jpeg" })] } });
    await waitFor(() => expect(screen.getByTestId("reconciliation-review-attachments").textContent).toContain("statement.jpg"));
    fireEvent.click(screen.getByTestId("reconciliation-review-submit"));
    await waitFor(() => expect(posts.some((post) => post.url.endsWith("/api/v1/finance/reconciliation/differences/rd-b/close"))).toBe(true));
    const sent = JSON.parse(String(posts.find((post) => post.url.endsWith("/rd-b/close"))!.body));
    expect(sent).toMatchObject({ close_reason: "以平台口径为准", evidence_attachment_ids: ["att-9"], final_approver_id: "usr-boss" });
  });

  it("待复核的行没有识别、提交复核按钮，提示到主管复核页审批并可跳转", async () => {
    stub([]);
    const props = renderReal();
    // 先等列表与首行（待识别）的「识别」按钮落定；默认选中首行在被动 effect 里补做、本页单选框 checked 又回落首行，没落定就点 rd-c 会被补跑的
    // effect 改回 rd-a（全量负载下偶发），所以没勾上就再点一次，直到 rd-c 勾上、出「复核中」提示（点已勾上的单选不触发 onChange）。
    await waitFor(() => expect(screen.queryByTestId("reconciliation-select-rd-c")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("reconciliation-identify-submit")).not.toBeNull());
    await waitFor(() => {
      const rdC = screen.getByTestId("reconciliation-select-rd-c") as HTMLInputElement;
      if (!rdC.checked) fireEvent.click(rdC);
      expect(rdC.checked).toBe(true);
      expect(screen.queryByTestId("reconciliation-reviewing")).not.toBeNull();
    });

    expect(screen.queryByTestId("reconciliation-identify-submit")).toBeNull();
    expect(screen.queryByTestId("reconciliation-review-submit")).toBeNull();
    expect(screen.getByTestId("reconciliation-reviewing").textContent).toContain("复核中，到主管复核页审批");
    fireEvent.click(screen.getByTestId("reconciliation-goto-review"));
    expect(props.navigateTo).toHaveBeenCalledWith("ADM-GOV-006");
  });
});

// 第十八批 Y 段第 181 条 181-R2（ENUM:191「待识别与待处理两个阶段各自的停留时限为业务参数，按项目可配，出厂默认 1 个工作日，须同时配置上下限；
// 超时未推进的，系统自动提醒并升级到上一级负责人。」）：持 finance.reconciliation.policy.manage 才出「差异停留时限（按项目）」，填项目编号查询、
// 按阶段以蛇形键 PUT 时限与上下限；本阶段已超时升级的差异在状态后标「已超时升级」。走真实客户端，假响应用文本体。
describe("ReconciliationPage 181-R2 差异停留时限", () => {
  const BATCHES_R2 = [{ id: "rb-1", batch_no: "RB-0001", scope_type: "bar", scope_id: "bar_a", business_date: "2026-07-01", status: "open", created_at: "2026-07-02T01:00:00Z" }];
  const row = (id: string, status: string, escalatedAt: string | null) => ({ id, batch_id: "rb-1", difference_type: "amount", business_ref: `pay-${id}`,
    amount_cent: 2500, expected_amount_cent: 10000, actual_amount_cent: 7500, source: "manual", status, created_at: "2026-07-02T02:00:00Z",
    evidence_attachment_ids: ["att-0"], responsible_user_id: "usr-fin-1", stage_entered_at: "2026-07-02T02:00:00Z", identified_cause: null, review_id: null,
    stage_escalated_at: escalatedAt });
  const ROWS_R2 = [row("rd-e", "pending_handling", "2026-07-03T02:00:00Z"), row("rd-f", "pending_identification", null)];
  const LIMITS = [
    { project_id: "proj_1", stage: "pending_identification", working_days: 1, min_working_days: null, max_working_days: null },
    { project_id: "proj_1", stage: "pending_handling", working_days: 3, min_working_days: 1, max_working_days: 5 },
  ];

  function body(data: unknown, status = 200): Response {
    return new Response(JSON.stringify(status < 400 ? { success: true, code: "OK", message: "success", data } : { success: false, code: "FORBIDDEN", message: "无权限" }),
      { status, headers: { "content-type": "application/json" } });
  }

  function stub(calls: Array<{ url: string; method?: string; body: unknown }>) {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      calls.push({ url, method: init?.method, body: init?.body });
      if (url.includes("/reconciliation/stage-limits")) return Promise.resolve(body(init?.method === "PUT" ? LIMITS[1] : LIMITS));
      if (url.includes("/analytics/metric-detail")) return Promise.resolve(body(null, 403));
      if (url.includes("/reconciliation/batches")) return Promise.resolve(body({ items: BATCHES_R2, page: 1, page_size: 50, total: 1 }));
      if (url.includes("/reconciliation/differences")) return Promise.resolve(body({ items: ROWS_R2, page: 1, page_size: 50, total: 2 }));
      return Promise.resolve(body({ items: [], page: 1, page_size: 20, total: 0 }));
    }));
  }

  function renderReal(effectivePermissions: EffectivePermissionsState = ADMIN_PERMS) {
    const props: AdminPageProps = {
      api: new AdminApi("test-token"),
      user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(),
      setGlobalError: vi.fn(),
      effectivePermissions,
    };
    render(<ReconciliationPage {...props} />);
    return props;
  }

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
  });

  it("填项目编号查询两阶段时限，按阶段以蛇形键 PUT stage、working_days、min_working_days、max_working_days", async () => {
    const calls: Array<{ url: string; method?: string; body: unknown }> = [];
    stub(calls);
    renderReal();
    // 先等差异表加载落定与时限小节出现，再操作。
    await waitFor(() => expect(screen.queryByTestId("reconciliation-difference-row-rd-e")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("reconciliation-stage-limit-project")).not.toBeNull());

    fireEvent.change(screen.getByTestId("reconciliation-stage-limit-project"), { target: { value: "proj_1" } });
    fireEvent.click(screen.getByTestId("reconciliation-stage-limit-query"));
    await waitFor(() => expect(screen.getByTestId("reconciliation-stage-limit-current").textContent ?? "").toContain("待识别：1 个工作日（未配置，按出厂默认）"));
    expect(calls.some((call) => call.url.endsWith("/api/v1/finance/reconciliation/stage-limits?project_id=proj_1"))).toBe(true);
    expect(screen.getByTestId("reconciliation-stage-limit-current").textContent).toContain("待处理：3 个工作日（下限 1，上限 5）");

    fireEvent.change(screen.getByTestId("reconciliation-stage-limit-stage"), { target: { value: "pending_handling" } });
    fireEvent.change(screen.getByTestId("reconciliation-stage-limit-days"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("reconciliation-stage-limit-min"), { target: { value: "1" } });
    fireEvent.change(screen.getByTestId("reconciliation-stage-limit-max"), { target: { value: "5" } });
    fireEvent.click(screen.getByTestId("reconciliation-stage-limit-save"));
    await waitFor(() => expect(calls.some((call) => call.method === "PUT")).toBe(true));
    const put = calls.find((call) => call.method === "PUT")!;
    expect(put.url.endsWith("/api/v1/finance/reconciliation/stage-limits/proj_1")).toBe(true);
    const sent = JSON.parse(String(put.body));
    expect(Object.keys(sent).sort()).toEqual(["idempotency_key", "max_working_days", "min_working_days", "stage", "working_days"]);
    expect(sent).toMatchObject({ stage: "pending_handling", working_days: 3, min_working_days: 1, max_working_days: 5 });
  });

  it("没有 finance.reconciliation.policy.manage 不出时限小节", async () => {
    stub([]);
    renderReal({ status: "ready", value: { grants: ["finance.reconciliation.view", "finance.reconciliation.resolve"], denies: [], wildcard: false } });
    await waitFor(() => expect(screen.queryByTestId("reconciliation-difference-row-rd-e")).not.toBeNull());
    expect(screen.queryByTestId("reconciliation-stage-limit-project")).toBeNull();
  });

  it("本阶段已超时升级的差异在状态后标「已超时升级」，未升级的不标", async () => {
    stub([]);
    renderReal();
    await waitFor(() => expect(screen.queryByTestId("reconciliation-difference-row-rd-f")).not.toBeNull());
    expect(screen.getByTestId("reconciliation-difference-row-rd-e").textContent).toContain("待处理（已超时升级）");
    expect(screen.getByTestId("reconciliation-difference-row-rd-f").textContent).not.toContain("已超时升级");
  });
});

// 第十八批 Y 段复核修复（BSM:1682「按营业日生成日结批次。」；ENUM:191「差异未关闭期间，高价值差异所涉营业日的日结不得确认锁定」）：180-R1 起对账批次随日结生成，
// 同页日结区块生成日结后重读对账批次与差异，「登记差异」批次下拉即出现新批次；范围内还没有任何批次和差异时空态只挡列表，登记表单照常可见。
// ENUM:202「渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证」：登记差异与提交复核两处上传控件另收 PDF、Excel、CSV 原件。走真实客户端，假响应用文本体。
describe("ReconciliationPage 生成日结后重读批次与对账单原件格式", () => {
  const OLD_BATCH = { id: "rb-1", batch_no: "RB-0001", scope_type: "bar", scope_id: "bar_a", business_date: "2026-07-01", status: "open", created_at: "2026-07-02T01:00:00Z" };
  const NEW_BATCH = { id: "rb-2", batch_no: "RB-0002", scope_type: "project", scope_id: "proj_1", business_date: "2026-11-02", status: "open", created_at: "2026-11-03T01:00:00Z" };
  const HANDLING = { id: "rd-h", batch_id: "rb-1", difference_type: "amount", business_ref: "pay-h", amount_cent: 2500, expected_amount_cent: 10000,
    actual_amount_cent: 7500, source: "manual", status: "pending_handling", created_at: "2026-07-02T02:00:00Z", evidence_attachment_ids: [] };

  function body(data: unknown, status = 200): Response {
    return new Response(JSON.stringify(status < 400 ? { success: true, code: "OK", message: "success", data } : { success: false, code: "FORBIDDEN", message: "无权限" }),
      { status, headers: { "content-type": "application/json" } });
  }

  // 生成日结（POST /api/v1/finance/daily-closes）之后，批次接口才带上随日结生成的新批次。
  function stub(batches: unknown[], differences: unknown[] = []) {
    let generated = false;
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "POST" && url.endsWith("/api/v1/finance/daily-closes")) {
        generated = true;
        return Promise.resolve(body({ id: "dc-new", business_date: "2026-11-02", scope_type: "project", scope_id: "proj_1", status: "pending_confirm" }));
      }
      if (url.includes("/analytics/metric-detail")) return Promise.resolve(body(null, 403));
      if (url.includes("/reconciliation/batches")) {
        const items = generated ? [...batches, NEW_BATCH] : batches;
        return Promise.resolve(body({ items, page: 1, page_size: 50, total: items.length }));
      }
      if (url.includes("/reconciliation/differences")) return Promise.resolve(body({ items: differences, page: 1, page_size: 50, total: differences.length }));
      return Promise.resolve(body({ items: [], page: 1, page_size: 20, total: 0 }));
    }));
  }

  function renderReal() {
    render(<ReconciliationPage api={new AdminApi("test-token")} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
  }

  // 中文日期选择器：选年 → 选月 → 选日（同 FinanceDailyClosePanel.test.tsx）。
  async function pickDate(year: string, month: string, day: string) {
    fireEvent.click(screen.getByTestId("daily-close-date"));
    fireEvent.click(await screen.findByRole("button", { name: "选择年月" }));
    fireEvent.click(screen.getByRole("button", { name: "选择年份" }));
    fireEvent.click(screen.getByRole("button", { name: year }));
    fireEvent.click(screen.getByRole("button", { name: month }));
    fireEvent.click(screen.getByRole("button", { name: day }));
    await waitFor(() => expect(screen.getByTestId("daily-close-date").textContent).toContain(`${year}年`));
  }

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/finance/reconciliation");
  });

  it("范围内还没有任何批次和差异时显示暂无数据，「登记差异」表单照常可见", async () => {
    stub([]);
    renderReal();
    await waitFor(() => expect(screen.queryByTestId("admin-empty")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("reconciliation-register-form")).not.toBeNull());
  });

  it("同页日结区块生成日结后，「登记差异」批次下拉出现随日结生成的新批次，批次数随之更新", async () => {
    stub([OLD_BATCH]);
    renderReal();
    await waitFor(() => expect((screen.queryByTestId("reconciliation-register-batch") as HTMLSelectElement | null)?.options.length).toBe(2));
    await waitFor(() => expect(screen.queryByTestId("daily-close-generate-submit")).not.toBeNull());

    await pickDate("2026", "11月", "2");
    fireEvent.change(screen.getByLabelText("范围编号"), { target: { value: "proj_1" } });
    fireEvent.click(screen.getByTestId("daily-close-generate-submit"));
    await waitFor(() => expect([...(screen.getByTestId("reconciliation-register-batch") as HTMLSelectElement).options].map((option) => option.textContent))
      .toContain("2026年11月2日 · 项目 proj_1"));
    expect(screen.getByTestId("reconciliation-kpi-batches").textContent).toBe("2");
  });

  it("登记差异与提交复核两处上传控件除照片、视频外另收 PDF、Excel、CSV，并写明支持 PDF、Excel、CSV 原件", async () => {
    stub([OLD_BATCH], [HANDLING]);
    renderReal();
    await waitFor(() => expect(screen.queryByTestId("reconciliation-review-attachment")).not.toBeNull());
    for (const testId of ["reconciliation-register-statement", "reconciliation-review-attachment"]) {
      const input = screen.getByTestId(testId);
      expect(input.getAttribute("accept")).toBe("image/*,video/*,.pdf,.xlsx,.csv");
      expect(input.closest("label")?.textContent).toContain("支持 PDF、Excel、CSV 原件");
    }
  });
});
