import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const methods = ["get", "post", "put", "patch", "delete", "head", "options"];

const paths = {
  openapi: path.join(root, "docs", "openapi", "openapi-p0.json"),
  task: path.join(root, "docs", "formal-security-baseline-generation-task-v0.1.md"),
  ledger: path.join(root, "docs", "production-readiness-implementation-ledger-template-v0.1.md"),
  credentialRunbook: path.join(root, "docs", "production-credential-rotation-runbook-v0.1.md"),
  nasRunbook: path.join(root, "docs", "nas-backup-and-restore-runbook-v0.1.md"),
  server160Runbook: path.join(root, "docs", "server-160-runbook-v0.2.md"),
  packageJson: path.join(root, "package.json"),
  pom: path.join(root, "backend", "pom.xml"),
  authService: path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "auth", "AuthService.java"),
  bearerFilter: path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "auth", "BearerAuthenticationFilter.java"),
  productionAuthService: path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "auth", "ProductionAuthService.java"),
  productionAuthController: path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "auth", "ProductionAuthController.java"),
  productionBearerFilter: path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "auth", "ProductionBearerAuthenticationFilter.java"),
  productionAuthMigration: path.join(root, "backend", "src", "main", "resources", "db", "migration", "V018__p0_production_auth_provider.sql"),
  productionAuthTest: path.join(root, "backend", "src", "test", "java", "com", "complex", "platform", "auth", "ProductionAuthJdbcIntegrationTests.java"),
  dataIsolationSpec: path.join(root, "docs", "data-isolation-and-tenant-scope-security-spec-v0.1.md"),
  domainOwnershipScopeCsv: path.join(root, "docs", "domain-ownership-scope-audit-v0.1.csv"),
  tenantIsolationVerifier: path.join(root, "tools", "verify-tenant-isolation.mjs"),
  tenantIsolationBaseline: path.join(root, "security-scans", "tenant-isolation-baseline.json"),
  ops160BackupScript: path.join(root, "ops", "160", "complex-backup.sh"),
  ops160RestoreDrillScript: path.join(root, "ops", "160", "complex-restore-drill.sh"),
  ops160EnvExample: path.join(root, "ops", "160", "complex.env.example"),
  nasRestoreDrillEvidence: path.join(root, "docs", "production-readiness-evidence", "nas-restore-drill-latest.json"),
  securityConfig: path.join(root, "backend", "src", "main", "java", "com", "complex", "platform", "config", "SecurityConfig.java"),
  baseline: path.join(root, "security-scans", "formal-security-baseline-v1.md"),
  findings: path.join(root, "security-scans", "findings-register-v1.md"),
};

function readRequired(file) {
  if (!fs.existsSync(file)) {
    throw new Error(`missing required input: ${path.relative(root, file)}`);
  }
  return fs.readFileSync(file, "utf8");
}

function readOptional(file) {
  return fs.existsSync(file) ? fs.readFileSync(file, "utf8") : "";
}

function sha256(text) {
  return crypto.createHash("sha256").update(text).digest("hex");
}

function escapeCell(value) {
  return String(value ?? "-").replace(/\r?\n/g, "<br>").replace(/\|/g, "\\|");
}

function countBy(values) {
  const result = new Map();
  for (const value of values) {
    result.set(value, (result.get(value) ?? 0) + 1);
  }
  return result;
}

function markdownCountTable(map, firstHeader, secondHeader) {
  return [
    `| ${firstHeader} | ${secondHeader} |`,
    "|---|---:|",
    ...[...map.entries()]
      .sort((a, b) => String(a[0]).localeCompare(String(b[0])))
      .map(([key, count]) => `| \`${escapeCell(key)}\` | ${count} |`),
  ].join("\n");
}

function extractFirst(text, pattern, fallback = "unknown") {
  return pattern.exec(text)?.[1] ?? fallback;
}

function parseOperations(openapi) {
  const operations = [];
  for (const [apiPath, pathItem] of Object.entries(openapi.paths ?? {})) {
    for (const method of methods) {
      const operation = pathItem?.[method];
      if (!operation) {
        continue;
      }
      operations.push({
        method: method.toUpperCase(),
        path: apiPath,
        operationId: operation.operationId ?? "-",
        tag: operation.tags?.[0] ?? "-",
        authContext: operation["x-auth-context"] ?? "-",
        permission: operation["x-permission"] ?? "-",
        dataScope: operation["x-data-scope"] ?? "-",
        idempotency: operation["x-idempotency-required"] === true,
        successEvent: operation["x-audit-event-success"] ?? "-",
        failureEvent: operation["x-audit-event-failure"] ?? "-",
        terminalAllowed: Array.isArray(operation["x-terminal-allowed"]) ? operation["x-terminal-allowed"] : [],
        invocationChannel: Array.isArray(operation["x-invocation-channel"]) ? operation["x-invocation-channel"] : [],
      });
    }
  }
  return operations;
}

