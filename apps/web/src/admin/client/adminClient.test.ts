import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "./adminClient";
import { ApiError } from "../../shared/apiError";

function jsonResponse(data: unknown): Response {
  return new Response(JSON.stringify({ code: "OK", message: "ok", data }), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}

function errorResponse(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

function clearCookies(): void {
  for (const part of document.cookie.split(";")) {
    const name = part.split("=")[0]?.trim();
    if (name) document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  }
}

describe("AdminApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    clearCookies();
  });

  it("unwraps API envelopes, sends cookies, and never adds an Authorization header", async () => {
    // 令牌迁 HttpOnly Cookie：请求不再手动加 Authorization 头，改由 credentials:include 让浏览器自动携带 Cookie。
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ items: [{ ticket_type_id: "ticket-1" }] }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(new AdminApi().get("/api/v1/ticketing/ticket-types")).resolves.toEqual([
      { ticketTypeId: "ticket-1" },
    ]);

    const init = fetchMock.mock.calls[0]![1] as RequestInit;
    expect((init.headers as Headers).get("authorization")).toBeNull();
    expect(init.credentials).toBe("include");
  });

  it("attaches the CSRF double-submit header on writes when XSRF-TOKEN cookie is present", async () => {
    document.cookie = "XSRF-TOKEN=csrf-abc; path=/";
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await new AdminApi().post("/api/v1/ticketing/orders", { ticketTypeId: "ticket-1" });

    const init = fetchMock.mock.calls[0]![1] as RequestInit;
    expect((init.headers as Headers).get("x-xsrf-token")).toBe("csrf-abc");
  });

  it("does not attach the CSRF header on safe reads", async () => {
    document.cookie = "XSRF-TOKEN=csrf-abc; path=/";
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ items: [] }));
    vi.stubGlobal("fetch", fetchMock);

    await new AdminApi().get("/api/v1/ticketing/ticket-types");

    const init = fetchMock.mock.calls[0]![1] as RequestInit;
    expect((init.headers as Headers).get("x-xsrf-token")).toBeNull();
  });

  it("keeps dotted dictionary keys verbatim (metric maps stay addressable)", async () => {
    // BI S2-S5:dashboards/{domain} 的 metrics 是字典(key="ar.aging_31_60"),
    // 字段名转换不得吞掉字典 key 里的下划线,否则真后端下页面读不到指标。
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      business_date: "2026-07-01",
      metrics: { "ar.aging_31_60": { value: 900, unit: "cent" } },
    }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(new AdminApi().get("/api/v1/analytics/dashboards/finance")).resolves.toEqual({
      businessDate: "2026-07-01",
      metrics: { "ar.aging_31_60": { value: 900, unit: "cent" } },
    });
  });

  it("normalizes request keys and appends an idempotency key", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await new AdminApi().post("/api/v1/ticketing/orders", { ticketTypeId: "ticket-1" });

    const init = fetchMock.mock.calls[0]![1] as RequestInit;
    expect(JSON.parse(String(init?.body))).toMatchObject({
      ticket_type_id: "ticket-1",
      idempotency_key: expect.stringMatching(/^admin-web-/),
    });
  });

  it("translates backend errors into Chinese business messages with technical detail attached", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      errorResponse(401, { code: "UNAUTHORIZED", detail_code: "INVALID_CREDENTIALS", message: "invalid username or password", request_id: "req-1" }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const error = await new AdminApi().post("/api/v1/auth/login", { loginName: "admin" }).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toBe("账号或密码不正确");
    expect((error as ApiError).detail).toMatchObject({ path: "/api/v1/auth/login", status: 401, requestId: "req-1" });
  });

  it("maps FORBIDDEN / NOT_FOUND / 5xx to business wording", async () => {
    const cases: Array<[Response, string]> = [
      [errorResponse(403, { code: "FORBIDDEN", message: "forbidden" }), "当前账号无权进行此操作"],
      [errorResponse(404, { code: "NOT_FOUND", message: "missing" }), "数据不存在或已被删除"],
      [errorResponse(500, { code: "INTERNAL_ERROR", message: "boom" }), "服务暂时不可用，请稍后重试"],
      [new Response("<html>bad gateway</html>", { status: 502 }), "服务暂时不可用，请稍后重试"],
      [errorResponse(400, { code: "PARAM_INVALID", detail_code: "VALIDATION_FAILED", message: "字段缺失" }), "字段缺失"],
    ];
    for (const [response, expected] of cases) {
      vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
      await expect(new AdminApi().get("/api/v1/x")).rejects.toThrow(expected);
    }
  });

  it("refreshes on 401 via the cookie refresh flow and replays the original request once", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(errorResponse(401, { code: "UNAUTHORIZED", message: "expired" }))
      .mockResolvedValueOnce(jsonResponse({ user: { id: "u-1" } })) // /auth/refresh：后端已用 Set-Cookie 轮转令牌
      .mockResolvedValueOnce(jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    const api = new AdminApi();
    await expect(api.get("/api/v1/ticketing/orders")).resolves.toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(3);

    const refreshCall = fetchMock.mock.calls[1]!;
    expect(String(refreshCall[0])).toContain("/api/v1/auth/refresh");
    expect((refreshCall[1] as RequestInit).credentials).toBe("include");
    // 刷新令牌来自 HttpOnly Cookie，请求体不再携带 refresh_token（JS 读不到它）。
    expect(String((refreshCall[1] as RequestInit).body)).not.toContain("refresh_token");
    // 重放不带 Authorization 头（靠 Cookie）。
    const replayHeaders = (fetchMock.mock.calls[2]![1] as RequestInit).headers as Headers;
    expect(replayHeaders.get("authorization")).toBeNull();
  });

  it("expires the session when refresh fails", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(errorResponse(401, { code: "UNAUTHORIZED", message: "expired" }))
      .mockResolvedValueOnce(errorResponse(401, { code: "UNAUTHORIZED", message: "refresh rejected" }));
    vi.stubGlobal("fetch", fetchMock);

    const api = new AdminApi();
    const expired = vi.fn();
    api.onSessionExpired = expired;

    await expect(api.get("/api/v1/ticketing/orders")).rejects.toThrow("登录已过期，请重新登录");
    expect(expired).toHaveBeenCalledTimes(1);
    // 原请求 401 → 尝试一次 Cookie 刷新（也 401）→ 回登录。
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("does not expire the session on 403 (real permission denial stays on page)", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      errorResponse(403, { code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "denied" }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const api = new AdminApi();
    const expired = vi.fn();
    api.onSessionExpired = expired;

    await expect(api.get("/api/v1/ticketing/orders")).rejects.toThrow("当前账号无权进行此操作");
    expect(expired).not.toHaveBeenCalled();
  });

  it("notifies session expiry only once for concurrent 401s", async () => {
    const fetchMock = vi.fn().mockResolvedValue(errorResponse(401, { code: "UNAUTHORIZED", message: "expired" }));
    vi.stubGlobal("fetch", fetchMock);

    const api = new AdminApi();
    const expired = vi.fn();
    api.onSessionExpired = expired;

    const results = await Promise.allSettled([api.get("/api/v1/a"), api.get("/api/v1/b"), api.post("/api/v1/c")]);
    expect(results.every((r) => r.status === "rejected")).toBe(true);
    expect(expired).toHaveBeenCalledTimes(1);
  });

  it("does not treat login failure as session expiry", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      errorResponse(401, { code: "UNAUTHORIZED", detail_code: "INVALID_CREDENTIALS", message: "bad credentials" }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const api = new AdminApi();
    const expired = vi.fn();
    api.onSessionExpired = expired;

    await expect(api.post("/api/v1/auth/login", { loginName: "admin" })).rejects.toThrow();
    expect(expired).not.toHaveBeenCalled();
  });
});
