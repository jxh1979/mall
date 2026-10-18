import fs from "node:fs";
import path from "node:path";
import YAML from "yaml";

const root = process.cwd();
const openapiPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const openapiYamlPath = path.join(root, "docs", "openapi", "openapi-p0.yaml");
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
  // CG-11d：受控第十一端——独立经销商 Web 门户（ENUM:233；V454 终端闭集）。
  "dealer_portal",
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

function checkNoEmptyKeys(value, location) {
  if (!value || typeof value !== "object") {
    return;
  }
  for (const [key, child] of Object.entries(value)) {
    if (key === "") {
      fail(`${location}: empty object key (likely a lost "$ref")`);
    }
    if (child && typeof child === "object") {
      checkNoEmptyKeys(child, `${location}.${key}`);
    }
  }
}

function resolvePointer(doc, ref) {
  if (!ref.startsWith("#/")) {
    return undefined;
  }
  let node = doc;
  for (const rawSegment of ref.slice(2).split("/")) {
    const segment = rawSegment.replaceAll("~1", "/").replaceAll("~0", "~");
    if (!node || typeof node !== "object" || !(segment in node)) {
      return undefined;
    }
    node = node[segment];
  }
  return node;
}

function checkRefsResolve(doc, value, location) {
  if (!value || typeof value !== "object") {
    return;
  }
  for (const [key, child] of Object.entries(value)) {
    if (key === "$ref") {
      if (typeof child !== "string" || !child.startsWith("#/")) {
        fail(`${location}.$ref: must be an internal "#/..." reference, got ${JSON.stringify(child)}`);
      } else if (resolvePointer(doc, child) === undefined) {
        fail(`${location}.$ref: unresolvable reference ${child}`);
      }
    }
    if (child && typeof child === "object") {
      checkRefsResolve(doc, child, `${location}.${key}`);
    }
  }
}

function checkOperationIdUniqueness(doc, label) {
  const seen = new Map();
  for (const [apiPath, pathItem] of Object.entries(doc.paths ?? {})) {
    for (const [method, operation] of Object.entries(pathItem ?? {})) {
      if (!operationMethods.has(method) || !operation?.operationId) {
        continue;
      }
      const existing = seen.get(operation.operationId);
      const here = `${method.toUpperCase()} ${apiPath}`;
      if (existing) {
        fail(`${label}: duplicate operationId ${operation.operationId} (${existing} vs ${here})`);
      } else {
        seen.set(operation.operationId, here);
      }
    }
  }
}

function checkDocumentIntegrity(doc, label) {
  checkNoEmptyKeys(doc, label);
  checkRefsResolve(doc, doc, label);
  checkOperationIdUniqueness(doc, label);
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

// json↔yaml 深等价：两份契约的 paths+components 必须逐值一致（键序不计），防止只改一边的漂移再次发生。
function checkJsonYamlEquivalence(jsonDoc, yamlDoc) {
  const diffs = [];
  const typeOf = (v) => (v === null ? "null" : Array.isArray(v) ? "array" : typeof v);
  function walk(a, b, location) {
    const ta = typeOf(a);
    const tb = typeOf(b);
    if (ta !== tb) {
      diffs.push(`${location}: type ${ta} (json) vs ${tb} (yaml)`);
      return;
    }
    if (ta === "array") {
      if (a.length !== b.length) {
        diffs.push(`${location}: array length ${a.length} (json) vs ${b.length} (yaml)`);
        return;
      }
      a.forEach((item, i) => walk(item, b[i], `${location}[${i}]`));
      return;
    }
    if (ta === "object") {
      for (const key of Object.keys(a)) {
        if (key in b) {
          walk(a[key], b[key], `${location}.${key}`);
        } else {
          diffs.push(`${location}.${key}: missing in yaml`);
        }
      }
      for (const key of Object.keys(b)) {
        if (!(key in a)) {
          diffs.push(`${location}.${key}: missing in json (extra in yaml)`);
        }
      }
      return;
    }
    if (a !== b) {
      diffs.push(`${location}: ${JSON.stringify(a)} (json) vs ${JSON.stringify(b)} (yaml)`);
    }
  }
  walk(jsonDoc.paths ?? {}, yamlDoc.paths ?? {}, "paths");
  walk(jsonDoc.components ?? {}, yamlDoc.components ?? {}, "components");
  if (diffs.length > 0) {
    fail(`openapi-p0.json vs openapi-p0.yaml: ${diffs.length} deep-equality diff(s) in paths/components; first ${Math.min(diffs.length, 10)}:`);
    for (const diff of diffs.slice(0, 10)) {
      fail(`  ${diff}`);
    }
  }
}

let openapiJsonDoc = null;

if (!fs.existsSync(openapiPath)) {
  fail(`OpenAPI file not found: ${openapiPath}`);
} else {
  const openapi = JSON.parse(fs.readFileSync(openapiPath, "utf8"));
  openapiJsonDoc = openapi;
  checkNoQuestionPlaceholder(openapi, "openapi");
  checkDocumentIntegrity(openapi, "openapi-p0.json");

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

if (!fs.existsSync(openapiYamlPath)) {
  fail(`OpenAPI YAML file not found: ${openapiYamlPath}`);
} else {
  try {
    const openapiYaml = YAML.parse(fs.readFileSync(openapiYamlPath, "utf8"), { maxAliasCount: -1 });
    checkNoQuestionPlaceholder(openapiYaml, "openapi-p0.yaml");
    checkDocumentIntegrity(openapiYaml, "openapi-p0.yaml");
    if (openapiJsonDoc) {
      checkJsonYamlEquivalence(openapiJsonDoc, openapiYaml);
    }
  } catch (error) {
    fail(`openapi-p0.yaml: failed to parse (${error.message})`);
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
