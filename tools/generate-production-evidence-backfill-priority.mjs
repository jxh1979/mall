import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const statusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json";
const mdPath = "docs/production-evidence-backfill-priority-v0.1.md";

const priorityRank = new Map([["P0", 0], ["P1", 1], ["P2", 2], ["P3", 3]]);
const categoryRank = new Map([
  ["MDM", 0],
  ["payment", 1],
  ["credential", 2],
  ["nas", 3],
  ["ops160", 4],
  ["face_age", 5],
  ["wecom", 6],
  ["third_party", 7],
  ["security", 8],
]);
const statusRank = new Map([["invalid_submission", 0], ["awaiting_submission", 1], ["valid_submission", 2]]);

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

function rankValue(map, value, fallback = 99) {
  return map.has(value) ? map.get(value) : fallback;
}

function batchActionFor(batch) {
  if (batch.invalid_submission > 0) return "先修复无效 manifest，再补齐缺失证据。";
  if (batch.priority === "P0" && batch.category === "MDM") return "先完成 MDM/Android Enterprise 选型、设备 enroll、策略下发和丢失/擦除演练证据。";
  if (batch.priority === "P0" && batch.category === "payment") return "先补聚合支付商户、回调验签、退款、对账和财务确认材料。";
  if (batch.priority === "P0" && batch.category === "credential") return "先补生产账号、数据库、对象存储、JWT/第三方密钥的 credential_ref、到期提醒和轮换证据。";
  if (batch.priority === "P0" && batch.category === "nas") return "先补 NAS 自动挂载、定时备份、容量/失败告警和周期恢复演练证据。";
  if (batch.priority === "P0" && batch.category === "ops160") return "先补 160 生产运行配置、systemd、健康检查、日志和回滚证据。";
  if (batch.priority === "P0" && batch.category === "face_age") return "先补人脸/年龄核验供应商 SDK/API、测试账号、合规和成功/失败样例。";
  return "按 manifest 模板补齐外部可追溯证据链接、复核人、测试时间和 credential_ref。";
}

function batchKey(row) {
  return [row.owner_group, row.priority, row.category].join("||");
}

const status = readJson(statusPath);
const kit = readJson(kitPath);
const kitBySubmissionId = new Map((kit.submissions ?? []).map((submission) => [submission.submission_id, submission]));
const rows = (status.submissions ?? []).map((row) => {
  const kitRow = kitBySubmissionId.get(row.submission_id) ?? {};
  return {
    ...row,
    source_status: kitRow.source_status,
    blocking_scope: kitRow.blocking_scope,
    required_evidence_summary: kitRow.required_evidence_summary ?? [],
    accepted_evidence_types: kitRow.accepted_evidence_types ?? [],
    not_accepted_evidence: kitRow.not_accepted_evidence ?? [],
  };
});

const ownerGroups = [...new Set(rows.map((row) => row.owner_group))].sort((a, b) => a.localeCompare(b, "zh-Hans-CN"));
const ownerSummaries = ownerGroups.map((ownerGroup) => {
  const owned = rows.filter((row) => row.owner_group === ownerGroup);
  return {
    owner_group: ownerGroup,
    expected_submissions: owned.length,
    p0_expected: owned.filter((row) => row.priority === "P0").length,
    p0_awaiting: owned.filter((row) => row.priority === "P0" && row.status === "awaiting_submission").length,
    p0_valid: owned.filter((row) => row.priority === "P0" && row.status === "valid_submission").length,
    p0_invalid: owned.filter((row) => row.priority === "P0" && row.status === "invalid_submission").length,
    awaiting_total: owned.filter((row) => row.status === "awaiting_submission").length,
    valid_total: owned.filter((row) => row.status === "valid_submission").length,
    invalid_total: owned.filter((row) => row.status === "invalid_submission").length,
  };
}).sort((a, b) => b.p0_invalid - a.p0_invalid || b.p0_awaiting - a.p0_awaiting || b.awaiting_total - a.awaiting_total || a.owner_group.localeCompare(b.owner_group, "zh-Hans-CN"));

