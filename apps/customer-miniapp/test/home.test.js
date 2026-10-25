// 文件版本：V0.01
// 首页（BSM「查看综合体介绍、营业时间、活动公告、票务入口」；第十三批裁决第 5 条「运营编辑后走审批，批准才对客人展示」）：
// 不登录也可看；服务端未发布的介绍与公告为空，本页不展示。
import { beforeEach, expect, test } from "vitest";

import { fire, flush, freshModules, installWx, mount, ok, q, text } from "./helpers.js";

export const HOME_ROUTES = {
  "GET /api/v1/customer/projects": ok([
    {
      project_id: "p1", project_name: "星河综合体", address: "滨江路 1 号", intro: "综合体介绍正文", announcement: "国庆活动公告正文",
      business_hours: [{ venue_id: "v1", venue_name: "主场馆", day_of_week: 5, open_time: "20:00", close_time: "02:00" }],
    },
    { project_id: "p2", project_name: "第二综合体", address: null, intro: null, announcement: null, business_hours: [] },
  ]),
  "GET /api/v1/customer/ticket-products": ok([
    { ticket_type_id: "t1", ticket_name: "单人通票", weekday_price_cent: 8800, weekend_price_cent: 9900, holiday_price_cent: null, currency: "CNY", status: "active" },
  ]),
};

beforeEach(() => freshModules());

test("homeShowsPublishedIntroAnnouncementHoursAndProductsWithoutLogin", async () => {
  const { requests } = installWx(HOME_ROUTES);
  const page = mount("pages/home/index");
  await flush();
  const first = text(q(page, "#project-p1"));
  expect(first).toContain("综合体介绍正文");
  expect(first).toContain("活动公告");
  expect(first).toContain("国庆活动公告正文");
  expect(first).toContain("主场馆：周五 20:00–次日 02:00");
  expect(text(q(page, "#project-p2"))).toContain("第二综合体");
  expect(text(q(page, "#project-p2"))).not.toContain("活动公告");
  expect(text(q(page, "#products"))).toContain("单人通票");
  expect(text(q(page, "#products"))).toContain("平日 ¥88.00 · 周末 ¥99.00 · 节假日 —");
  expect(requests.length).toBeGreaterThan(0);
  expect(requests.every((r) => !r.header["x-customer-token"])).toBe(true);
});

// BSM L858「查看票种、套餐、通票、游乐项目和酒吧规则」、L878「连接游乐系统，展示可预定项目和场次」：每个综合体只读列出开放中的游乐项目与
// 尚未结束的场次（起止时间、状态、余量），只展示、不在此预约。
test("homeShowsAmusementProjectsAndSessionsPerProject", async () => {
  const { requests } = installWx({
    ...HOME_ROUTES,
    "GET /api/v1/customer/amusement/projects": (req) => ok(req.path.includes("project_id=p1") ? [
      { amusement_project_id: "ap1", project_name: "旋转木马", open_status: "open", sessions: [
        { session_id: "s1", start_time: "2026-10-25T12:00:00Z", end_time: "2026-10-25T13:00:00Z", capacity: 20, used_capacity: 5, status: "scheduled" }] },
      { amusement_project_id: "ap2", project_name: "碰碰车", open_status: "open", sessions: [] },
    ] : []),
  });
  const page = mount("pages/home/index");
  await flush();
  const amusement = text(q(page, "#amusement-p1"));
  for (const part of ["游乐项目", "旋转木马", "已排期", "余 15 / 20", "碰碰车", "暂无可预定场次"]) expect(amusement).toContain(part);
  expect(q(page, "#amusement-p2")).toBeNull();
  expect(requests.filter((r) => r.key === "GET /api/v1/customer/amusement/projects").map((r) => r.path))
    .toEqual(["/api/v1/customer/amusement/projects?project_id=p1", "/api/v1/customer/amusement/projects?project_id=p2"]);
});

test("homeEntriesOpenBuyAndParking", async () => {
  const { wx } = installWx(HOME_ROUTES);
  const page = mount("pages/home/index");
  await flush();
  fire(page, ["#entry-buy"], "tap");
  fire(page, ["#entry-parking"], "tap");
  await flush();
  expect(wx.navigateTo.mock.calls.map((c) => c[0].url)).toEqual(["/pages/buy/index", "/pages/parking/index"]);
});
