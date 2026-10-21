// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";

import { BuyTicketPage } from "./BuyTicketPage";
import type { CustomerService } from "../client/customerService";

// TK-06-R1（BSM:843「不设需要人脸照的票种；购票和转赠均不要求录入人脸照」）：即使旧后端仍下发
// requireFacePhoto=true，购票页也不出现任何人脸区块，提交载荷不带人脸照与人脸同意。
const AMUSEMENT_TICKET = {
  ticketTypeId: "t-amuse",
  ticketName: "游乐单人票",
  weekdayPriceCent: 9900,
  weekendPriceCent: 9900,
  holidayPriceCent: 9900,
  currency: "CNY",
  status: "published",
  requireFacePhoto: true,
  facePhotoRetentionDays: 30,
};

describe("BuyTicketPage 购票不要人脸照（TK-06）", () => {
  afterEach(() => vi.restoreAllMocks());

  it("游乐票购买页不出现人脸区块，提交载荷不含 facePhotos / faceConsent", async () => {
    const createTicketOrder = vi.fn().mockResolvedValue({ id: "o-1", orderNo: "TO-1", amountCent: 9900, currency: "CNY", payStatus: "unpaid" });
    const service = {
      listTicketProducts: vi.fn().mockResolvedValue([AMUSEMENT_TICKET]),
      createTicketOrder,
      createPaymentIntent: vi.fn().mockResolvedValue({ status: "pending" }),
    } as unknown as CustomerService;
    render(<BuyTicketPage service={service} />);

    await userEvent.click(screen.getByTestId("buy-ticket-type").querySelector("input") ?? screen.getByTestId("buy-ticket-type"));
    await waitFor(() => expect(screen.getByTitle("游乐单人票")).toBeTruthy());
    await userEvent.click(screen.getByTitle("游乐单人票"));

    await waitFor(() => expect(screen.queryByTestId("buy-ticket-face-photo")).toBeNull());
    await waitFor(() => expect(screen.getByTestId("buy-ticket-submit")).not.toBeDisabled());
    await userEvent.click(screen.getByTestId("buy-ticket-submit"));

    await waitFor(() => expect(createTicketOrder).toHaveBeenCalledTimes(1));
    const payload = createTicketOrder.mock.calls[0]?.[0] as Record<string, unknown>;
    expect(payload).not.toHaveProperty("facePhotos");
    expect(payload).not.toHaveProperty("faceConsent");
  });
});
