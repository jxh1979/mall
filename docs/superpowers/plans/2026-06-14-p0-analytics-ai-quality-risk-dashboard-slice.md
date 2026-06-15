# P0 Analytics AI Quality Risk Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the P0 analytics, AI quality ledger, risk alert, and dashboard endpoints defined in `docs/openapi/openapi-p0.yaml`.

**Architecture:** Keep this as a Spring Boot backend slice with deterministic Java ledger/rule handling only. Do not start a Python AI service, do not call third-party AI, and do not add async model inference; persist the P0 boundary fields that allow a later Python AI service to plug in.

**Tech Stack:** Java 21, Spring Boot 4.x, PostgreSQL, Flyway, `NamedParameterJdbcTemplate`, JUnit, MockMvc, Testcontainers-compatible PostgreSQL profile already used by the project.

---

## Scope And Contract

Implement these P0 endpoints exactly:

- `GET /api/v1/analytics/ai-quality/results`, permission `analytics.risk_alert.view`, success event `analytics.ai_quality_result.viewed`, failure event `analytics.ai_quality_result.viewed_failed`.
- `GET /api/v1/analytics/ai-quality/tasks`, permission `analytics.risk_alert.view`, success event `analytics.ai_quality_task.viewed`, failure event `analytics.ai_quality_task.viewed_failed`.
- `GET /api/v1/analytics/customer-intents`, permission `analytics.risk_alert.view`, success event `analytics.customer_intent.viewed`, failure event `analytics.customer_intent.viewed_failed`.
- `GET /api/v1/analytics/dashboards/p0-overview`, permission `analytics.dashboard.view`, success event `analytics.dashboard_viewed`, failure event `analytics.dashboard_view_failed`.
- `GET /api/v1/analytics/merchant-health`, permission `analytics.dashboard.view`, success event `analytics.merchant_health.viewed`, failure event `analytics.merchant_health.viewed_failed`.
- `GET /api/v1/analytics/merchants`, permission `analytics.dashboard.view`, success event `analytics.merchant_summary.viewed`, failure event `analytics.merchant_summary.viewed_failed`.
- `GET /api/v1/analytics/metric-anomalies`, permission `analytics.risk_alert.view`, success event `analytics.metric_anomaly.viewed`, failure event `analytics.metric_anomaly.viewed_failed`.
- `GET /api/v1/analytics/metric-definitions`, permission `analytics.dashboard.view`, success event `analytics.metric_definition.viewed`, failure event `analytics.metric_definition.viewed_failed`.
- `GET /api/v1/analytics/risk-alerts`, permission `analytics.risk_alert.view`, success event `risk_alert.viewed`, failure event `risk_alert.view_failed`.
- `POST /api/v1/analytics/risk-alerts`, permission `analytics.risk_alert.manage`, idempotency required, success event `risk_alert.created`, failure event `risk_alert.create_failed`.
- `POST /api/v1/analytics/risk-alerts/{id}/resolve`, permission `analytics.risk_alert.view`, idempotency required, success event `analytics.risk_alert.resolved`, failure event `analytics.risk_alert.resolved_failed`.
- `GET /api/v1/analytics/risk-rules`, permission `analytics.risk_alert.view`, success event `analytics.risk_rule.viewed`, failure event `analytics.risk_rule.viewed_failed`.
- `GET /api/v1/analytics/robot`, permission `analytics.dashboard.view`, success event `analytics.robot.viewed`, failure event `analytics.robot.viewed_failed`.
- `GET /api/v1/analytics/staff-service-score`, permission `analytics.risk_alert.view`, success event `analytics.staff_service_score.viewed`, failure event `analytics.staff_service_score.viewed_failed`.

P0 business boundaries:

- AI quality is a ledger and rule-output view in P0. It stores task/result/finding metadata and a `processing_mode`/`model_provider`/`model_version`/`confidence_score` boundary, but it must not call external AI or spawn local Python.
- Dashboard values are persisted snapshots and ledgers. Do not build a live BI engine in P0.
- Risk alert create/resolve must be idempotent and must keep audit trail.
- `risk_level` must be one of `normal`, `suspicious`, `high`, `critical`.
- `resolve` must require `idempotency_key`, target status from `StatusChangeRequest`, and an operator note/reason.
- Money/count fields must use integer cents/counts or `BigDecimal`; no `double`/`float` analytics calculations.
- SQL must use `NamedParameterJdbcTemplate`; no string-built SQL.

## File Structure

Create:

