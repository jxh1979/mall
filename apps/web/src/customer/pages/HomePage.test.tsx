import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { HomePage } from "./HomePage";
import type { CustomerService } from "../client/customerService";

describe("HomePage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("renders ticket products from the service", async () => {
    const service = {
      listTicketProducts: vi.fn().mockResolvedValue([
        {
          ticketTypeId: "t-1",
          ticketName: "平日票",
          weekdayPriceCent: 9900,
          weekendPriceCent: 12900,
          holidayPriceCent: 15900,
          currency: "CNY",
          status: "active",
        },
      ]),
    } as unknown as CustomerService;

    render(<HomePage service={service} />);

    await waitFor(() => expect(screen.getByText("平日票")).toBeInTheDocument());
    expect(screen.getByText(/平日 ¥99.00/)).toBeInTheDocument();
  });

  it("shows an error alert when loading fails", async () => {
    const service = {
      listTicketProducts: vi.fn().mockRejectedValue(new Error("network down")),
    } as unknown as CustomerService;

    render(<HomePage service={service} />);

    await waitFor(() => expect(screen.getByText("network down")).toBeInTheDocument());
  });
});
