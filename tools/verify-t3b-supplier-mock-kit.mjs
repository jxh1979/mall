import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";

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

function checkIncludes(relativePath, markers) {
  const text = readText(relativePath);
  for (const marker of markers) {
    if (!text.includes(marker)) failures.push(`${relativePath} must include marker: ${marker}`);
  }
  return text;
}

const packageJson = readJson("package.json");
if (packageJson) {
  const scripts = packageJson.scripts ?? {};
  if (scripts["verify:t3b-supplier-mock-kit"] !== "node tools/verify-t3b-supplier-mock-kit.mjs") {
    failures.push("package.json must expose verify:t3b-supplier-mock-kit");
  }
}

checkIncludes("docs/superpowers/plans/2026-06-15-t3b-supplier-mock-kit.md", [
  "T3B_SUPPLIER_MOCK_PLAN_READY",
  "allowed_scope=sdk_mock_examples_only",
  "no_real_vendor_control",
  "no_real_secret",
]);
const kitDoc = checkIncludes("docs/t3b-supplier-sdk-mock-kit-v0.1.md", [
  "T3B_SUPPLIER_MOCK_KIT_READY",
  "NO_REAL_SECRET",
  "NO_PRODUCTION_VENDOR_CONTROL",
  "verify:t3b-supplier-mock-kit",
  "tools/t3b-open-platform-signature-helper.mjs",
]);
const helper = checkIncludes("tools/t3b-open-platform-signature-helper.mjs", [
  "T3B_SIGNATURE_HELPER_READY",
  "createHmac",
  "stableStringify",
  "webhookCanonical",
  "openAuthCanonical",
]);

const authExample = readJson("docs/supplier-mock/t3b-open-auth-request.example.json");
if (authExample) {
  if (authExample.method !== "POST") failures.push("auth example method must be POST");
  if (authExample.path !== "/api/open/auth/token") failures.push("auth example path must be /api/open/auth/token");
  if (authExample.body?.app_key !== "p0-open-app") failures.push("auth example must use p0-open-app mock app key");
  if (authExample.body?.signature !== "{{generated_hmac_sha256}}") failures.push("auth example must use generated signature placeholder");
}
const webhookExample = readJson("docs/supplier-mock/t3b-webhook-envelope.example.json");
if (webhookExample) {
  if (webhookExample.method !== "POST") failures.push("webhook example method must be POST");
  if (webhookExample.path !== "/api/open/d/p0-default/webhooks/mock-provider/payment.paid") failures.push("webhook example path mismatch");
  if (webhookExample.headers?.["X-Webhook-Signature"] !== "{{generated_hmac_sha256}}") failures.push("webhook example must use generated signature placeholder");
  if (!webhookExample.body?.idempotency_key) failures.push("webhook example must include idempotency_key");
}
const adapterExample = readJson("docs/supplier-mock/t3b-provider-adapter-test.example.json");
if (adapterExample) {
  if (adapterExample.method !== "POST") failures.push("adapter mock example method must be POST");
  if (adapterExample.path !== "/api/v1/integration/provider-adapters/{id}/test-mock") failures.push("adapter mock example path mismatch");
  if (adapterExample.body?.provider_code !== "mock-provider") failures.push("adapter mock example must use mock-provider");
  if (adapterExample.body?.scenario !== "t3b_contract_smoke") failures.push("adapter mock example scenario mismatch");
}

const scannedText = [kitDoc, helper]
  .concat([
    "docs/supplier-mock/t3b-open-auth-request.example.json",
    "docs/supplier-mock/t3b-webhook-envelope.example.json",
    "docs/supplier-mock/t3b-provider-adapter-test.example.json",
  ].map(readText))
  .join("\n");
for (const forbidden of [
  "sk_live_",
  "AKIA",
  "BEGIN PRIVATE KEY",
  "prod_secret_value",
  "real_vendor_token",
  "real_payment_key",
]) {
  if (scannedText.includes(forbidden)) failures.push(`T3-B mock kit must not include forbidden secret/control token: ${forbidden}`);
}

if (fs.existsSync(path.join(repoRoot, "tools/t3b-open-platform-signature-helper.mjs"))) {
  const run = spawnSync(
    process.execPath,
    ["tools/t3b-open-platform-signature-helper.mjs", "auth", "--app-key", "p0-open-app", "--timestamp", "1781424000000", "--nonce", "nonce-t3b"],
    { cwd: repoRoot, env: { ...process.env, T3B_MOCK_SIGNING_KEY: "mock-key-for-tests-only" }, encoding: "utf8" }
  );
  if (run.status !== 0) {
    failures.push(`signature helper auth mode failed: ${run.stderr || run.stdout}`);
  } else {
    try {
      const payload = JSON.parse(run.stdout);
      const expected = crypto.createHmac("sha256", "mock-key-for-tests-only").update("app_key=p0-open-app\nnonce=nonce-t3b\ntimestamp=1781424000000").digest("hex");
      if (payload.signature !== expected) failures.push("signature helper auth signature mismatch");
      if (payload.secret_value || payload.secret) failures.push("signature helper must not print secret material");
    } catch (error) {
      failures.push(`signature helper output is not JSON: ${error.message}`);
    }
  }
}

const dashboard = readText("docs/project-progress-dashboard.md");
if (!dashboard.includes("T3-B SDK/mock 套件")) failures.push("project dashboard must mention T3-B SDK/mock kit");
const entry = readText("docs/00-current-effective-document-entry-v0.1.md");
if (!entry.includes("verify:t3b-supplier-mock-kit")) failures.push("effective entry must mention verify:t3b-supplier-mock-kit");

if (failures.length > 0) {
  console.error("T3B_SUPPLIER_MOCK_KIT_FAIL");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log("T3B_SUPPLIER_MOCK_KIT_PASS");
