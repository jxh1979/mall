// 文件版本：V0.03
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const openapiPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const appendixPath = path.join(root, "docs", "p0-openapi-contract-closure-appendix-v0.1.md");
const permissionMatrixPath = path.join(root, "docs", "p0-operation-permission-matrix-v0.1.md");
const eventMatrixPath = path.join(root, "docs", "p0-action-event-evidence-matrix-v0.1.md");
const taskMapPath = path.join(root, "docs", "p0-task-api-permission-event-acceptance-map-v0.1.md");

const checkOnly = process.argv.includes("--check");
const methods = ["get", "post", "put", "patch", "delete"];

const openapi = JSON.parse(fs.readFileSync(openapiPath, "utf8"));
const permissionMatrix = readIfExists(permissionMatrixPath);
const eventMatrix = readIfExists(eventMatrixPath);
const taskMap = readIfExists(taskMapPath);

function readIfExists(file) {
  return fs.existsSync(file) ? fs.readFileSync(file, "utf8") : "";
}

function escapeCell(value) {
  return String(value ?? "-").replace(/\r?\n/g, "<br>").replace(/\|/g, "\\|");
}

function code(value) {
  return `\`${escapeCell(value)}\``;
}

function formatList(value) {
  if (Array.isArray(value)) return value.filter(Boolean).join(", ");
  return value || "-";
}

function terminalAndChannel(operation) {
  const parts = [];
  if (operation["x-terminal-allowed"]) parts.push(formatList(operation["x-terminal-allowed"]));
  if (operation["x-device-capability"]) parts.push(`device: ${formatList(operation["x-device-capability"])}`);
  if (operation["x-invocation-channel"]) parts.push(`channel: ${formatList(operation["x-invocation-channel"])}`);
  return parts.length ? parts.join("<br>") : "-";
}

function taskGroup(tag, operationId) {
  const id = operationId.toLowerCase();
  if (tag === "access") return "AUTH-账号权限";
  if (tag === "auth") return "AUTH-登录会话";
  if (tag === "platform") return "GOV-平台治理";
  if (tag === "terminals") return "TERM-终端设备";
  if (tag === "ticketing") return "TIC-票务权益";
  if (tag === "customer" && id.includes("ticket")) return "CUS/TIC-客人购票";
  if (tag === "customer" && id.includes("bar")) return "CUS/BAR-客人订台";
  if (tag === "customer") return "CUS-客人端";
  if (tag === "bar") return "BAR-酒吧经营";
  if (tag === "inventory") return "INV-库存存酒";
  if (tag === "amusement") return "AMU-游乐安全";
  if (tag === "finance") return "FIN-财务台账";
  if (tag === "integration") return "INT-第三方集成";
  if (tag === "open-platform") return "OPEN-开放平台";
  if (tag === "evidence") return "EVD-事件证据";
  if (tag === "analytics") return "ANA-风控分析";
  if (tag === "system") return "SYS-系统健康";
  if (tag === "ops") return "OPS-生产前置";
  if (tag === "staff") return "STAFF-员工";
  return "P0-待归类";
}

const operations = [];
for (const [apiPath, pathItem] of Object.entries(openapi.paths || {})) {
  for (const method of methods) {
    const operation = pathItem[method];
    if (!operation) continue;
    operations.push({
      tag: operation.tags?.[0] || "-",
      method: method.toUpperCase(),
      path: apiPath,
      operationId: operation.operationId || "-",
      summary: operation.summary || "-",
      permission: operation["x-permission"] || "-",
      authContext: operation["x-auth-context"] || "-",
      dataScope: operation["x-data-scope"] || "-",
      terminal: terminalAndChannel(operation),
      idempotency: operation["x-idempotency-required"] ? "是" : "否",
      successEvent: operation["x-audit-event-success"] || "-",
      failureEvent: operation["x-audit-event-failure"] || "-",
      taskGroup: taskGroup(operation.tags?.[0] || "-", operation.operationId || ""),
      isWrite: ["POST", "PUT", "PATCH", "DELETE"].includes(method.toUpperCase()),
    });
  }
}

