import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StoredValueRulesPage } from "./StoredValueRulesPage";
import { apiErrorFromResponse } from "../../shared/apiError";
import type { AdminApiClient, AdminPageProps, AdminUser, EffectivePermissionsState } from "../types";

const RULE = { merchantId: "m1", refundAllowed: true, refundRatioBps: 10000, cardIssuanceEnabled: false, fundOwnership: "merchant_prepaid", consumeOrder: "principal_first" };
function makeApi(post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return { get: vi.fn().mockResolvedValue(RULE), getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const PLATFORM: AdminUser = { id: "u1", username: "admin", accountType: "platform_admin" };
const MERCHANT: AdminUser = { id: "u2", username: "shop", accountType: "merchant_admin", merchantId: "m1" };
function renderPage(api: AdminApiClient, user: AdminUser = MERCHANT) {
  const props: AdminPageProps = { api, user, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<StoredValueRulesPage {...props} />);
  return props;
}
const RULES_PATH = "/api/v1/loyalty/stored-value/merchant-refund-rules/m1";

// 第十八批 CL 段 423-R2（第十八批裁决 79：ENUM:86、ENUM:107、BSM:1066「由发卡商户在商户后台自行设置」；BSM:833「平台各岗位（含最高权限）一律看不到
// 商户级卡……」）：商户账号直接读、设本商户储值卡；平台账号只剩通用卡说明与退出公示天数，不再出现商户级卡配置。
describe("StoredValueRulesPage 储值规则配置(ADM-CUS-005)", () => {
  it("平台账号不出现商户级卡配置，只留退出公示天数", async () => {
    const api = makeApi();
    renderPage(api, PLATFORM);
    await waitFor(() => expect(screen.getByTestId("stored-value-rules-page")).toBeTruthy());
    await waitFor(() => expect(screen.getByTestId("stored-value-exit-notice-policy-form")).toBeTruthy());
    expect(screen.queryByTestId("stored-value-merchant-filter")).toBeNull();
    expect(screen.queryByTestId("stored-value-card-config-form")).toBeNull();
    expect(screen.queryByTestId("stored-value-refund-rule-form")).toBeNull();
    expect(api.get).not.toHaveBeenCalledWith(expect.stringContaining("merchant-refund-rules"));
  });

  it("商户账号打开即读本商户规则，设扣款顺序走 merchant-card-config", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = makeApi(post);
    renderPage(api);
    await waitFor(() => expect(api.get).toHaveBeenCalledWith(RULES_PATH));
    await waitFor(() => expect(screen.getByTestId("stored-value-current")).toBeTruthy());
    expect(screen.getByTestId("stored-value-card-config-form")).toBeTruthy();
    expect(screen.queryByLabelText("商户编号")).toBeNull();
    expect(screen.queryByTestId("stored-value-exit-notice-policy-form")).toBeNull();
    expect(screen.queryByRole("option", { name: /平台代收/ })).toBeNull();
    fireEvent.change(screen.getByLabelText("发卡资格"), { target: { value: "on" } });
    fireEvent.change(screen.getByLabelText("消费扣款顺序"), { target: { value: "bonus_first" } });
    fireEvent.click(screen.getByTestId("stored-value-card-config-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/stored-value/merchant-card-config",
      expect.objectContaining({ merchantId: "m1", cardIssuanceEnabled: true, fundOwnership: "merchant_prepaid", consumeOrder: "bonus_first" }),
    ));
  });

  it("商户账号退款比例按百分比转基点后 POST", async () => {
    const post = vi.fn().mockResolvedValue({});
    const api = makeApi(post);
    renderPage(api);
    await waitFor(() => expect(api.get).toHaveBeenCalledWith(RULES_PATH));
    await waitFor(() => expect(screen.getByTestId("stored-value-current")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("可退比例"), { target: { value: "50" } });
    fireEvent.click(screen.getByTestId("stored-value-refund-rule-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/loyalty/stored-value/merchant-refund-rules",
      expect.objectContaining({ merchantId: "m1", refundAllowed: true, refundRatioBps: 5000 }),
    ));
  });

  // 18CL 收尾复核（BSM:833 裁决 79 新句「平台方……按工商登记合计持股过半的商户，不得发行商户级卡，只能使用综合体通用卡。」；PGM:291「用户看到的每一处文字
  // 都是中文，含……提示、报错」）：保存被后端拒绝时报出后端中文提示，不记成功（修复前拒绝未处理、页面无任何提示）
  it("商户账号保存被拒时报出后端中文提示", async () => {
    const post = vi.fn(async (path: string) => {
      if (path.endsWith("/merchant-card-config")) {
        throw apiErrorFromResponse(path, 409, JSON.stringify({ success: false, code: "STATE_CONFLICT", detail_code: "MERCHANT_CARD_PLATFORM_MAJORITY",
          message: "平台方合计持股过半的商户不得发行商户级卡，只能使用综合体通用卡" }));
      }
      throw apiErrorFromResponse(path, 403, JSON.stringify({ success: false, code: "FORBIDDEN", detail_code: "PERMISSION_DENIED", message: "permission denied" }));
    });
    const api = makeApi(post);
    const props = renderPage(api);
    await waitFor(() => expect(api.get).toHaveBeenCalledWith(RULES_PATH));
    await waitFor(() => expect(screen.getByTestId("stored-value-current")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("发卡资格"), { target: { value: "on" } });
    fireEvent.click(screen.getByTestId("stored-value-card-config-save"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("平台方合计持股过半的商户不得发行商户级卡，只能使用综合体通用卡")));
    fireEvent.click(screen.getByTestId("stored-value-refund-rule-save"));
    await waitFor(() => expect(props.setGlobalError).toHaveBeenCalledWith("当前账号无权进行此操作"));
    expect(props.setActionResult).not.toHaveBeenCalled();
  });
});
