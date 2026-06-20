// One-off: add V128 / REQ-39 溯源码主数据（inventory_trace_codes）4 个 operation 到 P0 OpenAPI spec。
// 背景：R4-G1 DRIFT —— 矩阵声称 inventory_trace_codes 表，迁移从未建，trace_code 仅作共用列。V128 建码主数据表
// 并补齐“贴码登记 → 出库打印 → 责任链查询/列表”CRUD 端点。本脚本补齐 OpenAPI 契约（与 scan-handover 互补）。
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
  description: "操作幂等键；重复提交必须返回同一处理结果，避免重复贴码登记 / 重复打印。",
  maxLength: 128,
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.RegisterTraceCodeRequest = {
  type: "object",
  description:
    "请求载荷 `RegisterTraceCodeRequest`（V128/REQ-39 溯源码贴码登记，入库）；status=registered，merchant_id 强制取自认证上下文不由请求体传入。trace_code 可空（缺省服务端生成），重复 trace_code 唯一冲突拒绝。",
  additionalProperties: false,
  required: ["item_id"],
  properties: {
    idempotency_key: idempotencyKey,
    item_id: { type: "string", description: "物料/商品 id（供应链域只读引用，必填）。", maxLength: 64 },
    batch_id: { type: "string", description: "批次 id（可空）。", maxLength: 64 },
    trace_code: { type: "string", description: "码值（可空，缺省服务端按幂等键稳定生成；唯一）。", maxLength: 128 },
  },
};
spec.components.schemas.PrintTraceCodeRequest = {
  type: "object",
  description:
    "请求载荷 `PrintTraceCodeRequest`（V128/REQ-39 溯源码出库打印）；status registered→printed，记 printed_at，条件 UPDATE 防重复打印（非 registered 拒绝）。",
  additionalProperties: false,
  properties: {
    idempotency_key: idempotencyKey,
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
const reqBody = (name) => ({
  required: true,
  content: { "application/json": { schema: { $ref: `#/components/schemas/${name}` } } },
});
spec.components.requestBodies.RegisterTraceCodeRequest = reqBody("RegisterTraceCodeRequest");
spec.components.requestBodies.PrintTraceCodeRequest = reqBody("PrintTraceCodeRequest");

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
const param = (ref) => ({
  $ref: `#/components/parameters/${ref}`,
  description: "路径参数；必须按 OpenAPI 类型、枚举和必填规则校验。",
});

function getOp({ operationId, summary, permission, success, failure, params, responses }) {
  return {
    tags: ["inventory"],
    operationId,
    summary,
    "x-permission": permission,
    "x-data-scope": "project",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": false,
    "x-audit-event-success": success,
    "x-audit-event-failure": failure,
    "x-auth-context": "staff-token",
    parameters: params.map(param),
    responses,
  };
}
function postOp({ operationId, summary, permission, success, failure, params, body }) {
  return {
    tags: ["inventory"],
    operationId,
    summary,
    "x-permission": permission,
    "x-data-scope": "project",
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

spec.paths["/api/v1/inventory/trace-codes"] = {
  get: getOp({
    operationId: "listInventoryTraceCodes",
    summary: "查询溯源码主数据列表",
    permission: "inventory.trace_code.view",
    success: "inventory.trace_code.listed",
    failure: "inventory.trace_code.list_failed",
    params: [],
    responses: pageResponses,
  }),
  post: postOp({
    operationId: "registerInventoryTraceCode",
    summary: "贴码登记溯源码（入库）",
    permission: "inventory.trace_code.register",
    success: "inventory.trace_code.registered",
    failure: "inventory.trace_code.register_failed",
    params: [],
    body: "RegisterTraceCodeRequest",
  }),
};
spec.paths["/api/v1/inventory/trace-codes/{id}"] = {
  get: getOp({
    operationId: "getInventoryTraceCode",
    summary: "查询溯源码责任链（按 id 或 trace_code）",
    permission: "inventory.trace_code.view",
    success: "inventory.trace_code.viewed",
    failure: "inventory.trace_code.view_failed",
    params: ["Id"],
    responses: detailResponses,
  }),
};
spec.paths["/api/v1/inventory/trace-codes/{id}/print"] = {
  post: postOp({
    operationId: "printInventoryTraceCode",
    summary: "出库打印溯源码（registered→printed）",
    permission: "inventory.trace_code.print",
    success: "inventory.trace_code.printed",
    failure: "inventory.trace_code.print_failed",
    params: ["Id"],
    body: "PrintTraceCodeRequest",
  }),
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("inventory trace-codes OpenAPI operations added.");
