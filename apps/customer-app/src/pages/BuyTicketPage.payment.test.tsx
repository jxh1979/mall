// 文件版本：V0.01
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { platform } from "../platform";
import { BuyTicketPage } from "./BuyTicketPage";

// 单测里配一个允许的 H5 支付主机（正式支付资料未到，打包产物不带此值）。
vi.mock("../api", async (importOriginal) => ({ ...(await importOriginal<typeof import("../api")>()), CUSTOMER_PAY_HOSTS: ["pay.example.com"] }));

// BSM「多种资格价不叠加，选择最终价格最低的一项」；BSM「购票前须以门票守则的形式向客人告知该票的规则，包括能否退款、有效期、年龄限制与可使用范围」；
// BSM「通过官方 App/小程序购票、预定、确认订单、查看支付结果」；操作员原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」；
// OpenAPI 客人本人订单发起在线付款：渠道未启用或未配置时客户端显示「支付暂不可用，未扣款」。
const PRODUCT = {
  ticketTypeId: "tt-1", ticketName: "夜场单人票", weekdayPriceCent: 10000, weekendPriceCent: 12000, holidayPriceCent: 15000,
  currency: "CNY", status: "published", barId: null, barName: null, myPriceCent: 8000, myPriceBasis: "会员价",
  refundable: false, validityDays: 1, usageScope: "综合体通票", entitlements: ["入场", "赠饮一杯"], ageLimitText: "全场 18 岁以上方可入场",
};
const ORDER = { id: "o-1", orderNo: "TO-1", amountCent: 10000, currency: "CNY", payStatus: "unpaid", issueStatus: "not_issued",
  status: "created", projectId: "p1", barId: null };
const PAID = { ...ORDER, payStatus: "paid", issueStatus: "issued", status: "paid", paidAmountCent: 10000, remainingCent: 0 };
const launchView = (overrides: Record<string, unknown>) => ({ paymentIntentId: "pi-1", businessOrderId: "o-1", amountCent: 10000,
  channel: "wechat", gatewayStatus: "PENDING", paymentUrl: null, launch: null, ...overrides });

async function chooseProduct() {
  await waitFor(() => expect(screen.queryByTestId("buy-ticket-type")).not.toBeNull());
  await waitFor(() => expect(screen.getByTestId("buy-ticket-type")).toBeInTheDocument());
  await userEvent.click(screen.getByTestId("buy-ticket-type").querySelector("input") ?? screen.getByTestId("buy-ticket-type"));
  await waitFor(() => expect(screen.queryByTitle("夜场单人票")).not.toBeNull());
  await waitFor(() => expect(screen.getByTitle("夜场单人票")).toBeTruthy());
  await userEvent.click(screen.getByTitle("夜场单人票"));
}

