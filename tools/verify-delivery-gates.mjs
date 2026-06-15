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
  const requiredDeliveryCommands = [
    "verify:delivery-gates-docs",
    "verify:coding-host-protection",
    "verify:external-dependency-gates",
    "verify:t3-coding-package",
    "verify:t3b-supplier-mock-kit",
    "verify:t3d-local-llm-ai-quality",
    "verify:p0-contract",
    "verify:security-baseline",
    "verify:production-readiness-evidence",
    "verify:production-blockers",
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
  "npm run verify:delivery-gates",
  "verify:external-dependency-gates",
  "verify:coding-host-protection",
  "verify:production-readiness-evidence",
  "verify:production-blockers",
  "verify:production-evidence-intake",
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
]);

requireIncludes("docs/delivery-package-manifest-v0.1.md", [
  "delivery-readiness-gates-v0.1.md",
  "external-dependency-gates-v0.1.md",
  "npm run verify:delivery-gates",
]);

if (failures.length > 0) {
  console.error("DELIVERY_GATES_DOCS_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("DELIVERY_GATES_DOCS_PASS");