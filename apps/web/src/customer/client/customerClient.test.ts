import { afterEach, describe, expect, it, vi } from "vitest";

import {
  CustomerApi,
  customerPhoneLogin,
  customerRestoreSession,
  deriveRefreshIntervalSeconds,
  normalizeRequestBody,
  setCustomerSessionExpiredHandler,
  unwrap,
} from "./customerClient";
import { ApiError } from "../../shared/apiError";

function jsonResponse(data: unknown): Response {
  return new Response(JSON.stringify({ code: "OK", message: "ok", data }), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}

describe("customerClient unwrap/normalize", () => {
  it("unwraps envelope and extracts items list", () => {
    expect(unwrap({ code: "OK", data: { items: [{ ticket_type_id: "t-1" }] } })).toEqual([{ ticketTypeId: "t-1" }]);
  });

  it("unwraps envelope object and camelCases keys", () => {
    expect(unwrap({ code: "OK", data: { customer_id: "c-1", masked_phone: "138****0000" } })).toEqual({
      customerId: "c-1",
      maskedPhone: "138****0000",
    });
  });

  it("snake_cases request body and keeps provided idempotency key", () => {
    expect(normalizeRequestBody({ ticketTypeId: "t-1", quantity: 2 }, "k-1")).toEqual({
      ticket_type_id: "t-1",
      quantity: 2,
      idempotency_key: "k-1",
    });
  });
});

describe("deriveRefreshIntervalSeconds", () => {
  it("returns 60 by default when timestamps missing", () => {
    expect(deriveRefreshIntervalSeconds(null, null)).toBe(60);
  });

  it("derives 30 for a short window", () => {
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:00:00Z", "2026-06-18T00:00:30Z")).toBe(30);
  });

  it("derives 60 for a 60s window", () => {
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:00:00Z", "2026-06-18T00:01:00Z")).toBe(60);
  });

  it("falls back to 60 for non-positive window", () => {
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:01:00Z", "2026-06-18T00:00:00Z")).toBe(60);
  });
});

describe("CustomerApi requests", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("sends Bearer + X-Customer-Token headers on GET", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ items: [] }));
    vi.stubGlobal("fetch", fetchMock);

    await new CustomerApi("tok").get("/api/v1/customer/orders");

    const init = fetchMock.mock.calls[0]![1] as RequestInit;
    const headers = init.headers as Headers;
    expect(headers.get("authorization")).toBe("Bearer tok");
    expect(headers.get("x-customer-token")).toBe("tok");
  });

  it("adds idempotency header + body key on POST", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: "o-1" }));
    vi.stubGlobal("fetch", fetchMock);

    await new CustomerApi("tok").post("/api/v1/customer/ticket-orders", { ticketTypeId: "t-1", quantity: 1 });

    const init = fetchMock.mock.calls[0]![1] as RequestInit;
    const headers = init.headers as Headers;
    const idemHeader = headers.get("idempotency-key");
    expect(idemHeader).toMatch(/^customer-web-/);
    const body = JSON.parse(String(init.body));
    expect(body).toMatchObject({ ticket_type_id: "t-1", quantity: 1, idempotency_key: idemHeader });
  });

  it("phoneLogin posts without auth header", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ customer_id: "c-1", customer_token: "tok", masked_phone: "138****0000" }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await customerPhoneLogin({ phone: "13800000000", smsCode: "1234" });

    expect(result.customerToken).toBe("tok");
    const init = fetchMock.mock.calls[0]![1] as RequestInit;
    expect((init.headers as Headers).get("authorization")).toBeNull();
  });

  it("throws business-worded errors on non-OK responses (technical info in detail)", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("boom", { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    const error = await new CustomerApi("tok").get("/api/v1/customer/points").catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toBe("登录已过期，请重新登录");
    expect((error as ApiError).detail).toMatchObject({ path: "/api/v1/customer/points", status: 401, raw: "boom" });
  });

  it("translates invalid credentials into business wording", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: "UNAUTHORIZED", detail_code: "INVALID_CREDENTIALS", message: "invalid" }), { status: 401 }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(customerPhoneLogin({ phone: "13800000000", smsCode: "0000" })).rejects.toThrow("账号或密码不正确");
  });

  it("restores null on anonymous 204 probe without throwing (#7)", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    // 匿名探测：后端 204 空会话，前端视为「未登录正常态」返回 null（不产生 error）。
    await expect(customerRestoreSession()).resolves.toBeNull();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("restores null (swallows) on 401 stale session (#7)", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("stale", { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    // 有凭证但失效：仍不抛给调用方，回登录页（try/catch 兜底）。
    await expect(customerRestoreSession()).resolves.toBeNull();
  });
});

describe("customer session expiry redirect", () => {
  afterEach(() => {
    setCustomerSessionExpiredHandler(null);
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  function response401(): Response {
    return new Response(JSON.stringify({ code: "UNAUTHORIZED", detail_code: "CUSTOMER_UNAUTHENTICATED", message: "expired" }), { status: 401 });
  }

  it("fires the session-expired handler on 401 from any business endpoint", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response401()));
    const expired = vi.fn();
    setCustomerSessionExpiredHandler(expired);

    await expect(new CustomerApi("stale").get("/api/v1/customer/orders")).rejects.toThrow("登录已过期，请重新登录");
    expect(expired).toHaveBeenCalledTimes(1);
  });

  it("fires only once for concurrent 401s", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response401()));
    const expired = vi.fn();
    setCustomerSessionExpiredHandler(expired);

    const api = new CustomerApi("stale");
    const results = await Promise.allSettled([
      api.get("/api/v1/customer/orders"),
      api.get("/api/v1/customer/points"),
      api.post("/api/v1/customer/ticket-orders", { ticketTypeId: "t-1" }),
    ]);
    expect(results.every((r) => r.status === "rejected")).toBe(true);
    expect(expired).toHaveBeenCalledTimes(1);
  });

  it("does not fire on 403 (real permission denial stays on page)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: "FORBIDDEN", message: "denied" }), { status: 403 })),
    );
    const expired = vi.fn();
    setCustomerSessionExpiredHandler(expired);

    await expect(new CustomerApi("tok").get("/api/v1/customer/orders")).rejects.toThrow("当前账号无权进行此操作");
    expect(expired).not.toHaveBeenCalled();
  });

  it("does not fire for login failure or session probe", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response401()));
    const expired = vi.fn();
    setCustomerSessionExpiredHandler(expired);

    await expect(customerPhoneLogin({ phone: "13800000000", smsCode: "0000" })).rejects.toThrow();
    await expect(customerRestoreSession()).resolves.toBeNull();
    expect(expired).not.toHaveBeenCalled();
  });
});
