// 文件版本：V0.01
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import { VALUE_LABELS } from "../i18n/fieldLabels";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AK 段第 230 条 230-R2（BSM:1058「已预留只能经取消订台、超时释放、到店开台三条路径退出；店长及以上可人工取消预留，必须填原因并留痕。」）：
// 后台「卡座与订台」页订台列表选中行（已预订）给「取消订台」——持 bar.reservation.cancel 才显示，原因必填，按蛇形键发出，成功后刷新列表。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TABLES = [{ id: "t-1", table_code: "A-01", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "active", held_reservation_id: "resv-live" }];
const RESERVATIONS = [
  { id: "resv-live", bar_id: "bar-1", table_id: "t-1", guest_count: 4, status: "reserved", deposit_status: "paid" },
  { id: "resv-gone", bar_id: "bar-1", table_id: "t-1", guest_count: 2, status: "cancelled", deposit_status: "refund_pending" },
];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: unknown[]): Response {
  return envelope({ items, page: 1, page_size: 50, total: items.length });
}

type Post = { url: string; body: Record<string, unknown> };

function renderPage(perms: EffectivePermissionsState = ADMIN_PERMS) {
  const posts: Post[] = [];
  const gets: string[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(envelope({ id: "resv-live", status: "cancelled" }));
    }
    gets.push(url);
    if (url.includes("/platform/merchants")) return Promise.resolve(list([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
    if (url.includes("/bar/table-sessions")) return Promise.resolve(list([]));
    if (url.includes("/bar/tables")) return Promise.resolve(list(TABLES));
    if (url.includes("/bar/reservations")) return Promise.resolve(list(RESERVATIONS));
    return Promise.resolve(list([]));
  }));
  const setActionResult = vi.fn();
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "店长", accountType: "staff" }, setActionResult, setGlobalError: vi.fn(), effectivePermissions: perms };
  render(<BarTableSessionsPage {...props} />);
  return { posts, gets, setActionResult };
}

const reservationLoads = (gets: string[]) => gets.filter((url) => url.includes("/bar/reservations")).length;

describe("BarTableSessionsPage 第 230 条人工取消订台", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("选中已预订的订台：未填原因不发请求；填原因后按蛇形键发 POST …/cancel，成功后刷新列表", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const { posts, gets, setActionResult } = renderPage();
    await waitFor(() => expect(screen.queryByTestId("bar-reservation-cancel-submit")?.textContent).toBe("取消订台"), { timeout: 3000 });
    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    expect(screen.getByTestId("bar-reservation-cancel-form").textContent).toContain("resv-live");

    fireEvent.click(screen.getByTestId("bar-reservation-cancel-submit"));
    expect(screen.getByTestId("bar-create-error").textContent).toContain("原因");
    expect(confirm).not.toHaveBeenCalled();
    expect(posts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("bar-reservation-cancel-reason"), { target: { value: " 客人来电取消 " } });
    const before = reservationLoads(gets);
    fireEvent.click(screen.getByTestId("bar-reservation-cancel-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(posts[0]!.url).toMatch(/\/api\/v1\/bar\/reservations\/resv-live\/cancel$/);
    expect(posts[0]!.body.reason).toBe("客人来电取消");
    expect(posts[0]!.body.idempotency_key).toBeTruthy();
    expect(Object.keys(posts[0]!.body).some((key) => /[A-Z]/.test(key))).toBe(false);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_reservation_cancelled", reservationId: "resv-live" })));
    await waitFor(() => expect(reservationLoads(gets)).toBe(before + 1));
    expect(VALUE_LABELS.bar_reservation_cancelled).toBe("订台已取消");
  });

  it("持 bar.reservation.cancel 才显示取消订台；只有员工代订在用的 bar.reservation.manage 不显示", async () => {
    const base = ["bar.table_session.view", "bar.reservation.manage", "bar.reservation.create"];
    renderPage({ status: "ready", value: { grants: [...base, "bar.reservation.cancel"], denies: [], wildcard: false } });
    await waitFor(() => expect(screen.queryByTestId("bar-reservation-cancel-submit")).not.toBeNull(), { timeout: 3000 });
    expect((screen.getByTestId("bar-reservation-cancel-submit") as HTMLButtonElement).disabled).toBe(false);
    cleanup();

    renderPage({ status: "ready", value: { grants: base, denies: [], wildcard: false } });
    await waitFor(() => expect((screen.queryByTestId("bar-reservation-select-resv-live") as HTMLInputElement | null)?.checked).toBe(true), { timeout: 3000 });
    expect(screen.queryByTestId("bar-reservation-cancel-submit")).toBeNull();
    expect(screen.queryByTestId("bar-reservation-cancel-reason")).toBeNull();
  });

  it("已取消的订台不显示取消订台", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("bar-reservation-cancel-submit")).not.toBeNull(), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("bar-reservation-select-resv-gone"));
    await waitFor(() => expect(screen.queryByTestId("bar-reservation-cancel-submit")).toBeNull());
    expect(screen.queryByTestId("bar-reservation-cancel-reason")).toBeNull();
    fireEvent.click(screen.getByTestId("bar-reservation-select-resv-live"));
    await waitFor(() => expect(screen.queryByTestId("bar-reservation-cancel-submit")).not.toBeNull());
  });
});
