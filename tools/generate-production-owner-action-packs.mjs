import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const sourcePath = "docs/production-readiness-evidence/production-blocker-register-latest.json";
const submissionKitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-owner-action-packs-latest.json";
const mdPath = "docs/production-owner-action-packs-v0.1.md";

function readText(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function readOptionalJson(relativePath) {
  const fullPath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(fullPath)) return null;
  return JSON.parse(fs.readFileSync(fullPath, "utf8"));
}

function normalize(content) {
  return content.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

function escapeCell(value) {
  return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>");
}

function containsAny(text, markers) {
  return markers.some((marker) => String(text).includes(marker));
}

function inferPriority(item) {
  const severity = String(item.severity ?? "").trim();
  if (severity) return severity;
  const id = String(item.id ?? "");
  if (id.includes("MDM") || id.includes("PAY") || id.includes("FACE") || id.includes("CRED") || id.includes("NAS") || id.includes("160")) return "P0";
  if (id.includes("WEBHOOK")) return "P1";
  if (id.includes("CSRF")) return "P2";
  return "P1";
}

function ownerGroupsFor(item) {
  const owner = `${item.owner_department ?? ""}/${item.owner ?? ""}`;
  const groups = [];
  if (containsAny(owner, ["IT", "运维", "安全", "技术负责人"])) groups.push("IT/运维/安全");
  if (containsAny(owner, ["财务"])) groups.push("财务");
  if (containsAny(owner, ["采购", "供应商", "供应商联调"])) groups.push("采购/供应商联调");
  if (containsAny(owner, ["运营", "法务", "HR"])) groups.push("运营/法务");
  if (containsAny(owner, ["后端", "前端", "集成开发", "技术负责人"])) groups.push("技术实现负责人");
  if (groups.length === 0) groups.push("总控/项目管理");
  return unique(groups);
}

function categoryOf(item) {
  const id = String(item.id ?? "");
  const category = String(item.category ?? "");
  if (category) return category;
  if (id.includes("MDM")) return "MDM";
  if (id.includes("OPS") || id.includes("160")) return "ops160";
  if (id.includes("WEBHOOK")) return "third_party";
  if (id.includes("CSRF")) return "security";
  return "general";
}

function evidenceFor(item) {
  const id = String(item.id ?? "");
  const category = categoryOf(item);
  if (category === "MDM") return ["MDM/Android Enterprise 选型或 Android Management API 项目资料", "策略模板截图或导出", "工作手机、平板、售票型一体机、非售票型一体机 enroll 证据", "远程锁定、擦除、丢失模式演练记录", "平台设备绑定记录和责任人"];
  if (category === "payment") return ["聚合支付测试或生产商户资料", "回调地址、验签资料和对账文件样例", "退款权限、退款审批和日结口径", "财务责任人确认", "密钥仅填写 credential_ref，不写明文"];
  if (category === "face_age") return ["年龄/人脸供应商 SDK 或 API 文档", "测试账号、错误码、成功和失败样例", "合规材料与采购确认", "生产 credential_ref", "运营验收记录"];
  if (category === "nas") return ["重启后自动挂载证据", "读写删除测试证据", "定时备份归档和 sha256", "从定时归档恢复演练报告", "容量告警和失败告警送达记录"];
  if (category === "credential") return ["credential_ref、owner_person、owner_department", "到期日、提醒策略和轮换窗口", "轮换步骤、回滚步骤和验证步骤", "最近一次轮换或首次发放证据", "不得出现明文密钥、token 或密码"];
  if (category === "wecom") return ["企业微信应用和通讯录权限配置", "回调配置、同步任务配置和失败告警", "一次增量同步验收记录", "会话存档或适用范围确认", "生产 credential_ref"];
  if (category === "third_party") return ["供应商 SDK/API、签名算法和回调样例", "字段映射、错误码、补偿和对账口径", "沙箱账号和生产 credential_ref", "Webhook nonce/timestamp/幂等验证", "轮询校验策略和失败重试策略"];
  if (category === "ops160") return ["/etc/complex/complex.env 或受控配置等效证据", "systemd 服务、日志路径和健康检查", "备份、恢复、告警和容量监控证据", "运维 Runbook 和回滚步骤", "不得由 Codex 写入系统级生产 secret"];
  if (category === "security") return ["认证方案分支说明", "CSRF 或 Bearer Token 安全复核记录", "安全负责人签收", "生产前复测命令输出"];
  if (id.includes("WEBHOOK")) return ["供应商签名验签样例", "timestamp、nonce、重放攻击测试", "幂等键和异常补偿测试", "对账或轮询兜底验证"];
  return ["来源台账更新", "证据链接", "责任人", "复测命令输出"];
}

function actionsFor(item) {
  const category = categoryOf(item);
  if (category === "MDM") return ["完成选型/采购/控制台配置", "纳管最小设备矩阵", "回填 enroll、策略和丢失处置证据", "复跑生产阻断清单和交付闸口"];
  if (category === "payment") return ["开通聚合支付资料", "配置回调和验签", "完成退款与对账样例", "财务确认日结和异常处理口径"];
  if (category === "credential") return ["建立人工台账", "设置到期提醒", "执行首次发放或轮换演练", "记录 credential_ref 和回滚步骤"];
  if (category === "nas") return ["固化系统级挂载", "配置定时备份和校验", "配置容量/失败告警", "完成周期性恢复演练"];
  if (category === "third_party") return ["收集 SDK/API 和沙箱", "确认签名、回调和字段映射", "完成 mock 到真实适配器切换计划", "补充异常补偿和对账验证"];
  if (category === "face_age") return ["完成供应商资料收集", "确认年龄核验合规材料", "联调成功/失败样例", "运营验收后再关闭阻断"];
  if (category === "wecom") return ["配置企业微信应用和权限", "完成通讯录/回调同步", "演练失败告警", "运营验收同步结果"];
  if (category === "ops160") return ["由 IT 写系统级配置", "接入 systemd 和日志", "接入健康检查和告警", "保留用户级覆盖仅作为开发兜底"];
  return ["补齐来源证据", "更新来源台账或 findings", "生成阻断清单和动作包", "复跑交付闸口"];
}

function itemSummary(item, ownerGroup, submissionRowsByItem) {
  const relatedSubmissionRows = relatedSubmissionsFor(item, ownerGroup, submissionRowsByItem);
  return {
    id: item.id,
    priority: inferPriority(item),
    category: categoryOf(item),
    status: item.status,
    blocking_scope: item.blocking_scope,
    source: item.source,
    source_table: item.source_table ?? "findings_register",
    next_action: item.next_action,
    required_evidence: evidenceFor(item),
    owner_actions: actionsFor(item),
    related_submissions: relatedSubmissionRows,
    manifest_paths: unique(relatedSubmissionRows.map((row) => row.manifest_path)),
    accepted_evidence_types: unique(relatedSubmissionRows.flatMap((row) => row.accepted_evidence_types)),
    verification_commands: unique(relatedSubmissionRows.flatMap((row) => row.verification_commands_after_backfill)),
  };
}

function packSortKey(pack) {
  const order = ["IT/运维/安全", "财务", "采购/供应商联调", "运营/法务", "技术实现负责人", "总控/项目管理"];
  return order.indexOf(pack.owner_group) < 0 ? 999 : order.indexOf(pack.owner_group);
}

function mdTable(rows, columns) {
  if (rows.length === 0) return "无。\n";
  const header = `| ${columns.map((column) => column.label).join(" | ")} |`;
  const separator = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => escapeCell(row[column.key])).join(" | ")} |`);
  return [header, separator, ...body].join("\n") + "\n";
}

function buildSubmissionRowsByItem(submissionKit) {
  const map = new Map();
  for (const row of submissionKit?.submissions ?? []) {
    const itemId = row.item_id;
    if (!itemId) continue;
    const manifestPath = `${row.packet_folder ?? ""}${row.manifest_file_name ?? ""}`;
    const normalized = {
      submission_id: row.submission_id,
      owner_group: row.owner_group,
      item_id: itemId,
      priority: row.priority,
      category: row.category,
      submission_status: row.submission_status,
      manifest_path: manifestPath,
      accepted_evidence_types: row.accepted_evidence_types ?? [],
      required_evidence_summary: row.required_evidence_summary ?? [],
      not_accepted_evidence: row.not_accepted_evidence ?? [],
      verification_commands_after_backfill: row.verification_commands_after_backfill ?? [],
    };
    if (!map.has(itemId)) map.set(itemId, []);
    map.get(itemId).push(normalized);
  }
  return map;
}

function relatedSubmissionsFor(item, ownerGroup, submissionRowsByItem) {
  const rows = submissionRowsByItem.get(item.id) ?? [];
  const exactOwnerRows = rows.filter((row) => row.owner_group === ownerGroup);
  return exactOwnerRows.length > 0 ? exactOwnerRows : rows;
}

const source = readJson(sourcePath);
const submissionKit = readOptionalJson(submissionKitPath);
const submissionRowsByItem = buildSubmissionRowsByItem(submissionKit);
const unresolvedItems = [
  ...(source.readiness_blockers ?? []),
  ...(source.credential_blockers ?? []),
  ...(source.finding_blockers ?? []),
];

const packMap = new Map();
for (const item of unresolvedItems) {
  for (const ownerGroup of ownerGroupsFor(item)) {
    if (!packMap.has(ownerGroup)) {
      packMap.set(ownerGroup, {
        owner_group: ownerGroup,
        status: "open_actions_present",
        blocker_count: 0,
        priority_summary: [],
        items: [],
        commands_after_evidence_update: [
          "npm run docs:production-blockers",
          "npm run docs:production-owner-actions",
          "npm run verify:production-owner-actions",
          "npm run verify:production-readiness-evidence",
          "npm run verify:delivery-gates",
        ],
        not_accepted_evidence: [
          "口头确认",
          "mock 截图",
          "未纳管设备截图",
          "无 credential_ref 的明文密钥记录",
          "无法追溯来源台账或 findings 的手工删项",
        ],
      });
    }
    const pack = packMap.get(ownerGroup);
    pack.items.push(itemSummary(item, ownerGroup, submissionRowsByItem));
  }
}

const packs = [...packMap.values()].sort((a, b) => packSortKey(a) - packSortKey(b));
for (const pack of packs) {
  pack.blocker_count = pack.items.length;
  pack.priority_summary = unique(pack.items.map((item) => item.priority)).sort();
}

const output = {
  register_version: "v0.1",
  status: packs.length > 0 ? "open_actions_present" : "no_open_actions",
  source_register_path: sourcePath,
  source_submission_kit_path: submissionKitPath,
  generated_from: [sourcePath, submissionKitPath],
  policy: {
    no_fake_closure: true,
    source_of_truth_is_blocker_register: true,
    credential_ref_only_no_plaintext_secret: true,
    mock_is_not_production_evidence: true,
  },
  counts: {
    unique_unresolved_items: unresolvedItems.length,
    owner_action_packs: packs.length,
    duplicated_owner_action_rows: packs.reduce((sum, pack) => sum + pack.items.length, 0),
    related_submission_rows: packs.reduce((sum, pack) => sum + pack.items.reduce((itemSum, item) => itemSum + item.related_submissions.length, 0), 0),
    items_without_manifest: packs.reduce((sum, pack) => sum + pack.items.filter((item) => item.manifest_paths.length === 0).length, 0),
  },
  owner_action_packs: packs,
};

const summaryRows = packs.map((pack) => ({
  owner_group: pack.owner_group,
  status: pack.status,
  blocker_count: pack.blocker_count,
  priorities: pack.priority_summary.join(", "),
  first_actions: unique(pack.items.flatMap((item) => item.owner_actions)).slice(0, 5).join("；"),
}));

const detailSections = packs.map((pack) => {
  const rows = pack.items.map((item) => ({
    id: item.id,
    priority: item.priority,
    category: item.category,
    status: item.status,
    blocking_scope: item.blocking_scope,
    next_action: item.next_action,
    required_evidence: item.required_evidence.join("；"),
    manifest_paths: item.manifest_paths.join("\uFF1B") || "missing: check production-evidence-submission-kit",
    accepted_evidence_types: item.accepted_evidence_types.join("\uFF1B") || "missing",
    verification_commands: item.verification_commands.join("\uFF1B") || "npm run verify:delivery-gates",
  }));
  return `### ${pack.owner_group}\n\n${mdTable(rows, [
    { key: "id", label: "id" },
    { key: "priority", label: "priority" },
    { key: "category", label: "category" },
    { key: "status", label: "status" },
    { key: "blocking_scope", label: "blocking_scope" },
    { key: "next_action", label: "next_action" },
    { key: "required_evidence", label: "required_evidence" },
    { key: "manifest_paths", label: "manifest_paths" },
    { key: "accepted_evidence_types", label: "accepted_evidence_types" },
    { key: "verification_commands", label: "verification_commands" },
  ])}`;
}).join("\n");

const md = normalize(`# 生产责任人动作包 V0.1\n\n## 1. 机器标记\n\n- PRODUCTION_OWNER_ACTION_PACKS_READY\n- OWNER_ACTIONS_FROM_PRODUCTION_BLOCKERS\n- OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS\n- NO_OWNER_ACTION_CAN_CLOSE_WITHOUT_SOURCE_LEDGER\n- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET\n- VERIFY_PRODUCTION_OWNER_ACTIONS_READY\n\n## 2. 定位\n\n本文从 \`${sourcePath}\` 自动生成，把仍未关闭的生产阻断项按责任人拆成可执行动作包。本文不改变来源台账状态，不允许用本文手工删项来关闭阻断。\n\nEvidence manifest source: \`${submissionKitPath}\`. Every owner action item must link to at least one manifest path; verification fails when items_without_manifest is not 0.\n\n## 3. 总览\n\n| 指标 | 数量 |\n|---|---:|\n| 未关闭唯一阻断项 | ${output.counts.unique_unresolved_items} |\n| 责任人动作包 | ${output.counts.owner_action_packs} |\n| 按责任人展开后的动作行 | ${output.counts.duplicated_owner_action_rows} |\n| \u5df2\u5173\u8054\u8bc1\u636e\u63d0\u4ea4\u884c | ${output.counts.related_submission_rows} |\n| \u672a\u5339\u914d manifest \u7684\u52a8\u4f5c\u884c | ${output.counts.items_without_manifest} |\n\n## 4. 责任人动作包总表\n\n${mdTable(summaryRows, [
  { key: "owner_group", label: "owner_group" },
  { key: "status", label: "status" },
  { key: "blocker_count", label: "blocker_count" },
  { key: "priorities", label: "priorities" },
  { key: "first_actions", label: "first_actions" },
])}\n## 5. 分责任人明细\n\n${detailSections}\n## 6. 回填后必跑命令\n\n1. \`npm run docs:production-blockers\`\n2. \`npm run docs:production-owner-actions\`\n3. \`npm run verify:production-owner-actions\`\n4. \`npm run verify:production-readiness-evidence\`\n5. \`npm run verify:delivery-gates\`\n\n## 7. 禁止事项\n\n1. 不得把口头确认、mock 截图、未纳管设备截图当作生产证据。\n2. 不得把真实密码、token、API secret、私钥正文写入文档、仓库、聊天或日志。\n3. 不得绕过来源台账或 findings register 直接手工删除本文阻断项。\n4. 不得把供应商沙箱或模拟适配器当作生产接入完成证据。\n`);

