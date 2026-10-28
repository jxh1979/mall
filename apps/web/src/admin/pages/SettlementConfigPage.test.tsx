// 文件版本：V0.04
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

  it("通票分账栏目渲染 6 个配置块 + 通票分账方案(留存卡片已并入综合服务费)", async () => {
    renderPage(makeApi());
    await screen.findByTestId("settlement-config-ticket-distribution");
    for (const key of [
      "distribution_method", "comprehensive_service_fee", "pass_gift_cost", "combo_discount", "channel_fee",
      "revenue_stream_attribution",
    ]) {
      expect(screen.getByTestId(`settlement-config-block-${key}`)).toBeTruthy();
    }
    // S4:原「项目公司留存比例」卡片不复存在——与门票综合服务费为同一项、只扣一次。
    expect(screen.queryByTestId("settlement-config-block-retention")).toBeNull();
    // 第十八批第 2 条:旧「排名档位梯子」「商户每客单价 / 固定额」两块合并为通票分账方案(参与家数档位表＋第一段名单)。
    expect(screen.queryByTestId("settlement-config-block-rank_tiers")).toBeNull();
    expect(screen.queryByTestId("settlement-config-block-merchant_rate")).toBeNull();
    expect(screen.getByTestId("distribution-scheme-panel")).toBeTruthy();
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

  // 第十八批第 3 条（BSM 第 749 行「渠道费用分两层、性质不同不可混用……官方渠道售出的票不扣销售渠道费，但仍扣通路费」）。
  it("「改」渠道费用：通路费按支付通路只配比例，销售渠道费才有每张票固定金额；POST 带费用层与渠道码", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT), post));
    await waitFor(() => expect(screen.queryByTestId("settlement-config-edit-channel_fee")).not.toBeNull());
    fireEvent.click(screen.getByTestId("settlement-config-edit-channel_fee"));
    await waitFor(() => expect(screen.queryByTestId("settlement-config-field-feeLayer")).not.toBeNull());

    fireEvent.change(screen.getByTestId("settlement-config-field-feeLayer"), { target: { value: "sales" } });
    expect(screen.getByTestId("settlement-config-field-fixedAmountCent")).toBeTruthy();
    fireEvent.change(screen.getByTestId("settlement-config-field-feeLayer"), { target: { value: "route" } });
    expect(screen.queryByTestId("settlement-config-field-fixedAmountCent")).toBeNull();

    fireEvent.change(screen.getByTestId("settlement-config-field-channel"), { target: { value: "wechat" } });
    fireEvent.change(screen.getByTestId("settlement-config-field-feeBps"), { target: { value: "60" } });
    fireEvent.click(screen.getByTestId("settlement-config-guided-next"));
    fireEvent.click(screen.getByTestId("settlement-config-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/channel-fee",
      expect.objectContaining({ feeLayer: "route", channel: "wechat", feeBps: 60, fixedAmountCent: null, idempotencyKey: expect.any(String) }),
    ));
  });

  // 18B 复核后裁决第 11 条「对，下线两个选项（推荐）」（ENUM 第 52 行「预扣数与日后实付数的差额也留在娱乐平台公司」）。
  it("「改」赠送权益预估成本：差额固定显示「差额留娱乐平台公司」，无结转下期／回补当期池选项，POST 不带差额规则", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT), post));
    await waitFor(() => expect(screen.queryByTestId("settlement-config-edit-pass_gift_cost")).not.toBeNull());
    fireEvent.click(screen.getByTestId("settlement-config-edit-pass_gift_cost"));
    await waitFor(() => expect(screen.queryByTestId("settlement-config-variance-fixed")?.textContent ?? "").toContain("差额留娱乐平台公司"));
    expect(screen.queryByTestId("settlement-config-field-varianceHandling")).toBeNull();
    expect(document.body.textContent).not.toContain("结转下期");
    expect(document.body.textContent).not.toContain("回补当期");

    fireEvent.change(screen.getByTestId("settlement-config-field-ticketGiftAmountYuan"), { target: { value: "59" } });
    fireEvent.click(screen.getByTestId("settlement-config-guided-next"));
    fireEvent.click(screen.getByTestId("settlement-config-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/pass-gift-cost",
      expect.objectContaining({ predeductMode: "by_ticket_amount", ticketGiftAmountCent: 5900, idempotencyKey: expect.any(String) }),
    ));
    expect(post.mock.calls[0]?.[1]).not.toHaveProperty("varianceHandling");
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

