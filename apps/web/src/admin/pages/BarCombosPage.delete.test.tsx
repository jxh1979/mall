// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { BarCombosPage } from "./BarCombosPage";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AO 段第 249 条 249-R8（BSM:570「由具备最高权限的人审批通过后执行（申请人不能审批自己的申请）」；BSM:576「持授权 U 盘密钥的最高权限
//   本人发起的操作，不需要经过任何审批。」；第十八批裁决 71「看全部酒吧目录」）：已存档套餐的删除，店员「申请删除」、提交后需最高权限审批；
//   最高权限本人「直接删除」、提交即删。真实 AdminApi（蛇形键响应经客户端转驼峰），假响应用文本体。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const MASTER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
const STAFF = { id: "u1", username: "bar_manager", accountType: "staff" };
const COMBOS = '[{"id":"combo-arc","bar_id":"bar-main","combo_name":"下架的套餐","price_cent":9900,"status":"active",'
  + '"items":[{"id":"ci-1","product_id":"prod-1","product_name":"威士忌","quantity":1}],'
  + '"archived_at":"2026-11-01T00:00:00Z","delete_status":"none"}]';

function okBody(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
    { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(user: typeof STAFF, posted: string[]) {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET") !== "GET") {
      posted.push(url);
      return okBody('{"id":"combo-arc"}');
    }
    if (url.includes("/api/v1/bar/catalog/combos")) return okBody(COMBOS);
    return okBody('{"items":[]}');
  }));
  const props = { api: new AdminApi(), user, setActionResult: vi.fn(), setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS } as unknown as AdminPageProps;
  render(<BarCombosPage {...props} />);
  return props;
}

// 已存档行有「取消存档」「删除」两个动作，收在「操作▾」下拉里：等行出现再展开，等删除项文字落定再点。
async function clickDelete(label: string) {
  await waitFor(() => expect(screen.getByTestId("bar-combo-actions-combo-arc")).toBeTruthy());
  fireEvent.click(screen.getByTestId("bar-combo-actions-combo-arc"));
  await waitFor(() => expect(screen.getByTestId("bar-combo-delete-request-combo-arc")).toHaveTextContent(label));
  fireEvent.click(screen.getByTestId("bar-combo-delete-request-combo-arc"));
}

describe("BarCombosPage 249-R8 套餐删除的提示与结果", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it("店员：「申请删除」，提示提交后需最高权限审批，结果为删除申请已提交", async () => {
    const posted: string[] = [];
    const props = renderPage(STAFF, posted);
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("配错的套餐");

    await clickDelete("申请删除");

    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "bar_combo_delete_requested", id: "combo-arc" })));
    expect(prompt).toHaveBeenCalledWith("删除理由（提交后需最高权限审批）：");
  });

  it("最高权限本人：「直接删除」，提示最高权限本人发起即删除，结果为套餐删除已批准", async () => {
    const posted: string[] = [];
    const props = renderPage(MASTER, posted);
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("配错的套餐");

    await clickDelete("直接删除");

    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "bar_combo_delete_approved", id: "combo-arc", deleted: true })));
    expect(prompt).toHaveBeenCalledWith("删除理由（最高权限本人发起即删除）：");
    expect(posted.some((url) => url.endsWith("/api/v1/bar/catalog/combos/combo-arc/delete-request"))).toBe(true);
  });
});
