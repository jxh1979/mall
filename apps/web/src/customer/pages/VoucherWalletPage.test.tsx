import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { VoucherWalletPage } from "./VoucherWalletPage";
import type { CustomerService } from "../client/customerService";
import type { CustomerVoucher, VoucherTransfer } from "../types";

// 组件会拉取待领取/我发起的转赠列表；默认桩返回空，个别用例覆写。
function svc(overrides: Partial<Record<keyof CustomerService, unknown>>): CustomerService {
  return {
    listMyVouchers: vi.fn().mockResolvedValue([]),
    listIncomingVoucherTransfers: vi.fn().mockResolvedValue([] as VoucherTransfer[]),
    listOutgoingVoucherTransfers: vi.fn().mockResolvedValue([] as VoucherTransfer[]),
    ...overrides,
  } as unknown as CustomerService;
}

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
  code: "VC-USE-CODE",
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
    const service = svc({ listMyVouchers: vi.fn().mockResolvedValue([discount, usedGoods, expiredPartner]) });

    render(<VoucherWalletPage service={service} />);

    await waitFor(() => expect(screen.getByTestId("voucher-group-available")).toBeInTheDocument());

    const available = screen.getByTestId("voucher-group-available");
    // 可用组含折扣券：面额（抵扣）+ 核销方式说明展示。
    expect(within(available).getByText("满100减20券")).toBeInTheDocument();
    expect(within(available).getByText("抵扣 ¥20.00")).toBeInTheDocument();
    expect(within(available).getByText(/到店由店员核销/)).toBeInTheDocument();
    // 可用的统一台账券带「使用」「转赠」自助入口。
    expect(within(available).getByTestId("voucher-use")).toBeInTheDocument();
    expect(within(available).getByTestId("voucher-transfer")).toBeInTheDocument();

    // 已核销组含实物券。
    const redeemed = screen.getByTestId("voucher-group-redeemed");
    expect(within(redeemed).getByText("店内实物兑换券")).toBeInTheDocument();

    // 已过期组含第三方券；核销方式仅可用券展示，过期券不展示。
    const expired = screen.getByTestId("voucher-group-expired");
    expect(within(expired).getByText("餐厅合作券")).toBeInTheDocument();
    expect(within(expired).queryByText(/核销方式/)).not.toBeInTheDocument();
  });

  it("reveals the redemption code when tapping 使用 on an available voucher", async () => {
    const service = svc({ listMyVouchers: vi.fn().mockResolvedValue([discount]) });
    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.getByTestId("voucher-use")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("voucher-use"));
    await waitFor(() => expect(screen.getByTestId("voucher-use-code")).toHaveTextContent("VC-USE-CODE"));
  });

  it("initiates a phone-channel transfer and refreshes", async () => {
    const initiate = vi.fn().mockResolvedValue({
      id: "vt1",
      voucherInstanceId: "v1",
      voucherName: "满100减20券",
      channel: "phone",
      status: "pending",
      receiverPhoneMasked: "138****8888",
      createdAt: null,
    } as VoucherTransfer);
    const service = svc({
      listMyVouchers: vi.fn().mockResolvedValue([discount]),
      initiateVoucherTransfer: initiate,
    });
    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.getByTestId("voucher-transfer")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("voucher-transfer"));
    await waitFor(() => expect(screen.getByTestId("voucher-transfer-phone")).toBeInTheDocument());
    fireEvent.change(screen.getByTestId("voucher-transfer-phone"), { target: { value: "13800008888" } });
    fireEvent.click(screen.getByTestId("voucher-transfer-submit"));
    await waitFor(() =>
      expect(initiate).toHaveBeenCalledWith({ voucherInstanceId: "v1", channel: "phone", receiverPhone: "13800008888" }),
    );
  });

  it("shows empty state when the customer has no vouchers", async () => {
    const service = svc({ listMyVouchers: vi.fn().mockResolvedValue([]) });

    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.getByTestId("voucher-empty")).toBeInTheDocument());
  });

  it("shows an error alert with retry when loading fails", async () => {
    const service = svc({ listMyVouchers: vi.fn().mockRejectedValue(new Error("网络异常")) });

    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.getByTestId("voucher-retry")).toBeInTheDocument());
    expect(screen.getByText("网络异常")).toBeInTheDocument();
  });
});
