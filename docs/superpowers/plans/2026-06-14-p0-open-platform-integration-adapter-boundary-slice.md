# P0 Open Platform Integration Adapter Boundary Slice Plan

## 1. Objective

Implement the P0 open-platform and third-party integration boundary slice on 192.168.88.160 with Java 21 + Spring Boot 4.x + PostgreSQL + Flyway.

This slice must turn the OpenAPI open-platform and integration contract into executable backend code for server-to-server auth, webhook receiving, event result query, provider adapter configuration, mock adapter testing, Webhook + polling boundary language, field-mapping placeholders, retry/compensation ledgers, and read-only third-party operational ledgers.

## 2. In Scope

Formalize these OpenAPI P0 open-platform paths:

- `POST /api/open/auth/token`
- `GET /api/open/d/{endpoint_key}/events/{event_id}`
- `POST /api/open/d/{endpoint_key}/webhooks/{provider}/{event}`

Formalize these OpenAPI P0 integration paths:

- `GET /api/v1/integration/compensation-tasks`
- `GET /api/v1/integration/devices`
- `GET /api/v1/integration/devices/events`
- `GET /api/v1/integration/devices/offline-sync-batches`
- `GET /api/v1/integration/order-syncs`
- `GET /api/v1/integration/provider-adapters`
- `POST /api/v1/integration/provider-adapters`
- `PUT /api/v1/integration/provider-adapters/{id}`
- `POST /api/v1/integration/provider-adapters/{id}/mark-production-ready`
- `POST /api/v1/integration/provider-adapters/{id}/test-mock`
- `GET /api/v1/integration/retail/skus`
- `GET /api/v1/integration/retry-tasks`
- `GET /api/v1/integration/robot/task-events`
- `GET /api/v1/integration/robot/tasks`
- `GET /api/v1/integration/video/cameras`
- `GET /api/v1/integration/voucher-verifications`
- `GET /api/v1/integration/wecom/bindings`
- `GET /api/v1/integration/wecom/conversations`
- `GET /api/v1/integration/wecom/messages`
- `GET /api/v1/integration/wecom/risk-hits`

## 3. Out of Scope

- Real supplier SDK/API calls.
- Real payment channel direct connection.
- Real Enterprise WeChat, robot, video, retail, catering, face-age provider integration.
- Production secret storage. P0 stores secret references and uses test/mock resolver only.
- Complex async workers. P0 stores retry and compensation tasks for later scheduled workers.

## 4. Files To Add Or Modify

Add:

- `backend/src/main/resources/db/migration/V015__p0_open_platform_integration_adapter_boundary.sql`
- `backend/src/main/java/com/complex/platform/integration/IntegrationController.java`
- `backend/src/main/java/com/complex/platform/integration/OpenPlatformController.java`
- `backend/src/main/java/com/complex/platform/integration/IntegrationService.java`
- `backend/src/main/java/com/complex/platform/integration/IntegrationRepository.java`
- `backend/src/main/java/com/complex/platform/integration/JdbcIntegrationRepository.java`
- `backend/src/main/java/com/complex/platform/integration/*Record.java`
- `backend/src/main/java/com/complex/platform/integration/*Request.java`
- `backend/src/main/java/com/complex/platform/integration/*Response.java`
- `backend/src/test/java/com/complex/platform/integration/IntegrationControllerTests.java`
- `backend/src/test/java/com/complex/platform/integration/OpenPlatformControllerTests.java`
- `backend/src/test/java/com/complex/platform/integration/IntegrationServiceTests.java`
- `backend/src/test/java/com/complex/platform/integration/IntegrationJdbcIntegrationTests.java`

Modify only if required:

- `backend/src/test/java/com/complex/platform/governance/PlatformJdbcIntegrationTests.java` if it asserts latest Flyway version.
- `backend/src/test/java/com/complex/platform/amusement/AmusementJdbcIntegrationTests.java` if it asserts latest Flyway version.
- OpenAPI generated closure/matrix files only if contract generation requires sync; do not invent undocumented endpoints.

## 5. Data Model

Add PostgreSQL tables with idempotent Flyway DDL:

- `open_app_credentials`
  - `id`, `app_key`, `secret_ref`, `status`, `allowed_endpoint_keys`, `created_at`, `updated_at`
  - Do not store production app secrets. `secret_ref` points to manual ledger or secret manager.
- `open_app_tokens`
  - `id`, `app_key`, `token_hash`, `issued_at`, `expires_at`, `status`, `nonce`, `created_at`
  - Store token hash only. Return the raw token only once from auth response.
- `open_app_nonce_replays`
  - `id`, `app_key`, `nonce`, `timestamp_value`, `created_at`
  - Reject duplicate nonce for the same app.
