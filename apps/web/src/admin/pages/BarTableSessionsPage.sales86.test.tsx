// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 I 段第 86 条：BSM 第 1021 行「由有销售归属的订台开台的，自动记营销带客并带出该销售人员；以上都不命中的直接到店开台默认记自来客——
// 此时现场可人工补选营销带客并选定销售人员」；BSM 第 1077 行「主责服务提供人按当日该场次排班指派到该卡座」。后台请求按真实形状（AdminApi 发蛇形键）核对。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TABLES = [{ id: "t-1", table_code: "A-01", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "active", held_reservation_id: "rsv-1" }];
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
      return Promise.resolve(envelope({ id: "ok", status: "open" }));
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

describe("BarTableSessionsPage 第 86 条开台销售与服务员", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("填了订台号：不再填销售，显示按订台带出，请求不带 sales_staff_id", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect((screen.getByTestId("bar-new-session-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("bar-new-session-sales"), { target: { value: "staff-onsite" } });
    fireEvent.change(screen.getByTestId("bar-new-session-table"), { target: { value: "t-1" } });
    fireEvent.change(screen.getByTestId("bar-new-session-reservation"), { target: { value: "rsv-1" } });
    await waitFor(() => expect(screen.queryByTestId("bar-new-session-sales")).toBeNull());
    expect(screen.getByTestId("bar-new-session-sales-from-reservation").textContent).toContain("销售：按订台带出");
    fireEvent.click(screen.getByTestId("bar-new-session-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/table-sessions");
    expect(posts[0]!.body.reservation_id).toBe("rsv-1");
    expect("sales_staff_id" in posts[0]!.body).toBe(false);
  });

  it("直接到店（订台号为空）：照旧可现场补选销售，按蛇形键带出", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect((screen.getByTestId("bar-new-session-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    expect(screen.queryByTestId("bar-new-session-sales-from-reservation")).toBeNull();
    fireEvent.change(screen.getByTestId("bar-new-session-table"), { target: { value: "t-1" } });
    fireEvent.change(screen.getByTestId("bar-new-session-sales"), { target: { value: "staff-onsite" } });
    fireEvent.click(screen.getByTestId("bar-new-session-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.body.sales_staff_id).toBe("staff-onsite");
  });

  it("服务员输入标明不填按排班自动指派；填写时照旧带 service_staff_id", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect(screen.getByTestId("bar-create-session-form").textContent).toContain("服务员（不填按排班自动指派；当班多人时填其一）"), { timeout: 5000 });
    await waitFor(() => expect((screen.getByTestId("bar-new-session-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("bar-new-session-table"), { target: { value: "t-1" } });
    fireEvent.change(screen.getByTestId("bar-new-session-service"), { target: { value: "staff-waiter" } });
    fireEvent.click(screen.getByTestId("bar-new-session-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.body.service_staff_id).toBe("staff-waiter");
  });
});
