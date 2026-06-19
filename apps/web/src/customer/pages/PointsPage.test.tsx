import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { PointsPage } from "./PointsPage";
import type { CustomerService } from "../client/customerService";

describe("PointsPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("renders points summary, level and P1 placeholder", async () => {
    const service = {
      getPoints: vi.fn().mockResolvedValue({ customerId: "c-1", balance: 320, level: "黄金", recentSummary: null }),
      listCheckIns: vi.fn().mockResolvedValue([
        { id: "ci-1", customerId: "c-1", checkInDate: "2026-06-18", streak: 3, awardedPoints: 30, sourceTerminal: "customer-web", createdAt: "" },
      ]),
    } as unknown as CustomerService;

    render(<PointsPage service={service} />);

    await waitFor(() => expect(screen.getByText("320")).toBeInTheDocument());
    expect(screen.getByText("黄金")).toBeInTheDocument();
    expect(screen.getByTestId("points-p1-placeholder")).toBeInTheDocument();
    expect(screen.getByText(/连续 3 天/)).toBeInTheDocument();
  });
});