// 第十八批第 2 条（ENUM 第 52 行「按本张票的第二段参与家数查「参与家数档位表」……每一行的百分比合计必须等于 100%，合计不等于 100% 的配置
// 不得生效……档位表变更须提前向受影响的酒吧商户发出通告……通告期为业务参数，后台可配，出厂默认一个月，须同时配置上下限」；第一段名单与金额
// 同一套通告—生效—版本机制）：通票分账方案编辑器、版本列表与复核转正、通告期、「补配该家数档位行」待办。
const SCHEME = {
  noticePeriod: { periodValue: 1, periodUnit: "month", minValue: null, maxValue: null },
  versions: [
    {
      id: "tdsv_cur", projectId: null, status: "notified", state: "effective", draftedBy: "u3", approvedBy: "u4",
      notifiedAt: "2026-09-01T02:00:00Z", effectiveAt: "2026-10-01T00:00:00Z",
      tiers: [
        { participantCount: 1, rankNo: 1, shareBps: 10000, minBps: 9000, maxBps: 10000 },
        { participantCount: 3, rankNo: 1, shareBps: 4000, minBps: 3000, maxBps: 5000 },
        { participantCount: 3, rankNo: 2, shareBps: 3000, minBps: 2000, maxBps: 4000 },
        { participantCount: 3, rankNo: 3, shareBps: 3000, minBps: 2000, maxBps: 4000 },
      ],
      firstSegment: [],
    },
    {
      id: "tdsv_d1", projectId: null, status: "draft", state: "draft", draftedBy: "u2", approvedBy: null,
      notifiedAt: null, effectiveAt: null,
      tiers: [{ participantCount: 1, rankNo: 1, shareBps: 10000, minBps: 9000, maxBps: 10000 }],
      firstSegment: [{ merchantId: "m_bar_1", ruleType: "fixed", perGuestCent: null, fixedAmountCent: 1000 }],
    },
  ],
  missingRows: [{ projectId: null, participantCount: 11, units: 1, amountCent: 9000 }],
};

describe("SettlementConfigPage 通票分账方案（参与家数档位表＋第一段名单）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("按家数分行显示行合计，改一格使合计≠100% 后禁止保存；缺行暂挂待办可见", async () => {
    const get = vi.fn().mockImplementation(async (url: string) => (url.endsWith("/distribution-scheme") ? SCHEME : OVERVIEW_NO_DRAFT));
    renderPage(makeApi(get));
    await waitFor(() => expect(screen.queryByTestId("distribution-scheme-total-3")?.textContent ?? "").toContain("100%"));
    expect((screen.getByTestId("distribution-scheme-save") as HTMLButtonElement).disabled).toBe(false);

    fireEvent.change(screen.getByTestId("distribution-scheme-share-3-3"), { target: { value: "29" } });

    expect(screen.getByTestId("distribution-scheme-total-3").textContent).toContain("99%");
    expect((screen.getByTestId("distribution-scheme-save") as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByTestId("distribution-scheme-missing-rows").textContent).toContain("11 家");
    expect(screen.getByTestId("distribution-scheme-version-tdsv_d1").textContent).toContain("草案");
  });

  it("保存草案、复核转正与通告期请求体为蛇形键（tiers/first_segment/period_value…）", async () => {
    const calls: { url: string; method: string; body: Record<string, unknown> | null }[] = [];
    const ok = (data: unknown) => new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } });
    const snake = {
      notice_period: { period_value: 1, period_unit: "month", min_value: null, max_value: null },
      versions: SCHEME.versions.map((v) => ({
        id: v.id, project_id: v.projectId, status: v.status, state: v.state, drafted_by: v.draftedBy, approved_by: v.approvedBy,
        notified_at: v.notifiedAt, effective_at: v.effectiveAt,
        tiers: v.tiers.map((t) => ({ participant_count: t.participantCount, rank_no: t.rankNo, share_bps: t.shareBps,
          min_bps: t.minBps, max_bps: t.maxBps })),
        first_segment: v.firstSegment.map((f) => ({ merchant_id: f.merchantId, rule_type: f.ruleType,
          per_guest_cent: f.perGuestCent, fixed_amount_cent: f.fixedAmountCent })),
      })),
      missing_rows: [],
    };
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) as Record<string, unknown> : null });
      if (method === "POST") return Promise.resolve(ok("tdsv_new"));
      if (url.endsWith("/distribution-scheme")) return Promise.resolve(ok(snake));
      if (url.includes("/overview")) return Promise.resolve(ok({ platform_defaults: [], pending_draft_count: 0, pending_drafts: [] }));
      return Promise.resolve(ok([]));
    }));
    const { AdminApi } = await import("../client/adminClient");
    render(<SettlementConfigPage api={new AdminApi("test-token")} user={{ id: "u1", username: "finance", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);

    await waitFor(() => expect(screen.queryByTestId("distribution-scheme-total-1")?.textContent ?? "").toContain("100%"));
    fireEvent.click(screen.getByTestId("distribution-scheme-add-first"));
    fireEvent.change(screen.getByTestId("distribution-scheme-first-merchant-0"), { target: { value: "m_bar_9" } });
    fireEvent.change(screen.getByTestId("distribution-scheme-first-amount-0"), { target: { value: "12.5" } });
    fireEvent.click(screen.getByTestId("distribution-scheme-save"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.endsWith("/settlement/config/distribution-scheme"))).toBe(true));
    const draft = calls.find((c) => c.method === "POST" && c.url.endsWith("/settlement/config/distribution-scheme"))!.body!;
    expect(draft.tiers).toContainEqual({ participant_count: 3, rank_no: 1, share_bps: 4000, min_bps: 3000, max_bps: 5000 });
    expect(draft.first_segment).toEqual([{ merchant_id: "m_bar_9", fixed_amount_cent: 1250 }]);
    expect(typeof draft.idempotency_key).toBe("string");

    fireEvent.click(screen.getByTestId("distribution-scheme-activate-tdsv_d1"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.endsWith("/distribution-scheme/tdsv_d1/activate"))).toBe(true));

    fireEvent.change(screen.getByTestId("distribution-scheme-notice-value"), { target: { value: "10" } });
    fireEvent.change(screen.getByTestId("distribution-scheme-notice-unit"), { target: { value: "day" } });
    fireEvent.change(screen.getByTestId("distribution-scheme-notice-min"), { target: { value: "7" } });
    fireEvent.change(screen.getByTestId("distribution-scheme-notice-max"), { target: { value: "30" } });
    fireEvent.click(screen.getByTestId("distribution-scheme-notice-save"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.endsWith("/distribution-scheme/notice-period"))).toBe(true));
    expect(calls.find((c) => c.url.endsWith("/distribution-scheme/notice-period"))!.body)
      .toMatchObject({ period_value: 10, period_unit: "day", min_value: 7, max_value: 30 });
  });
});

