import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const openapiPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const operationMethods = new Set(["get", "post", "put", "patch", "delete", "head", "options"]);
const allowedAuthContexts = new Set([
  "public",
  "staff-token",
  "customer-token",
  "device-credential",
  "open-app-credential",
  "webhook-signature",
]);
const allowedTerminalTypes = new Set([
  "app",
  "miniapp",
  "web",
  "tablet",
  "staff_work_phone",
  "staff_private_phone",
  "merchant_portal",
  "merchant_app",
  "kiosk",
  "mobile_admin",
]);
const allowedInvocationChannels = new Set([
  "server-to-server",
  "provider-webhook",
  "scheduled-polling",
  "system-job",
]);
const oldPathPrefixes = ["/api/admin", "/api/mobile", "/api/integration", "/api/auth"];
const productionReadinessStatuses = [
  "not_started",
  "pending_info",
  "in_progress",
  "blocked",
  "ready_for_test",
  "passed",
  "failed",
  "waived",
  "not_applicable",
];
const errors = [];

function fail(message) {
  errors.push(message);
}

function checkNoQuestionPlaceholder(value, location) {
  if (typeof value === "string") {
    if (/\?{2,}/.test(value)) {
      fail(`${location}: contains unreadable question-mark placeholder text`);
    }
    return;
  }

  if (!value || typeof value !== "object") {
    return;
  }

  for (const [key, child] of Object.entries(value)) {
    checkNoQuestionPlaceholder(child, `${location}.${key}`);
  }
}

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function checkProductionReadinessStatusEnum(openapi) {
  const enumValues = openapi.components?.schemas?.ProductionReadinessItemUpdateRequest?.properties?.status?.enum;
  const label = "components.schemas.ProductionReadinessItemUpdateRequest.properties.status.enum";
  if (!Array.isArray(enumValues)) {
    fail(`${label}: missing enum`);
    return;
  }

  const actual = [...enumValues].sort();
  const expected = [...productionReadinessStatuses].sort();
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    fail(`${label}: must be exactly ${productionReadinessStatuses.join(", ")}`);
  }
  if (enumValues.includes("pending")) {
    fail(`${label}: pending is not allowed; use pending_info or not_started`);
  }
}

function checkOperation(apiPath, method, operation) {
  const label = `${method.toUpperCase()} ${apiPath}`;

  if (!operation.operationId || typeof operation.operationId !== "string") {
    fail(`${label}: missing operationId`);
  }

  if (!operation["x-auth-context"] || !allowedAuthContexts.has(operation["x-auth-context"])) {
    fail(`${label}: invalid x-auth-context`);
  }

  if (!operation["x-permission"]) {
    fail(`${label}: missing x-permission`);
  }

  const terminals = asArray(operation["x-terminal-allowed"]);
  const invocationChannels = asArray(operation["x-invocation-channel"]);
  const isServerInvocation = ["open-app-credential", "webhook-signature"].includes(operation["x-auth-context"]);
  if (isServerInvocation) {
    if (invocationChannels.length === 0) {
      fail(`${label}: server invocation must declare x-invocation-channel`);
    }
    for (const channel of invocationChannels) {
      if (!allowedInvocationChannels.has(channel)) {
        fail(`${label}: invalid invocation channel ${channel}`);
      }
    }
  } else {
    if (terminals.length === 0) {
      fail(`${label}: missing x-terminal-allowed`);
    }
    for (const terminal of terminals) {
      if (!allowedTerminalTypes.has(terminal)) {
        fail(`${label}: invalid terminal ${terminal}`);
      }
    }
  }

  if (!operation["x-audit-event-success"]) {
    fail(`${label}: missing x-audit-event-success`);
  }
  if (!operation["x-audit-event-failure"]) {
    fail(`${label}: missing x-audit-event-failure`);
  }

  if (["post", "put", "patch", "delete"].includes(method) && operation["x-idempotency-required"] === undefined) {
    fail(`${label}: missing x-idempotency-required`);
  }

  const requestBodyText = JSON.stringify(operation.requestBody ?? {});
  if (requestBodyText.includes("GenericWrite")) {
    fail(`${label}: requestBody references GenericWrite`);
  }
  if (requestBodyText.includes("ClientPlaceholderRequest")) {
    fail(`${label}: requestBody references ClientPlaceholderRequest`);
  }
}

if (!fs.existsSync(openapiPath)) {
  fail(`OpenAPI file not found: ${openapiPath}`);
} else {
  const openapi = JSON.parse(fs.readFileSync(openapiPath, "utf8"));
  checkNoQuestionPlaceholder(openapi, "openapi");

  if (!openapi.openapi?.startsWith("3.")) {
    fail("OpenAPI version must be 3.x");
  }

  if (openapi.components?.schemas?.ClientPlaceholderRequest || openapi.components?.requestBodies?.ClientPlaceholderRequest) {
    fail("ClientPlaceholderRequest is not allowed in formal P0 OpenAPI; use a field-level request DTO");
  }

  checkProductionReadinessStatusEnum(openapi);

  for (const apiPath of Object.keys(openapi.paths ?? {})) {
    for (const prefix of oldPathPrefixes) {
      if (apiPath.startsWith(prefix)) {
        fail(`${apiPath}: old path prefix is not allowed in formal P0 OpenAPI`);
      }
    }

    const pathItem = openapi.paths[apiPath] ?? {};
    for (const [method, operation] of Object.entries(pathItem)) {
      if (!operationMethods.has(method)) {
        continue;
      }
      checkOperation(apiPath, method, operation);
    }
  }
}

if (errors.length > 0) {
  console.error("OpenAPI contract verification failed:");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log("OpenAPI contract verification passed.");