const openapiText = readRequired(paths.openapi);
const openapi = JSON.parse(openapiText);
const taskText = readRequired(paths.task);
const ledgerText = readRequired(paths.ledger);
const credentialRunbookText = readRequired(paths.credentialRunbook);
const packageJson = JSON.parse(readRequired(paths.packageJson));
const pom = readRequired(paths.pom);
const authService = readRequired(paths.authService);
const bearerFilter = readRequired(paths.bearerFilter);
const productionAuthService = readOptional(paths.productionAuthService);
const productionAuthController = readOptional(paths.productionAuthController);
const productionBearerFilter = readOptional(paths.productionBearerFilter);
const productionAuthMigration = readOptional(paths.productionAuthMigration);
const productionAuthTest = readOptional(paths.productionAuthTest);
const dataIsolationSpec = readRequired(paths.dataIsolationSpec);
const domainOwnershipScopeCsv = readRequired(paths.domainOwnershipScopeCsv);
const tenantIsolationVerifier = readRequired(paths.tenantIsolationVerifier);
const tenantIsolationBaseline = readOptional(paths.tenantIsolationBaseline);
const ops160BackupScript = readOptional(paths.ops160BackupScript);
const ops160RestoreDrillScript = readOptional(paths.ops160RestoreDrillScript);
const ops160EnvExample = readOptional(paths.ops160EnvExample);
const nasRunbookText = readOptional(paths.nasRunbook);
const server160RunbookText = readOptional(paths.server160Runbook);
const nasRestoreDrillEvidenceText = readOptional(paths.nasRestoreDrillEvidence);
const securityConfig = readRequired(paths.securityConfig);
const operations = parseOperations(openapi);
const writeOperations = operations.filter((operation) => ["POST", "PUT", "PATCH", "DELETE"].includes(operation.method));
const externalOperations = operations.filter((operation) => ["open-app-credential", "webhook-signature"].includes(operation.authContext));
const publicOperations = operations.filter((operation) => operation.authContext === "public");
const authContextCounts = countBy(operations.map((operation) => operation.authContext));
const terminalCounts = countBy(operations.flatMap((operation) => operation.terminalAllowed.length ? operation.terminalAllowed : operation.invocationChannel));

const springBootVersion = extractFirst(pom, /<artifactId>spring-boot-starter-parent<\/artifactId>\s*<version>([^<]+)<\/version>/);
const javaVersion = extractFirst(pom, /<java\.version>([^<]+)<\/java\.version>/);
const springdocVersion = extractFirst(pom, /<springdoc\.version>([^<]+)<\/springdoc\.version>/);
const mavenDependencyCount = [...pom.matchAll(/<dependency>/g)].length;
const nasRestoreDrillEvidence = nasRestoreDrillEvidenceText ? JSON.parse(nasRestoreDrillEvidenceText) : {};

