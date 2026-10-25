// 文件版本：V0.03
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// TK-05（BSM:804「只有票种规则明确允许、活动或场次取消、平台或履约方无法提供服务、重复扣款、法律法规要求这五种情形才允许退款」
// 「五种情形之外只能由持授权 U 盘密钥的最高权限逐笔放行」「通票安全例外……由夜班值班经理及以上批准」；BSM:806 记录退款原因、责任主体）。
const REFUNDS = [
  { id: "r-req", targetId: "o1", refundAmountCent: 9900, status: "requested", reasonCode: "master_override", responsibleEntityId: "be_x", requestedBy: "usr_a" },
  { id: "r-ok", targetId: "o1", refundAmountCent: 9900, status: "approved", reasonCode: "event_cancelled" },
];
const EVENTS = [
  { id: "tse-1", parentTicketId: "tkt_1", slotIndex: 2, reason: "情绪失控", situation: "场内冲突", outcome: "refund", status: "pending", requestedBy: "usr_a" },
  { id: "tse-2", parentTicketId: "tkt_2", slotIndex: 1, reason: "受伤", situation: "游乐中扭伤", outcome: "restore", status: "approved", approvedBy: "usr_night", approverBasis: "night_duty" },
];

function perms(grants: string[]): EffectivePermissionsState {
  return { status: "ready", value: { grants, denies: [], wildcard: false } };
}

function renderPage(grants: string[]) {
  const post = vi.fn().mockResolvedValue({});
  const api = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes("/ticketing/orders")) return Promise.resolve([{ id: "o1", orderNo: "TO-1", payStatus: "paid", issueStatus: "issued" }]);
      if (url.includes("/ticketing/refunds")) return Promise.resolve(REFUNDS);
      if (url.includes("/ticketing/safety-events")) return Promise.resolve(EVENTS);
      return Promise.resolve([]);
    }),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "staff", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: perms(grants),
  };
  render(<TicketOrdersPage {...props} />);
  return post;
}

afterEach(() => vi.restoreAllMocks());

