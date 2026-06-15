import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const repoRoot = process.cwd();
const failures = [];

const expected = {
  nasServer: process.env.NAS_SERVER ?? "192.168.88.113",
  nasShare: process.env.NAS_SHARE ?? "bn",
  nasUnc: process.env.NAS_UNC ?? "//192.168.88.113/bn",
  mountPath: process.env.NAS_MOUNT_PATH ?? "/mnt/complex-nas-backup",
  fstype: "cifs",
  smbUsername: process.env.NAS_SMB_USERNAME ?? "jxh1",
  credentialRef: "cred://prod/nas/smb/complex-backup-rw",
  retentionDays: 30,
  capacityLimit: "1TB",
  encryptionEnabled: false,
};

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

function requireEqual(label, actual, expectedValue) {
  if (actual !== expectedValue) failures.push(`${label} must be ${expectedValue}, got ${actual}`);
}

function requireIncludes(label, actual, expectedValue) {
  if (!String(actual ?? "").includes(expectedValue)) failures.push(`${label} must include ${expectedValue}, got ${actual}`);
}

function forbidPlaintextSecrets(label, text) {
  const patterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/,
    /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
    /\bAKIA[0-9A-Z]{16}\b/,
    /\b(?:password|token)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
    /\b(?:secret|secret_key)\s*[:=]\s*["'][^"'<\n]{8,}["']/i,
  ];
  for (const [index, pattern] of patterns.entries()) {
    if (pattern.test(text)) failures.push(`${label} contains apparent plaintext secret pattern #${index + 1}`);
  }
}

const evidenceText = readText("docs/production-readiness-evidence/nas-smb-mount-latest.json");
forbidPlaintextSecrets("nas smb mount evidence", evidenceText);
const evidence = evidenceText ? JSON.parse(evidenceText) : null;
if (evidence) {
  requireEqual("evidence.status", evidence.status, "passed");
  requireEqual("evidence.evidence_type", evidence.evidence_type, "nas_smb_mount_live_rw");
  requireEqual("evidence.nas_server", evidence.nas_server, expected.nasServer);
  requireEqual("evidence.nas_share", evidence.nas_share, expected.nasShare);
  requireEqual("evidence.nas_unc", evidence.nas_unc, expected.nasUnc);
  requireEqual("evidence.mount_path", evidence.mount_path, expected.mountPath);
  requireEqual("evidence.fstype", evidence.fstype, expected.fstype);
  requireEqual("evidence.smb_username", evidence.smb_username, expected.smbUsername);
  requireEqual("evidence.credential_ref", evidence.credential_ref, expected.credentialRef);
  requireEqual("evidence.retention_days", evidence.retention_days, expected.retentionDays);
  requireEqual("evidence.capacity_limit", evidence.capacity_limit, expected.capacityLimit);
  requireEqual("evidence.encryption_enabled", evidence.encryption_enabled, expected.encryptionEnabled);
  requireEqual("evidence.write_test_status", evidence.write_test_status, "passed");
  requireEqual("evidence.plaintext_secret_present", evidence.plaintext_secret_present, false);
}

let findmntOutput = "";
try {
  findmntOutput = execFileSync("findmnt", ["-no", "TARGET,SOURCE,FSTYPE,OPTIONS", expected.mountPath], { encoding: "utf8" }).trim();
} catch (error) {
  failures.push(`findmnt ${expected.mountPath} failed: ${error.message}`);
}

if (findmntOutput) {
  const [target, source, fstype, ...optionParts] = findmntOutput.split(/\s+/);
  const options = optionParts.join(" ");
  requireEqual("live mount target", target, expected.mountPath);
  requireEqual("live mount source", source, expected.nasUnc);
  requireEqual("live mount fstype", fstype, expected.fstype);
  requireIncludes("live mount options", options, "rw");
  requireIncludes("live mount options", options, "vers=3.0");
  requireIncludes("live mount options", options, `username=${expected.smbUsername}`);
}

if (!fs.existsSync(expected.mountPath)) {
  failures.push(`mount path does not exist: ${expected.mountPath}`);
} else {
  const testFile = path.join(expected.mountPath, `.complex-nas-evidence-write-test-${process.pid}`);
  try {
    fs.writeFileSync(testFile, "complex nas evidence write test\n", { mode: 0o600 });
    const readBack = fs.readFileSync(testFile, "utf8");
    if (readBack !== "complex nas evidence write test\n") failures.push("NAS write/read test content mismatch");
  } catch (error) {
    failures.push(`NAS write/read test failed: ${error.message}`);
  } finally {
    try {
      if (fs.existsSync(testFile)) fs.rmSync(testFile);
    } catch (error) {
      failures.push(`NAS write test cleanup failed: ${error.message}`);
    }
  }
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  if (scripts["verify:nas-smb-mount-evidence"] !== "node tools/verify-nas-smb-mount-evidence.mjs") {
    failures.push("package.json must expose verify:nas-smb-mount-evidence");
  }
  if (!String(scripts["verify:ops160-recovery"] ?? "").includes("verify:nas-smb-mount-evidence")) {
    failures.push("verify:ops160-recovery must include verify:nas-smb-mount-evidence");
  }
}

const readinessBackfill = readText("docs/production-readiness-evidence/production-readiness-evidence-backfill-20260615.md");
for (const marker of [
  "nas-smb-mount-latest.json",
  "verify:nas-smb-mount-evidence",
  "PRD-NAS-001",
]) {
  if (!readinessBackfill.includes(marker)) failures.push(`production readiness evidence backfill must include ${marker}`);
}

const nasRunbook = readText("docs/nas-backup-and-restore-runbook-v0.1.md");
for (const marker of [
  "nas-smb-mount-latest.json",
  "verify:nas-smb-mount-evidence",
]) {
  if (!nasRunbook.includes(marker)) failures.push(`NAS runbook must include ${marker}`);
}

if (failures.length > 0) {
  console.error("NAS_SMB_MOUNT_EVIDENCE_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("NAS_SMB_MOUNT_EVIDENCE_PASS");