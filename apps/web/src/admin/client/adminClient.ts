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

function unwrap(value: unknown): JsonValue {
  const normalized = mapKeys(value, toCamelCaseKey);
  if (isPlainObject(normalized) && "code" in normalized && "data" in normalized) {
    const data = normalized.data;
    if (isPlainObject(data) && Array.isArray(data.items)) return data.items;
    return data;
  }
  return normalized;
}

function nextIdempotencyKey(): string {
  return `admin-web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function normalizeRequestBody(body: JsonBody): JsonValue {
  return mapKeys({ ...body, idempotencyKey: body.idempotencyKey ?? nextIdempotencyKey() }, toSnakeCaseKey);
}

export class AdminApi {
  private readonly apiBaseUrl = getApiBaseUrl();

  /** 3.19 二期：当前所在菜单模块（AdminApp 路由切换时更新），随请求带 X-Module-Code 供终端开关按模块闸。 */
  moduleCode: string | null = null;

  /** 商户专区（3.18 方案A）：merchant 归属账号按 merchant_portal 终端上报，受 V197 商户门户总开关约束；员工/平台账号仍为 web。 */
  terminalType: string = "web";

  constructor(private readonly accessToken: string) {}

  get<T>(path: string): Promise<T> {
    return this.request<T>(path);
  }

  post<T>(path: string, body: JsonBody = {}): Promise<T> {
    return this.request<T>(path, "POST", body);
  }

  put<T>(path: string, body: JsonBody = {}): Promise<T> {
    return this.request<T>(path, "PUT", body);
  }

  patch<T>(path: string, body: JsonBody = {}): Promise<T> {
    return this.request<T>(path, "PATCH", body);
  }

  delete<T>(path: string): Promise<T> {
    return this.request<T>(path, "DELETE");
  }

  private async request<T>(path: string, method = "GET", body?: JsonBody): Promise<T> {
    const headers = new Headers();
    if (this.accessToken) headers.set("authorization", `Bearer ${this.accessToken}`);
    // 3.19 二期：声明终端后后端按 terminal.enabled 逐请求强制（矩阵页关端/关模块=接口硬拒）。
    headers.set("x-terminal-type", this.terminalType);
    if (this.moduleCode) headers.set("x-module-code", this.moduleCode);
    const init: RequestInit = { method, headers };
    if (body) {
      headers.set("content-type", "application/json");
      init.body = JSON.stringify(normalizeRequestBody(body));
    }
    const response = await fetch(`${this.apiBaseUrl}${path}`, init);
    const text = await response.text();
    if (!response.ok) throw new Error(`${path} ${response.status}: ${text || response.statusText}`);
    return (text ? unwrap(JSON.parse(text)) : null) as T;
  }
}
