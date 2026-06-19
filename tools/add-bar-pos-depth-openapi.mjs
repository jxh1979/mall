// One-off: add V091 bar POS depth operations (void item / reopen bill / sold-out / tip) to the P0 OpenAPI spec.
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
  description: "操作幂等键；重复提交时必须返回同一处理结果，避免重复出票、扣减或入账。",
  maxLength: 128,
};
const operatorNote = {
  type: "string",
  description: "操作备注；用于解释人工处理、异常处理或审批判断，进入审计留痕。",
  maxLength: 1000,
};
const evidence = {
  type: "array",
  description: "证据附件 ID 列表；用于绑定图片、合同、审批、支付凭证或异常说明。",
  items: { type: "string", maxLength: 64 },
  maxItems: 50,
};

// ── parameters ──────────────────────────────────────────────────────────────
spec.components.parameters.ProductId = {
  name: "product_id",
  in: "path",
  required: true,
  schema: { type: "string", maxLength: 64 },
  description: "ProductId 路径参数；必须引用当前数据范围内存在且允许访问的记录。",
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.BarOrderItemVoidRequest = {
  type: "object",
  description: "请求载荷 `BarOrderItemVoidRequest`（V091 退菜退酒）；未出品=取消明细，已出品=退酒并按 return_to_stock 回补库存，资金从账单扣减。",
  additionalProperties: false,
  required: ["order_item_id", "reason"],
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    order_item_id: { type: "string", description: "待退菜退酒的订单明细 ID；必须属于路径订单。", maxLength: 64 },
    reason: { type: "string", description: "退菜退酒原因码/说明；进入审计与责任归属留痕。", maxLength: 500 },
    return_to_stock: { type: "boolean", description: "已出品退酒是否回补库存（仅 delivered 明细生效）；未出品撤单不计库存损耗。" },
    bill_id: { type: "string", description: "关联账单 ID；提供时资金回退（未结=减应付/已结=退款冲正）。", maxLength: 64 },
  },
};
spec.components.schemas.BarBillReopenRequest = {
  type: "object",
  description: "请求载荷 `BarBillReopenRequest`（V091 反结）；已结账单 settled→reopened 需原因+高危权限+审批，与日结锁定协同（锁定日须走补录审批）。",
  additionalProperties: false,
  required: ["reason"],
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    reason: { type: "string", description: "反结原因；进入主管复核与审计留痕。", maxLength: 500 },
  },
};
spec.components.schemas.BarProductSoldOutRequest = {
  type: "object",
  description: "请求载荷 `BarProductSoldOutRequest`（V091 沽清/估清 86）；soldOut=true 置沽清，false 恢复供应；沽清后下单该品被拒。",
  additionalProperties: false,
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    sold_out: { type: "boolean", description: "true=置沽清(86)，false=恢复供应；省略默认置沽清。" },
    reason: { type: "string", description: "沽清/恢复原因；进入审计留痕。", maxLength: 500 },
  },
};
spec.components.schemas.BarBillTipRequest = {
  type: "object",
  description: "请求载荷 `BarBillTipRequest`（V091 小费）；tip_amount_cent 或 tip_ratio_bps 二选一；小费计入 payable 与结算（代收口径，不计低消/提成基数）。",
  additionalProperties: false,
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    tip_amount_cent: { type: "integer", description: "`tip_amount_cent` 小费固定额，单位分；不得使用浮点数。", minimum: 0, maximum: 999999999999 },
    tip_ratio_bps: { type: "integer", description: "小费比例（基点 bp，10000=100%）；按账单应付计算小费额。", minimum: 0, maximum: 100000 },
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
const reqBody = (name) => ({
  required: true,
  content: { "application/json": { schema: { $ref: `#/components/schemas/${name}` } } },
});
spec.components.requestBodies.BarOrderItemVoidRequest = reqBody("BarOrderItemVoidRequest");
spec.components.requestBodies.BarBillReopenRequest = reqBody("BarBillReopenRequest");
spec.components.requestBodies.BarProductSoldOutRequest = reqBody("BarProductSoldOutRequest");
spec.components.requestBodies.BarBillTipRequest = reqBody("BarBillTipRequest");

// ── operations ────────────────────────────────────────────────────────────────
const responses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const op = ({ operationId, summary, permission, success, failure, paramRef, body }) => ({
  tags: ["bar"],
  operationId,
  summary,
  "x-permission": permission,
  "x-data-scope": "project-bar-table",
  "x-terminal-allowed": ["tablet", "web"],
  "x-idempotency-required": true,
  "x-audit-event-success": success,
  "x-audit-event-failure": failure,
  "x-auth-context": "staff-token",
  parameters: [{ $ref: `#/components/parameters/${paramRef}`, description: "undefined 参数；必须按 OpenAPI 类型、枚举和必填规则校验。" }],
  requestBody: { $ref: `#/components/requestBodies/${body}` },
  responses,
});

spec.paths["/api/v1/bar/orders/{order_id}/void-item"] = {
  post: op({
    operationId: "voidBarOrderItem",
    summary: "退菜退酒",
    permission: "bar.order_item.void",
    success: "bar.order_item_voided",
    failure: "bar.order_item_void_failed",
    paramRef: "OrderId",
    body: "BarOrderItemVoidRequest",
  }),
};
spec.paths["/api/v1/bar/bills/{bill_id}/reopen"] = {
  post: op({
    operationId: "reopenBarBill",
    summary: "账单反结",
    permission: "bar.bill.reopen",
    success: "bar.bill_reopened",
    failure: "bar.bill_reopen_failed",
    paramRef: "BillId",
    body: "BarBillReopenRequest",
  }),
};
spec.paths["/api/v1/bar/bills/{bill_id}/tip"] = {
  post: op({
    operationId: "addBarBillTip",
    summary: "账单加小费",
    permission: "bar.bill.tip",
    success: "bar.bill_tip_added",
    failure: "bar.bill_tip_failed",
    paramRef: "BillId",
    body: "BarBillTipRequest",
  }),
};
spec.paths["/api/v1/bar/products/{product_id}/sold-out"] = {
  post: op({
    operationId: "markBarProductSoldOut",
    summary: "商品沽清/恢复",
    permission: "bar.product.sold_out",
    success: "bar.product_sold_out",
    failure: "bar.product_sold_out_failed",
    paramRef: "ProductId",
    body: "BarProductSoldOutRequest",
  }),
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("bar POS depth OpenAPI operations added.");
