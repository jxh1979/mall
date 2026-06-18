import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

beforeEach(() => {
  vi.resetModules();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("legacy console api client", () => {
  it("normalizes formal API payloads to snake_case and attaches idempotency key", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: "0", data: { ticket_type_id: "ticket-1" } }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    const { postJson, setApiAccessToken, hasApiAccessToken } = await import("./apiClient");
    setApiAccessToken("token-1");

    const response = await postJson<{ ticketTypeId: string }>("/api/v1/ticketing/orders", {
      ticketTypeId: "ticket-1",
      dynamicPricingContext: { conditionKey: "weekday" },
      payload: { keepCamelCase: true },
    });

    expect(hasApiAccessToken()).toBe(true);
    expect(response).toEqual({ ticketTypeId: "ticket-1" });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(path.endsWith("/api/v1/ticketing/orders")).toBe(true);
    expect(new Headers(init.headers).get("authorization")).toBe("Bearer token-1");
    const body = JSON.parse(String(init.body));
    expect(body.ticket_type_id).toBe("ticket-1");
    expect(body.dynamic_pricing_context.condition_key).toBe("weekday");
    expect(body.payload).toEqual({ keepCamelCase: true });
    expect(body.idempotency_key).toMatch(/^web-/);
  });

  it("keeps open webhook payload shape unchanged", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ accepted: true }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    const { postJson } = await import("./apiClient");
    await postJson("/api/open/d/demo/webhooks/device/event", { requestId: "r1", nestedValue: { keepCamelCase: true } });

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(JSON.parse(String(init.body))).toEqual({ requestId: "r1", nestedValue: { keepCamelCase: true } });
  });

  it("unwraps paged API response items and converts them to camelCase", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: "0", data: { items: [{ rule_id: "r1", final_price_cent: 1200 }] } }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);

    const { getJson } = await import("./apiClient");
    await expect(getJson("/api/v1/ticketing/dynamic-pricing-rules")).resolves.toEqual([{ ruleId: "r1", finalPriceCent: 1200 }]);
  });
});
