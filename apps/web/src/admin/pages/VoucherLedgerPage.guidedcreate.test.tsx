// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VoucherLedgerPage } from "./VoucherLedgerPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

// 券台账管理（V301 统一台账）：引导式新建券定义 / 发放（营销赠发）/ 作废理由必填 / 核销打统一端点 / 全部券聚合视图。
const DEFINITIONS = [
  // 第十批 TK-28：商户折扣券改为只在酒吧结账时抵扣核销，台账独立核销用例改用店内实物券承载。
  { id: "vdef_1", name: "啤酒一杯券", voucher_kind: "in_store_goods", redeem_method: "in_store_staff", settlement_mode: "none", merchant_id: "merchant-1", face_value_cent: null, inventory_item_id: "item_1", stock_location_id: "loc_1", stock_quantity: 1, internal_price_cent: null, valid_days: 30, valid_until: null, status: "active", created_at: "2026-07-18T00:00:00Z", updated_at: "2026-07-18T00:00:00Z" },
];
const INSTANCES = [
  { id: "vi_1", definition_id: "vdef_1", customer_id: "customer-1", code: "VCAAA", source_type: "points_redemption", source_ref: "redemption_1", status: "issued", issued_at: "2026-07-18T00:00:00Z", expires_at: "2026-08-17T00:00:00Z", redeemed_at: null, revoked_at: null, revoke_reason: null, created_at: "2026-07-18T00:00:00Z", updated_at: "2026-07-18T00:00:00Z" },
  { id: "vi_2", definition_id: "vdef_1", customer_id: "customer-2", code: "VCBBB", source_type: "marketing_grant", source_ref: "vgrant_1", status: "redeemed", issued_at: "2026-07-18T00:00:00Z", expires_at: null, redeemed_at: "2026-07-18T02:00:00Z", revoked_at: null, revoke_reason: null, created_at: "2026-07-18T00:00:00Z", updated_at: "2026-07-18T02:00:00Z" },
];
const OVERVIEW = [
  { id: "vi_1", source: "unified", name: "啤酒一杯券", voucher_kind: "in_store_goods", customer_id: "customer-1", status: "issued", remaining_times: 1, expires_at: "2026-08-17T00:00:00Z" },
  { id: "ie_1", source: "pass_partner", name: "餐厅第三方券", voucher_kind: "external_partner", customer_id: "customer-1", status: "active", remaining_times: 3, expires_at: "2026-12-31T00:00:00Z" },
];

function envelope(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } },
  );
}

let lastPostUrl: string | null = null;
let lastPostBody: unknown = null;
function makeApi(): AdminApi {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if ((init?.method ?? "GET").toUpperCase() === "POST") {
        lastPostUrl = url;
        lastPostBody = init?.body ? JSON.parse(String(init.body)) : null;
        if (url.includes("/revoke")) return Promise.resolve(envelope({ id: "vi_1", status: "revoked" }));
        if (url.includes("/redemptions")) return Promise.resolve(envelope({ id: "vrr_vi_1", voucher_instance_id: "vi_1", settlement_mode: "none" }));
        if (url.includes("/definitions")) return Promise.resolve(envelope({ id: "vdef_new" }));
        return Promise.resolve(envelope({ definition_id: "vdef_1", customer_id: "customer-9", quantity: 2, instances: [] }));
      }
      if (url.includes("/vouchers/definitions")) {
        return Promise.resolve(envelope({ items: DEFINITIONS, page: 1, page_size: 200, total: DEFINITIONS.length }));
      }
      if (url.includes("/vouchers/overview")) {
        return Promise.resolve(envelope({ items: OVERVIEW, page: 1, page_size: 20, total: OVERVIEW.length }));
      }
      if (url.includes("/vouchers/instances")) {
        return Promise.resolve(envelope({ items: INSTANCES, page: 1, page_size: 20, total: INSTANCES.length }));
      }
      return Promise.resolve(envelope([]));
    }),
  );
  return new AdminApi("test-token");
}

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage() {
  const setActionResult = vi.fn();
  const props: AdminPageProps = {
    api: makeApi(),
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<VoucherLedgerPage {...props} />);
  return { setActionResult };
}

