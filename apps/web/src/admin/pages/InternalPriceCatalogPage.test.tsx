import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { InternalPriceCatalogPage } from "./InternalPriceCatalogPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// 内部结算价目录（ADM-STL-003，批10/E V379/V380）组件测试：
// 三维筛选加载、草案落存（新建返回编码）、他人复核转正走泛化端点、下线归档必填理由。
const ROWS = [
  { id: "row_a", itemCode: "sku_bp_p1", merchantId: "m_bar_1", businessFormat: "bar", category: "spirit",
    itemName: "威士忌", priceCent: 5100, status: "active", isDraft: false, updatedBy: "op_a", updatedAt: "2026-08-01T00:00:00Z" },
  { id: "row_d", itemCode: "sku_bp_p1", merchantId: "m_bar_1", businessFormat: "bar", category: "spirit",
    itemName: "威士忌", priceCent: 3000, status: "active", isDraft: true, updatedBy: "op_b", updatedAt: "2026-08-01T01:00:00Z" },
];

function makeApi(
  get = vi.fn().mockResolvedValue(ROWS),
  post = vi.fn().mockResolvedValue("sku_new_1"),
): AdminApiClient {
  return {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient, setGlobalError = vi.fn()) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  };
  render(<InternalPriceCatalogPage {...props} />);
  return { setGlobalError };
}

afterEach(() => vi.restoreAllMocks());

describe("InternalPriceCatalogPage 内部结算价目录(ADM-STL-003)", () => {
  it("加载即请求三维筛选列表(含草案)，草案与生效行分表展示", async () => {
    const get = vi.fn().mockResolvedValue(ROWS);
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith(
      "/api/v1/settlement/config/internal-price-items?include_drafts=true"));
    expect(await screen.findByTestId("internal-price-catalog-page")).toBeTruthy();
    // 生效行 5100 分（¥51.00）、草案行 3000 分（¥30.00）各归各表——草案不生效，不混入生效价。
    expect(screen.getByTestId("ipc-price-sku_bp_p1").textContent).toContain("51.00");
    expect(screen.getByTestId("ipc-draft-sku_bp_p1").textContent).toContain("30.00");
  });

  it("商家/业态/品类三维筛选变化 → 重新按筛选参数请求", async () => {
    const get = vi.fn().mockResolvedValue(ROWS);
    renderPage(makeApi(get));
    await screen.findByTestId("ipc-filters");
    fireEvent.change(screen.getByTestId("ipc-filter-merchant"), { target: { value: "m_bar_1" } });
    fireEvent.change(screen.getByTestId("ipc-filter-format"), { target: { value: "bar" } });
    fireEvent.change(screen.getByTestId("ipc-filter-category"), { target: { value: "spirit" } });
    await waitFor(() => expect(get).toHaveBeenCalledWith(
      "/api/v1/settlement/config/internal-price-items?include_drafts=true&merchant_id=m_bar_1&business_format=bar&category=spirit"));
  });

  it("新建条目落草案：POST internal-price-item 带商家/品类/名称/价(分)，编码留空=新建", async () => {
    const post = vi.fn().mockResolvedValue("sku_new_1");
    renderPage(makeApi(vi.fn().mockResolvedValue([]), post));
    await screen.findByTestId("ipc-create-form");
    fireEvent.change(screen.getByTestId("ipc-form-merchant"), { target: { value: "m_bar_1" } });
    fireEvent.change(screen.getByTestId("ipc-form-category"), { target: { value: "spirit" } });
    fireEvent.change(screen.getByTestId("ipc-form-name"), { target: { value: "新款金酒" } });
    fireEvent.change(screen.getByTestId("ipc-form-price"), { target: { value: "68" } });
    fireEvent.click(screen.getByTestId("ipc-form-submit"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/internal-price-item",
      expect.objectContaining({
        itemCode: null, merchantId: "m_bar_1", category: "spirit", itemName: "新款金酒",
        priceCent: 6800, idempotencyKey: expect.any(String),
      }),
    ));
  });

  it("草案复核转正走泛化端点 internal_price_item/{id}/activate", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(ROWS), post));
    fireEvent.click(await screen.findByTestId("ipc-activate-sku_bp_p1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/internal_price_item/row_d/activate"));
  });

  it("下线归档必填理由；带理由 POST archive（软删留库，不影响历史引用）", async () => {
    const post = vi.fn().mockResolvedValue({});
    const { setGlobalError } = renderPage(makeApi(vi.fn().mockResolvedValue(ROWS), post));
    fireEvent.click(await screen.findByTestId("ipc-archive-sku_bp_p1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith(expect.stringContaining("理由")));
    expect(post).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("ipc-archive-reason-sku_bp_p1"), { target: { value: "该 SKU 已停售" } });
    fireEvent.click(screen.getByTestId("ipc-archive-sku_bp_p1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith(
      "/api/v1/settlement/config/internal_price_item/row_a/archive",
      expect.objectContaining({ archiveReason: "该 SKU 已停售", idempotencyKey: expect.any(String) }),
    ));
  });
});
