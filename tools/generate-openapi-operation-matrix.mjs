// 文件版本：V0.03
import fs from "node:fs";

const jsonPath = "docs/openapi/openapi-p0.json";
const matrixPath = "docs/p0-openapi-operation-acceptance-matrix-v0.1.md";
const openapi = JSON.parse(fs.readFileSync(jsonPath, "utf8"));
const methods = ["get", "post", "put", "patch", "delete"];
const checkOnly = process.argv.includes("--check");

function escapeCell(value) {
  return String(value ?? "-").replace(/\r?\n/g, "<br>").replace(/\|/g, "\\|");
}

function resolveRef(ref) {
  if (!ref?.startsWith("#/")) return undefined;
  return ref
    .slice(2)
    .split("/")
    .reduce((current, part) => current?.[part], openapi);
}

function refName(schema) {
  if (!schema) return "-";
  if (schema.$ref) return schema.$ref.split("/").pop();
  if (schema.allOf?.[0]?.$ref) return schema.allOf[0].$ref.split("/").pop();
  return schema.type || "inline";
}

function requestSchema(operation) {
  let body = operation.requestBody;
  if (!body) return "-";
  if (body.$ref) body = resolveRef(body.$ref);
  const content = body?.content || {};
  const media = content["application/json"] || content["multipart/form-data"] || Object.values(content)[0];
  return refName(media?.schema);
}

function formatList(value) {
  if (Array.isArray(value)) return value.filter(Boolean).join(", ");
  return value || "";
}

function channel(operation) {
  const parts = [];
  const terminals = formatList(operation["x-terminal-allowed"]);
  const capabilities = formatList(operation["x-device-capability"]);
  const invocation = formatList(operation["x-invocation-channel"]);
  if (terminals) parts.push(terminals);
  if (capabilities) parts.push(`capability: ${capabilities}`);
  if (invocation) parts.push(`channel: ${invocation}`);
  return parts.length ? parts.join("<br>") : "-";
}

const rows = [];
for (const [path, item] of Object.entries(openapi.paths || {})) {
  for (const method of methods) {
    const operation = item[method];
    if (!operation) continue;
    rows.push({
      tag: operation.tags?.[0] || "-",
      method: method.toUpperCase(),
      path,
      operationId: operation.operationId || "-",
      summary: operation.summary || "-",
      requestBody: requestSchema(operation),
      permission: operation["x-permission"] || "-",
      authContext: operation["x-auth-context"] || "-",
      dataScope: operation["x-data-scope"] || "-",
      terminal: channel(operation),
      idempotency: operation["x-idempotency-required"] ? "是" : "否",
      successEvent: operation["x-audit-event-success"] || operation["x-success-event"] || "-",
      failureEvent: operation["x-audit-event-failure"] || operation["x-failure-event"] || "-",
    });
  }
}

const tableRows = rows
  .map((row) =>
    `| ${escapeCell(row.tag)} | \`${escapeCell(row.method)}\` | \`${escapeCell(row.path)}\` | \`${escapeCell(row.operationId)}\` | ${escapeCell(row.summary)} | \`${escapeCell(row.requestBody)}\` | \`${escapeCell(row.permission)}\` | \`${escapeCell(row.authContext)}\` | \`${escapeCell(row.dataScope)}\` | ${escapeCell(row.terminal)} | ${row.idempotency} | \`${escapeCell(row.successEvent)}\` | \`${escapeCell(row.failureEvent)}\` |`
  )
  .join("\n");