const batchesByKey = new Map();
for (const row of rows.filter((item) => item.status !== "valid_submission")) {
  const key = batchKey(row);
  if (!batchesByKey.has(key)) {
    batchesByKey.set(key, {
      owner_group: row.owner_group,
      priority: row.priority,
      category: row.category,
      item_ids: [],
      manifest_paths: [],
      blocking_scopes: [],
      required_evidence_summary: [],
      accepted_evidence_types: new Set(),
      awaiting_submission: 0,
      invalid_submission: 0,
      valid_submission: 0,
    });
  }
  const batch = batchesByKey.get(key);
  batch.item_ids.push(row.item_id);
  batch.manifest_paths.push(row.manifest_path);
  if (row.blocking_scope) batch.blocking_scopes.push(row.blocking_scope);
  batch.required_evidence_summary.push(...(row.required_evidence_summary ?? []));
  for (const evidenceType of row.accepted_evidence_types ?? []) batch.accepted_evidence_types.add(evidenceType);
  batch[row.status] += 1;
}

const batches = [...batchesByKey.values()].map((batch) => ({
  ...batch,
  accepted_evidence_types: [...batch.accepted_evidence_types].sort(),
  item_ids: [...new Set(batch.item_ids)].sort(),
  manifest_paths: [...new Set(batch.manifest_paths)].sort(),
  blocking_scopes: [...new Set(batch.blocking_scopes)].sort(),
  required_evidence_summary: [...new Set(batch.required_evidence_summary)].sort(),
})).sort((a, b) => {
  return b.invalid_submission - a.invalid_submission
    || rankValue(priorityRank, a.priority) - rankValue(priorityRank, b.priority)
    || rankValue(categoryRank, a.category) - rankValue(categoryRank, b.category)
    || b.awaiting_submission - a.awaiting_submission
    || a.owner_group.localeCompare(b.owner_group, "zh-Hans-CN");
}).map((batch, index) => ({
  batch_id: `BATCH-${String(index + 1).padStart(2, "0")}`,
  rank: index + 1,
  owner_group: batch.owner_group,
  priority: batch.priority,
  category: batch.category,
  awaiting_submission: batch.awaiting_submission,
  invalid_submission: batch.invalid_submission,
  valid_submission: batch.valid_submission,
  item_ids: batch.item_ids,
  manifest_paths: batch.manifest_paths,
  blocking_scopes: batch.blocking_scopes,
  required_evidence_summary: batch.required_evidence_summary,
  accepted_evidence_types: batch.accepted_evidence_types,
  recommended_action: batchActionFor(batch),
  verification_commands: [
    "npm run verify:production-evidence-submissions-guard",
    "npm run docs:production-evidence-submission-status",
    "npm run verify:production-evidence-submission-status",
    "npm run docs:production-evidence-backfill-priority",
    "npm run verify:production-evidence-backfill-priority",
    "npm run verify:delivery-gates",
  ],
}));

const counts = {
  expected_submissions: rows.length,
  p0_expected: rows.filter((row) => row.priority === "P0").length,
  p0_awaiting: rows.filter((row) => row.priority === "P0" && row.status === "awaiting_submission").length,
  p0_valid: rows.filter((row) => row.priority === "P0" && row.status === "valid_submission").length,
  p0_invalid: rows.filter((row) => row.priority === "P0" && row.status === "invalid_submission").length,
  awaiting_submission: rows.filter((row) => row.status === "awaiting_submission").length,
  valid_submission: rows.filter((row) => row.status === "valid_submission").length,
  invalid_submission: rows.filter((row) => row.status === "invalid_submission").length,
  open_batches: batches.length,
};

const output = {
  priority_version: "v0.1",
  status: counts.invalid_submission > 0 ? "invalid_evidence_blocks_backfill" : counts.p0_awaiting > 0 ? "awaiting_p0_evidence" : counts.awaiting_submission > 0 ? "awaiting_lower_priority_evidence" : "all_evidence_submitted",
  generated_from: [statusPath, kitPath],
  policy: {
    priority_board_does_not_close_blockers: true,
    p0_evidence_first: true,
    invalid_manifest_blocks_first: true,
    raw_evidence_stays_external: true,
    credential_ref_only_no_plaintext_secret: true,
  },
  counts,
  owner_summaries: ownerSummaries,
  batches,
};

const ownerRows = ownerSummaries.map((summary) => ({
  owner_group: summary.owner_group,
  p0_expected: summary.p0_expected,
  p0_awaiting: summary.p0_awaiting,
  p0_valid: summary.p0_valid,
  p0_invalid: summary.p0_invalid,
  awaiting_total: summary.awaiting_total,
  invalid_total: summary.invalid_total,
}));

