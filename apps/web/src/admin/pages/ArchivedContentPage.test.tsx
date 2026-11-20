// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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

function renderPage(permissions: EffectivePermissionsState, lists: Record<string, () => unknown>) {
  const gets: string[] = [];
  const posts: Post[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(ok({ id: "restored" }));
    }
    gets.push(url);
    const hit = Object.keys(lists).find((key) => url.includes(key));
    return Promise.resolve(ok(hit ? lists[hit]!() : { items: [] }));
  }));
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "运营", accountType: "staff" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: permissions,
  };
  render(<ArchivedContentPage {...props} />);
  return { gets, posts, setActionResult };
}

const optionLabels = () => Array.from(screen.getByTestId("archived-content-category").querySelectorAll("option")).map((option) => option.textContent);
const cells = (testId: string) => Array.from(screen.getByTestId(testId).querySelectorAll("td")).map((td) => td.textContent ?? "");

describe("已存档内容页（第 343 条）", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("分类覆盖票种、商品、商户、员工、设备、工单、预订，入口权限取各分类查看权限", () => {
    const labels = ARCHIVED_CATEGORIES.map((category) => category.label);
    for (const label of ["票种", "酒吧商品", "商户档案", "员工档案", "设施设备", "商户服务工单", "卡座预订"]) expect(labels).toContain(label);
    expect(ARCHIVED_CONTENT_VIEW_PERMISSIONS).toContain("ticket.product.view");
    expect(ARCHIVED_CONTENT_VIEW_PERMISSIONS).toContain("staff.profile.view");
    const route = adminRoutes.find((item) => item.pageId === "ADM-OPS-003");
    expect(route?.route).toBe("/admin/ops/archived-content");
    expect(route?.requiredPermissions).toEqual(ARCHIVED_CONTENT_VIEW_PERMISSIONS);
  });

  it("只列有查看权限的分类；默认分类只显示已存档的对象，带名称、北京时间的存档时间、存档人与理由", async () => {
    const { gets } = renderPage(perms(["ticket.product.view", "bar.product.view"]), {
      "/ticketing/ticket-types": () => ({ items: [
        { id: "tt-1", ticket_name: "夜场通票", archived_at: "2026-11-01T18:30:00Z", archived_by: "usr-ops-1", archive_reason: "活动结束" },
        { id: "tt-2", ticket_name: "日场票", archived_at: null },
      ] }),
    });
    await waitFor(() => expect(screen.queryByTestId("archived-content-row-tt-1")).not.toBeNull(), { timeout: 3000 });
    expect(optionLabels()).toEqual(["票种", "酒吧商品"]);
    expect(gets.some((url) => url.includes("/api/v1/ticketing/ticket-types?includeArchived=true"))).toBe(true);
    expect(screen.queryByTestId("archived-content-row-tt-2")).toBeNull();
    const row = cells("archived-content-row-tt-1");
    expect(row).toContain("夜场通票");
    expect(row).toContain("tt-1");
    expect(row).toContain("2026年11月2日 02:30");
    expect(row).toContain("usr-ops-1");
    expect(row).toContain("活动结束");
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
