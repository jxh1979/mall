// 文件版本：V0.05
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import DealerPortalApp from "./DealerPortalApp";
import type { DealerPortalApi } from "./dealerPortalClient";
import { DealerPortalApiError } from "./dealerPortalClient";

/** CG-11d：独立经销商门户——登录后只见本企业；库存外显四态无数量；询价 / 下单 / 付款 / 签收 / 退货调用门户端点；停用即退出。 */
describe("DealerPortalApp 独立经销商 Web 门户", () => {
  const dealer = { id: "dlr_1", dealerName: "华北经销", status: "approved", portalEnabled: true, legalRepresentative: "法人", contactName: "联系人", contactPhone: "139" };
  function makeApi(overrides: Partial<Record<string, unknown>> = {}) {
    let loggedIn = false;
    const api = {
      onSessionExpired: null,
      login: vi.fn(async () => { loggedIn = true; return { id: "usr_1", username: "dealer_1", accountType: "dealer" }; }),
      logout: vi.fn(async () => undefined),
      get: vi.fn(async (url: string) => {
        if (url === "/api/v1/dealer-portal/me") {
          if (!loggedIn) throw new DealerPortalApiError("需要登录", 401, "UNAUTHENTICATED");
          return overrides.me ?? dealer;
        }
        if (url === "/api/v1/dealer-portal/contracts") return [{ id: "dct_1", contractNo: "DC-1", validFrom: "2026-01-01", validTo: "2026-12-31", creditTerm: false }];
        if (url === "/api/v1/dealer-portal/inquiries") return [{ id: "dqi_1", itemId: "item_b", spec: "箱", quantity: 5, status: "quote_approval" }];
        if (url === "/api/v1/dealer-portal/orders" && overrides.order) return [overrides.order];
        if (url === "/api/v1/dealer-portal/orders") return [{ id: "dord_1", orderNo: "DO-1", status: "shipping", totalAmountCent: 5000, paidAmountCent: 5000 }];
        if (url === "/api/v1/dealer-portal/contracts/dct_1/external-stock") return [{ itemId: "item_a", itemName: "威士忌", spec: "箱", unitPriceCent: 1000, availability: "low" }, { itemId: "item_c", itemName: "红酒", spec: "瓶", unitPriceCent: 500, availability: "expected_restock", expectedRestockDate: "2026-11-01" }];
        if (url === "/api/v1/dealer-portal/orders/dord_1" && overrides.order) return overrides.order;
        if (url === "/api/v1/dealer-portal/orders/dord_1") return { id: "dord_1", orderNo: "DO-1", status: "shipping", totalAmountCent: 5000, paidAmountCent: 5000 };
        if (url.endsWith("/delivery")) return { netReceivableCent: 5000, receivedCent: 5000, pendingRefundCent: 0, moneyStatus: "settled", batches: [{ id: "dord_1", shippedAt: "2026-10-01T00:00:00Z", lines: [{ orderLineId: "line_1", quantity: 5, recordedQuantity: 0, acceptedQuantity: 0 }] }], receipts: [], discrepancies: [] };
        if (url.endsWith("/lines")) return [{ id: "line_1", itemId: "item_a", spec: "箱" }];
        if (url.endsWith("/payments")) return [{ id: "dpay_1", method: "bank_transfer", amountCent: 5000, status: "verified", referenceNo: "TX-1" }];
        if (url.endsWith("/return-sources")) return overrides.sources ?? [];
        if (url.endsWith("/return-financials")) return overrides.financials ?? [];
        if (url.endsWith("/invoices")) return overrides.invoices ?? [];
        if (url.endsWith("/returns")) return overrides.returns ?? [];
        return [];
      }),
      post: vi.fn(async () => ({ id: "dord_new", orderNo: "DO-NEW" })),
      postForm: vi.fn().mockResolvedValue({ id: "portal_actual_proof" }),
    };
    return api as unknown as DealerPortalApi & { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn>; login: ReturnType<typeof vi.fn> };
  }

  async function loginAs(api: DealerPortalApi) {
    render(<DealerPortalApp api={api} />);
    await screen.findByTestId("dealer-portal-login");
    fireEvent.change(screen.getByTestId("dealer-login-name"), { target: { value: "dealer_1" } });
    fireEvent.change(screen.getByTestId("dealer-login-password"), { target: { value: "Portal#2026" } });
    fireEvent.click(screen.getByTestId("dealer-login-submit"));
    await screen.findByTestId("dealer-portal-home");
  }

  it("登录后只见本企业；库存外显只有四态中文且不显示数量；状态全中文", async () => {
    const api = makeApi();
    await loginAs(api);
    expect(api.login).toHaveBeenCalledWith("dealer_1", "Portal#2026");
    expect(screen.getByTestId("dealer-portal-status").textContent).toContain("已通过");
    fireEvent.change(screen.getByTestId("contract-select"), { target: { value: "dct_1" } });
    await screen.findByTestId("stock-table");
    expect(screen.getByTestId("stock-availability-item_a").textContent).toBe("紧张");
    expect(screen.getByTestId("stock-availability-item_c").textContent).toBe("预计交期");
    expect(screen.getByTestId("stock-table").textContent).not.toMatch(/\d+\s*(箱|件|瓶)\b|数量/);
    expect(screen.getByTestId("inquiry-status-dqi_1").textContent).toBe("报价审批中");
    expect(screen.getByTestId("order-status-dord_1").textContent).toBe("配送中");
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    await screen.findByTestId("order-detail");
    const methods = Array.from((screen.getByTestId("pay-method") as HTMLSelectElement).querySelectorAll("option")).map((o) => o.textContent);
    expect(methods).toEqual(["对公转账", "平台线上支付"]);
    expect(screen.getByTestId("payment-status-dpay_1").textContent).toBe("已核验");
    expect(screen.queryByTestId("cancel-form")).toBeNull();
  });

  it("订单详情显示发票列表（金额 / 状态 / 发票号 / 重开自），出库前可提交取消申请", async () => {
    const api = makeApi({
      order: { id: "dord_1", orderNo: "DO-1", status: "pending_payment", totalAmountCent: 5000, paidAmountCent: 0 },
      invoices: [{ id: "dinv_1", amountCent: 5000, status: "reversed", invoiceNo: "INV-1" }, { id: "dinv_2", amountCent: 4000, status: "issued", invoiceNo: "INV-2", reissuedFromInvoiceId: "dinv_1" }],
    });
    await loginAs(api);
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    await screen.findByTestId("order-detail");
    await waitFor(() => expect(screen.queryByTestId("invoice-table")).not.toBeNull());
    expect(screen.getByTestId("invoice-status-dinv_1").textContent).toBe("已红冲");
    const row = screen.getByText("INV-2").closest("tr")!;
    expect(Array.from(row.querySelectorAll("td")).map((cell) => cell.textContent)).toEqual(["¥40", "已开票", "INV-2", "INV-1"]);
    fireEvent.change(screen.getByTestId("cancel-reason"), { target: { value: "经销商改期" } });
    fireEvent.click(screen.getByTestId("cancel-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/orders/dord_1/cancel-request", expect.objectContaining({ reason: "经销商改期" })));
    await screen.findByText("取消申请已提交，由平台内部审批后生效");
  });

  it("询价、下单（建单+提交）、付款登记、签收、退货申请调用门户端点", async () => {
    const api = makeApi();
    await loginAs(api);
    fireEvent.change(screen.getByTestId("contract-select"), { target: { value: "dct_1" } });
    await screen.findByTestId("stock-table");
    fireEvent.change(screen.getByTestId("inquiry-item"), { target: { value: "item_b" } });
    fireEvent.change(screen.getByTestId("inquiry-qty"), { target: { value: "5" } });
    fireEvent.click(screen.getByTestId("inquiry-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/inquiries", expect.objectContaining({ dealerContractId: "dct_1", itemId: "item_b", quantity: 5 })));
    fireEvent.change(screen.getByTestId("order-lines"), { target: { value: "item_a,箱,4," } });
    fireEvent.click(screen.getByTestId("order-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/orders", expect.objectContaining({ dealerContractId: "dct_1", lines: [{ itemId: "item_a", spec: "箱", quantity: 4, inquiryId: undefined }] })));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/orders/dord_new/submit", expect.anything()));
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    await screen.findByTestId("order-detail");
    fireEvent.change(screen.getByTestId("pay-amount"), { target: { value: "12.5" } });
    fireEvent.change(screen.getByTestId("pay-ref"), { target: { value: "TX-9" } });
    fireEvent.click(screen.getByTestId("pay-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/orders/dord_1/payments", expect.objectContaining({ method: "bank_transfer", amountCent: 1250, referenceNo: "TX-9" })));
    fireEvent.change(await screen.findByLabelText("item_a（箱） 合格接收"), { target: { value: "3" } });
    fireEvent.change(screen.getByLabelText("已核验收货确认人"), { target: { value: "企业收货人" } });
    fireEvent.change(screen.getByLabelText("实际交接凭证"), { target: { files: [new File(["image"], "收货.png", { type: "image/png" })] } });
    await screen.findByText("收货.png（已上传）");
    expect(api.postForm).toHaveBeenCalledWith("/api/v1/dealer-portal/orders/dord_1/delivery-evidence", expect.any(FormData));
    fireEvent.click(screen.getByRole("button", { name: "保存本次收货事实" }));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/orders/dord_1/sign", expect.objectContaining({ buyerContact: "企业收货人", evidenceAttachmentIds: ["portal_actual_proof"], lines: [{ orderLineId: "line_1", acceptedQuantity: 3, rejectedQuantity: 0, damagedQuantity: 0, shortQuantity: 0 }] })));
  });

  it("四类资料审核前不能进入门户，也不开放自助提交入口", async () => {
    const api = makeApi({ me: { ...dealer, status: "pending_submission", portalEnabled: false } });
    render(<DealerPortalApp api={api} />);
    await screen.findByTestId("dealer-portal-login");
    fireEvent.change(screen.getByTestId("dealer-login-name"), { target: { value: "dealer_1" } });
    fireEvent.change(screen.getByTestId("dealer-login-password"), { target: { value: "Portal#2026" } });
    fireEvent.click(screen.getByTestId("dealer-login-submit"));
    await screen.findByText("四类资料审核通过后才可登录经销商门户");
    expect(screen.queryByTestId("dealer-portal-home")).toBeNull();
    expect(screen.queryByTestId("profile-submit")).toBeNull();
    expect(api.get).not.toHaveBeenCalledWith("/api/v1/dealer-portal/orders");
  });

  it("部分退货后按净未付登记尾款，待合同事件的日期明确展示", async () => {
    const api = makeApi({ order: { id: "dord_1", orderNo: "DO-1", status: "partially_returned", totalAmountCent: 10000, paidAmountCent: 3000, returnedAmountCent: 2000, creditTerm: true } });
    await loginAs(api);
    expect(screen.getByTestId("order-unpaid-dord_1").textContent).toBe("¥50");
    expect(screen.getByText("待合同约定事件确定")).toBeTruthy();
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    await screen.findByTestId("order-detail");
    fireEvent.change(screen.getByTestId("pay-amount"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("pay-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/orders/dord_1/payments", expect.objectContaining({ amountCent: 5000 })));
  });

  it("首程费减款进入净未付，退货按真实签收来源提交并区分运费待退与已退", async () => {
    const api = makeApi({
      order: { id: "dord_1", orderNo: "DO-1", status: "partially_returned", totalAmountCent: 10000, paidAmountCent: 3000, returnedAmountCent: 2000, firstLegAdjustmentCent: 1000 },
      sources: [{receiptLineId:"receipt_2",orderLineId:"line_1",batchId:"batch_2",availableQuantity:2}],
      returns: [{id:"ret_1",returnNo:"RET-1",status:"completed",reasonType:"quality_issue",requestedQuantity:1,refundCent:1000,refundedCent:1000}],
      financials: [{returnId:"ret_1",firstLegOffsetCent:0,firstLegRefundCent:1000,firstLegRefundedCent:0}],
    });
    await loginAs(api);
    expect(screen.getByTestId("order-unpaid-dord_1").textContent).toBe("¥40");
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    fireEvent.click(await screen.findByRole("button",{name:"item_a 箱，批次 batch_2，可退 2"}));
    expect((screen.getByTestId("return-lines") as HTMLTextAreaElement).value).toBe("item_a,箱,2,receipt_2");
    fireEvent.click(screen.getByTestId("return-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/orders/dord_1/returns",expect.objectContaining({lines:[{itemId:"item_a",spec:"箱",quantity:2,sourceReceiptLineId:"receipt_2"}]})));
    const row=screen.getByText("RET-1").closest("tr")!;
    expect(Array.from(row.querySelectorAll("td")).slice(-3).map(cell=>cell.textContent)).toEqual(["¥0","¥10","¥0"]);
    expect(screen.getByText("货款办理状态")).toBeTruthy();
  });

  it("停用后的业务请求返回登录并提示撤权", async () => {
    const api = makeApi();
    await loginAs(api);
    fireEvent.click(screen.getByTestId("order-select-dord_1"));
    await screen.findByTestId("order-detail");
    api.post.mockRejectedValueOnce(new DealerPortalApiError("经销商已停用，门户权限已撤销", 403, "DEALER_DEACTIVATED"));
    fireEvent.change(screen.getByTestId("pay-amount"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("pay-submit"));
    await screen.findByTestId("dealer-portal-login");
    expect(screen.getByTestId("dealer-portal-error").textContent).toBe("经销商已停用，门户权限已撤销");
  });
});
