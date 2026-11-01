// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VoucherLedgerPage } from "./VoucherLedgerPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

// 第十八批 L 段 103-R1（BSM:1197「酒水/礼品/高价值物料贴平台唯一二维码，出库须同时核验订单和物品码」；ENUM:81「到店由店员核销领取时才实扣」）：
// 店内实物券核销逐件填物品码，去重后个数须等于每张券件数才放行；商户折扣券核销不填物品码。
const DEFINITIONS = [
  { id: "vdef_g2", name: "礼品两件券", voucher_kind: "in_store_goods", redeem_method: "in_store_staff", settlement_mode: "none", merchant_id: "merchant-1", inventory_item_id: "item_1", stock_location_id: "loc_1", stock_quantity: 2, valid_days: 30, status: "active" },
  { id: "vdef_c", name: "餐饮满减券", voucher_kind: "merchant_discount", redeem_method: "in_store_staff", settlement_mode: "none", merchant_id: "merchant-c", merchant_type: "catering", face_value_cent: 3000, valid_days: 30, status: "active" },
];
const INSTANCES = [
  { id: "vi_g2", definition_id: "vdef_g2", customer_id: "customer-1", code: "VCG2", source_type: "points_redemption", source_ref: "redemption_1", status: "issued", issued_at: "2026-10-30T00:00:00Z" },
  { id: "vi_c", definition_id: "vdef_c", customer_id: "customer-1", code: "VCCC", source_type: "marketing_grant", source_ref: "vgrant_1", status: "issued", issued_at: "2026-10-30T00:00:00Z" },
];

function envelope(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }), { status: 200, headers: { "content-type": "application/json" } });
}

let posts: Array<{ url: string; body: Record<string, unknown> }> = [];
function renderPage() {
  posts = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push({ url, body: JSON.parse(String(init?.body)) as Record<string, unknown> });
        return Promise.resolve(envelope({ id: "vrr_1", voucher_instance_id: "vi_g2", settlement_mode: "none" }));
      }
      if (url.includes("/vouchers/definitions")) return Promise.resolve(envelope({ items: DEFINITIONS, page: 1, page_size: 200, total: DEFINITIONS.length }));
      if (url.includes("/vouchers/instances")) return Promise.resolve(envelope({ items: INSTANCES, page: 1, page_size: 20, total: INSTANCES.length }));
      return Promise.resolve(envelope([]));
    }),
  );
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } },
  };
  render(<VoucherLedgerPage {...props} />);
}

describe("VoucherLedgerPage 店内实物券核销逐件核验物品码（第十八批 L 段 103-R1）", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("每张 2 件的店内实物券只填 1 个物品码被拦、不发请求；填 2 个后请求体 trace_codes 为两项", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-open-vi_g2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-redeem-open-vi_g2"));
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-trace-codes")).toBeTruthy());

    fireEvent.change(screen.getByTestId("voucher-redeem-trace-codes"), { target: { value: "TRC-1 TRC-1" } });
    fireEvent.click(screen.getByTestId("voucher-redeem-guided-next"));
    expect(screen.getByTestId("voucher-redeem-guided-error").textContent).toContain("须逐件扫 2 个物品码");
    expect(screen.queryByTestId("voucher-redeem-save")).toBeNull();
    expect(posts).toHaveLength(0);

    fireEvent.change(screen.getByTestId("voucher-redeem-trace-codes"), { target: { value: "TRC-1，TRC-2" } });
    fireEvent.click(screen.getByTestId("voucher-redeem-guided-next"));
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-guided-summary").textContent).toContain("TRC-2"));
    fireEvent.click(screen.getByTestId("voucher-redeem-save"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/vouchers/redemptions");
    expect(posts[0]!.body.trace_codes).toEqual(["TRC-1", "TRC-2"]);
  });

  it("商户折扣券核销不显示物品码框，请求不带 trace_codes", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-open-vi_c")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-redeem-open-vi_c"));
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-note")).toBeTruthy());
    expect(screen.queryByTestId("voucher-redeem-trace-codes")).toBeNull();
    fireEvent.click(screen.getByTestId("voucher-redeem-guided-next"));
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-redeem-save"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.body).not.toHaveProperty("trace_codes");
  });
});
