// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SettlementConfigPage } from "./SettlementConfigPage";
import { ApiError } from "../../shared/apiError";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 结算配置中心总控页(ADM-STL-001)组件测试:概览默认值 + 通票分账 8 块「改」落草案 + 草案队列转正/下线路由 + 差人友好提示。
const DEFAULTS = [
  // S4:原「项目公司留存比例」已并入门票综合服务费(同一项只扣一次),留存卡片下线。
  { group: "通票分账", key: "combo_discount_bearer", label: "套票折扣归属", value: "platform_absorb" },
  // V340(P0-D):提成/积分出厂默认 = 改造前写死值(销售 1500bp / 每积 1 分需 100 分)。
  { group: "提成与积分", key: "commission_rate_sales", label: "卡座提成率·销售人员(万分比)", value: "1500" },
  { group: "提成与积分", key: "commission_rate_non_sales", label: "卡座提成率·非销售人员(万分比)", value: "1000" },
  { group: "提成与积分", key: "points_amount_cent_per_point", label: "积分累计率(每积1分所需消费·分)", value: "100" },
];
const OVERVIEW_NO_DRAFT = { platformDefaults: DEFAULTS, pendingDraftCount: 0, pendingDrafts: [] };
const OVERVIEW_WITH_DRAFTS = {
  platformDefaults: DEFAULTS,
  pendingDraftCount: 2,
  pendingDrafts: [
    { id: "tdmr_d_1", configGroup: "merchant_rate", summary: "project=__DEFAULT__ 商户=m1 每客/固定=500/-" },
    { id: "trt_d_1", configGroup: "rank_tiers", summary: "project=proj_9 排名档位草案(2 档)" },
  ],
};
const OVERVIEW_WITH_COMMISSION_DRAFT = {
  platformDefaults: DEFAULTS,
  pendingDraftCount: 1,
  pendingDrafts: [
    { id: "bcrc_d_1", configGroup: "commission_rate", summary: "project=__DEFAULT__ 商户=m1 角色=sales 提成 2000bps" },
  ],
};

function makeApi(
  get = vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT),
  post = vi.fn().mockResolvedValue({}),
): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  };
  render(<SettlementConfigPage {...props} />);
  return { setGlobalError };
}

afterEach(() => vi.restoreAllMocks());

