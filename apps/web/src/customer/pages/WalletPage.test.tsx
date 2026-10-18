// 文件版本：V0.04
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { WalletPage } from "./WalletPage";
import type { CustomerService } from "../client/customerService";

const projectCard = {
  id: "svacc_prj",
  customerId: "c-1",
  projectId: "prj_1",
  merchantId: null,
  cardScope: "project",
  fundOwnership: null,
  balanceCent: 5000,
  principalCent: 4500,
  bonusCent: 500,
  status: "active",
  createdAt: "",
  updatedAt: "",
};

const merchantCard = {
  id: "svacc_m1",
  customerId: "c-1",
  projectId: null,
  merchantId: "m1",
  cardScope: "merchant",
  fundOwnership: "platform_collect",
  balanceCent: 3000,
  principalCent: 3000,
  bonusCent: 0,
  status: "active",
  createdAt: "",
  updatedAt: "",
};

describe("WalletPage", () => {
  afterEach(() => { vi.restoreAllMocks(); sessionStorage.removeItem("customer-pending-stored-value-topups"); });

  it("renders per-card balances for the general card and each merchant card", async () => {
    const service = {
      getStoredValueAccounts: vi.fn().mockResolvedValue([projectCard, merchantCard]),
      topupStoredValue: vi.fn(),
      getStoredValueTransactions: vi.fn().mockResolvedValue([]),
    } as unknown as CustomerService;

    render(<WalletPage service={service} />);

    // 分卡余额展示：通用卡 ¥50.00 + 商户卡 ¥30.00，各自独立。
    await waitFor(() => expect(screen.getByText("综合体通用卡")).toBeInTheDocument());
    expect(screen.getByText(/商户卡（m1）/)).toBeInTheDocument();
    expect(screen.getByText("余额 ¥50.00")).toBeInTheDocument();
    expect(screen.getByText("余额 ¥30.00")).toBeInTheDocument();
    // CG-06a：本金与赠送金分列（BSM「我的余额」）。
    expect(screen.getByText("本金 ¥45.00 · 赠送金 ¥5.00")).toBeInTheDocument();
    expect(screen.getByText("本金 ¥30.00 · 赠送金 ¥0.00")).toBeInTheDocument();
    // 商户卡资金模式标签（平台代收）。
    expect(screen.getByText("平台代收")).toBeInTheDocument();
    expect(screen.getByTestId("wallet-topup")).toBeInTheDocument();
    expect(screen.queryByTestId("wallet-consume")).not.toBeInTheDocument();
    expect(screen.getByText("交易记录")).toBeInTheDocument();
  });

  it("按选择的渠道发起线上充值，未配置时不报告到账或要求柜台收款", async () => {
    const topup = vi.fn().mockResolvedValue({ id: "pay-1", gatewayStatus: "NOT_CONFIGURED" });
    const service = {
      getStoredValueAccounts: vi.fn().mockResolvedValue([merchantCard]),
      getStoredValueTransactions: vi.fn().mockResolvedValue([]),
      topupStoredValue: topup,
    } as unknown as CustomerService;
    render(<WalletPage service={service} />);
    await screen.findAllByText(/商户卡（m1）/);
    fireEvent.change(screen.getByLabelText("充值支付方式"), { target: { value: "alipay" } });
    fireEvent.change(screen.getByRole("spinbutton"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("wallet-topup"));
    await waitFor(() => expect(topup).toHaveBeenCalledWith(expect.objectContaining({ channel: "alipay", amountCent: 5000 })));
    expect(await screen.findByText("支付渠道未配置，尚未扣款或充值到账")).toBeInTheDocument();
    expect(screen.queryByText(/请到柜台完成支付/)).toBeNull();
  });


  it("返回钱包后仅按服务端确认结果刷新余额，失败充值继续保留供查询", async () => {
    sessionStorage.setItem("customer-pending-stored-value-topups", JSON.stringify([
      { paymentIntentId: "paid", merchantId: "m1" }, { paymentIntentId: "pending", merchantId: "m1" },
    ]));
    const settle = vi.fn().mockImplementation(({ paymentIntentId }) =>
      paymentIntentId === "paid" ? Promise.resolve({ id: "actual-topup" }) : Promise.reject(new Error("支付尚未成功")));
    const accounts = vi.fn().mockResolvedValue([merchantCard]);
    const service = {
      getStoredValueAccounts: accounts,
      getStoredValueTransactions: vi.fn().mockResolvedValue([]),
      settleStoredValueTopup: settle,
    } as unknown as CustomerService;
    render(<WalletPage service={service} />);
    expect(await screen.findByText("支付尚未成功")).toBeInTheDocument();
    expect(settle).toHaveBeenCalledWith({ paymentIntentId: "paid", merchantId: "m1" });
    expect(settle).toHaveBeenCalledWith({ paymentIntentId: "pending", merchantId: "m1" });
    expect(accounts).toHaveBeenCalledTimes(2);
    expect(JSON.parse(sessionStorage.getItem("customer-pending-stored-value-topups") ?? "[]"))
      .toEqual([{ paymentIntentId: "pending", merchantId: "m1" }]);
    expect(screen.queryByRole("button", { name: "查询充值到账 · paid" })).toBeNull();
    expect(screen.getByRole("button", { name: "查询充值到账 · pending" })).toBeInTheDocument();
  });

  it("逐笔记录展示本金与赠送金拆分及调整事由", async () => {
    const service = {
      getStoredValueAccounts: vi.fn().mockResolvedValue([projectCard]),
      topupStoredValue: vi.fn(),
      getStoredValueTransactions: vi.fn().mockResolvedValue([
        { id: "t1", accountId: "svacc_prj", customerId: "c-1", txnType: "topup", amountCent: 5500, balanceAfterCent: 5500, principalCent: 5000, bonusCent: 500, reason: null, createdAt: "2026-10-17T00:00:00Z" },
        { id: "t2", accountId: "svacc_prj", customerId: "c-1", txnType: "consume", amountCent: -5200, balanceAfterCent: 300, principalCent: -5000, bonusCent: -200, reason: null, createdAt: "2026-10-17T01:00:00Z" },
        { id: "t3", accountId: "svacc_prj", customerId: "c-1", txnType: "adjust", amountCent: -300, balanceAfterCent: 0, principalCent: 0, bonusCent: -300, reason: "销户：未使用赠送金作废（不退现）", createdAt: "2026-10-17T02:00:00Z" },
      ]),
    } as unknown as CustomerService;

    render(<WalletPage service={service} />);
    await waitFor(() => expect(screen.getByText("综合体通用卡")).toBeInTheDocument());
    expect(await screen.findByText("充值 · ¥55.00")).toBeInTheDocument();
    expect(screen.getByText("本金 ¥50.00 · 赠送金 ¥5.00")).toBeInTheDocument();
    expect(screen.getByText("消费 · ¥-52.00")).toBeInTheDocument();
    expect(screen.getByText("本金 ¥-50.00 · 赠送金 ¥-2.00")).toBeInTheDocument();
    expect(screen.getByText("调整 · ¥-3.00")).toBeInTheDocument();
    expect(screen.getByText("本金 ¥0.00 · 赠送金 ¥-3.00 · 销户：未使用赠送金作废（不退现）")).toBeInTheDocument();
  });

  it("shows empty state when the customer has no stored-value cards", async () => {
    const service = {
      getStoredValueAccounts: vi.fn().mockResolvedValue([]),
      topupStoredValue: vi.fn(),
      getStoredValueTransactions: vi.fn().mockResolvedValue([]),
    } as unknown as CustomerService;

    render(<WalletPage service={service} />);
    await waitFor(() => expect(screen.getByText("暂无储值卡")).toBeInTheDocument());
  });
});
