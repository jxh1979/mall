// 文件版本：V0.01
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import { VALUE_LABELS } from "../i18n/fieldLabels";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AU 段第 265 条 265-R2（BSM:577「卡座预订这类业务记录，现在也支持存档、以及审批后删除」「存档及删除都需要填写理由」；
//   BSM:569「想恢复点「取消存档」即可」）：后台「卡座与订台」页订台行给存档、取消存档、申请删除（最高权限「直接删除」）、批准与驳回入口；
//   勾「显示已存档订台」才带 include_archived=true 重新拉取。真实 AdminApi（蛇形键响应经客户端转驼峰），假响应用文本体。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const STAFF = { id: "u1", username: "店长", accountType: "staff" };
const MASTER = { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" };
const TABLES = [{ id: "t-1", table_code: "A-01", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "active" }];
const ACTIVE = { id: "r1", bar_id: "bar-1", table_id: "t-1", guest_count: 4, status: "cancelled", deposit_status: "not_required", delete_status: "none" };
const ARCHIVED = { id: "r2", bar_id: "bar-1", table_id: "t-1", guest_count: 2, status: "arrived", deposit_status: "not_required",
  archived_at: "2026-11-01T00:00:00Z", delete_status: "none" };
const PENDING = { id: "r3", bar_id: "bar-1", table_id: "t-1", guest_count: 2, status: "cancelled", deposit_status: "not_required",
  archived_at: "2026-11-01T00:00:00Z", delete_status: "pending" };

function okText(data: string): Response {
  return new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`, { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: unknown[]): Response {
  return okText(`{"items":${JSON.stringify(items)},"page":1,"page_size":50,"total":${items.length}}`);
}

type Post = { url: string; body: Record<string, unknown> };

function renderPage(opts: { user?: typeof STAFF; perms?: EffectivePermissionsState; archived?: unknown[] } = {}) {
  const posts: Post[] = [];
  const gets: string[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(okText(url.endsWith("/delete-approve") ? '{"id":"r3","decision":"approved","deleted":true}' : '{"id":"r1"}'));
    }
    gets.push(url);
    if (url.includes("/platform/merchants")) return Promise.resolve(list([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
    if (url.includes("/bar/table-sessions")) return Promise.resolve(list([]));
    if (url.includes("/bar/tables")) return Promise.resolve(list(TABLES));
    if (url.includes("/bar/reservations")) return Promise.resolve(list(url.includes("include_archived=true") ? [ACTIVE, ...(opts.archived ?? [ARCHIVED])] : [ACTIVE]));
    return Promise.resolve(list([]));
  }));
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: opts.user ?? STAFF, setActionResult, setGlobalError,
    effectivePermissions: opts.perms ?? ADMIN_PERMS };
  render(<BarTableSessionsPage {...props} />);
  return { posts, gets, setActionResult, setGlobalError };
}

/** 等订台行出现、页面被动 effect（酒吧下拉默认值）落定。 */
async function settled(rowId: string) {
  await waitFor(() => expect(screen.queryByTestId(`bar-reservation-row-${rowId}`)).not.toBeNull(), { timeout: 3000 });
  await waitFor(() => expect((screen.queryByTestId("bar-new-reservation-bar") as HTMLSelectElement | null)?.value).toBe("bar-1"), { timeout: 5000 });
}

/** 勾「显示已存档订台」，等已存档行出现。 */
async function showArchived(rowId: string) {
  await waitFor(() => expect(screen.queryByTestId("bar-reservations-include-archived")).not.toBeNull());
  fireEvent.click(screen.getByTestId("bar-reservations-include-archived"));
  await waitFor(() => expect(screen.queryByTestId(`bar-reservation-row-${rowId}`)).not.toBeNull(), { timeout: 3000 });
}

/** 已存档、待审批行有两个动作，收在「操作▾」下拉里：展开后等动作项文字落定。 */
async function openActions(rowId: string, testId: string, label: string) {
  fireEvent.click(screen.getByTestId(`bar-reservation-actions-${rowId}`));
  await waitFor(() => expect(screen.queryByTestId(testId)?.textContent).toBe(label));
}

const reservationLoads = (gets: string[]) => gets.filter((url) => url.includes("/bar/reservations")).length;

describe("BarTableSessionsPage 第 265 条订台存档与删除", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); cleanup(); });

  it("店长存档未存档的订台：理由为空不发请求；填理由后 POST …/archive，成功后刷新列表", async () => {
    const { posts, gets, setActionResult, setGlobalError } = renderPage();
    await settled("r1");
    // antd 按钮两个汉字时会在中间加空格。
    await waitFor(() => expect(screen.queryByTestId("bar-reservation-archive-r1")?.textContent ?? "").toMatch(/^存\s?档$/));

    vi.spyOn(window, "prompt").mockReturnValueOnce("  ");
    fireEvent.click(screen.getByTestId("bar-reservation-archive-r1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("存档理由必填"));
    expect(posts).toHaveLength(0);

    vi.spyOn(window, "prompt").mockReturnValueOnce(" 客人改期，清理列表 ");
    const before = reservationLoads(gets);
    fireEvent.click(screen.getByTestId("bar-reservation-archive-r1"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toMatch(/\/api\/v1\/bar\/reservations\/r1\/archive$/);
    expect(posts[0]!.body).toMatchObject({ reason: "客人改期，清理列表" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_reservation_archived", id: "r1" })));
    await waitFor(() => expect(reservationLoads(gets)).toBe(before + 1));
    expect(VALUE_LABELS.bar_reservation_archived).toBe("订台已存档");
  });

  it("勾「显示已存档订台」带 include_archived=true 拉取；已存档行显示「已存档」，可取消存档、申请删除", async () => {
    const { posts, gets, setActionResult } = renderPage();
    await settled("r1");
    expect(gets.some((url) => url.includes("include_archived"))).toBe(false);

    await showArchived("r2");
    expect(gets.some((url) => /\/api\/v1\/bar\/reservations\?include_archived=true$/.test(url))).toBe(true);
    expect(screen.getByTestId("bar-reservation-row-r2").textContent).toContain("已存档");

    await openActions("r2", "bar-reservation-unarchive-r2", "取消存档");
    expect(screen.getByTestId("bar-reservation-delete-request-r2")).toHaveTextContent("申请删除");
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("重复录入的订台");
    fireEvent.click(screen.getByTestId("bar-reservation-delete-request-r2"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(prompt).toHaveBeenCalledWith("删除理由（提交后需最高权限审批）：");
    expect(posts[0]!.url).toMatch(/\/api\/v1\/bar\/reservations\/r2\/delete-request$/);
    expect(posts[0]!.body).toMatchObject({ reason: "重复录入的订台" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_reservation_delete_requested", id: "r2" })));
  });

  it("最高权限本人：已存档行为「直接删除」，提交即删，结果为订台删除已批准", async () => {
    const { posts, setActionResult } = renderPage({ user: MASTER });
    await settled("r1");
    await showArchived("r2");

    await openActions("r2", "bar-reservation-delete-request-r2", "直接删除");
    const prompt = vi.spyOn(window, "prompt").mockReturnValue("重复录入的订台");
    fireEvent.click(screen.getByTestId("bar-reservation-delete-request-r2"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(prompt).toHaveBeenCalledWith("删除理由（最高权限本人发起即删除）：");
    expect(posts[0]!.url).toMatch(/\/api\/v1\/bar\/reservations\/r2\/delete-request$/);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "bar_reservation_delete_approved", id: "r2", deleted: true })));
    expect(VALUE_LABELS.bar_reservation_delete_approved).toBe("订台删除已批准");
  });

  it("待审批删除的行：持 bar.reservation.approve 出「批准删除」「驳回」，批准发 decision=approved；没有该权限不出这两个按钮", async () => {
    const base = ["bar.table.view", "bar.table_session.view", "bar.reservation.view", "bar.reservation.manage"];
    const { posts, setActionResult } = renderPage({ perms: { status: "ready", value: { grants: [...base, "bar.reservation.approve"], denies: [], wildcard: false } }, archived: [PENDING] });
    await settled("r1");
    await showArchived("r3");
    expect(screen.getByTestId("bar-reservation-row-r3").textContent).toContain("待审批删除");

    await openActions("r3", "bar-reservation-delete-approve-r3", "批准删除");
    expect(screen.getByTestId("bar-reservation-delete-reject-r3")).toHaveTextContent("驳回");
    fireEvent.click(screen.getByTestId("bar-reservation-delete-approve-r3"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toMatch(/\/api\/v1\/bar\/reservations\/r3\/delete-approve$/);
    expect(posts[0]!.body).toMatchObject({ decision: "approved" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
      expect.objectContaining({ action: "bar_reservation_delete_approved", id: "r3", deleted: true })));
    cleanup();

    renderPage({ perms: { status: "ready", value: { grants: base, denies: [], wildcard: false } }, archived: [PENDING] });
    await settled("r1");
    await showArchived("r3");
    await waitFor(() => expect(screen.queryByTestId("bar-reservation-actions-r3-none")).not.toBeNull());
    expect(screen.queryByTestId("bar-reservation-delete-approve-r3")).toBeNull();
    expect(screen.queryByTestId("bar-reservation-delete-reject-r3")).toBeNull();
  });
});
