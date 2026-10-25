// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { platform } from "../platform";
import { VoucherWalletPage } from "./VoucherWalletPage";
import type { CustomerService } from "@complex/customer-core";
import type { CustomerVoucher, VoucherTransfer } from "@complex/customer-core";

// BSM「我的券包：客人可查看自己名下全部虚拟券，按「可用 / 已核销（已使用） / 已过期 / 已失效」分组；已失效详情显示原因以及是否退回积分。
// 核销后的券不能再次使用；可用券只有在券种允许时才显示转赠入口」；BSM「客人出示的……权益核销码……都不是一张固定的图，而是每隔一段时间自动更新的
// 动态码」；第十三批裁决第 9 条「和门票一致不冻结：发起转赠后原持有人仍可自用或改转他人，先成功的动作为准，其余自动作废」。
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
  code: null,
  invalidReason: null,
  pointsRefunded: null,
  transferable: true,
};

const usedGoods: CustomerVoucher = {
  ...discount,
  id: "v2",
  name: "店内实物兑换券",
  voucherKind: "in_store_goods",
  status: "redeemed",
  remainingTimes: 0,
  expiresAt: null,
  faceValueCent: null,
  transferable: false,
};

const expiredPartner: CustomerVoucher = {
  ...discount,
  id: "v3",
  source: "pass_partner",
  name: "餐厅合作券",
  voucherKind: "external_partner",
  status: "expired",
  remainingTimes: 0,
  expiresAt: "2026-01-01T00:00:00Z",
  faceValueCent: null,
  redeemMethod: "external_reflow",
  transferable: false,
};

const revokedPoints: CustomerVoucher = {
  ...discount,
  id: "v4",
  name: "积分兑换饮品券",
  voucherKind: "in_store_goods",
  status: "revoked",
  remainingTimes: 0,
  invalidReason: "兑换商品缺货",
  pointsRefunded: 200,
  transferable: false,
};

const notTransferable: CustomerVoucher = { ...discount, id: "v5", name: "生日专享券", faceValueCent: 1000, redeemMethod: "onsite_code",
  transferable: false };

const LINK_TRANSFER: VoucherTransfer = { id: "vt1", voucherInstanceId: "v1", voucherName: "满100减20券", channel: "link",
  status: "pending", claimCode: "VCLAIM1", createdAt: null };

