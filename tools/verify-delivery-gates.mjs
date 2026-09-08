// 文件版本：V0.01
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

function requireIncludes(relativePath, markers) {
  const text = readText(relativePath);
  for (const marker of markers) {
    if (!text.includes(marker)) failures.push(`${relativePath} must include marker: ${marker}`);
  }
  return text;
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  const docsScript = scripts["verify:delivery-gates-docs"];
  const deliveryScript = scripts["verify:delivery-gates"];
  if (docsScript !== "node tools/verify-delivery-gates.mjs") {
    failures.push("package.json must expose verify:delivery-gates-docs");
  }
  if (scripts["docs:production-closure-gap-reports"] !== "node tools/generate-production-closure-gap-report.mjs") {
    failures.push("package.json must expose docs:production-closure-gap-reports");
  }
  if (scripts["docs:ui-experience-acceptance"] !== "node tools/generate-ui-experience-acceptance-report.mjs") {
    failures.push("package.json must expose docs:ui-experience-acceptance");
  }
  if (scripts["verify:ui-experience-acceptance"] !== "node tools/generate-ui-experience-acceptance-report.mjs --check") {
    failures.push("package.json must expose verify:ui-experience-acceptance");
  }
  if (scripts["docs:production-evidence-owner-execution-packets"] !== "node tools/generate-production-evidence-owner-execution-packets.mjs") {
    failures.push("package.json must expose docs:production-evidence-owner-execution-packets");
  }
  if (scripts["verify:production-evidence-owner-execution-packets"] !== "node tools/generate-production-evidence-owner-execution-packets.mjs --check") {
    failures.push("package.json must expose verify:production-evidence-owner-execution-packets");
  }
  if (scripts["docs:production-no-go-command-center"] !== "node tools/generate-production-no-go-resolution-command-center.mjs") {
    failures.push("package.json must expose docs:production-no-go-command-center");
  }
  if (scripts["verify:production-no-go-command-center"] !== "node tools/generate-production-no-go-resolution-command-center.mjs --check") {
    failures.push("package.json must expose verify:production-no-go-command-center");
  }
  if (scripts["verify:production-evidence-crosslinks"] !== "node tools/verify-production-evidence-crosslinks.mjs") {
    failures.push("package.json must expose verify:production-evidence-crosslinks");
  }
  if (scripts["docs:partial-requirement-mock-coverage"] !== "node tools/generate-partial-requirement-mock-coverage.mjs") {
    failures.push("package.json must expose docs:partial-requirement-mock-coverage");
  }
  if (scripts["verify:partial-requirement-mock-coverage"] !== "node tools/generate-partial-requirement-mock-coverage.mjs --check") {
    failures.push("package.json must expose verify:partial-requirement-mock-coverage");
  }
  if (scripts["prepare:production-evidence-drafts"] !== "node tools/prepare-production-evidence-manifest-drafts.mjs") {
    failures.push("package.json must expose prepare:production-evidence-drafts");
  }
  if (scripts["verify:production-evidence-draft-prep"] !== "node tools/prepare-production-evidence-manifest-drafts.mjs --check") {
    failures.push("package.json must expose verify:production-evidence-draft-prep");
  }
  if (scripts["verify:production-evidence-draft-prep-fixtures"] !== "node tools/verify-production-evidence-draft-prep-fixtures.mjs") {
    failures.push("package.json must expose verify:production-evidence-draft-prep-fixtures");
  }
  if (scripts["verify:production-evidence-positive-fixtures"] !== "node tools/verify-production-evidence-positive-fixtures.mjs") {
    failures.push("package.json must expose verify:production-evidence-positive-fixtures");
  }
  if (scripts["verify:production-evidence-positive-propagation-fixtures"] !== "node tools/verify-production-evidence-positive-propagation-fixtures.mjs") {
    failures.push("package.json must expose verify:production-evidence-positive-propagation-fixtures");
  }
  if (scripts["verify:production-ready-no-go-fixtures"] !== "node tools/verify-production-ready-no-go-fixtures.mjs") {
    failures.push("package.json must expose verify:production-ready-no-go-fixtures");
  }
  if (scripts["verify:production-blocker-register-no-go-fixtures"] !== "node tools/verify-production-blocker-register-no-go-fixtures.mjs") {
    failures.push("package.json must expose verify:production-blocker-register-no-go-fixtures");
  }
  if (scripts["verify:production-ready-positive-fixtures"] !== "node tools/verify-production-ready-positive-fixtures.mjs") {
    failures.push("package.json must expose verify:production-ready-positive-fixtures");
  }
  const requiredDeliveryCommands = [
    "verify:delivery-gates-docs",
    "verify:full-admin-matrix",
    "verify:domain-ownership-scope",
    "verify:unit",
    "verify:coding-host-protection",
    "verify:external-dependency-gates",
    "verify:external-evidence-requirement-traceability",
    "verify:partial-requirement-mock-coverage",
    "verify:t3-coding-package",
    "verify:t3b-supplier-mock-kit",
    "verify:t3d-local-llm-ai-quality",
    "verify:p0-contract",
    "verify:security-baseline",
    "verify:production-readiness-evidence",
    "verify:production-blockers",
    "verify:production-owner-actions",
    "verify:production-evidence-submission-kit",
    "verify:production-evidence-manifest-templates",
    "verify:production-evidence-owner-guides",
    "verify:production-evidence-submission-status",
    "verify:production-evidence-submissions-guard",
    "verify:production-evidence-negative-fixtures",
    "verify:production-evidence-positive-fixtures",
  "verify:production-evidence-positive-propagation-fixtures",
  "verify:production-ready-no-go-fixtures",
  "verify:production-blocker-register-no-go-fixtures",
  "verify:production-ready-positive-fixtures",
  "verify:production-evidence-positive-propagation-fixtures",
    "verify:production-evidence-positive-propagation-fixtures",
    "verify:production-ready-no-go-fixtures",
    "verify:production-blocker-register-no-go-fixtures",
    "verify:production-ready-positive-fixtures",
    "verify:production-evidence-backfill-priority",
    "verify:production-evidence-backfill-execution-ledger",
    "verify:production-evidence-p0-quickstart",
    "verify:production-evidence-owner-execution-packets",
    "verify:production-no-go-command-center",
  "verify:production-evidence-crosslinks",
    "verify:production-evidence-draft-prep",
    "verify:production-evidence-draft-prep-fixtures",
    "verify:production-closure-gap-reports",
    "verify:nas-it-handoff",
    "verify:nas-diagnostics",
    "verify:nas-current-state-drift",
    "verify:final-closed-loop-audit",
    "verify:closed-loop-current-state",
    "verify:runtime-acceptance-evidence",
    "verify:ui-experience-acceptance",
  "verify:production-no-go-command-center",
  "verify:production-evidence-intake",
  ];
  if (!deliveryScript) {
    failures.push("package.json must expose verify:delivery-gates");
  } else {
    for (const command of requiredDeliveryCommands) {
      if (!deliveryScript.includes(command)) failures.push(`verify:delivery-gates must include ${command}`);
    }
    for (const forbidden of ["test:backend", "android:p0", "dev:backend", "dev:web"]) {
      if (deliveryScript.includes(forbidden)) failures.push(`verify:delivery-gates must stay lightweight and not include ${forbidden}`);
    }
  }
}

