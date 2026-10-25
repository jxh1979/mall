// 文件版本：V0.03
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PointsPage } from "./PointsPage";
import type { CustomerService } from "@complex/customer-core";

describe("PointsPage 副资源降级（批2）", () => {
  afterEach(() => vi.restoreAllMocks());

  it("兑换目录失败 → 积分余额（主资源）照常，积分商城显示降级提示，无整页错误", async () => {
    const service = {
      listTicketRewards: vi.fn().mockResolvedValue({ rewards: [], pendingRecovery: 0 }),
      listTicketRewardRules: vi.fn().mockResolvedValue([]),
      listPointsLedger: vi.fn().mockResolvedValue([]),
      listMerchantRewards: vi.fn().mockResolvedValue([]),
      getPoints: vi.fn().mockResolvedValue({ customerId: "c-1", balance: 320, level: "黄金", recentSummary: null }),
      listCheckIns: vi.fn().mockResolvedValue([]),
      listRedemptionItems: vi.fn().mockRejectedValue(new Error("boom")),
      listRedemptions: vi.fn().mockResolvedValue([]),
    } as unknown as CustomerService;

    render(<PointsPage service={service} />);

    // 主资源：积分余额照常渲染
    await waitFor(() => expect(screen.getByText("320")).toBeInTheDocument());
    // 兑换目录副资源失败 → 积分商城降级提示
    expect(screen.getByTestId("points-items-degraded")).toBeInTheDocument();
    // 副资源失败不触发页面级“积分加载失败”错误告警
    expect(screen.queryByText("积分加载失败")).toBeNull();
  });
});
