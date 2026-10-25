// 文件版本：V0.05
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { MyOrdersPage } from "./MyOrdersPage";
import type { CustomerService } from "@complex/customer-core";

// 我的订单页同屏另读我的票与第三方渠道接入状态，本组用例不涉及，一律空。
const OTHER_READS = { listMyTickets: vi.fn().mockResolvedValue([]), listTicketChannels: vi.fn().mockResolvedValue([]) };

it("M006-027 在订单中选择本人储值卡并提交短信验证，不允许填写扣款金额", async () => {
  const service = {
    ...OTHER_READS,
    listOrders: vi.fn().mockResolvedValue([{ id: "order-1", orderNo: "T1", payStatus: "unpaid",
      issueStatus: "unissued", status: "created", salesChannel: "app", amountCent: 2500, quantity: 1, barId: "m1" }]),
    getStoredValueAccounts: vi.fn().mockResolvedValue([{ id: "card-1", merchantId: "m1",
      projectId: null, cardScope: "merchant", balanceCent: 10000, status: "active" }]),
    requestStoredValueCode: vi.fn().mockResolvedValue({}),
    payTicketOrderWithStoredValue: vi.fn().mockResolvedValue({ id: "order-1", payStatus: "paid" }),
  };
  render(<MyOrdersPage service={service as unknown as CustomerService} />);
  fireEvent.click(await screen.findByRole("button", { name: "储值余额支付" }));
  await screen.findByLabelText("付款手机号");
  fireEvent.change(screen.getByLabelText("付款手机号"), { target: { value: "13800138000" } });
  fireEvent.change(screen.getByLabelText("短信验证码"), { target: { value: "123456" } });
  const submit = await screen.findByRole("button", { name: /确认余额支付/ });
  await waitFor(() => expect(submit).toBeEnabled());
  fireEvent.click(submit);
  await waitFor(() => expect(service.payTicketOrderWithStoredValue).toHaveBeenCalledWith("order-1", {
    accountId: "card-1", phone: "13800138000", smsCode: "123456",
  }));
  expect(screen.queryByRole("spinbutton")).toBeNull();
});

it.each([
  { name: "同综合体项目卡", projectId: "p1", orderProjectId: "p1", merchantId: null, payable: true },
  { name: "其他综合体项目卡", projectId: "p2", orderProjectId: "p1", merchantId: null, payable: false },
  { name: "订单归属未知的项目卡", projectId: "p1", orderProjectId: null, merchantId: null, payable: false },
  { name: "其他商户的储值卡", projectId: null, orderProjectId: "p1", merchantId: "other", payable: false },
])("$name按原订单范围选择，不适用的卡不能付款", async ({ projectId, orderProjectId, merchantId, payable }) => {
  const service = {
    ...OTHER_READS,
    listOrders: vi.fn().mockResolvedValue([{ id: "order-1", orderNo: "T1", payStatus: "unpaid",
      issueStatus: "unissued", status: "created", salesChannel: "app", amountCent: 2500,
      quantity: 1, barId: "m1", projectId: orderProjectId }]),
    getStoredValueAccounts: vi.fn().mockResolvedValue([{ id: "card-1", merchantId,
      projectId, cardScope: merchantId ? "merchant" : "project", balanceCent: 10000, status: "active" }]),
    payTicketOrderWithStoredValue: vi.fn().mockResolvedValue({ id: "order-1", payStatus: "paid" }),
  };
  render(<MyOrdersPage service={service as unknown as CustomerService} />);
  fireEvent.click(await screen.findByRole("button", { name: "储值余额支付" }));
  await screen.findByLabelText("付款手机号");
  await waitFor(() => expect(screen.getByRole("button", { name: "发送扣款验证码" })).toBeEnabled());
  fireEvent.change(screen.getByLabelText("付款手机号"), { target: { value: "13800138000" } });
  fireEvent.change(screen.getByLabelText("短信验证码"), { target: { value: "123456" } });
  const submit = screen.getByRole("button", { name: /确认余额支付/ });
  if (payable) {
    await waitFor(() => expect(submit).toBeEnabled());
    fireEvent.click(submit);
    await waitFor(() => expect(service.payTicketOrderWithStoredValue).toHaveBeenCalledWith("order-1", {
      accountId: "card-1", phone: "13800138000", smsCode: "123456",
    }));
  } else {
    expect(submit).toBeDisabled();
    fireEvent.click(submit);
    expect(service.payTicketOrderWithStoredValue).not.toHaveBeenCalled();
  }
});
