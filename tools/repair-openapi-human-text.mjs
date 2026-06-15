import fs from "node:fs";
import YAML from "yaml";

const jsonPath = "docs/openapi/openapi-p0.json";
const yamlPath = "docs/openapi/openapi-p0.yaml";

const openapi = JSON.parse(fs.readFileSync(jsonPath, "utf8"));
const badText = (value) => typeof value === "string" && /\?{2,}/.test(value);

const termMap = new Map([
  ["access", "权限"],
  ["users", "用户"],
  ["user", "用户"],
  ["permission", "权限"],
  ["permissions", "权限点"],
  ["templates", "模板"],
  ["template", "模板"],
  ["roles", "角色"],
  ["role", "角色"],
  ["temporary", "临时"],
  ["grants", "授权"],
  ["amusement", "游乐"],
  ["device", "设备"],
  ["devices", "设备"],
  ["statuses", "状态"],
  ["status", "状态"],
  ["manual", "人工"],
  ["overrides", "放行"],
  ["override", "放行"],
  ["projects", "项目"],
  ["project", "项目"],
  ["rules", "规则"],
  ["rule", "规则"],
  ["queue", "排队"],
  ["entries", "记录"],
  ["reservations", "预约"],
  ["reservation", "预约"],
  ["verifications", "核销记录"],
  ["verification", "核销"],
  ["analytics", "经营分析"],
  ["quality", "质检"],
  ["results", "结果"],
  ["tasks", "任务"],
  ["customer", "客户"],
  ["customers", "客户"],
  ["intents", "意向"],
  ["merchant", "商户"],
  ["merchants", "商户"],
  ["health", "健康度"],
  ["metric", "指标"],
  ["metrics", "指标"],
  ["anomalies", "异常"],
  ["definitions", "定义"],
  ["risk", "风险"],
  ["alerts", "预警"],
  ["robot", "机器人"],
  ["staff", "员工"],
  ["service", "服务"],
  ["score", "评分"],
  ["bar", "酒吧"],
  ["orders", "订单"],
  ["order", "订单"],
  ["bill", "账单"],
  ["bills", "账单"],
  ["items", "明细"],
  ["item", "物品"],
  ["products", "商品"],
  ["product", "商品"],
  ["production", "出品"],
  ["tickets", "票据"],
  ["ticket", "票"],
  ["evidence", "证据"],
  ["events", "事件"],
  ["event", "事件"],
  ["finance", "财务"],
  ["reconciliation", "对账"],
  ["batches", "批次"],
  ["batch", "批次"],
  ["integration", "第三方集成"],
  ["compensation", "补偿"],
  ["offline", "离线"],
  ["sync", "同步"],
  ["retail", "零售"],
  ["skus", "SKU"],
  ["sku", "SKU"],
  ["video", "视频"],
  ["cameras", "摄像头"],
  ["camera", "摄像头"],
  ["voucher", "券"],
  ["wecom", "企业微信"],
  ["bindings", "绑定"],
  ["binding", "绑定"],
  ["conversations", "会话"],
  ["messages", "消息"],
  ["hits", "命中"],
  ["inventory", "库存"],
  ["stock", "库存"],
  ["balances", "余额"],
  ["balance", "余额"],
  ["movements", "流水"],
  ["movement", "流水"],
  ["transfer", "调拨"],
  ["platform", "平台"],
  ["contracts", "合同"],
  ["contract", "合同"],
  ["spaces", "空间"],
  ["space", "空间"],
  ["terminals", "终端"],
  ["terminal", "终端"],
  ["return", "归还"],
  ["entitlements", "权益"],
  ["entitlement", "权益"],
  ["issued", "已发放"],
  ["types", "类型"],
  ["type", "类型"],
  ["publish", "发布"],
]);