describe("TicketOrdersPage 退款理由闭集与通票安全例外（TK-05）", () => {
  it("发起退款提交理由类别、责任主体与所选票；票种规则之外未填责任主体不提交", async () => {
    const post = renderPage(["ticket.refund.request", "ticket.refund.view"]);
    await waitFor(() => expect(screen.queryByTestId("ticket-refund-request-form")).not.toBeNull());

    await userEvent.selectOptions(screen.getByTestId("ticket-refund-reason-code"), "event_cancelled");
    await userEvent.type(screen.getByTestId("ticket-refund-reason"), "演出取消");
    await userEvent.click(screen.getByTestId("ticket-refund-request"));
    await waitFor(() => expect(screen.getByTestId("ticket-orders-error").textContent).toContain("责任主体"));
    expect(post).not.toHaveBeenCalled();

    await userEvent.type(screen.getByTestId("ticket-refund-responsible-entity"), "be_x");
    await userEvent.type(screen.getByTestId("ticket-refund-instance-ids"), "tkt_1, tkt_2");
    await userEvent.click(screen.getByTestId("ticket-refund-request"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/refunds", {
      targetId: "o1", reasonCode: "event_cancelled", responsibleEntityId: "be_x", ticketInstanceIds: ["tkt_1", "tkt_2"], reason: "演出取消",
    }));
    // 通票安全例外不在普通理由下拉里（只走安全事件审批）。
    expect(screen.getByTestId("ticket-refund-reason-code").textContent).not.toContain("通票安全例外");
  });

  it("票种规则明确允许的退款也须填责任主体，未填不提交（BSM:806 完整记录责任主体）", async () => {
    const post = renderPage(["ticket.refund.request", "ticket.refund.view"]);
    await waitFor(() => expect(screen.queryByTestId("ticket-refund-request-form")).not.toBeNull());

    await userEvent.selectOptions(screen.getByTestId("ticket-refund-reason-code"), "ticket_type_rule");
    await userEvent.type(screen.getByTestId("ticket-refund-reason"), "客人改期");
    await userEvent.click(screen.getByTestId("ticket-refund-request"));
    await waitFor(() => expect(screen.queryByTestId("ticket-orders-error")?.textContent ?? "").toContain("责任主体"));
    expect(post).not.toHaveBeenCalled();
  });

  it("待审批退款显示理由类别，审批人填意见后批准或驳回", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const post = renderPage(["ticket.refund.approve", "ticket.refund.view"]);
    await waitFor(() => expect(screen.queryByTestId("ticket-refund-pending-row-r-req")).not.toBeNull());
    expect(screen.getByTestId("ticket-refund-pending-row-r-req").textContent).toContain("最高权限逐笔放行");
    expect(screen.queryByTestId("ticket-refund-pending-row-r-ok")).toBeNull();
    // 2026-10-25 操作员更正撤回客人自助退票：退款都由员工发起时填责任主体，批准处不再设补填栏。
    expect(screen.queryByTestId("ticket-refund-decision-entity")).toBeNull();

    await userEvent.click(screen.getByTestId("ticket-refund-approve-r-req"));
    await waitFor(() => expect(screen.getByTestId("ticket-orders-error").textContent).toContain("审批意见"));
    expect(post).not.toHaveBeenCalled();

    await userEvent.type(screen.getByTestId("ticket-refund-decision-note"), "逐笔放行");
    await userEvent.click(screen.getByTestId("ticket-refund-approve-r-req"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/refunds/r-req/approve", { action: "approve", reason: "逐笔放行" }));
    await waitFor(() => expect(screen.getByTestId("ticket-refund-reject-r-req")).not.toBeDisabled());
    await userEvent.click(screen.getByTestId("ticket-refund-reject-r-req"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/refunds/r-req/reject", { action: "reject", reason: "逐笔放行" }));
  });

  it("登记通票安全事件带人份与当时情形；待批准事件可批准，已批准事件显示批准人与依据", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const post = renderPage(["ticket.refund.request", "ticket.refund.approve", "ticket.refund.view"]);
    await waitFor(() => expect(screen.queryByTestId("ticket-safety-event-row-tse-1")).not.toBeNull());
    expect(screen.getByTestId("ticket-safety-event-row-tse-2").textContent).toContain("当班夜班值班经理");
    expect(screen.getByTestId("ticket-safety-event-row-tse-2").textContent).toContain("恢复本次 1 次完整补偿使用机会");
    expect(screen.queryByTestId("ticket-safety-approve-tse-2")).toBeNull();

    await userEvent.type(screen.getByTestId("ticket-safety-instance"), "tkt_1");
    await userEvent.clear(screen.getByTestId("ticket-safety-slot"));
    await userEvent.type(screen.getByTestId("ticket-safety-slot"), "2");
    await userEvent.type(screen.getByTestId("ticket-safety-reason"), "情绪失控");
    await userEvent.type(screen.getByTestId("ticket-safety-situation"), "场内冲突");
    await userEvent.type(screen.getByTestId("ticket-safety-entity"), "be_amusement");
    await userEvent.click(screen.getByTestId("ticket-safety-request"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/safety-events", {
      ticketInstanceId: "tkt_1", slotIndex: 2, reason: "情绪失控", situation: "场内冲突", responsibleEntityId: "be_amusement",
    }));

    await userEvent.type(screen.getByTestId("ticket-refund-decision-note"), "核实属实");
    await waitFor(() => expect(screen.getByTestId("ticket-safety-approve-tse-1")).not.toBeDisabled());
    await userEvent.click(screen.getByTestId("ticket-safety-approve-tse-1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/safety-events/tse-1/approve", { action: "approve", reason: "核实属实" }));
  });
});
