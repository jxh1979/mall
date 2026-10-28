// 文件版本：V0.02
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SettlementReviewPage } from "./SettlementReviewPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批第 2 条（ENUM 第 52 行「未核销的酒吧对应的分账金额从池中减除，单独列示……归属娱乐平台公司」；参与家数档位表缺该家数行时
// 该份第二段暂挂、补配后续算；「三者即使同归一个主体，也必须分三栏分别列示」）：月度结算单分列未分配池余额、
// 通票未核销减除额（归属主体）、分账尾差，暂挂另列。

const BILL = {
  id: "bill_pp", projectId: "p1", periodStart: "2026-04-01", periodEnd: "2026-04-30",
  ticketRevenueCent: 30000, channelFeeCent: 0, routeFeeCent: 0, compServiceFeePooledCent: 0, compServiceFeeExclusiveCent: 0,
  feeBeneficiaryEntityId: null, feeBeneficiaryEntityName: null,
  availablePoolCent: 30000, distributedCent: 17000, comboDiscountCent: 0, roundingResidualCent: 0,
  distributionMethod: "by_rank_tier", status: "confirmed",
  pooledDistributedCent: 30000,
  unverifiedDeductionCent: 4000, unverifiedDeductionEntityId: "be_ent", unverifiedDeductionEntityName: "娱乐平台公司",
  poolUndistributedCent: 500, poolSuspendedCent: 9000,
};
const TRANSACTION_ROW = {
  id: "s1", transactionNo: "ST-1", projectId: "p1", merchantId: "m1", exclusiveMerchantId: null,
  ticketTypeId: null, entitlementType: "entry", beneficiaryType: "merchant",
  verifiedCount: 1, numPeople: 1, verifiedAmountCent: 10000, settlementStatus: "draft", createdAt: "2026-04-15T10:00:00Z",
};

describe("SettlementReviewPage 通票未核销减除额与暂挂", () => {
  it("结算单分列未分配池余额、通票未核销减除额（含归属主体）与暂挂金额", async () => {
    const api = {
      get: vi.fn(async (url: string) => (url.startsWith("/api/v1/settlement/bills") ? [BILL] : [])),
      getPaged: vi.fn(async () => ({ items: [TRANSACTION_ROW], total: 1, page: 1, size: 20 })),
      post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), navigateTo: vi.fn(),
      effectivePermissions: { status: "ready", value: { grants: ["finance.settlement.view"], denies: [], wildcard: false } },
    };
    render(<SettlementReviewPage {...props} />);

    await waitFor(() => expect(screen.queryByTestId("settlement-bill-unverified-deduction-bill_pp")?.textContent ?? "")
      .toContain("40.00"));
    expect(screen.getByTestId("settlement-bill-unverified-deduction-bill_pp").textContent).toContain("娱乐平台公司");
    expect(screen.getByTestId("settlement-bill-suspended-bill_pp").textContent).toBe("90.00");
    expect(screen.getByTestId("settlement-bill-undistributed-bill_pp").textContent).toBe("5.00");
    expect(screen.getByTestId("settlement-review-bills-table").textContent).toContain("通票未核销减除额");
  });

  // 第 75 条（ENUM 第 52 行「超池的判定与审批一律在事前完成，结算环节不再设“超池待审批”」「超出部分由娱乐平台公司承担」）。
  it("超池结算单已确认、没有「超池特批」按钮，超池列写明由娱乐平台公司承担", async () => {
    const overPool = { ...BILL, id: "bill_op", pooledDistributedCent: 30500 };
    const api = {
      get: vi.fn(async (url: string) => (url.startsWith("/api/v1/settlement/bills") ? [overPool] : [])),
      getPaged: vi.fn(async () => ({ items: [TRANSACTION_ROW], total: 1, page: 1, size: 20 })),
      post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), navigateTo: vi.fn(),
      effectivePermissions: { status: "ready", value: { grants: [], denies: [], wildcard: true } },
    };
    render(<SettlementReviewPage {...props} />);

    await waitFor(() => expect(screen.queryByTestId("settlement-bill-overpool-amount-bill_op")?.textContent ?? "").toBe("5.00"));
    expect(screen.getByTestId("settlement-review-bills-table").textContent).toContain("超池（元，娱乐平台公司承担）");
    expect(screen.queryByText("超池特批")).toBeNull();
    expect(screen.queryByTestId("settlement-bill-approve-bill_op")).toBeNull();
    expect(screen.getByTestId("settlement-review-bills").textContent).toContain("结算不设超池待审批");
  });

  // 18B 复核后裁决第 11 条「对，下线两个选项（推荐）」：结算审阅页的差额规则列与「结转下期／回补当期池」说明一起删掉，差额一律留娱乐平台公司。
  it("月度结算单不再列差额规则，说明只写差额留在娱乐平台公司", async () => {
    const api = {
      get: vi.fn(async (url: string) => (url.startsWith("/api/v1/settlement/bills") ? [BILL] : [])),
      getPaged: vi.fn(async () => ({ items: [TRANSACTION_ROW], total: 1, page: 1, size: 20 })),
      post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
    } as unknown as AdminApiClient;
    const props: AdminPageProps = {
      api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
      setActionResult: vi.fn(), setGlobalError: vi.fn(), navigateTo: vi.fn(),
      effectivePermissions: { status: "ready", value: { grants: ["finance.settlement.view"], denies: [], wildcard: false } },
    };
    render(<SettlementReviewPage {...props} />);

    await waitFor(() => expect(screen.queryByTestId("settlement-review-bills")?.textContent ?? "").toContain("差额留在娱乐平台公司"));
    const bills = screen.getByTestId("settlement-review-bills").textContent ?? "";
    expect(bills).not.toContain("结转下期");
    expect(bills).not.toContain("回补当期池");
    expect(bills).not.toContain("差额规则");
    expect(screen.queryByTestId("settlement-bill-gift-handling-bill_pp")).toBeNull();
  });
});
