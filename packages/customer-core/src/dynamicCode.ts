// 文件版本：V0.01
// 三端统一的动态码行为（门票验票码、动态取酒码、被邀请人入场码、券核销码共用）。依据：
// BSM「凡是出示出来给人验证的二维码……都不是一张固定的图……有效期（刷新间隔）由后台统一设置、以 5 秒为单位可调、最长 60 秒；
// 截图、转发出去的旧码过了有效期就作废」；BSM「二维码原始值不存在手机本地，只在线核验」。
// - 显示时长只取服务端给的值：refresh_interval_seconds，缺则 expires_at − issued_at；不吸附档位、不设缺省；
// - 自收到响应起倒计时（不比对手机时钟与服务端时钟），到点立即隐藏旧码并重新取；
// - 取码失败、切到后台即隐藏；回前台先取新码再显示；
// - 码值只在内存里，本模块不写任何存储。

export type DynamicCodeWindow = {
  issuedAt?: string | null;
  expiresAt?: string | null;
  refreshIntervalSeconds?: number | null;
};

/** 这张码可显示的毫秒数（服务端值）；拿不到有效值返回 0（按取码失败处理，不显示）。 */
export function codeWindowMs(code: DynamicCodeWindow): number {
  if (typeof code.refreshIntervalSeconds === "number" && code.refreshIntervalSeconds > 0) {
    return code.refreshIntervalSeconds * 1000;
  }
  const issued = Date.parse(code.issuedAt ?? "");
  const expires = Date.parse(code.expiresAt ?? "");
  return Number.isFinite(issued) && Number.isFinite(expires) && expires > issued ? expires - issued : 0;
}

export type DynamicCodeState<T> =
  | { status: "loading" }
  | { status: "shown"; code: T; secondsLeft: number }
  | { status: "hidden"; reason: "failed" | "paused"; message?: string };

export type DynamicCodeTimers = {
  setTimeout(handler: () => void, ms: number): unknown;
  clearTimeout(handle: unknown): void;
  setInterval(handler: () => void, ms: number): unknown;
  clearInterval(handle: unknown): void;
};

export type DynamicCodeController = {
  /** 取新码并显示（首次进入、回前台、失败后重试）。取到之前不显示任何码。 */
  start(): void;
  /** 切到后台：立即隐藏、停计时，迟到的响应也不显示。 */
  pause(): void;
  /** 离开页面：停计时，之后不再回调。 */
  stop(): void;
};

export function createDynamicCode<T extends DynamicCodeWindow>(
  fetchCode: () => Promise<T>,
  onChange: (state: DynamicCodeState<T>) => void,
  timers: DynamicCodeTimers = globalThis as unknown as DynamicCodeTimers,
): DynamicCodeController {
  let generation = 0;
  let expiry: unknown;
  let ticker: unknown;
  const clear = () => {
    timers.clearTimeout(expiry);
    timers.clearInterval(ticker);
    expiry = undefined;
    ticker = undefined;
  };
  const start = () => {
    clear();
    const mine = ++generation;
    onChange({ status: "loading" });
    fetchCode().then(
      (code) => {
        if (mine !== generation) return;
        const windowMs = codeWindowMs(code);
        if (windowMs <= 0) {
          onChange({ status: "hidden", reason: "failed", message: "二维码有效期无效" });
          return;
        }
        const deadline = Date.now() + windowMs;
        const emit = () => onChange({ status: "shown", code, secondsLeft: Math.max(0, Math.ceil((deadline - Date.now()) / 1000)) });
        emit();
        ticker = timers.setInterval(emit, 1000);
        expiry = timers.setTimeout(start, windowMs);
      },
      (error: unknown) => {
        if (mine !== generation) return;
        onChange({ status: "hidden", reason: "failed", message: error instanceof Error ? error.message : undefined });
      },
    );
  };
  return {
    start,
    pause() {
      clear();
      generation += 1;
      onChange({ status: "hidden", reason: "paused" });
    },
    stop() {
      clear();
      generation += 1;
    },
  };
}
