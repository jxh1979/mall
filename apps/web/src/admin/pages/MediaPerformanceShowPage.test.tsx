// 文件版本：V0.05
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MediaPerformanceShowPage, type MediaShow } from "./MediaPerformanceShowPage";
import { AdminApi } from "../client/adminClient";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// TK-29 演出合同与履约结算页（ADM-STL-004）组件测试：三种情形任意组合可提交（只填其中一种）、锁定后字段只读且改动走新建版本。
const BASE = "/api/v1/settlement/media-shows";
const LOCKED: MediaShow = {
  id: "ms_1", showGroupId: "ms_1", contractVersion: 1, projectId: "prj_1", barMerchantId: "m_bar", mediaMerchantId: "m_media",
  showKind: "bar_show", showDate: "2026-10-25", plannedStartAt: null, plannedEndAt: null, contractRef: "合同-1",
  ticketShareMode: "ratio", ticketShareBps: 5000, ticketShareAmountCent: null, ticketSurchargePerTicketCent: 2000, ticketTypeId: "tt_1",
  fixedFeeCent: 50000, drinkCommissionBps: 1000, drinkBaseTemplate: "whole_store_session", status: "locked", lockedBy: "mgr",
};

function makeApi(get = vi.fn().mockResolvedValue([]), post = vi.fn().mockResolvedValue({ id: "ms_new" })): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<MediaPerformanceShowPage {...props} />);
}

afterEach(() => vi.restoreAllMocks());

