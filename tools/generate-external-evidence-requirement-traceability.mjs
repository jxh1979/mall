import fs from "node:fs";
import path from "node:path";
import { defaultSwitchState, disabledItemIds, disabledFindingIds } from "./integration-capability-switches.mjs";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check") || process.argv.includes("--verify");

const matrixPath = "docs/requirements-code-implementation-matrix-v0.1.csv";
const ledgerPath = "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json";
const statusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const jsonPath = "docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json";
const csvPath = "docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.csv";
const mdPath = "docs/external-evidence-requirement-traceability-v0.1.md";

const evidenceMappings = {
  "REQ-17": {
    evidence_item_ids: ["PRD-PAY-001", "PRD-PAY-002", "PRD-FACE-001", "PRD-CRED-3P-001", "PRD-CRED-OBJ-001"],
    reason: "酒吧桌台/卡座/吧台位/最低消费/点酒/支付/赠饮/账单/异常/存酒虽然已有代码闭环，但真实收款、年龄核验、供应商凭证和附件/证据存储仍需生产证据。",
  },
  "REQ-21": {
    evidence_item_ids: ["PRD-PAY-001", "PRD-PAY-002"],
    reason: "吧台买酒线上支付、现金和 POS 口径已实现为业务能力，真实收款、退款、回调验签和日结对账必须以聚合支付/财务生产证据关闭。",
  },
  "REQ-23b": {
    evidence_item_ids: ["PRD-FACE-001", "PRD-CRED-3P-001"],
    reason: "酒吧年龄/人脸或等效设备核验依赖供应商 SDK/API、合规材料、测试账号和生产 credential_ref，mock 不可作为生产通过证据。",
  },
  "REQ-25": {
    evidence_item_ids: ["PRD-PAY-001", "PRD-PAY-002"],
    reason: "订台定金闭环依赖真实聚合支付商户、回调验签、退款权限和财务日结对账证据。",
  },
  "REQ-32": {
    evidence_item_ids: ["PRD-CRED-OBJ-001", "PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"],
    reason: "高价值报损、盘点差异、存酒作废需要照片/视频留证和主管复核；生产闭环需对象存储凭证、视频/设备供应商资料、Webhook/轮询防重放与补偿证据。",
  },
  "REQ-40": {
    evidence_item_ids: ["PRD-3P-001", "PRD-CRED-OPEN-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"],
    reason: "第三方自动留痕必须绑定真实供应商 SDK/API、开放平台凭证、供应商凭证、签名回调、轮询校验和异常补偿证据。",
  },
  "REQ-41": {
    evidence_item_ids: ["PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"],
    reason: "留痕与监控视频一一对应依赖真实视频平台或设备厂商 API、凭证、回调/轮询样例和失败补偿证据；P0 可不强依赖真实视频，但生产联调不能伪关闭。",
  },
  "REQ-44": {
    evidence_item_ids: ["PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001", "PRD-MDM-001", "SEC-P0-MDM-DEVICE-001"],
    reason: "机器人或人工配送交接扫码留痕需真实机器人/设备/开放平台资料，同时生产工作手机、平板、一体机执行高风险交接动作前必须有 MDM 纳管证据。",
  },
};

const switchState = defaultSwitchState();
const disabledEvidenceItemIds = disabledItemIds(switchState);
const disabledSecurityFindingIds = disabledFindingIds(switchState);

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function readText(relativePath) {
  return fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, "");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function normalize(content) {
  return content.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function parseCsv(text) {
  const rows = [];
  let row = [];
  let value = "";
  let quoted = false;
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index];
    const next = text[index + 1];
    if (quoted) {
      if (char === '"' && next === '"') {
        value += '"';
        index += 1;
      } else if (char === '"') {
        quoted = false;
      } else {
        value += char;
      }
      continue;
    }
    if (char === '"') {
      quoted = true;
      continue;
    }
    if (char === ",") {
      row.push(value);
      value = "";
      continue;
    }
    if (char === "\n") {
      row.push(value.replace(/\r$/, ""));
      rows.push(row);
      row = [];
      value = "";
      continue;
    }
    value += char;
  }
  if (value.length > 0 || row.length > 0) {
    row.push(value);
    rows.push(row);
  }
  if (rows.length === 0) return [];
  const [headers, ...body] = rows;
  return body
    .filter((cells) => cells.some((cell) => String(cell).trim() !== ""))
    .map((cells) => Object.fromEntries(headers.map((header, index) => [header.replace(/^\uFEFF/, ""), cells[index] ?? ""])));
}

