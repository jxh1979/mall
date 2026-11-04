// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十三批 B 段 ROOT-03（B-bar）后台：代客订台必填客人手机号与最晚到店时间（裁决第 7 条、BSM 第 1062 行）、
// 邀请函由酒吧员工「确认并签发」（裁决第 4 条）、门口按本人动态入场码核验（BSM 第 870 行）、散台与固定桌台码（BSM 第 1025 行）。
// 后台请求按真实形状（AdminApi 发蛇形键）核对。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TABLES = [
  { id: "t-1", table_code: "A-01", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "active", held_reservation_id: "rsv-1" },
  { id: "t-loose", table_code: "S-01", bar_id: "bar-1", table_type: "loose_table", capacity: 4, status: "active" },
];
const RESERVATIONS = [{ id: "rsv-1", bar_id: "bar-1", table_id: "t-1", guest_count: 2, deposit_status: "paid", status: "reserved" }];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: unknown[]): Response {
  return envelope({ items, page: 1, page_size: 50, total: items.length });
}

function renderPage(posts: { url: string; body: Record<string, unknown> }[]) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(envelope({ id: "ok", status: "entered" }));
    }
    if (url.includes("/platform/merchants")) return Promise.resolve(list([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
    if (url.includes("/bar/reservations")) return Promise.resolve(list(RESERVATIONS));
    if (url.includes("/bar/table-sessions")) return Promise.resolve(list([]));
    if (url.includes("/bar/tables")) return Promise.resolve(list(TABLES));
    return Promise.resolve(list([]));
  }));
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<BarTableSessionsPage {...props} />);
}

describe("BarTableSessionsPage 第十三批 B 段订台与扫码点单", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("代客订台必须填客人手机号与最晚到店时间，按蛇形键发出", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("bar-new-reservation-table"), { target: { value: "t-1" } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    expect(screen.queryByTestId("bar-create-error")?.textContent ?? "").toContain("客人手机号");
    fireEvent.change(screen.getByTestId("bar-new-reservation-phone"), { target: { value: "13800000000" } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    expect(screen.getByTestId("bar-create-error").textContent).toContain("最晚到店时间");
    expect(posts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("bar-new-reservation-latest"), { target: { value: "2030-06-20T23:00" } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/reservations");
    expect(posts[0]!.body.customer_phone).toBe("13800000000");
    expect(posts[0]!.body.latest_arrival_at).toBe(new Date("2030-06-20T23:00").toISOString().replace(/\.\d{3}Z$/, "Z"));
  });

  it("代客订台手机号去掉空格横线后须为 11 位，按归一后的号码发出", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("bar-new-reservation-table"), { target: { value: "t-1" } });
    fireEvent.change(screen.getByTestId("bar-new-reservation-latest"), { target: { value: "2030-06-20T23:00" } });
    fireEvent.change(screen.getByTestId("bar-new-reservation-phone"), { target: { value: "138 0000-000" } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    expect(screen.queryByTestId("bar-create-error")?.textContent ?? "").toContain("11 位");
    expect(posts).toHaveLength(0);
    fireEvent.change(screen.getByTestId("bar-new-reservation-phone"), { target: { value: "138 0000-0000" } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.body.customer_phone).toBe("13800000000");
  });

  it("被有效预留占着的台位显示已预留；到店开台带上订台号按蛇形键发出", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect(screen.queryByTestId("bar-table-held-t-1")?.textContent ?? "").toContain("已预留"), { timeout: 3000 });
    expect(screen.queryByTestId("bar-table-held-t-1")?.textContent ?? "").toContain("rsv-1");
    expect(screen.queryByTestId("bar-table-held-t-loose")).toBeNull();
    await waitFor(() => expect((screen.getByTestId("bar-new-session-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("bar-new-session-table"), { target: { value: "t-1" } });
    fireEvent.change(screen.getByTestId("bar-new-session-reservation"), { target: { value: "rsv-1" } });
    fireEvent.click(screen.getByTestId("bar-new-session-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/table-sessions");
    expect(posts[0]!.body.table_id).toBe("t-1");
    expect(posts[0]!.body.reservation_id).toBe("rsv-1");
  });

  it("员工确认并签发邀请函；门口核验发送客人出示的本人动态入场码", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect(screen.getByTestId("bar-reservation-select-rsv-1")).toBeTruthy(), { timeout: 5000 });
    expect(screen.getByTestId("bar-invitation-issue").textContent).toContain("确认并签发");
    fireEvent.click(screen.getByTestId("bar-reservation-select-rsv-1"));
    fireEvent.change(screen.getByTestId("bar-invitation-member"), { target: { value: "bie.invm_1.1999999999.abcd.sig" } });
    fireEvent.click(screen.getByTestId("bar-invitation-entry"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/reservations/rsv-1/invitation/entry-verify");
    expect(posts[0]!.body.credential_payload).toBe("bie.invm_1.1999999999.abcd.sig");
  });

  it("第 135 条：页面不再提供按人数登记入场，卡座免通票入场只走门口核验本人动态入场码", async () => {
    // 依据 BSM:1036「到店出示本人的动态入场码核验入场」、ENUM:36「不存在免核销入场的情形」。
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect(screen.getByTestId("bar-reservation-select-rsv-1")).toBeTruthy(), { timeout: 5000 });
    await waitFor(() => {
      expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1");
      expect((screen.getByTestId("bar-new-session-bar") as HTMLSelectElement).value).toBe("bar-1");
    }, { timeout: 5000 });
    fireEvent.click(screen.getByTestId("bar-reservation-select-rsv-1"));
    expect(screen.queryByTestId("bar-checkin-count")).toBeNull();
    expect(screen.queryByTestId("bar-checkin-submit")).toBeNull();
    fireEvent.change(screen.getByTestId("bar-invitation-member"), { target: { value: "bie.invm_1.1999999999.abcd.sig" } });
    fireEvent.click(screen.getByTestId("bar-invitation-entry"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/reservations/rsv-1/invitation/entry-verify");
    expect(posts[0]!.body.credential_payload).toBe("bie.invm_1.1999999999.abcd.sig");
    expect(posts[0]!.body.point_id).toBe("bar-1");
    expect((posts[0]!.body.business_context as Record<string, unknown>).bar_id).toBe("bar-1");
    expect(posts.some((post) => post.url.includes("check-in-guest"))).toBe(false);
  });

  it("建台可选散台；散台行可显示固定桌台码", async () => {
    renderPage([]);
    await waitFor(() => expect(screen.queryByTestId("bar-table-qr-t-loose")).not.toBeNull(), { timeout: 3000 });
    expect(screen.queryByTestId("bar-table-qr-t-1")).toBeNull();
    fireEvent.click(screen.getByTestId("bar-table-qr-t-loose"));
    expect(screen.getByTestId("bar-table-qr-value").textContent).toBe("table?t=t-loose");

    fireEvent.click(screen.getByTestId("bar-new-table-open"));
    await waitFor(() => expect(screen.getByTestId("bar-new-table-type")).toBeTruthy());
    const options = Array.from((screen.getByTestId("bar-new-table-type") as HTMLSelectElement).options).map((option) => option.value);
    expect(options).toContain("loose_table");
  });
});
