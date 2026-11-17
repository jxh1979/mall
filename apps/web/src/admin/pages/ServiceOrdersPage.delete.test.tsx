// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { ServiceOrdersPage } from "./ServiceOrdersPage";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AO 段第 249 条 249-R8（BSM:576「删除一律须由具备最高权限的人审批通过后执行，不设例外……持授权 U 盘密钥的最高权限本人发起的操作，
//   不需要经过任何审批。」）：已存档服务目录项的删除，最高权限本人看到「直接删除」、提交即删；其他人「申请删除」、提交后需最高权限审批。
//   真实 AdminApi（蛇形键响应经客户端转驼峰），假响应用文本体。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const MASTER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
const STAFF = { id: "u1", username: "admin", accountType: "platform_admin" };
const CATALOG = '{"items":[{"id":"svc-arc","name":"足疗","price_cent":10000,"duration_minutes":30,"active":true,'
  + '"mgmt_fee_mode":"none","mgmt_fee_value":0,"archived_at":"2026-11-01T00:00:00Z","delete_status":"none"}],"page":1,"page_size":50,"total":1}';

function okBody(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
    { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(user: typeof STAFF, posted: string[]) {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET") !== "GET") {
      posted.push(url);
      return okBody('{"id":"svc-arc"}');
    }
    if (url.includes("/services/catalog/all")) return okBody(CATALOG);
    return okBody('{"items":[],"page":1,"page_size":50,"total":0}');
  }));
  const props = { api: new AdminApi(), user, setActionResult: vi.fn(), setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS } as unknown as AdminPageProps;
  render(<ServiceOrdersPage {...props} />);
  return props;
}

// 已存档行有「取消存档」「删除」两个动作，收在「操作▾」下拉里：等行出现再展开，等删除项文字落定再点。
async function clickDelete(label: string) {
  await waitFor(() => expect(screen.getByTestId("svc-catalog-actions-svc-arc")).toBeTruthy());
  fireEvent.click(screen.getByTestId("svc-catalog-actions-svc-arc"));
  await waitFor(() => expect(screen.getByTestId("svc-catalog-delete-request-svc-arc")).toHaveTextContent(label));
  fireEvent.click(screen.getByTestId("svc-catalog-delete-request-svc-arc"));
}

describe("ServiceOrdersPage 249-R8 服务目录删除的提示与结果", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it("最高权限本人：「直接删除」，提示最高权限本人发起即删除，结果为服务目录删除已批准", async () => {
    const posted: string[] = [];
    const props = renderPage(MASTER, posted);
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("停售的服务");

    await clickDelete("直接删除");

    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "service_catalog_delete_approved", id: "svc-arc", deleted: true })));
    expect(prompt).toHaveBeenCalledWith("删除理由（最高权限本人发起即删除）：");
    expect(posted.some((url) => url.endsWith("/api/v1/services/catalog/svc-arc/delete-request"))).toBe(true);
  });

  it("其他人：「申请删除」，提示提交后需最高权限审批，结果为删除申请已提交", async () => {
    const posted: string[] = [];
    const props = renderPage(STAFF, posted);
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("停售的服务");

    await clickDelete("申请删除");

    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "service_catalog_delete_requested", id: "svc-arc" })));
    expect(prompt).toHaveBeenCalledWith("删除理由（提交后需最高权限审批）：");
  });
});