describe("SettlementConfigPage 结算配置中心(ADM-STL-001)", () => {
  it("概览默认展示当前生效默认值 + 无草案空态", async () => {
    const get = vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT);
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/settlement/config/overview"));
    expect(await screen.findByTestId("settlement-config-page")).toBeTruthy();
    expect(screen.getByTestId("settlement-config-default-combo_discount_bearer").textContent).toContain("platform_absorb");
    expect(screen.getByTestId("settlement-config-drafts-empty")).toBeTruthy();
  });

  it("通票分账栏目渲染 7 个配置块(留存卡片已并入综合服务费)", async () => {
    renderPage(makeApi());
    await screen.findByTestId("settlement-config-ticket-distribution");
    for (const key of [
      "distribution_method", "rank_tiers", "merchant_rate",
      "comprehensive_service_fee", "combo_discount", "channel_fee", "revenue_stream_attribution",
    ]) {
      expect(screen.getByTestId(`settlement-config-block-${key}`)).toBeTruthy();
    }
    // S4:原「项目公司留存比例」卡片不复存在——与门票综合服务费为同一项、只扣一次。
    expect(screen.queryByTestId("settlement-config-block-retention")).toBeNull();
  });

  it("「改」套票折扣归属 → GuidedFlowModal 填写 → POST 落草案端点带 idempotency_key", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT), post));
    fireEvent.click(await screen.findByTestId("settlement-config-edit-combo_discount"));
    fireEvent.click(screen.getByTestId("settlement-config-guided-next")); // 填写 → 确认
    fireEvent.click(screen.getByTestId("settlement-config-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/combo-discount",
      expect.objectContaining({ discountBearer: "platform_absorb", idempotencyKey: expect.any(String) }),
    ));
  });

  it("草案队列:merchant_rate 转正走泛化端点、下线走 archive 端点(带原因)", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_WITH_DRAFTS), post));
    fireEvent.click(await screen.findByTestId("settlement-config-activate-tdmr_d_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/settlement/config/merchant_rate/tdmr_d_1/activate"));

    vi.spyOn(window, "prompt").mockReturnValue("重复配置");
    fireEvent.click(screen.getByTestId("settlement-config-archive-tdmr_d_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/merchant_rate/tdmr_d_1/archive",
      expect.objectContaining({ archiveReason: "重复配置", idempotencyKey: expect.any(String) }),
    ));
  });

  it("仅营业额租金草案提示初始日期与变更次月生效规则", async () => {
    renderPage(makeApi(vi.fn().mockResolvedValue({
      ...OVERVIEW_WITH_DRAFTS,
      pendingDraftCount: 3,
      pendingDrafts: [
        ...OVERVIEW_WITH_DRAFTS.pendingDrafts,
        { id: "turnover_d_1", configGroup: "turnover_rent_rule", summary: "营业额租金草案" },
      ],
    })));
    const rentDraft = await screen.findByTestId("settlement-config-draft-turnover_d_1");
    expect(rentDraft.textContent).toContain("初始规则按填写日期生效");
    expect(rentDraft.textContent).toContain("已有规则变更统一按实际批准次月1日生效");
    expect(rentDraft.textContent).toContain("手填起日不决定变更生效");
    expect(screen.getByTestId("settlement-config-draft-tdmr_d_1").textContent).not.toContain("次月1日");
    expect(screen.getByTestId("settlement-config-draft-trt_d_1").textContent).not.toContain("次月1日");
  });

  it("草案队列:rank_tiers 转正走 /rank-tiers/activate 并从 summary 解析 project", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_WITH_DRAFTS), post));
    fireEvent.click(await screen.findByTestId("settlement-config-activate-trt_d_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/rank-tiers/activate",
      { projectId: "proj_9" },
    ));
    // rank_tiers 不在软删白名单 → 无「下线」按钮
    expect(screen.queryByTestId("settlement-config-archive-trt_d_1")).toBeNull();
  });

  // V340(P0-D/CFG-01):提成率/积分累计率原写死在代码里,现进本页「提成与积分」栏目,同走落草案 + 差人复核转正。
  it("提成与积分栏目渲染 2 个配置块 + 概览展示其当前生效值", async () => {
    renderPage(makeApi());
    await screen.findByTestId("settlement-config-commission-points");
    expect(screen.getByTestId("settlement-config-block-commission_rate")).toBeTruthy();
    expect(screen.getByTestId("settlement-config-block-points_accrual_rate")).toBeTruthy();
    expect(screen.getByTestId("settlement-config-default-commission_rate_sales").textContent).toContain("1500");
    expect(screen.getByTestId("settlement-config-default-points_amount_cent_per_point").textContent).toContain("100");
  });

  it("「改」卡座提成费率 → 按项目×商户×角色档 POST 落草案端点", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT), post));
    fireEvent.click(await screen.findByTestId("settlement-config-edit-commission_rate"));
    fireEvent.change(screen.getByTestId("settlement-config-field-merchantId"), { target: { value: "m_bar_1" } });
    fireEvent.change(screen.getByTestId("settlement-config-field-roleType"), { target: { value: "non_sales" } });
    fireEvent.change(screen.getByTestId("settlement-config-field-rateBp"), { target: { value: "1200" } });
    fireEvent.click(screen.getByTestId("settlement-config-guided-next"));
    fireEvent.click(screen.getByTestId("settlement-config-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/commission-rate",
      expect.objectContaining({
        merchantId: "m_bar_1", roleType: "non_sales", rateBp: 1200, idempotencyKey: expect.any(String),
      }),
    ));
  });

  it("「改」积分累计率 → POST 落草案端点；累计单位须大于 0", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT), post));
    fireEvent.click(await screen.findByTestId("settlement-config-edit-points_accrual_rate"));
    // 0 非法 → 不放行进确认步
    fireEvent.change(screen.getByTestId("settlement-config-field-amountCentPerPoint"), { target: { value: "0" } });
    fireEvent.click(screen.getByTestId("settlement-config-guided-next"));
    expect(screen.getByTestId("settlement-config-field-amountCentPerPoint")).toBeTruthy(); // 仍停在填写步
    fireEvent.change(screen.getByTestId("settlement-config-field-amountCentPerPoint"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("settlement-config-guided-next"));
    fireEvent.click(screen.getByTestId("settlement-config-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/points-accrual-rate",
      expect.objectContaining({ amountCentPerPoint: 50, idempotencyKey: expect.any(String) }),
    ));
  });

  it("草案队列:commission_rate 草案可复核转正、可下线(商户特例回落默认)", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_WITH_COMMISSION_DRAFT), post));
    fireEvent.click(await screen.findByTestId("settlement-config-activate-bcrc_d_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/commission_rate/bcrc_d_1/activate"));

    vi.spyOn(window, "prompt").mockReturnValue("特例作废");
    fireEvent.click(screen.getByTestId("settlement-config-archive-bcrc_d_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/commission_rate/bcrc_d_1/archive",
      expect.objectContaining({ archiveReason: "特例作废", idempotencyKey: expect.any(String) }),
    ));
  });

  it("差人转正被拒(SELF_ACTIVATION_FORBIDDEN)→ 友好中文提示", async () => {
    const err = new ApiError("当前账号无权进行此操作", {
      detail: { path: "x", status: 403, requestId: null, raw: "" },
      code: "FORBIDDEN",
      detailCode: "SELF_ACTIVATION_FORBIDDEN",
    });
    const post = vi.fn().mockRejectedValue(err);
    const { setGlobalError } = renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_WITH_DRAFTS), post));
    fireEvent.click(await screen.findByTestId("settlement-config-activate-tdmr_d_1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("另一位")));
  });
});

