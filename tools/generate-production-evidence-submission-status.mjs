import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const mdPath = "docs/production-evidence-submission-status-v0.1.md";

const resultAllowedValues = new Set(["passed", "failed", "partial", "blocked"]);

function stripBom(text) {
  return text.replace(/^\uFEFF/, "");
}

function readText(relativePath) {
  return stripBom(fs.readFileSync(path.join(repoRoot, relativePath), "utf8"));
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

function collectTextValues(value) {
  if (value === null || value === undefined) return [];
  if (Array.isArray(value)) return value.flatMap((item) => collectTextValues(item));
  if (typeof value === "object") return Object.values(value).flatMap((item) => collectTextValues(item));
  return [String(value).trim()].filter(Boolean);
}

function validateUniqueTextValues(fieldName, value) {
  const failures = [];
  const seen = new Set();
  const duplicateValues = new Set();
  for (const entry of collectTextValues(value)) {
    const key = entry.trim().toLowerCase();
    if (seen.has(key)) duplicateValues.add(entry);
    seen.add(key);
  }
  for (const duplicateValue of duplicateValues) failures.push(`${fieldName} must not contain duplicate value: ${duplicateValue}`);
  return failures;
}

function validateNonEmptyArrayField(fieldName, value) {
  if (!Array.isArray(value)) return [`${fieldName} must be a non-empty array`];
  if (value.length === 0) return [`${fieldName} must be a non-empty array`];
  return [];
}

function hasPlaceholderText(value) {
  return /(待填写|TODO|TBD|placeholder|replace|example|sample|dummy|test-placeholder|示例|占位)/i.test(String(value));
}

function hasTemplateOnlyMarker(value) {
  return /(TEMPLATE_ONLY|NOT_PRODUCTION_EVIDENCE|模板不是生产证据|仅作填写样例)/i.test(String(value));
}

function isIsoDateLike(value) {
  const match = String(value).trim().match(/^(\d{4})-(\d{2})-(\d{2})(?:[T ]([01]\d|2[0-3]):([0-5]\d)(?::([0-5]\d)(?:\.\d{1,6})?)?(?:Z|[+-](?:[01]\d|2[0-3]):[0-5]\d)?)?$/);
  if (!match) return false;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (month < 1 || month > 12 || day < 1) return false;
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate();
  return day <= daysInMonth;
}

function parseIsoDateLike(value) {
  const text = String(value).trim();
  if (!isIsoDateLike(text)) return null;
  const normalized = /^\d{4}-\d{2}-\d{2}$/.test(text) ? `${text}T00:00:00Z` : text.replace(" ", "T");
  const parsed = new Date(normalized);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function isFutureDate(value, now = new Date()) {
  const parsed = parseIsoDateLike(value);
  if (!parsed) return false;
  return parsed.getTime() > now.getTime() + 5 * 60 * 1000;
}

function retestDueExpiryInstant(value) {
  const text = String(value).trim();
  if (!isIsoDateLike(text)) return null;
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return new Date(`${text}T23:59:59.999Z`);
  return parseIsoDateLike(text);
}

function isExpiredRetestDue(value, now = new Date()) {
  const expiry = retestDueExpiryInstant(value);
  if (!expiry) return false;
  return expiry.getTime() < now.getTime() - 5 * 60 * 1000;
}

function isAfterDate(left, right) {
  const leftParsed = parseIsoDateLike(left);
  const rightParsed = parseIsoDateLike(right);
  if (!leftParsed || !rightParsed) return false;
  return leftParsed.getTime() > rightParsed.getTime();
}

function isLocalOrDevReference(value) {
  const text = String(value).trim();
  return /^(?:file:\/\/|[A-Za-z]:\\|\\\\|\/(?:home|mnt|tmp|var|etc|Users|Volumes)\/)/.test(text)
    || /^https?:\/\/(?:localhost|127\.0\.0\.1|0\.0\.0\.0|\[?::1\]?)(?:[:/]|$)/i.test(text)
    || /^https?:\/\/(?:example\.com|example\.org|example\.net)(?:[:/]|$)/i.test(text);
}

function isPlainHttpReference(value) {
  return /^http:\/\//i.test(String(value).trim());
}

function isTraceableEvidenceReference(value) {
  const text = String(value).trim();
  return /^(?:https?:\/\/|smb:\/\/|nas:\/\/|sharepoint:\/\/|oss:\/\/|s3:\/\/|minio:\/\/|ticket:\/\/|itsm:\/\/|workorder:\/\/|approval:\/\/|supplier:\/\/|doc:\/\/|vault:\/\/|urn:)/i.test(text)
    || /^(?:ITSM|TICKET|WO|APPROVAL|SUPPLIER|NAS-EVIDENCE|DOC|EVD)-[A-Za-z0-9][A-Za-z0-9_.:-]{3,}$/i.test(text);
}

function validateTraceableReferences(fieldName, value) {
  const failures = [];
  for (const entry of collectTextValues(value)) {
    if (hasPlaceholderText(entry)) failures.push(`${fieldName} contains placeholder value: ${entry}`);
    if (isLocalOrDevReference(entry)) failures.push(`${fieldName} must not use local/dev path or localhost reference: ${entry}`);
    if (isPlainHttpReference(entry)) failures.push(`${fieldName} must use HTTPS or an approved non-web evidence protocol: ${entry}`);
    if (!isTraceableEvidenceReference(entry)) failures.push(`${fieldName} must use traceable external reference or approved evidence id prefix: ${entry}`);
  }
  return failures;
}

function validateCredentialRefs(value) {
  const failures = [];
  for (const entry of collectTextValues(value)) {
    if (hasPlaceholderText(entry)) failures.push(`credential_refs contains placeholder value: ${entry}`);
    if (!/^cred:\/\/[A-Za-z0-9][A-Za-z0-9/_.:@-]*$/i.test(entry)) {
      failures.push(`credential_refs must use cred:// reference only: ${entry}`);
    } else if (!/^cred:\/\/prod\/[A-Za-z0-9][A-Za-z0-9/_.:@-]*$/i.test(entry)) {
      failures.push(`credential_refs must use production cred://prod/... reference only: ${entry}`);
    }
  }
  return failures;
}

function validateEvidenceIds(value) {
  const failures = [];
  for (const entry of collectTextValues(value)) {
    if (!/^EVD-[A-Za-z0-9][A-Za-z0-9_.:-]{3,}$/i.test(entry)) failures.push(`evidence_ids must use EVD-... reference only: ${entry}`);
  }
  return failures;
}

function loadManifest(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) return { exists: false };
  const text = stripBom(fs.readFileSync(absolutePath, "utf8"));
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
  const allowedFields = new Set([...(template.required_fields ?? []), "evidence_types", "notes"]);
  for (const field of Object.keys(data)) {
    if (!allowedFields.has(field)) failures.push(`unknown manifest field: ${field}`);
  }
  for (const forbiddenField of ["template_id", "template_notice"]) {
    if (!isBlank(data[forbiddenField])) failures.push(`${forbiddenField} must not be present in real submission manifest`);
  }
  for (const [field, value] of Object.entries(data)) {
    for (const entry of collectTextValues(value)) {
      if (hasPlaceholderText(entry)) failures.push(`${field} contains placeholder value: ${entry}`);
      if (hasTemplateOnlyMarker(entry)) failures.push(`${field} contains template-only marker: ${entry}`);
    }
  }
  for (const field of template.required_fields ?? []) {
    if (isBlank(data[field])) failures.push(`missing required field: ${field}`);
  }
  if (!isBlank(data.owner_person) && !isBlank(data.reviewer) && String(data.owner_person).trim().toLowerCase() === String(data.reviewer).trim().toLowerCase()) {
    failures.push("reviewer must be different from owner_person");
  }
  for (const field of ["submitted_at", "last_test_time", "next_retest_due"]) {
    if (!isBlank(data[field]) && !isIsoDateLike(data[field])) failures.push(`${field} must be valid ISO date or datetime`);
  }
  for (const field of ["submitted_at", "last_test_time"]) {
    if (!isBlank(data[field]) && isFutureDate(data[field])) failures.push(`${field} must not be in the future`);
  }
  if (!isBlank(data.submitted_at) && !isBlank(data.last_test_time) && isAfterDate(data.last_test_time, data.submitted_at)) {
    failures.push("last_test_time must not be after submitted_at");
  }
  if (!isBlank(data.next_retest_due) && isExpiredRetestDue(data.next_retest_due)) {
    failures.push("next_retest_due must not be expired");
  }
  for (const field of ["evidence_ids", "evidence_links", "failure_case_evidence", "credential_refs", "evidence_types"]) {
    if (!isBlank(data[field]) || (template.required_fields ?? []).includes(field)) failures.push(...validateNonEmptyArrayField(field, data[field]));
    if (!isBlank(data[field])) failures.push(...validateUniqueTextValues(field, data[field]));
  }
  if (!isBlank(data.evidence_ids)) failures.push(...validateEvidenceIds(data.evidence_ids));
  for (const [field, expected] of [
    ["submission_id", template.submission_id],
    ["item_id", template.item_id],
    ["owner_group", template.owner_group],
  ]) {
    if (data[field] !== expected) failures.push(`${field} must be ${expected}`);
  }
  if (!resultAllowedValues.has(data.result)) {
    failures.push(`result must be one of ${[...resultAllowedValues].join(", ")}`);
  } else if (data.result !== "passed") {
    failures.push(`result must be passed to count as valid production evidence submission; got ${data.result}`);
  }
  if (data.redaction_confirmed !== true) failures.push("redaction_confirmed must be true");
  if (!Array.isArray(data.evidence_types) || data.evidence_types.length === 0) {
    failures.push("evidence_types must include at least one accepted evidence type");
  } else {
    const allowed = new Set(template.accepted_evidence_types ?? []);
    for (const evidenceType of data.evidence_types) {
      if (!allowed.has(evidenceType)) failures.push(`evidence_type ${evidenceType} is not in accepted_evidence_types`);
    }
  }
  if (["passed", "partial"].includes(data.result) && isBlank(data.evidence_links)) {
    failures.push("passed or partial result must include evidence_links");
  }
  if (!isBlank(data.evidence_links)) failures.push(...validateTraceableReferences("evidence_links", data.evidence_links));
  if (!isBlank(data.failure_case_evidence)) failures.push(...validateTraceableReferences("failure_case_evidence", data.failure_case_evidence));
  if (!isBlank(data.approval_record)) failures.push(...validateTraceableReferences("approval_record", data.approval_record));
  if (!isBlank(data.credential_refs)) failures.push(...validateCredentialRefs(data.credential_refs));
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
    raw_evidence_stays_external_not_in_repo: true,
  },
  counts,
  owner_summaries: ownerRows,
  submissions: rows,
};