describe("VoucherWalletPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("voucherWalletHasFourGroupsAndNoStaticCode：四组分组，已失效写原因与是否退回积分；只认券种允许的转赠入口", async () => {
    const service = svc({ listMyVouchers: vi.fn().mockResolvedValue([discount, usedGoods, expiredPartner, revokedPoints, notTransferable]) });
    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.queryByTestId("voucher-group-invalid")).not.toBeNull());

    const available = screen.getByTestId("voucher-group-available");
    expect(within(available).getByText("满100减20券")).toBeInTheDocument();
    expect(within(available).getByText("抵扣 ¥20.00")).toBeInTheDocument();
    expect(within(available).getByText(/到店由店员核销/)).toBeInTheDocument();
    expect(within(available).getAllByTestId("voucher-use")).toHaveLength(2);
    expect(within(available).getAllByTestId("voucher-transfer")).toHaveLength(1);

    expect(within(screen.getByTestId("voucher-group-redeemed")).getByText("店内实物兑换券")).toBeInTheDocument();
    const expired = screen.getByTestId("voucher-group-expired");
    expect(within(expired).getByText("餐厅合作券")).toBeInTheDocument();
    expect(within(expired).queryByText(/核销方式/)).not.toBeInTheDocument();

    const invalid = screen.getByTestId("voucher-group-invalid");
    expect(invalid.textContent).toContain("积分兑换饮品券");
    expect(invalid.textContent).toContain("失效原因：兑换商品缺货");
    expect(invalid.textContent).toContain("已退回 200 积分");
    expect(expired.textContent).not.toContain("积分兑换饮品券");
  });

  it("useShowsDynamicCodeNotStaticCode：「使用」出示按统一间隔刷新的动态码，不显示也不能复制固定券码", async () => {
    const getCode = vi.fn().mockResolvedValue({ voucherInstanceId: "v1", code: "DYN-CODE-1", expiresAt: "2026-10-24T12:00:20Z",
      refreshIntervalSeconds: 20 });
    const service = svc({ listMyVouchers: vi.fn().mockResolvedValue([{ ...discount, code: "VC-USE-CODE" }]), getVoucherRedeemCode: getCode });
    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.queryByTestId("voucher-use")).not.toBeNull());
    fireEvent.click(screen.getByTestId("voucher-use"));
    await waitFor(() => expect(getCode).toHaveBeenCalledWith("v1"));
    await waitFor(() => expect(screen.queryByTestId("qr-countdown")?.textContent ?? "").toContain("20 秒后失效"));
    expect(document.body.textContent).not.toContain("VC-USE-CODE");
    expect(document.body.textContent).not.toContain("DYN-CODE-1");
    expect(screen.queryByText("复制券码")).toBeNull();
  });

  it("initiates a phone-channel transfer without freezing the voucher", async () => {
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
    expect(document.body.textContent).toContain("对方确认领取前，你仍可自己使用这张券或改转他人");
    expect(document.body.textContent).not.toContain("将不再属于你");
    fireEvent.change(screen.getByTestId("voucher-transfer-phone"), { target: { value: "13800008888" } });
    fireEvent.click(screen.getByTestId("voucher-transfer-submit"));
    await waitFor(() =>
      expect(initiate).toHaveBeenCalledWith({ voucherInstanceId: "v1", channel: "phone", receiverPhone: "13800008888" }),
    );
  });

  it("transferShareUsesDeepLinkWithoutToken：链接方式转赠分享领取深链（只带领取码）", async () => {
    const share = vi.spyOn(platform, "share").mockResolvedValue("shared");
    const service = svc({ listMyVouchers: vi.fn().mockResolvedValue([discount]), initiateVoucherTransfer: vi.fn().mockResolvedValue(LINK_TRANSFER) });
    render(<VoucherWalletPage service={service} />);
    await waitFor(() => expect(screen.queryByTestId("voucher-transfer")).not.toBeNull());
    fireEvent.click(screen.getByTestId("voucher-transfer"));
    await waitFor(() => expect(screen.queryByText("生成领取链接")).not.toBeNull());
    fireEvent.click(screen.getByText("生成领取链接"));
    fireEvent.click(screen.getByTestId("voucher-transfer-submit"));
    await waitFor(() => expect(screen.queryByTestId("voucher-transfer-share")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("voucher-transfer-share")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("voucher-transfer-share"));
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1));
    expect(share.mock.calls[0]![0].link).toBe("https://c.complex.test/c/voucher-claim?claim_code=VCLAIM1");
  });

  it("claimDeepLinkPreviewThenConfirm：领取深链打开只预览，点「确认领取」才领取", async () => {
    const preview = vi.fn().mockResolvedValue({ ...LINK_TRANSFER, claimCode: null });
    const claim = vi.fn().mockResolvedValue({ ...LINK_TRANSFER, status: "claimed" });
    const service = svc({ previewVoucherTransfer: preview, claimVoucherTransfer: claim });
    render(<VoucherWalletPage service={service} initialClaimCode="VCLAIM1" />);
    await waitFor(() => expect(screen.queryByTestId("voucher-claim-preview")?.textContent ?? "").toContain("满100减20券"));
    expect(preview).toHaveBeenCalledWith("VCLAIM1");
    expect(claim).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.getByTestId("voucher-claim-confirm")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("voucher-claim-confirm"));
    await waitFor(() => expect(claim).toHaveBeenCalledWith("vt1", "VCLAIM1"));
  });

  it("scanClaimQrPreviewsOnly：扫对方出示的领取二维码只认券领取链接，扫到即预览、不领取；别的码提示无法识别", async () => {
    const scan = vi.spyOn(platform, "scanCode")
      .mockResolvedValueOnce("https://c.complex.test/c/ticket-claim?claim_code=VCLAIM1")
      .mockResolvedValueOnce("https://c.complex.test/c/voucher-claim?claim_code=VCLAIM1");
    const preview = vi.fn().mockResolvedValue({ ...LINK_TRANSFER, claimCode: null });
    const claim = vi.fn();
    render(<VoucherWalletPage service={svc({ previewVoucherTransfer: preview, claimVoucherTransfer: claim })} />);
    await waitFor(() => expect(screen.queryByTestId("voucher-claim-scan")).not.toBeNull());
    fireEvent.click(screen.getByTestId("voucher-claim-scan"));
    await waitFor(() => expect(document.body.textContent).toContain("无法识别的二维码"));
    expect(preview).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("voucher-claim-scan"));
    await waitFor(() => expect(screen.queryByTestId("voucher-claim-preview")?.textContent ?? "").toContain("满100减20券"));
    expect(preview).toHaveBeenCalledWith("VCLAIM1");
    expect(claim).not.toHaveBeenCalled();
    expect(scan).toHaveBeenCalledTimes(2);
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
