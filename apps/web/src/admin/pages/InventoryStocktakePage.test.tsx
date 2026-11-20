// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { InventoryStocktakePage } from "./InventoryStocktakePage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 BF 段第 351 条 351-R1（BSM:1164「记录入库、出库、调拨、盘点、报损、报溢和库存调整。」；ENUM:164「报损/盘点差异/存酒作废须照片+视频
// 双证据+主管复核」）：后台「库存盘点」页——建盘点单、登记差异、差异复核、差异过账都按蛇形键走已有接口；账面数量不由页面填；
// 复核、过账被准入闸拦下时提示并保留所填内容。

function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}
const ALL = perms(["inventory.stocktake.view", "inventory.stocktake.create", "inventory.stocktake.variance_record",
  "inventory.stocktake.variance_review", "finance.inventory_variance.post"]);

const STOCKTAKES = [
  { id: "st-1", stocktake_no: "ST001", warehouse_id: "wh-1", business_date: "2026-11-19", status: "counting", remark: "月末盘点", created_at: "2026-11-19T18:30:00Z" },
  { id: "st-2", stocktake_no: "ST002", warehouse_id: "wh-2", business_date: "2026-11-18", status: "closed", remark: null, created_at: "2026-11-18T02:00:00Z" },
];
const VARIANCES = [
  { id: "v-pending", stocktake_id: "st-1", item_id: "item-1", book_qty: 10, counted_qty: 8, variance_qty: -2, variance_amount_cent: -60000, reason: "shortage", review_status: "pending", recorded_by: "u-keeper", photo_attachment_ids: ["att-p"], video_attachment_ids: ["att-v"] },
  { id: "v-supplier", stocktake_id: "st-1", item_id: "item-2", book_qty: 5, counted_qty: 4, variance_qty: -1, variance_amount_cent: -8800, reason: "supplier_fault", supplier_id: "sup-9", review_status: "approved", reviewed_by: "u-boss", reviewed_at: "2026-11-19T19:00:00Z" },
  { id: "v-damage", stocktake_id: "st-1", item_id: "item-1", book_qty: 3, counted_qty: 2, variance_qty: -1, variance_amount_cent: -30000, reason: "damage", review_status: "approved", reviewed_by: "u-boss", reviewed_at: "2026-11-19T19:05:00Z" },
  { id: "v-posted", stocktake_id: "st-1", item_id: "item-2", book_qty: 1, counted_qty: 2, variance_qty: 1, variance_amount_cent: 8800, reason: "overage", review_status: "approved", posted_ar_ap_item_id: "arap-1" },
];

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}
function ok(data: unknown): Response {
  return json(200, { success: true, code: "OK", message: "success", data });
}
const BLOCKED = () => json(409, { success: false, code: "STATE_CONFLICT", detail_code: "ADMISSION_BLOCKED", message: "操作已被拦截，等待主管复核，复核单号：review_7" });

type Post = { url: string; body: Record<string, unknown> };

function renderPage(options: { permissions?: EffectivePermissionsState; postResponse?: (url: string) => Response } = {}) {
  const posts: Post[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(options.postResponse ? options.postResponse(url) : ok({ id: "new-1" }));
    }
    if (url.includes("/api/v1/inventory/stocktakes/st-1")) return Promise.resolve(ok({ stocktake: STOCKTAKES[0], variances: VARIANCES }));
    if (url.includes("/api/v1/inventory/stocktakes")) return Promise.resolve(ok({ items: STOCKTAKES }));
    if (url.includes("/api/v1/inventory/warehouses")) return Promise.resolve(ok({ items: [{ id: "wh-1", name: "一号吧台仓" }, { id: "wh-2", name: "总仓" }] }));
    if (url.includes("/api/v1/inventory/items")) return Promise.resolve(ok({ items: [{ id: "item-1", item_name: "麦卡伦 18 年", unit: "瓶" }, { id: "item-2", item_name: "青岛纯生", unit: "箱" }] }));
    return Promise.resolve(ok({ items: [] }));
  }));
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "库管", accountType: "staff" },
    setActionResult,
    setGlobalError,
    effectivePermissions: options.permissions ?? ALL,
  };
  render(<InventoryStocktakePage {...props} />);
  return { posts, setActionResult, setGlobalError };
}

