// 文件版本：V0.01
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { platform } from "../platform";
import { BarReservationPage } from "./BarReservationPage";

vi.mock("../api", async (importOriginal) => ({ ...(await importOriginal<typeof import("../api")>()), CUSTOMER_PAY_HOSTS: ["pay.example.com"] }));

// BSM「官方 App 和小程序都可以订台，订台需要支付定金；订台页面须写明该卡座适用的最低消费金额与服务费比例」；
// BSM「客人自助订台必须支付定金……每一笔订台都必须填写最晚到店时间」；ENUM「一个台位同一时刻只能存在一笔有效预留」；
// 13B 复核：酒吧未配置定金规则时客人自助订台明确不可用（OpenAPI 409 BAR_SELF_RESERVATION_UNAVAILABLE，选项 deposit_amount_cent=0）。
const OPTION = { barId: "bar-1", tableId: "t-a", tableType: "card_seat", tableName: "A01", capacity: 6, allowedPassFreeGuestCount: 2,
  minConsumptionCent: 100000, currency: "CNY", status: "active", reserved: false, serviceFeeRateBps: 1000, depositAmountCent: 20000 };
const RESERVED = { ...OPTION, tableId: "t-b", tableName: "A02", reserved: true };
const NO_RULE = { ...OPTION, barId: "bar-2", tableId: "t-c", tableName: "B01", depositAmountCent: 0 };
const RESERVATION = { id: "res-1", barId: "bar-1", tableId: "t-a", customerId: "c-1", reservedAt: "2026-10-25T12:00:00Z", guestCount: 4,
  depositAmountCent: 20000, depositStatus: "pending", orderSource: "app", status: "pending", depositIntentId: null, createdAt: "" };
const launchView = (overrides: Record<string, unknown>) => ({ paymentIntentId: "pi-1", businessOrderId: "res-1", amountCent: 20000,
  channel: "wechat", gatewayStatus: "PENDING", paymentUrl: null, launch: null, ...overrides });

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    listBarReservationOptions: vi.fn().mockResolvedValue([OPTION, RESERVED, NO_RULE]),
    createBarReservation: vi.fn().mockResolvedValue(RESERVATION),
    startDepositPayment: vi.fn().mockResolvedValue(launchView({ gatewayStatus: "NOT_CONFIGURED" })),
    confirmDepositPayment: vi.fn().mockResolvedValue({ ...RESERVATION, depositStatus: "paid" }),
    ...overrides,
  };
}

