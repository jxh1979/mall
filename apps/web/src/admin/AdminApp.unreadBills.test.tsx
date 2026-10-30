// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { MerchantAppStore } from "./client/adminClient";

// 第 92 条（BSM:1597「商户专区首页保留未读账单提醒，该提醒不因异议期届满而消失。」）：商户形态账号落地页 ADM-OV-001 顶部的未读账单提醒，
// 在页面门禁之外；从提醒点「查看」进对应页（我的账单带该月）。平台账号、商户的其他页、商户专区模块被终端开关关闭时不出也不发请求。
vi.mock("./pages/OverviewPage", () => ({ OverviewPage: () => <section data-testid="overview-stub">总控</section> }));
vi.mock("./pages/MerchantZoneSettlementsPage", () => ({ MerchantZoneSettlementsPage: () => <section data-testid="settlements-stub">我的对账</section> }));

afterEach(() => { vi.unstubAllGlobals(); window.localStorage.clear(); window.history.replaceState({}, "", "/"); });

const MERCHANT_USER = { id: "mu-1", username: "商户甲账号", accountType: "merchant", merchantId: "m-1" };
const PLATFORM_USER = { id: "pu-1", username: "平台甲", accountType: "staff", merchantId: null };
const UNREAD = {
  bill_months: [{ month: "2026-08", unread_count: 2 }],
  ticket_settlement_unread_count: 1,
  dividend_statements: [{ id: "ds-1", period_label: "2026-07", kind: "monthly" }],
};
const UNREAD_PATH = "/api/v1/merchant-portal/unread-bills";

type Call = { url: string };
type Options = { unread?: unknown; grants?: string[]; switches?: unknown[] };

function json(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", data }), { status: 200, headers: { "content-type": "application/json" } });
}

function stubServer({ unread = UNREAD, grants, switches = [] }: Options = {}): Call[] {
  const calls: Call[] = [];
  vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    calls.push({ url });
    const path = url.split("?")[0]!;
    if (path.endsWith("/api/v1/access/effective-permissions")) {
      return json(grants ? { grants, denies: [], wildcard: false } : { grants: [], denies: [], wildcard: true });
    }
    if (path.endsWith("/api/v1/terminals/capability-switches/effective")) return json(switches);
    if (path.endsWith("/api/v1/security/page-views")) return json({ code: "K7MX3PQR" });
    if (path.endsWith(UNREAD_PATH)) return json(unread);
    return json([]);
  }));
  return calls;
}

async function mountApp(user: Record<string, unknown>, path: string, store: MerchantAppStore | null = null) {
  vi.resetModules();
  const client = await import("./client/adminClient");
  if (store) client.configureMerchantAppMode(store, "0.1.0");
  const { default: AdminApp } = await import("./AdminApp");
  window.localStorage.setItem("admin.session.v1", JSON.stringify({ user, sessionId: "s-1" }));
  window.history.replaceState({}, "", path);
  if (typeof HTMLElement.prototype.scrollTo !== "function") HTMLElement.prototype.scrollTo = () => undefined;
  render(<AdminApp />);
}

function called(calls: Call[], suffix: string) {
  return calls.some((call) => call.url.split("?")[0]!.endsWith(suffix));
}

/** 等终端开关读回、外壳安定，再断言「没有发」。 */
async function settled(calls: Call[]) {
  await waitFor(() => expect(called(calls, "/api/v1/terminals/capability-switches/effective")).toBe(true));
  await new Promise((resolve) => setTimeout(resolve, 50));
}

function rowWith(text: string): HTMLElement {
  const row = [...screen.getByTestId("merchant-unread-bills").querySelectorAll("li")].find((li) => li.textContent?.includes(text));
  if (!row) throw new Error(`提醒里没有「${text}」`);
  return row as HTMLElement;
}

