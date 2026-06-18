import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";

const repoRoot = process.cwd();
const statusScript = path.join(repoRoot, "tools/generate-production-evidence-submission-status.mjs");
const kitRelativePath = "docs/production-readiness-evidence/production-evidence-submission-kit-latest.json";
const generatedStatusPath = "docs/production-readiness-evidence/production-evidence-submission-status-latest.json";
const failures = [];

function readJson(absolutePath) {
  return JSON.parse(fs.readFileSync(absolutePath, "utf8"));
}

function writeJson(absolutePath, value) {
  fs.mkdirSync(path.dirname(absolutePath), { recursive: true });
  fs.writeFileSync(absolutePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

function copyKit(tmpRoot) {
  const source = path.join(repoRoot, kitRelativePath);
  const target = path.join(tmpRoot, kitRelativePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(source, target);
}

function chooseTemplate(kit) {
  return kit.submissions.find((submission) => submission.item_id === "PRD-CRED-NAS-001" && submission.owner_group === "IT/运维/安全")
    ?? kit.submissions[0];
}

function defaultValue(field, template) {
  const itemId = template.item_id;
  const evidenceType = template.accepted_evidence_types?.[0] ?? "credential_ref_record";
  const values = {
    submission_id: template.submission_id,
    item_id: itemId,
    owner_group: template.owner_group,
    owner_person: "IT Owner A",
    submitted_at: "2026-06-16T10:00:00Z",
    evidence_ids: [`EVD-${itemId}-20260616-001`],
    evidence_links: [`ITSM-${itemId}-20260616-001`],
    evidence_types: [evidenceType],
    last_test_time: "2026-06-16T09:00:00Z",
    result: "passed",
    reviewer: "IT Reviewer B",
    redaction_confirmed: true,
    credential_refs: ["cred://prod/nas/smb/complex-backup-rw"],
    rollback_step: `执行 ${itemId} 已审批回滚 Runbook，禁止在 manifest 写入密钥正文。`,
    next_retest_due: "2026-12-31",
    failure_case_evidence: [`ITSM-FAIL-${itemId}-20260616-001`],
    approval_record: `APPROVAL-${itemId}-20260616-001`,
    notes: "negative fixture baseline",
  };
  if (Object.prototype.hasOwnProperty.call(values, field)) return values[field];
  if (/refs?|links?|ids?|types?|evidence/i.test(field)) return [`ITSM-${itemId}-${field}-20260616-001`];
  if (/confirmed|enabled|passed/i.test(field)) return true;
  if (/time|date|due/i.test(field)) return "2026-12-31";
  return `verified-${field}-${itemId}`;
}

function buildValidBaseline(template) {
  const manifest = {};
  for (const field of template.required_fields ?? []) {
    manifest[field] = defaultValue(field, template);
  }
  manifest.evidence_types = manifest.evidence_types ?? [template.accepted_evidence_types?.[0] ?? "credential_ref_record"];
  manifest.notes = manifest.notes ?? "negative fixture baseline";
  return manifest;
}

function runNode(args, cwd) {
  return spawnSync(process.execPath, args, {
    cwd,
    encoding: "utf8",
    env: { ...process.env },
  });
}

function runCase(testCase) {
  const tmpRoot = fs.mkdtempSync(path.join(os.tmpdir(), "complex-evidence-negative-"));
  try {
    copyKit(tmpRoot);
    const kit = readJson(path.join(tmpRoot, kitRelativePath));
    const template = chooseTemplate(kit);
    const manifest = buildValidBaseline(template);
    testCase.mutate(manifest, template);
    const manifestPath = path.join(tmpRoot, template.packet_folder, template.manifest_file_name);
    writeJson(manifestPath, manifest);

    const generate = runNode([statusScript], tmpRoot);
    if (generate.status !== 0) {
      failures.push(`${testCase.name}: status generator should produce a dashboard, got exit ${generate.status}: ${generate.stderr || generate.stdout}`);
      return;
    }

    const status = readJson(path.join(tmpRoot, generatedStatusPath));
    const row = status.submissions.find((submission) => submission.submission_id === template.submission_id);
    if (!row) {
      failures.push(`${testCase.name}: generated status did not include target submission ${template.submission_id}`);
      return;
    }
    if (row.status !== "invalid_submission") {
      failures.push(`${testCase.name}: expected invalid_submission, got ${row.status}`);
    }
    const rowFailures = (row.failures ?? []).join("\n");
    for (const expected of testCase.expectedFailures) {
      if (!rowFailures.includes(expected)) failures.push(`${testCase.name}: expected failure fragment not found: ${expected}`);
    }

    const check = runNode([statusScript, "--check"], tmpRoot);
    const checkOutput = `${check.stdout}\n${check.stderr}`;
    if (check.status === 0) failures.push(`${testCase.name}: --check must fail when invalid submissions are present`);
    if (!checkOutput.includes("invalid evidence submissions present")) {
      failures.push(`${testCase.name}: --check output must mention invalid evidence submissions present`);
    }
  } finally {
    fs.rmSync(tmpRoot, { recursive: true, force: true });
  }
}

const cases = [
  {
    name: "same owner and reviewer",
    mutate: (manifest) => {
      manifest.reviewer = manifest.owner_person;
    },
    expectedFailures: ["reviewer must be different from owner_person"],
  },
  {
    name: "local evidence path",
    mutate: (manifest) => {
      manifest.evidence_links = ["/mnt/complex-nas-backup/evidence.json"];
    },
    expectedFailures: ["evidence_links must not use local/dev path or localhost reference"],
  },
  {
    name: "non production credential ref",
    mutate: (manifest) => {
      manifest.credential_refs = ["cred://dev/nas/smb/complex-backup-rw"];
    },
    expectedFailures: ["credential_refs must use production cred://prod/... reference only"],
  },
  {
    name: "wrong item binding",
    mutate: (manifest) => {
      manifest.item_id = "PRD-CRED-DB-001";
    },
    expectedFailures: ["item_id must be PRD-CRED-NAS-001"],
  },
  {
    name: "sensitive query parameter",
    mutate: (manifest) => {
      manifest.evidence_links = ["https://ops.example.org/evidence?token=leaky"];
    },
    expectedFailures: ["evidence_links must not include sensitive URL query parameter(s): token"],
  },
  {
    name: "plaintext secret in notes",
    mutate: (manifest) => {
      manifest.notes = ["pass", "word=\\\"supersecret123\\\""].join("");
    },
    expectedFailures: ["contains apparent plaintext secret pattern"],
  },
  {
    name: "non passed result",
    mutate: (manifest) => {
      manifest.result = "partial";
    },
    expectedFailures: ["result must be passed to count as valid production evidence submission"],
  },
];

for (const testCase of cases) runCase(testCase);

if (failures.length > 0) {
  console.error("PRODUCTION_EVIDENCE_NEGATIVE_FIXTURES_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(`PRODUCTION_EVIDENCE_NEGATIVE_FIXTURES_PASS: ${cases.length} invalid manifest cases rejected`);
