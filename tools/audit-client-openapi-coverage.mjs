// 文件版本：V0.02
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const rootDir = process.cwd();
const strict = process.argv.includes('--strict');
const openapiPath = path.join(rootDir, 'docs', 'openapi', 'openapi-p0.json');

const scanFiles = [
  'apps/web/src/main.tsx',
  'apps/web/src/admin/adminPageConfigs.ts',
  'apps/web/src/admin/client/adminClient.ts',
  // ROOT-03-R1：三端共用的客人接口路径。
  'packages/customer-core/src/service.ts',
  'apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt',
  'apps/android-terminal/src/main/kotlin/com/complex/terminal/P0Fixture.kt',
  'apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt',
];

const apiPathPattern = /\/api\/(?:\$\{[^}\r\n]*\}|[A-Za-z0-9_?&=/:{}.\-$\[\]])*/g;

function normalizeApiPath(rawPath) {
  return rawPath.replace(/[),.;]+$/g, '').split('?')[0];
}

function isFormalClientPath(apiPath) {
  return (
    apiPath === '/api/v1' ||
    apiPath.startsWith('/api/v1/') ||
    apiPath === '/api/open' ||
    apiPath.startsWith('/api/open/')
  );
}

function segments(apiPath) {
  return apiPath.split('/').filter(Boolean);
}

function isWildcardSegment(segment) {
  return segment.startsWith('{') || segment.includes('${');
}

function matchesOpenApiPath(clientPath, contractPath) {
  const clientSegments = segments(clientPath);
  const contractSegments = segments(contractPath);

  if (clientSegments.length !== contractSegments.length) {
    return false;
  }

  return clientSegments.every((segment, index) => {
    const contractSegment = contractSegments[index];
    return (
      segment === contractSegment ||
      isWildcardSegment(segment) ||
      isWildcardSegment(contractSegment)
    );
  });
}

function lineNumberAt(source, index) {
  let line = 1;
  for (let i = 0; i < index; i += 1) {
    if (source.charCodeAt(i) === 10) {
      line += 1;
    }
  }
  return line;
}

const interpolationProbe = '/api/v1/staff/attendance/receipt-events/${urlEncode(id)}/reviews';
assert.equal([...interpolationProbe.matchAll(apiPathPattern)][0][0], interpolationProbe);
assert.equal(matchesOpenApiPath(interpolationProbe, '/api/v1/staff/attendance/receipt-events/{id}/reviews'), true);
assert.equal(matchesOpenApiPath(interpolationProbe, '/api/v1/staff/attendance/receipt-events/{id}/review'), false);

if (!fs.existsSync(openapiPath)) {
  console.error(`[client-openapi-coverage] Missing ${path.relative(rootDir, openapiPath)}`);
  process.exit(1);
}

const openapi = JSON.parse(fs.readFileSync(openapiPath, 'utf8'));
const contractPaths = Object.keys(openapi.paths ?? {});
const contractMethodsByPath = new Map(
  Object.entries(openapi.paths ?? {}).map(([apiPath, pathItem]) => [
    apiPath,
    new Set(Object.keys(pathItem ?? {}).filter((method) => ['get', 'post', 'put', 'patch', 'delete'].includes(method))),
  ]),
);
const clientRoutes = new Map();
const clientMethodRoutes = new Map();

