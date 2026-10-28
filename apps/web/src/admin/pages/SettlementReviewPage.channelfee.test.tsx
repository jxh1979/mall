// 文件版本：V0.02
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SettlementReviewPage } from "./SettlementReviewPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批第 3 条（BSM 第 749 行「渠道费用分两层……官方渠道售出的票不扣销售渠道费，但仍扣通路费」）：月度结算单把通路费与销售渠道费分两列。

const BILL = {
  id: "bill_rf", projectId: "p1", periodStart: "2026-04-01", periodEnd: "2026-04-30",
  ticketRevenueCent: 10000, channelFeeCent: 0, routeFeeCent: 60, compServiceFeePooledCent: 0, compServiceFeeExclusiveCent: 0,
  feeBeneficiaryEntityId: null, feeBeneficiaryEntityName: null,
  availablePoolCent: 9940, distributedCent: 9940, comboDiscountCent: 0, roundingResidualCent: 0,
  distributionMethod: "by_rank_tier", status: "confirmed",
  pooledDistributedCent: 9940,
};
const TRANSACTION_ROW = {
  id: "s1", transactionNo: "ST-1", projectId: "p1", merchantId: "m1", exclusiveMerchantId: null,
  ticketTypeId: null, entitlementType: "entry", beneficiaryType: "merchant",
  verifiedCount: 1, numPeople: 1, verifiedAmountCent: 10000, settlementStatus: "draft", createdAt: "2026-04-15T10:00:00Z",
};

describe("SettlementReviewPage 渠道费用两层", () => {
  it("结算单分列通路费与销售渠道费：官方渠道月份通路费 0.60、销售渠道费 0.00", async () => {
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

    await waitFor(() => expect(screen.queryByTestId("settlement-bill-route-fee-bill_rf")?.textContent).toBe("0.60"));
    expect(screen.getByTestId("settlement-bill-sales-fee-bill_rf").textContent).toBe("0.00");
    expect(screen.getByTestId("settlement-review-bills-table").textContent).toContain("销售渠道费");
  });
});
