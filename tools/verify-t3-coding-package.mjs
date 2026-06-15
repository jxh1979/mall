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
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) {
    failures.push(`${relativePath} is missing`);
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(absolutePath, "utf8"));
  } catch (error) {
    failures.push(`${relativePath} is not valid JSON: ${error.message}`);
    return null;
  }
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  if (scripts["verify:t3-coding-package"] !== "node tools/verify-t3-coding-package.mjs") {
    failures.push("package.json must expose verify:t3-coding-package");
  }
}

const requiredDocs = {
  "docs/t3-coding-entry-pack-v0.1.md": [
    "T3_CODING_PACKAGE_READY",
    "T3_ALLOWED_FIRST_SLICE=T3-A_READONLY_ANALYTICS",
    "NO_T3_REAL_VENDOR_CONTROL_WITHOUT_SUPPLIER_READY",
    "NO_PYTHON_AI_SERVICE_WITHOUT_T3D_GATE",
    "verify:t3-coding-package",
  ],
  "docs/t3-user-story-map-and-sprint-slices-v0.1.md": [
    "T3_USER_STORY_MAP_READY",
    "T3-A_READONLY_ANALYTICS",
    "T3-B_SDK_MOCK_DEEPENING",
    "T3-C_VENDOR_CONTROL_BLOCKED",
    "T3-D_PYTHON_AI_BLOCKED",
  ],
  "docs/t3-security-and-supplier-readiness-gate-v0.1.md": [
    "T3_SECURITY_GATE_READY",
    "robot_platform=blocked_until_supplier_api",
    "video_platform=blocked_until_supplier_api",
    "device_vendor=blocked_until_supplier_api",
    "python_ai_service=blocked_until_model_sla_data_review",
  ],
  "docs/t3-entry-assessment-and-coding-package-audit-v0.1.md": [
    "T3_ENTRY_AUDIT_READY",
    "decision=do_not_start_t3_control_or_python_ai_yet",
    "allowed_next=T3-A_READONLY_ANALYTICS",
  ],
};

for (const [relativePath, markers] of Object.entries(requiredDocs)) {
  const text = readText(relativePath);
  for (const marker of markers) {
    if (!text.includes(marker)) {
      failures.push(`${relativePath} must include marker: ${marker}`);
    }
  }
}

const effectiveEntry = readText("docs/00-current-effective-document-entry-v0.1.md");
for (const marker of [
  "docs/t3-coding-entry-pack-v0.1.md",
  "docs/t3-security-and-supplier-readiness-gate-v0.1.md",
  "verify:t3-coding-package",
]) {
  if (!effectiveEntry.includes(marker)) {
    failures.push(`docs/00-current-effective-document-entry-v0.1.md must reference ${marker}`);
  }
}

const dashboard = readText("docs/project-progress-dashboard.md");
for (const marker of [
  "T3-0",
  "T3-A",
  "verify:t3-coding-package",
]) {
  if (!dashboard.includes(marker)) {
    failures.push(`docs/project-progress-dashboard.md must reference ${marker}`);
  }
}

if (failures.length > 0) {
  console.error("T3_CODING_PACKAGE_FAIL");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log("T3_CODING_PACKAGE_PASS");
