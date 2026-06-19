import { afterEach, describe, expect, it, vi } from "vitest";

import {
  CustomerApi,
  customerPhoneLogin,
  deriveRefreshIntervalSeconds,
  normalizeRequestBody,
  unwrap,
} from "./customerClient";

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

  it("throws on non-OK responses", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("boom", { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(new CustomerApi("tok").get("/api/v1/customer/points")).rejects.toThrow(/401/);
  });
});
