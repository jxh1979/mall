// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AA 段第 206 条 206-R2：后台卡座表与卡座详情的台位状态按 ENUM:100 名称显示（「桌台/卡座/吧台位状态｜空闲、已预留、已开台、待清台、停用、异常」，
// PGM:287 状态全中文）；空闲台位可停用、停用台位可恢复（ENUM:100「台位停用与恢复由店长及以上操作，填原因留痕」），原因必填，按蛇形键发出。

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const TABLES = [
  { id: "t-idle", table_code: "A-01", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "active" },
  { id: "t-off", table_code: "A-02", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "disabled" },
  { id: "t-open", table_code: "A-03", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "opened" },
  { id: "t-clear", table_code: "A-04", bar_id: "bar-1", table_type: "card_seat", capacity: 6, status: "pending_clear" },
];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function list(items: unknown[]): Response {
  return envelope({ items, page: 1, page_size: 50, total: items.length });
}

function renderPage(posts: { url: string; body: Record<string, unknown> }[], perms: EffectivePermissionsState = ADMIN_PERMS) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(envelope({ id: "t-idle", status: "disabled" }));
    }
    if (url.includes("/platform/merchants")) return Promise.resolve(list([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
    if (url.includes("/bar/table-sessions")) return Promise.resolve(list([]));
    if (url.includes("/bar/tables")) return Promise.resolve(list(TABLES));
    return Promise.resolve(list([]));
  }));
  const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "店长", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: perms };
  render(<BarTableSessionsPage {...props} />);
}

describe("BarTableSessionsPage 第 206 条台位停用与恢复", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("卡座表与卡座详情的台位状态显示中文名；空闲行有停用、停用行有恢复，已开台与待清台没有", async () => {
    renderPage([]);
    await waitFor(() => expect(screen.queryByTestId("bar-table-row-t-idle")?.textContent ?? "").toContain("空闲"), { timeout: 3000 });
    expect(screen.getByTestId("bar-table-row-t-off").textContent).toContain("停用");
    expect(screen.getByTestId("bar-table-row-t-open").textContent).toContain("已开台");
    expect(screen.getByTestId("bar-table-row-t-clear").textContent).toContain("待清台");
    expect(screen.getByTestId("bar-tables-table").textContent ?? "").not.toMatch(/active|disabled|opened|pending_clear|有效|已禁用/);
    expect(screen.getByTestId("bar-table-disable-t-idle").textContent).toBe("停用");
    expect(screen.getByTestId("bar-table-enable-t-off").textContent).toBe("恢复");
    expect(screen.queryByTestId("bar-table-enable-t-idle")).toBeNull();
    expect(screen.queryByTestId("bar-table-disable-t-off")).toBeNull();
    for (const id of ["t-open", "t-clear"]) {
      expect(screen.queryByTestId(`bar-table-disable-${id}`)).toBeNull();
      expect(screen.queryByTestId(`bar-table-enable-${id}`)).toBeNull();
    }
    fireEvent.click(screen.getByTestId("bar-table-row-t-open"));
    const drawer = screen.getByTestId("row-detail-drawer");
    expect(drawer.textContent).toContain("已开台");
    expect(drawer.textContent).not.toContain("opened");
  });

  it("停用须填原因：不填不发请求；填了按蛇形键发 POST …/disable，恢复发 …/enable", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts);
    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    await waitFor(() => expect(screen.queryByTestId("bar-table-disable-t-idle")).not.toBeNull(), { timeout: 3000 });
    const prompt = vi.spyOn(window, "prompt").mockReturnValueOnce("  ").mockReturnValueOnce("设备检修").mockReturnValueOnce("检修完成");

    fireEvent.click(screen.getByTestId("bar-table-disable-t-idle"));
    expect(screen.getByTestId("bar-create-error").textContent).toContain("原因");
    expect(posts).toHaveLength(0);

    fireEvent.click(screen.getByTestId("bar-table-disable-t-idle"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/tables/t-idle/disable");
    expect(posts[0]!.body.reason).toBe("设备检修");
    expect(posts[0]!.body.idempotency_key).toBeTruthy();

    await waitFor(() => expect((screen.getByTestId("bar-table-enable-t-off") as HTMLButtonElement).disabled).toBe(false), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("bar-table-enable-t-off"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(posts[1]!.url).toContain("/api/v1/bar/tables/t-off/enable");
    expect(posts[1]!.body.reason).toBe("检修完成");
    expect(prompt).toHaveBeenCalledTimes(3);
  });

  it("没有 bar.table.disable 时停用、恢复按钮不可用", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    renderPage(posts, { status: "ready", value: { grants: ["bar.table_session.view"], denies: [], wildcard: false } });
    await waitFor(() => expect(screen.queryByTestId("bar-table-disable-t-idle")).not.toBeNull(), { timeout: 3000 });
    expect((screen.getByTestId("bar-table-disable-t-idle") as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByTestId("bar-table-enable-t-off") as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(screen.getByTestId("bar-table-disable-t-idle"));
    expect(posts).toHaveLength(0);
  });
});
