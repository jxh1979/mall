// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient } from "../types";

describe("M006-027 现场报手机号短信扣款", () => {
  it("按真实账单提交手机号和验证码，不从订单总额重算扣款", async () => {
    const api = {
      get: vi.fn().mockResolvedValue([{ id: "order-1", totalAmountCent: 9000,
        bills: [{ id: "bill-1" }] }]),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<BarOrdersPage api={api as unknown as AdminApiClient}
      user={{ id: "u1", username: "cashier", accountType: "staff" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()}
      effectivePermissions={{ status: "ready", value: {
        grants: ["finance.payment.register"], denies: [], wildcard: false } }} />);
    await screen.findByTestId("bar-order-select-order-1");
    fireEvent.click(screen.getByTestId("bar-order-select-order-1"));
    expect(screen.getByRole("option", { name: "储值余额（手机号验证）" })).toBeTruthy();
    fireEvent.change(screen.getByTestId("bar-settle-channel"), { target: { value: "stored_value" } });
    fireEvent.click(screen.getByTestId("bar-order-settle"));
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("付款手机号"), { target: { value: "13800138000" } });
    fireEvent.change(screen.getByLabelText("短信验证码"), { target: { value: "123456" } });
    fireEvent.click(screen.getByTestId("bar-order-settle"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/settle",
      expect.objectContaining({ paymentChannel: "stored_value", phone: "13800138000", smsCode: "123456", storedValueCard: "merchant" })));
    expect(api.post.mock.calls[0]?.[1]).not.toHaveProperty("settlementAmountCent");
  });

  // 第十八批 BD 段第 367 条 367-R3（BSM:833「消费时按客人指定的某一张卡扣减，不自动跨卡」）：收银按客人所说选扣综合体通用卡。
  it("选综合体通用卡：结账请求体带 storedValueCard general", async () => {
    const api = {
      get: vi.fn().mockResolvedValue([{ id: "order-1", totalAmountCent: 9000,
        bills: [{ id: "bill-1" }] }]),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({}), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<BarOrdersPage api={api as unknown as AdminApiClient}
      user={{ id: "u1", username: "cashier", accountType: "staff" }}
      setActionResult={vi.fn()} setGlobalError={vi.fn()}
      effectivePermissions={{ status: "ready", value: {
        grants: ["finance.payment.register"], denies: [], wildcard: false } }} />);
    await waitFor(() => expect(screen.queryByTestId("bar-order-select-order-1")).not.toBeNull());
    fireEvent.click(screen.getByTestId("bar-order-select-order-1"));
    fireEvent.change(screen.getByTestId("bar-settle-channel"), { target: { value: "stored_value" } });
    await waitFor(() => expect(screen.queryByTestId("bar-settle-stored-value-card")).not.toBeNull());
    expect(screen.getByRole("option", { name: "本酒吧商户卡" })).toBeTruthy();
    fireEvent.change(screen.getByTestId("bar-settle-stored-value-card"), { target: { value: "general" } });
    fireEvent.change(screen.getByLabelText("付款手机号"), { target: { value: "13800138000" } });
    fireEvent.change(screen.getByLabelText("短信验证码"), { target: { value: "123456" } });
    fireEvent.click(screen.getByTestId("bar-order-settle"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/settle",
      expect.objectContaining({ paymentChannel: "stored_value", storedValueCard: "general" })));
  });
});
