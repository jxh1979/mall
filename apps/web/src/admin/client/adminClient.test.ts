import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "./adminClient";

function jsonResponse(data: unknown): Response {
  return new Response(JSON.stringify({ code: "OK", message: "ok", data }), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}

describe("AdminApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("unwraps API envelopes and normalizes response keys", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ items: [{ ticket_type_id: "ticket-1" }] }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(new AdminApi("token").get("/api/v1/ticketing/ticket-types")).resolves.toEqual([
      { ticketTypeId: "ticket-1" },
    ]);

    const firstCall = fetchMock.mock.calls[0]!;
    const init = firstCall[1] as RequestInit;
    expect((init?.headers as Headers).get("authorization")).toBe("Bearer token");
  });

  it("normalizes request keys and appends an idempotency key", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await new AdminApi("token").post("/api/v1/ticketing/orders", { ticketTypeId: "ticket-1" });

    const firstCall = fetchMock.mock.calls[0]!;
    const init = firstCall[1] as RequestInit;
    expect(JSON.parse(String(init?.body))).toMatchObject({
      ticket_type_id: "ticket-1",
      idempotency_key: expect.stringMatching(/^admin-web-/),
    });
  });
});
