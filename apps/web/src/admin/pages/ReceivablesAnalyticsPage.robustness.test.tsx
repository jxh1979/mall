import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ReceivablesAnalyticsPage } from "./ReceivablesAnalyticsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const DASHBOARD = { business_date: "2026-07-04", metrics: { "ar.balance": { value: 500000 }, "ar.aging_90p": { value: 120000 } } };

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("ReceivablesAnalyticsPage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("欠费商户明细接口 403 → 仪表盘 KPI 主内容照常渲染，欠费明细区块降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/dashboards/finance")) return Promise.resolve(dataEnvelope(DASHBOARD));
      if (url.includes("/finance-receivables/debtors")) return Promise.resolve(forbidden());
      return Promise.resolve(dataEnvelope({ items: [], page: 1, page_size: 200, total: 0 }));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<ReceivablesAnalyticsPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("receivables-debtors-degraded")).toBeTruthy());
    expect(screen.getByTestId("receivables-summary")).toBeTruthy();
    expect(screen.queryByTestId("receivables-debtors-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
