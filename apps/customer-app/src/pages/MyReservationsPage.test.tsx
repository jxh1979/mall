// 文件版本：V0.01
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { platform } from "../platform";
import { MyReservationsPage } from "./MyReservationsPage";

// BSM「订卡客人可在 App/小程序查看自己的订卡邀请函二维码（注明酒吧、卡座、可进人数），转发给朋友……作为开卡人，可以在 App/小程序上看到
// 已接受邀请的名单与各自是否已入场，并可把尚未入场的被邀请人移出邀请函、收回其名额……被邀请人一旦完成核销入场，开卡人不得再将其移出」；
// 第十三批裁决第 4 条「酒吧员工确认后签发」；BSM「该名额自动关联到接受人本人的动态入场码」（开卡人本人同样凭动态入场码入场）。
const VIEW = { reservationId: "res-1", barId: "bar-1", barName: "一号酒吧", tableId: "t-a", tableName: "A01", tableType: "card_seat",
  reservedAt: "2026-10-25T12:00:00Z", latestArrivalAt: "2026-10-25T13:00:00Z", guestCount: 4, depositAmountCent: 20000,
  depositStatus: "paid", status: "reserved", invitationStatus: "active" };
const NOT_ISSUED = { ...VIEW, reservationId: "res-2", tableName: "A02", invitationStatus: "none" };
const UNPAID = { ...VIEW, reservationId: "res-3", tableName: "A03", depositStatus: "pending", invitationStatus: "none" };
const INVITATION = { reservationId: "res-1", barId: "bar-1", tableId: "t-a", invitationToken: "inv_abc", invitationSignature: "sig123",
  capacity: 6, status: "active", issuedAt: "2026-10-24T10:00:00Z", confirmedCount: 3, enteredCount: 1, remaining: 3,
  members: [
    { id: "m-h", isHolder: true, status: "confirmed", memberCustomerId: "c-1" },
    { id: "m-1", isHolder: false, status: "confirmed" },
    { id: "m-2", isHolder: false, status: "entered" },
    { id: "m-3", isHolder: false, status: "revoked" },
  ] };

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    listMyBarReservations: vi.fn().mockResolvedValue([VIEW, NOT_ISSUED, UNPAID]),
    getMyBarInvitation: vi.fn().mockResolvedValue(INVITATION),
    removeBarInvitationMember: vi.fn().mockResolvedValue({ id: "m-1", status: "revoked" }),
    getBarInvitationEntryCode: vi.fn().mockResolvedValue({ reservationId: "res-1", memberId: "m-h", code: "ENTRY-CODE-1",
      expiresAt: "2026-10-25T12:00:30Z", refreshIntervalSeconds: 30 }),
    startDepositPayment: vi.fn(),
    confirmDepositPayment: vi.fn(),
    ...overrides,
  };
}

async function openInvitation(service: ReturnType<typeof makeService>) {
  render(<MyReservationsPage service={service as unknown as CustomerService} />);
  await waitFor(() => expect(screen.queryByTestId("invitation-open-res-1")).not.toBeNull());
  await waitFor(() => expect(screen.getByTestId("invitation-open-res-1")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("invitation-open-res-1"));
  await waitFor(() => expect(screen.queryByTestId("invitation-detail")?.textContent ?? "").toContain("一号酒吧"));
}

describe("MyReservationsPage 我的订台与邀请函", () => {
  it("listsReservationsAndInvitationIssuedByBar：列出本人订台；未签发的写明由酒吧确认后签发，待付定金的可付定金", async () => {
    render(<MyReservationsPage service={makeService() as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("reservation-item-res-1")?.textContent ?? "").toContain("一号酒吧"));
    expect(screen.getByTestId("reservation-item-res-1").textContent).toContain("A01");
    expect(screen.getByTestId("reservation-item-res-1").textContent).toContain("定金已支付");
    expect(screen.getByTestId("reservation-item-res-2").textContent).toContain("邀请函待酒吧确认订台后签发");
    expect(screen.queryByTestId("invitation-open-res-2")).toBeNull();
    expect(within(screen.getByTestId("reservation-item-res-3")).getByTestId("deposit-pay")).toBeInTheDocument();
    expect(within(screen.getByTestId("reservation-item-res-1")).queryByTestId("deposit-pay")).toBeNull();
  });

  it("holderSharesInvitationDeepLink：邀请函注明酒吧、卡座、可进人数，分享的深链只带邀请函码载荷", async () => {
    const share = vi.spyOn(platform, "share").mockResolvedValue("shared");
    const service = makeService();
    await openInvitation(service);
    const detail = screen.getByTestId("invitation-detail").textContent ?? "";
    for (const text of ["一号酒吧", "A01", "可进 6 人", "剩余 3 个名额"]) expect(detail).toContain(text);
    expect(service.getMyBarInvitation).toHaveBeenCalledWith("res-1");
    expect(within(screen.getByTestId("invitation-detail")).getByTestId("invitation-qr")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTestId("invitation-share")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("invitation-share"));
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1));
    expect(share.mock.calls[0]![0].link).toBe("https://c.complex.test/c/invitation?token=inv_abc&sig=sig123&table_id=t-a&capacity=6");
  });

  it("removeOnlyNotEnteredInvitee：名单显示是否已入场；只有尚未入场的被邀请人可移出", async () => {
    const service = makeService();
    await openInvitation(service);
    expect(screen.getByTestId("invitation-member-m-2").textContent).toContain("已入场");
    expect(screen.getByTestId("invitation-member-m-3").textContent).toContain("已取消");
    expect(screen.getByTestId("invitation-member-m-h").textContent).toContain("开卡人");
    expect(screen.queryByTestId("invitation-remove-m-2")).toBeNull();
    expect(screen.queryByTestId("invitation-remove-m-3")).toBeNull();
    expect(screen.queryByTestId("invitation-remove-m-h")).toBeNull();
    await waitFor(() => expect(screen.getByTestId("invitation-remove-m-1")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("invitation-remove-m-1"));
    await waitFor(() => expect(service.removeBarInvitationMember).toHaveBeenCalledWith("res-1", "m-1"));
    await waitFor(() => expect(service.getMyBarInvitation).toHaveBeenCalledTimes(2));
  });

  it("holderEntryCodeIsDynamic：开卡人本人的入场码按服务端有效期显示的动态码，不显示码值文字", async () => {
    const service = makeService();
    await openInvitation(service);
    await waitFor(() => expect(service.getBarInvitationEntryCode).toHaveBeenCalledWith("res-1"));
    await waitFor(() => expect(screen.queryByTestId("qr-countdown")?.textContent ?? "").toContain("30 秒后失效"));
    expect(document.body.textContent).not.toContain("ENTRY-CODE-1");
  });
});
