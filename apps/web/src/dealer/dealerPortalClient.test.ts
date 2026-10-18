// 文件版本：V0.01
import { afterEach, describe, expect, it, vi } from "vitest";
import { DealerPortalApi } from "./dealerPortalClient";

describe("经销门户交付凭证客户端", () => {
  afterEach(() => { vi.unstubAllGlobals(); document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/"; });
  it("真实 multipart 保留原文件并携带门户身份与 CSRF，不把文件转成 JSON", async () => {
    document.cookie = "XSRF-TOKEN=proof-csrf; path=/";
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "att_1" }), { status: 200 }));
    vi.stubGlobal("fetch", fetcher);
    const form = new FormData(), file = new File(["receipt"], "签收.png", { type: "image/png" }); form.append("file", file);
    await new DealerPortalApi().postForm("/api/v1/dealer-portal/orders/order_a/delivery-evidence", form);
    const request = fetcher.mock.calls[0]![1] as RequestInit;
    expect(request.body).toBe(form);
    expect((request.headers as Headers).get("content-type")).toBeNull();
    expect((request.headers as Headers).get("x-terminal-type")).toBe("dealer_portal");
    expect((request.headers as Headers).get("x-xsrf-token")).toBe("proof-csrf");
    expect(request.credentials).toBe("include");
  });
  it("读取原凭证继续发送受控终端头，不使用绕过门户的裸文件链接", async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response("proof-content", { status: 200, headers: { "content-type": "image/png" } })); vi.stubGlobal("fetch", fetcher);
    const blob = await new DealerPortalApi().readFile("/api/v1/dealer-portal/orders/order_a/delivery-evidence/att_1");
    expect(await blob.text()).toBe("proof-content");
    expect((fetcher.mock.calls[0]![1].headers as Headers).get("x-terminal-type")).toBe("dealer_portal");
  });
});