async function placeOrder(service: Record<string, unknown>) {
  render(<BuyTicketPage service={service as unknown as CustomerService} />);
  await chooseProduct();
  await waitFor(() => expect(screen.queryByTestId("buy-ticket-submit")).not.toBeNull());
  await waitFor(() => expect(screen.getByTestId("buy-ticket-submit")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("buy-ticket-submit"));
  await waitFor(() => expect(screen.queryByTestId("order-payment-remaining")?.textContent ?? "").toContain("¥100.00"));
}

async function clickOnlinePay() {
  await waitFor(() => expect(screen.queryByTestId("order-pay-online")).not.toBeNull());
  await waitFor(() => expect(screen.getByTestId("order-pay-online")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("order-pay-online"));
}

describe("BuyTicketPage 我的价格、门票守则与组合支付", () => {
  it("showsMyLowestPriceAndTicketRulesBeforeOrder：选票后先展示系统取的最低资格价与门票守则", async () => {
    const service = { listTicketProducts: vi.fn().mockResolvedValue([PRODUCT]) };
    render(<BuyTicketPage service={service as unknown as CustomerService} />);
    await chooseProduct();
    await waitFor(() => expect(screen.queryByTestId("buy-ticket-my-price")?.textContent ?? "").toContain("¥80.00"));
    expect(screen.getByTestId("buy-ticket-my-price").textContent).toContain("会员价");
    const rules = screen.getByTestId("ticket-rules").textContent ?? "";
    for (const text of ["售出后不退款", "1 天", "综合体通票", "入场、赠饮一杯", "全场 18 岁以上方可入场"]) {
      expect(rules).toContain(text);
    }
  });

  it("onlinePaymentUnavailableShowsNotCharged：在线支付未配置时显示「支付暂不可用，未扣款」，不显示成功、不走旧支付意向接口", async () => {
    const service = {
      listTicketProducts: vi.fn().mockResolvedValue([PRODUCT]),
      createTicketOrder: vi.fn().mockResolvedValue(ORDER),
      createPaymentIntent: vi.fn(),
      startTicketPayment: vi.fn().mockResolvedValue(launchView({ gatewayStatus: "NOT_CONFIGURED" })),
      confirmTicketPayment: vi.fn(),
    };
    await placeOrder(service);
    await clickOnlinePay();
    await waitFor(() => expect(screen.queryByTestId("order-payment-unavailable")?.textContent ?? "").toContain("支付暂不可用，未扣款"));
    expect(service.startTicketPayment).toHaveBeenCalledWith("o-1");
    expect(service.confirmTicketPayment).not.toHaveBeenCalled();
    expect(service.createPaymentIntent).not.toHaveBeenCalled();
    expect(screen.queryByTestId("buy-ticket-paid")).toBeNull();
  });

  it("storedValuePartThenOnlineTopUpCompletesOnlyWhenFullyPaid：储值先扣卡内余额、在线补足，整单付清才显示支付成功", async () => {
    const service = {
      listTicketProducts: vi.fn().mockResolvedValue([PRODUCT]),
      createTicketOrder: vi.fn().mockResolvedValue(ORDER),
      getStoredValueAccounts: vi.fn().mockResolvedValue([{ id: "card-1", customerId: "c-1", projectId: "p1", merchantId: null,
        cardScope: "project", balanceCent: 3000, principalCent: 3000, bonusCent: 0, status: "active" }]),
      requestStoredValueCode: vi.fn().mockResolvedValue({}),
      payTicketOrderWithStoredValue: vi.fn().mockResolvedValue({ ...ORDER, paidAmountCent: 3000, remainingCent: 7000 }),
      startTicketPayment: vi.fn().mockResolvedValue(launchView({ paymentIntentId: "pi-2", amountCent: 7000, gatewayStatus: "SUCCEEDED" })),
      confirmTicketPayment: vi.fn().mockResolvedValue(PAID),
    };
    await placeOrder(service);
    await waitFor(() => expect(screen.queryByRole("button", { name: "储值余额支付" })).not.toBeNull());
    await waitFor(() => expect(screen.getByRole("button", { name: "储值余额支付" })).not.toBeDisabled());
    fireEvent.click(screen.getByRole("button", { name: "储值余额支付" }));
    await waitFor(() => expect(screen.queryByLabelText("付款手机号")).not.toBeNull());
    await waitFor(() => expect(screen.getByLabelText("付款手机号")).toBeInTheDocument());
    await waitFor(() => expect(screen.queryByTestId("order-payment-stored-value-amount")?.textContent ?? "").toContain("¥30.00"));
    fireEvent.change(screen.getByLabelText("付款手机号"), { target: { value: "13800138000" } });
    fireEvent.change(screen.getByLabelText("短信验证码"), { target: { value: "123456" } });
    await waitFor(() => expect(screen.queryByRole("button", { name: /确认余额支付/ })).not.toBeNull());
    await waitFor(() => expect(screen.getByRole("button", { name: /确认余额支付/ })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: /确认余额支付/ }));
    await waitFor(() => expect(service.payTicketOrderWithStoredValue).toHaveBeenCalledWith("o-1", {
      accountId: "card-1", phone: "13800138000", smsCode: "123456", amountCent: 3000,
    }));
    await waitFor(() => expect(screen.queryByTestId("order-payment-remaining")?.textContent ?? "").toContain("¥70.00"));
    expect(screen.queryByTestId("buy-ticket-paid")).toBeNull();

    await clickOnlinePay();
    await waitFor(() => expect(screen.queryByTestId("buy-ticket-paid")?.textContent ?? "").toContain("支付成功"));
    expect(service.confirmTicketPayment).toHaveBeenCalledWith("o-1", "pi-2");
  });

  it("h5PaymentOpensOutsideAppAndConfirmsWhenBackToForeground：H5 付款页交系统打开，回到前台即查单，渠道确认收款才成功", async () => {
    let resume: () => void = () => undefined;
    vi.spyOn(platform, "onResume").mockImplementation((listener) => {
      resume = listener;
      return () => undefined;
    });
    const openPayment = vi.spyOn(platform, "openPayment").mockResolvedValue(true);
    const service = {
      listTicketProducts: vi.fn().mockResolvedValue([PRODUCT]),
      createTicketOrder: vi.fn().mockResolvedValue(ORDER),
      startTicketPayment: vi.fn().mockResolvedValue(launchView({ paymentIntentId: "pi-3",
        launch: { kind: "h5_url", params: { url: "https://pay.example.com/cashier?no=1" } } })),
      confirmTicketPayment: vi.fn().mockResolvedValue(PAID),
    };
    await placeOrder(service);
    await clickOnlinePay();
    await waitFor(() => expect(openPayment).toHaveBeenCalledWith("https://pay.example.com/cashier?no=1"));
    expect(service.confirmTicketPayment).not.toHaveBeenCalled();
    expect(screen.queryByTestId("buy-ticket-paid")).toBeNull();

    act(() => resume());
    await waitFor(() => expect(service.confirmTicketPayment).toHaveBeenCalledWith("o-1", "pi-3"));
    await waitFor(() => expect(screen.queryByTestId("buy-ticket-paid")?.textContent ?? "").toContain("支付成功"));
  });
});
