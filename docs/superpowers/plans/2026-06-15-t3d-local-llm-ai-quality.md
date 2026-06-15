# T3-D1 Local LLM AI Quality Implementation Plan

For agentic workers: REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

Goal: Add a safe T3-D1 backend slice that uses a 160-local OpenAI-compatible LLM endpoint to generate AI quality suggestions without changing core business state.

Architecture: The Analytics module exposes one idempotent write endpoint. AnalyticsService redacts and truncates text, calls a loopback-only LocalLlmClient, stores task/result records, and emits audit events. Local LLM is disabled by default and degrades to manual review.

Tech Stack: Java 21, Spring Boot 4.x, PostgreSQL/Flyway, Java HttpClient, Jackson, JUnit, MockMvc, Node verification scripts.

---

### Task 1: RED service tests for local LLM suggestion

Files:
- Modify: backend/src/test/java/com/complex/platform/analytics/AnalyticsServiceTests.java

- [ ] Step 1: Write failing tests

Add tests asserting that generateLocalLlmAiQualitySuggestion saves one task/result, redacts sensitive text before calling the client, and falls back to manual review when the client is unavailable.

- [ ] Step 2: Run test to verify it fails

Run: npm run test:backend -- -Dtest=AnalyticsServiceTests

Expected: compile failure or missing method/type failure for the new T3-D1 API.

### Task 2: GREEN domain records, repository writes, redactor, and local client

Files:
- Modify: backend/src/main/java/com/complex/platform/analytics/AnalyticsRecords.java
- Modify: backend/src/main/java/com/complex/platform/analytics/AnalyticsRepository.java
- Modify: backend/src/main/java/com/complex/platform/analytics/JdbcAnalyticsRepository.java
- Create: backend/src/main/java/com/complex/platform/analytics/AiQualitySuggestionRequest.java
- Create: backend/src/main/java/com/complex/platform/analytics/LocalLlmClient.java
- Create: backend/src/main/java/com/complex/platform/analytics/OpenAiCompatibleLocalLlmClient.java
- Create: backend/src/main/java/com/complex/platform/analytics/AiTextRedactor.java
- Create: backend/src/main/resources/db/migration/V040__t3d_local_llm_ai_quality_mode.sql

- [ ] Step 1: Implement minimal types

Add request/response records, local LLM suggestion records, repository save methods, and a redactor that masks phone, ID card, card-like numbers, and email addresses.

- [ ] Step 2: Implement local client

Use Java HttpClient against a loopback-only OpenAI-compatible /chat/completions endpoint. Default disabled client returns model_status=unavailable and never sends text outside the machine.

- [ ] Step 3: Run tests

Run: npm run test:backend -- -Dtest=AnalyticsServiceTests

Expected: service tests pass.

### Task 3: Controller endpoint and OpenAPI contract

Files:
- Modify: backend/src/main/java/com/complex/platform/analytics/AnalyticsController.java
- Modify: backend/src/test/java/com/complex/platform/analytics/AnalyticsControllerTests.java
- Modify: docs/openapi/openapi-p0.yaml
- Modify: docs/openapi/openapi-p0.json
- Modify generated docs via scripts: docs/p0-openapi-operation-acceptance-matrix-v0.1.md, docs/p0-openapi-contract-closure-appendix-v0.1.md

- [ ] Step 1: Add failing controller test

Assert POST /api/v1/analytics/ai-quality/local-llm-suggestions requires analytics.risk_alert.manage and rejects missing idempotency_key.

- [ ] Step 2: Implement controller method

Add endpoint that checks permission, requires idempotency, and delegates to service.

- [ ] Step 3: Update OpenAPI

Add operationId generateLocalLlmAiQualitySuggestion, request body schema, example, permission, idempotency, audit events, and terminal scope.

- [ ] Step 4: Regenerate matrix and closure appendix

Run: npm run docs:operation-matrix && npm run docs:openapi-closure

### Task 4: T3-D1 verification script and entry docs

Files:
- Create: tools/verify-t3d-local-llm-ai-quality.mjs
- Modify: package.json
- Create: docs/t3d-local-llm-ai-quality-v0.1.md
- Modify: docs/00-current-effective-document-entry-v0.1.md
- Modify: docs/project-progress-dashboard.md

- [ ] Step 1: Add machine verifier

Verifier must require markers T3D_LOCAL_LLM_ADAPTER_READY, LOOPBACK_ONLY_LLM, AI_OUTPUT_NO_AUTO_DECISION, and package script verify:t3d-local-llm-ai-quality.

- [ ] Step 2: Update docs

Document the local model setup, default disabled mode, loopback-only rule, manual review fallback, and exact verification commands.

### Task 5: Full verification and commit

Files:
- All changed files from Tasks 1-4.

- [ ] Step 1: Run targeted checks

Run:
- npm run verify:t3d-local-llm-ai-quality
- npm run test:backend -- -Dtest=AnalyticsServiceTests,AnalyticsControllerTests,AnalyticsJdbcIntegrationTests
- npm run verify:p0-contract

- [ ] Step 2: Run security and build checks

Run:
- npm run test:backend
- npm run build
- npm run verify:security-baseline
- ~/.local/bin/semgrep scan --config p/owasp-top-ten --config p/secrets --metrics=off --json --output security-scans/latest-semgrep-output.json backend apps/web packages tools docs/supplier-mock

- [ ] Step 3: Commit

Commit message: feat: add t3d local llm ai quality slice

## Risk and rollback

- If local model runtime is unstable, keep complex.analytics.local-llm.enabled=false; endpoint degrades to manual review.
- If OpenAPI contract drifts, run npm run docs:openapi-closure and npm run verify:p0-contract before commit.
- If the LLM client leaks non-loopback traffic, revert Task 2 and keep only the design docs.
