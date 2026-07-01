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
    // 1.7: raw credential token must not be rendered as plaintext in the DOM (only inside the QR image).
    expect(screen.queryByTestId("qr-token")).toBeNull();
    expect(document.body.textContent).not.toContain("token-0");
    expect(screen.getByTestId("qr-refresh-count").textContent).toBe("0");
    expect(screen.getByTestId("qr-code").querySelector("svg")).not.toBeNull();

    // 30 秒短窗 -> 推进 30 秒触发自动在线刷新。
    await act(async () => {
      await vi.advanceTimersByTimeAsync(30_000);
    });
    expect(getTicketQr).toHaveBeenCalledTimes(2);
    expect(screen.queryByTestId("qr-token")).toBeNull();
    expect(document.body.textContent).not.toContain("token-1");
    expect(screen.getByTestId("qr-refresh-count").textContent).toBe("1");
  });
});
