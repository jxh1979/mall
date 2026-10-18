// 文件版本：V0.02
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
        if (url.endsWith("/admission")) return { dealerId: "dlr_1", companyAttachmentIds: ["company-1"], contractAttachmentIds: ["contract-1"], invoiceAttachmentIds: ["invoice-1"], bankAttachmentIds: ["bank-1"], submittedBy: "other-user" };
        if (url.endsWith("/contracts")) return [{ id: "dct_1", contractNo: "DC-2026-001", salesEntityId: "be_1", centralWarehouseId: "wh_main", validFrom: "2026-01-01", validTo: "2026-12-31", status: "active" }];
        if (url.endsWith("/portal-accounts")) return [{ userId: "usr_dlr_1", username: "dealer_north", status: "active", createdAt: "2026-10-17T00:00:00Z" }, { userId: "usr_dlr_disabled", username: "dealer_prebuilt", status: "disabled" }, { userId: "usr_dlr_2", username: "dealer_old", status: "revoked", createdAt: "2026-10-01T00:00:00Z", revokedAt: "2026-10-10T00:00:00Z" }];
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

  it("门户账号（第十一端）区块：状态全中文，开通调用门户账号端点", async () => {
    const api = makeApi();
    renderPage(api);
    await screen.findByTestId("dealer-table");
    fireEvent.click(screen.getAllByTestId(/^dealer-select-/)[0]!);
    await screen.findByTestId("dealer-portal-account-table");
    expect(screen.getByTestId("portal-account-status-usr_dlr_1").textContent).toBe("有效");
    expect(screen.getByTestId("portal-account-status-usr_dlr_2").textContent).toBe("已撤权");
    expect(screen.getByTestId("portal-account-status-usr_dlr_disabled").textContent).toBe("禁用待准入");
    fireEvent.change(screen.getByTestId("portal-username-input"), { target: { value: "dealer_new" } });
    fireEvent.change(screen.getByTestId("portal-password-input"), { target: { value: "Portal#2026" } });
    fireEvent.click(screen.getByTestId("portal-account-create"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(expect.stringMatching(/\/api\/v1\/dealers\/.+\/portal-accounts$/), expect.objectContaining({ username: "dealer_new", password: "Portal#2026" })));
  });
  it("四类真实上传资料绑定当前经销商，缺任一类不能送审", async () => {
    const api = makeApi();
    const oldGet = api.get;
    api.get = vi.fn(async (url: string) => {
      if (url === "/api/v1/dealers") return [{ id: "dlr_new", dealerName: "待审经销", status: "pending_submission", createdBy: "u1" }];
      if (url.endsWith("/admission")) return null;
      return oldGet(url);
    }) as AdminApiClient["get"];
    api.postForm = vi.fn(async (_url: string, form: FormData) => ({ id: `file-${form.get("attachment_role")}` })) as AdminApiClient["postForm"];
    renderPage(api);
    fireEvent.click(await screen.findByTestId("dealer-select-dlr_new"));
    await screen.findByTestId("admission-upload-company");
    fireEvent.click(screen.getByTestId("dealer-submit-dlr_new"));
    await screen.findByText("请选中该经销商并上传企业、合同、开票、银行四类资料后提交");
    expect(api.post).not.toHaveBeenCalled();
    for (const role of ["company", "contract", "invoice", "bank"]) {
      fireEvent.change(screen.getByTestId(`admission-upload-${role}`), { target: { files: [new File(["scan"], `${role}.png`, { type: "image/png" })] } });
      await waitFor(() => expect(screen.getByTestId(`admission-upload-${role}`)).not.toBeDisabled());
    }
    fireEvent.click(screen.getByTestId("dealer-submit-dlr_new"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/dealers/dlr_new/submit", expect.objectContaining({
      companyAttachmentIds: ["file-company"], contractAttachmentIds: ["file-contract"], invoiceAttachmentIds: ["file-invoice"], bankAttachmentIds: ["file-bank"],
    })));
    const firstUpload = vi.mocked(api.postForm!).mock.calls[0]![1];
    expect(firstUpload.get("business_type")).toBe("dealer_admission");
    expect(firstUpload.get("business_id")).toBe("dlr_new");
  });

  it("提交人即使有审核权限也不能在界面批准本人申请", async () => {
    const api = makeApi(); const oldGet = api.get;
    api.get = vi.fn(async (url: string) => url.endsWith("/admission") ? { dealerId: "dlr_1", companyAttachmentIds: ["c"], contractAttachmentIds: ["t"], invoiceAttachmentIds: ["i"], bankAttachmentIds: ["b"], submittedBy: "u1" } : oldGet(url)) as AdminApiClient["get"];
    renderPage(api);
    fireEvent.click(await screen.findByTestId("dealer-select-dlr_1"));
    await waitFor(() => expect(screen.getByTestId("dealer-review-approve")).toBeDisabled());
    expect(screen.getByTestId("dealer-review-reject")).toBeDisabled();
    expect(screen.getByRole("link", { name: "查看企业资料 1" })).toHaveAttribute("href", "/api/v1/evidence/attachments/c/file");
  });

});
