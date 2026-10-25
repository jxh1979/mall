// 文件版本：V0.02
// 小程序入口（app.js）：启动与回前台拉终端开关（PGM「关闭后菜单不可见或不可点，接口也要拒绝业务动作」：本端被关或拉取失败 → 本端暂停服务；
// 模块被关只隐藏该模块入口）；已登录时弹未核销票到期提醒（BSM「未核销的票在到期前通过官方 App 或小程序弹出的到期提醒」），
// 点「知道了」后本次运行内不再弹同一张票。
import path from "node:path";
import { beforeEach, expect, test, vi } from "vitest";

import { fail, fire, flush, freshModules, header, installWx, mount, ok, q, require, ROOT } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const SESSION = { "GET /api/v1/customer/session": ok({ customer_id: "c1", masked_phone: "138****0000", expires_at: null }) };

function loadApp() {
  let definition;
  globalThis.App = (def) => {
    definition = def;
  };
  require(path.join(ROOT, "app.js"));
  return definition;
}

beforeEach(() => freshModules());

test("expiryReminderPopsOnShowAndNotRepeatedAfterDismiss", async () => {
  const { wx } = installWx({
    ...SESSION,
    "GET /api/v1/customer/terminal-switches/effective": ok([]),
    "GET /api/v1/customer/ticket-expiry-reminders": ok([
      { ticket_id: "tk1", parent_ticket_id: "P1", ticket_name: "单人通票", order_id: "o1", valid_to: "2026-10-25T12:00:00Z" },
    ]),
  }, TOKEN);
  const app = loadApp();
  app.onLaunch();
  app.onShow();
  await flush();
  expect(wx.showModal).toHaveBeenCalledTimes(1);
  expect(wx.showModal.mock.calls[0][0]).toMatchObject({ title: "门票即将到期", showCancel: false, confirmText: "知道了" });
  expect(wx.showModal.mock.calls[0][0].content).toContain("单人通票 将于");
  app.onShow();
  await flush();
  expect(wx.showModal).toHaveBeenCalledTimes(1);
});

// OBS-13-01（BSM L820）：降级预警、「已保级」与降级补发通知只在站内弹出（BSM L1596 客人短信清单不含等级通知）；点「知道了」回写已读。
test("levelNoticePopsOnShowAndMarksReadOnConfirm", async () => {
  const { wx, requests } = installWx({
    ...SESSION,
    "GET /api/v1/customer/terminal-switches/effective": ok([]),
    "GET /api/v1/customer/level-notices": ok([
      { id: "lvn-1", notice_type: "downgrade_warning", current_level: "gold", current_level_name: "金卡", target_level: "silver",
        target_level_name: "银卡", cycle_end: "2031-06-15T04:00:00Z", created_at: "2031-05-17T04:00:00Z" },
    ]),
    "POST /api/v1/customer/level-notices/lvn-1/read": ok(null),
  }, TOKEN);
  const app = loadApp();
  app.onLaunch();
  app.onShow();
  await vi.waitFor(() => expect(wx.showModal.mock.calls.some((c) => c[0].title === "会员等级通知")).toBe(true));
  const modal = wx.showModal.mock.calls.find((c) => c[0].title === "会员等级通知")[0];
  expect(modal).toMatchObject({ showCancel: false, confirmText: "知道了" });
  expect(modal.content).toContain("将降至银卡");
  expect(modal.content).toContain("这只是提醒");
  expect(modal.content).not.toContain("silver");
  await vi.waitFor(() => expect(requests.some((r) => r.key === "POST /api/v1/customer/level-notices/lvn-1/read")).toBe(true));
});

test("terminalClosedOrSwitchFetchFailureShowsPausedPage", async () => {
  const closed = installWx({
    "GET /api/v1/customer/terminal-switches/effective": ok([
      { terminal_type: "miniapp", capability_code: "terminal.enabled", scope_type: "global", scope_id: "*", enabled: false },
    ]),
  });
  loadApp().onShow();
  await flush();
  expect(closed.wx.reLaunch).toHaveBeenCalledWith({ url: "/pages/paused/index" });

  freshModules();
  const broken = installWx({ "GET /api/v1/customer/terminal-switches/effective": fail(500, null, "服务异常") });
  loadApp().onShow();
  await flush();
  expect(broken.wx.reLaunch).toHaveBeenCalledWith({ url: "/pages/paused/index" });
});