async function submitReservation(service: ReturnType<typeof makeService>) {
  render(<BarReservationPage service={service as unknown as CustomerService} />);
  await waitFor(() => expect(screen.queryByTestId("reservation-pick-t-a")).not.toBeNull());
  await waitFor(() => expect(screen.getByTestId("reservation-pick-t-a")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("reservation-pick-t-a"));
  await waitFor(() => expect(screen.queryByLabelText("到店时间")).not.toBeNull());
  fireEvent.change(screen.getByLabelText("到店时间"), { target: { value: "2026-10-25T20:00" } });
  fireEvent.change(screen.getByLabelText("最晚到店时间"), { target: { value: "2026-10-25T21:00" } });
  fireEvent.change(screen.getByLabelText("人数"), { target: { value: "4" } });
  await waitFor(() => expect(screen.getByTestId("reservation-submit")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("reservation-submit"));
  await waitFor(() => expect(screen.queryByTestId("reservation-result")?.textContent ?? "").toContain("待支付定金 ¥200.00"));
}

async function clickDepositPay() {
  await waitFor(() => expect(screen.queryByTestId("deposit-pay")).not.toBeNull());
  await waitFor(() => expect(screen.getByTestId("deposit-pay")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("deposit-pay"));
}

describe("BarReservationPage 订卡规则、定金与不可自助订台", () => {
  it("showsMinConsumptionServiceFeeAndDeposit：写明最低消费、服务费比例与定金；已预留与未配定金规则的不可订", async () => {
    render(<BarReservationPage service={makeService() as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("reservation-option-t-a")?.textContent ?? "").toContain("最低消费 ¥1000.00"));
    const option = screen.getByTestId("reservation-option-t-a").textContent ?? "";
    for (const text of ["卡座", "服务费 10%", "定金 ¥200.00", "可进 6 人"]) expect(option).toContain(text);
    expect(screen.getByTestId("reservation-option-t-b").textContent).toContain("已预留");
    expect(screen.getByTestId("reservation-pick-t-b")).toBeDisabled();
    expect(screen.getByTestId("reservation-option-t-c").textContent).toContain("该酒吧暂未开放自助订台");
    expect(screen.getByTestId("reservation-pick-t-c")).toBeDisabled();
    expect(document.body.textContent).not.toContain("card_seat");
  });

  it("paymentUnavailableShowsNoSuccess：下单后付定金走本人订台定金付款；渠道未配置显示「支付暂不可用，未扣款」、不显示预订成功", async () => {
    const service = makeService();
    await submitReservation(service);
    expect(service.createBarReservation).toHaveBeenCalledWith(expect.objectContaining({
      barId: "bar-1", tableId: "t-a", tableType: "card_seat", guestCount: 4,
      reservationTime: new Date("2026-10-25T20:00").toISOString(), latestArrivalAt: new Date("2026-10-25T21:00").toISOString(),
    }));
    await clickDepositPay();
    await waitFor(() => expect(screen.queryByTestId("deposit-unavailable")?.textContent ?? "").toContain("支付暂不可用，未扣款"));
    expect(service.startDepositPayment).toHaveBeenCalledWith("res-1");
    expect(service.confirmDepositPayment).not.toHaveBeenCalled();
    expect(screen.queryByTestId("reservation-paid")).toBeNull();
  });

  it("depositConfirmedOnResumeShowsReserved：H5 付款页交系统打开，回前台查单确认收款才显示定金已付", async () => {
    let resume: () => void = () => undefined;
    vi.spyOn(platform, "onResume").mockImplementation((listener) => {
      resume = listener;
      return () => undefined;
    });
    const openPayment = vi.spyOn(platform, "openPayment").mockResolvedValue(true);
    const service = makeService({ startDepositPayment: vi.fn().mockResolvedValue(launchView({
      launch: { kind: "h5_url", params: { url: "https://pay.example.com/cashier?no=9" } } })) });
    await submitReservation(service);
    await clickDepositPay();
    await waitFor(() => expect(openPayment).toHaveBeenCalledWith("https://pay.example.com/cashier?no=9"));
    expect(screen.queryByTestId("reservation-paid")).toBeNull();
    act(() => resume());
    await waitFor(() => expect(service.confirmDepositPayment).toHaveBeenCalledWith("res-1", "pi-1"));
    await waitFor(() => expect(screen.queryByTestId("reservation-paid")?.textContent ?? "").toContain("定金已支付"));
    expect(screen.getByTestId("reservation-paid").textContent).toContain("邀请函由酒吧确认后签发");
  });

  it("selfReservationUnavailableErrorShown：服务端拒绝自助订台时原样提示、不进入付定金", async () => {
    const service = makeService({ createBarReservation: vi.fn().mockRejectedValue(new Error("该酒吧暂未开放自助订台")) });
    render(<BarReservationPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("reservation-pick-t-a")).not.toBeNull());
    fireEvent.click(screen.getByTestId("reservation-pick-t-a"));
    await waitFor(() => expect(screen.queryByLabelText("到店时间")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("到店时间"), { target: { value: "2026-10-25T20:00" } });
    fireEvent.change(screen.getByLabelText("最晚到店时间"), { target: { value: "2026-10-25T21:00" } });
    await waitFor(() => expect(screen.getByTestId("reservation-submit")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("reservation-submit"));
    await waitFor(() => expect(screen.queryByTestId("reservation-error")?.textContent ?? "").toContain("该酒吧暂未开放自助订台"));
    expect(screen.queryByTestId("deposit-pay")).toBeNull();
  });

  // BSM L858「凡与客人有关的规则一律在其消费之前告知……订卡时在订卡规则中写明」；BSM L889「定金不抵扣消费、不计入最低消费累计；
  // 客人在该台位的消费达到该台位适用的最低消费后，定金原路退回；到店未达最低消费、爽约或超时取消的，按既有规则没收」。
  it("depositRulesToldBeforeReserving：订台表单写明定金不抵扣消费、达到最低消费原路退回、未达没收", async () => {
    render(<BarReservationPage service={makeService() as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("reservation-pick-t-a")).not.toBeNull());
    fireEvent.click(screen.getByTestId("reservation-pick-t-a"));
    await waitFor(() => expect(screen.queryByTestId("reservation-deposit-rules")?.textContent ?? "").toContain("定金不抵扣消费、不计入最低消费累计"));
    const rules = screen.getByTestId("reservation-deposit-rules").textContent;
    expect(rules).toContain("在该台位的消费达到该台位适用的最低消费后，定金原路退回");
    expect(rules).toContain("到店未达最低消费、爽约或超时取消的，定金按规则没收");
  });
});
