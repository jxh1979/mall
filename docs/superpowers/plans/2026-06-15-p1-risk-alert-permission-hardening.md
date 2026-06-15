# P1 Risk Alert Permission Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tighten the risk alert resolve write operation so it requires `analytics.risk_alert.manage`, matching the permission matrix and avoiding write access through view-only permission.

**Architecture:** This is a narrow P1 security hardening slice. It changes only the analytics risk alert resolve permission in controller, service audit metadata, OpenAPI contract files, and generated acceptance/closure documents.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL/Flyway, Maven, OpenAPI YAML/JSON, Semgrep.

---

### Task 1: Controller Permission Test

**Files:**
- Modify: `backend/src/test/java/com/complex/platform/analytics/AnalyticsControllerTests.java`

- [x] **Step 1: Write the failing test**

Change the route permission expectation for:

`POST /api/v1/analytics/risk-alerts/alert_1/resolve`

from:

```java
"POST,/api/v1/analytics/risk-alerts/alert_1/resolve,analytics.risk_alert.view",
```

to:

```java
"POST,/api/v1/analytics/risk-alerts/alert_1/resolve,analytics.risk_alert.manage",
```

- [x] **Step 2: Run test to verify it fails**

Run on 160:

```bash
cd /home/james/complex-management/backend
mvn -Dtest=AnalyticsControllerTests#analyticsRoutesUseExactOpenApiPermission test
```

Expected: FAIL because implementation still calls `analytics.risk_alert.view`.

Actual evidence: failed on 160 as expected; `AnalyticsController.resolveRiskAlert` still invoked `analytics.risk_alert.view` before implementation.

### Task 2: Minimal Implementation

**Files:**
- Modify: `backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java`
- Modify: `backend/src/main/java/com/complex/platform/analytics/AnalyticsService.java`

- [x] **Step 1: Change controller permission**

Use `analytics.risk_alert.manage` in `resolveRiskAlert`.

- [x] **Step 2: Change service audit permission metadata**

Use `analytics.risk_alert.manage` in the `executeWrite` call for `resolveRiskAlert`.

- [x] **Step 3: Run focused tests**

```bash
cd /home/james/complex-management/backend
mvn -Dtest=AnalyticsControllerTests,AnalyticsServiceTests test
```

Expected: PASS.

Actual evidence: `AnalyticsControllerTests` and `AnalyticsServiceTests` passed on 160, 26 tests, 0 failures, 0 errors.

### Task 3: Contract Synchronization

**Files:**
- Modify: `docs/openapi/openapi-p0.yaml`
- Modify: `docs/openapi/openapi-p0.json`
- Modify: `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`
- Modify: `docs/p0-openapi-contract-closure-appendix-v0.1.md`

- [x] **Step 1: Update `resolveRiskAlert` permission**

Set `x-permission` and matrix/closure permission to `analytics.risk_alert.manage`.

- [x] **Step 2: Run contract gate**

```bash
cd /home/james/complex-management
npm run verify:p0-contract
```

Expected: PASS.

Actual evidence: regenerated operation matrix and closure appendix, then `npm run verify:p0-contract` passed on 160.

### Task 4: Security And Completion Verification

**Files:**
- No additional files.

- [x] **Step 1: Run backend focused and full tests**

```bash
cd /home/james/complex-management/backend
mvn -Dtest=AnalyticsControllerTests,AnalyticsServiceTests,AnalyticsJdbcIntegrationTests test
mvn test
```

Expected: PASS.

Actual evidence: focused analytics/JDBC suite passed, then full backend `mvn test` passed on 160, 528 tests, 0 failures, 0 errors.

- [x] **Step 2: Run Semgrep**

```bash
cd /home/james/complex-management
~/.local/bin/semgrep --metrics=off --config p/java --error backend/src/main/java/com/complex/platform/analytics backend/src/test/java/com/complex/platform/analytics
```

Expected: 0 findings.

Actual evidence: targeted analytics Semgrep scan covered 7 files with 60 Java rules and returned 0 findings.

- [x] **Step 3: Re-scan P1 completion**

Confirm there are no remaining P1 coding gaps except external supplier/API/hardware dependencies already excluded by `p1-coding-completion-audit-v0.1.md`.

Actual evidence: P1 completion audit updated with the latest verification evidence and the remaining non-coding external dependencies.
