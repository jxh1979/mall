import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { AdminApi } from "../client/adminClient";
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
    expect(body).toMatchObject({ ticketTypeId: "tt_real", ruleName: "低库存加价", adjustmentRate: 15 });
    for (const key of ["effectiveFrom", "effectiveTo", "minPriceCent", "maxPriceCent", "conditionRule", "operatorNote", "evidenceAttachmentIds"]) {
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
      action: "ticket_dynamic_rule_staged", note: expect.stringContaining("动态价已存入规则草稿版") })));
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
