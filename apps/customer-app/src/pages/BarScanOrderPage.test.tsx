// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { platform } from "../platform";
import { BarScanOrderPage } from "./BarScanOrderPage";

// BSM「扫码点单只适用于散台……开台后客人扫描桌台二维码即可自助点单，下单时即时付款、付款成功订单才成立，不挂账……付款成功后所点酒水
// 计入该桌位当前账单，账单上逐项列出物品名称与价格，并分别标注已上菜与未上菜」；ENUM「沽清商品在客人扫码点单菜单上置灰展示为已售完，不隐藏」；
// ENUM「非会员可以浏览、点单和付款」。
const MENU = [
  { productId: "p1", productName: "莫吉托", productType: "drink", priceCent: 3800, currency: "CNY", soldOut: false },
  { productId: "p2", productName: "长岛冰茶", productType: "drink", priceCent: 5800, currency: "CNY", soldOut: true },
];
// 本桌账单响应 {table_id, items, total_cent} 经统一信封解包只留 items。
const BILL = [
  { productName: "莫吉托", unitPriceCent: 3800, quantity: 2, amountCent: 7600, served: true },
  { productName: "长岛冰茶", unitPriceCent: 5800, quantity: 1, amountCent: 5800, served: false },
];
const order = (overrides: Record<string, unknown>) => ({ scanOrderId: "so-1", tableId: "tbl-1", amountCent: 7600, status: "pending",
  paymentIntentId: "pi-1", channel: "wechat", gatewayStatus: "PENDING", paymentUrl: null, launch: null, ...overrides });

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    listBarScanMenu: vi.fn().mockResolvedValue(MENU),
    getBarTableCurrentBill: vi.fn().mockResolvedValue([]),
    placeBarScanOrder: vi.fn().mockResolvedValue(order({ gatewayStatus: "NOT_CONFIGURED" })),
    confirmBarScanOrder: vi.fn().mockResolvedValue(order({ status: "paid", barOrderId: "bo-1" })),
    ...overrides,
  };
}

async function addTwoAndPay() {
  await waitFor(() => expect(screen.queryByTestId("scan-add-p1")).not.toBeNull());
  await waitFor(() => expect(screen.getByTestId("scan-add-p1")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("scan-add-p1"));
  fireEvent.click(screen.getByTestId("scan-add-p1"));
  await waitFor(() => expect(screen.getByTestId("scan-pay").textContent).toContain("¥76.00"));
  await waitFor(() => expect(screen.getByTestId("scan-pay")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("scan-pay"));
}

describe("BarScanOrderPage 扫桌台码点单、即时付款与本桌账单", () => {
  it("scanTableLinkOpensMenu：扫散台桌台码进入该桌菜单；沽清的置灰写已售完、不能加", async () => {
    vi.spyOn(platform, "scanCode").mockResolvedValue("https://c.complex.test/c/table?table_id=tbl-9");
    const service = makeService();
    render(<BarScanOrderPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("scan-start")).not.toBeNull());
    fireEvent.click(screen.getByTestId("scan-start"));
    await waitFor(() => expect(service.listBarScanMenu).toHaveBeenCalledWith("tbl-9"));
    await waitFor(() => expect(screen.queryByTestId("scan-item-p2")?.textContent ?? "").toContain("已售完"));
    expect(screen.getByTestId("scan-add-p2")).toBeDisabled();
    expect(screen.queryByPlaceholderText(/桌台编号/)).toBeNull();
    expect(document.body.textContent).not.toContain("drink");
  });

  it("scanForeignUrlRejected：扫到非本平台桌台码提示无法识别，不加载菜单", async () => {
    vi.spyOn(platform, "scanCode").mockResolvedValue("https://evil.example/c/table?table_id=tbl-9");
    const service = makeService();
    render(<BarScanOrderPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("scan-start")).not.toBeNull());
    fireEvent.click(screen.getByTestId("scan-start"));
    await waitFor(() => expect(screen.queryByTestId("scan-error")?.textContent ?? "").toContain("无法识别的二维码"));
    expect(service.listBarScanMenu).not.toHaveBeenCalled();
  });

  it("paymentUnavailableNoOrder：支付渠道未配置时显示「支付暂不可用，未扣款」，不成单、不查单", async () => {
    const service = makeService();
    render(<BarScanOrderPage service={service as unknown as CustomerService} initialTableId="tbl-1" />);
    await addTwoAndPay();
    await waitFor(() => expect(screen.queryByTestId("scan-unavailable")?.textContent ?? "").toContain("支付暂不可用，未扣款"));
    expect(service.placeBarScanOrder).toHaveBeenCalledWith("tbl-1", [{ productId: "p1", quantity: 2 }]);
    expect(service.confirmBarScanOrder).not.toHaveBeenCalled();
    expect(screen.queryByTestId("scan-paid")).toBeNull();
  });

  it("paidShowsCurrentBill：渠道确认收款才成单，随即显示本桌当前账单（逐项、已上菜与未上菜）", async () => {
    const getBill = vi.fn().mockResolvedValueOnce([]).mockResolvedValue(BILL);
    const service = makeService({ placeBarScanOrder: vi.fn().mockResolvedValue(order({ gatewayStatus: "SUCCEEDED" })),
      getBarTableCurrentBill: getBill });
    render(<BarScanOrderPage service={service as unknown as CustomerService} initialTableId="tbl-1" />);
    await addTwoAndPay();
    await waitFor(() => expect(service.confirmBarScanOrder).toHaveBeenCalledWith("tbl-1", "so-1"));
    await waitFor(() => expect(screen.queryByTestId("scan-paid")?.textContent ?? "").toContain("付款成功"));
    await waitFor(() => expect(screen.queryByTestId("scan-bill")?.textContent ?? "").toContain("莫吉托"));
    const bill = screen.getByTestId("scan-bill").textContent ?? "";
    for (const text of ["¥38.00 × 2", "已上菜", "长岛冰茶", "未上菜", "合计 ¥134.00"]) expect(bill).toContain(text);
  });
});
