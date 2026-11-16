// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import { AdminApi } from "../client/adminClient";
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

// 第十八批 AM 段第 241 条 241-R2、241-R5（BSM:1803；第十八批裁决 55、65「门票退款打款」、68）：已通过、待原路打款的门票退款先发起付款确认，
// 全部审批通过后才能确认拨付。
describe("TicketOrdersPage 门票退款付款确认", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("待确认拨付的退款可打开付款确认面板，发起路径为该退款单的付款确认接口", async () => {
    const posts: { url: string; body: Record<string, unknown> }[] = [];
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      let data: unknown = [];
      if (init?.method === "POST") {
        posts.push({ url, body: JSON.parse(String(init.body)) as Record<string, unknown> });
        data = { confirmation_id: "pc_t", status: "pending_review" };
      } else if (url.includes("/api/v1/finance/payment-confirmations?")) {
        data = { payment_type: "ticket_refund", payment_type_label: "门票退款打款", required_line_codes: ["finance_review", "legal_review"], confirmations: [] };
      } else if (url.includes("/api/v1/ticketing/orders")) data = [{ id: "o1", order_no: "TO-1", pay_status: "paid", issue_status: "issued" }];
      else if (url.includes("/api/v1/ticketing/refunds")) {
        data = [{ id: "r-mix", target_id: "o1", refund_amount_cent: 5000, refund_channel: "original", status: "approved",
          payout_items: [{ channel: "wechat", amount_cent: 2000 }] }];
      }
      return new Response(JSON.stringify({ success: true, code: "OK", data }));
    }));
    const perms: EffectivePermissionsState = { status: "ready", value: { grants: ["ticket.refund.view", "ticket.refund.approve"], denies: [], wildcard: false } };
    render(<TicketOrdersPage api={new AdminApi("test")} user={{ id: "u1", username: "finance", accountType: "staff" }} setActionResult={vi.fn()}
      setGlobalError={vi.fn()} effectivePermissions={perms} />);
    await waitFor(() => expect(screen.queryByTestId("ticket-refund-payment-confirmation-r-mix")).not.toBeNull());
    fireEvent.click(screen.getByTestId("ticket-refund-payment-confirmation-r-mix"));
    await waitFor(() => expect(screen.queryByTestId("payment-confirmation-line-legal_review")).not.toBeNull());
    expect(screen.getByTestId("payment-confirmation-panel").textContent).toContain("门票退款打款");
    fireEvent.change(screen.getByTestId("payment-confirmation-reason"), { target: { value: "退票在线笔原路打款" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-final-approver"), { target: { value: "usr_boss" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-finance_review"), { target: { value: "usr_f" } });
    fireEvent.change(screen.getByTestId("payment-confirmation-line-legal_review"), { target: { value: "usr_l" } });
    fireEvent.click(screen.getByTestId("payment-confirmation-submit"));
    await waitFor(() => expect(posts).toHaveLength(1));
    expect(posts[0]!.url).toContain("/api/v1/ticketing/refunds/r-mix/payment-confirmations");
    expect(posts[0]!.body).toMatchObject({ reason: "退票在线笔原路打款", final_approver_id: "usr_boss" });
  });
});
