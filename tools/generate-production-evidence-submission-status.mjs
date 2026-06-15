import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const mdPath = "docs/production-evidence-submission-status-v0.1.md";

const resultAllowedValues = new Set(["passed", "failed", "partial", "blocked"]);

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

function forbidPlaintextSecrets(label, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
    /\b(?:secret|secret_key)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
  ];
  const hits = [];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) hits.push(`${label} contains apparent plaintext secret pattern #${index + 1}`);
  }
  return hits;
}

function isBlank(value) {
  if (value === null || value === undefined) return true;
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === "object") return Object.keys(value).length === 0;
  return String(value).trim() === "";
}

function loadManifest(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) return { exists: false };
  const text = fs.readFileSync(absolutePath, "utf8");
  const secretFindings = forbidPlaintextSecrets(relativePath, text);
  try {
    return { exists: true, text, data: JSON.parse(text), secret_findings: secretFindings };
  } catch (error) {
    return { exists: true, text, parse_error: error.message, secret_findings: secretFindings };
  }
}

function validateManifest(template, manifest) {
  const failures = [];
  const warnings = [];
  if (!manifest.exists) return { status: "awaiting_submission", failures, warnings };
  if (manifest.secret_findings?.length) failures.push(...manifest.secret_findings);
  if (manifest.parse_error) {
    failures.push(`manifest JSON parse error: ${manifest.parse_error}`);
    return { status: "invalid_submission", failures, warnings };
  }
  const data = manifest.data;
  for (const field of template.required_fields ?? []) {
    if (isBlank(data[field])) failures.push(`missing required field: ${field}`);
  }
  for (const [field, expected] of [
    ["submission_id", template.submission_id],
    ["item_id", template.item_id],
    ["owner_group", template.owner_group],
  ]) {
    if (data[field] !== expected) failures.push(`${field} must be ${expected}`);
  }
  if (!resultAllowedValues.has(data.result)) failures.push(`result must be one of ${[...resultAllowedValues].join(", ")}`);
  if (data.redaction_confirmed !== true) failures.push("redaction_confirmed must be true");
  if (Array.isArray(data.evidence_types)) {
    const allowed = new Set(template.accepted_evidence_types ?? []);
    for (const evidenceType of data.evidence_types) {
      if (!allowed.has(evidenceType)) warnings.push(`evidence_type ${evidenceType} is not in accepted_evidence_types`);
    }
  }
  if (["passed", "partial"].includes(data.result) && isBlank(data.evidence_links)) {
    failures.push("passed or partial result must include evidence_links");
  }
  return { status: failures.length > 0 ? "invalid_submission" : "valid_submission", failures, warnings };
}

function statusRank(status) {
  return {
    invalid_submission: 0,
    awaiting_submission: 1,
    valid_submission: 2,
  }[status] ?? 9;
}

function statusLabel(status) {
  if (status === "invalid_submission") return "invalid_submission";
  if (status === "valid_submission") return "valid_submission";
  return "awaiting_submission";
}

const kit = readJson(kitPath);
const rows = [];
for (const template of kit.submissions ?? []) {
  const manifestRelativePath = `${template.packet_folder}${template.manifest_file_name}`;
  const manifest = loadManifest(manifestRelativePath);
  const validation = validateManifest(template, manifest);
  rows.push({
    submission_id: template.submission_id,
    owner_group: template.owner_group,
    item_id: template.item_id,
    priority: template.priority,
    category: template.category,
    manifest_path: manifestRelativePath,
    status: validation.status,
    failures: validation.failures,
    warnings: validation.warnings,
    required_fields_count: (template.required_fields ?? []).length,
    accepted_evidence_types_count: (template.accepted_evidence_types ?? []).length,
  });
}

rows.sort((a, b) => statusRank(a.status) - statusRank(b.status) || String(a.owner_group).localeCompare(String(b.owner_group)) || String(a.item_id).localeCompare(String(b.item_id)));

const counts = {
  expected_submissions: rows.length,
  awaiting_submission: rows.filter((row) => row.status === "awaiting_submission").length,
  valid_submission: rows.filter((row) => row.status === "valid_submission").length,
  invalid_submission: rows.filter((row) => row.status === "invalid_submission").length,
  p0_awaiting_submission: rows.filter((row) => row.priority === "P0" && row.status === "awaiting_submission").length,
  p0_valid_submission: rows.filter((row) => row.priority === "P0" && row.status === "valid_submission").length,
  p0_invalid_submission: rows.filter((row) => row.priority === "P0" && row.status === "invalid_submission").length,
};

const ownerRows = [...new Set(rows.map((row) => row.owner_group))].map((ownerGroup) => {
  const ownedRows = rows.filter((row) => row.owner_group === ownerGroup);
  return {
    owner_group: ownerGroup,
    expected_submissions: ownedRows.length,
    awaiting_submission: ownedRows.filter((row) => row.status === "awaiting_submission").length,
    valid_submission: ownedRows.filter((row) => row.status === "valid_submission").length,
    invalid_submission: ownedRows.filter((row) => row.status === "invalid_submission").length,
  };
});

