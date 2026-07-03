// 两端（后台 adminClient / 客人端 customerClient）共用的接口错误翻译层。
// 目标：err.message 一律是中文业务话术（62 页现有 err.message 渲染点自动受益），
// 技术信息（path/status/request_id/原始报文）收进 err.detail，由 UI 折叠展示供报障。

export type ApiErrorDetail = {
  path: string;
  status: number;
  requestId: string | null;
  raw: string;
};

export class ApiError extends Error {
  readonly detail: ApiErrorDetail;
  /** 后端顶层错误码（10 枚举，如 UNAUTHORIZED/FORBIDDEN/PARAM_INVALID）。 */
  readonly code: string | null;
  /** 后端细粒度业务码（detail_code，如 INVALID_CREDENTIALS/PARTNER_ORG_NOT_BOUND）。 */
  readonly detailCode: string | null;

  constructor(message: string, options: { detail: ApiErrorDetail; code?: string | null; detailCode?: string | null }) {
    super(message);
    this.name = "ApiError";
    this.detail = options.detail;
    this.code = options.code ?? null;
    this.detailCode = options.detailCode ?? null;
  }
}

/** 供 UI「技术详情」折叠块使用的报障文本；非 ApiError 返回 null。 */
export function formatApiErrorDetail(error: unknown): string | null {
  if (!(error instanceof ApiError)) return null;
  const { path, status, requestId, raw } = error.detail;
  return [`${path} ${status}`, requestId ? `request_id=${requestId}` : null, raw || null]
    .filter(Boolean)
    .join("\n");
}

function hasChinese(text: string | null | undefined): boolean {
  return Boolean(text && /[一-鿿]/.test(text));
}

function translate(status: number, code: string | null, detailCode: string | null, backendMessage: string | null): string {
  if (detailCode === "INVALID_CREDENTIALS") return "账号或密码不正确";
  if (code === "PARAM_INVALID" || detailCode === "VALIDATION_FAILED") {
    return hasChinese(backendMessage) ? `提交内容有误：${backendMessage}` : "提交内容有误，请检查后重新提交";
  }
  if (status === 401 || code === "UNAUTHORIZED") return "登录已过期，请重新登录";
  if (status === 403 || code === "FORBIDDEN") return "当前账号无权进行此操作";
  if (status === 404 || code === "NOT_FOUND") return "数据不存在或已被删除";
  if (status >= 500 || code === null) return "服务暂时不可用，请稍后重试";
  if (hasChinese(backendMessage)) return backendMessage as string;
  return "操作未能完成，请稍后重试";
}

/** 由非 2xx 响应构造业务化错误：解析 {code, detail_code, message, request_id}，无法解析按服务不可用。 */
export function apiErrorFromResponse(path: string, status: number, rawText: string): ApiError {
  let code: string | null = null;
  let detailCode: string | null = null;
  let backendMessage: string | null = null;
  let requestId: string | null = null;
  try {
    const parsed = JSON.parse(rawText) as Record<string, unknown>;
    if (parsed && typeof parsed === "object") {
      code = typeof parsed.code === "string" ? parsed.code : null;
      detailCode = typeof parsed.detail_code === "string" ? parsed.detail_code : null;
      backendMessage = typeof parsed.message === "string" ? parsed.message : null;
      requestId = typeof parsed.request_id === "string" ? parsed.request_id : null;
    }
  } catch {
    // 非 JSON（网关 HTML/空响应）→ code 保持 null，走"服务暂时不可用"。
  }
  return new ApiError(translate(status, code, detailCode, backendMessage), {
    detail: { path, status, requestId, raw: rawText },
    code,
    detailCode,
  });
}
