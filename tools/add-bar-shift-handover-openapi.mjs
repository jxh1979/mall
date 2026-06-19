// One-off: add V095 bar shift handover / cash drawer reconciliation operations to the P0 OpenAPI spec.
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
  description: "操作幂等键；重复提交时必须返回同一处理结果，避免重复开班/班结或重复入账。",
  maxLength: 128,
};

// ── parameters ──────────────────────────────────────────────────────────────
spec.components.parameters.ShiftId = {
  name: "shift_id",
  in: "path",
  required: true,
  schema: { type: "string", maxLength: 64 },
  description: "ShiftId 路径参数；必须引用当前数据范围内存在且允许访问的收银班次。",
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.BarShiftOpenRequest = {
  type: "object",
  description:
    "请求载荷 `BarShiftOpenRequest`（V095 开班）；记录吧台、收银员、备用金（opening_cash_cent，可缺省 0）、现金差异复核阈值（cash_diff_threshold_cent，默认 0=任何差异即复核）；同一吧台同一收银员仅允许一个 open 班次。",
  additionalProperties: false,
  required: ["bar_id", "cashier_user_id"],
  properties: {
    idempotency_key: idempotencyKey,
    bar_id: { type: "string", description: "开班吧台 ID。", maxLength: 64 },
    cashier_user_id: { type: "string", description: "本班收银员用户 ID。", maxLength: 64 },
    project_id: { type: "string", description: "所属项目 ID（可空）。", maxLength: 64 },
    opening_cash_cent: { type: "integer", description: "备用金，单位分；不得使用浮点数。", minimum: 0, maximum: 999999999999 },
    cash_diff_threshold_cent: { type: "integer", description: "现金差异复核阈值，单位分；差异绝对值超此值进复核（默认 0）。", minimum: 0, maximum: 999999999999 },
    note: { type: "string", description: "开班备注；进入审计留痕。", maxLength: 1000 },
  },
};
spec.components.schemas.BarShiftCloseRequest = {
  type: "object",
  description:
    "请求载荷 `BarShiftCloseRequest`（V095 班结对账）；录入实点现金 declared_cash_cent，可选交接下一班收银员（handover_to_user_id）。系统应收（各支付方式/退款/小费）按班次时间窗口聚合，无需客户端传入；现金差异 = 实点 -（备用金 + 系统现金应收），超阈值进复核。",
  additionalProperties: false,
  required: ["declared_cash_cent"],
  properties: {
    idempotency_key: idempotencyKey,
    declared_cash_cent: { type: "integer", description: "班结实点现金（含备用金），单位分；不得使用浮点数。", minimum: 0, maximum: 999999999999 },
    handover_to_user_id: { type: "string", description: "交接下一班收银员用户 ID（可空）。", maxLength: 64 },
    note: { type: "string", description: "班结备注；进入审计留痕。", maxLength: 1000 },
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
const reqBody = (name) => ({
  required: true,
  content: { "application/json": { schema: { $ref: `#/components/schemas/${name}` } } },
});
spec.components.requestBodies.BarShiftOpenRequest = reqBody("BarShiftOpenRequest");
spec.components.requestBodies.BarShiftCloseRequest = reqBody("BarShiftCloseRequest");

// ── operations ────────────────────────────────────────────────────────────────
const writeResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};

spec.paths["/api/v1/bar/shifts"] = {
  get: {
    tags: ["bar"],
    operationId: "listBarShifts",
    summary: "查询收银班次",
    "x-permission": "bar.shift.manage",
    "x-data-scope": "project-bar-table",
    "x-terminal-allowed": ["tablet", "web"],
    "x-idempotency-required": false,
    "x-audit-event-success": "bar.shift.viewed",
    "x-audit-event-failure": "bar.shift.view_failed",
    "x-auth-context": "staff-token",
    responses: {
      200: { $ref: "#/components/responses/Page" },
      403: { $ref: "#/components/responses/Error" },
      500: { $ref: "#/components/responses/Error" },
    },
  },
};
spec.paths["/api/v1/bar/shifts/open"] = {
  post: {
    tags: ["bar"],
    operationId: "openBarShift",
    summary: "开班",
    "x-permission": "bar.shift.manage",
    "x-data-scope": "project-bar-table",
    "x-terminal-allowed": ["tablet", "web"],
    "x-idempotency-required": true,
    "x-audit-event-success": "bar.shift_opened",
    "x-audit-event-failure": "bar.shift_open_failed",
    "x-auth-context": "staff-token",
    requestBody: { $ref: "#/components/requestBodies/BarShiftOpenRequest" },
    responses: writeResponses,
  },
};
spec.paths["/api/v1/bar/shifts/{shift_id}/close"] = {
  post: {
    tags: ["bar"],
    operationId: "closeBarShift",
    summary: "班结对账",
    "x-permission": "bar.shift.close",
    "x-data-scope": "project-bar-table",
    "x-terminal-allowed": ["tablet", "web"],
    "x-idempotency-required": true,
    "x-audit-event-success": "bar.shift_closed",
    "x-audit-event-failure": "bar.shift_close_failed",
    "x-auth-context": "staff-token",
    parameters: [
      { $ref: "#/components/parameters/ShiftId", description: "undefined 参数；必须按 OpenAPI 类型、枚举和必填规则校验。" },
    ],
    requestBody: { $ref: "#/components/requestBodies/BarShiftCloseRequest" },
    responses: writeResponses,
  },
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("bar shift handover OpenAPI operations added.");