const permissions = new Map();
for (const operation of operations) {
  const permission = operation.permission;
  if (!permissions.has(permission)) {
    permissions.set(permission, {
      permission,
      authContexts: new Set(),
      dataScopes: new Set(),
      operations: [],
      coveredInHumanMatrix: permissionMatrix.includes(permission),
    });
  }
  const item = permissions.get(permission);
  item.authContexts.add(operation.authContext);
  item.dataScopes.add(operation.dataScope);
  item.operations.push(operation.operationId);
}

const events = [];
for (const operation of operations) {
  events.push({
    event: operation.successEvent,
    result: "success",
    operationId: operation.operationId,
    method: operation.method,
    path: operation.path,
    taskGroup: operation.taskGroup,
    permission: operation.permission,
    coveredInHumanMatrix: eventMatrix.includes(operation.successEvent),
  });
  events.push({
    event: operation.failureEvent,
    result: "failure",
    operationId: operation.operationId,
    method: operation.method,
    path: operation.path,
    taskGroup: operation.taskGroup,
    permission: operation.permission,
    coveredInHumanMatrix: eventMatrix.includes(operation.failureEvent),
  });
}

const taskRows = operations.map((operation) => ({
  ...operation,
  coveredInHumanTaskMap: taskMap.includes(operation.operationId),
}));

const missingPermissionCount = [...permissions.values()].filter((item) => !item.coveredInHumanMatrix).length;
const missingEventCount = events.filter((item) => !item.coveredInHumanMatrix).length;
const missingTaskCount = taskRows.filter((item) => !item.coveredInHumanTaskMap).length;
const writeOperations = operations.filter((item) => item.isWrite);

const permissionRows = [...permissions.values()]
  .sort((a, b) => a.permission.localeCompare(b.permission))
  .map((item) => {
    const status = item.coveredInHumanMatrix ? "已在权限矩阵正文出现" : "OpenAPI 补充，以本附录为准";
    return `| ${code(item.permission)} | ${escapeCell([...item.authContexts].sort().join(", "))} | ${escapeCell([...item.dataScopes].sort().join(", "))} | ${item.operations.length} | ${escapeCell(item.operations.slice(0, 12).join(", "))}${item.operations.length > 12 ? " ..." : ""} | ${status} |`;
  })
  .join("\n");

const eventRows = events
  .sort((a, b) => a.event.localeCompare(b.event) || a.operationId.localeCompare(b.operationId))
  .map((item) => {
    const status = item.coveredInHumanMatrix ? "已在事件矩阵正文出现" : "接口级事件，以本附录为准";
    return `| ${code(item.event)} | ${item.result} | ${code(item.operationId)} | ${code(item.method)} | ${code(item.path)} | ${escapeCell(item.taskGroup)} | ${code(item.permission)} | ${status} |`;
  })
  .join("\n");

const taskOperationRows = taskRows
  .sort((a, b) => a.taskGroup.localeCompare(b.taskGroup) || a.operationId.localeCompare(b.operationId))
  .map((item) => {
    const status = item.coveredInHumanTaskMap ? "已在任务映射正文出现" : "operation 级补充";
    return `| ${escapeCell(item.taskGroup)} | ${code(item.operationId)} | ${code(item.method)} | ${code(item.path)} | ${escapeCell(item.summary)} | ${code(item.permission)} | ${code(item.successEvent)} / ${code(item.failureEvent)} | ${escapeCell(item.terminal)} | ${status} |`;
  })
  .join("\n");

const exampleCoverage = openapi["x-p0-example-coverage"] || {};
const exampleRequestBodies = Array.isArray(exampleCoverage.request_bodies)
  ? exampleCoverage.request_bodies.join(", ")
  : "-";
const exampleResponses = Array.isArray(exampleCoverage.responses) ? exampleCoverage.responses.join(", ") : "-";

