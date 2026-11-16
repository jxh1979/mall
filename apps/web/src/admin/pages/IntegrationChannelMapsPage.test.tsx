import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { IntegrationChannelMapsPage } from "./IntegrationChannelMapsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const TICKET_MAPS = [
  {
    id: "map-1", providerCode: "meituan-groupbuy", channel: "meituan", externalProductId: "mt-prod-1",
    ticketTypeId: "tt_pass", priceRuleId: "rule_groupbuy", status: "active",
  },
];
const STORE_MAPS = [
  {
    id: "smap-1", providerCode: "meituan-groupbuy", channel: "douyin", externalStoreId: "dy-store-1",
    merchantId: "mch_bar_1", status: "inactive",
  },
  {
    id: "smap-2", providerCode: "meituan-groupbuy", channel: "douyin", externalStoreId: "dy-store-2",
    merchantId: "mch_bar_2", status: "active",
  },
];

function makeApi(): AdminApiClient {
  return {
    get: vi.fn().mockImplementation((url: string) =>
      Promise.resolve(url.includes("channel-ticket-type-maps") ? TICKET_MAPS : STORE_MAPS)),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(): AdminApiClient {
  const api = makeApi();
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  } as unknown as AdminPageProps;
  render(<IntegrationChannelMapsPage {...props} />);
  return api;
}

describe("IntegrationChannelMapsPage 渠道映射维护（批6）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("两张映射表渲染，渠道/状态全中文", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-map-row-mt-prod-1")).toBeTruthy());
    const ticketTable = screen.getByTestId("ticket-map-table").textContent ?? "";
    expect(ticketTable).toContain("美团");
    expect(ticketTable).toContain("生效");
    expect(ticketTable).toContain("tt_pass");
    const storeTable = screen.getByTestId("store-map-table").textContent ?? "";
    expect(storeTable).toContain("抖音");
    expect(storeTable).toContain("已停用");
    expect(storeTable).not.toContain("inactive");
  });

  it("登记票种映射：必填校验 + 提交走 channel-ticket-type-maps 端点", async () => {
    const api = renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-map-form")).toBeTruthy());
    // 空表单 → 中文必填提示，不发请求。
    fireEvent.click(screen.getByTestId("ticket-map-save"));
    await waitFor(() => expect(screen.getByTestId("channel-maps-error").textContent).toContain("必填"));
    expect(api.post).not.toHaveBeenCalled();
    // 填齐 → 提交。
    fireEvent.change(screen.getByTestId("ticket-map-provider"), { target: { value: "meituan-groupbuy" } });
    fireEvent.change(screen.getByTestId("ticket-map-product"), { target: { value: "mt-prod-2" } });
    fireEvent.change(screen.getByTestId("ticket-map-ticket-type"), { target: { value: "tt_pass_v2" } });
    fireEvent.click(screen.getByTestId("ticket-map-save"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/integration/channel-ticket-type-maps",
      expect.objectContaining({ external_product_id: "mt-prod-2", ticket_type_id: "tt_pass_v2" }),
    ));
  });

  // 第十八批 AO 段第 251 条 251-R2（BSM:576「存档及删除都需要填写理由」）：停用理由由操作人填写，不再写死。
  it("停用票种映射：弹窗填的理由随 archive 请求提交", async () => {
    vi.spyOn(window, "prompt").mockReturnValue("渠道下架");
    const api = renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-map-archive-mt-prod-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-map-archive-mt-prod-1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/integration/channel-ticket-type-maps/map-1/archive",
      expect.objectContaining({ reason: "渠道下架" }),
    ));
  });

  it("停用弹窗取消：不提交", async () => {
    vi.spyOn(window, "prompt").mockReturnValue(null);
    const api = renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-map-archive-mt-prod-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-map-archive-mt-prod-1"));
    await waitFor(() => expect(window.prompt).toHaveBeenCalled());
    expect(api.post).not.toHaveBeenCalled();
  });

  it("停用理由为空白：提示必填、不提交", async () => {
    vi.spyOn(window, "prompt").mockReturnValue("   ");
    const api = renderPage();
    await waitFor(() => expect(screen.getByTestId("ticket-map-archive-mt-prod-1")).toBeTruthy());
    fireEvent.click(screen.getByTestId("ticket-map-archive-mt-prod-1"));
    await waitFor(() => expect(screen.queryByTestId("channel-maps-error")?.textContent ?? "").toContain("停用理由不能为空"));
    expect(api.post).not.toHaveBeenCalled();
  });

  it("停用门店映射：弹窗填的理由随 archive 请求提交", async () => {
    vi.spyOn(window, "prompt").mockReturnValue("门店闭店");
    const api = renderPage();
    await waitFor(() => expect(screen.getByTestId("store-map-archive-dy-store-2")).toBeTruthy());
    fireEvent.click(screen.getByTestId("store-map-archive-dy-store-2"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/integration/channel-store-maps/smap-2/archive",
      expect.objectContaining({ reason: "门店闭店" }),
    ));
  });
});
