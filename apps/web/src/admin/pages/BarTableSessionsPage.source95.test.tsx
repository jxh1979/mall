// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 K 段第 95 条：BSM 第 1021 行「开台时系统自动判定并记录客人来源……经平台自有渠道订台或购票到店的，自动记平台自有渠道……
// 来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」；裁决 21「开台记来源只看有没有订台、订台从哪里来」。
// 后台请求按真实形状（AdminApi 发蛇形键）核对，假响应用文本体。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const SESSION = { id: "s-1", bar_id: "bar-1", table_id: "t-1", status: "open", guest_source: "platform_own", session_version: 1,
  opened_at: "2026-10-29T12:00:00Z" };
const SALES_BACKFILL = { id: "mb-1", target_type: "bar_session_sales", target_id: "s-1", proposed: { sales_staff_id: "u-sales" },
  original_snapshot: { guest_source: "platform_own", session_sales: null, orders: [], bills: [] }, evidence_attachment_ids: [], status: "pending" };

type Post = { url: string; body: Record<string, unknown> };

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: unknown[]): Response {
  return envelope({ items, page: 1, page_size: 50, total: items.length });
}

function renderPage(posts: Post[], salesBackfills: unknown[] = [], perms: EffectivePermissionsState = ADMIN_PERMS) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(envelope({ ...SESSION, guest_source: "marketing", sales_staff_id: "u-c", session_version: 2 }));
    }
    if (url.includes("/evidence/manual-backfills") && url.includes("bar_session_sales")) return Promise.resolve(list(salesBackfills));
    if (url.includes("/evidence/manual-backfills") || url.includes("/service-responsibilities")) return Promise.resolve(list([]));
    if (url.includes("/bar/table-sessions")) return Promise.resolve(list([SESSION]));
    return Promise.resolve(list([]));
  }));
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u-m", username: "店长", accountType: "staff" }, setActionResult: vi.fn(),
    setGlobalError: vi.fn(), effectivePermissions: perms };
  render(<BarTableSessionsPage {...props} />);
}

describe("BarTableSessionsPage 第 95 条开台客人来源", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("95-R1：会话明细显示「客人来源 平台自有渠道」「销售 无」，不再把销售为空写成自来客", async () => {
    renderPage([]);
    await waitFor(() => expect(screen.queryByTestId("bar-session-row-s-1")).not.toBeNull(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("bar-session-row-s-1"));
    const drawer = screen.getByTestId("row-detail-drawer");
    const fields = Array.from(drawer.querySelectorAll(".admin-drawer-field")).map((field) => field.textContent ?? "");
    expect(fields).toContain("客人来源平台自有渠道");
    expect(fields).toContain("销售无");
    expect(drawer.textContent).not.toContain("无销售（自来客）");
  });

  it("95-R1：本场销售归属面板显示客人来源；补录行原记录按原快照来源显示「无销售（平台自有渠道）」", async () => {
    renderPage([], [SALES_BACKFILL]);
    await waitFor(() => expect(screen.queryByTestId("bar-sales-current")?.textContent ?? "").toContain("客人来源：平台自有渠道"), { timeout: 3000 });
    expect(screen.getByTestId("bar-sales-current").textContent).toContain("销售：无");
    await waitFor(() => expect(screen.queryByTestId("bar-sales-backfill-mb-1")?.textContent ?? "").toContain("无销售（平台自有渠道）"));
  });

  it("95-R2：有改派权限且开台中给「改派来源与销售」；选营销带客才出销售账号，请求体为蛇形键 guest_source、sales_staff_id", async () => {
    const posts: Post[] = [];
    renderPage(posts);
    await waitFor(() => expect((screen.queryByTestId("bar-attribution-reassign-open") as HTMLButtonElement | null)?.disabled).toBe(false), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("bar-attribution-reassign-open"));
    await waitFor(() => expect(screen.queryByTestId("bar-attribution-reassign-form")).not.toBeNull());
    expect(screen.queryByTestId("bar-attribution-reassign-staff")).toBeNull();
    fireEvent.change(screen.getByTestId("bar-attribution-reassign-source"), { target: { value: "marketing" } });
    await waitFor(() => expect(screen.queryByTestId("bar-attribution-reassign-staff")).not.toBeNull());
    fireEvent.change(screen.getByTestId("bar-attribution-reassign-staff"), { target: { value: "u-c" } });
    fireEvent.click(screen.getByTestId("bar-attribution-reassign-form-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 3000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/table-sessions/s-1/attribution-reassign");
    expect(posts[0]!.body).toMatchObject({ guest_source: "marketing", sales_staff_id: "u-c" });
    expect(posts[0]!.body.idempotency_key).toBeTruthy();
  });

  it("95-R2：改为自来客不带销售账号", async () => {
    const posts: Post[] = [];
    renderPage(posts);
    await waitFor(() => expect((screen.queryByTestId("bar-attribution-reassign-open") as HTMLButtonElement | null)?.disabled).toBe(false), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("bar-attribution-reassign-open"));
    await waitFor(() => expect(screen.queryByTestId("bar-attribution-reassign-form")).not.toBeNull());
    fireEvent.change(screen.getByTestId("bar-attribution-reassign-source"), { target: { value: "walk_in" } });
    fireEvent.click(screen.getByTestId("bar-attribution-reassign-form-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 3000 });
    expect(posts[0]!.body.guest_source).toBe("walk_in");
    expect("sales_staff_id" in posts[0]!.body).toBe(false);
  });

  it("95-R2：没有改派权限时按钮不可点", async () => {
    renderPage([], [], { status: "ready", value: { grants: ["bar.table_session.view", "bar.table_session.create"], denies: [], wildcard: false } });
    await waitFor(() => expect((screen.queryByTestId("bar-attribution-reassign-open") as HTMLButtonElement | null)?.disabled).toBe(true), { timeout: 3000 });
  });
});