// FS-021-R3（BSM L1673「三个付账日……各笔普通内部结算应付只在配置的付账日中选定」；BSM L1683「每一笔应付具体落在哪一个付账日，
// 由财务人员按其工作负荷统一制定规则，规则一经确定至少一年内不得变更」）：付账日与钱路指派。走真实 AdminApi（键名蛇形转换）。
describe("SettlementConfigPage 付账日与钱路指派", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("提交钱路指派规则请求体为蛇形 {route, statement_day, pay_slot, effective_from}；待审规则可审批；补偿应付钱路可选", async () => {
    const calls: { url: string; method: string; body: Record<string, unknown> | null }[] = [];
    const ok = (data: unknown) => new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) as Record<string, unknown> : null });
      if (method === "POST") return Promise.resolve(ok({ id: "ipdr-new", status: "pending" }));
      if (url.includes("/internal-pay-days/rules")) {
        return Promise.resolve(ok([{ id: "ipdr-1", kind: "route", route: "ticket_distribution", statement_day: 3, pay_slot: 3,
          effective_from: "2027-01-01", status: "pending", reason: "按工作负荷", submitted_by: "u2" }]));
      }
      if (url.includes("/overview")) return Promise.resolve(ok({ platform_defaults: [], pending_draft_count: 0, pending_drafts: [] }));
      return Promise.resolve(ok([]));
    }));
    const { AdminApi } = await import("../client/adminClient");
    render(<SettlementConfigPage api={new AdminApi("test-token")} user={{ id: "u1", username: "finance", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);

    await waitFor(() => expect(screen.queryByTestId("internal-pay-day-rule-ipdr-1")).not.toBeNull());
    expect(Array.from((screen.getByTestId("internal-pay-day-route") as HTMLSelectElement).options).map((o) => o.textContent)).toContain("补偿新增履约结算");
    fireEvent.change(screen.getByTestId("internal-pay-day-route"), { target: { value: "ticket_distribution" } });
    fireEvent.change(screen.getByTestId("internal-pay-day-statement-day"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("internal-pay-day-slot"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("internal-pay-day-effective-from"), { target: { value: "2027-06-01" } });
    fireEvent.change(screen.getByTestId("internal-pay-day-reason"), { target: { value: "按工作负荷统一制定" } });
    fireEvent.click(screen.getByTestId("internal-pay-day-submit"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.endsWith("/internal-pay-days/rules"))).toBe(true));
    const body = calls.find((c) => c.method === "POST" && c.url.endsWith("/internal-pay-days/rules"))!.body!;
    expect(body).toMatchObject({ kind: "route", route: "ticket_distribution", statement_day: 3, pay_slot: 3,
      effective_from: "2027-06-01", reason: "按工作负荷统一制定" });
    expect(typeof body.idempotency_key).toBe("string");

    fireEvent.click(screen.getByTestId("internal-pay-day-approve-ipdr-1"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.endsWith("/rules/ipdr-1/decision"))).toBe(true));
    expect(calls.find((c) => c.url.endsWith("/rules/ipdr-1/decision"))!.body).toMatchObject({ decision: "approve" });
  });
});
