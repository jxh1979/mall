// 文件版本：V0.01
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { codeWindowMs, createDynamicCode, type DynamicCodeState } from "./dynamicCode";

// BSM「有效期（刷新间隔）由后台统一设置、以 5 秒为单位可调、最长 60 秒；截图、转发出去的旧码过了有效期就作废」。
const ISSUED = "2026-10-24T00:00:00Z";
const plus = (seconds: number) => new Date(Date.parse(ISSUED) + seconds * 1000).toISOString();

describe("codeWindowMs（显示时长只取服务端值）", () => {
  it("windowFollowsServerExpiryFor5_20_25_45_60Seconds", () => {
    for (const seconds of [5, 20, 25, 45, 60]) {
      expect(codeWindowMs({ issuedAt: ISSUED, expiresAt: plus(seconds) })).toBe(seconds * 1000);
    }
  });

  it("usesRefreshIntervalSecondsWhenPresent", () => {
    expect(codeWindowMs({ issuedAt: ISSUED, expiresAt: plus(60), refreshIntervalSeconds: 35 })).toBe(35_000);
  });

  it("没有有效的到期信息就不给显示时长", () => {
    expect(codeWindowMs({})).toBe(0);
    expect(codeWindowMs({ issuedAt: plus(10), expiresAt: ISSUED })).toBe(0);
  });
});

describe("createDynamicCode（到期隐藏、失败隐藏、切后台隐藏）", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  const code = (n: number, seconds: number) => ({ value: `code-${n}`, issuedAt: ISSUED, expiresAt: plus(seconds) });
  type Code = ReturnType<typeof code>;

  it("hidesAtExpiryThenRefetches：5 秒窗口到点即隐藏旧码并取新码", async () => {
    const fetchCode = vi.fn<() => Promise<Code>>().mockResolvedValueOnce(code(1, 5)).mockResolvedValueOnce(code(2, 5));
    const states: Array<DynamicCodeState<Code>> = [];
    const controller = createDynamicCode(fetchCode, (s) => states.push(s));

    controller.start();
    await vi.advanceTimersByTimeAsync(0);
    expect(states.at(-1)).toMatchObject({ status: "shown", code: { value: "code-1" }, secondsLeft: 5 });

    await vi.advanceTimersByTimeAsync(4_999);
    expect(fetchCode).toHaveBeenCalledTimes(1);
    expect(states.at(-1)).toMatchObject({ status: "shown", secondsLeft: 1 });

    await vi.advanceTimersByTimeAsync(1);
    expect(fetchCode).toHaveBeenCalledTimes(2);
    expect(states).toContainEqual({ status: "loading" });
    await vi.advanceTimersByTimeAsync(0);
    expect(states.at(-1)).toMatchObject({ status: "shown", code: { value: "code-2" } });
    controller.stop();
  });

  it("refreshFailureHidesPreviousCode：到期后取码失败，不再显示上一张码", async () => {
    const fetchCode = vi.fn<() => Promise<Code>>().mockResolvedValueOnce(code(1, 5)).mockRejectedValueOnce(new Error("网络中断"));
    const states: Array<DynamicCodeState<Code>> = [];
    const controller = createDynamicCode(fetchCode, (s) => states.push(s));

    controller.start();
    await vi.advanceTimersByTimeAsync(5_000);
    await vi.advanceTimersByTimeAsync(0);
    expect(fetchCode).toHaveBeenCalledTimes(2);
    expect(states.at(-1)).toEqual({ status: "hidden", reason: "failed", message: "网络中断" });
    // 失败后不自己显示旧码，计时也停了。
    await vi.advanceTimersByTimeAsync(10_000);
    expect(states.at(-1)).toEqual({ status: "hidden", reason: "failed", message: "网络中断" });
    controller.stop();
  });

  it("pauseHidesImmediatelyAndIgnoresLateResponse：切后台立即隐藏，迟到的码也不显示；回前台先取新码", async () => {
    let release: (value: Code) => void = () => undefined;
    const fetchCode = vi.fn<() => Promise<Code>>()
      .mockImplementationOnce(() => new Promise<Code>((resolve) => { release = resolve; }))
      .mockResolvedValueOnce(code(2, 30));
    const states: Array<DynamicCodeState<Code>> = [];
    const controller = createDynamicCode(fetchCode, (s) => states.push(s));

    controller.start();
    controller.pause();
    release(code(1, 30));
    await vi.advanceTimersByTimeAsync(0);
    expect(states.at(-1)).toEqual({ status: "hidden", reason: "paused" });

    controller.start();
    expect(states.at(-1)).toEqual({ status: "loading" });
    await vi.advanceTimersByTimeAsync(0);
    expect(states.at(-1)).toMatchObject({ status: "shown", code: { value: "code-2" }, secondsLeft: 30 });
    controller.stop();
  });
});
