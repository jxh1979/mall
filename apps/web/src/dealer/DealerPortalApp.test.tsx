// 文件版本：V0.02
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
        if (url.endsWith("/payments")) return [{ id: "dpay_1", method: "bank_transfer", amountCent: 5000, status: "verified", referenceNo: "TX-1" }];
        if (url.endsWith("/returns")) return [];
        return [];
      }),
      post: vi.fn(async () => ({ id: "dord_new", orderNo: "DO-NEW" })),
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
    fireEvent.click(screen.getByTestId("order-sign"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealer-portal/orders/dord_1/sign", expect.anything()));
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