requireIncludes("docs/delivery-readiness-gates-v0.1.md", [
  "DELIVERY_READINESS_GATES_READY",
  "VERIFY_DELIVERY_GATES_READY",
  "NO_DELIVERY_WITHOUT_EXTERNAL_DEPENDENCY_GATES",
  "NO_FORMAL_CODE_OUTSIDE_160",
  "NO_PRODUCTION_DEPLOYMENT_WITH_PENDING_EXTERNAL_BLOCKERS",
  "OWNER_ACTION_PACKS_FROM_PRODUCTION_BLOCKERS",
  "OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS",
  "PRODUCTION_EVIDENCE_SUBMISSION_KIT_READY",
  "PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_READY",
  "PRODUCTION_EVIDENCE_OWNER_GUIDES_READY",
  "PRODUCTION_EVIDENCE_SUBMISSION_STATUS_READY",
  "PRODUCTION_EVIDENCE_SUBMISSIONS_GUARD_READY",
  "PRODUCTION_EVIDENCE_NEGATIVE_FIXTURES_READY",
  "PRODUCTION_EVIDENCE_POSITIVE_FIXTURES_READY",
  "PRODUCTION_EVIDENCE_POSITIVE_PROPAGATION_FIXTURES_READY",
  "PRODUCTION_READY_NO_GO_FIXTURES_READY",
  "PRODUCTION_BLOCKER_REGISTER_NO_GO_FIXTURES_READY",
  "PRODUCTION_READY_POSITIVE_FIXTURES_READY",
  "PRODUCTION_EVIDENCE_BACKFILL_PRIORITY_READY",
  "PRODUCTION_EVIDENCE_BACKFILL_EXECUTION_LEDGER_READY",
  "PRODUCTION_EVIDENCE_P0_QUICKSTART_READY",
  "PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKETS_READY",
  "PRODUCTION_NO_GO_RESOLUTION_COMMAND_CENTER_READY",
  "PRODUCTION_EVIDENCE_CROSSLINKS_READY",
  "OWNER_PACKET_ONE_STOP_BACKFILL_READY",
  "PRODUCTION_EVIDENCE_DRAFT_PREP_READY",
  "PRODUCTION_EVIDENCE_DRAFT_PREP_FIXTURES_READY",
  "PRODUCTION_CLOSURE_GAP_REPORTS_READY",
  "EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_READY",
  "PARTIAL_REQUIREMENTS_HAVE_OWNER_MANIFESTS",
  "PARTIAL_REQUIREMENT_MOCK_COVERAGE_READY",
  "PARTIAL_REQUIREMENTS_HAVE_MOCK_ACCEPTANCE_PATHS",
  "NO_FAKE_PRODUCTION_MANIFEST_CLOSURE",
  "NAS_DIAGNOSTIC_REPORT_READY",
  "VERIFY_NAS_DIAGNOSTICS_READY",
  "NAS_CURRENT_STATE_DRIFT_REPORT_READY",
  "VERIFY_NAS_CURRENT_STATE_DRIFT_READY",
  "HISTORICAL_NAS_EVIDENCE_NOT_CURRENT_CLOSURE",
  "VERIFY_RUNTIME_ACCEPTANCE_EVIDENCE_READY",
  "RUNTIME_ACCEPTANCE_EVIDENCE_READY",
  "UI_EXPERIENCE_ACCEPTANCE_REPORT_READY",
  "VERIFY_UI_EXPERIENCE_ACCEPTANCE_READY",
  "docs:production-closure-gap-reports",
  "verify:production-closure-gap-reports",
  "verify:external-evidence-requirement-traceability",
  "verify:partial-requirement-mock-coverage",
  "npm run verify:delivery-gates",
  "verify:external-dependency-gates",
    "verify:external-evidence-requirement-traceability",
  "verify:coding-host-protection",
  "verify:production-readiness-evidence",
  "verify:production-blockers",
  "verify:production-owner-actions",
  "verify:production-evidence-submission-kit",
  "verify:production-evidence-manifest-templates",
  "verify:production-evidence-owner-guides",
  "verify:production-evidence-submission-status",
  "verify:production-evidence-backfill-priority",
  "verify:production-evidence-backfill-execution-ledger",
  "verify:production-evidence-intake",
  "verify:production-evidence-positive-fixtures",
  "prepare:production-evidence-drafts",
  "verify:production-evidence-draft-prep",
  "verify:production-evidence-draft-prep-fixtures",
  "verify:nas-current-state-drift",
]);

