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
    fireEvent.change(screen.getByLabelText("资金模式"), { target: { value: "merchant_prepaid" } });
    fireEvent.change(screen.getByLabelText("消费扣款顺序"), { target: { value: "bonus_first" } });
    fireEvent.click(screen.getByTestId("stored-value-card-config-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/stored-value/merchant-card-config",
      expect.objectContaining({ merchantId: "m1", cardIssuanceEnabled: true, fundOwnership: "merchant_prepaid", consumeOrder: "bonus_first" }),
    ));
  });

  // 第十八批第 11 条：BSM L833「商户级卡的充值资金归发卡商户预收，平台不代收」、DEC-CFG-2026-08-03-11「关闭「可由后台改为平台统一收取」开关」；
  // 赠送比例（通用卡按项目、商户级卡按发卡商户）走涉钱配置版本审批（BSM L521；cg-pending-decisions 第 13 条），本页不再直接保存。
  it("资金模式只有商户预收，赠送比例改到门票积分页提交审批", async () => {
    renderPage(makeApi());
    await waitFor(() => expect(screen.queryByText(/综合体通用卡充值赠送比例在「门票积分、成长值与销售责任」页提交审批/)).not.toBeNull());
    expect(screen.queryByTestId("stored-value-bonus-policy-form")).toBeNull();
    fireEvent.change(screen.getByLabelText("商户 id"), { target: { value: "m1" } });
    fireEvent.click(screen.getByTestId("stored-value-query"));
    await waitFor(() => expect(screen.getByLabelText("资金模式")).toBeTruthy());
    expect(screen.queryByRole("option", { name: /平台代收/ })).toBeNull();
    expect(screen.queryByText(/商户级卡赠送比例在「门票积分、成长值与销售责任」页提交审批/)).not.toBeNull();
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
