// One-off: add V108/V126 库存盘点录入闭环 5 个 operation 到 P0 OpenAPI spec。
// 表已建写入侧全缺：V108 stocktake（盘点单）、inventory_variance（库存差异明细）；finance 入账依赖
// review_status='approved'，此前无端点可建单/录差异/复核 → 链路断裂。本脚本补齐 OpenAPI 契约。
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
  description: "操作幂等键；重复提交必须返回同一处理结果，避免重复建盘点单 / 重复录差异 / 重复复核。",
  maxLength: 128,
};

// ── parameters ──────────────────────────────────────────────────────────────
spec.components.parameters.VarianceId = {
  name: "vid",
  in: "path",
  required: true,
  schema: { type: "string", maxLength: 64 },
  description: "VarianceId 路径参数；必须引用当前数据范围内、隶属该盘点单的库存差异明细行 id。",
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.CreateStocktakeRequest = {
  type: "object",
  description:
    "请求载荷 `CreateStocktakeRequest`（V108 盘点单建单）；建盘点单 status=counting，merchant_id 强制取自认证上下文不由请求体传入。",
  additionalProperties: false,
  required: ["warehouse_id"],
  properties: {
    idempotency_key: idempotencyKey,
    warehouse_id: { type: "string", description: "盘点库房 id（必填）。", maxLength: 64 },
    business_date: { type: "string", format: "date", description: "盘点经营日（缺省取当日）。" },
    remark: { type: "string", description: "盘点单备注（可空）。", maxLength: 255 },
  },
};
spec.components.schemas.RecordVarianceRequest = {
  type: "object",
  description:
    "请求载荷 `RecordVarianceRequest`（V108 库存差异明细）；variance_qty=counted_qty−book_qty（带符号），review_status=pending。variance_amount_cent 显式传入优先，否则按 variance_qty×unit_cost_cents 成本估算。",
  additionalProperties: false,
  required: ["item_id", "reason"],
  properties: {
    idempotency_key: idempotencyKey,
    item_id: { type: "string", description: "物料/商品 id（供应链域只读引用，必填）。", maxLength: 64 },
    batch_no: { type: "string", description: "批次号（可空）。", maxLength: 64 },
    book_qty: { type: "number", description: "账面数量（缺省 0）。" },
    counted_qty: { type: "number", description: "实盘数量（缺省 0）。" },
    reason: {
      type: "string",
      description: "差异原因：报损 damage / 盘亏 shortage / 盘盈 overage / 供应商责任 supplier_fault。",
      enum: ["damage", "shortage", "overage", "supplier_fault"],
    },
    supplier_id: { type: "string", description: "供应商责任（supplier_fault）时的责任供应商 id（→ AP 对手方）。", maxLength: 64 },
    variance_amount_cent: {
      type: "integer",
      format: "int64",
      description: "金额差（带符号，分）；显式传入优先于成本估算。",
    },
    unit_cost_cents: {
      type: "integer",
      format: "int64",
      description: "成本单价（分，非负）；未显式传 variance_amount_cent 时按 variance_qty×unit_cost_cents 估算。",
      minimum: 0,
    },
  },
};
spec.components.schemas.ReviewVarianceRequest = {
  type: "object",
  description:
    "请求载荷 `ReviewVarianceRequest`（V108 库存差异复核）；decision=approved/rejected，pending→终态，幂等，重复/非 pending 拒绝。approved 是解锁 finance postInventoryVariance 入账的关键。",
  additionalProperties: false,
  required: ["decision"],
  properties: {
    idempotency_key: idempotencyKey,
    decision: {
      type: "string",
      description: "复核裁决：approved（复核通过，可入账）/ rejected（驳回，不入账）。",
      enum: ["approved", "rejected"],
    },
    reason: { type: "string", description: "复核意见（可空）。", maxLength: 1000 },
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
const reqBody = (name) => ({
  required: true,
  content: { "application/json": { schema: { $ref: `#/components/schemas/${name}` } } },
});
spec.components.requestBodies.CreateStocktakeRequest = reqBody("CreateStocktakeRequest");
spec.components.requestBodies.RecordVarianceRequest = reqBody("RecordVarianceRequest");
spec.components.requestBodies.ReviewVarianceRequest = reqBody("ReviewVarianceRequest");

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

spec.paths["/api/v1/inventory/stocktakes"] = {
  get: getOp({
    operationId: "listInventoryStocktakes",
    summary: "查询库存盘点单列表",
    permission: "inventory.stocktake.view",
    success: "inventory.stocktake.viewed",
    failure: "inventory.stocktake.view_failed",
    params: [],
    responses: pageResponses,
  }),
  post: postOp({
    operationId: "createInventoryStocktake",
    summary: "建库存盘点单",
    permission: "inventory.stocktake.create",
    success: "inventory.stocktake.created",
    failure: "inventory.stocktake.create_failed",
    params: [],
    body: "CreateStocktakeRequest",
  }),
};
spec.paths["/api/v1/inventory/stocktakes/{id}"] = {
  get: getOp({
    operationId: "getInventoryStocktake",
    summary: "查询库存盘点单详情（含差异明细）",
    permission: "inventory.stocktake.view",
    success: "inventory.stocktake.detail_viewed",
    failure: "inventory.stocktake.detail_view_failed",
    params: ["Id"],
    responses: detailResponses,
  }),
};
spec.paths["/api/v1/inventory/stocktakes/{id}/variances"] = {
  post: postOp({
    operationId: "recordInventoryStocktakeVariance",
    summary: "录入库存盘点差异行",
    permission: "inventory.stocktake.variance_record",
    success: "inventory.stocktake.variance_recorded",
    failure: "inventory.stocktake.variance_record_failed",
    params: ["Id"],
    body: "RecordVarianceRequest",
  }),
};
spec.paths["/api/v1/inventory/stocktakes/{id}/variances/{vid}/review"] = {
  post: postOp({
    operationId: "reviewInventoryStocktakeVariance",
    summary: "复核库存盘点差异（approved/rejected）",
    permission: "inventory.stocktake.variance_review",
    success: "inventory.stocktake.variance_reviewed",
    failure: "inventory.stocktake.variance_review_failed",
    params: ["Id", "VarianceId"],
    body: "ReviewVarianceRequest",
  }),
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("inventory stocktake OpenAPI operations added.");