requireIncludes("docs/external-dependency-gates-v0.1.md", [
  "EXTERNAL_DEPENDENCY_GATES_READY",
  "NO_REAL_VENDOR_CONTROL_WITHOUT_SUPPLIER_READY",
  "NO_INDEPENDENT_PYTHON_AI_SERVICE_WITHOUT_GATE",
  "PRODUCTION_EXTERNAL_BLOCKERS_TRACKED",
]);

requireIncludes("docs/00-current-effective-document-entry-v0.1.md", [
  "docs/delivery-readiness-gates-v0.1.md",
  "docs/external-dependency-gates-v0.1.md",
  "npm run verify:delivery-gates",
  "docs/nas-diagnostic-report-v0.1.md",
  "docs/nas-current-state-drift-report-v0.1.md",
  "npm run verify:nas-current-state-drift",
  "docs/production-evidence-p0-quickstart-v0.1.md",
  "docs/production-evidence-owner-execution-packets-v0.1.md",
  "npm run verify:production-evidence-owner-execution-packets",
  "npm run verify:production-evidence-crosslinks",
  "npm run prepare:production-evidence-drafts",
  "npm run verify:production-evidence-draft-prep",
  "npm run verify:production-evidence-draft-prep-fixtures",
  "npm run verify:runtime-acceptance-evidence",
  "npm run verify:ui-experience-acceptance",
  "docs/runtime-acceptance-evidence-latest.json",
  "docs/runtime-acceptance-evidence-v0.1.md",
  "docs/closed-loop-implementation-ui-acceptance-report-v0.1.md",
  "docs/partial-requirement-mock-coverage-v0.1.md",
  "npm run verify:partial-requirement-mock-coverage",
]);

