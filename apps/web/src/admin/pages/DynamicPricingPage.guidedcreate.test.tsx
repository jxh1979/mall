import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { useState } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
import { ActionResultView, formatActionResult } from "../components/ActionResultView";
import { DynamicPricingPage } from "./DynamicPricingPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, TicketDynamicPricingRuleRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

// form 在 AdminDataState empty 内，需 ≥1 条规则才渲染新建入口。
const RULES: TicketDynamicPricingRuleRow[] = [
  { id: "r-1", ruleName: "现有规则", salesChannel: "web", priority: 10, adjustmentType: "percent_delta", adjustmentRate: 10, status: "draft", effectiveFrom: "2026-06-01", effectiveTo: "2026-06-30", ticketTypeId: "t-1" },
];

// 第 229 条 229-R7：票种下拉读 GET /api/v1/ticketing/ticket-types（真实接口回分页信封）。
const TICKET_TYPES = [{ id: "tt_real", ticketName: "真实门票" }];

function makeApi(post = vi.fn().mockResolvedValue({ id: "rule-new" })): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.startsWith("/api/v1/ticketing/ticket-types") ? { items: TICKET_TYPES } : RULES)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient): void {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<DynamicPricingPage {...props} />);
}

function pickDay(testId: string, day: string) {
  const trigger = screen.getByTestId(testId);
  fireEvent.click(trigger);
  fireEvent.click(within(trigger.parentElement as HTMLElement).getByRole("button", { name: day }));
}

// 先等票种下拉读到选项（被动 effect 落定）再操作引导弹窗。
async function openCreateWithTicketTypes(): Promise<void> {
  await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-create-open")).not.toBeNull());
  fireEvent.click(screen.getByTestId("dynamic-pricing-create-open"));
  await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-ticket-type")?.textContent ?? "").toContain("真实门票"));
}

function postBody(post: { mock: { calls: unknown[][] } }, path: string): unknown {
  return post.mock.calls.find((c) => c[0] === path)?.[1];
}

