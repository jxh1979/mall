import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const failures = [];

function readText(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) {
    failures.push(`${relativePath} is missing`);
    return "";
  }
  return fs.readFileSync(absolutePath, "utf8").replace(/^\uFEFF/, "");
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

function requireIncludes(relativePath, markers) {
  const text = readText(relativePath);
  for (const marker of markers) {
    if (!text.includes(marker)) failures.push(`${relativePath} must include marker: ${marker}`);
  }
  return text;
}

function escapeRegex(text) {
  return String(text).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function requireRegex(relativePath, pattern, description) {
  const text = readText(relativePath);
  if (!pattern.test(text)) failures.push(`${relativePath} must include ${description}`);
}

const closureReportPath = "docs/production-evidence-closure-gap-report-v0.1.md";
const existingGapReportPath = "docs/production-existing-evidence-to-manifest-gap-audit-v0.1.md";
const nasDriftReportPath = "docs/nas-current-state-drift-report-v0.1.md";
const status = readJson("docs/production-readiness-evidence/production-evidence-submission-status-latest.json");
const blockers = readJson("docs/production-readiness-evidence/production-blocker-register-latest.json");
const priority = readJson("docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json");
const ledger = readJson("docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json");
const nasDrift = readJson("docs/production-readiness-evidence/nas-current-state-drift-latest.json");

requireIncludes(closureReportPath, [
  "PRODUCTION_CLOSURE_GAP_REPORT_READY",
  "GENERATED_FROM_PRODUCTION_BLOCKER_REGISTER",
  "GENERATED_FROM_PRODUCTION_EVIDENCE_SUBMISSION_STATUS",
  "NO_FAKE_PRODUCTION_EVIDENCE_CLOSURE",
  "MANIFEST_FIRST_RAW_EVIDENCE_EXTERNAL",
  "P0_EVIDENCE_FIRST_CLOSURE_ORDER",
]);

requireIncludes(existingGapReportPath, [
  "EXISTING_EVIDENCE_TO_MANIFEST_GAP_AUDIT_READY",
  "NO_FAKE_PRODUCTION_MANIFEST_CLOSURE",
  "EXISTING_NAS_EVIDENCE_PARTIAL_ONLY",
  "IT_OWNER_REVIEWER_APPROVAL_REQUIRED",
  "SYSTEM_LEVEL_HARDENING_EVIDENCE_REQUIRED",
]);

requireIncludes(nasDriftReportPath, [
  "NAS_CURRENT_STATE_DRIFT_REPORT_READY",
  "HISTORICAL_NAS_EVIDENCE_NOT_CURRENT_CLOSURE",
  "NO_FAKE_NAS_PRODUCTION_MANIFEST",
  "NAS_MANIFEST_REMAINS_AWAITING_IT_EVIDENCE",
]);

requireIncludes("docs/00-current-effective-document-entry-v0.1.md", [
  closureReportPath,
  existingGapReportPath,
]);

requireIncludes("docs/delivery-package-manifest-v0.1.md", [
  closureReportPath.replace("docs/", ""),
  existingGapReportPath.replace("docs/", ""),
]);


if (nasDrift) {
  if (!nasDrift.report_policy?.does_not_close_manifest) failures.push("NAS drift report must not close manifest");
  if (!nasDrift.report_policy?.historical_passed_evidence_is_not_current_production_closure) failures.push("NAS drift report must mark historical evidence as non-current closure");
  if (!nasDrift.production_closure_assessment || nasDrift.production_closure_assessment.can_close_prd_nas_automount !== false) failures.push("NAS drift report must keep PRD-NAS-AUTOMOUNT-001 open");
}

if (status && blockers && priority && ledger) {
  const counts = status.counts ?? {};
  const blockerCounts = blockers.counts ?? {};
  const priorityCounts = priority.counts ?? {};
  const ledgerCounts = ledger.counts ?? {};
  const expectedTableRows = [
    ["生产前置台账阻断项", blockerCounts.readiness_blockers],
    ["生产凭证轮换阻断项", blockerCounts.credential_blockers],
    ["安全 findings open / open_for_prod_review", blockerCounts.finding_blockers],
    ["预期生产证据 manifest", counts.expected_submissions],
    ["P0 manifest", (counts.p0_awaiting_submission ?? 0) + (counts.p0_valid_submission ?? 0) + (counts.p0_invalid_submission ?? 0)],
    ["P1 manifest", (status.submissions ?? []).filter((row) => row.priority === "P1").length],
    ["P2 manifest", (status.submissions ?? []).filter((row) => row.priority === "P2").length],
    ["awaiting_submission", counts.awaiting_submission],
    ["valid_submission", counts.valid_submission],
    ["invalid_submission", counts.invalid_submission],
    ["回填执行台账行数", ledgerCounts.ledger_rows],
    ["回填批次数", (priority.batches ?? []).length],
  ];
  for (const [label, value] of expectedTableRows) {
    requireRegex(closureReportPath, new RegExp(`\\|\\s*${escapeRegex(label)}\\s*\\|\\s*${escapeRegex(value)}\\s*\\|`), `${label} count ${value}`);
  }
  if ((priorityCounts.p0_awaiting ?? 0) !== (counts.p0_awaiting_submission ?? 0)) {
    failures.push("priority p0_awaiting must match submission status p0_awaiting_submission");
  }

  const itOpsRows = new Map((status.submissions ?? [])
    .filter((row) => row.owner_group === "IT/运维/安全")
    .map((row) => [row.item_id, row.status]));
  for (const itemId of [
    "PRD-NAS-AUTOMOUNT-001",
    "PRD-NAS-SCHEDULE-001",
    "PRD-NAS-DRILL-CADENCE-001",
    "PRD-160-001",
    "SEC-P0-OPS-RECOVERY-001",
  ]) {
    const itemStatus = itOpsRows.get(itemId);
    if (!itemStatus) {
      failures.push(`${itemId} must exist in IT/运维/安全 submission status`);
      continue;
    }
    requireRegex(existingGapReportPath, new RegExp(`\\|\\s*${escapeRegex(itemId)}\\s*\\|\\s*${escapeRegex(itemStatus)}\\s*\\|`), `${itemId} current status ${itemStatus}`);
  }
}

if (failures.length > 0) {
  console.error("PRODUCTION_CLOSURE_GAP_REPORTS_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("PRODUCTION_CLOSURE_GAP_REPORTS_PASS");
