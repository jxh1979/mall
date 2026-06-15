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
  if (scripts["verify:t3d-local-llm-ai-quality"] !== "node tools/verify-t3d-local-llm-ai-quality.mjs") {
    failures.push("package.json must expose verify:t3d-local-llm-ai-quality");
  }
}

requireIncludes("docs/superpowers/specs/2026-06-15-t3d-local-llm-ai-quality-design.md", [
  "T3D_LOCAL_LLM_AI_QUALITY_DESIGN_READY",
  "127.0.0.1",
  "manual_review_required",
]);

requireIncludes("docs/superpowers/plans/2026-06-15-t3d-local-llm-ai-quality.md", [
  "LocalLlmClient",
  "loopback-only",
  "verify:t3d-local-llm-ai-quality",
]);

requireIncludes("docs/t3d-local-llm-ai-quality-v0.1.md", [
  "T3D_LOCAL_LLM_AI_QUALITY_READY",
  "LOOPBACK_ONLY_LLM",
  "AI_OUTPUT_NO_AUTO_DECISION",
  "verify:t3d-local-llm-ai-quality",
]);

const client = requireIncludes("backend/src/main/java/com/complex/platform/analytics/OpenAiCompatibleLocalLlmClient.java", [
  "T3D_LOCAL_LLM_ADAPTER_READY",
  "LOOPBACK_ONLY_LLM",
  "AI_OUTPUT_NO_AUTO_DECISION",
  "127.0.0.1",
  "localhost",
  "::1",
  "chat/completions",
]);
if (client.includes("Authorization") || client.includes("Bearer ")) {
  failures.push("local LLM client must not require or embed API authorization secrets in T3-D1");
}

requireIncludes("backend/src/main/java/com/complex/platform/analytics/AiTextRedactor.java", [
  "[PHONE_REDACTED]",
  "[ID_REDACTED]",
  "[EMAIL_REDACTED]",
  "[CARD_REDACTED]",
]);

requireIncludes("backend/src/main/java/com/complex/platform/analytics/AnalyticsService.java", [
  "analytics.local-llm-ai-quality-suggestion",
  "analytics.ai_quality.local_llm_suggestion.created",
  "local_llm_suggestion",
  "manualReviewRequired",
]);

requireIncludes("backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java", [
  "/ai-quality/local-llm-suggestions",
  "analytics.risk_alert.manage",
]);

requireIncludes("backend/src/main/resources/db/migration/V040__t3d_local_llm_ai_quality_mode.sql", [
  "local_llm_suggestion",
  "chk_analytics_ai_quality_tasks_mode",
]);

const openapi = readJson("docs/openapi/openapi-p0.json");
if (openapi) {
  const operation = openapi.paths?.["/api/v1/analytics/ai-quality/local-llm-suggestions"]?.post;
  if (!operation) {
    failures.push("OpenAPI must include POST /api/v1/analytics/ai-quality/local-llm-suggestions");
  } else {
    if (operation.operationId !== "generateLocalLlmAiQualitySuggestion") failures.push("OpenAPI operationId mismatch");
    if (operation["x-permission"] !== "analytics.risk_alert.manage") failures.push("OpenAPI permission mismatch");
    if (operation["x-idempotency-required"] !== true) failures.push("OpenAPI idempotency flag mismatch");
    if (operation["x-audit-event-success"] !== "analytics.ai_quality.local_llm_suggestion.created") failures.push("OpenAPI success audit event mismatch");
    if (operation["x-audit-event-failure"] !== "analytics.ai_quality.local_llm_suggestion.failed") failures.push("OpenAPI failure audit event mismatch");
  }
  const schema = openapi.components?.schemas?.AiQualitySuggestionRequest;
  if (!schema) failures.push("OpenAPI must include AiQualitySuggestionRequest schema");
  if (schema?.properties?.source_text?.maxLength !== 12000) failures.push("source_text maxLength must be 12000");
}

const dashboard = readText("docs/project-progress-dashboard.md");
if (!dashboard.includes("T3-D1") || !dashboard.includes("verify:t3d-local-llm-ai-quality")) {
  failures.push("project dashboard must mention T3-D1 and verifier");
}
const entry = readText("docs/00-current-effective-document-entry-v0.1.md");
if (!entry.includes("docs/t3d-local-llm-ai-quality-v0.1.md") || !entry.includes("verify:t3d-local-llm-ai-quality")) {
  failures.push("effective entry must mention T3-D1 doc and verifier");
}

const combined = [client, readText("docs/t3d-local-llm-ai-quality-v0.1.md")].join("\n");
for (const forbidden of ["sk_live_", "AKIA", "BEGIN PRIVATE KEY", "prod_secret_value", "real_model_token"]) {
  if (combined.includes(forbidden)) failures.push(`T3-D1 files must not include forbidden secret token: ${forbidden}`);
}

if (failures.length > 0) {
  console.error("T3D_LOCAL_LLM_AI_QUALITY_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("T3D_LOCAL_LLM_AI_QUALITY_PASS");