// 引导式流程 phase-3：新建定价规则从内联表单改为「＋新建定价规则」按钮 → 分步引导弹窗。
describe("DynamicPricingPage 引导式新建定价规则", () => {
  afterEach(() => {
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing");
  });

  // 第 229 条 229-R7：票种取所选；生效期不填不发；不带写死的价格上下限、条件、备注与附件。
  it("入口→选票种→步进→提交调用建规则接口", async () => {
    const post = vi.fn().mockResolvedValue({ id: "rule-new" });
    renderPage(makeApi(post));
    await openCreateWithTicketTypes();
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt_real" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-rule-name"), { target: { value: "低库存加价" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-adjustment-rate"), { target: { value: "15" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(true));
    const body = postBody(post, "/api/v1/ticketing/dynamic-pricing-rules");
    expect(body).toMatchObject({ ticketTypeId: "tt_real", ruleName: "低库存加价", adjustmentType: "percent_delta", adjustmentRate: 15 });
    for (const key of ["adjustmentAmountCent", "effectiveFrom", "effectiveTo", "minPriceCent", "maxPriceCent", "conditionRule", "operatorNote", "evidenceAttachmentIds"]) {
      expect(body).not.toHaveProperty(key);
    }
  });

  it("未选票种 → 提示「请选择票种。」，不建规则", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await openCreateWithTicketTypes();
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-guided-error")?.textContent).toBe("请选择票种。"));
    expect(screen.queryByTestId("dynamic-pricing-create")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(false);
  });

  it("填生效开始与结束 → 确认页列出票种与生效期，请求体带所填日期", async () => {
    const post = vi.fn().mockResolvedValue({ id: "rule-new" });
    renderPage(makeApi(post));
    await openCreateWithTicketTypes();
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt_real" } });
    pickDay("dynamic-pricing-effective-from", "1");
    pickDay("dynamic-pricing-effective-to", "31");
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    const summary = screen.getByTestId("dynamic-pricing-guided-summary").textContent ?? "";
    expect(summary).toContain("真实门票");
    expect(summary).toContain("2025年1月1日");
    expect(summary).toContain("2025年1月31日");
    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(true));
    expect(postBody(post, "/api/v1/ticketing/dynamic-pricing-rules")).toMatchObject({ ticketTypeId: "tt_real", effectiveFrom: "2025-01-01", effectiveTo: "2025-01-31" });
  });

  it("试算 → 票种取选中规则，不带写死条件，到访日空则不发", async () => {
    const post = vi.fn().mockResolvedValue({ finalPriceCent: 11000 });
    renderPage(makeApi(post));
    await waitFor(() => expect((screen.queryByTestId("dynamic-pricing-select-r-1") as HTMLInputElement | null)?.checked).toBe(true));
    fireEvent.click(screen.getByTestId("dynamic-pricing-simulate"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing/simulations")).toBe(true));
    expect(postBody(post, "/api/v1/ticketing/dynamic-pricing/simulations")).toEqual({ ticketTypeId: "t-1", salesChannel: "web" });
  });

  it("发布 → 原因「后台发布」，不带写死附件", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const post = vi.fn().mockResolvedValue({ id: "r-1", status: "published" });
    renderPage(makeApi(post));
    await waitFor(() => expect((screen.queryByTestId("dynamic-pricing-select-r-1") as HTMLInputElement | null)?.checked).toBe(true));
    fireEvent.click(screen.getByTestId("dynamic-pricing-publish"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules/r-1/publish")).toBe(true));
    expect(postBody(post, "/api/v1/ticketing/dynamic-pricing-rules/r-1/publish")).toEqual({ action: "publish", reason: "后台发布" });
    confirmSpy.mockRestore();
  });

  // 第十八批 AS 段 297-R4（第十八批裁决 63；裁决 61「改动另存一份草稿版审批，审批期间照旧版卖，获批后自动换成新版」）：已上架跨域组合通票的动态价
  //   发布只存进规则草稿版、后端回 status draft，提示去票务策略提交审批。真实 AdminApi，假响应用文本体。
  it("发布回 status draft → 提示动态价已存入规则草稿版", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const ok = (data: string) => new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
      { status: 200, headers: { "content-type": "application/json" } });
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET") === "POST" && url.endsWith("/dynamic-pricing-rules/r-1/publish")) {
        return ok('{"id":"r-1","ticket_type_id":"tt-eff","rule_name":"现有规则","status":"draft"}');
      }
      if (url.includes("/ticketing/ticket-types")) return ok('{"items":[{"id":"tt-eff","ticket_name":"已上架跨域组合通票"}]}');
      return ok('[{"id":"r-1","rule_name":"现有规则","sales_channel":"web","priority":10,"adjustment_type":"percent_delta","adjustment_rate":10,'
        + '"status":"draft","effective_from":"2026-11-01","effective_to":"2026-11-30","ticket_type_id":"tt-eff"}]');
    }));
    const props: AdminPageProps = { api: new AdminApi(), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<DynamicPricingPage {...props} />);
    await waitFor(() => expect((screen.queryByTestId("dynamic-pricing-select-r-1") as HTMLInputElement | null)?.checked).toBe(true));
    fireEvent.click(screen.getByTestId("dynamic-pricing-publish"));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({
      action: "ticket_dynamic_rule_staged", note: expect.stringContaining("这张票是已上架的未挂酒吧的票：动态价已存入规则草稿版") })));
    confirmSpy.mockRestore();
    vi.unstubAllGlobals();
  });

  it("规则名为空 → 步内校验拦截，不建规则", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-create-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("dynamic-pricing-create-open"));
    // 规则名默认非空,显式清空以触发校验
    fireEvent.change(screen.getByTestId("dynamic-pricing-rule-name"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    await waitFor(() => expect(screen.getByTestId("dynamic-pricing-guided-error")).toBeTruthy());
    expect(screen.queryByTestId("dynamic-pricing-create")).toBeNull();
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(false);
  });
});

