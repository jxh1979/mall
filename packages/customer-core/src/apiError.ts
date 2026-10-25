// 文件版本：V0.01
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

// 后端 PARAM_INVALID/VALIDATION_FAILED 报错里实际出现的字段名 → 中文业务名。
// 范围＝grep backend `X is required`/`X is invalid` 全量 + 少量高频机读键；只覆盖实际出现的键。
// 缺失的键在 translate 里降级显示原文（不再丢信息），补键即可。
const FIELD_LABELS: Record<string, string> = {
  // 组织 / 主体
  merchant_id: "商户",
  merchant_ids: "商户",
  merchant_name: "商户名称",
  project_id: "项目",
  space_ids: "空间",
  entitlement_id: "权益",
  candidate_ids: "招商对象",
  owner_department: "责任部门",
  owner_person: "责任人",
  // 认证
  username: "用户名",
  password: "密码",
  authentication: "登录凭证",
  // 通用标识 / 状态
  id: "标识",
  name: "名称",
  status: "状态",
  reason: "原因",
  enabled: "启用开关",
  accepted: "确认",
  action: "操作",
  event: "事件",
  events: "事件",
  line: "明细行",
  items: "明细项",
  tasks: "任务",
  steps: "步骤",
  now: "当前时间",
  timestamp: "时间戳",
  // 时间 / 有效期
  expires_at: "到期时间",
  valid_until: "有效期至",
  target_date: "目标日期",
  holiday: "节假日",
  // 巡检 / 设备 / 能耗
  checked_endpoints: "巡检点位",
  qr_content: "二维码内容",
  gps_lat: "纬度",
  gps_lng: "经度",
  gather_object: "采集对象",
  check_scope: "巡检范围",
  scope_type: "范围类型",
  blocking_scope: "阻断范围",
  equipment_type: "设备类型",
  meter_type: "表计类型",
  read_mode: "抄表方式",
  energy_fee_basis: "能耗计费口径",
  terminal_type: "终端类型",
  kiosk_mode: "自助机模式",
  fallback_mode: "降级模式",
  max_concurrent: "最大并发",
  // 财务 / 结算
  adjustment_amount_cent: "调整金额",
  adjustment_rate: "调整比例",
  commission_mode: "分成方式",
  fee_event: "费用事件",
  expiry_action: "到期处理方式",
  ticket_category: "票务类别",
  // 工单 / 风控 / 合规
  abnormal_note: "异常说明",
  disposition: "处置结论",
  request_type: "申请类型",
  risk_level: "风险等级",
  violation_type: "违规类型",
  policy_status: "策略状态",
  segment: "分段",
  channel: "渠道",
  acceptance_by: "验收人",
  capabilities: "能力项",
  attachment_ids: "附件",
  registration_photo_refs: "登记照片",
  source_payload: "来源报文",
  signature: "签名",
  request: "请求",
};

// 机读键：直接给整句用户话术（不套「XX 未填写」模板）。
const MACHINE_KEY_MESSAGES: Record<string, string> = {
  idempotency_key: "请勿重复提交，请刷新后再试",
};

// Spring bean 校验默认英文文案 → 中文原因（VALIDATION_FAILED 的 `field: msg` 段）。
const BEAN_REASONS: Array<[RegExp, string]> = [
  [/must not be (null|empty|blank)/i, "未填写"],
  [/must be greater than 0|must be positive/i, "必须大于 0"],
  [/size must be between/i, "长度不符合要求"],
  [/must not be/i, "格式不正确"],
];

function labelFor(key: string): string {
  return FIELD_LABELS[key] ?? key;
}

/** 翻译单个字段错误段；无法识别则原样返回（降级不丢信息）。 */
function translateSegment(segment: string): string {
  const trimmed = segment.trim();
  if (!trimmed) return trimmed;

  // 形态一：手工抛点 `field is required` / `field is invalid`
  const manual = /^([a-z][a-z0-9_]*) is (required|invalid)$/i.exec(trimmed);
  if (manual) {
    const key = manual[1] ?? "";
    if (MACHINE_KEY_MESSAGES[key]) return MACHINE_KEY_MESSAGES[key];
    return `${labelFor(key)}${manual[2]?.toLowerCase() === "required" ? "未填写" : "格式不正确"}`;
  }

  // 形态二：Spring bean 校验 `field: must not be null`
  const bean = /^([a-z][a-z0-9_]*):\s*(.+)$/i.exec(trimmed);
  if (bean) {
    const key = bean[1] ?? "";
    const reason = bean[2] ?? "";
    if (MACHINE_KEY_MESSAGES[key]) return MACHINE_KEY_MESSAGES[key];
    const zhReason = BEAN_REASONS.find(([re]) => re.test(reason))?.[1] ?? reason;
    return `${labelFor(key)}${zhReason}`;
  }

  // 形态三：缺失必填 query 参数 `required parameter 'x' is missing`
  const missingParam = /^required parameter '([a-z0-9_]+)' is missing$/i.exec(trimmed);
  if (missingParam) return `${labelFor(missingParam[1] ?? "")}未填写`;

  return trimmed; // 已是中文或未知形态 → 原样透传，不丢信息。
}

/** PARAM_INVALID / VALIDATION_FAILED 的字段级翻译：多字段按 `; ` 拆分逐段翻译。 */
function translateFieldErrors(backendMessage: string | null): string {
  if (!backendMessage) return "提交内容有误，请检查后重新提交";
  const parts = backendMessage.split(";").map(translateSegment).filter(Boolean);
  return parts.length ? parts.join("；") : "提交内容有误，请检查后重新提交";
}

function translate(
  status: number,
  code: string | null,
  detailCode: string | null,
  backendMessage: string | null,
  method: string,
): string {
  if (detailCode === "INVALID_CREDENTIALS") return "账号或密码不正确";

  // 参数/校验错误：始终展示字段级中文，绝不套笼统兜底吞掉字段信息。
  if (code === "PARAM_INVALID" || detailCode === "VALIDATION_FAILED") {
    return translateFieldErrors(backendMessage);
  }

  if (status === 401 || code === "UNAUTHORIZED") return "登录已过期，请重新登录";
  if (status === 403 || code === "FORBIDDEN") return "当前账号无权进行此操作";
  if (status === 404 || code === "NOT_FOUND") return "数据不存在或已被删除";

  // 409 冲突：区别于「读取失败」，明确是并发/状态/重复提交冲突。
  if (status === 409 || code === "STATE_CONFLICT" || code === "IDEMPOTENCY_CONFLICT" || detailCode === "IDEMPOTENCY_CONFLICT") {
    if (code === "IDEMPOTENCY_CONFLICT" || detailCode === "IDEMPOTENCY_CONFLICT") {
      return "请勿重复提交：该操作已受理，请刷新后确认结果";
    }
    return hasChinese(backendMessage) ? `操作冲突：${backendMessage}` : "操作冲突：数据已变化，请刷新后重试";
  }

  if (status >= 500 || code === null) return "服务暂时不可用，请稍后重试";
  if (hasChinese(backendMessage)) return backendMessage as string;
  // 提交（写）与读取语义区分兜底。
  return method === "GET" ? "读取失败，请稍后重试" : "操作未能完成，请稍后重试";
}

/** 由非 2xx 响应构造业务化错误：解析 {code, detail_code, message, request_id}，无法解析按服务不可用。 */
export function apiErrorFromResponse(path: string, status: number, rawText: string, method = "POST"): ApiError {
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
  return new ApiError(translate(status, code, detailCode, backendMessage, method), {
    detail: { path, status, requestId, raw: rawText },
    code,
    detailCode,
  });
}
