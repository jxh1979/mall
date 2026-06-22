// One-off: add 票务订单支付确认 operation (confirmTicketOrderPayment) 到 P0 OpenAPI spec。
// 背景：2a-2 支付回写闭环 —— 此前非 admin 渠道无支付成功回调，订单永卡 unpaid 无法出票（issueOrder 反写 paid 因果倒置）。
// 新增 POST /ticketing/orders/{order_id}/confirm-payment：校验支付意图归属/金额后置意图 succeeded 并回写订单 pay_status=paid。
// 内部确认（staff/对账）与真实第三方回调共用本入口（用户裁定"两者都做"）。
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
  description: "操作幂等键；重复确认必须返回同一处理结果，避免重复回写 / 重复入账。",
  maxLength: 128,
};

// ── schema ─────────────────────────────────────────────────────────────────
spec.components.schemas.ConfirmTicketOrderPaymentRequest = {
  type: "object",
  description:
    "请求载荷 `ConfirmTicketOrderPaymentRequest`（2a-2 票务支付回写闭环）；payment_intent_id 必填，服务端据其校验归属（须属本订单）与金额（须等于订单金额）后置支付意图 succeeded 并回写订单 pay_status=paid。内部确认与真实回调共用本入口。",
  additionalProperties: false,
  required: ["payment_intent_id"],
  properties: {
    idempotency_key: idempotencyKey,
    payment_intent_id: {
      type: "string",
      description: "被确认的票务支付意图 id（必填，须属本订单且金额相符）。",
      maxLength: 128,
    },
    gateway_ref: {
      type: "string",
      description: "真实第三方回调渠道流水号（可空）；内部 staff/对账确认可留空，仅作留痕。",
      maxLength: 128,
    },
  },
};

// ── requestBody ──────────────────────────────────────────────────────────────
spec.components.requestBodies.ConfirmTicketOrderPaymentRequest = {
  required: true,
  content: { "application/json": { schema: { $ref: "#/components/schemas/ConfirmTicketOrderPaymentRequest" } } },
};

// ── operation ────────────────────────────────────────────────────────────────
const successResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  403: { $ref: "#/components/responses/Error" },
  404: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};

const orderIdParam = {
  name: "order_id",
  in: "path",
  required: true,
  schema: { type: "string", maxLength: 64 },
  description: "票务订单 id（路径参数，必填）。",
};

spec.paths["/api/v1/ticketing/orders/{order_id}/confirm-payment"] = {
  post: {
    tags: ["ticketing"],
    operationId: "confirmTicketOrderPayment",
    summary: "确认票务订单支付成功（回写 paid）",
    "x-permission": "finance.payment.register",
    "x-data-scope": "platform",
    "x-terminal-allowed": ["web", "kiosk"],
    "x-idempotency-required": true,
    "x-audit-event-success": "ticket_order.payment_confirmed",
    "x-audit-event-failure": "ticket_order.payment_confirm_failed",
    "x-auth-context": "staff-token",
    parameters: [orderIdParam],
    requestBody: { $ref: "#/components/requestBodies/ConfirmTicketOrderPaymentRequest" },
    responses: successResponses,
  },
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("ticketing confirm-payment OpenAPI operation added.");
