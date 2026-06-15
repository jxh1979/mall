import fs from 'node:fs';
import path from 'node:path';

const rootDir = process.cwd();
const strict = process.argv.includes('--strict');
const findings = [];

function read(relativeFile) {
  const filePath = path.join(rootDir, relativeFile);
  if (!fs.existsSync(filePath)) {
    findings.push(`${relativeFile}: missing file`);
    return '';
  }
  return fs.readFileSync(filePath, 'utf8');
}

function expectContains(relativeFile, source, needle, reason) {
  if (!source.includes(needle)) {
    findings.push(`${relativeFile}: missing ${reason}`);
  }
}

function expectNotContains(relativeFile, source, needle, reason) {
  if (source.includes(needle)) {
    findings.push(`${relativeFile}: forbidden ${reason}`);
  }
}

function hasCamelCase(value) {
  return /[a-z][A-Z]/.test(value);
}

function resolveSchema(openapi, schema) {
  if (!schema) return null;
  if (schema.$ref) {
    const name = schema.$ref.replace('#/components/schemas/', '');
    return openapi.components?.schemas?.[name] ?? null;
  }
  return schema;
}

function resolveRequestBody(openapi, requestBody) {
  let body = requestBody;
  if (body?.$ref) {
    const name = body.$ref.replace('#/components/requestBodies/', '');
    body = openapi.components?.requestBodies?.[name];
  }
  return resolveSchema(openapi, body?.content?.['application/json']?.schema);
}

function checkSchemaProperties(openapi, schema, label, visited = new Set()) {
  const resolved = resolveSchema(openapi, schema);
  if (!resolved || visited.has(resolved)) return;
  visited.add(resolved);

  for (const propertyName of Object.keys(resolved.properties ?? {})) {
    if (hasCamelCase(propertyName)) {
      findings.push(`${label}: request schema property must be snake_case: ${propertyName}`);
    }
  }

  for (const propertySchema of Object.values(resolved.properties ?? {})) {
    checkSchemaProperties(openapi, propertySchema, label, visited);
  }

  if (resolved.items) {
    checkSchemaProperties(openapi, resolved.items, label, visited);
  }

  for (const composed of [...(resolved.allOf ?? []), ...(resolved.oneOf ?? []), ...(resolved.anyOf ?? [])]) {
    checkSchemaProperties(openapi, composed, label, visited);
  }
}

const webFile = 'apps/web/src/main.tsx';
const androidClientFile = 'apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt';
const openapiFile = 'docs/openapi/openapi-p0.json';

const webSource = read(webFile);
expectContains(webFile, webSource, 'function normalizeOutgoingBody', 'outgoing snake_case request normalizer');
expectContains(webFile, webSource, 'function normalizeIncomingBody', 'incoming camelCase response normalizer');
expectContains(webFile, webSource, 'body: JSON.stringify(normalizeOutgoingBody(path, body))', 'normalized POST body');
expectNotContains(webFile, webSource, 'body: JSON.stringify(body)', 'direct POST JSON body without OpenAPI field normalization');

const androidSource = read(androidClientFile);
expectContains(androidClientFile, androidSource, 'JsonNamingStrategy.SnakeCase', 'kotlinx serialization snake_case naming strategy');
expectContains(androidClientFile, androidSource, 'normalizeJsonObjectKeys(body)', 'raw JsonObject request key normalizer');
expectContains(androidClientFile, androidSource, 'private fun normalizeJsonElementKeys', 'recursive raw JsonObject key normalizer');

const openapiPath = path.join(rootDir, openapiFile);
if (!fs.existsSync(openapiPath)) {
  findings.push(`${openapiFile}: missing file`);
} else {
  const openapi = JSON.parse(fs.readFileSync(openapiPath, 'utf8'));
  for (const [apiPath, pathItem] of Object.entries(openapi.paths ?? {})) {
    for (const [method, operation] of Object.entries(pathItem ?? {})) {
      if (!['post', 'put', 'patch', 'delete'].includes(method)) continue;
      const schema = resolveRequestBody(openapi, operation.requestBody);
      checkSchemaProperties(openapi, schema, `${method.toUpperCase()} ${apiPath}`);
    }
  }
}

if (findings.length === 0) {
  console.log('[client-payload-policy] PASS: client payload normalization and OpenAPI request field casing are enforced.');
  process.exit(0);
}

console.error(`[client-payload-policy] Found ${findings.length} payload policy issue(s).`);
for (const finding of findings) {
  console.error(`  ${finding}`);
}

if (strict) {
  console.error('\n[client-payload-policy] FAIL: strict mode blocks P0 verification until client payloads and request schemas follow the formal OpenAPI field contract.');
  process.exit(1);
}

console.error('\n[client-payload-policy] WARN: audit mode is informational. Use npm run verify:client-payload for the strict P0 gate.');
