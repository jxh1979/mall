// 文件版本：V0.03
import { afterEach, describe, expect, it, vi } from "vitest";

import {
  CustomerApi,
  normalizeRequestBody,
  setCustomerSessionExpiredHandler,
  unwrap,
  type CustomerRequestContext,
  type CustomerTransport,
  type CustomerTransportResponse,
} from "./client";
import { CustomerService } from "./service";
import { ApiError } from "./apiError";
import { REPLAYED_NOTICE, onReplayedNotice } from "./operationKey";

// 三端共用协议的单测用假 transport（App 注入 fetch、小程序注入 wx.request，协议本身只收发字符串）。
function ok(data: unknown): CustomerTransportResponse {
  return { status: 200, text: JSON.stringify({ code: "OK", message: "ok", data }) };
}

function failure(status: number, body: unknown): CustomerTransportResponse {
  return { status, text: typeof body === "string" ? body : JSON.stringify(body) };
}

function transportMock() {
  return vi.fn<CustomerTransport>();
}

function makeApi(transport: CustomerTransport, token: string | null = "tok", context: CustomerRequestContext = {}): CustomerApi {
  return new CustomerApi({
    baseUrl: "https://api.example.test",
    terminalType: "app",
    clientVersion: "1.2.3",
    transport,
    getToken: () => token,
    getContext: () => context,
  });
}

function makeService(transport: CustomerTransport, token: string | null = null): CustomerService {
  return new CustomerService(makeApi(transport, token));
}

function sentAt(transport: ReturnType<typeof transportMock>, index: number) {
  return transport.mock.calls[index]![0];
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

  // ROOT-03-R18「params 原样透传给客户端 SDK」：拉起参数键名不做驼峰转换。
  it("keeps payment launch params verbatim while camelCasing the rest", () => {
    expect(unwrap({ code: "OK", data: { payment_intent_id: "pi-1", launch: { kind: "app_wechat", params: { prepay_id: "wx1", nonce_str: "n" } } } })).toEqual({
      paymentIntentId: "pi-1",
      launch: { kind: "app_wechat", params: { prepay_id: "wx1", nonce_str: "n" } },
    });
  });
});