- `backend/src/main/resources/db/migration/V017__p0_analytics_ai_quality_risk_dashboard.sql`
- `backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java`
- `backend/src/main/java/com/complex/platform/analytics/AnalyticsService.java`
- `backend/src/main/java/com/complex/platform/analytics/AnalyticsRepository.java`
- `backend/src/main/java/com/complex/platform/analytics/JdbcAnalyticsRepository.java`
- `backend/src/main/java/com/complex/platform/analytics/AnalyticsRecords.java`
- `backend/src/main/java/com/complex/platform/analytics/RiskAlertRequest.java`
- `backend/src/main/java/com/complex/platform/analytics/StatusChangeRequest.java`
- `backend/src/test/java/com/complex/platform/analytics/AnalyticsControllerTests.java`
- `backend/src/test/java/com/complex/platform/analytics/AnalyticsServiceTests.java`
- `backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java`

Modify only if required by compilation:

- Existing `backend/src/test/java/**/JdbcIntegrationTests.java` files that assert the latest Flyway version; update expected version from `016` to `017`.

Do not modify existing V001-V016 migrations.

## Data Model

Migration V017 must create and seed:

- `analytics_metric_definitions`: metric key, name, domain, unit, calculation_note, owner_role, status, created_at, updated_at.
- `analytics_dashboard_snapshots`: snapshot_key, project_id, business_date, totals as integer counts/cents, risk counts, payload_json, created_at.
- `analytics_merchant_summaries`: merchant_id, merchant_name, merchant_type, revenue_amount_cents, order_count, risk_alert_count, settlement_status, business_date, created_at, updated_at.
- `analytics_merchant_health_scores`: merchant_id, score, score_level, risk_count, overdue_ar_amount_cents, last_reviewed_at, summary, created_at, updated_at.
- `analytics_metric_anomalies`: metric_key, domain, risk_level, observed_value, expected_value, summary, status, detected_at, resolved_at.
- `analytics_risk_rules`: rule_id, rule_code, rule_name, domain, risk_level, enabled, rule_config_json, action_note, created_at, updated_at.
- `analytics_risk_alerts`: id, rule_id, alert_type, risk_level, object_type, object_id, summary, status, operator_note, evidence_attachment_ids_json, idempotency_key, created_at, resolved_at, updated_at.
- `analytics_ai_quality_tasks`: task_id, source_type, source_id, processing_mode, model_provider, model_version, status, requested_by, created_at, completed_at.
- `analytics_ai_quality_results`: result_id, task_id, source_type, source_id, quality_score, risk_level, confidence_score, summary, finding_count, created_at.
- `analytics_customer_intents`: intent_id, source_type, source_id, customer_ref, intent_type, confidence_score, risk_level, summary, created_at.
- `analytics_robot_metrics`: metric_id, robot_id, provider_code, task_count, success_count, failure_count, business_date, summary, created_at.
- `analytics_staff_service_scores`: score_id, staff_id, staff_name, role_code, service_score, complaint_count, positive_event_count, risk_level, business_date, created_at.

Seed permissions:

- `analytics.dashboard.view`
- `analytics.risk_alert.view`
- `analytics.risk_alert.manage`

Add the permissions to the platform admin role in the same style as previous migrations.

Seed at least one active/sample row for every read endpoint so P0 smoke and integration tests have deterministic data.

## Task 1: Controller Contract Tests

**Files:**

- Create: `backend/src/test/java/com/complex/platform/analytics/AnalyticsControllerTests.java`
- Later create: `backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java`

- [ ] **Step 1: Write failing MockMvc tests**

Test all 14 endpoints. Assert each endpoint returns `$.code == "OK"` when the mocked service returns a record/page. Verify exact permission strings:

```java
verify(authorizationService).requirePermission("analytics.dashboard.view");
verify(authorizationService).requirePermission("analytics.risk_alert.view");
verify(authorizationService).requirePermission("analytics.risk_alert.manage");
```

Also test that both write endpoints reject missing `idempotency_key` with `VALIDATION_FAILED`.

- [ ] **Step 2: Run controller test and verify RED**

Run:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=AnalyticsControllerTests test
```

Expected: fail because `AnalyticsController` and analytics records do not exist.

- [ ] **Step 3: Implement minimal controller and records**

Create the controller with exact request mappings, response wrappers, permission checks, and idempotency key validation. Keep it thin: delegate all business behavior to `AnalyticsService`.

- [ ] **Step 4: Run controller test and verify GREEN**

Run the same Maven command. Expected: `Tests run` with zero failures/errors.

## Task 2: Service Behavior Tests

**Files:**

- Create: `backend/src/test/java/com/complex/platform/analytics/AnalyticsServiceTests.java`
- Later create: `backend/src/main/java/com/complex/platform/analytics/AnalyticsService.java`
- Later create: `backend/src/main/java/com/complex/platform/analytics/AnalyticsRepository.java`

- [ ] **Step 1: Write failing service tests**

Cover these behaviors:

- list endpoints call repository and record the exact success audit event.
- repository exception records the exact failure audit event and rethrows.
- `createRiskAlert` rejects invalid `risk_level`.
- `createRiskAlert` rejects missing `alert_type`, `object_type`, or `summary`.
- `createRiskAlert` uses `IdempotencyService.execute("analytics.create-risk-alert", idempotency_key, request, ...)`.
- `resolveRiskAlert` rejects missing target status/note.
- `resolveRiskAlert` rejects unknown alert id with `RISK_ALERT_NOT_FOUND`.
- `resolveRiskAlert` rejects already resolved alert with `RISK_ALERT_ALREADY_RESOLVED`.
- `resolveRiskAlert` uses `IdempotencyService.execute("analytics.resolve-risk-alert", idempotency_key, request, ...)`.

- [ ] **Step 2: Run service test and verify RED**

Run:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=AnalyticsServiceTests test
```

