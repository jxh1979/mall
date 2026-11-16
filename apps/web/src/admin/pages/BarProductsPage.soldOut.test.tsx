// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BarProductsPage } from "./BarProductsPage";
import { AdminApi } from "../client/adminClient";
import { VALUE_LABELS } from "../i18n/fieldLabels";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 第十八批 AK 段第 221 条 221-R3（ENUM:111「沽清分两档：临时沽清（本营业日有效，营业日切换时点自动恢复在售）与长期下架（须主管审批解除后才恢复）；
// 沽清与解除同样须审批留痕」「库存归零不自动沽清，由现场按实际发起。」；BSM:1069「沽清（卖完、临时下架）均需主管审批留痕」）：
// 后台商品页的沽清、解除沽清入口——弹框选档位、填原因，经客户端按蛇形键提交；首次提交被拦下送主管复核时给出提示；状态按档位显示。

function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}
const SOLD_OUT_PERMS = perms(["bar.product.view", "bar.product.sold_out"]);

const PRODUCTS = [
  { id: "prod-1", bar_id: "bar-main", product_name: "威士忌", product_type: "spirit", price_cent: 8800, status: "active", sold_out: false },
  { id: "prod-2", bar_id: "bar-main", product_name: "生啤", product_type: "beer", price_cent: 3000, status: "active", sold_out: true, sold_out_type: "temporary", sold_out_until: "2026-11-13T06:00:00Z" },
  { id: "prod-3", bar_id: "bar-main", product_name: "香槟", product_type: "wine", price_cent: 68800, status: "active", sold_out: true, sold_out_type: "long_term", sold_out_until: null },
];

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}
function ok(data: unknown): Response {
  return json(200, { success: true, code: "OK", message: "success", data });
}

type Post = { url: string; body: Record<string, unknown> };

function renderPage(options: { permissions?: EffectivePermissionsState; postResponse?: () => Response } = {}) {
  const posts: Post[] = [];
  const gets: string[] = [];
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown> });
      return Promise.resolve(options.postResponse ? options.postResponse() : ok({ id: "prod-1", sold_out: true }));
    }
    gets.push(url);
    if (url.includes("/api/v1/bar/products")) return Promise.resolve(ok({ items: PRODUCTS }));
    return Promise.resolve(ok({ items: [] }));
  }));
  const setActionResult = vi.fn();
  const setGlobalError = vi.fn();
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "吧台主管", accountType: "staff" },
    setActionResult,
    setGlobalError,
    effectivePermissions: options.permissions ?? SOLD_OUT_PERMS,
  };
  render(<BarProductsPage {...props} />);
  return { posts, gets, setActionResult, setGlobalError };
}

const productListLoads = (gets: string[]) => gets.filter((url) => url.includes("/api/v1/bar/products")).length;

