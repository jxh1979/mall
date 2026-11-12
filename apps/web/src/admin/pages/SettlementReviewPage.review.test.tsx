// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SettlementReviewPage } from "./SettlementReviewPage";
import type { AdminApiClient, AdminPageProps } from "../types";

// 第十八批 AI 段 237-R3（BSM 第 1754 行「通票结算审阅财务侧设置独立审阅台账，按项目、经营域、责任主体和实际提供方列明第三方渠道费用、域间份额、
// 域内分配、内部结算价权益、酒吧入场可分配池和责任主体承担的差额」）：月度结算单每行「审阅明细」，展开经营域份额、域内分配与按实际提供方的内部结算价权益三表。

const BILL = {
  id: "bill_rv", projectId: "p1", periodStart: "2026-04-01", periodEnd: "2026-04-30",
  ticketRevenueCent: 8000, channelFeeCent: 640, routeFeeCent: 80, compServiceFeePooledCent: 0, compServiceFeeExclusiveCent: 0,
  feeBeneficiaryEntityId: null, feeBeneficiaryEntityName: null,
  availablePoolCent: 3000, distributedCent: 3000, comboDiscountCent: 0, roundingResidualCent: 0,
  distributionMethod: "by_rank_tier", status: "confirmed", pooledDistributedCent: 3000,
};
const TRANSACTION_ROW = {
  id: "s1", transactionNo: "ST-1", projectId: "p1", merchantId: "m1", exclusiveMerchantId: null,
  ticketTypeId: null, entitlementType: "entry", beneficiaryType: "merchant",
  verifiedCount: 1, numPeople: 1, verifiedAmountCent: 10000, settlementStatus: "draft", createdAt: "2026-04-15T10:00:00Z",
};
const REVIEW = {
  domainShares: [
    {
      businessFormat: "catering", beneficiaryKind: "entity", beneficiaryEntityId: "be_city", beneficiaryEntityName: "城市项目公司甲",
      grossRevenueCent: 3000, channelFeeCent: 240, routeFeeCent: 30, compServiceFeeCent: 0, giftCostCent: 0, giftActualPaidCent: 0,
      netRevenueCent: 2730, interEntityNetCent: 2730, unverifiedNetCent: 0, internalSettledCent: 2000, differenceCent: 730,
    },
    {
      businessFormat: "amusement", beneficiaryKind: "entity", beneficiaryEntityId: "be_amusement", beneficiaryEntityName: "游乐公司",
      grossRevenueCent: 3000, channelFeeCent: 240, routeFeeCent: 30, compServiceFeeCent: 0, giftCostCent: 0, giftActualPaidCent: 0,
      netRevenueCent: 2730, interEntityNetCent: 910, unverifiedNetCent: 1820, internalSettledCent: 0, differenceCent: null,
    },
  ],
  allocations: [
    {
      merchantId: "rest1", merchantName: "直付餐厅", businessFormat: "catering", distributionMethod: "exclusive_full",
      verifiedCount: 2, guestCount: 2, rankNo: null, finalAmountCent: 4000, singleBarRouteFeeCent: 0, singleBarChannelFeeCent: 0, status: "draft",
    },
    {
      merchantId: "bar1", merchantName: "单店酒吧", businessFormat: "bar", distributionMethod: "exclusive_full",
      verifiedCount: 1, guestCount: 1, rankNo: null, finalAmountCent: 4550, singleBarRouteFeeCent: 50, singleBarChannelFeeCent: 400, status: "posted",
    },
  ],
  internalPriceByProvider: [
    { merchantId: "bar1", merchantName: "单店酒吧", businessFormat: "bar", giftDrinkPaidCent: 800, paidDirectSettledCent: 0, freeDirectSettledCent: 0 },
    { merchantId: "rest1", merchantName: "直付餐厅", businessFormat: "catering", giftDrinkPaidCent: 0, paidDirectSettledCent: 2000, freeDirectSettledCent: 2000 },
  ],
};

function renderPage(review: unknown, items: unknown[] = [TRANSACTION_ROW]) {
  const get = vi.fn(async (url: string) => {
    if (url === "/api/v1/settlement/bills/bill_rv/review") return review;
    return url.startsWith("/api/v1/settlement/bills") ? [BILL] : [];
  });
  const api = {
    get,
    getPaged: vi.fn(async () => ({ items, total: items.length, page: 1, size: 20 })),
    post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn(),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api, user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), navigateTo: vi.fn(),
    effectivePermissions: { status: "ready", value: { grants: ["finance.settlement.view"], denies: [], wildcard: false } },
  };
  render(<SettlementReviewPage {...props} />);
  return get;
}

async function openReview() {
  await waitFor(() => expect(screen.queryByTestId("settlement-bill-review-bill_rv")).not.toBeNull());
  await waitFor(() => expect(screen.queryByTestId("settlement-review-row-s1")).not.toBeNull());
  fireEvent.click(screen.getByTestId("settlement-bill-review-bill_rv"));
}

