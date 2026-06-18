import fs from "node:fs";
import path from "node:path";
import { capabilitySwitchDocument, switchSourcePath } from "./integration-capability-switches.mjs";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");
const mdPath = "docs/integration-capability-switches-v0.1.md";

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function normalize(text) {
  return text.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function cell(value) {
  if (Array.isArray(value)) return value.join(", ").replace(/\|/g, "/");
  return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>");
}

function mdTable(rows, columns) {
  const header = `| ${columns.map((column) => column.label).join(" | ")} |`;
  const divider = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => cell(row[column.key])).join(" | ")} |`);
  return [header, divider, ...body].join("\n") + "\n";
}

function writeOrCheck(relativePath, content) {
  const fullPath = absolute(relativePath);
  const expected = normalize(content);
  if (checkMode) {
    if (!fs.existsSync(fullPath)) throw new Error(`${relativePath} is missing`);
    const current = normalize(fs.readFileSync(fullPath, "utf8").replace(/^\uFEFF/, ""));
    if (current !== expected) throw new Error(`${relativePath} is not in sync with integration capability switches`);
    return;
  }
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, expected, "utf8");
  console.log(`wrote ${relativePath}`);
}

const doc = capabilitySwitchDocument();
const json = JSON.stringify(doc, null, 2);
const rows = doc.switches.map((item) => ({
  capability_id: item.capability_id,
  label: item.label,
  enabled: item.enabled ? "on" : "off",
  production_interface_required: item.production_interface_required ? "yes" : "no",
  item_ids: [...new Set([...(item.evidence_item_ids ?? []), ...(item.credential_item_ids ?? [])])],
  finding_ids: item.security_finding_ids ?? [],
  disabled_behavior: item.disabled_behavior,
  enabled_requirements: item.enabled_requirements,
}));
const md = `# 集成能力开关矩阵 V0.1

## 1. 机器标记

- INTEGRATION_CAPABILITY_SWITCHES_READY
- DEFAULT_EXTERNAL_INTEGRATIONS_OFF
- DISABLED_CAPABILITY_DOES_NOT_REQUIRE_INTERFACE
- ENABLED_CAPABILITY_REQUIRES_REAL_INTERFACE_EVIDENCE
- LOCAL_AI_LLM_ON_160_REPLACES_EXTERNAL_AI_FOR_NOW
- NAS_BACKUP_REMAINS_REQUIRED_UNTIL_EQUIVALENT_RECOVERY_TARGET_EXISTS

## 2. 结论

本矩阵把第三方接口、设备、对象存储、开放平台、企业微信、工作手机纳管和 AI 能力统一改成“开关化”生产准入口径。默认除 160 本地 AI 与 NAS 备份恢复外，外部集成全部关闭。关闭表示不读取外部接口、不要求供应商密钥和生产 evidence；但对应业务能力也必须降级、隐藏或走人工替代流程，不能一边关闭接口一边宣称自动能力已经上线。

## 3. 开关矩阵

${mdTable(rows, [
  { key: "capability_id", label: "capability_id" },
  { key: "label", label: "能力" },
  { key: "enabled", label: "默认" },
  { key: "production_interface_required", label: "需真实接口" },
  { key: "item_ids", label: "关联证据项" },
  { key: "finding_ids", label: "关联安全项" },
  { key: "disabled_behavior", label: "关闭后的业务口径" },
  { key: "enabled_requirements", label: "开启后的要求" },
])}
## 4. 后台手动工作人员录入需求

新增后台用户录入渠道：平台管理员、IT 管理员或 HR 可在后台手动创建工作人员账号，录入员工账号、手机号、组织、商户、岗位/角色、权限模板、临时权限时效或单次有效设置，并写入审计事件。企业微信关闭时，后台手动录入是默认员工入口；企业微信开启后，企业微信同步结果只能作为辅助来源，不能绕过后台权限审批和留痕。

## 5. 验收标准

1. Given 某能力开关为关闭，When 运行生产阻断和外部证据追踪，Then 该能力对应的供应商接口、密钥、Webhook、轮询和生产 evidence 不再构成上线阻断。
2. Given 某能力开关为关闭，When 用户访问对应后台入口，Then 页面必须显示关闭/未启用状态，写接口不得伪调用外部供应商。
3. Given 某能力开关为开启，When 进入生产准入检查，Then 必须要求真实 SDK/API、credential_ref、Webhook/轮询、对账/补偿、失败样例和生产证据。
4. Given 企业微信关闭，When 创建工作人员，Then 后台手动录入、角色或权限模板分配、审计留痕必须可完成。
5. Given 工作手机/平板/一体机 MDM 关闭，When 执行高风险核销、出酒或支付动作，Then 移动终端不得放行，必须转后台或人工流程。
6. Given 酒吧人脸/年龄核验关闭，When 酒吧入场，Then 系统只能记录人工证件核验结果，不能展示自动人脸核验已通过。

## 6. 餐饮 POS 和零售 POS 解释

餐饮 POS/零售 POS 不是“只刷卡”。刷卡或扫码收款属于支付通道；餐饮 POS/零售 POS 是商户自己的点单、收银、退款、券核销、SKU/商品、订单流水和对账系统。本项目默认不接餐饮/零售 POS。只有对应开关打开后，才需要供应商 API/SDK、字段映射、Webhook、轮询和对账文件。
`;

writeOrCheck(switchSourcePath, json);
writeOrCheck(mdPath, md);
if (checkMode) console.log("INTEGRATION_CAPABILITY_SWITCHES_PASS");