describe("VoucherLedgerPage 券台账管理", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    lastPostUrl = null;
    lastPostBody = null;
  });

  it("券定义 + 实例台账展示；已发放行才有核销/作废；默认不展示弹窗", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-definition-row-vdef_1")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("voucher-instance-row-vi_1")).toBeTruthy());
    expect(screen.getByTestId("voucher-redeem-open-vi_1")).toBeTruthy();
    expect(screen.getByTestId("voucher-revoke-open-vi_1")).toBeTruthy();
    // 已核销行不再提供核销/作废。
    expect(screen.queryByTestId("voucher-redeem-open-vi_2")).toBeNull();
    expect(screen.queryByTestId("voucher-revoke-open-vi_2")).toBeNull();
    expect(screen.queryByTestId("voucher-create-def-guided")).toBeNull();
    expect(screen.queryByTestId("voucher-issue-guided")).toBeNull();
  });

  it("引导式新建券定义：折扣券缺面额被拦截→补齐提交（元转分、载荷正确）", async () => {
    const { setActionResult } = renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-create-def-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-create-def-open"));

    // singlePage 模式：全部字段一屏。面额留空点下一步 → 校验拦截，不进确认页。
    fireEvent.change(screen.getByTestId("voucher-create-def-name"), { target: { value: "新客满减券" } });
    fireEvent.change(screen.getByTestId("voucher-create-def-merchant"), { target: { value: "merchant-1" } });
    fireEvent.click(screen.getByTestId("voucher-create-def-guided-next"));
    expect(screen.getByTestId("voucher-create-def-guided-error").textContent).toContain("面额");
    expect(screen.queryByTestId("voucher-create-def-guided-summary")).toBeNull();

    fireEvent.change(screen.getByTestId("voucher-create-def-face"), { target: { value: "50.5" } });
    fireEvent.change(screen.getByTestId("voucher-create-def-days"), { target: { value: "30" } });
    fireEvent.click(screen.getByTestId("voucher-create-def-guided-next"));

    await waitFor(() => expect(screen.getByTestId("voucher-create-def-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-create-def-save"));

    await waitFor(() => expect(lastPostUrl).toContain("/api/v1/vouchers/definitions"));
    const body = lastPostBody as Record<string, unknown>;
    expect(body.name).toBe("新客满减券");
    expect(body.voucher_kind).toBe("merchant_discount");
    expect(body.settlement_mode).toBe("none");
    expect(body.merchant_id).toBe("merchant-1");
    expect(body.face_value_cent).toBe(5050);
    expect(body.valid_days).toBe(30);
    await waitFor(() => expect(screen.queryByTestId("voucher-create-def-guided")).toBeNull());
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "voucher_definition_created" }));
  });

  it("发放券（营销赠发）：客人必填→提交带定义/客人/张数", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-issue-open")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-issue-open"));

    fireEvent.change(screen.getByTestId("voucher-issue-customer"), { target: { value: "" } });
    fireEvent.click(screen.getByTestId("voucher-issue-guided-next"));
    expect(screen.getByTestId("voucher-issue-guided-error").textContent).toContain("客人");

    fireEvent.change(screen.getByTestId("voucher-issue-customer"), { target: { value: "customer-9" } });
    fireEvent.change(screen.getByTestId("voucher-issue-quantity"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("voucher-issue-guided-next"));
    await waitFor(() => expect(screen.getByTestId("voucher-issue-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-issue-save"));

    await waitFor(() => expect(lastPostUrl).toContain("/api/v1/vouchers/instances"));
    const body = lastPostBody as Record<string, unknown>;
    expect(body.definition_id).toBe("vdef_1");
    expect(body.customer_id).toBe("customer-9");
    expect(body.quantity).toBe(2);
    await waitFor(() => expect(screen.queryByTestId("voucher-issue-guided")).toBeNull());
  });

  it("作废：理由必填，提交打到 /instances/{id}/revoke", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-revoke-open-vi_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-revoke-open-vi_1"));

    fireEvent.click(screen.getByTestId("voucher-revoke-guided-next"));
    expect(screen.getByTestId("voucher-revoke-guided-error").textContent).toContain("理由");
    fireEvent.change(screen.getByTestId("voucher-revoke-reason"), { target: { value: "活动下线，统一作废" } });
    fireEvent.click(screen.getByTestId("voucher-revoke-guided-next"));
    await waitFor(() => expect(screen.getByTestId("voucher-revoke-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-revoke-save"));

    await waitFor(() => expect(lastPostUrl).toContain("/api/v1/vouchers/instances/vi_1/revoke"));
    expect((lastPostBody as Record<string, unknown>).reason).toBe("活动下线，统一作废");
    await waitFor(() => expect(screen.queryByTestId("voucher-revoke-guided")).toBeNull());
  });

  it("核销：提交打到统一核销端点（按实例 id）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-open-vi_1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-redeem-open-vi_1"));

    fireEvent.change(screen.getByTestId("voucher-redeem-note"), { target: { value: "客人到店领取" } });
    fireEvent.click(screen.getByTestId("voucher-redeem-guided-next"));
    await waitFor(() => expect(screen.getByTestId("voucher-redeem-guided-summary")).toBeTruthy());
    fireEvent.click(screen.getByTestId("voucher-redeem-save"));

    await waitFor(() => expect(lastPostUrl).toContain("/api/v1/vouchers/redemptions"));
    const body = lastPostBody as Record<string, unknown>;
    expect(body.voucher_instance_id).toBe("vi_1");
    expect(body.note).toBe("客人到店领取");
    await waitFor(() => expect(screen.queryByTestId("voucher-redeem-guided")).toBeNull());
  });

  it("全部券聚合视图：统一台账与通票第三方券两源并列（含剩余次数）", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("voucher-filter-view")).toBeTruthy());
    fireEvent.change(screen.getByTestId("voucher-filter-view"), { target: { value: "all" } });

    await waitFor(() => expect(screen.getByTestId("voucher-overview-row-ie_1")).toBeTruthy());
    expect(screen.getByTestId("voucher-overview-row-vi_1")).toBeTruthy();
    expect(screen.getByTestId("voucher-overview-row-ie_1").textContent).toContain("通票第三方券");
    expect(screen.getByTestId("voucher-overview-row-ie_1").textContent).toContain("3");
  });
});
