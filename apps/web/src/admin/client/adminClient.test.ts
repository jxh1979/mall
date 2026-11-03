import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "./adminClient";
import { ApiError } from "../../shared/apiError";
import { REPLAYED_NOTICE, onReplayedNotice } from "../../shared/operationKey";

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

// M725-F02、M725-F03（治理地图第 303 行「同一个操作被重复提交，系统只认一次」「没有返回确认的不算完成……
// 同一次操作重复提交时，返回第一次的结果并提示已完成」）：请求编号跟着「一次操作」走。
describe("AdminApi 同一次操作的请求编号", () => {
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

  function sentKey(fetchMock: ReturnType<typeof vi.fn>, index: number): string {
    return String(JSON.parse(String((fetchMock.mock.calls[index]![1] as RequestInit).body)).idempotency_key);
  }

  const lostResponse = () => new TypeError("Failed to fetch");

  it("网络中断后再提交同一操作沿用首发编号；服务端返回第一次的结果时按成功返回并提示已完成；之后同样内容是新的一次操作", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(replayedResponse({ id: "vrr_vi_1" }))
      .mockResolvedValueOnce(jsonResponse({ id: "vrr_vi_1b" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const redeem = () => api.post("/api/v1/vouchers/redemptions", { voucherInstanceId: "vi_1", note: "客人到店领取" });

    await expect(redeem()).rejects.toThrow("Failed to fetch");
    // 第二次还能过吗：连续两次都没拿到结果，第三次仍沿用首发的编号。
    await expect(redeem()).rejects.toThrow("Failed to fetch");
    await expect(redeem()).resolves.toEqual({ id: "vrr_vi_1" });
    expect(sentKey(fetchMock, 1)).toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 2)).toBe(sentKey(fetchMock, 0));
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    // 拿到了明确结果，同样内容再提交是新的一次操作；新提交一开始就撤下上一条「已完成」，本次不是重复提交也不再提示。
    await expect(redeem()).resolves.toEqual({ id: "vrr_vi_1b" });
    expect(sentKey(fetchMock, 3)).not.toBe(sentKey(fetchMock, 0));
    expect(notices[notices.length - 1]).toBeNull();
    off();
  });

  it("5xx 与「请求正在处理中」不算明确结果，再提交沿用；首发的业务错误是明确结果，之后是新的一次操作", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response("<html>bad gateway</html>", { status: 502 }))
      .mockResolvedValueOnce(errorResponse(409, { code: "STATE_CONFLICT", detail_code: "IDEMPOTENCY_IN_PROGRESS", message: "请求正在处理中，请稍后刷新查看结果" }))
      .mockResolvedValueOnce(replayedResponse({ id: "divs_1", status: "approved" }))
      .mockResolvedValueOnce(errorResponse(409, { code: "STATE_CONFLICT", detail_code: "DIVIDEND_STATE_INVALID", message: "当前状态不允许此操作" }))
      .mockResolvedValueOnce(errorResponse(409, { code: "STATE_CONFLICT", detail_code: "DIVIDEND_STATE_INVALID", message: "当前状态不允许此操作" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const approve = () => api.post("/api/v1/finance/dividends/statements/divs_1/approve", {});

    await expect(approve()).rejects.toThrow("服务暂时不可用");
    await expect(approve()).rejects.toThrow("请求正在处理中");
    await expect(approve()).resolves.toEqual({ id: "divs_1", status: "approved" });
    expect(sentKey(fetchMock, 1)).toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 2)).toBe(sentKey(fetchMock, 0));
    // 拿到结果后再点是新的一次（首发）；首发的业务错误是它自己的明确结果，之后再点又是新的一次。
    await expect(approve()).rejects.toThrow("当前状态不允许此操作");
    await expect(approve()).rejects.toThrow("当前状态不允许此操作");
    expect(sentKey(fetchMock, 3)).not.toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 4)).not.toBe(sentKey(fetchMock, 3));
  });

  // 沿用编号的再次提交被拒，只说明这一次没成，不说明第一次没生效。凭单次授权或放行单的动作，第一次还没成功完成时同一编号的
  // 再次提交按不具备权限被拒（403 PERMISSION_DENIED：单次授权或放行单已用掉，且没有第一次的成功结果可返回）；这时删掉编号，
  // 第三次就会换新编号、用掉别的放行单再执行一次，或凭单次授权的动作永远 403、拿不到第一次的结果。
  it("沿用编号的再次提交被拒（403 PERMISSION_DENIED）不是第一次的结果：编号照留，第三次仍沿用，拿到第一次的结果并提示已完成；之后同样内容是新的一次操作", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(errorResponse(403, { code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "没有操作权限：单次授权或放行单已用掉，且没有第一次的成功结果可返回" }))
      .mockResolvedValueOnce(replayedResponse({ id: "si-2", status: "reopen_approved" }))
      .mockResolvedValueOnce(jsonResponse({ id: "si-2", status: "reopen_approved" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // AmusementSafetyPage 审批重开的原样正文，页面每次点击现生成编号。
    const reopen = (click: number) =>
      api.post("/api/v1/amusement/safety-inspections/si-2/approve-reopen", {
        idempotencyKey: `amusement-safety-approve-reopen-${click}`,
        action: "approve",
        reason: "整改复检通过，同意恢复开放",
        evidenceAttachmentIds: ["att-amusement-safety-approve"],
      });

    await expect(reopen(1000)).rejects.toThrow("Failed to fetch");
    await expect(reopen(2000)).rejects.toMatchObject({ detailCode: "PERMISSION_DENIED" });
    await expect(reopen(3000)).resolves.toEqual({ id: "si-2", status: "reopen_approved" });
    expect(sentKey(fetchMock, 1)).toBe("amusement-safety-approve-reopen-1000");
    expect(sentKey(fetchMock, 2)).toBe("amusement-safety-approve-reopen-1000");
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    // 第二次还能过吗：拿到第一次的结果后，同样内容是新的一次操作，用页面这次的编号。
    await reopen(4000);
    expect(sentKey(fetchMock, 3)).toBe("amusement-safety-approve-reopen-4000");
    off();
  });

  it("沿用编号的再次提交遇到「该幂等键已被不同的请求使用」时清掉：留着只会一直被拒，之后是新的一次操作", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(errorResponse(409, { code: "STATE_CONFLICT", detail_code: "IDEMPOTENCY_CONFLICT", message: "该幂等键已被不同的请求使用，请刷新后重试" }))
      .mockResolvedValueOnce(jsonResponse({ id: "divs_3", status: "approved" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const approve = () => api.post("/api/v1/finance/dividends/statements/divs_3/approve", {});

    await expect(approve()).rejects.toThrow("Failed to fetch");
    await expect(approve()).rejects.toMatchObject({ detailCode: "IDEMPOTENCY_CONFLICT" });
    await expect(approve()).resolves.toEqual({ id: "divs_3", status: "approved" });
    expect(sentKey(fetchMock, 1)).toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 2)).not.toBe(sentKey(fetchMock, 0));
  });

  // 后台遇到 401 先静默刷新会话、再重发一次：重发与原请求是同一笔提交，带同一编号。刷新也失败（会话失效）时这一笔被拒，
  // 不是第一次的结果，编号照留，之后再提交仍沿用。
  it("401 刷新会话后的重发沿用同一编号；会话失效被拒的再次提交不清编号，之后再提交仍沿用首发编号并提示已完成", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const expired = () => errorResponse(401, { code: "UNAUTHORIZED", message: "未登录或登录已过期" });
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(expired())
      .mockResolvedValueOnce(expired()) // /auth/refresh 也被拒：会话失效
      .mockResolvedValueOnce(expired())
      .mockResolvedValueOnce(jsonResponse({ user: { id: "u-1" } })) // /auth/refresh 成功
      .mockResolvedValueOnce(replayedResponse({ id: "vrr_vi_5" }));
    vi.stubGlobal("fetch", fetchMock);
    const redeem = () => new AdminApi().post("/api/v1/vouchers/redemptions", { voucherInstanceId: "vi_5", note: "客人到店领取" });

    await expect(redeem()).rejects.toThrow("Failed to fetch");
    await expect(redeem()).rejects.toThrow("登录已过期");
    await expect(redeem()).resolves.toEqual({ id: "vrr_vi_5" });
    expect(String(fetchMock.mock.calls[4]![0])).toContain("/api/v1/auth/refresh");
    expect(sentKey(fetchMock, 1)).toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 3)).toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 5)).toBe(sentKey(fetchMock, 3));
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    off();
  });

  it("首次执行的成功、失败响应、只读请求都不提示已完成；首发成功后同样内容再提交换新编号", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ id: "vrr_vi_6" }))
      .mockResolvedValueOnce(errorResponse(409, { code: "STATE_CONFLICT", detail_code: "VOUCHER_ALREADY_REDEEMED", message: "券已核销" }))
      // 只读请求不看信封里的已完成标记（服务端也不给查看类请求带它）。
      .mockResolvedValueOnce(replayedResponse({ items: [] }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const redeem = () => api.post("/api/v1/vouchers/redemptions", { voucherInstanceId: "vi_6", note: "客人到店领取" });

    await expect(redeem()).resolves.toEqual({ id: "vrr_vi_6" });
    await expect(redeem()).rejects.toThrow("券已核销");
    await expect(api.get("/api/v1/vouchers/instances")).resolves.toEqual([]);
    expect(sentKey(fetchMock, 1)).not.toBe(sentKey(fetchMock, 0));
    expect(notices).not.toContain(REPLAYED_NOTICE);
    off();
  });

  // 第 23 条复核（M725-F03「同一次操作重复提交时，返回第一次的结果并提示已完成」）：外壳自发的打开留痕 page-views 不是用户的
  // 下一次提交，不撤下「已完成」提示、不进操作编号登记，照常每次带新的请求编号。
  it("页面打开留痕 page-views 不撤下「已完成」提示，每次带新的请求编号", async () => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(replayedResponse({ id: "sb_1", status: "transferred" }))
      .mockResolvedValueOnce(jsonResponse({ code: "ABCDEFGH" }))
      .mockResolvedValueOnce(jsonResponse({ code: "ABCDEFGJ" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();

    await api.post("/api/v1/inventory/saved-bottles/sb_1/transfer", { targetPhone: "13800000000", idempotencyKey: "transfer-sb_1-13800000000" });
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    const view = { pageId: "ADM-BAR-003", sensitive: true, objectRefs: [{ objectType: "saved_bottle", objectId: "sb_1" }] };
    await api.post("/api/v1/security/page-views", view);
    await api.post("/api/v1/security/page-views", view);

    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    expect(JSON.parse(String((fetchMock.mock.calls[1]![1] as RequestInit).body))).toMatchObject({
      page_id: "ADM-BAR-003", sensitive: true, object_refs: [{ object_type: "saved_bottle", object_id: "sb_1" }],
    });
    expect(sentKey(fetchMock, 1)).toMatch(/^admin-web-/);
    expect(sentKey(fetchMock, 2)).not.toBe(sentKey(fetchMock, 1));
    off();
  });

  // PUT 改写的是路径所指的那个对象。关开关没拿到结果（其实已关），操作员重新读数后打开、再关，
  // 第三次正文与第一次逐字相同，但已不是同一个操作：不能沿用第一次的编号被当成重放（服务端不执行，开关仍开着）。
  it("同一对象的 PUT 改写成功后，之前对它没拿到结果的登记作废：关（丢响应）→ 开 → 再关，第三次换新编号、真正执行", async () => {
    let enabled = true;
    const done = new Map<string, unknown>();
    const lose: boolean[] = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
      // 服务端替身按幂等层约定：同一请求编号已执行过，回放第一次的结果并带 replayed；否则执行并记下结果。
      const body = JSON.parse(String(init?.body)) as { idempotency_key: string; enabled: boolean };
      const first = done.get(body.idempotency_key);
      if (first) return Promise.resolve(replayedResponse(first));
      enabled = body.enabled;
      const result = { id: "sw-1", enabled };
      done.set(body.idempotency_key, result);
      return lose.shift() ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // IntegrationSwitchesPage.toggle 的原样正文。
    const toggle = (next: boolean) =>
      api.put("/api/v1/terminals/capability-switches/sw-1", { enabled: next, terminalType: "web", capabilityCode: "sms.notification", scopeId: null });

    // 第二次还能过吗：再来一轮关（丢响应）→ 开 → 再关，仍然正确。
    for (const round of [0, 3]) {
      lose.push(true);
      await expect(toggle(false)).rejects.toThrow("Failed to fetch");
      expect(enabled).toBe(false);
      await expect(toggle(true)).resolves.toEqual({ id: "sw-1", enabled: true });
      await expect(toggle(false)).resolves.toEqual({ id: "sw-1", enabled: false });
      expect(enabled).toBe(false);
      expect(sentKey(fetchMock, round + 2)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
    }
    off();
  });

  // 服务端的请求编号记录不过期（IdempotencyService 只写 expires_at、从不读；查找与占位都不看它，也没有清理作业），
  // 同一编号隔多久都回放第一次的结果。页面开着、隔天再提交没拿到结果的那一笔，仍须沿用首发编号，否则按新编号提交：券已核销，
  // 得到的是「已核销」冲突而不是第一次的结果。例子取只能用一次的动作（同一张券只能核销一次，同样内容不会合法地再做一次）。
  it("没拿到结果的登记不设时限：券核销满 24 小时、48 小时后再提交仍沿用首发编号，服务端回放第一次的结果并提示已完成，只核销一次", async () => {
    const start = Date.parse("2026-09-26T08:00:00Z");
    const now = vi.spyOn(Date, "now").mockReturnValue(start);
    const redeemed = new Map<string, { key: string; result: unknown }>();
    const lose: boolean[] = [];
    let executed = 0;
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
      // 服务端替身按幂等层与券状态机的真实行为：同一请求编号不论隔多久都回放第一次的结果；别的编号核销已核销的券得 409。
      const body = JSON.parse(String(init?.body)) as { idempotency_key: string; voucher_instance_id: string };
      const first = redeemed.get(body.voucher_instance_id);
      if (first && first.key !== body.idempotency_key) {
        return Promise.resolve(errorResponse(409, { code: "STATE_CONFLICT", detail_code: "VOUCHER_ALREADY_REDEEMED", message: "券已核销" }));
      }
      if (first) return lose.shift() ? Promise.reject(lostResponse()) : Promise.resolve(replayedResponse(first.result));
      executed += 1;
      const result = { id: `vrr_${body.voucher_instance_id}` };
      redeemed.set(body.voucher_instance_id, { key: body.idempotency_key, result });
      return lose.shift() ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // VoucherLedgerPage 核销的正文（页面按点击现生成的编号略去：它让位于登记的编号）。
    const redeem = (voucherInstanceId: string) => api.post("/api/v1/vouchers/redemptions", { voucherInstanceId, note: "客人到店核销" });

    lose.push(true, true);
    await expect(redeem("vi_31")).rejects.toThrow("Failed to fetch");
    now.mockReturnValue(start + 86_400_000);
    await expect(redeem("vi_31")).rejects.toThrow("Failed to fetch");
    now.mockReturnValue(start + 2 * 86_400_000);
    await expect(redeem("vi_31")).resolves.toEqual({ id: "vrr_vi_31" });
    expect(sentKey(fetchMock, 1)).toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 2)).toBe(sentKey(fetchMock, 0));
    expect(executed).toBe(1);
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);

    // 第二次还能过吗：换一张券再走一轮，核销没拿到结果，隔 3 天再提交仍沿用它自己的首发编号。
    lose.push(true);
    await expect(redeem("vi_32")).rejects.toThrow("Failed to fetch");
    now.mockReturnValue(start + 5 * 86_400_000);
    await expect(redeem("vi_32")).resolves.toEqual({ id: "vrr_vi_32" });
    expect(sentKey(fetchMock, 3)).not.toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 4)).toBe(sentKey(fetchMock, 3));
    expect(executed).toBe(2);
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    off();
  });

  // 同一路径的 PUT／PATCH 在开始时就作废之前的登记，不等它成功。关没拿到结果（其实已关）→ 开也没拿到结果
  // （其实已开）→ 再关：「开」没拿到结果也说明服务端可能已开，再关须真正执行。只在成功时作废的话，「开」没成功、登记没作废，
  // 再关沿用第一次的编号被回放，开关仍开着、页面却提示已完成。
  it("同一路径的 PUT：关没拿到结果 → 开也没拿到结果（服务端已开）→ 再关，换新编号、真正执行，开关终态为关", async () => {
    let enabled = true;
    const done = new Map<string, unknown>();
    const lose: boolean[] = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
      const body = JSON.parse(String(init?.body)) as { idempotency_key: string; enabled: boolean };
      const first = done.get(body.idempotency_key);
      if (first) return Promise.resolve(replayedResponse(first));
      enabled = body.enabled;
      const result = { id: "sw-3", enabled };
      done.set(body.idempotency_key, result);
      return lose.shift() ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // IntegrationSwitchesPage.toggle 的原样正文。
    const toggle = (next: boolean) =>
      api.put("/api/v1/terminals/capability-switches/sw-3", { enabled: next, terminalType: "web", capabilityCode: "sms.notification", scopeId: null });

    // 第二次还能过吗：再来一轮（每轮三笔，上一轮的「再关」已拿到结果）。
    for (const round of [0, 3]) {
      lose.push(true, true);
      await expect(toggle(false)).rejects.toThrow("Failed to fetch");
      await expect(toggle(true)).rejects.toThrow("Failed to fetch");
      expect(enabled).toBe(true);
      await expect(toggle(false)).resolves.toEqual({ id: "sw-3", enabled: false });
      expect(enabled).toBe(false);
      expect(sentKey(fetchMock, round + 2)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
    }
    off();
  });

  // PATCH 与 PUT 一样改写路径所指的那个对象（现有页面都用 PUT，这里只核对客户端对两种方法一视同仁）。
  it("同一路径的 PATCH：关没拿到结果 → 开成功 → 再关，换新编号", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(jsonResponse({ id: "sw-4", enabled: true }))
      .mockResolvedValueOnce(jsonResponse({ id: "sw-4", enabled: false }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const toggle = (enabled: boolean) => api.patch("/api/v1/terminals/capability-switches/sw-4", { enabled });

    await expect(toggle(false)).rejects.toThrow("Failed to fetch");
    await expect(toggle(true)).resolves.toEqual({ id: "sw-4", enabled: true });
    await expect(toggle(false)).resolves.toEqual({ id: "sw-4", enabled: false });
    expect(sentKey(fetchMock, 2)).not.toBe(sentKey(fetchMock, 0));
  });

  // PUT／PATCH 只作废同一路径（路径所指的那个对象）的登记。别的路径上的改写成功，与没拿到结果的发券无关；
  // 作废了它，再发同样的券就换新编号、再发一批。
  it("发券没拿到结果 → 另一路径的 PUT 成功 → 再发同样的券：仍沿用首发编号，服务端只发一批", async () => {
    const done = new Map<string, unknown>();
    const lose: boolean[] = [];
    let batches = 0;
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const body = JSON.parse(String(init?.body)) as { idempotency_key: string; enabled?: boolean };
      const first = done.get(body.idempotency_key);
      if (first) return Promise.resolve(replayedResponse(first));
      const result = String(input).includes("/vouchers/instances") ? { batch: ++batches } : { id: "sw-2", enabled: body.enabled };
      done.set(body.idempotency_key, result);
      return lose.shift() ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const issue = () => api.post("/api/v1/vouchers/instances", { definitionId: "vdef_7", customerId: "customer-7", quantity: 5 });
    // IntegrationSwitchesPage.toggle 的原样正文，另一个对象。
    const toggle = (next: boolean) =>
      api.put("/api/v1/terminals/capability-switches/sw-2", { enabled: next, terminalType: "web", capabilityCode: "sms.notification", scopeId: null });

    // 第二次还能过吗：同样的来回再走一轮（换一张券的内容之前先拿到结果，之后同样内容是新的一批）。
    for (const round of [0, 3]) {
      lose.push(true);
      await expect(issue()).rejects.toThrow("Failed to fetch");
      await expect(toggle(round === 0)).resolves.toMatchObject({ id: "sw-2" });
      await expect(issue()).resolves.toEqual({ batch: round === 0 ? 1 : 2 });
      expect(sentKey(fetchMock, round + 2)).toBe(sentKey(fetchMock, round));
    }
    expect(batches).toBe(2);
  });

  it("页面按点击现生成的编号，在结果未明后的再提交让位于首发编号；拿到结果后页面自己的编号照常用", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(replayedResponse({ definition_id: "vdef_1", quantity: 2 }))
      .mockResolvedValueOnce(jsonResponse({ definition_id: "vdef_1", quantity: 2 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const issue = (idempotencyKey: string) =>
      api.post("/api/v1/vouchers/instances", { idempotencyKey, definitionId: "vdef_1", customerId: "customer-9", quantity: 2 });

    await expect(issue("voucher-issue-1000")).rejects.toThrow("Failed to fetch");
    await expect(issue("voucher-issue-2000")).resolves.toMatchObject({ quantity: 2 });
    expect(sentKey(fetchMock, 1)).toBe("voucher-issue-1000");
    await expect(issue("voucher-issue-3000")).resolves.toMatchObject({ quantity: 2 });
    expect(sentKey(fetchMock, 2)).toBe("voucher-issue-3000");
  });

  it("改了请求内容是另一次操作，不沿用；原内容再提交仍沿用它自己的首发编号", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(jsonResponse({ quantity: 3 }))
      .mockResolvedValueOnce(replayedResponse({ quantity: 2 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const issue = (quantity: number) => api.post("/api/v1/vouchers/instances", { definitionId: "vdef_1", customerId: "customer-9", quantity });

    await expect(issue(2)).rejects.toThrow("Failed to fetch");
    await expect(issue(3)).resolves.toEqual({ quantity: 3 });
    await expect(issue(2)).resolves.toEqual({ quantity: 2 });
    expect(sentKey(fetchMock, 1)).not.toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 2)).toBe(sentKey(fetchMock, 0));
  });

  it("页面用下划线键 idempotency_key 传的按点击编号不算请求内容：结果未明后再提交仍是同一次操作，沿用首发编号", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(replayedResponse({ generated_count: 3 }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // ComplianceRemindersPage 的扫描写法：下划线键 `crs-${Date.now()}`，两次点击必然不同。
    const scan = (idempotency_key: string) =>
      api.post("/api/v1/compliance/reminders/scan", { idempotency_key, scan_date: null, default_lookahead_days: 30, operator_note: null });

    await expect(scan("crs-1000")).rejects.toThrow("Failed to fetch");
    await expect(scan("crs-2000")).resolves.toEqual({ generatedCount: 3 });
    expect(sentKey(fetchMock, 1)).toBe(sentKey(fetchMock, 0));
  });

  it("同时再提交的两次里一次已拿到结果、另一次随后没拿到结果，不把这个编号放回去，下一次提交是新的一次操作", async () => {
    let releaseSecond: (response: Response) => void = () => undefined;
    let releaseThird: (response: Response) => void = () => undefined;
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockImplementationOnce(() => new Promise<Response>((resolve) => { releaseSecond = resolve; }))
      .mockImplementationOnce(() => new Promise<Response>((resolve) => { releaseThird = resolve; }))
      .mockResolvedValueOnce(jsonResponse({ id: "bill_2" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const createBill = () => api.post("/api/v1/bar/orders/ord-1/bill", { operatorNote: "完整后台验收账单" });

    await expect(createBill()).rejects.toThrow("Failed to fetch");
    const second = createBill();
    const third = createBill();
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3));
    releaseSecond(replayedResponse({ id: "bill_1" }));
    await expect(second).resolves.toEqual({ id: "bill_1" });
    releaseThird(new Response("<html>gateway timeout</html>", { status: 504 }));
    await expect(third).rejects.toThrow("服务暂时不可用");
    expect(sentKey(fetchMock, 1)).toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 2)).toBe(sentKey(fetchMock, 0));

    await expect(createBill()).resolves.toEqual({ id: "bill_2" });
    expect(sentKey(fetchMock, 3)).not.toBe(sentKey(fetchMock, 0));
  });

  // 首发还在路上时再点（双击、切页回来再点），同样内容也是同一次操作。首发一开始就登记，第二笔带同一编号：
  // 服务端要么答「请求正在处理中」，要么回放第一次的结果并提示已完成，不会再执行一次。
  it("首发还在路上时再提交同一内容：第二笔带首发编号，拿到第一次的结果并提示已完成；之后同样内容是新的一次操作", async () => {
    const releases: Array<(response: Response) => void> = [];
    const pending = () => new Promise<Response>((resolve) => { releases.push(resolve); });
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation(pending);
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // FinanceDividendsPage 审批通过：页面每次点击现生成编号。
    const approve = (click: number) => api.post("/api/v1/finance/dividends/statements/divs_9/approve", { idempotencyKey: `dividend-dividend_approved-${click}` });

    // 第二次还能过吗：同样的双击再来一轮，第二轮是新的一次操作。
    for (const [round, click] of [[0, 1000], [3, 4000]] as const) {
      const first = approve(click);
      const second = approve(click + 1000);
      const third = approve(click + 2000);
      await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(round + 3));
      expect(sentKey(fetchMock, round)).toBe(`dividend-dividend_approved-${click}`);
      expect(sentKey(fetchMock, round + 1)).toBe(sentKey(fetchMock, round));
      expect(sentKey(fetchMock, round + 2)).toBe(sentKey(fetchMock, round));
      releases[round + 1]!(errorResponse(409, { code: "STATE_CONFLICT", detail_code: "IDEMPOTENCY_IN_PROGRESS", message: "请求正在处理中，请稍后刷新查看结果" }));
      await expect(second).rejects.toThrow("请求正在处理中");
      releases[round]!(jsonResponse({ id: "divs_9", status: "approved" }));
      await expect(first).resolves.toEqual({ id: "divs_9", status: "approved" });
      releases[round + 2]!(replayedResponse({ id: "divs_9", status: "approved" }));
      await expect(third).resolves.toEqual({ id: "divs_9", status: "approved" });
      expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    }
    expect(sentKey(fetchMock, 3)).toBe("dividend-dividend_approved-4000");
    off();
  });

  // 首发一开始就登记、此后只删不加：后发的先成功、先发的随后网络失败时，不会把先发的编号登记回去；登记回去的话，
  // 下一次新的提交就沿用它、被当成重放（服务端不执行、页面却提示已完成）。
  it("同一内容先发的后失败、后发的先成功：下一次提交是新的一次操作，换新编号、真正执行", async () => {
    const done = new Map<string, unknown>();
    let executed = 0;
    let call = 0;
    let dropFirst: (error: Error) => void = () => undefined;
    const fetchMock = vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
      // 服务端替身按幂等层约定：同一请求编号回放第一次的结果、不执行；否则执行一次。每轮第一笔已执行，但响应迟迟不回、最后网络失败。
      const key = String(JSON.parse(String(init?.body)).idempotency_key);
      call += 1;
      const first = done.get(key);
      if (first) return Promise.resolve(replayedResponse(first));
      const result = { bill_order: ++executed };
      done.set(key, result);
      if (call % 3 === 1) return new Promise<Response>((_resolve, reject) => { dropFirst = reject; });
      return Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const createBill = () => api.post("/api/v1/bar/orders/ord-3/bill", { operatorNote: "完整后台验收账单" });

    // 第二次还能过吗：同样的先后再来一轮。
    for (const round of [0, 3]) {
      const first = createBill();
      await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(round + 1));
      await expect(createBill()).resolves.toEqual({ billOrder: round === 0 ? 1 : 3 });
      dropFirst(lostResponse());
      await expect(first).rejects.toThrow("Failed to fetch");
      await expect(createBill()).resolves.toEqual({ billOrder: round === 0 ? 2 : 4 });
      expect(sentKey(fetchMock, round + 1)).toBe(sentKey(fetchMock, round));
      expect(sentKey(fetchMock, round + 2)).not.toBe(sentKey(fetchMock, round));
    }
    expect(executed).toBe(4);
  });

  // 首发一开始就登记后，双击时同一编号有两笔同时在路上。首发被拒（限速拦截器在 preHandle 回 429，未执行；
  // 或业务报错整笔回滚、占位行随之消失），第二笔在服务端照样可能执行。这时首发的明确失败不能清掉登记：第二笔没拿到结果的话，
  // 下一次提交换新编号，服务端再执行一次。
  it("双击时首发被限速（429，未执行）、第二笔带首发编号已执行但没拿到结果：再提交仍沿用首发编号，拿到第一次的结果并提示已完成，只执行一次", async () => {
    const done = new Map<string, unknown>();
    let executed = 0;
    const steps: Array<"hold429" | "executeThenHoldLost"> = [];
    const holds: Array<() => void> = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((_input: RequestInfo | URL, init?: RequestInit) => {
      const key = String(JSON.parse(String(init?.body)).idempotency_key);
      const step = steps.shift();
      if (step === "hold429") {
        // RateLimitInterceptor：preHandle 拦下，业务没执行。
        return new Promise<Response>((resolve) => holds.push(() => resolve(errorResponse(429, { code: "RATE_LIMITED", detail_code: "RATE_LIMITED", message: "请求过于频繁，请稍后再试" }))));
      }
      const first = done.get(key);
      if (first) return Promise.resolve(replayedResponse(first));
      executed += 1;
      const result = { id: `arp_${executed}`, amount: 5000 };
      done.set(key, result);
      if (step === "executeThenHoldLost") return new Promise<Response>((_resolve, reject) => holds.push(() => reject(lostResponse())));
      return Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // FinanceArApLedgerPage 核销／调账登记的正文（限速拦截器按 PAYMENT 类目覆盖该路径）。
    const pay = () => api.post("/api/v1/finance/ar-ap-items/ap-7/payments", { amount: 5000, reason: "分期回款" });

    // 第二次还能过吗：再来一轮，两笔的结果倒过来回（第二笔先没拿到结果、首发随后才 429）。
    for (const [round, first429] of [[0, true], [3, false]] as const) {
      steps.push("hold429", "executeThenHoldLost");
      const first = pay();
      const second = pay();
      await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(round + 2));
      expect(sentKey(fetchMock, round + 1)).toBe(sentKey(fetchMock, round));
      const [releaseFirst, releaseSecond] = [holds.shift()!, holds.shift()!];
      if (first429) {
        releaseFirst();
        await expect(first).rejects.toMatchObject({ detailCode: "RATE_LIMITED" });
        releaseSecond();
        await expect(second).rejects.toThrow("Failed to fetch");
      } else {
        releaseSecond();
        await expect(second).rejects.toThrow("Failed to fetch");
        releaseFirst();
        await expect(first).rejects.toMatchObject({ detailCode: "RATE_LIMITED" });
      }
      await expect(pay()).resolves.toEqual({ id: round === 0 ? "arp_1" : "arp_2", amount: 5000 });
      expect(sentKey(fetchMock, round + 2)).toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
      expect(executed).toBe(round === 0 ? 1 : 2);
    }
    expect(sentKey(fetchMock, 3)).not.toBe(sentKey(fetchMock, 0));
    off();
  });

  // 同一对象上一正一反的两个 POST（装配／卸载角色、设置／移除员工级覆盖、商户存档／取消存档、发出／撤回邀请函），
  // 第一次没拿到结果（其实已生效）→ 反向操作 → 再做与第一次逐字相同的操作：已不是同一个操作，不能沿用第一次的编号被当成重放。
  // 反向操作自己没拿到结果（服务端可能已反向）时同样：再做第一次那个操作须真正执行。正文逐字取自各页面（页面按点击现生成的编号略去：
  // 它让位于登记的编号，与本条无关）。
  const REVERSIBLE_PAIRS: Array<[string, string, Record<string, unknown>, string, Record<string, unknown>]> = [
    [
      "装配／卸载角色（AccessGovernancePage）",
      "/api/v1/access/role-assignments",
      { userId: "u-7", roleId: "role-cashier", operatorNote: "后台装配角色" },
      "/api/v1/access/role-assignments/unassign",
      { userId: "u-7", roleId: "role-cashier", operatorNote: "后台卸载角色" },
    ],
    [
      "设置／移除员工级显式拒绝（AccessGovernancePage）",
      "/api/v1/access/users/u-7/permission-overrides",
      { permissionCodes: ["finance.dividend.manage"], effect: "deny", reason: "后台显式拒绝", operatorNote: "后台员工级覆盖" },
      "/api/v1/access/users/u-7/permission-overrides/remove",
      { permissionCodes: ["finance.dividend.manage"], effect: "deny", reason: "后台显式拒绝", operatorNote: "后台员工级覆盖" },
    ],
    [
      "商户存档／取消存档（OrganizationMerchantPage）",
      "/api/v1/platform/merchants/m-7/archive",
      { reason: "停业" },
      "/api/v1/platform/merchants/m-7/unarchive",
      {},
    ],
    [
      "发出／撤回订台邀请函（BarTableSessionsPage）",
      "/api/v1/bar/reservations/rsv-7/invitation",
      {},
      "/api/v1/bar/reservations/rsv-7/invitation/revoke",
      {},
    ],
  ];

  it.each(REVERSIBLE_PAIRS)("%s：第一次没拿到结果 → 反向操作 → 再做第一次那个操作，换新编号、真正执行；反向操作没拿到结果时同样", async (_label, doPath, doBody, undoPath, undoBody) => {
    let applied = false;
    const done = new Map<string, unknown>();
    const lose: boolean[] = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      // 服务端替身按幂等层约定：同一请求编号回放第一次的结果、不执行；否则执行（正向置为已生效，反向撤掉）。
      const key = String(JSON.parse(String(init?.body)).idempotency_key);
      const first = done.get(key);
      if (first) return Promise.resolve(replayedResponse(first));
      applied = String(input).endsWith(doPath);
      const result = { applied };
      done.set(key, result);
      return lose.shift() ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const act = () => api.post(doPath, doBody);
    const undo = () => api.post(undoPath, undoBody);

    // 一：第一次没拿到结果 → 反向成功 → 再做一次（每轮四笔）。第二次还能过吗：同样再来一轮。
    for (const round of [0, 4]) {
      lose.push(true);
      await expect(act()).rejects.toThrow("Failed to fetch");
      await expect(undo()).resolves.toEqual({ applied: false });
      await expect(act()).resolves.toEqual({ applied: true });
      expect(applied).toBe(true);
      expect(sentKey(fetchMock, round + 2)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
      await expect(undo()).resolves.toEqual({ applied: false });
    }
    // 二：第一次没拿到结果 → 反向也没拿到结果（服务端已反向）→ 再做一次：须真正执行，不能回放第一次。
    lose.push(true, true);
    await expect(act()).rejects.toThrow("Failed to fetch");
    await expect(undo()).rejects.toThrow("Failed to fetch");
    await expect(act()).resolves.toEqual({ applied: true });
    expect(applied).toBe(true);
    expect(notices[notices.length - 1]).toBeNull();

    // 反向先行，两个方向都要作废对方的登记。
    // 三：反向没拿到结果（其实已反向）→ 正向成功 → 再反向：换新编号、真正执行，终态为已反向（每轮四笔，末笔正向复位）。
    // 第二次还能过吗：同样再来一轮。
    for (const round of [11, 15]) {
      lose.push(true);
      await expect(undo()).rejects.toThrow("Failed to fetch");
      await expect(act()).resolves.toEqual({ applied: true });
      await expect(undo()).resolves.toEqual({ applied: false });
      expect(applied).toBe(false);
      expect(sentKey(fetchMock, round + 2)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
      await expect(act()).resolves.toEqual({ applied: true });
    }
    // 四：反向没拿到结果 → 正向也没拿到结果（服务端已恢复）→ 再反向：须真正执行（每轮四笔，末笔正向复位）。第二次还能过吗：再来一轮。
    for (const round of [19, 23]) {
      lose.push(true, true);
      await expect(undo()).rejects.toThrow("Failed to fetch");
      await expect(act()).rejects.toThrow("Failed to fetch");
      expect(applied).toBe(true);
      await expect(undo()).resolves.toEqual({ applied: false });
      expect(applied).toBe(false);
      expect(sentKey(fetchMock, round + 2)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
      await expect(act()).resolves.toEqual({ applied: true });
    }
    expect(fetchMock).toHaveBeenCalledTimes(27);
    off();
  });

  // 对象写在正文里的成对路径（装配／卸载角色是集合路径，员工级覆盖的路径只带用户），只有正文里的对象相同才作废。
  // 对别的对象做反向操作，与这个对象没拿到结果的那一笔无关：作废了它，再做同样的操作就换新编号、服务端再执行一次、再记一条审计。
  it("给 u-7 装配角色没拿到结果 → 卸载别人或别的角色成功 → 再给 u-7 装配同一角色：仍沿用首发编号，回放并提示已完成，只执行一次", async () => {
    const done = new Map<string, unknown>();
    const lose: boolean[] = [];
    const executed: string[] = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const body = JSON.parse(String(init?.body)) as { idempotency_key: string; user_id: string; role_id: string };
      const first = done.get(body.idempotency_key);
      if (first) return Promise.resolve(replayedResponse(first));
      const action = String(input).endsWith("/unassign") ? "unassign" : "assign";
      executed.push(`${action} ${body.user_id} ${body.role_id}`);
      const result = { user_id: body.user_id, role_id: body.role_id, action };
      done.set(body.idempotency_key, result);
      return lose.shift() ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // AccessGovernancePage.roleAssignment 的原样正文。
    const assign = (userId: string, roleId: string) =>
      api.post("/api/v1/access/role-assignments", { userId, roleId, operatorNote: "后台装配角色" });
    const unassign = (userId: string, roleId: string) =>
      api.post("/api/v1/access/role-assignments/unassign", { userId, roleId, operatorNote: "后台卸载角色" });

    // 一：卸载的是另一个人的另一个角色。二：另一个人的同一个角色（只换人；只比角色的写法在这里红）。
    // 第二次还能过吗：三：同一个人的另一个角色（只换角色）。
    for (const [round, other] of [[0, ["u-8", "role-waiter"]], [3, ["u-8", "role-cashier"]], [6, ["u-7", "role-waiter"]]] as const) {
      lose.push(true);
      await expect(assign("u-7", "role-cashier")).rejects.toThrow("Failed to fetch");
      await expect(unassign(other[0], other[1])).resolves.toMatchObject({ action: "unassign" });
      await expect(assign("u-7", "role-cashier")).resolves.toEqual({ userId: "u-7", roleId: "role-cashier", action: "assign" });
      expect(sentKey(fetchMock, round + 2)).toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    }
    expect(executed).toEqual([
      "assign u-7 role-cashier", "unassign u-8 role-waiter",
      "assign u-7 role-cashier", "unassign u-8 role-cashier",
      "assign u-7 role-cashier", "unassign u-7 role-waiter",
    ]);
    off();
  });

  it("员工级覆盖：设置拒绝没拿到结果 → 移除别的权限码或另一效果成功 → 再设置同样的拒绝：仍沿用首发编号；移除的码有交集时作废，真正执行", async () => {
    const done = new Map<string, unknown>();
    const lose: boolean[] = [];
    const executed: string[] = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const body = JSON.parse(String(init?.body)) as { idempotency_key: string; effect: string; permission_codes: string[] };
      const first = done.get(body.idempotency_key);
      if (first) return Promise.resolve(replayedResponse(first));
      const action = String(input).endsWith("/remove") ? "remove" : "set";
      executed.push(`${action} ${body.effect} ${body.permission_codes.join(",")}`);
      const result = { user_id: "u-7", action, effect: body.effect };
      done.set(body.idempotency_key, result);
      return lose.shift() ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    // AccessGovernancePage.userPermissionOverride 的原样正文。
    const override = (kind: "set" | "remove", effect: string, permissionCodes: string[]) =>
      api.post(`/api/v1/access/users/u-7/permission-overrides${kind === "remove" ? "/remove" : ""}`, {
        permissionCodes, effect, reason: "后台显式拒绝", operatorNote: "后台员工级覆盖",
      });

    // 一：移除的是别的权限码。第二次还能过吗：二：移除的是同一权限码的另一效果（授予）。
    for (const [round, effect, codes] of [[0, "deny", ["c.d"]], [3, "grant", ["a.b"]]] as const) {
      lose.push(true);
      await expect(override("set", "deny", ["a.b"])).rejects.toThrow("Failed to fetch");
      await expect(override("remove", effect, [...codes])).resolves.toMatchObject({ action: "remove" });
      await expect(override("set", "deny", ["a.b"])).resolves.toMatchObject({ action: "set", effect: "deny" });
      expect(sentKey(fetchMock, round + 2)).toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    }
    // 三：移除的码与第一次有交集（部分撤掉了第一次的效果）→ 再设置同样的拒绝：换新编号、真正执行。
    lose.push(true);
    await expect(override("set", "deny", ["a.b", "e.f"])).rejects.toThrow("Failed to fetch");
    await expect(override("remove", "deny", ["e.f"])).resolves.toMatchObject({ action: "remove" });
    await expect(override("set", "deny", ["a.b", "e.f"])).resolves.toMatchObject({ action: "set" });
    expect(sentKey(fetchMock, 8)).not.toBe(sentKey(fetchMock, 6));
    expect(notices[notices.length - 1]).toBeNull();
    expect(executed).toEqual([
      "set deny a.b", "remove deny c.d", "set deny a.b", "remove grant a.b",
      "set deny a.b,e.f", "remove deny e.f", "set deny a.b,e.f",
    ]);
    off();
  });

  // 反向或改写操作一开始就作废第一次那笔的登记；它随后以首发身份被明确拒绝（服务端没执行，对象没变）时，
  // 再提交与第一次逐字相同的操作仍是同一次操作的重复提交，须沿用首发编号、回放并提示已完成，服务端只执行一次。
  // 员工级显式拒绝的移除取正常拒绝路径 403 GRANT_REQUIRES_APPROVAL；另两组取控制器权限闸（在幂等层之前）的 403
  // （限速拦截器只管登录、支付与退款几类路径，不管存档与开关）。正文逐字取自各页面。
  type WriteCall = ["POST" | "PUT", string, Record<string, unknown>];
  const REJECTED_REVERSALS: Array<[string, WriteCall, WriteCall, number, Record<string, unknown>]> = [
    [
      "设置／移除员工级显式拒绝（AccessGovernancePage，移除须审批）",
      ["POST", "/api/v1/access/users/u-9/permission-overrides", { permissionCodes: ["finance.dividend.manage"], effect: "deny", reason: "后台显式拒绝", operatorNote: "后台员工级覆盖" }],
      ["POST", "/api/v1/access/users/u-9/permission-overrides/remove", { permissionCodes: ["finance.dividend.manage"], effect: "deny", reason: "后台显式拒绝", operatorNote: "后台员工级覆盖" }],
      403,
      { code: "FORBIDDEN", detail_code: "GRANT_REQUIRES_APPROVAL", message: "移除显式拒绝将使以下须审批的权限生效，须经授出审批" },
    ],
    [
      "商户存档／取消存档（OrganizationMerchantPage）",
      ["POST", "/api/v1/platform/merchants/m-9/archive", { reason: "停业" }],
      ["POST", "/api/v1/platform/merchants/m-9/unarchive", {}],
      403,
      { code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "没有操作权限" },
    ],
    [
      "同一路径的 PUT 关／开（IntegrationSwitchesPage）",
      ["PUT", "/api/v1/terminals/capability-switches/sw-9", { enabled: false, terminalType: "web", capabilityCode: "sms.notification", scopeId: null }],
      ["PUT", "/api/v1/terminals/capability-switches/sw-9", { enabled: true, terminalType: "web", capabilityCode: "sms.notification", scopeId: null }],
      403,
      { code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "没有操作权限" },
    ],
  ];

  it.each(REJECTED_REVERSALS)("%s：第一次没拿到结果 → 反向或改写被明确拒绝（没执行）→ 再做第一次那个操作：沿用首发编号，回放并提示已完成，只执行一次", async (_label, first, reversal, status, rejection) => {
    const done = new Map<string, unknown>();
    const steps: Array<"lose" | "reject"> = [];
    const executed: string[] = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      // 服务端替身：拒绝的不执行；否则按幂等层约定，同一请求编号回放第一次的结果、不执行，新编号执行一次。
      const { idempotency_key: key, ...rest } = JSON.parse(String(init?.body)) as Record<string, unknown>;
      const step = steps.shift();
      if (step === "reject") return Promise.resolve(errorResponse(status, rejection));
      const earlier = done.get(String(key));
      if (earlier) return Promise.resolve(replayedResponse(earlier));
      executed.push(`${init?.method} ${String(input)} ${JSON.stringify(rest)}`);
      const result = { executed: executed.length };
      done.set(String(key), result);
      return step === "lose" ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const send = ([method, path, body]: WriteCall) => (method === "PUT" ? api.put(path, body) : api.post(path, body));

    // 第二次还能过吗：同样再来一轮（每轮三笔；上一轮的再提交已拿到结果，这一轮第一笔是新的一次操作）。
    for (const round of [0, 3]) {
      steps.push("lose", "reject");
      await expect(send(first)).rejects.toThrow("Failed to fetch");
      await expect(send(reversal)).rejects.toMatchObject({ detailCode: rejection.detail_code });
      await expect(send(first)).resolves.toEqual({ executed: round / 3 + 1 });
      expect(sentKey(fetchMock, round + 2)).toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
      expect(executed).toHaveLength(round / 3 + 1);
    }
    expect(sentKey(fetchMock, 3)).not.toBe(sentKey(fetchMock, 0));
    expect(new Set(executed).size).toBe(1);
    off();
  });

  // 作废它的反向操作里有一个执行了（或结果未明），对象就可能已变，登记不放回，再做第一次那个操作须真正执行。
  // 正文取自 AccessGovernancePage.userPermissionOverride。
  it("反向被拒也不放回的四种：同一编号的另一笔执行了、首发成功而沿用的一笔被拒、另一个反向成功、沿用的一笔先执行而首发随后被拒——再设置换新编号、真正执行，不提示已完成", async () => {
    const done = new Map<string, unknown>();
    const executed: string[] = [];
    const replies: Array<"hold" | "lose" | "ok"> = [];
    const holds: Array<(response: Response | Error) => void> = [];
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const body = JSON.parse(String(init?.body)) as { idempotency_key: string; effect: string; permission_codes: string[] };
      const reply = replies.shift() ?? "ok";
      // hold：先不执行，等测试给回复（拒绝，或 Error 表示执行了但没拿到结果）。
      if (reply === "hold") {
        return new Promise<Response>((resolve, reject) => holds.push((response) => {
          if (response instanceof Error) {
            executed.push(`${String(input).endsWith("/remove") ? "remove" : "set"} ${body.permission_codes.join(",")}`);
            done.set(body.idempotency_key, { executed: executed.length });
            reject(response);
          } else {
            resolve(response);
          }
        }));
      }
      const earlier = done.get(body.idempotency_key);
      if (earlier) return Promise.resolve(replayedResponse(earlier));
      executed.push(`${String(input).endsWith("/remove") ? "remove" : "set"} ${body.permission_codes.join(",")}`);
      const result = { executed: executed.length };
      done.set(body.idempotency_key, result);
      return reply === "lose" ? Promise.reject(lostResponse()) : Promise.resolve(jsonResponse(result));
    });
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const override = (kind: "set" | "remove", permissionCodes: string[]) =>
      api.post(`/api/v1/access/users/u-10/permission-overrides${kind === "remove" ? "/remove" : ""}`, {
        permissionCodes, effect: "deny", reason: "后台显式拒绝", operatorNote: "后台员工级覆盖",
      });
    const conflict = () => errorResponse(409, { code: "STATE_CONFLICT", detail_code: "STATE_CONFLICT", message: "数据已被他人修改，请刷新后重试" });
    const revoked = () => errorResponse(403, { code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "没有操作权限" });

    // 甲：移除双击，首发那笔被拒（并发冲突回滚）时第二笔还在路上，第二笔执行了但没拿到结果。第二次还能过吗：再来一轮。
    for (const round of [0, 4]) {
      replies.push("lose", "hold", "hold");
      await expect(override("set", ["a.b"])).rejects.toThrow("Failed to fetch");
      const [removeA, removeB] = [override("remove", ["a.b"]), override("remove", ["a.b"])];
      await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(round + 3));
      holds.shift()!(conflict());
      await expect(removeA).rejects.toMatchObject({ detailCode: "STATE_CONFLICT" });
      holds.shift()!(lostResponse());
      await expect(removeB).rejects.toThrow("Failed to fetch");
      const before = executed.length;
      await expect(override("set", ["a.b"])).resolves.toEqual({ executed: before + 1 });
      expect(sentKey(fetchMock, round + 3)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
    }
    // 乙：移除双击，首发那笔成功，沿用编号的第二笔被控制器权限闸拒（权限在两笔之间被收回，闸在幂等层之前）。第二次还能过吗：再来一轮。
    for (const round of [8, 12]) {
      replies.push("lose", "hold", "hold");
      await expect(override("set", ["a.b"])).rejects.toThrow("Failed to fetch");
      const [removeA, removeB] = [override("remove", ["a.b"]), override("remove", ["a.b"])];
      await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(round + 3));
      executed.push("remove a.b");
      holds.shift()!(jsonResponse({ executed: executed.length }));
      await expect(removeA).resolves.toMatchObject({});
      holds.shift()!(revoked());
      await expect(removeB).rejects.toMatchObject({ detailCode: "PERMISSION_DENIED" });
      const before = executed.length;
      await expect(override("set", ["a.b"])).resolves.toEqual({ executed: before + 1 });
      expect(sentKey(fetchMock, round + 3)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
    }
    // 丙：移除 [a.b, x.y] 还在路上时改成移除 [a.b] 并成功，前一笔随后被拒（未知权限编码）。第二次还能过吗：再来一轮。
    for (const round of [16, 20]) {
      replies.push("lose", "hold");
      await expect(override("set", ["a.b"])).rejects.toThrow("Failed to fetch");
      const typo = override("remove", ["a.b", "x.y"]);
      await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(round + 2));
      await expect(override("remove", ["a.b"])).resolves.toMatchObject({});
      holds.shift()!(errorResponse(400, { code: "VALIDATION_FAILED", detail_code: "VALIDATION_FAILED", message: "未知的权限编码：x.y" }));
      await expect(typo).rejects.toMatchObject({ detailCode: "VALIDATION_FAILED" });
      const before = executed.length;
      await expect(override("set", ["a.b"])).resolves.toEqual({ executed: before + 1 });
      expect(sentKey(fetchMock, round + 3)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
    }
    // 丁：移除双击，沿用编号的第二笔先回 200（已执行），首发那笔随后才回 409（并发冲突回滚）。
    // 同一编号有一笔执行了，显式拒绝已被移除，再设置是新的一次操作。第二次还能过吗：再来一轮。
    for (const round of [24, 28]) {
      replies.push("lose", "hold", "hold");
      await expect(override("set", ["a.b"])).rejects.toThrow("Failed to fetch");
      const [removeA, removeB] = [override("remove", ["a.b"]), override("remove", ["a.b"])];
      await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(round + 3));
      expect(sentKey(fetchMock, round + 2)).toBe(sentKey(fetchMock, round + 1));
      const [holdA, holdB] = holds.splice(0);
      executed.push("remove a.b");
      holdB!(jsonResponse({ executed: executed.length }));
      await expect(removeB).resolves.toMatchObject({});
      holdA!(conflict());
      await expect(removeA).rejects.toMatchObject({ detailCode: "STATE_CONFLICT" });
      const before = executed.length;
      await expect(override("set", ["a.b"])).resolves.toEqual({ executed: before + 1 });
      expect(sentKey(fetchMock, round + 3)).not.toBe(sentKey(fetchMock, round));
      expect(notices[notices.length - 1]).toBeNull();
    }
    expect(executed.filter((line) => line === "set a.b")).toHaveLength(16);
    off();
  });

  // 成对以外的路径不作废：同一上级路径下的另一条路径（发券与核销）、接在原路径后但不是撤销的动作（节假日保存与拉取）。
  it("不成对的路径成功不作废没拿到结果的登记：再提交仍沿用首发编号，拿到第一次的结果", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(jsonResponse({ id: "vrr_1" }))
      .mockResolvedValueOnce(jsonResponse({ fetched: 3 }))
      .mockResolvedValueOnce(replayedResponse({ quantity: 4 }))
      .mockResolvedValueOnce(replayedResponse({ id: "hol_1" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();
    const issue = () => api.post("/api/v1/vouchers/instances", { definitionId: "vdef_8", customerId: "customer-8", quantity: 4 });
    const holiday = () => api.post("/api/v1/analytics/holiday-calendar", { name: "国庆", startDate: "2026-10-01", endDate: "2026-10-07" });

    await expect(issue()).rejects.toThrow("Failed to fetch");
    await expect(holiday()).rejects.toThrow("Failed to fetch");
    await api.post("/api/v1/vouchers/redemptions", { voucherInstanceId: "vi_8", note: "到店" });
    await api.post("/api/v1/analytics/holiday-calendar/fetch", { year: 2026 });
    await expect(issue()).resolves.toEqual({ quantity: 4 });
    await expect(holiday()).resolves.toEqual({ id: "hol_1" });
    expect(sentKey(fetchMock, 4)).toBe(sentKey(fetchMock, 0));
    expect(sentKey(fetchMock, 5)).toBe(sentKey(fetchMock, 1));
  });

  // 各页面的原样请求（路径、正文；页面按点击现生成编号的，两次给不同编号）。
  const PAGE_WRITES: Array<[string, string, Record<string, unknown>, Record<string, unknown>]> = [
    ["票务订单页确认拨付", "/api/v1/ticketing/refunds/rf_1/confirm-payout", { reason: "原路退回 TX001" }, { reason: "原路退回 TX001" }],
    ["酒吧订单页生成账单", "/api/v1/bar/orders/ord_9/bill", { operatorNote: "完整后台验收账单" }, { operatorNote: "完整后台验收账单" }],
    ["分红页审批通过", "/api/v1/finance/dividends/statements/divs_2/approve", { idempotencyKey: "dividend-approve-1000" }, { idempotencyKey: "dividend-approve-2000" }],
    ["分红页标记发放", "/api/v1/finance/dividends/statements/divs_2/mark-paid", { idempotencyKey: "dividend-mark_paid-1000" }, { idempotencyKey: "dividend-mark_paid-2000" }],
    [
      "团体预约页释放",
      "/api/v1/amusement/group-bookings/b-1/release",
      { idempotencyKey: "group-booking-release-b-1-1000", reason: "验收释放团体容量", evidenceAttachmentIds: ["att-group-booking-release"] },
      { idempotencyKey: "group-booking-release-b-1-2000", reason: "验收释放团体容量", evidenceAttachmentIds: ["att-group-booking-release"] },
    ],
    [
      "游乐安全页维保复工",
      "/api/v1/amusement/maintenance-records/mr-1/confirm-resume",
      { idempotencyKey: "amusement-maintenance-resume-1000" },
      { idempotencyKey: "amusement-maintenance-resume-2000" },
    ],
    [
      "游乐安全页审批重开",
      "/api/v1/amusement/safety-inspections/si-1/approve-reopen",
      { idempotencyKey: "amusement-safety-approve-reopen-1000", action: "approve", reason: "整改复检通过，同意恢复开放", evidenceAttachmentIds: ["att-amusement-safety-approve"] },
      { idempotencyKey: "amusement-safety-approve-reopen-2000", action: "approve", reason: "整改复检通过，同意恢复开放", evidenceAttachmentIds: ["att-amusement-safety-approve"] },
    ],
    [
      "招商工作台签约",
      "/api/v1/leasing/intents/li-1/sign",
      { idempotencyKey: "leasing-intent-sign-1000", merchantId: "m-1", contractSubjectId: "sub-1", operationSubjectId: "sub-1", settlementSubjectId: "sub-1", effectiveDate: "2026-10-01", expiryDate: "2027-09-30" },
      { idempotencyKey: "leasing-intent-sign-2000", merchantId: "m-1", contractSubjectId: "sub-1", operationSubjectId: "sub-1", settlementSubjectId: "sub-1", effectiveDate: "2026-10-01", expiryDate: "2027-09-30" },
    ],
  ];

  it.each(PAGE_WRITES)("%s：没拿到结果后再点一次，沿用首发编号，拿到第一次的结果并提示已完成", async (_label, path, first, again) => {
    const notices: Array<string | null> = [];
    const off = onReplayedNotice((notice) => notices.push(notice));
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(replayedResponse({ id: "first" }));
    vi.stubGlobal("fetch", fetchMock);
    const api = new AdminApi();

    await expect(api.post(path, first)).rejects.toThrow("Failed to fetch");
    await expect(api.post(path, again)).resolves.toEqual({ id: "first" });
    expect(sentKey(fetchMock, 1)).toBe(sentKey(fetchMock, 0));
    expect(notices[notices.length - 1]).toBe(REPLAYED_NOTICE);
    off();
  });

  // 编号只记在页面内存里（模块级变量）：刷新页面就是另一份内存，另开标签页、换设备同理，此前没拿到结果的编号不沿用。
  // 放在本组最后：清模块缓存之后，前面静态引入的客户端与重新加载的客户端不再共用同一份登记。
  it("编号只记在页面内存里：页面重新加载后同样内容换新编号", async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(lostResponse())
      .mockResolvedValueOnce(jsonResponse({ id: "vrr_vi_7" }));
    vi.stubGlobal("fetch", fetchMock);
    const body = { voucherInstanceId: "vi_7", note: "客人到店领取" };

    await expect(new AdminApi().post("/api/v1/vouchers/redemptions", body)).rejects.toThrow("Failed to fetch");
    // 模拟刷新页面：清掉模块缓存、重新加载客户端。
    vi.resetModules();
    const reloaded = await import("./adminClient");
    await expect(new reloaded.AdminApi().post("/api/v1/vouchers/redemptions", body)).resolves.toEqual({ id: "vrr_vi_7" });
    expect(sentKey(fetchMock, 1)).not.toBe(sentKey(fetchMock, 0));
  });
});
