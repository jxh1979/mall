// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { DealerAdmissionCatalogPage } from "./DealerAdmissionCatalogPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

/** CG-11a（BSM:1216-1222）：经销商五态与四项审核、价格目录版本审批、询价、库存外显四态全中文。 */
describe("DealerAdmissionCatalogPage 经销商准入与价格目录（ADM-DLR-001）", () => {
  const dealers = [
    { id: "dlr_1", dealerName: "华东经销", status: "under_review", portalEnabled: false, contactName: "王五" },
    { id: "dlr_2", dealerName: "华北经销", status: "approved", portalEnabled: true, contactName: "赵六" },
  ];
  function makeApi() {
    return {
      get: vi.fn(async (url: string) => {
        if (url === "/api/v1/dealers") return dealers;
        if (url.endsWith("/contracts")) return [{ id: "dct_1", contractNo: "DC-2026-001", salesEntityId: "be_1", centralWarehouseId: "wh_main", validFrom: "2026-01-01", validTo: "2026-12-31", status: "active" }];
        if (url.endsWith("/inquiries")) return [{ id: "dqi_1", itemId: "item_x", spec: "箱", quantity: 5, status: "quote_approval", quotedPriceCent: 8800, quotedBy: "usr_a", quoteValidUntil: "2026-11-01" }];
        if (url.endsWith("/price-catalogs")) return [{ id: "dpc_1", version: 1, status: "pending_approval", editedBy: "usr_a", submittedBy: "usr_a" }];
        if (url.endsWith("/external-stock")) return [
          { itemId: "item_a", itemName: "威士忌", spec: "箱", unitPriceCent: 120000, availability: "in_stock" },
          { itemId: "item_b", itemName: "伏特加", spec: "箱", unitPriceCent: 90000, availability: "low" },
          { itemId: "item_c", itemName: "金酒", spec: "箱", unitPriceCent: 80000, availability: "out_of_stock" },
          { itemId: "item_d", itemName: "朗姆", spec: "箱", unitPriceCent: 70000, availability: "expected_restock", expectedRestockDate: "2026-11-20" },
        ];
        return [];
      }),
      getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
      post: vi.fn().mockResolvedValue({ id: "dlr_new", status: "approved", portalEnabled: true, version: 2, contractNo: "DC-X" }),
      put: vi.fn().mockResolvedValue({}),
      patch: vi.fn().mockResolvedValue({}),
      delete: vi.fn().mockResolvedValue({}),
    } as unknown as AdminApiClient;
  }
  function renderPage(api: AdminApiClient) {
    const setActionResult = vi.fn();
    const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult, setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
    render(<DealerAdmissionCatalogPage {...props} />);
    return setActionResult;
  }

  it("经销商状态全中文；审核中的经销商四项全勾才通过并启用门户", async () => {
    const api = makeApi();
    const setActionResult = renderPage(api);
    await screen.findByTestId("dealer-table");
    expect(screen.getByTestId("dealer-status-dlr_1").textContent).toBe("审核中");
    expect(screen.getByTestId("dealer-status-dlr_2").textContent).toBe("已通过");
    fireEvent.click(screen.getByTestId("dealer-select-dlr_1"));
    await screen.findByTestId("dealer-review-form");
    for (const k of ["companyProfileReviewed", "contractReviewed", "invoiceReviewed", "bankReviewed"]) {
      fireEvent.click(screen.getByTestId(`dealer-review-${k}`));
    }
    fireEvent.change(screen.getByTestId("dealer-review-note"), { target: { value: "四项均核" } });
    fireEvent.click(screen.getByTestId("dealer-review-approve"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/dlr_1/review", expect.objectContaining({
      decision: "approve", companyProfileReviewed: true, contractReviewed: true, invoiceReviewed: true, bankReviewed: true, note: "四项均核",
    })));
    await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "dealer_reviewed", 结论: "已通过", 门户: "已启用" })));
  });

  it("合同目录审批、询价审批与库存外显四态全中文，外显不含数量", async () => {
    const api = makeApi();
    renderPage(api);
    await screen.findByTestId("dealer-table");
    fireEvent.click(screen.getByTestId("dealer-select-dlr_2"));
    await screen.findByTestId("dealer-contract-table");
    fireEvent.click(screen.getByTestId("contract-select-dct_1"));
    await screen.findByTestId("catalog-table");
    expect(screen.getByTestId("catalog-status-dpc_1").textContent).toBe("待审批");
    expect(screen.getByTestId("stock-availability-item_a").textContent).toBe("有货");
    expect(screen.getByTestId("stock-availability-item_b").textContent).toBe("紧张");
    expect(screen.getByTestId("stock-availability-item_c").textContent).toBe("缺货");
    expect(screen.getByTestId("stock-availability-item_d").textContent).toBe("预计交期");
    expect(screen.getByTestId("external-stock-table").textContent).not.toMatch(/\d+\s*(件|箱|瓶)库存|数量/);
    expect(screen.getByTestId("inquiry-status-dqi_1").textContent).toBe("报价审批中");
    vi.spyOn(window, "prompt").mockReturnValue("同意");
    fireEvent.click(screen.getByTestId("catalog-approve-dpc_1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/price-catalogs/dpc_1/decision", expect.objectContaining({ decision: "approve", note: "同意" })));
    fireEvent.click(screen.getByTestId("inquiry-approve-dqi_1"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/inquiries/dqi_1/decision", expect.objectContaining({ decision: "approve" })));
    fireEvent.change(screen.getByTestId("catalog-lines-input"), { target: { value: "item_z,箱,1200,13,2026-12-01" } });
    fireEvent.click(screen.getByTestId("catalog-draft-create"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/contracts/dct_1/price-catalogs", expect.objectContaining({
      lines: [{ itemId: "item_z", spec: "箱", unitPriceCent: 120000, taxRateBps: 1300, expectedRestockDate: "2026-12-01" }],
    })));
  });
});
