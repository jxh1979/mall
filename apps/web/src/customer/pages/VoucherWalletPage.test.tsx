import { render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VoucherWalletPage } from "./VoucherWalletPage";
import type { CustomerService } from "../client/customerService";
import type { CustomerVoucher } from "../types";

const discount: CustomerVoucher = {
  id: "v1",
  source: "unified",
  name: "满100减20券",
  voucherKind: "merchant_discount",
  customerId: "c-1",
  status: "issued",
  remainingTimes: 1,
  expiresAt: "2026-12-31T00:00:00Z",
  faceValueCent: 2000,
  redeemMethod: "in_store_staff",
};

const usedGoods: CustomerVoucher = {
  id: "v2",
  source: "unified",
  name: "店内实物兑换券",
  voucherKind: "in_store_goods",
  customerId: "c-1",
  status: "redeemed",
  remainingTimes: 0,
  expiresAt: null,
  faceValueCent: null,
  redeemMethod: "in_store_staff",
};

const expiredPartner: CustomerVoucher = {
  id: "v3",
  source: "pass_partner",
  name: "餐厅合作券",
  voucherKind: "external_partner",
  customerId: "c-1",
  status: "expired",
  remainingTimes: 0,
  expiresAt: "2026-01-01T00:00:00Z",
  faceValueCent: null,
  redeemMethod: "external_reflow",
};

describe("VoucherWalletPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("groups vouchers into available / redeemed / expired and shows value + method for available", async () => {
    const service = {
      listMyVouchers: vi.fn().mockResolvedValue([discount, usedGoods, expiredPartner]),
    } as unknown as CustomerService;

    render(<VoucherWalletPage service={service} />);

    await waitFor(() => expect(screen.getByTestId("voucher-group-available")).toBeInTheDocument());

    const available = screen.getByTestId("voucher-group-available");
    // 可用组含折扣券：面额（抵扣）+ 核销方式说明展示。
    expect(within(available).getByText("满100减20券")).toBeInTheDocument();
    expect(within(available).getByText("抵扣 ¥20.00")).toBeInTheDocument();
    expect(within(available).getByText(/到店由店员核销/)).toBeInTheDocument();

    // 已核销组含实物券。
    const redeemed = screen.getByTestId("voucher-group-redeemed");
    expect(within(redeemed).getByText("店内实物兑换券")).toBeInTheDocument();

    // 已过期组含第三方券；核销方式仅可用券展示，过期券不展示。
    const expired = screen.getByTestId("voucher-group-expired");
    expect(within(expired).getByText("餐厅合作券")).toBeInTheDocument();
    expect(within(expired).queryByText(/核销方式/)).not.toBeInTheDocument();
  });

  it("shows empty state when the customer has no vouchers", async () => {
    const service = {
      listMyVouchers: vi.fn().mockResolvedValue([]),
    } as unknown as CustomerService;

    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.getByTestId("voucher-empty")).toBeInTheDocument());
  });

  it("shows an error alert with retry when loading fails", async () => {
    const service = {
      listMyVouchers: vi.fn().mockRejectedValue(new Error("网络异常")),
    } as unknown as CustomerService;

    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.getByTestId("voucher-retry")).toBeInTheDocument());
    expect(screen.getByText("网络异常")).toBeInTheDocument();
  });
});
