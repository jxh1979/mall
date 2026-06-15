# P0 NAS Backup Restore Drill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the NAS backup and restore prerequisite from a document-only blocker into repeatable scripts and verifiable evidence.

**Architecture:** Extend the existing 160 backup script with checksum and optional NAS sync, then add a restore drill script that decrypts/extracts a backup and restores PostgreSQL into a temporary database only. Do not restore over the live database.

**Tech Stack:** Bash, Docker PostgreSQL utilities, OpenSSL, sha256sum, Node.js static verifier.

---

### Task 1: Add Backup/Restore Script Contract Verification

**Files:**
- Create: `tools/verify-160-backup-restore-scripts.mjs`
- Modify: `package.json`

- [ ] **Step 1: Write failing verifier**

The verifier checks that:
- `ops/160/complex-backup.sh` writes `.sha256` files and supports `NAS_BACKUP_TARGET_DIR`.
- `ops/160/complex-restore-drill.sh` exists and restores only into `complex_restore_drill_*`.
- `ops/160/complex-restore-drill.sh` emits `restore.drill_completed` and `restore-drill-report.json`.
- `package.json` exposes `verify:ops160-recovery`.

- [ ] **Step 2: Run verifier to confirm RED**

Run: `node tools/verify-160-backup-restore-scripts.mjs`

Expected: FAIL because `ops/160/complex-restore-drill.sh` does not exist yet.

### Task 2: Extend Backup Script

**Files:**
- Modify: `ops/160/complex-backup.sh`

- [ ] **Step 1: Add checksum generation**

For encrypted backup archives, write `<archive>.sha256` using `sha256sum` and restrict permissions to `600`.

- [ ] **Step 2: Add optional NAS sync**

If `NAS_BACKUP_TARGET_DIR` is set, copy the encrypted archive and checksum into `$NAS_BACKUP_TARGET_DIR/$BACKUP_KIND/`. If `REQUIRE_NAS_BACKUP_SYNC=true`, fail when the target is not configured or not writable.

### Task 3: Add Restore Drill Script

**Files:**
- Create: `ops/160/complex-restore-drill.sh`

- [ ] **Step 1: Resolve backup source**

Accept an explicit backup archive or `latest`. For `latest`, use the newest encrypted backup under `/opt/complex/backups/$RESTORE_BACKUP_KIND`.

- [ ] **Step 2: Decrypt and extract**

Use the same encryption parameters as `complex-backup.sh`, require passphrase permissions, and verify checksum when a `.sha256` file exists.

- [ ] **Step 3: Restore into temporary DB**

Create `complex_restore_drill_<timestamp>`, restore `postgres-complex.dump`, verify required tables, record Flyway version, then drop the temporary DB unless `KEEP_RESTORE_DB=true`.

- [ ] **Step 4: Emit evidence**

Write `restore-drill-report.json` under `$HOME/.complex/restore-drills/<drill-id>/` and print `restore.drill_completed`.

### Task 4: Deploy and Verify on 160

**Files:**
- Copy changed files to `/home/james/complex-management`
- Copy scripts to `/opt/complex/scripts`

- [ ] **Step 1: Run local verifier**

Run: `npm run verify:ops160-recovery`

Expected: PASS.

- [ ] **Step 2: Run remote verifier**

Run: `ssh codex-160 "cd /home/james/complex-management && npm run verify:ops160-recovery"`

Expected: PASS.

- [ ] **Step 3: Run encrypted backup**

Run: `ssh codex-160 "/opt/complex/scripts/complex-backup.sh manual"`

Expected: encrypted archive and `.sha256` generated.

- [ ] **Step 4: Run restore drill**

Run: `ssh codex-160 "/opt/complex/scripts/complex-restore-drill.sh latest"`

Expected: `restore.drill_completed`, temporary DB verified and removed.

- [ ] **Step 5: Re-run P0 verification**

Run: `ssh codex-160 "AUTO_START_RUNTIME=true /opt/complex/scripts/complex-verify-p0.sh"`

Expected: `P0 verification PASS`.
