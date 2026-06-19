// One-off: add 清单#6 amusement operational config CRUD operations to the P0 OpenAPI spec.
// 表已建无端点：V082 amusement_project_entitlements（项目↔权益绑定）、V083 amusement_capacity_config（点位/设备并发容量）。
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
  description: "操作幂等键；重复提交时必须返回同一处理结果，避免重复绑定或重复录入容量配置。",
  maxLength: 128,
};
const operatorNote = {
  type: "string",
  description: "操作备注；用于解释运营配置变更原因，进入审计留痕。",
  maxLength: 1000,
};
const evidence = {
  type: "array",
  description: "证据附件 ID 列表；用于绑定审批、说明或现场凭证。",
  items: { type: "string", maxLength: 64 },
  maxItems: 50,
};

// ── parameters ──────────────────────────────────────────────────────────────
spec.components.parameters.EntitlementType = {
  name: "entitlementType",
  in: "path",
  required: true,
  schema: { type: "string", maxLength: 64 },
  description: "EntitlementType 路径参数；项目已绑定的权益类型（须以 amusement 开头的游乐类）。",
};
spec.components.parameters.ConfigId = {
  name: "configId",
  in: "path",
  required: true,
  schema: { type: "string", maxLength: 64 },
  description: "ConfigId 路径参数；必须引用当前数据范围内存在的容量配置记录。",
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.AmusementProjectEntitlementRequest = {
  type: "object",
  description: "请求载荷 `AmusementProjectEntitlementRequest`（V082 项目↔权益绑定）；配置后该项目核销收紧为仅接受已绑定权益类型，不再回退按游乐类放行。",
  additionalProperties: false,
  required: ["entitlement_type"],
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    entitlement_type: {
      type: "string",
      description: "可用于本项目的票务权益类型（须以 amusement 开头的游乐类，如 amusement_single / amusement_pass）。",
      maxLength: 64,
    },
  },
};
spec.components.schemas.AmusementCapacityConfigRequest = {
  type: "object",
  description: "请求载荷 `AmusementCapacityConfigRequest`（V083 点位/设备并发容量，三维容量第三维）；location_point_id 与 device_id 至少其一；max_concurrent 为该点位/设备并发在场上限。",
  additionalProperties: false,
  required: ["max_concurrent"],
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    location_point_id: { type: "string", description: "点位 ID；与 device_id 至少配置其一。", maxLength: 128 },
    device_id: { type: "string", description: "设备 ID；与 location_point_id 至少配置其一。", maxLength: 128 },
    max_concurrent: { type: "integer", description: "该点位/设备并发在场上限（>=0）；核销三维度取最小可放行余量。", minimum: 0, maximum: 999999 },
    effective_session_pattern: { type: "string", description: "适用场次模式（空=适用全部场次）。", maxLength: 64 },
    enabled: { type: "boolean", description: "是否启用（省略默认 true）；停用后该配置不再参与三维容量校验。" },
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
const reqBody = (name, required = true) => ({
  required,
  content: { "application/json": { schema: { $ref: `#/components/schemas/${name}` } } },
});
spec.components.requestBodies.AmusementProjectEntitlementRequest = reqBody("AmusementProjectEntitlementRequest");
spec.components.requestBodies.AmusementCapacityConfigRequest = reqBody("AmusementCapacityConfigRequest");
spec.components.requestBodies.StatusChangeRequest =
  spec.components.requestBodies.StatusChangeRequest || reqBody("StatusChangeRequest", false);

// ── operations ────────────────────────────────────────────────────────────────
const successResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  403: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const pageResponses = {
  200: { $ref: "#/components/responses/Page" },
  403: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const param = (ref) => ({ $ref: `#/components/parameters/${ref}`, description: "undefined 参数；必须按 OpenAPI 类型、枚举和必填规则校验。" });

function getOp({ operationId, summary, permission, success, failure, params }) {
  return {
    tags: ["amusement"],
    operationId,
    summary,
    "x-permission": permission,
    "x-data-scope": "project-amusement",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": false,
    "x-audit-event-success": success,
    "x-audit-event-failure": failure,
    "x-auth-context": "staff-token",
    parameters: params.map(param),
    responses: pageResponses,
  };
}
function postOp({ operationId, summary, permission, success, failure, params, body }) {
  return {
    tags: ["amusement"],
    operationId,
    summary,
    "x-permission": permission,
    "x-data-scope": "project-amusement",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": true,
    "x-audit-event-success": success,
    "x-audit-event-failure": failure,
    "x-auth-context": "staff-token",
    parameters: params.map(param),
    requestBody: { $ref: `#/components/requestBodies/${body}` },
    responses: successResponses,
  };
}

const entSuccess = "amusement.project_entitlement.changed";
const entFailure = "amusement.project_entitlement.change_failed";
const entViewed = "amusement.project_entitlement.viewed";
const entViewFailed = "amusement.project_entitlement.view_failed";
const capSuccess = "amusement.capacity_config.changed";
const capFailure = "amusement.capacity_config.change_failed";
const capViewed = "amusement.capacity_config.viewed";
const capViewFailed = "amusement.capacity_config.view_failed";

spec.paths["/api/v1/amusement/projects/{id}/entitlements"] = {
  get: getOp({
    operationId: "listAmusementProjectEntitlements",
    summary: "查询游乐项目可用权益绑定",
    permission: "amusement.project_entitlement.manage",
    success: entViewed,
    failure: entViewFailed,
    params: ["Id"],
  }),
  post: postOp({
    operationId: "createAmusementProjectEntitlement",
    summary: "维护游乐项目可用权益绑定",
    permission: "amusement.project_entitlement.manage",
    success: entSuccess,
    failure: entFailure,
    params: ["Id"],
    body: "AmusementProjectEntitlementRequest",
  }),
};
spec.paths["/api/v1/amusement/projects/{id}/entitlements/{entitlementType}/remove"] = {
  post: postOp({
    operationId: "removeAmusementProjectEntitlement",
    summary: "解除游乐项目权益绑定",
    permission: "amusement.project_entitlement.manage",
    success: entSuccess,
    failure: entFailure,
    params: ["Id", "EntitlementType"],
    body: "StatusChangeRequest",
  }),
};
spec.paths["/api/v1/amusement/projects/{id}/capacity-config"] = {
  get: getOp({
    operationId: "listAmusementCapacityConfigs",
    summary: "查询游乐点位设备并发容量配置",
    permission: "amusement.capacity_config.manage",
    success: capViewed,
    failure: capViewFailed,
    params: ["Id"],
  }),
  post: postOp({
    operationId: "createAmusementCapacityConfig",
    summary: "维护游乐点位设备并发容量配置",
    permission: "amusement.capacity_config.manage",
    success: capSuccess,
    failure: capFailure,
    params: ["Id"],
    body: "AmusementCapacityConfigRequest",
  }),
};
spec.paths["/api/v1/amusement/capacity-config/{configId}/disable"] = {
  post: postOp({
    operationId: "disableAmusementCapacityConfig",
    summary: "停用游乐容量配置",
    permission: "amusement.capacity_config.manage",
    success: capSuccess,
    failure: capFailure,
    params: ["ConfigId"],
    body: "StatusChangeRequest",
  }),
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("amusement operational config OpenAPI operations added.");
