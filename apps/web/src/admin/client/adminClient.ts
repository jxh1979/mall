// 文件版本：V0.05
import { apiErrorFromResponse } from "../../shared/apiError";
import { announceReplayed, beginWriteOperation } from "../../shared/operationKey";
import type { DownloadedFile } from "../types";

type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

type JsonBody = Record<string, unknown>;

function getApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL as string | undefined;
  if (configured) return configured.replace(/\/$/, "");
  // 令牌迁 HttpOnly Cookie 后必须同源（否则跨源 XHR 不带 Cookie）：dev（vite 3000）走同源，
  // 由 vite server.proxy 的 /api → 后端转发；prod 由 nginx 同源转发 /api。VITE_API_BASE_URL 显式跨源覆盖需自配 CORS。
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
        .map(([key, item]) => [convertKey(key),
          // 商品、绩效等级及扣分规则的字典键是业务标识，不能按接口字段名转换。
          ["unitPrices", "unit_prices", "gradeCoefficients", "grade_coefficients", "deductionRules", "deduction_rules", "deductions"].includes(key) && isPlainObject(item)
            ? Object.fromEntries(Object.entries(item).map(([id, amount]) => [id, mapKeys(amount, convertKey)]))
            : mapKeys(item, convertKey)]),
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

/** 读取非 HttpOnly 的 XSRF-TOKEN Cookie，回填 X-XSRF-TOKEN 头（CSRF 双提交）。HttpOnly 的令牌 Cookie 由浏览器自动携带、JS 读不到。 */
function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const escaped = name.replace(/([.*+?^${}()|[\]\\])/g, "\\$1");
  const match = document.cookie.match(new RegExp("(?:^|; )" + escaped + "=([^;]*)"));
  return match && match[1] !== undefined ? decodeURIComponent(match[1]) : null;
}

