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

const packageJson = JSON.parse(readRequired('package.json'));
const currentEntry = readRequired('docs/00-current-effective-document-entry-v0.1.md');
const serverRunbook = readRequired('docs/server-160-runbook-v0.2.md');
const nasRunbook = readRequired('docs/nas-backup-and-restore-runbook-v0.1.md');
const productionLedger = readRequired('docs/production-readiness-implementation-ledger-template-v0.1.md');
const projectDashboard = readRequired('docs/project-progress-dashboard.md');
const backendReadme = readRequired('backend/README.md');
const envExample = readRequired('ops/160/complex.env.example');

for (const [label, content] of [
  ['current effective entry', currentEntry],
  ['server 160 runbook', serverRunbook],
  ['NAS runbook', nasRunbook],
  ['production readiness ledger', productionLedger],
  ['project progress dashboard', projectDashboard],
  ['160 env example', envExample],
]) {
  requireIncludes(content, '192.168.88.113', label);
}

for (const marker of [
  'NAS_BACKUP_HOST=192.168.88.113',
  'NAS_BACKUP_TARGET_DIR=',
  'REQUIRE_NAS_BACKUP_SYNC=',
]) {
  requireIncludes(envExample, marker, 'ops/160/complex.env.example');
}

for (const marker of [
  'ssh codex-160',
  'npm run test:backend',
  '/opt/complex/scripts/complex-verify-be000.sh',
  '/opt/complex/scripts/complex-verify-p0.sh',
]) {
  requireIncludes(serverRunbook, marker, 'server 160 runbook');
  requireIncludes(backendReadme, marker, 'backend README');
}

for (const marker of [
  'NAS_BACKUP_TARGET_DIR',
  'REQUIRE_NAS_BACKUP_SYNC',
  'SMB',
  '//192.168.88.113/<share>',
]) {
  requireIncludes(nasRunbook, marker, 'NAS runbook');
}

if (packageJson.scripts?.['verify:ops160-execution-policy'] !== 'node tools/verify-ops160-execution-policy.mjs') {
  throw new Error('package.json must expose verify:ops160-execution-policy');
}

if (!packageJson.scripts?.['verify:ops160-recovery']?.includes('verify:ops160-execution-policy')) {
  throw new Error('verify:ops160-recovery must include verify:ops160-execution-policy');
}

console.log('ops160 execution policy verification passed.');
