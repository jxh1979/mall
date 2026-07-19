import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { WorkOrderSlaPage } from "./WorkOrderSlaPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const DASHBOARD = { business_date: "2026-07-04", metrics: { "wo.response_hours_p50": { value: 1.2 }, "wo.backlog": { value: 5 } } };

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("WorkOrderSlaPage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("超时明细两下钻接口 403 → 时效 KPI 主内容照常渲染，两区块各自降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/dashboards/facility")) return Promise.resolve(dataEnvelope(DASHBOARD));
      if (url.includes("/work-order-sla/overdue-orders")) return Promise.resolve(forbidden());
      return Promise.resolve(dataEnvelope({ items: [], page: 1, page_size: 200, total: 0 }));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<WorkOrderSlaPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("wo-overdue-degraded")).toBeTruthy());
    expect(screen.getByTestId("mwo-overdue-degraded")).toBeTruthy();
    expect(screen.getByTestId("wo-sla-summary")).toBeTruthy();
    expect(screen.queryByTestId("wo-overdue-table")).toBeNull();
    expect(screen.queryByTestId("mwo-overdue-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
