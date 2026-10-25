// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketTransferPage } from "./TicketTransferPage";
import type { CustomerService } from "@complex/customer-core";

describe("TicketTransferPage 副资源降级（批2）", () => {
  afterEach(() => vi.restoreAllMocks());

  it("我发起的转赠(转出)失败 → 我的票（主资源）照常，转出区块降级提示，无整页错误", async () => {
    const service = {
      listMyTickets: vi.fn().mockResolvedValue([
        { id: "t-1", parentTicketId: "p-1", slotIndex: 1, ticketName: "夜场通票", transferState: null },
      ]),
      listOutgoingTicketTransfers: vi.fn().mockRejectedValue(new Error("boom")),
      listIncomingTicketTransfers: vi.fn().mockResolvedValue([]),
    } as unknown as CustomerService;

    render(<TicketTransferPage service={service} />);

    // 主资源：我持有的可转赠票渲染
    await waitFor(() => expect(screen.getByText("夜场通票")).toBeInTheDocument());
    // 转出副资源失败 → 降级提示
    expect(screen.getByTestId("ticket-transfer-outgoing-degraded")).toBeInTheDocument();
    // 副资源失败不触发页面级“转赠信息加载失败”错误告警
    expect(screen.queryByText("转赠信息加载失败")).toBeNull();
  });
});