test("moduleSwitchHidesOnlyThatEntry", async () => {
  installWx({
    "GET /api/v1/customer/terminal-switches/effective": ok([
      { terminal_type: "miniapp", capability_code: "terminal.enabled", scope_type: "module", scope_id: "parking", enabled: false },
    ]),
    "GET /api/v1/customer/projects": ok([]),
    "GET /api/v1/customer/ticket-products": ok([]),
  });
  loadApp().onShow();
  await flush();
  const page = mount("pages/home/index");
  await flush();
  expect(q(page, "#entry-buy")).not.toBeNull();
  expect(q(page, "#entry-parking")).toBeNull();
});

const project = (id) => ({ project_id: id, project_name: `综合体${id}`, address: null, intro: null, announcement: null, business_hours: [] });

// PGM L736「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」、L737「关闭后菜单不可见或不可点，接口也要拒绝业务动作」：
// 只有一个综合体时以它为当前综合体——业务请求带 X-Project-Id（后端按综合体拒绝），该综合体被关即本端暂停服务。
test("onlyProjectIsCurrentProjectAndClosingItPausesTheMiniapp", async () => {
  const open = installWx({
    "GET /api/v1/customer/projects": ok([project("p1")]),
    "GET /api/v1/customer/terminal-switches/effective": ok([]),
    "GET /api/v1/customer/orders": ok([]),
    "GET /api/v1/customer/my-tickets": ok([]),
  }, TOKEN);
  loadApp().onShow();
  await flush();
  expect(open.wx.reLaunch).not.toHaveBeenCalled();
  mount("pages/orders/index");
  await flush();
  expect(header(open.requests.find((r) => r.key === "GET /api/v1/customer/orders"), "x-project-id")).toBe("p1");

  freshModules();
  const closed = installWx({
    "GET /api/v1/customer/projects": ok([project("p1")]),
    "GET /api/v1/customer/terminal-switches/effective": ok([
      { terminal_type: "miniapp", capability_code: "terminal.enabled", scope_type: "project", scope_id: "p1", enabled: false },
    ]),
  });
  loadApp().onShow();
  await flush();
  expect(closed.wx.reLaunch).toHaveBeenCalledWith({ url: "/pages/paused/index" });
});

// BSM L1596「未核销的票在到期前通过官方 App 或小程序弹出的到期提醒」：本次运行中刚登录的客人登录成功即拉一次（与 App 同口径），
// 之后回到前台不重复弹同一张票。
test("expiryReminderPopsRightAfterLoginInThisRun", async () => {
  const { wx } = installWx({
    "POST /api/v1/customer/auth/phone-login": ok({ customer_id: "c1", masked_phone: "138****0000", customer_token: "tok-new", expires_at: null }),
    "GET /api/v1/customer/terminal-switches/effective": ok([]),
    "GET /api/v1/customer/ticket-expiry-reminders": ok([
      { ticket_id: "tk1", parent_ticket_id: "P1", ticket_name: "单人通票", order_id: "o1", valid_to: "2026-10-25T12:00:00Z" },
    ]),
  });
  const app = loadApp();
  app.onLaunch();
  app.onShow();
  await flush();
  expect(wx.showModal).not.toHaveBeenCalled();
  const page = mount("pages/login/index");
  fire(page, ["#phone"], "input", { value: "13800000000" });
  fire(page, ["#sms"], "input", { value: "123456" });
  fire(page, ["#submit"], "tap");
  await flush();
  expect(wx.showModal).toHaveBeenCalledTimes(1);
  expect(wx.showModal.mock.calls[0][0]).toMatchObject({ title: "门票即将到期" });
  app.onShow();
  await flush();
  expect(wx.showModal).toHaveBeenCalledTimes(1);
});
