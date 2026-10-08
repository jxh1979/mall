// 客人端 API client：复用 admin 的请求封装口径
// - Bearer 客人 Token（同时写 X-Customer-Token，对齐后端双读口径）
// - 请求体 camelCase -> snake_case；响应 snake_case -> camelCase
// - 统一信封 { code, message, data } 解包，data.items 列表拆箱
// - 写操作自动补幂等键（Idempotency-Key 头 + body.idempotency_key，二者同值；同一次操作结果未明时再提交沿用原键）

import { apiErrorFromResponse } from "../../shared/apiError";
import { announceReplayed, beginWriteOperation, forgetWriteOperations } from "../../shared/operationKey";

type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

type JsonBody = Record<string, unknown>;

function getApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL as string | undefined;
  if (configured) return configured.replace(/\/$/, "");
  if (window.location.port === "3000") return `${window.location.protocol}//${window.location.hostname}:8080`;
  return "";
}

function isPlainObject(value: unknown): value is Record<string, JsonValue> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function toSnakeCaseKey(key: string): string {
  return key.replace(/([a-z0-9])([A-Z])/g, "$1_$2").toLowerCase();
}

function toCamelCaseKey(key: string): string {
  // 含 "." 的是字典 key(如 metrics 的 "ar.aging_31_60"),不是字段名,原样保留。
  if (key.includes(".")) return key;
  return key.replace(/_([a-z0-9])/g, (_, letter: string) => letter.toUpperCase());
}

function mapKeys(value: unknown, convertKey: (key: string) => string): JsonValue {
  if (Array.isArray(value)) return value.map((item) => mapKeys(item, convertKey));
  if (isPlainObject(value)) {
    return Object.fromEntries(
      Object.entries(value)
        .filter(([, item]) => item !== undefined)
        .map(([key, item]) => [convertKey(key), mapKeys(item, convertKey)]),
    );
  }
  if (value === undefined) return null;
  return value as JsonValue;
}

export function unwrap(value: unknown): JsonValue {
  const normalized = mapKeys(value, toCamelCaseKey);
  if (isPlainObject(normalized) && "code" in normalized && "data" in normalized) {
    const data = normalized.data;
    if (isPlainObject(data) && Array.isArray(data.items)) return data.items;
    return data;
  }
  return normalized;
}

