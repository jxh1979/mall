// 文件版本：V0.02
// 客人端请求协议（三端共用，纯 TS）：
// - 不碰 DOM/window/fetch：各端注入 transport（App 用 fetch，小程序用 wx.request 包一层），只收发字符串；
// - 每个请求带 X-Terminal-Type（app／miniapp）与 X-Client-Version（本端版本）；有会话才带 X-Customer-Token；
//   有当前综合体、当前页面所属模块时带 X-Project-Id、X-Module-Code——后端终端开关按这几层逐层判关
//   （PGM「可以按全局、综合体、商户、业务系统、模块、版本/设备、灰度开关」「关闭后菜单不可见或不可点，接口也要拒绝业务动作」）；
// - 请求体 camelCase -> snake_case；响应 snake_case -> camelCase；统一信封 { code, message, data } 解包，data.items 列表拆箱；
// - 写操作自动补幂等键（Idempotency-Key 头 + body.idempotency_key，二者同值；同一次操作结果未明时再提交沿用原键）；
// - 带文件的提交（投诉附件、学生证件照片，OpenAPI 为 multipart/form-data）经 postForm：字段蛇形、文件原样交给本端组表单，幂等规则同上。

import { apiErrorFromResponse } from "./apiError";
import { announceReplayed, beginWriteOperation } from "./operationKey";

type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

type JsonBody = Record<string, unknown>;

export type CustomerTerminalType = "app" | "miniapp";

/** 表单里的一个文件：file 为本端的文件句柄（App 为浏览器 File/Blob），协议层只转交、不读取。 */
export type CustomerFormFile = { field: string; file: unknown; fileName: string };
export type CustomerTransportRequest = {
  method: string;
  url: string;
  headers: Record<string, string>;
  body?: string;
  /** multipart 提交：本端按字段与文件组表单（不手写 content-type，由运行环境带上分隔符）。 */
  form?: { fields: Record<string, string>; files: CustomerFormFile[] };
};
export type CustomerTransportResponse = { status: number; text: string };
/** 各端的请求适配：网络中断、超时须抛错（结果未明，请求编号留给再次提交）；收到响应一律按状态码与原文返回。 */
export type CustomerTransport = (request: CustomerTransportRequest) => Promise<CustomerTransportResponse>;

export type CustomerRequestContext = { projectId?: string | null; moduleCode?: string | null };

export type CustomerApiOptions = {
  /** 接口基址（不带末尾斜杠）；空串＝同源相对路径。 */
  baseUrl?: string;
  terminalType: CustomerTerminalType;
  clientVersion: string;
  transport: CustomerTransport;
  /** 当前会话令牌；令牌只经请求头发出，不进 URL。 */
  getToken?: () => string | null | undefined;
  /** 当前选中的综合体、当前页面所属的客人模块。 */
  getContext?: () => CustomerRequestContext;
};

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

function mapKeys(value: unknown, convertKey: (key: string) => string, parentKey = ""): JsonValue {
  if (Array.isArray(value)) return value.map((item) => mapKeys(item, convertKey));
  if (isPlainObject(value)) {
    return Object.fromEntries(
      Object.entries(value)
        .filter(([, item]) => item !== undefined)
        // 支付拉起参数 launch.params 由渠道签发、原样交给支付 SDK，键名不改（ROOT-03-R18「params 原样透传给客户端 SDK」）。
        .map(([key, item]) => [convertKey(key), parentKey === "launch" && key === "params" ? (item as JsonValue) : mapKeys(item, convertKey, key)]),
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

export function nextIdempotencyKey(terminalType: CustomerTerminalType): string {
  return `customer-${terminalType}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

// 请求体里的请求编号一律取传入的这一个，与 Idempotency-Key 头同值（M725-F02）。
export function normalizeRequestBody(body: JsonBody, idempotencyKey: string): JsonValue {
  return mapKeys({ ...body, idempotencyKey }, toSnakeCaseKey);
}

// 会话过期全局回登录：客人端外壳注册回调（清会话+回登录页），任何页面任何请求 401 都走这里。
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
  constructor(private readonly options: CustomerApiOptions) {}

  get<T>(path: string): Promise<T> {
    return this.request<T>(path);
  }

  post<T>(path: string, body: JsonBody = {}): Promise<T> {
    return this.request<T>(path, "POST", body);
  }

  /** multipart 提交：fields 为驼峰字段（发出时蛇形），同一次操作按字段与文件名认。 */
  postForm<T>(path: string, fields: Record<string, string>, files: CustomerFormFile[]): Promise<T> {
    return this.request<T>(path, "POST", { ...fields, files: files.map((item) => `${item.field}:${item.fileName}`) }, { fields, files });
  }

  private async request<T>(path: string, method = "GET", body?: JsonBody,
    form?: { fields: Record<string, string>; files: CustomerFormFile[] }): Promise<T> {
    const { baseUrl = "", terminalType, clientVersion, transport, getToken, getContext } = this.options;
    const headers: Record<string, string> = { "x-terminal-type": terminalType, "x-client-version": clientVersion };
    const token = getToken?.();
    if (token) headers["x-customer-token"] = token;
    const context = getContext?.() ?? {};
    if (context.projectId) headers["x-project-id"] = context.projectId;
    if (context.moduleCode) headers["x-module-code"] = context.moduleCode;
    // M725-F02：请求编号跟着「一次操作」走（见 operationKey）——上一次没拿到明确结果时，
    // 再提交同一内容沿用原编号，服务端据此返回第一次的结果，不会再扣一次、再发一次。
    const operation = body ? beginWriteOperation(method, path, body, () => nextIdempotencyKey(terminalType)) : null;
    let requestBody: string | undefined;
    let requestForm: CustomerTransportRequest["form"];
    if (body && operation) {
      const idempotencyKey = operation.key;
      headers["idempotency-key"] = idempotencyKey;
      if (form) {
        requestForm = { fields: normalizeRequestBody(form.fields, idempotencyKey) as Record<string, string>, files: form.files };
      } else {
        headers["content-type"] = "application/json";
        requestBody = JSON.stringify(normalizeRequestBody(body, idempotencyKey));
      }
    }
    let response: CustomerTransportResponse;
    try {
      response = await transport({ method, url: `${baseUrl}${path}`, headers, body: requestBody, ...(requestForm ? { form: requestForm } : {}) });
    } catch (networkError) {
      // 网络中断、超时：这次提交是否已生效不知道，结果未明，请求编号留给再次提交。
      operation?.settle(null);
      throw networkError;
    }
    const { status, text } = response;
    // 会话失效（401）全局回登录页；真权限不足（403）不登出，仍走下方错误翻译提示。
    if (status === 401 && !isSessionProbeOrAuthPath(path)) notifySessionExpired();
    // 错误翻译层：客人只看中文业务话术（err.message），技术信息收进 err.detail 供「详情」折叠展示。
    if (status < 200 || status >= 300) {
      const error = apiErrorFromResponse(path, status, text, method);
      operation?.settle(status, error.detailCode);
      throw error;
    }
    operation?.settle(status);
    const parsed: unknown = text ? JSON.parse(text) : null;
    // 重复提交返回第一次的结果（replayed=true）：照常按成功返回，并提示已完成。
    if (operation && isPlainObject(parsed) && parsed.replayed === true) announceReplayed(true);
    return (text ? unwrap(parsed) : null) as T;
  }
}
