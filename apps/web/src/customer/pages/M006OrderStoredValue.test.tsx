// 文件版本：V0.03
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { MyOrdersPage } from "./MyOrdersPage";
import type { CustomerService } from "../client/customerService";

it("M006-027 在订单中选择本人储值卡并提交短信验证，不允许填写扣款金额", async () => {
  const service = {
    listOrders: vi.fn().mockResolvedValue([{ id: "order-1", orderNo: "T1", payStatus: "unpaid",
      issueStatus: "unissued", status: "created", salesChannel: "app", amountCent: 2500, quantity: 1 }]),
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
