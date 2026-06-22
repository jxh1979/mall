// One-off: add 票务订单取消 operation (cancelTicketOrder) 到 P0 OpenAPI spec。
// 背景：2a-5 库存超时释放 —— 下单即扣库存 + 未支付订单无取消/超时路径 → 限量库存被未支付订单永久锁死。
// 新增 POST /ticketing/orders/{order_id}/cancel：原子取消未支付订单（仅 created+unpaid）并回补库存。
// 配套 TicketOrderExpiryJob 定时按超时阈值（默认 30 分钟）自动取消未支付订单并回补库存。
// Source of truth = JSON; YAML regenerated from JSON. Idempotent.
import fs from "node:fs";
import path from "node:path";
import YAML from "yaml";

const root = process.cwd();
const jsonPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const yamlPath = path.join(root, "docs", "openapi", "openapi-p0.yaml");
const spec = JSON.parse(fs.readFileSync(jsonPath, "utf8"));

const successResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  403: { $ref: "#/components/responses/Error" },
  404: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};

spec.paths["/api/v1/ticketing/orders/{order_id}/cancel"] = {
  post: {
    tags: ["ticketing"],
    operationId: "cancelTicketOrder",
    summary: "取消未支付票务订单（回补库存）",
    "x-permission": "ticket.order.create",
    "x-data-scope": "platform",
    "x-terminal-allowed": ["web", "kiosk"],
    "x-idempotency-required": true,
    "x-audit-event-success": "ticket_order.cancelled",
    "x-audit-event-failure": "ticket_order.cancel_failed",
    "x-auth-context": "staff-token",
    parameters: [{ $ref: "#/components/parameters/OrderId" }],
    requestBody: { $ref: "#/components/requestBodies/StatusChangeRequest" },
    responses: successResponses,
  },
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("ticketing cancel-order OpenAPI operation added.");