/** 第 23 条 23-R3：把下载到的导出文件交给浏览器保存。 */
export function saveDownloadedFile(file: DownloadedFile): void {
  const url = URL.createObjectURL(file.blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = file.fileName;
  link.click();
  URL.revokeObjectURL(url);
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

/**
 * 第 25 条 25-R3：商户手机应用（apps/merchant-app）的令牌与设备编号存放处（App 的系统安全存储，第十八批裁决 19）。
 * tokens()/deviceId() 同步读内存；clearTokens 只清令牌、设备编号保留（裁决 19：卸载重装或清除应用数据才算新设备）。
 */
export type MerchantAppTokens = { accessToken: string; refreshToken: string };
export type MerchantAppStore = {
  tokens(): MerchantAppTokens | null;
  deviceId(): string | null;
  save(tokens: MerchantAppTokens, deviceId: string | null): Promise<void>;
  clearTokens(): Promise<void>;
};

let merchantApp: { store: MerchantAppStore; clientVersion: string } | null = null;

/** 只由 apps/merchant-app 入口调用（网页入口 main.tsx 不调用，网页版行为不变）：此后终端报 merchant_app，请求改带访问令牌与 App 版本号、不走 Cookie。 */
export function configureMerchantAppMode(store: MerchantAppStore, clientVersion: string): void {
  merchantApp = { store, clientVersion };
}

export function isMerchantAppMode(): boolean {
  return merchantApp !== null;
}

export function merchantAppStore(): MerchantAppStore | null {
  return merchantApp?.store ?? null;
}

function credentialsMode(): RequestCredentials {
  return merchantApp ? "omit" : "include";
}

// App 内不做文件导出（BSM:1886 商户 App 未列导出；WebView 里存不下文件）：这几类下载在 App 里不发请求，免得生成导出、扣掉单次授权却拿不到文件。
const MERCHANT_APP_EXPORT_ENDPOINTS = new Set(["export", "monthly-export", "editable-copy"]);

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

// 第 23 条 23-R2、23-R5：外壳自发的页面打开留痕。不是用户的一次操作，不进操作编号登记、不撤下「已完成」提示（M725-F03），每次带新编号。
const PAGE_VIEWS_PATH = "/api/v1/security/page-views";

export class AdminApi {
  private readonly apiBaseUrl = getApiBaseUrl();

  /** 商户专区（3.18 方案A）：merchant 归属账号按 merchant_portal 终端上报，受 V197 商户门户总开关约束；员工/平台账号仍为 web。商户手机应用里一律 merchant_app（第 25 条 25-R3）。 */
  terminalType: string = merchantApp ? "merchant_app" : "web";

  /** 会话失效回调（AdminApp 清会话回登录页，提示「登录已过期」）。 */
  onSessionExpired: (() => void) | null = null;

  /** 并发 401 只发一次刷新请求。 */
  private refreshing: Promise<boolean> | null = null;

  /** 并发多个 401 只广播一次会话失效（防跳转风暴）；重新登录会新建 AdminApi 实例，标记自然复位。 */
  private sessionExpiredNotified = false;

  // 令牌迁 HttpOnly Cookie 后 AdminApi 不再需要令牌。保留可选形参仅为兼容既有大量调用点
  // （new AdminApi("token")/new AdminApi("")），传入值一律忽略——鉴权全靠浏览器自动携带的 Cookie。
  // ponytail: 兼容垫片，待所有调用点改为无参后可删。
  constructor(_legacyAccessTokenIgnored?: string) {
    void _legacyAccessTokenIgnored;
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

  /** multipart 上传：复用终端/CSRF 头，但不设 content-type（浏览器带 boundary），不转键。走 401 刷新重放一次。 */
  async postForm<T>(path: string, form: FormData): Promise<T> {
    const buildInit = (): RequestInit => {
      const headers = new Headers();
      headers.set("x-terminal-type", this.terminalType);
      this.applyAuth(headers, "POST");
      return { method: "POST", headers, body: form, credentials: credentialsMode() };
    };
    let response = await fetch(`${this.apiBaseUrl}${path}`, buildInit());
    if (response.status === 401 && !path.includes("/auth/")) {
      if (await this.tryRefresh()) {
        response = await fetch(`${this.apiBaseUrl}${path}`, buildInit());
      }
      if (response.status === 401) this.notifySessionExpired();
    }
    const text = await response.text();
    if (!response.ok) throw apiErrorFromResponse(path, response.status, text, "POST");
    return (text ? unwrap(JSON.parse(text)) : null) as T;
  }

  /**
   * 第 23 条 23-R3：下载导出文件（服务端生成的带密码 PDF，浏览器拿不到明文数据）。同 401 刷新重放一次；
   * 下载文件名取 Content-Disposition 的 UTF-8 文件名。
   */
  async getFile(path: string): Promise<DownloadedFile> {
    if (merchantApp && MERCHANT_APP_EXPORT_ENDPOINTS.has(path.split("?")[0]!.split("/").pop() ?? "")) {
      throw new Error("导出文件请在商户网页门户办理");
    }
    let response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit("GET", undefined));
    if (response.status === 401) {
      if (await this.tryRefresh()) {
        response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit("GET", undefined));
      }
      if (response.status === 401) this.notifySessionExpired();
    }
    if (!response.ok) throw apiErrorFromResponse(path, response.status, await response.text(), "GET");
    const name = /filename\*=UTF-8''([^;]+)/i.exec(response.headers.get("content-disposition") ?? "")?.[1];
    return { blob: await response.blob(), fileName: name ? decodeURIComponent(name) : "导出文件.pdf" };
  }

  /** 写请求带 CSRF 双提交头（X-XSRF-TOKEN = XSRF-TOKEN Cookie）；读请求不需要。商户手机应用不用 Cookie 与 CSRF，改带访问令牌与 App 版本号（PGM:736 版本档）。 */
  private applyAuth(headers: Headers, method: string): void {
    if (merchantApp) {
      const tokens = merchantApp.store.tokens();
      if (tokens) headers.set("authorization", `Bearer ${tokens.accessToken}`);
      headers.set("x-client-version", merchantApp.clientVersion);
      return;
    }
    if (SAFE_METHODS.has(method)) return;
    const csrf = readCookie("XSRF-TOKEN");
    if (csrf) headers.set("x-xsrf-token", csrf);
  }

  private buildInit(method: string, body: JsonBody | undefined): RequestInit {
    const headers = new Headers();
    // 3.19 二期：声明终端后后端按 terminal.enabled 逐请求强制（矩阵页关端/关模块=接口硬拒）。
    headers.set("x-terminal-type", this.terminalType);
    this.applyAuth(headers, method);
    // 令牌在 HttpOnly Cookie 里，浏览器自动携带（credentials:include 覆盖同源/跨源），JS 不再手动加 Authorization 头；商户手机应用除外（见 applyAuth）。
    const init: RequestInit = { method, headers, credentials: credentialsMode() };
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
      if (await this.tryRefresh()) {
        response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit("GET", undefined));
      }
      if (response.status === 401) this.notifySessionExpired();
    }
    const text = await response.text();
    if (!response.ok) throw apiErrorFromResponse(path, response.status, text, "GET");
    return text ? JSON.parse(text) : null;
  }

  private async request<T>(path: string, method = "GET", body?: JsonBody): Promise<T> {
    // M725-F02：请求编号跟着「一次操作」走（见 shared/operationKey）——上一次没拿到明确结果时，
    // 再提交同一内容沿用原编号；401 刷新后的重放也沿用同一编号（服务端按请求编号去重）。
    const operation = body && path !== PAGE_VIEWS_PATH ? beginWriteOperation(method, path, body, nextIdempotencyKey) : null;
    const stableBody = body && operation ? { ...body, idempotencyKey: operation.key } : body;
    // 认证域自身（登录/刷新/登出）失败不再走刷新，避免自递归。
    const isAuthEndpoint = path.includes("/auth/");
    let response: Response;
    let text: string;
    try {
      response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit(method, stableBody));
      if (response.status === 401 && !isAuthEndpoint) {
        // 先静默续期重放一次；刷新失败 / 重放仍 401 → 会话失效回登录页。
        if (await this.tryRefresh()) {
          response = await fetch(`${this.apiBaseUrl}${path}`, this.buildInit(method, stableBody));
        }
        if (response.status === 401) this.notifySessionExpired();
      }
      text = await response.text();
    } catch (networkError) {
      // 网络中断、超时：这次提交是否已生效不知道，结果未明，请求编号留给再次提交。
      operation?.settle(null);
      throw networkError;
    }
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
      if (merchantApp) return await this.refreshMerchantApp(merchantApp.store);
      // 刷新令牌在 HttpOnly Cookie 里（浏览器自动携带）；空体即可，后端从 Cookie 取。带 CSRF 头 + credentials。
      const headers = new Headers({ "content-type": "application/json", "x-terminal-type": this.terminalType });
      this.applyAuth(headers, "POST");
      const response = await fetch(`${this.apiBaseUrl}/api/v1/auth/refresh`, {
        method: "POST",
        headers,
        body: "{}",
        credentials: "include",
      });
      // 成功即续期：后端已用 Set-Cookie 轮转新令牌 Cookie，JS 无需读取令牌本身。
      return response.ok;
    } catch {
      return false;
    }
  }

  /** 商户手机应用：头带旧访问令牌，体带刷新令牌与设备编号（绑定设备编号的会话刷新须同号）；成功存新令牌，失败只清令牌、设备编号保留，随后回登录页。 */
  private async refreshMerchantApp(store: MerchantAppStore): Promise<boolean> {
    const tokens = store.tokens();
    if (tokens) {
      const headers = new Headers({ "content-type": "application/json", "x-terminal-type": this.terminalType });
      this.applyAuth(headers, "POST");
      const response = await fetch(`${this.apiBaseUrl}/api/v1/auth/refresh`, {
        method: "POST",
        headers,
        body: JSON.stringify(normalizeRequestBody({ refreshToken: tokens.refreshToken, deviceId: store.deviceId() ?? undefined })),
        credentials: "omit",
      });
      const renewed = response.ok ? (unwrap(await response.json()) as { accessToken?: string; refreshToken?: string; deviceId?: string } | null) : null;
      if (renewed?.accessToken) {
        await store.save({ accessToken: renewed.accessToken, refreshToken: renewed.refreshToken ?? tokens.refreshToken }, renewed.deviceId ?? store.deviceId());
        return true;
      }
    }
    await store.clearTokens();
    return false;
  }
}
