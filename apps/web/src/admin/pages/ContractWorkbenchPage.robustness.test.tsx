import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ContractWorkbenchPage } from "./ContractWorkbenchPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const CONTRACTS = [{ id: "c-1", merchant_id: "m-1", status: "active" }];

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("ContractWorkbenchPage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("审批链/到期着色/铺位租赁状态三配置副资源 403 → 合同主表照常渲染，配置合并降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(forbidden());
      if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(forbidden());
      if (url.includes("/contracts/space-rental-status")) return Promise.resolve(forbidden());
      if (url.includes("/contracts")) return Promise.resolve(listEnvelope(CONTRACTS));
      return Promise.resolve(listEnvelope([]));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<ContractWorkbenchPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("contract-config-degraded")).toBeTruthy());
    expect(screen.getByTestId("contracts-table")).toBeTruthy();
    expect(screen.getByTestId("contract-c-1")).toBeTruthy();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
