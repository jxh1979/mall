import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const repoRoot = process.cwd();
const marker = "PRODUCTION_BLOCKER_REGISTER_NO_GO_FIXTURES_READY";
const statusToolPath = "tools/generate-production-evidence-submission-status.mjs";
const currentStateToolPath = "tools/generate-closed-loop-current-state.mjs";
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const statusJsonPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const blockerRegisterPath = "docs/production-readiness-evidence/production-blocker-register-latest.json";
const currentStateJsonPath = "docs/closed-loop-current-state-snapshot-latest.json";

const requiredTempFiles = [
  statusToolPath,
  currentStateToolPath,
  kitPath,
  blockerRegisterPath,
  "docs/production-readiness-evidence/production-owner-action-packs-latest.json",
  "docs/production-readiness-evidence/nas-current-state-drift-latest.json",
  "docs/production-readiness-evidence/nas-diagnostic-latest.json",
  "docs/runtime-acceptance-evidence-latest.json",
  "docs/partial-requirement-mock-coverage-latest.json",
  "security-scans/semgrep-latest-summary.json",
  "docs/requirements-code-implementation-matrix-v0.1.csv",
  "docs/full-admin-mvp-requirements-code-matrix-v0.1.csv",
  "docs/full-admin-remaining-requirements-code-matrix-v0.1.csv",
  "apps/web/src/main.tsx",
  "apps/web/src/legacy-console/LegacyConsoleApp.tsx",
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
  if (!fs.existsSync(source)) throw new Error(`${relativePath} is required for blocker-register no-go fixture`);
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

function runNodeTool(tempRoot, relativePath) {
  const result = spawnSync(process.execPath, [relativePath], {
    cwd: tempRoot,
    encoding: "utf8",
  });
  if (result.status !== 0) {
    throw new Error(`${relativePath} failed:\nSTDOUT:\n${result.stdout}\nSTDERR:\n${result.stderr}`);
  }
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function safeId(value) {
  return String(value).replace(/[^A-Za-z0-9_.:-]/g, "-");
}

function manifestValue(field, template, safeSubmission) {
  if (field === "submission_id") return template.submission_id;
  if (field === "item_id") return template.item_id;
  if (field === "owner_group") return template.owner_group;
  if (field === "owner_person") return "Evidence Owner";
  if (field === "reviewer") return "Evidence Reviewer";
  if (field === "submitted_at") return "2026-06-16T10:00:00Z";
  if (field === "last_test_time") return "2026-06-16T09:30:00Z";
  if (field === "next_retest_due") return "2099-12-31";
  if (field === "redaction_confirmed") return true;
  if (field === "credential_refs") return [`cred://prod/evidence/${safeSubmission.toLowerCase()}/fixture-ref`];
  if (field === "evidence_ids") return [`EVD-${safeSubmission}-20260616-001`];
  if (field === "evidence_links") return [`ticket://ITSM-20260616-${safeSubmission}`];
  if (field === "failure_case_evidence") return [`ITSM-20260616-${safeSubmission}-FAILURE`];
  if (field === "approval_record") return `APPROVAL-20260616-${safeSubmission}`;
  if (field === "result") return "passed";
  return `DOC-${safeSubmission}-${field}`;
}

function writeValidManifest(tempRoot, template) {
  const safeSubmission = safeId(template.submission_id);
  const manifest = {};
  for (const field of template.required_fields ?? []) manifest[field] = manifestValue(field, template, safeSubmission);
  manifest.evidence_types = [template.accepted_evidence_types?.[0] ?? "console_export"];
  manifest.notes = "temporary blocker-register no-go fixture only";
  const manifestPath = path.join(tempRoot, `${template.packet_folder}${template.manifest_file_name}`);
  mkdirFor(manifestPath);
  fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, "utf8");
}

function patchNasReady(tempRoot) {
  const nasPath = path.join(tempRoot, "docs/production-readiness-evidence/nas-current-state-drift-latest.json");
  const nas = JSON.parse(fs.readFileSync(nasPath, "utf8"));
  nas.current_state = { ...(nas.current_state ?? {}), live_status: "passed" };
  nas.production_closure_assessment = {
    ...(nas.production_closure_assessment ?? {}),
    can_close_sec_p0_ops_recovery: true,
    reason: "temporary fixture simulates NAS ready",
  };
  fs.writeFileSync(nasPath, `${JSON.stringify(nas, null, 2)}\n`, "utf8");
}

function patchRequirementsComplete(tempRoot) {
  const csvPath = path.join(tempRoot, "docs/requirements-code-implementation-matrix-v0.1.csv");
  const completeStatus = String.fromCodePoint(0x5df2, 0x5b8c, 0x6574, 0x5b9e, 0x73b0);
  const script = [
    "import csv",
    "from pathlib import Path",
    `path = Path(${JSON.stringify(csvPath)})`,
    "rows = list(csv.reader(path.open(encoding='utf-8-sig', newline='')))",
    "for row in rows[1:]:",
    "    if len(row) > 8:",
    `        row[8] = ${JSON.stringify(completeStatus)}`,
    "with path.open('w', encoding='utf-8', newline='') as f:",
    "    csv.writer(f).writerows(rows)",
  ].join("\n");
  const result = spawnSync("python3", ["-c", script], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(`requirements csv patch failed: ${result.stderr || result.stdout}`);
}

function blockingTotal(counts) {
  return Number(counts.readiness_blockers ?? 0) + Number(counts.credential_blockers ?? 0) + Number(counts.finding_blockers ?? 0);
}

const kit = readJson(kitPath);
const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "complex-production-blocker-register-no-go-fixture-"));
try {
  for (const file of requiredTempFiles) copyFileToTemp(tempRoot, file);
  for (const template of kit.submissions ?? []) writeValidManifest(tempRoot, template);
  patchNasReady(tempRoot);
  patchRequirementsComplete(tempRoot);

  runNodeTool(tempRoot, statusToolPath);
  runNodeTool(tempRoot, currentStateToolPath);

  const statusJson = readTempJson(tempRoot, statusJsonPath);
  const blockerRegister = readTempJson(tempRoot, blockerRegisterPath);
  const currentStateJson = readTempJson(tempRoot, currentStateJsonPath);
  assert(currentStateJson.gates.owner_action_manifest_links?.items_without_manifest === 0, "closed-loop current state should keep owner action manifest links complete");
  const productionEvidence = currentStateJson.gates.production_evidence;
  const externalEvidence = currentStateJson.gates.external_evidence;
  const productionBlockers = currentStateJson.gates.production_blockers;
  const expectedBlockingTotal = blockingTotal(blockerRegister.counts ?? {});

  assert(statusJson.counts.valid_submission === kit.submissions.length, "all fixture manifests should be valid");
  assert(statusJson.counts.invalid_submission === 0, "fixture manifests must not be invalid");
  assert(statusJson.counts.awaiting_submission === 0, "fixture manifests should cover all submissions");
  assert(productionEvidence.complete === true, "production evidence gate should be complete in this fixture");
  assert(currentStateJson.gates.nas_current_state.can_close_sec_p0_ops_recovery === true, "NAS gate should be simulated as ready");
  assert(externalEvidence.complete === true, "external evidence gate should be simulated as complete");
  assert(expectedBlockingTotal > 0, "fixture must retain production blocker register blockers");
  assert(productionBlockers.blocking_total === expectedBlockingTotal, "current state should expose production blocker register total");
  assert(productionBlockers.complete === false, "production blocker register gate must remain incomplete");
  assert(currentStateJson.go_no_go.can_claim_production_ready === false, "open production blocker register items must block production ready claim");
  assert(
    (currentStateJson.go_no_go.production_ready_blockers ?? []).some((item) => item.includes("production blocker register items remain open")),
    "production ready blockers should include production blocker register gap",
  );

  console.log(`${marker}: valid=${statusJson.counts.valid_submission}, blocking_total=${productionBlockers.blocking_total}, production_ready=false`);
} finally {
  fs.rmSync(tempRoot, { recursive: true, force: true });
}
