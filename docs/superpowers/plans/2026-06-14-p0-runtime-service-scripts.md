# P0 Runtime Service Scripts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the 160 P0 runtime reproducible by adding one user-space runtime script for API/Web start, stop, and status, then wiring P0 verification to optionally start it.

**Architecture:** Keep production deployment scripts intact. Add a lightweight runtime manager under `ops/160` that uses the checked-out project, user-owned pid files, and user-owned logs, so it works without sudo on 192.168.88.160.

**Tech Stack:** Bash, Node.js verification script, Java 21 Spring Boot 4.x jar, Vite preview/dev server.

---

### Task 1: Add Runtime Script Contract Verification

**Files:**
- Create: `tools/verify-160-runtime-scripts.mjs`

- [ ] **Step 1: Write failing verifier**

Create a Node.js verifier that checks `ops/160/complex-runtime.sh` exists and `ops/160/complex-verify-p0.sh` supports optional auto-start.

- [ ] **Step 2: Run verifier to confirm RED**

Run: `node tools/verify-160-runtime-scripts.mjs`

Expected: FAIL because `ops/160/complex-runtime.sh` does not exist yet.

### Task 2: Add User-Space Runtime Manager

**Files:**
- Create: `ops/160/complex-runtime.sh`

- [ ] **Step 1: Implement `up`**

Start API from `backend/target/complex-api-0.1.0-SNAPSHOT.jar` on `COMPLEX_API_PORT`, building the jar if missing. Start Web on `WEB_PORT` by preferring `npm run dev --workspace apps/web -- --host 127.0.0.1 --port 3000 --strictPort`.

- [ ] **Step 2: Implement `status`**

Check API actuator health and Web HTTP 200. Print `api=running` / `api=down` and `web=running` / `web=down`.

- [ ] **Step 3: Implement `down`**

Stop only processes recorded in the script's pid files. Do not use broad process-kill patterns in the runtime script.

### Task 3: Wire Runtime Into P0 Verification

**Files:**
- Modify: `ops/160/complex-verify-p0.sh`

- [ ] **Step 1: Add optional auto-start**

If `AUTO_START_RUNTIME=true`, run `complex-runtime.sh up` before checks and `complex-runtime.sh status` before the final health check.

- [ ] **Step 2: Keep default strict behavior**

Default remains `AUTO_START_RUNTIME=false`, so production verification still fails if runtime is not already running.

### Task 4: Deploy and Verify on 160

**Files:**
- Copy changed scripts to `/home/james/complex-management/ops/160`
- Copy changed scripts to `/opt/complex/scripts`

- [ ] **Step 1: Run local verifier**

Run: `node tools/verify-160-runtime-scripts.mjs`

Expected: PASS.

- [ ] **Step 2: Run remote verifier**

Run: `ssh codex-160 "cd /home/james/complex-management && node tools/verify-160-runtime-scripts.mjs"`

Expected: PASS.

- [ ] **Step 3: Runtime restart verification**

Run `complex-runtime.sh down`, then `complex-runtime.sh up`, then `complex-runtime.sh status`.

Expected: API and Web both running.

- [ ] **Step 4: Full P0 verification**

Run: `AUTO_START_RUNTIME=true /opt/complex/scripts/complex-verify-p0.sh`

Expected: `P0 verification PASS`.