const output = {
  status_version: "v0.1",
  status: counts.invalid_submission > 0 ? "invalid_submissions_present" : counts.valid_submission > 0 ? "partial_real_evidence_submitted" : "awaiting_real_evidence",
  source_submission_kit_path: kitPath,
  generated_from: [kitPath],
  policy: {
    no_fake_closure: true,
    status_dashboard_does_not_close_blockers: true,
    missing_manifest_is_not_failure: true,
    invalid_manifest_blocks_delivery_gate: true,
    credential_ref_only_no_plaintext_secret: true,
  },
  counts,
  owner_summaries: ownerRows,
  submissions: rows,
};

const md = normalize(`# 生产证据提交状态看板 V0.1\n\n## 1. 机器标记\n\n- PRODUCTION_EVIDENCE_SUBMISSION_STATUS_READY\n- STATUS_FROM_PRODUCTION_EVIDENCE_SUBMISSION_KIT\n- MISSING_MANIFEST_IS_AWAITING_NOT_PASSED\n- INVALID_MANIFEST_BLOCKS_DELIVERY_GATE\n- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET\n\n## 2. 定位\n\n本文从 \`${kitPath}\` 自动生成，并扫描各责任组提交目录下的 \`manifest-<item_id>.json\`。未提交 manifest 只显示为 \`awaiting_submission\`，不会被误判为 passed；一旦提交 manifest 出现缺字段、格式错误、明文密钥或关键字段不匹配，则进入 \`invalid_submission\` 并阻断交付闸口。\n\n## 3. 总览\n\n| 指标 | 数量 |\n|---|---:|\n| 预期提交项 | ${counts.expected_submissions} |\n| 等待提交 | ${counts.awaiting_submission} |\n| 有效提交 | ${counts.valid_submission} |\n| 无效提交 | ${counts.invalid_submission} |\n| P0 等待提交 | ${counts.p0_awaiting_submission} |\n| P0 有效提交 | ${counts.p0_valid_submission} |\n| P0 无效提交 | ${counts.p0_invalid_submission} |\n\n## 4. 责任组状态\n\n${mdTable(ownerRows, [
  { key: "owner_group", label: "owner_group" },
  { key: "expected_submissions", label: "expected_submissions" },
  { key: "awaiting_submission", label: "awaiting_submission" },
  { key: "valid_submission", label: "valid_submission" },
  { key: "invalid_submission", label: "invalid_submission" },
])}\n## 5. 提交项状态\n\n${mdTable(rows.map((row) => ({
  submission_id: row.submission_id,
  owner_group: row.owner_group,
  item_id: row.item_id,
  priority: row.priority,
  category: row.category,
  status: statusLabel(row.status),
  manifest_path: row.manifest_path,
  failures: row.failures.join("；"),
  warnings: row.warnings.join("；"),
})), [
  { key: "submission_id", label: "submission_id" },
  { key: "owner_group", label: "owner_group" },
  { key: "item_id", label: "item_id" },
  { key: "priority", label: "priority" },
  { key: "category", label: "category" },
  { key: "status", label: "status" },
  { key: "manifest_path", label: "manifest_path" },
  { key: "failures", label: "failures" },
  { key: "warnings", label: "warnings" },
])}\n## 6. 使用规则\n\n1. 责任人提交真实材料时，在对应 \`packet_folder\` 下放置 \`manifest-<item_id>.json\` 和证据文件或链接。\n2. manifest 必须使用 \`production-evidence-submission-kit-v0.1.md\` 定义的字段，真实密钥只能写 \`credential_ref\`。\n3. 提交后执行 \`npm run docs:production-evidence-submission-status\` 和 \`npm run verify:production-evidence-submission-status\`。\n4. 看板为 \`valid_submission\` 也不自动关闭阻断项；仍需更新来源台账、findings register 和复跑交付闸口。\n`);

const json = normalize(JSON.stringify(output, null, 2));
const outputSecretHits = [...forbidPlaintextSecrets(mdPath, md), ...forbidPlaintextSecrets(jsonPath, json)];
if (outputSecretHits.length > 0) throw new Error(outputSecretHits.join("\n"));

function failWith(failures) {
  console.error("PRODUCTION_EVIDENCE_SUBMISSION_STATUS_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

if (checkMode) {
  const failures = [];
  const missingOutputs = [jsonPath, mdPath].filter((relativePath) => !fs.existsSync(path.join(repoRoot, relativePath)));
  if (missingOutputs.length > 0) failWith(missingOutputs.map((relativePath) => `${relativePath} is missing; run npm run docs:production-evidence-submission-status`));
  const currentJson = normalize(readText(jsonPath));
  const currentMd = normalize(readText(mdPath));
  if (currentJson !== json) failures.push(`${jsonPath} is not in sync`);
  if (currentMd !== md) failures.push(`${mdPath} is not in sync`);
  if (counts.invalid_submission > 0) failures.push(`invalid evidence submissions present: ${counts.invalid_submission}`);
  if (failures.length > 0) failWith(failures);
  console.log("PRODUCTION_EVIDENCE_SUBMISSION_STATUS_PASS");
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
}