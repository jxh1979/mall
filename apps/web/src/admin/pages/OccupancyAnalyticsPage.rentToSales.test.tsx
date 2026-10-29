import { render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OccupancyAnalyticsPage } from "./OccupancyAnalyticsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const DASHBOARD = {
  business_date: "2099-09-15",
  metrics: {
    "occupancy.rate": { value: 0.82 },
    "occupancy.rentable_area": { value: 10000 },
    "occupancy.leased_area": { value: 8200 },
    "occupancy.signed_pending_area": { value: 300 },
    "occupancy.parking_rate": { value: 0.65 },
    "contract.expiring_90d": { value: 2 },
  },
};

// P1c 租售比响应(snake_case,客户端深驼峰化):店级含 null 分母行、业态级、平台级、未归属营业额。
const R2S = {
  period: "2099-09",
  by_merchant: [
    { key: "m_a", label: "餐饮A", rent_cent: 120000, sales_cent: 500000, ratio_bps: 2400, note: null },
    { key: "m_c", label: "零售C", rent_cent: 30000, sales_cent: 0, ratio_bps: null, note: "无第三方营业额归属，租售比不可计（仅计入平台级分母）" },
  ],
  by_merchant_type: [
    { key: "catering", label: "catering", rent_cent: 170000, sales_cent: 700000, ratio_bps: 2429, note: null },
    { key: "retail", label: "retail", rent_cent: 30000, sales_cent: 0, ratio_bps: null, note: "无第三方营业额归属，租售比不可计（仅计入平台级分母）" },
  ],
  platform: { key: "__platform__", label: "平台合计", rent_cent: 200000, sales_cent: 950000, ratio_bps: 2105, note: null },
  unattributed_sales_cent: 250000,
  note: "含未归属第三方营业额（无门店号 / 未维护渠道映射）：仅计入平台级分母，不参与任何店级或业态抽成",
};

function dataEnvelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 200, total: items.length });
}

// 18F 第 80 条：跨项目经营的商户在两个项目各一行（项目级查看人拿不到平台合计）。
const R2S_SPLIT = {
  period: "2099-09",
  by_merchant: [
    { key: "m_x", label: "餐饮X", rent_cent: 100, sales_cent: 0, ratio_bps: null, note: null, project_id: "prj_a" },
    { key: "m_x", label: "餐饮X", rent_cent: 900, sales_cent: 0, ratio_bps: null, note: null, project_id: "prj_b" },
  ],
  by_merchant_type: [
    { key: "catering", label: "catering", rent_cent: 100, sales_cent: 0, ratio_bps: null, note: null, project_id: "prj_a" },
    { key: "catering", label: "catering", rent_cent: 900, sales_cent: 0, ratio_bps: null, note: null, project_id: "prj_b" },
  ],
  platform: null,
  unattributed_sales_cent: 0,
  note: "仅含您可查看项目内经营的商户，不含平台合计与未归属营业额",
};

function makeApi(r2s: unknown = R2S): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/dashboards/leasing")) return Promise.resolve(dataEnvelope(DASHBOARD));
      if (url.includes("/rent-to-sales")) return Promise.resolve(dataEnvelope(r2s));
      if (url.includes("/occupancy/vacant-spaces")) return Promise.resolve(listEnvelope([]));
      if (url.includes("/occupancy/expiring-contracts")) return Promise.resolve(listEnvelope([]));
      return Promise.resolve(listEnvelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(r2s?: unknown) {
  const fullProps: AdminPageProps = {
    api: makeApi(r2s),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<OccupancyAnalyticsPage {...fullProps} />);
}

describe("OccupancyAnalyticsPage 租售比(P1c)", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/analytics/occupancy");
  });

  it("平台级/业态级/店级三档 + 未归属营业额标注,且无营业额归属的店诚实标而非伪造 0", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("occupancy-r2s-merchant-table")).toBeTruthy());

    // 平台级:2105bps → 21.05%;未归属营业额 250000 分 → 2,500.00 元。
    expect(screen.getByTestId("occupancy-r2s-platform-ratio").textContent).toContain("21.05%");
    expect(screen.getByTestId("occupancy-r2s-unattributed").textContent).toContain("2,500.00 元");

    // 业态级:merchant_type 码映射中文(catering→餐饮)+ 比率(2429bps → 24.29%)。
    const catering = screen.getByTestId("occupancy-r2s-type-catering");
    expect(catering.textContent).toContain("餐饮");
    expect(within(catering).getByText("24.29%")).toBeTruthy();
    // retail 分母 0 → 诚实标,不伪造。
    expect(screen.getByTestId("occupancy-r2s-type-retail").textContent).toContain("—（无营业额归属）");

    // 店级:m_a 有营业额 → 24.00%;m_c 无营业额归属 → 诚实标。
    expect(screen.getByTestId("occupancy-r2s-merchant-m_a").textContent).toContain("24.00%");
    expect(screen.getByTestId("occupancy-r2s-merchant-m_c").textContent).toContain("—（无营业额归属）");
  });

  it("18F 第 80 条：同一商户、同一业态按项目各一行，门店表与业态表都有「项目」列", async () => {
    renderPage(R2S_SPLIT);
    await waitFor(() => expect(screen.queryByTestId("occupancy-r2s-merchant-m_x-prj_a")?.textContent).toContain("prj_a"));
    expect(screen.getByTestId("occupancy-r2s-merchant-m_x-prj_a").textContent).toContain("1.00 元");
    expect(screen.getByTestId("occupancy-r2s-merchant-m_x-prj_b").textContent).toContain("prj_b");
    expect(screen.getByTestId("occupancy-r2s-merchant-m_x-prj_b").textContent).toContain("9.00 元");
    expect(screen.getByTestId("occupancy-r2s-type-catering-prj_b").textContent).toContain("9.00 元");
    expect(within(screen.getByTestId("occupancy-r2s-merchant-table")).getByText("项目")).toBeTruthy();
    expect(within(screen.getByTestId("occupancy-r2s-type-table")).getByText("项目")).toBeTruthy();
  });
});