const actionMap = [
  ["list", "查询"],
  ["get", "获取"],
  ["create", "创建"],
  ["update", "更新"],
  ["delete", "删除"],
  ["add", "新增"],
  ["approve", "审批"],
  ["resolve", "处理"],
  ["transfer", "调拨"],
  ["sync", "同步"],
  ["return", "归还"],
  ["bind", "绑定"],
  ["unbind", "解绑"],
  ["issue", "发放"],
  ["publish", "发布"],
  ["calculate", "计算"],
  ["deliver", "送达确认"],
  ["settle", "结清"],
  ["request", "申请"],
];

function wordsFromOperationId(operationId = "") {
  return operationId
    .replace(/([a-z0-9])([A-Z])/g, "$1-$2")
    .split(/[-_]/)
    .map((word) => word.toLowerCase())
    .filter(Boolean);
}

function wordsFromPath(path = "") {
  return path
    .split("/")
    .filter(Boolean)
    .filter((part) => !part.startsWith("{") && !["api", "v1", "open", "d"].includes(part))
    .flatMap((part) => part.split(/[-_]/))
    .map((word) => word.toLowerCase())
    .filter(Boolean);
}

function labelFromWords(words) {
  const labels = words
    .filter((word) => !actionMap.some(([action]) => action === word))
    .filter((word) => !["id", "by", "of", "to", "for"].includes(word))
    .map((word) => termMap.get(word) || word)
    .filter(Boolean);
  return [...new Set(labels)].join("") || "业务对象";
}

function summaryFor(operationId, path) {
  const words = wordsFromOperationId(operationId);
  const action = actionMap.find(([prefix]) => words[0] === prefix)?.[1] || "处理";
  const label = labelFromWords(words.slice(1).length ? words.slice(1) : wordsFromPath(path));
  return `${action}${label}`;
}

function schemaLabel(name) {
  return labelFromWords(
    name
      .replace(/Request$/, "")
      .replace(/([a-z0-9])([A-Z])/g, "$1-$2")
      .split(/[-_]/)
      .map((word) => word.toLowerCase())
  );
}

let repairedOperations = 0;
let repairedSchemas = 0;

for (const [path, item] of Object.entries(openapi.paths || {})) {
  for (const method of ["get", "post", "put", "patch", "delete"]) {
    const operation = item[method];
    if (!operation) continue;
    const shouldRepair = badText(operation.summary) || badText(operation.description) || !operation.summary;
    if (shouldRepair) {
      const summary = summaryFor(operation.operationId, path);
      operation.summary = summary;
      operation.description = `用于${summary}。必须按 OpenAPI schema、权限、数据范围、终端、幂等和审计事件实现；P1/P2/P3 深度能力不得反向扩大 P0 范围。`;
      repairedOperations += 1;
    }
    for (const parameter of operation.parameters || []) {
      if (badText(parameter.description) || !parameter.description) {
        parameter.description = parameter.name === "movement_type"
          ? "库存流水类型路径参数，允许 inbound、outbound、damage。"
          : `${parameter.name} 参数；必须按 OpenAPI 类型、枚举和必填规则校验。`;
      }
    }
  }
}

for (const [name, schema] of Object.entries(openapi.components?.schemas || {})) {
  const label = schemaLabel(name);
  let changed = false;
  if (badText(schema.description) || !schema.description) {
    schema.description = `${label}请求体。字段必须与 OpenAPI、后端 DTO、前端请求、Android 请求和供应商 mock 保持一致。`;
    changed = true;
  }
  for (const [propName, prop] of Object.entries(schema.properties || {})) {
    if (badText(prop.description) || !prop.description) {
      prop.description = `${label}.${propName} 字段；业务含义以字段名和所属接口为准，测试必须覆盖缺失、错类型和越界场景。`;
      changed = true;
    }
  }
  if (changed) repairedSchemas += 1;
}

fs.writeFileSync(jsonPath, `${JSON.stringify(openapi, null, 2)}\n`, "utf8");
fs.writeFileSync(yamlPath, YAML.stringify(openapi), "utf8");
console.log(`repaired operations: ${repairedOperations}`);
console.log(`repaired schemas: ${repairedSchemas}`);
