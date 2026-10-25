// 文件版本：V0.06
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { platform } from "../platform";
import { WalletPage } from "./WalletPage";
import type { CustomerService } from "@complex/customer-core";

vi.mock("../api", async (importOriginal) => ({ ...(await importOriginal<typeof import("../api")>()), CUSTOMER_PAY_HOSTS: ["pay.example.com"] }));

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

const RULES = { cardScope: "merchant", projectId: null, merchantId: "m1", bonusBps: 0, consumeOrder: "bonus_first",
  consumeOrderText: "先扣赠送金、后扣本金", bonusRuleText: "赠送金不可提现、不退现", refundRuleText: "可退卡，按剩余本金的 80% 退回",
  refundAllowed: true, refundRatioBps: 8000 };

function makeService(overrides: Record<string, unknown> = {}): CustomerService {
  return {
    getStoredValueAccounts: vi.fn().mockResolvedValue([projectCard, merchantCard]),
    getStoredValueTransactions: vi.fn().mockResolvedValue([]),
    getStoredValueTopupRules: vi.fn().mockResolvedValue(RULES),
    listMerchantCardExitNotices: vi.fn().mockResolvedValue([]),
    topupStoredValue: vi.fn(),
    settleStoredValueTopup: vi.fn(),
    ...overrides,
  } as unknown as CustomerService;
}

async function chooseMerchantCardAndAmount() {
  await screen.findAllByText(/商户卡（m1）/);
  fireEvent.click(screen.getByTestId("wallet-pick-svacc_m1"));
  await waitFor(() => expect(screen.queryByTestId("wallet-topup-rules")?.textContent ?? "").toContain("先扣赠送金、后扣本金"));
  fireEvent.change(screen.getByLabelText("充值金额（元）"), { target: { value: "50" } });
  await waitFor(() => expect(screen.getByTestId("wallet-topup-pay")).not.toBeDisabled());
}

