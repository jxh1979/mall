// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// ROOT-03-R20（BSM 第 1749 行「混合支付的退款按收款相反顺序逐笔原路退回……储值部分回冲仍按先赠送金后本金的既有顺序」）：
// 储值付一部分、在线补足的订单退票时储值份额已当场回卡，确认拨付页只按待打款明细（渠道×金额）打款，不按整笔金额。
const REFUNDS = [
  { id: "r-mix", targetId: "o1", refundAmountCent: 5000, refundChannel: "original", status: "approved",
    payoutItems: [{ channel: "wechat", amountCent: 2000 }] },
  { id: "r-old", targetId: "o1", refundAmountCent: 3000, refundChannel: "original", status: "approved" },
];

function renderPage() {
  const api = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes("/ticketing/orders")) return Promise.resolve([{ id: "o1", orderNo: "TO-1", payStatus: "paid", issueStatus: "issued" }]);
      if (url.includes("/ticketing/refunds")) return Promise.resolve(REFUNDS);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
  const perms: EffectivePermissionsState = { status: "ready", value: { grants: ["ticket.refund.view", "ticket.refund.approve"], denies: [], wildcard: false } };
  const props: AdminPageProps = { api, user: { id: "u1", username: "finance", accountType: "staff" }, setActionResult: vi.fn(),
    setGlobalError: vi.fn(), effectivePermissions: perms };
  render(<TicketOrdersPage {...props} />);
}

afterEach(() => vi.restoreAllMocks());

describe("TicketOrdersPage 确认拨付按待打款明细", () => {
  it("组合支付退款只显示在线笔待打款金额，并注明储值部分已回卡", async () => {
    renderPage();
    await waitFor(() => expect(screen.queryByTestId("ticket-refund-payout-r-mix")?.textContent ?? "").toContain("待打款"));
    const text = screen.queryByTestId("ticket-refund-payout-r-mix")?.textContent ?? "";
    expect(text).toContain("微信");
    expect(text).toContain("20.00");
    expect(text).toContain("储值");
    expect(text).toContain("30.00");
    expect(text).toContain("已回卡");
    // 未分笔的旧退款单仍按整笔与退款方式显示。
    expect(screen.queryByTestId("ticket-refund-payout-r-old")).toBeNull();
  });
});
