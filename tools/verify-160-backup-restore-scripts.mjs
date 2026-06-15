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

const backup = readRequired('ops/160/complex-backup.sh');
const restore = readRequired('ops/160/complex-restore-drill.sh');
const packageJson = JSON.parse(readRequired('package.json'));

for (const marker of [
  'NAS_BACKUP_TARGET_DIR',
  'REQUIRE_NAS_BACKUP_SYNC',
  'sync_to_nas()',
  'ALLOW_UNENCRYPTED_BACKUPS',
  'Backup completed without encryption',
  'sha256sum',
  '.sha256',
]) {
  requireIncludes(backup, marker, 'complex-backup.sh');
}

if (!/ALLOW_UNENCRYPTED_BACKUPS:-false[^]*?ENCRYPT_BACKUP=false/.test(backup)) {
  throw new Error('complex-backup.sh must let ALLOW_UNENCRYPTED_BACKUPS=true explicitly select unencrypted archives');
}

for (const marker of [
  'complex_restore_drill_',
  'pg_restore',
  'restore.drill_completed',
  'restore-drill-report.json',
  '*.tar.gz.enc',
  '*.tar.gz',
  'unencrypted .tar.gz archive',
  'KEEP_RESTORE_DB',
  'dropdb',
]) {
  requireIncludes(restore, marker, 'complex-restore-drill.sh');
}

if (!/dropdb[\s\S]*"\$RESTORE_DB"/.test(restore)) {
  throw new Error('complex-restore-drill.sh must drop only the generated restore drill database');
}

const unsafeDropLine = restore
  .split(/\r?\n/)
  .find((line) => /\bdropdb\b/.test(line) && !line.includes('$RESTORE_DB'));

if (unsafeDropLine) {
  throw new Error('complex-restore-drill.sh must not drop the live complex database');
}

if (packageJson.scripts?.['verify:ops160-backup-restore'] !== 'node tools/verify-160-backup-restore-scripts.mjs') {
  throw new Error('package.json must expose verify:ops160-backup-restore');
}

if (!packageJson.scripts?.['verify:ops160-recovery']?.includes('verify:ops160-backup-restore')) {
  throw new Error('verify:ops160-recovery must include verify:ops160-backup-restore');
}

console.log('160 backup and restore script verification passed.');
