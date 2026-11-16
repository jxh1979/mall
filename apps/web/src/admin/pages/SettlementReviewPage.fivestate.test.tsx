// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SettlementReviewPage } from "./SettlementReviewPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批 AL 段第 240 条 240-R2（ENUM:192「结算状态 草稿、待复核、已确认、已调整、已归档」；第十八批裁决 54「通票月结单、通票流水（随所属月结单）
// ……按五态显示」「顺带修掉流水恒显示草稿」；PGM:291「用户看到的每一处文字都是中文」）：流水表、流水详情与月结单表的「结算状态」按后端派生的
// settlement_state 显示五态中文；流水原值恒为 draft，不再显示成草稿。

const BILL = {
  id: "bill_fs", projectId: "p1", periodStart: "2026-04-01", periodEnd: "2026-04-30",
  ticketRevenueCent: 8000, channelFeeCent: 0, routeFeeCent: 0, compServiceFeePooledCent: 0, compServiceFeeExclusiveCent: 0,
  feeBeneficiaryEntityId: null, feeBeneficiaryEntityName: null,
  availablePoolCent: 3000, distributedCent: 3000, comboDiscountCent: 0, roundingResidualCent: 0,
  distributionMethod: "by_rank_tier", status: "confirmed", pooledDistributedCent: 3000, settlementState: "archived",
};
const ROW = {
  projectId: "p1", merchantId: "m1", exclusiveMerchantId: null, ticketTypeId: null, entitlementType: "entry", beneficiaryType: "merchant",
  verifiedCount: 1, numPeople: 1, verifiedAmountCent: 10000, settlementStatus: "draft", createdAt: "2026-04-15T10:00:00Z",
};
const ITEMS = [
  { ...ROW, id: "s1", transactionNo: "ST-1", settlementState: "confirmed" },
  { ...ROW, id: "s2", transactionNo: "ST-2", settlementState: "archived" },
];

function renderPage() {
  const api = {
    get: vi.fn(async (url: string) => (url.startsWith("/api/v1/settlement/bills") ? [BILL] : [])),
    getPaged: vi.fn(async () => ({ items: ITEMS, total: ITEMS.length, page: 1, size: 20 })),
    post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), navigateTo: vi.fn(),
    effectivePermissions: { status: "ready", value: { grants: ["finance.settlement.view"], denies: [], wildcard: false } },
  };
  render(<SettlementReviewPage {...props} />);
}

describe("SettlementReviewPage 结算五态", () => {
  it("流水与月结单的结算状态显示已确认、已归档，不再恒显示草稿，也不出英文码", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("settlement-review-row-s1").textContent).toContain("已确认"));
    await waitFor(() => expect(screen.getByTestId("settlement-review-bill-bill_fs").textContent).toContain("已归档"));
    expect(screen.getByTestId("settlement-review-row-s2").textContent).toContain("已归档");
    expect(screen.getByTestId("settlement-review-table").textContent).not.toContain("草稿");
    expect(within(screen.getByTestId("settlement-review-bills-table")).getAllByRole("columnheader").map((th) => th.textContent)).toContain("结算状态");
    for (const id of ["settlement-review-table", "settlement-review-bills-table"]) {
      expect(screen.getByTestId(id).textContent).not.toMatch(/draft|confirmed|archived/);
    }

    fireEvent.click(screen.getByTestId("settlement-review-row-s2"));
    await waitFor(() => expect(screen.getByTestId("row-detail-drawer").textContent).toContain("已归档"));
    expect(screen.getByTestId("row-detail-drawer").textContent).not.toContain("草稿");
  });
});
