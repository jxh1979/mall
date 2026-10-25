// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { PointsPage } from "./PointsPage";

// BSM「查看会员积分摘要和会员等级；积分明细、礼物列表、兑换发起和兑换记录」；PGM「界面全中文（所有终端零英文）」。
describe("PointsPage 积分明细", () => {
  it("pointsLedgerShown：逐笔列出积分变动（来源中文、增减、变动后余额、待确认），不显示后端英文说明", async () => {
    const service = {
      getPoints: vi.fn().mockResolvedValue({ customerId: "c-1", balance: 245, level: "黄金" }),
      listPointsLedger: vi.fn().mockResolvedValue([
        { id: "l1", customerId: "c-1", pointsDelta: 10, balanceAfter: 330, sourceType: "check_in", sourceId: "ci-1",
          description: "check-in streak 3", changeType: "check_in", status: "confirmed", createdAt: "2026-10-20T01:00:00Z" },
        { id: "l2", customerId: "c-1", pointsDelta: -100, balanceAfter: 230, sourceType: "points_redemption", sourceId: "r-1",
          description: "Redeemed 精酿啤酒", changeType: "points_redemption", status: "confirmed", createdAt: "2026-10-21T01:00:00Z" },
        { id: "l3", customerId: "c-1", pointsDelta: 15, balanceAfter: 245, sourceType: "earn", sourceId: "mcr-1",
          description: "商户消费待确认积分", changeType: "earn", status: "pending", createdAt: "2026-10-22T01:00:00Z" },
      ]),
      listTicketRewards: vi.fn().mockResolvedValue({ rewards: [], pendingRecovery: 0 }),
      listTicketRewardRules: vi.fn().mockResolvedValue([]),
      listMerchantRewards: vi.fn().mockResolvedValue([]),
      listCheckIns: vi.fn().mockResolvedValue([]),
      listRedemptions: vi.fn().mockResolvedValue([]),
      listRedemptionItems: vi.fn().mockResolvedValue([]),
    } as unknown as CustomerService;
    render(<PointsPage service={service} />);
    await waitFor(() => expect(screen.queryByTestId("points-ledger")?.textContent ?? "").toContain("签到"));
    const ledger = screen.getByTestId("points-ledger").textContent ?? "";
    for (const text of ["签到 +10", "余额 330", "积分兑换 -100", "获得积分 +15", "待确认"]) expect(ledger).toContain(text);
    expect(ledger).not.toMatch(/check-in|Redeemed/);
  });
});
