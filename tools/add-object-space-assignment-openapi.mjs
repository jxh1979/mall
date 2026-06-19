// One-off: add 处理清单 #8 空间指派/回填 operations to the P0 OpenAPI spec.
//   PUT /api/v1/bar/tables/{table_id}/space        设 bar_tables.space_id
//   GET /api/v1/bar/tables/without-space           列出 space_id 为空的桌台（运营回填核对）
//   PUT /api/v1/amusement/projects/{id}/space      设 amusement_projects.space_id
//   GET /api/v1/amusement/projects/without-space   列出 space_id 为空的项目（运营回填核对）
// 背景：bar_tables / amusement_projects 无可派生源，无法 SQL 自动回填 space_id；
//   over_space 判定依赖它，故提供指派端点让运营录入。
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
  description: "操作幂等键；重复提交时必须返回同一处理结果，避免重复指派或重复入账。",
  maxLength: 128,
};
const operatorNote = {
  type: "string",
  description: "操作备注；用于解释人工录入/回填判断，进入审计留痕。",
  maxLength: 1000,
};
const evidence = {
  type: "array",
  description: "证据附件 ID 列表；用于绑定空间归属说明、审批或现场凭证。",
  items: { type: "string", maxLength: 64 },
  maxItems: 50,
};
const spaceId = {
  type: "string",
  description: "目标空间 ID（spaces.id）；必须存在且属于同一商户，否则拒绝（跨商户禁止）。",
  maxLength: 64,
};

// ── schemas ─────────────────────────────────────────────────────────────────
spec.components.schemas.BarTableSpaceAssignmentRequest = {
  type: "object",
  description: "请求载荷 `BarTableSpaceAssignmentRequest`（处理清单 #8 空间指派/回填）；设 bar_tables.space_id。录入后 AdmissionGuard over_space 对该桌台关联账单（bar_bill→table_id→space_id）真实生效。bar_tables 无可派生源，无法 SQL 自动回填，须运营录入。",
  additionalProperties: false,
  required: ["space_id"],
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    space_id: spaceId,
  },
};
spec.components.schemas.AmusementProjectSpaceAssignmentRequest = {
  type: "object",
  description: "请求载荷 `AmusementProjectSpaceAssignmentRequest`（处理清单 #8 空间指派/回填）；设 amusement_projects.space_id。录入后 AdmissionGuard over_space 对该项目相关受控对象（inspection/maintenance_record/incident/group_booking 经 project_id→space_id）真实生效。amusement_projects 无可派生源，无法 SQL 自动回填，须运营录入。",
  additionalProperties: false,
  required: ["space_id"],
  properties: {
    idempotency_key: idempotencyKey,
    operator_note: operatorNote,
    evidence_attachment_ids: evidence,
    space_id: spaceId,
  },
};

// ── requestBodies ─────────────────────────────────────────────────────────────
const reqBody = (name) => ({
  required: true,
  content: { "application/json": { schema: { $ref: `#/components/schemas/${name}` } } },
});
spec.components.requestBodies.BarTableSpaceAssignmentRequest = reqBody("BarTableSpaceAssignmentRequest");
spec.components.requestBodies.AmusementProjectSpaceAssignmentRequest = reqBody("AmusementProjectSpaceAssignmentRequest");

// ── operations ────────────────────────────────────────────────────────────────
const writeResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  403: { $ref: "#/components/responses/Error" },
  404: { $ref: "#/components/responses/Error" },
  409: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};
const readResponses = {
  200: { $ref: "#/components/responses/Success" },
  400: { $ref: "#/components/responses/Error" },
  500: { $ref: "#/components/responses/Error" },
};

spec.paths["/api/v1/bar/tables/{table_id}/space"] = {
  put: {
    tags: ["bar"],
    operationId: "assignBarTableSpace",
    summary: "卡座/桌台空间指派（回填）",
    description: "处理清单 #8：设 bar_tables.space_id（运营录入/回填）。bar_tables 无可派生源，无法 SQL 自动回填；over_space 判定依赖该列。校验空间存在且同商户，currentMerchantScope 隔离。",
    "x-permission": "bar.table.manage",
    "x-data-scope": "project-bar-table",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": true,
    "x-audit-event-success": "bar.table_space.assigned",
    "x-audit-event-failure": "bar.table_space.assign_failed",
    "x-auth-context": "staff-token",
    parameters: [
      { $ref: "#/components/parameters/TableId", description: "TableId 路径参数；必须引用当前数据范围内存在且允许访问的记录。" },
    ],
    requestBody: { $ref: "#/components/requestBodies/BarTableSpaceAssignmentRequest" },
    responses: writeResponses,
  },
};
spec.paths["/api/v1/bar/tables/without-space"] = {
  get: {
    tags: ["bar"],
    operationId: "listBarTablesWithoutSpace",
    summary: "未回填空间的卡座/桌台清单",
    description: "处理清单 #8：列出 space_id 为空、待运营回填的桌台，便于核对（over_space 对这些桌台关联账单不生效，保守放行）。",
    "x-permission": "bar.table.manage",
    "x-data-scope": "project-bar-table",
    "x-terminal-allowed": ["web"],
    "x-audit-event-success": "bar.table_space_backlog.viewed",
    "x-audit-event-failure": "bar.table_space_backlog.view_failed",
    "x-auth-context": "staff-token",
    responses: readResponses,
  },
};
spec.paths["/api/v1/amusement/projects/{id}/space"] = {
  put: {
    tags: ["amusement"],
    operationId: "assignAmusementProjectSpace",
    summary: "游乐项目空间指派（回填）",
    description: "处理清单 #8：设 amusement_projects.space_id（运营录入/回填）。amusement_projects 无可派生源，无法 SQL 自动回填；over_space 判定依赖该列。校验空间存在且同商户，currentMerchantScope 隔离。",
    "x-permission": "amusement.session.manage",
    "x-data-scope": "project",
    "x-terminal-allowed": ["web"],
    "x-idempotency-required": true,
    "x-audit-event-success": "amusement.project_space.assigned",
    "x-audit-event-failure": "amusement.project_space.assign_failed",
    "x-auth-context": "staff-token",
    parameters: [
      { $ref: "#/components/parameters/Id", description: "Id 路径参数；必须引用当前数据范围内存在且允许访问的记录。" },
    ],
    requestBody: { $ref: "#/components/requestBodies/AmusementProjectSpaceAssignmentRequest" },
    responses: writeResponses,
  },
};
spec.paths["/api/v1/amusement/projects/without-space"] = {
  get: {
    tags: ["amusement"],
    operationId: "listAmusementProjectsWithoutSpace",
    summary: "未回填空间的游乐项目清单",
    description: "处理清单 #8：列出 space_id 为空、待运营回填的项目，便于核对（over_space 对这些项目相关受控对象不生效，保守放行）。",
    "x-permission": "amusement.session.manage",
    "x-data-scope": "project",
    "x-terminal-allowed": ["web"],
    "x-audit-event-success": "amusement.project_space_backlog.viewed",
    "x-audit-event-failure": "amusement.project_space_backlog.view_failed",
    "x-auth-context": "staff-token",
    responses: readResponses,
  },
};

fs.writeFileSync(jsonPath, JSON.stringify(spec, null, 2), "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(spec, { lineWidth: 120 }), "utf8");
console.log("object-space assignment OpenAPI operations added (#8).");
