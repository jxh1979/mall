// 文件版本：V0.01
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { REPLAYED_NOTICE, announceReplayed } from "@complex/customer-core";

import CustomerApp from "./CustomerApp";
import { platform } from "./platform";

// 客人 App 外壳：底部四标签＋二级页、返回键、终端开关隐藏、令牌只在内存与本端安全会话里。
type Call = { path: string; query: string; method: string; headers: Headers };
let calls: Call[] = [];

function envelope(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(status >= 400 ? data : { code: "OK", message: "ok", data }), {
    status,
    headers: { "content-type": "application/json" },
  });
}

const SWITCHES = "/api/v1/customer/terminal-switches/effective";
const TOKEN = "tok-secret-1";

function stubBackend(routes: Record<string, () => Response> = {}): void {
  calls = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const [path = "", query = ""] = String(input).replace(/^https?:\/\/[^/]+/, "").split("?");
      calls.push({ path, query, method: init?.method ?? "GET", headers: new Headers(init?.headers) });
      const route = routes[path];
      if (route) return Promise.resolve(route());
      if (path === SWITCHES) return Promise.resolve(envelope({ items: [] }));
      // 匿名会话探测：后端返回 204 空会话。
      if (path === "/api/v1/customer/session") return Promise.resolve(new Response(null, { status: 204 }));
      if (path === "/api/v1/customer/auth/phone-login") {
        return Promise.resolve(envelope({ customer_id: "c-1", masked_phone: "138****8888", customer_token: TOKEN, expires_at: "2026-12-31T00:00:00Z" }));
      }
      if (path === "/api/v1/customer/points") return Promise.resolve(envelope({ customer_id: "c-1", balance: 10, level: "普通会员", recent_summary: null }));
      return Promise.resolve(envelope({ items: [] }));
    }),
  );
}

const switchRow = (scopeType: string, scopeId: string) => ({
  terminal_type: "app",
  capability_code: "terminal.enabled",
  scope_type: scopeType,
  scope_id: scopeId,
  enabled: false,
});

const title = () => document.querySelector(".customer-page-title")?.textContent;

async function login(): Promise<void> {
  render(<CustomerApp />);
  await waitFor(() => expect(screen.queryByTestId("customer-login-view")).not.toBeNull());
  fireEvent.change(screen.getByTestId("login-phone"), { target: { value: "13800138888" } });
  fireEvent.change(screen.getByTestId("login-sms"), { target: { value: "000000" } });
  fireEvent.click(screen.getByTestId("login-submit"));
  await waitFor(() => expect(screen.queryByTestId("customer-app")).not.toBeNull(), { timeout: 5000 });
}

async function openEntry(tab: string, entry: string): Promise<void> {
  await waitFor(() => expect(screen.queryByTestId(`customer-tab-${tab}`)).not.toBeNull());
  fireEvent.click(screen.getByTestId(`customer-tab-${tab}`));
  await waitFor(() => expect(screen.queryByTestId(`customer-entry-${entry}`)).not.toBeNull());
  fireEvent.click(screen.getByTestId(`customer-entry-${entry}`));
}