// 第十八批 AT 段 335-R1（BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生；动态价按时段、容量等规则配置，可先试算再发布。」；
//   PGM:287「界面全中文」）：调整列按后端取值 percent_delta、fixed_delta 显示中文加减价；引导可按固定金额调价。
describe("DynamicPricingPage 335-R1 按固定金额调价", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing");
  });

  it("调整列：按固定金额显示「减价 ¥5.00」、按比例显示「加价 10%」，不出英文取值", async () => {
    const ok = (data: string) => new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
      { status: 200, headers: { "content-type": "application/json" } });
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      if (String(input).includes("/ticketing/ticket-types")) return ok('{"items":[]}');
      return ok('[{"id":"dp-fix","rule_name":"淡季减价","sales_channel":"web","priority":10,"adjustment_type":"fixed_delta","adjustment_rate":null,'
        + '"adjustment_amount_cent":-500,"status":"draft","effective_from":"2026-11-01","effective_to":"2026-11-30","ticket_type_id":"tt-1"},'
        + '{"id":"dp-pct","rule_name":"旺季加价","sales_channel":"web","priority":5,"adjustment_type":"percent_delta","adjustment_rate":10,'
        + '"adjustment_amount_cent":null,"status":"published","effective_from":"2026-11-01","effective_to":"2026-11-30","ticket_type_id":"tt-1"}]');
    }));
    const props: AdminPageProps = { api: new AdminApi(), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<DynamicPricingPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-row-dp-fix")?.textContent ?? "").toContain("减价 ¥5.00"));
    const fixedRow = screen.getByTestId("dynamic-pricing-row-dp-fix").textContent ?? "";
    expect(fixedRow).not.toContain("fixed_delta");
    expect(fixedRow).not.toContain("absolute_delta");
    expect(screen.getByTestId("dynamic-pricing-row-dp-pct").textContent ?? "").toContain("加价 10%");
  });

  it("调价方式选按固定金额填 -5 → 确认页「减价 ¥5.00」，请求体 fixed_delta、adjustmentAmountCent -500，不带 adjustmentRate", async () => {
    const post = vi.fn().mockResolvedValue({ id: "rule-new" });
    renderPage(makeApi(post));
    await openCreateWithTicketTypes();
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-adjustment-type")).not.toBeNull());
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt_real" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-adjustment-type"), { target: { value: "fixed_delta" } });
    expect(screen.queryByTestId("dynamic-pricing-adjustment-rate")).toBeNull();
    fireEvent.change(screen.getByTestId("dynamic-pricing-adjustment-amount"), { target: { value: "-5" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    expect(screen.getByTestId("dynamic-pricing-guided-summary").textContent ?? "").toContain("减价 ¥5.00");
    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(true));
    const body = postBody(post, "/api/v1/ticketing/dynamic-pricing-rules");
    expect(body).toMatchObject({ ticketTypeId: "tt_real", adjustmentType: "fixed_delta", adjustmentAmountCent: -500 });
    expect(body).not.toHaveProperty("adjustmentRate");
  });
});

