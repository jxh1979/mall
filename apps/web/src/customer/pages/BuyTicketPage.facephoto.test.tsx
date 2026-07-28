import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BuyTicketPage } from "./BuyTicketPage";
import type { CustomerService } from "../client/customerService";

const FACE_TICKET = {
  ticketTypeId: "t-amuse",
  ticketName: "游乐单人票",
  weekdayPriceCent: 9900,
  weekendPriceCent: 9900,
  holidayPriceCent: 9900,
  currency: "CNY",
  status: "published",
  requireFacePhoto: true,
  facePhotoRetentionDays: 45,
};

const PLAIN_TICKET = { ...FACE_TICKET, ticketTypeId: "t-plain", ticketName: "全场通用日票", requireFacePhoto: false, facePhotoRetentionDays: null };

function makeService(products: unknown[], createTicketOrder = vi.fn()): CustomerService {
  return {
    listTicketProducts: vi.fn().mockResolvedValue(products),
    createTicketOrder,
    createPaymentIntent: vi.fn().mockResolvedValue({ status: "pending" }),
  } as unknown as CustomerService;
}

async function pickTicket(label: string) {
  await userEvent.click(screen.getByTestId("buy-ticket-type").querySelector("input") ?? screen.getByTestId("buy-ticket-type"));
  await userEvent.click(await screen.findByTitle(label));
}

describe("BuyTicketPage 入场人脸照(C1)", () => {
  afterEach(() => vi.restoreAllMocks());

  it("需拍照的票种给出用途与留存期限的中文告知，未同意不能开摄像头、不能提交", async () => {
    render(<BuyTicketPage service={makeService([FACE_TICKET])} />);
    await pickTicket("游乐单人票");

    const notice = await screen.findByTestId("buy-ticket-face-notice");
    expect(notice.textContent).toContain("比对");
    // 留存期限必须是后台当前生效值（45），写死 30 会在这里红。
    expect(notice.textContent).toContain("45 天");

    expect(screen.getByTestId("buy-ticket-face-open-camera")).toBeDisabled();
    expect(screen.getByTestId("buy-ticket-submit")).toBeDisabled();

    await userEvent.click(screen.getByTestId("buy-ticket-face-consent"));
    await waitFor(() => expect(screen.getByTestId("buy-ticket-face-open-camera")).not.toBeDisabled());
    // 同意了但一张没拍，仍不能提交。
    expect(screen.getByTestId("buy-ticket-submit")).toBeDisabled();
  });

  it("不需拍照的票种一个字都不问客人要人脸照", async () => {
    render(<BuyTicketPage service={makeService([PLAIN_TICKET])} />);
    await pickTicket("全场通用日票");
    await waitFor(() => expect(screen.queryByTestId("buy-ticket-face-photo")).toBeNull());
    expect(screen.getByTestId("buy-ticket-submit")).not.toBeDisabled();
  });

  it("浏览器不支持摄像头时给出中文兜底提示，不抛异常", async () => {
    render(<BuyTicketPage service={makeService([FACE_TICKET])} />);
    await pickTicket("游乐单人票");
    await userEvent.click(await screen.findByTestId("buy-ticket-face-consent"));
    await userEvent.click(screen.getByTestId("buy-ticket-face-open-camera"));

    const cameraError = await screen.findByTestId("buy-ticket-face-camera-error");
    expect(cameraError.textContent).toContain("不支持调用摄像头");
  });
});