describe("AdminApp 商户专区首页未读账单提醒", () => {
  it("商户形态账号打开 /admin：读未读账单，按月、门票分账、分红单各一行；点月份「查看」进我的账单并请求该月", async () => {
    const calls = stubServer();
    await mountApp(MERCHANT_USER, "/admin");

    await waitFor(() => expect(called(calls, UNREAD_PATH)).toBe(true));
    await waitFor(() => expect(screen.queryByTestId("merchant-unread-bills")?.textContent ?? "").toContain("2026 年 8 月账单 2 笔未读"));
    const text = screen.getByTestId("merchant-unread-bills").textContent ?? "";
    expect(text).toContain("未读账单提醒");
    expect(text).toContain("门票分账 1 笔未读");
    expect(text).toContain("分红单 2026-07 未读");

    fireEvent.click(rowWith("2026 年 8 月账单").querySelector("button")!);
    await waitFor(() => expect(window.location.pathname).toBe("/admin/merchant-zone/bills"));
    await waitFor(() => expect(calls.some((call) => call.url.includes("/api/v1/merchant-portal/fee-statement?month=2026-08"))).toBe(true), { timeout: 5000 });
    expect(screen.queryByTestId("merchant-unread-bills")).toBeNull();
  });

  it("点门票分账一行「查看」进我的对账", async () => {
    stubServer();
    await mountApp(MERCHANT_USER, "/admin");
    await waitFor(() => expect(screen.queryByTestId("merchant-unread-bills")?.textContent ?? "").toContain("门票分账 1 笔未读"));
    fireEvent.click(rowWith("门票分账").querySelector("button")!);
    await waitFor(() => expect(window.location.pathname).toBe("/admin/merchant-zone/settlements"));
  });

  it("没有驾驶舱权限（页面门禁拒绝）时提醒照样显示", async () => {
    stubServer({ grants: ["merchant.billing.view", "merchant.portal.settlement.view", "finance.dividend.view"] });
    await mountApp(MERCHANT_USER, "/admin");
    await waitFor(() => expect(screen.queryByTestId("merchant-unread-bills")?.textContent ?? "").toContain("2026 年 8 月账单 2 笔未读"));
    expect(screen.queryByTestId("overview-stub")).toBeNull();
  });

  it("商户手机应用打开根路径同样显示", async () => {
    stubServer();
    const store: MerchantAppStore = {
      tokens: () => ({ accessToken: "at-1", refreshToken: "rt-1" }),
      deviceId: () => "WBABCDEFGHJKMN",
      save: vi.fn(async () => undefined),
      clearTokens: vi.fn(async () => undefined),
    };
    await mountApp(MERCHANT_USER, "/", store);
    await waitFor(() => expect(screen.queryByTestId("merchant-unread-bills")?.textContent ?? "").toContain("分红单 2026-07 未读"));
  });

  it("三组都空不显示", async () => {
    const calls = stubServer({ unread: { bill_months: [], ticket_settlement_unread_count: 0, dividend_statements: [] } });
    await mountApp(MERCHANT_USER, "/admin");
    await waitFor(() => expect(called(calls, UNREAD_PATH)).toBe(true));
    await settled(calls);
    expect(screen.queryByTestId("merchant-unread-bills")).toBeNull();
  });

  it("平台账号打开 /admin 不发请求、不显示", async () => {
    const calls = stubServer();
    await mountApp(PLATFORM_USER, "/admin");
    await waitFor(() => expect(screen.getByTestId("overview-stub")).toBeTruthy());
    await settled(calls);
    expect(called(calls, UNREAD_PATH)).toBe(false);
    expect(screen.queryByTestId("merchant-unread-bills")).toBeNull();
  });

  it("商户在我的账单页不显示提醒", async () => {
    const calls = stubServer();
    await mountApp(MERCHANT_USER, "/admin/merchant-zone/bills");
    await waitFor(() => expect(called(calls, "/api/v1/merchant-portal/fee-statement")).toBe(true), { timeout: 5000 });
    await settled(calls);
    expect(called(calls, UNREAD_PATH)).toBe(false);
    expect(screen.queryByTestId("merchant-unread-bills")).toBeNull();
  });

  it("商户专区模块被终端开关关闭时不显示、不发请求", async () => {
    const calls = stubServer({ switches: [{ terminal_type: "merchant_portal", capability_code: "terminal.enabled", scope_type: "module", scope_id: "merchant" }] });
    await mountApp(MERCHANT_USER, "/admin");
    await waitFor(() => expect(screen.getByTestId("overview-stub")).toBeTruthy());
    await settled(calls);
    expect(called(calls, UNREAD_PATH)).toBe(false);
    expect(screen.queryByTestId("merchant-unread-bills")).toBeNull();
  });
});
