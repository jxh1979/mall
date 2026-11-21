// 文件版本：V0.03
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ARCHIVED_CATEGORIES, ARCHIVED_CONTENT_VIEW_PERMISSIONS, ArchivedContentPage } from "./ArchivedContentPage";
import { adminRoutes } from "../adminRoutes";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BE 段第 343 条 343-R1（BSM:569「后台另设「已存档内容」页，进去可以看到全部被存档的对象，按分类查阅、可逐条恢复。」；
// BSM:571「所有存档与删除的数据都按分类（票种、商品、商户、员工、设备、工单、预订等）分门别类归拢」）。

function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

type Post = { url: string; body: Record<string, unknown> };

// 列表与恢复的假响应可以给现成的值、晚一点才兑现的 Promise，或一个现成的失败响应（如 403）。
const respond = (value: unknown) => Promise.resolve(value).then((data) => (data instanceof Response ? data : ok(data)));

function forbidden(): Response {
  return new Response(JSON.stringify({ success: false, code: "FORBIDDEN", message: "权限不足" }), { status: 403, headers: { "content-type": "application/json" } });
}

function renderPage(permissions: EffectivePermissionsState, lists: Record<string, () => unknown>, onPost: () => unknown = () => ({ id: "restored" })) {
  const gets: string[] = [];
  const posts: Post[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return respond(onPost());
    }
    gets.push(url);
    const hit = Object.keys(lists).find((key) => url.includes(key));
    return respond(hit ? lists[hit]!() : { items: [] });
  }));
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "运营", accountType: "staff" },
    setActionResult,
    setGlobalError,
    effectivePermissions: permissions,
  };
  render(<ArchivedContentPage {...props} />);
  return { gets, posts, setActionResult, setGlobalError };
}

const optionLabels = () => Array.from(screen.getByTestId("archived-content-category").querySelectorAll("option")).map((option) => option.textContent);
const headers = () => Array.from(screen.getByTestId("archived-content-table").querySelectorAll("th")).map((th) => th.textContent);
const settle = () => act(async () => { await new Promise((resolve) => setTimeout(resolve, 50)); });
const cells = (testId: string) => Array.from(screen.getByTestId(testId).querySelectorAll("td")).map((td) => td.textContent ?? "");

