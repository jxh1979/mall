// 文件版本：V0.01
import { unwrap } from "../customer/client/customerClient";

function getApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL as string | undefined;
  if (configured) return configured.replace(/\/$/, "");
  if (window.location.port === "3000") return `${window.location.protocol}//${window.location.hostname}:8080`;
  return "";
}

/**
 * 独立经销商 Web 门户客户端（CG-11d，受控第十一端 dealer_portal）。
 * 与员工后台、客人端客户端隔离：终端自报 dealer_portal（服务端按 V454 总开关逐请求强制），凭证走 HttpOnly Cookie 会话，
 * 写请求带 CSRF 头与请求编号；401 回登录；403（含 TERMINAL_DISABLED / DEALER_DEACTIVATED）原地中文提示。
 */
export const DEALER_PORTAL_TERMINAL = "dealer_portal";

type JsonBody = Record<string, unknown>;

export class DealerPortalApiError extends Error {
  constructor(message: string, readonly status: number, readonly code: string | null) {
    super(message);
  }
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const hit = document.cookie.split("; ").find((part) => part.startsWith(`${name}=`));
  return hit ? decodeURIComponent(hit.slice(name.length + 1)) : null;
}

function toSnake(key: string): string {
  return key.replace(/[A-Z]/g, (c) => `_${c.toLowerCase()}`);
}
function snakeKeys(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(snakeKeys);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([k, v]) => [toSnake(k), snakeKeys(v)]));
  }
  return value;
}

const MESSAGES: Record<string, string> = {
  TERMINAL_DISABLED: "经销商门户已被平台暂停，请稍后再试",
  TERMINAL_NOT_ALLOWED: "当前终端不允许访问经销商门户",
  DEALER_DEACTIVATED: "经销商已停用，门户权限已撤销",
  DEALER_PORTAL_NOT_ENABLED: "企业资料、合同、开票和银行资料审核通过后才开放此功能",
  DEALER_PORTAL_ACCOUNT_REQUIRED: "请使用经销商门户账号登录",
  INVALID_CREDENTIALS: "账号或密码不正确",
  USER_NOT_ACTIVE: "该账号已停用",
  PAYMENT_METHOD_FORBIDDEN: "只支持对公转账和平台线上支付",
};

export class DealerPortalApi {
  private readonly apiBaseUrl = getApiBaseUrl();
  onSessionExpired: (() => void) | null = null;

  get<T>(path: string): Promise<T> {
    return this.request<T>(path, "GET");
  }

  post<T>(path: string, body: JsonBody = {}): Promise<T> {
    return this.request<T>(path, "POST", body);
  }

  async login(loginName: string, password: string): Promise<{ id: string; username: string; accountType: string }> {
    const result = await this.request<{ user: { id: string; username: string; accountType: string } }>("/api/v1/auth/login", "POST", {
      loginName, password, terminalType: DEALER_PORTAL_TERMINAL,
    });
    return result.user;
  }

  async logout(): Promise<void> {
    await this.request("/api/v1/auth/logout", "POST", { idempotencyKey: `dp-logout-${Date.now()}` }).catch(() => undefined);
  }

  private async request<T>(path: string, method: string, body?: JsonBody): Promise<T> {
    const headers = new Headers({ "x-terminal-type": DEALER_PORTAL_TERMINAL, "x-requested-with": "XMLHttpRequest" });
    const init: RequestInit = { method, headers, credentials: "include" };
    if (body) {
      const idempotencyKey = typeof body.idempotencyKey === "string" ? body.idempotencyKey : `dp-${Date.now()}-${Math.random().toString(16).slice(2)}`;
      headers.set("content-type", "application/json");
      headers.set("idempotency-key", idempotencyKey);
      const csrf = readCookie("XSRF-TOKEN");
      if (csrf) headers.set("x-xsrf-token", csrf);
      init.body = JSON.stringify(snakeKeys({ ...body, idempotencyKey }));
    }
    const response = await fetch(`${this.apiBaseUrl}${path}`, init);
    const text = await response.text();
    if (response.status === 401 && !path.includes("/auth/")) this.onSessionExpired?.();
    if (!response.ok) {
      let code: string | null = null;
      let message = "操作失败，请稍后再试";
      try {
        const parsed = JSON.parse(text) as { code?: string; detail_code?: string; message?: string; error?: { code?: string; message?: string } };
        code = parsed.detail_code ?? parsed.code ?? parsed.error?.code ?? null;
        message = (code && MESSAGES[code]) || parsed.message || parsed.error?.message || message;
      } catch {
        // 非 JSON 错误体：保留通用中文提示
      }
      throw new DealerPortalApiError(message, response.status, code);
    }
    return (text ? unwrap(JSON.parse(text)) : null) as T;
  }
}
