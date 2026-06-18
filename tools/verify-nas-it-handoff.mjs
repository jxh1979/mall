import { execFileSync } from "node:child_process";
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

function forbidRawSecretPatterns(relativePath, text) {
  const patterns = [
    /password\s*=/i,
    /pass\s*=/i,
    /token\s*=/i,
    /secret\s*=/i,
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
  ];
  for (const pattern of patterns) {
    if (pattern.test(text)) failures.push(`${relativePath} must not contain raw secret pattern ${pattern}`);
  }
}

function requireFileExists(relativePath) {
  if (!fs.existsSync(path.join(repoRoot, relativePath))) failures.push(`${relativePath} must exist`);
}

function requireScript(scriptName, expectedCommand) {
  const packageJson = readJson("package.json");
  if (!packageJson) return;
  const actual = packageJson.scripts?.[scriptName];
  if (!actual) failures.push(`package.json must expose ${scriptName}`);
  if (expectedCommand && actual !== expectedCommand) failures.push(`package.json script ${scriptName} must equal ${expectedCommand}`);
}

function requireSubmissionTemplate(itemId, expectedPath) {
  requireFileExists(expectedPath);
  const template = readJson(expectedPath);
  if (!template) return;
  if (template.item_id !== itemId) failures.push(`${expectedPath} item_id must be ${itemId}`);
  if (template.result !== "blocked") failures.push(`${expectedPath} template result must default to blocked`);
  if (template.template_notice !== "TEMPLATE_ONLY_NOT_PRODUCTION_EVIDENCE") failures.push(`${expectedPath} must be marked as template-only evidence`);
}

function requireSubmissionKitItem(itemId, expectedManifestPath) {
  const kit = readJson("docs/production-readiness-evidence/production-evidence-submission-kit-latest.json");
  if (!kit) return;
  const item = (kit.submissions ?? []).find((submission) => submission.item_id === itemId);
  if (!item) {
    failures.push(`production evidence submission kit must include ${itemId}`);
    return;
  }
  const actualManifestPath = `${item.packet_folder}${item.manifest_file_name}`;
  if (actualManifestPath !== expectedManifestPath) failures.push(`${itemId} target manifest path must be ${expectedManifestPath}, got ${actualManifestPath}`);
  if (item.submission_status !== "awaiting_real_evidence") failures.push(`${itemId} submission_status must remain awaiting_real_evidence until IT backfills real evidence`);
}


const docPath = "docs/nas-it-controlled-mount-handoff-v0.1.md";
const scriptPath = "ops/160/complex-nas-system-mount.example.sh";
const docText = requireIncludes(docPath, [
  "NAS_IT_CONTROLLED_MOUNT_HANDOFF_READY",
  "VERIFY_NAS_IT_HANDOFF_READY",
  "DEFAULT_DRY_RUN_NO_SYSTEM_WRITE",
  "NO_PLAINTEXT_SECRET_COLLECTION",
  "MANIFEST_EVIDENCE_MAPPING_READY",
  "SEC_P0_OPS_RECOVERY_REMAINS_BLOCKED_UNTIL_LIVE_PASS",
  "npm run verify:delivery-gates",
  "npm run docs:nas-current-state-drift",
  "npm run verify:nas-current-state-drift",
  "npm run docs:closed-loop-current-state",
  "npm run verify:closed-loop-current-state",
  "npm run verify:production-evidence-submissions-guard",
  "PRD-NAS-AUTOMOUNT-001",
  "PRD-NAS-SCHEDULE-001",
  "PRD-NAS-ALERT-001",
  "PRD-NAS-DRILL-CADENCE-001",
  "SEC-P0-OPS-RECOVERY-001",
  "template-SEC-P0-OPS-RECOVERY-001.json",
]);
const scriptText = requireIncludes(scriptPath, [
  "NAS_IT_CONTROLLED_MOUNT_EXAMPLE_READY",
  "EXAMPLE_ONLY_REQUIRES_IT_MAINTENANCE_WINDOW",
  "NO_PLAINTEXT_SECRET_IN_SCRIPT",
  "DEFAULT_DRY_RUN_NO_SYSTEM_WRITE",
  "MANIFEST_EVIDENCE_MAPPING_READY",
  "EXECUTE_MOUNT=\"${EXECUTE_MOUNT:-false}\"",
  "credentials=$CREDENTIALS_FILE",
  "NAS_IT_CONTROLLED_MOUNT_DRY_RUN",
  "NAS_IT_CONTROLLED_MOUNT_PASS",
  "cred://prod/nas/smb/complex-backup-rw",
]);
forbidRawSecretPatterns(docPath, docText);
forbidRawSecretPatterns(scriptPath, scriptText.replace("credentials=$CREDENTIALS_FILE", "credentials=REDACTED"));

