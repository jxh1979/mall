import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const repoRoot = process.cwd();
const marker = "PRODUCTION_EVIDENCE_POSITIVE_PROPAGATION_FIXTURES_READY";
const statusToolPath = "tools/generate-production-evidence-submission-status.mjs";
const currentStateToolPath = "tools/generate-closed-loop-current-state.mjs";
const closureGapToolPath = "tools/generate-production-closure-gap-report.mjs";
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const statusJsonPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const closureGapPath = "docs/production-evidence-closure-gap-report-v0.1.md";
const currentStateJsonPath = "docs/closed-loop-current-state-snapshot-latest.json";

const requiredTempFiles = [
  statusToolPath,
  currentStateToolPath,
  closureGapToolPath,
  kitPath,
  "docs/production-readiness-evidence/production-blocker-register-latest.json",
  "docs/production-readiness-evidence/production-owner-action-packs-latest.json",
  "docs/production-readiness-evidence/production-evidence-backfill-priority-latest.json",
  "docs/production-readiness-evidence/production-evidence-backfill-execution-ledger-latest.json",
  "docs/production-readiness-evidence/nas-current-state-drift-latest.json",
  "docs/production-readiness-evidence/nas-diagnostic-latest.json",
  "docs/runtime-acceptance-evidence-latest.json",
  "docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json",
  "docs/partial-requirement-mock-coverage-latest.json",
  "security-scans/semgrep-latest-summary.json",
  "docs/requirements-code-implementation-matrix-v0.1.csv",
  "docs/full-admin-mvp-requirements-code-matrix-v0.1.csv",
  "docs/full-admin-remaining-requirements-code-matrix-v0.1.csv",
  "apps/web/src/main.tsx",
  "backend/src/main/java/com/complex/platform/ticketing/TicketingService.java",
  "backend/src/main/java/com/complex/platform/bar/BarService.java",
  "backend/src/main/java/com/complex/platform/bar/BarOrderService.java",
  "backend/src/main/java/com/complex/platform/bar/BarBillingService.java",
  "backend/src/main/java/com/complex/platform/bar/BarTableSessionService.java",
];

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function mkdirFor(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function copyFileToTemp(tempRoot, relativePath) {
  const source = absolute(relativePath);
  if (!fs.existsSync(source)) throw new Error(`${relativePath} is required for propagation fixture`);
  const target = path.join(tempRoot, relativePath);
  mkdirFor(target);
  fs.copyFileSync(source, target);
}

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, ""));
}

function readTempJson(tempRoot, relativePath) {
  return JSON.parse(fs.readFileSync(path.join(tempRoot, relativePath), "utf8").replace(/^\uFEFF/, ""));
}

function runNodeTool(tempRoot, relativePath, args = []) {
  const result = spawnSync(process.execPath, [relativePath, ...args], {
    cwd: tempRoot,
    encoding: "utf8",
  });
  if (result.status !== 0) {
    throw new Error(`${relativePath} failed:\nSTDOUT:\n${result.stdout}\nSTDERR:\n${result.stderr}`);
  }
  return result;
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

const kit = readJson(kitPath);
const template =
  (kit.submissions ?? []).find((item) => item.owner_group === "IT/\u8fd0\u7ef4/\u5b89\u5168" && item.item_id === "PRD-MDM-001") ??
  kit.submissions?.[0];
if (!template) throw new Error("submission kit has no fixture template");

const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "complex-evidence-propagation-fixture-"));
try {
  for (const file of requiredTempFiles) copyFileToTemp(tempRoot, file);

  const manifestPath = path.join(tempRoot, `${template.packet_folder}${template.manifest_file_name}`);
  mkdirFor(manifestPath);
  const manifest = {
    submission_id: template.submission_id,
    item_id: template.item_id,
    owner_group: template.owner_group,
    owner_person: "IT Ops Owner",
    submitted_at: "2026-06-16T10:00:00Z",
    evidence_ids: [`EVD-${template.item_id}-20260616-001`],
    evidence_links: ["ticket://ITSM-20260616-MDM-001"],
    evidence_types: [template.accepted_evidence_types?.[0] ?? "console_export"],
    last_test_time: "2026-06-16T09:30:00Z",
    result: "passed",
    reviewer: "Security Reviewer",
    redaction_confirmed: true,
    credential_refs: [`cred://prod/${template.category.toLowerCase()}/${template.item_id.toLowerCase()}/fixture-ref`],
    next_retest_due: "2099-12-31",
    failure_case_evidence: ["ITSM-20260616-MDM-FAILURE-001"],
    approval_record: "APPROVAL-20260616-MDM-001",
    rollback_step: "ROLLBACK-20260616-NAS-AUTOMOUNT-001",
    notes: "positive propagation fixture stored in temporary directory only; not production evidence",
  };
  fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, "utf8");

  runNodeTool(tempRoot, statusToolPath);
  runNodeTool(tempRoot, currentStateToolPath);
  runNodeTool(tempRoot, closureGapToolPath);

  const statusJson = readTempJson(tempRoot, statusJsonPath);
  const currentStateJson = readTempJson(tempRoot, currentStateJsonPath);
  const closureGapMarkdown = fs.readFileSync(path.join(tempRoot, closureGapPath), "utf8");
  const row = statusJson.submissions.find((item) => item.submission_id === template.submission_id);
  const expectedAwaiting = (kit.submissions.length ?? 0) - 1;

  assert(statusJson.counts.valid_submission === 1, "status generator should count one valid submission");
  assert(statusJson.counts.invalid_submission === 0, "status generator must not count invalid submissions");
  assert(statusJson.counts.awaiting_submission === expectedAwaiting, "all non-fixture submissions should remain awaiting");
  assert(row?.status === "valid_submission", "fixture row must propagate as valid_submission");
  assert(currentStateJson.gates.production_evidence.valid_submission === 1, "closed-loop current state should see one valid submission");
  assert(currentStateJson.gates.owner_action_manifest_links?.items_without_manifest === 0, "closed-loop current state should keep owner action manifest links complete");
  assert(currentStateJson.gates.production_evidence.awaiting_submission === expectedAwaiting, "closed-loop current state should preserve remaining awaiting submissions");
  assert(currentStateJson.gates.production_evidence.complete === false, "one valid manifest must not close production evidence gate");
  assert(currentStateJson.go_no_go.can_claim_production_ready === false, "positive fixture must not claim production readiness");
  assert(
    (currentStateJson.go_no_go.production_ready_blockers ?? []).some((item) => item.includes("production evidence submissions awaiting")),
    "closed-loop current state should still expose awaiting production evidence blocker",
  );
  assert(closureGapMarkdown.includes("| valid_submission | 1 |"), "closure gap report should show one valid submission");
  assert(closureGapMarkdown.includes(`| awaiting_submission | ${expectedAwaiting} |`), "closure gap report should preserve remaining awaiting count");
  assert(closureGapMarkdown.includes("| \u53ef\u5ba3\u79f0\u751f\u4ea7\u4e0a\u7ebf\u5c31\u7eea | \u5426 |"), "closure gap report must not claim production ready");

  console.log(`${marker}: item=${template.item_id}, valid=1, awaiting=${expectedAwaiting}, production_ready=false`);
} finally {
  fs.rmSync(tempRoot, { recursive: true, force: true });
}
