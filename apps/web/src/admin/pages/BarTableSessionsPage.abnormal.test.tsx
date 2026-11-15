// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import { VALUE_LABELS } from "../i18n/fieldLabels";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AK 段第 226 条 226-R2（ENUM:100「桌台/卡座/吧台位状态｜空闲、已预留、已开台、待清台、停用、异常。」
// 「异常由系统告警或人工登记进入，处置完成后由当班主管确认恢复，全程留痕。」；PGM:287 状态全中文；第十八批裁决 53）：
// 后台卡座表异常状态显示中文名；空闲、待清台行可「登记异常」，异常行可「确认恢复」，原因必填，按蛇形键发出。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TABLES = [
  { id: "t-idle", table_code: "A-01", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "active" },
  { id: "t-held", table_code: "A-02", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "active", held_reservation_id: "resv-1" },
  { id: "t-open", table_code: "A-03", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "opened" },
  { id: "t-clear", table_code: "A-04", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "pending_clear" },
  { id: "t-off", table_code: "A-05", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "disabled" },
  { id: "t-bad", table_code: "A-06", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "abnormal" },
];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: unknown[]): Response {
  return envelope({ items, page: 1, page_size: 50, total: items.length });
}

type Post = { url: string; body: Record<string, unknown> };

function renderPage(posts: Post[], perms: EffectivePermissionsState = ADMIN_PERMS) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(envelope({ id: "t-idle", status: "abnormal" }));
    }
    if (url.includes("/platform/merchants")) return Promise.resolve(list([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
    if (url.includes("/bar/table-sessions")) return Promise.resolve(list([]));
    if (url.includes("/bar/tables")) return Promise.resolve(list(TABLES));
    return Promise.resolve(list([]));
  }));
  const setActionResult = vi.fn();
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "当班主管", accountType: "staff" }, setActionResult, setGlobalError: vi.fn(), effectivePermissions: perms };
  render(<BarTableSessionsPage {...props} />);
  return { setActionResult };
}

describe("BarTableSessionsPage 第 226 条台位异常登记与确认恢复", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("异常行状态显示「异常」并出现「确认恢复」；空闲、待清台行有「登记异常」，已预留、已开台、停用行没有", async () => {
    renderPage([]);
    await waitFor(() => expect(screen.queryByTestId("bar-table-abnormal-resolve-t-bad")?.textContent).toBe("确认恢复"), { timeout: 3000 });
    expect(screen.getByTestId("bar-table-row-t-bad").textContent).toContain("异常");
    expect(screen.getByTestId("bar-tables-table").textContent ?? "").not.toMatch(/abnormal/);
    expect(screen.getByTestId("bar-table-abnormal-mark-t-idle").textContent).toBe("登记异常");
    expect(screen.getByTestId("bar-table-abnormal-mark-t-clear").textContent).toBe("登记异常");
    for (const id of ["t-held", "t-open", "t-off", "t-bad"]) {
      expect(screen.queryByTestId(`bar-table-abnormal-mark-${id}`)).toBeNull();
    }
    for (const id of ["t-idle", "t-held", "t-open", "t-clear", "t-off"]) {
      expect(screen.queryByTestId(`bar-table-abnormal-resolve-${id}`)).toBeNull();
    }
    // 异常台位不能停用，也不是停用台位的「恢复」。
    expect(screen.queryByTestId("bar-table-disable-t-bad")).toBeNull();
    expect(screen.queryByTestId("bar-table-enable-t-bad")).toBeNull();
    fireEvent.click(screen.getByTestId("bar-table-row-t-bad"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("异常");
    expect(drawer.textContent).not.toContain("abnormal");
  });

  it("登记异常须填原因：不填不发请求；填了按蛇形键发 POST …/abnormal，确认恢复发 …/abnormal/resolve", async () => {
    const posts: Post[] = [];
    const { setActionResult } = renderPage(posts);
    await waitFor(() => expect(screen.queryByTestId("bar-table-abnormal-mark-t-idle")).not.toBeNull(), { timeout: 3000 });
    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    const prompt = vi.spyOn(window, "prompt").mockReturnValueOnce("  ").mockReturnValueOnce(" 沙发破损 ").mockReturnValueOnce("已修好");

    fireEvent.click(screen.getByTestId("bar-table-abnormal-mark-t-idle"));
    expect(screen.getByTestId("bar-create-error").textContent).toContain("原因");
    expect(posts).toHaveLength(0);

    fireEvent.click(screen.getByTestId("bar-table-abnormal-mark-t-idle"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toMatch(/\/api\/v1\/bar\/tables\/t-idle\/abnormal$/);
    expect(posts[0]!.body.reason).toBe("沙发破损");
    expect(posts[0]!.body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_table_abnormal_marked", tableId: "t-idle" })));

    await waitFor(() => expect((screen.getByTestId("bar-table-abnormal-resolve-t-bad") as HTMLButtonElement).disabled).toBe(false), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("bar-table-abnormal-resolve-t-bad"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(posts[1]!.url).toMatch(/\/api\/v1\/bar\/tables\/t-bad\/abnormal\/resolve$/);
    expect(posts[1]!.body.reason).toBe("已修好");
    expect(posts[1]!.body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_table_abnormal_resolved", tableId: "t-bad" })));
    expect(prompt).toHaveBeenCalledTimes(3);
    expect(VALUE_LABELS.bar_table_abnormal_marked).toBe("台位已登记异常");
    expect(VALUE_LABELS.bar_table_abnormal_resolved).toBe("台位已确认恢复");
  });

  it("没有 bar.table.open_close 时登记异常、确认恢复按钮不可用", async () => {
    const posts: Post[] = [];
    renderPage(posts, { status: "ready", value: { grants: ["bar.table_session.view"], denies: [], wildcard: false } });
    await waitFor(() => expect((screen.queryByTestId("bar-table-abnormal-mark-t-idle") as HTMLButtonElement | null)?.disabled).toBe(true), { timeout: 3000 });
    expect((screen.getByTestId("bar-table-abnormal-resolve-t-bad") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("bar-table-abnormal-mark-t-idle"));
    expect(posts).toHaveLength(0);
  });
});
