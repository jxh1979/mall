import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const statusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const priorityPath = "docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json";
const ledgerPath = "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.json";
const csvPath = "docs/production-readiness-evidence/production-evidence-p0-quickstart-latest.csv";
const mdPath = "docs/production-evidence-p0-quickstart-v0.1.md";

function readText(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8").replace(/^\uFEFF/, "");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function normalize(text) {
  return text.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function escapeMd(value) {
  return String(value ?? "").replace(/\|/g, "/").replace(/\r?\n/g, "<br>");
}

function mdTable(rows, columns) {
  if (rows.length === 0) return "无。\n";
  const header = `| ${columns.map((column) => column.label).join(" | ")} |`;
  const separator = `| ${columns.map(() => "---").join(" | ")} |`;
  const body = rows.map((row) => `| ${columns.map((column) => escapeMd(row[column.key])).join(" | ")} |`);
  return [header, separator, ...body].join("\n") + "\n";
}

function csvEscape(value) {
  const text = Array.isArray(value) ? value.join("; ") : String(value ?? "");
  const escaped = text.replace(/"/g, '""');
  return /[",\r\n]/.test(escaped) ? `"${escaped}"` : escaped;
}

function csvTable(rows, columns) {
  return "\uFEFF" + [
    columns.map((column) => csvEscape(column.label)).join(","),
    ...rows.map((row) => columns.map((column) => csvEscape(row[column.key])).join(",")),
  ].join("\n") + "\n";
}

function forbidPlaintextSecrets(label, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
    /\b(?:secret_key|secret)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) throw new Error(`${label} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

function firstActionFor(ownerGroup, rows) {
  const categories = new Set(rows.map((row) => row.category));
  if (ownerGroup === "采购/供应商联调" && categories.has("MDM")) return "先补 MDM/Android Enterprise 选型、设备 enroll、策略下发和丢失/擦除演练证据。";
  if (ownerGroup === "财务" && categories.has("payment")) return "先补聚合支付商户、回调验签、退款权限、对账样例和财务确认材料。";
  if (ownerGroup === "IT/运维/安全" && categories.has("credential")) return "先补生产账号、数据库、对象存储、JWT、第三方和 NAS credential_ref、到期提醒和轮换证据。";
  if (ownerGroup === "IT/运维/安全" && categories.has("nas")) return "先按 NAS 诊断报告恢复 CIFS 挂载，再补自动挂载、定时备份、告警和恢复演练 manifest。";
  if (categories.has("face_age")) return "先补人脸/年龄核验供应商 SDK/API、测试账号、合规材料和成功/失败样例。";
  return "按 owner guide 复制模板为目标 manifest，补外部证据链接、复核人、测试时间和 cred:// 引用。";
}

function sortRows(a, b) {
  const ownerOrder = ["采购/供应商联调", "IT/运维/安全", "财务", "技术实现负责人", "运营/法务"];
  const categoryOrder = ["MDM", "payment", "credential", "nas", "ops160", "face_age"];
  const ownerDelta = ownerOrder.indexOf(a.owner_group) - ownerOrder.indexOf(b.owner_group);
  if (ownerDelta !== 0) return ownerDelta;
  const categoryDelta = categoryOrder.indexOf(a.category) - categoryOrder.indexOf(b.category);
  if (categoryDelta !== 0) return categoryDelta;
  return a.item_id.localeCompare(b.item_id, "zh-Hans-CN");
}

const status = readJson(statusPath);
const priority = readJson(priorityPath);
const ledger = readJson(ledgerPath);
const p0Rows = (ledger.rows ?? [])
  .filter((row) => row.priority === "P0")
  .sort(sortRows)
  .map((row) => ({
    owner_group: row.owner_group,
    category: row.category,
    item_id: row.item_id,
    current_status: row.current_status,
    template_path: row.template_path,
    target_manifest_path: row.target_manifest_path,
    accepted_evidence_types: row.accepted_evidence_types ?? [],
    required_evidence_summary: row.required_evidence_summary ?? [],
    operator_next_step: row.operator_next_step,
  }));

const ownerGroups = [...new Set(p0Rows.map((row) => row.owner_group))];
const ownerSummaries = ownerGroups.map((ownerGroup) => {
  const owned = p0Rows.filter((row) => row.owner_group === ownerGroup);
  const awaiting = owned.filter((row) => row.current_status === "awaiting_submission");
  const invalid = owned.filter((row) => row.current_status === "invalid_submission");
  return {
    owner_group: ownerGroup,
    p0_total: owned.length,
    awaiting_submission: awaiting.length,
    invalid_submission: invalid.length,
    categories: [...new Set(owned.map((row) => row.category))].join(", "),
    first_action: invalid.length > 0 ? "先修复 invalid manifest，再继续补缺失证据。" : firstActionFor(ownerGroup, owned),
    owner_guide_path: `docs/production-readiness-evidence/owner-guides/${ownerGroup === "IT/运维/安全" ? "it-ops-security" : ownerGroup === "采购/供应商联调" ? "procurement-supplier" : ownerGroup === "技术实现负责人" ? "technical-owner" : ownerGroup === "运营/法务" ? "operations-legal" : "finance"}.md`,
  };
});

const p0Batches = (priority.batches ?? []).filter((batch) => batch.priority === "P0").map((batch) => ({
  batch_id: batch.batch_id,
  rank: batch.rank,
  owner_group: batch.owner_group,
  category: batch.category,
  awaiting_submission: batch.awaiting_submission,
  invalid_submission: batch.invalid_submission,
  item_ids: batch.item_ids ?? [],
  recommended_action: batch.recommended_action,
}));

const counts = {
  expected_submissions: status.counts?.expected_submissions ?? 0,
  p0_expected: p0Rows.length,
  p0_awaiting_submission: p0Rows.filter((row) => row.current_status === "awaiting_submission").length,
  p0_invalid_submission: p0Rows.filter((row) => row.current_status === "invalid_submission").length,
  p0_valid_submission: p0Rows.filter((row) => row.current_status === "valid_submission").length,
  owner_groups: ownerSummaries.length,
  p0_batches: p0Batches.length,
};

const output = {
  quickstart_version: "v0.1",
  status: counts.p0_invalid_submission > 0 ? "invalid_p0_manifest_blocks_quickstart" : counts.p0_awaiting_submission > 0 ? "awaiting_p0_evidence" : "p0_evidence_submitted",
  generated_from: [statusPath, priorityPath, ledgerPath],
  policy: {
    p0_only: true,
    quickstart_does_not_close_blockers: true,
    raw_evidence_stays_external: true,
    credential_ref_only_no_plaintext_secret: true,
    no_fake_production_manifest_closure: true,
  },
  counts,
  owner_summaries: ownerSummaries,
  p0_batches: p0Batches,
  p0_rows: p0Rows,
};

const md = normalize(`# P0 生产证据快速回填快照 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_P0_QUICKSTART_READY
- P0_ONLY_EVIDENCE_BACKFILL_VIEW
- OWNER_FIRST_ACTION_READY
- QUICKSTART_DOES_NOT_CLOSE_BLOCKERS
- RAW_EVIDENCE_STAYS_EXTERNAL
- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET

## 2. 定位

本文从 \`${statusPath}\`、\`${priorityPath}\` 和 \`${ledgerPath}\` 自动生成，只给责任人看 P0 证据缺口。它是快速派发表，不是生产证据本身；不能关闭任何生产阻断项。

## 3. P0 总览

| 指标 | 数量 |
|---|---:|
| P0 预期提交 | ${counts.p0_expected} |
| P0 等待提交 | ${counts.p0_awaiting_submission} |
| P0 无效提交 | ${counts.p0_invalid_submission} |
| P0 有效提交 | ${counts.p0_valid_submission} |
| 责任组 | ${counts.owner_groups} |
| P0 批次 | ${counts.p0_batches} |

## 4. 责任组第一动作

${mdTable(ownerSummaries, [
  { key: "owner_group", label: "owner_group" },
  { key: "p0_total", label: "p0_total" },
  { key: "awaiting_submission", label: "awaiting" },
  { key: "invalid_submission", label: "invalid" },
  { key: "categories", label: "categories" },
  { key: "first_action", label: "first_action" },
  { key: "owner_guide_path", label: "owner_guide_path" },
])}
## 5. P0 批次顺序

${mdTable(p0Batches, [
  { key: "batch_id", label: "batch_id" },
  { key: "rank", label: "rank" },
  { key: "owner_group", label: "owner_group" },
  { key: "category", label: "category" },
  { key: "awaiting_submission", label: "awaiting" },
  { key: "item_ids", label: "item_ids" },
  { key: "recommended_action", label: "recommended_action" },
])}
## 6. P0 明细

${mdTable(p0Rows, [
  { key: "owner_group", label: "owner_group" },
  { key: "category", label: "category" },
  { key: "item_id", label: "item_id" },
  { key: "current_status", label: "status" },
  { key: "template_path", label: "template_path" },
  { key: "target_manifest_path", label: "target_manifest_path" },
  { key: "operator_next_step", label: "operator_next_step" },
])}
## 7. 回填后复跑

\`\`\`bash
npm run verify:production-evidence-submissions-guard
npm run docs:production-evidence-submission-status
npm run verify:production-evidence-submission-status
npm run docs:production-evidence-backfill-priority
npm run verify:production-evidence-backfill-priority
npm run docs:production-evidence-backfill-execution-ledger
npm run verify:production-evidence-backfill-execution-ledger
npm run docs:production-evidence-p0-quickstart
npm run verify:production-evidence-p0-quickstart
npm run verify:delivery-gates
\`\`\`
`);

const columns = [
  { key: "owner_group", label: "owner_group" },
  { key: "category", label: "category" },
  { key: "item_id", label: "item_id" },
  { key: "current_status", label: "current_status" },
  { key: "template_path", label: "template_path" },
  { key: "target_manifest_path", label: "target_manifest_path" },
  { key: "accepted_evidence_types", label: "accepted_evidence_types" },
  { key: "required_evidence_summary", label: "required_evidence_summary" },
  { key: "operator_next_step", label: "operator_next_step" },
];
const json = normalize(JSON.stringify(output, null, 2));
const csv = csvTable(p0Rows, columns);
forbidPlaintextSecrets(jsonPath, json);
forbidPlaintextSecrets(csvPath, csv);
forbidPlaintextSecrets(mdPath, md);

function fail(failures) {
  console.error("PRODUCTION_EVIDENCE_P0_QUICKSTART_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

if (checkMode) {
  const failures = [];
  for (const relativePath of [jsonPath, csvPath, mdPath]) {
    if (!fs.existsSync(path.join(repoRoot, relativePath))) failures.push(`${relativePath} is missing; run npm run docs:production-evidence-p0-quickstart`);
  }
  if (failures.length === 0) {
    if (normalize(readText(jsonPath)) !== json) failures.push(`${jsonPath} is not in sync`);
    const currentCsv = fs.readFileSync(path.join(repoRoot, csvPath), "utf8").replace(/\r\n/g, "\n");
    if (currentCsv !== csv) failures.push(`${csvPath} is not in sync`);
    if (normalize(readText(mdPath)) !== md) failures.push(`${mdPath} is not in sync`);
  }
  const expectedP0FromStatus = (status.counts?.p0_awaiting_submission ?? 0) + (status.counts?.p0_valid_submission ?? 0) + (status.counts?.p0_invalid_submission ?? 0);
  if (counts.p0_expected !== expectedP0FromStatus) failures.push('P0 row count must match current submission status');
  if (counts.p0_expected === 0) failures.push('quickstart must keep at least one current P0 row');
  if (!output.policy.quickstart_does_not_close_blockers) failures.push("quickstart must not close blockers");
  if (failures.length > 0) fail(failures);
  console.log("PRODUCTION_EVIDENCE_P0_QUICKSTART_PASS");
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, csvPath), csv, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${csvPath}`);
  console.log(`generated ${mdPath}`);
}
