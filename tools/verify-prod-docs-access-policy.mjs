import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = process.cwd();

function readRequired(relativePath) {
  const file = path.join(root, relativePath);
  if (!fs.existsSync(file)) {
    throw new Error(`${relativePath} is missing`);
  }
  return fs.readFileSync(file, 'utf8');
}

function requireIncludes(content, needle, label) {
  if (!content.includes(needle)) {
    throw new Error(`${label} missing required marker: ${needle}`);
  }
}

const securityConfig = readRequired('backend/src/main/java/com/complex/platform/config/SecurityConfig.java');
const productionAuthTest = readRequired('backend/src/test/java/com/complex/platform/auth/ProductionAuthJdbcIntegrationTests.java');
const packageJson = JSON.parse(readRequired('package.json'));

const directPermitAllDocs = /requestMatchers\(\s*[\s\S]*"\/docs\/\*\*"[\s\S]*"\/swagger-ui\/\*\*"[\s\S]*"\/v3\/api-docs\/\*\*"[\s\S]*\)\.permitAll\(\)/.test(securityConfig);
if (directPermitAllDocs) {
  throw new Error('SecurityConfig must not directly permitAll OpenAPI/Swagger docs for every profile');
}

for (const marker of [
  'Environment environment',
  'Profiles.of("prod")',
  'apiDocumentationPaths()',
  'publicApiPaths()',
  '"/docs"',
  '"/docs/**"',
  '"/swagger-ui.html"',
  '"/swagger-ui/**"',
  '"/v3/api-docs"',
  '"/v3/api-docs/**"',
]) {
  requireIncludes(securityConfig, marker, 'SecurityConfig');
}

requireIncludes(
  productionAuthTest,
  'prodProfileProtectsOpenApiAndSwaggerEndpointsFromAnonymousAccess',
  'ProductionAuthJdbcIntegrationTests'
);
requireIncludes(productionAuthTest, 'expectAnonymousAccessProtected("/v3/api-docs")', 'ProductionAuthJdbcIntegrationTests');
requireIncludes(productionAuthTest, 'expectAnonymousAccessProtected("/swagger-ui/index.html")', 'ProductionAuthJdbcIntegrationTests');
requireIncludes(productionAuthTest, 'expectAnonymousAccessProtected("/docs/openapi")', 'ProductionAuthJdbcIntegrationTests');
requireIncludes(productionAuthTest, 'expectAnonymousAccessProtected', 'ProductionAuthJdbcIntegrationTests');
requireIncludes(productionAuthTest, 'isIn(401, 403)', 'ProductionAuthJdbcIntegrationTests');

if (packageJson.scripts?.['verify:prod-docs-access'] !== 'node tools/verify-prod-docs-access-policy.mjs') {
  throw new Error('package.json must expose verify:prod-docs-access');
}

console.log('production OpenAPI/Swagger docs access policy verification passed.');