// U17-007：标题版本跟随契约源 info.version（本文件是 openapi-p0.json 的投影；从现文件回读会让内容变化不入账）。
const documentVersion = String(openapi.info?.version ?? "").match(/V?\d+\.\d+/) ? `V${String(openapi.info.version).replace(/^V/, "")}` : "V0.01";
const document = `# P0 OpenAPI 权限、事件、任务追溯收口附录 ${documentVersion}

## 1. 文档定位

本文档由 \`docs/openapi/openapi-p0.json\` 反向生成，用于收口本轮文档审计发现的三个执行缺口：

1. OpenAPI 已声明的权限码没有全部进入人工权限矩阵。
2. OpenAPI 已声明的成功/失败事件没有全部进入人工事件证据矩阵。
3. P0 任务文档不能逐 operation 追溯到权限、事件、终端和验收责任。

本文不是新需求池，不改变已拍板业务口径。若本文与历史 PRD、历史接口表或旧能力路径冲突，以 \`00-current-effective-document-entry-v0.1.md\`、\`docs/openapi/openapi-p0.yaml/json\`、\`p0-openapi-operation-acceptance-matrix-v0.1.md\` 和本文为准。

## 2. 收口结论

| 检查项 | 当前结果 | 执行结论 |
|---|---:|---|
| OpenAPI operation 数 | ${operations.length} | 全部必须进入 Java Controller、DTO、Service、权限、事件和验收追溯 |
| OpenAPI 权限/通用权限状态数 | ${permissions.size} | 人工权限矩阵正文未覆盖 ${missingPermissionCount} 项；缺口以本文第 3 节补齐 |
| OpenAPI 成功/失败事件数 | ${events.length} | 人工事件矩阵正文未覆盖 ${missingEventCount} 项；完整事件字典以本文第 4 节为准 |
| operation 级任务追溯 | ${operations.length} | 人工任务映射正文未逐项覆盖 ${missingTaskCount} 个 operation；逐 operation 映射以本文第 5 节为准 |
| 写接口数量 | ${writeOperations.length} | 写接口、外部接入接口进入编码或联调前必须补请求/响应示例 |

## 3. 权限码与认证上下文补充表

> 本节覆盖 OpenAPI 中全部 \`x-permission\`。其中 \`public\`、\`authenticated\`、\`not_applicable\` 是通用权限状态，不是员工岗位权限；员工、设备、客人、开放应用和 Webhook 的身份来源必须看 \`x-auth-context\`。

| 权限码/通用状态 | 认证上下文 | 数据范围 | operation 数 | 代表 operationId | 覆盖状态 |
|---|---|---:|---:|---|---|
${permissionRows}

## 4. OpenAPI 事件字典

> 本节覆盖每个 operation 的 \`x-audit-event-success\` 和 \`x-audit-event-failure\`。人工事件证据矩阵保留高风险证据策略；接口级完整事件名以本节为准。实现时不得只更新业务表而不写事件。

| 事件名 | 结果 | operationId | 方法 | 路径 | 建议任务簇 | 权限 | 覆盖状态 |
|---|---|---|---|---|---|---|---|
${eventRows}

## 5. operationId 到任务、权限、事件、终端的追溯表

> 本节用于开发排期、测试用例、供应商联调和审计抽样。任务簇是执行归类，不替代 \`p0-implementation-task-list.md\` 中的正式任务编号；若某 operation 准备进入编码，必须在具体任务单中引用本表 operationId。

| 建议任务簇 | operationId | 方法 | 路径 | 摘要 | 权限 | 成功/失败事件 | 终端/调用通道 | 覆盖状态 |
|---|---|---|---|---|---|---|---|---|
${taskOperationRows}

## 6. OpenAPI 示例覆盖执行口径

当前 OpenAPI 已有 P0 首批高风险样例覆盖：

| 类型 | 已覆盖 |
|---|---|
| request body | ${escapeCell(exampleRequestBodies)} |
| response | ${escapeCell(exampleResponses)} |

执行口径：

1. 上表只是 P0 首批高风险样例，不代表所有写接口和外部接口样例已齐。
2. 任一写接口或外部接入接口进入 Controller、SDK/mock、接口自动化或供应商联调前，必须补齐请求样例、成功响应样例、主要失败响应样例。
3. 支付、核销、订台、出票、退款、年龄/人脸核验、应收应付、Webhook、轮询校验和补偿接口不得无样例进入联调。
4. 示例字段必须使用 OpenAPI schema 的 snake_case 字段名，不得沿用旧 TypeScript/SQLite 原型字段。

## 7. 验收标准

### AC-CLOSURE-001 权限闭环

Given 任一 P0 operation 进入开发，When 开发者查看权限要求，Then 必须能从 OpenAPI 和本文第 3 节找到 \`x-permission\`、\`x-auth-context\`、\`x-data-scope\` 和允许终端。

### AC-CLOSURE-002 事件闭环

Given 任一 P0 operation 成功或失败，When 测试审计事件，Then 必须能从本文第 4 节找到对应成功/失败事件名，并在事件中心查到操作者、终端、设备、业务对象、结果、失败原因和 trace_id。

### AC-CLOSURE-003 任务追溯闭环

Given 任一 P0 任务准备进入编码，When 任务包含后端接口、前端调用、Android 调用或供应商联调，Then 任务说明必须引用本文第 5 节中的 operationId，不得只写“见相关接口”。

### AC-CLOSURE-004 示例准入

Given 任一 P0 写接口或外部接口进入联调，When 没有请求样例、成功响应样例和主要失败响应样例，Then 该接口不得标记为联调就绪。

### AC-CLOSURE-005 历史路径隔离

Given 文档中仍保留 \`/api/admin/*\`、\`/api/mobile/*\`、\`/api/integration/*\` 历史能力路径，When 开发或测试引用接口，Then 必须改用 OpenAPI exact path 或 operationId；历史路径不得作为 Controller、SDK/mock 或接口自动化依据。

## 8. 边界场景

| 场景 | 必测要求 |
|---|---|
| 权限码只在 OpenAPI 出现，不在人工权限矩阵正文出现 | 以本文第 3 节为准，并在下一次权限矩阵维护时同步 |
| 事件名只在 OpenAPI 出现，不在人工事件矩阵正文出现 | 以本文第 4 节为准，事件中心必须照常落库 |
| 客人端、设备端、Webhook 使用 \`not_applicable\` | 不得误建员工权限点，必须按 \`x-auth-context\` 校验身份 |
| 售票型与非售票型一体机能力混用 | 按 \`kiosk_mode\`、设备绑定和能力开关拒绝（服务端按可信终端判，一体机售票只认启用售票的一体机）；\`x-terminal-allowed\` 是契约声明，列出可调用该接口的终端，运行时不逐接口按它拦 |
| 旧路径表格与 OpenAPI 冲突 | 旧路径表格只作为历史能力名，正式实现以 OpenAPI 和本文为准 |

版本说明：本文档版本与 \`docs/openapi/openapi-p0.json\` 的 \`info.version\` 同步，契约变更即版本递增；不另维护变更记录表（历史见 OpenAPI 与 git 记录）。
`;

function normalize(value) {
  return value.replace(/\r\n/g, "\n").trimEnd();
}

if (checkOnly) {
  const current = readIfExists(appendixPath);
  if (normalize(current) !== normalize(document)) {
    console.error(`${appendixPath} is out of sync with ${openapiPath}; run npm run docs:openapi-closure`);
    process.exit(1);
  }
  console.log(`${appendixPath} is in sync with ${openapiPath}; ${operations.length} operations checked.`);
} else {
  fs.writeFileSync(appendixPath, document, "utf8");
  console.log(`generated ${appendixPath} with ${operations.length} operations, ${permissions.size} permissions, ${events.length} events`);
}
