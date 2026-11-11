import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
    // P2 双保险：商户名单加载失败时禁用出账按钮 + 提示，防止名单不全误出账。
    expect((screen.getByTestId("property-fee-bill-run") as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByTestId("property-fee-bill-blocked")).toBeTruthy();
  });
});

// 第十八批 AF 段 198-R5：物业费出账只读项目、计费期、商户，契约删 operator_note、evidence_attachment_ids，网页停发写死的备注与假附件。
describe("FinancePropertyFeePage 出账请求体", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("出账只发项目、计费期与所选商户，不带写死的操作备注和附件编号", async () => {
    const isBillPost = (input: unknown, init: unknown) => String(input).includes("/property-fee/bills") && (init as RequestInit | undefined)?.method === "POST";
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (isBillPost(input, init)) return Promise.resolve(dataEnvelope([]));
      if (url.includes("/platform/merchants")) return Promise.resolve(listEnvelope([{ id: "m-1", merchant_name: "商户甲", status: "active", leased_area_square_meter: 100 }]));
      if (url.includes("/platform/projects")) return Promise.resolve(listEnvelope(PROJECTS));
      return Promise.resolve(listEnvelope([]));
    });
    vi.stubGlobal("fetch", fetchMock);
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<FinancePropertyFeePage {...props} />);
    await waitFor(() => expect(screen.getByTestId("property-fee-merchant-select-m-1")).toBeTruthy());
    await waitFor(() => expect((screen.getByTestId("property-fee-project") as HTMLSelectElement).value).toBe("p-1"));

    fireEvent.change(screen.getByTestId("property-fee-bill-period"), { target: { value: "2026-07" } });
    fireEvent.click(screen.getByTestId("property-fee-merchant-select-m-1"));
    fireEvent.click(screen.getByTestId("property-fee-bill-run"));

    await waitFor(() => expect(fetchMock.mock.calls.some(([input, init]) => isBillPost(input, init))).toBe(true));
    const [, init] = fetchMock.mock.calls.find(([input, i]) => isBillPost(input, i))!;
    const body = JSON.parse(String((init as RequestInit).body)) as Record<string, unknown>;
    expect(body).toMatchObject({ project_id: "p-1", period: "2026-07", merchant_ids: ["m-1"] });
    expect(body).not.toHaveProperty("operator_note");
    expect(body).not.toHaveProperty("evidence_attachment_ids");
  });
});
