import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VoucherLedgerPage } from "./VoucherLedgerPage";
import { AdminApi } from "../client/adminClient";
import { REPLAYED_NOTICE } from "../../shared/operationKey";
import { ReplayedNotice } from "../../shared/ReplayedNotice";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// M725-F02、M725-F03（治理地图第 303 行「同一个操作被重复提交，系统只认一次，不会因为重试就……重复核销」
// 「同一次操作重复提交时，返回第一次的结果并提示已完成」）：
// 第一次提交服务端已生效、响应却丢了，对话框留着，操作员再点提交——这是同一次核销／发放，不是新的一次。
const DEFINITIONS = [
  { id: "vdef_1", name: "满 100 减 50 券", voucher_kind: "merchant_discount", redeem_method: "in_store_staff", settlement_mode: "none", merchant_id: "merchant-1", face_value_cent: 5000, valid_days: 30, status: "active" },
];
const INSTANCES = [
  { id: "vi_1", definition_id: "vdef_1", customer_id: "customer-1", code: "VCAAA", source_type: "marketing_grant", source_ref: "vgrant_1", status: "issued", issued_at: "2026-07-18T00:00:00Z" },
];

function envelope(data: unknown, replayed = false): Response {
  const body = replayed
    ? { success: true, code: "OK", message: REPLAYED_NOTICE, data, replayed: true }
    : { success: true, code: "OK", message: "success", data };
  return new Response(JSON.stringify(body), { status: 200, headers: { "content-type": "application/json" } });
}

let posts: Array<{ url: string; key: string }> = [];
function makeApi(): AdminApi {
  posts = [];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        posts.push({ url, key: String(JSON.parse(String(init?.body)).idempotency_key) });
        // 第一次：服务端已生效，响应丢在路上。再提交：服务端认出是同一次，返回第一次的结果并带 replayed。
        if (posts.length === 1) return Promise.reject(new TypeError("Failed to fetch"));
        if (url.includes("/redemptions")) return Promise.resolve(envelope({ id: "vrr_vi_1", voucher_instance_id: "vi_1", settlement_mode: "none" }, true));
        return Promise.resolve(envelope({ definition_id: "vdef_1", customer_id: "customer-9", quantity: 2, instances: [] }, true));
      }
      if (url.includes("/vouchers/definitions")) return Promise.resolve(envelope({ items: DEFINITIONS, page: 1, page_size: 200, total: 1 }));
      if (url.includes("/vouchers/instances")) return Promise.resolve(envelope({ items: INSTANCES, page: 1, page_size: 20, total: 1 }));
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  // 页面按点击用「前缀-毫秒」现生成编号；让两次点击的毫秒必然不同。
  let now = 1_760_000_000_000;
  vi.spyOn(Date, "now").mockImplementation(() => (now += 1000));
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(
    <>
      <ReplayedNotice />
      <VoucherLedgerPage {...props} />
    </>,
  );
}

describe("VoucherLedgerPage 同一次核销／发放没拿到结果再提交", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("核销：再点提交沿用首发编号，拿到第一次的核销结果并提示已完成", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-open-vi_1")).toBeTruthy(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("voucher-redeem-open-vi_1"));
    fireEvent.change(screen.getByTestId("voucher-redeem-note"), { target: { value: "客人到店领取" } });
    fireEvent.click(screen.getByTestId("voucher-redeem-guided-next"));
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-guided-summary")).toBeTruthy(), { timeout: 5000 });

    fireEvent.click(screen.getByTestId("voucher-redeem-save"));
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-guided-error")).toBeTruthy(), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    await waitFor(() => expect(screen.getByTestId("voucher-redeem-save").className).not.toContain("ant-btn-loading"), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("voucher-redeem-save"));
    await waitFor(() => expect(screen.queryByTestId("voucher-redeem-guided")).toBeNull(), { timeout: 5000 });
    expect(posts).toHaveLength(2);
    expect(posts[1]!.url).toContain("/api/v1/vouchers/redemptions");
    expect(posts[1]!.key).toBe(posts[0]!.key);
    expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE);
  });

  it("发放：再点提交沿用首发编号，不会再发一批，拿到第一次的发放结果并提示已完成", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-issue-open")).toBeTruthy(), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("voucher-issue-open"));
    fireEvent.change(screen.getByTestId("voucher-issue-customer"), { target: { value: "customer-9" } });
    fireEvent.change(screen.getByTestId("voucher-issue-quantity"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("voucher-issue-guided-next"));
    await waitFor(() => expect(screen.getByTestId("voucher-issue-guided-summary")).toBeTruthy(), { timeout: 5000 });

    fireEvent.click(screen.getByTestId("voucher-issue-save"));
    await waitFor(() => expect(screen.getByTestId("voucher-issue-guided-error")).toBeTruthy(), { timeout: 5000 });
    expect(posts).toHaveLength(1);

    await waitFor(() => expect(screen.getByTestId("voucher-issue-save").className).not.toContain("ant-btn-loading"), { timeout: 5000 });
    fireEvent.click(screen.getByTestId("voucher-issue-save"));
    await waitFor(() => expect(screen.queryByTestId("voucher-issue-guided")).toBeNull(), { timeout: 5000 });
    expect(posts).toHaveLength(2);
    expect(posts[1]!.url).toContain("/api/v1/vouchers/instances");
    expect(posts[1]!.key).toBe(posts[0]!.key);
    expect(screen.getByTestId("replayed-notice").textContent).toContain(REPLAYED_NOTICE);
  });
});