const batchRows = batches.map((batch) => ({
  batch_id: batch.batch_id,
  rank: batch.rank,
  owner_group: batch.owner_group,
  priority: batch.priority,
  category: batch.category,
  awaiting_submission: batch.awaiting_submission,
  invalid_submission: batch.invalid_submission,
  item_ids: batch.item_ids.join(", "),
  recommended_action: batch.recommended_action,
}));

const md = normalize(`# 生产证据回填优先级看板 V0.1

## 1. 机器标记

- PRODUCTION_EVIDENCE_BACKFILL_PRIORITY_READY
- P0_EVIDENCE_GAPS_TRACKED
- OWNER_BACKFILL_BATCHES_READY
- PRIORITY_BOARD_DOES_NOT_CLOSE_BLOCKERS
- RAW_EVIDENCE_STAYS_EXTERNAL

## 2. 定位

本文从 \`${statusPath}\` 和 \`${kitPath}\` 自动生成，用于告诉 IT、财务、采购、运营、法务和技术负责人下一批应该先补哪些真实生产证据。本文只排序和分派，不关闭生产阻断项；只有真实 manifest 通过状态看板和来源台账/finding 同步更新后，才能进入关闭判断。

## 3. 总览

| 指标 | 数量 |
|---|---:|
| 预期提交项 | ${counts.expected_submissions} |
| P0 预期提交项 | ${counts.p0_expected} |
| P0 等待提交 | ${counts.p0_awaiting} |
| P0 有效提交 | ${counts.p0_valid} |
| P0 无效提交 | ${counts.p0_invalid} |
| 全部等待提交 | ${counts.awaiting_submission} |
| 全部有效提交 | ${counts.valid_submission} |
| 全部无效提交 | ${counts.invalid_submission} |
| 开放回填批次 | ${counts.open_batches} |

## 4. 责任组 P0 缺口

${mdTable(ownerRows, [
  { key: "owner_group", label: "owner_group" },
  { key: "p0_expected", label: "p0_expected" },
  { key: "p0_awaiting", label: "p0_awaiting" },
  { key: "p0_valid", label: "p0_valid" },
  { key: "p0_invalid", label: "p0_invalid" },
  { key: "awaiting_total", label: "awaiting_total" },
  { key: "invalid_total", label: "invalid_total" },
])}
## 5. 建议回填批次

${mdTable(batchRows, [
  { key: "batch_id", label: "batch_id" },
  { key: "rank", label: "rank" },
  { key: "owner_group", label: "owner_group" },
  { key: "priority", label: "priority" },
  { key: "category", label: "category" },
  { key: "awaiting_submission", label: "awaiting_submission" },
  { key: "invalid_submission", label: "invalid_submission" },
  { key: "item_ids", label: "item_ids" },
  { key: "recommended_action", label: "recommended_action" },
])}
## 6. 执行规则

1. 先处理 \`invalid_submission\` 批次，再处理 P0 批次。
2. 同为 P0 时，按 MDM、支付、凭证、NAS、160 运维、人脸年龄核验、企微、第三方、安全的顺序推进。
3. 每个批次只提交 manifest；原始证据继续留在外部受控系统，并在 \`evidence_links\` 留链。
4. 每批回填后执行 \`npm run verify:production-evidence-submissions-guard\`、\`npm run docs:production-evidence-submission-status\`、\`npm run docs:production-evidence-backfill-priority\` 和 \`npm run verify:delivery-gates\`。
`);

const json = normalize(JSON.stringify(output, null, 2));
forbidPlaintextSecrets(jsonPath, json);
forbidPlaintextSecrets(mdPath, md);

function failWith(failures) {
  console.error("PRODUCTION_EVIDENCE_BACKFILL_PRIORITY_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

if (checkMode) {
  const failures = [];
  const missing = [jsonPath, mdPath].filter((relativePath) => !fs.existsSync(path.join(repoRoot, relativePath)));
  if (missing.length > 0) failWith(missing.map((relativePath) => `${relativePath} is missing; run npm run docs:production-evidence-backfill-priority`));
  if (normalize(readText(jsonPath)) !== json) failures.push(`${jsonPath} is not in sync`);
  if (normalize(readText(mdPath)) !== md) failures.push(`${mdPath} is not in sync`);
  if (counts.p0_expected < counts.p0_valid) failures.push("p0_valid cannot exceed p0_expected");
  if (batches.some((batch, index) => batch.rank !== index + 1)) failures.push("batch ranks must be sequential");
  if (failures.length > 0) failWith(failures);
  console.log("PRODUCTION_EVIDENCE_BACKFILL_PRIORITY_PASS");
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
}
