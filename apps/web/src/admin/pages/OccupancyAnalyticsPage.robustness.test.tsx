import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { OccupancyAnalyticsPage } from "./OccupancyAnalyticsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const DASHBOARD = { business_date: "2026-07-04", metrics: { "occupancy.rate": { value: 0.82 }, "occupancy.rentable_area": { value: 10000 }, "occupancy.leased_area": { value: 8200 } } };

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("OccupancyAnalyticsPage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/analytics/occupancy");
  });

  it("空置/到期两下钻接口 403 → 出租率 KPI 主内容照常渲染，两区块各自降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/dashboards/leasing")) return Promise.resolve(dataEnvelope(DASHBOARD));
      if (url.includes("/occupancy/vacant-spaces")) return Promise.resolve(forbidden());
      if (url.includes("/occupancy/expiring-contracts")) return Promise.resolve(forbidden());
      return Promise.resolve(dataEnvelope({ items: [], page: 1, page_size: 200, total: 0 }));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<OccupancyAnalyticsPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("occupancy-vacant-degraded")).toBeTruthy());
    expect(screen.getByTestId("occupancy-expiring-degraded")).toBeTruthy();
    expect(screen.getByTestId("occupancy-summary")).toBeTruthy();
    expect(screen.queryByTestId("occupancy-vacant-table")).toBeNull();
    expect(screen.queryByTestId("occupancy-expiring-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
