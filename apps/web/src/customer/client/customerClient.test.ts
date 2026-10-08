import { afterEach, describe, expect, it, vi } from "vitest";

import {
  CustomerApi,
  customerLogout,
  customerPhoneLogin,
  customerRestoreSession,
  deriveRefreshIntervalSeconds,
  normalizeRequestBody,
  setCustomerSessionExpiredHandler,
  unwrap,
} from "./customerClient";
import { CustomerService } from "./customerService";
import { ApiError } from "../../shared/apiError";
import { REPLAYED_NOTICE, onReplayedNotice } from "../../shared/operationKey";

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
  it("缺时间戳时回退默认 15 秒(2026-07-18 拍板)", () => {
    expect(deriveRefreshIntervalSeconds(null, null)).toBe(15);
  });

  it("按窗口精确推导四档 10/15/30/60", () => {
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:00:00Z", "2026-06-18T00:00:10Z")).toBe(10);
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:00:00Z", "2026-06-18T00:00:15Z")).toBe(15);
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:00:00Z", "2026-06-18T00:00:30Z")).toBe(30);
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:00:00Z", "2026-06-18T00:01:00Z")).toBe(60);
  });

  it("非法窗口取最接近合法档(20 秒→15)", () => {
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:00:00Z", "2026-06-18T00:00:20Z")).toBe(15);
  });

  it("非正窗口回退默认 15 秒", () => {
    expect(deriveRefreshIntervalSeconds("2026-06-18T00:01:00Z", "2026-06-18T00:00:00Z")).toBe(15);
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

// M725-F02、M725-F03（治理地图第 303 行「没有返回确认的不算完成，不扣减……
// 同一次操作重复提交时，返回第一次的结果并提示已完成」）：积分兑换丢了响应再点，是同一次兑换，不是新的一笔。
describe("客人端同一次操作的请求编号", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  function replayedResponse(data: unknown): Response {
    return new Response(JSON.stringify({ success: true, code: "OK", message: REPLAYED_NOTICE, data, replayed: true }), {
      status: 200,
      headers: { "content-type": "application/json" },
    });
  }

  function sent(fetchMock: ReturnType<typeof vi.fn>, index: number): { header: string | null; body: string } {
    const init = fetchMock.mock.calls[index]![1] as RequestInit;
    return {
      header: (init.headers as Headers).get("idempotency-key"),
      body: String(JSON.parse(String(init.body)).idempotency_key),
    };
  }

  it("积分兑换没拿到结果再点：头与请求体沿用同一个首发编号，拿到第一次的兑换并提示已完成；之后再兑是新的一笔", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const record = { id: "redemption_1", item_name: "精酿啤酒", quantity: 1, total_points: 100 };
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(replayedResponse(record))
      .mockResolvedValueOnce(jsonResponse({ ...record, id: "redemption_2" }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    await expect(service.redeemPoints("ri-1", 1)).rejects.toThrow("Failed to fetch");
    // 第二次还能过吗：连续两次都没拿到结果，第三次仍沿用首发的编号。
    await expect(service.redeemPoints("ri-1", 1)).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-1", 1)).resolves.toMatchObject({ id: "redemption_1", itemName: "精酿啤酒" });
    const first = sent(fetchMock, 0);
    expect(first.header).toMatch(/^customer-web-/);
    expect(first.body).toBe(first.header);
    expect(sent(fetchMock, 1)).toEqual(first);
    expect(sent(fetchMock, 2)).toEqual(first);
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    await expect(service.redeemPoints("ri-1", 1)).resolves.toMatchObject({ id: "redemption_2" });
    expect(sent(fetchMock, 3).header).not.toBe(first.header);
    expect(notices[notices.length - 1]).toBeNull();
    off();
  });

  it("首发的业务错误是明确结果（如积分不足），之后再点是新的一次操作；5xx 不算，沿用", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ code: "STATE_CONFLICT", detail_code: "POINTS_INSUFFICIENT", message: "积分余额不足" }), { status: 409 }),
      )
      .mockResolvedValueOnce(new Response("<html>bad gateway</html>", { status: 502 }))
      .mockResolvedValueOnce(jsonResponse({ id: "redemption_3" }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    await expect(service.redeemPoints("ri-2", 1)).rejects.toThrow("积分余额不足");
    await expect(service.redeemPoints("ri-2", 1)).rejects.toThrow("服务暂时不可用");
    await expect(service.redeemPoints("ri-2", 1)).resolves.toMatchObject({ id: "redemption_3" });
    expect(sent(fetchMock, 1).header).not.toBe(sent(fetchMock, 0).header);
    expect(sent(fetchMock, 2)).toEqual(sent(fetchMock, 1));
  });

  it("首次执行的成功、失败响应、只读请求都不提示已完成", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ id: "redemption_6" }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ code: "STATE_CONFLICT", detail_code: "POINTS_INSUFFICIENT", message: "积分余额不足" }), { status: 409 }),
      )
      // 只读请求不看信封里的已完成标记（服务端也不给查看类请求带它）。
      .mockResolvedValueOnce(replayedResponse({ items: [] }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    await expect(service.redeemPoints("ri-6", 1)).resolves.toMatchObject({ id: "redemption_6" });
    await expect(service.redeemPoints("ri-6", 1)).rejects.toThrow("积分余额不足");
    await expect(service.listRedemptions()).resolves.toEqual([]);
    expect(notices).not.toContain(REPLAYED_NOTICE);
    off();
  });

  // 沿用编号的再次提交收到的报错只说明这一次没成，不说明第一次没生效；会话失效回登录页后再兑，
  // 仍是同一次兑换，不能换新编号再扣一次积分。
  it("沿用编号的再次提交遇到会话失效（401）不是第一次的结果：编号照留，再兑仍沿用首发编号，拿到第一次的兑换并提示已完成", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(new Response(JSON.stringify({ code: "UNAUTHORIZED", message: "未登录或登录已过期" }), { status: 401 }))
      .mockResolvedValueOnce(replayedResponse({ id: "redemption_4" }))
      .mockResolvedValueOnce(jsonResponse({ id: "redemption_5" }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    await expect(service.redeemPoints("ri-4", 1)).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-4", 1)).rejects.toThrow("登录已过期");
    await expect(service.redeemPoints("ri-4", 1)).resolves.toMatchObject({ id: "redemption_4" });
    expect(sent(fetchMock, 1)).toEqual(sent(fetchMock, 0));
    expect(sent(fetchMock, 2)).toEqual(sent(fetchMock, 0));
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    // 第二次还能过吗：拿到第一次的结果后再兑是新的一笔。
    await expect(service.redeemPoints("ri-4", 1)).resolves.toMatchObject({ id: "redemption_5" });
    expect(sent(fetchMock, 3).header).not.toBe(sent(fetchMock, 0).header);
    off();
  });

  // 两端登出都不刷新页面，编号只按「方法＋路径＋正文」认、正文里不带是谁；
  // 共用设备上换一位客人登录，上一位没拿到结果的编号不能沿用到这一位的同样内容上。
  const loginAs = (customerId: string) =>
    jsonResponse({ customer_id: customerId, masked_phone: "138****0000", customer_token: "", expires_at: "2026-09-27T00:00:00Z" });

  it("客人 A 兑换没拿到结果 → 登出 → 客人 B 登录兑换同一件：换新编号；B 自己没拿到结果再点仍沿用 B 的首发编号", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(jsonResponse(null))
      .mockResolvedValueOnce(loginAs("c-b"))
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(replayedResponse({ id: "redemption_b" }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    await customerPhoneLogin({ phone: "13800000001", smsCode: "123456", scene: "login" });
    await expect(service.redeemPoints("ri-9", 1)).rejects.toThrow("Failed to fetch");
    await customerLogout();
    await customerPhoneLogin({ phone: "13800000002", smsCode: "123456", scene: "login" });
    await expect(service.redeemPoints("ri-9", 1)).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-9", 1)).resolves.toMatchObject({ id: "redemption_b" });
    expect(sent(fetchMock, 4).header).not.toBe(sent(fetchMock, 1).header);
    // 第二次还能过吗：清过一次之后，B 自己没拿到结果再点，照常沿用 B 的首发编号。
    expect(sent(fetchMock, 5)).toEqual(sent(fetchMock, 4));
  });

  it("客人 A 的兑换还在路上时 A 登出、B 登录，A 这一笔随后才没拿到结果：不登记，B 兑换同一件换新编号", async () => {
    let dropA: (error: Error) => void = () => undefined;
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockImplementationOnce(() => new Promise<Response>((_resolve, reject) => { dropA = reject; }))
      .mockResolvedValueOnce(jsonResponse(null))
      .mockResolvedValueOnce(loginAs("c-b"))
      .mockResolvedValueOnce(jsonResponse({ id: "redemption_b" }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    await customerPhoneLogin({ phone: "13800000001", smsCode: "123456", scene: "login" });
    const redeemA = service.redeemPoints("ri-10", 1);
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    await customerLogout();
    await customerPhoneLogin({ phone: "13800000002", smsCode: "123456", scene: "login" });
    dropA(new TypeError("Failed to fetch"));
    await expect(redeemA).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-10", 1)).resolves.toMatchObject({ id: "redemption_b" });
    expect(sent(fetchMock, 4).header).not.toBe(sent(fetchMock, 1).header);
  });

  // 服务端认同一位客人、同一编号的重复提交，与会话无关。同一位客人登出（或会话失效回登录页）后重新登录，
  // 此前没拿到结果的那一笔再兑仍是同一次兑换，沿用首发编号，拿到第一次的结果，不再扣一次积分、不再发一次券。
  it("客人 A 兑换没拿到结果 → 登出 → A 再登录兑换同一件：沿用 A 的首发编号，拿到第一次的兑换并提示已完成", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(jsonResponse(null))
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockResolvedValueOnce(replayedResponse({ id: "redemption_a1" }))
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(jsonResponse(null))
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockResolvedValueOnce(replayedResponse({ id: "redemption_a2" }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");
    const logoutAndLoginAgain = async () => {
      await customerLogout();
      await customerPhoneLogin({ phone: "13800000001", smsCode: "123456", scene: "login" });
    };

    await customerPhoneLogin({ phone: "13800000001", smsCode: "123456", scene: "login" });
    await expect(service.redeemPoints("ri-11", 1)).rejects.toThrow("Failed to fetch");
    await logoutAndLoginAgain();
    await expect(service.redeemPoints("ri-11", 1)).resolves.toMatchObject({ id: "redemption_a1" });
    expect(sent(fetchMock, 4)).toEqual(sent(fetchMock, 1));
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    // 第二次还能过吗：拿到结果后再兑是新的一笔（新编号）；这一笔又没拿到结果，再登出、再以 A 登录，仍沿用这一笔的首发编号。
    await expect(service.redeemPoints("ri-11", 1)).rejects.toThrow("Failed to fetch");
    await logoutAndLoginAgain();
    await expect(service.redeemPoints("ri-11", 1)).resolves.toMatchObject({ id: "redemption_a2" });
    expect(sent(fetchMock, 5).header).not.toBe(sent(fetchMock, 1).header);
    expect(sent(fetchMock, 8)).toEqual(sent(fetchMock, 5));
    off();
  });

  it("客人 A 的兑换还在路上时 A 登出、又以 A 登录，这一笔随后才没拿到结果：仍登记，再兑沿用原编号", async () => {
    let dropA: (error: Error) => void = () => undefined;
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockImplementationOnce(() => new Promise<Response>((_resolve, reject) => { dropA = reject; }))
      .mockResolvedValueOnce(jsonResponse(null))
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockResolvedValueOnce(replayedResponse({ id: "redemption_a3" }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    await customerPhoneLogin({ phone: "13800000001", smsCode: "123456", scene: "login" });
    const redeemA = service.redeemPoints("ri-12", 1);
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    await customerLogout();
    await customerPhoneLogin({ phone: "13800000001", smsCode: "123456", scene: "login" });
    dropA(new TypeError("Failed to fetch"));
    await expect(redeemA).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-12", 1)).resolves.toMatchObject({ id: "redemption_a3" });
    expect(sent(fetchMock, 4)).toEqual(sent(fetchMock, 1));
  });

  it("恢复会话也记下是谁：此前是另一位客人，恢复为客人 A 后兑换没拿到结果，A 再登录兑换同一件：沿用首发编号", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(loginAs("c-z"))
      .mockResolvedValueOnce(jsonResponse({ customer_id: "c-q", masked_phone: "139****0000", expires_at: null }))
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(loginAs("c-q"))
      .mockResolvedValueOnce(replayedResponse({ id: "redemption_q" }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    await customerPhoneLogin({ phone: "13800000009", smsCode: "123456", scene: "login" });
    await expect(customerRestoreSession()).resolves.toMatchObject({ customerId: "c-q" });
    await expect(service.redeemPoints("ri-13", 1)).rejects.toThrow("Failed to fetch");
    await customerPhoneLogin({ phone: "13900000000", smsCode: "123456", scene: "login" });
    await expect(service.redeemPoints("ri-13", 1)).resolves.toMatchObject({ id: "redemption_q" });
    expect(sent(fetchMock, 4)).toEqual(sent(fetchMock, 2));
  });

  // 兑换还在路上时再点（网络卡住时切到别的页面再回来，积分商城页重新挂载、按钮复位），仍是同一次兑换。
  it("兑换还在路上时再兑同一件：头与请求体都带首发编号，拿到第一次的兑换并提示已完成，不会再扣一次积分；之后再兑是新的一笔", async () => {
    const releases: Array<(response: Response) => void> = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation(() => new Promise<Response>((resolve) => { releases.push(resolve); }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new CustomerService("");

    // 第二次还能过吗：同样再来一轮，第二轮是新的一笔（新编号）。
    for (const round of [0, 2]) {
      const first = service.redeemPoints("ri-14", 1);
      const again = service.redeemPoints("ri-14", 1);
      await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(round + 2));
      expect(sent(fetchMock, round + 1)).toEqual(sent(fetchMock, round));
      releases[round]!(jsonResponse({ id: `redemption_r${round}` }));
      releases[round + 1]!(replayedResponse({ id: `redemption_r${round}` }));
      await expect(first).resolves.toMatchObject({ id: `redemption_r${round}` });
      await expect(again).resolves.toMatchObject({ id: `redemption_r${round}` });
      expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    }
    expect(sent(fetchMock, 2).header).not.toBe(sent(fetchMock, 0).header);
    off();
  });

  it("调用方在请求体里给了编号时，头与请求体同值；结果未明后再提交，头与请求体都换成首发编号", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError("Failed to fetch"))
      .mockResolvedValueOnce(replayedResponse({ id: "t-1" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new CustomerApi("tok");

    await expect(api.post("/api/v1/customer/ticket-transfers", { ticketId: "tk-1", idempotencyKey: "caller-key-1" })).rejects.toThrow("Failed to fetch");
    expect(sent(fetchMock, 0)).toEqual({ header: "caller-key-1", body: "caller-key-1" });
    await expect(api.post("/api/v1/customer/ticket-transfers", { ticketId: "tk-1", idempotencyKey: "caller-key-2" })).resolves.toEqual({ id: "t-1" });
    expect(sent(fetchMock, 1)).toEqual({ header: "caller-key-1", body: "caller-key-1" });
  });
});
