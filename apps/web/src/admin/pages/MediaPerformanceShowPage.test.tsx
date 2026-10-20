// 文件版本：V0.01
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

  it("锁定后字段只读：无「锁定」按钮、输入禁用；改动走「新建合同版本」提交到 /versions", async () => {
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
  });
});