// 第十八批 AW 段 338-R3（BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生」；PGM:287「界面全中文」）：动态价可按固定价。
describe("DynamicPricingPage 338-R3 按固定价", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing");
  });

  it("调价方式选固定价填 199 → 确认页「固定价 ¥199.00」，请求体 fixed_price、adjustmentAmountCent 19900，不带 adjustmentRate", async () => {
    const post = vi.fn().mockResolvedValue({ id: "rule-new" });
    renderPage(makeApi(post));
    await openCreateWithTicketTypes();
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt_real" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-adjustment-type"), { target: { value: "fixed_price" } });
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-adjustment-amount")).not.toBeNull());
    fireEvent.change(screen.getByTestId("dynamic-pricing-adjustment-amount"), { target: { value: "199" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    expect(screen.getByTestId("dynamic-pricing-guided-summary").textContent ?? "").toContain("固定价 ¥199.00");
    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(true));
    const body = postBody(post, "/api/v1/ticketing/dynamic-pricing-rules");
    expect(body).toMatchObject({ ticketTypeId: "tt_real", adjustmentType: "fixed_price", adjustmentAmountCent: 19900 });
    expect(body).not.toHaveProperty("adjustmentRate");
  });

  it("调整列：按固定价显示「固定价 ¥199.00」，不出英文取值", async () => {
    const ok = (data: string) => new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
      { status: 200, headers: { "content-type": "application/json" } });
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      if (String(input).includes("/ticketing/ticket-types")) return ok('{"items":[]}');
      return ok('[{"id":"dp-fp","rule_name":"晚场固定价","sales_channel":"web","priority":10,"adjustment_type":"fixed_price","adjustment_rate":null,'
        + '"adjustment_amount_cent":19900,"status":"draft","effective_from":"2026-11-01","effective_to":"2026-11-30","ticket_type_id":"tt-1"}]');
    }));
    const props: AdminPageProps = { api: new AdminApi(), user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<DynamicPricingPage {...props} />);
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-row-dp-fp")?.textContent ?? "").toContain("固定价 ¥199.00"));
    expect(screen.getByTestId("dynamic-pricing-row-dp-fp").textContent ?? "").not.toContain("fixed_price");
  });
});