- `open_endpoint_configs`
  - `id`, `endpoint_key`, `provider_code`, `secret_ref`, `status`, `webhook_enabled`, `polling_check_enabled`, `created_at`, `updated_at`
- `open_api_events`
  - `id`, `endpoint_key`, `provider_code`, `event_name`, `idempotency_key`, `signature_valid`, `event_status`, `payload_json`, `result_json`, `failure_reason`, `received_at`, `processed_at`
- `integration_provider_adapters`
  - `id`, `provider_code`, `adapter_type`, `mode`, `capabilities`, `environment`, `status`, `webhook_enabled`, `polling_check_enabled`, `field_mapping_json`, `webhook_config_json`, `polling_config_json`, `credential_secret_ref`, `production_ready`, `operator_note`, `evidence_attachment_ids`, `created_at`, `updated_at`
  - Unique on `provider_code`.
- `integration_provider_adapter_tests`
  - `id`, `adapter_id`, `provider_code`, `scenario`, `mock_payload_json`, `test_status`, `result_json`, `created_at`
- `integration_read_ledger`
  - `id`, `ledger_type`, `provider_code`, `business_id`, `status`, `payload_json`, `created_at`, `updated_at`
  - Used by read-only endpoints: devices, device events, offline sync batches, order syncs, retail SKUs, retry tasks, robot tasks, robot task events, video cameras, voucher verifications, WeCom bindings/conversations/messages/risk hits, compensation tasks.

Seed permissions and attach to `role_platform_admin`:

- `integration.provider_adapter.view`
- `integration.provider_adapter.create`
- `integration.provider_adapter.update`
- `integration.provider_adapter.production_ready`
- `integration.provider_adapter.test`

Seed P0 mock boundary data:

- One `open_app_credentials` record for P0 contract tests with `app_key=p0-open-app`, `secret_ref=manual-ledger:p0-open-app`.
- One `open_endpoint_configs` record with `endpoint_key=p0-default`, `provider_code=mock-provider`, `secret_ref=manual-ledger:p0-webhook`.
- A few `integration_read_ledger` rows for each read-only ledger type so list endpoints return stable shapes.

## 6. Business Rules

- Open platform auth:
  - `app_key`, `timestamp`, `nonce`, and `signature` are required.
  - Reject disabled or unknown app.
  - Reject replayed nonce.
  - Reject stale timestamp outside the P0 tolerance window.
  - Verify HMAC-SHA256 signature over a deterministic canonical string.
  - Store only token hash. Return raw token only in the auth response.
  - Audit success as `open_auth.succeeded`, failure as `open_auth.failed`.
- Webhook receiving:
  - `endpoint_key`, `provider`, and `event` must resolve to an enabled endpoint/provider boundary.
  - Require idempotency key from request body `idempotency_key` or `X-Idempotency-Key`.
  - Verify webhook signature using endpoint/provider secret reference and canonical payload.
  - Store raw payload as JSONB and create `open_api_events`.
  - Duplicate idempotency key must return the original accepted result.
  - Audit success as `api_call.logged`, failure as `api_call.rejected`.
- Event result query:
  - Require valid open-app signature.
  - Return the event status/result by `endpoint_key` + `event_id`.
  - Return `404` when event is unknown.
- Provider adapter create/update:
  - Validate adapter type/mode/status/capabilities against OpenAPI enums.
  - Store field mapping, webhook config, and polling config as JSONB placeholders.
  - Store `credential_secret_ref` only. Do not store plaintext API key/secret/password.
  - `production_ready=true` is rejected unless status is `production_ready`, environment is `production`, webhook is enabled, polling check is enabled, and `credential_secret_ref` is present.
- Mark production ready:
  - Requires reason.
  - Requires provider adapter to have production environment, Webhook enabled, polling enabled, non-empty capabilities, and secret reference.
  - Updates status to `production_ready` and `production_ready=true`.
- Mock adapter test:
  - Supports scenarios `auth`, `webhook`, `polling`, `reconcile`, `field_mapping`, `failure_retry`.
  - Writes a test record with status `passed` or fails with `424`-style business conflict when adapter is not testable.
- Read-only ledgers:
  - Return paged ledger rows for each OpenAPI list endpoint.
  - Do not call real suppliers.
  - All rows carry provider code, status, payload, timestamps for later reconciliation.
- Retry/compensation:
  - P0 records tasks and statuses only; scheduled execution remains future work.

## 7. Interface Rules

- Staff `/api/v1/integration/**` endpoints must call `AuthorizationService.requirePermission(...)` with exact OpenAPI permissions.
- `/api/open/**` endpoints must not require staff permission. They must enforce server-to-server signature/auth context.
- All staff write endpoints with `x-idempotency-required: true` must reject missing/blank `idempotency_key` with `VALIDATION_FAILED`.
- Open webhook must enforce idempotency through body/header idempotency key.
- All write operations must go through `IdempotencyService` or a repository-level unique idempotency guard for server-to-server webhooks.
- SQL must use `NamedParameterJdbcTemplate` with named parameters.
- Do not introduce legacy `/api/admin/*`, `/api/mobile/*`, or `/api/integration/*` compatibility routes beyond the formal `/api/v1/integration/**` contract.

