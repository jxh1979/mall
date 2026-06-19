// 客人端 API client：复用 admin/legacy-console 的请求封装口径
// - Bearer 客人 Token（同时写 X-Customer-Token，对齐后端双读口径）
// - 请求体 camelCase -> snake_case；响应 snake_case -> camelCase
// - 统一信封 { code, message, data } 解包，data.items 列表拆箱
// - 写操作自动补幂等键（Idempotency-Key 头 + body.idempotency_key）

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

export function normalizeRequestBody(body: JsonBody, idempotencyKey: string): JsonValue {
  return mapKeys({ ...body, idempotencyKey: body.idempotencyKey ?? idempotencyKey }, toSnakeCaseKey);
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
    const init: RequestInit = { method, headers };
    if (body) {
      const idempotencyKey = nextIdempotencyKey();
      headers.set("content-type", "application/json");
      headers.set("idempotency-key", idempotencyKey);
      init.body = JSON.stringify(normalizeRequestBody(body, idempotencyKey));
    }
    const response = await fetch(`${this.apiBaseUrl}${path}`, init);
    const text = await response.text();
    if (!response.ok) throw new Error(`${path} ${response.status}: ${text || response.statusText}`);
    return (text ? unwrap(JSON.parse(text)) : null) as T;
  }
}

// 登录不需要既有 Token：单独的无鉴权调用，便于换取客人 Token
export async function customerPhoneLogin(payload: {
  phone: string;
  smsCode: string;
  scene?: string;
}): Promise<CustomerLoginResponse> {
  const api = new CustomerApi("");
  return api.post<CustomerLoginResponse>("/api/v1/customer/auth/phone-login", payload);
}

export type CustomerLoginResponse = {
  customerId: string;
  maskedPhone: string;
  customerToken: string;
  expiresAt: string;
};

// 二维码刷新窗口：后端 TicketCredentialResponse 仅返回 issuedAt/expiresAt（不含 refresh_interval_seconds），
// 客人端按 (expiresAt - issuedAt) 推导刷新间隔，落在票种级 {30,60} 秒口径，缺省 60 秒。
export function deriveRefreshIntervalSeconds(issuedAt?: string | null, expiresAt?: string | null): number {
  const fallback = 60;
  if (!issuedAt || !expiresAt) return fallback;
  const issued = Date.parse(issuedAt);
  const expires = Date.parse(expiresAt);
  if (Number.isNaN(issued) || Number.isNaN(expires)) return fallback;
  const seconds = Math.round((expires - issued) / 1000);
  if (seconds <= 0) return fallback;
  // 防伪口径：30 或 60 秒在线刷新；其它取最接近的合法窗口。
  return seconds <= 45 ? 30 : 60;
}