const md = normalize(`# 生产证据提交状态看板 V0.1\n\n## 1. 机器标记\n\n- PRODUCTION_EVIDENCE_SUBMISSION_STATUS_READY\n- STATUS_FROM_PRODUCTION_EVIDENCE_SUBMISSION_KIT\n- MISSING_MANIFEST_IS_AWAITING_NOT_PASSED\n- INVALID_MANIFEST_BLOCKS_DELIVERY_GATE\n- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET\n- PRODUCTION_CREDENTIAL_REF_REQUIRED\n- RAW_EVIDENCE_STAYS_EXTERNAL\n- TRACEABLE_EVIDENCE_LINKS_REQUIRED\n- CREDENTIAL_REF_PLACEHOLDER_FORBIDDEN\n\n## 2. 定位\n\n本文从 \`${kitPath}\` 自动生成，并扫描各责任组提交目录下的 \`manifest-<item_id>.json\`。未提交 manifest 只显示为 \`awaiting_submission\`，不会被误判为 passed；一旦提交 manifest 出现缺字段、格式错误（兼容 UTF-8 BOM）、明文密钥、非生产 credential_ref、占位值、模板专用字段、未知字段、证据编号格式错误、重复证据引用、证据数组字段结构错误、未接受的证据类型、result 非 passed、无效日历日期、未来提交/测试时间、过期复测日期、提交人与复核人相同、本地路径、localhost、明文 HTTP 证据链接、不可追溯证据链接或关键字段不匹配，则进入 \`invalid_submission\` 并阻断交付闸口。\n\n## 3. 总览\n\n| 指标 | 数量 |\n|---|---:|\n| 预期提交项 | ${counts.expected_submissions} |\n| 等待提交 | ${counts.awaiting_submission} |\n| 有效提交 | ${counts.valid_submission} |\n| 无效提交 | ${counts.invalid_submission} |\n| P0 等待提交 | ${counts.p0_awaiting_submission} |\n| P0 有效提交 | ${counts.p0_valid_submission} |\n| P0 无效提交 | ${counts.p0_invalid_submission} |\n\n## 4. 责任组状态\n\n${mdTable(ownerRows, [
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
])}\n## 6. 使用规则\n\n1. 责任人提交真实材料时，只在对应 \`packet_folder\` 下放置 \`manifest-<item_id>.json\`；原始证据文件必须存放在外部受控位置，并通过 \`evidence_links\` 引用。\n2. manifest 必须使用 \`production-evidence-submission-kit-v0.1.md\` 定义的字段，真实密钥只能写 \`credential_ref\`；\`evidence_links\`、\`failure_case_evidence\` 和 \`approval_record\` 必须是外部可追溯链接或批准的证据编号，不能是 TODO、示例值、本地路径或 localhost。\n3. 提交后执行 \`npm run verify:production-evidence-submissions-guard\`、\`npm run docs:production-evidence-submission-status\` 和 \`npm run verify:production-evidence-submission-status\`。\n4. 看板为 \`valid_submission\` 也不自动关闭阻断项；仍需更新来源台账、findings register 和复跑交付闸口。\n`);

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
