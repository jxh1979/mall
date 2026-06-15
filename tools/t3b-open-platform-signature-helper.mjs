#!/usr/bin/env node
// T3B_SIGNATURE_HELPER_READY
import { createHash, createHmac } from "node:crypto";

function usage() {
  console.error(`Usage:
  node tools/t3b-open-platform-signature-helper.mjs auth --app-key <key> --timestamp <ms> --nonce <nonce> [--key-env T3B_MOCK_SIGNING_KEY]
  node tools/t3b-open-platform-signature-helper.mjs webhook --endpoint-key <key> --provider <provider> --event <event> --idempotency-key <id> --payload-json <json> [--key-env T3B_MOCK_SIGNING_KEY]`);
}

function readArg(name, fallback) {
  const index = process.argv.indexOf(name);
  if (index === -1) return fallback;
  return process.argv[index + 1];
}

function requireArg(name) {
  const value = readArg(name);
  if (!value) {
    usage();
    throw new Error(`${name} is required`);
  }
  return value;
}

function stableStringify(value) {
  if (value === null || typeof value !== "object") return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map((item) => stableStringify(item)).join(",")}]`;
  return `{${Object.keys(value).sort().map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`).join(",")}}`;
}

function sha256Hex(value) {
  return createHash("sha256").update(value).digest("hex");
}

function hmacSha256Hex(key, canonical) {
  return createHmac("sha256", key).update(canonical).digest("hex");
}

function openAuthCanonical({ appKey, timestamp, nonce }) {
  return `app_key=${appKey}\nnonce=${nonce}\ntimestamp=${timestamp}`;
}

function webhookCanonical({ endpointKey, provider, event, idempotencyKey, payloadHash }) {
  return `endpoint_key=${endpointKey}\nevent=${event}\nidempotency_key=${idempotencyKey}\npayload_hash=${payloadHash}\nprovider=${provider}`;
}

function readSigningKey() {
  const keyEnv = readArg("--key-env", "T3B_MOCK_SIGNING_KEY");
  const key = process.env[keyEnv];
  if (!key) throw new Error(`${keyEnv} environment variable is required; do not pass signing material on the command line`);
  return { keyEnv, key };
}

function parsePayloadJson(rawValue) {
  if (!rawValue) return {};
  try {
    return JSON.parse(rawValue);
  } catch (error) {
    throw new Error(`--payload-json is not valid JSON: ${error.message}`);
  }
}

function main() {
  const mode = process.argv[2];
  const { keyEnv, key } = readSigningKey();
  if (mode === "auth") {
    const appKey = requireArg("--app-key");
    const timestamp = requireArg("--timestamp");
    const nonce = requireArg("--nonce");
    const canonical = openAuthCanonical({ appKey, timestamp, nonce });
    console.log(JSON.stringify({ mode, algorithm: "HMAC-SHA256", keyEnv, canonical, signature: hmacSha256Hex(key, canonical) }, null, 2));
    return;
  }
  if (mode === "webhook") {
    const endpointKey = requireArg("--endpoint-key");
    const provider = requireArg("--provider");
    const event = requireArg("--event");
    const idempotencyKey = requireArg("--idempotency-key");
    const payload = parsePayloadJson(readArg("--payload-json", "{}"));
    const payloadJson = stableStringify(payload);
    const payloadHash = sha256Hex(payloadJson);
    const canonical = webhookCanonical({ endpointKey, provider, event, idempotencyKey, payloadHash });
    console.log(JSON.stringify({ mode, algorithm: "HMAC-SHA256", keyEnv, payloadHash, canonical, signature: hmacSha256Hex(key, canonical) }, null, 2));
    return;
  }
  usage();
  throw new Error("mode must be auth or webhook");
}

try {
  main();
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
}
