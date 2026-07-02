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

  it("keeps dotted dictionary keys verbatim (metric maps stay addressable)", async () => {
    // BI S2-S5:dashboards/{domain} 的 metrics 是字典(key="ar.aging_31_60"),
    // 字段名转换不得吞掉字典 key 里的下划线,否则真后端下页面读不到指标。
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({
      business_date: "2026-07-01",
      metrics: { "ar.aging_31_60": { value: 900, unit: "cent" } },
    }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(new AdminApi("token").get("/api/v1/analytics/dashboards/finance")).resolves.toEqual({
      businessDate: "2026-07-01",
      metrics: { "ar.aging_31_60": { value: 900, unit: "cent" } },
    });
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