try {
  execFileSync("bash", ["-n", scriptPath], { cwd: repoRoot, stdio: "pipe" });
} catch (error) {
  failures.push(`${scriptPath} must pass bash -n: ${String(error.stderr ?? error.message)}`);
}


const requiredFiles = [
  "ops/160/complex-nas-preflight.sh",
  "ops/160/complex-backup.sh",
  "ops/160/complex-restore-drill.sh",
  "ops/160/complex-backup-schedule.example",
  "ops/160/complex-backup-alert.example",
  "docs/nas-current-state-drift-report-v0.1.md",
  "docs/production-readiness-evidence/nas-current-state-drift-latest.json",
  "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json",
  "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-SCHEDULE-001.json",
  "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json",
  "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json",
  "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json",
];
for (const requiredFile of requiredFiles) requireFileExists(requiredFile);

for (const [itemId, templatePath, targetManifestPath] of [
  ["PRD-NAS-AUTOMOUNT-001", "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-AUTOMOUNT-001.json", "docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-AUTOMOUNT-001/manifest-PRD-NAS-AUTOMOUNT-001.json"],
  ["PRD-NAS-SCHEDULE-001", "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-SCHEDULE-001.json", "docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-SCHEDULE-001/manifest-PRD-NAS-SCHEDULE-001.json"],
  ["PRD-NAS-ALERT-001", "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-ALERT-001.json", "docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-ALERT-001/manifest-PRD-NAS-ALERT-001.json"],
  ["PRD-NAS-DRILL-CADENCE-001", "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-PRD-NAS-DRILL-CADENCE-001.json", "docs/production-readiness-evidence/submissions/it-ops-security/PRD-NAS-DRILL-CADENCE-001/manifest-PRD-NAS-DRILL-CADENCE-001.json"],
  ["SEC-P0-OPS-RECOVERY-001", "docs/production-readiness-evidence/submission-manifest-templates/it-ops-security/template-SEC-P0-OPS-RECOVERY-001.json", "docs/production-readiness-evidence/submissions/it-ops-security/SEC-P0-OPS-RECOVERY-001/manifest-SEC-P0-OPS-RECOVERY-001.json"],
]) {
  requireSubmissionTemplate(itemId, templatePath);
  requireSubmissionKitItem(itemId, targetManifestPath);
}

for (const command of ["bash", "node", "npm"]) {
  try {
    execFileSync("bash", ["-lc", `command -v ${command}`], { cwd: repoRoot, stdio: "pipe" });
  } catch {
    failures.push(`${command} must be available for NAS handoff verification`);
  }
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  if (scripts["verify:nas-it-handoff"] !== "node tools/verify-nas-it-handoff.mjs") failures.push("package.json must expose verify:nas-it-handoff");
  if (!scripts["verify:delivery-gates"]?.includes("verify:nas-it-handoff")) failures.push("verify:delivery-gates must include verify:nas-it-handoff");
  for (const scriptName of [
    "docs:nas-current-state-drift",
    "verify:nas-current-state-drift",
    "docs:closed-loop-current-state",
    "verify:closed-loop-current-state",
    "verify:production-evidence-submissions-guard",
    "verify:delivery-gates",
  ]) {
    if (!scripts[scriptName]) failures.push(`package.json must expose ${scriptName}`);
  }
}

requireIncludes("docs/delivery-readiness-gates-v0.1.md", ["NAS_IT_CONTROLLED_MOUNT_HANDOFF_READY", "VERIFY_NAS_IT_HANDOFF_READY", "verify:nas-it-handoff"]);
requireIncludes("docs/00-current-effective-document-entry-v0.1.md", ["docs/nas-it-controlled-mount-handoff-v0.1.md", "ops/160/complex-nas-system-mount.example.sh", "npm run verify:nas-it-handoff"]);
requireIncludes("docs/delivery-package-manifest-v0.1.md", ["nas-it-controlled-mount-handoff-v0.1.md", "ops/160/complex-nas-system-mount.example.sh", "verify:nas-it-handoff"]);

if (failures.length > 0) {
  console.error("NAS_IT_HANDOFF_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("NAS_IT_HANDOFF_PASS");
