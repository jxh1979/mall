// One-off: add V104 超容处置策略写入侧 (V127) 两个端点到 P0 OpenAPI spec。
// 表已建、读路径已有，但写入侧全缺 → 软上限告警/warn_only「有读取代码但永不触发」。
// 本脚本补：PUT/GET /api/v1/amusement/projects/{projectId}/over-capacity-policy。
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
  description: "操作幂等键；重复提交同一策略配置时必须返回同一处理结果，避免重复写入。",
  maxLength: 128,
};
const operatorNote = {
  type: "string",
  description: "操作备注；用于解释超容处置策略变更原因，进入审计留痕。",
  maxLength: 1000,
};
const evidence = {
  type: "array",
  description: "证据附件 ID 列表；用于绑定审批、说明或现场凭证。",
  items: { type: "string", maxLength: 64 },
  maxItems: 50,
};

// ── parameters ──────────────────────────────────────────────────────────────
spec.components.parameters.ProjectId = spec.components.parameters.ProjectId || {
  name: "projectId",
  in: "path",
  required: true,
  schema: { type: "string", maxLength: 64 },
  description: "ProjectId 路径参数；必须引用当前数据范围内存在且归属本商户的游乐项目。",
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.ConfigureOverCapacityPolicyRequest = {
  type: "object",
  description:
    "请求载荷 `ConfigureOverCapacityPolicyRequest`（V104 amusement_over_capacity_policy 写入侧，按 project_id 幂等 upsert）；" +
    "operating_capacity 为运营容量（软上限，>=0），disposal_mode 决定超容处置（block_hard 硬阻断+软告警 / warn_only 仅告警留痕不阻断）。",
  additionalProperties: false,
  required: ["disposal_mode"],
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    operating_capacity: {
      type: "integer",
      description: "运营容量（软上限，>=0）；核销在场达到该值即软超容（告警放行）。省略默认 0（不启用软上限告警）。",
      minimum: 0,
      maximum: 999999,
    },
    disposal_mode: {
      type: "string",
      description: "处置模式：block_hard=硬阻断+软告警（默认）；warn_only=仅告警留痕不阻断。",
      enum: ["block_hard", "warn_only"],
    },
    enabled: { type: "boolean", description: "是否启用该策略（省略默认 true）；停用后退化为仅硬上限阻断、无软上限告警。" },
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
spec.components.requestBodies.ConfigureOverCapacityPolicyRequest = {
  required: true,
  content: { "application/json": { schema: { $ref: "#/components/schemas/ConfigureOverCapacityPolicyRequest" } } },
};

// ── operations ────────────────────────────────────────────────────────────────
const successResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  403: { $ref: "#/components/responses/Error" },
  404: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const readResponses = {
  200: { $ref: "#/components/responses/Success" },
  403: { $ref: "#/components/responses/Error" },
  404: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const projectIdParam = {
  $ref: "#/components/parameters/ProjectId",
  description: "ProjectId 参数；必须按 OpenAPI 类型、枚举和必填规则校验。",
};

spec.paths["/api/v1/amusement/projects/{projectId}/over-capacity-policy"] = {
  get: {
    tags: ["amusement"],
    operationId: "getAmusementOverCapacityPolicy",
    summary: "查询游乐项目超容处置策略",
    "x-permission": "amusement.over_capacity_policy.view",
    "x-data-scope": "project-amusement",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": false,
    "x-audit-event-success": "amusement.over_capacity_policy.viewed",
    "x-audit-event-failure": "amusement.over_capacity_policy.view_failed",
    "x-auth-context": "staff-token",
    parameters: [projectIdParam],
    responses: readResponses,
  },
  put: {
    tags: ["amusement"],
    operationId: "configureAmusementOverCapacityPolicy",
    summary: "配置游乐项目超容处置策略",
    "x-permission": "amusement.over_capacity_policy.configure",
    "x-data-scope": "project-amusement",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": true,
    "x-audit-event-success": "amusement.over_capacity_policy.configured",
    "x-audit-event-failure": "amusement.over_capacity_policy.configure_failed",
    "x-auth-context": "staff-token",
    parameters: [projectIdParam],
    requestBody: { $ref: "#/components/requestBodies/ConfigureOverCapacityPolicyRequest" },
    responses: successResponses,
  },
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("amusement over-capacity-policy OpenAPI operations added.");
