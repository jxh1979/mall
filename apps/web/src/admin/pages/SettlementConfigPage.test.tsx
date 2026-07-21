import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { SettlementConfigPage } from "./SettlementConfigPage";
import { ApiError } from "../../shared/apiError";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 结算配置中心总控页(ADM-STL-001)组件测试:概览默认值 + 通票分账 8 块「改」落草案 + 草案队列转正/下线路由 + 差人友好提示。
const DEFAULTS = [
  { group: "通票分账", key: "platform_retention_bps", label: "项目公司留存比例(万分比)", value: "2000" },
  { group: "通票分账", key: "combo_discount_bearer", label: "套票折扣归属", value: "platform_absorb" },
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
    expect(screen.getByTestId("settlement-config-default-platform_retention_bps").textContent).toContain("2000");
    expect(screen.getByTestId("settlement-config-drafts-empty")).toBeTruthy();
  });

  it("通票分账栏目渲染 8 个配置块", async () => {
    renderPage(makeApi());
    await screen.findByTestId("settlement-config-ticket-distribution");
    for (const key of [
      "retention", "distribution_method", "rank_tiers", "merchant_rate",
      "comprehensive_service_fee", "combo_discount", "channel_fee", "revenue_stream_attribution",
    ]) {
      expect(screen.getByTestId(`settlement-config-block-${key}`)).toBeTruthy();
    }
  });

  it("「改」留存比例 → GuidedFlowModal 填写 → POST 落草案端点带 idempotency_key", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT), post));
    fireEvent.click(await screen.findByTestId("settlement-config-edit-retention"));
    fireEvent.change(screen.getByTestId("settlement-config-field-retentionBps"), { target: { value: "2500" } });
    fireEvent.click(screen.getByTestId("settlement-config-guided-next")); // 填写 → 确认
    fireEvent.click(screen.getByTestId("settlement-config-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/retention",
      expect.objectContaining({ retentionBps: 2500, idempotencyKey: expect.any(String) }),
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