// 第十八批 BP 段第 359 条 359-R2、359-R3（BSM:782「动态价按时段、容量等规则配置，可先试算再发布。」；第十八批裁决 75「规则里填『到访当天已售满多少张起』」；
//   PGM:287「界面全中文」）：新建引导录入时段与到访当天已售张数，列表与明细显示条件，试算可填试算时刻与当天已售张数。
describe("DynamicPricingPage 359 时段与到访当天已售条件", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing");
  });

  function stubRules(rows: string) {
    const ok = (data: string) => new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
      { status: 200, headers: { "content-type": "application/json" } });
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => (String(input).includes("/ticketing/ticket-types") ? ok('{"items":[]}') : ok(rows))));
    render(<DynamicPricingPage api={new AdminApi()} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()} effectivePermissions={ADMIN_PERMS} />);
  }

  const row = (id: string, condition: string) => `{"id":"${id}","rule_name":"晚场加价","sales_channel":"web","priority":10,"adjustment_type":"percent_delta",`
    + `"adjustment_rate":10,"status":"draft","effective_from":"2026-11-01","effective_to":"2026-11-30","ticket_type_id":"tt-1","condition_rule":${condition}}`;

  // 359-R2
  it("填时段 20:00～22:00 → 确认页列出时段，请求体带 conditionRule", async () => {
    const post = vi.fn().mockResolvedValue({ id: "rule-new" });
    renderPage(makeApi(post));
    await openCreateWithTicketTypes();
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-time-from")).not.toBeNull());
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt_real" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-time-from"), { target: { value: "20:00" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-time-to"), { target: { value: "22:00" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    expect(screen.getByTestId("dynamic-pricing-guided-summary").textContent ?? "").toContain("时段 20:00～22:00");
    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(true));
    expect((postBody(post, "/api/v1/ticketing/dynamic-pricing-rules") as { conditionRule?: unknown }).conditionRule)
      .toEqual({ timeFrom: "20:00", timeTo: "22:00" });
  });

  it("只填时段开始 → 提示「时段开始与结束须同时填写。」，不建规则", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    await openCreateWithTicketTypes();
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-time-from")).not.toBeNull());
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt_real" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-time-from"), { target: { value: "20:00" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-guided-error")?.textContent).toBe("时段开始与结束须同时填写。"));
    expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(false);
  });

  it("列表与明细显示时段，结束早于开始写「次日」", async () => {
    stubRules(`[${row("dp-eve", '{"time_from":"20:00","time_to":"22:00"}')},${row("dp-night", '{"time_from":"22:00","time_to":"02:00"}')}]`);
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-row-dp-eve")?.textContent ?? "").toContain("时段 20:00～22:00"));
    expect(screen.getByTestId("dynamic-pricing-row-dp-night").textContent ?? "").toContain("时段 22:00～次日 02:00");
    fireEvent.click(screen.getByTestId("dynamic-pricing-row-dp-eve"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")?.textContent ?? "").toContain("时段 20:00～22:00"));
  });

  it("试算填试算时刻 → 请求体 context.atTime", async () => {
    const post = vi.fn().mockResolvedValue({ finalPriceCent: 11000 });
    renderPage(makeApi(post));
    await waitFor(() => expect((screen.queryByTestId("dynamic-pricing-select-r-1") as HTMLInputElement | null)?.checked).toBe(true));
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-simulate-time")).not.toBeNull());
    expect(screen.getByTestId("dynamic-pricing-simulate-time").closest("label")?.textContent ?? "").toContain("试算时刻（空＝此刻）");
    fireEvent.change(screen.getByTestId("dynamic-pricing-simulate-time"), { target: { value: "01:30" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-simulate"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing/simulations")).toBe(true));
    expect(postBody(post, "/api/v1/ticketing/dynamic-pricing/simulations")).toEqual({ ticketTypeId: "t-1", salesChannel: "web", context: { atTime: "01:30" } });
  });

  // 359-R3（第十八批裁决 75）
  it("填到访当天已售满 50 张 → 确认页列出，请求体 conditionRule.daySoldMin 50", async () => {
    const post = vi.fn().mockResolvedValue({ id: "rule-new" });
    renderPage(makeApi(post));
    await openCreateWithTicketTypes();
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-day-sold-min")).not.toBeNull());
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt_real" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-day-sold-min"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    expect(screen.getByTestId("dynamic-pricing-guided-summary").textContent ?? "").toContain("到访当天已售满 50 张起");
    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(post.mock.calls.some((c) => c[0] === "/api/v1/ticketing/dynamic-pricing-rules")).toBe(true));
    expect((postBody(post, "/api/v1/ticketing/dynamic-pricing-rules") as { conditionRule?: unknown }).conditionRule).toEqual({ daySoldMin: 50 });
  });

  it("列表显示「到访当天已售满 50 张起」；试算填当天已售张数 → 请求体 context.daySold", async () => {
    stubRules(`[${row("dp-sold", '{"day_sold_min":50}')}]`);
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-row-dp-sold")?.textContent ?? "").toContain("到访当天已售满 50 张起"));
    await waitFor(() => expect((screen.getByTestId("dynamic-pricing-select-dp-sold") as HTMLInputElement).checked).toBe(true));
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-simulate-day-sold")).not.toBeNull());
    fireEvent.change(screen.getByTestId("dynamic-pricing-simulate-day-sold"), { target: { value: "80" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-simulate"));
    await waitFor(() => expect(vi.mocked(fetch).mock.calls.some(([url]) => String(url).endsWith("/dynamic-pricing/simulations"))).toBe(true));
    const sent = vi.mocked(fetch).mock.calls.find(([url]) => String(url).endsWith("/dynamic-pricing/simulations"))?.[1];
    expect(JSON.parse(String(sent?.body)).context).toEqual({ day_sold: 80 });
  });
});

// 18AW 收尾复核修正（PGM:287「展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错……必须全部为中文，不得出现任何英文」）：
//   新建、发布动态价规则与试算后，结果区的调整类型与试算说明都显示中文。真实 AdminApi，假响应用文本体，结果区同后台外壳用 ActionResultView 渲染。
describe("DynamicPricingPage 结果区中文", () => {
  const setGlobalError = vi.fn();

  function PageWithResult({ api }: { api: AdminApiClient }) {
    const [result, setResult] = useState<unknown>(null);
    return (
      <>
        <DynamicPricingPage api={api} user={{ id: "u1", username: "admin", accountType: "platform_admin" }}
          setActionResult={setResult} setGlobalError={setGlobalError} effectivePermissions={ADMIN_PERMS} />
        {result ? <ActionResultView value={result} /> : null}
      </>
    );
  }

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/admin/ticketing/dynamic-pricing");
  });

  it("新建固定价规则、发布、试算后结果区显示「调整类型：固定价」与中文说明，不出 fixed_price 等英文取值", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const rule = (status: string) => '{"id":"dp-fp","ticket_type_id":"tt-1","rule_name":"晚场固定价","priority":10,"effective_from":null,'
      + '"effective_to":null,"sales_channel":"web","adjustment_type":"fixed_price","adjustment_rate":null,"adjustment_amount_cent":19900,'
      + `"min_price_cent":null,"max_price_cent":null,"status":"${status}","condition_rule":{},"operator_note":null,"evidence_attachment_ids":[]}`;
    const ok = (data: string) => new Response(`{"success":true,"code":"OK","message":"success","data":${data}}`,
      { status: 200, headers: { "content-type": "application/json" } });
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const isPost = (init?.method ?? "GET") === "POST";
      if (isPost && url.endsWith("/dynamic-pricing-rules")) return ok(rule("draft"));
      if (isPost && url.endsWith("/dynamic-pricing-rules/dp-fp/publish")) return ok(rule("published"));
      if (isPost && url.endsWith("/dynamic-pricing/simulations")) {
        return ok('{"ticket_type_id":"tt-1","visit_date":"2026-11-20","sales_channel":"web","base_price_cent":9900,"adjustment_cent":10000,'
          + '"final_price_cent":19900,"matched_rule_id":"dp-fp","explanation":"命中规则：晚场固定价"}');
      }
      if (url.includes("/ticketing/ticket-types")) return ok('{"items":[{"id":"tt-1","ticket_name":"真实门票"}]}');
      return ok(`[${rule("draft")}]`);
    }));
    render(<PageWithResult api={new AdminApi()} />);
    await waitFor(() => expect((screen.queryByTestId("dynamic-pricing-select-dp-fp") as HTMLInputElement | null)?.checked).toBe(true));
    const resultText = () => screen.queryByTestId("admin-action-result")?.textContent ?? "";
    const englishCodes = /fixed_price|percent_delta|fixed_delta|matched|no matching/;

    fireEvent.click(screen.getByTestId("dynamic-pricing-create-open"));
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-ticket-type")?.textContent ?? "").toContain("真实门票"));
    fireEvent.change(screen.getByTestId("dynamic-pricing-ticket-type"), { target: { value: "tt-1" } });
    fireEvent.change(screen.getByTestId("dynamic-pricing-adjustment-type"), { target: { value: "fixed_price" } });
    await waitFor(() => expect(screen.queryByTestId("dynamic-pricing-adjustment-amount")).not.toBeNull());
    fireEvent.change(screen.getByTestId("dynamic-pricing-adjustment-amount"), { target: { value: "199" } });
    fireEvent.click(screen.getByTestId("dynamic-pricing-guided-next"));
    fireEvent.click(screen.getByTestId("dynamic-pricing-create"));
    await waitFor(() => expect(resultText()).toContain("动态定价规则已创建"));
    expect(resultText()).toContain("调整类型：固定价");
    expect(resultText()).not.toMatch(englishCodes);

    fireEvent.click(screen.getByTestId("dynamic-pricing-publish"));
    await waitFor(() => expect(resultText()).toContain("动态定价规则已发布"));
    expect(resultText()).toContain("调整类型：固定价");
    expect(resultText()).not.toMatch(englishCodes);

    fireEvent.click(screen.getByTestId("dynamic-pricing-simulate"));
    await waitFor(() => expect(resultText()).toContain("动态定价已试算"));
    expect(resultText()).toContain("说明：命中规则：晚场固定价");
    expect(resultText()).not.toMatch(englishCodes);
    confirmSpy.mockRestore();
  });

  it("按比例、按固定金额两种调整类型在结果区也显示中文", () => {
    expect(formatActionResult({ adjustment_type: "percent_delta" })).toBe("调整类型：按比例");
    expect(formatActionResult({ adjustmentType: "fixed_delta" })).toBe("调整类型：按固定金额");
  });
});
