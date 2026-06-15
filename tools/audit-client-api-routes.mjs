import fs from 'node:fs';
import path from 'node:path';

const rootDir = process.cwd();
const strict = process.argv.includes('--strict');

const scanTargets = [
  {
    name: 'web',
    dir: path.join(rootDir, 'apps', 'web', 'src'),
    extensions: new Set(['.ts', '.tsx', '.js', '.jsx']),
  },
  {
    name: 'android-terminal',
    dir: path.join(rootDir, 'apps', 'android-terminal'),
    extensions: new Set(['.kt', '.kts', '.java']),
  },
];

const ignoredSegments = [
  `${path.sep}node_modules${path.sep}`,
  `${path.sep}dist${path.sep}`,
  `${path.sep}build${path.sep}`,
  `${path.sep}.gradle${path.sep}`,
];

const apiPathPattern = /\/api\/[A-Za-z0-9_?&=/:{}.${}\-[\]]*/g;

function walkFiles(dir, extensions, result = []) {
  if (!fs.existsSync(dir)) {
    return result;
  }

  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);

    if (ignoredSegments.some((segment) => fullPath.includes(segment))) {
      continue;
    }

    if (entry.isDirectory()) {
      walkFiles(fullPath, extensions, result);
      continue;
    }

    if (entry.isFile() && extensions.has(path.extname(entry.name))) {
      result.push(fullPath);
    }
  }

  return result;
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

function normalizeApiPath(rawPath) {
  return rawPath.replace(/[),.;]+$/g, '');
}

function isFormalApiPath(apiPath) {
  return (
    apiPath === '/api/v1' ||
    apiPath.startsWith('/api/v1/') ||
    apiPath === '/api/open' ||
    apiPath.startsWith('/api/open/')
  );
}

const findings = [];

for (const target of scanTargets) {
  const files = walkFiles(target.dir, target.extensions);

  for (const filePath of files) {
    const source = fs.readFileSync(filePath, 'utf8');
    const matches = source.matchAll(apiPathPattern);

    for (const match of matches) {
      const apiPath = normalizeApiPath(match[0]);

      if (!isFormalApiPath(apiPath)) {
        findings.push({
          target: target.name,
          file: path.relative(rootDir, filePath).replaceAll(path.sep, '/'),
          line: lineNumberAt(source, match.index ?? 0),
          apiPath,
        });
      }
    }
  }
}

const grouped = new Map();

for (const finding of findings) {
  const key = finding.file;
  if (!grouped.has(key)) {
    grouped.set(key, []);
  }
  grouped.get(key).push(finding);
}

if (findings.length === 0) {
  console.log('[client-api-route-audit] PASS: client code only references formal /api/v1 or /api/open paths.');
  process.exit(0);
}

console.error(`[client-api-route-audit] Found ${findings.length} non-formal client API route reference(s).`);
console.error('[client-api-route-audit] Formal P0 clients must use /api/v1/* or /api/open/* from docs/openapi/openapi-p0.json.');

for (const [file, fileFindings] of grouped.entries()) {
  console.error(`\n${file}`);

  const unique = new Map();
  for (const finding of fileFindings) {
    const key = `${finding.apiPath}`;
    if (!unique.has(key)) {
      unique.set(key, []);
    }
    unique.get(key).push(finding.line);
  }

  let printed = 0;
  for (const [apiPath, lines] of unique.entries()) {
    printed += 1;
    if (printed > 30) {
      console.error(`  ... ${unique.size - 30} more route(s) omitted in this file`);
      break;
    }

    const firstLines = [...new Set(lines)].slice(0, 8).join(', ');
    console.error(`  ${apiPath}  line(s): ${firstLines}`);
  }
}

if (strict) {
  console.error('\n[client-api-route-audit] FAIL: strict mode blocks formal P0 verification until these routes are migrated.');
  process.exit(1);
}

console.error('\n[client-api-route-audit] WARN: audit mode is informational. Use npm run verify:client-routes for the strict P0 gate.');
