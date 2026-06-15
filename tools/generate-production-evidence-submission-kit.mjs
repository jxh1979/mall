import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const checkMode = process.argv.includes("--check");
const sourcePath = "docs/production-readiness-evidence/production-owner-action-packs-latest.json";
const jsonPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const mdPath = "docs/production-evidence-submission-kit-v0.1.md";

function readText(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), "utf8");
}

function readJson(relativePath) {
  return JSON.parse(readText(relativePath));
}

function normalize(content) {
  return content.replace(/\r\n/g, "\n").trimEnd() + "\n";
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

const ownerSlugMap = new Map([
  ["IT/运维/安全", "it-ops-security"],
  ["财务", "finance"],
  ["采购/供应商联调", "procurement-supplier"],
  ["运营/法务", "operations-legal"],
  ["技术实现负责人", "technical-owner"],
  ["总控/项目管理", "program-management"],
]);

function slug(value) {
  const raw = String(value);
  if (ownerSlugMap.has(raw)) return ownerSlugMap.get(raw);
  return raw
    .replace(/IT/g, "it")
    .replace(/[\u4e00-\u9fff]+/g, "")
    .replace(/[^A-Za-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase() || "owner";
}

function duplicateValues(values) {
  const seen = new Set();
  const duplicates = new Set();
  for (const value of values) {
    if (seen.has(value)) duplicates.add(value);
    seen.add(value);
  }
  return [...duplicates];
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

function hasHighRiskEvidence(item) {
  const category = String(item.category ?? "");
  const priority = String(item.priority ?? "");
  return priority === "P0" || ["payment", "credential", "MDM", "nas", "ops160", "face_age", "third_party"].includes(category);
}

function requiredSubmissionFieldsFor(item) {
  const base = [
    "submission_id",
    "owner_group",
    "item_id",
    "owner_person",
    "submitted_at",
    "evidence_ids",
    "evidence_links",
    "last_test_time",
    "result",
    "reviewer",
    "redaction_confirmed",
  ];
  const category = String(item.category ?? "");
  if (["payment", "credential", "wecom", "third_party", "face_age", "ops160", "nas", "MDM"].includes(category)) base.push("credential_refs");
  if (["payment", "credential", "third_party", "ops160", "nas"].includes(category)) base.push("rollback_step");
  if (["credential", "nas", "MDM", "ops160"].includes(category)) base.push("next_retest_due");
  if (hasHighRiskEvidence(item)) base.push("failure_case_evidence", "approval_record");
  return unique(base);
}

function acceptedEvidenceTypesFor(item) {
  const category = String(item.category ?? "");
  if (category === "MDM") return ["console_export", "device_enroll_screenshot", "policy_export", "remote_lock_wipe_test_report", "platform_device_binding_record"];
  if (category === "payment") return ["merchant_opening_record", "callback_signature_doc", "refund_test_report", "reconciliation_sample", "finance_approval_record"];
  if (category === "credential") return ["credential_ref_record", "rotation_runbook", "expiry_reminder_record", "rotation_or_issuance_evidence", "rollback_test_record"];
  if (category === "nas") return ["automount_export", "scheduled_backup_log", "sha256_file", "restore_drill_report", "alert_delivery_record"];
  if (category === "face_age") return ["supplier_sdk_or_api_doc", "success_failure_case_report", "compliance_material", "procurement_approval_record", "operation_acceptance_record"];
  if (category === "wecom") return ["wecom_app_config_export", "directory_sync_report", "callback_config_export", "failure_alert_test", "credential_ref_record"];
  if (category === "third_party") return ["supplier_api_doc", "signature_sample", "webhook_sample", "field_mapping_sheet", "sandbox_test_report", "credential_ref_record"];
  if (category === "ops160") return ["system_config_redacted_export", "systemd_status_export", "health_check_log", "rollback_drill_report", "alert_delivery_record"];
  if (category === "security") return ["security_review_record", "auth_scheme_decision", "test_output", "approval_record"];
  return ["test_report", "approval_record", "evidence_link"];
}

function notAcceptedEvidenceFor(item) {
  const common = ["oral_confirmation", "mock_screenshot", "placeholder_file", "plaintext_secret", "unowned_screenshot"];
  if (hasHighRiskEvidence(item)) common.push("screenshot_only_without_export_or_test_report");
  return unique(common);
}

function makeSubmission(ownerGroup, item) {
  const ownerSlug = slug(ownerGroup);
  return {
    submission_id: `SUB-${ownerSlug}-${item.id}`,
    owner_group: ownerGroup,
    item_id: item.id,
    priority: item.priority,
    category: item.category,
    source_status: item.status,
    blocking_scope: item.blocking_scope,
    submission_status: "awaiting_real_evidence",
    packet_folder: `docs/production-readiness-evidence/submissions/${ownerSlug}/${item.id}/`,
    manifest_file_name: `manifest-${item.id}.json`,
    required_fields: requiredSubmissionFieldsFor(item),
    accepted_evidence_types: acceptedEvidenceTypesFor(item),
    not_accepted_evidence: notAcceptedEvidenceFor(item),
    required_evidence_summary: item.required_evidence ?? [],
    verification_commands_after_backfill: [
      "npm run docs:production-blockers",
      "npm run docs:production-owner-actions",
      "npm run docs:production-evidence-submission-kit",
      "npm run verify:production-evidence-submission-kit",
      "npm run verify:production-readiness-evidence",
      "npm run verify:delivery-gates",
    ],
  };
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

const source = readJson(sourcePath);
const submissions = [];
for (const pack of source.owner_action_packs ?? []) {
  for (const item of pack.items ?? []) {
    submissions.push(makeSubmission(pack.owner_group, item));
  }
}

const ownerSummaries = (source.owner_action_packs ?? []).map((pack) => {
  const ownerSubmissions = submissions.filter((submission) => submission.owner_group === pack.owner_group);
  return {
    owner_group: pack.owner_group,
    submission_count: ownerSubmissions.length,
    p0_count: ownerSubmissions.filter((submission) => submission.priority === "P0").length,
    packet_root: `docs/production-readiness-evidence/submissions/${slug(pack.owner_group)}/`,
    status: ownerSubmissions.length > 0 ? "awaiting_real_evidence" : "no_open_submission",
  };
});

const output = {
  kit_version: "v0.1",
  status: submissions.length > 0 ? "awaiting_real_evidence" : "no_open_submission",
  source_owner_action_pack_path: sourcePath,
  generated_from: [sourcePath],
  policy: {
    no_fake_closure: true,
    no_plaintext_secret: true,
    credential_ref_only: true,
    mock_is_not_production_evidence: true,
    submission_kit_does_not_close_blockers: true,
  },
  counts: {
    owner_groups: ownerSummaries.length,
    submission_templates: submissions.length,
    p0_submission_templates: submissions.filter((submission) => submission.priority === "P0").length,
  },
  required_manifest_schema: {
    required: [
      "submission_id",
      "item_id",
      "owner_group",
      "owner_person",
      "submitted_at",
      "evidence_ids",
      "evidence_links",
      "last_test_time",
      "result",
      "redaction_confirmed",
      "reviewer",
    ],
    result_allowed_values: ["passed", "failed", "partial", "blocked"],
    secret_policy: "only credential_ref is allowed; plaintext secrets are forbidden",
  },
  owner_summaries: ownerSummaries,
  submissions,
};

const ownerRows = ownerSummaries.map((summary) => ({
  owner_group: summary.owner_group,
  status: summary.status,
  submission_count: summary.submission_count,
  p0_count: summary.p0_count,
  packet_root: summary.packet_root,
}));

const submissionRows = submissions.map((submission) => ({
  submission_id: submission.submission_id,
  owner_group: submission.owner_group,
  item_id: submission.item_id,
  priority: submission.priority,
  category: submission.category,
  submission_status: submission.submission_status,
  packet_folder: submission.packet_folder,
  manifest_file_name: submission.manifest_file_name,
}));

const md = normalize(`# 生产证据提交包 V0.1\n\n## 1. 机器标记\n\n- PRODUCTION_EVIDENCE_SUBMISSION_KIT_READY\n- SUBMISSION_KIT_FROM_OWNER_ACTION_PACKS\n- AWAITING_REAL_PRODUCTION_EVIDENCE\n- CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET\n- VERIFY_PRODUCTION_EVIDENCE_SUBMISSION_KIT_READY\n\n## 2. 定位\n\n本文从 \`${sourcePath}\` 自动生成，把责任人动作包进一步转成可提交、可复核、可追溯的证据包结构。本文只定义提交格式，不改变生产前置台账、阻断清单或安全 findings 的状态。\n\n## 3. 总览\n\n| 指标 | 数量 |\n|---|---:|\n| 责任组 | ${output.counts.owner_groups} |\n| 证据提交模板 | ${output.counts.submission_templates} |\n| P0 证据提交模板 | ${output.counts.p0_submission_templates} |\n\n## 4. 责任组提交根目录\n\n${mdTable(ownerRows, [
  { key: "owner_group", label: "owner_group" },
  { key: "status", label: "status" },
  { key: "submission_count", label: "submission_count" },
  { key: "p0_count", label: "p0_count" },
  { key: "packet_root", label: "packet_root" },
])}\n## 5. 提交模板清单\n\n${mdTable(submissionRows, [
  { key: "submission_id", label: "submission_id" },
  { key: "owner_group", label: "owner_group" },
  { key: "item_id", label: "item_id" },
  { key: "priority", label: "priority" },
  { key: "category", label: "category" },
  { key: "submission_status", label: "submission_status" },
  { key: "packet_folder", label: "packet_folder" },
  { key: "manifest_file_name", label: "manifest_file_name" },
])}\n## 6. 单项 manifest 最低字段\n\n每个 \`manifest-<item_id>.json\` 至少包含：\n\n- submission_id\n- item_id\n- owner_group\n- owner_person\n- submitted_at\n- evidence_ids\n- evidence_links\n- last_test_time\n- result：只允许 passed、failed、partial、blocked\n- redaction_confirmed\n- reviewer\n- credential_refs：涉及账号、支付、企微、供应商、NAS、JWT、对象存储、数据库时必填；只写引用，不写密钥正文\n- rollback_step：涉及支付、凭证、供应商、160 配置、NAS 时必填\n- next_retest_due：涉及周期演练、凭证轮换、MDM、NAS、160 运行配置时必填\n\n## 7. 回填后必跑命令\n\n1. \`npm run docs:production-blockers\`\n2. \`npm run docs:production-owner-actions\`\n3. \`npm run docs:production-evidence-submission-kit\`\n4. \`npm run verify:production-evidence-submission-kit\`\n5. \`npm run verify:production-readiness-evidence\`\n6. \`npm run verify:delivery-gates\`\n\n## 8. 禁止事项\n\n1. 本提交包不能替代真实证据，不能把 \`awaiting_real_evidence\` 当作 passed。\n2. 不得提交明文密码、明文 token、API secret、私钥正文、支付密钥、人脸供应商密钥、企业微信密钥或 NAS SMB 密码。\n3. 只能提交 credential_ref、脱敏导出、工单、供应商回执、验收报告和机器命令输出。\n4. 只有 mock、口头确认、占位截图、无责任人截图、无测试时间截图，均不得关闭生产阻断。\n`);

const duplicateSubmissionIds = duplicateValues(output.submissions.map((submission) => submission.submission_id));
const duplicatePacketFolders = duplicateValues(output.submissions.map((submission) => submission.packet_folder));
if (duplicateSubmissionIds.length > 0 || duplicatePacketFolders.length > 0) {
  throw new Error(`submission kit generated duplicate ids or folders: ids=${duplicateSubmissionIds.join(", ")}; folders=${duplicatePacketFolders.join(", ")}`);
}

const json = normalize(JSON.stringify(output, null, 2));
forbidPlaintextSecrets(mdPath, md);
forbidPlaintextSecrets(jsonPath, json);

if (checkMode) {
  const failures = [];
  const missingOutputs = [jsonPath, mdPath].filter((relativePath) => !fs.existsSync(path.join(repoRoot, relativePath)));
  if (missingOutputs.length > 0) {
    console.error("PRODUCTION_EVIDENCE_SUBMISSION_KIT_FAIL");
    for (const relativePath of missingOutputs) console.error(`- ${relativePath} is missing; run npm run docs:production-evidence-submission-kit`);
    process.exit(1);
  }
  const currentJson = normalize(readText(jsonPath));
  const currentMd = normalize(readText(mdPath));
  if (currentJson !== json) failures.push(`${jsonPath} is not in sync`);
  if (currentMd !== md) failures.push(`${mdPath} is not in sync`);
  if (output.status !== "awaiting_real_evidence") failures.push("submission kit must stay awaiting_real_evidence while production blockers are open");
  if (output.counts.submission_templates < output.counts.owner_groups) failures.push("submission kit must include templates for owner groups");
  const duplicateSubmissionIds = duplicateValues(output.submissions.map((submission) => submission.submission_id));
  const duplicatePacketFolders = duplicateValues(output.submissions.map((submission) => submission.packet_folder));
  if (duplicateSubmissionIds.length > 0) failures.push(`submission_id values must be unique: ${duplicateSubmissionIds.join(", ")}`);
  if (duplicatePacketFolders.length > 0) failures.push(`packet_folder values must be unique: ${duplicatePacketFolders.join(", ")}`);
  if (failures.length > 0) {
    console.error("PRODUCTION_EVIDENCE_SUBMISSION_KIT_FAIL");
    for (const failure of failures) console.error(`- ${failure}`);
    process.exit(1);
  }
  console.log("PRODUCTION_EVIDENCE_SUBMISSION_KIT_PASS");
} else {
  fs.mkdirSync(path.dirname(path.join(repoRoot, jsonPath)), { recursive: true });
  fs.writeFileSync(path.join(repoRoot, jsonPath), json, "utf8");
  fs.writeFileSync(path.join(repoRoot, mdPath), md, "utf8");
  console.log(`generated ${jsonPath}`);
  console.log(`generated ${mdPath}`);
}