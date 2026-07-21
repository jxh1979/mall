import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { SettlementConfigPage } from "./SettlementConfigPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 结算配置中心总控页(ADM-STL-001)组件测试:概览读回 + 默认值展示 + 草案复核队列/转正交互。
const OVERVIEW_NO_DRAFT = {
  platformDefaults: [
    { group: "通票分账", key: "platform_retention_bps", label: "项目公司留存比例(万分比)", value: "2000" },
    { group: "赠酒", key: "giveaway_cost_per_cup_cent", label: "每杯赠酒成本补偿(分)", value: "300" },
  ],
  pendingDraftCount: 0,
  pendingDrafts: [],
};
const OVERVIEW_WITH_DRAFT = {
  platformDefaults: OVERVIEW_NO_DRAFT.platformDefaults,
  pendingDraftCount: 1,
  pendingDrafts: [{ id: "mrar_1", configGroup: "revenue_attribution", summary: "bar / default · gross · 10000bps" }],
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
function renderPage(api: AdminApiClient, setGlobalError = vi.fn(), setActionResult = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  };
  render(<SettlementConfigPage {...props} />);
}

describe("SettlementConfigPage 结算配置中心(ADM-STL-001)", () => {
  it("读回概览:展示当前生效默认值 + 无草案空态", async () => {
    const get = vi.fn().mockResolvedValue(OVERVIEW_NO_DRAFT);
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/settlement/config/overview"));
    expect(await screen.findByTestId("settlement-config-page")).toBeTruthy();
    expect(screen.getByTestId("settlement-config-defaults-table")).toBeTruthy();
    expect(screen.getByTestId("settlement-config-default-platform_retention_bps").textContent).toContain("2000");
    expect(screen.getByTestId("settlement-config-drafts-empty")).toBeTruthy();
  });

  it("有草案:展示复核队列,点「复核转正」走 POST activate 端点", async () => {
    const post = vi.fn().mockResolvedValue({});
    const get = vi.fn().mockResolvedValue(OVERVIEW_WITH_DRAFT);
    renderPage(makeApi(get, post));
    const btn = await screen.findByTestId("settlement-config-activate-mrar_1");
    expect(screen.getByTestId("settlement-config-drafts-table")).toBeTruthy();
    fireEvent.click(btn);
    await waitFor(() =>
      expect(post).toHaveBeenCalledWith("/api/v1/settlement/config/attribution-rules/mrar_1/activate"),
    );
  });
});
