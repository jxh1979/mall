// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { platform } from "../platform";
import { InvitationAcceptPage } from "./InvitationAcceptPage";

// BSM「朋友在 App/小程序确认接受后占一个名额，该名额自动关联到接受人本人的动态入场码，到店出示本人的动态入场码入场」；
// BSM「邀请函二维码只用于转发与接受，本身不作为入场凭证（邀请函二维码带防伪标记，被复制或篡改卡座、可进人数即无法接受）」。
const LINK = { token: "inv_abc", sig: "sig123", table_id: "t-a", capacity: "6" };
const CODE = { invitationToken: "inv_abc", invitationSignature: "sig123", tableId: "t-a", capacity: 6 };
const PREVIEW = { reservationId: "res-1", barId: "bar-1", barName: "一号酒吧", tableId: "t-a", tableName: "A01",
  reservedAt: "2026-10-25T12:00:00Z", capacity: 6, remaining: 3, holderClaimable: false };

function makeService(overrides: Record<string, unknown> = {}) {
  return {
    previewBarInvitation: vi.fn().mockResolvedValue(PREVIEW),
    confirmBarInvitation: vi.fn().mockResolvedValue({ id: "m-9", reservationId: "res-1", status: "confirmed", isHolder: false }),
    getBarInvitationEntryCode: vi.fn().mockResolvedValue({ reservationId: "res-1", memberId: "m-9", code: "ENTRY-9",
      expiresAt: "2026-10-25T12:00:15Z", refreshIntervalSeconds: 15 }),
    ...overrides,
  };
}