describe("MediaPerformanceShowPage 演出合同与履约结算(ADM-STL-004)", () => {
  it("三种情形任意组合可提交：只勾②固定演出费也能保存草案，未勾的情形不带字段", async () => {
    const get = vi.fn().mockResolvedValue([]);
    const post = vi.fn().mockResolvedValue({ id: "ms_new" });
    renderPage(makeApi(get, post));
    await waitFor(() => expect(get).toHaveBeenCalledWith(BASE));
    fireEvent.change(screen.getByTestId("mps-form-project"), { target: { value: "prj_1" } });
    fireEvent.change(screen.getByTestId("mps-form-bar"), { target: { value: "m_bar" } });
    fireEvent.change(screen.getByTestId("mps-form-media"), { target: { value: "m_media" } });
    fireEvent.change(screen.getByTestId("mps-form-date"), { target: { value: "2026-10-25" } });
    fireEvent.click(screen.getByTestId("mps-fee-fixed"));
    fireEvent.change(screen.getByTestId("mps-fixed-fee"), { target: { value: "500" } });
    fireEvent.click(screen.getByTestId("mps-form-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(BASE, expect.objectContaining({
      projectId: "prj_1", barMerchantId: "m_bar", mediaMerchantId: "m_media", showKind: "bar_show", showDate: "2026-10-25",
      ticketShareMode: "none", fixedFeeCent: 50000, idempotencyKey: expect.any(String),
    })));
    const sent = post.mock.calls[0]?.[1] as Record<string, unknown>;
    expect(sent).not.toHaveProperty("drinkCommissionBps");
    expect(sent).not.toHaveProperty("ticketSurchargePerTicketCent");
  });

  it("①门票加价分成按裁决填「每张加价 + 适用票种」：元转分、比例万分比一起提交", async () => {
    const post = vi.fn().mockResolvedValue({ id: "ms_new" });
    renderPage(makeApi(vi.fn().mockResolvedValue([]), post));
    await waitFor(() => expect(screen.getByTestId("mps-form")).toBeTruthy());
    fireEvent.change(screen.getByTestId("mps-form-project"), { target: { value: "prj_1" } });
    fireEvent.change(screen.getByTestId("mps-form-bar"), { target: { value: "m_bar" } });
    fireEvent.change(screen.getByTestId("mps-form-media"), { target: { value: "m_media" } });
    fireEvent.change(screen.getByTestId("mps-form-date"), { target: { value: "2026-10-25" } });
    fireEvent.click(screen.getByTestId("mps-fee-ticket"));
    fireEvent.change(screen.getByTestId("mps-ticket-surcharge"), { target: { value: "20" } });
    fireEvent.change(screen.getByTestId("mps-ticket-type"), { target: { value: "tt_1" } });
    fireEvent.change(screen.getByTestId("mps-ticket-bps"), { target: { value: "5000" } });
    fireEvent.click(screen.getByTestId("mps-form-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(BASE, expect.objectContaining({
      ticketShareMode: "ratio", ticketSurchargePerTicketCent: 2000, ticketTypeId: "tt_1", ticketShareBps: 5000,
    })));
  });

  it("锁定后字段只读：无「锁定」按钮、输入禁用；改动走「新建合同版本」提交到 /versions，不带沿用原版本的项目、场次类型、传媒商户", async () => {
    const get = vi.fn().mockImplementation((path: string) =>
      Promise.resolve(path === BASE ? [LOCKED] : { show: LOCKED, fulfillment: null, settlements: [] }));
    const post = vi.fn().mockResolvedValue({ ...LOCKED, id: "ms_2", contractVersion: 2, status: "draft" });
    renderPage(makeApi(get, post));
    await waitFor(() => expect(get).toHaveBeenCalledWith(BASE));
    fireEvent.click(await screen.findByTestId("mps-open-ms_1"));
    await waitFor(() => expect(get).toHaveBeenCalledWith(`${BASE}/ms_1`));
    await screen.findByTestId("mps-detail");
    expect(screen.queryByTestId("mps-lock")).toBeNull();
    expect((screen.getByTestId("mps-fixed-fee") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByTestId("mps-ticket-surcharge") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByTestId("mps-fee-drink") as HTMLInputElement).disabled).toBe(true);

    fireEvent.click(screen.getByTestId("mps-new-version"));
    expect((screen.getByTestId("mps-fixed-fee") as HTMLInputElement).disabled).toBe(false);
    fireEvent.change(screen.getByTestId("mps-fixed-fee"), { target: { value: "990" } });
    fireEvent.click(screen.getByTestId("mps-version-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(`${BASE}/ms_1/versions`, expect.objectContaining({ fixedFeeCent: 99000 })));
    // 第十八批 AF 段 198-R5：新建版本时项目、场次类型、传媒公司商户沿用原版本，服务端不读请求体这三项，网页不再发。
    const versionBody = post.mock.calls.find((call) => call[0] === `${BASE}/ms_1/versions`)?.[1];
    expect(versionBody).not.toHaveProperty("projectId");
    expect(versionBody).not.toHaveProperty("showKind");
    expect(versionBody).not.toHaveProperty("mediaMerchantId");
  });

  it("BSM:1839 卡片明细对账：场次列表在现行版本行上标「结算待处理 n 项」，被取代的版本行不标", async () => {
    const get = vi.fn().mockResolvedValue([
      { ...LOCKED, id: "ms_2", contractVersion: 2, pendingSettlementCount: 2 },
      { ...LOCKED, status: "superseded", pendingSettlementCount: 0 },
    ]);
    renderPage(makeApi(get));
    await waitFor(() => expect(screen.getByTestId("mps-show-ms_2").textContent).toContain("已锁定；结算待处理 2 项"));
    expect(screen.getByTestId("mps-show-ms_1").textContent).toContain("已被新版本取代");
    expect(screen.getByTestId("mps-show-ms_1").textContent).not.toContain("结算待处理");
  });

  // 修复项 F11（PGM:287；18BE 控制席判定①）：实际履约起止是服务端按 UTC 带 Z 输出的时刻，按北京时间显示，不上屏 ISO 原文。
  it("实际履约起止按北京时间显示，不出现 ISO 原文", async () => {
    const fulfillment = { id: "mf_1", showId: "ms_1", actualStartAt: "2026-07-05T13:00:00Z", actualEndAt: "2026-07-05T15:00:00Z", completedContent: "全场演出",
      uncompletedContent: null, basis: "现场签到表", recordedBy: "media_op", status: "confirmed", confirmedBy: "bar_mgr", disputeReason: null, resolutionNote: null, resolvedBy: null };
    const get = vi.fn().mockImplementation((path: string) =>
      Promise.resolve(path === BASE ? [LOCKED] : { show: LOCKED, fulfillment, settlements: [] }));
    renderPage(makeApi(get));
    await waitFor(() => expect(screen.getByTestId("mps-open-ms_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("mps-open-ms_1"));
    await waitFor(() => expect(screen.getByTestId("mps-fulfillment").textContent).toContain("实际 2026年7月5日 21:00 ～ 2026年7月5日 23:00"));
    expect(screen.getByTestId("mps-fulfillment").textContent).not.toContain("2026-07-05T13:00:00Z");
  });

  // 第十八批 AL 段 240-R2（ENUM:192「结算状态 草稿、待复核、已确认、已调整、已归档」；第十八批裁决 54「演出挂起……→待复核；已出结果或已入账→已确认；
  // 被财务调整取代的演出行→已调整」）：结算表「结算状态」按后端派生的 settlement_state 显示五态中文，挂起行括注原挂起原因。
  it("结算表「结算状态」按五态中文显示：待复核（括注原因）、已确认、已调整、草稿", async () => {
    const base = { baseAmountCent: 0, rateBps: null, fixedAmountCent: null, amountCent: 0, payerMerchantId: null, payerEntityId: null,
      payeeEntityId: null, pendingReason: null, arApItemId: null, adjustsSettlementId: null, adjustReason: null };
    const settlements = [
      { ...base, id: "st_p", feeKind: "ticket_share", status: "pending", pendingReason: "unconfirmed", settlementState: "pending_review" },
      { ...base, id: "st_c", feeKind: "fixed_fee", status: "posted", settlementState: "confirmed" },
      { ...base, id: "st_a", feeKind: "drink_commission", status: "adjusted", settlementState: "adjusted" },
      { ...base, id: "st_d", feeKind: "drink_commission", status: "pending", pendingReason: "contract_silent", settlementState: "draft" },
    ];
    const get = vi.fn().mockImplementation((path: string) =>
      Promise.resolve(path === BASE ? [LOCKED] : { show: LOCKED, fulfillment: null, settlements }));
    renderPage(makeApi(get));
    await waitFor(() => expect(screen.getByTestId("mps-open-ms_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("mps-open-ms_1"));
    await waitFor(() => expect(screen.getByTestId("mps-settlement-ticket_share-pending").textContent).toContain("待复核（履约事实尚未确认）"));
    expect(screen.getByTestId("mps-settlement-fixed_fee-posted").textContent).toContain("已确认");
    expect(screen.getByTestId("mps-settlement-drink_commission-adjusted").textContent).toContain("已调整");
    expect(screen.getByTestId("mps-settlement-drink_commission-pending").textContent).toContain("草稿");
    expect(screen.getByTestId("mps-settlement-table").textContent).toContain("结算状态");
    expect(screen.getByTestId("mps-settlement-table").textContent).not.toContain("待处理");
  });
});

// 第 381 条 381-R1（BSM:1145 逐场记录、随合同固化版本；18BE 控制席判定①）：计划起止按本机时区回填 datetime-local，
// 与提交时 toInstant 的换算互逆——打开草案不改再保存、锁定后新建版本不改再提交，发出的仍是原来的时刻。
describe("MediaPerformanceShowPage 计划起止回填（第 381 条）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  const PLANNED = {
    id: "ms_1", show_group_id: "ms_1", contract_version: 1, project_id: "prj_1", bar_merchant_id: "m_bar", media_merchant_id: "m_media",
    show_kind: "bar_show", show_date: "2026-10-25", planned_start_at: "2026-10-25T12:00:00Z", planned_end_at: "2026-10-25T14:30:00Z",
    contract_ref: "合同-1", ticket_share_mode: "none", ticket_share_bps: null, ticket_share_amount_cent: null,
    ticket_surcharge_per_ticket_cent: null, ticket_type_id: null, fixed_fee_cent: null, drink_commission_bps: null,
    drink_base_template: null, status: "draft", locked_by: null,
  };
  const START_MS = new Date("2026-10-25T12:00:00Z").getTime();
  const END_MS = new Date("2026-10-25T14:30:00Z").getTime();

  function ok(data: unknown): Promise<Response> {
    return Promise.resolve(new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
      { status: 200, headers: { "content-type": "application/json" } }));
  }

  /** 桩 fetch：列表、详情（任意编号都回同一场次）；PATCH 与 POST 按「方法 路径」记下请求体并回该场次。 */
  function stubShowApi(show: Record<string, unknown>): Record<string, Record<string, unknown>> {
    const sent: Record<string, Record<string, unknown>> = {};
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const method = (init?.method ?? "GET").toUpperCase();
      if (method !== "GET") {
        sent[`${method} ${url.slice(url.indexOf(BASE))}`] = JSON.parse(String(init?.body));
        return ok(show);
      }
      if (url.endsWith(BASE)) return ok([show]);
      return ok({ show, fulfillment: null, settlements: [] });
    }));
    return sent;
  }

  function renderWithRealApi() {
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<MediaPerformanceShowPage {...props} />);
  }

  it("北京时间下打开草案：计划起止回填为北京钟点，不改直接「保存修改」，PATCH 发出的仍是原时刻", async () => {
    vi.stubEnv("TZ", "Asia/Shanghai");
    expect(new Date("2026-06-14T00:00:00Z").getHours()).toBe(8); // 时区确已生效，否则本用例无意义
    const sent = stubShowApi(PLANNED);
    renderWithRealApi();
    await waitFor(() => expect(screen.getByTestId("mps-open-ms_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("mps-open-ms_1"));
    await waitFor(() => expect((screen.getByLabelText("计划开始") as HTMLInputElement).value).toBe("2026-10-25T20:00"));
    expect((screen.getByLabelText("计划结束") as HTMLInputElement).value).toBe("2026-10-25T22:30");
    fireEvent.click(screen.getByTestId("mps-form-save"));
    await waitFor(() => expect(sent[`PATCH ${BASE}/ms_1`]).toBeTruthy());
    expect(new Date(String(sent[`PATCH ${BASE}/ms_1`]!.planned_start_at)).getTime()).toBe(START_MS);
    expect(new Date(String(sent[`PATCH ${BASE}/ms_1`]!.planned_end_at)).getTime()).toBe(END_MS);
  });

  it("北京时间下锁定版本「新建合同版本」不改直接提交，POST /versions 发出的计划起止仍是原时刻", async () => {
    vi.stubEnv("TZ", "Asia/Shanghai");
    expect(new Date("2026-06-14T00:00:00Z").getHours()).toBe(8); // 时区确已生效，否则本用例无意义
    const sent = stubShowApi({ ...PLANNED, status: "locked", locked_by: "mgr" });
    renderWithRealApi();
    await waitFor(() => expect(screen.getByTestId("mps-open-ms_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("mps-open-ms_1"));
    await waitFor(() => expect(screen.getByTestId("mps-new-version")).toBeTruthy());
    fireEvent.click(screen.getByTestId("mps-new-version"));
    fireEvent.click(screen.getByTestId("mps-version-submit"));
    await waitFor(() => expect(sent[`POST ${BASE}/ms_1/versions`]).toBeTruthy());
    expect(new Date(String(sent[`POST ${BASE}/ms_1/versions`]!.planned_start_at)).getTime()).toBe(START_MS);
    expect(new Date(String(sent[`POST ${BASE}/ms_1/versions`]!.planned_end_at)).getTime()).toBe(END_MS);
  });
});
