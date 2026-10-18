// 文件版本：V0.03
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
        if (url.endsWith("/overpayments")) return [{ id: "dovp_1", orderId: "dord_3", amountCent: 2500, status: "unconfirmed", unconfirmedAmountCent: 2500, prepaymentBalanceCent: 0, appliedAmountCent: 0 }];
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
    expect(screen.getByTestId("payment-status-dpay_2").textContent).toBe("取消后到账待核实");
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
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/refunds/drf_1/execute", expect.objectContaining({ idempotencyKey: expect.any(String) })));
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
  it("超收按本次金额和真实双方凭证提交，可使用已有预付款", async () => {
    const api = makeApi();
    const original = api.get;
    api.get = vi.fn(async (url: string) => url.endsWith("/overpayments")
      ? [{ id: "dovp_1", orderId: "dord_3", amountCent: 500000, status: "prepayment", unconfirmedAmountCent: 0, prepaymentBalanceCent: 500000, appliedAmountCent: 0 }]
      : original(url)) as AdminApiClient["get"];
    api.postForm = vi.fn().mockResolvedValue({ id: "att_confirm_1" });
    renderPage(api);
    fireEvent.click(await screen.findByTestId("dealer-orders-select-dlr_1"));
    fireEvent.click(await screen.findByTestId("overpayment-apply-dovp_1"));
    fireEvent.change(screen.getByTestId("allocation-amount"), { target: { value: "3000" } });
    fireEvent.change(screen.getByTestId("allocation-target"), { target: { value: "dord_target" } });
    fireEvent.change(screen.getByTestId("allocation-note"), { target: { value: "双方确认抵乙订单" } });
    fireEvent.click(screen.getByTestId("allocation-save"));
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("allocation-evidence"), { target: { files: [new File(["image"], "双方确认.png", { type: "image/png" })] } });
    await screen.findByText("双方确认.png");
    const form = vi.mocked(api.postForm).mock.calls[0]![1];
    expect(form.get("business_type")).toBe("dealer_overpayment");
    expect(form.get("business_id")).toBe("dovp_1");
    fireEvent.click(screen.getByTestId("allocation-save"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/overpayments/dovp_1/use",
      expect.objectContaining({ usage: "prepayment_offset", amountCent: 300000, targetOrderId: "dord_target", confirmationAttachmentIds: ["att_confirm_1"] })));
  });

  it("取消后待核实的原到账仍有核验入口，交财务不提示已退款", async () => {
    const api = makeApi(); const result = renderPage(api);
    fireEvent.click(await screen.findByTestId("dealer-orders-select-dlr_1"));
    fireEvent.click(await screen.findByTestId("order-select-dord_1"));
    fireEvent.click(await screen.findByTestId("payment-verify-dpay_2"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/payments/dpay_2/verify", expect.objectContaining({ decision: "approve" })));
    fireEvent.click(screen.getByTestId("refund-execute-drf_1"));
    await waitFor(() => expect(result).toHaveBeenCalledWith(expect.objectContaining({ 状态: "已交财务待办理，尚未结清" })));
    expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/refunds/drf_1/execute", expect.not.objectContaining({ referenceNo: expect.anything() }));
  });

  it("合同起点须显式选择，可登记签约后首付和签收后尾款", async () => {
    const api = makeApi(); renderPage(api);
    fireEvent.click(await screen.findByTestId("dealer-orders-select-dlr_1"));
    await screen.findByTestId("terms-contract-select");
    fireEvent.change(screen.getByTestId("terms-contract-select"), { target: { value: "dct_1" } });
    fireEvent.click(screen.getByTestId("terms-save"));
    await screen.findByText("首尾款起点须按合同明确选择");
    expect(api.post).not.toHaveBeenCalled();
    fireEvent.change(screen.getByTestId("terms-first-anchor"), { target: { value: "contract_signed" } });
    fireEvent.change(screen.getByTestId("terms-balance-anchor"), { target: { value: "signed" } });
    fireEvent.change(screen.getByTestId("terms-signed-at"), { target: { value: "2026-10-17T10:00" } });
    fireEvent.change(screen.getByTestId("terms-ratio"), { target: { value: "30" } });
    fireEvent.change(screen.getByTestId("terms-deadline-days"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("terms-balance-days"), { target: { value: "30" } });
    fireEvent.click(screen.getByTestId("terms-save"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/contracts/dct_1/terms", expect.objectContaining({
      firstPaymentAnchor: "contract_signed", balanceDueAnchor: "signed", firstPaymentDeadlineDays: 3, balanceDueDays: 30,
      contractSignedAt: new Date("2026-10-17T10:00").toISOString(), firstPaymentRatioBps: 3000,
    })));
  });

  it("部分退货显示净应收并保留尾款登记", async () => {
    const api = makeApi(); const oldGet = api.get;
    const returned = { ...orderRow, status: "partially_returned", totalAmountCent: 10000, paidAmountCent: 3000, returnedAmountCent: 2000 };
    api.get = vi.fn(async (url: string) => url.endsWith("/orders") ? [returned] : url === "/api/v1/dealers/orders/dord_1" ? returned : oldGet(url)) as AdminApiClient["get"];
    renderPage(api);
    fireEvent.click(await screen.findByTestId("dealer-orders-select-dlr_1"));
    await waitFor(() => expect(screen.getByTestId("order-unpaid-dord_1").textContent).toBe("¥50"));
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    await screen.findByTestId("pay-register");
    fireEvent.change(screen.getByTestId("pay-amount"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("pay-register"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/orders/dord_1/payments", expect.objectContaining({ amountCent: 5000 })));
  });

});
