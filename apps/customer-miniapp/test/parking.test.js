// 文件版本：V0.01
// 停车缴费（BSM「出口扫码支付，或在官方 App/小程序提前缴费后在离场宽限时限内离场」）：拉起参数为空显示「支付暂不可用，未扣款」；
// 有小程序微信支付参数才拉起，回来查单，渠道确认收款才显示已缴与离场宽限截止。
import path from "node:path";
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, header, installWx, mount, ok, q, require, ROOT, text } from "./helpers.js";

const TOKEN = { customer_token: "tok-1" };
const WX_PARAMS = { timeStamp: "1729756800", nonceStr: "n0nce", package: "prepay_id=wx2410p", signType: "RSA", paySign: "c2ln" };
const ROUTES = {
  "GET /api/v1/customer/parking/credits": ok({ project_id: "p1", business_date: "2026-10-24", status: "not_reached", eligible_amount_cent: 5000,
    threshold_cent: 20000, credit_minutes: 120 }),
  "GET /api/v1/customer/parking/sessions": ok({ parking_session_id: "ps1", project_id: "p1", plate_no: "粤B12345", temp_code: null,
    entered_at: "2026-10-24T10:00:00Z", gross_fee_cent: 1500, fee_cent: 1500, paid_cent: 0, due_cent: 1500, grace_until: null }),
};

beforeEach(() => freshModules());

async function lookedUp(routes) {
  const env = installWx({ ...ROUTES, ...routes }, TOKEN);
  const page = mount("pages/parking/index", { query: { project_id: "p1", code: "粤B12345" } });
  await flush();
  fire(page, ["#lookup"], "tap");
  await flush();
  return { ...env, page };
}

test("parkingWithoutLaunchShowsUnavailableAndNoPaidState", async () => {
  const { wx, requests, page } = await lookedUp({
    "POST /api/v1/customer/parking/payments": ok({ id: "pp1", parking_session_id: "ps1", amount_cent: 1500, status: "pending", payment_url: null, launch: null }),
  });
  expect((requests.find((r) => r.key === "GET /api/v1/customer/parking/sessions")?.path ?? "")).toContain(`plate_no=${encodeURIComponent("粤B12345")}`);
  expect(text(q(page, "#parking-credit"))).toContain("未达门槛");
  expect(text(q(page, "#due"))).toContain("¥15.00");
  fire(page, ["#pay"], "tap");
  await flush();
  expect(wx.requestPayment).not.toHaveBeenCalled();
  expect(text(q(page, "#parking-unavailable"))).toContain("支付暂不可用，未扣款");
  expect(requests.some((r) => r.key.endsWith("/confirm"))).toBe(false);
  expect(q(page, "#grace")).toBeNull();
});

test("parkingWechatPayThenConfirmShowsGrace", async () => {
  const { wx, page } = await lookedUp({
    "POST /api/v1/customer/parking/payments": ok({ id: "pp1", parking_session_id: "ps1", amount_cent: 1500, status: "pending", payment_url: null,
      launch: { kind: "miniapp_wechat", params: WX_PARAMS } }),
    "POST /api/v1/customer/parking/payments/pp1/confirm": ok({ id: "pp1", parking_session_id: "ps1", amount_cent: 1500, status: "paid",
      paid_at: "2026-10-24T12:55:00Z", grace_until: "2026-10-24T13:15:00Z" }),
  });
  fire(page, ["#pay"], "tap");
  await flush();
  expect(wx.requestPayment).toHaveBeenCalledTimes(1);
  const { success, fail, complete, ...passed } = wx.requestPayment.mock.calls[0][0];
  expect(passed).toEqual(WX_PARAMS);
  expect(text(q(page, "#grace"))).toContain("已缴费 ¥15.00");
});

const project = (id, name) => ({ project_id: id, project_name: name, address: null, intro: null, announcement: null, business_hours: [] });
const closedProject = (id) => ({ terminal_type: "miniapp", capability_code: "terminal.enabled", scope_type: "project", scope_id: id, enabled: false });

// BSM L1435「在官方 App/小程序提前缴费后在离场宽限时限内离场」：从首页入口进入（不带项目）也能按车牌查费——只有一个综合体就直接用它，
// 请求带该综合体（X-Project-Id，后端按综合体拒绝）。
test("parkingFromHomeEntryUsesTheOnlyProject", async () => {
  const { requests } = installWx({ ...ROUTES, "GET /api/v1/customer/projects": ok([project("p1", "长沙综合体")]) }, TOKEN);
  const page = mount("pages/parking/index");
  await flush();
  fire(page, ["#code"], "input", { value: "粤B12345" });
  fire(page, ["#lookup"], "tap");
  await flush();
  const lookup = requests.find((r) => r.key === "GET /api/v1/customer/parking/sessions");
  expect(lookup?.path ?? "").toContain("project_id=p1");
  expect(header(lookup, "x-project-id")).toBe("p1");
  expect(text(q(page, "#due"))).toContain("¥15.00");
});

// 多个综合体时由客人选停车场所在综合体再查；客人端已暂停服务的综合体不列（PGM「关闭后菜单不可见或不可点」）。
test("parkingFromHomeEntryWithSeveralProjectsLetsGuestChooseOpenOne", async () => {
  const { requests } = installWx({
    ...ROUTES,
    "GET /api/v1/customer/projects": ok([project("p1", "长沙综合体"), project("p2", "株洲综合体"), project("p3", "湘潭综合体")]),
    "GET /api/v1/customer/terminal-switches/effective": ok([closedProject("p3")]),
  }, TOKEN);
  await require(path.join(ROOT, "lib/api.js")).loadSwitches();
  const page = mount("pages/parking/index");
  await flush();
  expect(text(q(page, "#project-picker"))).toContain("请选择停车场所在综合体");
  expect(page.instance.data.projectNames).toEqual(["长沙综合体", "株洲综合体"]);
  fire(page, ["#project-picker"], "change", { value: "1" });
  fire(page, ["#code"], "input", { value: "粤B12345" });
  fire(page, ["#lookup"], "tap");
  await flush();
  const lookup = requests.find((r) => r.key === "GET /api/v1/customer/parking/sessions");
  expect(lookup?.path ?? "").toContain("project_id=p2");
  expect(header(lookup, "x-project-id")).toBe("p2");
});

// 停车出口码带的综合体被关：本页只提示暂停服务、不发停车请求。
test("parkingLinkToClosedProjectShowsPaused", async () => {
  const { requests } = installWx({
    ...ROUTES,
    "GET /api/v1/customer/projects": ok([project("p1", "长沙综合体"), project("p2", "株洲综合体")]),
    "GET /api/v1/customer/terminal-switches/effective": ok([closedProject("p2")]),
  }, TOKEN);
  await require(path.join(ROOT, "lib/api.js")).loadSwitches();
  const page = mount("pages/parking/index", { query: { project_id: "p2", code: "粤B12345" } });
  await flush();
  expect(text(q(page, "#parking-paused"))).toContain("暂停服务");
  expect(q(page, "#lookup")).toBeNull();
  expect(requests.some((r) => r.key.includes("/customer/parking/"))).toBe(false);
});