describe("CustomerApi requests", () => {
  // PGM「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」：客人端每个请求带本端类型与版本，
  // 有当前综合体、当前模块时一并带上，后端终端开关按这几层判关；客人无网页端，不再声明 web、不带 cookie 凭证。
  it("sendsTerminalVersionProjectModuleHeadersAndNoWebOrCredentials", async () => {
    const transport = transportMock().mockResolvedValue(ok({ items: [] }));

    await makeApi(transport, "tok", { projectId: "p-1", moduleCode: "wallet" }).get("/api/v1/customer/orders");

    const sent = sentAt(transport, 0);
    expect(sent.url).toBe("https://api.example.test/api/v1/customer/orders");
    expect(sent.headers).toEqual({
      "x-terminal-type": "app",
      "x-client-version": "1.2.3",
      "x-customer-token": "tok",
      "x-project-id": "p-1",
      "x-module-code": "wallet",
    });
  });

  it("writeBodyIdempotencyKeyEqualsHeader", async () => {
    const transport = transportMock().mockResolvedValue(ok({ id: "o-1" }));

    await makeApi(transport).post("/api/v1/customer/ticket-orders", { ticketTypeId: "t-1", quantity: 1 });

    const sent = sentAt(transport, 0);
    const idemHeader = sent.headers["idempotency-key"];
    expect(idemHeader).toMatch(/^customer-app-/);
    expect(sent.headers["content-type"]).toBe("application/json");
    expect(JSON.parse(sent.body!)).toMatchObject({ ticket_type_id: "t-1", quantity: 1, idempotency_key: idemHeader });
  });

  it("phoneLogin posts without token header", async () => {
    const transport = transportMock().mockResolvedValue(ok({ customer_id: "c-1", customer_token: "tok", masked_phone: "138****0000" }));

    const result = await makeService(transport).phoneLogin({ phone: "13800000000", smsCode: "1234" });

    expect(result.customerToken).toBe("tok");
    expect(sentAt(transport, 0).headers["x-customer-token"]).toBeUndefined();
    expect(JSON.parse(sentAt(transport, 0).body!)).toMatchObject({ phone: "13800000000", sms_code: "1234" });
  });

  it("throws business-worded errors on non-OK responses (technical info in detail)", async () => {
    const transport = transportMock().mockResolvedValue(failure(401, "boom"));

    const error = await makeApi(transport).get("/api/v1/customer/points").catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).message).toBe("登录已过期，请重新登录");
    expect((error as ApiError).detail).toMatchObject({ path: "/api/v1/customer/points", status: 401, raw: "boom" });
  });

  it("translates invalid credentials into business wording", async () => {
    const transport = transportMock().mockResolvedValue(failure(401, { code: "UNAUTHORIZED", detail_code: "INVALID_CREDENTIALS", message: "invalid" }));

    await expect(makeService(transport).phoneLogin({ phone: "13800000000", smsCode: "0000" })).rejects.toThrow("账号或密码不正确");
  });

  it("restores null on anonymous 204 probe without throwing (#7)", async () => {
    const transport = transportMock().mockResolvedValue({ status: 204, text: "" });

    await expect(makeService(transport, "tok").restoreSession()).resolves.toBeNull();
    expect(transport).toHaveBeenCalledTimes(1);
  });

  it("restores null (swallows) on 401 stale session (#7)", async () => {
    const transport = transportMock().mockResolvedValue(failure(401, "stale"));

    await expect(makeService(transport, "stale").restoreSession()).resolves.toBeNull();
  });

  // 只有服务端明确判会话失效（401）或匿名（204）才返回空、由外壳清令牌；断网、超时与 5xx 不算失效，往外抛，外壳留着令牌下次再试。
  it("restoreSessionThrowsOnNetworkErrorOr5xx：断网或服务端异常不当作会话失效", async () => {
    const offline = transportMock().mockRejectedValue(new TypeError("Failed to fetch"));
    await expect(makeService(offline, "tok").restoreSession()).rejects.toThrow("Failed to fetch");
    const down = transportMock().mockResolvedValue(failure(502, "bad gateway"));
    await expect(makeService(down, "tok").restoreSession()).rejects.toBeInstanceOf(ApiError);
  });

  // clients-R9（BSM「连接游乐系统，展示可预定项目和场次」）：按综合体匿名读开放中的游乐项目与尚未结束的场次。
  it("listAmusementProjectsByProject：按综合体读游乐项目与场次", async () => {
    const transport = transportMock().mockResolvedValue(ok([{ amusement_project_id: "ap-1", project_name: "旋转木马", open_status: "open",
      sessions: [{ session_id: "s-1", start_time: "2026-10-25T12:00:00Z", end_time: "2026-10-25T13:00:00Z", capacity: 20, used_capacity: 5,
        status: "scheduled" }] }]));
    const service = makeService(transport) as CustomerService & { listAmusementProjects?: (projectId: string) => Promise<unknown> };
    expect(typeof service.listAmusementProjects).toBe("function");

    const rows = await service.listAmusementProjects!("p 1");

    expect(sentAt(transport, 0).url).toBe("https://api.example.test/api/v1/customer/amusement/projects?project_id=p%201");
    expect(sentAt(transport, 0).method).toBe("GET");
    expect(rows).toEqual([{ amusementProjectId: "ap-1", projectName: "旋转木马", openStatus: "open",
      sessions: [{ sessionId: "s-1", startTime: "2026-10-25T12:00:00Z", endTime: "2026-10-25T13:00:00Z", capacity: 20, usedCapacity: 5,
        status: "scheduled" }] }]);
  });

  // 第十八批 R 段第 152 条 152-R3（第十八批裁决 34「客户点击后确定自己是订台人还是参与者」）：接受邀请函带上所选身份，请求体蛇形 as_holder。
  it("confirmBarInvitation：选「我是订台人」请求体带 as_holder:true，不带第二参按参与者 as_holder:false", async () => {
    const transport = transportMock().mockResolvedValue(ok({ id: "m-1", reservation_id: "r-1", is_holder: true, status: "confirmed" }));
    const service = makeService(transport, "tok");
    const code = { invitationToken: "inv_abc", invitationSignature: "sig123", tableId: "t-a", capacity: 6 };

    await service.confirmBarInvitation(code, true);
    await service.confirmBarInvitation(code);

    expect(sentAt(transport, 0).url).toBe("https://api.example.test/api/v1/customer/bar-invitations/confirm");
    expect(JSON.parse(sentAt(transport, 0).body!)).toMatchObject({ invitation_token: "inv_abc", invitation_signature: "sig123", table_id: "t-a",
      capacity: 6, as_holder: true });
    expect(JSON.parse(sentAt(transport, 1).body!)).toMatchObject({ invitation_token: "inv_abc", as_holder: false });
  });

  it("logout carries the session token so the server revokes it", async () => {
    const transport = transportMock().mockResolvedValue(ok(null));

    await makeService(transport, "tok").logout();

    expect(sentAt(transport, 0).url).toBe("https://api.example.test/api/v1/customer/auth/logout");
    expect(sentAt(transport, 0).headers["x-customer-token"]).toBe("tok");
  });
});

