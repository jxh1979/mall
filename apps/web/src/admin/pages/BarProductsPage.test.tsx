// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarProductsPage } from "./BarProductsPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 K 段 97-R2（BSM:1065「按杯按标准出酒量扣库存、超出记损耗」；BSM:1049「连接供应链库存，扣减吧台售卖消耗」）：
// 新建商品带「库存与出酒量」：勾选扣库后选库存商品规格与库位（库位取该规格已入库的余额行），按杯的填标准出酒量；经客户端按蛇形键提交。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function ok(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), {
    status: 200,
    headers: { "content-type": "application/json" },
  });
}

let posts: { url: string; body: Record<string, unknown> }[] = [];

function makeApi(): AdminApi {
  posts = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(ok({ id: "product-new" }));
    }
    if (url.includes("/api/v1/platform/merchants")) return Promise.resolve(ok({ items: [{ id: "bar-main", merchant_name: "主酒吧", merchant_type: "bar" }] }));
    if (url.includes("/api/v1/inventory/items")) return Promise.resolve(ok({ items: [{ id: "item-whisky", item_name: "某某威士忌 700 毫升瓶装", unit: "瓶", spec_volume_ml: 700 }] }));
    if (url.includes("/api/v1/inventory/stock-balances")) return Promise.resolve(ok({ items: [{ id: "bal-1", item_id: "item-whisky", location_id: "counter-1", quantity: 10 }] }));
    return Promise.resolve(ok({ items: [] }));
  }));
  return new AdminApi();
}

function renderPage(): void {
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<BarProductsPage {...props} />);
}

async function fillBasics(): Promise<void> {
  await waitFor(() => expect(screen.getByTestId("bar-product-create-open")).toBeTruthy());
  fireEvent.click(screen.getByTestId("bar-product-create-open"));
  await waitFor(() => expect(screen.getByTestId("bar-product-bar-id").querySelectorAll("option").length).toBeGreaterThan(1));
  fireEvent.change(screen.getByTestId("bar-product-bar-id"), { target: { value: "bar-main" } });
  fireEvent.change(screen.getByTestId("bar-product-name"), { target: { value: "某某威士忌（按杯）" } });
  fireEvent.change(screen.getByTestId("bar-product-price"), { target: { value: "58" } });
}

describe("BarProductsPage 新建商品挂库存与标准出酒量（第十八批 K 段 97-R2）", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("勾选扣库、选库存商品规格与库位、填标准出酒量 25：POST /api/v1/bar/products 带四个蛇形键", async () => {
    renderPage();
    await fillBasics();
    await waitFor(() => expect(screen.queryByTestId("bar-product-stock-deduction")).not.toBeNull());
    fireEvent.click(screen.getByTestId("bar-product-stock-deduction"));
    await waitFor(() => expect(screen.getByTestId("bar-product-inventory-item").textContent).toContain("某某威士忌 700 毫升瓶装（700 毫升）"));
    fireEvent.change(screen.getByTestId("bar-product-inventory-item"), { target: { value: "item-whisky" } });
    await waitFor(() => expect(screen.getByTestId("bar-product-stock-location").textContent).toContain("counter-1"));
    fireEvent.change(screen.getByTestId("bar-product-stock-location"), { target: { value: "counter-1" } });
    fireEvent.change(screen.getByTestId("bar-product-standard-pour"), { target: { value: "25" } });
    fireEvent.click(screen.getByTestId("bar-product-guided-next"));
    expect(screen.getByTestId("bar-product-guided-summary").textContent).toContain("某某威士忌 700 毫升瓶装");
    fireEvent.click(screen.getByTestId("bar-product-submit"));

    await waitFor(() => expect(posts.some((p) => p.url.endsWith("/api/v1/bar/products"))).toBe(true));
    expect(posts.find((p) => p.url.endsWith("/api/v1/bar/products"))?.body).toMatchObject({
      bar_id: "bar-main",
      stock_deduction_required: true,
      inventory_item_id: "item-whisky",
      stock_location_id: "counter-1",
      standard_pour_volume: 25,
      catalog_item_code: null,
    });
  });

  it("勾选扣库但未选库位：当步拦下、不提交", async () => {
    renderPage();
    await fillBasics();
    await waitFor(() => expect(screen.queryByTestId("bar-product-stock-deduction")).not.toBeNull());
    fireEvent.click(screen.getByTestId("bar-product-stock-deduction"));
    await waitFor(() => expect(screen.getByTestId("bar-product-inventory-item").textContent).toContain("某某威士忌 700 毫升瓶装"));
    fireEvent.change(screen.getByTestId("bar-product-inventory-item"), { target: { value: "item-whisky" } });
    fireEvent.click(screen.getByTestId("bar-product-guided-next"));
    await waitFor(() => expect(screen.getByTestId("bar-product-guided-error").textContent).toContain("库位"));
    expect(screen.queryByTestId("bar-product-submit")).toBeNull();
    expect(posts.some((p) => p.url.endsWith("/api/v1/bar/products"))).toBe(false);
  });
});
