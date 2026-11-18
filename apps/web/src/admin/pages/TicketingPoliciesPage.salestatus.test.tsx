// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AW 段 299-R2（ENUM:30「票务策略状态为：草稿（规则未发布）、待启用（规则已配置但未到生效时间）、售卖中（当前可售卖）、
//   暂停售卖（临时停止售卖）、已结束（售卖周期结束）、归档（历史策略）。」；PGM:287「必须全部为中文，不得出现任何英文」）：
//   票种行按后端派生的 sale_status 显示六态中文；「售卖设置」弹窗整体替换暂停售卖与开售、停售时间。
//   真实 AdminApi（响应蛇形键经客户端转驼峰、请求体转蛇形并带幂等键），假响应用文本体。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TICKETS = '{"items":['
  + '{"id":"t1","ticket_name":"暂停中的日票","status":"published","delete_status":"none","sale_status":"paused",'
  + '"sale_paused_at":"2026-11-18T02:00:00Z"},'
  + '{"id":"t2","ticket_name":"在售日票","status":"published","delete_status":"none","sale_status":"on_sale"},'
  + '{"id":"t3","ticket_name":"过季日票","status":"published","delete_status":"none","sale_status":"ended",'
  + '"sale_ends_at":"2026-10-31T16:00:00Z"}]}';

function okBody(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
    { status: 200, headers: { "content-type": "application/json" } });
}

function errorBody(status: number, code: string, detailCode: string, message: string): Response {
  return new Response(`{"success":false,"code":"${code}","detail_code":"${detailCode}","message":"${message}"}`,
    { status, headers: { "content-type": "application/json" } });
}

describe("TicketingPoliciesPage 299-R2 票务策略六态与售卖设置", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it("票种行显示暂停售卖、售卖中、已结束；售卖设置取消暂停、填停售时间提交，后端拒绝时弹窗原样显示中文报错", async () => {
    const puts: Array<{ url: string; body: Record<string, unknown> }> = [];
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET") === "PUT") {
        puts.push({ url, body: JSON.parse(String(init?.body)) as Record<string, unknown> });
        return puts.length === 1
          ? okBody('{"id":"t1","ticket_name":"暂停中的日票","status":"published","sale_status":"on_sale"}')
          : errorBody(400, "PARAM_INVALID", "VALIDATION_FAILED", "停售时间须晚于开售时间");
      }
      if (/\/ticketing\/ticket-types(\?|$)/.test(url)) return okBody(TICKETS);
      return okBody('{"items":[]}');
    }));
    const props = { api: new AdminApi(), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS } as unknown as AdminPageProps;
    render(<TicketingPoliciesPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("ticket-type-t1")).toHaveTextContent("暂停售卖"));
    expect(screen.getByTestId("ticket-type-t2")).toHaveTextContent("售卖中");
    expect(screen.getByTestId("ticket-type-t3")).toHaveTextContent("已结束");
    for (const id of ["t1", "t2", "t3"]) {
      expect(screen.getByTestId(`ticket-type-${id}`).textContent ?? "").not.toMatch(/[A-Za-z]/);
    }

    // 回显：t1 已暂停，勾选框为选中；取消暂停、填停售时间后保存。
    fireEvent.click(screen.getByTestId("ticket-type-sale-settings-t1"));
    await waitFor(() => expect((screen.getByTestId("ticket-type-sale-paused") as HTMLInputElement).checked).toBe(true));
    fireEvent.click(screen.getByTestId("ticket-type-sale-paused"));
    fireEvent.change(screen.getByTestId("ticket-type-sale-ends-at"), { target: { value: "2026-12-31T22:00" } });
    fireEvent.click(screen.getByTestId("ticket-type-sale-settings-submit"));

    await waitFor(() => expect(puts).toHaveLength(1));
    expect(puts[0]!.url).toMatch(/\/api\/v1\/ticketing\/ticket-types\/t1\/sale-settings$/);
    expect(puts[0]!.body).toMatchObject({ sale_paused: false, sale_starts_at: null, sale_ends_at: new Date("2026-12-31T22:00").toISOString() });
    expect(typeof puts[0]!.body.idempotency_key).toBe("string");
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "ticket_type_sale_settings_changed", id: "t1" })));
    await waitFor(() => expect(screen.queryByTestId("ticket-type-sale-settings-modal")).toBeNull());

    // 后端拒绝：弹窗不关，错误区原样显示中文原因。
    fireEvent.click(screen.getByTestId("ticket-type-sale-settings-t2"));
    await waitFor(() => expect(screen.queryByTestId("ticket-type-sale-settings-modal")).not.toBeNull());
    fireEvent.click(screen.getByTestId("ticket-type-sale-settings-submit"));
    await waitFor(() => expect(screen.getByTestId("ticket-type-sale-settings-modal-error")).toHaveTextContent("停售时间须晚于开售时间"));
    expect(puts).toHaveLength(2);
    expect(puts[1]!.url).toMatch(/\/ticket-types\/t2\/sale-settings$/);
  });
});
