// 文件版本：V0.01
// 第十八批 N 段第 123 条 123-R3（BSM:1876「使用 Web 端必须打开地理识别，没有打开地理识别的无法正常使用。Web 端同样适用：
// 核销、收款、退款、放行等敏感操作在授权位置范围之外一律拒绝并留痕。」）：网页端地理识别准入，现场动作取当下位置。
// 只在动作那一刻取一次位置，不轮询、不做轨迹（平台治理 PRD §15.3）。

export type GeoPosition = { latitude: number; longitude: number };

// 与后端定位闸（SensitiveLocationGuard）同一份现场动作闭集 12 条（第十八批裁决 31：财务中心后台办理不查位置），只认 POST。
const SENSITIVE_OPERATION_PATHS = [
  /^\/api\/v1\/ticketing\/verifications$/,
  /^\/api\/v1\/vouchers\/redemptions$/,
  /^\/api\/v1\/amusement\/verifications$/,
  /^\/api\/v1\/amusement\/entitlement-sessions\/[^/]+\/claim$/,
  /^\/api\/v1\/bar\/reservations\/[^/]+\/check-in-guest$/,
  /^\/api\/v1\/bar\/reservations\/[^/]+\/invitation\/entry-verify$/,
  /^\/api\/v1\/parking\/manual-releases$/,
  /^\/api\/v1\/services\/orders\/[^/]+\/mark-paid$/,
  /^\/api\/v1\/bar\/bills\/[^/]+\/settle$/,
  /^\/api\/v1\/ticketing\/orders\/[^/]+\/confirm-payment$/,
  /^\/api\/v1\/bar\/reservations\/[^/]+\/pay-deposit$/,
  /^\/api\/v1\/loyalty\/stored-value\/[^/]+\/counter-refund$/,
];

export function isSensitiveOperation(method: string, path: string): boolean {
  const bare = path.split("?")[0]!;
  return method === "POST" && SENSITIVE_OPERATION_PATHS.some((pattern) => pattern.test(bare));
}

/** 地理识别是否已打开：拒绝授权为 false，已授权为 true；还没问过就问一次，拿到位置、超时或暂时取不到都算已打开（HTML 只要求打开）。 */
export async function geolocationEnabled(): Promise<boolean> {
  if (typeof navigator === "undefined" || !navigator.geolocation) return false;
  try {
    const status = await navigator.permissions?.query({ name: "geolocation" });
    if (status?.state === "granted") return true;
    if (status?.state === "denied") return false;
  } catch {
    // 不支持按名查询授权的浏览器：直接问一次位置。
  }
  return new Promise((resolve) => {
    // code 1 = PERMISSION_DENIED
    navigator.geolocation.getCurrentPosition(() => resolve(true), (error) => resolve(error.code !== 1));
  });
}

/** 当下位置（WGS-84 十进制度）；取不到返回 null，请求照常发出、由后端拒绝并给中文提示。 */
export function currentPosition(): Promise<GeoPosition | null> {
  if (typeof navigator === "undefined" || !navigator.geolocation) return Promise.resolve(null);
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => resolve({ latitude: coords.latitude, longitude: coords.longitude }),
      () => resolve(null),
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 0 },
    );
  });
}