Expected: fail because service and repository do not exist.

- [ ] **Step 3: Implement minimal service**

Use immutable record DTOs. Generate IDs with `UUID.randomUUID().toString()`. Use `Clock` injection for deterministic tests. Do not use double/float. Store confidence and rates as `BigDecimal`.

- [ ] **Step 4: Run service test and verify GREEN**

Run the same Maven command. Expected: zero failures/errors.

## Task 3: Migration And Repository Tests

**Files:**

- Create: `backend/src/main/resources/db/migration/V017__p0_analytics_ai_quality_risk_dashboard.sql`
- Create: `backend/src/main/java/com/complex/platform/analytics/JdbcAnalyticsRepository.java`
- Create: `backend/src/test/java/com/complex/platform/analytics/AnalyticsJdbcIntegrationTests.java`
- Modify: existing latest-Flyway integration tests from `016` to `017` only when they assert current version.

- [ ] **Step 1: Write failing JDBC integration tests**

Assert:

- all V017 tables exist with `to_regclass`.
- the three analytics permissions exist.
- platform admin role contains the three analytics permissions.
- seeded rows exist for metric definitions, dashboard snapshot, merchant summary, merchant health, metric anomaly, risk rule, AI quality task/result, customer intent, robot metric, staff service score, and risk alert.
- create/list/resolve risk alert persists `status`, `idempotency_key`, `operator_note`, `evidence_attachment_ids`, `resolved_at`.
- latest Flyway version is `017`.

- [ ] **Step 2: Run JDBC test and verify RED**

Run:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=AnalyticsJdbcIntegrationTests test
```

Expected: fail because V017 tables do not exist.

- [ ] **Step 3: Implement migration and JDBC repository**

Use `jsonb` for structured payload fields. Use `CHECK` constraints for statuses and `risk_level`. Use unique index on `analytics_risk_alerts(idempotency_key)` where not null.

- [ ] **Step 4: Run JDBC test and verify GREEN**

Run the same Maven command. Expected: zero failures/errors.

## Task 4: Contract And Security Review

**Files:**

- Review: `docs/openapi/openapi-p0.yaml`
- Review: `docs/p0-openapi-operation-acceptance-matrix-v0.1.md`
- Review: `backend/src/main/java/com/complex/platform/analytics/**`
- Review: `backend/src/test/java/com/complex/platform/analytics/**`

- [ ] **Step 1: Run targeted analytics tests**

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=AnalyticsServiceTests,AnalyticsControllerTests,AnalyticsJdbcIntegrationTests test
```

Expected: zero failures/errors.

- [ ] **Step 2: Run static safety grep**

```bash
cd /home/james/complex-management
grep -R -n -E 'createStatement|Statement|String\.format|SELECT .*\+|UPDATE .*\+|INSERT .*\+|DELETE .*\+|Double|double|getDouble|Math\.round|float|Float|python|subprocess|ProcessBuilder|Runtime\.getRuntime|http://|WebClient|RestTemplate|OkHttp|Feign' backend/src/main/java/com/complex/platform/analytics backend/src/test/java/com/complex/platform/analytics || true
```

Expected: no output except harmless test string literals if present; remove or justify any production hit.

- [ ] **Step 3: Run full P0 verification**

```bash
/opt/complex/scripts/complex-verify-p0.sh
```

Expected: backend tests pass, frontend type/build pass, Android terminal check pass, OpenAPI contract check pass, health check pass, and final output includes `P0 verification PASS`.

## Acceptance Criteria

- All 14 analytics OpenAPI operations are implemented and return the existing `ApiResponse`/`PageResponse` shapes.
- Every endpoint enforces the exact OpenAPI permission.
- Every endpoint records the exact OpenAPI success/failure event through existing audit infrastructure.
- Write endpoints reject missing idempotency keys before calling service logic.
- Risk alert creation is idempotent and validates required fields and enum values.
- Risk alert resolve is idempotent and blocks already-resolved alerts.
- V017 migration creates all analytics P0 tables and seed data without modifying older migrations.
- No hardcoded AI credentials, AI URLs, Python process execution, or outbound AI calls are introduced.
- No `double`/`float` calculations are introduced in analytics production code.
- Targeted analytics tests and full P0 verification pass.
