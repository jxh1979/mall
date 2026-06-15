import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const failures = [];
const operationMethods = new Set(["get", "post", "put", "patch", "delete", "head", "options"]);

function readText(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) {
    failures.push(`${relativePath} is missing`);
    return "";
  }
  return fs.readFileSync(absolutePath, "utf8");
}

function readJson(relativePath) {
  const text = readText(relativePath);
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (error) {
    failures.push(`${relativePath} is not valid JSON: ${error.message}`);
    return null;
  }
}

function operationCount(openapi) {
  let count = 0;
  for (const pathItem of Object.values(openapi.paths ?? {})) {
    for (const method of Object.keys(pathItem ?? {})) {
      if (operationMethods.has(method)) count += 1;
    }
  }
  return count;
}

function requireIncludes(relativePath, text, marker) {
  if (!text.includes(marker)) failures.push(`${relativePath} must include ${marker}`);
}

function requireTableLineIncludes(relativePath, text, itemId, markers) {
  const line = text.split(/\r?\n/).find((candidate) => candidate.includes(`| ${itemId} |`));
  if (!line) {
    failures.push(`${relativePath} must include table row for ${itemId}`);
    return "";
  }
  for (const marker of markers) {
    if (!line.includes(marker)) failures.push(`${relativePath} row ${itemId} must include ${marker}`);
  }
  return line;
}

