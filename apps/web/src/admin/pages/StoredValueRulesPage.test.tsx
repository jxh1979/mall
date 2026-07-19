import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StoredValueRulesPage } from "./StoredValueRulesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const RULE = { merchantId: "m1", refundAllowed: true, refundRatioBps: 10000, cardIssuanceEnabled: false, fundOwnership: "merchant_prepaid" };
function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return { get: vi.fn().mockResolvedValue(RULE), getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<StoredValueRulesPage {...props} />);
}

describe("StoredValueRulesPage 储值规则配置(ADM-CUS-005)", () => {
  it("查询商户后读回商户卡规则", async () => {
    const api = makeApi();
    renderPage(api);
    fireEvent.change(screen.getByLabelText("商户 id"), { target: { value: "m1" } });
    fireEvent.click(screen.getByTestId("stored-value-query"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/loyalty/stored-value/merchant-refund-rules/m1"));
    await waitFor(() => expect(screen.getByTestId("stored-value-current")).toBeTruthy());
  });

  it("保存发卡与资金模式走 merchant-card-config", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    fireEvent.change(screen.getByLabelText("商户 id"), { target: { value: "m1" } });
    fireEvent.click(screen.getByTestId("stored-value-query"));
    await waitFor(() => expect(screen.getByTestId("stored-value-card-config-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("发卡资格"), { target: { value: "on" } });
    fireEvent.change(screen.getByLabelText("资金模式"), { target: { value: "platform_collect" } });
    fireEvent.click(screen.getByTestId("stored-value-card-config-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/stored-value/merchant-card-config",
      expect.objectContaining({ merchantId: "m1", cardIssuanceEnabled: true, fundOwnership: "platform_collect" }),
    ));
  });

  it("退款比例按百分比转基点后 POST", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(post));
    fireEvent.change(screen.getByLabelText("商户 id"), { target: { value: "m1" } });
    fireEvent.click(screen.getByTestId("stored-value-query"));
    await waitFor(() => expect(screen.getByTestId("stored-value-refund-rule-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("可退比例"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("stored-value-refund-rule-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/stored-value/merchant-refund-rules",
      expect.objectContaining({ merchantId: "m1", refundAllowed: true, refundRatioBps: 5000 }),
    ));
  });
});