const json = normalize(JSON.stringify(output, null, 2));

function forbidPlaintextSecrets(relativePath, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
    /\b(?:secret_key|secret)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) throw new Error(`${relativePath} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

forbidPlaintextSecrets(mdPath, md);
forbidPlaintextSecrets(jsonPath, json);

if (checkMode) {
  const failures = [];
  const missingOutputs = [jsonPath, mdPath].filter((relativePath) => !fs.existsSync(path.join(repoRoot, relativePath)));
  if (missingOutputs.length > 0) {
    console.error("PRODUCTION_OWNER_ACTIONS_FAIL");
    for (const relativePath of missingOutputs) console.error(`- ${relativePath} is missing; run npm run docs:production-owner-actions`);
    process.exit(1);
  }
  const currentJson = normalize(readText(jsonPath));
  const currentMd = normalize(readText(mdPath));
  if (currentJson !== json) failures.push(`${jsonPath} is not in sync`);
  if (currentMd !== md) failures.push(`${mdPath} is not in sync`);
  if (output.status !== "open_actions_present") failures.push("owner action packs must keep open actions visible while blockers exist");
  if (output.counts.items_without_manifest !== 0) failures.push("every owner action item must link to at least one evidence manifest");
  if (output.counts.related_submission_rows <= 0) failures.push("owner action packs must expose related evidence submission rows");
  if (failures.length > 0) {
    console.error("PRODUCTION_OWNER_ACTIONS_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log("PRODUCTION_OWNER_ACTIONS_PASS");
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
}