// 第 75 条（ENUM 第 52 行「第一段的固定金额或固定单价在配置生效之前，必须先按该票种……最低可能可分配池金额试算……试算不通过的配置无效、
// 不得生效……娱乐平台公司确需让第一段合计高于池金额的，须在配置生效前提交短期经营激励申请并经项目之上的平台权限审批……初始有效期不超过
// 后台配置的上限（业务参数，按项目可配，出厂默认三个月，须同时配置上下限）、到期重审」）。走真实 AdminApi（键名蛇形转换）的用例 stub fetch。
describe("SettlementConfigPage 短期经营激励（第 75 条）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });
  const ok = (data: unknown) => new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
  const snakeDraft = {
    notice_period: { period_value: 1, period_unit: "month", min_value: 1, max_value: 3 },
    versions: [{ id: "tdsv_d9", project_id: "p1", status: "draft", state: "draft", drafted_by: "u2", approved_by: null,
      notified_at: null, effective_at: null,
      tiers: [{ participant_count: 1, rank_no: 1, share_bps: 10000, min_bps: 9000, max_bps: 10000 }],
      first_segment: [{ merchant_id: "m_bar_1", rule_type: "fixed", per_guest_cent: null, fixed_amount_cent: 5000 }] }],
    missing_rows: [],
    incentive_applications: [],
  };

  it("「改」分配方式：短期经营激励有效期上限须同时填下限与上限，缺一不放行；齐全时 POST 蛇形体带上下限", async () => {
    const calls: { url: string; method: string; body: Record<string, unknown> | null }[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) as Record<string, unknown> : null });
      if (method === "POST") return Promise.resolve(ok(null));
      if (url.endsWith("/distribution-scheme")) return Promise.resolve(ok(snakeDraft));
      if (url.includes("/overview")) return Promise.resolve(ok({ platform_defaults: [], pending_draft_count: 0, pending_drafts: [] }));
      return Promise.resolve(ok([]));
    }));
    const { AdminApi } = await import("../client/adminClient");
    render(<SettlementConfigPage api={new AdminApi("test-token")} user={{ id: "u1", username: "finance", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);

    await waitFor(() => expect(screen.queryByTestId("settlement-config-edit-distribution_method")).not.toBeNull());
    fireEvent.click(screen.getByTestId("settlement-config-edit-distribution_method"));
    await waitFor(() => expect(screen.queryByTestId("settlement-config-field-specialApprovalTermMinValue")).not.toBeNull());
    fireEvent.change(screen.getByTestId("settlement-config-field-specialApprovalTermValue"), { target: { value: "2" } });
    fireEvent.change(screen.getByTestId("settlement-config-field-specialApprovalTermMaxValue"), { target: { value: "6" } });
    fireEvent.click(screen.getByTestId("settlement-config-guided-next"));
    expect(screen.getByTestId("settlement-config-field-specialApprovalTermMinValue")).toBeTruthy(); // 缺下限：仍停在填写步
    expect(calls.some((c) => c.method === "POST")).toBe(false);

    fireEvent.change(screen.getByTestId("settlement-config-field-specialApprovalTermMinValue"), { target: { value: "1" } });
    fireEvent.click(screen.getByTestId("settlement-config-guided-next"));
    fireEvent.click(screen.getByTestId("settlement-config-submit"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.endsWith("/settlement/config/distribution-method"))).toBe(true));
    expect(calls.find((c) => c.url.endsWith("/settlement/config/distribution-method"))!.body).toMatchObject({
      special_approval_term_value: 2, special_approval_term_unit: "month",
      special_approval_term_min_value: 1, special_approval_term_max_value: 6,
    });
  });

  it("转正报试算不通过后出现短期经营激励申请入口，提交 POST 体为蛇形 {idempotency_key, reason, term_value, term_unit}", async () => {
    const calls: { url: string; method: string; body: Record<string, unknown> | null }[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      calls.push({ url, method, body: typeof init?.body === "string" ? JSON.parse(init.body) as Record<string, unknown> : null });
      if (method === "POST" && url.endsWith("/activate")) {
        return Promise.resolve(new Response(JSON.stringify({ success: false, code: "STATE_CONFLICT", detail_code: "POOL_TRIAL_FAILED",
          message: "第一段试算不通过，配置不得生效：票种「试算通票」：最低实付价 75.00 元，最不利一份可分配池 44.73 元，第一段应分合计 50.00 元，超出 5.27 元" }),
        { status: 409, headers: { "content-type": "application/json" } }));
      }
      if (method === "POST") return Promise.resolve(ok({ id: "pia_1", status: "pending" }));
      if (url.endsWith("/distribution-scheme")) return Promise.resolve(ok(snakeDraft));
      if (url.includes("/overview")) return Promise.resolve(ok({ platform_defaults: [], pending_draft_count: 0, pending_drafts: [] }));
      return Promise.resolve(ok([]));
    }));
    const { AdminApi } = await import("../client/adminClient");
    render(<SettlementConfigPage api={new AdminApi("test-token")} user={{ id: "u1", username: "finance", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);

    await waitFor(() => expect(screen.queryByTestId("distribution-scheme-activate-tdsv_d9")).not.toBeNull());
    expect(screen.queryByTestId("distribution-scheme-incentive-tdsv_d9")).toBeNull();
    fireEvent.click(screen.getByTestId("distribution-scheme-activate-tdsv_d9"));
    await waitFor(() => expect(screen.queryByTestId("distribution-scheme-incentive-tdsv_d9")).not.toBeNull());
    expect(screen.getByTestId("distribution-scheme-error").textContent).toContain("超出 5.27 元");

    fireEvent.click(screen.getByTestId("distribution-scheme-incentive-tdsv_d9"));
    fireEvent.change(screen.getByTestId("distribution-scheme-incentive-reason"), { target: { value: "开业季保底" } });
    fireEvent.change(screen.getByTestId("distribution-scheme-incentive-term"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("distribution-scheme-incentive-submit"));
    await waitFor(() => expect(calls.some((c) => c.method === "POST" && c.url.endsWith("/distribution-scheme/tdsv_d9/incentive-applications"))).toBe(true));
    const body = calls.find((c) => c.url.endsWith("/distribution-scheme/tdsv_d9/incentive-applications"))!.body!;
    expect(body).toMatchObject({ reason: "开业季保底", term_value: 2, term_unit: "month" });
    expect(typeof body.idempotency_key).toBe("string");
  });

  const SCHEME_WITH_APPLICATION = {
    noticePeriod: { periodValue: 1, periodUnit: "month", minValue: 1, maxValue: 3 },
    versions: [{ id: "tdsv_d9", projectId: "p1", status: "draft", state: "draft", draftedBy: "u2", approvedBy: null,
      notifiedAt: null, effectiveAt: null, tiers: [{ participantCount: 1, rankNo: 1, shareBps: 10000, minBps: 9000, maxBps: 10000 }],
      firstSegment: [] }],
    missingRows: [],
    incentiveApplications: [{ id: "pia_1", schemeVersionId: "tdsv_d9", kind: "initial", status: "pending", reason: "开业季保底",
      termValue: 2, termUnit: "month", maxExcessCent: 527,
      trialResult: [{ ticketTypeId: "tt1", ticketName: "试算通票", lowestPriceCent: 7500, lowestUnitPoolCent: 4473, firstSegmentCent: 5000, excessCent: 527 }],
      submittedBy: "u2", submittedAt: "2026-10-27T02:00:00Z", decidedBy: null, decidedAt: null, decisionNote: null, validFrom: null, validUntil: null }],
  };

  it("待审申请：没有审批权限的人看不到「批准」", async () => {
    const get = vi.fn().mockImplementation(async (url: string) => (url.endsWith("/distribution-scheme") ? SCHEME_WITH_APPLICATION : OVERVIEW_NO_DRAFT));
    render(<SettlementConfigPage api={makeApi(get)} user={{ id: "u1", username: "finance", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()}
      effectivePermissions={{ status: "ready", value: { grants: ["settlement.config.view", "settlement.config.manage"], denies: [], wildcard: false } }} />);

    await waitFor(() => expect(screen.queryByTestId("distribution-scheme-incentive-row-pia_1")).not.toBeNull());
    expect(screen.getByTestId("distribution-scheme-incentive-row-pia_1").textContent).toContain("超出 5.27 元");
    expect(screen.queryByTestId("distribution-scheme-incentive-approve-pia_1")).toBeNull();
    expect(screen.getByTestId("distribution-scheme-incentive-row-pia_1").textContent).toContain("待平台审批");
  });

  it("待审申请：有审批权限的平台负责人可批准，POST 批准端点带幂等键与意见", async () => {
    const get = vi.fn().mockImplementation(async (url: string) => (url.endsWith("/distribution-scheme") ? SCHEME_WITH_APPLICATION : OVERVIEW_NO_DRAFT));
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(get, post));

    await waitFor(() => expect(screen.queryByTestId("distribution-scheme-incentive-approve-pia_1")).not.toBeNull());
    fireEvent.change(screen.getByTestId("distribution-scheme-incentive-note-pia_1"), { target: { value: "同意" } });
    fireEvent.click(screen.getByTestId("distribution-scheme-incentive-approve-pia_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/settlement/config/incentive-applications/pia_1/approve",
      expect.objectContaining({ idempotencyKey: expect.any(String), note: "同意" })));
  });

  // 18C 核对后操作员裁决补充第 12、13 条：激励到期未重新获批，到期那天起新卖的票改按此前在用的方案；已到期的版本不再受理重审。
  it("激励到期的版本显示「激励到期，已改回此前方案」，不再给「提交重审」", async () => {
    const expired = {
      ...SCHEME_WITH_APPLICATION,
      versions: [{ id: "tdsv_e1", projectId: "p1", status: "notified", state: "incentive_expired", draftedBy: "u2", approvedBy: "u3",
        notifiedAt: "2026-06-01T02:00:00Z", effectiveAt: "2026-07-01T02:00:00Z",
        tiers: [{ participantCount: 1, rankNo: 1, shareBps: 10000, minBps: 9000, maxBps: 10000 }], firstSegment: [] }],
      incentiveApplications: [{ ...SCHEME_WITH_APPLICATION.incentiveApplications[0], id: "pia_e1", schemeVersionId: "tdsv_e1",
        status: "approved", decidedBy: "u3", decidedAt: "2026-06-01T01:00:00Z",
        validFrom: "2026-07-01T02:00:00Z", validUntil: "2026-10-01T02:00:00Z" }],
    };
    const get = vi.fn().mockImplementation(async (url: string) => (url.endsWith("/distribution-scheme") ? expired : OVERVIEW_NO_DRAFT));
    renderPage(makeApi(get));

    await waitFor(() => expect(screen.queryByTestId("distribution-scheme-version-tdsv_e1")?.textContent ?? "").toContain("激励到期，已改回此前方案"));
    expect(screen.queryByTestId("distribution-scheme-incentive-tdsv_e1")).toBeNull();
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
