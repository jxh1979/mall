// 文件版本：V0.04
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { MediaPerformanceShowPage, type MediaShow } from "./MediaPerformanceShowPage";
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