function inferHttpMethod(source, index) {
  const before = source.slice(Math.max(0, index - 100), index);
  const after = source.slice(index, index + 160);

  if (/postJson\s*(?:<[^>]+>)?\s*\(\s*['"`]?$/s.test(before) || /post\s*(?:<[^>]+>)?\s*\(\s*['"`]?$/s.test(before)) {
    return 'post';
  }

  if (/getJson\s*(?:<[^>]+>)?\s*\(\s*['"`]?$/s.test(before) || /get\s*(?:<[^>]+>)?\s*\(\s*['"`]?$/s.test(before)) {
    return 'get';
  }

  if (/\.GET\(\)/s.test(after)) {
    return 'get';
  }

  return null;
}

for (const relativeFile of scanFiles) {
  const filePath = path.join(rootDir, relativeFile);

  if (!fs.existsSync(filePath)) {
    continue;
  }

  const source = fs.readFileSync(filePath, 'utf8');
  for (const match of source.matchAll(apiPathPattern)) {
    const apiPath = normalizeApiPath(match[0]);

    if (!isFormalClientPath(apiPath)) {
      continue;
    }

    if (!clientRoutes.has(apiPath)) {
      clientRoutes.set(apiPath, []);
    }

    clientRoutes.get(apiPath).push({
      file: relativeFile.replaceAll(path.sep, '/'),
      line: lineNumberAt(source, match.index ?? 0),
    });

    const httpMethod = inferHttpMethod(source, match.index ?? 0);
    if (httpMethod) {
      const methodRouteKey = `${httpMethod.toUpperCase()} ${apiPath}`;
      if (!clientMethodRoutes.has(methodRouteKey)) {
        clientMethodRoutes.set(methodRouteKey, {
          method: httpMethod,
          clientPath: apiPath,
          references: [],
        });
      }
      clientMethodRoutes.get(methodRouteKey).references.push({
        file: relativeFile.replaceAll(path.sep, '/'),
        line: lineNumberAt(source, match.index ?? 0),
      });
    }
  }
}

const uncovered = [];
const methodMismatches = [];

for (const [clientPath, references] of clientRoutes.entries()) {
  const covered = contractPaths.some((contractPath) => matchesOpenApiPath(clientPath, contractPath));

  if (!covered) {
    uncovered.push({ clientPath, references });
  }
}

for (const methodRoute of clientMethodRoutes.values()) {
  // 客户端通配段也会匹配契约里的字面段：任一匹配的契约路径声明了该方法即算覆盖。
  const candidates = contractPaths.filter((candidate) => matchesOpenApiPath(methodRoute.clientPath, candidate));
  const contractPath = candidates.find((candidate) => contractMethodsByPath.get(candidate)?.has(methodRoute.method)) ?? candidates[0];
  const contractMethods = contractPath ? contractMethodsByPath.get(contractPath) : null;

  if (contractPath && contractMethods && !contractMethods.has(methodRoute.method)) {
    methodMismatches.push({
      ...methodRoute,
      contractPath,
      contractMethods: [...contractMethods].map((method) => method.toUpperCase()).sort(),
    });
  }
}

uncovered.sort((a, b) => a.clientPath.localeCompare(b.clientPath));
methodMismatches.sort((a, b) => `${a.method} ${a.clientPath}`.localeCompare(`${b.method} ${b.clientPath}`));

if (uncovered.length === 0 && methodMismatches.length === 0) {
  console.log(`[client-openapi-coverage] PASS: ${clientRoutes.size} client route template(s) are covered by OpenAPI; ${clientMethodRoutes.size} method-specific call(s) match OpenAPI methods.`);
  process.exit(0);
}

if (uncovered.length > 0) {
  console.error(
    `[client-openapi-coverage] Found ${uncovered.length} client route template(s) not covered by docs/openapi/openapi-p0.json.`,
  );
  console.error(
    `[client-openapi-coverage] Covered ${clientRoutes.size - uncovered.length}/${clientRoutes.size} formal client route template(s).`,
  );

  for (const finding of uncovered) {
    const refs = finding.references
      .map((reference) => `${reference.file}:${reference.line}`)
      .join(', ');
    console.error(`  ${finding.clientPath}  ${refs}`);
  }
}

if (methodMismatches.length > 0) {
  console.error(
    `[client-openapi-coverage] Found ${methodMismatches.length} client method call(s) whose HTTP method is not declared on the matching OpenAPI path.`,
  );

  for (const finding of methodMismatches) {
    const refs = finding.references
      .map((reference) => `${reference.file}:${reference.line}`)
      .join(', ');
    console.error(`  ${finding.method.toUpperCase()} ${finding.clientPath}  contract=${finding.contractPath} methods=${finding.contractMethods.join('|')}  ${refs}`);
  }
}

if (strict) {
  console.error('\n[client-openapi-coverage] FAIL: strict mode blocks formal P0 verification until client paths and HTTP methods are in OpenAPI or removed from P0 client code.');
  process.exit(1);
}

console.error('\n[client-openapi-coverage] WARN: audit mode is informational. Use npm run verify:client-openapi for the strict P0 gate.');
