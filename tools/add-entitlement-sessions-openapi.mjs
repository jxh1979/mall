// One-off: add V129 / REQ-53 权益会话（entitlement_sessions）4 个 operation 到 P0 OpenAPI spec。
// 背景：R4-G1 DRIFT —— 矩阵声称已补会话表 + entitlement_session_id + 会话时效 + 防错领，迁移从未建
// entitlement_sessions，零落地。V129 建会话主数据表并补齐「工作手机扫附赠权益码发起 → 一体机展示 → 领取核销
// （防错领 + 会话时效）」端点。本脚本补齐 OpenAPI 契约（amusement 域，与 offline-entitlements 离线快照互补）。
// Source of truth = JSON; YAML regenerated from JSON. Idempotent: re-running yields the same spec.
import fs from "node:fs";
import path from "node:path";
import YAML from "yaml";

const root = process.cwd();
const jsonPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const yamlPath = path.join(root, "docs", "openapi", "openapi-p0.yaml");
const spec = JSON.parse(fs.readFileSync(jsonPath, "utf8"));

const idempotencyKey = {
  type: "string",
  description: "操作幂等键；重复提交必须返回同一处理结果，避免重复发起 / 重复领取核销。",
  maxLength: 128,
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.InitiateEntitlementSessionRequest = {
  type: "object",
  description:
    "请求载荷 `InitiateEntitlementSessionRequest`（V129/REQ-53 工作手机扫附赠权益码发起会话）；status=initiated，merchant_id 强制取自认证上下文不由请求体传入。entitlement_ref 为被扫权益引用（发放 id 或权益码，跨域通用字符串，不强加 FK）。",
  additionalProperties: false,
  required: ["entitlement_ref", "ttl_seconds"],
  properties: {
    idempotency_key: idempotencyKey,
    entitlement_ref: { type: "string", description: "被扫的权益引用（发放 id 或权益码，必填，非空）。", maxLength: 128 },
    project_id: { type: "string", description: "可选游乐项目 id（可空）。", maxLength: 64 },
    ttl_seconds: { type: "integer", description: "会话有效期秒数（必填且 >0，决定 expires_at=now+ttl_seconds）。", minimum: 1 },
    initiated_by: { type: "string", description: "工作手机操作员（可空，缺省取系统占位）。", maxLength: 64 },
  },
};
spec.components.schemas.ClaimEntitlementSessionRequest = {
  type: "object",
  description:
    "请求载荷 `ClaimEntitlementSessionRequest`（V129/REQ-53 一体机领取核销）；防错领由服务端条件 UPDATE 保证（仅 initiated/displayed 且未过期可领，→claimed），过期 / 已领 / 重复领 / 已取消均拒。",
  additionalProperties: false,
  properties: {
    idempotency_key: idempotencyKey,
    claimed_by: { type: "string", description: "领取核销操作员 / 一体机标识（可空，缺省取系统占位）。", maxLength: 64 },
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
const reqBody = (name) => ({
  required: true,
  content: { "application/json": { schema: { $ref: `#/components/schemas/${name}` } } },
});
spec.components.requestBodies.InitiateEntitlementSessionRequest = reqBody("InitiateEntitlementSessionRequest");
spec.components.requestBodies.ClaimEntitlementSessionRequest = reqBody("ClaimEntitlementSessionRequest");

// ── operations ────────────────────────────────────────────────────────────────
const successResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  403: { $ref: "#/components/responses/Error" },
  404: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const detailResponses = {
  200: { $ref: "#/components/responses/Success" },
  403: { $ref: "#/components/responses/Error" },
  404: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const pageResponses = {
  200: { $ref: "#/components/responses/Page" },
  403: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const idParam = {
  $ref: "#/components/parameters/Id",
  description: "路径参数（会话 id 或 session_code）；必须按 OpenAPI 类型、枚举和必填规则校验。",
};

spec.paths["/api/v1/amusement/entitlement-sessions"] = {
  get: {
    tags: ["amusement"],
    operationId: "listAmusementEntitlementSessions",
    summary: "查询权益会话列表（merchant scope）",
    "x-permission": "amusement.entitlement_session.view",
    "x-data-scope": "merchant",
    "x-terminal-allowed": ["web", "kiosk"],
    "x-idempotency-required": false,
    "x-audit-event-success": "amusement.entitlement_session.listed",
    "x-audit-event-failure": "amusement.entitlement_session.list_failed",
    "x-auth-context": "staff-token",
    parameters: [],
    responses: pageResponses,
  },
  post: {
    tags: ["amusement"],
    operationId: "initiateAmusementEntitlementSession",
    summary: "发起权益会话（工作手机扫附赠权益码）",
    "x-permission": "amusement.entitlement_session.initiate",
    "x-data-scope": "merchant",
    "x-terminal-allowed": ["staff_work_phone", "web"],
    "x-idempotency-required": true,
    "x-audit-event-success": "amusement.entitlement_session.initiated",
    "x-audit-event-failure": "amusement.entitlement_session.initiate_failed",
    "x-auth-context": "staff-token",
    parameters: [],
    requestBody: { $ref: "#/components/requestBodies/InitiateEntitlementSessionRequest" },
    responses: successResponses,
  },
};
spec.paths["/api/v1/amusement/entitlement-sessions/{id}"] = {
  get: {
    tags: ["amusement"],
    operationId: "getAmusementEntitlementSession",
    summary: "一体机查询展示权益会话（按 id 或 session_code）",
    "x-permission": "amusement.entitlement_session.view",
    "x-data-scope": "merchant",
    "x-terminal-allowed": ["web", "kiosk"],
    "x-idempotency-required": false,
    "x-audit-event-success": "amusement.entitlement_session.viewed",
    "x-audit-event-failure": "amusement.entitlement_session.view_failed",
    "x-auth-context": "staff-token",
    parameters: [idParam],
    responses: detailResponses,
  },
};
spec.paths["/api/v1/amusement/entitlement-sessions/{id}/claim"] = {
  post: {
    tags: ["amusement"],
    operationId: "claimAmusementEntitlementSession",
    summary: "一体机领取核销权益会话（防错领 + 会话时效）",
    "x-permission": "amusement.entitlement_session.claim",
    "x-data-scope": "merchant",
    "x-terminal-allowed": ["web", "kiosk"],
    "x-idempotency-required": true,
    "x-audit-event-success": "amusement.entitlement_session.claimed",
    "x-audit-event-failure": "amusement.entitlement_session.claim_failed",
    "x-auth-context": "staff-token",
    parameters: [idParam],
    requestBody: { $ref: "#/components/requestBodies/ClaimEntitlementSessionRequest" },
    responses: successResponses,
  },
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("amusement entitlement-sessions OpenAPI operations added.");
