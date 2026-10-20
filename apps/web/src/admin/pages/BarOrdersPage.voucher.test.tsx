// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BarOrdersPage } from "./BarOrdersPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十批 TK-28（ENUM L81「商户折扣券——到对应商户抵扣」；操作员裁决「一次结账一张」）：
// 结账弹窗可填一张折扣券码，随结账请求体 voucherCode 提交（adminClient.mapKeys 转蛇形 voucher_code）；结果回显抵扣额。
describe("酒吧结账带折扣券", () => {
  afterEach(() => vi.restoreAllMocks());

  it("填券码后结账请求体含 voucherCode，成功后回显抵扣额", async () => {
    const post = vi.fn().mockResolvedValue({ id: "bill-1", discountAmountCent: 5000, payableAmountCent: 25000, settlementStatus: "settled" });
    const api = {
      get: vi.fn(async (url: string) => (url === "/api/v1/bar/orders"
        ? [{ id: "ord-1", tableId: "T-1", totalAmountCent: 30000, status: "ordered", createdAt: "2026-07-05T10:00:00Z", bills: [{ id: "bill-1", status: "unsettled" }] }]
        : [])),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post, put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const setActionResult = vi.fn();
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult, setGlobalError: vi.fn(),
      effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
    };
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<BarOrdersPage {...props} />);

    await waitFor(() => expect(screen.getByTestId("bar-order-select-ord-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("bar-order-select-ord-1"));
    fireEvent.change(screen.getByLabelText("本次付款凭据编号"), { target: { value: "pay-1" } });
    expect(screen.queryByTestId("bar-settle-voucher-code")).toBeTruthy();
    fireEvent.change(screen.getByTestId("bar-settle-voucher-code"), { target: { value: "VCAAA" } });
    fireEvent.click(screen.getByTestId("bar-order-settle"));

    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/bar/bills/bill-1/settle",
      expect.objectContaining({ voucherCode: "VCAAA", paymentId: "pay-1", paymentChannel: "aggregate_pay" })));
    await waitFor(() => expect(screen.getByTestId("bar-settle-voucher-result").textContent).toContain("50.00"));
    expect((screen.getByTestId("bar-settle-voucher-code") as HTMLInputElement).value).toBe("");
  });
});
