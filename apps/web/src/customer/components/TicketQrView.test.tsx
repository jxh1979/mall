import { render, screen, act } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { TicketQrView } from "./TicketQrView";
import type { CustomerService } from "../client/customerService";

function makeCredential(refreshedCount: number) {
  return {
    id: `cred-${refreshedCount}`,
    ticketId: "ticket-1",
    orderId: "order-1",
    credentialToken: `token-${refreshedCount}`,
    credentialType: "qr",
    issuedAt: "2026-06-18T00:00:00Z",
    expiresAt: "2026-06-18T00:00:30Z",
    refreshedCount,
  };
}

describe("TicketQrView refresh", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("loads the credential and re-fetches when the countdown elapses", async () => {
    const getTicketQr = vi
      .fn()
      .mockResolvedValueOnce(makeCredential(0))
      .mockResolvedValueOnce(makeCredential(1));
    const service = { getTicketQr } as unknown as CustomerService;

    render(<TicketQrView service={service} ticketId="ticket-1" />);

    // 初次挂载即拉取凭证。
    await act(async () => {
      await Promise.resolve();
    });
    expect(getTicketQr).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("qr-token").textContent).toBe("token-0");

    // 30 秒短窗 -> 推进 30 秒触发自动在线刷新。
    await act(async () => {
      await vi.advanceTimersByTimeAsync(30_000);
    });
    expect(getTicketQr).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId("qr-token").textContent).toBe("token-1");
  });
});
