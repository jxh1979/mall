// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { VoucherLedgerPage } from "./VoucherLedgerPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十批 TK-28（ENUM L81「商户折扣券——到对应商户抵扣」；PGM L303）：商户折扣券不再从券台账独立核销，
// 行内不出现「核销」按钮，改提示「在酒吧结账时抵扣核销」；店内实物券仍可核销。
const DEFINITIONS = [
  { id: "vdef_d", name: "满 100 减 50 券", voucherKind: "merchant_discount", redeemMethod: "in_store_staff", settlementMode: "none", merchantId: "merchant-1", merchantType: "bar", faceValueCent: 5000, status: "active" },
  { id: "vdef_c", name: "餐饮满减券", voucherKind: "merchant_discount", redeemMethod: "in_store_staff", settlementMode: "none", merchantId: "merchant-c", merchantType: "catering", faceValueCent: 3000, status: "active" },
  { id: "vdef_g", name: "啤酒一杯券", voucherKind: "in_store_goods", redeemMethod: "in_store_staff", settlementMode: "none", merchantId: "merchant-1", status: "active" },
];
const INSTANCES = [
  { id: "vi_d", definitionId: "vdef_d", customerId: "customer-1", code: "VCDDD", sourceType: "marketing_grant", status: "issued", issuedAt: "2026-07-18T00:00:00Z" },
  { id: "vi_g", definitionId: "vdef_g", customerId: "customer-1", code: "VCGGG", sourceType: "marketing_grant", status: "issued", issuedAt: "2026-07-18T00:00:00Z" },
  { id: "vi_c", definitionId: "vdef_c", customerId: "customer-1", code: "VCCCC", sourceType: "marketing_grant", status: "issued", issuedAt: "2026-07-18T00:00:00Z" },
];

describe("券台账：商户折扣券只在结账时抵扣核销", () => {
  afterEach(() => vi.restoreAllMocks());

  it("折扣券行无核销按钮并提示结账抵扣，实物券行仍可核销", async () => {
    const api = {
      get: vi.fn().mockResolvedValue([]),
      getPaged: vi.fn(async (url: string) => {
        if (url.includes("/vouchers/definitions")) return { items: DEFINITIONS, total: 2, page: 1, size: 200 };
        if (url.includes("/vouchers/instances")) return { items: INSTANCES, total: 3, page: 1, size: 20 };
        return { items: [], total: 0, page: 1, size: 20 };
      }),
      post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
    };
    render(<VoucherLedgerPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("voucher-redeem-open-vi_g")).toBeTruthy());
    expect(screen.queryByTestId("voucher-redeem-open-vi_d")).toBeNull();
    expect(screen.getByTestId("voucher-instance-row-vi_d").textContent).toContain("在酒吧结账时抵扣核销");
    expect(screen.getByTestId("voucher-revoke-open-vi_d")).toBeTruthy();
  });

  // 操作员裁决 2026-10-20 第三轮第 9 条「非酒吧保留独立核销」：只对酒吧商户的折扣券隐藏核销按钮。
  it("非酒吧商户折扣券行保留核销按钮", async () => {
    const api = {
      get: vi.fn().mockResolvedValue([]),
      getPaged: vi.fn(async (url: string) => {
        if (url.includes("/vouchers/definitions")) return { items: DEFINITIONS, total: 3, page: 1, size: 200 };
        if (url.includes("/vouchers/instances")) return { items: INSTANCES, total: 3, page: 1, size: 20 };
        return { items: [], total: 0, page: 1, size: 20 };
      }),
      post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
    };
    render(<VoucherLedgerPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("voucher-redeem-open-vi_c")).toBeTruthy());
    expect(screen.queryByTestId("voucher-redeem-open-vi_d")).toBeNull();
  });
});
