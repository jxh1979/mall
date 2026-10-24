// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ApiError } from "../../shared/apiError";
import { MerchantZoneContractsPage } from "./MerchantZoneContractsPage";
import { StoredValueRulesPage } from "./StoredValueRulesPage";
import { TicketingPoliciesPage } from "./TicketingPoliciesPage";
import { VenueBusinessHoursPage } from "./VenueBusinessHoursPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// ROOT-03 B 段券与展示组：四块客人端相关后台配置分别嵌在既有页面里，请求按后台真实形状（adminClient 再转蛇形键）。
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function makeApi(get: (url: string) => unknown = () => []): AdminApiClient {
  return {
    get: vi.fn(async (url: string) => get(url)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function props(api: AdminApiClient): AdminPageProps {
  return {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
}

describe("客人端相关后台配置（ROOT-03 B 段券与展示组）", () => {
  it("储值规则页：商户卡退出公示天数值与上下限同配后保存", async () => {
    const api = makeApi((url) => (url.includes("exit-notice-policy") ? null : {}));
    const p = props(api);
    render(<StoredValueRulesPage {...p} />);
    await waitFor(() => expect(screen.getByTestId("stored-value-exit-notice-policy-form")).toBeTruthy());
    fireEvent.click(screen.getByTestId("stored-value-exit-notice-policy-query"));
    await waitFor(() => expect(screen.getByTestId("stored-value-exit-notice-policy-current").textContent).toBe("尚未配置"));
    fireEvent.change(screen.getByLabelText("退出公示天数"), { target: { value: "30" } });
    fireEvent.click(screen.getByTestId("stored-value-exit-notice-policy-save"));
    expect(api.post).not.toHaveBeenCalled();
    expect(p.setGlobalError).toHaveBeenCalledWith(expect.stringContaining("须同时填写"));
    fireEvent.change(screen.getByLabelText("退出公示天数下限"), { target: { value: "7" } });
    fireEvent.change(screen.getByLabelText("退出公示天数上限"), { target: { value: "90" } });
    fireEvent.click(screen.getByTestId("stored-value-exit-notice-policy-save"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy",
      expect.objectContaining({ publishDays: 30, minDays: 7, maxDays: 90 })));
  });

  it("票务策略页：未核销票到期提醒按项目读出厂值、值与上下限同配后 PUT", async () => {
    const api = makeApi((url) => (url.includes("expiry-reminder-policies")
      ? { projectId: "__global__", remindHours: 24, minHours: null, maxHours: null } : []));
    render(<TicketingPoliciesPage {...props(api)} />);
    await waitFor(() => expect(screen.getByTestId("ticket-expiry-reminder-policy")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("到期提醒项目 id"), { target: { value: "prj_1" } });
    fireEvent.click(screen.getByTestId("ticket-expiry-reminder-query"));
    await waitFor(() => expect(screen.getByTestId("ticket-expiry-reminder-current").textContent).toContain("到期前 24 小时（出厂值"));
    fireEvent.change(screen.getByLabelText("到期提醒提前小时数"), { target: { value: "48" } });
    fireEvent.change(screen.getByLabelText("到期提醒下限"), { target: { value: "12" } });
    fireEvent.change(screen.getByLabelText("到期提醒上限"), { target: { value: "72" } });
    fireEvent.click(screen.getByTestId("ticket-expiry-reminder-save"));
    await waitFor(() => expect(api.put).toHaveBeenCalledWith("/api/v1/ticketing/expiry-reminder-policies/prj_1",
      { remindHours: 48, minHours: 12, maxHours: 72 }));
  });

  it("商户专区我的合同页：发布储值卡退出公告，五项条件齐全、比例按百分比转基点", async () => {
    const api = makeApi();
    render(<MerchantZoneContractsPage {...props(api)} />);
    await waitFor(() => expect(screen.getByTestId("merchant-card-exit-notice-form")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("储值卡可用至"), { target: { value: "2026-12-31" } });
    fireEvent.change(screen.getByLabelText("储值卡退款比例"), { target: { value: "80" } });
    fireEvent.change(screen.getByLabelText("储值卡怎么退"), { target: { value: "到店收银台原路退回" } });
    fireEvent.change(screen.getByLabelText("储值卡找谁退"), { target: { value: "店长 王女士" } });
    fireEvent.change(screen.getByLabelText("退出公告公示截止日"), { target: { value: "2026-12-01" } });
    fireEvent.click(screen.getByTestId("merchant-card-exit-notice-publish"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/merchant-portal/card-exit-notices", {
      useUntil: "2026-12-31", refundable: true, refundRatioBps: 8000, refundMethod: "到店收银台原路退回",
      refundContact: "店长 王女士", publishUntil: "2026-12-01",
    }));
  });

  it("场地营业时间页：首页介绍与公告首次提交被拦下送审，提示批准后原样再提交", async () => {
    const api = makeApi((url) => (url.includes("home-contents") ? null : []));
    (api.post as ReturnType<typeof vi.fn>).mockRejectedValueOnce(new ApiError("操作已被拦截，等待主管复核，复核单号：review_1",
      { detail: {} as never, code: "STATE_CONFLICT", detailCode: "ADMISSION_BLOCKED" }));
    const p = props(api);
    render(<VenueBusinessHoursPage {...p} />);
    await waitFor(() => expect(screen.getByTestId("customer-home-content")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("首页内容项目 id"), { target: { value: "prj_1" } });
    fireEvent.change(screen.getByLabelText("综合体介绍"), { target: { value: "城市娱乐综合体" } });
    fireEvent.change(screen.getByLabelText("活动公告"), { target: { value: "周六乐队演出" } });
    fireEvent.click(screen.getByTestId("customer-home-content-publish"));
    await waitFor(() => expect(screen.getByTestId("customer-home-content-note").textContent).toContain("已提交上级审批"));
    expect(api.post).toHaveBeenCalledWith("/api/v1/home-contents/prj_1/publish", { intro: "城市娱乐综合体", announcement: "周六乐队演出" });
    fireEvent.click(screen.getByTestId("customer-home-content-publish"));
    await waitFor(() => expect(p.setActionResult).toHaveBeenCalledWith({ action: "customer_home_content_published", projectId: "prj_1" }));
  });
});
