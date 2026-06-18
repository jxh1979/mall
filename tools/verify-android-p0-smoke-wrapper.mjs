import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDir, "..");

function fail(message) {
  console.error(`[android-p0-smoke-wrapper] ${message}`);
  process.exit(1);
}

const packageJson = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));
const smokeScript = packageJson.scripts?.["android:p0:smoke"] ?? "";
if (!smokeScript.includes("tools/run-android-p0-smoke.mjs")) {
  fail("package.json android:p0:smoke must use tools/run-android-p0-smoke.mjs so verify:p0 does not depend on a manually started backend.");
}

const runnerPath = path.join(root, "tools", "run-android-p0-smoke.mjs");
if (!fs.existsSync(runnerPath)) {
  fail("tools/run-android-p0-smoke.mjs is missing.");
}

const runner = fs.readFileSync(runnerPath, "utf8");
const requiredSnippets = [
  "tools/run-backend-maven.mjs",
  "waitForHealth",
  "P0_API_BASE_URL",
  "P0_ADMIN_USERNAME",
  "P0_ADMIN_PASSWORD",
  "tools/run-android-gradle.mjs",
  "stopBackend",
];

for (const snippet of requiredSnippets) {
  if (!runner.includes(snippet)) {
    fail(`tools/run-android-p0-smoke.mjs must contain ${snippet}.`);
  }
}

console.log("ANDROID_P0_SMOKE_WRAPPER_PASS");
