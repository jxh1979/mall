import { execFileSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const repoRoot = process.cwd();
const expectedHostIp = process.env.COMPLEX_EXPECTED_CODING_HOST_IP ?? "192.168.88.160";
const expectedHostname = process.env.COMPLEX_EXPECTED_CODING_HOSTNAME ?? "y9000p-ai";
const expectedRepoRoot = process.env.COMPLEX_EXPECTED_CODING_REPO_ROOT ?? "/home/james/complex-management";
const failures = [];

function normalizePath(value) {
  return String(value).trim().replaceAll("\\\\", "/");
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

function readText(relativePath) {
  const absolutePath = path.join(repoRoot, relativePath);
  if (!fs.existsSync(absolutePath)) {
    failures.push(`${relativePath} is missing`);
    return "";
  }
  return fs.readFileSync(absolutePath, "utf8");
}

const gitRoot = normalizePath(execFileSync("git", ["rev-parse", "--show-toplevel"], { encoding: "utf8" }));
if (gitRoot !== expectedRepoRoot) {
  failures.push(`git root must be ${expectedRepoRoot}, got ${gitRoot}`);
}

if (normalizePath(repoRoot) !== expectedRepoRoot) {
  failures.push(`current working directory must be ${expectedRepoRoot}, got ${normalizePath(repoRoot)}`);
}

const hostname = os.hostname();
if (hostname !== expectedHostname) {
  failures.push(`coding host hostname must be ${expectedHostname}, got ${hostname}`);
}

const addresses = Object.values(os.networkInterfaces())
  .flatMap((items) => items ?? [])
  .map((item) => item.address);
if (!addresses.includes(expectedHostIp)) {
  failures.push(`coding host must expose ${expectedHostIp}; found ${addresses.join(", ")}`);
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  if (scripts["verify:coding-host-protection"] !== "node tools/verify-coding-host-protection.mjs") {
    failures.push("package.json must expose verify:coding-host-protection");
  }
  if (!String(scripts["verify:ops160-recovery"] ?? "").includes("verify:coding-host-protection")) {
    failures.push("verify:ops160-recovery must include verify:coding-host-protection");
  }
  if (!String(scripts["verify:p0"] ?? "").includes("verify:coding-host-protection")) {
    failures.push("verify:p0 must include verify:coding-host-protection so full P0 validation cannot be run from a non-160 coding host");
  }
}

const baselineText = readText("docs/coding-host-protection-baseline-v0.1.md");
for (const marker of [
  `canonical_host_ip=${expectedHostIp}`,
  `canonical_hostname=${expectedHostname}`,
  `canonical_repo_root=${expectedRepoRoot}`,
  "local_role=coordination_quality_only",
  "local_code_policy=no_formal_code_on_local_machine",
  "verify:coding-host-protection",
]) {
  if (!baselineText.includes(marker)) {
    failures.push(`coding host baseline document must include marker: ${marker}`);
  }
}

const evidence = readJson("docs/production-readiness-evidence/coding-host-protection-latest.json");
if (evidence) {
  const requiredFields = {
    status: "passed",
    canonical_host_ip: expectedHostIp,
    canonical_hostname: expectedHostname,
    canonical_repo_root: expectedRepoRoot,
    local_role: "coordination_quality_only",
    local_code_policy: "no_formal_code_on_local_machine",
  };
  for (const [field, expectedValue] of Object.entries(requiredFields)) {
    if (evidence[field] !== expectedValue) {
      failures.push(`coding host evidence ${field} must be ${expectedValue}, got ${evidence[field]}`);
    }
  }
  const guardedScripts = Array.isArray(evidence.guarded_scripts) ? evidence.guarded_scripts : [];
  for (const scriptName of ["verify:coding-host-protection", "verify:ops160-recovery", "verify:p0"]) {
    if (!guardedScripts.includes(scriptName)) {
      failures.push(`coding host evidence guarded_scripts must include ${scriptName}`);
    }
  }
}

if (failures.length > 0) {
  console.error("CODING_HOST_PROTECTION_FAIL");
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log("CODING_HOST_PROTECTION_PASS");