describe("customer session expiry redirect", () => {
  afterEach(() => {
    setCustomerSessionExpiredHandler(null);
  });

  function response401(): CustomerTransportResponse {
    return failure(401, { code: "UNAUTHORIZED", detail_code: "CUSTOMER_UNAUTHENTICATED", message: "expired" });
  }

  it("unauthorizedTriggersSessionExpiredButForbiddenDoesNot", async () => {
    const expired = vi.fn();
    setCustomerSessionExpiredHandler(expired);

    await expect(makeApi(transportMock().mockResolvedValue(response401()), "stale").get("/api/v1/customer/orders"))
      .rejects.toThrow("登录已过期，请重新登录");
    expect(expired).toHaveBeenCalledTimes(1);

    setCustomerSessionExpiredHandler(expired);
    expired.mockClear();
    const forbidden = transportMock().mockResolvedValue(failure(403, { code: "FORBIDDEN", message: "denied" }));
    await expect(makeApi(forbidden).get("/api/v1/customer/orders")).rejects.toThrow("当前账号无权进行此操作");
    expect(expired).not.toHaveBeenCalled();
  });

  it("fires only once for concurrent 401s", async () => {
    const expired = vi.fn();
    setCustomerSessionExpiredHandler(expired);

    const api = makeApi(transportMock().mockResolvedValue(response401()), "stale");
    const results = await Promise.allSettled([
      api.get("/api/v1/customer/orders"),
      api.get("/api/v1/customer/points"),
      api.post("/api/v1/customer/ticket-orders", { ticketTypeId: "t-1" }),
    ]);
    expect(results.every((r) => r.status === "rejected")).toBe(true);
    expect(expired).toHaveBeenCalledTimes(1);
  });

  it("does not fire for login failure or session probe", async () => {
    const expired = vi.fn();
    setCustomerSessionExpiredHandler(expired);
    const service = makeService(transportMock().mockResolvedValue(response401()), "stale");

    await expect(service.phoneLogin({ phone: "13800000000", smsCode: "0000" })).rejects.toThrow();
    await expect(service.restoreSession()).resolves.toBeNull();
    expect(expired).not.toHaveBeenCalled();
  });
});

