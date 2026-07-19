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

describe("ContractWorkbenchPage 无权限副资源不请求（根因修 AUD-auxload）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("仅 contract.draft.manage 角色：审批链/着色配置端点根本不请求、无降级提示，同权限副资源仍请求，主表照常", async () => {
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/contracts/space-rental-status")) return Promise.resolve(listEnvelope([]));
      if (url.includes("/contracts/approval-flow")) return Promise.resolve(listEnvelope([]));
      if (url.includes("/contracts/expiry-color-rules")) return Promise.resolve(listEnvelope([]));
      if (url.includes("/platform/")) return Promise.resolve(listEnvelope([]));
      if (url.includes("/contracts")) return Promise.resolve(listEnvelope(CONTRACTS));
      return Promise.resolve(listEnvelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const SCOPED: EffectivePermissionsState = { status: "ready", value: { grants: ["contract.draft.manage"], denies: [], wildcard: false } };
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "drafter", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: SCOPED };
    render(<ContractWorkbenchPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("contract-c-1")).toBeTruthy());
    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    // 缺权限副资源：根本不发请求（不再"请求→403→吞成提示"）
    expect(urls.some((u) => u.includes("/contracts/approval-flow"))).toBe(false);
    expect(urls.some((u) => u.includes("/contracts/expiry-color-rules"))).toBe(false);
    // 同权限副资源（contract.draft.manage）仍正常请求
    expect(urls.some((u) => u.includes("/contracts/space-rental-status"))).toBe(true);
    // 未请求 → 无失败 → 无配置降级提示惊扰
    expect(screen.queryByTestId("contract-config-degraded")).toBeNull();
  });
});
