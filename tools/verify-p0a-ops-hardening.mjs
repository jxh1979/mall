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

function forbidPattern(content, pattern, label, description) {
  if (pattern.test(content)) {
    throw new Error(`${label} contains forbidden pattern: ${description}`);
  }
}

const packageJson = JSON.parse(readRequired('package.json'));

if (packageJson.scripts?.['verify:p0a-ops-hardening'] !== 'node tools/verify-p0a-ops-hardening.mjs') {
  throw new Error('package.json must expose verify:p0a-ops-hardening');
}

if (!packageJson.scripts?.['verify:ops160-recovery']?.includes('verify:p0a-ops-hardening')) {
  throw new Error('verify:ops160-recovery must include verify:p0a-ops-hardening');
}

const preflight = readRequired('ops/160/complex-nas-preflight.sh');
const schedule = readRequired('ops/160/complex-backup-schedule.example');
const alertTemplate = readRequired('ops/160/complex-backup-alert.example');
const evidence = readRequired('docs/production-readiness-evidence/p0a-ops-hardening-latest.json');

const serverRunbook = readRequired('docs/server-160-runbook-v0.2.md');
const nasRunbook = readRequired('docs/nas-backup-and-restore-runbook-v0.1.md');
const readinessLedger = readRequired('docs/production-readiness-implementation-ledger-template-v0.1.md');
const credentialRunbook = readRequired('docs/production-credential-rotation-runbook-v0.1.md');
const findings = readRequired('security-scans/findings-register-v1.md');
const plan = readRequired('docs/superpowers/plans/2026-06-14-p0a-ops-production-hardening-verifiable.md');

for (const [label, content] of [
  ['complex-nas-preflight.sh', preflight],
  ['complex-backup-schedule.example', schedule],
  ['complex-backup-alert.example', alertTemplate],
  ['p0a evidence', evidence],
  ['server 160 runbook', serverRunbook],
  ['NAS runbook', nasRunbook],
  ['production readiness ledger', readinessLedger],
  ['credential rotation runbook', credentialRunbook],
  ['findings register', findings],
  ['P0-A plan', plan],
]) {
  requireIncludes(content, 'P0-A no-system-write boundary', label);
}

for (const marker of [
  'P0A_NAS_PREFLIGHT',
  'MOUNT_PATH="${MOUNT_PATH:-/mnt/complex-nas-backup}"',
  'NAS_BACKUP_TARGET_DIR',
  'RETENTION_DAYS',
  'CAPACITY_LIMIT_BYTES',
  'findmnt',
  'df -P',
  '.complex-preflight-write-test',
]) {
  requireIncludes(preflight, marker, 'complex-nas-preflight.sh');
}

for (const marker of [
  'P0A_SCHEDULE_TEMPLATE',
  'complex-backup.sh',
  'NAS_BACKUP_TARGET_DIR',
  'REQUIRE_NAS_BACKUP_SYNC=true',
  'ALLOW_UNENCRYPTED_BACKUPS=true',
  'IT-only install step',
]) {
  requireIncludes(schedule, marker, 'complex-backup-schedule.example');
}

for (const marker of [
  'P0A_ALERT_TEMPLATE',
  'WECOM_WEBHOOK_REF',
  'EMAIL_RECIPIENTS_REF',
  'backup_failed',
  'capacity_threshold_exceeded',
  'secret reference only',
]) {
  requireIncludes(alertTemplate, marker, 'complex-backup-alert.example');
}

for (const marker of [
  '"system_write_policy": "codex_no_system_write"',
  '"nas_mount_path": "/mnt/complex-nas-backup"',
  '"schedule_installed_by_codex": false',
  '"plaintext_secret_present": false',
]) {
  requireIncludes(evidence, marker, 'p0a evidence');
}

for (const marker of [
  'PRD-NAS-AUTOMOUNT-001',
  'PRD-NAS-SCHEDULE-001',
  'PRD-NAS-ALERT-001',
  'PRD-NAS-DRILL-CADENCE-001',
]) {
  requireIncludes(readinessLedger, marker, 'production readiness ledger');
}

for (const marker of [
  'SEC-P0-OPS-RECOVERY-001',
  '开机自动挂载',
  '定时备份',
  '容量告警',
  '失败告警',
]) {
  requireIncludes(findings, marker, 'findings register');
}

for (const [label, content] of [
  ['complex-nas-preflight.sh', preflight],
  ['complex-backup-schedule.example', schedule],
  ['complex-backup-alert.example', alertTemplate],
]) {
  forbidPattern(content, /\bsudo\b/, label, 'sudo usage');
  forbidPattern(content, /\bsystemctl\b/, label, 'systemctl usage');
  forbidPattern(content, /\/etc\/fstab/, label, '/etc/fstab write path');
  forbidPattern(content, /\/etc\/systemd\/system/, label, 'systemd system path');
  forbidPattern(content, /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/, label, 'private key');
  forbidPattern(content, /\b(?:password|secret|token)\s*[:=]\s*["'][^"'<\n]{12,}["']/i, label, 'plaintext secret assignment');
  forbidPattern(content, /key=[A-Za-z0-9_-]{16,}/, label, 'webhook key');
}

console.log('P0-A ops hardening verification passed.');
