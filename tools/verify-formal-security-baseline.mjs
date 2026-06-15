import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const strict = process.argv.includes("--strict");
const openapiPath = path.join(root, "docs", "openapi", "openapi-p0.json");
const baselinePath = path.join(root, "security-scans", "formal-security-baseline-v1.md");
const findingsPath = path.join(root, "security-scans", "findings-register-v1.md");
const packageJsonPath = path.join(root, "package.json");
const nasRestoreDrillEvidencePath = path.join(root, "docs", "production-readiness-evidence", "nas-restore-drill-latest.json");
const productionAuthArtifacts = [
  path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "auth", "ProductionAuthService.java"),
  path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "auth", "ProductionAuthController.java"),
  path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "auth", "ProductionBearerAuthenticationFilter.java"),
  path.join(root, "backend", "src", "main", "resources", "db", "migration", "V018__p0_production_auth_provider.sql"),
  path.join(root, "backend", "src", "test", "java", "com", "complex", "platform", "auth", "ProductionAuthJdbcIntegrationTests.java"),
];
const productionAuthProviderPresent = productionAuthArtifacts.every((artifact) => fs.existsSync(artifact));
const nasRestoreDrillEvidencePresent = fs.existsSync(nasRestoreDrillEvidencePath);
const operationMethods = new Set(["get", "post", "put", "patch", "delete", "head", "options"]);
const errors = [];

const requiredBaselineSections = [
  "threat-model-v1",
  "authz-review-v1",
  "openapi-exposure-review-v1",
  "webhook-signature-review-v1",
  "payment-risk-review-v1",
  "pii-retention-review-v1",
  "device-mdm-review-v1",
  "ops-160-review-v1",
  "dependency-sbom-review-v1",
  "findings-register-v1",
];

function fail(message) {
  errors.push(message);
}

function readRequired(file) {
  if (!fs.existsSync(file)) {
    fail(`missing required artifact: ${path.relative(root, file)}`);
    return "";
  }
  return fs.readFileSync(file, "utf8");
}

function operationCount(openapi) {
  let count = 0;
  for (const pathItem of Object.values(openapi.paths ?? {})) {
    for (const method of Object.keys(pathItem ?? {})) {
      if (operationMethods.has(method)) {
        count += 1;
      }
    }
  }
  return count;
}

function checkNoSecretValues(label, text) {
  const secretPatterns = [
    /-----BEGIN (?:RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/i,
    /\b(?:P0_ADMIN_PASSWORD|POSTGRES_PASSWORD|MINIO_SECRET_KEY|MINIO_ROOT_PASSWORD|AWS_SECRET_ACCESS_KEY|ALIYUN_ACCESS_KEY_SECRET|API_SECRET|SECRET_KEY)\s*[:=]\s*["']?[A-Za-z0-9_./+=-]{8,}/i,
    /\bsk-[A-Za-z0-9_-]{20,}\b/,
  ];

  for (const pattern of secretPatterns) {
    if (pattern.test(text)) {
      fail(`${label}: contains a value that looks like a pasted secret`);
    }
  }
}

if (!fs.existsSync(openapiPath)) {
  fail(`missing OpenAPI contract: ${path.relative(root, openapiPath)}`);
} else {
  const packageJson = JSON.parse(readRequired(packageJsonPath));
  const openapi = JSON.parse(fs.readFileSync(openapiPath, "utf8"));
  const count = operationCount(openapi);
  const baseline = readRequired(baselinePath);
  const findings = readRequired(findingsPath);

  if (!packageJson.scripts?.["verify:p0"]?.includes("verify:security-baseline")) {
    fail("package.json verify:p0 must include verify:security-baseline");
  }

  if (!packageJson.scripts?.["verify:ops160-recovery"]?.includes("verify:ops160-execution-policy")) {
    fail("package.json verify:ops160-recovery must include verify:ops160-execution-policy");
  }

  if (baseline) {
    for (const section of requiredBaselineSections) {
      if (!baseline.includes(`## ${section}`)) {
        fail(`formal baseline missing section: ${section}`);
      }
    }

    if (!baseline.includes(`OpenAPI operation count: ${count}`)) {
      fail(`formal baseline operation count is not in sync with OpenAPI count ${count}`);
    }

    if (!baseline.includes("Java 21 + Spring Boot 4.x + PostgreSQL + Flyway")) {
      fail("formal baseline must use the Java/Spring/PostgreSQL/Flyway formal stack wording");
    }

    if (!baseline.includes("192.168.88.113")) {
      fail("formal baseline must mention the confirmed NAS server 192.168.88.113");
    }

    if (/TypeScript\/Fastify\/SQLite[^\n]*(current|formal|production|baseline)/i.test(baseline)) {
      fail("formal baseline appears to reuse legacy TypeScript/Fastify/SQLite as a current basis");
    }

    if (nasRestoreDrillEvidencePresent && !baseline.includes("restore.drill_completed")) {
      fail("formal baseline must mention restore.drill_completed when NAS restore drill evidence exists");
    }

    checkNoSecretValues("formal baseline", baseline);
  }

  if (findings) {
    if (!findings.includes(`OpenAPI operation count: ${count}`)) {
      fail(`findings register operation count is not in sync with OpenAPI count ${count}`);
    }

    if (!findings.includes("192.168.88.113")) {
      fail("findings register must mention the confirmed NAS server 192.168.88.113");
    }

    for (const severity of ["P0", "P1", "P2", "P3"]) {
      if (!findings.includes(`| ${severity} |`)) {
        fail(`findings register missing ${severity} severity row`);
      }
    }

    if (!/SEC-P2-CREDENTIALS-001[\s\S]*?\|\s*resolved\s*\|/.test(findings)) {
      fail("findings register must mark SEC-P2-CREDENTIALS-001 as resolved");
    }

    if (productionAuthProviderPresent) {
      if (!/SEC-P0-AUTH-PROD-001[\s\S]*?\|\s*resolved\s*\|/.test(findings)) {
        fail("findings register must mark SEC-P0-AUTH-PROD-001 as resolved after the production auth provider lands");
      }
    } else if (!/SEC-P0-AUTH-PROD-001[\s\S]*?\|\s*open\s*\|/.test(findings)) {
      fail("findings register must keep SEC-P0-AUTH-PROD-001 open before the production auth provider lands");
    }

    if (!/SEC-P1-DOCS-EXPOSURE-001[\s\S]*?\|\s*resolved\s*\|/.test(findings)) {
      fail("findings register must mark SEC-P1-DOCS-EXPOSURE-001 as resolved after prod docs access protection lands");
    }

    if (nasRestoreDrillEvidencePresent && !findings.includes("restore.drill_completed")) {
      fail("findings register must mention restore.drill_completed when NAS restore drill evidence exists");
    }

    checkNoSecretValues("findings register", findings);
  }
}

if (errors.length > 0) {
  console.error("Formal security baseline verification failed:");
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  if (strict) {
    process.exit(1);
  }
}

console.log(`Formal security baseline verification passed: ${path.relative(root, baselinePath)} and ${path.relative(root, findingsPath)}.`);