requireIncludes("docs/delivery-package-manifest-v0.1.md", [
  "delivery-readiness-gates-v0.1.md",
  "external-dependency-gates-v0.1.md",
  "npm run verify:delivery-gates",
  "docs/nas-diagnostic-report-v0.1.md",
  "docs/nas-current-state-drift-report-v0.1.md",
  "npm run verify:nas-current-state-drift",
  "docs/production-evidence-p0-quickstart-v0.1.md",
  "docs/production-evidence-owner-execution-packets-v0.1.md",
  "npm run verify:production-evidence-owner-execution-packets",
  "npm run verify:production-evidence-crosslinks",
  "npm run prepare:production-evidence-drafts",
  "npm run verify:production-evidence-draft-prep",
  "npm run verify:production-evidence-draft-prep-fixtures",
  "npm run verify:runtime-acceptance-evidence",
  "npm run verify:ui-experience-acceptance",
  "docs/runtime-acceptance-evidence-latest.json",
  "docs/runtime-acceptance-evidence-v0.1.md",
  "docs/closed-loop-implementation-ui-acceptance-report-v0.1.md",
]);

// CI 定义唯一：模板与生效 workflow 必须字节一致，且 checkout 带完整历史（fetch-depth: 0）——
// 浅克隆会让 verify:html-ledger-sync / verify:html-param-tier 取不到基线（决策台账 2026-08-06 补注；M001 审计 2026-09-03）。
const lf = (text) => text.replace(/\r\n/g, "\n");
const liveWorkflow = lf(readText(".github/workflows/predeploy-gates.yml"));
const templateWorkflow = lf(readText("ops/ci/predeploy-gates.yml"));
if (liveWorkflow && templateWorkflow && liveWorkflow !== templateWorkflow) {
  failures.push("ops/ci/predeploy-gates.yml must be byte-identical to .github/workflows/predeploy-gates.yml");
}
for (const workflow of [".github/workflows/predeploy-gates.yml", "ops/ci/predeploy-gates.yml"]) {
  if (!readText(workflow).includes("fetch-depth: 0")) failures.push(`${workflow} must checkout with fetch-depth: 0`);
}
if (packageJson) {
  const predeploy = packageJson.scripts?.["verify:predeploy"] ?? "";
  for (const gate of ["verify:html-ledger-sync", "verify:entry-anchors", "verify:html-param-tier"]) {
    if (!predeploy.includes(gate)) failures.push(`verify:predeploy must include ${gate}`);
  }
}

if (failures.length > 0) {
  console.error("DELIVERY_GATES_DOCS_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("DELIVERY_GATES_DOCS_PASS");