describe("BarProductsPage 第 221 条沽清与解除沽清", () => {
  afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

  it("在售商品点「沽清」：不选档位不发请求；选临时沽清并填原因后按蛇形键发 POST …/sold-out", async () => {
    const { posts, gets, setActionResult } = renderPage();
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-prod-1")).not.toBeNull(), { timeout: 3000 });
    expect(screen.queryByTestId("bar-product-sold-out-release-prod-1")).toBeNull();

    fireEvent.click(screen.getByTestId("bar-product-sold-out-prod-1"));
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-modal")).not.toBeNull());
    const options = Array.from(screen.getByTestId("bar-product-sold-out-type").querySelectorAll("option")).map((option) => option.textContent ?? "");
    expect(options.some((text) => text.includes("临时沽清") && text.includes("本营业日有效"))).toBe(true);
    expect(options.some((text) => text.includes("长期下架") && text.includes("主管审批解除"))).toBe(true);

    fireEvent.click(screen.getByTestId("bar-product-sold-out-submit"));
    expect(screen.getByTestId("bar-product-sold-out-modal-error").textContent).toContain("档位");
    expect(posts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("bar-product-sold-out-type"), { target: { value: "temporary" } });
    fireEvent.change(screen.getByTestId("bar-product-sold-out-reason"), { target: { value: " 今日已售完 " } });
    const before = productListLoads(gets);
    fireEvent.click(screen.getByTestId("bar-product-sold-out-submit"));

    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/products/prod-1/sold-out");
    expect(posts[0]!.body).toMatchObject({ sold_out: true, sold_out_type: "temporary", reason: "今日已售完" });
    expect(posts[0]!.body.idempotency_key).toBeTruthy();
    expect(Object.keys(posts[0]!.body).some((key) => /[A-Z]/.test(key))).toBe(false);
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_product_sold_out", productId: "prod-1" })));
    await waitFor(() => expect(productListLoads(gets)).toBe(before + 1));
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-modal")).toBeNull());
    expect(VALUE_LABELS.bar_product_sold_out).toBe("商品已临时沽清");
    expect(VALUE_LABELS.bar_product_delisted).toBe("商品已长期下架");
    expect(VALUE_LABELS.bar_product_sold_out_released).toBe("商品已解除沽清");
  });

  it("首次提交被拦下送主管复核（ADMISSION_BLOCKED）时，提示已提交主管审批、批准后再点一次", async () => {
    const { posts, setActionResult, setGlobalError } = renderPage({
      postResponse: () => json(409, { success: false, code: "STATE_CONFLICT", detail_code: "ADMISSION_BLOCKED", message: "操作已被拦截，等待主管复核，复核单号：review_1" }),
    });
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-prod-1")).not.toBeNull(), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("bar-product-sold-out-prod-1"));
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-modal")).not.toBeNull());
    fireEvent.change(screen.getByTestId("bar-product-sold-out-type"), { target: { value: "long_term" } });
    fireEvent.change(screen.getByTestId("bar-product-sold-out-reason"), { target: { value: "供应商停产" } });
    fireEvent.click(screen.getByTestId("bar-product-sold-out-submit"));

    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-note")?.textContent ?? "").toContain("已提交主管审批"), { timeout: 5000 });
    const note = screen.getByTestId("bar-product-sold-out-note").textContent ?? "";
    expect(note).toContain("主管复核");
    expect(note).toContain("再点一次");
    expect(note).toContain("威士忌");
    expect(note).toContain("长期下架");
    expect(posts).toHaveLength(1);
    expect(posts[0]!.body).toMatchObject({ sold_out: true, sold_out_type: "long_term", reason: "供应商停产" });
    expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("review_1"));
    expect(setActionResult).not.toHaveBeenCalled();
  });

  it("被拦下后再点同一商品的「沽清」，弹框原样带回上次的档位与原因（主管的批准只认同样内容的再次提交）", async () => {
    const { posts } = renderPage({
      postResponse: () => json(409, { success: false, code: "STATE_CONFLICT", detail_code: "ADMISSION_BLOCKED", message: "操作已被拦截，等待主管复核，复核单号：review_1" }),
    });
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-prod-1")).not.toBeNull(), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("bar-product-sold-out-prod-1"));
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-modal")).not.toBeNull());
    fireEvent.change(screen.getByTestId("bar-product-sold-out-type"), { target: { value: "long_term" } });
    fireEvent.change(screen.getByTestId("bar-product-sold-out-reason"), { target: { value: "供应商停产" } });
    fireEvent.click(screen.getByTestId("bar-product-sold-out-submit"));
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-note")?.textContent ?? "").toContain("原因保持不变"), { timeout: 5000 });
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-modal")).toBeNull());

    fireEvent.click(screen.getByTestId("bar-product-sold-out-prod-1"));
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-modal")).not.toBeNull());
    expect((screen.getByTestId("bar-product-sold-out-type") as HTMLSelectElement).value).toBe("long_term");
    expect((screen.getByTestId("bar-product-sold-out-reason") as HTMLInputElement).value).toBe("供应商停产");
    fireEvent.click(screen.getByTestId("bar-product-sold-out-submit"));
    await waitFor(() => expect(posts).toHaveLength(2), { timeout: 5000 });
    const { idempotency_key: firstKey, ...first } = posts[0]!.body;
    const { idempotency_key: secondKey, ...second } = posts[1]!.body;
    expect(second).toEqual(first);
    expect(firstKey).toBeTruthy();
    expect(secondKey).toBeTruthy();
  });

  it("已沽清行按档位显示临时沽清、长期下架，并给「解除沽清」；点击发 sold_out:false", async () => {
    const { posts, setActionResult } = renderPage();
    await waitFor(() => expect(screen.queryByTestId("bar-product-status-prod-2")?.textContent).toBe("临时沽清"), { timeout: 3000 });
    expect(screen.getByTestId("bar-product-status-prod-3").textContent).toBe("长期下架");
    expect(screen.getByTestId("bar-product-status-prod-1").textContent).toBe("有效");
    expect(screen.getByTestId("bar-products-table").textContent ?? "").not.toMatch(/temporary|long_term/);
    expect(screen.getByTestId("bar-product-sold-out-release-prod-2").textContent).toBe("解除沽清");
    expect(screen.getByTestId("bar-product-sold-out-release-prod-3").textContent).toBe("解除沽清");
    expect(screen.queryByTestId("bar-product-sold-out-prod-2")).toBeNull();
    expect(screen.queryByTestId("bar-product-sold-out-prod-3")).toBeNull();

    fireEvent.click(screen.getByTestId("bar-product-row-prod-3"));
    expect(screen.getByTestId("row-detail-drawer").textContent).toContain("长期下架");
    fireEvent.keyDown(window, { key: "Escape" });

    fireEvent.click(screen.getByTestId("bar-product-sold-out-release-prod-2"));
    await waitFor(() => expect(posts).toHaveLength(1), { timeout: 5000 });
    expect(posts[0]!.url).toContain("/api/v1/bar/products/prod-2/sold-out");
    expect(posts[0]!.body.sold_out).toBe(false);
    expect(posts[0]!.body.sold_out_type).toBeUndefined();
    expect(posts[0]!.body.idempotency_key).toBeTruthy();
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "bar_product_sold_out_released", productId: "prod-2" })));
  });

  it("解除沽清首次提交被拦下送主管复核时同样提示", async () => {
    renderPage({
      postResponse: () => json(409, { success: false, code: "STATE_CONFLICT", detail_code: "ADMISSION_BLOCKED", message: "操作已被拦截，等待主管复核，复核单号：review_2" }),
    });
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-release-prod-3")).not.toBeNull(), { timeout: 3000 });
    fireEvent.click(screen.getByTestId("bar-product-sold-out-release-prod-3"));
    await waitFor(() => expect(screen.queryByTestId("bar-product-sold-out-note")?.textContent ?? "").toContain("已提交主管审批"), { timeout: 5000 });
    const note = screen.getByTestId("bar-product-sold-out-note").textContent ?? "";
    expect(note).toContain("香槟");
    expect(note).toContain("解除沽清");
  });

  it("没有 bar.product.sold_out 权限时不出沽清、解除沽清", async () => {
    renderPage({ permissions: perms(["bar.product.view"]) });
    await waitFor(() => expect(screen.queryByTestId("bar-product-status-prod-2")?.textContent).toBe("临时沽清"), { timeout: 3000 });
    expect(screen.queryByTestId("bar-product-sold-out-prod-1")).toBeNull();
    expect(screen.queryByTestId("bar-product-sold-out-release-prod-2")).toBeNull();
  });
});
