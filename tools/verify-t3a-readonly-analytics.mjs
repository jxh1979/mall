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
  if (scripts["verify:t3a-readonly-analytics"] !== "node tools/verify-t3a-readonly-analytics.mjs") {
    failures.push("package.json must expose verify:t3a-readonly-analytics");
  }
}

const plan = readText("docs/superpowers/plans/2026-06-15-t3a-readonly-analytics.md");
for (const marker of [
  "T3A_READONLY_ANALYTICS_PLAN_READY",
  "allowed_scope=readonly_analytics_only",
  "no_backend_write_api_change",
  "no_python_ai_service",
  "no_real_vendor_control",
]) {
  if (!plan.includes(marker)) {
    failures.push(`T3-A plan must include marker: ${marker}`);
  }
}

const source = readText("apps/web/src/main.tsx");
for (const marker of [
  "T3A_READONLY_ANALYTICS_READY",
  "T3A_READONLY_ANALYTICS_VIEW_START",
  "T3A_READONLY_ANALYTICS_VIEW_END",
  "t3aReadOnlyApiPaths",
  "type AnalyticsMerchantSummary",
  "type RobotMetric",
  "function summarizeRobotMetrics",
  "function renderT3AReadOnlyAnalytics",
  "T3-A 只读经营分析",
  "只读分析，不发控制指令",
]) {
  if (!source.includes(marker)) {
    failures.push(`apps/web/src/main.tsx must include marker: ${marker}`);
  }
}

for (const apiPath of [
  "/api/v1/analytics/business-metric-snapshots",
  "/api/v1/analytics/merchant-health",
  "/api/v1/analytics/staff-service-score",
  "/api/v1/analytics/customer-intents",
  "/api/v1/analytics/robot",
  "/api/v1/analytics/metric-definitions",
]) {
  if (!source.includes(apiPath)) {
    failures.push(`T3-A web snapshot must read ${apiPath}`);
  }
}

const start = source.indexOf("T3A_READONLY_ANALYTICS_VIEW_START");
const end = source.indexOf("T3A_READONLY_ANALYTICS_VIEW_END");
if (start === -1 || end === -1 || end <= start) {
  failures.push("T3-A view region markers must wrap the readonly view");
} else {
  const t3aRegion = source.slice(start, end);
  for (const forbidden of [
    "postJson(",
    "method: \"POST\"",
    "method: 'POST'",
    "handleRiskAlert",
    "handleResolveRisk",
    "confirmHighImpactAction",
  ]) {
    if (t3aRegion.includes(forbidden)) {
      failures.push(`T3-A readonly view must not include write/control token: ${forbidden}`);
    }
  }
}

const dashboard = readText("docs/project-progress-dashboard.md");
if (!dashboard.includes("T3-A 只读经营分析")) {
  failures.push("docs/project-progress-dashboard.md must mention T3-A readonly analytics delivery");
}

if (failures.length > 0) {
  console.error("T3A_READONLY_ANALYTICS_FAIL");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log("T3A_READONLY_ANALYTICS_PASS");