describe("SettlementReviewPage 结算单审阅明细", () => {
  it("按经营域、责任主体和实际提供方列明域间份额、域内分配与内部结算价权益", async () => {
    const get = renderPage(REVIEW);
    await openReview();

    await waitFor(() => expect(screen.queryByTestId("settlement-review-detail")?.textContent ?? "").toContain("城市项目公司甲"));
    expect(get).toHaveBeenCalledWith("/api/v1/settlement/bills/bill_rv/review");
    const shares = screen.getByTestId("settlement-review-domain-shares").textContent ?? "";
    for (const head of ["经营域", "责任主体", "第三方渠道费用（元）", "域间份额（元）", "到期未核销归属（元）", "内部结算价直付（元）", "责任主体承担的差额（元）"]) {
      expect(shares).toContain(head);
    }
    expect(screen.getByTestId("settlement-share-difference-catering").textContent).toBe("7.30");
    expect(screen.getByTestId("settlement-share-difference-amusement").textContent).toBe("—");
    expect(screen.getByTestId("settlement-share-inter-entity-amusement").textContent).toBe("9.10");
    expect(screen.getByTestId("settlement-share-unverified-amusement").textContent).toBe("18.20");
    expect(screen.getByTestId("settlement-share-format-catering").textContent).toBe("餐饮");

    const allocations = screen.getByTestId("settlement-review-allocations").textContent ?? "";
    for (const head of ["实际提供方", "分配方式", "单店门票销售渠道费（元）", "单店门票通路费（元）"]) {
      expect(allocations).toContain(head);
    }
    expect(allocations).toContain("专属直付");
    expect(allocations).toContain("已入账");
    expect(screen.getByTestId("settlement-allocation-channel-fee-bar1-exclusive_full").textContent).toBe("4.00");
    expect(screen.getByTestId("settlement-allocation-route-fee-bar1-exclusive_full").textContent).toBe("0.50");

    expect(screen.getByTestId("settlement-review-detail").textContent).toContain("内部结算价权益（按实际提供方）");
    expect(screen.getByTestId("settlement-review-internal-price").textContent).toContain("赠饮内部结算价应付（元）");
    expect(screen.getByTestId("settlement-provider-gift-bar1").textContent).toBe("8.00");
    expect(screen.getByTestId("settlement-provider-paid-rest1").textContent).toBe("20.00");
    expect(screen.getByTestId("settlement-provider-free-rest1").textContent).toBe("20.00");
  });

  it("本单没有份额、分配与内部结算价权益时逐表提示", async () => {
    renderPage({ domainShares: [], allocations: [], internalPriceByProvider: [] });
    await openReview();

    await waitFor(() => expect(screen.queryByTestId("settlement-review-detail")?.textContent ?? "").toContain("本单没有经营域份额"));
    const detail = screen.getByTestId("settlement-review-detail").textContent ?? "";
    expect(detail).toContain("本单没有域内分配明细");
    expect(detail).toContain("本单没有内部结算价权益");
  });
});

// 第十八批 AI 段 236-R1（BSM 第 1754 行「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」；ENUM 第 54 行「价格变更形成新版本并只对后续生效，
// 核销或结算时锁定当时版本。」）：结算流水详情列出核销所用内部结算价版本（目录行编号），不按内部结算价结算的显示「—」。
function drawerValue(label: string): string | null {
  const term = Array.from(screen.getByTestId("row-detail-drawer").querySelectorAll("dt")).find((node) => node.textContent === label);
  return term?.nextElementSibling?.textContent ?? null;
}

describe("SettlementReviewPage 结算流水内部结算价版本", () => {
  it("流水详情显示核销所用内部结算价版本，没有版本的显示「—」", async () => {
    renderPage(REVIEW, [
      { ...TRANSACTION_ROW, id: "s_v", transactionNo: "ST-V", settlementCatalogEntryId: "ipc_d_236" },
      { ...TRANSACTION_ROW, id: "s_n", transactionNo: "ST-N", settlementCatalogEntryId: null },
    ]);
    await waitFor(() => expect(screen.queryByTestId("settlement-bill-review-bill_rv")).not.toBeNull());
    await waitFor(() => expect(screen.queryByTestId("settlement-review-row-s_n")).not.toBeNull());

    fireEvent.click(screen.getByTestId("settlement-review-row-s_v"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-drawer")?.textContent ?? "").toContain("内部结算价版本"));
    expect(drawerValue("内部结算价版本")).toBe("ipc_d_236");

    fireEvent.click(screen.getByTestId("row-detail-close"));
    fireEvent.click(screen.getByTestId("settlement-review-row-s_n"));
    await waitFor(() => expect(screen.queryByTestId("row-detail-title")?.textContent ?? "").toContain("ST-N"));
    expect(drawerValue("内部结算价版本")).toBe("—");
  });
});
