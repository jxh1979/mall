// One-off: F-OPS-08 物业费固定计费替代 CAM——向 P0 OpenAPI 增「物业费费率维护 / 列出 / 按期计费」操作，
// 并把 CAM 权重分摊 + 年度 true-up 两端点标记 deprecated（保留仅返回 409 CAM_*_ABOLISHED）。
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
  description: "操作幂等键；重复提交时必须返回同一处理结果，避免重复计费/重复入账。",
  maxLength: 128,
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.PropertyFeeRateRequest = {
  type: "object",
  description:
    "请求载荷 `PropertyFeeRateRequest`（F-OPS-08 物业费费率维护，upsert）；effective_period 为空=设为该项目默认费率（精确期未命中时回退）。rate_per_square_meter_cent 为每平米单价（分/㎡/期，>=0）。替代已废止的 CAM 权重分摊。",
  additionalProperties: false,
  required: ["project_id", "rate_per_square_meter_cent"],
  properties: {
    idempotency_key: idempotencyKey,
    project_id: { type: "string", description: "项目 ID。", maxLength: 64 },
    effective_period: { type: "string", description: "计费期（如 2026-06）；为空=该项目默认费率。", maxLength: 32 },
    rate_per_square_meter_cent: {
      type: "integer",
      description: "每平米单价。（单位：分，禁止浮点）",
      minimum: 0,
      maximum: 999999999999,
    },
    status: { type: "string", description: "费率状态。", enum: ["active", "inactive"] },
  },
};
spec.components.schemas.PropertyFeeBillRequest = {
  type: "object",
  description:
    "请求载荷 `PropertyFeeBillRequest`（F-OPS-08 物业费按期计费）；对 project_id 下运营提供的 merchant_ids 按「每平米固定单价 × 商户面积」逐户生成应收，不退不补、按 (项目,期,商户) 幂等。merchants 无 project_id，故商户名单由运营显式提供。",
  additionalProperties: false,
  required: ["project_id", "period", "merchant_ids"],
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: { type: "string", description: "操作留痕备注；进入审计。", maxLength: 1000 },
    evidence_attachment_ids: {
      type: "array",
      description: "证据附件 ID 列表；进入审计留痕。",
      items: { type: "string", maxLength: 64 },
    },
    project_id: { type: "string", description: "项目 ID（费率按此 + period 解析）。", maxLength: 64 },
    period: { type: "string", description: "计费期（如 2026-06）。", maxLength: 32 },
    merchant_ids: {
      type: "array",
      description: "本期计费的商户 ID 列表（运营提供）。",
      items: { type: "string", maxLength: 64 },
    },
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
const reqBody = (name) => ({
  required: true,
  content: { "application/json": { schema: { $ref: `#/components/schemas/${name}` } } },
});
spec.components.requestBodies.PropertyFeeRateRequest = reqBody("PropertyFeeRateRequest");
spec.components.requestBodies.PropertyFeeBillRequest = reqBody("PropertyFeeBillRequest");

// ── operations ────────────────────────────────────────────────────────────────
const writeResponses = {
  "200": { $ref: "#/components/responses/Success" },
  "400": { $ref: "#/components/responses/Error" },
  "403": { $ref: "#/components/responses/Error" },
  "409": { $ref: "#/components/responses/Error" },
  "500": { $ref: "#/components/responses/Error" },
};

spec.paths["/api/v1/finance/property-fee/rates"] = {
  post: {
    tags: ["finance"],
    operationId: "createPropertyFeeRate",
    summary: "维护物业费费率（项目级、可按期）",
    "x-auth-context": "staff-token",
    "x-permission": "finance.property_fee.manage",
    "x-data-scope": "platform",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": true,
    "x-audit-event-success": "finance_property_fee.rate_set",
    "x-audit-event-failure": "finance_property_fee.rate_set_failed",
    requestBody: { $ref: "#/components/requestBodies/PropertyFeeRateRequest" },
    responses: writeResponses,
  },
  get: {
    tags: ["finance"],
    operationId: "listPropertyFeeRates",
    summary: "列出某项目的物业费费率",
    "x-auth-context": "staff-token",
    "x-permission": "finance.property_fee.manage",
    "x-data-scope": "platform",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": false,
    "x-audit-event-success": "finance_property_fee.rate_viewed",
    "x-audit-event-failure": "finance_property_fee.rate_view_failed",
    parameters: [
      {
        name: "project_id",
        in: "query",
        required: true,
        schema: { type: "string", maxLength: 64 },
        description: "项目 ID；按其列出物业费费率。",
      },
    ],
    responses: {
      "200": { $ref: "#/components/responses/Success" },
      "400": { $ref: "#/components/responses/Error" },
      "403": { $ref: "#/components/responses/Error" },
      "500": { $ref: "#/components/responses/Error" },
    },
  },
};
spec.paths["/api/v1/finance/property-fee/bills"] = {
  post: {
    tags: ["finance"],
    operationId: "billPropertyFee",
    summary: "物业费按期计费（每平米固定单价 × 面积，不退不补）",
    "x-auth-context": "staff-token",
    "x-permission": "finance.property_fee.manage",
    "x-data-scope": "platform",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": true,
    "x-audit-event-success": "finance_property_fee.billed",
    "x-audit-event-failure": "finance_property_fee.bill_failed",
    requestBody: { $ref: "#/components/requestBodies/PropertyFeeBillRequest" },
    responses: writeResponses,
  },
};

// ── 废止 CAM 分摊 / true-up：标记 deprecated（端点保留仅返回 409 CAM_*_ABOLISHED）──────
const camAlloc = spec.paths["/api/v1/finance/cam/allocations"];
if (camAlloc && camAlloc.post) {
  camAlloc.post.deprecated = true;
  camAlloc.post.summary = "（已废止）CAM 权重分摊 → 改用物业费固定计费；调用返回 409 CAM_ALLOCATION_ABOLISHED";
}
const camTrueup = spec.paths["/api/v1/finance/cam/trueups"];
if (camTrueup && camTrueup.post) {
  camTrueup.post.deprecated = true;
  camTrueup.post.summary = "（已废止）CAM 年度 true-up → 物业费不退不补、无对账；调用返回 409 CAM_TRUEUP_ABOLISHED";
}

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("property fee OpenAPI operations added; CAM allocate/trueup marked deprecated.");
