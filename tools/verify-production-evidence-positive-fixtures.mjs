import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const repoRoot = process.cwd();
const marker = "PRODUCTION_EVIDENCE_POSITIVE_FIXTURES_READY";
const statusToolPath = "tools/generate-production-evidence-submission-status.mjs";
const kitPath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const statusJsonPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const statusMdPath = "docs/production-evidence-submission-status-v0.1.md";

function absolute(relativePath) {
  return path.join(repoRoot, relativePath);
}

function mkdirFor(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function copyFileToTemp(tempRoot, relativePath) {
  const target = path.join(tempRoot, relativePath);
  mkdirFor(target);
  fs.copyFileSync(absolute(relativePath), target);
}

function readJson(relativePath) {
  return JSON.parse(fs.readFileSync(absolute(relativePath), "utf8").replace(/^\uFEFF/, ""));
}

function runStatusTool(tempRoot, args = []) {
  const result = spawnSync(process.execPath, [statusToolPath, ...args], {
    cwd: tempRoot,
    encoding: "utf8",
  });
  if (result.status !== 0) {
    throw new Error(`status tool failed: ${result.stderr || result.stdout}`);
  }
  return result;
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

const kit = readJson(kitPath);
const template = (kit.submissions ?? []).find((item) => item.owner_group === "IT/运维/安全" && item.item_id === "PRD-MDM-001") ?? kit.submissions?.[0];
if (!template) throw new Error("submission kit has no fixture template");

const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), "complex-evidence-positive-fixture-"));
try {
  copyFileToTemp(tempRoot, statusToolPath);
  copyFileToTemp(tempRoot, kitPath);

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
    notes: "positive fixture stored in temporary directory only; not production evidence",
  };
  fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, "utf8");

  runStatusTool(tempRoot);
  const statusJson = JSON.parse(fs.readFileSync(path.join(tempRoot, statusJsonPath), "utf8"));
  const statusMd = fs.readFileSync(path.join(tempRoot, statusMdPath), "utf8");
  const row = statusJson.submissions.find((item) => item.submission_id === template.submission_id);

  assert(statusJson.status !== "invalid_submissions_present", "one valid fixture must not produce invalid_submissions_present status");
  assert(statusJson.counts.valid_submission === 1, "positive fixture should count exactly one valid submission");
  assert(statusJson.counts.invalid_submission === 0, "positive fixture must not create invalid submissions");
  assert(statusJson.counts.awaiting_submission === (kit.submissions.length - 1), "all non-fixture submissions should remain awaiting");
  assert(row?.status === "valid_submission", "fixture row must be valid_submission");
  assert(Array.isArray(row.failures) && row.failures.length === 0, "fixture row must not have failures");
  assert(statusMd.includes("有效提交 | 1"), "markdown status should show one valid submission");

  const check = runStatusTool(tempRoot, ["--check"]);
  assert(check.stdout.includes("PRODUCTION_EVIDENCE_SUBMISSION_STATUS_PASS"), "--check should pass after generated positive fixture status");

  console.log(`${marker}: item=${template.item_id}, status=${row.status}`);
} finally {
  fs.rmSync(tempRoot, { recursive: true, force: true });
}
