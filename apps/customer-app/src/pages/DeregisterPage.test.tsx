// 文件版本：V0.03
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { DeregisterPage } from "./DeregisterPage";
import type { CustomerService } from "@complex/customer-core";

// TK-21-R3 / ROOT-03-R21（BSM「发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销；放弃的存酒按存酒到期
// 规则转为所在酒吧的自有库存资产」）。
const BOTTLE = { id: "sb-1", barProjectId: "bar-1", barName: "一号酒吧", bottleName: "轩尼诗 VSOP", remainingQuantity: 1, unit: "瓶",
  expireAt: "2026-12-31T12:00:00Z", status: "active", bottleLabelValue: null, pickupLocation: null, handedOverToPlatform: false };

function preview(overrides: Record<string, unknown> = {}) {
  return { storedValueCards: [], savedBottles: [], ...overrides };
}

describe("DeregisterPage", () => {
  it("shows balances and sends waive flag", async () => {
    const service = {
      getDeregisterPreview: vi.fn().mockResolvedValue(preview({
        storedValueCards: [
          { accountId: "svacc_prj", cardScope: "project", merchantId: null, principalCent: 3000, bonusCent: 500 },
          { accountId: "svacc_m1", cardScope: "merchant", merchantId: "m1", principalCent: 2000, bonusCent: 0 },
        ],
      })),
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
    await waitFor(() => expect(service.deregister).toHaveBeenCalledWith({ waiveBalance: true, abandonSavedBottles: false }));
    await waitFor(() => expect(onDeregistered).toHaveBeenCalled());
  });

  it("sends waive=false by default so refundable principal becomes pending refund", async () => {
    const service = {
      getDeregisterPreview: vi.fn().mockResolvedValue(preview({
        storedValueCards: [{ accountId: "svacc_prj", cardScope: "project", merchantId: null, principalCent: 3000, bonusCent: 0 }],
      })),
      deregister: vi.fn().mockResolvedValue({ status: "deregistered" }),
    } as unknown as CustomerService;

    render(<DeregisterPage service={service} onDeregistered={vi.fn()} />);

    await waitFor(() => expect(screen.getByText("本金 ¥30.00 · 赠送金 ¥0.00")).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button", { name: "确认注销" }));
    await waitFor(() => expect(service.deregister).toHaveBeenCalledWith({ waiveBalance: false, abandonSavedBottles: false }));
  });

  it("deregisterRemindsSavedBottles：逐条提醒名下存酒（所属酒吧、到期日），须明确确认放弃存酒才可注销", async () => {
    const service = {
      getDeregisterPreview: vi.fn().mockResolvedValue(preview({ savedBottles: [BOTTLE] })),
      deregister: vi.fn().mockResolvedValue({ status: "deregistered" }),
    } as unknown as CustomerService;
    render(<DeregisterPage service={service} onDeregistered={vi.fn()} />);
    await waitFor(() => expect(screen.queryByTestId("deregister-bottles")?.textContent ?? "").toContain("一号酒吧"));
    const text = screen.getByTestId("deregister-bottles").textContent ?? "";
    for (const item of ["轩尼诗 VSOP", "1 瓶", "到期 2026-12-31"]) expect(text).toContain(item);
    expect(screen.getByRole("button", { name: "确认注销" })).toBeDisabled();
    fireEvent.click(screen.getByLabelText("放弃名下全部存酒（转为所在酒吧的自有库存）"));
    await waitFor(() => expect(screen.getByRole("button", { name: "确认注销" })).not.toBeDisabled());
    fireEvent.click(screen.getByRole("button", { name: "确认注销" }));
    await waitFor(() => expect(service.deregister).toHaveBeenCalledWith({ waiveBalance: false, abandonSavedBottles: true }));
  });
});
