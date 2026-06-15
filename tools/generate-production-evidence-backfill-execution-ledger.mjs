import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const priorityPath = "docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json";
const statusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const templateIndexPath = "docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json";
const csvPath = "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.csv";
const mdPath = "docs/production-evidence-backfill-execution-ledger-v0.1.md";

function readText(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function normalize(content) {
  return content.replace(/\r\n/g, "\n").trimEnd() + "\n";
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

function forbidPlaintextSecrets(relativePath, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
    /\b(?:secret|secret_key)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) throw new Error(`${relativePath} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

function keyFor(row) {
  return `${row.owner_group}||${row.item_id}||${row.priority}||${row.category}`;
}

function operatorStepFor(row) {
  if (row.current_status === "invalid_submission") return "先修复已提交 manifest 的格式、链接、脱敏或 credential_ref 问题，再复跑状态看板。";
  return "复制模板为目标 manifest，替换模板占位，填写外部证据链接、复核人、测试时间和 cred:// 引用后提交。";
}

const priority = readJson(priorityPath);
const status = readJson(statusPath);
const kit = readJson(kitPath);
const templateIndex = readJson(templateIndexPath);

const statusRows = Array.isArray(status.submissions) ? status.submissions : [];
const kitRows = Array.isArray(kit.submissions) ? kit.submissions : [];
const templateRows = Array.isArray(templateIndex.templates) ? templateIndex.templates : [];
const statusByKey = new Map(statusRows.map((row) => [keyFor(row), row]));
const kitBySubmissionId = new Map(kitRows.map((row) => [row.submission_id, row]));
const templateBySubmissionId = new Map(templateRows.map((row) => [row.submission_id, row]));

const ledgerRows = [];
for (const batch of priority.batches ?? []) {
  const batchRows = (batch.item_ids ?? [])
    .map((itemId) => statusByKey.get(`${batch.owner_group}||${itemId}||${batch.priority}||${batch.category}`))
    .filter(Boolean)
    .sort((a, b) => a.item_id.localeCompare(b.item_id, "zh-Hans-CN"));
  batchRows.forEach((statusRow, index) => {
    const kitRow = kitBySubmissionId.get(statusRow.submission_id) ?? {};
    const templateRow = templateBySubmissionId.get(statusRow.submission_id) ?? {};
    ledgerRows.push({
      ledger_id: `${batch.batch_id}-${String(index + 1).padStart(2, "0")}`,
      batch_id: batch.batch_id,
      batch_rank: batch.rank,
      owner_group: statusRow.owner_group,
      priority: statusRow.priority,
      category: statusRow.category,
      item_id: statusRow.item_id,
      submission_id: statusRow.submission_id,
      current_status: statusRow.status,
      source_status: kitRow.source_status ?? "",
      blocking_scope: kitRow.blocking_scope ?? "",
      packet_folder: kitRow.packet_folder ?? path.dirname(statusRow.manifest_path) + "/",
      template_path: templateRow.template_path ?? "",
      target_manifest_path: statusRow.manifest_path,
      required_fields_count: statusRow.required_fields_count ?? (kitRow.required_fields ?? []).length,
      accepted_evidence_types: kitRow.accepted_evidence_types ?? [],
      required_evidence_summary: kitRow.required_evidence_summary ?? [],
      not_accepted_evidence: kitRow.not_accepted_evidence ?? [],
      recommended_action: batch.recommended_action,
      operator_next_step: operatorStepFor({ current_status: statusRow.status }),
      verification_commands: batch.verification_commands ?? [],
    });
  });
}

const counts = {
  ledger_rows: ledgerRows.length,
  p0_rows: ledgerRows.filter((row) => row.priority === "P0").length,
  awaiting_rows: ledgerRows.filter((row) => row.current_status === "awaiting_submission").length,
  invalid_rows: ledgerRows.filter((row) => row.current_status === "invalid_submission").length,
  owner_groups: new Set(ledgerRows.map((row) => row.owner_group)).size,
  batches: new Set(ledgerRows.map((row) => row.batch_id)).size,
};

const output = {
  ledger_version: "v0.1",
  status: counts.invalid_rows > 0 ? "invalid_evidence_blocks_execution" : counts.p0_rows > 0 ? "awaiting_p0_execution" : counts.awaiting_rows > 0 ? "awaiting_lower_priority_execution" : "all_evidence_submitted",
  generated_from: [priorityPath, statusPath, kitPath, templateIndexPath],
  policy: {
    execution_ledger_does_not_close_blockers: true,
    one_row_per_evidence_submission: true,
    raw_evidence_stays_external: true,
    credential_ref_only_no_plaintext_secret: true,
    template_copy_requires_replacing_placeholders: true,
  },
  counts,
  rows: ledgerRows,
};

const mdRows = ledgerRows.map((row) => ({
  ledger_id: row.ledger_id,
  batch_id: row.batch_id,
  owner_group: row.owner_group,
  priority: row.priority,
  category: row.category,
  item_id: row.item_id,
  current_status: row.current_status,
  template_path: row.template_path,
  target_manifest_path: row.target_manifest_path,
  operator_next_step: row.operator_next_step,
}));

const columns = [
  { key: "ledger_id", label: "ledger_id" },
  { key: "batch_id", label: "batch_id" },
  { key: "batch_rank", label: "batch_rank" },
  { key: "owner_group", label: "owner_group" },
  { key: "priority", label: "priority" },
  { key: "category", label: "category" },
  { key: "item_id", label: "item_id" },
  { key: "submission_id", label: "submission_id" },
  { key: "current_status", label: "current_status" },
  { key: "source_status", label: "source_status" },
  { key: "blocking_scope", label: "blocking_scope" },
  { key: "packet_folder", label: "packet_folder" },
  { key: "template_path", label: "template_path" },
  { key: "target_manifest_path", label: "target_manifest_path" },
  { key: "required_fields_count", label: "required_fields_count" },
  { key: "accepted_evidence_types", label: "accepted_evidence_types" },
  { key: "required_evidence_summary", label: "required_evidence_summary" },
  { key: "not_accepted_evidence", label: "not_accepted_evidence" },
  { key: "recommended_action", label: "recommended_action" },
  { key: "operator_next_step", label: "operator_next_step" },
  { key: "verification_commands", label: "verification_commands" },
];

const md = normalize(`# 生产证据回填执行台账 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_BACKFILL_EXECUTION_LEDGER_READY
- ONE_ROW_PER_EVIDENCE_ITEM
- OPERATOR_BACKFILL_RUNBOOK_READY
- EXECUTION_LEDGER_DOES_NOT_CLOSE_BLOCKERS
- NO_RAW_EVIDENCE_IN_REPO

## 2. 定位

本文从 \`${priorityPath}\`、\`${statusPath}\`、\`${kitPath}\` 和 \`${templateIndexPath}\` 自动生成，用于把生产证据回填优先级拆成一行一个待补证据项的执行台账。本文只负责告诉责任组复制哪个模板、提交到哪个 manifest 路径、补哪些外部证据和复跑哪些命令；它不关闭任何生产阻断项，也不接收原始证据入库。

配套 CSV：\`${csvPath}\`，用于给 IT、财务、采购、运营、法务和技术负责人分派执行。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 台账行 | ${counts.ledger_rows} |
| P0 台账行 | ${counts.p0_rows} |
| 等待提交 | ${counts.awaiting_rows} |
| 无效提交 | ${counts.invalid_rows} |
| 责任组 | ${counts.owner_groups} |
| 批次 | ${counts.batches} |

## 4. 执行台账

${mdTable(mdRows, [
  { key: "ledger_id", label: "ledger_id" },
  { key: "batch_id", label: "batch_id" },
  { key: "owner_group", label: "owner_group" },
  { key: "priority", label: "priority" },
  { key: "category", label: "category" },
  { key: "item_id", label: "item_id" },
  { key: "current_status", label: "current_status" },
  { key: "template_path", label: "template_path" },
  { key: "target_manifest_path", label: "target_manifest_path" },
  { key: "operator_next_step", label: "operator_next_step" },
])}
## 5. 使用规则

1. 先按 \`batch_rank\` 从小到大处理，P0 优先。
2. 只允许把 manifest 放入 \`target_manifest_path\`；原始截图、导出、报告、合同、回执继续放在外部受控系统。
3. \`credential_refs\` 只能填写 \`cred://...\`，不得写明文密码、token、secret 或私钥正文。
4. 每个责任组回填后复跑 \`npm run verify:production-evidence-submissions-guard\`、\`npm run docs:production-evidence-submission-status\`、\`npm run docs:production-evidence-backfill-priority\`、\`npm run docs:production-evidence-backfill-execution-ledger\` 和 \`npm run verify:delivery-gates\`。
`);

const json = normalize(JSON.stringify(output, null, 2));
const csv = csvTable(ledgerRows, columns);
forbidPlaintextSecrets(jsonPath, json);
forbidPlaintextSecrets(csvPath, csv);
forbidPlaintextSecrets(mdPath, md);

function failWith(failures) {
  console.error("PRODUCTION_EVIDENCE_BACKFILL_EXECUTION_LEDGER_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

if (checkMode) {
  const failures = [];
  const missing = [jsonPath, csvPath, mdPath].filter((relativePath) => !fs.existsSync(path.join(repoRoot, relativePath)));
  if (missing.length > 0) failWith(missing.map((relativePath) => `${relativePath} is missing; run npm run docs:production-evidence-backfill-execution-ledger`));
  if (normalize(readText(jsonPath)) !== json) failures.push(`${jsonPath} is not in sync`);
  if (readText(csvPath).replace(/\r\n/g, "\n") !== csv) failures.push(`${csvPath} is not in sync`);
  if (normalize(readText(mdPath)) !== md) failures.push(`${mdPath} is not in sync`);
  if (!output.policy.execution_ledger_does_not_close_blockers) failures.push("execution ledger must not close blockers");
  if (counts.ledger_rows !== ledgerRows.length) failures.push("ledger row count mismatch");
  if (counts.p0_rows > counts.ledger_rows) failures.push("p0 row count cannot exceed ledger row count");
  if (failures.length > 0) failWith(failures);
  console.log("PRODUCTION_EVIDENCE_BACKFILL_EXECUTION_LEDGER_PASS");
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, csvPath), csv, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${csvPath}`);
  console.log(`generated ${mdPath}`);
}