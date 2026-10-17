import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StoredValueRulesPage } from "./StoredValueRulesPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const RULE = { merchantId: "m1", refundAllowed: true, refundRatioBps: 10000, cardIssuanceEnabled: false, fundOwnership: "merchant_prepaid", consumeOrder: "principal_first" };
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
    fireEvent.change(screen.getByLabelText("消费扣款顺序"), { target: { value: "bonus_first" } });
    fireEvent.click(screen.getByTestId("stored-value-card-config-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/stored-value/merchant-card-config",
      expect.objectContaining({ merchantId: "m1", cardIssuanceEnabled: true, fundOwnership: "platform_collect", consumeOrder: "bonus_first" }),
    ));
  });

  it("CG-06a 通用卡赠送比例按百分比转万分比后 POST，含上下限", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = makeApi(post);
    (api.get as ReturnType<typeof vi.fn>).mockResolvedValue({ projectId: "prj_1", bonusBps: 500, minBps: 0, maxBps: 2000 });
    renderPage(api);
    fireEvent.change(screen.getByLabelText("赠送比例项目 id"), { target: { value: "prj_1" } });
    fireEvent.click(screen.getByTestId("stored-value-bonus-policy-query"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/loyalty/stored-value/bonus-policies/prj_1"));
    await waitFor(() => expect(screen.getByTestId("stored-value-bonus-policy-current").textContent).toContain("赠送 5%"));
    fireEvent.change(screen.getByLabelText("赠送比例"), { target: { value: "10" } });
    fireEvent.change(screen.getByLabelText("赠送比例下限"), { target: { value: "0" } });
    fireEvent.change(screen.getByLabelText("赠送比例上限"), { target: { value: "20" } });
    fireEvent.change(screen.getByLabelText("赠送比例配置缘由"), { target: { value: "国庆活动" } });
    fireEvent.click(screen.getByTestId("stored-value-bonus-policy-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/stored-value/bonus-policies",
      expect.objectContaining({ projectId: "prj_1", bonusBps: 1000, minBps: 0, maxBps: 2000, reason: "国庆活动" }),
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