// M725-F02、M725-F03（治理地图第 303 行「没有返回确认的不算完成，不扣减……
// 同一次操作重复提交时，返回第一次的结果并提示已完成」）：积分兑换丢了响应再点，是同一次兑换，不是新的一笔。
describe("客人端同一次操作的请求编号", () => {
  function replayed(data: unknown): CustomerTransportResponse {
    return { status: 200, text: JSON.stringify({ success: true, code: "OK", message: REPLAYED_NOTICE, data, replayed: true }) };
  }

  const networkDown = () => new TypeError("Failed to fetch");

  function sent(transport: ReturnType<typeof transportMock>, index: number): { header: string | undefined; body: string } {
    const request = sentAt(transport, index);
    return { header: request.headers["idempotency-key"], body: String(JSON.parse(request.body!).idempotency_key) };
  }

  it("积分兑换没拿到结果再点：头与请求体沿用同一个首发编号，拿到第一次的兑换并提示已完成；之后再兑是新的一笔", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const record = { id: "redemption_1", item_name: "精酿啤酒", quantity: 1, total_points: 100 };
    const transport = transportMock()
      .mockRejectedValueOnce(networkDown())
      .mockRejectedValueOnce(networkDown())
      .mockResolvedValueOnce(replayed(record))
      .mockResolvedValueOnce(ok({ ...record, id: "redemption_2" }));
    const service = makeService(transport);

    await expect(service.redeemPoints("ri-1", 1)).rejects.toThrow("Failed to fetch");
    // 第二次还能过吗：连续两次都没拿到结果，第三次仍沿用首发的编号。
    await expect(service.redeemPoints("ri-1", 1)).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-1", 1)).resolves.toMatchObject({ id: "redemption_1", itemName: "精酿啤酒" });
    const first = sent(transport, 0);
    expect(first.header).toMatch(/^customer-app-/);
    expect(first.body).toBe(first.header);
    expect(sent(transport, 1)).toEqual(first);
    expect(sent(transport, 2)).toEqual(first);
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    await expect(service.redeemPoints("ri-1", 1)).resolves.toMatchObject({ id: "redemption_2" });
    expect(sent(transport, 3).header).not.toBe(first.header);
    expect(notices[notices.length - 1]).toBeNull();
    off();
  });

  it("首发的业务错误是明确结果（如积分不足），之后再点是新的一次操作；5xx 不算，沿用", async () => {
    const transport = transportMock()
      .mockResolvedValueOnce(failure(409, { code: "STATE_CONFLICT", detail_code: "POINTS_INSUFFICIENT", message: "积分余额不足" }))
      .mockResolvedValueOnce(failure(502, "<html>bad gateway</html>"))
      .mockResolvedValueOnce(ok({ id: "redemption_3" }));
    const service = makeService(transport);

    await expect(service.redeemPoints("ri-2", 1)).rejects.toThrow("积分余额不足");
    await expect(service.redeemPoints("ri-2", 1)).rejects.toThrow("服务暂时不可用");
    await expect(service.redeemPoints("ri-2", 1)).resolves.toMatchObject({ id: "redemption_3" });
    expect(sent(transport, 1).header).not.toBe(sent(transport, 0).header);
    expect(sent(transport, 2)).toEqual(sent(transport, 1));
  });

  it("首次执行的成功、失败响应、只读请求都不提示已完成", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const transport = transportMock()
      .mockResolvedValueOnce(ok({ id: "redemption_6" }))
      .mockResolvedValueOnce(failure(409, { code: "STATE_CONFLICT", detail_code: "POINTS_INSUFFICIENT", message: "积分余额不足" }))
      // 只读请求不看信封里的已完成标记（服务端也不给查看类请求带它）。
      .mockResolvedValueOnce(replayed({ items: [] }));
    const service = makeService(transport);

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
    const transport = transportMock()
      .mockRejectedValueOnce(networkDown())
      .mockResolvedValueOnce(failure(401, { code: "UNAUTHORIZED", message: "未登录或登录已过期" }))
      .mockResolvedValueOnce(replayed({ id: "redemption_4" }))
      .mockResolvedValueOnce(ok({ id: "redemption_5" }));
    const service = makeService(transport);

    await expect(service.redeemPoints("ri-4", 1)).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-4", 1)).rejects.toThrow("登录已过期");
    await expect(service.redeemPoints("ri-4", 1)).resolves.toMatchObject({ id: "redemption_4" });
    expect(sent(transport, 1)).toEqual(sent(transport, 0));
    expect(sent(transport, 2)).toEqual(sent(transport, 0));
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    // 第二次还能过吗：拿到第一次的结果后再兑是新的一笔。
    await expect(service.redeemPoints("ri-4", 1)).resolves.toMatchObject({ id: "redemption_5" });
    expect(sent(transport, 3).header).not.toBe(sent(transport, 0).header);
    off();
  });

  // 两端登出都不刷新页面，编号只按「方法＋路径＋正文」认、正文里不带是谁；
  // 共用设备上换一位客人登录，上一位没拿到结果的编号不能沿用到这一位的同样内容上。
  const loginAs = (customerId: string) =>
    ok({ customer_id: customerId, masked_phone: "138****0000", customer_token: "t", expires_at: "2026-09-27T00:00:00Z" });

  it("客人 A 兑换没拿到结果 → 登出 → 客人 B 登录兑换同一件：换新编号；B 自己没拿到结果再点仍沿用 B 的首发编号", async () => {
    const transport = transportMock()
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockRejectedValueOnce(networkDown())
      .mockResolvedValueOnce(ok(null))
      .mockResolvedValueOnce(loginAs("c-b"))
      .mockRejectedValueOnce(networkDown())
      .mockResolvedValueOnce(replayed({ id: "redemption_b" }));
    const service = makeService(transport);

    await service.phoneLogin({ phone: "13800000001", smsCode: "123456" });
    await expect(service.redeemPoints("ri-9", 1)).rejects.toThrow("Failed to fetch");
    await service.logout();
    await service.phoneLogin({ phone: "13800000002", smsCode: "123456" });
    await expect(service.redeemPoints("ri-9", 1)).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-9", 1)).resolves.toMatchObject({ id: "redemption_b" });
    expect(sent(transport, 4).header).not.toBe(sent(transport, 1).header);
    // 第二次还能过吗：清过一次之后，B 自己没拿到结果再点，照常沿用 B 的首发编号。
    expect(sent(transport, 5)).toEqual(sent(transport, 4));
  });

  it("客人 A 的兑换还在路上时 A 登出、B 登录，A 这一笔随后才没拿到结果：不登记，B 兑换同一件换新编号", async () => {
    let dropA: (error: Error) => void = () => undefined;
    const transport = transportMock()
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockImplementationOnce(() => new Promise<CustomerTransportResponse>((_resolve, reject) => { dropA = reject; }))
      .mockResolvedValueOnce(ok(null))
      .mockResolvedValueOnce(loginAs("c-b"))
      .mockResolvedValueOnce(ok({ id: "redemption_b" }));
    const service = makeService(transport);

    await service.phoneLogin({ phone: "13800000001", smsCode: "123456" });
    const redeemA = service.redeemPoints("ri-10", 1);
    await vi.waitFor(() => expect(transport).toHaveBeenCalledTimes(2));
    await service.logout();
    await service.phoneLogin({ phone: "13800000002", smsCode: "123456" });
    dropA(networkDown());
    await expect(redeemA).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-10", 1)).resolves.toMatchObject({ id: "redemption_b" });
    expect(sent(transport, 4).header).not.toBe(sent(transport, 1).header);
  });

  // 服务端认同一位客人、同一编号的重复提交，与会话无关。同一位客人登出（或会话失效回登录页）后重新登录，
  // 此前没拿到结果的那一笔再兑仍是同一次兑换，沿用首发编号，拿到第一次的结果，不再扣一次积分、不再发一次券。
  it("客人 A 兑换没拿到结果 → 登出 → A 再登录兑换同一件：沿用 A 的首发编号，拿到第一次的兑换并提示已完成", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const transport = transportMock()
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockRejectedValueOnce(networkDown())
      .mockResolvedValueOnce(ok(null))
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockResolvedValueOnce(replayed({ id: "redemption_a1" }))
      .mockRejectedValueOnce(networkDown())
      .mockResolvedValueOnce(ok(null))
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockResolvedValueOnce(replayed({ id: "redemption_a2" }));
    const service = makeService(transport);
    const logoutAndLoginAgain = async () => {
      await service.logout();
      await service.phoneLogin({ phone: "13800000001", smsCode: "123456" });
    };

    await service.phoneLogin({ phone: "13800000001", smsCode: "123456" });
    await expect(service.redeemPoints("ri-11", 1)).rejects.toThrow("Failed to fetch");
    await logoutAndLoginAgain();
    await expect(service.redeemPoints("ri-11", 1)).resolves.toMatchObject({ id: "redemption_a1" });
    expect(sent(transport, 4)).toEqual(sent(transport, 1));
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    // 第二次还能过吗：拿到结果后再兑是新的一笔（新编号）；这一笔又没拿到结果，再登出、再以 A 登录，仍沿用这一笔的首发编号。
    await expect(service.redeemPoints("ri-11", 1)).rejects.toThrow("Failed to fetch");
    await logoutAndLoginAgain();
    await expect(service.redeemPoints("ri-11", 1)).resolves.toMatchObject({ id: "redemption_a2" });
    expect(sent(transport, 5).header).not.toBe(sent(transport, 1).header);
    expect(sent(transport, 8)).toEqual(sent(transport, 5));
    off();
  });

  it("客人 A 的兑换还在路上时 A 登出、又以 A 登录，这一笔随后才没拿到结果：仍登记，再兑沿用原编号", async () => {
    let dropA: (error: Error) => void = () => undefined;
    const transport = transportMock()
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockImplementationOnce(() => new Promise<CustomerTransportResponse>((_resolve, reject) => { dropA = reject; }))
      .mockResolvedValueOnce(ok(null))
      .mockResolvedValueOnce(loginAs("c-a"))
      .mockResolvedValueOnce(replayed({ id: "redemption_a3" }));
    const service = makeService(transport);

    await service.phoneLogin({ phone: "13800000001", smsCode: "123456" });
    const redeemA = service.redeemPoints("ri-12", 1);
    await vi.waitFor(() => expect(transport).toHaveBeenCalledTimes(2));
    await service.logout();
    await service.phoneLogin({ phone: "13800000001", smsCode: "123456" });
    dropA(networkDown());
    await expect(redeemA).rejects.toThrow("Failed to fetch");
    await expect(service.redeemPoints("ri-12", 1)).resolves.toMatchObject({ id: "redemption_a3" });
    expect(sent(transport, 4)).toEqual(sent(transport, 1));
  });

  it("恢复会话也记下是谁：此前是另一位客人，恢复为客人 A 后兑换没拿到结果，A 再登录兑换同一件：沿用首发编号", async () => {
    const transport = transportMock()
      .mockResolvedValueOnce(loginAs("c-z"))
      .mockResolvedValueOnce(ok({ customer_id: "c-q", masked_phone: "139****0000", expires_at: null }))
      .mockRejectedValueOnce(networkDown())
      .mockResolvedValueOnce(loginAs("c-q"))
      .mockResolvedValueOnce(replayed({ id: "redemption_q" }));
    const service = makeService(transport, "tok");

    await service.phoneLogin({ phone: "13800000009", smsCode: "123456" });
    await expect(service.restoreSession()).resolves.toMatchObject({ customerId: "c-q" });
    await expect(service.redeemPoints("ri-13", 1)).rejects.toThrow("Failed to fetch");
    await service.phoneLogin({ phone: "13900000000", smsCode: "123456" });
    await expect(service.redeemPoints("ri-13", 1)).resolves.toMatchObject({ id: "redemption_q" });
    expect(sent(transport, 4)).toEqual(sent(transport, 2));
  });

  // 兑换还在路上时再点（网络卡住时切到别的页面再回来，积分商城页重新挂载、按钮复位），仍是同一次兑换。
  it("兑换还在路上时再兑同一件：头与请求体都带首发编号，拿到第一次的兑换并提示已完成，不会再扣一次积分；之后再兑是新的一笔", async () => {
    const releases: Array<(response: CustomerTransportResponse) => void> = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const transport = transportMock().mockImplementation(() => new Promise<CustomerTransportResponse>((resolve) => { releases.push(resolve); }));
    const service = makeService(transport);

    // 第二次还能过吗：同样再来一轮，第二轮是新的一笔（新编号）。
    for (const round of [0, 2]) {
      const first = service.redeemPoints("ri-14", 1);
      const again = service.redeemPoints("ri-14", 1);
      await vi.waitFor(() => expect(transport).toHaveBeenCalledTimes(round + 2));
      expect(sent(transport, round + 1)).toEqual(sent(transport, round));
      releases[round]!(ok({ id: `redemption_r${round}` }));
      releases[round + 1]!(replayed({ id: `redemption_r${round}` }));
      await expect(first).resolves.toMatchObject({ id: `redemption_r${round}` });
      await expect(again).resolves.toMatchObject({ id: `redemption_r${round}` });
      expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    }
    expect(sent(transport, 2).header).not.toBe(sent(transport, 0).header);
    off();
  });

  it("调用方在请求体里给了编号时，头与请求体同值；结果未明后再提交，头与请求体都换成首发编号", async () => {
    const transport = transportMock()
      .mockRejectedValueOnce(networkDown())
      .mockResolvedValueOnce(replayed({ id: "t-1" }));
    const api = makeApi(transport);

    await expect(api.post("/api/v1/customer/ticket-transfers", { ticketId: "tk-1", idempotencyKey: "caller-key-1" })).rejects.toThrow("Failed to fetch");
    expect(sent(transport, 0)).toEqual({ header: "caller-key-1", body: "caller-key-1" });
    await expect(api.post("/api/v1/customer/ticket-transfers", { ticketId: "tk-1", idempotencyKey: "caller-key-2" })).resolves.toEqual({ id: "t-1" });
    expect(sent(transport, 1)).toEqual({ header: "caller-key-1", body: "caller-key-1" });
  });
});
