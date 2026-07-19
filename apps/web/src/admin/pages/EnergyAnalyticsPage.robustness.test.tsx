import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { EnergyAnalyticsPage } from "./EnergyAnalyticsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const DASHBOARD = { business_date: "2026-07-04", metrics: { "energy.total_electricity": { value: 1200 }, "energy.abnormal_count": { value: 3 } } };

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("EnergyAnalyticsPage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("异常读数明细接口 403 → 能耗指标主内容照常渲染，异常明细区块降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/dashboards/energy")) return Promise.resolve(dataEnvelope(DASHBOARD));
      if (url.includes("/energy/abnormal-readings")) return Promise.resolve(forbidden());
      return Promise.resolve(dataEnvelope({ items: [], page: 1, page_size: 200, total: 0 }));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<EnergyAnalyticsPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("energy-abnormal-degraded")).toBeTruthy());
    expect(screen.getByTestId("energy-summary")).toBeTruthy();
    expect(screen.queryByTestId("energy-abnormal-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