describe("已存档内容页（第 343 条）", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("分类覆盖票种、商品、商户、员工、设备、工单、预订，连同游乐项目权益绑定与岗位角色模板；入口权限取各分类查看权限", () => {
    const labels = ARCHIVED_CATEGORIES.map((category) => category.label);
    for (const label of ["票种", "酒吧商品", "商户档案", "员工档案", "设施设备", "商户服务工单", "卡座预订", "游乐项目权益绑定", "岗位角色模板"]) expect(labels).toContain(label);
    expect(ARCHIVED_CONTENT_VIEW_PERMISSIONS).toContain("ticket.product.view");
    expect(ARCHIVED_CONTENT_VIEW_PERMISSIONS).toContain("staff.profile.view");
    // 修复项 F14：两类的入口码同原页面（权益绑定管理码、岗位角色模板的查看码）；游乐项目列表的查看码只为读项目用，不单独放人进本页。
    expect(ARCHIVED_CONTENT_VIEW_PERMISSIONS).toContain("amusement.project_entitlement.manage");
    expect(ARCHIVED_CONTENT_VIEW_PERMISSIONS).toContain("access.role.assign");
    expect(ARCHIVED_CONTENT_VIEW_PERMISSIONS).not.toContain("amusement.session.view");
    const route = adminRoutes.find((item) => item.pageId === "ADM-OPS-003");
    expect(route?.route).toBe("/admin/ops/archived-content");
    expect(route?.requiredPermissions).toEqual(ARCHIVED_CONTENT_VIEW_PERMISSIONS);
  });

  it("只列有查看权限的分类；默认分类只显示已存档的对象，带名称、北京时间的存档时间、存档人与存档理由", async () => {
    const { gets } = renderPage(perms(["ticket.product.view", "bar.product.view"]), {
      "/ticketing/ticket-types": () => ({ items: [
        { id: "tt-1", ticket_name: "夜场通票", archived_at: "2026-11-01T18:30:00Z", archived_by: "usr-ops-1", archive_reason: "季节票下线" },
        { id: "tt-2", ticket_name: "日场票", archived_at: null },
      ] }),
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-tt-1")).not.toBeNull(), { timeout: 3000 });
    expect(optionLabels()).toEqual(["票种", "酒吧商品"]);
    expect(gets.some((url) => url.includes("/api/v1/ticketing/ticket-types?includeArchived=true"))).toBe(true);
    expect(screen.queryByTestId("archived-content-row-tt-2")).toBeNull();
    expect(headers()).toEqual(["名称", "编号", "存档时间", "存档人", "存档理由", "操作"]);
    expect(cells("archived-content-row-tt-1").slice(0, -1)).toEqual(["夜场通票", "tt-1", "2026年11月2日 02:30", "usr-ops-1", "季节票下线"]);
  });

  // 第十八批 BY 段 387-R3（第十八批裁决 81「每个分类都显示存档理由；空间类型补存存档时间、存档人、理由」）：全类同列。
  it("每类都显示存档时间、存档人、存档理由（服务项目；空间类型补存的三项也显示）", async () => {
    renderPage(perms(["service.catalog.manage", "governance.space.view"]), {
      "/services/catalog/all": () => [
        { id: "svc-1", name: "代客泊车", status: "inactive", archived_at: "2026-11-02T01:00:00Z", archived_by: "usr-svc-1", archive_reason: "合作到期" },
        { id: "svc-2", name: "行李寄存", status: "active", archived_at: null, archived_by: null, archive_reason: null },
      ],
      "/platform/space-types": () => [{ id: "st-1", type_code: "kiosk", type_name: "花车", sort_order: 3, status: "archived", created_at: "2026-10-01T03:00:00Z", updated_at: "2026-11-02T03:00:00Z",
        archived_at: "2026-11-02T03:00:00Z", archived_by: "usr-ops-2", archive_reason: "不再使用的种类" }],
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-svc-1")).not.toBeNull(), { timeout: 3000 });
    expect(headers()).toEqual(["名称", "编号", "存档时间", "存档人", "存档理由", "操作"]);
    expect(cells("archived-content-row-svc-1").slice(0, -1)).toEqual(["代客泊车", "svc-1", "2026年11月2日 09:00", "usr-svc-1", "合作到期"]);
    expect(screen.queryByTestId("archived-content-row-svc-2")).toBeNull();

    fireEvent.change(screen.getByTestId("archived-content-category"), { target: { value: "space-types" } });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-st-1")).not.toBeNull(), { timeout: 3000 });
    expect(headers()).toEqual(["名称", "编号", "存档时间", "存档人", "存档理由", "操作"]);
    expect(cells("archived-content-row-st-1").slice(0, -1)).toEqual(["花车", "st-1", "2026年11月2日 11:00", "usr-ops-2", "不再使用的种类"]);
  });

  // 修复项 F12：票价规则、卡座预订的记录里没有名称字段，名称按原页面认得出的内容拼。
  it("票价规则名称按票种名、价格档、价格拼；票种对不上的显示票种编号", async () => {
    const { gets } = renderPage(perms(["ticket.price_variant.view"]), {
      "/ticketing/price-rules": () => ({ items: [
        { id: "price_rule_a1", ticket_type_id: "tt-1", variant_type: "student", price_mode: "fixed_price", weekday_price_cent: 8000, weekend_price_cent: 9000,
          holiday_price_cent: 9900, discount_rate: null, channel_id: null, currency: "CNY", status: "active", archived_at: "2026-11-01T18:30:00Z", archived_by: "usr-ops-1",
          delete_status: "none", adjustment_amount_cent: null },
        { id: "price_rule_b2", ticket_type_id: "tt-gone", variant_type: "member", price_mode: "amount_delta", weekday_price_cent: null, weekend_price_cent: null,
          holiday_price_cent: null, discount_rate: null, channel_id: null, currency: "CNY", status: "active", archived_at: "2026-11-02T18:30:00Z", archived_by: "usr-ops-1",
          delete_status: "none", adjustment_amount_cent: -500 },
      ] }),
      "/ticketing/ticket-types": () => ({ items: [{ id: "tt-1", ticket_name: "夜场通票", archived_at: null }] }),
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-price_rule_a1")).not.toBeNull(), { timeout: 3000 });
    expect(gets.some((url) => url.includes("/api/v1/ticketing/ticket-types?includeArchived=true"))).toBe(true);
    expect(cells("archived-content-row-price_rule_a1").slice(0, -1)).toEqual(["夜场通票 · 学生票 · 固定价 ¥80.00", "price_rule_a1", "2026年11月2日 02:30", "usr-ops-1", "—"]);
    expect(cells("archived-content-row-price_rule_b2")[0]).toBe("tt-gone · 会员价 · 按基础价加减金额 减 ¥5.00");
  });

  it("卡座预订名称按桌台、到店时间（北京时间）、人数拼", async () => {
    renderPage(perms(["bar.reservation.view"]), {
      "/bar/reservations": () => ({ items: [
        { id: "resv-1", bar_id: "bar-1", merchant_id: "m-1", table_id: "tbl-1", customer_id: "cus-1", reserved_at: "2026-11-05T12:30:00Z", guest_count: 6,
          deposit_amount_cent: 0, status: "cancelled", archived_at: "2026-11-06T02:00:00Z", archived_by: "usr-bar-1", delete_status: "none", remark: null },
        { id: "resv-2", bar_id: "bar-1", merchant_id: "m-1", table_id: "tbl-1", customer_id: "cus-2", reserved_at: "2026-11-07T14:00:00Z", guest_count: 2,
          deposit_amount_cent: 0, status: "booked", archived_at: null, archived_by: null, delete_status: "none", remark: null },
      ] }),
      "/bar/tables": () => ({ items: [{ id: "tbl-1", bar_id: "bar-1", table_code: "A-08", table_name: "卡座 A08", capacity: 8, status: "available" }] }),
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-resv-1")).not.toBeNull(), { timeout: 3000 });
    expect(cells("archived-content-row-resv-1").slice(0, -1)).toEqual(["A-08 · 2026年11月5日 20:30 · 6 人", "resv-1", "2026年11月6日 10:00", "usr-bar-1", "—"]);
    expect(screen.queryByTestId("archived-content-row-resv-2")).toBeNull();
  });

  it("没有桌台查看权限（桌台接口 403）时卡座预订照样列出，桌台显示桌台编号", async () => {
    renderPage(perms(["bar.reservation.view"]), {
      "/bar/reservations": () => ({ items: [
        { id: "resv-1", bar_id: "bar-1", merchant_id: "m-1", table_id: "tbl-1", customer_id: "cus-1", reserved_at: "2026-11-05T12:30:00Z", guest_count: 6,
          deposit_amount_cent: 0, status: "cancelled", archived_at: "2026-11-06T02:00:00Z", archived_by: "usr-bar-1", delete_status: "none", remark: null },
      ] }),
      "/bar/tables": () => forbidden(),
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-resv-1")).not.toBeNull(), { timeout: 3000 });
    expect(cells("archived-content-row-resv-1")[0]).toBe("tbl-1 · 2026年11月5日 20:30 · 6 人");
    expect(screen.queryByTestId("archived-content-error")).toBeNull();
  });

  // 修复项 F14（BSM:569「进去可以看到全部被存档的对象」）：补游乐项目权益绑定、岗位角色模板两类。
  it("游乐项目权益绑定：先取游乐项目，再逐项目取含已存档的绑定；恢复走该项目该权益的取消存档接口", async () => {
    let archived = true;
    const { gets, posts, setActionResult } = renderPage(perms(["amusement.project_entitlement.manage", "amusement.session.view"]), {
      "/amusement/projects/proj-1/entitlements": () => ({ items: [
        { id: "ent-1", project_id: "proj-1", entitlement_type: "entry", created_at: "2026-10-01T03:00:00Z", archived_at: archived ? "2026-11-04T04:00:00Z" : null,
          archived_by: archived ? "usr-amu-1" : null, archive_reason: archived ? "设备检修" : null, delete_status: "none" },
        { id: "ent-2", project_id: "proj-1", entitlement_type: "amusement", created_at: "2026-10-01T03:00:00Z", archived_at: null, archived_by: null, archive_reason: null, delete_status: "none" },
      ] }),
      "/amusement/projects/proj-2/entitlements": () => ({ items: [
        { id: "ent-3", project_id: "proj-2", entitlement_type: "amusement", created_at: "2026-10-01T03:00:00Z", archived_at: "2026-11-03T04:00:00Z", archived_by: "usr-amu-1",
          archive_reason: "配错", delete_status: "approved", delete_reason: "配错" },
      ] }),
      "/amusement/projects": () => ({ items: [{ id: "proj-1", project_name: "旋转木马" }, { id: "proj-2", project_name: "碰碰车" }] }),
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-ent-1")).not.toBeNull(), { timeout: 3000 });
    expect(optionLabels()).toEqual(["游乐项目权益绑定"]);
    expect(gets.some((url) => url.includes("/api/v1/amusement/projects/proj-1/entitlements?include_archived=true"))).toBe(true);
    expect(gets.some((url) => url.includes("/api/v1/amusement/projects/proj-2/entitlements?include_archived=true"))).toBe(true);
    expect(headers()).toEqual(["名称", "编号", "存档时间", "存档人", "存档理由", "操作"]);
    expect(cells("archived-content-row-ent-1").slice(0, -1)).toEqual(["旋转木马 · 入园", "ent-1", "2026年11月4日 12:00", "usr-amu-1", "设备检修"]);
    // 生效中的绑定、已审批删除的绑定都不是「被存档的对象」。
    expect(screen.queryByTestId("archived-content-row-ent-2")).toBeNull();
    expect(screen.queryByTestId("archived-content-row-ent-3")).toBeNull();

    archived = false;
    fireEvent.click(screen.getByTestId("archived-content-restore-ent-1"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 3000 });
    expect(posts[0]!.url).toContain("/api/v1/amusement/projects/proj-1/entitlements/entry/unarchive");
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 分类: "游乐项目权益绑定", 编号: "ent-1" })));
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-ent-1")).toBeNull());
  });

  it("岗位角色模板：只列已存档未删除的；恢复须填理由，走生命周期接口的恢复动作", async () => {
    // 只有权益绑定管理码、没有游乐项目查看码的，看不到权益绑定这一类（同原页面）。
    const { gets, posts, setGlobalError } = renderPage(perms(["access.role.assign", "amusement.project_entitlement.manage"]), {
      "/access/position-role-templates": () => ({ items: [
        { id: "prt-1", position_code: "ops_manager", staff_level_id: "lvl_m3_upper", role_id: "role-1", template_id: null, organization_id: null, status: "inactive",
          created_by: "usr-admin-1", created_at: "2026-10-01T03:00:00Z", updated_at: "2026-11-05T01:00:00Z", archived_at: "2026-11-05T01:00:00Z", deleted_at: null,
          archived_by: "usr-admin-1", archive_reason: "岗位撤并" },
        { id: "prt-2", position_code: "night_duty_manager", staff_level_id: "lvl_m2_lower", role_id: "role-2", status: "inactive",
          archived_at: "2026-11-01T01:00:00Z", deleted_at: "2026-11-02T01:00:00Z", delete_reason: "岗位撤销" },
        { id: "prt-3", position_code: "auditor_internal", staff_level_id: "lvl_m4_middle", role_id: "role-3", status: "active", archived_at: null, deleted_at: null },
      ] }),
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-prt-1")).not.toBeNull(), { timeout: 3000 });
    expect(optionLabels()).toEqual(["岗位角色模板"]);
    expect(gets.some((url) => url.includes("/api/v1/access/position-role-templates?include_archived=true"))).toBe(true);
    expect(headers()).toEqual(["名称", "编号", "存档时间", "存档人", "存档理由", "操作"]);
    expect(cells("archived-content-row-prt-1").slice(0, -1)).toEqual(["运营经理 / M3·上", "prt-1", "2026年11月5日 09:00", "usr-admin-1", "岗位撤并"]);
    expect(screen.queryByTestId("archived-content-row-prt-2")).toBeNull();
    expect(screen.queryByTestId("archived-content-row-prt-3")).toBeNull();

    const prompt = vi.spyOn(window, "prompt").mockReturnValueOnce(null).mockReturnValueOnce("   ").mockReturnValueOnce(" 岗位恢复启用 ");
    fireEvent.click(screen.getByTestId("archived-content-restore-prt-1"));
    expect(prompt).toHaveBeenCalledTimes(1);
    expect(setGlobalError).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("archived-content-restore-prt-1"));
    expect(setGlobalError).toHaveBeenCalledWith("恢复理由不能为空");
    expect(posts).toHaveLength(0);
    fireEvent.click(screen.getByTestId("archived-content-restore-prt-1"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 3000 });
    expect(posts[0]!.url).toContain("/api/v1/access/position-role-templates/prt-1/lifecycle");
    expect(posts[0]!.body).toMatchObject({ action: "restore", reason: "岗位恢复启用" });
  });

  // 修复项 F15：读取记请求序号，过期响应丢弃；恢复完成后按当前分类重读。
  it("切分类时旧请求晚到不盖住当前分类", async () => {
    let deliverTickets: (value: unknown) => void = () => undefined;
    const { gets } = renderPage(perms(["ticket.product.view", "bar.product.view"]), {
      "/ticketing/ticket-types": () => new Promise((resolve) => { deliverTickets = resolve; }),
      "/bar/products": () => [{ id: "prod-9", product_name: "威士忌", archived_at: "2026-11-03T02:00:00Z", archived_by: "usr-bar-1" }],
    });
    // 默认分类「票种」读得慢，还在读取时改选「酒吧商品」。
    await waitFor(() => expect(gets.some((url) => url.includes("/api/v1/ticketing/ticket-types"))).toBe(true), { timeout: 3000 });
    fireEvent.change(screen.getByTestId("archived-content-category"), { target: { value: "bar-products" } });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-prod-9")).not.toBeNull(), { timeout: 3000 });
    deliverTickets({ items: [{ id: "tt-1", ticket_name: "夜场通票", archived_at: "2026-11-01T18:30:00Z", archived_by: "usr-ops-1" }] });
    await settle();
    expect(screen.queryByTestId("archived-content-row-tt-1")).toBeNull();
    expect(screen.queryByTestId("archived-content-row-prod-9")).not.toBeNull();
  });

  it("恢复途中切了分类，恢复完成后重读的是当前分类", async () => {
    let finishRestore: (value: unknown) => void = () => undefined;
    const { gets, posts, setActionResult } = renderPage(perms(["bar.product.view", "bar.product.manage", "bar.category.view"]), {
      "/bar/products": () => [{ id: "prod-9", product_name: "威士忌", archived_at: "2026-11-03T02:00:00Z", archived_by: "usr-bar-1" }],
      "/bar/catalog/categories": () => [{ id: "cat-1", category_name: "洋酒", archived_at: "2026-11-03T03:00:00Z", archived_by: "usr-bar-1" }],
    }, () => new Promise((resolve) => { finishRestore = resolve; }));
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-prod-9")).not.toBeNull(), { timeout: 3000 });
    // 点「恢复」后、恢复请求还没回来就改选「酒吧商品分类」。
    fireEvent.click(screen.getByTestId("archived-content-restore-prod-9"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 3000 });
    fireEvent.change(screen.getByTestId("archived-content-category"), { target: { value: "bar-categories" } });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-cat-1")).not.toBeNull(), { timeout: 3000 });
    const categoryReads = () => gets.filter((url) => url.includes("/api/v1/bar/catalog/categories")).length;
    const readsBefore = categoryReads();
    finishRestore({ id: "prod-9" });
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 分类: "酒吧商品", 编号: "prod-9" })), { timeout: 3000 });
    await settle();
    expect(screen.queryByTestId("archived-content-row-prod-9")).toBeNull();
    await waitFor(() => expect(categoryReads()).toBe(readsBefore + 1), { timeout: 3000 });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-cat-1")).not.toBeNull(), { timeout: 3000 });
  });

  it("切到酒吧商品后逐条恢复：走该类对象已有的取消存档接口，恢复后重新读取", async () => {
    let archived = true;
    const { gets, posts, setActionResult } = renderPage(perms(["ticket.product.view", "bar.product.view", "bar.product.manage"]), {
      "/bar/products": () => (archived ? [{ id: "prod-9", product_name: "威士忌", archived_at: "2026-11-03T02:00:00Z" }] : []),
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-category")).not.toBeNull(), { timeout: 3000 });
    fireEvent.change(screen.getByTestId("archived-content-category"), { target: { value: "bar-products" } });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-prod-9")).not.toBeNull(), { timeout: 3000 });
    expect(gets.some((url) => url.includes("/api/v1/bar/products?includeArchived=true"))).toBe(true);

    archived = false;
    fireEvent.click(screen.getByTestId("archived-content-restore-prod-9"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 3000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/catalog/products/prod-9/unarchive");
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 分类: "酒吧商品", 编号: "prod-9" })));
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-prod-9")).toBeNull());
    expect(screen.getByTestId("archived-content-empty").textContent).toContain("酒吧商品");
  });

  it("只有查看权限、没有该类的管理权限时不能恢复", async () => {
    const { posts } = renderPage(perms(["bar.product.view"]), {
      "/bar/products": () => [{ id: "prod-9", product_name: "威士忌", archived_at: "2026-11-03T02:00:00Z" }],
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-prod-9")).not.toBeNull(), { timeout: 3000 });
    const button = screen.queryByTestId("archived-content-restore-prod-9") as HTMLButtonElement | null;
    expect(button === null || button.disabled).toBe(true);
    if (button) fireEvent.click(button);
    expect(posts).toHaveLength(0);
  });

  it("商户档案逐页取完；空间类型按状态判已存档", async () => {
    const page1 = Array.from({ length: 100 }, (_, i) => ({ id: `m-${i}`, merchant_name: `商户${i}`, archived_at: null }));
    const { gets } = renderPage(perms(["merchant.profile.view", "governance.space.view"]), {
      "page=1&size=100": () => ({ items: page1, total: 101, page: 1, size: 100 }),
      "page=2&size=100": () => ({ items: [{ id: "m-old", merchant_name: "已撤场商户", archived_at: "2026-10-01T03:00:00Z" }], total: 101, page: 2, size: 100 }),
      "/platform/space-types": () => [{ id: "st-1", type_code: "kiosk", type_name: "花车", status: "archived" }, { id: "st-2", type_code: "shop", type_name: "商铺", status: "active" }],
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-m-old")).not.toBeNull(), { timeout: 3000 });
    expect(gets.filter((url) => url.includes("/api/v1/platform/merchants?includeArchived=true")).length).toBe(2);
    expect(screen.queryByTestId("archived-content-row-m-0")).toBeNull();

    fireEvent.change(screen.getByTestId("archived-content-category"), { target: { value: "space-types" } });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-st-1")).not.toBeNull(), { timeout: 3000 });
    expect(cells("archived-content-row-st-1")).toContain("花车");
    expect(screen.queryByTestId("archived-content-row-st-2")).toBeNull();
  });

  it("没有任何分类的查看权限时给出说明，不发请求", async () => {
    const { gets } = renderPage(perms(["finance.ar_ap.view"]), {});
    await waitFor(() => expect(screen.queryByTestId("archived-content-no-category")).not.toBeNull());
    expect(gets).toHaveLength(0);
  });
});