describe("InvitationAcceptPage 接受邀请", () => {
  it("previewThenAcceptShowsOwnEntryCode：深链打开只预览不占名额，点「确认接受」后出示本人动态入场码", async () => {
    const service = makeService();
    render(<InvitationAcceptPage service={service as unknown as CustomerService} initialLink={LINK} />);
    await waitFor(() => expect(screen.queryByTestId("invitation-preview")?.textContent ?? "").toContain("一号酒吧"));
    const text = screen.getByTestId("invitation-preview").textContent ?? "";
    for (const item of ["A01", "可进 6 人", "剩余 3 个名额"]) expect(text).toContain(item);
    expect(service.previewBarInvitation).toHaveBeenCalledWith(CODE);
    expect(service.confirmBarInvitation).not.toHaveBeenCalled();
    // 订台已有订台人（客人自助订台）：只有「确认接受」，只能作参与者。
    expect(screen.queryByTestId("invitation-accept-holder")).toBeNull();
    expect(screen.getByTestId("invitation-accept").textContent).toContain("确认接受");
    await waitFor(() => expect(screen.getByTestId("invitation-accept")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("invitation-accept"));
    await waitFor(() => expect(service.confirmBarInvitation).toHaveBeenCalledWith(CODE, false));
    await waitFor(() => expect(service.getBarInvitationEntryCode).toHaveBeenCalledWith("res-1"));
    await waitFor(() => expect(screen.queryByTestId("qr-countdown")?.textContent ?? "").toContain("15 秒后失效"));
    expect(document.body.textContent).not.toContain("ENTRY-9");
  });

  // 第十八批 R 段第 152 条 152-R3（第十八批裁决 34「销售可以通过微信等发送邀请函，客户点击后确定自己是订台人还是参与者」；BSM「邀请函上的人分两种身份：
  // 开卡人（订下这个卡座的人）与被邀请人（接受邀请占用名额的人）」）：员工代订、一体机订台还没有订台人，打开邀请函给两个身份选项。
  it("holderClaimableChooseHolder：订台还没有订台人时给「我是订台人」「我是参与者」，选订台人后提示已成为订台人并出示本人动态入场码", async () => {
    const service = makeService({
      previewBarInvitation: vi.fn().mockResolvedValue({ ...PREVIEW, holderClaimable: true }),
      confirmBarInvitation: vi.fn().mockResolvedValue({ id: "m-1", reservationId: "res-1", status: "confirmed", isHolder: true }),
    });
    render(<InvitationAcceptPage service={service as unknown as CustomerService} initialLink={LINK} />);
    await waitFor(() => expect(screen.queryByTestId("invitation-accept-holder")?.textContent ?? "").toContain("我是订台人"));
    expect(screen.getByTestId("invitation-accept").textContent).toContain("我是参与者");
    expect(screen.getByTestId("invitation-preview").textContent).toContain("这张订台还没有订台人");
    await waitFor(() => expect(screen.getByTestId("invitation-accept-holder")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("invitation-accept-holder"));
    await waitFor(() => expect(service.confirmBarInvitation).toHaveBeenCalledWith(CODE, true));
    await waitFor(() => expect(document.body.textContent).toContain("你已成为这张订台的订台人"));
    expect(document.body.textContent).toContain("可在「我的订台」查看名单、转发邀请函");
    await waitFor(() => expect(service.getBarInvitationEntryCode).toHaveBeenCalledWith("res-1"));
    await waitFor(() => expect(screen.queryByTestId("qr-countdown")?.textContent ?? "").toContain("15 秒后失效"));
  });

  it("holderClaimableChooseParticipant：订台还没有订台人时选「我是参与者」按参与者接受", async () => {
    const service = makeService({ previewBarInvitation: vi.fn().mockResolvedValue({ ...PREVIEW, holderClaimable: true }) });
    render(<InvitationAcceptPage service={service as unknown as CustomerService} initialLink={LINK} />);
    await waitFor(() => expect(screen.queryByTestId("invitation-accept")?.textContent ?? "").toContain("我是参与者"));
    await waitFor(() => expect(screen.getByTestId("invitation-accept")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("invitation-accept"));
    await waitFor(() => expect(service.confirmBarInvitation).toHaveBeenCalledWith(CODE, false));
    await waitFor(() => expect(document.body.textContent).toContain("已接受邀请，名额已为你保留"));
    expect(document.body.textContent).not.toContain("你已成为这张订台的订台人");
  });

  it("scanForeignCodeRejected：扫到不是本平台邀请函的码提示无法识别，不发起预览", async () => {
    vi.spyOn(platform, "scanCode").mockResolvedValue("https://evil.example/c/invitation?token=inv_abc&sig=sig123&table_id=t-a&capacity=6");
    const service = makeService();
    render(<InvitationAcceptPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("invitation-scan")).not.toBeNull());
    fireEvent.click(screen.getByTestId("invitation-scan"));
    await waitFor(() => expect(screen.queryByTestId("invitation-error")?.textContent ?? "").toContain("无法识别的二维码"));
    expect(service.previewBarInvitation).not.toHaveBeenCalled();
  });

  it("scanInvitationCodePreviews：扫开卡人转发的邀请函码即预览", async () => {
    vi.spyOn(platform, "scanCode").mockResolvedValue("https://c.complex.test/c/invitation?token=inv_abc&sig=sig123&table_id=t-a&capacity=6");
    const service = makeService();
    render(<InvitationAcceptPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("invitation-scan")).not.toBeNull());
    fireEvent.click(screen.getByTestId("invitation-scan"));
    await waitFor(() => expect(service.previewBarInvitation).toHaveBeenCalledWith(CODE));
  });

  it("tamperedInvitationCannotBeAccepted：防伪核验不过时提示原因，不出现「确认接受」", async () => {
    const service = makeService({ previewBarInvitation: vi.fn().mockRejectedValue(new Error("邀请函已被复制或篡改，无法接受")) });
    render(<InvitationAcceptPage service={service as unknown as CustomerService} initialLink={LINK} />);
    await waitFor(() => expect(screen.queryByTestId("invitation-error")?.textContent ?? "").toContain("邀请函已被复制或篡改"));
    expect(screen.queryByTestId("invitation-accept")).toBeNull();
  });
});
