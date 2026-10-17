// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { DealerOrderFulfillmentPage } from "./DealerOrderFulfillmentPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

/** CG-11b（BSM:1226-1234）：十五态、支付方式、到账核验、出库、退款全中文；只提供对公转账与平台线上支付。 */
describe("DealerOrderFulfillmentPage 经销订单与履约（ADM-DLR-002）", () => {
  const orderRow = { id: "dord_1", orderNo: "DO-1", status: "payment_verifying", creditTerm: true, goodsAmountCent: 10000, freightCent: 1000, totalAmountCent: 11000, firstPaymentRequiredCent: 3300, paidAmountCent: 0, overdue: false };
  function makeApi() {
    return {
      get: vi.fn(async (url: string) => {
        if (url === "/api/v1/dealers") return [{ id: "dlr_1", dealerName: "华北经销", status: "approved", creditSuspended: true }];
        if (url.endsWith("/contracts")) return [{ id: "dct_1", contractNo: "DC-1", creditTerm: true, freightTemplateVersion: 2 }];
        if (url.endsWith("/orders")) return [orderRow, { id: "dord_2", orderNo: "DO-2", status: "pending_refund", totalAmountCent: 6000, paidAmountCent: 1000 }];
        if (url.endsWith("/overpayments")) return [{ id: "dovp_1", orderId: "dord_3", amountCent: 2500, status: "unconfirmed" }];
        if (url === "/api/v1/dealers/orders/dord_1") return orderRow;
        if (url.endsWith("/payments")) return [{ id: "dpay_1", method: "bank_transfer", amountCent: 3000, status: "pending_verification" }, { id: "dpay_2", method: "platform_online", amountCent: 500, status: "late" }];
        if (url.endsWith("/refunds")) return [{ id: "drf_1", paymentId: "dpay_2", amountCent: 500, status: "approved", reviewId: "review_1" }];
        return [];
      }),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({ id: "x", orderNo: "DO-NEW", totalAmountCent: 11000, status: "draft" }),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }
  function renderPage(api: AdminApiClient) {
    const setActionResult = vi.fn();
    const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<DealerOrderFulfillmentPage {...props} />);
    return setActionResult;
  }

  it("订单十五态、到账状态与逾期暂停全中文；支付方式只列对公转账与平台线上支付", async () => {
    const api = makeApi();
    renderPage(api);
    await screen.findByTestId("dealer-orders-dealer-table");
    expect(screen.getByTestId("dealer-credit-dlr_1").textContent).toBe("逾期暂停账期订单");
    fireEvent.click(screen.getByTestId("dealer-orders-select-dlr_1"));
    await screen.findByTestId("order-table");
    expect(screen.getByTestId("order-status-dord_1").textContent).toBe("付款核验中");
    expect(screen.getByTestId("order-status-dord_2").textContent).toBe("待退款");
    expect(screen.getByTestId("overpayment-status-dovp_1").textContent).toBe("用途未确认");
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    await screen.findByTestId("payment-table");
    expect(screen.getByTestId("payment-status-dpay_1").textContent).toBe("待核验");
    expect(screen.getByTestId("payment-status-dpay_2").textContent).toBe("取消后迟到（待退款）");
    expect(screen.getByTestId("refund-status-drf_1").textContent).toBe("已批准待执行");
    const methods = Array.from((screen.getByTestId("pay-method") as HTMLSelectElement).querySelectorAll("option")).map((o) => o.textContent);
    expect(methods).toEqual(["对公转账", "平台线上支付"]);
  });

  it("到账核验、退款执行与建单调用对应端点", async () => {
    const api = makeApi();
    const setActionResult = renderPage(api);
    await screen.findByTestId("dealer-orders-dealer-table");
    fireEvent.click(screen.getByTestId("dealer-orders-select-dlr_1"));
    await screen.findByTestId("order-table");
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    await screen.findByTestId("payment-table");
    fireEvent.click(screen.getByTestId("payment-verify-dpay_1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/payments/dpay_1/verify", expect.objectContaining({ decision: "approve" })));
    vi.spyOn(window, "prompt").mockReturnValue("RF-9");
    fireEvent.click(screen.getByTestId("refund-execute-drf_1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/refunds/drf_1/execute", expect.objectContaining({ referenceNo: "RF-9" })));
    fireEvent.change(screen.getByTestId("pay-amount"), { target: { value: "35" } });
    fireEvent.click(screen.getByTestId("pay-register"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/orders/dord_1/payments", expect.objectContaining({ method: "bank_transfer", amountCent: 3500 })));
    fireEvent.change(screen.getByTestId("draft-contract-select"), { target: { value: "dct_1" } });
    fireEvent.change(screen.getByTestId("draft-lines-input"), { target: { value: "item_a,箱,10,\nitem_b,瓶,2,dqi_1" } });
    fireEvent.click(screen.getByTestId("draft-create"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/dlr_1/orders", expect.objectContaining({
      dealerContractId: "dct_1", lines: [{ itemId: "item_a", spec: "箱", quantity: 10, inquiryId: undefined }, { itemId: "item_b", spec: "瓶", quantity: 2, inquiryId: "dqi_1" }],
    })));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "dealer_order_drafted", 订单: "DO-NEW" })));
  });
});