## 8. Audit Events

Use exact OpenAPI event names:

- `open_auth.succeeded` / `open_auth.failed`
- `api_call.logged` / `api_call.rejected`
- `integration.compensation_task.viewed` / `integration.compensation_task.viewed_failed`
- `integration.device.viewed` / `integration.device.viewed_failed`
- `integration.device_event.viewed` / `integration.device_event.viewed_failed`
- `integration.offline_sync_batch.viewed` / `integration.offline_sync_batch.viewed_failed`
- `integration.order_sync.viewed` / `integration.order_sync.viewed_failed`
- `provider_adapter.viewed` / `provider_adapter.view_failed`
- `provider_adapter.created` / `provider_adapter.create_failed`
- `open_mapping.changed` / `provider_adapter.update_failed`
- `provider_adapter.production_ready_marked` / `provider_adapter.production_ready_failed`
- `provider_adapter.tested` / `provider_adapter.test_failed`
- `integration.retail_sku.viewed` / `integration.retail_sku.viewed_failed`
- `integration.retry_task.viewed` / `integration.retry_task.viewed_failed`
- `integration.robot_task_event.viewed` / `integration.robot_task_event.viewed_failed`
- `integration.robot_task.viewed` / `integration.robot_task.viewed_failed`
- `integration.video_camera.viewed` / `integration.video_camera.viewed_failed`
- `integration.voucher_verification.viewed` / `integration.voucher_verification.viewed_failed`
- `integration.wecom_binding.viewed` / `integration.wecom_binding.viewed_failed`
- `integration.wecom_conversation.viewed` / `integration.wecom_conversation.viewed_failed`
- `integration.wecom_message.viewed` / `integration.wecom_message.viewed_failed`
- `integration.wecom_risk_hit.viewed` / `integration.wecom_risk_hit.viewed_failed`

## 9. TDD Plan

RED tests first:

- `OpenPlatformControllerTests` for:
  - auth success/failure;
  - webhook success/failure;
  - event result query;
  - missing/invalid signature;
  - webhook missing idempotency key.
- `IntegrationControllerTests` for:
  - exact permissions on all 20 integration endpoints;
  - missing idempotency key on 5 write endpoints.
- `IntegrationServiceTests` for:
  - HMAC signature verification;
  - nonce replay rejection;
  - token hash only stored;
  - webhook event persisted and duplicate idempotency returns same result;
  - production-ready rejects missing secret/webhook/polling/capabilities;
  - mark production ready success;
  - mock test scenarios recorded;
  - read ledger routing returns the correct ledger type.
- `IntegrationJdbcIntegrationTests` for:
  - V015 tables;
  - permission seed;
  - no plaintext token/secret persistence in token table;
  - provider adapter create/update/list;
  - open event save/query;
  - latest Flyway version if asserted.

GREEN implementation:

- Add V015 migration.
- Add integration package controller/service/repository/records/DTOs.
- Keep real supplier calls out of scope.
- Implement a P0 mock secret resolver by secret reference. Do not persist production secrets.

Refactor only after targeted tests pass.

## 10. Verification

Run targeted tests:

```bash
cd /home/james/complex-management/backend
mvn -q -Dtest=OpenPlatformControllerTests,IntegrationControllerTests,IntegrationServiceTests,IntegrationJdbcIntegrationTests test
```

Run contract and full P0:

```bash
cd /home/james/complex-management
npm run verify:p0-contract
/opt/complex/scripts/complex-verify-p0.sh
```

Security and quality scan:

```bash
cd /home/james/complex-management
grep -R -n -E 'createStatement|Statement|String\\.format|SELECT .*\\+|UPDATE .*\\+|INSERT .*\\+|DELETE .*\\+|Double|double|getDouble|Math\\.round|float|Float|api[_-]?secret|access[_-]?token\\s*=' backend/src/main/java/com/complex/platform/integration backend/src/test/java/com/complex/platform/integration || true
```

Plaintext persistence spot check:

```bash
cd /home/james/complex-management
grep -R -n -E 'secret_value|plain_secret|access_token VARCHAR|token VARCHAR' backend/src/main/resources/db/migration/V015__p0_open_platform_integration_adapter_boundary.sql || true
```

## 11. Rollback

- Before implementation, create a tar snapshot under `/home/james/complex-management-backups/`.
- If the slice fails after V015 migration, restore the snapshot for code and reset the development database only if needed.
- Do not edit or rewrite already applied V001-V014 migrations; add only V015 for this slice.
