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

function pathExists(relativePath) {
  return fs.existsSync(path.join(repoRoot, relativePath));
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  if (scripts["verify:external-dependency-gates"] !== "node tools/verify-external-dependency-gates.mjs") {
    failures.push("package.json must expose verify:external-dependency-gates");
  }
  for (const [name, value] of Object.entries(scripts)) {
    const scriptText = `${name} ${value}`;
    for (const forbidden of ["python-ai-service", "vendor-control", "robot-control", "video-control", "device-control"]) {
      if (scriptText.includes(forbidden)) failures.push(`package script must not introduce blocked service/control command: ${scriptText}`);
    }
  }
}

requireIncludes("docs/external-dependency-gates-v0.1.md", [
  "EXTERNAL_DEPENDENCY_GATES_READY",
  "NO_REAL_VENDOR_CONTROL_WITHOUT_SUPPLIER_READY",
  "NO_INDEPENDENT_PYTHON_AI_SERVICE_WITHOUT_GATE",
  "PRODUCTION_EXTERNAL_BLOCKERS_TRACKED",
  "verify:external-dependency-gates",
]);

requireIncludes("docs/t3-coding-entry-pack-v0.1.md", [
  "NO_T3_REAL_VENDOR_CONTROL_WITHOUT_SUPPLIER_READY",
  "NO_PYTHON_AI_SERVICE_WITHOUT_T3D_GATE",
]);

// b969b8cf 把入口包 §3 切片准入表搬到 scope-wide 规则正本 §B-19（同 U13-010 先例），判据读取面随之指向正本。
requireIncludes("docs/scope-wide-coding-rule-canonical-V0.01.md", [
  "T3-C 机器人/视频/设备真实控制闭环 | 阻断",
  "T3-D 独立 Python AI 服务 | 阻断",
]);

requireIncludes("docs/t3-security-and-supplier-readiness-gate-v0.1.md", [
  "robot_platform=blocked_until_supplier_api",
  "video_platform=blocked_until_supplier_api",
  "device_vendor=blocked_until_supplier_api",
  "python_ai_service=blocked_until_model_sla_data_review",
  "T3-D2 独立 Python AI 服务和真实供应商控制仍阻断",
]);

requireIncludes("docs/t3b-supplier-sdk-mock-kit-v0.1.md", [
  "NO_REAL_SECRET",
  "NO_PRODUCTION_VENDOR_CONTROL",
  "T3-C 真实机器人/视频/设备控制仍保持 blocked",
]);

requireIncludes("docs/p2-full-completion-audit-v0.1.md", [
  "P2-E 第三方深度联动：边界完成",
  "P2-F 独立 Python AI 服务：边界完成，正式服务编码暂缓",
  "P2-E 不伪造厂商 SDK",
  "P2-F 不让 AI 结果直接影响付款、核销、处罚、退款和员工绩效",
]);

requireIncludes("docs/production-readiness-implementation-ledger-template-v0.1.md", [
  "PRD-MDM-001",
  "PRD-PAY-001",
  "PRD-FACE-001",
  "PRD-WECOM-001",
  "PRD-3P-001",
  "PRD-CRED-3P-001",
  "pending_info",
  "供应商未确定时",
]);

requireIncludes("docs/production-readiness-evidence/production-readiness-evidence-backfill-20260615.md", [
  "仍需 IT/采购/安全回填的生产阻断证据",
  "SEC-P0-MDM-DEVICE-001",
  "PRD-FACE-001",
  "PRD-3P-001",
  "PRD-PAY-001",
  "PRD-WECOM-001",
  "mock adapter 不等于真实生产接入",
]);

const forbiddenPaths = [
  "apps/python-ai-service",
  "apps/ai-service",
  "services/python-ai-service",
  "services/ai-service",
  "backend/src/main/python",
  "docker-compose.ai.yml",
  "docker-compose.python-ai.yml",
  "ops/160/python-ai-service.service",
  "ops/160/vendor-control.service",
];
for (const relativePath of forbiddenPaths) {
  if (pathExists(relativePath)) failures.push(`blocked external dependency implementation path exists: ${relativePath}`);
}

const openapi = readJson("docs/openapi/openapi-p0.json");
if (openapi) {
  const paths = Object.keys(openapi.paths ?? {});
  const forbiddenPathPatterns = [
    /\/api\/v1\/robots?\/[^/]+\/(commands?|control|dispatch|move|stop)/i,
    /\/api\/v1\/robot-control/i,
    /\/api\/v1\/video\/(play|stream|clip|control)/i,
    /\/api\/v1\/video-control/i,
    /\/api\/v1\/devices?\/[^/]+\/(unlock|open|close|control|command)/i,
    /\/api\/v1\/device-control/i,
    /\/api\/v1\/python-ai/i,
    /\/api\/v1\/ai-service/i,
  ];
  for (const apiPath of paths) {
    if (forbiddenPathPatterns.some((pattern) => pattern.test(apiPath))) {
      failures.push(`OpenAPI contains blocked real-control or independent AI service path: ${apiPath}`);
    }
  }
}

const t3Gate = readText("docs/t3-security-and-supplier-readiness-gate-v0.1.md");
const p2Audit = readText("docs/p2-full-completion-audit-v0.1.md");
const combined = `${t3Gate}\n${p2Audit}`;
for (const forbidden of ["production_ready=true", "真实供应商控制已完成", "独立 Python AI 服务已完成", "自动处罚已完成", "自动退款已完成", "自动调价已完成"]) {
  if (combined.includes(forbidden)) failures.push(`external dependency gate docs must not claim blocked capability is complete: ${forbidden}`);
}

if (failures.length > 0) {
  console.error("EXTERNAL_DEPENDENCY_GATES_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("EXTERNAL_DEPENDENCY_GATES_PASS");