describe("CustomerApp（客人 App 外壳）", () => {
  afterEach(async () => {
    act(() => announceReplayed(false));
    await platform.session.clear();
    vi.unstubAllGlobals();
  });

  // M725-F03（治理地图「同一次操作重复提交时，返回第一次的结果并提示已完成」）：提示挂在外壳上，任何页面都能看到。
  it("客人端外壳挂着「已完成」提示", async () => {
    stubBackend();
    await login();
    act(() => announceReplayed(true));
    expect(document.getElementById("main-content")!.querySelector("[data-testid='replayed-notice']")?.textContent).toContain(REPLAYED_NOTICE);
  });

  it("tokenOnlyInMemoryHeaderNotInStorageOrUrl：令牌只走请求头，不进存储、地址与页面", async () => {
    const setItem = vi.spyOn(Storage.prototype, "setItem");
    stubBackend();
    await login();
    await openEntry("me", "points");
    await waitFor(() => expect(calls.some((c) => c.path === "/api/v1/customer/points")).toBe(true));

    const pointsCall = calls.find((c) => c.path === "/api/v1/customer/points")!;
    expect(pointsCall.headers.get("x-customer-token")).toBe(TOKEN);
    expect(pointsCall.headers.get("x-terminal-type")).toBe("app");
    expect(pointsCall.headers.get("x-module-code")).toBe("points");
    expect(calls.every((c) => !`${c.path}?${c.query}`.includes(TOKEN))).toBe(true);
    expect(window.location.href).not.toContain(TOKEN);
    expect(document.body.textContent).not.toContain(TOKEN);
    expect(setItem).not.toHaveBeenCalled();
    await expect(platform.session.load()).resolves.toBe(TOKEN);
  });

  it("bottomTabsAndAndroidBackButtonNavigate：二级页返回上一页，标签根页回首页，首页再按退出", async () => {
    let backButton: () => void = () => undefined;
    vi.spyOn(platform, "onBackButton").mockImplementation((listener) => {
      backButton = listener;
      return () => undefined;
    });
    const exitApp = vi.spyOn(platform, "exitApp");
    stubBackend();
    await login();

    await openEntry("me", "points");
    await waitFor(() => expect(title()).toBe("积分 / 签到"));
    fireEvent.click(screen.getByTestId("customer-back"));
    expect(title()).toBe("我的");

    fireEvent.click(screen.getByTestId("customer-entry-points"));
    expect(title()).toBe("积分 / 签到");
    act(() => backButton());
    expect(title()).toBe("我的");
    act(() => backButton());
    expect(title()).toBe("首页");
    expect(exitApp).not.toHaveBeenCalled();
    act(() => backButton());
    expect(exitApp).toHaveBeenCalledTimes(1);
  });

  it("terminalDisabledShowsPausedPage：本端被关或开关拉取失败即暂停服务", async () => {
    stubBackend({ [SWITCHES]: () => envelope({ items: [switchRow("global", "global")] }) });
    const { unmount } = render(<CustomerApp />);
    await waitFor(() => expect(screen.queryByTestId("customer-paused")?.textContent).toContain("本端暂停服务"));
    unmount();

    stubBackend({ [SWITCHES]: () => envelope({ code: "INTERNAL_ERROR", message: "boom" }, 500) });
    render(<CustomerApp />);
    await waitFor(() => expect(screen.queryByTestId("customer-paused")?.textContent).toContain("本端暂停服务"));
  });

  it("moduleSwitchHidesOnlyThatModule：储值模块被关只隐藏储值入口；拉开关不带模块头", async () => {
    stubBackend({ [SWITCHES]: () => envelope({ items: [switchRow("module", "wallet")] }) });
    await login();
    await waitFor(() => expect(screen.queryByTestId("customer-tab-me")).not.toBeNull());
    fireEvent.click(screen.getByTestId("customer-tab-me"));

    await waitFor(() => expect(screen.queryByTestId("customer-entry-points")).not.toBeNull());
    expect(screen.queryByTestId("customer-entry-wallet")).toBeNull();
    const switchCalls = calls.filter((c) => c.path === SWITCHES);
    expect(switchCalls.length).toBeGreaterThan(0);
    expect(switchCalls.every((c) => c.headers.get("x-module-code") === null && c.headers.get("x-terminal-type") === "app")).toBe(true);
  });

  it("deepLinkOpensWhitelistedPageOnly：停车深链带项目与车牌直达，外站链接不认", async () => {
    let deliver: (url: string) => void = () => undefined;
    vi.spyOn(platform, "onDeepLink").mockImplementation((listener) => {
      deliver = listener;
      return () => undefined;
    });
    stubBackend();
    await login();

    act(() => deliver("https://evil.example/c/parking?project_id=proj-1"));
    expect(title()).toBe("首页");

    act(() => deliver(`https://c.complex.test/c/parking?project_id=proj-1&code=${encodeURIComponent("粤A12345")}`));
    await waitFor(() => expect(title()).toBe("停车缴费"));
    expect(screen.getByDisplayValue("粤A12345")).toBeInTheDocument();
    await waitFor(() => expect(calls.some((c) => c.path === "/api/v1/customer/parking/credits")).toBe(true));
    const credit = calls.find((c) => c.path === "/api/v1/customer/parking/credits")!;
    expect(credit.query).toContain("project_id=proj-1");
    expect(credit.headers.get("x-module-code")).toBe("parking");
  });

  // BSM「未核销的票在到期前通过官方 App 或小程序弹出的到期提醒（业务参数，按项目可配，出厂默认到期前二十四小时，须同时配置上下限）」：
  // 启动（登录）与回前台拉取本人即将到期的未核销票并弹出；已点「知道了」的本次运行不再重复弹。
  it("ticketExpiryReminderPopsUpAfterLoginAndOnResume：登录后与回前台弹出未核销票到期提醒", async () => {
    const resumeListeners: Array<() => void> = [];
    vi.spyOn(platform, "onResume").mockImplementation((listener) => {
      resumeListeners.push(listener);
      return () => undefined;
    });
    const reminder = (id: string, name: string) => ({ ticket_id: id, parent_ticket_id: `p-${id}`, ticket_name: name, order_id: "o-1",
      valid_to: "2026-10-25T04:00:00Z" });
    let reminders = [reminder("e-1", "夜场单人票")];
    stubBackend({ "/api/v1/customer/ticket-expiry-reminders": () => envelope(reminders) });
    await login();
    await waitFor(() => expect(screen.queryByTestId("ticket-expiry-reminder")?.textContent ?? "").toContain("夜场单人票"));
    fireEvent.click(screen.getByRole("button", { name: "知道了" }));
    await waitFor(() => expect(screen.queryByTestId("ticket-expiry-reminder")).toBeNull());

    reminders = [reminder("e-1", "夜场单人票"), reminder("e-2", "游乐次票")];
    act(() => resumeListeners.forEach((listener) => listener()));
    await waitFor(() => expect(screen.queryByTestId("ticket-expiry-reminder")?.textContent ?? "").toContain("游乐次票"));
    expect(screen.getByTestId("ticket-expiry-reminder").textContent).not.toContain("夜场单人票");
  });

  // ENUM「扫码点单身份：非会员可以浏览、点单和付款；使用会员权益时才引导本人登录或注册」。
  it("anonymousTableDeepLinkOpensScanOrderWithoutLogin：未登录收到桌台码深链直接进扫码点单，请求不带令牌；返回回到登录页", async () => {
    let deliver: (url: string) => void = () => undefined;
    vi.spyOn(platform, "onDeepLink").mockImplementation((listener) => {
      deliver = listener;
      return () => undefined;
    });
    stubBackend({ "/api/v1/customer/bar-tables/tbl-1/menu": () => envelope({ items: [{ product_id: "p1", product_name: "莫吉托",
      product_type: "drink", price_cent: 3800, currency: "CNY", sold_out: false }] }) });
    render(<CustomerApp />);
    await waitFor(() => expect(screen.queryByTestId("customer-login-view")).not.toBeNull());
    act(() => deliver("https://c.complex.test/c/table?table_id=tbl-1"));
    await waitFor(() => expect(title()).toBe("扫码点单"));
    await waitFor(() => expect(screen.queryByTestId("scan-item-p1")?.textContent ?? "").toContain("莫吉托"));
    expect(screen.queryByTestId("customer-login-view")).toBeNull();
    const menu = calls.find((c) => c.path === "/api/v1/customer/bar-tables/tbl-1/menu")!;
    expect(menu.headers.get("x-customer-token")).toBeNull();
    expect(menu.headers.get("x-module-code")).toBe("bar");
    fireEvent.click(screen.getByTestId("customer-back"));
    await waitFor(() => expect(screen.queryByTestId("customer-login-view")).not.toBeNull());
  });

  it("anonymousCanStartScanOrderFromLoginView：登录页可不登录直接扫桌台码点单", async () => {
    stubBackend();
    render(<CustomerApp />);
    await waitFor(() => expect(screen.queryByTestId("customer-anonymous-scan")).not.toBeNull());
    fireEvent.click(screen.getByTestId("customer-anonymous-scan"));
    await waitFor(() => expect(title()).toBe("扫码点单"));
    expect(screen.getByTestId("scan-start")).toBeInTheDocument();
  });

  it("invitationDeepLinkOpensAcceptPage：已登录收到邀请函深链进入「接受邀请」并按码载荷预览", async () => {
    let deliver: (url: string) => void = () => undefined;
    vi.spyOn(platform, "onDeepLink").mockImplementation((listener) => {
      deliver = listener;
      return () => undefined;
    });
    stubBackend();
    await login();
    act(() => deliver("https://c.complex.test/c/invitation?token=inv_abc&sig=sig123&table_id=t-a&capacity=6"));
    await waitFor(() => expect(title()).toBe("接受邀请"));
    await waitFor(() => expect(calls.some((c) => c.path === "/api/v1/customer/bar-invitations/preview")).toBe(true));
    const preview = calls.find((c) => c.path === "/api/v1/customer/bar-invitations/preview")!;
    for (const part of ["invitation_token=inv_abc", "invitation_signature=sig123", "table_id=t-a", "capacity=6"]) {
      expect(preview.query).toContain(part);
    }
    expect(preview.headers.get("x-module-code")).toBe("bar");
  });

  it("meAndBarTabsListPages：「我的」与「酒吧」标签列出本组页面入口", async () => {
    stubBackend();
    await login();
    fireEvent.click(screen.getByTestId("customer-tab-me"));
    for (const page of ["points", "wallet", "vouchers", "bottles", "complaints", "student", "account", "deregister"]) {
      await waitFor(() => expect(screen.queryByTestId(`customer-entry-${page}`)).not.toBeNull());
    }
    fireEvent.click(screen.getByTestId("customer-tab-bar"));
    for (const page of ["reserve", "reservations", "invitation", "scan"]) {
      await waitFor(() => expect(screen.queryByTestId(`customer-entry-${page}`)).not.toBeNull());
    }
  });

  const project = (id: string, name: string) => ({ project_id: id, project_name: name, address: null, intro: null, announcement: null,
    business_hours: [] });

  // PGM L736「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、L737「关闭后菜单不可见或不可点，接口也要拒绝业务动作」：
  // 只有一个综合体时外壳以它为当前综合体——业务请求带 X-Project-Id（后端按综合体拒绝），该综合体被关即本端暂停服务。
  it("projectSwitchAppliesToTheCurrentProject：请求带当前综合体；当前综合体被关即暂停服务", async () => {
    const projects = { "/api/v1/customer/projects": () => envelope([project("proj-1", "长沙综合体")]) };
    stubBackend(projects);
    await login();
    await openEntry("me", "points");
    await waitFor(() => expect(calls.find((c) => c.path === "/api/v1/customer/points")?.headers.get("x-project-id")).toBe("proj-1"));
    cleanup();

    stubBackend({ ...projects, [SWITCHES]: () => envelope({ items: [switchRow("project", "proj-1")] }) });
    render(<CustomerApp />);
    await waitFor(() => expect(screen.queryByTestId("customer-paused")?.textContent).toContain("本端暂停服务"));
  });

  // PGM L736「可以按全局、综合体……开关」：多个综合体时，购票请求带所选票种所属的综合体、订台请求带所选台位所属的综合体
  // （票种列表与订台选项每项带 project_id），后端按综合体的终端开关拒绝。
  it("buyAndReserveCarrySelectedItemProject：购票带所选票种的综合体，订台带所选台位的综合体", async () => {
    const product = (id: string, name: string, projectId: string) => ({ ticket_type_id: id, ticket_name: name, weekday_price_cent: 100,
      weekend_price_cent: 100, holiday_price_cent: 100, currency: "CNY", status: "published", bar_id: null, bar_name: null,
      refundable: false, entitlements: [], project_id: projectId });
    const table = (id: string, barId: string, projectId: string) => ({ bar_id: barId, table_id: id, table_type: "card_seat",
      table_name: id, capacity: 6, allowed_pass_free_guest_count: 2, min_consumption_cent: 100000, currency: "CNY", status: "active",
      reserved: false, service_fee_rate_bps: 1000, deposit_amount_cent: 20000, project_id: projectId });
    stubBackend({
      "/api/v1/customer/projects": () => envelope([project("proj-1", "长沙综合体"), project("proj-2", "株洲综合体")]),
      "/api/v1/customer/ticket-products": () => envelope({ items: [product("tt-1", "长沙通票", "proj-1"), product("tt-2", "株洲通票", "proj-2")] }),
      "/api/v1/customer/ticket-orders": () => envelope({ id: "o-1", order_no: "TO-1", amount_cent: 100, currency: "CNY", pay_status: "unpaid",
        issue_status: "not_issued", status: "created", project_id: "proj-2" }),
      "/api/v1/customer/bar-reservation-options": () => envelope({ items: [table("t-1", "bar-1", "proj-1"), table("t-2", "bar-2", "proj-2")] }),
      "/api/v1/customer/bar-reservations": () => envelope({ id: "res-1", bar_id: "bar-1", table_id: "t-1", deposit_amount_cent: 20000,
        deposit_status: "pending", status: "pending" }),
    });
    await login();
    await openEntry("home", "buy");
    await waitFor(() => expect(screen.queryByTestId("buy-ticket-type")).not.toBeNull());
    await userEvent.click(screen.getByTestId("buy-ticket-type").querySelector("input") ?? screen.getByTestId("buy-ticket-type"));
    await waitFor(() => expect(screen.queryByTitle("株洲通票")).not.toBeNull());
    await userEvent.click(screen.getByTitle("株洲通票"));
    fireEvent.click(screen.getByTestId("buy-ticket-submit"));
    await waitFor(() => expect(calls.find((c) => c.path === "/api/v1/customer/ticket-orders" && c.method === "POST")?.headers
      .get("x-project-id")).toBe("proj-2"));

    await openEntry("bar", "reserve");
    await waitFor(() => expect(screen.queryByTestId("reservation-pick-t-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("reservation-pick-t-1"));
    await waitFor(() => expect(screen.queryByLabelText("到店时间")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("到店时间"), { target: { value: "2026-10-25T20:00" } });
    fireEvent.change(screen.getByLabelText("最晚到店时间"), { target: { value: "2026-10-25T21:00" } });
    await waitFor(() => expect(screen.getByTestId("reservation-submit")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("reservation-submit"));
    await waitFor(() => expect(calls.find((c) => c.path === "/api/v1/customer/bar-reservations" && c.method === "POST")?.headers
      .get("x-project-id")).toBe("proj-1"));
  });

  // 2026-10-25 操作员更正「即使注销，门票是不可以退的，除非碰上特殊安全情况」（撤回第十三批第 11 条）：客人端不设退票入口——
  // 「我的票」不读退票申请、不显示「申请退票」。
  it("myTicketsHaveNoRefundEntry：我的票不读退票申请、不显示申请退票", async () => {
    stubBackend({
      "/api/v1/customer/my-tickets": () => envelope({ items: [{ id: "e-1", ticket_name: "夜场单人票", entitlement_type: "entry", status: "active",
        total_times: 1, used_times: 0, remaining_times: 1, num_people: 1, valid_from: null, valid_to: null, parent_ticket_id: "pt-1",
        slot_index: 1, ticket_no: "TK-1" }] }),
    });
    await login();
    await openEntry("tickets", "orders");
    await waitFor(() => expect(screen.queryByTestId("ticket-item")?.textContent ?? "").toContain("夜场单人票"));
    expect(calls.some((c) => c.path === "/api/v1/customer/ticket-refunds")).toBe(false);
    expect(screen.queryByText("申请退票")).toBeNull();
  });

  // 多个综合体时，深链带的综合体（停车出口码 project_id）被关：该页只显示暂停服务、不发业务请求，可返回。
  it("linkToClosedProjectShowsPausedPage：深链指向被关的综合体只显示暂停服务，可返回", async () => {
    let deliver: (url: string) => void = () => undefined;
    vi.spyOn(platform, "onDeepLink").mockImplementation((listener) => {
      deliver = listener;
      return () => undefined;
    });
    stubBackend({
      "/api/v1/customer/projects": () => envelope([project("proj-1", "长沙综合体"), project("proj-2", "株洲综合体")]),
      [SWITCHES]: () => envelope({ items: [switchRow("project", "proj-2")] }),
    });
    await login();
    act(() => deliver(`https://c.complex.test/c/parking?project_id=proj-2&code=${encodeURIComponent("粤A12345")}`));
    await waitFor(() => expect(screen.queryByTestId("customer-project-paused")?.textContent ?? "").toContain("暂停服务"));
    expect(title()).toBe("停车缴费");
    expect(calls.some((c) => c.path.startsWith("/api/v1/customer/parking/"))).toBe(false);
    fireEvent.click(screen.getByTestId("customer-back"));
    expect(title()).toBe("首页");
    expect(screen.queryByTestId("customer-project-paused")).toBeNull();
  });

  // 恢复会话时断网或服务端异常不算会话失效：本端令牌留着（按未登录显示），回到前台再恢复。
  it("restoreKeepsTokenOnNetworkErrorAndRetriesOnResume：断网恢复会话不删令牌，回前台再恢复", async () => {
    const resumeListeners: Array<() => void> = [];
    vi.spyOn(platform, "onResume").mockImplementation((listener) => {
      resumeListeners.push(listener);
      return () => undefined;
    });
    await platform.session.save(TOKEN);
    let online = false;
    stubBackend({
      "/api/v1/customer/session": () => {
        if (!online) throw new TypeError("Failed to fetch");
        return envelope({ customer_id: "c-1", masked_phone: "138****8888", expires_at: null });
      },
    });
    render(<CustomerApp />);
    await waitFor(() => expect(screen.queryByTestId("customer-login-view")).not.toBeNull());
    await expect(platform.session.load()).resolves.toBe(TOKEN);
    online = true;
    act(() => resumeListeners.forEach((listener) => listener()));
    await waitFor(() => expect(screen.queryByTestId("customer-app")).not.toBeNull());
    expect(calls.filter((c) => c.path === "/api/v1/customer/session").every((c) => c.headers.get("x-customer-token") === TOKEN)).toBe(true);
  });

  // BSM L857「查看综合体介绍、营业时间、活动公告、票务入口」（首页只调匿名接口，与小程序首页同口径）：未登录也能看首页；其余页面仍要登录。
  it("anonymousCanViewHomeFromLoginView：登录页可不登录先看首页，返回回到登录页；首页入口仍要登录", async () => {
    stubBackend({ "/api/v1/customer/projects": () => envelope([project("proj-1", "长沙综合体"), project("proj-2", "株洲综合体")]) });
    render(<CustomerApp />);
    await waitFor(() => expect(screen.queryByTestId("customer-anonymous-home")).not.toBeNull());
    fireEvent.click(screen.getByTestId("customer-anonymous-home"));
    await waitFor(() => expect(screen.queryAllByTestId("home-project").map((n) => n.textContent ?? "").join()).toContain("长沙综合体"));
    expect(title()).toBe("首页");
    expect(calls.every((c) => c.headers.get("x-customer-token") === null)).toBe(true);
    fireEvent.click(screen.getByTestId("customer-back"));
    await waitFor(() => expect(screen.queryByTestId("customer-login-view")).not.toBeNull());
    fireEvent.click(screen.getByTestId("customer-anonymous-home"));
    await waitFor(() => expect(screen.queryByTestId("customer-entry-buy")).not.toBeNull());
    fireEvent.click(screen.getByTestId("customer-entry-buy"));
    await waitFor(() => expect(screen.queryByTestId("customer-login-view")).not.toBeNull());
  });

  it("logoutRevokesServerSessionThenClearsToken", async () => {
    stubBackend();
    await login();
    await waitFor(() => expect(screen.queryByTestId("customer-tab-me")).not.toBeNull());
    fireEvent.click(screen.getByTestId("customer-tab-me"));
    fireEvent.click(screen.getByTestId("customer-logout"));

    await waitFor(() => expect(screen.queryByTestId("customer-login-view")).not.toBeNull());
    const logout = calls.find((c) => c.path === "/api/v1/customer/auth/logout")!;
    expect(logout.method).toBe("POST");
    expect(logout.headers.get("x-customer-token")).toBe(TOKEN);
    await expect(platform.session.load()).resolves.toBeNull();
  });
});
