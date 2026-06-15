import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const failures = [];

function read(relativePath) {
  const filePath = path.join(root, relativePath);
  if (!fs.existsSync(filePath)) {
    failures.push(relativePath + ': missing file');
    return '';
  }
  return fs.readFileSync(filePath, 'utf8');
}

function expectContains(file, source, needle, reason) {
  if (!source.includes(needle)) failures.push(file + ': missing ' + reason);
}

const webFile = 'apps/web/src/main.tsx';
const packageFile = 'package.json';
const openapiFile = 'docs/openapi/openapi-p0.json';
const web = read(webFile);
const pkgText = read(packageFile);
const pkg = pkgText ? JSON.parse(pkgText) : {};
const openapiText = read(openapiFile);
const openapi = openapiText ? JSON.parse(openapiText) : { paths: {}, components: {} };

expectContains(packageFile, JSON.stringify(pkg.scripts || {}), 'verify:p2-floor-plan-web-entry', 'verification script entry');
expectContains(webFile, web, 'floorplan', 'floorplan SectionKey value');
expectContains(webFile, web, 'children: ["foundation", "floorplan", "interfaces", "security", "events"]', 'floorplan navigation entry under platform governance');
expectContains(webFile, web, 'P2 \u5e73\u9762\u56fe\u5bfc\u5165', 'P2 floor plan import visible workbench heading');
expectContains(webFile, web, '\u5546\u573a\u5e73\u9762\u56fe', 'floor plan menu label');
expectContains(webFile, web, 'floorPlans:', 'floor plan snapshot collection');
expectContains(webFile, web, 'floorPlanImportDetails', 'floor plan import candidate/error details state');
expectContains(webFile, web, '/api/v1/floor-plans', 'floor plan list/create API reference');
expectContains(webFile, web, '/api/v1/floor-plans/${floorPlanId}/imports', 'floor plan import create/list API reference');
expectContains(webFile, web, '/api/v1/floor-plans/${floorPlanId}/imports/${importId}/candidates', 'floor plan import details API reference');
expectContains(webFile, web, '/api/v1/floor-plans/${floorPlanId}/imports/${importId}/publish', 'floor plan import publish API reference');
expectContains(webFile, web, 'confirmHighImpactAction', 'manual confirmation function usage');
expectContains(webFile, web, '\u53d1\u5e03\u5e73\u9762\u56fe\u5bfc\u5165\u5019\u9009', 'floor plan publish confirmation title');
expectContains(webFile, web, '\u5019\u9009 ID', 'candidate id field label');

const requiredPaths = [
  '/api/v1/floor-plans',
  '/api/v1/floor-plans/{id}/shapes',
  '/api/v1/floor-plans/{id}/imports',
  '/api/v1/floor-plans/{id}/imports/{importId}/candidates',
  '/api/v1/floor-plans/{id}/imports/{importId}/publish',
];
for (const apiPath of requiredPaths) {
  if (!openapi.paths || !openapi.paths[apiPath]) failures.push(openapiFile + ': missing path ' + apiPath);
}

const requestBodies = openapi.components && openapi.components.requestBodies ? openapi.components.requestBodies : {};
const requiredRequestBodies = [
  'FloorPlanCreateRequest',
  'FloorPlanImportCreateRequest',
  'FloorPlanImportPublishRequest',
];
for (const requestBody of requiredRequestBodies) {
  if (!requestBodies[requestBody]) failures.push(openapiFile + ': missing request body ' + requestBody);
}

if (failures.length > 0) {
  console.error('[p2-floor-plan-web-entry] FAIL');
  for (const failure of failures) console.error('- ' + failure);
  process.exit(1);
}

console.log('[p2-floor-plan-web-entry] PASS: Web entry, navigation, API references, publish confirmation, and OpenAPI coverage are present.');