const sourceFindings = {
  smokeAuthProfileOnly: /@Profile\(\{"local",\s*"dev",\s*"test"\}\)/.test(authService),
  smokePasswordUsesSha256: /MessageDigest\.getInstance\("SHA-256"\)/.test(authService),
  bearerCredentialCleared: /UsernamePasswordAuthenticationToken\(\s*user,\s*null,/s.test(bearerFilter),
  productionAuthProviderReady: [
    /@Profile\("prod"\)/.test(productionAuthService),
    /BCryptPasswordEncoder/.test(productionAuthService),
    /auth_sessions/.test(productionAuthService),
    /sha256Hex/.test(productionAuthService),
    /@Profile\("prod"\)/.test(productionAuthController),
    /@Profile\("prod"\)/.test(productionBearerFilter),
    /UsernamePasswordAuthenticationToken\(\s*actor,\s*null,/s.test(productionBearerFilter),
    /CREATE TABLE IF NOT EXISTS auth_credentials/.test(productionAuthMigration),
    /CREATE TABLE IF NOT EXISTS auth_sessions/.test(productionAuthMigration),
    /ProductionAuthJdbcIntegrationTests/.test(productionAuthTest),
  ].every(Boolean),
  publicDocsPermitAll: /"\/docs\/\*\*"[\s\S]*"\/swagger-ui\/\*\*"[\s\S]*"\/v3\/api-docs\/\*\*"[\s\S]*\)\.permitAll\(\)/.test(securityConfig),
  csrfDisabled: /\.csrf\(csrf -> csrf\.disable\(\)\)/.test(securityConfig),
  productionLedgerHasMdmPending: /\| PRD-MDM-001 \|[\s\S]*?\| pending_info \|/.test(ledgerText),
  productionLedgerHasNasPending: /\| PRD-NAS-002 \|[\s\S]*?\| pending_info \|/.test(ledgerText),
  nasServerKnown: [
    ledgerText,
    nasRunbookText,
    server160RunbookText,
    ops160EnvExample,
  ].every((content) => content.includes("192.168.88.113")),
  productionLedgerHasCredentialPending: /\| PRD-CRED-001 \|[\s\S]*?\| pending_info \|/.test(ledgerText),
  tenantIsolationFindingManaged: [
    /DATA_ISOLATION_SPEC_AUTO_START/.test(dataIsolationSpec),
    /verify:tenant-isolation/.test(dataIsolationSpec),
    /SEC-.*TENANT-ISOLATION/.test(dataIsolationSpec) || /tenant-isolation/.test(dataIsolationSpec),
    domainOwnershipScopeCsv.includes("scope_type"),
    /UNSCOPED_DIRECT_READ/.test(tenantIsolationVerifier),
    packageJson.scripts?.["verify:tenant-isolation"] === "node tools/verify-tenant-isolation.mjs",
    packageJson.scripts?.["verify:delivery-gates"]?.includes("verify:tenant-isolation"),
  ].every(Boolean),
  tenantIsolationBaselineReady: tenantIsolationBaseline.trim().length > 0,
  productionCredentialLedgerVerifiable: [
    /## 4\.1 生产凭证明细台账模板/.test(ledgerText),
    /PRD-CRED-OPS-001/.test(ledgerText),
    /PRD-CRED-DB-001/.test(ledgerText),
    /PRD-CRED-OBJ-001/.test(ledgerText),
    /PRD-CRED-JWT-001/.test(ledgerText),
    /PRD-CRED-3P-001/.test(ledgerText),
    /PRD-CRED-OPEN-001/.test(ledgerText),
    /生产服务器\/运维账号/.test(credentialRunbookText),
    /PostgreSQL 生产库/.test(credentialRunbookText),
    /对象存储/.test(credentialRunbookText),
    /JWT\/Token 签名密钥/.test(credentialRunbookText),
    /第三方供应商 API 密钥/.test(credentialRunbookText),
    /reminder_days/.test(credentialRunbookText),
    /rollback_step/.test(credentialRunbookText),
    packageJson.scripts?.["verify:production-credentials"] === "node tools/verify-production-credential-rotation-ledger.mjs",
    packageJson.scripts?.["verify:ops160-recovery"]?.includes("verify:production-credentials"),
  ].every(Boolean),
  securityBaselineInP0Gate: packageJson.scripts?.["verify:p0"]?.includes("verify:security-baseline"),
  nasBackupScriptsReady: [
    /NAS_BACKUP_TARGET_DIR/.test(ops160BackupScript),
    /REQUIRE_NAS_BACKUP_SYNC/.test(ops160BackupScript),
    /sha256sum/.test(ops160BackupScript),
    /complex_restore_drill_/.test(ops160RestoreDrillScript),
    /restore\.drill_completed/.test(ops160RestoreDrillScript),
    /pg_restore/.test(ops160RestoreDrillScript),
  ].every(Boolean),
  nasRestoreDrillEvidenceReady: [
    nasRestoreDrillEvidence.event === "restore.drill_completed",
    nasRestoreDrillEvidence.verification_result === "passed",
    nasRestoreDrillEvidence.restore_database_dropped === true,
    nasRestoreDrillEvidence.flyway_version === "018",
    String(nasRestoreDrillEvidence.verified_tables ?? "").includes("flyway_schema_history"),
  ].every(Boolean),
  nasRestoreDrillEvidenceFromNas: String(nasRestoreDrillEvidence.backup_path ?? "").startsWith("/mnt/complex-nas-backup/"),
  taskDefinesAllOutputs: [
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
  ].every((section) => taskText.includes(section)),
};

const findings = [
  {
    id: "SEC-P0-AUTH-PROD-001",
    severity: "P0",
    title: "正式生产认证 Provider 与会话治理未落地",
    status: sourceFindings.productionAuthProviderReady ? "resolved" : (sourceFindings.smokeAuthProfileOnly ? "open" : "needs_review"),
    blockingScope: "阻塞公开生产和长期预生产；不阻塞当前 dev smoke 编码验证",
    owner: "技术负责人/安全",
    evidence: "`AuthService` 与 `BearerAuthenticationFilter` 仅在 local/dev/test profile 生效",
    nextAction: "接入正式员工/设备/客人/开放应用认证 Provider，补 Token 过期、刷新、吊销、限速、失败锁定和审计",
  },
  {
    id: "SEC-P0-MDM-DEVICE-001",
    severity: "P0",
    title: "MDM/Android Enterprise 生产纳管仍处 pending_info",
    status: sourceFindings.productionLedgerHasMdmPending ? "open" : "needs_review",
    blockingScope: "阻塞生产工作手机、平板、一体机执行高风险核销/出酒/支付相关动作",
    owner: "IT/采购/运维",
    evidence: "`production-readiness-implementation-ledger-template-v0.1.md` 中 PRD-MDM-001 为 pending_info",
    nextAction: "完成 MDM 选型、enroll、策略下发、设备绑定截图和生产准入记录",
  },
  {
    id: "SEC-P0-OPS-RECOVERY-001",
    severity: "P0",
    title: sourceFindings.productionCredentialLedgerVerifiable
      ? (sourceFindings.nasRestoreDrillEvidenceFromNas
        ? "NAS 挂载、真实同步和从 NAS 恢复证据已完成，生产凭证发放证据仍需补齐"
        : (sourceFindings.nasServerKnown
          ? "NAS 服务器已明确，160 挂载/真实同步/恢复证据和生产凭证发放证据仍需补齐"
          : "NAS 真实恢复/同步证据仍需补齐，生产凭证轮换台账已可验证"))
      : "NAS 恢复演练和生产密钥轮换台账未完成",
    status: (sourceFindings.productionLedgerHasNasPending || sourceFindings.productionLedgerHasCredentialPending || !sourceFindings.productionCredentialLedgerVerifiable) ? "open" : "needs_review",
    blockingScope: "阻塞公开生产安全准入；不阻塞开发联调",
    owner: "IT/安全/运维",
    evidence: sourceFindings.productionCredentialLedgerVerifiable
      ? (sourceFindings.nasRestoreDrillEvidenceFromNas
        ? "`verify:ops160-execution-policy` 已固化 NAS 服务器 `192.168.88.113` 和 160 后端测试口径；`verify:production-credentials` 已覆盖生产账号、PostgreSQL、对象存储、JWT/Token、第三方/支付/企微/开放平台密钥、NAS SMB 凭证和备份加密预留台账；NAS 服务器 `192.168.88.113` 的真实挂载、备份同步和从 NAS 恢复演练 `restore.drill_completed` 已通过"
        : (sourceFindings.nasServerKnown
          ? "`verify:ops160-execution-policy` 已固化 NAS 服务器 `192.168.88.113` 和 160 后端测试口径；`verify:production-credentials` 已覆盖生产账号、PostgreSQL、对象存储、JWT/Token、第三方/支付/企微/开放平台密钥、NAS SMB 凭证和备份加密预留台账；PRD-NAS-002 仍需 160 挂载、真实同步和恢复证据"
          : "`verify:production-credentials` 已覆盖生产账号、PostgreSQL、对象存储、JWT/Token、第三方/支付/企微/开放平台密钥、NAS SMB 凭证和备份加密预留台账；PRD-NAS-002 仍需真实恢复/同步证据"))
      : "PRD-NAS-002 与 PRD-CRED-001 仍需实施证据",
    nextAction: sourceFindings.productionCredentialLedgerVerifiable
      ? (sourceFindings.nasRestoreDrillEvidenceFromNas
        ? "P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据"
        : (sourceFindings.nasServerKnown
          ? "将 `//192.168.88.113/bn` 通过 SMB 挂载到 160 的 `/mnt/complex-nas-backup`，配置 `NAS_BACKUP_TARGET_DIR`、`REQUIRE_NAS_BACKUP_SYNC=true`、`ALLOW_UNENCRYPTED_BACKUPS=true` 并执行端到端同步和恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据"
          : "继续执行真实 NAS 端到端同步和恢复演练；生产凭证实际发放前按明细台账补责任人、到期日、credential_ref、提醒和轮换证据"))
      : "执行 NAS 恢复演练，建立密钥引用、到期提醒、轮换记录和回滚 runbook",
  },
  {
    id: "SEC-P0-TENANT-ISOLATION-001",
    severity: "P0",
    title: "跨租户/跨商户越权 finding 系列必须由安全基线纳管",
    status: sourceFindings.tenantIsolationFindingManaged ? "resolved" : "open",
    blockingScope: sourceFindings.tenantIsolationFindingManaged
      ? "已纳入安全基线生成器和交付闸口；继续通过 verify:tenant-isolation 防回归"
      : "阻塞公开生产安全准入；未纳管前无法证明跨租户越权风险已进入正式闭环",
    owner: "安全/后端/架构负责人",
    evidence: sourceFindings.tenantIsolationFindingManaged
      ? "`data-isolation-and-tenant-scope-security-spec-v0.1.md` §3A 自动基线、`domain-ownership-scope-audit-v0.1.csv`、`verify:tenant-isolation` 与 `tenant-isolation-baseline.json` 已纳入生成器"
      : "安全 finding 台账缺少 SEC-*-TENANT-ISOLATION-*；或 tenant isolation 机审未接入交付闸口",
    nextAction: sourceFindings.tenantIsolationFindingManaged
      ? "后续新增带 scope 数据表时同步 CSV/迁移/Repository，并执行 `npm run verify:tenant-isolation` 与 `npm run verify:security-baseline`"
      : "补跨租户越权 finding、CSV scope 登记、Repository 机审和回归测试后重新生成安全基线",
  },
  {
    id: "SEC-P1-DOCS-EXPOSURE-001",
    severity: "P1",
    title: "OpenAPI/Swagger 文档在当前 SecurityConfig 中公开放行",
    status: sourceFindings.publicDocsPermitAll ? "open" : "resolved",
    blockingScope: sourceFindings.publicDocsPermitAll
      ? "阻塞公网公开生产；内网预生产可通过网络 ACL 或账号保护临时接受"
      : "公开生产匿名访问已保护；dev/test 仍可公开用于契约导出和联调验证",
    owner: "后端/运维",
    evidence: sourceFindings.publicDocsPermitAll
      ? "`SecurityConfig` 对 `/docs/**`、`/swagger-ui/**`、`/v3/api-docs/**` permitAll"
      : "`SecurityConfig` 仅在非 prod profile 对 `apiDocumentationPaths()` permitAll；`verify:prod-docs-access` 已覆盖 prod 匿名访问保护",
    nextAction: sourceFindings.publicDocsPermitAll
      ? "公开生产 profile 下关闭或加认证保护，仅保留内网/运维可访问"
      : "后续若变更 SecurityConfig，必须继续执行 `npm run verify:prod-docs-access` 和 `npm run verify:security-baseline`",
  },
  {
    id: "SEC-P1-WEBHOOK-REPLAY-001",
    severity: "P1",
    title: "真实供应商 Webhook 签名、防重放和轮询校验需在供应商资料到位后补实",
    status: externalOperations.length > 0 ? "open" : "needs_review",
    blockingScope: "阻塞真实支付/人脸/设备/开放平台供应商联调上线；不阻塞 mock adapter",
    owner: "后端/IT/供应商联调负责人",
    evidence: `OpenAPI 当前存在 ${externalOperations.length} 个开放应用或 webhook 签名上下文 operation`,
    nextAction: "按供应商 SDK/API 补字段映射、nonce、timestamp、签名验签、幂等、补偿和对账测试",
  },
  {
    id: "SEC-P2-CREDENTIALS-001",
    severity: "P2",
    title: "Bearer 原始 token 不再进入 Spring Security credentials",
    status: sourceFindings.bearerCredentialCleared ? "resolved" : "open",
    blockingScope: "已修复；继续通过单测防回归",
    owner: "后端",
    evidence: "`BearerAuthenticationFilter` 构造 `UsernamePasswordAuthenticationToken(user, null, authorities)`",
    nextAction: "保留 `BearerAuthenticationFilterTests`，后续正式认证实现沿用不把原始凭据放入认证对象的原则",
  },
  {
    id: "SEC-P2-SMOKE-HASH-001",
    severity: "P2",
    title: "dev smoke AuthService 使用 SHA-256 密码摘要，不可迁移到生产认证",
    status: sourceFindings.smokePasswordUsesSha256 ? "accepted_for_dev_smoke" : "resolved",
    blockingScope: "仅允许 local/dev/test；公开生产必须替换为正式身份源或强密码哈希方案",
    owner: "后端/安全",
    evidence: "`AuthService` 使用 `MessageDigest.getInstance(\"SHA-256\")`，且类上存在 local/dev/test profile",
    nextAction: "生产认证不得复用该 smoke 账号存储；若自建密码，采用 Argon2id/bcrypt/PBKDF2 并补 MFA/锁定策略",
  },
  {
    id: "SEC-P2-CSRF-API-001",
    severity: "P2",
    title: "当前 API 关闭 CSRF，需在最终认证形态中复核 Web Cookie 场景",
    status: sourceFindings.csrfDisabled ? "open_for_prod_review" : "resolved",
    blockingScope: "不阻塞 Bearer/API 调用 smoke；若后台 Web 采用 Cookie 会话则阻塞公开生产",
    owner: "后端/前端/安全",
    evidence: "`SecurityConfig` 调用 `csrf.disable()`",
    nextAction: "正式认证方案定稿时按 Bearer、SameSite Cookie 或双提交 Token 分支复核",
  },
  {
    id: "SEC-P3-BASELINE-AUTOMATION-001",
    severity: "P3",
    title: "安全基线已脚本化，后续需接入发布闸口",
    status: sourceFindings.securityBaselineInP0Gate ? "resolved" : "open",
    blockingScope: sourceFindings.securityBaselineInP0Gate ? "已纳入完整 P0 验证链" : "不阻塞当前编码；建议纳入发布前检查",
    owner: "技术负责人",
    evidence: sourceFindings.securityBaselineInP0Gate
      ? "`verify:p0` 已包含 `verify:security-baseline`，安全基线验证会随完整 P0 验证执行"
      : "`docs:security-baseline` 与 `verify:security-baseline` 已建立独立脚本入口",
    nextAction: sourceFindings.securityBaselineInP0Gate
      ? "后续发布前继续执行 `npm run verify:p0`，不要绕过安全基线闸口"
      : "在生产发布流程中把 `verify:security-baseline` 加入 release checklist",
  },
];

if (sourceFindings.productionAuthProviderReady) {
  const productionAuthFinding = findings.find((finding) => finding.id === "SEC-P0-AUTH-PROD-001");
  productionAuthFinding.evidence = "`ProductionAuthService`/`ProductionAuthController`/`ProductionBearerAuthenticationFilter`、`V018__p0_production_auth_provider.sql` 和 `ProductionAuthJdbcIntegrationTests` 已落地";
  productionAuthFinding.nextAction = "继续保留 MDM、NAS、密钥轮换、真实供应商接入等非代码生产前置项的阻断状态";
}

if (sourceFindings.nasRestoreDrillEvidenceReady) {
  const opsRecoveryFinding = findings.find((finding) => finding.id === "SEC-P0-OPS-RECOVERY-001");
  opsRecoveryFinding.evidence = sourceFindings.productionCredentialLedgerVerifiable
    ? (sourceFindings.nasRestoreDrillEvidenceFromNas
      ? "`complex-backup.sh` 已向 NAS 服务器 `192.168.88.113` 生成真实备份归档与 `.sha256`；`complex-restore-drill.sh` 已从 `/mnt/complex-nas-backup` 完成 `restore.drill_completed`；`verify:production-credentials` 已覆盖生产凭证台账结构；生产凭证发放和轮换证据仍需实施"
      : (sourceFindings.nasServerKnown
        ? "`complex-backup.sh` 已生成备份归档与 `.sha256`；`complex-restore-drill.sh` 已完成 `restore.drill_completed`；NAS 服务器已明确为 `192.168.88.113`；`verify:production-credentials` 已覆盖生产凭证台账结构；160 到 NAS 的真实挂载/同步证据和真实凭证发放证据仍需实施"
        : "`complex-backup.sh` 已生成备份归档与 `.sha256`；`complex-restore-drill.sh` 已完成 `restore.drill_completed`；`verify:production-credentials` 已覆盖生产凭证台账结构；真实 NAS 同步目标和真实凭证发放证据仍需实施"))
    : (sourceFindings.nasServerKnown
      ? "`complex-backup.sh` 已生成备份归档与 `.sha256`；`complex-restore-drill.sh` 已完成 `restore.drill_completed`，恢复到临时库并校验 Flyway 018 与核心表；NAS 服务器已明确为 `192.168.88.113`；PRD-CRED-001 与 160 到 NAS 的真实挂载/同步证据仍需实施"
      : "`complex-backup.sh` 已生成备份归档与 `.sha256`；`complex-restore-drill.sh` 已完成 `restore.drill_completed`，恢复到临时库并校验 Flyway 018 与核心表；PRD-CRED-001 与真实 NAS 同步目标仍需实施证据");
  opsRecoveryFinding.nextAction = sourceFindings.productionCredentialLedgerVerifiable
    ? (sourceFindings.nasRestoreDrillEvidenceFromNas
      ? "P0-A no-system-write boundary：Codex 不安装系统级配置；后续由 IT 固化 160 开机自动挂载、定时备份、容量告警、失败告警和周期性恢复演练；生产凭证实际发放前补齐 credential_ref、到期提醒和轮换证据"
      : "将 `//192.168.88.113/bn` 挂载到 160 的 `/mnt/complex-nas-backup`，配置 `NAS_BACKUP_TARGET_DIR` 并执行 NAS 端到端同步验证；生产凭证实际发放前补齐 credential_ref、到期提醒和轮换证据")
    : "将 `//192.168.88.113/bn` 挂载到 160 的 `/mnt/complex-nas-backup`，配置 `NAS_BACKUP_TARGET_DIR` 并执行 NAS 端到端同步验证；继续建立密钥引用、到期提醒、轮换记录和回滚 runbook";
}

const nasRestoreDrillStatus = sourceFindings.nasRestoreDrillEvidenceReady
  ? (sourceFindings.nasRestoreDrillEvidenceFromNas
    ? "`restore.drill_completed` 已有证据，临时库恢复和清理通过；备份来源为 `/mnt/complex-nas-backup` 真实 NAS 归档"
    : (sourceFindings.nasServerKnown
      ? "`restore.drill_completed` 已有证据，临时库恢复和清理通过；NAS 服务器 `192.168.88.113` 已明确，160 挂载和真实同步证据仍需配置"
      : "`restore.drill_completed` 已有证据，临时库恢复和清理通过；真实 NAS 同步目标仍需配置"))
  : "PRD-NAS-002 仍需证据";
const nasRestoreDrillBlocking = sourceFindings.nasRestoreDrillEvidenceReady
  ? (sourceFindings.nasRestoreDrillEvidenceFromNas
    ? "不再阻塞真实 NAS 可恢复性；生产前仍需固化自动挂载、定时备份和周期性演练"
    : "不再阻塞本地备份可恢复性；仍阻塞真实 NAS 可恢复结论")
  : "阻塞生产可恢复结论";
const credentialRotationStatus = sourceFindings.productionCredentialLedgerVerifiable
  ? "`verify:production-credentials` 已通过；明细台账覆盖生产账号、PostgreSQL、对象存储、JWT/Token、第三方/支付/企微/开放平台密钥、NAS SMB 凭证和备份加密预留"
  : "PRD-CRED-001 仍需明细台账、到期提醒和分类型 Runbook";
const credentialRotationBlocking = sourceFindings.productionCredentialLedgerVerifiable
  ? "台账结构和 Runbook 不再阻塞；真实生产凭证发放仍需责任人、credential_ref、到期日和轮换证据"
  : "阻塞长期预生产/生产";

function findingsTable() {
  return [
    "| severity | id | title | status | blocking_scope | owner | evidence | next_action |",
    "|---|---|---|---|---|---|---|---|",
    ...findings.map((finding) =>
      `| ${finding.severity} | \`${finding.id}\` | ${escapeCell(finding.title)} | ${finding.status} | ${escapeCell(finding.blockingScope)} | ${escapeCell(finding.owner)} | ${escapeCell(finding.evidence)} | ${escapeCell(finding.nextAction)} |`
    ),
  ].join("\n");
}

const openapiHash = sha256(openapiText);

const baseline = `# 正式安全基线 V1

> 本报告由 \`tools/generate-formal-security-baseline.mjs\` 生成，覆盖当前 Java 21 + Spring Boot 4.x + PostgreSQL + Flyway 首批 P0 编码状态。

- Generated from deterministic workspace inputs; execution time is captured by the verification logs.
- OpenAPI operation count: ${operations.length}
- OpenAPI SHA-256: \`${openapiHash}\`
- Java version: \`${javaVersion}\`
- Spring Boot version: \`${springBootVersion}\`
- springdoc-openapi version: \`${springdocVersion}\`
- Maven dependency count: ${mavenDependencyCount}
- Verification command: \`npm run verify:security-baseline\`

## scope-and-inputs

| 输入 | 状态 |
|---|---|
| OpenAPI 静态契约 | 已读取 \`docs/openapi/openapi-p0.json\` |
| 安全基线生成任务 | ${sourceFindings.taskDefinesAllOutputs ? "已覆盖 10 类输出物" : "缺输出物定义，需人工复核"} |
| 生产前置台账 | 已读取 MDM、NAS、密钥轮换、安全基线等状态 |
| Spring Security 配置 | 已读取 \`SecurityConfig\` |
| dev smoke 鉴权实现 | 已读取 \`AuthService\` 与 \`BearerAuthenticationFilter\` |
| 数据隔离与租户 scope | 已读取 \`data-isolation-and-tenant-scope-security-spec-v0.1.md\`、scope CSV、\`verify-tenant-isolation.mjs\` 和 tenant-isolation baseline |

## threat-model-v1

| 资产/边界 | 当前结论 | 主要风险 | 当前缓解 | 后续动作 |
|---|---|---|---|---|
| 客户、员工、商户、会员身份 | P0 契约已区分 staff-token、customer-token、device-credential、open-app-credential、webhook-signature | 生产认证 Provider 未落地 | dev smoke 限定 local/dev/test profile | 正式身份源、Token 生命周期、设备凭证、开放应用签名补实 |
| 票务、权益、核销、酒水、存酒、卡座 | OpenAPI、权限矩阵和事件矩阵已声明闭环 | 重复核销、越权核销、卡座人数并发超额 | operation 级幂等和审计事件已进入契约 | 后续业务实现中必须用数据库约束和事务固化 |
| 支付、退款、应收应付、对账 | 聚合支付优先、保留直连抽象 | 回调重放、退款冲正缺审计、日结后改账 | OpenAPI 声明幂等和审计事件 | 供应商资料到位后补验签、对账文件、补偿策略 |
| Android 工作手机、平板、一体机 | 端侧边界已在文档和 OpenAPI 表达 | 未纳管设备生产使用 | 生产前置台账阻断 MDM pending_info | Android Enterprise/商业 MDM enroll 后才可生产使用 |
| 开放平台与第三方 | 双向开放与 adapter 边界已定义 | 签名、防重放、字段映射供应商差异 | mock adapter 和 OpenAPI 先行 | 真实供应商 SDK/API 到位后补字段和异常码 |
| 多租户/商户数据隔离 | scope 注入只来自安全 token；scope CSV、数据隔离规格和 \`verify:tenant-isolation\` 已形成机审链路 | Repository 漏 scope、请求体伪造 merchant_id、跨项目读写越权 | \`verify:tenant-isolation\` + 自动数据隔离基线 + SEC-P0-TENANT-ISOLATION-001 finding | 新增带租户数据的表必须同步 CSV、迁移、Repository scoped 通道和越权测试 |
| 160 运维、NAS、密钥 | 已有 runbook、台账模板和恢复演练脚本 | ${sourceFindings.productionCredentialLedgerVerifiable ? (sourceFindings.nasRestoreDrillEvidenceFromNas ? "NAS 服务器已明确为 `192.168.88.113`，真实 NAS 挂载、同步备份和从 NAS 恢复演练 `restore.drill_completed` 已通过；生产凭证台账结构已可验证，真实凭证发放/轮换证据仍需补齐" : (sourceFindings.nasServerKnown ? "NAS 服务器已明确为 `192.168.88.113`，生产凭证台账结构已可验证；160 挂载/真实 NAS 同步与真实凭证发放/轮换证据仍需补齐" : "生产凭证台账结构已可验证，真实 NAS 同步目标与真实凭证发放/轮换证据仍需补齐")) : "无恢复演练、密钥轮换台账结构和证据不足"} | 当前不在报告中记录明文密钥 | ${sourceFindings.nasRestoreDrillEvidenceFromNas ? "固化自动挂载、定时备份、容量告警、到期提醒和轮换证据归档" : "完成 NAS 同步验证、真实 credential_ref、到期提醒和轮换证据归档"} |

## authz-review-v1

${markdownCountTable(authContextCounts, "auth_context", "operation_count")}

| 检查项 | 结论 |
|---|---|
| 认证上下文 | 已按 OpenAPI 统计；员工、客户、设备、开放应用和 webhook 不混用员工权限 |
| 权限码 | \`verify:p0-contract\` 已要求每个 operation 具备 \`x-permission\` |
| 数据范围 | \`verify:p0-contract\` 已要求 operation 具备数据范围和终端/调用通道 |
| 高风险审批 | 权限矩阵声明高风险权限审批、显式拒绝、临时授权和模板机制 |
| 当前缺口 | 正式生产认证 Provider、Token 过期/刷新/吊销、失败锁定和限速仍为生产前阻断 |

## openapi-exposure-review-v1

| 指标 | 数量 |
|---|---:|
| 全部 operation | ${operations.length} |
| 写 operation | ${writeOperations.length} |
| public operation | ${publicOperations.length} |
| open-app/webhook operation | ${externalOperations.length} |

${markdownCountTable(terminalCounts, "terminal_or_channel", "operation_count")}

当前 SecurityConfig 允许 \`/docs/**\`、\`/swagger-ui/**\`、\`/v3/api-docs/**\` 公开访问。该配置可用于内网开发和联调，但公网公开生产必须关闭或加认证保护。

## webhook-signature-review-v1

| 检查项 | 当前状态 | 结论 |
|---|---|---|
| 签名上下文 | OpenAPI 使用 \`webhook-signature\` 与 \`open-app-credential\` 区分外部调用 | 边界正确 |
| 幂等 | 写接口由 \`verify:p0-contract\` 强制声明 \`x-idempotency-required\` | 契约已具备验收基础 |
| 防重放 | 供应商 nonce、timestamp、签名算法未到位 | 真实供应商联调前必须补实 |
| 轮询校验 | 文档采用 Webhook + 定时轮询校验 | 策略正确，待按供应商字段落地 |

## payment-risk-review-v1

| 检查项 | 当前状态 | 结论 |
|---|---|---|
| 支付通道 | 聚合支付优先，保留直连接口抽象 | 选型口径已收口 |
| 回调 | OpenAPI/事件矩阵已要求回调、轮询、幂等和补偿 | 待供应商资料 |
| 退款 | 权限矩阵要求退款发起/审批分离 | 后续实现必须不可覆盖原单，只做冲正 |
| 对账 | 财务与开放平台文档要求对账/补偿 | 真实对账文件样例到位后补测试 |

## pii-retention-review-v1

| 数据类型 | 当前策略 | 结论 |
|---|---|---|
| 法定实名/年龄核验 | 只在法定必要范围内强制实名；酒吧年龄/人脸核验走第三方接口 | 供应商合规材料和最小化字段待补 |
| 人脸数据 | 默认只保存最小必要结果和第三方流水，不默认保存原始人脸图像 | 符合最小化方向 |
| 敏感证据 | 查看和导出需最小授权、二次审批和用途留痕 | 事件矩阵已定义 |
| 日志 | 不得记录密码、原始 Token、生产密钥和完整敏感证据 | 本报告验证器会拦截明显明文密钥 |

## device-mdm-review-v1

| 检查项 | 当前状态 | 阻断结论 |
|---|---|---|
| Android Enterprise/商业 MDM | PRD-MDM-001 仍为 pending_info | 阻塞生产高风险终端动作 |
| 设备绑定 | 权限矩阵已声明设备绑定/解绑和 MDM 策略下发权限 | 开发可继续，生产需证据 |
| 离线边界 | 高价值酒水、支付、核销保持在线 | 正确 |
| 一体机 | 售票型和非售票型能力分离 | 正确，需设备能力开关测试 |

## ops-160-review-v1

| 检查项 | 当前状态 | 阻断结论 |
|---|---|---|
| 160 运维脚本 | P0 验证脚本已存在并可作为准入证据 | 需持续跑完整验证 |
| 160 后端测试执行位置 | Java/后端集成测试、BE-000 和完整 P0 验收默认在 \`192.168.88.160\` 或等效 Java 21 环境执行 | 本机缺 Java 21 只记录为本机环境前置缺失，不作为代码失败 |
| systemd/正式配置 | 文档要求迁移到系统配置和告警 | 公开生产前必须完成 |
| NAS 恢复演练 | ${nasRestoreDrillStatus} | ${nasRestoreDrillBlocking} |
| 密钥轮换 | ${credentialRotationStatus} | ${credentialRotationBlocking} |
| 报告密钥安全 | 本报告不写真实密钥、数据库密码或对象存储密码 | 正确 |

## dependency-sbom-review-v1

| 依赖面 | 当前状态 | 后续动作 |
|---|---|---|
| Maven | \`backend/pom.xml\` 声明 ${mavenDependencyCount} 个 dependency；Spring Boot \`${springBootVersion}\` | 生成 CycloneDX 或等效 SBOM，并做漏洞处置/豁免 |
| npm/Web 工具链 | 根目录使用 lock 文件和 workspaces | 发布前执行 npm audit 或等效扫描 |
| Android/Gradle | 完整 P0 验证脚本覆盖 Android env/build/smoke/APK | 发布前补 Android 依赖 SBOM |

## findings-register-v1

完整发现清单见 \`security-scans/findings-register-v1.md\`。

${findingsTable()}

## verification-and-gates

| Gate | 目的 | 命令 |
|---|---|---|
| 安全基线生成 | 生成 V1 报告和 findings register | \`npm run docs:security-baseline\` |
| 安全基线验证 | 检查章节、计数、发现状态、密钥泄漏 | \`npm run verify:security-baseline\` |
| P0 契约验证 | 检查 OpenAPI、追溯附录、客户端契约 | \`npm run verify:p0-contract\` |
| P0 全量验证 | 检查后端、Web、Android、APK | \`/opt/complex/scripts/complex-verify-p0.sh\` |
`;

const findingsRegister = `# Findings Register V1

> 本清单由 \`tools/generate-formal-security-baseline.mjs\` 生成。P0 未修复或未正式豁免前，不允许进入公开生产安全准入。

- Generated from deterministic workspace inputs; execution time is captured by the verification logs.
- OpenAPI operation count: ${operations.length}
- OpenAPI SHA-256: \`${openapiHash}\`

## status-policy

| 状态 | 含义 |
|---|---|
| open | 未修复，仍需进入责任人计划 |
| resolved | 已修复并有验证依据 |
| accepted_for_dev_smoke | 仅允许当前开发 smoke，不能迁移到生产 |
| open_for_prod_review | 生产方案定稿时必须复核 |
| needs_review | 静态证据不足，需要人工复核 |

## findings

${findingsTable()}

## production-blocking-summary

| 阻断级别 | 规则 |
|---|---|
| P0 open | 阻塞公开生产或长期预生产安全准入，必须修复或正式豁免 |
| P1 open | 阻塞对应公开面或真实供应商联调，必须有责任人和计划 |
| P2/P3 open | 不阻塞当前编码，但必须进入后续排期或风险接受记录 |

## resolved-evidence

| finding | 证据 |
|---|---|
| \`SEC-P2-CREDENTIALS-001\` | Bearer 认证对象的 credentials 为 \`null\`，避免把原始 token 保存在 Spring Security authentication credentials |

## next-verification

1. 修复任一 finding 后，重新执行 \`npm run docs:security-baseline\`。
2. 再执行 \`npm run verify:security-baseline\`。
3. 生产前还必须执行 \`npm run verify:p0-contract\` 和完整 P0 验证脚本。
`;

fs.mkdirSync(path.dirname(paths.baseline), { recursive: true });
fs.writeFileSync(paths.baseline, baseline, "utf8");
fs.writeFileSync(paths.findings, findingsRegister, "utf8");

console.log(`generated ${path.relative(root, paths.baseline)} with ${operations.length} operations and ${findings.length} findings`);
console.log(`generated ${path.relative(root, paths.findings)} with ${operations.length} operations and ${findings.length} findings`);