function csvEscape(value) {
  const text = Array.isArray(value) ? value.join("; ") : String(value ?? "");
  const escaped = text.replace(/"/g, '""');
  return /[",\r\n]/.test(escaped) ? `"${escaped}"` : escaped;
}

function csvTable(rows, columns) {
  const lines = [columns.map((column) => csvEscape(column.label)).join(",")];
  for (const row of rows) lines.push(columns.map((column) => csvEscape(row[column.key])).join(","));
  return "\uFEFF" + lines.join("\n") + "\n";
}

function escapeCell(value) {
  return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>");
}

function mdTable(rows, columns) {
  if (rows.length === 0) return "无。\n";
  const header = `| ${columns.map((column) => column.label).join(" | ")} |`;
  const separator = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => escapeCell(row[column.key])).join(" | ")} |`);
  return [header, separator, ...body].join("\n") + "\n";
}

function forbidPlaintextSecrets(relativePath, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*(?:["'][^"'<\n]{8,}["']|[A-Za-z0-9_./+=-]{12,})/i,
    /\b(?:secret_key|secret)\s*[:=]\s*(?:["'][^"'<\n]{8,}["']|[A-Za-z0-9_./+=-]{12,})/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) throw new Error(`${relativePath} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

const requirementRows = parseCsv(readText(matrixPath)).filter((row) => !String(row["需求编号"] ?? "").trim().startsWith("#"));
const ledger = readJson(ledgerPath);
const status = readJson(statusPath);
const ledgerRows = Array.isArray(ledger.rows) ? ledger.rows : [];
const partialRequirements = requirementRows.filter((row) => row["当前实现状态"] !== "已完整实现");
const ledgerByItemId = new Map();
for (const row of ledgerRows) {
  if (!ledgerByItemId.has(row.item_id)) ledgerByItemId.set(row.item_id, []);
  ledgerByItemId.get(row.item_id).push(row);
}

const traceRows = partialRequirements.map((requirement) => {
  const requirementId = requirement["需求编号"];
  const mapping = evidenceMappings[requirementId];
  const rawItemIds = mapping?.evidence_item_ids ?? [];
  const effectiveItemIds = rawItemIds.filter((itemId) => !disabledEvidenceItemIds.has(itemId) && !disabledSecurityFindingIds.has(itemId));
  const switchDisabledItemIds = rawItemIds.filter((itemId) => !effectiveItemIds.includes(itemId));
  const evidenceRows = effectiveItemIds.flatMap((itemId) => ledgerByItemId.get(itemId) ?? []);
  const missingItemIds = effectiveItemIds.filter((itemId) => !ledgerByItemId.has(itemId));
  const awaiting = evidenceRows.filter((row) => row.current_status === "awaiting_submission").length;
  const invalid = evidenceRows.filter((row) => row.current_status === "invalid_submission").length;
  const valid = evidenceRows.filter((row) => row.current_status === "valid_submission").length;
  const p0Rows = evidenceRows.filter((row) => row.priority === "P0");
  const ownerGroups = [...new Set(evidenceRows.map((row) => row.owner_group))].sort((a, b) => a.localeCompare(b, "zh-Hans-CN"));
  const productionBlocking = effectiveItemIds.length > 0 && p0Rows.some((row) => row.current_status !== "valid_submission");
  return {
    requirement_id: requirementId,
    module: requirement["业务模块"],
    acceptance: requirement["验收标准"],
    current_status: requirement["当前实现状态"],
    matrix_gap: requirement["缺口"],
    evidence_mapping_reason: mapping?.reason ?? "",
    mapped_item_ids: effectiveItemIds,
    switch_disabled_item_ids: switchDisabledItemIds,
    owner_groups: ownerGroups,
    evidence_rows_total: evidenceRows.length,
    p0_evidence_rows: p0Rows.length,
    awaiting_evidence_rows: awaiting,
    invalid_evidence_rows: invalid,
    valid_evidence_rows: valid,
    missing_item_ids: missingItemIds,
    production_blocking: productionBlocking,
    production_trace_status: effectiveItemIds.length === 0
      ? "covered_by_disabled_integration_switches"
      : missingItemIds.length > 0
        ? "traceability_missing_manifest"
        : invalid > 0
          ? "invalid_evidence_manifest_blocks"
          : awaiting > 0
            ? "awaiting_real_external_evidence"
            : "all_mapped_evidence_valid",
    target_manifest_paths: [...new Set(evidenceRows.map((row) => row.target_manifest_path).filter(Boolean))],
    verification_commands: [...new Set(evidenceRows.flatMap((row) => row.verification_commands ?? []))],
    evidence_rows: evidenceRows.map((row) => ({
      ledger_id: row.ledger_id,
      batch_id: row.batch_id,
      owner_group: row.owner_group,
      priority: row.priority,
      category: row.category,
      item_id: row.item_id,
      submission_id: row.submission_id,
      current_status: row.current_status,
      blocking_scope: row.blocking_scope,
      template_path: row.template_path,
      target_manifest_path: row.target_manifest_path,
      required_evidence_summary: row.required_evidence_summary ?? [],
      accepted_evidence_types: row.accepted_evidence_types ?? [],
      operator_next_step: row.operator_next_step,
    })),
  };
});

const failures = [];
for (const requirement of partialRequirements) {
  if (!evidenceMappings[requirement["需求编号"]]) failures.push(`${requirement["需求编号"]} is partial but has no external evidence mapping`);
}
for (const [requirementId, mapping] of Object.entries(evidenceMappings)) {
  if (!requirementRows.some((row) => row["需求编号"] === requirementId)) failures.push(`${requirementId} mapping does not exist in ${matrixPath}`);
  for (const itemId of mapping.evidence_item_ids.filter((id) => !disabledEvidenceItemIds.has(id) && !disabledSecurityFindingIds.has(id))) {
    if (!ledgerByItemId.has(itemId)) failures.push(`${requirementId} maps to ${itemId}, but ${itemId} is missing from ${ledgerPath}`);
  }
}
for (const row of traceRows) {
  if (row.mapped_item_ids.length === 0 && row.switch_disabled_item_ids.length > 0) continue;
  if (row.evidence_rows_total === 0) failures.push(`${row.requirement_id} has no evidence ledger rows`);
  if (row.missing_item_ids.length > 0) failures.push(`${row.requirement_id} has missing item ids: ${row.missing_item_ids.join(", ")}`);
  if (row.target_manifest_paths.length === 0) failures.push(`${row.requirement_id} has no target manifest paths`);
}

const counts = {
  partial_requirements: partialRequirements.length,
  traceable_partial_requirements: traceRows.filter((row) => (row.evidence_rows_total > 0 && row.missing_item_ids.length === 0) || (row.mapped_item_ids.length === 0 && row.switch_disabled_item_ids.length > 0)).length,
  mapped_evidence_items: new Set(traceRows.flatMap((row) => row.mapped_item_ids)).size,
  mapped_ledger_rows: traceRows.reduce((sum, row) => sum + row.evidence_rows_total, 0),
  p0_evidence_rows: traceRows.reduce((sum, row) => sum + row.p0_evidence_rows, 0),
  awaiting_evidence_rows: traceRows.reduce((sum, row) => sum + row.awaiting_evidence_rows, 0),
  invalid_evidence_rows: traceRows.reduce((sum, row) => sum + row.invalid_evidence_rows, 0),
  valid_evidence_rows: traceRows.reduce((sum, row) => sum + row.valid_evidence_rows, 0),
  production_blocking_requirements: traceRows.filter((row) => row.production_blocking).length,
};

const output = {
  traceability_version: "v0.1",
  status: failures.length > 0
    ? "traceability_gap"
    : counts.production_blocking_requirements > 0
      ? "traceable_but_awaiting_real_external_evidence"
      : "all_mapped_external_evidence_valid",
  generated_from: [matrixPath, ledgerPath, statusPath],
  policy: {
    no_fake_external_evidence_closure: true,
    mock_or_sandbox_is_not_production_evidence: true,
    partial_requirement_must_map_to_owner_manifest_or_disabled_switch: true,
    integration_capability_switches_applied: true,
    raw_evidence_stays_external: true,
    credential_ref_only_no_plaintext_secret: true,
    this_report_does_not_close_blockers: true,
  },
  submission_status_counts: status.counts ?? {},
  counts,
  requirement_traceability: traceRows,
};

const summaryRows = traceRows.map((row) => ({
  requirement_id: row.requirement_id,
  module: row.module,
  current_status: row.current_status,
  production_trace_status: row.production_trace_status,
  mapped_item_ids: row.mapped_item_ids.join(", "),
  owner_groups: row.owner_groups.join(", "),
  p0_evidence_rows: row.p0_evidence_rows,
  awaiting_evidence_rows: row.awaiting_evidence_rows,
  target_manifest_paths: row.target_manifest_paths.join("<br>"),
}));

const detailRows = traceRows.flatMap((requirement) => requirement.evidence_rows.map((evidence) => ({
  requirement_id: requirement.requirement_id,
  item_id: evidence.item_id,
  owner_group: evidence.owner_group,
  priority: evidence.priority,
  category: evidence.category,
  current_status: evidence.current_status,
  template_path: evidence.template_path,
  target_manifest_path: evidence.target_manifest_path,
  operator_next_step: evidence.operator_next_step,
})));

const csvRows = detailRows.map((row) => ({
  requirement_id: row.requirement_id,
  item_id: row.item_id,
  owner_group: row.owner_group,
  priority: row.priority,
  category: row.category,
  current_status: row.current_status,
  template_path: row.template_path,
  target_manifest_path: row.target_manifest_path,
  operator_next_step: row.operator_next_step,
}));

const md = normalize(`# 外部证据需求追踪报告 V0.1

## 1. 机器标记

- EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_READY
- GENERATED_FROM_REQUIREMENTS_CODE_MATRIX
- GENERATED_FROM_PRODUCTION_EVIDENCE_LEDGER
- GENERATED_FROM_PRODUCTION_EVIDENCE_STATUS
- PARTIAL_REQUIREMENTS_HAVE_OWNER_MANIFESTS
- NO_FAKE_EXTERNAL_EVIDENCE_CLOSURE
- MOCK_IS_NOT_PRODUCTION_EVIDENCE

## 2. 定位

本文由 \`tools/generate-external-evidence-requirement-traceability.mjs\` 从 \`${matrixPath}\`、\`${ledgerPath}\` 和 \`${statusPath}\` 自动生成，用于解决 8 条“部分实现”需求的验收追踪问题：代码和 mock/沙箱可支持联调与业务验收，但真实支付/POS/人脸/视频/机器人/设备/对象存储/开放平台凭证等生产证据未回填前，不得宣称生产闭环完成。

本文只证明“每条 partial 需求已经挂到责任组、证据 item_id、模板和目标 manifest 路径”，不关闭任何 blocker，不接收原始证据入库，也不允许把 mock 截图当生产证据。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| partial 需求 | ${counts.partial_requirements} |
| 已挂证据台账的 partial 需求 | ${counts.traceable_partial_requirements} |
| 去重证据 item_id | ${counts.mapped_evidence_items} |
| 证据台账行 | ${counts.mapped_ledger_rows} |
| P0 证据台账行 | ${counts.p0_evidence_rows} |
| awaiting evidence 行 | ${counts.awaiting_evidence_rows} |
| invalid evidence 行 | ${counts.invalid_evidence_rows} |
| 仍阻塞生产声明的需求 | ${counts.production_blocking_requirements} |

## 4. 需求到生产证据摘要

${mdTable(summaryRows, [
  { key: "requirement_id", label: "需求" },
  { key: "module", label: "模块" },
  { key: "current_status", label: "矩阵状态" },
  { key: "production_trace_status", label: "生产证据追踪状态" },
  { key: "mapped_item_ids", label: "证据 item_id" },
  { key: "owner_groups", label: "责任组" },
  { key: "p0_evidence_rows", label: "P0 行" },
  { key: "awaiting_evidence_rows", label: "待提交行" },
  { key: "target_manifest_paths", label: "目标 manifest" },
])}
## 5. 执行台账明细

${mdTable(detailRows, [
  { key: "requirement_id", label: "需求" },
  { key: "item_id", label: "item_id" },
  { key: "owner_group", label: "责任组" },
  { key: "priority", label: "优先级" },
  { key: "category", label: "类别" },
  { key: "current_status", label: "状态" },
  { key: "template_path", label: "模板" },
  { key: "target_manifest_path", label: "目标 manifest" },
  { key: "operator_next_step", label: "下一步" },
])}
## 6. 验收口径

1. 进入联调/业务验收：允许使用 mock、adapter、沙箱和本地证据链验证业务路径。
2. 进入生产上线声明：必须由责任组按目标 manifest 回填真实外部证据，并通过生产证据提交目录护栏、状态看板、回填优先级、执行台账、交付总闸口和安全扫描。
3. 对支付、人脸、视频、机器人、设备和开放平台凭证，任何明文密钥、token、密码、私钥正文都不得写入仓库；只能写 \`cred://...\` 引用。
4. 若供应商 SDK/API 尚未确定，需求保持 partial，不得在需求矩阵或最终验收审计中改写为生产完成。

## 7. 复跑命令

- \`npm run docs:external-evidence-requirement-traceability\`
- \`npm run verify:external-evidence-requirement-traceability\`
- \`npm run verify:delivery-gates\`
`);

const json = normalize(JSON.stringify(output, null, 2));
const csv = csvTable(csvRows, [
  { key: "requirement_id", label: "requirement_id" },
  { key: "item_id", label: "item_id" },
  { key: "owner_group", label: "owner_group" },
  { key: "priority", label: "priority" },
  { key: "category", label: "category" },
  { key: "current_status", label: "current_status" },
  { key: "template_path", label: "template_path" },
  { key: "target_manifest_path", label: "target_manifest_path" },
  { key: "operator_next_step", label: "operator_next_step" },
]);

forbidPlaintextSecrets(jsonPath, json);
forbidPlaintextSecrets(csvPath, csv);
forbidPlaintextSecrets(mdPath, md);

function failWith(items) {
  console.error("EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_FAIL");
  for (const item of items) console.error(`- ${item}`);
  process.exit(1);
}

if (checkMode) {
  const checkFailures = [...failures];
  for (const relativePath of [jsonPath, csvPath, mdPath]) {
    if (!fs.existsSync(absolute(relativePath))) checkFailures.push(`${relativePath} is missing; run npm run docs:external-evidence-requirement-traceability`);
  }
  if (checkFailures.length === 0) {
    if (normalize(readText(jsonPath)) !== json) checkFailures.push(`${jsonPath} is not in sync`);
    if (fs.readFileSync(absolute(csvPath), "utf8").replace(/\r\n/g, "\n") !== csv) checkFailures.push(`${csvPath} is not in sync`);
    if (normalize(readText(mdPath)) !== md) checkFailures.push(`${mdPath} is not in sync`);
  }
  if (counts.partial_requirements !== counts.traceable_partial_requirements) checkFailures.push("not every partial requirement is traceable to owner manifests");
  if (counts.partial_requirements === 0) checkFailures.push("expected at least one external evidence partial requirement while production evidence is open");
  if (checkFailures.length > 0) failWith(checkFailures);
  console.log("EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_PASS");
} else {
  fs.mkdirSync(path.dirname(absolute(jsonPath)), { recursive: true });
  fs.writeFileSync(absolute(jsonPath), json, "utf8");
  fs.writeFileSync(absolute(csvPath), csv, "utf8");
  fs.writeFileSync(absolute(mdPath), md, "utf8");
  if (failures.length > 0) failWith(failures);
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${csvPath}`);
  console.log(`generated ${mdPath}`);
}