function forbidPlaintextSecrets(label, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:P0_ADMIN_PASSWORD|POSTGRES_PASSWORD|MINIO_SECRET_KEY|MINIO_ROOT_PASSWORD|AWS_SECRET_ACCESS_KEY|ALIYUN_ACCESS_KEY_SECRET|API_SECRET|SECRET_KEY)\s*[:=]\s*["']?[A-Za-z0-9_./+=-]{8,}/i,
    /\b(?:password|token)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
    /\b(?:secret|secret_key)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) failures.push(`${label} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  if (scripts["verify:production-readiness-evidence"] !== "node tools/verify-production-readiness-evidence.mjs") {
    failures.push("package.json must expose verify:production-readiness-evidence");
  }
  if (!String(scripts["verify:delivery-gates"] ?? "").includes("verify:production-readiness-evidence")) {
    failures.push("verify:delivery-gates must include verify:production-readiness-evidence");
  }
}

const openapi = readJson("docs/openapi/openapi-p0.json");
const expectedOperationCount = openapi ? operationCount(openapi) : 0;
const baselineText = readText("security-scans/formal-security-baseline-v1.md");
const findingsText = readText("security-scans/findings-register-v1.md");
const ledgerText = readText("docs/production-readiness-implementation-ledger-template-v0.1.md");
const backfillText = readText("docs/production-readiness-evidence/production-readiness-evidence-backfill-20260615.md");
const nasEvidenceText = readText("docs/production-readiness-evidence/nas-smb-mount-latest.json");
const securityEvidenceText = readText("docs/production-readiness-evidence/security-baseline-latest.json");
const securityEvidence = securityEvidenceText ? JSON.parse(securityEvidenceText) : null;
const semgrepEvidence = readJson("security-scans/delivery-readiness-semgrep-20260615.json");
const productionBlockerRegister = readJson("docs/production-readiness-evidence/production-blocker-register-latest.json");
const productionOwnerActionPacks = readJson("docs/production-readiness-evidence/production-owner-action-packs-latest.json");
const productionEvidenceSubmissionKit = readJson("docs/production-readiness-evidence/production-evidence-submission-kit-latest.json");
const productionEvidenceSubmissionStatus = readJson("docs/production-readiness-evidence/production-evidence-submission-status-latest.json");
const productionEvidenceManifestTemplates = readJson("docs/production-readiness-evidence/production-evidence-manifest-template-index-latest.json");

forbidPlaintextSecrets("production readiness ledger", ledgerText);
forbidPlaintextSecrets("production readiness backfill", backfillText);
forbidPlaintextSecrets("security baseline evidence", securityEvidenceText);
forbidPlaintextSecrets("nas smb evidence", nasEvidenceText);

if (securityEvidence) {
  const expected = {
    status: "passed",
    evidence_type: "formal_security_baseline_v1",
    verification_command: "npm run verify:security-baseline",
    baseline_path: "security-scans/formal-security-baseline-v1.md",
    findings_register_path: "security-scans/findings-register-v1.md",
    plaintext_secret_present: false,
  };
  for (const [field, value] of Object.entries(expected)) {
    if (securityEvidence[field] !== value) failures.push(`security evidence ${field} must be ${value}, got ${securityEvidence[field]}`);
  }
  if (securityEvidence.openapi_operation_count !== expectedOperationCount) {
    failures.push(`security evidence openapi_operation_count must be ${expectedOperationCount}, got ${securityEvidence.openapi_operation_count}`);
  }
}

if (productionBlockerRegister) {
  if (productionBlockerRegister.status !== "open_blockers_present") {
    failures.push(`production blocker register status must be open_blockers_present, got ${productionBlockerRegister.status}`);
  }
  for (const [field, minimum] of [["readiness_blockers", 1], ["credential_blockers", 1], ["finding_blockers", 1]]) {
    const items = Array.isArray(productionBlockerRegister[field]) ? productionBlockerRegister[field] : [];
    if (items.length < minimum) failures.push(`production blocker register ${field} must contain current unresolved blockers`);
  }
}

if (productionOwnerActionPacks) {
  if (productionOwnerActionPacks.status !== "open_actions_present") {
    failures.push(`production owner action packs status must be open_actions_present, got ${productionOwnerActionPacks.status}`);
  }
  if (productionOwnerActionPacks.source_register_path !== "docs/production-readiness-evidence/production-blocker-register-latest.json") {
    failures.push("production owner action packs must be generated from production blocker register");
  }
  const packs = Array.isArray(productionOwnerActionPacks.owner_action_packs) ? productionOwnerActionPacks.owner_action_packs : [];
  if (packs.length < 1) failures.push("production owner action packs must contain at least one owner pack while blockers are open");
}

if (productionEvidenceSubmissionKit) {
  if (productionEvidenceSubmissionKit.status !== "awaiting_real_evidence") {
    failures.push(`production evidence submission kit status must be awaiting_real_evidence, got ${productionEvidenceSubmissionKit.status}`);
  }
  if (productionEvidenceSubmissionKit.source_owner_action_pack_path !== "docs/production-readiness-evidence/production-owner-action-packs-latest.json") {
    failures.push("production evidence submission kit must be generated from owner action packs");
  }
  const submissions = Array.isArray(productionEvidenceSubmissionKit.submissions) ? productionEvidenceSubmissionKit.submissions : [];
  if (submissions.length < 1) failures.push("production evidence submission kit must contain submission templates while blockers are open");
}

if (productionEvidenceManifestTemplates) {
  if (productionEvidenceManifestTemplates.status !== "templates_ready_not_real_evidence") {
    failures.push(`production evidence manifest templates status must be templates_ready_not_real_evidence, got ${productionEvidenceManifestTemplates.status}`);
  }
  if (productionEvidenceManifestTemplates.source_submission_kit_path !== "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json") {
    failures.push("production evidence manifest templates must be generated from submission kit");
  }
  if ((productionEvidenceManifestTemplates.counts?.templates ?? 0) < 1) {
    failures.push("production evidence manifest templates must include template files while blockers are open");
  }
}

if (productionEvidenceSubmissionStatus) {
  if (!["awaiting_real_evidence", "partial_real_evidence_submitted"].includes(productionEvidenceSubmissionStatus.status)) {
    failures.push(`production evidence submission status must not contain invalid submissions, got ${productionEvidenceSubmissionStatus.status}`);
  }
  if (productionEvidenceSubmissionStatus.source_submission_kit_path !== "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json") {
    failures.push("production evidence submission status must be generated from submission kit");
  }
  if ((productionEvidenceSubmissionStatus.counts?.invalid_submission ?? 0) !== 0) {
    failures.push("production evidence submission status must have 0 invalid submissions");
  }
}

if (semgrepEvidence) {
  const results = Array.isArray(semgrepEvidence.results) ? semgrepEvidence.results : [];
  const errors = Array.isArray(semgrepEvidence.errors) ? semgrepEvidence.errors : [];
  const scanned = Array.isArray(semgrepEvidence.paths?.scanned) ? semgrepEvidence.paths.scanned : [];
  if (results.length !== 0) failures.push(`delivery readiness semgrep must have 0 findings, got ${results.length}`);
  if (errors.length !== 0) failures.push(`delivery readiness semgrep must have 0 errors, got ${errors.length}`);
  for (const target of ["tools/generate-production-blocker-register.mjs", "tools/generate-production-owner-action-packs.mjs", "tools/generate-production-evidence-submission-kit.mjs", "tools/generate-production-evidence-manifest-templates.mjs", "tools/generate-production-evidence-submission-status.mjs", "tools/verify-delivery-gates.mjs", "tools/verify-nas-smb-mount-evidence.mjs", "tools/verify-production-evidence-intake-templates.mjs", "tools/verify-production-readiness-evidence.mjs"]) {
    if (!scanned.includes(target)) failures.push(`delivery readiness semgrep must scan ${target}`);
  }
}

for (const [relativePath, text, markers] of [
  ["security-scans/formal-security-baseline-v1.md", baselineText, ["正式安全基线 V1", `OpenAPI operation count: ${expectedOperationCount}`, "Verification command: `npm run verify:security-baseline`"]],
  ["security-scans/findings-register-v1.md", findingsText, ["Findings Register V1", `OpenAPI operation count: ${expectedOperationCount}`, "SEC-P0-MDM-DEVICE-001", "SEC-P0-OPS-RECOVERY-001"]],
  ["docs/production-readiness-implementation-ledger-template-v0.1.md", ledgerText, ["| PRD-SEC-001 | security | 正式安全基线 |", "| PRD-SEC-001 | security | 正式安全基线 | V1 已生成；后续 OpenAPI、认证、权限、第三方、支付、MDM、生产凭证或 160 运维证据变化后必须重生成 | 安全/技术负责人 | 自管/第三方 | passed |", "security-scans/formal-security-baseline-v1.md", "security-scans/findings-register-v1.md"]],
  ["docs/production-readiness-evidence/production-readiness-evidence-backfill-20260615.md", backfillText, ["PRD-SEC-001", "security-baseline-latest.json", "verify:production-readiness-evidence", "delivery-readiness-semgrep-20260615.json", "production-blocker-register-latest.json", "verify:production-blockers", "production-owner-action-packs-latest.json", "verify:production-owner-actions", "production-evidence-submission-kit-latest.json", "verify:production-evidence-submission-kit", "production-evidence-manifest-template-index-latest.json", "verify:production-evidence-manifest-templates", "production-evidence-submission-status-latest.json", "verify:production-evidence-submission-status"]],
  ["docs/production-blocker-register-v0.1.md", readText("docs/production-blocker-register-v0.1.md"), ["PRODUCTION_BLOCKER_REGISTER_READY", "PRODUCTION_BLOCKERS_OPEN", "NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE"]],
  ["docs/production-readiness-evidence/nas-smb-mount-latest.json", nasEvidenceText, ["nas_smb_mount_live_rw", "plaintext_secret_present", "false"]],
]) {
  for (const marker of markers) requireIncludes(relativePath, text, marker);
}

requireTableLineIncludes("docs/production-readiness-implementation-ledger-template-v0.1.md", ledgerText, "PRD-SEC-001", [
  "passed",
  "security-scans/formal-security-baseline-v1.md",
  "security-scans/findings-register-v1.md",
  "security-baseline-latest.json",
]);

for (const [itemId, reason] of [
  ["PRD-MDM-001", "real MDM evidence"],
  ["PRD-PAY-001", "real payment test merchant evidence"],
  ["PRD-PAY-002", "real payment production merchant and refund evidence"],
  ["PRD-FACE-001", "real face or age verification supplier evidence"],
  ["PRD-WECOM-001", "real WeCom evidence"],
  ["PRD-3P-001", "real supplier evidence"],
  ["PRD-CRED-001", "real production credential issuance and rotation evidence"],
]) {
  const line = requireTableLineIncludes("docs/production-readiness-implementation-ledger-template-v0.1.md", ledgerText, itemId, ["pending_info"]);
  if (line.includes("passed")) failures.push(`${itemId} must not be passed until ${reason} is available`);
}

if (failures.length > 0) {
  console.error("PRODUCTION_READINESS_EVIDENCE_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("PRODUCTION_READINESS_EVIDENCE_PASS");