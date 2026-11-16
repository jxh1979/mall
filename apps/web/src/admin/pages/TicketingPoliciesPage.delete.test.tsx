// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AO 段第 249 条 249-R8（BSM:576「删除一律须由具备最高权限的人审批通过后执行，不设例外……持授权 U 盘密钥的最高权限本人发起的操作，
//   不需要经过任何审批。」）：已存档票种的删除，最高权限本人看到「直接删除」、提交即删；其他人「申请删除」、提交后需最高权限审批。
//   真实 AdminApi（蛇形键响应经客户端转驼峰），假响应用文本体。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const MASTER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
const STAFF = { id: "u1", username: "admin", accountType: "platform_admin" };
const TICKETS = '{"items":[{"id":"tt-arc","ticket_name":"已存档票种","status":"published",'
  + '"archived_at":"2026-11-01T00:00:00Z","delete_status":"none"}]}';
const PENDING = TICKETS.replace('"delete_status":"none"', '"delete_status":"pending"');

function okBody(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
    { status: 200, headers: { "content-type": "application/json" } });
}

function errorBody(status: number, code: string, detailCode: string, message: string): Response {
  return new Response(`{"success":false,"code":"${code}","detail_code":"${detailCode}","message":"${message}"}`,
    { status, headers: { "content-type": "application/json" } });
}

function renderPage(user: typeof STAFF, posted: string[], tickets = TICKETS, reply = () => okBody('{"id":"tt-arc"}')) {
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET") !== "GET") {
      posted.push(url);
      return reply();
    }
    if (/\/ticketing\/ticket-types(\?|$)/.test(url)) return okBody(tickets);
    return okBody('{"items":[]}');
  }));
  const props = { api: new AdminApi(), user, setActionResult: vi.fn(), setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS } as unknown as AdminPageProps;
  render(<TicketingPoliciesPage {...props} />);
  return props;
}

// 已存档行有「取消存档」「删除」两个动作，收在「操作▾」下拉里：等行出现再展开，等删除项文字落定再点。
async function clickDelete(label: string) {
  await waitFor(() => expect(screen.getByTestId("ticket-type-actions-tt-arc")).toBeTruthy());
  fireEvent.click(screen.getByTestId("ticket-type-actions-tt-arc"));
  await waitFor(() => expect(screen.getByTestId("ticket-type-delete-request-tt-arc")).toHaveTextContent(label));
  fireEvent.click(screen.getByTestId("ticket-type-delete-request-tt-arc"));
}

describe("TicketingPoliciesPage 249-R8 票种删除的提示与结果", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it("最高权限本人：「直接删除」，提示最高权限本人发起即删除，结果为票种删除已批准", async () => {
    const posted: string[] = [];
    const props = renderPage(MASTER, posted);
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("配错的票种");

    await clickDelete("直接删除");

    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "ticket_type_delete_approved", id: "tt-arc", deleted: true })));
    expect(prompt).toHaveBeenCalledWith("删除理由（最高权限本人发起即删除）：");
    expect(posted.some((url) => url.endsWith("/api/v1/ticketing/ticket-types/tt-arc/delete-request"))).toBe(true);
  });

  it("其他人：「申请删除」，提示提交后需最高权限审批，结果为删除申请已提交", async () => {
    const posted: string[] = [];
    const props = renderPage(STAFF, posted);
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("配错的票种");

    await clickDelete("申请删除");

    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "ticket_type_delete_requested", id: "tt-arc" })));
    expect(prompt).toHaveBeenCalledWith("删除理由（提交后需最高权限审批）：");
  });
});

// 18AO 收尾复核意见 5（PGM:291「用户看到的每一处文字都是中文，含……提示、报错」；第 209 条先例）：批准、驳回、直接删除被后端拒绝时
//   页面报出中文原因，不静默。403 经客户端统一翻译为「当前账号无权进行此操作」，409 带后端原句。
describe("TicketingPoliciesPage 票种删除被后端拒绝时报错", () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it("非最高权限点「批准删除」被 403：报中文原因，不记删除已批准", async () => {
    const posted: string[] = [];
    const props = renderPage(STAFF, posted, PENDING,
      () => errorBody(403, "FORBIDDEN", "MASTER_APPROVAL_REQUIRED", "票种删除须由最高权限本人批准"));

    await waitFor(() => expect(screen.getByTestId("ticket-type-actions-tt-arc")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-type-actions-tt-arc"));
    await waitFor(() => expect(screen.getByTestId("ticket-type-delete-approve-tt-arc")).toHaveTextContent("批准删除"));
    fireEvent.click(screen.getByTestId("ticket-type-delete-approve-tt-arc"));

    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith("当前账号无权进行此操作"));
    expect(posted.some((url) => url.endsWith("/api/v1/ticketing/ticket-types/tt-arc/delete-approve"))).toBe(true);
    expect(props.setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "ticket_type_delete_approved" }));
  });

  it("最高权限「直接删除」仍被订单引用的票种被 409：报后端原句，不记已删除", async () => {
    const posted: string[] = [];
    const message = "票种仍被 3 条业务记录引用（订单/权益/价格规则/结算等），不可删除";
    const props = renderPage(MASTER, posted, TICKETS, () => errorBody(409, "STATE_CONFLICT", "TICKET_TYPE_IN_USE", message));
    vi.spyOn(window, "prompt").mockReturnValue("配错的票种");

    await clickDelete("直接删除");

    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith("操作冲突：" + message));
    expect(props.setActionResult).not.toHaveBeenCalledWith(expect.objectContaining({ action: "ticket_type_delete_approved" }));
  });
});
