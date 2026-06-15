# Legacy API Physical Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the historical TypeScript/Fastify/Prisma/SQLite `apps/api` prototype from the 160 source tree and add a regression gate that prevents it from returning.

**Architecture:** Keep `backend/` as the only formal API implementation. Add a small Node verification script under `tools/` and wire it into the existing verification chain before deleting `apps/api`. Update only the authoritative documentation that currently says the old prototype still physically exists.

**Tech Stack:** Node.js verification script, npm scripts, Git, Java 21 + Spring Boot 4.x backend remains unchanged.

---

### Task 1: Add A Failing Legacy-Prototype Regression Gate

**Files:**
- Create: `tools/verify-no-legacy-api-prototype.mjs`
- Modify: `package.json`

- [ ] **Step 1: Write the failing verification script**

Create `tools/verify-no-legacy-api-prototype.mjs` to reject `apps/api`, legacy root scripts, legacy package-lock entries, and root workspace reintroduction.

- [ ] **Step 2: Wire the script into npm**

Add `"verify:no-legacy-api-prototype": "node tools/verify-no-legacy-api-prototype.mjs"` and include it in `verify:p0-contract` before OpenAPI/client checks.

- [ ] **Step 3: Run RED verification**

Run: `npm run verify:no-legacy-api-prototype`

Expected: FAIL with `NO_LEGACY_API_PROTOTYPE_FAIL` and at least `forbidden legacy API path exists: apps/api`.

### Task 2: Physically Remove The Legacy Prototype

**Files:**
- Delete: `apps/api/**`
- Modify: `docs/prototype-deprecation-and-technology-baseline-v0.1.md`
- Modify: `docs/00-current-effective-document-entry-v0.1.md`
- Modify: `docs/project-progress-dashboard.md`

- [ ] **Step 1: Verify the deletion target**

Run: `realpath apps/api`

Expected: `/home/james/complex-management/apps/api`.

- [ ] **Step 2: Remove tracked and ignored legacy files**

Run: `git rm -r -- apps/api && rm -rf -- apps/api`

Expected: `apps/api` no longer exists and `git status --short` shows deleted legacy files.

- [ ] **Step 3: Update authoritative docs**

Rewrite active references from ?old prototype exists but scripts are blocked? to ?old prototype has been physically removed; restoration is forbidden and guarded by `npm run verify:no-legacy-api-prototype`?.

### Task 3: GREEN Verification And Review

**Files:**
- Verify: `tools/verify-no-legacy-api-prototype.mjs`
- Verify: `package.json`
- Verify: active docs changed above

- [ ] **Step 1: Run the new targeted verification**

Run: `npm run verify:no-legacy-api-prototype`

Expected: PASS with `NO_LEGACY_API_PROTOTYPE_PASS`.

- [ ] **Step 2: Run existing gates**

Run: `npm run verify:p0-contract && npm run test:backend && npm run build && git diff --check`

Expected: all commands exit 0.

- [ ] **Step 3: Run security checks**

Run: `~/.local/bin/semgrep scan --config p/owasp-top-ten --config p/secrets --metrics=off --json --output security-scans/latest-semgrep-output.json backend apps/web packages tools`

Expected: no findings introduced by this change.

- [ ] **Step 4: Commit**

Run: `git add package.json tools/verify-no-legacy-api-prototype.mjs docs/superpowers/plans/2026-06-15-legacy-api-physical-removal.md docs/prototype-deprecation-and-technology-baseline-v0.1.md docs/00-current-effective-document-entry-v0.1.md docs/project-progress-dashboard.md && git add -u apps/api && git commit -m "chore: remove legacy api prototype"`

Expected: commit created on 160 `master`; local machine remains documentation-only.
