// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { DeregisterPage } from "./DeregisterPage";
import type { CustomerService } from "../client/customerService";

// TK-21-R3（BSM「账户注销」：「发起注销时系统须提醒其名下尚有……储值卡余额，客人可以选择……放弃余额，径行注销」）。
describe("DeregisterPage", () => {
  it("shows balances and sends waive flag", async () => {
    const service = {
      getDeregisterPreview: vi.fn().mockResolvedValue([
        { accountId: "svacc_prj", cardScope: "project", merchantId: null, principalCent: 3000, bonusCent: 500 },
        { accountId: "svacc_m1", cardScope: "merchant", merchantId: "m1", principalCent: 2000, bonusCent: 0 },
      ]),
      deregister: vi.fn().mockResolvedValue({ status: "deregistered" }),
    } as unknown as CustomerService;
    const onDeregistered = vi.fn();

    render(<DeregisterPage service={service} onDeregistered={onDeregistered} />);

    // 注销前提醒：按卡分列本金与赠送金。
    await waitFor(() => expect(screen.getByText("本金 ¥30.00 · 赠送金 ¥5.00")).toBeInTheDocument());
    expect(screen.getByText("综合体通用卡")).toBeInTheDocument();
    expect(screen.getByText(/商户卡（m1）/)).toBeInTheDocument();
    expect(screen.getByText("本金 ¥20.00 · 赠送金 ¥0.00")).toBeInTheDocument();

    // 勾选放弃余额后确认注销：请求体带 waiveBalance=true（客户端自动转蛇形 waive_balance）。
    fireEvent.click(screen.getByLabelText("放弃全部余额（归发卡主体）"));
    fireEvent.click(screen.getByRole("button", { name: "确认注销" }));
    await waitFor(() => expect(service.deregister).toHaveBeenCalledWith(true));
    await waitFor(() => expect(onDeregistered).toHaveBeenCalled());
  });

  it("sends waive=false by default so refundable principal becomes pending refund", async () => {
    const service = {
      getDeregisterPreview: vi.fn().mockResolvedValue([
        { accountId: "svacc_prj", cardScope: "project", merchantId: null, principalCent: 3000, bonusCent: 0 },
      ]),
      deregister: vi.fn().mockResolvedValue({ status: "deregistered" }),
    } as unknown as CustomerService;

    render(<DeregisterPage service={service} onDeregistered={vi.fn()} />);

    await waitFor(() => expect(screen.getByText("本金 ¥30.00 · 赠送金 ¥0.00")).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: "确认注销" }));
    await waitFor(() => expect(service.deregister).toHaveBeenCalledWith(false));
  });
});
