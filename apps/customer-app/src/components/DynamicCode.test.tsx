// 文件版本：V0.01
import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { DynamicCode } from "./DynamicCode";

// BSM「凡是出示出来给人验证的二维码……有效期（刷新间隔）由后台统一设置、以 5 秒为单位可调、最长 60 秒；截图、转发出去的旧码过了有效期就作废」；
// BSM「二维码原始值不存在手机本地，只在线核验」。
const ISSUED = "2026-10-24T00:00:00Z";
const credential = (n: number, seconds: number) => ({
  credentialToken: `token-${n}`,
  issuedAt: ISSUED,
  expiresAt: new Date(Date.parse(ISSUED) + seconds * 1000).toISOString(),
});
type Credential = ReturnType<typeof credential>;

function defineVisibility(state: DocumentVisibilityState): void {
  Object.defineProperty(document, "visibilityState", { configurable: true, get: () => state });
}

function setVisibility(state: DocumentVisibilityState): void {
  defineVisibility(state);
  document.dispatchEvent(new Event("visibilitychange"));
}

function renderCode(fetchCode: () => Promise<Credential>) {
  return render(<DynamicCode title="验票二维码" fetchCode={fetchCode} valueOf={(c) => c.credentialToken} testId="ticket-qr-view" />);
}

const pending = () => new Promise<Credential>(() => undefined);

describe("DynamicCode（三端统一动态码）", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    defineVisibility("visible");
    vi.useRealTimers();
  });

  it("fiveSecondWindowHidesCodeAtExpiryAndRefetches", async () => {
    const fetchCode = vi.fn<() => Promise<Credential>>().mockResolvedValueOnce(credential(1, 5)).mockImplementationOnce(pending);
    renderCode(fetchCode);

    await vi.waitFor(() => expect(screen.getByTestId("qr-code").querySelector("svg")).not.toBeNull());
    // 码值只画进码图，不以文字出现在页面上。
    expect(document.body.textContent).not.toContain("token-1");

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5_000);
    });
    expect(screen.queryByTestId("qr-code")).toBeNull();
    expect(fetchCode).toHaveBeenCalledTimes(2);
  });

  it("refreshFailureHidesPreviousCode", async () => {
    const fetchCode = vi.fn<() => Promise<Credential>>()
      .mockResolvedValueOnce(credential(1, 5))
      .mockRejectedValueOnce(new Error("网络中断"));
    renderCode(fetchCode);

    await vi.waitFor(() => expect(screen.getByTestId("qr-code")).toBeInTheDocument());
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5_000);
    });
    expect(screen.queryByTestId("qr-code")).toBeNull();
    expect(screen.getByTestId("qr-hidden").textContent).toContain("二维码已隐藏，请联网后重试");
  });

  it("visibilityHiddenClearsCodeAndResumeRefetchesFirst", async () => {
    let releaseSecond: (value: Credential) => void = () => undefined;
    const fetchCode = vi.fn<() => Promise<Credential>>()
      .mockResolvedValueOnce(credential(1, 30))
      .mockImplementationOnce(() => new Promise<Credential>((resolve) => { releaseSecond = resolve; }));
    renderCode(fetchCode);

    await vi.waitFor(() => expect(screen.getByTestId("qr-code")).toBeInTheDocument());
    act(() => setVisibility("hidden"));
    expect(screen.queryByTestId("qr-code")).toBeNull();

    // 回前台：先取新码，取到之前不显示任何码（旧码不再出现）。
    act(() => setVisibility("visible"));
    expect(fetchCode).toHaveBeenCalledTimes(2);
    expect(screen.queryByTestId("qr-code")).toBeNull();
    await act(async () => {
      releaseSecond(credential(2, 30));
      await vi.advanceTimersByTimeAsync(0);
    });
    expect(screen.getByTestId("qr-code")).toBeInTheDocument();
  });

  it("neverWritesCodeToAnyStorage", async () => {
    const setItem = vi.spyOn(Storage.prototype, "setItem");
    const fetchCode = vi.fn<() => Promise<Credential>>().mockResolvedValueOnce(credential(1, 5)).mockResolvedValueOnce(credential(2, 5));
    renderCode(fetchCode);

    await vi.waitFor(() => expect(screen.getByTestId("qr-code")).toBeInTheDocument());
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5_000);
    });
    expect(fetchCode).toHaveBeenCalledTimes(2);
    expect(setItem).not.toHaveBeenCalled();
  });
});
