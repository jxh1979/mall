import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { FinancePropertyFeePage } from "./FinancePropertyFeePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const PROJECTS = [{ id: "p-1", project_name: "综合体一期" }];

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

describe("FinancePropertyFeePage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("商户名单接口 403 → 项目/费率主内容照常渲染，商户勾选区块降级提示，整页不报错", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/platform/merchants")) return Promise.resolve(forbidden());
      if (url.includes("/platform/projects")) return Promise.resolve(listEnvelope(PROJECTS));
      if (url.includes("/property-fee/rates")) return Promise.resolve(listEnvelope([]));
      return Promise.resolve(listEnvelope([]));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<FinancePropertyFeePage {...props} />);

    await waitFor(() => expect(screen.getByTestId("property-fee-merchants-degraded")).toBeTruthy());
    // 主资源（项目 + 费率）照常渲染：商户表壳仍在（降级为空），且未落入整页错误态。
    expect(screen.getByTestId("property-fee-merchants-table")).toBeTruthy();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});
