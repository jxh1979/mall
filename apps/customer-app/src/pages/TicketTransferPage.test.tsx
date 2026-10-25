// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { platform } from "../platform";
import { TicketTransferPage } from "./TicketTransferPage";

// BSM「发起转赠后不冻结该权益：在接收人点击“确认领取”之前，原持有人仍然可以自己使用这份权益，也可以再次转赠给其他人」；
// BSM「转出时，系统须同时向转出人与受赠人明示这一场次限制。受赠人打开转赠页面时，若该人份权益所绑定的场次已经结束，页面须明确提示
// 「该权益所在场次已结束、权益已终结」，此时不发生领取、「确认领取」不可点击」；BSM「仅打开转赠页面不算领取」。
const HELD = { id: "e-1", parentTicketId: "pt-1", slotIndex: 2, ticketName: "夜场双人票", transferState: null, status: "active", numPeople: 2 };
const LINK_TRANSFER = { id: "tf-1", parentTicketId: "pt-1", slotIndex: 2, channel: "link", status: "pending", claimCode: "CLAIM123",
  ticketName: "夜场双人票", createdAt: "2026-10-24T12:00:00Z", sessionRestricted: true, sessionLabel: "2026-10-24", sessionEnded: false };
const PREVIEW = { transferId: "tf-1", parentTicketId: "pt-1", slotIndex: 2, channel: "link", status: "pending", ticketName: "夜场双人票",
  numPeople: 2, totalTimes: 1, sessionRestricted: true, sessionLabel: "2026-10-24", sessionEnded: false };

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    listMyTickets: vi.fn().mockResolvedValue([HELD]),
    listOutgoingTicketTransfers: vi.fn().mockResolvedValue([]),
    listIncomingTicketTransfers: vi.fn().mockResolvedValue([]),
    initiateTicketTransfer: vi.fn().mockResolvedValue(LINK_TRANSFER),
    previewTicketTransfer: vi.fn().mockResolvedValue(PREVIEW),
    claimTicketTransfer: vi.fn().mockResolvedValue({ ...LINK_TRANSFER, status: "claimed" }),
    revokeTicketTransfer: vi.fn(),
    ...overrides,
  };
}

function renderPage(service: ReturnType<typeof makeService>, initialClaimCode?: string) {
  render(<TicketTransferPage service={service as unknown as CustomerService} initialClaimCode={initialClaimCode} />);
}

async function initiateByLink() {
  await waitFor(() => expect(screen.queryByText("微信链接 / 二维码")).not.toBeNull());
  await waitFor(() => expect(screen.getByText("微信链接 / 二维码")).toBeInTheDocument());
  fireEvent.click(screen.getByText("微信链接 / 二维码"));
  await waitFor(() => expect(screen.queryByTestId("ticket-transfer-initiate")).not.toBeNull());
  await waitFor(() => expect(screen.getByTestId("ticket-transfer-initiate")).not.toBeDisabled());
  fireEvent.click(screen.getByTestId("ticket-transfer-initiate"));
}