// U17-008/U18：标题版本跟随契约源 info.version（本文件是 openapi-p0.json 的投影；从现文件回读会让内容变化不入账）。
const documentVersion = String(openapi.info?.version ?? "").match(/V?\d+\.\d+/) ? `V${String(openapi.info.version).replace(/^V/, "")}` : "V0.01";
const document = `# P0 OpenAPI Operation 验收矩阵 ${documentVersion}

## 1. 文档定位

本文基于 \`docs/openapi/openapi-p0.yaml\` 与 \`docs/openapi/openapi-p0.json\` 生成，是 P0 可编码接口、权限、终端、幂等、审计事件和测试验收的逐接口矩阵。后端、前端、Android 终端、一体机、客人端、小程序/App/Web、测试与第三方供应商联调均以本文为执行入口。

本文不是新需求池；若本文与业务 PRD、技术方案或旧接口表冲突，以 OpenAPI、统一口径基准、权限矩阵、OpenAPI 追溯附录和本文为准。未进入 OpenAPI 与 \`p0-openapi-contract-closure-appendix-v0.1.md\` 的能力不得直接进入 P0 开发。

## 2. 强制规则

1. 每个 operation 必须对应 Spring Boot Controller 方法、请求 DTO、响应 DTO、Service 方法、权限校验、审计事件、追溯附录条目和验收用例。
2. \`x-permission\` 只表达员工或系统权限点，或使用 \`public\`、\`authenticated\`、\`not_applicable\`；客人 Token、设备凭证、开放应用凭证、Webhook 签名统一写入 \`x-auth-context\`。
3. \`requestBody\` 不允许继续使用 \`GenericWrite\`；每个写接口必须有独立 schema，字段名、必填项、枚举和单位必须可直接生成 DTO。
4. \`x-idempotency-required=true\` 的接口必须校验 \`X-Idempotency-Key\`，并把重复提交返回、并发锁、失败重放、审计留痕纳入测试。
5. \`x-terminal-allowed\` 只能使用 \`unified-standards-baseline-v0.1.md\` D5 的 10 个终端码；门禁设备、人脸年龄设备、监控和服务端调用必须使用 \`x-device-capability\` 或 \`x-invocation-channel\` 表达，不能伪装成终端类型。
6. 本文列出的成功事件和失败事件必须进入事件中心；不能只做业务表更新而没有操作留痕。

## 3. Operation 清单

> 当前 \`docs/openapi/openapi-p0.json\` 共 ${rows.length} 个 operation。下表由 OpenAPI 生成；若历史变更记录中的 operation 数量与本节不一致，以本节和 OpenAPI 为准。

| 标签 | 方法 | 路径 | operationId | 摘要 | 请求体 | 权限 | 认证上下文 | 数据范围 | 终端/设备能力/调用通道 | 幂等 | 成功事件 | 失败事件 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
${tableRows}

## 4. 通用 Given/When/Then 验收标准

### AC-OP-001 接口实现一致性

Given 开发领取任一 P0 接口任务，When 提交 Controller、DTO、Service、Repository 和测试用例，Then 接口路径、方法、operationId、字段、枚举、单位、权限码、鉴权上下文、审计事件必须与 OpenAPI、本文和 \`p0-openapi-contract-closure-appendix-v0.1.md\` 一致；不得新增 \`/api/admin/*\`、\`/api/mobile/*\`、\`/api/integration/*\` 等旧草案路径作为正式 P0 接口。

### AC-OP-002 权限与鉴权上下文分离

Given 接口需要访问控制，When 访问方是员工、商户员工、客人、设备、开放应用或第三方 Webhook，Then 系统必须先按 \`x-auth-context\` 完成鉴权，再按 \`x-permission\` 和 \`x-data-scope\` 完成授权；客人 Token、设备凭证、开放应用凭证和 Webhook 签名不得被误写成员工权限码。

### AC-OP-003 请求字段可测试

Given 写接口有 requestBody，When 测试读取 schema 的 required、type、format、enum、minimum、maximum 和 description，Then 必须能生成字段级正向、缺字段、错类型、越界、枚举非法、金额单位错误和幂等重复提交测试。

### AC-OP-004 幂等与并发一致性

Given 接口声明 \`x-idempotency-required=true\` 或业务涉及支付、核销、库存、入账、合同审批、退款、酒水领取、存酒、卡座预订等不可重复动作，When 同一请求被重复提交、网络超时后重放、或并发执行，Then 系统必须返回同一业务结果或明确拒绝，并写入可追溯事件。

### AC-OP-005 终端边界

Given 某接口只允许特定终端调用，When 其它终端、设备或 Web 页面尝试调用，Then 服务端按可信终端（设备凭证、会话、账号类型派生）拦 HTML 限定了终端的接口——私人手机与平板只放行清单内接口，客人只经 App 或小程序，经销商门户只认门户终端，一体机售票只认启用售票的一体机，服务点单现场动作不许私人手机，拒绝时记录失败事件；\`x-terminal-allowed\` 是契约声明，列出可调用该接口的终端，运行时不逐接口按它拦；门禁核销、人脸年龄核验、监控和服务端调用必须额外校验 \`x-device-capability\` 或 \`x-invocation-channel\`；一体机售票版与非售票版必须按设备能力开关决定可用接口。

### AC-OP-006 留痕闭环

Given 任一 operation 被调用，When 成功、失败、部分成功、超时、供应商回调异常或人工补录发生，Then 事件中心必须记录操作者、终端、设备、对象、前后状态、失败原因、关联订单/票/权益/酒水/卡座/合同/财务流水 ID。

### AC-OP-007 第三方适配器边界

Given 机器人、视频平台、支付、短信、人脸、企业微信或设备厂商 SDK/API 尚未最终到位，When 进入 P0 编码，Then 只能实现平台侧标准接口、Adapter 抽象、mock 契约、Webhook 接收和轮询校验框架；供应商字段映射、签名细节、异常码和补偿策略必须在供应商资料到位后补齐。

### AC-OP-008 Springdoc 反向校验

Given Java Spring Boot 代码完成，When 通过 springdoc 生成运行时 OpenAPI，Then 必须与 \`docs/openapi/openapi-p0.yaml\` 的 operationId、路径、方法、schema、权限扩展字段、审计事件和 OpenAPI 追溯附录一致；差异必须在代码或文档中收口，不能由测试口头解释。

## 5. 边界与异常场景

| 场景 | 风险 | 必测要求 |
|---|---|---|
| 重复提交 | 支付、核销、酒水领取、存酒、合同审批、财务入账重复执行 | 同一幂等键重复请求返回同一结果；不同幂等键但同一业务对象冲突时必须拒绝并留痕 |
| 并发库存 | 票务库存、酒水库存、权益次数、卡座容量被超卖或超扣 | 并发测试必须验证行锁/乐观锁、库存预扣与最终落账一致 |
| 终端越权 | 员工端、平板、一体机、客人端调用不属于自己的接口 | 服务端按可信终端（设备凭证、会话、账号类型派生）拦 HTML 限定了终端的接口——私人手机与平板只放行清单内接口，客人只经 App 或小程序，经销商门户只认门户终端，一体机售票只认启用售票的一体机，服务点单现场动作不许私人手机；同时校验设备能力、登录主体和数据范围；\`x-terminal-allowed\` 是契约声明，列出可调用该接口的终端，运行时不逐接口按它拦 |
| 数据范围穿透 | 商户员工查看其它商户、酒吧、卡座、订单、库存或财务数据 | 每个查询和写入接口必须按 project/merchant/venue/bar/space 等范围过滤 |
| 第三方不稳定 | Webhook 丢失、重复、乱序、签名失败、供应商超时 | Webhook + 定时轮询校验；所有回调按事件 ID 和业务对象做幂等补偿 |
| 人工补录 | 人工补录绕过原始凭证、库存、财务、审批或实名边界 | 补录必须审批、附件归档、原因码、前后状态和责任人留痕 |
| 退款/撤销 | 票务、权益、卡座、酒水、支付状态之间不一致 | 退款前校验核销、权益使用、酒水领取、定金、应收应付和结算状态 |
| 离线/弱网 | 工作手机或 Android 端离线导致核销、库存、支付误判 | P1 仅允许策略摘要离线查看；高价值酒水、支付、核销必须在线 |
| 开放平台鉴权 | nonce 重放、签名过期、开放应用被停用仍能访问 | 签名窗口、nonce 唯一、密钥轮换、锁定与人工解锁必须可测 |
| 附件归档 | 合同、审批、事故、整改附件缺失或 hash 不一致 | 附件上传、下载、病毒扫描、hash 校验、权限隔离和归档留痕必须可测 |

## 6. 用户故事拆分

- \`US-P0-API-001\` 作为后端开发，我要按 OpenAPI 每个 operation 生成 Controller/DTO/Service/Repository/审计事件和测试，确保接口可直接联调。
- \`US-P0-API-002\` 作为前端开发，我要按 OpenAPI 和追溯附录生成或维护 API client，并废弃旧 \`/api/admin/*\`、\`/api/mobile/*\`、\`/api/integration/*\` 草案路径。
- \`US-P0-API-003\` 作为 Android/一体机开发，我要按设备能力开关决定可调用接口，避免售票版和非售票版混用；服务端按可信终端（设备凭证、会话、账号类型派生）拦 HTML 限定了终端的接口——私人手机与平板只放行清单内接口，客人只经 App 或小程序，经销商门户只认门户终端，一体机售票只认启用售票的一体机，服务点单现场动作不许私人手机；\`x-terminal-allowed\` 是契约声明，列出可调用该接口的终端，运行时不逐接口按它拦。
- \`US-P0-API-004\` 作为测试负责人，我要把本文每个 P0 operation 转换为正向、权限、数据范围、幂等、异常、审计留痕和并发测试用例。
- \`US-P0-API-005\` 作为第三方供应商联调负责人，我要先对接 Adapter、Webhook 接收、mock 契约和轮询校验，再在供应商资料到位后补字段映射与异常补偿。

版本说明：本文档版本与 \`docs/openapi/openapi-p0.json\` 的 \`info.version\` 同步，契约变更即版本递增；不另维护变更记录表（历史见 OpenAPI 与 git 记录）。
`;

function normalize(value) {
  return value.replace(/\r\n/g, "\n").trimEnd();
}

if (checkOnly) {
  const current = fs.existsSync(matrixPath) ? fs.readFileSync(matrixPath, "utf8") : "";
  if (normalize(current) !== normalize(document)) {
    console.error(`${matrixPath} is out of sync with ${jsonPath}; run npm run docs:operation-matrix`);
    process.exit(1);
  }
  console.log(`${matrixPath} is in sync with ${jsonPath}; ${rows.length} operations checked.`);
} else {
  fs.writeFileSync(matrixPath, document, "utf8");
  console.log(`generated ${matrixPath} with ${rows.length} operations`);
}
