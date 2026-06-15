import fs from "node:fs";
import path from "node:path";

const repoRoot = process.cwd();
const failures = [];

const forbiddenPaths = [
  "apps/api",
  "apps/api/src",
  "apps/api/prisma",
  "apps/api/dist",
  "apps/api/dev.db",
  "apps/api/package.json",
  "apps/api/tsconfig.json",
];

for (const relativePath of forbiddenPaths) {
  if (fs.existsSync(path.join(repoRoot, relativePath))) {
    failures.push(`forbidden legacy API path exists: ${relativePath}`);
  }
}

const packageJson = JSON.parse(fs.readFileSync(path.join(repoRoot, "package.json"), "utf8"));
const workspaces = Array.isArray(packageJson.workspaces) ? packageJson.workspaces : [];
if (workspaces.includes("apps/api")) {
  failures.push("root package.json must not include apps/api in workspaces");
}

const scripts = packageJson.scripts ?? {};
const forbiddenScriptNames = [
  "legacy:api:block",
  "prisma:generate",
  "prisma:migrate",
  "seed:p0",
  "smoke:p1-bar",
  "smoke:p1-amusement",
  "smoke:p1-integration",
  "smoke:p2-integration",
  "smoke:p3-analytics",
];

for (const scriptName of forbiddenScriptNames) {
  if (Object.hasOwn(scripts, scriptName)) {
    failures.push(`root package.json still exposes legacy script: ${scriptName}`);
  }
}

const forbiddenValuePatterns = [
  /apps\/api/i,
  /legacy:api:block/i,
  /TypeScript\/Fastify\/SQLite/i,
  /Fastify/i,
  /Prisma/i,
  /SQLite/i,
];

for (const [scriptName, scriptValue] of Object.entries(scripts)) {
  for (const pattern of forbiddenValuePatterns) {
    if (pattern.test(String(scriptValue))) {
      failures.push(`root package.json script ${scriptName} still references legacy API wording`);
    }
  }
}

const packageLockPath = path.join(repoRoot, "package-lock.json");
if (fs.existsSync(packageLockPath)) {
  const lockText = fs.readFileSync(packageLockPath, "utf8");
  const forbiddenLockPatterns = [/apps\/api/i, /legacy-apps-api/i, /@fastify\//i, /\bfastify\b/i, /@prisma\//i, /\bprisma\b/i, /\bsqlite\b/i];
  for (const pattern of forbiddenLockPatterns) {
    if (pattern.test(lockText)) {
      failures.push(`package-lock.json still contains legacy dependency marker: ${pattern}`);
    }
  }
}

if (failures.length > 0) {
  console.error("NO_LEGACY_API_PROTOTYPE_FAIL");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log("NO_LEGACY_API_PROTOTYPE_PASS");