describe("TicketTransferPage 发起不冻结、场次限制明示、深链只预览", () => {
  it("pendingTransferDoesNotFreezeAndCanRetransfer：转赠待领取的票不冻结，可改转他人", async () => {
    const service = makeService({ listMyTickets: vi.fn().mockResolvedValue([{ ...HELD, transferState: "transferring" }]) });
    renderPage(service);
    await waitFor(() => expect(screen.queryByTestId("ticket-transfer-slot")?.textContent ?? "").toContain("转赠待领取"));
    expect(screen.getByTestId("ticket-transfer-initiate").textContent).toContain("改转他人");
    expect(document.body.textContent).not.toContain("冻结");
    await initiateByLink();
    await waitFor(() => expect(service.initiateTicketTransfer).toHaveBeenCalledWith({
      parentTicketId: "pt-1", slotIndex: 2, channel: "link", receiverPhone: undefined,
    }));
  });

  it("transferPageShowsSessionLimitAndSharesDeepLinkWithoutToken：转出时向转出人明示场次限制，分享的是不带令牌的领取深链", async () => {
    const share = vi.spyOn(platform, "share").mockResolvedValue("shared");
    const service = makeService();
    renderPage(service);
    await initiateByLink();
    await waitFor(() => expect(screen.queryByTestId("transfer-session-limit")?.textContent ?? "").toContain("2026-10-24"));
    await waitFor(() => expect(screen.queryByTestId("ticket-transfer-share")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("ticket-transfer-share")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("ticket-transfer-share"));
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1));
    const { link } = share.mock.calls[0]![0];
    expect(link).toBe("https://c.complex.test/c/ticket-claim?claim_code=CLAIM123");
    expect(link).not.toMatch(/token/i);
  });

  it("claimDisabledWhenSessionEnded：深链打开只预览不领取；场次已结束时明确提示且「确认领取」不可点", async () => {
    const service = makeService({ previewTicketTransfer: vi.fn().mockResolvedValue({ ...PREVIEW, sessionEnded: true }) });
    renderPage(service, "CLAIM123");
    await waitFor(() => expect(screen.queryByTestId("ticket-transfer-preview")?.textContent ?? "").toContain("该权益所在场次已结束、权益已终结"));
    expect(service.previewTicketTransfer).toHaveBeenCalledWith("CLAIM123");
    expect(screen.getByTestId("ticket-transfer-claim")).toBeDisabled();
    expect(service.claimTicketTransfer).not.toHaveBeenCalled();
  });

  it("claimPreviewThenConfirm：受赠人预览时也明示场次限制，点「确认领取」才领取", async () => {
    const service = makeService();
    renderPage(service, "CLAIM123");
    await waitFor(() => expect(screen.queryByTestId("ticket-transfer-preview")?.textContent ?? "").toContain("2026-10-24"));
    expect(service.claimTicketTransfer).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByTestId("ticket-transfer-claim")).not.toBeNull());
    await waitFor(() => expect(screen.getByTestId("ticket-transfer-claim")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("ticket-transfer-claim"));
    await waitFor(() => expect(service.claimTicketTransfer).toHaveBeenCalledWith("tf-1", "CLAIM123"));
  });

  it("scanClaimQrPreviewsOnly：扫对方出示的领取二维码只认门票领取链接，扫到即预览、不领取；别的码提示无法识别", async () => {
    const scan = vi.spyOn(platform, "scanCode")
      .mockResolvedValueOnce("https://c.complex.test/c/voucher-claim?claim_code=CLAIM123")
      .mockResolvedValueOnce("https://c.complex.test/c/ticket-claim?claim_code=CLAIM123");
    const service = makeService();
    renderPage(service);
    await waitFor(() => expect(screen.queryByTestId("ticket-transfer-scan-claim")).not.toBeNull());
    fireEvent.click(screen.getByTestId("ticket-transfer-scan-claim"));
    await waitFor(() => expect(document.body.textContent).toContain("无法识别的二维码"));
    expect(service.previewTicketTransfer).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.getByTestId("ticket-transfer-scan-claim")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("ticket-transfer-scan-claim"));
    await waitFor(() => expect(screen.queryByTestId("ticket-transfer-preview")?.textContent ?? "").toContain("夜场双人票"));
    expect(service.previewTicketTransfer).toHaveBeenCalledWith("CLAIM123");
    expect(service.claimTicketTransfer).not.toHaveBeenCalled();
    expect(scan).toHaveBeenCalledTimes(2);
  });

  it("incomingTransferClaimDisabledWhenSessionEnded：按手机号转来的、场次已结束的不能确认领取", async () => {
    const service = makeService({ listIncomingTicketTransfers: vi.fn().mockResolvedValue([{ ...LINK_TRANSFER, id: "tf-2", channel: "phone",
      claimCode: null, sessionEnded: true }]) });
    renderPage(service);
    await waitFor(() => expect(screen.queryByTestId("ticket-transfer-incoming")?.textContent ?? "").toContain("该权益所在场次已结束、权益已终结"));
    expect(screen.getByTestId("ticket-transfer-incoming-claim")).toBeDisabled();
  });
});