async function openFirstStocktake() {
  await waitFor(() => expect(screen.queryByTestId("stocktake-select-st-1")).not.toBeNull(), { timeout: 3000 });
  fireEvent.click(screen.getByTestId("stocktake-select-st-1"));
  await waitFor(() => expect(screen.queryByTestId("stocktake-variance-table")).not.toBeNull(), { timeout: 3000 });
}

describe("InventoryStocktakePage 第 351 条库存盘点页", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("列出盘点单（状态中文、库房显示名称、时间按北京时间）；不选库房不建单，选库房后按蛇形键建盘点单", async () => {
    const { posts, setActionResult } = renderPage();
    await waitFor(() => expect(screen.queryByTestId("stocktake-row-st-1")).not.toBeNull(), { timeout: 3000 });
    const first = screen.getByTestId("stocktake-row-st-1").textContent ?? "";
    expect(first).toContain("ST001");
    expect(first).toContain("盘点中");
    expect(first).toContain("一号吧台仓");
    expect(first).toContain("2026年11月19日");
    expect(first).toContain("2026年11月20日 02:30");
    expect(screen.getByTestId("stocktake-row-st-2").textContent ?? "").toContain("已关闭");

    fireEvent.click(screen.getByTestId("stocktake-create-submit"));
    expect(screen.getByTestId("stocktake-error").textContent).toContain("库房");
    expect(posts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("stocktake-create-warehouse"), { target: { value: "wh-2" } });
    fireEvent.change(screen.getByTestId("stocktake-create-remark"), { target: { value: " 季度盘点 " } });
    fireEvent.click(screen.getByTestId("stocktake-create-submit"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toMatch(/\/api\/v1\/inventory\/stocktakes$/);
    expect(posts[0]!.body).toMatchObject({ warehouse_id: "wh-2", remark: "季度盘点" });
    expect(posts[0]!.body.idempotency_key).toBeTruthy();
    expect(Object.keys(posts[0]!.body).some((key) => /[A-Z]/.test(key))).toBe(false);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 结果: "盘点单已建立", 库房: "总仓" })));
  });

  it("登记差异：必填项不全不提交；按蛇形键发物料、实盘数量、原因、责任供应商与单位成本（分），不发账面数量", async () => {
    const { posts, setActionResult } = renderPage();
    await openFirstStocktake();

    fireEvent.click(screen.getByTestId("stocktake-variance-submit"));
    expect(screen.getByTestId("stocktake-error").textContent).toContain("物料");
    expect(posts).toHaveLength(0);

    expect(screen.queryByTestId("stocktake-variance-supplier")).toBeNull();
    fireEvent.change(screen.getByTestId("stocktake-variance-item"), { target: { value: "item-2" } });
    fireEvent.change(screen.getByTestId("stocktake-variance-counted"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("stocktake-variance-reason"), { target: { value: "supplier_fault" } });
    fireEvent.change(screen.getByTestId("stocktake-variance-supplier"), { target: { value: "sup-9" } });
    fireEvent.change(screen.getByTestId("stocktake-variance-unit-cost"), { target: { value: "12.5" } });
    fireEvent.click(screen.getByTestId("stocktake-variance-submit"));

    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/inventory/stocktakes/st-1/variances");
    expect(posts[0]!.body).toMatchObject({ item_id: "item-2", counted_qty: 3, reason: "supplier_fault", supplier_id: "sup-9", unit_cost_cents: 1250 });
    expect(posts[0]!.body).not.toHaveProperty("book_qty");
    expect(Object.keys(posts[0]!.body).some((key) => /[A-Z]/.test(key))).toBe(false);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 结果: "盘点差异已登记", 盘点单号: "ST001", 物料: "青岛纯生" })));
  });

  it("差异表显示中文原因、状态与证据链接；待复核的可通过或驳回；被准入闸拦下时提示并保留复核意见", async () => {
    const { posts, setActionResult, setGlobalError } = renderPage({ postResponse: BLOCKED });
    await openFirstStocktake();
    const pending = screen.getByTestId("stocktake-variance-row-v-pending");
    expect(pending.textContent).toContain("麦卡伦 18 年");
    expect(pending.textContent).toContain("盘亏");
    expect(pending.textContent).toContain("待复核");
    expect(pending.textContent).toContain("-600.00");
    expect(pending.querySelectorAll("a")).toHaveLength(2);
    expect(screen.getByTestId("stocktake-variance-row-v-supplier").textContent).toContain("2026年11月20日 03:00");
    expect(screen.queryByTestId("stocktake-variance-approve-v-supplier")).toBeNull();

    fireEvent.change(screen.getByTestId("stocktake-review-reason"), { target: { value: "已核对监控" } });
    fireEvent.click(screen.getByTestId("stocktake-variance-approve-v-pending"));
    await waitFor(() => expect(screen.queryByTestId("stocktake-note")?.textContent ?? "").toContain("已提交主管审批"), { timeout: 5000 });
    expect(posts).toHaveLength(1);
    expect(posts[0]!.url).toContain("/api/v1/inventory/stocktakes/st-1/variances/v-pending/review");
    expect(posts[0]!.body).toMatchObject({ decision: "approved", reason: "已核对监控" });
    expect((screen.getByTestId("stocktake-review-reason") as HTMLInputElement).value).toBe("已核对监控");
    expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("review_7"));
    expect(setActionResult).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("stocktake-variance-reject-v-pending"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(posts[1]!.body).toMatchObject({ decision: "rejected", reason: "已核对监控" });
  });

  it("过账：复核通过、未过账且金额不为 0 的差异才有「过账」；供应商责任默认取责任供应商，其余须填往来单位", async () => {
    const { posts, setActionResult } = renderPage();
    await openFirstStocktake();
    expect(screen.queryByTestId("stocktake-variance-post-v-pending")).toBeNull();
    expect(screen.queryByTestId("stocktake-variance-post-v-posted")).toBeNull();
    expect(screen.getByTestId("stocktake-variance-row-v-posted").textContent).toContain("已过账");

    fireEvent.click(screen.getByTestId("stocktake-variance-post-v-damage"));
    expect(screen.getByTestId("stocktake-error").textContent).toContain("往来单位");
    expect(posts).toHaveLength(0);

    fireEvent.click(screen.getByTestId("stocktake-variance-post-v-supplier"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/finance/inventory-variances/post");
    expect(posts[0]!.body).toMatchObject({ inventory_variance_id: "v-supplier", counterparty_id: "sup-9" });
    expect(posts[0]!.body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ 结果: "盘点差异已过账", 往来单位: "sup-9" })));

    fireEvent.change(screen.getByTestId("stocktake-post-counterparty"), { target: { value: "cp-loss" } });
    fireEvent.change(screen.getByTestId("stocktake-post-note"), { target: { value: "酒瓶破损" } });
    fireEvent.click(screen.getByTestId("stocktake-variance-post-v-damage"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    expect(posts[1]!.body).toMatchObject({ inventory_variance_id: "v-damage", counterparty_id: "cp-loss", operator_note: "酒瓶破损" });
  });

  it("只有查看权限时，建单、登记差异、复核、过账按钮都不可用；已关闭的盘点单不给登记差异", async () => {
    renderPage({ permissions: perms(["inventory.stocktake.view"]) });
    await openFirstStocktake();
    for (const id of ["stocktake-create-submit", "stocktake-variance-submit", "stocktake-variance-approve-v-pending",
      "stocktake-variance-reject-v-pending", "stocktake-variance-post-v-supplier"]) {
      expect((screen.getByTestId(id) as HTMLButtonElement).disabled, id).toBe(true);
    }
    fireEvent.click(screen.getByTestId("stocktake-select-st-2"));
    await waitFor(() => expect(screen.queryByTestId("stocktake-closed-hint")).not.toBeNull(), { timeout: 3000 });
    expect(screen.queryByTestId("stocktake-variance-submit")).toBeNull();
  });
});
