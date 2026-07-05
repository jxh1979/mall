import { apiErrorFromResponse } from "../../shared/apiError";

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

export interface PagedResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

/**
 * 服务端分页读取:保留信封的 {items,total,page,size}(既有 unwrap 把 data 拍平成裸数组会丢 total)。
 * 不改 get 的任何调用点——需要真分页的页面显式改用 getPaged。
 */
function unwrapPaged(value: unknown): PagedResult<unknown> {
  const normalized = mapKeys(value, toCamelCaseKey);
  const data = isPlainObject(normalized) && "data" in normalized ? normalized.data : normalized;
  if (isPlainObject(data) && Array.isArray(data.items)) {
    return {
      items: data.items as unknown[],
      total: typeof data.total === "number" ? data.total : (data.items as unknown[]).length,
      page: typeof data.page === "number" ? data.page : 1,
      size: typeof data.pageSize === "number" ? data.pageSize : (data.items as unknown[]).length,
    };
  }
  const items = Array.isArray(data) ? (data as unknown[]) : [];
  return { items, total: items.length, page: 1, size: items.length };
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

  /** 会话续期（用户裁决 localStorage+refresh_token）：401 时用它调 /auth/refresh，成功重放原请求一次。 */
  refreshToken: string | null = null;

  /** 刷新成功回调（AdminApp 持久化新 token 并更新状态）。 */
  onTokensRefreshed: ((tokens: { accessToken: string; refreshToken: string | null }) => void) | null = null;

  /** 刷新失败/无刷新凭证回调（AdminApp 清凭证回登录页，提示「登录已过期」）。 */
  onSessionExpired: (() => void) | null = null;

  private accessToken: string;

  /** 并发 401 只发一次刷新请求。 */
  private refreshing: Promise<boolean> | null = null;

  /** 并发多个 401 只广播一次会话失效（防跳转风暴）；重新登录会新建 AdminApi 实例，标记自然复位。 */
  private sessionExpiredNotified = false;

  constructor(accessToken: string) {
    this.accessToken = accessToken;
  }

  get<T>(path: string): Promise<T> {
    return this.request<T>(path);
  }

  /** 服务端分页读取:返回 {items,total,page,size}。分页参数由调用方拼进 path 的 query。 */
  async getPaged<T>(path: string): Promise<PagedResult<T>> {
    const raw = await this.requestRaw(path);
    return unwrapPaged(raw) as PagedResult<T>;
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

  private buildInit(method: string, body: JsonBody | undefined): RequestInit {
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
    return init;
  }

  /** 与 request 同管线(401 刷新重放),但返回 unwrap 前的原始 JSON,供 getPaged 保留分页字段。 */
  private async requestRaw(path: string): Promise<unknown> {
    const isAuthEndpoint = path.includes("/auth/");
    let response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit("GET", undefined));
    if (response.status === 401 && !isAuthEndpoint) {
      // 先静默续期重放一次；无 refresh_token / 刷新失败 / 重放仍 401 → 会话失效回登录页。
      if (this.refreshToken && (await this.tryRefresh())) {
        response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit("GET", undefined));
      }
      if (response.status === 401) this.notifySessionExpired();
    }
    const text = await response.text();
    if (!response.ok) throw apiErrorFromResponse(path, response.status, text);
    return text ? JSON.parse(text) : null;
  }

  private async request<T>(path: string, method = "GET", body?: JsonBody): Promise<T> {
    // 幂等键在首发时生成一次，401 刷新后的重放沿用同一键（服务端按幂等键去重）。
    const stableBody = body ? { ...body, idempotencyKey: body.idempotencyKey ?? nextIdempotencyKey() } : undefined;
    // 认证域自身（登录/刷新/登出）失败不再走刷新，避免自递归。
    const isAuthEndpoint = path.includes("/auth/");
    let response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit(method, stableBody));
    if (response.status === 401 && !isAuthEndpoint) {
      // 先静默续期重放一次；无 refresh_token / 刷新失败 / 重放仍 401 → 会话失效回登录页。
      if (this.refreshToken && (await this.tryRefresh())) {
        response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit(method, stableBody));
      }
      if (response.status === 401) this.notifySessionExpired();
    }
    const text = await response.text();
    if (!response.ok) throw apiErrorFromResponse(path, response.status, text);
    return (text ? unwrap(JSON.parse(text)) : null) as T;
  }

  private notifySessionExpired(): void {
    if (this.sessionExpiredNotified) return;
    this.sessionExpiredNotified = true;
    this.onSessionExpired?.();
  }

  private tryRefresh(): Promise<boolean> {
    this.refreshing ??= this.doRefresh().finally(() => {
      this.refreshing = null;
    });
    return this.refreshing;
  }

  private async doRefresh(): Promise<boolean> {
    try {
      const headers = new Headers({ "content-type": "application/json", "x-terminal-type": this.terminalType });
      if (this.accessToken) headers.set("authorization", `Bearer ${this.accessToken}`);
      const response = await fetch(`${this.apiBaseUrl}/api/v1/auth/refresh`, {
        method: "POST",
        headers,
        body: JSON.stringify({ refresh_token: this.refreshToken }),
      });
      if (!response.ok) return false;
      const text = await response.text();
      const data = (text ? unwrap(JSON.parse(text)) : null) as { accessToken?: string; refreshToken?: string } | null;
      if (!data?.accessToken) return false;
      this.accessToken = data.accessToken;
      if (data.refreshToken) this.refreshToken = data.refreshToken;
      this.onTokensRefreshed?.({ accessToken: this.accessToken, refreshToken: this.refreshToken });
      return true;
    } catch {
      return false;
    }
  }
}
