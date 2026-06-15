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

function requireRegex(content, pattern, label, description) {
  if (!pattern.test(content)) {
    throw new Error(`${label} missing required pattern: ${description}`);
  }
}

const runbook = readRequired('docs/production-credential-rotation-runbook-v0.1.md');
const ledger = readRequired('docs/production-readiness-implementation-ledger-template-v0.1.md');
const packageJson = JSON.parse(readRequired('package.json'));

const requiredCredentialKinds = [
  '生产服务器/运维账号',
  'PostgreSQL 生产库',
  '对象存储',
  'JWT/Token 签名密钥',
  '聚合支付凭证',
  '企业微信凭证',
  '第三方供应商 API 密钥',
  '开放平台应用/回调签名密钥',
  'NAS SMB 凭证',
  '备份加密口令预留',
];

for (const kind of requiredCredentialKinds) {
  requireIncludes(runbook, kind, 'production credential rotation runbook');
  requireIncludes(ledger, kind, 'production readiness ledger template');
}

for (const marker of [
  '人工台账 + 到期提醒 + Runbook',
  '默认提前 30 天、7 天、1 天提醒',
  'credential_ref',
  'reminder_days',
  'rotation_status',
  'rotation_evidence_link',
  'verification_step',
  'rollback_step',
  'emergency_disable_step',
  '不得写真实密钥',
]) {
  requireIncludes(runbook, marker, 'production credential rotation runbook');
}

for (const marker of [
  '## 4.1 生产凭证明细台账模板',
  'PRD-CRED-OPS-001',
  'PRD-CRED-DB-001',
  'PRD-CRED-OBJ-001',
  'PRD-CRED-JWT-001',
  'PRD-CRED-3P-001',
  'PRD-CRED-OPEN-001',
  '阻塞长期预生产和生产',
  '只接受 credential_ref',
]) {
  requireIncludes(ledger, marker, 'production readiness ledger template');
}

for (const heading of [
  '### 7.1 生产服务器/运维账号轮换',
  '### 7.2 PostgreSQL 生产库密码轮换',
  '### 7.3 对象存储 Access Key/Secret Key 轮换',
  '### 7.4 JWT/Token 签名密钥轮换',
  '### 7.5 第三方、支付、企业微信和开放平台密钥轮换',
]) {
  requireIncludes(runbook, heading, 'production credential rotation runbook');
}

requireRegex(
  runbook,
  /Given[\s\S]*生产数据库密码[\s\S]*When[\s\S]*没有责任人、到期日或 Runbook[\s\S]*Then[\s\S]*不得发放该凭证/,
  'production credential rotation runbook',
  'database credential issuance acceptance criteria'
);
requireRegex(
  runbook,
  /Given[\s\S]*距离到期不足 7 天[\s\S]*When[\s\S]*到期检查[\s\S]*Then[\s\S]*生成提醒任务/,
  'production credential rotation runbook',
  'expiry reminder acceptance criteria'
);
requireRegex(
  runbook,
  /Given[\s\S]*疑似泄露[\s\S]*When[\s\S]*应急停用[\s\S]*Then[\s\S]*记录停用时间/,
  'production credential rotation runbook',
  'leak emergency disable acceptance criteria'
);

const forbiddenSecretPatterns = [
  /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/,
  /\b(?:sk|pk)_(?:live|test)_[A-Za-z0-9]{16,}/,
  /\bAKIA[0-9A-Z]{16}\b/,
  /\b(?:password|secret|token)\s*[:=]\s*["'][^"'<\n]{12,}["']/i,
];

for (const [index, pattern] of forbiddenSecretPatterns.entries()) {
  if (pattern.test(runbook) || pattern.test(ledger)) {
    throw new Error(`production credential docs contain an apparent plaintext secret pattern #${index + 1}`);
  }
}

if (packageJson.scripts?.['verify:production-credentials'] !== 'node tools/verify-production-credential-rotation-ledger.mjs') {
  throw new Error('package.json must expose verify:production-credentials');
}

if (!packageJson.scripts?.['verify:ops160-recovery']?.includes('verify:production-credentials')) {
  throw new Error('verify:ops160-recovery must include verify:production-credentials');
}

console.log('production credential rotation ledger verification passed.');