export function nextIdempotencyKey(): string {
  return `customer-web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

// 请求体里的请求编号一律取传入的这一个，与 Idempotency-Key 头同值（M725-F02）。
export function normalizeRequestBody(body: JsonBody, idempotencyKey: string): JsonValue {
  return mapKeys({ ...body, idempotencyKey }, toSnakeCaseKey);
}

// 会话过期全局回登录：CustomerApp 注册回调（清会话+回登录页），任何页面任何请求 401 都走这里。
// 客人端凭证是 httpOnly cookie（无 refresh 机制），401 即会话失效，直接回登录。
let sessionExpiredHandler: (() => void) | null = null;
let sessionExpiredNotified = false;

export function setCustomerSessionExpiredHandler(handler: (() => void) | null): void {
  sessionExpiredHandler = handler;
  sessionExpiredNotified = false;
}

/** 重新登录成功后复位防抖标记，下次会话再过期仍能触发回登录。 */
export function resetCustomerSessionExpiredNotice(): void {
  sessionExpiredNotified = false;
}

function notifySessionExpired(): void {
  // 并发多个 401 只触发一次（防跳转风暴）。
  if (sessionExpiredNotified) return;
  sessionExpiredNotified = true;
  sessionExpiredHandler?.();
}

/** 登录/登出/会话探测自身的 401 不算"会话过期"（登录失败要在登录页原地报错，探测本就允许未登录）。 */
function isSessionProbeOrAuthPath(path: string): boolean {
  return path.includes("/auth/") || path.endsWith("/customer/session");
}

export class CustomerApi {
  private readonly apiBaseUrl = getApiBaseUrl();

  constructor(private readonly customerToken: string) {}

  get<T>(path: string): Promise<T> {
    return this.request<T>(path);
  }

  post<T>(path: string, body: JsonBody = {}): Promise<T> {
    return this.request<T>(path, "POST", body);
  }

  private async request<T>(path: string, method = "GET", body?: JsonBody): Promise<T> {
    const headers = new Headers();
    if (this.customerToken) {
      headers.set("authorization", `Bearer ${this.customerToken}`);
      headers.set("x-customer-token", this.customerToken);
    }
    // 3.3 免登录：token 为空时走 httpOnly cookie 鉴权；自定义头是后端 cookie 写操作的 CSRF 闸。
    headers.set("x-requested-with", "XMLHttpRequest");
    // 3.19 二期：客人 web 端声明终端类型，接受 terminal.enabled 总开关逐请求强制。
    headers.set("x-terminal-type", "web");
    const init: RequestInit = { method, headers, credentials: "include" };
    // M725-F02：请求编号跟着「一次操作」走（见 shared/operationKey）——上一次没拿到明确结果时，
    // 再提交同一内容沿用原编号，服务端据此返回第一次的结果，不会再扣一次、再发一次。
    const operation = body ? beginWriteOperation(method, path, body, nextIdempotencyKey) : null;
    if (body && operation) {
      const idempotencyKey = operation.key;
      headers.set("content-type", "application/json");
      headers.set("idempotency-key", idempotencyKey);
      init.body = JSON.stringify(normalizeRequestBody(body, idempotencyKey));
    }
    let response: Response;
    let text: string;
    try {
      response = await fetch(`${this.apiBaseUrl}${path}`, init);
      text = await response.text();
    } catch (networkError) {
      // 网络中断、超时：这次提交是否已生效不知道，结果未明，请求编号留给再次提交。
      operation?.settle(null);
      throw networkError;
    }
    // 会话失效（401）全局回登录页；真权限不足（403）不登出，仍走下方错误翻译提示。
    if (response.status === 401 && !isSessionProbeOrAuthPath(path)) notifySessionExpired();
    // 错误翻译层：客人只看中文业务话术（err.message），技术信息收进 err.detail 供「详情」折叠展示。
    if (!response.ok) {
      const error = apiErrorFromResponse(path, response.status, text, method);
      operation?.settle(response.status, error.detailCode);
      throw error;
    }
    operation?.settle(response.status);
    const parsed: unknown = text ? JSON.parse(text) : null;
    // 重复提交返回第一次的结果（replayed=true）：照常按成功返回，并提示已完成。
    if (operation && isPlainObject(parsed) && parsed.replayed === true) announceReplayed(true);
    return (text ? unwrap(parsed) : null) as T;
  }
}

// 登录不需要既有 Token：单独的无鉴权调用，便于换取客人 Token
export async function customerPhoneLogin(payload: {
  phone: string;
  smsCode: string;
  scene?: string;
}): Promise<CustomerLoginResponse> {
  const api = new CustomerApi("");
  const result = await api.post<CustomerLoginResponse>("/api/v1/customer/auth/phone-login", payload);
  // M725-F02：上一位客人没拿到结果的请求编号不沿用到这位客人身上（共用设备登出不刷新页面）；
  // 同一位客人重新登录照常沿用。
  forgetWriteOperations(result.customerId);
  return result;
}

export type CustomerLoginResponse = {
  customerId: string;
  maskedPhone: string;
  customerToken: string;
  expiresAt: string;
};

export type CustomerSessionInfo = {
  customerId: string;
  maskedPhone: string;
  expiresAt: string | null;
};

// 3.3 免登录：刷新后凭 httpOnly cookie 恢复会话；无有效 cookie 返回 null（回登录页）。
export async function customerRestoreSession(): Promise<CustomerSessionInfo | null> {
  try {
    const info = await new CustomerApi("").get<CustomerSessionInfo>("/api/v1/customer/session");
    // M725-F02：恢复会话也记下是谁，同一位客人会话失效后重新登录照常沿用没拿到结果的请求编号。
    if (info) forgetWriteOperations(info.customerId);
    return info;
  } catch {
    return null;
  }
}

// 3.3 免登录：登出——后端清 httpOnly cookie（幂等，失败也不阻断本地清态）。
export async function customerLogout(): Promise<void> {
  try {
    await new CustomerApi("").post<null>("/api/v1/customer/auth/logout");
  } catch {
    // cookie 清除失败不阻断登出（本地态已清，cookie 到期自灭）。
  }
}

// 二维码刷新窗口：后端 TicketCredentialResponse 仅返回 issuedAt/expiresAt（不含 refresh_interval_seconds），
// 客人端按 (expiresAt - issuedAt) 推导刷新间隔，落在票种级 {10,15,30,60} 秒口径，缺省 15 秒（2026-07-18 拍板默认）。
export function deriveRefreshIntervalSeconds(issuedAt?: string | null, expiresAt?: string | null): number {
  const fallback = 15;
  if (!issuedAt || !expiresAt) return fallback;
  const issued = Date.parse(issuedAt);
  const expires = Date.parse(expiresAt);
  if (Number.isNaN(issued) || Number.isNaN(expires)) return fallback;
  const seconds = Math.round((expires - issued) / 1000);
  if (seconds <= 0) return fallback;
  // 防伪口径：10/15/30/60 秒在线刷新；其它取最接近的合法窗口。
  const allowed = [10, 15, 30, 60];
  return allowed.reduce((best, option) => (Math.abs(option - seconds) < Math.abs(best - seconds) ? option : best));
}
