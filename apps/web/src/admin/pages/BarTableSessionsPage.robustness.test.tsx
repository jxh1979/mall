import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarTableSessionsPage } from "./BarTableSessionsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const TABLES = [{ id: "t-1", name: "卡座A", bar_id: "bar-1", status: "idle" }];
const SESSIONS = [{ id: "sess-1", table_id: "t-1", status: "open" }];

function dataEnvelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}
function listEnvelope(items: unknown[]): Response {
  return dataEnvelope({ items, page: 1, page_size: 50, total: items.length });
}
function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "forbidden", request_id: "req-403" }), { status: 403, headers: { "content-type": "application/json" } });
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

describe("BarTableSessionsPage 副资源降级（副资源加载健壮性批）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("订台预定接口 403 → 卡座与开台会话主内容照常渲染，预定区块降级提示，整页不报错（原丢弃的 /bar/orders 拉取已删除，不再拖垮整页）", async () => {
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/bar/reservations")) return Promise.resolve(forbidden());
      if (url.includes("/bar/table-sessions")) return Promise.resolve(listEnvelope(SESSIONS));
      if (url.includes("/bar/tables")) return Promise.resolve(listEnvelope(TABLES));
      return Promise.resolve(listEnvelope([]));
    }));
    const setGlobalError = vi.fn();
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError, effectivePermissions: ADMIN_PERMS };
    render(<BarTableSessionsPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("bar-reservations-degraded")).toBeTruthy());
    expect(screen.getByTestId("bar-tables-table")).toBeTruthy();
    expect(screen.queryByTestId("bar-reservations-table")).toBeNull();
    expect(screen.queryByTestId("admin-error")).toBeNull();
    expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String));
  });
});

// M725-F02（治理地图第 303 行「同一个操作被重复提交，系统只认一次」）：新建订台第一次提交服务端可能已订上、响应却丢了，
// 表单留着，操作员再点——这是同一次订台，不是新的一次：订台时间沿用第一次点击时取的，请求内容逐字不变，带首发的提交编号。
describe("BarTableSessionsPage 新建订台没拿到结果再点", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("第二次请求的内容与提交编号都与第一次相同", async () => {
    // 页面按点击取当前时刻；让每次取到的时刻必然不同。
    const realToISOString = Date.prototype.toISOString;
    let now = 1_760_000_000_000;
    vi.spyOn(Date.prototype, "toISOString").mockImplementation(() => realToISOString.call(new Date((now += 1000))));
    const posts: string[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push(String(init?.body));
        // 第一次：响应丢在路上。
        if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
        return Promise.resolve(dataEnvelope({ id: "reservation_1", table_id: "t-1", status: "reserved" }));
      }
      if (url.includes("/platform/merchants")) return Promise.resolve(listEnvelope([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
      if (url.includes("/bar/table-sessions")) return Promise.resolve(listEnvelope(SESSIONS));
      if (url.includes("/bar/tables")) return Promise.resolve(listEnvelope(TABLES));
      return Promise.resolve(listEnvelope([]));
    }));
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<BarTableSessionsPage {...props} />);

    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("bar-new-reservation-table"), { target: { value: "t-1" } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    await waitFor(() => expect(screen.getByTestId("bar-create-error").textContent).toContain("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(JSON.parse(posts[1]!).reservation_time).toBe(JSON.parse(posts[0]!).reservation_time);
    expect(JSON.parse(posts[1]!).idempotency_key).toBe(JSON.parse(posts[0]!).idempotency_key);
    expect(posts[1]).toBe(posts[0]);
  });

  // 记住的订台时间跟着「这次订台」走，不跟着页面组件走：第一次没拿到结果后切到别的菜单再回来（页面重新挂载），先给另一张卡座
  // 订成了，回头把第一张再订一遍（卡座号后多敲了一个空格，发出的内容不变）——仍是同一次订台。
  it("切页回来、中间订成了另一张卡座，再订第一张：请求内容与提交编号仍与第一次相同", async () => {
    // 页面按点击取当前时刻；让每次取到的时刻必然不同。
    const realToISOString = Date.prototype.toISOString;
    let now = 1_760_000_000_000;
    vi.spyOn(Date.prototype, "toISOString").mockImplementation(() => realToISOString.call(new Date((now += 1000))));
    const posts: string[] = [];
    vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push(String(init?.body));
        // 第一次：响应丢在路上。
        if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
        return Promise.resolve(dataEnvelope({ id: "reservation_1", table_id: "t-1", status: "reserved" }));
      }
      if (url.includes("/platform/merchants")) return Promise.resolve(listEnvelope([{ id: "bar-1", merchant_name: "酒吧甲", merchant_type: "bar" }]));
      if (url.includes("/bar/table-sessions")) return Promise.resolve(listEnvelope(SESSIONS));
      if (url.includes("/bar/tables")) return Promise.resolve(listEnvelope(TABLES));
      return Promise.resolve(listEnvelope([]));
    }));
    const props: AdminPageProps = { api: new AdminApi("test-token"), user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    const firstVisit = render(<BarTableSessionsPage {...props} />);

    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("bar-new-reservation-table"), { target: { value: "t-7" } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    await waitFor(() => expect(screen.getByTestId("bar-create-error").textContent).toContain("Failed to fetch"), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    // 切到别的菜单再回来：页面重新挂载。
    firstVisit.unmount();
    render(<BarTableSessionsPage {...props} />);
    await waitFor(() => expect((screen.getByTestId("bar-new-reservation-bar") as HTMLSelectElement).value).toBe("bar-1"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("bar-new-reservation-table"), { target: { value: "t-8" } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    // 另一张订成了：表单清掉卡座号，按钮恢复可点。
    await waitFor(() => {
      expect((screen.getByTestId("bar-new-reservation-table") as HTMLInputElement).value).toBe("");
      expect((screen.getByTestId("bar-new-reservation-submit") as HTMLButtonElement).disabled).toBe(false);
    }, { timeout: 5000 });

    fireEvent.change(screen.getByTestId("bar-new-reservation-table"), { target: { value: "t-7 " } });
    fireEvent.click(screen.getByTestId("bar-new-reservation-submit"));
    await waitFor(() => expect(posts).toHaveLength(3), { timeout: 5000 });
    expect(JSON.parse(posts[2]!).reservation_time).toBe(JSON.parse(posts[0]!).reservation_time);
    expect(JSON.parse(posts[2]!).idempotency_key).toBe(JSON.parse(posts[0]!).idempotency_key);
    expect(posts[2]).toBe(posts[0]);
  });
});
