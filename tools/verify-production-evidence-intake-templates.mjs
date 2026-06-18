import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const failures = [];

function readText(relativePath) {
  const file = path.join(repoRoot, relativePath);
  if (!fs.existsSync(file)) {
    failures.push(`${relativePath} is missing`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
}

function requireIncludes(relativePath, text, marker) {
  if (!text.includes(marker)) failures.push(`${relativePath} must include ${marker}`);
}

function forbidPlaintextSecrets(relativePath, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
    /\b(?:secret_key|secret)\s*[:=]\s*(?:\\*["'][^"'<\n\\]{8,}\\*["']|[A-Za-z0-9_./+=-]{12,})/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) failures.push(`${relativePath} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

const docPath = "docs/production-readiness-evidence-intake-template-v0.1.md";
const doc = readText(docPath);
forbidPlaintextSecrets(docPath, doc);

for (const marker of [
  "PRODUCTION_EVIDENCE_INTAKE_TEMPLATES_READY",
  "CREDENTIAL_REF_ONLY_NO_PLAINTEXT_SECRET",
  "MOCK_IS_NOT_PRODUCTION_EVIDENCE",
  "NO_SCREENSHOT_ONLY_FOR_HIGH_RISK_ITEMS",
  "VERIFY_PRODUCTION_EVIDENCE_INTAKE_READY",
  "PRD-MDM-001",
  "PRD-PAY-001",
  "PRD-PAY-002",
  "PRD-FACE-001",
  "PRD-NAS-AUTOMOUNT-001",
  "PRD-NAS-SCHEDULE-001",
  "PRD-NAS-ALERT-001",
  "PRD-NAS-DRILL-CADENCE-001",
  "PRD-CRED-001",
  "PRD-WECOM-001",
  "PRD-3P-001",
  "PRD-160-001",
  "credential_ref",
  "npm run verify:production-blockers",
  "npm run verify:production-readiness-evidence",
  "npm run verify:delivery-gates",
  "production-evidence-submission-kit-v0.1.md",
]) {
  requireIncludes(docPath, doc, marker);
}

const packageJson = JSON.parse(readText("package.json"));
if (packageJson.scripts?.["verify:production-evidence-intake"] !== "node tools/verify-production-evidence-intake-templates.mjs") {
  failures.push("package.json must expose verify:production-evidence-intake");
}
if (packageJson.scripts?.["verify:production-evidence-submission-kit"] !== "node tools/generate-production-evidence-submission-kit.mjs --check") {
  failures.push("package.json must expose verify:production-evidence-submission-kit");
}
if (!String(packageJson.scripts?.["verify:delivery-gates"] ?? "").includes("verify:production-evidence-intake")) {
  failures.push("verify:delivery-gates must include verify:production-evidence-intake");
}

const deliveryDoc = readText("docs/delivery-readiness-gates-v0.1.md");
requireIncludes("docs/delivery-readiness-gates-v0.1.md", deliveryDoc, "verify:production-evidence-intake");
const entry = readText("docs/00-current-effective-document-entry-v0.1.md");
requireIncludes("docs/00-current-effective-document-entry-v0.1.md", entry, "production-readiness-evidence-intake-template-v0.1.md");

if (failures.length > 0) {
  console.error("PRODUCTION_EVIDENCE_INTAKE_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("PRODUCTION_EVIDENCE_INTAKE_PASS");
