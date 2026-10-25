// 文件版本：V0.03
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PointsPage } from "./PointsPage";
import type { CustomerService } from "@complex/customer-core";

describe("PointsPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows ticket use facts and disables redemption while points remain recoverable", async () => {
    const service = {
      getPoints: vi.fn().mockResolvedValue({ customerId: "c-1", balance: 320, level: "黄金" }),
      listTicketRewards: vi.fn().mockResolvedValue({ pendingRecovery: 40, rewards: [{ id: "reward1", parentTicketId: "ticket1", slotIndex: 2, useIndex: 3, points: 100, growth: 7, waitingHours: 24, availableAt: "2026-10-20T08:00:00Z", status: "pending" }] }),
      listTicketRewardRules: vi.fn().mockResolvedValue([{ id: "r1", ticketName: "游乐日票", projectName: "长沙项目", kind: "points", fixedValue: 100, version: "V0.01", effectiveAt: "2026-10-20T00:00:00Z", publishedAt: "2026-10-18T00:00:00Z" }]),
      listPointsLedger: vi.fn().mockResolvedValue([]),
      listMerchantRewards: vi.fn().mockResolvedValue([]),
      listCheckIns: vi.fn().mockResolvedValue([]), listRedemptions: vi.fn().mockResolvedValue([]),
      listRedemptionItems: vi.fn().mockResolvedValue([{ id: "gift", itemName: "礼品", itemType: "gift", pointsCost: 10, stockQuantity: 5, status: "active" }]),
    } as unknown as CustomerService;
    render(<PointsPage service={service} />);
    await waitFor(() => expect(screen.getByTestId("points-recovery")).toHaveTextContent("待追回 40 积分"));
    expect(screen.getByText(/长沙项目 · 游乐日票/)).toBeInTheDocument();
    expect(screen.getByText(/第 2 人份 · 第 3 次/)).toHaveTextContent("100 积分 · 7 成长值");
    expect(screen.getByTestId("redeem-gift")).toBeDisabled();
    expect(screen.getByText(/原等待期至/)).toHaveTextContent("退款处理期间不发放");
  });

  it("renders points summary, level, check-in and redemption mall", async () => {
    const service = {
      listTicketRewards: vi.fn().mockResolvedValue({ rewards: [], pendingRecovery: 0 }),
      listTicketRewardRules: vi.fn().mockResolvedValue([]),
      listPointsLedger: vi.fn().mockResolvedValue([]),
      listMerchantRewards: vi.fn().mockResolvedValue([]),
      getPoints: vi.fn().mockResolvedValue({ customerId: "c-1", balance: 320, level: "黄金", recentSummary: null }),
      listCheckIns: vi.fn().mockResolvedValue([
        { id: "ci-1", customerId: "c-1", checkInDate: "2026-06-18", streak: 3, awardedPoints: 30, sourceTerminal: "customer-web", createdAt: "" },
      ]),
      listRedemptionItems: vi.fn().mockResolvedValue([
        { id: "ri-1", itemName: "精酿啤酒", itemType: "酒水", pointsCost: 100, stockQuantity: 5, status: "active" },
      ]),
      listRedemptions: vi.fn().mockResolvedValue([]),
    } as unknown as CustomerService;

    render(<PointsPage service={service} />);

    await waitFor(() => expect(screen.getByText("320")).toBeInTheDocument());
    expect(screen.getByText("黄金")).toBeInTheDocument();
    expect(screen.getByTestId("points-redemption-catalog")).toBeInTheDocument();
    expect(screen.getByText("精酿啤酒")).toBeInTheDocument();
    expect(screen.getByText(/连续 3 天/)).toBeInTheDocument();
  });

  // 第十一批 TK-04-R5（BSM L877 客人端只展示结果；ENUM L77「从消费单据可以反查该单产生的积分与成长值记录」）。
  it("lists merchant consumption rewards with merchant, source bill number and waiting status", async () => {
    const service = {
      getPoints: vi.fn().mockResolvedValue({ customerId: "c-1", balance: 0, level: "黄金" }),
      listTicketRewards: vi.fn().mockResolvedValue({ rewards: [], pendingRecovery: 0 }),
      listTicketRewardRules: vi.fn().mockResolvedValue([]),
      listPointsLedger: vi.fn().mockResolvedValue([]),
      listMerchantRewards: vi.fn().mockResolvedValue([{ id: "mcr1", sourceType: "bar_bill", sourceId: "bill1", sourceNo: "BILL-001", merchantName: "一号酒吧", points: 15, growth: 150, status: "pending", completedAt: "2026-10-20T14:00:00Z", availableAt: "2026-10-21T14:00:00Z", confirmedAt: null }]),
      listCheckIns: vi.fn().mockResolvedValue([]), listRedemptions: vi.fn().mockResolvedValue([]), listRedemptionItems: vi.fn().mockResolvedValue([]),
    } as unknown as CustomerService;
    render(<PointsPage service={service} />);
    await waitFor(() => expect(screen.getByTestId("merchant-rewards")).toHaveTextContent("一号酒吧 · 单号 BILL-001 · 15 积分 · 150 成长值"));
    expect(screen.getByTestId("merchant-rewards")).toHaveTextContent("待确认 · 等待期至");
    expect(screen.getByTestId("merchant-rewards")).toHaveTextContent("退款处理期间不发放");
  });
});
