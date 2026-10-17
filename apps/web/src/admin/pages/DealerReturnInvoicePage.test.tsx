// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { DealerReturnInvoicePage } from "./DealerReturnInvoicePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

/** CG-11c（BSM:1238-1251）：退货单十态、原因、运费承担方、发票状态全中文；验收 / 红冲 / 退款结果调用对应端点。 */
describe("DealerReturnInvoicePage 经销退货与开票（ADM-DLR-003）", () => {
  const retRow = { id: "dret_1", returnNo: "DR-1", status: "inspecting", reasonType: "quality_issue", requestedQuantity: 4, acceptedQuantity: 0, returnFreightBearer: "sales_entity", freightConfirmed: true };
  function makeApi() {
    return {
      get: vi.fn(async (url: string) => {
        if (url === "/api/v1/dealers") return [{ id: "dlr_1", dealerName: "华北经销", status: "approved" }];
        if (url.endsWith("/orders")) return [{ id: "dord_1", orderNo: "DO-1", status: "partially_returned", totalAmountCent: 6000, paidAmountCent: 4000, returnedAmountCent: 3000 }];
        if (url === "/api/v1/dealers/orders/dord_1") return { id: "dord_1", orderNo: "DO-1", status: "partially_returned", totalAmountCent: 6000, paidAmountCent: 4000, returnedAmountCent: 3000 };
        if (url.endsWith("/returns")) return [retRow, { id: "dret_2", returnNo: "DR-2", status: "pending_refund", reasonType: "unopened_resalable", returnFreightBearer: "pending_confirmation", refundApproved: true, refundCent: 1000 }];
        if (url.endsWith("/invoices")) return [{ id: "dinv_1", amountCent: 5000, status: "reversed", invoiceNo: "INV-1", reversedByReturnId: "dret_1" }, { id: "dinv_2", amountCent: 3000, status: "pending", reissuedFromInvoiceId: "dinv_1" }];
        if (url === "/api/v1/dealers/returns/dret_1") return retRow;
        if (url === "/api/v1/dealers/returns/dret_2") return { ...retRow, id: "dret_2", status: "pending_refund", refundApproved: true, returnFreightBearer: "pending_confirmation", freightConfirmed: false };
        if (url.endsWith("/lines")) return [{ id: "drl_1", itemId: "item_a", spec: "箱", requestedQuantity: 4, acceptedQuantity: 0, resalableQuantity: 0, quarantinedQuantity: 0, unitPriceCent: 1000, acceptedAmountCent: 0 }];
        return [];
      }),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({ id: "x", returnNo: "DR-NEW", status: "pending_refund", acceptedAmountCent: 3000, offsetReceivableCent: 2000, refundCent: 1000 }),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }
  function renderPage(api: AdminApiClient) {
    const setActionResult = vi.fn();
    const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<DealerReturnInvoicePage {...props} />);
    return setActionResult;
  }

  it("退货单状态、原因、运费承担方与发票状态全中文；退货原因只列两类", async () => {
    const api = makeApi();
    renderPage(api);
    await screen.findByTestId("dealer-returns-dealer-table");
    fireEvent.click(screen.getByTestId("dealer-returns-select-dlr_1"));
    await screen.findByTestId("return-order-table");
    expect(screen.getByTestId("return-order-status-dord_1").textContent).toBe("部分退货");
    fireEvent.click(screen.getByTestId("return-order-select-dord_1"));
    await screen.findByTestId("return-table");
    expect(screen.getByTestId("return-status-dret_1").textContent).toBe("验收中");
    expect(screen.getByTestId("return-reason-dret_1").textContent).toBe("质量问题");
    expect(screen.getByTestId("return-bearer-dret_1").textContent).toBe("销售主体");
    expect(screen.getByTestId("return-status-dret_2").textContent).toBe("待退款");
    expect(screen.getByTestId("return-bearer-dret_2").textContent).toBe("待双方确认");
    expect(screen.getByTestId("invoice-status-dinv_1").textContent).toBe("已红冲");
    expect(screen.getByTestId("invoice-status-dinv_2").textContent).toBe("待开票");
    const reasons = Array.from((screen.getByTestId("return-reason") as HTMLSelectElement).querySelectorAll("option")).map((o) => o.textContent);
    expect(reasons).toEqual(["未拆封可复售", "质量问题"]);
  });

  it("验收、退款结果、运费确认与退货申请调用对应端点", async () => {
    const api = makeApi();
    const setActionResult = renderPage(api);
    await screen.findByTestId("dealer-returns-dealer-table");
    fireEvent.click(screen.getByTestId("dealer-returns-select-dlr_1"));
    await screen.findByTestId("return-order-table");
    fireEvent.click(screen.getByTestId("return-order-select-dord_1"));
    await screen.findByTestId("return-table");
    fireEvent.click(screen.getByTestId("return-select-dret_1"));
    await screen.findByTestId("return-inspection-form");
    fireEvent.change(screen.getByTestId("inspection-lines-input"), { target: { value: "item_a,箱,3,2,1,一箱破损" } });
    fireEvent.click(screen.getByTestId("return-inspect"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/returns/dret_1/inspection", expect.objectContaining({
      lines: [{ itemId: "item_a", spec: "箱", acceptedQuantity: 3, resalableQuantity: 2, quarantinedQuantity: 1, note: "一箱破损" }],
    })));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "dealer_return_inspected", 冲减未收货款: "¥20", 待退款: "¥10" })));

    fireEvent.click(screen.getByTestId("return-select-dret_2"));
    await screen.findByTestId("return-refund-success");
    vi.spyOn(window, "prompt").mockReturnValue("RF-7");
    fireEvent.click(screen.getByTestId("return-refund-success"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/returns/dret_2/refund-result", expect.objectContaining({ result: "success", referenceNo: "RF-7" })));
    fireEvent.change(screen.getByTestId("freight-bearer"), { target: { value: "sales_entity" } });
    fireEvent.click(screen.getByTestId("freight-confirm"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/returns/dret_2/freight-confirmation", expect.objectContaining({ returnFreightBearer: "sales_entity", firstLegFreightRefundable: false })));

    fireEvent.change(screen.getByTestId("return-reason"), { target: { value: "unopened_resalable" } });
    fireEvent.change(screen.getByTestId("return-lines-input"), { target: { value: "item_a,箱,2" } });
    fireEvent.click(screen.getByTestId("return-request"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/orders/dord_1/returns", expect.objectContaining({ reasonType: "unopened_resalable", lines: [{ itemId: "item_a", spec: "箱", quantity: 2 }] })));
    fireEvent.click(screen.getByTestId("invoice-request"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/orders/dord_1/invoices", expect.objectContaining({ idempotencyKey: expect.any(String) })));
  });
});