describe("WalletPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("renders per-card balances for the general card and each merchant card", async () => {
    render(<WalletPage service={makeService()} />);

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
    expect(screen.getByTestId("wallet-topup-pay")).toBeInTheDocument();
    expect(screen.queryByTestId("wallet-consume")).not.toBeInTheDocument();
    expect(screen.getByText("交易记录")).toBeInTheDocument();
  });

  // ENUM「储值卡的消费扣款顺序、赠送金的性质与退卡口径，须在客人充值之前告知」。
  it("topupRulesShownBeforeTopup：选卡后先读该卡的充值规则并展示，读不到不许充值", async () => {
    // 商户卡的规则读得到、通用卡的读不到。
    const rules = vi.fn().mockImplementation(async (scope: { merchantId?: string }) => {
      if (scope.merchantId) return RULES;
      throw new Error("网络异常");
    });
    const service = makeService({ getStoredValueTopupRules: rules });
    render(<WalletPage service={service} />);
    await chooseMerchantCardAndAmount();
    expect(rules).toHaveBeenCalledWith({ projectId: undefined, merchantId: "m1" });
    const text = screen.getByTestId("wallet-topup-rules").textContent ?? "";
    for (const item of ["先扣赠送金、后扣本金", "赠送金不可提现、不退现", "可退卡，按剩余本金的 80% 退回"]) expect(text).toContain(item);

    fireEvent.click(screen.getByTestId("wallet-pick-svacc_prj"));
    await waitFor(() => expect(rules).toHaveBeenCalledWith({ projectId: "prj_1", merchantId: undefined }));
    await waitFor(() => expect(screen.queryByTestId("wallet-topup-rules")?.textContent ?? "").toContain("充值规则暂时读不到"));
    expect(screen.getByTestId("wallet-topup-pay")).toBeDisabled();
  });

  it("按选择的渠道发起线上充值，未配置时显示「支付暂不可用，未扣款」、不报到账", async () => {
    const topup = vi.fn().mockResolvedValue({ id: "pay-1", gatewayStatus: "NOT_CONFIGURED", paymentUrl: null, launch: null });
    const service = makeService({ topupStoredValue: topup });
    render(<WalletPage service={service} />);
    await chooseMerchantCardAndAmount();
    fireEvent.change(screen.getByLabelText("充值支付方式"), { target: { value: "alipay" } });
    fireEvent.click(screen.getByTestId("wallet-topup-pay"));
    await waitFor(() => expect(topup).toHaveBeenCalledWith({ merchantId: "m1", projectId: undefined, amountCent: 5000, channel: "alipay" }));
    await waitFor(() => expect(screen.queryByTestId("wallet-topup-unavailable")?.textContent ?? "").toContain("支付暂不可用，未扣款"));
    expect(screen.queryByText("充值已到账")).toBeNull();
  });

  it("topupOpensPaymentOutsideAndSettlesOnResume：H5 付款页交系统打开（不在应用内跳转），回前台按服务端确认入账", async () => {
    let resume: () => void = () => undefined;
    vi.spyOn(platform, "onResume").mockImplementation((listener) => {
      resume = listener;
      return () => undefined;
    });
    const openPayment = vi.spyOn(platform, "openPayment").mockResolvedValue(true);
    const setItem = vi.spyOn(Storage.prototype, "setItem");
    const topup = vi.fn().mockResolvedValue({ id: "pay-1", gatewayStatus: "PENDING", paymentUrl: "https://pay.example.com/c?no=1",
      launch: { kind: "h5_url", params: { url: "https://pay.example.com/c?no=1" } } });
    const settle = vi.fn().mockResolvedValue({ id: "txn-1" });
    const accounts = vi.fn().mockResolvedValue([projectCard, merchantCard]);
    const service = makeService({ topupStoredValue: topup, settleStoredValueTopup: settle, getStoredValueAccounts: accounts });
    render(<WalletPage service={service} />);
    await chooseMerchantCardAndAmount();
    fireEvent.click(screen.getByTestId("wallet-topup-pay"));
    await waitFor(() => expect(openPayment).toHaveBeenCalledWith("https://pay.example.com/c?no=1"));
    expect(settle).not.toHaveBeenCalled();
    act(() => resume());
    await waitFor(() => expect(settle).toHaveBeenCalledWith({ paymentIntentId: "pay-1", merchantId: "m1", projectId: undefined }));
    await waitFor(() => expect(screen.queryByText("充值已到账")).not.toBeNull());
    await waitFor(() => expect(accounts).toHaveBeenCalledTimes(2));
    expect(setItem).not.toHaveBeenCalled();
  });

  // ENUM「发卡商户退租……前，必须提前公告告知持卡消费者……（可用到哪天、能否退、退多少比例、怎么退、找谁退），公告须在客人手机应用、
  // 小程序……同时公示」；第十三批裁决第 2 条「商户在商户门户自行发公告，平台只校验公示天数」。
  it("merchantCardExitNoticesShown：公示中的商户卡退出公告逐条展示，持卡的标注", async () => {
    const service = makeService({ listMerchantCardExitNotices: vi.fn().mockResolvedValue([
      { id: "n1", merchantId: "m1", merchantName: "一号酒吧", projectId: "prj_1", useUntil: "2026-12-31", refundable: true,
        refundRatioBps: 8000, refundMethod: "到店凭卡办理", refundContact: "前台 0731-0000", publishFrom: "2026-10-01T00:00:00Z",
        publishUntil: "2026-11-30", holdsCard: true },
    ]) });
    render(<WalletPage service={service} />);
    await waitFor(() => expect(screen.queryByTestId("wallet-exit-notices")?.textContent ?? "").toContain("一号酒吧"));
    const text = screen.getByTestId("wallet-exit-notices").textContent ?? "";
    for (const item of ["你持有该商户的卡", "可用至 2026-12-31", "可退，退回比例 80%", "到店凭卡办理", "前台 0731-0000", "公示至 2026-11-30"]) {
      expect(text).toContain(item);
    }
  });

  it("逐笔记录展示本金与赠送金拆分、充值支付通路与扣款商户", async () => {
    const service = makeService({
      getStoredValueAccounts: vi.fn().mockResolvedValue([projectCard]),
      getStoredValueTransactions: vi.fn().mockResolvedValue([
        { id: "t1", accountId: "svacc_prj", customerId: "c-1", txnType: "topup", amountCent: 5500, balanceAfterCent: 5500, principalCent: 5000, bonusCent: 500, reason: null, createdAt: "2026-10-17T00:00:00Z", paymentChannel: "wechat" },
        { id: "t2", accountId: "svacc_prj", customerId: "c-1", txnType: "consume", amountCent: -5200, balanceAfterCent: 300, principalCent: -5000, bonusCent: -200, reason: null, createdAt: "2026-10-17T01:00:00Z", merchantName: "一号酒吧" },
        { id: "t3", accountId: "svacc_prj", customerId: "c-1", txnType: "adjust", amountCent: -300, balanceAfterCent: 0, principalCent: 0, bonusCent: -300, reason: "销户：未使用赠送金作废（不退现）", createdAt: "2026-10-17T02:00:00Z" },
      ]),
    });

    render(<WalletPage service={service} />);
    await waitFor(() => expect(screen.getByText("综合体通用卡")).toBeInTheDocument());
    expect(await screen.findByText("充值 · ¥55.00")).toBeInTheDocument();
    expect(screen.getByText("本金 ¥50.00 · 赠送金 ¥5.00 · 支付通路：微信")).toBeInTheDocument();
    expect(screen.getByText("消费 · ¥-52.00")).toBeInTheDocument();
    expect(screen.getByText("本金 ¥-50.00 · 赠送金 ¥-2.00 · 扣款商户：一号酒吧")).toBeInTheDocument();
    expect(screen.getByText("调整 · ¥-3.00")).toBeInTheDocument();
    expect(screen.getByText("本金 ¥0.00 · 赠送金 ¥-3.00 · 销户：未使用赠送金作废（不退现）")).toBeInTheDocument();
  });

  // TK-20-R4（BSM「我的余额」：每笔退款与回冲（含回冲顺序结果））：消费退款回原卡的流水显示「退款回冲」，两段金额=先赠送金后本金的回冲结果。
  it("renders consume reversal row with bonus-first split", async () => {
    const service = makeService({
      getStoredValueAccounts: vi.fn().mockResolvedValue([projectCard]),
      getStoredValueTransactions: vi.fn().mockResolvedValue([
        { id: "t1", accountId: "svacc_prj", customerId: "c-1", txnType: "consume", amountCent: -12000, balanceAfterCent: 3000, principalCent: -10000, bonusCent: -2000, reason: null, createdAt: "2026-10-17T00:00:00Z" },
        { id: "t2", accountId: "svacc_prj", customerId: "c-1", txnType: "consume_reversal", amountCent: 3000, balanceAfterCent: 6000, principalCent: 1000, bonusCent: 2000, reason: "酒吧账单退款", createdAt: "2026-10-17T01:00:00Z" },
      ]),
    });

    render(<WalletPage service={service} />);
    await waitFor(() => expect(screen.getByText("退款回冲 · ¥30.00")).toBeInTheDocument());
    expect(screen.getByText("本金 ¥10.00 · 赠送金 ¥20.00 · 酒吧账单退款")).toBeInTheDocument();
  });

  it("shows empty state when the customer has no stored-value cards", async () => {
    render(<WalletPage service={makeService({ getStoredValueAccounts: vi.fn().mockResolvedValue([]) })} />);
    await waitFor(() => expect(screen.getAllByText("暂无储值卡").length).toBeGreaterThan(0));
  });
});
