import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import AdminApp from "./AdminApp";
import { AdminApi } from "./client/adminClient";
import { REPLAYED_NOTICE } from "../shared/operationKey";

// M725-F02（治理地图第 303 行「同一个操作被重复提交，系统只认一次」）：请求编号只按
// 「方法＋路径＋正文」认，正文里不一定带是谁；后台登出不刷新页面。同一标签页里换人登录，上一个人没拿到结果的
// 编号不能沿用到下一个人的同样内容上（沿用了，服务端会把下一个人的这次提交当成上一个人那一次的重复提交，不按新操作执行）。
function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}

describe("后台换人登录后的请求编号", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.localStorage.clear();
    window.history.replaceState({}, "", "/");
  });

  it("A 发券没拿到结果 → 登出 → B 登录发同样的券：换新编号；B 自己没拿到结果再点仍沿用 B 的首发编号", async () => {
    const issueKeys: string[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if (url.endsWith("/api/v1/vouchers/instances") && init?.method === "POST") {
          issueKeys.push(String(JSON.parse(String(init.body)).idempotency_key));
          // 前两次（A 一次、B 一次）响应丢在路上，第三次（B 再点）拿到结果。
          return issueKeys.length <= 2 ? Promise.reject(new TypeError("Failed to fetch")) : Promise.resolve(envelope({ quantity: 2 }));
        }
        if (url.endsWith("/api/v1/auth/login")) {
          return Promise.resolve(envelope({ access_token: "", session_id: "s-b", user: { id: "u-b", username: "staff_b", account_type: "platform_admin" } }));
        }
        return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
      }),
    );
    window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "u-a", username: "staff_a", accountType: "platform_admin" }, sessionId: "s-a" }));
    window.history.replaceState({}, "", "/admin");
    render(<AdminApp />);
    await waitFor(() => expect(screen.getByTestId("admin-shell")).toBeTruthy(), { timeout: 5000 });
    const issue = () => new AdminApi().post("/api/v1/vouchers/instances", { definitionId: "vdef_1", customerId: "customer-9", quantity: 2 });

    await expect(issue()).rejects.toThrow("Failed to fetch");
    fireEvent.click(screen.getByTestId("admin-logout"));
    await waitFor(() => expect(screen.getByTestId("login-view")).toBeTruthy(), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("login-username"), { target: { value: "staff_b" } });
    fireEvent.change(screen.getByTestId("login-password"), { target: { value: "pw-b" } });
    fireEvent.submit(screen.getByTestId("login-view"));
    await waitFor(() => expect(screen.getByTestId("admin-shell")).toBeTruthy(), { timeout: 5000 });

    await expect(issue()).rejects.toThrow("Failed to fetch");
    await expect(issue()).resolves.toEqual({ quantity: 2 });
    expect(issueKeys[1]).not.toBe(issueKeys[0]);
    // 第二次还能过吗：清过一次之后，B 自己没拿到结果再点，照常沿用 B 的首发编号。
    expect(issueKeys[2]).toBe(issueKeys[1]);
  });

  // 服务端按提交范围（账号与所属组织，带商户归属的按所属项目与商户）认同一编号的重复提交，与会话无关；同一个人登出后（或会话失效回登录页后）重新登录，
  // 此前没拿到结果的那一笔再提交仍是同一次操作，沿用首发编号，拿到第一次的结果（不再发一批券）。
  it("A 发券没拿到结果 → 登出 → A 再登录发同样的券：沿用 A 的首发编号，拿到第一次的结果并提示已完成", async () => {
    const issueKeys: string[] = [];
    const issued = new Map<string, unknown>();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if (url.endsWith("/api/v1/vouchers/instances") && init?.method === "POST") {
          // 服务端替身按幂等层约定：同一请求编号已执行过，回放第一次的结果并带 replayed；否则执行并记下结果。
          const key = String(JSON.parse(String(init.body)).idempotency_key);
          issueKeys.push(key);
          const first = issued.get(key);
          if (first) {
            return Promise.resolve(
              new Response(JSON.stringify({ success: true, code: "OK", message: REPLAYED_NOTICE, data: first, replayed: true }), {
                status: 200,
                headers: { "content-type": "application/json" },
              }),
            );
          }
          const result = { batch: `batch-${issued.size + 1}`, quantity: 2 };
          issued.set(key, result);
          // 每批的首发都已执行、响应丢在路上。
          return Promise.reject(new TypeError("Failed to fetch"));
        }
        if (url.endsWith("/api/v1/auth/login")) {
          return Promise.resolve(envelope({ access_token: "", session_id: "s-same-2", user: { id: "u-same", username: "staff_same", account_type: "platform_admin" } }));
        }
        return Promise.resolve(envelope({ items: [], page: 1, page_size: 20, total: 0 }));
      }),
    );
    window.localStorage.setItem("admin.session.v1", JSON.stringify({ user: { id: "u-same", username: "staff_same", accountType: "platform_admin" }, sessionId: "s-same-1" }));
    window.history.replaceState({}, "", "/admin");
    render(<AdminApp />);
    await waitFor(() => expect(screen.getByTestId("admin-shell")).toBeTruthy(), { timeout: 5000 });
    const issue = () => new AdminApi().post("/api/v1/vouchers/instances", { definitionId: "vdef_2", customerId: "customer-7", quantity: 2 });
    const logoutAndLoginAgain = async () => {
      fireEvent.click(screen.getByTestId("admin-logout"));
      await waitFor(() => expect(screen.getByTestId("login-view")).toBeTruthy(), { timeout: 5000 });
      fireEvent.change(screen.getByTestId("login-username"), { target: { value: "staff_same" } });
      fireEvent.change(screen.getByTestId("login-password"), { target: { value: "pw-same" } });
      fireEvent.submit(screen.getByTestId("login-view"));
      await waitFor(() => expect(screen.getByTestId("admin-shell")).toBeTruthy(), { timeout: 5000 });
    };

    await expect(issue()).rejects.toThrow("Failed to fetch");
    await logoutAndLoginAgain();
    await expect(issue()).resolves.toEqual({ batch: "batch-1", quantity: 2 });
    expect(issueKeys[1]).toBe(issueKeys[0]);
    await waitFor(() => expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE), { timeout: 5000 });

    // 第二次还能过吗：拿到结果后同样内容是新的一次（新编号）；这一次又没拿到结果，再登出、再以同一个人登录，仍沿用这一次的首发编号。
    await expect(issue()).rejects.toThrow("Failed to fetch");
    await logoutAndLoginAgain();
    await expect(issue()).resolves.toEqual({ batch: "batch-2", quantity: 2 });
    expect(issueKeys[2]).not.toBe(issueKeys[0]);
    expect(issueKeys[3]).toBe(issueKeys[2]);
    expect(issued.size).toBe(2);
  });
});
